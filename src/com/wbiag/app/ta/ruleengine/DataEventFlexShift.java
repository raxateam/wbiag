package com.wbiag.app.ta.ruleengine;

import java.sql.*;

import java.util.Calendar;
import java.util.*;
import java.util.Date;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.util.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.tool.overrides.*;

public class DataEventFlexShift
    extends DataEvent {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(DataEventFlexShift.class);
    
    private static final String ERROR_NOT_FLEX = "Emplolyee is not on a flex shift: ";
    private static final String WORKBRAIN = "WORKBRAIN";
    private static final String ON_STRING = " on ";

    /**
     * This creates a scheduled override for a clock on if the employee is on
     * flex shift.
     *
     * If am employee already has a flex shift then it is canceled and a new
     * one is created from the latest clock.
     *
     * See 4.1.1.2 of the tech doc
     *
     * @param clock Clock
     * @param empId int
     * @param conn DBConnection
     * @param data ImportData
     */
    public void afterProcessOneClock(Clock clock, int empId,
                                     DBConnection conn,
                                     ImportData data) {
        final int clockId = -1;
        /**
         * Clock Type ON
         */
        if (clock.getClockType() != Clock.TYPE_ON) {
            return;
        }
        /**
         * Check to see that employee is on a flex shift
         * using EmployeeScheduleAccess and CodeMapper
         */
        java.util.Date workDate;
        CodeMapper codeMapper;
        EmployeeAccess ea;
        EmployeeScheduleAccess esa;
        EmployeeScheduleData esd;
        ShiftData shift;

        Date blank = DateHelper.getCurrentDate();
        workDate = DateHelper.setTimeValues(clock.getClockDate(), blank);
        codeMapper = (CodeMapper) conn.getCodeMapper();

        ea = new EmployeeAccess(conn, codeMapper);
        esa = new EmployeeScheduleAccess(conn, codeMapper);

        try {
            esd = esa.load(ea.load(empId, workDate), workDate);
        }
        catch (java.sql.SQLException e) {
            throw new NestedRuntimeException(
                "Can not access employee schedule data " + empId, e);
        }

        shift = codeMapper.getShiftById(esd.getEmpskdActShiftId());
        if (shift == null) {
            if (logger.isInfoEnabled()) {
                logger.info(
                    ERROR_NOT_FLEX + empId);
            }
            return;
        }

        if (shift.getShftFlag1() == null) {
            if (logger.isInfoEnabled()) {
                logger.info(
                    ERROR_NOT_FLEX + empId);
            }
            return;
        }

        if (!shift.getShftFlag1().equalsIgnoreCase("Y")) {
            if (logger.isInfoEnabled()) {
                logger.info("Employee is not on a flex shift: " +
                            shift.getShftName());
            }
            return;
        }
        /**
         * It is now known that the employee is on a flex shift.
         *
         * Next check if a schedule already exists.
         */
        if (esd.isEmployeeScheduledActual()) {
            if (logger.isInfoEnabled()) {
                logger.info("Employee has a scheduled override for this day: " +
                            clock.getClockDate());
            }
            /**
             * The Employee already has a schedule override.
             *
             * Next check that start time of the schedule compaired with
             * this clock. If the clock is the latest event and the same
             * type of event as the first or the earliest event
             * then create the schedule.
             */
            Calendar scheduleStartTime;
            Calendar overrideStartTime;

            scheduleStartTime = DateHelper.getCalendar();
            overrideStartTime = DateHelper.getCalendar();
            scheduleStartTime.setTime(esd.getEmpskdActStartTime());
            overrideStartTime = convertClockToCalendar(DateHelper.
                convertDateString(clock.getClockDate(), "00yyyyMMddHHmmss"));
            //Round override time down to nearest second.
            overrideStartTime.setTime(DateHelper.roundTime(overrideStartTime.
                getTime(), 1, 0, null));

            if (scheduleStartTime.getTime().compareTo(overrideStartTime.getTime()) == 0) {
                //Start Times are the same, do nothing
                return;
            }
            else if (scheduleStartTime.getTime().before(overrideStartTime.
                getTime())) {
                if (logger.isInfoEnabled()) {
                    logger.info(
                        "Employee has a scheduled start time of " +
                        scheduleStartTime.getTime() +
                        " which is before clock start time of " +
                        overrideStartTime.getTime());
                }
                //This is the second override, compair types
                OverrideData schedTimeOvr = null;
                int lastOvrType;

                schedTimeOvr = getSchedTimeOvr(empId, workDate, conn);
                lastOvrType = Integer.parseInt(schedTimeOvr.getOvrUdf1());
                if (clockId != lastOvrType) {
                    //Different Override Types, do nothing
                    if (logger.isInfoEnabled()) {
                        logger.info(
                            "Override types are different no schedule override created ");
                    }
                    return;
                }
            }
            /**
             * This Override is a candidate for schedule override, check if
             * it is within the window.
             */
            if (checkWindow(shift, overrideStartTime.getTime(), workDate) != 0) {
                //Not in window, do nothing
                if (logger.isInfoEnabled()) {
                    logger.info(
                        "Second clock not withing window, no schedule override created");
                }
                return;
            }
            /**
             * This clock is within the window and a schedule alread exists.
             *
             * Cancel the old schedule and create a new one based on this clock.
             */

            OverrideAccess oa;

            oa = new OverrideAccess(conn);
            try {
                oa.cancelByRangeAndType(empId, workDate, workDate,
                                        OverrideData.SCHEDULE_SCHEDTIMES_TYPE,
                                        OverrideData.SCHEDULE_SCHEDTIMES_TYPE, null);
            }
            catch (java.sql.SQLException e) {
                throw new NestedRuntimeException(
                    "Can not cancel schedule override for employee " + empId +
                    ON_STRING + workDate, e);
            }
            if (logger.isInfoEnabled()) {
                logger.info(
                    "Last schedule canceled, new schedule based on new clock will be created");
            }

            /**
             * At this point the override is ether of the same type and later
             * or different type and earlier then the previously scheduled
             * override. As well as being within the flex window.
             *
             * The old override is canceled and this override will be scheduled
             */
        }

        //Schedule Override
        Calendar newClockTime;

        newClockTime = DateHelper.getCalendar();
        newClockTime = convertClockToCalendar(DateHelper.convertDateString(
            clock.getClockDate(), "00yyyyMMddHHmmss"));

        Calendar windowStartTime = DateHelper.getCalendar();
        Calendar windowEndTime = DateHelper.getCalendar();
        Calendar windowStart = DateHelper.getCalendar();
        Calendar windowEnd = DateHelper.getCalendar();
        int shiftDuration = 0;

        windowStartTime = convertShiftToCalendar(shift.getShftUdf1());
        windowEndTime = convertShiftToCalendar(shift.getShftUdf2());

        windowStart.setTime(DateHelper.setTimeValues(clock.getClockDate(),
            windowStartTime.getTime()));
        windowEnd.setTime(DateHelper.setTimeValues(clock.getClockDate(),
            windowEndTime.getTime()));

        if (newClockTime.getTime().before(windowStart.getTime())) {
            //Start Time Before window
            newClockTime.setTime(windowStart.getTime());
        }
        else if (newClockTime.getTime().after(windowEnd.getTime())) {
            //Start Time After window
            newClockTime.setTime(windowEnd.getTime());
        }

        shiftDuration = new Integer(shift.getShftUdf3()).intValue();

        //Build Override
        if (logger.isInfoEnabled()) {
            logger.info("SET CLOCK FROM SHIFT START " +
                        newClockTime.getTime());
        }
        OverrideBuilder ob;
        InsertEmployeeScheduleOverride ins;

        ob = new OverrideBuilder(conn);
        ins = new InsertEmployeeScheduleOverride(conn);

        ins.setEmpId(empId);
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setStartDate(workDate);
        ins.setEndDate(workDate);
        ins.setOvrComment("Automatic Schedule Time Override For Flex Shift");
        ins.setWbuNameBoth(WORKBRAIN, WORKBRAIN);
        ins.setOvrUdf1(String.valueOf(clockId));

        ins.setEmpskdActStartTime(newClockTime.getTime());
        ins.setEmpskdActEndTime(DateHelper.addMinutes(newClockTime.getTime(),
            shiftDuration));

        ob.add(ins);

        try {
            ob.execute(true);
        }
        catch (OverrideException e) {
            throw new NestedRuntimeException(
                "Can not create schedule override for employee " + empId +
                ON_STRING + workDate, e);
        }
    }

    /**
     *  This captures all eligable overrides for flex shift and checkes if the
     * employee is on a flex shift. If they are candidates then the following
     * four cases are applied.
     *
     * No existing schedule:
     * Creates schedule override from work summray clocks or time of override.
     *
     * Schedule exists:
     * -If this is the second override of the sametype within the window then
     * the old schedule is canceled and a new one is created.
     * -If it is a different type and earlier then a new schedule is created
     *
     * See 4.1.1.1 of tech doc
     *
     * @param od OverrideData
     * @param c DBConnection
     * @throws RuleEngineException
     */
    public void beforeOverrideInsert(OverrideData od, DBConnection c) throws
        RuleEngineException {

        /**
         * The override type must be flex enabled.
         */
        OverrideTypeAccess ota = new OverrideTypeAccess(c);
        OverrideTypeData otd = ota.load(od.getOvrtypId());

        if (otd.getOvrtypFlag1() == null) {
            if (logger.isInfoEnabled()) {
                logger.info(
                    "Override is not flex enabled: " + otd.getOvrtypName());
            }
            return;
        }
        if (!otd.getOvrtypFlag1().equalsIgnoreCase("Y")) {
            if (logger.isInfoEnabled()) {
                logger.info(
                    "Override is not flex enabled: " + otd.getOvrtypName());
            }
            return;
        }

        /**
         * Check employee is on a flex shift using CodeMapper and
         * EmployeeScheduleAccess
         */
        int empId;
        java.util.Date workDate;
        CodeMapper codeMapper;
        EmployeeAccess ea;
        EmployeeScheduleAccess esa;
        EmployeeScheduleData esd;
        
        ShiftData shift;

        empId = od.getEmpId();
        workDate = od.getOvrStartDate();
        codeMapper = (CodeMapper) c.getCodeMapper();

        ea = new EmployeeAccess(c, codeMapper);
        esa = new EmployeeScheduleAccess(c, codeMapper);

        try {
            esd = esa.load(ea.load(empId, workDate), workDate);
        }
        catch (java.sql.SQLException e) {
            throw new NestedRuntimeException(
                "Can not Access employee Data " + empId, e);
        }

        shift = codeMapper.getShiftById(esd.getEmpskdActShiftId());
        if (shift == null) {
            if (logger.isInfoEnabled()) {
                logger.info(
                    ERROR_NOT_FLEX + empId);
            }
            return;
        }
        if (shift.getShftFlag1() == null) {
            if (logger.isInfoEnabled()) {
                logger.info(
                    ERROR_NOT_FLEX + empId);
            }
            return;
        }
        if (!shift.getShftFlag1().equalsIgnoreCase("Y")) {
            if (logger.isInfoEnabled()) {
                logger.info(
                    ERROR_NOT_FLEX + empId);
            }
            return;
        }
        /**
         * Check if employee already has a schedule override.
         */
        if (esd.isEmployeeScheduledActual()) {
            //if (schedTimeOvr != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Employee has a scheduled override for this day");
            }
            /**
             * An override exisit, compare times and types to determing
             * if a new schedule should be created.
             */
            
            Calendar scheduleStartTime;
            Calendar overrideStartTime;

            scheduleStartTime = DateHelper.getCalendar();
            overrideStartTime = DateHelper.getCalendar();
            scheduleStartTime.setTime(esd.getEmpskdActStartTime());
            overrideStartTime.setTime(getClockOverrideStartTime(od, c));

            if (scheduleStartTime.getTime().before(overrideStartTime.getTime())) {
                if (logger.isInfoEnabled()) {
                    logger.info(
                        "Employee has a scheduled start time of " +
                        scheduleStartTime.getTime() +
                        " which is before clock start time of " +
                        overrideStartTime.getTime());
                }
                //This is the second override, compair types
                //OverrideData schedTimeOvr = null;
                int lastOvrType;
                OverrideData schedTimeOvr = null;

                schedTimeOvr = getSchedTimeOvr(empId, workDate, c);
                lastOvrType = Integer.parseInt(schedTimeOvr.getOvrUdf1());
                if (od.getOvrtypId() != lastOvrType) {
                    if (logger.isInfoEnabled()) {
                        logger.info(
                            "Override types are different no schedule override created ");
                    }
                    //Different Override Types, do nothing
                    return;
                }
            }
            /**
             * This Override is a candidate for schedule override, check if
             * it is within the window.
             */
            if (checkWindow(shift, overrideStartTime.getTime(), workDate) != 0) {
                //Not in window, do nothing
                if (logger.isInfoEnabled()) {
                    logger.info(
                        "Second clock not withing window, no schedule override created");
                }
                return;
            }
            //With in window, Cancel scheduled override
            OverrideAccess oa;

            oa = new OverrideAccess(c);
            try {
                oa.cancelByRangeAndType(empId, workDate, workDate,
                                        OverrideData.SCHEDULE_SCHEDTIMES_TYPE,
                                        OverrideData.SCHEDULE_SCHEDTIMES_TYPE, DateHelper.DATE_3000);
            }
            catch (java.sql.SQLException e) {
                throw new NestedRuntimeException(
                    "Can not cancel schedule override for employee " + empId +
                    ON_STRING + workDate, e);
            }
            if (logger.isInfoEnabled()) {
                logger.info(
                    "Last schedule canceled, new schedule based on new clock will be created");
            }

            /**
             * At this point the override is ether of the same type and later
             * or different type and earlier then the previously scheduled
             * override. As well as being within the flex window.
             *
             * The old override is canceled and this override will be scheduled
             */
        }

        //Schedule Override
        createScheduleOverrideFromOverrideData(od, shift, c);

    }

    /**
     * Checks for canceled overrides that created a schedule override.
     *
     * If the canceled override created a schedule override then that scheduled
     * override is also canceled and a new scheduled override is created from
     * the last eligable applied override. If no override exists then no
     * scheduled override will be created.
     *
     * see 4.1.3 of the tech doc
     *
     * @param od OverrideData
     * @param c DBConnection
     */
    public void beforeSaveOneOverride(OverrideData od, DBConnection c) {

        /**
         * The override status must be CANCELLED and the override must be
         * flex eligable.
         */
        if (od.getOvrStatus().equalsIgnoreCase(OverrideData.CANCEL)) {
            OverrideTypeAccess ota = new OverrideTypeAccess(c);
            OverrideTypeData otd = ota.load(od.getOvrtypId());
            
            if ( (otd.getOvrtypFlag1() == null) ||
                (!otd.getOvrtypFlag1().equalsIgnoreCase("Y"))) {
                if (logger.isInfoEnabled()) {
                    logger.info(
                        "Override is not flex enabled: " + otd.getOvrtypName());
                }
                return;
            }

            /**
             * Employee must be on a flex shift.
             *
             * Check using CodeMapper and EMployeeScheduleAccess
             */
            int empId;
            java.util.Date workDate;
            CodeMapper codeMapper;
            EmployeeAccess ea;
            EmployeeScheduleAccess esa;
            EmployeeScheduleData esd;
            
            ShiftData shift;

            empId = od.getEmpId();
            workDate = od.getOvrStartDate();
            codeMapper = (CodeMapper) c.getCodeMapper();

            ea = new EmployeeAccess(c, codeMapper);
            esa = new EmployeeScheduleAccess(c, codeMapper);

            try {
                esd = esa.load(ea.load(empId, workDate), workDate);
            }
            catch (java.sql.SQLException e) {
                throw new NestedRuntimeException(
                    "Can not Access employee Data " + empId, e);
            }

            shift = codeMapper.getShiftById(esd.getEmpskdActShiftId());

            if (!"Y".equalsIgnoreCase(shift.getShftFlag1())) {
                if (logger.isInfoEnabled()) {
                    logger.info(
                        ERROR_NOT_FLEX + empId);
                }
                return;
            }

            /**
             * If a schedule already exists, compare it to override to see if
             * it was created from this override.
             */
            if (esd.isEmployeeScheduledActual()) {
                if (logger.isInfoEnabled()) {
                    logger.info(
                        "Employee has a scheduled override for this day");
                }
                
                //Check the start time of the last scheduled override
                Calendar scheduleStartTime;
                Calendar overrideStartTime;

                scheduleStartTime = DateHelper.getCalendar();
                overrideStartTime = DateHelper.getCalendar();
                scheduleStartTime.setTime(esd.getEmpskdActStartTime());
                overrideStartTime.setTime(getClockOverrideStartTime(od, c));

                //Check if this is the override that created the schedule override
                if (scheduleStartTime.getTime().compareTo(
                			overrideStartTime.getTime()) == 0) {
                	
                    //Times are equal Check override Type
                    int lastOvrType;
                    OverrideData schedTimeOvr = null;

                    schedTimeOvr = getSchedTimeOvr(empId, workDate, c);
                    lastOvrType = Integer.parseInt(schedTimeOvr.getOvrUdf1());
                    if (od.getOvrtypId() != lastOvrType) {
                        if (logger.isInfoEnabled()) {
                            logger.info(
                                "Override types are different no schedule override canceled ");
                        }
                        //Different Override Types, do nothing
                        return;
                    }
                    /**
                     * Override types and start time is the same. This override
                     * created the schedule override.
                     *
                     * We must now cancele the schedule override and create a
                     * new one based on the last applied override.
                     *
                     */
                    cancelFlexScheduleTimeOverride(od, c);
                    OverrideData lastOvr = null;
                    try {
                        lastOvr = getLastAppliedOvr(od.getEmpId(),
                            od.getOvrStartDate(), c);
                    }
                    catch (Exception e) {
                        throw new NestedRuntimeException(
                            "Error Getting last override ", e);
                    }

                    if (lastOvr == null) {
                        //Do nothing
                        return;
                    }
                    
                    //Create Scheduled Override from lastOvr
                    createScheduleOverrideFromOverrideData(lastOvr, shift, c);

                }
            }

        }
    }

    /**
     * Chaging a shift from a Flex type to anything else canceles Flex overrides
     *
     * See 4.1.3 of the tech doc
     *
     * @param od OverrideData
     * @param c DBConnection
     * @throws RuleEngineException
     */
    public void beforeScheduleOverrideInsert(OverrideData od,
                                             DBConnection c) throws
        RuleEngineException {
        try {
            /**
             * If the override changed the employee schedule then we must
             * cancele any flex shift created.
             */
            OverrideData.OverrideToken ot = null;
            ot = od.getNewOverrideByName("EMPSKD_ACT_SHIFT_NAME");
            if (ot != null) {
                CodeMapper codeMapper = (CodeMapper) c.getCodeMapper();
                EmployeeAccess ea = new EmployeeAccess(c, codeMapper);
                EmployeeScheduleAccess esa = new EmployeeScheduleAccess(c,
                    codeMapper);
                EmployeeData empData = null;
                EmployeeScheduleData empSchedData = null;
                ShiftData shift = null;

                empData = (EmployeeData) ea.loadRecordDataByPrimaryKey(new
                    EmployeeData(), od.getEmpId());
                empSchedData = esa.load(empData, od.getOvrStartDate());
                shift = codeMapper.getShiftById(empSchedData.
                                                getEmpskdActShiftId());

                if (shift.getShftFlag1() != null &&
                    shift.getShftFlag1().equalsIgnoreCase("Y")) {
                    cancelFlexScheduleTimeOverride(od, c);
                }
            }
        }
        catch (Exception e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
                logger.debug("Error in determing new Clock Information " +
                             e);
            }
        }

    }

    /**
     * Using the override all scheduled overrided with in the start and end
     * date are canceled for that employee
     *
     * @param od OverrideData
     * @param c DBConnection
     */
    public void cancelFlexScheduleTimeOverride(OverrideData od,
                                               DBConnection c) {
        //EmployeeAccess ea = new EmployeeAccess(c,codeMapper);
        OverrideAccess oa = new OverrideAccess(c);
        try {
            oa.cancelByRangeAndType(od.getEmpId(), od.getOvrStartDate(),
                                    od.getOvrStartDate(),
                                    OverrideData.SCHEDULE_SCHEDTIMES_TYPE,
                                    OverrideData.SCHEDULE_SCHEDTIMES_TYPE,
                                    DateHelper.getLatestEmployeeDate());
        }
        catch (SQLException s) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
                logger.debug(
                    "Error in During Cancelling Flex Schedule Override " +
                    s);
            }
        }
    }

    /**
     * Converts a string clock data into a Calendar object.
     *
     * Clock in format XXyyyyMMddhhmmss
     *
     * @param clock String
     * @return Calendar
     */
    public static Calendar convertClockToCalendar(String clock) {
        try {
            int year = new Integer(clock.substring(2, 6)).intValue();
            int mth = new Integer(clock.substring(6, 8)).intValue();
            int day = new Integer(clock.substring(8, 10)).intValue();
            int hr = new Integer(clock.substring(10, 12)).intValue();
            int min = new Integer(clock.substring(12, 14)).intValue();
            int sec = new Integer(clock.substring(14, 16)).intValue();
            Calendar cal = DateHelper.getCalendar();
            cal.clear();
            cal.set(year, mth - 1, day, hr, min, sec);

            return cal;
        }
        catch (Exception e) {
            throw new NestedRuntimeException(
                "Invalid date format in clock " + clock, e);
        }
    }

    /**
     * Converts a string shift values in to a date object.
     *
     * Shift in format yyyyMMddhhmmss
     *
     * @param shift String
     * @return Calendar
     */
    public static java.util.Date convertFlexTimeStringToDate(String
        clockDate) {
        try {
            int year = Integer.parseInt(clockDate.substring(0, 4));
            int month = Integer.parseInt(clockDate.substring(4, 6));
            int day = Integer.parseInt(clockDate.substring(6, 8));
            int hour = Integer.parseInt(clockDate.substring(9, 11));
            int minute = Integer.parseInt(clockDate.substring(11, 13));
            int second = Integer.parseInt(clockDate.substring(13, 15));
            Calendar cal = DateHelper.getCalendar();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, second);
            cal.set(Calendar.MILLISECOND, 0);

            return cal.getTime();

        }
        catch (Exception e) {
            throw new NestedRuntimeException(
                "Invalid date format in clock_time " + clockDate, e);
        }
    }

    /**
     * Converts a string shift values in to a Calendar object.
     *
     * Shift in format yyyyMMddhhmmss
     *
     * @param shift String
     * @return Calendar
     */
    public static Calendar convertShiftToCalendar(String shift) {
        try {
            int year = new Integer(shift.substring(0, 4)).intValue();
            int mth = new Integer(shift.substring(4, 6)).intValue();
            int day = new Integer(shift.substring(6, 8)).intValue();
            int hr = new Integer(shift.substring(9, 11)).intValue();
            int min = new Integer(shift.substring(11, 13)).intValue();
            int sec = new Integer(shift.substring(13, 14)).intValue();
            Calendar cal = DateHelper.getCalendar();
            cal.set(year, mth, day, hr, min, sec);
            return cal;
        }
        catch (Exception e) {
            throw new NestedRuntimeException(
                "Invalid date format in shift " + shift, e);
        }
    }

    /**
     * Returns the OverrideToken by name WRKS_CLOCKS is not null then the
     * clock data is returned as Date. Otherwise the override start time is
     * returned.
     *
     * @param od OverrideData
     * @param c DBConnection
     * @return Date
     */
    public static java.util.Date getClockOverrideStartTime(OverrideData od,
        DBConnection c) {
        String inTime = null;
        try {
            Calendar newClockTime = DateHelper.getCalendar();
            OverrideData.OverrideToken ot = null;
            if (od.getOvrtypId() == OverrideData.WORK_SUMMARY_TYPE_START ||
                od.getOvrtypId() == OverrideData.WORK_DETAIL_TYPE_END) {
                ot = od.getNewOverrideByName("WRKS_CLOCKS");
            }
            if (ot != null) {
                StringTokenizer st = new StringTokenizer(ot.getValue(), "~");
                inTime = st.nextToken();
                if (inTime.substring(17, 18).equals("1")) {
                    newClockTime = convertClockToCalendar(inTime);
                }
            }
            else {
                Date startTime = od.getOvrStartTime();
                if (startTime == null) {
                    startTime = od.getOvrStartDate();
                }
                newClockTime.setTime(startTime);

            }
            return newClockTime.getTime();
        }
        catch (Exception e) {
            throw new NestedRuntimeException(
                "Invalid clock format WRKS_CLOCKS " + inTime, e);
        }
    }

    /**
     * Return the scheduled override for given employee and workdate.
     *
     * @param empId int
     * @param workDate Date
     * @param c DBConnection
     * @return OverrideData
     */
    public static OverrideData getSchedTimeOvr(int empId,
                                               java.util.Date workDate,
                                               DBConnection c) {
        OverrideAccess oa = new OverrideAccess(c);
        try {
            String[] status = new String[1];
            status[0] = "APPLIED";
            OverrideList ol = oa.load(empId, workDate, workDate);
            OverrideList ol1 = ol.filter(workDate, workDate, status,
            											OverrideData.SCHEDULE_SCHEDTIMES_TYPE, 
														OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
            ol1.sortByCreateDate();
            if (!ol1.isEmpty()) {
                return ol1.getOverrideData(0);
            }

        }
        catch (java.sql.SQLException e) {
            throw new NestedRuntimeException(
                "Override Access Error ", e);
        }
        return null;
    }

    /**
     * Returns the last flex shift applied over ride for given employee
     * date, override ID, and shift.
     *
     * @param empId int
     * @param workDate Date
     * @param c DBConnection
     * @throws Exception
     * @return OverrideData
     */
    public OverrideData getLastAppliedOvr(int empId, java.util.Date workDate,
                                          DBConnection c) throws Exception {
        try {
            OverrideAccess oa = new OverrideAccess(c);
            OverrideList ol;
            OverrideData lastOD = null;
            OverrideData tmpOD;

            ol = oa.load(empId, workDate, workDate);

            Iterator it = ol.iterator();

            while (it.hasNext()) {
                tmpOD = (OverrideData) it.next();
                if (tmpOD.isApplied()) {
                    OverrideTypeAccess ota = new OverrideTypeAccess(c);
                    OverrideTypeData otd = ota.load(tmpOD.getOvrtypId());
                    if ( (otd.getOvrtypFlag1() != null) &&
                        (otd.getOvrtypFlag1().equalsIgnoreCase("Y"))) {
                        //Eligable Override.
                        if (lastOD == null) {
                            lastOD = tmpOD;
                        }
                        else {
                            //Set start times to OvrStartTime, unless it's null, then use OvrStartDate
                            Date tmpStartTime = (tmpOD.getOvrStartTime() == null) ?
                                    tmpOD.getOvrStartDate() : tmpOD.getOvrStartTime();
                            Date lastStartTime = (lastOD.getOvrStartTime() == null) ?
                                    lastOD.getOvrStartDate() : lastOD.getOvrStartTime();
                            if (tmpStartTime.before(lastStartTime)) {
                                //This is an earlier override
                                lastOD = tmpOD;
                            }
                        }
                    }
                }
            }
            return lastOD;

        }
        catch (java.lang.Exception e) {
            throw new NestedRuntimeException("Error getting last Override", e);
        }

    }

    /**
     * Creates a scheduled override from the override data.
     *
     * @param od OverrideData
     * @param shift ShiftData
     * @param c DBConnection
     */
    public void createScheduleOverrideFromOverrideData(OverrideData od,
        ShiftData shift, DBConnection c) {

        int empId;
        int shiftDuration;
        java.util.Date workDate;
        Calendar newClockTime;
        Calendar windowStartTime;
        Calendar windowEndTime;
        Calendar windowStart;
        Calendar windowEnd;
        OverrideBuilder ob;
        InsertEmployeeScheduleOverride ins;

        empId = od.getEmpId();
        workDate = od.getOvrStartDate();

        newClockTime = DateHelper.getCalendar();
        newClockTime.setTime(getClockOverrideStartTime(od, c));

        windowStartTime = DateHelper.getCalendar();
        windowEndTime = DateHelper.getCalendar();
        windowStart = DateHelper.getCalendar();
        windowEnd = DateHelper.getCalendar();

        shiftDuration = 0;

        windowStartTime = convertShiftToCalendar(shift.getShftUdf1());
        windowEndTime = convertShiftToCalendar(shift.getShftUdf2());

        windowStart.setTime(DateHelper.setTimeValues(od.
            getOvrStartDate(),
            windowStartTime.getTime()));
        windowEnd.setTime(DateHelper.setTimeValues(od.getOvrEndDate(),
            windowEndTime.getTime()));

        if (newClockTime.getTime().before(windowStart.getTime())) {
            //Start Time Before window
            newClockTime.setTime(windowStart.getTime());
        }
        else if (newClockTime.getTime().after(windowEnd.getTime())) {
            //Start Time After window
            newClockTime.setTime(windowEnd.getTime());
        }

        shiftDuration = new Integer(shift.getShftUdf3()).intValue();

        //Build Override
        if (logger.isInfoEnabled()) {
            logger.info(
                "SET CLOCK FROM SHIFT START " +
                newClockTime.getTime());
        }

        ob = new OverrideBuilder(c);
        ins = new InsertEmployeeScheduleOverride(c);

        ins.setEmpId(empId);
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setStartDate(od.getOvrStartDate());
        ins.setEndDate(od.getOvrEndDate());
        ins.setOvrComment(
            "Automatic Schedule Time Override For Flex Shift");
        ins.setWbuNameBoth(WORKBRAIN, WORKBRAIN);
        ins.setOvrUdf1(String.valueOf(od.getOvrtypId()));

        ins.setEmpskdActStartTime(newClockTime.getTime());
        ins.setEmpskdActEndTime(DateHelper.addMinutes(newClockTime.
            getTime(),
            shiftDuration));

        ob.add(ins);

        try {
            ob.execute(true);
        }
        catch (OverrideException e) {
            throw new NestedRuntimeException(
                "Can not create schedule override for employee " +
                empId +
                ON_STRING + workDate, e);
        }

    }

    /**
     * Compairs dateItem to the flex shift window. If dateItem is before the
     * window then -1 is returned, 1 after and 0 if it is within the window.
     *
     * @param shift ShiftData
     * @param dateItem Date
     * @param workDate Date
     * @return int
     */
    public int checkWindow(ShiftData shift, java.util.Date dateItem,
                           java.util.Date workDate) {

        Calendar windowStartTime;
        Calendar windowEndTime;
        Calendar windowStart;
        Calendar windowEnd;

        windowStartTime = DateHelper.getCalendar();
        windowEndTime = DateHelper.getCalendar();
        windowStart = DateHelper.getCalendar();
        windowEnd = DateHelper.getCalendar();

        windowStartTime = convertShiftToCalendar(shift.getShftUdf1());
        windowEndTime = convertShiftToCalendar(shift.getShftUdf2());

        windowStart.setTime(DateHelper.setTimeValues(workDate,
            windowStartTime.getTime()));
        windowEnd.setTime(DateHelper.setTimeValues(workDate,
            windowEndTime.getTime()));

        if (windowStart.getTime().after(dateItem)) {
            return -1; //Before window
        }
        else if (windowEnd.getTime().before(dateItem)) {
            return 1; //After Window
        }
        else {
            return 0; //Within Window
        }

    }
}
