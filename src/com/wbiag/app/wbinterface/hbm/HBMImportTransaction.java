package com.wbiag.app.wbinterface.hbm;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.workbrain.app.modules.retailSchedule.db.ClientStfdefAccess;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.VolumeWorkloadAccess;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntityStaffRequirement;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.model.VolumeWorkload;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.app.wbinterface.TransactionTypeBatch;
import com.workbrain.app.wbinterface.WBInterfaceException;
import com.workbrain.app.wbinterface.db.ImportData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

/** Title:         HBMImportTransaction
 * Description:    imports data into the SO_CLIENT_STFDEF table
 * Copyright:      Copyright (c) 2006
 * Company:        Workbrain Inc
 * @author         Philip Liew
 * @version 1.0
 */

public class HBMImportTransaction extends TransactionTypeBatch
{
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HBMImportTransaction.class);

	public static final int SKDGRP_PARENT_NAME_COL = 0;
	public static final int SKDGRP_NAME_COL = 1;
	public static final int STANDARD_DESCRIPTION_COL = 2;
	public static final int JOB_NAME_COL = 3;
	public static final int ACT_NAME_COL = 4;
	public static final int VOLTYP_NAME_COL = 5;
	public static final int WRKLD_STDVOL_HOUR_COL = 6;
    public static final int STANDARD_TYPE_COL = 7;
    public static final int CSD_EFF_START_DATE_COL = 8;
    public static final int CSD_EFF_END_DATE_COL = 9;
    
    public static final String ACTIVITY_TABLE_NAME = "SO_ACTIVITY";
    public static final String JOB_TABLE_NAME = "JOB";
    public static final String SKDGRP_TABLE_NAME = "SO_SCHEDULE_GROUP";
	public static final String STAFF_DEF_TABLE_NAME = "SO_CLIENT_STFDEF";
	public static final String STAFF_DEF_ID_FIELD_NAME = "CSD_ID";
	public static final String VOLUME_TABLE_NAME = "SO_VOLUME_WORKLOAD";
	public static final String VOLUME_ID_FIELD_NAME = "WRKLD_ID";
    
	public static final String PARAM_BATCH_PROCESS_SIZE = "batchProcessSize";
	public static final int DEFAULT_BATCH_PROCESS_SIZE  = 100;

	public static final String PARAM_DEFAULT_START_DATE = "defaultStartDate";
	public static final String PARAM_DEFAULT_END_DATE = "defaultEndDate";
	public static final String DEFAULT_START_DATE = DateHelper.convertDateString(new Date(), "MM/dd/yyyy");
	public static final String DEFAULT_END_DATE = "01/01/3000";

	//Default values for staffing requirement
	private static final Integer DEFAULT_SKILL_LEVEL = new Integer(1);
	private static final Integer DEFAULT_MIN_REQ = new Integer(0);
	private static final Integer DEFAULT_MAX_REQ = new Integer(0);
	private static final Integer DEFAULT_SHIFT_STAFF_REQ_ID = new Integer(1);
	private static final Integer DEFAULT_CONTROL_WORK = new Integer(0);

	// Error Messages
	public static final String INVALID_DATE_MESSAGE = "Error - Date format is incorrect";
	public static final String INVALID_STORE_MESSAGE = "Error - Store not found in system";
	public static final String INVALID_DEPARTMENT_MESSAGE = "Error - Department not found in system";
	public static final String INVALID_ACTIVITY_MESSAGE = "Error - Activity not found in system";
	public static final String INVALID_JOB_MESSAGE = "Error - Job not found in system";
	public static final String INVALID_VOL_PROD_MESSAGE = "Error - Volume Productivity not provided";
		
	protected String status = ImportData.STATUS_APPLIED;
	protected String message;
	protected HashMap params;
	protected CodeMapper codeMapper;

	public HBMImportTransaction(){}

	public void initializeTransaction(DBConnection conn)
	{
	}
	public void finalizeTransaction(DBConnection conn)
	{
	}
	public void process(ImportData data, DBConnection conn)
	{
	}
	protected void preProcessBatch(DBConnection c, List importDataList)
		throws SQLException, WBInterfaceException
	{
	}
	protected void postProcessBatch(DBConnection c, List importDataList)
		throws SQLException, WBInterfaceException
	{
	}
	public List processBatch(DBConnection conn, List importDataList)
		throws Exception
	{
		List results;
		preProcessBatch(conn, importDataList);

//		loadCache(conn, importDataList);
		results = processRecords(conn, importDataList);

		postProcessBatch(conn, results);
		return results;
	}

	public List processRecords(DBConnection conn, List importDataList)
		throws Exception
	{
		Iterator it = importDataList.iterator();
		ImportData data;
		RecordAccess ra;
		CorporateEntityStaffRequirement srd;
		VolumeWorkload vwd;

		ArrayList staffDefInserts = new ArrayList();
		ArrayList staffDefUpdates = new ArrayList();
		ArrayList volumeUpdates = new ArrayList();
		ArrayList volumeInserts = new ArrayList();

		int stfDefSeqNum = conn.getDBSequence("SEQ_CSD_ID").getNextValue(importDataList.size());
		int volWrkldSeqNum = conn.getDBSequence("SEQ_WRKLD_ID").getNextValue(importDataList.size());

		try
		{
			ra = new RecordAccess(conn);
        	DBInterface.init(conn);
						
			while(it.hasNext())
			{
				data = (ImportData)it.next();
				data.setStatus(ImportData.STATUS_ERROR);

				// Retreive entries from ImportData object
	        	String activity = data.getField(ACT_NAME_COL);
	        	String job = data.getField(JOB_NAME_COL);
	        	String skdGrp = data.getField(SKDGRP_NAME_COL);
	        	String skdGrpParent = data.getField(SKDGRP_PARENT_NAME_COL);
	        	String volume = data.getField(WRKLD_STDVOL_HOUR_COL);
	        	String volumeName = data.getField(VOLTYP_NAME_COL);
	        	String effStartDate = data.getField(CSD_EFF_START_DATE_COL);
	        	String effEndDate = data.getField(CSD_EFF_END_DATE_COL);
	        	
	        	CodeMapper cm = getCodeMapper(conn);
	        	Integer activityId = null;
	        	Integer jobId = null;
	        	Integer skdGrpId = null;

	        	// Validate if Activity, Job, ScheduleGroup exist in the current database
	        	if ( activity == null || activity.length() == 0 || cm.getSOActivityByName(activity.trim().toUpperCase()) == null ) {
	        		data.setMessage(INVALID_ACTIVITY_MESSAGE);
	        		continue;
	        	} else {
	        		activityId = cm.getSOActivityByName(activity.trim().toUpperCase()).getActId();
	        	}
	        	if ( job == null || job.length() == 0 || cm.getJobByName(job.trim().toUpperCase()) == null ) {
	        		data.setMessage(INVALID_JOB_MESSAGE);
	        		continue;
	        	} else {
	        		jobId = new Integer(cm.getJobByName(job.trim().toUpperCase()).getJobId());
	        	}
	        	if ( skdGrp != null && skdGrp.length() != 0 && skdGrpParent != null && skdGrpParent.length() != 0) {
	        		ScheduleGroupData skdObject = cm.getScheduleGroupByName(skdGrp.trim().toUpperCase());
	        		ScheduleGroupData parentObject = cm.getScheduleGroupByName(skdGrpParent.trim().toUpperCase());
	        		
	        		if ( skdObject == null || parentObject == null || parentObject.getSkdgrpId().intValue() != skdObject.getSkdgrpParentId().intValue() ) {
		        		data.setMessage(INVALID_STORE_MESSAGE);
	        			continue;
	        		}
	        		skdGrpId = skdObject.getSkdgrpId();
	        	} else {
	        		data.setMessage(INVALID_STORE_MESSAGE);
        			continue;	        		
	        	}
	        	// Validate if volume workload exists
	        	if ( volume == null || volume.length() == 0 ) {
	        		data.setMessage(INVALID_VOL_PROD_MESSAGE);
	        		continue;
	        	}

	        	// Create new Data classes to either insert/update in database
	        	srd = new CorporateEntityStaffRequirement();
	        	vwd = new VolumeWorkload();
	        	srd.assignIsNew(true);
	        	vwd.assignIsNew(true);
	        	
	        	// Properly format data
	        	activity = activity.trim().toUpperCase();
	        	job = job.trim().toUpperCase();
	        	skdGrp = skdGrp.trim().toUpperCase();
	        	volumeName = volumeName.trim().toUpperCase();

	        	// Check for invalid date
	        	Date start;
	        	Date end;
	        	if ( effStartDate == null || effStartDate.length() == 0 ) {
	        		start = getDefaultStartDate(params);
	        	} else {
	        		try {
	        			start = DateHelper.convertStringToDate(effStartDate, "MM/dd/yyyy");
	        		}
	        		catch (Exception e) {
	        			// Invalidly formatted date
		        		data.setMessage(INVALID_DATE_MESSAGE);
		        		continue;
	        		}
	        	}
	        	if ( effEndDate == null || effEndDate.length() == 0 ) {
	        		end = getDefaultEndDate(params);
	        	} else {
	        		try {
	        			end = DateHelper.convertStringToDate(effEndDate, "MM/dd/yyyy");
	        		}
	        		catch (Exception e) {
	        			// Invalidly formatted date
		        		data.setMessage(INVALID_DATE_MESSAGE);
		        		continue;
	        		}
	        	}
	        	// Invert the volume workload
	        	Double vol = Double.valueOf(volume);
	        	if ( vol.doubleValue() != 0 ) {
	        		vol = new Double(1/vol.doubleValue());
	        	}	        	
	        	
	        	
	        	// Set Staffing Requirements Default Values
	        	srd.setSkillLevel(DEFAULT_SKILL_LEVEL);
	        	srd.setCsdMinReq(DEFAULT_MIN_REQ);
	        	srd.setCsdMaxReq(DEFAULT_MAX_REQ);
	        	srd.setShftstfreqId(DEFAULT_SHIFT_STAFF_REQ_ID);
	        	srd.setCsdControlWrk(DEFAULT_CONTROL_WORK);
	        	
	        	// Set the Staffing requirement Values
	        	srd.setActId(activityId);
	        	srd.setJobId(jobId);
	        	srd.setSkdgrpId(skdGrpId);
	        	srd.setCsdEffEndDate(end);
	        	srd.setCsdEffStartDate(start);

	        	// Set the Volume Workload values
	        	vwd.setWrkldDesc(volumeName);
            	vwd.setWrkldStdvolHour(vol.doubleValue());
            	vwd.setSkdgrpId(skdGrpId.intValue());

	        	// Proceed to enter into SO_CLIENT_STFDEF
	        	// First check if entry already exists in the database, if so update, if not insert
	        	String whereClause = "ACT_ID=" + activityId + " AND SKDGRP_ID=" + skdGrpId + " AND JOB_ID=" + jobId;
	            List schedGroups = new ClientStfdefAccess(conn).loadRecordData(
	            		new CorporateEntityStaffRequirement(), STAFF_DEF_TABLE_NAME, whereClause );
	            Iterator iter = schedGroups.iterator();
	            
	            if ( iter.hasNext() ) {
		            // Update Record
	            	CorporateEntityStaffRequirement item = (CorporateEntityStaffRequirement)iter.next();
	            	srd.setCsdId(item.getCsdId());
	            	staffDefUpdates.add(srd);
	            	
	            	// Check if volume workload exists
	            	vwd.setCsdId(item.getCsdId().intValue());
		            List volWrkLoad = new VolumeWorkloadAccess(conn).loadRecordData(
		            		new VolumeWorkload(), VOLUME_TABLE_NAME, "CSD_ID="+vwd.getCsdId()+" AND SKDGRP_ID="+vwd.getSkdgrpId()+" AND WRKLD_DESC='"+vwd.getWrkldDesc()+"'");
		        
		            Iterator volIter = volWrkLoad.iterator();
		            if ( volIter.hasNext() ) {
		            	// Update the volume
		            	VolumeWorkload volItem = (VolumeWorkload) volIter.next();
		            	vwd.setWrkldId(volItem.getWrkldId());
		            	volumeUpdates.add(vwd);
		            	
		            } else {
		            	// Insert the volume
		            	vwd.setWrkldId(volWrkldSeqNum++);
		            	volumeInserts.add(vwd);
		            }
	            } else {
	            	// Insert Staffing Definition Record
	            	Integer csdId = new Integer(stfDefSeqNum++);
	            	srd.setCsdId(csdId);
	            	staffDefInserts.add(srd);
	            	
	            	// Insert Volume Workload Record
	            	vwd.setWrkldId(volWrkldSeqNum++);
	            	vwd.setCsdId(csdId.intValue());
	            	volumeInserts.add(vwd);
	            }
	            data.setStatus(ImportData.STATUS_APPLIED);
			}
			ra.insertRecordData(staffDefInserts, STAFF_DEF_TABLE_NAME);
			ra.insertRecordData(volumeInserts, VOLUME_TABLE_NAME);
			ra.updateRecordData(staffDefUpdates, STAFF_DEF_TABLE_NAME, STAFF_DEF_ID_FIELD_NAME);
			ra.updateRecordData(volumeUpdates, VOLUME_TABLE_NAME,VOLUME_ID_FIELD_NAME);
		}
		catch(Exception e)
		{
		    if(conn != null)
		    {
		        conn.rollback();
		    }
		    throw e;
		}
		return importDataList;
	}

	public CodeMapper getCodeMapper(DBConnection conn)
		throws SQLException
	{
		if(codeMapper == null)
		{
			codeMapper = CodeMapper.createCodeMapper(conn);
		}
		return codeMapper;
	}

	public String getStatus()
	{
		if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
		{
			logger.debug("HBMImportTransaction.getStatus() returns " + status);
		}
		return status;
	}

	public String getMessage()
	{
		if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
		{
			logger.debug("HBMImportTransaction.getMessage");
		}
		return message;
	}

	public String getTaskUI()
	{
		return "/jobs/wbinterface/hbm/hbmParams.jsp";
	}

	public void reset()
	{
		message = "";
		status = ImportData.STATUS_APPLIED;
	}

	public void resetBatch()
	{
		message = "";
		status = ImportData.STATUS_APPLIED;
	}

	public void setTransactionParameters( HashMap param )
	{
		params = (HashMap) param;
	}

	public int getBatchProcessSize(HashMap params) {
		int size = params!= null && params.containsKey(PARAM_BATCH_PROCESS_SIZE)
				? params.get(PARAM_BATCH_PROCESS_SIZE) == null
						  ? DEFAULT_BATCH_PROCESS_SIZE :
						  Integer.parseInt( (String) params.get(PARAM_BATCH_PROCESS_SIZE))
				: 1;
		return size;
	}

	public Date getDefaultStartDate(HashMap params) {
		Date date = params!= null && params.containsKey(PARAM_DEFAULT_START_DATE)
				? params.get(PARAM_DEFAULT_START_DATE) == null || ((String)params.get(PARAM_DEFAULT_START_DATE)).length() == 0
						  ? DateHelper.convertStringToDate(DEFAULT_START_DATE, "MM/dd/yyyy"):
							  DateHelper.convertStringToDate( (String) params.get(PARAM_DEFAULT_START_DATE), "yyyyMMdd HHmmss")
				: DateHelper.convertStringToDate(DEFAULT_START_DATE, "MM/dd/yyyy");
		return date;
	}

	public Date getDefaultEndDate(HashMap params) {
		Date date = params!= null && params.containsKey(PARAM_DEFAULT_END_DATE)
				? params.get(PARAM_DEFAULT_END_DATE) == null || ((String)params.get(PARAM_DEFAULT_END_DATE)).length() == 0
						  ? DateHelper.convertStringToDate(DEFAULT_END_DATE, "MM/dd/yyyy"):
							  DateHelper.convertStringToDate( (String) params.get(PARAM_DEFAULT_END_DATE), "yyyyMMdd HHmmss")
				: DateHelper.convertStringToDate(DEFAULT_END_DATE, "MM/dd/yyyy");
		return date;
	}
}
