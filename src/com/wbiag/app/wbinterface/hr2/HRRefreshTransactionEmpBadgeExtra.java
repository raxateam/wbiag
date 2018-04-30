package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.wbinterface.hr2.handlers.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.tool.security.*;
/**
 * Customization for extra employee badge attributes
 *
 **/
public class HRRefreshTransactionEmpBadgeExtra extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionEmpBadgeExtra.class);

    private static final int EMP_BDG_EXTRA = 90;
    private static final String EMPBDG_FLAG_PREFIX = "EMPBDG_FLAG";
    private static final String EMPBDG_UDF_PREFIX = "EMPBDG_UDF";
    private static final String EMPBDG_SEQ_NO = "EMPBDG_SEQ_NO";

    private static List eligibleEmpBadgeFlds = new ArrayList();

    private int[] custColInds = new int[] {EMP_BDG_EXTRA};
    private DBConnection conn = null;

    static {
        for (int i = 1; i <= 5; i++) {
            eligibleEmpBadgeFlds.add(EMPBDG_FLAG_PREFIX + i);
            eligibleEmpBadgeFlds.add(EMPBDG_UDF_PREFIX + i);
            eligibleEmpBadgeFlds.add(EMPBDG_SEQ_NO);
        }
    }

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
        if (custColInds == null || custColInds.length == 0) {
            throw new WBInterfaceException ("Custom column index not supplied");
        }
        this.conn = conn;
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            try {
                processEmpBdgExtra(data);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error when processing extra employee badge fields" , ex);}
                data.error(ex.getMessage() );
            }
        }
    }

    protected void processEmpBdgExtra( HRRefreshTransactionData data ) throws Exception{
        String empBdgExtra = data.getImportData().getField(custColInds[0]);
        if (StringHelper.isEmpty(empBdgExtra)) {
            if (logger.isDebugEnabled()) logger.debug("Employee badge extra was not supplied");
            return;
        }
        EmployeeBadgeData bdgData = data.getHRRefreshData().getEmployeeBadgeData() ;
        if (bdgData == null || StringHelper.isEmpty(bdgData.getEmpbdgBadgeNumber())) {
            if (logger.isDebugEnabled()) logger.debug("Employee badge extra will not be applied because employee badge was not supplied");
            return;
        }
        StringBuffer sb = new StringBuffer(200);
        sb.append("UPDATE employee_badge SET ");
        int cnt = 0;
        List vals = StringHelper.detokenizeStringAsNameValueList(empBdgExtra,
            "~", "=", true);
        Iterator iter = vals.iterator();
        while (iter.hasNext()) {
            NameValue item = (NameValue)iter.next();
            if (StringHelper.isEmpty(item.getName())) {
                continue;
            }
            if (!eligibleEmpBadgeFlds.contains(item.getName())) {
                throw new WBInterfaceException ("Field name not supported :" + item.getName());
            }
            sb.append(cnt++ > 0 ? " , " : "");
            sb.append(item.getName()).append(" = ? ");
            bdgData.setField(item.getName(), item.getValue() );
        }
        if (cnt >0) {
            sb.append(" WHERE empbdg_badge_number = ?");
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(sb.toString());
                cnt = 1;
                Iterator iter2 = vals.iterator();
                while (iter2.hasNext()) {
                    NameValue item = (NameValue) iter2.next();
                    ps.setString(cnt++, item.getValue());
                }
                ps.setString(cnt++, bdgData.getEmpbdgBadgeNumber());
                int upd = ps.executeUpdate();
                if (logger.isDebugEnabled())
                    logger.debug("Updated :" + upd + " records for :" +
                                 bdgData.getEmpbdgBadgeNumber());
            }
            finally {
                if (ps != null)
                    ps.close();
            }
        }


    }



}