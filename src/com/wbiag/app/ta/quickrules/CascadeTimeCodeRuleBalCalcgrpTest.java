package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.quickrules.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import junit.framework.*;
import com.wbiag.app.ta.db.*;
import com.wbiag.app.ta.model.*;
import com.wbiag.app.ta.quickrules.*;
/**
 * Test for CascadeTimeCodeRule.
 */
public class CascadeTimeCodeRuleBalCalcgrpTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CascadeTimeCodeRuleBalCalcgrpTest.class);

    public CascadeTimeCodeRuleBalCalcgrpTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CascadeTimeCodeRuleBalCalcgrpTest.class);
        return result;
    }


    /**
     * 1. Creates an LTA override for VAC code
     * 2. Checks if VAC has been converted to TRN
     * @throws Exception
     */
    public void testCascadeBelowMinimum() throws Exception {
        BalanceCalcgrpCache.getInstance().updateCacheContents(getConnection());
        TestUtil.getInstance().setVarTemp("/system/customer/customerdataevent" ,
                                          "com.wbiag.app.ta.ruleengine.CDataEventBalCalcgrp");
        TestUtil.getInstance().setVarTemp("/system/WORKBRAIN_PARAMETERS/TEST_BALANCES" ,
                                          "false");
        DBConnection c = getConnection();

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15 , vacBalId = 0;
        final int tcodeId = 6; // *** TRN
        final int tcbtId = 2; // *** decrement days
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        BalanceAccess ba = new BalanceAccess(getConnection());
        BalanceData bd = ba.load(vacBalId);
        bd.setBalMax(99999);
        bd.setBalMin(-99999);
        ba.updateRecordData(bd, "BALANCE" , "bal_id");

        IagBalanceCalcgrpData balCg = new IagBalanceCalcgrpData();
        balCg.setBalId(vacBalId);
        balCg.setCalcgrpId(getEmployeeData(empId , start).getCalcgrpId());
        balCg.setBaltypId(1);
        balCg.setIbcgBalMin(-1);
        balCg.setIbcgBalMax(100);
        balCg.setIbcgId(getConnection().getDBSequence("seq_ibcg_id").getNextValue());
        new RecordAccess(getConnection()).insertRecordData(balCg , "IAG_BALANCE_CALCGRP");

        TimeCodeAccess ta = new TimeCodeAccess(getConnection());
        TimeCodeData td = ta.load(tcodeId);
        td.setTcodeAffectsBalances("Y");
        ta.updateRecordData(td, "TIME_CODE" , "tcode_id");

        IagTimeCodeBalData tcodeBal = new IagTimeCodeBalData();
        tcodeBal.setBalId(vacBalId);
        tcodeBal.setCalcgrpId(getEmployeeData(empId , start).getCalcgrpId());
        tcodeBal.setTcodeId(tcodeId);
        tcodeBal.setTcbtId(tcbtId);
        new RecordAccess(getConnection()).insertRecordData(tcodeBal , "IAG_TIME_CODE_BAL");

        Rule rule = new CascadeTimeCodeRuleBalCalcgrp();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(CascadeTimeCodeRule.PARAM_CASCADE_FROM_TIMECODE, "VAC");
        ruleparams.addParameter(CascadeTimeCodeRule.PARAM_CASCADE_TO_TIMECODE, "TRN");
        ruleparams.addParameter(CascadeTimeCodeRule.PARAM_CASCADE_TYPE, CascadeTimeCodeRule.BELOW_MINIMUM);
        ruleparams.addParameter(CascadeTimeCodeRule.PARAM_CASCADE_FOR_BALANCE, "VACATION");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeBalanceOverride insBal = new InsertEmployeeBalanceOverride(c);
        insBal.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insBal.setEmpId(empId);
        insBal.setStartDate(start);
        insBal.setEndDate(start);
        insBal.setEmpbalActionSET();
        insBal.setBalId(vacBalId);
        insBal.setEmpbalValue(-1);

        ovrBuilder.add(insBal);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(c);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setWrkdTcodeName("VAC");
        ins.setOvrType(OverrideData.LTA_TYPE_START);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertTrue(getEmployeeBalanceValueAsOfEndOfDate(empId , start, vacBalId) == -1);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);;
        assertTrue(wds.size() > 0);
        assertEquals("TRN" , wds.getWorkDetail(0).getWrkdTcodeName() );
    }



    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
