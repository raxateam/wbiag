<%@ page import="com.wbiag.tool.regressiontest.util.RequestHelper" %>
<%@ page import="com.wbiag.tool.regressiontest.report.*" %>
<%@ page import="com.workbrain.server.jsp.JSPHelper" %>
<%@ page import="com.workbrain.sql.DBConnection" %>


<%!
    ReportDisplaySuite reportObj = null;
%>

<%
	// NOTE: testSuiteType is a String that must be defined in the jsp that
	// includes this page.

	// Get the report Id from the request.
 	int reportId = 0;
 	
 	// Look in the parameters.
	String reportIdObj = request.getParameter("reportId");
	
	// Look in the attributes.
	if (reportIdObj != null) {
		reportId = Integer.parseInt(reportIdObj);
	} else {
		reportId = RequestHelper.getReportId(request);
	}

	if (reportId == 0) {
		throw new Exception("reportId not found in request.");
	}
 	
   	DBConnection conn = JSPHelper.getConnection(request);

	// Get the IReportDisplayGenerator object for this test suite type.
	IReportDisplayGenerator displayGenerator = ReportDisplayGeneratorFactory.getInstance(testSuiteType);

	// Generate the ReportDisplaySuite object to be used by the JSP that
	// included this page.
	try {
	    reportObj = displayGenerator.generateReportDisplay(reportId, conn);
	} catch (Exception e) {
	    e.printStackTrace();
	}
%>
