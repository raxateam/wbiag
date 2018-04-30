package com.wbiag.app.ta.ruleengine;

import java.util.*;

import com.wbiag.app.ta.db.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import junit.framework.*;
import java.math.BigDecimal;
/**
 * Test for RecalculateTaskTest.
 * @deprecated    As of 4.1 FP28, use core classes
 */
public class RecalculateTaskTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecalculateTaskTest.class);

    public RecalculateTaskTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(RecalculateTaskTest.class);
        return result;
    }

    /**
     * Dummy to avoid warning, tests have been deprecated
     * @throws Exception
     */
    public void testDummy() throws Exception {
        assertTrue(1==1);
    }
    /**
     * Tests business recalc. makes sure payperiod is calced
     * @throws Exception
     */
    public void xRecalculateTaskBusiness() throws Exception {

        resetLastRecalcDate();

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON");
        // *** craete def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start, start).execute(false);

        // *** create one override
        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setOvrType(OverrideData.WORK_SUMMARY_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setWrksFlag1("Y");

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        RecalculateTask task = new RecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(RecalculateTask.PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC ,
                   String.valueOf(OverrideData.WORK_SUMMARY_TYPE_START));
        params.put(RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "1");
        params.put(RecalculateTask.PARAM_ROLLOUT_DATE,
                   DateHelper.convertDateString(DateHelper.addMinutes(new Date() , -10) , RecalculateTask.ROLLOUT_DATE_FORMAT ));
        params.put(RecalculateTask.PARAM_BATCH_SIZE, "50");

        task.execute(getConnection() , params);

        assertTrue(task.getEmpDatesToRecalc().contains(empId , start ));
        EmployeeData ed = getEmployeeData(empId , start );
        PayGroupData pg = getCodeMapper().getPayGroupById(ed.getPaygrpId()) ;
        assertTrue(task.getEmpDatesToRecalc().contains(empId ,
            DateHelper.getUnitPayPeriod(DateHelper.APPLY_ON_FIRST_DAY, false, start, pg) ));
        assertTrue(task.getEmpDatesToRecalc().contains(empId ,
            DateHelper.getUnitPayPeriod(DateHelper.APPLY_ON_LAST_DAY, false, start, pg) ));
    }

    /**
     * Tests business recalc. makes sure payperiod is calced
     * @throws Exception
     */
    public void xRecalculateTaskBusinessCancel() throws Exception {

        resetLastRecalcDate();

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON");
        // *** craete def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start, start).execute(false);

        // *** create one override
        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        int ovrId = getConnection().getDBSequence("seq_ovr_id").getNextValue();
        ins.setOverrideId(ovrId);
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setOvrType(OverrideData.WORK_SUMMARY_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setWrksFlag1("Y");

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false); ovrBuilder.clear();

        RecalculateTask task = new RecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(RecalculateTask.PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC ,
                   String.valueOf(OverrideData.WORK_SUMMARY_TYPE_START));
        params.put(RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "1");
        params.put(RecalculateTask.PARAM_ROLLOUT_DATE,
                   DateHelper.convertDateString(DateHelper.addMinutes(new Date() , -10) , RecalculateTask.ROLLOUT_DATE_FORMAT ));
        params.put(RecalculateTask.PARAM_BATCH_SIZE, "50");

        task.execute(getConnection() , params);
        assertTrue(task.getEmpDatesToRecalc().contains(empId , start ));

        Thread.sleep(1000);

        DeleteOverride dov = new DeleteOverride();
        dov.setOverrideId(ovrId);
        dov.setWbuNameBoth("JUNIT", "JUNIT");
        ovrBuilder.add(dov);
        ovrBuilder.execute(true , false); ovrBuilder.clear();

        task.execute(getConnection() , params);

        assertTrue(task.getEmpDatesToRecalc().contains(empId , start ));
        EmployeeData ed = getEmployeeData(empId , start );
        PayGroupData pg = getCodeMapper().getPayGroupById(ed.getPaygrpId()) ;
        assertTrue(task.getEmpDatesToRecalc().contains(empId ,
            DateHelper.getUnitPayPeriod(DateHelper.APPLY_ON_FIRST_DAY, false, start, pg) ));
        assertTrue(task.getEmpDatesToRecalc().contains(empId ,
            DateHelper.getUnitPayPeriod(DateHelper.APPLY_ON_LAST_DAY, false, start, pg) ));
    }


    /**
     * Tests business recalc. makes sure every date until infinity are calced
     * @throws Exception
     */
    public void xRecalculateTaskBusinessInfinite() throws Exception {

        resetLastRecalcDate();

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;
        final int empId2 = 11;

        Date start = DateHelper.addDays(DateHelper.nextDay(DateHelper.getCurrentDate() , "MON"), -21);
        Date days15After = DateHelper.addDays(start , 15);
        Date days30After = DateHelper.addDays(start , 30);
        // *** craete def records
        new CreateDefaultRecords(getConnection() , new int[] {empId , empId2},
                                 start, days15After).execute(false);
        // *** craete def records
        new CreateDefaultRecords(getConnection() , new int[] {empId , empId2},
                                 start, days30After).execute(false);

        // *** create one override
        InsertEmployeeOverride ins = new InsertEmployeeOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setEmpFlag1("N");

        ovrBuilder.add(ins);

        InsertEmployeeOverride ins2 = new InsertEmployeeOverride(getConnection());
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId2);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setEmpFlag1("N");

        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        RecalculateTask task = new RecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(RecalculateTask.PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC ,
                   String.valueOf(OverrideData.EMPLOYEE_TYPE_START));
        params.put(RecalculateTask.PARAM_BUSINESS_RULE_RECALC_INFINITE, "Y");
        params.put(RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "1");
        params.put(RecalculateTask.PARAM_ROLLOUT_DATE,
                   DateHelper.convertDateString(DateHelper.addMinutes(new Date() , -10) , RecalculateTask.ROLLOUT_DATE_FORMAT ));
        params.put(RecalculateTask.PARAM_BATCH_SIZE, "50");

        task.execute(getConnection() , params);

        assertTrue(task.getEmpDatesToRecalc().contains(empId , start ));
        assertTrue(task.getEmpDatesToRecalc().contains(empId , days15After ));
        assertTrue(task.getEmpDatesToRecalc().contains(empId , days30After ));
        EmployeeData ed = getEmployeeData(empId , start );
        PayGroupData pg = getCodeMapper().getPayGroupById(ed.getPaygrpId()) ;
        assertTrue(task.getEmpDatesToRecalc().contains(empId ,
            DateHelper.getUnitPayPeriod(DateHelper.APPLY_ON_FIRST_DAY, false, start, pg) ));

        assertTrue(task.getEmpDatesToRecalc().contains(empId2 , start ));
    }

    /**
     * Tests business recalc
     * @throws Exception
     */
    public void xRecalculatePending() throws Exception {

        resetLastRecalcDate();

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON");
        Date days30Ago = DateHelper.addDays(start , -30);
        // *** craete def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start, start).execute(false);

        // *** create one override
        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_SUMMARY_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setWrksFlag1("Y");

        ovrBuilder.add(ins);
        ovrBuilder.execute(false , false);

        RecalculateTask task = new RecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(RecalculateTask.PARAM_CHECK_PENDING_OVERRIDES , "Y");
        params.put(RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "1");
        params.put(RecalculateTask.PARAM_ROLLOUT_DATE,
                   DateHelper.convertDateString(DateHelper.addMinutes(new Date() , -10) , RecalculateTask.ROLLOUT_DATE_FORMAT ));
        params.put(RecalculateTask.PARAM_BATCH_SIZE, "50");

        task.execute(getConnection() , params);

        assertTrue(task.getEmpDatesToRecalc().contains(empId , start ));
    }

    /**
     * Test recalc table
     * @throws Exception
     */
    public void xRecalculateTaskCalcRecalcTableMultiThread() throws Exception {
        // *** clean up table
        java.sql.PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM calc_employee_date");
            ps = getConnection().prepareStatement(sb.toString());
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        resetLastRecalcDate();

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;
        final int empId2 = 14;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON");
        Date days100Ago = DateHelper.addDays(start , -100);
        // *** craete def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start, start).execute(false);

        // *** create CALC_EMPLOYEE_DATE table records
        CalcEmployeeDateAccess.addCalcEmployeeDate(getConnection(), empId,  days100Ago, start , "Test");
        CalcEmployeeDateAccess.addCalcEmployeeDate(getConnection(), empId2,  days100Ago, start , "Test");
        getConnection().commit();

        RecalculateTask task = new RecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(RecalculateTask.PARAM_PROCESS_CALC_EMP_DATE_TABLE , "Y");
        params.put(RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "2");
        params.put(RecalculateTask.PARAM_CHECK_PENDING_OVERRIDES , "N");
        params.put(RecalculateTask.PARAM_ROLLOUT_DATE,
                   DateHelper.convertDateString(DateHelper.addMinutes(new Date() , -10) , RecalculateTask.ROLLOUT_DATE_FORMAT ));
        params.put(RecalculateTask.PARAM_BATCH_SIZE, "50");

        task.execute(getConnection() , params);

        assertTrue(task.getEmpDatesToRecalc().contains(empId , start ));
        assertTrue(task.getEmpDatesToRecalc().contains(empId , days100Ago ));
        assertTrue(task.getEmpDatesToRecalc().contains(empId2 , start ));

        List afterRecs = CalcEmployeeDateAccess.loadCalcEmployeeDate(getConnection())   ;
        assertTrue("Table should be empty", afterRecs.size() == 0);
    }


    /**
     * Tests no-show option
     * @throws Exception
     */
    public void xRecalculateTaskNoShow() throws Exception {

        resetLastRecalcDate();

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 11;

        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7) , "MON");
        Date days30Ago = DateHelper.addDays(start , -30);
        // *** craete def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start, start).execute(false);

        // *** create one override
        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_SUMMARY_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setWrksClocks("");

        ovrBuilder.setFirstStartDate(start);
        ovrBuilder.setLastEndDate(start);
        IntegerList emps = new IntegerList();
        emps.add(empId);
        ovrBuilder.setEmpIdList(emps);
        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        RecalculateTask task = new RecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(RecalculateTask.PARAM_PROCESS_NO_SHOW_EMPLOYEES , "Y");
        params.put(RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "1");
        params.put(RecalculateTask.PARAM_CHECK_PENDING_OVERRIDES , "N");
        params.put(RecalculateTask.PARAM_ROLLOUT_DATE,
                   DateHelper.convertDateString(DateHelper.addDays(start , -1) , RecalculateTask.ROLLOUT_DATE_FORMAT ));
        params.put(RecalculateTask.PARAM_BATCH_SIZE, "50");

        task.execute(getConnection() , params);

        assertTrue(task.getEmpDatesToRecalc().contains(empId , start ));
    }

    /**
     * Test recalc table
     * @throws Exception
     */
    public void xRecalculateTaskJobRate() throws Exception {

        resetLastRecalcDate();

        final int empId = 15;

        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7) , "MON");
        Date days30Ago = DateHelper.addDays(start , -30);

        final int jobId = 12;
        //final int jobRateId = 10001;

        JobRateAccess ja = new JobRateAccess (getConnection());
        JobRateData jrd = new JobRateData();
        jrd.setJobrateId(new Integer(getConnection().getDBSequence(ja.JOB_RATE_SEQ).getNextValue()));
        jrd.setJobId(new Integer(jobId));
        jrd.setJobrateEffectiveDate(new Date());
        jrd.setJobrateIndex(new Integer(1));
        jrd.setJobrateRate(new BigDecimal(10));
        ja.insert(jrd);

        jrd.setJobrateRate(new BigDecimal(12));
        jrd.setJobrateEffectiveDate(DateHelper.DATE_1900);
        ja.updateRecordData(jrd , ja.JOB_RATE_TABLE, ja.JOB_RATE_PRI_KEY);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);
        InsertWorkDetailOverride ins = new InsertWorkDetailOverride (getConnection());
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setJobId(jobId);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        RecalculateTask task = new RecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(RecalculateTask.PARAM_PROCESS_JOB_RATE_UPDATES , "Y");
        params.put(RecalculateTask.PARAM_PROCESS_CALC_EMP_DATE_TABLE , "N");
        params.put(RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "1");
        params.put(RecalculateTask.PARAM_CHECK_PENDING_OVERRIDES , "N");
        params.put(RecalculateTask.PARAM_ROLLOUT_DATE,
                   DateHelper.convertDateString(DateHelper.addDays(start , -1) , RecalculateTask.ROLLOUT_DATE_FORMAT ));
        params.put(RecalculateTask.PARAM_BATCH_SIZE, "50");

        task.execute(getConnection() , params);

        assertTrue(task.getEmpDatesToRecalc().contains(empId , start ));
    }

    /**
     * Tests CalcgrpDefChange option
     * @throws Exception
     */
    public void xRecalculateTaskCalcgrpDefChange() throws Exception {

        resetLastRecalcDate();

        final int empId = 15;

        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.
            getCurrentDate(), -7), "MON");
        Date days7Ago = DateHelper.addDays(start, -7);
        // *** craete def records
        new CreateDefaultRecords(getConnection(), new int[] {empId}
                                 ,
                                 start, start).execute(false);
        EmployeeData ed = getEmployeeData(empId , DateHelper.getCurrentDate());
        int cgId = ed.getCalcgrpId();
        // *** update calcgroup with audit
        CalcGroupData cgData = getCodeMapper().getCalcGroupById(cgId);
        CalculationGroup calcGroup = new CalculationGroup(cgData);
        calcGroup.setDescription("Test");

        CalcGroupAccess cgAccess = new CalcGroupAccess(getConnection());
        cgAccess.load(cgId);
        cgData.setCalcgrpXml(calcGroup.makeXml(true));
        new CalcGroupAccess(getConnection()).save(cgData , days7Ago, start, "Test");


        RecalculateTask task = new RecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(RecalculateTask.PARAM_PROCESS_CALCGRP_DEF_CHANGES , "Y");
        params.put(RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "1");
        params.put(RecalculateTask.PARAM_ROLLOUT_DATE,
                   DateHelper.convertDateString(DateHelper.addMinutes(new Date() , -10) , RecalculateTask.ROLLOUT_DATE_FORMAT ));
        params.put(RecalculateTask.PARAM_BATCH_SIZE, "50");

        task.execute(getConnection() , params);

        assertTrue(task.getEmpDatesToRecalc().contains(empId , start ));
    }

    /**
     * Tests EntemppolChange option
     * @throws Exception
     */
    public void xRecalculateTaskEntemppolChange() throws Exception {

        resetLastRecalcDate();

        final int empId = 15;

        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.
            getCurrentDate(), -7), "MON");
        Date days7Ago = DateHelper.addDays(start, -7);
        // *** craete def records
        new CreateDefaultRecords(getConnection(), new int[] {empId}
                                 ,
                                 days7Ago, start).execute(false);
        EmployeeData ed = getEmployeeData(empId , DateHelper.getCurrentDate());
        String polName = "TEST";
        // *** update ent emp policy
        EntPolicyData entPolData = new EntPolicyData();
        int polId = getConnection().getDBSequence(EntitlementAccess.ENT_POLICY_SEQ).getNextValue();
        entPolData.setEntpolId(polId);
        entPolData.setEntpolName(polName);
        entPolData.setEntpolStartDate(DateHelper.DATE_1900 );
        entPolData.setEntpolEndDate(DateHelper.DATE_3000);
        new RecordAccess(getConnection()).insertRecordData(entPolData,
            EntitlementAccess.ENT_POLICY_TABLE);

        EntEmpPolicyData data = new EntEmpPolicyData();
        data.setEntemppolId(getConnection().getDBSequence(EntEmpPolicyAccess.ENT_EMP_POLICY_SEQ).getNextValue());
        data.setEmpId(empId);
        data.setEntpolId(polId);
        data.setEntemppolStartDate(days7Ago);
        data.setEntemppolEndDate(start);
        new EntEmpPolicyAccess(getConnection()).insert(data);

        RecalculateTask task = new RecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(RecalculateTask.PARAM_PROCESS_ENTEMPPOLICY_CHANGES , "Y");
        params.put(RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "1");
        params.put(RecalculateTask.PARAM_ROLLOUT_DATE,
                   DateHelper.convertDateString(DateHelper.addMinutes(new Date() , -10) , RecalculateTask.ROLLOUT_DATE_FORMAT ));
        params.put(RecalculateTask.PARAM_BATCH_SIZE, "50");

        task.execute(getConnection() , params);

        assertTrue(task.getEmpDatesToRecalc().contains(empId , start ));
    }

    public void xRecalculateClksProc() throws Exception {

        resetLastRecalcDate();

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate(), "Mon");
        Date clkOn = DateHelper.addMinutes(start, 550);
        Date clkOff = DateHelper.addMinutes(start, 1020);
        ClockTranAccess access = new ClockTranAccess(getConnection());
        ClockTranPendJData data1 = new ClockTranPendJData();

        data1.setCtpjIdentifier(Integer.toString(empId));
        data1.setCtpjIdentType("I");
        data1.setCtpjTime(DateHelper.convertDateString(clkOn,
            Clock.CLOCKDATEFORMAT_STRING));
        data1.setCtpjRdrName("VIRTUAL READER");
        data1.setCtpjType(Integer.toString(Clock.TYPE_ON));
        access.insert(data1);

        com.workbrain.app.clockInterface.processing.WBClockProcessTask ctask =
            new com.workbrain.app.clockInterface.processing.WBClockProcessTask();
        ctask.setShouldCommit(false);
        ctask.setCheckForInterrupt(false);
        ctask.execute(getConnection());

        RecalculateTask task = new RecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(RecalculateTask.PARAM_PROCESS_CLOCKS_PROCESSED , "Y");
        params.put(RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "1");
        params.put(RecalculateTask.PARAM_ROLLOUT_DATE,
                   DateHelper.convertDateString(DateHelper.addMinutes(new Date() , -10) , RecalculateTask.ROLLOUT_DATE_FORMAT ));
        params.put(RecalculateTask.PARAM_BATCH_SIZE, "50");

        task.execute(getConnection() , params);

        assertTrue(task.getEmpDatesToRecalc().contains(empId , start ));

    }

    /**
     * Create clocks for two emps but assure only one gets recalced due to calcgrp constraint
     * @throws Exception
     */
    public void xRecalculateApplyToCalcgrp() throws Exception {

        resetLastRecalcDate();

        final int empId1 = 15, empId2 = 20;
        EmployeeData e1 = getEmployeeData(empId1, DateHelper.getCurrentDate() );
        EmployeeData e2 = getEmployeeData(empId2, DateHelper.getCurrentDate() );
        assertFalse("Calcgrps need to be different to resume test" , e1.getCalcgrpId() == e2.getCalcgrpId());

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate(), "Mon");
        Date clkOn = DateHelper.addMinutes(start, 550);
        Date clkOff = DateHelper.addMinutes(start, 1020);
        ClockTranAccess access = new ClockTranAccess(getConnection());
        ClockTranPendJData data1 = new ClockTranPendJData();

        data1.setCtpjIdentifier(Integer.toString(empId1));
        data1.setCtpjIdentType("I");
        data1.setCtpjTime(DateHelper.convertDateString(clkOn,
            Clock.CLOCKDATEFORMAT_STRING));
        data1.setCtpjRdrName("VIRTUAL READER");
        data1.setCtpjType(Integer.toString(Clock.TYPE_ON));
        access.insert(data1);

        ClockTranPendJData data2 = new ClockTranPendJData();

        data2.setCtpjIdentifier(Integer.toString(empId2));
        data2.setCtpjIdentType("I");
        data2.setCtpjTime(DateHelper.convertDateString(clkOn,
            Clock.CLOCKDATEFORMAT_STRING));
        data2.setCtpjRdrName("VIRTUAL READER");
        data2.setCtpjType(Integer.toString(Clock.TYPE_ON));
        access.insert(data2);

        com.workbrain.app.clockInterface.processing.WBClockProcessTask ctask =
            new com.workbrain.app.clockInterface.processing.WBClockProcessTask();
        ctask.setShouldCommit(false);
        ctask.setCheckForInterrupt(false);
        ctask.execute(getConnection());

        RecalculateTask task = new RecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(RecalculateTask.PARAM_APPLY_TO_CALCGRPS , String.valueOf(e1.getCalcgrpId()));
        params.put(RecalculateTask.PARAM_APPLY_TO_CALCGRPS_INCLUSIVE , "N");
        params.put(RecalculateTask.PARAM_PROCESS_CLOCKS_PROCESSED , "Y");
        params.put(RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "1");
        params.put(RecalculateTask.PARAM_ROLLOUT_DATE,
                   DateHelper.convertDateString(DateHelper.addMinutes(new Date() , -10) , RecalculateTask.ROLLOUT_DATE_FORMAT ));
        params.put(RecalculateTask.PARAM_BATCH_SIZE, "50");

        task.execute(getConnection() , params);

        assertFalse(task.getEmpDatesToRecalc().contains(empId1 , start ));
        assertTrue(task.getEmpDatesToRecalc().contains(empId2 , start ));
    }

    private void resetLastRecalcDate() throws Exception {
        java.sql.PreparedStatement ps = null;
        try {
            String sql = "UPDATE wbint_type SET wbityp_udf1=? WHERE wbityp_name = ?";
            ps = getConnection().prepareStatement(sql);
            ps.setNull(1   , java.sql.Types.CHAR   );
            ps.setString(2 , RecalculateTask.WBITYP_RECALC_NAME);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
