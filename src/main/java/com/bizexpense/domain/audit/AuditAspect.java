package com.bizexpense.domain.audit;

import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.bizexpense.global.common.ApiResponse;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link Audited} 컨트롤러 메서드의 결과를 감사 로그로 남긴다.
 * <ul>
 *   <li>수행자: 로그인한 사용자. 로그인 요청처럼 아직 인증 전이면 요청 본문의 loginId 로 찾는다.</li>
 *   <li>대상 ID: 응답 데이터의 첫 번째 ID(Long) → 없으면(삭제 등) 주소의 마지막 경로 변수.</li>
 *   <li>실패(업무 오류·권한 없음 등)도 오류 코드와 함께 남기고, 원래 예외는 그대로 던진다.</li>
 * </ul>
 * 감사 로그 저장이 실패해도 업무 요청 결과에는 영향을 주지 않는다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable e) {
            save(audited.value(), pjp.getArgs(), null, errorCode(e));
            throw e;
        }
        save(audited.value(), pjp.getArgs(), result, null);
        return result;
    }

    private void save(AuditAction action, Object[] args, Object result, String errorCode) {
        try {
            HttpServletRequest request = currentRequest();
            Actor actor = actor(args);
            auditLogRepository.save(AuditLog.builder()
                    .actorId(actor.id())
                    .actorLoginId(actor.loginId())
                    .actorName(actor.name())
                    .action(action)
                    .targetId(targetId(result, request))
                    .result(errorCode == null ? AuditResult.SUCCESS : AuditResult.FAILURE)
                    .errorCode(errorCode)
                    .httpMethod(request == null ? "-" : request.getMethod())
                    .uri(request == null ? "-" : request.getRequestURI())
                    .ip(request == null ? null : request.getRemoteAddr())
                    .build());
        } catch (RuntimeException e) {
            log.warn("감사 로그 저장 실패: {}", action, e);
        }
    }

    private Actor actor(Object[] args) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            String name = userRepository.findById(loginUser.userId()).map(User::getName).orElse(null);
            return new Actor(loginUser.userId(), loginUser.loginId(), name);
        }
        // 인증 전 요청(로그인): 요청 본문의 loginId 로 사용자를 찾는다
        return Arrays.stream(args)
                .map(AuditAspect::loginIdOf)
                .flatMap(Optional::stream)
                .findFirst()
                .map(loginId -> userRepository.findByLoginId(loginId)
                        .map(u -> new Actor(u.getId(), loginId, u.getName()))
                        .orElse(new Actor(null, loginId, null)))
                .orElse(new Actor(null, null, null));
    }

    private static Long targetId(Object result, HttpServletRequest request) {
        if (result instanceof ApiResponse<?> response && response.data() != null
                && response.data().getClass().isRecord()) {
            for (RecordComponent c : response.data().getClass().getRecordComponents()) {
                if (c.getType() == Long.class && c.getName().endsWith("Id")) {
                    return (Long) invoke(c, response.data());
                }
            }
        }
        if (request != null
                && request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> vars
                && !vars.isEmpty()) {
            List<?> values = List.copyOf(vars.values());
            try {
                return Long.valueOf(String.valueOf(values.getLast()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Optional<String> loginIdOf(Object arg) {
        if (arg == null || !arg.getClass().isRecord()) {
            return Optional.empty();
        }
        return Arrays.stream(arg.getClass().getRecordComponents())
                .filter(c -> c.getName().equals("loginId") && c.getType() == String.class)
                .map(c -> (String) invoke(c, arg))
                .findFirst();
    }

    private static Object invoke(RecordComponent component, Object target) {
        try {
            return component.getAccessor().invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static String errorCode(Throwable e) {
        if (e instanceof BusinessException be) {
            return be.getErrorCode().name();
        }
        if (e instanceof AccessDeniedException) {
            return "ACCESS_DENIED";
        }
        return e.getClass().getSimpleName();
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? attrs.getRequest() : null;
    }

    private record Actor(Long id, String loginId, String name) {
    }
}
