package com.wbiag.app.ta.ruleengine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

import com.wbiag.app.ta.db.WbiagCalcgrpParamOvrCache;
import com.wbiag.app.ta.quickrules.VariableBreakRule;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.security.team.*;
import com.workbrain.sql.DBSequence;
import com.workbrain.test.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.tool.security.*;
import com.workbrain.util.*;

import junit.framework.*;
/**
 *  Title: DataEventCalcgrpShiftBreakParamTest
 *  Description: Test for Data Event for overriding global registry parameters
 *               PARAM_NO_SWIPE_FOR_BREAKS and ALLOW_PARTIAL_BREAKS
 *  Copyright: Copyright (c) 2005, 2006, 200nc.
 *
 * @author gtam@workbrain.com
*/
public class DataEventCalcgrpShiftBreakParamTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DataEventCalcgrpShiftBreakParamTest.class);

    public DataEventCalcgrpShiftBreakParamTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(DataEventCalcgrpShiftBreakParamTest.class);
        return result;
    }

    /**
     * Example 1: Clock In/Out for Breaks with No Swipe False and Allow Partial Breaks Yes 
     * @throws Exception
     */
    public void test1() throws Exception
    {
        setDataEventClassPath("com.wbiag.app.ta.ruleengine.DataEventCalcgrpShiftBreakParam");
        RegistryHelper regHelper = new RegistryHelper();
        
        
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);
    
        final int empId = 3;
        final String breakCode = "BRK";
        final String arriveEarlyCode = "GUAR";
        final String arriveLateCode = "TRN";     
        final String wrkCode = "WRK";
        final String uatCode = "UAT";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();
        
        //Set global parameters
        regHelper.setVar("system/WORKBRAIN_PARAMETERS/ALLOW_PARTIAL_BREAKS","TRUE");
        regHelper.setVar("system/WORKBRAIN_PARAMETERS/NO_SWIPE_FOR_BREAKS","FALSE");
        
        //Set calcgrp level parameters
        EmployeeAccess ea = new EmployeeAccess(getConnection(), getCodeMapper());
        EmployeeData ed = ea.load(empId, start);
        int calcGrpId = ed.getCalcgrpId();
        insertCalcGrpParamOvr(calcGrpId, start, start, "Yes", "False");

        // *** create the rule
        Rule rule = new VariableBreakRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        
        clearAndAddRule(empId , start , rule , ruleparams);
        
        InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
        ies.setWbuNameBoth("JUNIT", "JUNIT");
        ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ies.setEmpId(empId);
        ies.setStartDate(start);      
        ies.setEndDate(start);
        Datetime sh1Start = DateHelper.addMinutes(start, 7*60+30);
        Datetime sh1End = DateHelper.addMinutes(start, 14*60);
        ies.setEmpskdActStartTime(sh1Start);
        ies.setEmpskdActEndTime(sh1End);
        
        //create breaks
        List breakList = new ArrayList();
        
        //break 1
        ShiftBreakData shiftBreak = new ShiftBreakData();
        shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
        shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 10*60+30));
        shiftBreak.setShftbrkMinutes(30);
        shiftBreak.setTcodeId(breakCodeId);
        breakList.add(shiftBreak);
                
        //add breaks to override
        EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
        ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));               
        
        ovrBuilder.add(ies);
                
        Datetime clk1On = DateHelper.addMinutes(start , 7*60+30);
        Datetime clk1Off = DateHelper.addMinutes(start , 8*60);
        Datetime clk1On1 = DateHelper.addMinutes(start , 8*60+15);
        Datetime clk1Off1 = DateHelper.addMinutes(start , 14*60);       
    
        //create clocks list
        List clockTimes = new ArrayList();
        clockTimes.add(clk1On);
        clockTimes.add(clk1Off);
        clockTimes.add(clk1On1);
        clockTimes.add(clk1Off1);
        
        InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
        String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(start);
        wsOvr.setEndDate(start);
        ovrBuilder.add(wsOvr);
        
        ovrBuilder.execute(true , false); 
        ovrBuilder.clear();    
        
        WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);              
        
        WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
        assertTrue(workDetail.getWrkdMinutes() == 30);
        assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
        
        workDetail = (WorkDetailData)wdl.get(1);
        assertTrue(workDetail.getWrkdMinutes() == 15);        
        assertTrue(uatCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));         
        
        workDetail = (WorkDetailData)wdl.get(2);
        assertTrue(workDetail.getWrkdMinutes() == 5*60 + 45);
        assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));     
        
    }    

    /**
     * Example 2: Clock In/Out for Breaks with No Swipe True and Allow Partial Breaks Yes 
     * @throws Exception
     */
    public void test2() throws Exception
    {
        setDataEventClassPath("com.wbiag.app.ta.ruleengine.DataEventCalcgrpShiftBreakParam");
        RegistryHelper regHelper = new RegistryHelper();
        
        
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);
    
        final int empId = 3;
        final String breakCode = "BRK";
        final String arriveEarlyCode = "GUAR";
        final String arriveLateCode = "TRN";     
        final String wrkCode = "WRK";
        final String uatCode = "UAT";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "TUE");
        int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();
        
        //Set global parameters
        regHelper.setVar("system/WORKBRAIN_PARAMETERS/ALLOW_PARTIAL_BREAKS","TRUE");
        regHelper.setVar("system/WORKBRAIN_PARAMETERS/NO_SWIPE_FOR_BREAKS","FALSE");
        
        //Set calcgrp level parameters
        EmployeeAccess ea = new EmployeeAccess(getConnection(), getCodeMapper());
        EmployeeData ed = ea.load(empId, start);
        int calcGrpId = ed.getCalcgrpId();
        insertCalcGrpParamOvr(calcGrpId, start, start, "Yes", "True");

        // *** create the rule
        Rule rule = new VariableBreakRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        
        clearAndAddRule(empId , start , rule , ruleparams);
        
        InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
        ies.setWbuNameBoth("JUNIT", "JUNIT");
        ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ies.setEmpId(empId);
        ies.setStartDate(start);      
        ies.setEndDate(start);
        Datetime sh1Start = DateHelper.addMinutes(start, 7*60+30);
        Datetime sh1End = DateHelper.addMinutes(start, 14*60);
        ies.setEmpskdActStartTime(sh1Start);
        ies.setEmpskdActEndTime(sh1End);
        
        //create breaks
        List breakList = new ArrayList();
        
        //break 1
        ShiftBreakData shiftBreak = new ShiftBreakData();
        shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
        shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 10*60+30));
        shiftBreak.setShftbrkMinutes(30);
        shiftBreak.setTcodeId(breakCodeId);
        breakList.add(shiftBreak);
                
        //add breaks to override
        EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
        ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));               
        
        ovrBuilder.add(ies);
                
        Datetime clk1On = DateHelper.addMinutes(start , 7*60+30);
        Datetime clk1Off = DateHelper.addMinutes(start , 8*60);
        Datetime clk1On1 = DateHelper.addMinutes(start , 8*60+15);
        Datetime clk1Off1 = DateHelper.addMinutes(start , 14*60);       
    
        //create clocks list
        List clockTimes = new ArrayList();
        clockTimes.add(clk1On);
        clockTimes.add(clk1Off);
        clockTimes.add(clk1On1);
        clockTimes.add(clk1Off1);
        
        InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
        String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(start);
        wsOvr.setEndDate(start);
        ovrBuilder.add(wsOvr);
        
        ovrBuilder.execute(true , false); 
        ovrBuilder.clear();    
        
        WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);              
        
        WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
        assertTrue(workDetail.getWrkdMinutes() == 30);
        assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
        
        workDetail = (WorkDetailData)wdl.get(1);
        assertTrue(workDetail.getWrkdMinutes() == 15);        
        assertTrue(uatCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));         
        
        workDetail = (WorkDetailData)wdl.get(2);
        assertTrue(workDetail.getWrkdMinutes() == 1*60 + 45);
        assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));     

        workDetail = (WorkDetailData)wdl.get(3);
        assertTrue(workDetail.getWrkdMinutes() == 30);
        assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));     

        workDetail = (WorkDetailData)wdl.get(4);
        assertTrue(workDetail.getWrkdMinutes() == 3*60 + 30);
        assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

    }    

    /**
     * Example 3: Do not Clock In/Out for Breaks with No Swipe False and Allow Partial Breaks Yes
     *            Calcgrp param overrides global param. 
     * @throws Exception
     */
    public void test3() throws Exception
    {
        setDataEventClassPath("com.wbiag.app.ta.ruleengine.DataEventCalcgrpShiftBreakParam");
        RegistryHelper regHelper = new RegistryHelper();
        
        
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);
    
        final int empId = 3;
        final String breakCode = "BRK";
        final String arriveEarlyCode = "GUAR";
        final String arriveLateCode = "TRN";     
        final String wrkCode = "WRK";
        final String uatCode = "UAT";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "WED");
        int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();
        
        //Set global parameters
        regHelper.setVar("system/WORKBRAIN_PARAMETERS/ALLOW_PARTIAL_BREAKS","TRUE");
        regHelper.setVar("system/WORKBRAIN_PARAMETERS/NO_SWIPE_FOR_BREAKS","TRUE");
        
        //Set calcgrp level parameters
        EmployeeAccess ea = new EmployeeAccess(getConnection(), getCodeMapper());
        EmployeeData ed = ea.load(empId, start);
        int calcGrpId = ed.getCalcgrpId();
        insertCalcGrpParamOvr(calcGrpId, start, start, "Yes", "False");

        // *** create the rule
        Rule rule = new VariableBreakRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        
        clearAndAddRule(empId , start , rule , ruleparams);
        
        InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
        ies.setWbuNameBoth("JUNIT", "JUNIT");
        ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ies.setEmpId(empId);
        ies.setStartDate(start);      
        ies.setEndDate(start);
        Datetime sh1Start = DateHelper.addMinutes(start, 7*60+30);
        Datetime sh1End = DateHelper.addMinutes(start, 14*60);
        ies.setEmpskdActStartTime(sh1Start);
        ies.setEmpskdActEndTime(sh1End);
        
        //create breaks
        List breakList = new ArrayList();
        
        //break 1
        ShiftBreakData shiftBreak = new ShiftBreakData();
        shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
        shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 10*60+30));
        shiftBreak.setShftbrkMinutes(30);
        shiftBreak.setTcodeId(breakCodeId);
        breakList.add(shiftBreak);
                
        //add breaks to override
        EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
        ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));               
        
        ovrBuilder.add(ies);
                
        Datetime clk1On = DateHelper.addMinutes(start , 7*60+30);
        Datetime clk1Off = DateHelper.addMinutes(start , 14*60);       
    
        //create clocks list
        List clockTimes = new ArrayList();
        clockTimes.add(clk1On);
        clockTimes.add(clk1Off);
        
        InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
        String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(start);
        wsOvr.setEndDate(start);
        ovrBuilder.add(wsOvr);
        
        ovrBuilder.execute(true , false); 
        ovrBuilder.clear();    
        
        WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);              
        
        WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
        assertTrue(workDetail.getWrkdMinutes() == 6*60 + 30);
        assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
        
    }    
    
    /**
     * Example 4: Do not Clock In/Out for Breaks with No Swipe True and Allow Partial Breaks Yes
     *            Calcgrp param set to null -> defaults to global param. 
     * @throws Exception
     */
    public void test4() throws Exception
    {
        setDataEventClassPath("com.wbiag.app.ta.ruleengine.DataEventCalcgrpShiftBreakParam");
        RegistryHelper regHelper = new RegistryHelper();
        
        
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);
    
        final int empId = 3;
        final String breakCode = "BRK";
        final String arriveEarlyCode = "GUAR";
        final String arriveLateCode = "TRN";     
        final String wrkCode = "WRK";
        final String uatCode = "UAT";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "THU");
        int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();
        
        //Set global parameters
        regHelper.setVar("system/WORKBRAIN_PARAMETERS/ALLOW_PARTIAL_BREAKS","TRUE");
        regHelper.setVar("system/WORKBRAIN_PARAMETERS/NO_SWIPE_FOR_BREAKS","TRUE");
        
        //Set calcgrp level parameters
        EmployeeAccess ea = new EmployeeAccess(getConnection(), getCodeMapper());
        EmployeeData ed = ea.load(empId, start);
        int calcGrpId = ed.getCalcgrpId();
        insertCalcGrpParamOvr(calcGrpId, start, start, null, null);

        // *** create the rule
        Rule rule = new VariableBreakRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        
        clearAndAddRule(empId , start , rule , ruleparams);
        
        InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
        ies.setWbuNameBoth("JUNIT", "JUNIT");
        ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ies.setEmpId(empId);
        ies.setStartDate(start);      
        ies.setEndDate(start);
        Datetime sh1Start = DateHelper.addMinutes(start, 7*60+30);
        Datetime sh1End = DateHelper.addMinutes(start, 14*60);
        ies.setEmpskdActStartTime(sh1Start);
        ies.setEmpskdActEndTime(sh1End);
        
        //create breaks
        List breakList = new ArrayList();
        
        //break 1
        ShiftBreakData shiftBreak = new ShiftBreakData();
        shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
        shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 10*60+30));
        shiftBreak.setShftbrkMinutes(30);
        shiftBreak.setTcodeId(breakCodeId);
        breakList.add(shiftBreak);
                
        //add breaks to override
        EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
        ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));               
        
        ovrBuilder.add(ies);
                
        Datetime clk1On = DateHelper.addMinutes(start , 7*60+30);
        Datetime clk1Off = DateHelper.addMinutes(start , 14*60);       
    
        //create clocks list
        List clockTimes = new ArrayList();
        clockTimes.add(clk1On);
        clockTimes.add(clk1Off);
        
        InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
        String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(start);
        wsOvr.setEndDate(start);
        ovrBuilder.add(wsOvr);
        
        ovrBuilder.execute(true , false); 
        ovrBuilder.clear();    
        
        WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);              
        
        WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
        assertTrue(workDetail.getWrkdMinutes() == 2*60 + 30);
        assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

        workDetail = (WorkDetailData)wdl.get(1);
        assertTrue(workDetail.getWrkdMinutes() == 30);
        assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

        workDetail = (WorkDetailData)wdl.get(2);
        assertTrue(workDetail.getWrkdMinutes() == 3*60 + 30);
        assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

    }    
    private void insertCalcGrpParamOvr(int calcGrpId, Date startDate, Date endDate, 
            String allowPartialBreaks, String noSwipeForBreaks) throws Exception{

        PreparedStatement pstmt = null;
        try {
            Connection conn = getConnection();
            WbiagCalcgrpParamOvrCache wbiagCalcgrpParamOvrCache = WbiagCalcgrpParamOvrCache.getInstance();
            wbiagCalcgrpParamOvrCache.updateCacheContents(conn);

            int wcpoId = getConnection().getDBSequence("seq_wcpo_id").getNextValue();

            pstmt = conn.prepareStatement("insert into wbiag_calcgrp_param_ovr "
                                                            + "(wcpo_id, calcgrp_id, wcpo_start_date, wcpo_end_date, wcpo_allow_partial_breaks, wcpo_no_swipe_for_breaks) "
                                                            + "values (?,?,?,?,?,?)");

            pstmt.setInt(1, wcpoId);
            pstmt.setInt(2, calcGrpId);
            pstmt.setDate(3, new java.sql.Date(startDate.getTime()));
            pstmt.setDate(4, new java.sql.Date(endDate.getTime()));
            pstmt.setString(5, allowPartialBreaks);
            pstmt.setString(6, noSwipeForBreaks);
            pstmt.executeUpdate();
           
        } catch(Exception e){
            throw e;
        } finally {
            pstmt.close();
        }
        

    }
    
    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
