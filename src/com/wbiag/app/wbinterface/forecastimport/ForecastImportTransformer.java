package com.wbiag.app.wbinterface.forecastimport;

import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;

/**
 * ForecastImportTransformer.java
 *
 *@author     Neshan Kumar
 *@created    April 01, 2006
 */
public class ForecastImportTransformer implements ImportTransformer {
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ForecastImportTransformer.class);
	
    public void transform( DBConnection conn, ImportData data, DelimitedInputStream is )
            throws ImportTransformerException {
        for( int ii = 1; ii < is.getColumnCount() + 1; ii++ ) {
            addString( ii, is, data );
        }
        data.setRecNum( is.getColumnCount() );
    }

    private void addString( int col, DelimitedInputStream is, ImportData data ) {
        String inStr = is.getString( col ).trim();
        data.setField( col - 1, inStr );
    }
}
