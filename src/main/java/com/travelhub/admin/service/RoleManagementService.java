package com.travelhub.admin.service;

import com.travelhub.admin.dto.*;
import com.travelhub.common.exception.*;
import com.travelhub.common.response.PageResponse;
import com.travelhub.common.util.Pages;
import com.travelhub.user.dto.UserResponse;
import com.travelhub.user.entity.*;
import com.travelhub.user.mapper.UserMapper;
import com.travelhub.user.repository.*;
import com.travelhub.user.service.UserService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class RoleManagementService {
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final RolePermissionRepository links;
    private final UserService users;
    private final UserMapper userMapper;
    private final EntityManager em;

    public PageResponse<RoleResponse> roles(Pageable p) {
        return PageResponse.from(roles.findAll(Pages.checked(p, "id", "name")).map(this::response));
    }

    public PageResponse<PermissionResponse> permissions(Pageable p) {
        return PageResponse.from(
                permissions
                        .findAll(Pages.checked(p, "id", "name"))
                        .map(
                                v ->
                                        new PermissionResponse(
                                                v.getId(), v.getName(), v.getDescription())));
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        if (roles.findByName(request.name()).isPresent())
            throw new DuplicateResourceException("Role already exists");
        Role r = new Role();
        r.setName(request.name());
        r.setDescription(request.description());
        roles.saveAndFlush(r);
        return response(r);
    }

    @Transactional
    public PermissionResponse createPermission(PermissionCreateRequest request) {
        if (permissions.findByName(request.name()).isPresent())
            throw new DuplicateResourceException("Permission already exists");
        Permission p = new Permission();
        p.setName(request.name());
        p.setDescription(request.description());
        permissions.saveAndFlush(p);
        return new PermissionResponse(p.getId(), p.getName(), p.getDescription());
    }

    @Transactional
    public RoleResponse setPermissions(Long roleId, RolePermissionsRequest request) {
        Role r = em.find(Role.class, roleId, LockModeType.PESSIMISTIC_WRITE);
        if (r == null) throw new ResourceNotFoundException("Role", roleId);
        var desired = permissions.findAllById(request.permissionIds());
        if (desired.size() != request.permissionIds().size())
            throw new BusinessException("One or more permissions do not exist");
        var current = links.findByRoleId(roleId);
        Set<Long> existing = new HashSet<>();
        for (var link : current) {
            existing.add(link.getPermission().getId());
            if (!request.permissionIds().contains(link.getPermission().getId())) links.delete(link);
        }
        links.flush();
        for (var permission : desired)
            if (!existing.contains(permission.getId())) {
                RolePermission link = new RolePermission();
                link.setRole(r);
                link.setPermission(permission);
                links.save(link);
            }
        links.flush();
        return response(r);
    }

    @Transactional
    public UserResponse assignRole(Long userId, UserRoleRequest request) {
        var user = users.lock(userId);
        user.setRole(
                roles.findById(request.roleId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Role", request.roleId())));
        em.flush();
        return userMapper.toResponse(user);
    }

    private RoleResponse response(Role r) {
        return new RoleResponse(
                r.getId(),
                r.getName(),
                r.getDescription(),
                links.findPermissionNames(r.getId()).stream().sorted().toList());
    }
}
