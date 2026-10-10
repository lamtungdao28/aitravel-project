package com.travelhub.address.mapper;

import com.travelhub.address.dto.AddressResponse;
import com.travelhub.address.entity.Address;

import org.springframework.stereotype.Component;

@Component
public class AddressMapper {
    public AddressResponse toResponse(Address a) {
        return new AddressResponse(
                a.getId(),
                a.getUser().getId(),
                a.getRecipientName(),
                a.getPhone(),
                a.getAddressLine(),
                a.getCity(),
                a.getState(),
                a.getCountry(),
                a.getPostalCode(),
                a.isDefaultAddress(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
