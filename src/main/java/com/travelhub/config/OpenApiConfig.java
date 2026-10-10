package com.travelhub.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI travelHubApi() {
        return new OpenAPI().info(new Info().title("TravelHub API").version("v1")
                .description("Travel discovery, reviews, itineraries and bookings. Collections use zero-based page, size (1-100), and sort=field,asc|desc. Obtain a bearer token with /api/v1/auth/login. Client userId is ignored for members; administrators may act on behalf of users."))
                .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("bearerAuth",
                        new io.swagger.v3.oas.models.security.SecurityScheme().type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    org.springdoc.core.customizers.OpenApiCustomizer apiResponses() {
        return api -> {
            io.swagger.v3.core.converter.ModelConverters.getInstance()
                    .read(com.travelhub.common.response.ErrorResponse.class).forEach(api.getComponents()::addSchemas);
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                if (path.contains("/auth/") || method == io.swagger.v3.oas.models.PathItem.HttpMethod.GET
                        && path.matches("/api/v1/(destinations|places|categories|reviews)(/.*)?") && !path.endsWith("/review-summary"))
                    operation.setSecurity(java.util.List.of());
                for (var entry : java.util.Map.of("400","Validation or business rule failure","401","Authentication required or invalid token",
                        "403","Insufficient permission or resource belongs to another user","404","Resource not found","409","Duplicate or conflicting state",
                        "500","Unexpected server error").entrySet()) {
                    operation.getResponses().addApiResponse(entry.getKey(), new io.swagger.v3.oas.models.responses.ApiResponse()
                            .description(entry.getValue()).content(new io.swagger.v3.oas.models.media.Content().addMediaType("application/json",
                                    new io.swagger.v3.oas.models.media.MediaType().schema(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/ErrorResponse")))));
                }
                if(method == io.swagger.v3.oas.models.PathItem.HttpMethod.POST && !path.endsWith("/login") && !path.endsWith("/refresh")) {
                    var success=operation.getResponses().remove("200");
                    if(success!=null) operation.getResponses().addApiResponse("201",success.description("Created"));
                }
                if(method == io.swagger.v3.oas.models.PathItem.HttpMethod.DELETE) {
                    operation.getResponses().remove("200");
                    operation.getResponses().addApiResponse("204",new io.swagger.v3.oas.models.responses.ApiResponse().description("Deleted"));
                }
            }));
        };
    }
}
