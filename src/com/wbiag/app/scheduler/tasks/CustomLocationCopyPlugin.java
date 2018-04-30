package com.wbiag.app.scheduler.tasks;

import java.util.Iterator;
import java.util.List;

import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.app.ta.db.WorkbrainTeamAccess;
import com.workbrain.app.ta.model.WorkbrainTeamData;
import com.workbrain.sql.DBConnection;

/** 
 * Title:			Custom Location Copy Plugin
 * Description:		Plugin to edit the names of the new locations copied.	
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Jun 21, 2005
 * @author         	Kevin Tsoi
 */
public class CustomLocationCopyPlugin implements LocationCopyPlugin
{
    public static final int INTRNL_TYPE_STORE = 10; 
    public static final int STORE_TEAM_TYPE = 10001;
    public static final String LOCATION_STORE_PREFIX = "REST ";	    
    public static final String TEAM_STORE_PREFIX = "TEAM ";
    
    public void editLocations(DBConnection conn, List locationIds)
    	throws Exception
    {	    	   
	    RecordAccess recordAccess = null;
	    List locationsList = null;
	    List teamsList = null;	    
	    Iterator it = null;
	    ScheduleGroupData scheduleGroup = null;
	    WorkbrainTeamData teamData = null;
	    StringBuffer locationName = null;
	    StringBuffer teamName = null;
	    StringBuffer whereClause = null;
	    int wbttId = 10001;
	    int wbtId = 0;
	    int count = 0;
	    int intrnlType = 0;
	    
	    recordAccess = new RecordAccess(conn);	    
	    whereClause = new StringBuffer();	    	  	    
	    
	    //load list of locations created by copy task
	    locationsList = ScheduleGroupData.getScheduleGroupData(locationIds);
	    
	    whereClause.append("WBTT_ID = ");
	    whereClause.append(STORE_TEAM_TYPE);
	    whereClause.append(" AND ");
	    whereClause.append("WBT_ID IN (");
	    
	    it = locationsList.iterator();	    
	    while(it.hasNext())
	    {
	        scheduleGroup = (ScheduleGroupData)it.next();
	        wbtId = scheduleGroup.getWbtId();	        
	        whereClause.append(String.valueOf(wbtId));
	        
	        //update scheduleGroup name
	        intrnlType = scheduleGroup.getSkdgrpIntrnlType();	        	
	        locationName = new StringBuffer(scheduleGroup.getSkdgrpName());
	        //append store_prefix
	        if(INTRNL_TYPE_STORE == intrnlType)
	        {	            
	            locationName = locationName.insert(1, LOCATION_STORE_PREFIX);	           
	        }
	        //strip out prefix
	        else
	        {
	            locationName = locationName.deleteCharAt(0);
	        }
	        scheduleGroup.setSkdgrpName(locationName.toString());
	        
	        count++;
	        if(count < locationsList.size())
	        {
	            whereClause.append(",");
	        }
	        
	    }
	    whereClause.append(")");	    
	    
	    //load teams of type store that were created by copy task
	    teamsList = recordAccess.loadRecordData(new WorkbrainTeamData(), WorkbrainTeamAccess.WB_TEAM_TABLE, whereClause.toString() );
	    
	    it = teamsList.iterator();
	    while(it.hasNext())
	    {
	        //update team name
	        teamData = (WorkbrainTeamData)it.next();
	        teamName = new StringBuffer(teamData.getWbtName());
	        //remove prefix and append team_store_prefix
	        teamName = teamName.deleteCharAt(0);
	        teamName = teamName.insert(0, TEAM_STORE_PREFIX);
	        teamData.setWbtName(teamName.toString());
	    }	    	    
	    
	    recordAccess.updateRecordData(teamsList, WorkbrainTeamAccess.WB_TEAM_TABLE, WorkbrainTeamAccess.WB_TEAM_PRI_KEY);
	    recordAccess.updateRecordData(locationsList, ScheduleGroupData.TABLE_NAME, ScheduleGroupData.PRIMARY_KEY);
    }
}
