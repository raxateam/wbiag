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
<wb:page login="true" popupPage="false" subsidiaryPage="false" title="Hours of Operation" submitMethod="post"> 
<%
String toProcessRequest = request.getParameter("process");
String actionType = request.getParameter("actionType");


if ( "specDayDELETE".equals(actionType) ) {

		DBConnection conn = JSPHelper.getConnection(request);

		MCEConfiguration mceConfig = new MCEConfiguration(request);
		mceConfig.deleteSpecialDay(conn, request);


} else if ( "distribDELETE".equals(actionType) ) {

		DBConnection conn = JSPHelper.getConnection(request);

		MCEConfiguration mceConfig = new MCEConfiguration(request);
		mceConfig.deleteDistribution(conn, request);


}
String userName = JSPHelper.getWebLogin(request).getUserName();
String actualUserName = JSPHelper.getWebLogin(request).getActualUserName();
//expecting either basicStoreConfiguration or basicDepartmentConfiguration
String deptSearch = request.getParameter("dept_search");
String locList = request.getParameter("selected_skdgrp_list");
System.out.println("locList    " + locList );

final String STORE_CAPTION = "Special Days / Distributions";
final String DEPT_CAPTION = "Special Days";
String configCaption = null;

	if ( !deptSearch.equals("true") ) {
		
		configCaption = STORE_CAPTION;

	} else  {

		configCaption = DEPT_CAPTION;

	}


%>

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
   <wb:define id="client_type_id"><wb:get id="client_type_id" scope="parameter" default=''/></wb:define>
   <wb:submit id="client_type_id"><wb:get id="client_type_id"/></wb:submit>

	<wb:define id="formCaption"/>
	<wb:set id="formCaption"><%=configCaption%></wb:set>
	<INPUT TYPE="hidden" name="userName" value="<%=userName%>">
	<INPUT TYPE="hidden" name="actualUserName" value="<%=actualUserName%>">


	<wb:sql createDataSource="distinctSpecDaySQL">
	SELECT DISTINCT (SPECDAY_NAME)
	FROM SO_FCAST_SPEC_DAY 
	WHERE SKDGRP_ID IN ( <%=locList%> )
	</wb:sql>

	<wb:sql createDataSource="distinctDistributionSQL">
	SELECT DISTINCT(DIST_NAME)  FROM SO_DISTRIBUTION 
	WHERE SKDGRP_ID IN ( <%=locList%> )
	</wb:sql>



	<wba:table class="contentTable form" caption='#formCaption#' captionLocalizeIndex='Table Caption'> 
	<tr> 
		<td>
		</td> 
		<th><wb:localize id='Special Days'>Special Days</wb:localize></th> 
		<td></td>
	</tr>


	<% int specDayCount = 1; %>
	<wb:define id="specDayId, specDayName, lnkShowSpecDaySQL"/>
	<wb:dataSet id="distinctSpecDayDataSet" dataSource="distinctSpecDaySQL">

		<wb:forEachRow dataSet="distinctSpecDayDataSet">

				<%	
			
					String lnkShowSpecDay = "lnkShowSpecDay" + specDayCount;
					String chkSpecDay = "chkSpecDay" + specDayCount;
					String txtPropVal = "txtPropVal" + specDayCount;
					String chkDelProp = "chkDelProp" + specDayCount;

				%>

				
				<wb:set id="specDayName"><wb:getDataFieldValue name="SPECDAY_NAME"/></wb:set>
				
				<INPUT TYPE="hidden" NAME="specDayName<%=specDayCount%>" VALUE="<%=specDayName%>">
					<wb:set id="lnkShowSpecDaySQL">
					SELECT SPECDAY_ID, SPECDAY_NAME, SKDGRP_NAME  
					FROM SO_FCAST_SPEC_DAY SD, SO_SCHEDULE_GROUP SG  
					WHERE SD.SKDGRP_ID IN ( <%=locList%> )
					AND SD.SKDGRP_ID = SG.SKDGRP_ID 
					AND SPECDAY_NAME = \'<%=specDayName%>\'
					</wb:set>
					<tr > 
						<td>
						</td> 
						<td class="subSection"><wb:get id="specDayName"/></td> 
						<td width='600'>
							<div class="labeled">
								<wb:controlField 
									id="<%=lnkShowSpecDay%>" 
									submitName="<%=lnkShowSpecDay%>"
									ui='DBLookupUI' 
									uiParameter="source='#lnkShowSpecDaySQL#' sourceType='SQL' labelFieldStatus='hidden'" 
									cssClass="inputField"
									/>
							</div>

							<div class="labeled">
							<wb:controlField id="<%=chkSpecDay%>" submitName="<%=chkSpecDay%>" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
							</div>

					</tr> 
		<% specDayCount++; %>
		</wb:forEachRow>
	</wb:dataSet> 
	<INPUT TYPE="hidden" name="specDayCount" value="<%=specDayCount%>">
</wba:table>


	<wba:table class="contentTable form"> 
		<tr> 
		<td>
		</td>
		<td colspan="5"> 

			<div class="submitaction"> 
			<wba:button name="btnAddSpecDay" label="Add" labelLocalizeIndex="Add" onClick="add(this);"></wba:button>
			<wba:button name="btnEditSpecDay" label="Edit" labelLocalizeIndex="Edit" onClick="edit(this);"></wba:button>
			<wba:button name="btnDeleteSpecDay" label="Delete" labelLocalizeIndex="Delete" onClick="deleteSpecialDay(this);"></wba:button>
			</div>
		</td>
		</tr>
	</wba:table>
	<INPUT TYPE="hidden" name="specDayNameIn">
	<INPUT TYPE="hidden" name="distNameIn">
	<INPUT TYPE="hidden" name="actionType">
	<INPUT TYPE="hidden" name="process" value="N">
	<INPUT TYPE="hidden" name="selected_skdgrp_list" value="<%=locList%>">

<%
if ( deptSearch.equals("false") ) {
%>

	<wba:table class="contentTable form">
	<tr> 
		<td>
		</td> 
		<th><wb:localize id='Distribution from Historical Results'>Distribution from Historical Results</wb:localize></th> 
		<td></td>
	</tr>


	<% int distCount = 1; %>
	<wb:define id="distId, distName, lnkShowDistSQL"/>
	<wb:dataSet id="distinctDistributionDataSet" dataSource="distinctDistributionSQL">

		<wb:forEachRow dataSet="distinctDistributionDataSet">

				<%	
			
					String lnkShowDist = "lnkDistrib" + distCount;
					String chkDist = "chkDistrib" + distCount;

				%>

				
				<wb:set id="distName"><wb:getDataFieldValue name="dist_NAME"/></wb:set>
				
				<INPUT TYPE="hidden" NAME="distName<%=distCount%>" VALUE="<%=distName%>">
					<wb:set id="lnkShowDistSQL">
					SELECT D.DIST_ID, DIST_NAME, SKDGRP_NAME  
					FROM SO_DISTRIBUTION D, SO_SCHEDULE_GROUP SG
					WHERE D.SKDGRP_ID = SG.SKDGRP_ID
					AND SG.SKDGRP_ID IN ( <%=locList%> )
					AND DIST_NAME=\'<%=distName%>\'
					</wb:set>
					<tr > 
						<td>
						</td> 
						<td class="subSection"><wb:get id="distName"/></td> 
						<td width='600'>
							<div class="labeled">
								<wb:controlField 
									id="<%=lnkShowDist%>" 
									submitName="<%=lnkShowDist%>"
									ui='DBLookupUI' 
									uiParameter="source='#lnkShowDistSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
									cssClass="inputField"
									/>
							</div>

							<div class="labeled">
							<wb:controlField id="<%=chkDist%>" submitName="<%=chkDist%>" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
							</div>

					</tr> 
		<% distCount++; %>
		</wb:forEachRow>
	</wb:dataSet> 
	<INPUT TYPE="hidden" name="distCount" value="<%=distCount%>">



	<tr> 
		<td>
		</td>
		<td colspan="5"> 

			<div class="submitaction"> 
			<wba:button name="btnAddDistrib" label="Add" labelLocalizeIndex="Add" onClick="addDistrib(this);"></wba:button>
			<wba:button name="btnEditDistrib" label="Edit" labelLocalizeIndex="Edit" onClick="editDistrib(this);"></wba:button>
			<wba:button name="btnDeleteDistrib" label="Delete" labelLocalizeIndex="Delete" onClick="deleteDistribution(this);"></wba:button>
			</div>
		</td>
		</tr>
		<tr> 
		<td>
		</td>
		<td colspan="5"> 
			<div class="submitaction"> 
			<wba:button name="btnDone" label="Done" labelLocalizeIndex="Done" onClick="history.back();"></wba:button>
			</div>
		</td>
		</tr>


	</wba:table>

<%
	}
%>

<script type='text/javascript'>


function add(thisForm) {

	//alert(document.location.href);
	document.getElementsByName('actionType')[0].value = 'ADD';
	document.forms[0].action="./specialDaysEdit.jsp";
	document.forms[0].submit();

}

function edit(thisForm) {

	document.getElementsByName('actionType')[0].value = 'EDIT';
	var specDayCount = document.getElementsByName('specDayCount')[0].value;
	var checked = 0;
	var nameSelected;

	for (var i=1; i<specDayCount;i++ ) {
		var chkboxVal = document.getElementsByName('chkSpecDay'+i)[0].value;
		var specDayName = document.getElementsByName('specDayName'+i)[0].value;
		
		if ( chkboxVal == 'Y') {
			document.getElementsByName('specDayNameIn')[0].value = specDayName;
			nameSelected = specDayName;
			checked++
		}

	}

		if (checked==0) {
			alert('At least one checkbox should be selected for editing.');
			return false;
		} else if (checked>1) {
			alert('Only one checkbox should be selected for editing.');
			return false;
		}

	document.forms[0].action="./specialDaysEdit.jsp";
	document.forms[0].submit();
	
}

function deleteSpecialDay(thisForm) {

	//alert(document.location.href);
	document.getElementsByName('actionType')[0].value = 'specDayDELETE';
	//document.getElementsByName('process')[0].value = 'Y';
	document.forms[0].submit();
	

}

function deleteDistribution(thisForm) {

	//alert(document.location.href);
	document.getElementsByName('actionType')[0].value = 'distribDELETE';
	//document.getElementsByName('process')[0].value = 'Y';
	document.forms[0].submit();
	

}

function addDistrib(thisForm) {

	//alert(document.location.href);
	document.getElementsByName('actionType')[0].value = 'DIST_ADD';
	document.forms[0].action="./distributionEdit.jsp";
	document.forms[0].submit();

}

function editDistrib(thisForm) {

	document.getElementsByName('actionType')[0].value = 'DIST_EDIT';
	var distCount = document.getElementsByName('distCount')[0].value;
	var checked = 0;

	for (var i=1; i<distCount;i++ ) {
		var chkboxVal = document.getElementsByName('chkDistrib'+i)[0].value;
		var distName = document.getElementsByName('distName'+i)[0].value;
		
		if ( chkboxVal == 'Y') {
			document.getElementsByName('distNameIn')[0].value = distName;
			checked++
		}

	}

		if (checked==0) {
			alert('At least one checkbox should be selected for editing.');
			return false;
		} else if (checked>1) {
			alert('Only one checkbox should be selected for editing.');
			return false;
		}

		

	
	document.forms[0].action="./distributionEdit.jsp";
	document.forms[0].submit();
	
}


</script>

</wb:page>