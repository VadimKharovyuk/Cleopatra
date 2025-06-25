package com.example.cleopatra.ExistsException;

public class GroupAccessDeniedException extends RuntimeException {
    public GroupAccessDeniedException(String message) {
        super(message);
    }
}
