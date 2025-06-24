package com.example.cleopatra.ExistsException;

public class GroupCreationException extends RuntimeException {
    public GroupCreationException(String message, Exception e) {
        super(message);
    }
    }
