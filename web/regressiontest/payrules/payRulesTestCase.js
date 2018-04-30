/*
 * Test Case Functions.
 *
 */
 
// This function is take from the timesheet parameters screen.  dailytimesheet/dailyselection.jsp
function setDates() {
    var endDate, dat_endDate;
    if ((document.forms[0].startDate) && (document.forms[0].endDate)) {
        startDate=document.forms[0].startDate.value;
        endDate=document.forms[0].endDate.value;
        if (startDate != '') {
            if (startDate > endDate) {
                document.forms[0].endDate_dummy.value=document.forms[0].startDate_dummy.value;
                document.forms[0].endDate.value=document.forms[0].startDate.value;
            }
	    }
    }
}
 
function checkMandatoryFields() {
	
	var message = ""; 
	
	if (document.forms.page_form.employeeIds.value == ""
    	    && document.forms.page_form.teamIds.value == ""
    	    && document.forms.page_form.payGroupIds.value == ""
    	    && document.forms.page_form.calcGroupIds.value == ""
    	) {
	    
	    message = "Please specify the criteria for selecting employees.";

	} else if (document.forms.page_form.startDate.value == "") {
	    message = "Please specify a Test Case Start Date.";

	}
	
	if (message != "") {
		alert(message);
		return false;
	} else {
		return true;
	}
} 

 
function addTestCase()
{
	if (checkMandatoryFields()) {

		prepareOutputAttribForSubmit();
		
		document.forms.page_form.actionType.value='AddTestCase';
		document.forms.page_form.submit();
	}
}


function deleteTestCase()
{
    var caseIdList = "";
    var caseId;
    var formField;
    
    // Look through the form for any fields named deleteCaseId_xxx.
    for (var i=0; i < document.forms.page_form.elements.length; i++) {
        
        formField = document.forms.page_form.elements[i];
        
        // If this is a deleteCaseId and it is checked.
        if (formField.name.indexOf("deleteCaseId_") >= 0
                && formField.checked == true) {
            
            // CaseId is after the "_".
            caseId = formField.name.substring(formField.name.indexOf("_")+1, formField.name.length);
            caseIdList = caseIdList + caseId + ",";

        }
    }
    
    if (caseIdList != "") {
        // strip the trailing ','
        caseIdList = caseIdList.substring(0, caseIdList.length-1);

    	document.forms.page_form.testCaseId.value=caseIdList;
    	document.forms.page_form.actionType.value='DeleteTestCase';
    	document.forms.page_form.submit();
    }
}

function reCreateTestCase() 
{
    var caseIdList = "";
    var caseId;
    var formField;
    
    // Look through the form for any fields named reCreateCaseId_xxx.
    for (var i=0; i < document.forms.page_form.elements.length; i++) {
        
        formField = document.forms.page_form.elements[i];
        
        // If this is a deleteCaseId and it is checked.
        if (formField.name.indexOf("reCreateCaseId_") >= 0
                && formField.checked == true) {
            
            // CaseId is after the "_".
            caseId = formField.name.substring(formField.name.indexOf("_")+1, formField.name.length);
            caseIdList = caseIdList + caseId + ",";

        }
    }
    
    if (caseIdList != "") {
        // strip the trailing ','
        caseIdList = caseIdList.substring(0, caseIdList.length-1);

    	document.forms.page_form.testCaseId.value=caseIdList;
    	document.forms.page_form.actionType.value='ReCreateTestCase';
    	document.forms.page_form.submit();
    }
}


function copyTestCase(copyCaseId, empIdFieldName)
{
    var empIdFieldValue = document.getElementsByName(empIdFieldName).item(0).value; 
    var empNameFieldValue = document.getElementsByName(empIdFieldName + "_label").item(0).value; 
    
	if (empIdFieldValue == "") {
		alert("Please select an Employee(s) to Copy to.");
		return;
	}
	
   	document.forms.page_form.testCaseId.value=copyCaseId;
   	document.forms.page_form.copyToEmpIdList.value=empIdFieldValue;
   	document.forms.page_form.copyToEmpNameList.value=empNameFieldValue;
	document.forms.page_form.actionType.value='CopyTestCase';
	document.forms.page_form.submit();
}

function saveChangesTestCase()
{   
    var caseIdList = "";
    var caseId;
    var formField;

    // Look through the form for any fields named deleteCaseId_xxx.
    for (var i=0; i < document.forms.page_form.elements.length; i++) {
        
        formField = document.forms.page_form.elements[i];
        
        // If this is a deleteCaseId and it is checked.
        if (formField.name.indexOf("testCaseName_") >= 0) {
            // CaseId is after the "_".
            caseId = formField.name.substring(formField.name.indexOf("_")+1, formField.name.length);
            caseIdList = caseIdList + caseId + ",";

        }
    }    	

	if (caseIdList != "") {
        // strip the trailing ','
        caseIdList = caseIdList.substring(0, caseIdList.length-1);

    	document.forms.page_form.testCaseId.value=caseIdList;
    	document.forms.page_form.actionType.value='SaveChangesTestCase';
    	document.forms.page_form.submit();
    }
   
}

function setDeleteAllCases(isChecked) 
{
	setAllCheckboxes(isChecked, "deleteCaseId_");
}


function setReCreateAllCases(isChecked)
{
	setAllCheckboxes(isChecked, "reCreateCaseId_");
}


function setAllCheckboxes(isChecked, fieldNamePrefix)
{
    // Look through the form for any fields named reCreateCaseId_xxx.
    for (var i=0; i < document.forms.page_form.elements.length; i++) {
        
        formField = document.forms.page_form.elements[i];
        
        // If this is a deleteCaseId and it is checked.
        if (formField.name.indexOf(fieldNamePrefix) >= 0) {
			formField.checked = isChecked;
        }
    }
}

function warning(){
	document.getElementById("warning").style.display = 'block';
	
}
