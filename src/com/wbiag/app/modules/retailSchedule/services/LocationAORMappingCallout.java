package com.wbiag.app.modules.retailSchedule.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
//import java.util.List;

import javax.naming.NamingException;

import org.apache.log4j.Logger;

//import com.wbiag.app.modules.retailSchedule.services.CompressibleTaskScheduleCallout;
//import com.wbiag.app.modules.retailSchedule.services.model.CompWorkFactorMoselData;
//import com.wbiag.app.modules.retailSchedule.services.model.CompWorkFactorMoselData;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.EmployeeGroupAccess;
import com.workbrain.app.modules.retailSchedule.db.RuleGroupAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.exceptions.SQLRetailException;
import com.workbrain.app.modules.retailSchedule.model.EmployeeGroup;
import com.workbrain.app.modules.retailSchedule.model.RuleGroup;
import com.workbrain.app.modules.retailSchedule.model.ScheduleEmployeeGroup;
import com.workbrain.app.modules.retailSchedule.model.StaffRule;
import com.workbrain.app.modules.retailSchedule.services.FileManager;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;
import com.workbrain.app.modules.retailSchedule.services.rules.RulesFile;
import com.workbrain.app.modules.retailSchedule.type.InternalType;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.NestedRuntimeException;
import com.workbrain.util.callouts.BudgetCallout;
import com.workbrain.util.callouts.BudgetContext;

public class LocationAORMappingCallout {
	//set the locations of the temp files relative to the SO base dir
	private static final String MAP_FILE_LOCATION = "_A_DeptAOR";
	//private static final String BUDGET_FILE_LOCATION = "TempSchd_LocationBudget.txt";
	
	private static Logger logger = Logger.getLogger(LocationAORMappingCallout.class);
	
	//variables to hold the registry key names
	public static final String NOT_SUBTRACT_PTO_REGISTRY_KEY = "system/WORKBRAIN_PARAMETERS/SO_BUDGET_BY_AOR_NOT_SUBTRACT_PTO";
	public static final String SO_BASE_DIR_REGISTRY_KEY = "SO_BASE_DIR";
	
	//variables to hold ID's
	private static final int NOT_EXCEED_BUDGET_RULE_ID = 20;
	private static final String BUDGET_BY_AOR_MOSEL_RULE_ID = "1350";
	private static final int AOR_LOCATION_PROPERTY_ID = -1000000;
	
	//file writing values
	private static final String DELIMITER = ",";
    private static final String END_OF_LINE = "\n";
	
	private static DBConnection conn;
	
	public static boolean appendRules(SOData soContext) {
		boolean returnCode = false;
	        
        if(logger.isDebugEnabled())
        	logger.debug("appending rules file");
        
        try {
        	conn = soContext.getDBconnection();
        	
        	Vector empGroupList = soContext.getScheduleEmployeeGroups();
            ScheduleEmployeeGroup skdEmpGroup = null;
            //EmployeeGroupAccess ega = new EmployeeGroupAccess(soContext.getDBconnection());
            EmployeeGroup empGroup = new EmployeeGroup();
            RuleGroupAccess rga = new RuleGroupAccess(conn);
            String ruleGroupId = "";
            
	        //write the employee groups to a file
	        RulesFile file = RulesFile.getRulesFile(soContext);
	        
	        if(logger.isDebugEnabled())
	        	logger.debug("Employee Group List size is " + empGroupList.size());
	        
	        CodeMapper cm = null;
	        
	        try {
	        	cm = CodeMapper.createCodeMapper(conn);
	        }
	        catch(SQLException e) {
	        	throw new NestedRuntimeException(e);
	        }
	        
	        //Write a rule entry for each staff group in the staff rule file
            for (int i = 0; i<empGroupList.size(); i++) {
            	skdEmpGroup = (ScheduleEmployeeGroup)(empGroupList.elementAt(i));
                empGroup = cm.getSOEmployeeGroupById(skdEmpGroup.getEmpgrpId().intValue());
                ruleGroupId = "" + empGroup.getRulegrpId();
                
                RuleGroup rg = rga.load(empGroup.getRulegrpId());
                Vector rules = rg.getStaffRulesList();
                
                if(logger.isDebugEnabled())
                	logger.debug("rules.size = " + rules.size());
                
                for(int j = 0; j < rules.size(); j++) {
                	StaffRule sr = (StaffRule)rules.elementAt(j);
                	
                	if(logger.isDebugEnabled())
                		logger.debug("sr.getRulId = " + sr.getRulId());
                	
                	if(sr.getRulId().intValue() == NOT_EXCEED_BUDGET_RULE_ID) {
                		file.writeRule(BUDGET_BY_AOR_MOSEL_RULE_ID, ruleGroupId, "1", "", "", "1");
                		
                		if(logger.isDebugEnabled())
        	        		logger.debug("Wrote line to the rule file: '" + BUDGET_BY_AOR_MOSEL_RULE_ID + "," + ruleGroupId + ", \"1\", \"\", \"\", \"1\"" + "'");
                		
                		break;
                	}
                }//end for rules.size
	        }//end for empgrouplist.size
	        
	        returnCode = true;
        }
        catch(SQLRetailException e) {
        	logger.error("Could not get connection: " + e.getMessage());
        	throw new NestedRuntimeException(e);
        }
        catch(RetailException e) {
        	logger.error("Retail Exception thrown: " + e.getMessage());
        	throw new NestedRuntimeException(e);
        }
        
        if(logger.isDebugEnabled())
        	logger.debug("appendRules return code is " + returnCode);
        
		return returnCode;
	}
	
	public static boolean writeLocationAORMappingFile(SOData context) {
//		sql query definitions
		StringBuffer uniqueBuffer = new StringBuffer("SELECT DISTINCT LOCPROP_VALUE AS AOR ");
		uniqueBuffer.append("FROM SO_LOCATION_PROP ");
		uniqueBuffer.append("WHERE PROP_ID = ?");
		
		String GET_UNIQUE_AOR_QUERY = uniqueBuffer.toString();
		
		StringBuffer locationAORBuffer = new StringBuffer("SELECT DISTINCT G.SKDGRP_ID, SKDGRP_INTRNL_TYPE, LOCPROP_VALUE ");
		locationAORBuffer.append("FROM (SELECT SKDGRP_ID, SKDGRP_INTRNL_TYPE   ");
		locationAORBuffer.append("            FROM SO_SCHEDULE_GROUP, WORKBRAIN_TEAM   ");
		locationAORBuffer.append("	                WHERE WORKBRAIN_TEAM.WBT_ID = SO_SCHEDULE_GROUP.WBT_ID AND ");  
		locationAORBuffer.append("	                    SKDGRP_INTRNL_TYPE <> ? AND   ");
		locationAORBuffer.append("	                   SO_SCHEDULE_GROUP.WBT_ID IN   ");
		locationAORBuffer.append("	                        (SELECT CHILD_WBT_ID   ");
		locationAORBuffer.append("	                        FROM SEC_WB_TEAM_CHILD_PARENT ");  
		locationAORBuffer.append("	                        WHERE PARENT_WBT_ID =   ");
		locationAORBuffer.append("	                                (SELECT WBT_ID   ");
		locationAORBuffer.append("	                                FROM SO_SCHEDULE_GROUP   ");
		locationAORBuffer.append("	                                WHERE SKDGRP_ID = ?)) ");
		locationAORBuffer.append("	                ORDER BY WBT_LEVEL DESC) G LEFT OUTER JOIN SO_LOCATION_PROP LP ON (G.SKDGRP_ID = LP.SKDGRP_ID) ");
		locationAORBuffer.append("WHERE PROP_ID = ? OR PROP_ID IS NULL ");
		
		String GET_LOCATION_AOR_QUERY = locationAORBuffer.toString();
		
		StringBuffer aorWithBudgetSetBuffer = new StringBuffer("");
		aorWithBudgetSetBuffer.append("SELECT DISTINCT LOCPROP_VALUE ");
		aorWithBudgetSetBuffer.append("FROM SO_BUDG_PAY_COST B, SO_SCHEDULE_GROUP G, SO_LOCATION_PROP L ");
		aorWithBudgetSetBuffer.append("WHERE B.SKDGRP_ID = G.SKDGRP_ID ");
		aorWithBudgetSetBuffer.append("    AND L.SKDGRP_ID = G.SKDGRP_ID ");
		aorWithBudgetSetBuffer.append("    AND G.SKDGRP_PARENT_ID = ? ");
		aorWithBudgetSetBuffer.append("    AND L.PROP_ID = ? ");
		aorWithBudgetSetBuffer.append("    AND BDGPAY_DATE BETWEEN ? AND ? ");
		
		String GET_AOR_WITH_BUDGET_QUERY = aorWithBudgetSetBuffer.toString();
		
		boolean returnCode = false;
		
		//determine if we should write the mapping file
		//only write the mapping file if there is an
		//emp group that has rule 20 enabled.
		boolean writeToAORMappingFile = false;
		try {
			Vector empGroupList = context.getScheduleEmployeeGroups();
	        ScheduleEmployeeGroup skdEmpGroup = null;
	        EmployeeGroupAccess ega = new EmployeeGroupAccess(context.getDBconnection());
	        EmployeeGroup empGroup = new EmployeeGroup();
	        RuleGroupAccess rga = new RuleGroupAccess(conn);
			
	        //check each staff group to see if rule 20
	        //is enabled for it
	        for (int i = 0; i<empGroupList.size(); i++) {
	        	skdEmpGroup = (ScheduleEmployeeGroup)(empGroupList.elementAt(i));
	            empGroup = (EmployeeGroup)ega.loadRecordDataByPrimaryKey(empGroup, skdEmpGroup.getEmpgrpId().intValue());
	            RuleGroup rg = rga.load(empGroup.getRulegrpId());
	            Vector rules = rg.getStaffRulesList();
	            
	            if(logger.isDebugEnabled())
	            	logger.debug("rules.size = " + rules.size());
	            
	            for(int j = 0; j < rules.size(); j++) {
	            	StaffRule sr = (StaffRule)rules.elementAt(j);
	            	
	            	if(logger.isDebugEnabled())
	            		logger.debug("sr.getRulId = " + sr.getRulId());
	            	
	            	if(sr.getRulId().intValue() == NOT_EXCEED_BUDGET_RULE_ID) {
	            		writeToAORMappingFile = true;
	            		
	            		if(logger.isDebugEnabled())
	    	        		logger.debug("Rule 20 is in use. Write the mapping file");
	            		
	            		break;
	            	}
	            }//end for rules.size
	            
	            if(writeToAORMappingFile)
	            	break;
	        }//end for empgrouplist.size
        }
        catch(SQLRetailException e) {
        	logger.error("Could not get connection: " + e.getMessage());
        	throw new NestedRuntimeException(e);
        }
        catch(RetailException e) {
        	logger.error("Retail Exception thrown: " + e.getMessage());
        	throw new NestedRuntimeException(e);
        }
        
        //if the rule is enabled, we should write the
        //mapping file
        if(writeToAORMappingFile) {
	        //get the skdgrp_id of the store we're working on
	        int skdgrpId = 0;
	        try {
	        	skdgrpId = context.getScheduleCE().getID().intValue();
	        }
	        catch(RetailException e) {
	        	logger.error("CE SKDGRP_ID not set!");
	        	throw new NestedRuntimeException(e);
	        }
	        
	        if(logger.isDebugEnabled())
	        	logger.debug("Got parameters: skdgrpId=" + skdgrpId + /*"; soBaseDir='" + soBaseDir + "'*/";");
			
	//      get all the employee groups
	        PreparedStatement ps = null;
	        ResultSet rs = null;
	        
			try {
	        	conn = context.getDBconnection();
	        	
	        	//get the AOR's that have a budget and put them
	        	//into a hashmap for later comparison
	        	Map aorWithBudget = new HashMap();
	        	
	        	ps = conn.prepareStatement(GET_AOR_WITH_BUDGET_QUERY);
	        	ps.setInt(1, context.getScheduleCE().getID().intValue());
	        	ps.setInt(2, AOR_LOCATION_PROPERTY_ID);
	        	ps.setTimestamp(3, new java.sql.Timestamp(context.getScheduleStartDate().getCalendar().getTimeInMillis()));
	        	ps.setTimestamp(4, new java.sql.Timestamp(context.getScheduleEndDate().getCalendar().getTimeInMillis()));
	        	
	        	rs = ps.executeQuery();
	        	
	        	while(rs.next()) 
	        		aorWithBudget.put(rs.getString("locprop_value"), rs.getString("locprop_value"));
		        
		        SQLHelper.cleanUp(ps, rs);
	        	
	//			get the AOR types and map them to an integer
		        Map aorValues = new HashMap();
		        
		        ps = conn.prepareStatement(GET_UNIQUE_AOR_QUERY);
		        ps.setInt(1, AOR_LOCATION_PROPERTY_ID);
		        rs = ps.executeQuery();
		        
		        int counter = 0;
		        
		        while(rs.next()) {
		        	counter++;
		        	aorValues.put(rs.getString("AOR"), new Integer(counter));
		        	
		        	if(logger.isDebugEnabled())
		        		logger.debug("Added AOR " + rs.getString("AOR") + " with key " + counter);
		        }
		        
		        SQLHelper.cleanUp(ps, rs);
		        
		        //get the AOR for each location and print it into the file for mosel
		        ps = conn.prepareStatement(GET_LOCATION_AOR_QUERY);
		        ps.setInt(1, InternalType.DRIVER.intValue());
		        ps.setInt(2, skdgrpId);
		        ps.setInt(3, AOR_LOCATION_PROPERTY_ID);
		        rs = ps.executeQuery();
		        
		        FileManager fileManager = context.getLocalFileManager();
	            Integer fileIndex = fileManager.registerFile(MAP_FILE_LOCATION);
	            
		        while(rs.next()) {
		        	String outString = new String("");
		        	
		        	outString = rs.getString("SKDGRP_ID");
		        	outString += DELIMITER;
		        	
		        	String propValueString = rs.getString("LOCPROP_VALUE");
		        	Integer propValueInteger = null;
		        	
		        	if(propValueString != null && 
		        			!propValueString.equals("") &&
		        			aorWithBudget.get(propValueString) != null)
		        		propValueInteger = (Integer)aorValues.get(propValueString);
		        	else
		        		propValueInteger = (Integer)aorValues.get("STORE");
		        	
		        	outString += propValueInteger.toString();
		        	outString += END_OF_LINE;
		        	
		        	if(logger.isDebugEnabled())
		        		logger.debug("Output string is '" + outString + "'");
		        	
		        	fileManager.write(fileIndex, outString);
		        }
		        
		        //SQLHelper.cleanUp(ps, rs);
		        returnCode = true;
	        }
	        catch(SQLException e) {
	        	logger.error("SQL Exception thrown: " + e.getMessage());
	        	throw new NestedRuntimeException(e);
	        }
	        catch(SQLRetailException e) {
	        	logger.error("Could not get connection: " + e.getMessage());
	        	throw new NestedRuntimeException(e);
	        }
	        catch(RetailException e) {
	        	logger.error("Retail Exception thrown: " + e.getMessage());
	        	throw new NestedRuntimeException(e);
	        }
	        finally {
	        	SQLHelper.cleanUp(ps, rs);
	        }
        }
        else {
        	if(logger.isDebugEnabled())
        		logger.debug("No rule groups are using the budget by AOR feature. No file written");
        	
        	returnCode = true;
        }
        if(logger.isDebugEnabled())
        	logger.debug("writeLocationAORMappingFIle return code is " + returnCode);
		
        return returnCode;
	}
	
	/**
	 * returns false if the engine should subtract PTO
	 * (i.e. use core behaviour)
	 * returns TRUE if the engine should NOT subtract PTO
	 * (i.e. custom behaviour in getBudgetWithPTOIncluded method)
	 */
	public static boolean notSubtractPTO() {
//		check the registry to see if SO_BUDGET_BY_AOR_NOT_SUBTRACT_PTO is enabled
        boolean notSubtractPTO = false;
        
        try {
        	notSubtractPTO = (Boolean.valueOf((String)Registry.getVar(NOT_SUBTRACT_PTO_REGISTRY_KEY))).booleanValue();
        	
        	if(logger.isDebugEnabled())
        		logger.debug("Found registry setting '" + NOT_SUBTRACT_PTO_REGISTRY_KEY + "': value=" + notSubtractPTO);
        } catch (NamingException ne) {
            logger.error("Could not find registry variable! Defaulting to FALSE: " + ne.getMessage());
        	throw new NestedRuntimeException(ne);
        }
        
        if(logger.isDebugEnabled())
        	logger.debug("Returning notSubtractPTO=" + notSubtractPTO);
        
        return notSubtractPTO;
	}
	
	public static double getBudgetWithPTOIncluded(int skdgrpId, Calendar startCal, Calendar endCal) {
		//sql query definitions
		StringBuffer budgetBuffer = new StringBuffer("SELECT BDGPAY_AMOUNT ");
		budgetBuffer.append("FROM SO_BUDG_PAY_COST ");
		budgetBuffer.append("WHERE SKDGRP_ID = ? AND ");
		budgetBuffer.append("    BDGPAY_DATE BETWEEN ? AND ? ");

		String GET_BUDGET_FOR_LOCATION_QUERY = budgetBuffer.toString();
		
		double locBudget = 0.0;

//      get the budget for this location for the given time period
        PreparedStatement ps = null;
        ResultSet rs = null;
        
		try {
        	conn = DBInterface.getCurrentConnection();
	        
        	ps = conn.prepareStatement(GET_BUDGET_FOR_LOCATION_QUERY);
        	ps.setInt(1, skdgrpId);
        	ps.setTimestamp(2, new Timestamp(startCal.getTimeInMillis()));
        	ps.setTimestamp(3, new Timestamp(endCal.getTimeInMillis()));
        	rs = ps.executeQuery();
        	
        	if(logger.isDebugEnabled())
        		logger.debug("Executing query to get budget amount of location " + skdgrpId);
        	
        	if(rs.next()) {
        		locBudget = rs.getDouble("BDGPAY_AMOUNT");
        		
        		if(logger.isDebugEnabled())
        			logger.debug("Budget number of " + locBudget + " found for location " + skdgrpId);
        	}
        	else {
        		if(logger.isDebugEnabled())
        			logger.debug("No budget number found for location " + skdgrpId);
        	}
	        //SQLHelper.cleanUp(ps, rs);
        }
        catch(SQLException e) {
        	logger.error("SQL Exception thrown: " + e.getMessage());
        	throw new NestedRuntimeException(e);
        }
        catch(SQLRetailException e) {
        	logger.error("Could not get connection: " + e.getMessage());
        	throw new NestedRuntimeException(e);
        }
        finally {
        	SQLHelper.cleanUp(ps, rs);
        }
        
        if(logger.isDebugEnabled())
        	logger.debug("Returning a budget of " + locBudget + " for location " + skdgrpId);
        //locBudget = 12;
		return locBudget;
	}
	
	public static boolean setContextBudget(BudgetContext context) {
		boolean returnCode = false;
		
		int skdgrpId = context.getSkdgrp_id();
		Calendar startCal = context.getStart_date();
		Calendar endCal = context.getEnd_date();
		
		if(logger.isDebugEnabled())
			logger.debug("setContextBudget: skdgrp_id=" + skdgrpId + "; startCal=" + startCal.toString() + "; endCal=" + endCal.toString() + ";");
		
		double locBudget = getBudgetWithPTOIncluded(skdgrpId, startCal, endCal);
		
		if(logger.isDebugEnabled())
			logger.debug("Adding budget value of " + locBudget + " to context...");
		
		try {
			context.add(BudgetCallout.BUDGET_VALUE, new Double(locBudget));
			returnCode = true;
		}
		catch(Exception e) {
			returnCode = false;
        	throw new NestedRuntimeException(e);
		}
		
		return returnCode;
	}
}
