package com.wbiag.app.modules.retailSchedule.services.volumeBestFit.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.server.cache.WorkbrainCache;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;

public class ClientStfDefCache extends WorkbrainCache {

	private static final Logger logger = Logger
			.getLogger(ClientStfDefCache.class);

	public static final String CLIENT_STF_DEF = "CLIENT_STF_DEF";

	public static final String CACHE_NAME = "ClientStfDefCache";

	public static final int ACCESS_INTERVAL = 10000;

	private static final ClientStfDefCache instance = new ClientStfDefCache(
			CACHE_NAME);

	public static final ClientStfDefCache getInstance() {
		instance.setAccessInterval(ACCESS_INTERVAL);
		return instance;
	}

	private ClientStfDefCache(String name) {
		super(name);
	}

	protected void updateCacheContents(Connection c) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Reloading " + CLIENT_STF_DEF + " data");
		}

		put(CLIENT_STF_DEF, new HashMap());

	}

	public Vector getClientStfDefId(int skdgrp_id, int job_id, int skill_id,
			int act_id, DBConnection conn) throws RetailException {
		String key = new StringBuffer().append(skdgrp_id).append("_").append(
				job_id).append("_").append(skill_id).append("_").append(act_id)
				.toString();
		
		String keySkdgrpId = new StringBuffer().append(skdgrp_id).toString();

		HashMap clientStfDefIds = (HashMap) get(CLIENT_STF_DEF, conn);
		Vector ret = null;

		if (clientStfDefIds.size() == 0 || !clientStfDefIds.containsKey(keySkdgrpId)) {
			loadClientStfDefId(skdgrp_id, conn);
		} 
		if (clientStfDefIds.containsKey(keySkdgrpId)) {
			HashMap skdgrpClientStfDefIds = (HashMap) clientStfDefIds.get(keySkdgrpId);
			if (skdgrpClientStfDefIds.containsKey(key)){
				ret = (Vector) skdgrpClientStfDefIds.get(key);
			} else { 
				ret = new Vector();
			}
		}
	

		return ret;

	}

	private void loadClientStfDefId(int skdgrp_id, DBConnection conn) throws RetailException {
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = " SELECT a.csd_id, a.job_id, a.stskl_id, a.act_id FROM so_client_stfdef a, wbiag_volume_bestfit b "
				+ "WHERE a.csd_id = b.csd_id "
				+ "   AND b.driver_skdgrp_id = ? ";
		
		HashMap skdgrpStfDefId = (HashMap) get(CLIENT_STF_DEF, conn);
		HashMap clientStfDefIds =  new HashMap();

		try {
			ps = conn.prepareStatement(sql);
			ps.setInt(1, skdgrp_id);

			rs = ps.executeQuery();

			String key;
			while (rs.next()) {
				Vector result;
				key = new StringBuffer().append(skdgrp_id).append("_").append(
						rs.getInt("job_id")).append("_").append(rs.getInt("stskl_id")).append("_").append(rs.getInt("act_id"))
						.toString();
				if (clientStfDefIds.containsKey(key)){
					result = (Vector) clientStfDefIds.get(key);
				} else {
					result = new Vector();
				}
				result.add(new Integer(rs.getInt("csd_id")));
				
				clientStfDefIds.put(key, result);
			}

		} catch (SQLException sqle) {
			throw new RetailException(sqle);
		} finally {
			SQLHelper.cleanUp(ps);
			SQLHelper.cleanUp(rs);
		}
		
		skdgrpStfDefId.put(String.valueOf(skdgrp_id), clientStfDefIds );
	}

}
