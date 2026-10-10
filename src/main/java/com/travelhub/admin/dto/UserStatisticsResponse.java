package com.travelhub.admin.dto;

import java.util.Map;

public record UserStatisticsResponse(
        long totalUsers, long newUsersLast30Days, Map<String, Long> byRole) {}
