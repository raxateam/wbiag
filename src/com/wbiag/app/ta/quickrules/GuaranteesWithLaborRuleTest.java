package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;

import junit.framework.*;
/**
 * Test for GuaranteesWithLaborRule.
 */
public class GuaranteesWithLaborRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GuaranteesWithLaborRuleTest.class);

    public GuaranteesWithLaborRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(GuaranteesWithLaborRuleTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testGuaranteeDefault() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String guarCode = "GUAR" , checkCode = "TRN";
        final String job1 = "JANITOR" , job2 = "CLERK";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new GuaranteesWithLaborRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_GUARANTEED_MINUTES, "240");
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(480));
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(60));
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_INDEPENDENT_OCCURRENCES, "true");
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_TIMECODELIST, checkCode);
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_PREMIUM_LABOR_METHOD,
                                PremiumWithLabor.PARAM_PLM_VAL_DEFAULT_METHOD);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 60*10);
        Datetime end1 = DateHelper.addMinutes(start, 60*12);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job1 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*12);
        end1 = DateHelper.addMinutes(start, 60*13);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job2 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);


        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*14);
        end1 = DateHelper.addMinutes(start, 60*15);
        ins.setStartTime(st1);  ins.setEndTime(end1);

        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job2 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);
        //System.out.println( getWorkDetailsForDate(empId , start).toDescription());

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //System.out.println(wps.toDescription());

        assertTrue(wps.size() == 3);
        assertEquals(480, wps.getMinutes(null, null, guarCode, true, null, true ) );

    }


    /**
     * @throws Exception
     */
    public void testGuaranteeIndepFalseProrated() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String guarCode = "GUAR" , checkCode = "TRN";
        final String job1 = "JANITOR" , job2 = "CLERK", job3 = "WELDER";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new GuaranteesWithLaborRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_GUARANTEED_MINUTES, "240");
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(0));
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(0));
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_INDEPENDENT_OCCURRENCES, "false");
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_TIMECODELIST, checkCode);
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_PREMIUM_LABOR_METHOD,
                                PremiumWithLabor.PARAM_PLM_VAL_PRORATED_METHOD);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 60*10);
        Datetime end1 = DateHelper.addMinutes(start, 60*12);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job1 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*12);
        end1 = DateHelper.addMinutes(start, 60*13);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job2 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*13);
        end1 = DateHelper.addMinutes(start, 60*13 + 30);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job3 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //System.out.println(getWorkDetailsForDate(empId , start).toDescription());
        //System.out.println(wps.toDescription());
        assertTrue(wps.size() == 3);
        assertEquals(30, wps.getMinutes(null, null, guarCode, true, null, true ) );

        WorkDetailData wd1 = wps.getWorkDetail(0); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(job1 , wd1.getWrkdJobName()  );

        WorkDetailData wd2 = wps.getWorkDetail(1); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(job2 , wd2.getWrkdJobName()  );

    }

    /**
     * @throws Exception
     */
    public void testGuaranteeIndepFalseLast() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String guarCode = "GUAR" , checkCode = "TRN";
        final String job1 = "JANITOR" , job2 = "CLERK", job3 = "WELDER";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new GuaranteesWithLaborRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_GUARANTEED_MINUTES, "240");
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(0));
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(0));
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_INDEPENDENT_OCCURRENCES, "false");
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_TIMECODELIST, checkCode);
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_PREMIUM_LABOR_METHOD,
                                PremiumWithLabor.PARAM_PLM_VAL_LAST_METHOD);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 60*10);
        Datetime end1 = DateHelper.addMinutes(start, 60*12);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job1 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*12);
        end1 = DateHelper.addMinutes(start, 60*13);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job2 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*13);
        end1 = DateHelper.addMinutes(start, 60*13 + 30);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job3 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //System.out.println(getWorkDetailsForDate(empId , start).toDescription());
        //System.out.println(wps.toDescription());
        assertTrue(wps.size() == 1);
        assertEquals(30, wps.getMinutes(null, null, guarCode, true, null, true ) );

        WorkDetailData wd1 = wps.getWorkDetail(0); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(job3 , wd1.getWrkdJobName()  );


    }

    /**
     * @throws Exception
     */
    public void testGuaranteeIndepTrueHourForHour() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String guarCode = "GUAR" , checkCode = "TRN";
        final String job1 = "JANITOR" , job2 = "CLERK";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new GuaranteesWithLaborRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_GUARANTEED_MINUTES, "240");
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(480));
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(60));
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_INDEPENDENT_OCCURRENCES, "true");
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_TIMECODELIST, checkCode);
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_PREMIUM_LABOR_METHOD,
                                PremiumWithLabor.PARAM_PLM_VAL_HOUR_FOR_HOUR_METHOD);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 60*10);
        Datetime end1 = DateHelper.addMinutes(start, 60*12);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job1 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*12);
        end1 = DateHelper.addMinutes(start, 60*13);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job2 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);


        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*14);
        end1 = DateHelper.addMinutes(start, 60*15);
        ins.setStartTime(st1);  ins.setEndTime(end1);

        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job2 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);
        //System.out.println( getWorkDetailsForDate(empId , start).toDescription());

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //System.out.println(wps.toDescription());

        assertTrue(wps.size() == 3);
        assertEquals(480, wps.getMinutes(null, null, guarCode, true, null, true ) );

        WorkDetailData wd1 = wps.getWorkDetail(0); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(job1 , wd1.getWrkdJobName()  );

        WorkDetailData wd2 = wps.getWorkDetail(1); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(job2 , wd2.getWrkdJobName()  );

        WorkDetailData wd3 = wps.getWorkDetail(2); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(job2 , wd3.getWrkdJobName()  );

    }

    /**
     * @throws Exception
     */
    public void testGuaranteeIndepTrueLast() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String guarCode = "GUAR" , checkCode = "TRN";
        final String job1 = "JANITOR" , job2 = "CLERK";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new GuaranteesWithLaborRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_GUARANTEED_MINUTES, "240");
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(480));
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(60));
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_INDEPENDENT_OCCURRENCES, "true");
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_TIMECODELIST, checkCode);
        ruleparams.addParameter(GuaranteesWithLaborRule.PARAM_PREMIUM_LABOR_METHOD,
                                PremiumWithLabor.PARAM_PLM_VAL_LAST_METHOD);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 60*10);
        Datetime end1 = DateHelper.addMinutes(start, 60*12);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job1 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*12);
        end1 = DateHelper.addMinutes(start, 60*13);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job2 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);


        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*14);
        end1 = DateHelper.addMinutes(start, 60*15);
        ins.setStartTime(st1);  ins.setEndTime(end1);

        ins.setWrkdTcodeName(checkCode);
        ins.setWrkdJobName(job2 );
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);
        //System.out.println( getWorkDetailsForDate(empId , start).toDescription());

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //System.out.println(wps.toDescription());

        assertTrue(wps.size() == 3);
        assertEquals(480, wps.getMinutes(null, null, guarCode, true, null, true ) );

        WorkDetailData wd1 = wps.getWorkDetail(0); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(job2 , wd1.getWrkdJobName()  );

        WorkDetailData wd2 = wps.getWorkDetail(1); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(job2 , wd2.getWrkdJobName()  );

        WorkDetailData wd3 = wps.getWorkDetail(2); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(job2 , wd3.getWrkdJobName()  );

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

