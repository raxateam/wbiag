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
 * Test for CDataEventPayrollSplitAdjTest.
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class CDataEventPayrollSplitAdjTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventPayrollSplitAdjTest.class);

    public CDataEventPayrollSplitAdjTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CDataEventPayrollSplitAdjTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testAdjOff() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventPayrollSplitAdj");


        Date pgStart = DateHelper.parseSQLDate("2006-06-04");
        Date pgEnd = DateHelper.parseSQLDate("2006-06-17");
        Date adjDate = DateHelper.parseSQLDate("2006-06-03");

        final int empId = 15;
        updateCalcgrpUdf(empId, "19000101 060000");
        // *** make it in current period before export
        EmployeeData ed = getEmployeeData(empId, DateHelper.getCurrentDate() ) ;
        PayGroupData pgd = getCodeMapper().getPayGroupById(ed.getPaygrpId() );
        pgd.setPaygrpStartDate(DateHelper.addDays(pgStart, -14));
        pgd.setPaygrpEndDate(DateHelper.addDays(pgEnd, -14));
        pgd.setPaygrpAdjustDate(DateHelper.addDays(adjDate, -14));
        pgd.setPgcId(null);
        new PayGroupAccess(getConnection()).update(pgd)  ;

        Date start = DateHelper.addDays(pgd.getPaygrpEndDate() , 1);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setOvrType(OverrideData.SCHEDULED_TIMES_WITH_BREAKS);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setEmpskdActShiftName("OFF");
        ovrBuilder.add(ins);

        InsertWorkSummaryOverride insW = new InsertWorkSummaryOverride(getConnection());
        insW.setWbuNameBoth("JUNIT", "JUNIT");
        insW.setEmpId(empId);
        insW.setStartDate(start);      insW.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 5*60 + 55);
        Datetime clk1Off = DateHelper.addMinutes(start, 14*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        insW.setWrksClocks(clks);
        ovrBuilder.add(insW);
        ovrBuilder.execute(true, false);


        WorkDetailList wdl = getWorkDetailsForDate(empId,start);
        //System.out.println(wdl.toDescription());
        WorkDetailAdjustAccess wadjAcc = new WorkDetailAdjustAccess (getConnection() );
        List adjs = wadjAcc.loadRecordData(new WorkDetailAdjustData(), WorkDetailAdjustAccess.WORK_DETAIL_ADJUST_TABLE, "emp_id", empId , "wrkda_work_date", start );
        //System.out.println("Adjs: \n" + adjs);

        // *** forward pay period dates as if payroll ran
        pgd.setPaygrpStartDate(DateHelper.addDays(pgd.getPaygrpStartDate(), 14));
        pgd.setPaygrpEndDate(DateHelper.addDays(pgd.getPaygrpEndDate(), 14));
        pgd.setPaygrpAdjustDate(DateHelper.addDays(pgd.getPaygrpAdjustDate(), 14));
        pgd.setPgcId(null);
        new PayGroupAccess(getConnection()).update(pgd)  ;


        InsertWorkDetailOverride insD = new InsertWorkDetailOverride(getConnection());
        insD.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD.setWbuNameBoth("JUNIT", "JUNIT");
        insD.setEmpId(empId);
        insD.setStartDate(start);      insD.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 5*60 + 55);
        clk1Off = DateHelper.addMinutes(start, 6*60);
        insD.setStartTime(clk1On);      insD.setEndTime(clk1Off);
        insD.setWrkdTcodeName("TRN");
        ovrBuilder.add(insD);
        ovrBuilder.execute(true, false);
        wdl = getWorkDetailsForDate(empId,start);
        //System.out.println(wdl.toDescription());

        adjs = wadjAcc.loadRecordData(new WorkDetailAdjustData(), WorkDetailAdjustAccess.WORK_DETAIL_ADJUST_TABLE, "emp_id", empId , "wrkda_work_date", start );
        //System.out.println(adjs);
        WorkDetailAdjustData adj0 = (WorkDetailAdjustData) adjs.get(0);
        assertEquals(-5 , adj0.getWrkdaMinutes());
    }

    /**
     * @throws Exception
     */
    public void testAdjClock() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventPayrollSplitAdj");


        final int empId = 15;
        updateCalcgrpUdf(empId, "19000101 060000");

        Date pgStart = DateHelper.parseSQLDate("2006-06-04");
        Date pgEnd = DateHelper.parseSQLDate("2006-06-17");
        Date adjDate = DateHelper.parseSQLDate("2006-06-03");

        // *** make it in current period before export
        EmployeeData ed = getEmployeeData(empId, DateHelper.getCurrentDate() ) ;
        PayGroupData pgd = getCodeMapper().getPayGroupById(ed.getPaygrpId() );
        pgd.setPaygrpStartDate(DateHelper.addDays(pgStart, -14));
        pgd.setPaygrpEndDate(DateHelper.addDays(pgEnd, -14));
        pgd.setPaygrpAdjustDate(DateHelper.addDays(adjDate, -14));
        pgd.setPgcId(null);
        new PayGroupAccess(getConnection()).update(pgd)  ;


        Date start = DateHelper.addDays(pgd.getPaygrpEndDate() , -14);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkSummaryOverride insW = new InsertWorkSummaryOverride(getConnection());
        insW.setWbuNameBoth("JUNIT", "JUNIT");
        insW.setEmpId(empId);
        insW.setStartDate(start);      insW.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 22*60);
        Datetime clk1Off = DateHelper.addMinutes(start, 32*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        insW.setWrksClocks(clks);
        ovrBuilder.add(insW);
        ovrBuilder.execute(true, false);


        WorkDetailList wdl = getWorkDetailsForDate(empId,start);
        //System.out.println(wdl.toDescription());
        WorkDetailAdjustAccess wadjAcc = new WorkDetailAdjustAccess (getConnection() );
        List adjs = wadjAcc.loadRecordData(new WorkDetailAdjustData(), WorkDetailAdjustAccess.WORK_DETAIL_ADJUST_TABLE, "emp_id", empId , "wrkda_work_date", start );
        assertTrue(1 == adjs.size() );
        WorkDetailAdjustData adj0 = (WorkDetailAdjustData) adjs.get(0);
        assertEquals(480 , adj0.getWrkdaMinutes());


    }

    /**
     * @throws Exception
     */
    public void testJobChange() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventPayrollSplitAdj");

        final int empId = 15;
        updateCalcgrpUdf(empId, "19000101 060000");

        Date pgStart = DateHelper.parseSQLDate("2006-06-04");
        Date pgEnd = DateHelper.parseSQLDate("2006-06-17");
        Date adjDate = DateHelper.parseSQLDate("2006-06-03");

        // *** make it in current period before export
        EmployeeData ed = getEmployeeData(empId, DateHelper.getCurrentDate() ) ;
        PayGroupData pgd = getCodeMapper().getPayGroupById(ed.getPaygrpId() );
        pgd.setPaygrpStartDate(DateHelper.addDays(pgStart, -14));
        pgd.setPaygrpEndDate(DateHelper.addDays(pgEnd, -14));
        pgd.setPaygrpAdjustDate(DateHelper.addDays(adjDate, -14));
        pgd.setPgcId(null);
        new PayGroupAccess(getConnection()).update(pgd)  ;

        Date start = DateHelper.addDays(pgd.getPaygrpEndDate(), 1);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkDetailOverride insW = new InsertWorkDetailOverride(getConnection());
        insW.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        insW.setWbuNameBoth("JUNIT", "JUNIT");
        insW.setEmpId(empId);
        insW.setStartDate(start);      insW.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 3*60);
        Datetime clk1Off = DateHelper.addMinutes(start, 16*60);
        insW.setStartTime(clk1On) ;
        insW.setEndTime(clk1Off) ;
        insW.setWrkdJobName("JANITOR");
        ovrBuilder.add(insW);
        ovrBuilder.execute(true, false); ovrBuilder.clear();


        WorkDetailList wdl = getWorkDetailsForDate(empId,start);
        //System.out.println(wdl.toDescription());
        WorkDetailAdjustAccess wadjAcc = new WorkDetailAdjustAccess (getConnection() );
        List adjs = wadjAcc.loadRecordData(new WorkDetailAdjustData(), WorkDetailAdjustAccess.WORK_DETAIL_ADJUST_TABLE, "emp_id", empId , "wrkda_work_date", start );
        //System.out.println("Adjs: \n" + adjs);

        // *** forward pay period dates as if payroll ran
        pgd.setPaygrpStartDate(DateHelper.addDays(pgd.getPaygrpStartDate(), 14));
        pgd.setPaygrpEndDate(DateHelper.addDays(pgd.getPaygrpEndDate(), 14));
        pgd.setPaygrpAdjustDate(DateHelper.addDays(pgd.getPaygrpAdjustDate(), 14));
        pgd.setPgcId(null);
        new PayGroupAccess(getConnection()).update(pgd)  ;


        insW = new InsertWorkDetailOverride(getConnection());
        insW.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        insW.setWbuNameBoth("JUNIT", "JUNIT");
        insW.setEmpId(empId);
        insW.setStartDate(start);      insW.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 3*60);
        clk1Off = DateHelper.addMinutes(start, 16*60);
        insW.setStartTime(clk1On) ;
        insW.setEndTime(clk1Off) ;
        insW.setWrkdJobName("CLERK");
        ovrBuilder.add(insW);
        ovrBuilder.execute(true, false); ovrBuilder.clear();


        adjs = wadjAcc.loadRecordData(new WorkDetailAdjustData(), WorkDetailAdjustAccess.WORK_DETAIL_ADJUST_TABLE, "emp_id", empId , "wrkda_work_date", start );
        assertTrue(2 == adjs.size() );
        WorkDetailAdjustData adj0 = (WorkDetailAdjustData) adjs.get(0);
        assertEquals(-180 , adj0.getWrkdaMinutes());
        WorkDetailAdjustData adj1 = (WorkDetailAdjustData) adjs.get(1);
        assertEquals(180 , adj1.getWrkdaMinutes());
    }

    /**
     * @throws Exception
     */
    public void testAdjLTA() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventPayrollSplitAdj");

        final int empId = 15;
        updateCalcgrpUdf(empId, "19000101 000000");

        Date pgStart = DateHelper.parseSQLDate("2006-06-04");
        Date pgEnd = DateHelper.parseSQLDate("2006-06-17");
        Date adjDate = DateHelper.parseSQLDate("2006-06-03");

        // *** make it in current period before export
        EmployeeData ed = getEmployeeData(empId, DateHelper.getCurrentDate() ) ;
        PayGroupData pgd = getCodeMapper().getPayGroupById(ed.getPaygrpId() );
        pgd.setPaygrpStartDate(DateHelper.addDays(pgStart, -14));
        pgd.setPaygrpEndDate(DateHelper.addDays(pgEnd, -14));
        pgd.setPaygrpAdjustDate(DateHelper.addDays(adjDate, -14));
        pgd.setPgcId(null);
        new PayGroupAccess(getConnection()).update(pgd)  ;

        Date start = DateHelper.addDays(pgd.getPaygrpEndDate(), 0);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setOvrType(OverrideData.SCHEDULED_TIMES_WITH_BREAKS);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setEmpskdActShiftName("OFF");
        ovrBuilder.add(ins);

        InsertWorkDetailOverride insW = new InsertWorkDetailOverride(getConnection());
        int ovrId = getConnection().getDBSequence("seq_ovr_id") .getNextValue();
        insW.setOverrideId(ovrId) ;
        insW.setOvrType(OverrideData.LTA_TYPE_START );
        insW.setWbuNameBoth("JUNIT", "JUNIT");
        insW.setEmpId(empId);
        insW.setStartDate(start);      insW.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 22*60);
        Datetime clk1Off = DateHelper.addMinutes(start, 30*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        insW.setStartTime(clk1On) ;
        insW.setEndTime(clk1Off) ;
        insW.setWrkdTcodeName("TRN");
        ovrBuilder.add(insW);
        ovrBuilder.execute(true, false); ovrBuilder.clear();


        WorkDetailList wdl = getWorkDetailsForDate(empId,start);
        //System.out.println(wdl.toDescription());
        WorkDetailAdjustAccess wadjAcc = new WorkDetailAdjustAccess (getConnection() );
        List adjs = wadjAcc.loadRecordData(new WorkDetailAdjustData(), WorkDetailAdjustAccess.WORK_DETAIL_ADJUST_TABLE, "emp_id", empId , "wrkda_work_date", start );
        //System.out.println("Adjs: \n" + adjs);

        // *** forward pay period dates as if payroll ran
        pgd.setPaygrpStartDate(DateHelper.addDays(pgd.getPaygrpStartDate(), 14));
        pgd.setPaygrpEndDate(DateHelper.addDays(pgd.getPaygrpEndDate(), 14));
        pgd.setPaygrpAdjustDate(DateHelper.addDays(pgd.getPaygrpAdjustDate(), 14));
        pgd.setPgcId(null);
        new PayGroupAccess(getConnection()).update(pgd)  ;


        DeleteOverride del = new DeleteOverride();
        del.setWbuNameBoth("JUNIT", "JUNIT");
        del.setOverrideId(ovrId);
        ovrBuilder.add(del);
        ovrBuilder.execute(true, false);
        wdl = getWorkDetailsForDate(empId,start);
        //System.out.println(wdl.toDescription());

        adjs = wadjAcc.loadRecordData(new WorkDetailAdjustData(), WorkDetailAdjustAccess.WORK_DETAIL_ADJUST_TABLE, "emp_id", empId , "wrkda_work_date", start );
        assertTrue(1 == adjs.size() );
        WorkDetailAdjustData adj0 = (WorkDetailAdjustData) adjs.get(0);
        assertEquals(-120 , adj0.getWrkdaMinutes());
    }

    /**
     * @throws Exception
     */
    public void testAdjClockNoChange() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventPayrollSplitAdj");


        final int empId = 15;
        updateCalcgrpUdf(empId, "19000101 060000");

        Date pgStart = DateHelper.parseSQLDate("2006-06-04");
        Date pgEnd = DateHelper.parseSQLDate("2006-06-17");
        Date adjDate = DateHelper.parseSQLDate("2006-06-03");

        // *** make it in current period before export
        EmployeeData ed = getEmployeeData(empId, DateHelper.getCurrentDate() ) ;
        PayGroupData pgd = getCodeMapper().getPayGroupById(ed.getPaygrpId() );
        pgd.setPaygrpStartDate(DateHelper.addDays(pgStart, -14));
        pgd.setPaygrpEndDate(DateHelper.addDays(pgEnd, -14));
        pgd.setPaygrpAdjustDate(DateHelper.addDays(adjDate, -14));
        pgd.setPgcId(null);
        new PayGroupAccess(getConnection()).update(pgd)  ;


        Date start = DateHelper.addDays(pgd.getPaygrpEndDate() , -13);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkSummaryOverride insW = new InsertWorkSummaryOverride(getConnection());
        insW.setWbuNameBoth("JUNIT", "JUNIT");
        insW.setEmpId(empId);
        insW.setStartDate(start);      insW.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 7*60);
        Datetime clk1Off = DateHelper.addMinutes(start, 18*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        insW.setWrksClocks(clks);
        ovrBuilder.add(insW);
        ovrBuilder.execute(true, false);


        WorkDetailList wdl = getWorkDetailsForDate(empId,start);
        //System.out.println(wdl.toDescription());
        WorkDetailAdjustAccess wadjAcc = new WorkDetailAdjustAccess (getConnection() );
        List adjs = wadjAcc.loadRecordData(new WorkDetailAdjustData(), WorkDetailAdjustAccess.WORK_DETAIL_ADJUST_TABLE, "emp_id", empId , "wrkda_work_date", start );
        //System.out.println(adjs);
        assertTrue(0 == adjs.size() );
        //WorkDetailAdjustData adj0 = (WorkDetailAdjustData) adjs.get(0);
        //assertEquals(480 , adj0.getWrkdaMinutes());


    }

    private void updateCalcgrpUdf(int empId, String split) throws Exception {
        CalcGroupData cg = getCodeMapper().getCalcGroupById(getEmployeeData(empId, DateHelper.getCurrentDate() ).getCalcgrpId());
        CalcGroupAccess cgAccess = new CalcGroupAccess(getConnection());
        cg.setCalcgrpUdf1(split);
        cgAccess.updateRecordData(cg , CalcGroupAccess.CALC_GROUP_TABLE , CalcGroupAccess.CALC_GROUP_PRI_KEY);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
