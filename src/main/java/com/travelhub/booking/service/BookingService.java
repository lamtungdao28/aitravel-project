package com.travelhub.booking.service;
import com.travelhub.booking.dto.*;
import com.travelhub.booking.entity.*;
import com.travelhub.booking.mapper.BookingMapper;
import com.travelhub.booking.repository.BookingRepository;
import com.travelhub.common.exception.*;
import com.travelhub.common.response.PageResponse;
import com.travelhub.common.util.Pages;
import com.travelhub.place.entity.Place;
import com.travelhub.place.service.PlaceService;
import com.travelhub.user.service.UserService;
import java.time.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Slf4j @Transactional(readOnly=true)
public class BookingService {
    private final BookingRepository repository;
    private final BookingMapper mapper;
    private final UserService users;
    private final PlaceService places;
    private final Clock clock;

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('BOOKING_CREATE') and @access.owns(#r.userId())")
    public BookingResponse create(BookingCreateRequest r) {
        Place p=places.lock(r.placeId());
        if(!p.isBookingEnabled()) throw new BusinessException("This place does not accept bookings");
        Booking b=new Booking(); b.setPlace(p); b.setUser(users.require(r.userId()));
        apply(b,r.bookingDate(),r.bookingTime(),r.numberOfPeople(),r.note());
        var saved=repository.saveAndFlush(b);
        log.info("Created booking {} with status PENDING",saved.getId());
        return mapper.toResponse(saved);
    }
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('BOOKING_READ') and @access.owns(@bookingService.ownerId(#id))")
    public BookingResponse get(Long id) { return mapper.toResponse(require(id)); }
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('BOOKING_READ') and @access.owns(#userId)")
    public PageResponse<BookingResponse> list(Long userId,Pageable p) {
        users.require(userId);
        return PageResponse.from(repository.findByUserId(userId,Pages.checked(p,"id","bookingDate","bookingTime","createdAt","status")).map(mapper::toResponse));
    }
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('BOOKING_UPDATE') and @access.owns(@bookingService.ownerId(#id))")
    public BookingResponse update(Long id,BookingUpdateRequest r) {
        Booking b=lock(id);
        if(b.getStatus()!=BookingStatus.PENDING) throw new BusinessException("Only pending bookings can be edited");
        apply(b,r.bookingDate(),r.bookingTime(),r.numberOfPeople(),r.note());
        repository.flush(); return mapper.toResponse(b);
    }
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') and hasAuthority('BOOKING_UPDATE')")
    public BookingResponse confirm(Long id) { return transition(id,BookingStatus.CONFIRMED); }
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('BOOKING_UPDATE') and @access.owns(@bookingService.ownerId(#id))")
    public BookingResponse cancel(Long id) { return transition(id,BookingStatus.CANCELLED); }
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') and hasAuthority('BOOKING_UPDATE')")
    public BookingResponse complete(Long id) { return transition(id,BookingStatus.COMPLETED); }
    public Long ownerId(Long id) {
        return repository.findUserId(id).orElseThrow(() -> new ResourceNotFoundException("Booking",id));
    }
    private BookingResponse transition(Long id,BookingStatus next) {
        Booking b=lock(id);
        boolean allowed=switch(b.getStatus()) {
            case PENDING -> next==BookingStatus.CONFIRMED || next==BookingStatus.CANCELLED;
            case CONFIRMED -> next==BookingStatus.COMPLETED || next==BookingStatus.CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };
        if(!allowed) throw new BusinessException("Cannot transition booking from "+b.getStatus()+" to "+next);
        log.info("Booking {} transition {} to {}",id,b.getStatus(),next);
        b.setStatus(next); repository.flush(); return mapper.toResponse(b);
    }
    private Booking require(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking",id)); }
    private Booking lock(Long id) { return repository.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException("Booking",id)); }
    private void apply(Booking b,LocalDate date,LocalTime time,int people,String note) {
        String timezone=b.getPlace().getDestination().getTimezone();
        ZoneId zone=timezone==null ? ZoneOffset.UTC : ZoneId.of(timezone);
        if(date.isBefore(LocalDate.now(clock.withZone(zone)))) throw new BusinessException("Booking date must not be in the past");
        if(people<=0) throw new BusinessException("Number of people must be positive");
        b.setBookingDate(date);b.setBookingTime(time);b.setNumberOfPeople(people);b.setNote(note);
    }
}
