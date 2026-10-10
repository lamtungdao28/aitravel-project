package com.travelhub.common.util;

import com.travelhub.common.exception.BusinessException;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class Pages {
    private Pages() {}

    public static Pageable checked(Pageable pageable, String... allowedSorts) {
        Set<String> allowed = Set.of(allowedSorts);
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new BusinessException("Unsupported sort field: " + order.getProperty());
            }
        }
        Sort sort = pageable.getSort();
        if (allowed.contains("id") && sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id"));
        }
        return PageRequest.of(
                pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), sort);
    }

    public static String contains(String text) {
        return "%"
                + text.trim()
                        .toLowerCase(java.util.Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_")
                + "%";
    }
}
