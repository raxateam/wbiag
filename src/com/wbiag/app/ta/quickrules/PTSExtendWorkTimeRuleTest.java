package com.wbiag.app.ta.quickrules;

import java.util.Date;

import junit.framework.TestSuite;

import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleTestCase;
import com.workbrain.tool.overrides.InsertWorkDetailOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.DateHelper;
import com.workbrain.util.Datetime;

/**
 * Title:			PTS ExtendWorkTimeRule Test
 * Description:		Junit test for PTSExtendWorkTimeRule
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		May 13, 2005
 * @author         	Kevin Tsoi
 */
public class PTSExtendWorkTimeRuleTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PTSExtendWorkTimeRuleTest.class);

    public PTSExtendWorkTimeRuleTest(String testName)
    	throws Exception
    {
        super(testName);
    }

    public static TestSuite suite()
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(PTSExtendWorkTimeRuleTest.class);
        return result;
    }

    public void testPTSExtendWorkTimeRule()
	    throws Exception
	{
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        int empId = 15;
        String minutesExtend = "120";
        String tcodeNameList = "WRK";
        String tcodeInclusive = "true";
        String htypeNameList = "REG";
        String htypeInclusive = "true";
        String extendTcode = "TRN";
        String extendHtype = "REG";

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new PTSExtendWorkTimeRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PTSExtendWorkTimeRule.PARAM_MINUTES_EXTEND, minutesExtend);
        ruleparams.addParameter(PTSExtendWorkTimeRule.PARAM_TCODENAME_LIST, tcodeNameList);
        ruleparams.addParameter(PTSExtendWorkTimeRule.PARAM_TCODE_INCLUSIVE, tcodeInclusive);
        ruleparams.addParameter(PTSExtendWorkTimeRule.PARAM_HTYPENAME_LIST, htypeNameList);
        ruleparams.addParameter(PTSExtendWorkTimeRule.PARAM_HTYPE_INCLUSIVE, htypeInclusive);
        ruleparams.addParameter(PTSExtendWorkTimeRule.PARAM_EXTEND_TCODE, extendTcode);
        ruleparams.addParameter(PTSExtendWorkTimeRule.PARAM_EXTEND_HTYPE, extendHtype);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        Datetime on = DateHelper.addMinutes(start, 17*60);
        Datetime off = DateHelper.addMinutes(start, 18*60);
        ins.setWrkdTcodeName("WRK");
        ins.setStartTime(on);
        ins.setEndTime(off);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 1);
        assertRuleApplied(empId, start, rule);

        WorkDetailList workDetailList = getWorkDetailsForDate(empId, start);
        int minutes = workDetailList.getMinutes(start, DateHelper.addDays(start, 1), extendTcode, true, extendHtype, true);

        assertTrue(minutes == 120);
	}

    public static void main(String[] args)
    	throws Exception
   	{
        junit.textui.TestRunner.run(suite());
    }
}
