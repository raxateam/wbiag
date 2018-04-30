package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for IsWorkSummaryPropertyGenericTest.
 */
public class IsSplitShiftConditionTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsSplitShiftConditionTest.class);

    public IsSplitShiftConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsSplitShiftConditionTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testCond() throws Exception {

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;


        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(getConnection());
        insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insSkd.setEmpId(empId);
        insSkd.setStartDate(start);      insSkd.setEndDate(start);

        Datetime s1Start = DateHelper.addMinutes(start, 10*60);
        Datetime s1End = DateHelper.addMinutes(start, 18*60);

        Datetime s2Start = DateHelper.addMinutes(start, 20*60);
        Datetime s2End = DateHelper.addMinutes(start, 22*60);

        insSkd.setEmpskdActStartTime(s1Start);      insSkd.setEmpskdActEndTime(s1End);
        insSkd.setEmpskdActStartTime2(s2Start);      insSkd.setEmpskdActEndTime2(s2End);

        ovrBuilder.add(insSkd);
        ovrBuilder.execute(true , false);

        // *** create the cond
        Condition cond = new IsSplitShiftCondition();
        Parameters params = new Parameters();
        params.addParameter(IsSplitShiftCondition.PARAM_MINS_BETWEEN, "60");
        params.addParameter(IsSplitShiftCondition.PARAM_NUMBER_OF_SHIFTS, "2");


        assertConditionTrue(empId , start , cond , params);

        params.removeAllParameters();
        //params.addParameter(IsSplitShiftCondition.PARAM_CLOCK_MODE,
        //                    IsSplitShiftCondition.
        //                    PARAM_VAL_CLOCK_MODE_READER);
        //assertConditionFalse(empId , start , cond , params);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
