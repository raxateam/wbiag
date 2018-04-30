<%@ include file="/system/wbheader.jsp"%> <%@ taglib uri="/wbsys" prefix="wb" %> <%@ page import="java.io.*"%> <%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMWTShelper"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMBiWeeklyTimeSheetConstants"%>
<%@ page import="com.workbrain.server.cache.WorkbrainCache"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>
<%@ page import="org.apache.log4j.*"%>
<%-- ********************** --%>

<%!
private static Logger logger = Logger.getLogger("jsp.biweeklyTimeSheet.CSMBiweeklyTimesheet");
private static int MFRM_ID = -99;
%>
<%
	MFRM_ID = CSMWTShelper.getMfrmId(request);
%>
<wb:page login='true' maintenanceFormId='<%=MFRM_ID%>'>
<wb:define id="none"><wb:localize id="None" ignoreConfig="true">None</wb:localize></wb:define>
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<wb:define id="SCHEDULE_VALUE"><wb:get id="SCHEDULE_VALUE" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location='/timesheet/CSMSchedule.jsp';</wb:define>

<%
	if ("SUBMIT".equals(OPERATION.toString())) {

		//System.out.println(CSMBiWeeklyTimeSheetConstants.CSM_SCHEDULE);
		//System.out.println(SCHEDULE_VALUE.toString());
		RegistryHelper rh = new RegistryHelper();
        rh.setVar(CSMBiWeeklyTimeSheetConstants.CSM_SCHEDULE, SCHEDULE_VALUE.toString());
		Registry.unloadCache();
%>
		<Span>Schedule value updated successfully</Span>
		<BR>
		<wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>
	<%
	} else {

		String schedule="";
		try {
			schedule = (String)Registry.getVar(CSMBiWeeklyTimeSheetConstants.CSM_SCHEDULE);
		} catch (NamingException e) {
		}
	%>
		<wba:table caption="Set Schedule Value Form" captionLocalizeIndex="Set Schedule Value Form" width="200">

			<wba:tr>
				<wba:th width='30%'>
					<wb:localize id="CSM_SKD_VAL">Schedule Value</wb:localize>
				</wba:th>
				<wba:td width='70%'>
					<wb:controlField cssClass="inputField" submitName="SCHEDULE_VALUE" nullable="false" ui="StringUI" uiParameter="width=60"><%=schedule%></wb:controlField>
				</wba:td>
			</wba:tr>

		</wba:table>

		<BR>

		<wb:submit id="OPERATION">SUBMIT</wb:submit>
		<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick=''/>&nbsp;
<%	}%>

</wb:page>

