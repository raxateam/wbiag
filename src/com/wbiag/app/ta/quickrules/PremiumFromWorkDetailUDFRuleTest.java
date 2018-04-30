package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for RemoveWorkDetailRule.
 */
public class PremiumFromWorkDetailUDFRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PremiumFromWorkDetailUDFRuleTest.class);

    public PremiumFromWorkDetailUDFRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(PremiumFromWorkDetailUDFRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testPrem() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String prem = "TRN";
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new PremiumFromWorkDetailUDFRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumFromWorkDetailUDFRule.PARAM_PREMIUM_TIME_CODE, prem);
        ruleparams.addParameter(PremiumFromWorkDetailUDFRule.PARAM_WORK_DETAIL_UDF, "wrkd_udf1");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime on = DateHelper.addMinutes(start, 9*60);
        Datetime off = DateHelper.addMinutes(start, 13*60);
        ins.setStartTime(on);      ins.setEndTime(off);
        ins.setWrkdUdf1("10");
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        on = DateHelper.addMinutes(start, 15*60);
        off = DateHelper.addMinutes(start, 16*60);
        ins.setStartTime(on);      ins.setEndTime(off);
        ins.setWrkdUdf1("15");
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertWorkPremiumTimeCodeMinutes(empId, start, prem , 25);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
