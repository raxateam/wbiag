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
 * Main exception thrown by Clock server.
 * @author akovacs
 * @version %I% %G%
 *
 */
public class ReaderServerException extends Exception {

    /**
     * Constructs a ReaderServerException object.
     * @param message String message
     */
    public ReaderServerException(String message) {
        super(message);
    }

    /**
     * Constructs a ReaderServerException object.
     */
    public ReaderServerException() {
        super("");
    }

} //end readerException
