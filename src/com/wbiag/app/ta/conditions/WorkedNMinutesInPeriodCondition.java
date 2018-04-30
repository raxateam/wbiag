package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;

import java.util.*;

/**
 *  Title:          WorkedNMinutesInPeriodCondition
 *  Description:    A condition to check the number of minutes worked so far in the given period
 * Period can be
 * <ul>
 * <li>Week
 * <li>Pay Period
 * <li>Month
 * <li>Quarter
 * <li>Year
 * </ul>
 */

public class WorkedNMinutesInPeriodCondition extends Condition
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WorkedNMinutesInPeriodCondition.class);
    public final static String PARAM_WORK_MINUTES = "WorkMinutes";
    public final static String PARAM_OPERATOR = "Operator";
    public final static String PARAM_TCODE_NAME_LIST = "TcodeNameList";
    public final static String PARAM_TCODE_INCLUSIVE = "TcodeInclusive";
    public final static String PARAM_HTYPE_NAME_LIST = "HtypeNameList";
    public final static String PARAM_HTYPE_INCLUSIVE = "HtypeInclusive";
    public final static String PARAM_APPLY_ON_UNIT = "ApplyOnUnit";
    public final static String PARAM_APPLY_ON_VALUE_START = "ApplyOnValueStart";
    public final static String PARAM_APPLY_ON_VALUE_END = "ApplyOnValueEnd";
    public final static String PARAM_VAL_UP_TO_CURRENT_WORK_DATE_INCLUDING = "UP_TO_CURRENT_WORK_DATE_INCLUDING";
    public final static String PARAM_VAL_UP_TO_CURRENT_WORK_DATE_EXCLUDING = "UP_TO_CURRENT_WORK_DATE_EXCLUDING";

    public boolean evaluate(WBData wbData, Parameters parameters)
        throws Exception
    {

        int requiredMinutes = Integer.parseInt(parameters.getParameter(PARAM_WORK_MINUTES));
        String operator = parameters.getParameter(PARAM_OPERATOR);
        String tcodeNameList = parameters.getParameter(PARAM_TCODE_NAME_LIST, null);
        boolean tcodeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TCODE_INCLUSIVE,"true")).booleanValue();
        String htypeNameList = parameters.getParameter(PARAM_HTYPE_NAME_LIST, null);
        boolean  htypeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HTYPE_INCLUSIVE,"true")).booleanValue();
        String applyOnUnit = parameters.getParameter(PARAM_APPLY_ON_UNIT);
        if (StringHelper.isEmpty(applyOnUnit)) {
            throw new RuntimeException("Apply on unit cannot be empty");
        }
        String applyOnValueStart = parameters.getParameter(PARAM_APPLY_ON_VALUE_START);
        if (StringHelper.isEmpty(applyOnValueStart)) {
            throw new RuntimeException("Apply on value start cannot be empty");
        }
        String applyOnValueEnd = parameters.getParameter(PARAM_APPLY_ON_VALUE_END);
        if (StringHelper.isEmpty(applyOnValueEnd)) {
            throw new RuntimeException("Apply on value end cannot be empty");
        }

        Date dateStarts = getRangeDate(wbData , applyOnUnit , applyOnValueStart) ;
        Date dateEnds = null;
        if (PARAM_VAL_UP_TO_CURRENT_WORK_DATE_INCLUDING.equals(applyOnValueEnd)) {
            dateEnds = wbData.getWrksWorkDate();
        }
        else if (PARAM_VAL_UP_TO_CURRENT_WORK_DATE_EXCLUDING.equals(applyOnValueEnd)) {
            dateEnds = DateHelper.addDays(wbData.getWrksWorkDate() , -1);
        }
        else {
            dateEnds = getRangeDate(wbData , applyOnUnit , applyOnValueEnd) ;
        }
        int minutes = wbData.getMinutesWorkDetailRange(dateStarts,
            dateEnds, null, null,
            tcodeNameList, tcodeInclusive,
            htypeNameList, htypeInclusive);

        if (logger.isDebugEnabled()) logger.debug("Checking Worked Minutes between " + dateStarts + " and " + dateEnds + " for " + minutes + " " + operator + " " + requiredMinutes);
        return RuleHelper.evaluate(new Integer(minutes),
                                   new Integer(requiredMinutes), operator);
    }

    public List getParameterInfo( DBConnection conn )
    {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_WORK_MINUTES, RuleParameterInfo.INT_TYPE, false));
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_OPERATOR, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(RuleHelper.EQ);
        rpi.addChoice(RuleHelper.LESS);
        rpi.addChoice(RuleHelper.BIGGER);
        rpi.addChoice(RuleHelper.LESSEQ);
        rpi.addChoice(RuleHelper.BIGGEREQ);
        rpi.addChoice(RuleHelper.NOTEQ1);
        result.add(rpi);
        result.add(new RuleParameterInfo(PARAM_TCODE_NAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TCODE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_NAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        RuleParameterInfo rpiApplyOnUnit = new RuleParameterInfo(PARAM_APPLY_ON_UNIT, RuleParameterInfo.CHOICE_TYPE, false);
        rpiApplyOnUnit.addChoice(DateHelper.APPLY_ON_UNIT_DAY);
        rpiApplyOnUnit.addChoice(DateHelper.APPLY_ON_UNIT_MONTH);
        rpiApplyOnUnit.addChoice(DateHelper.APPLY_ON_UNIT_PAYPERIOD);
        rpiApplyOnUnit.addChoice(DateHelper.APPLY_ON_UNIT_QTR);
        rpiApplyOnUnit.addChoice(DateHelper.APPLY_ON_UNIT_WEEK);
        rpiApplyOnUnit.addChoice(DateHelper.APPLY_ON_UNIT_YEAR);
        result.add(rpiApplyOnUnit);
        result.add(new RuleParameterInfo(PARAM_APPLY_ON_VALUE_START, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_APPLY_ON_VALUE_END, RuleParameterInfo.STRING_TYPE, false));

        return result;
    }

    public String getComponentName()
    {
        return "WBIAG: Worked N Minutes In Period Condition";
    }

    protected Date getRangeDate(WBData wbData, String applyOnUnit , String applyOnValue) {
        Date ret = null;
        if (DateHelper.APPLY_ON_UNIT_DAY.equals(applyOnUnit)) {
            if (!StringHelper.isEmpty(applyOnValue)) {
                return DateHelper.addMinutes(wbData.getWrksWorkDate(),
                                          Integer.parseInt(applyOnValue));
            }
        }
        else if (DateHelper.APPLY_ON_UNIT_MONTH.equals(applyOnUnit)) {
            ret = DateHelper.getUnitMonth(applyOnValue , false, wbData.getWrksWorkDate());
        }
        else if (DateHelper.APPLY_ON_UNIT_PAYPERIOD.equals(applyOnUnit)) {
            PayGroupData pgd = wbData.getRuleData().getCodeMapper().getPayGroupById(wbData.getPaygrpId());
            ret = DateHelper.getUnitPayPeriod(applyOnValue , false, wbData.getWrksWorkDate() , pgd);
        }
        else if (DateHelper.APPLY_ON_UNIT_QTR.equals(applyOnUnit)) {
            ret = DateHelper.getUnitQtr(applyOnValue , false, wbData.getWrksWorkDate());
        }
        else if (DateHelper.APPLY_ON_UNIT_YEAR.equals(applyOnUnit)) {
            ret = DateHelper.getUnitYear(applyOnValue , false, wbData.getWrksWorkDate());
        }
        else if (DateHelper.APPLY_ON_UNIT_WEEK.equals(applyOnUnit)) {
            ret = DateHelper.getUnitWeek(applyOnValue , false, wbData.getWrksWorkDate());
        }
        else {
            throw new RuntimeException("ApplyonUnit not supported : " + applyOnUnit);
        }
        return ret;
    }

}
