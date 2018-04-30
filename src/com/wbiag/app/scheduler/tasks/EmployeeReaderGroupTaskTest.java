/// TESTED ON IAG01DV

package com.wbiag.app.scheduler.tasks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import junit.framework.TestSuite;
import com.wbiag.app.ta.db.EmployeeDateEffRdrgrpAccess;
import com.wbiag.app.ta.db.EmployeeReaderAccess;
import com.wbiag.app.ta.model.EmployeeDateEffRdrgrpData;
import com.wbiag.app.ta.ruleengine.DataEventTestCase;
import com.workbrain.app.scheduler.enterprise.ScheduledJob;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.EmployeeAccess;
import com.workbrain.app.ta.db.ShiftAccess;
import com.workbrain.app.ta.model.CalcGroupData;
import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.app.ta.model.PayGroupData;
import com.workbrain.app.ta.model.ShiftData;
import com.workbrain.app.ta.model.ShiftPatternData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;


public class EmployeeReaderGroupTaskTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger
    .getLogger(EmployeeReaderGroupTaskTest.class);

    int loanedRdrGrpId2= 100;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(EmployeeReaderGroupTaskTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO reader_group(rdrgrp_id, rdrgrp_name, rdrsvr_id) VALUES (?,?,?)");
            ps = getConnection().prepareStatement(sb.toString());
            loanedRdrGrpId2 = getConnection().getDBSequence("seq_rdrgrp_id").getNextValue();
            ps.setInt(1  ,  loanedRdrGrpId2 );
            ps.setString(2, "DUMMY1");
            ps.setInt(3, 55);
            ps.addBatch();
            //ps.setInt(1  , homeRdrGrpId  );
            //ps.setString(2, "DUMMY2");
            //ps.setInt(3, 55);
            //ps.addBatch();

            ps.executeBatch();
        }
        finally {
            if (ps != null) ps.close();
        }
        getConnection().commit();
    }


    protected void tearDown() throws Exception {
        super.tearDown();

        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM reader_group WHERE rdrgrp_name IN (?,?)");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setString(1  , "DUMMY1");
            ps.setString(2  , "DUMMY2");
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        getConnection().commit();
    }


    public EmployeeReaderGroupTaskTest(String arg0) throws Exception {
        super(arg0);
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        return suite;
    }

    public void test17() throws Exception {

        logger.debug("in test 17");

        final int empId=11; //TEST492 Michaels
        final int loanedRdrGrpId=55;

        final int homeRdrGrpId=0;


        EmployeeReaderGroupTask task = new EmployeeReaderGroupTask();
        EmployeeDateEffRdrgrpAccess access = new EmployeeDateEffRdrgrpAccess(
            getConnection());

        // clear
        access.deleteEmployeeEffRdrGrp(access.loadEmployeeEffRdrGrp());
        List list = access.loadEmployeeEffRdrGrp();
        logger.debug("After clear list=" + list);

        access.addEmployeeEffRdrGrp(
            empId,
            loanedRdrGrpId,
            DateHelper.createDate(2005, 9, 17),
            DateHelper.createDate(2005, 9, 18), "Test");
//Added homegrpId **
        access.addEmployeeEffRdrGrp(
            empId,
            homeRdrGrpId,
            DateHelper.createDate(2005, 9, 17),
            DateHelper.createDate(2005, 9, 18), "Test");

        /*  // employee 2
          access.addEmployeeEffRdrGrp(
                  empId2 ,
                  loanedRdrGrpId,
                  DateHelper.createDate(2005,9,18),
                  DateHelper.createDate(2005,9,18),"Test");
         */
        access.addEmployeeEffRdrGrp(
            empId,
            loanedRdrGrpId,
            DateHelper.createDate(2005, 9, 15),
            DateHelper.createDate(2005, 9, 15), "Test");

        access.addEmployeeEffRdrGrp(
            empId,
            loanedRdrGrpId,
            DateHelper.createDate(2005, 9, 22),
            DateHelper.createDate(2005, 9, 22), "Test");
        access.addEmployeeEffRdrGrp(
            empId,
            loanedRdrGrpId2,
            DateHelper.createDate(2005, 9, 19),
            DateHelper.createDate(2005, 9, 19), "Test");
        access.addEmployeeEffRdrGrp(
            empId,
            loanedRdrGrpId,
            DateHelper.createDate(2005, 9, 15),
            DateHelper.createDate(2005, 9, 15), "Test");

        list = access.loadEmployeeEffRdrGrp();
        logger.debug("list=" + list);

//  Run on 17-Oct-2005
        Date runDate1 = DateHelper.createDate(2005, 9, 17);
        Map params = new HashMap();
        task.setCurrentDate(runDate1);
        task.execute(getConnection() , params);
        //getConnection().commit();
        IntegerList rdrgrps = getRdrGrps(empId);
        assertTrue(rdrgrps.size() == 2);
        assertTrue("Run on 17-Oct-2005 Id check",
                     rdrgrps.contains(loanedRdrGrpId));
        assertTrue("Run on 17-Oct-2005 Id check",
                     rdrgrps.contains(homeRdrGrpId));

        runDate1 = DateHelper.createDate(2005, 9, 22);
        task.setCurrentDate(runDate1);
        task.execute(getConnection() , params);
        rdrgrps = getRdrGrps(empId);
        assertTrue(rdrgrps.size() == 1);
        assertTrue("Run on 18-Oct-2005 Id check",
                     rdrgrps.contains(loanedRdrGrpId));
    }

    private IntegerList getRdrGrps(int empId) throws Exception {

        PreparedStatement ps = null;
        IntegerList retGrpIds = new IntegerList();
        try {
            ps = getConnection().prepareStatement(
                "select emp_id,rdrgrp_id from employee_reader_group where emp_id=" +
                empId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                //    int emp_id=rs.getInt(1);
                int rdrgrp_id = rs.getInt(2);
                retGrpIds.add(rdrgrp_id);
            }
        }
        finally {
            if (ps != null)
                ps.close();
        }
        return retGrpIds;
    }


}
