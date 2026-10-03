package com.bizexpense.global.security;

import com.bizexpense.domain.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_LOGIN_ID = "loginId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_DEPARTMENT_ID = "deptId";

    private final SecretKey key;
    private final long validityMillis;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.access-token-validity}") long validityMillis) {
        this.key = signingKey(secret);
        this.validityMillis = validityMillis;
    }

    /**
     * Base64 로 인코딩된 256bit 이상 키면 그대로 쓰고, 아니면(배포 환경이 생성한 임의 문자열 등)
     * SHA-256 으로 256bit 키를 만든다. 어떤 형식의 비밀값이 주어져도 서버가 뜨도록 하기 위함.
     */
    private static SecretKey signingKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret 이 설정되지 않았습니다. (JWT_SECRET 환경변수)");
        }
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length >= 32) {
                return Keys.hmacShaKeyFor(decoded);
            }
        } catch (RuntimeException ignored) {
            // Base64 가 아니면 아래에서 해시로 키를 만든다
        }
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String createToken(LoginUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim(CLAIM_LOGIN_ID, user.loginId())
                .claim(CLAIM_ROLE, user.role().name())
                .claim(CLAIM_DEPARTMENT_ID, user.departmentId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validityMillis))
                .signWith(key)
                .compact();
    }

    public long getValiditySeconds() {
        return validityMillis / 1000;
    }

    /** 서명·만료가 유효하면 LoginUser 를, 아니면 empty 를 반환한다. */
    public Optional<LoginUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            Number deptId = claims.get(CLAIM_DEPARTMENT_ID, Number.class);
            return Optional.of(new LoginUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_LOGIN_ID, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    deptId == null ? null : deptId.longValue()));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
