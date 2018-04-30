package com.wbiag.tool.regressiontest.report;

import com.workbrain.sql.DBConnection;


/**
 * @author bviveiros
 *
 */
public interface IReportDisplayGenerator {

	public ReportDisplaySuite 
					generateReportDisplay(int reportId, DBConnection conn)
											throws Exception;
}
