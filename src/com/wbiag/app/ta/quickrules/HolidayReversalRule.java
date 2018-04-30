package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

import java.util.*;
import java.sql.SQLException;

/**
 * @author bviveiros
 *
 * Business Scenario:
 * An employee gets paid for a holiday if they work their next
 * scheduled shift before and after the holiday.  If there is no scheduled shift
 * between the holiday and the end of the pay period, the employee will get receive
 * the holiday pay.  Then whenever their next scheduled shift occurs (x number of
 * pay periods later), if the employee does not work the shift then create a premium
 * to deduct the holiday pay.
 *
 * This rule will determine if this is the next scheduled shift after a holiday.  If it is,
 * and the shift was not worked, then create a premium to negate the holiday pay.
 *
 * The rule assumes that the pay period has always the same length.
 * Unexpected results may occur if the pay period duration is changed!!
 *
 * EDIT: TT1171 Consecutive Holidays Reversal
 * date: Sept 29, 2006
 * author: Jonathan Chan
 * 
 * TT1171 specifies that the rule should check for consecutive holidays.  (i.e. X-mas
 * and boxing day).  If the employee does not work on the next scheduled shift after the
 * holidays a timecode will be inserted that dedeucts the pay for both days.  
 * 
 * This functionality can be turned on or off by the CheckForConsecHoliday parameter. 
 * By default, this functionality is turned OFF.  The parameter must be set to TRUE for it to be on.
 * If this functionality is turned on then another timecode should be created which deducts
 * the pay for consecutive holiday.  A second time code should be used so that the premium
 * doesn't look like a 'double deduction' error.
 * This timecode is then set under the parameter ConsecReversalPremiumTimeCode
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class HolidayReversalRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HolidayReversalRule.class);

    public final static String PARAM_HOLIDAY_TIMECODES = "HolidayPremiumTimeCodes";
    public final static String PARAM_HOLIDAY_REVERSAL_TIMECODE = "ReversalPremiumTimeCode";
    //************** TT1171: parameter definition **************************************
    public final static String PARAM_CHECK_FOR_CONSEC_HOLIDAY = "CheckForConsecHoliday";
    public final static String PARAM_CONSEC_HOLIDAY_REVERSAL_TIMECODE = "ConsecReversalPremiumTimeCode";
    //************************************************************************************
    public final static String PARAM_ELIGIBLE_WORK_TIMECODES = "EligibleWorkTimeCodes";
    public final static String PARAM_VIOLATION_TIMECODES = "ViolationTimeCodes";
    public final static String PARAM_MAX_PAY_PERIODS = "MaxPayPeriods";
    public final static String PARAM_REVERSE_IF_NOT_SCHEDULED = "ReverseIfNotScheduled";
    public final static String PARAM_INSERT_NEGATIVE_MINUTES = "InsertNegativeMinutes";



    public String getComponentName() {
        return "WBIAG: Holiday Reversal Rule";
    }

    public List getParameterInfo(DBConnection conn) {

        List result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_HOLIDAY_TIMECODES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_HOLIDAY_REVERSAL_TIMECODE, RuleParameterInfo.STRING_TYPE, true));
        //*************TT1171: add params *******************************************************
        result.add(new RuleParameterInfo(PARAM_CHECK_FOR_CONSEC_HOLIDAY, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_CONSEC_HOLIDAY_REVERSAL_TIMECODE, RuleParameterInfo.STRING_TYPE, true));
        //***************************************************************************************
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_WORK_TIMECODES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_VIOLATION_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_MAX_PAY_PERIODS, RuleParameterInfo.INT_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_REVERSE_IF_NOT_SCHEDULED, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_INSERT_NEGATIVE_MINUTES, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {

        ParametersResolved pars = new  ParametersResolved ();
        // Get parameter values
        pars.holidayTimeCodes = parameters.getParameter(PARAM_HOLIDAY_TIMECODES, null);
        pars.holidayReversalTimeCode = parameters.getParameter(PARAM_HOLIDAY_REVERSAL_TIMECODE, null);
        //**************TT1171: set parameters ******************************************
        pars.checkConsecHolidays = Boolean.valueOf(parameters.getParameter(PARAM_CHECK_FOR_CONSEC_HOLIDAY, "false")).booleanValue();
        pars.consecHolidayReversalTimeCode = parameters.getParameter(PARAM_CONSEC_HOLIDAY_REVERSAL_TIMECODE, null);
        //*******************************************************************************
        pars.eligibleWorkTimeCodes = parameters.getParameter(PARAM_ELIGIBLE_WORK_TIMECODES, null);
        pars.violationTimeCodes = parameters.getParameter(PARAM_VIOLATION_TIMECODES, null);
        pars.maxPayPeriods = parameters.getIntegerParameter(PARAM_MAX_PAY_PERIODS, 2);
        pars.reverseIfNotScheduled = Boolean.valueOf(parameters.getParameter(PARAM_REVERSE_IF_NOT_SCHEDULED, "false")).booleanValue();
        pars.insertNegativeMinutes = Boolean.valueOf(parameters.getParameter(PARAM_INSERT_NEGATIVE_MINUTES, "false")).booleanValue();

        // maxPayPeriods must be at least 0.
        // Zero means only look in the current pay period.
        if (pars.maxPayPeriods < 0) {
            throw new IllegalArgumentException("maxPayPeriods must be greater or equal than 0 : " + pars.maxPayPeriods);
        }

        // If there is work today, then a reversal is not necessary.
        if (wbData.getMinutesWorkDetail(pars.eligibleWorkTimeCodes, true, null, true) > 0
            && (pars.violationTimeCodes == null
                    || (pars.violationTimeCodes != null
                        && wbData.getMinutesWorkDetail(pars.violationTimeCodes, true, null, true) == 0))
                ) {
            return;
        }

        Date wrksDate = wbData.getWrksWorkDate();

        // Get the date of the previous holiday.
        Date previousHolidayDate = getPreviousHolidayDate(wbData);

        // If no holidays defined for the employee then return.
        if (previousHolidayDate == null) {
            return;
        }

        // Based on the date of the previous holiday, calculate the last date
        // to check.  It is based on number of pay periods we are checking.
        Date maxCheckDate = getMaxCheckDate(wbData, previousHolidayDate, pars);

        // No need to look past the wrksWorkDate.  Those dates will get processed
        // when the date gets calculated.
        Date endCheckDate = DateHelper.min(maxCheckDate, wrksDate);

        if (logger.isDebugEnabled()) {
            logger.debug("previousHolidayDate: " + previousHolidayDate);
            logger.debug("maxCheckDate: " + maxCheckDate);
            logger.debug("endCheckDate: " + endCheckDate);
        }

        // If today is past the max pay periods after the previous
        // holiday, then a reversal is not necessary.
        // The maxCheckDate has already been processed by an earlier work summary date.
        if (wbData.getWrksWorkDate().after(maxCheckDate)) {
            return;
        }

        // Check the schedule for each date since the holiday, up to the max date.
        // Quit once a scheduled day has been found, or we've reached the max date.
        EmployeeScheduleData empSched = null;
        Date checkDate = DateHelper.addDays(previousHolidayDate, 1);

        while (DateHelper.dateDifferenceInDays(checkDate, endCheckDate) <= 0) {

            // Get the schedule
            if (checkDate.equals(wrksDate)) {
                empSched = wbData.getEmployeeScheduleData();
            } else {
                empSched = wbData.getEmployeeScheduleData(checkDate);
            }

            // If the employee was scheduled.
            if (empSched.isEmployeeScheduledActual()) {

                // If the scheduled date is previous to the wrks date
                // then it has already been processed.
                if (checkDate.before(wrksDate)) {
                    return;

                // If the scheduled date is the wrksDate, then it is
                // the first since the holiday.
                } else if (checkDate.equals(wrksDate)) {

                    logger.debug("wrksWorkDate is first scheduled date since holiday.  Inserting Premium");

                    // We already know that the employee didn't work.
                    // This was checked at the begin of the function.
                    reverseHoliday(wbData, previousHolidayDate, pars);

                    return;
                }

            }

            checkDate = DateHelper.addDays(checkDate, 1);
        }

        // At this point we have not found a scheduled day in the range.
        // Insert the reversal if reverseIfNotScheduled was set to true.
        if (endCheckDate.equals(maxCheckDate) && pars.reverseIfNotScheduled) {
            logger.debug("maxCheckDate reached and no schedule found.  Inserting Premium.");

            reverseHoliday(wbData, previousHolidayDate, pars);
        }
    }


    /**
     * Get the end of the pay period n periods after the given date.
     *
     * @param wbData
     * @param previousHolidayDate
     * @return
     */
    protected Date getMaxCheckDate(WBData wbData, Date previousHolidayDate,
                                   ParametersResolved pars) {

        Date maxCheckDate = previousHolidayDate;
        PayGroupData pgd = wbData.getRuleData().getCodeMapper().getPayGroupById(wbData.getPaygrpId());

        // Begin at the holiday date, find the last day of the
        // pay period n pay periods later.
        for (int i = 0; i <= pars.maxPayPeriods; i++) {
            maxCheckDate = DateHelper.getUnitPayPeriod(
                                        DateHelper.APPLY_ON_LAST_DAY,
                                        false,
                                        DateHelper.addDays(maxCheckDate, 1),
                                        pgd);
        }

        return maxCheckDate;
    }


    /**
     * Return the date of the holiday previous to the wrksWorkDate.
     *
     * @param wbData
     * @param startDate
     * @param endDate
     * @return
     * @throws SQLException
     */
    protected Date getPreviousHolidayDate(WBData wbData) throws SQLException {

        // Look back 1 year from the current date.
        Date startDate = DateHelper.addDays(wbData.getWrksWorkDate(), -365);
        Date endDate = wbData.getWrksWorkDate();
        Date wrksDate = wbData.getWrksWorkDate();

        List holidays = wbData.getHolidaysRange(startDate, endDate, null);
        Date holidayDate = DateHelper.DATE_1900;
        HolidayData holiday = null;

        if (holidays != null && holidays.size() > 0) {

            // Find the holiday closest to the wrksDate.
            Iterator i = holidays.iterator();
            while (i.hasNext()) {

                holiday = (HolidayData) i.next();

                // If this holiday is closer to the wrksWorkDate.
                if (wrksDate.getTime() - holiday.getHolDate().getTime()
                        < wrksDate.getTime() - holidayDate.getTime()) {

                    holidayDate = holiday.getHolDate();
                }
            }
        }

        // Return null if a holiday was not found.
        if (holidayDate.equals(DateHelper.DATE_1900)) {
            holidayDate = null;
        }

        return holidayDate;
    }


    /**
     * Finds a holiday premium on the given work date, then inserts a premium
     * equal to the holiday premium, but the duration is negative.
     *
     * @param wbData
     * @param workDate
     * @throws SQLException
     */
    protected void reverseHoliday(WBData wbData, Date holidayDate,
                                  ParametersResolved pars) throws SQLException {

        // Find the holdiay Premium.
        WorkDetailData holidayPremium = findHolidayPremium(wbData, holidayDate, pars);

        // Insert a premium to reverse the holiday premium.
        if (holidayPremium != null) {
            if (!pars.insertNegativeMinutes)insertReverseHolidayPremium(wbData,
                    holidayPremium, pars.holidayReversalTimeCode);
            else insertNegativeMinutesReverseHolidayPremium(wbData,
                    holidayPremium, pars.holidayReversalTimeCode);
        }
        
        //********** TT1171 **********************************
        //If checkConsecHolidays is true, check for a holiday the day before the holiday
        if (pars.checkConsecHolidays) {
        	Date prevDate = DateHelper.addDays(holidayDate, -1);
        	holidayPremium = findHolidayPremium(wbData, prevDate, pars);
        	
            // Insert a premium to reverse the holiday premium.
            if (holidayPremium != null) {
                if (!pars.insertNegativeMinutes)insertReverseHolidayPremium(wbData,
                        holidayPremium, pars.consecHolidayReversalTimeCode);
                else insertNegativeMinutesReverseHolidayPremium(wbData,
                        holidayPremium, pars.consecHolidayReversalTimeCode);
            }
        }
        //*******************************************************************
    }


    /**
     * Returns the holiday premium on the given workDate. Returns null if not found.
     *
     * @param wbData
     * @param workDate
     * @return
     * @throws SQLException
     *
     */
    protected WorkDetailData findHolidayPremium(WBData wbData,
                                                Date workDate,
                                                ParametersResolved pars) throws SQLException {

        WorkDetailList wdl = wbData.getWorkPremiumsForDate(workDate);

        // get current holiday work detail
        Iterator it = wdl.iterator();
        WorkDetailData hwdd = null;
        WorkDetailData wdd = null;

        // Find the holdiay Premium.
        while (it.hasNext() && hwdd == null) {
            wdd = (WorkDetailData) it.next();
            if (RuleHelper.isCodeInList(pars.holidayTimeCodes, wdd.getWrkdTcodeName())) {
                hwdd = wdd;
            }
        }

        return hwdd;
    }

    /**
     * Creates a premium equal to the holiday premium, but the rate is negative.
     * The reverse premium is inserted on the current work summary date.
     *
     * @param wbData
     * @param holidayPremium
     */
   
    /****** OLD CODE
     * TT1171 - Change insertReverseHolidayPremium to use string instead of pars
     
   
    protected void insertReverseHolidayPremium(WBData wbData, WorkDetailData holidayPremium, ParametersResolved pars) {
  
        // Create the holiday reversal work detail
        WorkDetailData reversePremium = holidayPremium.duplicate();

        // Set tCode, hType, and negate the rate.
        if (pars.holidayReversalTimeCode != null) {
            reversePremium.setWrkdTcodeName(pars.holidayReversalTimeCode);
        }

        reversePremium.setWrkdRate(holidayPremium.getWrkdRate() * -1);

        reversePremium.setWrkdWorkDate(wbData.getWrksWorkDate());
        reversePremium.setWrksId(wbData.getRuleData().getWorkSummary().getWrksId());
        reversePremium.setWrksWorkDate(wbData.getWrksWorkDate());

        // Add the holiday reversal premium to the current work summary date.
        wbData.insertWorkPremiumRecord(reversePremium);
    }
     */
    
  
    protected void insertReverseHolidayPremium(WBData wbData,
                                                            WorkDetailData holidayPremium,
                                                            String reversalTimeCode) {
        // Create the holiday reversal work detail
        WorkDetailData reversePremium = holidayPremium.duplicate();

        // Set tCode, hType, and negate the rate.
        if (reversalTimeCode != null) {
        	reversePremium.setWrkdTcodeName(reversalTimeCode);
        }

        reversePremium.setWrkdRate(holidayPremium.getWrkdRate() * -1);

        reversePremium.setWrkdWorkDate(wbData.getWrksWorkDate());
        reversePremium.setWrksId(wbData.getRuleData().getWorkSummary().getWrksId());
        reversePremium.setWrksWorkDate(wbData.getWrksWorkDate());

        // Add the holiday reversal premium to the current work summary date.
        //wbData.insertWorkPremiumRecord(reversePremium);
        //edit JC
        wbData.getRuleData().getWorkPremiums().add( reversePremium );
    }

     /**
     * Creates a premium equal to the holiday premium, but the duration is negative.
     * The reverse premium is inserted on the current work summary date.
     * Added on 9/20/2005 to cater to InsertNegativeMinutes
     * @param wbData
     * @param holidayPremium
     * @author snimbkar
     */
    /******* OLD CODE 
     * TT1171 - Edited to use string instead of pars
     * 
     * protected void insertNegativeMinutesReverseHolidayPremium(WBData wbData,
        WorkDetailData holidayPremium,
        ParametersResolved pars)
    {
        WorkDetailData reversePremium = holidayPremium.duplicate();
        if(pars.holidayReversalTimeCode != null)
            reversePremium.setWrkdTcodeName(pars.holidayReversalTimeCode);
        reversePremium.setWrkdMinutes(holidayPremium.getWrkdMinutes() * -1);
        reversePremium.setWrkdRate(holidayPremium.getWrkdRate() * 1D);
        reversePremium.setWrkdWorkDate(wbData.getWrksWorkDate());
        reversePremium.setWrksId(wbData.getRuleData().getWorkSummary().getWrksId());
        reversePremium.setWrksWorkDate(wbData.getWrksWorkDate());
        wbData.getRuleData().getWorkPremiums().add( reversePremium );

    }
    */
    
    protected void insertNegativeMinutesReverseHolidayPremium(WBData wbData,
            WorkDetailData holidayPremium,
            String reversalTimeCode)
        {
            WorkDetailData reversePremium = holidayPremium.duplicate();
            if(reversalTimeCode != null)
                reversePremium.setWrkdTcodeName(reversalTimeCode);
            reversePremium.setWrkdMinutes(holidayPremium.getWrkdMinutes() * -1);
            reversePremium.setWrkdRate(holidayPremium.getWrkdRate() * 1D);
            reversePremium.setWrkdWorkDate(wbData.getWrksWorkDate());
            reversePremium.setWrksId(wbData.getRuleData().getWorkSummary().getWrksId());
            reversePremium.setWrksWorkDate(wbData.getWrksWorkDate());
            wbData.getRuleData().getWorkPremiums().add( reversePremium );

        }
    public class ParametersResolved {
        // Rule parameters
        public String holidayTimeCodes;
        public String holidayReversalTimeCode;
        //***** TT1171**********************
        public boolean checkConsecHolidays;
        public String consecHolidayReversalTimeCode;
        //**********************************
        public String eligibleWorkTimeCodes;
        public String violationTimeCodes;
        public int maxPayPeriods;
        public boolean reverseIfNotScheduled = false;
        public boolean insertNegativeMinutes = false;

    }
}
