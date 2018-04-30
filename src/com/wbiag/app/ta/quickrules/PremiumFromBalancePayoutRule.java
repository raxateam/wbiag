package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

import java.util.*;

/**
 * Created to implement the requirement stated in TT50222.
 * Inserts work premium records to pay-out balance totals in minutes of the specified time codes and hour types.
 * Can optionally concurrently decrement the balance by the amount paid-out.
 * No special rounding of minutes in the getBalanceMinutes(...) methods, only casting of double to int (truncation).
 * Does not provide zero or negative payout amounts.
 * CAUTION: if the balance is not of the current date
 *             and is greater than the current date balance (date of decrement),
 *            an insufficient balance error may occur,
 *            especially for "To final value" is selected
 * N.B.: no attention is paid to the balance minimum
 *         (all balances are considered to have a minimum of zero)
 * N.B.: if the balance is at or below the final value or zero,
 *          no balance will be decremented and no payout will occur
 *
 * Maintains no instance-level state.
 * Does not create overrides - deducts balances directly and inserts premium records directly.
 * Contains many public interfaces to facilitate extension:
 * <ul> (use the getLogStatement(...) methods for consistency in debug messaging)
 *        <li>to change how existing parameters are parsed: override the parseXXX(...) method(s)</li>
 *        <li>to add new payout approached: implement getPayoutValue(...) and extend getPayoutApproaches()</li>
 *        <li>to add new balance as of date options: implement getBalanceDate(...) and extend getBalanceDates()</li>
 *        <li>to support additional balance types: implement getBalanceMinutes(...)</li>
 *        <li>to support population of additional work premium fields: override the parseAdditionalFields(...) and populateAdditionalField(...) methods</li>
 * </ul>
 * Future configuration possibilities:
 * <ul>
 *        <li>Allowing the date of the premium record to be specified (e.g. by offset)</li>
 *        <li>Allowing additional override fields to be specified</li>
 * </ul>
 *
 * <i>
 * TT50222:
 * Many projects require an employee to paid all or a portion of a balance upto a certain amount.
 *
 * An example is:
 * SICK/Personal Payout Rule
 * Employee receives payout for SICK/PER hours in excess of 48 hrs accrued as of December 1.
 * Payout is made in the first pay period end date of December in each year.
 * 1.1    Business Scenarios
 * All employees with remaining SICK/PER balance as of December 1st will be paid for the remaining balance.
 * On decemeber 1st, a premium override will be placed on the timesheet for the remaining balance
 * Timecode: SICK_PER
 * Hourtype: REG
 * </i>
 *
 * Unit test case ideas:
 * - failure: non-existant time codes and hour types
 * - failure: minutes per day beyond normal bounds (&lt;0 and &gt;1440)
 * - failure: minutes per unit beyond normal bounds (&lt;0)
 * - failure: different length lists of time codes, balances and hour types
 * - failure: unsupported additional premium fields
 * - success: span periods that include entitlements, transfers and cascades
 * - success: different existing balance types (daily, hourly, units), time codes and hour types
 * - success: to final value/fixed balance value and non-integer values for minutes and approach values
 * - success: single time code and hour type, multiple balances
 * - success: multiple time codes, hour types and balances
 * - success: supported single/multiple additional premium fields
 *
 * @author dferguson@workbrain.com
 * 
 * MLee - Modified rule for use at TPS, cleaned up unused parameters and removed unused code
*/
public class PremiumFromBalancePayoutRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PremiumFromBalancePayoutRule.class);

    /** Display name for this Rule. */
    public static final String COMPONENT_NAME = "WBIAG: Premium From Balance Payout Rule";

    /** Comma-delimited list of balance names (case-insensitive); mandatory, no default. */
    public static final String PARAM_BALANCE_NAMES = "BalanceNames";

    /** Flag to indicate whether the balances will be decremented in addition to the payout; mandatory, default is "TRUE". */
    public static final String PARAM_BALANCE_DECREMENTED = "BalanceDecremented";

    /** The approach to take for pay-out (see options); mandatory, no default */
    public static final String PARAM_PAYOUT_APPROACH = "PayoutApproach";

    public static final String PARAM_PAYOUT_TO_FINAL = "To final value";
    public static final String PARAM_PAYOUT_FIXED = "Fixed value";

    /** The value to use for the approach specified; mandatory, no default*/
    public static final String PARAM_PAYOUT_APPROACH_VALUE = "PayoutApproachValue";

    /** For unit-based balances, how many minutes are there in a unit of the balance specified; optional, default is 0. */
    public static final String PARAM_MINUTES_PER_UNIT = "MinutesPerBalanceUnit";

    /** Comma-delimited list of time codes for each balance listed or a single time code for all balances listed; mandatory, no default. */
    public static final String PARAM_TIME_CODE_NAMES = "PremiumTimeCodeNames";

    /** Comma-delimited list of hour types for each balance listed or a single hour type for all balances listed; mandatory, no default. */
    public static final String PARAM_HOUR_TYPE_NAMES = "PremiumHourTypeNames";

    /** Additional premium field values to set: UDF's, flags, comments, messages, labor allocation; optional, no default. */
    public static final String PARAM_ADDITIONAL_PREM = "AdditionalPremiumFieldValues";

    /** Matching the WorkDetailData class constants e.g. WorkDetailData.WRKD_UDF1 */
    public static final String[] validAdditionalPremiumFields = {
        WorkDetailData.WRKD_UDF1, WorkDetailData.WRKD_UDF2, WorkDetailData.WRKD_UDF3, WorkDetailData.WRKD_UDF4, WorkDetailData.WRKD_UDF5
    ,    WorkDetailData.WRKD_UDF6, WorkDetailData.WRKD_UDF7, WorkDetailData.WRKD_UDF8, WorkDetailData.WRKD_UDF9, WorkDetailData.WRKD_UDF10
    ,    WorkDetailData.WRKD_FLAG1, WorkDetailData.WRKD_FLAG2, WorkDetailData.WRKD_FLAG3, WorkDetailData.WRKD_FLAG4, WorkDetailData.WRKD_FLAG5
    ,    WorkDetailData.WRKD_FLAG6, WorkDetailData.WRKD_FLAG7, WorkDetailData.WRKD_FLAG8, WorkDetailData.WRKD_FLAG9, WorkDetailData.WRKD_FLAG10
    ,    WorkDetailData.WRKD_DOCK_NAME, WorkDetailData.WRKD_DEPT_NAME, WorkDetailData.WRKD_JOB_NAME, WorkDetailData.WRKD_PROJ_NAME
    ,    WorkDetailData.WRKD_QUANTITY, WorkDetailData.WRKD_RATE
    ,    WorkDetailData.WRKD_COMMENTS, WorkDetailData.WRKD_MESSAGES
    };

    /** does nothing */
    public PremiumFromBalancePayoutRule() {}

    /** @return COMPONENT_NAME */
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    //TPS customization to match the appearance of the core rule as of Feature Pack 3
    public String getComponentUI(){
    	return "/quickrules/wbiagPremiumFromBalanceParams.jsp";
    }
    
    /** @return input parameters (see parameter constants for descriptions) */
    public List getParameterInfo(DBConnection dbConnection) {
        List parameterInfo = new ArrayList();
        boolean OPTIONAL = true;
        boolean MANDATORY = false;

        RuleParameterInfo balanceNamesRPI = new RuleParameterInfo(PARAM_BALANCE_NAMES, RuleParameterInfo.STRING_TYPE, MANDATORY);        

        RuleParameterInfo balanceDecrementRPI = new RuleParameterInfo(PARAM_BALANCE_DECREMENTED, RuleParameterInfo.CHOICE_TYPE, MANDATORY);
        balanceDecrementRPI.addChoice("TRUE");
        balanceDecrementRPI.addChoice("FALSE");

        RuleParameterInfo payoutApproachRPI = new RuleParameterInfo(PARAM_PAYOUT_APPROACH, RuleParameterInfo.CHOICE_TYPE, MANDATORY);
        String[] payoutApproaches = getPayoutApproaches();
        for(int choiceIndex = 0 ; choiceIndex < payoutApproaches.length ; ++choiceIndex) {
            payoutApproachRPI.addChoice(payoutApproaches[choiceIndex]);
        }
        RuleParameterInfo payoutApproachValueRPI = new RuleParameterInfo(PARAM_PAYOUT_APPROACH_VALUE, RuleParameterInfo.STRING_TYPE, MANDATORY);

        RuleParameterInfo minutesPerUnitRPI = new RuleParameterInfo(PARAM_MINUTES_PER_UNIT, RuleParameterInfo.STRING_TYPE, OPTIONAL);
        RuleParameterInfo timeCodeRPI = new RuleParameterInfo(PARAM_TIME_CODE_NAMES, RuleParameterInfo.STRING_TYPE, MANDATORY);
        RuleParameterInfo hourTypeRPI = new RuleParameterInfo(PARAM_HOUR_TYPE_NAMES, RuleParameterInfo.STRING_TYPE, MANDATORY);
        RuleParameterInfo additionalPremiumFieldsRPI = new RuleParameterInfo(PARAM_ADDITIONAL_PREM, RuleParameterInfo.STRING_TYPE, OPTIONAL);

        parameterInfo.add(balanceNamesRPI);
        parameterInfo.add(balanceDecrementRPI);
        parameterInfo.add(payoutApproachRPI);
        parameterInfo.add(payoutApproachValueRPI);
        parameterInfo.add(minutesPerUnitRPI);
        parameterInfo.add(timeCodeRPI);
        parameterInfo.add(hourTypeRPI);
        parameterInfo.add(additionalPremiumFieldsRPI);

        return parameterInfo;
    }

    /**
     * <ul>Execution:
     *         <li>extracts passed parameters using Parameters.getParameter(...) and specified parameter defaults</li>
     *         <li>calls the parseXXX(...) method for each parameter (can be overridden)</li>
     *         <li>calls the extractPayoutMinutes(...) method</li>
     *         <li>calls the insertWorkPremiumOverrides(...) method</li>
     * </ul>
    */
    public void execute(WBData wbData, Parameters parameters) throws Exception {

        if(logger.isDebugEnabled()) {
            logger.debug("Parameters: " + parameters);
        }

        // extract passed parameters
        String balanceNamesParam = parameters.getParameter(PARAM_BALANCE_NAMES, "");
        String balanceDecrementParam = parameters.getParameter(PARAM_BALANCE_DECREMENTED, "");
        String payoutApproachParam = parameters.getParameter(PARAM_PAYOUT_APPROACH, "");
        double payoutApproachValueParam = parameters.getDoubleParameter(PARAM_PAYOUT_APPROACH_VALUE, -1.0);
        String timeCodeNamesParam = parameters.getParameter(PARAM_TIME_CODE_NAMES, "");
        String hourTypeNamesParam = parameters.getParameter(PARAM_HOUR_TYPE_NAMES, "");
        double minutesPerUnitParam = parameters.getDoubleParameter(PARAM_MINUTES_PER_UNIT, 60.0);
        String additionalPremiumFieldsParam = parameters.getParameter(PARAM_ADDITIONAL_PREM, "");

        // parse and validate passed parameters
        String[] balanceNames = parseBalanceNames(balanceNamesParam, wbData);
        BalanceTypeData[] balanceTypeData = parseBalanceTypeData(balanceNames, wbData);
        boolean decrementBalances = (balanceDecrementParam.equals("TRUE") ? true : false);
        if(logger.isDebugEnabled()) {
            logger.debug("Parsed parameter: " + getLogStatement("Balance decrement", String.valueOf(decrementBalances)));
        }
        String payoutApproach = payoutApproachParam;
        double payoutApproachValue = parsePayoutApproachValue(payoutApproachParam, payoutApproachValueParam, wbData);
        String[] timeCodeNames = parseTimeCodeNames(timeCodeNamesParam, balanceNames, wbData);
        String[] hourTypeNames= parseHourTypeNames(hourTypeNamesParam, balanceNames, wbData);
        if(hourTypeNames.length != 1 && timeCodeNames.length != hourTypeNames.length) {
            throw new IllegalArgumentException("Time code Names list length does not match hour type Names list length.");
        }
        double minutesPerUnit = parseMinutesPerUnit(minutesPerUnitParam, wbData);
        Map additionalPremiumFields = parseAdditionalFields(additionalPremiumFieldsParam, validAdditionalPremiumFields, wbData);

        // determine balance minutes (parallel array with balance values)
        // and decrement the balances if specified
        int[] payoutMinutes = extractPayoutMinutes(        balanceNames
                                                    ,    balanceTypeData
                                                    ,    payoutApproach
                                                    ,    payoutApproachValue
                                                    ,    minutesPerUnit
                                                    ,    decrementBalances
                                                    ,    timeCodeNames
                                                    ,    wbData);

        // insert payout overrides
        insertWorkPremiumOverrides(        balanceNames
                                    ,    payoutMinutes
                                    ,    timeCodeNames
                                    ,    hourTypeNames
                                    ,    additionalPremiumFields
                                    ,    wbData);
    }

    /**
     * @param decrementBalances iff true decrements balances once the payout amount is determined
     * @return the number of minutes for each balance specified.
    */
    public int[] extractPayoutMinutes(String[] balanceNames, BalanceTypeData[] balanceTypeData, String payoutApproach, double payoutApproachValue, double minutesPerUnit, boolean decrementBalances, String[] timeCodeNames, WBData wbData) throws Exception {
        if(balanceNames.length != balanceTypeData.length) {
            throw new IllegalArgumentException("Balance Names list length does not match Types list length.");
        }
        CodeMapper codeMapper = wbData.getCodeMapper();
        int[] balanceMinutes = new int[balanceNames.length];

        for(int balanceIndex = 0; balanceIndex < balanceNames.length; balanceIndex++) {
            // gather balance information based-on the balance Name
            String balanceName = balanceNames[balanceIndex];
            int balanceId = codeMapper.getBalanceByName(balanceName).getBalId();
            double balanceValue = getBalanceValue(balanceId, timeCodeNames, wbData);
            String balanceTypeName = balanceTypeData[balanceIndex].getBaltypName().trim();
            if(logger.isDebugEnabled()) {
                StringBuffer balanceInfo = new StringBuffer("Balance information: ");
                balanceInfo.append(getLogStatement("Name", balanceName));
                balanceInfo.append(getLogStatement("Id", String.valueOf(balanceId)));
                balanceInfo.append(getLogStatement("Type Name", balanceTypeName));
                balanceInfo.append(getLogStatement("value", String.valueOf(balanceValue)));
                logger.debug(balanceInfo.toString());
            }
            // determine the payout balance value
            double payoutBalanceValue = getBalancePayoutValue(balanceValue, payoutApproach, payoutApproachValue, wbData);
            // determine the number of minutes that correspond to that balance payout value
            balanceMinutes[balanceIndex] = getBalanceMinutes(balanceTypeName, payoutBalanceValue, minutesPerUnit, wbData);
            // decrement the balances for the payout
            if(decrementBalances && payoutBalanceValue > 0) {
                wbData.addEmployeeBalanceValue(balanceId, -payoutBalanceValue, getComponentName());
            }
        }
        return balanceMinutes;
    }

    /** @return the current value for the balance, considering all work detail and premium records; does not include premium time codes used by this rule. */
    public double getBalanceValue(int balanceId, String[] timeCodeNames, WBData wbData) throws Exception {
        // get the cached balance value
        double balanceValue = wbData.getEmployeeBalanceValue(balanceId);
        if(logger.isDebugEnabled()) {
            StringBuffer balanceInfo = new StringBuffer("Balance initial value information: ");
            balanceInfo.append(getLogStatement("Emp Id", String.valueOf(wbData.getEmpId())));
            balanceInfo.append(getLogStatement("Balance Id", String.valueOf(balanceId)));
            balanceInfo.append(getLogStatement("Initial value", String.valueOf(balanceValue)));
            logger.debug(balanceInfo.toString());
        }

        // cache the balance Id - time code Id associations
        List timeCodeBalamces = wbData.getCodeMapper().getTCBByBalanceId(balanceId) ;
        HashMap timeCodeBalanceCache = new HashMap();
        Iterator iter = timeCodeBalamces.iterator();
        while (iter.hasNext()) {
            TimeCodeBalanceData item = (TimeCodeBalanceData)iter.next();
            timeCodeBalanceCache.put(new Integer(item.getTcodeId()) ,
                                     new Integer(item.getTcbtId()) );
        }
        // update the balance with adjustments for each applicable work detail record.
        RuleData ruleData = wbData.getRuleData();
        CodeMapper codeMapper = ruleData.getCodeMapper();
        int workDetailCount = ruleData.getWorkDetailCount();
        for (int i = 0; i < workDetailCount; i++) {
            WorkDetailData workDetailData = ruleData.getWorkDetail(i);
            balanceValue = processOneWorkDetail(workDetailData, balanceValue, timeCodeBalanceCache, codeMapper, wbData);
        }
        int workPremiumCount = ruleData.getWorkPremiumCount();
        for (int i = 0; i < workPremiumCount; i++) {
            WorkDetailData workDetailData = ruleData.getWorkPremium(i);
            balanceValue = processOneWorkDetail(workDetailData, balanceValue, timeCodeBalanceCache, codeMapper, wbData);
        }
        return balanceValue;
    }

    /** @return the passed balance value adjusted for the passed work detail record. */
    protected double processOneWorkDetail(WorkDetailData workDetailData, double balanceValue, HashMap timeCodeBalanceCache, CodeMapper codeMapper, WBData wbData) throws Exception {
        Integer timeCodeId = new Integer(workDetailData.getTcodeId());
        int minutes = workDetailData.getWrkdMinutes();
        if(logger.isDebugEnabled()) {
            StringBuffer balanceInfo = new StringBuffer("Work detail information: ");
            balanceInfo.append(getLogStatement("Work Detail Id", String.valueOf(workDetailData.getWrkdId())));
            balanceInfo.append(getLogStatement("Time Code Id", String.valueOf(timeCodeId)));
            balanceInfo.append(getLogStatement("Minutes", String.valueOf(minutes)));
            logger.debug(balanceInfo.toString());
        }
        // get the balance value for the minutes worked by that timecode
        Integer timeCodeBalanceTypeId = (Integer)timeCodeBalanceCache.get(timeCodeId);
        if(timeCodeBalanceTypeId != null) {
            TimeCodeBalanceTypeData timeCodeBalanceTypeData = codeMapper.getTimeCodeBalanceTypeById(timeCodeBalanceTypeId.intValue());
            double adjustmentValue = RuleHelper.calcBalanceChange(minutes, timeCodeBalanceTypeData);
            balanceValue += adjustmentValue;
            if(logger.isDebugEnabled()) {
                StringBuffer balanceInfo = new StringBuffer("Balance adjusted value information: ");
                balanceInfo.append(getLogStatement("Work Detail Id", String.valueOf(workDetailData.getWrkdId())));
                balanceInfo.append(getLogStatement("Adjustment value", String.valueOf(adjustmentValue)));
                balanceInfo.append(getLogStatement("New balance value", String.valueOf(balanceValue)));
                logger.debug(balanceInfo.toString());
            }
        }
        return balanceValue;
    }

    /** @return the number of minutes elapsed for the specified balance Type and value. */
    public int getBalanceMinutes(String balanceTypeName, double balanceValue, double minutesPerUnit, WBData wbData) throws Exception {
        // convert payout amount to minutes
        int balanceMinutes = (int)(minutesPerUnit * balanceValue);
        if(logger.isDebugEnabled()) {
            logger.debug(getLogStatement("Balance minutes", String.valueOf(balanceMinutes)));
        }
        return balanceMinutes;
    }

    /** @return the balance value to be paid-out. */
    public double getBalancePayoutValue(double balanceValue, String payoutApproach, double payoutApproachValue, WBData wbData) throws Exception {
        double payoutBalanceValue = balanceValue;
        // determine balanceValue to payout
        if(payoutApproach.equals(PARAM_PAYOUT_TO_FINAL)) {
            if(balanceValue <= payoutApproachValue) {
                payoutBalanceValue = 0;
            } else {
                payoutBalanceValue = (balanceValue - payoutApproachValue);
            }
        } else if(payoutApproach.equals(PARAM_PAYOUT_FIXED)) {
            payoutBalanceValue = payoutApproachValue;
        } else {
            payoutBalanceValue = getPayoutValue(balanceValue, payoutApproach, payoutApproachValue, wbData);
        }
        if(logger.isDebugEnabled()) {
            logger.debug(getLogStatement("Balance payout value", String.valueOf(payoutBalanceValue)));
        }
        return payoutBalanceValue;
    }



    /**
     * Override to add logic for custom balance types.
     * @throws IllegalArgumentException by default and for all unhandled balance types
    */
    public int getPayoutValue(double balanceValue, String payoutApproach, double payoutApproachValue, WBData wbData) throws Exception {
        throw new IllegalArgumentException("Unknown payout approach: " + payoutApproach);
    }


    /**
     * Inserts the premium records into WBData, not as overrides.
    */
    public void insertWorkPremiumOverrides( String[] balanceNames
                                        ,    int[] balanceMinutes
                                        ,     String[] timeCodeNames
                                        ,     String[] hourTypeNames
                                        ,     Map additionalPremiumFields
                                        ,    WBData wbData) throws Exception {
        int wrksId = wbData.getRuleData().getWorkSummary().getWrksId();
        EmployeeDefaultLaborData empDefLabData = wbData.getRuleData().getEmpDefaultLabor(0);
        // one premium record for all balance minutes
        
        //Check to see if hourTypeNames has length greater than zero
        boolean hasMultipleHTypes = (hourTypeNames.length > 1);
        String currentHType = null;
        //If there is only one hour type name then load it now
        if(!hasMultipleHTypes){
            currentHType = hourTypeNames[0];
        }
        
        if(timeCodeNames.length == 1) {
            int balanceMinutesTotal = 0;
            for(int balanceIndex = 0 ; balanceIndex < balanceMinutes.length ; balanceIndex++) {
                balanceMinutesTotal += balanceMinutes[balanceIndex];
            }
            if(balanceMinutesTotal == 0) {
                logger.info("No balance payout for empId=" + wbData.getEmpId() + " and wrksDate=" + wbData.getWrksWorkDate());
                return;
            }
            insertWorkPremium(wrksId, balanceMinutesTotal, timeCodeNames[0], hourTypeNames[0], empDefLabData, additionalPremiumFields, wbData);
        } else {
            // one premium record for each balance
            for(int timeCodeIndex = 0 ; timeCodeIndex < timeCodeNames.length ; timeCodeIndex++) {
                if(balanceMinutes[timeCodeIndex] == 0) {
                    logger.info("No balance payout for balanceName=" + balanceNames[timeCodeIndex] + " and empId=" + wbData.getEmpId() + " and wrksDate=" + wbData.getWrksWorkDate());
                    continue;
                }
                
                //If there are multiple hour types then load the hour type that corresponds to the current time code
                if(hasMultipleHTypes){
                    currentHType = hourTypeNames[timeCodeIndex];
                }
                
                insertWorkPremium(wrksId, balanceMinutes[timeCodeIndex], timeCodeNames[timeCodeIndex], currentHType, empDefLabData, additionalPremiumFields, wbData);
            }
        }
    }

    private void insertWorkPremium(int wrksId, int balanceMinutes, String timeCodeName, String hourTypeName, EmployeeDefaultLaborData empDefLabData, Map additionalPremiumFields, WBData wbData) throws Exception {
        WorkDetailData workDetailData = new WorkDetailData();
        workDetailData.setWrksId(wrksId);
        workDetailData.setDeptId(empDefLabData.getDeptId());
        workDetailData.setProjId(empDefLabData.getProjId());
        workDetailData.setJobId(empDefLabData.getJobId());
        workDetailData.setDockId(empDefLabData.getDockId());
        workDetailData.setCodeMapper(wbData.getCodeMapper());
        populateAdditionalFields(workDetailData, additionalPremiumFields, wbData);
        if(logger.isDebugEnabled()) {
            logger.debug("Premium record to be inserted:\n" + workDetailData);
        }
        wbData.insertWorkPremiumRecord(balanceMinutes, timeCodeName, hourTypeName, workDetailData);
    }

    public void populateAdditionalFields(WorkDetailData workDetailData, Map additionalPremiumFields, WBData wbData) throws Exception {
        Iterator itAdditionalFields = additionalPremiumFields.keySet().iterator();
        while(itAdditionalFields.hasNext()) {
            String fieldName = (String)itAdditionalFields.next();
            String fieldValue = String.valueOf(additionalPremiumFields.get(fieldName));
            try {
                // *** parseAdditionalFields already validated the eligible field names, use reflection
                workDetailData.setField(fieldName, fieldValue);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error setting additional field" , ex);}
                populateAdditionalField(fieldName, fieldValue, wbData);
            }
        }
    }

    /** Override to populate additional work premium data fields. */
    public void populateAdditionalField(String fieldName, String fieldValue, WBData wbData) throws Exception {
        throw new IllegalArgumentException("Invalid additional field passed: " + fieldName + " - possible discrepency between this method and the parse method for the additional fields.");
    }

    //---------------------------------------------------------------
    //        PARAMETER PARSING & VALIDATION
    //---------------------------------------------------------------

    /** @throws IllegalArgumentException if an empty list of balance names is passed. */
    public String[] parseBalanceNames(String balanceNamesParam, WBData wbData) {
        String[] balanceNames = StringHelper.detokenizeString(balanceNamesParam, ",", true /* trims */);
        if(balanceNames.length == 0) {
            throw new IllegalArgumentException("Empty list of balance Names received.");
        }
        if(logger.isDebugEnabled()) {
            logger.debug("Parsed parameter: " + getLogStatement("Balance Names", balanceNames));
        }
        return balanceNames;
    }

    /** @throws IllegalArgumentException for errors during balance type retrieval. */
    public BalanceTypeData[] parseBalanceTypeData(String[] balanceNames, WBData wbData) {
        BalanceTypeData[] balanceTypeData = new BalanceTypeData[balanceNames.length];
        for(int balanceIndex = 0 ; balanceIndex < balanceNames.length ; balanceIndex++) {
            String balanceName = balanceNames[balanceIndex];
            try {
                BalanceData balanceData = wbData.getCodeMapper().getBalanceByName(balanceName);
                balanceTypeData[balanceIndex] = wbData.getCodeMapper().getBalanceTypeById(balanceData.getBaltypId());
            } catch(Exception ex) {
                String errorMessage = "Could not determine balance Type for balance Name: " + balanceName + "; please ensure balance exists.";
                logger.error(errorMessage, ex);
                throw new IllegalArgumentException(errorMessage);
            }
        }
        if(logger.isDebugEnabled()) {
            logger.debug("Parsed parameter: " + getLogStatement("Balance Types", balanceTypeData));
        }
        return balanceTypeData;
    }

    /** @return the actual Date for the specified date parameter. */
    public Date parseBalanceDate(String balanceDateParam, WBData wbData) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(wbData.getWrksWorkDate());

        if(logger.isDebugEnabled()) {
            logger.debug("Parsed parameter: " + getLogStatement("Balance date", String.valueOf(calendar.getTime())));
        }
        return calendar.getTime();
    }



    /**
     * Override to add logic for custom balance dates.
     * @throws IllegalArgumentException by default and for all unhandled balance dates
    */
    public Date getBalanceDate(String balanceDateParam, WBData wbData) {
        throw new IllegalArgumentException("Unknown balance date: " + balanceDateParam);
    }

    /** @return the passed payoutApproachValue */
    public double parsePayoutApproachValue(String payoutApproachParam, double payoutApproachParamValue, WBData wbData) {
        if(logger.isDebugEnabled()) {
            logger.debug("Parsed parameter: " + getLogStatement("Payout approach", payoutApproachParam));
            logger.debug("Parsed parameter: " + getLogStatement("Payout approach value ", String.valueOf(payoutApproachParamValue)));
        }
        return payoutApproachParamValue;
    }

    /** Override to add additional payout approach parameters */
    public String[] getPayoutApproaches() {
        return new String[] {
            PARAM_PAYOUT_TO_FINAL
        ,    PARAM_PAYOUT_FIXED
        };
    }

    /** @throws IllegalArgumentException for an empty time code list or invalid time codes in the list. */
    public String[] parseTimeCodeNames(String timeCodeNamesParam, String[] balanceNames, WBData wbData) {
        String[] timeCodeNames = StringHelper.detokenizeString(timeCodeNamesParam, ",", true /* trims */);
        if(timeCodeNames.length == 0) {
            throw new IllegalArgumentException("Empty list of time code names received.");
        }
        if(timeCodeNames.length > 1 && timeCodeNames.length != balanceNames.length) {
            throw new IllegalArgumentException("Incorrect number of time codes specified; must be one for all balances or one for each balance");
        }
        for(int timeCodeIndex = 0 ; timeCodeIndex < timeCodeNames.length ; timeCodeIndex++) {
            String timeCodeName = timeCodeNames[timeCodeIndex];
            TimeCodeData timeCodeData = wbData.getCodeMapper().getTimeCodeByName(timeCodeName);
            if(timeCodeData == null) {
                throw new IllegalArgumentException("Unknown time code Name: " + timeCodeName);
            }
        }
        if(logger.isDebugEnabled()) {
            logger.debug("Parsed parameter: " + getLogStatement("Time code Names", timeCodeNames));
        }
        return timeCodeNames;
    }

    /** @throws IllegalArgumentException for an empty hour type list or invalid hour types in the list. */
    public String[] parseHourTypeNames(String hourTypeNamesParam, String[] balanceNames, WBData wbData) {
        String[] hourTypeNames = StringHelper.detokenizeString(hourTypeNamesParam, ",", true /* trims */);
        if(hourTypeNames.length == 0) {
            throw new IllegalArgumentException("Empty list of hour type Names received.");
        }
        if(hourTypeNames.length > 1 && hourTypeNames.length != balanceNames.length) {
            throw new IllegalArgumentException("Incorrect number of hour types specified; must be one for all balances or one for each balance");
        }
        for(int hourTypeIndex = 0 ; hourTypeIndex < hourTypeNames.length ; hourTypeIndex++) {
            String hourTypeName = hourTypeNames[hourTypeIndex];
            HourTypeData hourTypeData = wbData.getCodeMapper().getHourTypeByName(hourTypeName);
            if(hourTypeData == null) {
                throw new IllegalArgumentException("Unknown hour type Name: " + hourTypeName);
            }
        }
        if(logger.isDebugEnabled()) {
            logger.debug("Parsed parameter: " + getLogStatement("Hour type Names", hourTypeNames));
        }
        return hourTypeNames;
    }

    /** @throws IllegalArgumentException if the minutes per unit 0. */
    public double parseMinutesPerUnit(double minutesPerUnitParam, WBData wbData) {
        double minutesPerUnit = minutesPerUnitParam;
        if(minutesPerUnit < 0.0) {
            throw new IllegalArgumentException("Invalid number of minutes specified: " + minutesPerUnit);
        }
        if(logger.isDebugEnabled()) {
            logger.debug("Parsed parameter: " + getLogStatement("Minutes per unit", String.valueOf(minutesPerUnit)));
        }
        return minutesPerUnit;
    }

    /** @throws IllegalArgumentException if at least one of the passed fields is not in the list of valid additional fields.  */
    public Map parseAdditionalFields(String additionalFieldsParam, String[] validAdditionalFields, WBData wbData) {
        Map additionalFields = StringHelper.detokenizeStringAsNameValueMap(additionalFieldsParam, ",", "=", true /* trims */);
        List validFields = Arrays.asList(validAdditionalFields);
        StringBuffer invalidFields = new StringBuffer();
        Iterator fieldIterator = additionalFields.keySet().iterator();
        while(fieldIterator.hasNext()) {
            String field = String.valueOf(fieldIterator.next());
            if(!validFields.contains(field)) {
                invalidFields.append(field).append(';');
            }
        }
        if(invalidFields.length() > 0) {
            throw new IllegalArgumentException("Invalid additional fields passed: " + invalidFields.toString());
        }
        if(logger.isDebugEnabled()) {
            logger.debug("Parsed parameter: " + getLogStatement("Additional fields", additionalFields));
        }
        return additionalFields;
    }

    //---------------------------------------------------------------
    //    DEBUG
    //---------------------------------------------------------------
    protected String getLogStatement(String paramName, String paramValue) {
        StringBuffer logStatement = new StringBuffer();
        logStatement.append(paramName).append(':');
        logStatement.append(paramValue);
        logStatement.append(';');
        return logStatement.toString();
    }

    protected String getLogStatement(String paramName, int[] paramValues) {
        StringBuffer logStatement = new StringBuffer();
        logStatement.append(paramName).append(':');
        for(int paramIndex = 0 ; paramIndex < paramValues.length ; paramIndex++) {
            logStatement.append(String.valueOf(paramValues[paramIndex])).append(';');
        }
        return logStatement.toString();
    }

    protected String getLogStatement(String paramName, Object[] paramValues) {
        StringBuffer logStatement = new StringBuffer();
        logStatement.append(paramName).append(':');
        for(int paramIndex = 0 ; paramIndex < paramValues.length ; paramIndex++) {
            logStatement.append(String.valueOf(paramValues[paramIndex])).append(';');
        }
        return logStatement.toString();
    }

    protected String getLogStatement(String paramName, Map paramValues) {
        StringBuffer logStatement = new StringBuffer();
        logStatement.append(paramName).append(':');
        Object[] paramKeys = paramValues.keySet().toArray();
        for(int paramIndex = 0 ; paramIndex < paramKeys.length ; paramIndex++) {
            String paramKey = String.valueOf(paramKeys[paramIndex]);
            String paramValue = String.valueOf(paramValues.get(paramKey));
            logStatement.append(paramKey).append('=').append(paramValue).append(';');
        }
        return logStatement.toString();
    }
}
