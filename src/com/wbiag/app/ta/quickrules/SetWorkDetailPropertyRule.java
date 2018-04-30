package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.*;

import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.StringHelper;

/**
 * Simple SetWorkDetailAttribute Rule <br>
 * Set Work Detail Fields to given value for ELIGIBLE_PROPS if evaluateExpression is satisfied<br>
 *  WrkdProperty=wrkdFlag1, WrkdValue=Y ExpressionString=wrkdUdf1=SA will set all wrkdFlag1s to Y when wkrdUdf1=SA
 */
public class SetWorkDetailPropertyRule extends Rule {

	private static final Logger logger = Logger.getLogger(SetWorkDetailPropertyRule.class);

	public static final String PARAM_WRKD_PROPERTY = "WrkdProperty";
	public static final String PARAM_WRKD_VALUE = "WrkdValue";
    public static final String PARAM_EXPRESSION_STRING = "ExpressionString";	
    public final static String PARAM_PREMIUM_DETAIL = "PremiumDetail";    
	public static final List ELIGIBLE_PROPS = new ArrayList(); 
	static {
        for (int i=1;i<=10;i++) {
            ELIGIBLE_PROPS.add("wrkdFlag" + i);
        }
        for (int i=1;i<=10;i++) {
            ELIGIBLE_PROPS.add("wrkdUdf" + i);
        }
        ELIGIBLE_PROPS.add("wrkdTcodeName");
        ELIGIBLE_PROPS.add("wrkdHtypeName");        
        ELIGIBLE_PROPS.add("wrkdJobName");        
        ELIGIBLE_PROPS.add("wrkdProjName");
        ELIGIBLE_PROPS.add("wrkdDockName");        
        ELIGIBLE_PROPS.add("wrkdDeptName");               
        ELIGIBLE_PROPS.add("wrkdWbtName");        
        ELIGIBLE_PROPS.add("wrkdRate");
        ELIGIBLE_PROPS.add("wrkdAuth");        
        ELIGIBLE_PROPS.add("wrkdAuthBy");        
        ELIGIBLE_PROPS.add("wrkdMessages");        
        ELIGIBLE_PROPS.add("wrkdComments");        
	}
	
	public List getParameterInfo(DBConnection conn) {
		List list = new ArrayList();
        RuleParameterInfo props = new RuleParameterInfo(
                PARAM_WRKD_PROPERTY, RuleParameterInfo.CHOICE_TYPE, true);
        Iterator iter = ELIGIBLE_PROPS.iterator();
        while (iter.hasNext()) {
            String item = (String)iter.next();
            props.addChoice(item);
        }   
        list.add(props); 		
        list.add(new RuleParameterInfo(PARAM_WRKD_VALUE,
				RuleParameterInfo.STRING_TYPE));
        list.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING,
                RuleParameterInfo.STRING_TYPE));        
		return list;

	}

	public void execute(WBData wbData, Parameters parameters) throws Exception {
		String property = parameters.getParameter(PARAM_WRKD_PROPERTY);
		if (!ELIGIBLE_PROPS.contains(property)) {
		    throw new RuleEngineException("Wrks Property must be one of " + ELIGIBLE_PROPS);
		}
		String val = parameters.getParameter(PARAM_WRKD_VALUE);
        String exprString = parameters.getParameter(PARAM_EXPRESSION_STRING);		
        String premiumDetail = parameters.getParameter(PARAM_PREMIUM_DETAIL);
        if (WorkDetailData.DETAIL_TYPE.equals(premiumDetail)
            || StringHelper.isEmpty(premiumDetail)){
            updateDetails(wbData.getRuleData().getWorkDetails() , 
                    exprString, property, val);
        }
        else if (WorkDetailData.PREMIUM_TYPE.equals(premiumDetail)
                 || StringHelper.isEmpty(premiumDetail)){                
            updateDetails(wbData.getRuleData().getWorkPremiums() , 
                    exprString, property, val);
        }
        else {
            throw new RuleEngineException("Premium details must be D or P or blank");
        }        
	}

	private void updateDetails(WorkDetailList wrkdList, String exprString,
	        String property, String val) {
        for (int i = 0, j = wrkdList.size(); i < j; i++) {
            WorkDetailData wd = wrkdList.getWorkDetail(i);
            if (wd.evaluateExpression(exprString)) {
                wd.setProperty(property, val);
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  {logger.debug("Set " + property + " to :" + val + " for work detail: \n" + wd); }                     
            }
        }   	    
	}
	public String getComponentName() {
		return "WBIAG: SetWorkDetailProperty Rule";
	}

}
