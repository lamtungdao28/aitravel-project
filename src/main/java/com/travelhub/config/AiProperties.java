package com.travelhub.config;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private boolean enabled = false;
    @NotBlank private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    @NotBlank private String model = "gpt-4o-mini";
    @NotNull private Duration timeout = Duration.ofSeconds(20);

    @Min(1)
    @Max(100)
    private int maxReviews = 30;

    @Min(50)
    @Max(2000)
    private int maxReviewChars = 1000;

    @Min(1)
    @Max(100)
    private int maxCandidates = 30;

    @Min(100)
    @Max(4000)
    private int maxOutputTokens = 1500;
}
