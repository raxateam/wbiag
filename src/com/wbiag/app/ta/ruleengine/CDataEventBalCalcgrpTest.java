package com.wbiag.app.ta.ruleengine;

import java.util.*;

import com.wbiag.app.ta.db.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.test.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for CDataEventBalCalcgrpTest.
 */
public class CDataEventBalCalcgrpTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventBalCalcgrpTest.class);

    public CDataEventBalCalcgrpTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CDataEventBalCalcgrpTest.class);
        return result;
    }


    /**
     * EmpBal overrides not error based on calcgrp specific min/max
     * @throws Exception
     */
    public void testBalCalcgrpNoError() throws Exception {
        BalanceCalcgrpCache.getInstance().updateCacheContents(getConnection());
        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventBalCalcgrp");
        TestUtil.getInstance().setVarTemp("/system/WORKBRAIN_PARAMETERS/TEST_BALANCES" ,
                                          "false");

        final int empId = 15;
        final int balId = 0;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        BalanceAccess ba = new BalanceAccess(getConnection());
        BalanceData bd = ba.load(balId);
        bd.setBalMax(99999);
        bd.setBalMin(-99999);
        ba.updateRecordData(bd, "BALANCE" , "bal_id");

        IagBalanceCalcgrpData balCg = new IagBalanceCalcgrpData();
        balCg.setBalId(balId);
        balCg.setCalcgrpId(getEmployeeData(empId , start).getCalcgrpId());
        balCg.setBaltypId(1);
        balCg.setIbcgBalMin(-1);
        balCg.setIbcgBalMax(100);
        balCg.setIbcgId(getConnection().getDBSequence("seq_ibcg_id").getNextValue());
        new RecordAccess(getConnection()).insertRecordData(balCg , "IAG_BALANCE_CALCGRP");

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        InsertEmployeeBalanceOverride ins = new InsertEmployeeBalanceOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setBalId(balId);
        ins.setEmpbalValue(-1);
        ins.setEmpbalActionSET();

        ovrBuilder.add(ins);

        ovrBuilder.execute(true, false);

        WorkSummaryData ws = getWorkSummaryForDate(empId , start);
        assertFalse(ws.isError());
    }

    /**
     * EmpBal overrides error based on calcgrp specific min/max
     * @throws Exception
     */
    public void testBalCalcgrpError() throws Exception {
        BalanceCalcgrpCache.getInstance().updateCacheContents(getConnection());

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventBalCalcgrp");
        TestUtil.getInstance().setVarTemp("/system/WORKBRAIN_PARAMETERS/TEST_BALANCES" ,
                                          "false");

        final int empId = 15;
        final int balId = 0;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        BalanceAccess ba = new BalanceAccess(getConnection());
        BalanceData bd = ba.load(balId);
        bd.setBalMax(99999);
        bd.setBalMin(-99999);
        ba.updateRecordData(bd, "BALANCE" , "bal_id");

        IagBalanceCalcgrpData balCg = new IagBalanceCalcgrpData();
        balCg.setBalId(balId);
        balCg.setCalcgrpId(getEmployeeData(empId , start).getCalcgrpId());
        balCg.setBaltypId(1);
        balCg.setIbcgBalMin(-1);
        balCg.setIbcgBalMax(10);
        balCg.setIbcgId(getConnection().getDBSequence("seq_ibcg_id").getNextValue());
        new RecordAccess(getConnection()).insertRecordData(balCg , "IAG_BALANCE_CALCGRP");

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        InsertEmployeeBalanceOverride ins = new InsertEmployeeBalanceOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setBalId(balId);
        ins.setEmpbalValue(15);
        ins.setEmpbalActionSET();

        ovrBuilder.add(ins);

        ovrBuilder.execute(true, false);

        WorkSummaryData ws = getWorkSummaryForDate(empId , start);
        assertTrue(ws.isError());
    }

    /**
     * Creates a time code calcgrp bal data and makes sure that amount is applied to employee
     * @throws Exception
     */
    public void testTcodeCalcgrp() throws Exception {
        BalanceCalcgrpCache.getInstance().updateCacheContents(getConnection());
        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventBalCalcgrp");
        final int empId = 15;
        final int balId = 0; // *** Vacation
        final int tcodeId = 10; // *** TRN
        final int tcbtId = 5; // *** decrement hours
        final double initialVal = 10.0;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        TimeCodeAccess ta = new TimeCodeAccess(getConnection());
        TimeCodeData td = ta.load(tcodeId);
        td.setTcodeAffectsBalances("Y");
        ta.updateRecordData(td, "TIME_CODE" , "tcode_id");

        IagTimeCodeBalData tcodeBal = new IagTimeCodeBalData();
        tcodeBal.setBalId(balId);
        tcodeBal.setCalcgrpId(getEmployeeData(empId , start).getCalcgrpId());
        tcodeBal.setTcodeId(tcodeId);
        tcodeBal.setTcbtId(tcbtId);
        new RecordAccess(getConnection()).insertRecordData(tcodeBal , "IAG_TIME_CODE_BAL");

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        InsertEmployeeBalanceOverride ins = new InsertEmployeeBalanceOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setBalId(balId);
        ins.setEmpbalValue(initialVal);
        ins.setEmpbalActionSET();
        ovrBuilder.add(ins);

        InsertWorkSummaryOverride ins2 = new InsertWorkSummaryOverride(getConnection());
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);
        ins2.setEndDate(start);
        ins2.setWrksFullDayCode("TRN");
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true, false);

        System.out.println(getWorkDetailsForDate(empId , start).toDescription()  );
        double val = getEmployeeBalanceValueAsOfEndOfDate(empId , start , balId);
        assertEquals(initialVal - 8 , val, 0);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
