package com.wbiag.app.ta.conditions;

import java.util.*;

import org.apache.log4j.Logger;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;


/**
 *  Title:        IsShiftPatternPropertyGenericCondition
 *  Description:  Parses the ExpressionString and checks whether the shift pattern
 *                satifies the conditions in ExpressionString
 *                ExpressionString is comma delimited quote enclosed string of work summary table
 *                attributes where each condition is ANDed.
 *                "shftpatFlag1=Y","shftpatUdf1!=XX",
 */
public class IsShiftPatternPropertyGenericCondition extends Condition {

	private static Logger logger = Logger.getLogger(IsShiftPatternPropertyGenericCondition.class);

    public static final String PARAM_EXPRESSION_STRING = "ExpressionString";

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {

        String expressionString = parameters.getParameter(
            PARAM_EXPRESSION_STRING);

        boolean expr = false;
        int shftpatId = wbData.getEmployeeScheduleData().getEmpskdActShftpatId();
        ShiftPatternData spd = wbData.getCodeMapper().getShiftPatternById(shftpatId);
        if (spd == null) {
            throw new RuntimeException ("Shift pattern not found :" + shftpatId);
        }
        expr = spd.evaluateExpression(expressionString);
        if (logger.isDebugEnabled()) logger.debug("evaluated expressionString :" + expressionString + " to :" + expr);
        return  expr;
    }

    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING, RuleParameterInfo.STRING_TYPE, false));
        return result;
    }

    public String getComponentName() {
        return "WBIAG: Is Shift Pattern Property Generic Condition";
    }

}
