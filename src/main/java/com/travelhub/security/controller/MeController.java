package com.travelhub.security.controller;
import com.travelhub.security.Access;
import com.travelhub.user.dto.*;
import com.travelhub.user.service.UserService;
import com.travelhub.address.dto.*;
import com.travelhub.address.service.AddressService;
import com.travelhub.favorite.dto.FavoriteResponse;
import com.travelhub.favorite.service.FavoriteService;
import com.travelhub.place.dto.PlaceSummaryResponse;
import com.travelhub.trip.dto.TripResponse;
import com.travelhub.trip.service.TripService;
import com.travelhub.booking.dto.BookingResponse;
import com.travelhub.booking.service.BookingService;
import com.travelhub.ai.dto.PlaceRecommendation;
import com.travelhub.ai.service.TravelAiService;
import com.travelhub.common.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/me") @RequiredArgsConstructor @Validated @Tag(name="My Account")
public class MeController {
    private final Access access;
    private final UserService users;
    private final AddressService addresses;
    private final FavoriteService favorites;
    private final TripService trips;
    private final BookingService bookings;
    private final TravelAiService ai;
    @GetMapping public UserResponse get() {return users.get(access.id());}
    @PutMapping public UserResponse update(@Valid @RequestBody UserUpdateRequest r) {return users.update(access.id(),r);}
    @DeleteMapping public ResponseEntity<Void> delete() {users.delete(access.id());return ResponseEntity.noContent().build();}
    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> address(@Valid @RequestBody AddressCreateRequest r) {
        var response=addresses.create(access.id(),r);
        return ResponseEntity.created(URI.create("/api/v1/addresses/"+response.id())).body(response);
    }
    @GetMapping("/addresses") public PageResponse<AddressResponse> addresses(@ParameterObject Pageable p) {return addresses.list(access.id(),p);}
    @PostMapping("/favorites/{placeId}")
    public ResponseEntity<FavoriteResponse> favorite(@PathVariable @Positive Long placeId) {
        return ResponseEntity.created(URI.create("/api/v1/me/favorites/"+placeId)).body(favorites.add(access.id(),placeId));
    }
    @DeleteMapping("/favorites/{placeId}")
    public ResponseEntity<Void> unfavorite(@PathVariable @Positive Long placeId) {
        favorites.remove(access.id(),placeId);return ResponseEntity.noContent().build();
    }
    @GetMapping("/favorites") public PageResponse<PlaceSummaryResponse> favorites(@ParameterObject Pageable p) {return favorites.list(access.id(),p);}
    @GetMapping("/trips") public PageResponse<TripResponse> trips(@ParameterObject Pageable p) {return trips.list(access.id(),p);}
    @GetMapping("/bookings") public PageResponse<BookingResponse> bookings(@ParameterObject Pageable p) {return bookings.list(access.id(),p);}
    @GetMapping("/recommendations")
    public List<PlaceRecommendation> recommendations(@RequestParam(required=false) @Positive Long destinationId,
            @RequestParam(defaultValue="10") @Min(1) @Max(20) int limit) {
        return ai.recommendations(access.id(),destinationId,limit);
    }
}
