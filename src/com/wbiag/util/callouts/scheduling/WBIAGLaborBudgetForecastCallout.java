package com.wbiag.util.callouts.scheduling;

import java.io.IOException;
import java.util.*;
import java.text.*;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;

import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.util.*;
import com.workbrain.util.callouts.scheduling.ForecastContext;
import com.workbrain.util.callouts.scheduling.DefaultForecastCallout;
import com.wbiag.app.modules.budgeting.wbinterface.LongTermBudgetUtility;

import com.workbrain.app.modules.retailSchedule.info.ExtendedForecastInfo;
import com.workbrain.server.jsp.JSPHelper;
import com.workbrain.app.ta.db.*;
import com.workbrain.sql.*;
import com.workbrain.server.jsp.taglib.util.*;

public class WBIAGLaborBudgetForecastCallout extends DefaultForecastCallout {

	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( WBIAGLaborBudgetForecastCallout.class );
	
    private static final String PREFIX = "CUST_forecastPage_";
    private String LABOR_FORECAST_HEADER_LBL = "LABOR_FORECAST_HEADER_LBL";
    private String LF_NO_TIMEPERIOD_ERROR_MSG_LBL = "LF_NO_TIMEPERIOD_ERROR_MSG_LBL";
    private static final int MFRM_ID = 828;
	
    /**
     * Calls <code>LongTermBudgetSOCallout.outputLaborBudgetUITable()</code>
     * to output addition UI output to the appropriate location on the forecast
     * page. The output is for the table containing the control fields for
     * setting the individual payroll budget percentages.
     *
     * @param context Context object containing objects related to forecast
     * callouts
     * @throws CalloutException If an error occurs while performing this
     * callout
     */
    public void forecastPageLayerWbManagerDataAfter(ForecastContext context) throws CalloutException {
        try {
        	logger.debug("***********writing out the UI table");
            this.outputLaborBudgetUITable(context);
        } catch (WBException wbe) {
            logger.error(wbe.getMessage(), wbe);
            throw new CalloutException(wbe);
        } catch (SQLException sqle) {
            logger.error(sqle.getMessage(), sqle);
            throw new CalloutException(sqle);
        }  
    }
    
    public void outputLaborBudgetUITable(ForecastContext context) throws WBException, SQLException {
    	
        HttpServletRequest request = context.getHttpServletRequest();
        JspWriter out = context.getJspWriter();
        DBConnection dbConn = JSPHelper.getConnection(request);
        
        Date fromDateDt = DateHelper.convertStringToDate( (String) request.getSession().getAttribute("fcast_extended_details.htm::StartDate"), "MMM dd, yyyy" );
        Date endDateDt = DateHelper.convertStringToDate( (String) request.getSession().getAttribute("fcast_extended_details.htm::EndDate"), "MMM dd, yyyy" );
        String locName = (String) request.getSession().getAttribute("fcast_extended_details.htm::LocationName");
        
        //ExtendedForecastInfo info = context.getExtendedForecastInfo();
        Calendar fromDate = DateHelper.toCalendar(fromDateDt);
        Calendar toDate = DateHelper.toCalendar(endDateDt);
        Integer locId = CodeMapper.createCodeMapper(dbConn).getScheduleGroupByName(locName).getSkdgrpId();

        logger.debug(" ^^^^^^^^^^^^^^^^^^ locId " + locId);
        logger.debug(" ^^^^^^^^^^^^^^^^^^ From " + fromDate.getTime());
        logger.debug(" ^^^^^^^^^^^^^^^^^^ To " + toDate.getTime());

    	LongTermBudgetUtility ltbu = new LongTermBudgetUtility();
    	if (	!ltbu.areFcstDatesAndLabBudgDatesAligned(fromDate, toDate, dbConn) ) {

            try {

            	LocalizationHelper localHelper = new LocalizationHelper(context.getPageContext());
            	
            	String localizedLbl = localHelper.getCaption(LF_NO_TIMEPERIOD_ERROR_MSG_LBL, LocalizableTag.CONFIG_MESSAGE, MFRM_ID, "ERROR: The LFSO Forecast Start and End Date must be aligned with the Labor Budgeting Time periods Start and End Dates.");
            	
                out.print("<tr><td>");
                out.print("<table border='0' cellpadding='2' width='250px'");
                out.print("cellspacing='0' class='contentTable finance'>");
                out.print("<tr><th align='left'><b>");
                out.print(localizedLbl);
                out.print("</b></th>");
                out.print("</tr></table><br>");
               
                out.print("</td></tr>");
                
                return;

            } catch (IOException ioe){
                logger.error("Unable to write to JspWriter.", ioe);
                throw new WBException(ioe);
            }    		
    		
    	}
        
        if (out==null) {
            logger.error("Cannot write to page because a valid JspWriter not found in the context");
            throw new WBException("Cannot write to page because a valid JspWriter not found in the context");
        }

		double budgetValuePercentageDouble = ltbu.getLongTermBudgetPct(locId.intValue(), fromDate, toDate, dbConn);
		double budgetValueDouble = ltbu.getLongTermBudget(locId.intValue(), fromDate, toDate, dbConn);
		
		StringBuffer budgetValuePercentage = new StringBuffer("");
		StringBuffer budgetValue = new StringBuffer("");
		
	
		budgetValue.append("<td align='right'>").append( formatSales( (new Double(budgetValueDouble) ).intValue() ));
		budgetValue.append("</td>");
		
		budgetValuePercentage.append("<td align='right'>").append( formatPercentage(new Double(budgetValuePercentageDouble) ) );
		budgetValuePercentage.append("%</td>");
        
        try {

        	LocalizationHelper localHelper = new LocalizationHelper(context.getPageContext());
        	
        	String localizedLbl = localHelper.getCaption(LABOR_FORECAST_HEADER_LBL, LocalizableTag.CONFIG_MESSAGE, MFRM_ID, "LB Forecast");
        	
            out.print("<tr><td>");
            out.print("<table border='0' cellpadding='2' width='300px'");
            out.print("cellspacing='0' class='contentTable finance'>");
            out.print("<tr><th align='left' width='100px'>");
            out.print(localizedLbl);
            out.print("</th>");
            out.print(budgetValue.toString());
            out.print(budgetValuePercentage.toString());
            out.print("</tr></table><br>");
           
            out.print("</td></tr>");

        } catch (IOException ioe){
            logger.error("Unable to write to JspWriter.", ioe);
            throw new WBException(ioe);
        }
        
    }
    
    /* Formats a percentage value to have two decimal places. */
    private String formatPercentage(Double value) {
        double newVal = (Math.round(value.doubleValue()*100.0d) / 100.0d) + 0.001d;
        String retVal = String.valueOf(newVal);
        return retVal.substring(0, retVal.indexOf('.') + 3);
    }
    
    private static String formatSales(int value) {

    	   NumberFormat usFormat = 
    		      NumberFormat.getIntegerInstance(Locale.US);
    		    
    	   return usFormat.format(value);
    		    
    	
    }
    
    public static void  main (String args[]) {
    	
    	System.out.println( WBIAGLaborBudgetForecastCallout.formatSales(1246789));
    	
    	
    	
    	
    }
    
}
