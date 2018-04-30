<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="com.workbrain.sql.DBConnection"%>
<%@ page import="com.workbrain.util.*,com.workbrain.server.jsp.taglib.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.jsp.locale.*"%>
<%@ page import="com.workbrain.app.ta.ruleengine.*"%>
<%@ page import="com.wbiag.app.ta.ruleTrace.*,com.wbiag.app.ta.ruleTrace.model.*
         ,com.wbiag.app.ta.ruleTrace.engine.*, com.wbiag.app.ta.ruleTrace.db.*"%>
<%@ page import="com.wbiag.util.NameValueList"%>

<%@ page import="javax.servlet.jsp.JspException" %>
<%-- ********************** --%>
<%!
  private String makeContentHTML(RuleTraceContent content , RuleTraceConfig config , HttpServletRequest request)  throws Exception {
      StringBuffer sb = new StringBuffer(200);
      if (content == null) return "";
      sb.append("<table style='border-style:solid; border-width:1px; padding:0px; ;background-color:#E7E7E7'>");
      if (content.getEmployeeData() != null) {
        sb.append(makeDataHTML("Employee Data", config.getEmployeeFields() , content.getEmployeeData() , request));
      }
      if (content.getWorkSummaryData() != null) {
        sb.append(makeDataHTML("Work Summary Data", config.getWorkSummaryFields(), content.getWorkSummaryData() , request));
      }
      if (content.getWorkDetails() != null) {
        sb.append(makeWrkdHTML("Work Details", config.getWorkDetailFields(), content.getWorkDetails() , request));
      }
      if (content.getWorkPremiums() != null) {
        sb.append(makeWrkdHTML("Work Premiums", config.getWorkPremiumFields() , content.getWorkPremiums() , request));
      }
      if (content.getEmployeeBalances()  != null) {
        sb.append(makeBalanceHTML("Employee Balances", config.getEmployeeBalances() , content.getEmployeeBalances()));
      }

      sb.append("</table >");
      return sb.toString();
  }

  private String makeWrkdHTML(String header, List fields, List workDetails, HttpServletRequest request)  throws Exception {
      StringBuffer sb = new StringBuffer(200);
      sb.append("<tr><td>").append(header).append("</td></tr>");
      sb.append("<tr><td><table border='1'>");
      // *** heading
      sb.append(makeHeading(fields , request));
      // *** data
      Iterator iterW = workDetails.iterator();
      while (iterW.hasNext()) {
          NameValueList data = (NameValueList)iterW.next();
          sb.append(makeOneRowData(fields , data));
      }
      sb.append("</table></td>");
      sb.append("</tr>");
      return sb.toString();
  }

  private String makeDataHTML(String header, List fields, NameValueList data, HttpServletRequest request)  throws Exception {
      StringBuffer sb = new StringBuffer(200);
      sb.append("<tr><td>").append(header).append("</td></tr>");
      // *** heading
      sb.append("<tr><td><table border='1''>");
      sb.append(makeHeading(fields , request));
      // *** data
      sb.append(makeOneRowData(fields , data));
      sb.append("</table></td>");
      sb.append("</tr>");
      return sb.toString();
  }

  private String makeBalanceHTML(String header, List balances, NameValueList data) {
      StringBuffer sb = new StringBuffer(200);
      sb.append("<tr><td>").append(header).append("</td></tr>");
      // *** heading
      sb.append("<tr><td><table border='1''>");
      Iterator iter  = balances.iterator();
      while (iter.hasNext()) {
          String bal = (String)iter.next();
          sb.append("<td>").append(bal).append("</td>");
          String val = data.getByName(bal).getValue();
          sb.append("<td>").append(val).append("</td>");
      }
      sb.append("</tr>");
      sb.append("</table></td>");
      sb.append("</tr>");
      return sb.toString();
  }

  private String makeOneRowData(List fields , NameValueList data){
      StringBuffer sb = new StringBuffer(200);
      sb.append("<tr>");
      Iterator iter  = fields.iterator();
      while (iter.hasNext()) {
          String fld = (String)iter.next();
          String val = data.getByName(fld).getValue();
          if (!StringHelper.isEmpty(val)) {
            if ("WRKD_MINUTES".equalsIgnoreCase(fld)) {
               val = (Integer.parseInt(val) / 60) + ":"
                 + StringHelper.padRight(String.valueOf(Integer.parseInt(val)%60), 2, "0");
            }
          }
          else {
            val = "&nbsp;";
          }
          sb.append("<td>").append(val).append("</td>");
      }
      sb.append("</tr>");
      return sb.toString();
  }

  private String makeHeading(List fields , HttpServletRequest request)  throws Exception {
      LocalizationDictionary ld =  LocalizationDictionary.get();
      StringBuffer sb = new StringBuffer(200);
      sb.append("<tr>");
      Iterator iter = fields.iterator();
      while (iter.hasNext()) {
          String fld = (String)iter.next();
          String fldLcl = ld.localizeField(JSPHelper.getConnection(request),
                       JSPHelper.getWebLocale(request), fld);
          if (StringHelper.isEmpty(fldLcl)) {
            fldLcl = fld;
          }
          sb.append("<td>").append(fldLcl).append("</td>");
      }
      sb.append("</tr>");
      return sb.toString();
  }

  private String makeParameters(Parameters pars, int cntSpc) {
      StringBuffer sb = new StringBuffer(200);
      sb.append("<table>");
      Enumeration enumP = pars.getParameters() ;
      while (enumP.hasMoreElements()) {
        Parm parm = (Parm)enumP.nextElement();
        sb.append("<tr><td>").append(addSpaces(cntSpc)).append("</td>")
          .append("<td>").append(parm._name).append(addSpaces(1)).append("=").append(addSpaces(1))
          .append(parm._value ).append("</td></tr>")    ;
      }
      sb.append("</tr>");
      sb.append("</table>");
      return sb.toString();
  }

  private String createCEIcon(String idImg, String id) {
      StringBuffer sb = new StringBuffer(200);
      sb.append("<img style='cursor:pointer;border:none' src='/images/icon_expand.gif' name='").append(idImg)
            .append("' onclick=\"switchItem('").append(id).append("');\">");
      return sb.toString();
  }

  private String createEvalIcon(boolean eval) {
      StringBuffer sb = new StringBuffer(200);
      sb.append("<img width=15 height=15 src='")
        .append(eval ? "/images/interface/sc_app_warn_16x16.gif" : "/images/interface/iconLogoutx.gif")
        .append("' ")
        .append(eval ? " alt='Rule executed for at least one condition set' " : " alt='Rule not executed' ")
        .append(">&nbsp;");
      return sb.toString();
  }

  private String addSpaces(int cnt) {
    StringBuffer sb = new StringBuffer(200);
    for (int i = 1; i <= cnt; i++) {
          sb.append("&nbsp;");
    }
    return sb.toString();
  }

  public String makeHTML(RuleTraceList rtlist , RuleTraceConfig config, HttpServletRequest request) throws Exception {
      StringBuffer sb = new StringBuffer(200);
      if (rtlist == null || rtlist.size() == 0) {
        sb.append("<h3>No rules have been executed</h3>");
        return sb.toString();
      }
      sb.append("<a href=# onClick=\"switchAll('").append(rtlist.size())
        .append("');\"'><img border=0 src='/images/icon_expand_all.gif' alt='Expand All' name='SwitchAll'></a>");

      sb.append("<dl>"); // RULE LIST
      for (int ind=0; ind < rtlist.size() ; ind++) {
        RuleNodeExt rt = (RuleNodeExt) rtlist.get(ind);
        String id = "RULE_" + (ind + 1);
        String idImg = id + "_img";
        // RULE HEADING
        sb.append("<dt>");
        sb.append(createCEIcon(idImg, id));
        sb.append(createEvalIcon(rt.isEval()))
          .append("for RULE (").append(rt.getRuleName()).append(")");
        if (rt.isChangedFromPrevious()) {
            sb.append("&nbsp;<img  width=12 height=12 border=0 src='/images/interface/nextarrowblue_16x16.gif' alt='Data Has Changed Compared to Last Rule' name='DataHChanged'>");
        }

        sb.append("<div style='position:relative' id='").append(id).append("'>");

        Enumeration enumCS = rt.getConditionSets();
        int csCnt = 1;
        while (enumCS.hasMoreElements()) {
            ConditionSetNodeExt csNode = (ConditionSetNodeExt)enumCS.nextElement();
            sb.append("<dl>");
            String idCS = id + "_CS_" + csCnt++;
            String idImgCS = idCS + "_img";
            // CONDITION SET HEADING
            sb.append("<dt>");
            sb.append(createCEIcon(idImgCS , idCS));

            sb.append(createEvalIcon(csNode.isEval()))
              .append("for CONDITION SET (").append(csNode.getDescription()).append(")");

            sb.append("<dl>"); // CSET LIST
            sb.append("<div id='").append(idCS).append("'>");
            // CONDITIONS
            String idParamsCS = id + "_CSPARAMS_" + csCnt;
            String idImgParamsCS = idParamsCS + "_img";
            Enumeration enumC = csNode.getConditions();
            int cCnt = 1;
            while (enumC.hasMoreElements()) {
              ConditionNodeExt cNode = (ConditionNodeExt)enumC.nextElement();
              String idParamsC = idParamsCS + "_CPARAMS_" + cCnt++;
              String idImgParamsC = idParamsC + "_img";

              sb.append(createCEIcon(idImgParamsC , idParamsC));
              sb.append(createEvalIcon(cNode.isEval()))
                .append("if CONDITION (").append(cNode.getName()).append(")");

              sb.append("<dt>");
              sb.append("<div id='").append(idParamsC).append("'>"); // DIV COND PARAMS
              Parameters parsC = cNode.getParameters();
              sb.append(makeParameters(parsC, 15));
              sb.append("</div>"); // DIV COND PARAMS
              sb.append("</dt>");
              sb.append("<script type='text/javascript'>collapseItem('").append(idParamsC).append("'); </script>");

            }
            // CONDITION SET PARAMETERS
            sb.append("<img style='cursor:pointer;border:none' src='/images/icon_expand.gif' name='").append(idImgParamsCS)
              .append("' onclick=\"switchItem('").append(idParamsCS).append("');\">");
            sb.append(addSpaces(5)).append("then use these RULE PARAMETERS");
            sb.append("<dt>");
            sb.append("<div id='").append(idParamsCS).append("'>"); // DIV CONDSET PARAMS
            Parameters parsCS = csNode.getParameters();
            sb.append(makeParameters(parsCS, 15));
            sb.append("</div>"); // DIV CONDSET PARAMS
            sb.append("</div>"); // DIV CONDSET
            sb.append("</dt>");
            sb.append("</dl>"); // CSET LIST
            sb.append("<script type='text/javascript'>collapseItem('").append(idParamsCS).append("');</script>");
            // DATA
            sb.append("<dt>");
            String idData = idParamsCS + "_DATA_";
            String idImgData = idData + "_img";
            sb.append(createCEIcon(idImgData , idData))
              .append(addSpaces(5)).append("the RESULTS are");
            sb.append("<div id='").append(idData).append("'>").append(addSpaces(0));  // DIV DATA
            RuleTraceContent content =  csNode.getRuleTraceContent();
            sb.append(makeContentHTML(content , config , request));
            sb.append("</div>"); // DIV DATA
            sb.append("</dt>");
            sb.append("</dl>");
            sb.append("<script type='text/javascript'>collapseItem('").append(idData).append("'); </script>");
            sb.append("<script type='text/javascript'>collapseItem('").append(idCS).append("'); </script>");
        }
        sb.append("</div>"); // div RULE
        sb.append("</dt>");
        sb.append("<script type='text/javascript'>collapseItem('").append(id).append( "'); </script>");

      }
      sb.append("</dl>"); // RULE LIST
      return sb.toString();
  }


%>
<wb:page title="Rule Trace" >
<style type="text/css">
</style>
<script language="JavaScript">
  function switchItem(obj) {
	var el = document.getElementById(obj);
	if ( el.style.display != "none" ) {
          el.style.display = 'none';
          document.getElementById(obj + "_img").src = '/images/icon_expand.gif';
	}
	else {
          el.style.display = '';
          document.getElementById(obj + "_img").src = '/images/icon_collapse.gif';
	}
  }

  function collapseItem(obj) {
    document.getElementById(obj).style.display = 'none';
    document.getElementById(obj + "_img").src = '/images/icon_expand.gif';
  }

  function expandItem(obj) {
    document.getElementById(obj).style.display = '';
    document.getElementById(obj + "_img").src = '/images/icon_collapse.gif';
  }

  function switchAll(cnt) {
    if (document.getElementById("SwitchAll").src.search('icon_expand_all.gif') > -1) {
      for (var i=1; i<=cnt; i++){
        expandItem("RULE_" + i);
      }
      document.getElementById("SwitchAll").src = '/images/icon_collapse_all.gif';
      document.getElementById("SwitchAll").alt = 'Collapse All';
    }
    else {
      for (var i=1; i<=cnt; i++){
        collapseItem("RULE_" + i);
      }
      document.getElementById("SwitchAll").src = '/images/icon_expand_all.gif';
      document.getElementById("SwitchAll").alt = 'Expand All';
    }
  }

</script>

    <%
        String sWrksId = request.getParameter("WRKS_ID");
        if (StringHelper.isEmpty(sWrksId)) {
            throw new RuntimeException ("wrks_id request parameter not found");
        }
        DBConnection conn = JSPHelper.getConnection(request);
        int wrksId = Integer.parseInt(sWrksId);
        RuleTraceAccess acc = new RuleTraceAccess(conn);
        String traceXml = acc.getTraceByWrksId(wrksId);
        RuleTraceConfig cfg = RuleTraceConfig.getRuleTraceConfig(conn);
        boolean isEnabled = cfg.isEnabled(conn , wrksId);
        if (isEnabled) {
          // *** check empty XML for existing days where traceXML wouldnt be available, recalc to generate the xml
          if (StringHelper.isEmpty(traceXml)) {
            RuleEngine.runCalcGroup(conn, wrksId, true);
            traceXml = acc.getTraceByWrksId(wrksId);
          }
          RuleTraceList rtlist = RuleTraceList.fromXML(traceXml , cfg );
          out.println (makeHTML(rtlist , cfg , request)) ;
        }
        else {
          out.println ("<h3>Rule Trace is disabled for employee.Config parameters were enabled=(" + cfg.isEnabled() + ") applyToCalcGroups=(" + cfg.getApplyToCalcGroups() +")</h3>") ;
        }


    %>

</wb:page>