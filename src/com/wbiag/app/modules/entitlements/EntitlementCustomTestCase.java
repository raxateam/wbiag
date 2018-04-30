package com.wbiag.app.modules.entitlements ;

import java.io.*;
import java.util.*;
import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.test.*;
import com.workbrain.util.*;
import junit.framework.*;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
/**
 * Common Test Case Methods for Entitlement Custom Tests.
 */
public class EntitlementCustomTestCase extends TATestCase {

    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EntitlementCustomTestCase.class);


    public EntitlementCustomTestCase(String testName) throws Exception {
        super(testName);
    }

    public int createEntPolicy(String polName , Date start, Date end) throws Exception {
        EntPolicyData entPolData = new EntPolicyData();
        int polId = getConnection().getDBSequence("seq_entpol_id").getNextValue();
        entPolData.setEntpolId(polId);
        entPolData.setEntpolName(polName);
        entPolData.setEntpolStartDate(start);
        entPolData.setEntpolEndDate(end);
        new RecordAccess(getConnection()).insertRecordData(entPolData,
            "ENT_POLICY");
        return polId;
    }

    public int createEntitlement(String entName ,
                                  String applyOnUnit,
                                  String applyOnValue,
                                  Date start,
                                  Date end) throws Exception {
        EntitlementData entData = new EntitlementData();
        int entId = getConnection().getDBSequence("seq_ent_id").getNextValue();
        entData.setEntId(entId);
        entData.setEntName(entName);
        entData.setEntApplyOnUnit(applyOnUnit);
        entData.setEntApplyOnValue(applyOnValue);
        entData.setEntStartDate(start);
        entData.setEntEndDate(end);
        new EntitlementAccess(getConnection()).insertRecordData(entData,
            "ENT_ENTITLEMENT");

        return entId;
    }

    public int createEntPolicyEntitlement(int entId,
                                          int entPolId,
                                          Date start,
                                          Date end) throws Exception {
        int entPolEntId = getConnection().getDBSequence("seq_entpolent_id").getNextValue();
        PreparedStatement ps = null;
        try {
            String sql = "INSERT INTO ent_policy_entitlement (entpolent_id, ent_id, entpol_id,entpolent_start_date,entpolent_end_date) VALUES(?,?,?,?,?)";
            ps = getConnection().prepareStatement(sql);
            ps.setInt(1, entPolEntId);
            ps.setInt(2, entId);
            ps.setInt(3, entPolId);
            ps.setTimestamp(4, new Timestamp(start.getTime()));
            ps.setTimestamp(5, new Timestamp(end.getTime()));
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        return entPolEntId;
    }

    public EntDetailData getDefaultEntDetail(int entId) throws Exception {
        EntDetailData det = new EntDetailData();
        det.setEntdetId(getConnection().getDBSequence("seq_entdet_id").getNextValue());
        det.setEntId(entId);
        det.setEntdetPriority(1);
        det.setEntdetAction(EntitlementProcessor.DEFAULT_TEST_DETAIL);
        det.setEntdetRatio(EntitlementProcessor.DEFAULT_TEST_DETAIL);
        det.setEntdetCondition(EntitlementProcessor.DEFAULT_TEST_DETAIL);
        //det.setEntdetOnMaxOut(EntitlementProcessor.DEFAULT_TEST_DETAIL);
        //det.setEntdetOnMinOut(EntitlementProcessor.DEFAULT_TEST_DETAIL);
        det.setEntdetMinSenValue(0);
        det.setEntdetMinSenUnits(EntitlementData.APPLY_ON_UNIT_YEAR);
        det.setEntdetMaxSenValue(100);
        det.setEntdetMaxSenUnits(EntitlementData.APPLY_ON_UNIT_YEAR);
        return det;
    }

    public void associatePolicyWithEmployee(int empId, int polId) throws Exception{
        EntEmpPolicyData pol = new EntEmpPolicyData();
        pol.setEmpId(empId);
        pol.setEntemppolStartDate(DateHelper.DATE_1900);
        pol.setEntemppolEndDate(DateHelper.DATE_3000);
        pol.setEntemppolEnabled("Y");
        pol.setEntemppolPriority(1);
        pol.setEntpolId(polId);
        new EntEmpPolicyAccess(getConnection()).insert(pol);

    }

    public void createEntDetail(EntDetailData det) {
        new EntDetailAccess(getConnection()).insertRecordData(det, "ENT_DETAIL");
    }

}
