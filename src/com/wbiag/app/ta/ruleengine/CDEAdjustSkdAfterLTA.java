package com.wbiag.app.ta.ruleengine;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.workbrain.app.modules.retailSchedule.services.SOEmployeeTerminationHandler;
import com.workbrain.app.ta.db.OverrideAccess;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.OverrideList;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.WBException;

/**
 * Remove schedule detail and recreate unassigned shift after inserting a full day LTA
 * 
 */

public class CDEAdjustSkdAfterLTA {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDEAdjustSkdAfterLTA.class);

    // Safeguard against long or runaway process
    private static int MAX_NUMBER_OF_DAYS_TO_PROCESS = 30;
    private static final String OVR_COMMENT = "";
    private static final String OVR_WBU_NAME = "DataEventAdjustSkdAfterLTA";
    
    // Exclude all status exception of UNPUBLISHED and UNPUBLISHED_WARNING
    private final static String[] EXCLUDE_STATUS = {
                    OverrideData.PENDING, 
                    OverrideData.APPLIED,
                    OverrideData.HOLDING,
                    OverrideData.WARNING,
                    OverrideData.ERROR, 
                    OverrideData.CANCELLED,
                    OverrideData.CANCEL,
                    OverrideData.WARNING
                    };
    
    public static void process( OverrideData data, DBConnection c ) 
        throws SQLException {
        
        int empId = data.getEmpId();
        java.util.Date startDate = data.getOvrStartDate();
        java.util.Date endDate = data.getOvrEndDate();
        
        OverrideAccess oa = new OverrideAccess( c );

        int maxNumberOfDaysToProcess = Math.min( DateHelper.dateDifferenceInDays(endDate, startDate), MAX_NUMBER_OF_DAYS_TO_PROCESS ); 
        java.util.Date curDate = startDate;
        
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  logger.debug("ItellaDEAdjustSkdAfterLTA.removeScheduleDetail(): for emp_id " + empId + " ,from " + startDate + " to " + endDate );
        
        // Need to loop per day because there is no method in OverrideAccess that allows to 
        // load by date range *AND* (excluded) ovr_status
        for( int dayCount=0; dayCount <= maxNumberOfDaysToProcess; dayCount++ ) {
            OverrideList ol = oa.loadAffectingOverrides(
                empId, curDate,
                OverrideData.SCHEDULE_DETAIL_TYPE, OverrideData.SCHEDULE_DETAIL_TYPE,
                EXCLUDE_STATUS
                );
            // Recreate the unassigned shift for the day
            if( ol.size() > 0 ) {
                try {
                    if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  logger.debug("ItellaDEAdjustSkdAfterLTA.recreateUnassignedShift(): for emp_id " + empId + ", from " + startDate + " to " + endDate );                    
                    SOEmployeeTerminationHandler soeth = new SOEmployeeTerminationHandler();
                    // Set connection cache for SOEmployeeTerminationHandler
                    if ( !DBConnection.isCurrentConnectionSet() ) {
                        DBConnection.setCurrentConnection(c);
                    }                    
                    soeth.execute(empId, curDate, curDate);
                } catch (WBException wbe) {
                    throw new SQLException( wbe.getMessage() );
                }
            }                      
            // Cancel all unpublished schedule detail override for the day. This will also remove
            // employee_sched_dtl and emp_schd_dtl_layer.
            for( int i=0; i < ol.size(); i++ ) {
                OverrideData od = ol.getOverrideData(i);
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  logger.debug("ItellaDEAdjustSkdAfterLTA.removeScheduleDetail(): cancelling unpublished ovr_id " + od.getOvrId() );                
                od.setOvrStatusCancel(OVR_COMMENT);
                od.setOvrCancelledBy( OVR_WBU_NAME );
                od.setOvrCancelledByA( OVR_WBU_NAME);
                od.setUpdated( true );
                oa.save( od );
            }
            curDate = DateHelper.addDays(curDate, 1);
        } // for dayCount        
    
        // TEMP WORKAROUND -- NEED TO REMOVE
        tempWorkaroundToRemoveTerminationText( c );        
    }

    /*
     * THIS SHOULD BE REMOVED once R&D fix SOEmployeeTerminationHandler to accept
     * a status flag instead of assuming the shift is recreated because of employee
     * termination. 
     */
    private static void tempWorkaroundToRemoveTerminationText( DBConnection c ) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = c.prepareStatement("update so_shift_detail set shftdet_edited=1 where shftdet_edited=2");
            ps.executeUpdate();
        }
        finally {
            SQLHelper.cleanUp(ps);
        }        
    }
    

}
