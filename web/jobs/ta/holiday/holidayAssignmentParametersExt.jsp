<%@ include file="/system/wbheader.jsp" %>
<%@ page import="com.workbrain.app.scheduler.*" %>
<%@ page import="com.workbrain.app.scheduler.enterprise.*" %>
<%@ page import="com.workbrain.app.ta.holiday.HolidayAssignmentTask" %>
<%@ page import="com.wbiag.app.ta.holiday.HolidayAssignmentTaskExt" %>
<%@ page import="com.workbrain.security.SecurityService" %>
<%@ page import="com.workbrain.server.WorkbrainParametersRetriever" %>
<%@ page import="com.workbrain.server.registry.*" %>
<%@ page import="com.workbrain.util.*" %>
<%@ page import="java.util.*" %>

<%!
    private static String getMapParam( Map map, String key )
    {
        String value = (String) map.get( key );
        if ( StringHelper.isEmpty( value ) )
            return "";
        return value;
    }
%>

<wb:page login='true' >
    <wb:config id="AT_LEAST_ONE_HOLIDAY_OR_HOLIDAY_CALENDAR_CRITERIA_MUST_BE_SPECIFIED"/>
    <wb:config id="PLEASE_READ_THE_EXPLANATION_NEXT_TO_CHECKBOX"/>
    <wb:config id="PLEASE_SPECIFY_HOLIDAY_CALENDAR"/>
    <wb:config id="AT_LEAST_ONE_HOLIDAY_CRITERIA_MUST_BE_SPECIFIED"/>
    <wb:config id="ONE_OR_MORE_EMPLOYEE_CRITERIA_MUST_BE_SPECIFIED"/>
    <wb:config id="DAYS_TO_PROCESS_MUST_BE_POSITIVE"/>

    <wb:define id='operation'><wb:get id='operation' scope='parameter' default=''/></wb:define>
    <wb:define id='USER_NAME_PARAM'><wb:getPageProperty id='userName'/></wb:define>
    <wb:define id='TASK_ID'><wb:get id='TASK_ID' scope='parameter' default=''/></wb:define>
    <wb:submit id='TASK_ID'><wb:get id='TASK_ID'/></wb:submit>
    <wb:define id='contextPath'><%= request.getContextPath() %></wb:define>
<%
  Scheduler scheduler = (Scheduler) GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = null;
  if ( !StringHelper.isEmpty( TASK_ID.toString() ) )
        rec = scheduler.getTask( Integer.parseInt( TASK_ID.toString() ) );

    if ( "submit".equalsIgnoreCase( operation.toString() ) )
    {
        HashMap newParams = new HashMap();
        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTask.HOLIDAY_IDS_PARAM ) ) )
            newParams.put( HolidayAssignmentTask.HOLIDAY_IDS_PARAM, request.getParameter( HolidayAssignmentTask.HOLIDAY_IDS_PARAM ) );
        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTask.HOLIDAY_CALENDAR_IDS_PARAM ) ) )
            newParams.put( HolidayAssignmentTask.HOLIDAY_CALENDAR_IDS_PARAM, request.getParameter( HolidayAssignmentTask.HOLIDAY_CALENDAR_IDS_PARAM ) );
        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTask.DAYS_TO_PROCESS_PARAM ) ) )
            newParams.put( HolidayAssignmentTask.DAYS_TO_PROCESS_PARAM, request.getParameter( HolidayAssignmentTask.DAYS_TO_PROCESS_PARAM ) );
        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTask.HOLIDAY_CALENDAR_CHECKBOX ) ) )
            newParams.put( HolidayAssignmentTask.HOLIDAY_CALENDAR_CHECKBOX, request.getParameter( HolidayAssignmentTask.HOLIDAY_CALENDAR_CHECKBOX ) );
        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTask.EMPLOYEE_IDS_PARAM ) ) )
            newParams.put( HolidayAssignmentTask.EMPLOYEE_IDS_PARAM, request.getParameter( HolidayAssignmentTask.EMPLOYEE_IDS_PARAM ) );
        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTask.TEAM_IDS_PARAM ) ) )
            newParams.put( HolidayAssignmentTask.TEAM_IDS_PARAM, request.getParameter( HolidayAssignmentTask.TEAM_IDS_PARAM ) );

        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTask.SUB_TEAM_IDS_PARAM ) ) )
            newParams.put( HolidayAssignmentTask.SUB_TEAM_IDS_PARAM, request.getParameter( HolidayAssignmentTask.SUB_TEAM_IDS_PARAM ) );

        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTask.PAY_GROUP_IDS_PARAM ) ) )
            newParams.put( HolidayAssignmentTask.PAY_GROUP_IDS_PARAM, request.getParameter( HolidayAssignmentTask.PAY_GROUP_IDS_PARAM ) );
        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTask.CALC_GROUP_IDS_PARAM ) ) )
            newParams.put( HolidayAssignmentTask.CALC_GROUP_IDS_PARAM, request.getParameter( HolidayAssignmentTask.CALC_GROUP_IDS_PARAM ) );

        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTaskExt.EXACT_DAYS_TO_PROCESS_PARAM ) ) )
            newParams.put( HolidayAssignmentTaskExt.EXACT_DAYS_TO_PROCESS_PARAM, request.getParameter( HolidayAssignmentTaskExt.EXACT_DAYS_TO_PROCESS_PARAM ) );

        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTaskExt.RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM ) ) )
            newParams.put( HolidayAssignmentTaskExt.RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM, request.getParameter( HolidayAssignmentTaskExt.RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM ) );

        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTaskExt.RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM ) ) )
            newParams.put( HolidayAssignmentTaskExt.RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM, request.getParameter( HolidayAssignmentTaskExt.RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM ) );

        if ( !StringHelper.isEmpty( request.getParameter( HolidayAssignmentTaskExt.CHECK_EMPLOYEES_DAYS_BACK_PARAM ) ) )
            newParams.put( HolidayAssignmentTaskExt.CHECK_EMPLOYEES_DAYS_BACK_PARAM, request.getParameter( HolidayAssignmentTaskExt.CHECK_EMPLOYEES_DAYS_BACK_PARAM ) );

        newParams.put( HolidayAssignmentTask.USERNAME_PARAM, USER_NAME_PARAM.toString() );
        newParams.put( HolidayAssignmentTask.CLIENT_ID_PARAM, SecurityService.getCurrentClientId() );

        if ( rec != null )
        {
            scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), newParams );
            %>
            <script><!--
                window.location = '<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';
            //--></script><%
        }
        else // submitting on-the-fly
        {
        %>
            <wba:table caption='Status' width='400'>
                <wba:tr><wba:td><wb:localize id='Holiday_status_start'>Holiday Assignment Process started ... </wb:localize></wba:td></wba:tr>
                <wba:tr><wba:td><wb:localize id='Holiday_wait'>Holiday assigment is being done.  You will receive a Workmail when the process has completed.</wb:localize></wba:td></wba:tr>
            </wba:table>
<%
                // submit a new job
                out.flush();
                response.flushBuffer();
                ScheduledJob task = new HolidayAssignmentTask();
                task.run( -1, newParams); %>
            <wba:table width='400'>
                <wba:tr><wba:td><wb:localize id='Holiday_status_end'>Holiday Assignment Ended </wb:localize></wba:td></wba:tr>
            </wba:table>
<%                out.flush();
        }
    }

    boolean usesHoliday = WorkbrainParametersRetriever.getBoolean( "HOLIDAY/HOLIDAY_ASSIGNMENT_USE_HOLIDAY" , true );
    boolean usesHolidayCalendar = WorkbrainParametersRetriever.getBoolean( "HOLIDAY/HOLIDAY_ASSIGNMENT_USE_CALENDAR" , false );
    if ( !usesHoliday && !usesHolidayCalendar )
        usesHoliday = true;

    Map params;
    if ( StringHelper.isEmpty( TASK_ID.toString() ) )
      params = new HashMap();
    else
      params = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));

    if ( !"submit".equalsIgnoreCase( operation.toString() ) )
    {

        LocalizationHelper lh = new LocalizationHelper(pageContext);
        int mfrmId = 651; // mfrmId allocated for this form
        if (request.getParameter("mfrm_id") != null) {
          mfrmId = Integer.parseInt(String.valueOf(request.getParameter("mfrm_id")));
        }
        String sMfrmId = String.valueOf(mfrmId);
        boolean controlVisible = true;
    %>
    <wba:table caption='Holiday Assignment Extension Parameters' captionLocalizeIndex='Holiday_Assignment_Parameters'>

        <!-- scheduled task paramters -->
        <% if ( usesHoliday ) { %>
        <wba:tr>
            <wb:define id='HOLIDAY_IDS_PARAM'><%= HolidayAssignmentTask.HOLIDAY_IDS_PARAM %></wb:define>
        <wba:th>
                <wb:localize id='Holiday' overrideId='<%=sMfrmId%>' type='field'>Holiday</wb:localize>
        </wba:th>
        <wba:td>
              <wb:controlField submitName='#HOLIDAY_IDS_PARAM#' overrideId='<%=sMfrmId%>' id='Holiday' cssClass="inputField"><%= getMapParam( params, HolidayAssignmentTask.HOLIDAY_IDS_PARAM ) %></wb:controlField>
        </wba:td>
        </wba:tr>
        <% } %>

        <% if ( usesHolidayCalendar ) { %>
        <wba:tr>
            <wb:define id='HOLIDAY_CALENDAR_IDS_PARAM'><%= HolidayAssignmentTask.HOLIDAY_CALENDAR_IDS_PARAM %></wb:define>
        <wba:th>
            <wb:localize id='Holiday_Calendar' overrideId='<%=sMfrmId%>' type='field'>Holiday Calendar</wb:localize>
        </wba:th>
        <wba:td>
            <wb:controlField submitName='#HOLIDAY_CALENDAR_IDS_PARAM#' overrideId='<%=sMfrmId%>' id='Holiday_Calendar' cssClass="inputField"><%= getMapParam( params, HolidayAssignmentTask.HOLIDAY_CALENDAR_IDS_PARAM ) %></wb:controlField>
        </wba:td>
        </wba:tr>
        <% } %>

        <% if ( rec != null ) { %>
        <wba:tr>
            <wb:define id='DAYS_TO_PROCESS_PARAM'><%= HolidayAssignmentTask.DAYS_TO_PROCESS_PARAM %></wb:define>
        <wba:th>
                <wb:localize id='Days_To_Process'>Days to Process</wb:localize>
        </wba:th>
        <wba:td>
                <wb:controlField
                    uiParameter="scale=4 precision=0 width='42'"
                    submitName='#DAYS_TO_PROCESS_PARAM#' ui='NumberUI'><%= getMapParam( params, HolidayAssignmentTask.DAYS_TO_PROCESS_PARAM ) %></wb:controlField>
        </wba:td>
        </wba:tr>
        <% } %>

        <% if ( usesHolidayCalendar ) { %>
        <wba:tr>
            <wb:define id='HOLIDAY_CALENDAR_CHECKBOX'><%= HolidayAssignmentTask.HOLIDAY_CALENDAR_CHECKBOX %></wb:define>
        <wba:td colspan='2'>
                <wb:localize id='Holiday_Calendar_Checkbox'>Select this checkbox to disregard the relationship between<br>holiday calendar and employee calculation groups.  <br>If checked, employee selection through the criteria below is mandatory.</wb:localize>
        </wba:td>
        <wba:td>
                <wb:controlField
                    submitName='#HOLIDAY_CALENDAR_CHECKBOX#' ui='CheckboxUI'
                    ><%= getMapParam( params, HolidayAssignmentTask.HOLIDAY_CALENDAR_CHECKBOX ) %></wb:controlField>
        </wba:td>
        </wba:tr>
        <% } %>

        <% controlVisible = lh.isControlVisible("Employee", mfrmId);
        if (controlVisible) {  %>
            <wba:tr>
                <wb:define id='EMPLOYEE_IDS_PARAM'><%= HolidayAssignmentTask.EMPLOYEE_IDS_PARAM %></wb:define>
            <wba:th>
                <wb:localize id='Employee' overrideId='<%=sMfrmId%>' type='field'>Employee</wb:localize>
            </wba:th>
            <wba:td>
                <wb:controlField submitName='#EMPLOYEE_IDS_PARAM#' overrideId='<%=sMfrmId%>' id='Employee' cssClass="inputField"><%= getMapParam( params, HolidayAssignmentTask.EMPLOYEE_IDS_PARAM ) %></wb:controlField>
            </wba:td>
            </wba:tr>
        <% } %>

        <% controlVisible = lh.isControlVisible("Team", mfrmId);
        if (controlVisible) {  %>
            <wba:tr>
            <wb:define id='TEAM_IDS_PARAM'><%= HolidayAssignmentTask.TEAM_IDS_PARAM %></wb:define>
            <wba:th>
                <wb:localize id='Team' overrideId='<%=sMfrmId%>' type='field'>Team</wb:localize>
            </wba:th>
            <wba:td>
                <wb:controlField submitName='#TEAM_IDS_PARAM#' overrideId='<%=sMfrmId%>' id='Team' cssClass="inputField"><%= getMapParam( params, HolidayAssignmentTask.TEAM_IDS_PARAM ) %></wb:controlField>
            </wba:td>
            </wba:tr>
        <% } %>

        <% controlVisible = lh.isControlVisible("Sub_Team", mfrmId);
        if (controlVisible) {  %>
            <wba:tr>
            <wb:define id='SUB_TEAM_IDS_PARAM'><%= HolidayAssignmentTask.SUB_TEAM_IDS_PARAM %></wb:define>
            <wba:th>
                <wb:localize id='SubTeam' overrideId='<%=sMfrmId%>' type='field'>Include Sub Teams</wb:localize>
            </wba:th>
            <wba:td>
                <wb:controlField submitName='#SUB_TEAM_IDS_PARAM#' overrideId='<%=sMfrmId%>' id='SubTeam' ui="CheckBoxUI" cssClass="inputField"><%= getMapParam( params, HolidayAssignmentTask.SUB_TEAM_IDS_PARAM ) %></wb:controlField>
            </wba:td>
            </wba:tr>
        <% } %>

        <% controlVisible = lh.isControlVisible("Payroll_Group", mfrmId);
        if (controlVisible) {  %>
            <wba:tr>
                <wb:define id='PAY_GROUP_IDS_PARAM'><%= HolidayAssignmentTask.PAY_GROUP_IDS_PARAM %></wb:define>
            <wba:th>
                <wb:localize id='Payroll_Group' overrideId='<%=sMfrmId%>' type='field'>Payroll Group</wb:localize>
            </wba:th>
            <wba:td>
                <wb:controlField submitName='#PAY_GROUP_IDS_PARAM#' overrideId='<%=sMfrmId%>' id='Payroll_Group' cssClass="inputField"><%= getMapParam( params, HolidayAssignmentTask.PAY_GROUP_IDS_PARAM ) %></wb:controlField>
            </wba:td>
            </wba:tr>
        <% } %>

        <% controlVisible = lh.isControlVisible("Calc_Group", mfrmId);
        if (controlVisible) { %>
            <wba:tr>
                <wb:define id='CALC_GROUP_IDS_PARAM'><%= HolidayAssignmentTask.CALC_GROUP_IDS_PARAM %></wb:define>
            <wba:th>
                <wb:localize id='Calc_Group' overrideId='<%=sMfrmId%>' type='field'>Calc Group</wb:localize>
            </wba:th>
            <wba:td>
                <wb:controlField submitName='#CALC_GROUP_IDS_PARAM#' overrideId='<%=sMfrmId%>' id='Calc_Group' cssClass="inputField"><%= getMapParam( params, HolidayAssignmentTask.CALC_GROUP_IDS_PARAM ) %></wb:controlField>
            </wba:td>
            </wba:tr>
        <% } %>

 		<% controlVisible = lh.isControlVisible("Exact_Days_To_Process", mfrmId);
        if (controlVisible) { %>
	        <wba:tr>
	            <wb:define id='EXACT_DAYS_TO_PROCESS_PARAM'><%= HolidayAssignmentTaskExt.EXACT_DAYS_TO_PROCESS_PARAM %></wb:define>
	        <wba:th>
	        	Exact Days to Process
	        </wba:th>
	        <wba:td>
	        	<wb:controlField uiParameter="scale=4 precision=0 width='42'" submitName='#EXACT_DAYS_TO_PROCESS_PARAM#' ui='NumberUI'><%= getMapParam( params, HolidayAssignmentTaskExt.EXACT_DAYS_TO_PROCESS_PARAM ) %></wb:controlField>
	        </wba:td>
	        </wba:tr>
        <% } %>

        <% controlVisible = lh.isControlVisible("Run_For_New_Employees_Only", mfrmId);
        if (controlVisible) { %>
            <wba:tr>
            <wb:define id='RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM'><%= HolidayAssignmentTaskExt.RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM %></wb:define>
            <wba:th>
                Run for New Employees Only
            </wba:th>
            <wba:td>
                <wb:controlField submitName='#RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM#' overrideId='<%=sMfrmId%>' id='RunForNewEmployeesOnly' ui="CheckBoxUI" cssClass="inputField"><%= getMapParam( params, HolidayAssignmentTaskExt.RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM ) %></wb:controlField>
            </wba:td>
            </wba:tr>
        <% } %>

        <% controlVisible = lh.isControlVisible("Run_For_Changed_Employees_Attributes", mfrmId);
        if (controlVisible) { %>
            <wba:tr>
            <wb:define id='RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM'><%= HolidayAssignmentTaskExt.RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM %></wb:define>
            <wba:th>
                Run for Changed Employees Attributes
            </wba:th>
            <wba:td>
                <wb:controlField submitName='#RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM#' overrideId='<%=sMfrmId%>' id='RunForChangedEmployeesAttribute' ui='StringUI' uiParameter='width=42' cssClass="inputField"><%= getMapParam( params, HolidayAssignmentTaskExt.RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM ) %></wb:controlField>
            </wba:td>
            </wba:tr>
        <% } %>

        <% controlVisible = lh.isControlVisible("Check_Employees_Days_Back", mfrmId);
        if (controlVisible) { %>
	        <wba:tr>
	            <wb:define id='CHECK_EMPLOYEES_DAYS_BACK_PARAM'><%= HolidayAssignmentTaskExt.CHECK_EMPLOYEES_DAYS_BACK_PARAM %></wb:define>
	        <wba:th>
	        	Check for New and Changed Employees Days Back
	        </wba:th>
	        <wba:td>
	        	<wb:controlField uiParameter="scale=4 precision=0 width='42'" submitName='#CHECK_EMPLOYEES_DAYS_BACK_PARAM#' ui='NumberUI'><%= getMapParam( params, HolidayAssignmentTaskExt.CHECK_EMPLOYEES_DAYS_BACK_PARAM ) %></wb:controlField>
	        </wba:td>
	        </wba:tr>
        <% } %>

    </wba:table>

    <!-- submission logic -->
    <wb:submit id='operation'>submit</wb:submit>
    <wba:button label='Submit' labelLocalizeIndex='Submit' onClick='checkSubmit();'/>&nbsp;
    <wba:button label='Cancel' labelLocalizeIndex='Cancel' onClick="disableAllButtons(); window.location='#contextPath#/system/history.jsp?goBack=2';"/>

    <script>
        function checkSubmit()
        {
            var form = document.page_form;
            var holidayCalendarCheckbox = form.<%= HolidayAssignmentTask.HOLIDAY_CALENDAR_CHECKBOX %>;
            var hasHolCriteria = false;
            hasHolCriteria |= form.<%= HolidayAssignmentTask.HOLIDAY_IDS_PARAM %>
                    && form.<%= HolidayAssignmentTask.HOLIDAY_IDS_PARAM %>.value != '';

            hasHolCriteria |= form.<%= HolidayAssignmentTaskExt.EXACT_DAYS_TO_PROCESS_PARAM %>
                    && form.<%= HolidayAssignmentTaskExt.EXACT_DAYS_TO_PROCESS_PARAM %>.value != '';

            var hasEmpCriteria = false;
            hasEmpCriteria |= form.<%= HolidayAssignmentTask.CALC_GROUP_IDS_PARAM %>
                    && form.<%= HolidayAssignmentTask.CALC_GROUP_IDS_PARAM %>.value != '';

            hasEmpCriteria |= form.<%= HolidayAssignmentTask.EMPLOYEE_IDS_PARAM %>
                    && form.<%= HolidayAssignmentTask.EMPLOYEE_IDS_PARAM %>.value != '';

            hasEmpCriteria |= form.<%= HolidayAssignmentTask.TEAM_IDS_PARAM %>
                    && form.<%= HolidayAssignmentTask.TEAM_IDS_PARAM %>.value != '';

            hasEmpCriteria |= form.<%= HolidayAssignmentTask.PAY_GROUP_IDS_PARAM %>
                    && form.<%= HolidayAssignmentTask.PAY_GROUP_IDS_PARAM %>.value != '';
            /*
			hasEmpCriteria |= form.<%= HolidayAssignmentTaskExt.EXACT_DAYS_TO_PROCESS_PARAM %>
                    && form.<%= HolidayAssignmentTaskExt.EXACT_DAYS_TO_PROCESS_PARAM %>.value != '';

			hasEmpCriteria |= form.<%= HolidayAssignmentTaskExt.RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM %>
                    && form.<%= HolidayAssignmentTaskExt.RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM %>.value != '';

			hasEmpCriteria |= form.<%= HolidayAssignmentTaskExt.RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM %>
                    && form.<%= HolidayAssignmentTaskExt.RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM %>.value != '';
            */
            if ( holidayCalendarCheckbox ) {
                hasHolCriteria |= form.<%= HolidayAssignmentTask.HOLIDAY_CALENDAR_IDS_PARAM %>
                        && form.<%= HolidayAssignmentTask.HOLIDAY_CALENDAR_IDS_PARAM %>.value != '';

                if (!hasHolCriteria) {
                    alert('<wb:localize ignoreConfig="true" id="AT_LEAST_ONE_HOLIDAY_OR_HOLIDAY_CALENDAR_CRITERIA_MUST_BE_SPECIFIED">At least one holiday or holiday calendar criteria must be specified</wb:localize>');
                    return false;
                }

                if ( holidayCalendarCheckbox.checked && !hasEmpCriteria ) {
                    alert('<wb:localize ignoreConfig="true" id="ONE_OR_MORE_EMPLOYEE_CRITERIA_MUST_BE_SPECIFIED">One or more employee selection criteria must be specified</wb:localize>');
                    return false;
                }

                if ( form.<%= HolidayAssignmentTask.HOLIDAY_CALENDAR_IDS_PARAM %>
                     && form.<%= HolidayAssignmentTask.HOLIDAY_CALENDAR_IDS_PARAM %>.value != ''
                     && !holidayCalendarCheckbox.checked
                     && hasEmpCriteria ) {
                    alert('<wb:localize ignoreConfig="true" id="PLEASE_READ_THE_EXPLANATION_NEXT_TO_CHECKBOX">Cannot submit this job. \n You have not selected to disregard holiday calendar - employee calculation group relationships \n but you have specificied employee selection criteria. \n Please read the explanation next to checkbox.</wb:localize>');
                    return false;
                }

                if ( form.<%= HolidayAssignmentTask.HOLIDAY_IDS_PARAM %>
                     && form.<%= HolidayAssignmentTask.HOLIDAY_IDS_PARAM %>.value != ''
                     && form.<%= HolidayAssignmentTask.HOLIDAY_CALENDAR_IDS_PARAM %>
                     && form.<%= HolidayAssignmentTask.HOLIDAY_CALENDAR_IDS_PARAM %>.value == ''
                     && !holidayCalendarCheckbox.checked
                     && !hasEmpCriteria ) {
                    alert('<wb:localize ignoreConfig="true" id="PLEASE_SPECIFY_HOLIDAY_CALENDAR">Cannot submit this job. \n You have not selected to disregard holiday calendar - employee calculation group relationships \n but you have not specified any holiday calendar. \n Please specifiy holiday calendar.</wb:localize>');
                    return false;
                }

            } else {
                if (!hasHolCriteria) {
                    alert('<wb:localize ignoreConfig="true" id="AT_LEAST_ONE_HOLIDAY_CRITERIA_MUST_BE_SPECIFIED">At least one holiday criteria must be specified</wb:localize>');
                    return false;
                }
                if (!hasEmpCriteria ) {
                    alert('<wb:localize ignoreConfig="true" id="ONE_OR_MORE_EMPLOYEE_CRITERIA_MUST_BE_SPECIFIED">One or more employee selection criteria must be specified</wb:localize>');
                    return false;
                }
            }

            if ( form.<%= HolidayAssignmentTask.DAYS_TO_PROCESS_PARAM %> )
            {
                var daysToProcess = form.<%= HolidayAssignmentTask.DAYS_TO_PROCESS_PARAM %>.value;
                if ( daysToProcess != '' )
                {
                    daysToProcess = parseInt( daysToProcess, 10 );
                    if ( daysToProcess <= 0 )
                    {
                        alert('<wb:localize ignoreConfig="true" id="DAYS_TO_PROCESS_MUST_BE_POSITIVE">Days To Process must be positive, use blank to indicate all days</wb:localize>');
                        return false;
                    }
                }
            }
            disableAllButtons();
            document.page_form.submit();
        }

    </script>
<% } // if not submitting %>
</wb:page>
