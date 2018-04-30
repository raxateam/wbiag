package com.wbiag.tool.overrides;

import java.util.*;

import org.apache.log4j.*;

import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;

import java.sql.Timestamp;
import com.workbrain.app.ta.model.*;

public class WBIAGInsertWorkSummaryBulkOverride
    extends InsertWorkSummaryBulkOverride {
    //private static final Logger logger = Logger.getLogger(WBIAGInsertWorkSummaryBulkOverride.class);

    private static List supportedOvrTokensExtended = new ArrayList();

    private static final String OVR_TOKEN_WRKS_AUTHORIZED = "WRKS_AUTHORIZED";
    private static final String OVR_TOKEN_WRKS_FLAG1 = "WRKS_FLAG1";
    private static final String OVR_TOKEN_WRKS_FLAG2 = "WRKS_FLAG2";
    private static final String OVR_TOKEN_WRKS_FLAG3 = "WRKS_FLAG3";
    private static final String OVR_TOKEN_WRKS_FLAG4 = "WRKS_FLAG4";
    private static final String OVR_TOKEN_WRKS_FLAG5 = "WRKS_FLAG5";

    static {
        supportedOvrTokensExtended.add(OVR_TOKEN_WRKS_AUTHORIZED);
        supportedOvrTokensExtended.add(OVR_TOKEN_WRKS_FLAG1);
        supportedOvrTokensExtended.add(OVR_TOKEN_WRKS_FLAG2);
        supportedOvrTokensExtended.add(OVR_TOKEN_WRKS_FLAG3);
        supportedOvrTokensExtended.add(OVR_TOKEN_WRKS_FLAG4);
        supportedOvrTokensExtended.add(OVR_TOKEN_WRKS_FLAG5);
    }

    public WBIAGInsertWorkSummaryBulkOverride(DBConnection conn) {
        super(conn);
    }

    public BulkStatement getUpdatePreparedStatement(DBConnection conn) throws
        OverrideException {

        if (!isOverrideTypeEligible()) {
            throw new OverrideException(
                "Override type not eligible for InsertWorkSummaryBulkOverride : " +
                getOvrType());
        }

        List updateSqlParamValues = null;

        StringBuffer sql = new StringBuffer("UPDATE work_summary SET ");
        List odList = executeReturningList(conn);
        // *** for work summary overrides, only expect one override data, otw no processing
        if (odList == null || odList.size() == 0) {
            return null;
        }
        OverrideData od = (OverrideData) odList.get(0);
        List ovrTokens = od.getNewOverrides();
        Iterator iter = ovrTokens.iterator();
        boolean anySetDone = false;
        while (iter.hasNext()) {
            OverrideData.OverrideToken token = (OverrideData.OverrideToken)
                iter.next();
            String item = token.getName();
            if (!isOverrideTokenSupported(item)) {
                throw new OverrideException("Override token : " + item +
                                            " is not supported");
            }
            if (item.equals(OVR_TOKEN_WRKS_AUTHORIZED)) {
                String val = token.getValue();
                sql.append(" wrks_authorized = ? ");
                sql.append(" , wrks_auth_by = ? ");
                sql.append(" , wrks_auth_date = ? ");
                updateSqlParamValues = new ArrayList();
                updateSqlParamValues.add(val);
                updateSqlParamValues.add(od.getWbuNameActual());
                updateSqlParamValues.add(new Timestamp(od.getOvrCreateDate().
                    getTime()));
                anySetDone = true;
            }
            else if (item.equals(OVR_TOKEN_WRKS_FLAG1)) {
                String val = token.getValue();
                sql.append(" wrks_flag1 = ? ");
                updateSqlParamValues = new ArrayList();
                updateSqlParamValues.add(val);
                anySetDone = true;
            }
            else if (item.equals(OVR_TOKEN_WRKS_FLAG2)) {
                String val = token.getValue();
                sql.append(" wrks_flag2 = ? ");
                updateSqlParamValues = new ArrayList();
                updateSqlParamValues.add(val);
                anySetDone = true;
            }
            else if (item.equals(OVR_TOKEN_WRKS_FLAG3)) {
                String val = token.getValue();
                sql.append(" wrks_flag3 = ? ");
                updateSqlParamValues = new ArrayList();
                updateSqlParamValues.add(val);
                anySetDone = true;
            }
            else if (item.equals(OVR_TOKEN_WRKS_FLAG4)) {
                String val = token.getValue();
                sql.append(" wrks_flag4 = ? ");
                updateSqlParamValues = new ArrayList();
                updateSqlParamValues.add(val);
                anySetDone = true;
            }
            else if (item.equals(OVR_TOKEN_WRKS_FLAG5)) {
                String val = token.getValue();
                sql.append(" wrks_flag5 = ? ");
                updateSqlParamValues = new ArrayList();
                updateSqlParamValues.add(val);
                anySetDone = true;
            }
        }
        if (anySetDone) {
            sql.append(" WHERE emp_id = ? AND wrks_work_date = ? ");
            updateSqlParamValues.add(new Integer(getEmpId()));
            updateSqlParamValues.add(new Timestamp(od.getOvrStartDate().getTime()));
        }
        else {
            sql = null;
        }
        return sql == null || sql.length() == 0
            ? null
            : new BulkStatement(sql.toString(), updateSqlParamValues);

    }

    public boolean isOverrideTokenSupported(String tokenName) {
        return supportedOvrTokensExtended != null &&
            supportedOvrTokensExtended.contains(tokenName);
    }

}
