package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.*;
import junit.framework.*;

/**
 * Test for ExistsOverrideTest.
 */
public class ExistsOverrideConditionTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(ExistsOverrideConditionTest.class);

    public ExistsOverrideConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ExistsOverrideConditionTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testOverrideExists() throws Exception {

        Date overrideDate = DateHelper.parseDate("07/27/2005", "MM/dd/yyyy");
        int empId = 3;

        // Insert an override.
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(overrideDate);
        ins.setEndDate(overrideDate);
        ins.setWrksUdf1("TEST");

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);


        // *** create the condition
        Condition cond = new ExistsOverrideCondition();

        // Try with start and end the same.
        Parameters params = new Parameters();
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_START, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_START));
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_END, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_END));

        assertConditionTrue(empId, overrideDate, cond, params);

        // Try with start and end as a range.
        params.removeAllParameters();
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_START, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_START));
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_END, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_END));

        assertConditionTrue(empId, overrideDate, cond, params);
    }

    /**
     * @throws Exception
     */
    public void testOverrideNotExists() throws Exception {

        Date overrideDate = DateHelper.parseDate("07/28/2005", "MM/dd/yyyy");
        int empId = 3;

        // *** create the condition
        Condition cond = new ExistsOverrideCondition();

        // Try with start and end the same.
        Parameters params = new Parameters();
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_START, String.valueOf(OverrideData.HOLIDAY_TYPE_START));
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_END, String.valueOf(OverrideData.HOLIDAY_TYPE_START));

        assertConditionFalse(empId, overrideDate, cond, params);

        // Try with start and end as a range.
        params.removeAllParameters();
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_START, String.valueOf(OverrideData.HOLIDAY_TYPE_START));
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_END, String.valueOf(OverrideData.HOLIDAY_TYPE_END));

        assertConditionFalse(empId, overrideDate, cond, params);
    }


    /**
     * @throws Exception
     */
    public void testPeriod() throws Exception {

        Date overrideDate = DateHelper.parseDate("07/27/2005", "MM/dd/yyyy");
        int empId = 3;

        // Insert an override.
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(overrideDate);
        ins.setEndDate(overrideDate);
        ins.setWrksUdf1("TEST");

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);


        // *** create the condition
        Condition cond = new ExistsOverrideCondition();

        // *** try this month
        Parameters params = new Parameters();
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_START, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_START));
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_END, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_END));
        params.addParameter(ExistsOverrideCondition.PARAM_UNIT_PERIOD, String.valueOf(DateHelper.APPLY_ON_UNIT_MONTH));

        assertConditionTrue(empId, overrideDate, cond, params);

        // *** try token
        params.removeAllParameters();
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_START, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_START));
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_END, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_END));
        params.addParameter(ExistsOverrideCondition.PARAM_UNIT_PERIOD, String.valueOf(DateHelper.APPLY_ON_UNIT_MONTH));
        params.addParameter(ExistsOverrideCondition.PARAM_NEW_VALUE_TOKEN_NAME, "WRKS_UDF1");
        params.addParameter(ExistsOverrideCondition.PARAM_NEW_VALUE_TOKEN_VALUES, "Y,X");

        assertConditionFalse(empId, overrideDate, cond, params);

        // *** try beginning of last month to current work date
        params.removeAllParameters();
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_START, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_START));
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_END, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_END));
        params.addParameter(ExistsOverrideCondition.PARAM_UNIT_PERIOD, String.valueOf(DateHelper.APPLY_ON_UNIT_MONTH));
        params.addParameter(ExistsOverrideCondition.PARAM_UNIT_VALUE_START, "-1");
        params.addParameter(ExistsOverrideCondition.PARAM_UNIT_VALUE_END, ExistsOverrideCondition.PARAM_VAL_WORK_DATE);

        assertConditionTrue(empId, overrideDate, cond, params);

        params.removeAllParameters();
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_START, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_START));
        params.addParameter(ExistsOverrideCondition.PARAM_RANGE_END, String.valueOf(OverrideData.WORK_SUMMARY_TYPE_END));
        params.addParameter(ExistsOverrideCondition.PARAM_UNIT_PERIOD, String.valueOf(DateHelper.APPLY_ON_UNIT_MONTH));
        params.addParameter(ExistsOverrideCondition.PARAM_UNIT_VALUE_START, "1");
        params.addParameter(ExistsOverrideCondition.PARAM_UNIT_VALUE_END, "1");

        assertConditionFalse(empId, overrideDate, cond, params);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
