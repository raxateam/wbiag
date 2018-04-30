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
	height: 35px;
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
	   <wb:define id="client_type_id"><wb:get id="client_type_id" scope="parameter" default=''/></wb:define>
   <wb:submit id="client_type_id"><wb:get id="client_type_id"/></wb:submit>
<%
String toProcessRequest = request.getParameter("process");
String mfrmId = request.getParameter("mfrm_id");
String cancelUrl =  "location.href = '" + request.getContextPath() + "/maintenance/mntForms.jsp?mfrm_id=" + mfrmId + "'; return false;";

	if ("Y".equals(toProcessRequest)) {
		
		System.out.println("TESTTETSET");
		DBConnection conn = JSPHelper.getConnection(request);

		MCEConfiguration mceConfig = new MCEConfiguration(request);
		mceConfig.processSpecialDays(conn, request);

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
System.out.println("locList    " + locList );
String specDayNameIn = request.getParameter("specDayNameIn");
String specDayNameInQuoted = "'"+specDayNameIn+"'";
String actionType = request.getParameter("actionType");

	String STORE_CAPTION = null;
	String DEPT_CAPTION = null;

String configCaption = null;

if ( "EDIT".equals(actionType) ) {
	STORE_CAPTION = "Edit Special Days";
	DEPT_CAPTION = "Edit Special Days";

} else {
	STORE_CAPTION = "Add Special Days";
	DEPT_CAPTION = "Add Special Days";
} 

	if ( !deptSearch.equals("true") ) {
		
		configCaption = STORE_CAPTION;

	} else  {

		configCaption = DEPT_CAPTION;

	}


%>


	<wb:define id="formCaption"/>
	<wb:set id="formCaption"><%=configCaption%></wb:set>
	<INPUT TYPE="hidden" name="actionType" value="<%=actionType%>">
	<INPUT TYPE="hidden" name="specDayNameIn" value="<%=specDayNameIn%>">
	<INPUT TYPE="hidden" name="locList" value="<%=locList%>">
	<INPUT TYPE="hidden" name="userName" value="<%=userName%>">
	<INPUT TYPE="hidden" name="actualUserName" value="<%=actualUserName%>">


	<wb:define id="lnkShowDistSQL">
	SELECT DIST_ID, DIST_NAME  FROM SO_DISTRIBUTION 
	</wb:define>


	<wb:sql createDataSource="distinctSpecDayOccurSQL">
	SELECT DISTINCT(SPECDET_DATE)
    FROM SO_FCAST_SPEC_DAY SD, SO_SCHEDULE_GROUP SG, SO_FCAST_SPEC_DET SDD
	WHERE SD.SKDGRP_ID IN ( <%=locList%> ) 
    AND SD.SKDGRP_ID = SG.SKDGRP_ID 
    AND SDD.SPECDAY_ID = SD.SPECDAY_ID
	AND SPECDAY_NAME = <%=specDayNameInQuoted%>
	</wb:sql>



	<wba:table class="contentTable form" caption='#formCaption#' captionLocalizeIndex='Table Caption'> 



	<tr > 
		<td>
			<wb:controlField id="chkSection1" submitName="chkSection1" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<td class="subSection">Name</td> 
		<td>
		<div class="labeled">
		<wb:controlField id="txtName" submitName="txtName" ui='StringUI' cssClass="inputField" uiParameter="alternateField='true'"><%=specDayNameIn.trim()%></wb:controlField>
		</div>
		</td>
	</tr>
	<tr > 
		<td>
		</td> 
		<td class="subSection">Description</td> 
		<td>
		<div class="labeled">
		<wb:controlField id="txtDescr" submitName="txtDescr" ui='StringUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		</td>
	</tr>
	<tr > 
		<td>
		</td> 
		<td class="subSection">Distribution</td> 
		<td>
		<div class="labeled">
			<wb:controlField 
				id="dblDistribution" 
				submitName="dblDistribution"
				ui='DBListBoxUI' 
				uiParameter="source='#lnkShowDistSQL#' sourceType='SQL'" 
				cssClass="inputField"
				/>
		</div>
		</td>
	</tr>

</wba:table>
	<wba:table class="contentTable form">
	<tr> 
		<td>
			<wb:controlField id="chkSection2" submitName="chkSection2" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<td class="subSection">Special Days Skip</td> 
		<td width='600'>
			<div class="labeled">
			<wb:controlField id="chkSkip" submitName="chkSkip" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
			</div>
		</td> 
	</tr> 
</wba:table>
<wba:table class="contentTable form">
	<tr> 
		<td>
			<wb:controlField id="chkSection3" submitName="chkSection3" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Special Days Type'>Special Days Type</wb:localize>
	</tr> 
	<tr > 
		<td>
		</td> 
		<td class="subSection"></td> 
		<td>
		<div class="labeled">
			<INPUT TYPE="radio" NAME="chkSpecDayType" VALUE="1" checked>Forecast from all special days in the historical period<br>
			<INPUT TYPE="radio" NAME="chkSpecDayType" VALUE="2">Forecast from all special days in the historical period which fall on the same day of the week<br>
			<INPUT TYPE="radio" NAME="chkSpecDayType" VALUE="3">Adjust calculated forecast by percentage:&nbsp;<wb:controlField id="txtPercent" submitName="txtPercent" ui='PercentEditUI' cssClass="inputField" uiParameter="alternateField='true' precision='2'"/>

		</td>
	</tr>
	</wba:table>

<wba:table class="contentTable form">
	<tr> 
		<td>
			<wb:controlField id="chkSection4" submitName="chkSection4" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Special Days Occurrences'>Special Days Occurrences</wb:localize>
	</tr> 

	<% int specDayOccurCount = 1; %>
	<wb:define id="specDayId, specDayName, specDayDate, lnkShowSpecDayOccurSQL"/>
	<wb:dataSet id="distinctSpecDayOccurDataSet" dataSource="distinctSpecDayOccurSQL" orderBy="SPECDET_DATE">

		<wb:forEachRow dataSet="distinctSpecDayOccurDataSet">

				<%	
			
					String lnkShowSpecDay = "lnkShowSpecDayOccur" + specDayOccurCount;
					String txtDate = "txtDate" + specDayOccurCount;
					String inkDate = "inkDate" + specDayOccurCount;
					String chkDate = "chkDate" + specDayOccurCount;
					String delDate = "dateToDelete" + specDayOccurCount;

				%>

				
				<wb:set id="specDayDate"><wb:getDataFieldValue name="SPECDET_DATE"/></wb:set>
				
				<INPUT TYPE="hidden" NAME="specDayName<%=specDayOccurCount%>" VALUE="<%=specDayName%>">
					<wb:set id="lnkShowSpecDayOccurSQL">
					SELECT SD.SPECDAY_ID, SPECDET_ID, SPECDAY_NAME, SKDGRP_NAME , SPECDET_DATE
					FROM SO_FCAST_SPEC_DAY SD, SO_SCHEDULE_GROUP SG, SO_FCAST_SPEC_DET SDD
					WHERE SD.SKDGRP_ID IN ( <%=locList%> )
					AND SD.SKDGRP_ID = SG.SKDGRP_ID 
					AND SDD.SPECDAY_ID = SD.SPECDAY_ID
					AND SPECDET_DATE  = TO_DATE(\'<%=specDayDate%>\',\'YYYYMMDD HH24MiSS\')
					</wb:set>
					<tr > 
						<td>
						</td> 
						<td class="subSection">
							<wb:get id="specDayDate"/>
							<INPUT TYPE="hidden" name="<%=delDate%>" value="<%=specDayDate%>">
						</td> 
						<td width='600'>
							<div class="labeled">
								<wb:controlField 
									id="<%=inkDate%>" 
									submitName="<%=inkDate%>"
									ui='DBLookupUI' 
									uiParameter="source='#lnkShowSpecDayOccurSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
									cssClass="inputField"
									/>
							</div>

							<div class="labeled">
							<wb:controlField id="<%=chkDate%>" submitName="<%=chkDate%>" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
							</div>

					</tr> 
		<% specDayOccurCount++; %>
		</wb:forEachRow>
	</wb:dataSet> 
	<INPUT TYPE="hidden" name="specDayOccurCount" value="<%=specDayOccurCount%>">

	<tr>
		<td></td> 
		<td class="subSection">Add</td> 
		<td>
			<div class="labeled">
				<wb:controlField id="txtNewDate1" submitName="txtNewDate1" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
			</div><br><br>
			<div class="labeled">
				<wb:controlField id="txtNewDate2" submitName="txtNewDate2" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
			</div><br><br>
			<div class="labeled">
				<wb:controlField id="txtNewDate3" submitName="txtNewDate3" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
			</div><br><br>
			<div class="labeled">
				<wb:controlField id="txtNewDate4" submitName="txtNewDate4" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
			</div><br><br>
			<div class="labeled">
				<wb:controlField id="txtNewDate5" submitName="txtNewDate5" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
			</div>
		</td>
		</tr>
	</wba:table>

	<wba:table class="contentTable form">
		<tr> 
		<td>
		</td>
		<td colspan="5"> 

			<div class="submitaction"> 
			<wba:button name="btnApply" label="Apply" labelLocalizeIndex="Apply" onClick="apply(this);"></wba:button>
			<wba:button name="btnCancel" label="Cancel" labelLocalizeIndex="Cancel" onClick='<%=cancelUrl%>'></wba:button>
			<wba:button name="btnBack" label="Back" labelLocalizeIndex="Back" onClick="history.back();"></wba:button>
			</div>
		</td>
		</tr>
	</wba:table>
		



<INPUT TYPE="hidden" name="process" value="N">

<script type='text/javascript'>


function apply(thisForm) {

	if (validate()) {
		document.getElementsByName('process')[0].value = 'Y';
		document.forms[0].submit()
	}

}

function validate() {
     
	//ensure atleast one section is checked
	var passed = true;
	var oneChecked = false;
	var startIndex = 1;
	var numChkBoxes = 4;

	for (i=startIndex;i<=numChkBoxes;i++)
	{		
		if(document.page_form.elements["chkSection"+i+"_dummy"].checked){
		
			oneChecked = true;
			//alert("element " + i + " is checked");
		}
	}
	if (!oneChecked){
		   alert("Please specify at least one change or click Cancel");
		
	}

	passed = (passed && oneChecked);

	return passed;
}

</script>

</wb:page>