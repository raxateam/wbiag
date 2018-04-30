package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
//import java.util.Date;

import javax.naming.NamingException;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.EmpUdfAccess;
import com.workbrain.app.ta.model.EmpUdfData;
import com.workbrain.app.ta.model.EmpUdfDefData;
//import com.workbrain.app.ta.model.*;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Customization for EMPLOYEE_READER_GROUP
 *
 **/
public class HRRefreshTransactionEmpRdrGroup extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionEmpRdrGroup.class);

    private static final int EMPRDRGRP_COL = 90;
    private static final String EMPRDRGRP_INSERT_SQL
        = "INSERT INTO employee_reader_group(emprdrgrp_id, emp_id, rdrgrp_id) VALUES (?,?,?)";
    private static final String EMPRDRGRP_UPDATE_SQL
        = "UPDATE employee_reader_group SET rdrgrp_id = ? WHERE emp_id = ? AND rdrgrp_id = ?";
    private static final String EMPRDRGRP_SELECT_SQL
        = "SELECT rdrgrp_id FROM employee_reader_group WHERE emp_id = ? ";
    private static final String RDRGRP_SELECT_SQL
        = "SELECT rdrgrp_id,rdrgrp_name FROM reader_group";

    public static final String MULTIPLE_READER_GROUP_REGISTRY_PARAM = "system/WORKBRAIN_PARAMETERS/ENABLE_MULTIPLE_READER_GROUPS";
    public static final String HOME_READER_GROUP_ID_UDF = "HOME_READER_GROUP_ID";

    private Map readerGroups = null;
    private int[] custColInds = new int[] {EMPRDRGRP_COL};
    private DBConnection conn = null;

    /**
     * Sets custom column indexes to be used by the customization. Indexes start at 0
     * @param inds
     */
    public void setCustomColInds(int[] inds) {
        custColInds = inds;
    }

    /**
     * Override this class to customize after process batch events.
     * At this time, all HRRefreshTransactionData data has been processed.
     * All processed is available through <code>HRRefreshCache</code>.
     *
     * @param conn                          DBConnection
     * @param hrRefreshTransactionDataList  List of <code>HRRefreshTransactionData</code>
     * @param process                       HRRefreshProcessor
     * @throws Exception
     */
    public void postProcessBatch(DBConnection conn,
                                    List hrRefreshTransactionDataList,
                                    HRRefreshProcessor process) throws Exception {
        if (hrRefreshTransactionDataList == null || hrRefreshTransactionDataList.size() == 0) {
                return;
        }
        if (custColInds == null || custColInds.length != 1) {
            throw new WBInterfaceException ("Custom column index not supplied or too many, must be 1");
        }
        this.conn = conn;
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            try {
                processEmpRdrGroup(data.getImportData().getField(custColInds[0]),
                                   data,
                                   conn);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionEmpRdrGroup." , ex);}
                data.error("Error in HRRefreshTransactionEmpRdrGroup." + ex.getMessage() );
            }

        }
    }

    protected void insertEmpUDFData(int empId, int rdrGrpId, int eudfdId)
    throws SQLException{
    	EmpUdfAccess eua = new EmpUdfAccess(conn);
    	EmpUdfData eud = new EmpUdfData();

    	eud = new EmpUdfData();
		eud.setEmpId(empId);
		eud.setEudfdValue("" + rdrGrpId);
		eud.setEmpudfId(eudfdId);
        eud.setEudfdEffDate(new java.util.Date());

        eua.insert(eud);
    }

    protected void processEmpRdrGroup(String empRdrGroupVal,
                               HRRefreshTransactionData data,
                               DBConnection c) throws SQLException, WBInterfaceException {
        // *** only do this if EMPRDRGRP_COL is not blank in the file
        if (StringHelper.isEmpty(empRdrGroupVal)) {
            return;
        }

        String rdrGrpName = empRdrGroupVal;
        int newRdrGrpId = getReaderGroupId(rdrGrpName);

        boolean enableMultipleReaderGroups = false;
        try {
        	enableMultipleReaderGroups = (Boolean.valueOf((String)Registry.getVar(MULTIPLE_READER_GROUP_REGISTRY_PARAM))).booleanValue();

        	if(logger.isDebugEnabled())
        		logger.debug("Found registry setting '" + MULTIPLE_READER_GROUP_REGISTRY_PARAM + "': value=" + enableMultipleReaderGroups);
        } catch (NamingException ne) {
            logger.error("Could not find registry variable '" + MULTIPLE_READER_GROUP_REGISTRY_PARAM + "'! Defaulting to FALSE: " + ne.getMessage());
        }

        if (newRdrGrpId == -1) {
            throw new RuntimeException ("Reader group not found : " +  rdrGrpName);
        }

        CodeMapper cm = CodeMapper.createCodeMapper(conn);
    	EmpUdfDefData eudd = cm.getEmpUdfDefByName(HOME_READER_GROUP_ID_UDF);

        if (data.isNewEmployee()) {
            insertEmpRdrGroup(data.getEmpId(), newRdrGrpId);

            if(enableMultipleReaderGroups)
            	insertEmpUDFData(data.getEmpId(), newRdrGrpId, eudd.getEmpudfId());
        }
        else {
            List rdrGrpList = getEmpRdrGroups(data.getEmpId());

            if(rdrGrpList.size() == 0) {
            	insertEmpRdrGroup(data.getEmpId() , newRdrGrpId);

            	if(enableMultipleReaderGroups)
            		insertEmpUDFData(data.getEmpId(), newRdrGrpId, eudd.getEmpudfId());
            }
            else {
            	int oldHomeRdrGrpId = newRdrGrpId;

            	if(enableMultipleReaderGroups) {
            		//get the old home reader group id
            		EmpUdfAccess eua = new EmpUdfAccess(conn);
            		EmpUdfData eud = eua.loadByEmpIdAndUdfDefName(data.getEmpId(), HOME_READER_GROUP_ID_UDF);

            		//if it's null, set it
            		if(eud == null) {
            			insertEmpUDFData(data.getEmpId(), newRdrGrpId, eudd.getEmpudfId());
            		}
            		//else if it's empty, set it
            		else if(eud.getEudfdValue().equals("")) {
            			updateEmpUDFData(data.getEmpId(), newRdrGrpId, eudd.getEmpudfId());
            		}
            		//else get it
            		else {
            			oldHomeRdrGrpId = Integer.parseInt(eud.getEudfdValue());
            		}

            		//if the old group is different from the new one
            		//update it
            		if(oldHomeRdrGrpId != newRdrGrpId) {
            			updateEmpRdrGroup(data.getEmpId(), newRdrGrpId, oldHomeRdrGrpId);
            			updateEmpUDFData(data.getEmpId(), newRdrGrpId, eudd.getEmpudfId());
            		}
            	}//end if enable multiple reader groups
            	else {
            		//get the current reader group
                    int empRdrGrpId = getEmpRdrGroup(data.getEmpId());

                    //if there are no records, insert the reader group
                    if (empRdrGrpId == -1) {
                        insertEmpRdrGroup(data.getEmpId() , newRdrGrpId);
                    }
                    else {
                        //otherwise, if the old group is not
                    	//the same as the new group, update
                    	//the old group
                        if (newRdrGrpId != empRdrGrpId) {
                            updateEmpRdrGroup(data.getEmpId(), newRdrGrpId, empRdrGrpId);
                        }
                    }
            	}//end else if enable multiple reader groups
            }//end else if reader groups list size == 0
        }//end if new employee
    }

    protected int insertEmpRdrGroup(int empId , int rdrGrpId) throws SQLException{
        int upd = 0;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(EMPRDRGRP_INSERT_SQL);
            ps.setInt(1 , conn.getDBSequence("seq_emprdrgrp_id").getNextValue());
            ps.setInt(2 , empId);
            ps.setInt(3 , rdrGrpId);
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return upd;
    }

    protected int getReaderGroupId(String rdrGrpName) throws SQLException{
        int ret = -1;
        if (readerGroups == null) {
            readerGroups = new HashMap();
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = conn.prepareStatement(RDRGRP_SELECT_SQL);
                rs = ps.executeQuery();
                while (rs.next()) {
                    readerGroups.put(rs.getString(2) , new Integer(rs.getInt(1)));
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }

        }
        if (readerGroups.containsKey(rdrGrpName)) {
            ret = ((Integer)readerGroups.get(rdrGrpName)).intValue();
        }
        return ret;
    }

    protected void updateEmpUDFData(int empId, int rdrGrpId, int eudfdId) {
    	EmpUdfAccess eua = new EmpUdfAccess(conn);
    	try {
	    	EmpUdfData eud = eua.loadByEmpAndUdfDefId(empId, eudfdId);

	    	eud.setEudfdValue("" + rdrGrpId);

			eua.update(eud);
    	}
    	catch(SQLException e) {
    		throw new RuntimeException("Could not get employee UDF data!", e);
    	}
    }

    protected int updateEmpRdrGroup(int empId , int rdrGrpId, int oldRdrGrpId) throws SQLException{
        int upd = 0;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(EMPRDRGRP_UPDATE_SQL);
            ps.setInt(1 , rdrGrpId);
            ps.setInt(2 , empId);
            ps.setInt(3 , oldRdrGrpId);
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return upd;
    }

    protected int getEmpRdrGroup(int empId) throws SQLException{
        int ret = -1;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(EMPRDRGRP_SELECT_SQL);
            ps.setInt(1 , empId);
            rs = ps.executeQuery();
            if(rs.next()) {
                ret = rs.getInt(1);
            }
        }
        finally {
            if (rs != null)
                rs.close();
            if (ps != null)
                ps.close();
        }
        return ret;
    }

    protected List getEmpRdrGroups(int empId) throws SQLException{
        List ret = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(EMPRDRGRP_SELECT_SQL);
            ps.setInt(1 , empId);
            rs = ps.executeQuery();
            while(rs.next()) {
                Integer rdrGrp = new Integer(rs.getInt(1));
                ret.add(rdrGrp);
            }
        }
        finally {
            if (rs != null)
                rs.close();
            if (ps != null)
                ps.close();
        }
        return ret;
    }
}