package com.wbiag.app.wbinterface.hr2;

import com.workbrain.util.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.ImportData;
import com.workbrain.security.validator.AbstractValidator;
import com.workbrain.sql.*;
import java.text.*;

/**
 * Transformer for HR Refresh data processing.
 * Massage the data that will go into wbint_import here
 * 
 * This example uppercases all calcgrp,paygrp, shiftpat names
 *
 **/

public class CustomHRFileTransformer extends AbstractImportTransformer{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CustomHRFileTransformer.class);

    private static final int COLUMN_PAYGRP_NAME = 10;
    private static final int COLUMN_CALCGRP_NAME = 8;    
    private static final int COLUMN_SHFTPAT_NAME = 7;    
    private static final int COLUMN_LAST_NAME = 4;
    private static final int COLUMN_FIRST_NAME = 5;
    private static final int COLUMN_EMP_FLAG = 18;

    public void transform( DBConnection conn, ImportData data, DelimitedInputStream is )
            throws ImportTransformerException {
        
        /* TT57995 the apostrophe character has to be accepted in first and last names regardless of IV state.
         * Get validator from DelimitedInputStream and if it is not null, get its set of restricted characters,
         * remove the apostrophe from it and use it for validation of employee first and last names.
         */
        AbstractValidator validator = is.getInputValidator();

        String illegalChars = null;
        String nameIllegalChars = null;

        if (validator != null) {
            illegalChars = validator.getIllegalChars();
        }
        if (illegalChars != null && illegalChars.length() > 0) {
            nameIllegalChars = illegalChars.replaceAll("'", "");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("transform: validator=" + validator);
            logger.debug("transform: illegalChars=" + illegalChars);
            logger.debug("transform: nameIllegalChars=" + nameIllegalChars);
        }
        for (int ii = 1, columnCount = is.getColumnCount() + 1; ii < columnCount; ii++) {
            switch (ii) {
            case COLUMN_FIRST_NAME:
            case COLUMN_LAST_NAME:
                if (validator != null) {
                    validator.setIllegalChars(nameIllegalChars);
                    addString(ii, is, data, true);
                    validator.setIllegalChars(illegalChars);
                } else {
                    addString(ii, is, data, true);
                }
                break;
            case COLUMN_EMP_FLAG:            	
                addString(ii, is, data, false);     // space character has a meaning in this field, do not trim
                break;
            case COLUMN_CALCGRP_NAME:
            case COLUMN_PAYGRP_NAME:                
            case COLUMN_SHFTPAT_NAME:
                addString(ii, is, data, true, true);
                break;                
            default:
                addString(ii, is, data, true);
            }
        }
    }
    
	    protected void addString( int col, DelimitedInputStream is, ImportData data ,
	            boolean trims, boolean uppercases) {
	    	String inStr = is.getString( col );
	    	if (trims && inStr != null) inStr = inStr.trim();
	    	if (uppercases && inStr != null) inStr = inStr.toUpperCase();	    	
	    	data.setField( col - 1, inStr );
	    }    
}
