package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Customization for EMPLOYEE.tz_id
 *
 **/
public class HRRefreshTransactionEmpTzone extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionEmpTzone.class);

    private static final int EMPTZONENAME_COL = 90;

    private Map readerGroups = null;
    private int[] custColInds = new int[] {EMPTZONENAME_COL};
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
            try {
                processEmpTZonePre(data, conn, process);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionEmpTzone." , ex);}
                data.error("Error in HRRefreshTransactionEmpTzone." + ex.getMessage() );
            }

        }
    }

    protected void processEmpTZonePre(HRRefreshTransactionData data,
                               DBConnection c,
                               HRRefreshProcessor process)   throws SQLException, WBInterfaceException {
        // *** only do this if EMPTZONENAME_COL is not blank in the file
        String empTZoneVal = data.getImportData().getField(custColInds[0]);
        if (StringHelper.isEmpty(empTZoneVal)) {
            return;
        }
        TimezoneData tzNew = CodeMapper.createCodeMapper(c).getTimeZoneByJavaName(empTZoneVal);
        if (tzNew == null) {
            data.setStatus("ERROR");
            throw new WBInterfaceException("Time zone not found : " +
                                           empTZoneVal);
        }

        // *** until TT41776 is resolved, only handle new employee here
        data.getHRRefreshData().getEmployeeData().setTzId(new Integer(tzNew.getTzId()));

    }


}