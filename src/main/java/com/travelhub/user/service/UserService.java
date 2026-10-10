package com.travelhub.user.service;

import com.travelhub.common.exception.BusinessException;
import com.travelhub.common.exception.DuplicateResourceException;
import com.travelhub.common.exception.ResourceNotFoundException;
import com.travelhub.user.dto.UserCreateRequest;
import com.travelhub.user.dto.UserResponse;
import com.travelhub.user.dto.UserUpdateRequest;
import com.travelhub.user.entity.Role;
import com.travelhub.user.entity.User;
import com.travelhub.user.mapper.UserMapper;
import com.travelhub.user.repository.RoleRepository;
import com.travelhub.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = normalizeEmail(request.email());
        checkEmailAvailable(email, -1L);
        Role role =
                roleRepository
                        .findByName("USER")
                        .orElseThrow(
                                () -> new BusinessException("Default user role is unavailable"));
        User user = new User();
        user.setEmail(email);
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new BusinessException("Password must not exceed 72 UTF-8 bytes");
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setAvatarUrl(request.avatarUrl());
        user.setRole(role);
        userRepository.saveAndFlush(user);
        log.info("Created user {}", user.getId());
        return userMapper.toResponse(user);
    }

    @PreAuthorize("hasAuthority('USER_READ') and @access.owns(#id)")
    public UserResponse get(Long id) {
        return userMapper.toResponse(require(id));
    }

    @Transactional
    @PreAuthorize("hasAuthority('USER_UPDATE') and @access.owns(#id)")
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = lock(id);
        String email = normalizeEmail(request.email());
        checkEmailAvailable(email, id);
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setAvatarUrl(request.avatarUrl());
        userRepository.flush();
        log.info("Updated user {}", id);
        return userMapper.toResponse(user);
    }

    @Transactional
    @PreAuthorize("hasAuthority('USER_DELETE') and @access.owns(#id)")
    public void delete(Long id) {
        User user = lock(id);
        if (userRepository.countReviews(id) > 0
                || userRepository.countTrips(id) > 0
                || userRepository.countBookings(id) > 0
                || userRepository.countAddresses(id) > 0
                || userRepository.countFavorites(id) > 0) {
            throw new BusinessException(
                    "Cannot delete a user with reviews, trips, bookings, addresses, or favorites");
        }
        userRepository.delete(user);
        userRepository.flush();
        log.info("Deleted user {}", id);
    }

    public User require(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public User lock(Long id) {
        return userRepository
                .findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void checkEmailAvailable(String email, Long excludedId) {
        if (userRepository.existsByEmailAndIdNot(email, excludedId)) {
            throw new DuplicateResourceException("Email is already registered");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
