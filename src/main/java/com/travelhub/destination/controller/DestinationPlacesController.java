package com.travelhub.destination.controller;
import com.travelhub.place.dto.PlaceSummaryResponse;
import com.travelhub.place.service.PlaceService;
import com.travelhub.common.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/destinations")
@RequiredArgsConstructor
@Validated
@Tag(name="Destinations")
public class DestinationPlacesController {
    private final PlaceService placeService;
    @GetMapping("/{id}/places")
    public PageResponse<PlaceSummaryResponse> places(@PathVariable @Positive Long id,
            @ParameterObject @PageableDefault(sort="name") Pageable pageable) {
        return placeService.destinationPlaces(id, pageable);
    }
}
