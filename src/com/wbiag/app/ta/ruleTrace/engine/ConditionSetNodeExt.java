package com.wbiag.app.ta.ruleTrace.engine;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
import java.util.*;
import com.wbiag.app.ta.ruleTrace.model.*;

/**
 *  @deprecated Core as of 5.0.3.0
 */

public class ConditionSetNodeExt extends ConditionSetNode{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ConditionSetNodeExt.class);
    private RuleTraceContent ruleTraceContent;

    public boolean isEval(){
        boolean ret = false;
        Enumeration enum = getConditions();
        while( enum.hasMoreElements() ) {
            ConditionNodeExt cNode = (ConditionNodeExt) enum.nextElement();
            if (cNode.isEval()) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    public ConditionSetNodeExt(RuleNodeExt rule) {
        super(rule);
    }

    public ConditionSetNodeExt(ConditionSetNode csetNode) {
        super(csetNode.getParent());
        super.setDescription(csetNode.getDescription());
        super.setParameters(csetNode.getParameters());

        Enumeration enum = csetNode.getConditions();
        while( enum.hasMoreElements() ) {
            ConditionNodeExt cNode = new ConditionNodeExt((ConditionNode) enum.nextElement());
            addConditionNode(cNode) ;
        }
    }

    public ConditionNodeExt getLastConditionNode() {
        ConditionNodeExt lastnode = null;
        Enumeration enum = getConditions();
        while( enum.hasMoreElements() ) {
            lastnode  = (ConditionNodeExt) enum.nextElement();
        }
        return lastnode;
    }

    public boolean evaluate( WBData wbData ) throws NestedRuntimeException {
        Enumeration enum = getConditions();
        while (enum.hasMoreElements())
        {
            ConditionNodeExt conditionNode = (ConditionNodeExt) enum.nextElement();
            boolean eval = conditionNode.evaluate( wbData );
            /** @todo BC */
            beforeCondition(wbData , conditionNode, eval );
            if ( !eval )
                return false;
            /** @todo BC */
            afterCondition(wbData , conditionNode, eval);
        }

        // All test have return true so execute the rule.
        return true;
    }

    private void beforeCondition(WBData data, ConditionNodeExt condition, boolean eval) {
        condition.setEval(eval);
    }

    private void afterCondition(WBData data, ConditionNode condition, boolean eval) {
    }



    public void setRuleTraceContent(RuleTraceContent v){
        ruleTraceContent = v;
    }

    public RuleTraceContent getRuleTraceContent(){
        return ruleTraceContent;
    }


    public String toXML(RuleTraceConfig config){
        StringBuffer sb = new StringBuffer();
        sb.append("  <conditionset "); ;
        sb.append(" name='").append(getDescription()).append("' ")
            .append(" eval='").append(isEval()).append("' ")
            .append("> \n") ;
        Enumeration enumP = getParameters().getParameters();
        while (enumP.hasMoreElements()) {
            Parm item = (Parm)enumP.nextElement();
            sb.append("   <parameter name='").append(item._name).append("' ")
                .append(" value='").append(item._value).append("'/> \n") ;
        }
        Enumeration enumC = getConditions();
        while (enumC.hasMoreElements()) {
            ConditionNodeExt item = (ConditionNodeExt)enumC.nextElement();
            sb.append(item.toXML(config)) ;
        }
        if (getRuleTraceContent() != null) {
            sb.append(getRuleTraceContent().toXMLData(config));
        }
        sb.append("  </conditionset>").append("\n") ;
        return sb.toString();

    }
}



