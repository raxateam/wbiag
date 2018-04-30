package com.wbiag.server.cognos;

import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.db.MaintenanceFormAccess;
import com.workbrain.app.ta.model.MaintenanceFormData;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * @author atam
 * 
 * TT521 Date: Nov 18, 2005.
 * 
 * This class creates the maintenance form and the cognos mapping report given
 * the maintenance form name and cognos report path.
 * 
 */
public class CreateCognosMapping {

	DBConnection conn = null;
	
	public static final String QUERY_UPDATEMFRM_JSPPATH = "update maintenance_form set mfrm_jsp=? where mfrm_id=?";

	public static final String QUERY_INSERT_CGMAP_TBL = "insert into cognos_mapping "
			+ "(CGMAP_ID, CGMAP_NAME, CGMAP_DESC, CGMAP_REPORT_PATH, MFRM_ID) values (?, ?, ?, ?, ?)";

	public static final String QUERY_INSERT_MFMGRP_TBL = "insert into maintenance_form_grp "
			+ "(MFG_ID, MFRM_ID, WBG_ID) values (?, ?, ?)";

	public static final String QUERY_UPDATEPARENTFORM_NULL = "update maintenance_form set mfrm_parent_mfrm_id=null where mfrm_id=?";

	public static final String QUERY_CHECKVALID_MFRMID = "select * from maintenance_form where mfrm_id=?";

	public static final int MFRMT_ID = 11;

	public CreateCognosMapping(DBConnection conn) {
		this.conn = conn;
	}
	
	private int cgmapId = 0;
	private String cgmapName = null;
	private String cgmapDesc = null;
	private String cgmapRptPath = null;
	private int mfrmId = 0;
	private int mfrmMenuParentId = 0;
	private boolean loaded = false;

	public void buildCognosMapper(String frmName, String frmDesc,
			String pathName, String menuParent, DBConnection conn)
			throws SQLException {
		cgmapName = frmName;
		cgmapDesc = frmDesc;
		cgmapRptPath = pathName;
		mfrmMenuParentId = Integer.parseInt(menuParent);
		this.conn = conn;
		
		buildCognosMapper();
	}
	/**
	 * @param frmName
	 * @param frmDesc
	 * @param pathName
	 * @param menuParent
	 * @param conn
	 * @throws SQLException
	 * 
	 * Called by createCognosMapping.jsp
	 */
	public void buildCognosMapper() throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		int newMfrm_Id = conn.getDBSequence("SEQ_MFRM_ID").getNextValue();
		int mfrmGrp_id;
		String jsp_path;
		boolean validMfrmId = false;

		try {
			// this function grabs the next valid unused maintenance form ID
			// handles cases where sequences and most recent mfrm_id is out of
			// sync
			while (validMfrmId == false) {

				// check whether this mfrmId is already in use
				// the maintenance_form will use the nextVal of the sequence, so
				// we need to decrement by
				// one in order for the form to always use the mfrm_id that was
				// validated as free
				ps = conn.prepareStatement(QUERY_CHECKVALID_MFRMID);
				ps.setInt(1, newMfrm_Id);
				rs = ps.executeQuery();

				validMfrmId = (rs.next()) ? false : true;

				if (validMfrmId == false) {
					newMfrm_Id = conn.getDBSequence("SEQ_MFRM_ID") .getNextValue();
				}

			}

			// create a new maintenance form with above parameters
			MaintenanceFormAccess mfrmAcc = new MaintenanceFormAccess(conn);
			MaintenanceFormData mfrmData = new MaintenanceFormData();

			mfrmData.setMfrmName(cgmapName.trim());
			mfrmData.setMfrmDesc(cgmapName.trim());
			mfrmData.setMfrmAsp("maintenance.asp");
			mfrmData.setMfrmtId(MFRMT_ID);
			mfrmData.setMfrmTable("x");
			mfrmData.setMfrmKeyfield("x");
			mfrmData.setMfrmParameter("[NEW]=TRUE~[EDT]=BTN~[DEL]=TRUE");
			mfrmData.setMfrmMenuparentId(mfrmMenuParentId);
			mfrmAcc.insert(mfrmData);
			
			mfrmId = mfrmData.getMfrmId();

			jsp_path = "/reports/cognos/reportParams.jsp?mfrm_id="
					+ mfrmId + "&report_name=" + cgmapName.trim();

			//update maintenance_form set mfrm_jsp=? where mfrm_id=?
			ps = conn.prepareStatement(QUERY_UPDATEMFRM_JSPPATH);
			ps.clearParameters();
			ps.setString(1, jsp_path);
			ps.setInt(2, mfrmId);
			ps.executeUpdate();
			
			//update maintenance_form set mfrm_parent_mfrm_id=null where mfrm_id=?
			ps = conn.prepareStatement(QUERY_UPDATEPARENTFORM_NULL);
			ps.clearParameters();
			ps.setInt(1, mfrmId);
			ps.executeUpdate();

			// update COGNOS_MAPPING table
			cgmapId = conn.getDBSequence("SEQ_CGMAP_ID").getNextValue();
			ps = conn.prepareStatement(QUERY_INSERT_CGMAP_TBL);
			ps.setInt(1, cgmapId);
			ps.setString(2, cgmapName.trim());
			ps.setString(3, cgmapDesc.trim());
			ps.setString(4, cgmapRptPath.trim());
			ps.setInt(5, mfrmId);
			ps.executeUpdate();

			// update MAINTENANCE_FORM_GRP table
			mfrmGrp_id = conn.getDBSequence("SEQ_MFG_ID").getNextValue();
			ps = conn.prepareStatement(QUERY_INSERT_MFMGRP_TBL);
			ps.setInt(1, mfrmGrp_id);
			ps.setInt(2, mfrmId);
			ps.setInt(3, 2);
			ps.executeUpdate();

		} catch (SQLException e) {
			throw e;
		} finally { 
			
			if (ps!=null)
				ps.close();
			if (rs!=null)
				rs.close();
		}
		
		loaded = true;
	}
	
	public void updateCognosMapper() throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;

		String mfrmSql = "UPDATE MAINTENANCE_FORM SET"
			+ " MFRM_NAME = ?, MFRM_DESC = ?, MFRM_JSP = ?, MFRM_MENUPARENT_ID = ?" 
			+ " WHERE MFRM_ID = ?";
		String jspPath = "/reports/cognos/reportParams.jsp?mfrm_id=" 
				+ mfrmId + "&report_name=" + cgmapName.trim();
		try {
			ps = conn.prepareStatement(mfrmSql);
			ps.setString(1, cgmapName);
			ps.setString(2, cgmapName);
			ps.setString(3, jspPath);
			ps.setInt(4, mfrmMenuParentId);
			ps.setInt(5, mfrmId);
			ps.executeUpdate();
		} finally { 
			SQLHelper.cleanUp(ps, rs);
		}
		
		String localeSql = "UPDATE WB_LOCALZD_TBLVAL SET WBLTV_DATA_NAME = ? "
				+ " WHERE WB_LOCALZD_TBLVAL.CLIENT_ID=1 AND 1=1 AND wblt_id = "
				+ " (SELECT wblt_id FROM wb_localzd_tbl WHERE wblt_tb_name = 'VL_MAINTENANCE_FORM')" 
				+ " and WBLTV_DATA_ID = ?";
		try {
			ps = conn.prepareStatement(localeSql);
			ps.setString(1, cgmapName);
			ps.setInt(2, mfrmId);
			ps.executeUpdate();
		} finally { 
			SQLHelper.cleanUp(ps, rs);
		}
						
		String cgmapSql = "UPDATE COGNOS_MAPPING SET"
			+ " CGMAP_NAME = ?, CGMAP_DESC = ?, CGMAP_REPORT_PATH = ?" 
			+ " WHERE CGMAP_ID = ?";
		try {
			ps = conn.prepareStatement(cgmapSql);
			ps.setString(1, cgmapName);
			ps.setString(2, cgmapDesc);
			ps.setString(3, cgmapRptPath);
			ps.setInt(4, cgmapId);
			ps.executeUpdate();
		} finally { 
			SQLHelper.cleanUp(ps, rs);
		}

	}

	public void deleteCognosMapper() throws SQLException {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute("DELETE FROM COGNOS_MAPPING WHERE CGMAP_ID = " + cgmapId);
			stmt.execute("DELETE FROM MAINTENANCE_FORM_GRP WHERE MFRM_ID = " + mfrmId);
			stmt.execute("DELETE FROM MAINTENANCE_FORM WHERE MFRM_ID = " + mfrmId);
		} finally {
			SQLHelper.cleanUp(stmt);
		}
		
	}

	public void loadCGMapById(int cgmapId) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = "SELECT B.MFRM_NAME, CGMAP_DESC, CGMAP_REPORT_PATH, B.MFRM_ID, B.MFRM_MENUPARENT_ID"
			+ " FROM COGNOS_MAPPING A, MAINTENANCE_FORM B"
			+ " WHERE A.MFRM_ID = B.MFRM_ID"
			+ " AND CGMAP_ID = ?";
		
		try {
			ps = conn.prepareStatement(sql);
			ps.setInt(1, cgmapId);
			rs = ps.executeQuery();
			if (rs.next()) {
				this.loaded = true;
				this.cgmapId = cgmapId;
				this.cgmapName = rs.getString("MFRM_NAME");
				this.cgmapDesc = rs.getString("CGMAP_DESC");
				this.cgmapRptPath = rs.getString("CGMAP_REPORT_PATH");
				this.mfrmId = rs.getInt("MFRM_ID");
				this.mfrmMenuParentId = rs.getInt("MFRM_MENUPARENT_ID");
			}
			
		} finally { 
			SQLHelper.cleanUp(ps, rs);
		}
	}

	public int getMfrmIdByCgmapId(int cgmapId) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = "SELECT A.MFRM_ID FROM MAINTENANCE_FORM A, COGNOS_MAPPING B"
			//+ " WHERE A.MFRM_NAME = CGMAP_NAME"
			+ " WHERE A.MFRM_ID = B.MFRM_ID"  // bchan 3/31/2005 
			+ " AND B.CGMAP_ID = ?";

		try {
			ps = conn.prepareStatement(sql);
			ps.setInt(1, cgmapId);
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			
		} finally { 
			SQLHelper.cleanUp(ps, rs);
		}
		return 0;
	}
	
	
	public boolean checkCGMapPathExists(int cgMapId, String cgMapPath) throws SQLException {
		return checkColValueExists(cgMapId, cgMapPath, "COGNOS_MAPPING", "CGMAP_ID", "CGMAP_REPORT_PATH");
	}

	public boolean checkMFrmNameExists(int mFrmId, String mFrmName) throws SQLException {
		return checkColValueExists(mFrmId, mFrmName, "MAINTENANCE_FORM", "MFRM_ID", "MFRM_NAME");
	}
	
	private boolean checkColValueExists(int id, String val, String tbl, String idCol, String col) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		String sql = "SELECT COUNT(1) FROM " + tbl
			+ " WHERE " + col + " = ? "
			+ " AND NOT " + idCol + " = ?";
		
		try {
			ps = conn.prepareStatement(sql);
			ps.setString(1, val);
			ps.setInt(2, id);
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt(1) > 0;
			}
			
		} finally { 
			SQLHelper.cleanUp(ps, rs);
		}
		return true;
	}

	public String getCgmapDesc() {
		return cgmapDesc;
	}

	public void setCgmapDesc(String cgmapDesc) {
		this.cgmapDesc = cgmapDesc;
	}

	public int getCgmapId() {
		return cgmapId;
	}

	public void setCgmapId(int cgmapId) {
		this.cgmapId = cgmapId;
	}

	public String getCgmapName() {
		return cgmapName;
	}

	public void setCgmapName(String cgmapName) {
		this.cgmapName = cgmapName;
	}

	public String getCgmapRptPath() {
		return cgmapRptPath;
	}

	public void setCgmapRptPath(String cgmapRptPath) {
		this.cgmapRptPath = cgmapRptPath;
	}

	public int getMfrmId() {
		return mfrmId;
	}

	public void setMfrmId(int mfrmId) {
		this.mfrmId = mfrmId;
	}
	
	public boolean isLoaded() {
		return loaded;
	}

	public int getMfrmMenuParentId() {
		return mfrmMenuParentId;
	}
	public void setMfrmMenuParentId(int mfrmMenuParentId) {
		this.mfrmMenuParentId = mfrmMenuParentId;
	}
}
