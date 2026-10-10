package com.travelhub.admin.dto;
import com.travelhub.booking.entity.BookingStatus;
import java.util.Map;
public record BookingStatisticsResponse(long totalBookings,Map<BookingStatus,Long> byStatus) {}
