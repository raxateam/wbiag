package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.rules.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for ShiftPremiumExtRuleTest.
 */
public class ShiftPremiumExtRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ShiftPremiumExtRuleTest.class);

    public ShiftPremiumExtRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ShiftPremiumExtRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testSpZoneTwoShifts() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final int tcodeId = 10;
        int spzoneId;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        spzoneId = spz.getSpzoneId();
        spz.setTcodeId(10);
        spz.setHtypeId(1);
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 15*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , 24*60));
        spz.setSpzoneMwMinutes("50" + ShiftPremiumExtRule.SHIFT_PCT_SUFFIX);
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");


        // *** create the rule
        Rule rule = new ShiftPremiumExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_SHIFT_PREMIUM_ZONE_ID, String.valueOf(spzoneId));
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY, "true");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime sh1Start = DateHelper.addMinutes(start, 9*60);
        Datetime sh1End = DateHelper.addMinutes(start, 13*60);
        Datetime sh2Start = DateHelper.addMinutes(start, 19*60);
        Datetime sh2End = DateHelper.addMinutes(start, 28*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);
        ins.setEmpskdActStartTime2(sh2Start);
        ins.setEmpskdActEndTime2(sh2End);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        assertWorkPremiumTimeCodeMinutes(empId , start, "TRN", 540);
    }

    /**
     * @throws Exception
     */
    public void testSpZoneOneShiftPopDates() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final int tcodeId = 10;
        int spzoneId;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        spzoneId = spz.getSpzoneId();
        spz.setTcodeId(10);
        spz.setHtypeId(1);
        spz.setSpzoneRate(1);
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 0*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , (23*60+59)));
        spz.setSpzoneMwMinutes("50" + ShiftPremiumExtRule.SHIFT_PCT_SUFFIX);
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        // *** create the rule
        Rule rule = new ShiftPremiumExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_SHIFT_PREMIUM_ZONE_ID, String.valueOf(spzoneId));
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY, "true");
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_POPULATE_DATES, "true");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime sh1Start = DateHelper.addMinutes(start, 14*60);
        Datetime sh1End = DateHelper.addMinutes(start, 22*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        assertWorkPremiumTimeCodeMinutes(empId , start, "TRN", 480);

        WorkDetailList wpl = getWorkPremiumsForDate(empId , start);
        assertEquals(1, wpl.size() );
        WorkDetailData wd = wpl.getWorkDetail(0);
        assertEquals(sh1Start , wd.getWrkdStartTime() );
        assertEquals(sh1End , wd.getWrkdEndTime() );
    }

    /**
     * @throws Exception
     */
    public void testSpZoneOneShiftMultZonePopDates() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final int tcodeId = 10;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        int spzoneId = spz.getSpzoneId();
        spz.setTcodeId(10);
        spz.setHtypeId(1);
        spz.setSpzoneRate(25);
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 21*60));
        spz.setSpzoneEndTime(start );
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        ShiftPremiumZoneData spz2 = createDefaultPremiumZone();
        int spzoneId2 = spz2.getSpzoneId();
        spz2.setTcodeId(10);
        spz2.setHtypeId(1);
        spz2.setSpzoneRate(50);
        spz2.setSpzoneStartTime(DateHelper.addMinutes(start , 6*60));
        spz2.setSpzoneEndTime(DateHelper.addMinutes(start , 21*60));
        //spz2.setSpzoneMwMinutes("50" + ShiftPremiumExtRule.SHIFT_PCT_SUFFIX);
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz2, "SHIFT_PREMIUM_ZONE");

        // *** create the rule
        Rule rule = new ShiftPremiumExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_SHIFT_PREMIUM_ZONE_ID, (spzoneId + "," + spzoneId2));
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY, "true");
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_POPULATE_DATES, "true");
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_APPLIY_TO, ShiftPremiumExtRule.CHOICE_CURRENT_DAY);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime sh1Start = DateHelper.addMinutes(start, 16*60);
        Datetime sh1End = DateHelper.addMinutes(start, 24*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wpl = getWorkPremiumsForDate(empId , start);
        assertEquals(2, wpl.size() );
        WorkDetailData wd1 = wpl.getWorkDetail(0);
        WorkDetailData wd2 = wpl.getWorkDetail(1);
        assertEquals(sh1Start , wd1.getWrkdStartTime() );
        assertEquals(sh1End , wd2.getWrkdEndTime() );

    }

    /**
     * @throws Exception
     */
    public void testSpZoneOneShift() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final int tcodeId = 10;
        int spzoneId;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        spzoneId = spz.getSpzoneId();
        spz.setTcodeId(10);
        spz.setHtypeId(1);
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 0*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , 24*60));
        spz.setSpzoneMwMinutes("50" + ShiftPremiumExtRule.SHIFT_PCT_SUFFIX);
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        // *** create the rule
        Rule rule = new ShiftPremiumExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_SHIFT_PREMIUM_ZONE_ID, String.valueOf(spzoneId));
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY, "true");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime sh1Start = DateHelper.addMinutes(start, 14*60);
        Datetime sh1End = DateHelper.addMinutes(start, 22*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        assertWorkPremiumTimeCodeMinutes(empId , start, "TRN", 480);
    }


    /**
     * @throws Exception
     */
    public void testSpZoneMultZones() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String tcodeName1 = "TRN";
        final String tcodeName2 = "REST";
        int spzoneId1 , spzoneId2 ;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        spzoneId1 = spz.getSpzoneId();
        spz.setTcodeId(getCodeMapper().getTimeCodeByName(tcodeName1).getTcodeId());
        spz.setHtypeId(1);
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 0*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , 23*60+59));
        spz.setSpzoneRate(1);
        spz.setSpzoneBestOfGrp("A");
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        ShiftPremiumZoneData spz2 = createDefaultPremiumZone();
        spzoneId2 = spz2.getSpzoneId();
        spz2.setTcodeId(getCodeMapper().getTimeCodeByName(tcodeName2).getTcodeId());
        spz2.setHtypeId(1);
        spz2.setSpzoneStartTime(DateHelper.addMinutes(start , 0*60));
        spz2.setSpzoneEndTime(DateHelper.addMinutes(start , 23*60+59));
        spz2.setSpzoneRate(4);
        spz2.setSpzoneBestOfGrp("A");
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz2, "SHIFT_PREMIUM_ZONE");

        // *** create the rule
        Rule rule = new ShiftPremiumExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_SHIFT_PREMIUM_ZONE_ID, spzoneId1 + "," + spzoneId2 );

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime sh1Start = DateHelper.addMinutes(start, 14*60);
        Datetime sh1End = DateHelper.addMinutes(start, 22*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);

        RuleEngine.runCalcGroup(getConnection() , empId , start, start, false, true);
        assertRuleApplied(empId, start, rule);

        assertWorkPremiumTimeCodeMinutes(empId , start, tcodeName2, 480);
    }

    /**
     * Checking min minutes and dates for FLAT RATE
     * @throws Exception
     */
    public void testSpZoneFlatRateMinMinutes() throws Exception {

        final int empId = 11;
        final int tcodeId = 10; //TRN
        int spzoneId;
        final int dayShiftId = 1;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        spzoneId = spz.getSpzoneId();
        spz.setTcodeId(10);
        spz.setHtypeId(1);
        spz.setSpzoneRate(1);
        spz.setSpzoneInclTcodeNames("WRK");
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 0*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , (23*60+59)));
        spz.setSpzoneApplyType(ShiftPremiumZoneData.APPLY_TYPE_FLAT_RATE);
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        // *** create the rule
        Rule rule = new ShiftPremiumExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_SHIFT_PREMIUM_ZONE_ID, String.valueOf(spzoneId));
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_MINIMUM_NUMBER_OF_MINUTES, "1");
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_POPULATE_DATES, "true");

        clearAndAddRule(empId , start , rule , ruleparams);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setEmpskdActShiftId(dayShiftId);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);


        assertRuleApplied(empId, start, rule);

        assertWorkPremiumTimeCodeMinutes(empId , start, "TRN", 60);

        WorkDetailList wpl = getWorkPremiumsForDate(empId , start);
        assertEquals(1, wpl.size() );
        WorkDetailData wd = wpl.getWorkDetail(0);
        assertEquals(start , wd.getWrkdStartTime() );
        assertEquals(start , wd.getWrkdEndTime() );
    }

    /**
     * Checking min minutes and dates for FLAT RATE
     * @throws Exception
     */
    public void testSpZoneEffDate() throws Exception {

        final int empId = 11;
        final int tcodeId = 10; //TRN
        final int dayShiftId = 1;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        int spzoneId = getConnection().getDBSequence("seq_spzone_id").getNextValue();
        ShiftPremiumZoneData spz = new ShiftPremiumZoneData();
        spz.setSpzoneId(spzoneId);
        spz.setSpzoneName("TEST_" + spzoneId);
        spz.setSpzoneShftToChk("B");
        spz.setSpzoneMaxMinutes(1000);
        spz.setSpzoneMaxDollars(1000);
        spz.setSpzonePercentage("N");
        spz.setSpzoneInclTcodeNames("WRK");
        spz.setSpzoneApplyType(spz.APPLY_TYPE_ZONE);
        spz.setTcodeId(10);
        spz.setHtypeId(1);
        spz.setSpzoneRate(5);
        spz.setSpzoneInclTcodeNames("WRK");
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 0*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , (23*60+59)));
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        Date effDate = DateHelper.addDays(start , 1);
        WbiagSpzoneDateEffData effDateData = new WbiagSpzoneDateEffData ();
        effDateData.setSpdeId(getConnection().getDBSequence("seq_spde_id").getNextValue());
        effDateData.setSpzoneId(spzoneId) ;
        effDateData.setSpdeEffDate(effDate);
        effDateData.setSpdeValue("spzone_rate=6");
        new RecordAccess(getConnection()).insertRecordData(effDateData, "WBIAG_SPZONE_DATE_EFF");

        // *** create the rule
        Rule rule = new ShiftPremiumExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_SHIFT_PREMIUM_ZONE_ID, String.valueOf(spzoneId));
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_MINIMUM_NUMBER_OF_MINUTES, "1");
        ruleparams.addParameter(ShiftPremiumExtRule.PARAM_CHECK_EFFECTIVE_DATE, "true");

        clearAndAddRule(empId , start , rule , ruleparams);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setEmpskdActShiftId(dayShiftId);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);


        assertRuleApplied(empId, start, rule);


        WorkDetailList wpl = getWorkPremiumsForDate(empId , start);
        //System.out.println(wpl.toDescription() );
        assertEquals(1, wpl.size() );
        WorkDetailData wd = wpl.getWorkDetail(0);
        assertEquals(5 , wd.getWrkdRate() , 0);

        RuleEngine.runCalcGroup(getConnection() , empId, effDate, effDate, false);
        wpl = getWorkPremiumsForDate(empId , effDate);
        //System.out.println(wpl.toDescription() );
        wd = wpl.getWorkDetail(0);
        assertEquals(6 , wd.getWrkdRate() , 0);
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

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

