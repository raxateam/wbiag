package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

public class HOPEffDateRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(HOPEffDateRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Hours of Op Effective Date");
		actionResult.setHelpTip("Hours of operation must exist for the previous year when using Trend of Historic Averages");
		actionResult.setHelpDesc("Hours of Operation have to be set for past dates when using Trend of Historic Averages. The forecast generation will look to these past dates for the Hours of Operation to determine the forecast values for those particular days. The forecast will not generate with this problem.");
		actionResult.setErrorMsg("FAILED: The following locations does not have hours of op set for previous year, or the date ranges are not continuous: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.9...");
		
			if(corpTree !=null)
		    {
				PreparedStatement stmt = null;
				ResultSet rs = null;
				
		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		        	try {
		        		ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
		        		
			            // Forecast method is "Trend of Historic Averages"
			            if( tempSkdgrpData.getSkdgrpFcastMtd() == 10 )
			            {

						String strSelect = "SELECT MIN(CORPENTHR_FROMDATE) AS FROM_DATE, MAX(CORPENTHR_TODATE) AS TO_DATE ";
						strSelect += "FROM SO_CORP_ENT_HOUR ";
	                    strSelect += "WHERE SKDGRP_ID = ? ";
	                    strSelect += "GROUP BY SKDGRP_ID ";
						stmt = conn.prepareStatement(strSelect);
	                    stmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
						rs = stmt.executeQuery();
						while( rs.next() )
						{
	    					java.sql.Date fromDate = rs.getDate( "FROM_DATE" );
	    					java.sql.Date toDate = rs.getDate( "TO_DATE" );
	    					java.util.Date currentDate = Calendar.getInstance().getTime();

	                        // Check if current date is between the defined date range
	    					if( DateHelper.isBetween( currentDate,
	    					    ( java.util.Date )fromDate,
	    					    ( java.util.Date )toDate ) )
	    				    {
	    				        if( DateHelper.getWeeksBetween( ( java.util.Date )fromDate, currentDate ) >= 60 )
	    				        {
	    				            java.sql.Date tempFromDate = null;
	    				            java.sql.Date tempToDate = null;

	                                String tempSelect = "SELECT * FROM SO_CORP_ENT_HOUR WHERE SKDGRP_ID = ?";
	                                PreparedStatement tempStmt = conn.prepareStatement( tempSelect );
	                                tempStmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
	                                ResultSet tempRS = tempStmt.executeQuery();

	            					while( tempRS.next() )
	            					{
	                					if( tempFromDate == null )
	                					{
	                					    tempToDate = rs.getDate( "CORPENTHR_TODATE" );
	                					}
	                					else
	                					{
	                    					tempFromDate = rs.getDate( "CORPENTHR_FROMDATE" );

	                    					if( DateHelper.getDifferenceInDays(
	                    					        ( java.util.Date ) tempToDate,
	                    					        ( java.util.Date ) tempFromDate ) != 1 ||
	                    					    DateHelper.compare(
	                    					        ( java.util.Date )tempToDate,
	                    					        ( java.util.Date )tempFromDate ) >= 0 )
	                    					{
	                                		    if( result.length() == 0 )
	                                		        result += tempSkdgrpData.getSkdgrpName();
	                                		    else
	                                		        result += ", " + tempSkdgrpData.getSkdgrpName();
	                        					//result += "FAILED: The date ranges are either not continuous or overlapping.";
	                    					}
	                					}
	                                }
	    				        }
	    				        else
	    				        {
	                    		    if( result.length() == 0 )
	                    		        result += tempSkdgrpData.getSkdgrpName();
	                    		    else
	                    		        result += ", " + tempSkdgrpData.getSkdgrpName();
	            					//result += "FAILED: The 'FROM' date is less than 60 weeks from the current date.";
	    				        }
	    				    }
	    				    else
	    				    {
	                		    if( result.length() == 0 )
	                		        result += tempSkdgrpData.getSkdgrpName();
	                		    else
	                		        result += ", " + tempSkdgrpData.getSkdgrpName();
	        					//result += "FAILED: Current date is outside of defined Hours of Operations date range.";
	    				    }
	    				}
		            }
	        	}
				catch(RetailException e) {
					result = e.getMessage();
				}
				catch(SQLException e) {
					result = e.getMessage();
				}
			    finally
			    {
			        SQLHelper.cleanUp(rs);
			        SQLHelper.cleanUp(stmt);
			    }
            }
        }
			
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.9");
	
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {

		return "Hours of Op Effective Date";
	}

}
