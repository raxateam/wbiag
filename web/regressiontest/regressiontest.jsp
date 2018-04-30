<%@ include file="/system/wbheader.jsp"%>

<%@ page import="com.wbiag.tool.regressiontest.action.*" %>

<!--
	This is the start page.  For now we will forward to the Pay Rules module
	but in future this can be the index page to link to other test modules.
-->

<wb:page >

	<!-- Go to the init action of the Pay Rules Test Suite.-->
	<%
	    String forwardURL = "payrules/action.jsp?actionType=" + ActionTypeTestSuite.INIT_TEST_SUITE;
	%>
	
	<wb:forward page='<%= forwardURL %>' />


</wb:page>