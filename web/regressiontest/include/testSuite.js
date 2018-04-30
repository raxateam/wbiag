/*
 * Test Suite Functions.
 *
 */
function retrieveTestSuite()
{
    if (document.forms.page_form.retrieveTestSuiteId.value == "") {
        alert("Please Select a Test Suite.");
    } else {
    	document.forms.page_form.testSuiteId.value=document.forms.page_form.retrieveTestSuiteId.value;
    	document.forms.page_form.actionType.value='GetTestSuite';
    	document.forms.page_form.submit();
    }
}


function newTestSuite()
{
	document.forms.page_form.actionType.value='InitTestSuite';
	document.forms.page_form.submit();
}


function saveTestSuite()
{
    if (document.forms.page_form.testSuiteName.value == "") {
        alert("Please specify a Test Suite name.");
        
    } else {
    
        if (document.forms.page_form.testSuiteId.value == ""
                || document.forms.page_form.testSuiteId.value <= 0) {
    
        	document.forms.page_form.actionType.value='CreateTestSuite';
    	} else {
        	document.forms.page_form.actionType.value='SaveTestSuite';
    	}
	
    	document.forms.page_form.submit();
    }
}


function deleteTestSuite()
{
	document.forms.page_form.actionType.value='DeleteTestSuite';
	document.forms.page_form.submit();
}


function runTestSuite()
{
	var testSuiteId = document.forms.page_form.testSuiteId.value;
	var testSuiteName = document.forms.page_form.testSuiteName.value;
    var reportXSLFilename = document.forms.page_form.reportXSLFilename.value;
    
    // Run the Test Suite from a new window.
	window.open("/regressiontest/testSuiteProcessing.jsp?testSuiteId=" + testSuiteId + "&testSuiteName=" + testSuiteName + "&reportXSLFilename=" + reportXSLFilename);

    // Reset the current screen.
    newTestSuite();
}

function showTestSuiteReport() {

    var reportId = document.forms.page_form.retrieveTestReportId.value;
    
    window.open("/regressiontest/report/payRulesReport.jsp?reportId=" + reportId);
}