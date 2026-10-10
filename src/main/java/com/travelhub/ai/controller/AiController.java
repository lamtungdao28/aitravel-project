package com.travelhub.ai.controller;
import com.travelhub.ai.dto.*;
import com.travelhub.ai.service.TravelAiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor @Validated @Tag(name="AI")
public class AiController {
    private final TravelAiService service;
    @GetMapping("/places/{placeId}/review-summary")
    public ReviewSummaryResponse summary(@PathVariable @Positive Long placeId) {return service.summary(placeId);}
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/recommendations")
    public List<PlaceRecommendation> recommendations(@PathVariable @Positive Long userId,
            @RequestParam(required=false) @Positive Long destinationId,@RequestParam(defaultValue="10") @Min(1) @Max(20) int limit) {
        return service.recommendations(userId,destinationId,limit);
    }
}
