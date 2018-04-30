package com.wbiag.app.modules.retailSchedule.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.workbrain.app.modules.retailSchedule.db.ActivityAccess;
import com.workbrain.app.modules.retailSchedule.db.EmployeeAccess;
import com.workbrain.app.modules.retailSchedule.db.RetailScheduleAccessFactory;
import com.workbrain.app.modules.retailSchedule.db.ShiftActivityAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.exceptions.SQLRetailException;
import com.workbrain.app.modules.retailSchedule.model.Activity;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.app.modules.retailSchedule.model.Schedule;
import com.workbrain.app.modules.retailSchedule.model.ShiftActivity;
import com.workbrain.app.modules.retailSchedule.model.ShiftDetail;
import com.workbrain.app.modules.retailSchedule.utils.SODate;
import com.workbrain.app.modules.retailSchedule.utils.SOTime;
import com.workbrain.app.modules.retailSchedule.utils.SOTimeInterval;
import com.workbrain.app.ta.db.EmployeeHistoryAccess;
import com.workbrain.app.ta.db.ShiftPatternShiftsAccess;
import com.workbrain.app.ta.model.EmployeeHistoryData;
import com.workbrain.app.ta.model.EmployeeHistoryMatrix;
import com.workbrain.app.ta.model.ShiftPatternShiftsData;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.StringHelper;

public class PreScheduledMealProcessor {

    public static final Logger logger = Logger.getLogger(PreScheduledMealProcessor.class);

    private String udfStart;

    private String udfDuration;

    private String udfActivity;

    private String udfDefaultActivity;

    private List lstEmployees = null;

    private List lstActivities = null;

    private EmployeeHistoryMatrix histMatrix;
    
    protected static final String PRESCHEDULED_MEAL_START = "/system/modules/scheduleOptimization/prescheduledMeals/MEAL_START";
    protected static final String PRESCHEDULED_MEAL_DURATION = "/system/modules/scheduleOptimization/prescheduledMeals/MEAL_DURATION";
    protected static final String PRESCHEDULED_MEAL_ACTIVITY = "/system/modules/scheduleOptimization/prescheduledMeals/MEAL_ACTIVITY";
    protected static final String PRESCHEDULED_MEAL_DEFAULT = "/system/modules/scheduleOptimization/prescheduledMeals/MEAL_DEFAULT_ACTIVITY";

    public PreScheduledMealProcessor() {
        udfStart = Registry.getVarString(PRESCHEDULED_MEAL_START, "SHFTPATSHFT_UDF3");
        udfDuration = Registry.getVarString(PRESCHEDULED_MEAL_DURATION, "SHFTPATSHFT_UDF4");
        udfActivity = Registry.getVarString(PRESCHEDULED_MEAL_ACTIVITY, "ACT_FLAG4");
        udfDefaultActivity = Registry.getVarString(PRESCHEDULED_MEAL_DEFAULT, "ACT_FLAG5");
    }

    public void init(Schedule schedule, DBConnection connection) throws CalloutException {
        try {
            lstEmployees = getEmployees(schedule, connection);
            int[] empIds = new int[lstEmployees.size()];
            for (int ix = 0; ix < lstEmployees.size(); ix++) {
                Employee oEmployee = (Employee) lstEmployees.get(ix);
                empIds[ix] = oEmployee.getEmpId();
            }

            Calendar from = (Calendar) schedule.getSkdFromDate().clone();
            from.add(Calendar.DATE, -1);
            Calendar to = (Calendar) schedule.getSkdToDate().clone();
            to.add(Calendar.DATE, +1);

            EmployeeHistoryAccess empHistAccess = new EmployeeHistoryAccess(connection);
            histMatrix = empHistAccess.load(empIds, from.getTime(), to.getTime());

            lstActivities = getActivities(udfActivity, connection);

        } catch (RetailException e) {
            logger.error(e.getMessage());
            throw new CalloutException(e.getMessage(), e);
        }
    }

    /**
     * Obtains the list of activities whose meal UDF is set to Y
     * 
     * @param udf
     * @param connection
     * @return
     */
    protected static List getActivities(String udf, DBConnection connection) {
        ActivityAccess actAc = new ActivityAccess(connection);
        return actAc.loadRecordData(new Activity(), "SO_ACTIVITY", udf, "Y");
    }

    /**
     * Gets a list of employees who are assigned to the store related to this
     * schedule and who are on a fixed or partially fixed shift
     * 
     * @param schedule
     *            The schedule to check
     * @param connection
     *            a connection to the DB
     * @return
     * @throws CalloutException
     */
    protected static List getEmployees(Schedule schedule, DBConnection connection) throws CalloutException {
        try {
            EmployeeAccess empAccess = new EmployeeAccess(connection);
            String strWhere = "SEMP_ONFIXED_SKD <> 0 AND SKDGRP_ID = " + schedule.getSkdgrpId();
            return empAccess.loadRecordData(new Employee(), "SO_EMPLOYEE", strWhere);
        } catch (RetailException e) {
            logger.error(e.getMessage());
            throw new CalloutException(e.getMessage(), e);
        }
    }

    /**
     * Process the given schedule. Find all MEALs for fixed employees, and alter
     * their start/end times based on their shift pattern
     * 
     * @param skd
     * @param conn
     * @throws CalloutException
     */
    public void process(Schedule skd, DBConnection conn) throws CalloutException {
        try {
            List lstShifts = skd.getShiftDetailList();
            for (Iterator iter = lstShifts.iterator(); iter.hasNext();) {
                ShiftDetail oDetail = (ShiftDetail) iter.next();
                process(oDetail, conn);
            }
        } catch (RetailException e) {
            logger.error(e.getMessage());
            throw new CalloutException(e.getMessage(), e);
        }
    }

    private void process(ShiftDetail shiftDetail, DBConnection connection) throws CalloutException {

        try {
            Employee oEmployee = shiftDetail.getEmployee();
            if (!lstEmployees.contains(oEmployee)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping meal for non-fixed Employee " + oEmployee.getEmpId());
                }
                return;
            }
            Date oDate = shiftDetail.getShftDate();
            if (!histMatrix.containsEmployeeDate(oEmployee.getEmpId(), oDate)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Employee " + oEmployee.getEmpId() + " has no history for date ["
                            + SODate.toDisplayString(oDate) + "]");
                }
                return;
            }

            // get the shift pattern
            ShiftPatternShiftsAccess spsa = new ShiftPatternShiftsAccess(connection);
            EmployeeHistoryData history = histMatrix.getEmployeeHistoryForDate(oEmployee.getEmpId(), oDate);
            ShiftPatternShiftsData result = spsa.loadByShiftPatDatOffset(history.getShftpatId(), oDate, history
                    .getEmpShftpatOffset());
            if (result == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(history.getEmpId() + " PATTERN IS NULL " + history.getShftpatId());
                }
                return;
            }

            // get the meal info from the shift pattern
            String mealStart = getMealInfo(result, udfStart, 3);
            String mealDuration = getMealInfo(result, udfDuration, 4);
            if (StringHelper.isEmpty(mealStart) || StringHelper.isEmpty(mealDuration)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(history.getEmpId() + " mealStart [" + mealStart + "] mealDuration [" + mealDuration
                            + "]");
                }
                return;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
            SOTime start = new SOTime(sdf.parse(mealStart));
            SOTime end = (SOTime) start.clone();
            end.add(Calendar.MINUTE, Integer.parseInt(mealDuration));

            if (!isMealInsideShift(shiftDetail, start, end)) {
                return;
            }
            boolean found = false;
            List shftActs = shiftDetail.getShiftActivityList();
            for (int ix = 0; ix < shftActs.size(); ix++) {
                ShiftActivity shftAct = (ShiftActivity) shftActs.get(ix);
                Activity act = shftAct.getActivity();
                if (!lstActivities.contains(act)) {
                    continue;
                }

                found = true;
                updateActivity(start, end, shftAct);
            }
            if (!found) {
                addActivity(start, end, udfDefaultActivity, shiftDetail, connection);
            }
        } catch (ParseException e) {
            logger.error(e.getMessage());
            throw new CalloutException(e.getMessage(), e);
        } catch (RetailException e) {
            logger.error(e.getMessage());
            throw new CalloutException(e.getMessage(), e);
        }
    }

    protected static boolean isMealInsideShift(ShiftDetail shiftDetail, SOTime start, SOTime end)
            throws CalloutException {
        try {
            SOTimeInterval detailInterval = new SOTimeInterval(shiftDetail.getShftdetStartTime(), shiftDetail
                    .getShftdetEndTime());
            return detailInterval.isIn(start) && detailInterval.isIn(end);
        } catch (RetailException e) {
            logger.error(e.getMessage());
            throw new CalloutException(e.getMessage(), e);
        }
    }

    protected static void addActivity(SOTime start, SOTime end, String udfDefaultActivity2, ShiftDetail oShiftDetail,
            DBConnection conn) throws CalloutException {
        ActivityAccess actAccess = new ActivityAccess(conn);
        List lst = actAccess.loadRecordData(new Activity(), "SO_ACTIVITY", "ACT_NAME", udfDefaultActivity2);
        if (lst.size() != 1) {
            if(logger.isEnabledFor(Priority.ERROR)){
                logger.error("COULD NOT FIND DEFAULT ACTIVITY, SKIPPING");
            }
            return;
        }
        try {
            ShiftActivity oShiftActivity = RetailScheduleAccessFactory.getInstance().createShiftActivityData(
                    new ShiftActivityAccess(conn));
            oShiftActivity.setShiftDetail(oShiftDetail);
            oShiftActivity.setActivity((Activity) lst.get(0));
            oShiftActivity.setShftactDate(oShiftDetail.getShftDate());
            oShiftActivity.setShftactLen((int) (end.toDateObject().getTime() - start.toDateObject().getTime()) / 1000);
            oShiftActivity.setSkdgrpId(oShiftDetail.getSkdgrpId());
            oShiftActivity.setSkdId(oShiftDetail.getSkdId());
            oShiftDetail.addShiftActivity(oShiftActivity);
            updateActivity(start, end, oShiftActivity);
        } catch (RetailException e) {
            logger.error(e.getMessage());
            throw new CalloutException(e.getMessage(), e);
        }
    }

    /**
     * Helper method to update the shift activity's start and end time based on
     * the input start and duration. Implemented as a helper to aid in unit
     * testing
     * 
     * @param mealStart
     *            The string meal start, in HHmm format
     * @param mealDuration
     *            The meal duration, in minutes
     * @param shftAct
     *            The shift activity to change
     * @throws CalloutException
     */
    protected static void updateActivity(SOTime start, SOTime end, ShiftActivity shftAct) throws CalloutException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Changing meal for employee [" + shftAct.getShiftDetail().getEmpId() + "] WAS: "
                        + SOTime.toDisplayString(shftAct.getShftactStartTime()) + "-"
                        + SOTime.toDisplayString(shftAct.getShftactEndTime()) + " NEW: " + start.toDisplayString()
                        + "-" + end.toDisplayString());
            }
            shftAct.setShftactStartTime(start.toDateObject());
            shftAct.setShftactEndTime(end.toDateObject());
        } catch (RetailException e) {
            logger.error(e.getMessage());
            throw new CalloutException(e.getMessage(), e);
        }
    }

    /**
     * Obtains the UDF value, depending on the given inputs
     * 
     * @param result
     * @param position
     * @param def
     * @return
     */
    protected static String getMealInfo(ShiftPatternShiftsData result, String position, int def) {
        int i = position.charAt(position.length() - 1) - 48;
        for (int x = 0; x < 1; x++) {
            switch (i) {
            case 1:
                return result.getShftpatshftUdf1();
            case 2:
                return result.getShftpatshftUdf2();
            case 3:
                return result.getShftpatshftUdf3();
            case 4:
                return result.getShftpatshftUdf4();
            case 5:
                return result.getShftpatshftUdf5();
            default:
                i = def;
            }
        }
        return null;
    }

    /**
     * This is the UDF for the SO_ACTIVITY UDF field
     * 
     * @return
     */
    public String getUdfActivity() {
        return udfActivity;
    }

    /**
     * This is the UDF for the shift pattern UDF field denoting the meal
     * duration
     * 
     * @return
     */
    public String getUdfDuration() {
        return udfDuration;
    }

    /**
     * This is the UDF for the shift pattern UDF field denoting the meal start
     * 
     * @return
     */
    public String getUdfStart() {
        return udfStart;
    }

}
