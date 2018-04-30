package com.wbiag.app.ta.quickrules;

import java.util.ArrayList;
import java.util.List;

import com.workbrain.app.ta.model.ShiftWithBreaks;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleHelper;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

/**
 *  Title:        Rounding Rule
 */
public class RoundingWorkPremiumsRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RoundingWorkPremiumsRule.class);

    public final static String PARAM_ELIGIBLE_TIMECODES = "EligibleTimeCodes";
    public final static String PARAM_MULTIPLE = "MultipleForMiddle";
    public final static String PARAM_SPLIT = "SplitForMiddle";
    public final static String PARAM_ELIGIBLE_HOURTYPES = "EligibleHourTypes";

    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_TIMECODES, RuleParameterInfo.STRING_TYPE,true));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_HOURTYPES, RuleParameterInfo.STRING_TYPE,true));
        result.add(new RuleParameterInfo(PARAM_MULTIPLE, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_SPLIT, RuleParameterInfo.INT_TYPE));

        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {

        // *** Parameters ***
        int mult = parameters.getIntegerParameter(PARAM_MULTIPLE, 0);
        int split = parameters.getIntegerParameter(PARAM_SPLIT, 0);
        String eligibleCodes = parameters.getParameter(PARAM_ELIGIBLE_TIMECODES);
        String eligibleHourTypes = parameters.getParameter(PARAM_ELIGIBLE_HOURTYPES);

        for (int i = 0; i < wbData.getRuleData().getWorkPremiumCount(); i++) {

            WorkDetailData theRecord = wbData.getRuleData().getWorkPremium(i);
            if (RuleHelper.isCodeInList(eligibleCodes, theRecord.getWrkdTcodeName())
                &&
                RuleHelper.isCodeInList(eligibleHourTypes, theRecord.getWrkdHtypeName())
                && theRecord.getWrkdMinutes() > 0 ) {
                theRecord.setWrkdMinutes(DateHelper.roundDuration(
                    theRecord.getWrkdMinutes(),
                    mult, split));
                if (logger.isDebugEnabled()) logger.debug("Rounded work premium :\n" + theRecord);
            }
        }

        // **** Remove Work Detail Records that have 0 or less work minutes
        for (int i = wbData.getRuleData().getWorkPremiumCount() - 1; i >= 0; i--) {
            if (wbData.getRuleData().getWorkPremium(i).getWrkdMinutes() <= 0) {
                wbData.getRuleData().getWorkPremiums().remove(i);
            }
        }
        // **** Sort The List
        wbData.getRuleData().getWorkPremiums().sort();
    }


    public String getComponentName() {
        return "WBIAG:Work Premium Rounding Rule";
    }

}
