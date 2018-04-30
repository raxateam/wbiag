package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;

import java.util.*;

/**
 *  Title:          EmployeeRateInPeriodCondition
 *  Description:    A condition to check employee rate totals for given period
 * Period can be
 * <ul>
 * <li>Week
 * <li>Pay Period
 * <li>Month
 * <li>Quarter
 * <li>Year
 * </ul>
 */

public class EmployeeRateInPeriodCondition extends Condition
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmployeeRateInPeriodCondition.class);
    public final static String PARAM_RATE_TOTAL = "RateTotal";
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
    public final static String PARAM_DETAIL_PREMIUM = "DetailPremium";

    public List getParameterInfo( DBConnection conn )
    {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_RATE_TOTAL, RuleParameterInfo.INT_TYPE, false));
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
        RuleParameterInfo rpiDp = new RuleParameterInfo(PARAM_DETAIL_PREMIUM, RuleParameterInfo.CHOICE_TYPE, false);
        rpiDp.addChoice(WorkDetailData.DETAIL_TYPE);
        rpiDp.addChoice(WorkDetailData.PREMIUM_TYPE);
        result.add(rpiDp);

        return result;
    }

    public boolean evaluate(WBData wbData, Parameters parameters)
        throws Exception
    {

        int rateTot = Integer.parseInt(parameters.getParameter(PARAM_RATE_TOTAL));
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
        String detailPremium = parameters.getParameter(PARAM_DETAIL_PREMIUM,null);
        double rate = getRateWorkDetailPremiumRange(wbData , dateStarts,
            dateEnds, null, null,
            tcodeNameList, tcodeInclusive,
            htypeNameList, htypeInclusive, detailPremium);

        if (logger.isDebugEnabled()) logger.debug("Checking Rate Total between " + dateStarts + " and " + dateEnds + " for " + rate + " " + operator  + " " + rateTot);
        return RuleHelper.evaluate(new Double(rate),
                                   new Double(rateTot), operator);
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

    /**
     * Finds rate total between given dates for eligible details
     * @param wbData
     * @param startDate
     * @param endDate
     * @param startTime
     * @param endTime
     * @param tcodeNameList
     * @param tcodeInclusive
     * @param htypeNameList
     * @param htypeInclusive
     * @param detailPremium
     * @return
     * @throws Exception
     */
    private double getRateWorkDetailPremiumRange(
            WBData wbData,
            Date startDate,
            Date endDate,
            Date startTime,
            Date endTime,
            String tcodeNameList,
            boolean tcodeInclusive,
            String htypeNameList,
            boolean htypeInclusive,
            String  detailPremium) throws Exception {

        if ((startTime == null && startDate == null) ||
                (endTime == null && endDate == null)) {
            return 0;
        }

        double ret = 0;

        Date datWrksWorkDate = wbData.getWrksWorkDate();
        Date datStartTime = (startTime == null
                 ? new GregorianCalendar(1900, 0, 1).getTime()
                 : new Date(startTime.getTime()));
        Date datEndTime = (endTime == null
                 ? new GregorianCalendar(3000, 0, 1).getTime()
                 : new Date(endTime.getTime()));
        Date datStartDate = (startDate == null
                 ? datWrksWorkDate
                 : new Date(DateHelper.truncateToDays(startDate).getTime()));
        Date datEndDate = (endDate == null
                 ? datWrksWorkDate
                 : new Date(DateHelper.truncateToDays(endDate).getTime()));

        WorkDetailList details = wbData.getWorkDetails(
            datStartDate,
            datEndDate,
            detailPremium);

        if (details == null) {
            return 0;
        }
        Iterator iter = details.iterator();
        while (iter.hasNext()) {
            WorkDetailData wd = (WorkDetailData)iter.next();
            boolean isEligible =
                RuleHelper.isCodeInList(htypeNameList, wd.getWrkdHtypeName()) == htypeInclusive
                && RuleHelper.isCodeInList(tcodeNameList, wd.getWrkdTcodeName()) == tcodeInclusive;
            if (!isEligible) continue;
            Date wrkdStartTime = wd.getWrkdStartTime();
            Date wrkdEndTime = wd.getWrkdEndTime();
            String wrkdType = wd.getWrkdType();
            int htypeId = wd.getHtypeId();
            double thisRate = wd.getWrkdRate() * wd.getWrkdMinutes() /60;
            ret += thisRate ;
        }

        return ret;
    }

}
