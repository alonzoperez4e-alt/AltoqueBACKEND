package com.altoque.altoque.Exception;

public class ApiServerException extends RuntimeException {
    public ApiServerException(String message) {
        super(message);
    }
}