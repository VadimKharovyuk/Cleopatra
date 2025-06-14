package com.example.cleopatra.maper;

import com.example.cleopatra.dto.BirthdayUser.BirthdayUserCardDto;
import com.example.cleopatra.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BirthdayUserMapper {

    public BirthdayUserCardDto toDto(User user) {
        if (user == null) {
            return null;
        }

        BirthdayUserCardDto dto = new BirthdayUserCardDto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setImageUrl(user.getImageUrl());
        dto.setBirthDate(user.getBirthDate());

        // Вычисляем возраст
        if (user.getBirthDate() != null) {
            dto.setAge(calculateAge(user.getBirthDate()));
            dto.setBirthdayType(determineBirthdayType(user.getBirthDate()));
        }

        return dto;
    }

    public List<BirthdayUserCardDto> toDtoList(List<User> users) {
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private Integer calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private String determineBirthdayType(LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        LocalDate thisBirthday = birthDate.withYear(today.getYear());

        // Если день рождения уже прошел в этом году, берем следующий год
        if (thisBirthday.isBefore(today)) {
            thisBirthday = thisBirthday.plusYears(1);
        }

        long daysUntilBirthday = ChronoUnit.DAYS.between(today, thisBirthday);

        if (daysUntilBirthday == 0) {
            return "today";
        } else if (daysUntilBirthday == 1) {
            return "tomorrow";
        } else if (daysUntilBirthday <= 7) {
            return "this_week";
        } else if (daysUntilBirthday <= 30) {
            return "this_month";
        } else {
            return "none";
        }
    }
}