package com.wbiag.app.ta.ruleengine;

import java.util.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import com.wbiag.app.ta.quickrules.*;
import junit.framework.*;
/**
 * Test for CDataEventWrksStartEndSkdTest.
 */
public class CDataEventWrksStartEndSkdTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventWrksStartEndSkdTest.class);

    public CDataEventWrksStartEndSkdTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CDataEventWrksStartEndSkdTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testUpdateWrks() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventWrksStartEndSkd");

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.createsDefaultRecords();

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setOvrType(OverrideData.SCHEDULE_SHIFT_TYPE);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        Date st = DateHelper.addMinutes(start, 420);
        Date end = DateHelper.addMinutes(start, 960);
        ins.setEmpskdActStartTime(st);
        ins.setEmpskdActEndTime(end);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true, false);
        WorkSummaryData data = getWorkSummaryForDate(empId,start);
        assertEquals(st , data.getWrksStartTime());
        assertEquals(end , data.getWrksEndTime());
    }


    /**
     * @throws Exception
     */
    public void testUpdateWrksMulti() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventWrksStartEndSkd");

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.createsDefaultRecords();

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setOvrType(OverrideData.SCHEDULE_SHIFT_TYPE);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        Date st = DateHelper.addMinutes(start, 420);
        Date end = DateHelper.addMinutes(start, 960);
        ins.setEmpskdActStartTime(st);
        ins.setEmpskdActEndTime(end);
        Date st2 = DateHelper.addMinutes(start, 1200);
        Date end2 = DateHelper.addMinutes(start, 1320);
        ins.setEmpskdActStartTime2(st2);
        ins.setEmpskdActEndTime2(end2);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true, false);
        WorkSummaryData data = getWorkSummaryForDate(empId,start);
        assertEquals(st , data.getWrksStartTime());
        assertEquals(end2 , data.getWrksEndTime());
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
