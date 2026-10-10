package com.travelhub;

import static org.assertj.core.api.Assertions.*;

import com.travelhub.security.JwtService;

import io.jsonwebtoken.JwtException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.*;

class JwtServiceTest {
    private static final String KEY = "test-only-jwt-signing-secret-at-least-32-bytes";
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void validatesSignatureIssuerAndExpiry() {
        var issuer = new JwtService(KEY, Duration.ofMinutes(15), clock, new MockEnvironment());
        String token = issuer.issue(42L);
        assertThat(issuer.userId(token)).isEqualTo(42L);
        var expired =
                new JwtService(
                        KEY,
                        Duration.ofMinutes(15),
                        Clock.offset(clock, Duration.ofMinutes(16)),
                        new MockEnvironment());
        assertThatThrownBy(() -> expired.userId(token)).isInstanceOf(JwtException.class);
        var wrongKey =
                new JwtService(
                        KEY + "different", Duration.ofMinutes(15), clock, new MockEnvironment());
        assertThatThrownBy(() -> wrongKey.userId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void refusesMissingProductionSecretAndWeakKeys() {
        var mixedProfiles = new MockEnvironment();
        mixedProfiles.setActiveProfiles("dev", "prod");
        assertThatThrownBy(() -> new JwtService("", Duration.ofMinutes(15), clock, mixedProfiles))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                new JwtService(
                                        "", Duration.ofMinutes(15), clock, new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                new JwtService(
                                        "short",
                                        Duration.ofMinutes(15),
                                        clock,
                                        new MockEnvironment()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(KEY, Duration.ZERO, clock, new MockEnvironment()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
