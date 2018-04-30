package com.wbiag.app.ta.conditions;

import com.wbiag.app.ta.ruleengine.RuleHelperExt;
import com.workbrain.sql.*;
import com.workbrain.util.MultiDelimTokenizer;
import com.workbrain.util.StringHelper;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.EmployeeSchedDtlData;
import com.workbrain.app.ta.model.RecordData;

import java.util.*;
/**
 *  Title:        Is Employee Schedule Dtl Property Generic Condition
 *  Description:  Parses the PropertyString and checks whether any|all employee schedule dtl record
 *                satisfies the conditions in PropertyString <br>
 *                PropertyString is comma delimited quote enclosed string of employee table
 *                attributes where each condition is ANDed. Supports [IN] and [NOT_IN] in addition
 *                to core expressionString <br>
 *                expressionString="eschdActName[IN]CLEANING,SWEEPING","eschdJobName=JANITOR"
 *                AnyOrAll=Any <br>
 *                EmplSkdDetails <br>
 *                10:00 16:00   Job=Janitor Act=CLEANING <br>
 *                16:00 18:00   Job=Janitor Act=TRN <br>
 *                
 *                Condition returns true since 10:00 16:00 satisfies Any criterion 
 *
 */
public class IsEmpScheduleDtlPropertyGeneric extends Condition {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsEmpScheduleDtlPropertyGeneric.class);

    public final static String PARAM_EXPRESSION_STRING = "ExpressionString";
    public final static String PARAM_ANY_OR_ALL = "AnyOrAll";
    public final static String PARAM_VAL_ANY = "Any";    
    public final static String PARAM_VAL_ALL = "All";
    
    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {

        String expressionString = parameters.getParameter(PARAM_EXPRESSION_STRING);
        String anyOrAll = parameters.getParameter(PARAM_ANY_OR_ALL, PARAM_VAL_ANY);
        List skdDetails = wbData.getRuleData().getEmployeeScheduleDetails();
        boolean ret = PARAM_VAL_ANY.equals(anyOrAll) 
            ? false : (skdDetails.size() > 0 ? true : false);        
        for (Iterator iterator = skdDetails.iterator(); iterator.hasNext(); ) {
            EmployeeSchedDtlData skdDet = (EmployeeSchedDtlData) iterator.next();
            skdDet.setCodeMapper(wbData.getCodeMapper());
            boolean exp = evaluateExpression(skdDet , 
                    expressionString);
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  {logger.debug("Evaluated " + exp + " for empskDetail \n" + skdDet); }
            if (PARAM_VAL_ANY.equals(anyOrAll)) {
                ret |= exp;
                if (ret) break;
            }
            else {
                ret &= exp;                
            }
        }
        return ret;
    }

    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING, RuleParameterInfo.STRING_TYPE, false));
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_ANY_OR_ALL,
                RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(PARAM_VAL_ANY);
        rpi.addChoice(PARAM_VAL_ALL);
        result.add(rpi);        
        return result;
    }

    public String getComponentName() {
        return "WBIAG: Is Employee Schedule Detail Property Generic Condition";
    }

    public String getDescription() {
        return "Applies if an employee is scheduled to a specific shift detail or time on that day";
    }

    /** Use custom evaluateExpression as EmployeeSchedDtlData does not have Name properties 
     * i.e like eschdActName 
     * 
     * @param rd
     * @param expressionString
     * @return
     */
    private boolean evaluateExpression(EmployeeSchedDtlData rd , String expressionString) {

        if (StringHelper.isEmpty(expressionString)) return true;

        MultiDelimTokenizer st = new MultiDelimTokenizer("\",");
        st.parseInput(expressionString);
        while( st.hasNext() ) {
            String s = st.nextToken().trim();
            int i = -1;
            String opr = null;
            // **** find the operator in the token
            for (int k=0; k < RuleHelperExt.OPERATORS.length ; k++) {
                i = s.indexOf(RuleHelperExt.OPERATORS[k]);
                if (i != -1) {
                    opr = RuleHelperExt.OPERATORS[k];
                    break;
                }
            }
            if( i > 0 ) {
                String name  = s.substring(0,i);
                String val = (i+opr.length()) == s.length() ? null : s.substring( i+opr.length());
                try {
                    Object obj = null;
                    if ("eschdActName".equals(name)) {
                        obj = rd.getEschdActName();                        
                    }
                    else if ("eschdDeptName".equals(name)) {
                        obj = rd.getEschdDeptName();                        
                    }
                    else if ("eschdJobName".equals(name)) {
                        obj = rd.getEschdJobName();                        
                    }                    
                    else if ("eschdWbtName".equals(name)) {
                        obj = rd.getEschdWbtName();                        
                    }                    
                    else if ("eschdDockName".equals(name)) {
                        obj = rd.getEschdDockName();                        
                    }                    
                    else if ("eschdProjName".equals(name)) {
                        obj = rd.getEschdProjName();                        
                    }   
                    else if ("eschdTcodeName".equals(name)) {
                        obj = rd.getEschdTcodeName();                        
                    }     
                    else if ("eschdHtypeName".equals(name)) {
                        obj = rd.getEschdHtypeName();                        
                    }                      
                    else {
                        obj = rd.getProperty(name);
                    }
                    if (!RuleHelperExt.evaluateExt(obj,val,opr)) {
                        return false;
                    }
                } catch(Exception e){
                    // **** if any exception, evaluate false
                    if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Evaluate" , e);}
                    return false;
                }
            // **** if no valid operator, throw exception
            } else {
                throw new RuntimeException ("No valid operator found in the expression , "
                                            + " valid operators are : " + Arrays.asList(RuleHelper.operators));
            }
        }
        return true;
    }    
}
