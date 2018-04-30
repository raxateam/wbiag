package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import java.util.*;

/**
 *  Title:          IsNthDayOfPeriodCondition
 *  Description:    Checks to see if day worked is the given day of the period <br>
 *   The options for PARAM_APPLY_ON_UNIT are YEAR,QUARTER,MONTH,PAY PERIOD,WEEK.
 *   Values could be any number valid for the unit or FIRST and LAST constants <br>
 *   i.e wrksDate = 01/10/2006 PARAM_APPLY_ON_UNIT=YEAR, PARAM_APPLY_ON_VALUE=10 returns true <br>
 *   i.e wrksDate = 01/04/2006 PARAM_APPLY_ON_UNIT=QUARTER, PARAM_APPLY_ON_VALUE=FIRST returns true <br>
 */
public class IsNthDayOfPeriodCondition extends Condition
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsNthDayOfPeriodCondition.class);

    public static final String PARAM_APPLY_ON_UNIT = "ApplyOnUnit";
    public static final String PARAM_APPLY_ON_VALUE = "ApplyOnValue";

    public boolean evaluate(WBData wbData, Parameters parameters)
        throws Exception
    {
        boolean ret = false;
        Date wrksDate = wbData.getWrksWorkDate();

        String unit = parameters.getParameter(PARAM_APPLY_ON_UNIT);
        String val = parameters.getParameter(PARAM_APPLY_ON_VALUE);
        Date compare = null;
        if (DateHelper.APPLY_ON_UNIT_WEEK.equals(unit)) {
            compare = DateHelper.getUnitWeek(val, false, wrksDate);
        }
        else if (DateHelper.APPLY_ON_UNIT_PAYPERIOD.equals(unit)) {
            PayGroupData pg = wbData.getCodeMapper().getPayGroupById(wbData.getPaygrpId());
            compare = DateHelper.getUnitPayPeriod(val, false, wrksDate, pg);
        }
        else if (DateHelper.APPLY_ON_UNIT_MONTH.equals(unit)) {
            compare = DateHelper.getUnitMonth(val, false, wrksDate);
        }
        else if (DateHelper.APPLY_ON_UNIT_QTR.equals(unit)) {
            compare = DateHelper.getUnitQtr(val, false, wrksDate);
        }

        else if (DateHelper.APPLY_ON_UNIT_YEAR.equals(unit)) {
            compare = DateHelper.getUnitYear(val, false, wrksDate);
        }
        else {
            throw new RuleEngineException ("Unit not supported:" + unit);
        }

        if (logger.isDebugEnabled()) logger.debug("Comparing:" + compare + " with work date:" + wrksDate);
        ret = DateHelper.equals(compare, wrksDate);
        return ret;
    }

    public List getParameterInfo( DBConnection conn )
    {
        ArrayList result = new ArrayList();
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_APPLY_ON_UNIT, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(DateHelper.APPLY_ON_UNIT_WEEK);
        rpi.addChoice(DateHelper.APPLY_ON_UNIT_PAYPERIOD);
        rpi.addChoice(DateHelper.APPLY_ON_UNIT_MONTH);
        rpi.addChoice(DateHelper.APPLY_ON_UNIT_QTR);
        rpi.addChoice(DateHelper.APPLY_ON_UNIT_YEAR);
        result.add(rpi);

        result.add(new RuleParameterInfo(PARAM_APPLY_ON_VALUE,
                                         RuleParameterInfo.STRING_TYPE, false));
        return result;
    }

    public String getComponentName()
    {
        return "WBIAG: Is Nth Day Of Period Condition";
    }
}
