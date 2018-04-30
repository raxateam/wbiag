package com.wbiag.app.ta.ruleengine;

import java.util.Date;

import com.wbiag.app.ta.quickrules.PTSExtendWorkTimeRule;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.WorkSummaryData;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.tool.overrides.InsertWorkDetailOverride;
import com.workbrain.tool.overrides.InsertWorkSummaryOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.DateHelper;
import com.workbrain.util.Datetime;

import junit.framework.TestSuite;

/** 
 * Title:			PTS Data Event Test
 * Description:		JUnit test for PTSDataEvent
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		May 16, 2005
 * @author         	Kevin Tsoi
 */
public class PTSDataEventTest extends DataEventTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PTSDataEventTest.class);

    public PTSDataEventTest(String testName) 
    	throws Exception 
    {
        super(testName);
    }

    public static TestSuite suite() 
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(PTSDataEventTest.class);
        return result;
    }
    
    public void testPTSDataEvent()
    	throws Exception
    {
        setDataEventClassPath("com.wbiag.app.ta.ruleengine.PTSDataEvent");
        
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        int empId = 15;                
        
        Date start = DateHelper.parseSQLDate("2004-11-09");                 
                        
        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setEmpId(empId);
        ins.setOvrType(OverrideData.WORK_SUMMARY_TYPE_START);        
        ins.setWrksClocks("~XX2004110908000001~");
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        
        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);    
        WorkSummaryData ws = getWorkSummaryForDate(empId , start);        
             
    //    assertNotNull(ws.getWrksUdf10());
    }
    
    public static void main(String[] args) 
    	throws Exception 
    {
        junit.textui.TestRunner.run(suite());
    }
}
