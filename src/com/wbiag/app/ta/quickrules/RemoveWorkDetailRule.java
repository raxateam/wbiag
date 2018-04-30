package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
/**
 * RemoveWorkDetailRule
 */
public class RemoveWorkDetailRule extends Rule{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RemoveWorkDetailRule.class);

    public static final String PARAM_TIME_CODES = "TimeCodes";
    public static final String PARAM_TIME_CODES_INCLUSIVE = "TimeCodesInclusive";
    public static final String PARAM_WORK_DETAIL_FILTER = "WorkDetailFilter";
    public static final String PARAM_HOUR_TYPES = "HourTypes";
    public static final String PARAM_HOUR_TYPES_INCLUSIVE = "HourTypesInclusive";
    public static final String PARAM_EXPRESSION_STRING = "ExpressionString";

    public static final String PARAM_VAL_ALL = "All";
    public static final String PARAM_VAL_PREMIUMS = "Premiums";
    public static final String PARAM_VAL_WITHIN_SCHEDULE = "WithinSchedule";
    public static final String PARAM_VAL_OUTSIDE_SCHEDULE = "OutsideSchedule";



    public List getParameterInfo(DBConnection dBConnection) {
        List result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_TIME_CODES,
                                         RuleParameterInfo.STRING_TYPE, true));
        RuleParameterInfo timeCodesInclusive = new RuleParameterInfo(
            PARAM_TIME_CODES_INCLUSIVE, RuleParameterInfo.CHOICE_TYPE, true);
        timeCodesInclusive.addChoice("true");
        timeCodesInclusive.addChoice("false");
        result.add(timeCodesInclusive);
        result.add(new RuleParameterInfo(PARAM_HOUR_TYPES,
                                         RuleParameterInfo.STRING_TYPE, true));
        RuleParameterInfo hourTypesInclusive = new RuleParameterInfo(
            PARAM_HOUR_TYPES_INCLUSIVE, RuleParameterInfo.CHOICE_TYPE, true);
        hourTypesInclusive.addChoice("true");
        hourTypesInclusive.addChoice("false");
        result.add(hourTypesInclusive);
        RuleParameterInfo wdFilterP = new RuleParameterInfo(PARAM_WORK_DETAIL_FILTER, RuleParameterInfo.CHOICE_TYPE, true);
        wdFilterP.addChoice(PARAM_VAL_ALL);
        wdFilterP.addChoice(PARAM_VAL_WITHIN_SCHEDULE);
        wdFilterP.addChoice(PARAM_VAL_OUTSIDE_SCHEDULE);
        result.add(wdFilterP);
        result.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }


    public void execute(WBData wbData, Parameters parameters) throws Exception {

        ParametersResolved pars = new ParametersResolved();
        pars.tcodes =  parameters.getParameter(PARAM_TIME_CODES , null);
        pars.tcodesInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TIME_CODES_INCLUSIVE, "true")).booleanValue();
        pars.htypes =  parameters.getParameter(PARAM_HOUR_TYPES , null);
        pars.htypesInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HOUR_TYPES_INCLUSIVE, "true")).booleanValue();
        pars.wdFilter = parameters.getParameter(PARAM_WORK_DETAIL_FILTER, PARAM_VAL_ALL);
        pars.exprString = parameters.getParameter(PARAM_EXPRESSION_STRING, null);

        if (PARAM_VAL_ALL.equals(pars.wdFilter)) {
            wbData.removeWorkDetails(null, null, pars.tcodes, pars.tcodesInclusive,
                                     pars.htypes, pars.htypesInclusive, pars.exprString);
        }
        else if (PARAM_VAL_WITHIN_SCHEDULE.equals(pars.wdFilter)) {
            processWithinSchedule(wbData, pars);
        }
        else if (PARAM_VAL_OUTSIDE_SCHEDULE.equals(pars.wdFilter)) {
            processOutsideSchedule(wbData, pars);
        }
        else if (PARAM_VAL_PREMIUMS.equals(pars.wdFilter)) {
            wbData.removeWorkPremiums(pars.tcodes, pars.tcodesInclusive,
                                     pars.htypes, pars.htypesInclusive, pars.exprString);
        }
        else {
            throw new RuntimeException ("Unknown filter : " + pars.wdFilter);
        }
    }

    protected void processWithinSchedule(WBData wbData, ParametersResolved pars) {
        for (int i=0 , k = wbData.getShiftsWithBreaks().size(); i<k; i++) {
            ShiftWithBreaks swb = wbData.getShiftWithBreaks(i);
            if (!swb.isScheduledActual()) break;
            wbData.removeWorkDetails(swb.getShftStartTime() , swb.getShftEndTime() ,
                                     pars.tcodes, pars.tcodesInclusive,
                                     pars.htypes, pars.htypesInclusive, pars.exprString);
        }

    }

    protected void processOutsideSchedule(WBData wbData, ParametersResolved pars) {
        for (int i=0 , k = wbData.getShiftsWithBreaks().size(); i<k; i++) {
            ShiftWithBreaks swb = wbData.getShiftWithBreaks(i);
            if (!swb.isScheduledActual()) break;
            ShiftWithBreaks swbPrior = wbData.getShiftWithBreaksPrior(i);
            ShiftWithBreaks swbNext = wbData.getShiftWithBreaksNext(i);
            Date prevEnd = swbPrior == null ? null : swbPrior.getShftEndTime();
            Date nextStart = swbNext == null ? null : swbNext.getShftStartTime();
            wbData.removeWorkDetails(prevEnd , swb.getShftStartTime() ,
                                     pars.tcodes, pars.tcodesInclusive,
                                     pars.htypes, pars.htypesInclusive, pars.exprString);
            wbData.removeWorkDetails(swb.getShftEndTime() , nextStart ,
                                     pars.tcodes, pars.tcodesInclusive,
                                     pars.htypes, pars.htypesInclusive, pars.exprString);

        }
    }

    public String getComponentName() {
        return "WBIAG: Remove Work Detail Rule";
    }

    public class ParametersResolved {
        public String tcodes = null;
        public boolean tcodesInclusive = true;
        public String htypes = null;
        public String wdFilter = null;
        public boolean htypesInclusive = true;
        public String exprString = null;

    }
}
