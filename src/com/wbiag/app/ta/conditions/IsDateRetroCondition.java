package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * The condition determines whether work summary date creates retros
 *
 * @see  com.workbrain.app.ta.ruleengine.Rule
 */
public class IsDateRetroCondition extends Condition {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsDateRetroCondition.class);

    public IsDateRetroCondition() {
    }

    /**
     */
    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        return result;
    }

    /**
     * Returns the name of the Rule
     *
     * @return String - the unique name of this rule component
     */
    public String getComponentName() {
        return "WBIAG: Is Date Retro Condition";
    }

    /**
     * @param wbData - all the information <BR>
     *  parameters - RuleData parameters
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws
        Exception {

        Date wrksWorkDate = wbData.getWrksWorkDate();
        PayGroupData pg = wbData.getCodeMapper().getPayGroupById(wbData.getPaygrpId());
        boolean bolResult = DateHelper.isBetween(wrksWorkDate ,
                                                 pg.getPaygrpHandsOffDate(),
                                                 pg.getPaygrpAdjustDate());
        return bolResult;

    }

}
