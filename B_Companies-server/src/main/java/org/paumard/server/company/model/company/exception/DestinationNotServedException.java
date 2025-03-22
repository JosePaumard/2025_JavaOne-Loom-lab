package org.paumard.server.company.model.company.exception;

public class DestinationNotServedException extends RuntimeException {
    public DestinationNotServedException(String message) {
        super(message);
    }
}