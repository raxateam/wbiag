package com.wbiag.app.ta.conditions;

import com.wbiag.app.ta.ruleengine.RuleHelperExt;
import com.workbrain.sql.*;
import com.workbrain.util.MultiDelimTokenizer;
import com.workbrain.util.StringHelper;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;

import java.util.*;
/**
 *  Title:        Is Work Detail Property Generic Condition
 *  Description:  Parses the PropertyString and checks whether any|all work detail|premium record(s)
 *                satisfy(ies) the conditions in PropertyString <br>
 *                PropertyString is comma delimited quote enclosed string of employee table
 *                attributes where each condition is ANDed. Supports [IN] and [NOT_IN] in addition
 *                to core expressionString and attributes for dock, job, proj, tcode, htype, dept, wbt attributes<br>
 *                Ex. expressionString="wrkdDockName[IN]CLEANING,SWEEPING","wrkdDockName=JANITOR"
 *                ExpressionDelimiter=AND<br>                
 *                AnyOrAll=Any <br>
 *                Work Details <br>
 *                10:00 16:00   Job=Janitor Docket=CLEANING <br>
 *                16:00 18:00   Job=Janitor Docket=TRN <br>
 *                
 *                Condition returns true since 10:00 16:00 satisfies Any criterion 
 *                
 *                Ex. expressionString="wrkdFlag1=Y","wrkdFlag2=Y"
 *                ExpressionDelimiter=OR<br>                
 *                AnyOrAll=Any <br>
 *                Work Details <br>
 *                10:00 16:00   wrkdFlag1=Y wrkdFlag2=N <br>
 *                16:00 18:00   wrkdFlag1=Y wrkdFlag2=N <br>
 *                
 *                Condition returns true since at least one of the details has  wrkdFlag1=Y
 *                
 *                Ex. expressionString="wrkdDockName.dockFlag1=Y"
 *                ExpressionDelimiter=AND<br>                
 *                AnyOrAll=Any <br>
 *                Work Details <br>
 *                10:00 16:00   Docket=CLEANING <br>dock_FLAG1=Y
 *                16:00 18:00   Docket=TRN <br> dock_FLAG1=N
 *                
 *                Condition returns true because CLEANING docket has dock_FLAG1=Y
 *
 *
 */
public class IsWorkDetailPropertyGeneric extends Condition {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsWorkDetailPropertyGeneric.class);

    public final static String PARAM_EXPRESSION_STRING = "ExpressionString";
    public final static String PARAM_ANY_OR_ALL = "AnyOrAll";
    public final static String PARAM_VAL_ANY = "Any";    
    public final static String PARAM_VAL_ALL = "All";    
    public final static String PARAM_DETAIL_PREMIUM = "DetailPremium";    
    public final static String PARAM_VAL_DETAIL = "D";    
    public final static String PARAM_VAL_PREMIUM = "P";
    public final static String PARAM_EXPRESSION_STRING_LOGICAL_DELIMITER = "ExprStLogicalDelimiter";    
    public final static String PARAM_VAL_ESLD_AND = "AND";    
    public final static String PARAM_VAL_ESLD_OR = "OR";
    
    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING, RuleParameterInfo.STRING_TYPE, false));
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_ANY_OR_ALL,
                RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(PARAM_VAL_ANY);
        rpi.addChoice(PARAM_VAL_ALL);
        result.add(rpi); 
        rpi = new RuleParameterInfo(PARAM_DETAIL_PREMIUM,
                RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(PARAM_VAL_DETAIL);
        rpi.addChoice(PARAM_VAL_PREMIUM);
        result.add(rpi);           
        rpi = new RuleParameterInfo(PARAM_EXPRESSION_STRING_LOGICAL_DELIMITER,  RuleParameterInfo.CHOICE_TYPE, true);
        rpi.addChoice(PARAM_VAL_ESLD_AND);
        rpi.addChoice(PARAM_VAL_ESLD_OR);
        result.add(rpi);         
        return result;
    }
    
    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {

        String expressionString = parameters.getParameter(PARAM_EXPRESSION_STRING);
        String esLogDelimter= parameters.getParameter(PARAM_EXPRESSION_STRING_LOGICAL_DELIMITER , PARAM_VAL_ESLD_AND);
        String anyOrAll = parameters.getParameter(PARAM_ANY_OR_ALL, PARAM_VAL_ANY);
        String detailPremium = parameters.getParameter(PARAM_DETAIL_PREMIUM, PARAM_VAL_DETAIL);
        
        List details = new WorkDetailList();
        if (StringHelper.isEmpty(detailPremium) || PARAM_VAL_DETAIL.equals(detailPremium)) {
            details.addAll(wbData.getRuleData().getWorkDetails());
        }
        else if (PARAM_VAL_PREMIUM.equals(detailPremium)) {
            details.addAll(wbData.getRuleData().getWorkPremiums());            
        }
        boolean ret = PARAM_VAL_ANY.equals(anyOrAll) 
            ? false : (details.size() > 0 ? true : false);        
        for (Iterator iterator = details.iterator(); iterator.hasNext(); ) {
            WorkDetailData det = (WorkDetailData) iterator.next();
            det.setCodeMapper(wbData.getCodeMapper());
            boolean exp = evaluateExpression(wbData, det , expressionString , esLogDelimter);
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  {logger.debug("Evaluated " + exp + " for wrkDetail \n" + det); }
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

    public String getComponentName() {
        return "WBIAG: Is Work Detail Property Generic Condition";
    }

    public String getDescription() {
        return "Checks if any|all work details|premiums satisfy given expression string";
    }

    /** Use custom evaluateExpression as WorkDetailData for en
     * 
     * @param rd
     * @param expressionString
     * @return
     */
    private boolean evaluateExpression(WBData wbData, WorkDetailData rd , 
            String expressionString,
            String esLogDelimiter) {

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
                    // *** property with .
                    if (name.indexOf(".") > -1) {                    
                        if (name.toLowerCase().startsWith("wrkddockname.")) {
                            obj = wbData.getCodeMapper().getDocketByName(rd.getWrkdDockName());                        
                        }
                        else if (name.toLowerCase().startsWith("wrkddeptname.")) {
                            obj = wbData.getCodeMapper().getDepartmentByName(rd.getWrkdDeptName());                        
                        }
                        else if (name.toLowerCase().startsWith("wrkdjobname.")) {
                            obj = wbData.getCodeMapper().getJobByName(rd.getWrkdJobName());                        
                        }                    
                        else if (name.toLowerCase().startsWith("wrkdprojname.")) {
                            obj = wbData.getCodeMapper().getProjectByName(rd.getWrkdProjName());                        
                        }                    
                        else if (name.toLowerCase().startsWith("wrkdtcodename.")) {
                            obj = wbData.getCodeMapper().getTimeCodeByName(rd.getWrkdTcodeName());                        
                        }                    
                        else if (name.toLowerCase().startsWith("wrkdhtypename.")) {
                            obj = wbData.getCodeMapper().getHourTypeByName(rd.getWrkdHtypeName());                        
                        }   
                        else if (name.toLowerCase().startsWith("wrkdwbtname.")) {
                            obj = wbData.getCodeMapper().getWBTeamByName(rd.getWrkdWbtName());                        
                        }     
                    }
                    else {
                        obj = rd.getProperty(name);
                    }
                    boolean eval = RuleHelperExt.evaluateExt(obj,val,opr);
                    if (PARAM_VAL_ESLD_AND.equals(esLogDelimiter)) {
                        if (!eval) {
                            return false;
                        }
                    }
                    else if (PARAM_VAL_ESLD_OR.equals(esLogDelimiter)) {
                        if (eval) {
                            return true;
                        }                        
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
