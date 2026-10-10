package com.travelhub.admin.service;
import com.travelhub.admin.dto.*;
import com.travelhub.admin.repository.AdminStatisticsRepository;
import com.travelhub.booking.dto.BookingResponse;
import com.travelhub.booking.entity.*;
import com.travelhub.booking.mapper.BookingMapper;
import com.travelhub.booking.repository.BookingRepository;
import com.travelhub.common.exception.BusinessException;
import com.travelhub.common.response.PageResponse;
import com.travelhub.common.util.Pages;
import com.travelhub.destination.repository.DestinationRepository;
import com.travelhub.place.repository.PlaceRepository;
import com.travelhub.review.dto.ReviewResponse;
import com.travelhub.review.entity.Review;
import com.travelhub.review.repository.ReviewRepository;
import com.travelhub.review.service.ReviewService;
import com.travelhub.trip.repository.TripRepository;
import com.travelhub.user.dto.UserResponse;
import com.travelhub.user.entity.User;
import com.travelhub.user.mapper.UserMapper;
import com.travelhub.user.repository.UserRepository;
import com.travelhub.user.service.UserService;
import jakarta.persistence.criteria.Predicate;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class AdminService {
    private final UserRepository users;
    private final DestinationRepository destinations;
    private final PlaceRepository places;
    private final ReviewRepository reviews;
    private final TripRepository trips;
    private final BookingRepository bookings;
    private final AdminStatisticsRepository statistics;
    private final UserMapper userMapper;
    private final BookingMapper bookingMapper;
    private final UserService userService;
    private final ReviewService reviewService;
    private final Clock clock;

    public DashboardOverviewResponse overview() {
        return new DashboardOverviewResponse(users.count(),destinations.count(),places.count(),reviews.count(),trips.count(),
                bookings.count(),bookings.countByStatus(BookingStatus.PENDING),bookings.countByStatus(BookingStatus.CONFIRMED),
                bookings.countByStatus(BookingStatus.CANCELLED));
    }
    public UserStatisticsResponse userStatistics() {
        return new UserStatisticsResponse(users.count(),statistics.newUsersSince(clock.instant().minus(Duration.ofDays(30))),statistics.usersByRole());
    }
    public BookingStatisticsResponse bookingStatistics() {
        Map<BookingStatus,Long> counts=new EnumMap<>(BookingStatus.class);
        for(var s:BookingStatus.values()) counts.put(s,bookings.countByStatus(s));
        return new BookingStatisticsResponse(bookings.count(),counts);
    }
    public PlaceStatisticsResponse placeStatistics() {
        Double avg=statistics.averageRating();
        return new PlaceStatisticsResponse(places.count(),statistics.bookablePlaces(),
                avg==null ? BigDecimal.ZERO : BigDecimal.valueOf(avg).setScale(2,RoundingMode.HALF_UP));
    }
    public PageResponse<UserResponse> users(String keyword,String role,Pageable p) {
        Specification<User> spec=(root,q,cb) -> {
            List<Predicate> filters=new ArrayList<>();
            if(keyword!=null && !keyword.isBlank()) filters.add(cb.or(cb.like(cb.lower(root.get("email")),Pages.contains(keyword),'\\'),
                    cb.like(cb.lower(root.get("fullName")),Pages.contains(keyword),'\\')));
            if(role!=null && !role.isBlank()) filters.add(cb.equal(root.get("role").get("name"),role.toUpperCase(Locale.ROOT)));
            return cb.and(filters.toArray(Predicate[]::new));
        };
        return PageResponse.from(users.findAll(spec,Pages.checked(p,"id","email","fullName","createdAt")).map(userMapper::toResponse));
    }
    public UserResponse user(Long id) {return userService.get(id);}
    @Transactional public void deleteUser(Long id) {userService.delete(id);}
    @Transactional public void deleteReview(Long id) {reviewService.delete(id);}
    public PageResponse<ReviewResponse> reviews(Long userId,Long placeId,Integer minRating,Pageable p) {
        Specification<Review> spec=(root,q,cb) -> {
            List<Predicate> f=new ArrayList<>();
            if(userId!=null)f.add(cb.equal(root.get("user").get("id"),userId));
            if(placeId!=null)f.add(cb.equal(root.get("place").get("id"),placeId));
            if(minRating!=null)f.add(cb.ge(root.get("rating"),minRating));
            return cb.and(f.toArray(Predicate[]::new));
        };
        return reviewService.mapPage(reviews.findAll(spec,Pages.checked(p,"id","createdAt","rating")));
    }
    public PageResponse<BookingResponse> bookings(Long userId,Long placeId,BookingStatus status,LocalDate from,LocalDate to,Pageable p) {
        if(from!=null && to!=null && to.isBefore(from)) throw new BusinessException("dateTo must not precede dateFrom");
        Specification<Booking> spec=(root,q,cb) -> {
            List<Predicate> f=new ArrayList<>();
            if(userId!=null)f.add(cb.equal(root.get("user").get("id"),userId));
            if(placeId!=null)f.add(cb.equal(root.get("place").get("id"),placeId));
            if(status!=null)f.add(cb.equal(root.get("status"),status));
            if(from!=null)f.add(cb.greaterThanOrEqualTo(root.get("bookingDate"),from));
            if(to!=null)f.add(cb.lessThanOrEqualTo(root.get("bookingDate"),to));
            return cb.and(f.toArray(Predicate[]::new));
        };
        return PageResponse.from(bookings.findAll(spec,Pages.checked(p,"id","createdAt","bookingDate","status")).map(bookingMapper::toResponse));
    }
}
