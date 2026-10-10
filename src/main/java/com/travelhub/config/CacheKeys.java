package com.travelhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.*;

@Component("cacheKeys")
@RequiredArgsConstructor
public class CacheKeys {
    private final ObjectMapper mapper;

    public String key(Object... values) {
        try {
            byte[] json =
                    mapper.writeValueAsBytes(
                            Arrays.stream(values)
                                    .map(v -> v == null ? null : v.toString())
                                    .toList());
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot generate cache key", ex);
        }
    }
}
