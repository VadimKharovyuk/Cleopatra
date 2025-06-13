package com.example.cleopatra.enums;


import lombok.Getter;

@Getter
public enum PostVisibility {
    PUBLIC("Виден всем"),         // Виден всем
    FRIENDS("Только друзьям"),    // Только друзьям/подписчикам
    PRIVATE("Только мне");        // Только владельцу

    private final String displayName;

    PostVisibility(String displayName) {
        this.displayName = displayName;
    }

}
