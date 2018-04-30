<%@ include file="/system/wbheader.jsp"%>

<%@ page import="com.wbiag.sql.DBServer" %>
<%@ page import="com.workbrain.util.DateHelper" %>
<%@ page import="com.workbrain.server.jsp.JSPHelper" %>
<%@ page import="com.wbiag.tool.regressiontest.model.TestSuiteData" %>
<%@ page import="com.wbiag.tool.regressiontest.util.RequestHelper" %>

<wb:page maintenanceFormId='-1000005'>

<!-- Get attributes from the request -->
<%
TestSuiteData testSuiteData = RequestHelper.getTestSuiteBean(request);
if (testSuiteData == null) {
	testSuiteData = new TestSuiteData();
}
%>

<wb:define id="DATE_TIME_FORMAT">MM/dd/yyyy HH:mm</wb:define>

<input type='HIDDEN' name='testSuiteId' value='<%= testSuiteData.getSuiteId() %>' >
<input type='HIDDEN' name='createdDate' value='<%= testSuiteData.getSuiteCreatedDate() == null ? "" : DateHelper.convertDateString(testSuiteData.getSuiteCreatedDate(), DATE_TIME_FORMAT.toString()) %>' >
<input type='HIDDEN' name='creatorWbuName' value='<%= testSuiteData.getSuiteCreatorName() == null ? "" : testSuiteData.getSuiteCreatorName() %>' >

    <!-- Retrieve Report -->
    <tr>
    <td>
    	<wba:table width="100%" caption="Retrieve Report" captionLocalizeIndex='TestSuiteRetrieveReport'>
    	<wba:tr>
    		<wba:th width="25%"><wb:localize id='TestSuiteReport'>Report</wb:localize>
    		</wba:th>
    		<wba:td>
                <wb:define id="DATE_TO_STRING"><%= (new DBServer(JSPHelper.getConnection(request))).getToCharTimestamp("REPORT_DATE") %></wb:define>
    		    <wb:define id="REPORT_HISTORY_SQL">SELECT REPORT_ID, SUITE_NAME, #DATE_TO_STRING# REPORT_EXECUTED_DATE FROM WBIAG_TST_REPORT tsr, WBIAG_TST_SUITE ts WHERE tsr.SUITE_ID=ts.SUITE_ID ORDER BY REPORT_ID DESC</wb:define>
        		
        		<wb:controlField cssClass="inputField" id='retrieveTestReportId' submitName='retrieveTestReportId' 
        							ui='DBLookupUI' 
        							uiParameter='sourceType="SQL" source="#REPORT_HISTORY_SQL#"'>
        		</wb:controlField>
            </wba:td>
            <wba:td width="75%">                
    		    <wba:button label="Show Report" labelLocalizeIndex="ShowTestSuiteReport" 
    		            onClick="showTestSuiteReport()"/>
    		</wba:td>
    	</wba:tr>
    	</wba:table>
    </td>
    </tr>

	<!-- Retrieve Test Suite -->
	<tr>
	<td>
		<wba:table width="100%" caption="Retrieve Test Suite" captionLocalizeIndex='TestSuiteRetrieve'>
	    	<wba:tr>
	    		<wba:th width="25%"><wb:localize id='TestSuiteName'>Name</wb:localize>
	    		</wba:th>
	    		<wba:td>
	    		<wb:controlField cssClass="inputField" id='retrieveTestSuiteId' submitName='retrieveTestSuiteId' 
	    							ui='DBLookupUI' 
	    							uiParameter='sourceType="SQL" source="SELECT SUITE_ID, SUITE_NAME, SUITE_DESC FROM WBIAG_TST_SUITE"'>
	    		</wb:controlField>
	    		</wba:td>
	    		<wba:td width="75%">
	    		       <wba:button label="Retrieve" labelLocalizeIndex="Retrieve" 
	    		            onClick="retrieveTestSuite()"/>
	    		</wba:td>
	    	</wba:tr>
	    </wba:table>
    </td>
    </tr>
    
    <!-- Current/New Test Suite -->
    <tr>
    <td>
        <wba:table width="100%" caption="Current/New Test Suite" captionLocalizeIndex='TestSuiteNew'>
        
            <!-- Input fields -->
        	<wba:tr>
        		<wba:th width="15%"><wb:localize id='TestSuiteName'>Name *</wb:localize>
        		</wba:th>
        		<wba:td>
        		<wb:controlField cssClass="inputField" id='TestSuiteName' 
        					submitName='testSuiteName'><%= testSuiteData.getSuiteName() == null ? "" : testSuiteData.getSuiteName() %></wb:controlField>
        		</wba:td>

                <!-- Run Button. -->
    			<wba:td width="100%"><wba:button label="Run" labelLocalizeIndex="RunTestSuite" 
    			            onClick="runTestSuite()" 
    			            disabled="<%= String.valueOf(testSuiteData.getSuiteId() <= 0) %>"/></wba:td>
    			
        	</wba:tr>
        
        	<wba:tr>
        		<wba:th><wb:localize id='TestSuiteDescription'>Description</wb:localize>
        		</wba:th>
        		<wba:td><wb:controlField cssClass="inputField" id='TestSuiteDescription' 
        					submitName="testSuiteDescription"><%= testSuiteData.getSuiteDesc() == null ? "" : testSuiteData.getSuiteDesc() %></wb:controlField>
        		</wba:td>
        	</wba:tr>
        
        	<wba:tr>
        		<wba:th><wb:localize id='LastRun'>Last Run</wb:localize>
        		</wba:th>
        		<wba:td><wb:controlField cssClass="inputField" id='LastRun' mode='view' 
        					submitName="lastRun">
        				<%= testSuiteData.getSuiteExecuteDate() == null ? ""
        				    : DateHelper.convertDateString(testSuiteData.getSuiteExecuteDate(), DATE_TIME_FORMAT.toString()) %>
        			</wb:controlField>
        		</wba:td>
        		
        	</wba:tr>
        	
        	<!-- Buttons -->
        	<wba:tr>
        	<wba:td colspan="3">
        	    <wba:table>
        	        <wba:tr>

            		<wba:td><wba:button label="New Test Suite" labelLocalizeIndex="NewTestSuite" 
            		            onClick="newTestSuite()"/></wba:td>

        			<wba:td><wba:button label="Save" labelLocalizeIndex="Save" 
        			            onClick="saveTestSuite()"/></wba:td>
        			
        	
        			<!-- If this is a new test suite, do not enable the Delete and Run buttons. -->
        			<wba:td><wba:button label="Delete" labelLocalizeIndex="Delete" 
        			            onClick="deleteTestSuite()"
        			            disabled="<%= String.valueOf(testSuiteData.getSuiteId() <= 0) %>"/></wba:td>

        			</wba:tr>
        		</wba:table>
        	</wba:td>
            </wba:tr>
    
    	</wba:table> 
    </td>
    </tr>
    	

</wb:page>