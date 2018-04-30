package com.wbiag.app.wbinterface.pos;

import java.sql.Date;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.*;

/** Title:         POSMapper
 * Description:    Caches POS Data from table SO_RESULTS_DETAIL.  Intended for long processes where
 *                 a lot of requests are performed for POS data on the same date and location.
 * 
 *                 Workbrain 4.1 - When a query is executed by skdgrp_id, resdet_date, and 
 *                 resdet_time - all of theresults are returned for the date. 
 *                 
 *                 Workbrain 5.0 - Same as 4.1, but with VOLTYP_ID
 * Copyright:      Copyright (c) 2006
 * Company:        Workbrain Inc
 * @author         Brian Chan
 * @version 1.0
 */

public class POSMapper {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(POSMapper.class);

	private DBConnection conn = null;
	
	private Map posMap = null;
	
	private final static String DATE_FORMAT = "MM/dd/yyyy";
	private final static String TIME_FORMAT = "HH:mm:ss";
	private final static String KEY_DELIM = "~";
	public final static int NULL_VOLTYP_ID = -9999;

	// for workbrain 4.1 only
	private String QUERY_BY_SKDGRP_ID_AND_DATE = "SELECT RESDET_ID, RESDET_TIME, RESDET_VOLUME, INVTYP_ID FROM SO_RESULTS_DETAIL WHERE SKDGRP_ID = ? AND RESDET_DATE = ?";

	// for workbrain 5.0 only
	private String QUERY_BY_SKDGRP_ID_AND_DATE_AND_VOLTYP_ID = "SELECT RESDET_ID, RESDET_TIME, RESDET_VOLUME, VOLTYP_ID, INVTYP_ID FROM SO_RESULTS_DETAIL WHERE SKDGRP_ID = ? AND RESDET_DATE = ? AND VOLTYP_ID = ?";
	private String QUERY_BY_SKDGRP_ID_AND_DATE_AND_NULL_VOLTYP = "SELECT RESDET_ID, RESDET_TIME, RESDET_VOLUME, VOLTYP_ID, INVTYP_ID FROM SO_RESULTS_DETAIL WHERE SKDGRP_ID = ? AND RESDET_DATE = ? AND VOLTYP_ID IS NULL";
	
	/**
	 * @deprecated use loadBySkdgrpDateTimeVolTyp or loadBySkdgrpDateTimeNullVoltyp for WB5.0 and up
	 */
	public POSData loadBySkdgrpDateTime(int skdgrpId, Datetime date, Datetime time) throws SQLException {
		String key = buildKey(skdgrpId, date, time);
		POSData pd = getPOSDataByKey(key);
		if (pd != null) return pd;
		
		// posData does not exist or has not been loaded yet
		loadPOSDataBySkdgrpIdDate(skdgrpId, date);
		return getPOSDataByKey(key);
	}
	
	/**
	 * Loads POS Data by Location, Date, Time and Volume Type.  Use only for WB 5.0 and above
	 * @param skdgrpId
	 * @param date
	 * @param time
	 * @param voltypId
	 * @return
	 * @throws SQLException
	 */
	public POSData loadBySkdgrpDateTimeVolTyp(int skdgrpId, Datetime date, Datetime time, int voltypId) throws SQLException {
		String key = buildKeyWithVolTyp(skdgrpId, date, time, voltypId);
		POSData pd = getPOSDataByKey(key);
		if (pd != null) return pd;
		
		// posData does not exist or has not been loaded yet
		loadPOSDataBySkdgrpIdDateVoltypId(skdgrpId, date, voltypId);
		return getPOSDataByKey(key);
	}
	
	/**
	 * Loads POS Data by Location, Date, Time with null Volume Type.  Use only for WB 5.0 and above
	 * @param skdgrpId
	 * @param date
	 * @param time
	 * @return
	 * @throws SQLException
	 */
	public POSData loadBySkdgrpDateTimeNullVoltyp(int skdgrpId, Datetime date, Datetime time) throws SQLException {
		return loadBySkdgrpDateTimeVolTyp(skdgrpId, date, time, NULL_VOLTYP_ID);
	}
	
	/**
	 * Loads all the pos data by schedule group and date, than adds it to the posMap
	 * @param skdgrpId
	 * @param date
	 * @throws SQLException
	 */
	private void loadPOSDataBySkdgrpIdDate(int skdgrpId, Datetime date) throws SQLException {
		POSData posData = null;
		Datetime posTime = null;
		
		PreparedStatement ps = null;
		ResultSet rs = null;
				
		try {
			ps = conn.prepareStatement(QUERY_BY_SKDGRP_ID_AND_DATE);
			ps.clearParameters();
			ps.setInt(1, skdgrpId);
			ps.setTimestamp(2, new Timestamp(date.getTime()));
			
			rs = ps.executeQuery();
			
			while (rs.next()) {
				posData = new POSData();
				
				posData.setResdetId(rs.getInt("RESDET_ID"));
				posData.setSkdgrpId(skdgrpId);
				posData.setResdetDate(date);
				posTime = new Datetime(rs.getTimestamp("RESDET_TIME").getTime());
				posData.setResdetTime(posTime);
				posData.setResdetVolume(rs.getFloat("RESDET_VOLUME"));
				
				getPosMap().put(buildKey(skdgrpId, date, posTime), posData);
			}
			
		} finally {
			SQLHelper.cleanUp(ps, rs);
		}
		
	}

	
	/**
	 * Loads all the pos data by schedule group and date, than adds it to the posMap
	 * @param skdgrpId
	 * @param date
	 * @throws SQLException
	 */
	private void loadPOSDataBySkdgrpIdDateVoltypId(int skdgrpId, Datetime date, int voltypId) throws SQLException {
		POSData posData = null;
		Datetime posTime = null;
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = NULL_VOLTYP_ID == voltypId ? 
				QUERY_BY_SKDGRP_ID_AND_DATE_AND_NULL_VOLTYP :
				QUERY_BY_SKDGRP_ID_AND_DATE_AND_VOLTYP_ID;

		try {
			ps = conn.prepareStatement(sql);
			ps.clearParameters();
			ps.setInt(1, skdgrpId);
			ps.setTimestamp(2, new Timestamp(date.getTime()));
			if (NULL_VOLTYP_ID != voltypId) ps.setInt(3, voltypId);

			rs = ps.executeQuery();
			
			while (rs.next()) {
				posData = new POSData();
				
				posData.setResdetId(rs.getInt("RESDET_ID"));
				posData.setSkdgrpId(skdgrpId);
				posData.setResdetDate(date);
				posTime = new Datetime(rs.getTimestamp("RESDET_TIME").getTime());
				posData.setResdetTime(posTime);				
				posData.setResdetVolume(rs.getFloat("RESDET_VOLUME"));
				if (NULL_VOLTYP_ID != voltypId) {
					posData.setVoltypId(new Integer(rs.getInt("VOLTYP_ID")));
				}
				
				getPosMap().put(buildKeyWithVolTyp(skdgrpId, date, posTime, voltypId), posData);
			}
			
		} finally {
			SQLHelper.cleanUp(ps, rs);
		}
		
	}
	
	/**
	 * Builds the key for the Hashmap, to be in the format skdgrpId~MM/dd/yyyyHH:mm:ss
	 * @param skdgrpId
	 * @param date
	 * @param time
	 * @return
	 */
	private String buildKey(int skdgrpId, Datetime date, Datetime time) {
		return skdgrpId + KEY_DELIM 
			+ DateHelper.convertDateString(date, DATE_FORMAT) 
			+ DateHelper.convertDateString(time, TIME_FORMAT);
	}
	
	/**
	 * Builds the key for the Hashmap, to be in the format skdgrpId~MM/dd/yyyyHH:mm:ss~voltypId
	 * @param skdgrpId
	 * @param date
	 * @param time
	 * @param voltypId
	 * @param nullVoltyp
	 * @return
	 */
	private String buildKeyWithVolTyp(int skdgrpId, Datetime date, Datetime time, int voltypId) {
		return buildKey(skdgrpId, date, time)
			+ KEY_DELIM + (NULL_VOLTYP_ID == voltypId? "null" : String.valueOf(voltypId));
	}
	
	private POSData getPOSDataByKey(String key) {
		Object o = getPosMap().get(key);
		if (o != null) return (POSData)o;
		return null;
	}
	
	private Map getPosMap() {
		if (posMap == null) posMap = new HashMap();
		return posMap;
	}
	
	public void clear() {
		getPosMap().clear();
	}
	
	public POSMapper(DBConnection conn) {
		this.conn = conn;
	}
	
}
