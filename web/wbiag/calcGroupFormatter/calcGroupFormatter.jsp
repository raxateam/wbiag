<%@ include file="/system/wbheader.jsp"%>
<%@ taglib uri="/wbsys" prefix="wb" %>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="javax.servlet.*"%>
<%@ page import="java.sql.PreparedStatement,java.sql.ResultSet,java.sql.Timestamp"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.server.jsp.*" %>
<%@ page import="com.workbrain.app.ta.db.*,com.workbrain.app.ta.model.*" %>
<wb:page >
<%!
    public static final String STYLESHEET = "<?xml-stylesheet type=\"text/xsl\" href=\"calcGroupFormatter.xsl\"?>";

    public boolean createFile(String file, String xml) throws IOException {
        if (FileUtil.fileExists(file)) {
          FileUtil.deleteFile(file);
        }
        PrintWriter pw = null;
        try {
            pw = new PrintWriter( new FileOutputStream( file, true ) );
            pw.println(STYLESHEET);
            pw.println(xml);
        }
        finally {
            if (pw != null) {
                pw.close();
            }
        }
        return true;
    }
%>

<%
    DBConnection conn = JSPHelper.getConnection(request);
    String calcgrpId = request.getParameter("calcGroupId");
    if (StringHelper.isEmpty(calcgrpId)) {
      throw new RuntimeException ("calcgrpId must be supplied in query string");
    }

    String XMLFileName = "calcGroup_" + calcgrpId + ".xml";
    String forwardPage = "/wbiag/calcGroupFormatter/" + XMLFileName;

    CalcGroupData cg = new CalcGroupAccess(conn).load(Integer.parseInt(calcgrpId))  ;
    if (StringHelper.isEmpty(cg.getCalcgrpXml())) {
      out.println("<h3>Calc Group XML is empty</h3>");
    }
    else {
      String forwardPageRealPath = application.getRealPath(forwardPage) ;
      if (createFile(forwardPageRealPath , cg.getCalcgrpXml())) {
%>
        <jsp:forward page="<%=forwardPage%>"/>
<%
      }
      else {
        out.println("<h3>Could not create the file, check file system privileges</h3>");
      }
    }
%>
</wb:page>