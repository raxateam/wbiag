package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Rounding rule that works based on whole duration and gives +- premium if any rounding is done.
 */
public class RoundingDurationRule extends Rule {

    public final static String PARAM_ELIGIBLE_TIMECODES = "EligibleTimeCodes";
    public final static String PARAM_ELIGIBLE_HOURTYPES = "EligibleHourTypes";
    public final static String PARAM_MULTIPLE = "Multiple";
    public final static String PARAM_SPLIT = "Split";
    public final static String PARAM_PREMIUM_TIMECODE = "PremiumTimeCode";
    public final static String PARAM_PREMIUM_HOURTYPE = "PremiumHourType";

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RoundingDurationRule.class);

    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_TIMECODES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_HOURTYPES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_MULTIPLE, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_SPLIT, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODE, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_HOURTYPE, RuleParameterInfo.STRING_TYPE));

        return result;
    }


    public void execute(WBData wbData, Parameters parameters) throws Exception {
        // *** Parameters ***
        int mult = parameters.getIntegerParameter(PARAM_MULTIPLE, 0);
        int split = parameters.getIntegerParameter(PARAM_SPLIT, 0);
        String eligibleCodes = parameters.getParameter(PARAM_ELIGIBLE_TIMECODES, "WRK");
        String eligibleHourTypes = parameters.getParameter(PARAM_ELIGIBLE_HOURTYPES);
        String premCode = parameters.getParameter(PARAM_PREMIUM_TIMECODE);
        String premHourType = parameters.getParameter(PARAM_PREMIUM_HOURTYPE);

        if (mult == 0 || split ==0) {
            throw new RuntimeException ("Multiple and/or split must be defined");
        }
        // *** check if default hour type is required
        if (StringHelper.isEmpty(premHourType)
            && !StringHelper.isEmpty(premCode)) {
            TimeCodeData premCodeData =
                wbData.getRuleData().getCodeMapper().getTimeCodeByName(premCode);
            if (premCodeData == null) {
                throw new RuntimeException ("Premium time code not found : " + premCode);
            }
            else {
                premHourType = wbData.getRuleData().getCodeMapper().
                    getHourTypeById(premCodeData.getHtypeId()).getHtypeName();
            }
        }

        int duration = wbData.getMinutesWorkDetail(
            eligibleCodes , true, eligibleHourTypes , true);

        int rndDuration = roundDuration(duration , mult, split);

        int premMins = rndDuration - duration;

        if (Math.abs(premMins) != 0) {
            if (!StringHelper.isEmpty(premCode)) {
                wbData.insertWorkPremiumRecord(premMins, premCode, premHourType);
            }
            else {
                createPremiumBasedOnDetail(wbData ,premMins, eligibleCodes,
                                           eligibleHourTypes ,
                                           premHourType);
            }
        }
    }

    protected void createPremiumBasedOnDetail(WBData wbData,
                                              int premMins ,
                                              String eligibleCodes,
                                              String eligibleHourTypes,
                                              String premHourType) {
        WorkDetailList details = wbData.getRuleData().getWorkDetails();
        int lastInd = details.getLastRecordIndex(
            eligibleCodes , true,
            eligibleHourTypes , true);
        if (lastInd == -1) {
            return;
        }
        if (premMins > 0) {
            WorkDetailData wd = details.getWorkDetail(lastInd);
            wbData.insertWorkPremiumRecord(premMins,
                                           wd.getWrkdTcodeName(),
                                           StringHelper.isEmpty(premHourType) ?
                                           wd.getWrkdHtypeName() : premHourType);
        }
        else {
            // *** consume minutes starting from last eligible detail
            List premCodes = new ArrayList();
            int leftMins = Math.abs(premMins);
            int ind = lastInd;
            while (leftMins > 0 && ind != -1) {
                WorkDetailData wd = details.getWorkDetail(ind);
                if (RuleHelper.isCodeInList(eligibleCodes, wd.getWrkdTcodeName())
                    && RuleHelper.isCodeInList(eligibleHourTypes, wd.getWrkdHtypeName())) {
                    int thisPremMins = Math.min(wd.getWrkdMinutes() , leftMins);
                    leftMins -= thisPremMins;
                    PremiumCodeHType pch = new PremiumCodeHType(thisPremMins,
                        wd.getWrkdTcodeName(),
                        StringHelper.isEmpty(premHourType) ?
                        wd.getWrkdHtypeName()
                           : premHourType);
                    int pInd = premCodes.indexOf(pch);
                    if (pInd >= 0) {
                        PremiumCodeHType ePch = (PremiumCodeHType)premCodes.get(pInd);
                        ePch.mins += thisPremMins;
                    }
                    else {
                        premCodes.add(pch);
                    }
                }
                ind--;
            }
            if (premCodes.size() > 0) {
                Iterator iter = premCodes.iterator();
                while (iter.hasNext()) {
                    PremiumCodeHType item = (PremiumCodeHType)iter.next();
                    wbData.insertWorkPremiumRecord(-1 * item.mins ,
                                                   item.tcode,
                                                   item.htype);
                }
            }
        }

    }

    private class PremiumCodeHType {
        int mins;
        String tcode;
        String htype;

        public PremiumCodeHType(int mins, String tcode, String htype) {
            this.mins = mins;
            this.tcode = tcode;
            this.htype = htype;
        }

        public boolean equals(Object o) {
            PremiumCodeHType pcd = (PremiumCodeHType) o;
            return mins == pcd.mins
                && StringHelper.equals(tcode, pcd.tcode)
                && StringHelper.equals(htype, pcd.htype);
        }

        public int hashCode() {
            return mins
                + (tcode != null ? tcode.hashCode() : 0)
                + (htype != null ? htype.hashCode() : 0);
        }

    }

    /**
     * Rounds the given duration based on given multiple and split.
     *
     * @param durationMins duration in minutes
     * @param multiple multiple
     * @param split split
     * @return rounded duration in minutes
     */
    protected int roundDuration(int durationMins,
                       int multiple,
                       int split) {

        if (multiple <= 0 || split < 0) {
            return 0;
        }
        int lowerBound, retDuration;

        lowerBound = (durationMins / multiple) * multiple;

        if (split==0) {
            retDuration = lowerBound;
        } else if (multiple==split && durationMins!=lowerBound) {
            retDuration = lowerBound + multiple ;
        } else if (durationMins <= (lowerBound + split)) {
            retDuration = lowerBound ;
        } else {
            retDuration = lowerBound + multiple ;
        }
        return retDuration;
    }

    public String getComponentName() {
        return "WBIAG: Rounding Duration Rule";
    }
}
