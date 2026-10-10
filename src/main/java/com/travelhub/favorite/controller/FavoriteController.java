package com.travelhub.favorite.controller;

import com.travelhub.common.response.PageResponse;
import com.travelhub.favorite.dto.FavoriteResponse;
import com.travelhub.favorite.service.FavoriteService;
import com.travelhub.place.dto.PlaceSummaryResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/favorites")
@RequiredArgsConstructor
@Validated
@Tag(name = "Favorites")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class FavoriteController {
    private final FavoriteService service;

    @PostMapping("/{placeId}")
    public ResponseEntity<FavoriteResponse> add(@PathVariable @Positive Long userId,
                                                @PathVariable @Positive Long placeId) {
        var response = service.add(userId, placeId);
        return ResponseEntity.created(URI.create("/api/v1/users/" + userId + "/favorites/" + placeId)).body(response);
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> remove(@PathVariable @Positive Long userId,
                                       @PathVariable @Positive Long placeId) {
        service.remove(userId, placeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public PageResponse<PlaceSummaryResponse> list(@PathVariable @Positive Long userId,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(userId, pageable);
    }
}
