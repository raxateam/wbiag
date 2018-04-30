package com.wbiag.app.modules.retailSchedule.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import com.workbrain.app.modules.retailSchedule.db.ActivityAccess;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.RetailScheduleAccessFactory;
import com.workbrain.app.modules.retailSchedule.db.ShiftActivityAccess;
import com.workbrain.app.modules.retailSchedule.db.ShiftDetailAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Activity;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.app.modules.retailSchedule.model.Schedule;
import com.workbrain.app.modules.retailSchedule.model.ShiftActivity;
import com.workbrain.app.modules.retailSchedule.model.ShiftDetail;
import com.workbrain.app.modules.retailSchedule.utils.SOTime;
import com.workbrain.app.ta.model.ShiftPatternShiftsData;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.SQLHelper;
import com.workbrain.test.TestCaseHW;
import com.workbrain.test.TestUtil;

public class PreScheduledMealProcessorTest extends TestCaseHW {

    private PreScheduledMealProcessor mp;

    protected void setUp() throws Exception {
        super.setUp();
        DBInterface.init(getConnection());
        mp = new PreScheduledMealProcessor();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        DBInterface.remove();
    }

    public void testIAGPreScheduledMealProcessor() {
        assertNotNull(mp);
    }

    public void testGetActivities() throws Exception {
        TestUtil tu = TestUtil.getInstance();
        String udfActivity = Registry.getVarString(PreScheduledMealProcessor.PRESCHEDULED_MEAL_ACTIVITY, "ACT_FLAG4");
        List lst = PreScheduledMealProcessor.getActivities(udfActivity, getConnection());
        assertNotNull(lst);
        int size = lst.size();
        Activity act = RetailScheduleAccessFactory.getInstance()
                .createActivityData(new ActivityAccess(getConnection()));
        act.setActName("TEST");
        act.setSkdgrpId(new Integer(1));
        act.setActPaid(new Integer(1));
        act.setActContToCost(new Integer(1));
        act.setActDailyCost(new Double(1.0d));
        act.setActService("N");
        act.setActShiftOffset(0);
        act.setActWorking(new Integer(1));
        act.setColrId(new Integer(1));
        act.setTcodeId(new Integer(1));
        act.dbInsert();
        Integer actId = act.getActId();
        updateActFlag(actId, "ACT_FLAG4");

        udfActivity = Registry.getVarString(PreScheduledMealProcessor.PRESCHEDULED_MEAL_ACTIVITY, "ACT_FLAG4");
        lst = PreScheduledMealProcessor.getActivities(udfActivity, getConnection());
        assertEquals(size + 1, lst.size());

        tu.setVarTemp(PreScheduledMealProcessor.PRESCHEDULED_MEAL_ACTIVITY, "ACT_FLAG3");
        udfActivity = "ACT_FLAG3";
        lst = PreScheduledMealProcessor.getActivities(udfActivity, getConnection());
        assertEquals(0, lst.size());
        updateActFlag(actId, "ACT_FLAG3");
        lst = PreScheduledMealProcessor.getActivities(udfActivity, getConnection());
        assertEquals(1, lst.size());
        tu.clearTempVars();
        act.dbDelete();
    }

    public void testGetEmployees() throws Exception {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Schedule oSchedule = getSchedule();

        int count = 0;
        try {
            ps = getConnection().prepareStatement(
                    "SELECT count(EMP_ID) FROM SO_EMPLOYEE WHERE SEMP_ONFIXED_SKD <> 0 AND SKDGRP_ID = ?");
            ps.setInt(1, oSchedule.getSkdgrpId());
            rs = ps.executeQuery();
            if (!rs.next()) {
                fail("no columns returned, expected one");
            }
            count = rs.getInt(1);
        } finally {
            SQLHelper.cleanUp(ps, rs);
        }

        List lstEmployees = PreScheduledMealProcessor.getEmployees(oSchedule, getConnection());
        assertEquals(count, lstEmployees.size());

        for (Iterator iter = lstEmployees.iterator(); iter.hasNext();) {
            Employee emp = (Employee) iter.next();
            assertTrue(emp.getSempOnfixedSkd() != 0);
        }
    }

    private Schedule getSchedule() throws SQLException, RetailException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Schedule oSchedule;
        try {
            ps = getConnection().prepareStatement("SELECT skd_id FROM SO_SCHEDULE WHERE ROWNUM < 2");
            rs = ps.executeQuery();
            if (!rs.next()) {
                fail("no schedules found");
            }
            oSchedule = new Schedule(rs.getInt(1));

        } finally {
            SQLHelper.cleanUp(ps, rs);
        }
        return oSchedule;
    }

    public void testUpdateActivity() throws Exception {

        Schedule oSchedule = getSchedule();
        ShiftDetail shftDet = RetailScheduleAccessFactory.getInstance().createShiftDetailData(
                new ShiftDetailAccess(getConnection()));
        ShiftActivity shftAct = RetailScheduleAccessFactory.getInstance().createShiftActivityData(
                new ShiftActivityAccess(getConnection()));
        shftAct.setSchedule(oSchedule);
        shftAct.setShiftDetail(shftDet);
        SOTime start = new SOTime("12:00:00");
        SOTime end = new SOTime("20:00:00");
        shftAct.setShftactStartTime(start.toDateObject());
        shftAct.setShftactEndTime(end.toDateObject());
        start = new SOTime("13:00:00");
        end = new SOTime("14:00:00");
        PreScheduledMealProcessor.updateActivity(start, end, shftAct);
        start = new SOTime("13:00:00");
        end = new SOTime("14:00:00");
        assertEquals(shftAct.getShftactStartTime(), start.toDateObject());
        assertEquals(shftAct.getShftactEndTime(), end.toDateObject());
    }

    public void testAddActivity() throws Exception {
        String mealStart = "1300";
        String mealDuration = "60";
        SOTime start = new SOTime("13:00:00");
        SOTime end = new SOTime("14:00:00");
        Schedule oSchedule = getSchedule();
        ShiftDetail oShiftDetail = RetailScheduleAccessFactory.getInstance().createShiftDetailData(
                new ShiftDetailAccess(getConnection()));
        oShiftDetail.setSchedule(oSchedule);
        assertEquals(0, oShiftDetail.getShiftActivityList().size());
        PreScheduledMealProcessor.addActivity(start, end, "MEAL", oShiftDetail, getConnection());

        assertEquals(1, oShiftDetail.getShiftActivityList().size());
        ShiftActivity oShiftActivity = (ShiftActivity) oShiftDetail.getShiftActivityList().get(0);
        start = new SOTime("13:00:00");
        end = new SOTime("14:00:00");
        assertEquals(oShiftActivity.getShftactStartTime(), start.toDateObject());
        assertEquals(oShiftActivity.getShftactEndTime(), end.toDateObject());
        assertEquals(60, oShiftActivity.getLength().intValue());
    }

    public void testIsMealInsideShift() throws Exception {
        SOTime start = new SOTime("08:00:00");
        SOTime end = new SOTime("16:00:00");
        Schedule oSchedule = getSchedule();
        ShiftDetail oShiftDetail = RetailScheduleAccessFactory.getInstance().createShiftDetailData(
                new ShiftDetailAccess(getConnection()));
        oShiftDetail.setSchedule(oSchedule);
        oShiftDetail.setShftdetStartTime(start.toDateObject());
        oShiftDetail.setShftdetEndTime(end.toDateObject());

        start = new SOTime("17:00:00");
        end = new SOTime("18:00:00");
        boolean result = PreScheduledMealProcessor.isMealInsideShift(oShiftDetail, start, end);
        assertFalse(result);
        start = new SOTime("12:00:00");
        end = new SOTime("13:00:00");
        result = PreScheduledMealProcessor.isMealInsideShift(oShiftDetail, start, end);
        assertTrue(result);
    }

    public void testGetMealInfo() {
        ShiftPatternShiftsData data = new ShiftPatternShiftsData();
        String position = "SHFTPATSHFT_UDF3";
        String result = PreScheduledMealProcessor.getMealInfo(data, position, 3);
        assertNull(result);
        data.setShftpatshftUdf3("Y");
        result = PreScheduledMealProcessor.getMealInfo(data, position, 3);
        assertEquals("Y", result);
    }

    private void updateActFlag(Integer actId, String flag) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = getConnection().prepareStatement("UPDATE SO_ACTIVITY SET " + flag + " = ? WHERE ACT_ID = ?");
            stmt.setString(1, "Y");
            stmt.setInt(2, actId.intValue());
            stmt.execute();
        } finally {
            SQLHelper.cleanUp(stmt);
        }
    }
}
