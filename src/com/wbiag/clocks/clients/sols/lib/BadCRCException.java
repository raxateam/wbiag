package com.wbiag.clocks.clients.sols.lib;

/**
 * Exception generated when a string fails the CRC check.
 * @author Octavian Tarcea
 */
public class BadCRCException extends Exception{

    /**
     * Constructs a BadCRCException object.
     */
    public BadCRCException() {
        super();
    }

    /**
     * Constructs a BadCRCException object and sets its message to be the passed in string.
     * @param message The message for this exception.
     */
    public BadCRCException(String message) {
        super(message);
    }

}