package com.wbiag.app.ta.quickrules;


import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.OverrideList;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
/**
 * Unauthorizes the work summary record if no applied overrides authorizing the record exist.
*/
public class UnauthorizeUntilAuthorizedRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(UnauthorizeUntilAuthorizedRule.class);
    public static final String UNAUTH_TYPE_UNAUTH_UNTIL_AUTHORIZED = "UnauthorizeUntilAuthorizedRule";

    /**
     * @see Rule
     * @param conn conn
     * @return List
     */
    public List getParameterInfo(DBConnection conn) {
        List result = new ArrayList();
        return result;
    }

    /**
     *
     * @return ComponentName
     */
    public String getComponentName() {
        return "WBIAG: UnauthorizeUntilAuthorizedRule Rule";
    }

    /**
     *     Unauthorizes the work summary record if no applied overrides authorizing the record exist.
     * @param wbData WBData
     * @param parameters parameters
     * @throws Exception
     */
    public void execute(WBData wbData, Parameters parameters) throws Exception {
		// parameters are not yet required; so no parsing to be done
		// determine if an override exists for the current day
		Date wrksWorkDate = wbData.getWrksWorkDate();
		OverrideList ovrList = wbData.getOverridesAppliedRange(wrksWorkDate,
            wrksWorkDate, OverrideData.WORK_SUMMARY_TYPE_START,
            OverrideData.WORK_SUMMARY_TYPE_END);
		boolean authOvrExists = false;
		if(ovrList != null && ovrList.size() > 0) {
			if(logger.isDebugEnabled()) { logger.debug("Matching overrides found: " + ovrList.size()); }
			int ovrListSize = ovrList.size();
			for(int ovrListIndex = 0 ; ovrListIndex < ovrListSize ; ovrListIndex++) {
				OverrideData ovrData = ovrList.getOverrideData(ovrListIndex);
				String ovrNewValue = ovrData.getOvrNewValue();
				if(ovrNewValue != null && ovrNewValue.trim().length() > 0) {
					if(logger.isDebugEnabled()) { logger.debug("ovrNewValue=[" + ovrNewValue + "]"); }
					if(ovrNewValue.indexOf("WRKS_AUTHORIZED=Y") > -1) {
						authOvrExists = true;
					}
				}
			}
		}
		if(logger.isDebugEnabled()) { logger.debug("authOvrExists=[" + authOvrExists + "]"); }
		if(!authOvrExists) {
			wbData.setWrksAuthDate(wrksWorkDate);
			wbData.setWrksAuthBy("AUTO");
			wbData.setWrksAuthorized("N");
			wbData.setWrksMessages(UNAUTH_TYPE_UNAUTH_UNTIL_AUTHORIZED);
		}
    }
}
