package com.example.springai.aoai.exception;

/**
 * Exception to be thrown when receiving a 429 status code from a remote API
 */
public class TooManyRequestsException extends RuntimeException {

    /**
     * Constructor
     *
     * @param message message
     */
    public TooManyRequestsException(String message) {
        super(message);
    }
}
