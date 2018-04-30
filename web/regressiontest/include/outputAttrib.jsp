<%@ include file="/system/wbheader.jsp"%>

<%@ page import="com.wbiag.tool.regressiontest.jsp.OutputAttributeBean" %>
<%@ page import="com.wbiag.tool.regressiontest.util.RequestHelper" %>
<%@ page import="com.workbrain.server.jsp.JSPHelper" %>


<wb:page maintenanceFormId='1000005'>

<!-- Get attributes from the request -->
<%
// Get the string of selected attributes, or null if there is non test case.
String selectedAttributes = null;

// Create the bean that will generated the Included/Excluded Lists.
// null selectedAttributes will init to defaults.
OutputAttributeBean attribBean = new OutputAttributeBean(selectedAttributes, JSPHelper.getConnection(request));
%>

<wb:define id="labels"/>
<wb:define id="ids"/>

<script src="<%= request.getContextPath() %>/regressiontest/include/outputAttrib.js" ></script>

<wba:table caption='Snapshot Attributes' captionLocalizeIndex='SnapshotAttributes'>

<!-- Table Header -->
<wba:tr>
	<wba:th><wb:localize id="Table">Table</wb:localize></wba:th>
	<wba:th><wb:localize id="Exclude">Exclude</wb:localize></wba:th>
	<wba:th><wb:localize id="Add/Remove">Add/Remove</wb:localize></wba:th>
	<wba:th><wb:localize id="Include">Include</wb:localize></wba:th>
</wba:tr>

<!-- Work Summary -->
<wba:tr>

	<wba:th>Work Summary</wba:th>
	
	<wb:set id="labels"><%= attribBean.getWorkSummaryExcludedLabels() %></wb:set>
	<wb:set id="ids"><%= attribBean.getWorkSummaryExcludedIds() %></wb:set>
	
	<wba:td><wb:controlField id='workSummaryFieldsExclude' submitName="workSummaryFieldsExclude"
			ui='ListboxUI'
			uiParameter='height="4" multiChoice="true" labelList="#labels#" valueList="#ids#"'>
		</wb:controlField></wba:td>
	
	<wba:td>
		<wba:button label=">>" labelLocalizeIndex="Add>>" onClick="addWorkSummaryField()"/>
		<br>
		<wba:button label="<<" labelLocalizeIndex="Remove<<" onClick="removeWorkSummaryField()"/>
	</wba:td>
	
	<wb:set id="labels"><%= attribBean.getWorkSummaryIncludedLabels() %></wb:set>
	<wb:set id="ids"><%= attribBean.getWorkSummaryIncludedIds() %></wb:set>

	<wba:td><wb:controlField id='workSummaryFieldsInclude' submitName="workSummaryFieldsInclude"
			ui='ListboxUI'
			uiParameter='height="4" multiChoice="true" labelList="#labels#" valueList="#ids#"'>
		</wb:controlField></wba:td>
		
</wba:tr>


<!-- Work Detail -->
<wba:tr>

	<wba:th>Work Detail</wba:th>
	
	<wb:set id="labels"><%= attribBean.getWorkDetailExcludedLabels() %></wb:set>
	<wb:set id="ids"><%= attribBean.getWorkDetailExcludedIds() %></wb:set>

	<wba:td><wb:controlField id='workDetailFieldsExclude' submitName="workDetailFieldsExclude"
			ui='ListboxUI'
			uiParameter='height="4" multiChoice="true" labelList="#labels#" valueList="#ids#"'>
		</wb:controlField></wba:td>
	
	<wba:td>
		<wba:button label=">>" labelLocalizeIndex="Add>>" onClick="addWorkDetailField()"/>
		<br>
		<wba:button label="<<" labelLocalizeIndex="Remove<<" onClick="removeWorkDetailField()"/>
	</wba:td>
	
	<wb:set id="labels"><%= attribBean.getWorkDetailIncludedLabels() %></wb:set>
	<wb:set id="ids"><%= attribBean.getWorkDetailIncludedIds() %></wb:set>

	<wba:td><wb:controlField id='workDetailFieldsInclude' submitName="workDetailFieldsInclude"
			ui='ListboxUI'
			uiParameter='height="4" multiChoice="true" labelList="#labels#" valueList="#ids#"'>
		</wb:controlField></wba:td>

</wba:tr>


<!-- Work Premium -->
<wba:tr>	

	<wba:th>Work Premium</wba:th>
	
	<wb:set id="labels"><%= attribBean.getWorkPremiumExcludedLabels() %></wb:set>
	<wb:set id="ids"><%= attribBean.getWorkPremiumExcludedIds() %></wb:set>

	<wba:td><wb:controlField id='workPremiumFieldsExclude' submitName="workPremiumFieldsExclude"
			ui='ListboxUI'
			uiParameter='height="4" multiChoice="true" labelList="#labels#" valueList="#ids#"'>
		</wb:controlField></wba:td>
	
	<wba:td>
		<wba:button label=">>" labelLocalizeIndex="Add>>" onClick="addWorkPremiumField()"/>
		<br>
		<wba:button label="<<" labelLocalizeIndex="Remove<<" onClick="removeWorkPremiumField()"/>
	</wba:td>
	
	<wb:set id="labels"><%= attribBean.getWorkPremiumIncludedLabels() %></wb:set>
	<wb:set id="ids"><%= attribBean.getWorkPremiumIncludedIds() %></wb:set>

	<wba:td><wb:controlField id='workPremiumFieldsInclude' submitName="workPremiumFieldsInclude"
			ui='ListboxUI'
			uiParameter='height="4" multiChoice="true" labelList="#labels#" valueList="#ids#"'>
		</wb:controlField></wba:td>

</wba:tr>


<!-- Employee Balance -->
<wba:tr>	

	<wba:th>Employee Balances</wba:th>
	
	<wb:set id="labels"><%= attribBean.getEmployeeBalanceExcludedLabels() %></wb:set>
	<wb:set id="ids"><%= attribBean.getEmployeeBalanceExcludedIds() %></wb:set>

	<wba:td><wb:controlField id='employeeBalancesExclude' submitName="employeeBalancesExclude"
			ui='ListboxUI'
			uiParameter='height="4" multiChoice="true" labelList="#labels#" valueList="#ids#"'>
		</wb:controlField></wba:td>
	
	<wba:td>
		<wba:button label=">>" labelLocalizeIndex="Add>>" onClick="addEmployeeBalance()"/>
		<br>
		<wba:button label="<<" labelLocalizeIndex="Remove<<" onClick="removeEmployeeBalance()"/>
	</wba:td>
	
	<wb:set id="labels"><%= attribBean.getEmployeeBalanceIncludedLabels() %></wb:set>
	<wb:set id="ids"><%= attribBean.getEmployeeBalanceIncludedIds() %></wb:set>

	<wba:td><wb:controlField id='employeeBalancesInclude' submitName="employeeBalancesInclude"
			ui='ListboxUI'
			uiParameter='height="4" multiChoice="true" labelList="#labels#" valueList="#ids#"'>
		</wb:controlField></wba:td>

</wba:tr>


</wba:table>

</wb:page>