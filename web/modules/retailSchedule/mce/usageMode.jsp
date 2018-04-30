<%@ include file="/system/wbheader.jsp"%>
<%@ page import="com.workbrain.util.*,com.workbrain.server.jsp.taglib.util.*"%>
<%@ page import="com.wbiag.app.modules.retailSchedule.*"%>
<%@ page import="com.wbiag.app.modules.retailSchedule.mce.MCEProcess"%>
<%@ page import="com.workbrain.app.modules.retailSchedule.db.*"%>
<%@ page import="com.workbrain.app.modules.retailSchedule.model.*"%>
<%@ page import="com.workbrain.app.ta.db.CodeMapper"%>
<%@ page import="com.workbrain.sql.*"%>

<wb:page login='true'>
   <wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" /></wb:define>
   <wb:submit id="mfrm_id"><wb:get id="mfrm_id"/></wb:submit>
   <wb:define id="dept_search"><wb:get id="dept_search" scope="parameter" default='true'/></wb:define>
   <wb:define id="selected_skdgrp_list"><wb:get id="selected_skdgrp_list" scope="parameter" default=''/></wb:define>
   <wb:submit id="selected_skdgrp_list"><wb:get id="selected_skdgrp_list"/></wb:submit>
   <wb:define id="MCESS_TYPE"><wb:get id="MCESS_TYPE" scope="parameter" default=''/></wb:define>
   <wb:submit id="MCESS_TYPE"><wb:get id="MCESS_TYPE"/></wb:submit>
   <wb:define id="searched_skdgrp_list"><wb:get id="searched_skdgrp_list" scope="parameter" default=''/></wb:define>
   <wb:config id="NO_DATA_FOUND_FOR_YOUR_SEARCH"/>
   <wb:define id="client_type_id"><wb:get id="client_type_id" scope="parameter" default=''/></wb:define>
   

<%
    DBConnection conn = JSPHelper.getConnection(request);
	MCEProcess mcep = new MCEProcess(conn, request);
    boolean isDeptSearch = true;
	String schedGrpIdList = "";
	String clientTypId = "";

	if (searched_skdgrp_list.toString().length()!=0) {
		schedGrpIdList = searched_skdgrp_list.toString();
		isDeptSearch = "true".equals(dept_search.toString()) ? true : false;
		if (isDeptSearch) {
           clientTypId = client_type_id.toString();
        }
	} else {
		schedGrpIdList = mcep.processSearch();
		isDeptSearch = mcep.isDeptSearch();
		if (isDeptSearch) {
           clientTypId = mcep.getClnttypId().toString();
        }
	}

   String mfrmIdStr = request.getParameter("mfrm_id");
   String goBack =  "location.href = '" + request.getContextPath() + "/maintenance/mntForms.jsp?mfrm_id=" + mfrmIdStr + "'; return false;";
  

   if (StringHelper.isEmpty(schedGrpIdList)) {
%>
      <span><wb:localize ignoreConfig="true" id="NO_RESULT_FOUND_FOR_YOUR_SEARCH">No result found for your search.</wb:localize></span>
	  <div class="separatorSmall"/>
	  <wba:button label="Go back to Search List" labelLocalizeIndex="go_back_list" onClick="<%=goBack%>"/><span>&nbsp;</span>
	  <wba:button label="Go back to Search Form" labelLocalizeIndex="go_back_form" onClick="history.back()"/>
<%
   }
   else {
	  CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
	  int rowCount = 0;
	  String deptName = "";
      
	  //getting dept name to display if dept search type
	  if (isDeptSearch) {
	     mcep = new MCEProcess(conn);
		 deptName = mcep.getClnttypName(new Integer(clientTypId));
	  }

	  String userModeSql = "SELECT WIMCEUM_PATH, WIMCEUM_NAME FROM WBIAG_MCE_USAGE_MODE ";
	  String searchType = isDeptSearch ? "DEPT" : "STORE";
	  String whereClause =  "WIMCEUM_TYPE='" + searchType + "'";
	  String userModeUiParams = "sourceType=SQL source=\"" + userModeSql + "\" where=\"" + whereClause + "\"";
%>

      <wba:table caption="Location Scope and Usage Mode" captionLocalizeIndex="Location_Scope_and_Usage_Mode">
	     <tr>
		    <th>
			<input type="checkbox" name="checkall" onclick="checkUncheckAll(this);"/>
			</th>
		    <th>
			   <wb:localize id="Store_Loc">Store Location</wb:localize>
			</th>
			<th>
			   <wb:localize id="Parent_Loc">Parent Location</wb:localize>
			</th>
<%
			if (isDeptSearch) {
%>
               <th>
			      <wb:localize id="Department">Department</wb:localize>
			   </th> 
<%
			}
%>
		 </tr>
<%
		 try {
		    int[] schedGrpIdArray = StringHelper.detokenizeStringAsIntArray(schedGrpIdList, ",", true);
			String rowColour = "";
			for( int i = 0 ; i < schedGrpIdArray.length; i++ ){
			   ScheduleGroupData skdgrpData = codeMapper.getScheduleGroupById(schedGrpIdArray[i]);
			   String pntSkdGrpName = "";
			   Integer parentId = skdgrpData.getSkdgrpParentId(); 
			   if ( parentId != null) {
			      ScheduleGroupData pntSkdgrpData = codeMapper.getScheduleGroupById(parentId.intValue());
			      pntSkdGrpName = pntSkdgrpData.getSkdgrpName();
			   }
			   rowColour = ((rowCount % 2) == 0) ? "evenRow" : "oddRow";
			   out.print("<tr class=\"" + rowColour + "\">");
			   out.print("<td><input type='checkbox' name=\"skdgrpid_" + rowCount + "\" value=\"" + schedGrpIdArray[i] + "\"/></td>");
			   out.print("<td>" + skdgrpData.getSkdgrpName() + "</td>");
			   out.print("<td>" + pntSkdGrpName + "</td>" );

			   if (isDeptSearch) {
                  out.print("<td>" + deptName + "</td>" );
			   }

			   out.print("</tr>");
			   rowCount++;
			}
%>
			<tr>
               <td colspan='<%=isDeptSearch ? "4" : "3" %>'>
			      <table>
				     <tr>
					    <td>
						   <wb:localize id="User Mode">User Mode</wb:localize>
				           <span>:</span>
						</td>
						<td>
				           <wb:controlField ui='DBComboboxUI' submitName="user_mode_jsp" uiParameter='<%=userModeUiParams%>'></wb:controlField>
	                    </td>
					 </tr>
				  </table>
			   </td>
			</tr>
<%
	     }catch(Exception e) {
%>
	     <span>Search was not processed.  The following error occured:</span>
         <P><%=e%></P>
<%		 
		 }
%>   
	  </wba:table>


	  <wb:set id="dept_search"><%=isDeptSearch ? "true" : "false"%></wb:set>
	  <wb:submit id="dept_search"><wb:get id="dept_search"/></wb:submit>
	  <wb:set id="client_type_id"><%=clientTypId%></wb:set>
	  <wb:submit id="client_type_id"><wb:get id="client_type_id"/></wb:submit>

	  <input name="rowCount" type="hidden" value=<%=rowCount%>>
	  <div class="separatorSmall"/>
      <wba:button label='Next' onClick='submitForm();'/><span>&nbsp;</span>
	  <wba:button label="Cancel" onClick="<%=goBack%>"/>  
<%
   }
%> 

   <script>
   function submitForm() {
     if( validate()) {
		document.forms[0].action = '<%=request.getContextPath()%>' + document.page_form.user_mode_jsp.value;
        disableAllButtons();
        document.page_form.submit();
     }
   }

   function validate(){
      if (document.page_form.elements["user_mode_jsp"].value == '') {
	     alert ( 'Please select a User Mode.' );
	     return false;
	  }
      var checkboxCount = document.page_form.rowCount.value;
      var skdgrpList = '';
	  for ( var i = 0; i < checkboxCount ; i ++) {
		if (eval(document.page_form.elements["skdgrpid_" + i].checked) ) {
		   if (skdgrpList != '') {
		      skdgrpList += ',';
		   }
           skdgrpList += document.page_form.elements["skdgrpid_" + i].value;	
		}
	  }
	  if ( skdgrpList != '' ) {
		 document.page_form.selected_skdgrp_list.value  = skdgrpList;
		 return true;
		}
	  
	  alert ( 'At least one location must be in scope for editing.' );
	  return false;
   }

   function checkUncheckAll(theElement) {
       var theForm = theElement.form;
	   var z = 0;
	   for( z=0;z<theForm.length;z++ ){
          if(theForm[z].type == 'checkbox' && theForm[z].name.indexOf("skdgrpid_") == 0){
	         theForm[z].checked = theElement.checked;
	      }
       }
    }
   </script>
     
	   <wb:set id="searched_skdgrp_list"><%=schedGrpIdList%></wb:set>
	   <wb:submit id="searched_skdgrp_list"><wb:get id="searched_skdgrp_list"/></wb:submit>

</wb:page>