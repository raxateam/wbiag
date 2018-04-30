 <!-- Declares typelibs etc. -->
<%@ include file="/system/wbheader.jsp"%>

<!-- Imports -->
<%@ page import="java.util.List, java.util.Iterator" %>
<%@ page import="com.workbrain.util.DateHelper" %>
<%@ page import  = "com.workbrain.server.jsp.taglib.dataset.DataSetTag"%>
<%@ page import="com.wbiag.tool.regressiontest.jsp.RulesCaseParamsBean" %>
<%@ page import="com.wbiag.tool.regressiontest.model.RulesCaseData" %>
<%@ page import="com.wbiag.tool.regressiontest.model.TestSuiteData" %>
<%@ page import="com.wbiag.tool.regressiontest.util.RequestHelper" %>

<!-- Begin the page. -->
<wb:page 	submitAction='/regressiontest/payrules/action.jsp' 
			maintenanceFormId='1000005' 
			>

<!-- Javascript -->
<script src="<%= request.getContextPath() %>/regressiontest/include/testSuite.js"></script>
<script src="<%= request.getContextPath() %>/regressiontest/payrules/payRulesTestCase.js"></script>

<!-- Define page variables -->
<wb:define id="DATE_FORMAT">MM/dd/yyyy</wb:define>
<wb:define id="DATE_TIME_FORMAT">MM/dd/yyyy hh:mm</wb:define>
<wb:define id="DEFAULT_START_DATE"></wb:define>
<wb:define id="DEFAULT_END_DATE"></wb:define>


<!-- Hidden Fields -->
<input type=HIDDEN name="actionType" value="">
<input type=HIDDEN name="reportXSLFilename" value="payRules-report.xsl">


<!-- INCLUDE - Test Suite Parameters and Buttons -->
<table>


<wb:include page="../include/testSuiteParams.jsp" /> 



<!-- Get attributes from the request -->
<%
TestSuiteData testSuiteData = RequestHelper.getTestSuiteBean(request);
if (testSuiteData == null) {
	testSuiteData = new TestSuiteData();
}
%>
<wb:if expression="<%= String.valueOf(testSuiteData.getSuiteId()) %>" operator=">" compareToExpression="0">

<!-- Page OnLoad -->
<wb:pageOnLoad id="initOutputAttrib">initOutputAttrib()</wb:pageOnLoad>

<%
// Test Case - If one is being edited.
RulesCaseParamsBean formBean = RequestHelper.getRulesCaseParameters(request);
if (formBean == null) {
	formBean = new RulesCaseParamsBean();
}

// Test Case List.
List testCaseList = RequestHelper.getTestCaseList(request);
%>



<!-- Test Case Parameters -->
<tr>
<td>
<wba:table width="100%" caption="Pay Rules Test Case" captionLocalizeIndex='PayRulesTestCase'>

    <wba:tr>
    <wba:td>
    <wba:table caption="Properties" captionLocalizeIndex='PayRulesTestCaseProperties'>

        <!-- Test Case Name -->
    	<wba:tr>
    		<wba:th><wb:localize id='TestCaseName'>Test Case Name</wb:localize></wba:th>
    		<wba:td><wb:controlField cssClass="inputField" id='TestCaseName' 
    						submitName="testCaseName"><%= formBean.getName() == null ? "" : formBean.getName() %></wb:controlField>
    		</wba:td>
    	</wba:tr>
    	
        <!-- Test Case Description -->
    	<wba:tr>
    		<wba:th><wb:localize id='TestCaseDescription'>Test Case Description</wb:localize></wba:th>
    		<wba:td><wb:controlField cssClass="inputField" id='TestCaseDescription' 
    						submitName="testCaseDescription"><%= formBean.getDescription() == null ? "" : formBean.getDescription() %></wb:controlField>
    		</wba:td>
    	</wba:tr>
    
        <!-- Employees -->
    	<wba:tr>
    		<wba:th><wb:localize id='Employees'>Employees</wb:localize></wba:th>
    		<wba:td>
    		<wb:controlField cssClass="inputField" id='Employees' submitName='employeeIds'
    					ui='DBLookupUI' 
    					uiParameter="labelFieldStatus='edit' sourceType='SQL' source='SELECT distinct EMP_ID,EMP_NAME,FULL_NAME FROM SEC_EMPLOYEE_LOOKUP WHERE WBU_ID = #page.property.userId# ORDER BY FULL_NAME, EMP_NAME' multiChoice='true'"><%= formBean.getEmpIds() %></wb:controlField>
    		</wba:td>
    	</wba:tr>
    
        <!-- Teams -->
    	<wba:tr>
    		<wba:th><wb:localize id='Teams'>Teams</wb:localize></wba:th>
    		<wba:td>
    		<wb:controlField cssClass="inputField" id='Teams' submitName='teamIds'
    					ui='DBLookupUI' 
    					uiParameter="multiChoice=true sourceType=SQL source='select wbt_id, wbt_name from sec_workbrain_team where wbu_id = #page.property.userId#' labelFieldStatus='edit'"><%= formBean.getTeamIds() %></wb:controlField>
    		</wba:td>
    	</wba:tr>

        <!-- Include Sub Teams -->
    	<wba:tr>
    		<wba:th><wb:localize id='IncludeSubTeams'>Include Sub Teams</wb:localize></wba:th>
    		<wba:td>
    		<wb:controlField cssClass="inputField" id='includeSubTeams' submitName='includeSubTeams'
    					ui='CheckboxUI' 
    					><%= formBean.getIncludeSubTeams() %></wb:controlField>
    		</wba:td>
    	</wba:tr>

        <!-- Pay Group -->
    	<wba:tr>
    		<wba:th><wb:localize id='PayGroup'>Pay Group</wb:localize></wba:th>
    		<wba:td>
    		<wb:controlField cssClass="inputField" id='PayGroup' submitName='payGroupIds'
    					ui='DBLookupUI' 
    					uiParameter="labelFieldStatus='edit' sourceType='SQL' source='SELECT PAYGRP_ID, PAYGRP_NAME, PAYGRP_DESC FROM PAY_GROUP'"><%= formBean.getPayGroupIds() %></wb:controlField>
    		</wba:td>
    	</wba:tr>

        <!-- Calc Group -->
    	<wba:tr>
    		<wba:th><wb:localize id='CalcGroup'>Calculation Group</wb:localize></wba:th>
    		<wba:td>
    		<wb:controlField cssClass="inputField" id='CalcGroup' submitName='calcGroupIds'
    					ui='DBLookupUI' 
    					uiParameter="multiChoice='true' sourceType='SQL' source='SELECT CALCGRP_ID, CALCGRP_NAME, CALCGRP_DESC FROM CALC_GROUP' labelFieldStatus='edit'"><%= formBean.getCalcGroupIds() %></wb:controlField>
    		</wba:td>
    	</wba:tr>

        <!-- Start Date -->
    	<wba:tr>
    		<wba:th><wb:localize id='StartDate'>Start Date *</wb:localize></wba:th>
            <wba:td><wb:controlField cssClass="inputField" id='StartDate' submitName="startDate" 
           				onChange="setDates()"
           				ui="DatePickerUI" 
           				uiParameter="format=\"#DATE_FORMAT#\""><%= formBean.getStartDate() == null ? DEFAULT_START_DATE.toString() : formBean.getStartDate() %></wb:controlField>
            </wba:td>
    	</wba:tr>
    
        <!-- End Date -->
    	<wba:tr>
    		<wba:th><wb:localize id='EndDate'>End Date</wb:localize></wba:th>
            <wba:td><wb:controlField cssClass="inputField" id='EndDate' submitName="endDate" 
           				onChange="setDates()"
           				ui="DatePickerUI" 
           				uiParameter="format=\"#DATE_FORMAT#\""><%= formBean.getEndDate() == null ? DEFAULT_END_DATE.toString() : formBean.getEndDate() %></wb:controlField>
            </wba:td>
    	</wba:tr>
    
    </wba:table>
    </wba:td>
    </wba:tr>

    <wba:tr>
    <wba:td>
        <!-- INCLUDE - Output Attributes to Capture -->
        <wb:include page="../include/outputAttrib.jsp" /> 
    </wba:td>
    </wba:tr>


    <wba:tr>
    <wba:td>
        <!-- Add Test Case button -->
        <wba:button label="AddTestCase" labelLocalizeIndex="AddTestCase" onClick="addTestCase()"/>
    </wba:td>
    </wba:tr>

</wba:table>
</td>
</tr>

<!-- Test Case list -->
<tr>
<td>
<wba:table width="100%" caption="Pay Rules Test Case List" captionLocalizeIndex='PayRulesTestCaseList'>

<wba:tr>
	<wba:th><wb:localize id="Delete">Del</wb:localize></wba:th>
	<wba:th><wb:localize id="ReCreate">Re-Create</wb:localize></wba:th>
	<wba:th colspan="2"><wb:localize id="Copy">Copy to Employees</wb:localize></wba:th>
	<wba:th><wb:localize id="TestCaseName">Test Case Name</wb:localize></wba:th>
	<wba:th><wb:localize id="TestCaseDescription">Description</wb:localize></wba:th>
	<wba:th><wb:localize id='Employee'>Employee</wb:localize></wba:th>
	<wba:th colspan="2"><wb:localize id='Dates'>Dates</wb:localize></wba:th>
	<wba:th><wb:localize id='LastModified'>Last Modified</wb:localize></wba:th>
</wba:tr>
<wba:tr>
    <wba:th><input type="checkbox" name="DeleteAll" onClick="setDeleteAllCases(this.checked)"></wba:th>
	<wba:th><input type="checkbox" name="ReCreateAll" onClick="setReCreateAllCases(this.checked)"></wba:th>
	<wba:th colspan="8"></wba:th>
</wba:tr>

<input type=HIDDEN name="testCaseId" value="0">
<input type=HIDDEN name="copyToEmpIdList" value="0">
<input type=HIDDEN name="copyToEmpNameList" value="0">


<wb:define id='deleteCaseIdList'/>
<wb:define id='reCreateCaseIdList'/>
<wb:define id='copyToEmpIdList'/>
<wb:define id='rulesCaseId'/>


<%
if (testCaseList != null && testCaseList.size() > 0) {

	Iterator i = testCaseList.iterator();
	RulesCaseData testCase = null	;

	while (i.hasNext()) {
		testCase = (RulesCaseData) i.next();
		
	%>

	<wb:set id='rulesCaseId'><%= String.valueOf(testCase.getCaseId()) %></wb:set>
	<wb:set id='deleteCaseIdList'><%= "deleteCaseId_" + String.valueOf(testCase.getCaseId()) %></wb:set>
	<wb:set id='reCreateCaseIdList'><%= "reCreateCaseId_" + String.valueOf(testCase.getCaseId()) %></wb:set>
	<wb:set id='copyToEmpIdList'><%= "copyToEmpId_" + String.valueOf(testCase.getCaseId()) %></wb:set>
	

	
    <wba:tr>
		<wba:td><wb:controlField ui='CheckboxUI' id='Delete' submitName="#deleteCaseIdList#"/></wba:td>
		<wba:td><wb:controlField ui='CheckboxUI' id='Recreate' submitName="#reCreateCaseIdList#"/></wba:td>
		<wba:td>
    		<wb:controlField cssClass="inputField" id='CopyToEmployeeList' submitName='#copyToEmpIdList#'
    					ui='DBLookupUI' 
    					uiParameter="width='10' labelFieldStatus='edit' sourceType='SQL' source='SELECT distinct EMP_ID,EMP_NAME,FULL_NAME FROM SEC_EMPLOYEE_LOOKUP WHERE WBU_ID = #page.property.userId# ORDER BY FULL_NAME, EMP_NAME' multiChoice='true'"></wb:controlField>  
		</wba:td>
		
		<wba:td>
    		<wba:button label="Copy" labelLocalizeIndex="CreateTestCases" onClick="copyTestCase('#rulesCaseId#', '#copyToEmpIdList#')"/>
		</wba:td>

		<wba:td><wb:controlField cssClass="inputField" id='testCaseName_#rulesCaseId#' 
    						submitName="testCaseName_#rulesCaseId#"><%= testCase.getCaseName() %></wb:controlField></wba:td>
		<wba:td><wb:controlField cssClass="inputField" id='testCaseDesc_#rulesCaseId#' 
    						submitName="testCaseDesc_#rulesCaseId#"><%= testCase.getCaseDesc() %></wb:controlField>		
		</wba:td>
		<wba:td><wb:controlField cssClass="inputField" id='employeeName_#rulesCaseId#' submitName='editEmployee_#rulesCaseId#'
    					ui='DBLookupUI' 
    					onChange="javascript:warning()"
    					uiParameter="width='15' labelFieldStatus='edit' sourceType='SQL' source='SELECT distinct EMP_ID,EMP_NAME,FULL_NAME FROM SEC_EMPLOYEE_LOOKUP WHERE WBU_ID = #page.property.userId# ORDER BY FULL_NAME, EMP_NAME' multiChoice='false'"><%=testCase.getEmpId()%></wb:controlField>
		</wba:td>
		<wba:td>
			<wb:controlField cssClass="inputField" id='startDate_#rulesCaseId#' submitName="startDate_#rulesCaseId#" 
           				onChange="javascript:warning()"
           				ui="DatePickerUI" 
           				uiParameter="format=\"#DATE_FORMAT#\""><%= testCase.getCaseEndDate() == null ? DateHelper.convertDateString(testCase.getCaseStartDate(), DATE_FORMAT.toString()): DateHelper.convertDateString(testCase.getCaseStartDate(), DATE_FORMAT.toString())%></wb:controlField>
		</wba:td>        
		<wba:td>
			<wb:controlField cssClass="inputField" id='endDate_#rulesCaseId#' submitName="endDate_#rulesCaseId#" 
           				onChange="javascript:warning()"
           				ui="DatePickerUI" 
           				uiParameter="format=\"#DATE_FORMAT#\""><%=DateHelper.convertDateString(testCase.getCaseEndDate(), DATE_FORMAT.toString()) %></wb:controlField>
		</wba:td>
		<wba:td><%= DateHelper.convertDateString(testCase.getCaseUpdatedDate(), DATE_TIME_FORMAT.toString()) %></wba:td>
	</wba:tr>
	
	<% } %>

<% } else { %>
    
    <wba:tr>
    <wba:td>
    None.
    </wba:td>
    </wba:tr>
    
<% } %>

</wba:table>
</td>
</tr>

<tr>
<td>
<wba:table width="100%">

	<!-- Delete, Recreate buttons -->
    <wba:tr>
    	<wba:th>
    	    <wba:button label="Delete" labelLocalizeIndex="Delete" onClick="deleteTestCase()"/>
    	    <wba:button label="Re-Create" labelLocalizeIndex="ReCreate" onClick="reCreateTestCase()"/>
    	    <wba:button label="Save" labelLocalizeIndex="Save" onClick="saveChangesTestCase()"/>
    	</wba:th>
    </wba:tr>
    <wba:tr>
    	<wba:th>
    	    <div id="warning" style="display:none;background-color:#FFFF99;">
	    	    <wb:localize id="TestCaseEditWarning">Changing dates or employee will cause the expected results to be regenerated for each modified test case.</wb:localize>
    	    </div>
    	</wba:th>
    </wba:tr>    

</wba:table>
</td>
</tr>

<!-- End display test case data only if this is a new Test Suite. -->
</wb:if>

</table>

</wb:page>
