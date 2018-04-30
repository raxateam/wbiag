<%@ include file="/system/wbheader.jsp"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMWTShelper"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMBiWeeklyTimeSheetConstants"%>
<%!
public static int MFRM_ID = -99;
%>
<%
MFRM_ID = CSMWTShelper.getMfrmId(request);
%>
<wb:page maintenanceFormId='<%=MFRM_ID%>'>
<script type="text/javascript" src="<%= request.getContextPath() %>/modules/retailSchedule/scripts/date.js"></script>
<script Language="JavaScript">
	var bHasFormBeenSubmitted=false;

	function noMealBreak( day ) {
		document.forms[0].elements['BRK_START_TIME_'+day+'_time'].value='';
		document.forms[0].elements['BRK_END_TIME_'+day+'_time'].value='';
		document.forms[0].elements['BRK_START_TIME_'+day+'_time'].blur();
		document.forms[0].elements['BRK_END_TIME_'+day+'_time'].blur();
	}

	function noMealBreakChecked( day ) {
		if ((document.forms[0].elements['BRK_START_TIME_'+day+'_time'].value != '') &&
		 	(document.forms[0].elements['BRK_END_TIME_'+day+'_time'].value != '')) {
		 		document.forms[0].elements['NO_MEAL_BREAK_'+day].checked=false;
		 }
		 if ((document.forms[0].elements['BRK_START_TIME_'+day+'_time'].value == '') &&
		 	(document.forms[0].elements['BRK_END_TIME_'+day+'_time'].value == '')) {
		 		document.forms[0].elements['NO_MEAL_BREAK_'+day].checked=true;
		 }
	}

	function delAllElapsedTimes( numElapsedTimeLines ) {
		for (i = 0; i < numElapsedTimeLines; i++) {
			del = document.forms[0].elements['WTSDELET_' + i];
			if (del.disabled == false) {
				del.checked = true;
			}
		}
	}

	function openPopup(url, winWidth, winHeight){
        posX = window.screen.width / 7;
        posY = window.screen.height / 5;
        var width = "";
        var height = "";

        if (winWidth != null) {
        	width = "width=" + winWidth + ",";
        }
        if (winHeight != null) {
        	height = "height=" + winHeight + ",";
        }

        newWindowOptions = width + height + "scrollbars=1,alwaysRaised=1, resizable=1, left="+posX+",top="+posY;
        window.open( contextPath + url, 'popUpWindow', newWindowOptions);
    }

	function openPopup(url, winWidth, winHeight, windowId){
        posX = window.screen.width / 7;
        posY = window.screen.height / 5;
        var width = "";
        var height = "";

        if (winWidth != null) {
        	width = "width=" + winWidth + ",";
        }
        if (winHeight != null) {
        	height = "height=" +  winHeight + ",";
        }

        newWindowOptions = width + height + "scrollbars=1,alwaysRaised=1, resizable=1, left="+posX+",top="+posY;
        url = url.searchReplace(" ", "+");
        window.open( contextPath + url, windowId, newWindowOptions);
    }

    function fdapCheckboxOnClick(day, line, fullDayAbsenceLines, userChecked) {
    	documentChanged();
		var prefix = 'WTSLTA' + day + 'X_';
		for (ln = 0; ln < fullDayAbsenceLines; ln++) {
			var elem = document.forms[0].elements[prefix + ln];

			if (ln != line) {
				elem.disabled = userChecked;
				if (userChecked) {
					document.forms[0].elements['WTSLTAALL_' + ln].disabled = true;
				}
			}
		}

		if (userChecked) {
			// If the user checked a checkbox on this line, then if all other checkboxes
			// on this line are checked then check the 'all' checkbox.
			// The other 'all' checkboxes are already disabled at this point.
			// Do nothing if this 'all' checkbox is already disabled.
			var allCheckbox = document.forms[0].elements['WTSLTAALL_' + line];
			if (!allCheckbox.disabled) {
				var checkAllCheckbox = true;
				var tail = 'X_' + line;
		    	for (i = 0; i < <%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++) {
		    		if (!document.forms[0].elements['WTSLTA' + i + tail].checked) {
	    				checkAllCheckbox = false;
	    				break;
			    	}
			    }
	   			allCheckbox.checked = checkAllCheckbox;
		    }
		} else {
			for (ln = 0; ln < fullDayAbsenceLines; ln++) {
				// The 'all' checkbox for this line gets checked if all days are checked & enabled.
				// If all days are enabled then the all checkbox for this line is enabled.
				var allCheckbox = document.forms[0].elements['WTSLTAALL_' + ln];
				var checkAllCheckbox = true;
				var disableAllCheckbox = false;
				var tail = 'X_' + ln;

		    	for (i = 0; i < <%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++) {
		    		var checkbox = document.forms[0].elements['WTSLTA' + i + tail];
			    	if (checkbox.disabled) {
						disableAllCheckbox = true;
		    			checkAllCheckbox = false;
						break;
			    	}
		    		if (!checkbox.checked) {
		    			checkAllCheckbox = false;
			    	}
			    }
				allCheckbox.checked  = checkAllCheckbox;
				allCheckbox.disabled = disableAllCheckbox;
			}
		}
    }

	function fdapAllCheckboxOnClick(line, userChecked, fullDayAbsenceLines) {
		documentChanged();
		document.forms[0].elements["WTSLTAALL_" + line].checked = userChecked;
		for (ln = 0; ln < fullDayAbsenceLines; ln++) {
			var tail = "X_" + ln;
			if (ln != line) {
				document.forms[0].elements["WTSLTAALL_" + ln].disabled = userChecked;
				for (day = 0; day < <%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; day++) {
					document.forms[0].elements["WTSLTA" + day + tail].disabled = userChecked;
				}
			} else {
				document.forms[0].elements["WTSLTAALL_" + ln].checked = userChecked;
				for (day = 0; day < <%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; day++) {
					document.forms[0].elements["WTSLTA" + day + tail].checked = userChecked;
				}
			}
		}
    }

	function submitTimesheet() {
		if(checkFormSubmittedMarkIfNot()==true){
			return;
		}
		if (!checkOverlappingTime()){
			return;
		}
		if (confirmOnSubmit()) {
			document.forms[0].action='<%= request.getContextPath() + CSMWTShelper.getActionURL(CSMBiWeeklyTimeSheetConstants.ACTION_SUBMIT_TIMESHEET)%>';document.forms[0].submit();
			if (document.forms[0].Submit) {
				document.forms[0].Submit.disabled=true;
		    }
		    if (document.forms[0].SubmitForApproval) {
                document.forms[0].SubmitForApproval.disabled=true;
            }
            if (document.forms[0].SubmitOverride) {
				document.forms[0].SubmitOverride.disabled=true;
			}
		}
	}

	function newShift(shift) {
		if(checkFormSubmittedMarkIfNot()==true){
			return;
		}
		if (confirmOnSubmit()) {
			document.forms[0].action='<%= request.getContextPath() %>/action/timesheet.action?action=NewShiftAction&SHIFT=' + shift;document.forms[0].submit();
			if (document.forms[0].Submit) {
     			document.forms[0].Submit.disabled=true;
     		}
     		if (document.forms[0].SubmitForApproval) {
	            document.forms[0].SubmitForApproval.disabled=true;
	        }
			if (document.forms[0].SubmitOverride) {
				document.forms[0].SubmitOverride.disabled=true;
			}
		}
	}

	function checkFormSubmittedMarkIfNot(){
		if(bHasFormBeenSubmitted==true){
			return true;
		}
		bHasFormBeenSubmitted=true;
		return false;
	}

    function onSubmit( url ) {
        document.forms[0].action=url;
        document.forms[0].submit();
    }

    function documentChanged() {
        document.forms[0].DOC_CHANGED.value = "Y";
    }

    var WTS_WARN_WHEN_SUBMITTED_NOT_CHECKED = <%= String.valueOf( Registry.getVarBoolean("/system/weekly timesheet/WTS_WARN_WHEN_SUBMITTED_NOT_CHECKED", true) ) %>;
    var WTS_PROMPT_FOR_MESSAGE_ON_SUBMIT = <%= String.valueOf( Registry.getVarBoolean("/system/weekly timesheet/WTS_PROMPT_FOR_MESSAGE_ON_SUBMIT", true) ) %>;

    function submitToSupervisor() {
        if( !checkOverlappingTime() )
          return;

        if( !confirmOnSubmit() )
          return;

        var answer;
        if( !document.forms[0].elements['SUBMITTED'].checked && WTS_WARN_WHEN_SUBMITTED_NOT_CHECKED ) {
            answer = confirm(getToSupervisorMsg());
        } else {
            answer = true;
        }

        if (answer) {
            var inputCom;
            if( WTS_PROMPT_FOR_MESSAGE_ON_SUBMIT )
                inputCom = prompt (supervisorComment()," ");
            if (inputCom==null){
                inputCom = ' ';
            }
            document.forms[0].elements['SUBMITTED'].checked = true;
            if (document.forms[0].Submit) {
                document.forms[0].Submit.disabled=true;
            }
            if (document.forms[0].SubmitForApproval) {
	            document.forms[0].SubmitForApproval.disabled=true;
	        }
            document.forms[0].action='<%= request.getContextPath() + CSMWTShelper.getActionURL(CSMBiWeeklyTimeSheetConstants.ACTION_SUBMIT_TO_SUPERVISOR)%>&SUP_COMMENT='+inputCom; document.forms[0].submit();
            //window.location ='<%= request.getContextPath() %>/action/timesheet.action?action=SubmitToSupervisorAction&SUP_COMMENT='+inputCom;
            alert ("<wb:localize id="YOUR_TIMESHEET_HAS_BEEN_SUBMITTED" ignoreConfig="true" escapeForJavascript="true">Your timesheet has been submitted</wb:localize>");
        }

    }
  function errorPopup(){
    alert ("<wb:localize id="ERROR_ON_SUBMIT" ignoreConfig="true" escapeForJavascript="true">There are errors in your timesheet.  Please correct the error and try again</wb:localize>");

  }

        function sendMessage(){
                var inputCom = prompt (supervisorComment(),"");
                window.location ='<%= request.getContextPath()  + CSMWTShelper.getActionURL(CSMBiWeeklyTimeSheetConstants.ACTION_SUBMIT_TO_SUPERVISOR)%>&SUP_COMMENT='+inputCom;
                alert ("<wb:localize id="YOUR_TIMESHEET_HAS_BEEN_SUBMITTED" ignoreConfig="true" escapeForJavascript="true">Your timesheet has been submitted</wb:localize>");
        }

        function forwardTimesheet(){
                var popUpWindow;
        posX = window.screen.width / 7;
        posY = window.screen.height / 5;

        //winHeight = 600;
        winWidth = 400;

        newWindowID = "popUpWindow";
        newWindowOptions = "width="+winWidth+",scrollbars=1,alwaysRaised=1, resizable=1, left="+posX+",top="+posY;
        //alert( url );
        window.open( contextPath + '/timesheet/forwardSelect.jsp?pageSize=10','popUpWindow',newWindowOptions);
        }

    function confirmedLeave( url, msg ) {
        url = '<%= request.getContextPath() %>' + url;
        if( document.forms[0].DOC_CHANGED.value == "Y" ) {
                var answer = confirm( getConfirmedLeaveMsg() );
                if( answer ) {
                        window.location = url;
                }
        } else {
            window.location = url;
        }
    }

    function confirmedLeaveWithSubmit( url, msg ) {

        if( document.forms[0].DOC_CHANGED.value == "Y" ) {
                var answer = confirm( getConfirmedLeaveMsg() );
                if( answer ) {
                     document.forms[0].action=url;
                     setScrollPosition();
                     document.forms[0].submit();
                }
        } else {
            document.forms[0].action=url;
            setScrollPosition();
            document.forms[0].submit();
        }
    }

/*    function weekSelect(){
      if (document.all) {
        // Internet Explorer (separate procedure because it queries the options differently than NS.
          if (document.forms[0].elements['WEEK_START_DATE'].value != -99){
              if( document.forms[0].DOC_CHANGED.value == "Y" ) {
                  var answer = confirm( getConfirmedLeaveMsg() );
                  if ( answer ){
                      var tempLink = document.forms[0].elements['WEEK_START_DATE'].value;
                      tempLink = tempLink + " 000000";
                      //window.location ='<%= request.getContextPath() %>/action/timesheet/NewWeek?NEWWEEK='+tempLink;
                      window.location ='<%= request.getContextPath() %>/action/timesheet.action?action=NewWeekAction&NEWWEEK='+tempLink;
                  }
              } else {
                  var tempLink = document.forms[0].elements['WEEK_START_DATE'].value;
                  tempLink = tempLink + " 000000";
                  //window.location = '<%= request.getContextPath() %>/action/timesheet/NewWeek?NEWWEEK='+tempLink;
                  window.location = '<%= request.getContextPath() %>/action/timesheet.action?action=NewWeekAction&NEWWEEK='+tempLink;
              }
          }
    } else {
        // NETSCAPE (separate procedure because it queries the options differently than IE.
      if (document.forms[0].elements['WEEK_START_DATE'].options[document.forms[0].elements['WEEK_START_DATE'].selectedIndex].value != -99){
              if( document.forms[0].DOC_CHANGED.value == "Y" ) {
                  var answer = confirm( getConfirmedLeaveMsg() );
                  if ( answer ){
                      var tempLink = document.forms[0].elements['WEEK_START_DATE'].options[document.forms[0].elements['WEEK_START_DATE'].selectedIndex].value
                      tempLink += " 000000";
                      //window.location ='<%= request.getContextPath() %>/action/timesheet/NewWeek?NEWWEEK='+tempLink;
                      window.location ='<%= request.getContextPath() %>/action/timesheet.action?action=NewWeekAction&NEWWEEK='+escape(tempLink);
                  }
              } else {
                  var tempLink = document.forms[0].elements['WEEK_START_DATE'].options[document.forms[0].elements['WEEK_START_DATE'].selectedIndex].value;
                  tempLink += " 000000";
                  //window.location = '<%= request.getContextPath() %>/action/timesheet/NewWeek?NEWWEEK='+tempLink;
                  window.location = '<%= request.getContextPath() %>/action/timesheet.action?action=NewWeekAction&NEWWEEK='+escape(tempLink);
              }
          }
    }
    }
*/
    function trim(a){
        return a.replace(/^\s+/,'').replace(/\s+$/,'')
    }

        function hasAnyComments() {
                for( i=0; i<<%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++ ) {
                var name = 'COMMENTS_' + i;
                var val = document.forms[0].elements[name].value;
                    if (val != null && trim(val) != '') {
                        return true;
                    }
            }
                return false;
        }

        function hasAnyFlagsOrUDFs() {
            var name;
                var value;
                for( i=0; i<<%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++ ) {
                    for( j=0; j<5; j++ ) {
                        name = 'FLAGS_' + i + '_' + j;
                        visibileFlags = document.forms[0].VISIBILE_FLAGS.value;
			visibileFlags = visibileFlags.substring(j, j+1);
                            //if( document.forms[0].elements[name].value == 'Y' ) {
			    if( document.forms[0].elements[name].value == 'Y' && visibileFlags == 'Y') {
                                    return true;
                                }
                        }
                        for( j=0; j<10; j++ ) {
                        name = 'UDFS_' + i + '_' + j;
                        visibileUDFs = document.forms[0].VISIBILE_UDFS.value;
			visibileUDFs = visibileUDFs.substring(j, j+1);
                        value = document.forms[0].elements[name].value;
                                //if (value != null && trim(value) != '') {
                                if (value != null && trim(value) != '' && visibileUDFs == 'Y' ) {
                                    return true;
                                }
            }
                    name = 'FLAG_BRK_' + i;
                        visibileFlagBrk = document.forms[0].VISIBILE_FLAG_BRK.value;
                        //if( document.forms[0].elements[name].value == 'Y' ) {
                        if( document.forms[0].elements[name].value == 'Y' && visibileFlagBrk == 'Y' ) {
                            return true;
                        }
            name = 'FLAG_RECALL_' + i;
                        visibileFlagRecall = document.forms[0].VISIBILE_FLAG_RECALL.value;
                        //if( document.forms[0].elements[name].value == 'Y' ) {
                        if( document.forms[0].elements[name].value == 'Y' && visibileFlagRecall == 'Y' ) {
                            return true;
                        }
            }
                return false;
        }


    function copyComments( dest, src, copyBack ) {
        for( i=0; i<<%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++ ) {
            var name = 'COMMENTS_' + i;
            var val = '';
            if (src.document.forms[0].elements[name]) {
                val = src.document.forms[0].elements[name].value;
                if (dest.document.forms[0].elements[name]) {
                   dest.document.forms[0].elements[name].value = val;
                }
            }
        }

        if( copyBack ) {
            if (hasAnyComments()) {
               addOvrnMark( document.forms[0].CommentsBtn );
            } else {
               removeOvrnMark( document.forms[0].CommentsBtn );
            }
            documentChanged();
        }
    }

    function cancelForm() {
        document.forms[0].reset();
        if (document.forms[0].CommentsBtn) {
          var comments = document.forms[0].CommentsBtn.value;
          if( document.forms[0].COMMENTS_OVRN.value != '*' && comments.substring(comments.length-1) == "*" ) {
              comments = comments.substring(0, comments.length-1);
              document.forms[0].CommentsBtn.value = comments;
          }
        }
        if (document.forms[0].FlagsBtn) {
          var flags = document.forms[0].FlagsBtn.value;
          if( document.forms[0].FLAGS_OVRN.value != '*' && flags.substring(flags.length-1) == "*" ) {
              flags = flags.substring(0, flags.length-1);
              document.forms[0].FlagsBtn.value = flags;
          }
        }
    }

    function copyFlags( dest, src, copyBack ) {
        var name;
        var val;
        for( i=0; i<<%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++ ) {
                    for( j=0; j<5; j++ ) {
                        name = 'FLAGS_' + i + '_' + j;

                                if( !copyBack ) {
                  if ( dest.document.forms[0].elements[name] != null ){
                                        val = src.document.forms[0].elements[name].value;
                                        if( val == "Y" )
                                        dest.document.forms[0].elements[name].checked = 1;
                  }
                                } else {
                  if ( src.document.forms[0].elements[name] != null ){
                                        if( src.document.forms[0].elements[name].checked ) {
                                                dest.document.forms[0].elements[name].value = "Y";
                                        } else {
                                        dest.document.forms[0].elements[name].value = "";
                  }
                                }

                     }
            }
            for( j=0; j<10; j++ ) {
        name = 'UDFS_' + i + '_' + j;
        if( !copyBack ) {
                  if ( dest.document.forms[0].elements[name] != null ){
                        val = src.document.forms[0].elements[name].value;
                        dest.document.forms[0].elements[name].value = val;
          }
        }else{
          if ( src.document.forms[0].elements[name] != null ){
            val = src.document.forms[0].elements[name].value;
                        dest.document.forms[0].elements[name].value = val;
          }
        }
            }

            if( !copyBack ) {
        name = 'FLAG_BRK_' + i;
        if ( dest.document.forms[0].elements[name] != null ){

                        val = src.document.forms[0].elements[name].value;

                        if( val == "Y" )
                            dest.document.forms[0].elements[name].checked = 1;
        }
                name = 'FLAG_RECALL_' + i;
        if ( dest.document.forms[0].elements[name] != null ){
                        val = src.document.forms[0].elements[name].value;

                        if( val == "Y" )
                            dest.document.forms[0].elements[name].checked = 1;
        }
            } else {
                name = 'FLAG_BRK_' + i;
        if ( src.document.forms[0].elements[name] != null ){
                                if( src.document.forms[0].elements[name].checked ) {
                                        dest.document.forms[0].elements[name].value = "Y";
                                } else {
                                        dest.document.forms[0].elements[name].value = "";
                                }
        }
                 name = 'FLAG_RECALL_' + i;
         if ( src.document.forms[0].elements[name] != null ){
                                if( src.document.forms[0].elements[name].checked ) {
                                        dest.document.forms[0].elements[name].value = "Y";
                                } else {
                                        dest.document.forms[0].elements[name].value = "";
                                }
        }
            }
                }

                if( copyBack ) {
                    if (hasAnyFlagsOrUDFs()) {
                       addOvrnMark( document.forms[0].FlagsBtn );
                    } else {
                        removeOvrnMark( document.forms[0].FlagsBtn );
                }
                    documentChanged();
                }
    }

    function addOvrnMark( elem ) {
      if (elem) {
        var val = elem.value;
        if( val.substring(val.length-1) != "*" ) {
            val = val + "*";
            elem.value = val;
        }
      }
    }

    function removeOvrnMark( elem ) {
      if (elem) {
        var val = elem.value;
        if( val.substring(val.length-1) == "*" ) {
            val = val.substring(0,val.length-1);
            elem.value = val;
        }
      }
    }

    function openCodePopup( url ) {
        var popUpWindow;
        posX = window.screen.width / 7;
        posY = window.screen.height / 5;

        winHeight = 300;
        winWidth = 750;

        newWindowID = "popUpWindow";
        newWindowOptions = "width="+winWidth+",height="+winHeight+",scrollbars=1,alwaysRaised=1, resizable=1, left="+posX+",top="+posY;
        var tempLink = document.forms[0].elements['SELECT_DEFAULT'].options[document.forms[0].elements['SELECT_DEFAULT'].selectedIndex].value
        document.forms[0].elements['SELECT_DEFAULT'].selectedIndex=0;
        window.open(url+"&URL_STRING="+tempLink,'popUpWindow',newWindowOptions);
    }

    function namePreset( url ) {
        var popUpWindow;
        posX = window.screen.width / 7;
        posY = window.screen.height / 5;

        winHeight = 100;
        winWidth = 200;
        newWindowOptions = "width="+winWidth+",height="+winHeight+",scrollbars=1,alwaysRaised=1, resizable=1, left="+posX+",top="+posY;
		window.open(url, 'popUpWindow', newWindowOptions);
    }

  function printPopup( url ) {
        var popUpWindow;
        posX = window.screen.width / 7;
        posY = window.screen.height / 5;

        winHeight = 500;
        winWidth = 800;

        newWindowID = "popUpWindow";
        newWindowOptions = "width="+winWidth+",height="+winHeight+",scrollbars=1,alwaysRaised=1, resizable=1, left="+posX+",top="+posY;
        window.open(url,'popUpWindow',newWindowOptions);
    }

    function loadDefaultElapsedTimes( url ) {
        var tempLink = document.forms[0].elements['SELECT_DEFAULT'].value
        document.forms[0].elements['SELECT_DEFAULT'].selectedIndex=0;
        window.location = url+"&URL_STRING="+tempLink;
    }
/*
    function openCommentsPopup( dayNumber, readOnly ) {
        var url;
        if (dayNumber >= 0) {
	        url = "/timesheet/weeklyComments.jsp?dayNumber="+dayNumber+"&readOnly="+readOnly;
	    } else {
	        url = "/timesheet/weeklyComments.jsp?readOnly="+readOnly;
	    }
  	    var popUpWindow;
       	posX = window.screen.width / 7;
        posY = window.screen.height / 5;

        var winHeight;
        if (dayNumber >= 0) {
	        winHeight = 200;
	    } else {
	        winHeight = 500;
	    }
        winWidth = 550;

        newWindowID = "popUpWindow";
        newWindowOptions = "width="+winWidth+",height="+winHeight+",scrollbars=1,alwaysRaised=1, resizable=1, left="+posX+",top="+posY;
        popUpWindow = window.open(contextPath + url,'popUpWindow',newWindowOptions);
    }
*/
/*
    function openFlagsPopup() {
        var url = '/timesheet/weeklyFlags.jsp';
        var popUpWindow;
        posX = window.screen.width / 7;
        posY = window.screen.height / 5;

        winHeight = 450;
        winWidth = 600;

        newWindowID = "popUpWindow";
        newWindowOptions = "width="+winWidth+",height="+winHeight+",scrollbars=1,alwaysRaised=1, resizable=1, left="+posX+",top="+posY;
        popUpWindow = window.open(contextPath + url,'popUpWindow',newWindowOptions);
    }
*/

/*    function saveTimeSheet() {
    	if ( confirmOnSubmit() ) {
	        document.forms[0].action='<%--= request.getContextPath() + CSMWTShelper.getActionURL(CSMBiWeeklyTimeSheetsConstants.ACTION_SAVE_TIMESHEET) --%>';
    	    document.forms[0].submit();
    	}
    }
*/
/*
        function copyTimeSheet() {
                //document.forms[0].action='<%= request.getContextPath() %>/action/timesheet/CopyPreviousWeek';
                //alert(document.forms[0].action);
                document.forms[0].action='<%= request.getContextPath() %>/action/timesheet.action?action=CopyPreviousWeekAction';
        document.forms[0].submit();
    }
*/
	//-------------------------------------------------------
	// returns weekly sum of hours worked for the specified
	// elapsed time line.
	// first elapsed time line has lineNumber = 0.
	//-------------------------------------------------------
	function addElapsedTimeLine(lineNumber){
		var bookedHours = 0;
		var hourBox = 0;
		var elemName = '';

		// loop through all the days on one elapsed time line
		while (1) {
			elemName = 'HOUR_'+lineNumber+'_'+hourBox+'_time';

	 	 	if(document.forms[0].elements[elemName] == null){
				// when we pass the last line there will be no more elements.
				break;
		 	}

			if( document.forms[0].elements[elemName].value != "" ){
				bookedHours = bookedHours + parseFloat(document.forms[0].elements[elemName].value);
			}
			++hourBox;
	  	}
		return bookedHours;
	}

	//    Checks to make sure that user has entered a timecode
	//    for every line on the elapsed time grid, which have
	//    hours entered in the timeboxes.  If timecode is missing
	//    then a warning message is displayed and wts is not submitted.
	function confirmOnSubmit(){
		<%if("Y".equalsIgnoreCase(session.getAttribute("WTS_checkTCodeMissing").toString())){%>
			//check time code field and make sure it is filled out if any hours are entered
			var del;
			var i = 0;
			var tCodeMissing = false;
			var hours;

			// loop through elapsed time lines and check if timecode for any are not entered.
			while (1){
				hours = 0;
				del = "WTSDELET_" + i;

				if(document.forms[0].elements[del] == null){
					// no more lines so stop looping
					break;
				} else if( document.forms[0].elements[del].checked) {
				    // this row was deleted so don't add up the hours
					++i;
					continue;
				}
			  	hours = addElapsedTimeLine(i);

				if(hours > 0 && document.forms[0].elements['FIELDS0X_' + i].value == ""){
					// this elapsed time line has booked hours so check if tCode is missing?
					<wb:define id="tCodeMissingMsg1"><wb:localize id="JAVASCRIPT_Please_fill_in_an_appropriate_Timecode_for_the_Elapsed_Time_line_" ignoreConfig="true">Please fill in an appropriate Timecode for the Elapsed Time line </wb:localize></wb:define>
					<wb:define id="tCodeMissingMsg2"><wb:localize id="JAVASCRIPT_,_then_press_submit." ignoreConfig="true">, then press submit.</wb:localize></wb:define>
					var tCodeMissingMsg = "<wb:get id="tCodeMissingMsg1"/>"+(i+1)+"<wb:get id="tCodeMissingMsg2"/>";
					alert(tCodeMissingMsg);
					bHasFormBeenSubmitted=false;
					return false;
				}
				++i;
			}
		<%}%>
		return true;
	}

	/*
    function distributeTime() {
        document.forms[0].action='<%= request.getContextPath()%>/action/timesheet.action?action=DistributeTimeAction';
        document.forms[0].submit();
    }

    function RetrieveTimeSheet() {
        document.forms[0].action='<%= request.getContextPath() %>/action/timesheet.action?action=RetrieveTimeSheetAction';
        document.forms[0].submit();
    }

    function resetForm() {
        document.forms[0].action='<%= request.getContextPath() %>/action/timesheet.action?action=ResetTimesheetAction';
        document.forms[0].submit();
    }


    function backToSupervisorSummary() {
        location.href = '<%= request.getContextPath() %>/action/timesheet.action?action=LoadSupervisorSummaryAction&FROM_DEFAULT_TIMESHEET=T';
        return;
    }

    function backToPaySummary() {
        location.href = '<%= request.getContextPath() %>/action/timesheet.action?action=PaySummaryLoadAction&FROM_DEFAULT_TIMESHEET=T';
        return;
    }


    function backToSelectionScreen() {
    	location.href = '<%= request.getContextPath() %>/action/timesheet.action?action=SelectEmployeeAction';
    	return;
    }*/

    function authorizeAll(isChecked) {
    	var form = document.forms[0];
    	var permElem = "CANNOT_AUTH_WRKS_";
    	var val = "Y";
    	if (!isChecked) {
    		permElem = "CANNOT_UNAUTH_WRKS_";
    		val = "N";
    	}

	    for (i=0; i < <%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++) {
	   	  if(document.forms[0].elements["LOCKED_" + i]) {
	         if (document.forms[0].elements["LOCKED_" + i].checked) {
				alert ("<wb:localize id="YOU_CANNOT_AUTHORIZE_ENTIRE_WEEK" ignoreConfig="true" escapeForJavascript="true">Authorization or un-authorization for the entire week cannot be performed when one or more days of the week have been locked.</wb:localize>");
	         	break;
	         }
	      }
	    }


   		// If the user does not have permission to make the change
   		// then undo the change and fail.
		if (form.elements["USING_WORK_DETAIL_APPROVAL"]) {
			for (i = 0; i < <%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++) {
				if (form.elements[permElem + i]) {
					if (form.elements["AUTHORIZED"]) {
						form.elements["AUTHORIZED"].checked = !isChecked;
					}
					return false;
				}
			}
		}

		// Update daily authorization checkboxes.
		for (i = 0; i < <%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++) {
			if(document.forms[0].elements["LOCKED_" + i]) {
	          if(!document.forms[0].elements["LOCKED_" + i].checked) {
					if (form.elements["AUTHORIZED_" + i]) {
						form.elements["AUTHORIZED_" + i].checked = isChecked;
						form.elements["AUTHORIZED_" + i].value = val;
					}
				}
			}
		}

	   if (isChecked) {
       		document.forms[0].elements["AUTHORIZED"].value = 'Y';
       } else {
       		document.forms[0].elements["AUTHORIZED"].value = 'N';
       }
	  return true;
	}

    function updateAllAuthorized(checkbox, day) {
		var form = document.forms[0];
		var permElem = "CANNOT_AUTH_WRKS_";
		var val = "Y";
		if (!checkbox.checked) {
    		permElem = "CANNOT_UNAUTH_WRKS_";
			val = "N";
		}

   		// If the user does not have permission to make the change
   		// then undo the change and fail.
    	if (form.elements["USING_WORK_DETAIL_APPROVAL"]) {
			if (form.elements[permElem + day]) {
				checkbox.checked = !checkbox.checked;
				return false;
			}
		}

		// Update this checkbox.
		checkbox.value = val;

		// Update 'Authorize All' checkbox.
		if (form.elements["AUTHORIZED"]) {
			var allAuthorized = true;
			for (i = 0; i < <%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++) {
				if (form.elements["AUTHORIZED_" + i]) {
					if (!form.elements["AUTHORIZED_" + i].checked) {
						allAuthorized = false;
					}
				}
			}
			form.elements["AUTHORIZED"].checked = allAuthorized;
		}
		return true;
	}

    function lockAll(isChecked) {
       for (i=0; i < <%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++) {
          if(document.forms[0].elements["LOCKED_" + i]) {
             if (isChecked) {
                document.forms[0].elements["LOCKED_" + i].checked = true;
                document.forms[0].elements["LOCKED_" + i].value = 'Y';
             } else {
                document.forms[0].elements["LOCKED_" + i].checked = false;
                document.forms[0].elements["LOCKED_" + i].value = 'N';
             }
          }
       }
    }

    function updateAllLocked(checkbox) {
       if (checkbox.checked) {
          checkbox.value='Y';
       } else {
          checkbox.value='N';
       }
       var allLocked = true;
       for (i=0; i < <%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>; i++) {
          if(document.forms[0].elements["LOCKED_" + i]) {
             if (!document.forms[0].elements["LOCKED_" + i].checked) {
                allLocked = false;
             }
          }
       }
       document.forms[0].elements["LOCKED"].checked = allLocked;
    }

    // Check if break times and non work times overlap each other
    function checkOverlappingTime()
    {
    /*
    	if ((document.forms[0].elements["totalLineCount"] != null) && (document.forms[0].elements["BRK_START_TIME_0"] != null)){
    		if (document.forms[0].elements["totalLineCount"].value != 0){
				for( var b=0;b<<%=CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET%>;b++){
					if (document.forms[0].elements["BRK_START_TIME_"+b] != null && document.forms[0].elements['NO_MEAL_BREAK_'+b] != null){
						if (!document.forms[0].elements['NO_MEAL_BREAK_'+b].checked){
					    	var brkStartTime = extractTime(document.forms[0].elements["BRK_START_TIME_"+b].value);
					    	var brkEndTime = extractTime(document.forms[0].elements["BRK_END_TIME_"+b].value);
							for( var n=0;n<=document.forms[0].elements["totalLineCount"].value;n++)
				    		{
						    	if (document.forms[0].elements["WTSNWTSTART"+b+"X_"+n] != null){
							    	var nonWrkStartTime = extractTime(document.forms[0].elements["WTSNWTSTART"+b+"X_"+n].value);
							    	var nonWrkEndTime = extractTime(document.forms[0].elements["WTSNWTSTOP"+b+"X_"+n].value);
							    	//check if break values overlap non-work time
							    	if (((compareWBTimes(brkStartTime,nonWrkStartTime)) == 1 && (compareWBTimes(brkStartTime,nonWrkEndTime)) == -1)
							    		|| ((compareWBTimes(brkEndTime,nonWrkStartTime)) == 1 && (compareWBTimes(brkEndTime,nonWrkEndTime)) == -1)
							    		|| ((compareWBTimes(brkStartTime,nonWrkStartTime)) == 0 && (compareWBTimes(brkEndTime,nonWrkEndTime)) == 0))
							    	{
							    		alert ("<wb:localize id="YOU_CANNOT_ENTER_OVERLAPPING_BREAK_NONWORK" ignoreConfig="true" escapeForJavascript="true">You cannot enter overlapping break and non-work times.</wb:localize>");
							    		bHasFormBeenSubmitted=false;
							    		return false;
							    	}
							    	//check if non-work values overlap break time
							    	if (((compareWBTimes(nonWrkStartTime,brkStartTime)) == 1 && (compareWBTimes(nonWrkStartTime,brkEndTime)) == -1)
							    		|| ((compareWBTimes(nonWrkEndTime,brkStartTime)) == 1 && (compareWBTimes(nonWrkEndTime,brkEndTime)) == -1))
							    	{
							    		alert ("<wb:localize id="YOU_CANNOT_ENTER_OVERLAPPING_BREAK_NONWORK" ignoreConfig="true" escapeForJavascript="true">You cannot enter overlapping break and non-work times.</wb:localize>");
							    		bHasFormBeenSubmitted=false;
							    		return false;
							    	}
							    }
						    }
						}
				 	}
			    }
	    	}
	    }
    */
    	return true;
    }

</script> </wb:page>