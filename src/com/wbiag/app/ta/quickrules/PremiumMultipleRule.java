package com.wbiag.app.ta.quickrules;

import java.util.List;

import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;


/**
 * @author bviveiros
 *
 * Inserts a premium for each occurance of a work detail that meets the criteria.
 * Contiguous work details are counted as 1 if they meet the same criteria.
 *
 * The rule only applies to work details, it does not make sense for premiums.
 * Therefore, the parameter PARAM_USE_DETAIL_PREMIUM from the super class does not apply.
 *
 */
public class PremiumMultipleRule extends PremiumRule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PremiumMultipleRule.class);


    /* Inherits all parameters except PARAM_USE_DETAIL_PREMIUM.
	 *
	 * (non-Javadoc)
	 * @see com.workbrain.app.ta.ruleengine.Rule#getParameterInfo(com.workbrain.sql.DBConnection)
	 */
    public List getParameterInfo(DBConnection conn) {
    	List parameters = super.getParameterInfo(conn);
    	parameters.remove(new RuleParameterInfo(super.PARAM_USE_DETAIL_PREMIUM, RuleParameterInfo.STRING_TYPE));
    	return parameters;
    }

	/* (non-Javadoc)
	 * @see com.workbrain.app.ta.ruleengine.Rule#execute(com.workbrain.app.ta.ruleengine.WBData, com.workbrain.app.ta.ruleengine.Parameters)
	 */
	public void execute(WBData wbData, Parameters parameters) throws Exception {

    	// Get the rule parameters
        ParametersResolved pars = getParameters(wbData , parameters);

        CodeMapper codeMapper = wbData.getRuleData().getCodeMapper();
        int premiumMinutes = 0;
    	WorkDetailData wdCurrent = null;

    	// Count the Work Details.
        for (int k = 0, l = wbData.getRuleData().getWorkDetailCount(); k < l; k++) {

        	wdCurrent = wbData.getRuleData().getWorkDetail(k);

        	// If the current work detail is eligible
        	if (isWorkDetailEligible(wdCurrent, pars)) {

        		premiumMinutes += getMinutesInRange(wbData, wdCurrent, pars);

        		if (logger.isDebugEnabled()) {
        			logger.debug("Work Detail is eligible.  Start Time: " + wdCurrent.getWrkdStartTime());
        		}

            	// If this is the last work detail
        		// or the next work detail is not eligible, then insert the premium for this detail.
            	if ( (k+1 == l)
            			|| (k+1 < l
                            && !isWorkDetailEligible(wbData.getRuleData().getWorkDetail(k+1), pars))
						) {

            		// If a fixed premium was specified, use it.
            		if (pars.premiumUseMinutes != 0) {
            			premiumMinutes = pars.premiumUseMinutes;
            		}

            		// Insert the premium.
            		if (premiumMinutes != 0) {
            			insertPremium(wbData, premiumMinutes, pars);
            		}

            		// Reset the premium minutes counter.
            		premiumMinutes = 0;
            	}
            }
        }
	}

	/* (non-Javadoc)
	 * @see com.workbrain.app.ta.ruleengine.RuleComponent#getComponentName()
	 */
	public String getComponentName() {
		return "WBIAG: Premium Multiple Rule";
	}
}
