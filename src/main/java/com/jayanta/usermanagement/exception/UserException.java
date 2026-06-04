package com.jayanta.usermanagement.exception;


public class UserException extends RuntimeException {
    private final String code;
    private final String field;

    public UserException(String message, String code) {
        super(message);
        this.code = code;
        this.field = null;
    }

    public UserException(String message, String code, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String getCode() { return code; }
    public String getField() { return field; }
}
