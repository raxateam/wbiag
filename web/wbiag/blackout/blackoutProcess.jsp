<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*,com.workbrain.server.jsp.taglib.util.*"%>
<%@ page import="com.workbrain.server.data.sql.*,com.workbrain.server.data.*,com.workbrain.server.data.ui.*,com.workbrain.server.data.type.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>
<%@ page import="com.wbiag.app.modules.blackout.*"%>
<%-- ********************** --%>
<%!
    private static final String MODE_ADD = "ADD";
    private static final String MODE_DELETE = "DELETE";

%>
<wb:page login='true'>

<script language="JavaScript">
  function checkSubmit(){
      if (document.page_form.BBY_BLKDGRP_NAME.value == "") {
          alert("Blackout Group Name can't be empty");
          return false;
      }
      if (document.page_form.BBY_BLKDGRP_DESC.value == "") {
          alert("Blackout Group Description can't be empty");
          return false;
      }
      if (document.page_form.BBY_BLKDGRP_START_DATE.value == "") {
          alert("Blackout Group Start Date can't be empty");
          return false;
      }
      if (document.page_form.WBT_ID.value == "") {
          alert("Blackout Group Team can't be empty");
          return false;
      }

      if (document.page_form.WBT_SUB_CHOICE[1].checked
          && document.page_form.WBT_SUB.value == "") {
          alert("At least one sub team needs to be selected when (Include Only These Subteams) is selected");
          return false;
      }

      document.page_form.OPERATION.value  = "SUBMIT";
      disableAllButtons();
      document.page_form.submit();
  }
</script>
    <wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" /></wb:define>
    <wb:submit id="mfrm_id"><wb:get id="mfrm_id"/></wb:submit>
    <wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>

    <%
    String mfrmIdStr = mfrm_id.toString();
    if (StringHelper.isEmpty(mfrmIdStr)) {
        throw new RuntimeException("mfrm_id must be supplied in querystring, check mfrm_parameter_jps setting for this maintenance form");
    }
    String goBack =  "location.href = '" + request.getContextPath() + "/maintenance/mntForms.jsp?mfrm_id=" + mfrmIdStr + "'; return false;";

    String MODE = request.getParameter("MODE");
    if (StringHelper.isEmpty(MODE)) {
        MODE = MODE_ADD;
    }
    if (MODE_DELETE.equals(MODE)) {
        BBYBlkoutAccess blkAccess = new BBYBlkoutAccess(JSPHelper.getConnection(request));
        String recordId =  request.getParameter("record_id");
        if (StringHelper.isEmpty(recordId)) {
            throw new RuntimeException("record_id must be supplied, check mfrm_parameter_jps setting for this maintenance form");
        }
        blkAccess.deleteBlackoutDateGroup(Integer.parseInt(recordId)) ;
        %>
            <span>Blackout Group has been deleted successfully.</span>
            <BR><wba:button label="OK" size="small" intensity="low" onClick="<%=goBack%>"/>
            <wb:stop/>
        <%

    }
    int mfrmId = Integer.parseInt(mfrmIdStr);
    String BBY_BLKDGRP_NAME = request.getParameter("BBY_BLKDGRP_NAME");
    String BBY_BLKDGRP_DESC = request.getParameter("BBY_BLKDGRP_DESC");
    String BBY_BLKDGRP_START_DATE = request.getParameter("BBY_BLKDGRP_START_DATE");
    String BBY_BLKDGRP_END_DATE = request.getParameter("BBY_BLKDGRP_END_DATE");
    String WBT_ID = request.getParameter("WBT_ID");
    String WBTT_ID = request.getParameter("WBTT_ID");
    String JOB_ID = request.getParameter("JOB_ID");
    String WBT_SUB_CHOICE= request.getParameter("WBT_SUB_CHOICE");
    String WBT_SUB= request.getParameter("WBT_SUB");
    LocalizationHelper lh = new LocalizationHelper(pageContext);

    if("SUBMIT".equals(OPERATION.toString()))
    {
        boolean flag = true;
        BBYBlkoutAccess blkAccess = new BBYBlkoutAccess(JSPHelper.getConnection(request));
        try
        {
            BBYBlkoutDateGrpData grpData = new   BBYBlkoutDateGrpData();
            grpData.setBbyBlkdgrpName(BBY_BLKDGRP_NAME);
            grpData.setBbyBlkdgrpDesc(BBY_BLKDGRP_DESC);
            if (!StringHelper.isEmpty(BBY_BLKDGRP_START_DATE)) {
                grpData.setBbyBlkdgrpStartDate(DateHelper.convertStringToDate(BBY_BLKDGRP_START_DATE,"yyyyMMddHHmmss"));
            }
            if (!StringHelper.isEmpty(BBY_BLKDGRP_END_DATE)) {
                grpData.setBbyBlkdgrpEndDate(DateHelper.convertStringToDate(BBY_BLKDGRP_END_DATE,"yyyyMMddHHmmss"));
            }
            if (!StringHelper.isEmpty(WBT_ID)) {
                grpData.setWbtId(new Integer(WBT_ID));
            }
            if (!StringHelper.isEmpty(WBTT_ID)) {
                grpData.setWbttId(new Integer(WBTT_ID));
            }
            if (!StringHelper.isEmpty(JOB_ID)) {
                grpData.setJobId((new Integer(JOB_ID)));
            }
            int[] subWbtIds = null;
            if ("ONLY".equals(WBT_SUB_CHOICE)) {
                subWbtIds = StringHelper.detokenizeStringAsIntArray(WBT_SUB, ",", true);
            }
            blkAccess.insertBlackoutDateGroup(grpData, subWbtIds, true);
        }
        catch(Exception e)
        {
            flag = false;
    %>
        <span>Blackout Group : <%=BBY_BLKDGRP_NAME%> was not created.  The following error occured:</span>
        <P><%=e%></P>

    <%  }

        if(flag)
        {
        %>
            <span>Blackout Group : <%=BBY_BLKDGRP_NAME%> has been created successfully.</span>
            <BR><wba:button label="OK" size="small" intensity="low" onClick="<%=goBack%>"/>

        <%
        }
    }

    if (MODE_ADD.equals(MODE)) {
    %>

    <wba:table caption="" captionLocalizeIndex="Add_Blackout_Date_Group">
	<%
	out.print("<tr><td height='60px' width='40%' style='font-size:17pt;background-color:#D1CEFD;font-color:#BCBCBC;'>");
	out.print("Blackout Entry Form");
	out.print("</td></tr>");
	%>


	</wba:table><p><p>
	
	<wba:table caption="" captionLocalizeIndex="Add_Blackout_Date_Group">
	
<%
    out.print("<tr><th width='40%'>");
    out.print(lh.getCaption("BBY_BLKDGRP_NAME", LocalizableTag.CONFIG_FIELD, mfrmId, "Name"));
    out.print("</th><td width='60%'>");
    out.print(lh.getUI("BBY_BLKDGRP_NAME", "BBY_BLKDGRP_NAME", "", null, "edit", mfrmId));
    out.print("</td></tr>");

    out.print("<tr><th width='40%'>");
    out.print(lh.getCaption("BBY_BLKDGRP_DESC", LocalizableTag.CONFIG_FIELD, mfrmId, "Description"));
    out.print("</th><td width='60%'>");
    out.print(lh.getUI("BBY_BLKDGRP_DESC", "BBY_BLKDGRP_DESC", "", null, "edit", mfrmId));
    out.print("</td></tr>");

    out.print("<tr><th width='40%'>");
    out.print(lh.getCaption("BBY_BLKDGRP_START_DATE", LocalizableTag.CONFIG_FIELD, mfrmId, "Start Date *"));
    out.print("</th><td width='60%'>");
    out.print(lh.getUI("BBY_BLKDGRP_START_DATE", "BBY_BLKDGRP_START_DATE", "", null, "edit", mfrmId));
    out.print("</td></tr>");

    out.print("<tr><th width='40%'>");
                out.print(lh.getCaption("BBY_BLKDGRP_END_DATE", LocalizableTag.CONFIG_FIELD, mfrmId, "End Date "));
    out.print("</th><td width='60%'>");
    out.print(lh.getUI("BBY_BLKDGRP_END_DATE", "BBY_BLKDGRP_END_DATE", "", null, "edit", mfrmId));
    out.print("</td></tr>");

    out.print("<tr><th width='40%'>");
    out.print(lh.getCaption("WBT_ID", LocalizableTag.CONFIG_FIELD, mfrmId, "Team *"));
    out.print("</th><td width='60%'>");
    out.print(lh.getUI("WBT_ID", "WBT_ID", "", null, "edit", mfrmId));
    out.print("</td></tr>");

    out.print("<tr><td width='40%'>");
%>
              <input type='radio' name='WBT_SUB_CHOICE' value='ALL' checked><wb:localize id='Include_All_Subteams'>Include All Departments</wb:localize>
<%
    out.print("</td><td width='60%'>");
%>
              <input type='radio' name='WBT_SUB_CHOICE' value='ONLY' default><wb:localize id='Include_Only_These_Subteams'>Include Only These Departments</wb:localize>
            <wb:controlField cssClass='inputField' submitName="WBT_SUB" ui='DBLookupUI'
               uiParameter="multiChoice=true sourceType=SQL
               sourceDisplayFields='SKDGRP_NAME'
               source='SELECT CHILD_WBT_ID, SKDGRP_NAME,PARENT_WBT_ID FROM SEC_WB_TEAM_CHILD_PARENT WTCP, SO_SCHEDULE_GROUP SG WHERE SG.WBT_ID = WTCP.CHILD_WBT_ID AND SKDGRP_INTRNL_TYPE=11'
               slaveInputFields=WBT_SUB masterInputFields=[WBT_ID] masterDataFields=[parent_wbt_id]
               "></wb:controlField>
<%
    out.print("</td></tr>");

    out.print("<tr><th width='40%'>");
    out.print(lh.getCaption("WBTT_ID", LocalizableTag.CONFIG_FIELD, mfrmId, "Applies to Team Type"));
    out.print("</th><td width='60%'>");
    out.print(lh.getUI("WBTT_ID", "WBTT_ID", "", null, "edit", mfrmId));
    out.print("</td></tr>");

    //out.print("<tr><th width='40%'>");
    //out.print(lh.getCaption("JOB_ID", LocalizableTag.CONFIG_FIELD, mfrmId, "Job"));
    //out.print("</th><td width='60%'>");
    //out.print(lh.getUI("JOB_ID", "JOB_ID", "", null, "edit", mfrmId));

    out.print("</td></tr>");

    for (int k = 1; k <=5; k++) {
        String ufName = "BBY_BLKDGRP_UDF" + k;
        System.out.println(ufName + "-" + lh.isControlVisible(ufName, mfrmId));
        if (lh.isControlVisible(ufName, mfrmId)) {
           out.print("<tr><th width='40%'>");
           out.print(lh.getCaption(ufName, LocalizableTag.CONFIG_FIELD, mfrmId, ufName));
           out.print("</th><td width='60%'>");
           out.print(lh.getUI(ufName, ufName, "", null, "edit", mfrmId));
           out.print("</td></tr>");
        }
    }

    for (int k = 1; k <=5; k++) {
        String ufName = "BBY_BLKDGRP_FLAG" + k;
        System.out.println(ufName + "-" + lh.isControlVisible(ufName, mfrmId));
        if (lh.isControlVisible(ufName, mfrmId)) {
           out.print("<tr><th width='40%'>");
           out.print(lh.getCaption(ufName, LocalizableTag.CONFIG_FIELD, mfrmId, ufName));
           out.print("</th><td width='60%'>");
           out.print(lh.getUI(ufName, ufName, "", null, "edit", mfrmId));
           out.print("</td></tr>");
        }
    }

%>
    </wba:table>

    <wba:table caption="" captionLocalizeIndex="">
	<%
	out.print("<tr><td height='100px' >");
	out.print("<B>Instructions for Black Date</B><br>");

	out.print("<TEXTAREA NAME='' ROWS='11' COLS='80'>");
	out.println("1. Insert New Blackout Date:"); 
	out.println("&nbsp;*Mandated Fields: Names, Start Date, End Date.");
	out.println("&nbsp;*Optional Fields: Location Type, Location #, Department. If these fields are empty then system will assume the Blackout Date will be for all Locations and all Departments.");
	out.println("&nbsp;*Click on Add Button to add.");
	out.println("2. Delete the Blackout Date:");
	out.println("&nbsp;*Select the Blackout Date that you want to delete by select the check box.");
	out.println("&nbsp;*Click on Delete Button to delete.");
	out.print("</TEXTAREA>");
	out.print("</td></tr>");
	%>
	</wba:table>



<% } %>
    <input type='hidden' name='OPERATION'>
    <wba:button label='Submit' onClick='checkSubmit();'/>
    <wba:button label="Cancel" onClick="<%=goBack%>"/>
</wb:page>