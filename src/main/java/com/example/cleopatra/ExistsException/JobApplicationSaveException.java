package com.example.cleopatra.ExistsException;

public class JobApplicationSaveException extends RuntimeException {
    public JobApplicationSaveException(String message, Exception e) {
        super(message);
    }
}
