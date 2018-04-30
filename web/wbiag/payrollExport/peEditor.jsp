<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.*"%>
<%@ page import="com.workbrain.server.sql.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.server.jsp.taglib.util.*"%>
<%@ page import="com.workbrain.app.export.payroll.*"%>
<%@ page import="com.wbiag.app.export.payroll.*"%>
<%-- ********************** --%>
<wb:page login="true">
<script language="JavaScript">
	function submitForm(operation) {
	    document.forms[0].OPERATION.value = operation;
    	    document.forms[0].submit();
	}

	function addData() {
        var index = document.forms[0].data_option_row.options[document.forms[0].data_option_row.selectedIndex].value;
        var field = document.forms[0].data_option.options[document.forms[0].data_option.selectedIndex].value;
	    document.forms[0].OPERATION.value = "UPDATE";
	    document.forms[0].OPERATION_EXTRA.value = "DATA_ADD," + index + "," + field;
    	document.forms[0].submit();
	}

	function removeData(index) {
        document.forms[0].OPERATION.value = "UPDATE";
	    document.forms[0].OPERATION_EXTRA.value = "DATA_REMOVE," + index;
    	document.forms[0].submit();
	}

	function addLogic() {
        var index = document.forms[0].logic_option_row.options[document.forms[0].logic_option_row.selectedIndex].value;
        var match = document.forms[0].logic_add_match.options[document.forms[0].logic_add_match.selectedIndex].value;
        var oRow = document.forms[0].logic_add_output_row.options[document.forms[0].logic_add_output_row.selectedIndex].value;
        document.forms[0].OPERATION.value = "UPDATE";
	    document.forms[0].OPERATION_EXTRA.value = "LOGIC_ADD," + index + "," +  match + "," + oRow;
    	document.forms[0].submit();
	}

	function removeLogic(index) {
	    document.forms[0].OPERATION.value = "UPDATE";
	    document.forms[0].OPERATION_EXTRA.value = "LOGIC_REMOVE," + index;
    	document.forms[0].submit();
	}

	function addFormat(part) {
        var index = eval("document.forms[0].format_" + part +"_option_row.options[document.forms[0].format_" + part +"_option_row.selectedIndex].value");
        var field = eval("document.forms[0].format_" + part +"_option.options[document.forms[0].format_" + part +"_option.selectedIndex].value");
	    document.forms[0].OPERATION.value = "UPDATE";
	    document.forms[0].OPERATION_EXTRA.value = "FORMAT_ADD," + index + "," + field + "," + part;
    	document.forms[0].submit();
	}

	function removeFormat(part, index) {
        document.forms[0].OPERATION.value = "UPDATE";
        document.forms[0].OPERATION_EXTRA.value = "FORMAT_REMOVE," + index  + "," + part;
        document.forms[0].submit();
	}

	function forwardPage(toPage){
        window.location.replace(toPage);
	}
</script>
<%!
   String contextPath = null ;

   public String makeDataHeaderCommands(int dataCnt) {
       StringBuffer sb = new StringBuffer(200);
       String fieldName = "data_option";
       sb.append("&nbsp;&nbsp;<button type='button' onClick='addData();' class='buttonSmall' >Add</button>");
       sb.append("&nbsp;<select name='").append(fieldName).append("' class=inputField onChange=''>");
       Iterator iter = PayrollExportEditor.DataField.TYPE_NAMES.iterator();
       while (iter.hasNext()) {
           String item = (String)iter.next();
           sb.append("<option value='" + item + "' >" + item + "</option>");
       }
       sb.append("</select>");
       sb.append(" After Row ").append(createDD("data_option_row" , -1, dataCnt));
       return sb.toString();
   }

   public String makeDataCommands(int dataInd) {
       StringBuffer sb = new StringBuffer(200);
       String onClickRemove = "removeData(" + dataInd + ");";
       sb.append("&nbsp;<button type='button' onClick='" + onClickRemove + "' class='buttonSmall'' >Remove</button>");
       return sb.toString();
   }

   public String makeLogicHeaderCommands(int logicCnt) {
       StringBuffer sb = new StringBuffer(200);
       String fieldNameMatch = "logic_add_match";
       String fieldNameRow = "logic_add_output_row";
       sb.append("&nbsp;&nbsp;<button type='button' onClick='addLogic();' class='buttonSmall' >Add</button>");
       sb.append("&nbsp;").append(createDD(fieldNameMatch , 3, 10));
       sb.append("Match");
       sb.append("&nbsp;").append(createDD(fieldNameRow , 1, 5));
       sb.append("Map");

       sb.append(" After Row ").append(createDD("logic_option_row" , -1, logicCnt));
       return sb.toString();
   }

   public String makeLogicCommands(int logicInd) {
       StringBuffer sb = new StringBuffer(200);
       String onClickRemove = "removeLogic(" + logicInd + ");";
       sb.append("&nbsp;<button type='button' onClick='" + onClickRemove + "' class='buttonSmall'' >Remove</button>");
       return sb.toString();
   }

   public String createFieldDD(String name, String selected, List dataFields) {
     StringBuffer sb = new StringBuffer(200);
     sb.append("<select name='" + name + "' class=inputField>");
     sb.append("<option value='' ").append(StringHelper.isEmpty(selected) ? " selected " : "").append("></option>");
     Iterator iter = dataFields.iterator();
     while (iter.hasNext()) {
         String item = (String)iter.next();
         sb.append("<option value='" +item + "' ");
         if (selected.equalsIgnoreCase(item)) {
           sb.append(" selected ");
         }
         sb.append(">" + item + "</option>");
     }
     sb.append("</select>");
     return sb.toString();
   }

   public String createDD(String name, String selected, String[] allItems) {
     StringBuffer sb = new StringBuffer(200);
     sb.append("<select name='" + name + "' class=inputField>");
     for (int i = 0; i < allItems.length; i++) {
         sb.append("<option value='" + allItems[i] + "' ");
         if (selected != null
             && allItems[i] != null
             && allItems[i].toLowerCase().equals(selected.toLowerCase())) {
           sb.append(" selected ");
         }
         sb.append(">" + allItems[i] + "</option>");
     }
     sb.append("</select>");
     return sb.toString();
   }

   public String createDD(String name, int selected, int cnt) {
     StringBuffer sb = new StringBuffer(200);
     sb.append("<select name='" + name + "' class=inputField>");
     for (int i = 0; i <= cnt; i++) {
         sb.append("<option value='" + i + "' ");
         if (selected == i) {
           sb.append(" selected ");
         }
         sb.append(">" + i + "</option>");
     }
     sb.append("</select>");
     return sb.toString();
   }
   public String buttons (String mfrmId) {
     StringBuffer sb = new StringBuffer(200);
     sb.append("<table  class='contentTable' cellspacing=0 style='border-width:1px;'><tr>");
     sb.append("<td><button type='button' onClick=\"submitForm('SUBMIT');\" class='buttonMedium' >Submit</button></td>");
     sb.append("<td><button type='button' onClick=\"location.href = '../../maintenance/mntForms.jsp?mfrm_id=" + mfrmId + "'; return false;\" class='buttonMedium'  >Cancel</button></td>");
     sb.append("<td><a href ='#Data'>Data</td>");
     sb.append("<td><a href ='#Logic'>Logic</td>");
     sb.append("<td><a href ='#Format'>Format</td>");
     sb.append("</tr></table>");
     return sb.toString();
   }

   public String processData (PayrollExportEditor.PayrollExportData ped) {
     PayrollExportEditor.Data data = ped.getData();
     List dataFields = data.dataFields;
     StringBuffer sb = new StringBuffer(200);
     // *** heading
     sb.append("<table class='contentTable' cellspacing=0 style='border-width:1px;' width='100%'>");
     sb.append("<tr><th width='60%'><a name='Data'>DATA</a>&nbsp;&nbsp;<a href='#Top'><img src='" + contextPath + "/images/arrowup.gif' border=0>Top</a></th>");
     sb.append("<th>").append(makeDataHeaderCommands(data.dataFields.size()));
     sb.append("</th></tr>");
     int cnt =0;
     Iterator iter = dataFields.iterator();
     while (iter.hasNext()) {
         cnt ++;
         PayrollExportEditor.DataField item = (PayrollExportEditor.DataField)iter.next();
         sb.append("<tr><td>");
         sb.append("<input type='hidden' name='data_type_" + cnt + "' value='" + item.getType() +"'>");
         sb.append(cnt).append(".").append("&nbsp;");
         sb.append(item.getType() ).append("&nbsp;") ;
         Iterator iterAttr = item.getAttributeNames().iterator();
         while (iterAttr.hasNext()) {
             String attrName = (String)iterAttr.next();
             String fieldName = "data_" + attrName + "_" + cnt;
             String val = (String)item.attributes.get(attrName);
             sb.append(attrName).append(" = ");
             if ("group".equals(attrName)) {
                 sb.append(createDD(fieldName , val, PayrollExportEditor.BOOLEANS));
             }
             else if ("group_function".equals(attrName)) {
                 sb.append(createDD(fieldName , val, PayrollExportEditor.GROUP_FUNCTIONS ));
             }
             else if ("summary_function".equals(attrName)) {
                 sb.append(createDD(fieldName , val, PayrollExportEditor.SUMMARY_FUNCTIONS));
             }
             else {
                 sb.append("<INPUT type='text' Class='inputField' name='" + fieldName + "' size='15' Value='" + val + "' >");
             }
             sb.append("&nbsp;");
         }
         sb.append("</td>");
         sb.append("<td>").append(makeDataCommands(cnt)).append("</td>");
         sb.append("</tr>");
     }
     sb.append("<input type='hidden' name='data_count' value='" + cnt +"'>");
     sb.append("</table>");
     return sb.toString();
   }

   public String processLogic (PayrollExportEditor.PayrollExportData ped) {
       PayrollExportEditor.Logic logic = ped.getLogic();
       StringBuffer sb = new StringBuffer(200);
       // *** heading
       sb.append("<table class='contentTable' cellspacing=0 style='border-width:1px;' width='100%'>");
       sb.append("<tr><th width='60%'><a name='Logic'>LOGIC</a>&nbsp;&nbsp;<a href='#Top'><img src='" + contextPath + "/images/arrowup.gif' border=0>Top</a></th>");
       sb.append("<th>").append(makeLogicHeaderCommands(ped.getLogic().matchOutputs.size()));
       sb.append("</th></tr>");
       int logicCnt = 0;
       List logicList = logic.matchOutputs;
       Iterator iterLogic = logicList.iterator();
       while (iterLogic.hasNext()) {
           logicCnt ++;
           sb.append("<table class='contentTable' cellspacing=0 style='border-width:0px;'>");
           sb.append("<tr><th colspan=2>Match Output &nbsp; ").append(logicCnt).append(".</th>");
           sb.append("<th>").append(makeLogicCommands(logicCnt));
           sb.append("</th></tr>");
           sb.append("");
           PayrollExportEditor.MatchOutput item = (PayrollExportEditor.MatchOutput)iterLogic.next();
           List matches = item.matches;
           Iterator iterMatch = matches.iterator();
           int matchCnt = 0;
           while (iterMatch.hasNext()) {
              PayrollExportEditor.MatchOutputField match = (PayrollExportEditor.MatchOutputField)iterMatch.next();
              matchCnt ++;
              sb.append("<tr><td> match ");
              List attrNames = match.getAttributeNames();
              Iterator iter = attrNames.iterator();
              while (iter.hasNext()) {
                String attrName = (String)iter.next();
                String fieldName = "logic_match_" + attrName + "_" + logicCnt + "_" + matchCnt;
                String val = (String)match.attributes.get(attrName);
                sb.append(attrName).append(" = ");
                if (!"field".equals(attrName)) {
                  sb.append("<INPUT type='text' Class='inputField' name='" + fieldName + "' size='10' value='" + val  + "' >");
                }
                else {
                  sb.append(createFieldDD(fieldName , val , ped.getData().getDataFieldNames()));
                }
                sb.append("&nbsp;");
              }
              sb.append("</td>");
           }
           if (matchCnt == 0) {
              sb.append("<td>No Matches</td>");
           }
           sb.append("<td align=center><img src='" + contextPath + "/images/arrowright.gif' width=20 height=20></td>");
           sb.append("<input type=hidden name = 'logic_match_count_").append(logicCnt).append("' value='").append(matchCnt).append("'>");

           List outputRows = item.outputRows;
           Iterator iterMaps = outputRows.iterator();
           int outputRowCnt = 0;
           while (iterMaps.hasNext()) {
               PayrollExportEditor.MatchOutputField outputRow = (PayrollExportEditor.MatchOutputField)iterMaps.next();
               outputRowCnt ++;
               sb.append("<td> map ");
               List attrNames = outputRow.getAttributeNames();
               Iterator iter = attrNames.iterator();
               while (iter.hasNext()) {
                  String attrName = (String)iter.next();
                  String fieldName = "logic_map_" + attrName + "_" + logicCnt + "_" + outputRowCnt;
                  String val = (String)outputRow.attributes.get(attrName);
                  sb.append(attrName).append(" = ");
                  if (!"field".equals(attrName)) {
                     sb.append("<INPUT type='text' Class='inputField' name='" + fieldName + "' size='10' value='" + val + "' >");
                  }
                  else {
                    sb.append(createFieldDD(fieldName , val , ped.getData().getDataFieldNames()));
                  }
                  sb.append("&nbsp;");
               }
               sb.append("</td></tr>");
           }
           if (outputRowCnt == 0) {
              sb.append("<td>No Output Rows</td>");
           }
           sb.append("");
           sb.append("<input type=hidden name = 'logic_outputRow_count_").append(logicCnt).append("' value='").append(outputRowCnt).append("'>");
      }
      sb.append("<input type='hidden' name='logic_count' value='" + logicCnt +"'>");
      sb.append("</table>");
      return sb.toString();
   }

   public String processPlugin(PayrollExportEditor.PayrollExportData ped) {
       StringBuffer sb = new StringBuffer(200);
       sb.append("<table class='contentTable' cellspacing=0 style='border-width:1px;'>");
       sb.append("<tr><th width='20%'>Name</th>");
       sb.append("<td width='80%'>").append(ped.getPetName()).append("</th>");
       sb.append("</tr>");
       sb.append("<tr><th width='20%'>Plugin</th>");
       sb.append("<td><INPUT type='text' class='inputField' name='plugin' size='80' value='").append(ped.getPlugin()).append("' >").append("&nbsp;</th>");
       sb.append("</tr></table>");
       return sb.toString();
   }

   public String processFormat(PayrollExportEditor.PayrollExportData ped) {
       StringBuffer sb = new StringBuffer(200);
       sb.append("<table class='contentTable' cellspacing=0 style='border-width:1px;' width='100%'>");
       sb.append("<r><th width='60%'><a name='Format'>FORMAT</a>&nbsp;&nbsp;<a href='#Top'><img src='" + contextPath + "/images/arrowup.gif' border=0>Top</a></th>");
       sb.append("</tr></table>");
       sb.append(processFormatPart("header", ped.getFormat().header , ped.getData().getDataFieldNames()));
       sb.append(processFormatPart("body", ped.getFormat().body , ped.getData().getDataFieldNames()));
       sb.append(processFormatPart("footer", ped.getFormat().footer , ped.getData().getDataFieldNames()));
       return sb.toString();
   }

   public String processFormatPart (String partName, List part, List dataFldNames) {
     StringBuffer sb = new StringBuffer(200);
     sb.append("<table  class='contentTable' cellspacing=0 style='border-width:1px;' width='100%'>");
     sb.append("<tr><th width='70%'>" + partName + "</th><th>");
     sb.append(makeFormatHeaderCommands(partName , part.size() ));
     sb.append("</th><th></th></tr>");
     int cnt =0;
     Iterator iter = part.iterator();
     while (iter.hasNext()) {
         cnt ++;
         PayrollExportEditor.FormatField item = (PayrollExportEditor.FormatField)iter.next();
         sb.append("<tr><td>");
         sb.append("<input type='hidden' name='" + partName + "_type_" + cnt + "' value='" + item.getType() +"'>");
         sb.append(cnt).append(".").append("&nbsp;");
         sb.append(item.getType() ).append("&nbsp;") ;
         if (item.isTypePCData(item.getType())) {
             String fieldName = "format_" + partName + "_pcdata_" + cnt;
             sb.append("<INPUT type='text' Class='inputField' name='" + fieldName + "' size='10' Value='" + item.PCDataValue + "' >").append("&nbsp;") ;
         }
         Iterator iterAttr = item.getAttributeNames().iterator();
         while (iterAttr.hasNext()) {
             String attrName = (String)iterAttr.next();
             String fieldName = "format_" + partName + "_" + attrName + "_" + cnt;
             String val = (String)item.attributes.get(attrName);
             if (!item.isTemp && StringHelper.isEmpty(val)) {
                 continue;
             }
             sb.append(attrName).append(" = ");
             if (!"field".equals(attrName)) {
                 sb.append("<INPUT type='text' Class='inputField' name='" + fieldName + "' size='10' Value='" + val + "' >");
             }
             else {
                 sb.append(createFieldDD(fieldName , val , dataFldNames));
             }
             sb.append("&nbsp;");
         }
         sb.append("</td>");
         sb.append("<td>").append(makeFormatCommands(partName , cnt)).append("</td>");
         sb.append("</tr>");
     }
     sb.append("</table>");
     sb.append("<input type='hidden' name='format_" + partName + "_count' value='" + cnt +"'>");
     return sb.toString();
   }

   public String makeFormatHeaderCommands(String part, int partCnt) {
       StringBuffer sb = new StringBuffer(200);
       String fieldName = "format_" + part + "_option";
       sb.append("&nbsp;&nbsp;<button type='button' onClick=\"addFormat('" + part + "');\" class='buttonSmall' >Add</button>");
       sb.append("&nbsp;<select name='" + fieldName + "' class=inputField onChange=''>");
       Iterator iter = PayrollExportEditor.FormatField.TYPE_NAMES.iterator();
       int cnt=0;
       while (iter.hasNext()) {
           String item = (String)iter.next();
           sb.append("<option ");
           if (cnt == 0) {
             sb.append(" selected ");
           }
           sb.append("value='" + item + "' >" + item + "</option>");
       }
       sb.append("</select>");
       sb.append(" After Row ").append(createDD("format_" + part + "_option_row" , -1, partCnt));
       return sb.toString();
   }

   public String makeFormatCommands(String partName, int formatInd) {
       StringBuffer sb = new StringBuffer(200);
       String onClickRemove = "removeFormat('" + partName + "'," + formatInd + ");";
       sb.append("&nbsp;<button type='button' onClick=\"" + onClickRemove + "\" class='buttonSmall'' >Remove</button>");
       return sb.toString();
   }
%>
<%
    int petId = 0;
    contextPath = (String)request.getContextPath() ;
    String mfrm_id  = (String)request.getParameter("mfrm_id") ;
    String goBack =  "location.href = '../maintenance/mntForms.jsp?mfrm_id=" + mfrm_id + "'; return false;";
    String spetId = (String)request.getParameter("PET_ID") ;

    if (StringHelper.isEmpty(spetId)) {
        throw new RuntimeException ("PET_ID must be supplied");
    }
    else {
       petId = Integer.parseInt(spetId);
    }
    DBConnection conn = JSPHelper.getConnection(request);
    PayrollExportEditor pee = new PayrollExportEditor(conn);

    PayrollExportEditor.PayrollExportData ped = null;
    String operation = request.getParameter("OPERATION") ;
    if ("SUBMIT".equals(operation)) {
        String errMsg =  null;
        try {
          pee.savePayrollExportData(request, petId);
        }
        catch (Exception e){
          errMsg = e.getMessage() + "<br> Trace:" + StringHelper.getStackTrace(e) ;
          conn.rollback();
        }
        if (!StringHelper.isEmpty(errMsg)) {
        %>
            <span fgColor="red">Payroll Group Task could not be saved. <br> Error Message: <%=errMsg %></span>
        <%
        }
        else {
        %>
            <span>Payroll Group Task has been updated successfully.</span>
        <%
        }
        ped = pee.createPayrollExportData(petId) ;
        session.setAttribute("PayrollExportData", ped);
    }
    else if ( "UPDATE".equals(operation)) {
        ped = (PayrollExportEditor.PayrollExportData)session.getAttribute("PayrollExportData");
        String opExtra = request.getParameter("OPERATION_EXTRA") ;
        String forwardPage = null;
        if (opExtra.startsWith("DATA_ADD")) {
            String[] tokens = StringHelper.detokenizeString(opExtra, ",") ;
            String fld = tokens[2];
            int index = Integer.parseInt(tokens[1]);
            ped.getData().addDataFieldEmpty(fld , index);
            forwardPage = "#data";
        }
        else if ( opExtra.startsWith("DATA_REMOVE")) {
            String[] tokens = StringHelper.detokenizeString(opExtra, ",") ;
            int index = Integer.parseInt(tokens[1]);
            ped.getData().removeField(index - 1);
            forwardPage = "#data";
        }
        else if (opExtra.startsWith("LOGIC_ADD")) {
            String[] tokens = StringHelper.detokenizeString(opExtra, ",") ;
            int index = Integer.parseInt(tokens[1]);
            int matchCnt = Integer.parseInt(tokens[2]);
            int oRowCnt = Integer.parseInt(tokens[3]);
            ped.getLogic().addMatchOutputEmpty(index , matchCnt, oRowCnt);
            forwardPage = "#logic";
        }
        else if ( opExtra.startsWith("LOGIC_REMOVE")) {
            String[] tokens = StringHelper.detokenizeString(opExtra, ",") ;
            int index = Integer.parseInt(tokens[1]);
            ped.getLogic().removeMatchOutput(index - 1);
            forwardPage = "#logic";
        }
        else if (opExtra.startsWith("FORMAT_ADD")) {
            String[] tokens = StringHelper.detokenizeString(opExtra, ",") ;
            int index = Integer.parseInt(tokens[1]);
            String type = tokens[2];
            String part = tokens[3];
            ped.getFormat().addFormatFieldEmpty(type , part, index);
            forwardPage = "#format";
        }
        else if ( opExtra.startsWith("FORMAT_REMOVE")) {
            String[] tokens = StringHelper.detokenizeString(opExtra, ",") ;
            int index = Integer.parseInt(tokens[1]);
            String part = tokens[2];
            ped.getFormat().removeFormatField(part , index - 1);
            forwardPage = "#format";
        }
        if (forwardPage != null) {
%>
	    <script language='javascript' type='text/javascript'>
		  forwardPage('<%=forwardPage%>');
	    </script>
<%
        }
    }
    else {
        ped = pee.createPayrollExportData(petId) ;
        session.setAttribute("PayrollExportData", ped);
    }

    String petName = ped.getPetName();
%>
    <a name="Top"></a>
<%
    out.println(buttons(mfrm_id));

    // *** Plugin etc
    out.println(processPlugin(ped));

    // *** Data
    out.println(processData(ped));

    // *** Logic
    out.println(processLogic(ped));

    // *** Format
    out.println(processFormat(ped));
%>
    <input type=hidden name = "PET_ID" value="<%=petId%>">
    <wb:submit id="mfrm_id"><%=mfrm_id%></wb:submit>
    <wb:submit id="OPERATION_EXTRA"></wb:submit>
    <wb:submit id="OPERATION"></wb:submit>
<%
    out.println(buttons(mfrm_id));
%>
</wb:page>