package com.example.cleopatra.dto.user;

import lombok.Data;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class UpdateProfileDto {

    @Size(max = 50, message = "Имя не должно превышать 50 символов")
    private String firstName;

    @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
    private String lastName;


    @Size(max = 50, message = "Город не должна превышать 50 символов")
    private String city;
}