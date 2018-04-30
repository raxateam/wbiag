<%@ include file="/system/wbheader.jsp"%> 

<%@ page import="com.workbrain.app.ta.rulesgui.QuickRuleHelper" %> 
<%@ page import="com.workbrain.app.ta.rulesgui.QuickRuleHelper" %> 
<%@ page import="com.workbrain.app.ta.ruleengine.ConditionSetNode" %> 
<%@ page import="com.workbrain.app.ta.ruleengine.ConditionNode" %> 
<%@ page import="com.workbrain.app.ta.ruleengine.Parameters" %> 
<%@ page import="com.workbrain.app.ta.ruleengine.Parm" %> 
<%@ page import="java.util.Enumeration" %> 
<%@ page import="com.workbrain.app.ta.ruleengine.Condition" %> 
<%@ page import="java.util.List" %> 
<%@ page import="com.workbrain.app.ta.ruleengine.RuleParameterInfo" %> 
<%@ page import="com.workbrain.app.ta.quickrules.*" %> 
<%@ page import="com.wbiag.app.ta.quickrules.DailyOvertime24HourRule" %> 

<wb:page subsidiaryPage='true'> <wb:define id="ruleIndex"><wb:get id="ruleIndex" default="-1" scope="parameter"/></wb:define> <wb:define id="condSetIndex"><wb:get id="condSetIndex" default="0" scope="parameter"/></wb:define>

<%
QuickRuleHelper helper = new QuickRuleHelper(request, out, session, pageContext);
final String thisPageName = "qDailyOvertimeParams.jsp";
final String TIME_CODE_SQL = "sourceType=SQL source=\"SELECT TCODE_NAME, TCODE_NAME Name FROM TIME_CODE\" ";
final String HOUR_TYPE_SQL = "sourceType=SQL source=\"SELECT HTYPE_NAME, HTYPE_NAME Name FROM HOUR_TYPE\" ";
final String PAYRULE_HOURSET_SQL = "sourceType=SQL source=\"SELECT PH_TOKEN, PH_TOKEN Name, PH_DESC Description FROM PAYRULE_HOURSET\" ";

helper.makePageHeader(QuickRuleHelper.NEW_BUTTON + QuickRuleHelper.APPLY_BUTTON + QuickRuleHelper.CANCEL_BUTTON);
%> <%@ include file="/quickrules/conditionSetParameters.inc"%> <%

  if (helper.getRuleNode().getConditionSetCount() > 0) {

%> <wba:table width="100%"> <wba:th width="50%"><wb:localize id="Rule_Set_Parameter_Name">Rule Set Parameter</wb:localize></wba:th> <wba:th width="50%"><wb:localize id="Rule_Set_Parameter_Value">Value</wb:localize></wba:th> <!-- (1) Hour Set Description --> <wba:tr> <wba:td>* <wb:localize id="Hour Set Description">Hour Set Description</wb:localize></wba:td> <wba:td align="right"> <%
String hoursetValue = helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION);
if (helper.getQRUseHoursetComboBox()) {
	if ( hoursetValue.indexOf(",")>-1 ) {
		hoursetValue = hoursetValue.replace(',',';');
		helper.createHoursetTokenIfNotExist(hoursetValue);
	}
%> <wb:controlField cssClass="inputField" submitName="<%=DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION%>" id="<%=DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION%>" ui='DBLookupUI' uiParameter='<%= PAYRULE_HOURSET_SQL + " multiChoice=false"%>'><%=hoursetValue%></wb:controlField> <%} else {
	if ( hoursetValue.indexOf(";")>-1 ) {
		hoursetValue = hoursetValue.replace(';',',');
	}
%> <wb:controlField submitName="<%=DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION%>" id="<%=DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION%>" cssClass='inputField' ui='StringUI' uiParameter='width=33'><%=hoursetValue%></wb:controlField> <%}%> </wba:td> </wba:tr> <!-- (1.5) Add Premium for First hourtype token --> <wba:tr> <wba:td><wb:localize id="Add Premium For First Hourtype Token">Add Premium For First Hourtype</wb:localize></wba:td> <wba:td align="right"> <%
boolean addPremiumForFirstHourtypeToken = "true".equals(helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN));
%> <input type="checkbox" name="AddPremiumForFirstHourtypeToken_check" <%= addPremiumForFirstHourtypeToken ? "checked" : "" %> onClick="if (this.checked) {this.form.AddPremiumForFirstHourtypeToken.value='true';} else {this.form.AddPremiumForFirstHourtypeToken.value='false';}"> <input type='hidden' name="<%=DailyOvertime24HourRule.PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN%>" value='<%= addPremiumForFirstHourtypeToken %>'> </wba:td> </wba:tr> <!-- (2) Work Detail Time Codes --> <wba:tr> <wba:td><wb:localize id="Work Detail Time Codes">Work Detail Time Codes</wb:localize></wba:td> <wba:td align="right"> <wb:controlField cssClass="inputField" submitName="<%=DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES%>" id="<%=DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES%>" ui="DBLookupUI" uiParameter='<%= TIME_CODE_SQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES)%></wb:controlField> </wba:td> </wba:tr> <!-- (3) Eligible Hour Types --> <wba:tr> <wba:td><wb:localize id="Eligible Hour Types">Eligible Hour Types</wb:localize></wba:td> <wba:td align="right"> <wb:controlField cssClass="inputField" submitName="<%=DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES%>" id="<%=DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES%>" ui="DBLookupUI" uiParameter='<%= HOUR_TYPE_SQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES)%></wb:controlField> </wba:td> </wba:tr> <!-- (4) Premium Time Codes Counted --> <wba:tr> <wba:td><wb:localize id="Premium Time Codes Counted">Premium Time Codes Counted</wb:localize></wba:td> <wba:td align="right"> <wb:controlField cssClass="inputField" submitName="<%=DailyOvertime24HourRule.PARAM_PREMIUM_TIMECODES_COUNTED%>" id="<%=DailyOvertime24HourRule.PARAM_PREMIUM_TIMECODES_COUNTED%>" ui="DBLookupUI" uiParameter='<%= TIME_CODE_SQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_PREMIUM_TIMECODES_COUNTED)%></wb:controlField> </wba:td> </wba:tr> <!-- (4.5) Discount Time Codes --> <wba:tr> <wba:td><wb:localize id="Discount Time Codes">Discount Time Codes</wb:localize></wba:td> <wba:td align="right"> <wb:controlField cssClass="inputField" submitName="<%=DailyOvertime24HourRule.PARAM_DISCOUNT_TIMECODES%>" id="<%=DailyOvertime24HourRule.PARAM_DISCOUNT_TIMECODES%>" ui="DBLookupUI" uiParameter='<%= TIME_CODE_SQL + " multiChoice=true"%>'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_DISCOUNT_TIMECODES)%></wb:controlField> </wba:td> </wba:tr> <!-- (5) Apply Based On Schedule --> <wba:tr> <wba:td><wb:localize id="Apply Based On Schedule">Apply OT to unscheduled hours</wb:localize></wba:td> <wba:td align="right"> <%
boolean applyBasedOnSchedule = "true".equals(helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_APPLY_BASED_ON_SCHEDULE));
%> <input type="checkbox" name="ApplyBasedOnSchedule_check" <%= applyBasedOnSchedule ? "checked" : "" %> onClick="if (this.checked) {this.form.ApplyBasedOnSchedule.value='true';} else {this.form.ApplyBasedOnSchedule.value='false';}"> <input type='hidden' name='<%=DailyOvertime24HourRule.PARAM_APPLY_BASED_ON_SCHEDULE%>' value='<%= applyBasedOnSchedule %>'> </wba:td> </wba:tr> <!-- (6) Premium Time Code Inserted --> <wba:tr> <wba:td><wb:localize id="Premium Time Code Inserted">Premium Time Code Inserted</wb:localize></wba:td> <wba:td align="right"> <wb:controlField cssClass="inputField" submitName="<%=DailyOvertime24HourRule.PARAM_PREMIUM_TIMECODE_INSERTED%>" id="<%=DailyOvertime24HourRule.PARAM_PREMIUM_TIMECODE_INSERTED%>" ui="DBLookupUI" uiParameter='<%= TIME_CODE_SQL + " multiChoice=false"%>'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_PREMIUM_TIMECODE_INSERTED)%></wb:controlField> </wba:td> </wba:tr> <!-- (6.1) Hour type For Overtime Work Details --> <wba:tr> <wba:td><wb:localize id="Hour type For Overtime Work Details">Hour type For Overtime Work Details</wb:localize></wba:td> <wba:td align="right"> <wb:controlField cssClass="inputField" submitName="<%=DailyOvertime24HourRule.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS%>" id="<%=DailyOvertime24HourRule.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS%>" ui="DBLookupUI" uiParameter='<%= HOUR_TYPE_SQL + " multiChoice=false"%>'><%=helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS)%></wb:controlField> </wba:td> </wba:tr> 


<!-- (7) 24 Hour Start Time --> <%
//Need to check for values from previous versions of the product
//id "24HourStartTime" does not work with TimeEditUI control - changed to "p24HourStartTime"
String twentyFourHourStartTime = "";
if (!helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME).equals("")){
	twentyFourHourStartTime = helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME);
}
else {
	twentyFourHourStartTime = helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME);
}
%> 
<wba:tr> 
<wba:td><wb:localize id="24 Hour Start Time">24 Hour Start Time</wb:localize></wba:td> 
<wba:td align="right"> <wb:controlField submitName="<%=DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME%>" 
                                        id='<%= "IAG_" + DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME %>'
                                        cssClass='inputField' 
                                        ui='StringUI' 
                                        uiParameter='width=33'
                                        ><%= twentyFourHourStartTime %></wb:controlField> </wba:td> </wba:tr> 

<!-- Max Days Look Back --> 
<wba:tr> 
<wba:td>
<wb:localize id="MAX_DAYS_LOOK_BACK">Max Days Look Back</wb:localize></wba:td> 
<wba:td align="right"> <wb:controlField submitName="<%= DailyOvertime24HourRule.PARAM_MAX_DAYS_LOOK_BACK %>" 
                                        id="<%= DailyOvertime24HourRule.PARAM_MAX_DAYS_LOOK_BACK %>" 
                                        cssClass='inputField' 
                                        ui='StringUI' 
                                        uiParameter='width=33'
                                        ><%= helper.getConditionSetParameterValue(condSetIndex.toInt(), DailyOvertime24HourRule.PARAM_MAX_DAYS_LOOK_BACK) %></wb:controlField> 
</wba:td> 
</wba:tr>


<!-- Assign Better Rate --> 
<wba:tr> <wba:td><wb:localize id="Assign Better Rate">Assign Better Rate</wb:localize></wba:td> <wba:td align="right"> <%
boolean assignBetterRate = "true".equals(helper.getConditionSetParameterValue(condSetIndex.toInt(),DailyOvertime24HourRule.PARAM_ASSIGN_BETTERRATE));
%> <input type="checkbox" name="AssignBetterRate_check" <%= assignBetterRate ? "checked" : "" %> onClick="if (this.checked) {this.form.AssignBetterRate.value='true';} else {this.form.AssignBetterRate.value='false';}"> <input type='hidden' name='<%=DailyOvertime24HourRule.PARAM_ASSIGN_BETTERRATE%>' value='<%= assignBetterRate %>'> </wba:td> </wba:tr> </wba:table> <% } %> <%
helper.makePageFooter(QuickRuleHelper.NEW_BUTTON + QuickRuleHelper.APPLY_BUTTON + QuickRuleHelper.CANCEL_BUTTON);
%> </wb:page>