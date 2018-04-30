package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.util.DateHelper;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;

/**
 * Checks to see if there is a holiday in the current week.
 *
 * @deprecated Use {@link #ExistsOverrideCondition} as of 5.0 with OverrideIDRange=900-999
 */
public class ExistsHolidayInWeekCondition
    extends Condition {

    public static final String REGISTRY_DAY_WEEK_STARTS = "DAY_WEEK_STARTS";

    public ExistsHolidayInWeekCondition() {
    }

    /**
     * There are no parameter in this condition
     *
     * @param conn - database Connection Object
     * @return List - a list of RuleParameterInfo instances that describe the parameters used by the rule component
     *
     */
    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        return result;
    }

    /**
     * Returns the name of the Rule
     *
     * @return String - the unique name of this rule component
     */
    public String getComponentName() {
        return "WBIAG: Exists Holiday In Week Condition";
    }

    /**
     * Checks if there is a holiday in the current week
     *
     * @param wbData - all the information <BR>
     *  parameters - RuleData parameters
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws
        Exception {

        Date now = wbData.getWrksWorkDate();
        Date start = DateHelper.getUnitWeek("0", false, now);
        Date end = DateHelper.getUnitWeek("7", false, now);
        return wbData.existsHolidayRange(start, end, null);

    } //end of execute() method

} //end of class
