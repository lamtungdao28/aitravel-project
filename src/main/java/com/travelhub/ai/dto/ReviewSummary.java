package com.travelhub.ai.dto;

import java.util.List;

public record ReviewSummary(
        String summary, List<String> positivePoints, List<String> negativePoints) {}
