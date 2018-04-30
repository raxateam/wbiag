<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.*"%>
<%@ page import="com.workbrain.server.sql.*"%>
<%@ page import="com.workbrain.server.jsp.taglib.util.*"%>
<%@ page import="com.wbiag.server.cleanup.*"%>
<%-- ********************** --%>
<wb:page login="true">
<script language="JavaScript">
	function submitForm(operation) {
        if (Trim(document.forms[0].CLEANUP_PROPERTIES_FILE_PATH.value) == '') {
           alert ("Properties fle path must be supplied");
           return false;
        }
        if (document.forms[0].CONFIRMATION_ONLY.checked == false) {
           if (!confirm("CONFIRMATION_ONLY is off. You are about to purge records based on the properties file. Do you want to continue?")) {
             return false;
           }
        }
	    document.forms[0].OPERATION.value = operation;
        document.forms[0].submit();
	}
</script>
<%!
   String contextPath = null ;
   public String buttons (String mfrmId) {
     StringBuffer sb = new StringBuffer(200);
     sb.append("<table  class='contentTable' cellspacing=0 style='border-width:1px;'><tr>");
     sb.append("<td><button type='button' onClick=\"submitForm('SUBMIT');\" class='buttonMedium' >Submit</button></td>");
     sb.append("<td><button type='button' onClick=\"location.href = '../../maintenance/mntForms.jsp?mfrm_id=" + mfrmId + "'; return false;\" class='buttonMedium'  >Cancel</button></td>");
     sb.append("</tr></table>");
     return sb.toString();
   }
%>
<%
    contextPath = (String)request.getContextPath() ;
    String mfrm_id  = (String)request.getParameter("mfrm_id") ;
    String goBack =  "location.href = '../../maintenance/mntForms.jsp?mfrm_id=" + mfrm_id + "'; return false;";
    String spetId = (String)request.getParameter("PET_ID") ;

    if (StringHelper.isEmpty(mfrm_id)) {
        throw new RuntimeException ("mfrm_id must be supplied");
    }

    DBConnection conn = JSPHelper.getConnection(request);

    String operation = (String)request.getParameter("OPERATION") ;
    String propFilePath = (String)request.getParameter("CLEANUP_PROPERTIES_FILE_PATH") ;
    String confOnly = (String)request.getParameter("CONFIRMATION_ONLY") ;

    if ("SUBMIT".equals(operation)) {

        String errMsg =  null;
        CleanupProcess cp = null;
        try {
            cp = new CleanupProcess(conn , propFilePath);
            cp.getCleanupProcessContext().setConfirmationOnly("Y".equals(confOnly) ? true : false);
            cp.execute();
        }
        catch (Exception e){
          errMsg = e.getMessage() + "<br> Trace:" + StringHelper.getStackTrace(e) ;
          conn.rollback();
        }
        if (!StringHelper.isEmpty(errMsg)) {
        %>
            <span fgColor="red">Cleanup Process could not run. <br> Error Message: <%=errMsg %></span>
        <%
        }
        else {
        %>
            <span>Cleanup Process has run successfully.</span>
        <%
        }
        if (cp != null) {
%>
          <!-- logs -->
          <wba:table caption="Cleanup Logs" captionLocalizeIndex="Cleanup Logs">
<%
          Iterator iter = cp.getLogMessages().iterator();
          while (iter.hasNext()) {
            String item = (String) iter.next();
            out.println("<tr><td>" + item + "</td></tr>");
          }
%>
    </wba:table>
<%
        }
    }
%>

    <wba:table caption="Cleanup Process" captionLocalizeIndex="Cleanup Process">
        <wba:tr>
          <wba:th>
            <wb:localize id="CLEANUP_PROPERTIES_FILE_PATH">CLEANUP_PROPERTIES_FILE_PATH</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="CLEANUP_PROPERTIES_FILE_PATH" ui='StringUI' uiParameter='width=60'><%=propFilePath%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="CONFIRMATION_ONLY">CONFIRMATION_ONLY</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="CONFIRMATION_ONLY" ui='CheckboxUI' uiParameter=''><%=StringHelper.isEmpty(confOnly) ? "Y" : confOnly%></wb:controlField>
          </wba:td>
        </wba:tr>
    </wba:table>
    <wb:submit id="mfrm_id"><%=mfrm_id%></wb:submit>
    <wb:submit id="OPERATION"></wb:submit>
<%
    out.println(buttons(mfrm_id));
%>
</wb:page>
