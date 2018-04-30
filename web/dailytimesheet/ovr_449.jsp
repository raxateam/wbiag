<%@ include file="/dailytimesheet/dailyTypes.jsp"%> <%@ include file="/system/wbheader.jsp"%> <wb:page title="New Edit" popupPage="true"> <%
OvrTypes objOT = new OvrTypes(request, out, session, pageContext);
objOT.overrideTableTop();
objOT.getEmpskdActField("SHIFT_NAME");
objOT.getEmpskdActField("YESTERDAY");
objOT.overrideTableBottom();
%> </wb:page>