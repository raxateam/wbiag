package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.app.modules.retailSchedule.model.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;

/**
 *  Title:        Is Employee Property Generic Condition
 *  Description:  Parses the ExpressionString and checks whether the so_employee
 *                satifies the conditions in ExpressionString
 *                ExpressionString is comma delimited quote enclosed string of so_employee table attributes where each condition is ANDed.
 *                "sempIskeyholder=0","sempIsMinor=0",
 *
 */
public class IsSOEmpPropertyGeneric extends Condition {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsSOEmpPropertyGeneric.class);

    public final static String PARAM_EXPRESSION_STRING = "ExpressionString";
    private static final String ENTITY_CACHE_SO_EMP = "SO_EMPLOYEE";

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {

        String expressionString = parameters.getParameter(PARAM_EXPRESSION_STRING);

        Employee soEmp = getSOEmployee(wbData);
        if (soEmp == null) {
            if (logger.isDebugEnabled()) logger.debug("Couldnd't find SO_EMPLOYEE record for empId :" +  wbData.getEmpId());
            return false;
        }
        return soEmp.evaluateExpression(expressionString) ;
    }

    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING, RuleParameterInfo.STRING_TYPE, false));
        return result;
    }

    public String getComponentName() {
        return "WBIAG: Is SO Employee Property Generic Condition";
    }

    public String getDescription() {
        return "Applies if query on so_employee data is true on that day";
    }

    /**
     * Since SOEmployee is not available through WBData, stores it in short term EntityCache at first load
     * @param wbData
     * @return
     */
    private Employee getSOEmployee(WBData wbData) {
        Hashtable entityCache = wbData.getRuleData().getCalcDataCache().getEntityCache();
        Integer key = new Integer(wbData.getEmpId());
        Map soEmpMap = (Map)entityCache.get(ENTITY_CACHE_SO_EMP);
        if (soEmpMap == null) {
            soEmpMap = new HashMap();
            entityCache.put(ENTITY_CACHE_SO_EMP, soEmpMap);
        }
        Employee soEmp = (Employee)soEmpMap.get(key);
        if (soEmp == null) {
            soEmp = loadSOEmployee(wbData.getDBconnection(), wbData.getEmpId());
            soEmpMap.put(key, soEmp);
        }
        return soEmp;
    }

    private Employee loadSOEmployee(DBConnection c,int empId) {
        if (logger.isDebugEnabled()) logger.debug("Loading SO_EMPLOYEE: " + empId);
        List list = new com.workbrain.app.modules.retailSchedule.db.EmployeeAccess(c).
            loadRecordData( new Employee(),Employee.TABLE_NAME, "emp_id" , empId);
        if (list.size() > 0) {
            return (Employee) list.get(0);
        } else {
            return null;
        }
    }

}
