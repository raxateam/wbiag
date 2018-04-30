function startProcess(actionTypeVal, disableLaborMetrics){

	var customValidation = window.document.page_form.enableCustomValidation.value;

    if(areTimesValid())
    {
    	if(disableLaborMetrics == "true" || (areLaborTimesValid() && gapsInLaborTimes()))
    	{
	    	if("true" == customValidation)
	    	{
	    		if(customizeValidation())
	    		{
	        		window.document.page_form.actionType.value = actionTypeVal;
	      			window.document.page_form.submit();
	      		}
	    	}
	    	else
	    	{
	    		window.document.page_form.actionType.value = actionTypeVal;
	      		window.document.page_form.submit();
	    	}
    	}
    }
} // end startProcess


function startRetrieve(actionTypeVal){

	var empString = window.document.page_form.retEmpName.value;
    var empDate = window.document.page_form.retStartDate.value;
    var valid = true;

	if (actionTypeVal == "retrieve") {

	    if (empString.indexOf(",", 0) > 0 || empString == "") {
	        valid = false;
		    alert(window.document.page_form.mustSelectEmployeeMsg.value);
	    } else if (empDate == "") {
	        valid = false;
		    alert(window.document.page_form.mustSelectDateMsg.value);
        }
	}

	if (valid == true) {
	    window.document.page_form.actionType.value = actionTypeVal;
	    window.document.page_form.submit();
	}
}

function getInfo(actionTypeVal){
    var empString = window.document.page_form.empName.value;
    if(empString.indexOf(",", 0) > 0 || empString == "")
    {
        alert(window.document.page_form.mustSelectEmployeeMsg.value);
    }
    else
    {
        window.document.page_form.actionType.value = actionTypeVal;
        window.document.page_form.submit();
    }
}
function addRow(actionTypeVal, row){
        window.document.page_form.actionType.value = actionTypeVal;
        window.document.page_form.whichRow.value = row;
        window.document.page_form.submit();
} // end startProcess

function prePopulate(i, when, flag){
    if(when == "start" && flag)
    {
        eval("window.document.page_form.LabStartTime_"+i+"_1_time.value = window.document.page_form.shiftStartTime_"+i+"_shift_1_time.value");
        eval("window.document.page_form.LabStartTime_"+i+"_1.value = window.document.page_form.shiftStartTime_"+i+"_shift_1.value");
    }
    else if(when == "end" && flag)
    {
        eval("window.document.page_form.LabEndTime_"+i+"_1_time.value = window.document.page_form.shiftEndTime_"+i+"_shift_1_time.value");
        eval("window.document.page_form.LabEndTime_"+i+"_1.value = window.document.page_form.shiftEndTime_"+i+"_shift_1.value");
    }
}

function populateRetrieve(){
    var empString = window.document.page_form.empName.value;
    if(empString.indexOf(",", 0) == -1)
    {
        window.document.page_form.retEmpName_label.value = window.document.page_form.empName_label.value;
        window.document.page_form.retEmpName.value = empString;
    }
}

function setDays(actionType){
    window.document.page_form.numberOfDaysSet.value=window.document.page_form.numberOfDays.value;
    window.document.page_form.actionType.value = actionType;
    window.document.page_form.submit();
}// end setDays


function limitValues(obj, msg){
    minValue = 1;
    maxValue = window.document.page_form.maxNumDays.value;

    intValue = parseInt(obj.value);

    if(maxValue != "" && (isNaN(intValue) || intValue < minValue || intValue > maxValue)){
        alert(msg);
        obj.focus();
    }
}

function activateEndDate(){
    if(eval('window.document.page_form.patternEndDate_dummy')) {
        window.document.page_form.patternEndDate_dummy.disabled=window.document.page_form.isPermanent.checked;
    }
} // end activateEndDate

function areTimesValid(){
	var daysNo = window.document.page_form.numberOfDaysSet.value;
	var shiftsNo = document.page_form.numberOfShifts.value;

	for (j=1; j<=shiftsNo; j++){
	    for (i=1; i<=daysNo; i++){
	        val1 = eval("window.document.page_form.shiftStartTime_" + i + "_shift_" + j + ".value");
	        val2 = eval("window.document.page_form.shiftEndTime_" + i + "_shift_" + j + ".value");

	        // Check the shift start/end times.
	        if(((val1=="")&&(val2!="")) || ((val1!="")&&(val2==""))){
	            alert("Both Shift Start/End times for day " +i + " shift " + j + " have to be empty or populated.\nPlease fix the problem before processing  the form.");
	            return false;
	        }
	        bothSTEmpty = ((val1=="") && (val2==""));

			// Check the break start/end times.
			fieldName = "document.page_form.breakStartTime_" + i + "_shift_" + j;
			if (undefined==eval(fieldName)) {
			    val1 = "";
			} else {
			    val1 = eval(fieldName + ".value");
			}

			fieldName = "document.page_form.breakEndTime_" + i + "_shift_" + j;
			if (undefined==eval(fieldName)) {
			    val2 = "";
			} else {
			    val2 = eval(fieldName + ".value");
			}

	        if(((val1=="")&&(val2!="")) || ((val1!="")&&(val2==""))){
	            alert("Both Break Start/End times for day " +i + " shift " + j + " have to be empty or populated.\nPlease fix the problem before processing  the form.");
	            return false;
	        }
	        bothBTEmpty = ((val1=="") && (val2==""));


	        if(bothSTEmpty && (!bothBTEmpty)){
	            alert("Cannot insert Break Times without having Shift Times for day " + i + " shift " + j + ".\nPlease fix the problem before processing  the form.");
	            return false;
	        }
	    }
	}
    return true;
} // end areTimesValid

function areLaborTimesValid()
{
    var flag;
    var count;
    var shiftStartTime;
    var shiftEndTime;
    var labStartTime;
    var labEndTime;
    var daysNo = window.document.page_form.numberOfDaysSet.value;

    for (i=1; i<=daysNo; i++)
    {
        flag = 1;
        count = 1;
        shiftStartTime = eval("window.document.page_form.shiftStartTime_" + i + "_shift_1.value").substring(9,15);
        shiftEndTime = eval("window.document.page_form.shiftEndTime_" + i + "_shift_1.value").substring(9,15);

        while(undefined!=eval("window.document.page_form.LabStartTime_" + i + "_" + count))
        {
            labStartTime = eval("window.document.page_form.LabStartTime_" + i + "_" + count + ".value").substring(9,15);
            labEndTime = eval("window.document.page_form.LabEndTime_" + i + "_" + count + ".value").substring(9,15);

            if(labStartTime != "" || labEndTime != "")
            {
                if(shiftStartTime > labStartTime || shiftEndTime < labStartTime)
                {
                    alert(window.document.page_form.validateLaborMsg1.value);
                    return false;
                }
                else if(shiftStartTime > labEndTime || shiftEndTime < labEndTime)
                {
                    alert(window.document.page_form.validateLaborMsg1.value);
                    return false;
                }
                else if(labEndTime < labStartTime)
                {
                    alert(window.document.page_form.validateLaborMsg1.value);
                    return false;
                }
            }
            count++;
        }
    }
    return true;
}

function gapsInLaborTimes()
{
    var flag;
    var found;
    var valid;
    var count;
    var shiftStartTime;
    var shiftEndTime;
    var labStartTime;
    var labEndTime;
    var daysNo = window.document.page_form.numberOfDaysSet.value;

    if("false" == window.document.page_form.checkGaps.value)
    {
        return true;
    }
    for (i=1; i<=daysNo; i++)
    {
        flag = 1;
        count = 1;
        valid = false;
        shiftStartTime = eval("window.document.page_form.shiftStartTime_" + i + "_shift_1.value").substring(9,15);
        shiftEndTime = eval("window.document.page_form.shiftEndTime_" + i + "_shift_1.value").substring(9,15);

        while(undefined!=eval("window.document.page_form.LabStartTime_" + i + "_" + count))
        {
            count2 = 1;
            found = false;
            labStartTime = eval("window.document.page_form.LabStartTime_" + i + "_" + count2 + ".value").substring(9,15);
            if(labStartTime == "")
            {
                valid = true;
                break;
            }
            while(undefined!=eval("window.document.page_form.LabStartTime_" + i + "_" + count2))
            {
                labStartTime = eval("window.document.page_form.LabStartTime_" + i + "_" + count2 + ".value").substring(9,15);
                labEndTime = eval("window.document.page_form.LabEndTime_" + i + "_" + count2 + ".value").substring(9,15);

                if(labStartTime == shiftStartTime)
                {
                    found = true;
                    if(labEndTime == shiftEndTime)
                    {
                        valid = true;
                    }
                    else
                    {
                        shiftStartTime = labEndTime;
                    }
                    break;
                }
                count2++;
            }
            if(!found)
            {
                alert(window.document.page_form.checkGapInvalidMsg.value);
                return false;
            }
            count++;
        }
        if(!valid)
        {
            alert(window.document.page_form.checkGapInvalidMsg.value);
            return false;
        }
    }
    return true;

}

function displaySpan(name){
    //alert(document.getElementById(name).style.display);
    if(document.getElementById(name).style.display =='none'){
        document.getElementById(name).style.display ='';
    } else {
        document.getElementById(name).style.display ='none';
    }
} // end displaySpan


function showHideAll(){

    var daysNo = document.page_form.numberOfDaysSet.value;
    var shiftsNo = document.page_form.numberOfShifts.value;

    if(window.document.page_form.showAll.value=='true'){
        for(j=1;j<=daysNo;j++){
			for(i=1;i<=shiftsNo;i++){
	            document.getElementById("SHIFT_GROUP_SPAN_TITLE_" + j).style.display ='';
	            document.getElementById("SHIFT_YAG_SPAN_TITLE_" + j).style.display ='';
	            document.getElementById("SHIFT_COLOR_SPAN_TITLE_" + j).style.display ='';
	            document.getElementById("BREAK_HT_SPAN_TITLE_" + j).style.display ='';
	            document.getElementById("BREAK_TC_SPAN_TITLE_" + j).style.display ='';
	            document.getElementById("SHIFT_GROUP_SPAN_" + j + "_shift_" + i).style.display ='';
	            document.getElementById("SHIFT_YAG_SPAN_" + j + "_shift_" + i).style.display ='';
	            document.getElementById("SHIFT_COLOR_SPAN_" + j + "_shift_" + i).style.display ='';
	            document.getElementById("BREAK_HT_SPAN_" + j + "_shift_" + i).style.display ='';
	            document.getElementById("BREAK_TC_SPAN_" + j + "_shift_" + i).style.display ='';
        	}
        }
        window.document.page_form.showAll.value='false';
    } else {
        for(j=1;j<=daysNo;j++){
        	for(i=1;i<=shiftsNo;i++){
	            document.getElementById("SHIFT_GROUP_SPAN_TITLE_" + j).style.display ='none';
	            document.getElementById("SHIFT_YAG_SPAN_TITLE_" + j).style.display ='none';
	            document.getElementById("SHIFT_COLOR_SPAN_TITLE_" + j).style.display ='none';
	            document.getElementById("BREAK_HT_SPAN_TITLE_" + j).style.display ='none';
	            document.getElementById("BREAK_TC_SPAN_TITLE_" + j).style.display ='none';
	            document.getElementById("SHIFT_GROUP_SPAN_" + j + "_shift_" + i).style.display ='none';
	            document.getElementById("SHIFT_YAG_SPAN_" + j + "_shift_" + i).style.display ='none';
	            document.getElementById("SHIFT_COLOR_SPAN_" + j + "_shift_" + i).style.display ='none';
	            document.getElementById("BREAK_HT_SPAN_" + j + "_shift_" + i).style.display ='none';
	            document.getElementById("BREAK_TC_SPAN_" + j + "_shift_" + i).style.display ='none';
	        }
        }
        window.document.page_form.showAll.value='true';
    }
} // end showHideAll


function setRows(noRows, divName) {
    showThis="<tr><td></td>" +
             "<td><font color='navy'>Shift Start Time</td>" +
             "<td><font color='navy'>Shift End Time</td>" +
             "<td><font color='navy'>Break Start Time</td>" +
             "<td><font color='navy'>Break End Time</td>" +
             "<td><font color='navy'>Break Duration</td>" +
             "<td><font color='navy'>Break Default Hour Type</td>" +
             "<td><font color='navy'>Break Time Code</td></tr>";
    for (i = 1; i <= noRows; i++) {
        fname1 = "shiftStartTime_" + i + "_shift_1";
        fname2 = "shiftEndTime_" + i + "_shift_1";
        fname3 = "breakStartTime_" + i;
        fname4 = "breakEndTime_" + i;
        fname5 = "breakDuration_" + i;
        fname6 = "breakDefaultHourType_" + i;
        fname7 = "breakTimeCode_" + i;

        showThis= showThis + "<tr><td><font color='navy'>Day_" + i + "</td>" +
        "<td><input type='text' name='" + fname1 + "' value='' size='5' maxlength='5'></td>" +
        "<td><input type='text' name='" + fname2 + "' value='' size='5' maxlength='5'></td>" +
        "<td><input type='text' name='" + fname3 + "' value='' size='5' maxlength='5'></td>" +
        "<td><input type='text' name='" + fname4 + "' value='' size='5' maxlength='5'></td>" +
        "<td><input type='text' name='" + fname5 + "' value='' size='3' maxlength='3'></td>" +
        "<td><input type='text' name='" + fname6 + "' value='' size='10' maxlength='10'></td>" +
        "<td><input type='text' name='" + fname7 + "' value='' size='10' maxlength='10'></td>" +
        "</tr>";
    }
    document.getElementById(divName).innerHTML = "<table class='contentTable' width='100%' border='1'>" + showThis + "</table>";
} // end setRows