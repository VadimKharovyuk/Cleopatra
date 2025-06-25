package com.example.cleopatra.ExistsException;

public class GroupAlreadyDeletedException extends RuntimeException {
    public GroupAlreadyDeletedException(String message) {
        super(message);
    }
}
