package com.example.cleopatra.enums;



import lombok.Getter;

@Getter
public enum DeviceType {
    DESKTOP("Десктоп"),
    MOBILE("Мобильный"),
    TABLET("Планшет"),
    UNKNOWN("Неизвестно");

    private final String displayName;

    DeviceType(String displayName) {
        this.displayName = displayName;
    }
}