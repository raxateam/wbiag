package com.wbiag.app.ta.quickrules;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

public class HolidayAverageRule extends Rule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HolidayAverageRule.class);

    public static final String PARAM_NUMBER_OF_DAYS = "NumberOfDays";
    public static final String PARAM_AVG_TIME_CODES = "AvgTimeCodes";
    public static final String PARAM_AVG_TIME_CODES_INCLUSIVE = "AvgTimeCodesInclusive";
    public static final String PARAM_AVG_HOUR_TYPES = "AvgHourTypes";
    public static final String PARAM_AVG_HOUR_TYPES_INCLUSIVE = "AvgHourTypesInclusive";
    public static final String PARAM_DETAIL_PREMIUM = "DetailPremium";
    public final static String PARAM_TIMECODELIST = "TimeCodeList";
    public final static String PARAM_HOLIDAY_PREMIUM_TIMECODE = "HolidayPremiumTimeCode";
    public final static String PARAM_HOLIDAY_PREMIUM_HOURTYPE = "HolidayPremiumHourType";
    public final static String PARAM_WORK_CONVERTCODE = "WorkConvertCode";
    
    //#2539477
    public static final String PARAM_MAXIMUM_PREMIUM_MINUTES = "MaximumPremiumMinutes";

    public List getParameterInfo (DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_NUMBER_OF_DAYS, RuleParameterInfo.INT_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_AVG_TIME_CODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_AVG_TIME_CODES_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_AVG_HOUR_TYPES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_AVG_HOUR_TYPES_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DETAIL_PREMIUM, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TIMECODELIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HOLIDAY_PREMIUM_TIMECODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_HOLIDAY_PREMIUM_HOURTYPE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_WORK_CONVERTCODE, RuleParameterInfo.STRING_TYPE, true));
        
        //#2539477
        result.add(new RuleParameterInfo(PARAM_MAXIMUM_PREMIUM_MINUTES, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws SQLException, Exception {
        CodeMapper codeMapper = wbData.getRuleData().getCodeMapper();

        ParametersResolved pars = new ParametersResolved();
        pars.daysBack = parameters.getIntegerParameter(PARAM_NUMBER_OF_DAYS);
        pars.avgTcodes =  parameters.getParameter(PARAM_AVG_TIME_CODES , null);
        pars.avgTcodesInclusive = Boolean.valueOf(parameters.getParameter(PARAM_AVG_TIME_CODES_INCLUSIVE, "true")).booleanValue();
        pars.avgHtypes =  parameters.getParameter(PARAM_AVG_HOUR_TYPES , null);
        pars.avgHtypesInclusive = Boolean.valueOf(parameters.getParameter(PARAM_AVG_HOUR_TYPES_INCLUSIVE, "true")).booleanValue();
        pars.detPrem = parameters.getParameter(PARAM_DETAIL_PREMIUM, null);

        String timeCodeList = parameters.getParameter(PARAM_TIMECODELIST, null);
        String holidayPremiumTimeCode = parameters.getParameter(PARAM_HOLIDAY_PREMIUM_TIMECODE);
        String holidayPremiumHourType = parameters.getParameter(PARAM_HOLIDAY_PREMIUM_HOURTYPE);
        String workConvertCode = parameters.getParameter(PARAM_WORK_CONVERTCODE, null);
        
        //#2539477
        if(parameters.getParameter(PARAM_MAXIMUM_PREMIUM_MINUTES, null) != null){
        	pars.useMaximumPremiumMinutes = true;
        	pars.maximumPremiumMinutes = parameters.getIntegerParameter(PARAM_MAXIMUM_PREMIUM_MINUTES);	
        }

        Date start = DateHelper.addDays(wbData.getWrksWorkDate() , pars.daysBack * -1);
        Date end = DateHelper.addDays(wbData.getWrksWorkDate() , -1);
        int holidayPremiumMinutes = getAvgMinutes(wbData, start, end, pars);
        
        //#2539477
        if(logger.isDebugEnabled()){
        	logger.debug("Maximum Premium Minutes :" + pars.maximumPremiumMinutes);
        }
        if(pars.useMaximumPremiumMinutes && holidayPremiumMinutes > pars.maximumPremiumMinutes) {
        	holidayPremiumMinutes = pars.maximumPremiumMinutes;
        }

        // *** If a premium code is supplied, add the premium.
        if (!StringHelper.isEmpty(holidayPremiumTimeCode)) {
            if (StringHelper.isEmpty(holidayPremiumHourType)) {
                holidayPremiumHourType = codeMapper.getHourTypeById(
                        codeMapper.getTimeCodeByName(holidayPremiumTimeCode).getHtypeId()).getHtypeName();
            }
            if (holidayPremiumMinutes != 0) {
                wbData.insertWorkPremiumRecord(holidayPremiumMinutes,
                                               holidayPremiumTimeCode,
                                               holidayPremiumHourType);
            }
        }

        // *** Convert all time codes in the TimeCodeList into WorkConvertCode,
        // *** if it (WorkConvertCode) is supplied.
        if (!StringHelper.isEmpty(workConvertCode)) {
            wbData.setWorkDetailTcodeName(workConvertCode, null, null, timeCodeList, false, false);
        }
        //** Otherwise, if clock count = 0 and there is no WRK then remove any UAT
        else if (StringHelper.isEmpty(wbData.getRuleData().getWorkSummary().getWrksClocks())) {
            Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
            if (wbData.getMinutesWorkedOnDate(workSummaryDate, workConvertCode, false) == 0) { // tt#7912
                wbData.removeWorkDetails(null, null, WBData.TCODE_UAT , true, null, true, null);
            }
        }
    }

    protected int getAvgMinutes(WBData wbData, Date start, Date end, ParametersResolved pars) throws SQLException{
        int daysWrkd = wbData.getCountWorkSummaryRange(wbData.getRuleData().getWorkSummary().
                                        getWrksId(),
                                        start, end, null, null, pars.avgTcodes,
                                        pars.avgTcodesInclusive, pars.avgHtypes,
                                        pars.avgHtypesInclusive,
                                        pars.detPrem, 1);
        int minsWrkd = wbData.getMinutesWorkDetailPremiumRange(
                                        start, end, null, null, pars.avgTcodes,
                                        pars.avgTcodesInclusive, pars.avgHtypes,
                                        pars.avgHtypesInclusive,
                                        pars.detPrem);

        if (logger.isDebugEnabled()) logger.debug("Days worked : " + daysWrkd + ", minsWorked :" + minsWrkd + " from :" + start + " to end :" + end);
        int ret = 0;
        if (daysWrkd != 0) {
            ret = minsWrkd / daysWrkd;
        }
        return ret;
    }

    public String getComponentName() {
        return "WBIAG: Holiday Average Rule";
    }

    class ParametersResolved {
        int daysBack;
        String avgTcodes;
        boolean avgTcodesInclusive;
        String avgHtypes;
        boolean avgHtypesInclusive;
        String detPrem;
        
        //#2539477
        int maximumPremiumMinutes;
        boolean useMaximumPremiumMinutes = false;

    }
}
