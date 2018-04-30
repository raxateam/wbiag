<%@ include file="/system/wbheader.jsp" %>
<%@ page import="com.workbrain.app.scheduler.*" %>
<%@ page import="com.workbrain.app.ta.db.*" %>
<%@ page import="com.workbrain.security.SecurityService" %>
<%@ page import="com.workbrain.server.registry.*" %>
<%@ page import="com.workbrain.server.jsp.*" %>
<%@ page import="com.workbrain.util.*" %>
<%@ page import="com.workbrain.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.wbiag.app.ta.ruleengine.*" %>

<wb:page login='true' maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>
    <wb:define id="dstFallbackChangeDatetime"><wb:get id="dstFallbackChangeDatetime" scope="parameter" default=""/></wb:define>
    <wb:define id="dstSpringforwardChangeDatetime"><wb:get id="dstSpringforwardChangeDatetime" scope="parameter" default=""/></wb:define>
    <wb:define id="employeeWhereClause"><wb:get id="employeeWhereClause" scope="parameter" default=""/></wb:define>
    <wb:define id="ovrWbuName"><wb:get id="ovrWbuName" scope="parameter" default=""/></wb:define>
    <wb:define id="deletesDstAdjustmentOverrides"><wb:get id="deletesDstAdjustmentOverrides" scope="parameter" default=""/></wb:define>
    <wb:define id='operation'><wb:get id='operation' scope='parameter' default=''/></wb:define>

<%
    if ( "submit".equalsIgnoreCase( operation.toString() ) ) {
%>
      <wba:table width='400'>
         <wba:tr><wba:td><wb:localize id='DSTSchedule_Start'>DST Schedule Adjustment for Current Client Started ...</wb:localize></wba:td></wba:tr>
      </wba:table>
      <br>

<%
        out.flush();
        response.flushBuffer();

        DBConnection connCore = JSPHelper.getConnection( request );

        DSTScheduleAdjustment.DSTScheduleAdjustmentContext ctx = new DSTScheduleAdjustment.DSTScheduleAdjustmentContext();

        ctx.setDBConnection(connCore);
        ctx.setClientId(SecurityService.getCurrentClientId());

        Date dDstFallbackChangeDatetime = null;
        if (!StringHelper.isEmpty(dstFallbackChangeDatetime)) {
            dDstFallbackChangeDatetime = DSTScheduleAdjustment.convertStringToDate(dstFallbackChangeDatetime.toString() ,
                DSTScheduleAdjustment.PROP_DATE_FORMAT);
        }
        ctx.setDstFallbackChangeDatetime(dDstFallbackChangeDatetime);

        Date dDstSpringforwardChangeDatetime = null;
        if (!StringHelper.isEmpty(dstSpringforwardChangeDatetime)) {
            dDstSpringforwardChangeDatetime = DSTScheduleAdjustment.convertStringToDate(dstSpringforwardChangeDatetime.toString() ,
                DSTScheduleAdjustment.PROP_DATE_FORMAT);
        }
        ctx.setDstSpringforwardChangeDatetime(dDstSpringforwardChangeDatetime);

        ctx.setEmployeeWhereClause(employeeWhereClause.toString());
        ctx.setOvrWbuName(ovrWbuName.toString());
        boolean bDeletesDstAdjustmentOverrides = false;
        if (!StringHelper.isEmpty(deletesDstAdjustmentOverrides)) {
           System.out.println(deletesDstAdjustmentOverrides.toString().trim());
           bDeletesDstAdjustmentOverrides = "Y".equals(deletesDstAdjustmentOverrides.toString().trim()) ? true : false;
        }
        ctx.setDeletesDstAdjustmentOverrides(bDeletesDstAdjustmentOverrides);
        DSTScheduleAdjustment dst = new DSTScheduleAdjustment(ctx);
        dst.execute();

        // *** print the logs
        List logs = dst.getRunLogs();
        if (logs != null && logs.size() > 0) {
%>
        <wba:table width='800' caption="Run Logs">
<%
            Iterator it = logs.iterator();
            while (it.hasNext()) {
                String log = (String) it.next();
%>
                <wba:tr><wba:td><%=log%></wba:td></wba:tr>
<%
            }
%>
        </wba:table>
<%
        } // if
%>
        <br>
      <wba:table width='400'>
         <wba:tr><wba:td><wb:localize id='DSTSchedule_End'>DST Schedule Adjustment for Current Client Finished </wb:localize></wba:td></wba:tr>
      </wba:table>
      <br>
<%  }
%>
    <wba:table width='400'>
       <wba:tr><wba:td>
           <wb:localize id='DSTSchedule_Message'>Click Submit to Start DST Schedule Adjustment</wb:localize>
       </wba:td></wba:tr>
    </wba:table>
    <wba:table width='400'>
       <wba:tr>
         <wba:td>dstFallbackChangeDatetime</wba:td>
         <wba:td><wb:controlField cssClass='inputField' submitName="dstFallbackChangeDatetime" ui='StringUI' uiParameter=''></wb:controlField></wba:td>
       </wba:tr>
       <wba:tr>
         <wba:td>dstSpringforwardChangeDatetime</wba:td>
         <wba:td><wb:controlField cssClass='inputField' submitName="dstSpringforwardChangeDatetime" ui='StringUI' uiParameter=''></wb:controlField></wba:td>
       </wba:tr>
       <wba:tr>
         <wba:td>employeeWhereClause</wba:td>
         <wba:td><wb:controlField cssClass='inputField' submitName="employeeWhereClause" ui='StringUI' uiParameter='width=100'></wb:controlField></wba:td>
       </wba:tr>
       <wba:tr>
         <wba:td>ovrWbuName</wba:td>
         <wba:td><wb:controlField cssClass='inputField' submitName="ovrWbuName" ui='StringUI' uiParameter='width=40'></wb:controlField></wba:td>
       </wba:tr>
       <wba:tr>
         <wba:td>deletesDstAdjustmentOverrides</wba:td>
         <wba:td><wb:controlField cssClass='inputField' submitName="deletesDstAdjustmentOverrides" ui='CheckBoxUI' uiParameter=''></wb:controlField></wba:td>
       </wba:tr>
    </wba:table>

     <br>
    <!-- submission buttons -->
    <wb:submit id='operation'>submit</wb:submit>
    <wba:button label='Submit' labelLocalizeIndex='Submit' onClick='document.page_form.submit();'/>&nbsp;

</wb:page>