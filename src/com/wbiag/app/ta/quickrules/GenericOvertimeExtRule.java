package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.rules.*;

import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;
import java.text.*;
import java.util.*;

/**
 * - Accepts EMP_VALs as thresholds
 * i.e REG=EMP_VAL1|2400,OT2-9999
 */

public class GenericOvertimeExtRule extends GenericOvertimeRule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GenericOvertimeExtRule.class);

    private static List eligibleEmpVals = new ArrayList();
    public static String EMP_VAL_PREFIX = "EMP_VAL";
    public static String EMP_UDF_PREFIX = "EMP_UDF";
    private static String EMP_VAL_HOUR = "*60";
    public final static String PARAM_ELIGIBLE_WORK_DETAIL_EXPRESSION_STRING = "WorkDetailExpressionString";

    static {
        for (int i = 1; i <= 20; i++) {
            eligibleEmpVals.add(EMP_VAL_PREFIX + i);
        }

    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {

        if (wbData.getRuleData().getWorkDetailCount() == 0) {
            // **** Nothing to apply the rule to ****
            return;
        }

        // *** Parameters ***
        Parameters hourSet = Parameters.parseParameters(parameters.getParameter(PARAM_HOURSET_DESCRIPTION, "REG=0,OT1=9999"));
        boolean addPremiumForFirstHourtypeToken;
        try {
            addPremiumForFirstHourtypeToken = Boolean.valueOf(parameters.
                getParameter(PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN, "false")).
                booleanValue();
        }
        catch (IllegalArgumentException e) {
            addPremiumForFirstHourtypeToken = false;
        }

        Date in_start_time = null;
        Date in_end_time = null;
        Date in_seed_start_time = null;
        Date in_seed_end_time = null;
        Date in_seed_start_date = null;
        Date in_seed_end_date = null;
        boolean in_inclusive = true;
        String str = null;
        try {
            str = parameters.getParameter(PARAM_ARE_TIMECODES_INCLUSIVE);
        } catch (IllegalArgumentException e) {
            str = null;
        }
        if ("false".equalsIgnoreCase(str)) {
            in_inclusive = false;
        }
        int in_wrks_id = wbData.getRuleData().getWorkSummary().getWrksId();
        DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);

        try {
            in_start_time = parameters.getParameter(PARAM_STARTTIME_WITHIN_SHIFT) == null ? null :
                dateFormat.parse(parameters.getParameter(PARAM_STARTTIME_WITHIN_SHIFT));
        } catch (IllegalArgumentException e) {
            in_start_time = null;
        }

        try {
            in_end_time = parameters.getParameter(PARAM_ENDTIME_WITHIN_SHIFT) == null ? null :
                dateFormat.parse(parameters.getParameter(PARAM_ENDTIME_WITHIN_SHIFT));
        } catch (IllegalArgumentException e) {
            in_end_time = null;
        }

        try {
            in_seed_start_time = parameters.getParameter(PARAM_INTERVAL_STARTTIME) == null ? null :
                dateFormat.parse(parameters.getParameter(PARAM_INTERVAL_STARTTIME));
        } catch (IllegalArgumentException e) {
            in_seed_start_time = null;
        }

        try {
            in_seed_end_time = parameters.getParameter(PARAM_INTERVAL_ENDTIME) == null ? null :
                dateFormat.parse(parameters.getParameter(PARAM_INTERVAL_ENDTIME));
        } catch (IllegalArgumentException e) {
            in_seed_end_time = null;
        }

        try {
            in_seed_start_date = parameters.getParameter(PARAM_INTERVAL_STARTDATE) == null ? null :
                dateFormat.parse(parameters.getParameter(PARAM_INTERVAL_STARTDATE));
        } catch (IllegalArgumentException e) {
            in_seed_start_date = null;
        }

        try {
            in_seed_end_date = parameters.getParameter(PARAM_INTERVAL_ENDDATE) == null ? null :
               dateFormat.parse(parameters.getParameter(PARAM_INTERVAL_ENDDATE));
        } catch (IllegalArgumentException e) {
            in_seed_end_date = null;
        }

        int in_seed_minutes = 0;
        try {
            in_seed_minutes = parameters.getIntegerParameter(PARAM_ADDITIONAL_MINUTES_WORKED, 0);
        } catch (IllegalArgumentException e) {
            in_seed_minutes = 0;
        }

        String in_inclInSumHTypeList = "REG";
        try {
            in_inclInSumHTypeList = parameters.getParameter(PARAM_ELIGIBLE_HOURTYPES, "REG");
        } catch (IllegalArgumentException e) {
            in_inclInSumHTypeList = "REG";
        }

        String in_wrkdTCodeNameList = null;
        try {
            in_wrkdTCodeNameList = parameters.getParameter(PARAM_ELIGIBLE_TIMECODES);
        } catch (IllegalArgumentException e) {
            in_wrkdTCodeNameList = null;
        }

        String wdExprString = null;
        try {
            wdExprString = parameters.getParameter(PARAM_ELIGIBLE_WORK_DETAIL_EXPRESSION_STRING);
        } catch (IllegalArgumentException e) {
            wdExprString = null;
        }

        str = "false";
        try {
            str = parameters.getParameter(PARAM_ADD_PREMIUMRECORD, "false");
        } catch (IllegalArgumentException e) {
            str = "false";
        }
        boolean addPremium = Boolean.valueOf(str).booleanValue();

        String premiumTimeCode = "WRK";
        try {
            premiumTimeCode = parameters.getParameter(PARAM_PREMIUM_TIMECODE, "WRK");
        } catch (IllegalArgumentException e) {
            premiumTimeCode = "WRK";
        }

        Date skdZoneStartDate = wbData.getWrksWorkDate();
        try {
            skdZoneStartDate = parameters.getParameter(PARAM_SKDZONE_STARTDATE) == null
                           ? wbData.getWrksWorkDate()
                           : dateFormat.parse(parameters.getParameter(PARAM_SKDZONE_STARTDATE));
        } catch (IllegalArgumentException e) {
            skdZoneStartDate = wbData.getWrksWorkDate();
        }

        Date skdZoneEndDate = wbData.getWrksWorkDate();
        try {
            skdZoneEndDate = parameters.getParameter(PARAM_SKDZONE_ENDDATE) == null
                ? wbData.getWrksWorkDate()
                : dateFormat.parse(parameters.getParameter(PARAM_SKDZONE_ENDDATE));
        } catch (IllegalArgumentException e) {
            skdZoneEndDate = wbData.getWrksWorkDate();
        }

        String hourTypeForOvertimeWorkDetails = null;
        try {
            hourTypeForOvertimeWorkDetails = parameters.getParameter(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, null);
        } catch (IllegalArgumentException e) {
            hourTypeForOvertimeWorkDetails = null;
        }

        boolean assignBetterRateInternal;
        try {
            assignBetterRateInternal = Boolean.valueOf(parameters.getParameter(PARAM_ASSIGN_BETTERRATE)).booleanValue();
        } catch (IllegalArgumentException e) {
            //take the value from the protected field (which is now depricated...in future replace assignment with TRUE)
            assignBetterRateInternal = true;
        }

        // *** overtime logic
        Date dat_start_time = new Date(in_start_time == null ? wbData.getRuleData().getWorkDetail(0).getWrkdStartTime().getTime() : in_start_time.getTime());
        Date dat_end_time = new Date(in_end_time == null ? wbData.getRuleData().getWorkDetail(wbData.getRuleData().getWorkDetailCount() - 1).getWrkdEndTime().getTime() : in_end_time.getTime());
        Date dat_min_time = new Date();

        int int_seed_minutes = getWorkedMinutes(wbData, in_wrks_id, in_seed_start_date, in_seed_end_date, in_seed_start_time,
            in_seed_end_time, in_inclusive, in_wrkdTCodeNameList, in_inclInSumHTypeList, wdExprString);
        int_seed_minutes += in_seed_minutes;
        pluck(wbData , dat_start_time,
                     dat_end_time,
                     in_inclusive,
                     in_wrkdTCodeNameList,
                     true,
                     in_inclInSumHTypeList , wdExprString);
        if (wbData.getRuleData().getWorkDetailCount() > 0) {
            dat_min_time.setTime(dat_start_time.getTime());
            dat_start_time.setTime(dat_start_time.getTime() - int_seed_minutes * DateHelper.MINUTE_MILLISECODS);
            WorkDetailList splitWorkDetailList = splitWorkDetailsByHourSetDurations(wbData, parameters);
            Enumeration enum = hourSet.getParameters();
            int iHourSetIndex = 0;
            int last_int_minutes = 0;
            while (enum.hasMoreElements()) {
                String hourTypeName = ((Parm) enum.nextElement())._name;
                boolean uptoFlag = false;
                String int_minutes_str = hourSet.getParameter(hourTypeName);
                if (int_minutes_str.endsWith("*")) {
                    uptoFlag = true;
                    int_minutes_str = int_minutes_str.substring(0, int_minutes_str.length()-1);
                }
                int int_minutes = evaluateHoursetDesc(wbData , int_minutes_str);
                if (int_minutes == 0) {
                    // -- if current wrks date, use memory data
                    if (in_seed_start_date == null && in_seed_end_date == null) {
                         // -- duration excludes breaks
                        int_minutes = wbData.getScheduleDuration(true);
                    } else {
                        // -- if range specified, get the total schedule mins of the range
                        // -- duration excludes breaks
                        int_minutes = wbData.getScheduleDurationRange(
                                wbData.getRuleData().getEmployeeData().getEmpId(),
                                DateHelper.truncateToDays(skdZoneStartDate),
                                DateHelper.truncateToDays(skdZoneEndDate),
                                true);
                    }
                }
                int temp_int_minutes  = int_minutes;
                if (uptoFlag) {
                    int_minutes = int_minutes - last_int_minutes >= 0 ? int_minutes - last_int_minutes : int_minutes;
                }
                last_int_minutes = temp_int_minutes;

                dat_end_time.setTime(dat_start_time.getTime() + int_minutes * DateHelper.MINUTE_MILLISECODS);
                if (dat_end_time.after(dat_min_time)) {
                    Date maxStart = RuleHelper.max(dat_start_time, dat_min_time);
                    Date minEnd = RuleHelper.min(dat_end_time, wbData.getMaxEndTime(null, false));
                    /**
                     * adding premium used to be dependant on multiple, however some clients
                     * use hour types that have multiples <= 1 which caused the premiums not to be created.
                     * The assumption is everything after first hourset index causes overtime.
                     *
                     * If the client must assign premium to the first hourtype token, then they can do it through
                     * seeting addPremiumForFirstHourtypeToken to true, this is done for TT25461
                     */
                    if ( addPremium && (iHourSetIndex!=0 || addPremiumForFirstHourtypeToken) ) {
                        int minutes = RuleHelper.diffInMinutes(maxStart, minEnd);
                        if (minutes > 0) {
                            if (splitWorkDetailList != null) {
                                for (int i = 0; i < splitWorkDetailList.size(); i++) {
                                    WorkDetailData wrkDetail = splitWorkDetailList.getWorkDetail(i);
                                    if (wrkDetail.getWrkdStartTime().getTime() >= maxStart.getTime()
                                            && wrkDetail.getWrkdEndTime().getTime() <= minEnd.getTime()) {

                                        wbData.insertWorkPremiumRecord(wrkDetail.getWrkdMinutes(), premiumTimeCode, hourTypeName, wrkDetail);
                                        if (!StringHelper.isEmpty(hourTypeForOvertimeWorkDetails)) {
                                            wbData.setWorkDetailHtypeName(hourTypeForOvertimeWorkDetails,
                                                maxStart,
                                                minEnd,
                                                null,
                                                false
                                                );
                                        }
                                    }
                                }
                            } else {
                                wbData.insertWorkPremiumRecord(minutes, premiumTimeCode, hourTypeName);
                                if (!StringHelper.isEmpty(hourTypeForOvertimeWorkDetails)) {
                                    wbData.setWorkDetailHtypeName(hourTypeForOvertimeWorkDetails,
                                        maxStart,
                                        minEnd,
                                        null,
                                        false
                                        );
                                }
                            }
                        }
                    } else {
                        wbData.setWorkDetailHtypeId(
                                wbData.getRuleData().getCodeMapper().getHourTypeByName(hourTypeName).getHtypeId(),
                                maxStart,
                                dat_end_time,
                                null,
                                assignBetterRateInternal
                                );

                    }
                }
                dat_start_time.setTime(dat_end_time.getTime());
                iHourSetIndex ++;
            }
        }
        wbData.unpluck();
    }

    private int evaluateHoursetDesc(WBData wbData,
                                    String int_minutes_str) throws Exception {

        int int_minutes = 0;
        String mins = int_minutes_str;
        boolean isHour = false;
        // *** check i.e REG=EMP_VAL1|2400
        if (int_minutes_str.startsWith(EMP_VAL_PREFIX)) {
            int defMinsIndex = int_minutes_str.indexOf("|");
            String defMins = null;
            if (defMinsIndex >= 0) {
                mins = int_minutes_str.substring(0, defMinsIndex);
                defMins = int_minutes_str.substring(defMinsIndex + 1);
            }
            if (mins.endsWith(EMP_VAL_HOUR)) {
                isHour = true;
                mins = mins.substring(0, mins.indexOf(EMP_VAL_HOUR) ) ;
            }
            if (!eligibleEmpVals.contains(mins)) {
                throw new RuleEngineException ("Token must be EMP_VAL1 to EMP_VAL20 : " + int_minutes_str);
            }
            mins = (String)wbData.getRuleData().getEmployeeData().getField(mins);
            if (StringHelper.isEmpty(mins)
                && !StringHelper.isEmpty(defMins) ) {
                mins = defMins;
            }
            if (logger.isDebugEnabled()) logger.debug("Resolved empval to :" + mins);
        }
        // *** check i.eREG=EMP_UDF~XX|2400
        else if (int_minutes_str.startsWith(EMP_UDF_PREFIX)) {
            int defMinsIndex = int_minutes_str.indexOf("|");
            String defMins = null;
            if (defMinsIndex >= 0) {
                mins = int_minutes_str.substring(0, defMinsIndex);
                defMins = int_minutes_str.substring(defMinsIndex + 1);
            }
            if (mins.endsWith(EMP_VAL_HOUR)) {
                isHour = true;
                mins = mins.substring(0, mins.indexOf(EMP_VAL_HOUR) ) ;
            }
            int empudfIndex = mins.indexOf("~");
            if (empudfIndex == -1) {
                throw new RuleEngineException ("EMP_UDF definition is not defined");
            }
            String empUdfName = mins.substring(empudfIndex + 1);

            mins = (String)wbData.getEmpUdfValue(empUdfName);
            if (StringHelper.isEmpty(mins)
                && !StringHelper.isEmpty(defMins) ) {
                mins = defMins;
            }
            if (logger.isDebugEnabled()) logger.debug("Resolved empudf to :" + mins);
        }

        if (!StringHelper.isEmpty(mins) ) {
            try {
                int_minutes = Integer.parseInt(mins);
                if (isHour) {
                    int_minutes *= 60;
                }
            }
            catch (NumberFormatException ex) {
                throw new RuleEngineException ("Couldn't parse :" + int_minutes_str + " for hourset description" );
            }
        }

        return int_minutes;
    }

    private WorkDetailList splitWorkDetailsByHourSetDurations(WBData wbData, Parameters parameters) throws Exception {
        Parameters hourSetParams = Parameters.parseParameters(parameters.getParameter(PARAM_HOURSET_DESCRIPTION, "REG=0,OT1=9999"));
        Enumeration enum = hourSetParams.getParameters();

        Date startTimeWithinShift = null;
        Date endTimeWithinShift = null;
        Date intervalStartTime = null;
        Date intervalEndTime = null;
        Date intervalStartDate = null;
        Date intervalEndDate = null;
        int additionalMinutesWorked = 0;
        Date skdZoneStartDate = wbData.getWrksWorkDate();
        Date skdZoneEndDate = wbData.getWrksWorkDate();
        Date startTime = null;
        Date endTime = null;
        Date originalStartTime = null;
        DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);
        int seedMinutes = 0;
        int workSummaryId = wbData.getRuleData().getWorkSummary().getWrksId();
        boolean timeCodesInclusive = true;
        String timeCodeNameList = null;
        String hourTypeNameList = null;
        int previousHtypeDuration = 0;
        long nextEndTime = 0;
        WorkDetailList wdList = null;

        wdList = wbData.getWorkDetailsOrPremiumsForDate(wbData.getWrksWorkDate(), true);
        if (wdList != null) {
            wdList = wdList.duplicate();

            try {
                String startTimeWithinShiftValue = parameters.getParameter(PARAM_STARTTIME_WITHIN_SHIFT);
                if (startTimeWithinShiftValue != null) {
                    startTimeWithinShift = dateFormat.parse(startTimeWithinShiftValue);
                }
            } catch (IllegalArgumentException e){
                logger.debug("Unable to get parameter. The parameter value used will be set to null.", e);
                startTimeWithinShift = null;
            } catch (ParseException e) {
                logger.debug("Unable to parse date. The paramater value used will be set to null", e);
                startTimeWithinShift = null;
            }

            try {
                String endTimeWithinShiftValue = parameters.getParameter(PARAM_ENDTIME_WITHIN_SHIFT);
                if (endTimeWithinShiftValue != null) {
                    endTimeWithinShift = dateFormat.parse(endTimeWithinShiftValue);
                }
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get parameter. The parameter value used will be null.", e);
                endTimeWithinShift = null;
            } catch (ParseException e) {
                logger.debug("Unable to parse date. The paramater value used willbe set to null.", e);
                endTimeWithinShift = null;
            }

            try {
                String intervalStartTimeValue = parameters.getParameter(PARAM_INTERVAL_STARTTIME);
                if (intervalStartTimeValue != null) {
                    intervalStartTime = dateFormat.parse(intervalStartTimeValue);
                }
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get parameter. The paramater value used will be null.", e);
                intervalStartTime = null;
            } catch (ParseException e) {
                logger.debug("Unable to parse date.  The parameter value used will be null.", e);
                intervalStartTime = null;
            }

            try {
                String intervalEndTimeValue = parameters.getParameter(PARAM_INTERVAL_ENDTIME);
                if (intervalEndTimeValue != null) {
                    intervalEndTime = dateFormat.parse(intervalEndTimeValue);
                }
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get parameter. The paramater value used will be null.", e);
                intervalEndTime = null;
            } catch (ParseException e) {
                logger.debug("Unable to parse date.  The paramete value used will be null", e);
                intervalEndTime = null;
            }

            try {
                String intervalStartDateValue = parameters.getParameter(PARAM_INTERVAL_STARTDATE);
                if (intervalStartDateValue != null) {
                    intervalStartDate = dateFormat.parse(intervalStartDateValue);
                }
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get parameter. The parameter value used will be null.", e);
                intervalStartDate = null;
            } catch (ParseException e) {
                logger.debug("Unable to parse date.  The parameter value used will be null.", e);
                intervalStartDate = null;
            }

            try {
                String intervalEndDateValue = parameters.getParameter(PARAM_INTERVAL_ENDDATE);
                if (intervalEndDateValue != null) {
                    intervalEndDate = dateFormat.parse(intervalEndDateValue);
                }
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get parameter.  The parameter valueused will be null", e);
                intervalEndDate = null;
            } catch (ParseException e) {
                logger.debug("Unable to parse date.  The paramater value used will be null", e);
                intervalEndDate = null;
            }

            try {
                additionalMinutesWorked = parameters.getIntegerParameter(PARAM_ADDITIONAL_MINUTES_WORKED, 0);
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get parameter.  The parameter value used will be 0");
            }

            try {
                String skdZoneStartDateValue = parameters.getParameter(PARAM_SKDZONE_STARTDATE);
                if (skdZoneStartDateValue != null) {
                    skdZoneStartDate = dateFormat.parse(skdZoneStartDateValue);
                }
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get parameter.  Using defaul value: " + skdZoneStartDate);
            }

            try {
                String skdZoneEndDateValue = parameters.getParameter(PARAM_SKDZONE_ENDDATE);
                if (skdZoneEndDateValue != null) {
                    skdZoneEndDate = dateFormat.parse(skdZoneEndDateValue);
                }
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get parameter. Using default value: " + skdZoneEndDate);
            }

            try {
                String timeCodesInclusiveValue = parameters.getParameter(PARAM_ARE_TIMECODES_INCLUSIVE);
                if ("false".equals(timeCodesInclusiveValue)) {
                    timeCodesInclusive = false;
                }
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get parameter.  Using defaul value: " + timeCodesInclusive);
            }

            try {
                timeCodeNameList = parameters.getParameter(PARAM_ELIGIBLE_TIMECODES);
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get parameter.  The parameter value used will be null.");
            }

            try {
                hourTypeNameList = parameters.getParameter(PARAM_ELIGIBLE_HOURTYPES, "REG");
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to get parameter.  The parameter value used will be null.");
            }

            if (startTimeWithinShift  == null) {
                startTime = new Date(wbData.getRuleData().getWorkDetail(0).getWrkdStartTime().getTime());
            } else {
                startTime = new Date(startTimeWithinShift.getTime());
            }

            if (endTimeWithinShift == null) {
                WorkDetailData lastWorkDetail = wbData.getRuleData().getWorkDetail(wbData.getRuleData().getWorkDetailCount() -1);
                endTime = new Date(lastWorkDetail.getWrkdEndTime().getTime());
            } else {
                endTime = new Date(endTimeWithinShift.getTime());
            }

            originalStartTime = new Date(startTime.getTime());
            seedMinutes = wbData.getWorkedMinutes(workSummaryId, intervalStartDate, intervalEndDate,
                            startTimeWithinShift, endTimeWithinShift, timeCodesInclusive, timeCodeNameList,hourTypeNameList);

            seedMinutes += additionalMinutesWorked;

            while (enum.hasMoreElements()) {
                String htypeName = ((Parm)enum.nextElement())._name;
                String htypeValue = hourSetParams.getParameter(htypeName);
                int htypeDuration = 0;
                boolean limitTime = false;


                if (htypeValue.endsWith("*")) {
                    limitTime = true;
                    htypeValue = htypeValue.substring(0, htypeValue.length() -1);
                }

                htypeDuration = evaluateHoursetDesc(wbData , htypeValue);

                if (htypeDuration == 0) {
                    if (intervalStartDate == null && intervalEndDate == null) {
                        htypeDuration = wbData.getScheduleDuration(true);
                    } else {
                        int empId = wbData.getRuleData().getEmployeeData().getEmpId();
                        Date schedStartTime = DateHelper.truncateToDays(skdZoneStartDate);
                        Date schedEndTime = DateHelper.truncateToDays(skdZoneEndDate);
                        htypeDuration = wbData.getScheduleDurationRange(empId, schedStartTime, schedEndTime);
                    }
                }

                previousHtypeDuration = htypeDuration;
                if (limitTime) {
                    if (htypeDuration - previousHtypeDuration >= 0) {
                        htypeDuration -= previousHtypeDuration;
                    }
                }

                if (seedMinutes < htypeDuration){
                    htypeDuration -= seedMinutes;
                    seedMinutes = 0;
                    nextEndTime = startTime.getTime() + htypeDuration * DateHelper.MINUTE_MILLISECODS;
                }
                else{
                    nextEndTime = startTime.getTime() + htypeDuration * DateHelper.MINUTE_MILLISECODS;
                    seedMinutes -= htypeDuration;
                    htypeDuration = 0;
                }

                endTime.setTime(nextEndTime);

                if (endTime.after(originalStartTime)) {
                    Date htypeStartTime = RuleHelper.max(startTime, originalStartTime);
                    Date htypeEndTime = RuleHelper.min(endTime, wbData.getMaxEndTime(null, false));

                    wdList.splitAt(htypeStartTime);
                    wdList.splitAt(htypeEndTime);

                    startTime.setTime(endTime.getTime());

                }
            }
        }
        return wdList;
    }

    /**
     * Extended getWorkedMinutes to work with Expression String
     */
    protected int getWorkedMinutes(
            WBData wbData,
            int wrksId,
            Date startDate,
            Date endDate,
            Date startTime,
            Date endTime,
            boolean inclusive,
            String tcodeNameList,
            String includeInSumHtypeList,
            String wdExpressionString
            ) throws Exception {
            if ((startTime == null && startDate == null) ||
                    (endTime == null && endDate == null)) {
                    return 0;
            }

            int minutes = 0;
            Date datStartTime = (startTime == null
                             ? new GregorianCalendar(1900, 0, 1).getTime()
                             : new Date(startTime.getTime()));

            Date datEndTime = (endTime == null
                             ? new GregorianCalendar(3000, 0, 1).getTime()
                             : new Date(endTime.getTime()));

            Date datStartDate = (startDate == null
                             ? new Date(startTime.getTime() - DateHelper.DAY_MILLISECODS)
                             : new Date(startDate.getTime()));

            Date datEndDate = (endDate == null
                             ? new Date(endTime.getTime() + 2 * DateHelper.DAY_MILLISECODS)
                             : new Date(endDate.getTime()));


            WorkDetailList details = wbData.getRuleData().getCalcDataCache().getWorkDetails(wbData.getEmpId() ,
                datStartDate,
                datEndDate,
                wbData.getDBconnection());
            if (details != null) {
                details.setCodeMapper(wbData.getRuleData().getCodeMapper());
            } else {
                return 0;
            }

            Iterator iter = details.iterator();
            while (iter.hasNext()) {
                WorkDetailData wd = (WorkDetailData)iter.next();
                boolean isEligible =
                    RuleHelper.isCodeInList(includeInSumHtypeList, wd.getWrkdHtypeName())
                    && RuleHelper.isCodeInList(tcodeNameList, wd.getWrkdTcodeName()) == inclusive
                    && evaluateExpression(wd , wdExpressionString )
                    && wd.getWrksId() != wrksId;
                if (!isEligible) continue;
                Date wrkdStartTime = wd.getWrkdStartTime();
                Date wrkdEndTime = wd.getWrkdEndTime();
                if (wrkdStartTime != null && wrkdEndTime != null) {
                    minutes +=
                        Math.max(0,
                                 Math.min(wrkdEndTime.getTime(),
                                          datEndTime.getTime()) -
                                 Math.max(wrkdStartTime.getTime(),
                                          datStartTime.getTime()))
                        / DateHelper.MINUTE_MILLISECODS;
                }

            }
            // **** Calculating time within Current Work Details
            if (startTime != null && endTime != null) {
              for (int i = 0, j = wbData.getRuleData().getWorkDetailCount(); i < j; i++) {
                String sHtypeName = wbData.getRuleData().getWorkDetail(i).getWrkdHtypeName();
                String sTcodeName = wbData.getRuleData().getWorkDetail(i).getWrkdTcodeName();
                if (RuleHelper.isCodeInList(includeInSumHtypeList, sHtypeName) &&
                    RuleHelper.isCodeInList(tcodeNameList, sTcodeName) == inclusive) {
                  minutes +=
                      RuleHelper.diffInMinutes(RuleHelper.max(wbData.getRuleData().
                      getWorkDetail(i).getWrkdStartTime(), startTime),
                                               RuleHelper.min(wbData.getRuleData().
                      getWorkDetail(i).getWrkdEndTime(), endTime));
                }
              }
            }
            return minutes;
    }

    /**
     * Extended pluck to work with Expression String
     */
    private void pluck(WBData wbData,
                       java.util.Date startTime,
                       java.util.Date endTime,
                       boolean tcodeInclusive,
                       String timeCodeList,
                       boolean htypeInclusive,
                       String hourTypeList,
                       String exprString) throws Exception {
        startTime = (startTime == null ? new java.util.Date(0) : startTime);
        endTime = (endTime == null ? new java.util.Date(Long.MAX_VALUE) : endTime);
        wbData.getRuleData().splitAt(startTime);
        wbData.getRuleData().splitAt(endTime);
        for (int i = wbData.getRuleData().getWorkDetailCount() - 1; i >= 0; i--) {
            WorkDetailData wrkDtl = wbData.getRuleData().getWorkDetail(i);
            if (wrkDtl.getWrkdEndTime().after(endTime)) {
                pluck(wbData , i);
            } else if (!(wrkDtl.getWrkdStartTime().before(startTime))) {
                if (tcodeInclusive != RuleHelper.isCodeInList(timeCodeList, wrkDtl.getWrkdTcodeName())) {
                    pluck(wbData , i);
                }
                else if (htypeInclusive != RuleHelper.isCodeInList(hourTypeList, wrkDtl.getWrkdHtypeName())) {
                    pluck(wbData , i);
                }
                else if (!evaluateExpression(wrkDtl , exprString) ) {
                    pluck(wbData , i);
                }
            }
        }
    }

    private void pluck(WBData wbData, int in_Index) throws Exception{
        if (wbData.getRuleData().getWorkDetail(in_Index) != null) {
            long shiftBy = wbData.getRuleData().getWorkDetail(in_Index).getWrkdEndTime().getTime() -
                    wbData.getRuleData().getWorkDetail(in_Index).getWrkdStartTime().getTime();
            java.lang.reflect.Field fldCache = wbData.getClass().getDeclaredField("_workDetailPlucked");
            fldCache.setAccessible(true);
            ArrayList _workDetailPlucked = (ArrayList) fldCache.get(wbData);

             _workDetailPlucked.add(wbData.getRuleData().getWorkDetail(in_Index));
            wbData.getRuleData().getWorkDetails().remove(in_Index);
            for (int i = in_Index, j = wbData.getRuleData().getWorkDetailCount(); i < j; i++) {
                wbData.getRuleData().getWorkDetail(i).setWrkdStartTime(new java.util.Date(
                        wbData.getRuleData().getWorkDetail(i).getWrkdStartTime().getTime() - shiftBy
                        ));
                wbData.getRuleData().getWorkDetail(i).setWrkdEndTime(new java.util.Date(
                        wbData.getRuleData().getWorkDetail(i).getWrkdEndTime().getTime() - shiftBy
                        ));
            }
        }
    }

    private boolean evaluateExpression(RecordData rd, String expressionString) {

        if (StringHelper.isEmpty(expressionString)) return true;
        boolean isAnd = expressionString.indexOf("&&") > -1;
        boolean ret = isAnd ? true : false;
        StringTokenizer st = new StringTokenizer(expressionString, "\"" + (isAnd ? "&&" : "||"));
        while( st.hasMoreTokens() ) {
            String s = st.nextToken().trim();
            int i = -1;
            String opr = null;
            // **** find the operator in the token
            for (int k=0; k < RuleHelper.operators.length ; k++) {
                i = s.indexOf(RuleHelper.operators[k]);
                if (i != -1) {
                    opr = RuleHelper.operators[k];
                    break;
                }
            }
            if( i > 0 ) {
                String name  = s.substring(0,i);
                String val = (i+opr.length()) == s.length() ? null : s.substring( i+opr.length());
                try {
                    Object obj = rd.getProperty(name);
                    if (isAnd) {
                        ret &= RuleHelper.evaluate(obj, val, opr);
                    }
                    else {
                        ret |= RuleHelper.evaluate(obj, val, opr);
                    }
                } catch(Exception e){
                    // **** if any exception, evaluate false
                    if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Evaluate" , e);}
                    return false;
                }
            // **** if no valid operator, throw exception
            } else {
                throw new RuntimeException ("No valid operator found in the expression , "
                                            + " valid operators are : " + Arrays.asList(RuleHelper.operators));
            }
        }
        return ret;
    }

}