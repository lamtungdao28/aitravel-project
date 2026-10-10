package com.travelhub.admin.controller;
import com.travelhub.admin.dto.*;
import com.travelhub.admin.service.RoleManagementService;
import com.travelhub.common.response.PageResponse;
import com.travelhub.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin") @RequiredArgsConstructor @Validated @Tag(name="Roles and Permissions")
public class RoleManagementController {
    private final RoleManagementService service;
    @GetMapping("/roles") public PageResponse<RoleResponse> roles(@ParameterObject Pageable p){return service.roles(p);}
    @GetMapping("/permissions") public PageResponse<PermissionResponse> permissions(@ParameterObject Pageable p){return service.permissions(p);}
    @PostMapping("/roles") public ResponseEntity<RoleResponse> role(@Valid @RequestBody RoleCreateRequest r) {
        var result=service.createRole(r);return ResponseEntity.created(URI.create("/api/v1/admin/roles/"+result.id())).body(result);
    }
    @PostMapping("/permissions") public ResponseEntity<PermissionResponse> permission(@Valid @RequestBody PermissionCreateRequest r) {
        var result=service.createPermission(r);return ResponseEntity.created(URI.create("/api/v1/admin/permissions/"+result.id())).body(result);
    }
    @PutMapping("/roles/{id}/permissions") public RoleResponse permissions(@PathVariable @Positive Long id,@Valid @RequestBody RolePermissionsRequest r) {
        return service.setPermissions(id,r);
    }
    @PutMapping("/users/{id}/role") public UserResponse role(@PathVariable @Positive Long id,@Valid @RequestBody UserRoleRequest r) {
        return service.assignRole(id,r);
    }
}
