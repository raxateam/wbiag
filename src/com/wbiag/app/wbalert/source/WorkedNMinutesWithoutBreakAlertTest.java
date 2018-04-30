package com.wbiag.app.wbalert.source ;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestSuite;

import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.model.Clock;
import com.workbrain.app.wbalert.WBAlertProcess;
import com.workbrain.app.wbalert.db.WBAlertAccess;
import com.workbrain.app.wbalert.model.WBAlertData;
import com.workbrain.test.TestCaseHW;
import com.workbrain.tool.overrides.InsertWorkSummaryOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.DateHelper;
import com.workbrain.util.Datetime;

public class WorkedNMinutesWithoutBreakAlertTest extends TestCaseHW {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WorkedNMinutesWithoutBreakAlertTest.class);

    public WorkedNMinutesWithoutBreakAlertTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkedNMinutesWithoutBreakAlertTest.class);
        return result;
    }
    public String createWorkSummaryClockStringForOnOffs(List clockTimes) {
        if (clockTimes == null || clockTimes.size() == 0) {
            return null;
        }
        List clocks = new ArrayList();
        for (int i=0 , k=clockTimes.size() ; i<k ; i++) {
            Date dat = (Date)clockTimes.get(i);
            if (dat == null) {
                continue;
            }
            Clock clk = new Clock();
            clk.setClockDate(dat);
            clk.setClockType( (i % 2 == 0) ? Clock.TYPE_ON : Clock.TYPE_OFF);
            clocks.add(clk);
        }
        return Clock.createStringFromClockList(clocks);
    }

    /**
     */
    public void testAlert() throws Exception {
    	
    	   final int alertId = getConnection().getDBSequence(WBAlertAccess.WB_ALERT_SEQ ).getNextValue();
           PreparedStatement ps = null;
           try {
               StringBuffer sb = new StringBuffer(200);
               sb.append("INSERT INTO wb_alert (wbal_id, wbal_name, wbal_src_type, wbal_src_class, wbal_rc_wbu_names)");
               sb.append(" VALUES (?,?,?,?,?) ");
               ps = getConnection().prepareStatement(sb.toString());
               ps.setInt(1  , alertId);
               ps.setString(2 , "Worked N Minutes Without Break");
               ps.setString(3, WBAlertProcess.SOURCE_TYPE_ROWSOURCE);
               ps.setString(4, "com.wbiag.app.wbalert.source.WorkedNMinutesWithoutBreakAlertSourceBuilder");
               ps.setString(5, "WORKBRAIN");
               ps.executeUpdate();
           }
           finally {
               if (ps != null) ps.close();
           }
		final int empId = 3;
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
		ovrBuilder.setCreatesDefaultRecords(true);

        CodeMapper codeMapper = CodeMapper.createCodeMapper(getConnection());
    	InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(
    			getConnection());

		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);
		ins.setStartDate(DateHelper.addDays(DateHelper.getCurrentDate(), -1));
		ins.setEndDate(DateHelper.addDays(DateHelper.getCurrentDate(), -1));

		Datetime clksArr[] = new Datetime[10];
		clksArr[0] = DateHelper.addMinutes(DateHelper.addDays(DateHelper.getCurrentDate(), -1), 7 * 60);// on
		clksArr[1] = DateHelper.addMinutes(DateHelper.addDays(DateHelper.getCurrentDate(), -1), 12 * 60 + 15);// off
		clksArr[2] = DateHelper.addMinutes(DateHelper.addDays(DateHelper.getCurrentDate(), -1), 12 * 60 + 45);// on
		clksArr[3] = DateHelper.addMinutes(DateHelper.addDays(DateHelper.getCurrentDate(), -1), 17 * 60);// off

		String clks = createWorkSummaryClockStringForOnOffs(Arrays
				.asList(clksArr));
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

        WBAlertAccess access = new WBAlertAccess(getConnection());
        WBAlertData data = access.load(alertId);
        HashMap params = new HashMap();
        params.put(WorkedNMinutesWithoutBreakAlertSource.PARAM_BRK_TCODE, "BRK,UAT");
        params.put(WorkedNMinutesWithoutBreakAlertSource.PARAM_CALCGROUP, "ALL");
        params.put(WorkedNMinutesWithoutBreakAlertSource.PARAM_EMPLOYEE, "3");
        params.put(WorkedNMinutesWithoutBreakAlertSource.PARAM_HTYPE, "");
        params.put(WorkedNMinutesWithoutBreakAlertSource.PARAM_HTYPE_INCLUSIVE, "TRUE");
        params.put(WorkedNMinutesWithoutBreakAlertSource.PARAM_N_MINUTES, "60");
        params.put(WorkedNMinutesWithoutBreakAlertSource.PARAM_PAYGROUP, "ALL");
        params.put(WorkedNMinutesWithoutBreakAlertSource.PARAM_TEAM, "ALL");
        params.put(WorkedNMinutesWithoutBreakAlertSource.PARAM_WRK_TCODE, "");
        params.put(WorkedNMinutesWithoutBreakAlertSource.PARAM_WRK_TCODE_INCLUSIVE, "TRUE");
        
        WBAlertProcess apr = new WBAlertProcess(getConnection() , alertId, params);
        apr.execute();
        // *** must be only sent to workbrain
        assertEquals(1 , apr.getSentUserCount());
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

