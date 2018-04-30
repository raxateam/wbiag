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
public class HasEmployeeClockedConditionTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HasEmployeeClockedConditionTest.class);

    public HasEmployeeClockedConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HasEmployeeClockedConditionTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testWrks() throws Exception {

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        // *** create the cond
        Condition cond = new HasEmployeeClockedCondition();
        Parameters params = new Parameters();
        params.addParameter(HasEmployeeClockedCondition.PARAM_CLOCK_MODE,
                            HasEmployeeClockedCondition.
                            PARAM_VAL_CLOCK_MODE_ALL);

        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
        String clks = createWorkSummaryClockStringForOnOffs(DateHelper.addMinutes(start, 9*60) , DateHelper.addMinutes(start, 17*60));
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(start);
        wsOvr.setEndDate(start);
        ovrBuilder.add(wsOvr);
        ovrBuilder.execute(true, false);


        assertConditionTrue(empId , start , cond , params);

        params.removeAllParameters();
        params.addParameter(HasEmployeeClockedCondition.PARAM_CLOCK_MODE,
                            HasEmployeeClockedCondition.
                            PARAM_VAL_CLOCK_MODE_READER);
        assertConditionFalse(empId , start , cond , params);
    }

    public void testCompareTypes() throws Exception {

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;


        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
        String clks = createWorkSummaryClockStringForOnOffs(DateHelper.addMinutes(start, 8*60) , DateHelper.addMinutes(start, 17*60));
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(start);
        wsOvr.setEndDate(start);
        ovrBuilder.add(wsOvr);
        ovrBuilder.execute(true, false);


        // *** create the cond
        Condition cond = new HasEmployeeClockedCondition();
        Parameters params = new Parameters();
        params.addParameter(HasEmployeeClockedCondition.PARAM_CLOCK_MODE,
                            HasEmployeeClockedCondition.
                            PARAM_VAL_CLOCK_MODE_ALL);
        params.addParameter(HasEmployeeClockedCondition.PARAM_COMPARE_TIME_OPERATOR, "<");
        params.addParameter(HasEmployeeClockedCondition.PARAM_COMPARE_TIME, EmployeeScheduleData.EMPSKD_ACT_START_TIME);

        assertConditionTrue(empId , start , cond , params);


        params.removeAllParameters();
        params.addParameter(HasEmployeeClockedCondition.PARAM_CLOCK_MODE,
                            HasEmployeeClockedCondition.
                            PARAM_VAL_CLOCK_MODE_ALL);
        params.addParameter(HasEmployeeClockedCondition.PARAM_COMPARE_TIME_OPERATOR, "<");
        params.addParameter(HasEmployeeClockedCondition.PARAM_COMPARE_TIME, EmployeeScheduleData.EMPSKD_ACT_START_TIME);
        params.addParameter(HasEmployeeClockedCondition.PARAM_CLOCK_TYPES, "2");
        assertConditionFalse(empId , start , cond , params);

    }

    public void testClockData() throws Exception {

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;


        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
        Clock clk1 = createClock(DateHelper.addMinutes(start, 8*60), 1, "JOB=JANITOR");
        Clock clk2 = createClock(DateHelper.addMinutes(start, 17*60), 2, "");
        List clksList = new ArrayList(); clksList.add(clk1);  clksList.add(clk2);
        String clks = Clock.createStringFromClockList(clksList);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(start);
        wsOvr.setEndDate(start);
        ovrBuilder.add(wsOvr);
        ovrBuilder.execute(true, false);


        // *** create the cond
        Condition cond = new HasEmployeeClockedCondition();
        Parameters params = new Parameters();
        params.addParameter(HasEmployeeClockedCondition.PARAM_CLOCK_MODE,
                            HasEmployeeClockedCondition.
                            PARAM_VAL_CLOCK_MODE_ALL);
        params.addParameter(HasEmployeeClockedCondition.PARAM_CLOCK_DATA_NAME, "JOB");
        params.addParameter(HasEmployeeClockedCondition.PARAM_CLOCK_DATA_VALUES, "JANITOR");

        assertConditionTrue(empId , start , cond , params);

        params.removeAllParameters();
        params.addParameter(HasEmployeeClockedCondition.PARAM_CLOCK_MODE,
                            HasEmployeeClockedCondition.
                            PARAM_VAL_CLOCK_MODE_ALL);
        params.addParameter(HasEmployeeClockedCondition.PARAM_CLOCK_DATA_NAME, "JOB");
        params.addParameter(HasEmployeeClockedCondition.PARAM_CLOCK_DATA_VALUES, "X,Y");

        assertConditionFalse(empId , start , cond , params);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
