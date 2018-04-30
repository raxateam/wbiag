<%@ include file="/system/wbheader.jsp"%>
<%@ page import="com.workbrain.app.ta.rulesgui.QuickRuleHelper" %>
<%@ page import="com.workbrain.app.ta.ruleengine.ConditionSetNode" %>
<%@ page import="java.util.Enumeration" %>

<%@ page import="com.workbrain.app.ta.ruleengine.ConditionNode" %>
<%@ page import="com.workbrain.app.ta.ruleengine.Parameters" %>
<%@ page import="com.workbrain.app.ta.ruleengine.Parm" %>
<%@ page import="com.workbrain.app.ta.ruleengine.Condition" %>
<%@ page import="java.util.List" %>
<%@ page import="com.workbrain.app.ta.ruleengine.RuleParameterInfo" %>
<%@ page import="com.wbiag.app.ta.quickrules.PremiumFromBalancePayoutRule" %>
<%@ page import="com.workbrain.util.StringHelper" %>

<wb:page subsidiaryPage='true'>
<wb:define id="ruleIndex"><wb:get id="ruleIndex" default="-1" scope="parameter"/></wb:define>
<wb:define id="condSetIndex"><wb:get id="condSetIndex" default="0" scope="parameter"/></wb:define>

<%
QuickRuleHelper helper = new QuickRuleHelper(request, out, session, pageContext);
helper.makePageHeader(QuickRuleHelper.NEW_BUTTON + QuickRuleHelper.APPLY_BUTTON + QuickRuleHelper.CANCEL_BUTTON);
final String thisPageName = "wbiagPremiumFromBalanceParams.jsp";
String balanceNameSQL = "sourceType=SQL source='SELECT BAL_NAME, BAL_LOC_NAME, BAL_LOC_DESC FROM VL_BALANCE' title='LOOKUP_BALANCE_NAME' labelFieldStatus=edit";
String tcodeSQL = "sourceType=SQL source='SELECT TCODE_NAME,TCODE_LOC_NAME, TCODE_LOC_DESC FROM VL_TIME_CODE' title='LOOKUP_TCODE_NAME' labelFieldStatus=edit";
String htypeSQL = "sourceType=SQL source='SELECT HTYPE_NAME, HTYPE_LOC_NAME, HTYPE_LOC_DESC FROM VL_HOUR_TYPE' title='LOOKUP_HOUR_TYPE' labelFieldStatus=edit";

%>

<%@ include file="/quickrules/conditionSetExecuteMutuallyExclusive.inc"%>

<%@ include file="/quickrules/conditionSetParameters.inc"%>

<%

  if (helper.getRuleNode().getConditionSetCount() > 0) {

%>

<wba:table width="100%">
<th width="50%"><wb:localize id="Rule_Parameter_Name">Parameter</wb:localize></th>
<th width="50%"><wb:localize id="Rule_Parameter_Value">Value</wb:localize></th>


<!-- Balance Names -->
<tr>
<td>* <wb:localize id="Prem_Balance_Names">Balance Names</wb:localize></td>
<td align="right">
<wb:controlField cssClass="inputField" submitName="<%=PremiumFromBalancePayoutRule.PARAM_BALANCE_NAMES%>" ui="DBLookupUI" uiParameter='<%=balanceNameSQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),PremiumFromBalancePayoutRule.PARAM_BALANCE_NAMES)%></wb:controlField>
</td>
</tr>

<!-- Balance Decremented -->
<tr>
<td>* <wb:localize id="Prem_Balance_Decremented">Balance Decremented</wb:localize></td>
<td align="right">
<%
    boolean paramBalDec = "true".equals(helper.getConditionSetParameterValue(condSetIndex.toInt(),PremiumFromBalancePayoutRule.PARAM_BALANCE_DECREMENTED));
%>
<input type="checkbox" name="ParamBalDec" <%= paramBalDec ? "checked" : "" %> onClick="if (this.checked) {this.form.BalanceDecremented.value='true';} else {this.form.BalanceDecremented.value='false';}">
<input type='hidden' name='<%=PremiumFromBalancePayoutRule.PARAM_BALANCE_DECREMENTED%>' value='<%= paramBalDec %>'>
</td>
</tr>

<!-- Payout Approach -->
<tr>
<td>* <wb:localize id="Prem_Payout_Approach">Payout Approach</wb:localize></td>
<td align="right">
<wb:controlField cssClass="inputField" submitName="<%=PremiumFromBalancePayoutRule.PARAM_PAYOUT_APPROACH%>" id="<%=PremiumFromBalancePayoutRule.PARAM_PAYOUT_APPROACH%>" ui="ComboBoxUI" uiParameter="labelList='Fixed value,To final value' valueList='Fixed value,To final value'"><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),PremiumFromBalancePayoutRule.PARAM_PAYOUT_APPROACH)%></wb:controlField>
</td>
</tr>

<!-- Payout Approach Value -->
<tr>
<td>* <wb:localize id="Prem_Payout_Approach_Value">Payout Approach Value</wb:localize></td>
<td align="right">
<wb:controlField submitName="<%=PremiumFromBalancePayoutRule.PARAM_PAYOUT_APPROACH_VALUE%>" id="<%=PremiumFromBalancePayoutRule.PARAM_PAYOUT_APPROACH_VALUE%>" cssClass='inputField' ui='StringUI' uiParameter='width=33'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),PremiumFromBalancePayoutRule.PARAM_PAYOUT_APPROACH_VALUE)%></wb:controlField>
</td>
</tr>

<!-- Minutes Per Balance Unit -->
<tr>
<td><wb:localize id="Prem_Min_Per_Unit">Minutes Per Balance Unit</wb:localize></td>
<td align="right">
<wb:controlField submitName="<%=PremiumFromBalancePayoutRule.PARAM_MINUTES_PER_UNIT%>" id="<%=PremiumFromBalancePayoutRule.PARAM_MINUTES_PER_UNIT%>" cssClass='inputField' ui='StringUI' uiParameter='width=33'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),PremiumFromBalancePayoutRule.PARAM_MINUTES_PER_UNIT)%></wb:controlField>
</td>
</tr>

<!-- Premium Time code names -->
<tr>
<td>* <wb:localize id="Prem_TCode_Name">Premium Time Code</wb:localize></td>
<td align="right">
<wb:controlField cssClass="inputField" submitName="<%=PremiumFromBalancePayoutRule.PARAM_TIME_CODE_NAMES%>" ui="DBLookupUI" uiParameter='<%=tcodeSQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),PremiumFromBalancePayoutRule.PARAM_TIME_CODE_NAMES)%></wb:controlField>
</td>
</tr>

<!-- Premium Hour Type names -->
<tr>
<td>* <wb:localize id="Prem_Htype_Name">Premium Hour Type</wb:localize></td>
<td align="right">
<wb:controlField cssClass="inputField" submitName="<%=PremiumFromBalancePayoutRule.PARAM_HOUR_TYPE_NAMES%>" ui="DBLookupUI" uiParameter='<%=htypeSQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),PremiumFromBalancePayoutRule.PARAM_HOUR_TYPE_NAMES)%></wb:controlField>
</td>
</tr>


<!-- Minutes Per Balance Unit -->
<tr>
<td><wb:localize id="Prem_Addiontal_Param">Additional Premium Field Values</wb:localize></td>
<td align="right">
<wb:controlField submitName="<%=PremiumFromBalancePayoutRule.PARAM_ADDITIONAL_PREM%>" id="<%=PremiumFromBalancePayoutRule.PARAM_ADDITIONAL_PREM%>" cssClass='inputField' ui='StringUI' uiParameter='width=33'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),PremiumFromBalancePayoutRule.PARAM_ADDITIONAL_PREM)%></wb:controlField>
</td>
</tr>

</wba:table>
        <% } %>
<%
helper.makePageFooter(QuickRuleHelper.NEW_BUTTON + QuickRuleHelper.APPLY_BUTTON + QuickRuleHelper.CANCEL_BUTTON);
%>
</wb:page>