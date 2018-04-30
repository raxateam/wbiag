package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Customization for Workbrain_User.wbu_active
 * @deprecated Use {@link #HRRefreshTransactionWbuExtra} that supports all Workbrain_user fields
 **/
public class HRRefreshTransactionWbuActive extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionWbuActive.class);

    private static final int WBUACTIVE_COL = 90;

    private int[] custColInds = new int[] {WBUACTIVE_COL};
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
            processWbuActivePre(data , conn , process);

        }
    }

    protected void processWbuActivePre(HRRefreshTransactionData data,
                               DBConnection c,
                               HRRefreshProcessor process)   throws SQLException, WBInterfaceException {
        // *** only do this if workbrain user data is supplied
        if (data.getHRRefreshData().getWorkbrainUserData() == null) {
            if (logger.isDebugEnabled()) logger.debug("No workbrain user data to update wbu_active");
            return;
        }
        // *** only do this if WBUACTIVE_COL is not blank in the file
        String wbuActive = data.getImportData().getField(custColInds[0]);
        if (StringHelper.isEmpty(wbuActive)) {
            return;
        }
        wbuActive = wbuActive.trim().toUpperCase();
        if (wbuActive.length() > 1
            || (!"Y".equals(wbuActive) && !"N".equals(wbuActive))) {
            data.error("wbu_active value must be Y or N");
            return;
        }
        data.getHRRefreshData().getWorkbrainUserData().setWbuActive(wbuActive);
    }


}