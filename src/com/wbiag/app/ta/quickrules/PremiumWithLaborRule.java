package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.server.registry.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;
import java.sql.SQLException;
import java.util.*;
/**
 * Premium Rule for all labor metrics and premium on premiums
 */
public class PremiumWithLaborRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PremiumWithLaborRule.class);

    public static final String PARAM_ZONE_START_TIME = "ZoneStartTime";
    public static final String PARAM_ZONE_END_TIME = "ZoneEndTime";
    public static final String PARAM_ZONE_IS_24_HOUR = "ZoneIs24Hour";
    public static final String PARAM_ELIGIBLE_TCODENAMES = "EligibleTCodeNames";
    public static final String PARAM_ELIGIBLE_HTYPENAMES = "EligibleHTypeNames";
    public static final String PARAM_ELIGIBLE_JOBNAMES = "EligibleJobNames";
    public static final String PARAM_ELIGIBLE_PROJNAMES = "EligibleProjNames";
    public static final String PARAM_ELIGIBLE_DOCKNAMES = "EligibleDockNames";
    public static final String PARAM_ELIGIBLE_DEPTNAMES = "EligibleDeptNames";
    public static final String PARAM_ELIGIBLE_WORKDETAIL_CONDITION = "EligibleWorkDetailCondition";
    public static final String PARAM_PREMIUM_TCODE_NAME = "PremiumTCodeName";
    public static final String PARAM_PREMIUM_HTYPE_NAME = "PremiumHTypeName";
    public static final String PARAM_PREMIUM_RATE = "PremiumRate";
    public static final String PARAM_PREMIUM_USE_MINUTES = "PremiumUseMinutes";
    public static final String PARAM_USE_HOURTYPE_MULTIPLIER = "UseHourTypeMultiplier";
    public static final String PARAM_USE_DETAIL_PREMIUM = "UseDetailPremium";
    public static final String PARAM_VAL_USE_DETAIL_PREMIUM_BOTH = "B";
    public static final String PARAM_VAL_USE_DETAIL_PREMIUM_DETAIL = "D";
    public static final String PARAM_VAL_USE_DETAIL_PREMIUM_PREMIUM = "P";
    public static final String PARAM_USE_DAY_MAX_MINUTES = "UseDayMaxMinutes";
    public static final String PARAM_USE_WEEK_MAX_MINUTES = "UseWeekMaxMinutes";
    public static final String PARAM_USE_DAY_MIN_MINUTES = "UseDayMinMinutes";
    public final static String PARAM_PREMIUM_LABOR_METHOD = "MethodLaborMetric";

    //TT48476 Added Applied to date param
    public static final String PARAM_APPLIY_TO = "ApplyTo";
    public static final String PARAM_EMPLOYEE_VAL = "EmployeeVal";
    public static final String PARAM_MULTIPLE = "Multiple";

    public static final String ZONE_TIME_FORMAT = "HH:mm";



    /**
     * Get the rule parameter list.
     */
    public List getParameterInfo(DBConnection conn) {

        List result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_ZONE_START_TIME, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_ZONE_END_TIME, RuleParameterInfo.STRING_TYPE));

        RuleParameterInfo applyToOptions = new RuleParameterInfo(
                PARAM_APPLIY_TO, RuleParameterInfo.CHOICE_TYPE, true);
        applyToOptions.addChoice("PreviousDay");
        applyToOptions.addChoice("CurrentDay");
        applyToOptions.addChoice("NextDay");
        result.add(applyToOptions);

        RuleParameterInfo is24hour = new RuleParameterInfo(
            PARAM_ZONE_IS_24_HOUR, RuleParameterInfo.CHOICE_TYPE, true);
        is24hour.addChoice(Boolean.TRUE.toString());
        is24hour.addChoice(Boolean.FALSE.toString());
        result.add(is24hour);

        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_TCODENAMES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_HTYPENAMES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_JOBNAMES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_PROJNAMES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_DOCKNAMES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_DEPTNAMES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_WORKDETAIL_CONDITION, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TCODE_NAME, RuleParameterInfo.STRING_TYPE , false));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_HTYPE_NAME, RuleParameterInfo.STRING_TYPE , false));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_RATE, RuleParameterInfo.STRING_TYPE , true));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_USE_MINUTES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_EMPLOYEE_VAL, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_MULTIPLE, RuleParameterInfo.STRING_TYPE, true));

        RuleParameterInfo useHtMultiplier = new RuleParameterInfo(
            PARAM_USE_HOURTYPE_MULTIPLIER, RuleParameterInfo.CHOICE_TYPE, true);
        useHtMultiplier.addChoice(Boolean.TRUE.toString());
        useHtMultiplier.addChoice(Boolean.FALSE.toString());
        result.add(useHtMultiplier);

        result.add(new RuleParameterInfo(PARAM_USE_DETAIL_PREMIUM, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_USE_DAY_MAX_MINUTES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_USE_WEEK_MAX_MINUTES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_USE_DAY_MIN_MINUTES, RuleParameterInfo.STRING_TYPE));
        RuleParameterInfo rpiLaborMthd = new RuleParameterInfo(
            PARAM_PREMIUM_LABOR_METHOD, RuleParameterInfo.CHOICE_TYPE);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_DEFAULT_METHOD);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_FIRST_METHOD);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_LAST_METHOD);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_HOUR_FOR_HOUR_METHOD);
        //rpiLaborMthd.addChoice(PARAM_PLM_VAL_PRORATED_METHOD);
        result.add(rpiLaborMthd);

        return result;
    }

    public String getComponentName() {
        return "WBIAG: Premium With Labor Rule";
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {

    	// Get the rule parameters
        ParametersResolved pars = getParameters(wbData , parameters);
        PremiumWithLabor pwl = new PremiumWithLabor();
        // Determine if we are processing Details.
        boolean doDetails = PARAM_VAL_USE_DETAIL_PREMIUM_DETAIL.equals(pars.useDetailPremium)
            					|| PARAM_VAL_USE_DETAIL_PREMIUM_BOTH.equals(pars.useDetailPremium);

        // Determine if we are processing Premiums.
        boolean doPremiums = PARAM_VAL_USE_DETAIL_PREMIUM_PREMIUM.equals(pars.useDetailPremium)
            					|| PARAM_VAL_USE_DETAIL_PREMIUM_BOTH.equals(pars.useDetailPremium);

        // If the premium minutes were given as a parameter, use it.
        if (pars.premiumUseMinutes != 0) {
        	int premiumMinutes = pars.premiumUseMinutes;
            addWrkdDates(pwl, wbData ,   DateHelper.DATE_1900 ,
                    DateHelper.DATE_1900 ,  premiumMinutes,  null, pars);
        // If the employeeVal parameter is set, then use it.
        } else if (pars.employeeVal != null) {
        	String empValStringValue = (String) wbData.getRuleData()
                .getEmployeeData().getProperty(pars.employeeVal);
        	double empValValue = 0;
        	try {
        		empValValue = Double.parseDouble(empValStringValue);
        	} catch (Exception e) {
        		throw new IllegalArgumentException("The value in: " + pars.employeeVal + " must be a double.");
        	}

        	if (pars.multiple == 0) {
        		throw new IllegalArgumentException("The parameter Multiple cannot be set to 0.");
        	}
        	int premiumMinutes = (int) (empValValue * pars.multiple);
            addWrkdDates(pwl, wbData ,   DateHelper.DATE_1900 ,
                    DateHelper.DATE_1900 ,  premiumMinutes,  null, pars);
        // Calculate the number of minutes earned for the premium.
        } else {
            /***    Change request TT1308 [START]
             *      Ali Ajellu - March 16 2006 - Solution Center
            ***/
            Date todaysDate = wbData.getWrksWorkDate();
            Date yesterdaysDate = DateHelper.addDays(todaysDate,-1);

            WorkDetailList yesterdaysOvernightWds = new WorkDetailList();
            yesterdaysOvernightWds.setCodeMapper(wbData.getCodeMapper());

            //find all work details that started yesterday and ended today.
            for (int k = 0, l = wbData.getWorkDetailsForDate(yesterdaysDate).size(); k < l; k++) {
                WorkDetailData currWdData = wbData.getWorkDetailsForDate(yesterdaysDate).getWorkDetail(k);
                if (currWdData.getWrkdEndTime().getTime() > todaysDate.getTime()){
                    yesterdaysOvernightWds.add(currWdData);
                    break;
                }
            }

            //split the list by the zone start and end.
            if (pars.startZoneWrksDate != null && pars.endZoneWrksDate != null
                    && pars.isZoneCrossMidnight == true){
                yesterdaysOvernightWds.splitAt(pars.startZoneWrksDate);
                yesterdaysOvernightWds.splitAt(pars.endZoneWrksDate);
            }

            //count eligible minutes for cross-midnight wds
            if (doDetails) {
                for (int k = 0; k < yesterdaysOvernightWds.size(); k++) {
                    WorkDetailData wd = yesterdaysOvernightWds.getWorkDetail(k);
                    if (isWorkDetailEligible(wd, pars)) {
                        getMinutesInRange(pwl, wbData, wd, pars);
                    }
                }
            }

            /***    Change request TT1308 [END] ***/


            // add all of today's WD's
            WorkDetailList todayWds = wbData.getRuleData().getWorkDetails();

	        if (doDetails) {
                //count eligible minutes for todays wds
	            for (int k = 0; k < todayWds.size(); k++) {
	                WorkDetailData wd = todayWds.getWorkDetail(k);
	                if (isWorkDetailEligible(wd, pars)) {
	                    getMinutesInRange(pwl, wbData, wd, pars);
	                }
	            }
	        }

	        // Count the Work Premiums.
	        if (doPremiums) {
	            for (int k = 0; k < wbData.getRuleData().getWorkPremiumCount(); k++) {
	                WorkDetailData wd = wbData.getRuleData().getWorkPremium(k);
	                if (isWorkDetailEligible(wd, pars)) {
	                    getMinutesInRange(pwl, wbData, wd, pars);
	                }
	            }
	        }
        }

        insertPremium(pwl, wbData, pars);
    }

    /**
     * Inserts the premium if it meets daily and weekly min/max parameters.
     *
     * @param wbData
     * @param premiumMinutes
     * @throws Exception
     */
    protected void insertPremium(PremiumWithLabor pwl, WBData wbData,
                                 ParametersResolved pars) throws Exception {

        CodeMapper codeMapper = wbData.getRuleData().getCodeMapper();
        int premiumMinutes = pwl.getWrkdDatesMinutes();
        // If the number of eligible minutes is not 0, and is greater than the daily minimum.
        if (premiumMinutes > pars.dayMinMins) {

            // Check that we are still paying the premium, after min/max validations.
            if (premiumMinutes != 0 && premiumMinutes > pars.dayMinMins) {
            	if (pars.useRate) {
            		pwl.insertWorkPremiumRecords(pars.methodLaborMetric , wbData ,
                                             codeMapper.getTimeCodeByName(pars.premiumTCode).getTcodeId(),
                                             codeMapper.getHourTypeByName(pars.premiumHType).getHtypeId(),
                                             premiumMinutes,
                                             pars.premiumRate,
                                             false, false);
            	} else {
                    HourTypeData htd = codeMapper.getHourTypeByName(pars.premiumHType);
                    double rate = htd.getHtypeMultiple() * wbData.getEmpBaseRate();

                    pwl.insertWorkPremiumRecords(pars.methodLaborMetric , wbData ,
                                             codeMapper.getTimeCodeByName(pars.premiumTCode).getTcodeId(),
                                             codeMapper.getHourTypeByName(pars.premiumHType).getHtypeId(),
                                             premiumMinutes,
                                             rate, false, false);
            	}
            }
        }
    }

    /**
     * Determine if the work detail is eligible for the premium, based on the rule parameters.
     *
     * @param wd
     * @return
     */
    protected boolean isWorkDetailEligible(WorkDetailData wd, ParametersResolved pars) {

        boolean eligible =
            RuleHelper.isCodeInList(pars.eligibleTCodeNames, wd.getWrkdTcodeName())
            &&  RuleHelper.isCodeInList(pars.eligibleHTypeNames, wd.getWrkdHtypeName())
            &&  RuleHelper.isCodeInList(pars.eligibleJobNames, wd.getWrkdJobName())
            &&  RuleHelper.isCodeInList(pars.eligibleProjNames, wd.getWrkdProjName())
            &&  RuleHelper.isCodeInList(pars.eligibleDockNames, wd.getWrkdDockName())
            &&  RuleHelper.isCodeInList(pars.eligibleDeptNames, wd.getWrkdDeptName())
            &&  RuleHelper.isCodeInList(pars.eligibleHTypeNames, wd.getWrkdHtypeName())
            && wd.evaluateExpression(pars.eligibleWorkDetailCondition)
            ;

        return eligible;
    }


    /**
     * Returns the start date of the current week.  Uses the registry parameters
     * Workbrain Parameters/DAY_WEEK_STARTS
     *
     * @param wbData
     * @return
     * @throws Exception
     */
    protected Date getWeekStart(WBData wbData) {

    	String firstDayOfWeek = (String) Registry.getVarString("system/WORKBRAIN_PARAMETERS/DAY_WEEK_STARTS" );

        Date dateWeekStarts = DateHelper.nextDay(
            DateHelper.addDays(wbData.getWrksWorkDate() , -7), firstDayOfWeek);

        return dateWeekStarts;
    }


    /**
     * Get the rule parameters into member variables.
     *
     * @param wbData
     * @param parameters
     * @throws Exception
     */
    protected ParametersResolved getParameters(WBData wbData , Parameters parameters) throws Exception {
        ParametersResolved ret = new ParametersResolved();
        CodeMapper codeMapper = wbData.getRuleData().getCodeMapper();

        ret.premiumTCode = parameters.getParameter(PARAM_PREMIUM_TCODE_NAME);
        ret.premiumHType = parameters.getParameter(PARAM_PREMIUM_HTYPE_NAME);

        // Ensure the Premium TimeCode is valid.
        if (codeMapper.getTimeCodeByName(ret.premiumTCode) == null) {
            throw new RuntimeException("PremiumTCode not found :" +  ret.premiumTCode);
        }

        // Ensure the Premium HourType is valid.
        if (codeMapper.getHourTypeByName(ret.premiumHType) == null) {
            throw new RuntimeException("PremiumHType not found :" +  ret.premiumHType);
        }

        // Get the range start and end times.
        Date zoneStartTimeParam = DateHelper.parseDate(
            parameters.getParameter(PARAM_ZONE_START_TIME, "00:00") ,
            ZONE_TIME_FORMAT);

    	Date zoneEndTimeParam = DateHelper.parseDate(
            parameters.getParameter(PARAM_ZONE_END_TIME, "00:00") ,
            ZONE_TIME_FORMAT);

        ret.startZoneTime = DateHelper.getDayFraction(zoneStartTimeParam);
        ret.endZoneTime = DateHelper.getDayFraction(zoneEndTimeParam);

        //Get apply to param, default CurrentDay
        ret.applyTo = parameters.getParameter(PARAM_APPLIY_TO);
        if (ret.applyTo == null){
            ret.applyTo = "CurrentDay";
        }
        //If the apply to param is previous then subtract one day.
        if ("PreviousDay".equals(ret.applyTo)){
            ret.startZoneTime -=DateHelper.DAY_MILLISECODS;
            ret.endZoneTime -=  DateHelper.DAY_MILLISECODS;
        }
        //If apply to param is next day then add 1 day.
        else if ("NextDay".equals(ret.applyTo)){
            ret.startZoneTime +=DateHelper.DAY_MILLISECODS;
            ret.endZoneTime +=  DateHelper.DAY_MILLISECODS;
        }

        // If the times cross midnight, then add 1 day to the end time.
        if (ret.endZoneTime < ret.startZoneTime) {
            ret.endZoneTime += DateHelper.DAY_MILLISECODS;
        }

        // If the start and end times are equal, and rule param says crosses midnight, then
        // add 1 day to the end time.
        ret.isZoneCrossMidnight = Boolean.valueOf(parameters.getParameter(PARAM_ZONE_IS_24_HOUR, Boolean.FALSE.toString())).booleanValue();
        if (ret.isZoneCrossMidnight && (ret.endZoneTime == ret.startZoneTime)) {
            ret.endZoneTime += DateHelper.DAY_MILLISECODS;
        }

        // Split the work details at the start and end times.
        if (ret.startZoneTime != ret.endZoneTime) {
	        ret.startZoneWrksDate = new Date(wbData.getWrksWorkDate().getTime() + ret.startZoneTime);
	        ret.endZoneWrksDate = new Date(wbData.getWrksWorkDate().getTime() + ret.endZoneTime);
	        wbData.getRuleData().getWorkDetails().splitAt(ret.startZoneWrksDate);
	        wbData.getRuleData().getWorkDetails().splitAt(ret.endZoneWrksDate);
        }

        // Get the remaining parameters.
    	ret.useHtypeMultiplier = Boolean.valueOf(
            parameters.getParameter(PARAM_USE_HOURTYPE_MULTIPLIER, Boolean.FALSE.toString())).booleanValue();

        ret.eligibleTCodeNames = parameters.getParameter(PARAM_ELIGIBLE_TCODENAMES);
        ret.eligibleHTypeNames = parameters.getParameter(PARAM_ELIGIBLE_HTYPENAMES);
        ret.eligibleJobNames = parameters.getParameter(PARAM_ELIGIBLE_JOBNAMES);
        ret.eligibleProjNames = parameters.getParameter(PARAM_ELIGIBLE_PROJNAMES);
        ret.eligibleDockNames = parameters.getParameter(PARAM_ELIGIBLE_DOCKNAMES);
        ret.eligibleDeptNames = parameters.getParameter(PARAM_ELIGIBLE_DEPTNAMES);
        ret.eligibleWorkDetailCondition = parameters.getParameter(PARAM_ELIGIBLE_WORKDETAIL_CONDITION);

        if (parameters.getParameter(PARAM_PREMIUM_RATE, null) != null) {
        	ret.useRate = true;
        	ret.premiumRate = parameters.getDoubleParameter(PARAM_PREMIUM_RATE);
        }

        ret.premiumUseMinutes = parameters.getIntegerParameter(PARAM_PREMIUM_USE_MINUTES , 0);
        ret.dayMaxMins = parameters.getIntegerParameter(PARAM_USE_DAY_MAX_MINUTES , Integer.MIN_VALUE);
        ret.weekMaxMins = parameters.getIntegerParameter(PARAM_USE_WEEK_MAX_MINUTES , Integer.MIN_VALUE);
        ret.dayMinMins = parameters.getIntegerParameter(PARAM_USE_DAY_MIN_MINUTES , 0);
        ret.useDetailPremium = parameters.getParameter(PARAM_USE_DETAIL_PREMIUM , PARAM_VAL_USE_DETAIL_PREMIUM_DETAIL);
        ret.employeeVal = parameters.getParameter(PARAM_EMPLOYEE_VAL, null);
        ret.multiple = parameters.getDoubleParameter(PARAM_MULTIPLE, 1);
        ret.methodLaborMetric = parameters.getParameter(PARAM_PREMIUM_LABOR_METHOD,
            PremiumWithLabor.PARAM_PLM_VAL_DEFAULT_METHOD );
        return ret;
    }


    /**
     * For the given work details, returns the number of minutes that fall within
     * the given time range.
     *
     * @param wbData
     * @param wd
     * @return
     * @throws SQLException
     */
    protected int getMinutesInRange(PremiumWithLabor pwl, WBData wbData,
                                    WorkDetailData wd,
                                    ParametersResolved pars) throws SQLException{

        int minutes = 0;

    	// If this is a premium, then just return the number of minutes.
    	if (WorkDetailData.PREMIUM_TYPE.equals(wd.getWrkdType())) {
            minutes = addWrkdDates(pwl, wbData ,
                    DateHelper.DATE_1900 ,
                    DateHelper.DATE_1900 ,
                    wd.getWrkdMinutes(),
                    wd, pars
                    );
            return minutes ;
        }

    	// If a time range was given, and the work detail does not fall withing the range,
    	// then return 0.
        if (pars.startZoneWrksDate != null && pars.endZoneWrksDate != null) {
            if (wd.getWrkdStartTime().before(pars.startZoneWrksDate)
            		|| wd.getWrkdEndTime().after(pars.endZoneWrksDate)) {
                return 0;
            }
        }

        // If a time range was not given then use the full duration of the work details.
        if (pars.startZoneTime == pars.endZoneTime) {
            minutes = (int) ((wd.getWrkdEndTime().getTime() - wd.getWrkdStartTime().getTime())
                             		/ DateHelper.MINUTE_MILLISECODS);
            addWrkdDates(pwl, wbData , wd.getWrkdStartTime(),
                         wd.getWrkdEndTime(), minutes, wd, pars);
        // If a time range was given, limit the minutes to within the range.
        } else {

            long diffStart = 0;
            long diffEnd = 0;
            CodeMapper codeMapper = wbData.getRuleData().getCodeMapper();

	        long startWorkTime = DateHelper.getDayFraction(wd.getWrkdStartTime());
	        long endWorkTime = DateHelper.getDayFraction(wd.getWrkdEndTime());

	        // Add 1 day to end time if work detail crosses midnight.
        	if (endWorkTime < startWorkTime) {
                endWorkTime += DateHelper.DAY_MILLISECODS;
            }

        	// Use the times against yesterday, today, and tomorrow dates to cover cross midnight scenarios.
        	// If the work detail falls within one of the ranges, use the minutes.
            for (int i = -1; i < 2; i++) {

            	diffStart = Math.max(pars.startZoneTime + i * DateHelper.DAY_MILLISECODS,
            							startWorkTime);

            	diffEnd = Math.min(pars.endZoneTime + i * DateHelper.DAY_MILLISECODS,
            							endWorkTime);

                if (diffEnd > diffStart) {
                    if (pars.useHtypeMultiplier) {
                        minutes += ((diffEnd - diffStart)
                                    * codeMapper.getHourTypeById(wd.getHtypeId()).getHtypeMultiple()) / DateHelper.MINUTE_MILLISECODS;
                    } else {
                        minutes += (diffEnd - diffStart) / DateHelper.MINUTE_MILLISECODS;
                    }
                    minutes = addWrkdDates(pwl, wbData ,
                            new Date((DateHelper.truncateToDays(wd.getWrkdStartTime())).getTime() + diffStart),
                            new Date((DateHelper.truncateToDays(wd.getWrkdStartTime())).getTime() + diffEnd),
                            (int) ((diffEnd - diffStart) / DateHelper.MINUTE_MILLISECODS),
                            wd, pars
                            );
                }
            }
        }

        if (logger.isDebugEnabled()) {
        	logger.debug("Minutes in Range: " + minutes);
        }

        return minutes;
    }

    private int addWrkdDates( PremiumWithLabor pwl,
                              WBData wbData,
                              Date startDate, Date endDate, int minutes,
                              WorkDetailData wd,
                              ParametersResolved pars) throws SQLException {

        int premiumMinutesCapped = minutes;

        // If a daily max premium was specified, then enforce it.
        if (pars.dayMaxMins != Integer.MIN_VALUE) {

            if (logger.isDebugEnabled())  logger.debug("Premium minutes : " + minutes + " - Day max minutes:" + pars.dayMaxMins);
            // Do not allow greater than dayMaxMins.
            int totMinsSofar = pwl.getWrkdDatesMinutes();
            int totMinsWithThis = totMinsSofar + minutes;
            if (totMinsWithThis > pars.dayMaxMins) {
                premiumMinutesCapped = minutes - (totMinsWithThis - pars.dayMaxMins);
                endDate = DateHelper.addMinutes(endDate, minutes - premiumMinutesCapped);
            }
        }

        // If a weekly max premium was specified, then enforce it.
        if (pars.weekMaxMins != Integer.MIN_VALUE) {

            // Get the number of minutes so far this week for this premium.
            Date weekStart = getWeekStart(wbData);
            int weekMins = wbData.getMinutesWorkDetailPremiumRange(
                                            weekStart ,
                                            DateHelper.max(DateHelper.addDays(wbData.getWrksWorkDate() , -1) , weekStart) ,
                                            null, null,
                                            pars.premiumTCode , true,
                                            pars.premiumHType , true,
                                            WorkDetailData.PREMIUM_TYPE);

            // Minutes this week, plus the minutes we are planning to award.
            int minsIncludingCurrent = weekMins + premiumMinutesCapped;

            if (logger.isDebugEnabled()) {
                logger.debug("Week Premium minutes up to current date: " + minsIncludingCurrent + " - Week max minutes:" + pars.weekMaxMins);
            }

            // If the total will be greater than the max, then cap the premium at the max.
            if (minsIncludingCurrent > pars.weekMaxMins) {
                premiumMinutesCapped =  Math.max(pars.weekMaxMins - weekMins, 0) ;
                endDate = DateHelper.addMinutes(endDate, minutes - premiumMinutesCapped);
            }
        }
        if (premiumMinutesCapped > 0) {
            pwl.addWrkdDates(startDate, endDate, premiumMinutesCapped, wd);
        }
        return premiumMinutesCapped;
    }

    public class ParametersResolved {
        public String premiumTCode = null;
        public String premiumHType = null;
        public boolean useHtypeMultiplier = false;
        public boolean isZoneCrossMidnight = false;
        public long startZoneTime;
        public long endZoneTime;
        public String applyTo;
        public Date startZoneWrksDate;
        public Date endZoneWrksDate;
        public String eligibleTCodeNames;
        public String eligibleHTypeNames;
        public String eligibleJobNames;
        public String eligibleProjNames;
        public String eligibleDockNames;
        public String eligibleDeptNames;
        public String eligibleWorkDetailCondition;
        public double premiumRate = 0;
        public int premiumUseMinutes = 0;
        public int dayMaxMins = 0;
        public int weekMaxMins = 0;
        public int dayMinMins = 0;
        public String useDetailPremium = null;
        public String employeeVal = null;
        public double multiple = 0;
        public boolean useRate = false;
        public String  methodLaborMetric;

    }

}
