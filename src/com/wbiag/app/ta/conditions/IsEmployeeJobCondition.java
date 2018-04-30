package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;

/**
 * The condition compares the employee's job as of calculation date with given job string and operator <br>
 * Condition is satisfied if any of employee jobs satisfy the pattern<br>
 * i.e Emp. JOB= JANITOR<br>
 *     JobString = *JANITOR* Operator =  WILDCARD Returns true
 *     JobString = CLEANING,TRAINING Operator =  IN Returns false
 *     JobString = CLERK Operator =  EQUALS Returns false
 *
 */
public class IsEmployeeJobCondition    extends Condition {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsEmployeeJobCondition.class);


    public static final String PARAM_JOB_STRING = "JobString";
    public static final String PARAM_OPERATOR = "Operator";
    public static final String PARAM_VAL_WILDCARD = "WILDCARD";
    public static final String PARAM_VAL_IN = "IN";
    public static final String PARAM_VAL_EQUALS = "EQUALS";


    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        RuleParameterInfo rpiJob = new RuleParameterInfo(PARAM_JOB_STRING,
            RuleParameterInfo.STRING_TYPE, false);
        result.add(rpiJob);

        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_OPERATOR,
            RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(PARAM_VAL_WILDCARD);
        rpi.addChoice(PARAM_VAL_EQUALS);
        rpi.addChoice(PARAM_VAL_IN);
        result.add(rpi);

        return result;
    }

    /**
     * Returns the name of the Rule
     *
     * @return String - the unique name of this rule component
     */
    public String getComponentName() {
        return "WBIAG: Is Employee Job Condition";
    }

    /**
     *
     * @param wbData
     * @param parameters
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws
        Exception {

        String jobStr = parameters.getParameter(PARAM_JOB_STRING);
        String operator = parameters.getParameter(PARAM_OPERATOR);
        boolean bolResult = false;
        List empJobs = wbData.getRuleData().getCalcDataCache()
            .getEmployeeJobList(wbData.getEmpId(), wbData.getWrksWorkDate(),
                              wbData.getDBconnection());
        Iterator iter = empJobs.iterator();
        while (iter.hasNext()) {
            EmployeeJobData ejd = (EmployeeJobData) iter.next();
            String jobName = wbData.getCodeMapper().getJobById(ejd.getJobId()).getJobName();
            bolResult |= evaluate(jobStr , jobName , operator);
            if (logger.isDebugEnabled()) logger.debug("Job :" + jobName + ", Job String : " + jobStr + ", evaluated :" + bolResult);
            if (bolResult) break;
        }

        return bolResult ;
    }

    private boolean evaluate(String jobString, String jobName, String opr) {
        if (PARAM_VAL_WILDCARD.equals(opr)) {
            return StringHelper.isWildcardMatch(jobString , jobName  ) ;
        }
        else if (PARAM_VAL_EQUALS.equals(opr)) {
            return StringHelper.equals(jobString , jobName);
        }
        else if (PARAM_VAL_IN.equals(opr)) {
            return StringHelper.isItemInList(jobString , jobName);
        }
        else {
            throw new RuntimeException("Operator not supported :" + opr);
        }
    }
}

