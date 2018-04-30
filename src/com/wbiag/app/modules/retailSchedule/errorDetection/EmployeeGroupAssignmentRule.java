package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.EmployeeAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.app.modules.retailSchedule.model.EmployeeGroup;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.services.PreProcessorUtils;
import com.workbrain.app.modules.retailSchedule.type.InternalType;
import com.workbrain.app.modules.retailSchedule.utils.SchoolAttendance;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

public class EmployeeGroupAssignmentRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(EmployeeGroupAssignmentRule.class);

	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		DBConnection conn = context.getConnection();
		int DEFAULT_EMP_GROUP_ID = context.getDefaultEmpgrpId();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employees assigned to the right groups");
		actionResult.setHelpTip("Employees should belong to a staff group and fall into at least one rule group");
		actionResult.setHelpDesc("Employees who are part of the team assigned to a store should belong to a staff group assigned to that store, and fit into at least one rule group assigned to their staff group");
		actionResult.setErrorMsg("ERROR:\n");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.18...");
		
		if(corpTree !=null)
	    {
			PreparedStatement stmt = null;
			ResultSet rs = null;
			
			for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
	        {
	        	try {
	        		ScheduleGroupData loc = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
		            
	        		if(InternalType.isStore(new Integer(loc.getSkdgrpIntrnlType()))) {
		        		int locTeamID = loc.getWbtId();
			            int skdgrpID = loc.getSkdgrpId().intValue();
						
						String strSelect = "SELECT E.EMP_ID, E.EMP_NAME, T.WBT_NAME ";
						strSelect += "FROM EMPLOYEE E, WORKBRAIN_TEAM T, EMPLOYEE_TEAM ET ";
						strSelect += "WHERE E.EMP_ID = ET.EMP_ID AND  ";
						strSelect += "T.WBT_ID = ET.WBT_ID AND ";
						strSelect += "T.WBT_ID = ? ";
	
						stmt = conn.prepareStatement(strSelect);
						stmt.setInt(1, locTeamID);
				        rs = stmt.executeQuery();
				        
				        while(rs.next()) {
				        	strSelect = "SELECT COUNT(EMP_ID) AS C ";
				        	strSelect += "FROM SO_EMPLOYEE ";
				        	strSelect += "WHERE EMP_ID = ?  AND ";
				        	strSelect += "SKDGRP_ID = ? ";
				        	
				        	PreparedStatement soEmpStmt = conn.prepareStatement(strSelect);
				        	soEmpStmt.setInt(1, rs.getInt("EMP_ID"));
				        	soEmpStmt.setInt(2, skdgrpID);
				        	ResultSet soEmpRS = soEmpStmt.executeQuery();
				        	
				        	soEmpRS.next();
				        	if(soEmpRS.getInt("C") == 0)
				        		result += "-Employee " + rs.getString("EMP_NAME") + " is on Workbrain team " + rs.getString("WBT_NAME") + " but not assigned to a staff group at location " + loc.getSkdgrpName() + "\n";
				        	else if(determineGroupRulesToApply(conn, rs.getInt("EMP_ID"), skdgrpID, Calendar.getInstance().getTime()) == DEFAULT_EMP_GROUP_ID)
				        		result += "-Employee " + rs.getString("EMP_NAME") + " does not fit a rule group at location " + loc.getSkdgrpName() + "\n";
				        }//end while more records
		            }//end if store type
		        }//end try
	        	catch(SQLException e) {
	        		result += e.getMessage() + "\n";
	        	}
	        	catch(RetailException e) {
	        		result += e.getMessage() + "\n";
	        	}
	        }//end for iter
	    }//end if corptree not null
		
		if(result.compareTo("") == 0)
			result = "OK";

        if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.18");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Employees assigned to the right groups";
	}
	
	 private int determineGroupRulesToApply(DBConnection conn, int empID, int skdgrpID, Date targetDate) throws RetailException {
	        try {
		    	EmployeeAccess empAccess = new EmployeeAccess(conn);
		    	Employee emp = empAccess.loadByEmpId(empID); 
		    	DBInterface.init(conn);
		    	SchoolAttendance sa = new SchoolAttendance(DBInterface.getCurrentConnection());
		    	
		    	List groups = DBInterface.findEmployeeGroupList(CorporateEntity.ALL_SUBTREE, CorporateEntity.getCorporateEntity(skdgrpID));
		    	
		    	int retVal = emp.getEmpgrpId();
		        
		        Date birthDate = emp.getWBEmployee().getEmpBirthDate();
		        int age = (int)Math.floor(DateHelper.getMonthsBetween(birthDate, Calendar.getInstance().getTime())/12);
		        int maxMinorAge = PreProcessorUtils.getMaxMinorAge();               
		        boolean minorExempt = (emp.getSempIsMinor() == 1)? true : false;
		        
		        if ((age > maxMinorAge) || (minorExempt)) {
		            return retVal;
		        } else {
		            
		            boolean isStudent = sa.isStudent(emp.getEmpId(), targetDate);
		            boolean isSchoolDay = sa.isSchoolDay(emp.getEmpId(), targetDate);            
		
		            Iterator iter = groups.iterator();
		            while(iter.hasNext()) {
		                EmployeeGroup oEmpGroup = (EmployeeGroup)iter.next();
		                if ((oEmpGroup.getEmpgrpAgesFrom() <= age) 
		                        && (age <= oEmpGroup.getEmpgrpAgesTo())
		                        && (isStudent == ((oEmpGroup.getEmpgrpStudents() == 1)? true : false))
		                        && (isSchoolDay == ((oEmpGroup.getEmpgrpSchoolDay() == 1)? true : false))) {
		                    return oEmpGroup.getEmpgrpId();
		                }
		            }
		
		            return retVal;
		        }
	        }
	        catch(SQLException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());
				
	        	throw new RetailException(e);
	        }
	    }

}
