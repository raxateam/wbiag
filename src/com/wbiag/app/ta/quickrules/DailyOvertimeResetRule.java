package com.wbiag.app.ta.quickrules;

import java.text.*;
import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.quickrules.DailyOvertimeRule;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.rules.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * DailyOvertimeResetRule - resets overtime after a time code is encountered for given duration
 */
public class DailyOvertimeResetRule extends GenericOvertimeRule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DailyOvertimeResetRule.class);

    public final static String PARAM_DISCOUNT_TIMECODES = "DiscountTimeCodes";
    public final static String PARAM_ASSIGN_BETTERRATE = "AssignBetterRate";
    public final static String PARAM_RESET_TIME_CODES = "ResetTimeCodes";
    public final static String PARAM_RESET_DURATION = "ResetDuration";

    //protected String resetTCodes;
    //protected int resetDuration;

    public List getParameterInfo(DBConnection parm1) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_HOURSET_DESCRIPTION, RuleParameterInfo.STRING_TYPE, false));

        RuleParameterInfo rpiAddPremiumForFirstHourtypeTokenChoice = new RuleParameterInfo(PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN, RuleParameterInfo.CHOICE_TYPE, true);
        rpiAddPremiumForFirstHourtypeTokenChoice.addChoice("true");
        rpiAddPremiumForFirstHourtypeTokenChoice.addChoice("false");
        result.add(rpiAddPremiumForFirstHourtypeTokenChoice);

        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_HOURTYPES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DISCOUNT_TIMECODES, RuleParameterInfo.STRING_TYPE, true));

        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODE, RuleParameterInfo.STRING_TYPE, true));

        RuleParameterInfo rpiAssignBetterRateChoice = new RuleParameterInfo(PARAM_ASSIGN_BETTERRATE, RuleParameterInfo.CHOICE_TYPE, true);
        rpiAssignBetterRateChoice.addChoice("true");
        rpiAssignBetterRateChoice.addChoice("false");
        result.add(rpiAssignBetterRateChoice);
        result.add(new RuleParameterInfo(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, RuleParameterInfo.STRING_TYPE, true));

        result.add(new RuleParameterInfo(PARAM_RESET_TIME_CODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_RESET_DURATION, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {
        // *** Retrieve parameters
        String resetTCodes = parameters.getParameter(PARAM_RESET_TIME_CODES);
        int resetDuration = parameters.getIntegerParameter(PARAM_RESET_DURATION , 0);

        if (StringHelper.isEmpty(resetTCodes)) {
            if (logger.isDebugEnabled()) logger.debug("PARAM_RESET_TIME_CODES not defined , exiting rule");
            return;
        }
        if (wbData.getRuleData().getWorkDetails().size() == 0) {
            return;
        }

        List zones = getOvertimeZones(wbData, resetTCodes, resetDuration);
        Iterator iter = zones.iterator();
        while (iter.hasNext()) {
            StartEndTime se = (StartEndTime)iter.next();
            ruleDailyOTByStartEndEndTime(wbData , parameters, se.startTime , se.endTime);
        }
    }

    /**
     * Returns List of <code>StartEndTime</code> that Overtime will be applied seperately.
     * @param wbData
     * @return
     */
    protected List getOvertimeZones(WBData wbData, String resetTCodes, int resetDuration) {
        List ret = new ArrayList();
        WorkDetailList wdl = wbData.getRuleData().getWorkDetails();
        wdl.sort();
        int resetMinutesTotal = 0;
        boolean isReset = false;
        Iterator iter = wdl.iterator();
        while (iter.hasNext()) {
            WorkDetailData wd = (WorkDetailData)iter.next();
            if (RuleHelper.isCodeInList(resetTCodes , wd.getWrkdTcodeName())) {
                resetMinutesTotal+= wd.getWrkdMinutes();
                isReset = resetMinutesTotal > resetDuration ? true : false;
            }
            else {
                if (isReset || ret.size() == 0) {
                    StartEndTime se = new StartEndTime();
                    se.startTime = wd.getWrkdStartTime();
                    se.endTime = wd.getWrkdEndTime();
                    ret.add(se);
                    resetMinutesTotal = 0;
                    isReset = false;
                }
                else {
                    if (ret.size() > 0) {
                        StartEndTime se = (StartEndTime)ret.get(ret.size() - 1);
                        se.endTime = wd.getWrkdEndTime();
                    }
                }
            }
        }
        if (logger.isDebugEnabled() && ret.size() > 0) {
            logger.debug("Overtime zones :");
            Iterator iter2 = ret.iterator();
            while (iter2.hasNext()) {
                StartEndTime item = (StartEndTime)iter2.next();
                logger.debug(item);
            }
        }
        return ret;
    }

    protected void ruleDailyOTByStartEndEndTime (WBData wbData, Parameters parameters,
                                      Date startTime, Date endTime) throws Exception {

        String hourSetDescription = parameters.getParameter(PARAM_HOURSET_DESCRIPTION);
        boolean addPremiumForFirstHourtypeToken = Boolean.valueOf(parameters.getParameter(PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN, "false")).booleanValue();
        String workDetailTimeCodes = parameters.getParameter(PARAM_ELIGIBLE_TIMECODES);
        String eligibleHourTypes = parameters.getParameter(PARAM_ELIGIBLE_HOURTYPES, "REG");
        String discountTimeCodes = parameters.getParameter(PARAM_DISCOUNT_TIMECODES, null);
        String premiumTimeCodeInserted = parameters.getParameter(PARAM_PREMIUM_TIMECODE, null);
        // assignBetterRate is a protected property inherited from GenericOvertimeRule
        boolean assignBetterRate = Boolean.valueOf(parameters.getParameter(PARAM_ASSIGN_BETTERRATE, "true")).booleanValue();
        String hourTypeForOvertimeWorkDetails = parameters.getParameter(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, null);

        // after cutOff time
        Parameters parametersForGenericOvertimeRule = new Parameters();

        int seedMinutes = 0;

        if ((discountTimeCodes != null) && (wbData.getRuleData().getWorkDetailCount() > 0)) {
            seedMinutes += wbData.getMinutesWorkDetailPremiumRange(wbData.getWrksWorkDate(),
                wbData.getWrksWorkDate(),
                startTime,
                endTime,
                discountTimeCodes, true, eligibleHourTypes, true, "D" , false);
        }

        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURSET_DESCRIPTION, hourSetDescription);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN,
                String.valueOf(addPremiumForFirstHourtypeToken));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADDITIONAL_MINUTES_WORKED, String.valueOf(seedMinutes));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_TIMECODES, workDetailTimeCodes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ARE_TIMECODES_INCLUSIVE, "true");
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_HOURTYPES, eligibleHourTypes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADD_PREMIUMRECORD, String.valueOf(premiumTimeCodeInserted != null));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_PREMIUM_TIMECODE, premiumTimeCodeInserted);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
            hourTypeForOvertimeWorkDetails);
        DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_STARTTIME_WITHIN_SHIFT,
            dateFormat.format(startTime));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ENDTIME_WITHIN_SHIFT,
            dateFormat.format(endTime));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
            String.valueOf(assignBetterRate));
        super.execute(wbData, parametersForGenericOvertimeRule);
    }

    public String getComponentUI() {
        return null;
    }

    protected class StartEndTime {
        Date startTime;
        Date endTime;

        public String toString() {
            return "startTime :" + startTime + " endTime : " + endTime;
        }
    }

    public String getComponentName() {
        return "WBIAG: Daily Overtime Reset Rule";
    }


}