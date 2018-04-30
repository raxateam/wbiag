/*
 * Output Attribute Functions.
 *
 */

function removeBlanks(listbox) {

	if (listbox.options == null) {
		return;
	}

	var length = listbox.options.length;

	// Need to work backwards since we will be removing
	// items from the list, thus changing the count.
	for (var i = length - 1; i >= 0; i--) {
		if (listbox.options[i].text == "") {
			listbox.options[i] = null;
		}
	}
}
 
function selectAll(listbox) {

	if (listbox.options == null) {
		return;
	}

	var length = listbox.options.length;

	for (var i = 0; i < length; i++) {
		listbox.options[i].selected = true;
	}
} 
 
function initOutputAttrib() {
	removeBlanks(document.forms.page_form.workSummaryFieldsExclude);
	removeBlanks(document.forms.page_form.workSummaryFieldsInclude);
	removeBlanks(document.forms.page_form.workDetailFieldsExclude);
	removeBlanks(document.forms.page_form.workDetailFieldsInclude);
	removeBlanks(document.forms.page_form.workPremiumFieldsExclude);
	removeBlanks(document.forms.page_form.workPremiumFieldsInclude);
	removeBlanks(document.forms.page_form.employeeBalancesExclude);
	removeBlanks(document.forms.page_form.employeeBalancesInclude);
} 
 
function prepareOutputAttribForSubmit() {
	selectAll(document.forms.page_form.workSummaryFieldsInclude);
	selectAll(document.forms.page_form.workDetailFieldsInclude);
	selectAll(document.forms.page_form.workPremiumFieldsInclude);
	selectAll(document.forms.page_form.employeeBalancesInclude);
} 
 
function moveOptions(source, target) {

	var optionObject = null;
	var optionRank = 0;
	var sourceLength = source.options.length;
	
	// Need to work backwards since we will be removing
	// items from the list, thus changing the count.
	for (var i = sourceLength - 1; i >= 0; i--) {
	
		if (source.options[i].selected == true) {

			// Add to the target.
			optionObject = new Option(source.options[i].text, source.options[i].value);
		    optionRank = target.options.length;
		    target.options[optionRank] = optionObject;

			// Remove from the source.
		    source.options[i] = null;
		}
	}
}


function addWorkSummaryField()
{
	if (document.forms.page_form.workSummaryFieldsExclude.selectedIndex != -1) {

		moveOptions(document.forms.page_form.workSummaryFieldsExclude,
					document.forms.page_form.workSummaryFieldsInclude
					);
	}
}

function removeWorkSummaryField()
{
	if (document.forms.page_form.workSummaryFieldsInclude.selectedIndex != -1) {

		moveOptions(document.forms.page_form.workSummaryFieldsInclude,
					document.forms.page_form.workSummaryFieldsExclude
					);
	}
}


function addWorkDetailField()
{
	if (document.forms.page_form.workDetailFieldsExclude.selectedIndex != -1) {

		moveOptions(document.forms.page_form.workDetailFieldsExclude,
					document.forms.page_form.workDetailFieldsInclude
					);
	}
}


function removeWorkDetailField()
{
	if (document.forms.page_form.workDetailFieldsInclude.selectedIndex != -1) {

		moveOptions(document.forms.page_form.workDetailFieldsInclude,
					document.forms.page_form.workDetailFieldsExclude
					);
	}
}


function addWorkPremiumField()
{
	if (document.forms.page_form.workPremiumFieldsExclude.selectedIndex != -1) {

		moveOptions(document.forms.page_form.workPremiumFieldsExclude,
					document.forms.page_form.workPremiumFieldsInclude
					);
	}
}


function removeWorkPremiumField()
{
	if (document.forms.page_form.workPremiumFieldsInclude.selectedIndex != -1) {

		moveOptions(document.forms.page_form.workPremiumFieldsInclude,
					document.forms.page_form.workPremiumFieldsExclude
					);
	}
}


function addEmployeeBalance()
{
	if (document.forms.page_form.employeeBalancesExclude.selectedIndex != -1) {

		moveOptions(document.forms.page_form.employeeBalancesExclude,
					document.forms.page_form.employeeBalancesInclude
					);
	}
}


function removeEmployeeBalance()
{
	if (document.forms.page_form.employeeBalancesInclude.selectedIndex != -1) {

		moveOptions(document.forms.page_form.employeeBalancesInclude,
					document.forms.page_form.employeeBalancesExclude
					);
	}
}
