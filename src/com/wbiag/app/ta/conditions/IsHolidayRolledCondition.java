package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 *  Title:        Is Holiday Rolled Condition
 */
public class IsHolidayRolledCondition extends Condition {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsHolidayRolledCondition.class);

    public final static String PARAM_HOLIDAY_NAME = "HolidayName";

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {
        String holName = parameters.getParameter(PARAM_HOLIDAY_NAME);

        List holRolls = wbData.getHolidaysRolled();
        boolean exists = false;
        if (StringHelper.isEmpty(holName)) {
            exists = holRolls.size() != 0;
            if (logger.isDebugEnabled()) logger.debug(holRolls.size() + " rolled holidays exist");
        }
        else {
            Iterator iter = holRolls.iterator();
            while (iter.hasNext()) {
                HolidayData item = (HolidayData)iter.next();
                exists = RuleHelper.isCodeInList(holName, item.getHolName());
                if (exists) {
                    if (logger.isDebugEnabled()) logger.debug("Rolled holiday exists : " + item.getHolName());
                    break;
                }
            }
        }

        return exists;
    }


    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_HOLIDAY_NAME, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }


    public String getComponentName() {
        return "WBIAG: Is Holiday Rolled Condition";
    }

    public String getDescription() {
        return "Evaluates if employee has a holiday rolled or not";
    }


}
