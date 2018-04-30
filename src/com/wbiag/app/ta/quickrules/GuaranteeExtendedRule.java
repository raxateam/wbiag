package com.wbiag.app.ta.quickrules;

import java.math.BigDecimal;
import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Extended guarantees rule
 */
public class GuaranteeExtendedRule extends Rule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GuaranteeExtendedRule.class);

    public final static String PARAM_GUARANTEED_PREMIUM_TIMECODE = "GuaranteedPremiumTimeCode";
    public final static String PARAM_GUARANTEED_PREMIUM_HOURTYPE = "GuaranteedPremiumHourType";
    public final static String PARAM_TIMECODELIST = "TimeCodeList";
    public final static String PARAM_HOURTYPELIST = "HourTypeList";
    public final static String PARAM_MINIMUM_MINUTES_TO_QUALIFY = "MinimumMinutesToQualify";
    public final static String PARAM_MAXIMUM_MINUTES_TO_QUALIFY = "MaximumMinutesToQualify";
    public final static String PARAM_GUARANTEED_MINUTES = "GuaranteedMinutes";
    public final static String PARAM_VAL_GUARANTEED_MINUTES_SCHEDULE = "S";
    public final static String PARAM_VAL_GUARANTEED_MINUTES_PCT_PREFIX = "-";

    public final static String PARAM_DISCOUNT_MINUTES = "DiscountMinutes";
    public final static String PARAM_USE_EFFECTIVE_MINUTES = "UseEffectiveMinutes";
    public final static String PARAM_INDEPENDENT_OCCURRENCES = "IndependentOccurrences";
    public final static String PARAM_VAL_INDEPENDENT_OCCURRENCES_EXTENDED = "EXTENDED";
    public final static String PARAM_INDEPENDENT_CONTINUE_TIMECODES = "IndependentContinueTimeCodes";
    public final static String PARAM_PREMIUM_DETAIL = "PremiumDetail";
    public final static String PARAM_VAL_BOTH = "B";

    public final static String PARAM_COPY_DETAIL_FIELD = "CopyDetailField";
    public final static String PARAM_DISTRIBUTE_PREMIUM = "DistributePremium";

    public static String EMP_UDF_PREFIX = "EMP_UDF";
    private static String EMP_UDF_HOUR = "*60";


    public List getParameterInfo (DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_GUARANTEED_PREMIUM_TIMECODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_GUARANTEED_PREMIUM_HOURTYPE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TIMECODELIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HOURTYPELIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_MINIMUM_MINUTES_TO_QUALIFY, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_MAXIMUM_MINUTES_TO_QUALIFY, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_GUARANTEED_MINUTES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_DISCOUNT_MINUTES, RuleParameterInfo.STRING_TYPE, true));

        RuleParameterInfo rpiUseEffectiveMinutesChoice = new RuleParameterInfo(PARAM_USE_EFFECTIVE_MINUTES, RuleParameterInfo.CHOICE_TYPE, true);
        rpiUseEffectiveMinutesChoice.addChoice("true");
        rpiUseEffectiveMinutesChoice.addChoice("false");
        result.add(rpiUseEffectiveMinutesChoice);

        RuleParameterInfo rpiIndependentOccurrencesChoice = new RuleParameterInfo(PARAM_INDEPENDENT_OCCURRENCES, RuleParameterInfo.CHOICE_TYPE, true);
        rpiIndependentOccurrencesChoice.addChoice("true");
        rpiIndependentOccurrencesChoice.addChoice("false");
        rpiIndependentOccurrencesChoice.addChoice(PARAM_VAL_INDEPENDENT_OCCURRENCES_EXTENDED);
        result.add(rpiIndependentOccurrencesChoice);

        result.add(new RuleParameterInfo(PARAM_INDEPENDENT_CONTINUE_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_DETAIL, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_COPY_DETAIL_FIELD, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DISTRIBUTE_PREMIUM, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {
        ParametersResolved pars = new ParametersResolved();
        // Retrieve parameters
        pars.guaranteedPremiumTimeCode = parameters.getParameter(PARAM_GUARANTEED_PREMIUM_TIMECODE);
        TimeCodeData tcdPremTcode = wbData.getCodeMapper().getTimeCodeByName(pars.guaranteedPremiumTimeCode);
        if (tcdPremTcode == null) {
            throw new RuleEngineException("Premium timecode not found : " + pars.guaranteedPremiumTimeCode);
        }
        pars.guaranteedPremiumHourType = parameters.getParameter(PARAM_GUARANTEED_PREMIUM_HOURTYPE);
        if (StringHelper.isEmpty(pars.guaranteedPremiumHourType)) {
            pars.guaranteedPremiumHourType = wbData.getCodeMapper().getHourTypeById(tcdPremTcode.getHtypeId()).getHtypeName();
        }
        pars.timeCodeList = parameters.getParameter(PARAM_TIMECODELIST);
        pars.hourTypeList = parameters.getParameter(PARAM_HOURTYPELIST, null);
        pars.minimumMinutesToQualify = parameters.getIntegerParameter(PARAM_MINIMUM_MINUTES_TO_QUALIFY, 0);
        pars.maximumMinutesToQualify = parameters.getIntegerParameter(PARAM_MAXIMUM_MINUTES_TO_QUALIFY, Integer.MAX_VALUE);
        pars.maximumMinutesToQualify = (pars.maximumMinutesToQualify == 0) ? Integer.MAX_VALUE : pars.maximumMinutesToQualify;

        String guaranteedMinutesString = parameters.getParameter(PARAM_GUARANTEED_MINUTES);
        String discountMinutesString = parameters.getParameter(PARAM_DISCOUNT_MINUTES);
        pars.guaranteedMinutes = getGuaranteedMins(wbData , guaranteedMinutesString , discountMinutesString);
        if (logger.isDebugEnabled()) logger.debug("guaranteedMinutes : " + pars.guaranteedMinutes);

        pars.useEffectiveMinutes = Boolean.valueOf(parameters.getParameter(PARAM_USE_EFFECTIVE_MINUTES, "false")).booleanValue();
        String  independentOccurrencesString = parameters.getParameter(PARAM_INDEPENDENT_OCCURRENCES, "true");
        pars.independentOccurrences = false;
        pars.independentOccurrencesExtended = false;
        if (Boolean.TRUE.toString().equalsIgnoreCase(independentOccurrencesString)
            || Boolean.FALSE.toString().equalsIgnoreCase(independentOccurrencesString)) {
            pars.independentOccurrences = Boolean.valueOf(independentOccurrencesString).booleanValue();
        }
        else if (PARAM_VAL_INDEPENDENT_OCCURRENCES_EXTENDED.equalsIgnoreCase(independentOccurrencesString)) {
            pars.independentOccurrencesExtended = true;
            pars.independentOccurrences = true;
        }
        else {
            throw new RuleEngineException ("IndependentOccurrences must be true/false or EXTENDED");
        }
        pars.independentContinueTimeCodes =  parameters.getParameter(PARAM_INDEPENDENT_CONTINUE_TIMECODES);
        pars.premiumDetail =  parameters.getParameter(PARAM_PREMIUM_DETAIL , WorkDetailData.DETAIL_TYPE);
        pars.copyDetailField = parameters.getParameter(PARAM_COPY_DETAIL_FIELD, null);
        pars.distributePremium = Boolean.valueOf(parameters.getParameter(PARAM_DISTRIBUTE_PREMIUM, "false")).booleanValue();

        if (WorkDetailData.DETAIL_TYPE.equals(pars.premiumDetail)) {
            applyGuarantee(wbData, wbData.getRuleData().getWorkDetails(), pars);
        }
        else if (WorkDetailData.PREMIUM_TYPE.equals(pars.premiumDetail)) {
            applyGuarantee(wbData, wbData.getRuleData().getWorkPremiums(), pars);
        }
        else if (PARAM_VAL_BOTH.equals(pars.premiumDetail)) {
            WorkDetailList wdl = new WorkDetailList();
            wdl.addAll(wbData.getRuleData().getWorkPremiums());
            wdl.addAll(wbData.getRuleData().getWorkDetails());
            applyGuarantee(wbData, wdl , pars);
        }

        else {
            throw new RuleEngineException("Premium details must be D, P or B");
        }
    }

    /**
     * Applies guarantee to given <code>WorkDetailList</code>
     * @param wbData
     * @param wdl
     */
    protected void applyGuarantee(WBData wbData , WorkDetailList wdl
                                  , ParametersResolved pars) {

        HashMap distribution = null;
        String copyFieldValue = null;
        String copyField = null;
        boolean continues = false;
        int actualMinutesWorked = 0;
        int effectiveMinutesWorked = 0;
        double storedValue = 0;

        distribution = new HashMap();
        if(!StringHelper.isEmpty(pars.copyDetailField) && !pars.distributePremium)
        {
            copyField = getFieldOfLastWD(wbData.getRuleData(), pars);
        }

        for (int i = 0, j = wdl.size() ; i < j; i++) {
            WorkDetailData workDetailData = wdl.getWorkDetail(i);

            if (RuleHelper.isCodeInList(pars.timeCodeList,workDetailData.getWrkdTcodeName())
                && (RuleHelper.isCodeInList(pars.hourTypeList,workDetailData.getWrkdHtypeName()))) {

                double effectiveMinutesMultiplier = pars.useEffectiveMinutes
                    ? wbData.getCodeMapper().getHourTypeById(workDetailData.getHtypeId()).getHtypeMultiple()
                    : 1;

                effectiveMinutesWorked += (int) (workDetailData.getWrkdMinutes() * effectiveMinutesMultiplier);
                actualMinutesWorked += workDetailData.getWrkdMinutes();

                //get field value
                if(!StringHelper.isEmpty(pars.copyDetailField))
                {
	                if(workDetailData.getField(pars.copyDetailField) instanceof Integer)
	                {
	                    copyFieldValue = ((Integer)workDetailData.getField(pars.copyDetailField)).toString();
	                }
	                else
	                {
	                    copyFieldValue = (String)workDetailData.getField(pars.copyDetailField);
	                }

	                //keep track of amount of minutes worked per field value
	                if(distribution.containsKey(copyFieldValue))
	                {
	                    storedValue = Double.parseDouble((String)distribution.get(copyFieldValue));
	                    storedValue += workDetailData.getWrkdMinutes() * effectiveMinutesMultiplier;
	                    distribution.put(copyFieldValue, String.valueOf(storedValue));
	                }
	                else
	                {
	                    distribution.put(copyFieldValue, String.valueOf(workDetailData.getWrkdMinutes() * effectiveMinutesMultiplier));
	                }
                }

                if (pars.independentOccurrences) {
                    WorkDetailData workDetailDataNext = null;
                    if (i != j - 1) {
                        workDetailDataNext = wdl.getWorkDetail(i + 1);
                    }
                    // *** if independentContinueTimeCodes defined, check next record
                    boolean isIndepContinueEligible = false;
                    if (!StringHelper.isEmpty(pars.independentContinueTimeCodes)) {
                        isIndepContinueEligible = workDetailDataNext != null
                            &&
                            RuleHelper.isCodeInList(pars.independentContinueTimeCodes,
                            workDetailDataNext.getWrkdTcodeName());
                    }

                    boolean isNextCodeHtypeEligible = false;
                    // *** if extended check next detail
                    if (pars.independentOccurrencesExtended) {
                        if (workDetailDataNext!= null) {
                            isNextCodeHtypeEligible =
                                workDetailData.getWrkdTcodeName().equals(workDetailDataNext.getWrkdTcodeName())
                                && workDetailData.getWrkdHtypeName().equals(workDetailDataNext.getWrkdHtypeName());
                        }
                    }
                    if (isNextCodeHtypeEligible || isIndepContinueEligible) {
                        continues = true;
                    }
                    else {
                        continues = false;
                    }

                    if (!continues) {
                        if ( (actualMinutesWorked > 0)
                            && (actualMinutesWorked >= pars.minimumMinutesToQualify)
                            && (actualMinutesWorked <= pars.maximumMinutesToQualify)) {
                            int premMins = pars.guaranteedMinutes - effectiveMinutesWorked;
                            if ( premMins > 0) {
                                if(pars.distributePremium && !distribution.isEmpty())
                                {
                                    insertDistributedPremium(
            	                            distribution,
            	                            wbData.getRuleData(),
            	                            premMins,
            	                            effectiveMinutesWorked,
            	                            pars.guaranteedPremiumTimeCode,
            	                            pars.guaranteedPremiumHourType,
            	                            copyField,
                                            pars);
                                    distribution.clear();
                                }
                                else
                                {
                                    insertWorkPremiumRecordWithField(
                                            wbData.getRuleData(),
                                            premMins,
                                            pars.guaranteedPremiumTimeCode,
                                            pars.guaranteedPremiumHourType,
                                            copyField,
                                            pars);
                                }
                                if (logger.isDebugEnabled()) logger.debug("Added guarantee premium for : " + premMins + " minutes");
                            }
                        }
                        effectiveMinutesWorked = 0;
                        actualMinutesWorked = 0;
                    }
                }
            }
        }

        if (!pars.independentOccurrences) {
            if ( (actualMinutesWorked > 0) &&
                 (actualMinutesWorked >= pars.minimumMinutesToQualify) &&
                 (actualMinutesWorked <= pars.maximumMinutesToQualify) ) {
                int premMins = pars.guaranteedMinutes - effectiveMinutesWorked;
                if (premMins > 0) {
                    if(pars.distributePremium && !distribution.isEmpty())
                    {
	                    insertDistributedPremium(
	                            distribution,
	                            wbData.getRuleData(),
	                            premMins,
	                            effectiveMinutesWorked,
	                            pars.guaranteedPremiumTimeCode,
	                            pars.guaranteedPremiumHourType,
	                            copyField,
                                pars);
                    }
                    else
                    {
	                    insertWorkPremiumRecordWithField(
	                            wbData.getRuleData(),
	                            premMins,
	                            pars.guaranteedPremiumTimeCode,
	                            pars.guaranteedPremiumHourType,
	                            copyField, pars);
                    }
                    if (logger.isDebugEnabled()) logger.debug("Added guarantee premium for : " + premMins + " minutes");
                }
            }
        }
    }

    /**
     * Calculate guaranteedMinutes based on guaranteedMinutesString
     * @param wbData
     * @param guaranteedMinutesString
     * @param discountMinutesString
     * @return
     * @throws Exception
     */
    protected int getGuaranteedMins(WBData wbData ,
                                    String guaranteedMinutesString,
                                    String discountMinutesString) throws Exception {
        if (StringHelper.isEmpty(guaranteedMinutesString)) {
            throw new RuleEngineException ("guaranteedMinutesString cannot be empty");
        }
        int guarMins = 0;
        int discountMins = 0;
        try {
            if (!StringHelper.isEmpty(discountMinutesString)) {
                discountMins = Integer.parseInt(discountMinutesString);
            }

            if (guaranteedMinutesString.equalsIgnoreCase(PARAM_VAL_GUARANTEED_MINUTES_SCHEDULE)) {
                guarMins = wbData.getScheduleDuration();
            }
            else if (guaranteedMinutesString.startsWith("EMP_VAL")) {
                String empVal = (String) wbData.getRuleData().
                    getEmployeeData().getField(guaranteedMinutesString);
                if (StringHelper.isEmpty(empVal)) {
                    throw new RuleEngineException(guaranteedMinutesString + " emp_val is empty");
                }
                guarMins = Integer.parseInt(empVal);
            }
            else if (guaranteedMinutesString.startsWith("WRKS_UDF")) {
                String wrksUdf = (String) wbData.getRuleData().
                    getWorkSummary().getField(guaranteedMinutesString);
                if (StringHelper.isEmpty(wrksUdf)) {
                    throw new RuleEngineException(guaranteedMinutesString + " wrksUdf is empty");
                }
                guarMins = Integer.parseInt(wrksUdf);
            }
            else if (guaranteedMinutesString.startsWith(PARAM_VAL_GUARANTEED_MINUTES_PCT_PREFIX)) {
                String pctString = guaranteedMinutesString.substring(
                    guaranteedMinutesString.indexOf(PARAM_VAL_GUARANTEED_MINUTES_PCT_PREFIX) + 1);
                if (StringHelper.isEmpty(pctString)) {
                    throw new RuleEngineException("Percentage is empty");
                }
                int pct = Integer.parseInt(pctString);
                if (logger.isDebugEnabled()) logger.debug("Schedule duration : " + wbData.getScheduleDuration());
                guarMins = (wbData.getScheduleDuration() - discountMins) * pct / 100;
            }
            else if (guaranteedMinutesString.startsWith(EMP_UDF_PREFIX)) {
                String mins = null; boolean isHour = false;
                int defMinsIndex = guaranteedMinutesString.indexOf("|");
                String defMins = null;
                if (defMinsIndex >= 0) {
                    mins = guaranteedMinutesString.substring(0, defMinsIndex);
                    defMins = guaranteedMinutesString.substring(defMinsIndex + 1);
                }
                if (mins.endsWith(EMP_UDF_HOUR)) {
                    isHour = true;
                    mins = mins.substring(0, mins.indexOf(EMP_UDF_HOUR) ) ;
                }
                int empudfIndex = mins.indexOf("~");
                if (empudfIndex == -1) {
                    throw new RuleEngineException ("EMP_UDF definition is not defined");
                }
                String empUdfName = mins.substring(empudfIndex + 1);

                mins = (String)wbData.getEmpUdfValue(empUdfName);
                if (StringHelper.isEmpty(mins)
                    && !StringHelper.isEmpty(defMins) ) {
                    mins = defMins;
                }

                guarMins = Integer.parseInt(mins);
                if (isHour) {
                    guarMins *= 60;
                }
                if (logger.isDebugEnabled()) logger.debug("Resolved empudf to :" + guarMins + " guaranteed hours");
            }
            else {
                guarMins = Integer.parseInt(guaranteedMinutesString);
            }
        }
        catch (Exception ex) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("Error in resolving guaranteedMinutes" , ex);
            throw new NestedRuntimeException("Error in resolving guaranteedMinutes : " + guaranteedMinutesString, ex);
        }
        return guarMins;
    }

    private void insertDistributedPremium(HashMap distribution,
            RuleData ruleData,
            int premMinutes,
            int totalMinutes,
            String tcodeName,
            String htypeName,
            String copyField,
            ParametersResolved pars)
    {
        Iterator itKeys = null;
        String key = null;
        double value = 0;
        double percent = 0;
        BigDecimal minutes = null;

        //create premium for each key in list
        itKeys = distribution.keySet().iterator();
        while(itKeys.hasNext())
        {
            key = (String)itKeys.next();

            //minutes contributing to this key
            value = Double.parseDouble((String)distribution.get(key));
            percent = value/totalMinutes;
            minutes = new BigDecimal(percent * premMinutes);
            minutes = minutes.setScale(0, BigDecimal.ROUND_HALF_UP);

            //create premium with the calculated percentage and key
            insertWorkPremiumRecordWithField(
                    ruleData,
                    (int)minutes.intValue(),
                    pars.guaranteedPremiumTimeCode,
                    pars.guaranteedPremiumHourType,
                    key, pars);
        }
    }

    private String getFieldOfLastWD(RuleData ruleData, ParametersResolved pars)
    {
        List workDetailList = null;
        HourTypeData hourType = null;
        WorkDetailData workDetail = null;
        Iterator itWDList = null;
        String copyField = null;

        //look for last Worked work detail
        workDetailList = ruleData.getWorkDetails();
        itWDList = workDetailList.iterator();

        hourType = ruleData.getCodeMapper().getHourTypeByName("UNPAID");

        while(itWDList.hasNext())
        {
            workDetail = (WorkDetailData)itWDList.next();

            //set deptId to the deptId of last Worked work detail
            if(!StringHelper.isEmpty(pars.copyDetailField) && workDetail.getHtypeId() != hourType.getHtypeId())
            {
                if(workDetail.getField(pars.copyDetailField) instanceof Integer)
                {
                    copyField = ((Integer)workDetail.getField(pars.copyDetailField)).toString();
                }
                else
                {
                    copyField = (String)workDetail.getField(pars.copyDetailField);
                }
            }
        }

        return copyField;
    }

    private void insertWorkPremiumRecordWithField(RuleData ruleData,
            int minutes,
            String tcodeName,
            String htypeName,
            String copyField,
            ParametersResolved pars)
    {
        WorkDetailData wd = null;
        HourTypeData htd = null;
        Date funnyDate = null;

        funnyDate = new GregorianCalendar(1900, 0, 1).getTime();
        wd = ruleData.getWorkPremiums().add(funnyDate, funnyDate, ruleData.getEmpDefaultLabor(0));
        wd.setWrkdType(WorkDetailData.PREMIUM_TYPE);
        wd.setWrkdMinutes(minutes);
        wd.setWrkdTcodeName(tcodeName);
        wd.setWrkdHtypeName(htypeName);

        //overwrite the dept Id from empdeflab with deptId
        if(!StringHelper.isEmpty(copyField))
        {
            wd.setField(pars.copyDetailField, copyField);
        }

        htd = ruleData.getCodeMapper().getHourTypeById(wd.getHtypeId());
        wd.setWrkdRate(htd.getHtypeMultiple() * ruleData.getEmployeeData().getEmpBaseRate());
    }


    public String getComponentName() {
        return "WBIAG : Guarantees Extended Rule";
    }

    class ParametersResolved {
        String guaranteedPremiumTimeCode;
        String guaranteedPremiumHourType;
        String timeCodeList;
        String hourTypeList;
        int minimumMinutesToQualify;
        int maximumMinutesToQualify;
        int guaranteedMinutes;

        boolean useEffectiveMinutes = false;
        boolean independentOccurrences = false;
        boolean independentOccurrencesExtended = false;
        boolean distributePremium = false;
        final int invalidDeptId = -9876;
        String independentContinueTimeCodes;
        String premiumDetail;
        String copyDetailField;

    }

}
