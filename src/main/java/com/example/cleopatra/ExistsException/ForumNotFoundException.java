package com.example.cleopatra.ExistsException;

public class ForumNotFoundException extends RuntimeException {
    public ForumNotFoundException(String message) {
        super(message);
    }
}
