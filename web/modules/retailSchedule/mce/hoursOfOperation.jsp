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
	   <wb:define id="client_type_id"><wb:get id="client_type_id" scope="parameter" default=''/></wb:define>
	   <wb:submit id="client_type_id"><wb:get id="client_type_id"/></wb:submit>
<%
String toProcessRequest = request.getParameter("process");
String mfrmId = request.getParameter("mfrm_id");
String cancelUrl =  "location.href = '" + request.getContextPath() + "/maintenance/mntForms.jsp?mfrm_id=" + mfrmId + "'; return false;";

	if ("Y".equals(toProcessRequest)) {
		DBConnection conn = JSPHelper.getConnection(request);

		MCEConfiguration mceConfig = new MCEConfiguration(request);
		mceConfig.processHoursOfOperation(conn, request);
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

final String STORE_CAPTION = "Store Hours of Operation";
final String DEPT_CAPTION = "Department Hours of Operation";
String configCaption = null;

	if ( !deptSearch.equals("true") ) {
		
		configCaption = STORE_CAPTION;

	} else  {

		configCaption = DEPT_CAPTION;

	}

String sqlString = "";
%>


	<wb:define id="formCaption, propId, propName, lnkShowPropSQL"/>
	<wb:set id="formCaption"><%=configCaption%></wb:set>
	<INPUT TYPE="hidden" name="locList" value="<%=locList%>">
	<INPUT TYPE="hidden" name="dept_Search" value="<%=deptSearch%>">
    <INPUT TYPE="hidden" name="userName" value="<%=userName%>">
	<INPUT TYPE="hidden" name="actualUserName" value="<%=actualUserName%>">

	<wb:define id="lnkShowHourTypeSQL">
	SELECT SKDGRP_ID, SKDGRP_NAME, SKDGRP_HRS_OP_SRC  
	FROM SO_SCHEDULE_GROUP 
	WHERE SKDGRP_ID IN (<%=locList%>)
	</wb:define>

	<wb:define id="lnkShowDefaultHoursSQL">
	SELECT HRSOPD_ID, SKDGRP_NAME, HRSOPD_DAY, TO_CHAR(HRSOPD_OPEN_TIME, \'HH24:Mi\'), TO_CHAR(HRSOPD_CLOSE_TIME, \'HH24:Mi\')
	FROM WORKBRAIN_TEAM WBT, SO_SCHEDULE_GROUP SG, HRSOP_DAY HRSOPD
	WHERE WBT.WBT_ID = SG.WBT_ID AND
	WBT.HRSOP_ID = HRSOPD.HRSOP_ID AND
	SKDGRP_ID IN (<%=locList%>)
	ORDER BY SKDGRP_NAME, HRSOPD_DAY
	</wb:define>

	<wb:define id="lnkShowEffectiveDatedHoursSQL">
	SELECT SG.SKDGRP_ID, SG.SKDGRP_NAME, CORPENTHR_FROMDATE, CORPENTHR_TODATE 
	FROM SO_CORP_ENT_HOUR HRS, SO_SCHEDULE_GROUP SG
	WHERE SG.SKDGRP_ID = HRS.SKDGRP_ID 
	AND SG.SKDGRP_ID IN ( <%=locList%> )
	</wb:define>



	<wba:table class="contentTable form" caption='#formCaption#' captionLocalizeIndex='Table Caption'> 

<%
if ( deptSearch.equals("true") ) {
%>


		<tr> 
			<td>
				<wb:controlField id="chkSection1" submitName="chkSection1" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
			</td> 
			<th><wb:localize id='Department Hours Source'>Department Hours Source</wb:localize></th> 
			<td width='600'>
			<div class="labeled">
			<wb:controlField 
					id="lnkShowHourType" 
					submitName="lnkShowHourType" 
					ui='DBLookupUI' 
					uiParameter="source='#lnkShowHourTypeSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
					cssClass="inputField"
					/>
					</div>
			</td> 
		</tr> 
	<tr > 
		<td>
		</td> 
		<td class="subSection"></td> 
		<td>
		<div class="labeled">
		<INPUT TYPE="radio" NAME="chkHourType" value="chkHourTypeInherit">
		</div>
		<div class="labeled">
		<wb:localize id='Same as Store Location' escapeForJavascript="true">
		Same as Store Location
		</wb:localize><p>
		<wb:localize id='Offset from parent location hours' escapeForJavascript="true">
		<U>Offset from parent location hours:</U>
		</wb:localize><p>
		Open&nbsp;&nbsp;<wb:controlField id="txtOffsetStart" submitName="txtOffsetStart" ui='NumberUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		hours
			<wb:controlField 
				id="ddlOffsetStart" 
				submitName="ddlOffsetStart"
				ui='ComboBoxUI' 
				uiParameter="valueList='earlier,later' labelList='earlier,later' labelFieldStatus='default'" 
				cssClass="inputField"
				/>
		than store location <br>
		Close&nbsp;&nbsp;<wb:controlField id="txtOffsetEnd" submitName="txtOffsetEnd" ui='NumberUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		hours
			<wb:controlField 
				id="ddlOffsetEnd" 
				submitName="ddlOffsetEnd"
				ui='ComboBoxUI' 
				uiParameter="valueList='earlier,later' labelList='earlier,later' labelFieldStatus='default'" 
				cssClass="inputField"
				/>
		than store location
		<p>
		<wb:localize id='Staff Offset' escapeForJavascript="true">
		<U>Staff Offset:</U>
		</wb:localize><p>
		Open&nbsp;&nbsp;<wb:controlField id="txtStaffOffsetStart" submitName="txtStaffOffsetStart" ui='NumberUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		hours earlier than store location <br>
		Close&nbsp;&nbsp;<wb:controlField id="txtStaffOffsetEnd" submitName="txtStaffOffsetEnd" ui='NumberUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		hours later than store location
		</div>
		</td>
	</tr>

	<tr > 
		<td>
		</td> 
		<td class="subSection"></td> 
		<td>
			<div class="labeled">
			<INPUT TYPE="radio" NAME="chkHourType" value="chkHourTypeDefaultSets">
			</div>
			<div class="labeled">
			<wb:localize id='Select Defined Sets' escapeForJavascript="true">
			Select Defined Sets (uses hours defined below)
			</wb:localize><p>
			</div>
		</td>
	</tr>



<%
	}
%>
</wba:table>
	<wba:table class="contentTable form">
	<tr> 
		<td>
			<wb:controlField id="chkSection2" submitName="chkSection2" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Default Hours'>Default Hours</wb:localize></th> 
		<td width='600'>
		<div class="labeled">
		<wb:controlField 
				id="lnkShowDefaultHours" 
				submitName="lnkShowDefaultHours" 
				ui='DBLookupUI' 
				uiParameter="source='#lnkShowDefaultHoursSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
				cssClass="inputField"
				/>
				</div>
		</td> 
	</tr> 
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Open'>Open</wb:localize></td> 
		<td>
		<div class="labeled"><B>Sun</B>
		<wb:controlField id="txtDefHoursSunOpen" submitName="txtDefHoursSunOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Mon</B>
		<wb:controlField id="txtDefHoursMonOpen" submitName="txtDefHoursMonOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Tue</B>
		<wb:controlField id="txtDefHoursTueOpen" submitName="txtDefHoursTueOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Wed</B>
		<wb:controlField id="txtDefHoursWedOpen" submitName="txtDefHoursWedOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Thu</B>
		<wb:controlField id="txtDefHoursThuOpen" submitName="txtDefHoursThuOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Fri</B>
		<wb:controlField id="txtDefHoursFriOpen" submitName="txtDefHoursFriOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Sat</B>
		<wb:controlField id="txtDefHoursSatOpen" submitName="txtDefHoursSatOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>

		</td>
	</tr>

	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Close'>Close</wb:localize></td> 
		<td>
		<div class="labeled">
		<wb:controlField id="txtDefHoursSunClose" submitName="txtDefHoursSunClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtDefHoursMonClose" submitName="txtDefHoursMonClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtDefHoursTueClose" submitName="txtDefHoursTueClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtDefHoursWedClose" submitName="txtDefHoursWedClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtDefHoursThuClose" submitName="txtDefHoursThuClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtDefHoursFriClose" submitName="txtDefHoursFriClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtDefHoursSatClose" submitName="txtDefHoursSatClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>

		</td>
	</tr>


	</wba:table>

	<wba:table class="contentTable form"> 

	<tr> 
		<td>
			<wb:controlField id="chkSection3" submitName="chkSection3" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Date-effective Hours'>Date-effective Hours</wb:localize>
		</th> 


		<td width='600'>
		<div class="labeled">
		From <wb:controlField id="txtEffDateStart" submitName="txtEffDateStart" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		To <wb:controlField id="txtEffDateEnd" submitName="txtEffDateEnd" ui='DatePickerUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField 
				id="lnkShowEffectiveDatedHours" 
				submitName="lnkShowEffectiveDatedHours" 
				ui='DBLookupUI' 
				uiParameter="source='#lnkShowEffectiveDatedHoursSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
				cssClass="inputField"
				/>
				</div>
		</td> 
	</tr> 

	<tr > 
		<td>
		</td> 
		<td class="subSection"></td> 
		<td>
		<div class="labeled">
		<INPUT TYPE="radio" NAME="chkEffDate" VALUE="chkAdd">
		</div>
		<div class="labeled">
		<wb:localize id='Add Date Message' escapeForJavascript="true">
		Add/overwrite date-effective hours <I>(leave hours empty to indicate store closing)</I>.
		</wb:localize>
		</div>
		</td>
	</tr>

	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Open'>Open</wb:localize></td> 
		<td>
		<div class="labeled"><B>Sun</B>
		<wb:controlField id="txtEffDateHoursSunOpen" submitName="txtEffDateHoursSunOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Mon</B>
		<wb:controlField id="txtEffDateHoursMonOpen" submitName="txtEffDateHoursMonOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Tue</B>
		<wb:controlField id="txtEffDateHoursTueOpen" submitName="txtEffDateHoursTueOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Wed</B>
		<wb:controlField id="txtEffDateHoursWedOpen" submitName="txtEffDateHoursWedOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Thu</B>
		<wb:controlField id="txtEffDateHoursThuOpen" submitName="txtEffDateHoursThuOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Fri</B>
		<wb:controlField id="txtEffDateHoursFriOpen" submitName="txtEffDateHoursFriOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled"><B>Sat</B>
		<wb:controlField id="txtEffDateHoursSatOpen" submitName="txtEffDateHoursSatOpen" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>

		</td>
	</tr>

	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Close'>Close</wb:localize></td> 
		<td>
		<div class="labeled">
		<wb:controlField id="txtEffDateHoursSunClose" submitName="txtEffDateHoursSunClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtEffDateHoursMonClose" submitName="txtEffDateHoursMonClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtEffDateHoursTueClose" submitName="txtEffDateHoursTueClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtEffDateHoursWedClose" submitName="txtEffDateHoursWedClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtEffDateHoursThuClose" submitName="txtEffDateHoursThuClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtEffDateHoursFriClose" submitName="txtEffDateHoursFriClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled">
		<wb:controlField id="txtEffDateHoursSatClose" submitName="txtEffDateHoursSatClose" ui='TimeEditUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>

		</td>
	</tr>
	<tr > 
		<td>
		</td> 
		<td class="subSection"></td> 
		<td>
		<div class="labeled">
		<INPUT TYPE="radio" NAME="chkEffDate" VALUE="chkDelete">
		</div>
		<div class="labeled">
			<img src='<%= request.getContextPath() %>/images/interface/icon_head_trash.gif' border='0'>
		</div>
		<div class="labeled">
		<wb:localize id='Delete Date Message' escapeForJavascript="true">
		Delete date-effective hours for the specified date range.
		</wb:localize>
		</div>
		</td>
	</tr>
		<tr> 
		<td>
		</td>
		<td colspan="5"> 

			<div class="submitaction"> 
			<wba:button name="btnApply" label="Apply" labelLocalizeIndex="Apply" onClick="apply(this);"></wba:button>
			<wba:button name="btnCancel" label="Cancel" labelLocalizeIndex="Cancel" onClick='<%=cancelUrl%>'></wba:button>
			<wba:button name="btnBack" label="Back" labelLocalizeIndex="Back" onClick="history.back()"></wba:button>
			</div>
		</td>
		</tr>
	</wba:table>

	
<p>



<INPUT TYPE="hidden" name="process" value="N">

<script type='text/javascript'>


function apply(thisForm) {

	if (validate(thisForm)){
		//alert(document.location.href);
		document.getElementsByName('process')[0].value = 'Y';
		document.forms[0].submit()
	}

}

function validate(thisForm) {
     
	var daysArray = new Array();
	daysArray[0] = 'Sun';daysArray[1] = 'Mon';daysArray[2] = 'Tue';daysArray[3] = 'Wed';daysArray[4] = 'Thu';daysArray[5] = 'Fri';daysArray[6] = 'Sat';

	//ensure atleast one section is checked
	var passed = true;
	var oneChecked = false;
	var startIndex = 2;
	var numChkBoxes = 3;
	var deptSearch = document.getElementsByName('dept_Search')[0].value;

	if (deptSearch=='true') {
		startIndex = 1;
	}

	for (i=startIndex;i<=numChkBoxes;i++)
	{		
		//alert('i' + i);
		if(document.page_form.elements["chkSection"+i+"_dummy"].checked){
		
			oneChecked = true;
			//alert("element " + i + " is checked");
		}
	}
	if (!oneChecked){
		   alert("Please specify at least one change or click Cancel");
		
	}

<%
	if (deptSearch.equals("true")) {
%>
	//Validate Department Hours Source
	if( document.page_form.elements["chkSection1_dummy"].checked ) {

		//alert (document.getElementsByName('chkHourType')[0].checked);

		if ( document.getElementsByName('chkHourType')[0].checked == false &&
				document.getElementsByName('chkHourType')[1].checked == false ) {
			alert('Please specify the Department Hours Source option.');
			return false;
		}

		if ( document.getElementsByName('chkHourType')[0].checked == true ) {

			if ( document.getElementsByName('txtOffsetStart')[0].value == "") {
				alert('Please specify the openning hours for \'Offset from parent location hours\'.');
				return false;
			}

			if ( document.getElementsByName('txtOffsetStart')[0].value != "" && 
				document.getElementsByName('txtOffsetStart')[0].value < 0) {
				alert('Hours must be a positive number.');
				return false;
			}

			if ( document.getElementsByName('txtOffsetEnd')[0].value == "") {
				alert('Please specify the closing hours for \'Offset from parent location hours\'.');
				return false;
			}

			if ( document.getElementsByName('txtOffsetEnd')[0].value != "" && 
				document.getElementsByName('txtOffsetEnd')[0].value < 0) {
				alert('Hours must be a positive number.');
				return false;
			}

			if ( document.getElementsByName('ddlOffsetStart')[0].value == "") {
				alert('Please specify the openning hours earlier/later than store location.');
				return false;
			}

			if ( document.getElementsByName('ddlOffsetEnd')[0].value == "") {
				alert('Please specify the closing hours earlier/later than store location.');
				return false;
			}

			if ( document.getElementsByName('txtStaffOffsetStart')[0].value == "") {
				alert('Please specify the openning hours for \'Staff Offset\'.');
				return false;
			}

			if ( document.getElementsByName('txtStaffOffsetStart')[0].value != "" && 
				document.getElementsByName('txtStaffOffsetStart')[0].value < 0) {
				alert('Hours must be a positive number.');
				return false;
			}

			if ( document.getElementsByName('txtStaffOffsetEnd')[0].value == "") {
				alert('Please specify the closing hours for \'Staff Offset\'.');
				return false;
			}

			if ( document.getElementsByName('txtStaffOffsetEnd')[0].value != "" && 
				document.getElementsByName('txtStaffOffsetEnd')[0].value < 0) {
				alert('Hours must be a positive number.');
				return false;
			}

		}

	}
<%
}
%>


	//Validate Defaults Hours section if checked
	if( document.page_form.elements["chkSection2_dummy"].checked ) {
		for (i=0; i<7; i++) {
			var openHr = document.page_form.elements["txtDefHours"+daysArray[i]+"Open"].value;
			var closeHr = document.page_form.elements["txtDefHours"+daysArray[i]+"Close"].value;

			if ( openHr!="" && closeHr=="" ) {
				alert("Please enter default closing hour for " + daysArray[i] + ".");
				return false;
			}

			if ( openHr=="" && closeHr!="" ) {
				alert("Please enter default openning hour for " + daysArray[i] + ".");
				return false;
			}

		}
	}


	//Validate Date-effective Hours section if checked
	if( document.page_form.elements["chkSection3_dummy"].checked ) {
		
		//Ensure start and end dates are filled in
		if(document.page_form.elements["txtEffDateStart"].value == "" || 
				document.page_form.elements["txtEffDateEnd"].value == "") {
			
			alert("From Date and End Date must be specified.");
			return false;

		}

		for (i=0; i<7; i++) {
			var openHr = document.page_form.elements["txtEffDateHours"+daysArray[i]+"Open"].value;
			var closeHr = document.page_form.elements["txtEffDateHours"+daysArray[i]+"Close"].value;

			if ( openHr!="" && closeHr=="" ) {
				alert("Please enter date-effective closing hour for " + daysArray[i] + ".");
				return false;
			}

			if ( openHr=="" && closeHr!="" ) {
				alert("Please enter date-effective openning hour for " + daysArray[i] + ".");
				return false;
			}

		}

	}

	passed = (passed && oneChecked);

	return passed;
}


</script>

</wb:page>