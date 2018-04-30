package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.conditions.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.DateHelper;
import java.util.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
/**
 *  Title:        Is Holiday Extended Condition
 *  Description:  Checks whether given holidays (if HolidayName null, any holiday)
 *                exist for either Yesterday, Today or Tomorrow.
 *  Copyright:    Copyright (c) 2002
 *  Company:      Workbrain Inc
 *
 *@deprecated As of 5.0.2.0, use core classes 
 *@author     Manisha Luthra
 *@version    2.0
 */

public class IsHolidayExtendedCondition extends IsHolidayCondition {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsHolidayExtendedCondition.class);

    public final static String PARAM_HOLIDAY_NAME = "HolidayName";
    public final static String PARAM_DAYS_TO_CHECK = "DayToCheck";
    public final static String PARAM_DAY_TYPE_APPLIEDTO = "DayTypeAppliedTo";
    public final static String SKD_ALL = "ALL";
    public final static String SKD_SCHEDULED = "SCHEDULED";
    public final static String SKD_UNSCHEDULED = "UNSCHEDULED";


    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
		result.add(new RuleParameterInfo(PARAM_HOLIDAY_NAME, RuleParameterInfo.STRING_TYPE, true));
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_DAYS_TO_CHECK, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice("Yesterday");
        rpi.addChoice("Today");
        rpi.addChoice("Tomorrow");
        rpi.addChoice(WBData.WORKED_LAST);
        rpi.addChoice(WBData.WORKED_NEXT);
        result.add(rpi);
        RuleParameterInfo rpiDayTypeAppliedToChoice = new RuleParameterInfo(PARAM_DAY_TYPE_APPLIEDTO, RuleParameterInfo.CHOICE_TYPE, true);
        rpiDayTypeAppliedToChoice.addChoice(SKD_ALL);
        rpiDayTypeAppliedToChoice.addChoice(SKD_SCHEDULED);
        rpiDayTypeAppliedToChoice.addChoice(SKD_UNSCHEDULED);
        result.add(rpiDayTypeAppliedToChoice);

        return result;
    }

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception
    {
        String holidayNameList = parameters.getParameter(PARAM_HOLIDAY_NAME);
        String op = parameters.getParameter(PARAM_DAYS_TO_CHECK);

        Date today = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        Date evaluateDate = null;

        if (op.equals("Yesterday")) { //check if yesterday was a holiday
            evaluateDate = DateHelper.addDays(today, -1);
        }
        else if (op.equals("Tomorrow")) { //check if tomorrow is a holiday
            evaluateDate = DateHelper.addDays(today, +1);
        }
        else if (op.equals("Today")) {
            evaluateDate = today;
        }
        else if (op.equals(WBData.WORKED_LAST) || op.equals(WBData.WORKED_NEXT)) {
            String dayType = parameters.getParameter(PARAM_DAY_TYPE_APPLIEDTO);
            int searchDays = WBData.WORKED_LAST.equalsIgnoreCase(op)
                ? -WBData.WORKED_LAST_NEXT_MAXIMUM_DAYS_CHECK   : WBData.WORKED_LAST_NEXT_MAXIMUM_DAYS_CHECK;
            evaluateDate = wbData.getWorkedNthDayDate(wbData.getWrksWorkDate(),
                searchDays, dayType, 1,
                null, true, null, true, op, -999, WorkDetailData.DETAIL_TYPE);
        }
        else {
            throw new RuleEngineException ("Day to check not supported :" + op);
        }
        return wbData.existsHolidayRange(evaluateDate,
                                         evaluateDate,
                                         holidayNameList);
    }

    public String getComponentName() {
        return "WBIAG: Is Holiday Extended Condition";
    }

    public String getDescription() {
        return "Applies if the specific holiday is within x days of that day";
    }

}
