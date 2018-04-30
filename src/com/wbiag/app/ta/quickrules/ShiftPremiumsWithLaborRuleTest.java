package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.rules.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for ShiftPremiumsWithLaborRuleTest.
 */
public class ShiftPremiumsWithLaborRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ShiftPremiumsWithLaborRuleTest.class);

    public ShiftPremiumsWithLaborRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ShiftPremiumsWithLaborRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testSpZoneLaborHourForHour() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final int tcodeId = 10;
        final String deptName1 = "D 1";
        final String deptName2 = "D 2";
        new DepartmentAccess(getConnection()).insertDefault(deptName1 , deptName1, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        new DepartmentAccess(getConnection()).insertDefault(deptName2 , deptName2, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        getCodeMapper().invalidateDepartments();

        int spzoneId;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        spzoneId = spz.getSpzoneId();
        spz.setTcodeId(10);
        spz.setHtypeId(1);
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 0*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , 24*60));
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        // *** create the rule
        Rule rule = new ShiftPremiumsWithLaborRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumsWithLaborRule.PARAM_SHIFT_PREMIUM_ZONE_ID, String.valueOf(spzoneId));
        ruleparams.addParameter(ShiftPremiumsWithLaborRule.PARAM_PREMIUM_LABOR_METHOD,
                               PremiumWithLabor .PARAM_PLM_VAL_HOUR_FOR_HOUR_METHOD);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 60*10);
        Datetime end1 = DateHelper.addMinutes(start, 60*12);
        ins.setStartTime(st1);  ins.setEndTime(end1);
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
        ins.setWrkdDeptName(deptName2);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);


        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 4);
        //System.out.println(getWorkDetailsForDate(empId , start).toDescription());
        //System.out.println(wps.toDescription());

        assertEquals(480, wps.getMinutes(null, null, "TRN", true, null, true ) );
        WorkDetailData wd1 = wps.getWorkDetail(1); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(deptName1 , wd1.getWrkdDeptName()  );

        WorkDetailData wd2 = wps.getWorkDetail(2); wd1.setCodeMapper(getCodeMapper() );
        assertEquals(deptName2 , wd2.getWrkdDeptName()  );

    }



    private ShiftPremiumZoneData createDefaultPremiumZone() throws Exception {
        final int spzoneId = getConnection().getDBSequence("seq_spzone_id").getNextValue();
        ShiftPremiumZoneData ret = new ShiftPremiumZoneData();
        ret.setSpzoneId(spzoneId);
        ret.setSpzoneName("TEST_" + spzoneId);
        ret.setSpzoneShftToChk("B");
        ret.setSpzoneMaxMinutes(1000);
        ret.setSpzoneMaxDollars(1000);
        ret.setSpzonePercentage("N");
        ret.setSpzoneInclTcodeNames("WRK");
        ret.setSpzoneApplyType(ret.APPLY_TYPE_ZONE);
        return ret;
    }

    /**
     * @throws Exception
     */
    public void testSpZoneLaborLast() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final int tcodeId = 10;
        final String deptName1 = "D 1";
        final String deptName2 = "D 2";
        new DepartmentAccess(getConnection()).insertDefault(deptName1 , deptName1, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        new DepartmentAccess(getConnection()).insertDefault(deptName2 , deptName2, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        getCodeMapper().invalidateDepartments();

        int spzoneId;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        spzoneId = spz.getSpzoneId();
        spz.setTcodeId(10);
        spz.setHtypeId(1);
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 0*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , 24*60));
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        // *** create the rule
        Rule rule = new ShiftPremiumsWithLaborRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumsWithLaborRule.PARAM_SHIFT_PREMIUM_ZONE_ID, String.valueOf(spzoneId));
        ruleparams.addParameter(ShiftPremiumsWithLaborRule.PARAM_PREMIUM_LABOR_METHOD,
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
        ins.setWrkdDeptName(deptName2);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);


        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //assertTrue(wps.size() == 4);
        //System.out.println(getWorkDetailsForDate(empId , start).toDescription());
        //System.out.println(wps.toDescription());

        assertEquals(480, wps.getMinutes(null, null, "TRN", true, null, true ) );
        //WorkDetailData wd1 = wps.getWorkDetail(1); wd1.setCodeMapper(getCodeMapper() );
        //assertEquals(deptName1 , wd1.getWrkdDeptName()  );

        WorkDetailData wd2 = wps.getWorkDetail(0); wd2.setCodeMapper(getCodeMapper() );
        assertEquals(deptName2 , wd2.getWrkdDeptName()  );

    }

    /**
     * @throws Exception
     */
    public void testSpZoneLaborFlat() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final int tcodeId = 10;
        final String deptName1 = "D 1";
        final String deptName2 = "D 2";
        new DepartmentAccess(getConnection()).insertDefault(deptName1 , deptName1, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        new DepartmentAccess(getConnection()).insertDefault(deptName2 , deptName2, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        getCodeMapper().invalidateDepartments();

        int spzoneId;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        spzoneId = spz.getSpzoneId();
        spz.setTcodeId(10);
        spz.setHtypeId(1);
        spz.setSpzoneRate(7);
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 0*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , 24*60));
        spz.setSpzoneApplyType(ShiftPremiumZoneData.APPLY_TYPE_FLAT_RATE );
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        // *** create the rule
        Rule rule = new ShiftPremiumsWithLaborRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumsWithLaborRule.PARAM_SHIFT_PREMIUM_ZONE_ID, String.valueOf(spzoneId));
        ruleparams.addParameter(ShiftPremiumsWithLaborRule.PARAM_PREMIUM_LABOR_METHOD,
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
        ins.setWrkdDeptName(deptName2);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);


        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 1);
        //System.out.println(getWorkDetailsForDate(empId , start).toDescription());
        //System.out.println(wps.toDescription());

        assertEquals(60, wps.getMinutes(null, null, "TRN", true, null, true ) );

        WorkDetailData wd2 = wps.getWorkDetail(0); wd2.setCodeMapper(getCodeMapper() );
        assertEquals(deptName2 , wd2.getWrkdDeptName()  );
    }

    /**
     * @throws Exception
     */
    public void testSpZoneLaborFlatThreshold() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final int tcodeId = 10;
        final String deptName1 = "D 1";
        final String deptName2 = "D 2";
        new DepartmentAccess(getConnection()).insertDefault(deptName1 , deptName1, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        new DepartmentAccess(getConnection()).insertDefault(deptName2 , deptName2, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        getCodeMapper().invalidateDepartments();

        int spzoneId;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        spzoneId = spz.getSpzoneId();
        spz.setTcodeId(10);
        spz.setHtypeId(1);
        spz.setSpzoneRate(7);
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 0*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , 24*60));
        spz.setSpzoneApplyType(ShiftPremiumZoneData.APPLY_TYPE_FLAT_RATE );
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        // *** create the rule
        Rule rule = new ShiftPremiumsWithLaborRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumsWithLaborRule.PARAM_SHIFT_PREMIUM_ZONE_ID, String.valueOf(spzoneId));
        ruleparams.addParameter(ShiftPremiumsWithLaborRule.PARAM_PREMIUM_LABOR_METHOD,
                                PremiumWithLabor.PARAM_PLM_VAL_FLAT_RATE_THRESHOLD_METHOD  );
        ruleparams.addParameter(ShiftPremiumsWithLaborRule.PARAM_PREMIUM_LABOR_VALUE,
                                "60,120,300"  );

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 60*10);
        Datetime end1 = DateHelper.addMinutes(start, 60*12);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdDeptName(deptName1);
        ins.setWrkdHtypeName("OT2");
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*12);
        end1 = DateHelper.addMinutes(start, 60*17);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdDeptName(deptName2);
        ins.setWrkdTcodeName("WRK");
        ins.setWrkdHtypeName("OT2");
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        st1 = DateHelper.addMinutes(start, 60*17);
        end1 = DateHelper.addMinutes(start, 60*19);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdDeptName(deptName1);
        ins.setWrkdTcodeName("WRK");
        ins.setWrkdHtypeName("OT2");
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);


        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //assertTrue(wps.size() == 1);
        //System.out.println(getWorkDetailsForDate(empId , start).toDescription());
        //System.out.println(wps.toDescription());

        assertEquals(180, wps.getMinutes(null, null, "TRN", true, null, true ) );

        //WorkDetailData wd2 = wps.getWorkDetail(0); wd2.setCodeMapper(getCodeMapper() );
        //assertEquals(deptName2 , wd2.getWrkdDeptName()  );
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

