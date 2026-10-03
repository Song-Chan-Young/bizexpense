package com.bizexpense.domain.approval;

import com.bizexpense.domain.approval.dto.ApprovalResponse;
import com.bizexpense.domain.approval.dto.ApprovalSearchCondition;
import com.bizexpense.domain.approval.dto.ApprovalSearchCondition.ApprovalBox;
import com.bizexpense.domain.user.Role;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.bizexpense.global.common.PageResponse;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import com.bizexpense.global.security.LoginUser;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final Map<ApprovalTargetType, ApprovalHandler> handlers = new EnumMap<>(ApprovalTargetType.class);

    public ApprovalService(ApprovalRepository approvalRepository, UserRepository userRepository,
                           List<ApprovalHandler> handlers) {
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        handlers.forEach(h -> this.handlers.put(h.targetType(), h));
    }

    /**
     * 결재 요청 생성. 결재자는 신청자의 역할에 따라 정한다.
     * - 일반 직원: 같은 부서 팀장 (없으면 관리자)
     * - 팀장 / 관리자: 다른 관리자
     */
    @Transactional
    public Approval request(ApprovalTargetType targetType, Long targetId, String title, User requester) {
        User approver = resolveApprover(requester)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVER_NOT_FOUND));
        return approvalRepository.save(new Approval(targetType, targetId, title, requester, approver));
    }

    /** 신청자가 결재 전 신청을 취소한 경우 대기 중인 결재 건도 취소한다. */
    @Transactional
    public void cancelPending(ApprovalTargetType targetType, Long targetId) {
        approvalRepository.findByTargetTypeAndTargetIdAndStatus(targetType, targetId, ApprovalStatus.PENDING)
                .ifPresent(Approval::cancel);
    }

    public List<ApprovalResponse> history(ApprovalTargetType targetType, Long targetId, Long viewerId) {
        return approvalRepository.findByTargetTypeAndTargetIdOrderByIdDesc(targetType, targetId).stream()
                .map(a -> ApprovalResponse.of(a, viewerId))
                .toList();
    }

    public PageResponse<ApprovalResponse> search(LoginUser loginUser, ApprovalSearchCondition cond, Pageable pageable) {
        if (cond.boxOrDefault() == ApprovalBox.ALL && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return PageResponse.of(
                approvalRepository.findAll(ApprovalSpecs.search(loginUser.userId(), cond), pageable),
                a -> ApprovalResponse.of(a, loginUser.userId()));
    }

    /** 승인: 결재 상태와 대상 업무 상태를 한 트랜잭션에서 바꾼다. */
    @Transactional
    public ApprovalResponse approve(LoginUser loginUser, Long approvalId, String comment) {
        Approval approval = findForProcess(loginUser, approvalId);
        approval.approve(comment);
        handler(approval.getTargetType()).onApproved(approval.getTargetId());
        return ApprovalResponse.of(approval, loginUser.userId());
    }

    @Transactional
    public ApprovalResponse reject(LoginUser loginUser, Long approvalId, String comment) {
        Approval approval = findForProcess(loginUser, approvalId);
        approval.reject(comment);
        handler(approval.getTargetType()).onRejected(approval.getTargetId());
        return ApprovalResponse.of(approval, loginUser.userId());
    }

    private Approval findForProcess(LoginUser loginUser, Long approvalId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));
        // 지정된 결재자만 처리할 수 있다 (팀장은 자기 팀 신청 건만 배정받는다)
        if (!approval.isApprover(loginUser.userId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return approval;
    }

    private ApprovalHandler handler(ApprovalTargetType type) {
        ApprovalHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalStateException("결재 처리기가 없습니다: " + type);
        }
        return handler;
    }

    private Optional<User> resolveApprover(User requester) {
        if (requester.getRole() == Role.USER && requester.getDepartment() != null) {
            Optional<User> manager = userRepository.findFirstByDepartmentIdAndRoleAndActiveTrueOrderByIdAsc(
                    requester.getDepartment().getId(), Role.MANAGER);
            if (manager.isPresent()) {
                return manager;
            }
        }
        return userRepository.findFirstByRoleAndActiveTrueAndIdNotOrderByIdAsc(Role.ADMIN, requester.getId());
    }
}
