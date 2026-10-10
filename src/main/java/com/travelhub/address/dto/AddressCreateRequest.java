package com.travelhub.address.dto;

import jakarta.validation.constraints.*;

public record AddressCreateRequest(
        @NotBlank @Size(max = 150) String recipientName,
        @NotBlank @Size(max = 40) @Pattern(regexp = "[+0-9 ()-]{5,40}") String phone,
        @NotBlank @Size(max = 500) String addressLine,
        @Size(max = 120) String city,
        @Size(max = 120) String state,
        @Size(max = 120) String country,
        @Size(max = 30) String postalCode,
        boolean defaultAddress) {}
