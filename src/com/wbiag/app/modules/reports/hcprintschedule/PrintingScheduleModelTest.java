package com.wbiag.app.modules.reports.hcprintschedule;

import java.util.Date;

import junit.framework.TestSuite;
import org.apache.log4j.BasicConfigurator;
import com.workbrain.app.ta.ruleengine.RuleTestCase;
import com.workbrain.util.DateHelper;
import com.workbrain.sql.DBConnection;
import java.util.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.jsp.action.timesheet.NewWeekAction;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import java.rmi.AccessException;
import com.workbrain.app.modules.launchpads.staffingcoverage.*;

public class PrintingScheduleModelTest extends RuleTestCase {

	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PrintingScheduleControllerTest.class);
	private DBConnection conn = this.getConnection();

	public PrintingScheduleModelTest(String testName) throws Exception {
        super(testName);
    }

	public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(PrintingScheduleModelTest.class);
        return result;
    }

	public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

	public void testParamsReceived () throws Exception{
		PrintingScheduleModel model =
			new PrintingScheduleModel(conn);

		Date sampleDate = new Date();
		model.setTeams("10199"); //10159 VUH 9N, 10152 SURGERY TRAUMA
		model.setStartDate(DateHelper.convertStringToDate("10/6/2006", "dd/MM/yyyy"));
		model.setEndDate(DateHelper.convertStringToDate("20/6/2006", "dd/MM/yyyy"));
		model.setDayparts("ALL"); //10041 12HR DAY, 10042 12HR NGT
		model.setDaypartsSortDir("Ascending");
		model.setJobs("ALL"); //10066 RN, 10067 TR, 10035 AA
		model.setJobsSortDir("Ascending");
		model.setEmps("ALL"); //10325 richard e sacco
		model.setUserId(10321);//10321 SACCORE
		model.setReportOutputFormat("PDF");

		assertEquals(model.getTeams(), "VUH 10N, VUH 9N");
		assertEquals(model.getStartDate(), sampleDate);
		assertEquals(model.getEndDate(), DateHelper.addDays(sampleDate, 10));
		assertEquals(model.getDayparts(), "DAY12");
		assertEquals(model.getJobs(), "RN,CJ");
		assertEquals(model.getEmps(), "ALL");
		assertEquals(model.getUserId(), 10003);
		assertEquals(model.getReportOutputFormat(), "PDF");

//		model.retrieveModelData();
	}
}
