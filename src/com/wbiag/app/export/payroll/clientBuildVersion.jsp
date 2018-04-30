<%@ include file="/system/wbheader.jsp"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="java.util.*,java.io.*,"%>

<%-- specific to the client build reading and update --%>
<%	
		String clientBuildDate=null;
		String clientBuild=null;
		int clientBuildLessOne;
		java.util.Date now = new Date();
		try{
			Properties settings = new Properties();
			String getRealPath=application.getRealPath("\\system\\client_build.properties");
			FileInputStream in = new FileInputStream(getRealPath);
			settings.load(in);
			clientBuildDate=settings.getProperty("client.build.date");			
			clientBuild=settings.getProperty("client.build.number");
			//because the build version is incremented AFTER the build is complete
			//we have to decrement the build version here by one to get the proper build number.
			clientBuildLessOne=Integer.parseInt(clientBuild)-1;
		}catch (FileNotFoundException fne){
			//clientBuildDate=now.toString();
			clientBuildDate="no client build yet";
			clientBuildLessOne=0;
			clientBuild="0";
		}
%>

<wb:page subsidiaryPage="true" login="false" title="About Workbrain" popupPage='true'>
<wba:resizeWindowToMakeContentsVisible>
<style>
body {padding:0px; margin:0px; }
.aboutVersion { font-family:trebuchet MS; font-size:20px; color:#0061B0; padding-right:3px; }
.aboutField { font-family:trebuchet MS; font-size:15px; color:#0061B0 ; padding:6px; padding-top:2px; padding-bottom:2px ;}
.aboutValue { font-family:trebuchet MS; font-size:14px; color:black ; padding:6px ; padding-top:2px; padding-bottom:2px}
</style>

<wb:define id="databaseName, databaseVersion"/>

<wb:sql createDataSource="dbVersion">
	select wbv_name dbVersion from workbrain_version order by wbv_date desc
</wb:sql>

<wb:dataSet dataSource="dbVersion" id="dsDbVersion">
	<wb:switch>
	<wb:case expression="#dsDbVersion.property.isRowFetched#">
		<wb:set id="databaseVersion"><wb:getDataFieldValue name="dbVersion"/></wb:set>
	</wb:case>
	</wb:switch>
</wb:dataSet>

<table class=aboutTable cellpadding=0 cellspacing=0>
<tr>
	<td>
		<table cellpadding=0 cellspacing=0>
		<tr>
			<td colspan=2 align=right class=aboutVersion>version <strong><wb:evaluate>workbrain.version</wb:evaluate></strong></td>
		</tr>
		<tr>
			<td class=aboutField><wb:localize id="AboutCustomer">Customer</wb:localize></td>
			<td class=aboutValue><wb:evaluate>workbrain.customer</wb:evaluate></td>
		</tr>
		<tr><td colspan=2></td></tr>
		<tr>
			<td class=aboutField><wb:localize id="DatabaseType">DB Type</wb:localize></td>
			<td class=aboutValue><%=JSPHelper.getDBType(request).getName()%>
            &nbsp;
			</td>
		</tr>
		<tr><td colspan=2></td></tr>
		
		<tr><td colspan=2></td></tr>
		<%--<tr>
			<td class=aboutField>Database Version</td>
			<td class=aboutValue><wb:evaluate>workbrain.dbversion</wb:evaluate></td>
		</tr>--%>
		<tr><td colspan=2></td></tr>
		<tr>
			<td class=aboutField><wb:localize id="AboutJ2EEVersion">J2EE Version</wb:localize></td>
			<td class=aboutValue><wb:evaluate>workbrain.version</wb:evaluate></td>
		</tr>
		<tr><td colspan=2></td></tr>
		<tr>
			<td class=aboutField><wb:localize id="AboutJ2EEBuild">J2EE Build</wb:localize></td>
			<td class=aboutValue><wb:evaluate>workbrain.build</wb:evaluate></td>
		</tr>
        <tr><td colspan=2></td></tr>
        <tr>
            <td class=aboutField><wb:localize id="AboutJ2EEBuildDate">J2EE Build Date</wb:localize></td>
            <td class=aboutValue><wb:evaluate>workbrain.builddate</wb:evaluate></td>
        </tr>
        <tr><td colspan=2></td></tr>
        <tr>
            <td class=aboutField><wb:localize id="AboutJ2EEAppServerName">App Server Name</wb:localize></td>
            <td class=aboutValue><%=com.workbrain.server.WebSystem.getAppServerName()%></td>
        </tr>
        <tr><td colspan=2></td></tr>
        <tr>
            <td class=aboutField><wb:localize id="AboutJ2EEAppServerVersion">App Server Version</wb:localize></td>
            <td class=aboutValue><%=com.workbrain.server.WebSystem.getAppServerVersion()%></td>
        </tr>
		<tr>
            <td class=aboutField><wb:localize id="AboutJ2EEAppServerOSName">App Server OS Name</wb:localize></td>
            <td class=aboutValue><%=com.workbrain.server.WebSystem.getAppServerOSName()%></td>
        </tr>
		<tr>
            <td class=aboutField><wb:localize id="AboutJ2EEAppServerOSVer">App Server OS Ver#</wb:localize></td>
            <td class=aboutValue><%=com.workbrain.server.WebSystem.getAppServerOSVer()%></td>
        </tr>
		<tr><td colspan=2></td></tr>
		<tr> <td class=aboutField><wb:localize id="AboutClientBuildDate">Client Build Date</wb:localize></td>
				 <td class=aboutValue><%=clientBuildDate%></td>
    </tr>
		<tr><td colspan=2></td></tr>
	  <tr> <td class=aboutField><wb:localize id="AboutClientBuildNum">Client Build #</wb:localize></td>
				 <td class=aboutValue><%=clientBuild%></td>
	  </tr>
	 
		<tr>
			<td colspan=2>
				&nbsp;
			</td>
		</tr>
		</table>
	</td>
</tr>
</table>
</wba:resizeWindowToMakeContentsVisible>
<script>disableCloseWindowLink = 1;</script>
</wb:page>
