package com.wbiag.app.modules.blackout;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.workflow.*;
import com.wbiag.app.bo.*;
import com.workbrain.util.*;
import com.workbrain.tool.security.*;
import com.workbrain.test.*;
import junit.framework.*;

/**
 * WbiagBlkoutAccessTest
 */
public class WbiagBlkoutAccessTest extends TestCaseHW {
    private static Logger logger = Logger.getLogger(WbiagBlkoutAccessTest.class);

    public WbiagBlkoutAccessTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WbiagBlkoutAccessTest.class);
        return result;
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

    public void testInsertGroup() throws Exception{
        final String wbtName = "RETAILHEAD";
        final int wbtId = 11;
        final String grpName = "TEST_" + System.currentTimeMillis() ;

        WbiagBlkoutDtGrpData grp = new WbiagBlkoutDtGrpData();
        grp.setWiblkdgStartDate(DateHelper.getCurrentDate()) ;
        grp.setWbtId(new Integer(wbtId));
        grp.setWiblkdgName(grpName);
        grp.setWiblkdgDesc(grpName);

        WbiagBlkoutAccess acc = new WbiagBlkoutAccess(getConnection());
        acc.insertBlackoutDateGroup(grp);

        WbiagBlkoutDtGrpData grpAssert = acc.loadBlackoutDateGroup(grpName);
        assertNotNull(grpAssert) ;
        List dats  = acc.loadBlackoutDatesByGroup(grpName);
        assertTrue("2 subteams should be created", 2 == dats.size() );
        WbiagBlkoutDatData data1 =  (WbiagBlkoutDatData)dats.get(0);
        assertEquals("Group should be set", grpAssert.getWiblkdgId() , data1.getWiblkdgId());

        // *** test with team type
        acc.deleteBlackoutDateGroup(grpName);
        grp.setWiblkdgId(null);
        grp.setWbttId(new Integer(2));
        acc.insertBlackoutDateGroup(grp);
        dats  = acc.loadBlackoutDatesByGroup(grpName);
        assertTrue("1 subteam should be created", 1 == dats.size() );

        // *** test with team type
        acc.deleteBlackoutDateGroup(grpName);
        grp.setWiblkdgId(null);
        grp.setWbttId(null);
        acc.insertBlackoutDateGroup(grp, new int[] {12}, true);
        dats  = acc.loadBlackoutDatesByGroup(grpName);
        assertTrue("1 subteam should be created", 1 == dats.size() );

    }

    public void testIsEmp() throws Exception{


        final String grpName = "TEST_" + System.currentTimeMillis() ;
        int empId = 15;
        int homeTeamId = SecurityEmployee.getHomeTeamId(getConnection(), empId, com.workbrain.sql.SQLHelper.getCurrDate());
        final int wbtId = homeTeamId;

        WbiagBlkoutDtGrpData grp = new WbiagBlkoutDtGrpData();
        grp.setWiblkdgStartDate(DateHelper.getCurrentDate()) ;
        grp.setWbtId(new Integer(wbtId));
        grp.setWiblkdgName(grpName);
        grp.setWiblkdgDesc(grpName);

        WbiagBlkoutAccess acc = new WbiagBlkoutAccess(getConnection());
        acc.insertBlackoutDateGroup(grp);
        assertTrue(acc.isEmployeeOnBlackout(empId, DateHelper.getCurrentDate()));
        assertFalse(acc.isEmployeeOnBlackout(empId, DateHelper.addDays(DateHelper.getCurrentDate(), -10)));
    }

}
