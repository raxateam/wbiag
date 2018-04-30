package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.quickrules.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
/**
 * DailyOvertimeRule that applies hourset to premium codes defined in PARAM_PREMIUM_TIMECODES_COUNTED
 */
public class DailyOvertimePremiumRule extends DailyOvertimeRule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DailyOvertimePremiumRule.class);

    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {
        String premiumTimeCodesAppliedTo = parameters.getParameter("PremiumTimeCodesCounted", null);
        if (premiumTimeCodesAppliedTo == null) {
            super.execute(wbData , parameters);
        }
        else {
            try {
                changePremiums(wbData , premiumTimeCodesAppliedTo);
                super.execute(wbData, parameters);
            }
            finally {
                resetPremiums(wbData);
            }
        }
    }

    protected void changePremiums(WBData wbData , String premiumTimeCodesAppliedTo) {
        for (int i = wbData.getRuleData().getWorkPremiums().size() - 1;
             i >= 0; i--) {
            WorkDetailData curWorkPrem = wbData.getRuleData().
                getWorkPremium(i).duplicate();
            if (RuleHelper.isCodeInList(premiumTimeCodesAppliedTo,
                                        curWorkPrem.getWrkdTcodeName())) {
                Date max = wbData.getRuleData().getWorkDetails().
                    getMaxEndDate();
                if (max == null) {
                    max = wbData.getWrksWorkDate();
                }
                curWorkPrem.setWrkdStartTime(max);
                curWorkPrem.setWrkdEndTime(DateHelper.addMinutes(max,
                    curWorkPrem.getWrkdMinutes()));
                curWorkPrem.setWrkdType(WorkDetailData.DETAIL_TYPE);
                curWorkPrem.setWrkdOverridden("Y");
                wbData.getRuleData().getWorkDetails().add(curWorkPrem);
                wbData.getRuleData().getWorkPremiums().remove(i);
            }
        }
    }

    protected void resetPremiums(WBData wbData) {
        for (int i = wbData.getRuleData().getWorkDetails().size() -1 ;
             i >= 0; i--) {
            WorkDetailData curWorkDet = wbData.getRuleData().
                getWorkDetail(i);
            if ("Y".equals(curWorkDet.getWrkdOverridden())) {
                curWorkDet = curWorkDet.duplicate();
                curWorkDet.setWrkdType(WorkDetailData.PREMIUM_TYPE);
                curWorkDet.setWrkdStartTime(DateHelper.DATE_1900);
                curWorkDet.setWrkdEndTime(DateHelper.DATE_1900);
                curWorkDet.setWrkdOverridden(null);
                wbData.getRuleData().getWorkPremiums().add(curWorkDet);
                wbData.getRuleData().getWorkDetails().remove(i);
            }
        }
    }

    public String getComponentName() {
        return "WBIAG: Daily Overtime Premium Rule";
    }

}