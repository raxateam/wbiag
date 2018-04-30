<%@ include file="/system/wbheader.jsp"%>
<%@ page import="com.workbrain.app.scheduler.*" %>
<%@ page import="com.workbrain.server.jsp.*" %>
<%@ page import="com.workbrain.sql.*" %>
<%@ page import="com.workbrain.util.*" %>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.export.payroll.PayrollExportTask"%>
<%@ page import="com.workbrain.app.scheduler.*"%>
<%@ page import="com.workbrain.app.scheduler.ejb.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.sql.*"%>
<%@ page import="org.apache.log4j.*"%>


<wb:page maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>
    <INPUT TYPE=HIDDEN NAME='COMMAND' VALUE='CANCEL'>
    <wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/interface/home.jsp"/></wb:define>
    <wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
    <wb:define id="none"><wb:localize id="None" ignoreConfig="true">None</wb:localize></wb:define>

    <%-- LOGIC ------------------------------------------------------ --%>
    <%
        DateFormat df = new SimpleDateFormat(PayrollExportTask.DATE_FORMAT_STR);
        boolean isPostBack          = request.getParameter("COMMAND")!=null;
        String taskId               = request.getParameter("TASK_ID");
        String payGroupIdString     = request.getParameter("PAYGRP_ID");
        String startDateString      = request.getParameter("START_DATE");
        String endDateString        = request.getParameter("END_DATE");
        String error                = null;
        String petIdString          = request.getParameter("PET_ID");
        boolean writeToFile         = request.getParameter("WRITE_TO_FILE")!=null;
        boolean mergeFiles          = request.getParameter("MERGE_FILES")!=null;
        boolean writeToStagingTable = request.getParameter("WRITE_TO_STAGING_TABLE")!=null;
        boolean adjustDates         = request.getParameter("ADJUST_DATES")!=null;
        boolean useCurrentPP 	     = request.getParameter("USE_CURRENT_PP")!=null;


        if(!isPostBack){
            Scheduler scheduler = (Scheduler)Registry.getVar ("/services/Scheduler");
            ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(taskId));

            Map params = rec.getParam();
            try{
                petIdString         = (String) params.get(PayrollExportTask.PAY_EXP_TSK_ID_PARAM);
                payGroupIdString    = (String) params.get(PayrollExportTask.PAY_GRP_ID_PARAM);
                startDateString     = (String) params.get(PayrollExportTask.FROM_DATE_PARAM);
                endDateString       = (String) params.get(PayrollExportTask.TO_DATE_PARAM);

                writeToStagingTable = Boolean.valueOf((String) params.get(
                        PayrollExportTask.WRITE_TO_TABLE_PARAM)).booleanValue();
                writeToFile         = Boolean.valueOf((String) params.get(
                        PayrollExportTask.WRITE_TO_FILE_PARAM)).booleanValue();
                mergeFiles          = Boolean.valueOf((String) params.get(
                        PayrollExportTask.MERGE_FILES_PARAM)).booleanValue();
                adjustDates         = Boolean.valueOf((String) params.get(
                        PayrollExportTask.ADJUST_DATES_PARAM)).booleanValue();
            } catch(Exception e){
                petIdString=payGroupIdString=startDateString=endDateString= "";
                writeToStagingTable = writeToFile = adjustDates = false;
            }
        } else {
            if("SUBMIT".equals(request.getParameter("COMMAND"))){


                try{
                    if(StringHelper.isEmpty(petIdString))      throw new Exception("Provide Export Type");
                    if(StringHelper.isEmpty(payGroupIdString)) throw new Exception("Provide paygroup");

                    if(!StringHelper.isEmpty(startDateString)){
                        df.parse(startDateString);
                    }

                    if(!StringHelper.isEmpty(endDateString)){
                        df.parse(endDateString);
                    }
                        //Create ExportProcessor and process export
                        DBConnection conn = JSPHelper.getConnection(request);
                        int petId = Integer.valueOf(petIdString).intValue()
                        String[] st = StringHelper.detokenizeString(payGroupIdString , ",");
                        int[] stArray = new int[st.length];
                        for (int i = 0; i < st.length; i++) {
                          stArray[i] = Integer.parseInt(st[i]);
                        }
                       int[] paygrpIds = stArray;
                       Date startDate = DateHelper.parseDate(startDateString, PayrollExportTask.DATE_FORMAT_STR);
                       Date endDate = DateHelper.parseDate(endDateString, PayrollExportTask.DATE_FORMAT_STR);

                       String filePath;
                       String fileName;
                       String fileExt;

                       PayrollExportProcessor processor = new PayrollExportProcessor();

                       processor.setPetId(petId);
                       processor.setPayGrpIds(paygrpIds);
                       if (startDate != null) {
                           processor.setFromDate(startDate);
                       }
                       if (endDate != null) {
                           processor.setToDate(endDate);
                       }
                       processor.setWriteToTable(writeToTable);
                       processor.setWriteToFile(writeToFile);
                       processor.setMergeFiles(mergeFiles);
                       processor.setAdjustDates(adjustDates);
                       // petXML, filename and directory
                       PreparedStatement ps = null;
                       ResultSet rs = null;
                       try {
                           String petSql = "SELECT * FROM payroll_export_tsk WHERE pet_id = ?";
                           ps = conn.prepareStatement(petSql);
                           ps.setInt(1, petId);
                           rs = ps.executeQuery();
                           if (rs.next()) {
                               Clob clob = rs.getClob("pet_xml");
                               String xml = clob.getSubString(1L, (int) clob.length());
                               processor.setPetXml( xml );
                               filePath = rs.getString("PET_OUT_FILE_PATH");
                               fileName = rs.getString("PET_OUT_FILE_MASK");
                               fileExt = rs.getString("PET_OUT_FILE_EXT");
                               System.out.println("Setting file info " + filePath + fileName + fileExt );
                               processor.setPetPath(filePath);
                               processor.setPetMask(fileName);
                               processor.setPetExt(fileExt);
                           }
                           else {
                               throw new RuntimeException ("Payroll export task id not found : " + petId);
                           }
                       }
                       finally {
                           if (ps != null) ps.close();
                           if (rs != null) rs.close();
                       }
                       processor.process(conn , null);


                }catch(Exception e){
                    logger.error("payrollExport.jsp",e);
                    error = e.getMessage();
                }
            }
            if(error==null){
                %>
                <wb:forward page='#GO_TO#' />
                <%
            }
        }

%>

    <INPUT TYPE=HIDDEN NAME='TASK_ID' VALUE='<%=taskId%>'>
    <%-- RENDERING ------------------------------------------------------ --%>

        <wb:define id="TaskNames"><%=names.toString()%></wb:define>
    <wb:define id="uiParms"/>
    <wb:define id="startDateValue"/>
    <wb:define id="endDateValue"/>
    <wb:define id="defaultValue"/>

    <wba:table caption="Payroll Export" >
    <tr>
        <wba:th><wb:localize id="EXPORT_TYPE" >Export Type</wb:localize></wba:th>
        <td>
            <wb:controlField submitName='PET_ID' id='PET_ID'
                cssClass="inputField"
                ui='DBLookupUI'
                uiParameter="multiChoice=false sourceType=SQL source='SELECT pet_id, pet_name, pet_desc FROM payroll_export_tsk' all=false labelFieldStatus=edit"
            ><%=petIdString%></wb:controlField>
        </td>
    </tr>
    <tr>
        <wba:th><wb:localize id="PAYGRP_ID" type="field" overrideId='205'>Pay Group</wb:localize></wba:th>
        <td>
            <wb:controlField
                submitName='PAYGRP_ID'
                id='PAYGRP_ID'
                overrideId="205"
                cssClass="inputField"
                ui='DBLookupUI'
                uiParameter="multiChoice=true sourceType=SQL source='SELECT PAYGRP_ID, PAYGRP_NAME, PAYGRP_DESC FROM PAY_GROUP' all=false labelFieldStatus=edit"
             ><%=payGroupIdString%></wb:controlField>
        </td>
    </tr>
    <tr>
        <wba:th><wb:localize id="Start_Date" type="field" overrideId="205">Start Date</wb:localize></wba:th>
        <td>
            <wb:controlField id="Start_Date"    overrideId="205" submitName='START_DATE'
            onChange="" cssClass="inputField" ><%=startDateString%></wb:controlField>
        </td>
    </tr>
    <tr>
        <wba:th><wb:localize id="End_Date" overrideId='205' type='field'>End Date</wb:localize></wba:th>
        <td>
            <wb:controlField id='End_Date' submitName='END_DATE'
            onChange="" cssClass="inputField" overrideId='205'><%=endDateString%></wb:controlField>
        </td>
    </tr>
    <tr>
                <wba:th>&nbsp;</wba:th>
      <td>
        <wb:secureContent securityName='USE_CURRENT_PP'>
        <input type=checkbox name='USE_CURRENT_PP' <%=useCurrentPP?"checked":""%> >
            <wb:localize id="use_current_pay_period" >
                 Use the Current pay period (Will not use start and end dates when checked)
        </wb:localize>
        </wb:secureContent>
      </td>
    </tr>
    <tr>
    <wba:th>&nbsp;</wba:th>
        <td>
        <wb:secureContent securityName='WRITE_TO_STAGING_TABLE'>
        <input type=checkbox name='WRITE_TO_STAGING_TABLE'  <%=writeToStagingTable?"checked":""%> >
            <wb:localize id="write_to_staging_table" >Write to Staging Table</wb:localize>
        </wb:secureContent>
        </td>
    </tr>
    <tr>
    <wba:th>&nbsp;</wba:th>
    <td>
        <wb:secureContent securityName='WRITE_TO_FILE'>
        <input type=checkbox name='WRITE_TO_FILE'  <%=writeToFile?"checked":""%> >
            <wb:localize id="write_to_file" >Write to File</wb:localize>
        </wb:secureContent>
    </td>
    </tr>
    <tr>
    <wba:th>&nbsp;</wba:th>
    <td>
        <wb:secureContent securityName='MERGE_FILES'>
        <input type=checkbox name='MERGE_FILES'  <%=mergeFiles?"checked":""%> >
            <wb:localize id="merge_files" >Merge Files</wb:localize>
        </wb:secureContent>
    </td>
    </tr>
    <tr>
    <wba:th>&nbsp;</wba:th>
    <td>
        <wb:secureContent securityName='ADJUST_DATES'>
        <input type=checkbox name='ADJUST_DATES' <%=adjustDates?"checked":""%> >
            <wb:localize id="auto_adjust_payroll_dates" >
                 Automatically adjust payroll payroll dates to the next pay period
        </wb:localize>
        </wb:secureContent>
    </td>
    </tr>
  </wba:table>

        <wba:button label="Go" labelLocalizeIndex="Submit"
             onClick="document.forms[0].COMMAND.value='SUBMIT'; document.forms[0].submit(); "
        />

        <wba:button label="Cancel" labelLocalizeIndex="Cancel"
             onClick="document.forms[0].submit(); "
        />


    <BR>
    <BR>
    <div color=red>
       <%=error==null?"":error%>
    </div>

    <%--
    <div class=separatorSmall />
        <wba:button label="Go" labelLocalizeIndex="Submit"
        onClick="validateFormFields('setSubmit()'); return false; "
    />
    --%>
</wb:page>
