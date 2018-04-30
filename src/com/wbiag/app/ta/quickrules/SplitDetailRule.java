package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.DBConnection;
import java.util.*;


import com.workbrain.util.*;
/**
 * Split detail rule for split week solution
 *
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class SplitDetailRule extends Rule {
    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SplitDetailRule.class);

    public final static String PARAM_SPLIT_TIME = "SplitTime";
    public final static String PARAM_CHANGES_WRKD_WORK_DATE = "ChangesWrkdWorkDate";

    private final String SPLIT_TIME_FORMAT = "yyyyMMdd HHmmss";
    private final String SPLIT_TIME_DEFAULT = "19000101 000000";

    public List getParameterInfo(DBConnection parm1) {
        List result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_SPLIT_TIME, RuleParameterInfo.STRING_TYPE, false));
        //result.add(new RuleParameterInfo(PARAM_CHANGES_WRKD_WORK_DATE, RuleParameterInfo.STRING_TYPE, true));
        RuleParameterInfo rpiChangesWrkdWorkDate = new RuleParameterInfo(PARAM_CHANGES_WRKD_WORK_DATE, RuleParameterInfo.CHOICE_TYPE, true);
        rpiChangesWrkdWorkDate.addChoice("true");
        rpiChangesWrkdWorkDate.addChoice("false");
        result.add(rpiChangesWrkdWorkDate);
        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {
        // *** Retrieve parameters
        String sSplitTime = parameters.getParameter(PARAM_SPLIT_TIME, SPLIT_TIME_DEFAULT);
        Date splitTime = null;
        try {
            splitTime = DateHelper.parseDate(sSplitTime , SPLIT_TIME_FORMAT);
        } catch (IllegalArgumentException e) {
            throw new Exception("Split Details Rule SplitTime parameter " + sSplitTime + " has invalid date format. Correct format is yyyyMMdd HHmmss");
        }
        boolean changesWrkdWorkDate =
            Boolean.valueOf(parameters.getParameter(PARAM_CHANGES_WRKD_WORK_DATE, "true")).booleanValue();
        doSplit(wbData , splitTime , changesWrkdWorkDate);
    }

    protected void doSplit(WBData data, Date splitTime , boolean changesWrkdWorkDate) {
        WorkDetailList wdl = data.getRuleData().getWorkDetails();
        Date wrksWorkDate = DateHelper.truncateToDays(data.getWrksWorkDate());
        Date splitDatetimeYesterday = null, splitDatetimeToday = null, splitDatetimeTomorrow = null;
        if (wdl.size() > 0) {
            splitDatetimeYesterday = DateHelper.setTimeValues(DateHelper.addDays(wrksWorkDate , -1) , splitTime);
            doSplit(wdl , splitDatetimeYesterday, changesWrkdWorkDate);
            splitDatetimeToday = DateHelper.setTimeValues(wrksWorkDate  , splitTime);
            doSplit(wdl , splitDatetimeToday, changesWrkdWorkDate);
            splitDatetimeTomorrow = DateHelper.setTimeValues(DateHelper.addDays(wrksWorkDate , 1) , splitTime);
            doSplit(wdl , splitDatetimeTomorrow , changesWrkdWorkDate);

            if (changesWrkdWorkDate) {
                changeDate(wdl, splitDatetimeYesterday, splitDatetimeToday,
                           splitDatetimeTomorrow, wrksWorkDate, splitTime);
            }

        }

    }

    /**
     *
     * @param wdl WorkDetailList
     * @param splitDateTime splitDateTime
     * @param changesWrkdWorkDate Whether wrkdWorkDate will be updated with split date
     */
    protected void doSplit(WorkDetailList wdl , Date splitDateTime ,
                           boolean changesWrkdWorkDate) {
        wdl.splitAt(splitDateTime);

    }

    protected void changeDate(WorkDetailList wdl ,
                              Date splitDatetimeYesterday,
                              Date splitDatetimeToday,
                              Date splitDatetimeTomorrow,
                              Date workDate,
                              Date splitTime) {
        if (splitDatetimeToday == null) {
            return;
        }
        long noonInMillisSinceMidnight = DateHelper.getDateSetToNoon().getTime()
            - DateHelper.truncateToDays(DateHelper.getDateSetToNoon()).getTime();
        if (DateHelper.getDayFraction(splitDatetimeToday) <= noonInMillisSinceMidnight ) {
            splitDatetimeYesterday = DateHelper.setTimeValues(DateHelper.addDays(DateHelper.truncateToDays(splitDatetimeYesterday),1), splitTime);
            splitDatetimeToday = DateHelper.setTimeValues(DateHelper.addDays(DateHelper.truncateToDays(splitDatetimeToday),1), splitTime);
            splitDatetimeTomorrow = DateHelper.setTimeValues(DateHelper.addDays(DateHelper.truncateToDays(splitDatetimeTomorrow),1), splitTime);

        }

        Iterator iter = wdl.iterator();
        Date dayBeforeWorkDate = DateHelper.addDays(workDate , -1);
        Date dayAfterWorkDate = DateHelper.addDays(workDate , 1);

        while (iter.hasNext()) {
            WorkDetailData item = (WorkDetailData) iter.next();
            if (item.getWrkdStartTime().compareTo(splitDatetimeToday) < 0
                && item.getWrkdStartTime().compareTo(splitDatetimeYesterday) >=0 ) {
                item.setWrkdWorkDate(workDate);
            }
            else if (item.getWrkdStartTime().compareTo(splitDatetimeYesterday) <0) {
                item.setWrkdWorkDate(dayBeforeWorkDate );
            }
            else if (item.getWrkdStartTime().compareTo(splitDatetimeToday) >=  0) {
                item.setWrkdWorkDate(dayAfterWorkDate );
            }
        }
    }

    public String getComponentName() {
        return "WBIAG: Split Details Rule";
    }

    public String getComponentUI() {
        return null;
    }

}
