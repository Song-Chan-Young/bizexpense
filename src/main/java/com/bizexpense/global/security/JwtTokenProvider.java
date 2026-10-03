package com.bizexpense.global.security;

import com.bizexpense.domain.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.validityMillis = validityMillis;
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
