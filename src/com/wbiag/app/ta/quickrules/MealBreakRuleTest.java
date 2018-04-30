/*
 * Created on Mar 13, 2006
 *
 * State Pay Rules Project
 * Meal Break Rule Test Case
 * Description:  J-Unit tests for Meal Break Rule.
 * 
 */

package com.wbiag.app.ta.quickrules;

import java.util.*;

import junit.framework.*;

import com.workbrain.tool.overrides.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 *  Title:        Meal Break Rule Test
 *  Description:  J-Unit tests for Meal Break Rule.
 *  Copyright:    Copyright (c) 2006
 *  Company:      Workbrain Inc
 *   
 *@deprecated As of 5.0.2.0, use core classes 
 *@version    1.0
 */
public class MealBreakRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MealBreakRuleTest.class);

	DBConnection dbConnection;			//Database Connection
	Date start;	
	final int empId = 15;
	
	private static final int TC_BRK = 44;
	private static final int HT_REG = 1;
	private static final int HT_UNPAID = 0;
	
	private static final boolean disableAllTests = false;
	
    public MealBreakRuleTest(String testName) throws Exception {
        super(testName);
        dbConnection = getConnection();
        start = DateHelper.nextDay(
        		DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(MealBreakRuleTest.class);
        return result;
    }

    public static void main(String[] args) throws Exception {
        /*
         * Set employee to have a plain calc group
         * 
         */
        junit.textui.TestRunner.run(suite());
    }
    
    private void setCommonConsParameters(
    		Parameters params,
    		String consMinutes,
    		String brkDuration,
    		String inclBreaks,
    		String newPeriod,
    		int maxPrem) {
        setParameters(params, "0", consMinutes, inclBreaks, newPeriod, 
        		"WRK,LL,TRN,EARLY,PGR","REG,OT1,OT2,OT3,OTS,UNPAID", 
        		MealBreakRule.VALUE_FALSE,"-1","-1","-1","-1","-1","-1",brkDuration,"BRK","REG",
        		"GAP",MealBreakRule.VALUE_TRUE,
        		"WRK","REG","20","0",String.valueOf(maxPrem));
    }
    private void setCommonShiftParameters(
    		Parameters params,
    		String minShiftLength,
    		String brkDuration,
    		String relTime,
    		String shftStartFrom,
    		String shftStartTo,
    		String shftEndFrom,
    		String brkFrom,
    		String brkTo,
    		String inclBreaks,
    		String newPeriod,
    		int maxPrem) {
        setParameters(params, minShiftLength, "0", inclBreaks, newPeriod, 
        		"WRK,LL,LNCH,EARLY,PGR", "REG,OT1,OT2,OT3,OTS,UNPAID", 
        		relTime,shftStartFrom,shftStartTo,shftEndFrom,"-1",brkFrom,
        		brkTo,brkDuration,"BRK","REG","GAP",MealBreakRule.VALUE_TRUE,
        		"WRK","REG","20","0",String.valueOf(maxPrem));
    }
    
    private void setParameters(
    		Parameters params, 
    		String minLengthOfShift, 
    		String consMinutes, 
    		String incBreaks,
    		String newPeriod,
    		String validWorkTimeCodes, 
    		String validWorkHourTypes,
    		String relActTime,
    		String shftStartFrom,
    		String shftStartTo,
    		String shftEndFrom,
    		String shftEndTo,
    		String brkFrom,
    		String brkTo,
    		String durBreak, 
    		String breakTC,
    		String breakHT,
    		String shftDivTC,
    		String shftDivInc,
    		String premTcode, 
    		String premHtype, 
    		String premMin,
    		String premRate,
    		String maxPrem)
    {
	   params.addParameter(MealBreakRule.PARAM_MIN_LENGTH_OF_SHIFT, minLengthOfShift);
       params.addParameter(MealBreakRule.PARAM_CONSEC_MINUTES, consMinutes);
       params.addParameter(MealBreakRule.PARAM_INCLUDE_BREAKS_IN_WORK, incBreaks);
       params.addParameter(MealBreakRule.PARAM_NEW_PERIOD_AFTER_BREAK, newPeriod);
       params.addParameter(MealBreakRule.PARAM_VALID_WORKED_TIME_CODES, validWorkTimeCodes);
       params.addParameter(MealBreakRule.PARAM_VALID_WORKED_HOUR_TYPE, validWorkHourTypes);
       params.addParameter(MealBreakRule.PARAM_RELATIVE_TO_ACTUAL_TIME, relActTime);
       params.addParameter(MealBreakRule.PARAM_WORKED_SHIFT_START_FROM, shftStartFrom);
       params.addParameter(MealBreakRule.PARAM_WORKED_SHIFT_START_TO, shftStartTo);
       params.addParameter(MealBreakRule.PARAM_WORKED_SHIFT_END_FROM, shftEndFrom);
       params.addParameter(MealBreakRule.PARAM_WORKED_SHIFT_END_TO, shftEndTo);
       params.addParameter(MealBreakRule.PARAM_BREAK_FROM, brkFrom);
       params.addParameter(MealBreakRule.PARAM_BREAK_TO, brkTo);
       params.addParameter(MealBreakRule.PARAM_DURATION_OF_BREAK, durBreak);        
       params.addParameter(MealBreakRule.PARAM_VALID_BREAK_TIME_CODE, breakTC);
       params.addParameter(MealBreakRule.PARAM_VALID_BREAK_HOUR_TYPE, breakHT);
       params.addParameter(MealBreakRule.PARAM_SHIFT_DIVIDER_TIME_CODES, shftDivTC);
       params.addParameter(MealBreakRule.PARAM_SHIFT_DIVIDER_INCLUSIVE, shftDivInc);
       params.addParameter(MealBreakRule.PARAM_PREMIUM_TCODE, premTcode);
       params.addParameter(MealBreakRule.PARAM_PREMIUM_HOURTYPE, premHtype);
       params.addParameter(MealBreakRule.PARAM_PREMIUM_MINUTES, premMin);
       params.addParameter(MealBreakRule.PARAM_PREMIUM_RATE, premRate);
       params.addParameter(MealBreakRule.PARAM_MAX_PREMIUM_ALLOWED, maxPrem);
       
    }
    
    private void setCA1Params(Parameters params,
    		String inclBreaks,String newPeriod,int maxPrem) {
    	setCommonConsParameters(params,"240","10",inclBreaks,newPeriod,maxPrem);
    }
    
    private void setCA2Params(Parameters params,
    		String inclBreaks,String newPeriod,int maxPrem) {
    	setCommonShiftParameters(params,"360","30",MealBreakRule.VALUE_FALSE,"-1","-1","-1","-1","-1",inclBreaks,newPeriod,maxPrem);
    }

    private void setNY1Params(Parameters params,
    		String inclBreaks,String newPeriod,int maxPrem) {
    	setCommonShiftParameters(params,"480","20",MealBreakRule.VALUE_FALSE,"-1","660","1140","1020","1140",inclBreaks,newPeriod,maxPrem);
    }

    private void setNY2Params(Parameters params,
    		String inclBreaks,String newPeriod,int maxPrem) {
    	setCommonShiftParameters(params,"360","60",MealBreakRule.VALUE_FALSE,"780","360","-1","-1","-1",inclBreaks,newPeriod,maxPrem);
    }
    
    private void setNY3Params(Parameters params,
    		String inclBreaks,String newPeriod,int maxPrem) {
    	setCommonShiftParameters(params,"360","30",MealBreakRule.VALUE_FALSE,"-1","-1","-1","660","840",inclBreaks,newPeriod,maxPrem);
    }
    
    private void setWA2Params(Parameters params,
    		String inclBreaks,String newPeriod,int maxPrem) {
    	setCommonShiftParameters(params,"300","30",MealBreakRule.VALUE_TRUE,"-1","-1","-1","120","300",inclBreaks,newPeriod,maxPrem);
    }

    private void setCA1JCPParams(Parameters params,int maxPrem) {
        setParameters(params, "0", "300", MealBreakRule.VALUE_FALSE, 
        		MealBreakRule.VALUE_TRUE, "WRK,LL,LNCH,EARLY,PGR", 
        		"REG,OT1,OT2,OT3,OTS,UNPAID", MealBreakRule.VALUE_TRUE,
        		"-1","-1","-1","-1","-1","-1","30","BRK","REG","GAP",
        		MealBreakRule.VALUE_TRUE,"WRK","REG","60","0",
        		String.valueOf(maxPrem));
    }
    
    /*Method has been taken from WeeklySplitOT Junit Test Class in the WBIAG Jar.*/
    private int getShiftID(int shiftStart, int shiftEnd)throws Exception
    {
        int shiftId=dbConnection.getDBSequence(ShiftAccess.SHIFT_SEQ).getNextValue();
        ShiftAccess sha1=new ShiftAccess(dbConnection);
        
        ShiftData shiftData1 = new ShiftData();
        
        shiftData1.setShftStartTime(DateHelper.addMinutes(start, shiftStart));
        shiftData1.setShftEndTime(DateHelper.addMinutes(start, shiftEnd));
        shiftData1.setShftName("J-Unit Meal Break Shift"); 
        shiftData1.setGeneratesPrimaryKeyValue(true);
        shiftData1.setShftDesc("Testing Meal Break Rule"); 
        shiftData1.setColrId(0);
        shiftData1.setShftgrpId(0);
        
        sha1.insert(shiftData1);
        shiftId=shiftData1.getShftId();
        return shiftId;
    }

    private void insertShiftBreak(int shiftId,int startMinutes,int endMinutes,int tcodeId, int htypeId) throws Exception {
		ShiftBreakAccess sba = new ShiftBreakAccess(dbConnection);
		ShiftBreakData sbd = new ShiftBreakData();
		int shftBrkId = dbConnection.getDBSequence("seq_shftbrk_id").getNextValue();
		sbd.setShftbrkId(shftBrkId);
		sbd.setShftbrkMinutes(endMinutes - startMinutes);
		sbd.setShftbrkStartTime(DateHelper.addMinutes(start, startMinutes));
		sbd.setShftbrkEndTime(DateHelper.addMinutes(start, endMinutes));
		sbd.setShftId(shiftId);
		sbd.setTcodeId(tcodeId);
		sbd.setHtypeId(htypeId);
		sba.insert(sbd);
    }
    
    private void insertScheduleOvrd(OverrideBuilder ovrBuilder, int shiftId, int startMinutes, int endMinutes) {
		InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(dbConnection);
		insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
		insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		insSkd.setEmpId(empId);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		insSkd.setEmpskdActShiftId(shiftId);
		Datetime skdStart = DateHelper.addMinutes(start, startMinutes);
		Datetime skdEnd = DateHelper.addMinutes(start, endMinutes);
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);
    }

    private void insertWrksOvrd(OverrideBuilder ovrBuilder, List clockList) {
		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(dbConnection);
		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);
		ins.setStartDate(start);
		ins.setEndDate(start);
		
		Datetime clksArr[] = new Datetime[clockList.size() * 2];
		Iterator clockItr = clockList.iterator();
		IntPair curClockPair = null;
		for (int curClock = 0; clockItr.hasNext(); curClock++) {
			curClockPair = (IntPair) clockItr.next();
			clksArr[curClock * 2] = DateHelper.addMinutes(start, curClockPair.start);// on
			clksArr[curClock * 2 + 1] = DateHelper.addMinutes(start, curClockPair.end);// on
		}
		String clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		ins.setWrksClocks(clks);
		ovrBuilder.add(ins);
    }

    private void setupData(List shiftList, List clockList, List brkList, List payList, OverrideBuilder ob) throws Exception {
    	Iterator shftItr = shiftList.iterator();
    	Iterator brkItr = brkList.iterator();
    	Iterator payItr = payList.iterator();
    	
    	int curId = 0;
    	int shiftId = 0;
    	IntPair curBrk = null;
    	IntPair curShft = null;
    	IntPair curPay = null;
    	
    	boolean brkEnd = false;
		if (brkItr.hasNext()) {
			curBrk = (IntPair) brkItr.next();
			curPay = (IntPair) payItr.next();
		} else {
			brkEnd = true;
		}
    	
    	while (shftItr.hasNext()) {
    		curShft = (IntPair) shftItr.next();
    		shiftId = getShiftID(curShft.start, curShft.end);
    		curId++;
    		while (!brkEnd && curBrk.id  == curId) {
    			insertShiftBreak(shiftId,curBrk.start,curBrk.end,curPay.start,curPay.end);
    			if (brkItr.hasNext()) {
    				curBrk = (IntPair) brkItr.next();
    				curPay = (IntPair) payItr.next();
    			} else {
    				brkEnd = true;
    			}
    		}
    		insertScheduleOvrd(ob,shiftId,curShft.start,curShft.end);
    	}
    	
       	insertWrksOvrd(ob,clockList);
    }
    
    private void generalTest(String testName, List shiftList, List clockList, List brkList, List payList, int numPrems, int maxPrems, Parameters params) throws Exception{
		WorkDetailList wdl = getWorkPremiumsForDate(empId,start);
		assertEquals(0,wdl.size());
    	System.out.println("START " + testName);
    	OverrideBuilder ovrBuilder = new OverrideBuilder(dbConnection);
    	ovrBuilder.setCreatesDefaultRecords(true);
    	/*Set up Rule*/
    	Rule ruleObject = new MealBreakRule();
    	clearAndAddRule(empId, start, ruleObject, params);
    	setupData(shiftList, clockList, brkList, payList, ovrBuilder);
    	ovrBuilder.execute(true, false);
    	/*Assert Statements*/
    	try {
    		assertRuleApplied(empId, start, ruleObject);
    		wdl = getWorkPremiumsForDate(empId,start);
    		assertEquals(numPrems,wdl.size());
    	} catch (AssertionFailedError aex) {
    		System.out.println("END " + testName + " with Errors");
    		throw aex;
    	} finally {
        	ovrBuilder.clear();
        	getConnection().rollback();
    	}
    	System.out.println("END " + testName + " successfully");
    }
    
    private void testWA2(String testName, List shiftList, List clockList, List brkList, List payList, int numPrems, int maxPrems) throws Exception {
    	Parameters params = new Parameters();
    	params.removeAllParameters();
		setWA2Params(params,MealBreakRule.VALUE_FALSE,MealBreakRule.VALUE_FALSE,maxPrems);
    	generalTest(testName, shiftList,clockList,brkList,payList,numPrems,maxPrems,params);
    }
    
    private void testCA1(String testName, List shiftList, List clockList, List brkList, List payList, int numPrems, int maxPrems) throws Exception {
    	Parameters params = new Parameters();
    	params.removeAllParameters();
		setCA1Params(params,MealBreakRule.VALUE_TRUE,MealBreakRule.VALUE_FALSE,maxPrems);
    	generalTest(testName, shiftList,clockList,brkList,payList, numPrems,maxPrems,params);
    }
    
    private void testCA2(String testName, List shiftList, List clockList, List brkList, List payList, int numPrems, int maxPrems) throws Exception {
    	Parameters params = new Parameters();
    	params.removeAllParameters();
		setCA2Params(params,MealBreakRule.VALUE_FALSE,MealBreakRule.VALUE_FALSE,maxPrems);
    	generalTest(testName, shiftList,clockList,brkList,payList, numPrems,maxPrems,params);
    }
    
    private void testNY1(String testName, List shiftList, List clockList, List brkList, List payList, int numPrems, int maxPrems) throws Exception {
    	Parameters params = new Parameters();
    	params.removeAllParameters();
		setNY1Params(params,MealBreakRule.VALUE_FALSE,MealBreakRule.VALUE_FALSE,maxPrems);
    	generalTest(testName, shiftList,clockList,brkList,payList, numPrems,maxPrems,params);
    }
    
    private void testNY2(String testName, List shiftList, List clockList, List brkList, List payList, int numPrems, int maxPrems) throws Exception {
    	Parameters params = new Parameters();
    	params.removeAllParameters();
		setNY2Params(params,MealBreakRule.VALUE_FALSE,MealBreakRule.VALUE_FALSE,maxPrems);
    	generalTest(testName, shiftList,clockList,brkList,payList, numPrems,maxPrems,params);
    }

    private void testNY3(String testName, List shiftList, List clockList, List brkList, List payList, int numPrems, int maxPrems) throws Exception {
    	Parameters params = new Parameters();
    	params.removeAllParameters();
		setNY3Params(params,MealBreakRule.VALUE_FALSE,MealBreakRule.VALUE_FALSE,maxPrems);
    	generalTest(testName, shiftList,clockList,brkList,payList, numPrems,maxPrems,params);
    }

    private void testCA1JCP(String testName, List shiftList, List clockList, List brkList, List payList, int numPrems, int maxPrems) throws Exception {
    	Parameters params = new Parameters();
    	params.removeAllParameters();
		setCA1JCPParams(params,maxPrems);
    	generalTest(testName, shiftList,clockList,brkList,payList, numPrems,maxPrems,params);
    }
    
    /**
     * Blue Cube Case 1
     * @throws Exception
     */
    public void testCustomCase1() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();
    	
    	shiftList.add(new IntPair(720,1351));
    	brkList.add(new IntPair(1020,1050,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(720,1020));
    	clockList.add(new IntPair(1050,1351));

    	
    	testCA1JCP("testCA1JCPCase1",shiftList,clockList,brkList,payList,1,0);

/*    	
    	Parameters params = new Parameters();
    	params.removeAllParameters();
        setParameters(params, "0", "301", MealBreakRule.VALUE_FALSE, 
        		MealBreakRule.VALUE_TRUE, "WRK", "REG", 
        		MealBreakRule.VALUE_TRUE,"-1","-1","-1","-1","-1","-1","30",
        		"BRK","UNPAID","GAP",MealBreakRule.VALUE_TRUE,"WRK","REG","60","0",
        		String.valueOf(1));
    	generalTest("testCustomCase1", shiftList,clockList,brkList,payList, 1,1,params);
    	*/
    }
    
    /**
     * Blue Cube Case 1
     * @throws Exception
     */
    public void testCA1JCPCase1() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();
    	
    	shiftList.add(new IntPair(720,1035));
    	brkList.add(new IntPair(1020,1035,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(720,1035));
    	
    	testCA1JCP("testCA1JCPCase1",shiftList,clockList,brkList,payList,1,0);
    }
    
    /**
     * Blue Cube Case 2
     * @throws Exception
     */
    public void testCA1JCPCase2() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	
    	shiftList.add(new IntPair(720,1035));
    	brkList.add(new IntPair(840,855,1));
    	clockList.add(new IntPair(720,1035));
    	
    	testCA1JCP("testCA1JCPCase2",shiftList,clockList,brkList,payList,1,0);
    }
    
    /**
     * Blue Cube Case 3
     * @throws Exception
     */
    public void testCA1JCPCase3() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	
    	shiftList.add(new IntPair(720,1140));
    	brkList.add(new IntPair(1035,1065,1));
    	clockList.add(new IntPair(720,1140));
    	
    	testCA1JCP("testCA1JCPCase3",shiftList,clockList,brkList,payList,1,0);
    }
    
    /**
     * Blue Cube Case 4
     * @throws Exception
     */
    public void testCA1JCPCase4() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(480,1410));
    	brkList.add(new IntPair(660,720,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(480,660));
    	clockList.add(new IntPair(720,1410));
    	
    	testCA1JCP("testCA1JCPCase4",shiftList,clockList,brkList,payList,2,0);
    }
    
    /**
     * Blue Cube Case 5
     * @throws Exception
     */
    public void testCA1JCPCase5() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();
    	
    	shiftList.add(new IntPair(720,1080));
    	brkList.add(new IntPair(1020,1035,1));
    	clockList.add(new IntPair(720,1080));
    	payList.add(new IntPair(TC_BRK, HT_REG));

    	testCA1JCP("testCA1JCPCase5",shiftList,clockList,brkList,payList,1,0);
    }
    
    /**
     * Blue Cube Case 6
     * @throws Exception
     */
    public void testCA1JCPCase6() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(720,1080));
    	brkList.add(new IntPair(1020,1050,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(720,1020));
    	clockList.add(new IntPair(1050,1080));
    	
    	testCA1JCP("testCA1JCPCase6",shiftList,clockList,brkList,payList,0,0);
    }
    
    /**
     * Blue Cube Case 7
     * @throws Exception
     */
    public void testCA1JCPCase7() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(720,1021));
    	clockList.add(new IntPair(720,1021));
    	
    	testCA1JCP("testCA1JCPCase7",shiftList,clockList,brkList,payList,1,0);
    }
    
    /**
     * Blue Cube Case 9
     * @throws Exception
     */
    public void testCA1JCPCase8() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(720,120));
    	brkList.add(new IntPair(1080,1140,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	brkList.add(new IntPair(1320,1380,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(720,1080));
    	clockList.add(new IntPair(1140,1320));
    	clockList.add(new IntPair(1380,120));
    	
    	testCA1JCP("testCA1JCPCase8",shiftList,clockList,brkList,payList,1,0);
    }
    
    /**
     * Blue Cube Case 10
     * @throws Exception
     */
    public void testCA1JCPCase9() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();
    	
    	shiftList.add(new IntPair(720,60));
    	brkList.add(new IntPair(900,960,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	brkList.add(new IntPair(1320,1380,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(720,900));
    	clockList.add(new IntPair(960,1320));
    	clockList.add(new IntPair(1380,60));

    	testCA1JCP("testCA1JCPCase9",shiftList,clockList,brkList,payList,1,0);
    }
    
    /**
     * Blue Cube Case 11
     * @throws Exception
     */
    public void testCA1JCPCase10() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();
    	
    	shiftList.add(new IntPair(720,1050));
    	brkList.add(new IntPair(900,915,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	brkList.add(new IntPair(960,975,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(720,1050));

    	testCA1JCP("testCA1JCPCase10",shiftList,clockList,brkList,payList,1,0);
    }
    
    /**
     * Blue Cube Case 12
     * @throws Exception
     */
    public void testCA1JCPCase11() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();
    	
    	shiftList.add(new IntPair(660,1080));
    	brkList.add(new IntPair(960,975,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	brkList.add(new IntPair(975,990,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(660,960));
    	clockList.add(new IntPair(975,975));
    	clockList.add(new IntPair(990,1080));

    	testCA1JCP("testCA1JCPCase11",shiftList,clockList,brkList,payList,0,0);
    }
    
    
    public void testWA2Case1() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();
    	
    	shiftList.add(new IntPair(730,1060));
    	brkList.add(new IntPair(900,930,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(730,900));
    	clockList.add(new IntPair(930,1060));

    	testWA2("testWA2Case1",shiftList,clockList,brkList,payList,0,0);
    }
    
    public void testWA2Case2() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(730,1060));
    	clockList.add(new IntPair(730,1060));
    	
    	testWA2("testWA2Case2",shiftList,clockList,brkList,payList,1,0);
    }
    
    /**
     * California Rule 1: Case 1
     * No premiums inserted, shift satisfying length and one break
     */
    public void testCA1Case1() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(540,1000));
    	brkList.add(new IntPair(720,730,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(540,720));
    	clockList.add(new IntPair(730,1000));
    	
    	testCA1("testCA1Case1",shiftList,clockList,brkList,payList,0,0);
    }

    /**
     * California Rule 1: Case 2
     * One premium inserted, shift satisfying length with no breaks
     */
    public void testCA1Case2() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(540,1019));
    	clockList.add(new IntPair(540,1019));
    	
    	testCA1("testCA1Case1",shiftList,clockList,brkList,payList,1,0);
    }

    /**
     * California Rule 1: Case 3
     * One premium inserted, shift satisfying consecutive length twice with 
     * only one break in first cons 4 hours
     */
    public void testCA1Case3() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(540,1020));
    	brkList.add(new IntPair(720,730,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(540,720));
    	clockList.add(new IntPair(730,1100));
    	
    	testCA1("testCA1Case3",shiftList,clockList,brkList,payList,1,0);
    }

    /**
     * California Rule 1: Case 4
     * One premium inserted, shift satisfying consecutive length twice with 
     * only one break in second cons 4 hours
     */
    public void testCA1Case4() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(540,1020));
    	brkList.add(new IntPair(1000,1010,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(540,1000));
    	clockList.add(new IntPair(1010,1100));
    	
    	testCA1("testCA1Case4",shiftList,clockList,brkList,payList,1,0);
    }

    /**
     * California Rule 1: Case 5
     * Two premiums inserted, shift satisfying consecutive length twice with 
     * no breaks
     */
    public void testCA1Case5() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(540,1020));
    	clockList.add(new IntPair(540,1100));
    	
    	testCA1("testCA1Case5",shiftList,clockList,brkList,payList,2,0);
    }
    
    /**
     * California Rule 1: Case 6
     * No premiums inserted, shift satisfying length and one break
     */
    public void testCA1Case6() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(540,1000));
    	brkList.add(new IntPair(775,790,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(540,1000));
    	
    	testCA1("testCA1Case6",shiftList,clockList,brkList,payList,1,0);
    }

    /**
     * California Rule 1: Case 7
     * No premiums inserted, shift satisfying length and one break
     */
    public void testCA1Case7() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(540,1000));
    	brkList.add(new IntPair(760,790,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(540,760));
    	clockList.add(new IntPair(790,1000));
    	
    	testCA1("testCA1Case7",shiftList,clockList,brkList,payList,0,0);
    }

    /**
     * California Rule 1: Case 8
     * One premium inserted, shift satisfying length and one break with wrong
     * hour type
     */
    public void testCA1Case8() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(540,1000));
    	brkList.add(new IntPair(720,730,1));
    	payList.add(new IntPair(TC_BRK, HT_UNPAID));
    	clockList.add(new IntPair(540,1000));
    	
    	testCA1("testCA1Case8",shiftList,clockList,brkList,payList,1,0);
    }

    /**
     * California Rule 2: Case 1
     * One premium inserted, shift satisfying consecutive length twice with 
     * only one break in second cons 4 hours
     */
    public void testCA2Case1() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(540,1020));
    	brkList.add(new IntPair(720,750,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(540,720));
    	clockList.add(new IntPair(750,1020));
    	
    	testCA2("testCA2Case1",shiftList,clockList,brkList,payList,0,0);
    }
    
    public void testCA2Case2() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(540,1020));
    	clockList.add(new IntPair(540,1020));
    	
    	testCA2("testCA2Case2",shiftList,clockList,brkList,payList,1,0);
    }
    
    
    
    public void testCA2Case3() throws Exception {
    	if (false) {
    		return;
    	}
    	String testName = "testCA2Case3";

		WorkDetailList wdl = getWorkPremiumsForDate(empId,start);
		assertEquals(0,wdl.size());
    	
    	Parameters params = new Parameters();
    	params.removeAllParameters();
		setCA1Params(params,MealBreakRule.VALUE_TRUE,MealBreakRule.VALUE_FALSE,0);
		
    	Parameters params2 = new Parameters();
    	params2.removeAllParameters();
        setParameters(params2, "360", "0", MealBreakRule.VALUE_FALSE, 
        		MealBreakRule.VALUE_FALSE, 
        		"WRK,LL,BRK,EARLY,PGR", "REG,OT1,OT2,OT3,OTS,UNPAID", 
        		MealBreakRule.VALUE_FALSE,"-1","-1","-1","-1","-1","-1","30",
        		"LNCH","UNPAID","GAP",MealBreakRule.VALUE_TRUE,
        		"WRK","REG","20","0","0");
		
    	System.out.println("START " + testName);
    	OverrideBuilder ovrBuilder = new OverrideBuilder(dbConnection);
    	ovrBuilder.setCreatesDefaultRecords(true);
    	
    	/*Set up Rule*/
    	Rule ruleObject = new MealBreakRule();
    	Rule ruleObject2 = new MealBreakRule();
    	clearAndAddRule(empId, start, ruleObject, params);
    	addRule(empId,start, ruleObject2, params2);

    	int startMinutes = 540;
    	int endMinutes = 781;
    	int startMinutes2 = 1020;
    	int endMinutes2 = 1320;
    	int clockStartMinutes = 540;
    	int clockEndMinutes = 779;
    	int clockStartMinutes2 = 1080;
    	int clockEndMinutes2 = 1379;
    		
    	int shiftId = 0;
    	int shiftId2 = 0;
    	
    	dbConnection.getDBSequence(ShiftAccess.SHIFT_SEQ).getNextValue();
    	
        ShiftAccess sha1=new ShiftAccess(dbConnection);
        
        ShiftData shiftData1 = new ShiftData();
        
        shiftData1.setShftStartTime(DateHelper.addMinutes(start, startMinutes));
        shiftData1.setShftEndTime(DateHelper.addMinutes(start, endMinutes));
        shiftData1.setShftName("J-Unit Meal Break Shift"); 
        shiftData1.setShftDesc("Testing Meal Break Rule"); 
        shiftData1.setColrId(0);
        shiftData1.setShftgrpId(0);
    	shiftData1.setShftId(dbConnection.getDBSequence(ShiftAccess.SHIFT_SEQ).getNextValue());
        
        sha1.insert(shiftData1);
        shiftId=shiftData1.getShftId();
        ShiftData shiftData2 = new ShiftData();
        
        shiftData2.setShftStartTime(DateHelper.addMinutes(start, startMinutes2));
        shiftData2.setShftEndTime(DateHelper.addMinutes(start, endMinutes2));
        shiftData2.setShftName("J-Unit Meal Break Shift2"); 
        shiftData2.setShftDesc("Testing Meal Break Rule2"); 
        shiftData2.setColrId(0);
        shiftData2.setShftgrpId(0);
    	shiftData2.setShftId(dbConnection.getDBSequence(ShiftAccess.SHIFT_SEQ).getNextValue());
        
        sha1.insert(shiftData2);
        shiftId2=shiftData2.getShftId();

    	
		InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(dbConnection);
		insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
		insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		insSkd.setEmpId(empId);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		insSkd.setEmpskdActShiftId(shiftId);
		insSkd.setEmpskdActShiftId2(shiftId2);
		Datetime skdStart = DateHelper.addMinutes(start, startMinutes);
		Datetime skdEnd = DateHelper.addMinutes(start, endMinutes);
		Datetime skdStart2 = DateHelper.addMinutes(start, startMinutes2);
		Datetime skdEnd2 = DateHelper.addMinutes(start, endMinutes2);
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		insSkd.setEmpskdActStartTime2(skdStart2);
		insSkd.setEmpskdActEndTime2(skdEnd2);
		
		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(dbConnection);
		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);
		ins.setStartDate(start);
		ins.setEndDate(start);
		List clksList= new ArrayList();
		List jobList = new ArrayList();
		clksList.add(DateHelper.addMinutes(start, clockStartMinutes));// on
		jobList.add("1");
		clksList.add(DateHelper.addMinutes(start, clockEndMinutes));// off
		jobList.add("1");
		clksList.add(DateHelper.addMinutes(start, clockStartMinutes2));// on
		jobList.add("2");
		clksList.add(DateHelper.addMinutes(start, clockEndMinutes2));// off
		jobList.add("2");
//		String clks = createClockStringWithJobs(clksList,jobList);
		String clks = createWorkSummaryClockStringForOnOffs(clksList);
		ins.setWrksClocks(clks);
		ovrBuilder.add(ins);
		
		ovrBuilder.add(insSkd);
    	
    	ovrBuilder.execute(true, false);
    	/*Assert Statements*/
    	try {
//    		assertRuleApplied(empId, start, ruleObject);
    		wdl = getWorkPremiumsForDate(empId,start);
    		assertEquals(1,wdl.size());
    	} catch (AssertionFailedError aex) {
    		System.out.println("END " + testName + " with Errors");
    		throw aex;
    	} finally {
        	ovrBuilder.clear();
        	getConnection().rollback();
    	}
    	System.out.println("END " + testName + " successfully");
    }
  
    
    public void testNY1Case1() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(630,1200));
    	brkList.add(new IntPair(1060,1080,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(630,1060));
    	clockList.add(new IntPair(1080,1200));
    	
    	testNY1("testNY1Case1",shiftList,clockList,brkList,payList,0,0);
    }
    
    public void testNY1Case2() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(630,1200));
    	clockList.add(new IntPair(630,1200));
    	
    	testNY1("testNY1Case2",shiftList,clockList,brkList,payList,1,0);
    }
    
    public void testNY2Case1() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(300,800));
    	brkList.add(new IntPair(420,480,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(300,420));
    	clockList.add(new IntPair(480,800));
    	
    	testNY2("testNY2Case1",shiftList,clockList,brkList,payList,0,0);
    }
    
    public void testNY2Case2() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(300,800));
    	clockList.add(new IntPair(300,800));
    	
    	testNY2("testNY2Case2",shiftList,clockList,brkList,payList,1,0);
    }
    
    public void testNY3Case1() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(600,990));
    	brkList.add(new IntPair(700,730,1));
    	payList.add(new IntPair(TC_BRK, HT_REG));
    	clockList.add(new IntPair(600,700));
    	clockList.add(new IntPair(730,990));
    	
    	testNY3("testNY3Case1",shiftList,clockList,brkList,payList,0,0);
    }

    public void testNY3Case2() throws Exception {
    	if (disableAllTests) {
    		return;
    	}
    	List shiftList = new ArrayList();
    	List brkList = new ArrayList();
    	List clockList = new ArrayList();
    	List payList = new ArrayList();

    	shiftList.add(new IntPair(600,990));
    	clockList.add(new IntPair(600,990));
    	
    	testNY3("testNY3Case2",shiftList,clockList,brkList,payList,1,0);
    }    
    
    private class IntPair {
    	int start;
    	int end;
    	int id = -1;
    	public IntPair(int start,int end) {
    		this.start = start;
    		this.end = end;
    	}

    	public IntPair(int start,int end, int id) {
    		this.start = start;
    		this.end = end;
    		this.id = id;
    	}
    }
    
    private String createClockStringWithJobs(List clockTimes, List jobs) {
    	Iterator jobItr = null;
    	String curJob = null;
    	if (jobs != null) {
    		jobItr = jobs.iterator();
    	}
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
            if (jobItr != null && jobItr.hasNext()) {
            	curJob = (String)jobItr.next();
            	if (curJob != null) {
            		clk.addClockData("JOB=" + curJob);
            	}
            }
            clk.setClockDate(dat);
            clk.setClockType( (i % 2 == 0) ? Clock.TYPE_ON : Clock.TYPE_OFF);
            clocks.add(clk);
        }
        return Clock.createStringFromClockList(clocks);
    }
}

