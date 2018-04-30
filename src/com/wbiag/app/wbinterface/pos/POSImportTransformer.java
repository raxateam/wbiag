/*
 * Created on Dec 16, 2004
 *
 */
package com.wbiag.app.wbinterface.pos;

import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;

/** Title:         POSImportTransaction
 * Description:    imports data into the SO_RESULTS_DETAIL table
 * Copyright:      Copyright (c) 2003
 * Company:        Workbrain Inc
 * @author         Kevin Tsoi
 * @version 1.0
 */
public class POSImportTransformer implements ImportTransformer {

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
