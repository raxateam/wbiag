<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.util.*"%> 
<%@ page import="java.sql.*"%> 
<%@ page import="com.workbrain.util.*" %> 
<%@ page import="com.workbrain.sql.*"%> 
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.wbiag.app.modules.retailSchedule.mce.*"%> 

<%!
	private static final String SEARCH_STRING = "-DEPT";

%>

<%
   String sLocList = null;
   String sDeptSearch = null;
   String sStaffReqNameIn = null;
   String sReqType = null;
   String driverNameList = null;
   String driverIdList = null;
   String comboBoxString = null;
%>
<style type="text/css"> 
@import url("<%=request.getContextPath()%>/modules/retailSchedule/css/wbextended.css"); 

table.contentTable.form th{
	width: 250px;
	height: 30px;
}

table.contentTable.form td.subSection{
	font-weight: normal;
	width: 250px;
	height: 20px;
	border: 1px solid #FFFFFF !important;
	background-color: #EEEEEE;
	padding: .1em .5em .1em .5em !important;
	vertical-align: top;
}

div.labeled_wrap {
	float: left;
	margin: 0px 1em 0px 0px;
	white-space: normal;
}

</style>
<wb:page login='true'>
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
   <wb:define id="staffReqNameIn"><wb:get id="staffReqNameIn" scope="parameter" default=''/></wb:define>
   <wb:submit id="staffReqNameIn"><wb:get id="staffReqNameIn"/></wb:submit>
   <wb:define id="REQ_TYPE"><wb:get id="REQ_TYPE" scope="parameter" default=''/></wb:define>
   <wb:submit id="REQ_TYPE"><wb:get id="REQ_TYPE"/></wb:submit>
   <wb:define id="client_type_id"><wb:get id="client_type_id" scope="parameter" default=''/></wb:define>
   <wb:submit id="client_type_id"><wb:get id="client_type_id"/></wb:submit>

   

   <wb:define id="lnkShowStafReqInstSQL,lnkShowActivitySQL,lnkShowJobSQL,lnkShowSkillSQL,lnkShowSkillLevelSQL,lnkShowMinRequiredSQL,lnkShowMaxRequiredSQL,lnkShowShiftStartOffsetSQL,lnkShowShiftEndOffsetSQL,lnkShowDriverSQL"/>
   <wb:define id="driverNames,driverIds"/>
<%   
   sLocList = selected_skdgrp_list.toString();
   sDeptSearch = dept_search.toString();
   sStaffReqNameIn = staffReqNameIn.toString();
   sReqType = REQ_TYPE.toString(); 
   int staffReqCount = 1;
   String toProcessRequest = request.getParameter("process");
   String actionType = request.getParameter("actionType");
   String mfrmIdStr = mfrm_id.toString();
   String goBack =  "location.href = '" + request.getContextPath() + "/maintenance/mntForms.jsp?mfrm_id=" + mfrmIdStr + "'; return false;";

if ( "EDITACTION".equals(actionType) ) {

	DBConnection conn = JSPHelper.getConnection(request);
	
	try{
		MCEStaffRequirements mceConfig = new MCEStaffRequirements(request);
		mceConfig.UpdateStaffingRequirements(conn, request);
		
		actionType = "EDIT";

		%>
		<script>
		document.forms[0].action = '<%=request.getContextPath()%>' + "/modules/retailSchedule/mce/usageMode.jsp";
		document.forms[0].submit();
		</script>
		<%
	}
	catch(Exception e){
		out.print(e.toString());
	}

}
else if ( "ADDACTION".equals(actionType) ) {

	DBConnection conn = JSPHelper.getConnection(request);
	
	try{
		MCEStaffRequirements mceConfig = new MCEStaffRequirements(request);
		mceConfig.AddStaffingRequirements(conn, request);
		
		actionType = "ADD";
	}
	catch(Exception e){
		out.print(e.toString());
	}
}
%>
	
	<INPUT TYPE="hidden" name="actionType">
     
	 <wb:set id="lnkShowStafReqInstSQL">
        select csd_id, substr(csd_desc,instr(csd_desc, \'-DEPT\')+5) reqirement_name
        from so_client_stfdef
        where csd_desc like \'%-DEPT%\'
        and skdgrp_id in ( <%=sLocList%> )
     </wb:set>

	 <wb:set id="lnkShowActivitySQL">
	    select distinct a.act_id, a.act_name 
        from so_activity a, so_client_stfdef cs 
        where a.act_id = cs.act_id
        and cs.skdgrp_id in (<%=sLocList%>)
     </wb:set>

	 <wb:set id="lnkShowJobSQL">
	    select distinct j.job_id, j.job_name 
        from job j, so_client_stfdef cs 
        where j.job_id = cs.job_id
        and cs.skdgrp_id in (<%=sLocList%>)
     </wb:set>

	 <wb:set id="lnkShowSkillSQL">
	    select distinct s.stskl_id, s.stskl_name 
        from st_skill s, so_client_stfdef cs 
        where s.stskl_id = cs.stskl_id
        and cs.skdgrp_id in (<%=sLocList%>)
     </wb:set>

	 <wb:set id="lnkShowSkillLevelSQL">
	    select distinct skdgrp_id, skill_level 
        from so_client_stfdef 
        where skdgrp_id in (<%=sLocList%>)
     </wb:set>

	 <wb:set id="lnkShowMinRequiredSQL">
	    select distinct skdgrp_id, csd_min_req 
        from so_client_stfdef 
        where skdgrp_id in (<%=sLocList%>)
     </wb:set>

	 <wb:set id="lnkShowMaxRequiredSQL">
	    select distinct skdgrp_id, csd_max_req 
        from so_client_stfdef 
        where skdgrp_id in (<%=sLocList%>)
     </wb:set>

	 <wb:set id="lnkShowShiftStartOffsetSQL">
	    select distinct skdgrp_id, csd_shftstr_ofs 
        from so_client_stfdef 
        where skdgrp_id in (<%=sLocList%>)
     </wb:set>

	 <wb:set id="lnkShowShiftEndOffsetSQL">
	    select distinct skdgrp_id, csd_shftend_ofs 
        from so_client_stfdef 
        where skdgrp_id in (<%=sLocList%>)
     </wb:set>

	 <wb:set id="lnkShowDriverSQL">
	    select distinct sg.skdgrp_id, sg.skdgrp_name Driver_Name
        from so_schedule_group sg, so_client_stfdef cs
        where sg.skdgrp_id = cs.skdgrp_id
        and sg.skdgrp_intrnl_type = 12
        and sg.skdgrp_parent_id in (<%=sLocList%>)
     </wb:set>

	
     <wb:sql createDataSource="driverNameSQL">
        SELECT SKDGRP_ID,SKDGRP_NAME FROM SO_SCHEDULE_GROUP WHERE SKDGRP_INTRNL_TYPE = 12
     </wb:sql>
<%
	String distributionSql = "SELECT DIST_ID, DIST_NAME  FROM SO_DISTRIBUTION ";
	String distributionUiParams = "sourceType=SQL source=\"" + distributionSql + "\"";

	String driverNameSQL = "select skdgrp_id, skdgrp_name " +
                           "from so_schedule_group " +
                           "where skdgrp_intrnl_type = 12 " +
                           "and skdgrp_parent_id in (" + sLocList + ") ";
	String driverNameUiParams = "sourceType=SQL source=\"" + driverNameSQL + "\"";
%>
	<wb:dataSet id="driverNameDataSet" dataSource="driverNameSQL">
	   <wb:forEachRow dataSet="driverNameDataSet">
          <wb:set id="driverNames"><wb:getDataFieldValue name="SKDGRP_NAME"/></wb:set>
          <wb:set id="driverIds"><wb:getDataFieldValue name="SKDGRP_ID"/></wb:set>
            <%
            if (driverNameList != null){
            	driverNameList += ",";
            }
            else
               driverNameList = driverNames.toString();
            driverNameList += driverNames;
            if (driverIdList != null){
               driverIdList += ",";
            }
            else
              	driverIdList = driverIds.toString();
            driverIdList += driverIds;
                       
            comboBoxString = "labelList=\'" + driverNameList + "\' valueList=\'" + driverIdList + "\'";
            %>
          </wb:forEachRow>
        </wb:dataSet>
		
<%

// check if edit mode or add mode 
if (StringHelper.equalsIgnoreCase("EDIT",actionType)){%>

	<wb:sql createDataSource="staffReqSQLS">
	SELECT * FROM SO_CLIENT_STFDEF 
	WHERE CSD_DESC LIKE '%<%=SEARCH_STRING + sStaffReqNameIn%>'
	ORDER BY CSD_DESC
	</wb:sql>
	
	<wba:table width="100%" caption="Add/Edit Staffing Requirement" captionLocalizeIndex="Caption_Add_Edit_Staffing_Requirement">
	   <wb:define id="skdgrpId,csdId,csdDesc,taskName,startDate,endDate,activity,job,skill,skillLevel,minReq,maxReq,shiftStartOff,shiftEndOff,description,driver,productivity,distribution,skdgrpIdnew,wrkldId"/>
	    <% int counter = 1;%>
	    <wb:dataSet id="staffReqDataSet" dataSource="staffReqSQLS">
	      <wb:forEachRow dataSet="staffReqDataSet">
	        <% String sCheckBoxName = "chkbx" + counter;%>
   	    <tr>
	       <wba:table > 
	      <%		
	         String sStaffReqInst = "StaffReqInst" + counter;
	         String sStaffReqAtts = "StaffReqAtts" + counter;
	         String sDrivers = "Drivers" + counter;
		     String sTaskName = MCEStaffRequirements.TASK_NAME + counter;
		     String sStartDate = MCEStaffRequirements.START_DATE + counter;
		     String sEndDate = MCEStaffRequirements.END_DATE + counter;
		     String sActivity = MCEStaffRequirements.ACTIVITY + counter;
		     String sJob = MCEStaffRequirements.JOB + counter;
		     String sSkill = MCEStaffRequirements.SKILL + counter;
		     String sSkillLevel = MCEStaffRequirements.SKILL_LEVEL + counter;
		     String sMinReq = MCEStaffRequirements.MIN_REQ + counter;
		     String sMaxReq = MCEStaffRequirements.MAX_REQ + counter;
		     String sShiftStartOff = MCEStaffRequirements.SHIFT_START_OFF + counter;
		     String sShiftEndOff = MCEStaffRequirements.SHIFT_END_OFF + counter;			 
	      %>
	      
	        <wb:set id="skdgrpId"><wb:getDataFieldValue name="SKDGRP_ID"/></wb:set>
	        <wb:set id="csdId"><wb:getDataFieldValue name="CSD_ID"/></wb:set>
			<wb:set id="taskName"><%=sStaffReqNameIn%></wb:set>
			<wb:set id="csdDesc"><wb:getDataFieldValue name="CSD_DESC"/></wb:set>
	        <wb:set id="startDate"><wb:getDataFieldValue name="CSD_EFF_START_DATE"/></wb:set>
	        <wb:set id="endDate"><wb:getDataFieldValue name="CSD_EFF_END_DATE"/></wb:set>
	        <wb:set id="activity"><wb:getDataFieldValue name="ACT_ID"/></wb:set>
	        <wb:set id="job"><wb:getDataFieldValue name="JOB_ID"/></wb:set>
	        <wb:set id="skill"><wb:getDataFieldValue name="STSKL_ID"/></wb:set>
	        <wb:set id="skillLevel"><wb:getDataFieldValue name="SKILL_LEVEL"/></wb:set>
	        <wb:set id="minReq"><wb:getDataFieldValue name="CSD_MIN_REQ"/></wb:set>
	        <wb:set id="maxReq"><wb:getDataFieldValue name="CSD_MAX_REQ"/></wb:set>
	        <wb:set id="shiftStartOff"><wb:getDataFieldValue name="CSD_SHFTSTR_OFS"/></wb:set>
	        <wb:set id="shiftEndOff"><wb:getDataFieldValue name="CSD_SHFTEND_OFS"/></wb:set>
<%
            String taskNamePrefix = "";
			String csdDescStr = csdDesc.toString();
			int index = csdDescStr.indexOf(sStaffReqNameIn);
			if (index > 0) {
			   taskNamePrefix = csdDescStr.substring(0, index);
			}
%>

		<INPUT TYPE="hidden" NAME="csdId<%=counter%>" VALUE="<%=csdId%>">
		<INPUT TYPE="hidden" NAME="taskNamePrefix<%=counter%>" VALUE="<%=taskNamePrefix%>">
	        <tr>
			   <%String lnkShowStafReqInst = "lnkShowStafReqInst_" + counter;%>
	          <td colspan=4>
                 <table>
		            <tr>
					   <td>
					      <wb:controlField cssClass="inputField" submitName="<%=sCheckBoxName%>" id="<%=sCheckBoxName%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
					   </td>
			           <td>
	                      <wb:localize id="<%=sStaffReqInst%>"><U>Staffing Requirement Instance</U></b></wb:localize>
	                   </td>
			           <td>
			            <wb:controlField id="<%=lnkShowStafReqInst%>" 
					         submitName="<%=lnkShowStafReqInst%>" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowStafReqInstSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			          </td>
			      </tr>
		       </table>
	         </td>
	        </tr>
	        
	        <%String chkbx_1 = "chkbx_1_" + counter;%>
	        <tr>
			  <td>&nbsp;</td>
	          <td width="10%">
   	            <wb:controlField cssClass="inputField" submitName="<%=chkbx_1%>" id="<%=chkbx_1%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td width="40%">Name / Task Name:</td>
	          <td width="50%">
   	            <wb:controlField cssClass="inputField" submitName="<%=sTaskName%>" id="<%=sTaskName%>"
   	        	  ui="StringUI"
   	      		  uiParameter="width=30"><%=taskName%></wb:controlField>
	          </td>
	        </tr>
	        
	        <%String chkbx_2 = "chkbx_2_" + counter;%>
	        <tr>
			  <td>&nbsp;</td>
	          <td width="10%">
   	            <wb:controlField cssClass="inputField" submitName="<%=chkbx_2%>" id="checkbx_<%=chkbx_2%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td width="40%">Effective Start Date:</td>
	          <td width="50%">
   	            <wb:controlField cssClass="inputField" submitName="<%=sStartDate%>" id="<%=sStartDate%>"
   	        	  ui="DatePickerUI"
   	      		  uiParameter="format=SHORT_DATE"><%=startDate%></wb:controlField>
	          </td>
	        </tr>
	        
	        <%String chkbx_3 = "chkbx_3_" + counter;%>
	        <tr>
			  <td>&nbsp;</td>
	          <td width="10%">
   	            <wb:controlField cssClass="inputField" submitName="<%=chkbx_3%>" id="<%=chkbx_3%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td width="40%">Effective End Date:</td>
	          <td width="50%">
   	            <wb:controlField cssClass="inputField" submitName="<%=sEndDate%>" id="<%=sEndDate%>"
   	        	  ui="DatePickerUI"
   	      		  uiParameter="format=SHORT_DATE"><%=endDate%></wb:controlField>
	          </td>
	        </tr>
	        
	        <tr>
			  <td>&nbsp;</td>
	          <td colspan=3>
	          <BR><BR><wb:localize id="<%=sStaffReqAtts%>"><U>Staffing Requirement Attributes</U></wb:localize>
	          </td>
	        </tr>  
	        
	        <%String chkbx_4 = "chkbx_4_" + counter;%>
			<%String lnkShowActivity = "lnkShowActivity_" + counter;%>
	        <tr>
			  <td>&nbsp;</td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=chkbx_4%>" id="<%=chkbx_4%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>

	          <td>
			     <table>
				    <tr>
					   <td>
			              Activity:
			           </td>
			           <td> 
					      <wb:controlField id="<%=lnkShowActivity%>" 
					         submitName="<%=lnkShowActivity%>" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowActivitySQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				    </table>
				 </td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=sActivity%>" id="<%=sActivity%>"
   	        	  ui="DBLookupUI"
   	      		  uiParameter="source='SELECT ACT_ID,ACT_NAME,ACT_DESC FROM SO_ACTIVITY' sourceType='SQL'"><%=activity%></wb:controlField>
	          </td>
	        </tr>
	        
	        <%String chkbx_5 = "chkbx_5_" + counter;%>
			<%String lnkShowJob = "lnkShowJob_" + counter;%>
	        <tr>
			  <td>&nbsp;</td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=chkbx_5%>" id="<%=chkbx_5%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td> 
			     <table>
				    <tr>
					   <td>
			              Job:
			           </td>
			           <td> 
					      <wb:controlField id="<%=lnkShowJob%>" 
					         submitName="<%=lnkShowJob%>" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowJobSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
				 </td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=sJob%>" id="<%=sJob%>"
   	        	  ui="DBLookupUI"
   	      		  uiParameter="source='SELECT JOB_ID,JOB_NAME,JOB_DESC FROM JOB' sourceType='SQL'"><%=job%></wb:controlField>
	          </td>
	        </tr>
	        
	        <%String chkbx_6 = "chkbx_6_" + counter;%>
			<%String lnkShowSkill = "lnkShowSkill_" + counter;%>
	        <tr>
			   <td>&nbsp;</td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=chkbx_6%>" id="<%=chkbx_6%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			  <table>
				    <tr>
					   <td>
			              Skill:
			           </td>
			           <td> 
					      <wb:controlField id="<%=lnkShowSkill%>" 
					         submitName="<%=lnkShowSkill%>" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowSkillSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
				 </td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=sSkill%>" id="<%=sSkill%>"
   	        	  ui="DBLookupUI"
   	      		  uiParameter="source='SELECT STSKL_ID,STSKL_NAME,STSKL_DESC FROM ST_SKILL' sourceType='SQL'"><%=skill%></wb:controlField>
	          </td>
	        </tr>
	        
	        <%String chkbx_7 = "chkbx_7_" + counter;%>
			<%String lnkShowSkillLevel = "lnkShowSkillLevel_" + counter;%>
	        <tr>
			   <td>&nbsp;</td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=chkbx_7%>" id="<%=chkbx_7%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Skill Level:
			           </td>
			           <td> 
					      <wb:controlField id="<%=lnkShowSkillLevel%>" 
					         submitName="<%=lnkShowSkillLevel%>" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowSkillLevelSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
				 </td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=sSkillLevel%>" id="<%=sSkillLevel%>"
   	        	  ui="NumberUI"
   	      		  uiParameter="width=10"><%=skillLevel%></wb:controlField>
	          </td>
	        </tr>
	        
	        <%String chkbx_8 = "chkbx_8_" + counter;%>
			<%String lnkShowMinRequired = "lnkShowMinRequired_" + counter;%>
	        <tr>
			   <td>&nbsp;</td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=chkbx_8%>" id="<%=chkbx_8%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Min Required:
			           </td>
			           <td> 
					      <wb:controlField id="<%=lnkShowMinRequired%>" 
					         submitName="<%=lnkShowMinRequired%>" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowMinRequiredSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
				 </td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=sMinReq%>" id="<%=sMinReq%>"
   	        	  ui="NumberUI"
   	      		  uiParameter="width=10"><%=minReq%></wb:controlField>
	          </td>
	        </tr>
	        
	        <%String chkbx_9 = "chkbx_9_" + counter;%>
			<%String lnkShowMaxRequired = "lnkShowMaxRequired_" + counter;%>
	        <tr>
			   <td>&nbsp;</td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=chkbx_9%>" id="<%=chkbx_9%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Max Required:
			           </td>
			           <td> 
					      <wb:controlField id="<%=lnkShowMaxRequired%>" 
					         submitName="<%=lnkShowMaxRequired%>" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowMaxRequiredSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
				 </td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=sMaxReq%>" id="<%=sMaxReq%>"
   	        	  ui="NumberUI"
   	      		  uiParameter="width=10"><%=maxReq%></wb:controlField>
	          </td>
	        </tr>
	        
	        <%String chkbx_10 = "chkbx_10_" + counter;%>
			<%String lnkShowShiftStartOffset = "lnkShowShiftStartOffset_" + counter;%>
	        <tr>
			   <td>&nbsp;</td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=chkbx_10%>" id="<%=chkbx_10%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Shift Start Offset:
			           </td>
			           <td> 
					      <wb:controlField id="<%=lnkShowShiftStartOffset%>" 
					         submitName="<%=lnkShowShiftStartOffset%>" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowShiftStartOffsetSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
				 </td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=sShiftStartOff%>" id="<%=sShiftStartOff%>"
   	        	  ui="NumberUI"
   	      		  uiParameter="width=10"><%=shiftStartOff%></wb:controlField>
	          </td>
	        </tr>
	        
	        <%String chkbx_11 = "chkbx_11_" + counter;%>
			<%String lnkShowShiftEndOffset = "lnkShowShiftEndOffset_" + counter;%>
	        <tr>
			   <td>&nbsp;</td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=chkbx_11%>" id="<%=chkbx_11%>"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Shift End Offset:
			           </td>
			           <td> 
					      <wb:controlField id="<%=lnkShowShiftEndOffset%>" 
					         submitName="<%=lnkShowShiftEndOffset%>" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowShiftEndOffsetSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
			   </td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="<%=sShiftEndOff%>" id="<%=sShiftEndOff%>"
   	        	  ui="NumberUI"
   	      		  uiParameter="width=10"><%=shiftEndOff%></wb:controlField>
	          </td>
	        </tr>
	        
	        <!-- now output associated drivers -->
	        
	        <wb:sql createDataSource="driverSQL">
		       SELECT a.SKDGRP_ID SKDGRP_ID, a.SKDGRP_DESC as SKDGRP_DESC,a.SKDGRP_NAME as SKDGRP_NAME,
		       b.DIST_ID as DIST_ID,b.WRKLD_STDVOL_HOUR as WRKLD_STDVOL_HOUR, b.WRKLD_ID WRKLD_ID, b.WRKLD_DESC WRKLD_DESC
		       FROM SO_SCHEDULE_GROUP a, SO_VOLUME_WORKLOAD b  
			   where a.SKDGRP_INTRNL_TYPE = 12
		       AND a.SKDGRP_ID = b.SKDGRP_ID 
			   AND b.CSD_ID = <%=csdId%>
		   </wb:sql>
		<% int driverCounter = 1; %>
	    <tr>
		   <td>&nbsp;</td>
		   <td colspan=10>
		<wba:table>
		      <tr>
			  <%String lnkShowDriver = "lnkShowDriver_" + counter;%>
	          <td>
			     <table>
				    <tr>
					   <td>
					      <wb:localize id="Drivers"><U>Drivers</U></wb:localize>
					   </td>
					   <td>
					      <wb:controlField id="<%=lnkShowDriver%>" 
					         submitName="<%=lnkShowDriver%>" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowDriverSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
					   </td>
					</tr>
				 </table>
			  </td>
	        </tr>  
	          <tr>
	            <td colspan=2><b>Description</b></td>
	            <td colspan=2><b>Driver</b></td>
	            <td colspan=2><b>Productivity</b></td>
	            <td colspan=2><b>Distribution</b></td>
				<td colspan=2>&nbsp;</td>
	          </tr>
		
	          <wb:dataSet id="driverDataSet" dataSource="driverSQL">
	            <wb:forEachRow dataSet="driverDataSet">
				<%
				  String sDescription = "Description" + String.valueOf(counter) + String.valueOf(driverCounter);
	              String sDriverName = "DriverName" + String.valueOf(counter) + String.valueOf(driverCounter);
	              String sProductivity = "Productivity" + String.valueOf(counter) + String.valueOf(driverCounter);
	              String sDistribution = "Distribution" + String.valueOf(counter) + String.valueOf(driverCounter);
	            %>
	            
	            <wb:set id="description"><wb:getDataFieldValue dataSet="driverDataSet" name="WRKLD_DESC"/></wb:set>
	            <wb:set id="driver"><wb:getDataFieldValue dataSet="driverDataSet" name="SKDGRP_ID"/></wb:set>
	            <wb:set id="productivity"><wb:getDataFieldValue dataSet="driverDataSet" name="WRKLD_STDVOL_HOUR"/></wb:set>
	            <wb:set id="distribution"><wb:getDataFieldValue dataSet="driverDataSet" name="DIST_ID"/></wb:set>
	            <wb:set id="skdgrpIdnew"><wb:getDataFieldValue dataSet="driverDataSet" name="SKDGRP_ID"/></wb:set>
	            <wb:set id="wrkldId"><wb:getDataFieldValue dataSet="driverDataSet" name="WRKLD_ID"/></wb:set>
		        <INPUT TYPE="hidden" NAME="skdgrpId<%=counter%><%=driverCounter%>" VALUE="<%=skdgrpIdnew%>">
		        <INPUT TYPE="hidden" NAME="wrkldId<%=counter%><%=driverCounter%>" VALUE="<%=wrkldId%>">
                
	             <%String chkbx_1_1 = "chkbx1" + counter + driverCounter;%>
	             <tr>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="<%=chkbx_1_1%>" id="<%=chkbx_1_1%>"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
   	                </td>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="<%=sDescription%>" id="<%=sDescription%>"
   	        	        ui="StringUI" uiParameter="width=20"><%=description%></wb:controlField>
	                </td>
	                
	             <%String chkbx_1_2 = "chkbx2" + counter + driverCounter;%>
	                <td width="5%">
   	                  <wb:controlField cssClass="inputField" submitName="<%=chkbx_1_2%>" id="<%=chkbx_1_2%>"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
	                </td>
	                <td>
					   <wb:controlField ui='DBComboboxUI' submitName="<%=sDriverName%>" id="<%=sDriverName%>" uiParameter='<%=driverNameUiParams%>'><%=driver%></wb:controlField>
	                </td>
	                
	             <%String chkbx_1_3 = "chkbx3" + counter + driverCounter;%>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="<%=chkbx_1_3%>" id="<%=chkbx_1_3%>"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
	                </td>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="<%=sProductivity%>" id="<%=sProductivity%>"
   	        	        ui="NumberUI" uiParameter="width=10"><%=productivity%></wb:controlField>
	                </td>
	                
	             <%String chkbx_1_4 = "chkbx4" + counter + driverCounter;%>
				 <%String chkbx_1_5 = "chkbx5" + counter + driverCounter;%>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="<%=chkbx_1_4%>" id="<%=chkbx_1_4%>"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
	                </td>
	                <td>
					      <wb:controlField ui='DBComboboxUI' submitName="<%=sDistribution%>" id="<%=sDistribution%>" uiParameter='<%=distributionUiParams%>'><%=distribution%></wb:controlField>
                    </td>
					<td>
						  <wb:controlField cssClass="inputField" submitName="<%=chkbx_1_5%>" id="<%=chkbx_1_5%>"
   	        	           ui="CheckboxUI"
   	      		           uiParameter="alternateField='true'"/>
					</td>
					<td>
					      <img src='<%= request.getContextPath() %>/images/interface/icon_head_trash.gif' border='0'>
	                </td>
	              </tr>
	              
	              <% driverCounter++; %>
	            </wb:forEachRow>
	          </wb:dataSet>
	          <INPUT TYPE="hidden" name="driverCounter<%=counter%>" value="<%=driverCounter%>">

			  <%
				  String newDescription = "Description" + String.valueOf(counter);
	              String newDriverName = "DriverName" + String.valueOf(counter);
	              String newProductivity = "Productivity" + String.valueOf(counter);
	              String newDistribution = "Distribution" + String.valueOf(counter);

			  %>
	             <tr>
	               <%String chkbxNew1 = "chkbxNew_1_" + counter;%>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="<%=chkbxNew1%>" id="<%=chkbxNew1%>"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
   	                </td>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="<%=newDescription%>" id="<%=newDescription%>"
   	        	        ui="StringUI"
   	      		        uiParameter="width=20">[New Driver]</wb:controlField>
	                </td>
	               <%String chkbxNew2 = "chkbxNew_2_" + counter;%>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="<%=chkbxNew2%>" id="<%=chkbxNew2%>"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
	                </td>
	                <td>
					   <wb:controlField ui='DBComboboxUI' submitName="<%=newDriverName%>" id="<%=newDriverName%>" uiParameter='<%=driverNameUiParams%>'></wb:controlField>
	                </td>
	               <%String chkbxNew3 = "chkbxNew_3_" + counter;%>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="<%=chkbxNew3%>" id="<%=chkbxNew3%>"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
	                </td>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="<%=newProductivity%>" id="<%=newProductivity%>"
   	        	        ui="NumberUI"
   	      		        uiParameter="width=10"></wb:controlField>
	                </td>
	               <%String chkbxNew4 = "chkbxNew_4_" + counter;%>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="<%=chkbxNew4%>" id="<%=chkbxNew4%>"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
	                </td>
	                <td>
					      <wb:controlField ui='DBComboboxUI' submitName="<%=newDistribution%>" id="<%=newDistribution%>" uiParameter='<%=distributionUiParams%>'></wb:controlField>      
	                </td>
					<td>&nbsp;</td>
					<td>&nbsp;</td>
	              </tr>
	        </wba:table>
	        </td>
			</tr>	        
	      <% counter++; %>  
	</wba:table>
	</td>
	</tr>
	      </wb:forEachRow>
	    </wb:dataSet>
	<INPUT TYPE="hidden" name="counter" value="<%=counter%>">
	</wba:table>	    
	<wba:button name="btnEdit" label="Apply" labelLocalizeIndex="Apply" onClick="edit(this);"></wba:button>
	<wba:button name="btnCancel" label="Cancel" labelLocalizeIndex="Cancel" onClick="<%=goBack%>"></wba:button>
	<wba:button name="btnBack" label="Back" labelLocalizeIndex="Back" onClick="history.back();"></wba:button>
<%}

// Case fo Add JSP
else if (StringHelper.equalsIgnoreCase("ADD",actionType)){
	
%>
	<wba:table width="100%" caption="Add/Edit Staffing Requirement" captionLocalizeIndex="Caption_Add_Edit_Staffing_Requirement">
	
	  <tr>
	     <td colspan="3">
	        <table>
		       <tr>
			      <td>
	                 <wb:localize id="StaffReqInst"><U>Staffing Requirement Instance</U></wb:localize>
	              </td>
			      <td>
			         <wb:controlField id="lnkShowStafReqInst" 
					         submitName="lnkShowStafReqInst" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowStafReqInstSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			     </td>

			   </tr>
		    </table>
		 </td>
	    
	        </tr>
	        
	        <tr>
	          <td width="10%">
   	            <wb:controlField cssClass="inputField" submitName="chkbx1" id="chkbx1"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td width="40%">Name / Task Name:</td>
	          <td width="50%">
   	            <wb:controlField cssClass="inputField" submitName="TaskName" id="TaskName"
   	        	  ui="StringUI"
   	      		  uiParameter="width=30"/>
	          </td>
	        </tr>
	        
	        <tr>
	          <td width="10%">
   	            <wb:controlField cssClass="inputField" submitName="chkbx2" id="chkbx2"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td width="40%">Effective Start Date:</td>
	          <td width="50%">
   	            <wb:controlField cssClass="inputField" submitName="StartDate" id="EndDate"
   	        	  ui="DatePickerUI"
   	      		  uiParameter="format=SHORT_DATE"/>
	          </td>
	        </tr>
	        
	        <tr>
	          <td width="10%">
   	            <wb:controlField cssClass="inputField" submitName="chkbx3" id="chkbx3"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td width="40%">Effective End Date:</td>
	          <td width="50%">
   	            <wb:controlField cssClass="inputField" submitName="EndDate" id="EndDate"
   	        	  ui="DatePickerUI"
   	      		  uiParameter="format=SHORT_DATE"/>
	          </td>
	        </tr>
	        
	        <tr>
	          <td colspan=3>
	          <BR><BR><wb:localize id="StaffReqAtts"><U>Staffing Requirement Attributes</U></wb:localize>
	          </td>
	        </tr>  
	        
	        <tr>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="chkbx4" id="chkbx4"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
			  <td>
			     <table>
				    <tr>
					   <td>
			              Activity:
			           </td>
			           <td> 
					      <wb:controlField id="lnkShowActivity" 
					         submitName="lnkShowActivity" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowActivitySQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
			  </td>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="Activity" id="Activity"
   	        	  ui="DBLookupUI"
   	      		  uiParameter="source='SELECT ACT_ID,ACT_NAME,ACT_DESC FROM SO_ACTIVITY' sourceType='SQL'"/>
	          </td>
	        </tr>
	        
	        <tr>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="chkbx5" id="chkbx5"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Job:
			           </td>
			           <td> 
					      <wb:controlField id="lnkShowJob" 
					         submitName="lnkShowJob" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowJobSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
			  </td>
			  
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="Job" id="Job"
   	        	  ui="DBLookupUI"
   	      		  uiParameter="source='SELECT JOB_ID,JOB_NAME,JOB_DESC FROM JOB' sourceType='SQL'"/>
	          </td>
	        </tr>
	        
	        <tr>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="chkbx6" id="chkbx6"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Skill:
			           </td>
			           <td> 
					      <wb:controlField id="lnkShowSkill" 
					         submitName="lnkShowSkill" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowSkillSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
			  </td>
			  
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="Skill" id="Skill"
   	        	  ui="DBLookupUI"
   	      		  uiParameter="source='SELECT STSKL_ID,STSKL_NAME,STSKL_DESC FROM ST_SKILL' sourceType='SQL'"/>
	          </td>
	        </tr>
	        
	        <tr>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="chkbx7" id="chkbx7"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Skill Level:
			           </td>
			           <td> 
					      <wb:controlField id="lnkShowSkillLevel" 
					         submitName="lnkShowSkillLevel" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowSkillLevelSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
			  </td>
			  
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="SkillLevel" id="SkillLevel"
   	        	  ui="NumberUI"
   	      		  uiParameter="width=10"/>
	          </td>
	        </tr>
	        
	        <tr>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="chkbx8" id="chkbx8"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Min Required:
			           </td>
			           <td> 
					      <wb:controlField id="lnkShowMinRequired" 
					         submitName="lnkShowMinRequired" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowMinRequiredSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
			  </td>
			  
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="MinReq" id="MinReq"
   	        	  ui="NumberUI"
   	      		  uiParameter="width=10"/>
	          </td>
	        </tr>
	        
	        <tr>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="chkbx9" id="chkbx9"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Max Required:
			           </td>
			           <td> 
					      <wb:controlField id="lnkShowMaxRequired" 
					         submitName="lnkShowMaxRequired" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowMaxRequiredSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
			  </td>
			  
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="MaxReq" id="MaxReq"
   	        	  ui="NumberUI"
   	      		  uiParameter="width=10"/>
	          </td>
	        </tr>
	        
	        <tr>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="chkbx10" id="chkbx10"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Shift Start Offset:
			           </td>
			           <td> 
					      <wb:controlField id="lnkShowShiftStartOffset" 
					         submitName="lnkShowShiftStartOffset" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowShiftStartOffsetSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
			  </td>
			  
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="ShiftStartOff" id="ShiftStartOff"
   	        	  ui="NumberUI"
   	      		  uiParameter="width=10"/>
	          </td>
	        </tr>
	        
	        <tr>
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="chkbx11" id="chkbx11"
   	        	  ui="CheckboxUI"
   	      		  uiParameter="alternateField='true'"/>
   	          </td>
	          <td>
			     <table>
				    <tr>
					   <td>
			              Shift End Offset:
			           </td>
			           <td> 
					      <wb:controlField id="lnkShowShiftEndOffset" 
					         submitName="lnkShowShiftEndOffset" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowShiftEndOffsetSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
			           </td> 
					</tr>
				 </table>
			  </td>
			  
	          <td>
   	            <wb:controlField cssClass="inputField" submitName="ShiftEndOff" id="ShiftEndOff"
   	        	  ui="NumberUI"
   	      		  uiParameter="width=10"/>
	          </td>
	        </tr>
	        
	        <!-- now output associated drivers -->
	        
	        
		<wba:table>
		      <tr>
	          <td colspan=8>
			     <table>
				    <tr>
					   <td>
					      <wb:localize id="Drivers"><U>Drivers</U></wb:localize>
					   </td>
					   <td>
					      <wb:controlField id="lnkShowDriver" 
					         submitName="lnkShowDriver" 
						     ui='DBLookupUI' 
						     uiParameter="source='#lnkShowDriverSQL#' sourceType='SQL' labelFieldStatus='hidden'" cssClass="inputField"/>
					   </td>
					</tr>
				 </table>
			  </td>
	        </tr>  
	          <tr>
	            <td colspan=2><b>Description</b></td>
	            <td colspan=2><b>Driver</b></td>
	            <td colspan=2><b>Productivity</b></td>
	            <td colspan=2><b>Distribution</b></td>
	          </tr>
	        <tr>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="chkbxNew1" id="chkbxNew1"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
   	                </td>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="Description" id="Description"
   	        	        ui="StringUI"
   	      		        uiParameter="width=20">[New Driver]</wb:controlField>
	                </td>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="chkbxNew2" id="chkbxNew2"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
	                </td>
	                <td>
					   <wb:controlField ui='DBComboboxUI' submitName="DriverName" id="DriverName" uiParameter='<%=driverNameUiParams%>'></wb:controlField>
	                </td>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="chkbxNew3" id="chkbxNew3"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
	                </td>
	                <td>
					   <wb:controlField cssClass="inputField" submitName="Productivity" id="Productivity"
   	        	       ui="NumberUI"
   	      		       uiParameter="width=10"/>
	                </td>
	                <td>
   	                  <wb:controlField cssClass="inputField" submitName="chkbxNew4" id="chkbxNew4"
   	        	        ui="CheckboxUI"
   	      		        uiParameter="alternateField='true'"/>
	                </td>
	                <td>
					   <div class="labeled">
					      <wb:controlField ui='DBComboboxUI' submitName="Distribution" id="Distribution" uiParameter='<%=distributionUiParams%>'></wb:controlField>
					      <!-- <img src='<%= request.getContextPath() %>/images/interface/icon_head_trash.gif' border='0'> -->
					   </div>
	                </td>
	              </tr>
	        </wba:table>
	</wba:table>
	
	<wba:button name="btnAdd" label="Add" labelLocalizeIndex="Add" onClick="add(this);"></wba:button>
	<wba:button name="btnCancel" label="Cancel" labelLocalizeIndex="Cancel" onClick="<%=goBack%>"></wba:button>
	<wba:button name="btnBack" label="Back" labelLocalizeIndex="Back" onClick="history.back();"></wba:button>
	
<%
}		
%>


<script type='text/javascript'>

function validate() {
   var taskName = document.page_form.TaskName.value;
   var startDate = document.page_form.StartDate.value;
   var endDate = document.page_form.EndDate.value;

   var avtivity = document.page_form.Activity.value;
   var job = document.page_form.Job.value;
   var skill = document.page_form.Skill.value;

   if (taskName == '') {
      alert('Task name is a required field.');
	  return false;
   }
   if (startDate == '') {
      alert('Effective Start Date is a required field.');
	  return false;
   }
   if (endDate == '') {
      alert('Effective End Date is a required field.');
	  return false;
   }
   if (avtivity == '') {
      alert('Activity is a required field.');
	  return false;
   }
   if (job == '') {
      alert('Job is a required field.');
	  return false;
   }
   if (skill == '') {
      alert('Skill is a required field.');
	  return false;
   }
   return true;
}

function add(thisForm) {
	if (validate() == false) {
	   return false;
	}
	var newDriver = 'false';
	var newStaffReq = 'false';
	for (var i=1; i<=11;i++ ) {
		var chkboxVal = document.getElementsByName('chkbx'+i)[0].value;

		//alert(chkboxVal);

		if ( chkboxVal == 'Y') {
			newStaffReq = 'true';
			break;
		}
	}
	for (var x=1; x<=4; x++){
		var chkboxVal = document.getElementsByName('chkbxNew'+x)[0].value;
		if ( chkboxVal == 'Y') {
			newDriver = 'true';
			break;		
		}
	}
	
	if ((i > 11 && newStaffReq == 'true') || (x > 4 && newDriver == 'true')){
	  alert ("At least one check box must be checked to add");
	}
	else{
		if (newDriver == 'true'){
			var description = document.getElementsByName('Description')[0].value;
		    var driverName = document.getElementsByName('DriverName')[0].value;
			var productivity = document.getElementsByName('Productivity')[0].value;
			if (description == '' || description == '[New Driver]' ){
		       alert("Please enter a description");
			   return false;
		    }
		    if (driverName == ''){
		       alert("Please select a driver");
			   return false;
		    }
			if (productivity == ''){
		       alert("Please enter a productivity.");
			   return false;
		    }
		}
		    document.getElementsByName('actionType')[0].value = 'ADDACTION';
		    document.forms[0].submit();

	}
}


function edit(thisForm) {
	var counter = document.getElementsByName('counter')[0].value;
	for (var i=1; i<counter;i++ ) {
		var chkboxVal = document.getElementsByName('chkbx'+i)[0].value;
		var csdId = document.getElementsByName('csdId'+i)[0].value;
		if ( chkboxVal == 'Y') {
			break;
		}
	}
	
	if (i == counter){
	  alert ("you must check a staffing requirement to edit");
	}
	else{
		for (var i=1; i<counter;i++ ) {
		   var chkboxVal = document.getElementsByName('chkbx'+i)[0].value;
		   var newDriver = 'false';
		   if ( chkboxVal == 'Y') {
			  var driverCounter = document.getElementsByName('driverCounter' + i)[0].value;
			  for (var j=1; j<driverCounter; j++) {
				 var updateWld = false;
				 var deleteWld = false;
			     for (var x=1; x<=4; x++) {
				    var wrkldChkboxVal = document.getElementsByName('chkbx'+ x + i + j)[0].value;
			        if (wrkldChkboxVal == "Y"){
					   updateWld = true;
					}
				 }
				 //alert(i + ":" + updateWld);
				 var delChkboxVal = document.getElementsByName('chkbx'+ 5 + i + j)[0].value;
				 if (delChkboxVal == "Y"){
				    deleteWld = true;
				 }
				 //alert(i + ":*****" + deleteWld);
				 if ( updateWld == true && deleteWld == true ) {
				    alert ("you cannot update and delete the driver workload at same time");
					return false;
				 }
			  }
			  //adding new volume workload....
		         var newChkbox1Val = document.getElementsByName('chkbxNew_1' + '_' + i)[0].value;
				 var newChkbox2Val = document.getElementsByName('chkbxNew_2' + '_' + i)[0].value;
				 var newChkbox3Val = document.getElementsByName('chkbxNew_3' + '_' + i)[0].value;
		         if ( newChkbox1Val == 'Y' && newChkbox2Val == 'Y' && newChkbox3Val == 'Y') {
			        newDriver = 'true';	
		         }
			  //alert(newDriver);
			  if (newDriver == 'true'){
			     var description = document.getElementsByName('Description' + i)[0].value;
		         var driverName = document.getElementsByName('DriverName' + i)[0].value;
			     var productivity = document.getElementsByName('Productivity' + i)[0].value;
			     if (description == '' || description == '[New Driver]' ){
		            alert("Please enter a description for the new workload.");
			        return false;
		         }
		         if (driverName == ''){
		            alert("Please select a driver for the new workload.");
			        return false;
		         }
			     if (productivity == ''){
		            alert("Please enter a productivity for the new workload.");
			        return false;
		         }
			  }
		   }
		}
		document.getElementsByName('actionType')[0].value = 'EDITACTION';
		document.forms[0].submit();
	}
}


function cancel(thisForm){
	document.forms[0].action="./staffReq.jsp";
	document.forms[0].submit();
}


</script>


</wb:page>