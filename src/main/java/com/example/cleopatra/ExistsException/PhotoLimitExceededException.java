package com.example.cleopatra.ExistsException;

public class PhotoLimitExceededException extends RuntimeException {
    public PhotoLimitExceededException(String message) {
        super(message);
    }
}
