<%@ page import="java.util.*"%> 
<%@ page import="java.sql.*"%> 
<%@ page import="java.text.SimpleDateFormat"%> 
<%@ page import="com.workbrain.util.*" %> 
<%@ page import="com.workbrain.sql.*"%> 
<%@ page import="com.workbrain.server.jsp.*"%> 
<%@ page import="com.wbiag.app.modules.retailSchedule.mce.*"%> 

<%@ include file="/system/wbheader.jsp"%>
<style type="text/css"> 
@import url("<%=request.getContextPath()%>/modules/retailSchedule/css/wbextended.css"); 

table.contentTable.form th{
	width: 250px;
	height: 50px;
}

table.contentTable.form td.subSection{
	font-weight: normal;
	width: 250px;
	height: 50px;
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
<wb:page login="true" popupPage="false" subsidiaryPage="false" title="Hours of Operation" submitMethod="post"> 
	<wb:define id="dept_search"><wb:get id="dept_search" scope="parameter" default='true'/></wb:define>
	<wb:submit id="dept_search"><wb:get id="dept_search"/></wb:submit>
	<wb:define id="selected_skdgrp_list"><wb:get id="selected_skdgrp_list" scope="parameter" default='true'/></wb:define>
	<wb:submit id="selected_skdgrp_list"><wb:get id="selected_skdgrp_list"/></wb:submit>
	<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" /></wb:define>
    <wb:submit id="mfrm_id"><wb:get id="mfrm_id"/></wb:submit>
	<wb:define id="searched_skdgrp_list"><wb:get id="searched_skdgrp_list" scope="parameter" default=''/></wb:define>
	<wb:submit id="searched_skdgrp_list"><wb:get id="searched_skdgrp_list"/></wb:submit>
	<wb:define id="MCESS_TYPE"><wb:get id="MCESS_TYPE" scope="parameter" default=''/></wb:define>
	<wb:submit id="MCESS_TYPE"><wb:get id="MCESS_TYPE"/></wb:submit>
	<wb:define id="actionType"><wb:get id="actionType" scope="parameter" default=''/></wb:define>
	<wb:submit id="actionType"><wb:get id="actionType"/></wb:submit>
<%
String toProcessRequest = request.getParameter("process");
String mfrmId = request.getParameter("mfrm_id");
String cancelUrl =  "location.href = '" + request.getContextPath() + "/maintenance/mntForms.jsp?mfrm_id=" + mfrmId + "'; return false;";

	if ("Y".equals(toProcessRequest)) {

		DBConnection conn = JSPHelper.getConnection(request);

		MCEConfiguration mceConfig = new MCEConfiguration(request);
		mceConfig.processDistribution(conn, request);

		%>
		<script>
		document.forms[0].action = '<%=request.getContextPath()%>' + "/modules/retailSchedule/mce/usageMode.jsp";
		document.forms[0].submit();
		</script>
		<%

	}

String userName = JSPHelper.getWebLogin(request).getUserName();
String actualUserName = JSPHelper.getWebLogin(request).getActualUserName();
//expecting either basicStoreConfiguration or basicDepartmentConfiguration
String deptSearch = request.getParameter("dept_search");
String locList = request.getParameter("selected_skdgrp_list");

final String STORE_CAPTION = "Add/Edit Distribution from Historical Results";
String configCaption = null;

configCaption = STORE_CAPTION;

String sqlString = "";
%>


	<wb:define id="formCaption, propId, propName, lnkShowPropSQL"/>
	<wb:set id="formCaption"><%=configCaption%></wb:set>
	<INPUT TYPE="hidden" name="locList" value="<%=locList%>">
    <INPUT TYPE="hidden" name="userName" value="<%=userName%>">
	<INPUT TYPE="hidden" name="actualUserName" value="<%=actualUserName%>">


	<wb:define id="dblDriverSQL">
	select DISTINCT(SG.CLNTTYP_ID), CLNTTYP_NAME 
	from so_schedule_group SG, SO_CLIENT_TYPE CT
	WHERE SG.CLNTTYP_ID = CT.CLNTTYP_ID 
	AND	SKDGRP_INTRNL_TYPE = 12 
	AND	SKDGRP_PARENT_ID IN ( <%=locList%> )
	</wb:define>



	<wba:table class="contentTable form" caption='#formCaption#' captionLocalizeIndex='Table Caption'> 



		<tr> 
			<td>
				<wb:controlField id="chkSection1" submitName="chkSection1" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
			</td> 
			<th><wb:localize id='Distribution Name'>Distribution Name</wb:localize></th> 
			<td width='600'>
			</td> 
		</tr> 
	<tr > 
		<td>
		</td> 
		<td class="subSection">Name</td> 
		<td>
		<div class="labeled">
			<wb:controlField id="txtName" submitName="txtName" ui='StringUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		</td>
	</tr>
</wba:table>
<wba:table class="contentTable form">
	<tr> 
		<td>
			<wb:controlField id="chkSection2" submitName="chkSection2" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Default Hours'>(Re-)Calculate Details</wb:localize></th> 
		<td width='600'>
		</td> 
	</tr> 
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Distribution Mode'>Distribution Mode</wb:localize></td> 
		<td>
		<div class="labeled">
			<wb:controlField 
				id="ddlMode" 
				submitName="ddlMode"
				ui='ComboBoxUI' 
				uiParameter="valueList='1,2' labelList='Normal,Inverse' labelFieldStatus='default' nullable=false" 
				cssClass="inputField"
				/>
		</div>
		</td>
	</tr>
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Generate distribution using results from'>Generate distribution using results from</wb:localize></td> 
		<td>
		<div class="labeled">
			<div class="labeled">
				<wb:controlField 
					id="dblDriver" 
					submitName="dblDriver"
					ui='DBLookupUI' 
					uiParameter="source='#dblDriverSQL#' sourceType='SQL' labelFieldStatus='default'" 
					cssClass="inputField"
					/>
			</div>
		</div>

		</td>
	</tr>

	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Using results from the period'>Using results from the period from</wb:localize> 
		<td width='600'>
		<div class="labeled">
		 <wb:controlField id="txtDateFrom1" submitName="txtDateFrom1" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		through
		</div>
		<div class="labeled">
		<wb:controlField id="txtDateTo1" submitName="txtDateTo1" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		</td> 		
	</tr>
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Using results from the period'>Using results from the period from</wb:localize> 
		<td width='600'>
		<div class="labeled">
		 <wb:controlField id="txtDateFrom2" submitName="txtDateFrom2" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		through
		</div>
		<div class="labeled">
		<wb:controlField id="txtDateTo2" submitName="txtDateTo2" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		</td> 		
	</tr>
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Using results from the period'>Using results from the period from</wb:localize> 
		<td width='600'>
		<div class="labeled">
		 <wb:controlField id="txtDateFrom1" submitName="txtDateFrom3" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		through
		</div>
		<div class="labeled">
		<wb:controlField id="txtDateTo3" submitName="txtDateTo3" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		</td> 		
	</tr>
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Using results from the period'>Using results from the period from</wb:localize> 
		<td width='600'>
		<div class="labeled">
		 <wb:controlField id="txtDateFrom4" submitName="txtDateFrom4" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		through
		</div>
		<div class="labeled">
		<wb:controlField id="txtDateTo4" submitName="txtDateTo4" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		</td> 		
	</tr>
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Using results from the period'>Using results from the period from</wb:localize> 
		<td width='600'>
		<div class="labeled">
		 <wb:controlField id="txtDateFrom5" submitName="txtDateFrom5" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		through
		</div>
		<div class="labeled">
		<wb:controlField id="txtDateTo5" submitName="txtDateTo5" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		</td> 		
	</tr>
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Use Data From'>Use Data From</wb:localize></td> 
		<td>
		<div class="labeled">
			<wb:controlField 
				id="ddlDays" 
				submitName="ddlDays"
				ui='ComboBoxUI' 
				uiParameter="valueList='0,1,2,3,4,5,6,7,8,9' labelList='All Days,Sunday,Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Weekdays Only, Weekends Only' nullable='false' labelFieldStatus='default' " 
				cssClass="inputField"
				/>
		</div>
		</td>
	</tr>
	<tr > 
		<td>
		</td> 
		<td class="subSection">Include Special Days</td> 
		<td>
		<div class="labeled">
			<wb:controlField id="chkIncludeSpecDay" submitName="chkIncludeSpecDay" ui='CheckboxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		</td>
	</tr>
	</wba:table>

	<wba:table class="contentTable form"> 
	<tr>
	<td>
			<div class="submitaction"> 
			<wba:button name="btnApply" label="Apply" labelLocalizeIndex="Apply" onClick="apply(this);"></wba:button>
			<wba:button name="btnCancel" label="Cancel" labelLocalizeIndex="Cancel" onClick='<%=cancelUrl%>'></wba:button>
			<wba:button name="btnBack" label="Back" labelLocalizeIndex="Back" onClick="history.back();"></wba:button>
			</div>
		</td>
		</tr>
	</wba:table>

	
<p>



<INPUT TYPE="hidden" name="process" value="N">

<script type='text/javascript'>


function apply(thisForm) {

	//alert(document.location.href);
	document.getElementsByName('process')[0].value = 'Y';
	document.forms[0].submit()

}

</script>

</wb:page>