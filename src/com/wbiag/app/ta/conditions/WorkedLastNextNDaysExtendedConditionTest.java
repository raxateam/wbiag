package com.wbiag.app.ta.conditions;

import java.util.*;

import org.apache.log4j.Logger;

import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.InsertWorkDetailOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.*;

import junit.framework.*;

/**
 * Test for WorkedLastNextNDaysExtendedConditionTest.
 */
public class WorkedLastNextNDaysExtendedConditionTest extends RuleTestCase {

    private static Logger logger = Logger.getLogger(WorkedLastNextNDaysExtendedConditionTest.class);

    public WorkedLastNextNDaysExtendedConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkedLastNextNDaysExtendedConditionTest.class);
        return result;
    }


    /**
     * Tests if emp worked last 5 days for an ALL DAYS employee.
     * Taken from core WorkedLastNextNDays.
     * 
     * @throws Exception
     */
    public void testCoreWorkedLastNDays() throws Exception {

        final int empId = 15;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date fri = DateHelper.addDays(mon, 4);

        new CreateDefaultRecords(getConnection(), new int[] {empId}, mon, fri).execute(false);

        // *** create condition to evaluate TRUE
        Condition condition = new WorkedLastNextNDaysExtendedCondition();
        Parameters condParams = new Parameters();
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_DAYSTOLOOK , "4");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_MIN_MINUTES , "60");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_LAST_NEXT , WorkedLastNextNDaysExtendedCondition.PARAM_VAL_LAST);
        assertConditionTrue(empId, fri, condition, condParams);

        // *** create condition to evaluate FALSE
        condParams.removeAllParameters();
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_DAYSTOLOOK , "4");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_MIN_MINUTES , "520");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_LAST_NEXT , WorkedLastNextNDaysExtendedCondition.PARAM_VAL_LAST);

        assertConditionFalse(empId, DateHelper.addDays(fri , -1), condition, condParams);

    }

    public void testWorkedWithViolation() throws Exception {
    	
        final int empId = 15;
        Date friday = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "FRI");
        Date thursday = DateHelper.addDays(friday, -1);
        
        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        new CreateDefaultRecords(getConnection(), new int[] {empId}, thursday, friday).execute(false);

        // *** create condition to evaluate TRUE
        Condition condition = new WorkedLastNextNDaysExtendedCondition();
        Parameters condParams = new Parameters();
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_DAYSTOLOOK , "1");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_MIN_MINUTES , "60");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_VIOLATION_TIMECODES , "LATE");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_SCHEDULE_TYPE , WorkedLastNextNDaysExtendedCondition.PARAM_VAL_SCHEDULED_ALL);
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_LAST_NEXT , WorkedLastNextNDaysExtendedCondition.PARAM_VAL_LAST);
        
		InsertWorkDetailOverride insWrkd = new InsertWorkDetailOverride(getConnection());
		insWrkd.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insWrkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insWrkd.setEmpId(empId);
        insWrkd.setStartDate(thursday);
        insWrkd.setEndDate(thursday);
        insWrkd.setStartTime(DateHelper.addMinutes(thursday, 9*60));
        insWrkd.setEndTime(DateHelper.addMinutes(thursday, 9*60 + 15));
        insWrkd.setTcodeId(getCodeMapper().getTimeCodeByName("LATE").getTcodeId());
        insWrkd.setHtypeId(getCodeMapper().getHourTypeByName("UNPAID").getHtypeId());
        ovrBuilder.add(insWrkd);
        
        ovrBuilder.execute(true, false);
        
        assertConditionFalse(empId, friday, condition, condParams);
    }
    

    public void testWorkedNoViolationCodes() throws Exception {
    	
        final int empId = 15;
        Date friday = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "FRI");
        Date thursday = DateHelper.addDays(friday, -1);
        
        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        new CreateDefaultRecords(getConnection(), new int[] {empId}, thursday, friday).execute(false);

        // *** create condition to evaluate TRUE
        Condition condition = new WorkedLastNextNDaysExtendedCondition();
        Parameters condParams = new Parameters();
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_DAYSTOLOOK , "1");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_MIN_MINUTES , "60");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_VIOLATION_TIMECODES , "");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_SCHEDULE_TYPE , WorkedLastNextNDaysExtendedCondition.PARAM_VAL_SCHEDULED_ALL);
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_LAST_NEXT , WorkedLastNextNDaysExtendedCondition.PARAM_VAL_LAST);
        
		InsertWorkDetailOverride insWrkd = new InsertWorkDetailOverride(getConnection());
		insWrkd.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insWrkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insWrkd.setEmpId(empId);
        insWrkd.setStartDate(thursday);
        insWrkd.setEndDate(thursday);
        insWrkd.setStartTime(DateHelper.addMinutes(thursday, 9*60));
        insWrkd.setEndTime(DateHelper.addMinutes(thursday, 9*60 + 15));
        insWrkd.setTcodeId(getCodeMapper().getTimeCodeByName("LATE").getTcodeId());
        insWrkd.setHtypeId(getCodeMapper().getHourTypeByName("UNPAID").getHtypeId());
        ovrBuilder.add(insWrkd);
        
        ovrBuilder.execute(true, false);
        
        assertConditionTrue(empId, friday, condition, condParams);
    }

    public void testOneOccurance() throws Exception {
    	
        final int empId = 15;
        Date friday = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "FRI");
        Date wednesday = DateHelper.addDays(friday, -2);
        
        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        new CreateDefaultRecords(getConnection(), new int[] {empId}, wednesday, friday).execute(false);

        // *** create condition to evaluate TRUE
        Condition condition = new WorkedLastNextNDaysExtendedCondition();
        Parameters condParams = new Parameters();
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_DAYSTOLOOK , "4");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_MIN_MINUTES , "60");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_VIOLATION_TIMECODES , "");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_TCODENAME_LIST , "TRN");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_TCODE_INCLUSIVE , "TRUE");
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_SCHEDULE_TYPE , WorkedLastNextNDaysExtendedCondition.PARAM_VAL_SCHEDULED_ALL);
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_LAST_NEXT , WorkedLastNextNDaysExtendedCondition.PARAM_VAL_LAST);
        condParams.addParameter(WorkedLastNextNDaysExtendedCondition.PARAM_ONE_OCCURANCE , "true");
        
        
		InsertWorkDetailOverride insWrkd = new InsertWorkDetailOverride(getConnection());
		insWrkd.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insWrkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insWrkd.setEmpId(empId);
        insWrkd.setStartDate(wednesday);
        insWrkd.setEndDate(wednesday);
        insWrkd.setStartTime(DateHelper.addMinutes(wednesday, 9*60));
        insWrkd.setEndTime(DateHelper.addMinutes(wednesday, 12*60));
        insWrkd.setTcodeId(getCodeMapper().getTimeCodeByName("TRN").getTcodeId());
        insWrkd.setHtypeId(getCodeMapper().getHourTypeByName("REG").getHtypeId());
        ovrBuilder.add(insWrkd);
        
        ovrBuilder.execute(true, false);
        
        assertConditionTrue(empId, friday, condition, condParams);
    }
    
    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
