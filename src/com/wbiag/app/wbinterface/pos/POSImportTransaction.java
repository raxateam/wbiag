/*
 * Created on Nov 29, 2004
 *
 */

package com.wbiag.app.wbinterface.pos;

import com.workbrain.app.wbinterface.*;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.util.*;
import com.wbiag.app.ta.model.SOResultsDetailData;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.modules.retailSchedule.model.*;
import com.workbrain.app.modules.retailSchedule.db.*;


import java.util.*;
import java.sql.SQLException;

/** Title:         POSImportTransaction
 * Description:    imports data into the SO_RESULTS_DETAIL table
 * Copyright:      Copyright (c) 2003
 * Company:        Workbrain Inc
 * @author         Kevin Tsoi
 * @version 1.0
 * 
 */

public class POSImportTransaction extends TransactionTypeBatch
{
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(POSImportTransaction.class);

	public static final int SKDGRP_NAME_COL = 0;
	public static final int INVTYP_ID_COL = 1;
	public static final int RESDET_DATE_COL = 2;
	public static final int RESDET_TIME_COL = 3;
	public static final int RESDET_VOLUME_COL = 4;
	public static final int INPUT_INVTYP_ID_COL = 5;
	public static final int OLD_SKDGRP_NAME_COL = 6;
    public static final int VOLTYP_COL = 7;
	public static final String DAILY_TIME = "00:00:00";
	public static final String TABLE_NAME = "SO_RESULTS_DETAIL";
	public static final String ID_FIELD_NAME = "RESDET_ID";
	public static final String PARAM_BATCH_PROCESS_SIZE = "batchProcessSize";
	public static final int DEFAULT_BATCH_PROCESS_SIZE  = 100;
	public static final String PARAM_AGGREGATE = "aggregate";
	public static final int DEFAULT_AGGREGATE  = 1;
	public static final String HISTORIC_UPDATE_ERROR_MESSAGE = "Historic data does not exist.";
	public static final String INVALID_DATA_ERROR_MESSAGE = "Invalid skdgrp_name provided.";
	public static final String INVALID_VOLTYP_ERROR_MESSAGE = "Invalid voltyp_name provided.";
	public static final String REG_SKIP_INVALID_RECORD = "system/POS/SKIP_INVALID_RECORD";
	public static final String DATE_FORMAT = "MM/dd/yyyy";
	public static final String DATETIME_FORMAT = "MM/dd/yyyyHH:mm:ss";

	protected String status = ImportData.STATUS_APPLIED;
	protected String message;
	protected HashMap params;
	protected CodeMapper codeMapper;
	protected POSMapper posMapper;

	private HashMap POSCache = new HashMap();
    private Map volumeTypesCache = null;

	public POSImportTransaction(){}

	public void initializeTransaction(DBConnection conn)
	{
        try {
            loadVolumeTypes(conn);
        }
        catch (Exception ex) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error when loading volume types" , ex);}
            throw new NestedRuntimeException("Error when loading volume types", ex);
        }
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
		if(getIsAggregate(params) == 1)
		{
			results = processNoAggregate(conn, importDataList);
		}
		else
		{
			results = processAggregate(conn, importDataList);
		}
		postProcessBatch(conn, results);
		getPOSMapper(conn).clear();
		return results;
	}

	public List processNoAggregate(DBConnection conn, List importDataList)
		throws Exception
	{
		Iterator it = importDataList.iterator();
		ImportData data;
		SOResultsDetailData rdd;
		RecordAccess ra;
		CodeMapper cm;
		POSMapper pm;
		POSData pd;
		ScheduleGroupData sg;
		int skdgrp_id;
		ArrayList inserts = new ArrayList();
		ArrayList updates = new ArrayList();

		int seqNum = conn.getDBSequence("SEQ_RESDET_ID").getNextValue(importDataList.size());
		boolean skipRecord = Registry.getVarBoolean(REG_SKIP_INVALID_RECORD);

		try
		{
			ra = new RecordAccess(conn);
			cm = getCodeMapper(conn);
			pm = getPOSMapper(conn);
			
			while(it.hasNext())
			{
				data = (ImportData)it.next();
				data.setStatus(ImportData.STATUS_ERROR);
				rdd = new SOResultsDetailData();

				//check if schedule group exist
				sg = cm.getScheduleGroupByName(data.getField(SKDGRP_NAME_COL));
				if(sg != null)
				{
					skdgrp_id = sg.getID().intValue();
				}
				else
				{
				    if(skipRecord)
				    {
				        continue;
				    }
				    else
				    {
				        throw new WBInterfaceException(INVALID_DATA_ERROR_MESSAGE);
				    }
				}

                String voltypName = data.getField(VOLTYP_COL);
                if (!StringHelper.isEmpty(voltypName)) {
                    Object val = volumeTypesCache.get(voltypName.toUpperCase().trim());
                    if (val != null) {
                        rdd.setVoltypId(((Integer)val));
                    }
                    else {
                        throw new WBInterfaceException (INVALID_VOLTYP_ERROR_MESSAGE);
                    }
                }
				rdd.setSkdgrpId(skdgrp_id);
				rdd.setInvtypId(Integer.parseInt(data.getField(INVTYP_ID_COL)));
				rdd.setResdetVolume(Float.parseFloat(data.getField(RESDET_VOLUME_COL)));
				rdd.setResdetDate(DateHelper.parseDate(data.getField(RESDET_DATE_COL), DATE_FORMAT));
				rdd.setResdetTime(DateHelper.parseDate(data.getField(RESDET_DATE_COL)+data.getField(RESDET_TIME_COL), DATETIME_FORMAT));

				//insert
				if(data.getField(OLD_SKDGRP_NAME_COL) == null || "".equals(data.getField(OLD_SKDGRP_NAME_COL)))
				{
					rdd.setResdetId(seqNum++);
					inserts.add(rdd);
				}
				//historic update
				else
				{
					if(!data.getField(SKDGRP_NAME_COL).equals(data.getField(OLD_SKDGRP_NAME_COL)))
					{
						//delete previous record if exist
						int voltypId = rdd.getVoltypId() == null ? POSMapper.NULL_VOLTYP_ID : rdd.getVoltypId().intValue();
						pd = pm.loadBySkdgrpDateTimeVolTyp(rdd.getSkdgrpId(), 
								rdd.getResdetDate(), rdd.getResdetTime(), voltypId);


						if(pd != null)
						{
							ra.deleteRecordData(TABLE_NAME, ID_FIELD_NAME, pd.getResdetId());
						}

					}
					//get old skpdgrp_id from code mapper
					sg = cm.getScheduleGroupByName(data.getField(OLD_SKDGRP_NAME_COL));
					if(sg != null)
					{
						skdgrp_id = sg.getID().intValue();
					}
					else
					{
						throw new WBInterfaceException(HISTORIC_UPDATE_ERROR_MESSAGE);
					}

					//add record to updates
					int voltypId = rdd.getVoltypId() == null ? POSMapper.NULL_VOLTYP_ID : rdd.getVoltypId().intValue();
					pd = pm.loadBySkdgrpDateTimeVolTyp(skdgrp_id, 
							rdd.getResdetDate(), rdd.getResdetTime(), voltypId);

					if(pd != null)
					{
						rdd.setResdetId(pd.getResdetId());
						updates.add(rdd);
					}
					else
					{
						throw new WBInterfaceException(HISTORIC_UPDATE_ERROR_MESSAGE);
					}

				}
				data.setStatus(ImportData.STATUS_APPLIED);
			}
			ra.insertRecordData(inserts, TABLE_NAME);
			ra.updateRecordData(updates, TABLE_NAME, ID_FIELD_NAME);
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

	public List processAggregate(DBConnection conn, List importDataList)
		throws Exception
	{
		Iterator it = importDataList.iterator();
		ImportData data;
		POSData posData;
		POSData currentPosData;
		java.util.Date tempTime;
		ScheduleGroupData sg;
		CodeMapper cm;
		String key;
		int insertCount = 0;

		boolean skipRecord = Registry.getVarBoolean(REG_SKIP_INVALID_RECORD);

		cm = getCodeMapper(conn);

		while(it.hasNext())
		{
		    data = (ImportData)it.next();
			data.setStatus(ImportData.STATUS_ERROR);

		  	//get skpdgrp_id from code mapper
			sg = cm.getScheduleGroupByName(data.getField(SKDGRP_NAME_COL));
			if(sg == null)
			{
			    if(skipRecord)
			    {
			        continue;
			    }
			    else
			    {
			        throw new WBInterfaceException(INVALID_DATA_ERROR_MESSAGE);
			    }
			}

			//create a POSData object from import data
			posData = new POSData();
			posData.setSkdgrpName(data.getField(SKDGRP_NAME_COL));
			posData.setInvtypId(Integer.parseInt(data.getField(INVTYP_ID_COL)));
			posData.setResdetVolume(Float.parseFloat(data.getField(RESDET_VOLUME_COL)));
			if(!StringHelper.isEmpty(data.getField(INPUT_INVTYP_ID_COL)))
			{
			    posData.setInputInvtypId(Integer.parseInt(data.getField(INPUT_INVTYP_ID_COL)));
			}
			else
			{
			    posData.setInputInvtypId(Integer.parseInt(data.getField(INVTYP_ID_COL)));
			}
			posData.setResdetDate(DateHelper.parseDate(data.getField(RESDET_DATE_COL), DATE_FORMAT));
			posData.setOldSkdgrpName(data.getField(OLD_SKDGRP_NAME_COL));

			//sets whether or not it is a historic update
			if(posData.getOldSkdgrpName()== null || "".equals(posData.getOldSkdgrpName()))
			{
				posData.setRecordType(0);
			}
			else
			{
				posData.setRecordType(2);
			}

			if (posData.getInvtypId() == posData.getInputInvtypId())
			{
				posData.setResdetTime(DateHelper.parseDate(data.getField(RESDET_DATE_COL)+data.getField(RESDET_TIME_COL), DATETIME_FORMAT));
			}
			else
			{
				//rounds record to appropriate interval
				switch (posData.getInvtypId())
				{
					case 1:
						posData.setResdetTime(DateHelper.parseDate(data.getField(RESDET_DATE_COL)+DAILY_TIME, DATETIME_FORMAT));
						break;

					case 2:
						tempTime = DateHelper.convertStringToDate(data.getField(RESDET_DATE_COL)+data.getField(RESDET_TIME_COL), DATETIME_FORMAT);
						tempTime = DateHelper.roundTime(tempTime, 60, 0, null);
						posData.setResdetTime(DateHelper.toDatetime(tempTime));
						break;

					case 3:
						tempTime = DateHelper.convertStringToDate(data.getField(RESDET_DATE_COL)+data.getField(RESDET_TIME_COL), DATETIME_FORMAT);
						tempTime = DateHelper.roundTime(tempTime, 30, 0, null);
						posData.setResdetTime(DateHelper.toDatetime(tempTime));
						break;

					default:
						tempTime = DateHelper.convertStringToDate(data.getField(RESDET_DATE_COL)+data.getField(RESDET_TIME_COL), DATETIME_FORMAT);
						tempTime = DateHelper.roundTime(tempTime, 15, 0, null);
						posData.setResdetTime(DateHelper.toDatetime(tempTime));
						break;
				}
			}
			
            String voltypName = data.getField(VOLTYP_COL);
            if (!StringHelper.isEmpty(voltypName)) {
                Object val = volumeTypesCache.get(voltypName.toUpperCase().trim());
                if (val != null) {
                    posData.setVoltypId(((Integer)val));
                }
                else {
                    throw new WBInterfaceException (INVALID_VOLTYP_ERROR_MESSAGE);
                }
            }

			key = posData.getSkdgrpName() + data.getField(RESDET_DATE_COL) + DateHelper.convertDateString(posData.getResdetTime(), "HH:mm:ss") + posData.getVoltypId();
			//look in cache
			currentPosData = (POSData)POSCache.get(key);
			if(currentPosData != null)
			{
				currentPosData.setResdetVolume(currentPosData.getResdetVolume() + posData.getResdetVolume());
			}
			//look in database
			else
			{
				if(loadFromDB(posData, conn))
				{
					insertCount++;
				}
				POSCache.put(key, posData);
			}
			data.setStatus(ImportData.STATUS_APPLIED);
		}
		flushCache(conn, insertCount);
		return importDataList;
	}

	public boolean loadFromDB(POSData posData, DBConnection conn)
		throws SQLException, WBInterfaceException
	{
		String skdgrp_name;
		int skdgrp_id;
		boolean insert = false;
		CodeMapper cm;
		POSMapper pm;
		POSData pd;
		ScheduleGroupData sg;

	    cm = getCodeMapper(conn);
	    pm = getPOSMapper(conn);
	    
	    //determine if its a historic update
	    if(posData.getRecordType() != 2)
	    {
	        skdgrp_name = posData.getSkdgrpName();
	    }
	    else
	    {
	        skdgrp_name = posData.getOldSkdgrpName();
	    }

	    //gets skdgrp_id from code mapper
	    sg = cm.getScheduleGroupByName(skdgrp_name);
	    if(sg != null)
	    {
	        skdgrp_id = sg.getID().intValue();
	    }
	    else if(posData.getRecordType() == 2)
	    {
	        throw new WBInterfaceException(HISTORIC_UPDATE_ERROR_MESSAGE);
	    }
	    else
	    {
	        throw new WBInterfaceException(INVALID_DATA_ERROR_MESSAGE);
	    }

		int voltypId = posData.getVoltypId() == null ? POSMapper.NULL_VOLTYP_ID : posData.getVoltypId().intValue();
		pd = pm.loadBySkdgrpDateTimeVolTyp(skdgrp_id, 
				posData.getResdetDate(), posData.getResdetTime(), voltypId);

	    if(pd != null)
	    {
	        posData.setResdetId(pd.getResdetId());
	        posData.setSkdgrpId(skdgrp_id);
	        if(posData.getRecordType() == 2)
	        {
	            posData.setResdetVolume(posData.getResdetVolume());
	        }
	        else
	        {
	            posData.setResdetVolume(pd.getResdetVolume() + posData.getResdetVolume());
	            posData.setRecordType(1);
	        }
	    }
	    else if(posData.getRecordType() == 2)
	    {
	        throw new WBInterfaceException(HISTORIC_UPDATE_ERROR_MESSAGE);
	    }
	    else
	    {
	        insert = true;
	    }

		return insert;
	}

	public void flushCache(DBConnection conn, int insertCount)
		throws Exception
	{
		//update db
		POSData pd;
		POSData pdOld;
		RecordAccess ra;
		CodeMapper cm;
		POSMapper pm;
		ScheduleGroupData sg;
		int skdgrp_id;
		String[] keyFields = new String[2];
		keyFields[0] = "SKDGRP_ID";
		keyFields[1] = "RESDET_VOLUME";
		ArrayList inserts = new ArrayList();
		ArrayList updates = new ArrayList();
		SOResultsDetailData rdd;
		Iterator it;
		int seqNum = 0;

		try
		{
			it = POSCache.values().iterator();
			ra = new RecordAccess(conn);
			cm = getCodeMapper(conn);
			pm = getPOSMapper(conn);
			
			if(insertCount > 0)
			{
				seqNum = conn.getDBSequence("SEQ_RESDET_ID").getNextValue(insertCount);
			}

			while(it.hasNext())
			{
				rdd = new SOResultsDetailData();
				pd = (POSData)it.next();
				switch (pd.getRecordType())
				{
					//insert
					case 0:
						sg = cm.getScheduleGroupByName(pd.getSkdgrpName());
						if(sg != null)
						{
							rdd.setResdetId(seqNum++);
							rdd.setSkdgrpId((sg.getID()).intValue());
							rdd.setInvtypId(pd.getInvtypId());
							rdd.setResdetDate(pd.getResdetDate());
							rdd.setResdetTime(pd.getResdetTime());
							rdd.setResdetVolume(pd.getResdetVolume());
							rdd.setVoltypId(pd.getVoltypId());
							inserts.add(rdd);
						}
						else
						{
							throw new WBInterfaceException(INVALID_DATA_ERROR_MESSAGE);
						}
						break;
					//update
					case 1:
						rdd.setResdetId(pd.getResdetId());
						rdd.setSkdgrpId(pd.getSkdgrpId());
						rdd.setInvtypId(pd.getInvtypId());
						rdd.setResdetDate(pd.getResdetDate());
						rdd.setResdetTime(pd.getResdetTime());
						rdd.setResdetVolume(pd.getResdetVolume());
						rdd.setVoltypId(pd.getVoltypId());
						updates.add(rdd);
						break;
					//update old
					case 2:
						//remove previous record if exist
						sg = cm.getScheduleGroupByName(pd.getSkdgrpName());
						if(sg != null)
						{
							skdgrp_id = sg.getID().intValue();
						}
						else
						{
							throw new WBInterfaceException(INVALID_DATA_ERROR_MESSAGE);
						}
						if(!pd.getSkdgrpName().equals(pd.getOldSkdgrpName()))
						{
							
							int voltypId = pd.getVoltypId() == null ? POSMapper.NULL_VOLTYP_ID : pd.getVoltypId().intValue();
							pdOld = pm.loadBySkdgrpDateTimeVolTyp(skdgrp_id, 
									pd.getResdetDate(), pd.getResdetTime(), voltypId);

							if(pdOld != null)
							{
								ra.deleteRecordData(TABLE_NAME, ID_FIELD_NAME, pdOld.getResdetId());
							}
						}
						//historic update
						rdd.setResdetId(pd.getResdetId());
						rdd.setSkdgrpId(skdgrp_id);
						rdd.setInvtypId(pd.getInvtypId());
						rdd.setResdetDate(pd.getResdetDate());
						rdd.setResdetTime(pd.getResdetTime());
						rdd.setResdetVolume(pd.getResdetVolume());
						rdd.setVoltypId(pd.getVoltypId());
						updates.add(rdd);
						break;
				}

			}
			ra.insertRecordData(inserts, TABLE_NAME);
			ra.updateRecordData(updates, TABLE_NAME, ID_FIELD_NAME);
		}
		catch (Exception e)
		{
		    if(conn != null)
		    {
		        conn.rollback();
		    }
		    throw e;
		}

		POSCache.clear();
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

	public POSMapper getPOSMapper(DBConnection conn)
	throws SQLException
{
	if(posMapper == null)
	{
		posMapper = new POSMapper(conn);
	}
	return posMapper;
}


	public String getStatus()
	{
		if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
		{
			logger.debug("POSImportTransaction.getStatus() returns " + status);
		}
		return status;
	}

	public String getMessage()
	{
		if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
		{
			logger.debug("POSImportTransaction.getMessage");
		}
		return message;
	}

	public String getTaskUI()
	{
		return "/jobs/wbinterface/pos/posParams.jsp";
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

	public int getIsAggregate(HashMap params) {
		int type = params!= null && params.containsKey(PARAM_AGGREGATE)
				? params.get(PARAM_AGGREGATE) == null
						  ? DEFAULT_AGGREGATE :
						  Integer.parseInt( (String) params.get(PARAM_AGGREGATE))
				: 1;
		return type;
	}

    private boolean loadVolumeTypes(DBConnection conn) throws Exception{
        volumeTypesCache = new HashMap();
        List volTypes = new VolumeTypeAccess(conn).loadRecordData(
            new VolumeType(), "SO_VOLUME_TYPE", "1=1" );
        Iterator iter = volTypes.iterator();
        while (iter.hasNext()) {
            VolumeType item = (VolumeType)iter.next();
            volumeTypesCache.put(item.getVoltypName().trim().toUpperCase() , item.getVoltypId() );
        }
        return true;
    }

}
