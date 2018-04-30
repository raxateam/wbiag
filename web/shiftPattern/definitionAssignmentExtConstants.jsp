<%@ include file="/system/wbheader.jsp"%>
<wb:page showUIPath="true" uiPathNameId="SPR_Page_Title" uiPathName="Shift Pattern Definition and Assignment" maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>
<input type=HIDDEN name="showAll" value="false">
<input type=HIDDEN name="whichRow" value="0">
<input type=HIDDEN name="validateLaborMsg1" value="<wb:localize id="validateLaborMsg1" ignoreConfig="true" escapeForJavascript="true"> Invalid labor time entered. </wb:localize>">
<input type=HIDDEN name="checkGapInvalidMsg" value="<wb:localize id="checkGapInvalidMsg" ignoreConfig="true" escapeForJavascript="true"> Labor times must cover the entire shift. </wb:localize>">
<input type=HIDDEN name="mustSelectEmployeeMsg" value="<wb:localize id="mustSelectEmployeeMsg" ignoreConfig="true" escapeForJavascript="true"> You must specify exact 1 employee. </wb:localize>">
<input type=HIDDEN name="mustSelectDateMsg" value="<wb:localize id="mustSelectDateMsg" ignoreConfig="true" escapeForJavascript="true"> Please Select a date. </wb:localize>">
</wb:page>