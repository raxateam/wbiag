package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.Distribution;
import com.workbrain.app.modules.retailSchedule.model.DistributionDetail;
import com.workbrain.app.modules.retailSchedule.model.Forecast;
import com.workbrain.app.modules.retailSchedule.model.ForecastDetail;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.utils.SODate;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;

public class NonZeroForecastRule extends ErrorDetectionRule {


	private static Logger logger = Logger.getLogger(NonZeroForecastRule.class);
	  
	public Integer getRuleType(){
		return FORECAST_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Non-zero forecast and distribution");
		actionResult.setHelpTip("This check will ensure that the distribution and forecast being used has non-zero entries");
		actionResult.setHelpDesc("No forecast or distribution should have all zero entries. This is obviously an oversight and needs to be corrected");
		actionResult.setErrorMsg("FAILED: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.10...");
		
		Vector forecastDetails;
	    Vector distributionDetails;
	    Integer forecastID = null;
	    Forecast forecast;
	    ForecastDetail fd;
	    Distribution distribution;
	    DistributionDetail dd;
	    
	    boolean ddVolumeAllZero = true;
	    boolean fdVolumeAllZero = true;
	    
	    if(corpTree !=null)
	    {
	    	PreparedStatement stmt = null;
			ResultSet rs = null;
			
	        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
	        {
	        	try {
	        		CorporateEntity cE = CorporateEntity.getCorporateEntity(new Integer((String)iter.next()));
	            	ScheduleGroupData tempSkdgrpData = cE.getSkdgrpData();
				
					String qs = "SELECT FCAST_ID FROM SO_FCAST WHERE SKDGRP_ID = " + tempSkdgrpData.getSkdgrpId().intValue();
	                stmt = conn.prepareStatement( qs );
	                
	                rs = stmt.executeQuery();
					if(rs.next()) {
					
						forecastID = new Integer(rs.getInt("FCAST_ID"));
			            
			            if(forecastID != null) {
				            forecast = new Forecast(forecastID);
				            forecastDetails = (Vector)forecast.getDetailList();
				            
				            for(int i = 0; i < forecastDetails.size(); i++) {
				            	fd = (ForecastDetail)forecastDetails.elementAt(i);
				            	distribution = fd.getDistribution();
				            	distributionDetails = (Vector)distribution.getDetailList(new SODate(fd.getFcastDate()));
	
				            	for(int j = 0; j < distributionDetails.size(); j++) {
				            		dd = (DistributionDetail)distributionDetails.elementAt(j);
				            		
				            		if(dd.getDistdetVolume() > 0) {
				            			ddVolumeAllZero = false;
				            			break;
				            		}
				            	}//end for j
				            	
				            	if(ddVolumeAllZero)
				            		result += "-Distribution for " + fd.getFcastDate().toString() + ", \"" + distribution.getDistName() + "\", is all zeros\n";
				            
				            	if(fd.getAdjustedVolume().doubleValue() > 0.0) {
				            		fdVolumeAllZero = false;
				            		break;
				            	}
				            	
				            	ddVolumeAllZero = true;
				            	if(fdVolumeAllZero && forecastDetails.size() == i+1)
				            		result += "-Forecast \"" + forecast.getFcastName() + "\" is all zeros\n";
				            }//end for i
				            
				        	fdVolumeAllZero = true;
			        	}//end if forecast ID not null
					}//end if rs.next()
		            else {
		        		result += "-No forecast associated with location \"" + tempSkdgrpData.getSkdgrpName() + "\"\n";
		        	}
	        	}//end try
	        	catch(SQLException e) {
	        		result = e.getMessage();
	        	}
	        	catch(RetailException e) {
	        		result = e.getMessage();
	        	}
	        	finally {
	        		SQLHelper.cleanUp(rs);
	    			SQLHelper.cleanUp(stmt);
	        	}
	        }//end for iter
	    }//end if org tree not null
	    
	    if(result.compareTo("") == 0)
	    	result = "OK";
	    
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.10");
	    
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		
		return "Non-zero forecast and distribution";
	}

}
