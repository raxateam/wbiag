package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Checks whether the current month is one of the given months
 */
public class IsSpecificMonthCondition extends Condition {
    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsSpecificMonthCondition.class);

    public final static String PARAM_MONTHS_OF_YEAR = "MonthsOfYear";
    public final static String PARAM_INCLUSIVE = "Inclusive";

    public List getParameterInfo(DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_MONTHS_OF_YEAR, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_INCLUSIVE, RuleParameterInfo.STRING_TYPE));
        return result;
    }

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {
        String monthsYear = parameters.getParameter(PARAM_MONTHS_OF_YEAR);
        boolean inclusive = Boolean.valueOf(parameters.getParameter(
            PARAM_INCLUSIVE,
            Boolean.TRUE.toString())).booleanValue();

        if (StringHelper.isEmpty(monthsYear)) {
            throw new RuntimeException ("MonthsOfYear cannot empty null");
        }

        String[] months = StringHelper.detokenizeString(monthsYear , ",");
        IntegerList monthsCal = new IntegerList();
        for (int i = 0; i < months.length; i++) {
            monthsCal.add(getCalendarMonth(months[i]));
        }
        Calendar workDate = DateHelper.getCalendarTruncatedToDay(wbData.getWrksWorkDate());
        boolean res = monthsCal.contains(workDate.get(Calendar.MONTH)) ==  inclusive;
        return res;
    }

    public String getComponentName() {
        return "WBIAG: Is Specific Month Condition";
    }

    public String getDescription() {
        return "Determines calculation date is one of given months or not";
    }

    /**
     * Returns the int  value for month of the yeat.
     *
     *@param   month String representing a month
     *@return      month of year as in Calendar class
     */
    private int getCalendarMonth(String month){
        month = month.toUpperCase();
        if (month.indexOf("JAN") == 0 ) {
            return Calendar.JANUARY;
        } else if (month.indexOf("FEB") == 0) {
            return Calendar.FEBRUARY;
        } else if (month.indexOf("MAR") == 0) {
            return Calendar.MARCH ;
        } else if (month.indexOf("APR") == 0) {
            return Calendar.APRIL;
        } else if (month.indexOf("MAY") == 0) {
            return Calendar.MAY;
        } else if (month.indexOf("JUN") == 0) {
            return Calendar.JUNE;
        } else if (month.indexOf("JUL") == 0) {
            return Calendar.JULY;
        } else if (month.indexOf("AUG") == 0) {
            return Calendar.AUGUST;
        } else if (month.indexOf("SEP") == 0) {
            return Calendar.SEPTEMBER;
        } else if (month.indexOf("OCT") == 0) {
            return Calendar.OCTOBER;
        } else if (month.indexOf("NOV") == 0) {
            return Calendar.NOVEMBER;
        } else if (month.indexOf("DEC") == 0) {
            return Calendar.DECEMBER;
        } else {
            throw new IllegalArgumentException("month must be one of {JAN, " +
                    "FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV ,DEC)");
        }
    }

}

