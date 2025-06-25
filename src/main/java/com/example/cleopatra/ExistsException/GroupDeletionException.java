package com.example.cleopatra.ExistsException;

public class GroupDeletionException extends RuntimeException {
    public GroupDeletionException(String message, Exception e) {
        super(message);
    }
}
