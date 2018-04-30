package com.wbiag.sql;

import java.sql.Connection;

import com.workbrain.sql.DBConnection;

/**
 * @author bviveiros
 *
 * Methods not available in the core class.
 * Cannot extend the core class because some members are private.  Need to
 * wrap the class instead.
 */
public class DBServer {

	private com.workbrain.sql.DBServer coreDBServer = null;
	
    public DBServer(DBConnection c) {
    	coreDBServer = c.getDBServer();
    }
    
    public String getToCharTimestamp(String dateField) {
        if (coreDBServer.isDB2()) return "CHAR(" + dateField + ")";
        else if (coreDBServer.isOracle()) return "TO_CHAR(" + dateField + ", 'mm/dd/yyyy HH24:mi:ss')";
        else if (coreDBServer.isMSSQL()) return "CONVERT(VARCHAR," + dateField + ",100)";
        else return "";
    }
    
}
