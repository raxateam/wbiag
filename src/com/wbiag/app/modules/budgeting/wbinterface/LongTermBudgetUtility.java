package com.wbiag.app.modules.budgeting.wbinterface;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import com.workbrain.app.modules.budgeting.db.FcstItemAccess;
import com.workbrain.app.modules.budgeting.db.FcstPubForecastAccess;
import com.workbrain.app.modules.budgeting.db.ForecastAccess;
import com.workbrain.app.modules.budgeting.model.FcstItemData;
import com.workbrain.app.modules.budgeting.model.ForecastData;
import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.utils.SODateInterval;
import com.workbrain.app.ta.db.HolidayCalDetailAccess;
import com.workbrain.app.ta.db.HolidayCalendarAccess;
import com.workbrain.app.ta.model.HolidayCalDetailData;
import com.workbrain.app.ta.model.HolidayCalendarData;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.budgeting.db.FcstTimePeriodAccess;

public class LongTermBudgetUtility {
	
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( LongTermBudgetUtility.class );
	
	public static final String FORECAST_NAME_ENTRY = "system/modules/LaborBudgeting/SO_FORECAST_NAME";
	public static final double DEFAULT_BUDGET_VALUE = 0;
	public static final String LABOR_BUDGET_CALENDAR = "LABOR BUDGET HOLIDAYS";
	
	public double getLongTermBudgetPct(int scheduleGroupId, Calendar startCal, Calendar endCal, DBConnection dbConn) throws RetailException, CalloutException {
		logger.debug("**********************LongTermBudgetUtility");
		
		DBInterface.init(dbConn);
		logger.debug("********************** db init");
		//DBConnection dbConn = DBInterface.getCurrentConnection();
		
		//Find the latest published forecast for a given root time period and 
		// and schedule group id to interface with LFSO 
		String forecastName = "";
		Long forecastId = null;
		try {
			ForecastAccess fcAccess = new ForecastAccess(dbConn);
			
			Integer fcId = (new FcstPubForecastAccess(dbConn)).
			getBudgetCalloutForecastId(scheduleGroupId, startCal, endCal);
			if ( fcId == null ) {
				logger.info("No forecast was found to interface with LFSO, " +
						"using default budget value of "+DEFAULT_BUDGET_VALUE+".");
				return DEFAULT_BUDGET_VALUE;
			}
			ForecastData fcData = fcAccess.load(fcId.intValue(), false); 
			forecastId = new Long(fcData.getFcId());
			forecastName = fcData.getFcName();
			
			// TT49873: if forecast name is invalid, return a budget of -1
			if (forecastName==null || "".equals(forecastName.trim())) {
				logger.info("Invalid value for ("+FORECAST_NAME_ENTRY+"), " +
						"using default budget value of "+DEFAULT_BUDGET_VALUE+".");
				return DEFAULT_BUDGET_VALUE;
			}
		} catch( Exception e ) {
			throw new CalloutException(e);
		}
		
		double budgetPctValue = 0;
		java.sql.Date startDate = new java.sql.Date(startCal.getTime().getTime());
		java.sql.Date endDate = new java.sql.Date(endCal.getTime().getTime());
		
		FcstItemAccess itemAccess = new FcstItemAccess(dbConn);
		
		logger.info("Getting Payroll data for forecastId=" + forecastId + 
				" , startDate="+startDate+", endDate="+endDate+
				" and scheduleGroupId="+scheduleGroupId);  
		
		List items = itemAccess.load(forecastId, startDate, endDate, new Integer(scheduleGroupId));
		Iterator iter = items.iterator();
		if (iter.hasNext()){
			FcstItemData data = (FcstItemData)iter.next();
			budgetPctValue = data.getFcitPayrollPct();
		}
		return budgetPctValue;
	}
	
	public double getLongTermBudget(int scheduleGroupId, Calendar startCal, Calendar endCal, DBConnection dbConn ) throws RetailException, SQLException {
		
		//DBConnection dbConn = DBInterface.getCurrentConnection();
		DBInterface.init(dbConn);
		logger.debug("********************** db init");
		
		// get corporate entity and from there to get LSFO master forecast
		CorporateEntity ce = CorporateEntity.getCorporateEntity(scheduleGroupId);
		SODateInterval sodi = new SODateInterval(startCal,endCal);
		List l = ce.getForecastList(sodi);
		com.workbrain.app.modules.retailSchedule.model.Forecast f = null;
		if (!l.isEmpty()){
			f = (com.workbrain.app.modules.retailSchedule.model.Forecast)l.get(0);
		}
		double fBudget = ce.calculateTotalForecastValue(sodi);
		double fPct = 0.0;
		if (f != null ) {
			fPct = f.getFcastBudgVal().doubleValue();
		}
		Values values = new Values();
		this.getBudgetValues(scheduleGroupId, startCal, endCal, values, dbConn );
		double ahr = values.getAhr();
		double otherHours = values.getOtherHours();
		double holidayHours = this.getDeductionHoursForHolida(scheduleGroupId, startCal, endCal, dbConn);
		double budgetValue = ( fBudget * fPct / 100 ) - ahr * ( otherHours + holidayHours);
		if (budgetValue < 0 ){
			budgetValue = 0;
		}
		return budgetValue;
	}
	
    public void getBudgetValues(int scheduleGroupId, Calendar startCal, Calendar endCal, Values values, DBConnection conn) throws CalloutException {

        //Find the latest published forecast for a given root time period and 
        // and schedule group id to interface with LFSO 
        String forecastName = "";
        Long forecastId = null;
        try {
            ForecastAccess fcAccess = new ForecastAccess(conn);
            
            Integer fcId = (new FcstPubForecastAccess(conn)).
                        getBudgetCalloutForecastId(scheduleGroupId, startCal, endCal);
            if ( fcId == null ) {
                logger.info("No forecast was found to interface with LFSO, " +
                            "using default budget value of "+DEFAULT_BUDGET_VALUE+".");
                
                fcId = new Integer ((new Double(DEFAULT_BUDGET_VALUE)).intValue());
            }
            
            ForecastData fcData = fcAccess.load(fcId.intValue(), false); 
            
            if (null==fcData) {
           	
                values.setAhr(DEFAULT_BUDGET_VALUE);
                values.setOtherHours(DEFAULT_BUDGET_VALUE);
            	return;
            	
            }
            
            forecastId = new Long(fcData.getFcId());
            forecastName = fcData.getFcName();
            
            // TT49873: if forecast name is invalid, return a budget of -1
            if (forecastName==null || "".equals(forecastName.trim())) {
                logger.info("Invalid value for ("+FORECAST_NAME_ENTRY+"), " +
                            "using default budget value of "+DEFAULT_BUDGET_VALUE+".");
                values.setAhr(DEFAULT_BUDGET_VALUE);
                values.setOtherHours(DEFAULT_BUDGET_VALUE);
            	return;
            }
        } catch( Exception e ) {
            throw new CalloutException(e);
        }
        
        double ahr = 0;
        double otherHours = 0;
        Date startDate = new Date(startCal.getTime().getTime());
        Date endDate = new Date(endCal.getTime().getTime());
    
        FcstItemAccess itemAccess = new FcstItemAccess(conn);
        logger.info("Getting Payroll data for forecastId=" + forecastId + 
                     " , startDate="+startDate+", endDate="+endDate+
                     " and scheduleGroupId="+scheduleGroupId);  
        List items = itemAccess.load(forecastId, startDate, endDate, new Integer(scheduleGroupId));
        Iterator iter = items.iterator();
 
        if (iter.hasNext()){
        	FcstItemData data = (FcstItemData)iter.next();
        	ahr = data.getFcitAhr();
        	otherHours = data.getFcitOtherHours();
        }
        values.setAhr(ahr);
        values.setOtherHours(otherHours);
        return;
    }
    
    public boolean areFcstDatesAndLabBudgDatesAligned(	Calendar startCal, 
    													Calendar endCal,
    													DBConnection dbConn) {
    	
    	
    	DBInterface.init(dbConn);
    	FcstTimePeriodAccess fctpAccess  = new FcstTimePeriodAccess(dbConn); 
    	
    	List fctpList = fctpAccess.loadLeafNodes(startCal.getTime(), endCal.getTime());
    	
    	if (fctpList.size() == 0) { 
    		return false ;
    	} else {
    		logger.debug( "&& fctpList: \n" + fctpList.toString() );
    	}
    		
    	return true;
    	
    }
    
    
    private int getDeductionHoursForHolida(int skdgrpId, Calendar start, Calendar end, DBConnection conn) throws SQLException{
    	int holidays = getHolidays(start, end, conn);
    	if (holidays > 0 ){
    		int partTemp = getPartTempCount(skdgrpId, conn);
    		int fullTime = getTotalCount(skdgrpId, conn) - partTemp;
    		return ( partTemp * 4 + fullTime * 8 ) * holidays ;
    	}
    	return 0;
    }
    
    private int getHolidays(Calendar start, Calendar end, DBConnection conn ) throws SQLException {
    	int holidays = 0;
    	int year = start.get(Calendar.YEAR);
    	HolidayCalendarAccess hca = new HolidayCalendarAccess(conn);
    	List l = hca.loadAll();
    	Iterator it = l.iterator();
    	HolidayCalendarData hcd = null;
    	while (it.hasNext()){
    		hcd = (HolidayCalendarData)it.next();
    		if (hcd.getHcalYear() == year && hcd.getHcalName()!=null && hcd.getHcalName().equalsIgnoreCase(LABOR_BUDGET_CALENDAR) ){
    			break;
    		}
    		hcd = null;
    	}
    	if (hcd!=null){
    		// get the holiday details
    		HolidayCalDetailAccess hda = new HolidayCalDetailAccess(conn);
    		l = hda.loadByHcalId(hcd.getHcalId());
    		it = l.iterator();
    		while (it.hasNext()){
    			HolidayCalDetailData hcdd = (HolidayCalDetailData)it.next();
    			if ( start.getTime().getTime() <= hcdd.getHcaldHolDate().getTime() && 
    					hcdd.getHcaldHolDate().getTime() <= end.getTime().getTime()){
    					holidays ++;
    			}
    		}
    		
    	}
    	return holidays;
    }
    
    private int getTotalCount(int skdgrpId, DBConnection conn ) throws SQLException{
    	int total = 0;
    	PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = "SELECT count(*) FROM SO_EMPLOYEE se where skdgrp_id = ?";
		try {
			ps = conn.prepareStatement(sql);
			ps.setInt(1,skdgrpId);
			rs = ps.executeQuery();
			if (rs.next()) {
				total = rs.getInt(1);
			}
		} catch (Exception e) {
			throw new SQLException(e.getMessage());
		}
		finally {
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
		}
    	return total;
    }
    
    private int getPartTempCount(int skdgrpId, DBConnection conn ) throws SQLException{
    	int total = 0;
    	PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = "select count(*) from SO_EMPLOYEE se, EMPLOYEE e, CALC_GROUP cg " + 
			" where skdgrp_id = ? and e.emp_id = se.emp_id " +
			" and cg.calcgrp_id = e.calcgrp_id " + 
			" and ( locate('PART',cg.calcgrp_name) > 0  or locate('TEMP',cg.calcgrp_name) > 0) "; 
		try {
			ps = conn.prepareStatement(sql);
			ps.setInt(1,skdgrpId);
			rs = ps.executeQuery();
			if (rs.next()) {
				total = rs.getInt(1);
			}
		} catch (Exception e) {
			throw new SQLException(e.getMessage());
		}
		finally {
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
		}
    	return total;
    }
    
    class Values {
    	double ahr;
    	double otherHours;
    	
    	public double getAhr(){
    		return ahr;
    	}
    	
    	public double getOtherHours(){
    		return otherHours;
    	}
    	
    	public void setAhr(double ahr){
    		this.ahr = ahr;
    	}
    	
    	public void setOtherHours(double otherHours){
    		this.otherHours = otherHours;
    	}
    }

}
