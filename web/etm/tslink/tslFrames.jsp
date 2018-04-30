<%@ include file="/system/wbheader.jsp"%> <%@ page import  = "com.workbrain.server.*"%> <%@ page import  = "com.workbrain.server.jsp.*"%> <%@ page import	 = "com.workbrain.util.*"%> <%@ page import	 = "java.util.*"%> <%@ page import	 = "java.text.*"%> <%@ page import  = "com.workbrain.server.data.*"%> <%@ page import  = "javax.naming.*"%> <wb:page type="VR" emitHtml="false" subsidiaryPage="true"> <wb:define id="empID"><wb:getPageProperty id='employeeId'/></wb:define> <%
session.setAttribute(WebConstants.WEB_SESSION_ATTRIBUTE_NAME, JSPHelper.getWebSession(request));
java.util.Date currentDate = DateHelper.truncateToDays(new java.util.Date());
SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd HHmmss");
StringBuffer content = new StringBuffer("/dailytimesheet/ovrRetrieveIds.jsp");
content.append("?EMP_ID_0=");
content.append(empID.toString());
content.append("&START_DATE_0=");
content.append(df.format(currentDate));
content.append("&END_DATE_0=");
content.append(df.format(currentDate));
/* DATE_SELECT=5 points to the current week */
content.append("&WBT_ID_0=&PAYGRP_ID_0=&CALCGRP_ID_0=&DATE_SELECT=5&AUTH_SELECT=0&VIEW_SELECT=0&ORDER_SELECT=0&SUBMIT_PARAMS=T");
%> <frameset id="TopFrame" rows="28,*" border=0 spacing=0 frameborder=0 framespacing=0 MARGINWIDTH=0 MARGINHEIGHT=0> <frame name="headerFrame" src="<%= request.getContextPath() %>/etm/tslink/tslHeader.jsp" border=0 frameborder=No scrolling="No" noresize marginwidth="0" marginheight="0" framespacing="0"> <frame name="contentFrame" src="<%= request.getContextPath() %><%=content.toString()%>" marginwidth="0" marginheight="No" frameborder="0" scrolling="Auto" framespacing="0"> </frameset> </wb:page> 