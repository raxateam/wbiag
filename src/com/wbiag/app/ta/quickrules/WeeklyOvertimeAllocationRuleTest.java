package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;

import junit.framework.*;

import java.util.*;

import org.apache.log4j.BasicConfigurator;

/**
 *  Title: WeeklyOvertimeAllocationRuleTest
 *  Description: JUnit testing for the WeeklyOvertimeAllocationRuleTest 
 *  Copyright: Copyright (c) 2005, 2006, 200nc.
 *
 * @author gtam@workbrain.com
*/
public class WeeklyOvertimeAllocationRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WeeklyOvertimeAllocationRuleTest.class);     

    public WeeklyOvertimeAllocationRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WeeklyOvertimeAllocationRuleTest.class);
        //BasicConfigurator.configure();
        return result;
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

    /*
     * Scenario 1 - OT Spans One day, Job Metric = Department
     */
    public void testCase1() throws Exception {
            
        final int empId = 3;
        final String ParamHourSetDescription = "REG=2400, OT1=99999";
        final String ParamEligibleHourTypes = "REG";
        final String ParamDayWeekStarts = "Sunday";
        final String ParamApplyBasedOnSchedule = "False";
        final String ParamPremiumTimeCodeInserted = "WRK";
        final String ParamPremiumHourTypeInserted = "REG";
        final String allocateTimeCode = "false";
        final String allocateHourType = "false";
        final String allocateProject = "false";
        final String allocateJob = "false";
        final String allocateDocket = "false";
        final String allocateDept = "true";

        int deptIdA = createDept("OT_TEST_A1");
        int deptIdB = createDept("OT_TEST_B1");
        Date startDate = DateHelper.createDate(2003, 9, 14); 
        Date endDate = DateHelper.addDays(startDate, 6);
        
        // CREATE THE RULE
        Rule rule = new WeeklyOvertimeAllocationRule();
        Parameters ruleparams = new Parameters();
                
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_HOURSET_DESCRIPTION, ParamHourSetDescription);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ELIGIBLE_HOURTYPES, ParamEligibleHourTypes);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_DAY_WEEK_STARTS, ParamDayWeekStarts);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_APPLY_BASED_ON_SCHEDULE, ParamApplyBasedOnSchedule);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_PREMIUM_TIMECODE_INSERTED, ParamPremiumTimeCodeInserted);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_PREMIUM_HOUR_TYPE_INSERTED, ParamPremiumHourTypeInserted);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_TIME_CODE, allocateTimeCode);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_HOUR_TYPE, allocateHourType);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_PROJECT, allocateProject);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_JOB, allocateJob);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_DOCKET, allocateDocket);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_DEPT, allocateDept);
               
        clearAndAddRule(empId, startDate, rule, ruleparams);
        
        // INSERT OVERRIDES: Work Detail Overrides
        CodeMapper cm = getCodeMapper();        
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.createsDefaultRecords(); 

        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,14), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,14)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,14),600),
                deptIdA, -1000, -1000, -1000, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,15), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,15)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,15),600),
                deptIdA, -1000, -1000, -1000, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,16), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,16)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,16),600),
                deptIdA, -1000, -1000, -1000, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,17), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,17)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,17),600),
                deptIdB, -1000, -1000, -1000, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,18), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,18)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,18),720),
                deptIdA, -1000, -1000, -1000, "WRK"));
                         
        
        ovrBuilder.setCreatesDefaultRecords(true);
        ovrBuilder.execute(true, false);
        RuleEngine.runCalcGroup(getConnection(), empId, startDate, endDate, false);
        
        // TEST INSERTION AND RULE EXECUTION
        assertOverrideAppliedCount(ovrBuilder, 5);
        assertRuleApplied(empId, DateHelper.createDate(2003,9,18), rule);
        WorkDetailList workPremiumList = getWorkPremiumsForDate(empId, DateHelper.createDate(2003,9,18));
        assertEquals(ParamPremiumTimeCodeInserted, ((WorkDetailData)workPremiumList.get(0)).getWrkdTcodeName());
        assertEquals(ParamPremiumHourTypeInserted, ((WorkDetailData)workPremiumList.get(0)).getWrkdHtypeName());
        int testDeptId = ((WorkDetailData)workPremiumList.get(0)).getDeptId();
        if (testDeptId == deptIdA) {
            assertEquals(582, ((WorkDetailData)workPremiumList.get(0)).getWrkdMinutes());
            assertEquals(deptIdB, ((WorkDetailData)workPremiumList.get(1)).getDeptId());
            assertEquals(138, ((WorkDetailData)workPremiumList.get(1)).getWrkdMinutes());
        } else if (testDeptId == deptIdB) {
            assertEquals(138, ((WorkDetailData)workPremiumList.get(0)).getWrkdMinutes());
            assertEquals(deptIdA, ((WorkDetailData)workPremiumList.get(1)).getDeptId());
            assertEquals(582, ((WorkDetailData)workPremiumList.get(1)).getWrkdMinutes());
        } else {
            assertTrue(false);
        }        
    }

    /*
     * Scenario 2 - OT spans more than one day, Job Metric = Project
     */
    public void testCase2() throws Exception {
            
        final int empId = 3;
        final String ParamHourSetDescription = "REG=2400, OT1=99999";
        final String ParamEligibleHourTypes = "REG";
        final String ParamDayWeekStarts = "Sunday";
        final String ParamApplyBasedOnSchedule = "False";
        final String ParamPremiumTimeCodeInserted = "WRK";
        final String ParamPremiumHourTypeInserted = "REG";
        final String allocateTimeCode = "false";
        final String allocateHourType = "false";
        final String allocateProject = "true";
        final String allocateJob = "false";
        final String allocateDocket = "false";
        final String allocateDept = "false";

        int projIdA = createProj("OT_TEST_A2");
        int projIdB = createProj("OT_TEST_B2");
        Date startDate = DateHelper.createDate(2003, 9, 13); 
        Date endDate = DateHelper.addDays(startDate, 6);
        
        // CREATE THE RULE
        Rule rule = new WeeklyOvertimeAllocationRule();
        Parameters ruleparams = new Parameters();
                
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_HOURSET_DESCRIPTION, ParamHourSetDescription);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ELIGIBLE_HOURTYPES, ParamEligibleHourTypes);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_DAY_WEEK_STARTS, ParamDayWeekStarts);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_APPLY_BASED_ON_SCHEDULE, ParamApplyBasedOnSchedule);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_PREMIUM_TIMECODE_INSERTED, ParamPremiumTimeCodeInserted);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_PREMIUM_HOUR_TYPE_INSERTED, ParamPremiumHourTypeInserted);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_TIME_CODE, allocateTimeCode);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_HOUR_TYPE, allocateHourType);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_PROJECT, allocateProject);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_JOB, allocateJob);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_DOCKET, allocateDocket);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_DEPT, allocateDept);
               
        clearAndAddRule(empId, startDate, rule, ruleparams);
        
        // INSERT OVERRIDES: Work Detail Overrides
        CodeMapper cm = getCodeMapper();        
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.createsDefaultRecords(); 

        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,13), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,13)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,13),600),
                -1000, projIdA, -1000, -1000, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,14), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,14)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,14),600),
                -1000, projIdB, -1000, -1000, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,15), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,15)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,15),600),
                -1000, projIdA, -1000, -1000, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,16), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,16)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,16),600),
                -1000, projIdA, -1000, -1000, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,17), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,17)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,17),420),
                -1000, projIdA, -1000, -1000, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,18), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,18)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,18),300),
                -1000, projIdA, -1000, -1000, "WRK"));
                         
        
        ovrBuilder.setCreatesDefaultRecords(true);
        ovrBuilder.execute(true, false);
        RuleEngine.runCalcGroup(getConnection(), empId, startDate, endDate, false);
        
        // TEST INSERTION AND RULE EXECUTION
        assertOverrideAppliedCount(ovrBuilder, 6);
        assertRuleApplied(empId, DateHelper.createDate(2003,9,17), rule);
        assertRuleApplied(empId, DateHelper.createDate(2003,9,18), rule);
        WorkDetailList workPremiumList1 = getWorkPremiumsForDate(empId, DateHelper.createDate(2003,9,17));
        WorkDetailList workPremiumList2 = getWorkPremiumsForDate(empId, DateHelper.createDate(2003,9,18));
        assertEquals(ParamPremiumTimeCodeInserted, ((WorkDetailData)workPremiumList1.get(0)).getWrkdTcodeName());
        assertEquals(ParamPremiumHourTypeInserted, ((WorkDetailData)workPremiumList1.get(0)).getWrkdHtypeName());
        assertEquals(ParamPremiumTimeCodeInserted, ((WorkDetailData)workPremiumList2.get(0)).getWrkdTcodeName());
        assertEquals(ParamPremiumHourTypeInserted, ((WorkDetailData)workPremiumList2.get(0)).getWrkdHtypeName());
        int testProjId = ((WorkDetailData)workPremiumList1.get(0)).getProjId();
        if (testProjId == projIdA) {
            assertEquals(339, ((WorkDetailData)workPremiumList1.get(0)).getWrkdMinutes());
            assertEquals(projIdB, ((WorkDetailData)workPremiumList1.get(1)).getProjId());
            assertEquals(81, ((WorkDetailData)workPremiumList1.get(1)).getWrkdMinutes());
        } else if (testProjId == projIdB) {
            assertEquals(81, ((WorkDetailData)workPremiumList1.get(0)).getWrkdMinutes());
            assertEquals(projIdA, ((WorkDetailData)workPremiumList1.get(1)).getProjId());
            assertEquals(339, ((WorkDetailData)workPremiumList1.get(1)).getWrkdMinutes());
        } else {
            assertTrue(false);
        }
        testProjId = ((WorkDetailData)workPremiumList2.get(0)).getProjId();
        if (testProjId == projIdA) {
            assertEquals(242, ((WorkDetailData)workPremiumList2.get(0)).getWrkdMinutes());
            assertEquals(projIdB, ((WorkDetailData)workPremiumList2.get(1)).getProjId());
            assertEquals(58, ((WorkDetailData)workPremiumList2.get(1)).getWrkdMinutes());
        } else if (testProjId == projIdB) {
            assertEquals(58, ((WorkDetailData)workPremiumList2.get(0)).getWrkdMinutes());
            assertEquals(projIdA, ((WorkDetailData)workPremiumList2.get(1)).getProjId());
            assertEquals(242, ((WorkDetailData)workPremiumList2.get(1)).getWrkdMinutes());
        } else {
            assertTrue(false);
        }        
       
    }
    
    /*
     * Scenario 3 - OT Spans One day, Multiple Job Metrics = Job, Docket
     */
    public void testCase3() throws Exception {
            
        final int empId = 3;
        final String ParamHourSetDescription = "REG=2400, OT1=99999";
        final String ParamEligibleHourTypes = "REG";
        final String ParamDayWeekStarts = "Sunday";
        final String ParamApplyBasedOnSchedule = "False";
        final String ParamPremiumTimeCodeInserted = "WRK";
        final String ParamPremiumHourTypeInserted = "REG";
        final String allocateTimeCode = "false";
        final String allocateHourType = "false";
        final String allocateProject = "false";
        final String allocateJob = "true";
        final String allocateDocket = "true";
        final String allocateDept = "false";

        int jobIdA = createJob("OT_TEST_JOB1");
        int jobIdB = createJob("OT_TEST_JOB2");
        int dockIdA = createDocket("OT_TEST_DOCK1");
        int dockIdB = createDocket("OT_TEST_DOCK2");
        Date startDate = DateHelper.createDate(2003, 9, 14); 
        Date endDate = DateHelper.addDays(startDate, 6);
        
        // CREATE THE RULE
        Rule rule = new WeeklyOvertimeAllocationRule();
        Parameters ruleparams = new Parameters();
                
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_HOURSET_DESCRIPTION, ParamHourSetDescription);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ELIGIBLE_HOURTYPES, ParamEligibleHourTypes);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_DAY_WEEK_STARTS, ParamDayWeekStarts);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_APPLY_BASED_ON_SCHEDULE, ParamApplyBasedOnSchedule);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_PREMIUM_TIMECODE_INSERTED, ParamPremiumTimeCodeInserted);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_PREMIUM_HOUR_TYPE_INSERTED, ParamPremiumHourTypeInserted);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_TIME_CODE, allocateTimeCode);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_HOUR_TYPE, allocateHourType);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_PROJECT, allocateProject);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_JOB, allocateJob);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_DOCKET, allocateDocket);
        ruleparams.addParameter(WeeklyOvertimeAllocationRule.PARAM_ALLOCATE_DEPT, allocateDept);
               
        clearAndAddRule(empId, startDate, rule, ruleparams);
        
        // INSERT OVERRIDES: Work Detail Overrides
        CodeMapper cm = getCodeMapper();        
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.createsDefaultRecords();

        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,14), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,14)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,14),600),
                -1000, -1000, jobIdA, dockIdA, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,15), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,15)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,15),600),
                -1000, -1000, jobIdB, dockIdB, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,16), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,16)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,16),600),
                -1000, -1000, jobIdB, dockIdA, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,17), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,17)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,17),600),
                -1000, -1000, jobIdA, dockIdB, "WRK"));
        ovrBuilder.add(createWorkDetailOvr(3, DateHelper.createDate(2003,9,18), 
                DateHelper.truncateToDays(DateHelper.createDate(2003,9,18)),
                DateHelper.addMinutes(DateHelper.createDate(2003,9,18),600),
                -1000, -1000, jobIdA, dockIdA, "WRK"));
                         
        
        ovrBuilder.setCreatesDefaultRecords(true);
        ovrBuilder.execute(true, false);
        RuleEngine.runCalcGroup(getConnection(), empId, startDate, endDate, false);
        
        // TEST INSERTION AND RULE EXECUTION
        assertOverrideAppliedCount(ovrBuilder, 5);
        assertRuleApplied(empId, DateHelper.createDate(2003,9,18), rule);
        WorkDetailList workPremiumList = getWorkPremiumsForDate(empId, DateHelper.createDate(2003,9,18));
        assertEquals(ParamPremiumTimeCodeInserted, ((WorkDetailData)workPremiumList.get(0)).getWrkdTcodeName());
        assertEquals(ParamPremiumHourTypeInserted, ((WorkDetailData)workPremiumList.get(0)).getWrkdHtypeName());
        int testJobId = ((WorkDetailData)workPremiumList.get(0)).getJobId();
        int testDockId = ((WorkDetailData)workPremiumList.get(0)).getDockId();
        if (testJobId == jobIdA && testDockId == dockIdA) {
            assertEquals(240, ((WorkDetailData)workPremiumList.get(0)).getWrkdMinutes());
        } else if (testJobId == jobIdA && testDockId == dockIdB) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(0)).getWrkdMinutes());
        } else if (testJobId == jobIdB && testDockId == dockIdA) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(0)).getWrkdMinutes());
        } else if (testJobId == jobIdB && testDockId == dockIdB) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(0)).getWrkdMinutes());
        } else {
            assertTrue(false);
        }        
        testJobId = ((WorkDetailData)workPremiumList.get(1)).getJobId();
        testDockId = ((WorkDetailData)workPremiumList.get(1)).getDockId();
        if (testJobId == jobIdA && testDockId == dockIdA) {
            assertEquals(240, ((WorkDetailData)workPremiumList.get(1)).getWrkdMinutes());
        } else if (testJobId == jobIdA && testDockId == dockIdB) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(1)).getWrkdMinutes());
        } else if (testJobId == jobIdB && testDockId == dockIdA) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(1)).getWrkdMinutes());
        } else if (testJobId == jobIdB && testDockId == dockIdB) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(1)).getWrkdMinutes());
        } else {
            assertTrue(false);
        }
        testJobId = ((WorkDetailData)workPremiumList.get(2)).getJobId();
        testDockId = ((WorkDetailData)workPremiumList.get(2)).getDockId();
        if (testJobId == jobIdA && testDockId == dockIdA) {
            assertEquals(240, ((WorkDetailData)workPremiumList.get(2)).getWrkdMinutes());
        } else if (testJobId == jobIdA && testDockId == dockIdB) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(2)).getWrkdMinutes());
        } else if (testJobId == jobIdB && testDockId == dockIdA) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(2)).getWrkdMinutes());
        } else if (testJobId == jobIdB && testDockId == dockIdB) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(2)).getWrkdMinutes());
        } else {
            assertTrue(false);
        } 
        testJobId = ((WorkDetailData)workPremiumList.get(3)).getJobId();
        testDockId = ((WorkDetailData)workPremiumList.get(3)).getDockId();
        if (testJobId == jobIdA && testDockId == dockIdA) {
            assertEquals(240, ((WorkDetailData)workPremiumList.get(3)).getWrkdMinutes());
        } else if (testJobId == jobIdA && testDockId == dockIdB) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(3)).getWrkdMinutes());
        } else if (testJobId == jobIdB && testDockId == dockIdA) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(3)).getWrkdMinutes());
        } else if (testJobId == jobIdB && testDockId == dockIdB) {
            assertEquals(120, ((WorkDetailData)workPremiumList.get(3)).getWrkdMinutes());
        } else {
            assertTrue(false);
        }                
    }
    
    private int createDept(String deptName) throws Exception {
        
        DepartmentAccess deptAccess = new DepartmentAccess(getConnection());
        DepartmentData dept = null;

        dept = createDeptData(deptName);
        deptAccess.insert(dept);
        return dept.getDeptId();         

    }
    
    private int createProj(String projName) throws Exception {
        
        ProjectAccess projAccess = new ProjectAccess(getConnection());
        ProjectData proj = null;

        proj = createProjData(projName);
        projAccess.insert(proj);
        return proj.getProjId();         
    }
    
    private int createJob(String jobName) throws Exception {
        
        JobAccess jobAccess = new JobAccess(getConnection());
        JobData job = null;

        job = createJobData(jobName);
        jobAccess.insert(job);
        return job.getJobId();         
    }    
    
    private int createDocket(String dockName) throws Exception {
        
        DocketAccess dockAccess = new DocketAccess(getConnection());
        DocketData dock = null;

        dock = createDockData(dockName);
        dockAccess.insert(dock);
        return dock.getDockId();         
    }    
    
    private DepartmentData createDeptData(String deptName) {
        DepartmentData dept = new DepartmentData();
        dept.setDeptDesc("");
        dept.setDeptEndDate(DateHelper.DATE_3000);
        dept.setDeptName(deptName);
        dept.setDeptStartDate(DateHelper.DATE_1900);
        dept.setLmsId(1);
        dept.setWbtId(0);
        //dept.setDeptUnauth("N");
        return dept;
    }    
    
    private ProjectData createProjData(String projName) {
        ProjectData proj = new ProjectData();
        proj.setProjDesc("");
        proj.setProjEndDate(DateHelper.DATE_3000);
        proj.setProjName(projName);
        proj.setProjStartDate(DateHelper.DATE_1900);
        proj.setLmsId(1);
        return proj;
    }  
    
    private JobData createJobData(String jobName) {
        JobData job = new JobData();
        job.setJobDesc("");
        job.setJobEndDate(DateHelper.DATE_3000);
        job.setJobName(jobName);
        job.setJobStartDate(DateHelper.DATE_1900);
        job.setLmsId(1);
        return job;
    }    
    
    private DocketData createDockData(String dockName) {
        DocketData dock = new DocketData();
        dock.setDockDesc("");
        dock.setDockEndDate(DateHelper.DATE_3000);
        dock.setDockName(dockName);
        dock.setDockStartDate(DateHelper.DATE_1900);
        dock.setLmsId(1);
        return dock;
    }        
    private InsertWorkDetailOverride createWorkDetailOvr(int empId, Date wrksDate, Datetime start, 
            Datetime end, int deptId, int projId, int jobId, int dockId, String tCodeName) throws Exception {
        InsertWorkDetailOverride insWrkd = new InsertWorkDetailOverride(getConnection());
        insWrkd.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insWrkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insWrkd.setEmpId(empId);
        if (deptId != -1000) {
            insWrkd.setDeptId(deptId);            
        }
        if (projId != -1000) {
            insWrkd.setProjId(projId);
        }
        if (jobId != -1000) {
            insWrkd.setJobId(jobId);            
        }
        if (dockId != -1000) {
            insWrkd.setDockId(dockId);
        }        
        insWrkd.setWrkdTcodeName(tCodeName);
        insWrkd.setWrkdHtypeName("REG");
        insWrkd.setStartDate(wrksDate);
        insWrkd.setEndDate(wrksDate);
        insWrkd.setStartTime(start);
        insWrkd.setEndTime(end);
        return insWrkd;
    }


}   