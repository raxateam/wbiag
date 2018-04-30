package com.wbiag.app.scheduler.tasks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.ScheduleGroupAccess;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.StringHelper;

/** 
 * Title:			Sync Teams And Locations Task
 * Description:		Synchronizes the structure between the team and location for a certain team type
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Jun 8, 2005
 * @author         	Kevin Tsoi
 */
public class SyncTeamsAndLocationsTask extends AbstractScheduledJob
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( SyncTeamsAndLocationsTask.class );
    private static final String PAGE_LOCATION = "/jobs/SyncTeamsAndLocationsParams.jsp";
    private static final String MESSAGE_JOB_OK = "Job ran successfully.";
    private static final String MESSAGE_JOB_FAILED = "Job failed.";    
    
    //parameters
    public static final String PARAM_TEAM_TYPE = "TeamType";
    public static final String PARAM_TEMPLATE = "Template";
    
    public static final String VALUE_DELIMITER = ";";
    public static final String TOKEN_DELIMITER = ",";
    
    public Status run(int taskID, Map params)
		throws Exception
	{	 
        DBConnection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuffer sql = null;
        
        ScheduleGroupAccess scheduleGroupAccess = null;
        ScheduleGroupData template = null;        
        ScheduleGroupData newScheduleGroup = null;
        HashMap teamToLocation = null;
        List insertScheduleGroups = null;
        List updateScheduleGroups = null;
        List skdGrpIdsList = null;
        List scheduleGroupDataList = null;
        List teamTypeList = null;
        Iterator it = null;
        StringTokenizer teamTypeTokens = null;
        String value = null;
        String teamTypeId = null;        
        String templateSkdGrpId = null;
        String wbtName = null;
        Integer skdGrpParentId = null;
                
        int wbtId = 0;        
        int wbtParentId = 0;
        int skdGrpId = 0;
        int paramIndex = 0;
                
        teamTypeId = (String)params.get(PARAM_TEAM_TYPE);        
        templateSkdGrpId = (String)params.get(PARAM_TEMPLATE);
                
        //get team type list from parameters
        teamTypeList = new ArrayList();
        teamTypeTokens = new StringTokenizer(teamTypeId, TOKEN_DELIMITER);
        while(teamTypeTokens.hasMoreTokens())
        {
            teamTypeList.add(teamTypeTokens.nextToken());            
        }                
        
        if(!StringHelper.isEmpty(teamTypeId) && !StringHelper.isEmpty(templateSkdGrpId))
        {                        
            //gets team id and team name that are not mapped to a schedule group for the specified team type(s)
            sql = new StringBuffer();
	        sql.append(" SELECT ");
	        sql.append(" WT.WBT_ID, ");
	        sql.append(" WT.WBT_NAME ");
	        sql.append(" FROM ");
	        sql.append(" WORKBRAIN_TEAM WT ");
	        sql.append(" WHERE ");
	        sql.append(" WT.WBTT_ID in ( ");
	        for (int i = 0; i < teamTypeList.size(); i++)
	    	{
	            sql.append(i > 0 ? ",?" : "?");
	    	}
	    	sql.append(" ) ");
	        sql.append(" AND ");
	        sql.append(" WT.WBT_ID NOT IN ");
	        sql.append(" ( ");
	        sql.append(" SELECT ");
	        sql.append(" SG.WBT_ID ");
	        sql.append(" FROM ");
	        sql.append(" SO_SCHEDULE_GROUP SG ");
	        sql.append(" ) ");
	        
	        try
	        {
		        conn = getConnection();
		        DBInterface.init(conn);
		        
		        paramIndex = 1;
		        ps = conn.prepareStatement(sql.toString());
		        
	        	it = teamTypeList.iterator();
	        	while(it.hasNext())
		        {            
		        	ps.setInt(paramIndex++, Integer.parseInt((String)it.next()));
		        }		        	        	
		        rs = ps.executeQuery();
		        
		        scheduleGroupAccess =  new ScheduleGroupAccess(conn);
		        teamToLocation = new HashMap();
		        insertScheduleGroups = new ArrayList();
		        updateScheduleGroups = new ArrayList();
		        skdGrpIdsList = new ArrayList();
		        
		        //retrieve schedule group template
		        template = ScheduleGroupData.getScheduleGroupData(Integer.parseInt(templateSkdGrpId));
		        
		        while(rs.next())
		        {
		            wbtId = rs.getInt("WBT_ID");
		            wbtName = rs.getString("WBT_NAME");
		            newScheduleGroup = (ScheduleGroupData)template.duplicate();
		            newScheduleGroup.setSkdgrpClientkey(String.valueOf(newScheduleGroup.getSkdgrpId()));		            
		            newScheduleGroup.setWbtId(wbtId);
		            newScheduleGroup.setSkdgrpName(wbtName);
		            skdGrpParentId = template.getSkdgrpParentId();
		            
		            //set parent id to template's parent id if not null
		            if(skdGrpParentId != null)
		            {
		                newScheduleGroup.setSkdgrpParentId(skdGrpParentId);
		            }
		            else
		            {
		                newScheduleGroup.setSkdgrpParentId(new Integer(1));
		            }		            
		            //add new schedule group to insert list
		            insertScheduleGroups.add(newScheduleGroup);
		        }
		        //create locations for teams that are not connected to a location
		        scheduleGroupAccess.batchInsert(insertScheduleGroups);		        		        
		        
		        //get team id, parent id, and skdgrp id for the specified team type(s)
		        sql = new StringBuffer();
		        sql.append(" SELECT ");
		        sql.append(" WT.WBT_ID, ");
		        sql.append(" WT.WBT_PARENT_ID, ");
		        sql.append(" SG.SKDGRP_ID ");
		        sql.append(" FROM ");
		        sql.append(" WORKBRAIN_TEAM WT, ");
		        sql.append(" SO_SCHEDULE_GROUP SG ");
		        sql.append(" WHERE ");		        
		        sql.append(" WT.WBTT_ID in ( ");
		        for (int i = 0; i < teamTypeList.size(); i++)
		    	{
		            sql.append(i > 0 ? ",?" : "?");
		    	}
		    	sql.append(" ) ");		        
		        sql.append(" AND ");
		        sql.append(" WT.WBT_ID=SG.WBT_ID ");
		       
		        paramIndex = 1;
		        ps = conn.prepareStatement(sql.toString());
		       
		        it = teamTypeList.iterator();
	        	while(it.hasNext())
		        {            
		        	ps.setInt(paramIndex++, Integer.parseInt((String)it.next()));
		        }		      
		        rs = ps.executeQuery();
		        
		        while(rs.next())
		        {
		            //create mapping of wbt id to wbtparent id + skdgrp id
		            wbtId = rs.getInt("WBT_ID");
		            wbtParentId = rs.getInt("WBT_PARENT_ID");
		            skdGrpId = rs.getInt("SKDGRP_ID");		            
		            teamToLocation.put(String.valueOf(wbtId), generateValue(wbtParentId, skdGrpId));
		            
		            //list of skdgrp ids to sync
		            skdGrpIdsList.add(new Integer(skdGrpId));
		        }
		        //loads list of schedule group data to sync
		        scheduleGroupDataList = ScheduleGroupData.getScheduleGroupData(skdGrpIdsList);
		        it = scheduleGroupDataList.iterator();
		        while(it.hasNext())
		        {
		            newScheduleGroup = (ScheduleGroupData)it.next();
		            
		            //check in map to see if skdgrp's parent has changed.  If so, then update
		            wbtId = newScheduleGroup.getWbtId();
		            value = (String)teamToLocation.get(String.valueOf(wbtId));
		            wbtParentId = getParentId(value);
		            value = (String)teamToLocation.get(String.valueOf(wbtParentId));
		            if(value != null)
		            {
			            skdGrpId = getSkdGrpId(value);
			            if(newScheduleGroup.getSkdgrpParentId() != null && skdGrpId != newScheduleGroup.getSkdgrpParentId().intValue())
			            {
			                newScheduleGroup.setSkdgrpParentId(new Integer(skdGrpId));
			                updateScheduleGroups.add(newScheduleGroup);
			            }
		            }
		        }
		        //updates all schedule groups that require sync
		        scheduleGroupAccess.batchUpdate(updateScheduleGroups);
		        
		        conn.commit();
	        }
	        catch(Exception e)
	        {
	            if(conn != null)
	            {
	                conn.rollback();	                
	            }
	            throw e;
	        }
	        finally
	        {
	            SQLHelper.cleanUp(ps, rs);
	        }
        
        }        
        return jobOk(MESSAGE_JOB_OK);
	}
    
    public String generateValue(int parentId, int skdGrpId)
    {
        StringBuffer value = null;
        
        value = new StringBuffer();
        value.append(parentId);
        value.append(VALUE_DELIMITER);
        value.append(skdGrpId);
        
        return value.toString();
    }
    
    public int getParentId(String value)
    {
        int parentId = 0;
        int index = 0;
       
        index = value.indexOf(VALUE_DELIMITER);
        parentId = Integer.parseInt(value.substring(0, index));
        
        return parentId;
    }
    
    public int getSkdGrpId(String value)
    {
        int skdGrpId = 0;
        int index = 0;
        
        index = value.indexOf(VALUE_DELIMITER);
        skdGrpId = Integer.parseInt(value.substring(index+1));
        
        return skdGrpId;
    }
    
    public String getTaskUI()
    {
        return PAGE_LOCATION;
    }
    
}
