package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 *  Title:          IsWhatPayPeriodCondition
 *  Description:    A condition to find what pay period current worksDate is at. Excludes
 *                  the period that start before 1st day of year
 */
public class IsWhatPayPeriodCondition extends Condition{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsWhatPayPeriodCondition.class);

    public final static String PARAM_PAY_PERIOD_NUMBER = "PayPeriodNumber";
    public final static String PARAM_OPERATOR = "Operator";

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception    {

        int ppNumber = parameters.getIntegerParameter(PARAM_PAY_PERIOD_NUMBER);
        String operator = parameters.getParameter(PARAM_OPERATOR);

        PayGroupData paygrp = wbData.getCodeMapper().getPayGroupById(wbData.getPaygrpId());
        int paygrpDuration = DateHelper.getDifferenceInDays(
            paygrp.getPaygrpEndDate() , paygrp.getPaygrpStartDate() ) + 1;
        Date wrksDate = wbData.getWrksWorkDate();
        Date lastDayOfPGOverlappingWrksDate = DateHelper.getUnitPayPeriod(
            DateHelper.APPLY_ON_LAST_DAY, false, wrksDate, paygrp);
        // *** find the first pay period in year that starts after 1st day of year
        Date firstDayOfYear = DateHelper.getUnitYear(DateHelper.APPLY_ON_FIRST_DAY, false, wrksDate);
        Date lastDayOfPGOverlappingFirstDay = DateHelper.getUnitPayPeriod(
            DateHelper.APPLY_ON_LAST_DAY, false, firstDayOfYear, paygrp);
        if (lastDayOfPGOverlappingFirstDay.before(firstDayOfYear)) {
            lastDayOfPGOverlappingFirstDay =
                DateHelper.addDays(lastDayOfPGOverlappingFirstDay, paygrpDuration) ;
        }
        // *** find the number
        int paygrpNumber = (DateHelper.getDifferenceInDays(
            lastDayOfPGOverlappingWrksDate , lastDayOfPGOverlappingFirstDay ) / paygrpDuration) + 1;

        if (logger.isDebugEnabled()) logger.debug("lastDayOfPGOverlappingWrksDate=" + lastDayOfPGOverlappingWrksDate + ",lastDayOfPGOverlappingFirstDay=" + lastDayOfPGOverlappingFirstDay + ",paygrpDuration=" + paygrpDuration + ",paygrpNumber=" + paygrpNumber);
        boolean ret = RuleHelper.evaluate(new Integer(paygrpNumber),
                                          new Integer(ppNumber), operator);
        return ret;
    }

    public List getParameterInfo( DBConnection conn )
    {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_PAY_PERIOD_NUMBER, RuleParameterInfo.INT_TYPE, false));
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_OPERATOR, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(RuleHelper.EQ);
        rpi.addChoice(RuleHelper.LESS);
        rpi.addChoice(RuleHelper.BIGGER);
        rpi.addChoice(RuleHelper.LESSEQ);
        rpi.addChoice(RuleHelper.BIGGEREQ);
        rpi.addChoice(RuleHelper.NOTEQ1);
        result.add(rpi);
        return result;
    }

    public String getComponentName()
    {
        return "WBIAG: Is What PayPeriod Condition";
    }
}
