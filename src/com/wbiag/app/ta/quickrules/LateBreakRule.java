package com.wbiag.app.ta.quickrules;

import java.util.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import java.sql.*;
/**
 * Late break rule
 */
public class LateBreakRule extends Rule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LateBreakRule.class);

    public final static String PARAM_MAX_WORK_MINS_PRE_BREAK = "MaxWorkMinsPreBreak";
    public final static String PARAM_BREAK_TIMECODELIST = "BreakTimeCodeList";
    public final static String PARAM_LATE_BREAK_PREMIUM_TIMECODE = "LateBreakPremiumTimeCode";
    public final static String PARAM_LATE_BREAK_PREMIUM_HOURTYPE = "LateBreakPremiumHourType";
    public final static String PARAM_WORK_TIMECODELIST = "WorkTimeCodeList";
    public final static String PARAM_WORK_HOURTYPELIST = "WorkHourTypeList";

    public final static String PARAM_WORK_STARTTIME = "WorkStartTime";
    public final static String PARAM_VAL_WS_SHIFT_START = "SHIFT_START";
    public final static String PARAM_VAL_WS_ANYTIME_WITHIN_SHIFT = "ANYTIME_WITHIN_SHIFT";



    public List getParameterInfo (DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_MAX_WORK_MINS_PRE_BREAK, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_BREAK_TIMECODELIST, RuleParameterInfo.STRING_TYPE, false));

        RuleParameterInfo rpiIndependentOccurrencesChoice = new RuleParameterInfo(PARAM_WORK_STARTTIME, RuleParameterInfo.CHOICE_TYPE, false);
        rpiIndependentOccurrencesChoice.addChoice(PARAM_VAL_WS_SHIFT_START);
        rpiIndependentOccurrencesChoice.addChoice(PARAM_VAL_WS_ANYTIME_WITHIN_SHIFT);
        result.add(rpiIndependentOccurrencesChoice);

        result.add(new RuleParameterInfo(PARAM_LATE_BREAK_PREMIUM_TIMECODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_LATE_BREAK_PREMIUM_HOURTYPE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_WORK_TIMECODELIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_WORK_HOURTYPELIST, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {

        ParametersResolved pars = new ParametersResolved();
        // Retrieve parameters
        pars.lateBreakPremiumTimeCode = parameters.getParameter(PARAM_LATE_BREAK_PREMIUM_TIMECODE);
        TimeCodeData tcdPremTcode = wbData.getCodeMapper().getTimeCodeByName(pars.lateBreakPremiumTimeCode);
        if (tcdPremTcode == null) {
            throw new RuleEngineException("Premium timecode not found : " + pars.lateBreakPremiumTimeCode);
        }
        pars.lateBreakPremiumHourType = parameters.getParameter(PARAM_LATE_BREAK_PREMIUM_HOURTYPE);
        if (StringHelper.isEmpty(pars.lateBreakPremiumHourType)) {
            pars.lateBreakPremiumHourType = wbData.getCodeMapper().getHourTypeById(tcdPremTcode.getHtypeId()).getHtypeName();
        }
        pars.breakTimeCodeList = parameters.getParameter(PARAM_BREAK_TIMECODELIST);
        pars.workTimeCodeList = parameters.getParameter(PARAM_WORK_TIMECODELIST);

        pars.workHourTypeList = parameters.getParameter(PARAM_WORK_HOURTYPELIST, null);

        pars.maxMinsPreBreak = parameters.getIntegerParameter(PARAM_MAX_WORK_MINS_PRE_BREAK, 0);

        pars.workStartTime =  parameters.getParameter(PARAM_WORK_STARTTIME);
        if (!PARAM_VAL_WS_SHIFT_START.equals(pars.workStartTime)
            && !PARAM_VAL_WS_ANYTIME_WITHIN_SHIFT.equals(pars.workStartTime)) {
            throw new RuleEngineException("workStartTime must be " + PARAM_VAL_WS_SHIFT_START + " or " + PARAM_VAL_WS_ANYTIME_WITHIN_SHIFT);
        }
        applyLateBreak (wbData , pars.workStartTime, pars);
    }

    protected void applyLateBreak(WBData wbData , String workStartTime,
                                  ParametersResolved pars) {

        WorkDetailList wdl = wbData.getRuleData().getWorkDetails();
        int brkMins = wdl.getMinutes(null , null, pars.breakTimeCodeList, true, null, true);
        if (brkMins == 0) {
            if (logger.isDebugEnabled()) logger.debug("No Break minutes to process");
            return;
        }
        int consecWrkMins = 0; int brksHandled = 0;
        for (int i = 0, j = wdl.size() ; i < j; i++) {
            WorkDetailData workDetailData = wdl.getWorkDetail(i);
            boolean isWrkCode =
                RuleHelper.isCodeInList(pars.workTimeCodeList , workDetailData.getWrkdTcodeName())
                && (RuleHelper.isCodeInList(pars.workHourTypeList , workDetailData.getWrkdHtypeName()));
            boolean isBrkCode =
                RuleHelper.isCodeInList(pars.breakTimeCodeList , workDetailData.getWrkdTcodeName());
            if (isWrkCode) {
                consecWrkMins += workDetailData.getWrkdMinutes();
            }
            if (isBrkCode) {
                int premMins = consecWrkMins - pars.maxMinsPreBreak;
                if (premMins > 0) {
                    wbData.insertWorkPremiumRecord(
                        premMins,
                        pars.lateBreakPremiumTimeCode,
                        pars.lateBreakPremiumHourType);
                }
                consecWrkMins = 0;
                brksHandled++;
                if (PARAM_VAL_WS_SHIFT_START.equals(workStartTime)
                    && brksHandled == 1) {
                    break;
                }
            }
            if (!isWrkCode) {
                consecWrkMins = 0;
            }
        }
    }

    public String getComponentName() {
        return "WBIAG : Late Break Rule";
    }

    public class ParametersResolved {
        public int maxMinsPreBreak;
        public String breakTimeCodeList;
        public String lateBreakPremiumTimeCode;
        public String lateBreakPremiumHourType;
        public String workTimeCodeList;
        public String workHourTypeList;

        public String workStartTime;

    }

}
