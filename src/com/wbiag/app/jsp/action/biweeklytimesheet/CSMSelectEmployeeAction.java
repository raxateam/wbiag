package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletRequest;

import com.workbrain.app.jsp.Action;
import com.workbrain.app.jsp.ActionContext;
import com.workbrain.app.jsp.action.dailytimesheet.TimesheetHelper;
import com.workbrain.app.jsp.action.timesheet.SelectionPage;
import com.workbrain.server.data.type.DatetimeType;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.TimeZoneUtil;

public class CSMSelectEmployeeAction implements Action {

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(CSMSelectEmployeeAction.class);

    protected DateFormat MMDDYYYY = new SimpleDateFormat( "MM/dd/yyyy" );

    public Object createRequest( ServletRequest request, DBConnection conn )
            throws Exception {
        return null;
    }

    public String process( DBConnection conn, ActionContext context,
                           Object requestObj )
            throws Exception {
        SelectionPage page = new SelectionPage();
        StringBuffer labels = new StringBuffer( "Manual Date Range" );
        StringBuffer values = new StringBuffer( DatetimeType.FORMAT.format(
                new java.util.Date(0) ) );
        Date sysDate = TimeZoneUtil.getSystemDateForTimeZone(context.getUserTimeZone());
        java.util.Date date = TimesheetHelper.getWeekStartDate(sysDate);

        String sqlQuery = "SELECT PAYGRP_START_DATE" +
	        " FROM EMPLOYEE_HISTORY EMPLOYEE, PAY_GROUP " +
            " WHERE EMPLOYEE.EMP_ID = ?" +
            " AND EMPLOYEE.PAYGRP_ID = PAY_GROUP.PAYGRP_ID " +
            " AND ? >=EMPLOYEE.EMPHIST_START_DATE " +
            " AND ? <=EMPLOYEE.EMPHIST_END_DATE ";
        ResultSet rs = null;
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sqlQuery);
            //CSM has confirmed that there is only going to be one paygroup(EVER!),
            //so basically, we can just search for the paygroup start date of the
            //person who logged in.
            //Here, we make the assumption that the SUP/PRC(the one who logged in)
            //belongs to the same paygroup as his/her employee
            //If there is going to be more than 1 paygroup, a master/slave DBLookup
            //will be required for the Biweekly Timesheet selection page.
            //In that case, it will no longer be correct to use context.getUserEmpId()
            //in this SQL query.
            stmt.setInt(1, Integer.parseInt(context.getUserEmpId()));

            stmt.setDate(2, new java.sql.Date(sysDate.getTime()));
            stmt.setDate(3, new java.sql.Date(sysDate.getTime()));
            rs = stmt.executeQuery();
            while( rs.next() ) {
                date = rs.getDate("PAYGRP_START_DATE");
                if(date!=null)
                	date = TimesheetHelper.getWeekStartDate(date);
            }
        } finally {
            SQLHelper.cleanUp(stmt, rs);
        }

        page.setWeekStartDate( date );
        for( int i = -3; i <= 12; i++ ) {
            java.util.Date start = DateHelper.addDays( date, i * CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET );
            java.util.Date end = DateHelper.addDays( start, CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET -1 );
            String label = MMDDYYYY.format( start ) + " - " +
                           MMDDYYYY.format( end ) + " ";
            if( i == 0 ) {
                label += "Current Pay Period";
            } else if( i > 0 ) {
                label += "Period +" + i;
            } else {
                label += "Period " + i;
            }
            labels.append( "," + label );
            values.append( "," + DatetimeType.FORMAT.format( start ) );
        }
        page.setWeekStartDateLabels( labels.toString() );
        page.setWeekStartDateValues( values.toString() );
        String wbuName = null;
        String wbuId = null;
        String wbuEmpId = null;
        try {
            wbuName = context.getUserName();
            wbuId = context.getUserId();
            wbuEmpId = context.getUserEmpId();
        } catch( Exception e ) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
        }
        if( wbuName == null ) {
            wbuName = "WORKBRAIN";
        }
        page.setWbuName( wbuName );
        page.setWbuId( wbuId );
        page.setWbuEmpId(wbuEmpId);
        context.setAttribute( "timesheet.select", page );
        return "/timesheet/biweeklySelection.jsp";
    }
}
