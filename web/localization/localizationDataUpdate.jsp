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
<%@ page import="com.wbiag.app.ta.db.LocalizationDataUpdate"%>
<%-- ********************** --%>
<wb:page maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>


    <wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/localization/localizationDataUpdate.jsp"/></wb:define>
    <wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>

    <wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
    <wb:submit id="OPERATION">SUBMIT</wb:submit>

    <wb:define id="OLDSTRING"><wb:get id="OLDSTRING" scope="parameter" default=""/></wb:define>
    <wb:define id="NEWSTRING"><wb:get id="NEWSTRING" scope="parameter" default=""/></wb:define>
    <wb:define id="MFRM"><wb:get id="MFRM" scope="parameter" default=""/></wb:define>
    <wb:define id="MFRM_DATA"><wb:get id="MFRM_DATA" scope="parameter" default=""/></wb:define>
    <wb:define id="FIELD"><wb:get id="FIELD" scope="parameter" default=""/></wb:define>
    <wb:define id="MSG"><wb:get id="MSG" scope="parameter" default=""/></wb:define>
    <wb:define id="ERROR_MSG"><wb:get id="ERROR_MSG" scope="parameter" default=""/></wb:define>
    <wb:define id="TIMESHEET_ORDER"><wb:get id="TIMESHEET_ORDER" scope="parameter" default=""/></wb:define>
    <wb:define id="LOCALE"><wb:get id="LOCALE" scope="parameter" default=""/></wb:define>

    <wb:define id="okOnClickUrl">window.location='#GO_TO#';</wb:define>
 
    <wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default=""/></wb:define>

    <%
    if("SUBMIT".equals(OPERATION.toString()))
    {
        boolean flag = false;
        boolean bMaintenanceForm = MFRM.toString().equals("Y");
        boolean bMaintenanceFormData = MFRM_DATA.toString().equals("Y");
        boolean bFieldData = FIELD.toString().equals("Y");
        boolean bMessageData = MSG.toString().equals("Y");
        boolean bErrorData = ERROR_MSG.toString().equals("Y");
        boolean bTimeSheetOrder = TIMESHEET_ORDER.toString().equals("Y");
        String sLocale = LOCALE.toString();
        int rowsModified = 0;
        if (sLocale.equals(""))
        {
            flag = false;
        }
        else
        {
        LocalizationDataUpdate ldu = new LocalizationDataUpdate();
            try
            {
                DBConnection conn = JSPHelper.getConnection(request);
                rowsModified = ldu.Update(OLDSTRING.toString(), NEWSTRING.toString(), bMaintenanceForm, bMaintenanceFormData, bFieldData, bMessageData, bErrorData, LOCALE.toInt(), conn);
                conn.commit();
                flag = true;
            }
            catch(SQLException e)
            {
                flag = false;
                %>
                <span>Unable to update the localization data.  The following error occured:</span>
                <P><%=e%></P>
                <wba:button label="Try again" intensity="low" onClick="#okOnClickUrl#"/>

            <%}
        }
        if(flag)
        {
            if(rowsModified > 0){
            %>
                <span><I><%=rowsModified%></I> occurances of localization string <I><%=OLDSTRING.toString()%></I> changed to  <I><%=NEWSTRING.toString()%></I>.</span>
                <BR><wba:button label="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>

            <%  
            } else {
            %>
                <span>Localization string <I><%=OLDSTRING.toString()%></I> not found.  No changes were made.</span>
                <BR><wba:button label="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>

            <%  

            }
        }
        else
        {
        %>
            <span>Please select a locale.  Localization data update tool not run.</span>
            <BR><wba:button label="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>

        <%
        }
    }
    else
    {

        String mfrmIdStr = mfrm_id.toString();
        //If the maintenance form ID is null, retrieve it from the database.
        if (mfrmIdStr.equals(""))
        {
            DBConnection dbc = JSPHelper.getConnection(request);
            PreparedStatement ps = null;
            ResultSet rs = null;
            ps = dbc.prepareStatement( "SELECT * " +
                                     "FROM maintenance_form " +
                                     "WHERE mfrm_jsp = ? ");
            ps.setString(1,"/localization/localizationDataUpdate.jsp");
            rs = ps.executeQuery();
            rs.next();
            mfrmIdStr = rs.getString("mfrm_id");
            rs.close();
            rs = null;
            ps.close();
            ps = null;        
        }
    %>

    <wba:table caption="Localization Data Update" captionLocalizeIndex="LocalizationDataUpdateText" >
        <wba:tr>
            <wba:th width='40%'> 
                <wb:localize id="Old_String" overrideId="<%=mfrmIdStr%>" type="field">Existing Localization Text </wb:localize>
            </wba:th>
            <wba:td width='60%'>
                <wb:controlField cssClass="inputField" id='Old_String' overrideId="<%=mfrmIdStr%>" submitName="OLDSTRING"><%= request.getParameter("OLDSTRING") %></wb:controlField>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='40%'> 
                <wb:localize id="New_String" overrideId="<%=mfrmIdStr%>" type="field">New Localization Text </wb:localize>
            </wba:th>
            <wba:td width='60%'>
                <wb:controlField cssClass="inputField" id='New_String' overrideId="<%=mfrmIdStr%>" submitName="NEWSTRING"><%= request.getParameter("NEWSTRING") %></wb:controlField>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='40%'> 
                <wb:localize id="Maintenance_Form" overrideId="<%=mfrmIdStr%>" type="field">Maintenance Form </wb:localize>
            </wba:th>
            <wba:td width='60%'>
                <wb:controlField ui='CheckboxUI' id='Maintenance_Form_Checkbox' submitName="MFRM"/>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='40%'> 
                <wb:localize id="Maintenance_Form_Data" overrideId="<%=mfrmIdStr%>" type="field">Maintenance Form Data </wb:localize>
            </wba:th>
            <wba:td width='60%'>
                <wb:controlField ui='CheckboxUI' id='Maintenance_Form_Data_Checkbox' submitName="MFRM_DATA"/>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='40%'> 
                <wb:localize id="Field_Locale_Data" overrideId="<%=mfrmIdStr%>" type="field">Field Locale Data </wb:localize>
            </wba:th>
            <wba:td width='60%'>
                <wb:controlField ui='CheckboxUI' id='Field_Locale_Checkbox' submitName="FIELD"/>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='40%'> 
                <wb:localize id="Message_Locale_Data" overrideId="<%=mfrmIdStr%>" type="field">Message Locale Data </wb:localize>
            </wba:th>
            <wba:td width='60%'>
                <wb:controlField ui='CheckboxUI' id='Message_Locale_Checkbox' submitName="MSG"/>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='40%'> 
                <wb:localize id="Error_Message_Locale_Data" overrideId="<%=mfrmIdStr%>" type="field">Error Message Locale Data </wb:localize>
            </wba:th>
            <wba:td width='60%'>
                <wb:controlField ui='CheckboxUI' id='Error_Message_Locale_Data_Checkbox' submitName="ERROR_MSG"/>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='40%'> 
                <wb:localize id="Locale_Field" overrideId="<%=mfrmIdStr%>" type="field">Locale </wb:localize>
            </wba:th>
            <wba:td width='60%'>
                <wb:controlField submitName="LOCALE" ui="DBListboxUI" cssClass="inputField" uiParameter="width=50 sourceType=SQL source='SELECT WBLL_ID, WBLL_NAME FROM WORKBRAIN_LOCALE' sourceKeyField=wbll_id sourceLabelField=wbll_id"><wb:get id="LOCALE"/></wb:controlField>
            </wba:td>
        </wba:tr>



    </wba:table>

    <wba:button type='submit' label='Submit' labelLocalizeIndex='Submit' onClick=''/>

    <%}%>
</wb:page>