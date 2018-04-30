<%@ include file="/system/wbheader.jsp"%>

<%@ page import="com.wbiag.tool.regressiontest.util.RequestHelper" %>
<%@ page import="com.wbiag.tool.regressiontest.access.TestReportAccess" %>
<%@ page import="com.wbiag.tool.regressiontest.model.TestReportData" %>
<%@ page import="com.workbrain.server.jsp.JSPHelper" %>
<%@ page import="com.workbrain.sql.DBConnection" %>

<%@ page import="javax.xml.transform.TransformerFactory"%>
<%@ page import="javax.xml.transform.Transformer"%>
<%@ page import="javax.xml.transform.stream.StreamSource"%>
<%@ page import="javax.xml.transform.stream.StreamResult"%>
<%@ page import="java.io.*"%>

<wb:page>

<%
 try{
	// Get the report Id from the request.
 	int reportId = 0;
 	
 	// Look in the parameters.
	String reportIdObj = request.getParameter("reportId");
	
	// Look in the attributes.
	try{
		if (reportIdObj != null) {
			reportId = Integer.parseInt(reportIdObj);
		} else {
			reportId = RequestHelper.getReportId(request);
		}
	}catch(Exception e){
		throw new Exception("Invalid reportId.");
	}

	if (reportId == 0) {
		throw new Exception("reportId not found in request.");
	}
 	
 	// Get the report XML from the database.
   	DBConnection conn = JSPHelper.getConnection(request);

 	TestReportData report = null;
 	TestReportAccess reportAccess = new TestReportAccess(conn);
 	report = reportAccess.getReport(reportId);
 	String reportXML = report.getReportOutput();
 
    // Create a transform factory instance.
    TransformerFactory tfactory = TransformerFactory.newInstance();
    
    // Create a transformer for the stylesheet.
    Transformer transformer = null;
    ByteArrayOutputStream bs = null;
    
    try {
    	String xslFilename = request.getParameter("reportXSLFilename");
    	if (xslFilename == null) {
    		xslFilename = "payRules-report.xsl";
    		System.out.println("xsl filename not found.  Using default: " + xslFilename);
    	}
    	
		String xslPath = application.getRealPath("/regressiontest/report") 
							+ "/" + xslFilename;

	    File f = new File(xslPath);
	    
      	transformer = tfactory.newTransformer(
                            new StreamSource(f)
                            );
    
        bs = new ByteArrayOutputStream();
        
        // Transform the source XML.
        transformer.transform( new StreamSource(new StringReader(reportXML)),
                               new StreamResult(bs));
    } catch (Exception e) {
        e.printStackTrace();
        throw e;
    }
    
	// Output the results.
    out.println(bs.toString());

 } catch(Exception e) {
      out.println(e);
 }
 
%>

</wb:page>