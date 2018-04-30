package com.wbiag.app.ta.ruleengine;

import java.util.Arrays;
import java.util.StringTokenizer;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.RecordData;
import com.workbrain.util.*;


public class RuleHelperExt {
    
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RuleHelperExt.class);

    /**
     *  operator constants
     */
    public final static String IN       = "[IN]";
    public final static String NOT_IN       = "[NOT_IN]";    
    public final static String[] OPERATORS_EXT = new String[] {IN , NOT_IN};    
    public final static String[] OPERATORS = new String[RuleHelper.operators.length + OPERATORS_EXT.length ];
    static {
        System.arraycopy(RuleHelper.operators, 0, OPERATORS, 0, RuleHelper.operators.length);
        System.arraycopy(OPERATORS_EXT, 0, OPERATORS, RuleHelper.operators.length, OPERATORS_EXT.length);        
    }
    
    protected RuleHelperExt () {}
    
    /**
     *  evaluates two objects according to given operator
     *  uses core method for all operators except [IN] and [NOT_IN]
     *
     *@param  operand1       operand1 object
     *@param  operand2       operand2 object
     *@param  opr            operator
     *@throws Exception
     *@return                boolean
     */
    public static boolean evaluateExt(Object operand1,Object operand2,String opr)
                                  throws Exception{
        // *** for all operators but IN use core method
        if (!opr.equals(IN) && !opr.equals(NOT_IN)) {
            return RuleHelper.evaluate(operand1, operand2, opr) ;
        }

        // *** if operand2 != null, operand1 == null, assumed all items
        if (operand1 == null && operand2 != null) {
            return opr.equals(IN) ? true : false;
        }
        // *** if operand1 != null
        else if (operand1 != null && operand2 == null) {
            return false;
        }
        else {
        	return StringHelper.isItemInList((String)operand2, (String)operand1);
        }
    }
    
    /**
     * evaluates record data according to given expressionString
     * expressionString has to be comma delimited quote enclosed. <p>
     * supports [IN],[NOT_IN] in addition core operands
     * i.e  "wrkdUdf1=N","wrkdDeptName!=XX","wrkdStartTime<2002-08-31 06:00:00",wrkdFlag1[IS_EMPTY]
     * ,"wrkdJobName[IN]J1,J2
     * <ul>
     * <li> dates have to be supplied in SQL_TIMESTAMP_FORMAT
     * which are yyyy-MM-dd , yyyy-MM-dd HH:mm:ss, HH:mm:ss and yyyy-MM-dd HH:mm.
     * For values supplied in HH:mm:ss format, date comparison will be made based on day fraction minutes.
     * <li> supported operators are =,!=,<>,<,<=,>,>=,[IS_EMPTY],[IS_NOT_EMPTY].
     * For [IS_EMPTY],[IS_NOT_EMPTY] second operand is not required and ignored
     * <li> evaluates false if token is not well formatted, property doesnot exist
     *  or types are not compatible
     * </ul>
     *
     * @param expressionString expression string comma delimited , optionally quote enclosed
     * @return boolean
     */    
    public static boolean evaluateExpression(RecordData rd , String expressionString) {

        if (StringHelper.isEmpty(expressionString)) return true;

        MultiDelimTokenizer st = new MultiDelimTokenizer("\",");
        st.parseInput(expressionString);
        //StringTokenizer st = new StringTokenizer(expressionString, "\",");
        while( st.hasNext() ) {
            String s = st.nextToken().trim();
            int i = -1;
            String opr = null;
            // **** find the operator in the token
            for (int k=0; k < OPERATORS.length ; k++) {
                i = s.indexOf(OPERATORS[k]);
                if (i != -1) {
                    opr = OPERATORS[k];
                    break;
                }
            }
            if( i > 0 ) {
                String name  = s.substring(0,i);
                String val = (i+opr.length()) == s.length() ? null : s.substring( i+opr.length());
                try {
                    Object obj = rd.getProperty(name);
                    if (!evaluateExt(obj,val,opr)) {
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