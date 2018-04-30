package com.wbiag.app.modules.retailSchedule;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Vector;
import java.util.Date;
import java.util.List;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Calendar;

import java.text.SimpleDateFormat;

import javax.naming.NamingException;

import com.workbrain.sql.DBConnection;

import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.EmployeeAccess;

import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.model.EmployeeGroup;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.app.modules.retailSchedule.model.HoursOfOperation;
import com.workbrain.app.modules.retailSchedule.model.Forecast;
import com.workbrain.app.modules.retailSchedule.model.ForecastDetail;
import com.workbrain.app.modules.retailSchedule.model.Distribution;
import com.workbrain.app.modules.retailSchedule.model.DistributionDetail;

import com.workbrain.app.modules.retailSchedule.type.InternalType;
import com.workbrain.app.modules.retailSchedule.type.HoursOfOperationType;

import com.workbrain.app.modules.retailSchedule.services.MoselRegistryKeys;

import com.workbrain.app.modules.retailSchedule.utils.SODate;
import com.workbrain.app.modules.retailSchedule.utils.SchoolAttendance;

import com.workbrain.app.modules.retailSchedule.services.PreProcessorUtils;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;

import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.WorkbrainTeamAccess;

import com.workbrain.app.modules.availability.db.AvailabilityAccess;
import com.workbrain.app.modules.availability.db.AvOvrAccess;

import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.app.ta.model.EmployeeJobData;
import com.workbrain.app.ta.model.JobData;
import com.workbrain.app.ta.model.WorkbrainTeamData;

import com.workbrain.server.registry.Registry;

import com.workbrain.server.sql.ConnectionManager;

import com.workbrain.sql.SQLHelper;

import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

import com.sshtools.j2ssh.SshClient;

import com.sshtools.j2ssh.configuration.SshConnectionProperties;

import com.sshtools.j2ssh.session.SessionChannelClient;

import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;

import com.sshtools.j2ssh.SftpClient;

import com.sshtools.j2ssh.sftp.FileAttributes;

import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;

import org.apache.log4j.Logger;

/**
 * This class performs the checks as required by the
 * LFSO Error Detection Script. Each public method in this
 * class follows a standard convention and naming system.
 * For consistency, please copy and modify the template
 * method ONLY.
 *
 * All helper methods should be defined as private methods
 * and all action methods should be public.
 *
 * @author tyoung - December 16th, 2005		TT603
 * @author tyoung - December 19th, 2005		TT502
 * @author tyoung - January 5th, 2005		TT602
 * @author tyoung - January 25th, 2005		TT607
 * @author tyoung - Frbruary 3rd, 2006		TT784
 *
 */
public class ErrorDetectionScript {

	//Some common objects that are static and used through
	//the whole class
	private DBConnection conn = null;
	private CodeMapper cm = null;
	private List corpTree;
	private String moselClass = "";
	private String pdfURI = "";
	private SimpleDateFormat sdf;
	private List allEmployees;

	private static final Logger logger = Logger.getLogger("modules.retailSchedule.LfsoErrorScript.jsp");

	private String SKDGRP_ID = "";
	private String EMPGRP_ID = "";

	private int DEFAULT_EMP_GROUP_ID = 1;

	public ErrorDetectionScript(String skdgrp_id, String empgrp_id) {
		SKDGRP_ID = skdgrp_id;
		EMPGRP_ID = empgrp_id;

		try {
			conn = new DBConnection(ConnectionManager.getConnection());
			DBInterface.init(conn);
	        cm = CodeMapper.createCodeMapper( conn );

			ScheduleGroupData skdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer(SKDGRP_ID.toString()));
			moselClass = (String)Registry.getVar("system/modules/scheduleOptimization/MOSEL_CLASS");

			//List of employees to check (According to Staffing Groups defined)
			List vEmpGrps = new Vector();

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

			pdfURI = (String)Registry.getVar("system/modules/scheduleOptimization/SO_PDF_URI");

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

	/***** 	TEMPLATE METHOD
	 * This method should be copied and altered for each
	 * new check that is to be added according to the
	 * following guidelines:
	 *
	 * 1) The message should contain the string 'OK' or
	 * the message to be returned to the user, without
	 * the error message added to the front of it. The
	 * error message will be added by the JSP page
	 *
	 * 2) If this check will cause a fatal error and halt
	 * execution of the script, the else statement at the
	 * end of the tamplate method should be uncommented.
	 * Otherwise, the else statement can be removed.
	 *
	 * 3) You must add a help title, help tip, help
	 * description and error message to the first
	 * block of commands in the template. This will set
	 * the values for the object that is returned to the
	 * JSP page. Leaving these values unset and hard-
	 * coding into the JSP page is not recommended.
	 *
	 * 4) None of the other code needs to be modified.
	 * Add the code to determine the result in the section
	 * of this template method marked as 'Code should
	 * go here'.
	//declare check 4.x.x:
	public ErrorDetectionScriptResult action<func_spec_number>() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("");
		actionResult.setHelpTip("");
		actionResult.setHelpDesc("");
		actionResult.setErrorMsg("\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.x.x...");

		//Code should go here. The only thing to be
		//changed is the result string

		if(result.compareTo("") == 0)
			result = "OK";
		//else
		 	//actionResult.setFatalError(true);
        if(logger.isDebugEnabled())
			logger.debug("Finished check 4.x.x");

		actionResult.setMessage(result);
		return actionResult;
	}
	        END TEMPLATE METHOD  *****/
    /*
     * DECLARE DEFAULT CHECKS
     */

    //Declare default check root location is a store
    public ErrorDetectionScriptResult action4_0_1() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Root location check");
		actionResult.setHelpTip("The root location must have an internal type of 'Store'");
		actionResult.setHelpDesc("The root location must have an internal type of 'Store'");
		actionResult.setErrorMsg("FATAL ERROR: ");

		String result = new String("");
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.0.1...");

		try {
		    ScheduleGroupData loc = ScheduleGroupData.getScheduleGroupData( new Integer( SKDGRP_ID.toString() ) );

		    if( loc != null )
		    {
		    	int type = loc.getSkdgrpIntrnlType();
		        if(!InternalType.isStore(new Integer(type)))
		            result += "Root Location is not Store type";

		    }
		}
		catch(RetailException e) {
			result = e.getMessage() + "\n";
		}

		if(result.compareTo("") == 0)
			result = "OK";
		else
		 	actionResult.setFatalError(true);

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.0.1");

		actionResult.setMessage(result);
		return actionResult;
	}

    //Declare default 2 check number of locations > 3
    public ErrorDetectionScriptResult action4_0_2() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Number of Locations");
		actionResult.setHelpTip("There must be at least three locations");
		actionResult.setHelpDesc("There must be at least three locations for the script to properly execute. One should be a store, one should be a schedule department, and one should be a forecast driver");
		actionResult.setErrorMsg("FATAL ERROR: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.0.2...");

	    if(corpTree !=null)
	    {
	        if(corpTree.size() <= 2 )
	        	result = "Number of Locations found: " + corpTree.size();

	    }

	    if(result.compareTo("") == 0)
			result = "OK";
		else
		 	actionResult.setFatalError(true);

	    if(logger.isDebugEnabled())
			logger.debug("Finished check 4.0.2");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.0.3:
	public ErrorDetectionScriptResult action4_0_3() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Location parent check");
		actionResult.setHelpTip("Checks to ensure a location's parent is not itself");
		actionResult.setHelpDesc("If a location's parent is set to itself, an endless loop could be entered causing a stack overflow. This check will ensure no location except the root location has itself as it's parent");
		actionResult.setErrorMsg("FATAL ERROR: The following locations have their parent location set to themself\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.6...");

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			String strSelect = "SELECT SKDGRP_NAME ";
			strSelect += "FROM SO_SCHEDULE_GROUP ";
			strSelect += "WHERE SKDGRP_ID = SKDGRP_PARENT_ID ";
			strSelect += "AND SKDGRP_ID >1 ";

			stmt = conn.prepareStatement(strSelect);
			rs = stmt.executeQuery();

			while(rs.next())
				result += "-" + rs.getString("SKDGRP_NAME") + "\n";
		}
		catch(SQLException e) {
			logger.error(e);
			result = e.getMessage();
		}
		finally {
			SQLHelper.cleanUp(stmt, rs);
		}

		if(result.compareTo("") == 0)
			result = "OK";
		else
			actionResult.setFatalError(true);

        if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.6");

		actionResult.setMessage(result);
		return actionResult;
	}

//	declare check 4.0.4:
	public ErrorDetectionScriptResult action4_0_4() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Team parent check");
		actionResult.setHelpTip("Checks to ensure a team's parent is not itself");
		actionResult.setHelpDesc("If a team's parent is set to itself, an endless loop could be entered causing a stack overflow. This check will ensure no team except the root team has itself as it's parent");
		actionResult.setErrorMsg("FATAL ERROR: The following teams have their parent team set to themself\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.0.4..");

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			String strSelect = "SELECT WBT_NAME ";
			strSelect += "FROM WORKBRAIN_TEAM ";
			strSelect += "WHERE WBT_ID = WBT_PARENT_ID ";
			strSelect += "AND WBT_ID > 1 ";

			stmt = conn.prepareStatement(strSelect);
			rs = stmt.executeQuery();

			while(rs.next())
				result += "-" + rs.getString("WBT_NAME") + "\n";
		}
		catch(SQLException e) {
			logger.error(e);
			result = e.getMessage();
		}
		finally {
			SQLHelper.cleanUp(stmt, rs);
		}

		if(result.compareTo("") == 0)
			result = "OK";
		else
			actionResult.setFatalError(true);

        if(logger.isDebugEnabled())
			logger.debug("Finished check 4.0.4");

		actionResult.setMessage(result);
		return actionResult;
	}

    /*
     * DECLARE BUG CHECKS
     */

//  declare check 4.1.1:
	public ErrorDetectionScriptResult action4_1_1() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Volume Type");
		actionResult.setHelpTip("Checks volume types");
		actionResult.setHelpDesc("A bug has been found in the system that puts a null value in the database. The cause of this error has not yet been determined. This method will notify the user of this problem if it finds a null value in the Volume Type. This error will halt execution of the script.");
		actionResult.setErrorMsg("FATAL ERROR: Volume type is null for the following location(s): ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.1...");

		if(corpTree !=null)
	    {
	        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
	        {
	        	try {
		            ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
		            if( tempSkdgrpData.getVoltypId() == null )
		            {
					    if( result.length() == 0 )
					        result += tempSkdgrpData.getSkdgrpName();
					    else
					        result += ", " + tempSkdgrpData.getSkdgrpName();
		            }
	        	}
	        	catch(RetailException e) {
	        		result += e.getMessage();
	        	}
	        }
	    }

		if(result.compareTo("") == 0)
			result = "OK";
		else
			actionResult.setFatalError(true);

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.1");

		actionResult.setMessage(result);
		return actionResult;
	}

//	declare check 4.1.2:
	public ErrorDetectionScriptResult action4_1_2() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employee Skills");
		actionResult.setHelpTip("Ensures that employees have at least one skill selected, and no duplicate skills");
		actionResult.setHelpDesc("Employees must have at least one skill selected to be considered for positions in the Scheduling process. The current fix that is used in the system is that there is a no_skill skill created and assigned to employees. If there is no skill assigned to employees, an error will be generated but the script will not halt execution. Finally, no employee should have a skill selected more than once.");
		actionResult.setErrorMsg("WARNING: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.2...");

		if( allEmployees.size() == 0 )
	        result = "No Employees found at this location\n";

		PreparedStatement stmt = null;
		ResultSet rs = null;

		for( Iterator iter = allEmployees.iterator(); iter.hasNext(); )
		{
			try {
				Employee employee = ( Employee )iter.next();
				if (employee.getEmpId() != -3){

					String strSelect = "select * from ST_EMP_SKILL where EMP_ID = ? ";

					stmt = conn.prepareStatement( strSelect );
					stmt.setInt(1 , employee.getEmpId() ) ;
					rs = stmt.executeQuery();

					if( !rs.next() )
						result += "-No skills selected for " + getEmpInfo( conn, cm, employee.getEmpId() ) + "!\n";

					strSelect = "SELECT * ";
					strSelect += "FROM (SELECT COUNT(ES.STSKL_ID) AS C, ES.EMP_ID, S.STSKL_NAME ";
					strSelect += "FROM ST_EMP_SKILL ES, ST_SKILL S ";
					strSelect += "WHERE ES.EMP_ID = ? AND ES.STSKL_ID = S.STSKL_ID ";
					strSelect += "GROUP BY S.STSKL_NAME, ES.EMP_ID) ";
					strSelect += "WHERE C >= 2 ";

					stmt = conn.prepareStatement( strSelect );
					stmt.setInt(1 , employee.getEmpId() ) ;
					rs = stmt.executeQuery();

					while(rs.next())
						result += "-Employee " + getEmpInfo( conn, cm, employee.getEmpId() ) + " has skill " + rs.getString("STSKL_NAME") + " defined " + rs.getInt("C") + " times!\n";

				} else {
	                if (logger.isDebugEnabled()){
	                	logger.debug("Workbrain Admin should not be in scheduled group");
	                }
					result += ", Workbrain Admin Should not be in scheduled group";
	            }
			}
			catch(RetailException e) {
				result = e.getMessage();
			}
			catch(SQLException e) {
				result = e.getMessage();
			}

		    finally
		    {
		        SQLHelper.cleanUp(rs);
		        SQLHelper.cleanUp(stmt);
		    }
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.2");

		actionResult.setMessage(result);
		return actionResult;
	}

//	declare check 4.1.3:
	public ErrorDetectionScriptResult action4_1_3() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employees with no Staff Group");
		actionResult.setHelpTip("Employees should be assigned to a staff group");
		actionResult.setHelpDesc("This simple test will allow for users of the LFSO system to see the employees that have not been added to a Staffing group, and therefore cannot and will not be shown on the schedule.");
		actionResult.setErrorMsg("FAILED: The following employee(s) have no Staff Group: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.3...");

		if(corpTree !=null){
			PreparedStatement stmt = null;
			ResultSet rs = null;

    		for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
    		{
    			try {
    				ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
    				int type = tempSkdgrpData.getSkdgrpIntrnlType();

    				if( InternalType.isDriver(new Integer(type)) &&
    						tempSkdgrpData.getSkdgrpVolsub() != -1 )
    				{

    					String strSelect = "select emp_id from so_employee where empgrp_id not in (select empgrp_id from so_employee_group) ";
    					strSelect += "and skdgrp_id = ? ";

    					stmt = conn.prepareStatement(strSelect);
                        stmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
    					rs = stmt.executeQuery();

    					while (rs.next())
    					{
    						int tempEmpId = rs.getInt( "emp_id" );

            			    if( result.length() == 0 )
            			        result += getEmpInfo( conn, cm, tempEmpId );
            			    else
            			        result += ", " + getEmpInfo( conn, cm, tempEmpId );
    					}
    				}
    			}
    			catch(SQLException e) {
    				result = e.getMessage();
    			}
    			catch(RetailException e) {
    				result = e.getMessage();
    			}

    		    finally
    		    {
    		        SQLHelper.cleanUp(rs);
    		        SQLHelper.cleanUp(stmt);
    		    }
        	}
    	}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.3");

		actionResult.setMessage(result);
		return actionResult;
	}

//	declare check 4.1.4:
	public ErrorDetectionScriptResult action4_1_4() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employees with more than one SO_EMPLOYEE record");
		actionResult.setHelpTip("Employees should only have one SO_EMPLOYEE record");
		actionResult.setHelpDesc("Such employees might cause the schedule to come up with empty employee/time slots.");
		actionResult.setErrorMsg("FATAL ERROR: The following employees have duplicate records:\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.4...");

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			String strSelect = "SELECT * FROM (";
			strSelect += "SELECT COUNT(SO_EMPLOYEE.EMP_ID) AS C, EMPLOYEE.EMP_FIRSTNAME, EMPLOYEE.EMP_LASTNAME ";
			strSelect += "FROM SO_EMPLOYEE JOIN EMPLOYEE ON SO_EMPLOYEE.EMP_ID = EMPLOYEE.EMP_ID ";
			strSelect += "GROUP BY SO_EMPLOYEE.EMP_ID, EMPLOYEE.EMP_FIRSTNAME, EMPLOYEE.EMP_LASTNAME";
			strSelect += ") WHERE C > 1";

			stmt = conn.prepareStatement(strSelect);
            rs = stmt.executeQuery();

			while (rs.next()) {
				result += rs.getInt("C") + " SO_EMPLOYEE records for " + rs.getString("EMP_FIRSTNAME") + " " + rs.getString("EMP_LASTNAME") + "\n";
			}
		}
		catch(SQLException e) {
			SQLHelper.cleanUp(rs);
			SQLHelper.cleanUp(stmt);
			result = "QUERY FAILED!\n" + e.getMessage();
		}
	    finally
	    {
	        SQLHelper.cleanUp(rs);
	        SQLHelper.cleanUp(stmt);
	    }

		if(result.compareTo("") == 0)
			result = "OK";
		else
			actionResult.setFatalError(true);

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.4");

		actionResult.setMessage(result);
		return actionResult;
	}

//	declare check 4.1.5:
	public ErrorDetectionScriptResult action4_1_5() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Store Hours of Operation with Missing Details");
		actionResult.setHelpTip("Lists stores missing hour of operation details for one or more days");
		actionResult.setHelpDesc("When trying to generate a forecast for a store, if you are linking a forecast driver to an hours of operation that is missing the details for a day or all days, then a null pointer stack trace error occurs");
		actionResult.setErrorMsg("ERROR:\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.5...");

		if(corpTree !=null){
    		for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
    		{
    			try {
    				CorporateEntity cE = CorporateEntity.getCorporateEntity(new Integer((String)iter.next()));

    				if(cE.getHoursOfOp() != null) {
    					HoursOfOperation locHop = cE.getHoursOfOp();
    					HashMap hopDetails = locHop.getHrSopDays();

    					for(int i = 1; i <= 7; i++) {
    						if(!hopDetails.containsKey(new Integer(i)))
    							result += "-Hours of operation set " + locHop.getHrsopName() + " does not contain an entry for day " + i + "!\n";
    					}
    				}
    			}
    			catch(RetailException e) {
    				result = e.getMessage();
    			}
        	}
    	}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.5");

		actionResult.setMessage(result);
		return actionResult;
	}

    /*
     * DECLARE FORECAST GENERATION CHECKS
     */

    //Declare FG check 4.2.1: Levsls
    public ErrorDetectionScriptResult action4_2_1() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Levels");
		actionResult.setHelpTip("Checks to ensure level requirements are met");
		actionResult.setHelpDesc("A user may mistakenly select a forecast driver's parent to be a schedule department or forecast driver level. This will not allow forecasts and/or schedules to generate properly (or at all).  This error will halt the execution of the script to avoid producing incorrect errors that may occur as a result.");
		actionResult.setErrorMsg("FAILED: The following locations have an invalid parent: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.1...");

		try {
			if(corpTree !=null)
		    {
		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		            ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));

		            // Ensure we're not at the very top level
		            if( tempSkdgrpData.getSkdgrpParentId() != null )
		            {
		                ScheduleGroupData parentSkdgrpData = ScheduleGroupData.getScheduleGroupData(tempSkdgrpData.getSkdgrpParentId());
		                if(!InternalType.isStore(new Integer(parentSkdgrpData.getSkdgrpIntrnlType())))
		                {
						    if( result.length() == 0 )
						        result += tempSkdgrpData.getSkdgrpName();
						    else
						        result += ", " + tempSkdgrpData.getSkdgrpName();
		                }
		            }
		        }
		    }
		}
		catch(RetailException e) {
			result = e.getMessage();
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.1");

		actionResult.setMessage(result);
		return actionResult;
	}

    //Declare FG check 4.2.2:
    public ErrorDetectionScriptResult action4_2_2() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Intervals");
		actionResult.setHelpTip("Checks to ensure all forecast intervals are same");
		actionResult.setHelpDesc("Typically, forecast data collected by POS systems will be collected at the same time interval across all departments for a store. The script should check to ensure that this is so in the database. If there are different intervals found the report should notify the user but not halt execution of the script.");
		actionResult.setErrorMsg("WARNING: The forecast interval for the following location(s) is different from the other locations witin the hierarchy: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.2...");

		try {
			int forecastInt = -1;
		    if(corpTree !=null)
		    {
		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		            ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));

		            if( forecastInt == -1 )
		                forecastInt = tempSkdgrpData.getSkdgrpFcastInt();

		            if( tempSkdgrpData.getSkdgrpFcastInt() != forecastInt )
		            {
					    if( result.length() == 0 )
					        result += tempSkdgrpData.getSkdgrpName();
					    else
					        result += ", " + tempSkdgrpData.getSkdgrpName();
		            }
		        }
		    }
		}
		catch(RetailException e) {
			result = e.getMessage();
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.2");

		actionResult.setMessage(result);
		return actionResult;
	}

//  Declare FG check 4.2.3:
    public ErrorDetectionScriptResult action4_2_3() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Start Day");
		actionResult.setHelpTip("Checks to ensure all locations start on the same start day of week");
		actionResult.setHelpDesc("Typically, forecast data collected by POS systems will be collected at the same time interval across all departments for a store. The script should check to ensure that this is so in the database. If there are different intervals found the report should notify the user but not halt execution of the script.");
		actionResult.setErrorMsg("FAILED: The Start Day for the following location(s) is different from the other locations witin the hierarchy: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.3...");

		try {
			int startDOW = -1;
		    if(corpTree !=null)
		    {
		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		        	ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));

		            if( startDOW == -1 )
		                startDOW = tempSkdgrpData.getSkdgrpStartdow();

		            if( tempSkdgrpData.getSkdgrpStartdow() != startDOW )
		            {
					    if( result.length() == 0 )
					        result += tempSkdgrpData.getSkdgrpName();
					    else
					        result += ", " + tempSkdgrpData.getSkdgrpName();
		            }
		        }
		    }
		}
		catch(RetailException e) {
			result = e.getMessage();
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.3");

		actionResult.setMessage(result);
		return actionResult;
	}

//  Declare FG check 4.2.4:
    public ErrorDetectionScriptResult action4_2_4() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Forecast Method");
		actionResult.setHelpTip("Checks forecast method consistency");
		actionResult.setHelpDesc("Forecast methods are typically uniform across sub-locations. Having different forecast methods may be valid, but the user should be warned if they are and not halt execution of the script.");
		actionResult.setErrorMsg("WARNING: The Forecast Method for the following location(s) is different from the other locations witin the hierarchy: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.4...");

		try {
			int forecastMtd = -1;
		    if(corpTree !=null)
		    {
		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		        	ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));

		            if( forecastMtd == -1 )
		                forecastMtd = tempSkdgrpData.getSkdgrpFcastMtd();

		            if( tempSkdgrpData.getSkdgrpFcastMtd() != forecastMtd )
		            {
					    if( result.length() == 0 )
					        result += tempSkdgrpData.getSkdgrpName();
					    else
					        result += ", " + tempSkdgrpData.getSkdgrpName();
		            }
		        }
		    }
	    }
	    catch(RetailException e) {
	    	result = e.getMessage();
	    }

	    if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.4");

		actionResult.setMessage(result);
		return actionResult;
	}

//  Declare FG check 4.2.5:

    /**
     * Code was missing from original LFSO EDS and did not make TT603 port
     *
     * @deprecated Check is no longer needed and has not been updated for 5.0
     */
    public ErrorDetectionScriptResult action4_2_5() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Volume Type and Variable Staffing Calculation");
		actionResult.setHelpTip("Correlation between Volume Type and Variable Staffing Calculation");
		actionResult.setHelpDesc("Typically, users will choose a Standard Volume per Hour for their Variable Staffing Calculation when their Volume Type is based on Revenue. Since this is not the only solution, violation of this correlation will not halt execution of the script and only display a warning to the user of a possible mismatch.");
		actionResult.setErrorMsg("");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.5...");

		result = "No longer necessary";

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.5");

		actionResult.setMessage(result);
		return actionResult;
	}

//  Declare FG check 4.2.6:
    public ErrorDetectionScriptResult action4_2_6() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Location Properties");
		actionResult.setHelpTip("Check the checkboxes associated to each location");
		actionResult.setHelpDesc("There is a set of 5 checkboxes on the Location Properties page. These must be set according to other items in the Location settings. For example, if the Volume Type is set to Revenue, the Sales Based Distribution checkbox should be activated.");
		actionResult.setErrorMsg("FAILED:\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.6...");

		try {
			if(corpTree !=null)
		    {
		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		            ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
					Integer type = new Integer(tempSkdgrpData.getSkdgrpIntrnlType());

		            if( InternalType.isStore(type) )
		            {
		                if(!( tempSkdgrpData.getSkdgrpVolsub() == 0 &&
		                    tempSkdgrpData.getSkdgrpHistdatSub() == 1 &&
		                    tempSkdgrpData.getSkdgrpOptchkskd() == 1 &&
		                    tempSkdgrpData.getSkdgrpSalesBased() == 1))
		                {
		                	result += "The following need to be checked/unchecked for store '" + tempSkdgrpData.getSkdgrpName() + "'\n";

		                    if(tempSkdgrpData.getSkdgrpVolsub() != 0)
		                    	result += "-'Volume is subset of parent volume' should be unchecked\n";
		                    if(tempSkdgrpData.getSkdgrpHistdatSub() != 1)
		                    	result += "-'Historical Data' should be checked\n";
		                    if(tempSkdgrpData.getSkdgrpOptchkskd() != 1)
		                    	result += "-'Check schedule assignments' should be checked\n";
		                    if(tempSkdgrpData.getSkdgrpSalesBased() != 1)
		                    	result += "-'Sales based distribution' should be checked\n";
		                }
		            }
		            else if( InternalType.isScheduleArea(type) )
		            {
		                if(!( tempSkdgrpData.getSkdgrpVolsub() == 1 &&
		                	tempSkdgrpData.getSkdgrpOptchkskd() == 0 &&
		                	tempSkdgrpData.getSkdgrpSalesBased() == 0 ))
		                {
                			result += "The following need to be checked/unchecked for schedule department '" + tempSkdgrpData.getSkdgrpName() + "'\n";

		                    if(tempSkdgrpData.getSkdgrpVolsub() != 1)
		                    	result += "-'Volume is subset of parent volume' should be checked\n";
		                    if(tempSkdgrpData.getSkdgrpOptchkskd() != 0)
		                    	result += "-'Check schedule assignments' should be unchecked\n";
		                    if(tempSkdgrpData.getSkdgrpSalesBased() != 0)
		                    	result += "-'Sales based distribution' should be unchecked\n";
		                }
		            }
		        }
		    }
		}
		catch(RetailException e) {
			result = e.getMessage();
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.6");

		actionResult.setMessage(result);
		return actionResult;
	}

//  Declare FG check 4.2.7:
    public ErrorDetectionScriptResult action4_2_7() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Duplicate POS");
		actionResult.setHelpTip("Check to ensure there is no duplicate entries in POS data");
		actionResult.setHelpDesc("Duplicate POS data will cause forecast numbers to be increased incorrectly. The engine aggregates all POS data for a particular time interval when it is doing its calculation, so duplicate entries will be added together. This method should look for duplicate entries for the same time period and location. If the numbers in the tables are the same (volume) for both entries, then it should report this error.");
		actionResult.setErrorMsg("FAILED:");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.7...");

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
	        String strSelect = "SELECT * ";
	        strSelect += "FROM  ";
	        strSelect += "    (SELECT COUNT(RESDET_VOLUME) AS C, RESDET_DATE, RESDET_TIME, SKDGRP_NAME, INVTYP_ID ";
	        strSelect += "    FROM SO_RESULTS_DETAIL RD, ";
	        strSelect += "        (SELECT SKDGRP_ID, SKDGRP_NAME  ";
	        strSelect += "        FROM SO_SCHEDULE_GROUP, WORKBRAIN_TEAM  ";
	        strSelect += "        WHERE WORKBRAIN_TEAM.WBT_ID = SO_SCHEDULE_GROUP.WBT_ID AND  ";
	        strSelect += "            SKDGRP_INTRNL_TYPE = 12 AND  ";
	        strSelect += "           SO_SCHEDULE_GROUP.WBT_ID IN  ";
	        strSelect += "                (SELECT CHILD_WBT_ID  ";
	        strSelect += "                FROM SEC_WB_TEAM_CHILD_PARENT  ";
	        strSelect += "                WHERE PARENT_WBT_ID =  ";
	        strSelect += "                        (SELECT WBT_ID  ";
	        strSelect += "                        FROM SO_SCHEDULE_GROUP  ";
	        strSelect += "                        WHERE SKDGRP_ID = ?))  ";
	        strSelect += "        ORDER BY WBT_LEVEL DESC) S ";
	        strSelect += "    WHERE S.SKDGRP_ID = RD.SKDGRP_ID AND ";
	        strSelect += "        RESDET_VOLUME > 0 ";
	        strSelect += "    GROUP BY RESDET_DATE, RESDET_TIME, SKDGRP_NAME, INVTYP_ID) ";
	        strSelect += "WHERE C > 1 ";

	        stmt = conn.prepareStatement( strSelect );

	        stmt.setInt(1, Integer.parseInt(SKDGRP_ID));

	        rs = stmt.executeQuery();

	        while( rs.next() )
			    result += "-Location " + rs.getString("SKDGRP_NAME") + " has " + rs.getInt("C") + " POS entries for " + rs.getDate("RESDET_DATE") + " at " + rs.getTime("RESDET_TIME") + "\n";
	    }
		catch(SQLException e) {
			result = e.getMessage();
		}
	    finally
	    {
	        SQLHelper.cleanUp(rs);
	        SQLHelper.cleanUp(stmt);
	    }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.7");

		actionResult.setMessage(result);
		return actionResult;
	}

//  Declare FG check 4.2.8:
    public ErrorDetectionScriptResult action4_2_8() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Fiscal Year Match");
		actionResult.setHelpTip("Fiscal Year date must be set so that the day of week matches the location start day of week");
		actionResult.setHelpDesc("If the Fiscal Year Date day of week is not the same as the day of week that is the start of locations, then there will be an offset problem when the forecast is being calculated for previous weeks. These have to be the same.");
		actionResult.setErrorMsg("FAILED:\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.8...");

		Integer thisYear = new Integer(Calendar.getInstance().getTime().getYear() + 1900);

		if(corpTree !=null){
			for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
    		{
    			try {
    				CorporateEntity cE = CorporateEntity.getCorporateEntity(new Integer((String)iter.next()));
    				ScheduleGroupData tempSkdgrpData = cE.getSkdgrpData();

    				//days of the week range from 1..7 in WB and in java
    				//1=sun and 7=sat in both systems
    				//so, we can do a direct comparison
					SODate startOfYear = cE.getFiscalYearStartDate(thisYear);
					int startOfWeek = tempSkdgrpData.getSkdgrpStartdow();

					if(startOfYear != null) {
						if(startOfYear.getDayOfWeek() != startOfWeek) {
							result += "-Start Day of Week does not match Fiscal Year Start Day of Week for location " + tempSkdgrpData.getSkdgrpName() + "\n";
						}
					}
					else {
						if(cE.getParent() != null)
							result += checkSOW(cE.getParent(), tempSkdgrpData);
						else
							result += "-No Fiscal Year Start Day of Week defined for location " + tempSkdgrpData.getSkdgrpName() + " and no parent defined for a parent check\n";
					}
				}
    			catch(RetailException e) {
    				result = e.getMessage();
    			}
    		}
        }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.8");

		actionResult.setMessage(result);
		return actionResult;
	}

//  Declare FG check 4.2.9:
    public ErrorDetectionScriptResult action4_2_9() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Hours of Op Effective Date");
		actionResult.setHelpTip("Hours of operation must exist for the previous year when using Trend of Historic Averages");
		actionResult.setHelpDesc("Hours of Operation have to be set for past dates when using Trend of Historic Averages. The forecast generation will look to these past dates for the Hours of Operation to determine the forecast values for those particular days. The forecast will not generate with this problem.");
		actionResult.setErrorMsg("FAILED: The following locations does not have hours of op set for previous year, or the date ranges are not continuous: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.9...");

			if(corpTree !=null)
		    {
				PreparedStatement stmt = null;
				ResultSet rs = null;

		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		        	try {
		        		ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));

			            // Forecast method is "Trend of Historic Averages"
			            if( tempSkdgrpData.getSkdgrpFcastMtd() == 10 )
			            {

						String strSelect = "SELECT MIN(CORPENTHR_FROMDATE) AS FROM_DATE, MAX(CORPENTHR_TODATE) AS TO_DATE ";
						strSelect += "FROM SO_CORP_ENT_HOUR ";
	                    strSelect += "WHERE SKDGRP_ID = ? ";
	                    strSelect += "GROUP BY SKDGRP_ID ";
						stmt = conn.prepareStatement(strSelect);
	                    stmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
						rs = stmt.executeQuery();
						while( rs.next() )
						{
	    					java.sql.Date fromDate = rs.getDate( "FROM_DATE" );
	    					java.sql.Date toDate = rs.getDate( "TO_DATE" );
	    					java.util.Date currentDate = Calendar.getInstance().getTime();

	                        // Check if current date is between the defined date range
	    					if( DateHelper.isBetween( currentDate,
	    					    ( java.util.Date )fromDate,
	    					    ( java.util.Date )toDate ) )
	    				    {
	    				        if( DateHelper.getWeeksBetween( ( java.util.Date )fromDate, currentDate ) >= 60 )
	    				        {
	    				            java.sql.Date tempFromDate = null;
	    				            java.sql.Date tempToDate = null;

	                                String tempSelect = "SELECT * FROM SO_CORP_ENT_HOUR WHERE SKDGRP_ID = ?";
	                                PreparedStatement tempStmt = conn.prepareStatement( tempSelect );
	                                tempStmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
	                                ResultSet tempRS = tempStmt.executeQuery();

	            					while( tempRS.next() )
	            					{
	                					if( tempFromDate == null )
	                					{
	                					    tempToDate = rs.getDate( "CORPENTHR_TODATE" );
	                					}
	                					else
	                					{
	                    					tempFromDate = rs.getDate( "CORPENTHR_FROMDATE" );

	                    					if( DateHelper.getDifferenceInDays(
	                    					        ( java.util.Date ) tempToDate,
	                    					        ( java.util.Date ) tempFromDate ) != 1 ||
	                    					    DateHelper.compare(
	                    					        ( java.util.Date )tempToDate,
	                    					        ( java.util.Date )tempFromDate ) >= 0 )
	                    					{
	                                		    if( result.length() == 0 )
	                                		        result += tempSkdgrpData.getSkdgrpName();
	                                		    else
	                                		        result += ", " + tempSkdgrpData.getSkdgrpName();
	                        					//result += "FAILED: The date ranges are either not continuous or overlapping.";
	                    					}
	                					}
	                                }
	    				        }
	    				        else
	    				        {
	                    		    if( result.length() == 0 )
	                    		        result += tempSkdgrpData.getSkdgrpName();
	                    		    else
	                    		        result += ", " + tempSkdgrpData.getSkdgrpName();
	            					//result += "FAILED: The 'FROM' date is less than 60 weeks from the current date.";
	    				        }
	    				    }
	    				    else
	    				    {
	                		    if( result.length() == 0 )
	                		        result += tempSkdgrpData.getSkdgrpName();
	                		    else
	                		        result += ", " + tempSkdgrpData.getSkdgrpName();
	        					//result += "FAILED: Current date is outside of defined Hours of Operations date range.";
	    				    }
	    				}
		            }
	        	}
				catch(RetailException e) {
					result = e.getMessage();
				}
				catch(SQLException e) {
					result = e.getMessage();
				}
			    finally
			    {
			        SQLHelper.cleanUp(rs);
			        SQLHelper.cleanUp(stmt);
			    }
            }
        }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.9");

		actionResult.setMessage(result);
		return actionResult;
	}

//  Declare FG check 4.2.10:
    public ErrorDetectionScriptResult action4_2_10() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Non-zero forecast and distribution");
		actionResult.setHelpTip("This check will ensure that the distribution and forecast being used has non-zero entries");
		actionResult.setHelpDesc("No forecast or distribution should have all zero entries. This is obviously an oversight and needs to be corrected");
		actionResult.setErrorMsg("FAILED: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.10...");

		Vector forecastDetails;
	    Vector distributionDetails;
	    Integer forecastID = null;
	    Forecast forecast;
	    ForecastDetail fd;
	    Distribution distribution;
	    DistributionDetail dd;

	    boolean ddVolumeAllZero = true;
	    boolean fdVolumeAllZero = true;

	    if(corpTree !=null)
	    {
	    	PreparedStatement stmt = null;
			ResultSet rs = null;

	        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
	        {
	        	try {
	        		CorporateEntity cE = CorporateEntity.getCorporateEntity(new Integer((String)iter.next()));
	            	ScheduleGroupData tempSkdgrpData = cE.getSkdgrpData();

					String qs = "SELECT FCAST_ID FROM SO_FCAST WHERE SKDGRP_ID = " + tempSkdgrpData.getSkdgrpId().intValue();
	                stmt = conn.prepareStatement( qs );

	                rs = stmt.executeQuery();
					if(rs.next()) {

						forecastID = new Integer(rs.getInt("FCAST_ID"));

			            if(forecastID != null) {
				            forecast = new Forecast(forecastID);
				            forecastDetails = (Vector)forecast.getDetailList();

				            for(int i = 0; i < forecastDetails.size(); i++) {
				            	fd = (ForecastDetail)forecastDetails.elementAt(i);
				            	distribution = fd.getDistribution();
				            	distributionDetails = (Vector)distribution.getDetailList(new SODate(fd.getFcastDate()));

				            	for(int j = 0; j < distributionDetails.size(); j++) {
				            		dd = (DistributionDetail)distributionDetails.elementAt(j);

				            		if(dd.getDistdetVolume() > 0) {
				            			ddVolumeAllZero = false;
				            			break;
				            		}
				            	}//end for j

				            	if(ddVolumeAllZero)
				            		result += "-Distribution for " + fd.getFcastDate().toString() + ", \"" + distribution.getDistName() + "\", is all zeros\n";

				            	if(fd.getAdjustedVolume().doubleValue() > 0.0) {
				            		fdVolumeAllZero = false;
				            		break;
				            	}

				            	ddVolumeAllZero = true;
				            	if(fdVolumeAllZero && forecastDetails.size() == i+1)
				            		result += "-Forecast \"" + forecast.getFcastName() + "\" is all zeros\n";
				            }//end for i

				        	fdVolumeAllZero = true;
			        	}//end if forecast ID not null
					}//end if rs.next()
		            else {
		        		result += "-No forecast associated with location \"" + tempSkdgrpData.getSkdgrpName() + "\"\n";
		        	}
	        	}//end try
	        	catch(SQLException e) {
	        		result = e.getMessage();
	        	}
	        	catch(RetailException e) {
	        		result = e.getMessage();
	        	}
	        	finally {
	        		SQLHelper.cleanUp(rs);
	    			SQLHelper.cleanUp(stmt);
	        	}
	        }//end for iter
	    }//end if org tree not null

	    if(result.compareTo("") == 0)
	    	result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.10");

		actionResult.setMessage(result);
		return actionResult;
	}

    /*
     * DECLARE THE SCHEDULE GENERATION CHECKS
     */

    //  declare check 4.3.1:

    /**
     * @deprecated Check is no longer needed and has not been updated for 5.0
     */
	public ErrorDetectionScriptResult action4_3_1() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employee Team Assignments");
		actionResult.setHelpTip("Ensures that each location has the appropriate teams assigned");
		actionResult.setHelpDesc("This will ensure that Security can be implemented from a Time and Attendance Team level. This error will not halt execution of script. It will display an error message to the user.");
		actionResult.setErrorMsg("FAILED:\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.1...");

		for(Iterator empIter = allEmployees.iterator(); empIter.hasNext();)
		{
			Employee employee = (Employee)empIter.next();
			if (logger.isDebugEnabled()){
					logger.debug("Checking Emp Full Time");
				}
			try {
				if (employee.getEmpId() != -3) {
		    			try {
		    				if (employee.getWBEmployee().getEmpFulltime() == null || employee.getWBEmployee().getEmpFulltime() == "")
		    				{
		    					if( result.length() == 0 )
		    						result += getEmpInfo( conn, cm, employee.getEmpId() );
		    					else
		    						result += ", " + getEmpInfo( conn, cm, employee.getEmpId() );
				        	}
			    		} catch (RetailException f){
		    				if (logger.isDebugEnabled()){
							 	logger.debug("No Employee Found");
		    				}
			    		}

				} else {
					if (logger.isDebugEnabled()){
		                          logger.debug("Workbrain Admin should not be in scheduled group");
		            }
					result += ", Workbrain Admin Should not be in scheduled group";
		        }
			}
			catch(RetailException e) {
				result = e.getMessage();
			}
    	}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.1");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.2:
	public ErrorDetectionScriptResult action4_3_2() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employee Availability");
		actionResult.setHelpTip("Ensures that employees have availability data loaded");
		actionResult.setHelpDesc("In order to be scheduled, employees must have availability loaded. This check will check all of the employees that are tied to the locations and can be scheduled to work. It will report the employees that do not have availability loaded. This error will not halt execution of script and only report a warning message.");
		actionResult.setErrorMsg("FAILED: The following employees does not have any Availability Data loaded: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.2...");

		for(Iterator empIter = allEmployees.iterator(); empIter.hasNext();)
		{
			try {
				Employee employee = ( Employee )empIter.next();
		        if (employee.getEmpId() != -3){
		        	AvailabilityAccess availAcc = new AvailabilityAccess( conn, cm );
				    List availData = availAcc.loadRecordData( "EMP_ID = " + employee.getEmpId() );

				    AvOvrAccess avOvrAcc = new AvOvrAccess( conn, cm );
				    List avOvrList = avOvrAcc.loadRecordData( "EMP_ID = " + employee.getEmpId() );

				    if( availData.size() == 0 && avOvrList.size() == 0)
				    {
				    	if( result.length() == 0 )
				    		result += getEmpInfo( conn, cm, employee.getEmpId() );
				        else
				            result += ", " + getEmpInfo( conn, cm, employee.getEmpId() );
				    }
		        } else {
		            if (logger.isDebugEnabled()){
		            	logger.debug("Workbrain Admin should not be in scheduled group");
		            }
		            result += ", Workbrain Admin Should not be in scheduled group";
		        }
			}
			catch(RetailException e) {
				result = e.getMessage();
			}
	    }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.2");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.3:
	public ErrorDetectionScriptResult action4_3_3() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Default Distribution");
		actionResult.setHelpTip("If forecast driver, check for default distribution (flag if flat)");
		actionResult.setHelpDesc("Typically a LFSO user will create distributions for their departments to use. There are times however when a Flat distribution is acceptable. The user will be notified which forecast drivers have Default Distributions set to Flat. This error will not halt execution of script and only report a warning message.");
		actionResult.setErrorMsg("WARNING: The following departments have the default distributation set to FLAT: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.3...");

		if(corpTree !=null)
	    {
	        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
	        {
	        	try {
		            ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
					int type = tempSkdgrpData.getSkdgrpIntrnlType();

		            if( InternalType.isDriver(new Integer(type)) )
		            {
		                if( tempSkdgrpData.getSkdgrpSunDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Sun)";
		                }
		                if( tempSkdgrpData.getSkdgrpMonDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Mon)";
		                }
		                if( tempSkdgrpData.getSkdgrpTueDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Tue)";
		                }
		                if( tempSkdgrpData.getSkdgrpWedDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Wed)";
		                }
		                if( tempSkdgrpData.getSkdgrpThuDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Thu)";
		                }
		                if( tempSkdgrpData.getSkdgrpFriDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Fri)";
		                }
		                if( tempSkdgrpData.getSkdgrpSatDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Sat)";
		                }
		            }
	        	}
	        	catch(RetailException e) {
	        		result = e.getMessage();
	        	}
	        }
	    }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.3");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.4:
	public ErrorDetectionScriptResult action4_3_4() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Staffing Requirements");
		actionResult.setHelpTip("Ensure there is same between schedule department and forecast drivers (jobs are listed)");
		actionResult.setHelpDesc("Jobs that are specified in the forecast driver’s Staffing Requirements section must be defined in one of the forecast driver’s Schedule department. Also, it should ensure that Schedule department jobs are defined in at least one of the forecast driver’s Staffing Requirements sections. Violations of this will result in an error displayed to the user but the script will not halt execution.");
		actionResult.setErrorMsg("WARNING:");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.4...");

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			String strSelect = "SELECT A.CSD_EFF_END_DATE AS GAP_START, B.CSD_EFF_START_DATE AS GAP_END, J.JOB_NAME, S.SKDGRP_NAME ";
			strSelect += "FROM SO_CLIENT_STFDEF A, SO_CLIENT_STFDEF B, JOB J, SO_SCHEDULE_GROUP S ";
			strSelect += "WHERE A.CSD_EFF_END_DATE < B.CSD_EFF_START_DATE AND ";
			strSelect += "A.JOB_ID = B.JOB_ID AND ";
			strSelect += "A.SKDGRP_ID = B.SKDGRP_ID AND ";
			strSelect += "S.SKDGRP_ID = A.SKDGRP_ID AND ";
			strSelect += "S.SKDGRP_ID IN (SELECT skdgrp_id FROM so_schedule_group, workbrain_team WHERE workbrain_team.wbt_id = so_schedule_group.wbt_id AND so_schedule_group.wbt_id IN (SELECT child_wbt_id FROM sec_wb_team_child_parent WHERE parent_wbt_id = (SELECT wbt_id FROM so_schedule_group WHERE skdgrp_id = ?)) ORDER BY wbt_level DESC) ";
			strSelect += "J.JOB_ID = A.JOB_ID ";

			stmt = conn.prepareStatement(strSelect);

			stmt.setInt(1, Integer.parseInt(SKDGRP_ID));

			rs = stmt.executeQuery();

			while(rs.next())
				result += "-There is a gap in staffing requirement " + rs.getString("JOB_NAME") + " at location " + rs.getString("SKDGRP_NAME") + " from " + rs.getDate("GAP_START") + " to " + rs.getDate("GAP_END") + "\n";
		}
		catch(SQLException e) {
			result = e.getMessage();
		}
		finally {
			SQLHelper.cleanUp(rs);
			SQLHelper.cleanUp(stmt);
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.4");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.5:
	public ErrorDetectionScriptResult action4_3_5() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Hours Of Operation");
		actionResult.setHelpTip("Check to ensure hours of operation are defined");
		actionResult.setHelpDesc("Hours of Operation have to be properly defined by each section. There are three possible scenarios that a user may select: Default Hours, Defined and Same as Parent.  If Default Hours is selected, then the script must ensure that the team has Hours of Operations specified.  If Defined is selected, then the script must ensure that hours were actually defined by the user.  If Same as Parent is selected, then the script must check to see that Parent (or higher if Parent has ‘Same as Parent’ selected) had hours defined.");
		actionResult.setErrorMsg("");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.5...");

		try {
			if(corpTree !=null){
				for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
				{
					ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));

		    		result = checkHOP( tempSkdgrpData, conn );
				}
		    }
		}
		catch(RetailException e) {
			result = e.getMessage();
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.5");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.6:
	public ErrorDetectionScriptResult action4_3_6() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employee Shift Patterns");
		actionResult.setHelpTip("Ensure that employees on fixed shift patterns have shifts specified");
		actionResult.setHelpDesc("Employees that are specified to work ‘fixed shifts’ must have fixed shifts defined to be properly accepted and scheduled by the system. The script will check for this requirement and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("FAILED: The following employee(s) have no assigned shifts for the 'Fixed Shift' option: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.6...");

		final int NOT_ASSIGNED = -1;
		final int NULL_ALL_OFFS1 = 0;
		final int NULL_ALL_OFFS2 = 10;

		for(Iterator empIter = allEmployees.iterator(); empIter.hasNext();)
		{
			try {
				Employee employee = ( Employee )empIter.next();
				if (employee.getEmpId() != -3) {
				     // Check if Employee is on fixed schedule
				     if( employee.getSempOnfixedSkd() == 1 )
				     {
				         com.workbrain.app.ta.db.EmployeeAccess empAcc = new com.workbrain.app.ta.db.EmployeeAccess( conn, cm );
				         com.workbrain.app.ta.model.EmployeeData empData = empAcc.loadRawData( employee.getEmpId() );

				         // Check if this employee have a valid fixed shift (not NULL OFFS)
				         if( empData.getShftpatId() == NOT_ASSIGNED ||
				             empData.getShftpatId() == NULL_ALL_OFFS1 ||
				             empData.getShftpatId() == NULL_ALL_OFFS2 )
				         {
					         if( result.length() == 0 )
					             result += getEmpInfo( conn, cm, employee.getEmpId() );
					         else
					             result += ", " + getEmpInfo( conn, cm, employee.getEmpId() );
					     }
				     }
		            } else {
		            	if (logger.isDebugEnabled()){
		                              logger.debug("Workbrain Admin should not be in scheduled group");
		                        }
						result += ", Workbrain Admin Should not be in scheduled group";
		            }
			}
			catch(RetailException e) {
				result = e.getMessage();
			}
	    }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.6");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.7:
	public ErrorDetectionScriptResult action4_3_7() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employee Preferred Job");
		actionResult.setHelpTip("Employee must have one and only one preferred job");
		actionResult.setHelpDesc("The system requires for there to be a selection of a preferred job for each employee. This must be checked to ensure that one and only one selection is made. The script will check for this requirement and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("WARNING: The following employee(s) should only have 1 and only 1 preferred job defined OR they have employee jobs with employee_job.empjob_preffered column as NULL (i.e should be set to 'N'): ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.7...");

		int iPrefPos = 0;
		Date preferredJobEndDate = null;

		for(Iterator iter = allEmployees.iterator(); iter.hasNext();)
		{
			try {
				iPrefPos = 0;
				Employee employee = (Employee)iter.next();
				if (employee.getEmpId() != -3) {
					for(Iterator posIter = employee.getEmployeeJobList().iterator(); posIter.hasNext();)
					{
						EmployeeJobData empJob = (EmployeeJobData)posIter.next();
						// *** null EmpjobPreferred causes problems
						if (StringHelper.isEmpty(empJob.getEmpjobPreferred())) {
							iPrefPos = 0;
						}
						else if(empJob.getEmpjobPreferred().equalsIgnoreCase("Y")) {
							if(preferredJobEndDate == null)
								iPrefPos++;
							else if(preferredJobEndDate.after(empJob.getEmpjobStartDate()))
								iPrefPos++;
							else //preferred jobs do not overlap
							{
//								do nothing
							}

							preferredJobEndDate = empJob.getEmpjobEndDate();
						}
					}
					if( iPrefPos != 1 )
					{
						if( result.length() == 0 )
							result += getEmpInfo( conn, cm, employee.getEmpId() );
						else
							result += ", " + getEmpInfo( conn, cm, employee.getEmpId() );
					}
				} else {
					if (logger.isDebugEnabled()){
						logger.debug("Workbrain Admin should not be in scheduled group");
                    }
                    result += ", Workbrain Admin Should not be in scheduled group";
             	}
			}
			catch(RetailException e) {
				result = e.getMessage();
			}
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.7");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.8:
	public ErrorDetectionScriptResult action4_3_8() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Job Rate Indexes");
		actionResult.setHelpTip("Ensure that Employee Job Rate Indexes can map to a job index in the job table");
		actionResult.setHelpDesc("The script will check consistency of this data and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.8...");

		int empjob_rate_index;
		java.util.Date empjob_start_date;
		java.util.Date empjob_end_date;
		String job_name;
		int jobrate_index;
		java.util.Date jobrate_effective_date;

		PreparedStatement stmt = null;
		ResultSet rs = null;

		for(Iterator empIter = allEmployees.iterator(); empIter.hasNext();)
		{
			try {
				Employee employee = (Employee)empIter.next();
				if (employee.getEmpId() != -3) {
					String strSelect = "select ej.EMPJOB_RATE_INDEX, ej.EMPJOB_START_DATE, ej.EMPJOB_END_DATE, ";
					strSelect += "j.job_name, j.JOB_START_DATE, j.job_end_date, jr.jobrate_index, jr.JOBRATE_EFFECTIVE_DATE ";
					strSelect += "from employee_job ej, job j, job_rate jr where ej.JOB_ID = j.JOB_ID and j.JOB_ID = jr.JOB_ID ";
					strSelect += "and ej.emp_id = ? " ;

					stmt = conn.prepareStatement(strSelect);
					stmt.setInt(1 , employee.getEmpId());
					rs = stmt.executeQuery();

					while (rs.next()) {
						empjob_rate_index = rs.getInt("EMPJOB_RATE_INDEX");
						empjob_start_date = rs.getDate("EMPJOB_START_DATE");
						empjob_end_date = rs.getDate("EMPJOB_END_DATE");

						job_name = rs.getString("JOB_NAME");
						//job_start_date = rs.getDate("JOB_START_DATE");
						//job_end_date = rs.getDate("JOB_END_DATE");

						jobrate_index = rs.getInt("JOBRATE_INDEX");
						jobrate_effective_date = rs.getDate("JOBRATE_EFFECTIVE_DATE");

						//check empjob_rate_index equals jobrate_index
						if (empjob_rate_index == -999)
							result += "Job " + job_name + " has a NULL empjob_rate_index for employee " + employee.getWBEmployee().getEmpName() + "\n";
						else if (empjob_rate_index != jobrate_index)
							result += "Job " + job_name + " for employee " + employee.getWBEmployee().getEmpName() + " does not have empjob_rate_index = jobrate_index\n";

						//check dates
						if (!empjob_start_date.equals(DateHelper.DATE_1900) || !empjob_end_date.equals(DateHelper.DATE_3000))
							result += "FAILED: job " + job_name + " for employee " + employee.getWBEmployee().getEmpName() + " may not be dated correctly (" + sdf.format(empjob_start_date) + " - " + sdf.format(empjob_end_date) + ")\n";
						else if (!jobrate_effective_date.equals(DateHelper.DATE_1900))
							result += "FAILED: job " + job_name + " for job rate index " + jobrate_index + " may not be dated correctly (" + sdf.format(jobrate_effective_date) + ")\n";
					}
				} else {
            		if (logger.isDebugEnabled())
            			logger.debug("Workbrain Admin should not be in scheduled group");

            		result += ", Workbrain Admin Should not be in scheduled group";
            	}
			}
			catch(SQLException e) {
				result = e.getMessage();
			}
			catch(RetailException e) {
				result = e.getMessage();
			}
		    finally
		    {
		        SQLHelper.cleanUp(rs);
		        SQLHelper.cleanUp(stmt);
		    }
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.8");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.9:
	public ErrorDetectionScriptResult action4_3_9() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Team Hours of Op");
		actionResult.setHelpTip("All teams must have hours of operation defined to prevent crashing");
		actionResult.setHelpDesc("Schedules will not generate if teams do not have hours of operation defined. These are used to ensure that the scheduled hours for employees are set within the hours of operations that the team has.");
		actionResult.setErrorMsg("FAILED: The following teams have no Hours of Operation defined: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.9...");

		if(corpTree !=null)
	    {
			PreparedStatement stmt = null;
			ResultSet rs = null;

	        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
	        {
	        	try {
	        		ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));

	                String strSelect = "SELECT wbt_name FROM workbrain_team WHERE hrsop_id is null AND wbt_id = ?"  ;
	                stmt = conn.prepareStatement( strSelect );
	                stmt.setInt(1 , tempSkdgrpData.getWbtId()) ;
	                rs = stmt.executeQuery();
	                if( rs.next() )
	                {
	    			    if( result.length() == 0 )
	    			        result += tempSkdgrpData.getSkdgrpName() + " - (WB Team: " + rs.getString(1)+ ")";
	    			    else
	    			        result += ", " + tempSkdgrpData.getSkdgrpName() + " - (WB Team: " + rs.getString(1)+ ")";
	                }
	   			}
				catch(SQLException e) {
					result = e.getMessage();
				}
				catch(RetailException e) {
					result = e.getMessage();
				}
			    finally
			    {
			        SQLHelper.cleanUp(rs);
			        SQLHelper.cleanUp(stmt);
			    }
			}
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.9");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.10:
	public ErrorDetectionScriptResult action4_3_10() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Shifts in Shift Patterns");
		actionResult.setHelpTip("Shift patterns must have shifts defined");
		actionResult.setHelpDesc("Empty shift patterns create errors in Schedule generation. The method must ensure that there are no empty shift Patterns assigned in the system that are linked to any of the employees in the Staffing Group selected by the user.");
		actionResult.setErrorMsg("FAILED: The following employee(s) does not have a shift pattern assigned: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.10...");

		for(Iterator empIter = allEmployees.iterator(); empIter.hasNext();)
		{
			try {
				Employee employee = ( Employee )empIter.next();
				if (employee.getEmpId() != -3) {
					com.workbrain.app.ta.db.EmployeeAccess empAcc = new com.workbrain.app.ta.db.EmployeeAccess( conn, cm );
					EmployeeData employeeData = empAcc.loadRawData( employee.getEmpId() );

					// Check if it's "NOT ASSIGNED" shift
					if( employeeData.getShftpatId() == -1 )
					{
						if( result.length() == 0 )
							result += getEmpInfo( conn, cm, employee.getEmpId() );
						else
							result += ", " + getEmpInfo( conn, cm, employee.getEmpId() );
					}
				} else {
	                if (logger.isDebugEnabled())
	                	logger.debug("Workbrain Admin should not be in scheduled group");

	                result += ", Workbrain Admin Should not be in scheduled group";
				}
			}
			catch(RetailException e) {
				result = e.getMessage();
			}
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.10");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.11:
	public ErrorDetectionScriptResult action4_3_11() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Productivity Numbers");
		actionResult.setHelpTip("Productivity numbers must be entered for Volume driven job requirements");
		actionResult.setHelpDesc("Productivity numbers have to exist for all jobs that are scheduled at the forecast driver Level that are Volume driven. These numbers are used to calculate the workload staffing requirements and cannot be 0.");
		actionResult.setErrorMsg("FAILED: The following jobs does not have volume productivity set for their respective location: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.11...");

		PreparedStatement skdgrpStmt = null;
		ResultSet skdgrpRs = null;

		try {
			String skdgrpQueryString = "SELECT SKDGRP_ID, SKDGRP_NAME ";
			skdgrpQueryString += "FROM SO_SCHEDULE_GROUP, WORKBRAIN_TEAM ";
			skdgrpQueryString += "WHERE WORKBRAIN_TEAM.WBT_ID = SO_SCHEDULE_GROUP.WBT_ID AND ";
			skdgrpQueryString += "    SKDGRP_INTRNL_TYPE = 12 AND ";
			skdgrpQueryString += "   SO_SCHEDULE_GROUP.WBT_ID IN ";
			skdgrpQueryString += "        (SELECT CHILD_WBT_ID ";
			skdgrpQueryString += "        FROM SEC_WB_TEAM_CHILD_PARENT ";
			skdgrpQueryString += "        WHERE PARENT_WBT_ID = ";
			skdgrpQueryString += "                (SELECT WBT_ID ";
			skdgrpQueryString += "                FROM SO_SCHEDULE_GROUP ";
			skdgrpQueryString += "                WHERE SKDGRP_ID = ?)) ";
			skdgrpQueryString += "ORDER BY WBT_LEVEL DESC";

			skdgrpStmt = conn.prepareStatement(skdgrpQueryString);
			skdgrpStmt.setInt(1, Integer.parseInt(SKDGRP_ID));

			skdgrpRs = skdgrpStmt.executeQuery();

			PreparedStatement stmt = null;
			ResultSet rs = null;

			while(skdgrpRs.next())
			{
				try {
					ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer(skdgrpRs.getInt("SKDGRP_ID")));
					int type = tempSkdgrpData.getSkdgrpIntrnlType();

					if( InternalType.isDriver(new Integer(type)) &&
							tempSkdgrpData.getSkdgrpVolsub() != -1 )
					{
	//    					gets the volume productivity, VP, the job name, NAME, and the job id, ID, for each job underneath a given store.
						String strSelect = "SELECT G.SKDGRP_NAME SGNAME, V.WRKLD_STDVOL_HOUR VP, R.JOB_ID ID, J.JOB_NAME NAME ";
						strSelect += "FROM JOB J, SO_SCHEDULE_GROUP G, SO_CLIENT_STFDEF R, SO_VOLUME_WORKLOAD V ";
						strSelect += "WHERE R.CSD_ID = V.CSD_ID AND ";
						strSelect += "R.JOB_ID = J.JOB_ID AND ";
						strSelect += "V.SKDGRP_ID = G.SKDGRP_ID AND ";
						strSelect += "V.SKDGRP_ID = ? ";

						stmt = conn.prepareStatement(strSelect);
	                    stmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
						rs = stmt.executeQuery();

						while (rs.next())
						{
							int volProd = rs.getInt( "VP" );

							//check empjob_rate_index equals jobrate_index
							if( volProd == 0 )
							{
							    if( result.length() == 0 )
	            			        result += rs.getString("NAME") + "(" + rs.getString("SGNAME") + ")";
	            			    else
	            			        result += ", " + rs.getString("NAME") + "(" + rs.getString("SGNAME") + ")";
							}
						}
					}
				}
				catch(RetailException e) {
					result = e.getMessage();
				}
				catch(SQLException e) {
					result = e.getMessage();
				}
			    finally
			    {
			        SQLHelper.cleanUp(rs);
			        SQLHelper.cleanUp(stmt);
			    }
			}
		}
		catch(SQLException e) {
			result = e.getMessage();
		}
	    finally
	    {
	        SQLHelper.cleanUp(skdgrpRs);
	        SQLHelper.cleanUp(skdgrpStmt);
	    }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.11");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.12:
	public ErrorDetectionScriptResult action4_3_12() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Non Volume Start/End Times");
		actionResult.setHelpTip("Non Volume job requirements must have Start and End times entered");
		actionResult.setHelpDesc("Locations must specify in the requirements section the Start and End times for the Volume driven Jobs listed.");
		actionResult.setErrorMsg("FAILED: The following jobs does not have start/end time set for their respective location: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.12...");

		if(corpTree !=null){
			PreparedStatement stmt = null;
			ResultSet rs = null;

    		for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
    		{
    			try {
    				ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));

					String strSelect = "select skdgrp_id, job_id, csd_start_time, csd_end_time from so_client_stfdef where csd_nonvlm_flag = 1 and (csd_start_time is null or csd_end_time is null) ";
					strSelect += "and skdgrp_id = ?";

					stmt = conn.prepareStatement(strSelect);
                    stmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
					rs = stmt.executeQuery();

					while (rs.next())
					{
						int jobId = rs.getInt( "JOB_ID" );
					    JobData tempJobData = cm.getJobById( jobId );

					    if( result.length() == 0 )
        			        result += tempJobData.getJobName() + "(" + tempSkdgrpData.getSkdgrpName() + ")";
        			    else
        			        result += ", " + tempJobData.getJobName() + "(" + tempSkdgrpData.getSkdgrpName() + ")";
					}
				}
    			catch(RetailException e) {
    				result = e.getMessage();
    			}
    			catch(SQLException e) {
    				result = e.getMessage();
    			}
    		    finally
    		    {
    		        SQLHelper.cleanUp(rs);
    		        SQLHelper.cleanUp(stmt);
    		    }
    		}
        }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.12");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.13:
	public ErrorDetectionScriptResult action4_3_13() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Empty Staff Group");
		actionResult.setHelpTip("Staff groups should not be empty");
		actionResult.setHelpDesc("If a staff group is selected and the scheduling check task is run, an error must be displayed to tell the user that the staff group they have selected is empty. Even if one of the groups is empty, this error must be displayed.");
		actionResult.setErrorMsg("FAILED: The following Staff Group(s) is empty: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.13...");

		if(corpTree !=null){
			PreparedStatement stmt = null;
			ResultSet rs = null;

    		for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
    		{
    			try {
    				ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
    				int type = tempSkdgrpData.getSkdgrpIntrnlType();

    				if( InternalType.isDriver(new Integer(type)) &&
    						tempSkdgrpData.getSkdgrpVolsub() != -1 )
    				{

						String strSelect = "select empgrp_id, empgrp_name from so_employee_group where empgrp_id not in (select distinct(empgrp_id) from so_employee) ";
						strSelect += "and skdgrp_id = ?";

						stmt = conn.prepareStatement(strSelect);
	                    stmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
						rs = stmt.executeQuery();

						while (rs.next())
						{
							String empgrpName = rs.getString( "empgrp_name" );

	        			    if( result.length() == 0 )
	        			        result += empgrpName;
	        			    else
	        			        result += ", " + empgrpName;
						}
    				}
				}
    			catch(RetailException e) {
    				result = e.getMessage();
    			}
    			catch(SQLException e) {
    				result = e.getMessage();
    			}
    		    finally
    		    {
    		        SQLHelper.cleanUp(rs);
    		        SQLHelper.cleanUp(stmt);
    		    }
    		}
        }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.13");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.14:
	public ErrorDetectionScriptResult action4_3_14() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Volume Productivity Too Low");
		actionResult.setHelpTip("For each job associated with a store's forecast driver, the store's forecasted revenue divided by the volume productivity for each job should be below 250.");
		actionResult.setHelpDesc("If the required number of shifts (forecasted revenue divided by volume productivity) exceeds 250, a warning is issued to note a potential problem with volume productivity being too low.");
		actionResult.setErrorMsg("FAILED: The following warning(s) have been produced:\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.14...");

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			//gets the schedule ID and forecast ID for any schedule of store type having a forecast for it
			String strSelect = "SELECT S.SKDGRP_ID, F.FCAST_ID, F.FCAST_NAME FNAME FROM (SELECT skdgrp_id FROM so_schedule_group, workbrain_team WHERE workbrain_team.wbt_id = so_schedule_group.wbt_id AND skdgrp_intrnl_type = 12 AND so_schedule_group.wbt_id IN (SELECT child_wbt_id FROM sec_wb_team_child_parent WHERE parent_wbt_id = (SELECT wbt_id FROM so_schedule_group WHERE skdgrp_id = ?)) ORDER BY wbt_level DESC) S JOIN SO_FCAST F ON (S.SKDGRP_ID = F.SKDGRP_ID)";
			stmt = conn.prepareStatement(strSelect);
            stmt.setInt(1, Integer.parseInt(SKDGRP_ID));

			rs = stmt.executeQuery();

			while(rs.next()) {
				int skdgrp_id = rs.getInt("SKDGRP_ID");
				int fcast_id = rs.getInt("FCAST_ID");
				Vector fd = (Vector)(new Forecast(new Integer(fcast_id))).getDetailList();

				//gets the volume productivity, VP, the job name, NAME, and the job id, ID, for each job underneath a given store.
				strSelect = "SELECT G.SKDGRP_NAME SGNAME, V.WRKLD_STDVOL_HOUR VP, R.JOB_ID ID, J.JOB_NAME NAME ";
				strSelect += "FROM JOB J, SO_SCHEDULE_GROUP G, SO_CLIENT_STFDEF R, SO_VOLUME_WORKLOAD V ";
				strSelect += "WHERE R.CSD_ID = V.CSD_ID AND ";
				strSelect += "R.JOB_ID = J.JOB_ID AND ";
				strSelect += "V.SKDGRP_ID = G.SKDGRP_ID AND ";
				strSelect += "V.SKDGRP_ID = ? ";
				PreparedStatement stmt2 = conn.prepareStatement(strSelect);
                stmt2.setInt(1, skdgrp_id);
                ResultSet jobs = stmt2.executeQuery();

            	while(jobs.next()) {
            		if(jobs.getInt("VP") == 0)
            			result += "-WARNING: Job \"" + jobs.getString("NAME") + "\" for location \"" + jobs.getString("SGNAME") + "\" has a volume productivity of 0! Skipping this location...\n";
            		else {
            			for(int i = 0; i < fd.size(); i++) {
            				ForecastDetail currentDetail = ((ForecastDetail)fd.elementAt(i));

            				if((currentDetail.getFcastCalls() + currentDetail.getFcastAdjval()) / (jobs.getInt("VP")*1.0) > 250)
            					result += "-WARNING: Volume productivity for job \"" + jobs.getString("NAME") + "\" for location \"" + jobs.getString("SGNAME") + "\" is too low for forecast \"" + rs.getString("FNAME") + "\" on " + currentDetail.getFcastDate() + " (" + (currentDetail.getFcastCalls() + currentDetail.getFcastAdjval()) / jobs.getInt("VP") + " > 250).\n";
            			}//end for i=0..fd.size
            		}//end else
            	}//end while jobs.next
        	}//end while rs.next
		}
		catch(SQLException e) {
			result += e.getMessage() + "\n";
		}
		catch(RetailException e) {
			result += e.getMessage() + "\n";
		}
	    finally
	    {
	        SQLHelper.cleanUp(rs);
	        SQLHelper.cleanUp(stmt);
	    }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.14");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.15:
	public ErrorDetectionScriptResult action4_3_15() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Job Team Definitions");
		actionResult.setHelpTip("Each job used for scheduling must have a job team associated with it that defines such things as the overtime thresholds. Missing definitions will cause stack trace errors.");
		actionResult.setHelpDesc("Each job used for scheduling must have a job team associated with it that defines such things as the overtime thresholds. Missing definitions will cause stack trace errors.");
		actionResult.setErrorMsg("FAILED: The following warning(s) have been produced:\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.15...");

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			String strSelect = "SELECT JOB_ID, JOB_NAME FROM JOB WHERE JOB_ID NOT IN (SELECT JOB.JOB_ID FROM JOB JOIN JOB_TEAM ON JOB.JOB_ID=JOB_TEAM.JOB_ID) AND JOB_ID >= 10000 ORDER BY JOB_ID";
			stmt = conn.prepareStatement(strSelect);
            rs = stmt.executeQuery();

        	while(rs.next()) {
        		result += "WARNING: Job \"" + rs.getString("JOB_NAME") + "\" does not have a job team record.\n";
        	}
		}
		catch(SQLException e) {
			logger.error(e.getMessage());
		}
	    finally
	    {
	        SQLHelper.cleanUp(rs);
	        SQLHelper.cleanUp(stmt);
	    }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.15");

		actionResult.setMessage(result);
		return actionResult;
	}

//  declare check 4.3.16:
	public ErrorDetectionScriptResult action4_3_16() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Workbrain Team Structure Match");
		actionResult.setHelpTip("Workbrain Team and location structures should always match all the way down ideally");
		actionResult.setHelpDesc("Workbrain Team and location structures should always match all the way down ideally");
		actionResult.setErrorMsg("FAILED: The following location(s) have to be revisited: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.16...");

		if(corpTree !=null)
	    {
			WorkbrainTeamAccess teamAccess = new WorkbrainTeamAccess(conn);

			for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
	        {
	        	try {
	        		ScheduleGroupData loc = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));

		            if(loc.getSkdgrpParentId() != null) {
						ScheduleGroupData parentLoc = ScheduleGroupData.getScheduleGroupData(loc.getSkdgrpParentId());

						int locTeamID = loc.getWbtId();
						int parentLocTeamID = parentLoc.getWbtId();
						WorkbrainTeamData locTeam = teamAccess.load(locTeamID);
						int parentTeamTeamID = locTeam.getWbtParentId();

						if(!(parentLocTeamID == parentTeamTeamID))
							result += "-WARNING: Location/Team structure does not match at location \"" + loc.getSkdgrpName() + "\" with team \"" + locTeam.getWbtName() + "\"\n";
			        }//end if get parent is not null
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
			logger.debug("Finished check 4.3.16");

		actionResult.setMessage(result);
		return actionResult;
	}

//	declare check 4.3.17:
	//added for TT502
	/**
	 * @deprecated No longer meaningful in WB 5.0 as jobs are not held at the driver level
	 */
	public ErrorDetectionScriptResult action4_3_17() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Matching Jobs");
		actionResult.setHelpTip("Jobs entered in a schedule departmentmust appear in a child forecast driver");
		actionResult.setHelpDesc("Each volume driven staffing requirement that is entered in a schedule department must appear in at least one of the child forecast drivers.");
		actionResult.setErrorMsg("FAILED: The following errors were produced\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.1...");

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			String strSelect = "SELECT SKDGRP_NAME, JOB_NAME FROM JOB J JOIN (SELECT SKDGRP_NAME, JOB FROM SO_SCHEDULE_GROUP S JOIN ((SELECT SKDGRP_ID ID, JOB_ID JOB FROM SO_CLIENT_STFDEF WHERE CSD_NONVLM_FLAG = 0 AND SKDGRP_ID IN (SELECT SKDGRP_ID FROM SO_SCHEDULE_GROUP WHERE SKDGRP_INTRNL_TYPE = 11)) MINUS (SELECT G.SKDGRP_PARENT_ID ID, JOB_ID JOB FROM SO_CLIENT_STFDEF S JOIN SO_SCHEDULE_GROUP G ON (S.SKDGRP_ID = G.SKDGRP_ID) WHERE CSD_NONVLM_FLAG = 0 AND S.SKDGRP_ID IN (SELECT SKDGRP_ID FROM SO_SCHEDULE_GROUP WHERE SKDGRP_PARENT_ID IN (SELECT SKDGRP_ID FROM SO_SCHEDULE_GROUP WHERE SKDGRP_INTRNL_TYPE = 11)))) X ON (S.SKDGRP_ID = X.ID)) Y ON (J.JOB_ID = Y.JOB)";
			stmt = conn.prepareStatement(strSelect);
	        rs = stmt.executeQuery();

	        while (rs.next())
	        {
	            result += "ERROR: job \"" + rs.getString("JOB_NAME") + "\" for schedule group \"" + rs.getString("SKDGRP_NAME") + "\" is not in a child forecast driver\n";
	        }
	    }
		catch(SQLException e) {
			result = e.getMessage();
		}
	    finally
	    {
	        SQLHelper.cleanUp(rs);
	        SQLHelper.cleanUp(stmt);
	    }

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.17");

		actionResult.setMessage(result);
		return actionResult;
	}

//	declare check 4.3.18:
	//added for TT607
	public ErrorDetectionScriptResult action4_3_18() {
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
				        	else if(determineGroupRulesToApply(rs.getInt("EMP_ID"), skdgrpID, Calendar.getInstance().getTime()) == DEFAULT_EMP_GROUP_ID)
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

    /*
     * DECLARE THE SYSTEM CHECKS
     */

    //Declare System Check 4.4.1: Check mosel attributes
    public ErrorDetectionScriptResult action4_4_1() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Temp Files Directory");
		actionResult.setHelpTip("Directory for temp files on application server not present / out of space");
		actionResult.setHelpDesc("A system check will be done to ensure that these directories exist and that the server is not out of hard drive space that is required to run the Schedule Optimizer. The script will check for this requirement and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("FAILED: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.4.1...");

		ScheduleGroupData skdgrpData = new ScheduleGroupData();
		try {
			skdgrpData = ScheduleGroupData.getScheduleGroupData(1);
		}
		catch(RetailException e) {
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());

			result += e.getMessage() + "\n";
		}

		if(moselClass.equals("com.workbrain.app.modules.retailSchedule.services.SingleServerExecute")) {
			String modelName = getBaseDirectory(moselClass) + "/models/" + skdgrpData.getSkdgrpScriptName() +".bim";
	        String tempDir = getBaseDirectory(moselClass) + "/temp";

	        try {
				Process proc = Runtime.getRuntime().exec("mosel");
				proc.destroy() ;
			}
			catch(IOException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());

				result += "Mosel command does not exist on system PATH\n";
			}

    		java.io.File f = new java.io.File(modelName);
    		if(!f.exists()) {
    			result += "File " + modelName + " does not exist\n";
    		}
    		if(!f.canWrite()) {
    			if(!f.exists()) {
    				result += "File " + modelName + " does not have writable permissions\n";
    			}
    			else {
    				result += "File " + modelName + " does not have writable permissions\n";
    			}
    		}

	    	f = new java.io.File(tempDir);
	    	if(!f.exists()) {
	    		result += "Directory " + tempDir + " does not exist\n";
	    	}
		}

		else if(moselClass.equals("com.workbrain.app.modules.retailSchedule.services.RemoteServerExecute")) {
			try {
				String strDestHost = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_HOST");
				String strDestHomeFolder = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_HOME_FOLDER");
				String strDestUName = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_USER_NAME");
				String strDestPwd = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_PASS");

		        //String tempDir = PreProcessorUtils.getBaseDirectory(moselClass) + "/temp";
		        //String tempDir = getBaseDirectory(moselClass) + "/temp";
		        String modelName = strDestHomeFolder + "/models/" + skdgrpData.getSkdgrpScriptName() +".bim";

				SshClient ssh = new SshClient();
				SshConnectionProperties properties = new SshConnectionProperties();
				properties.setHost(strDestHost);

				//Connect to the host
				ssh.connect(properties, new IgnoreHostKeyVerification());

				//Create a password authentication instance
				PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();

				//set user name
				pwd.setUsername(strDestUName);

				//Get the users password from the gatework.xml file
				pwd.setPassword(strDestPwd);

				//Try the authentication
				int xresult = ssh.authenticate(pwd);

				//Evaluate the result
				if(xresult == AuthenticationProtocolState.COMPLETE) {
					//removed check of mosel executable
					SftpClient sftp = ssh.openSftpClient();

	    			try {
		    			sftp.cd(strDestHomeFolder + "/models/");
		    			FileAttributes fa = sftp.stat(skdgrpData.getSkdgrpScriptName() +".bim");
		    			String perm = fa.getPermissionsString();
						//"-rwxrwxrwx"
		    			if(perm.charAt(1)!='r') {
	    					result += "File " + modelName + " does not have writable permissions\n";
		    			}
		    		}
		    		catch(IOException e) {
		    			if(logger.isDebugEnabled())
		    				logger.debug(e.getStackTrace());

		    			result += "Error " + e.getMessage() + " when checking for file " + modelName +"\n";
		    		}

		    		try {
		    			sftp.cd(strDestHomeFolder + "/temp/");
		    		}
		    		catch(IOException e) {
		    			if(logger.isDebugEnabled())
		    				logger.debug(e.getStackTrace());

						result += e.getMessage() + "\n";
					}
				}
				else {	//AUTHENTICATION FAILED
					result += "Authentication Failed\n";
				}
			}
			catch(NamingException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());

				result += e.getMessage() + "\n";
			}
			catch(IOException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());

				result += e.getMessage() + "\n";
			}
		}

		if("".compareTo(result) == 0) {
			result = "OK";
		}

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.4.1");

		actionResult.setMessage(result);
		return actionResult;
	}

    //Declare system check 4.4.2: SO Table stats
    public ErrorDetectionScriptResult action4_4_2() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Mosel Code");
		actionResult.setHelpTip("Mosel code on remote machine is not present/incorrect");
		actionResult.setHelpDesc("Mosel is the Schedule Optimization engine that is responsible for generating the Schedules for LFSO. It runs on a remote machine so connections must be verified by this script. Also, it must be verified that the Mosel code is running properly. The script will check for this requirement and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("FAILED: ");

		String result = "";

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.4.2...");

		PreparedStatement stmt = null;
		ResultSet rs = null;

	    try {
			String strSelect = "select LAST_ANALYZED from user_tables where table_name like 'SO%'";
            stmt = conn.prepareStatement(strSelect);
            rs = stmt.executeQuery();
            if(!rs.next())
                result += "No information on DB statistics available";
        }
        catch(SQLException e) {
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());

        	result = e.getMessage();
        }
	    finally
	    {
	        SQLHelper.cleanUp(rs);
	        SQLHelper.cleanUp(stmt);
	    }

        if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.4.2");

		actionResult.setMessage(result);
		return actionResult;
	}

    //Declare system check 4.4.3: check PDF URI Directory
    public ErrorDetectionScriptResult action4_4_3() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Dash Machine Registry");
		actionResult.setHelpTip("Registry not set up/setup incorrectly for remote Dash machine");
		actionResult.setHelpDesc("A check of the Workbrain Registry and validate the path specified. The script will check for this requirement and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("FAILED: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.4.3...");

		try {
			java.io.File f = new java.io.File(pdfURI);
			if(!f.exists()) {
				result = "Directory " + pdfURI + " does not exist";
			}
		}
		catch(NullPointerException e) {
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());

			result = "Missing registry entry for SO_PDF_URI";
		}

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.4.3");

		actionResult.setMessage(result);
		return actionResult;
	}

    //Declare system check 4.4.4: check for DASH license
    public ErrorDetectionScriptResult action4_4_4() {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Dash License Check");
		actionResult.setHelpTip("Checks to see if the Dash server has a valid license");
		actionResult.setHelpDesc("Each Dash server must be licensed. This will check to see if the Dash server is configured with a valid Dash license");
		actionResult.setErrorMsg("FAILED:\n");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.4.4...");

		if(moselClass.equals("com.workbrain.app.modules.retailSchedule.services.SingleServerExecute")) {
			try {
				Process proc = Runtime.getRuntime().exec("mosel");
				BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
				out.write("exit");

				proc.waitFor();
				int status = proc.exitValue();

				if(status == 3)
					result += "-License Error: " + procReader.readLine() + "\n";
			}
			catch(InterruptedException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());

				result += "-License check failed: Wait interrupted!";
			}
			catch(IOException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());

				result += "-License check failed: Mosel command does not exist on system PATH\n";
			}
		}
		else if(moselClass.equals("com.workbrain.app.modules.retailSchedule.services.RemoteServerExecute")) {
			try {
				String strDestHost = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_HOST");
				//String strDestHomeFolder = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_HOME_FOLDER");
				String strDestUName = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_USER_NAME");
				String strDestPwd = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_PASS");

		        SshClient ssh = new SshClient();
				SshConnectionProperties properties = new SshConnectionProperties();
				properties.setHost(strDestHost);

				//Connect to the host
				ssh.connect(properties, new IgnoreHostKeyVerification());

				//Create a password authentication instance
				PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();

				//set user name
				pwd.setUsername(strDestUName);

				//Get the users password from the gatework.xml file
				pwd.setPassword(strDestPwd);

				//Try the authentication
				int xresult = ssh.authenticate(pwd);

				//Evaluate the result
				if(xresult == AuthenticationProtocolState.COMPLETE) {
					SessionChannelClient ssc = ssh.openSessionChannel();
					BufferedReader inStream = new BufferedReader(new InputStreamReader(ssc.getInputStream()));

					boolean execResult = ssc.executeCommand("cd scheduleOptimization");

					if(execResult) {
						execResult = ssc.executeCommand("java com.workbrain.app.modules.retailSchedule.services.MoselLicense -classpath wbscheduleoptimization.jar");
						result += "java invocation: " + execResult + " " + inStream.readLine() + "\n";

						if(execResult) {
							int exitCode = ssc.getExitCode().intValue();

							if(exitCode == 1)
								result += "-DASH License error. Check your license setup and validity!";
						}
						else
							result += "-Error invoking MoselLicense class. Please ensure you have the newest copy of \"wbscheduleoptimization.jar\" installed on your DASH server.\n";
					}
					else
						result += "-Error changing to \"scheduleOptimization\" directory. Improper setup on DASH server?\n";
				}
				else {	//AUTHENTICATION FAILED
					result += "-Error checking license: Authentication Failed. Please check your configuration in the Workbrain registry\n";
				}
			}
			catch(NamingException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());

				result += "-License check failed: " + e.getMessage() + "\n";
			}
			catch(IOException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());

				result += "-License check failed: " + e.getMessage() + "\n";
			}
		}
		else
			result += "-Error: Mosel execution type not set in the Workbrain registry!";

		if(result.compareTo("") == 0)
			result = "OK";

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.4.4");

		actionResult.setMessage(result);
		return actionResult;
	}

    private String getEmpInfo( DBConnection conn, CodeMapper cm, int empId )
    {
        // Standardize the Employee info to this format: Lastname, Firstname (EmpID)
	    if (logger.isDebugEnabled()){
			logger.debug("getEmpInfo() - Checking Emp ID: " + empId);
	    }
	    com.workbrain.app.ta.db.EmployeeAccess empAcc = new com.workbrain.app.ta.db.EmployeeAccess( conn, cm );
	    com.workbrain.app.ta.model.EmployeeData empData = empAcc.loadRawData( empId );
          String result = null;
	    if (empData == null) {
			result = "Employee Not Found (id: " +  empId+ ")";
	    }
          else {
		     result = empData.getEmpLastname() + ", " + empData.getEmpFirstname() + " (" + empId + ")";
          }
	    if (logger.isDebugEnabled()){
			logger.debug("getEmpInfo() - Found Emp Info: " + result);
	    }
		return result;
    }

//  Recursive method for 4.3.5
    private String checkHOP( ScheduleGroupData skdgrpData, DBConnection conn )
    {
        String tempResult = "";

	    try
	    {
    		if( skdgrpData.getSkdgrpHrsOpSrc() == HoursOfOperationType.HRSOP_DEFAULT.intValue() )
    		{
                String strSelect = "SELECT wbt_name FROM workbrain_team WHERE hrsop_id is null AND wbt_id = ?" ;
                PreparedStatement stmt = conn.prepareStatement( strSelect );
                stmt.setInt(1, skdgrpData.getWbtId());
                ResultSet rs = stmt.executeQuery();
                if( rs.next() )
                {
    				tempResult = "FAILED: " + skdgrpData.getSkdgrpName() + " with Workbrain team " + rs.getString(1) + " (default set) has no Hours of Operation defined.\n";
                }
    		}
    		else if( skdgrpData.getSkdgrpHrsOpSrc() == HoursOfOperationType.HRSOP_SETS.intValue() )
    		{
    		    String strSelect = "SELECT * FROM SO_CORP_ENT_HOUR WHERE SKDGRP_ID = ? " ;
                PreparedStatement stmt = conn.prepareStatement( strSelect  );
                stmt.setInt(1, skdgrpData.getWbtId());
                ResultSet rs = stmt.executeQuery();
                if( !rs.next() )
                {
    				tempResult = "FAILED: " + skdgrpData.getSkdgrpName() + " (defined set) has no Hours of Operation defined.\n";
                }
    		}
    		else if( skdgrpData.getSkdgrpHrsOpSrc() == HoursOfOperationType.HRSOP_INHERIT.intValue() )
    		{
    		    Integer parentID = skdgrpData.getSkdgrpParentId();
    		    if( parentID == null )
    		    {
    		        tempResult = "FAILED: " + skdgrpData.getSkdgrpName() + " has no parent while Hours of Operation is set to 'same as parent'.\n";
    		    }
    		    else
    		    {
                    CorporateEntity cE = CorporateEntity.getCorporateEntity( parentID );
                    ScheduleGroupData parentSkdgrpData = cE.getSkdgrpData();
                    tempResult = checkHOP( parentSkdgrpData, conn );
                }
    		}
        }
        catch( Exception e )
        {
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());

            tempResult = e.getMessage();
        }
		return tempResult;
    }

    private String getBaseDirectory(String classname)
    {
        String strRet = null;
        try
        {
            if (classname.indexOf("Remote")==-1)
                strRet = (String)Registry.getVar(MoselRegistryKeys.REGKEY_SO_BASE_DIR);
            else
                strRet = (String)Registry.getVar(MoselRegistryKeys.REGKEY_REMOTE_HOME_FOLDER);
        }
        catch (NamingException e)
        {
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());

            strRet = e.getMessage();
        }

        if (strRet == null || strRet.length() == 0)
        {
            strRet = "/scheduleOptimization";
        }

        return strRet;
    }

    private String checkSOW(CorporateEntity cE, ScheduleGroupData originalLocation) {
    	String result = "";
    	Integer thisYear = new Integer(Calendar.getInstance().getTime().getYear() + 1900);

    	try {
			ScheduleGroupData tempSkdgrpData = cE.getSkdgrpData();

			SODate startOfYear = cE.getFiscalYearStartDate(thisYear);
			int startOfWeek = tempSkdgrpData.getSkdgrpStartdow();

			if(startOfYear != null) {
				if(startOfYear.getDayOfWeek() != startOfWeek) {
					result += "-No Fiscal Year Start Day of Week defined for location " + originalLocation.getSkdgrpName() + " and Start Day of Week does not match Fiscal Year Start Day of Week for parent location " + tempSkdgrpData.getSkdgrpName() + "\n";
				}
			}
			else {
				if(cE.getParent() != null)
					result += checkSOW(cE.getParent(), originalLocation);
				else
					result += "-No Fiscal Year Start Day of Week defined for location " + originalLocation.getSkdgrpName() + " or for any parent location\n";
			}
		}
		catch(RetailException e) {
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());

			result = e.getMessage();
		}

		return result;
    }

    private int determineGroupRulesToApply(int empID, int skdgrpID, Date targetDate) throws RetailException {
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