package com.travelhub.address.controller;
import com.travelhub.address.dto.*;
import com.travelhub.address.service.AddressService;
import com.travelhub.common.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name="Addresses")
public class AddressController {
    private final AddressService service;
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{userId}/addresses")
    public ResponseEntity<AddressResponse> create(@PathVariable @Positive Long userId, @Valid @RequestBody AddressCreateRequest request) {
        var response = service.create(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/addresses/" + response.id())).body(response);
    }
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/addresses")
    public PageResponse<AddressResponse> list(@PathVariable @Positive Long userId,
            @ParameterObject @PageableDefault(sort="createdAt") Pageable pageable) { return service.list(userId, pageable); }
    @GetMapping("/addresses/{id}")
    public AddressResponse get(@PathVariable @Positive Long id) { return service.get(id); }
    @PutMapping("/addresses/{id}")
    public AddressResponse update(@PathVariable @Positive Long id, @Valid @RequestBody AddressUpdateRequest request) {
        return service.update(id, request);
    }
    @PutMapping("/addresses/{id}/default")
    public AddressResponse makeDefault(@PathVariable @Positive Long id) { return service.makeDefault(id); }
    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id); return ResponseEntity.noContent().build();
    }
}
