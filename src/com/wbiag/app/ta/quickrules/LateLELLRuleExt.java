package com.wbiag.app.ta.quickrules;
import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 *  LateLELL Extended Rule to do extra logic for In Early
 */
public class LateLELLRuleExt extends Rule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LateLELLRuleExt.class);

    public final static String PARAM_UAT_CODES = "UATCodes";
    public final static String PARAM_WRK_CODES = "WRKCodes";
    public final static String PARAM_IN_EARLY_CODE = "InEarlyCode";
    public final static String PARAM_IN_LATE_CODE = "InLateCode";
    public final static String PARAM_OUT_EARLY_CODE = "OutEarlyCode";
    public final static String PARAM_OUT_LATE_CODE = "OutLateCode";
    public final static String PARAM_IN_EXTRA_LATE_CODE = "InExtraLateCode";
    public final static String PARAM_IN_EXTRA_LATE_MINUTES = "InExtraLateMinutes";
    public final static String PARAM_OUT_EXTRA_EARLY_CODE = "OutExtraEarlyCode";
    public final static String PARAM_OUT_EXTRA_EARLY_MINUTES = "OutExtraEarlyMinutes";
    public final static String PARAM_APPLY_TO_ALL_SHIFTS = "ApplyToAllShifts";
    public final static String PARAM_IN_EARLY_PARSING = "InEarlyParsing";
    public final static String PARAM_IE_NUM_MINS_LIMIT = "IENumMinutesLimit";
    public final static String PARAM_IE_INCLUDE_NUM_MINS_LIMIT_RECORD = "IEIncludeNumMinutesLimitRecord";
    public final static String PARAM_IE_STOP_TCODES = "IEStopTCodes";
    public final static String PARAM_IE_STOP_CLOCKIN = "IEStopClockIn";
    public final static String PARAM_OUT_LATE_PARSING = "OutLateParsing";
    public final static String PARAM_OL_NUM_MINS_LIMIT = "OLNumMinutesLimit";
    public final static String PARAM_OL_INCLUDE_NUM_MINS_LIMIT_RECORD = "OLIncludeNumMinutesLimitRecord";
    public final static String PARAM_OL_STOP_TCODES = "OLStopTCodes";
    public final static String PARAM_OL_STOP_CLOCKOUT = "OLStopClockOut";

    public LateLELLRuleExt() {
    }

    public List getParameterInfo(DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_UAT_CODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_WRK_CODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_IN_EARLY_CODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_IN_LATE_CODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_OUT_EARLY_CODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_OUT_LATE_CODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_IN_EXTRA_LATE_CODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_IN_EXTRA_LATE_MINUTES, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_OUT_EXTRA_EARLY_CODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_OUT_EXTRA_EARLY_MINUTES, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_APPLY_TO_ALL_SHIFTS, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_IN_EARLY_PARSING, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_IE_NUM_MINS_LIMIT, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_IE_INCLUDE_NUM_MINS_LIMIT_RECORD, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_IE_STOP_TCODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_IE_STOP_CLOCKIN, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_OUT_LATE_PARSING, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_OL_NUM_MINS_LIMIT, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_OL_INCLUDE_NUM_MINS_LIMIT_RECORD, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_OL_STOP_TCODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_OL_STOP_CLOCKOUT, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }

    public String getComponentName() {
        return "WBIAG: LateLELL Rule Ext";
    }

    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {
        // *** Retrieving the parameters ***
        String uatCodes = parameters.getParameter(PARAM_UAT_CODES, "UAT");
        String wrkCodes = parameters.getParameter(PARAM_WRK_CODES, "WRK");
        String inEarlyCode = parameters.getParameter(PARAM_IN_EARLY_CODE);
        String inLateCode = parameters.getParameter(PARAM_IN_LATE_CODE);
        String outEarlyCode = parameters.getParameter(PARAM_OUT_EARLY_CODE);
        String outLateCode = parameters.getParameter(PARAM_OUT_LATE_CODE);
        String inExtraLateCode = parameters.getParameter(PARAM_IN_EXTRA_LATE_CODE);
        int inExtraLateMinutes = parameters.getIntegerParameter(PARAM_IN_EXTRA_LATE_MINUTES, Integer.MAX_VALUE);
        String outExtraEarlyCode = parameters.getParameter(PARAM_OUT_EXTRA_EARLY_CODE);
        int outExtraEarlyMinutes = parameters.getIntegerParameter(PARAM_OUT_EXTRA_EARLY_MINUTES, Integer.MAX_VALUE);
        boolean applyToAllShifts = Boolean.valueOf(parameters.getParameter(PARAM_APPLY_TO_ALL_SHIFTS,"false")).booleanValue();

        boolean isArrEarlyParsing = Boolean.valueOf(parameters.getParameter(PARAM_IN_EARLY_PARSING,"false")).booleanValue();
        int IEnumMinsLimit = parameters.getIntegerParameter(PARAM_IE_NUM_MINS_LIMIT, Integer.MAX_VALUE);
        boolean IEincludeNumMinsLimitRecord = Boolean.valueOf(parameters.getParameter(PARAM_IE_INCLUDE_NUM_MINS_LIMIT_RECORD,"false")).booleanValue();
        String IEstopTcodes = parameters.getParameter(PARAM_IE_STOP_TCODES);
        boolean IEstopClockIn = Boolean.valueOf(parameters.getParameter(PARAM_IE_STOP_CLOCKIN,"false")).booleanValue();

        boolean isOutLateParsing = Boolean.valueOf(parameters.getParameter(PARAM_OUT_LATE_PARSING,"false")).booleanValue();
        int OLnumMinsLimit = parameters.getIntegerParameter(PARAM_OL_NUM_MINS_LIMIT, Integer.MAX_VALUE);
        boolean OLincludeNumMinsLimitRecord = Boolean.valueOf(parameters.getParameter(PARAM_OL_INCLUDE_NUM_MINS_LIMIT_RECORD,"false")).booleanValue();
        String OLstopTcodes = parameters.getParameter(PARAM_OL_STOP_TCODES);
        boolean OLstopClockOut = Boolean.valueOf(parameters.getParameter(PARAM_OL_STOP_CLOCKOUT,"false")).booleanValue();

        // *** Rule Logic ***
        RuleData rd = wbData.getRuleData();
        String error = null;

        if (inExtraLateMinutes != Integer.MAX_VALUE && StringHelper.isEmpty(inExtraLateCode)) {
            error = "LateLELL Rule: Cannot specify 'In Extra Late Minutes' without specifying 'In Extra Late Code'.";
        }
        if (outExtraEarlyMinutes != Integer.MAX_VALUE && StringHelper.isEmpty(outExtraEarlyCode)) {
            error = "LateLELL Rule: Cannot specify 'Out Extra Early Minutes' without specifying 'Out Extra Early Code'.";
        }

        if (!wbData.isEmployeeScheduledActual()) return;

        if (error != null) {
            if (logger.isEnabledFor(Priority.ERROR)) {    logger.error(error); }
            throw new RuntimeException(error);
        }

        for (int k=0; k < rd.getShiftsWithBreaks().size(); k++) {
            if (k > 0 && applyToAllShifts == false) {
                break;
            }

            Date scheduledStart = wbData.getShiftWithBreaks(k).getShftStartTime();
            Date scheduledEnd = wbData.getShiftWithBreaks(k).getShftEndTime();
            int maxIndex = -1, minIndex = -1;


            // *** in late, 12558 apply late only if there r clocks between
            if (wbData.getCountClocks(scheduledStart , scheduledEnd) > 0) {
                processArriveLate(wbData, uatCodes, inLateCode, inExtraLateCode, inExtraLateMinutes, rd, scheduledStart, scheduledEnd);

            }


            // *** in early
            ShiftWithBreaks previousShiftEnd = wbData.getShiftWithBreaksBefore(k);
            if (isArrEarlyParsing) {
                maxIndex = getIERecordIndex(wbData,
                                          previousShiftEnd == null ? null :
                                          previousShiftEnd.getShift().
                                          getShftEndTime()
                                          , scheduledStart, wrkCodes, true,
                                          IEnumMinsLimit, IEincludeNumMinsLimitRecord,
                                          IEstopTcodes, IEstopClockIn);
            }
            else {
                maxIndex = wbData.getMaxWorkDetailIndex(
                        previousShiftEnd == null ? null : previousShiftEnd.getShift().getShftEndTime()
                        , scheduledStart, wrkCodes, true, true);
            }
            if (logger.isDebugEnabled()) logger.debug("isArrEarlyParsing=" + isArrEarlyParsing + ",maxIndex=" + maxIndex);

            if (maxIndex != -1) {
                WorkDetailData wd = rd.getWorkDetail(maxIndex);
                if (isArrEarlyParsing
                    || (!isArrEarlyParsing
                        && wd.getWrkdEndTime().getTime() == scheduledStart.getTime())) {
                    if (!StringHelper.isEmpty(inEarlyCode)) {
                        RuleHelper.assignWrkdTcodeName(wd, rd, inEarlyCode);
                    }
                }
            }


            // *** out early, 12558 apply le only if there r clocks between
            if (wbData.getCountClocks(scheduledStart , scheduledEnd) > 0) {
                processLeaveEarly(wbData, uatCodes, outEarlyCode,
                                    outExtraEarlyCode, outExtraEarlyMinutes, rd,
                                    scheduledStart, scheduledEnd);
            }


            // *** out late
            ShiftWithBreaks nextShiftStart = wbData.getShiftWithBreaksAfter(k);


            if (isOutLateParsing) {
                minIndex = getOLRecordIndex(wbData,
                                            scheduledEnd,
                                            nextShiftStart == null ? null :
                                            nextShiftStart.getShift().
                                            getShftStartTime()
                                            , wrkCodes, true,
                                            OLnumMinsLimit, OLincludeNumMinsLimitRecord,
                                            OLstopTcodes, OLstopClockOut);
            }
            else {
                minIndex = wbData.getMinWorkDetailIndex(
                        scheduledEnd ,
                        nextShiftStart == null ? null : nextShiftStart.getShift().getShftStartTime() ,
                        wrkCodes, true, true);
            }
                if (logger.isDebugEnabled()) logger.debug("isOutLateParsing=" + isOutLateParsing + ",minindex=" + minIndex);
            if (minIndex != -1) {
                WorkDetailData wd = rd.getWorkDetail(minIndex);
                if (isOutLateParsing
                    || (!isOutLateParsing
                        && wd.getWrkdStartTime().getTime() == scheduledEnd.getTime())) {
                    if (!StringHelper.isEmpty(outLateCode)) {
                        RuleHelper.assignWrkdTcodeName(wd, rd, outLateCode);
                    }

                }
            }

        }
    }

    public List getSuitableConditions() {
      List list = new ArrayList();
      list.add(new com.workbrain.app.ta.conditions.AlwaysTrueCondition());
      return list;
    }


    private void processArriveLate( WBData wbData, String uatCodes,
                                    String inLateCode, String inExtraLateCode,
                                    int inExtraLateMinutes, RuleData rd,
                                    Date scheduledStart, Date scheduledEnd) {
        int minIndex = wbData.getMinWorkDetailIndex(scheduledStart , scheduledEnd, uatCodes, true, true);
        if ( minIndex == -1 ) {
            return;
        }
        // check that our first wrkd is at the edge of the schedule and not in the middle
        WorkDetailData wd = rd.getWorkDetail(minIndex);
        if ( wd.getWrkdStartTime().getTime() != scheduledStart.getTime() ) {
            return;
        }
        /* first count how many uat mins to determine which late code to apply
         * by working FORWARD from the first wrkd records and stop as soon as it hits
         * a non-uat record.
         * Then apply the leave early code in another loop according to the totalMins accumulated.
         */
        int idxBound = rd.getWorkDetailCount();
        boolean[] arriveLateWrkdFlg = new boolean[idxBound];
        boolean hasArriveLate = false;
        int idx = minIndex;
        int totalMins = 0;
        while ( idx < idxBound ) {
            wd = rd.getWorkDetail(idx);
            if ( RuleHelper.isCodeInList(uatCodes, rd.getCodeMapper().getTimeCodeById(wd.getTcodeId()).getTcodeName()) )  {
                arriveLateWrkdFlg[idx] = true;
                hasArriveLate = true;
                totalMins += wd.getWrkdMinutes();
            }
            else {
                break;
            }
            idx++;
        }

        // now apply the arrive late timecode only if its detected.
        if ( hasArriveLate ) {
            idx = minIndex;
            String lateCode = (totalMins > inExtraLateMinutes ? inExtraLateCode : inLateCode);
            while ( idx < idxBound ) {
                if ( arriveLateWrkdFlg[idx] ) {
                    if (!StringHelper.isEmpty(lateCode)) {
                        wd = rd.getWorkDetail(idx);
                        RuleHelper.assignWrkdTcodeName(wd, rd, lateCode);
                    }
                }
                idx++;
            }
        }   // if ( hasArriveLate )
    }





    private void processLeaveEarly( WBData wbData, String uatCodes,
                                    String outEarlyCode, String outExtraEarlyCode,
                                    int outExtraEarlyMinutes, RuleData rd,
                                    Date scheduledStart, Date scheduledEnd) {
        int maxIndex = wbData.getMaxWorkDetailIndex(scheduledStart , scheduledEnd, uatCodes, true, true);
        if ( maxIndex == -1 ) {
            return;
        }
        // check that our first wrkd is at the edge of the schedule and not in the middle
        WorkDetailData wd = rd.getWorkDetail(maxIndex);
        if ( wd.getWrkdEndTime().getTime() != scheduledEnd.getTime() ) {
            return;
        }
        /* first count how many uat mins to determine which early code to apply
         * by working BACKWARD from the last wrkd records and stop as soon as it hits
         * a non-uat record.
         * Then apply the leave early code in another loop according to the totalMins accumulated.
         */
        boolean[] leaveEarlyWrkdFlg = new boolean[rd.getWorkDetailCount()];
        boolean hasLeaveEarly = false;
        int idx = maxIndex;
        int totalMins = 0;
        while ( idx > 0 ) {
            wd = rd.getWorkDetail(idx);
            if ( RuleHelper.isCodeInList(uatCodes, rd.getCodeMapper().getTimeCodeById(wd.getTcodeId()).getTcodeName()) )  {
                leaveEarlyWrkdFlg[idx] = true;
                hasLeaveEarly = true;
                totalMins += wd.getWrkdMinutes();
            }
            else {
                break;
            }
            idx--;
        }

        // now apply the leave early timecode only if its detected.
        if ( hasLeaveEarly ) {
            idx = maxIndex;
            String earlyCode = (totalMins > outExtraEarlyMinutes ? outExtraEarlyCode : outEarlyCode);
            while ( idx > 0 ) {
                if ( leaveEarlyWrkdFlg[idx] ) {
                    if (!StringHelper.isEmpty(earlyCode)) {
                        wd = rd.getWorkDetail(idx);
                        RuleHelper.assignWrkdTcodeName(wd, rd, earlyCode);
                    }
                }
                idx--;
            }
        }   // if ( hasLeaveEarly)
    }

    private int getIERecordIndex(WBData wbData,
                               Date startTime ,
                               Date endTime,
                               String tcodeNameList,
                               boolean tcodeInclusive,
                               int minutesLimit,
                               boolean includeMinutesLimitRecord,
                               String stopTcodeFoundNames,
                               boolean stopWhenClockIn) {

        WorkDetailList workDetails = wbData.getRuleData().getWorkDetails();
        startTime = startTime == null ? DateHelper.DATE_1900  : startTime;
        endTime = endTime == null ? DateHelper.DATE_3000 : endTime;

        workDetails.sort();
        workDetails.splitAt(startTime);
        workDetails.splitAt(endTime);
        int maxI = -1;
        int mins = 0;

        for (int i = workDetails.size() - 1; i >= 0; i--) {
            WorkDetailData wd = workDetails.getWorkDetail(i);
            if (wd.getWrkdEndTime().compareTo(startTime) > 0
                && wd.getWrkdEndTime().compareTo(endTime) <= 0) {
                mins += wd.getWrkdMinutes();

                maxI = i;
                if (mins < minutesLimit) {
                    if (stopWhenClockIn
                        && existsClock(wbData , wd.getWrkdStartTime() , wd.getWrkdEndTime())) {
                        break;
                    }
                    else if (!StringHelper.isEmpty(stopTcodeFoundNames)) {
                        maxI = -1;
                        if (RuleHelper.isCodeInList(stopTcodeFoundNames, wd.getWrkdTcodeName())) {
                            int priorInd = i + 1;
                            if (workDetails.size() - 1 >= priorInd) {
                                WorkDetailData wdPrior = workDetails.
                                    getWorkDetail(priorInd);
                                if (wdPrior.getWrkdEndTime().compareTo(startTime) > 0
                                    &&  wdPrior.getWrkdEndTime().compareTo(endTime) <= 0) {
                                    maxI = priorInd;
                                }
                            }
                            break;
                        }
                    }
                }
                else {
                    if (includeMinutesLimitRecord) {
                        break;
                    }
                    else {
                       maxI = -1;
                    }
                }
            }
        }
        if (maxI != -1 && workDetails.size() > 0) {
            WorkDetailData wdRet = workDetails.getWorkDetail(maxI) ;
            if (!RuleHelper.isCodeInList(tcodeNameList,
                                         wdRet.getWrkdTcodeName()) == tcodeInclusive) {
                maxI = -1;
            }

        }

        return maxI;
    }

    private int getOLRecordIndex(WBData wbData,
                               Date startTime ,
                               Date endTime,
                               String tcodeNameList,
                               boolean tcodeInclusive,
                               int minutesLimit,
                               boolean includeMinutesLimitRecord,
                               String stopTcodeFoundNames,
                               boolean stopWhenClockOut) {

        WorkDetailList workDetails = wbData.getRuleData().getWorkDetails();
        startTime = startTime == null ? DateHelper.DATE_1900  : startTime;
        endTime = endTime == null ? DateHelper.DATE_3000 : endTime;

        workDetails.sort();
        workDetails.splitAt(startTime);
        workDetails.splitAt(endTime);
        int ind = -1;
        int mins = 0;

        for (int i = 0; i < workDetails.size() ; i++) {
            WorkDetailData wd = workDetails.getWorkDetail(i);
            if (wd.getWrkdStartTime().compareTo(startTime) >= 0
                && wd.getWrkdEndTime().compareTo(endTime) <= 0) {
                mins += wd.getWrkdMinutes();

                ind = i;
                if (mins < minutesLimit) {
                    if (stopWhenClockOut
                        && existsClock(wbData , wd.getWrkdStartTime() , wd.getWrkdEndTime())) {
                        break;
                    }
                    else if (!StringHelper.isEmpty(stopTcodeFoundNames)) {
                        ind = -1;
                        if (RuleHelper.isCodeInList(stopTcodeFoundNames, wd.getWrkdTcodeName())) {
                            int priorInd = i - 1;
                            if (workDetails.size() - 1 >= priorInd) {
                                WorkDetailData wdPrior = workDetails.
                                    getWorkDetail(priorInd);
                                if (wdPrior.getWrkdEndTime().compareTo(startTime) > 0
                                    &&  wdPrior.getWrkdEndTime().compareTo(endTime) <= 0) {
                                    ind = priorInd;
                                }
                            }
                            break;
                        }
                    }
                }
                else {
                    if (includeMinutesLimitRecord) {
                        break;
                    }
                    else {
                       ind = -1;
                    }
                }
            }
        }
        if (ind != -1 && workDetails.size() > 0) {
            WorkDetailData wdRet = workDetails.getWorkDetail(ind) ;
            if (!RuleHelper.isCodeInList(tcodeNameList,
                                         wdRet.getWrkdTcodeName()) == tcodeInclusive) {
                ind = -1;
            }

        }

        return ind;
    }

    private boolean existsClock(WBData wbData,
                                  Date startTime ,
                                  Date endTime) {
        return wbData.getClocks( startTime, endTime,
                                 String.valueOf(Clock.TYPE_ON)).size() > 0;
    }
}
