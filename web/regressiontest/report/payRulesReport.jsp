<%@ include file="/system/wbheader.jsp"%>

<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Date" %>

<%@ page import="com.wbiag.tool.regressiontest.testengine.TestSuiteTypes" %>
<%@ page import="com.wbiag.tool.regressiontest.report.*" %>
<%@ page import="com.wbiag.tool.regressiontest.xml.RulesResultsHelper" %>
<%@ page import="com.workbrain.server.jsp.JSPHelper" %>
<%@ page import="com.workbrain.sql.DBConnection" %>
<%@ page import="com.workbrain.util.DateHelper" %>

<wb:page maintenanceFormId='1000005'>

<%!
	// First declare the test suite type to be used by retrieveReportObj.jsp
	String testSuiteType= TestSuiteTypes.PAY_RULES;
	
	// Display object used to render this page.
	ReportDisplayRulesSuite rulesReportObj = null;
%>

<!-- the retrieveReportObj.jsp page will retrieve a ReportDisplaySuite object 
	named reportObj using the reportId in the request. 
-->	
<%@ include file="retrieveReportObj.jsp" %>

<!-- Cast reportObj to the object type that this page will use. -->
<%
	rulesReportObj = (ReportDisplayRulesSuite) reportObj;
%>

<!-- Declare page variables and functions. -->
<%! 
    static String DATE_FORMAT = "MM/dd/yyyy";
    static String DATE_TIME_FORMAT = "MM/dd/yyyy HH:mm:ss";
    static String COLUMN_DATE_TIME_FORMAT_XML = RulesResultsHelper.DATE_TIME_FORMAT;
    static String COLUMN_DATE_TIME_FORMAT_DISPLAY = "MM/dd/yyyy HH:mm";


    List dataColumns = null; 
    List expectedDataValues = null;
    List actualDataValues = null;
    Iterator iColumns = null;
    Iterator iRows = null;
    
    // Formats the data in the columns for display.
    public String formatColumnValue(String unformattedValue) {
    
        String formattedValue = null;
        Date dateObj = null;
        
        try {
            // If it's a date, convert from the XML string format to the display format.
            dateObj = DateHelper.parseDate(unformattedValue, COLUMN_DATE_TIME_FORMAT_XML);
            formattedValue = DateHelper.convertDateString(dateObj, COLUMN_DATE_TIME_FORMAT_DISPLAY);
        
        } catch (Exception e) {
            // No formatting required.
            formattedValue = unformattedValue;
        }
        
        return formattedValue;
    }
    
    // duration is given in seconds.  Format into HH:mm:ss
    public String formatDuration(long duration) {
        
        String dateFormat = null;
        
        if (duration < 10) {
            dateFormat = "s";
        } else if (duration < 60) {
            dateFormat = "ss";
        } else if (duration < 3600) {
            dateFormat = "mm:ss";
        } else {
            dateFormat = "HH:mm:ss";
        }
        
        return DateHelper.convertDateString(new Date(duration * 1000), dateFormat);
    }
    
%>
<script type="text/javascript">
	function toggleError(divId){
		var e = document.getElementById(divId);
		if(e.style.display == 'none')
			e.style.display = 'block';
		else
			e.style.display = 'none';
	}
</script>

<table width="100%">

    <!-- Test Suite Name -->
    <wba:tr>
        <wba:td>
            <B><wb:localize id='RegressionSuiteResults'>Test Results for Suite: </wb:localize></B> <%= rulesReportObj.getSuiteName() %>
        </wba:td>
        <wba:td align="right">
            <B><wb:localize id='RegressionExecutionDate'>Execution Date</wb:localize></B> <%= DateHelper.convertDateString(rulesReportObj.getExecutedDate(), DATE_TIME_FORMAT) %>
        </wba:td>
    </wba:tr>

    <!-- Executed Date -->

    <!-- Summary Table -->
    <wba:tr>
    <wba:td colspan="2">
    <wba:table width="100%" caption="Summary" captionLocalizeIndex='RegressionTestSummary'>
        
        <wba:tr>
            <wba:th><wb:localize id='RegressionTestCount'>Tests</wb:localize></wba:th>
            <wba:th><wb:localize id='RegressionFailureCount'>Failures</wb:localize></wba:th>
            <wba:th><wb:localize id='RegressionErrorCount'>Errors</wb:localize></wba:th>
            <wba:th><wb:localize id='RegressionSuccessRate'>Success Rate</wb:localize></wba:th>
            <wba:th><wb:localize id='RegressionDuration'>Duration</wb:localize></wba:th>
        </wba:tr>

        <wba:tr>
            <wba:td><%= rulesReportObj.getTestCount() %></wba:td>
            <wba:td><%= rulesReportObj.getFailCount() %></wba:td>
            <wba:td><%= rulesReportObj.getErrorCount() %></wba:td>
            <wba:td><%= rulesReportObj.getSuccessRate() %> % </wba:td>
            <wba:td><%= formatDuration(rulesReportObj.getExecutionTimeSeconds()) %> 
                    <% if (rulesReportObj.getExecutionTimeSeconds() < 60) { %>
                        <wb:localize id='RegressionSeconds'>seconds</wb:localize>
                    <% } %>
            </wba:td>
        </wba:tr>
        
        <wba:tr>
            <wba:td colspan="5">
            <wb:localize id='RegressionExceptionNote'>Note: <I>Failures</I> are test cases that have failed while <I>Errors</I> are when an exception was thrown</wb:localize>
            </wba:td>
        </wba:tr>
        
    </wba:table>
    </wba:td>
    </wba:tr>
    <!-- End Summary Table -->

    
    <!-- Test Case List Table -->
    <wba:tr>
    <wba:td colspan="2">
    <wba:table width="100%" caption="Test Case Results" captionLocalizeIndex='RegressionCaseResults'>
                
        <%  
        if (rulesReportObj.getCaseResults() != null) {

            Iterator iCase = rulesReportObj.getCaseResults().iterator();
            ReportDisplayRulesCase displayCase = null;
            
            for (int i=0; iCase.hasNext(); i++) {
                
                displayCase = (ReportDisplayRulesCase) iCase.next();  %>
        
                <wba:tr><wba:th>
                		<wb:localize id='ReportTestCaseName'><B>Test Case Name:</B></wb:localize> <%= displayCase.getCaseName() %> 
                		<wb:localize id='ReportTestCaseEmployee'>, <B>Employee:</B></wb:localize> <%= displayCase.getEmpName() %>
                		</wba:th>
                </wba:tr>

                <wba:tr>
                <% 
                // if there were no errors then Status is success.
                if (displayCase.getCaseDayResults() == null) {   %>
        
                    <wba:td><span style="font-size:14px;color:white;background-color:#009900;font-weight:bold">&nbsp;&nbsp;<wb:localize id='RegressionStatusSuccess'>Success</wb:localize>&nbsp;&nbsp;</span></wba:td>
                    
                <%
                // If there were errors, then display the status, error message,
                // expected, and actual data.
                } else {  
                    Iterator iCaseDay = displayCase.getCaseDayResults().iterator();
                    ReportDisplayRulesDay displayDay = null;
                        
                    for (int j=0;iCaseDay.hasNext();j++) {
                        
                        displayDay = (ReportDisplayRulesDay) iCaseDay.next();  
    
                        dataColumns = null; 
                        expectedDataValues = null;
                        actualDataValues = null;    %>
                        
                        <!-- Create a new table for each Fail/Error -->
                        <wba:tr><wba:td>
                        <a href="javascript:toggleError('errorDiv_<%= String.valueOf(i)+"_"+String.valueOf(j) %>')" style="font-size:14px;color:white;background-color:red;font-weight:bold">&nbsp;&nbsp;<wb:localize id='RegressionStatusFailure'>FAILED</wb:localize>&nbsp;&nbsp;</a>
						&nbsp;&nbsp;<%= DateHelper.convertDateString(displayDay.getWorkSummaryDate(), DATE_FORMAT) %>
                        <div id="errorDiv_<%= String.valueOf(i)+"_"+String.valueOf(j) %>" style="display:none">
                        <wba:table width="100%">
                        
                        <!-- Status and Message -->    
                        <wba:tr>
                            <wba:td class="error">
                            <% if (displayDay.getStatus() == TestCaseCompareResult.STATUS_ERROR) { %>
                                <wb:localize id='RegressionStatusError'>Error on </wb:localize>
                            <% } else if (displayDay.getStatus() == TestCaseCompareResult.STATUS_FAILED) { %>
                                <wb:localize id='RegressionStatusFailure'>Failure on </wb:localize>
                            <% } %>
                            (<%= DateHelper.convertDateString(displayDay.getWorkSummaryDate(), DATE_FORMAT) %>)
                            </wba:td>
                        </wba:tr>
                        <wba:tr>
                            <wba:td class="error"><%= displayDay.getMessage() %></wba:td>
                        </wba:tr>
                        
                        <!-- Table Name -->
                        <% if (displayDay.getWorkSummaryColumns() != null 
                                    && !displayDay.getWorkSummaryColumns().isEmpty()) {
                                dataColumns = displayDay.getWorkSummaryColumns();
                                expectedDataValues = displayDay.getWorkSummaryExpectedData();
                                actualDataValues = displayDay.getWorkSummaryActualData();  
                                %>
                        
                        <% } else if (displayDay.getWorkDetailColumns() != null
                                    && !displayDay.getWorkDetailColumns().isEmpty()) { 
                                dataColumns = displayDay.getWorkDetailColumns();
                                expectedDataValues = displayDay.getWorkDetailExpectedData();
                                actualDataValues = displayDay.getWorkDetailActualData();  
                                %>
                        
                        <% } else if (displayDay.getWorkPremiumColumns() != null
                                    && !displayDay.getWorkPremiumColumns().isEmpty()) { 
                                dataColumns = displayDay.getWorkPremiumColumns();
                                expectedDataValues = displayDay.getWorkPremiumExpectedData();
                                actualDataValues = displayDay.getWorkPremiumActualData();  
                                %>
                        
                        <% } else if (displayDay.getEmployeeBalanceColumns() != null
                                    && !displayDay.getEmployeeBalanceColumns().isEmpty()) { 
                                dataColumns = displayDay.getEmployeeBalanceColumns();
                                expectedDataValues = displayDay.getEmployeeBalanceExpectedData();
                                actualDataValues = displayDay.getEmployeeBalanceActualData();  
                                %>

                        <% } %>
                                
                        <!-- Table for the expected result, actual result. -->
                        <wba:tr>
                        <wba:td colspan="3">
                        <wba:table width="100%">

                            <!-- Expected Data -->
                            <wba:tr><wba:td colspan="99"><B><I><wb:localize id='RegressionCaseExpected'>Expected Data</wb:localize></I></B></wba:td></wba:tr>
                            
                            <!-- Column names is a List of String -->
                            <wba:tr>
                            <%
                            iColumns = dataColumns.iterator();
                            while (iColumns.hasNext()) { %>
                                <wba:th><%= (String) iColumns.next() %></wba:th>
                            <% } %>
                            </wba:tr>
                            
                            <!-- Data is a List, of a List of String -->
                            <%
                            if (expectedDataValues != null) {
                                iRows = expectedDataValues.iterator();
                                while (iRows.hasNext()) { %>
                                    <wba:tr>
                                    <%
                                    iColumns = ((List) iRows.next()).iterator();
                                    while (iColumns.hasNext()) {    %>
                                        <wba:td><%= formatColumnValue((String) iColumns.next()) %></wba:td>
                                    <% }  %>
                                    </wba:tr>
                                <% } %>
                            <% } else { %>
                                <wba:td><wb:localize id='RegressionDataNone'>None</wb:localize></wba:td>
                            <% } %>


                            <!-- Actual Data -->
                            <wba:tr><wba:td colspan="99"><B><I><wb:localize id='RegressionCaseActual'>Actual Data</wb:localize></I></B></wba:td></wba:tr>
                            
                            <!-- Column names is a List of String -->
                            <wba:tr>
                            <%
                            iColumns = dataColumns.iterator();
                            while (iColumns.hasNext()) { %>
                                <wba:th><%= (String) iColumns.next() %></wba:th>
                            <% } %>
                            </wba:tr>

                            <!-- Data is a List, of a List of String -->
                            <%
                            if (actualDataValues != null) {
                                iRows = actualDataValues.iterator();
                                while (iRows.hasNext()) { %>
                                    <wba:tr>
                                    <%
                                    iColumns = ((List) iRows.next()).iterator();
                                    while (iColumns.hasNext()) {    %>
                                        <wba:td><%= formatColumnValue((String) iColumns.next()) %></wba:td>
                                    <% }  %>
                                    </wba:tr>
                                <% } %>
                            <% } else { %>
                                <wba:td><wb:localize id='RegressionDataNone'>None</wb:localize></wba:td>
                            <% } %>
                        </wba:table>
                        </wba:td>
                        </wba:tr>
                        
                        </wba:table></div></wba:td></wba:tr>
                    <%
                    } // end while each test case date

                } // end if test case day results
            %>
            </wba:tr>
            <%
            } // end for test case results
        } // end if test case results  
        %>          

                
    </wba:table>
    </wba:td>
    </wba:tr>
    <!-- End Test Case List Table -->

</table>

</wb:page>