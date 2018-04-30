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
 * Test for CDataEventChangeDetailsToSkdTcodeTest.
 */
public class CDataEventChangeDetailsToSkdTcodeTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventChangeDetailsToSkdTcodeTest.class);

    public CDataEventChangeDetailsToSkdTcodeTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CDataEventChangeDetailsToSkdTcodeTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void test1() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventChangeDetailsToSkdTcode");

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        // *** check default activity and set it to BRK for testing

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
        wd.setTcodeId(10); // skd to TRN
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
        System.out.println(wdl.toDescription());
        WorkDetailData wd0 = wdl.getWorkDetail(0);
        assertEquals("TRN", wd0.getWrkdTcodeName());
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
