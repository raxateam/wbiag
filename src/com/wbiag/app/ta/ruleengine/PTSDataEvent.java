package com.wbiag.app.ta.ruleengine;

import java.util.List;
import java.util.StringTokenizer;

import com.wbiag.app.modules.pts.PTSHelper;
import com.workbrain.app.ta.model.Clock;
import com.workbrain.app.ta.ruleengine.DataEvent;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.StringHelper;

/** 
 * Title:			PTS Data Event
 * Description:		marks work summary if last clock is still ON CLOCK
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Apr 19, 2005
 * @author         	Kevin Tsoi
 */
public class PTSDataEvent extends DataEvent
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PTSDataEvent.class);   
    private static final String PARAM_FLAG = "FLAG";
    private static final String PARAM_UDF = "UDF";
    
    /* (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.DataEvent#afterApplyClocks(com.workbrain.app.ta.ruleengine.WBData, com.workbrain.sql.DBConnection)
     */
    public void afterApplyClocks(WBData data, DBConnection conn)
    {            
        List clockList = null;
        Clock lastClock = null;
        StringTokenizer onClockTypes = null;
        String workSummaryField = null;
        String flagOrUdf = null;
        int fieldNumber = 0;
        
        //get on clock types from registry
        onClockTypes = new StringTokenizer(PTSHelper.getRegistryValue(PTSHelper.REG_PTS_ON_CLOCK_TYPE), PTSHelper.REG_DELIMITER);
        
        //get mark work summary field from registry
        workSummaryField = PTSHelper.getRegistryValue(PTSHelper.REG_PTS_MARK_WS_FIELD).toUpperCase();
        
        clockList = data.getClocks();              
 
        if(!StringHelper.isEmpty(workSummaryField))
        {            
	        //determine whether to use a flag or udf work summary field
	        if(workSummaryField.indexOf(PARAM_FLAG) >= 0)
	        {
	            flagOrUdf = PARAM_FLAG;
	        }
	        else if(workSummaryField.indexOf(PARAM_UDF) >= 0)
	        {
	            flagOrUdf = PARAM_UDF;
	        }
	    	//determine the field number of the work summary field
	        if(workSummaryField.indexOf("10") >= 0)
	        {
	            fieldNumber = 10;
	        }
	        else
	        {
	            fieldNumber = Integer.parseInt(workSummaryField.substring(workSummaryField.length()-1));
	        }
        }
        //get last clock if there are clocks for the day
        if(!clockList.isEmpty() && flagOrUdf != null)
        {
	        lastClock = (Clock) clockList.get(clockList.size()-1);
	    	    	        	      	        
	        //iterate through the list of possible on clock types
	        while(onClockTypes.hasMoreTokens())
	        {
	            if(Integer.parseInt(onClockTypes.nextToken()) == lastClock.getClockType())
	            {
	                //mark work summary field with "Y" if last clock one of the types specified
	                setWorkSummaryField(data, flagOrUdf, fieldNumber, "Y");              
	                break;
	            }
	            else
	            {
	                //otherwise, mark work summary field with "N"
	                setWorkSummaryField(data, flagOrUdf, fieldNumber, "N");
	            }
	        }
        }
    }
    
    /**
     * Sets the appropriate work summary field with value
     * 
     * @param data
     * @param flagOrUdf
     * @param fieldNumber
     * @param value
     */
    protected void setWorkSummaryField(WBData data, String flagOrUdf, int fieldNumber, String value)
    {
        if(PARAM_FLAG.equals(flagOrUdf))
        {
            switch(fieldNumber)
            {
            	case 1:
            	    data.setWrksFlag1(value);
            	    break;
            	case 2:
            	    data.setWrksFlag2(value);
            	    break;
            	case 3:
            	    data.setWrksFlag3(value);
            	    break;
            	case 4:
            	    data.setWrksFlag4(value);
            	    break;
            	case 5:
            	    data.setWrksFlag5(value);
            	    break;
            	default:
            	    break;            	
            }            
        }
        else if(PARAM_UDF.equals(flagOrUdf))
        {
            switch(fieldNumber)
            {
            	case 1:
            	    data.setWrksUdf1(value);
            	    break;
            	case 2:
            	    data.setWrksUdf2(value);
            	    break;
            	case 3:
            	    data.setWrksUdf3(value);
            	    break;
            	case 4:
            	    data.setWrksUdf4(value);
            	    break;
            	case 5:
            	    data.setWrksUdf5(value);
            	    break;
            	case 6:
        	    	data.setWrksUdf6(value);
        	    	break;            	    
            	case 7:
            	    data.setWrksUdf7(value);
            	    break;
            	case 8:
            	    data.setWrksUdf8(value);
            	    break;
            	case 9:
            	    data.setWrksUdf9(value);
            	    break;
            	case 10:
            	    data.setWrksUdf10(value);
            	    break;
            	default:
            	    break;            	
            }                       
        }        
    }
}
