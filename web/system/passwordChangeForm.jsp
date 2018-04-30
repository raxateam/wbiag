<%--
//***************************************************************************************************************************
//
//	PURPOSE:				Allow user to change password.
//
//	DEPENDENCIES:
//
//
//	MODIFICATION HISTORY:	NONE TO DATE
//
//****************************************************************************************************************************

--%>

<%@ include file="/system/wbheader.jsp"%>
<%@ page import="com.workbrain.app.jsp.workbrain.etm.*"%>
<%@ page import="com.workbrain.security.*"%>
<%@ page import="com.workbrain.security.exception.*"%>
<%@ page import="com.workbrain.security.session.*"%>
<%@ page import="com.workbrain.tool.renderer.bo.html.*,
                 javax.servlet.*,
                 javax.servlet.http.*,
                 com.workbrain.server.*,
                 com.workbrain.util.*,
                 com.workbrain.sql.*,
                 com.workbrain.server.jsp.SecurityHelper,
                 com.workbrain.server.jsp.locale.LocalizationDictionary" %>

<wb:page domain="workbrain_system">
<%!
   final String OLD_PWD_CHANGE_COUNT = "system/wbiag/PWD_VALIDATE_ONE_OF_LAST_X_PWDS";

   /**
   *  Checks PWD_VALIDATE_ONE_OF_LAST_X_PWDS registry to see if user is attempting to change
*  *  password to any of last X passwords. If 0, ignore
   */
   boolean changePassword(HttpServletRequest request,
                       String currPwd , String newPwd) throws Exception{
        int userId = Integer.parseInt(JSPHelper.getWebLogin(request).getUserId());
        int valLastPwdCnt = com.workbrain.server.registry.Registry.getVarInt(OLD_PWD_CHANGE_COUNT, 0);
        if (valLastPwdCnt <= 0) {
 		    return new SecurityService().changePassword(currPwd, newPwd);
        }
		java.util.List oldPwds = new java.util.ArrayList();
		DBConnection conn = JSPHelper.getConnection(request);
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		try {
		    StringBuffer sb = new StringBuffer(200);
		    sb.append("SELECT  wupw_password FROM wbiag_wbuser_pwds WHERE wbu_id = ? ORDER BY wupw_pwd_changed_date DESC");
		    ps = conn.prepareStatement(sb.toString());
		    ps.setInt(1 , userId);
		    rs = ps.executeQuery();
            int cnt = 0;
		    while (rs.next()) {
                if (++cnt <= valLastPwdCnt) {
		            oldPwds.add(rs.getString(1));
                }
		    }
		}
		finally {
		    if (rs != null) rs.close();
		    if (ps != null) ps.close();
		}

		String hashedPwd
		  = com.workbrain.security.WorkbrainAuthenticator.hashPassword(newPwd);

		if (oldPwds.contains(hashedPwd)) {

            java.util.List args = new java.util.ArrayList();
            args.add(String.valueOf(valLastPwdCnt));
            String errorMsg = LocalizationDictionary.localizeMessage(conn ,
                "PWD_VALIDATE_ONE_OF_LAST_X_PWDS",
                "New password can't be one of last " + valLastPwdCnt + " passwords\n",
                args ,
                JSPHelper.getWebLogin(request).getDefaultLocale().getId());
		    throw new WorkbrainSecurityException(errorMsg);
		}
		return new SecurityService().changePassword(currPwd, newPwd);
   }
%>
<%--tt#5912-vrusu-Jun05,2002 --%>
<input type=HIDDEN name="passHidden" value="false">
<jsp:useBean id="etmcp" scope="page" class="com.workbrain.app.jsp.workbrain.etm.EtmPwdChange"/>
<jsp:useBean id="pa" scope="page" class="com.workbrain.server.PasswordAlert"/>
<%
    String pA = request.getParameter("passwordAlert");
    pa.setUserId(Integer.parseInt(JSPHelper.getWebContext(pageContext).getLogin().getUserId()));
	pa.setConnection(JSPHelper.getWebContext(pageContext).getConnection());
	String str = pa.getPasswordAlertMessage();
    JSPHelper.getWebContext(pageContext).getConnection().commit();
	if (pa.withinAlertPeriod()){%>
	<script>
		if (!confirm('<%=str%>')){
			document.location = contextPath + "\menu.jsp";
		}
	</script>
	<%}
    if (pA != null){
       pa.setConfirmResult(pA);
       if (pA.equalsIgnoreCase("true")){%>
       <script type='text/javascript'>

           window.document.page_form.passHidden.value = "true";

       </script>
       <%}
    } else {
        pA = request.getParameter("passHidden");
        pa.setConfirmResult(pA);
    }%>
<%--tt#5912-vrusu-Jun05,2002 --%>
<%-- ********************************************* Variable declaration ***************************************************--%>
<%-- User variable --%>
<wb:define id="userId"><wb:getPageProperty id="userId"/></wb:define>
<wb:define id="userName"><wb:getPageProperty id="userName"/></wb:define>
<wb:define id="employeeId"><wb:getPageProperty id="employeeId"/></wb:define>

<%-- Page variable --%>
<wb:define id="chgPwd"><wb:get id="chgPwd" scope="parameter" default="false"/></wb:define>
<wb:define id="processPasswordChange"><wb:get id="processPasswordChange" scope="parameter" default="false"/></wb:define>
<wb:define id="pwdProcessed"><wb:get id="pwdProcessed" scope="parameter" default="false"/></wb:define>
<wb:define id="changeForced"><wb:get id="forced" scope="parameter" default="false"/></wb:define>

<wb:config id="New_password_cannot_be_blank"/>
<wb:config id="New_pwd_same_as_current_pwd"/>
<wb:config id="Pwd_and_confirm_pwd_do_not_match"/>
<wb:config id="Max_length_of_New_Pwd_is_"/>
<wb:config id="Min_length_of_New_Pwd_is_"/>
<wb:config id="Password_must_be"/>
<wb:config id="character"/>
<wb:config id="characters"/>
<wb:config id="Password_must_be_no_more_then"/>
<wb:config id="Password_must_be_at_least"/>
<wb:config id="Invalid_current_password"/>
<wb:config id="Password_changed." />

<%-- Password format parameter --%>
<wb:define id="errMsgIndex,errMsg,pwdAlpha,pwdNumeric,minPwdLength,maxPwdLength,wbparm_name"/>
<wb:set id="maxPwdLength"><%=etmcp.getWorkbrainParameter("PWD_MAX_LENGTH")%></wb:set>
<wb:set id="minPwdLength"><%=etmcp.getWorkbrainParameter("PWD_MIN_LENGTH")%></wb:set>
<wb:set id="pwdNumeric"><%=etmcp.getWorkbrainParameter("PWD_IS_NUMERIC")%></wb:set>
<wb:set id="pwdAlpha"><%=etmcp.getWorkbrainParameter("PWD_IS_ALPHABET")%></wb:set>

<%-- Javascript password validation functions --%>
<script type='text/javascript'>
    function redirect(url){
        document.location = url;
    }

    function chkPwd(){
        <%@ include file="/system/pwdCheck.inc" %>
        return true;
    }
</script>


<%-- Change password --%>
<wb:if expression="#processPasswordChange#">
    <wb:define id="currPwd"><wb:get id="currPwd" scope="parameter"/></wb:define>
    <wb:define id="newPwd"><wb:get id="newPwd" scope="parameter"/></wb:define>
    <wb:define id="confPwd"><wb:get id="confPwd" scope="parameter"/></wb:define>

    <%-- Set error message, if no error, set it to blank later in statement --%>
    <wb:set id="errMsg"><wb:localize id="Password_change_unsuccessful._Please_Try_Again.">Password change unsuccessful. Please Try Again.</wb:localize></wb:set>
    <% try{
        changePassword(request, currPwd.toString(), newPwd.toString());
        %>
        <wb:set id="pwdProcessed">true</wb:set>
        <wb:set id="errMsg"></wb:set>
        <%
        WorkbrainAuthenticatedUser currentUser = (WorkbrainAuthenticatedUser)
            SecurityService.getCurrentUser();
        currentUser.setPasswordExpired(false);
        SecurityService.setCurrentUser(currentUser);
        com.workbrain.server.jsp.JSPHelper.getWebLogin(request).setPasswordChangeRequired(false);
    }catch(WorkbrainAuthenticationException ex){
        %>
        <wb:set id="errMsg"><wb:localize id="Invalid_current_password">Invalid current password. Please try again.</wb:localize></wb:set>
        <%
    } catch(WorkbrainSecurityException ex) {
    	String locErrMsg = LocalizationDictionary.get().localizeErrorMessage(
    			JSPHelper.getConnection(request),
    			ex.getMessage(),
    			JSPHelper.getWebLocale(request).getLanguageId() );
        %>
        <wb:set id="errMsg"><%=locErrMsg%></wb:set>
        <%
    } catch(Exception e) {
         //Format sql Error message to user friendly message.
         String eError = e.toString();
         String errStr = "";
         int errStrEnd = eError.indexOf('\n',1);
         errStr = eError.substring(27,errStrEnd);
         errStr = errStr.substring(errStr.indexOf(":")+1, errStr.length());
       %>
       <wb:set id="errMsgIndex"><%=StringHelper.searchReplace(errStr, " ", "_")%></wb:set>
       <wb:set id="errMsg"><wb:localize id="#eErrorStr#"><%=errStr%></wb:localize></wb:set>
       <%
   }
   %>
</wb:if>


<%-- ********************************************* Body Starts ***************************************************--%>

<%-- Redirect page to menu.jsp IF password change is processed --%>

    <%-- Needs to be Java if, currently forwar does not work from inside a body tag on WebLogic--%>
    <wb:define id="heading"><wb:localize id="Password_Change">Password Change</wb:localize></wb:define>

    <%if("true".equals(pwdProcessed.toString())){%>
        <wb:define id="success"><wb:localize id="Password_changed.">Your password has been updated successfully.</wb:localize></wb:define>
        <wb:define id="forward_to"/>
        <%
        String fwdUrl = "/menu.jsp?passwordAlert=" + pa.getConfirmResult();
        String url = request.getParameter("URL");
        if(url != null) {
            fwdUrl += "&URL=" + url;
        }
        fwdUrl = java.net.URLEncoder.encode(fwdUrl);
        %>
        <wb:set id="forward_to"><%=fwdUrl%></wb:set>
        <wb:forward page="/interface/message.jsp?heading=#heading#&msg=#success#&forward_to=#forward_to#"/>
    <%} else {%>
        <%-- If password change is not processed display change fields --%>
        <center>
        <BR><BR><BR>
        <table cellpadding=0 cellspacing=0 class='contentTableOutline' border=0><tr><td colspan=3><img src='<%= request.getContextPath() %>/images/pixel.gif' width=1 height=1></td></tr><tr><td><img src='images/pixel.gif' width=1 height=1></td><td bgcolor=white>

        <table border=0 width=393 cellpadding=0 cellspacing=0 class=generic>
            <tr>
                                <td colspan=2 bgcolor=#0765B4 background="<%= request.getContextPath() %>/images/interface/logoLogin.gif" bgcolor="#0061B0" height=52 style="padding-top:21px;padding-left:4px"><div class="headingLight" style="color:#194A7A"><%=heading.toString()%></div></td>
            </tr>

            <tr>
                <td colspan=2 align=center height=42><span class=textMediumSmall><wb:localize id="Pass_Change_Protects_Privacy">Periodic password changes protect the privacy of your information.</wb:localize></span></td>
            </tr>
            <tr>
                <td colspan=2 align=left class=passChangeRequirements>
                <wb:switch>
                    <wb:case expression="#errMsg#" compareToExpression="" operator="<>">
                        <center>
                        <font color=green><wb:get id="errMsgIndex"/></font>
                        <span class=textAlert>
                             <wb:localize id="#errMsgIndex#"><wb:get id="errMsg"/></wb:localize>
                        </span>
                        </center>
                    </wb:case>
                    <wb:case>
                        <ul class='listDanish'>

             <%if (Integer.parseInt(minPwdLength.toString()) == Integer.parseInt(maxPwdLength.toString())) {%>
                <li><span class=textDark><wb:localize id="Password_must_be">Password must be exactly</wb:localize>&nbsp;<%=maxPwdLength%>
                <%if (Integer.parseInt(maxPwdLength.toString()) > 1) {%>
                <wb:localize id="characters"> characters.</wb:localize>
                <%} else {%>
                    <wb:localize id="character"> character.</wb:localize>
                <%}%>
                </span></li>
             <%}else if (Integer.parseInt(minPwdLength.toString()) == 0 && Integer.parseInt(maxPwdLength.toString()) <= 50) {%>
                <li><span class=textDark><wb:localize id="Password_must_be_no_more_then">Password must be no more than </wb:localize><%=maxPwdLength%>
                <%if (Integer.parseInt(maxPwdLength.toString()) > 1) {%>
                <wb:localize id="characters"> characters.</wb:localize>
                <%} else {%>
                    <wb:localize id="character"> character.</wb:localize>
                <%}%>
                </span></li>
                <%}else if(Integer.parseInt(minPwdLength.toString()) > 0 && Integer.parseInt(maxPwdLength.toString()) > 50) {%>
                <li><span class=textDark><wb:localize id="Password_must_be_at_least">Password must be at least </wb:localize><%=minPwdLength%>
                <%if (Integer.parseInt(minPwdLength.toString()) > 1) {%>
                <wb:localize id="characters"> characters.</wb:localize>
                <%} else {%>
                    <wb:localize id="character"> character.</wb:localize>
                <%}%>
                </span></li>
                <%}else if(Integer.parseInt(minPwdLength.toString()) > 0 && Integer.parseInt(maxPwdLength.toString()) <= 50) {%>
                <li><span class=textDark><wb:localize id="Password_must_be_at_least">Password must be at least </wb:localize><%=minPwdLength%><wb:localize id="but_no_more_than">, but no more than</wb:localize>&nbsp;<%=maxPwdLength%>
                <%if (Integer.parseInt(maxPwdLength.toString()) > 1) {%>
                <wb:localize id="characters"> characters.</wb:localize>
                <%} else {%>
                    <wb:localize id="character"> character.</wb:localize>
                <%}%>
                </span></li>
                <%}%>

                            <%--<li>New password must be different from the current password</li>
                            <li>Password can not be the same as your User Name.</li>--%>
                        </ul>
                    </wb:case>
                </wb:switch>
                </td>
            </tr>
            <!--tt #6540 vrusu - 10/04/2002; removed maxlength=maxPwdLength from password fields-->
            <tr height=32>
                <td width=200><div class=textDarkMedium style='padding-left:6px'><wb:localize id="Current_Password">Current Password</wb:localize></div></td>
                <td><input type=password class=inputField name=currPwd size=20></td>
            </tr>

            <tr height=32>
                <td width=200><div class=textDarkMedium style='padding-left:6px'><wb:localize id="New_Password">New Password</wb:localize></div></td>
                <td><input type=password class=inputField name=newPwd size=20></td>
            </tr>

            <tr height=32>
                <td width=200><div class=textDarkMedium style='padding-left:6px'><wb:localize id="Confirm_New_Password">Confirm New Password</wb:localize></div></td>
                <td><input type=password class=inputField name=confPwd size=20></td>
            </tr>

            <tr height=45>
                <td><img src="<%= request.getContextPath() %>/images/pixel.gif" width=1 height=45/></td>

                <td valign=middle>
                    <wba:button type="submit" label="Change Password" labelLocalizeIndex="Change_Password"/>
                </td>
            </tr>

            <tr bgcolor="#0765B4" height="21">
                <td width=1><img src="<%= request.getContextPath() %>/images/pixel.gif" width=1 height=21/></td>
                <td align=left valign=center>
                <% if (pa.getConfirmResult() == true){%>
                   <a href="#" onClick="window.close(); window.opener.location='<%= request.getContextPath() %>/menu.jsp?pageAction=logout'" class="linkLightSmall"><wb:localize id="Cancel">Cancel</wb:localize></a>
                <%} else if ("true".equalsIgnoreCase(changeForced.toString())){%>
                   <a href="<%= request.getContextPath() %>/login.jsp?cancelled=true" class="linkLightSmall"><wb:localize id="Cancel">Cancel</wb:localize></a>
                <%} else {%>
                   <a href="<%= request.getContextPath() %>/menu.jsp" class="linkLightSmall"><wb:localize id="Cancel">Cancel</wb:localize></a>
                <%}%>
                </td>
            </tr>
        </table>
        </td><td><img src='<%= request.getContextPath() %>/images/pixel.gif' width=1 height=1></td></tr><tr><td colspan=3><img src='images/pixel.gif' width=1 height=1></td></tr></table>

        </center>

        <input type=hidden name=processPasswordChange value='false'>
        <script type='text/javascript'>
            function wbValidateForm(){
                if (chkPwd()) {
                    document.forms[0].processPasswordChange.value='true';
                    return true;
                } else {
                    return false;
                }
            }
        </script>
    <%}%>
    <% if (request.getParameter("URL") != null) {%>
        <input type='hidden' name='URL' value='<%=request.getParameter("URL")%>' />
    <%}%>
</wb:page>
