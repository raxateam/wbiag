package com.wbiag.app.modules.retailSchedule.util;

import java.util.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.wbiag.app.modules.retailSchedule.model.ForecastGroupExtTypeData;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;

/**
 * @author bchan
 * 
 * Caches forecast values for given forecast group types and the dates between
 * soFromDate and soToDate.  The method loadFcastByListNames must be called 
 * otherwise a NullPointer will be thrown (from fcastMap)
 * 
 * Change History:
 *
 */
public class FCastGrpExtMapper {
	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FCastGrpExtMapper.class);
	DBConnection conn = null;

	private Map fcastMap = null;  // Map of forecast group types, value is map of dates
	private Date soFromDate = null;  // date range of modified forecast
	private Date soToDate = null;
	
	public FCastGrpExtMapper(DBConnection conn) {
		this.conn = conn;
	}
	
	public static FCastGrpExtMapper createFCastGrpExtMapper(DBConnection conn) {
		return new FCastGrpExtMapper(conn);
	}
	
	public ForecastGroupExtTypeData getForecastGroupExtTypeByFcastgrptypName(int fcastgrptypId) {
		return null;
	}
	
	/**
	 * Using fcastMap, retrieve the Map of dates using fcastgrptypName as the
	 * key.  Then retrieve the forecast using the date as the key.  If Map of 
	 * dates or forecast is not found, then 0 is returned.
	 * 
	 * A NullPointer exception is thrown if fcastMap is null, likely because 
	 * loadFcastByListNames is not called.
	 * @param fcastgrptypName
	 * @param date
	 * @return
	 */
	public double getFcast(String fcastgrptypName, Date date) {
		Object o = fcastMap.get(fcastgrptypName);
		if (o == null) return 0;
		
		Map map = (Map)o;
		o = map.get(date);
		return o == null ? 0 : ((Double)o).doubleValue(); 
	}
	
	/**
	 * Given an iterator of names of fcast group types, calculate all the forecasts
	 * for the given date range. Can be called multiple times make additions to cache.
	 * 
	 * The skdGrpId is intended to a department, any location that is not below the 
	 * given departments parent will be filtered out.
	 * 
	 * This method must be called to load all the values, before any forecasts are
	 * retrieved.
	 * 
	 * @param iter
	 */
	public void loadFcast(int skdGrpId, Iterator iter) throws SQLException, RetailException {
		if (fcastMap == null) fcastMap = new HashMap();

		Map fcastDateMap = null;
		String name = null;
		double fcastVal = 0;
		Date date = null;

		PreparedStatement ps = null;
		ResultSet rs = null;

		StringBuffer sb = new StringBuffer();
		while (iter.hasNext()) {
			sb.append((sb.length() == 0 ? "'" : ",'") + iter.next() + "'");
		}

        String sql = "SELECT A.FCASTGRPTYP_NAME, E.FCAST_DATE, SUM(E.FCAST_VALUE) AS FCAST_VALUE "
			+ " FROM WBIAG_FCAST_GROUP_TYPE A, WBIAG_FCAST_GROUP_EXT B, SO_FCAST_GROUP C, SO_SCHEDULE_GROUP D, VIEW_WBIAG_SO_FCAST E"
			+ " WHERE A.FCASTGRPTYP_ID = B.FCASTGRPTYP_ID"
			+ " AND B.FCASTGRP_ID = C.FCASTGRP_ID"
			+ " AND C.FCASTGRP_ID = D.FCASTGRP_ID"
			+ " AND D.SKDGRP_ID = E.SKDGRP_ID"
			+ " AND C.VOLTYP_ID = D.VOLTYP_ID"
			+ " AND D.SKDGRP_INTRNL_TYPE = 12" // departments only
			+ " AND D.SKDGRP_ID IN ("
			+ "      SELECT SKDGRP_ID"
			+ "      FROM SO_SCHEDULE_GROUP, WORKBRAIN_TEAM"
			+ "      WHERE WORKBRAIN_TEAM.WBT_ID = SO_SCHEDULE_GROUP.WBT_ID"
			+ "      AND EXISTS"
			+ "      (SELECT CHILD_WBT_ID FROM SEC_WB_TEAM_CHILD_PARENT"
			+ "       WHERE PARENT_WBT_ID = (SELECT WBT_ID FROM SO_SCHEDULE_GROUP"
			+ "                              WHERE SKDGRP_ID = ?)"
			+ "       AND SEC_WB_TEAM_CHILD_PARENT.CHILD_WBT_ID = SO_SCHEDULE_GROUP.WBT_ID)"
			+ " )"
			+ " AND E.FCAST_TO_DATE >= ?"
			+ " AND E.FCAST_FROM_DATE <= ?"
			+ " AND E.FCAST_DATE BETWEEN ? AND ?"
			+ " AND A.FCASTGRPTYP_NAME IN (" + sb + ")"
			+ " GROUP BY FCASTGRPTYP_NAME, FCAST_DATE"
			+ " ORDER BY FCAST_DATE";

		try {
			java.sql.Date sqlFrom = null;
			java.sql.Date sqlTo = null;

			try {
				sqlFrom = new java.sql.Date(soFromDate.getTime());
				sqlTo = new java.sql.Date(soToDate.getTime());
			} catch (NullPointerException npe) {
				throw new RetailException("Both soFromDate and soToDate must be set", npe);
			}

			ps = conn.prepareStatement(sql);
			
			ps.clearParameters();
			ps.setInt(1, skdGrpId);
			ps.setDate(2, sqlFrom);
			ps.setDate(3, sqlTo);
			ps.setDate(4, sqlFrom);
			ps.setDate(5, sqlTo);
			
			rs = ps.executeQuery();
			
			Object o = null;
			boolean newMap = false;
			
			while (rs.next()) {
				name = rs.getString("FCASTGRPTYP_NAME");
				date = rs.getDate("FCAST_DATE");
				fcastVal = rs.getDouble("FCAST_VALUE");
		        if (logger.isDebugEnabled()) {
					logger.debug("loadFcastByListNames: adding name = " + name 
		        			+ ", date = " + date + ", fcastVal = " + fcastVal);
		        }

				o = fcastMap.get(name);
				newMap = (o == null);
				fcastDateMap = newMap ? new HashMap() : (Map)o; 
				
				fcastDateMap.put(date, new Double(fcastVal));
				if (newMap) fcastMap.put(name, fcastDateMap);
			}
			
		} finally {
			SQLHelper.cleanUp(ps, rs);
		}
	}
	
	/**
	 * @return Returns the soFromDate.
	 */
	public Date getSoFromDate() {
		return soFromDate;
	}

	/**
	 * @param soFromDate The soFromDate to set.
	 */
	public void setSoFromDate(Date soFromDate) {
		this.soFromDate = soFromDate;
	}

	/**
	 * @return Returns the soToDate.
	 */
	public Date getSoToDate() {
		return soToDate;
	}

	/**
	 * @param soToDate The soToDate to set.
	 */
	public void setSoToDate(Date soToDate) {
		this.soToDate = soToDate;
	}
	
	
	
}
