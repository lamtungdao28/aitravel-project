package com.travelhub.ai.client;

import com.fasterxml.jackson.databind.*;
import com.travelhub.ai.exception.*;
import com.travelhub.config.AiProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiLlmClient implements LlmClient {
    private final RestClient llmRestClient;
    private final ObjectMapper mapper;
    private final AiProperties properties;

    public JsonNode complete(String instructions, Object data) {
        if (!properties.isEnabled() || properties.getApiKey().isBlank())
            throw new AiUnavailableException("AI is not configured");
        try {
            var body =
                    Map.of(
                            "model",
                            properties.getModel(),
                            "messages",
                            List.of(
                                    Map.of("role", "system", "content", instructions),
                                    Map.of(
                                            "role",
                                            "user",
                                            "content",
                                            mapper.writeValueAsString(data))),
                            "response_format",
                            Map.of("type", "json_object"),
                            "max_completion_tokens",
                            properties.getMaxOutputTokens());
            return llmRestClient
                    .post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange(
                            (request, response) -> {
                                if (!response.getStatusCode().is2xxSuccessful())
                                    throw new AiUnavailableException(
                                            "AI provider is temporarily unavailable");
                                byte[] bytes = response.getBody().readNBytes(65537);
                                if (bytes.length > 65536)
                                    throw new AiResponseException(
                                            "AI response exceeds the size limit");
                                JsonNode result = mapper.readTree(bytes);
                                JsonNode choice = result.path("choices").path(0);
                                if (!"stop".equals(choice.path("finish_reason").asText())
                                        || !choice.path("message").path("content").isTextual())
                                    throw new AiResponseException(
                                            "AI provider returned an incomplete response");
                                JsonNode content =
                                        mapper.readTree(
                                                choice.path("message").path("content").textValue());
                                if (content == null || !content.isObject())
                                    throw new AiResponseException(
                                            "AI provider returned invalid JSON");
                                return content;
                            });
        } catch (AiResponseException | AiUnavailableException ex) {
            log.warn("AI provider request failed: {}", ex.getClass().getSimpleName());
            throw ex;
        } catch (Exception ex) {
            log.warn("AI provider request failed: {}", ex.getClass().getSimpleName());
            throw new AiUnavailableException("AI provider is temporarily unavailable");
        }
    }
}
