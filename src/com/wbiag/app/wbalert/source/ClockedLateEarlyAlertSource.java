package com.wbiag.app.wbalert.source;

import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;
import java.util.*;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import com.workbrain.app.wbalert.*;

/**
 * ClockedLateEarlyAlertSource
 *
 * <p>Copyright: Copyright (c) 2002 Workbrain Inc.</p>
 */
public class ClockedLateEarlyAlertSource extends AbstractRowSource{
   private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ClockedLateEarlyAlertSource.class);
    public static final String PARAM_MINUTES = "xMinutes";
    public static final String PARAM_EARLY_LATE = "earlyLate";
    public static final String PARAM_CLOCK_TYPE = "clockType";
    public static final String PARAM_MAXIMUM_AGE = "maxAge";
    public static final String PARAM_EMPLOYEE = "employee";
	public static final String PARAM_CALCGROUP = "calcGroup";
	public static final String PARAM_PAYGROUP = "payGroup";
	public static final String PARAM_TEAM = "team";
	public static final String LATE_TYPE = "LATE";
	public static final String EARLY_TYPE = "EARLY";
	public static final String IN_CLOCK = "IN";
	public static final String OUT_CLOCK = "OUT";
	public static final int IN_CLOCK_TYPE = 1;
	public static final int OUT_CLOCK_TYPE = 2;
    
    private RowDefinition rowDefinition;
    private DBConnection conn;
    private java.util.List rows = new ArrayList();
    private String strXminutes;
    private int intXminutes = 0;
    private String earyLate;
    private String strClockType;
    private int intClockType;
    private String strMaxAge;
    private int intMaxAge = 0;
    private String employee;
    private String calcGroup;
    private String payGroup;
    private String team;
    
    //private String workDate;
    private Date taskDateTime;
    
    // *** Define column names here
    {
        RowStructure rs = new RowStructure(6);
        rs.add("EMP_ID",CharType.get(100));
        rs.add("EMP_NAME",CharType.get(100));
        rs.add("EMP_FULLNAME",CharType.get(100));
        rs.add("WORK_DATE",CharType.get(100));
        rs.add("Early/Late",CharType.get(100));
        rs.add("Minutes",CharType.get(100)); 
        rowDefinition = new RowDefinition(-1,rs);
    }

    /**
     *
     *@param  c                    Connection
     *@param  alertParams          alert parameters
     *@exception  AccessException  Description of Exception
     */
    public ClockedLateEarlyAlertSource(DBConnection c , HashMap alertParams) throws AccessException {
        this.conn = c;
        this.strXminutes = (String)alertParams.get(PARAM_MINUTES);
        if (this.strXminutes == null) {
            throw new AccessException(PARAM_MINUTES + " can not be null");
        }
        if (!StringHelper.isEmpty(strXminutes)) {
            try {
                this.intXminutes = Integer.parseInt(strXminutes);
                this.intXminutes = (intXminutes < 0) ? 0 : intXminutes;
            }
            catch (NumberFormatException ex) {
                throw new AccessException(PARAM_MINUTES + " must be an Integer");
            }
        }
        
        this.earyLate = (String)alertParams.get(PARAM_EARLY_LATE);
        if (this.earyLate == null) {
            throw new AccessException(PARAM_EARLY_LATE + " can not be null");
        }
        
        this.strClockType = (String)alertParams.get(PARAM_CLOCK_TYPE);
        if (this.strClockType == null) {
            throw new AccessException(PARAM_CLOCK_TYPE + " can not be null");
        }
        this.intClockType = IN_CLOCK.equalsIgnoreCase(strClockType)? 1 : 2;
        
        this.strMaxAge = (String)alertParams.get(PARAM_MAXIMUM_AGE);
        if (!StringHelper.isEmpty(strMaxAge)) {
            try {
                this.intMaxAge = Integer.parseInt(strMaxAge);
            }
            catch (NumberFormatException ex) {
                throw new AccessException(PARAM_MAXIMUM_AGE + " must be an Integer");
            }
        }
        
        this.employee = (String)alertParams.get(PARAM_EMPLOYEE);
        this.calcGroup = (String)alertParams.get(PARAM_CALCGROUP);
        this.payGroup = (String)alertParams.get(PARAM_PAYGROUP);
        this.team = (String)alertParams.get(PARAM_TEAM);     
        this.taskDateTime = (Date) alertParams.get(WBAlertProcess.TASK_PARAM_TASKDATETIME);
        
        /*
        if (logger.isDebugEnabled()) {
           logger.debug("********************************************");
           logger.debug("***intXMinutes***: " + intXminutes);	
           logger.debug("***earyLate***: " + earyLate);
           logger.debug("***strClockType***: " + strClockType);
           logger.debug("***intClockType***: " + intClockType);
           logger.debug("***intMaxAge***: " + intMaxAge);
           logger.debug("***employee***: " + employee);
           logger.debug("***calcGroup***: " + calcGroup);
           logger.debug("***payGroup***: " + payGroup);
           logger.debug("***team***: " + team);
           logger.debug("***taskDateTime***: " + taskDateTime);
           logger.debug("***employee***: " + employee);
           logger.debug("********************************************");
        }
      */
        // Load the rows
        try {
            loadRows();
        } catch (Exception e) {
            throw new NestedRuntimeException (e);
        }
    }


    /** loads the employees who are late or early
     *
     *@exception  AccessException  Description of Exception
     */
    private void loadRows() throws AccessException , SQLException{
       rows.clear();
       PreparedStatement ps = null;
	   ResultSet rs = null;
       Date workDate = DateHelper.truncateToDays(new Date());
       List clocks  = new ArrayList();
       Timestamp scheduleStartTime;
       Timestamp scheduleEndTime;
       Date firstInClock;
       Date lastOutClock;
       long timeDiff;
       
       try {
          if(logger.isDebugEnabled()) { logger.debug(constructSQL()); }
          
       	  ps = conn.prepareStatement( constructSQL() );
       	  int cnt = 1;
       	  ps.setTimestamp( cnt++, new Timestamp(workDate.getTime()) );
       	  ps.setTimestamp( cnt++, new Timestamp(taskDateTime.getTime()) );
       	  
          //If max age is set
   	      if( intMaxAge != 0 ) {
   	         Datetime birthThreshold = DateHelper.addDays(DateHelper.getCurrentDate(), -(365*intMaxAge));
   	         ps.setTimestamp( cnt++, new Timestamp(birthThreshold.getTime()) );  
          }
   	      //if Team selected. - current date is between empt_start_date and empt_end_date
   	      if( !StringHelper.isEmpty(team) ) {
   	         ps.setTimestamp( cnt++, new Timestamp(DateHelper.getCurrentDate().getTime()));
   	      }
   	      
       	  rs = ps.executeQuery();
		  if(logger.isDebugEnabled()) { logger.debug("Query executed."); }

          while (rs.next()) {
             firstInClock = null;
             lastOutClock = null;
             timeDiff = 0;
             try {
		        clocks = Clock.createClockListFromString(rs.getString("wrks_clocks"));
		     }catch (Exception e){
		         if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { 
		            logger.error("Can not create clock list from a String: " + e.getMessage() , e);}
		            throw new NestedRuntimeException(e);
		     }
		     if (clocks.size() > 0) {
 		        for (int i=0; i < clocks.size(); i ++) {
 		           Clock theClock = (Clock)clocks.get(i);
 		           int clkType = theClock.getClockType();
 		           Date clkTime = theClock.getClockDate();
 		           
 		           if( clkType == IN_CLOCK_TYPE){
 		              if(firstInClock == null) {
 		              	firstInClock = clkTime; 
 		              }
 		              else if ( clkTime.before(firstInClock)) {
 		              	firstInClock = clkTime;	
 		              }
 		           }
 		           else if( clkType == OUT_CLOCK_TYPE){
		              if(lastOutClock == null) {
		              	lastOutClock = clkTime; 
		              }
		              else if ( clkTime.after(lastOutClock)) {
		              	lastOutClock = clkTime;	
		              }
		           }  
 		        }
 			    scheduleStartTime = rs.getTimestamp("empskd_act_start_time");
 		        scheduleEndTime = rs.getTimestamp("empskd_act_end_time");
 		        
 		        if(LATE_TYPE.equalsIgnoreCase(earyLate)) {
		           if(IN_CLOCK.equalsIgnoreCase(strClockType) && firstInClock != null) {
		              timeDiff =  DateHelper.getMinutesBetween(firstInClock, scheduleStartTime);  	
		           }
		           else if(OUT_CLOCK.equalsIgnoreCase(strClockType) && lastOutClock != null) {
		              timeDiff =  DateHelper.getMinutesBetween(lastOutClock, scheduleEndTime);   	
		           }   
		        }
		        else if(EARLY_TYPE.equalsIgnoreCase(earyLate)) {
		           if(IN_CLOCK.equalsIgnoreCase(strClockType) && firstInClock != null ) {
		              timeDiff =  DateHelper.getMinutesBetween(scheduleStartTime, firstInClock);   	
		           }
		           else if(OUT_CLOCK.equalsIgnoreCase(strClockType) && lastOutClock != null) {
		      	      timeDiff =  DateHelper.getMinutesBetween(scheduleEndTime, lastOutClock);   	
		           }	
		        }
 		        if(timeDiff > intXminutes) {
                   Row r = new BasicRow(getRowDefinition());
                   r.setValue("EMP_ID" , rs.getString("emp_id"));
                   r.setValue("EMP_NAME" , rs.getString("emp_name") );
                   r.setValue("EMP_FULLNAME" , rs.getString("emp_fullname"));
                   r.setValue("WORK_DATE" , rs.getDate("wrks_work_date"));
                   r.setValue("Early/Late" , earyLate);
                   r.setValue("Minutes" , String.valueOf(timeDiff));
                   rows.add(r);	
                }        
 		     }   
          }   
       } finally {
          if (rs != null) {
             rs.close();
          }
          if (ps != null) {
             ps.close();
          }
       }
    }
    
    /**Method constructs the sql statement according to the parameters"
	 * employee, calc group, pay group, team
	 * */
	private String constructSQL() {
	   StringBuffer selectSb = new StringBuffer(400);
	   StringBuffer fromSb = new StringBuffer(400);
	   StringBuffer whereSb = new StringBuffer(400);
	   
	   selectSb.append("SELECT e.emp_id, e.emp_name , e.emp_fullname, ws.wrks_work_date, ");
	   selectSb.append("es.empskd_act_start_time, es.empskd_act_end_time, ws.wrks_clocks ");
	   
	   fromSb.append("FROM employee_schedule es, employee e, work_summary ws ");
	   
       //If team selected
	   if( !StringHelper.isEmpty(team) ) {
	      fromSb.append(", employee_team et, workbrain_team wt " );   
       }
	   
	   whereSb.append("WHERE es.emp_id = e.emp_id ");
	   whereSb.append("and e.emp_id = ws.emp_id ");
	   whereSb.append("and es.work_date = ws.wrks_work_date ");
	   whereSb.append("and es.empskd_act_start_time <> es.empskd_act_end_time ");
	   whereSb.append("and ws.wrks_clocks is not null ");
	   whereSb.append("and ws.wrks_work_date = ? ");
	   
	   //make sure the alert is configured properly. 
	   //eg. The early - out alert should not pick an employee when employee goes out for break
	   if (IN_CLOCK.equalsIgnoreCase(strClockType)) {
	      whereSb.append("and es.empskd_act_start_time < ? "); 
	   }
	   else if (OUT_CLOCK.equalsIgnoreCase(strClockType)) {
	      whereSb.append("and es.empskd_act_end_time < ? "); 
	   } 
	   
	   //If max age is given
	   if( intMaxAge != 0 ) {
	      whereSb.append("and e.emp_birth_date >= ? ");   
       }
	   
       //If employee selected
	   if( !StringHelper.isEmpty(employee) ) {
	      whereSb.append("and e.emp_id in (" + employee + ") " );   
       }
       
	   //If calcgroup selected
	   if( !StringHelper.isEmpty(calcGroup) ) {
	      whereSb.append("and e.calcgrp_id in (" + calcGroup + ") " );   
       }
	   
       //If paygroup selected
	   if( !StringHelper.isEmpty(payGroup) ) {
	      whereSb.append("and e.paygrp_id in (" + payGroup + ") " );   
       }
	   
       //If team selected
	   if( !StringHelper.isEmpty(team) ) {
	      whereSb.append("and e.emp_id = et.emp_id ");
	      whereSb.append("and et.wbt_id = wt.wbt_id ");
	      whereSb.append("and et.empt_home_team = 'Y' ");
	      whereSb.append("and ? between et.empt_start_date and et.empt_end_date ");
	      whereSb.append("and wt.wbt_id in (" + team + ") " ); 
       }
	   return selectSb.toString() + " " + fromSb.toString() + " " + whereSb.toString();	
	}

    public RowDefinition getRowDefinition() throws AccessException {
        return rowDefinition;
    }

    public RowCursor query(String queryString) throws AccessException{
        return queryAll();
    }

    public RowCursor query(String queryString, String orderByString) throws AccessException{
        return queryAll();
    }

    public RowCursor query(List keys) throws AccessException{
        return queryAll();
    }

    public RowCursor query(String[] fields, Object[] values) throws AccessException {
        return queryAll();
    }

    public RowCursor queryAll()  throws AccessException{
        return new AbstractRowCursor(getRowDefinition()){
            private int counter = -1;
            protected Row getCurrentRowInternal(){
                return counter >= 0 && counter < rows.size() ? (BasicRow)rows.get(counter) : null;
            }
            protected boolean fetchRowInternal() throws AccessException{
                return ++counter < rows.size();
            }
            public void close(){}
        };
    }
    
    public boolean isReadOnly(){
       return true;
    }

    public int count() {
        return rows.size();
    }

    public int count(String where) {
        return rows.size();
    }

}






