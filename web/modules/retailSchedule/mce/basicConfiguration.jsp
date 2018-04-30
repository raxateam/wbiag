<%@ page import="java.util.*"%> 
<%@ page import="java.sql.*"%> 
<%@ page import="java.text.SimpleDateFormat"%> 
<%@ page import="com.workbrain.util.*" %> 
<%@ page import="com.workbrain.sql.*"%> 
<%@ page import="com.workbrain.server.jsp.*"%> 
<%@ page import="com.workbrain.server.WebLogin"%> 
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
<wb:page login="true" popupPage="false" subsidiaryPage="false" title="Basic Configuration" submitMethod="post"> 
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
<script type='text/javascript' src="<%= request.getContextPath() %>/modules/retailSchedule/mce/validate.js"></script>
<%

String toProcessRequest = request.getParameter("process");
String mfrmId = request.getParameter("mfrm_id");
String cancelUrl =  "location.href = '" + request.getContextPath() + "/maintenance/mntForms.jsp?mfrm_id=" + mfrmId + "'; return false;";

	if ("Y".equals(toProcessRequest)) {

		DBConnection conn = JSPHelper.getConnection(request);
		MCEConfiguration mceConfig = new MCEConfiguration(request);
		mceConfig.processBasicConfig(conn, request);

		%>
		<script>
		document.forms[0].action = '<%=request.getContextPath()%>' + "/modules/retailSchedule/mce/usageMode.jsp";
		document.forms[0].submit();
		</script>
		<%


	}

	String userName = JSPHelper.getWebLogin(request).getUserName();
	String actualUserName = JSPHelper.getWebLogin(request).getActualUserName();
	System.out.println(mfrmId + "userName *************************** " + userName);
		System.out.println("actualUserName *************************** " + actualUserName);

	if ( "Y".equals(request.getParameter("hideLocAdminUserSelect")) && 
			"Y".equals(request.getParameter("hideLocAllUserSelect")) ) {
	
		%>
			update both 
		<%

	} else if ( "Y".equals(request.getParameter("hideLocAdminUserSelect")) ) {

		%>
			update admin
		<%

	} else if ( "Y".equals(request.getParameter("hideLocAllUserSelect")) ) {

		%>
			update all
		<%

	}





//expecting either basicStoreConfiguration or basicDepartmentConfiguration
String locList = request.getParameter("selected_skdgrp_list");
String deptSearch = request.getParameter("dept_search");
System.out.println("deptSearch *************************** " + deptSearch);

final String STORE_CAPTION = "Basic Store Configuration";
final String DEPT_CAPTION = "Basic Department Configuration";
String configCaption = null;

//deptSearch = "true";
//locList = "10061, 10063";

	if ( deptSearch.equals("true") ) {
		
		configCaption = DEPT_CAPTION;

	} else  {

		configCaption = STORE_CAPTION;

	}


%>


	<wb:define id="formCaption, propId, propName, lnkShowPropSQL"/>
	<wb:set id="formCaption"><%=configCaption%></wb:set>
	<INPUT TYPE="hidden" name="locList" value="<%=locList%>">
	<INPUT TYPE="hidden" name="selected_skdgrp_list" value="<%=locList%>">
	<INPUT TYPE="hidden" name="dept_Search" value="<%=deptSearch%>">
	<INPUT TYPE="hidden" name="userName" value="<%=userName%>">
	<INPUT TYPE="hidden" name="actualUserName" value="<%=actualUserName%>">



	<wb:define id="lnkShowAllUserFlagSQL">
	SELECT SKDGRP_ID, SKDGRP_NAME, WBT_FLAG4
	FROM SO_SCHEDULE_GROUP SG, WORKBRAIN_TEAM WBT 
	WHERE SG.WBT_ID = WBT.WBT_ID AND SKDGRP_ID IN (<%=locList%>)
	</wb:define>

	<wb:define id="lnkShowNonAdminUserFlagSQL">
	SELECT SKDGRP_ID, SKDGRP_NAME, WBT_FLAG5
	FROM SO_SCHEDULE_GROUP SG, WORKBRAIN_TEAM WBT 
	WHERE SG.WBT_ID = WBT.WBT_ID AND SKDGRP_ID IN (<%=locList%>)
	</wb:define>

	<wb:define id="lnkShowStorePropertiesSQL">
	SELECT SG.SKDGRP_ID, SG.SKDGRP_NAME, PROP_NAME, LOCPROP_VALUE  
	FROM SO_PROPERTY SP, SO_LOCATION_PROP SLP, SO_SCHEDULE_GROUP SG
	WHERE 
	SP.PROP_ID = SLP.PROP_ID 
	AND SG.SKDGRP_ID = SLP.SKDGRP_ID
	AND SG.SKDGRP_ID IN ( <%=locList%> )
	</wb:define>

	<wb:define id="lnkShowVolDriversSQL">
	SELECT 
	V1.SKDGRP_ID, V1.SKDGRP_NAME AS "Drivers", V2.SKDGRP_NAME AS "Parent Location"
	from VIEW_SO_SCHED_GRP V1, VIEW_SO_SCHED_GRP V2
	WHERE 
	V1.SKDGRP_PARENT_ID = V2.SKDGRP_ID
	AND V1.SKDGRP_PARENT_ID IN ( <%=locList%> )
	AND V1.SKDGRP_INTRNL_TYPE=12
	</wb:define>

	<wb:define id="lnkShowAllGroupsSQL">
	SELECT EMPGRP_ID, EMPGRP_NAME AS "Staff Group", V.SKDGRP_NAME AS "Store"
	FROM SO_EMPLOYEE_GROUP EG, VIEW_SO_SCHED_GRP V
	WHERE V.SKDGRP_ID = EG.SKDGRP_ID AND
	V.SKDGRP_ID IN ( <%=locList%> )
	</wb:define>

	<wb:define id="lnkShowDepartmentsSQL">
	SELECT 
	V1.SKDGRP_ID, V1.SKDGRP_NAME AS "Departments", V2.SKDGRP_NAME AS "Parent Location"
	from VIEW_SO_SCHED_GRP V1, VIEW_SO_SCHED_GRP V2
	WHERE 
	V1.SKDGRP_PARENT_ID = V2.SKDGRP_ID
	AND V1.SKDGRP_PARENT_ID IN ( <%=locList%> )
	AND V1.SKDGRP_INTRNL_TYPE=11
	</wb:define>

	<wb:define id="dblVolDriverSQL">
	SELECT SKDGRP1.SKDGRP_ID, SKDGRP1.SKDGRP_NAME FROM 
	SO_SCHEDULE_GROUP SKDGRP1 WHERE SKDGRP1.SKDGRP_INTRNL_TYPE=12
	</wb:define>

	<wb:define id="dblDepartmentSQL">
	SELECT SKDGRP1.SKDGRP_ID, SKDGRP1.SKDGRP_NAME FROM 
	SO_SCHEDULE_GROUP SKDGRP1 WHERE SKDGRP1.SKDGRP_INTRNL_TYPE=11
	</wb:define>

	<wb:define id="dblNewPropertySQL">
	SELECT PROP_ID, PROP_NAME AS "Propery Name", PROP_DESC AS "Description" FROM SO_PROPERTY
	</wb:define>

	<wb:define id="lnkShowGroupFullTimeSQL">
	SELECT EMPGRP_ID, EMPGRP_NAME AS "Staff Group", V.SKDGRP_NAME AS "Store"
	FROM SO_EMPLOYEE_GROUP EG, VIEW_SO_SCHED_GRP V
	WHERE V.SKDGRP_ID = EG.SKDGRP_ID AND
	EMPGRP_NAME LIKE \'%FULL TIME%\' AND
	EG.SKDGRP_ID IN ( <%=locList%> )
	</wb:define>

	<wb:define id="lnkShowGroupPartTimeSQL">
	SELECT EMPGRP_ID, EMPGRP_NAME AS "Staff Group", V.SKDGRP_NAME AS "Store"
	FROM SO_EMPLOYEE_GROUP EG, VIEW_SO_SCHED_GRP V
	WHERE V.SKDGRP_ID = EG.SKDGRP_ID AND
	EMPGRP_NAME LIKE \'%PART TIME%\' AND
	EG.SKDGRP_ID IN ( <%=locList%> )
	</wb:define>

	<wb:define id="lnkShowGroupMinorSQL">
	SELECT EMPGRP_ID, EMPGRP_NAME AS "Staff Group", V.SKDGRP_NAME AS "Store"
	FROM SO_EMPLOYEE_GROUP EG, VIEW_SO_SCHED_GRP V
	WHERE V.SKDGRP_ID = EG.SKDGRP_ID AND
	EMPGRP_NAME LIKE \'%MINOR%\' AND
	EG.SKDGRP_ID IN ( <%=locList%> )
	</wb:define>

	<wb:define id="dblSchedRulesSQL">
	SELECT rulegrp_id, rulegrp_name, rulegrp_desc FROM SO_RULE_GROUP
	</wb:define>

	<wb:sql createDataSource="distinctStorePropSQL">
	SELECT DISTINCT(PROP_NAME), SP.PROP_ID
	FROM SO_PROPERTY SP, SO_LOCATION_PROP SLP, SO_SCHEDULE_GROUP SG
	WHERE 
	SP.PROP_ID = SLP.PROP_ID 
	AND SG.SKDGRP_ID = SLP.SKDGRP_ID
	AND SG.SKDGRP_ID IN ( <%=locList%> )
	ORDER BY PROP_NAME
	</wb:sql>

	<script type='text/javascript'>
	</script>


	<wba:table class="contentTable form" caption='#formCaption#' captionLocalizeIndex='Table Caption'> 


	<tr> 
		<td>
			<wb:controlField id="chkSection1" submitName="chkSection1" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Hide Location from All Users'>Hide Location from All Users</wb:localize></th> 
		<td width='600'>
		<div class="labeled">
		<wb:controlField 
				id="lnkShowAllUserFlag" 
				submitName="lnkShowAllUserFlag" 
				ui='DBLookupUI' 
				uiParameter="source='#lnkShowAllUserFlagSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
				cssClass="inputField"
				/>
				</div>
		<div class="labeled">
		<wb:controlField id="chkHideForAllUsers" submitName="chkHideForAllUsers" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		</td> 
	</tr> 
	</wba:table>
	<wba:table class="contentTable form">
	<tr > 
		<td>
			<wb:controlField id="chkSection2" submitName="chkSection2" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Hide Location from Admin Users'>Hide Location from Admin Users</wb:localize></th> 
		<td width='600'>
		<div class="labeled">
			<wb:controlField 
				id="lnkShowNonAdminUserFlag" 
				submitName="lnkShowNonAdminUserFlag" 
				ui='DBLookupUI' 
				uiParameter="source='#lnkShowNonAdminUserFlagSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
				cssClass="inputField"
				/>
		</div>
		<div class="labeled">			
		<wb:controlField id="chkHideForNonAdminUsers" submitName='chkHideForNonAdminUsers' ui='CheckBoxUI' cssClass="inputField" uiParameter='alternateField=true'/>
		</div>
		</td> 
	</tr> 
	</wba:table>

<%



if ( deptSearch.equals("false") ) {
%>
<wba:table class="contentTable form">
<tr > 
<td>
	<tr> 
		<td>
			<wb:controlField id="chkSection3" submitName="chkSection3" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Store Properties'>Store Properties</wb:localize></th> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="lnkShowStoreProperties" 
				submitName="lnkShowStoreProperties" 
				ui='DBLookupUI' 
				uiParameter="source='#lnkShowStorePropertiesSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
				cssClass="inputField"
				/>
		</div>
		</td> 
	</tr> 

	<% int propCount = 1; %>
	<wb:dataSet id="distinctStorePropDataSet" dataSource="distinctStorePropSQL">

		<wb:forEachRow dataSet="distinctStorePropDataSet">

				<%	
			
					String lnkShowProp = "lnkShowProp" + propCount;
					//String lnkShowPropSQL = "lnkShowProp" + propCount + "SQL";
					String chkChgPropVal = "chkChgPropVal" + propCount;
					String txtPropVal = "txtPropVal" + propCount;
					String chkDelProp = "chkDelProp" + propCount;

				%>

				<wb:set id="propId"><wb:getDataFieldValue name="PROP_ID"/></wb:set>
				<wb:set id="propName"><wb:getDataFieldValue name="PROP_NAME"/></wb:set>
				<INPUT TYPE="hidden" NAME="propId<%=propCount%>" VALUE="<%=propId%>">
				<INPUT TYPE="hidden" NAME="propName<%=propCount%>" VALUE="<%=propName%>">
					<wb:set id="lnkShowPropSQL">
					SELECT SP.PROP_ID, PROP_NAME, LOCPROP_VALUE, SG.SKDGRP_NAME
					FROM SO_PROPERTY SP, SO_LOCATION_PROP SLP, SO_SCHEDULE_GROUP SG
					WHERE 
					SP.PROP_ID = SLP.PROP_ID 
					AND SG.SKDGRP_ID = SLP.SKDGRP_ID
					AND SG.SKDGRP_ID IN ( <%=locList%> )
					AND SP.PROP_ID = <%=propId%>
					</wb:set>
					<tr > 
						<td>
						</td> 
						<td class="subSection"><wb:get id="propName"/></td> 
						<td width='600'>
							<div class="labeled">
								<wb:controlField 
									id="<%=lnkShowProp%>" 
									submitName="<%=lnkShowProp%>"
									ui='DBLookupUI' 
									uiParameter="source='#lnkShowPropSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
									cssClass="inputField"
									/>
							</div>

							<div class="labeled">
							<wb:controlField id="<%=chkChgPropVal%>" submitName="<%=chkChgPropVal%>" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
							</div>

						<div class="labeled">
							<wb:controlField id="<%=txtPropVal%>" submitName="<%=txtPropVal%>" ui='StringUI' cssClass="inputField" uiParameter="alternateField='true'"/>
						</div> 

						<div class="labeled">
							<wb:controlField id="<%=chkDelProp%>" submitName="<%=chkDelProp%>" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
						</div> 
						<div class="labeled">
							<img src='<%= request.getContextPath() %>/images/interface/icon_head_trash.gif' border='0'>
						</div>
					</tr> 
		<% propCount++; %>
		</wb:forEachRow>
		
	</wb:dataSet> 
	<INPUT TYPE="hidden" name="propCount" value="<%=propCount%>">
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='New Property'>New Property</wb:localize></td> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="dblNewPropertyVal1" 
				submitName="dblNewPropertyVal1" 
				ui='DBLookupUI' 
				uiParameter="source='#dblNewPropertySQL#' sourceType='SQL' labelFieldStatus='default' width='12'" 
				cssClass="inputField"
				/>
		</div>
		<div class="labeled">			
		<wb:controlField id="txtNewPropVal1" submitName="txtNewPropVal1" ui='StringUI' cssClass="inputField" uiParameter="alternateField='true' width='12'"/>
		</div>
		<div class="labeled_wrap">
		<wb:localize id='Add New Properties Message' escapeForJavascript="true">
		<I>New properties will only be added to <br>store locations in which they do not yet exist.</I>
		</wb:localize>
		</div>
		</td>

	</tr>

	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='New Property'>New Property</wb:localize></td> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="dblNewPropertyVal2" 
				submitName="dblNewPropertyVal2" 
				ui='DBLookupUI' 
				uiParameter="source='#dblNewPropertySQL#' sourceType='SQL' labelFieldStatus='default'  width='12'" 
				cssClass="inputField"
				/>
		</div>
		<div class="labeled">			
		<wb:controlField id="txtNewPropVal2" submitName="txtNewPropVal2" ui='StringUI' cssClass="inputField" uiParameter="alternateField='true'  width='12'"/>
		</div>
		</td> 
	</tr>

</td>
</tr>
</wba:table>
	<wba:table class="contentTable form">
	
	<tr> 
		<td>
			<wb:controlField id="chkSection4" submitName="chkSection4" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Add New Volume Driver'>Add New Volume Driver</wb:localize></th> 
		<td width="600">
		<div class="labeled">			
			<wb:controlField 
				id="lnkShowVolDrivers" 
				submitName="lnkShowVolDrivers" 
				ui='DBLookupUI' 
				uiParameter="source='#lnkShowVolDriversSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
				cssClass="inputField"
				/>
		</div>
		</td> 
	</tr> 
	<tr> 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='New volume driver'>New volume driver</wb:localize></td> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="dblVolDriver" 
				submitName="dblVolDriver" 
				ui='DBLookupUI' 
				uiParameter="source='#dblVolDriverSQL#' sourceType='SQL' labelFieldStatus='default'" 
				cssClass="inputField"
				/>
		</div>
		<div class="labeled_wrap">
		<wb:localize id='Add Volume Driver Message' escapeForJavascript="true">
			<I>New volume drivers will only be added to <br>store locations in which they do not yet exist</I>
		</wb:localize>
		</td> 
	</tr>
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Add Auto-Distribution'>Add Auto-Distribution</wb:localize></td> 
		<td>
		<div class="labeled">
		<wb:controlField id="chkAutoDistrib" submitName="chkAutoDistrib" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</div>
		<div class="labeled_wrap">
		<wb:localize id='Add Auto-Distribution Message' escapeForJavascript="true">
			<I>(Select this check box will create an automatic distribution for this volume driver)</I>
		</wb:localize>
		</div>
		</td>
	</tr>


	</wba:table>



	<wba:table class="contentTable form">

	<tr> 
		<td>
			<wb:controlField id="chkSection5" submitName="chkSection5" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Add New Store Department'>Add New Store Department</wb:localize></th> 
		<td width="600">
		<div class="labeled">			
			<wb:controlField 
				id="lnkShowDepartments" 
				submitName="lnkShowDepartments" 
				ui='DBLookupUI' 
				uiParameter="source='#lnkShowDepartmentsSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
				cssClass="inputField"
				/>
		</div>
		<div class="labeled_wrap">
		<wb:localize id="Add New Store Department Message" escapeForJavascript="true"><I>New Departments have to be setup and configured manually within the Template section of the location hierarchy before they can be added to store locations. Additionally, a department can only be added to a store if all volume drivers used in the department are available in the store.</I></wb:localize>
		</div>
		</td> 
	</tr> 
	<tr> 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='New Department'>New Department</wb:localize></td> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="dblDepartment" 
				submitName="dblDepartment"
				ui='DBLookupUI' 
				uiParameter="source='#dblDepartmentSQL#' sourceType='SQL' labelFieldStatus='default'" 
				cssClass="inputField"
				/>
		</div>
		<div class="labeled_wrap">
		<wb:localize id="Add New Department Message" escapeForJavascript="true"><I>New Departments will only be added <br>to store locations in which they do not yet exist.</I></wb:localize>
		</div>
		</td> 
	</tr>

	</wba:table>
	<wba:table class="contentTable form">
	<tr> 
		<td>
			<wb:controlField id="chkSection6" submitName="chkSection6" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Add New Staff Group'>Add New Staff Group</wb:localize></th> 
		<td width="600">
		<div class="labeled">			
			<wb:controlField 
				id="lnkShowAllGroups" 
				submitName="lnkShowAllGroups" 
				ui='DBLookupUI' 
				uiParameter="source='#lnkShowAllGroupsSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
				cssClass="inputField"
				/>
		</div>
		<div class="labeled_wrap">
		<wb:localize id="Add New Store Department Message" escapeForJavascript="true"><I>New staff groups will only be added <br>to store locations that do not yet have a staff group with the same name.</I></wb:localize>
		</div>
		</td> 
	</tr> 
	<tr> 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Name'>Name</wb:localize></td> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="txtGroupName" 
				submitName="txtGroupName"
				ui='StringUI' 
				uiParameter="width='12' labelFieldStatus='default'" 
				cssClass="inputField"
				/>
		</div>
		</td> 
	</tr>
	<tr> 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Description'>Description</wb:localize></td> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="txtGroupDesc" 
				submitName="txtGroupDesc"
				ui='StringUI' 
				uiParameter="width='12' labelFieldStatus='default'" 
				cssClass="inputField"
				/>
		</div>
		</td> 
	</tr>
	<tr> 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Schedule Rules'>Schedule Rules</wb:localize></td> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="dblSchedRules" 
				submitName="dblSchedRules"
				ui='DBLookupUI' 
				uiParameter="source='#dblSchedRulesSQL#' sourceType='SQL' labelFieldStatus='default'" 
				cssClass="inputField"
				/>
		</div>
	</tr>
	<tr> 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Group Ages From'>Group Ages From</wb:localize></td> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="txtGroupMinAge" 
				submitName="txtGroupMinAge"
				ui='NumberUI' 
				uiParameter="scale='3' width='4' labelFieldStatus='default'" 
				cssClass="inputField"
				/>
		</div>
		</td> 
	</tr>
	<tr> 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Group Ages To'>Group Ages To</wb:localize></td> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="txtGroupMaxAge" 
				submitName="txtGroupMaxAge"
				ui='NumberUI' 
				uiParameter="scale='3' width='4' labelFieldStatus='default'" 
				cssClass="inputField"
				/>
		</div>
		</td> 
	</tr>
	<tr> 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Group School Day'>Group School Day</wb:localize></td> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="ddlGroupSchool" 
				submitName="ddlGroupSchool"
				ui='ComboBoxUI' 
				uiParameter="valueList='1,0' labelList='Y,N' labelFieldStatus='default'" 
				cssClass="inputField"
				/>
		</div>
		</td> 
	</tr>
	<tr> 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Group Students'>Group Students</wb:localize></td> 
		<td>
		<div class="labeled">			
			<wb:controlField 
				id="ddlGroupStudents" 
				submitName="ddlGroupStudents"
				ui='ComboBoxUI' 
				uiParameter="valueList='1,0' labelList='Y,N' labelFieldStatus='default'" 
				cssClass="inputField"
				/>
		</div>
		</td> 
	</tr>
	</wba:table>
	
	<wba:table class="contentTable form">

	<tr> 
		<td>
			<wb:controlField id="chkSection7" submitName="chkSection7" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
		</td> 
		<th><wb:localize id='Delete Staff Groups'>Delete Staff Groups</wb:localize></th> 
		<td>
		<div class="labeled_wrap">
		<wb:localize id="Add New Store Department Message" escapeForJavascript="true"><I>Staff groups will only be deleted if no employees are assigned to them.</I></wb:localize>
		</div>
		</td> 
	</tr> 
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Full Time'>Full Time</wb:localize></td> 
		<td width='600'>
			<div class="labeled">
				<wb:controlField 
					id="lnkShowGroupFullTime" 
					submitName="lnkShowGroupFullTime" 
					ui='DBLookupUI' 
					uiParameter="source='#lnkShowGroupFullTimeSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
					cssClass="inputField"
					/>
			</div>

			<div class="labeled">
			<wb:controlField id="chkDeleteFullTime" submitName="chkDeleteFullTime" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
			</div>

		<div class="labeled">
			<img src='<%= request.getContextPath() %>/images/interface/icon_head_trash.gif' border='0'>
		</div>
	</td>
	</tr> 
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Part Time'>Part Time</wb:localize></td> 
		<td width='600'>
			<div class="labeled">
				<wb:controlField 
					id="lnkShowGroupPartTime" 
					submitName="lnkShowGroupPartTime" 
					ui='DBLookupUI' 
					uiParameter="source='#lnkShowGroupPartTimeSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
					cssClass="inputField"
					/>
			</div>

			<div class="labeled">
			<wb:controlField id="chkDeletePartTime" submitName="chkDeletePartTime" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
			</div>

		<div class="labeled">
			<img src='<%= request.getContextPath() %>/images/interface/icon_head_trash.gif' border='0'>
		</div>
	</td>
	</tr> 	
	<tr > 
		<td>
		</td> 
		<td class="subSection"><wb:localize id='Minor'>Minor</wb:localize></td> 
		<td width='600'>
			<div class="labeled">
				<wb:controlField 
					id="lnkShowGroupMinor" 
					submitName="lnkShowGroupMinor" 
					ui='DBLookupUI' 
					uiParameter="source='#lnkShowGroupMinorSQL#' sourceType='SQL' labelFieldStatus='hidden'" 
					cssClass="inputField"
					/>
			</div>

			<div class="labeled">
			<wb:controlField id="chkDeleteMinor" submitName="chkDeleteMinor" ui='CheckBoxUI' cssClass="inputField" uiParameter="alternateField='true'"/>
			</div>

		<div class="labeled">
			<img src='<%= request.getContextPath() %>/images/interface/icon_head_trash.gif' border='0'>
		</div>
	</td>
	</tr> 
	</wba:table>

<%
} 

%>
	<wba:table class="contentTable form">
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
	<wba:table class="contentTable form">
	</wba:table>
	
<p>



<INPUT TYPE="hidden" name="process" value="N">


<script type='text/javascript'>


function apply(thisForm) {

//call Validation
	if (validate()){

	document.getElementsByName('process')[0].value = 'Y';
	document.forms[0].submit()

    }
	
	
}

function validate(){
     
	//ensure atleast one section is checked
	var passed = true;
	var oneChecked = false;
	var startIndex = 1;
	var numChkBoxes = 7;
	var deptSearch = document.getElementsByName('dept_Search')[0].value;
		
	if (deptSearch=='true') {
		numChkBoxes = 2;
	}

	for (i=startIndex;i<=numChkBoxes;i++)	{		
		if(document.page_form.elements["chkSection"+i+"_dummy"].checked){
		
			oneChecked = true;
			//alert("element " + i + " is checked");
		}
	}
	if (!oneChecked){
		   alert("Please specify at least one change or click Cancel");
		
	}
	
	passed = (passed && oneChecked);
	
	<%
	if (deptSearch.equals("false")) {
	%>
	//new store property selected but no value provided

	if( document.page_form.elements["chkSection3_dummy"].checked ) {

		for (i=1;i<=2;i++)
		{		
			if(document.page_form.elements["dblNewPropertyVal" + i + "_label"].value != "" && document.page_form.elements["txtNewPropVal" + i].value == ""){
				alert("Please provide a value for store property " + 
					document.page_form.elements["dblNewPropertyVal" + i + "_label"].value);
				return false;
			}
		}

	}

	//add new volume driver checked but no volume driver selected
	if(document.page_form.elements["chkSection4_dummy"].checked &&
		document.page_form.dblVolDriver_label.value == ""){
			alert("Please select a new volume driver to be added");
			return false;
	}

	//add new department checked but no volume driver selected
	if(document.page_form.elements["chkSection5_dummy"].checked &&
		document.page_form.dblDepartment_label.value == ""){
			alert("Please select a new department to be added");
			return false;
	}
	
	//add new staffgroup checked but no staff group name specified
	if(document.page_form.elements["chkSection6_dummy"].checked &&
		document.page_form.txtGroupName.value == ""){
			alert("Staff Group Name is a required field");
			return false;
	}
	//add new staffgroup checked but no schedule rules specified
	if(document.page_form.elements["chkSection6_dummy"].checked &&
		document.page_form.dblSchedRules_label.value == ""){
			alert("Schedule Rules is a required field");
			return false;
	}
	<%
	}
	%>
	return passed;
}
</script>

</wb:page>