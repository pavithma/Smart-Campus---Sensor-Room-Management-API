package com.westminster.smartcampus.exception;

public class LinkedResourceNotFoundException extends RuntimeException {

    private final String fieldName;
    private final String value;

    public LinkedResourceNotFoundException(String fieldName, String value) {
        super("Linked resource not found — " + fieldName + ": " + value);
        this.fieldName = fieldName;
        this.value = value;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getValue() {
        return value;
    }
}
