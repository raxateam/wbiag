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
 * Test for CustomerDataEventSkdStartEndOnWrksTest.
 */
public class CustomerDataEventSkdStartEndOnWrksTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CustomerDataEventSkdStartEndOnWrksTest.class);

    public CustomerDataEventSkdStartEndOnWrksTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CustomerDataEventSkdStartEndOnWrksTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testWrksOvr() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CustomerDataEventSkdStartEndOnWrks");

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.createsDefaultRecords();

        InsertOverride ins = new InsertOverride();
        ins.setOvrType(OverrideData.SCHEDULE_SHIFT_TYPE);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        Date st = DateHelper.addMinutes(start, 420);
        Date end = DateHelper.addMinutes(start, 960);
        StringBuffer sb = new StringBuffer(200);
        sb.append(OverrideData.formatToken(
            CustomerDataEventSkdStartEndOnWrks.START_TOKEN_NAME ,
            DateHelper.convertDateString(st, CustomerDataEventSkdStartEndOnWrks.DATE_FMT)  ));
        sb.append(OverrideData.formatToken(
            CustomerDataEventSkdStartEndOnWrks.END_TOKEN_NAME ,
            DateHelper.convertDateString(end, CustomerDataEventSkdStartEndOnWrks.DATE_FMT)  ));

        ins.setOvrNewValue(sb.toString());
        ins.setOvrType(OverrideData.WORK_SUMMARY_TYPE_START);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true, false);
        EmployeeScheduleData esd = getEmployeeScheduleData(empId,start);
        assertEquals(st , esd.getEmpskdActStartTime());
        assertEquals(end , esd.getEmpskdActEndTime());
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
