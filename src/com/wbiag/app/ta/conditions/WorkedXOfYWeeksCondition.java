package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 *  Title:          WorkedXOfYWeeksCondition
 *  Description:    A condition to check check if the employee worked the same day on X of the last Y weeks.  The day checked is the current work day
 */
public class WorkedXOfYWeeksCondition extends Condition
{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WorkedXOfYWeeksCondition.class);

    public final static String PARAM_MIN_WEEKS_MUST_WORKED = "MinWeeksMustWorked";
    public final static String PARAM_WEEKS_TO_LOOK_BACK = "WeeksToLookBack";
    public final static String PARAM_MIN_WORKED_MINUTES = "MinWorkedMinutes";
    public final static String PARAM_TCODENAME_LIST = "TcodeNameList";
    public final static String PARAM_TCODE_INCLUSIVE = "TcodeInclusive";
    public final static String PARAM_HTYPENAME_LIST = "HtypeNameList";
    public final static String PARAM_HTYPE_INCLUSIVE = "HtypeInclusive";
    public final static String PARAM_DETAIL_PREMIUM = "DetailPremium";


    public boolean evaluate(WBData wbData, Parameters parameters)
        throws Exception     {

        int wksMstWorked = parameters.getIntegerParameter(PARAM_MIN_WEEKS_MUST_WORKED);
        int wksToLookBack = parameters.getIntegerParameter(PARAM_WEEKS_TO_LOOK_BACK);
        if (wksMstWorked > wksToLookBack) {
            throw new RuntimeException("wksMstWorked must not be bigger than wksToLookBack");
        }
        int minMins = parameters.getIntegerParameter(PARAM_MIN_WORKED_MINUTES);
        String tcodeNameList = parameters.getParameter(PARAM_TCODENAME_LIST,null);
        boolean tcodeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TCODE_INCLUSIVE,"true")).booleanValue();
        String htypeNameList = parameters.getParameter(PARAM_HTYPENAME_LIST,null);
        boolean htypeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HTYPE_INCLUSIVE,"true")).booleanValue();
        String detailPremium = parameters.getParameter(PARAM_DETAIL_PREMIUM,null);

        Date wrksDay = wbData.getWrksWorkDate();
        Date startDate = DateHelper.addDays(wrksDay , -1*7*wksToLookBack);
        Date endDate = DateHelper.addDays(wrksDay , -1*7);
        boolean ret = false;
        int cnt = 0;
        for (Date date = startDate; date.compareTo(endDate) <= 0;
             date = DateHelper.addDays(date, 7)) {
            int mins = wbData.getMinutesWorkDetailPremiumRange(date , date , null, null,
                tcodeNameList, tcodeInclusive, htypeNameList , htypeInclusive,
                detailPremium);
            if (logger.isDebugEnabled()) logger.debug("Worked : " + mins + " on :" + date);
            if (mins >= minMins) {
                if (++cnt >= wksMstWorked) {
                    ret = true;
                    break;
                }
            }
        }

        return ret;
    }

    public List getParameterInfo( DBConnection conn )
    {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_MIN_WEEKS_MUST_WORKED, RuleParameterInfo.INT_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_WEEKS_TO_LOOK_BACK, RuleParameterInfo.INT_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_MIN_WORKED_MINUTES, RuleParameterInfo.INT_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_TCODENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TCODE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DETAIL_PREMIUM, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    public String getComponentName()
    {
        return "WBIAG: Worked X of Y Weeks Condition";
    }
}
