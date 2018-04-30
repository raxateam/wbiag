package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.BasicConfigurator;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for PremiumWithLaborRuleTest.
 */
public class PremiumWithLaborRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PremiumWithLaborRuleTest.class);

    public PremiumWithLaborRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(PremiumWithLaborRuleTest.class);
        BasicConfigurator.configure();
        return result;
    }


    /**
     * @throws Exception
     */
    public void testJobWithLabor() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String jobName = "JANITOR";
        final String deptName1 = "D 1";
        final String deptName2 = "D 2";
        new DepartmentAccess(getConnection()).insertDefault(deptName1 , deptName1, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        new DepartmentAccess(getConnection()).insertDefault(deptName2 , deptName2, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        getCodeMapper().invalidateDepartments();

        final String premCode = "TRN";
        final double premRate = 10;
        final int maxMins = 300;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new PremiumWithLaborRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_ELIGIBLE_JOBNAMES, jobName);
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_USE_DAY_MAX_MINUTES, String.valueOf(maxMins));
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_PREMIUM_LABOR_METHOD,
                                PremiumWithLabor.PARAM_PLM_VAL_HOUR_FOR_HOUR_METHOD);
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 60*10);
        Datetime end1 = DateHelper.addMinutes(start, 60*12);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdJobName(jobName);
        ins.setWrkdDeptName(deptName1);
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*12);
        end1 = DateHelper.addMinutes(start, 60*16);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdJobName(jobName);
        ins.setWrkdDeptName(deptName2);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        //System.out.println(getWorkDetailsForDate(empId , start).toDescription() );

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 2);
        //System.out.println(wps.toDescription());
        assertEquals(maxMins, wps.getMinutes(null, null, premCode, true, null, true ) );
        WorkDetailData wd1 = wps.getWorkDetail(0); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(deptName1 , wd1.getWrkdDeptName()  );

        WorkDetailData wd2 = wps.getWorkDetail(1); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(deptName2 , wd2.getWrkdDeptName()  );
    }

    /**
     * @throws Exception
     */
    public void testJobWithLaborLast() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String jobName = "JANITOR";
        final String deptName1 = "D 1";
        final String deptName2 = "D 2";
        new DepartmentAccess(getConnection()).insertDefault(deptName1 , deptName1, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        new DepartmentAccess(getConnection()).insertDefault(deptName2 , deptName2, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        getCodeMapper().invalidateDepartments();

        final String premCode = "TRN";
        final double premRate = 10;
        final int maxMins = 300;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new PremiumWithLaborRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_ELIGIBLE_JOBNAMES, jobName);
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_USE_DAY_MAX_MINUTES, String.valueOf(maxMins));
        ruleparams.addParameter(PremiumWithLaborRule.PARAM_PREMIUM_LABOR_METHOD,
                                PremiumWithLabor.PARAM_PLM_VAL_LAST_METHOD);
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 60*10);
        Datetime end1 = DateHelper.addMinutes(start, 60*12);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdJobName(jobName);
        ins.setWrkdDeptName(deptName1);
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*12);
        end1 = DateHelper.addMinutes(start, 60*17);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdJobName(jobName);
        ins.setWrkdDeptName(deptName2);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        //System.out.println(getWorkDetailsForDate(empId , start).toDescription() );

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 1);
        System.out.println(wps.toDescription());
        assertEquals(maxMins, wps.getMinutes(null, null, premCode, true, null, true ) );
        //WorkDetailData wd1 = wps.getWorkDetail(0); wd1.setCodeMapper(getCodeMapper() );
        //assertEquals(deptName1 , wd1.getWrkdDeptName()  );

        WorkDetailData wd2 = wps.getWorkDetail(0); wd2.setCodeMapper(getCodeMapper() );
        assertEquals(deptName2 , wd2.getWrkdDeptName()  );
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
