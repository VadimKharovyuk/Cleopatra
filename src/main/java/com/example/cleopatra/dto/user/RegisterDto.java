package com.example.cleopatra.dto.user;

import com.example.cleopatra.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDto {
    private String email;
    private String password;
    private String confirmPassword; // для проверки
    private Gender gender;
}