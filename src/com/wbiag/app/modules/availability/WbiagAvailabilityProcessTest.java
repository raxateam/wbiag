package com.wbiag.app.modules.availability;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.modules.availability.db.*;
import com.workbrain.app.modules.availability.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.tool.security.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for WbiagAvailabilityProcessTest.
 * @deprecated As of 5.0, use core Make Availability Reportable Task.
 */
public class WbiagAvailabilityProcessTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WbiagAvailabilityProcessTest.class);
    private static final String TIME_PREFIX_START = "amxavStime";
    private static final String TIME_PREFIX_END = "amxavEtime";

    public WbiagAvailabilityProcessTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WbiagAvailabilityProcessTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testRecreates() throws Exception {
        final int empId = 15;
        final Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "SUN");
        final int startHrs = 9, endHrs = 17, days = 7;
        final Date end = DateHelper.addDays(start , days - 1);

        recreateDefAvail  (empId , start , startHrs , endHrs, days);

        EmployeeData ed = getEmployeeData(empId , start);
        int wbtId = SecurityEmployee.getHomeTeamId(getConnection(), empId , new java.sql.Date(start.getTime())  ) ;

        WbiagAvailabilityProcess.WbiagAvailabilityProcessContext cont =
            new WbiagAvailabilityProcess.WbiagAvailabilityProcessContext();
        cont.startDate = start;
        cont.endDate = end;
        cont.conn = getConnection();
        cont.empIds = new int[] {empId};
        cont.paygrpIds = new int[] {ed.getPaygrpId()};
        cont.calcgrpIds = new int[] {ed.getCalcgrpId()};
        cont.teamIds = new int[] {wbtId};
        cont.subteams = true;
        cont.recreatesSummaryDetail = true;
        cont.shouldCommit = false;

        WbiagAvailabilityProcess pr = new WbiagAvailabilityProcess();
        pr.process(cont);

        WbiagAvailabilityAccess wacc = new WbiagAvailabilityAccess(getConnection());
        WbiagAvalSummaryData sum = wacc.loadSummaryByEmpIdDate(empId , start);
        assertNotNull(sum);

        List l = wacc.loadDetailwBySummaryId(sum.getWavsId());
        assertEquals( 1, l.size());
        WbiagAvalDetailData det = (WbiagAvalDetailData) l.get(0);
        assertEquals(det.getWavdActStTime() , DateHelper.addMinutes(start, startHrs*60));
        assertEquals(det.getWavdActEndTime() , DateHelper.addMinutes(start, endHrs*60));
        assertEquals(det.getWavdActMinutes(), (endHrs - startHrs) * 60);
    }

    /**
     * @throws Exception
     */
    public void testChanges() throws Exception {
        final int empId1 = 15, empId2 = 16, empId3 = 17;
        final Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "SUN");
        final int startHrs = 9, endHrs = 17, startHrsOvr = 8, endHrsOvr = 18, days = 7;
        final Date end = DateHelper.addDays(start , days - 1);

        recreateDefAvail(empId1, start , startHrs , endHrs, days);
        recreateDefAvail(empId2, start , startHrs , endHrs, days);
        createAmxOverride(empId2 , start , startHrsOvr , endHrsOvr);
        recreateDefAvail(empId3, start , startHrs , endHrs, days);

        WbiagAvailabilityProcess.WbiagAvailabilityProcessContext cont =
            new WbiagAvailabilityProcess.WbiagAvailabilityProcessContext();
        cont.startDate = start;
        cont.endDate = end;
        cont.conn = getConnection();
        cont.empIds = new int[] {empId1 , empId2, empId3};
        cont.lastRun = DateHelper.getCurrentDate();
        cont.recreatesSummaryDetail = false;
        cont.shouldCommit = false;
        cont.runsForChangedEmps = true;
        cont.runsForMissingSummaries = true;

        WbiagAvailabilityProcess pr = new WbiagAvailabilityProcess();
        pr.process(cont);

        WbiagAvailabilityAccess wacc = new WbiagAvailabilityAccess(getConnection());

        WbiagAvalSummaryData sum = wacc.loadSummaryByEmpIdDate(empId1 , start);
        assertNotNull(sum);

        List l = wacc.loadDetailwBySummaryId(sum.getWavsId());
        assertEquals( 1, l.size());
        WbiagAvalDetailData det = (WbiagAvalDetailData) l.get(0);
        assertEquals(det.getWavdActStTime() , DateHelper.addMinutes(start, startHrs*60));
        assertEquals(det.getWavdActEndTime() , DateHelper.addMinutes(start, endHrs*60));
        assertEquals(det.getWavdActMinutes(), (endHrs - startHrs) * 60);

        sum = wacc.loadSummaryByEmpIdDate(empId2 , start);
        assertNotNull(sum);

        l = wacc.loadDetailwBySummaryId(sum.getWavsId());
        assertEquals( 1, l.size());
        det = (WbiagAvalDetailData) l.get(0);
        assertEquals(det.getWavdActStTime() , DateHelper.addMinutes(start, startHrsOvr*60));
        assertEquals(det.getWavdActEndTime() , DateHelper.addMinutes(start, endHrsOvr*60));
        assertEquals(det.getWavdActMinutes(), (endHrsOvr - startHrsOvr) * 60);

        wacc.deleteSummaries(new int[] {empId1}, start, start);
        pr.process(cont);
        sum = wacc.loadSummaryByEmpIdDate(empId1 , start);
        assertNotNull(sum);
    }

    private void recreateDefAvail(int empId, Date start,
                                int startHrs, int endHrs , int days) throws Exception{
        delEmpAvail(empId);
        createDefAvail(empId, start , startHrs , endHrs, days);
    }

    private void createDefAvail(int empId, Date start,
                                int startHrs, int endHrs , int days) throws Exception{

        AvailabilityAccess acc = new AvailabilityAccess(getConnection(), getCodeMapper()) ;
        AvailabilityData ad = new AvailabilityData();
        ad.setEmpId(empId);
        ad.setAmxavStartDate(AvailabilityData.DEF_START_DATE);
        ad.setAmxavEndDate(AvailabilityData.DEF_END_DATE);
        for (int i = 1 ; i <= days ; i++ ) {
            Date startTime = DateHelper.addMinutes(DateHelper.addDays(start , i - 1), startHrs*60);
            Date endTime = DateHelper.addMinutes(DateHelper.addDays(start , i - 1), endHrs*60);
            ad.setProperty(TIME_PREFIX_START + i , startTime );
            ad.setProperty(TIME_PREFIX_END + i , endTime);
        }
        acc.insert(ad);

    }

    private void createAmxOverride(int empId, Date start,
                                int startHrs, int endHrs ) throws Exception{

        AvOvrAccess acc = new AvOvrAccess(getConnection(), getCodeMapper()) ;
        AvOvrData od = new AvOvrData();
        od.setAmxovId(getConnection().getDBSequence(od.AVAIL_OVR_SEQ).getNextValue());
        od.setEmpId(empId);
        od.setAmxovCrtDt(new Date());
        od.setAmxovWdSt(start);
        od.setAmxovWdEnd(start);
        od.setAmxovStTime(DateHelper.addMinutes(start, startHrs*60));
        od.setAmxovEndTime(DateHelper.addMinutes(start, endHrs*60));
        od.setAmxovcId(2);
        od.setAmxovApprvStatus(0);
        od.setAmxovModifyDate(new Date());
        od.setAmxovModifyTime(new Date());
        acc.insert(od);

    }

    private void delEmpAvail(int empId) throws Exception{
        PreparedStatement ps = null;
        try {
            String sql = "DELETE FROM amx_availability WHERE emp_id = ?";
            ps = getConnection().prepareStatement(sql);
            ps.setInt(1  , empId);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
