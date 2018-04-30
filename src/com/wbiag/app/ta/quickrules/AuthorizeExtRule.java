package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.quickrules.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;

/**
 *  Extension to core AuthorizeRule to filter overrides for  PARAM_UNAUTHORIZE_IFEDIT_AFTERAUTHORIZATION
 *
 */
public class AuthorizeExtRule extends AuthorizeRule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AuthorizeExtRule.class);

    public static final String PARAM_UNAUTHORIZE_IFEDIT_AFTERAUTHORIZATION_STRING = "UnauthorizeIfEditAfterAuthorizationString";

    public List getParameterInfo (DBConnection conn) {
        List result = super.getParameterInfo(conn) ;
        result.add(new RuleParameterInfo(PARAM_UNAUTHORIZE_IFEDIT_AFTERAUTHORIZATION_STRING, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    // *** wrong to use instance variable but there is no nicer way to extend
    String unauthorizeIfEdifAfterAuthString;
    public void execute(WBData wbData, Parameters parameters) throws Exception {
        unauthorizeIfEdifAfterAuthString = parameters.getParameter(PARAM_UNAUTHORIZE_IFEDIT_AFTERAUTHORIZATION_STRING);
        super.execute(wbData, parameters);
    }

    protected void processEditAfterAuthorization(WBData wbData) {
        WorkSummaryData summary = wbData.getRuleData().getWorkSummary();
        // *** This part does not account for the ones that have already been CANCELLED ***
        OverrideList overrides = wbData.getRuleData().getOverrides();
        if (overrides != null) {
            for (int i = 0; i < overrides.size(); i++) {
                OverrideData od = overrides.getOverrideData(i);
                boolean checkString = od.evaluateExpression(unauthorizeIfEdifAfterAuthString);
                boolean cancelled = OverrideData.CANCELLED.equals(od.getOvrStatus()) ||
                       OverrideData.CANCEL.equals(od.getOvrStatus());
                if (logger.isDebugEnabled()) logger.debug("checkString=" + checkString + ",cancelled=" + cancelled);
                if (! cancelled && checkString ) {
                    OverrideData.OverrideToken token = od.getNewOverrideByName(
                        "WRKS_AUTHORIZED");
                    if ( (token == null)
                        && (od.getOvrCreateDate().getTime() >
                         summary.getWrksAuthDate().getTime())
                        // *** if override is not created by authorizing user
                        &&
                        ! (od.getWbuName().equalsIgnoreCase(summary.getWrksAuthBy()))) {
                        summary.setWrksAuthDate(od.getOvrCreateDate());
                        summary.setWrksAuthBy("AUTO");
                        summary.setWrksAuthorized("N");
                        summary.setWrksMessages(WorkSummaryData.
                            UNAUTH_TYPE_EDITAFTERAUTHORIZATION);
                    }
                }
            }
        }
    }

    public String getComponentName() {
        return "WBIAG : Authorize Ext Rule";
    }


    public String getComponentUI() {
        return null;
    }

}

