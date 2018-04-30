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
 * Test for ShiftPremiumsWDatesRuleTest.
 */
public class ShiftPremiumsWDatesRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ShiftPremiumsWDatesRuleTest.class);

    public ShiftPremiumsWDatesRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ShiftPremiumsWDatesRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testSpZoneTwoShifts() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final int tcodeId = 10;
        int spzoneId;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        spzoneId = spz.getSpzoneId();
        spz.setTcodeId(10);
        spz.setHtypeId(0);
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 15*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , 24*60));
        spz.setSpzoneMwMinutes("50");
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");


        // *** create the rule
        Rule rule = new ShiftPremiumsWDatesRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumsWDatesRule.PARAM_SHIFT_PREMIUM_ZONE_ID, String.valueOf(spzoneId));
        ruleparams.addParameter(ShiftPremiumsWDatesRule.PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY, "true");

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

        //assertRuleApplied(empId, start, rule);

        assertWorkPremiumTimeCodeMinutes(empId , start, "TRN", 300);
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
        spz.setHtypeId(0);
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 15*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , 24*60));
        spz.setSpzoneMwMinutes("50");
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        // *** create the rule
        Rule rule = new ShiftPremiumsWDatesRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumsWDatesRule.PARAM_SHIFT_PREMIUM_ZONE_ID, String.valueOf(spzoneId));
        ruleparams.addParameter(ShiftPremiumsWDatesRule.PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY, "true");

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

        //assertRuleApplied(empId, start, rule);

        assertWorkPremiumTimeCodeMinutes(empId , start, "TRN", 420);
    }


    /**
     * Not Currently supported in this Rule
     * @throws Exception

    public void testSpZoneMultZones() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String tcodeName1 = "TRN";
        final String tcodeName2 = "PREM";
        int spzoneId1 , spzoneId2 ;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        ShiftPremiumZoneData spz = createDefaultPremiumZone();
        spzoneId1 = spz.getSpzoneId();
        spz.setTcodeId(getCodeMapper().getTimeCodeByName(tcodeName1).getTcodeId());
        spz.setSpzoneStartTime(DateHelper.addMinutes(start , 14*60));
        spz.setSpzoneEndTime(DateHelper.addMinutes(start , 24*60));
        spz.setSpzoneRate(1);
        spz.setSpzoneBestOfGrp("A");
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz, "SHIFT_PREMIUM_ZONE");

        ShiftPremiumZoneData spz2 = createDefaultPremiumZone();
        spzoneId2 = spz2.getSpzoneId();
        spz2.setTcodeId(getCodeMapper().getTimeCodeByName(tcodeName2).getTcodeId());
        spz2.setSpzoneStartTime(DateHelper.addMinutes(start , 16*60));
        spz2.setSpzoneEndTime(DateHelper.addMinutes(start , 24*60));
        spz2.setSpzoneRate(4);
        spz2.setSpzoneBestOfGrp("A");
        new ShiftPremiumZoneAccess(getConnection()).insertRecordData(spz2, "SHIFT_PREMIUM_ZONE");

        // *** create the rule
        Rule rule = new ShiftPremiumsWDatesRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ShiftPremiumsWDatesRule.PARAM_SHIFT_PREMIUM_ZONE_ID, spzoneId1 + "," + spzoneId2 );

        clearAndAddRule(empId , start , rule , ruleparams);

        RuleEngine.runCalcGroup(getConnection() , empId , start, start, false, true);
        //assertRuleApplied(empId, start, rule);

        assertWorkPremiumTimeCodeMinutes(empId , start, tcodeName2, 300);
    }
*/
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

