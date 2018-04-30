<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.lang.*" %>
<%@ page import="com.workbrain.app.ta.db.*" %>
<%@ page import="com.workbrain.app.ta.model.*" %>
<%@ page import="com.workbrain.util.*" %>
<%@ page import="com.workbrain.sql.*" %>
<%@ page import="com.workbrain.server.data.sql.*" %>
<%@ page import="com.workbrain.server.jsp.*" %>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.server.jsp.taglib.util.LocalizableTag" %>
<%@ page import="com.workbrain.server.data.*" %>
<%@ page import="com.workbrain.server.data.type.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="com.workbrain.app.jsp.workbrain.overrides.*" %>
<%@ page import="com.workbrain.server.control.NumberTextbox" %>
<%@ page import="com.workbrain.server.control.LocalizedMessages"%>
<%@ page import="com.workbrain.server.jsp.taglib.sys.PageTag" %>
<%@ page import="com.workbrain.server.jsp.taglib.util.WebPageRequest" %>
<%@ page import="com.workbrain.server.jsp.security.Permission" %>
<%@ page import="com.workbrain.server.jsp.locale.LocalizationDictionary" %>
<%@ page import="com.workbrain.security.SecurityService"%>
<%@ page import="com.workbrain.security.userproxy.ProxyUserService"%>
<%@ page import="com.workbrain.tool.security.SecurityEmployee"%>
<%@ page import="com.workbrain.tool.security.SecurityException"%>

<%
/**
 * History:
 *    SDM001 - Aug24.06 (QV): Mask the override field and value if user does not have access to see the field
 *
 */
%>


<%!
class ControlFieldUI{

    public void ControlFieldUI(){}

    public String getDefaultUI(String id, PageContext pc) throws Exception{
        FieldDescription fd = SQLDataDictionary.get(JSPHelper.getWebContext(pc).getConnection()).getFieldDescription(id, 310);
        return (fd == null || fd.getFieldUI() == null ? "StringUI" : fd.getFieldUI().getName());
    }

    public String getUIParam(String id, PageContext pc) throws Exception{
        FieldDescription fd = SQLDataDictionary.get(JSPHelper.getWebContext(pc).getConnection()).getFieldDescription(id, 310);
        if ( fd==null ||  fd.getFieldUI()==null )
            return "";
        return fd.getUIParameters();
    }

    public int isRequired( String id, PageContext pc ) throws Exception {
        FieldDescription fd = SQLDataDictionary.get(JSPHelper.getWebContext(pc).getConnection()).getFieldDescription(id, 310);
        if ( fd == null || ( fd != null && fd.getFieldUI() == null && fd.getFieldType() == null ) ) {
            return 0;
        }
        return fd.getRequired();
    }

    public boolean isEditable( String id, PageContext pc ) throws Exception {

            // *** Let's see if the field is editable based on permissions ***
            PageTag pt = (PageTag) (((WebPageRequest) JSPHelper.getWebContext(pc)).getWebPage());
            Permission p = pt.getPermission(id);
            if (p!=null && (!p.allowAct())) {
                return false;
            }

        FieldDescription fd = SQLDataDictionary.get(JSPHelper.getWebContext(pc).getConnection()).getFieldDescription(id, 310);
        if ( fd == null || ( fd != null && fd.getFieldUI() == null && fd.getFieldType() == null ) ) {
            return true;
        }


        // *** Let's see if we can find anything like "label=true" ***
        String uiParams = fd.getUIParameters();
        if (uiParams == null) {
            uiParams = "";
        }
        int ind = uiParams.indexOf("label=");
        if (ind == -1) {
            return true;
        }
        String sub = uiParams.substring(ind + 6, uiParams.length()).toUpperCase();
        ind = sub.indexOf("TRUE");
        if (ind >= 0 && ind <= 2) {
            return false;
        }
        return true;
    }

    public String getFormattedNumber(String id, PageContext pc, String valueStr) throws Exception {
        FieldDescription fd = SQLDataDictionary.get(JSPHelper.getWebContext(pc).getConnection()).getFieldDescription(id, 310);
        if ( fd==null ||  fd.getFieldUI()==null || !fd.getFieldUI().getName().equals("NumberUI")) {
            return valueStr;
		}
		String uiParams = fd.getUIParameters();
		if (StringHelper.isEmpty(uiParams)) {
			return valueStr;
		}
		NamedParameterMap namedParamMap = new NamedParameterMap(uiParams);
		String paramVal = namedParamMap.getParameter("scale");
        int scale = paramVal==null?37:Integer.parseInt(paramVal);

        paramVal = namedParamMap.getParameter("precision");
        int precision = paramVal==null?0:Integer.parseInt(paramVal);

        NumberTextbox numberbox = new NumberTextbox();
        numberbox.setScale(scale);
        numberbox.setPrecision(precision);
        numberbox.setValue(valueStr);
        return numberbox.getValue().toString();
	}

}

	class OvrEmployeeHelper {
		DBConnection dbConn = null;

		public OvrEmployeeHelper(DBConnection dbConn) {
			this.dbConn = dbConn;
		}

		public Set getPolicyBalances(String empId) throws SQLException {
			Set employeesPolicyBalances = new HashSet();

			PreparedStatement pStmt = null;
			ResultSet rSet = null;
			try {
	            //TT25485 Restrict access to only those balances that are contained by leave management
	            // policies associated with this employee.
	            StringBuffer buf = new StringBuffer();
	            buf.append("select bal_name ");
	            buf.append("from balance ");
	            buf.append("where ");
	            buf.append("bal_id in ( ");
	            buf.append("    select bal_id ");
	            buf.append("    from employee_balance ");
	            buf.append("    where ");
	            buf.append("    bal_id in ( ");
	            buf.append("        select bal_id ");
	            buf.append("        from ent_detail ");
	            buf.append("        where ");
	            buf.append("        ent_id in ( ");
	            buf.append("            select ent_id ");
	            buf.append("            from ent_policy_entitlement ");
	            buf.append("            where ");
	            buf.append("            entpol_id in ( ");
	            buf.append("                select entpol_id ");
	            buf.append("                from ent_emp_policy ");
	            buf.append("                where emp_id = ? ))))");

				pStmt = this.dbConn.prepareStatement(buf.toString());
				pStmt.setString(1, empId);
	            rSet = pStmt.executeQuery();
	            while(rSet.next()) {
	                employeesPolicyBalances.add(rSet.getString(1));
	            }
	         }
	         finally {
	         	SQLHelper.cleanUp(pStmt, rSet);
	         }



        	return employeesPolicyBalances;
		}

	}


/* tt2252 */
String secureOvrNewValueDisplay( PageContext pc, String ovrNewValue ) {
    ArrayList updatedTokenList = new ArrayList();

    OverrideData od = new OverrideData();
    od.setOvrNewValue( ovrNewValue );

    List tokenList = od.getNewOverrides();
    PageTag pt = (PageTag) (((WebPageRequest) JSPHelper.getWebContext(pc)).getWebPage());
    for( int i=tokenList.size()-1; i >= 0; i-- ) {
        OverrideData.OverrideToken ot = (OverrideData.OverrideToken) tokenList.get(i);
        Permission permission = pt.getPermission(ot.getName());
        if (permission.equals(Permission.DENY)){
            ot.setName("***");
            ot.setValue("***");
        }

        updatedTokenList.add(ot);
    }

    return( od.createOverrideValue(updatedTokenList) );
}
%>

<%--
***************************************************************************************************************************

PURPOSE:				Allows the User to view an Employee's Details,Balances, Default Labour Allocation


DEPENDENCIES:			ovrEmployeeSelect.jsp

PARAMETERS:

MODIFICATION HISTORY:	Norm Poole - 05/29/2001 (Created)

****************************************************************************************************************************
--%>
<wb:page maintenanceFormId='354' uiPathName='Employee Overrides' uiPathNameId='EMPLOYEE_OVERRIDES' showUIPath='true'>
    <script src="<%= request.getContextPath() %>/overrides/ovrCode.js"></script>

    <wb:define id="js_noPermissionCreateOverrideForSpecifiedDate"><wb:localize id="NO_PERMISSION_CREATE_OVRRIDE_FOR_SPECIFIED_DATE" ignoreConfig="true">You do not have permissions to create an override for the specified start date</wb:localize></wb:define>
    <wb:define id="js_permanentCheckboxBeUncheckedToChangeOverrideEndDate"><wb:localize id="PERMANENT_CHECKBOX_BE_UNCHECKED_TO_CHANGE_OVERRIDE_END_DATE" ignoreConfig="true">The Permanent checkbox must be unchecked to change the Override End Date</wb:localize></wb:define>

    <wb:config id="NO_PERMISSION_CREATE_OVRRIDE_FOR_SPECIFIED_DATE"/>
    <wb:config id="PERMANENT_CHECKBOX_BE_UNCHECKED_TO_CHANGE_OVERRIDE_END_DATE"/>
    <wb:config id="OVERRIDE_BEFORE_HANDSOFF_DATE"/>
    <script>
         var JSVAR_NO_PERM_CREATE_OVR_SPECIFIED_DATE = "<%= js_noPermissionCreateOverrideForSpecifiedDate.toString() %>";
         var JSVAR_PERMANENT_CHECKBOX_BE_UNCHECKED_TO_CHANGE_OVERRIDE_END_DATE = "<%= js_permanentCheckboxBeUncheckedToChangeOverrideEndDate.toString() %>";
    </script>

    <wb:if expression="#isDefined('session.wbg_session')#" operator="<>">
        <wb:define id='wbg_session' scope='session'/>
    </wb:if>

    <wb:define id='empId'><wb:get id="EMP_ID_0" scope="parameter" default=""/></wb:define>
    <wb:define id='empDefLabNum, num, tempLoc, onChangeString, ovrId, temp, resolvedFormat'/>

<% if("".equals(empId.toString())) { %>
    <wb:forward page="ovrEmployeeSelect.jsp"/>
<% } else {
 	int userId;
	if (!JSPHelper.getWebLogin(request).isProxied()){
		userId = SecurityService.getCurrentUser().getUserId();
	} else {
		userId = ProxyUserService.getProxiedUser().getUserId();
	}
    if(!SecurityEmployee.canSeeEmployee(JSPHelper.getConnection(request),
            empId.toInt(), userId)) {
        throw new SecurityException("You do not have access to view this employee");
    }

	LocalizationHelper lh = new LocalizationHelper(pageContext);
    com.workbrain.server.control.ControlHelper.addLocalizedMessage(com.workbrain.server.jsp.JSPHelper.getWebContext(pageContext),
                                                                    "INCORRECT_VALUE");
    final String CF = LocalizableTag.CONFIG_FIELD;
	final String CM = LocalizableTag.CONFIG_MESSAGE;
	boolean configOn = lh.isConfigOn();
    DBConnection dbConn = JSPHelper.getConnection( request );
    DBServer dbs = dbConn.getDBServer();
    Statement stmt;
    ResultSet rs;
    stmt = dbConn.createStatement();

    ControlFieldUI cfui = new ControlFieldUI();
    OverrideFunctions ovrFun = new OverrideFunctions();

    String dateOnlyFormat = "yyyyMMdd";
    boolean blnOvrPermissions = false;
    boolean blnHandsOff = false;
    boolean blnSupervisor = false;
    String empWbg;
    int empid;
    String lookUpDate;
    java.sql.Date lookUpDateOnly;
    boolean blnPermissions;
    String ovrEmpVisibility;
    String ovrEmpBalVisibility;
    String ovrEDLAVisibility;
    String show = "";
    String sqlQuery = "";
    java.sql.Date ovrStartDate;
    String ovrEndDate = "";
    String ovrStartTime = "";
    String ovrEndTime = "";
    String ovrStatus = "";
    String ovrNewValue = "";
    String rowType = "";
    String ovrEmpOvr = "";
    String ovrEmpBalOvr = "";
    String ovrEmpDLAOvr = "";
    String ovrEmpOrig = "";
    String ovrEmpBalOrig = "";
    String strTemp = "";
    String strKey = "";
    String strValue = "";
    String strNewValue = "";
    String sTemp = "";
    String sTempDLAPercent = "";
    String sDateTimeFormat = "yyyyMMdd HHmmss";
    String oraDateTimeFormat = "yyyy-MM-dd HH:mm:ss.S";
    String mdyFmtString = "MM/dd/yyyy";
    String LOCK_DOWN = "";
    java.sql.Date dateHandsOff = null;
    java.sql.Date supervisorDate = null;
    String ovrMsg = null;

	OvrEmployeeHelper ovrEmpHelper = new OvrEmployeeHelper(JSPHelper.getConnection(request));

    DateFormat normalDateTimeFmt = new SimpleDateFormat( sDateTimeFormat );
    DateFormat jspDateTimeFmt = new SimpleDateFormat( JSPConstants.DEFAULT_DATETIME_FORMAT );
    DateFormat dateOnlyFmt = new SimpleDateFormat( "yyyyMMdd" );
    DateFormat mdyFmt = new SimpleDateFormat( mdyFmtString );
    // *** Override Type Permissions ***
    Hashtable ovrTypPermissions = new Hashtable();
    rs = stmt.executeQuery("SELECT OVRTYP_ID, WBP_ID FROM OVERRIDE_TYPE_GRP WHERE WBG_ID=" + JSPHelper.getWebContext(pageContext).getLogin().getGroupId());
    while (rs.next()) {
        ovrTypPermissions.put(rs.getString("OVRTYP_ID"), rs.getString("WBP_ID"));
    }
    if ( rs != null ) rs.close();
    session.putValue("OVRTYP_PERMISSIONS", ovrTypPermissions);
    Object obj = ovrTypPermissions.get(String.valueOf(OverrideData.EMP_DLA_TYPE_START));
    int edlaPermission = 1;
    if (obj != null) {
        edlaPermission = Integer.parseInt(obj.toString());
    }
    obj = ovrTypPermissions.get(String.valueOf(OverrideData.EMP_BAL_TYPE_START));
    int empbalPermission = 1;
    if (obj != null) {
        empbalPermission = Integer.parseInt(obj.toString());
    }
    obj = ovrTypPermissions.get(String.valueOf(OverrideData.EMPLOYEE_TYPE_START));
    int empPermission = 1;
    if (obj != null) {
        empPermission = Integer.parseInt(obj.toString());
    }


    int ovrTypId;
    int rowNum=0;
    int counter;
    int iIndex=-1;
    int index1=0;
    int index2=0;
    final int DLA_EMPTY_ROWS=2;

    if (session.getValue("wbg_session")!=null) {
        empWbg = session.getValue("wbg_session").toString();
    } else {
        empWbg = "";
    }
    if( request.getParameter("EMP_ID_0")!=null ) {
        try {
            empid = Integer.parseInt( request.getParameter("EMP_ID_0") );
        } catch( NumberFormatException exc ) {
            empid = 0;  /** @todo fix exception handling */
        }
    } else {
        empid = 0;
    }
    if( request.getParameter("OVR_DATE_0") != null ) {
        lookUpDate = request.getParameter("OVR_DATE_0");
        lookUpDateOnly = new java.sql.Date( dateOnlyFmt.parse( lookUpDate ).getTime() );
    } else {
        lookUpDate = "";
        lookUpDateOnly = new java.sql.Date( 0 );
    }
    if( request.getParameter("blnPermissions") != null ) {
        blnPermissions = Boolean.valueOf( request.getParameter(
            "blnPermissions" ) ).booleanValue();
    } else {
        blnPermissions = true;
    }
    if( request.getParameter("empVisibility")!=null ) {
        ovrEmpVisibility = request.getParameter("empVisibility");
    } else {
        ovrEmpVisibility = "HIDE";
    }
    if( request.getParameter("empBalVisibility")!=null ) {
        ovrEmpBalVisibility = request.getParameter("empBalVisibility");
    } else {
        ovrEmpBalVisibility = "HIDE";
    }
    if( request.getParameter("eDLAVisibility")!=null ) {
        ovrEDLAVisibility = request.getParameter("eDLAVisibility");
    } else {
        ovrEDLAVisibility = "HIDE";
    }
try{
    if(empWbg.equals("")){
        sqlQuery = "SELECT WBG_LOCKDOWN FROM WORKBRAIN_USER, WORKBRAIN_GROUP " +
                   " WHERE WORKBRAIN_USER.WBU_NAME= '" +
                   JSPHelper.getWebLogin(request).getUserName() +
                   "' AND WORKBRAIN_USER.WBG_ID = WORKBRAIN_GROUP.WBG_ID";
        rs = stmt.executeQuery( sqlQuery );
        if(rs.next()){
            LOCK_DOWN = rs.getString("WBG_LOCKDOWN");
            if("Y".equals(LOCK_DOWN) || "true".equals(LOCK_DOWN)){%>
                <wb:set id='wbg_session' scope='session'>Y</wb:set>
            <%}else{%>
                <wb:set id='wbg_session' scope='session'>N</wb:set>
            <%}
        }else{%>
            <wb:set id='wbg_session' scope='session'>N</wb:set>
        <%}
    if ( rs != null ) rs.close();
    }

    //Get the Hands OFF Date
    sqlQuery = "SELECT PAYGRP_SUPERVISOR_DATE SUPERVISOR_DATE," + // 'yyyyMMdd'
        " PAYGRP_HANDS_OFF_DATE HANDS_OFF_DATE" + // 'yyyyMMdd'
        " FROM EMPLOYEE, PAY_GROUP " +
        " WHERE EMPLOYEE.EMP_ID = " + empid +
        " AND EMPLOYEE.PAYGRP_ID = PAY_GROUP.PAYGRP_ID";
    rs = stmt.executeQuery( sqlQuery );
    if( rs.next() ) {
        dateHandsOff = rs.getDate( "HANDS_OFF_DATE" );
        supervisorDate = rs.getDate("SUPERVISOR_DATE");

        blnHandsOff =  lookUpDateOnly.compareTo( dateHandsOff ) <= 0 ;
        blnSupervisor =  lookUpDateOnly.compareTo( supervisorDate ) <= 0
                        && lookUpDateOnly.compareTo( dateHandsOff ) > 0 ;
    %>
        <wb:define id='wbg_page'><wb:get id='wbg_session' scope='session'/></wb:define>
        <%if(blnHandsOff ||
                (blnSupervisor && wbg_page.toString().equals("N"))){%>
            <h3><%= js_noPermissionCreateOverrideForSpecifiedDate.toString() %></h3>
            <%blnPermissions = false;
        }
    }
    if ( rs != null ) rs.close();

    String displayNameSQL = "VWU.WBU_NAME as DisplayName";
    String overrideCreatorDisplayNameType = (String) Registry.getVarString("system/WORKBRAIN_PARAMETERS/TS_OVERRIDE_CREATOR_DISPLAY_NAME","WBU_NAME");
 	if ("WBU_NAMEACTUAL".equalsIgnoreCase(overrideCreatorDisplayNameType)) {
        displayNameSQL = "WBU_NAME_ACTUAL as DisplayName";
    }
    if ("EMP_NAME".equalsIgnoreCase(overrideCreatorDisplayNameType)) {
	    displayNameSQL = "VWU.EMP_NAME as DisplayName";
	}

	if ("EMP_FULLNAME_FL".equalsIgnoreCase(overrideCreatorDisplayNameType)) {
		String args[] = {"VWU.EMP_FIRSTNAME","\'  \'","VWU.EMP_LASTNAME"};
	    displayNameSQL = dbConn.encodeConcatStrings(args) + " as DisplayName";
	}

	if ("EMP_FULLNAME_LF".equalsIgnoreCase(overrideCreatorDisplayNameType)) {
		String args[] = {"VWU.EMP_LASTNAME","\', \'","VWU.EMP_FIRSTNAME"};
	    displayNameSQL = dbConn.encodeConcatStrings(args) + " as DisplayName";
	}

    %>

    <%--TT10119-nsivakumar-Jan 15,2003-localized heading--%>
    <h3><wb:localize id="Employee_Override">Employee Override</wb:localize></h3>

    <div class=separatorSmall />

    <%if(ovrEmpVisibility.equals("SHOW")){%>
        <wba:button label="Hide Employee Overrides"
                        labelLocalizeIndex="Hide_Employee_Overrides"
                        width="225"
                        onClick="document.forms[0].empVisibility.value='HIDE';document.forms[0].submit();return false;" />&nbsp;
    <%}else{%>
        <wba:button label="Show Employee Overrides"
                        labelLocalizeIndex="Show_Employee_Overrides"
                        width="225"
                        onClick="document.forms[0].empVisibility.value='SHOW';document.forms[0].submit();return false;" />&nbsp;
    <%}

    if(ovrEmpBalVisibility.equals("SHOW")){%>
        <wba:button label="Hide Employee Balance Overrides"
                        labelLocalizeIndex="Hide_Employee_Balance_Overrides"
                        width="280"
                        onClick="document.forms[0].empBalVisibility.value='HIDE';document.forms[0].submit();return false;" />&nbsp;
    <%}else{%>
        <wba:button label="Show Employee Balance Overrides"
                        labelLocalizeIndex="Show_Employee_Balance_Overrides"
                        width="280"
                        onClick="document.forms[0].empBalVisibility.value='SHOW';document.forms[0].submit();return false;" />&nbsp;
    <%}

    if(ovrEDLAVisibility.equals("SHOW")){%>
        <wba:button label="Hide Employee Default Labour Overrides"
                        labelLocalizeIndex="Hide_Employee_Default_Labour_Overrides"
                        width="343"
                        onClick="document.forms[0].eDLAVisibility.value='HIDE';document.forms[0].submit();return false;" />&nbsp;
    <%}else{%>
        <wba:button label="Show Employee Default Labour Overrides"
                        labelLocalizeIndex="Show_Employee_Default_Labour_Overrides"
                        width="343"
                        onClick="document.forms[0].eDLAVisibility.value='SHOW';document.forms[0].submit();return false;" />&nbsp;
    <%}%>

    <div class=separatorSmall />

    <%
    String empRec = "";
    String empBalRec = "";
    Map edlDTO = new HashMap();

    CodeMapper codeMapper = CodeMapper.createCodeMapper( dbConn );
    // load employee view as of the view date

    EmployeeData empData = new EmployeeAccess( dbConn, codeMapper ).load( empid,
        lookUpDateOnly );
    if( empData != null ) {
        empRec = empData.makeEmployeeJspString( dbConn );
        empBalRec = empData.makeEmpBalJspString( dbConn, lookUpDateOnly );
        edlDTO = empData.createEmpDefLabDTO( dbConn, lookUpDateOnly );
    }

    empRec = empRec.substring(1,empRec.length()-1);
    StringBuffer requiredString = new StringBuffer("");
    %>

    <%
    boolean empDeny = empPermission == -1;
    if (!empDeny) {
    %>
    <%--Employee Details--%>
        <wba:table caption="Employee" captionLocalizeIndex="Employee" securityName="Employee">
        <%
        StringTokenizer stEmp = StringHelper.tokenize(empRec,"\",\"");
        String mode = empPermission == 1? "edit":"view";

        while (stEmp.hasMoreTokens()){
            strTemp = stEmp.nextToken();
            iIndex = strTemp.indexOf("=");
            if(iIndex!=-1){
                strKey = strTemp.substring(0,iIndex);
                strValue = strTemp.substring(iIndex+1,strTemp.length());
            }
            show = cfui.getDefaultUI(strKey,pageContext);
            %>
            <wb:set id='tempLoc'><%=strKey%></wb:set>
            <%if(JSPHelper.getWebSession(request).isConfigOn() || !show.equals("HiddenUI")){%>
            <wb:secureContent securityName='#tempLoc#' showKey='false'>
            <tr>
                <th><wb:localize id='#tempLoc#' overrideId='310' type='field'/></th>
                <td>
                    <%if ( blnPermissions ) {
                        if ("EMP_EFFECTIVE_DATE".equals(strKey)){
                            %><wb:controlField id='#tempLoc#' overrideId='310' cssClass="inputField" submitName='#tempLoc#' mode="view"><%=strValue%></wb:controlField><%
                            continue;
                        }
                        /*
                        / a hack - perform conversion from the internal override string date/time format to
                        / to the internal timestamp format
                        */

                        if(show.equals("DatePickerUI") && strValue.indexOf("/") > 0 && strValue.indexOf(":") > 0)
                            strValue = strValue.substring(6,10) + strValue.substring(0,2) + strValue.substring(3,5) +
                                " " + strValue.substring(11,13) + strValue.substring(14,16) + "00";

                        /*  get the system default string representation of the date.
                            needed for a hidden field to figure out what really changed
                            since the UI class generated hidden field always uses system
                            default format */
                        if(show.equals("DatePickerUI")){
                            String uiParamStr = cfui.getUIParam(strKey,pageContext);
                            String sysFmtDateVal = strValue;
                            if ( ! StringHelper.isEmpty( uiParamStr ) ) {
                                // strValue is a formatted value of date, restore the system default str representation for other process.
                                NamedParameterMap uiParamMap = new NamedParameterMap(uiParamStr);
                                %><wb:set id='resolvedFormat'><%=uiParamMap.getParameter("format")%></wb:set><%
                                SimpleDateFormat sdf = new SimpleDateFormat(resolvedFormat.toString());
                                try {
                                    java.util.Date dateVal = sdf.parse(strValue);
                                    sysFmtDateVal = DatetimeType.FORMAT.format(dateVal);
                                } catch (ParseException pe) {
                                    // incoming date format is not equal to whats put on the localize page.
                                    // could happen after changing format.
                                }
                            } else {
                                uiParamStr = "format='#page.property.defaultDateFormat#'";
                            }
                        %>
                            <wb:set id='onChangeString'>document.forms[0].DP_<%=strKey%>.value=this.value;</wb:set>
                            <input type='hidden' name='DP_<%=strKey%>' value='<%=sysFmtDateVal%>'>
                            <wb:controlField id='#tempLoc#' overrideId='310' cssClass="inputField" submitName='#tempLoc#' onChange='#onChangeString#' uiParameter='<%=uiParamStr%>' mode='<%= mode%>' ui='DatePickerUI'><%=strValue%></wb:controlField>
                        <%}else if(show.equals("TimeEditUI")){%>
                            <wb:set id='onChangeString'>document.forms[0].TIME_EDIT_<%=strKey%>.value=this.value;</wb:set>
                            <input type='hidden' name='TIME_EDIT_<%=strKey%>' value='<%=strValue%>'>
                            <wb:controlField id='#tempLoc#'  overrideId='310' cssClass="inputField" submitName='#tempLoc#' mode='<%= mode%>' onChange='#onChangeString#'><%=strValue%></wb:controlField>

                        <%}else if(show.equals("CurrencyUI")){%>
                            <wb:set id='onChangeString'>document.forms[0].Currency_<%=strKey%>.value=this.value;</wb:set>
                            <input type='hidden' name='Currency_<%=strKey%>' value='<%=strValue%>'>
                            <wb:controlField id='#tempLoc#'  overrideId='310' cssClass="inputField" submitName='#tempLoc#' mode='<%= mode%>' onChange='#onChangeString#'><%=strValue%></wb:controlField>

                        <%}else if(show.equals("DBLookupUI")){%>
                            <wb:controlField id='#tempLoc#'  overrideId='310' mode='<%= mode%>' cssClass="inputField" submitName='#tempLoc#'><%=strValue%></wb:controlField>
                        <%}else{%>
                            <wb:controlField id='#tempLoc#' overrideId='310' mode='<%= mode%>' cssClass="inputField" submitName='#tempLoc#'><%=strValue%></wb:controlField>
                        <%}%>
                        <!--input type='hidden' name='<%=strKey%>' value="<%=strValue%>"-->
                        <%
                        int required   = cfui.isRequired(strKey,pageContext);
                        if (cfui.isEditable(strKey, pageContext)) {
                            requiredString.append(strKey).append("=").append( (required==1)?"true":"false").append(",");
                            ovrEmpOrig = ovrEmpOrig + "'" + strKey + "=" + strValue + "',";
                        }
                    }else{
                        if (strKey.equals("EMP_ID")){%>
                            <input type='hidden' name='EMP_ID' value='<%=strValue%>'>
                        <%}else{
                            if (tempLoc.toString().indexOf("_NAME")==-1){%>
                                <wb:controlField id='#tempLoc#' overrideId='310' cssClass="inputField" submitName='#tempLoc#' mode='view'><%=strValue%></wb:controlField>
                            <%}else{%>
                                <SPAN style='' Class='inputField'><%=strValue%></SPAN>
                            <%}
                        }
                    }%>
                </td>
            </tr>
            </wb:secureContent>
            <%}
        }%>
        </wba:table>

        <input type='hidden' name='REQUIRED_FIELDS' value='<%=requiredString.toString()%>'>

        <%if (ovrEmpOrig.length()>0){%>
            <input type='hidden' name='EMP_ORIGINAL' value="<%=ovrEmpOrig.substring(0,ovrEmpOrig.length()-1)%>">
        <%}
    }

    //Show Employee Overrides
    if(ovrEmpVisibility.equalsIgnoreCase("SHOW")){%>
        <div class=separatorLarge />
        <%
            PreparedStatement prepStmt = dbConn.prepareStatement(
          	"SELECT OVR_ID, OVR_OLD_VALUE, OVR_NEW_VALUE, OVR_START_DATE, OVR_END_DATE, " +
			"OVR_START_TIME,OVR_END_TIME, OVR_STATUS, OVERRIDE.OVRTYP_ID AS OVRTYP_ID, OVR_MESSAGE, " +
			"OVR_COMMENT,OVR_CREATE_DATE, " + displayNameSQL +
			" FROM OVERRIDE,EMPLOYEE,OVERRIDE_TYPE, VIEW_WORKBRAIN_USER VWU WHERE EMPLOYEE.EMP_ID = ? " +
			"AND OVR_STATUS <> 'CANCELLED' AND OVR_STATUS <> 'CANCEL' AND " +
			"OVR_END_DATE >= ? " +
			"AND OVERRIDE.OVRTYP_ID BETWEEN " + OverrideData.EMPLOYEE_TYPE_START + " AND " + OverrideData.EMPLOYEE_TYPE_END +
			" AND OVERRIDE_TYPE.OVRTYP_EMPLOYEE='Y' AND OVERRIDE.EMP_ID = EMPLOYEE.EMP_ID AND " +
			"OVERRIDE.OVRTYP_ID = OVERRIDE_TYPE.OVRTYP_ID AND VWU.WBU_NAME = OVERRIDE.WBU_NAME " +
			" UNION ALL " +
			"SELECT OVR_ID, OVR_OLD_VALUE, OVR_NEW_VALUE, OVR_START_DATE, OVR_END_DATE, " +
			"OVR_START_TIME,OVR_END_TIME, OVR_STATUS, OVERRIDE.OVRTYP_ID, OVR_MESSAGE, " +
			"OVR_COMMENT,OVR_CREATE_DATE, WBU_NAME as DisplayName " +
			" FROM OVERRIDE,EMPLOYEE,OVERRIDE_TYPE WHERE EMPLOYEE.EMP_ID = ? " +
			"AND OVR_STATUS <> 'CANCELLED' AND OVR_STATUS <> 'CANCEL' AND " +
			"OVR_END_DATE >= ? " +
			"AND OVERRIDE.OVRTYP_ID BETWEEN " + OverrideData.EMPLOYEE_TYPE_START + " AND " + OverrideData.EMPLOYEE_TYPE_END +
			" AND OVERRIDE_TYPE.OVRTYP_EMPLOYEE='Y' AND OVERRIDE.EMP_ID = EMPLOYEE.EMP_ID AND " +
			"OVERRIDE.OVRTYP_ID = OVERRIDE_TYPE.OVRTYP_ID AND " +
			"OVERRIDE.WBU_NAME not in (SELECT WBU_NAME FROM VIEW_WORKBRAIN_USER) " +
			"ORDER BY OVRTYP_ID, OVR_START_DATE, OVR_START_TIME, OVR_ID");

        prepStmt.setInt( 1, empid );
        prepStmt.setTimestamp( 2, new java.sql.Timestamp(lookUpDateOnly.getTime()) );
        prepStmt.setInt( 3, empid );
        prepStmt.setTimestamp( 4, new java.sql.Timestamp(lookUpDateOnly.getTime()) );

        rs = prepStmt.executeQuery();
        rowNum = 0;

        if( rs.next() ) {%>
            <wba:table caption="Employee Overrides" captionLocalizeIndex="Employee_Overrides">
                <tr>
                    <%if( blnPermissions && empPermission == 1 ){%>
                    	<wb:secureContent securityName='empOvrDel' showKey='false'>
		                    <th>
		                    	<wb:secureContent securityName='empOvrDel' showKey='true' />
		                    	<wb:localize id='DELETE' overrideId='310' >Delete</wb:localize>
		                    </th>
		                </wb:secureContent>
	                <%}%>
	                <wb:secureContent securityName='empOvrStart' showKey='false'>
		                <th>
		                	<nobr>
		                		<wb:secureContent securityName='empOvrStart' showKey='true' />
		                		<wb:localize id='OVR_START_DATE' overrideId='310' type='field'>Start Date</wb:localize>
		                	</nobr>
		                </th>
		            </wb:secureContent>
	                <wb:secureContent securityName='empOvrEnd' showKey='false'>
	                    <th>
	                    	<nobr>
	                    		<wb:secureContent securityName='empOvrEnd' showKey='true' />
	                    		<wb:localize id='OVR_END_DATE' overrideId='310' type='field'>End Date</wb:localize>
	                    	</nobr>
	                    </th>
	                </wb:secureContent>
                    <th colspan='2'><wb:localize id='EMPLOYEE_OVERRIDES' overrideId='310' >Employee Overrides</wb:localize> <% out.print(lh.getConfigLink("OVERRIDE_CREATE_DATE", CF, 310)); %> </th>
                </tr>
                <%do{
                    rowType = ((rowNum%2)==0?"evenRow":"oddRow");
                    rs.getInt("OVRTYP_ID");
                    ovrTypId = (rs.wasNull()? 0 : rs.getInt("OVRTYP_ID"));%>
                    <tr class='<%=rowType%>'>
                      <%
                      ovrStartDate = rs.getDate( "OVR_START_DATE" );
                      ovrEndDate = DateHelper.convertDateString(rs.getString("OVR_END_DATE"),oraDateTimeFormat,sDateTimeFormat);
                      ovrStatus = rs.getString("OVR_STATUS");

                      rs.getDate("OVR_START_TIME");
                      ovrStartTime = (rs.wasNull()?"":
                                            DateHelper.convertDateString(rs.getString("OVR_START_TIME"),oraDateTimeFormat,sDateTimeFormat));

                      rs.getDate("OVR_END_TIME");
                      ovrEndTime = (rs.wasNull()?"":
                                            DateHelper.convertDateString(rs.getString("OVR_END_TIME"),oraDateTimeFormat,sDateTimeFormat));

                      if( blnPermissions && empPermission == 1 ){
                      	if (ovrStartDate.compareTo(dateHandsOff) > 0) {
                      		blnOvrPermissions = true;
                      	}
                      %>
                      	  <wb:secureContent securityName='empOvrDel' showKey='false'>
	                          <td> <%
		                      if( ovrStartDate.compareTo( dateHandsOff ) > 0 ) {%>
		                        <wb:set id='ovrId'><%=rs.getInt("OVR_ID")%></wb:set>
		                        <wb:controlField ui='CheckBoxUI' securityName='empOvrDel' secure='false' submitName='delEmpOvr_#ovrId#'>false</wb:controlField>
		                        <input type='hidden' name='delEmpOvrStartDate_<%=rs.getInt("OVR_ID")%>' value='<%=normalDateTimeFmt.format( ovrStartDate )%>'>
		                        <input type='hidden' name='delEmpOvrEndDate_<%=rs.getInt("OVR_ID")%>' value='<%=ovrEndDate%>'>
		                      <%}else{%>
		                        &nbsp;
		                      <%}%>
	                          </td>
	                      </wb:secureContent>
                      <%}%>
                      <wb:secureContent securityName='empOvrStart' showKey='false'>
	                      <td><wb:controlField submitName="start" id="OVR_START_DATE" overrideId="310" mode="view" securityName='empOvrStart' secure='false'><%=normalDateTimeFmt.format(ovrStartDate)%></wb:controlField></td>
	                  </wb:secureContent>
	                  <wb:secureContent securityName='empOvrEnd' showKey='false'>
	                  <td>
                      <%if(ovrEndDate.equals( EmployeeData.PERM_DATE )){%>
                        PERM
                      <%}else{%>
                        <wb:controlField submitName="end" id="OVR_END_DATE" overrideId="310" mode="view" securityName='empOvrEnd' secure='false'><%=normalDateTimeFmt.format(rs.getDate( "OVR_END_DATE" ))%></wb:controlField>
                      <%}%>
                      </td>
                      </wb:secureContent>
                      <td>

                        <%if(ovrStatus.equals("ERROR")){
                            out.println("<font class='txtOvrError'>!</font>");
                        }else if(ovrStatus.equals("APPLIED")){
                            out.println("<font class='txtOvrApplied'>+</font>");
                        }else if(ovrStatus.equals("PENDING")){
                            out.println("<font class='txtOvrPending'>?</font>");
                        }%>
                       </td>

                  <td>
                        <%if (!ovrStartTime.toString().equals("") || !ovrEndTime.toString().equals("")){
                            out.println(ovrStartTime + "-" + ovrEndTime + "&nbsp;&nbsp;");
                        }
                        sTemp = rs.getString( "OVR_NEW_VALUE" );

                        /* tt2252 */
                        if( sTemp.length() > 0 ) {
                            sTemp = secureOvrNewValueDisplay( pageContext, sTemp );
                        }

                        sTemp = sTemp.substring( 1, sTemp.length() - 1 );
                        out.println( StringHelper.searchReplace(
                            ovrFun.parseOverride( sTemp, "\",\"", pageContext, ovrTypId ),
                            "\",\"", "," ) );
                        if(ovrStatus.equals("ERROR")){
                            rs.getString("OVR_MESSAGE");
                            if (!rs.wasNull()) {
                                ovrMsg = LocalizationDictionary.localizeErrorMessage(
                                                dbConn, rs.getString("OVR_MESSAGE"),
                                                JSPHelper.getWebLocale(request).getLanguageId());
                            } else {
                                ovrMsg = "";
                            }
                            out.println(ovrMsg);
                        }%>
                        (<%=rs.getString("DisplayName")%>, <wb:controlField submitName="create" id="OVERRIDE_CREATE_DATE" overrideId="310" mode="view" securityName='OvrCrtDte' secure='false'><%=DateHelper.convertDateString(rs.getString("OVR_CREATE_DATE").toString(),"yyyy-MM-dd HH:mm:ss","yyyyMMdd HHmmss")%></wb:controlField>)<br>
                        <%rs.getString("OVR_COMMENT");
                        if(!rs.wasNull()){
                            out.println(rs.getString("OVR_COMMENT"));
                        }%>
                      </td>
                    </tr>
                    <%
                    ovrEmpOvr += "" + rs.getInt("OVR_ID") +",";
                    rowNum++;
                }while(rs.next());

                if(ovrEmpOvr.length()>0){
                    ovrEmpOvr = ovrEmpOvr.substring(0,ovrEmpOvr.length()-1);
                }%>
            </wba:table>
        <%}
        rs.close();
    }%>
    <input type='hidden' name='EMP_OVR' value='<%=ovrEmpOvr%>'>

    <div class=separatorLarge />
    <%
    boolean empbalDeny = empbalPermission == -1;
    if (!empbalDeny) {
    %>

    <%--Employee Balances--%>
        <wba:table caption="Employee Balances" captionLocalizeIndex="Employee_Balances" securityName="Employee_Balances">
            <tr>
                <th><wb:localize id='BALANCE_NAME' overrideId='310' >Balance Name</wb:localize></th>
                <th><wb:localize id='VALUE' overrideId='310' type='field'>Value</wb:localize></th>
                <%
                Permission empBalActionsPerm = lh.getPermission("EMPBAL_ACTIONS");
                if (empBalActionsPerm.allowAct() || configOn) {
	               	out.print("<th>");
                	out.print(lh.getSecurityLink("EMPBAL_ACTIONS"));
                	out.print(lh.getConfigLink("ACTION", CF, 310));
                	out.print(lh.getText("ACTION", CF, "Action"));
    	           	out.print("</th>");
    	        }
                %>
            </tr>
            <%
            iIndex=-1;
            StringTokenizer stEmpBal = new StringTokenizer( empBalRec, "|" );
            counter=0;
            String rowPosition = "";

            boolean restrictBalanceTypesBasedOnLMPolicies = false;
            Set employeesPolicyBalances = new HashSet();
            try {
                restrictBalanceTypesBasedOnLMPolicies = "true".equalsIgnoreCase((String)com.workbrain.server.registry.Registry.getVar("system/WORKBRAIN_PARAMETERS/BALANCE_ADMIN_BY_EMP_POLICY"));
                if (restrictBalanceTypesBasedOnLMPolicies) {
	                 employeesPolicyBalances = ovrEmpHelper.getPolicyBalances(empId.toString());
	            }
            } catch (Exception e) {
            	e.printStackTrace();
            }


			show = cfui.getDefaultUI("VALUE",pageContext);
            while (stEmpBal.hasMoreTokens()) {
                strTemp = stEmpBal.nextToken();
                iIndex = strTemp.indexOf(",");
                strKey = strTemp.substring(strTemp.indexOf("=")+1,iIndex-1);

                if (restrictBalanceTypesBasedOnLMPolicies && !employeesPolicyBalances.contains(strKey)) {
                    continue;
                }

                strValue = strTemp.substring(strTemp.lastIndexOf("=")+1,strTemp.length()-1);
                if (show.equals("NumberUI")) {
                    strValue = cfui.getFormattedNumber("VALUE", pageContext, strValue);
                }

                rowPosition = (counter % 2 == 0) ? "odd" : "even";
                ovrEmpBalOrig = ovrEmpBalOrig+ " ,";
                %>
                <wb:set id='num'><%=counter%></wb:set>
                <wb:set id='tempLoc'><%=strKey%></wb:set>
				<wb:secureContent securityName='#tempLoc#' showKey='false'>
	                <wba:tr position="<%= rowPosition %>">
						<%
						Permission empBalValuePerm = lh.getPermission(strKey);
						%>
    	                <th><wb:localize id='#tempLoc#' overrideId='310' type='field'/></th>
        	            <td>
            	            <% if (blnPermissions && empbalPermission == 1 && empBalActionsPerm.allowAct()) { %>
                	            <wb:controlField id='VALUE' overrideId='310' cssClass="inputField" submitName='#tempLoc#'><%=strValue.substring(strValue.indexOf("=")+1)%></wb:controlField>
                    	    <%  ovrEmpBalOrig = ovrEmpBalOrig.substring(0, ovrEmpBalOrig.lastIndexOf(",")-1) + "'" + strKey + "=" + strValue + "',";
                        	}else{
	                            if (tempLoc.toString().indexOf("_NAME")==-1){%>
    	                            <wb:controlField id='VALUE' overrideId='310' cssClass="inputField" submitName='#tempLoc#' mode='view'><%=strValue%></wb:controlField>
        	                    <%}else{%>
            	                    <SPAN style='' Class='inputField'><%=strValue%></SPAN>
                	            <%}
                    	    }%>
                    	    <input type='hidden' name='<%=tempLoc.toString()%>' value='<%=strValue%>'>
	                    </td>
	                    <%
	                    if (empBalActionsPerm.allowAct() && empBalValuePerm.allowAct() && blnPermissions && empbalPermission == 1 || configOn) {
	                    	out.print("<td>");
        	                %><wb:controlField cssClass="inputField" id='ACTION' overrideId='310' submitName='EMPBAL_ACTION_#num#' nullable='false'><%=EmployeeBalanceData.SET_ACTION%></wb:controlField><%
                        	out.print("</td>");
                        }
                        %>
	                </wba:tr>
	            </wb:secureContent>
             <%
             	counter ++;
             }%>
        </wba:table>
        <%if (ovrEmpBalOrig.length()>0){%>
                <input type='hidden' name='EMPBAL_ORIGINAL' value="<%=ovrEmpBalOrig.substring(0,ovrEmpBalOrig.length()-1)%>">
        <%}
     }
    //Show Employee Balance Overrides from 12 months ago and 2 months into the future
    if(ovrEmpBalVisibility.equalsIgnoreCase("SHOW")){
/*--begin #4491#
  --mharrison
  --April 1, 2002
  --show balances from the date specified instead of sysdate*/

          PreparedStatement prepStmt = dbConn.prepareStatement(
          	"SELECT OVR_ID, OVR_OLD_VALUE, OVR_NEW_VALUE, OVR_START_DATE, OVR_END_DATE, " +
			"OVR_START_TIME,OVR_END_TIME, OVR_STATUS, OVERRIDE.OVRTYP_ID AS OVRTYP_ID, OVR_MESSAGE, " +
			"OVR_COMMENT,OVR_CREATE_DATE, " +  displayNameSQL  +
			" FROM OVERRIDE,EMPLOYEE,OVERRIDE_TYPE, VIEW_WORKBRAIN_USER VWU WHERE EMPLOYEE.EMP_ID = ? " +
			"AND OVR_STATUS <> 'CANCELLED' AND OVR_STATUS <> 'CANCEL' AND " +
			"OVR_START_DATE BETWEEN ? AND ? " +
			"AND OVERRIDE.OVRTYP_ID BETWEEN " + OverrideData.EMP_BAL_TYPE_START + " AND " + OverrideData.EMP_BAL_TYPE_END +
			" AND OVERRIDE_TYPE.OVRTYP_EMPLOYEE='Y' AND OVERRIDE.EMP_ID = EMPLOYEE.EMP_ID AND " +
			"OVERRIDE.OVRTYP_ID = OVERRIDE_TYPE.OVRTYP_ID AND VWU.WBU_NAME = OVERRIDE.WBU_NAME " +
			" UNION ALL " +
			"SELECT OVR_ID, OVR_OLD_VALUE, OVR_NEW_VALUE, OVR_START_DATE, OVR_END_DATE, " +
			"OVR_START_TIME,OVR_END_TIME, OVR_STATUS, OVERRIDE.OVRTYP_ID, OVR_MESSAGE, " +
			"OVR_COMMENT,OVR_CREATE_DATE, WBU_NAME as DisplayName " +
			" FROM OVERRIDE,EMPLOYEE,OVERRIDE_TYPE WHERE EMPLOYEE.EMP_ID = ? " +
			"AND OVR_STATUS <> 'CANCELLED' AND OVR_STATUS <> 'CANCEL' AND " +
			"OVR_START_DATE BETWEEN ? AND ? " +
			"AND OVERRIDE.OVRTYP_ID BETWEEN " + OverrideData.EMP_BAL_TYPE_START + " AND " + OverrideData.EMP_BAL_TYPE_END +
			" AND OVERRIDE_TYPE.OVRTYP_EMPLOYEE='Y' AND OVERRIDE.EMP_ID = EMPLOYEE.EMP_ID AND " +
			"OVERRIDE.OVRTYP_ID = OVERRIDE_TYPE.OVRTYP_ID AND " +
			"OVERRIDE.WBU_NAME not in (SELECT WBU_NAME FROM VIEW_WORKBRAIN_USER) " +
			"ORDER BY OVRTYP_ID, OVR_START_DATE, OVR_START_TIME, OVR_ID");

/*--end #4491#*/
        prepStmt.setInt( 1, empid );
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime( lookUpDateOnly );
        cal.add( Calendar.MONTH, -12 );
        java.sql.Date start = new java.sql.Date( cal.getTime().getTime() );
        cal.setTime( lookUpDateOnly );
        cal.add( Calendar.MONTH, 2 );
        java.sql.Date end = new java.sql.Date( cal.getTime().getTime() );
        prepStmt.setTimestamp( 2, new java.sql.Timestamp(start.getTime()) );
        prepStmt.setTimestamp( 3, new java.sql.Timestamp(end.getTime()) );
		prepStmt.setInt( 4, empid );
		prepStmt.setTimestamp( 5, new java.sql.Timestamp(start.getTime()) );
        prepStmt.setTimestamp( 6, new java.sql.Timestamp(end.getTime()) );




        rs = prepStmt.executeQuery();
        rowNum=0;
        if(rs.next()){%>
            <div class=separatorLarge />
            <wba:table caption="Employee Balance Overrides" captionLocalizeIndex="Employee_Balance_Overrides">
                <tr>
	                <%if( blnPermissions && empbalPermission == 1 ){%>
	                	<wb:secureContent securityName='delEmpBalOvr' showKey='false'>
		                    <th>
		                    	<wb:secureContent securityName='delEmpBalOvr' showKey='true' />
		                    	<wb:localize id='DELETE'>Delete</wb:localize>
		                    </th>
		                </wb:secureContent>
	                <%}%>
	                <wb:secureContent securityName='empBalOvrStart' showKey='false'>
		                <th>
		                	<nobr>
			                	<wb:secureContent securityName='empBalOvrStart' showKey='true' />
		                		<wb:localize id='OVR_START_DATE' overrideId='310' type='field'>Start Date</wb:localize>
		                	</nobr>
		                </th>
		            </wb:secureContent>
		            <wb:secureContent securityName='empBalOvrEnd' showKey='false'>
	                    <th>
	                    	<nobr>
								<wb:secureContent securityName='empBalOvrEnd' showKey='true' />
	                    		<wb:localize id='OVR_END_DATE' overrideId='310' type='field'>End Date</wb:localize>
	                    	</nobr>
	                    </th>
	                </wb:secureContent>
                    <th colspan='2'><wb:localize id='EMPLOYEE_OVERRIDES'>Employee Overrides</wb:localize></th>
                </tr>
            <%
            do{
                rowType = ((rowNum%2)==0?"evenRow":"oddRow");
                rs.getInt("OVRTYP_ID");
                ovrTypId = (rs.wasNull()? 0 : rs.getInt("OVRTYP_ID"));%>
                <tr class='<%=rowType%>'>
                    <%
                    ovrStartDate = rs.getDate( "OVR_START_DATE" );
                    ovrEndDate = DateHelper.convertDateString(rs.getString("OVR_END_DATE"),oraDateTimeFormat,sDateTimeFormat);
                    ovrStatus = rs.getString("OVR_STATUS");

                    rs.getDate("OVR_START_TIME");
                    ovrStartTime = (rs.wasNull()?"":
                                            DateHelper.convertDateString(rs.getString("OVR_START_TIME"),oraDateTimeFormat,sDateTimeFormat));

                    rs.getDate("OVR_END_TIME");
                    ovrEndTime = (rs.wasNull()?"":
                                            DateHelper.convertDateString(rs.getString("OVR_END_TIME"),oraDateTimeFormat,sDateTimeFormat));

				    if ( blnPermissions && empbalPermission == 1 ){
				    	if (ovrStartDate.compareTo(dateHandsOff) > 0) {
	                        ovrEmpBalOvr += "" + rs.getInt("OVR_ID") +",";
	                        blnOvrPermissions = true;
	                    }
				    %>
				    	<wb:secureContent securityName='delEmpBalOvr' showKey='false'>
				    	<td> <%
	                    if( ovrStartDate.compareTo( dateHandsOff ) > 0 ) {%>
	                        <wb:set id='ovrId'><%=rs.getInt("OVR_ID")%></wb:set>
	                        <wb:controlField ui='CheckBoxUI' submitName='delEmpBalOvr_#ovrId#' securityName='delEmpBalOvr' secure='false'>false</wb:controlField>
	                        <input type='hidden' name='delEmpBalOvrStartDate_<%=rs.getInt("OVR_ID")%>'
	                            value='<%=normalDateTimeFmt.format( ovrStartDate )%>'>
	                        <input type='hidden' name='delEmpBalOvrEndDate_<%=rs.getInt("OVR_ID")%>'
	                            value='<%=ovrEndDate%>'>
	                    <%}else{
	                        out.println("&nbsp;");
	                    }%>
		                </td>
		                </wb:secureContent>
                <%}%>
                <wb:secureContent securityName='empBalOvrStart' showKey='false'>
	                <td>
	                	<wb:controlField submitName="start" id="OVR_START_DATE" overrideId="310" mode="view" securityName='empBalOvrStart' secure='false'><%=normalDateTimeFmt.format(ovrStartDate)%></wb:controlField>
	                </td>
	            </wb:secureContent>
	            <wb:secureContent securityName='empBalOvrEnd' showKey='false'>
					<td>
						<% if (ovrEndDate.equals(EmployeeData.PERM_DATE)) { %>
							PERM
						<% } else { %>
							<wb:controlField submitName="end" id="OVR_END_DATE" overrideId="310" mode="view" securityName='empBalOvrEnd' secure='false'><%=normalDateTimeFmt.format(rs.getDate( "OVR_END_DATE" ))%></wb:controlField>
						<% } %>
					</td>
				</wb:secureContent>

                  <td>
                    <%if(ovrStatus.equals("ERROR")){
                        out.println("<font class='txtOvrError'>!</font>");
                    }else if(ovrStatus.equals("APPLIED")){
                        out.println("<font class='txtOvrApplied'>+</font>");
                    }else if(ovrStatus.equals("PENDING")){
                        out.println("<font class='txtOvrPending'>?</font>");
                    }%>
                   </td>

               <td>
                    <%if (!ovrStartTime.toString().equals("") || !ovrEndTime.toString().equals("")){
                        out.println(ovrStartTime + "-" + ovrEndTime + "&nbsp;&nbsp;");
                    }
                    sTemp = rs.getString("OVR_NEW_VALUE");
                    sTemp = sTemp.substring(1,sTemp.length()-1);
                    out.println(StringHelper.searchReplace(ovrFun.parseOverride(sTemp,""+'"'+","+'"',pageContext,ovrTypId),'"'+","+'"',","));
                    if(ovrStatus.equals("ERROR")){
                        rs.getString("OVR_MESSAGE");
                        if (!rs.wasNull()) {
                            ovrMsg = LocalizationDictionary.localizeErrorMessage(
                                            dbConn, rs.getString("OVR_MESSAGE"),
                                            JSPHelper.getWebLocale(request).getLanguageId());
                        } else {
                            ovrMsg = "";
                        }
                        out.println(ovrMsg);
                    }%>
                    (<%=rs.getString("DisplayName")%>, <%=DateHelper.convertDateString(rs.getString("OVR_CREATE_DATE"), oraDateTimeFormat, JSPConstants.DEFAULT_DATETIME_FORMAT)%>)<br>
                    <%rs.getString("OVR_COMMENT");
                    if(!rs.wasNull()){
                        out.println(rs.getString("OVR_COMMENT"));
                    }%>
                  </td>
                </tr>
                <%rowNum++;
            }while(rs.next());%>
            </wba:table>
        <%}
        rs.close();
    }%>
    <input type='hidden' name='EMPBAL_OVR' value='<%=ovrEmpBalOvr%>'>

    <div class=separatorLarge />



    <%
    // **************************
    // Employee Labour Allocation
   	// **************************

    List edlNames = (List)edlDTO.get( "NAMES" );
    Set edlReqdFields = (Set)edlDTO.get( "REQUIRED_FIELDS" );
    List edlRows = (List)edlDTO.get( "ROWS" );
    boolean edlEditable = blnPermissions && edlaPermission == 1;
	boolean edlaDeny = 	edlaPermission == -1;
    session.setAttribute( "EMPDLA_FIELD_NAMES", edlNames );
    session.setAttribute( "EMPDLA_REQUIRED_FIELDS", edlReqdFields );

    if (!edlaDeny) {
	%>

	<wba:table caption="Employee Labour Allocation" captionLocalizeIndex="Employee_Labour_Allocation" securityName="Employee_Labour_Allocation">
		<wb:define id='currRow,mode' />
		<%-- Header row --%>
		<tr>
        	<% if (edlEditable) { %>
				<wb:secureContent securityName='delEmpDLA' showKey='false'>
	            	<th>
						<wb:secureContent securityName='delEmpDLA' showKey='true' />
						<wb:localize id='DELETE'>Delete</wb:localize>
					</th>
				</wb:secureContent>
			<% }

			for (int i = 0; i < edlNames.size(); i++) {
				String name = (String)edlNames.get(i);
				String uiType = cfui.getDefaultUI(name, pageContext);
                if (configOn || !uiType.equals("HiddenUI")) {
                %>
                	<wb:set id="temp"><%=name%></wb:set>
					<wb:secureContent securityName='#temp#' showKey='false'>
						<th>
							<wb:secureContent securityName='#temp#' showKey='true' />
							<wb:localize id='#temp#' overrideId='310' type='field'/>
						</th>
                    </wb:secureContent>
                <%
                }
			}
			%>
			<th style='display:none'></th>
		</tr>

		<%
		// Rows with data

		for (int row = 0; row < edlRows.size(); row++) {
			List edlRow = (List)edlRows.get(row);
			out.print("<tr>");

			// Delete checkbox
			if (edlEditable) { %>
				<wb:set id="currRow"><%=new Value(String.valueOf(row), false)%></wb:set>
				<wb:secureContent securityName='delEmpDLA' showKey='false'>
					<td>
						<wb:controlField cssClass='inputField' submitName='delEmpDLA_#currRow#' ui='CheckBoxUI' securityName='delEmpDLA' secure='false'>false</wb:controlField>
					</td>
				</wb:secureContent>
				<%
			}

			for (int col = 0; col < edlNames.size(); col++) {
				String name = (String)edlNames.get(col);
				Object oValue = edlRow.get(col);
				String value = oValue==null ? "" : oValue.toString();
				String uiType = cfui.getDefaultUI(name, pageContext);

				if (configOn || !uiType.equals("HiddenUI")) {
					%>
					<wb:set id='temp'><%=name%></wb:set>
					<wb:set id='tempLoc'><%=name + "_" + row%></wb:set>
					<wb:set id='mode'><%=edlEditable ? "edit" : "view"%></wb:set>
					<wb:secureContent securityName='#temp#' showKey='false'>
						<td>
							<wb:controlField overrideId='310' cssClass='inputField' id='#temp#' submitName='#tempLoc#' securityName='#temp#' secure='false' mode='#mode#'><%=value%></wb:controlField>
						</td>
					</wb:secureContent>
					<%
					if (!lh.isAct(name)){
					%>
					<input type=hidden name="<%=name + "_" + row%>" value="<%=value%>"></input>
					<%
					}
				}
				else{
				%>
					<input type=hidden name="<%=name + "_" + row%>" value="<%=value%>"></input>
				<%
				}
			}
			%>

				<td style='display:none'>
					<% if (row == edlRows.size() - 1) { %>
						<input type='hidden' name='EMPDLA_ROWS' value='<%=edlRows.size()%>' />
					<% }
					if (((String)edlNames.get(0)).equals("EDLA_PERCENTAGE")) {
						String ratioName = (String)edlNames.get(0);
						String ratioValue = edlRow.get(0).toString();
						%><input type='hidden' name='<%=ratioName + "_" + row%>' value='<%=ratioValue%>'><%
					}
					if (edlEditable) {
            session.setAttribute( "EMPDLA_ORIGINAL_" + row, edlRow );
          } %>
				</td>
			</tr><%
		}

		// Empty rows when the user has edit permissions.
		if (edlEditable) {
			for (int i = edlRows.size(); i < edlRows.size() + DLA_EMPTY_ROWS; i++) {%>
				<tr>
					<wb:secureContent securityName='delEmpDLA' showKey='false'>
						<td>&nbsp;</td>
					</wb:secureContent>

					<% for (int col = 0; col < edlNames.size(); col++) {
						String fieldName = (String)edlNames.get(col);
						String uiType = cfui.getDefaultUI(fieldName, pageContext);
                        if (configOn || !uiType.equals("HiddenUI")) {%>
							<wb:set id='tempLoc'><%=fieldName + "_" + i%></wb:set>
                            <wb:set id='temp'><%=fieldName%></wb:set>
                            <wb:secureContent securityName='#temp#' showKey='false'>
								<td><wb:controlField overrideId='310' cssClass='inputField' id='#temp#' submitName='#tempLoc#' securityName='#temp#' secure='false'><%if (col==0){%>0<%}%></wb:controlField></td>
							</wb:secureContent>
                        <%}
                    }
				out.print("<td sytle='display:none'></td></tr>");
			}
		}
		%>
	</wba:table>
	<%
	} %>

    <%--Employee Default Labour Allocation Overrides--%>
    <%if(ovrEDLAVisibility.equalsIgnoreCase("SHOW")){
        PreparedStatement prepStmt = dbConn.prepareStatement(
          	"SELECT OVR_ID, OVR_OLD_VALUE, OVR_NEW_VALUE, OVR_START_DATE, OVR_END_DATE, " +
			"OVR_START_TIME,OVR_END_TIME, OVR_STATUS, OVERRIDE.OVRTYP_ID AS OVRTYP_ID, OVR_MESSAGE, " +
			"OVR_COMMENT,OVR_CREATE_DATE, " + displayNameSQL +
			" FROM OVERRIDE,EMPLOYEE,OVERRIDE_TYPE, VIEW_WORKBRAIN_USER VWU WHERE EMPLOYEE.EMP_ID = ? " +
			"AND OVR_STATUS <> 'CANCELLED' AND OVR_STATUS <> 'CANCEL' AND " +
			"OVR_END_DATE >= ? " +
			"AND OVERRIDE.OVRTYP_ID BETWEEN " + OverrideData.EMP_DLA_TYPE_START + " AND " + OverrideData.EMP_DLA_TYPE_END+
			" AND OVERRIDE_TYPE.OVRTYP_EMPLOYEE='Y' AND OVERRIDE.EMP_ID = EMPLOYEE.EMP_ID AND " +
			"OVERRIDE.OVRTYP_ID = OVERRIDE_TYPE.OVRTYP_ID AND VWU.WBU_NAME = OVERRIDE.WBU_NAME " +
			" UNION ALL " +
			"SELECT OVR_ID, OVR_OLD_VALUE, OVR_NEW_VALUE, OVR_START_DATE, OVR_END_DATE, " +
			"OVR_START_TIME,OVR_END_TIME, OVR_STATUS, OVERRIDE.OVRTYP_ID, OVR_MESSAGE, " +
			"OVR_COMMENT,OVR_CREATE_DATE, WBU_NAME as DisplayName " +
			" FROM OVERRIDE,EMPLOYEE,OVERRIDE_TYPE WHERE EMPLOYEE.EMP_ID = ? " +
			"AND OVR_STATUS <> 'CANCELLED' AND OVR_STATUS <> 'CANCEL' AND " +
			"OVR_END_DATE >= ? " +
			"AND OVERRIDE.OVRTYP_ID BETWEEN " + OverrideData.EMP_DLA_TYPE_START + " AND " + OverrideData.EMP_DLA_TYPE_END+
			" AND OVERRIDE_TYPE.OVRTYP_EMPLOYEE='Y' AND OVERRIDE.EMP_ID = EMPLOYEE.EMP_ID AND " +
			"OVERRIDE.OVRTYP_ID = OVERRIDE_TYPE.OVRTYP_ID AND " +
			"OVERRIDE.WBU_NAME not in (SELECT WBU_NAME FROM VIEW_WORKBRAIN_USER) " +
			"ORDER BY OVRTYP_ID, OVR_START_DATE, OVR_START_TIME, OVR_ID");

        prepStmt.setInt( 1, empid );
        prepStmt.setTimestamp( 2, new java.sql.Timestamp(lookUpDateOnly.getTime()) );
		prepStmt.setInt( 3, empid );
        prepStmt.setTimestamp( 4, new java.sql.Timestamp(lookUpDateOnly.getTime()) );

        rs = prepStmt.executeQuery();
%>
        <div class=separatorLarge />
        <%if(rs.next()){%>
            <wba:table caption="Default Labour Overrides" captionLocalizeIndex="Default_Labour_Overrides">
                <tr>
					<% if( blnPermissions && edlaPermission == 1) { %>
						<wb:secureContent securityName='DELETE_DEFLABOVR' showKey='false'>
							<th>
								<wb:secureContent securityName='DELETE_DEFLABOVR' showKey='true' />
								<wb:localize id='DELETE'>Delete</wb:localize>
							</th>
						</wb:secureContent>
					<% } %>

                    <wb:secureContent securityName='defLabOvrStart' showKey='false'>
	                    <th>
							<nobr>
								<wb:secureContent securityName='defLabOvrStart' showKey='true' />
		                    	<wb:localize id='OVR_START_DATE' overrideId='310' type='field'>Start Date</wb:localize>
		                    </nobr>
						</th>
	                </wb:secureContent>

	                <wb:secureContent securityName='defLabOvrEnd' showKey='false'>
	                    <th>
	                    	<nobr>
								<wb:secureContent securityName='defLabOvrEnd' showKey='true' />
	                    		<wb:localize id='OVR_END_DATE' overrideId='310' type='field'>End Date</wb:localize>
	                    	</nobr>
	                    </th>
	                </wb:secureContent>

                    <th colspan='2'><wb:localize id='DEF_LAB_ALL_OVERRIDES'>Default Labour Allocation Overrides</wb:localize></th>
                </tr>
                <%rowNum=0;

                do{
                    rowType = ((rowNum%2)==0?"evenRow":"oddRow");
                    rs.getInt("OVRTYP_ID");
                    ovrTypId = (rs.wasNull()? 0 : rs.getInt("OVRTYP_ID"));%>
                    <tr class='<%=rowType%>'>
                        <%
                        ovrStartDate = rs.getDate( "OVR_START_DATE" );
                        ovrEndDate = DateHelper.convertDateString(rs.getString("OVR_END_DATE"),oraDateTimeFormat,sDateTimeFormat);
                        ovrStatus = rs.getString("OVR_STATUS");

                        rs.getDate("OVR_START_TIME");
                        ovrStartTime = (rs.wasNull()?"":
                                                DateHelper.convertDateString(rs.getString("OVR_START_TIME"),oraDateTimeFormat,sDateTimeFormat));

                        rs.getDate("OVR_END_TIME");
                        ovrEndTime = (rs.wasNull()?"":
                                                DateHelper.convertDateString(rs.getString("OVR_END_TIME"),oraDateTimeFormat,sDateTimeFormat));

                   	    if(blnPermissions && edlaPermission == 1) {
                   	    	if (ovrStartDate.compareTo(dateHandsOff) > 0) {
								ovrEmpDLAOvr += "" + rs.getInt("OVR_ID") +",";
								blnOvrPermissions = true;
							}
                   	    %>
							<wb:secureContent securityName='DELETE_DEFLABOVR' showKey='false'>
								<td> <%
									if( ovrStartDate.compareTo( dateHandsOff ) > 0 ) {%>
	    	                        	<wb:set id='ovrId'><%=rs.getInt("OVR_ID")%></wb:set>
										<wb:controlField ui='CheckBoxUI' submitName='delEmpDLAOvr_#ovrId#' securityName='DELETE_DEFLABOVR' secure='false'>false</wb:controlField>
										<input type='hidden' name='delEmpDLAOvrStartDate_<%=rs.getInt("OVR_ID")%>' value='<%=normalDateTimeFmt.format( ovrStartDate )%>'>
										<input type='hidden' name='delEmpDLAOvrEndDate_<%=rs.getInt("OVR_ID")%>' value='<%=ovrEndDate%>'>
									<%}else{
										out.println("&nbsp;");
									}%>
								</td>
							</wb:secureContent>
						<%}%>

						<wb:secureContent securityName='defLabOvrStart' showKey='false'>
	                    	<td>
	                    		<wb:controlField submitName="start" id="OVR_START_DATE" overrideId="310" mode="view" securityName='defLabOvrStart' secure='false'><%=normalDateTimeFmt.format(ovrStartDate)%></wb:controlField>
							</td>
						</wb:secureContent>

						<wb:secureContent securityName='defLabOvrEnd' showKey='false'>
							<td>
								<%if(ovrEndDate.equals( EmployeeData.PERM_DATE )){%>
									PERM
								<%}else{%>
									<wb:controlField submitName="end" id="OVR_END_DATE" overrideId="310" mode="view" securityName='defLabOvrEnd' secure='false'><%=normalDateTimeFmt.format(rs.getDate( "OVR_END_DATE" ))%></wb:controlField>
								<%}%>
							</td>
						</wb:secureContent>

                      <td>
                        <%if(ovrStatus.equals("ERROR")){
                            out.println("<font class='txtOvrError'>!</font>");
                        }else if(ovrStatus.equals("APPLIED")){
                            out.println("<font class='txtOvrApplied'>+</font>");
                        }else if(ovrStatus.equals("PENDING")){
                            out.println("<font class='txtOvrPending'>?</font>");
                        }%>
                  </td>

                  <td>
                        <%if (!ovrStartTime.toString().equals("") || !ovrEndTime.toString().equals("")){
                            out.println(ovrStartTime + "-" + ovrEndTime + "&nbsp;&nbsp;");
                        }
                        sTemp = rs.getString("OVR_NEW_VALUE");
                        sTemp = sTemp.substring(1,sTemp.length()-1);
                        out.println(StringHelper.searchReplace(ovrFun.parseOverride(sTemp,""+'"'+","+'"',pageContext,ovrTypId),'"'+","+'"',","));
                        if(ovrStatus.equals("ERROR")){
                            rs.getString("OVR_MESSAGE");
                            if (!rs.wasNull()) {
                                ovrMsg = LocalizationDictionary.localizeErrorMessage(
                                                dbConn, rs.getString("OVR_MESSAGE"),
                                                JSPHelper.getWebLocale(request).getLanguageId());
                            } else {
                                ovrMsg = "";
                            }
                            out.println(ovrMsg);
                        }%>
                        (<%=rs.getString("DisplayName")%>, <%=DateHelper.convertDateString(rs.getString("OVR_CREATE_DATE"), oraDateTimeFormat, JSPConstants.DEFAULT_DATETIME_FORMAT)%>)<br>
                       <%rs.getString("OVR_COMMENT");
                        if(!rs.wasNull()){
                            out.println(rs.getString("OVR_COMMENT"));
                        }%>
                      </td>
                    </tr>
                    <%rowNum++;
                }while(rs.next());%>
            </wba:table>
        <%}%>
    <input type='hidden' name='EMPDLA_OVR' value='<%=ovrEmpDLAOvr.toString()%>'>
    <%}%>

    <div class=separatorLarge />
    <%if( blnPermissions ){%>
        <wba:table >
        	<%
        	out.print("<tr>");
        	if (lh.getPermission("OVR_PERMANENT").allowView() || configOn) {
        		out.print("<th>");
        		out.print(lh.getSecurityLink("OVR_PERMANENT"));
        		out.print(lh.getConfigLink("PERMANENT", CM));
        		out.print(lh.getText("PERMANENT", CM, "Permanent"));
        		out.print("</th>");
        	}
        	if (lh.getPermission("OVR_CURR_START_DATE").allowView() || configOn) {
        		out.print("<th>");
        		out.print(lh.getSecurityLink("OVR_CURR_START_DATE"));
				out.print(lh.getConfigLink("OVR_CURR_START_DATE", CF, 310));
				out.print(lh.getConfigLink("OVERRIDE_START_DATE", CM));
				out.print(lh.getText("OVERRIDE_START_DATE", CM, "Override Start Date"));
        		out.print("</th>");
        	}
        	if (lh.getPermission("OVR_CURR_END_DATE").allowView() || configOn) {
        		out.print("<th>");
        		out.print(lh.getSecurityLink("OVR_CURR_END_DATE"));
        		out.print(lh.getConfigLink("OVR_CURR_END_DATE", CF, 310));
        		out.print(lh.getConfigLink("OVERRIDE_END_DATE", CM));
        		out.print(lh.getText("OVERRIDE_END_DATE", CM, "Override End Date"));
        		out.print("</th>");
        	}
        	if (lh.getPermission("APPLY_TO_EMPLOYEES").allowView() || configOn) {
        		out.print("<th>");
        		out.print(lh.getSecurityLink("APPLY_TO_EMPLOYEES"));
        		out.print(lh.getConfigLink("APPLY_TO_EMPLOYEES", CF));
        		out.print(lh.getText("APPLY_TO_EMPLOYEES", CF, "Apply to Employees"));
        		out.print("</th>");
        	}
        	if (lh.getPermission("APPLY_TO_TEAMS").allowView() || configOn) {
        		out.print("<th>");
        		out.print(lh.getSecurityLink("APPLY_TO_TEAMS"));
        		out.print(lh.getConfigLink("APPLY_TO_TEAMS", CF, 310));
        		out.print(lh.getText("APPLY_TO_TEAMS", CF, "Apply to Teams"));
        		out.print("</th>");
        	}
        	if (lh.getPermission("OVR_COMMENT").allowView() || configOn) {
        		out.print("<th>");
        		out.print(lh.getSecurityLink("OVR_COMMENT"));
        		out.print(lh.getConfigLink("OVERRIDE_COMMENT", CF, 310));
        		out.print(lh.getText("OVERRIDE_COMMENT", CF, "Override Comment"));
        		out.print("</th>");
        	}
        	out.print("</tr><tr>");
        	if (lh.getPermission("OVR_PERMANENT").allowView() || configOn) {
        		%><td><wb:controlField cssClass='inputField' submitName='OVR_PERMANENT' ui='CheckBoxUI' uiParameter='checked=true' onChange='enableEndDate(this.checked);' secure='false'>true</wb:controlField></td><%
        	}
        	if (lh.getPermission("OVR_CURR_START_DATE").allowView() || configOn) {
        		%><td><wb:controlField cssClass='inputField' overrideId='310' id='OVR_CURR_START_DATE' submitName='OVR_CURR_START_DATE' secure='false'><%=lookUpDate%></wb:controlField></td><%
        	}
        	if (lh.getPermission("OVR_CURR_END_DATE").allowView() || configOn) {
        		%>
	            <wb:set id='onChangeString'>checkPermOvr('<%=DateHelper.convertDateString( EmployeeData.PERM_DATE ,sDateTimeFormat, JSPConstants.DEFAULT_DATE_FORMAT)%>');</wb:set>
    	        <td><wb:controlField cssClass='inputField' overrideId='310' id='OVR_CURR_END_DATE' submitName='OVR_CURR_END_DATE' onChange='#onChangeString#' secure='false'><%=EmployeeData.PERM_DATE%></wb:controlField></td>
    	        <%
        	}
        	if (lh.getPermission("APPLY_TO_EMPLOYEES").allowView() || configOn) {
        		%><td><wb:controlField cssClass='inputField' overrideId='310' id='APPLY_TO_EMPLOYEES' submitName='APPLY_TO_EMPLOYEES' secure='false' /></td><%
        	}
        	if (lh.getPermission("APPLY_TO_TEAMS").allowView() || configOn) {
        		%><td><wb:controlField cssClass='inputField' overrideId='310' id='APPLY_TO_TEAMS' submitName='APPLY_TO_TEAMS' secure='false' /></td><%
        	}
        	if (lh.getPermission("APPLY_TO_TEAMS").allowView() || configOn) {
        		%><td><wb:controlField cssClass='inputField' overrideId='310' id='OVR_COMMENT' submitName='OVR_COMMENT' secure='false' /></td><%
        	}
        	%>
        	</tr>
        </wba:table>
    <%}%>
    <div class=separatorSmall />
    <%if( blnPermissions || blnOvrPermissions){%>
        <wba:button name="Submit" label="Submit" labelLocalizeIndex="Submit"
                onClick="if(!checkPreConditions()){return false;}SubmitEmpOverrides('SUBMIT');return false;" />
        &nbsp;<wba:button name="Save" label="Save & Continue" labelLocalizeIndex="Save_and_Continue"
                onClick="if(!checkPreConditions()){return false;}SubmitEmpOverrides('SAVE');return false;" />
    <%}else{%>
        <wba:button label="Submit" labelLocalizeIndex="Submit" disabled="true" />
        &nbsp;<wba:button label="Save & Continue" labelLocalizeIndex="Save_and_Continue" disabled="true"/>
    <%}%>

    &nbsp;<wba:button label="Cancel" labelLocalizeIndex="Cancel" onClick="CancelEmpOverrides(); return false;" />

    <input type='hidden' name='EMP_ID_0' value='<%=empid%>'>
    <input type='hidden' name='OVR_DATE_0' value='<%=lookUpDate%>'>
    <input type='hidden' name='empVisibility' value='<%=ovrEmpVisibility%>'>
    <input type='hidden' name='empBalVisibility' value='<%=ovrEmpBalVisibility%>'>
    <input type='hidden' name='eDLAVisibility' value='<%=ovrEDLAVisibility%>'>
    <input type='hidden' name='submitFlag' value=''>
    <input type='hidden' name='HANDS_OFF_DATE' value='<%=mdyFmt.format( dateHandsOff )%>'>
    <input type='hidden' name='SUPERVISOR_DATE' value='<%=mdyFmt.format( supervisorDate )%>'>
    <input type='hidden' name='SUPERVISOR' value='<%=blnSupervisor%>'>
    <input type='hidden' name='PERMISSIONS' value='<wb:get id='wbg_session' scope='session'/>'>
    <input type='hidden' name='blnPermissions' value='<%=blnPermissions%>'>
<%--begin #2026#
  --mharrison
  --Oct. 10, 2001
  --on submit from employee override return to the screen you came from
--%>
    <input type='hidden' name='SUBMIT_TYPE' value='<wb:get id="SUBMIT_TYPE" scope="parameter" default=""/>'>
<%--end #2026#--%>
    <%--
      --begin #949#
      --npoole
      --August 15, 2001
      --'End Date' date picker should be blocked out while 'Permanent' field is checked.
    --%>
    <script>
        enableEndDate(true);

        function checkPreConditions(){
        	validateFormFields(''); //validate form fields and perform resolving (execute nothing)
        	var validated = true;
        	validated = validated && checkRequiredFields();
        	validated = validated && checkDates();
        	return validated;
        }

        function checkDates(){
            var handsOffDateStr = document.forms[0].HANDS_OFF_DATE.value;
            var ovrStartDateStr = document.forms[0].OVR_CURR_START_DATE.value;
            var handsOffDate = handsOffDateStr.toDate("MM/dd/yyyy");
            /*same as mdyFmtString in ovrEmployee.jsp*/
            var ovrStartDate = ovrStartDateStr.toDate("yyyyMMdd");
            /*as defined by dateOnlyFormat in ovrEmployeeSubmit.jsp*/
            if(ovrStartDate <= handsOffDate){
            	alert ("<wb:localize id="OVERRIDE_BEFORE_HANDSOFF_DATE" ignoreConfig="true" escapeForJavascript="true">The override start date cannot be on or before the hands off date.</wb:localize>");
				return false;
            }

            return true;
        }

        function checkRequiredFields(){
            var requirements = document.forms[0].REQUIRED_FIELDS.value.split(",");
            var values;
            var requiredfields=true;
            for(var ix=0;ix<requirements.length;ix++){
                values=requirements[ix].split("=");
                if(values[1]=="true"){
                    var obj = eval("document.forms[0]."+values[0]);
                    if(obj.value.trimBothSides() == ''){
                        alert(getLocalizedMessage_VALUE_REQUIRED());
                        var focus;
                        if( document.forms[0].elements[values[0]+'_label'] != null ) {
                            focus = eval("document.forms[0]."+values[0]+'_label');
                        } else {
                            focus = eval("document.forms[0]."+values[0]);
                        }
                        focus.focus();
                        requiredfields=false;
                    }
                }
            }
            return requiredfields;
        }

    </script>
    <%--end--%>
<%
}// catch(SQLException e) {
//}
finally {
    if ( rs != null ) rs.close();
}
%>
<% } %>

</wb:page>
