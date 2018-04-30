package com.wbiag.app.ta.ruleTrace.engine  ;

import com.wbiag.app.ta.ruleTrace.model.*;
import com.wbiag.app.ta.ruleTrace.engine.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.CalcGroupData;
import com.workbrain.app.ta.model.RuleData;
import com.workbrain.app.ta.db.RuleAccess;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;

import com.workbrain.util.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;

/**
 *  @deprecated Core as of 5.0.3.0
 */

public class CalculationGroupExt extends CalculationGroup {

    public CalculationGroupExt() {
        super();
    }

    public CalculationGroupExt(CalculationGroup cg) throws Exception{
        super(cg.getData());
        super.setAutoRecalc(cg.getAutoRecalc());
        super.setCalcPeriod(cg.getCalcPeriod());
        super.setDescription(cg.getDescription());
        super.setName(cg.getDescription());
        Enumeration enum = cg.getRuleNodes();
        while (enum.hasMoreElements()) {
            RuleNodeExt ruleNode =
                    new RuleNodeExt((RuleNode) enum.nextElement());
            super.addRuleNode(ruleNode);
        }
    }

    // *** Runs the rules at a certain execution point ***
    public void run(RuleData rd, DBConnection conn, WBData wbData, Integer executionPoint) throws Exception {
        if( wbData == null ) {
            wbData = new WBData( rd, conn );
        }

        Enumeration enum = getRuleNodes();
        while (enum.hasMoreElements()) {
            RuleNodeExt ruleNode =
                    (RuleNodeExt) enum.nextElement();
            if (ruleNode.isActive()) {
                if (executionPoint == null ||
                    ruleNode.getExecutionPoint() == executionPoint.intValue()) {
                    ruleNode.execute(wbData);
                }
            }
            wbData.initialize();
        }
    }

}

