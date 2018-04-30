package com.wbiag.app.modules.pts;

import java.util.Date;
import java.util.List;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.model.ScheduleProfile;
import com.workbrain.server.registry.Registry;
import com.workbrain.util.DateHelper;
import com.workbrain.util.RegistryHelper;

/** 
 * Title:			PTS Helper
 * Description:		Helper class for the PTS system
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Apr 20, 2005
 * @author         	Kevin Tsoi
 */
public class PTSHelper
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PTSHelper.class);    
    
    //registy definitions
    public static final String REG_PTS_ON_CLOCK_TYPE = "system/PTS/PTS_ON_CLOCK_TYPE";    
    public static final String REG_PTS_MARK_WS_FIELD = "system/PTS/PTS_MARK_WS_FIELD";
    public static final String REG_PTS_VOLUME_TYPE = "system/PTS/PTS_VOLUME_TYPE";
    public static final String REG_PTS_LAST_UPDATED = "system/PTS/PTS_LAST_UPDATED";
    public static final String REG_PTS_WBT_FIELD_FOR_CALC_DATE = "system/PTS/PTS_WBT_FIELD_FOR_CALC_DATE";
    public static final String REG_PTS_JOB_RATE_INDEX = "system/PTS/PTS_JOB_RATE_INDEX";
    public static final String REG_PTS_TIMECODES = "system/PTS/PTS_TIMECODES";
    public static final String REG_PTS_HOURTYPES = "system/PTS/PTS_HOURTYPES";
    public static final String REG_PTS_MARK_TEAMS_WO_REVENUE_FIELD = "system/PTS/PTS_MARK_TEAMS_WO_REVENUE_FIELD";
    public static final String REG_DELIMITER = ";";

    public static final String BUDGET = "B";
    public static final String ACTUAL = "A";
    public static final String COST = "C";
    public static final String EARNED = "E";    
    
    public static final String MONDAY = "MONDAY";
    public static final String TUESDAY = "TUESDAY";
    public static final String WEDNESDAY = "WEDNESDAY";
    public static final String THURSDAY = "THURSDAY";
    public static final String FRIDAY = "FRIDAY";
    public static final String SATURDAY = "SATURDAY";
    public static final String SUNDAY = "SUNDAY";
    
    public static final String DATE_TIME_FORMAT = "MM/dd/yyyy HH:mm:ss";
    public static final String DATE_FORMAT = "MM/dd/yyyy";    
     
    public static final int INTRNL_TYPE_SCHEDULE_AREA = 11;
    public static final int INTRNL_TYPE_STORE = 10;
    
    public static final int INVALID_STORE_ID = -9999;
    
    //Gets the registry value for a given registry name.
    public static String getRegistryValue(String registryName)
    {
        String registyValue = "";
        try
        {
            registyValue = (String)Registry.getVar(registryName);
        }
        catch(Exception e)
        {
            logger.debug(e);
        }               
        return registyValue;
    }         
    
    public static void setRegistryValue(String registryName, String registryValue)
    {
        RegistryHelper registryHelper = null;
        registryHelper = new RegistryHelper();
        try
        {
        	registryHelper.setVar(registryName, registryValue);
		}
		catch(Exception e)
		{
		    logger.debug(e);
		}
    }
    
       
    //Gets the start date of the forecast generated.
    public static Date getForecastStartDate(ScheduleGroupData scheduleGroupData, Date calculationDate, int offset)
    {
        Date startDate = null;
        String dayOfWeekStr = null;
        int dayOfWeekInt = 0;
        
        dayOfWeekInt = scheduleGroupData.getSkdgrpStartdow();
        
        //defaults to sunday if start dow is not properly set
        switch(dayOfWeekInt)
        {        	
        	case 1:
        	    dayOfWeekStr = PTSHelper.SUNDAY;    
        	    break;
        	case 2:
        	    dayOfWeekStr = PTSHelper.MONDAY;
        	    break;
        	case 3:
        	    dayOfWeekStr = PTSHelper.TUESDAY;
        	    break;
        	case 4:
        	    dayOfWeekStr = PTSHelper.WEDNESDAY;
        	    break;
        	case 5:
        	    dayOfWeekStr = PTSHelper.THURSDAY;
        	    break;
        	case 6:
        	    dayOfWeekStr = PTSHelper.FRIDAY;
        	    break;
        	case 7:
        	    dayOfWeekStr = PTSHelper.SATURDAY;
        	    break;        	
        	default:
        	    dayOfWeekStr = PTSHelper.SUNDAY;    
    	    	break;
        }
                    
            startDate = DateHelper.addDays(calculationDate, offset);
            startDate = DateHelper.nextDay(startDate, dayOfWeekStr);
            
            return startDate;
        }  
    
    //Gets the number of days forecasted.
    public static int getDaysForecasted(ScheduleGroupData scheduleGroupData)
    	throws RetailException
    {
        CorporateEntity corporateEntity = null;
        List profileList = null;
        int daysForecasted = 0;
        
        corporateEntity = new CorporateEntity(scheduleGroupData);
        
        //get profile list
        profileList = corporateEntity.getScheduleProfileList();
        
        //take first profile if exist(there should only be up to 1 for each department)
        if(profileList.size() > 0)
        {
            daysForecasted = ((ScheduleProfile)profileList.get(0)).getSkdprofLen().intValue();
        }
        else
        {
            daysForecasted = -1;
        }
        return daysForecasted;
    }      
    
    //gets the scheduleGroup Id for the parent store of skdGrpId
    public static int getParentStore(int skdGrpId)
    	throws RetailException
    {
        int parentId = 0;
        ScheduleGroupData scheduleGroupChild = null;
        ScheduleGroupData scheduleGroupParent = null;
        
        //load schedule group of child
        scheduleGroupChild = ScheduleGroupData.getScheduleGroupData(skdGrpId);
        parentId = scheduleGroupChild.getSkdgrpParentId().intValue();
        
        //load schedule group of parent
        scheduleGroupParent = ScheduleGroupData.getScheduleGroupData(parentId);
        
        //recurse up until store is reached
        if(scheduleGroupParent.getSkdgrpIntrnlType() != INTRNL_TYPE_STORE)
        {
            parentId = getParentStore(parentId);
        }
        return parentId;        
    }
}
