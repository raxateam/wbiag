package com.wbiag.app.scheduler.tasks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.wbiag.app.ta.model.SOResultsDetailData;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.ResultsDetailAccess;
import com.workbrain.app.modules.retailSchedule.db.ScheduleGroupAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.Distribution;
import com.workbrain.app.modules.retailSchedule.model.DistributionDetail;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.security.team.WorkbrainTeamManager;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.app.ta.model.WorkbrainTeamTypeData;
import com.workbrain.app.ta.model.WorkbrainTeamData;
import com.workbrain.app.ta.db.WorkbrainTeamAccess;

/**
 * Title:			Custom Location Copy Plugin
 * Description:		Plugin to edit the names of the new locations copied.
 * Copyright:		Copyright (c) 2004
 * Company:        	Workbrain Inc
 * Created: 		Dec 14, 2004
 * @author         	Wilson Woo
 */
public class LocationCopyTask extends AbstractScheduledJob {

    private static org.apache.log4j.Logger logger =
        org.apache.log4j.Logger.getLogger( LocationCopyTask.class );
    private DBConnection conn = null;

    private static final String STORE_TYPE_NAME = "STORE";
    public static final String PARAM_SUB_LOCATIONS = "includeSubLocationsFlag";
    public static final String PARAM_HISTORY_DATA = "copyHistDataFlag";
    public static final String PARAM_DISTRIBUTION_DATA = "copyDistDataFlag";
    public static final String REG_LOCATION_COPY_PLUGIN = "/system/customer/LOCATION_COPY_PLUGIN";
    public static final String SKDGRP_SUN_DIST_ID_COLNAME = "SKDGRP_SUN_DIST_ID";
    public static final String SKDGRP_MON_DIST_ID_COLNAME = "SKDGRP_MON_DIST_ID";
    public static final String SKDGRP_TUE_DIST_ID_COLNAME = "SKDGRP_TUE_DIST_ID";
    public static final String SKDGRP_WED_DIST_ID_COLNAME = "SKDGRP_WED_DIST_ID";
    public static final String SKDGRP_THU_DIST_ID_COLNAME = "SKDGRP_THU_DIST_ID";
    public static final String SKDGRP_FRI_DIST_ID_COLNAME = "SKDGRP_FRI_DIST_ID";
    public static final String SKDGRP_SAT_DIST_ID_COLNAME = "SKDGRP_SAT_DIST_ID";

    //for plugin
	private String className = null;
	private Class pluginClass = null;
	private Object pluginObject = null;

    /**
     * Default Constructor
     */
    public LocationCopyTask()
    {
    }

    /**
     * Specifies the location of the JSP
     * @return  The context-path of the JSP parameter page for this task
     */
    public String getTaskUI()
    {
        return "/jobs/LocationCopyTaskParams.jsp";
    }

    /* (non-Javadoc)
     * @see com.workbrain.app.scheduler.enterprise.ScheduledJob#run(int, java.util.Map)
     */
    public Status run( int taskID, Map parameters )
    	throws Exception
   	{
        conn = getConnection();
        DBInterface.init( conn );

        List newWBTeams = getNewTeamsList();
        Iterator iter = newWBTeams.iterator();
        HashMap newTeamIDs = new HashMap();

        try
        {
		    List newLocationIds = new ArrayList();

		    while( iter.hasNext() )
		    {
		        WorkbrainTeamData wbTeam = ( WorkbrainTeamData )iter.next();

	            int teamID = performCopy( wbTeam, parameters, newLocationIds );
	            if( teamID != -1 )
	            {
	                newTeamIDs.put( new Integer( wbTeam.getWbtId() ), new Integer( teamID ) );
	            }
		    }

		    // Only perform the sync if there are new teams added
		    if( newTeamIDs.size() > 0 )
		    {
		        syncWBTeams( newTeamIDs );
		    }

		    //executes plugin if client has created one
		    LocationCopyPlugin locationCopyPlugin = getInterface();
		    if(locationCopyPlugin != null)
		    {
		        locationCopyPlugin.editLocations(conn, newLocationIds);
		    }
		    conn.commit();
        }
        catch( Exception e )
        {
            DBInterface.rollbackTransaction();
            if(conn != null)
            {
                conn.rollback();
            }
            throw new RetailException(e.getMessage(), e);
        }
        return jobOk( "job ran successfully." );
    }

    /**
     * This method have been extracted from:
     *  com.workbrain.app.modules.retailSchedule.action.LocationNavigationAction.java 's
     * APerformCopy method.  The changes to this method have been the parameters and the
     * return value.  After a location copy is successful complete, this method returns
     * the ID of the root of new location tree structure.
     *
     * @param   wbTeam      The team that nees to be copied
     * @param   parameters  Parameters taken from the job scheduler's paramaters JSP
     * @return              Returns the root ID of the newly "copied" location structure
     * @throws  Exception   Passes any exceptions to the main "run()" method
     */
    private int performCopy( WorkbrainTeamData wbTeam, Map parameters, List newLocationIds ) throws Exception
    {
        HashMap params = new HashMap( parameters );

        // The UDF field defines which template to copy from.  If it doesn't exist,
        // get the template from the job's parameters page
        String strCopyCeID = null;
        String strOldPrefix = "";

        String strNewLocationName = wbTeam.getWbtName();
        String strNewPrefix = strNewLocationName;
        String strIncludeSubLocationsFlag = ( String )params.get( PARAM_SUB_LOCATIONS );
        String strCopyHist = ( String )params.get( PARAM_HISTORY_DATA );
        String strCopyDist = ( String )params.get( PARAM_DISTRIBUTION_DATA );
        //String strCreateReaders = ( String )params.get( "createReaderFlag" );

        if( wbTeam.getWbtUdf1() != null )
        {
            WorkbrainTeamAccess workbrainTeamAccess = new WorkbrainTeamAccess(conn);
    	    WorkbrainTeamData workbrainTeamData = workbrainTeamAccess.loadByName(wbTeam.getWbtUdf1());
    	    if(workbrainTeamData != null)
    	    {
    	        int teamId = workbrainTeamData.getWbtId();
    	        strCopyCeID = ScheduleGroupAccess.getScheduleGroupFromTeam(teamId).toString();
    	    }
        }
        else
        {
            strCopyCeID = ( String )params.get( "copyLocationID" );
        }

    	//get newParent scheduleGroupId
        int wbtParentID = wbTeam.getWbtParentId();
        Integer newParentSgID = ScheduleGroupAccess.getScheduleGroupFromTeam(wbtParentID);

        if(!StringHelper.isEmpty(strCopyCeID))
        {
	        ScheduleGroupData skgData = ScheduleGroupData.getScheduleGroupData( Integer.parseInt( strCopyCeID ) );
	        if(skgData != null)
	        {
		        int index = skgData.getSkdgrpName().indexOf( "-" );
		        if( index != -1 )
		        {
		            strOldPrefix = skgData.getSkdgrpName().substring( 0, index + 1 );
		        }
	        }
        }
        if (newParentSgID != null && !StringHelper.isEmpty(strCopyCeID) && strNewLocationName != null  && strNewPrefix != null)
        {
            logger.debug( "Required parameters exist: Proceed with Location Copy logic." );

            PreparedStatement psSelectResDet = null;
            ResultSet rsResDet = null;

            PreparedStatement psSelectDistrib = null;
            PreparedStatement psSelectDistribDet = null;
            ResultSet rsDistrib = null;
            ResultSet rsDistribDet = null;

            PreparedStatement psSelectSkdgrp = null;
            ResultSet rsSkdgrp = null;

            try
            {
                ScheduleGroupAccess scheduleGroupAccess = new ScheduleGroupAccess(conn);
                ScheduleGroupData newParentSG = ScheduleGroupData.getScheduleGroupData( newParentSgID.intValue() );
                CorporateEntity oNewParentCE = newParentSG.getCorporateEntity();

                CorporateEntity oCopyCE = CorporateEntity.getCorporateEntity(new Integer(strCopyCeID));
                CorporateEntity oNewCE = null;

                HashMap newCEIdMap = new HashMap();
				HashMap oldCEIdMap = new HashMap();

                if ("N".equalsIgnoreCase(strIncludeSubLocationsFlag))
                {
                    oNewCE = oCopyCE.copyCE(oNewParentCE, strNewLocationName, strOldPrefix, strNewPrefix, CorporateEntity.NO_SUBTREE, newCEIdMap, oldCEIdMap);
                }
                else
                {
                    oNewCE = oCopyCE.copyCE(oNewParentCE, strNewLocationName, strOldPrefix, strNewPrefix, CorporateEntity.ALL_SUBTREE, newCEIdMap, oldCEIdMap);
                }

                //---- Synchronize the teams - otherwise getChildren call on CE will return extra results in future copies---
                WorkbrainTeamManager teamManager= new WorkbrainTeamManager(DBInterface.getCurrentConnection());
                teamManager.syncWorkbrainTeamTable();

                newLocationIds.addAll(newCEIdMap.values());

                //----BEGIN COPY RESULTS_DETAIL RECORDS-----------

                if ("Y".equalsIgnoreCase(strCopyHist))
                {
                    logger.debug( "Copy Historical Data flag enabled." );

                    List listCEIDs = new ArrayList();
                    if (strIncludeSubLocationsFlag != null)
                    {
                        listCEIDs = oCopyCE.getCorporateEntityIDList(CorporateEntity.CHILDREN);
                    }
                    listCEIDs.add(String.valueOf(oCopyCE.getID()));

                    //ResultsDetail SELECT statement:
                    StringBuffer sbResultDetSelectSQL = new StringBuffer();
                    sbResultDetSelectSQL.append(" SELECT * ");
                    sbResultDetSelectSQL.append(" FROM ");
                    sbResultDetSelectSQL.append(" SO_RESULTS_DETAIL ");
                    sbResultDetSelectSQL.append(" WHERE ");
                    sbResultDetSelectSQL.append(" SKDGRP_ID IN ( ");
                    for (int i = 0; i < listCEIDs.size(); i++)
                    {
                    	sbResultDetSelectSQL.append(i > 0 ? ",?" : "?");
                    }
                    sbResultDetSelectSQL.append(" )");

                    psSelectResDet = conn.prepareStatement(sbResultDetSelectSQL.toString());

                    int param = 1;
                    Iterator it = listCEIDs.iterator();

                    while(it.hasNext())
                    {
                        psSelectResDet.setInt(param++, Integer.parseInt((String)it.next()));
                    }
                    rsResDet = psSelectResDet.executeQuery();

                    ResultsDetailAccess resultsDetailAccess = new ResultsDetailAccess(conn);

                    //core ResultsDetail doesnt allow insert because of getID!!
                    SOResultsDetailData newResultsDetail = null;
                    List skdGrpList = new ArrayList();
                    int skdGrpId = 0;
                    while (rsResDet.next())
                    {
                        newResultsDetail = new SOResultsDetailData();
                        newResultsDetail.setResdetId(conn.getDBSequence("SEQ_RESDET_ID").getNextValue());
                        newResultsDetail.setSkdgrpId(rsResDet.getInt("SKDGRP_ID"));
                        newResultsDetail.setResdetDate(DateHelper.toDatetime(rsResDet.getTimestamp("RESDET_DATE")));
                        newResultsDetail.setResdetTime(DateHelper.toDatetime(rsResDet.getTimestamp("RESDET_TIME")));
                        newResultsDetail.setResdetVolume(rsResDet.getFloat("RESDET_VOLUME"));
                        newResultsDetail.setInvtypId(rsResDet.getInt("INVTYP_ID"));
                        //newResultsDetail.assignByName(rsResDet);

                        skdGrpId = ((Integer)newCEIdMap.get( new Integer(newResultsDetail.getSkdgrpId()) )) .intValue();
                        newResultsDetail.setSkdgrpId(skdGrpId);
                        skdGrpList.add(newResultsDetail);
                    }
                    resultsDetailAccess.insertRecordData(skdGrpList, ResultsDetailAccess.SO_RESULTS_DETAIL_TABLE);
                }

                //----END COPY RESULTS_DETAIL RECORDS-------------

                //----BEGIN COPY DISTRIBUTION + DETAILS-------------\

                if ("Y".equalsIgnoreCase(strCopyDist))
                {
                    logger.debug( "Copy Distribution Data flag enabled." );

                    HashMap distMap = new HashMap();
                    RecordAccess recordAccess = new RecordAccess(conn);
                    List distributionList = new ArrayList();
                    List distDetList = new ArrayList();
                    List templateIds = new ArrayList();
                    Distribution distributionData = null;
                    DistributionDetail distributionDetailData = null;

                    //COPY DISTRIBUTION
                    templateIds.addAll(newCEIdMap.keySet());

                    StringBuffer sDistribSelectSQL = new StringBuffer();
                    sDistribSelectSQL.append(" SELECT * ");
                    sDistribSelectSQL.append(" FROM ");
                    sDistribSelectSQL.append(" SO_DISTRIBUTION ");
                    sDistribSelectSQL.append(" WHERE ");
                    sDistribSelectSQL.append(" SKDGRP_ID in ( ");
                    for (int i = 0; i < templateIds.size(); i++)
                    {
                        sDistribSelectSQL.append(i > 0 ? ",?" : "?");
                    }
                    sDistribSelectSQL.append(" ) ");

                    psSelectDistrib = conn.prepareStatement(sDistribSelectSQL.toString());

                    int param = 1;
                    Iterator it = templateIds.iterator();
                    while(it.hasNext())
                    {
                        psSelectDistrib.setInt(param++, ((Integer)it.next()).intValue());
                    }

                    rsDistrib = psSelectDistrib.executeQuery();
                    int distId = 0;
                    while(rsDistrib.next())
                    {
                        distributionData = new Distribution();
                        distributionData.assignByName(rsDistrib);
                        distId = distributionData.getDistId().intValue();
                        distributionData.setDistId(new Integer(conn.getDBSequence("SEQ_DIST_ID").getNextValue()));
                        distributionData.setSkdgrpId((Integer)newCEIdMap.get(distributionData.getSkdgrpId()));

                        //change distribution name
                        String oldDistName = distributionData.getDistName();
                        oldDistName = replaceOldPrefix(strOldPrefix, oldDistName);
                        String newDistName = strNewPrefix + oldDistName;
                        if(newDistName.length()>40)
                        {
                            newDistName = newDistName.substring(0,40);
                        }
                        distributionData.setDistName(newDistName);

                        //change distribution desc
                        String oldDistDesc = distributionData.getDistDesc();
                        if(!StringHelper.isEmpty(oldDistDesc))
                        {
	                        oldDistDesc = replaceOldPrefix(strOldPrefix, oldDistDesc);
	                        String newDistDesc = strNewPrefix + oldDistDesc;
	                        if(newDistDesc.length()>40)
	                        {
	                            newDistDesc = newDistDesc.substring(0,40);
	                        }
	                        distributionData.setDistDesc(newDistDesc);
                        }

                        distributionList.add(distributionData);

                        //create map of old dist with new
                        distMap.put(new Integer(distId), distributionData.getDistId());
                    }
                    recordAccess.insertRecordData(distributionList, Distribution.DISTRIBUTION_TABLE);

                    //END COPY DISTRIBUTION

/*                    //COPY DISTRIBUTION DETAIL
                    List distIds = new ArrayList();
                    distIds.addAll(distMap.keySet());

                    if(distIds.size() > 0)
                    {
	                    StringBuffer sSelectDistDetSQL = new StringBuffer();
	                    sSelectDistDetSQL.append(" SELECT * ");
	                    sSelectDistDetSQL.append(" FROM ");
	                    sSelectDistDetSQL.append(" SO_DISTRIB_DETAIL ");
	                    sSelectDistDetSQL.append(" WHERE ");
	                    sSelectDistDetSQL.append(" DIST_ID IN ( ");
	                    for (int i = 0; i < distIds.size(); i++)
	                    {
	                        sSelectDistDetSQL.append(i > 0 ? ",?" : "?");
	                    }
	                    sSelectDistDetSQL.append(" ) ");

	                    psSelectDistribDet = conn.prepareStatement(sSelectDistDetSQL.toString());

	                    param = 1;
	                    it = distIds.iterator();
	                    while(it.hasNext())
	                    {
	                        psSelectDistribDet.setInt(param++, ((Integer)it.next()).intValue());
	                    }

	                    rsDistribDet = psSelectDistribDet.executeQuery();
	                    while(rsDistribDet.next())
	                    {
	                        distributionDetailData = new DistributionDetail();
	                        distributionDetailData.assignByName(rsDistribDet);
	                        if(rsDistribDet.getString("DP_ID") == null)
	                        {
	                            distributionDetailData.setDpId(null);
	                        }
	                        distributionDetailData.setDistdetId(conn.getDBSequence("SEQ_DISTDET_ID").getNextValue());
	                        distributionDetailData.setDistId(((Integer)distMap.get(new Integer(distributionDetailData.getDistId()))).intValue());
	                        distDetList.add(distributionDetailData);
	                    }
	                    recordAccess.insertRecordData(distDetList, DistributionDetail.DISTRIBUTION_DETAIL_TABLE);
                    }
                    //END COPY DISTRIBUTION DETAIL
*/
                    //FIX DAY_DIST_ID IN SCHEDULE GROUPS
                    List newSkdgrpIds = new ArrayList();
                    newSkdgrpIds.addAll(newCEIdMap.values());

                    StringBuffer sbSelectSkdgrp = new StringBuffer();
                    sbSelectSkdgrp.append(" SELECT * ");
                    sbSelectSkdgrp.append(" FROM ");
                    sbSelectSkdgrp.append(" SO_SCHEDULE_GROUP ");
                    sbSelectSkdgrp.append(" WHERE ");
                    sbSelectSkdgrp.append(" SKDGRP_ID IN ( ");
                    for (int i = 0; i < newSkdgrpIds.size(); i++)
                    {
                        sbSelectSkdgrp.append(i > 0 ? ",?" : "?");
                    }
                    sbSelectSkdgrp.append(" ) ");

                    psSelectSkdgrp = conn.prepareStatement(sbSelectSkdgrp.toString());

            		param = 1;
            		it = newSkdgrpIds.iterator();
            		while(it.hasNext())
            		{
            		    psSelectSkdgrp.setInt(param++, ((Integer)it.next()).intValue());
            		}

            		rsSkdgrp = psSelectSkdgrp.executeQuery();
            		List newScheduleGroupList = new ArrayList();
            		ScheduleGroupData newScheduleGroup = null;

            		while(rsSkdgrp.next())
            		{
            		    newScheduleGroup = new ScheduleGroupData();
            		    newScheduleGroup.assignByName(rsSkdgrp);
            		    newScheduleGroup.setSkdgrpSunDistId(new Integer(getDefaultDist(distMap, SKDGRP_SUN_DIST_ID_COLNAME, rsSkdgrp)));
            		    newScheduleGroup.setSkdgrpMonDistId(new Integer(getDefaultDist(distMap, SKDGRP_MON_DIST_ID_COLNAME, rsSkdgrp)));
            		    newScheduleGroup.setSkdgrpTueDistId(new Integer(getDefaultDist(distMap, SKDGRP_TUE_DIST_ID_COLNAME, rsSkdgrp)));
            		    newScheduleGroup.setSkdgrpWedDistId(new Integer(getDefaultDist(distMap, SKDGRP_WED_DIST_ID_COLNAME, rsSkdgrp)));
            		    newScheduleGroup.setSkdgrpThuDistId(new Integer(getDefaultDist(distMap, SKDGRP_THU_DIST_ID_COLNAME, rsSkdgrp)));
            		    newScheduleGroup.setSkdgrpFriDistId(new Integer(getDefaultDist(distMap, SKDGRP_FRI_DIST_ID_COLNAME, rsSkdgrp)));
            		    newScheduleGroup.setSkdgrpSatDistId(new Integer(getDefaultDist(distMap, SKDGRP_SAT_DIST_ID_COLNAME, rsSkdgrp)));
            		    newScheduleGroupList.add(newScheduleGroup);
            		}
            		recordAccess.updateRecordData(newScheduleGroupList, ScheduleGroupData.TABLE_NAME, ScheduleGroupData.PRIMARY_KEY);

            		//END FIX DIST ID FOR SCHEDULE GROUPS
                }

                //----END COPY DISTRIBUTION + DETAILS-------------

/*
                if( strCreateReaders != null )
                {
                    logger.debug( "Create Readers flag enabled." );

                    //----BEGIN CREATE READERS -------------



                    //----END CREATE READERS -------------
                }
*/

                return oNewCE.getSkdgrpData().getWbtId();
            }
            finally
            {
                //resultDetail sql utils:
                SQLHelper.cleanUp(psSelectResDet, rsResDet);

                //distibution sql utils:
                SQLHelper.cleanUp(psSelectDistrib, rsDistrib);
                SQLHelper.cleanUp(psSelectDistribDet, rsDistribDet);
                SQLHelper.cleanUp(psSelectSkdgrp, rsSkdgrp);
            }
        }
        return -1;
    }

    /**
     * Returns the list of new Workbrain teams of type "STORE" that doesn not currently
     * have a location associated with it.
     *
     * @return  a list of WorkbrainTeamData objects of type STORE that do no have
     *          a location
     * @throws  an SQL Exception, but handled at the main "run()" method
     */
    private List getNewTeamsList() throws Exception
    {

        // Get the STORE type ID
        CodeMapper cm = CodeMapper.createCodeMapper( conn );
        WorkbrainTeamTypeData wbTeamTypeData = ( WorkbrainTeamTypeData )cm.getWBTeamTypeByName( STORE_TYPE_NAME );
        int storeTypeID = wbTeamTypeData.getWbttId();

        // Load the list of Workbrain Teams
        WorkbrainTeamAccess wbTeamAcc = new WorkbrainTeamAccess( conn );
        List wbTeamList = wbTeamAcc.loadAll();

        // Create List of teams that need a location
        List newLocationNameList = new ArrayList();

        Iterator iter = wbTeamList.iterator();
        while( iter.hasNext() )
        {
            WorkbrainTeamData wbTeamData = ( WorkbrainTeamData )iter.next();

            // Continue if current team is of type STORE
            if( wbTeamData.getWbttId() == storeTypeID )
            {
                PreparedStatement stmt = null;
                ResultSet rs = null;
                try
                {
                    // Check to see if there is a location exists, if no, then add to the new list
                    stmt = conn.prepareStatement(
                        "SELECT SKDGRP_ID FROM SO_SCHEDULE_GROUP WHERE WBT_ID = ?");
                    stmt.setInt(1, wbTeamData.getWbtId());
                    rs = stmt.executeQuery();

                    // If no results found, then add to the new list
                    if (!rs.next()) {
                        newLocationNameList.add(wbTeamData);
                    }
                }
                finally
                {
                    SQLHelper.cleanUp( rs );
                    SQLHelper.cleanUp( stmt );
                }

            }
        }
        return newLocationNameList;

    }

    /**
     * This method traverses the teamIDs (each entry with old, new IDs) and updates
     * the WORKBRAIN_TEAM and SO_SCHEDULE_GROUP tables with the correct parent IDs.
     * Reason for this is the location copy creates a new team structure for each new
     * workbrain team, thus duplicating the team at the parent level (of type store).
     * This method will update all the children of the newly created parent to the
     * original (correct) team in order to have the SO component function properly.
     *
     * @param teamIDs
     * @exception Passes the exception to the main "run()" method
     */
    private void syncWBTeams( HashMap teamIDs ) throws Exception
    {
        PreparedStatement updateSkdGroupStmt = null;
        PreparedStatement updateWBTeamStmt = null;
        PreparedStatement updateHoursOfOperationStmt = null;
        PreparedStatement updateWBTeamHOPStmt = null;
        PreparedStatement removeStmt = null;

        final String updateHoursOfOperation =
            "UPDATE HOURS_OF_OPERATION SET WBT_ID = ? WHERE WBT_ID = ?";
        final String updateSkdGroup =
            "UPDATE SO_SCHEDULE_GROUP SET WBT_ID = ? WHERE WBT_ID = ?";
        final String updateWBTeam =
            "UPDATE WORKBRAIN_TEAM SET WBT_PARENT_ID = ? WHERE WBT_PARENT_ID = ?";
        final String updateWBTeamHOP =
            "UPDATE WORKBRAIN_TEAM SET HRSOP_ID = (SELECT HRSOP_ID FROM WORKBRAIN_TEAM WHERE WBT_ID = ? ) WHERE WBT_ID = ?";
        final String removeNewTeams =
            "DELETE FROM WORKBRAIN_TEAM WHERE WBT_ID = ?";

        Set oldIDs = teamIDs.keySet();
        Iterator iter = oldIDs.iterator();

        try
        {
            /*
             * Traverse the list of ID's, generate the appropriate statements,
             * and add to the batch for execution later.
             */
            updateHoursOfOperationStmt = conn.prepareStatement( updateHoursOfOperation );
            updateSkdGroupStmt = conn.prepareStatement( updateSkdGroup );
            updateWBTeamStmt = conn.prepareStatement( updateWBTeam );
            updateWBTeamHOPStmt = conn.prepareStatement( updateWBTeamHOP );
            removeStmt = conn.prepareStatement( removeNewTeams );
            while( iter.hasNext() )
            {
                int oldID = (( Integer )iter.next()).intValue();
                int newID = (( Integer )teamIDs.get( new Integer( oldID ))).intValue();

                //Update the Hours of Operation Table
                updateHoursOfOperationStmt.setInt( 1, oldID );
                updateHoursOfOperationStmt.setInt( 2, newID );
                updateHoursOfOperationStmt.addBatch();

                // Update the SO Schedule Group Table

                updateSkdGroupStmt.setInt( 1, oldID );
                updateSkdGroupStmt.setInt( 2, newID );
                updateSkdGroupStmt.addBatch();

                // Update the WB Team table
                updateWBTeamStmt.setInt( 1, oldID );
                updateWBTeamStmt.setInt( 2, newID );
                updateWBTeamStmt.addBatch();

                // Update the WB Team with HOP
                updateWBTeamHOPStmt.setInt(1, newID);
                updateWBTeamHOPStmt.setInt(2, oldID);
                updateWBTeamHOPStmt.addBatch();

                // Remove the newly created teams
                removeStmt.setInt( 1, newID );
                removeStmt.addBatch();
            }

            updateHoursOfOperationStmt.executeBatch();
            updateSkdGroupStmt.executeBatch();
            updateWBTeamStmt.executeBatch();
            updateWBTeamHOPStmt.executeBatch();
            removeStmt.executeBatch();
        }
        finally
        {
            SQLHelper.cleanUp( updateHoursOfOperationStmt );
            SQLHelper.cleanUp( updateSkdGroupStmt );
            SQLHelper.cleanUp( updateWBTeamStmt );
            SQLHelper.cleanUp( updateWBTeamHOPStmt );
            SQLHelper.cleanUp( removeStmt );
        }

        // *** TT1712 syncWorkbrainTeamTable must be callled as parents are being changed above
        WorkbrainTeamManager teamManager = new WorkbrainTeamManager(DBInterface.
            getCurrentConnection());
        teamManager.syncWorkbrainTeamTable();

    }

    private int getDefaultDist(Map distMap, String colName, ResultSet rsSkdgrp) throws SQLException
    {

        Integer oldDist = new Integer(rsSkdgrp.getInt(colName));
        if (distMap.containsKey(oldDist))
        {
            return ((Integer)distMap.get(oldDist)).intValue();
        }
        //resultSet returns 0's for nulls, set them to 1 for flat dist if not set.
        return (oldDist.intValue()!=0)?oldDist.intValue():1;
    }

    public LocationCopyPlugin getInterface()
    {
        //tries to retrieve the class name from the Registry
        try
		{
            className = getClassNameFromRegistry();
            if (className == null)
            {
               return null;
            }
        }
        catch (Exception e)
		{
        	logger.error(e);
        	return null;
        }
        //tries to load the class
        if (pluginClass == null)
        {
            try
			{
            	pluginClass = Class.forName(className);
            }
            catch(Exception e)
			{
                logger.error("Unable to load the class " + className, e);
                return null;
            }
        }
        //tries to instantiate the class
        if (pluginObject == null)
        {
            try
			{
            	pluginObject = pluginClass.newInstance();
            }
            catch (Exception ee)
			{
                logger.error("Unable to instantiate the class " + className, ee);
                return null;
            }
            if ((pluginObject instanceof LocationCopyPlugin) == false)
            {
                logger.error("The class loaded is not an implementation of the LocationCopyPlugin.");
                return null;
            }
        }
        return (LocationCopyPlugin)pluginObject;
    }

    private String getClassNameFromRegistry()
    	throws Exception
	{
        Object obj = Registry.getVar(REG_LOCATION_COPY_PLUGIN);
        if ((obj instanceof String) == false)
        {
            return null;
        }
        return (String)obj;
    }

    public String replaceOldPrefix(String oldPrefix,String oldName)
    {
        if(oldPrefix == null)
        {
            oldPrefix = "";
        }
        if(oldName == null)
        {
            oldName = "";
        }
        if ((oldPrefix.length() > 0) && (oldName.length() >= oldPrefix.length())
                && (oldName.substring(0, oldPrefix.length()).equalsIgnoreCase(oldPrefix)))
        {
            //get rid of the old prefix:
            oldName = oldName.substring(oldPrefix.length(), oldName.length());
        }
        return oldName;
    }

    public static void main(String[] args)
    {
    }
}
