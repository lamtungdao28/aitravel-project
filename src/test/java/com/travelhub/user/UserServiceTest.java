package com.travelhub.user;

import com.travelhub.common.exception.BusinessException;
import com.travelhub.common.exception.DuplicateResourceException;
import com.travelhub.common.exception.ResourceNotFoundException;
import com.travelhub.user.dto.UserCreateRequest;
import com.travelhub.user.dto.UserUpdateRequest;
import com.travelhub.user.entity.Role;
import com.travelhub.user.entity.User;
import com.travelhub.user.mapper.UserMapper;
import com.travelhub.user.repository.RoleRepository;
import com.travelhub.user.repository.UserRepository;
import com.travelhub.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, roleRepository, new UserMapper(), new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4));
    }

    @Test
    void creationNormalizesEmailAndAssignsUserRole() {
        Role role = new Role();
        role.setName("USER");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        var response = service.create(new UserCreateRequest(" MEMBER@Example.Test ", "test-password", " Member ", null));
        assertThat(response.email()).isEqualTo("member@example.test");
        assertThat(response.fullName()).isEqualTo("Member");
        assertThat(response.role()).isEqualTo("USER");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getRole()).isSameAs(role);
        assertThat(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("test-password", saved.getValue().getPassword())).isTrue();
    }

    @Test
    void duplicateEmailIsRejectedBeforeInsert() {
        when(userRepository.existsByEmailAndIdNot("member@example.test", -1L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(new UserCreateRequest("MEMBER@example.test", "test-password", "Member", null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatePreservesRoleAndPassword() {
        User user = user();
        Role role = new Role();
        role.setName("ADMIN");
        user.setRole(role);
        user.setPassword("existing-password");
        when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));
        var response = service.update(1L, new UserUpdateRequest("NEW@example.test", "New Name", null));
        assertThat(response.email()).isEqualTo("new@example.test");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(user.getPassword()).isEqualTo("existing-password");
    }

    @Test
    void referencedUserCannotBeDeleted() {
        when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user()));
        when(userRepository.countAddresses(1L)).thenReturn(1L);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessException.class);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void unreferencedUserCanBeDeleted() {
        User user = user();
        when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));
        service.delete(1L);
        verify(userRepository).delete(user);
    }

    @Test
    void missingUserReturnsDomainException() {
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setEmail("member@example.test");
        user.setFullName("Member");
        return user;
    }
}
