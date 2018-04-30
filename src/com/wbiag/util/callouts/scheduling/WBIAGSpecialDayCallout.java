/*
 * Created on Jan 10, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.wbiag.util.callouts.scheduling;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Date;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.utils.SpecialDayUtils;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.ForecastSpecialDay;
import com.workbrain.app.modules.retailSchedule.type.SpecialDayType;
import com.workbrain.app.modules.retailSchedule.utils.SODate;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.callouts.scheduling.SpecialDayContext;
import com.workbrain.util.callouts.scheduling.SpecialDayCallout;
import com.workbrain.util.callouts.scheduling.DefaultSpecialDayCallout;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.model.ResultDetail;
import com.workbrain.util.DateHelper;
import com.workbrain.app.modules.retailSchedule.model.Result;
import com.workbrain.sql.DBServer;

/**
 * @author sileung
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WBIAGSpecialDayCallout extends DefaultSpecialDayCallout{
    /**
     *
     * @param specialDayContext - context object
     * @return
     * @throws CalloutException
     */

	private static Logger logger = Logger.getLogger(WBIAGSpecialDayCallout.class);
	
    public boolean calculateDetailVolumesFromTrendsPreAction(SpecialDayContext specialDayContext) throws CalloutException
    {
        SODate forecastCurrDate = (SODate)specialDayContext.get(SpecialDayCallout.SPECIALDAY_CALLOUT_FORECASTCURRDAY);
        SODate oDate = (SODate)specialDayContext.get(SpecialDayCallout.SPECIALDAY_CALLOUT_EARLIERPERIODSTART);
        Hashtable m_hashSpecialDaysInForecast = (Hashtable)specialDayContext.get(SpecialDayCallout.SPECIALDAY_CALLOUT_SPECIALDAYSINFORECAST);
        Hashtable m_hashHistVolumeByDate = (Hashtable)specialDayContext.get(SpecialDayCallout.SPECIALDAY_CALLOUT_HISTORICVOLUMEBYDATE);
        CorporateEntity ce = (CorporateEntity)specialDayContext.get(SpecialDayCallout.SPECIALDAY_CALLOUT_FORECAST);
        Integer searchingRangeInt = (Integer)specialDayContext.get(SpecialDayCallout.SPECIALDAY_CALLOUT_SEARCHINGRANGE);
        double dPreviousYearVolume = 0d;

        // determine if forecast a special day or not.
        ForecastSpecialDay oSpecialDay = (ForecastSpecialDay)m_hashSpecialDaysInForecast.get(forecastCurrDate.toString());
        if (oSpecialDay != null) {
        	try {
        	    if ((oSpecialDay.getSpecdayType() == SpecialDayType.ALL_SPECDAY.intValue() || oSpecialDay.getSpecdayType() == SpecialDayType.ALL_SPECDAY_SAME_DAY_OF_WEEK.intValue())) {
                    String lastOccurrenceDateStr = SpecialDayUtils.findLastOccurrenceSpecialDay(oSpecialDay, oDate, searchingRangeInt.intValue(), ce, m_hashHistVolumeByDate);
               	    if (lastOccurrenceDateStr != null) {
                        Integer iTmp = (Integer) m_hashHistVolumeByDate.get(lastOccurrenceDateStr);
                        if (iTmp != null){
                        	dPreviousYearVolume = iTmp.doubleValue();
                        } else{
                        	Date lastOccrDate = DateHelper.convertStringToDate(lastOccurrenceDateStr, "MM/dd/yyyy");
                        	Date lastOccrEndDate = DateHelper.addDays(lastOccrDate, 1);
                        	if (logger.isDebugEnabled()) {
                        		logger.debug("lastOccurrenceDateStr: " + lastOccrDate.toString() );
                        		logger.debug("lastOccurEndDateStr: " + lastOccrEndDate.toString() );
                        	}

                        	DBConnection conn = DBInterface.getCurrentConnection();
                        	DBServer dbServ = conn.getDBServer();
                        	
                        	//Encode date strings according to db type
                        	String lastOccurDateStr    = dbServ.encodeDate(lastOccrDate);
                        	String lastOccurEndDateStr = dbServ.encodeDate(lastOccrEndDate);
                        	
                        	// retrieves the total volume for the matching historical special day 
                        	Vector details = DBInterface.findResults( " RESDET_DATE>=" + lastOccurDateStr + " AND RESDET_DATE < " + lastOccurEndDateStr + "", ce);    // Sample only. Do proper DB and Java syntax.
                        	if( !details.isEmpty() )
                        	{
                        		Result r = (Result) details.elementAt(0);
                        		Vector detailList = r.getDetailList();
                        		for (int i = 0; i < detailList.size(); i ++){
                        			ResultDetail rd = (ResultDetail) detailList.elementAt(i);
                            		dPreviousYearVolume += rd.getResdetVolume();
                        		}
                        	}
                        	if (logger.isDebugEnabled()) {
                        		logger.debug("last year date:" + lastOccurDateStr +  " previous year volume:" + dPreviousYearVolume);
                        	}
                        }
                        	
                        specialDayContext.add(SpecialDayCallout.SPECIALDAY_PREV_VOLUME, new Double(dPreviousYearVolume));
                        return false;
                    }
        	    }
        	    return true; // if skip, or cannot find the special event prev year or special type is 'calculated forecast by %', return true - using same day last year calculation.
        	} catch (RetailException re) {
        		throw new CalloutException(re.getMessage(), re);
        	}
        }
        return true;
    }

    /**
     *
     * @param specialDayContext - context object
     * @return
     * @throws CalloutException
     */
    public void calculateDetailVolumesFromTrendsPostAction(SpecialDayContext specialDayContext) throws CalloutException
    {
    }

    /**
     *
     * @param specialDayContext - context object
     * @return
     * @throws CalloutException
     */
    public boolean calculateDetailVolumesFromTrendsPreLoop(SpecialDayContext specialDayContext) throws CalloutException
    {
        return true;
    }

    /**
     *
     * @param specialDayContext - context object
     * @return
     * @throws CalloutException
     */
    public void calculateDetailVolumesFromTrendsPostLoop(SpecialDayContext specialDayContext) throws CalloutException
    {
    }
       
}
