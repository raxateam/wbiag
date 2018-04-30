package com.wbiag.app.modules.reports.hcprintschedule;

import java.util.Date;
import java.sql.SQLException;

import junit.framework.TestSuite;
import com.workbrain.app.ta.ruleengine.RuleTestCase;
import com.workbrain.util.DateHelper;

public class PrintingScheduleControllerTest extends RuleTestCase {

	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PrintingScheduleControllerTest.class);

	public PrintingScheduleControllerTest(String testName) throws Exception {
        super(testName);
    }

	public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(PrintingScheduleControllerTest.class);
        return result;
    }

	public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

	public void testParamsReceived () throws SQLException{
		PrintingScheduleController report =
			new PrintingScheduleController();

		Date sampleDate = new Date();
		report.setTeams("VUH 10N, VUH 9N");
		report.setStartDate(sampleDate);
		report.setEndDate(DateHelper.addDays(sampleDate, 10));
		report.setDayparts("DAY12");
        report.setDaypartsSortDir("Ascending");
		report.setJobs("RN,CJ");
        report.setJobsSortDir("Descending");
		report.setEmps("ALL");
		report.setUserId(10003);
		report.setReportOutputFormat("PDF");

		assertEquals(report.getTeams(), "VUH 10N, VUH 9N");
		assertEquals(report.getStartDate(), sampleDate);
		assertEquals(report.getEndDate(), DateHelper.addDays(sampleDate, 10));
		assertEquals(report.getDayparts(), "DAY12");
		assertEquals(report.getJobs(), "RN,CJ");
		assertEquals(report.getEmps(), "ALL");
		assertEquals(report.getUserId(), 10003);
		assertEquals(report.getReportOutputFormat(), "PDF");

		report.generateReport(getConnection());
	}
}
