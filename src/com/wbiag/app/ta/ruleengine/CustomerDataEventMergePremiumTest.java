package com.wbiag.app.ta.ruleengine;

import java.util.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import com.wbiag.app.ta.quickrules.*;
import junit.framework.*;
/**
 * Test for CustomerDataEventMergePremiumTest.
 */
public class CustomerDataEventMergePremiumTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CustomerDataEventMergePremiumTest.class);

    public CustomerDataEventMergePremiumTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CustomerDataEventMergePremiumTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testMerge() throws Exception {

        getConnection().setAutoCommit(false);
        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CustomerDataEventMergePremium");

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start,
                                 start).execute(false);
        // *** create the rule
        Rule rule = new TestRule();
        clearAndAddRule(empId , start , rule , new Parameters());

        RuleEngine.runCalcGroup(getConnection() , empId , start, start, false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertEquals("Must be 1 wrkp" , 1, wps.size());

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

    public static class TestRule extends Rule {
        public List getParameterInfo(DBConnection conn) {
            List result = new ArrayList();
            return result;
        }

        public String getComponentName() {
            return "Test Rule";
        }

        public void execute(WBData wbData, Parameters parameters) throws Exception {
            wbData.insertWorkPremiumRecord(60 , "TRN");
            wbData.insertWorkPremiumRecord(360 , "TRN");
        }

    }
}
