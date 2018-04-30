<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.util.Date" %>

<wb:page submitAction='/regressiontest/payrules/action.jsp'>

<input type=HIDDEN name="actionType" value="RunTestSuite">
<input type='HIDDEN' name='testSuiteId' 
		value='<%= request.getParameter("testSuiteId") %>' >
<input type='HIDDEN' name='reportXSLFilename' 
		value='<%= request.getParameter("reportXSLFilename") %>' >



<wb:localize id='Executing_Test_Suite'>Executing Test Suite: </wb:localize> <%= request.getParameter("testSuiteName") %>
<br>
<wb:localize id='Test_Suite_Started_At'>Started at: </wb:localize> <%= (new Date()).toString() %>



<script>
	document.forms.page_form.submit();
</script>

</wb:page>