<!-- 
	Controller Servlet for Pay Rules Test Suite actions 
-->

<%@ include file="/system/wbheader.jsp"%>

<%@ page import="com.wbiag.tool.regressiontest.action.*" %>

<%@ page import="com.workbrain.app.jsp.*" %>
<%@ page import="com.workbrain.server.jsp.JSPHelper" %>
<%@ page import="com.workbrain.server.jsp.proxy.*" %>
<%@ page import="com.workbrain.sql.DBConnection" %>

<%@ page import="javax.servlet.*" %>
<%@ page import="javax.servlet.http.*" %>


<%!

/*
 * An Action Name is submitted by a JSP form.  Instantiate and return the 
 * corresponding Action class.
 */
Action makeAction(String actionType) throws Exception
{
	final String TEST_CASE_ACTION_PACKAGE = "com.wbiag.tool.regressiontest.action";
	final String TEST_SUITE_ACTION_PACKAGE = "com.wbiag.tool.regressiontest.action";

	String actionClassName = null; 

	if (ActionTypeTestSuite.INIT_TEST_SUITE.equals(actionType)) {
			actionClassName = TEST_SUITE_ACTION_PACKAGE + ".TestSuiteInitAction";
			
	} else if (ActionTypeTestSuite.CREATE_TEST_SUITE.equals(actionType)) {
			actionClassName = TEST_SUITE_ACTION_PACKAGE + ".TestSuiteCreateAction";

	} else if (ActionTypeTestSuite.SAVE_TEST_SUITE.equals(actionType)) {
			actionClassName = TEST_SUITE_ACTION_PACKAGE + ".TestSuiteSaveAction";

	} else if (ActionTypeTestSuite.GET_TEST_SUITE.equals(actionType)) {
			actionClassName = TEST_SUITE_ACTION_PACKAGE + ".TestSuiteGetAction";

	} else if (ActionTypeTestSuite.DELETE_TEST_SUITE.equals(actionType)) {
			actionClassName = TEST_SUITE_ACTION_PACKAGE + ".TestSuiteDeleteAction";

	} else if (ActionTypeTestSuite.RUN_TEST_SUITE.equals(actionType)) {
			actionClassName = TEST_SUITE_ACTION_PACKAGE + ".TestSuiteRunAction";

	} else if (ActionTypeRulesCase.ADD_TEST_CASE.equals(actionType)) {
			actionClassName = TEST_CASE_ACTION_PACKAGE + ".RulesCaseAddAction";

	} else if (ActionTypeRulesCase.DELETE_TEST_CASE.equals(actionType)) {
			actionClassName = TEST_CASE_ACTION_PACKAGE + ".RulesCaseDeleteAction";

	} else if (ActionTypeRulesCase.COPY_TEST_CASE.equals(actionType)) {
			actionClassName = TEST_CASE_ACTION_PACKAGE + ".RulesCaseCopyAction";

	} else if (ActionTypeRulesCase.RECREATE_TEST_CASE.equals(actionType)) {
			actionClassName = TEST_CASE_ACTION_PACKAGE + ".RulesCaseReCreateAction";
	
	} else if (ActionTypeRulesCase.SAVE_CHANGES_TEST_CASE.equals(actionType)) {
			actionClassName = TEST_CASE_ACTION_PACKAGE + ".RulesCaseSaveChangesAction";
	}  
	
	else {
	    System.out.println("Unknown ActionType: " + actionType);
	}

	Action actionClass = null;
	
	if (actionClassName != null) {
	
        try {
        	actionClass = (Action) (Class.forName(actionClassName)).newInstance();
    	
    	} catch (Exception e) {
    		throw e;
    	}
    }
	
	return actionClass;
} 

/*
 * An Action Result is returned from the Action.process() method.  Map the actionResult
 * to a page URL.
 */
String getForwardURL(String actionResult, HttpServletRequest request)
{
	String forwardURL = null;

	if (actionResult == null) {
		forwardURL = "payRulesTestSuite.jsp";
	
	} else {

		if (
					ForwardRulesCase.ADD_TEST_CASE_SUCCEED.equals(actionResult)
				|| 	ForwardRulesCase.DELETE_TEST_CASE_SUCCEED.equals(actionResult)
				|| 	ForwardRulesCase.COPY_TEST_CASE_SUCCEED.equals(actionResult)
				|| 	ForwardRulesCase.RECREATE_TEST_CASE_SUCCEED.equals(actionResult)
				|| 	ForwardTestSuite.SAVE_TEST_SUITE_SUCCEED.equals(actionResult)
				||  ForwardRulesCase.SAVE_CHANGES_TEST_CASE_SUCCEED.equals(actionResult)
				
				|| 	ForwardRulesCase.ADD_TEST_CASE_FAILED.equals(actionResult)
				|| 	ForwardRulesCase.DELETE_TEST_CASE_FAILED.equals(actionResult)
				|| 	ForwardRulesCase.COPY_TEST_CASE_FAILED.equals(actionResult)
				|| 	ForwardRulesCase.RECREATE_TEST_CASE_FAILED.equals(actionResult)
				|| 	ForwardTestSuite.SAVE_TEST_SUITE_FAILED.equals(actionResult)
				||  ForwardRulesCase.SAVE_CHANGES_TEST_CASE_FAILED.equals(actionResult)
				) {

            forwardURL = "action.jsp?actionType=" + ActionTypeTestSuite.GET_TEST_SUITE;
				
		} else if ( 
					ForwardTestSuite.INIT_TEST_SUITE_SUCCEED.equals(actionResult)
				|| 	ForwardTestSuite.CREATE_TEST_SUITE_SUCCEED.equals(actionResult)
				|| 	ForwardTestSuite.GET_TEST_SUITE_SUCCEED.equals(actionResult)
				|| 	ForwardTestSuite.DELETE_TEST_SUITE_SUCCEED.equals(actionResult)

				|| 	ForwardTestSuite.INIT_TEST_SUITE_FAILED.equals(actionResult)
				|| 	ForwardTestSuite.CREATE_TEST_SUITE_FAILED.equals(actionResult)
				|| 	ForwardTestSuite.GET_TEST_SUITE_FAILED.equals(actionResult)
				|| 	ForwardTestSuite.DELETE_TEST_SUITE_FAILED.equals(actionResult)
				|| 	ForwardTestSuite.RUN_TEST_SUITE_FAILED.equals(actionResult)
				) {
				
            forwardURL = "payRulesTestSuite.jsp";
        
        } else if (ForwardTestSuite.RUN_TEST_SUITE_SUCCEED.equals(actionResult)) {
			forwardURL = "../report/payRulesReport.jsp";
        
        } else {
        	System.out.println("actionResult unknown: " + actionResult);
        }
	}
	
	return forwardURL;
}


/*
 * The main method of the page.
 *
 * 1. Get an instance of the Action class
 * 2. Call it's process method
 * 3. Forward to a URL based on the value returned by the process method.
 *
 */
String process(HttpServletRequest request, 
							HttpServletResponse response, 
							PageContext pageContext) 
							throws Exception 
{
	String actionType = request.getParameter("actionType");

    String actionResult = null;

    try {
		Action actionClass = makeAction(actionType);

        if (actionClass != null) {

        	DBConnection dbConn = JSPHelper.getConnection(request);
        
        	ProxyHttpServletRequest proxyreq = new ProxyHttpServletRequest(request, "UTF-8", true);
        	Object reqModel = actionClass.createRequest(proxyreq, dbConn);
        
        	actionResult = actionClass.process(dbConn, new ActionContext(proxyreq), reqModel);
        }
        
    } catch (Exception e) {
        e.printStackTrace();
        throw e;
    }
    
    String forwardURL = getForwardURL(actionResult, request);

	return forwardURL;
}
%>


<!-- 
	Begin the page. 
-->
<wb:page emitHtml='false'>

<%
    String forwardUrl = null;
    try {
        forwardUrl = process(request, response, pageContext);
        if( forwardUrl != null ) {
%>
			<wb:forward page='<%= forwardUrl %>' />
<%
        }
    
    } catch( Exception e ) {
        e.printStackTrace();
  		throw e;
    }
%>

</wb:page>
