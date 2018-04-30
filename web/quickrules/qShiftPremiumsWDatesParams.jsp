<%@ include file="/system/wbheader.jsp"%>
<%@ page import="com.workbrain.app.ta.rulesgui.QuickRuleHelper" %>
<%@ page import="com.workbrain.app.ta.ruleengine.ConditionSetNode" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="com.workbrain.app.ta.ruleengine.Condition" %>
<%@ page import="com.workbrain.app.ta.ruleengine.ConditionNode" %>
<%@ page import="com.workbrain.app.ta.ruleengine.Parameters" %>
<%@ page import="com.workbrain.app.ta.ruleengine.RuleParameterInfo" %>
<%@ page import="java.util.List" %>
<%@ page import="com.workbrain.app.ta.rules.*" %>
<wb:page>

<%
QuickRuleHelper helper = new QuickRuleHelper(request, out, session, pageContext);
helper.makePageHeader(QuickRuleHelper.NEW_BUTTON + QuickRuleHelper.APPLY_BUTTON + QuickRuleHelper.CANCEL_BUTTON);
String spZoneSQL = "sourceType=SQL source=\"SELECT SPZONE_ID, SPZONE_NAME Name FROM SHIFT_PREMIUM_ZONE\" ";
final String thisPageName = "qShiftPremiumsWDatesParams.jsp";

%>

<%@ include file="/quickrules/conditionSetParameters.inc"%>

<%

  if (helper.getRuleNode().getConditionSetCount() > 0) {

%>

<wba:table width="100%">
<wba:th width="50%"><wb:localize id="Rule_Parameter_Name">Parameter</wb:localize></wba:th>
<wba:th width="50%"><wb:localize id="Rule_Parameter_Value">Value</wb:localize></wba:th>

<!-- Minimum Number Of Minutes -->
<wba:tr>
<wba:td><wb:localize id="Minimum Number Of Minutes">Minimum Number Of Minutes</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField submitName="<%=ShiftPremiumsRule.PARAM_MINIMUM_NUMBER_OF_MINUTES%>" id="<%=ShiftPremiumsRule.PARAM_MINIMUM_NUMBER_OF_MINUTES%>" cssClass='inputField' ui='StringUI' uiParameter='width=33'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(), ShiftPremiumsRule.PARAM_MINIMUM_NUMBER_OF_MINUTES)%></wb:controlField>
</wba:td>
</wba:tr>

<!-- Shift Premium Zone ID -->
<wba:tr>
<wba:td><wb:localize id="Shift Premium Zone">Shift Premium Zone</wb:localize></wba:td>
<wba:td align="right">
<wb:controlField cssClass="inputField" submitName="<%=ShiftPremiumsRule.PARAM_SHIFT_PREMIUM_ZONE_ID%>" id="<%=ShiftPremiumsRule.PARAM_SHIFT_PREMIUM_ZONE_ID%>" ui="DBLookupUI" uiParameter='<%=spZoneSQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(), ShiftPremiumsRule.PARAM_SHIFT_PREMIUM_ZONE_ID)%></wb:controlField>
</wba:td>
</wba:tr>

<!-- Update Work Detail Fields -->
<wba:tr>
<wba:td><wb:localize id="Update Work Detail Fields">Update Work Detail Fields</wb:localize></wba:td>
<wba:td align="right">
<%
boolean updWD = "true".equals(helper.getConditionSetParameterValue(condSetIndex.toInt(), ShiftPremiumsRule.PARAM_UPDATE_WORKDETAIL_FIELDS));
%>
<input type="checkbox" name="UpdateWorkDetailFields_check" <%=updWD ? "checked" : ""%> onClick="if (this.checked) {this.form.UpdateWorkDetailFields.value='true';} else {this.form.UpdateWorkDetailFields.value='false';}">
<input type="hidden"   name="<%=ShiftPremiumsRule.PARAM_UPDATE_WORKDETAIL_FIELDS%>" value='<%=updWD ? "true" : "false"%>'>
</wba:td>
</wba:tr>

<!-- Apply To All Shifts During A Day -->
<wba:tr>
<wba:td><wb:localize id="Apply To All Shifts During A Day">Apply To All Shifts During A Day</wb:localize></wba:td>
<wba:td align="right">
<%
boolean allshifts = "true".equals(helper.getConditionSetParameterValue(condSetIndex.toInt(), ShiftPremiumsRule.PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY));
%>
<input type="checkbox" name="ApplyToAllShiftsDuringADay_check" <%=allshifts ? "checked" : ""%> onClick="if (this.checked) {this.form.ApplyToAllShiftsDuringADay.value='true';} else {this.form.ApplyToAllShiftsDuringADay.value='false';}">
<input type="hidden"   name="<%=ShiftPremiumsRule.PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY%>" value='<%=allshifts ? "true" : "false"%>'>
</wba:td>
</wba:tr>
<%--Apply Hour Type Multiplier to check--%>
<wba:tr>
    <wba:td><wb:localize id="Apply Hour Type Mulitplier">Apply Hour Type Mulitplier</wb:localize></wba:td>
    <wba:td align="right">
     <% boolean applyHrTypeMultipler = "true".equals(helper.getConditionSetParameterValue(condSetIndex.toInt(), ShiftPremiumsRule.PARAM_APPLY_HOURTYPE_MULTIPLIER)); %>
        <input type=checkbox name="ApplyHourTypeMultiplier_check" <%= applyHrTypeMultipler ? "checked" : ""%> onClick="if (this.checked) {this.form.ApplyHourTypeMultiplier.value='true';} else {this.form.ApplyHourTypeMultiplier.value='false';}">
        <input type="hidden" name="<%=ShiftPremiumsRule.PARAM_APPLY_HOURTYPE_MULTIPLIER%>" value='<%= applyHrTypeMultipler ? "true" : "false"%>'>
    </wba:td>
</wba:tr>
</wba:table>
<% } %>
<%
helper.makePageFooter(QuickRuleHelper.NEW_BUTTON + QuickRuleHelper.APPLY_BUTTON + QuickRuleHelper.CANCEL_BUTTON);
%>
</wb:page>