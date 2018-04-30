package com.wbiag.app.modules.retailSchedule.util;

import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.StringHelper;
import com.wbiag.app.modules.retailSchedule.db.ForecastGroupExtTypeAccess;
import com.wbiag.app.modules.retailSchedule.model.ForecastGroupExtTypeData;
import java.util.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.utils.ISODataCache;
import com.workbrain.app.modules.retailSchedule.utils.SODataCache;
import com.workbrain.server.registry.Registry;
import javax.naming.NamingException;


/**
 * @author bchan
 *
 */
public class FCastGrpExtHelper {
	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FCastGrpExtHelper.class);

	private DBConnection conn = null;
	private Set validTypes = null;
	
	private String WBIAG_FCAST_EXT_PRECISION = "system/modules/scheduleOptimization/WBIAG_FCAST_EXT_PRECISION";

	/*
	 * constructor
	 */
	public FCastGrpExtHelper(DBConnection conn) {
		this.conn = conn;
	}	
	
	/**
	 * Updates all the forecasts that are part of the given Forecast Group Type by 
	 * date.  All forecasts are updated by using multiplying the current value by
	 * the given ratio.
	 * @param fcastgrptypName
	 * @param date
	 * @param ratio
	 * @return
	 */
	public int updateFCastDeptsByMultiple(int skdgrpId, String fcastgrptypName, Date date, double multiple, boolean allVolumeTypes) throws SQLException {
        if (logger.isDebugEnabled()) {
        	logger.debug("updateFCastDeptsByRatio: skdgrpId = " + skdgrpId
					+ ", fcastgrptypName = " + fcastgrptypName + ", date = "
					+ date + ", multiple = " + multiple);
		}

		String sql = "UPDATE SO_FCAST_DETAIL"
			+ " SET FCAST_ADJTYP = 2," 
			+ " FCAST_ADJVAL = ? * DECODE(FCAST_ADJTYP, 1, FCAST_CALLS + (FCAST_CALLS * FCAST_ADJVAL * 0.01), FCAST_CALLS + FCAST_ADJVAL) - FCAST_CALLS"
			+ " WHERE FCASTDET_ID IN "
			+ "      (SELECT SO_FCAST_DETAIL.FCASTDET_ID"
			+ "       FROM SO_SCHEDULE_GROUP, WORKBRAIN_TEAM, SO_FCAST, SO_FCAST_GROUP, SO_FCAST_DETAIL, WBIAG_FCAST_GROUP_EXT, WBIAG_FCAST_GROUP_TYPE"
			+ "       WHERE WORKBRAIN_TEAM.WBT_ID = SO_SCHEDULE_GROUP.WBT_ID "
			+ "       AND SO_SCHEDULE_GROUP.SKDGRP_INTRNL_TYPE = 12" // departments only
			+ "       AND EXISTS"
			+ "            (SELECT CHILD_WBT_ID FROM SEC_WB_TEAM_CHILD_PARENT "
			+ "             WHERE PARENT_WBT_ID = (SELECT WBT_ID FROM SO_SCHEDULE_GROUP "
			+ "       	                           WHERE SKDGRP_ID = ?)"
			+ "       	    AND SEC_WB_TEAM_CHILD_PARENT.CHILD_WBT_ID  = SO_SCHEDULE_GROUP.WBT_ID) "
			+ "       AND SO_FCAST.SKDGRP_ID = SO_SCHEDULE_GROUP.SKDGRP_ID"
			+ "       AND ? BETWEEN SO_FCAST.FCAST_FROM_DATE AND SO_FCAST.FCAST_TO_DATE"
			+ "       AND SO_FCAST.FCAST_ID = SO_FCAST_DETAIL.FCAST_ID"
			+ "       AND SO_FCAST_DETAIL.FCAST_DATE = ?"
			+ "       AND SO_SCHEDULE_GROUP.FCASTGRP_ID = SO_FCAST_GROUP.FCASTGRP_ID"
			+ "       AND SO_FCAST_GROUP.FCASTGRP_ID = WBIAG_FCAST_GROUP_EXT.FCASTGRP_ID"
			+ "" + (allVolumeTypes ? "" : "       AND SO_SCHEDULE_GROUP.VOLTYP_ID = SO_FCAST_GROUP.VOLTYP_ID")
			+ "       AND WBIAG_FCAST_GROUP_EXT.FCASTGRPTYP_ID = WBIAG_FCAST_GROUP_TYPE.FCASTGRPTYP_ID"
			+ "       AND WBIAG_FCAST_GROUP_TYPE.FCASTGRPTYP_NAME = ?" 
			+ "      )";
		
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = conn.prepareStatement(sql);
			ps.clearParameters();
			
			java.sql.Date sqlDate = new java.sql.Date(date.getTime());
			ps.setDouble(1, multiple);
			ps.setInt(2, skdgrpId);
			ps.setDate(3, sqlDate);
			ps.setDate(4, sqlDate);
			ps.setString(5, fcastgrptypName);
			
			return ps.executeUpdate();
			
		} finally {
			SQLHelper.cleanUp(ps, rs);
		}
		
	}	
	
	/**
	 * Updates all the forecasts that are part of the given Forecast Group Type by 
	 * date.  All forecasts are updated by using adding the delta value
	 * @param fcastgrptypName
	 * @param date
	 * @param value
	 * @return
	 */
	public int updateFCastDeptsByValue(int skdgrpId, String fcastgrptypName, Date date, double value, boolean allVolumeTypes) throws SQLException {
        if (logger.isDebugEnabled()) {
        	logger.debug("updateFCastDeptsByValue: skdgrpId = " + skdgrpId
					+ ", fcastgrptypName = " + fcastgrptypName + ", date = "
					+ date + ", value = " + value);
		}

		String sql = "UPDATE SO_FCAST_DETAIL"
			+ " SET FCAST_ADJTYP = 2," 
			+ " FCAST_ADJVAL = ? - FCAST_CALLS"
			+ " WHERE FCASTDET_ID IN "
			+ "      (SELECT SO_FCAST_DETAIL.FCASTDET_ID"
			+ "       FROM SO_SCHEDULE_GROUP, WORKBRAIN_TEAM, SO_FCAST, SO_FCAST_GROUP, SO_FCAST_DETAIL, WBIAG_FCAST_GROUP_EXT, WBIAG_FCAST_GROUP_TYPE"
			+ "       WHERE WORKBRAIN_TEAM.WBT_ID = SO_SCHEDULE_GROUP.WBT_ID "
			+ "       AND SO_SCHEDULE_GROUP.SKDGRP_INTRNL_TYPE = 12" // departments only
			+ "       AND EXISTS"
			+ "            (SELECT CHILD_WBT_ID FROM SEC_WB_TEAM_CHILD_PARENT "
			+ "             WHERE PARENT_WBT_ID = (SELECT WBT_ID FROM SO_SCHEDULE_GROUP "
			+ "       	                           WHERE SKDGRP_ID = ?)"
			+ "       	    AND SEC_WB_TEAM_CHILD_PARENT.CHILD_WBT_ID  = SO_SCHEDULE_GROUP.WBT_ID) "
			+ "       AND SO_FCAST.SKDGRP_ID = SO_SCHEDULE_GROUP.SKDGRP_ID"
			+ "       AND ? BETWEEN SO_FCAST.FCAST_FROM_DATE AND SO_FCAST.FCAST_TO_DATE"
			+ "       AND SO_FCAST.FCAST_ID = SO_FCAST_DETAIL.FCAST_ID"
			+ "       AND SO_FCAST_DETAIL.FCAST_DATE = ?"
			+ "       AND SO_SCHEDULE_GROUP.FCASTGRP_ID = SO_FCAST_GROUP.FCASTGRP_ID"
			+ "       AND SO_FCAST_GROUP.FCASTGRP_ID = WBIAG_FCAST_GROUP_EXT.FCASTGRP_ID"
			+ (allVolumeTypes ? "" : "       AND SO_SCHEDULE_GROUP.VOLTYP_ID = SO_FCAST_GROUP.VOLTYP_ID")
			+ "       AND WBIAG_FCAST_GROUP_EXT.FCASTGRPTYP_ID = WBIAG_FCAST_GROUP_TYPE.FCASTGRPTYP_ID"
			+ "       AND WBIAG_FCAST_GROUP_TYPE.FCASTGRPTYP_NAME = ?" 
			+ "      )";
		
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = conn.prepareStatement(sql);
			ps.clearParameters();
			
			java.sql.Date sqlDate = new java.sql.Date(date.getTime());
			ps.setDouble(1, value);
			ps.setInt(2, skdgrpId);
			ps.setDate(3, sqlDate);
			ps.setDate(4, sqlDate);
			ps.setString(5, fcastgrptypName);
			
			return ps.executeUpdate();
			
		} finally {
			SQLHelper.cleanUp(ps, rs);
		}
		
		/**
		 * TODO: do inserts for departments that do not have forecasts yet??
		 */
	}	
	
	/**
	 * Returns a set of all available Forecast Group Types
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public Set getFCastGrpTypeNames() {
		Set set = new HashSet();
        ISODataCache cache = SODataCache.getInstance();
        
        List list = (List)cache.getData(List.class, "ForecastGroupExtTypeData.loadAll");
        if (list == null) {
    		ForecastGroupExtTypeAccess fa = new ForecastGroupExtTypeAccess(conn);
    		list = fa.loadAll();
            cache.addData(List.class, "ForecastGroupExtTypeData.loadAll", list);
        }

		ForecastGroupExtTypeData fd = null;
		for (int i = 0; i < list.size(); i ++) {
			fd = (ForecastGroupExtTypeData)list.get(i);
			set.add(fd.getFcastgrptypName());
		}
		
		return set;
	}
	
	/**
	 * Given the formula value for a extended forecast group type, parse the 
	 * formula and return as a List of FormulaTokens.  For example, "A + B - C"  
	 * is the list [A, +1], [B, +1], [C, -1].  Furthermore, A, B and C must be existing
	 * extended forecast group type otherwise an CalloutException is thrown.
	 * @param formula
	 * @return List of FormulaToken.  If formula is null, null is returned
	 * @throws CalloutException
	 */
	public List getCalcList(String formula) throws CalloutException {
		if (StringHelper.isEmpty(formula)) return null;
		if (validTypes == null) validTypes = this.getFCastGrpTypeNames();
		
		String s = StringHelper.searchReplace(formula, "+", "|+|");
		s = StringHelper.searchReplace(s, "-", "|-|");

		String[] tokens = StringHelper.detokenizeString(s, "|", true);
		
		// length must be odd number
		if (tokens.length % 2 != 1) {
			throw new CalloutException("Invalid formula: " + formula);
		}
		
		// check each token is valid
		for (int i = 0; i < tokens.length; i ++) {
			if (i % 2 == 0) {
				// Check for valid Extended Forecast Types
				if (!validTypes.contains(tokens[i])) {
					throw new CalloutException("Invalid FCastType: " + tokens[i]);
				}
			
			} else {
				// Check for valid Operands (+/-)
				if (!"+".equals(tokens[i]) && !"-".equals(tokens[i])) {
					throw new CalloutException("Invalid Operand: " + tokens[i]);
				}
			}
		} // for

		// Formula is valid - convert into calculation list
		List list = new ArrayList();
		FormulaToken ft = null;
		for (int i = 0; i < tokens.length; i ++) {
			if (i == 0) {
				list.add(new FormulaToken(tokens[0], 1));
			} else {
				if (i % 2 == 0) {
					ft.fCastType = tokens[i];
					list.add(ft);
				} else {
					ft = new FormulaToken("+".equals(tokens[i]) ? 1 : -1);
				}
			}
		}
		
		return list;
		
	}

	/**
	 * Recursively retrieves the parent location of the given location until
	 * a store (skdgrp_intrnl_type=10) or parent is reached.
	 * @param skdgrpId
	 * @return
	 */
	public int getParentStoreSkdGrpId(int skdgrpId) throws RetailException {
		String sql = "SELECT PG.SKDGRP_ID, PG.SKDGRP_INTRNL_TYPE"
			+ " FROM WORKBRAIN_TEAM PT INNER JOIN SO_SCHEDULE_GROUP PG ON (PG.WBT_ID = PT.WBT_ID),"
			+ "      WORKBRAIN_TEAM CT INNER JOIN SO_SCHEDULE_GROUP CG ON (CG.WBT_ID = CT.WBT_ID)"
			+ " WHERE CT.WBT_LFT BETWEEN PT.WBT_LFT AND PT.WBT_RGT "
			+ "    AND CT.WBT_RGT BETWEEN PT.WBT_LFT AND PT.WBT_RGT "
			+ "    AND CG.SKDGRP_ID = ?"
			+ " ORDER BY PT.WBT_LEVEL DESC";
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = conn.prepareStatement(sql);
			ps.clearParameters();
			ps.setInt(1, skdgrpId);
			
			rs = ps.executeQuery();
			while (rs.next()) {
				if (rs.getInt("SKDGRP_INTRNL_TYPE") == 10) {
					return rs.getInt("SKDGRP_ID");
				}
			}
			
		} catch (SQLException sqle) {
			throw new RetailException("Error retrieving parent store for skdgrpId " + skdgrpId, sqle);
		} finally {
			SQLHelper.cleanUp(ps, rs);
		}

		return 0;  // default, return parent
	}
	
	/**
	 * Given a (store) location skdgrpId and a forecast group type, return the 
	 * number of departments below its hierarchy
	 * @param skdgrpId
	 * @return
	 */
	public int getNumDeptForStoreAndFCastType(int skdgrpId, String fcastGrpTypName, boolean allVolumeTypes) throws RetailException {
		String sql = " SELECT COUNT(1)" 
			+ " FROM SO_SCHEDULE_GROUP, WORKBRAIN_TEAM, SO_FCAST_GROUP, WBIAG_FCAST_GROUP_EXT, WBIAG_FCAST_GROUP_TYPE"
			+ " WHERE WORKBRAIN_TEAM.WBT_ID = SO_SCHEDULE_GROUP.WBT_ID "
			+ " AND SO_SCHEDULE_GROUP.FCASTGRP_ID = SO_FCAST_GROUP.FCASTGRP_ID"
			+ " AND SO_FCAST_GROUP.FCASTGRP_ID = WBIAG_FCAST_GROUP_EXT.FCASTGRP_ID"
			+ (allVolumeTypes ? "" : " AND SO_SCHEDULE_GROUP.VOLTYP_ID = SO_FCAST_GROUP.VOLTYP_ID")
			+ " AND WBIAG_FCAST_GROUP_EXT.FCASTGRPTYP_ID = WBIAG_FCAST_GROUP_TYPE.FCASTGRPTYP_ID"
			+ " AND WBIAG_FCAST_GROUP_TYPE.FCASTGRPTYP_NAME = ?"
			+ " AND EXISTS"
			+ " (SELECT CHILD_WBT_ID FROM SEC_WB_TEAM_CHILD_PARENT "
			+ "  WHERE PARENT_WBT_ID = (SELECT WBT_ID FROM SO_SCHEDULE_GROUP "
			+ "                         WHERE SKDGRP_ID = ?)"
			+ "  AND SEC_WB_TEAM_CHILD_PARENT.CHILD_WBT_ID  = SO_SCHEDULE_GROUP.WBT_ID" 
			+ " )"
			+ " AND SO_SCHEDULE_GROUP.SKDGRP_INTRNL_TYPE = 12"; // departments only
					
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = conn.prepareStatement(sql);
			ps.clearParameters();
			ps.setString(1, fcastGrpTypName);
			ps.setInt(2, skdgrpId);
			
			rs = ps.executeQuery();
			if (rs.next()) return rs.getInt(1);
			
		} catch (SQLException sqle) {
			throw new RetailException("Error retrieving number of dept for skdgrpId " + skdgrpId, sqle);
		} finally {
			SQLHelper.cleanUp(ps, rs);
		}
		return 0;
	}

	/**
	 * Given two forecast values, determine if their values are different outside
	 * of a given precision.  The precision is determined by the workbrain parameter
	 * WBIAG_FCAST_EXT_PRECISION which is defined by a number or a percent value.
	 * 
	 * For example:
	 * i) Numeric value x means a and b are different if x > |a-b|
	 * ii) Percent value x% means a and b are different if x > |((a/b) * 100) - 100|
	 * @param a - forecast value
	 * @param b - forecast value to compare to
	 * @return boolean
	 */
	public boolean isFCastValDifferent(double a, double b) {
		double diff = Math.abs(a - b);
		boolean numericDiff = true;
		double precision = 0;

		if (diff == 0) return false;  // values are equal

		try {
			// define precision
			Object o = Registry.getVar(WBIAG_FCAST_EXT_PRECISION);
			if (o != null) {
				String s = o.toString().trim();
				int idx = s.indexOf("%"); 
				if (idx == -1) {
					precision = Double.parseDouble(s);
				} else if (idx + 1 == s.length()) {
					precision = Double.parseDouble(s.substring(0, idx));
					numericDiff = false;
				} else {
		            logger.error("Invalid format for WBIAG_FCAST_EXT_PRECISION: " + s);
				}
			}
		} catch (NamingException ne) {
            if (logger.isDebugEnabled()) logger.debug("Could not find Workbrain Registry param - " + WBIAG_FCAST_EXT_PRECISION + ". Check that the path and value exist in the Workbrain Registry.");
		}
		
		if (numericDiff) {
			return diff > precision;
		} else {
			double diffPercentage = Math.abs(((a / b) * 100) - 100);
			return diffPercentage > precision;
		}
	}
	
	/*
	 * Inner class represents an item in a formula.  Multiple is intended to be 1 or -1, 
	 * defining whether the value is to be added or subtracted.
	 */
	public class FormulaToken {
		public FormulaToken(int multiple) {
			this.multiple = multiple;
		}
		public FormulaToken(String fCastType, int multiple) {
			this.fCastType = fCastType;
			this.multiple = multiple;
		}
		public String fCastType = null;
		public int multiple = 0;
		
		public String toString() {
			return "FormulaToken fCastType = " + fCastType + ", multiple = " + multiple; 
		}
	}
	
}
