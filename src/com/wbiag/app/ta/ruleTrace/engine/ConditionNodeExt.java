package com.wbiag.app.ta.ruleTrace.engine;

import com.wbiag.app.ta.ruleTrace.model.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
import java.util.*;
import java.io.*;
import java.text.*;


/**
 *  @deprecated Core as of 5.0.3.0
 */

public class ConditionNodeExt extends ConditionNode{

    private RuleTraceContent ruleTraceContent;
    private boolean eval;

    public ConditionNodeExt(ConditionSetNodeExt csExt) {
        super(csExt);
    }

    public ConditionNodeExt(ConditionNode cNode) {
        super(cNode.getParent());
        super.setCondition(cNode.getCondition());
        super.setConditionClassName(cNode.getConditionClass());
        super.setDescription(cNode.getDescription());
        super.setName(cNode.getName());
        super.setParameters(cNode.getParameters()) ;
    }

    public void setRuleTraceContent(RuleTraceContent v) {
        ruleTraceContent = v;
    }

    public RuleTraceContent getRuleTraceContent() {
        return ruleTraceContent;
    }

    public void setEval(boolean v) {
        eval = v;
    }

    public boolean isEval() {
        return eval;
    }

    public String toXML(RuleTraceConfig config) {
        StringBuffer sb = new StringBuffer();
        sb.append("   <condition "); ;
        sb.append(" name='").append(getName()).append("' ")
            .append(" eval='").append(isEval()).append("' ")
            .append("> \n");
        Enumeration enumP = getParameters().getParameters();
        while (enumP.hasMoreElements()) {
            Parm item = (Parm) enumP.nextElement();
            sb.append("    <parameter name='").append(item._name).append("' ")
                .append(" value='").append(item._value).append("'/> \n");
        }

        sb.append("   </condition>").append("\n");
        return sb.toString();

    }

}




