package com.wbiag.app.wbinterface.hr2;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Customization for unassigned ent emp pols
 *
 **/
public class HRRefreshTransactionEndEntPols extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionEndEntPols.class);

    private static final int ENDENTPOLS_COL = 90;
    private DBConnection conn = null;
    private int[] custColInds = new int[] {ENDENTPOLS_COL};

    /**
     * Sets custom column indexes to be used by the customization. Indexes start at 0
     * @param inds
     */
    public void setCustomColInds(int[] inds) {
        custColInds = inds;
    }

    /**
     * Override this class to customize after process batch events.
     * At this time, all HRRefreshTransactionData data has been processed.
     * All processed is available through <code>HRRefreshCache</code>.
     *
     * @param conn                          DBConnection
     * @param hrRefreshTransactionDataList  List of <code>HRRefreshTransactionData</code>
     * @param process                       HRRefreshProcessor
     * @throws Exception
     */
    public void postProcessBatch(DBConnection conn,
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
            processEndEntPols(data , conn);
        }
    }

    protected void processEndEntPols(HRRefreshTransactionData data,
                               DBConnection c) throws SQLException, WBInterfaceException {
        String endEntpols = data.getImportData().getField(custColInds[0]);
        // *** no action is taken if no policies are passed
        if (StringHelper.isEmpty(endEntpols)) {
            return;
        }
        String[] polNames = StringHelper.detokenizeString(endEntpols, ",");
        IntegerList entPolIds = new IntegerList();
        for (int i = 0; i < polNames.length; i++) {
            CodeMapper cm = CodeMapper.createCodeMapper(c);
            EntPolicyData entpolData = cm.getEntPolicyByName(polNames[i]);
            if (entpolData != null) {
                entPolIds.add(entpolData.getEntpolId());
            }
            else {
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug( "Entitlement Policy :" + polNames[i] + " not found when ending unassigned policies");}
            }
        }
        if (entPolIds.size() > 0) {
            endEmpEntPol(data.getEmpId() ,  data.getOvrEndDate() , entPolIds);
        }
    }

    protected void endEmpEntPol(int empId, Date endDate, IntegerList entPolIds) throws SQLException {
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("UPDATE ent_emp_policy SET entemppol_end_date = ? ");
            sb.append(" WHERE emp_id = ? ");
            if (entPolIds != null && entPolIds.size() > 0) {
                sb.append("AND entpol_id IN (");
                for (int i = 0, k = entPolIds.size(); i < k; i++) {
                    sb.append(i > 0 ? ",?" : "?");
                }
                sb.append(")");
            }
            ps = conn.prepareStatement(sb.toString());
            ps.setTimestamp(1, new Timestamp(endDate.getTime()));
            ps.setInt(2 , empId);
            if (entPolIds != null && entPolIds.size() > 0) {
                for (int i = 0, k = entPolIds.size(); i < k; i++) {
                    ps.setInt(i + 3, entPolIds.getInt(i));
                }
            }
            int upd = ps.executeUpdate();
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug( "Deleted " + upd + " unassigned ent emp pols for empId : " + empId);}
        }
        finally {
            if (ps != null) ps.close();
        }
    }

}