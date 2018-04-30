package com.wbiag.app.modules.retailSchedule.services;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.wbiag.app.modules.retailSchedule.db.ForecastGroupExtTypeAccess;
import com.wbiag.app.modules.retailSchedule.model.ForecastGroupExtTypeData;
import com.wbiag.app.modules.retailSchedule.util.FCastGrpExtHelper;
import com.wbiag.app.modules.retailSchedule.util.FCastGrpExtMapper;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.info.ExtendedForecastInfo;
import com.workbrain.app.modules.retailSchedule.model.Forecast;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.callouts.scheduling.DefaultForecastCallout;
import com.workbrain.util.callouts.scheduling.ForecastContext;

/**
 * @author bchan
 *
 * 
 */
public class ForecastGroupExtCallout extends DefaultForecastCallout {
	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ForecastGroupExtCallout.class);

    /**
     * bchan 2/13/2005 - TT890: Extended Forecast Callout
     * 
     * @param context Context object containing objects related to forecast
     * callouts
     * @throws CalloutException If an error occurs while performing this
     * callout
     */
    public void forecastPostSave(ForecastContext context) throws CalloutException {
    	ExtendedForecastInfo efi = context.getExtendedForecastInfo();

    	try {
        	DBConnection conn = DBInterface.getCurrentConnection();
        	FCastGrpExtHelper helper = new FCastGrpExtHelper(conn);
        	Forecast f = efi.getMasterForcast();

        	int skdgrpId = ((Integer)f.getSkdgrpId()).intValue();
        	int storeId = helper.getParentStoreSkdGrpId(skdgrpId);
        	
        	Date fromDate = f.getFcastFromDate();
        	Date toDate = f.getFcastToDate();
        	
        	/*
        	 * Using the store that the forecast is part of, get all forecast group 
        	 * types for the locations below it
        	 */
        	ForecastGroupExtTypeAccess fa = new ForecastGroupExtTypeAccess(conn);
        	List fcastTypelist = fa.loadByChildSkdgrpId(storeId);

        	// continue only if departments have forecast groups which are part of
        	// extended forecast groups
        	if (fcastTypelist == null || fcastTypelist.size() == 0) return; 
        	
        	Map formulaMap = new HashMap();
        	Set allTypes = new HashSet();

        	//Parse formula and validate that each token is an existing group type
        	for (int i = 0; i < fcastTypelist.size(); i ++) {
        		ForecastGroupExtTypeData data = (ForecastGroupExtTypeData)fcastTypelist.get(i);
        		List tokenList = helper.getCalcList(data.getFcastgrptypFormula());
        		allTypes.add(data.getFcastgrptypName());
        		
        		if (tokenList == null || tokenList.size() == 0) continue;
        		
        		// pull out each token (fcastGrpType) for mass calculation
        		for (int j = 0; j < tokenList.size(); j ++) {
        			FCastGrpExtHelper.FormulaToken token = (FCastGrpExtHelper.FormulaToken)tokenList.get(j);
        			allTypes.add(token.fCastType);
        		} // for 
        		
        		if (tokenList.size() > 0) {
            		formulaMap.put(data.getFcastgrptypName(), tokenList);
        		}
        		
        	} // for 
        	
    		FCastGrpExtMapper fm = FCastGrpExtMapper.createFCastGrpExtMapper(conn);
    		try {
            	// calculate all affected forecasts for the store
        		fm.setSoFromDate(fromDate);
        		fm.setSoToDate(toDate);
        		fm.loadFcast(storeId, allTypes.iterator());
        	} catch (SQLException sqle) {
        		throw new CalloutException("Error calculating forecast values for: " + allTypes, sqle); 
    		}
        	
        	// calculate total forecast (new and existing) for each type in date range
        	Date curDate = fromDate;
        	
        	while (curDate.getTime() <= toDate.getTime()) {

        		// Calculate new and existing values for current date.  Update forecast if different.
            	for (int i = 0; i < fcastTypelist.size(); i ++) {
            		ForecastGroupExtTypeData data = (ForecastGroupExtTypeData)fcastTypelist.get(i);
            		String name = data.getFcastgrptypName();

        			int numDept = helper.getNumDeptForStoreAndFCastType(storeId, name, false);
        			if (numDept == 0) continue;  // no departments with same volume type to update

        			double newFcast = 0;
            		Object o = formulaMap.get(name);
            		if (o == null) continue; 
            		
            		List tokens = (List)o;
            		for (int j = 0; j < tokens.size(); j ++) {
            			FCastGrpExtHelper.FormulaToken token = (FCastGrpExtHelper.FormulaToken)tokens.get(j);
            			double tokenFcast = fm.getFcast(token.fCastType, curDate);
            			newFcast += token.multiple * tokenFcast;
            		}

            		double curFcast = fm.getFcast(name, curDate);
            		
            		if (helper.isFCastValDifferent(newFcast, curFcast)) {            			
            			if (curFcast > 0) {
                			double multiple = newFcast / curFcast;
                			helper.updateFCastDeptsByMultiple(storeId, name, curDate, multiple, true);
                			
                			if (logger.isDebugEnabled()) {
                				logger.debug("updated " + name + " forecast values by multiple of " + multiple + " on " + curDate);
                			}

            			} else {
            				// mimics the way core handles forecast group updates when initially 0, by dividing 
            				// new forecast amongst departments with same volume type and then updating all departments
                    		double fcastDist = newFcast / numDept; 
                			helper.updateFCastDeptsByValue(storeId, name, curDate, fcastDist, true);

                			if (logger.isDebugEnabled()) {
                				logger.debug("Forecast value for " + name + " is 0, updated forecast values to " + fcastDist + " on " + curDate);
                			}

            			}            			
            		}

            	} // for 
            	
            	curDate = DateHelper.addDays(curDate, 1);

        	} // while
        	
    	} catch (SQLException sqle) {
    		throw new CalloutException(sqle);
    	} catch (RetailException re) {
    		throw new CalloutException(re);
    	}

    }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
