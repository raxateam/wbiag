<%@ include file="/system/wbheader.jsp"%>
<%@ page import="com.wbiag.app.modules.retailSchedule.errorDetection.*"%>
<%@ page import="com.workbrain.server.registry.Registry"%>
<%@ page import="com.workbrain.util.DateHelper"%>
<%@ page import="com.workbrain.util.StringHelper"%>
<%@ page import="com.workbrain.util.LocalizationHelper"%>
<%@ page import="com.workbrain.server.jsp.taglib.util.LocalizableTag"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="org.apache.log4j.Logger"%>
<%@ page import="org.apache.log4j.Level"%>


<%!
    private static final Logger logger = Logger.getLogger("modules.retailSchedule.LfsoErrorScript.jsp");
    public static final String DEFAULT_DETECTION_REGISTRY_REG = "com.wbiag.app.modules.retailSchedule.errorDetection.DefaultErrorDetectionRegisty";

%>

<%--
****************************************************************************************************
*
* PURPOSE:        	This page validates the schedule optimization set up Part 2
*
* DEPENDENCIES:   	None
*
* PARAMETERS:		None
*
* CREATION INFORMATION:  Adapted from the solution created by Rob Rebelo and Tony Young by Chris Leigh
*
* MODIFICATION HISTORY:
*
*    Jan 19, 2005  Wilson Woo
*    ========================
*        - Added addition validation checks accord the TT 43002, LFSO Error Detection Script
*    Dec 07, 2005  Tony Young
*    ========================
*        - Added Functional Specifications in TT 280
*    Dec 16, 2005  Tony Young
*    ========================
*        - Refactored as in TT 603
*    Dec 19, 2005  Tony Young
*    ========================
*        - Added Functional Specifications in TT 502
*		 - Added Functional Specifications in TT 602
*	 Jan 26, 2006  Tony Young
*	 ========================
*		 - Updated for 5.0 according to TT 607
*	 Feb 03, 2006  Tony Young
*	 ========================
*		 - Updated with TT 748 specs
*	 Dec 2006 Chris Leigh
*    ========================
*		- Created Error Detection Module
*
****************************************************************************************************

--%>
<style>
TEXTAREA
{
    font-size:100%;
    font-family: Verdana,Sans Serif;

    border-right: #000000 0px solid;
    border-top: #000000 0px solid;
    border-left: #000000 0px solid;
    border-bottom: #000000 0px solid;

    height: 80;
    width: 660;
}

/* Colors */
.failed
{
    color: red;
}

.warning
{
    color: orange;
}
</style>

<script type="text/javascript">
<!--
function popHelpMsg( title, content ) {
    displayHTML = "<html>\n<head>\n<title>Help</title>\n";
    displayHTML += "<body>\n";
    displayHTML += "<link rel=\"stylesheet\" href=\"css/erosterIE.css\">\n";
    displayHTML += "<h2>" + title + "</h2>"
    displayHTML += content + "</body></html>";
    newWindow = window.open( "", "", "status = 1, height = 300, width = 300, resizable = 0" )
    newWindow.document.write( displayHTML );
}
//-->
</script>

<wb:page login="true" subsidiaryPage="false" title="Schedule Optimization Validation">

<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" /></wb:define>
<wb:submit id="mfrm_id"><wb:get id="mfrm_id"/></wb:submit>
<wb:define id="LFSO_ED_SKDGRP_ID"><wb:get id="LFSO_ED_SKDGRP_ID" scope="parameter" default=""/></wb:define>
<wb:define id="LFSO_ED_EMPGRP_ID"><wb:get id="LFSO_ED_EMPGRP_ID" scope="parameter" default=""/></wb:define>
<wb:define id="LFSO_ED_START_DATE"><wb:get id="LFSO_ED_START_DATE" scope="parameter" default=""/></wb:define>
<wb:define id="LFSO_ED_FAILED_FLAG"><wb:get id="LFSO_ED_FAILED_FLAG" scope="parameter" default=""/></wb:define>
<wb:define id="LFSO_ED_SHOW_APPLIED_FLAG"><wb:get id="LFSO_ED_SHOW_APPLIED_FLAG" scope="parameter" default=""/></wb:define>
<wb:define id="LFSO_ED_BUG_FLAG"><wb:get id="LFSO_ED_BUG_FLAG" scope="parameter" default=""/></wb:define>
<wb:define id="LFSO_ED_SYSTEM_FLAG"><wb:get id="LFSO_ED_SYSTEM_FLAG" scope="parameter" default=""/></wb:define>
<wb:define id="LFSO_ED_SCHEDULE_FLAG"><wb:get id="LFSO_ED_SCHEDULE_FLAG" scope="parameter" default=""/></wb:define>
<wb:define id="LFSO_ED_FORECAST_FLAG"><wb:get id="LFSO_ED_FORECAST_FLAG" scope="parameter" default=""/></wb:define>

<%

    String mfrmIdStr = mfrm_id.toString();
	if (StringHelper.isEmpty(mfrmIdStr)) {
	    throw new RuntimeException("mfrm_id must be supplied in querystring, check mfrm_parameter_jps setting for this maintenance form");
	}
    LocalizationHelper lh = new LocalizationHelper(pageContext);
    

%>
<br>

<wba:table caption="Schedule Optimization Validation" captionLocalizeIndex="SO_VALIDATION">

<tr>
    <td width="10%"/>
    <wba:th>
	    <% out.print(lh.getCaption("LFSO_ED_SKDGRP_ID", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "Root Location:"));%>
    </wba:th>
    <td>
   		<%out.print(lh.getUI("LFSO_ED_SKDGRP_ID", "LFSO_ED_SKDGRP_ID", LFSO_ED_SKDGRP_ID.toString(), null, "edit", Integer.parseInt(mfrmIdStr), "inputField"));%>
    </td>
</tr>
<tr>
    <td width="10%"/>
	<wba:th>
		<% out.print(lh.getCaption("LFSO_ED_EMPGRP_ID", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "Staff Group:"));%>
	</wba:th>
	<td>
		<%out.print(lh.getUI("LFSO_ED_EMPGRP_ID", "LFSO_ED_EMPGRP_ID", LFSO_ED_EMPGRP_ID.toString(), null, "edit", Integer.parseInt(mfrmIdStr), "inputField"));%>
	</td>
</tr>
<tr>
    <td width="10%"/>
	<wba:th>
		<%out.print(lh.getCaption("LFSO_ED_START_DATE", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "Start Date:"));%>
	</wba:th>
	<td>
		<%out.print(lh.getUI("LFSO_ED_START_DATE", "LFSO_ED_START_DATE", LFSO_ED_START_DATE.toString(), null, "edit", Integer.parseInt(mfrmIdStr), "inputField"));%>
	</td>
</tr>
<tr>
    <td width="10%"/>
    <wba:th>
    	<%out.print(lh.getCaption("LFSO_ED_FAILED_FLAG", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "Show Failed Only:"));%>
    </wba:th>
    <td>
	    <%out.print(lh.getUI("LFSO_ED_FAILED_FLAG", "LFSO_ED_FAILED_FLAG", LFSO_ED_FAILED_FLAG.toString(), null, "edit", Integer.parseInt(mfrmIdStr), "inputField"));%>
    </td>
</tr>
<tr>
    <td width="10%"/>
    <wba:th>
    	<%out.print(lh.getCaption("LFSO_ED_SHOW_APPLIED_FLAG", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "Show Applied:"));%>
    </wba:th>
    <td>
	    <%out.print(lh.getUI("LFSO_ED_SHOW_APPLIED_FLAG", "LFSO_ED_SHOW_APPLIED_FLAG", LFSO_ED_SHOW_APPLIED_FLAG.toString(), null, "edit", Integer.parseInt(mfrmIdStr), "inputField"));%>
    </td>
</tr>
<tr>
    <td width="10%"/>
    <wba:th>
    	<%out.print(lh.getCaption("LFSO_ED_BUG_FLAG", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "Bug Checks:"));%>
    </wba:th>
    <td>
    	<%out.print(lh.getUI("LFSO_ED_BUG_FLAG", "LFSO_ED_BUG_FLAG", LFSO_ED_BUG_FLAG.toString(), null, "edit", Integer.parseInt(mfrmIdStr), "inputField"));%>
    </td>
</tr>
<tr>
    <td width="10%"/>
    <wba:th>
    	<%out.print(lh.getCaption("LFSO_ED_SYSTEM_FLAG", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "System Environment:"));%>
    </wba:th>
    <td>
	    <%out.print(lh.getUI("LFSO_ED_SYSTEM_FLAG", "LFSO_ED_SYSTEM_FLAG", LFSO_ED_SYSTEM_FLAG.toString(), null, "edit", Integer.parseInt(mfrmIdStr), "inputField"));%>
    </td>
</tr>
<tr>
    <td width="10%"/>
    <wba:th>
    	<%out.print(lh.getCaption("LFSO_ED_FORECAST_FLAG", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "Forecast Generation:"));%>
    </wba:th>
    <td>
	    <%out.print(lh.getUI("LFSO_ED_FORECAST_FLAG", "LFSO_ED_FORECAST_FLAG", LFSO_ED_FORECAST_FLAG.toString(), null, "edit", Integer.parseInt(mfrmIdStr), "inputField"));%>
    </td>
</tr>
<tr>
    <td width="10%"/>
    <wba:th>
	    <%out.print(lh.getCaption("LFSO_ED_SCHEDULE_FLAG", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "Schedule Generation:"));%>
    </wba:th>
    <td>
	    <%out.print(lh.getUI("LFSO_ED_SCHEDULE_FLAG", "LFSO_ED_SCHEDULE_FLAG", LFSO_ED_SCHEDULE_FLAG.toString(), null, "edit", Integer.parseInt(mfrmIdStr), "inputField"));%>
    </td>
</tr>
</wba:table>
<br>
<input class="buttonSmall" type="Submit" value="Run Script Now" name="Submit">
<br>
<wb:if expression='#LFSO_ED_SKDGRP_ID#' compareToExpression='' operator="<>">
<wba:table caption="Validation Results" captionLocalizeIndex="SO_VALIDATION_RESULTS">


<!-------------------------------------------------------------------------------------------->
<!-------------------------------------------------------------------------------------------->
<!--Run Error Detection Script-->
<!-------------------------------------------------------------------------------------------->
<!-------------------------------------------------------------------------------------------->
<%


		 ErrorDetectionRegisty registry = null;
         String registryClass = null;
         try {
         	registryClass = Registry.getVarString(ErrorDetectionRegisty.DETECTION_REGISTRY_REG_PATH);
         } catch (Exception e) {
         	registryClass = null;
         }
         try {
              if (StringHelper.isEmpty(registryClass)){
        		 registry = ((ErrorDetectionRegisty)Class.forName(DEFAULT_DETECTION_REGISTRY_REG).newInstance());
        	 } else {
        		 registry = ((ErrorDetectionRegisty)Class.forName(registryClass).newInstance());
        	 }
         } catch (ClassNotFoundException cnfe) {
             logger.error("Unable to find ErrorDetectionRegisty class: " + cnfe.getMessage(),
                          cnfe);
             throw new Exception("Unable to find ErrorDetectionRegisty class: "
                     + cnfe.getMessage(), cnfe);
         } catch (IllegalAccessException iae) {
             logger.error("Access violation occured while creating ErrorDetectionRegisty: " 
                     + iae.getMessage(), iae);
             throw new Exception("Access violation occured while creating " +
                     "ErrorDetectionRegisty: " + iae.getMessage(), iae);
         } catch (InstantiationException ie)  {
             logger.error("Unable to create ErrorDetectionRegisty: " + ie.getMessage(), ie);
             throw new Exception("Unable to create ErrorDetectionRegisty: "+ ie.getMessage(), ie);
         }
        
        ArrayList checkTypes = new ArrayList();
        if (!StringHelper.isEmpty(LFSO_ED_BUG_FLAG) &&
        		("Y").equals(LFSO_ED_BUG_FLAG.toString())){
        	checkTypes.add(ErrorDetectionRule.BUG_TYPE);
        }
        if (!StringHelper.isEmpty(LFSO_ED_SYSTEM_FLAG) &&
        		("Y").equals(LFSO_ED_SYSTEM_FLAG.toString())){
        	checkTypes.add(ErrorDetectionRule.SYSTEM_TYPE);
        }
        if (!StringHelper.isEmpty(LFSO_ED_FORECAST_FLAG) &&
        		("Y").equals(LFSO_ED_FORECAST_FLAG.toString())){
        	checkTypes.add(ErrorDetectionRule.FORECAST_TYPE);
        }
        if (!StringHelper.isEmpty(LFSO_ED_SCHEDULE_FLAG) &&
        		("Y").equals(LFSO_ED_SCHEDULE_FLAG.toString())){
        	checkTypes.add(ErrorDetectionRule.SCHEDULE_TYPE);
        }
        
        registry.filterConfiguredRules(checkTypes);
    	registry.setSkdgrpId(LFSO_ED_SKDGRP_ID.toString());
    	registry.setEmpgrpId(LFSO_ED_EMPGRP_ID.toString());
    	if (!StringHelper.isEmpty(LFSO_ED_START_DATE.toString())){
	    	registry.setEffDate(DateHelper.convertStringToDate(LFSO_ED_START_DATE.toString(), "yyyyMMdd hhmmss")); 
    	}
    	registry.setEffDate(DateHelper.getCurrentDate());
    	ErrorDetectionEngine ede = new ErrorDetectionEngine();
    	
    	try {
	    	ede.validate(registry);
    	} catch (Exception e){
    		logger.error(e);
    		throw new Exception("LFSO Error Detection Failed", e);
    	}
    	
    	List errorList = registry.getErrorDetectionList();
    	
    	if (!StringHelper.isEmpty(LFSO_ED_SHOW_APPLIED_FLAG) && ("Y").equals(LFSO_ED_SHOW_APPLIED_FLAG.toString())) {
	    	String[] bugRulesApplied = registry.getRegisteredRuleNameArray(ErrorDetectionRule.BUG_TYPE);
	    	String[] systemRulesApplied = registry.getRegisteredRuleNameArray(ErrorDetectionRule.SYSTEM_TYPE);
	    	String[] forecastRulesApplied = registry.getRegisteredRuleNameArray(ErrorDetectionRule.FORECAST_TYPE);
	    	String[] scheduleRulesApplied = registry.getRegisteredRuleNameArray(ErrorDetectionRule.SCHEDULE_TYPE);

%>
<tr>
    <td colspan=3>
        <h3><b>Rules Applied</b></h3>
    </td>
</tr> 
<tr>
    <td colspan=3>
        <i><%out.print(lh.getCaption("LFSO_ED_BUG_FLAG", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "Bug Checks:"));%></i>
    </td>
</tr> 
<%
    		for(int i=0; i<bugRulesApplied.length; i++){
%>
    		<tr>
    		<td width="10%"/>
    	    <td colspan=3>
    	        <%=bugRulesApplied[i]%>
    	    </td>
    		</tr> 
<%
    		}
    	
%>
<tr>
    <td colspan=3>
        <i><%out.print(lh.getCaption("LFSO_ED_SYSTEM_FLAG", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "System Environment:"));%></i>
    </td>
</tr> 
<%
    		for(int i=0; i<systemRulesApplied.length; i++){
%>
    		<tr>
    		<td width="10%"/>
    	    <td colspan=3>
    	        <%=systemRulesApplied[i]%>
    	    </td>
    		</tr> 
<%
    		}
    	
%>
<tr>
    <td colspan=3>
        <i><%out.print(lh.getCaption("LFSO_ED_FORECAST_FLAG", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "Forecast Generation:"));%></i>
    </td>
</tr> 
<%
    		for(int i=0; i<forecastRulesApplied.length; i++){
%>
    		<tr>
    		<td width="10%"/>
    	    <td colspan=3>
    	        <%=forecastRulesApplied[i]%>
    	    </td>
    		</tr> 
<%
    		}
    	
%>
<tr>
    <td colspan=3>
        <i><%out.print(lh.getCaption("LFSO_ED_SCHEDULE_FLAG", LocalizableTag.CONFIG_FIELD, Integer.parseInt(mfrmIdStr), "Schedule Generation:"));%></i>
    </td>
</tr> 
<%
    		for(int i=0; i<scheduleRulesApplied.length; i++){
%>
    		<tr>
    		<td width="10%"/>
    	    <td colspan=3>
    	        <%=scheduleRulesApplied[i]%>
    	    </td>
    		</tr> 
<%
    		}
    	}
%>
<tr>
    <td colspan=3>
        <h3><b>Results</b></h3>
    </td>
</tr> 

<%
        for(Iterator it = errorList.iterator(); it.hasNext();){
        		ErrorDetectionScriptResult edsr = (ErrorDetectionScriptResult) it.next();
        	String helpTitle;
            String helpTip;
            String helpDesc;
            String errMsg = "<not set>"; 	
        	
        	if ( !("Y").equals(LFSO_ED_FAILED_FLAG.toString()) || edsr.getMessage().compareTo("OK") != 0) {
%>


<!-------------------------------------------------------------------------------------------->
<!--Default Checks-->
<!-------------------------------------------------------------------------------------------->


<tr>
    <td width="10%"/>
    <wba:th>
        <wb:localize id="<%=edsr.getHelpTitleId()%>"><%=edsr.getHelpTitle()%></wb:localize>
<%
            helpTitle = edsr.getHelpTitle();
            helpTip = edsr.getHelpTip();
            helpDesc = edsr.getHelpDesc();
            errMsg = edsr.getErrorMsg();
%>
        <br><img src="/images/question_small.JPG"
             border=0
             alt="<%=helpTip%>"
             onclick="javascript:popHelpMsg( '<%=helpTitle%>', '<%=helpDesc%>' );"
        />
    </wba:th>
    <td>
<%
		    if(edsr.getMessage().compareTo("OK") == 0) {
				%><nobr><%=edsr.getMessage()%></nobr><%          
			}
		    else {
				%><textarea wrap=true class="failed"><%=errMsg + edsr.getMessage()%></textarea><%
			}
%>
    </td>
</tr>

<%
		}
	}
%>

</wba:table>
</wb:if> <!-- END IF SKDGRP ID IS NOT SET -->

</wb:page>