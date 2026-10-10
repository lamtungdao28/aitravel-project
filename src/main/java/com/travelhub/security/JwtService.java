package com.travelhub.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.time.Clock;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service @Slf4j
public class JwtService {
    private final SecretKey key;
    private final Clock clock;
    private final Duration accessTtl;
    private static final String ISSUER="travelhub";
    public JwtService(@Value("${jwt.secret:}") String secret,@Value("${jwt.access-ttl:15m}") Duration accessTtl,Clock clock,Environment environment) {
        this.clock=clock;this.accessTtl=accessTtl;
        if(accessTtl.isZero() || accessTtl.isNegative()) throw new IllegalArgumentException("JWT access TTL must be positive");
        if(secret.isBlank()) {
            if(!environment.matchesProfiles("dev","test")) throw new IllegalStateException("JWT_SECRET is required outside development/test");
            byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);key=Keys.hmacShaKeyFor(bytes);
            log.warn("Using an ephemeral development JWT key; set JWT_SECRET for persistent sessions");
        } else {
            if(secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
            key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }
    public String issue(Long userId) {
        Instant now=clock.instant();
        return Jwts.builder().issuer(ISSUER).subject(userId.toString()).claim("type","access")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(accessTtl))).id(UUID.randomUUID().toString())
                .signWith(key,Jwts.SIG.HS256).compact();
    }
    public Long userId(String token) {
        var jwt=Jwts.parser().verifyWith(key).requireIssuer(ISSUER).require("type","access")
                .clock(() -> Date.from(clock.instant())).build().parseSignedClaims(token);
        if(!"HS256".equals(jwt.getHeader().getAlgorithm())) throw new JwtException("Unsupported algorithm");
        return Long.valueOf(jwt.getPayload().getSubject());
    }
}
