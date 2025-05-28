package com.example.cleopatra.dto.user;

import com.example.cleopatra.enums.Gender;
import com.example.cleopatra.enums.Role;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class UserResponse {
    private Long id;
    private String email;
    private Role role;
    private Gender gender;
    private LocalDateTime createdAt;
}
