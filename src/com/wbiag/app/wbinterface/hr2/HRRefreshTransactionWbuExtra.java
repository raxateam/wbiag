package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;

import com.workbrain.app.ta.model.WorkbrainUserData;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Customization for Workbrain_User extra fields
 *
 **/
public class HRRefreshTransactionWbuExtra extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionWbuExtra.class);

    private static final int WBUEXTRA_COL = 90;

    private int[] custColInds = new int[] {WBUEXTRA_COL};
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
            processWbuExtraPre(data, conn, process);

        }
    }

    protected void processWbuExtraPre(HRRefreshTransactionData data,
                               DBConnection c,
                               HRRefreshProcessor process)   throws SQLException, WBInterfaceException {
        // *** only do this if workbrain user data is supplied
        if (data.getHRRefreshData().getWorkbrainUserData() == null) {
            if (logger.isDebugEnabled()) logger.debug("No workbrain user data to update");
            return;
        }
        // *** only do this if WBUEXTRA_COL is not blank in the file
        String wbuExtra = data.getImportData().getField(custColInds[0]);
        if (StringHelper.isEmpty(wbuExtra)) {
            return;
        }
        Map vals = StringHelper.detokenizeStringAsNameValueMap(wbuExtra, "~", "=", true);
        WorkbrainUserData user = data.getHRRefreshData().getWorkbrainUserData();
        Iterator iter = vals.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry item =  (Map.Entry)iter.next();
            String fld = (String)item.getKey() ;
            Object value = item.getValue();
            try {
                user.setField(fld, value);
            }
            catch (Exception ex) {
                throw new WBInterfaceException ("Error in updating Workbrain User Field :" + fld + " to :"  + value);
            }
        }

    }


}