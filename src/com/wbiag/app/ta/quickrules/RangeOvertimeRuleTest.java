package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for RangeOvertimeRuleTest.
 */
public class RangeOvertimeRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RangeOvertimeRuleTest.class);

    public RangeOvertimeRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(RangeOvertimeRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testRangeOvertimeHourset() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date monthStart = DateHelper.getUnitMonth(DateHelper.APPLY_ON_FIRST_DAY ,
                                                  false,
                                                  DateHelper.getCurrentDate());
        Date monthEnd = DateHelper.getUnitMonth("15" ,
                                                false,
                                                DateHelper.getCurrentDate());

        Date start = monthEnd;
        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 monthStart, DateHelper.addDays(monthEnd, 6)).execute(false);
        // *** create the rule
        Rule rule = new RangeOvertimeRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(RangeOvertimeRule.PARAM_HOURSET_DESCRIPTION, "REG=0,OT2=99999");
        ruleparams.addParameter(RangeOvertimeRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(RangeOvertimeRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(RangeOvertimeRule.PARAM_APPLY_ON_UNIT, DateHelper.APPLY_ON_UNIT_MONTH);
        ruleparams.addParameter(RangeOvertimeRule.PARAM_APPLY_ON_VALUE_START, DateHelper.APPLY_ON_FIRST_DAY);
        ruleparams.addParameter(RangeOvertimeRule.PARAM_APPLY_ON_VALUE_END, "15");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_SUMMARY_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 540);
        Datetime clk1Off = DateHelper.addMinutes(start, 1200);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        if (getEmployeeScheduleData(empId , start).isEmployeeScheduledActual()) {
            assertTrue(wds.size() == 2);
            WorkDetailData wd1 = wds.getWorkDetail(0);
            assertEquals("REG", wd1.getWrkdHtypeName());
            assertEquals(480, wd1.getWrkdMinutes());
            WorkDetailData wd2 = wds.getWorkDetail(1);
            assertEquals("OT2", wd2.getWrkdHtypeName());
            assertEquals(180, wd2.getWrkdMinutes());
        }
        else {
            assertTrue(wds.size() == 1);
            WorkDetailData wd1 = wds.getWorkDetail(0);
            assertEquals("OT2", wd1.getWrkdHtypeName());
            assertEquals(660, wd1.getWrkdMinutes());
        }

    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
