package com.example.reliableevents.service;

public class ProcessingException extends RuntimeException {
    public ProcessingException(String message) {
        super(message);
    }
}
