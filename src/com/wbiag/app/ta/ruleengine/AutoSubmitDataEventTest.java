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
 * @deprecated Core as of 4.1 FP14 with registry paramter TS_APPLY_OVR_ON_INSERT
 */
public class AutoSubmitDataEventTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CustomerDataEventMergePremiumTest.class);

    public AutoSubmitDataEventTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(AutoSubmitDataEventTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void xAutoSubmit() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.AutoSubmitDataEvent");

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.createsDefaultRecords();

        InsertWorkDetailOverride insL = new InsertWorkDetailOverride(getConnection());
        insL.setOvrType(OverrideData.LTA_TYPE_START);
        insL.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insL.setEmpId(empId);
        insL.setStartDate(start);
        insL.setEndDate(start);
        Date st = DateHelper.addMinutes(start, 9*60);
        Date end = DateHelper.addMinutes(start, 19*60);
        insL.setStartTime(st);
        insL.setEndTime(end);
        insL.setWrkdTcodeName("TRN");
        ovrBuilder.add(insL);

        ovrBuilder.execute(false, false);

        WorkDetailList wdl = getWorkDetailsForDate(empId,start);
        System.out.println(wdl.toDescription());
        assertEquals(10*60,
                     wdl.getMinutes(null, null, "TRN", true, null, true) );
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
