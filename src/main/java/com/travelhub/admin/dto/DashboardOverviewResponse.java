package com.travelhub.admin.dto;

public record DashboardOverviewResponse(
        long totalUsers,
        long totalDestinations,
        long totalPlaces,
        long totalReviews,
        long totalTrips,
        long totalBookings,
        long pendingBookings,
        long confirmedBookings,
        long cancelledBookings) {}
