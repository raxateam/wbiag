package com.wbiag.app.ta.holiday;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.holiday.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.tool.overrides.*;
import com.workbrain.server.*;
import com.workbrain.tool.mail.Message;
/**
 * HolidayAssignmentTaskExt that runs
 *   - before N days a holiday happens
 *   - or for new employees created in the last variable # of days
 *	 - or for list of employees who's attributes have changed in the last variable # of days
 */
public class HolidayAssignmentTaskExt extends HolidayAssignmentTask
{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HolidayAssignmentTaskExt.class);

	public final static String EXACT_DAYS_TO_PROCESS_PARAM = "EXACT_DAYS_TO_PROCESS";
	public final static String RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM = "RUN_FOR_NEW_EMPLOYEES_ONLY";
	public final static String RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM = "RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES";
	public final static String CHECK_EMPLOYEES_DAYS_BACK_PARAM = "CHECK_EMPLOYEES_DAYS_BACK";
	public final static String EXACT = "exact";
	public final static String BETWEEN_FORWARD = "forward";
	public final static String BETWEEN_BACKWARD = "back";
	public final static String DAYS_TO_PROCESS = "99999";

    protected Date taskStartDate = DateHelper.getCurrentDate();
    protected Date taskStartDateTime = new Date(System.currentTimeMillis()) ;

    public Status run( int taskID, Map parameters )
    	throws Exception {
        int nDaysToProcessBefHol = 0;
        int nExactDaysToProcess = 0;
        int nCheckEmpsDaysBack = 0;
        String runForNewEmpsOnly = "";
        String runForChangedEmpsAttr = "";

        String sDaysToProcessBefHol = (String) parameters.get( DAYS_TO_PROCESS_PARAM );
        if(sDaysToProcessBefHol != null && !"".equals(sDaysToProcessBefHol))
        {
        	nDaysToProcessBefHol = Integer.parseInt( sDaysToProcessBefHol );
		}

        String sExactDaysToProcess = (String) parameters.get( EXACT_DAYS_TO_PROCESS_PARAM );
        if(sExactDaysToProcess != null && !"".equals(sExactDaysToProcess))
        {
        	nExactDaysToProcess = Integer.parseInt( sExactDaysToProcess );
        }

        runForNewEmpsOnly = (String) parameters.get( RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM );

        runForChangedEmpsAttr = (String) parameters.get( RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM );

		String sCheckEmpsDaysBack = (String) parameters.get( CHECK_EMPLOYEES_DAYS_BACK_PARAM );
		if(sCheckEmpsDaysBack != null && !"".equals(sCheckEmpsDaysBack))
        {
        	nCheckEmpsDaysBack = Integer.parseInt( sCheckEmpsDaysBack );
        }

		//run for exact holiday case
		if (nExactDaysToProcess > 0)
        {
            IntegerList holIds = getHolidayIdsDaysOffset(nExactDaysToProcess , EXACT);
            if (holIds.size() > 0)
            {
                parameters.put(DAYS_TO_PROCESS_PARAM , DAYS_TO_PROCESS);
                parameters.put(HOLIDAY_IDS_PARAM , StringHelper.createCSVForNumber(holIds));
                return super.run(taskID , parameters);
            }
            else
            {
				return jobOk("Holiday Assignment task ran for no employees.");
            }
        }

        Set empIdsToRun = new HashSet();
		//run for new employees case
        if ("Y".equals(runForNewEmpsOnly))
        {
            IntegerList newEmps = getNewEmpIds(nCheckEmpsDaysBack);
            empIdsToRun.addAll(newEmps);
        }

        //run for changed employees attributes
        if (!StringHelper.isEmpty(runForChangedEmpsAttr))
        {
	        EmployeeIdAndDateList  changedEmpIdDates = getChangedEmpIdDates(runForChangedEmpsAttr, nCheckEmpsDaysBack);
	        if (changedEmpIdDates.size() > 0)
	        {
                cancelChangedEmpIdDates(changedEmpIdDates);
                IntegerList changedEmpIds = new IntegerList();
                Iterator iter = changedEmpIdDates.iterator();
                while (iter.hasNext()) {
                    EmployeeIdAndDate item = (EmployeeIdAndDate)iter.next();
                    empIdsToRun.add(new Integer(item.getEmpId()));
                }
			}
    	}

        if (empIdsToRun.size() > 0) {
            StringBuffer message = new StringBuffer(200);
            try {
                message = assignForNewExistingEmployees(parameters, new ArrayList(empIdsToRun));
                if ( !isInterrupted() ) {
                    getConnection().commit();
                } else {
                    getConnection().rollback();
                    return jobInterrupted("Holiday Assignment task has been interrupted.");
                }
            }
            catch (Exception e) {
                message.append("<br>Holiday Assignment Process errored at: ").
                    append(new Date()).append("<br>");
                getLogger().error("Holiday Assisnment Ext", e);
                message.append(
                    "A general error occurred in the Holiday Assignment process:<br>");
                message.append(StringHelper.getStackTrace(e)).append("<br>");
                getConnection().rollback();
                return jobFailed("Holiday Assignment task failed.");
            }
            finally {
                String username = (String) parameters.get( USERNAME_PARAM );
                sendMessage(message.toString() , username);
                releaseConnection();
            }
            return jobOk("Holiday Assignment task finished for new and changed employees successfully.");
        }
        else
        {
            return jobOk("Holiday Assignment task ran for no employees.");
        }
    }


    public String getTaskUI()
    {
        return "/jobs/ta/holiday/holidayAssignmentParametersExt.jsp";
    }

    protected StringBuffer assignForNewExistingEmployees(Map parameters , List empIdsToRun) throws Exception {
        boolean holidayParam = WorkbrainParametersRetriever.getBoolean( "HOLIDAY/HOLIDAY_ASSIGNMENT_USE_HOLIDAY" , true );
        boolean holidayCalendarParam = WorkbrainParametersRetriever.getBoolean( "HOLIDAY/HOLIDAY_ASSIGNMENT_USE_CALENDAR" , false );
        if ( !holidayParam && !holidayCalendarParam ) {
            holidayParam = true;
        }
        Date startDate = DateHelper.getCurrentDate();
        Date endDate = DateHelper.getLatestEmployeeDate();

        String username = (String) parameters.get( USERNAME_PARAM );
        HolidayAssignmentExecution hae = new HolidayAssignmentExecution( getConnection(),
            username,
            username, this );

        if (holidayParam) {
            String sHolidayIds = (String) parameters.get( HOLIDAY_IDS_PARAM );
            int[] holidayIDs;
            if ( ALL_ITEMS.equalsIgnoreCase( sHolidayIds )) {
                holidayIDs = getAllHolidayIds( getConnection() );
            } else {
                holidayIDs = StringHelper.detokenizeStringAsIntArray( sHolidayIds, ",", true );
            }
            hae.assignHoliday( holidayIDs, empIdsToRun , startDate, endDate );
        }
        if ( holidayCalendarParam ) {
            String sHolidayCalendarIDs = (String) parameters.get( HOLIDAY_CALENDAR_IDS_PARAM );
            int[] holidayCalendarIDs;
            if ( ALL_ITEMS.equalsIgnoreCase( sHolidayCalendarIDs ) ) {
                holidayCalendarIDs = getAllHolidayCalendarIds( getConnection() );
            } else {
                holidayCalendarIDs = StringHelper.detokenizeStringAsIntArray( sHolidayCalendarIDs, ",", true );
            }
            hae.assignHolidayCalendar( holidayCalendarIDs, empIdsToRun,
                    startDate, endDate, false,
                    "Y".equalsIgnoreCase( (String) parameters.get( HOLIDAY_CALENDAR_CHECKBOX ) ));
        }
        Date taskEndDate = new Date();
        StringBuffer message = new StringBuffer(200);
        message.append( hae.getSuccessfullCount() ).append( " assignment(s) have been done successfully.<br>" );
        message.append( hae.getErrorCount() ).append( " assignment(s) had errors during execution.<br>" );

        if ( hae.getErrorCount() > 0 ) {
            message.append( "See Holiday Assignment Transaction maintenance form for error details.<br>" );
            createErrorTransactionAndLogs( hae.getErrorEmployees() );
        } else {
            createTransaction(HolAsgnTransAccess.STATUS_APPLIED,
                    "Holiday Assignment processed successfully for for new and changed employees.",
                    username,
                    taskEndDate );
        }
        return message;
    }

    /**
     * Returns all holidays in <code>daysOffset</code>
     * @param daysOffset
     * @param equalsBetween
     * @return
     * @throws Exception
     */
    protected IntegerList getHolidayIdsDaysOffset(int daysOffset , String exactBetween )
        throws Exception
    {
        IntegerList holIds = new IntegerList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        Date dateOffset = DateHelper.addDays(taskStartDate , daysOffset);

        try
        {
            String sql = "SELECT hol_id , hol_date FROM holiday ";
            ps = getConnection().prepareStatement(sql);

            rs = ps.executeQuery();
            while (rs.next())
            {
                Date holDate = rs.getDate(2);
                boolean eligible = false;
                if (exactBetween.equals(EXACT))
                {
                    eligible = DateHelper.equals(dateOffset, holDate);

                }
                else if (exactBetween.equals(BETWEEN_BACKWARD))
                {
                    eligible = DateHelper.isBetween(holDate, dateOffset, taskStartDate);
                }
                else if (exactBetween.equals(BETWEEN_FORWARD))
                {
                    eligible = DateHelper.isBetween(holDate, taskStartDate, dateOffset);
                }
                if (eligible)
                {
                    holIds.add(rs.getInt(1));
                }
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

        return holIds;
    }

    /**
     * Returns new employees created in last daysBack days. Optional trigger TRG_EMP_AIUD
     * has to be run to capture these
     * @param daysBack
     * @return
     * @throws SQLException
     */
    protected IntegerList getNewEmpIds(int daysBack)
    	throws SQLException
    {
        IntegerList empIds = new IntegerList();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            String sql = "SELECT emp_id FROM employee "
                			+ " WHERE emp_id IN (SELECT chnghist_record_id "
               				+ " FROM change_history "
                			+ " WHERE chnghist_table_name = ? "
                			+ " AND chnghist_change_date >= ? "
                			+ " AND change_history.chnghist_change_type = ?)";

            ps = getConnection().prepareStatement(sql);
            ps.setString(1 , EmployeeAccess.EMPLOYEE_TABLE.toUpperCase());
            ps.setTimestamp(2 , DateHelper.addDays(taskStartDateTime , -daysBack));
            ps.setString(3 , "I");
            rs = ps.executeQuery();
            while (rs.next())
            {
                empIds.add(rs.getInt(1));
            }
        }
        finally
        {
            if (rs != null)
            {
            	rs.close();
            }
            if (ps != null)
            {
            	ps.close();
            }
        }
        return empIds;
    }

    /**
     * Cancels all holiday overrides that happened in current year as of minimum ovrStartDate
     * @param empIdDates
     * @return
     * @throws SQLException
     */
    protected void cancelChangedEmpIdDates(EmployeeIdAndDateList empIdDates)
        throws Exception    {
        if (empIdDates == null || empIdDates.size() == 0) {
            return;
        }

        Date yearEnd = DateHelper.getUnitYear(DateHelper.APPLY_ON_LAST_DAY, false , taskStartDate);

        OverrideAccess oa = new OverrideAccess(getConnection());
        OverrideBuilder ob = new OverrideBuilder(getConnection());
        Map finalDates = findMinDatesInThisYear(empIdDates);
        Iterator iter = finalDates.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            int empId = Integer.parseInt((String)entry.getKey());
            Date dat = (Date)entry.getValue();
            OverrideList ol = oa.loadByRangeAndType(empId , dat, yearEnd,
                                           OverrideData.HOLIDAY_TYPE_START,
                                           OverrideData.HOLIDAY_TYPE_END);
            Iterator iterO = ol.iterator();
            while (iterO.hasNext()) {
                OverrideData item = (OverrideData)iterO.next();
                DeleteOverride dov = new DeleteOverride();
                dov.setOverrideId(item.getOvrId());
                ob.add(dov);
            }
        }

        ob.execute(true, false);
        if (logger.isDebugEnabled()) logger.debug("Cancelled " + ob.getUpdateCount() + " overrides for existing employees");
    }

    protected Map findMinDatesInThisYear(EmployeeIdAndDateList empIdDates) {
        Date yearStart = DateHelper.getUnitYear(DateHelper.APPLY_ON_FIRST_DAY , false , taskStartDate);
        Date yearEnd = DateHelper.getUnitYear(DateHelper.APPLY_ON_LAST_DAY, false , taskStartDate);

        Map finalEmpDates = new HashMap();
        Iterator iter = empIdDates.iterator();
        while (iter.hasNext()) {
            EmployeeIdAndDate item = (EmployeeIdAndDate)iter.next();
            if (DateHelper.isBetween(item.getDate() , yearStart , yearEnd)) {
                String key = String.valueOf(item.getEmpId());
                if (finalEmpDates.containsKey(key)) {
                    Date dat = (Date)finalEmpDates.get(key);
                    if (item.getDate().before(dat)) {
                        finalEmpDates.put(key , item.getDate());
                    }
                }
                else {
                    finalEmpDates.put(key , item.getDate());
                }
            }
        }
        return finalEmpDates;
    }

	/**
	 * Returns list of employees who's attributes have changed in the last daysBack days.
	 * @param attributes
	 * @param daysBack
	 * @return
	 * @throws SQLException
	 */
    protected EmployeeIdAndDateList getChangedEmpIdDates(String attributes, int daysBack)
    	throws SQLException
    {
        EmployeeIdAndDateList edList = new EmployeeIdAndDateList();

    	String[] AttributeList = StringHelper.detokenizeString(attributes, ",");
    	PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
        	String sql = "SELECT emp_id, ovr_new_value, ovr_start_date FROM override "
			        		+ "WHERE ovr_status = ? "
			        		+ "AND ovr_create_date BETWEEN ? AND ? "
                            + "AND ovrtyp_id BETWEEN ? AND ?";

            ps = getConnection().prepareStatement(sql);
            ps.setString(1 , OverrideData.APPLIED);
            ps.setTimestamp(2 , DateHelper.addDays(taskStartDateTime , -daysBack));
            ps.setTimestamp(3 , new Timestamp(taskStartDateTime.getTime()));
            ps.setInt(4 , OverrideData.EMPLOYEE_TYPE_START);
            ps.setInt(5 , OverrideData.EMPLOYEE_TYPE_END);

            rs = ps.executeQuery();
            while (rs.next())
            {
                int empId = rs.getInt(1);
                String newOvrValue = rs.getString(2);
                Date stDate = rs.getDate(3);
                String currentAttribute = "";
            	if(attributes != null && "ALL".equals(attributes.trim().toUpperCase()))
            	{
                    edList.add(empId , stDate);
            	}
            	else if(AttributeList !=null)
            	{
	            	for(int i=0 ; i<AttributeList.length ; i++)
	            	{
	                	currentAttribute = AttributeList[i].trim().toUpperCase();
	                	if(newOvrValue.indexOf(currentAttribute) > -1)
	                	{
                            edList.add(empId , stDate);
	                	}
	                }
            	}
            }
        }
        finally
        {
        	if (rs != null)
        	{
        		rs.close();
            }
            if (ps != null)
            {
            	ps.close();
            }
        }
    	return edList;
	}

    private static String intArrayToCsvString( int[] anArray ) {
        StringBuffer ret = new StringBuffer();
        for ( int i=0; i<anArray.length; i++ ) {
            if ( i != 0 ) {
                ret.append(',');
            }
            ret.append( anArray[i] );
        }
        return ret.toString();
    }


    private static int[] getAllHolidayIds( DBConnection con )
        throws Exception {
        HolidayAccess ha = new HolidayAccess( con );
        List list = ha.loadAll();
        if ( list == null ) {
            return new int[0];
        }
        int[] ret = new int[ list.size() ];
        Iterator it = list.iterator();
        for ( int i=0; it.hasNext(); i++ ) {
            HolidayData hd = (HolidayData) it.next();
            ret[i] = hd.getHolId();
        }
        return ret;
    }


    private static int[] getAllHolidayCalendarIds( DBConnection con )
        throws Exception {
        HolidayCalendarAccess hca = new HolidayCalendarAccess( con );
        List list = hca.loadAll();
        if ( list == null )
            return new int[0];

        int[] ret = new int[ list.size() ];
        Iterator it = list.iterator();
        for ( int i=0; it.hasNext(); i++ ) {
            HolidayCalendarData hd = (HolidayCalendarData) it.next();
            ret[i] = hd.getHcalId();
        }
        return ret;
    }

    /**
     * Creates an error transaction and logs all employees for which this
     * transaction has failed.
     * @param txLogs
     * @throws SQLException
     */
    protected void createErrorTransactionAndLogs( List txLogs )
        throws SQLException {
        int transId = createTransaction(HolAsgnTransAccess.STATUS_ERROR,
                  "Errors occured in some employees during holiday assignment");
        if ( txLogs == null ) {
            return;
        }
        HolAsgnTransLogAccess txLogAccess = new HolAsgnTransLogAccess( getConnection() );
        Iterator it = txLogs.iterator();
        while ( it.hasNext() ) {
            HolAsgnTransLogData txLogData = (HolAsgnTransLogData) it.next();
            txLogData.setHatranId( transId );
            txLogAccess.insert( txLogData );
        }
    }

    /**
     * Creates an entry in the HOL_ASGN_TRANS table for new holiday transactions.
     * @param tranStatus    This parameter can be HolAsgnTransAccess.STATUS_ERROR or
     *                      HolAsgnTransAccess.STATUS_APPLIED
     * @param msg           Message to be recorded for this holiday transaction
     * @return              Returns the holiday transaction id.
     * @throws SQLException
     */
    protected int createTransaction(String tranStatus, String msg, String userName, Date taskEndDate)
        throws SQLException {
        HolAsgnTransData txData = new HolAsgnTransData();
        txData.setHatranStartDate( taskStartDate );
        txData.setHatranEndDate( taskEndDate );
        txData.setHatranStatus( tranStatus );
        txData.setHatranWbuName( userName );
        txData.setHatranMsg( msg );

        HolAsgnTransAccess txAccess = new HolAsgnTransAccess( getConnection() );
        return txAccess.insertReturnId( txData );
    }

    protected void sendMessage( String message , String userName) {
        try {
            Message msg = new Message( getConnection() );
            msg.setSenderId( 3 );
            msg.setSenderName( "WORKBRAIN" );
            msg.setTo( userName );
            msg.setMessageId( 0 );
            msg.setMessageSubject( "Holiday Assignment Process result" );
            msg.setMessageBody( message );
            msg.setMessageType( com.workbrain.tool.mail.Util.MESSAGE_TYPE_MAIL );
            msg.send();
            msg.closeAllStatements();
            getConnection() .commit();
        } catch ( Exception e ) {
            getLogger().error( "Error in Message Send", e );
            try
            {
                getConnection() .rollback();
            } catch ( Exception e2 ) {
                getLogger().error( "Error in Message Send Rollack", e2 );
            }
        }
    }

}