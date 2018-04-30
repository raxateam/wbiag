package com.wbiag.tool.regressiontest.testengine;

import com.wbiag.tool.regressiontest.model.ITestCaseData;
import com.workbrain.sql.DBConnection;


/**
 * @author bviveiros
 *
 */
public interface ITestCaseRunner {

	public abstract long run(ITestCaseData testCase, DBConnection conn) throws Exception;

}
