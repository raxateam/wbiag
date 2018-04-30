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
 * Test for RoundingElapsedTimeOverridesRule.
 */
public class RoundingElapsedTimeOverridesRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RoundingElapsedTimeOverridesRuleTest.class);

    public RoundingElapsedTimeOverridesRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(RoundingElapsedTimeOverridesRuleTest.class);
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
        Rule rule = new RoundingElapsedTimeOverridesRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(RoundingElapsedTimeOverridesRule.PARAM_MULTIPLE, String.valueOf(15));
        ruleparams.addParameter(RoundingElapsedTimeOverridesRule.PARAM_SPLIT, String.valueOf(7));

        clearAndAddRule(empId , start , rule , ruleparams, ExecutionPointHelper.EXECPOINT_AFTER_WRKS_OVERRIDES_APPLIED );

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setOvrType(OverrideData.TIMESHEET_TYPE_START );
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setWrkdMinutes(455);
        ins.setWrkdTcodeName("WRK") ;

        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.TIMESHEET_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setWrkdMinutes(62);
        ins2.setWrkdTcodeName("TRN") ;

        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wdl = getWorkDetailsForDate(empId , start);
        assertEquals(450, wdl.getMinutes(null, null, "WRK", true, null, true) ) ;

        OverrideList ovrs = new OverrideAccess (getConnection()).load(empId, start , start);
        assertTrue(ovrs.size() == 2);
        OverrideData od = ovrs.getOverrideData(0);
        OverrideData.OverrideToken ot = od.getNewOverrideByName("WRKD_MINUTES");
        assertTrue(ot != null);
        assertEquals("450", ot.getValue());
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

