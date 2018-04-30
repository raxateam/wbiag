package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * The condition compares the employee's ent policy names as of calculation date with given pol name and operator <br>
 * If any of policy names satisfy condition, true is returned
 * i.e Emp. EntitlementPolicy= SICK,VACATION , <br>
 *     EntitlementPolicyString = *SICK* Operator =  WILDCARD Returns true
 *     EntitlementPolicyString = X,Y Operator =  IN Returns false
 *     EntitlementPolicyString = FT SICK Operator =  EQUALS Returns false
 *
 */
public class IsEmployeeEntPolCondition    extends Condition {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsEmployeeEntPolCondition.class);


    public static final String PARAM_ENT_POL_STRING = "EntitlementPolicyString";
    public static final String PARAM_OPERATOR = "Operator";
    public static final String PARAM_VAL_WILDCARD = "WILDCARD";
    public static final String PARAM_VAL_IN = "IN";
    public static final String PARAM_VAL_EQUALS = "EQUALS";


    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        RuleParameterInfo rpiTeam = new RuleParameterInfo(PARAM_ENT_POL_STRING,
            RuleParameterInfo.STRING_TYPE, false);
        result.add(rpiTeam);

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
        return "WBIAG: Is Employee Entitlement Policy Condition";
    }

    /**
     * @param wbData
     * @param parameters
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws
        Exception {

        Date employed = null;
        String entpolStr = parameters.getParameter(PARAM_ENT_POL_STRING);
        String operator = parameters.getParameter(PARAM_OPERATOR);
        EntitlementDataCache edc = EntitlementDataCache.
            createEntitlementDataCache(wbData.getRuleData().getCalcDataCache());
        EmployeeIdAndDateList empDates = new EmployeeIdAndDateList();
        empDates.add(wbData.getEmpId() , wbData.getWrksWorkDate());
        edc.loadPolicyIdsForEmployees(wbData.getDBconnection() , empDates);

        List polIds = edc.getPolicyIdsForEmployee(wbData.getDBconnection() ,
            wbData.getEmpId() , wbData.getWrksWorkDate());

        boolean bolTemp = false;
        Iterator iter = polIds.iterator();
        while (iter.hasNext()) {
            Integer item = (Integer)iter.next();
            int polId = item.intValue();
            String polName = wbData.getCodeMapper().getEntPolicyById(polId).getEntpolName();
            if (logger.isDebugEnabled()) logger.debug("Evaluating entpolStr=" + entpolStr + ",polName=" + polName + ",operator=" + operator);
            bolTemp = evaluate(entpolStr , polName , operator);
            if (bolTemp) {
                break;
            }
        }

        return bolTemp ;
    }

    private boolean evaluate(String teamString, String teamName, String opr) {
        if (PARAM_VAL_WILDCARD.equals(opr)) {
            return StringHelper.isWildcardMatch(teamString , teamName  ) ;
        }
        else if (PARAM_VAL_EQUALS.equals(opr)) {
            return StringHelper.equals(teamString , teamName);
        }
        else if (PARAM_VAL_IN.equals(opr)) {
            return StringHelper.isItemInList(teamString , teamName);
        }
        else {
            throw new RuntimeException("Operator not supported :" + opr);
        }
    }
}

