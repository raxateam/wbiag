package com.wbiag.app.ta.db;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.security.team.*;
import com.workbrain.sql.*;
import com.workbrain.tool.security.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for EmployeeTeamCacheTest.
 * @deprecated Core as of 5.0, wbt_id is now in work_Detail table and CalcDataCache
 */
public class EmployeeTeamCacheTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmployeeTeamCacheTest.class);

    public EmployeeTeamCacheTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(EmployeeTeamCacheTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void xCache() throws Exception {

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        String homeTeamName = SecurityEmployee.getHomeTeam(getConnection(),
            empId, new java.sql.Date(start.getTime()));

        String tempTeamName = "TEST_" + System.currentTimeMillis();
        WorkbrainTeamManager wtm = new WorkbrainTeamManager(getConnection());
        int wbtId = wtm.addTeam(tempTeamName, tempTeamName,
                    RuleHelper.getRootWorkbrainTeamId(getCodeMapper()) , 0);

        SecurityEmployee.addTempTeam(getConnection() , empId, wbtId,
                                     SQLHelper.getMinDate(), SQLHelper.getMaxDate() );

        addRule(empId, start, new TestRule(), new Parameters() );

        new CreateDefaultRecords(getConnection() ,new int[] {empId}, start, start ).execute(false);
        RuleEngine.runCalcGroup(getConnection(), empId, start, start, false);

        WorkSummaryData wsd = getWorkSummaryForDate(empId , start);
        assertTrue(homeTeamName.equals(wsd.getWrksUdf1()));
        assertTrue(wsd.getWrksUdf2().indexOf(tempTeamName ) > -1  );
    }

    /**
     * Dummy rule to populate wrksUdf1 = homeTeam, wrksUdf2 = tempTeams CSV
     */
    class TestRule extends Rule{

        public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {
            EmployeeTeamCache cache = new EmployeeTeamCache(wbData);
            wbData.setWrksUdf1(cache.getEmpHomeTeamName(wbData.getEmpId(),
                wbData.getWrksWorkDate()));
            Set tempTeams = cache.getEmpTempTeamNames(wbData.getEmpId(),
                wbData.getWrksWorkDate());

            wbData.setWrksUdf2(StringHelper.searchReplace(
                StringHelper.createCSVForCharacter(tempTeams), "'", ""));
        }

        public List getParameterInfo(DBConnection conn) {
            List result = new ArrayList();
            return result;
        }

        public String getComponentName() {
            return "Test";
        }

    }
    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
