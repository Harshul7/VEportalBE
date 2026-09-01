package org.example.veportal.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException resource(String resourceName, Object id) {
        return new NotFoundException(resourceName + " not found: " + id);
    }
}
