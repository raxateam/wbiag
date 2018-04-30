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
 * Test forRoundingWorkPremiumsRule.
 */
public class RoundingWorkPremiumsRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RoundingElapsedTimeOverridesRuleTest.class);

    public RoundingWorkPremiumsRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(RoundingWorkPremiumsRuleTest.class);
        return result;
    }


    /**
     * Tests round up
     */
    public void testRound() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String premCode = "RND";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new RoundingWorkPremiumsRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(RoundingWorkPremiumsRule.PARAM_MULTIPLE, String.valueOf(15));
        ruleparams.addParameter(RoundingWorkPremiumsRule.PARAM_SPLIT, String.valueOf(7));

        clearAndAddRule(empId , start , rule , ruleparams, ExecutionPointHelper.EXECPOINT_AFTER_PRECALC_WRKP_OVERRIDES_APPLIED );

        InsertWorkPremiumOverride ins = new InsertWorkPremiumOverride(getConnection());
        ins.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START );
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setWrkdMinutes(455);
        ins.setWrkdTcodeName("TRN") ;

        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wdl = getWorkPremiumsForDate(empId , start);
        assertEquals(450, wdl.getMinutes(null, null, "TRN", true, null, true) ) ;

    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

