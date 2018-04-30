package com.wbiag.app.ta.ruleengine;

import java.util.*;
import java.sql.*;
import java.util.Date;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import com.workbrain.app.clockInterface.processing.WBClockProcessTask;

import com.wbiag.app.ta.quickrules.*;
import junit.framework.*;

import com.wbiag.app.ta.ruleengine.DataEventFlexShift;

/**
 * Test for CustomerDataEventMergePremiumTest.
 */
public class DataEventFlexShiftTest
    extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(DataEventFlexShiftTest.class);

    public DataEventFlexShiftTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(DataEventFlexShiftTest.class);
        return result;
    }

    public void testDummy() throws Exception {
        assertTrue("Dummy Test until tests are done", 1 == 1);
    }


    public void xWrksOvr() throws Exception {
        DBConnection conn = this.getConnection();
        final int empId = 10038;
        final String shiftName = "FLEX_JUNIT";
        Date start = DateHelper.parseSQLDate("2004-11-09");
        Date startTime = DateHelper.convertStringToDate("11/09/2004 08:00:00",
            "MM/dd/yyyy hh:mm:ss");
        Date endTime = DateHelper.convertStringToDate("11/09/2004 16:00:00",
            "MM/dd/yyyy hh:mm:ss");

        //Set up data event class
        setDataEventClassPath("com.wbiag.app.ta.ruleengine.DataEventFlexShift");

        //Create shift
        createShift(shiftName, conn, start);

        //Insert employee override, assigning employee to the previously created shift
        OverrideBuilder ob = new OverrideBuilder(conn);

        InsertEmployeeOverride ins = new InsertEmployeeOverride(conn);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(DateHelper.DATE_1900);
        ins.setEndDate(DateHelper.DATE_3000);
        ins.setEmpFirstname("Workbrain");
        ins.setEmpLastname("Support");
        ins.setEmpShftpatName(shiftName);

        ob.add(ins);

        //Insert work summary override with clocks
        InsertWorkSummaryOverride ins2 = new InsertWorkSummaryOverride(conn);

        ins2.setStartDate(start);
        ins2.setEndDate(start);
        ins2.setEmpId(empId);
        ins2.setOvrType(OverrideData.WORK_SUMMARY_TYPE_START);

        ins2.setWrksClocks("~XX2004110908000001TCODE=WRK~XX2004110916000002~");
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");

        ob.add(ins2);

        try {
            ob.execute(true , false);

        }
        catch (OverrideException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            throw new Exception();
        }

        CodeMapper cm = null;
        EmployeeAccess ea = new EmployeeAccess(conn, cm);
        EmployeeData ed = ea.load(empId, start);
        EmployeeScheduleAccess esa = new EmployeeScheduleAccess(conn, cm);
        EmployeeScheduleData esd = esa.load(ed, start);
        System.out.println(esd.getEmpskdActStartTime() + "****************");
        assertTrue(esd.isScheduledTimesOverridden(0, conn));
        assertEquals(esd.getEmpskdActEndTime(), endTime);
        assertEquals(esd.getEmpskdActStartTime(), startTime);

        //Cancel Schedule Overrides for this day
        OverrideAccess oa = new OverrideAccess(conn);
        oa.cancelByRangeAndType(empId, start, start,
                                OverrideData.SCHEDULE_SCHEDTIMES_TYPE,
                                OverrideData.SCHEDULE_SCHEDTIMES_TYPE, null);
        oa.cancelByRangeAndType(empId, start, start,
                                OverrideData.WORK_SUMMARY_TYPE_START,
                                OverrideData.WORK_SUMMARY_TYPE_START, null);

        //Delete all Overrides for this day.
        DBServer dbs = DBServer.getServer(conn);
        PreparedStatement ps;

        StringBuffer delOvrSql = new StringBuffer();
        delOvrSql.append("delete from override where ovr_start_date = ");
        delOvrSql.append(dbs.encodeTimestamp(start));
        ps = conn.prepareStatement(delOvrSql.toString());
        ps.executeQuery();
        conn.commit();
        ps.close();

    }
/* Not a final test
    public void testSecondClkSameTypeOvr() throws Exception {
        DBConnection conn = this.getConnection();
        final int empId = 10038;
        Date start = DateHelper.parseSQLDate("2004-11-09");
        Date startTime = DateHelper.convertStringToDate("11/09/2004 00:00:00",
            "MM/dd/yyyy hh:mm:ss");
        Date endTime = DateHelper.getLatestEmployeeDate();

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.DataEventFlexShift");

        Date EmpskdActStartTime = DateHelper.convertStringToDate(
            "11/09/2004 09:00:00",
            "MM/dd/yyyy hh:mm:ss");
        Date EmpskdActEndTime = DateHelper.convertStringToDate(
            "11/09/2004 17:00:00",
            "MM/dd/yyyy hh:mm:ss");

        OverrideBuilder ob = new OverrideBuilder(conn);
        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(conn);

        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setEmpId(empId);
        ins.setOvrType(OverrideData.WORK_SUMMARY_TYPE_START);

        ins.setWrksClocks("~XX2004110909000001TCODE=WRK~XX2004110917000002~");
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");

        ob.add(ins);

        try {
            ob.execute(true , false);

        }
        catch (OverrideException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            throw new Exception();
        }


        EmpskdActStartTime = DateHelper.convertStringToDate(
            "11/09/2004 09:15:00",
            "MM/dd/yyyy hh:mm:ss");
        EmpskdActEndTime = DateHelper.convertStringToDate(
            "11/09/2004 17:15:00",
            "MM/dd/yyyy hh:mm:ss");

        OverrideBuilder ob2 = new OverrideBuilder(conn);
        InsertWorkSummaryOverride ins2 = new InsertWorkSummaryOverride(conn);

        ins2.setStartDate(start);
        ins2.setEndDate(start);
        ins2.setEmpId(empId);
        ins2.setOvrType(OverrideData.WORK_SUMMARY_TYPE_START);

        ins2.setWrksClocks("~XX2004110909150001TCODE=WRK~XX2004110917150002~");
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");

        ob2.add(ins2);

        try {
            ob2.execute(true);

        }
        catch (OverrideException ex) {
            ex.printStackTrace();
            throw new Exception("Error Creading Override: " + ex.getMessage());
        }

        CodeMapper cm = null;
        EmployeeAccess ea = new EmployeeAccess(conn, cm);
        EmployeeData ed = ea.load(empId, start);
        EmployeeScheduleAccess esa = new EmployeeScheduleAccess(conn, cm);
        EmployeeScheduleData esd = esa.load(ed, start);

        assertTrue(esd.isScheduledTimesOverridden(0, conn));
        assertEquals(EmpskdActStartTime, esd.getEmpskdActStartTime());
        assertEquals(EmpskdActEndTime, esd.getEmpskdActEndTime());

        //Cancel Schedule Overrides for this day
        OverrideAccess oa = new OverrideAccess(conn);
        oa.cancelByRangeAndType(empId, start, start,
                                OverrideData.WORK_SUMMARY_TYPE_START,
                                OverrideData.WORK_SUMMARY_TYPE_START, null);
        oa.cancelByRangeAndType(empId, start, start,
                                OverrideData.SCHEDULE_SCHEDTIMES_TYPE,
                                OverrideData.SCHEDULE_SCHEDTIMES_TYPE, null);

        //Delete all Overrides for this day.
        DBServer dbs = DBServer.getServer(conn);
        PreparedStatement ps;
        StringBuffer delOvrSql = new StringBuffer();
        delOvrSql.append("delete from override where ovr_start_date = ");
        delOvrSql.append(dbs.encodeTimestamp(start));
        ps = conn.prepareStatement(delOvrSql.toString());
        ps.executeQuery();
        conn.commit();
        ps.close();

    }
*/

    public void xClockOverride() throws Exception {
        DBConnection conn = this.getConnection();
        final int empId = 10038;
        Date start = DateHelper.parseSQLDate("2004-11-09");
        Date startTime = DateHelper.convertStringToDate("11/09/2004 09:00:00",
            "MM/dd/yyyy hh:mm:ss");
        Date endTime = DateHelper.convertStringToDate("11/09/2004 17:00:00",
            "MM/dd/yyyy hh:mm:ss");

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.DataEventFlexShift");

        Date clkOn = startTime; //DateHelper.addMinutes(start, 550);
        Date clkOff = endTime; //DateHelper.addMinutes(start, 1020);
        ClockTranAccess access = new ClockTranAccess(getConnection());
        ClockTranPendJData data1 = new ClockTranPendJData();

        data1.setCtpjIdentifier(Integer.toString(empId));
        data1.setCtpjIdentType("I");
        data1.setCtpjTime(DateHelper.convertDateString(clkOn,
            Clock.CLOCKDATEFORMAT_STRING));
        data1.setCtpjRdrName("VIRTUAL READER");
        data1.setCtpjType(Integer.toString(Clock.TYPE_ON));
        access.insert(data1);

        ClockTranPendJData data2 = new ClockTranPendJData();
        data2.setCtpjIdentifier(Integer.toString(empId));
        data2.setCtpjIdentType("I");
        data2.setCtpjTime(DateHelper.convertDateString(clkOff,
            Clock.CLOCKDATEFORMAT_STRING));
        data2.setCtpjRdrName("VIRTUAL READER");
        data2.setCtpjType(Integer.toString(Clock.TYPE_OFF));
        access.insert(data2);

        WBClockProcessTask task = new WBClockProcessTask();
        task.setShouldCommit(false);
        task.setCheckForInterrupt(false);
        task.execute(getConnection());

        CodeMapper cm = null;
        EmployeeAccess ea = new EmployeeAccess(conn, cm);
        EmployeeData ed = ea.load(empId, start);
        EmployeeScheduleAccess esa = new EmployeeScheduleAccess(conn, cm);
        EmployeeScheduleData esd = esa.load(ed, start);

        assertTrue(esd.isScheduledTimesOverridden(0, conn));
        assertEquals(esd.getEmpskdActEndTime(), endTime);
        assertEquals(esd.getEmpskdActStartTime(), startTime);

        //Cancel Schedule Overrides for this day
        OverrideAccess oa = new OverrideAccess(conn);
        oa.cancelByRangeAndType(empId, start, start,
                                OverrideData.SCHEDULE_SCHEDTIMES_TYPE,
                                OverrideData.SCHEDULE_SCHEDTIMES_TYPE, null);

        //Delete all Overrides for this day.
        DBServer dbs = DBServer.getServer(conn);
        PreparedStatement ps;
        StringBuffer delOvrSql = new StringBuffer();
        delOvrSql.append("delete from override where ovr_start_date = ");
        delOvrSql.append(dbs.encodeTimestamp(start));
        ps = conn.prepareStatement(delOvrSql.toString());
        System.out.println(ps.executeQuery());
        conn.commit();
        ps.close();

    }

    public void xCodeOvr() throws Exception {
        DBConnection conn = this.getConnection();
        final int empId = 10038;
        Date start = DateHelper.parseSQLDate("2004-11-09");
        Date startTime = DateHelper.convertStringToDate("11/09/2004 09:00:00",
            "MM/dd/yyyy hh:mm:ss");
        Date endTime = DateHelper.convertStringToDate("11/09/2004 17:00:00",
            "MM/dd/yyyy hh:mm:ss");

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.DataEventFlexShift");

        OverrideBuilder ob = new OverrideBuilder(conn);
        InsertOverride ins = new InsertOverride();

        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setEmpId(empId);
        ins.setOvrType(103);
        ins.setStartTime(startTime);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");

        ob.add(ins);

        try {
            ob.execute(true , false);

        }
        catch (OverrideException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            throw new Exception();
        }

        CodeMapper cm = null;
        EmployeeAccess ea = new EmployeeAccess(conn, cm);
        EmployeeData ed = ea.load(empId, start);
        EmployeeScheduleAccess esa = new EmployeeScheduleAccess(conn, cm);
        EmployeeScheduleData esd = esa.load(ed, start);

        assertTrue(esd.isScheduledTimesOverridden(0, conn));
        assertEquals(esd.getEmpskdActEndTime(), endTime);
        assertEquals(esd.getEmpskdActStartTime(), startTime);

        //Cancel Schedule Overrides for this day
        OverrideAccess oa = new OverrideAccess(conn);
        oa.cancelByRangeAndType(empId, start, start,
                                OverrideData.WORK_SUMMARY_TYPE_START,
                                OverrideData.WORK_SUMMARY_TYPE_START, null);
        oa.cancelByRangeAndType(empId, start, start,
                                OverrideData.SCHEDULE_SCHEDTIMES_TYPE,
                                OverrideData.SCHEDULE_SCHEDTIMES_TYPE, null);

        //Delete all Overrides for this day.
        DBServer dbs = DBServer.getServer(conn);
        PreparedStatement ps;
        StringBuffer delOvrSql = new StringBuffer();
        delOvrSql.append("delete from override where ovr_start_date = ");
        delOvrSql.append(dbs.encodeTimestamp(start));
        ps = conn.prepareStatement(delOvrSql.toString());
        ps.executeQuery();
        conn.commit();
        ps.close();

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

    private void createShift(String shiftName, DBConnection c, Date start) throws Exception {
        int shiftId  = 10050; //c.getDBSequence("seq_shift_id").getNextValue();
        ShiftData sd = new ShiftData();
        sd.setShftStartTime(DateHelper.addMinutes(start, 7 * 60));
        sd.setShftEndTime(DateHelper.addMinutes(start, 17 * 60));
        sd.setShftId(shiftId);
        sd.setShftName(shiftName);
        sd.setGeneratesPrimaryKeyValue(true);
        sd.setShftDesc(shiftName);
        sd.setShftgrpId(0);
        ShiftAccess sa = new ShiftAccess(c);
        sa.insert(sd);

        logger.debug("Shift added with id: " + shiftId);
    }

}
