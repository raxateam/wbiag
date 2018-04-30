package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;
import javax.servlet.ServletRequest;

import com.workbrain.app.jsp.ActionContext;
import com.workbrain.app.jsp.action.dailytimesheet.TimesheetHelper;
import com.workbrain.app.jsp.action.timesheet.EmployeeModel;
import com.workbrain.app.jsp.action.timesheet.NewWeekAction;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.EmployeeAccess;
import com.workbrain.app.ta.db.EmployeeJobAccess;
import com.workbrain.app.ta.db.JobAccess;
import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.app.ta.model.EmployeeIdStartEndDate;
import com.workbrain.app.ta.model.EmployeeJobData;
import com.workbrain.app.ta.model.JobData;
import com.workbrain.server.data.type.DatetimeType;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

public class CSMNewEmployeeAction extends NewWeekAction implements CSMBiWeeklyTimeSheetConstants {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(CSMNewEmployeeAction.class);


    public Object createRequest( ServletRequest request, DBConnection conn )
            throws Exception {

        Object requestObj[] = new Object[2];
        String dateString = request.getParameter( "WEEK_START_DATE" );
        try {
            requestObj[0] = DatetimeType.FORMAT.parse( dateString );
        } catch( Exception e ) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
        }

        int empId = 0;
        try {
            empId = Integer.parseInt( request.getParameter( "EMP_ID" ) );
        } catch( Exception e ) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
        }
        requestObj[1] = new Integer(empId);

        return requestObj;
    }

    public String process( DBConnection conn, ActionContext context,
                           Object requestObj )
            throws Exception {

        Object[] request = (Object[])requestObj;
        CSMBiWeeklyTimeSheetPage model = (CSMBiWeeklyTimeSheetPage)context.getAttribute(
                "timesheet" );
        if( model == null ) {
            model = new CSMBiWeeklyTimeSheetPage();
        }

        if( request[0] != null ) {
            model.setWeekStartDate( TimesheetHelper.getWeekStartDate((java.util.Date)request[0] ));
        }
        java.util.Date date = model.getWeekStartDate();
        int empId = ((Integer)request[1]).intValue();
        model.setEmployee( createEmployeeModel( conn, context, model.getWbuName(),
                empId, date ) );
        invalidateSummaries( model );

        context.setAttribute( "timesheet", model );
        return CSMWTShelper.getActionURL(ACTION_VIEW_TIMESHEET);
    }

    protected static boolean setWbgSession( DBConnection conn, ActionContext context,
                                  String wbuName ) throws SQLException {
        boolean result = false;

        String sqlQuery = "SELECT WBG_LOCKDOWN FROM WORKBRAIN_USER, WORKBRAIN_GROUP " +
                   " WHERE WORKBRAIN_USER.WBU_NAME= ?" +
                   " AND WORKBRAIN_USER.WBG_ID = WORKBRAIN_GROUP.WBG_ID";
        ResultSet rs = null;
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sqlQuery);
            stmt.setString(1, wbuName);
            rs = stmt.executeQuery();
            if( rs.next() ){
                String lockDown = rs.getString("WBG_LOCKDOWN");
                if(null != lockDown && (lockDown.equals("Y") || lockDown.equals("true"))){
                    result = true;
                }else{
                    result = false;
                }
            } else {
                result = false;
            }
        } finally {
            SQLHelper.cleanUp(stmt, rs);
        }

        if( result ) {
            context.setAttribute( "WBG_SESSION", "Y" );
        } else {
            context.setAttribute( "WBG_SESSION", "N" );
        }

        return result;
    }

    protected static CSMEmployeeModel createEmployeeModel( DBConnection conn,
            ActionContext context, String wbuName, int empId, Date date )
            throws SQLException {
        EmployeeAccess empAcc = new EmployeeAccess( conn,
                CodeMapper.createCodeMapper( conn ) );
        EmployeeData emp = empAcc.load( empId, date );

        return createEmployeeModel( conn, context, wbuName, emp, date );
    }

    protected static CSMEmployeeModel createEmployeeModel( DBConnection conn,
            ActionContext context, String wbuName, EmployeeData emp, Date date )
            throws SQLException {
        CSMEmployeeModel employee = new CSMEmployeeModel();

        employee.setEmpId( emp.getEmpId() );
        employee.setEmpName( emp.getEmpName() );
        employee.setFirstName( emp.getEmpFirstname() );
        employee.setLastName( emp.getEmpLastname() );
        employee.setFullName( emp.getEmpFullname() );
        //in order to fix TT31673, the employee hire date is needed
        employee.setHireDate(emp.getEmpHireDate());

        //csm: stuff model with extra CSM specific values
        employee.setDeptNo( emp.getEmpVal6()==null? "":emp.getEmpVal6());
        employee.setEmployeePct( emp.getEmpVal2()==null? "":emp.getEmpVal2());
        employee.setWorkgroup( emp.getEmpVal3()==null? "":emp.getEmpVal3());
        employee.setType( emp.getEmpVal4()==null? "":emp.getEmpVal4());

        EmployeeJobAccess empJobAccess = new EmployeeJobAccess(conn);
        JobAccess jobAccess = new JobAccess(conn);
        List empJobList = empJobAccess.loadByEmpDate(emp.getEmpId(), date);
        String empJobNames = "";
        String empJobDescs = "";
        Iterator i = empJobList.iterator();
        while(i.hasNext()){
        	EmployeeJobData empJobData = (EmployeeJobData)i.next();
        	if(empJobData!=null){
        		int jobId = empJobData.getJobId();
        		JobData jobData = jobAccess.load(jobId);
        		if(jobData!=null){
        			empJobNames = empJobNames + jobData.getJobName();
        			empJobDescs = empJobDescs + jobData.getJobDesc();

        			if(i.hasNext()){
        				empJobNames = empJobNames + CSMBiWeeklyTimeSheetConstants.JOB_DELIMITER;
        				empJobDescs = empJobDescs + CSMBiWeeklyTimeSheetConstants.JOB_DELIMITER;
        			}
        		}
        	}
        }

        employee.setJobClass(empJobNames);
        employee.setJobClassName(empJobDescs);

        /*String schedule = "";
        try {
        	schedule = (String)Registry.getVar(CSMBiWeeklyTimeSheetConstants.WBREG_TIMESHEET_NUMBER_OF_DAYS);
		} catch (NamingException e) {
			logger.error(e);
		}
        employee.setSchedule(schedule);*/


        String dayStartTime = null;
        Date dst= emp.getEmpDayStartTime();
        if(dst!=null){
	        Calendar cal = Calendar.getInstance();
	        cal.setTime(dst);
	        dayStartTime=(cal.get(Calendar.HOUR)==0? "00":cal.get(Calendar.HOUR)+"") + ":" +
	        		(cal.get(Calendar.MINUTE)==0? "00":cal.get(Calendar.MINUTE)+"");

        }
        else{
        	try {
        		dayStartTime = (String) Registry.getVar(CSMBiWeeklyTimeSheetConstants.DAY_START_TIME);
        	} catch (NamingException e) {
    			logger.error(e);
    		}
        }
		employee.setDayStartTime(dayStartTime);

        setWbgSession( conn, context, wbuName );

        Date dateHandsOff = null;
        Date supervisorDate = null;
        Date emphistStartDate = null;
        Date emphistEndDate = null;
        Date payGroupStartDate = null;
        Date payGroupEndDate = null;
        HashMap paygroupHandsoffDates = new HashMap();
        HashMap paygroupSupervisorDates = new HashMap();

        //Get the Hands OFF Date
        String sqlQuery = "SELECT PAYGRP_SUPERVISOR_DATE SUPERVISOR_DATE," + // 'yyyyMMdd'
	        " PAYGRP_HANDS_OFF_DATE HANDS_OFF_DATE," + // 'yyyyMMdd'
	        " EMPLOYEE.EMPHIST_START_DATE, EMPLOYEE.EMPHIST_END_DATE," + // 'yyyyMMdd'
	        " PAYGRP_START_DATE, PAYGRP_END_DATE " +
	        " FROM EMPLOYEE_HISTORY EMPLOYEE, PAY_GROUP " +
            " WHERE EMPLOYEE.EMP_ID = ?" +
            " AND EMPLOYEE.PAYGRP_ID = PAY_GROUP.PAYGRP_ID";
        ResultSet rs = null;
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sqlQuery);
            stmt.setInt(1, emp.getEmpId());
            rs = stmt.executeQuery();
            while( rs.next() ) {
                dateHandsOff = rs.getDate( "HANDS_OFF_DATE" );
                supervisorDate = rs.getDate( "SUPERVISOR_DATE" );
                emphistStartDate = rs.getDate("EMPHIST_START_DATE");
                emphistEndDate = rs.getDate("EMPHIST_END_DATE");
                payGroupStartDate = rs.getDate("PAYGRP_START_DATE");
                payGroupEndDate = rs.getDate("PAYGRP_END_DATE");
                EmployeeIdStartEndDate key = new EmployeeIdStartEndDate(
                        emp.getEmpId(),emphistStartDate,emphistEndDate);
                paygroupHandsoffDates.put(key,DateHelper.truncateToDays(dateHandsOff ));
                paygroupSupervisorDates.put(key,DateHelper.truncateToDays(supervisorDate ));
            }
        } finally {
            SQLHelper.cleanUp(stmt, rs);
        }

        employee.setHandsOffDate( DateHelper.truncateToDays(dateHandsOff ));
        employee.setSupervisorDate( DateHelper.truncateToDays(supervisorDate ));
        employee.setPayGroupStartDate(DateHelper.truncateToDays(payGroupStartDate ));
        employee.setPayGroupEndDate(DateHelper.truncateToDays(payGroupEndDate ));
        employee.setPaygroupHandsoffDates(paygroupHandsoffDates);
        employee.setPaygroupSupervisorDates(paygroupSupervisorDates);
        employee.setTerminationDate( emp.getEmpTerminationDate() );

        return employee;
    }
}
