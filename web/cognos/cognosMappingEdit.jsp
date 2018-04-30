<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>
<%@ page import="com.wbiag.server.cognos.CreateCognosMapping"%>
<%@ page import="com.workbrain.app.reports.cognos.CognosHelper"%>

<%-- ********************** --%>


<wb:page maintenanceFormId='<%=request.getParameter("mfrm_id")%>' uiPathName='Details' showUIPath='true' enableFloaters="true">

<wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/cognos/createCognosMapping.jsp"/></wb:define>
<wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>

<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<wb:submit id="OPERATION">SUBMIT</wb:submit>

<wb:define id="REPORTNAME"><wb:get id="REPORTNAME" scope="parameter" default=""/></wb:define>
<wb:define id="REPORTDESCRIPTION"><wb:get id="REPORTDESCRIPTION" scope="parameter" default=""/></wb:define>
<wb:define id="REPORTPATH"><wb:get id="REPORTPATH" scope="parameter" default=""/></wb:define>   
<wb:define id="MENUPARENT"><wb:get id="MENUPARENT" scope="parameter" default=""/></wb:define>        
<wb:define id="OVERWRITE"><wb:get id="OVERWRITE" scope="parameter" default=""/></wb:define>

<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default=""/></wb:define>
<wb:define id="mfrm_name"/>
<wb:define id="cgmapmfrm_id"><wb:get id="cgmapmfrm_id" scope="parameter" default=""/></wb:define>
<wb:define id="cgmap_id"><%=request.getParameter("CGMAP_ID")%></wb:define>
<wb:define id="del_detail"><%=request.getParameter("DELETEDETAIL")%></wb:define>

<wb:sql createDataSource="maintForm"> SELECT * FROM MAINTENANCE_FORM WHERE MFRM_ID = <wb:get id="mfrm_id"/> </wb:sql>
<wb:dataSet dataSource="maintForm">
  <wb:set id="mfrm_name"><wb:getDataFieldValue name="MFRM_NAME"/></wb:set>
</wb:dataSet>

<script language='JavaScript'>
   var mappingPath = new Array();
   var mfrmName = new Array();

  function checkSubmit(){
      disableAllButtons();
      document.page_form.submit();        
   } 

</script>         

    <%
    int mfrmId = valueToInt(mfrm_id);
    String mfrmIdStr = String.valueOf(mfrm_id);
    int cgmapmfrmId = valueToInt(cgmapmfrm_id);
    int cgmapId = valueToInt(cgmap_id);
    String cgmapName = REPORTNAME.toString();
    String cgmapDesc = REPORTDESCRIPTION.toString();
    String cgmapRptPath = REPORTPATH.toString();
    String action = request.getParameter("action");
    
    boolean validCGMapPath = true;
    boolean validMFrmName = true;

    String redirectUrl = "/maintenance/mntForms.jsp?mfrm_id=" + mfrmIdStr + "&uiPathLabel=" + response.encodeUrl("Cognos Mapping");
    String createNewUrl ="/cognos/cognosMappingEdit.jsp?mfrm_id=" + mfrmIdStr + "&action=NEW&uiPathLabel=New%20Record";

    CreateCognosMapping ccm = new CreateCognosMapping(JSPHelper.getConnection(request)); 
    ccm.setCgmapId(cgmapId);
    ccm.setCgmapName(REPORTNAME.toString());
    ccm.setCgmapDesc(REPORTDESCRIPTION.toString());
    ccm.setCgmapRptPath(REPORTPATH.toString());
    ccm.setMfrmId(cgmapmfrmId);
    ccm.setMfrmMenuParentId(valueToInt(MENUPARENT));

    if (cgmapmfrmId <= 0 && "EDIT".equals(action)) {
      cgmapmfrmId = ccm.getMfrmIdByCgmapId(cgmapId);
    }
    ccm.setMfrmId(cgmapmfrmId);
    
    if("SUBMIT".equals(OPERATION.toString())) {
    	boolean errorFlag = false;

    	// validate form values
    	if (StringHelper.isEmpty(REPORTNAME) || StringHelper.isEmpty(REPORTDESCRIPTION) || 
    	    StringHelper.isEmpty(REPORTPATH) || StringHelper.isEmpty(MENUPARENT)) {
    	  errorFlag = true;
    	}
    	
    	//validCGMapPath = !ccm.checkCGMapPathExists(cgmapId, REPORTPATH.toString());
    	validMFrmName = !ccm.checkMFrmNameExists(cgmapmfrmId, REPORTNAME.toString());
    	
    	try {
    	  // create or update submitted form  
          //if (validCGMapPath && validMFrmName) {
          if (validMFrmName) {

            if ("NEW".equals(action) && !errorFlag) {
              ccm.buildCognosMapper();
              action = "EDIT";
            } else if ("Y".equals(del_detail.toString())) {
              ccm.deleteCognosMapper();
              response.sendRedirect(redirectUrl);
            } else if ("EDIT".equals(action) && !errorFlag) {
              ccm.updateCognosMapper();
              ccm.loadCGMapById(cgmapId);
            }
          } else {
      	    errorFlag = true;
          }
          
        } catch(SQLException sqle)  {
                errorFlag = true;
    %>
                <span>Unable to edit Cognos Mapping.  The following error occured:</span>
                <P><%=sqle%></P>
                <wba:button label="Try again" intensity="low" onClick="#okOnClickUrl#"/>

    <%  }

        if(errorFlag == false) {
        %>
	    <input type="hidden" name="STATUS_MESSAGE_HIDDEN" value='<wb:localize id="MFRM_VALUE_CHANGES_SAVED">Your changes have been saved</wb:localize>'>
            <script>printPageActionStatus(window.document.page_form.STATUS_MESSAGE_HIDDEN);</script>

    <%  }
    } else {

        //If the maintenance form ID is null, retrieve it from the database.
        if (mfrmId == 0)
        {
          PreparedStatement ps3 = null;
          ResultSet rs3 = null;
          try {
            DBConnection dbc = JSPHelper.getConnection(request);
            ps3 = dbc.prepareStatement( "SELECT * FROM maintenance_form WHERE mfrm_jsp = ?");
            ps3.setString(1,"/cognos/createCognosMapping.jsp");
            rs3 = ps3.executeQuery();
            
            if (rs3.next()) {
              mfrmId = rs3.getInt("mfrm_id");
            }
          } finally {
            SQLHelper.cleanUp(ps3, rs3);
          }
        }

    	if (ccm.getCgmapId() > 0 && !ccm.isLoaded()) {
      	  ccm.loadCGMapById(cgmapId);
    	}

     %>

  <%}%>

<div class="contentTableTitle"><wb:localize id="CreateCognosMapping">Cognos Mapping Detail</wb:localize></div>
<div class='contentTableSubHeading'>
  <img src='/images/interface/arrowrightgreen9x9.gif' width=9 height=9 border=0 alt=''>&nbsp;<a href="<%=createNewUrl%>">Create New Entry</a>&nbsp;&nbsp;&nbsp;
  <img src='/images/interface/arrowrightgreen9x9.gif' width=9 height=9 border=0 alt=''>&nbsp;<a href="<%=redirectUrl%>">Return to form listing</a>
</div>
<br style='clear:left'>

<wba:table>
	<% if ("EDIT".equals(action)) { %>
        <wba:tr>
            <wba:th width='30%'> 
                <wb:localize id="del">Del</wb:localize>
            </wba:th>
            <wba:td width='70%'>
                <wb:controlField cssClass="inputField" id='DeleteDetail' ui="CheckboxUI" submitName="DELETEDETAIL"/>
            </wba:td>
        </wba:tr>
	<% } %>
        <wba:tr>
            <wba:th width='30%'> 
                <wb:localize id="Report_Name" overrideId="<%=mfrmIdStr%>" type="field">Report Name </wb:localize>
            </wba:th>
            <wba:td width='70%'>
                <wb:controlField cssClass="inputField" id='Report_Name' ui="StringUI" uiParameter="width=50" overrideId="<%=mfrmIdStr%>" submitName="REPORTNAME"><%=ccm.getCgmapName()%></wb:controlField>
		<%if ("SUBMIT".equals(OPERATION.toString())){%>
		  <%if (StringHelper.isEmpty(REPORTNAME)) {%>
		    <BR><span class='textAlert'><wb:localize id="MFRM_VALUE_REQUIRED">Value is required</wb:localize></span>
		  <%}else if (!validMFrmName){%>  
		    <BR><span class='textAlert'><wb:localize id="MFRM_VALUE_MUST_BE_UNIQUE">Value must be unique</wb:localize></span>
		  <%}%>
		<%}%>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='30%'> 
                <wb:localize id="Report_Description" overrideId="<%=mfrmIdStr%>" type="field">Report Description </wb:localize>
            </wba:th>
            <wba:td width='70%'>
                <wb:controlField cssClass="inputField" id='Report_Description' ui="TextAreaUI" uiParameter="width=37 height=5 length=5000" overrideId="<%=mfrmIdStr%>" submitName="REPORTDESCRIPTION"><%=ccm.getCgmapDesc()%></wb:controlField>
		<%if ("SUBMIT".equals(OPERATION.toString()) && StringHelper.isEmpty(REPORTDESCRIPTION)){%><BR><span class='textAlert'><wb:localize id="MFRM_VALUE_REQUIRED">Value is required</wb:localize></span><%}%>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='30%'> 
                <wb:localize id="Report_Path" type="field">Report Path </wb:localize>	
            </wba:th>
            <wba:td width='70%'>
                <wb:controlField cssClass="inputField" id='Report_Path' ui="StringUI" uiParameter="width=50" overrideId="<%=mfrmIdStr%>" submitName="REPORTPATH"><%=ccm.getCgmapRptPath()%></wb:controlField>
		<%if ("SUBMIT".equals(OPERATION.toString())){%>
		  <%if (StringHelper.isEmpty(REPORTPATH)) {%>
		    <BR><span class='textAlert'><wb:localize id="MFRM_VALUE_REQUIRED">Value is required</wb:localize></span>
		  <%}%>
		<%}%>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='30%'> 
                <wb:localize id="Menu_Parent" type="field">Menu Parent </wb:localize>	
            </wba:th>
            <wba:td width='70%'>
                <wb:controlField cssClass="inputField" id='Menu_Parent' ui="DBLookupUI" uiParameter="sourceType=SQL source='SELECT MFRM_ID, MFRM_LOC_NAME, MFRM_ID MFRM_ID_DISPLAY FROM VL_MAINTENANCE_FORM, (SELECT MFRM_ID VL_MFRM_ID FROM VIEW_MOD_MAINTENANCE_FORM) X where MFRM_ID = VL_MFRM_ID' title='LOOKUP_MENU_PARENT'" overrideId="<%=mfrmIdStr%>" submitName="MENUPARENT"><%=ccm.getMfrmMenuParentId()%></wb:controlField>
		<%if ("SUBMIT".equals(OPERATION.toString()) && StringHelper.isEmpty(MENUPARENT)){%><BR><span class='textAlert'><wb:localize id="MFRM_VALUE_REQUIRED">Value is required</wb:localize></span><%}%>
            </wba:td>
        </wba:tr>
        
        
        <input type='hidden' name='action' value="<%=action%>">
        <input type='hidden' name='mfrm_id' value="<%=mfrmIdStr%>">
        <input type='hidden' name='CGMAP_ID' value="<%=ccm.getCgmapId()%>">
        <input type='hidden' name='CGMAP_MFRM_ID' value="<%=ccm.getMfrmId()%>">
        <input type='hidden' name='uiPathLabel' value="Details">
        
    </wba:table>


    <wba:button label='Save' labelLocalizeIndex='Save' onClick='checkSubmit();'/>
    


</wb:page>

<%!
   public int valueToInt(Object o) {
     if (o != null && StringHelper.isNumber(o.toString())) {
       return Integer.parseInt(o.toString());
     }
     return -99;
   }

%>