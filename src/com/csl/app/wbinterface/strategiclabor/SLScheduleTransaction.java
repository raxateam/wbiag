package com.csl.app.wbinterface.strategiclabor;

import java.util.*;
import java.sql.SQLException;

import com.workbrain.util.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.schedulein.ScheduleTransaction;
import com.workbrain.app.wbinterface.schedulein.ScheduleTransactionMapper;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.db.OverrideAccess;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.ta.db.CodeMapper;

import com.csl.app.wbinterface.strategiclabor.SLScheduleTransactionBatch;

/**
 * Transaction type for Strategic Labor Schedule data processing.
 *
 * <p>Copyright: Copyright (c) 2009 Infor Global Solutions.</p>
 *
 **/

public class SLScheduleTransaction extends SLScheduleTransactionBatch {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SLScheduleTransaction.class);

    public final static int SCHEDULE_BREAK_MINUTES1 = 19;
    public final static int SCHEDULE_BREAK_MINUTES2 = 20;
    public final static int WBT_TEAM1 = 21;
    public final static int WBT_TEAM2 = 22;
    public final static int SCHEDULE_DETAIL = 23;
    
    public final static String inputDateFormat = "yyyyMMdd HHmm";
    public final static String ovrDateFormat = "yyyyMMdd";
    public final static String sqlDateFormat = "yyyy-MM-dd HH:mm:ss";
    public final static DatetimeFormat OVERRIDE_TIME_FORMAT = new DatetimeFormat(OverrideData.OVERRIDE_TIME_FORMAT_STR);
    
    public static final char SKDDET_SEPARATOR_CHAR = '~';
   
    public static final String SKDDET_WRKD_RATE = "WRKD_RATE" ;
    public static final String SKDDET_WRKD_DEPT_NAME = "WRKD_DEPT_NAME";
    public static final String SKDDET_WRKD_JOB_NAME = "WRKD_JOB_NAME";
    public static final String SKDDET_WRKD_TCODE_NAME = "WRKD_TCODE_NAME";
    public static final String SKDDET_WRKD_HTYPE_NAME = "WRKD_HTYPE_NAME";
    public static final String SKDDET_WRKD_PROJ_NAME = "WRKD_PROJ_NAME";        
    public static final String SKDDET_WRKD_DOCK_NAME = "WRKD_DOCK_NAME";        
    public static final String SKDDET_WRKD_WBT_NAME = "WRKD_WBT_NAME";
    public static final String SKDDET_ACT_NAME = "ACT_NAME"; 
    public static final String SKDDET_WRKD_COMMENTS = "WRKD_COMMENTS";
    public static final String SKDDET_SHFT_BRK = "SHFT_BRK";
    public static final String SKDDET_JT_START_TIME = "JT_START_TIME";        
    public static final String SKDDET_JT_END_TIME = "JT_END_TIME";
    public static final String SKDDET_ESCHD_TYPE = "ESCHD_TYPE";

    public static final String SKDDET_ESCHD_UDF1  = "ESCHD_UDF1";
    public static final String SKDDET_ESCHD_UDF2  = "ESCHD_UDF2";
    public static final String SKDDET_ESCHD_UDF3  = "ESCHD_UDF3";
    public static final String SKDDET_ESCHD_UDF4  = "ESCHD_UDF4";
    public static final String SKDDET_ESCHD_UDF5  = "ESCHD_UDF5";
    public static final String SKDDET_ESCHD_UDF6  = "ESCHD_UDF6";
    public static final String SKDDET_ESCHD_UDF7  = "ESCHD_UDF7";
    public static final String SKDDET_ESCHD_UDF8  = "ESCHD_UDF8";
    public static final String SKDDET_ESCHD_UDF9  = "ESCHD_UDF9";
    public static final String SKDDET_ESCHD_UDF10 = "ESCHD_UDF10";
    public static final String SKDDET_ESCHD_FLAG1 = "ESCHD_FLAG1";
    public static final String SKDDET_ESCHD_FLAG2 = "ESCHD_FLAG2";
    public static final String SKDDET_ESCHD_FLAG3 = "ESCHD_FLAG3";
    public static final String SKDDET_ESCHD_FLAG4 = "ESCHD_FLAG4";
    public static final String SKDDET_ESCHD_FLAG5 = "ESCHD_FLAG5";
    public static final String SKDDET_ESCHD_FLAG6 = "ESCHD_FLAG6";
    public static final String SKDDET_ESCHD_FLAG7 = "ESCHD_FLAG7";
    public static final String SKDDET_ESCHD_FLAG8 = "ESCHD_FLAG8";
    public static final String SKDDET_ESCHD_FLAG9 = "ESCHD_FLAG9";
    public static final String SKDDET_ESCHD_FLAG10 = "ESCHD_FLAG10";

    private static final String ADD = "A";
    private static final String DEL = "D";
    
    public static final String PARAM_NAME_MAX_DAYS_FOR_SCHED_ADJUST = "MaxDaysForScheduleAdjust";
    protected static final int DEFAULT_MAX_DAYS_FOR_SCHED_ADJUST = 7;
    protected Date lastDateForscheduleAdjust = null;
    
    public SLScheduleTransaction() {    	
    }
   
    
    /**
     * Overrides process method from super class in order to use schedule detail override
     * getting first 4 fields that includes date, schedule start and end and emp_id
     * other fields are udfs so first 5 udfs contain information on department, job start, job end time, team and job name
     */
    public void process(ImportData data, DBConnection conn)
                throws WBInterfaceException, SQLException{
        
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {  
            logger.debug( "SLScheduleTransaction.process: reading staging table row id " + data.getId() );
        }
        
        this.conn = conn;
        this.data = data;

        SkdData skdData = new SkdData();
        skdData.initialize( data );
               
        try {           
            preProcess(data, conn);
            
            ScheduleTransactionMapper stMapper = new ScheduleTransactionMapper();

            setValidateFieldValues(stMapper, skdData);
            stMapper.setOvrtypId(OverrideData.SCHEDULE_DETAIL_TYPE);        
            
            if (ADD.equals(skdData.action)) {                
				addOverride(OverrideData.SCHEDULE_DETAIL_TYPE, stMapper, skdData );
            }
            
            else if (DEL.equals(skdData.action))  {
            	deleteScheduleDetailOvr(stMapper.getEmpId(), skdData);
            }
        
            postProcess(data, stMapper, conn);
            
        } catch( Throwable t ) {
            status = ImportData.STATUS_ERROR;
            message = t.getMessage();
            WBInterfaceException.throwWBInterfaceException(t);
        }
    }    
    
    /**
     * Validates input data needed for schedule override
     * 
     */   
    public void setValidateFieldValues( ScheduleTransactionMapper stMapper, SkdData skdData ) 
            throws java.text.ParseException, WBInterfaceException , SQLException {
        
        super.setValidateFieldValues(stMapper);
        
        // Additional input data validation
        if (skdData.action.equals(ADD)) {
            CodeMapper cm = getCodeMapper();
            
            if (cm.getWBTeamByName(skdData.wbtTeam) == null) {
                throw new WBInterfaceException("Team ("+skdData.wbtTeam+") cannot be mapped to an existing team");
            }
            
            if (cm.getDepartmentByName(skdData.dept) == null) {
                throw new WBInterfaceException("Department ("+skdData.dept+") cannot be mapped to an existing department");
            }
            
            if (cm.getJobByName(skdData.jobName) == null) {
                throw new WBInterfaceException("Job ("+skdData.jobName+") cannot be mapped to an existing job");
            }           
        }
    }

    
    /**
     * Creates OvrNewValue string needed for override. This method assumes that input data had already
     * passed all validations.
     * 
     */
    private String getOvrNewValue( SkdData skdData ) throws Exception {
        StringBuffer sb = new StringBuffer();
        String tmpDateString = "";
        
        tmpDateString = OVERRIDE_TIME_FORMAT.format( DateHelper.parseDate(skdData.skdStartTime1, inputDateFormat) );     
        sb.append(OverrideData.formatToken(EmployeeScheduleData.EMPSKD_ACT_START_TIME, tmpDateString));

        tmpDateString = OVERRIDE_TIME_FORMAT.format( DateHelper.parseDate(skdData.skdEndTime1, inputDateFormat) );
        sb.append(OverrideData.formatToken(EmployeeScheduleData.EMPSKD_ACT_END_TIME, tmpDateString));

        sb.append(OverrideData.formatToken(SKDDET_WRKD_WBT_NAME, skdData.wbtTeam));
        sb.append(OverrideData.formatToken(SKDDET_WRKD_DEPT_NAME, skdData.dept));        
        sb.append(OverrideData.formatToken(SKDDET_WRKD_JOB_NAME, skdData.jobName));

        tmpDateString = OVERRIDE_TIME_FORMAT.format( DateHelper.parseDate(skdData.jobStartTime, inputDateFormat) );
        sb.append(OverrideData.formatToken(SKDDET_JT_START_TIME, tmpDateString));
        
        tmpDateString = OVERRIDE_TIME_FORMAT.format( DateHelper.parseDate(skdData.jobEndTime, inputDateFormat) );
        sb.append(OverrideData.formatToken(SKDDET_JT_END_TIME, tmpDateString));
        
        // Remove last coma inserted by formatToken
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        
        return sb.toString();
    }
    
    
    /**
     * Insert override in db. Also if applicable, add the ovr to the list to be calculated
     * at the end of the task.
     * 
     */
    protected void addOverride( int ovrTypId, ScheduleTransactionMapper stMapper, SkdData skdData )
            throws Exception {

        String ovrNewValue = getOvrNewValue( skdData );
        
        if (ovrNewValue == null) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
                logger.debug( "SLScheduleTransaction.addOverride: ovrNewValue is null, no override to add." );
            }            
            return;
        }
        
        Date workDate = DateHelper.parseDate(skdData.skdDate, ovrDateFormat);
        
        OverrideData ovr = new OverrideData();        
        ovr.setEmpId( stMapper.getEmpId() );
        ovr.setOvrCreateDate( new java.util.Date() );
        ovr.setOvrStartDate( workDate );
        ovr.setOvrEndDate( workDate );
        ovr.setOvrStartTime( DateHelper.parseDate(skdData.skdStartTime1, inputDateFormat) );
        ovr.setOvrEndTime( DateHelper.parseDate(skdData.skdEndTime1, inputDateFormat) );
        ovr.setOvrStatus( OverrideData.PENDING );
        ovr.setOvrtypId( ovrTypId );
        ovr.setOvrNewValue( ovrNewValue);
        ovr.setWbuNameBoth( ovrWbuName, ovrWbuName );
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
            logger.debug("Creating override with new value : " + ovrNewValue);
        }
        
        new OverrideAccess(conn).insert( ovr , false);

        // Only recalculate overrides that are within the days limit 
        if( !workDate.after(lastDateForscheduleAdjust) ) {
            addToEmpIdAndDates( stMapper.getEmpId(), workDate );
        }
   }    
 
	
	/**
     * Deleting (cancelling) a schedule override. Cancelled ovr are recalculated immediatly.  
     *  
	 */
	public void deleteScheduleDetailOvr(int empId, SkdData skdData) throws WBInterfaceException, SQLException {
		if (logger.isDebugEnabled()) {
            logger.debug("SLScheduleTransaction.deleteScheduleDetailOvr");
            logger.debug("***** START DATE/TIME: " + skdData.skdStartTime1);
            logger.debug("***** END DATE/TIME: " + skdData.skdEndTime1);
        }
		
        Date workDate = DateHelper.parseDate(skdData.skdDate, ovrDateFormat);
        
        OverrideAccess oa = new OverrideAccess(conn);
        OverrideList ol = oa.loadAffectingOverrides(
                empId, workDate,
                OverrideData.SCHEDULE_DETAIL_TYPE, OverrideData.SCHEDULE_DETAIL_TYPE,
                OverrideAccess.NON_AFFECTING_OVERRIDE_STATUS
                );
        
        Date startDateTime =  WBInterfaceUtil.parseDateTime(skdData.skdStartTime1, inputDateFormat, "Error parsing start time");
        Date endDateTime  =  WBInterfaceUtil.parseDateTime(skdData.skdEndTime1, inputDateFormat, "Error parsing end time");
        String ovrTeamToken = "WRKD_WBT_NAME=" + skdData.wbtTeam;
        
		for( int i=0; i < ol.size(); i++ ){
            OverrideData od = ol.getOverrideData(i);
            
            // Delete (cancel) if override match times and wb_team criteria
            if( startDateTime.equals(od.getOvrStartTime()) && endDateTime.equals(od.getOvrEndTime()) ) {
                if( od.getOvrNewValue().indexOf(ovrTeamToken) != -1 ) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("***** Cancelling ovr_id: " + od.getOvrId());
                    }
                    od.setOvrStatusCancel("Cancelled by SL Interface.");
                    od.setOvrCancelledBy("SL_INTERFACE");
                    od.setOvrCancelledByA("SL_INTERFACE");
                    od.setUpdated(true);
                    oa.save(od);
                }
            }            
		}
	}
	
    public String getTaskUI() {
        return "/jobs/wbinterface/SLScheduleParams.jsp";
    }
    
    
    /**
     * This method will be called each time a transaction is initialized
     *
     */
    public void initializeTransaction(DBConnection conn) throws Exception {
        super.initializeTransaction( conn );
        int maxDaysForScheduleAdjust;
        
        String sMaxDaysForScheduleAdjust = (String)params.get(PARAM_NAME_MAX_DAYS_FOR_SCHED_ADJUST);
        try {
            maxDaysForScheduleAdjust = Integer.parseInt(sMaxDaysForScheduleAdjust);
        } catch( NumberFormatException ne ) {
            maxDaysForScheduleAdjust = DEFAULT_MAX_DAYS_FOR_SCHED_ADJUST;
        }
        
        lastDateForscheduleAdjust = DateHelper.addDays(DateHelper.getCurrentDate(), maxDaysForScheduleAdjust);        
    }    
    
    
    /**
     * This method will be called each time a transaction is finalized
     *
     */
    
    public void finalizeTransaction(DBConnection conn) throws Exception {
        calculateEmpIdAndDates( true );        
        super.finalizeTransaction( conn );
    }

    
    class SkdData {
        String skdDetColumn     = null;
        String skdDate          = null;
        String skdStartTime1    = null;
        String skdEndTime1      = null;
        String action           = null;
        String jobName          = null;
        String jobStartTime     = null;
        String jobEndTime       = null;
        String dept             = null;
        String wbtTeam          = null;
        
        public void initialize( ImportData data ) {
            skdDetColumn    = data.getField(SCHEDULE_DETAIL);
            skdDate         = data.getField(ScheduleTransaction.SCHEDULE_DATE);
            skdStartTime1   = data.getField(ScheduleTransaction.SCHEDULE_START);
            skdEndTime1     = data.getField(ScheduleTransaction.SCHEDULE_END);
            action          = data.getField(ScheduleTransaction.SCHEDULE_FLAGS);
            jobName         = data.getField(ScheduleTransaction.SCHEDULE_UDF1);
            jobStartTime    = data.getField(ScheduleTransaction.SCHEDULE_UDF2);
            jobEndTime      = data.getField(ScheduleTransaction.SCHEDULE_UDF3);
            dept            = data.getField(ScheduleTransaction.SCHEDULE_UDF4);
            wbtTeam         = data.getField(ScheduleTransaction.SCHEDULE_UDF5);
            
            if( action == null ) {
                action = "";
            }
        }

    }
}
