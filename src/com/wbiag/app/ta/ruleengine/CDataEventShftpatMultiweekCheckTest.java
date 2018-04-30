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
 * Test for CDataEventShftpatMultiweekCheckTest.
 */
public class CDataEventShftpatMultiweekCheckTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventShftpatMultiweekCheckTest.class);

    public CDataEventShftpatMultiweekCheckTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CDataEventShftpatMultiweekCheckTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testMultiweekError() throws Exception {

        getConnection().setAutoCommit(false);

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventShftpatMultiweekCheck");

        final int empId = 15;
        final String shftPatName = "ROTATING";
        Date start = DateHelper.parseSQLDate("2004-11-05")  ;

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        InsertEmployeeOverride ins = new InsertEmployeeOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(DateHelper.DATE_3000);
        ins.setEmpShftpatName(shftPatName);

        ovrBuilder.add(ins);
        try {
            ovrBuilder.execute(false, false);
        }
        catch (OverrideException ex) {
            // *** till TT41007 fixed, the error is not gracefully handled
        }

    }

    /**
     * @throws Exception
     */
    public void testMultiweekNoError() throws Exception {

        getConnection().setAutoCommit(false);

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventShftpatMultiweekCheck");

        final int empId = 15;
        final String shftPatName = "ROTATING";
        Date start = DateHelper.parseSQLDate("2004-10-30")  ;

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        InsertEmployeeOverride ins = new InsertEmployeeOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(DateHelper.DATE_3000);
        ins.setEmpShftpatName(shftPatName);

        ovrBuilder.add(ins);
        ovrBuilder.execute(false, false);

        assertTrue(ovrBuilder.getOverridesProcessed().getOverrideData(0).getOvrStatus().equals(OverrideData.APPLIED));
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
