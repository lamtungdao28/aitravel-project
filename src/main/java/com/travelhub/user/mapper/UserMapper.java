package com.travelhub.user.mapper;

import com.travelhub.user.dto.UserResponse;
import com.travelhub.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getAvatarUrl(),
                user.getRole() == null ? null : user.getRole().getName(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
