/**
 * <p>Title: </p>
 * <p>Created on Dec 31, 2003</p>
 * <p>Description: </p>
 * <p>Copyright: Workbrain (c) 2002</p>
 * <p>Company: Workbrain</p>
 * @author Andrei Kovacs
 * @version 1.0
 */

package com.wbiag.clocks.clients.sols.lib;

/**
 * Exception throws during initialization of objects.
 * @author akovacs
 * @version %I% %G%
 *
 */
public class InitializationException extends ReaderServerException {

    /**
     * Constructs an InitializationException object.
     * @param message String message.
     */
    public InitializationException(String message) {
        super(message);
    }

    /**
     * Constructs an InitializationException object.
     */
    public InitializationException() {
        super();
    }
    

} //end readerException
