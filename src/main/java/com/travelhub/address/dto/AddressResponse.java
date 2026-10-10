package com.travelhub.address.dto;
import java.time.Instant;
public record AddressResponse(Long id, Long userId, String recipientName, String phone, String addressLine,
        String city, String state, String country, String postalCode, boolean defaultAddress,
        Instant createdAt, Instant updatedAt) {}
