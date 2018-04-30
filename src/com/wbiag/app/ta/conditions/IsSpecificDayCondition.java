package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Checks whether given day is the calculation date
 */
public class IsSpecificDayCondition extends Condition {
    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsSpecificDayCondition.class);

    public final static String PARAM_DAYS_OF_WEEK = "DaysOfWeek";
    public final static String PARAM_INCLUSIVE = "Inclusive";

    public List getParameterInfo(DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_DAYS_OF_WEEK, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_INCLUSIVE, RuleParameterInfo.STRING_TYPE));
        return result;
    }

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {
        String daysOfWeek = parameters.getParameter(PARAM_DAYS_OF_WEEK);
        boolean inclusive = Boolean.valueOf(parameters.getParameter(
            PARAM_INCLUSIVE,
            Boolean.TRUE.toString())).booleanValue();

        if (StringHelper.isEmpty(daysOfWeek)) {
            throw new RuntimeException ("DaysOfWeek cannot empty null");
        }

        String[] days = StringHelper.detokenizeString(daysOfWeek , ",");
        IntegerList daysCal = new IntegerList();
        for (int i = 0; i < days.length; i++) {
            daysCal.add(DateHelper.getCalendarDay(days[i]));
        }
        Calendar workDate = DateHelper.getCalendarTruncatedToDay(wbData.getWrksWorkDate());
        boolean res = daysCal.contains(workDate.get(Calendar.DAY_OF_WEEK)) ==  inclusive;
        return res;
    }

    public String getComponentName() {
        return "WBIAG: Is Specific Day Condition";
    }

    public String getDescription() {
        return "Determines calculation date is one of given days or not";
    }
}

