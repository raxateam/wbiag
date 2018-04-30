<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.util.*"%> 
<%@ page import="java.sql.*"%> 
<%@ page import="java.text.SimpleDateFormat"%> 
<%@ page import="com.workbrain.util.*" %> 
<%@ page import="com.workbrain.sql.*"%> 
<%@ page import="com.workbrain.server.jsp.*"%> 
<%@ page import="com.wbiag.app.modules.retailSchedule.mce.*"%> 

<%!
	private static final String SEARCH_STRING = "-DEPT";

%>

<wb:page login="true" popupPage="false" subsidiaryPage="false" title="Staffing Requirement" submitMethod="post"> 
   <wb:define id="dept_search"><wb:get id="dept_search" scope="parameter" default='true'/></wb:define>
   <wb:submit id="dept_search"><wb:get id="dept_search"/></wb:submit>
   <wb:define id="selected_skdgrp_list"><wb:get id="selected_skdgrp_list" scope="parameter" default=''/></wb:define>
   <wb:submit id="selected_skdgrp_list"><wb:get id="selected_skdgrp_list"/></wb:submit>
   <wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" /></wb:define>
   <wb:submit id="mfrm_id"><wb:get id="mfrm_id"/></wb:submit>
   <wb:define id="searched_skdgrp_list"><wb:get id="searched_skdgrp_list" scope="parameter" default=''/></wb:define>
   <wb:submit id="searched_skdgrp_list"><wb:get id="searched_skdgrp_list"/></wb:submit>
   <wb:define id="MCESS_TYPE"><wb:get id="MCESS_TYPE" scope="parameter" default=''/></wb:define>
   <wb:submit id="MCESS_TYPE"><wb:get id="MCESS_TYPE"/></wb:submit>
   <wb:define id="csdDesc, startDate, endDate, lnkShowSkdGroupSQL"/>
   <wb:define id="client_type_id"><wb:get id="client_type_id" scope="parameter" default=''/></wb:define>
   <wb:submit id="client_type_id"><wb:get id="client_type_id"/></wb:submit>
   
<%
   DBConnection conn = JSPHelper.getConnection(request);
   String actionType = request.getParameter("actionType");
   String locList = request.getParameter("selected_skdgrp_list");

   //expecting either basicStoreConfiguration or basicDepartmentConfiguration
   String deptSearch = request.getParameter("dept_search");
   
   //deleting
   if ( "DELETE".equals(actionType) ) {
      String staffReqToBeDeletedList = request.getParameter("staffReqNameIn");
	  //out.print(staffReqToBeDeletedList);
      try {
	     MCEConfiguration mceConfig = new MCEConfiguration(request);
         mceConfig.deleteStaffRequirement( conn, staffReqToBeDeletedList, locList );
      }catch(Exception e) {
%>
   <span>The action was not success.  The following error occured:</span>
   <P><%=e%></P>
<%
   
   }
 }

%>
	<wb:sql createDataSource="requirementSQL">
    select * from (
    select * from(
    select substr(nvl(csd_desc, 'xxxxxx'),instr(nvl(csd_desc, 'xxxxxx'), '-DEPT')+5) csd_desc, 
	to_char(csd_eff_start_date, 'MM/dd/yyyy') start_date, 
	to_char(csd_eff_end_date, 'MM/dd/yyyy') end_date
    from so_client_stfdef
    where skdgrp_id in ( <%=locList%> )
    and csd_desc like '%-DEPT%'
    )
    group by  csd_desc, start_date, end_date)
    order by csd_desc
	</wb:sql>

	<wba:table class="contentTable form" caption="Staffing Requirement" captionLocalizeIndex="Caption_Staffing_Requirement"> 
	<tr> 
		<th></th> 
		<th><wb:localize id='Requirement Name'>Requirement Name (Task Name)</wb:localize></th> 
		<th></th> 
		<th><wb:localize id='Effective Date Range'>Effective Date Ranges</wb:localize></th> 
	</tr>

	<% 
	   int reqCount = 0;
	   int recCount = 0;
	   String lastReqName = "";
	   String reqNameStr = "";
	   String csdDescStr = "";
	   String dateRangeStr = "";
	   String lnkShowSkdGroup = "";
	   String chkReq = "";
	   String lblReq = "";
	   String lblEffDateRng = "";
	
	%>
	      <wb:dataSet id="requirementDataSet" dataSource="requirementSQL">
	      <wb:forEachRow dataSet="requirementDataSet">
             <wb:set id="csdDesc"><wb:getDataFieldValue name="csd_desc"/></wb:set>
			 <wb:set id="startDate"><wb:getDataFieldValue name="start_date"/></wb:set>
			 <wb:set id="endDate"><wb:getDataFieldValue name="end_date"/></wb:set>
			 
	<%
			    //csdDescStr = csdDesc.toString();
				int index = csdDescStr.indexOf(SEARCH_STRING); 
                //if (index > 0) {
				   recCount ++;
				   //reqNameStr = csdDescStr.substring(index + 5);
				   reqNameStr = csdDesc.toString();
				   
			       if (!lastReqName.equals(reqNameStr) && !"".equals(lastReqName)) {
					  reqCount++;
				      lnkShowSkdGroup = "lnkShowSkdGroup" + reqCount;
		              chkReq = "chkReq" + reqCount;
		              lblReq = "lblReq" + reqCount;
		              lblEffDateRng = "lblEffDateRng" + reqCount;
%>
				      <wb:set id="lnkShowSkdGroupSQL">
				         select distinct sg.skdgrp_id, sg.skdgrp_name
                         from so_schedule_group sg, so_client_stfdef cs
                         where sg.skdgrp_id = cs.skdgrp_id
					     and sg.skdgrp_id in ( <%=locList%> )
                         and cs.csd_desc like \'%<%=SEARCH_STRING + lastReqName %>\'
					  </wb:set>

                      <tr> 
		                 <td>
					        <input type='checkbox' name="<%=chkReq%>" value="<%=lastReqName%>"/>
					     </td> 
		                 <td><%=lastReqName%></td>
					     <td> 
					        <wb:controlField id="<%=lnkShowSkdGroup%>" 
					         submitName="<%=lnkShowSkdGroup%>" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowSkdGroupSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
					     </td> 
					     <td>
						    <%=dateRangeStr%>
						 </td>
	                  </tr>
	<%
				    dateRangeStr = "";
					recCount = 0;
				   }
                   if (!"".equals(dateRangeStr)) {
				      dateRangeStr = dateRangeStr + "; "; 
					  if (recCount%2 == 1) {
				         dateRangeStr = dateRangeStr + "<br>"; 
				      }
				   }
				   dateRangeStr = dateRangeStr + startDate.toString() + " - " + endDate.toString();
			       lastReqName = reqNameStr; 
			  //}
    %>
	        
	      </wb:forEachRow>
   <%        
             //last record
		     if (!"".equals(lastReqName)) {
				reqCount++;
			    lnkShowSkdGroup = "lnkShowSkdGroup" + reqCount;
		        chkReq = "chkReq" + reqCount;
		        lblReq = "lblReq" + reqCount;
		        lblEffDateRng = "lblEffDateRng" + reqCount;
   %>
                <wb:set id="lnkShowSkdGroupSQL">
				   select distinct sg.skdgrp_id, sg.skdgrp_name
                   from so_schedule_group sg, so_client_stfdef cs
                   where sg.skdgrp_id = cs.skdgrp_id
				   and sg.skdgrp_id in ( <%=locList%> )
                   and cs.csd_desc like \'%<%=SEARCH_STRING + lastReqName %>\'
			    </wb:set>
                <tr> 
		           <td>
				      <input type='checkbox' name="<%=chkReq%>" value="<%=lastReqName%>"/>
				   </td> 
		           <td><%=lastReqName%></td>
				   <td> 
				      <wb:controlField id="<%=lnkShowSkdGroup%>" 
					     submitName="<%=lnkShowSkdGroup%>" 
						 ui='DBLookupUI' 
						 uiParameter="source='#lnkShowSkdGroupSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
					</td> 
					<td>
					   <%=dateRangeStr%>
				    </td>
	             </tr>
	<%
             }
    %>
	   </wb:dataSet> 
	   <input type="hidden" name="reqCount" value=<%=reqCount%>>
    </wba:table>
	<div class="separatorSmall"/>
    <wba:button name="btnAdd" label="Add" labelLocalizeIndex="Add" onClick="add(this);"></wba:button><span>&nbsp;</span>
    <wba:button name="btnEdit" label="Edit" labelLocalizeIndex="Edit" onClick="edit(this);"></wba:button><span>&nbsp;</span>
    <wba:button name="btnDelete" label="Delete" labelLocalizeIndex="Delete" onClick="deleteSpecialDay(this);"></wba:button> </br>
    <div class="separatorSmall"/>
    <wba:button name="Done" label="Done" labelLocalizeIndex="Done" onClick="history.back()"></wba:button>

	<INPUT TYPE="hidden" name="staffReqNameIn">
	<INPUT TYPE="hidden" name="actionType"> 
	
<script type='text/javascript'>

function add(thisForm) {
   document.page_form.actionType.value  = "ADD";
   document.forms[0].action="./staffReqType.jsp";
   document.forms[0].submit();
}

function edit(thisForm) {
   if (document.page_form.reqCount.value == 0) {
      return false;
   }
   document.page_form.actionType.value = 'EDIT';
   var checkboxCount = document.page_form.reqCount.value;
   var selectedStaffReqName = "";
   var checkCount = 0;

   for ( var i = 1; i <= checkboxCount ; i ++) {
      if (eval(document.page_form.elements["chkReq" + i].checked) ) {
	     checkCount++;
         selectedStaffReqName = document.page_form.elements["chkReq" + i].value;	
	  }
   }

   if (checkCount == 0) {
      alert ( 'Please select one staff requirement for editing.' );
	  return false;
   }
   if (checkCount > 1) {
      alert ( 'Please select only one staff requirement for editing.' );
	  return false;
   }

   document.page_form.staffReqNameIn.value  = selectedStaffReqName;
   document.forms[0].action="./staffReqEdit.jsp";
   document.forms[0].submit();

   return true;	
}

function deleteSpecialDay(thisForm) {
   if (document.page_form.reqCount.value == 0) {
	   //alert('goes here');
      return false;
   }
   var checkboxCount = document.page_form.reqCount.value;
   //alert(checkboxCount);
   var selectedStaffReqName = '';
   var checkCount = 0;
   for ( var i = 1; i <= checkboxCount ; i ++) {
      if (eval(document.page_form.elements["chkReq" + i].checked) ) {
	     if (selectedStaffReqName != '') {
		    selectedStaffReqName += ',';
		 }
	     checkCount++;
         selectedStaffReqName += document.page_form.elements["chkReq" + i].value;	
	  }
   }
   if (checkCount == 0) {
      alert ( 'At least one staff requirement must be in scope for deleting.' );
	  return false;
   }
   document.page_form.staffReqNameIn.value  = selectedStaffReqName;
   document.page_form.actionType.value = 'DELETE';
   document.forms[0].submit();

   return true;	
}
</script>

</wb:page>