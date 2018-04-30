package com.wbiag.app.ta.conditions;

import com.workbrain.sql.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.util.DateHelper;
import com.wbiag.app.ta.ruleengine.*;
import java.util.*;
/**
 *  Title:        Is Employee Property Generic Ext Condition
 *  Description:  Parses the ExpressionString and checks whether the employee
 *                satisfies the conditions in ExpressionString
 *                ExpressionString is comma delimited quote enclosed string of employee table
 *                attributes where each condition is ANDed.
 *                "empCalcGrpName=XX","empStatus!=A",empCalcgrpName[IN]A,B,C
 *                ,"empHireDate<1980-01-01 00:00:00" <br>
 *                [IN] and [NOT_IN] operators are supported in addition core operator
 *
 */
public class IsEmpPropertyGenericExt extends Condition {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsEmpPropertyGenericExt.class);

    public final static String PARAM_EXPRESSION_STRING = "ExpressionString";

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {

        String expressionString = parameters.getParameter(PARAM_EXPRESSION_STRING);
        wbData.getRuleData().getEmployeeData().setCodeMapper(wbData.getCodeMapper());
        boolean exp = RuleHelperExt.evaluateExpression(wbData.getRuleData().getEmployeeData() ,
        		expressionString) ;
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  {logger.debug("Evaluated : " + exp + " for employeeData:\n" + wbData.getRuleData().getEmployeeData()); }
        return exp;
    }

    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING, RuleParameterInfo.STRING_TYPE, false));
        return result;
    }

    public String getComponentName() {
        return "WBIAG : Is Employee Property Generic Ext Condition";
    }

    public String getDescription() {
        return "Applies if query on employee data is true on that day";
    }

}
