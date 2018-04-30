package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

public class FairLaborDistributionRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FairLaborDistributionRule.class);

    public static final String PARAM_TIME_CODES = "TimeCodes";
    public static final String PARAM_TIME_CODES_INCLUSIVE = "TimeCodesInclusive";
    public static final String PARAM_HOUR_TYPES = "HourTypes";
    public static final String PARAM_HOUR_TYPES_INCLUSIVE = "HourTypesInclusive";


    public List getParameterInfo(DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_TIME_CODES,
                                         RuleParameterInfo.STRING_TYPE, true));
        RuleParameterInfo timeCodesInclusive = new RuleParameterInfo(
            PARAM_TIME_CODES_INCLUSIVE, RuleParameterInfo.CHOICE_TYPE, true);
        timeCodesInclusive.addChoice("true");
        timeCodesInclusive.addChoice("false");
        result.add(timeCodesInclusive);
        result.add(new RuleParameterInfo(PARAM_HOUR_TYPES,
                                         RuleParameterInfo.STRING_TYPE, true));
        RuleParameterInfo hourTypesInclusive = new RuleParameterInfo(
            PARAM_HOUR_TYPES_INCLUSIVE, RuleParameterInfo.CHOICE_TYPE, true);
        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {

        WorkDetailList wdl = wbData.getRuleData().getWorkDetails();

        String tcodes =  parameters.getParameter(PARAM_TIME_CODES , null);
        boolean tcodesInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TIME_CODES_INCLUSIVE, "true")).booleanValue();
        String htypes =  parameters.getParameter(PARAM_HOUR_TYPES , null);
        boolean htypesInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HOUR_TYPES_INCLUSIVE, "true")).booleanValue();
        // *** keep only eligible details
        wbData.pluck(null, null, tcodesInclusive, tcodes, htypesInclusive, htypes);
        if (wdl.size() == 0) {
            if (logger.isDebugEnabled()) logger.debug("No work details to process in : FairLaborDistributionRule, exiting");
            wbData.unpluck();
            return;
        }
        int totalMins = wbData.getMinutesWorkDetail(null, null,
            tcodes, tcodesInclusive,
            htypes, htypesInclusive, null);
        if (logger.isDebugEnabled()) logger.debug("Total eligible mins :" + totalMins);
        List edlabs = wbData.getRuleData().getEmpDefaultLabor();
        int totalPct = getTotalEdlabPercentage(edlabs);
        int minutesAllocated = 0;
        Date start = wdl.getWorkDetail(0).getWrkdStartTime();
        Date end = null;
        for (int i=0, k=edlabs.size() ; i < k ; i++) {
            EmployeeDefaultLaborData edlab = (EmployeeDefaultLaborData)edlabs.get(i);
            double d1 = (double) edlab.getEdlaPercentage();
            double d2 = (double) totalPct;
            int detailLen;
            if (i != edlabs.size() - 1){
                detailLen = (int) ((d1 / d2) * totalMins);
            }
            else {
                detailLen = (int)(totalMins) - minutesAllocated;
            }
            end = DateHelper.addMinutes (start , detailLen < 0 ? 0 : detailLen);
            if (logger.isDebugEnabled()) logger.debug("Setting proj:" + edlab.getProjId() + " from:" + start + " to:" + end);
            wbData.setWorkDetailProjId(String.valueOf(edlab.getProjId()) , start, end);
            minutesAllocated += detailLen;
            start = end;
        }
        wbData.unpluck();
    }

    private int getTotalEdlabPercentage(List edlabs) {
        int ret = 0;
        Iterator iter = edlabs.iterator();
        while (iter.hasNext()) {
            EmployeeDefaultLaborData item = (EmployeeDefaultLaborData)iter.next();
            ret += item.getEdlaPercentage();
        }

        return ret;
    }


    public String getComponentName() {
        return "WBIAG: Fair labor Distribution Rule";
    }

    public String getComponentUI() {
        return null;
    }

}
