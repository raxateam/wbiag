package com.wbiag.app.modules.retailSchedule.services.volumeBestFit.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.server.cache.WorkbrainCache;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;

import com.wbiag.app.modules.retailSchedule.services.volumeBestFit.*;

public class BestFitWindowCache extends WorkbrainCache {

	private static final Logger logger = Logger
			.getLogger(BestFitWindowCache.class);

	public static final String BF_WINDOW_BY_DAY = "BF_WINDOW_BY_DAY";

	public static final String CACHE_NAME = "BestFitWindowCache";

	public static final int ACCESS_INTERVAL = 10000;

	private static final BestFitWindowCache instance = new BestFitWindowCache(
			CACHE_NAME);

	public static final BestFitWindowCache getInstance() {
		instance.setAccessInterval(ACCESS_INTERVAL);
		return instance;
	}

	private BestFitWindowCache(String name) {
		super(name);
	}

	protected void updateCacheContents(Connection c) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Reloading " + BF_WINDOW_BY_DAY + " data");
		}

		put(BF_WINDOW_BY_DAY, new HashMap());

	}

	public List getBFWindowByDay(int csd_id, int driverId, int dayIndex, DBConnection conn) throws RetailException {
		String key = new StringBuffer().append(csd_id).append("_").append(
				driverId).append("_").append(dayIndex).toString();
		
		String keyCsdId = String.valueOf(csd_id);

		HashMap bfWindowByCsd = (HashMap) get(BF_WINDOW_BY_DAY, conn);
		List ret = null;

		if (bfWindowByCsd.size() == 0 || !bfWindowByCsd.containsKey(keyCsdId)) {
			loadBFWindowByDay(csd_id, conn);
		} 
		if (bfWindowByCsd.containsKey(keyCsdId)) {
			HashMap bfWindowCsdByDay = (HashMap) bfWindowByCsd.get(keyCsdId);
			if (bfWindowCsdByDay.containsKey(key)){
				ret = (List) bfWindowCsdByDay.get(key);
			} else { 
				ret = new ArrayList();
			}
		}
	

		return ret;

	}

	private void loadBFWindowByDay(int csd_id,  DBConnection conn) throws RetailException {
		
		PreparedStatement ps = null;
		ResultSet rs = null;
    	String sql = " SELECT vbf_start_time, vbf_end_time, vbf_day_index, driver_skdgrp_id FROM wbiag_volume_bestfit " +
		 " WHERE csd_id = ? ";   
		
		HashMap bfWindowByCsd = (HashMap) get(BF_WINDOW_BY_DAY, conn);
		HashMap bfWindowCsdByDay =  new HashMap();

		try {
			ps = conn.prepareStatement(sql);
			ps.setInt(1, csd_id);

			rs = ps.executeQuery();

			String key;
			while (rs.next()) {
				List result;
				key = new StringBuffer().append(csd_id).append("_").append(
						rs.getInt("driver_skdgrp_id")).append("_").append(rs.getInt("vbf_day_index")).toString();
				if (bfWindowCsdByDay.containsKey(key)){
					result = (List) bfWindowCsdByDay.get(key);
				} else {
					result = new ArrayList();
				}
				Map curResult = new HashMap();
                Date start = rs.getTime("vbf_start_time");
                Date end = rs.getTime("vbf_end_time");
                
                curResult.put(BestFitWindowUtil.WINDOW_START, start);
                curResult.put(BestFitWindowUtil.WINDOW_END, end);
                
                result.add(curResult);

				
				bfWindowCsdByDay.put(key, result);
			}

		} catch (SQLException sqle) {
			throw new RetailException(sqle);
		} finally {
			SQLHelper.cleanUp(ps);
			SQLHelper.cleanUp(rs);
		}
		
		bfWindowByCsd.put(String.valueOf(csd_id), bfWindowCsdByDay );
	}

}
