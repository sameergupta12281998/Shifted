package com.porterlike.services.tracking.api;

public class NoLocationFoundException extends RuntimeException {
    public NoLocationFoundException(String message) {
        super(message);
    }
}
