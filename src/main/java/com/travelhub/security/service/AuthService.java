package com.travelhub.security.service;
import com.travelhub.security.*;
import com.travelhub.security.dto.*;
import com.travelhub.security.entity.RefreshToken;
import com.travelhub.security.repository.RefreshTokenRepository;
import com.travelhub.user.dto.UserCreateRequest;
import com.travelhub.user.repository.UserRepository;
import com.travelhub.user.service.UserService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserService users;
    private final UserRepository userRepository;
    private final RefreshTokenRepository tokens;
    private final JwtService jwt;
    private final Clock clock;
    private final Duration refreshTtl;
    private final SecureRandom random=new SecureRandom();
    public AuthService(AuthenticationManager authenticationManager,UserService users,UserRepository userRepository,RefreshTokenRepository tokens,
                       JwtService jwt,Clock clock,@Value("${jwt.refresh-ttl:7d}") Duration refreshTtl) {
        this.authenticationManager=authenticationManager;this.users=users;this.userRepository=userRepository;this.tokens=tokens;this.jwt=jwt;this.clock=clock;this.refreshTtl=refreshTtl;
        if(refreshTtl.isNegative() || refreshTtl.isZero())throw new IllegalArgumentException("Refresh TTL must be positive");
    }
    public TokenResponse register(UserCreateRequest r){return issue(users.create(r).id());}
    public TokenResponse login(LoginRequest r) {
        var a=authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(r.email(),r.password()));
        return issue(((TravelHubPrincipal)a.getPrincipal()).id());
    }
    public TokenResponse refresh(RefreshRequest r) {
        RefreshToken old=tokens.findLockedByHash(hash(r.refreshToken())).orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if(old.isRevoked() || !old.getExpiresAt().isAfter(clock.instant()))throw new BadCredentialsException("Invalid refresh token");
        old.setRevoked(true);
        return issue(old.getUser().getId());
    }
    private TokenResponse issue(Long userId) {
        byte[] bytes=new byte[32];random.nextBytes(bytes);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token=new RefreshToken();token.setUser(userRepository.getReferenceById(userId));
        token.setTokenHash(hash(raw));token.setExpiresAt(clock.instant().plus(refreshTtl));tokens.saveAndFlush(token);
        return new TokenResponse(jwt.issue(userId),raw,"Bearer");
    }
    private String hash(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
}
