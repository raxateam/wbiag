package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 *  Title:          PremiumFromWorkDetailUDFRule
 *  Description:    This rule will accumulate the values in a work detail UDF.
 *                  as a premium.
 */
public class PremiumFromWorkDetailUDFRule extends Rule
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PremiumFromWorkDetailUDFRule.class);

    public final static String PARAM_PREMIUM_TIME_CODE = "PremiumTimeCode";
    public final static String PARAM_PREMIUM_HOUR_TYPE = "PremiumHourType";
    public final static String PARAM_WORK_DETAIL_UDF = "WorkDetailUdf";

    public List getParameterInfo (DBConnection conn)
    {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIME_CODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_HOUR_TYPE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_WORK_DETAIL_UDF, RuleParameterInfo.STRING_TYPE, false));
        return result;
    }

    public void execute(WBData wbData, Parameters parameters)
        throws Exception
    {
        int premiumMinutes = 0;

        String premiumTimeCode = parameters.getParameter(PARAM_PREMIUM_TIME_CODE);
        String premiumHourType = parameters.getParameter(PARAM_PREMIUM_HOUR_TYPE, null);
        String wrkdUdf = parameters.getParameter(PARAM_WORK_DETAIL_UDF);

        // If a premium code is supplied, add the premium.
        if (!StringHelper.isEmpty(premiumTimeCode))
        {
            if (StringHelper.isEmpty(premiumHourType))
            {
                premiumHourType = wbData.getCodeMapper().getHourTypeById(
                    wbData.getCodeMapper().getTimeCodeByName(premiumTimeCode).
                    getHtypeId()).getHtypeName();
            }
            premiumMinutes = getUdfMins(wbData.getRuleData().getWorkDetails() , wrkdUdf);
            if (premiumMinutes > 0) { 
            	if (logger.isDebugEnabled()) logger.debug("Added premium for udfs :" + premiumMinutes);
            	wbData.insertWorkPremiumRecord(premiumMinutes, premiumTimeCode, premiumHourType);
            }
        }
    }

    private int getUdfMins(WorkDetailList wdl, String fldName) {
        int sum = 0;
        Iterator iter = wdl.iterator();
        while (iter.hasNext()) {
            WorkDetailData item = (WorkDetailData)iter.next();
            try {
                String val = (String) item.getField(fldName);
                if (!StringHelper.isEmpty(val)) {
                    sum += Integer.parseInt(val);
                }
            }
            catch (Exception ex) {
                throw new NestedRuntimeException("Error in parsing : " + fldName, ex);
            }
        }
        return sum;
    }

    public String getComponentName()
    {
        return "WBIAG: Premium From Work Detail UDF Rule";
    }
}
