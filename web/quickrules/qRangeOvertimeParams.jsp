<%@ include file="/system/wbheader.jsp"%>
<%@ page import="com.workbrain.app.ta.rulesgui.QuickRuleHelper" %>
<wb:page popupPage='true'>
<%
QuickRuleHelper helper = new QuickRuleHelper(request, out, session, pageContext);
helper.makePageHeader();
final String TIME_CODE_SQL = "sourceType=SQL source=\"SELECT TCODE_NAME, TCODE_NAME Name FROM TIME_CODE\" ";
final String HOUR_TYPE_SQL = "sourceType=SQL source=\"SELECT HTYPE_NAME, HTYPE_NAME Name FROM HOUR_TYPE\" ";
%>
<wba:table width="100%">
<wba:th width="50%"><wb:localize id="Rule_Parameter_Name">Parameter</wb:localize></wba:th>
<wba:th width="50%"><wb:localize id="Rule_Parameter_Value">Value</wb:localize></wba:th>

<!-- (1) Hour Set Description -->
<wba:tr>
<wba:td><wb:localize id="Hour Set Description">Hour Set Description</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField submitName="HourSetDescription" id="HourSetDescription" cssClass='inputField' ui='StringUI' uiParameter='width=33'><%=helper.getConditionSetParameterValue("HourSetDescription")%></wb:controlField>
</wba:td>
</wba:tr>

<!-- (1.5) Work Detail Time Codes -->
<wba:tr>
<wba:td><wb:localize id="Work Detail Time Codes">Work Detail Time Codes</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField cssClass="inputField" submitName="WorkDetailTimeCodes" id="WorkDetailTimeCodes" ui="DBLookupUI" uiParameter='<%= TIME_CODE_SQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue("WorkDetailTimeCodes")%></wb:controlField>
</wba:td>
</wba:tr>

<!-- (2) Eligible Hour Types -->
<wba:tr>
<wba:td><wb:localize id="Eligible Hour Types">Eligible Hour Types</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField cssClass="inputField" submitName="EligibleHourTypes" id="EligibleHourTypes" ui="DBLookupUI" uiParameter='<%= HOUR_TYPE_SQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue("EligibleHourTypes")%></wb:controlField>
</wba:td>
</wba:tr>

<!-- (3) Premium Time Codes Counted -->
<wba:tr>
<wba:td><wb:localize id="Premium Time Codes Counted">Premium Time Codes Counted</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField cssClass="inputField" submitName="PremiumTimeCodesCounted" id="PremiumTimeCodesCounted" ui="DBLookupUI" uiParameter='<%= TIME_CODE_SQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue("PremiumTimeCodesCounted")%></wb:controlField>
</wba:td>
</wba:tr>

<!-- (4) Discount Time Codes -->
<wba:tr>
<wba:td><wb:localize id="Discount Time Codes">Discount Time Codes</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField cssClass="inputField" submitName="DiscountTimeCodes" id="DiscountTimeCodes" ui="DBLookupUI" uiParameter='<%= TIME_CODE_SQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue("DiscountTimeCodes")%></wb:controlField>
</wba:td>
</wba:tr>

<!-- (5) Apply On Unit -->
<wba:tr>
<wba:td><wb:localize id="Apply On Unit">Apply On Unit</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField submitName="ApplyOnUnit" id="ApplyOnUnit" cssClass='inputField' ui='ComboBoxUI' uiParameter="labelList='WEEK,MONTH,PAY PERIOD,QUARTER,YEAR' valueList='WEEK,MONTH,PAY PERIOD,QUARTER,YEAR'"><%=helper.getConditionSetParameterValue("DayWeekStarts")%></wb:controlField>
</wba:td>
</wba:tr>

<!-- (5) Apply On Value Start -->
<wba:tr>
<wba:td><wb:localize id="Apply On Value Start">Apply On Value Start</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField submitName="ApplyOnValueStart" id="ApplyOnValueStart" cssClass='inputField' ui='StringUI' uiParameter='width=33'><%=helper.getConditionSetParameterValue("ApplyOnValueStart")%></wb:controlField>
</wba:td>
</wba:tr>

<!-- (5) Apply On Value End -->
<wba:tr>
<wba:td><wb:localize id="Apply On Value Start">Apply On Value Start</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField submitName="ApplyOnValueEnd" id="ApplyOnValueEnd" cssClass='inputField' ui='StringUI' uiParameter='width=33'><%=helper.getConditionSetParameterValue("ApplyOnValueEnd")%></wb:controlField>
</wba:td>
</wba:tr>

<!-- (6) Apply Based On Schedule -->
<wba:tr>
<wba:td><wb:localize id="Apply Based On Schedule">Apply Based On Schedule</wb:localize></wba:td>
<wba:td align="right">
<%
boolean applyBasedOnSchedule = "true".equals(helper.getConditionSetParameterValue("ApplyBasedOnSchedule"));
%>
<input type="checkbox" name="ApplyBasedOnSchedule_check" <%= applyBasedOnSchedule ? "checked" : "" %> onClick="if (this.checked) {this.form.ApplyBasedOnSchedule.value='true';} else {this.form.ApplyBasedOnSchedule.value='false';}">
<input type='hidden' name='ApplyBasedOnSchedule' value='<%= applyBasedOnSchedule %>'>
</wba:td>
</wba:tr>

<!-- (7) Premium Time Code Inserted -->
<wba:tr>
<wba:td><wb:localize id="Premium Time Code Inserted">Premium Time Code Inserted</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField cssClass="inputField" submitName="PremiumTimeCodeInserted" id="PremiumTimeCodeInserted" ui="DBLookupUI" uiParameter='<%= TIME_CODE_SQL + " multiChoice=false"%>'><%=helper.getConditionSetParameterValue("PremiumTimeCodeInserted")%></wb:controlField>
</wba:td>
</wba:tr>

<!-- (8) Hour type For Overtime Work Details -->
<wba:tr>
<wba:td><wb:localize id="Hour type For Overtime Work Details">Hour type For Overtime Work Details</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField cssClass="inputField" submitName="HourTypeForOvertimeWorkDetails" id="HourTypeForOvertimeWorkDetails" ui="DBLookupUI" uiParameter='<%= HOUR_TYPE_SQL + " multiChoice=false"%>'><%=helper.getConditionSetParameterValue("HourTypeForOvertimeWorkDetails")%></wb:controlField>
</wba:td>
</wba:tr>

<!-- Assign Better Rate -->
<wba:tr>
<wba:td><wb:localize id="Assign Better Rate">Assign Better Rate</wb:localize></wba:td>
<wba:td align="right">
<%
boolean assignBetterRate = "true".equals(helper.getConditionSetParameterValue("AssignBetterRate"));
%>
<input type="checkbox" name="AssignBetterRate_check" <%= assignBetterRate ? "checked" : "" %> onClick="if (this.checked) {this.form.AssignBetterRate.value='true';} else {this.form.AssignBetterRate.value='false';}">
<input type='hidden' name='AssignBetterRate' value='<%= assignBetterRate %>'>
</wba:td>
</wba:tr>


</wba:table>
<%
helper.makePageFooter();
%>
</wb:page>