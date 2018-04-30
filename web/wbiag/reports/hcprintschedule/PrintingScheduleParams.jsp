<%@ page import="java.lang.*,
                 com.workbrain.server.jsp.locale.LocalizationDictionary,
                 com.workbrain.server.jsp.taglib.util.LocalizableTag" %>
<%@ page import="java.lang.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.io.*" %>

<%@ page import="com.workbrain.server.jsp.*" %>
<%@ page import="com.workbrain.server.registry.Registry" %>
<%@ page import="com.workbrain.util.*" %>
<%@ page import="com.workbrain.sql.*" %>
<%@ page import="com.workbrain.app.jsp.util.Timesheet" %>
<%@ page import="com.workbrain.app.jsp.workbrain.overrides.*" %>
<%@ page import="com.workbrain.app.jsp.workbrain.reportquery.*"%>
<%@ page import="com.workbrain.app.ta.db.*" %>
<%@ page import="com.workbrain.app.ta.model.*" %>
<%@ page import="com.workbrain.server.*" %>
<%@ page import="com.workbrain.security.*" %>
<%@ page import="com.wbiag.app.modules.reports.hcprintschedule.*" %>

<%@ page import="com.lowagie.text.Document" %>
<%@ page import="com.lowagie.text.pdf.PdfWriter" %>

<%@ include file="/system/wbheader.jsp"%>

<wb:page>

<wb:define id='errorMsg'/>
<wb:define id="userName"><wb:getPageProperty id="userName"/></wb:define>
<wb:define id='mfrm_id'><wb:get id='mfrm_id' scope='parameter'/></wb:define>
<wb:submit id="mfrm_id"><%=mfrm_id%></wb:submit>
<wb:define id='action' ><wb:get id='action' scope='parameter' default=''/></wb:define>
<wb:submit id='action' ><wb:get id='action'/></wb:submit>
<wb:define id='field_changed' ><wb:get id='field_changed' scope='parameter' default=''/></wb:define>
<wb:submit id='field_changed' ><wb:get id='field_changed' default=''/></wb:submit>


<%!
  /**
   * @author      Ali Ajellu
   * @version     1.0
   * Date:        Friday, June 16 206
   * Copyright:   Workbrain Corp.
   * TestTrack:   1630
   *
   * This JSP is the parameter page for the Printing Schedule report.
   */

  /**
   * Determines whether this report is a Personal Report.
   * Used by the Save Parameters functionality
   */
  private boolean isPersRep(){
    boolean persRep;

    String defRep = Registry.getVarString("system/WORKBRAIN_PARAMETERS/DEFAULT_REPORT_TYPE", "");

    if (StringHelper.isEmpty(defRep.toString())) {
       //If registry parameter does not exist or does not have a value set it to a default value
       defRep = "Personal";
    }

    if (defRep.equalsIgnoreCase("Personal"))
      persRep = true;
    else
      persRep = false;

    return persRep;

  }
%>

<script language='javascript' type='text/javascript'>

  /********************* JAVASCRIPT COMMONG FUNCTIONS *********************/
  //override the brokn getElement function
  function getElement(name){
    returnValue = document.getElementsByName(name)[0];
    if (returnValue == null){
      returnValue = document.getElementById(name);
    }

    return returnValue;
  }

  //defines behaviour of Save, Delete and Personal checkboxes used in the
  //Save Parameters functionality.
  function ExclusiveUpdate(sName, boxValue) {
    if (sName=="save_params") {
      document.forms[0].delete_params.checked = false;
      if(boxValue==true){
         if (defaultPersRep) {
              document.forms[0].user_report.checked = document.forms[0].save_params.checked;
         }
      }else{
          document.forms[0].user_report.checked = false;
      }
    }
    else if (sName=="delete_params") {
      document.forms[0].save_params.checked = false;
    document.forms[0].user_report.checked = false;
    }
    else if (sName=="user_report") {
      if(boxValue==true){
        document.forms[0].save_params.checked = true;
        document.forms[0].delete_params.checked = false;
      }
    }
  }

  //reloads the page
  function reload(field_changed){
    setFieldChanged(field_changed);
    submitForm();

  }

  //fixes and validates forms, sets the action to "Run Report" and submits the form
  function runReport(){
    fixFields();
    isValid = validateFields();

    if (isValid){
      setAction("Run Report");
      submitForm();
    }
  }

  //set the page's action parameter
  function setAction(actionStr){
    actionField = getElement('action');
    actionField.value = actionStr;
  }

  //submits the form
  function submitForm(){
     getElement('btnRunReport').disabled = true;
     document.forms[0].submit();
  }

  //sets the 'field changed' parameter. Used by page to determine if any
  //dependent fields should be cleared as a result of the field that changed
  function setFieldChanged(field_changed){
    field_changed_param = getElement('field_changed');
    field_changed_param.value = field_changed;
  }

  //if any field (except start and end date) are empty, fill them with ALL
  function fixFields(){
    wbt_ids_0 = getElement('WBT_IDS_0');
    daypart_ids_0 = getElement('DAYPART_IDS_0');
    job_ids_0 = getElement('JOB_IDS_0');
    emp_ids_0 = getElement('EMP_IDS_0');

    fields = [wbt_ids_0, daypart_ids_0, job_ids_0, emp_ids_0];

    for (fieldsIndex =0; fieldsIndex < fields.length; fieldsIndex++){
      if (fields[fieldsIndex].value == ''){
        fields[fieldsIndex].value = 'ALL';
        labelField = getElement(fields[fieldsIndex].name+'_label');
        labelField.value = 'ALL';
      }
    }
  }

  //validates fields (atm, only date fields)
  function validateFields(){
    startDate = getElement('START_DATE_0').value;
    endDate = getElement('END_DATE_0').value;
    if((startDate=='' && endDate!='') ||
       (startDate!='' && endDate=='')){

        alert('The Start and End Date must either both be full or both empty');
        return false;
    }


    return true;
  }

  //get the difference b/w 2 dats in days
  function DaysDiff(D1, D2) {
    return Math.abs(Math.round((D1-D2)/864e5));
  }

  /****************** JAVASCRIPT EVENT HANDLERS *******************/
  function fixStartEndDates(){
    startDate = getElement('START_DATE_0').value;
    startDate_dummy = getElement('START_DATE_0_dummy').value;
    endDate = getElement('END_DATE_0').value;

    if (startDate != '' && endDate != ''){
      if (startDate > endDate){
        getElement('END_DATE_0').value = startDate;
        getElement('END_DATE_0_dummy').value = startDate_dummy;
      }

      startDate_date = startDate.toDate('yyyyMMdd HHmmss');
      endDate_date = endDate.toDate('yyyyMMdd HHmmss');
      daysDiff = DaysDiff(startDate_date, endDate_date);

      //days are inclusive, so a difference of 41 days = 42 days in report.
      if (daysDiff >= 42){
        newEndDate = new Date(startDate_date.getTime());
        newEndDate.setDate(newEndDate.getDate()+41);

        year = newEndDate.getYear().toString();
        month = (newEndDate.getMonth()+1).toString();
        day = (newEndDate.getDate()).toString();

        getElement('END_DATE_0').value = formatDateHidden(day, month, year);
        getElement('END_DATE_0_dummy').value = formatDate(day, month, year, 'MM/dd/yyyy');

      }
    }



  }

  function onClick_WBT_IDS_0_button_ALL(){
    simulateALLAndReload('WBT_IDS_0');
  }

  function onClick_DAYPART_IDS_0_button_ALL(){
    simulateALLAndReload('DAYPART_IDS_0');
  }

  function onClick_JOB_IDS_0_button_ALL(){
    simulateALLAndReload('JOB_IDS_0');
  }

  function onClick_EMP_IDS_0_button_ALL(){
    simulateALLAndReload('EMP_IDS_0');
  }

  function simulateALLAndReload(fieldName){
    field = getElement(fieldName);
    field_label = getElement(fieldName+'_label');

    field.value = 'ALL';
    field_label.value = 'ALL';
    field.resultSelected='';
    field.itemsCount=0;

    reload(fieldName);
  }


</script>

<%
DBConnection conn = JSPHelper.getConnection(request);
CodeMapper cm = CodeMapper.createBrandNewCodeMapper(conn);

int userId = SecurityService.getCurrentUser().getUserId();

boolean persRep = isPersRep();
String wbt_ids = "";                  //tied to WBT_IDS_0
String start_date = "";               //tied to START_DATE_0
String end_date = "";                 //tied to END_DATE_0
String daypart_ids = "";              //tied to DAYPART_IDS_0
String daypart_sort_dir = "";         //tied to DAYPART_SORT_DIR_0
String job_ids = "";                  //tied to JOB_IDS_0
String job_sort_dir = "";             //tied to JOB_SORT_DIR_0
String emp_ids = "";                  //tied to EMP_IDS_0

String totals = "";      

String deleteParams = "";             //tied to delete_params
String saveParams = "";               //tied to save_params
String saveName = "";                 //tied to save_name
String saveDesc = "";                 //tied to save_desc
String userReport = "";               //tied to user_report

String fieldChanged = "";             //tied to field_changed

Date start_date_d = null;             //date object tied to START_DATE_0
Date end_date_d = null;               //date object tied to START_DATE_0

/************** MAIN FORM'S PARAMS ************/
if (request.getParameter("WBT_IDS_0")!=null)
  wbt_ids = request.getParameter("WBT_IDS_0");
if (request.getParameter("START_DATE_0")!=null){
  start_date = request.getParameter("START_DATE_0");
  if(!StringHelper.isEmpty(start_date)){
    start_date_d  = DateHelper.convertStringToDate(start_date, "yyyyMMdd HHmmss");
  }
}
if (request.getParameter("END_DATE_0")!=null &&
    !StringHelper.isEmpty(request.getParameter("END_DATE_0"))){
  end_date = request.getParameter("END_DATE_0");
  if (!StringHelper.isEmpty(end_date)){
    end_date_d  = DateHelper.convertStringToDate(end_date, "yyyyMMdd HHmmss");
  }
}
if (request.getParameter("DAYPART_IDS_0")!=null)
  daypart_ids = request.getParameter("DAYPART_IDS_0");
if (request.getParameter("DAYPART_SORT_DIR_0")!=null)
  daypart_sort_dir = request.getParameter("DAYPART_SORT_DIR_0");
if (request.getParameter("JOB_IDS_0")!=null)
  job_ids = request.getParameter("JOB_IDS_0");
if (request.getParameter("JOB_SORT_DIR_0")!=null)
  job_sort_dir = request.getParameter("JOB_SORT_DIR_0");
if (request.getParameter("EMP_IDS_0")!=null)
  emp_ids = request.getParameter("EMP_IDS_0");

if (request.getParameter("TOTALS")!=null)
  totals = request.getParameter("TOTALS");

/*************** SAVE PARAMS PARAMS ***********/
if (request.getParameter("delete_params")!=null)
  deleteParams = request.getParameter("delete_params");
if (request.getParameter("save_params")!=null)
  saveParams = request.getParameter("save_params");
if (request.getParameter("save_name")!=null)
  saveName = request.getParameter("save_name");
if (request.getParameter("save_desc")!=null)
  saveDesc = request.getParameter("save_desc");
if (request.getParameter("user_report")!=null)
  userReport = request.getParameter("user_report");
if (request.getParameter("field_changed")!=null)
  fieldChanged = request.getParameter("field_changed");

/************ END OF PARAM RETRIEVAL *********/

//prepare queries for dblookups
%>
<wb:define id='jobIdsQuery'> <%=PrintingScheduleModel.getJobIdsQuery(wbt_ids, userId)%> </wb:define>
<wb:define id='empIdsQuery'> <%=PrintingScheduleModel.getEmpIdsQuery(wbt_ids, userId, start_date_d, end_date_d, conn)%> </wb:define>
<%

/************* SAVE PARAMS *******************/
if( saveParams.equalsIgnoreCase("T") || deleteParams.equalsIgnoreCase("T") ) {
   String _saveName   = "";
   String _saveDesc   = "";
   String _userReport = "ALL_USERS";   // Default access control to saved params
   String sqlNameCheck = "";
   String aParam;
   Enumeration enumParamNames = request.getParameterNames();
   String wantedParams = "!mfrm_id!WBT_IDS_0!START_DATE_0!END_DATE_0!DAYPART_IDS_0!DAYPART_SORT_DIR_0!JOB_IDS_0!JOB_SORT_DIR_0!EMP_IDS_0!TOTALS!";
   StringBuffer reportParams = new StringBuffer();

   // get report params submitted
   reportParams.append("save_name=" + saveName + "&");
   reportParams.append("save_desc=" + saveDesc + "&");

   while( enumParamNames.hasMoreElements() ) {
      aParam = (String)enumParamNames.nextElement();
      if( wantedParams.indexOf("!" + aParam + "!") != -1  ) {
         reportParams.append( aParam + "=" + request.getParameter(aParam)+ "&" );
      }
   }


   if( saveParams.equalsIgnoreCase("T") ) {
	    // extra handling when save param
      if( saveName.trim().length()==0 || saveDesc.trim().length()==0 ) {%>
         <wb:set id="errorMsg"><wb:localize id="Report_Name_must_not_be_blank">Name and Description must not be blank to save report parameters.  Parameters not saved.</wb:localize></wb:set>
      <%} else {
          _saveName = StringHelper.searchReplace(saveName, "'", "''");
          _saveDesc = StringHelper.searchReplace(saveDesc, "'", "''");
         if( userReport.equalsIgnoreCase("T") ) {
             _userReport = userName.toString();
         }
         sqlNameCheck = "SELECT COUNT(*) X FROM REPORT_SAVED WHERE REPTSAV_NAME = '" + _saveName +"'";
         %>
         <wb:sql createDataSource="sqlNameCheck"><%=sqlNameCheck%></wb:sql>
         <wb:dataSet dataSource="sqlNameCheck" id="ds">
             <wb:if expression="#ds.X != '0'#">
               <%
			   %>
			 </wb:if>

         </wb:dataSet>
      <%}  // end handling for save
   } else if (deleteParams.equalsIgnoreCase("T") ) {
      _saveName = StringHelper.searchReplace(saveName, "'", "''");
      _saveDesc = StringHelper.searchReplace(saveDesc, "'", "''");
   }%>

   <%-- if no error, perform save or delete action (in REPORT_SAVED) --%>
   <%if(!errorMsg.toString().equals("")){
	   %>
      <p><%=errorMsg%><p>
   <%}else{
      if(deleteParams.equals("T")){

        SavedReport.deleteReport( conn, _saveName );
        %><p><wb:localize id="Parameters_deleted_for_saved">'<%=_saveName%>' has been deleted.</wb:localize><p><%
      }else{
		//HUB 36378 modify saved parameters here
		SavedReport.updateOrInsertReport( conn, _saveName ,_saveDesc, mfrm_id.toString(), reportParams.toString(),_userReport );
        %><p><wb:localize id="Parameters_saved">'<%=_saveName%>' has been saved.</wb:localize><p><%
      }
   }
}  // end save or delete


/********************** RUN REPORT ***********************/
if ("Run Report".equals(action.toString())){
%>
<wb:forward page="/reports/PDFPrintSchedule" />
<%
}
%>

<%------------------------- RENDER PARAMS CONTROLS -------------------------%>
<wba:table caption="Printing Schedule Report" captionLocalizeIndex="Printing_Schedule_Report">
  <wba:tr>
    <wba:th>
      <wb:localize id="Team">Team</wb:localize> *
    </wba:th>
    <wba:td>
      <wb:controlField
            submitName='WBT_IDS_0'
            onChange="reload('WBT_IDS_0')"
            ui='DBLookupUI'
            uiParameter="multiChoice=true sourceType=REGISTERED source='SEC_WORKBRAIN_TEAM' sourceParams='ignoreCols==WBROLE_ID, WBUT_START_DATE, WBUT_END_DATE' sourceDisplayFields='WBT_NAME' all=true labelFieldStatus=edit title='LOOKUP_TEAM'"
      ><%=wbt_ids%></wb:controlField>
    </wba:td>
    <wba:td> &nbsp; </wba:td>
    <wba:td> &nbsp; </wba:td>
  </wba:tr>
  <wba:tr>
    <wba:th>
      <wb:localize id="Start_Date">Start Date</wb:localize>
    </wba:th>
    <wba:td>
      <wb:controlField
            submitName='START_DATE_0'
            onChange="reload('START_DATE_0');"
            ui='DatePickerUI'
            uiParameter="onBeforeSystemValidation='fixStartEndDates();'"
      ><%=start_date%></wb:controlField>
    </wba:td>
    <wba:td> &nbsp; </wba:td>
    <wba:td> &nbsp; </wba:td>
  </wba:tr>
  <wba:tr>
    <wba:th>
      <wb:localize id="End_Date">End Date</wb:localize>
    </wba:th>
    <wba:td>
      <wb:controlField
            submitName='END_DATE_0'
            onChange="reload('END_DATE_0');"
            ui='DatePickerUI'
            uiParameter="onBeforeSystemValidation='fixStartEndDates();'"
      ><%=end_date%></wb:controlField>
    </wba:td>
    <wba:td> &nbsp; </wba:td>
    <wba:td> &nbsp; </wba:td>
  </wba:tr>
  <wba:tr>
    <wba:th>
      <wb:localize id="Day_Part">Day Part</wb:localize>
    </wba:th>
    <wba:td>

    <wb:define id='rowsource_param_wbt_ids'> <%=wbt_ids%> </wb:define>
    <wb:controlField
                submitName='DAYPART_IDS_0'
                onChange="reload('DAYPART_IDS_0')"
                ui='DBLookupUI'
                uiParameter="multiChoice=true sourceType=REGISTERED source='TEAM_DAYPARTS' sourceParams='orderBy==DP_ID~|~where==#rowsource_param_wbt_ids#' sourceKeyField='DP_ID' sourceLabelField='DP_NAME' sourceDisplayFields='DP_NAME|DP_DESC' all=true labelFieldStatus=edit title='LOOKUP_DAYPART'"
      ><%
      if (!(fieldChanged.equals("WBT_IDS_0") ||
            fieldChanged.equals("START_DATE_0") ||
            fieldChanged.equals("END_DATE_0")) ||
            daypart_ids.equals("ALL")){
        out.print(daypart_ids);
      }
      %></wb:controlField>
    </wba:td>
    <wba:th>
      <wb:localize id="Day_Part_Sort_Dir">Day Part Sort Dir.</wb:localize>
    </wba:th>
    <wba:td>
      <select name='DAYPART_SORT_DIR_0'>
        <option value='Ascending' <%if(daypart_sort_dir.equalsIgnoreCase("Ascending")) out.println("selected='selected'");%>>Ascending</option>
        <option value='Descending' <%if(daypart_sort_dir.equalsIgnoreCase("Descending")) out.println("selected='selected'");%>>Descending</option>
      </select>
    </wba:td>
  </wba:tr>
  <wba:tr>
    <wba:th>
      <wb:localize id="Job">Job</wb:localize>
    </wba:th>
    <wba:td>
      <wb:controlField
              submitName='JOB_IDS_0'
              onChange="reload('JOB_IDS_0')"
              ui='DBLookupUI'
              uiParameter="multiChoice=true sourceType=SQL source='#jobIdsQuery#' sourceKeyField='JOB_ID' sourceLabelField='JOB_NAME' sourceDisplayFields='JOB_NAME|JOB_DESC' all=true labelFieldStatus=edit title='LOOKUP_JOB'"
      ><%
      if (!(fieldChanged.equals("WBT_IDS_0") ||
            fieldChanged.equals("START_DATE_0") ||
            fieldChanged.equals("END_DATE_0") ||
            fieldChanged.equals("DAYPART_IDS_0")) ||
            job_ids.equals("ALL")){
        out.print(job_ids);
      }
      %></wb:controlField>
    </wba:td>
    <wba:th>
      <wb:localize id="Job_Sort_Dir">Job Sort Dir.</wb:localize>
    </wba:th>
    <wba:td>
      <select name='JOB_SORT_DIR_0'>
        <option value='Ascending' <%if(job_sort_dir.equalsIgnoreCase("Ascending")) out.println("selected='selected'");%>>Ascending</option>
        <option value='Descending' <%if(job_sort_dir.equalsIgnoreCase("Descending")) out.println("selected='selected'");%>>Descending</option>

      </select>
    </wba:td>
  </wba:tr>
  <wba:tr>
    <wba:th>
      <wb:localize id="Employee">Employee</wb:localize>
    </wba:th>
    <wba:td>
      <wb:controlField
            submitName='EMP_IDS_0'
            onChange="reload('EMP_IDS_0')"
            ui='DBLookupUI'
            uiParameter='multiChoice=true sourceType=SQL source="#empIdsQuery#" sourceKeyField="EMP_ID" sourceLabelField="EMP_NAME" sourceDisplayFields="EMP_NAME|EMP_FULLNAME" all=true labelFieldStatus=edit title="LOOKUP_EMP"'
      ><%
        if (!(fieldChanged.equals("WBT_IDS_0") ||
              fieldChanged.equals("START_DATE_0") ||
              fieldChanged.equals("END_DATE_0") ||
              fieldChanged.equals("DAYPART_IDS_0") ||
              fieldChanged.equals("JOB_IDS_0")) ||
              emp_ids.equals("ALL")){
          out.print(emp_ids);
        }
      %></wb:controlField>
    </wba:td>
    <wba:td> &nbsp; </wba:td>
    <wba:td> &nbsp; </wba:td>
  </wba:tr>

   <%--HUB:42198  TT:2964--%>
  <wba:tr>
    <wba:th> 
      <wb:localize id="ShowTotals">Show Totals</wb:localize>
    </wba:th>
    <wba:td>
      <wb:controlField 
	        cssClass="inputField"
            submitName="TOTALS"
			nullable="false"
            ui="ListBoxUI"
			uiParameter="labelList='TRUE, FALSE' ValueList='TRUE, FALSE'"><%=totals%></wb:controlField>
    </wba:td>
    <wba:td> &nbsp; </wba:td>
    <wba:td> &nbsp; </wba:td>
  </wba:tr>
</wba:table>

<%-------------------------- RENDER 'SAVE PARAMS' -----------------------------------%>
<br>

<wba:table caption="Save Parameters" captionLocalizeIndex="Save_Parameters">
<tr><th colspan="10">
  <wb:localize id="Save">Save</wb:localize>&nbsp;&nbsp;
  <INPUT TYPE=CHECKBOX NAME='save_params' VALUE='T' onClick='ExclusiveUpdate(this.name,this.checked)'>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <wb:localize id="Delete">Delete</wb:localize>&nbsp;&nbsp;
  <INPUT TYPE=CHECKBOX NAME='delete_params' VALUE='T' onClick='ExclusiveUpdate(this.name,this.checked)'>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <wb:localize id="Personal_Report">Personal Report</wb:localize>&nbsp;&nbsp;
  <INPUT TYPE=CHECKBOX NAME='user_report' VALUE='T' onClick='ExclusiveUpdate(this.name,this.checked)'>
</th></tr>
<tr>
  <td><wb:localize type="field" overrideId="377" id="REPORT_SAVED_NAME">Name</wb:localize>&nbsp;*</td>
  <td><wb:controlField cssClass="inputField" submitName="save_name" id="REPORT_SAVED_NAME" overrideId="377"><%=saveName%></wb:controlField></td>
</tr>
<tr>
  <td><wb:localize type="field" overrideId="377" id="REPORT_SAVED_DESCRIPTION">Description</wb:localize>&nbsp;*</td>
  <td><wb:controlField cssClass="inputField" submitName="save_desc" id="REPORT_SAVED_DESCRIPTION" overrideId="377"><%=saveDesc%></wb:controlField></td>
</tr>
</wba:table>


<wba:button name='btnRunReport' label="Run Report" labelLocalizeIndex="Run Report" onClick="runReport();"/>

<%----------------------- <END> RENDERING ----------------------------------%>

<script type='text/javascript'>
  //common scripts for setting the page up on load
  getElement("WBT_IDS_0_button_ALL").onclick =      onClick_WBT_IDS_0_button_ALL;
  getElement("DAYPART_IDS_0_button_ALL").onclick =  onClick_DAYPART_IDS_0_button_ALL;
  getElement("JOB_IDS_0_button_ALL").onclick =      onClick_JOB_IDS_0_button_ALL;
  getElement("EMP_IDS_0_button_ALL").onclick =      onClick_EMP_IDS_0_button_ALL;

  var defaultPersRep = <%=persRep%>
</script>


</wb:page>