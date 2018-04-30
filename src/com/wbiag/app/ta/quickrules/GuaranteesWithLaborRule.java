package com.wbiag.app.ta.quickrules;

// standard imports for rules
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
// extra imports needed for this rule
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
import com.workbrain.sql.*;
import java.util.*;

public class GuaranteesWithLaborRule extends Rule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GuaranteesWithLaborRule.class);

    public final static String PARAM_GUARANTEED_PREMIUM_TIMECODE = "GuaranteedPremiumTimeCode";
    public final static String PARAM_GUARANTEED_PREMIUM_HOURTYPE = "GuaranteedPremiumHourType";
    public final static String PARAM_TIMECODELIST = "TimeCodeList";
    public final static String PARAM_HOURTYPELIST = "HourTypeList";
    public final static String PARAM_MINIMUM_MINUTES_TO_QUALIFY = "MinimumMinutesToQualify";
    public final static String PARAM_MAXIMUM_MINUTES_TO_QUALIFY = "MaximumMinutesToQualify";
    public final static String PARAM_GUARANTEED_MINUTES = "GuaranteedMinutes";
    public final static String PARAM_USE_EFFECTIVE_MINUTES = "UseEffectiveMinutes";
    public final static String PARAM_INDEPENDENT_OCCURRENCES = "IndependentOccurrences";
    public final static String PARAM_DAY_WEEK_STARTS = "DayWeekStarts";
    public final static String PARAM_APPLY_WEEK_WRKS_FLAG_FIELD = "ApplyWeekWrksFlagField";
    public final static String PARAM_PREMIUM_LABOR_METHOD = "MethodLaborMetric";

    public List getParameterInfo (DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_GUARANTEED_PREMIUM_TIMECODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_GUARANTEED_PREMIUM_HOURTYPE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TIMECODELIST, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_HOURTYPELIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_MINIMUM_MINUTES_TO_QUALIFY, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_MAXIMUM_MINUTES_TO_QUALIFY, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_GUARANTEED_MINUTES, RuleParameterInfo.STRING_TYPE, false));

        RuleParameterInfo rpiUseEffectiveMinutesChoice = new RuleParameterInfo(PARAM_USE_EFFECTIVE_MINUTES, RuleParameterInfo.CHOICE_TYPE, true);
        rpiUseEffectiveMinutesChoice.addChoice("true");
        rpiUseEffectiveMinutesChoice.addChoice("false");
        result.add(rpiUseEffectiveMinutesChoice);

        RuleParameterInfo rpiIndependentOccurrencesChoice = new RuleParameterInfo(PARAM_INDEPENDENT_OCCURRENCES, RuleParameterInfo.CHOICE_TYPE, true);
        rpiIndependentOccurrencesChoice.addChoice("true");
        rpiIndependentOccurrencesChoice.addChoice("false");
        result.add(rpiIndependentOccurrencesChoice);

        RuleParameterInfo rpiDayWeekStartsChoice = new RuleParameterInfo(PARAM_DAY_WEEK_STARTS, RuleParameterInfo.CHOICE_TYPE, true);
        rpiDayWeekStartsChoice.addChoice("");
        rpiDayWeekStartsChoice.addChoice("Sunday");
        rpiDayWeekStartsChoice.addChoice("Monday");
        rpiDayWeekStartsChoice.addChoice("Tuesday");
        rpiDayWeekStartsChoice.addChoice("Wednesday");
        rpiDayWeekStartsChoice.addChoice("Thursday");
        rpiDayWeekStartsChoice.addChoice("Friday");
        rpiDayWeekStartsChoice.addChoice("Saturday");
        result.add(rpiDayWeekStartsChoice);
        result.add(new RuleParameterInfo(PARAM_APPLY_WEEK_WRKS_FLAG_FIELD, RuleParameterInfo.STRING_TYPE, true));
        RuleParameterInfo rpiLaborMthd = new RuleParameterInfo(
            PARAM_PREMIUM_LABOR_METHOD, RuleParameterInfo.CHOICE_TYPE);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_DEFAULT_METHOD);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_FIRST_METHOD);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_LAST_METHOD);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_HOUR_FOR_HOUR_METHOD);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_PRORATED_METHOD);
        result.add(rpiLaborMthd);

        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {

        RuleData ruleData = wbData.getRuleData();
        CodeMapper codeMapper = ruleData.getCodeMapper();

        // Retrieve parameters
        String guaranteedPremiumTimeCode = parameters.getParameter(PARAM_GUARANTEED_PREMIUM_TIMECODE);
        String guaranteedPremiumHourType = parameters.getParameter(PARAM_GUARANTEED_PREMIUM_HOURTYPE, codeMapper.getHourTypeById(codeMapper.getTimeCodeByName(guaranteedPremiumTimeCode).getHtypeId()).getHtypeName());
        String timeCodeList = parameters.getParameter(PARAM_TIMECODELIST);
        String hourTypeList = parameters.getParameter(PARAM_HOURTYPELIST, null);
        int minimumMinutesToQualify = parameters.getIntegerParameter(PARAM_MINIMUM_MINUTES_TO_QUALIFY, 0);
        int maximumMinutesToQualify = parameters.getIntegerParameter(PARAM_MAXIMUM_MINUTES_TO_QUALIFY, Integer.MAX_VALUE);
        maximumMinutesToQualify = (maximumMinutesToQualify == 0) ? Integer.MAX_VALUE : maximumMinutesToQualify;
        int guaranteedMinutes = 0;
        String guaranteedMinutesString = parameters.getParameter(PARAM_GUARANTEED_MINUTES);
        if (guaranteedMinutesString.equalsIgnoreCase("S")) {
            guaranteedMinutes = wbData.getScheduleDuration();
        } else if (!StringHelper.isEmpty(guaranteedMinutesString)) {
            guaranteedMinutes = Integer.parseInt(guaranteedMinutesString);
        }
        boolean useEffectiveMinutes = Boolean.valueOf(parameters.getParameter(PARAM_USE_EFFECTIVE_MINUTES, "false")).booleanValue();
        boolean independentOccurrences = Boolean.valueOf(parameters.getParameter(PARAM_INDEPENDENT_OCCURRENCES, "true")).booleanValue();
        String dayWeekStarts = parameters.getParameter(PARAM_DAY_WEEK_STARTS, "");
        String applyWeekWrksFlagField = parameters.getParameter(PARAM_APPLY_WEEK_WRKS_FLAG_FIELD, "");
        String methodLaborMetric = parameters.getParameter(
            PARAM_PREMIUM_LABOR_METHOD, PremiumWithLabor.PARAM_PLM_VAL_DEFAULT_METHOD );
        if (independentOccurrences) {
            if (PremiumWithLabor.PARAM_PLM_VAL_PRORATED_METHOD.equals(methodLaborMetric)) {
                throw new RuleEngineException ("MethodLaborMetric " + PremiumWithLabor.PARAM_PLM_VAL_PRORATED_METHOD + " not supported when independentOccurrences=true");
            }
        }
        else {
            if (PremiumWithLabor.PARAM_PLM_VAL_HOUR_FOR_HOUR_METHOD.equals(methodLaborMetric)) {
               throw new RuleEngineException ("MethodLaborMetric " + PremiumWithLabor.PARAM_PLM_VAL_HOUR_FOR_HOUR_METHOD + " not supported when independentOccurrences=false");
           }
        }
        PremiumWithLabor pwl = new PremiumWithLabor();
        java.util.Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();

        int actualMinutesWorked = 0;
        int effectiveMinutesWorked = 0;

        // *** weekly range
        if (!StringHelper.isEmpty(dayWeekStarts)) {
            boolean bolAppyWeek = true;
            // *** only apply if flag is checked
            if (!StringHelper.isEmpty(applyWeekWrksFlagField)) {
                Object wrksFlagField = wbData.getRuleData().getWorkSummary().getField(applyWeekWrksFlagField);
                bolAppyWeek = (wrksFlagField != null) ? wrksFlagField.toString().equals("Y") : false;
            }
            if (bolAppyWeek) {
                java.util.Date dateWeekStarts = DateHelper.nextDay(DateHelper.addDays(workSummaryDate, -7), dayWeekStarts);
                actualMinutesWorked = wbData.getMinutesWorkDetailRange(
                        dateWeekStarts,
                        wbData.getRuleData().getWorkSummary().getWrksWorkDate(),
                        null,   null,
                        timeCodeList , true,  hourTypeList, true);
                effectiveMinutesWorked =
                            useEffectiveMinutes
                            ? wbData.getMinutesWorkDetailRangeEffective(
                                      dateWeekStarts,
                                      wbData.getRuleData().getWorkSummary().getWrksWorkDate(),
                                      null,   null,
                                      timeCodeList , true,  hourTypeList, true, useEffectiveMinutes)
                            : actualMinutesWorked;
                if ( (actualMinutesWorked > 0) &&
                     (actualMinutesWorked >= minimumMinutesToQualify) &&
                     (actualMinutesWorked <= maximumMinutesToQualify) ) {
                    if ((guaranteedMinutes - effectiveMinutesWorked) > 0) {
                        //wbData.insertWorkPremiumRecord(guaranteedMinutes - effectiveMinutesWorked,
                        //        guaranteedPremiumTimeCode,
                        //        guaranteedPremiumHourType);
                        pwl.addWrkdDates(DateHelper.DATE_1900 , DateHelper.DATE_1900,
                                     guaranteedMinutes - effectiveMinutesWorked,
                                     null);
                    }
                }
            }
        // *** daily range
        } else {
            // loop through all the work details looking for the sought after
            // GuaranteedPremiumTimeCode / GuarenteedPremiumHourType combination
            for (int i = 0, j = ruleData.getWorkDetailCount(); i < j; i++) {

                WorkDetailData workDetailData = ruleData.getWorkDetail(i);

                // We are only interested in this work detail if it has a time code in
                // in timeCodeList or an hour type in hourTypeList.
                if (RuleHelper.isCodeInList(timeCodeList,workDetailData.getWrkdTcodeName()) &&
                    ((hourTypeList == null) || (RuleHelper.isCodeInList(hourTypeList,workDetailData.getWrkdHtypeName())))) {

                    // All worked minutes will be multiplied by effectiveMinutesMultiplier
                    // if they didn't set the EffectiveMinutesMultiplier our multiplier will be 1
                    // otherwise our multiplier uses the hour type multiple of the work detail's
                    // hour type (not of the guaranteedPremiumHourType's hour type)
                    double effectiveMinutesMultiplier = useEffectiveMinutes ? codeMapper.getHourTypeById(workDetailData.getHtypeId()).getHtypeMultiple() : 1;

                    int wrkdMins = (int) (workDetailData.getWrkdMinutes() * effectiveMinutesMultiplier);
                    effectiveMinutesWorked += wrkdMins;
                    actualMinutesWorked += workDetailData.getWrkdMinutes();

                    // Add a premium record if within the min and max minutes and reset the
                    // minutes worked unless we're not counting independent occurrences
                    // (in that case, just keep looping through the work details)
                    if (independentOccurrences) {
                        if ( (actualMinutesWorked > 0) &&
                             (actualMinutesWorked >= minimumMinutesToQualify) &&
                             (actualMinutesWorked <= maximumMinutesToQualify) ) {
                            if ((guaranteedMinutes - effectiveMinutesWorked) > 0) {
                                //wbData.insertWorkPremiumRecord(guaranteedMinutes - effectiveMinutesWorked,
                                //        guaranteedPremiumTimeCode,
                                //       guaranteedPremiumHourType);
                                pwl.addWrkdDates(DateHelper.DATE_1900 , DateHelper.DATE_1900,
                                             guaranteedMinutes - effectiveMinutesWorked,
                                             workDetailData);

                           }
                       }
                       effectiveMinutesWorked = 0;
                       actualMinutesWorked = 0;
                    }
                    else {
                        pwl.addWrkdDates(DateHelper.DATE_1900 , DateHelper.DATE_1900,
                                     wrkdMins ,
                                     workDetailData);

                    }
                }
            }

            // If we weren't checking independent occurances once we've finished
            // looping through the work details we need to add the premium record
            // for the entire day's guarenteed minutes owed
            if (!independentOccurrences && pwl.tabWrkdDates.size() > 0) {
                if ( (actualMinutesWorked > 0) &&
                     (actualMinutesWorked >= minimumMinutesToQualify) &&
                     (actualMinutesWorked <= maximumMinutesToQualify) ) {
                    if ((guaranteedMinutes - effectiveMinutesWorked) > 0) {
                        //wbData.insertWorkPremiumRecord(guaranteedMinutes - effectiveMinutesWorked,
                        //        guaranteedPremiumTimeCode,
                        //        guaranteedPremiumHourType);
                        if (PremiumWithLabor.PARAM_PLM_VAL_FIRST_METHOD.equals(methodLaborMetric)) {
                            PremiumWithLabor.WrkdDates wrkd = (PremiumWithLabor.WrkdDates) pwl.tabWrkdDates.get(0);
                            wrkd.minutes = guaranteedMinutes - effectiveMinutesWorked;
                            pwl.tabWrkdDates.clear();
                            pwl.tabWrkdDates.add(wrkd);
                        }
                        else if (PremiumWithLabor.PARAM_PLM_VAL_LAST_METHOD.equals(methodLaborMetric)) {
                            PremiumWithLabor.WrkdDates wrkd = (PremiumWithLabor.WrkdDates) pwl.tabWrkdDates.get(pwl.tabWrkdDates.size() - 1);
                            wrkd.minutes = guaranteedMinutes - effectiveMinutesWorked;
                            pwl.tabWrkdDates.clear();
                            pwl.tabWrkdDates.add(wrkd);
                        }
                        else if (PremiumWithLabor.PARAM_PLM_VAL_DEFAULT_METHOD.equals(methodLaborMetric)) {
                            PremiumWithLabor.WrkdDates wrkd = (PremiumWithLabor.WrkdDates)pwl.tabWrkdDates.get(0);
                            wrkd.minutes = guaranteedMinutes - effectiveMinutesWorked;
                            wrkd.wd = null;
                            pwl.tabWrkdDates.clear();
                            pwl.tabWrkdDates.add(wrkd);
                        }
                        else if (PremiumWithLabor.PARAM_PLM_VAL_PRORATED_METHOD.equals(methodLaborMetric)) {
                            int guarMins = guaranteedMinutes - effectiveMinutesWorked;
                            int totMins = 0;
                            for (int i=0, k=pwl.tabWrkdDates.size() ; i < k ; i++) {
                                PremiumWithLabor.WrkdDates item = (PremiumWithLabor.WrkdDates)pwl.tabWrkdDates.get(i);
                                // watch out for rounding problems
                                if (i == k - 1) {
                                    item.minutes = guarMins - totMins;
                                }
                                else {
                                    double d1 = (double) item.minutes /
                                        effectiveMinutesWorked;
                                    double mins = d1 * guarMins;
                                    item.minutes = (int) mins;
                                    totMins += item.minutes;
                                }

                            }
                        }
                    }
                    else {
                        pwl.tabWrkdDates.clear();
                    }

                }
            }
        }

        pwl.insertWorkPremiumRecords(methodLaborMetric, wbData ,
                                 wbData.getCodeMapper().getTimeCodeByName(guaranteedPremiumTimeCode).getTcodeId() ,
                                 wbData.getCodeMapper().getHourTypeByName(guaranteedPremiumHourType).getHtypeId() ,
                                 pwl.getWrkdDatesMinutes(), 0, false, independentOccurrences);

    }

    public String getComponentName() {
        return "WBIAG: Guarantees With Labor Rule";
    }


    public boolean conditionSetExecutionIsMutuallyExclusive() {
      return true;
    }

}
