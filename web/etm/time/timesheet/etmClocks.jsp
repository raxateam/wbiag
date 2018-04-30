<%@ include file="/system/wbheader.jsp"%> <%@ page import="java.util.*" %> <%@ page import="java.text.*" %> <%@ page import="com.workbrain.app.ta.db.*" %> <%@ page import="com.workbrain.app.ta.model.*" %> <%@ page import="com.workbrain.util.*" %> <%@ page import="com.workbrain.sql.*" %> <%@ page import="com.workbrain.server.jsp.*" %> <%@ page import="com.workbrain.server.registry.*" %> <%@ page import="com.workbrain.server.data.type.DatetimeType" %> <%--
	_____________________________________________________________________
	This page registers clock transactions in the CLOCK_TRAN_PENDING table.
	Allows for CLOCK ON, CLOCK OFF, CHANGE JOB transactions.

	REQUIRES:
		tran	- type of transaction.
					Possible values = 	1 - Clock ON,
										2 - Clock OFF,
										3 - Change Job
										-1 = No action. Just display interface
		jobName - if changing jobs, a valid JOBNAME is required.

	NOTE:
		The reader name (VIRTUAL READER) is currently hard coded. It should be
		stored as a cookie on the kiosk. This cookie would be set during the
		setup of the kiosk. This value should then be a 'pageProperty'.

	creation date: 	March 25, 2001
	author:			Hammed Malik, Baris Ceyhan
	_____________________________________________________________________
--%> <wb:page type="VR" domain="VR" maintenanceFormId="606">
<%!
  // *** checks comma-deimited IPRanges and see if client IP starts with any of them
   boolean isIPEligible(String ipRange, String clientIP) {
     if (StringHelper.isEmpty(ipRange)) {
        return true;
     }
     String[] ranges = StringHelper.detokenizeString(ipRange , ",") ;
     for (int i = 0; i < ranges.length; i++) {
         if (!StringHelper.isEmpty(ranges[i])
             && clientIP.startsWith(ranges[i])) {
           return true;
         }
     }
     return false;
   }
%>
<%
    final String etmClocksPage = "/etm/time/timesheet/etmClocks.jsp";
    String clientIP = request.getRemoteAddr();
    String ipRange = Registry.getVarString("system/WORKBRAIN_PARAMETERS/ETM_CLOCK_IP_RANGE", null);
    boolean isIPEligible = isIPEligible(ipRange , clientIP);

    // *** if not defined or eligible, forward to core page
    if (!StringHelper.isEmpty(ipRange) && !isIPEligible) {
%>
     <jsp:include page="/etm/etmMenu.jsp" flush="true">
	  <jsp:param name="selectedTocID" value="41"/>
	  <jsp:param name="parentID" value="40"/>
     </jsp:include>
     <table cellpadding=0 cellspacing=0>
      <tr><td class="MSGlookupFullname"> You are not authorized to view clocks page from this IP Address : <%=clientIP%>
      </td></tr>
     </table>
     <wb:stop/>
<%
    }
%>
<wb:define id="badgeNumber"><wb:evaluate>page.property.employeeBadge</wb:evaluate></wb:define> <wb:define id="empID"><wb:getPageProperty id="employeeId"/></wb:define> <wb:define id="transactionType"><wb:get id="tran" scope="parameter" default="-1"/></wb:define> <wb:define id="job_NAME"><wb:get id="job_NAME" scope="parameter" default=""/></wb:define> <wb:define id="job_ID"><wb:get id="job_ID" scope="parameter" default=""/></wb:define> <wb:define id="dock_NAME"><wb:get id="dock_NAME" scope="parameter" default=""/></wb:define> <wb:define id="dock_ID"><wb:get id="dock_ID" scope="parameter" default=""/></wb:define> <wb:define id="tcode_NAME"><wb:get id="tcode_NAME" scope="parameter" default=""/></wb:define> <wb:define id="tcode_ID"><wb:get id="tcode_ID" scope="parameter" default=""/></wb:define> <wb:define id="dept_NAME"><wb:get id="dept_NAME" scope="parameter" default=""/></wb:define> <wb:define id="dept_ID"><wb:get id="dept_ID" scope="parameter" default=""/></wb:define> <wb:define id="RETAIL_SALE"><wb:get id="RETAIL_SALE" scope="parameter" default=""/></wb:define> <wb:define id="RETAIL_CASHTIP"><wb:get id="RETAIL_CASHTIP" scope="parameter" default=""/></wb:define> <wb:define id="RETAIL_CREDITTIP"><wb:get id="RETAIL_CREDITTIP" scope="parameter" default=""/></wb:define> <wb:define id="RETAIL_OTHERTIP"><wb:get id="RETAIL_OTHERTIP" scope="parameter" default=""/></wb:define> <wb:define id="RETAIL_TIPOUT"><wb:get id="RETAIL_TIPOUT" scope="parameter" default=""/></wb:define> <wb:define id="tmpLocaleId,clockData,clockMsg"/> <wb:define id="readerId">55</wb:define> <wb:define id="readerName">VIRTUAL READER</wb:define> <%  LocalizationHelper localHelper = new LocalizationHelper(pageContext);
    Date timeOfClockActionWithTZAdjustment = null;
    String schCompErrorMsg = "";
    boolean getTimeZoneFromEmpUDF = false;
    boolean getTimeZoneFromClientBrowser = false;
    String tzParam = null;
    String tzOffset = null;
    TimeZone pageTz = TimeZone.getDefault();
    DBConnection conn = JSPHelper.getConnection(request);


    try {
        tzParam = (String)Registry.getVar("system/WORKBRAIN_PARAMETERS/ETM_CLOCK_FACTOR_TIME_ZONE");
    } catch(Exception e) {
        throw new NestedRuntimeException("Failure reading registry item system/WORKBRAIN_PARAMETERS/ETM_CLOCK_FACTOR_TIME_ZONE.", e);
    }

    if ("EMPLOYEE_UDF".equalsIgnoreCase(tzParam)) {
        getTimeZoneFromEmpUDF = true;
    } else if ("CLIENT_BROWSER".equalsIgnoreCase(tzParam)) {
        getTimeZoneFromClientBrowser = true;
	   	tzOffset = request.getParameter("tzOffset");
        if (tzOffset == null) { %> <script type='text/javascript'>
        document.forms[0].action += '?tzOffset=' + -1*(new Date().getTimezoneOffset())*<%=DateHelper.MINUTE_MILLISECODS%>;
		document.forms[0].submit();
</script> <%
        }
    }
    Date clkDateShifted = null;
    //if (!"-1".equals(transactionType.toString())) {

        // This Date object is good for carrying epoch time in miliseconds, but not good for direct
        // use to format string.  For example, not good for DatetimeType.FORMAT.format((java.util.Date)timeOfClockActionWithTZAdjustment).
        // It needs TimeZoneUtil.dateFormatTZ() to show the proper time with time zone
        // see below use of dateFormatTZ()
        timeOfClockActionWithTZAdjustment = new Date();

        if (getTimeZoneFromClientBrowser && tzOffset != null) {
            //assert: this is not the first time we're visiting the page.  (tzOffset is defined if ETM_CLOCK_FACTOR_TIME_ZONE is CLIENT_BROWSER).
            timeOfClockActionWithTZAdjustment.setTime(TimeZoneUtil.getTimeZoneOffsetted(Integer.parseInt(tzOffset)).getTime());
        } else {
            if (getTimeZoneFromEmpUDF) {

                // Not using TimeZoneUtil.convertTime() to convert time, because it returns a
                // java.util.Date object which cannot display time during DST grey area for regions that
                // don't observer DST.
                // For example, the appserver is in Toronto that observes DST on April 3rd, 2005,
                // while the employee is in Arizona, and does not observe DST.  If the employee from Arizona
                // needs to see 2:30am April 3rd, 2005, but the date object cannot show this time,
                // because the date object is localized in the Toronto appserver that moves the
                // clock ahead after 1:59am on April 3rd, 2005.  In other words, in Toronto,
                // the time between 2am and 2:59am don't exist on April 3rd, 2005.
                java.util.TimeZone tz = com.workbrain.util.TimeZoneUtil.getEmployeeTimeZone(conn, Integer.parseInt(empID.toString()));
                if(tz != null){
                    // the time conversion is actually used in setClocksOn() method.
                    // the date object is OK here, because it carries eproch time in miliseconds
                    pageTz = tz;
                }

                // following code was to yield a result for clkDateShifted variable
                boolean useServerTime = false;
                try {
                    clkDateShifted = TimeZoneUtil.convertTime(JSPHelper.getConnection(request),
                            Integer.parseInt(empID.toString()),
                            timeOfClockActionWithTZAdjustment,
                            TimeZoneUtil.SERVER_TO_CLIENT);
                } catch (TimeZoneUtil.NoEmployeeTimeZoneDefinedException e) {
                    useServerTime = true;
                }
                if (!useServerTime) {
                    clkDateShifted = timeOfClockActionWithTZAdjustment;  // use server time
                }
            }

            if ("1".equals(transactionType.toString())) {
                //system/WORKBRAIN_PARAMETERS/ETM_CLOCK_FACTOR_TIME_ZONE must not be CLIENT_BROWSER for schedule compliance to run.
                //check for schedule compliance.
                //schedule compliance acounts for time zone differences on its own.
                schCompErrorMsg = com.workbrain.app.jsp.util.Clocks.validateScheduleConstraints(Integer.parseInt(empID.toString()), JSPHelper.getConnection(request));
            }
        }

        // *** figuring out if this employee is tipped or not ***
        int employeeId = Integer.parseInt(empID.toString());
        boolean tippedEmployee = false;
        String tipCalculation = "";
        String inferredPercentage = "";
        EmployeeJobAccess eja = new EmployeeJobAccess(conn);
        List empJobs = eja.loadByEmpId(employeeId);
        CodeMapper mapper = CodeMapper.createCodeMapper(conn);
        for (int i=0; i<empJobs.size(); i++) {
            EmployeeJobData ejd = (EmployeeJobData) empJobs.get(i);
            JobData jd = mapper.getJobById(ejd.getJobId());
            if ("Y".equalsIgnoreCase(jd.getJobTip())) {
                tippedEmployee = true;
                tipCalculation = jd.getJobTipCalculatn();
                inferredPercentage = jd.getJobInferredTip();
                break;
            }
        }
        //Insert clocks.
        if(transactionType.toString().equals("1")) {
            if (session.getAttribute("CLOCKED_ON") == null || session.getAttribute("CLOCKED_ON").toString().equals("F")) {
                if ("".equals(schCompErrorMsg)) {
                    session.setAttribute("CLOCKED_ON","T");
                    session.putValue("CLOCKED_OFF","F");
                    boolean DST = false;
		    String clockDataString = "";
		    if("EMPLOYEE_UDF".equalsIgnoreCase(tzParam)) {
		    	java.util.TimeZone tz = com.workbrain.util.TimeZoneUtil.getEmployeeTimeZone(conn, Integer.parseInt(empID.toString()));
		    	if(tz != null){
		    	pageTz = tz;  // update this tz for setClocksON()
				DST = tz.inDaylightTime(clkDateShifted);
				clockDataString = "CTPJ_EXTRADATA="+"DST="+(DST==true ? "T" : "F");
			}

		    }
                    // insert an on clock.
                    com.workbrain.app.jsp.util.Clocks.setClocksON(
                            conn,
                            1,
                            empID.toInt(),
                            readerName.toString(),
                            clockDataString,
                            timeOfClockActionWithTZAdjustment,
                            pageTz); %> <wb:set id="clockMsg"><wb:localize ignoreConfig="true" id="You_are_clocked_on">You are now clocked On</wb:localize> <wb:controlField id='ETM_CLOCK_DATE_FORMAT' overrideId='606' mode='view'><%=TimeZoneUtil.dateFormatTZ(timeOfClockActionWithTZAdjustment,pageTz)%></wb:controlField></wb:set> <%              } else { %> <wb:set id="clockMsg"><wb:localize ignoreConfig="true" id="Failed_to_clock_in">Failed to clock on at</wb:localize> <wb:controlField id='ETM_CLOCK_DATE_FORMAT' overrideId='606' mode='view'><%=TimeZoneUtil.dateFormatTZ(timeOfClockActionWithTZAdjustment,pageTz)%></wb:controlField> - <%=schCompErrorMsg%></wb:set> <%                  out.println("<script type='text/javascript'>alert('" + localHelper.getText("Failed_to_clock_in_abbrev", "", "Failed to clock on.") + "  " + schCompErrorMsg + ".');</script>");
                }
            } else { %> <wb:set id="clockMsg"><wb:localize ignoreConfig="true" id="You_are_already_clocked_on">You are already clocked On</wb:localize></wb:set> <%          }
		// display the server time on page access
    	} else if( transactionType.toString().equals("-1") ) { %> <wb:set id="clockMsg"><wb:localize ignoreConfig="true" id="Server_time_is">Server time is: </wb:localize> <wb:controlField id='ETM_CLOCK_DATE_FORMAT' overrideId='606' mode='view'><%=TimeZoneUtil.dateFormatTZ(timeOfClockActionWithTZAdjustment,pageTz)%></wb:controlField></wb:set> <%    	} else {
			boolean processClock = false;
            if (transactionType.toInt() == 2) {
	            if (session.getValue("CLOCKED_OFF") == null || session.getValue("CLOCKED_OFF").toString().equals("F")) {
	                session.setAttribute("CLOCKED_ON","F");
	                session.putValue("CLOCKED_OFF","T");
	                processClock = true;
	                StringBuffer extraData = new StringBuffer();
	                if (!StringHelper.isEmpty(RETAIL_SALE.toString())) {
	                    extraData.append(Clock.CLOCKDATA_SALE).append("=").append(RETAIL_SALE.toString());
	                }
	                if (!StringHelper.isEmpty(RETAIL_CASHTIP.toString())) {
	                    if (extraData.length() > 0) {
	                        extraData.append("&");
	                    }
	                    extraData.append(Clock.CLOCKDATA_CASHTIP).append("=").append(RETAIL_CASHTIP.toString());
	                }
	                if (!StringHelper.isEmpty(RETAIL_CREDITTIP.toString())) {
	                    if (extraData.length() > 0) {
	                        extraData.append("&");
	                    }
	                    extraData.append(Clock.CLOCKDATA_CREDITTIP).append("=").append(RETAIL_CREDITTIP.toString());
	                }

	                if (!StringHelper.isEmpty(RETAIL_OTHERTIP.toString())) {
	                    if (extraData.length() > 0) {
	                        extraData.append("&");
	                    }
	                    extraData.append(Clock.CLOCKDATA_OTHERTIP).append("=").append(RETAIL_OTHERTIP.toString());
	                }

	                if (!StringHelper.isEmpty(RETAIL_TIPOUT.toString())) {
	                    if (extraData.length() > 0) {
	                        extraData.append("&");
	                    }
	                    extraData.append(Clock.CLOCKDATA_TIPOUT).append("=").append(RETAIL_TIPOUT.toString());
	                }
	                if (extraData.length()>0) {
	                %> <wb:set id="clockData">CTPJ_EXTRADATA=<%=extraData.toString()%></wb:set> <wb:set id="clockMsg"><wb:localize ignoreConfig="true" id="You_are_clocked_off_and_tip_recorded">You are now clocked Off and your tip information has been recorded</wb:localize> <wb:controlField id='ETM_CLOCK_DATE_FORMAT' overrideId='606' mode='view'><%=TimeZoneUtil.dateFormatTZ(timeOfClockActionWithTZAdjustment,pageTz)%></wb:controlField></wb:set> <%
	                } else {
	                %> <wb:set id="clockData"/>
	                    <wb:set id="clockMsg"><wb:localize ignoreConfig="true" id="You_are_clocked_off">You are now clocked Off</wb:localize> <wb:controlField id='ETM_CLOCK_DATE_FORMAT' overrideId='606' mode='view'><%=TimeZoneUtil.dateFormatTZ(timeOfClockActionWithTZAdjustment,pageTz)%></wb:controlField></wb:set> <%
	                }
	             } else {
		             processClock = false;
	              %> <wb:set id="clockMsg"><wb:localize ignoreConfig="true" id="You_are_already_clocked_off">You are already clocked Off</wb:localize></wb:set> <% }
	        } else {
	        	processClock = true;
	         %> <wb:switch> <wb:case expression="#job_NAME#" compareToExpression="" operator="<>"> <wb:set id="clockData">JOB=<wb:get id="job_NAME"/></wb:set> <wb:set id="clockMsg"><wb:localize ignoreConfig="true" id="JOB_CHANGE_RECORDED">Your job has been changed to</wb:localize> <wb:get id="job_NAME"/></wb:set> </wb:case> <wb:case expression="#dock_NAME#" compareToExpression="" operator="<>"> <wb:set id="clockData">DKT=<wb:get id="dock_NAME"/></wb:set> <wb:set id="clockMsg"><wb:localize ignoreConfig="true" id="DOCKET_CHANGE_RECORDED">Your docket has been changed to</wb:localize> <wb:get id="dock_NAME"/></wb:set> </wb:case> <wb:case expression="#tcode_NAME#" compareToExpression="" operator="<>"> <wb:set id="clockData">TCODE=<wb:get id="tcode_NAME"/></wb:set> <wb:set id="clockMsg"><wb:localize ignoreConfig="true" id="TIMECODE_CHANGE_RECORDED">Your time code has been changed to</wb:localize> <wb:get id="tcode_NAME"/></wb:set> </wb:case> <wb:case expression="#dept_NAME#" compareToExpression="" operator="<>"> <wb:set id="clockData">DPT=<wb:get id="dept_NAME"/></wb:set> <wb:set id="clockMsg"><wb:localize ignoreConfig="true" id="DEPARTMENT_CHANGE_RECORDED">Your department has been changed to</wb:localize> <wb:get id="dept_NAME"/></wb:set> </wb:case> <wb:case> <wb:set id="clockData"/>
            		</wb:case>
            	</wb:switch>
<%          }

            if (processClock) {
	            boolean DST = false;
	            String clockDataString = clockData.toString();
			    if("EMPLOYEE_UDF".equalsIgnoreCase(tzParam)) {
			    	java.util.TimeZone tz = com.workbrain.util.TimeZoneUtil.getEmployeeTimeZone(conn, Integer.parseInt(empID.toString()));
			    	if(tz != null) {
		            pageTz = tz;
					DST = tz.inDaylightTime(clkDateShifted);
					clockDataString = clockDataString+(clockDataString.length()>0?"&":"")+"CTPJ_EXTRADATA=DST="+(DST==true ? "T" : "F");
				}

			    }
	            // insert a clock.
	            com.workbrain.app.jsp.util.Clocks.setClocks(conn,
	                    transactionType.toInt(),
	                    empID.toInt(),
	                    readerName.toString(),
	                    clockDataString,
	                    timeOfClockActionWithTZAdjustment,
	                    pageTz
	                    );
	        }
        } %>
<%--  } else { %>
        <wb:set id="clockMsg"/>
<%  } %>
--%>

<jsp:include page="/etm/etmMenu.jsp" flush="true">
	<jsp:param name="selectedTocID" value="41"/>
	<jsp:param name="parentID" value="40"/>
	<jsp:param name="msg" value="<%=clockMsg%>"/>
</jsp:include>


<wb:config id="Server_time_is"/>
<wb:config id="Are_you_sure_you_want_to_clock_on?"/>
<wb:config id="Are_you_sure_you_want_to_clock_off?"/>
<wb:config id="You_are_clocked_on"/>
<wb:config id="You_are_already_clocked_on"/>
<wb:config id="You_are_already_clocked_off"/>
<wb:config id="You_are_clocked_off"/>
<wb:config id="Please_try_again"/>
<wb:config id="Failed_to_clock_in"/>
<wb:config id="Are_you_sure_you_want_to_change_job?"/>
<wb:config id="Are_you_sure_you_want_to_change_docket?"/>
<wb:config id="Are_you_sure_you_want_to_change_time_code?"/>
<wb:config id="Are_you_sure_you_want_to_change_department?"/>
<wb:config id="JOB_CHANGE_RECORDED"/>
<wb:config id="DOCKET_CHANGE_RECORDED"/>
<wb:config id="TIMECODE_CHANGE_RECORDED"/>
<wb:config id="DEPARTMENT_CHANGE_RECORDED"/>
<%  if (JSPHelper.getWebSession(request).isConfigOn()) {
        out.println("<br>ETM Clock date format");
    } %>
<wb:config id="ETM_CLOCK_DATE_FORMAT" type="field" overrideId="606"/>



<table width='100%' cellpadding=10>
   <tr>
	   <td align='center' width='50%'><wb:secureContent securityName='mfrm_606_Clock_On'>
	   	<wb:config id="Clock_ON"/>
	   	<wb:set id="tmpLocaleId"><wb:localize ignoreConfig="true" id="Clock_ON">Clock ON</wb:localize></wb:set> <wb:vrbutton label="#tmpLocaleId#" imageBased="true" selected="false" onClick = "chkConfirm(1);"/> </wb:secureContent></td> <td align='center' width='50%'><wb:secureContent securityName='mfrm_606_Clock_Off'> <wb:config id="Clock_OFF"/> <wb:set id="tmpLocaleId"><wb:localize ignoreConfig="true" id="Clock_OFF">Clock OFF</wb:localize></wb:set> <wb:vrbutton label="#tmpLocaleId#" imageBased="true" selected="false" onClick = "chkConfirm(2);"/> </wb:secureContent></td> </tr> <tr> <td colspan=2 align=center><wb:secureContent securityName='mfrm_606_Change_Job'> <wb:config id="Change_Job"/> <wb:set id="tmpLocaleId"><wb:localize ignoreConfig="true" id="Change_Job">Change Job</wb:localize></wb:set> <input type='hidden' name='job_ID' value=''> <input type='hidden' name='job_NAME' value=''> <wb:vrbutton label="#tmpLocaleId#" imageBased="true" selected="false" onClick="if(changeJob()){pickJob();}"/> </wb:secureContent></td> </tr> <tr> <td colspan=2 align=center><wb:secureContent securityName='mfrm_606_Change_Docket'> <wb:config id="Change_Docket"/> <input type='hidden' name='dock_ID' value=''> <input type='hidden' name='dock_NAME' value=''> <wb:set id="tmpLocaleId"><wb:localize ignoreConfig="true" id="Change_Docket">Change Docket</wb:localize></wb:set> <wb:vrbutton label="#tmpLocaleId#" imageBased="true" selected="false" onClick="if(changeDocket()){pickDocket();}"/> </wb:secureContent></td> </tr> <tr> <td colspan=2 align=center><wb:secureContent securityName='mfrm_606_Change_TimeCode'> <wb:config id="Change_TimeCode"/> <input type='hidden' name='tcode_ID' value=''> <input type='hidden' name='tcode_NAME' value=''> <wb:set id="tmpLocaleId"><wb:localize ignoreConfig="true" id="Change_TimeCode">Change TimeCode</wb:localize></wb:set> <wb:vrbutton label="#tmpLocaleId#" imageBased="true" selected="false" onClick="if(changeTimeCode()){pickTimeCode();}"/> </wb:secureContent></td> </tr> <tr> <td colspan=2 align=center><wb:secureContent securityName='mfrm_606_Change_Department'> <wb:config id="Change_Department"/> <input type='hidden' name='dept_ID' value=''> <input type='hidden' name='dept_NAME' value=''> <wb:set id="tmpLocaleId"><wb:localize ignoreConfig="true" id="Change_Department">Change Department</wb:localize></wb:set> <wb:vrbutton label="#tmpLocaleId#" imageBased="true" selected="false" onClick="if(changeDepartment()){pickDepartment();}"/> </wb:secureContent></td> </tr> </table> <input type='hidden' name='TARGET_FIELD' value=''> <input type='hidden' name='RETAIL_SALE' value=''> <input type='hidden' name='RETAIL_CASHTIP' value=''> <input type='hidden' name='RETAIL_CREDITTIP' value=''> <input type='hidden' name='RETAIL_OTHERTIP' value=''> <input type='hidden' name='RETAIL_TIPOUT' value=''> <input type='hidden' name='CLOCKED_ON' value='<%=session.getAttribute("CLOCKED_ON")%>'> <input type='hidden' name='CLOCKED_OFF' value='<%=session.getValue("CLOCKED_OFF")%>'> <%  String TzOffsetPart = "";
    if (getTimeZoneFromClientBrowser) {
        TzOffsetPart = "&tzOffset=\" + -1*(new Date().getTimezoneOffset()*" + DateHelper.MINUTE_MILLISECODS + ") + \"";
    } %> <%-- Javascript password validation functions --%> <script type='text/javascript'>
	function chkConfirm(typ)
	{
		if (document.forms[0].elements.job_NAME)
         document.forms[0].elements.job_NAME.value = "";
      if (document.forms[0].elements.job_ID)
		   document.forms[0].elements.job_ID.value = "";
      if (document.forms[0].elements.dock_ID)
         document.forms[0].elements.dock_ID.value = "";
      if (document.forms[0].elements.dock_NAME)
         document.forms[0].elements.dock_NAME.value = "";
      if (document.forms[0].elements.tcode_NAME)
		   document.forms[0].elements.tcode_NAME.value = "";
		if (document.forms[0].elements.tcode_ID)
		   document.forms[0].elements.tcode_ID.value = "";
      if (document.forms[0].elements.dept_NAME)
		   document.forms[0].elements.dept_NAME.value = "";
		if (document.forms[0].elements.dept_ID)
		   document.forms[0].elements.dept_ID.value = "";

		var rightnow = new Date();
		var hours = rightnow.getHours();
		var minutes = rightnow.getMinutes();
		var ampm = (hours >= 12) ? "PM" : "AM";
		if (hours > 12) hours -= 12;
		if (hours == 0) hours = 12;
		if (minutes < 10) minutes = "0" + minutes;

        var clockDate = new Date();
		if (typ == 1) {
            if (confirm("<wb:localize ignoreConfig="true" id="Are_you_sure_you_want_to_clock_on?">Are you sure you want to clock on?</wb:localize>")) {
                window.location = "<%= request.getContextPath() %>/etm/time/timesheet/etmClocks.jsp?tran=1<%=TzOffsetPart%>";
            }
		}
		else if (typ == 2) {
			if (confirm("<wb:localize ignoreConfig="true" id="Are_you_sure_you_want_to_clock_off?">Are you sure you want to clock off?</wb:localize>")) {
                <%if (tippedEmployee) {%>
                    setFormAction("/etm/time/timesheet/etmClocks.jsp?tran=2<%=TzOffsetPart%>");
                    openWindow("/etm/time/timesheet/etmEnterTip.jsp?RetailSaleField=RETAIL_SALE&CashTipField=RETAIL_CASHTIP&CreditTipField=RETAIL_CREDITTIP&OtherTipField=RETAIL_OTHERTIP&TipOutField=RETAIL_TIPOUT&TipCalculation=<%=tipCalculation%>&InferredPercentage=<%=inferredPercentage%>&AsOfDate=<%=TimeZoneUtil.dateFormatTZ(timeOfClockActionWithTZAdjustment,pageTz)%>");
                <%} else {%>
                    window.location = "<%= request.getContextPath() %>/etm/time/timesheet/etmClocks.jsp?tran=2<%=TzOffsetPart%>";
                <%}%>
			}
		}
	}

	function changeJob() {

		var jobID = document.forms[0].elements.job_ID.value;
		var jobName = document.forms[0].elements.job_NAME.value;
		if (confirm("<wb:localize ignoreConfig="true" id="Are_you_sure_you_want_to_change_job?">Are you sure you want to change job?</wb:localize>")) {
			return true;
		}else{
			return false;
		}
	}

	function changeDocket() {
		var dockID = document.forms[0].elements.dock_ID.value;
		var dockName = document.forms[0].elements.dock_NAME.value;
		if (confirm("<wb:localize ignoreConfig="true" id="Are_you_sure_you_want_to_change_docket?">Are you sure you want to change docket?</wb:localize>")) {
			return true;
		}else{
			return false;
		}
	}

	function changeTimeCode() {
		var tcodeID = document.forms[0].elements.tcode_ID.value;
		var tcodeName = document.forms[0].elements.tcode_NAME.value;
		if (confirm("<wb:localize ignoreConfig="true" id="Are_you_sure_you_want_to_change_time_code?">Are you sure you want to change time code?</wb:localize>")) {
			return true;
		}else{
			return false;
		}
	}

	function changeDepartment() {
		var deptID = document.forms[0].elements.dept_ID.value;
		var deptName = document.forms[0].elements.dept_NAME.value;
		if (confirm("<wb:localize ignoreConfig="true" id="Are_you_sure_you_want_to_change_department?">Are you sure you want to change department?</wb:localize>")) {
			return true;
		}else{
			return false;
		}
	}

	function pickJob() {
    if (document.forms[0].elements.dock_ID)
        document.forms[0].elements.dock_ID.value = ""	;
    if (document.forms[0].elements.dock_NAME)
        document.forms[0].elements.dock_NAME.value = "";
    if (document.forms[0].elements.tcode_ID)
        document.forms[0].elements.tcode_ID.value = "";
    if (document.forms[0].elements.tcode_NAME)
        document.forms[0].elements.tcode_NAME.value = "";
    if (document.forms[0].elements.dept_ID)
    		document.forms[0].elements.dept_ID.value = ""	;
    if (document.forms[0].elements.dept_NAME)
    	  document.forms[0].elements.dept_NAME.value = "";
		var jobID = document.forms[0].elements.job_ID.value;
		var jobName = document.forms[0].elements.job_NAME.value;
        setFormAction("/etm/time/timesheet/etmClocks.jsp?tran=3");
		openWindow("/etm/etmDataPicker.jsp?field=job&selectID=176&title=Select a JOB");
	}

	function pickDocket() {
    if (document.forms[0].elements.tcode_ID)
        document.forms[0].elements.tcode_ID.value = ""	;
    if (document.forms[0].elements.tcode_NAME)
        document.forms[0].elements.tcode_NAME.value = "";
    if (document.forms[0].elements.job_ID)
        document.forms[0].elements.job_ID.value = "";
    if (document.forms[0].elements.job_NAME)
        document.forms[0].elements.job_NAME.value = "";
    if (document.forms[0].elements.dept_ID)
    		document.forms[0].elements.dept_ID.value = ""	;
    if (document.forms[0].elements.dept_NAME)
    	  document.forms[0].elements.dept_NAME.value = "";
		var dockID = document.forms[0].elements.dock_ID.value;
		var dockName = document.forms[0].elements.dock_NAME.value;
		setFormAction("/etm/time/timesheet/etmClocks.jsp?tran=4");
		openWindow("/etm/etmDataPicker.jsp?field=dock&selectID=2&title=Select a DOCKET");
	}

	function pickTimeCode() {
    if (document.forms[0].elements.dock_ID)
        document.forms[0].elements.dock_ID.value = ""	;
    if (document.forms[0].elements.dock_NAME)
        document.forms[0].elements.dock_NAME.value = "";
    if (document.forms[0].elements.job_ID)
        document.forms[0].elements.job_ID.value = "";
    if (document.forms[0].elements.job_NAME)
        document.forms[0].elements.job_NAME.value = "";
    if (document.forms[0].elements.dept_ID)
    		document.forms[0].elements.dept_ID.value = ""	;
    if (document.forms[0].elements.dept_NAME)
    	  document.forms[0].elements.dept_NAME.value = "";
		var tcodeID = document.forms[0].elements.tcode_ID.value;
		var tcodeName = document.forms[0].elements.tcode_NAME.value;
		setFormAction("/etm/time/timesheet/etmClocks.jsp?tran=6");
		openWindow("/etm/etmDataPicker.jsp?field=tcode&selectID=3200&title=Select a TIME CODE");
	}

	function pickDepartment() {
    if (document.forms[0].elements.dock_ID)
        document.forms[0].elements.dock_ID.value = ""	;
    if (document.forms[0].elements.dock_NAME)
        document.forms[0].elements.dock_NAME.value = "";
    if (document.forms[0].elements.job_ID)
        document.forms[0].elements.job_ID.value = "";
    if (document.forms[0].elements.job_NAME)
        document.forms[0].elements.job_NAME.value = "";
    if (document.forms[0].elements.tcode_ID)
    		document.forms[0].elements.tcode_ID.value = ""	;
    if (document.forms[0].elements.tcode_NAME)
    	  document.forms[0].elements.tcode_NAME.value = "";
		var deptID = document.forms[0].elements.dept_ID.value;
		var deptName = document.forms[0].elements.dept_NAME.value;
		setFormAction("/etm/time/timesheet/etmClocks.jsp?tran=7");
		openWindow("/etm/etmDataPicker.jsp?field=dept&selectID=5&title=Select a DEPARTMENT");
	}

	function openWindow(url) {
		var width = 375;
		var height = 564;
		var left = 0;
		var top = 0;
		url = url.replace(/\s/gi,"+");
		win = window.open("<%= request.getContextPath() %>" +url,'lookup','status=no,toolbar=no,menubar=no,location=no,scrollbars=no,resizable=yes,width='+width+',height='+height+',innerWidth='+width+',innerHeight='+height+',screenX='+left+',screenY='+top+',left='+left+',top='+top);
		win.focus();
	}

	function setFormAction( action ){
		document.forms[0].action = "<%= request.getContextPath() %>" + action;
	}

	function submitParentForm(){
        document.forms[0].action += '&tzOffset=' + -1*(new Date().getTimezoneOffset())*<%=DateHelper.MINUTE_MILLISECODS%>;
		document.forms[0].submit();
	}
</script> </wb:page>