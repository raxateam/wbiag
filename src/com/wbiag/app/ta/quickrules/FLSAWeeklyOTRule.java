package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;
import com.workbrain.app.ta.db.CodeMapper;
import org.apache.log4j.Logger;

/**
 * @author bviveiros
 *
 */
public class FLSAWeeklyOTRule extends Rule {

    private static Logger logger = Logger.getLogger( FLSAWeeklyOTRule.class );

    public static final String PARAM_MUST_WORKED_MINUTES = "MustWorkedMinutes";
    public static final String PARAM_FLSA_HOURS_TIMECODES = "FLSAHoursTimeCodes";
    public static final String PARAM_FLSA_HOURS_HOURTYPES = "FLSAHoursHourTypes";
    public static final String PARAM_FLSA_DOLLARS_TIMECODES = "FLSADollarsTimeCodes";
    public static final String PARAM_FLSA_DOLLARS_HOURTYPES = "FLSADollarsHourTypes";
    public static final String PARAM_OT_EARNED_TIMECODES = "OvertimeEarnedTimeCodes";
    public static final String PARAM_OT_EARNED_HOURTYPES = "OvertimeEarnedHourTypes";
    public static final String PARAM_PREMIUM_TIMECODE = "PremiumTimeCode";
    public static final String PARAM_PREMIUM_HOURTYPE = "PremiumHourType";
    public static final String PARAM_DETAILS_CALCULATED = "DetailsCalculated";

    public static final String PARAM_VAL_WD_WORK_DETAILS = "WORK DETAILS";
    public static final String PARAM_VAL_WD_WORK_PREMIUMS = "WORK PREMIUMS";
    public static final String PARAM_VAL_WD_ALL = "ALL";
        
    public final static String RPARAM_DAY_WEEK_STARTS = "system/WORKBRAIN_PARAMETERS/DAY_WEEK_STARTS";

    /* (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.Rule#execute(com.workbrain.app.ta.ruleengine.WBData, com.workbrain.app.ta.ruleengine.Parameters)
     */
    public void execute(WBData wbData, Parameters parameters) throws Exception {

    	ParametersResolved pars = getParameters(wbData, parameters);

        // End Date is today at midnight.
        pars.endTimeCurrentDay = wbData.getRuleData().getWorkDetails().getMaxEndDate();
        pars.currentDay = wbData.getWrksWorkDate();
        
        int flsaEligibleMinutes = getEligibleMinutes(wbData, pars);
        double flsaEligibleHours = flsaEligibleMinutes / 60d;

        if (logger.isDebugEnabled()) {
        	logger.debug("flsaEligibleMinutes: " + flsaEligibleMinutes);
        	logger.debug("from day: " + pars.firstDayOfWeek);
        	logger.debug("from time: " + pars.startTimeBeginWeek);
        	logger.debug("to day: " + pars.currentDay);
        	logger.debug("to time: " + pars.endTimeCurrentDay);
        }

        // Check if the employee has worked the min number of hours required for OT.
        if (flsaEligibleMinutes >= pars.mustWorkedMinutes)
        {
        	// Get the flsaEligibleDollars and otEarned.
        	parseWorkDetailsForWeek(wbData, pars);

        	// Get the employee's primary job rate
        	pars.empPrimaryJobRate = getEmpPrimaryJobRate(wbData);

        	// The formula.
            double avgRate = roundDecimal(pars.flsaEligibleDollars / flsaEligibleHours, 2);
            double flsaOTRate = pars.empPrimaryJobRate + roundDecimal(avgRate/2, 2);
            double otHours = (flsaEligibleMinutes - pars.mustWorkedMinutes) / 60d;
            double flsaEarned = roundDecimal(otHours * flsaOTRate, 2);
            double adjustmentForFLSA = roundDecimal(flsaEarned - pars.otEarned, 2);

            if (logger.isDebugEnabled()) {
            	logger.debug("----- Begin FLSA Calculation --------");
            	logger.debug("flsaEligibleHours: " + flsaEligibleHours);
            	logger.debug("flsaEligibleDollars: " + pars.flsaEligibleDollars);
            	logger.debug("avgRate: " + avgRate);
            	logger.debug("empPrimaryJobRate: " + pars.empPrimaryJobRate);
            	logger.debug("flsaOTRate: " + flsaOTRate);
            	logger.debug("otHours: " + otHours);
            	logger.debug("otEarned: " +  pars.otEarned);
            	logger.debug("flsaEarned: " + flsaEarned);
            	logger.debug("adjustmentForFLSA: " + adjustmentForFLSA);
            	logger.debug("----- End FLSA Calculation --------");
            }

            // If there is a premium to insert.
            if (adjustmentForFLSA > 0) {

            	CodeMapper codeMapper = wbData.getRuleData().getCodeMapper();

            	// Get the time code.
            	TimeCodeData tcodeData = codeMapper.getTimeCodeByName(pars.premiumTimeCode);
            	if (tcodeData == null) {
            		throw new RuntimeException("PremiumTimeCode not found: " +  pars.premiumTimeCode);
            	}
            	int timeCodeId = tcodeData.getTcodeId();

            	// Get the hour type.
            	int hourTypeId = 0;
            	if (pars.premiumHourType == null) {
            		hourTypeId = tcodeData.getHtypeId();
            	} else {
            		HourTypeData htData = codeMapper.getHourTypeByName(pars.premiumHourType);
            		if (htData == null) {
            			throw new RuntimeException("PremiumHourType not found: " +  pars.premiumHourType);
            		}
            		hourTypeId = htData.getHtypeId();
            	}

            	// Insert the premium.
        		wbData.insertWorkPremiumRecordFlatRate(
        					timeCodeId,
							hourTypeId,
							adjustmentForFLSA
                    		);
            }
        }
    }
    
    
    
    /**
     * Calculates the flsaEligibleMinutes based on input parameter
     *
     *@param wbData
     *@param pars
     */
    int getEligibleMinutes(WBData wbData, ParametersResolved pars) throws Exception{
      int eligibleMinutes = 0;
      if (StringHelper.equalsIgnoreCase(PARAM_VAL_WD_WORK_PREMIUMS, pars.detailsCalculated)){
          // get minutes from work premiums
          eligibleMinutes = wbData.getMinutesWorkPremiumRange(
                                                              pars.firstDayOfWeek,
                                                              pars.currentDay,
                                                              pars.flsaHoursTimeCodes,
                                                              true,
                                                              pars.flsaHoursHourTypes,
                                                              true);
      }
      else if (StringHelper.equalsIgnoreCase(PARAM_VAL_WD_ALL, pars.detailsCalculated)){
          // get minutes from both work premiums and work details
          eligibleMinutes = wbData.getMinutesWorkDetailPremiumRange(
                                                              pars.firstDayOfWeek,
                                                              pars.currentDay,
                                                              pars.startTimeBeginWeek,
                                                              pars.endTimeCurrentDay,
                                                              pars.flsaHoursTimeCodes,
                                                              true,
                                                              pars.flsaHoursHourTypes,
                                                              true,
                                                              null);
      }
      else {
          // get minutes from work details (default)
          eligibleMinutes = wbData.getMinutesWorkDetailRange(
                                                              pars.firstDayOfWeek,
                                                              pars.currentDay,
                                                              pars.startTimeBeginWeek,
                                                              pars.endTimeCurrentDay,
                                                              pars.flsaHoursTimeCodes,
                                                              true,
                                                              pars.flsaHoursHourTypes,
                                                              true
                                                              );
      }
      return eligibleMinutes;
    }


    /**
     * Calculates the flsaEligibleDollars and otEarned since the begin
     * of the week.
     *
     * @param wbData
     */
    private void parseWorkDetailsForWeek(WBData wbData, ParametersResolved pars) throws Exception {

    	Date dateToCheck = new Date(pars.firstDayOfWeek.getTime());
    	Map eligibleDollarsMap = new HashMap();
       	pars.otEarned = 0;

    	// For each Date, parse the work details and premiums to get the
    	// flsaEligibleDollars, and otEarned.
    	while (dateToCheck.getTime() <= wbData.getWrksWorkDate().getTime()) {
            if (StringHelper.equalsIgnoreCase(PARAM_VAL_WD_WORK_PREMIUMS, pars.detailsCalculated)){
                // only parse work premiums
                parseWorkDetailsForDay(wbData.getWorkPremiumsForDate(dateToCheck),
                                       eligibleDollarsMap, wbData.getCodeMapper()
                                       , pars);
            }
            else{
                // calculate both in other cases
                parseWorkDetailsForDay(wbData.getWorkDetailsForDate(dateToCheck),
                                       eligibleDollarsMap, wbData.getCodeMapper(), pars);
                parseWorkDetailsForDay(wbData.getWorkPremiumsForDate(dateToCheck),
                        eligibleDollarsMap, wbData.getCodeMapper()
                        , pars);
            }

    		dateToCheck = DateHelper.addDays(dateToCheck, 1);
    	}

    	// Calculate the flsaEligibleDollars from the Map.
    	// The Map key is tcode_htype, and value is a double of total dollars for the
    	// combination.
    	Iterator i = eligibleDollarsMap.values().iterator();
    	Double tcodeHtypeDollars = null;
    	pars.flsaEligibleDollars = 0;
    	while (i.hasNext()) {
    		tcodeHtypeDollars = (Double) i.next();
    		pars.flsaEligibleDollars += tcodeHtypeDollars.doubleValue();
    	}

    	// Output the values for debugging.
    	if (logger.isDebugEnabled()) {

    		i = eligibleDollarsMap.keySet().iterator();
    		String tcodeHtype = null;
    		tcodeHtypeDollars = null;

    		logger.debug("--- Begin Dollars per Tcode/Htype -----");
    		while (i.hasNext()) {
	    		tcodeHtype = (String) i.next();
	    		tcodeHtypeDollars = (Double) eligibleDollarsMap.get(tcodeHtype);
	    		logger.debug("tcodeHtype: " + tcodeHtype + ", dollars: " + tcodeHtypeDollars);
	    	}
    		logger.debug("--- End Dollars per Tcode/Htype -----");
    	}
    }


    /**
     * Calculates the flsaEligibleDollars per timeCode + hourType combination
     * and populates the Map.  Map key is timeCode_hourType, value is a Double
     * of the dollars earned.  Only creates timeCode_hourType entries for timeCodes
     * and hourTypes that count towards flsa.
     *
     * Also calculate the OT Dollars paid.
     *
     * @param detailList
     * @param eligibleDollarsMap
     */
    private void parseWorkDetailsForDay(WorkDetailList detailList,
                                        Map eligibleDollarsMap, CodeMapper codeMapper,
                                        ParametersResolved pars) {

    	WorkDetailData detail = null;
    	TimeCodeData tcodeData = null;
    	HourTypeData htypeData = null;
    	Double dollarsPerTcodeHtype = null;
    	double dollarsForWorkDetail = 0;
    	String mapKey = null;

    	for (int i=0; i < detailList.size(); i++) {

    		detail = detailList.getWorkDetail(i);

    		tcodeData = codeMapper.getTimeCodeById(detail.getTcodeId());
    		htypeData = codeMapper.getHourTypeById(detail.getHtypeId());

    		// Check if the time code and hour type are eligible for flsa dollars.
    		if (RuleHelper.isCodeInList(pars.flsaDollarsTimeCodes, tcodeData.getTcodeName())
    				&& RuleHelper.isCodeInList(pars.flsaDollarsHourTypes, htypeData.getHtypeName())
    				&& htypeData.getHtypeMultiple() != 0) {

    			// Calculate the dollars for the current work detail.
    			if (RuleHelper.isCodeInList(pars.flsaHoursTimeCodes, tcodeData.getTcodeName())) {

    				// If the timecode is also in the hours list, use this formula.
    				dollarsForWorkDetail = detail.getWrkdMinutes() / 60d
											* (detail.getWrkdRate() / htypeData.getHtypeMultiple());
    			} else {
    				dollarsForWorkDetail = detail.getWrkdMinutes() / 60d
											* detail.getWrkdRate();
    			}

    			// Look up the running total for the timcode/hourtype combination.
    			mapKey = tcodeData.getTcodeName() + "_" + htypeData.getHtypeName();
    			dollarsPerTcodeHtype = (Double) eligibleDollarsMap.get(mapKey);

    			// Add the dollars from the current work detail.
    			if (dollarsPerTcodeHtype == null) {
    				dollarsPerTcodeHtype = new Double(dollarsForWorkDetail);
    			} else {
    				dollarsPerTcodeHtype = new Double(dollarsPerTcodeHtype.doubleValue() + dollarsForWorkDetail);
    			}

    			eligibleDollarsMap.put(mapKey, dollarsPerTcodeHtype);
    		}

			// If it was eligible OT, then accumulate the OT Paid.
			if (RuleHelper.isCodeInList(pars.otEarnedTimeCodes, tcodeData.getTcodeName())
    				&& RuleHelper.isCodeInList(pars.otEarnedHourTypes, htypeData.getHtypeName())) {

				pars.otEarned += (detail.getWrkdMinutes() / 60d)
								* detail.getWrkdRate();

				if (logger.isDebugEnabled()) {
					logger.debug("wrkdStartTime: " + detail.getWrkdStartTime()
							+ "otEarned: "
							+ (detail.getWrkdMinutes() / 60d)
								* detail.getWrkdRate());
				}

			}
    	}

    }

    /**
     * Return the employee's primary job rate.
     *
     * @param wbData
     * @return
     */
    protected double getEmpPrimaryJobRate(WBData wbData) throws Exception {

    	List jobList = wbData.getEmployeeJobList();
    	EmployeeJobData jobData = null;
    	EmployeeJobData primaryJobData = null;
    	Iterator i = null;
    	double primaryJobRate = 0;

    	// Find the primary job.
    	if (jobList != null) {
    		i = jobList.iterator();
    		while (i.hasNext()) {
    			jobData = (EmployeeJobData) i.next();
    			if (jobData.getEmpjobRank() == 1){

    				// If a primary job was already found, then throw an exeception.
    				if (primaryJobData != null) {
    					throw new NestedRuntimeException("Multiple Primary Job Rates exist.");
    				} else {
    					primaryJobData = jobData;
    				}
    			}
    		}
    	}

    	// Find the job rate for the primary job.
    	if (primaryJobData != null) {
    		primaryJobRate = wbData.getJobRate(primaryJobData.getJobId(), primaryJobData.getEmpjobRateIndex(), wbData.getWrksWorkDate());
    	}

    	return primaryJobRate;
    }

    /*
     * Rounds to a number of decimal places.
     *
     * @param value
     * @param precision
     * @return
     */
    private double roundDecimal(double value, int precision) {
    	double precisionMultiple = Math.pow(10, precision);
    	return Math.round(value * precisionMultiple) / precisionMultiple;
    }


    /**
     * Get the config parameters.
     *
     * @param parameters
     */
    private ParametersResolved getParameters(WBData wbData, Parameters parameters) {
    	ParametersResolved ret = new ParametersResolved();
        // Start Date is the first day of the week.
    	// Returns the date with the time set to 00:00.
        String startDay = Registry.getVarString(RPARAM_DAY_WEEK_STARTS);
        ret.firstDayOfWeek = DateHelper.nextDay(DateHelper.addDays(wbData.getWrksWorkDate(), -7), startDay);

        // Calculate the start time of the beginning of the week.
        ret.startTimeBeginWeek = new Date(ret.firstDayOfWeek.getTime());

        ret.mustWorkedMinutes = parameters.getIntegerParameter(PARAM_MUST_WORKED_MINUTES, 0);
        ret.premiumTimeCode = parameters.getParameter(PARAM_PREMIUM_TIMECODE, null);
        ret.premiumHourType = parameters.getParameter(PARAM_PREMIUM_HOURTYPE, null);
        ret.flsaHoursTimeCodes = parameters.getParameter(PARAM_FLSA_HOURS_TIMECODES, null);
        ret.flsaHoursHourTypes = parameters.getParameter(PARAM_FLSA_HOURS_HOURTYPES, null);
        ret.flsaDollarsTimeCodes = parameters.getParameter(PARAM_FLSA_DOLLARS_TIMECODES, null);
        ret.flsaDollarsHourTypes = parameters.getParameter(PARAM_FLSA_DOLLARS_HOURTYPES, null);
        ret.otEarnedTimeCodes = parameters.getParameter(PARAM_OT_EARNED_TIMECODES, null);
        ret.otEarnedHourTypes = parameters.getParameter(PARAM_OT_EARNED_HOURTYPES, null);
        ret.detailsCalculated = parameters.getParameter(PARAM_DETAILS_CALCULATED, null);

        return ret;
    }


    /* (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.RuleComponent#getComponentName()
     */
    public String getComponentName() {
        return "WBIAG: FLSA Weekly OT Rule";
    }

    /* (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.RuleComponent#getParameterInfo(com.workbrain.sql.DBConnection)
     */
    public List getParameterInfo(DBConnection conn) {

    	List result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_MUST_WORKED_MINUTES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_FLSA_HOURS_TIMECODES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_FLSA_HOURS_HOURTYPES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_FLSA_DOLLARS_TIMECODES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_FLSA_DOLLARS_HOURTYPES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_OT_EARNED_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_OT_EARNED_HOURTYPES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_HOURTYPE, RuleParameterInfo.STRING_TYPE, true));
        
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_DETAILS_CALCULATED, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(PARAM_VAL_WD_WORK_DETAILS);
        rpi.addChoice(PARAM_VAL_WD_WORK_PREMIUMS);
        rpi.addChoice(PARAM_VAL_WD_ALL);
        result.add(rpi);

        return result;
    }


    class ParametersResolved {
        int mustWorkedMinutes = 0;
        String premiumTimeCode = null;
        String premiumHourType = null;
        String flsaHoursTimeCodes = null;
        String flsaHoursHourTypes = null;
        String flsaDollarsTimeCodes = null;
        String flsaDollarsHourTypes = null;
        String otEarnedTimeCodes = null;
        String otEarnedHourTypes = null;
        String detailsCalculated = "WORK DETAILS";

        Date firstDayOfWeek = null;
        Date startTimeBeginWeek = null;
        Date endTimeCurrentDay = null;
        double flsaEligibleDollars = 0;
        double otEarned = 0;
        double empPrimaryJobRate = 0;
        Date currentDay;
    }

}
