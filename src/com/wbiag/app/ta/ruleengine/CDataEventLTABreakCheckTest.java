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
 * Test for CDataEventLTABreakCheckTest.
 */
public class CDataEventLTABreakCheckTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventLTABreakCheckTest.class);

    public CDataEventLTABreakCheckTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CDataEventLTABreakCheckTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testLTA() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventLTABreakCheck");
        TestUtil.getInstance().setVarTemp(CDataEventLTABreakCheck.REG_LTA_PRESERVE_BREAKS, "true");

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        final int empId = 15;
        final String shftName = "DAY"; int shftId = 1;
        ShiftBreakData sb = new ShiftBreakData();
        sb.setShftbrkStartTime(DateHelper.addMinutes(start, 12*60));
        sb.setShftbrkEndTime(DateHelper.addMinutes(start, 12*60 + 30));
        sb.setShftbrkMinutes(30);
        sb.setTcodeId(getCodeMapper().getTimeCodeByName("BRK").getTcodeId()  );
        sb.setHtypeId(1);
        sb.setShftId(shftId) ;
        new ShiftBreakAccess(getConnection()).insert(sb);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.createsDefaultRecords();
        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setOvrType(OverrideData.SCHEDULED_TIMES_WITH_BREAKS);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setEmpskdActShiftName(shftName);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride insL = new InsertWorkDetailOverride(getConnection());
        insL.setOvrType(OverrideData.LTA_TYPE_START);
        insL.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insL.setEmpId(empId);
        insL.setStartDate(start);
        insL.setEndDate(start);
        Date st = DateHelper.addMinutes(start, 9*60);
        Date end = DateHelper.addMinutes(start, 17*60);
        insL.setStartTime(st);
        insL.setEndTime(end);
        insL.setWrkdTcodeName("TRN");
        ovrBuilder.add(insL);

        ovrBuilder.execute(true, false);
        WorkDetailList wdl = getWorkDetailsForDate(empId,start);
        System.out.println(wdl.toDescription());
        assertEquals(DateHelper.getMinutesBetween(end, st)  - 30,
                     wdl.getMinutes(null, null, "TRN", true, null, true) );
        assertEquals(30,
                     wdl.getMinutes(null, null, "BRK", true, null, true) );
    }

    /**
     * @throws Exception
     */
    public void xWithRegBRsk() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventLTABreakCheck");

        final int empId = 15;
        final String brkCode = "BRK";
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue") ;

        TestUtil.getInstance().setVarTemp(CDataEventLTABreakCheck.REG_LTA_BRK_TIMECODES, brkCode);
        TestUtil.getInstance().setVarTemp(CDataEventLTABreakCheck.REG_LTA_PRESERVE_BREAKS, "false");

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.createsDefaultRecords();

        InsertScheduleDetailOverride insSd = new InsertScheduleDetailOverride();
        insSd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        Date st = DateHelper.addMinutes(start, 9*60);
        Date end = DateHelper.addMinutes(start, 17*60);
        EmployeeScheduleData esd  = new EmployeeScheduleData();
        esd.setEmpskdActStartTime(st);
        esd.setEmpskdActEndTime(end);
        insSd.setEmployeeScheduleData(esd);
        Date stBrk = DateHelper.addMinutes(start, 12 * 60);
        Date endBrk = DateHelper.addMinutes(start, 12 * 60 + 30);
        WorkDetailData wd  = new WorkDetailData();
        wd.setCodeMapper(getCodeMapper());
        wd.setEmpId(empId);
        wd.setWrkdWorkDate(start);
        wd.setWrkdStartTime(stBrk);
        wd.setWrkdEndTime(endBrk);
        wd.setWrkdTcodeName(brkCode);
        wd.setWrkdHtypeName("UNPAID");
        insSd.setWorkDetailData(wd);
        ovrBuilder.add(insSd);

        InsertWorkDetailOverride insL = new InsertWorkDetailOverride(getConnection());
        insL.setOvrType(OverrideData.LTA_TYPE_START);
        insL.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insL.setEmpId(empId);
        insL.setStartDate(start);
        insL.setEndDate(start);
        //st = DateHelper.addMinutes(start, 9*60);
        //end = DateHelper.addMinutes(start, 16*60);
        //insL.setStartTime(st);
        //insL.setEndTime(end);
        insL.setWrkdTcodeName("TRN");
        ovrBuilder.add(insL);

        ovrBuilder.execute(true, false);
        WorkDetailList wdl = getWorkDetailsForDate(empId,start);
        System.out.println(wdl.toDescription());
        assertEquals(DateHelper.getMinutesBetween(end, st)  - 30,
                     wdl.getMinutes(null, null, "TRN", true, null, true) );
        assertEquals(0,
                     wdl.getMinutes(null, null, brkCode, true, null, true) );

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
