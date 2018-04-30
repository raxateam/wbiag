<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.*"%>
<%@ page import="com.workbrain.server.sql.*"%>
<%@ page import="com.workbrain.server.jsp.taglib.util.*"%>
<%@ page import="com.workbrain.app.ta.ruleengine.*"%>
<%@ page import="com.workbrain.app.ta.db.*"%>
<%@ page import="com.workbrain.app.ta.model.*"%>
<%-- ********************** --%>
<wb:page login="true">
<script language="JavaScript">
	function submitForm(operation) {
        if (Trim(document.forms[0].RULEANALYZE_CALCGRPS.value) == '') {
          document.forms[0].RULEANALYZE_CALCGRPS.value = 'ALL';
        }
	    document.forms[0].OPERATION.value = operation;
        document.forms[0].submit();
	}
</script>
<%!
    public Map calcGrpAnalyze(DBConnection conn,String calcgrpIds,
        boolean sortByUsage, boolean asc, String customCore) {
        CalcGroupAccess cga = new CalcGroupAccess(conn);
        CalcDataCache cdc = CalcDataCache.createCalcDataCache(conn);
        List calcgrps = cga.loadRecordData(new CalcGroupData(),
            "CALC_GROUP",
            StringHelper.isEmpty(calcgrpIds) ? "calcgrp_id > 0 " : "calcgrp_id IN (" + calcgrpIds + ")");

        Map ret = new HashMap();
        List conds = new ArrayList(); ret.put("CONDS", conds);
        List rules = new ArrayList(); ret.put("RULES", rules);

        Iterator iter = calcgrps.iterator();
        while (iter.hasNext()) {
            CalcGroupData cgData = (CalcGroupData) iter.next();
            CalculationGroup xmlCg = cdc.getCalcGroup(conn, cgData.getCalcgrpId(),
                DateHelper.truncateToDays(new Date()));
            Enumeration r = xmlCg.getRuleNodes();
            while (r.hasMoreElements()) {
                RuleNode ruleNode =
                    (RuleNode) r.nextElement();
                //if (ruleNode.isActive()) {
                    Enumeration cs = ruleNode.getConditionSets();
                    while (cs.hasMoreElements()) {
                        ConditionSetNode condSetNode = (ConditionSetNode) cs.
                            nextElement();
                        Enumeration c = condSetNode.getConditions();
                        while (c.hasMoreElements()) {
                            ConditionNode condNode = (ConditionNode) c.
                                nextElement();
                            String name = condNode.getCondition().getComponentName()  ;
                            RuleEntity ent = new RuleEntity(name , condNode.getConditionClass());
                            addRuleEntity(ent , conds, customCore, cgData)  ;
                        }
                    }
                    String name = ruleNode.getRule().getComponentName()  ;
                    RuleEntity ent = new RuleEntity(name , ruleNode.getRuleClass());
                    addRuleEntity(ent , rules, customCore, cgData)  ;
                //}
            }
        }

        if (sortByUsage) {
            Collections.sort(conds, new ComparatorUsage(asc));
            Collections.sort(rules, new ComparatorUsage(asc));

        }

        return ret;
    }

    private void addRuleEntity(RuleEntity ent, List list, String customCore, CalcGroupData cgData) {
        boolean shouldAdd = "ALL".equals(customCore)
          || (ent.isCore()     &&  "CORE".equals(customCore))
          || (!ent.isCore()     &&  "CUSTOM".equals(customCore));
        if (shouldAdd) {
          int ind = list.indexOf(ent);
          if (ind >= 0) {
            ent = (RuleEntity)list.get(ind);
          }
          else {
            list.add(ent);
          }
          if (ent.calcgrps.indexOf(cgData.getCalcgrpName()) == -1) {
            ent.calcgrps.add(cgData.getCalcgrpName());
          }
        }
    }

    private class ComparatorUsage implements Comparator {
        boolean asc = true;

        public ComparatorUsage(boolean asc) {
            this.asc = asc;
        }

        public int compare(Object o1, Object o2){
            RuleEntity od1 = (RuleEntity)o1;
            RuleEntity od2 = (RuleEntity)o2;
            int compareResult = od1.calcgrps.size() == od2.calcgrps.size()
                ? 0
                : (od1.calcgrps.size() < od2.calcgrps.size() ? asc ? -1  : 1: asc ? 1 : -1);
            return compareResult;
        }
    }

    private class RuleEntity {
        String name;
        String classPath;
        List  calcgrps = new ArrayList();

        public RuleEntity(String name, String classPath) {
            this.name = name;
            this.classPath = classPath;
        }

        public boolean isCore() {
            return classPath.startsWith("com.workbrain");
        }

        public boolean equals(Object o){
            RuleEntity comp = (RuleEntity)o;
            return StringHelper.equals(name, comp.name) ;
        }

        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

        public String toString() {
            return "name : " + name + "\n"
                + "calcgrps : " + StringHelper.createCSVForCharacter(calcgrps)+ "\n";
        }
    }

   String contextPath = null ;
   public String buttons (String mfrmId) {
     StringBuffer sb = new StringBuffer(200);
     sb.append("<table  class='contentTable' cellspacing=0 style='border-width:1px;'><tr>");
     sb.append("<td><button type='button' onClick=\"submitForm('SUBMIT');\" class='buttonMedium' >Submit</button></td>");
     sb.append("<td><button type='button' onClick=\"location.href = '../../maintenance/mntForms.jsp?mfrm_id=" + mfrmId + "'; return false;\" class='buttonMedium'  >Cancel</button></td>");
     sb.append("</tr></table>");
     return sb.toString();
   }

   public String dataTable(List data){
      StringBuffer sb = new StringBuffer(200);
      sb.append("<table class='contentTable' cellspacing=0 style='border-width:1px;'>");
      sb.append("<tr><th>Cond name</th><th># of Calcgrps Using</th><th>Calcgrps</th></tr>");
      Iterator iter = data.iterator();
      while (iter.hasNext()) {
        RuleEntity item = (RuleEntity) iter.next();
        sb.append("<tr><td>" + item.name + "</td>"
             + "<td>" + item.calcgrps.size()  + "</td>"
             + "<td>" + StringHelper.createCSVForCharacter(item.calcgrps)  + "</td>"
             + "</tr>");
      }
      sb.append("</table>");
      return sb.toString();
   }
%>
<%
    contextPath = (String)request.getContextPath() ;
    String mfrm_id  = (String)request.getParameter("mfrm_id") ;
    String goBack =  "location.href = '../../maintenance/mntForms.jsp?mfrm_id=" + mfrm_id + "'; return false;";

    if (StringHelper.isEmpty(mfrm_id)) {
        throw new RuntimeException ("mfrm_id must be supplied");
    }

    DBConnection conn = JSPHelper.getConnection(request);

    String operation = (String)request.getParameter("OPERATION") ;
    String calcgrps = (String)request.getParameter("RULEANALYZE_CALCGRPS") ;
    String doRules = (String)request.getParameter("RULEANALYZE_DO_RULES") ;
    String doConds = (String)request.getParameter("RULEANALYZE_DO_CONDS") ;
    String sortUsage = (String)request.getParameter("RULEANALYZE_SORT_USAGE") ;
    String customCore = (String)request.getParameter("RULEANALYZE_CUSTOM_CORE") ;

    if ("SUBMIT".equals(operation)) {

        String errMsg =  null;
        Map ret = null;
        try {
            ret = calcGrpAnalyze(JSPHelper.getConnection(request),
                 "ALL".equals(calcgrps) ? null : calcgrps
                , true
                , "Y".equals(sortUsage) ? false : true,
                customCore);
        }
        catch (Exception e){
          errMsg = e.getMessage() + "<br> Trace:" + StringHelper.getStackTrace(e) ;
        }
        if (!StringHelper.isEmpty(errMsg)) {
        %>
            <span fgColor="red">Rule Analyze Process could not run. <br> Error Message: <%=errMsg %></span>
        <%
        }

        if (ret != null) {
         if ("Y".equals(doRules)) {
            out.println("<h2>Rules</h2>");
            out.println(dataTable((List)ret.get("RULES")));
         }
         if ("Y".equals(doConds)) {
            out.println("<h2>Conditions</h2>");
            out.println(dataTable((List)ret.get("CONDS")));
         }
        }
    }
%>

    <wba:table caption="Rule Analyze Tool" captionLocalizeIndex="Rule Analyze Tool">
        <wba:tr>
          <wba:th>
            <wb:localize id="RULEANALYZE_CALCGRPS">Calcgroups to Analyze</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="RULEANALYZE_CALCGRPS" ui='DBLookupUI' uiParameter='sourceType=SQL source=\"SELECT calcgrp_id, calcgrp_name FROM calc_group WHERE calcgrp_id >0\" multiChoice=true all=true'><%=StringHelper.isEmpty(calcgrps) ? "ALL" : calcgrps%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="RULEANALYZE_DO_RULES">Analyze Rules</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="RULEANALYZE_DO_RULES" ui='CheckboxUI' uiParameter=''><%=StringHelper.isEmpty(doRules) ? "Y" : doRules%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="RULEANALYZE_DO_CONDS">Analyze Conditions</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="RULEANALYZE_DO_CONDS" ui='CheckboxUI' uiParameter=''><%=StringHelper.isEmpty(doConds) ? "Y" : doConds%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="RULEANALYZE_SORT_USAGE">Sort Usage Descending</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="RULEANALYZE_SORT_USAGE" ui='CheckboxUI' uiParameter=''><%=StringHelper.isEmpty(sortUsage) ? "Y" : sortUsage%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="RULEANALYZE_CUSTOM_CORE">Custom / Core</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="RULEANALYZE_CUSTOM_CORE" ui='ComboBoxUI' uiParameter='valueList=\"ALL,CUSTOM,CORE\"'><%=StringHelper.isEmpty(customCore) ? "ALL" : customCore%></wb:controlField>
          </wba:td>
        </wba:tr>

    </wba:table>
    <wb:submit id="mfrm_id"><%=mfrm_id%></wb:submit>
    <wb:submit id="OPERATION"></wb:submit>
<%
    out.println(buttons(mfrm_id));
%>
</wb:page>
