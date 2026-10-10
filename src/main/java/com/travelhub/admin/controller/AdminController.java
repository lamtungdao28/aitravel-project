package com.travelhub.admin.controller;
import com.travelhub.admin.dto.*;
import com.travelhub.admin.service.AdminService;
import com.travelhub.booking.dto.BookingResponse;
import com.travelhub.booking.entity.BookingStatus;
import com.travelhub.common.response.PageResponse;
import com.travelhub.place.dto.*;
import com.travelhub.place.service.PlaceService;
import com.travelhub.review.dto.ReviewResponse;
import com.travelhub.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin") @RequiredArgsConstructor @Validated @Tag(name="Admin")
public class AdminController {
    private final AdminService service;
    private final PlaceService places;
    @GetMapping("/dashboard/overview") public DashboardOverviewResponse overview(){return service.overview();}
    @GetMapping("/dashboard/users") public UserStatisticsResponse userStats(){return service.userStatistics();}
    @GetMapping("/dashboard/bookings") public BookingStatisticsResponse bookingStats(){return service.bookingStatistics();}
    @GetMapping("/dashboard/places") public PlaceStatisticsResponse placeStats(){return service.placeStatistics();}
    @GetMapping("/users")
    public PageResponse<UserResponse> users(@RequestParam(required=false) @Size(max=200) String keyword,
            @RequestParam(required=false) @Size(max=30) String role,@ParameterObject @PageableDefault(sort="createdAt") Pageable p) {
        return service.users(keyword,role,p);
    }
    @GetMapping("/users/{id}") public UserResponse user(@PathVariable @Positive Long id){return service.user(id);}
    @DeleteMapping("/users/{id}") public ResponseEntity<Void> deleteUser(@PathVariable @Positive Long id){
        service.deleteUser(id);return ResponseEntity.noContent().build();
    }
    @GetMapping("/reviews")
    public PageResponse<ReviewResponse> reviews(@RequestParam(required=false) @Positive Long userId,@RequestParam(required=false) @Positive Long placeId,
            @RequestParam(required=false) @Min(1) @Max(5) Integer minRating,@ParameterObject @PageableDefault(sort="createdAt") Pageable p) {
        return service.reviews(userId,placeId,minRating,p);
    }
    @DeleteMapping("/reviews/{id}") public ResponseEntity<Void> deleteReview(@PathVariable @Positive Long id){
        service.deleteReview(id);return ResponseEntity.noContent().build();
    }
    @GetMapping("/bookings")
    public PageResponse<BookingResponse> bookings(@RequestParam(required=false) @Positive Long userId,
            @RequestParam(required=false) @Positive Long placeId,@RequestParam(required=false) BookingStatus status,
            @RequestParam(required=false) LocalDate dateFrom,@RequestParam(required=false) LocalDate dateTo,
            @ParameterObject @PageableDefault(sort="bookingDate") Pageable p) {
        return service.bookings(userId,placeId,status,dateFrom,dateTo,p);
    }
    @GetMapping("/places")
    public PageResponse<PlaceSummaryResponse> places(@RequestParam(required=false) @Size(max=200) String keyword,
            @RequestParam(required=false) @Positive Long destinationId,@RequestParam(required=false) @Positive Long categoryId,
            @RequestParam(required=false) @DecimalMin("0") @DecimalMax("5") BigDecimal minRating,
            @RequestParam(required=false) @Min(0) @Max(4) Integer maxPriceLevel,@RequestParam(required=false) Boolean bookingEnabled,
            @ParameterObject @PageableDefault(sort="name") Pageable p) {
        return places.search(new PlaceFilter(keyword,destinationId,categoryId,minRating,maxPriceLevel,bookingEnabled),p);
    }
}
