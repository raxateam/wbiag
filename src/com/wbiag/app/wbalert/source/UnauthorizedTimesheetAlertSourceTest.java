package com.wbiag.app.wbalert.source;

import java.util.HashMap;

import junit.framework.TestSuite;

import com.workbrain.test.TestCaseHW;

/** 
 * Title:			UnauthorizedTimesheetAlertSourceTest
 * Description:		Junit test for UnauthorizedTimesheetAlertSource
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Dec 22, 2005
 * @author         	Kevin Tsoi
 */
public class UnauthorizedTimesheetAlertSourceTest extends TestCaseHW
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(UnauthorizedTimesheetAlertSourceTest.class);
    
    public UnauthorizedTimesheetAlertSourceTest(String testName) 
    	throws Exception 
    {
        super(testName);
    }

    public static TestSuite suite() 
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(UnauthorizedTimesheetAlertSourceTest.class);
        return result;
    }
    
    public void testUnauthorizedTimesheetAlertSource()
    	throws Exception
    {                        
        HashMap exportParam = new HashMap();
        
        UnauthorizedTimesheetAlertSource alertSource = new UnauthorizedTimesheetAlertSource(getConnection(), exportParam);
        
        assertNotNull(alertSource);
    }
    
    public static void main(String[] args) 
		throws Exception 
	{
        junit.textui.TestRunner.run(suite());
	} 
}
