package com.example.cleopatra.enums;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("Мужчина"),
    FEMALE("Женщина");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }


}
