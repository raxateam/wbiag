package com.wbiag.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.StringHelper;

/**
 * @author bviveiros
 *
 * WB 4.0 version of this file.  
 * 
 * 
 */
public class GetEmployeeHelper {

	/**
	 * Get a List of EmployeeData objects based on the input parameters.
	 * 
	 * Note: This code is copied from the core method 
	 * LoadEmployeeAction.loadEmployeesDefaultRecords(DBConnection, Timesheetparams, CodeMapper).  A TT has been
	 * created to have this code made into a core public API.  The API will most likely not be
	 * available until after WB 5.0.
	 * 
	 * @param conn
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public static List loadEmployees(DBConnection conn, GetEmployeeParameters params) throws Exception {
		
		List empList = null;
        Statement stmt = null;
		ResultSet rs = null;
        StringBuffer sql = new StringBuffer();

        // Main query based on wbuId and dates.
        sql.append(" SELECT DISTINCT EMPLOYEE.*");
        sql.append(" FROM SEC_EMPLOYEE,EMPLOYEE WHERE WBU_ID = ");
        sql.append(params.getWbuId());
        sql.append(" AND EMPLOYEE.EMP_ID = SEC_EMPLOYEE.EMP_ID ");
        sql.append(" AND ((" + conn.encodeTimestamp(params.getStartDate()) +
                   " BETWEEN START_DATE AND END_DATE) OR (" +
                   conn.encodeTimestamp(params.getEndDate()) +
                   " BETWEEN START_DATE AND END_DATE)) ");

        // Employees.
        if (!(StringHelper.isEmpty(params.getEmpIds()) || "ALL".equals(params.getEmpIds()))) {
            sql.append(" AND EMPLOYEE.EMP_ID IN (");
            sql.append(params.getEmpIds());
            sql.append(") ");
        }

        // Teams.
        if (!(StringHelper.isEmpty(params.getTeamIds()) || "ALL".equals(params.getTeamIds()))) {
            if (params.getIncludeSubTeams()) {
                sql.append(" AND SEC_EMPLOYEE.wbt_id in (select distinct child_wbt_id from SEC_WB_TEAM_CHILD_PARENT ");
                sql.append(" where parent_wbt_id  IN (");
                sql.append(params.getTeamIds());
                sql.append(" ) ) AND SEC_EMPLOYEE.wbt_id in (");
                sql.append(" select distinct sec_workbrain_team.wbt_id ");
                sql.append(" from sec_workbrain_team where sec_workbrain_team.wbu_id = ");
                sql.append(params.getWbuId());
                sql.append(")");
            } else {
                sql.append(" AND SEC_EMPLOYEE.WBT_ID IN (");
                sql.append(params.getTeamIds());
                sql.append(") ");
            }
        }

        // PayGroups.
        if (!(StringHelper.isEmpty(params.getPayGroupIds()) || "ALL".equals(params.getPayGroupIds()))) {
            sql.append(" AND EMPLOYEE.PAYGRP_ID IN (");
            sql.append(params.getPayGroupIds());
            sql.append(") ");
        }

        // Calc Groups.
        if (!(StringHelper.isEmpty(params.getCalcGroupIds()) || "ALL".equals(params.getCalcGroupIds()))) {
            sql.append(" AND EMPLOYEE.CALCGRP_ID IN (");
            sql.append(params.getCalcGroupIds());
            sql.append(") ");
        }
        
        // Order by.
        sql.append(createOrderByClause(params.getOrderBy()));

		try {
			// Create the statement
            stmt = conn.createStatement();
            
            // Execute the query
            rs = stmt.executeQuery(sql.toString());

            // Process the results.
            if (rs != null) {
				empList = populateEmployeeList(rs);
			}
            
		} finally {
            if (stmt != null) {
                stmt.close();
            }
            if (rs != null) {
                rs.close();
            }
		}
		
		return empList;
	}
	
	/*
	 * Create the order by clause based on the available options.
	 * 
	 * @param orderByOption
	 * @return
	 */
    private static String createOrderByClause(int orderByOption) {

    	switch (orderByOption) {
            case GetEmployeeParameters.ORDER_BY_EMP_NAME:
                return " ORDER BY EMP_NAME, EMP_FIRSTNAME, EMP_LASTNAME";
            case GetEmployeeParameters.ORDER_BY_LAST_NAME:
                return " ORDER BY EMP_LASTNAME, EMP_FIRSTNAME, EMP_NAME";
            case GetEmployeeParameters.ORDER_BY_FIRST_NAME:
                return " ORDER BY EMP_FIRSTNAME, EMP_LASTNAME, EMP_NAME";
            default:
                return " ORDER BY EMP_NAME, EMP_FIRSTNAME, EMP_LASTNAME";
        }
    }
	
    /*
     * From a ResultSet, create a List of EmployeeData.
     * 
     * @param rs
     * @return
     * @throws SQLException
     */
    private static List populateEmployeeList(ResultSet rs) throws SQLException {
		
		List empList = new ArrayList();
		EmployeeData emp = null;
		
		while (rs.next()) {
			emp = populateEmployee(rs);
			empList.add(emp);
		}
		
		return empList;
	}
	
	/*
	 * From a ResultSet, create an EmployeeData.
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private static EmployeeData populateEmployee(ResultSet rs) throws SQLException {

		EmployeeData emp = new EmployeeData();

		emp.assignByName(rs);
		
		return emp;
	}
}
