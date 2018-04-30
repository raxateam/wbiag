package com.wbiag.app.ta.ruleengine;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.security.team.*;
import com.workbrain.test.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.tool.security.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for CDataEventReaderToDetailTest.
 */
public class CDataEventReaderToDetailTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventReaderToDetailTest.class);

    public CDataEventReaderToDetailTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CDataEventReaderToDetailTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testRdr() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventReaderToDetail");

        TestUtil.getInstance().setVarTemp("/" + CDataEventReaderToDetail.REG_CLOCK_READER_FIELD,
            CDataEventReaderToDetail.REGVAL_CLOCK_READER_FIELD_RDR);
        TestUtil.getInstance().setVarTemp("/" + CDataEventReaderToDetail.REG_CLOCK_DATA_FIELD,
            Clock.CLOCKDATA_UDF1);

        final int empId = 10;
        final String rdrName = "VIRTUAL READER";
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate(), "Mon");
        Date clkOn = DateHelper.addMinutes(start, 9*60);
        Date clkOff = DateHelper.addMinutes(start, 17*60);
        ClockTranAccess access = new ClockTranAccess(getConnection());
        ClockTranPendJData data1 = new ClockTranPendJData();

        data1.setCtpjIdentifier(Integer.toString(empId));
        data1.setCtpjIdentType("I");
        data1.setCtpjTime(DateHelper.convertDateString(clkOn,
            Clock.CLOCKDATEFORMAT_STRING));
        data1.setCtpjRdrName(rdrName);
        data1.setCtpjType(Integer.toString(Clock.TYPE_ON));
        access.insert(data1);

        com.workbrain.app.clockInterface.processing.WBClockProcessTask ctask =
            new com.workbrain.app.clockInterface.processing.WBClockProcessTask();
        ctask.setShouldCommit(false);
        ctask.setCheckForInterrupt(false);
        ctask.execute(getConnection());

        WorkDetailList wdl = getWorkDetailsForDate(empId , start);
        assertTrue(wdl.size() > 0);
        WorkDetailData wd = wdl.getWorkDetail(0);
        assertEquals(rdrName, wd.getWrkdUdf1() );
    }


    /**
     * @throws Exception
     */
    public void testRdrGrp() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventReaderToDetail");

        TestUtil.getInstance().setVarTemp("/" + CDataEventReaderToDetail.REG_CLOCK_READER_FIELD,
            CDataEventReaderToDetail.REGVAL_CLOCK_READER_FIELD_RDRGRP);
        TestUtil.getInstance().setVarTemp("/" + CDataEventReaderToDetail.REG_CLOCK_DATA_FIELD,
            Clock.CLOCKDATA_UDF2);

        final int empId = 10;
        final String rdrName = "VIRTUAL READER";
        final String rdrgrpName = "VIRTUAL READER GROUP";
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate(), "Mon");
        Date clkOn = DateHelper.addMinutes(start, 9*60);
        Date clkOff = DateHelper.addMinutes(start, 17*60);
        ClockTranAccess access = new ClockTranAccess(getConnection());
        ClockTranPendJData data1 = new ClockTranPendJData();

        data1.setCtpjIdentifier(Integer.toString(empId));
        data1.setCtpjIdentType("I");
        data1.setCtpjTime(DateHelper.convertDateString(clkOn,
            Clock.CLOCKDATEFORMAT_STRING));
        data1.setCtpjRdrName(rdrName);
        data1.setCtpjType(Integer.toString(Clock.TYPE_ON));
        access.insert(data1);

        com.workbrain.app.clockInterface.processing.WBClockProcessTask ctask =
            new com.workbrain.app.clockInterface.processing.WBClockProcessTask();
        ctask.setShouldCommit(false);
        ctask.setCheckForInterrupt(false);
        ctask.execute(getConnection());

        WorkDetailList wdl = getWorkDetailsForDate(empId , start);
        assertTrue(wdl.size() > 0);
        WorkDetailData wd = wdl.getWorkDetail(0);
        assertEquals(rdrgrpName, wd.getWrkdUdf2() );
    }

    /**
     * Do nothing if registry is not set
     * @throws Exception
     */
    public void testDoNothing() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventReaderToDetail");

        final int empId = 10;
        final String rdrName = "VIRTUAL READER";
        final String rdrgrpName = "VIRTUAL READER GROUP";
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate(), "Mon");
        Date clkOn = DateHelper.addMinutes(start, 9*60);
        Date clkOff = DateHelper.addMinutes(start, 17*60);
        ClockTranAccess access = new ClockTranAccess(getConnection());
        ClockTranPendJData data1 = new ClockTranPendJData();

        data1.setCtpjIdentifier(Integer.toString(empId));
        data1.setCtpjIdentType("I");
        data1.setCtpjTime(DateHelper.convertDateString(clkOn,
            Clock.CLOCKDATEFORMAT_STRING));
        data1.setCtpjRdrName(rdrName);
        data1.setCtpjType(Integer.toString(Clock.TYPE_ON));
        access.insert(data1);

        com.workbrain.app.clockInterface.processing.WBClockProcessTask ctask =
            new com.workbrain.app.clockInterface.processing.WBClockProcessTask();
        ctask.setShouldCommit(false);
        ctask.setCheckForInterrupt(false);
        ctask.execute(getConnection());

        WorkDetailList wdl = getWorkDetailsForDate(empId , start);
        assertTrue(wdl.size() > 0);
        WorkDetailData wd = wdl.getWorkDetail(0);
        assertTrue( StringHelper.isEmpty(wd.getWrkdUdf2()) );
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
