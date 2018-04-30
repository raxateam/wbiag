<%@ include file="/system/wbheader.jsp"%>
<%@ taglib uri="/wbsys" prefix="wb" %>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="javax.servlet.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.scheduler.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.workbrain.app.export.process.*"%>
<%@ page import="com.wbiag.app.ta.ruleengine.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="org.apache.log4j.*"%>
<%!
    private static final Logger logger = Logger.getLogger("jsp.recalculateTaskParams.jobs");
    private static final String CLIENT_UI_PARAM =
      "sourceType=SQL source='SELECT client_id, client_name FROM workbrain_client' multiChoice=false";
    private String OVRTYP_UI_PARAM;
    private static final String BUSINESS_RANGE_PARAM =
      "labelList='" + RecalculateTask.PARAM_BUSINESS_RULE_RECALC_TYPE_WEEK + "," + RecalculateTask.PARAM_BUSINESS_RULE_RECALC_TYPE_PAYPERIOD + "' "
      + "valueList='" + RecalculateTask.PARAM_BUSINESS_RULE_RECALC_TYPE_WEEK + "," + RecalculateTask.PARAM_BUSINESS_RULE_RECALC_TYPE_PAYPERIOD + "' "
    ;
    private static final String APPLY_TO_CALCGRP_PARAM =
      "sourceType=SQL source='SELECT calcgrp_id, calcgrp_name FROM calc_group' multiChoice=true";

%>
<%-- ********************** --%>
<wb:page login="true">
<wb:define id="TYPE"><wb:get id="TYPE" scope="parameter" default=""/></wb:define>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>
<wb:define id="ovrtypIdsBusiness"><wb:get id="ovrtypIdsBusiness" scope="parameter" default=""/></wb:define>
<wb:define id="businessRangeType"><wb:get id="businessRangeType" scope="parameter" default=""/></wb:define>
<wb:define id="businessRangeInfinite"><wb:get id="businessRangeInfinite" scope="parameter" default=""/></wb:define>
<wb:define id="checkPending"><wb:get id="checkPending" scope="parameter" default=""/></wb:define>
<wb:define id="recalcThreadCnt"><wb:get id="recalcThreadCnt" scope="parameter" default=""/></wb:define>
<wb:define id="clientId"><wb:get id="clientId" scope="parameter" default=""/></wb:define>
<wb:define id="rolloutDate"><wb:get id="rolloutDate" scope="parameter" default=""/></wb:define>
<wb:define id="batchSize"><wb:get id="batchSize" scope="parameter" default=""/></wb:define>
<wb:define id="processCalcEmpTable"><wb:get id="processCalcEmpTable" scope="parameter" default=""/></wb:define>
<wb:define id="processNoShowEmployees"><wb:get id="processNoShowEmployees" scope="parameter" default=""/></wb:define>
<wb:define id="processJobRateUpdates"><wb:get id="processJobRateUpdates" scope="parameter" default=""/></wb:define>
<wb:define id="processCgDefChanges"><wb:get id="processCgDefChanges" scope="parameter" default=""/></wb:define>
<wb:define id="processEntEmpPolChanges"><wb:get id="processEntEmpPolChanges" scope="parameter" default=""/></wb:define>
<wb:define id="processClocksProcessed"><wb:get id="processClocksProcessed" scope="parameter" default=""/></wb:define>
<wb:define id="applyToCalcgrps"><wb:get id="applyToCalcgrps" scope="parameter" default=""/></wb:define>
<wb:define id="applyToCalcgrpsInclusive"><wb:get id="applyToCalcgrpsInclusive" scope="parameter" default=""/></wb:define>
<wb:define id="autoRecalc"><wb:get id="autoRecalc" scope="parameter" default=""/></wb:define>
<wb:define id="futureBalanceRecalc"><wb:get id="futureBalanceRecalc" scope="parameter" default=""/></wb:define>

<%
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new javax.servlet.jsp.JspException("No task id has been passed to the page");

  DBConnection conn = JSPHelper.getConnection(request);;
  DBServer dbs = DBServer.getServer(conn);

  if (dbs.isOracle()){
    OVRTYP_UI_PARAM =
    "sourceType=SQL source='SELECT ovrtyp_id, ovrtyp_name FROM override_type WHERE mod(ovrtyp_id, 100) = 0' multiChoice=true";
  }
  else if (dbs.isDB2()){
    OVRTYP_UI_PARAM =
    "sourceType=SQL source='SELECT ovrtyp_id, ovrtyp_name FROM override_type WHERE mod(ovrtyp_id, 100) = 0' multiChoice=true";
  }
  else if (dbs.isMSSQL()){
    OVRTYP_UI_PARAM =
      "sourceType=SQL source='SELECT ovrtyp_id, ovrtyp_name FROM override_type WHERE (ovrtyp_id % 100) = 0' multiChoice=true";
  }
  Map recparams = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
%>
<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
    <%
    if(recparams.isEmpty()) {
        recparams.put( RecalculateTask.PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC, "" );
        recparams.put( RecalculateTask.PARAM_BUSINESS_RULE_RECALC_RANGE_TYPE, RecalculateTask.PARAM_BUSINESS_RULE_RECALC_TYPE_PAYPERIOD );
        recparams.put( RecalculateTask.PARAM_BUSINESS_RULE_RECALC_INFINITE, "N" );
        recparams.put( RecalculateTask.PARAM_CHECK_PENDING_OVERRIDES, "N" );
        recparams.put( RecalculateTask.PARAM_CALCULATION_THREAD_COUNT, "1" );
        recparams.put( RecalculateTask.PARAM_CLIENT_ID, "1" );
        recparams.put( RecalculateTask.PARAM_ROLLOUT_DATE, "" );
        recparams.put( RecalculateTask.PARAM_BATCH_SIZE, String.valueOf(RecalculateTask.MAX_CALC_BATCH_SIZE) );
        recparams.put( RecalculateTask.PARAM_PROCESS_CALC_EMP_DATE_TABLE, "N" );
        recparams.put( RecalculateTask.PARAM_PROCESS_NO_SHOW_EMPLOYEES, "N" );
        recparams.put( RecalculateTask.PARAM_PROCESS_JOB_RATE_UPDATES, "N" );
        recparams.put( RecalculateTask.PARAM_PROCESS_CALCGRP_DEF_CHANGES, "N" );
        recparams.put( RecalculateTask.PARAM_PROCESS_ENTEMPPOLICY_CHANGES, "N" );
        recparams.put( RecalculateTask.PARAM_PROCESS_CLOCKS_PROCESSED, "N" );
        recparams.put( RecalculateTask.PARAM_APPLY_TO_CALCGRPS, "" );
        recparams.put( RecalculateTask.PARAM_APPLY_TO_CALCGRPS_INCLUSIVE, "Y" );
        recparams.put( RecalculateTask.PARAM_AUTO_RECALC, "True" );
        recparams.put( RecalculateTask.PARAM_FUTURE_BALANCE_RECALC, "" );
    }
    %>
    <wb:switch>
    <wb:case expression="#TYPE#" compareToExpression="">
      <input type="hidden" name="TYPE" value="scheduleTask">
      <wb:set id="ovrtypIdsBusiness"><%=recparams.get( RecalculateTask.PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC )%></wb:set>
      <wb:set id="businessRangeType"><%=recparams.get( RecalculateTask.PARAM_BUSINESS_RULE_RECALC_RANGE_TYPE )%></wb:set>
      <wb:set id="businessRangeInfinite"><%=recparams.get( RecalculateTask.PARAM_BUSINESS_RULE_RECALC_INFINITE )%></wb:set>
      <wb:set id="checkPending"><%=recparams.get( RecalculateTask.PARAM_CHECK_PENDING_OVERRIDES )%></wb:set>
      <wb:set id="recalcThreadCnt"><%=recparams.get( RecalculateTask.PARAM_CALCULATION_THREAD_COUNT )%></wb:set>
      <wb:set id="clientId"><%=recparams.get( RecalculateTask.PARAM_CLIENT_ID )%></wb:set>
      <wb:set id="rolloutDate"><%=recparams.get( RecalculateTask.PARAM_ROLLOUT_DATE )%></wb:set>
      <wb:set id="batchSize"><%=recparams.get( RecalculateTask.PARAM_BATCH_SIZE )%></wb:set>
      <wb:set id="processCalcEmpTable"><%=recparams.get( RecalculateTask.PARAM_PROCESS_CALC_EMP_DATE_TABLE )%></wb:set>
      <wb:set id="processNoShowEmployees"><%=recparams.get( RecalculateTask.PARAM_PROCESS_NO_SHOW_EMPLOYEES )%></wb:set>
      <wb:set id="processJobRateUpdates"><%=recparams.get( RecalculateTask.PARAM_PROCESS_JOB_RATE_UPDATES )%></wb:set>
      <wb:set id="processCgDefChanges"><%=recparams.get( RecalculateTask.PARAM_PROCESS_CALCGRP_DEF_CHANGES )%></wb:set>
      <wb:set id="processEntEmpPolChanges"><%=recparams.get( RecalculateTask.PARAM_PROCESS_ENTEMPPOLICY_CHANGES )%></wb:set>
      <wb:set id="processClocksProcessed"><%=recparams.get( RecalculateTask.PARAM_PROCESS_CLOCKS_PROCESSED )%></wb:set>
      <wb:set id="applyToCalcgrps"><%=recparams.get( RecalculateTask.PARAM_APPLY_TO_CALCGRPS )%></wb:set>
      <wb:set id="applyToCalcgrpsInclusive"><%=recparams.get( RecalculateTask.PARAM_APPLY_TO_CALCGRPS_INCLUSIVE )%></wb:set>
      <wb:set id="autoRecalc"><%=recparams.get( RecalculateTask.PARAM_AUTO_RECALC )%></wb:set>
      <wb:set id="futureBalanceRecalc"><%=recparams.get( RecalculateTask.PARAM_FUTURE_BALANCE_RECALC )%></wb:set>

      <wba:table caption="Recalculate Task" captionLocalizeIndex="Recalculate_Task" width="600">

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC">PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="ovrtypIdsBusiness" ui='DBLookupUI' uiParameter='<%=OVRTYP_UI_PARAM%>'><%=ovrtypIdsBusiness%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_BUSINESS_RULE_RECALC_RANGE_TYPE">PARAM_BUSINESS_RULE_RECALC_RANGE_TYPE</wb:localize>
          </wba:th>
          <wba:td>
            <wba:table>
            <wba:tr>
              <wba:td>
               <wb:controlField cssClass='inputField' submitName="businessRangeType" ui='ComboBoxUI' uiParameter="<%=BUSINESS_RANGE_PARAM%>"><%=businessRangeType%></wb:controlField>
              </wba:td>
              <wba:td>
               <wb:localize id="PARAM_BUSINESS_RULE_RECALC_INFINITE">Infinite :</wb:localize>
               <wb:controlField cssClass='inputField' submitName="businessRangeInfinite" ui='CheckboxUI' uiParameter=''><%=businessRangeInfinite%></wb:controlField>
              </wba:td>
            </wba:tr>
            </wba:table>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_CHECK_PENDING_OVERRIDES">PARAM_CHECK_PENDING_OVERRIDES</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="checkPending" ui='CheckboxUI' uiParameter=''><%=checkPending%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_CALCULATION_THREAD_COUNT">PARAM_CALCULATION_THREAD_COUNT</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="recalcThreadCnt" ui='NumberUI' uiParameter=''><%=recalcThreadCnt%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_CLIENT_ID">PARAM_CLIENT_ID</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="clientId" ui='DBLookupUI' uiParameter='<%=CLIENT_UI_PARAM%>'><%=clientId%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_ROLLOUT_DATE">PARAM_ROLLOUT_DATE</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="rolloutDate" ui='DatePickerUI' uiParameter=''><%=rolloutDate%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_BATCH_SIZE">PARAM_BATCH_SIZE</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="batchSize" ui='NumberUI' uiParameter=''><%=batchSize%></wb:controlField>
          </wba:td>
        </wba:tr>


        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_CALC_EMP_DATE_TABLE">PARAM_PROCESS_CALC_EMP_DATE_TABLE</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processCalcEmpTable" ui='CheckboxUI' uiParameter=''><%=processCalcEmpTable%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_NO_SHOW_EMPLOYEES">PARAM_PROCESS_NO_SHOW_EMPLOYEES</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processNoShowEmployees" ui='CheckboxUI' uiParameter=''><%=processNoShowEmployees%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_JOB_RATE_UPDATES">PARAM_PROCESS_JOB_RATE_UPDATES</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processJobRateUpdates" ui='CheckboxUI' uiParameter=''><%=processJobRateUpdates%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_CALCGRP_DEF_CHANGES">PARAM_PROCESS_CALCGRP_DEF_CHANGES</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processCgDefChanges" ui='CheckboxUI' uiParameter=''><%=processCgDefChanges%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_ENTEMPPOLICY_CHANGES">PARAM_PROCESS_ENTEMPPOLICY_CHANGES</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processEntEmpPolChanges" ui='CheckboxUI' uiParameter=''><%=processEntEmpPolChanges%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_CLOCKS_PROCESSED">PARAM_PROCESS_CLOCKS_PROCESSED</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processClocksProcessed" ui='CheckboxUI' uiParameter=''><%=processClocksProcessed%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_APPLY_TO_CALCGRPS">PARAM_APPLY_TO_CALCGRPS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="applyToCalcgrps" ui='DBLookupUI' uiParameter='<%=APPLY_TO_CALCGRP_PARAM%>'><%=applyToCalcgrps%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_APPLY_TO_CALCGRPS_INCLUSIVE">PARAM_APPLY_TO_CALCGRPS_INCLUSIVE</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="applyToCalcgrpsInclusive" ui='CheckboxUI' uiParameter=''><%=applyToCalcgrpsInclusive%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_AUTO_RECALC">PARAM_AUTO_RECALC</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="autoRecalc" ui='ComboBoxUI' uiParameter="valueList=',FALSE,TRUE' labelList='Default,False,True'"><%=autoRecalc%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_FUTURE_BALANCE_RECALC">PARAM_FUTURE_BALANCE_RECALC</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="futureBalanceRecalc" ui='ComboBoxUI' uiParameter="valueList=',FALSE,TRUE' labelList='Default,False,True'"><%=futureBalanceRecalc%></wb:controlField>
          </wba:td>
        </wba:tr>
      </wba:table>
      <div class="separatorLarge"></div>

      <wba:button type="submit" label="Submit" labelLocalizeIndex="Submit" onClick=""/>&nbsp;
      <wba:button label="cancel" labelLocalizeIndex="Cancel" onClick="#okOnClickUrl#"/>
    </wb:case>
    <wb:case expression="#TYPE#" compareToExpression="scheduleTask">
        <%
            recparams.put( RecalculateTask.PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC , ovrtypIdsBusiness.toString() );
            recparams.put( RecalculateTask.PARAM_BUSINESS_RULE_RECALC_RANGE_TYPE , businessRangeType.toString() );
            recparams.put( RecalculateTask.PARAM_BUSINESS_RULE_RECALC_INFINITE , businessRangeInfinite.toString() );
            recparams.put( RecalculateTask.PARAM_CHECK_PENDING_OVERRIDES , checkPending.toString() );
            recparams.put( RecalculateTask.PARAM_CALCULATION_THREAD_COUNT , recalcThreadCnt.toString() );
            recparams.put( RecalculateTask.PARAM_CLIENT_ID , clientId.toString() );
            recparams.put( RecalculateTask.PARAM_ROLLOUT_DATE , rolloutDate.toString() );
            recparams.put( RecalculateTask.PARAM_BATCH_SIZE , batchSize.toString() );
            recparams.put( RecalculateTask.PARAM_PROCESS_CALC_EMP_DATE_TABLE , processCalcEmpTable.toString() );
            recparams.put( RecalculateTask.PARAM_PROCESS_NO_SHOW_EMPLOYEES , processNoShowEmployees.toString() );
            recparams.put( RecalculateTask.PARAM_PROCESS_JOB_RATE_UPDATES , processJobRateUpdates.toString() );
            recparams.put( RecalculateTask.PARAM_PROCESS_CALCGRP_DEF_CHANGES , processCgDefChanges.toString() );
            recparams.put( RecalculateTask.PARAM_PROCESS_ENTEMPPOLICY_CHANGES , processEntEmpPolChanges.toString() );
            recparams.put( RecalculateTask.PARAM_PROCESS_CLOCKS_PROCESSED , processClocksProcessed.toString() );
            recparams.put( RecalculateTask.PARAM_APPLY_TO_CALCGRPS , applyToCalcgrps.toString() );
            recparams.put( RecalculateTask.PARAM_APPLY_TO_CALCGRPS_INCLUSIVE , applyToCalcgrpsInclusive.toString() );
            recparams.put( RecalculateTask.PARAM_AUTO_RECALC , autoRecalc.toString() );
            recparams.put( RecalculateTask.PARAM_FUTURE_BALANCE_RECALC , futureBalanceRecalc.toString() );
            scheduler.setTaskParams( Integer.parseInt( TASK_ID.toString() ), recparams );
        %>
        <Span>RecalculateTask Successfully Scheduled</Span>
        <br>
        <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>
    </wb:case>
    </wb:switch>
</wb:if>
</wb:page>
