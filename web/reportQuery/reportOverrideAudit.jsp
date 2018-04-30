<%@ include file="/system/wbheader.jsp"%>
<%@ page import="com.workbrain.util.*" %>
<%@ page import="com.workbrain.sql.*" %>
<%@ page import="com.workbrain.app.ta.db.*" %>
<%@ page import="com.workbrain.app.ta.model.*" %>
<%@ page import="com.workbrain.app.jsp.workbrain.reportquery.*"%>
<%@ page import="com.workbrain.server.jsp.*" %>
<%@ page import="com.workbrain.server.jsp.locale.*" %>
<%@ page import="java.util.*, java.sql.*, java.text.*" %>
<%@ page import="com.workbrain.server.*"%>

<%--
****************************************************************************************************
*
* PURPOSE:        Produces a report that shows overrides for selected employees
*
* DEPENDENCIES:
*
* PARAMETERS:
*           (req) mfrm_id: maintenance form id for localization param
*           (opt) action:   if "run", process report
*           (opt) following parameters set default value for report parameters:
*                    emp_name_0: employees name
*                    start_date_0, end_date_0: start/end date
*                    status: Y=success, N=failure
*
* MODIFICATION HISTORY:	None
*
****************************************************************************************************

--%>

<wb:page type="reportQuery" maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>
<%!

   String massageOvrNewValue (String ovrNew,
                  LocalizationDictionary ld, HttpServletRequest request) throws Exception {
        StringBuffer sb = new StringBuffer(200);
        sb.append("<table>");
        OverrideData od = new OverrideData();
        od.setOvrNewValue(ovrNew);
        List tokens = od.getNewOverrides();
        Iterator iter = tokens.iterator();
        while (iter.hasNext()) {
            OverrideData.OverrideToken  item = (OverrideData.OverrideToken)iter.next();
            String tokName = item.getName();
            String tokVal = item.getValue();
            String tokNameLoc = ld.localize(JSPHelper.getWebContext(request),
                       LocalizationDictionary.GLOBAL_DOMAIN, tokName);
            if (StringHelper.isEmpty(tokNameLoc)) {
                tokNameLoc =  tokName;
            }
            if (WorkSummaryData.WRKS_CLOCKS.equals(tokName)) {
                List clks = Clock.createClockListFromString(tokVal) ;
                tokVal = "";
                Iterator iterClk = clks.iterator();
                while (iterClk.hasNext()) {
                    Clock clk = (Clock)iterClk.next();
                    tokVal += DateHelper.convertDateString(clk.getClockDate(), "HH:mm");
                    tokVal += "&nbsp;";
                }
            }
            sb.append("<tr><td>" + tokNameLoc + "=</td>");
            sb.append("<td>" + tokVal + "</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
   }

%>
<%
DBConnection conn = JSPHelper.getConnection(request);
%>
<script language=javascript>

/*****************************
 Javascript Global Variables
*****************************/

/*****************************
 Javascript functions
*****************************/
/*--begin #1120#
  --mharrison
  --Sept. 20, 2001
  --added ability to save report settings
*/
function ExclusiveUpdate(sName, boxValue) {
  if (sName=="save_params") {
    if(boxValue==true){
  	  document.forms[0].delete_params.checked = false;
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
	}else{
	  document.forms[0].save_params.checked = false;
	}
  }
}

function onSubmit(action) {
   if( Trim(document.forms[0].start_date_0.value) == '' ) {
         document.forms[0].start_date_0.value = 'ALL'
   }
   if( Trim(document.forms[0].end_date_0.value) == '' ) {
         document.forms[0].end_date_0.value = 'ALL'
   }

   document.forms[0].action.value=action;
   document.forms[0].submit();
}


</script>



<%--
**** MAIN
--%>

<wb:define id="userId"><wb:getPageProperty id="userId"/></wb:define>
<wb:define id="userName"><wb:getPageProperty id="userName"/></wb:define>
<wb:define id="dateFormat"><wb:getPageProperty id="defaultDateFormat"/></wb:define>
<wb:define id="systemDateFormat">yyyyMMdd HHmmss</wb:define>

<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default="1000010"/></wb:define>
<wb:define id="action"><wb:get id="action" scope="parameter" default=""/></wb:define>
<wb:submit id="action"/>
<wb:define id="WBT_ID_0"><wb:get id="WBT_ID_0" scope="parameter" default="ALL"/></wb:define>
<wb:define id="subTeamsValue"><wb:get id="SUB_TEAM_0" scope="parameter" default=""/></wb:define>
<wb:define id="emp_name_0"><wb:get id="emp_name_0" scope="parameter" default="ALL"/></wb:define>
<wb:define id="startDate"><wb:get id="start_date_0" scope="parameter" default="ALL"/></wb:define>
<wb:define id="endDate"><wb:get id="end_date_0" scope="parameter" default="ALL"/></wb:define>
<wb:define id="orderByValue"><wb:get id="order_by" scope="parameter" default=""/></wb:define>

<wb:define id="errorMsg"/>

<wb:define id="deleteParams"><wb:get id="delete_params" scope="parameter" default=""/></wb:define>
<wb:define id="saveParams"/>
<wb:define id="saveName"/>
<wb:define id="saveDesc"/>
<wb:define id="userReport"/>
<wb:define id="link,logoutDate"/>

<%-- Begin #6086
  -- sanand
  -- June 27, 2002
  -- Added var, for the OnClick event of the Print Button.
--%>

<%
/* TestTrack #6086, S.Anand - Only show Print button for Netscape Users.. */
WebBrowserVersion browser = JSPHelper.getWebContext(request).getBrowserVersion();
boolean isNetscape = browser.isOldNetscape();
%>

<%-- initialize fields value only if not delete --%>
<wb:if expression="#deleteParams!='T'#">
   <wb:set id="saveParams"><wb:get id="save_params" scope="parameter" default=""/></wb:set>
   <wb:set id="saveName"><wb:get id="save_name" scope="parameter" default=""/></wb:set>
   <wb:set id="saveDesc"><wb:get id="save_desc" scope="parameter" default=""/></wb:set>
   <wb:set id="userReport"><wb:get id="user_report" scope="parameter" default=""/></wb:set>
</wb:if>


<%--
**** Generate Page
--%>

<%-------------------------- Make Criteria -------------------------------- --%>

<wb:if expression="#startDate=='ALL'#">
   <wb:set id="startDate"><wb:formatDate outFormat="#systemDateFormat#"/></wb:set>
</wb:if>
<wb:if expression="#endDate=='ALL'#">
   <wb:set id="endDate"><wb:formatDate outFormat="#systemDateFormat#"/></wb:set>
</wb:if>
<%
if(subTeamsValue.toString().equalsIgnoreCase("Y")){%>
   <wb:set id="subTeamsValue">checked</wb:set>
<%}%>

<div class="separatorLarge"/>
<wba:table caption="Override Audit Report" captionLocalizeIndex="Override_Audit_Report">
<wba:tr>
	<wba:th><wb:localize type="field" overrideId="#mfrm_id#" id="REPT_OVERRIDEAUDIT_EMP">User Name</wb:localize></wba:th>
  <wba:th><wb:localize type="field" overrideId="#mfrm_id#" id="REPT_OVERRIDEAUDIT_TEAM">Team</wb:localize></wba:th>
	<wba:th><wb:localize type="field" overrideId="#mfrm_id#" id="REPT_OVERRIDEAUDIT_SUB_TEAM">Sub Teams</wb:localize></wba:th>
	<wba:th><wb:localize type="field" overrideId="#mfrm_id#" id="REPT_OVERRIDEAUDIT_START_DATE">Start Date</wb:localize></wba:th>
	<wba:th><wb:localize type="field" overrideId="#mfrm_id#" id="REPT_OVERRIDEAUDIT_END_DATE">End Date</wb:localize></wba:th>
	<%--<wba:th><wb:localize type="field" overrideId="#mfrm_id#" id="REPT_OVERRIDEAUDIT_STATUS">Status</wb:localize></wba:th>
	<wba:th><wb:localize type="field" overrideId="#mfrm_id#" id="REPT_OVERRIDEAUDIT_ORDER_BY">Order By</wb:localize></wba:th>
--%>
</wba:tr>

<wba:tr><wba:td>
<wb:controlField cssClass="inputField" submitName="emp_name_0" id="REPT_OVERRIDEAUDIT_EMP" overrideId="#mfrm_id#"><wb:get id="emp_name_0"/></wb:controlField>
</wba:td>
<%-- Team selector --%>
<wba:td>
  <wb:controlField cssClass="inputField" submitName="WBT_ID_0" id="REPT_OVERRIDEAUDIT_TEAM" overrideId="#mfrm_id#"><%=WBT_ID_0%></wb:controlField>
</wba:td><wba:td>
  <input type='checkbox' name='SUB_TEAM_0' <%=subTeamsValue%> value='Y'> </wba:td>
<wba:td>
  <wb:controlField cssClass="inputField" submitName="start_date_0" id="REPT_OVERRIDEAUDIT_START_DATE" overrideId="#mfrm_id#"><wb:get id="startDate"/></wb:controlField>
</wba:td><wba:td>
  <wb:controlField cssClass="inputField" submitName="end_date_0" id="REPT_OVERRIDEAUDIT_END_DATE" overrideId="#mfrm_id#"><wb:get id="endDate"/></wb:controlField>
</wba:td>
</wba:tr>

<wba:tr>
	<wba:tf colspan="10"><wb:localize id="MSG_OverrideAudit_Report">This report gives a list of all override edits for selected employees</wb:localize></wba:tf>
</wba:tr>

</wba:table>

<div class="separatorSmall" />
<%-------------------------- Save Params -------------------------------- --%>
<br>
<wba:table caption="Save Parameters" captionLocalizeIndex="Save_Parameters">
<wba:tr><wba:th colspan="10">
<wb:localize id="Save">Save</wb:localize>&nbsp;&nbsp;
<INPUT TYPE=CHECKBOX NAME='save_params' VALUE='T' onClick='ExclusiveUpdate(this.name,this.checked)'>
&nbsp;&nbsp;&nbsp;&nbsp;
<wb:localize id="Delete">Delete</wb:localize>&nbsp;&nbsp;
<INPUT TYPE=CHECKBOX NAME='delete_params' VALUE='T' onClick='ExclusiveUpdate(this.name,this.checked)'>
&nbsp;&nbsp;&nbsp;&nbsp;
<wb:localize id="Personal_Report">Personal Report</wb:localize>&nbsp;&nbsp;
<INPUT TYPE=CHECKBOX NAME='user_report' VALUE='T' onClick='ExclusiveUpdate(this.name,this.checked)'>
</wba:th></wba:tr>

<wba:tr>
<wba:td><wb:localize type="field" overrideId="377" id="REPORT_SAVED_NAME">Name</wb:localize></wba:td>
<wba:td>
<wb:controlField cssClass="inputField" submitName="save_name" id="REPORT_SAVED_NAME" overrideId="377"><wb:get id="saveName"/></wb:controlField>

</wba:td>
</wba:tr><wba:tr>
<wba:td><wb:localize type="field" overrideId="377" id="REPORT_SAVED_DESCRIPTION">Description</wb:localize></wba:td>
<wba:td>
<wb:controlField cssClass="inputField" submitName="save_desc" id="REPORT_SAVED_DESCRIPTION" overrideId="377"><wb:get id="saveDesc"/></wb:controlField>
</wba:td>
</wba:tr>
</wba:table>

<%-- End Save Params --%>

<div class="separatorSmall" />
<%--begin #2279#
  --mharrison
  --Oct. 17, 2001
  --fixed Go button so that localizing its text does not cause the form to submit
--%>
<wba:button name='GO' label="GO" labelLocalizeIndex="GO" onClick="if(validateFormFields('return true')) {this.disabled=true;onSubmit('run')} return false;"/>
<%--end #2279#--%>


<div class="separatorLarge"/>

<%-------------------------- Processing -------------------------------- --%>

<%-- Save / Delete params --%>

<wb:set id="saveName"><wb:get id="save_name" scope="parameter" default=""/></wb:set>
<wb:set id="saveDesc"><wb:get id="save_desc" scope="parameter" default=""/></wb:set>
<wb:set id="userReport"><wb:get id="user_report" scope="parameter" default=""/></wb:set>

<%
if( saveParams.toString().equalsIgnoreCase("T") || deleteParams.toString().equalsIgnoreCase("T") ) {

   String _saveName   = "";
   String _saveDesc   = "";
   String _userReport = "ALL_USERS";   // Default access control to saved params
   String sqlNameCheck = "";

   Enumeration enum = request.getParameterNames();
   StringBuffer reportParams = new StringBuffer();
   String unwantedParams = "!action!save_params!emp_name_0_button!start_date_0_button!end_date_0_button!pageaction!";

   // get report params submitted
   while( enum.hasMoreElements() ) {
      String aParam = (String)enum.nextElement();
      if( unwantedParams.indexOf("!" + aParam.toLowerCase() + "!") == -1  ) {
         reportParams.append( aParam.toLowerCase() + "=" + request.getParameter(aParam)+ "&" );
      }
   }

   if( saveParams.toString().equalsIgnoreCase("T") ) {
      // extra handling when save param
      if( saveName.toString().trim().length()==0 || saveDesc.toString().trim().length()==0 ) {
         %>
         <wb:set id="errorMsg"><wb:localize id="Report_Name_must_not_be_blank">Name and Description must not be blank to save report parameters.  Parameters not saved.</wb:localize></wb:set>
         <%
      } else {
	      _saveName = StringHelper.searchReplace(saveName.toString(), "'", "''");
	      _saveDesc = StringHelper.searchReplace(saveDesc.toString(), "'", "''");
   	   if( userReport.toString().equalsIgnoreCase("T") ) {
	         _userReport = userName.toString();
         }
         sqlNameCheck = "SELECT COUNT(*) X FROM REPORT_SAVED WHERE REPTSAV_NAME = '" + _saveName +"'";
         %>
         <wb:sql createDataSource="sqlNameCheck"><%=sqlNameCheck%></wb:sql>
         <wb:dataSet dataSource="sqlNameCheck" id="ds">
            <wb:if expression="#ds.X != '0'#">
               <wb:set id="errorMsg">"<wb:get id='saveName'/>"&nbsp;<wb:localize id="already_exists">already exists.  Parameters not saved.</wb:localize></wb:set>
            </wb:if>
         </wb:dataSet>
      <%
      }  // end handling for save
   } else if (deleteParams.toString().equalsIgnoreCase("T") ) {
      _saveName = StringHelper.searchReplace(saveName.toString(), "'", "''");
      _saveDesc = StringHelper.searchReplace(saveDesc.toString(), "'", "''");
   }
   %>

   <%-- if no error, perform save or delete action (in REPORT_SAVED) --%>
   <wb:switch>
   <wb:case expression="#errorMsg!=''#">
      <p><wb:get id="errorMsg"/><p>
   </wb:case>
   <wb:case>
     <wb:if expression="#deleteParams=='T'#">
       <% SavedReport.deleteReport( conn, _saveName ); %>
       <p><wb:localize id="Parameters_deleted">Parameters deleted.</wb:localize><p>
	   </wb:if>
     <wb:if expression="#deleteParams!='T'#">
        <% SavedReport.updateOrInsertReport( conn, _saveName,_saveDesc, mfrm_id.toString(), reportParams.toString(),_userReport ); %>
        <p><wb:localize id="Parameters_saved">Parameters saved.</wb:localize><p>
     </wb:if>
   </wb:case>
   </wb:switch>

   <%
}  // end save or delete
%>

<wb:if expression="#deleteParams=='T'#">
   <wb:set id="action"></wb:set>    <%-- no further action required after delete report --%>
</wb:if>
<%--end #1120#--%>

<wb:if expression="#action=='run'#">
   <!-- #33374 - check for blank user dblookup, treat it like ALL -->
   <wba:table caption="Override Audit Report" captionLocalizeIndex="OverrideAudits_Report" >
   <%-- TestTrack #6086, S.Anand - Only show Print button for Netscape Users, and formulate the OnClick event --%>
	<% if (isNetscape) {	%>
		<wba:tr>
			<wba:td align='center' colspan='5'>
				<wb:set id="link">document.location='reportOverrideAudit.jsp?WBT_ID_0=<wb:get id='WBT_ID_0'/>&SUB_TEAM_0=<wb:get id='subTeamsValue'/>&start_date_0=<wb:get id='startDate'/>&end_date_0=<wb:get id='endDate'/>&emp_name_0=<wb:get id='emp_name_0'/>&status=<wb:get id='status'/>&action=run';</wb:set>
	            <wb:set id="link"><%=StringHelper.searchReplace(link.toString()," ", "%20")%></wb:set>
				&nbsp;<wba:button name="Printable_Report" label="Printable Report" labelLocalizeIndex="OverrideAuditReport" onClick="#link#" />
			</wba:td>
		</wba:tr>
    <% } %>
   <wba:tr>
   <wba:th><wb:localize id="OverrideAudit_Emp">Employee</wb:localize></wba:th>
   <wba:th><wb:localize id="OverrideAudit_Edit_Date">Create Date</wb:localize></wba:th>
   <wba:th><wb:localize id="OverrideAudit_Change">Change</wb:localize></wba:th>
   <wba:th><wb:localize id="OverrideAudit_Work_Date">Work Date</wb:localize></wba:th>
   <wba:th><wb:localize id="OverrideAudit_Wbuname">By</wb:localize></wba:th>
   <wba:th><wb:localize id="OverrideAudit_Comments">Comments</wb:localize></wba:th>
   </wba:tr>

<%
   String empWhere = emp_name_0.toString();
   int[] empWhereA = null;
   if (!"ALL".equals(empWhere)) {
      empWhereA = StringHelper.detokenizeStringAsIntArray(empWhere , ",", true);
   }
   String teamWhere = WBT_ID_0.toString();
   int[] teamWhereA = null;
   if (!"ALL".equals(teamWhere)) {
      teamWhereA = StringHelper.detokenizeStringAsIntArray(teamWhere , ",", true);
   }

   StringBuffer sb = new StringBuffer(200);
   sb.append("      SELECT  DISTINCT VL_EMPLOYEE.EMP_NAME, VL_EMPLOYEE.EMP_LOC_FULLNAME NAME,");
   sb.append("      OVERRIDE.OVR_NEW_VALUE,OVERRIDE.OVR_CREATE_DATE,");
   sb.append("      OVERRIDE.OVR_START_DATE, OVERRIDE.WBU_NAME_ACTUAL,  OVERRIDE.OVR_COMMENT,");
   sb.append("      OVERRIDE.OVRTYP_ID");
   sb.append("      FROM SEC_EMPLOYEE, VL_EMPLOYEE, OVERRIDE");
   sb.append("         WHERE    VL_EMPLOYEE.EMP_ID = SEC_EMPLOYEE.EMP_ID");
   if (empWhereA != null && empWhereA.length > 0) {
      sb.append("         AND   OVERRIDE.EMP_ID IN ( ");
      for (int i = 0; i < empWhereA.length; i++) {
       sb.append(i > 0 ? ",?" : "?");
      }
      sb.append("           ) ");
   }
   sb.append("      AND   SEC_EMPLOYEE.EMP_ID = OVERRIDE.EMP_ID ");
   sb.append("      AND   SEC_EMPLOYEE.WBU_ID = ? ");
   sb.append("      AND   OVERRIDE.OVR_START_DATE BETWEEN ? AND ? ");
   sb.append("      AND   OVERRIDE.OVR_STATUS = ? ");
   if (teamWhereA != null && teamWhereA.length > 0) {
        // Sub Teams
     if(subTeamsValue.toString().equalsIgnoreCase("checked")){
        sb.append("     AND SEC_EMPLOYEE.wbt_id in ");
        sb.append("        (select child_wbt_id from SEC_WB_TEAM_CHILD_PARENT where parent_wbt_id in (");
        for (int i = 0; i < teamWhereA.length; i++) {
          sb.append(i > 0 ? ",?" : "?");
        }
        sb.append("                )) ");
     }else{
         // Only Selected Team
        sb.append("      AND SEC_EMPLOYEE.WBT_ID IN (");
        for (int i = 0; i < teamWhereA.length; i++) {
          sb.append(i > 0 ? ",?" : "?");
        }
        sb.append("      )");
     }
   }
   sb.append(" ORDER BY name, ovrtyp_id, ovr_create_date");
   LocalizationDictionary ld =  LocalizationDictionary.get();
   PreparedStatement ps = null;
   ResultSet rs = null;
   boolean recsFound = false;
   try {
       ps = conn.prepareStatement(sb.toString());
       int cnt = 1;
       if (empWhereA != null && empWhereA.length > 0) {
         for (int i = 0; i < empWhereA.length; i++) {
            ps.setInt(cnt++ , empWhereA[i]);
         }
       }
       ps.setInt(cnt++ , Integer.parseInt(userId.toString()));
       ps.setTimestamp(cnt++, new java.sql.Timestamp (DateHelper.parseDate(startDate.toString().substring(0,8)+" 000000",systemDateFormat.toString()).getTime() ));
       ps.setTimestamp(cnt++, new java.sql.Timestamp (DateHelper.parseDate(endDate.toString().substring(0,8)+" 000000",systemDateFormat.toString()).getTime() ));
       ps.setString(cnt++ , OverrideData.APPLIED );
       if (teamWhereA != null && teamWhereA.length > 0) {
         for (int i = 0; i < teamWhereA.length; i++) {
            ps.setInt(cnt++ , teamWhereA[i]);
         }
       }
       rs = ps.executeQuery();
       String ovrNewValue = null;
       while (rs.next()) {
           recsFound = true;
           out.println("<tr>");
           out.println("<td>" + rs.getString("name") +  "</td>");
           out.println("<td>" + DateHelper.convertDateString(rs.getTimestamp("ovr_create_date"), "MM/dd/yyy hh:mm") +  "</td>");
           out.println("<td>" + massageOvrNewValue(rs.getString("ovr_new_value") , ld, request)  +  "</td>");
           out.println("<td>" + DateHelper.convertDateString(rs.getTimestamp("ovr_start_date"), "MM/dd/yyyy,") +  "</td>");
           out.println("<td>" + rs.getString("wbu_name_actual") +  "</td>");
           String ovrCmt = rs.getString("ovr_comment") ;
           out.println("<td>" + (StringHelper.isEmpty(ovrCmt) ? "N/A" :  ovrCmt)+  "</td>");
           out.println("</tr>");
       }
   }
   finally {
       if (rs != null) rs.close();
       if (ps != null) ps.close();
   }

   if (!recsFound) {
%>
         <wba:tr><wba:td align='center' colspan='5'><span class="datarowContent" style="color:red">
         <wb:localize id="No_records_found">No records match your query, please re-try.</wb:localize>
         </span></wba:td></wba:tr>
<%
   }
%>
   </wba:table>

</wb:if>    <%-- end processing block --%>

<wb:submit id="mfrm_id"><wb:get id="mfrm_id"/></wb:submit>

</wb:page>