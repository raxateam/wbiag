<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.*, java.util.*, javax.servlet.*, javax.servlet.http.*" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.sql.*" %>
<%@ page import="com.workbrain.server.*" %>
<%@ page import="com.workbrain.server.registry.Registry" %>
<%@ page import="com.workbrain.server.jsp.*" %>
<%@ page import="com.workbrain.sql.*" %>
<%@ page import="com.workbrain.app.ta.db.RecordAccess" %>
<%@ page import="com.workbrain.app.ta.model.RecordData" %>
<%@ page import="com.workbrain.util.PwdConfig" %>
<%@ page import="org.apache.soap.encoding.soapenc.Base64" %>


<wb:page title="Password Reminder" cssClass="login" login='false'>

<%
String parameters = "?config=false&locale=" + 
                com.workbrain.server.WebSystem.getVariable("DefaultLocale") +
                (request.getParameter("URL") == null ? "" : "&URL=" + 
                URLEncoder.encode(request.getParameter("URL")));

String fromPage = request.getParameter("from");
String toDo = request.getParameter("to_do");
String wbuName = request.getParameter("WBU_NAME");
String question = request.getParameter("Q");
String answer = request.getParameter("A");
String newPassword = null;
DBConnection conn = JSPHelper.getConnection(request);
boolean answerCorrect = false;
boolean isValidUser = false;
boolean isAnswerSetup = false;
String errMsg = null;

if ((wbuName == null) || (wbuName.trim().length() == 0)) {
	wbuName = "";
}

if ((toDo == null) || (toDo.trim().length() == 0)) {
	toDo = "show";
	question = "";
} else if ("showQ".equals(toDo)) {
	// get qustion and display
	isValidUser = validateUser(conn, wbuName);
	if (isValidUser) {
		question = getQuestionByUser(conn, wbuName);
		if (question == null) {
			isAnswerSetup = false;
		} else {
			isAnswerSetup = true;
		}
	} else {
		question = "";
	}
} else if ("checkA".equals(toDo)) {
	// check answer
	isValidUser = validateUser(conn, wbuName);
	if (isValidUser) {
		question = getQuestionByUser(conn, wbuName);
		if (question == null) {
			isAnswerSetup = false;
		} else {
			isAnswerSetup = true;
		}
	}
	answerCorrect = checkAnswer(conn, wbuName, answer);
	if (answerCorrect) {
		newPassword = changePassword(conn, wbuName);
	}
}

%>

<script language="javascript">
	
	function submitUserName() {
		if (document.forms[0].WBU_NAME.value == "") {
			alert("Please enter your user name!");
			document.forms[0].WBU_NAME.focus();
			return false;
		}

		document.forms[0].to_do.value = "showQ";
		document.forms[0].submit();

	}

	function checkAnswer() {
		if (document.forms[0].A.value == "") {
			alert("Please enter your answer!");
			document.forms[0].A.focus();
			return false;
		}

		document.forms[0].to_do.value = "checkA";
		document.forms[0].submit();

	}


</script>

<input type="hidden" name="to_do" value=""/>
<input type="hidden" name="from" value="<%=fromPage%>"/>

<div class='separatorLarge'/>
<div  class='contentTableTitle'><wb:localize id="PASSWORD QUESTION">User Password Verification Question</wb:localize></div>
<div class='separatorLarge'/>

<table cellspacing='2' cellpadding='2' border='0' width='750'>
  <tr>
    <td colspan=2 align=right>
<%		if ("e".equals(fromPage)) {  // from ETM
%>
			<a href='/etm/login.jsp'>
<%		} else {
%>
			<a href='/login.jsp'>
<%		} 
%>
		<wb:localize id="Back to Login">Back to Login</wb:localize></a>

<%		if (!"e".equals(fromPage)) {  // from ETM %>
            &nbsp;
            <a href='/wbiag/password/passwordReminder_fr.jsp'>[Fran&#231;ais]</a>
<%      } %>

	</td>
  </tr>


  <tr bgcolor="#E7E7E7">
    <td colspan=2>
		<b><wb:localize id="PASSWORD QUESTION">Forgotten Your Password?</wb:localize></b>
	</td>
  </tr>

  <tr>
    <td colspan=2>
		<wb:localize id="If you forgot your password">If you've forgotten the password to your account, please enter your user name and answer question below. If you answer the question correctly, we will provide you with a new password. Then you can login and immediately change your password. If you could not answer the question correctly, you need to contact your managers of security in your store (HR, Operations managers).</wb:localize>
	</td>
  </tr>
  <tr>
    <td colspan=2>&nbsp;</td>
  </tr>

  <tr bgcolor="#E7E7E7">
    <td colspan=2>
		<b><wb:localize id="1. Enter Your User Name">1. Enter Your User Name</wb:localize></b>
	</td>
  </tr>

  <tr>
	<td nowrap=true><wb:localize id="Your User Name">Your User Name</wb:localize>:</td>
	<td>
	  <wb:controlField cssClass="inputField"
					   submitName="WBU_NAME"
					   ui='StringUI'
					   uiParameter='width=40'
					   onChange = ''><%=wbuName%></wb:controlField>
	  <wba:button name="Go" label="Go" labelLocalizeIndex="Go" size="small" onClick="submitUserName(); return false;"/>
	</td>
  </tr>

  <tr>
    <td></td>
    <td>
		<%
			if (("showQ".equals(toDo) || "checkA".equals(toDo)) && !isValidUser) {
		%>
			<span style="font-size:20px; color:rgb(255,0,0);">
			  <wb:localize id="Invalid User">Invalid User</wb:localize>
			</span>
		<%
			}
		%>
		&nbsp;
	</td>
  </tr>

  <tr bgcolor="#E7E7E7">
    <td colspan=2 >
		<b><wb:localize id="2. Enter Answer">2. Enter Answer</wb:localize></b>
	</td>
  </tr>

  <tr>
	<td><wb:localize id="Question">Question</wb:localize>:</td>
	<td>
		<%
			if (isValidUser && !isAnswerSetup) {
		%>
			<span style="font-size:20px; color:rgb(255,0,0);">
			  <wb:localize id="No Question setup yet">No question and answer has been set up yet for this user!</wb:localize>
			</span>
		<%
			} else {
				out.println(question);
			}
		%>
	</td>
  </tr>

  <tr>
	<td><wb:localize id="Answer">Answer</wb:localize>:</td>
	<td>
	  <wb:controlField cssClass="inputField"
					   submitName="A"
					   ui='StringUI'
					   uiParameter='width=60'
					   onChange = ''><%=answer%></wb:controlField>
	  <wba:button name="Go" label="Go" labelLocalizeIndex="Go" size="small" onClick="checkAnswer(); return false;"/>
	</td>
  </tr>

</table>

<div class="separatorLarge"/>

<%
	if ("checkA".equals(toDo)) {
		if (answerCorrect) {
%>
			<table cellspacing='2' cellpadding='2' border='0' width='750'>
			  <tr bgcolor="#E7E7E7">
				<td colspan=2>
					<b><wb:localize id="Your answer is correct">Your answer is correct</wb:localize>!</b>
				</td>
			  </tr>
			  <tr>
				<td colspan=2>
					<wb:localize id="Your new password is">Your new password is</wb:localize>: <%= newPassword %>
				</td>
			  </tr>
			  <tr>
				<td colspan=2>&nbsp;</td>
			  </tr>
			  <tr>
				<td colspan=2>
			<%		if ("e".equals(fromPage)) {  // from ETM
			%>
						<a href='/etm/login.jsp'>
			<%		} else {
			%>
						<a href='/login.jsp'>
			<%		} 
			%>
					<wb:localize id="Back to Login">Back to Login</wb:localize>
					</a>
				</td>
			  </tr>
			</table>
<%
		} else {
%>
			<table cellspacing='2' cellpadding='2' border='0' width='750'>
			  <tr bgcolor="#E7E7E7">
				<td colspan=2>
					<b><span style="font-size:20px; color:rgb(255,0,0);"><wb:localize id="your answer is not correct">Sorry, your answer is not correct</wb:localize>!</span></b>
				</td>
			  </tr>
			  <tr>
				<td colspan=2>
					<span style="font-size:20px; color:rgb(255,0,0);"><wb:localize id="Contact Manager">Please contact your managers of security in your store (HR, Operations managers)</wb:localize>.</span>
				</td>
			  </tr>
			  <tr>
				<td colspan=2>&nbsp;</td>
			  </tr>
			</table>
<%
		}
	}
%>

<%
	if ("showQ".equals(toDo)) {
		if (isValidUser && !isAnswerSetup) {

%>
			<table cellspacing='2' cellpadding='2' border='0' width='750'>
			  <tr>
				<td colspan=2>
					<span style="font-size:20px; color:rgb(255,0,0);"><wb:localize id="Contact Manager">Please contact your managers of security in your store (HR, Operations managers)</wb:localize>.</span>
				</td>
			  </tr>
			</table>
<%
		}
	}
%>



</wb:page>

<%!

	public boolean validateUser(DBConnection conn, String wbuName)
		throws SQLException
	{
		if ((wbuName == null) || (wbuName.trim().length() == 0)) {
			return false;
		}

		boolean isUserValid = false;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String sql = "select wbu_id from WORKBRAIN_USER where upper(wbu_name) = ?";

		try {
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, wbuName.toUpperCase());
			rs = stmt.executeQuery();
			if (rs.next()) {
				isUserValid = true;
			}


		} finally {
			SQLHelper.cleanUp(rs);
			SQLHelper.cleanUp(stmt);
		}

		return isUserValid;
	} 


	public String getQuestionByUser(DBConnection conn, String wbuName) throws SQLException
	{
		if ((wbuName == null) || (wbuName.trim().length() == 0)) {
			return null;
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		String sql = "select wpq_name from WBIAG_PASS_QUESTION, WBIAG_USER_PASS_ANSWER, WORKBRAIN_USER where WBIAG_PASS_QUESTION.wpq_id = WBIAG_USER_PASS_ANSWER.wpq_id and WBIAG_USER_PASS_ANSWER.wbu_id = WORKBRAIN_USER.wbu_id and upper(wbu_name) = ?";
		String question = null;

		try {
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, wbuName.toUpperCase());
			rs = stmt.executeQuery();
			if (rs.next()) {
				question = rs.getString(1);
			}
		} finally {
			SQLHelper.cleanUp(rs);
			SQLHelper.cleanUp(stmt);
		}

		return question;
	}


	public boolean checkAnswer(DBConnection conn, String wbuName, String answer)
		throws SQLException
	{
		if ((answer == null) || (answer.trim().length() == 0)) {
			return false;
		}

		if ((wbuName == null) || (wbuName.trim().length() == 0)) {
			return false;
		}


		PreparedStatement stmt = null;
		ResultSet rs = null;
		String sql = "select wupa_answer from WBIAG_USER_PASS_ANSWER, WORKBRAIN_USER where WBIAG_USER_PASS_ANSWER.wbu_id = WORKBRAIN_USER.wbu_id and upper(wbu_name) = ?";
		String savedAnswer = null;

		try {
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, wbuName.toUpperCase());
			rs = stmt.executeQuery();
			if (rs.next()) {
				savedAnswer = rs.getString(1);
			}


		} finally {
			SQLHelper.cleanUp(rs);
			SQLHelper.cleanUp(stmt);
		}

		if (savedAnswer != null) {
			try {
				savedAnswer = decode(savedAnswer);
			} catch (Throwable t) {
				savedAnswer = "";
			}
		}

		return answer.equals(savedAnswer);
	} 


	public String changePassword(DBConnection conn, String wbuName)
		throws SQLException
	{
		String newPassword = null;

		newPassword = generatePassword();

		PreparedStatement stmt = null;
		ResultSet rs = null;
		String sql = "update workbrain_user set wbu_password = ?, WBU_ACTIVE = 'Y', WBU_BAD_LOGIN_COUNT=0, wbu_pwd_changed_date = SYSDATE - 10000 where upper(wbu_name) = ?";

		try {
			stmt = conn.prepareStatement(sql);
			stmt.setString(2, wbuName.toUpperCase());
			stmt.setString(1, newPassword);
			stmt.executeUpdate();
		} finally {
			SQLHelper.cleanUp(stmt);
		}

		return newPassword;
	} 

	public String generatePassword() 
	{
//		final char[] SPEC_CHAR = new char[]{'!','@','#','$','%','^','&','*','(',')','_','+','{','}' };
		final char[] SPEC_CHAR = new char[]{'!','$','*','_'};
		
		StringBuffer pwd = new StringBuffer(10);
		Random random = new Random(System.currentTimeMillis());
		int[] charLypeArray = new int[]{-1,-1,-1,-1,-1,-1,-1,-1};		
		int temp = 0;
		char c;
		
		//Place 2 numbers
		for (int i = 0; i < 2; i++) {
			int numLocation = random.nextInt(7);
			while (charLypeArray[numLocation] >= 0){
				numLocation++;
				if (numLocation > 7){
					numLocation =0;
				}
			}
			charLypeArray[numLocation] = 1;
		}
		//Place special char
		int charLocation = random.nextInt(7);
		while (charLypeArray[charLocation] >= 0){
			charLocation++;
			if (charLocation > 7){
				charLocation =0;
			}
		}
		charLypeArray[charLocation] = 0;
		//Place lower case
		for (int i = 0; i < 3; i++) {
			int lowerLocation = random.nextInt(7);
			while (charLypeArray[lowerLocation] >= 0){
				lowerLocation++;
				if (lowerLocation > 7){
					lowerLocation =0;
				}
			}
			charLypeArray[lowerLocation] = 2;
		}
		for (int i = 0; i < 2; i++) {
			int upperLocation = random.nextInt(7);
			while (charLypeArray[upperLocation] >= 0){
				upperLocation++;
				if (upperLocation > 7){
					upperLocation =0;
				}
			}
			charLypeArray[upperLocation] = 3;
		}

		
		for (int i = 0; i < 8; i++) {
			int charType = charLypeArray[i];
			switch (charType){
			case 0:
				temp = random.nextInt(3);
				c = SPEC_CHAR[temp];
				break;
			case 1:
				temp = random.nextInt(8);
				c = (char) (((int) '0') + temp);
				break;
			case 2:
				temp = random.nextInt(26);
				c = (char) (((int) 'a') + temp);
				break;
			case 3:
				temp = random.nextInt(26);
				c = (char) (((int) 'A') + temp);
				break;
			default:
				temp = random.nextInt(26);
				c = (char) (((int) 'A') + temp);
				break;
			}
			pwd.append(c);
		}
		return pwd.toString();
	}

	public String decode(String encodedStr) throws Exception
	{
		PwdConfig pwdConfig = new PwdConfig();
		byte[] bytes1 = Base64.decode(encodedStr);
		byte[] bytes2 = pwdConfig.decode(bytes1);
		return new String(bytes2, "UTF-8");
	}

%>