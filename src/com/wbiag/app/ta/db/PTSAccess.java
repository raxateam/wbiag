package com.wbiag.app.ta.db;

import com.wbiag.app.ta.model.PTSData;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.Datetime;

/** 
 * Title:			PTS Access
 * Description:		Access class for payroll to sales table
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Apr 21, 2005
 * @author         	Kevin Tsoi
 */
public class PTSAccess extends RecordAccess
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PTSAccess.class);
 
    public static final String PTS_TABLE = "PAYROLL_TO_SALES";
    public static final String PTS_KEY = "PTS_ID";    
    public static final String PTS_SEQ = "SEQ_PTS_ID";    
    
    /**
     * Constructor
     * 
     * @param conn
     */
    public PTSAccess(DBConnection conn)
    {
        super(conn);
    }
    
    /**
     * Load by PTS id
     * 
     * @param ptsId
     * @return
     */
    public PTSData load(int ptsId)
    {
        PTSData ptsData = null;
        
        return ptsData;
    }

    /**
     * Load by department id, work date, type, and category
     * 
     * @param deptSkdGrpId
     * @param workDate
     * @param type
     * @param category
     * @return
     */
    public PTSData load(int deptSkdGrpId, Datetime workDate, String type, String category)
    {
        PTSData ptsData = null;
        
        return ptsData;               
    }
}
