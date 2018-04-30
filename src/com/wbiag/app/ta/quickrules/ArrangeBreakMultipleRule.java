package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import java.util.*;

/**
 * Title:       Arrange Break Rule
 * Description: For employees with unscheduled breaks but punch in/out for breaks
 *              allocates one break and  arrive early/late codes.
 *
 *              Updated on 8/12/2005
 *
 *              Break Up To Minutes is used only when Break Minutes is empty or has a zero value.  Unlike Break Assign Minutes which assign the designated
 *              minutes to each break, Break Up To Minutes indicates the total threshold minutes
 *              for all breaks, where the rule uses it as the threshold to convert the total
 *              UAT details minutes up to the threshold minutes.  When “S” is provided, the
 *              Break Up To Minutes will be calculated based on employee schedule break
 *              duration defined in shift break section.  No Arrive Early, arrive late minutes or
 *              time codes will be used in conjunction with Break Up To Minutes
 */

public class ArrangeBreakMultipleRule extends Rule{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ArrangeBreakMultipleRule.class);

    public final static String PARAM_MAX_NUMBER_OF_BREAKS = "MaxNumberOfBreaks";
    public final static String PARAM_APPLY_TO_ALL_SHIFTS = "ApplyToAllShifts";
    public final static String PARAM_BREAK_TIMECODE = "BreakTimeCode";
    public final static String PARAM_BREAK_MINUTES = "BreakMinutes";
    public final static String PARAM_BREAK_ARRIVEEARLY_TIMECODE = "BreakArriveEarlyTimeCode";
    public final static String PARAM_BREAK_ARRIVELATE_TIMECODE = "BreakArriveLateTimeCode";
    public final static String PARAM_BREAK_EXTRA_ARRIVEEARLY_TIMECODE = "BreakExtraArriveEarlyTimeCode";
    public final static String PARAM_BREAK_EXTRA_ARRIVEEARLY_MINUTES = "BreakExtraArriveEarlyMinutes";
    public final static String PARAM_BREAK_EXTRA_ARRIVELATE_TIMECODE = "BreakExtraArriveLateTimeCode";
    public final static String PARAM_BREAK_EXTRA_ARRIVELATE_MINUTES = "BreakExtraArriveLateMinutes";
    public final static String PARAM_VAL_BREAK_MINUTES_UP_TO = "*";
    public final static String PARAM_BREAK_TOTAL_MINUTES_UP_TO = "BreakTotalMinutesUpTo";




    public ArrangeBreakMultipleRule() {

    }

    public List getParameterInfo(DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_MAX_NUMBER_OF_BREAKS, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_BREAK_TIMECODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_BREAK_MINUTES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_BREAK_ARRIVEEARLY_TIMECODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BREAK_ARRIVELATE_TIMECODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BREAK_EXTRA_ARRIVEEARLY_TIMECODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BREAK_EXTRA_ARRIVEEARLY_MINUTES, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BREAK_EXTRA_ARRIVELATE_TIMECODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BREAK_EXTRA_ARRIVELATE_MINUTES, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_APPLY_TO_ALL_SHIFTS, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_BREAK_TOTAL_MINUTES_UP_TO, RuleParameterInfo.STRING_TYPE));
        return result;
    }


    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {

        if (logger.isDebugEnabled()) logger.debug("Executing Arrange Break rule");
        if (logger.isDebugEnabled()) logger.debug("Work Details before executing Arrange Break rule \n" + wbData.getRuleData().getWorkDetails().toDescription());


        ParametersResolved  pars = new ParametersResolved  ();
        pars.maxNoOfBreaks = parameters.getIntegerParameter(PARAM_MAX_NUMBER_OF_BREAKS , 1);
        pars.breakTimeCode = parameters.getParameter(PARAM_BREAK_TIMECODE);
        String sBreakMinutes =parameters.getParameter(PARAM_BREAK_MINUTES);
        boolean IssBreakMinsEmpty=StringHelper.isEmpty(sBreakMinutes);
        if ((!IssBreakMinsEmpty)&& (sBreakMinutes.startsWith(PARAM_VAL_BREAK_MINUTES_UP_TO)) ) {
            pars.breakMinutes = Integer.parseInt(sBreakMinutes.substring(sBreakMinutes.indexOf(PARAM_VAL_BREAK_MINUTES_UP_TO) + 1));
            pars.isBreakMinsUpTo = true;

        } else {
            if (!IssBreakMinsEmpty){
                pars.breakMinutes = parameters.getIntegerParameter(PARAM_BREAK_MINUTES);
            }
        }

        if ( pars.breakMinutes <= 0 ) {
            if (logger.isDebugEnabled()) {
            	logger.debug("PARAM_BREAK_MINUTES=0 Now use PARAM_BREAK_TOTAL_MINUTES_UP_TO logic");
            }
            pars.isBreakTotalMinsUpTo=true;
        }

        pars.breakArriveEarlyTimeCode = parameters.getParameter(PARAM_BREAK_ARRIVEEARLY_TIMECODE);
        pars.breakArriveLateTimeCode = parameters.getParameter(PARAM_BREAK_ARRIVELATE_TIMECODE);

        pars.breakExtraArriveEarlyTimeCode = parameters.getParameter(PARAM_BREAK_EXTRA_ARRIVEEARLY_TIMECODE);
        pars.breakExtraArriveEarlyMinutes = parameters.getIntegerParameter(PARAM_BREAK_EXTRA_ARRIVEEARLY_MINUTES , 0);
        pars.breakExtraArriveLateTimeCode = parameters.getParameter(PARAM_BREAK_EXTRA_ARRIVELATE_TIMECODE);
        pars.breakExtraArriveLateMinutes = parameters.getIntegerParameter(PARAM_BREAK_EXTRA_ARRIVELATE_MINUTES , 0);

        pars.applyToAllShifts = Boolean.valueOf(parameters.getParameter(PARAM_APPLY_TO_ALL_SHIFTS,"false")).booleanValue();
        pars.strBreakTotalMinutesUpTo=parameters.getParameter(PARAM_BREAK_TOTAL_MINUTES_UP_TO);

        List swb = wbData.getShiftsWithBreaks();

        if ( logger.isDebugEnabled() )logger.debug("No of shifts with Breaks="+swb.size());

        for (int k=0 , l=swb.size() ; k < l; k++) {

            if ( k > 0 && pars.applyToAllShifts == false ) {
                break;
            }
            if (wbData.getShiftWithBreaks(k).isScheduledActual()) {
                applyToSchedule(wbData, pars, k);
            }
        }
        if (logger.isDebugEnabled()) logger.debug("Work Details after executing Arrange Break rule \n" + wbData.getRuleData().getWorkDetails().toDescription());
    }

    protected void applyToSchedule(WBData wbData, ParametersResolved pars,
                                   int shiftIndex) throws Exception {

        Date scheduledStart = wbData.getShiftWithBreaks(shiftIndex).getShftStartTime();
        Date scheduledEnd = wbData.getShiftWithBreaks(shiftIndex).getShftEndTime();

        if (!pars.isBreakTotalMinsUpTo){
            if (!StringHelper.isEmpty(pars.breakExtraArriveEarlyTimeCode)) {
                if (pars.breakExtraArriveEarlyMinutes < 0) {
                    int skdMins = wbData.getShiftWithBreaks(shiftIndex).
                    retrieveScheduledMinutesPaid(wbData.getCodeMapper());
                    pars.breakExtraArriveEarlyMinutes = (int) (skdMins *
                            ( -1.0 * pars.breakExtraArriveEarlyMinutes / 100));
                    if (logger.isDebugEnabled()) logger.debug("calculated breakExtraArriveEarlyMinutes =" + pars.breakExtraArriveEarlyMinutes);
                }
            }

            if (!StringHelper.isEmpty(pars.breakExtraArriveLateTimeCode)) {
                // <0 means extraArriveLateMinutes needs to be calculated as a percentage of the the scheduled duration
                if ( pars.breakExtraArriveLateMinutes < 0 ) {
                    int skdMins = wbData.getShiftWithBreaks(shiftIndex).
                    retrieveScheduledMinutesPaid(wbData.getCodeMapper());
                    pars.breakExtraArriveLateMinutes = (int) (skdMins *
                            (-1.0 * pars.breakExtraArriveLateMinutes / 100));

                    if ( logger.isDebugEnabled()) logger.debug("calculated breakExtraArriveLateMinutes =" + pars.breakExtraArriveLateMinutes);
                }
            }

            int brkCnt = 1;
            int firstUatIndex = getFirstWorkDetailUat(wbData , scheduledStart , scheduledEnd);

            while ( firstUatIndex != -1 && brkCnt <= pars.maxNoOfBreaks ) {

                int firstUatMins = wbData.getWrkdMinutes(firstUatIndex);
                if (logger.isDebugEnabled()) logger.debug("calculated UatMins =" + firstUatMins + " for shift :" + shiftIndex + " and break:" + brkCnt);
                java.util.Date startTime = wbData.getWrkdStartTime(firstUatIndex);

                if ( firstUatMins == pars.breakMinutes ) {

                    if ( logger.isDebugEnabled() ) logger.debug("arrived on time");

                    wbData.setWorkDetailTcodeName( pars.breakTimeCode,
                            startTime,
                            DateHelper.addMinutes(startTime , firstUatMins),
                            null, false, true);
                    //     arrive early

                } else if ( firstUatMins < pars.breakMinutes ) {
                    if (logger.isDebugEnabled()) logger.debug("arrived early");

                    wbData.setWorkDetailTcodeName( pars.breakTimeCode,
                            startTime,
                            DateHelper.addMinutes(startTime , firstUatMins),
                            null, false, true);

                    if ( !StringHelper.isEmpty(pars.breakExtraArriveEarlyTimeCode )
                            &&  (pars.breakMinutes - firstUatMins >= pars.breakExtraArriveEarlyMinutes)) {
                        pars.breakArriveEarlyTimeCode = pars.breakExtraArriveEarlyTimeCode;

                    }

                    if ( !StringHelper.isEmpty(pars.breakArriveEarlyTimeCode )) {

                        wbData.setWorkDetailTcodeName( pars.breakArriveEarlyTimeCode,
                                DateHelper.addMinutes(startTime , firstUatMins),
                                DateHelper.addMinutes(startTime , pars.breakMinutes),
                                null, false, true);
                    }
                    //  arrive late

                } else if ( firstUatMins > pars.breakMinutes ) {

                    if (!pars.isBreakMinsUpTo){

                        if (logger.isDebugEnabled())logger.debug("arrived late");

                        wbData.setWorkDetailTcodeName(pars.breakTimeCode,
                            startTime,DateHelper.addMinutes(startTime, pars.breakMinutes),
                                null, false, true);
                        if (!StringHelper.isEmpty(pars.breakExtraArriveLateTimeCode)
                                && ( firstUatMins - pars.breakMinutes >= pars.breakExtraArriveLateMinutes)){
                            pars.breakArriveLateTimeCode = pars.breakExtraArriveLateTimeCode;
                        }

                        if (!StringHelper.isEmpty(pars.breakArriveLateTimeCode)) {
                            wbData.setWorkDetailTcodeName(pars.breakArriveLateTimeCode,
                                    DateHelper.addMinutes(startTime, pars.breakMinutes),
                                    DateHelper.addMinutes(startTime, firstUatMins),
                                    null, false, true);
                        }
                    } else {
                        if (logger.isDebugEnabled()) logger.debug("Uat minutes : " + firstUatMins + " is bigger than upto minutes : " + pars.breakMinutes + ". No break code assigned");
                    }
                }
                brkCnt++;
                firstUatIndex = getFirstWorkDetailUat(wbData , scheduledStart , scheduledEnd);
            }
            resetOverriddenFlag(wbData );
        }
        else {

            int employeeBreakMinutes=0;
            int breakTotalMinutesUpTo = 0;
            int brkCnt=1;


            if (!StringHelper.isEmpty(pars.strBreakTotalMinutesUpTo)
                && pars.strBreakTotalMinutesUpTo.toUpperCase().startsWith("S")) {
                if ( logger.isDebugEnabled() ) logger.debug("PARAM_BREAK_MINUTES_UP_TO starts with 'S'");

	            // If no shift breaks are defined, then do nothing.
	            ShiftWithBreaks swb = wbData.getShiftWithBreaks(shiftIndex);
	            if (swb.getShiftBreaks() == null || swb.getShiftBreaks().size() == 0) {
	            	return;
	            }
                // Get the duration from the first shift break.
                breakTotalMinutesUpTo = swb.getShiftBreak(0).getShftbrkMinutes();

            } else {
                breakTotalMinutesUpTo = Integer.parseInt(pars.strBreakTotalMinutesUpTo);
            }

            if(logger.isDebugEnabled()) logger.debug("breakMinutesUpTo=" + breakTotalMinutesUpTo);

            int firstUatIndex = getFirstWorkDetailUat(wbData , scheduledStart,scheduledEnd);
            java.util.Date startTime =null;

            while( firstUatIndex!=-1 && brkCnt<= pars.maxNoOfBreaks ){

                startTime = wbData.getWrkdStartTime(firstUatIndex);

                int firstUatMins=wbData.getWrkdMinutes(firstUatIndex);

                if ( employeeBreakMinutes >= breakTotalMinutesUpTo ) {
                    break;
                }
                else if (( employeeBreakMinutes+firstUatMins ) > breakTotalMinutesUpTo ) {
                    wbData.setWorkDetailTcodeName( pars.breakTimeCode,
                            startTime,
                            DateHelper.addMinutes(startTime, breakTotalMinutesUpTo - employeeBreakMinutes),
                            null, false, true);
                    if (breakTotalMinutesUpTo < firstUatMins) employeeBreakMinutes += firstUatMins;

                    break;  // no need for further processing
                }
                else {
                    wbData.setWorkDetailTcodeName( pars.breakTimeCode,
                            startTime,
                            DateHelper.addMinutes(startTime , firstUatMins),
                            null, false, true);
                    employeeBreakMinutes += firstUatMins;
                }
                brkCnt++;
                firstUatIndex = getFirstWorkDetailUat(wbData , scheduledStart , scheduledEnd);
            }
        }
        resetOverriddenFlag(wbData );
    }

    /**
     * Finds the first uat work detail index after first record.
     * Updates wrkdOverriden to ignore same UAT in further passes
     */

    protected int getFirstWorkDetailUat(WBData wbData,
                                        Date startTime, Date endTime) {

        int ret = -1;
        WorkDetailList wdl = wbData.getRuleData().getWorkDetails();

        wdl.splitAt(startTime);
        wdl.splitAt(endTime);

        for (int i = 0, k=wdl.size()  ; i < k; i++) {
            //logger.debug("Time Code="+wdl.getWorkDetail(i).getWrkdTcodeName());
            //logger.debug("Worked Minutes="+wdl.getWorkDetail(i).getWrkdMinutes());
            boolean isBetween =
                wdl.getWorkDetail(i).getWrkdStartTime().compareTo(startTime) >= 0
                && wdl.getWorkDetail(i).getWrkdEndTime().compareTo(endTime) <= 0;

                if ( isBetween&& WBData.TCODE_UAT.equals(wdl.getWorkDetail(i).getWrkdTcodeName())
                        && wdl.getWorkDetail(i).getWrkdMinutes() > 0
                        && wdl.getWorkDetail(i).getWrkdStartTime().compareTo(startTime) != 0
                        && wdl.getWorkDetail(i).getWrkdEndTime().compareTo(endTime) != 0

                        && !"Y".equals(wdl.getWorkDetail(i).getWrkdOverridden())) {
                    wdl.getWorkDetail(i).setWrkdOverridden("Y");
                    ret = i;
                    break;
                }
        }

        return ret;
    }


    /*
     * Reset any of the WrkdOverridden flags.
     */
    private void resetOverriddenFlag(WBData wbData) {

        WorkDetailList wdl = wbData.getRuleData().getWorkDetails();
        for (int i = 0, k=wdl.size()  ; i < k; i++) {
            if ("Y".equals(wdl.getWorkDetail(i).getWrkdOverridden())) {
                wdl.getWorkDetail(i).setWrkdOverridden(null);
            }
        }
    }

    /**
     * Unique name that is to be used in editors.
     */

    public String getComponentName() {

        return "WBIAG: Arrange Break Multiple Rule";

    }

    public class ParametersResolved {
        // Rule Parameters.

        public int  maxNoOfBreaks = 0;
        public String breakTimeCode;
        public int  breakMinutes;
        public String breakArriveEarlyTimeCode;
        public String breakArriveLateTimeCode;
        public String breakExtraArriveEarlyTimeCode;
        public int  breakExtraArriveEarlyMinutes;
        public String breakExtraArriveLateTimeCode;
        public int  breakExtraArriveLateMinutes;
        public boolean applyToAllShifts;
        public String strBreakTotalMinutesUpTo;

        public boolean isBreakMinsUpTo = false;
        public boolean isBreakTotalMinsUpTo = false;
    }

}
