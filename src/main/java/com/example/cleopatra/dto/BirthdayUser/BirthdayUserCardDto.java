package com.example.cleopatra.dto.BirthdayUser;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BirthdayUserCardDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private LocalDate birthDate;
    private Integer age;
    private String birthdayType;
}
