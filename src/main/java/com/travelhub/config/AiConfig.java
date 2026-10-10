package com.travelhub.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {
    @Bean
    RestClient llmRestClient(AiProperties p) {
        URI uri = URI.create(p.getBaseUrl());
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || uri.getHost() == null)
            throw new IllegalArgumentException("AI base URL must be an HTTP(S) URL");
        if (p.getTimeout().isNegative() || p.getTimeout().isZero())
            throw new IllegalArgumentException("AI timeout must be positive");
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(p.getTimeout()).build());
        factory.setReadTimeout(p.getTimeout());
        return RestClient.builder().baseUrl(p.getBaseUrl()).requestFactory(factory).build();
    }
}
