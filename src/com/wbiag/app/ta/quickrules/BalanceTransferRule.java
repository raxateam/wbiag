package com.wbiag.app.ta.quickrules;

import java.sql.*;
import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
/**
 * Simple balance transfer rule.
 * To be used when core balance transfer definitions are not usable (i.e customer didn't buy it)
 * or point of execution is significant
 */
public class BalanceTransferRule extends Rule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BalanceTransferRule.class);

    public static final String PARAM_BALANCE_TRANSFER_FROM = "BalanceTransferFrom";
    public static final String PARAM_BALANCE_TRANSFER_TO = "BalanceTransferTo";
    public static final String PARAM_BALANCE_TRANSFER_RATIO = "BalanceTransferRatio";
    public static final String PARAM_BALANCE_TRANSFER_MESSAGE = "BalanceTransferMessage";
    public static final String PARAM_VAL_BTO_DEDUCT = "*DEDUCT_BALANCE";
    public static final String PARAM_VAL_DFLT_MSG = "BalanceTransferRule";

    public List getParameterInfo (DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_BALANCE_TRANSFER_FROM, RuleParameterInfo.INT_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_BALANCE_TRANSFER_TO, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_BALANCE_TRANSFER_RATIO, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BALANCE_TRANSFER_MESSAGE, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws SQLException, Exception {
        String balFrom = parameters.getParameter(PARAM_BALANCE_TRANSFER_FROM);
        BalanceData bdFrom = wbData.getCodeMapper().getBalanceByName(balFrom)   ;
        if (bdFrom == null) {
            throw new RuntimeException ("Balance from not found :" +  balFrom);
        }
        boolean deduct = false;
        BalanceData bdTo = null;
        String balTo = parameters.getParameter(PARAM_BALANCE_TRANSFER_TO);
        if (PARAM_VAL_BTO_DEDUCT.equals(balTo)) {
            deduct = true;
        }
        else {
            bdTo = wbData.getCodeMapper().getBalanceByName(balTo)   ;
            if (bdTo == null) {
                throw new RuntimeException ("Balance to not found :" +  balTo);
            }
        }
        if (!deduct) {
            if (bdFrom.getBaltypId() != bdTo.getBaltypId()) {
                throw new RuntimeException ("BalanceTransferFrom and BalanceTransferTo must have same unit types");
            }
        }
        double ratio = parameters.getDoubleParameter(PARAM_BALANCE_TRANSFER_RATIO , 1.0);
        String balMsg = parameters.getParameter(PARAM_BALANCE_TRANSFER_MESSAGE ,
                                                PARAM_VAL_DFLT_MSG);
        double fromBalVal = wbData.getEmployeeBalanceValue(bdFrom.getBalId());
        double transfer = fromBalVal * ratio;

        wbData.addEmployeeBalanceValue(bdFrom.getBalId() , -1 * transfer, balMsg);
        if (logger.isDebugEnabled()) logger.debug("Subtracted :" + transfer + " for balance : " + bdFrom.getBalName());
        // *** do transfer if not deduct
        if (!deduct) {
            wbData.addEmployeeBalanceValue(bdTo.getBalId() , transfer, balMsg);
            if (logger.isDebugEnabled()) logger.debug("Added :" + transfer + " for balance : " + bdTo.getBalName());
        }

    }

    public String getComponentName() {
        return "WBIAG: Balance Transfer Rule";
    }

}
