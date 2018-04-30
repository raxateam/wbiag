package com.wbiag.app.ta.ruleTrace.engine ;

import com.wbiag.util.NameValueList;
import com.wbiag.app.ta.ruleTrace.*;
import com.wbiag.app.ta.ruleTrace.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
import java.util.*;
import java.io.*;
import java.text.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.xerces.parsers.*;
import org.apache.log4j.*;
import com.jamonapi.*;
import com.workbrain.util.monitor.*;
import java.sql.*;
/**
 *  @deprecated Core as of 5.0.3.0
 */

public class RuleNodeExt extends RuleNode{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RuleNodeExt.class);

    private RuleTraceContent ruleTraceContent;

    public RuleNodeExt(CalculationGroup calcGroup) {
        super(calcGroup);
    }

    public RuleNodeExt(RuleNode ruleNode) {
        super(ruleNode.getParent());
        super.setDescription(ruleNode.getDescription());
        super.setRule(ruleNode.getRule());
        super.setRuleName(ruleNode.getRuleName());
        super.setExecutionPoint(ruleNode.getExecutionPoint());
        super.setIsActive(ruleNode.isActive());
        super.setRuleClassName(ruleNode.getRuleClass());
        Enumeration enum = ruleNode.getConditionSets();
        while( enum.hasMoreElements() ) {
            ConditionSetNodeExt cset = new ConditionSetNodeExt((ConditionSetNode) enum.nextElement());
            addConditionSetNode(cset) ;
        }
    }

    public boolean isEval() {
        boolean ret = false;
        Enumeration enum = getConditionSets();
        while( enum.hasMoreElements() ) {
            ConditionSetNodeExt cset = (ConditionSetNodeExt) enum.nextElement();
            if (cset.isEval()) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    public ConditionSetNodeExt getLastConditionSetNode() {
        ConditionSetNodeExt lastnode = null;
        Enumeration enum = getConditionSets();
        while( enum.hasMoreElements() ) {
            lastnode  = (ConditionSetNodeExt) enum.nextElement();
        }
        return lastnode;
    }

    public String toXML(RuleTraceConfig config){
        StringBuffer sb = new StringBuffer();
        sb.append(" <rule "); ;
        sb.append(" name='").append(getRuleName()).append("'>").append("\n") ;
        Enumeration enumC = getConditionSets();
        while (enumC.hasMoreElements()) {
            ConditionSetNodeExt item = (ConditionSetNodeExt)enumC.nextElement();
            sb.append(item.toXML(config)) ;
        }
        sb.append(" </rule>").append("\n") ;
        return sb.toString();

    }

    public void execute( WBData wbData ) throws Exception {
        /** @todo BC */
        beforeRuleNode(wbData , this);
        RuleNodeExt rext = (RuleNodeExt)CDataEventRuleTrace.getRuleTraceList(wbData).get(CDataEventRuleTrace.getRuleTraceList(wbData).size() - 1);
        Enumeration enum = rext.getConditionSets();
        while (enum.hasMoreElements()) {
            ConditionSetNodeExt rcNode = (ConditionSetNodeExt) enum.nextElement();
            /** @todo BC */
            beforeConditionSet(wbData , rcNode);
            boolean eval = rcNode.evaluate(wbData);
            if (eval) {
                //we get a list of RuleParameterInfos from the rule.
                List rpiList = getRule().getParameterInfo(wbData.getDBconnection());
                /** @todo BC */
                //rcNode.getParameters().setRuleParameterInfo(rpiList);
                //If a parameter is required, ensure that value is provided
                for (int j = 0; j < rpiList.size(); j++){
                    RuleParameterInfo info = (RuleParameterInfo) rpiList.get(j);
                    if (!info.isOptional() && (info.getDefaultValue() == null || info.getDefaultValue() == "") ){
                        String paramVal = rcNode.getParameters().getParameter(info.getName());
                        if (paramVal == null || paramVal == ""){
                            throw new Exception(ErrorMessageHelper.getMLString(
                                    "VALUE_MISSING_FOR_REQUIRED_PARAMETER_IN",
                                    "Value missing for required parameter {0} in {1}.",
                                    new String[] {info.getName(), getRule().getComponentName()}));
                        }
                    }
                }
                Monitor ruleMonitor = null;
                try {
                    if (WBMonitorFactory.isMonitorEnabled()) {
                        try {
                            ruleMonitor =
                                    WBMonitorFactory.start(RuleAccess.MNTR_RULEENGINE_RULE
                                        + "." + getRule().getComponentName()
                                        + "." + Thread.currentThread().getName());
                        } catch(Throwable t) {
                            logger.error("Rule monitor exception for "
                                    + RuleAccess.MNTR_RULEENGINE_RULE
                                    + "." + getRule().getComponentName()
                                    + "." + Thread.currentThread().getName(), t);
                        }
                    }
                    getRule().execute(wbData, rcNode.getParameters());
                }
                finally  {
                    if (ruleMonitor!= null) {
                        try {
                            ruleMonitor.stop();
                        } catch(Throwable t) {
                            logger.error("Rule monitor exception for "
                                    + RuleAccess.MNTR_RULEENGINE_RULE
                                    + "." + getRule().getComponentName()
                                    + "." + Thread.currentThread().getName(), t);
                        }
                    }
                }
                String ruleName = getRule().getComponentName();
                if (!StringHelper.isEmpty(ruleName)) {
                    String existingList = wbData.getRuleData().getWorkSummary().
                        getWrksRulesApplied();
                    if (!StringHelper.isEmpty(existingList)) {
                        existingList += ",";
                    }
                    wbData.getRuleData().getWorkSummary().setWrksRulesApplied( (
                        existingList == null ? "" : existingList) + ruleName);
                }
                try {
                    wbData.getRuleData().getWorkDetails().validate();
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(ruleName + " : " + e.getMessage());
                }

                if (getRule().conditionSetExecutionIsMutuallyExclusive()) {
                    afterConditionSet(wbData , rcNode);
                    break;
                }
            }
            afterConditionSet(wbData , rcNode);
        }
        /** @todo BC */
        afterRuleNode(wbData , this);
    }

    private void beforeConditionSet(WBData data, ConditionSetNodeExt conditionSet) {
    }

    private void afterConditionSet(WBData data, ConditionSetNodeExt conditionSet) {
        conditionSet.setRuleTraceContent(populateRuleTraceContent(data));
    }



    private void beforeRuleNode(WBData data, RuleNode rule) {
        if (logger.isDebugEnabled()) logger.debug("starting rulenode :" + rule.getRuleName() );
        RuleNodeExt rext = new RuleNodeExt(rule);

        CDataEventRuleTrace.getRuleTraceList(data).add(rext);
    }

    private void afterRuleNode(WBData data, RuleNode rule) {
    }

    private RuleTraceContent populateRuleTraceContent(WBData data) {

        RuleTraceContent cont = new RuleTraceContent();
        RuleTraceConfig config = RuleTraceConfig.getRuleTraceConfig(data.getCodeMapper());

        EmployeeData ed = data.getRuleData().getEmployeeData();
        ed.setCodeMapper(data.getCodeMapper() );
        cont.setEmployeeData(NameValueList.toNameValueList(ed , config.getEmployeeFields() )) ;

        WorkSummaryData ws = (WorkSummaryData)data.getRuleData().getWorkSummary().duplicate();
        ws.setCodeMapper(data.getCodeMapper() );
        cont.setWorkSummaryData(NameValueList.toNameValueList(ws , config.getWorkSummaryFields())) ;


        WorkDetailList wdl = data.getRuleData().getWorkDetails().duplicate() ;
        wdl.setCodeMapper(data.getCodeMapper() ) ;
        cont.setWorkDetails(NameValueList.toListOfNameValueLists(wdl , config.getWorkDetailFields() )) ;

        WorkDetailList wpl = data.getRuleData().getWorkPremiums().duplicate() ;
        wpl.setCodeMapper(data.getCodeMapper() ) ;
        cont.setWorkPremiums(NameValueList.toListOfNameValueLists(wpl , config.getWorkPremiumFields() )) ;

        NameValueList empBals = new NameValueList();
        cont.setEmployeeBalances(empBals);
        if (config.getEmployeeBalances() != null) {
            Iterator iter = config.getEmployeeBalances().iterator();
            while (iter.hasNext()) {
                String bal = (String) iter.next();
                try {
                    empBals.add(new NameValue(bal,  String.valueOf(data.getEmployeeBalanceValue(bal))));
                }
                catch (Exception ex) {
                    throw new NestedRuntimeException(
                        "Error in getting balance value for bal:" + bal);
                }
            }
        }
        return cont;
    }

    /**
     * Returns if any data has changes in any of condition sets compared to previous rule/condition set
     * @return
     */
    public boolean isChangedFromPrevious() {
        boolean ret = false;
        Enumeration enum = getConditionSets();
        while( enum.hasMoreElements() ) {
            ConditionSetNodeExt cset  = (ConditionSetNodeExt) enum.nextElement();
            ret |= (cset.getRuleTraceContent() != null && cset.getRuleTraceContent().isChangedFromPrevious());
        }
        return ret;
    }
}



