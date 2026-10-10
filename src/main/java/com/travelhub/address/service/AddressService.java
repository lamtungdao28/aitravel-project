package com.travelhub.address.service;

import com.travelhub.address.dto.*;
import com.travelhub.address.entity.Address;
import com.travelhub.address.mapper.AddressMapper;
import com.travelhub.address.repository.AddressRepository;
import com.travelhub.common.exception.ResourceNotFoundException;
import com.travelhub.common.response.PageResponse;
import com.travelhub.common.util.Pages;
import com.travelhub.user.entity.User;
import com.travelhub.user.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {
    private final AddressRepository repository;
    private final AddressMapper mapper;
    private final UserService users;

    @PreAuthorize("@access.owns(@addressService.ownerId(#id))")
    public AddressResponse get(Long id) {
        return mapper.toResponse(require(id));
    }

    @PreAuthorize("@access.owns(#userId)")
    public PageResponse<AddressResponse> list(Long userId, Pageable pageable) {
        users.require(userId);
        return PageResponse.from(
                repository
                        .findByUserId(
                                userId,
                                Pages.checked(
                                        pageable, "id", "createdAt", "city", "defaultAddress"))
                        .map(mapper::toResponse));
    }

    @Transactional
    @PreAuthorize("@access.owns(#userId)")
    public AddressResponse create(Long userId, AddressCreateRequest request) {
        User user = users.lock(userId);
        boolean makeDefault = request.defaultAddress() || !repository.existsByUserId(userId);
        if (makeDefault) clearDefault(userId);
        Address a = new Address();
        a.setUser(user);
        apply(
                a,
                request.recipientName(),
                request.phone(),
                request.addressLine(),
                request.city(),
                request.state(),
                request.country(),
                request.postalCode(),
                makeDefault);
        return mapper.toResponse(repository.saveAndFlush(a));
    }

    @Transactional
    @PreAuthorize("@access.owns(@addressService.ownerId(#id))")
    public AddressResponse update(Long id, AddressUpdateRequest request) {
        Long userId = ownerId(id);
        users.lock(userId);
        Address a = require(id);
        if (request.defaultAddress()) clearDefault(userId);
        apply(
                a,
                request.recipientName(),
                request.phone(),
                request.addressLine(),
                request.city(),
                request.state(),
                request.country(),
                request.postalCode(),
                request.defaultAddress());
        repository.flush();
        return mapper.toResponse(a);
    }

    @Transactional
    @PreAuthorize("@access.owns(@addressService.ownerId(#id))")
    public AddressResponse makeDefault(Long id) {
        Long userId = ownerId(id);
        users.lock(userId);
        Address a = require(id);
        clearDefault(userId);
        a.setDefaultAddress(true);
        repository.flush();
        return mapper.toResponse(a);
    }

    @Transactional
    @PreAuthorize("@access.owns(@addressService.ownerId(#id))")
    public void delete(Long id) {
        Long userId = ownerId(id);
        users.lock(userId);
        Address a = require(id);
        boolean wasDefault = a.isDefaultAddress();
        repository.delete(a);
        repository.flush();
        if (wasDefault)
            repository
                    .findFirstByUserIdOrderByCreatedAtAscIdAsc(userId)
                    .ifPresent(next -> next.setDefaultAddress(true));
        repository.flush();
    }

    public Long ownerId(Long id) {
        return repository
                .findUserId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", id));
    }

    private Address require(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", id));
    }

    private void clearDefault(Long userId) {
        repository
                .findByUserIdAndDefaultAddressTrue(userId)
                .forEach(a -> a.setDefaultAddress(false));
        // Flush before setting a new default so a partial unique index is never temporarily
        // violated.
        repository.flush();
    }

    private void apply(
            Address a,
            String name,
            String phone,
            String line,
            String city,
            String state,
            String country,
            String postalCode,
            boolean isDefault) {
        a.setRecipientName(name.trim());
        a.setPhone(phone.trim());
        a.setAddressLine(line.trim());
        a.setCity(city);
        a.setState(state);
        a.setCountry(country);
        a.setPostalCode(postalCode);
        a.setDefaultAddress(isDefault);
    }
}
