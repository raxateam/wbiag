package com.wbiag.app.ta.db;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.*;
import com.workbrain.sql.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;

/**
 * Provides database access for the WBIAG_DATEEFF_RDR_GRP table.
 *
 */
public class EmployeeDateEffRdrgrpAccess extends RecordAccess{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmployeeDateEffRdrgrpAccess.class);
    DBConnection conn;
    public static final String WBIAG_DATEEFF_RDR_GRP_TABLE = "WBIAG_DATEEFF_RDR_GRP";
    public static final String WBIAG_DATEEFF_RDR_GRP_PRI_KEY = "wdrg_id";
    public static final String WBIAG_DATEEFF_RDR_GRP_SEQ = "seq_wdrg_id";
    
    public static final String DELETE_WBIAG_DATEEFF_RDR_GRP_SQL
    = "DELETE FROM WBIAG_DATEEFF_RDR_GRP WHERE WDRG_id = ?";
    
    public static final String SELECT_WBIAG_DATEEFF_RDR_GRP_SQL
    = "SELECT * FROM WBIAG_DATEEFF_RDR_GRP";
    
    
    public EmployeeDateEffRdrgrpAccess(DBConnection conn) {
        super(conn);
        this.conn=conn;
    }
    
    /**
     * Adds empId, rdrgrp, startdate, enddate, comments to WBIAG_DATEEFF_RDR_GRP
     * @param conn
     * @param empId
     * @param date
     * @param msg
     * @throws SQLException
     */
    public void addEmployeeEffRdrGrp(           int empId ,
            int rdrgrpId,
            Date startDate,
            Date endDate, String comments) throws SQLException {
        EmployeeDateEffRdrgrpData elrd = new  EmployeeDateEffRdrgrpData();
        elrd.setEmpId(empId);
        elrd.setWdrgRdrgrpId(rdrgrpId);
        elrd.setWdrgStartDate(startDate);
        elrd.setWdrgEndDate(endDate);
        elrd.setWdrgComments(comments);
        List elrdList = new ArrayList();
        elrdList.add(elrd);
       
        addEmployeeEffRdrGrp(elrdList);
    } 
    
    /**
     * Adds a list of <code>EmployeeDateEffRdrgrpData</code> to WBIAG_DATEEFF_RDR_GRP
     * @param conn
     * @param empDates
     * @throws SQLException
     */
    public void addEmployeeEffRdrGrp( 
            List empDates) throws SQLException {
        if (empDates == null || empDates.size() == 0) {
            return;
        }
        List empLoanDates = new ArrayList();
        Iterator iter = empDates.iterator();
        while (iter.hasNext()) {
            EmployeeDateEffRdrgrpData item = (EmployeeDateEffRdrgrpData)iter.next();
            if (item == null || item.getWdrgStartDate() == null || item.getWdrgEndDate() == null) {
                throw new RuntimeException ("EmployeeDateEffRdrgrpData/Dates cannot be null");
            }
            item.setWdrgId(conn.getDBSequence(WBIAG_DATEEFF_RDR_GRP_SEQ).getNextValue() );
            empLoanDates.add(item);
        }
       
        insertRecordData(empLoanDates , WBIAG_DATEEFF_RDR_GRP_TABLE) ;
    }
    
    /**
     * Loads all records in WBIAG_DATEEFF_RDR_GRP
     * @param conn
     * @return
     * @throws SQLException
     */
    public List loadEmployeeEffRdrGrp() throws SQLException {
        
        return loadRecordData(new EmployeeDateEffRdrgrpData(), WBIAG_DATEEFF_RDR_GRP_TABLE, "1=1");
    }
    
    /**
     * Loads all records in WBIAG_DATEEFF_RDR_GRP TABLE where
     * @param conn
     * @param where
     * @return
     * @throws SQLException
     */
    public List loadEmployeeEffRdrGrpWhere( String where) throws SQLException {
        
        return loadRecordData(new EmployeeDateEffRdrgrpData(), WBIAG_DATEEFF_RDR_GRP_TABLE, where);
    }
    
    /**
     * Deletes given list of <code>EmployeeDateEffRdrgrpData</code>
     * @param conn
     * @param list
     * @return
     * @throws SQLException
     */
    public int deleteEmployeeEffRdrGrp(List list) throws SQLException {
        int ret = 0;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(DELETE_WBIAG_DATEEFF_RDR_GRP_SQL);
            Iterator iter = list.iterator();
            while (iter.hasNext()) {
                EmployeeDateEffRdrgrpData item = (EmployeeDateEffRdrgrpData)iter.next();
                ps.setInt(1 , item.getWdrgId());
                ps.addBatch();
            }
            int upd[] = ps.executeBatch();
            ret = upd.length;
        }
        finally {
            if (ps != null) ps.close();
        }
        return ret;
    }
    
}