package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Date;

import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.EmployeeGroup;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.server.registry.Registry;
import com.workbrain.server.sql.ConnectionManager;
import com.workbrain.sql.DBConnection;

public class ErrorDetectionContext {

	private static final Logger logger = Logger.getLogger(ErrorDetectionContext.class);
	
	
	private DBConnection conn = null;
	private CodeMapper cm = null;
	private List corpTree;
	private String moselClass = "";
	private SimpleDateFormat sdf;
	private Date startDate = null;
	private List allEmployees;
	private List vEmpGrps = null;
	private String SKDGRP_ID = "";
	private String EMPGRP_ID = "";
	
	private int DEFAULT_EMP_GROUP_ID = 1;
	
	public ErrorDetectionContext(DBConnection c, String skdgrp_id, String empgrp_id, Date sDate) {
		SKDGRP_ID = skdgrp_id;
		EMPGRP_ID = empgrp_id;
		startDate = sDate;
		
		try {
			conn = c;
			DBInterface.init(conn);
	        cm = CodeMapper.createCodeMapper( conn );

			ScheduleGroupData skdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer(SKDGRP_ID.toString()));
			moselClass = (String)Registry.getVar("system/modules/scheduleOptimization/MOSEL_CLASS");

			//List of employees to check (According to Staffing Groups defined)
			vEmpGrps = new Vector();

			//Grab the list of Staffing Groups selected
			StringTokenizer st = new StringTokenizer( EMPGRP_ID.toString(), "," );
			while( st.hasMoreTokens() ) {
	                 String token = st.nextToken().trim();
	                 if (logger.isDebugEnabled()){
	                     logger.debug("Adding token : " + token);
	                 }
	    		     vEmpGrps.add(new EmployeeGroup(new Integer(token  )));

			}
			allEmployees = DBInterface.findActiveEmployeeListUsingListOfEmployeeGroups((Vector)vEmpGrps);
			if (logger.isDebugEnabled()) {
				logger.debug(allEmployees.toString());
	        }
			
			corpTree = skdgrpData.getCorporateEntity().getTree(CorporateEntity.ALL_SUBTREE);
			
			sdf = new SimpleDateFormat("MM/dd/yyyy");
		}
		catch(SQLException e) {
			logger.error(e.getMessage());
		}
		catch(RetailException e) {
			logger.error(e.getMessage());
		}
		catch(NamingException e) {
			logger.error(e.getMessage());
		}
	}
	
	public DBConnection getConnection(){
		return conn;
	}
	
	public CodeMapper getCodeMapper(){
		return cm;
	}
	public List getCorpTree(){
		return corpTree;
	}
	public String getMoselClass(){
		return moselClass;
	}
	public SimpleDateFormat getDateFormat(){
		return sdf;
	}
	public List getAllEmployeesList(){
		return allEmployees;
	}
	public List getAllGroupVector()
	{
		return vEmpGrps;
	}
	public String getSkdgrpId() {
		return SKDGRP_ID;
	}
	public String getEmpgrpId(){
		return EMPGRP_ID;
	}
	public int getDefaultEmpgrpId(){
		return DEFAULT_EMP_GROUP_ID;
	}
	public Date getStartDate(){
		return startDate;
	}
	
	
}
