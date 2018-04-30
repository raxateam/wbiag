package com.wbiag.tool.autotest.chart;

import java.io.File;

import junit.framework.TestSuite;

import com.wbiag.tool.autotest.chart.ChartUtil.ExtensionFilter;
import com.workbrain.sql.DBConnection;
import com.workbrain.test.TestCaseHW;

/** 
 * Title:			Generate Charts Test
 * Description:		Junit test for GenerateCharts
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Oct 19, 2005
 * @author         	Kevin Tsoi
 */
public class GenerateChartsTest extends TestCaseHW
{
    private static final String TEMP_DIR = "C:\\temp\\autotest";
    private static final String FILE_EXT = ".jpeg";
    
	public GenerateChartsTest(String arg0) 
	{
		super(arg0);
	}

	public static TestSuite suite() 
	{
		TestSuite result = new TestSuite();
		result.addTestSuite(GenerateChartsTest.class);
		return result;
	}
	
	/**
	 * test for generating charts
	 * 
	 * @throws Exception
	 */
	public void testGenerateCharts()
		throws Exception
	{
	    DBConnection conn = null;
	    int atId = 1;	    	    
	    
	    conn = getConnection();	    	    	    
	    
	    GenerateCharts genCharts = new GenerateCharts();
	    String filename = genCharts.GenerateXYChart(conn, TEMP_DIR, atId, null, null, null, null, "3", "seconds", 0, false);	    	    	    	    	    	 
	    	    	    	    
	    assertNotNull(filename);	    	    		     	    	 
	}			
	
	/**
	 * test for deleting charts
	 * 
	 * @throws Exception
	 */
	public void testDelete()
		throws Exception
	{	    	   
	    ChartUtil.deleteFiles(TEMP_DIR, FILE_EXT);
	    
	    ExtensionFilter filter = new ExtensionFilter(FILE_EXT);
        File dir = new File(TEMP_DIR);
        String[] list = dir.list(filter);

        assertTrue(list.length == 0);                
	}	
	
	public static void main(String[] args) 
		throws Exception 
	{
		junit.textui.TestRunner.run(suite());
	}
}
