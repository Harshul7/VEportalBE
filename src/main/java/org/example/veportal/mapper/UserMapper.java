package org.example.veportal.mapper;

import org.example.veportal.dto.response.UserResponse;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.util.Labels;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserAccount user) {
        return new UserResponse(
                user.getFullName(),
                user.getEmail(),
                Labels.of(user.getRole()),
                user.getStaffCode(),
                user.getDepartment(),
                Labels.of(user.getStatus()),
                user.isMustChangePassword()
        );
    }
}
