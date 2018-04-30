/*
 * Created on Feb 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.wbiag.server.data.source;

import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

import java.util.*;
import java.sql.*;

/**
 * @author wwoo
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SOPlannedVsActualRowSource extends AbstractRowSource
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( SOPlannedVsActualRowSource.class );
    private RowDefinition rowDefinition;
    private DBConnection conn;

    private java.util.List rows = new ArrayList();

    // Parameters from the maintenance form
    private String locationID;
    private Datetime startDate, endDate;
    
    // Date format
    private final String DATE_FORMAT =  "yyyyMMdd HHmmss";

    // Number of columns capture in this query
    private final int SO_REPORT_COL_COUNT = 6;
    
    // *** Define column names here
    {
        RowStructure rowstruc = new RowStructure( SO_REPORT_COL_COUNT );
        rowstruc.add( "DEPT_NAME", CharType.get(100) );
        rowstruc.add( "SHIFT_DATE", CharType.get(100) );
        rowstruc.add( "TOTAL_SKD_HOURS", CharType.get(100) );
        rowstruc.add( "ACTUAL_REG", CharType.get(100) );
        rowstruc.add( "ACTUAL_OT", CharType.get(100) );
        rowstruc.add( "TOTAL_ACT_HOURS", CharType.get(100) );
        rowstruc.add( "TOTAL_HOURS_RATIO", CharType.get(100) );
        rowstruc.add( "TOTAL_SKD_PAYROLL", CharType.get(100) );
        rowstruc.add( "TOTAL_ACT_PAYROLL", CharType.get(100) );
        rowstruc.add( "TOTAL_PAYROLL_RATIO", CharType.get(100) );
        //rowstruc.add( "BUDGETED_LABOR", CharType.get(100) );
        //rowstruc.add( "BUDGET_VS_SKD_RATIO", CharType.get(100) );
        rowDefinition = new RowDefinition(-1,rowstruc);
    }
    
    /**
     * 
     */
    public SOPlannedVsActualRowSource( DBConnection conn, com.workbrain.server.data.ParameterList list ) throws AccessException
    {
        //set the connection object passed by the Builder program
        this.conn = conn;

        //put parameters into String object
        String locationIDParam = ( String )list.findParam( "locationID" ).getValue();
        String startDateParam = ( String )list.findParam( "startDate" ).getValue();
        String endDateParam = ( String )list.findParam( "endDate" ).getValue();

        if( logger.isDebugEnabled() )
        {
            logger.debug( "*** PARAM 1 - LOCATION ID: " + locationIDParam + " ***" );
            logger.debug( "*** PARAM 2 - START DATE:  " + startDateParam + " ***" );
            logger.debug( "*** PARAM 3 - END DATE: " + endDateParam + " ***" );
        }

        // *** do not attempt if params are null or strings like #request.TextBox#
        if(( locationIDParam == null || startDateParam == null ||  endDateParam == null )
                || (locationIDParam.indexOf( "#" ) != -1 || startDateParam.indexOf( "#" ) != -1 || endDateParam.indexOf( "#" ) != -1 ) )
        {
            //return;
        }
        
        this.locationID = locationIDParam ;
        
        // Set the start date
        if( startDateParam == null || startDateParam.indexOf("#") != -1)
        {
            this.startDate = null;
        }
        else
        {
            this.startDate = DateHelper.parseDate( startDateParam.trim(), DATE_FORMAT );
        }

        // Set the end date
        if( endDateParam == null || endDateParam.indexOf("#") != -1)
        {
            this.endDate = null;
        }
        else
        {
            this.endDate = DateHelper.parseDate( endDateParam.trim(), DATE_FORMAT );
        }

        if( logger.isDebugEnabled() )
        {
            logger.debug( "##########################################" );
            logger.debug( "*** RESOLVED - START DATE:  " + startDate.toLocaleString() + " ***" );
            logger.debug( "*** RESOLVED - END DATE: " + endDate.toLocaleString() + " ***" );
            logger.debug( "##########################################" );
        }
        
        loadRows();
    }
    
    private void loadRows() throws AccessException
    {
        PreparedStatement ps = null;
        ResultSet rs = null;

        //Get query string
        String sql = buildQuery( this.locationID );
        logger.error( "Query: " + sql );

        try
        {
            DBServer dbs = DBServer.getServer( this.conn );
            rows.clear();
            
            ps = this.conn.prepareStatement( sql );
            ps.setTimestamp( 1, new Timestamp( this.startDate.getTime() ) );
            ps.setTimestamp( 2, new Timestamp( this.endDate.getTime() ) );

            /*
            if( dateRequest != null ){
            }
            */

            rs = ps.executeQuery();

            if( rs == null )
            {
                return;
            }
            while( rs.next() )
            {
                // TODO: Perform Calculations for the Report columns here
                Row r = new BasicRow( getRowDefinition() );
                r.setValue( "DEPT_NAME" , rs.getString( "DEPT_NAME" ) );
                r.setValue( "SHIFT_DATE" , rs.getString( "SHIFT_DATE" ) );
                r.setValue( "TOTAL_SKD_HOURS" , rs.getString( "TOTAL_SKD_HOURS" ) );
                r.setValue( "ACTUAL_REG" , rs.getString( "ACTUAL_REG" ) );
                r.setValue( "ACTUAL_OT" , rs.getString( "ACTUAL_OT" ) );
                r.setValue( "TOTAL_ACT_HOURS" , rs.getString( "TOTAL_ACT_HOURS" ) );
                r.setValue( "TOTAL_HOURS_RATIO" , rs.getString( "TOTAL_HOURS_RATIO" ) );
                r.setValue( "TOTAL_SKD_PAYROLL" , rs.getString( "TOTAL_SKD_PAYROLL" ) );
                r.setValue( "TOTAL_ACT_PAYROLL" , rs.getString( "TOTAL_ACT_PAYROLL" ) );
                r.setValue( "TOTAL_PAYROLL_RATIO" , rs.getString( "TOTAL_PAYROLL_RATIO" ) );
                //r.setValue( "BUDGETED_LABOR" , rs.getString( "BUDGETED_LABOR" ) );
                
                rows.add( r );
            }
        }
        catch( SQLException e )
        {
            if( logger.isEnabledFor( org.apache.log4j.Level.ERROR ) ) { logger.error( "SQLException at LoadRows: " + e ); }
        }
        finally
        {
            SQLHelper.cleanUp( ps, rs );
        }
    }

    private String buildQuery( String inClause )
    {
        StringBuffer sqlBuffer = new StringBuffer();
        DBServer dbs = DBServer.getServer( this.conn );
/*
        sqlBuffer.append( "SELECT " );
        sqlBuffer.append( "    LOCATION.SKDGRP_NAME AS DEPT_NAME, " );
        sqlBuffer.append( "    SHIFT_DATE, " );
        sqlBuffer.append( "    " + dbs.getToChar( "(SCHEDULED_REG + SCHEDULED_OT)" ) + " AS TOTAL_SKD_HOURS, " );
        sqlBuffer.append( "    ACTUAL_REG, " );
        sqlBuffer.append( "    ACTUAL_OT, " );
        sqlBuffer.append( "    " + dbs.getToChar( "(ACTUAL_REG + ACTUAL_OT)" ) + " AS TOTAL_ACT_HOURS, " );
        sqlBuffer.append( "    " + dbs.getToChar( "((ACTUAL_REG + ACTUAL_OT) / (SCHEDULED_REG + SCHEDULED_OT))" ) + " AS TOTAL_HOURS_RATIO, " );
        sqlBuffer.append( "    " + dbs.getToChar( "(SCHEDULED_REG_PAYROLL + SCHEDULED_OT_PAYROLL)" ) + " AS TOTAL_SKD_PAYROLL, " );
        sqlBuffer.append( "    " + dbs.getToChar( "(ACTUAL_REG_PAYROLL + ACTUAL_OT_PAYROLL)" ) + " AS TOTAL_ACT_PAYROLL, " );
        sqlBuffer.append( "    " + dbs.getToChar( "((ACTUAL_REG_PAYROLL + ACTUAL_OT_PAYROLL) / (SCHEDULED_REG_PAYROLL + SCHEDULED_OT_PAYROLL))" ) + " AS TOTAL_PAYROLL_RATIO " );
        
        sqlBuffer.append( "FROM " );

        sqlBuffer.append( "    (SELECT " );
        sqlBuffer.append( "        SKD_SUM.SHIFT_DATE, " );
        sqlBuffer.append( "        " + dbs.getToChar( "SUM(SKD_SUM.REG_HOURS)" ) + " AS SCHEDULED_REG, " );
        sqlBuffer.append( "        " + dbs.getToChar( "SUM(SKD_SUM.OT_HOURS)" ) + " AS SCHEDULED_OT, " );
        sqlBuffer.append( "        " + dbs.getToChar( "SUM(SKD_SUM.REG_PAYROLL)" ) + " AS SCHEDULED_REG_PAYROLL, " );
        sqlBuffer.append( "        " + dbs.getToChar( "SUM(SKD_SUM.OT_PAYROLL)" ) + " AS SCHEDULED_OT_PAYROLL " );
        sqlBuffer.append( "    FROM " );
        sqlBuffer.append( "        ((SELECT " );
        sqlBuffer.append( "            WORKED.EMP_ID, " );
        sqlBuffer.append( "            WORKED.SHIFT_DATE, " );
        sqlBuffer.append( "            " + dbs.getToChar( "WORKED.HOURS_SKD" ) + " AS REG_HOURS, " );
        sqlBuffer.append( "            " + dbs.getToChar( "0" ) + " AS OT_HOURS, " );
        sqlBuffer.append( "            " + dbs.getToChar( "(WORKED.HOURS_SKD * E.EMP_BASE_RATE)" ) + " AS REG_PAYROLL, " );
        sqlBuffer.append( "            " + dbs.getToChar( "0" ) + " AS OT_PAYROLL " );
        sqlBuffer.append( "        FROM " );
        sqlBuffer.append( "            (SELECT EMP_ID, " + dbs.getToChar( "SUM(SHFTACT_LEN)" ) + " AS HOURS_SKD, SKDSHFT_DATE AS SHIFT_DATE, JOB_ID " );
        sqlBuffer.append( "            FROM VIEW_SO_ACTSFTHR " );
        sqlBuffer.append( "            WHERE ACT_WORKING = 1 " );
        sqlBuffer.append( "            AND EMP_ID IS NOT NULL " );
        sqlBuffer.append( "            GROUP BY EMP_ID, SKDSHFT_DATE, JOB_ID " );
        sqlBuffer.append( "            ORDER BY SKDSHFT_DATE, EMP_ID) WORKED, JOB_TEAM JT, EMPLOYEE E " );
        sqlBuffer.append( "        WHERE " );
        sqlBuffer.append( "            JT.JOB_ID = WORKED.JOB_ID " );
        sqlBuffer.append( "            AND WORKED.EMP_ID = E.EMP_ID " );
        sqlBuffer.append( "            AND ( WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH ) <= JT.JOBTEAM_DOT_THRESH) " );

        sqlBuffer.append( "    UNION " );
        
        sqlBuffer.append( "    (SELECT " );
        sqlBuffer.append( "        WORKED.EMP_ID, " );
        sqlBuffer.append( "        WORKED.SHIFT_DATE, " );
        sqlBuffer.append( "        " + dbs.getToChar( "0" ) + " AS REG_HOURS, " );
        sqlBuffer.append( "        " + dbs.getToChar( "(WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH)" ) + " AS OT_HOURS, " );
        sqlBuffer.append( "        " + dbs.getToChar( "0" ) + " AS REG_PAYROLL, " );
        sqlBuffer.append( "        " + dbs.getToChar( "((WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH) * E.EMP_BASE_RATE * JT.JOBTEAM_OT_MULT)" ) + " AS OT_PAYROLL " );
        sqlBuffer.append( "    FROM " );
        sqlBuffer.append( "        (SELECT EMP_ID, " + dbs.getToChar( "SUM(SHFTACT_LEN)" ) + " AS HOURS_SKD, SKDSHFT_DATE AS SHIFT_DATE, JOB_ID " );
        sqlBuffer.append( "         FROM VIEW_SO_ACTSFTHR " );
        sqlBuffer.append( "         WHERE ACT_WORKING = 1 " );
        sqlBuffer.append( "         AND EMP_ID IS NOT NULL " );
        sqlBuffer.append( "         GROUP BY EMP_ID, SKDSHFT_DATE, JOB_ID " );
        sqlBuffer.append( "         ORDER BY SKDSHFT_DATE, EMP_ID) WORKED, JOB_TEAM JT, EMPLOYEE E " );
        sqlBuffer.append( "    WHERE JT.JOB_ID = WORKED.JOB_ID " );
        sqlBuffer.append( "    AND WORKED.EMP_ID = E.EMP_ID " );
        sqlBuffer.append( "    AND ( WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH ) > 0 )) SKD_SUM " );
        sqlBuffer.append( "    GROUP BY SKD_SUM.SHIFT_DATE) SKD_SUM_TABLE, " ); 
            
        sqlBuffer.append( "    (SELECT " );
        sqlBuffer.append( "        WRKS_WORK_DATE, " );
        sqlBuffer.append( "        " + dbs.getToChar( "SUM(REG_HOURS)" ) + " AS ACTUAL_REG, " );
        sqlBuffer.append( "        " + dbs.getToChar( "SUM((REG_HOURS * REG_RATE))" ) + " AS ACTUAL_REG_PAYROLL, " );
        sqlBuffer.append( "        " + dbs.getToChar( "SUM(OT_HOURS)" ) + " AS ACTUAL_OT, " );
        sqlBuffer.append( "        " + dbs.getToChar( "SUM((OT_HOURS * OT_RATE))" ) + " AS ACTUAL_OT_PAYROLL, " );
        sqlBuffer.append( "        " + dbs.getToChar( "SUM(EMPLOYEE_PAY)" ) + " AS TOTAL_EMP_PAYROLL " );
        sqlBuffer.append( "    FROM VIEW_SO_EMP_WORK " );
        sqlBuffer.append( "    GROUP BY WRKS_WORK_DATE) ACT_SUM_TABLE, " );
        
        sqlBuffer.append( "    SO_SCHEDULE_GROUP LOCATION " );
        sqlBuffer.append( "WHERE " );
        sqlBuffer.append( "    LOCATION.SKDGRP_ID IN ( ? ) " );
        sqlBuffer.append( "    AND SKD_SUM_TABLE.SHIFT_DATE BETWEEN ? AND ? " );
        sqlBuffer.append( "    AND SKD_SUM_TABLE.SHIFT_DATE = ACT_SUM_TABLE.WRKS_WORK_DATE " );


*/
        sqlBuffer.append( "SELECT " );
        sqlBuffer.append( "    LOCATION.SKDGRP_NAME AS DEPT_NAME, " );
        sqlBuffer.append( "    SHIFT_DATE, " );
        sqlBuffer.append( "    (SCHEDULED_REG + SCHEDULED_OT) AS TOTAL_SKD_HOURS, " );
        sqlBuffer.append( "    ACTUAL_REG, " );
        sqlBuffer.append( "    ACTUAL_OT, " );
        sqlBuffer.append( "    (ACTUAL_REG + ACTUAL_OT) AS TOTAL_ACT_HOURS, " );
        sqlBuffer.append( "    ((ACTUAL_REG + ACTUAL_OT) / (SCHEDULED_REG + SCHEDULED_OT)) AS TOTAL_HOURS_RATIO, " );
        sqlBuffer.append( "    (SCHEDULED_REG_PAYROLL + SCHEDULED_OT_PAYROLL) AS TOTAL_SKD_PAYROLL, " );
        sqlBuffer.append( "    (ACTUAL_REG_PAYROLL + ACTUAL_OT_PAYROLL) AS TOTAL_ACT_PAYROLL, " );
        sqlBuffer.append( "    ((ACTUAL_REG_PAYROLL + ACTUAL_OT_PAYROLL) / (SCHEDULED_REG_PAYROLL + SCHEDULED_OT_PAYROLL)) AS TOTAL_PAYROLL_RATIO " );
        //sqlBuffer.append( "    SKDCOST_COST AS BUDGETED_LABOR " );
        
        sqlBuffer.append( "FROM " );

        sqlBuffer.append( "    (SELECT " );
        sqlBuffer.append( "        SKD_SUM.SKDGRP_ID, " );
        sqlBuffer.append( "        SKD_SUM.SHIFT_DATE, " );
        sqlBuffer.append( "        SUM(SKD_SUM.REG_HOURS) AS SCHEDULED_REG, " );
        sqlBuffer.append( "        SUM(SKD_SUM.OT_HOURS) AS SCHEDULED_OT, " );
        sqlBuffer.append( "        SUM(SKD_SUM.REG_PAYROLL) AS SCHEDULED_REG_PAYROLL, " );
        sqlBuffer.append( "        SUM(SKD_SUM.OT_PAYROLL) AS SCHEDULED_OT_PAYROLL " );
        sqlBuffer.append( "    FROM " );
        sqlBuffer.append( "        ((SELECT " );
        sqlBuffer.append( "            WORKED.SKDGRP_ID, " );
        sqlBuffer.append( "            WORKED.EMP_ID, " );
        sqlBuffer.append( "            WORKED.SHIFT_DATE, " );
        sqlBuffer.append( "            WORKED.HOURS_SKD AS REG_HOURS, " );
        sqlBuffer.append( "            0 AS OT_HOURS, " );
        sqlBuffer.append( "            (WORKED.HOURS_SKD * E.EMP_BASE_RATE) AS REG_PAYROLL, " );
        sqlBuffer.append( "            0 AS OT_PAYROLL " );
        sqlBuffer.append( "        FROM " );
        sqlBuffer.append( "            (SELECT SKDGRP_ID, EMP_ID, SUM(SHFTACT_LEN) AS HOURS_SKD, SKDSHFT_DATE AS SHIFT_DATE, JOB_ID " );
        sqlBuffer.append( "            FROM VIEW_SO_ACTSFTHR " );
        sqlBuffer.append( "            WHERE ACT_WORKING = 1 " );
        sqlBuffer.append( "            AND EMP_ID IS NOT NULL " );
        sqlBuffer.append( "            GROUP BY SKDGRP_ID, EMP_ID, SKDSHFT_DATE, JOB_ID " );
        sqlBuffer.append( "            ORDER BY SKDSHFT_DATE, EMP_ID) WORKED, JOB_TEAM JT, EMPLOYEE E " );
        sqlBuffer.append( "        WHERE " );
        sqlBuffer.append( "            JT.JOB_ID = WORKED.JOB_ID " );
        sqlBuffer.append( "            AND WORKED.EMP_ID = E.EMP_ID " );
        sqlBuffer.append( "            AND ( WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH ) <= JT.JOBTEAM_DOT_THRESH) " );

        sqlBuffer.append( "    UNION " );
        
        sqlBuffer.append( "    (SELECT " );
        sqlBuffer.append( "        WORKED.SKDGRP_ID, " );
        sqlBuffer.append( "        WORKED.EMP_ID, " );
        sqlBuffer.append( "        WORKED.SHIFT_DATE, " );
        sqlBuffer.append( "        0 AS REG_HOURS, " );
        sqlBuffer.append( "        (WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH) AS OT_HOURS, " );
        sqlBuffer.append( "        0 AS REG_PAYROLL, " );
        sqlBuffer.append( "        ((WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH) * E.EMP_BASE_RATE * JT.JOBTEAM_OT_MULT) AS OT_PAYROLL " );
        sqlBuffer.append( "    FROM " );
        sqlBuffer.append( "        (SELECT SKDGRP_ID, EMP_ID, SUM(SHFTACT_LEN) AS HOURS_SKD, SKDSHFT_DATE AS SHIFT_DATE, JOB_ID " );
        sqlBuffer.append( "         FROM VIEW_SO_ACTSFTHR " );
        sqlBuffer.append( "         WHERE ACT_WORKING = 1 " );
        sqlBuffer.append( "         AND EMP_ID IS NOT NULL " );
        sqlBuffer.append( "         GROUP BY SKDGRP_ID, EMP_ID, SKDSHFT_DATE, JOB_ID " );
        sqlBuffer.append( "         ORDER BY SKDSHFT_DATE, EMP_ID) WORKED, JOB_TEAM JT, EMPLOYEE E " );
        sqlBuffer.append( "    WHERE JT.JOB_ID = WORKED.JOB_ID " );
        sqlBuffer.append( "    AND WORKED.EMP_ID = E.EMP_ID " );
        sqlBuffer.append( "    AND ( WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH ) > 0 )) SKD_SUM " );
        sqlBuffer.append( "    GROUP BY SKD_SUM.SKDGRP_ID, SKD_SUM.SHIFT_DATE) SKD_SUM_TABLE, " ); 
            
        sqlBuffer.append( "    (SELECT " );
        sqlBuffer.append( "        SKDGRP_ID, " );
        sqlBuffer.append( "        WRKS_WORK_DATE, " );
        sqlBuffer.append( "        SUM(REG_HOURS) AS ACTUAL_REG, " );
        sqlBuffer.append( "        SUM((REG_HOURS * REG_RATE)) AS ACTUAL_REG_PAYROLL, " );
        sqlBuffer.append( "        SUM(OT_HOURS) AS ACTUAL_OT, " );
        sqlBuffer.append( "        SUM((OT_HOURS * OT_RATE)) AS ACTUAL_OT_PAYROLL, " );
        sqlBuffer.append( "        SUM(EMPLOYEE_PAY) AS TOTAL_EMP_PAYROLL " );
        sqlBuffer.append( "    FROM VIEW_SO_EMP_WORK " );
        sqlBuffer.append( "    GROUP BY SKDGRP_ID, WRKS_WORK_DATE) ACT_SUM_TABLE, " );
        
        sqlBuffer.append( "    SO_SCHEDULE_GROUP LOCATION " );
        //sqlBuffer.append( "    SO_SCHEDULE SKD, " );
        //sqlBuffer.append( "    SO_SCHEDULE_COST SKDCOST " );
        sqlBuffer.append( "WHERE " );
        sqlBuffer.append( "    SKD_SUM_TABLE.SHIFT_DATE BETWEEN ? AND ? " );
        sqlBuffer.append( "    AND SKD_SUM_TABLE.SHIFT_DATE = ACT_SUM_TABLE.WRKS_WORK_DATE " );
        sqlBuffer.append( "    AND SKD_SUM_TABLE.SKDGRP_ID = ACT_SUM_TABLE.SKDGRP_ID " );
        sqlBuffer.append( "    AND ACT_SUM_TABLE.SKDGRP_ID = LOCATION.SKDGRP_ID " );
        //sqlBuffer.append( "    AND SKD.SKDGRP_ID = LOCATION.SKDGRP_ID " );
        //sqlBuffer.append( "    AND SKD.SKD_ID = SKDCOST.SKD_ID " );
        sqlBuffer.append( "    AND LOCATION.SKDGRP_ID IN (" + inClause + ")" );

        /*
         * SQL statement for this LFSO report
SELECT
    SHIFT_DATE,
    (SCHEDULED_REG + SCHEDULED_OT) AS TOTAL_SKD_HOURS,
    ACTUAL_REG,
    ACTUAL_OT,
    (ACTUAL_REG + ACTUAL_OT) AS TOTAL_ACT_HOURS,
    ((ACTUAL_REG + ACTUAL_OT) / (SCHEDULED_REG + SCHEDULED_OT)) AS TOTAL_HOURS_RATIO,
    (SCHEDULED_REG_PAYROLL + SCHEDULED_OT_PAYROLL) AS TOTAL_SKD_PAYROLL,
    (ACTUAL_REG_PAYROLL + ACTUAL_OT_PAYROLL) AS TOTAL_ACT_PAYROLL,
    ((ACTUAL_REG_PAYROLL + ACTUAL_OT_PAYROLL) / (SCHEDULED_REG_PAYROLL + SCHEDULED_OT_PAYROLL)) AS TOTAL_PAYROLL_RATIO

FROM 

(SELECT
    SKD_SUM.SHIFT_DATE,
    SUM(SKD_SUM.REG_HOURS) AS SCHEDULED_REG,
    SUM(SKD_SUM.OT_HOURS) AS SCHEDULED_OT,
    SUM(SKD_SUM.REG_PAYROLL) AS SCHEDULED_REG_PAYROLL,
    SUM(SKD_SUM.OT_PAYROLL) AS SCHEDULED_OT_PAYROLL

FROM

    ((SELECT
        WORKED.EMP_ID,
        WORKED.SHIFT_DATE,
        WORKED.HOURS_SKD AS REG_HOURS,
        0 AS OT_HOURS,
        (WORKED.HOURS_SKD * E.EMP_BASE_RATE) AS REG_PAYROLL,
        0 AS OT_PAYROLL
    FROM
        (SELECT EMP_ID, SUM(SHFTACT_LEN) AS HOURS_SKD, SKDSHFT_DATE AS SHIFT_DATE, JOB_ID
         FROM VIEW_SO_ACTSFTHR
         WHERE ACT_WORKING = 1
         AND EMP_ID IS NOT NULL
         GROUP BY EMP_ID, SKDSHFT_DATE, JOB_ID
         ORDER BY SKDSHFT_DATE, EMP_ID) WORKED, JOB_TEAM JT, EMPLOYEE E
    WHERE JT.JOB_ID = WORKED.JOB_ID
    AND WORKED.EMP_ID = E.EMP_ID
    AND ( WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH ) <= JT.JOBTEAM_DOT_THRESH)

UNION

    (SELECT
        WORKED.EMP_ID,
        WORKED.SHIFT_DATE,
        0 AS REG_HOURS,
        (WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH) AS OT_HOURS,
        0 AS REG_PAYROLL,
        ((WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH) * E.EMP_BASE_RATE * JT.JOBTEAM_OT_MULT) AS OT_PAYROLL
    FROM
        (SELECT EMP_ID, SUM(SHFTACT_LEN) AS HOURS_SKD, SKDSHFT_DATE AS SHIFT_DATE, JOB_ID
         FROM VIEW_SO_ACTSFTHR
         WHERE ACT_WORKING = 1
         AND EMP_ID IS NOT NULL
         GROUP BY EMP_ID, SKDSHFT_DATE, JOB_ID
         ORDER BY SKDSHFT_DATE, EMP_ID) WORKED, JOB_TEAM JT, EMPLOYEE E
    WHERE JT.JOB_ID = WORKED.JOB_ID
    AND WORKED.EMP_ID = E.EMP_ID
    AND ( WORKED.HOURS_SKD - JT.JOBTEAM_DOT_THRESH ) > 0 )) SKD_SUM
    GROUP BY SKD_SUM.SHIFT_DATE) SKD_SUM_TABLE, 
    
    (SELECT
        WRKS_WORK_DATE,
        SUM(REG_HOURS) AS ACTUAL_REG,
        SUM((REG_HOURS * REG_RATE)) AS ACTUAL_REG_PAYROLL,
        SUM(OT_HOURS) AS ACTUAL_OT,
        SUM((OT_HOURS * OT_RATE)) AS ACTUAL_OT_PAYROLL,
        SUM(EMPLOYEE_PAY) AS TOTAL_EMP_PAYROLL
    FROM VIEW_SO_EMP_WORK
    GROUP BY WRKS_WORK_DATE) ACT_SUM_TABLE

WHERE
    SKD_SUM_TABLE.SHIFT_DATE BETWEEN TO_DATE('November 14, 2004, 12:00:00','Month dd, YYYY, HH:MI:SS')
    AND TO_DATE('November 21, 2004, 12:00:00','Month dd, YYYY, HH:MI:SS')
    AND SKD_SUM_TABLE.SHIFT_DATE = ACT_SUM_TABLE.WRKS_WORK_DATE
         * 
         */
        
        
        /*
        //retrieve or default common variables from registry if existing
        //If Regular Voucher, get emps with detail Voucher Flag checked
        if(optionSelection.equalsIgnoreCase(REGULAR_VOUCHER_STR)){
            sqlBuffer.append(" and " + dbs.encodeNullCheck(payExpHlp.WRKD_VOUCHER_FIELD,"'N'") + " = 'Y'");
        }
        
        //Disregard any previously Voucher Paid records
        sqlBuffer.append(" and " + dbs.encodeNullCheck(payExpHlp.WRKD_VOUCHER_PAID_FIELD, "'N'") + " = 'N' ");
        
        //If Regular Voucher, use requested date
        sqlBuffer.append(" and wrks_work_date = ");
        if(dateRequest == null){
            sqlBuffer.append(" wrks_work_date ");
        }else{
            sqlBuffer.append(" ? ");
        }

        //For Final Voucher get emps with flag checked
        if(!optionSelection.equalsIgnoreCase(REGULAR_VOUCHER_STR)){
            sqlBuffer.append(" and " + dbs.encodeNullCheck(payExpHlp.EMP_FINAL_VOUCHER_FIELD,"'N'") + " = 'Y'");
        }
        //Disregard any previously paid Final Voucher employees
        sqlBuffer.append(" and " + dbs.encodeNullCheck(payExpHlp.EMP_FINAL_VOUCHER_PAID_FIELD,"'N'") + " = 'N') ");
        
        return sqlBuffer.toString();    
        */
        
        return sqlBuffer.toString();
    }

    /* (non-Javadoc)
     * @see com.workbrain.server.data.RowSource#count(java.lang.String)
     */
    public int count( String where ) throws AccessException
    {
        // TODO Auto-generated method stub
        return rows.size();
    }

    /* (non-Javadoc)
     * @see com.workbrain.server.data.RowSource#count()
     */
    public int count() throws AccessException
    {
        // TODO Auto-generated method stub
        return rows.size();
    }

    /* (non-Javadoc)
     * @see com.workbrain.server.data.RowSource#query(java.lang.String[], java.lang.Object[])
     */
    public RowCursor query( String[] fields, Object[] values ) throws AccessException
    {
        // TODO Auto-generated method stub
        return queryAll();
    }

    /* (non-Javadoc)
     * @see com.workbrain.server.data.RowSource#query(java.util.List)
     */
    public RowCursor query( List keys ) throws AccessException
    {
        // TODO Auto-generated method stub
        return queryAll();
    }

    /* (non-Javadoc)
     * @see com.workbrain.server.data.RowSource#query(java.lang.String, java.lang.String)
     */
    public RowCursor query( String queryString, String orderByString ) throws AccessException
    {
        // TODO Auto-generated method stub
        return queryAll();
    }

    /* (non-Javadoc)
     * @see com.workbrain.server.data.RowSource#query(java.lang.String)
     */
    public RowCursor query( String queryString ) throws AccessException
    {
        // TODO Auto-generated method stub
        return queryAll();
    }

    /* (non-Javadoc)
     * @see com.workbrain.server.data.RowSource#queryAll()
     */
    public RowCursor queryAll() throws AccessException
    {
        return new AbstractRowCursor( getRowDefinition() )
        {
            private int counter = -1;
            protected Row getCurrentRowInternal() {
              return counter >= 0 && counter < rows.size() ? ( BasicRow )rows.get( counter ): null;
            }

            protected boolean fetchRowInternal() throws AccessException
            {
               return ++counter < rows.size();
            }
            public void close() { }
            };
    }

    /* (non-Javadoc)
     * @see com.workbrain.server.data.RowSource#getRowDefinition()
     */
    public RowDefinition getRowDefinition() throws AccessException
    {
        // TODO Auto-generated method stub
        return rowDefinition;
    }

}
