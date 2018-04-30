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
import com.workbrain.app.modules.retailSchedule.model.Activity;
/**
 * Test for CDataEventSOActivityTcodeTest.
 */
public class CDataEventSOActivityTcodeTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventSOActivityTcodeTest.class);

    public CDataEventSOActivityTcodeTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CDataEventSOActivityTcodeTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testDE() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventSOActivityTcode");

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        // *** check default activity and set it to BRK for testing
        RecordAccess ra = new RecordAccess(getConnection());
        List acts = ra.loadRecordData(new Activity(), "SO_ACTIVITY", "act_name", "0");
        assertEquals( "default activity should exist" , 1 , acts.size());
        Activity defAct = (Activity)acts.get(0);
        defAct.setTcodeId(new Integer(44)); // *** break
        ra.updateRecordData(defAct , "SO_ACTIVITY" , "act_id");

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.createsDefaultRecords();

        EmployeeScheduleData esd = getEmployeeScheduleData(empId, start);
        esd.setCodeMapper(getCodeMapper());

        InsertScheduleDetailOverride ins0 = new InsertScheduleDetailOverride();
        ins0.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins0.setEmpId(empId);
        ins0.setStartDate(start);
        ins0.setEndDate(start);
        WorkDetailData wd = new WorkDetailData();
        wd.setEmpId(empId);
        wd.setCodeMapper(getCodeMapper() );
        wd.setActId(defAct.getActId().intValue()  );
        wd.setWrkdWorkDate(start) ;
        wd.setWrkdStartTime(esd.getEmpskdActStartTime());
        wd.setWrkdEndTime(esd.getEmpskdActEndTime());
        ins0.setWorkDetailData(wd);
        EmployeeScheduleData esd0 = new EmployeeScheduleData();
        esd0.setEmpId(empId);
        esd0.setWorkDate(esd.getWorkDate() );
        esd0.setEmpskdActStartTime(esd.getEmpskdActStartTime() );
        esd0.setEmpskdActEndTime(esd.getEmpskdActEndTime() );
        ins0.setEmployeeScheduleData(esd0);
        ovrBuilder.add(ins0);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        String clks = createWorkSummaryClockStringForOnOffs(
            new Datetime(esd.getEmpskdActStartTime())
            , new Datetime(esd.getEmpskdActEndTime()));
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true, false);
        WorkDetailList wdl = getWorkDetailsForDate(empId,start);
        WorkDetailData wd0 = wdl.getWorkDetail(0);
        assertEquals("BRK", wd0.getWrkdTcodeName());
    }


    /**
     * @throws Exception
     */
    public void testActWithTcodeClock() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventSOActivityTcode");

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** check default activity and set it to BRK for testing
        RecordAccess ra = new RecordAccess(getConnection());
        List acts = ra.loadRecordData(new Activity(), "SO_ACTIVITY", "act_name", "0");
        assertEquals( "default activity should exist" , 1 , acts.size());
        Activity defAct = (Activity)acts.get(0);
        defAct.setTcodeId(new Integer(44)); // *** break
        ra.updateRecordData(defAct , "SO_ACTIVITY" , "act_id");

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.createsDefaultRecords();

        EmployeeScheduleData esd = getEmployeeScheduleData(empId, start);
        esd.setCodeMapper(getCodeMapper());

        InsertScheduleDetailOverride ins0 = new InsertScheduleDetailOverride();
        ins0.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins0.setEmpId(empId);
        ins0.setStartDate(start);
        ins0.setEndDate(start);
        WorkDetailData wd = new WorkDetailData();
        wd.setEmpId(empId);
        wd.setCodeMapper(getCodeMapper() );
        wd.setActId(defAct.getActId().intValue() );
        wd.setWrkdWorkDate(start) ;
        wd.setWrkdStartTime(esd.getEmpskdActStartTime());
        wd.setWrkdEndTime(esd.getEmpskdActEndTime());
        ins0.setWorkDetailData(wd);
        EmployeeScheduleData esd0 = new EmployeeScheduleData();
        esd0.setEmpId(empId);
        esd0.setWorkDate(esd.getWorkDate() );
        esd0.setEmpskdActStartTime(esd.getEmpskdActStartTime() );
        esd0.setEmpskdActEndTime(esd.getEmpskdActEndTime() );
        ins0.setEmployeeScheduleData(esd0);
        ovrBuilder.add(ins0);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        String clks = createWorkSummaryClockStringForOnOffs(
            new Datetime(esd.getEmpskdActStartTime())
            , new Datetime(esd.getEmpskdActEndTime()));
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkSummaryOverride ins2 = new InsertWorkSummaryOverride(getConnection());
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);
        ins2.setEndDate(start);
        Clock clk = createClock(DateHelper.addMinutes(start, 12*60 + 30), Clock.TYPE_TCODE, "TCODE=TRN");
        List clkList = new ArrayList(); clkList.add(clk);
        ins2.setWrksClocks(Clock.createStringFromClockList(clkList) );
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true, false);
        WorkDetailList wdl = getWorkDetailsForDate(empId,start);
        //System.out.println(wdl.toDescription());
        WorkDetailData wd0 = wdl.getWorkDetail(0);
        assertEquals("BRK", wd0.getWrkdTcodeName());
        WorkDetailData wd1 = wdl.getWorkDetail(1);
        assertEquals("TRN", wd1.getWrkdTcodeName());

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
