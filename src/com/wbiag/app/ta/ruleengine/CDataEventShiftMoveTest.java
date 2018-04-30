package com.wbiag.app.ta.ruleengine;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for CDataEventShiftMoveest.
 */
public class CDataEventShiftMoveTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventShiftMoveTest.class);

    public CDataEventShiftMoveTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CDataEventShiftMoveTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testYest() throws Exception {

        setDataEventClassPath(
            "com.wbiag.app.ta.ruleengine.CDataEventShiftMove");

        final int empId = 11;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate(), "Mon");

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertOverride insL = new InsertOverride();
        insL.setOvrType(OverrideData.SCHEDULE_SHIFT_TYPE);
        insL.setWbuNameBoth("JUNIT", "JUNIT");
        insL.setEmpId(empId);
        insL.setStartDate(start);
        insL.setEndDate(start);
        insL.setOvrNewValue("\"" + OverrideData.SHIFT_NAME_OVERRIDE_FIELDNAME
                            +"=NIGHT\",\"" + CDataEventShiftMove.TOKEN_EMPSKDACT_YESTERDAY + "=Y\"");
        ovrBuilder.add(insL);

        ovrBuilder.execute(true, false);

        assertTrue(1 == ovrBuilder.getOverridesProcessed().size());
        WorkDetailList wdl = getWorkDetailsForDate(empId , start);
        assertTrue(wdl.size() > 0);
        assertEquals("Should be next day",
                   DateHelper.addDays(start , -1),
                   DateHelper.truncateToDays(wdl.getWorkDetail(0).getWrkdStartTime())  );

        //System.out.println(ovrBuilder.getOverridesProcessed());
        //System.out.println(wdl.toDescription()  );
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
