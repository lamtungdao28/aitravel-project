package com.travelhub.booking.mapper;

import com.travelhub.booking.dto.BookingResponse;
import com.travelhub.booking.entity.Booking;

import org.springframework.stereotype.Component;

@Component
public class BookingMapper {
    public BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getUser().getId(),
                b.getPlace().getId(),
                b.getPlace().getName(),
                b.getBookingDate(),
                b.getBookingTime(),
                b.getNumberOfPeople(),
                b.getStatus(),
                b.getNote(),
                b.getCreatedAt(),
                b.getUpdatedAt());
    }
}
