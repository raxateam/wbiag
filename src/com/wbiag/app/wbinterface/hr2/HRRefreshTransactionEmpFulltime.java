package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.sql.*;
import com.workbrain.util.StringHelper;
import com.workbrain.tool.overrides.*;

/**
 * Customization for EMPLOYEE.emp_fulltime
 *
 **/
public class HRRefreshTransactionEmpFulltime extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionEmpFulltime.class);

    private static final int EMPFULLTIME_COL = 90;

    private int[] custColInds = new int[] {EMPFULLTIME_COL};
    private DBConnection conn = null;

    /**
     * Sets custom column indexes to be used by the customization. Indexes start at 0
     * @param inds
     */
    public void setCustomColInds(int[] inds) {
        custColInds = inds;
    }

    /**
     * Override this class to customize before process batch events.
     * At this time, all interface data has been converted to <code>HRRefreshTransactionData</code>
     * objects but not processed yet. All related employee data have been loaded
     * and is available through <code>HRRefreshCache</code>.
     *
     * @param conn              DBConnection
     * @param hrRefreshDataList List of <code>HRRefreshTransactionData</code>
     * @param process           HRRefreshProcessor
     * @throws Exception
     */
    public void preProcessBatch(DBConnection conn,
                                   List hrRefreshTransactionDataList,
                                   HRRefreshProcessor process) throws Exception {
        if (hrRefreshTransactionDataList == null || hrRefreshTransactionDataList.size() == 0) {
                return;
        }
        if (custColInds == null || custColInds.length != 1) {
            throw new WBInterfaceException ("Custom column index not supplied or too many, must be 1");
        }
        this.conn = conn;
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            processEmpFulltimePre(data , conn , process);

        }
    }

    protected void processEmpFulltimePre(HRRefreshTransactionData data,
                               DBConnection c,
                               HRRefreshProcessor process)   throws SQLException, WBInterfaceException {
        // *** only do this if EMPFULLTIME_COL is not blank in the file
        String empFTVal = data.getImportData().getField(custColInds[0]);
        if (StringHelper.isEmpty(empFTVal)) {
            return;
        }
        empFTVal = empFTVal.trim().toUpperCase();
        if (empFTVal.length() > 1
            || (!"Y".equals(empFTVal) && !"N".equals(empFTVal))) {
            data.error("Full time value must be Y or N");
            return;
        }
        data.getHRRefreshData().getEmployeeData().setEmpFulltime(empFTVal);
    }


}