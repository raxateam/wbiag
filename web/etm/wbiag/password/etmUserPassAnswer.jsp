<%@ include file="/system/wbheader.jsp" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.*" %>
<%@ page import="com.workbrain.server.jsp.*" %>
<%@ page import="com.workbrain.sql.*" %>
<%@ page import="com.workbrain.app.ta.db.RecordAccess" %>
<%@ page import="com.workbrain.app.ta.model.RecordData" %>
<%@ page import="com.wbiag.app.ta.password.model.WbiagUserPassAnswerData" %>
<%@ page import="com.workbrain.util.PwdConfig" %>
<%@ page import="org.apache.soap.encoding.soapenc.Base64" %>

<wb:page type="VR" domain="VR" uiPathName='Password Question Answer' >


<script language=javascript>

	function wbValidateFields(theDocument,field) {
		return true;
	}

	function validateFormFields(execute) {

		if (!wbValidateFields(document,'isResolving')) {
			/* wait..... */
			setTimeout('validateFormFields("' + execute + '")', 1000);
			return;
		}

		/* done waiting */

		if (!wbValidateFields(document,'wbValid')) {
			alert(JSVAR_NOT_VALID_VALUE);
			if( gButton ) {
				gButton.disabled=false;
			}
			return false;
		} else {
			setTimeout(execute,500);
			return true;
		}
	}


	function validateFormFieldsWithButtonsDisabled(button, execute) {
		gButton = button;
		if( gButton ) gButton.disabled=true;
		return validateFormFields(execute);
	}

	function setUIPathlabel(value) {
	  if (document.forms[0].uiPathLabel) {
		document.forms[0].uiPathLabel.value = value;
	  }
	}

	var bHasFormBeenSubmitted=false;
	function wasSubmitted(){
	  if(bHasFormBeenSubmitted==true){
		return true;
	  }
	  bHasFormBeenSubmitted=true;
	  return false;
	}

    window.status= "Done";

</script>

<wb:config id="NEW_RECORD_CREATED"/>

<wb:define id="infoNewRecordCreated"><wb:localize id="NEW_RECORD_CREATED" ignoreConfig="true">Your new record has been created.</wb:localize></wb:define>
<wb:define id="infoChangeSaved"><wb:localize id="YOUR_CHANGES_HAVE_BEEN_SAVED" ignoreConfig="true">Your changes has been saved.</wb:localize></wb:define>
<wb:define id="js_notValidValue"><wb:localize id="JAVASCRIPT_INCORRECT_VALUE" ignoreConfig="true">The value entered in the field is not correct.</wb:localize></wb:define>


<script>
var JSVAR_NOT_VALID_VALUE  = "<%=js_notValidValue.toString()%>";
</script>

<%
    int wbuId   = Integer.parseInt(JSPHelper.getWebLogin(request).getUserId());
    String userName = JSPHelper.getWebLogin(request).getUserName();
	String sqlSource = "select wpq_id, wpq_name from wbiag_pass_question"  ;
	String uiParamsQue = "width=100 multiChoice=false pageSize=10 title='Select Question' sourceType=SQL source=\"" + sqlSource + "\"";
	String sql = "select wupa_id, wpq_id, wupa_answer from wbiag_user_pass_answer where wbu_id = ?";
    DBConnection conn = JSPHelper.getConnection(request);
	PreparedStatement stmt = null;
    ResultSet rs = null;
	String question = null;
	String answer = null;
	String infoMsg = null;
	String errMsg = null;
	HashMap msgMap = new HashMap();
	int wpqId = 0;  // quesiton id
	int wupaId = 0;
	String toDo = request.getParameter("to_do");
	RecordAccess ra = new RecordAccess(conn);
	WbiagUserPassAnswerData data = new WbiagUserPassAnswerData();
	List list = null;

	try {
		wupaId = Integer.parseInt(request.getParameter("WUPA_ID"));
	} catch (Exception e) {
		wupaId = 0;
	}
	data.setWupaId(wupaId);

	try {
		wpqId = Integer.parseInt(request.getParameter("WPQ_ID"));
	} catch (Exception e) {
		wupaId = 0;
	}
	data.setWpqId(wpqId);

	data.setWbuId(wbuId);
	answer = request.getParameter("WUPA_ANSWER");
	String originalPwd = request.getParameter("WUPA_ANSWER");
	String encryptedPwd = null;
	String decryptedPwd = null;
	if (originalPwd != null) {
		data.setWupaAnswer(encode(originalPwd));
	} else {
		data.setWupaAnswer(null);
	}


	// Save data if submitted
	if ("save".equals(toDo)) {
		if (wupaId == 0) {
			data.setWupaId(conn.getDBSequence("SEQ_WUPA_ID").getNextValue());
			ra.insertRecordData(data, "WBIAG_USER_PASS_ANSWER");
			infoMsg = infoNewRecordCreated.toString();
		} else {
			// update
			ra.updateRecordData(data, "WBIAG_USER_PASS_ANSWER", "WUPA_ID");
			infoMsg = infoChangeSaved.toString();
		}
	}

	list = ra.loadRecordData(data, "WBIAG_USER_PASS_ANSWER", "wbu_id = " + wbuId);
	if ((list != null) && (list.size() > 0)) {
		data = (WbiagUserPassAnswerData) list.get(0);
		wupaId = data.getWupaId();
		wpqId = data.getWpqId();
		encryptedPwd = data.getWupaAnswer();
		if (encryptedPwd != null) {
			try {
				answer = decode(encryptedPwd);
			} catch (Throwable t) {
				answer = "";
			}
		} else {
			answer = "";
		}


	} else {
		data = null;
		wupaId = 0;
		wpqId = 0;
		answer = "";
	}

%>
<jsp:include page="/etm/etmMenu.jsp" flush="true"> <jsp:param name="selectedTocID" value="10101"/> <jsp:param name="parentID" value="10100"/> </jsp:include>

<%
	

	if (wupaId == 0) {
		// user has not choosen question and answer yet
		//errMsg = "You have not set up you password question answer yet!";

	}



%>
<input type="hidden" name="to_do" value=""/>
<input type="hidden" name="_form_group_maintenance_action", id="_form_group_maintenance_action" value="cancel|listView">
<input type="hidden" name="uiPathLabel" value=""/>
<input type=hidden name='WBU_ID' value='<%= wbuId %>'>
<input type=hidden name='WBU_NAME' value='<%= userName %>'>
<input type=hidden name='WUPA_ID' value='<%= wupaId %>'>
<%
	if (infoMsg != null) {	
%>
		<div style='position:static; float:none; clear:right;width:100%;height:21px;padding-top:10px;margin-bottom:.2em'>
		  <div style='margin:0px;padding-left:35px; height:21px; line-height:21px; width:auto; position:static; float:left; background:#E2E7ED  url(/images/interface/statusokleft.gif) no-repeat'><%=infoMsg%></div>
		  <div style='margin-top:0px; width:12px; height:21px; position:static;float:left;clear:right; background-image:url(/images/interface/statusokright.gif);'></div>
		</div>
<%
	}
%>

<%
	if (errMsg != null) {
%>
			    <span style="font-size:20px; color:rgb(255,0,0);"><%= errMsg %></span>

<%
	}
%>

<table cellspacing='2' cellpadding='2' border='0'>
  <tr>
    <td>
		<table cellspacing='2' cellpadding='2' style='border-color:#0000b4; border-width:1; border-style:solid;'>

		  <tr>
			<td>
				<table cellspacing='1' cellpadding='1' border='0'>
				  <tr>
					<td nowrap class="boldedLabel"><wb:localize id="Question">Question</wb:localize></td>
					<td>
					  <wb:controlField cssClass="inputField"
									   submitName="WPQ_ID"
									   ui='DBLookupUI'
									   uiParameter='<%= uiParamsQue %>'
									   onChange = '' ><%=wpqId%></wb:controlField>
					</td>
				  </tr>

				  <tr>
					<td nowrap class="boldedLabel"><wb:localize id="Answer">Answer</wb:localize></td>
					<td>
					  <wb:controlField cssClass="inputField"
									   submitName="WUPA_ANSWER"
									   ui='StringUI'
									   uiParameter='width=100'
									   onChange = ''><%=answer%></wb:controlField>
					</td>
				  </tr>

				</table>
			</td>
		  </tr>
		</table>
    </td>
  </tr>
</table>



<div class="separatorLarge"/>

<wb:define id="localSave"><wb:localize ignoreConfig="true" id="Save">Save</wb:localize></wb:define>
<wb:vrbutton imageBased="true" label="#localSave#" selected="false" onClick="if(!( validateFormFieldsWithButtonsDisabled(this, 'return true') && !wasSubmitted() )) return false; setUIPathlabel('Password Question Answer');document.forms.page_form.to_do.value='save'; document.forms.page_form.submit(); return false;"/>

</wb:page>

<%!
	public String encode(String plainStr) throws Exception
	{
		PwdConfig pwdConfig = new PwdConfig();
		byte[] bytes1 = plainStr.getBytes("UTF-8");
		byte[] bytes2 = pwdConfig.encode(bytes1);
		return Base64.encode(bytes2);
	}

	public String decode(String encodedStr) throws Exception
	{
		PwdConfig pwdConfig = new PwdConfig();
		byte[] bytes1 = Base64.decode(encodedStr);
		byte[] bytes2 = pwdConfig.decode(bytes1);
		return new String(bytes2, "UTF-8");
	}
%>