package com.travelhub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelhub.common.response.ErrorResponse;

import jakarta.servlet.http.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SecurityErrors {
    private final ObjectMapper mapper;

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        mapper.writeValue(
                response.getOutputStream(),
                new ErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        status == HttpStatus.UNAUTHORIZED
                                ? "Authentication required or token invalid"
                                : "Access denied",
                        request.getRequestURI(),
                        Map.of()));
    }
}
