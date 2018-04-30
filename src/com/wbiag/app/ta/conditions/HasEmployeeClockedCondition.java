package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;
/**
 * Condition to check if employee clocked in
 */
public class HasEmployeeClockedCondition extends Condition {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HasEmployeeClockedCondition.class);

    public static final String PARAM_CLOCK_MODE = "ClockMode";
    public static final String PARAM_VAL_CLOCK_MODE_ALL = "ALL";
    public static final String PARAM_VAL_CLOCK_MODE_READER = "READER";

    public static final String PARAM_COMPARE_TIME_OPERATOR = "CompareTimeOperator";
    public static final String PARAM_COMPARE_TIME = "CompareTime";
    public static final String COMPARE_TIME_FMT = "HH:mm";

    public static final String PARAM_CLOCK_TYPES = "ClockTypes";
    public static final String PARAM_CLOCK_DATA_NAME = "ClockDataName";
    public static final String PARAM_CLOCK_DATA_VALUES = "ClockDataValues";

    // *** add empskd times as eligible vals
    private static final ArrayList PARAM_VALS_COMP_TIME = new ArrayList() ;
    static {
        for (int i=0 ; i < 5 && i != 1; i++) {
             PARAM_VALS_COMP_TIME.add(EmployeeScheduleData.EMPSKD_PREFIX + "_ACT_START_TIME" + (i==0 ? "" : String.valueOf(i)));
             PARAM_VALS_COMP_TIME.add(EmployeeScheduleData.EMPSKD_PREFIX + "_ACT_END_TIME" + (i==0 ? "" : String.valueOf(i)));
             PARAM_VALS_COMP_TIME.add(EmployeeScheduleData.EMPSKD_PREFIX + "_DEF_START_TIME" + (i==0 ? "" : String.valueOf(i)));
             PARAM_VALS_COMP_TIME.add(EmployeeScheduleData.EMPSKD_PREFIX + "_DEF_END_TIME" + (i==0 ? "" : String.valueOf(i)));
        }
    }


        public List getParameterInfo( DBConnection conn ) {
            List result = new ArrayList();
            result.add(new RuleParameterInfo(PARAM_CLOCK_MODE, RuleParameterInfo.STRING_TYPE, false));

            RuleParameterInfo rpi = new RuleParameterInfo(PARAM_COMPARE_TIME_OPERATOR,
                RuleParameterInfo.CHOICE_TYPE);
            rpi.addChoice(RuleHelper.EQ);
            rpi.addChoice(RuleHelper.LESS);
            rpi.addChoice(RuleHelper.BIGGER);
            rpi.addChoice(RuleHelper.LESSEQ);
            rpi.addChoice(RuleHelper.BIGGEREQ);
            rpi.addChoice(RuleHelper.NOTEQ1);
            result.add(rpi);

            result.add(new RuleParameterInfo(PARAM_COMPARE_TIME, RuleParameterInfo.STRING_TYPE));
            result.add(new RuleParameterInfo(PARAM_CLOCK_TYPES, RuleParameterInfo.STRING_TYPE));
            result.add(new RuleParameterInfo(PARAM_CLOCK_DATA_NAME, RuleParameterInfo.STRING_TYPE));
            result.add(new RuleParameterInfo(PARAM_CLOCK_DATA_VALUES, RuleParameterInfo.STRING_TYPE));

            return result;
        }

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {

        String mode = parameters.getParameter(PARAM_CLOCK_MODE);
        if (!PARAM_VAL_CLOCK_MODE_ALL.equals(mode)
            && !PARAM_VAL_CLOCK_MODE_READER.equals(mode)) {
            throw new RuleEngineException ("Clock Mode must be ALL or READER");
        }
        boolean ret = false;

        List clocks = null;
        if (PARAM_VAL_CLOCK_MODE_ALL.equals(mode)) {
            clocks = wbData.getClocks();
        }
        else if (PARAM_VAL_CLOCK_MODE_READER.equals(mode)) {
            clocks = wbData.getClocksOriginal();
        }

        Date compTime = resolveCompareTime(wbData , parameters);
        String oper = parameters.getParameter(PARAM_COMPARE_TIME_OPERATOR);
        if (compTime != null && StringHelper.isEmpty(oper) ) {
            throw new RuleEngineException ("Operator must be defined if CompareTime is specified");
        }

        String clockTypes = parameters.getParameter(PARAM_CLOCK_TYPES);
        String clockDataName = parameters.getParameter(PARAM_CLOCK_DATA_NAME);
        String clockDataVals = parameters.getParameter(PARAM_CLOCK_DATA_VALUES);
        // *** loop through clocks for these parameters
        Iterator iter = clocks.iterator();
        while (iter.hasNext()) {
            Clock clk = (Clock)iter.next();
            if (compTime != null) {
                boolean evalCompTime = RuleHelper.evaluate(DateHelper.toDatetime(clk.getClockDate()), compTime, oper);
                if (!evalCompTime) {
                    iter.remove();
                    if (logger.isDebugEnabled())  logger.debug("Clock \n" + clk +  " was not eligible for compare time : " + compTime);
                    continue;
                }
            }
            if (!StringHelper.isEmpty(clockTypes)) {
                boolean evalClockTypes = RuleHelper.isCodeInList(clockTypes,  String.valueOf(clk.getClockType()));
                if (!evalClockTypes) {
                    iter.remove();
                    if (logger.isDebugEnabled())
                        logger.debug("Clock \n" + clk + " was not eligible for clock types : " +  clockTypes);
                    continue;
                }
            }

            boolean evalClockData = true;
            if (!StringHelper.isEmpty(clockDataName)) {
                String val = clk.getClockDataName(clockDataName);
                evalClockData = !StringHelper.isEmpty(val) && RuleHelper.isCodeInList(clockDataVals , val);
                if (!evalClockData) {
                    iter.remove();
                    if (logger.isDebugEnabled()) logger.debug("Clock \n" + clk + " was not eligible for clock data : " + clockDataName + " and values :" + clockDataVals);
                    continue;
                }
            }

        }

        ret = clocks.size() > 0;
        return  ret;
    }

    private Date resolveCompareTime(WBData wbData, Parameters parameters) throws RuleEngineException{
        String compTimeParam = parameters.getParameter(PARAM_COMPARE_TIME);
        Date compTime = null;
        if (!StringHelper.isEmpty(compTimeParam)) {
            compTimeParam = compTimeParam.toUpperCase();
            if (compTimeParam.startsWith(EmployeeScheduleData.EMPSKD_PREFIX)) {
                if (!PARAM_VALS_COMP_TIME.contains(compTimeParam)) {
                    throw new RuleEngineException(
                        "Compare time must be in empskdAct|DefStart|EndTime[ 2..5] format if starts with " + EmployeeScheduleData.EMPSKD_PREFIX);
                }
                compTime = (Date) wbData.getEmployeeScheduleData().getField(
                    compTimeParam);
            }
            // *** constant time
            else {
                try {
                    compTime = DateHelper.convertStringToDate(compTimeParam,
                        COMPARE_TIME_FMT);
                }
                catch (Exception ex) {
                    throw new RuleEngineException("Error in converting CompareTime : " + compTimeParam + ". Must be in " + COMPARE_TIME_FMT + " format");
                }
            }
        }
        return compTime;
    }

    public String getComponentName() {
        return "WBIAG: Has Employee Clocked Condition";
    }

}


