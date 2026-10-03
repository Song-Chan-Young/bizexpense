package com.bizexpense.domain.auth;

import com.bizexpense.domain.auth.dto.LoginRequest;
import com.bizexpense.domain.auth.dto.LoginResponse;
import com.bizexpense.domain.auth.dto.MeResponse;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.bizexpense.global.error.BusinessException;
import com.bizexpense.global.error.ErrorCode;
import com.bizexpense.global.security.JwtTokenProvider;
import com.bizexpense.global.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        // 아이디 존재 여부를 노출하지 않도록 아이디/비밀번호 오류는 같은 코드로 응답한다.
        User user = userRepository.findByLoginId(request.loginId())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.DISABLED_USER);
        }

        LoginUser loginUser = new LoginUser(
                user.getId(),
                user.getLoginId(),
                user.getRole(),
                user.getDepartment() == null ? null : user.getDepartment().getId());

        String token = jwtTokenProvider.createToken(loginUser);
        return LoginResponse.bearer(token, jwtTokenProvider.getValiditySeconds(), MeResponse.from(user));
    }

    public MeResponse me(LoginUser loginUser) {
        return userRepository.findWithDepartmentById(loginUser.userId())
                .map(MeResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
