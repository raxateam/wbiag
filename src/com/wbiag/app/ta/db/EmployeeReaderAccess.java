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
public class EmployeeReaderAccess extends RecordAccess{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmployeeDateEffRdrgrpAccess.class);
    DBConnection conn;
    public static final String EMPLOYEE_READER_GROUP_TABLE = "employee_reader_group";
    public static final String EMPLOYEE_READER_GROUP_PRI_KEY = "emprdrgrp_id";
    public static final String EMPLOYEE_READER_GROUP_SEQ = "seq_emprdrgrp_id";

    public static final String DELETE_EMPLOYEE_READER_GRP_SQL
        = "DELETE FROM employee_reader_group WHERE emprdrgrp_Id = ?";

    public static final String SELECT_EMPLOYEE_READER_GRP_SQL
        = "SELECT * FROM employee_reader_group";

   /* protected EmployeeReaderAccess() {
    }
*/
    public EmployeeReaderAccess(DBConnection conn) {
        super( conn );
    this.conn=conn;
    }

    /**
     * Adds empId, rdrgrp, startdate, enddate, comments to EMPLOYEE_READER_GROUP
     * @param conn
     * @param empId
     * @param date
     * @param msg
     * @throws SQLException
     */
    public void addEmployeeReader(         int empId ,
                                           int rdrgrpId
                                           ) throws SQLException {
        EmployeeReaderData elrd = new  EmployeeReaderData();
        elrd.setEmpId(empId);
        elrd.setRdrgrpId(rdrgrpId);
        List elrdList = new ArrayList();
        elrdList.add(elrd);
        addEmployeeReader( elrdList);
        
    }

    /**
     * Adds a list of <code>EmployeeLoanRdrgrpData</code> to EMPLOYEE_READER_GROUP
     * @param conn
     * @param empDates
     * @throws SQLException
     */
    public  void addEmployeeReader(
                                        List employeeReaderDataList) throws SQLException {
        if (employeeReaderDataList == null || employeeReaderDataList.size() == 0) {
            return;
        }
        List list = new ArrayList();
        Iterator iter = employeeReaderDataList.iterator();
        while (iter.hasNext()) {
            EmployeeReaderData item = (EmployeeReaderData)iter.next();
            
            item.setEmprdrgrpId(conn.getDBSequence(EMPLOYEE_READER_GROUP_SEQ).getNextValue() );
            list.add(item);
        }
        insertRecordData(list , EMPLOYEE_READER_GROUP_TABLE) ;
        
    }

    /**
     * Loads all records in EMPLOYEE_READER_GROUP_TABLE
     * @param conn
     * @return
     * @throws SQLException
     */
    public  List loadEmployeeReader() throws SQLException {
        return loadRecordData(new EmployeeReaderData(), EMPLOYEE_READER_GROUP_TABLE, "1=1");
    }
    
    /**
     * Loads all records in EMPLOYEE_READER_GROUP_TABLE where
     * @param conn
     * @param where
     * @return
     * @throws SQLException
     */
    public  List loadEmployeeReaderWhere(String where) throws SQLException {
        return loadRecordData(new EmployeeReaderData(), EMPLOYEE_READER_GROUP_TABLE, where);
    }

    /**
     * Deletes given list of <code>EmployeeLoanRdrgrpData</code>
     * @param conn
     * @param list
     * @return
     * @throws SQLException
     */
    public int deleteEmployeeReader(List list) throws SQLException {
        int ret = 0;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(DELETE_EMPLOYEE_READER_GRP_SQL);
            Iterator iter = list.iterator();
            while (iter.hasNext()) {
                EmployeeReaderData item = (EmployeeReaderData)iter.next();
                ps.setInt(1 , item.getEmprdrgrpId());
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