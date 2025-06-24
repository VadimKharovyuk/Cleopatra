package com.example.cleopatra.ExistsException;


public class ImageUploadException extends RuntimeException {
    public ImageUploadException(String message, Exception e) {
        super(message);

    }

}
