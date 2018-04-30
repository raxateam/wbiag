package com.wbiag.app.ta.quickrules;

// standard imports for rules
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import java.util.ArrayList;
import java.util.List;

// extra imports needed for this rule
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.util.StringHelper;
import com.workbrain.app.ta.ruleengine.RuleEngineException;
import java.sql.SQLException;

/**
 *  Title:          PremiumFromEmployeeUDFRule
 *  Description:    The rule will retrieve a value from an Employee UDF.  
 *                  If the divisor parameter is set, then the value will be 
 *                  divided by the divisor. The value will then be inserted 
 *                  as a premium.
 *  Copyright:      Copyright (c) 2003
 *  Company:        Workbrain Inc
 *
 *  @author         Kevin Tsoi
 *  @version        1.0
 */

public class PremiumFromEmployeeUDFRule extends Rule
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PremiumFromEmployeeUDFRule.class);
    
    public final static String PARAM_PREMIUM_TIME_CODE = "PremiumTimeCode";
    public final static String PARAM_PREMIUM_HOUR_TYPE = "PremiumHourType";
    public final static String PARAM_EMPLOYEE_UDF = "EmployeeUdf";
    public final static String PARAM_DIVISOR = "Divisor";

    public List getParameterInfo (DBConnection conn) 
    {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIME_CODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_HOUR_TYPE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_EMPLOYEE_UDF, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_DIVISOR, RuleParameterInfo.INT_TYPE, true));
        return result;
    }

    public void execute(WBData wbData, Parameters parameters) 
        throws SQLException, Exception 
    {
        String premiumTimeCode;
        String premiumHourType;
        String employeeUdf;
        int divisor;
        int premiumMinutes = 0;
        CodeMapper codeMapper;

        premiumTimeCode = parameters.getParameter(PARAM_PREMIUM_TIME_CODE);
        premiumHourType = parameters.getParameter(PARAM_PREMIUM_HOUR_TYPE, null);
        employeeUdf = parameters.getParameter(PARAM_EMPLOYEE_UDF);
        divisor = parameters.getIntegerParameter(PARAM_DIVISOR, 1);
        codeMapper = wbData.getRuleData().getCodeMapper();
        
        if(divisor == 0)
        {           
            throw new RuleEngineException("PremiumFromEmployeeUDFRule:  Cannot divide by zero.");
        }
        
        try
        {
            premiumMinutes = Integer.parseInt(wbData.getEmpUdfValue(employeeUdf.trim()))/divisor;
        }
        catch(Exception e)
        {
            return;
        }

        // If a premium code is supplied, add the premium.
        if (!StringHelper.isEmpty(premiumTimeCode)) 
        {
            if (StringHelper.isEmpty(premiumHourType)) 
            {
                premiumHourType = codeMapper.getHourTypeById(
                        codeMapper.getTimeCodeByName(premiumTimeCode).getHtypeId()).getHtypeName();
            }           
            wbData.insertWorkPremiumRecord(premiumMinutes, premiumTimeCode, premiumHourType);
        }
    }

    public String getComponentName() 
    {
        return "WBIAG: Premium From Employee UDF Rule";
    }
}
