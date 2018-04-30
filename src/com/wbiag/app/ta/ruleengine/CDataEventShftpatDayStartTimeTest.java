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
 * Test for CDataEventShftpatDayStartTimeTest.
 */
public class CDataEventShftpatDayStartTimeTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventShftpatDayStartTimeTest.class);

    public CDataEventShftpatDayStartTimeTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CDataEventShftpatDayStartTimeTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testDSTTime() throws Exception {


        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventShftpatDayStartTime");
        TestUtil.getInstance().setVarTemp("/" + CDataEventShftpatDayStartTime.REG_DAY_START_TIME_SHFTPAT_UDF, "shftpatshft_udf1")   ;

        final int empId = 11;
        final String shftPatName = "ALL DAYS"; final int shftpatId = 1;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON")  ;
        Date end = DateHelper.addDays(start , 6)  ;

        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("UPDATE shift_pattern_shifts SET shftpatshft_udf1 = ? WHERE shftpat_id = ?");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setString(1  , "11:00");
            ps.setInt(2, shftpatId) ;
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        EmployeeScheduleAccess acc = new EmployeeScheduleAccess(getConnection() , getCodeMapper());
        acc.deleteByDateRange(empId, start, end);
        List skds = acc.loadByDateRange(new int[] {empId}, start , end);
        assertTrue(skds.size() == 7);
        EmployeeScheduleData data1 = (EmployeeScheduleData)skds.get(0);
        System.out.println(data1);
        assertEquals("Should be next day",
                   DateHelper.addDays(start , 1),
                   DateHelper.truncateToDays(data1.getEmpskdActStartTime()));
    }

    /**
     * @throws Exception
     */
    public void testDSTYesterday() throws Exception {


        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventShftpatDayStartTime");
        TestUtil.getInstance().setVarTemp("/" + CDataEventShftpatDayStartTime.REG_DAY_START_TIME_SHFTPAT_UDF, "shftpatshft_udf1")   ;

        final int empId = 11;
        final String shftPatName = "ALL DAYS"; final int shftpatId = 1;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON")  ;
        Date end = DateHelper.addDays(start , 6)  ;

        final String shftName = "NIGHT" + System.currentTimeMillis() ;
        ShiftData sd = new ShiftData();

        sd.setShftName(shftName );
        sd.setShftDesc(shftName );
        sd.setShftStartTime(DateHelper.addMinutes(start, 20*60));
        sd.setShftEndTime(DateHelper.addMinutes(start, 28*60));
        sd.setShftgrpId(RuleHelper.getDefaultShiftGroupId(getCodeMapper()) );
        new ShiftAccess(getConnection()).insert(sd);
        int shftId = getConnection().getDBSequence(ShiftAccess.SHIFT_SEQ).getCurrentValue();

        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("UPDATE shift_pattern_shifts SET shftpatshft_udf1 = ?, shft_id = ? WHERE shftpat_id = ?");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setString(1  , CDataEventShftpatDayStartTime.SHFPAT_UDF_VAL_YESTERDAY );
            ps.setInt(2, shftId);
            ps.setInt(3, shftpatId) ;
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        getCodeMapper().invalidateAll();

        EmployeeScheduleAccess acc = new EmployeeScheduleAccess(getConnection() , getCodeMapper());
        acc.deleteByDateRange(empId, start, end);
        List skds = acc.loadByDateRange(new int[] {empId}, start , end);
        assertTrue(skds.size() == 7);
        EmployeeScheduleData data1 = (EmployeeScheduleData)skds.get(0);
        assertEquals("Should be previous day",
                   DateHelper.addDays(start , -1),
                   DateHelper.truncateToDays(data1.getEmpskdActStartTime()));
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
