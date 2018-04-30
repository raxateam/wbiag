package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.workbrain.app.jsp.ActionContext;
import com.workbrain.app.jsp.action.dailytimesheet.TimesheetConstants;
import com.workbrain.app.jsp.action.dailytimesheet.TimesheetHelper;
import com.workbrain.app.jsp.action.timesheet.EmployeeModel;
import com.workbrain.app.jsp.action.timesheet.NewEmployeeAction;
import com.workbrain.app.jsp.action.timesheet.SelectionPage;
import com.workbrain.app.jsp.action.timesheet.TSEmployeeTeamData;
import com.workbrain.app.jsp.action.timesheet.WorkSummaryModel;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.EmployeeAccess;
import com.workbrain.app.ta.db.WorkSummaryAccess;
import com.workbrain.app.ta.db.WorkbrainGroupAccess;
import com.workbrain.app.ta.db.WorkbrainUserAccess;
import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.app.ta.model.EmployeeHistoryMatrix;
import com.workbrain.app.ta.model.EmployeeIdAndDateList;
import com.workbrain.app.ta.model.EmployeeIdStartEndDateSet;
import com.workbrain.app.ta.model.WorkbrainGroupData;
import com.workbrain.app.ta.model.WorkbrainUserData;
import com.workbrain.app.ta.ruleengine.TooManyRowsException;
import com.workbrain.server.data.type.DatetimeType;
import com.workbrain.server.jsp.JSPHelper;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;
import com.workbrain.util.TimeZoneUtil;
import java.text.SimpleDateFormat;
public class CSMLoadEmployeeAction  extends NewEmployeeAction implements CSMBiWeeklyTimeSheetConstants{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CSMLoadEmployeeAction.class);

    public Object createRequest( ServletRequest request, DBConnection conn )
                throws Exception {
    	String newWeekStr = request.getParameter( "NEWWEEK" );
    	if(newWeekStr != null && newWeekStr.trim().length() > 0){
        Date model = null;
        try {
            model = DateHelper.truncateToDays( DatetimeType.FORMAT.parse( newWeekStr ) );
        } catch( Exception e ) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
        }

        return model;
    	}
    	//**
            SelectionPage page = new SelectionPage();
            java.util.Date date = TimesheetHelper.getWeekStartDate(
                TimeZoneUtil.getSystemDateForTimeZone(
                    JSPHelper.getWebLogin(request).getTimezone()
                )
            );
            String ppStr = request.getParameter( "PAYGROUP_START_DATE" );
            if(ppStr !=null){
            	try{
            		date = new Date(Long.parseLong(ppStr));
            	}catch(NumberFormatException e){
            		logger.error(e);
            	}
            }
            String dateStr = request.getParameter( "START_DATE" );

            if( dateStr != null ) {
                try {
                    page.setStartDate( DatetimeType.FORMAT.parse( dateStr ) );
                } catch( Exception e ) {
                    if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
                }
            }
            //fix for display from exception and balance reports *** sri
            dateStr = request.getParameter("DISPLAY_DATE");
            logger.debug("displayDate:"+dateStr);
            if( dateStr != null && !"-99".equals( dateStr ) ) {
            	SimpleDateFormat sf = new SimpleDateFormat("MM/dd/yyyy");
                try {
                	page.setWeekStartDate( sf.parse( dateStr ) ); //ADDED SRI**

                } catch( Exception e ) {
                    if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
                }

            }
            dateStr = request.getParameter( "WEEK_START_DATE" );

            if( dateStr != null && !"-99".equals( dateStr ) ) {
                try {
                    date = DateHelper.addDays( date, Integer.parseInt( dateStr ) * CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET );
                } catch( Exception e ) {
                    if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
                }
                page.setWeekStartDate( date );
            }

            page.setIncEmps( request.getParameter( "INC_EMPS" ) );
            page.setIncTeams( request.getParameter( "INC_TEAMS" ) );
            page.setIncSubTeams( request.getParameter( "INC_SUB_TEAMS" ) );
            page.setWeeklyOnly( request.getParameter( "EXCLUDE_DAILY" ) );
            page.setIncSPs( request.getParameter( "INC_SPS" ) );
            page.setIncCGs( request.getParameter( "INC_CGS" ) );
            page.setIncPGs( request.getParameter( "INC_PGS" ) );
            if ("T".equals(request.getParameter("FROM_SUPERVISOR_SUMMARY"))) {
                page.setPreviousPage(SelectionPage.SUPERVISOR_SUMMARY);
            }else if ("T".equals(request.getParameter("FROM_PAY_SUMMARY"))) {
                page.setPreviousPage(SelectionPage.PAY_SUMMARY);
            }else {
                page.setPreviousPage("");
            }
            if ("M".equals(request.getParameter("FLAG"))){
                page.setOrderBy("M");
            } else {
                page.setOrderBy(request.getParameter("ORDER_BY"));
                int tsoId = 0;
                try {
                    tsoId = Integer.parseInt(page.getOrderBy());
                    page.setOrderByClause(TimesheetHelper.loadOrderByString(conn, tsoId));
                } catch (NumberFormatException nfe) {
                    page.setOrderByClause("");
                }
            }
            int authSelection = 0;
            try {
                authSelection = Integer.parseInt( request.getParameter( "AUTH_SELECT" ) );
            } catch( Exception e ) {
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
            }
            page.setAuthSelection( authSelection );

            page.setFromUserDefaultTimesheet( Boolean.valueOf("T".equals(request.getParameter("FROM_SUPERVISOR_SUMMARY")) ||
					"T".equals(request.getParameter("FROM_PAY_SUMMARY"))) );

            return page;
    }

    protected Date getOldestUnsubmittedPeriodStartDate(DBConnection conn, CSMEmployeeModel emp, String wbuId) throws SQLException {

    	//This query is used to find the number of unlocked and unsubmitted work summaries with a given date range
    	StringBuffer queryOldestUnsubmitted = new StringBuffer();
    	queryOldestUnsubmitted.append("select 1 as RESULT from dual where exists(");
	   	queryOldestUnsubmitted.append("select ws.wrks_work_date from work_summary ws, sec_employee_date sec, employee ee, workbrain_user wbu, workbrain_group wbg ");
    	queryOldestUnsubmitted.append("where ");
        queryOldestUnsubmitted.append("sec.emp_id = ? and ");
        queryOldestUnsubmitted.append("sec.wbu_id = ? and ");
        queryOldestUnsubmitted.append("sec.emp_id = ws.emp_id and ");
        queryOldestUnsubmitted.append("ee.emp_id = ws.emp_id and ");
        queryOldestUnsubmitted.append("empt_start_date <= ? and " );
        queryOldestUnsubmitted.append("wbut_start_date <= ? and ");
        queryOldestUnsubmitted.append("empt_end_date >= ? and " );
        queryOldestUnsubmitted.append("wbut_end_date >= ? and ");
        queryOldestUnsubmitted.append("ws.wrks_work_date >= ? and ");
        queryOldestUnsubmitted.append("sec.wbu_id = wbu.wbu_id and ");
        queryOldestUnsubmitted.append("wbg.wbg_id = wbu.wbg_id and ");
        queryOldestUnsubmitted.append("(ws.wrks_work_date >= ? or wbg.wbg_lockdown = 'Y') and ");
        queryOldestUnsubmitted.append("empt_end_date >= ws.wrks_work_date and ");
        queryOldestUnsubmitted.append("wbut_end_date >= ws.wrks_work_date and ");
        queryOldestUnsubmitted.append("(ws.wrks_submitted is NULL or ws.wrks_submitted <> 'Y') and ");
        //query.append("(ws.wrks_authorized is NULL or ws.wrks_authorized <> 'Y') and ");
        queryOldestUnsubmitted.append("ee.emp_hire_date <= ws.wrks_work_date and ");
        queryOldestUnsubmitted.append("ee.emp_termination_date > ws.wrks_work_date and ");
        queryOldestUnsubmitted.append("ws.wrks_work_date >= ? and ");
        queryOldestUnsubmitted.append("ws.wrks_work_date <= ? )");

        WorkSummaryAccess wsAccess = new WorkSummaryAccess(conn);
        WorkbrainUserAccess wbuAccess = new WorkbrainUserAccess(conn);
        WorkbrainGroupAccess wbgAccess = new WorkbrainGroupAccess(conn);

        int wbuIdInt = Integer.parseInt(wbuId);
    	//Get oldest unsubmitted timesheet
    	//choose the latest date from hire date, hands-off date, and supervisor date(when lockdownPriv='N')
        //as the period start date
        Date startDate = null;			//actual start date
        Date periodStartDate = null;	//week start date
        Date hireDate = emp.getHireDate();
    	Date dayAfterHandsOffDate = DateHelper.addDays(CSMWTShelper.getHandsOffDateForEmp(emp,DateHelper.getCurrentDate()),1);
    	Date dayAfterSupervisorDate = DateHelper.addDays(CSMWTShelper.getSupervisorDateForEmp(emp,DateHelper.getCurrentDate()),1);
    	java.sql.Date sqlDayAfterHandsOffDate = new java.sql.Date(dayAfterHandsOffDate.getTime());
    	java.sql.Date sqlDayAfterSupervisorDate = new java.sql.Date(dayAfterSupervisorDate.getTime());

    	//always get the latest date between handsoffdate and hiredate
    	if(hireDate.after(dayAfterHandsOffDate))
    		startDate= hireDate;
    	else
    		startDate= dayAfterHandsOffDate;

    	//only compare supervisor date when lockdownPriv='N'
    	WorkbrainUserData wbuData = wbuAccess.loadByWbuId(wbuIdInt);
    	WorkbrainGroupData wbgData = wbgAccess.load(wbuData.getWbgId());
    	String lockDownPriv = wbgData.getWbgLockdown();
    	if(StringHelper.isEmpty(lockDownPriv) || !lockDownPriv.equals("Y")){
    		if(dayAfterSupervisorDate.after(startDate))
    			startDate = dayAfterSupervisorDate;
    	}

    	//align the period start date with the pay period start date
//    	periodStartDate = TimesheetHelper.getWeekStartDate(startDate);
//    	Date payGroupStartDate = TimesheetHelper.getWeekStartDate(emp.getPayGroupStartDate());
//    	int diff = DateHelper.getDifferenceInDays(payGroupStartDate,periodStartDate);
//    	if(diff%CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET!=0)
//    		periodStartDate = DateHelper.addDays(periodStartDate, -1*CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET/2);
//
    	periodStartDate = CSMWTShelper.getAlignedPeriodStartDate(startDate, emp.getPayGroupStartDate());
    	Date periodEndDate = DateHelper.addDays(periodStartDate,CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET-1);


    	List wsList = null;
    	//int unsubmittedUnlockedDaysCount = 0;
    	int result = 0;
    	PreparedStatement pstmt=conn.prepareStatement(queryOldestUnsubmitted.toString());
        ResultSet rs = null;
    	try{
	    	//while(unsubmittedUnlockedDaysCount==0 &&
    		while(result==0 &&
	    			emp.getTerminationDate().after(periodStartDate)){

		        wsList = wsAccess.loadByEmpIdAndDateRange(emp.getEmpId(),periodStartDate, periodEndDate);

		        //if there are no worksummary's to begin with, or we don't have all the work summaries
		        //use the start date as the oldest unsubmitted date
		        int numOfValidWorkSummaries = CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET;
		        if(startDate.after(periodStartDate))
		        	numOfValidWorkSummaries = DateHelper.getDifferenceInDays(periodEndDate,startDate);
		        if(wsList==null || wsList.size()<numOfValidWorkSummaries){
		        	return periodStartDate;
		        }
		        //if there are 14 days of worksummaries already, then find out if any summary is unlocked and unsubmitted
		        else{
		        	try{
			        	rs = null;

			        	//we're reusing the statement, so clear the paramters
				        pstmt.clearParameters();
				        pstmt.setInt(1, emp.getEmpId());
				        pstmt.setInt(2, wbuIdInt);
				        pstmt.setDate(3, new java.sql.Date(periodStartDate.getTime())); //emp team must start on and before the pay period start
				        pstmt.setDate(4, new java.sql.Date(periodStartDate.getTime())); //workbrain team must start on and before the pay period start
				        pstmt.setDate(5, new java.sql.Date(periodEndDate.getTime())); 	//emp team must end after the pay period end
				        pstmt.setDate(6, new java.sql.Date(periodEndDate.getTime()));   //workbrain team must end after the pay period end
				        pstmt.setDate(7, sqlDayAfterHandsOffDate);						//only days after the handsoff date is valid
				        pstmt.setDate(8, sqlDayAfterSupervisorDate);					//only days after the supervisor date is valid unless supervisor privledge is yes
				        pstmt.setDate(9, new java.sql.Date(periodStartDate.getTime())); //only look at days valid for this pay period
				        pstmt.setDate(10, new java.sql.Date(periodEndDate.getTime()));  //only look at days valid for this pay period

				        rs = pstmt.executeQuery();

				        while( rs.next() ) {
				        	result = rs.getInt(1);
				        }

		        	}finally{
		        		SQLHelper.cleanUp(rs);
		        	}

		        }

		        //if(unsubmittedUnlockedDaysCount>0){
		        if(result==1){
		        	return periodStartDate;
		        }

		        periodStartDate = DateHelper.addDays(periodEndDate,1);
		        periodEndDate = DateHelper.addDays(periodStartDate,CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET-1);

	    	}
    	}finally{
    		SQLHelper.cleanUp(pstmt);
    	}

    	//this happens when we've gone through the entire range of timesheets
    	//and still cannot find an unsubmitted t/s
    	//if(unsubmittedUnlockedDaysCount==0)
    	if(result!=1)
    		periodStartDate = emp.getTerminationDate();


        return periodStartDate;

	}

	public String process( DBConnection conn, ActionContext context,
                           Object requestObj )
            throws Exception {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("com.workbrain.app.jsp.action.biweeklyTimesheet.LoadEmployeeAction.class - LoadEmployeeAction.process()"); }
        PreparedStatement ps = null;
        ResultSet rs = null;
        HashMap otdPermissions = new HashMap();
        Date displayStartDate, displayEndDate;
        int maxNumberEmployees = Registry.getVarInt("system/timesheet/MAX_NUMBER_OF_EMPLOYEES", 1000);

        SelectionPage selection;
        int empId = -99;
        HttpServletRequest request = context.getRequest();
        HttpSession session = request.getSession(false);
        String selectionPageMfrmId = request.getParameter("sel_mfrm_Id");

        request.setAttribute("TS_BACK_TO_URL", request.getContextPath() + CSMBiWeeklyTimeSheetConstants.SELECTION_JSP_URL + "?mfrm_id="+selectionPageMfrmId);

        if ("Y".equals(session.getAttribute("COPY_FROM"))){
            selection = new SelectionPage();
            selection.setIncEmps(session.getAttribute("COPY_EMPLOYEE").toString());
            String dateStr = (String)session.getAttribute("COPY_START_DATE");
            if( dateStr != null ) {
                try {
                    selection.setStartDate( DatetimeType.FORMAT.parse( dateStr ) );
                } catch( Exception e ) {
                    if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
                }
            }
        }else{
            if (requestObj instanceof Date) {
                // got here from NewWeekAction (from Previous/Next buttons on the timesheet)
                Date newWeekStartDate = (Date) requestObj;
                selection = (SelectionPage) context.getAttribute("timesheet.loademployeselection.select");
                selection.setWeekStartDate(newWeekStartDate);
                CSMBiWeeklyTimeSheetPage page = (CSMBiWeeklyTimeSheetPage) context.getAttribute("timesheet");
                empId = page.getEmployee().getEmpId();
            } else {
                selection = (SelectionPage) requestObj;
            }
            // context.setAttribute( "timesheet.loademployeselection.select", selection);
        }
        context.setAttribute("timesheet.loademployeselection.select", selection);

        if ("M".equals(selection.getOrderBy())){
            SelectionPage selPage = new SelectionPage();
            StringBuffer labels = new StringBuffer( "Manual Date Range" );
            StringBuffer values = new StringBuffer( DatetimeType.FORMAT.format(
            new java.util.Date(0) ) );
            java.util.Date dateS = TimesheetHelper.getWeekStartDate(
                TimeZoneUtil.getSystemDateForTimeZone(context.getUserTimeZone())
            );
            selPage.setWeekStartDate( dateS );
            for( int i = -3; i <= 12; i++ ) {
            java.util.Date startS = DateHelper.addDays( dateS, i * DAYS_ON_TIMESHEET );
            java.util.Date endS = DateHelper.addDays( startS, DAYS_ON_TIMESHEET-1);
            String label = MMDDYYYY.format( startS ) + " - " +
                           MMDDYYYY.format( endS ) + " ";
            if( i == 0 ) {
                        label += "Current Week";
                    } else if( i > 0 ) {
                        label += "Week +" + i;
                    } else {
                        label += "Week " + i;
                    }
                    labels.append( "," + label );
                    values.append( "," + DatetimeType.FORMAT.format( startS ) );
            }

            selPage.setWeekStartDateLabels( labels.toString() );
            selPage.setWeekStartDateValues( values.toString() );

            String wbuNameS = null;
            String wbuIdS = null;
            String wbuEmpIdS = null;

            try {
                    wbuNameS = (String)context.getUserName();
                    wbuIdS = (String)context.getUserId();
                    wbuEmpIdS = (String)context.getUserEmpId();
            } catch( Exception e ) {
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
            }

            if( wbuNameS == null ) {
                        wbuNameS = "WORKBRAIN";
            }

            selPage.setWbuName( wbuNameS );
            selPage.setWbuId( wbuIdS );
            selPage.setWbuEmpId(wbuEmpIdS);

            context.setAttribute( "timesheet.select", selPage );
            selection.setOrderBy(null);
        }

        session.setAttribute("COPY_FROM", "N");

        CSMBiWeeklyTimeSheetPage page = new CSMBiWeeklyTimeSheetPage();

        // Load override type permissions
        try {
            ps = conn.prepareStatement("SELECT OVRTYP_ID, WBP_ID FROM OVERRIDE_TYPE_GRP WHERE WBG_ID = ?");
            ps.setInt(1, Integer.parseInt(JSPHelper.getWebLogin(context.getRequest()).getGroupId()));
            rs = ps.executeQuery();
            while (rs.next()) {
                otdPermissions.put(new Integer(rs.getInt("OVRTYP_ID")),
                        new Integer(rs.getInt("WBP_ID")));
            }
        } finally {
            SQLHelper.cleanUp(ps, rs);
        }
        page.setOverrideTypePermissions(otdPermissions);

        String wbuName = (String)context.getUserName();

        if( wbuName == null ) {
            wbuName = "WORKBRAIN";
        }

        if (session.getAttribute("EXTRA_WTS_PARAMETERS") == null) {
            String extraParams = CSMWTShelper.retrieveWTSExtraParams(conn, context.getUserEmpId());
            if (extraParams == null) {
                extraParams = "";
            }
            session.setAttribute("EXTRA_WTS_PARAMETERS", extraParams);
        }
        page.setExtraWtsParameters((String) session.getAttribute("EXTRA_WTS_PARAMETERS"));

        page.setWbuName( wbuName );
        page.setWbuId(context.getUserId());
        page.setWeekStartDate( TimesheetHelper.getWeekStartDate( new java.util.Date( 0 ) ) );

//      CSM: if we don't have INC_EMPs, then we pull up the employee's oldest unsubmitted timesheet
        EmployeeAccess empAcc = new EmployeeAccess( conn, CodeMapper.createCodeMapper( conn ) );
        if (StringHelper.isEmpty(selection.getIncEmps()) && StringHelper.isEmpty(selection.getIncTeams())){

        	String userEmpIdStr = context.getUserEmpId();
        	int userEmpId = Integer.parseInt(userEmpIdStr);

        	//employee is the person who logged in
        	selection.setIncEmps(userEmpIdStr);

        	//if we're in the load action because we pressed the next week button, then load next week's date
        	Date newWeekDate = null;
        	String newWeekStr = request.getParameter( "NEWWEEK" );
        	if(!StringHelper.isEmpty(newWeekStr)){
                try {
                	newWeekDate = DateHelper.truncateToDays( DatetimeType.FORMAT.parse( newWeekStr ) );
                	page.setWeekStartDate(newWeekDate);
	            } catch( Exception e ) {
	                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
	            }
        	}

        	//otherwise, set the oldest unsubmitted timesheet date
            if(newWeekDate==null){

            	//In phase 1, employee data is going to be static, so it doesn't matter what reference date we use
            	//Here, we've chosen the current date as reference.
	        	CSMEmployeeModel emp = (CSMEmployeeModel)CSMNewEmployeeAction.createEmployeeModel( conn, context, wbuName,
	        			userEmpId, DateHelper.getCurrentDate());
                boolean chkEarlistUnSubmitted = Registry.getVarBoolean(CSMBiWeeklyTimeSheetConstants.WBREG_CHECK_FOR_EARLIEST_UNSUBMITTED, false );
                Date baseDate = chkEarlistUnSubmitted
                    ? getOldestUnsubmittedPeriodStartDate(conn,emp, context.getUserId())
                    : DateHelper.getCurrentDate();
	        	page.setWeekStartDate(TimesheetHelper.getWeekStartDate(baseDate));

            }

        }else{
	        try {
	            java.util.Date date = selection.getWeekStartDate();
	            if( date == null || new java.util.Date(0).compareTo( date ) == 0 ) {
	                date = selection.getStartDate();
	                if( date == null ) {
	                    date = TimeZoneUtil.getSystemDateForTimeZone(context.getUserTimeZone());
	                }
	                //date = TimesheetHelper.getWeekStartDate( date );
	                page.setWeekStartDate( date );
	            } else {
	                page.setWeekStartDate( TimesheetHelper.getWeekStartDate( date ) );
	            }
	        } catch( Exception e ) {
	            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage());}
	        }
        }
        displayStartDate = page.getWeekStartDate();
        displayEndDate= DateHelper.addDays(displayStartDate, DAYS_ON_TIMESHEET-1);


        EmployeeIdStartEndDateSet empTeamVisibilitySet = new EmployeeIdStartEndDateSet(); // indicate which days are visible.
        EmployeeIdAndDateList editableDateSet = new EmployeeIdAndDateList(); //indicate which days are readonly for each employee
		//based on termination and hire date

        List empNavLst = null; // unique list of TSEmployeeTeamDat obj, use for display and navigation.
        {
            // tsEmpLst could contains duplicates if emp is assigned to multiple teams within the timeframe.
            List tsEmpLst = null;

            try {
            	tsEmpLst = loadEmployees( context, conn, selection, wbuName, page.getWeekStartDate() );
            } catch (TooManyRowsException e) {
            	return "/timesheet/tooManyEmployees.jsp";
            }

            if ( "Y".equalsIgnoreCase(selection.getWeeklyOnly()) ) {
                // filter out the non-weekly emps only if the user choose to.
                tsEmpLst = filterWeeklyOnlyEmp(conn, tsEmpLst);
            }

            if (tsEmpLst.size() == 0) {
                return "/timesheet/noRecordsFound.jsp";
            }

            /* now build a team visibility date range
             * AND a unique emp navigation list
             * based on tsEmpLst */
            empNavLst = new ArrayList(tsEmpLst.size()); // to preserve the order
            {
                Set uniqueEmpIdSet = new HashSet(); // just to filter out duplication.
                for (Iterator iter=tsEmpLst.iterator();  iter.hasNext();) {
                    TSEmployeeTeamData tsEmpDat = (TSEmployeeTeamData) iter.next();
                    empTeamVisibilitySet.add( tsEmpDat.getEmpId(), tsEmpDat.getTeamStartDate(), tsEmpDat.getTeamEndDate() );
                    Integer key = new Integer(tsEmpDat.getEmpId());
                    if (!uniqueEmpIdSet.contains(key)) {
                        uniqueEmpIdSet.add(key);
                        empNavLst.add(tsEmpDat);
                    }
    CSMEmployeeModel empModel = CSMNewEmployeeAction.createEmployeeModel( conn, context, wbuName,
    		tsEmpDat.getEmpId(), DateHelper.getCurrentDate() );
    page.setWeekStartDate(CSMWTShelper.getAlignedPeriodStartDate(page.getWeekStartDate(),TimesheetHelper.getWeekStartDate(empModel.getPayGroupStartDate())));

    //get Employee date for the range of the week
                    EmployeeHistoryMatrix ehm = empAcc.load(tsEmpDat.getEmpId(),
                    		page.getWeekStartDate(), page.getWeekEndDate());

                    //determine which dates are editable, based on termination date
                    Date currentDate = page.getWeekStartDate();
                    for(int i = 0; i < DAYS_ON_TIMESHEET; i++){
                    	Date terminationDate = (ehm.getEmployeeForDate(tsEmpDat.getEmpId(),
                    			currentDate)).getEmpTerminationDate();
                    	Date hireDate = (ehm.getEmployeeForDate(tsEmpDat.getEmpId(),
                    			currentDate)).getEmpHireDate();
                    	if(DateHelper.isBetween(currentDate,hireDate,
                    			DateHelper.addDays(terminationDate, -1))){
                    		editableDateSet.add(tsEmpDat.getEmpId(),currentDate);
                    	}
                    	currentDate = DateHelper.addDays(currentDate,1);
                    }
                }
            }

            if (empNavLst.size() > maxNumberEmployees) {
                return "/timesheet/tooManyEmployees.jsp";
            }

        }

        EmployeeData employee = (EmployeeData)empNavLst.get( 0 );

        // If we are here because someone pushed Previous/Next on the WTS, then
        // we need to set the currently displayed user to the one they were looking at.
        if (empId != -99) {
            for (Iterator iter = empNavLst.iterator(); iter.hasNext(); ) {
                employee = (EmployeeData) iter.next();
                if (employee.getEmpId() == empId) {
                    break;
                }
            }
            if (employee.getEmpId() != empId) {
                return "/timesheet/noRecordsFound.jsp";
            }
        }

        CSMEmployeeModel emp = CSMNewEmployeeAction.createEmployeeModel( conn, context, wbuName,
                employee.getEmpId(), DateHelper.getCurrentDate() );
        page.setEmployee( emp );


  page.setEditableDateSet(editableDateSet);

        page.setVisibleTeamDateSet(empTeamVisibilitySet);

        StringBuffer labels = new StringBuffer();
        StringBuffer values = new StringBuffer();
        makeEmpDropDownValuesAndLabels(selection, empNavLst, labels, values);
        page.setEmployeeLabels( labels.toString() );
        page.setEmployeeValues( values.toString() );

//vlo: invalidateSummaries() is a parent class method.
//      We override it in this class because the parent's method is specific for weekly timesheet(7days)
        invalidateSummaries( page );

		// DISPLAY_START_DATE & DISPLAY_END_DATE are needed to determine
        // when to display the clocks button on the work detail popups.
        // Used by com.workbrain.app.jsp.action.dailytimesheet.WorkDetailModel
        session.setAttribute("DISPLAY_START_DATE", displayStartDate);
        session.setAttribute("DISPLAY_END_DATE", displayEndDate);

        // Mark in the session that the weekly timesheet will be the users 'timesheet last visited'.
        context.setAttribute(TimesheetConstants.TIMESHEET_LAST_VISITED, "W");

        context.setAttribute( "timesheet", page );

        //CSM: These values are used in the Timecode DBLookup
        session.setAttribute("FORMATTED_START_DATE", new java.text.SimpleDateFormat("yyyyMMdd").format(displayStartDate));
        session.setAttribute("FORMATTED_END_DATE", new java.text.SimpleDateFormat("yyyyMMdd").format(displayEndDate));

        return CSMBiWeeklyTimeSheetConstants.ACTION_URL+"?action=CSMViewBiWeeklyTimeSheetAction";
    }

//vlo: overridden parent to take care of BiWeeklyTimeSheetPage
    protected void invalidateSummaries( CSMBiWeeklyTimeSheetPage model ) {
        List summaries = model.getSummaries();
        if( summaries != null ) {
            for( int i = 0; i < DAYS_ON_TIMESHEET; i++ ) {
                WorkSummaryModel summary = (WorkSummaryModel)summaries.get(i);
                summary.setInvalid( true );
            }
        }
    }

    /**
     * @param selection selection made by the user. (will not be modified in the method)
     * @param emps a list of emps resulted from the user selection. (will not be modified in the method)
     * @param labels the resulting labels made from the given list of emps and sorted according to the selection)
     * @param values the resulting corresponding values for the labels made from the given list of emps and sorted according to the selection)
     */
    private void makeEmpDropDownValuesAndLabels(final SelectionPage selection, final List emps, StringBuffer labels, StringBuffer values) {
        EmployeeData employee;
        Iterator i = emps.iterator();
        while( i.hasNext() ) {
            employee = (EmployeeData)i.next();
            if( labels.length() != 0 ) {
                labels.append( "," );
                values.append( "," );
            }
            if (selection.getOrderByClause().indexOf("EMP_LASTNAME") == 0){
                labels.append( employee.getEmpLastname() + " " +
                               employee.getEmpFirstname() + " - " +
                               employee.getEmpName() );
            }else if(selection.getOrderByClause().indexOf("EMP_FIRSTNAME") == 0){
                labels.append( employee.getEmpFirstname() + " " +
                               employee.getEmpLastname() + " - " +
                               employee.getEmpName() );
            }else{
                labels.append( employee.getEmpName() + " " +
                               employee.getEmpLastname() + " - " +
                               employee.getEmpFirstname() );
            }
            values.append( employee.getEmpId() );
        }
    }

    protected boolean isAuthorized(DBConnection conn, int empId, java.util.Date startDate, EmployeeIdStartEndDateSet teamVisibleDateSet) throws Exception{
        PreparedStatement pStmt = null;
        ResultSet rs = null;

        try {
            pStmt = conn.prepareStatement(
                    "select wrks_authorized, wrks_work_date from work_summary where emp_id = ?"
                    + " and wrks_work_date > ?"
                    + " and wrks_work_date < ?");
            pStmt.setInt(1, empId);
            pStmt.setTimestamp(2, new Timestamp(DateHelper.addDays(startDate, -1).getTime()));
            pStmt.setTimestamp(3, new Timestamp(DateHelper.addDays(startDate, 7).getTime()));
            rs = pStmt.executeQuery();

            while (rs.next()){
                Date wrksDate = rs.getDate("wrks_work_date");
                if (teamVisibleDateSet.includes(empId, wrksDate)) {
                    if (rs.getString("WRKS_AUTHORIZED").equals("N")){
                        return false;
                    }
                }
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (pStmt != null) {
                pStmt.close();
            }
        }
        return true;
    }

    protected List loadEmployees(ActionContext context, DBConnection conn, SelectionPage selection,
            String wbuName, java.util.Date weekStartDate ) throws Exception {
        java.util.Date weekEndDate = DateHelper.addDays( weekStartDate, DAYS_ON_TIMESHEET-1);

        return loadEmployees(context, conn, selection,wbuName,weekStartDate, weekEndDate);
    }

    /**
     * Returns a List of TSEmployeeData objects.
     * @param context	the ActionContext containing data used for loading employees.
     * @param conn	the connection object used to connect to the database.
     * @param selection	contains data made from the selection page.
     * @param wbuName the Workbrain user's name making the request.
     * @param startDate	the employee team's start date
     * @param endDate the employee team's end date
     * @return the list of TSEmployeeData objects
     * @throws Exception if an exception occurs
     * @throws IllegalArgumentException
     * 	if the Registry value used to specify the number of records permitted to load is less than 1.
     * @throws TooManyRowsException if the number of TSEmployeeData objects created exceeds the Registry's limit.
     */

    protected List loadEmployees(ActionContext context, DBConnection conn, SelectionPage selection,
                               String wbuName, java.util.Date startDate,
                               java.util.Date endDate)
            throws Exception {
    	int resultLimit = Registry.getVarInt("/system/WORKBRAIN_PARAMETERS/RECORD_DATA_LIMIT", 1000);
        EmployeeAccess empAccess = new EmployeeAccess( conn,
                CodeMapper.createCodeMapper( conn ) );
        String wbuId = context.getUserId();

        StringBuffer query = new StringBuffer();

        // *** Forming the WHERE clause ***
        query.append(" WBU_ID = ");
        query.append(wbuId);
        query.append(" AND EMPLOYEE.EMP_ID = SEC_EMPLOYEE_DATE.EMP_ID ");

        query.append(" AND EMPT_END_DATE >= ");
        query.append(conn.encodeTimestamp(startDate));
        query.append(" AND WBUT_END_DATE >= ");
        query.append(conn.encodeTimestamp(startDate));
        query.append(" AND EMPT_START_DATE <= ");
        query.append(conn.encodeTimestamp(endDate));
        query.append(" AND WBUT_START_DATE <= ");
        query.append(conn.encodeTimestamp(endDate));

        if (!(StringHelper.isEmpty(selection.getIncEmps()) || "ALL".equals(selection.getIncEmps()))) {
            query.append(" AND EMPLOYEE.EMP_ID IN (");
            query.append(selection.getIncEmps());
            query.append(") ");
        }

        if (!(StringHelper.isEmpty(selection.getIncTeams()) || "ALL".equals(selection.getIncTeams()))) {
            if ("Y".equalsIgnoreCase(selection.getIncSubTeams())) {
                query.append(" AND SEC_EMPLOYEE_DATE.wbt_id in (select distinct child_wbt_id from SEC_WB_TEAM_CHILD_PARENT ");
                query.append(" where parent_wbt_id  IN (");
                query.append(selection.getIncTeams());
                query.append(" ) ) AND SEC_EMPLOYEE_DATE.wbt_id in (");
                query.append(" select distinct sec_workbrain_team.wbt_id ");
                query.append(" from sec_workbrain_team where sec_workbrain_team.wbu_id = ");
                query.append(wbuId);
                query.append(")");
            } else {
                query.append(" AND SEC_EMPLOYEE_DATE.WBT_ID IN (");
                query.append(selection.getIncTeams());
                query.append(") ");
            }
        }

        if (!(StringHelper.isEmpty(selection.getIncPGs()) || "ALL".equals(selection.getIncPGs()))) {
            query.append(" AND EMPLOYEE.PAYGRP_ID IN (");
            query.append(selection.getIncPGs());
            query.append(") ");
        }

        if (!(StringHelper.isEmpty(selection.getIncCGs()) || "ALL".equals(selection.getIncCGs()))) {
            query.append(" AND EMPLOYEE.CALCGRP_ID IN (");
            query.append(selection.getIncCGs());
            query.append(") ");
        }

        if (!(StringHelper.isEmpty(selection.getIncSPs()) || "ALL".equals(selection.getIncSPs()))) {
            query.append(" AND EMPLOYEE.SHFTPAT_ID IN (");
            query.append(selection.getIncSPs());
            query.append(") ");
        }
        query.append(" AND WORKBRAIN_TEAM.WBT_ID = SEC_EMPLOYEE_DATE.WBT_ID ");

        if( selection.getAuthSelection() > 0 ) {
            String authSelectionCondition = loadAuthSelectionCondition( conn, selection.getAuthSelection(), startDate );
            if (!StringHelper.isEmpty(authSelectionCondition)) {
                  query.append(authSelectionCondition);
            }
        }

        return empAccess.loadRecordDataWithLimit(
        		new TSEmployeeTeamData(), "EMPLOYEE.*, SEC_EMPLOYEE_DATE.WBT_ID, WORKBRAIN_TEAM.WBT_NAME, " +
                                          "SEC_EMPLOYEE_DATE.EMPT_START_DATE, SEC_EMPLOYEE_DATE.WBUT_START_DATE, " +
                                          "SEC_EMPLOYEE_DATE.EMPT_END_DATE, SEC_EMPLOYEE_DATE.WBUT_END_DATE",
                "SEC_EMPLOYEE_DATE, EMPLOYEE, WORKBRAIN_TEAM",
                query.toString(), selection.getOrderByClause(), resultLimit);
    }
    protected String loadAuthSelectionCondition(DBConnection conn, int id, Date weekDate) throws
        SQLException {

      String result = "";

      String sql = "SELECT TSF_SQL FROM TIMESHEET_FILTER WHERE TSF_ID = ?";
      PreparedStatement stm = null;
      ResultSet rs = null;
      try {
        stm = conn.prepareStatement(sql);
        stm.setInt(1, id);
        rs = stm.executeQuery();
        while (rs.next()) {
          result = rs.getString(1);
          if( result != null && !"".equals( result ) ) {
            String startDate = conn.encodeTimestamp( weekDate );
            String endDate = conn.encodeTimestamp( DateHelper.addDays( weekDate, DAYS_ON_TIMESHEET-1) );
            result = StringHelper.searchReplace( result, "<start_date>", startDate );
            result = StringHelper.searchReplace( result, "<end_date>", endDate );
            result = " and " + result;
          }
        }
      }
      finally {
        if (rs != null)
          rs.close();
        if (stm != null)
          stm.close();
      }
      return result==null? "" : result;
    }

    protected void appendCondition( String field, String values, boolean include,
                                StringBuffer buffer ) {
        if( StringHelper.isEmpty(values) || values.equalsIgnoreCase("ALL") ) return;

        String result = field;

        if( !include ) {
            result = result + " not";
        }
        result = result + " " + getConditionValue( values );

        append( result, include, buffer );
    }

    protected void append( String values, boolean include, StringBuffer buffer ) {
        if( buffer.length() != 0 ) {
            buffer.append( " and " );
        }
        buffer.append( values );
    }

    protected String getConditionValue( String values ) {
        String result = null;

        if( !StringHelper.isEmpty(values)) {
            values = values.trim();
            if( "ALL".equalsIgnoreCase( values ) ) {
                result = "like '%'" ;
            } else if( values.length() != 0 ) {
                result = "in (" + StringHelper.searchReplace( values, ",",
                        "," ) + ")";
            }
        }

        return result;
    }

    /**
     * Given a list of employees will remove any duplicates.
     * Also, if weeklyOnly is TRUE will remove any NON-WEEKLY employees
     * @param conn
     * @param employees List of employees (not modified)
     * @param weeklyOnly boolean - corresponds to the WeklyOnly flag on the WTS selection screen
     * @return a list of EmployeeTeamData, but should be use as EmployeeData since
     *          the list might only carry one emp record while there could be multiple
     *          due to different teams.
     * @throws Exception
     */
    private List filterWeeklyOnlyEmp(DBConnection conn, final List employees) throws java.sql.SQLException {
        if (employees == null || employees.size() == 0) {
            return employees;
        }
        // *** Lets get the list of Daily Timesheet Employees ***
        Set dailyEmpIdSet = new HashSet();
        StringBuffer query;
        int size = employees.size();
        Iterator iter = employees.iterator();
        int lastRec;
        int chunkSize = 999;
        int chunkNumber =  (int)Math.ceil((double)size / chunkSize);
        //TT 47540 Changed the retrieval process to run in batch as this query is breaking with more than 1000 employees
        for (int recordChunk = 0; recordChunk < chunkNumber ;recordChunk++){
	        query = new StringBuffer("SELECT EMP_ID FROM TS_USER WHERE EMP_ID IN (");
	        lastRec = java.lang.Math.min(chunkSize, size - chunkSize * recordChunk );
	        for (int i = 0; i < lastRec; i++) {
	            query.append(i > 0 ? " ,?" : "?");
	        }
	        query.append(") AND TSU_TIMESHEET_FLAG = ?");
	        PreparedStatement ps = conn.prepareStatement(query.toString());
	        ps.clearParameters();
	        for (int paramCount = 0; paramCount < lastRec && iter.hasNext(); paramCount++) {
	            EmployeeData ed = (EmployeeData) iter.next();
	            ps.setInt(paramCount+1, ed.getEmpId());
	        }
	        ps.setString(lastRec + 1, "1"); // *** DAILY TIMESHEET EMPLOYEES ***
	        ResultSet rs = null;
	        try {
	            rs = ps.executeQuery();
	            while (rs.next()) {
	                dailyEmpIdSet.add(rs.getString("EMP_ID"));
	            }
	        }
	        finally {
	            SQLHelper.cleanUp(ps, rs);
	        }
        }
        // *** Now removing the daily guys from the list ***
        ArrayList weeklyEmpLst = new ArrayList(employees.size());
        for (iter=employees.iterator(); iter.hasNext(); ) {
            EmployeeData ed = (EmployeeData) iter.next();
            String empIdStr = String.valueOf(ed.getEmpId());
            if (!dailyEmpIdSet.contains(empIdStr)) {
                weeklyEmpLst.add(ed);
            }
        }

        return weeklyEmpLst;
    }

}
