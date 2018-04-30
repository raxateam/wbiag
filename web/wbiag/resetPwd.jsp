<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.sql.*"%>
<%@ page import="com.workbrain.tool.mail.*"%>
<%@ page import="com.workbrain.app.ta.model.*"%>
<%@ page import="com.workbrain.app.ta.db.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.security.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>
<%-- ********************** --%>
<%!
	void saveMessage(DBConnection conn,
                     String from, String subject, String msg, int msgId) throws SQLException{
	PreparedStatement messagePS = conn.prepareStatement(
	                "insert into message " +
	                    "(   MSG_ID, " +
	                        "MSG_DATE, " +
	                        "MSG_SUBJECT, " +
	                        "MSG_BODY_LRG, " +
	                        "MSGTYP_ID, " +
	                        "WBU_NAME,  " +
	                        "WBU_NAME_ACTUAL )" +
	                "values (?," + conn.getDBType().encodeCurrentTimestamp() + ",?,?,?,?, ?) ");	        
	        
	        messagePS.setInt(1, msgId);
	        messagePS.setString(2, subject);
		messagePS.setString(3, msg);        
		messagePS.setInt(4, Util.MESSAGE_TYPE_MAIL);
		messagePS.setString(5, from);
		messagePS.setString(6, from);
        	messagePS.executeUpdate();
        }
        
        void saveRecipientMessage(  DBConnection conn, int msgId, int folderId,String status,String newOld,String to) throws SQLException
	{
	        PreparedStatement pstmt = conn.prepareStatement(
                "insert into recipient_message " +
                    "(   RCPMSG_ID, " +
                        "MSG_ID, " +
                        "MSGFLDR_ID, " +
                        "RCPMSG_RCV_STATUS, " +
                        "RCPMSG_NEW_OLD, " +
                        "WBU_NAME,  " +
                        "WBU_NAME_ACTUAL )" +
                "values (?,?,?,?,?,?, ? )");
	
	        pstmt.setInt(1,
	        conn.getDBSequence("SEQ_RCPMSG_ID").getNextValue());
	        pstmt.setInt(2,msgId);
	        pstmt.setInt(3,folderId);
	        pstmt.setString(4,status);
	        pstmt.setString(5,newOld);
	        pstmt.setString(6,to);
	        pstmt.setString(7, to);
	        pstmt.executeUpdate();
    	}
        
	boolean sendMail(DBConnection conn, String to,
                     String from, String subject, String msg) throws SQLException{	
        
        int msgId = conn.getDBSequence("SEQ_MSG_ID").getNextValue();
      	saveMessage(conn, from, subject,msg, msgId);        
       	saveRecipientMessage(conn,msgId, Util.FOLDER_INBOX, "TO","N",to);
        conn.commit();
        
        return true;
	}
%>

<html>
<head>
	<title>Reset Password</title>
	<META HTTP-EQUIV='Pragma' CONTENT='no-cache'>
	<script src='/system/wbutil.js'></script>
	<link href='/system/system.css' rel='stylesheet' type='text/css'>
	<link href='/workbrain.css' rel='stylesheet' type='text/css'>
</head>
<body leftmargin=0 topmargin=0 bgcolor='white' onload='if ( window.initializePage ) { initializePage(); }jumpToLastScrollPos();'>
<form name=page_form method='POST' action="resetPwd.jsp" onSubmit=" return wbValidateForm(); ">

<script language="JavaScript">
  function checkSubmit(){
      if(document.page_form.USER_NAME_RESET.value  == "") {
          alert("user name has to be supplied to reset password");
          return false;
      }
      if (!confirm("Are you sure you want to reset password?")) {
          return false;
      }
      disableAllButtons();
      document.page_form.submit();
      
  }
</script>
    <%     
    Value OPERATION = new Value( (String) request.getParameter("OPERATION") , true);
    
    String userName = request.getParameter("USER_NAME_RESET");
    String fromName = null;
    String toName = null;
     
    if("SUBMIT".equals(OPERATION.toString()))
    {
        DBConnection conn = new DBConnection( ConnectionManager.getConnection());
        boolean flag = true;
        int changedPwdTimes = 0;
        WorkbrainUserAccess acc = new WorkbrainUserAccess(conn);
        try
        {
            SecurityService.setCurrentClientId("1");
            WorkbrainUserData data = acc.loadByWbuName(userName);
                        
            if (data == null) {
            	throw new RuntimeException ("User :" +  userName + " could not be found");
            }
            if (StringHelper.isEmpty(data.getWbuEmail())) {
                throw new RuntimeException ("User :" +  userName + " does not have valid email defined. Please contact System Administrator");
            }
            if (!"Y".equals(data.getWbuActive())) {
                throw new RuntimeException ("User has been deactivated, please contact System Administrator");
            }
            String pwdnew = String.valueOf(System.currentTimeMillis());
            
            data.setWbuPassword(pwdnew);
            data.setWbuPwdChangedDate(DateHelper.DATE_1900 );
            data.setWbuBadLoginCount(0);            
            acc.update(data);
            
            fromName = (String) Registry.getVar("/system/wbiag/resetPwd/SENDER_WBU_NAME");    	    
            if (fromName == null )
            	throw new RuntimeException ("System registry is not set for sender.");
            
            sendMail(conn, data.getWbuName() , fromName, "Your Password Has been Reset", "Your password has been reset to : " + pwdnew);
                     
            conn.close();
        }
        catch(Exception e)
        {        	
            flag = false;
            if (conn != null)
            {
            	conn.rollback();
            	conn.close();
            }
            
    %>
        <span>The following error occured:</span>
        <P><%=e.getMessage()%></P>

    <%  }
    	finally
    	{
    		if (conn != null)
    			conn.close();
    	}
    	

        if(flag)
        {
        %>
            <span>Password has been reset successfully. You will receive an email with your new password momentarily. <br>
            </span>
        <%
        }
    }
    else {
    %>
    <table class="contentTable" cellspacing=0 style='border-width:1px;'>
        <tr>
            <th colspan='2'>
                Enter your username and reset password will be sent to the email associated with your account, if supplied.
            </th>
        </tr>
        <tr>
            <th width='40%'>
               User Name:
            </th>
            <td width='60%'>
              <INPUT type='text' Class='inputField' Name='USER_NAME_RESET' OnBlur=" " onChange=""  onFocus=""  Size="20" style='null'>
            </td>
        </tr>
    </table>
    <input type='HIDDEN' name='OPERATION' value='SUBMIT'>
    <button type="button" onClick="checkSubmit();" class="buttonMedium"  style="">Submit</button>
<%
    }
%>
    <button type="button" onClick="window.close()" class="buttonMedium"  style="">Close</button>
</form>
<SCRIPT>

function getLocalizedMessage_VALUE_REQUIRED(){
     return "Value is required.";
}

function getLocalizedMessage_INCORRECT_VALUE(){
     return "The value entered in the field is not correct.";
}

function initializePage() {
}
</body>