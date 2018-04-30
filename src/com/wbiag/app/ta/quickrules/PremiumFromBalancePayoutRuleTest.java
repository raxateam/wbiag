package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.quickrules.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.rules.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;

import junit.framework.*;

import java.util.*;

public class PremiumFromBalancePayoutRuleTest extends RuleTestCase {

	private final int empId = 3;
	private final Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Wed");

	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PremiumFromBalancePayoutRuleTest.class);

	public PremiumFromBalancePayoutRuleTest(String testName) throws Exception {
		super(testName);
	}

	public static TestSuite suite() {
		TestSuite result = new TestSuite();
		result.addTestSuite(PremiumFromBalancePayoutRuleTest.class);
		return result;
	}

	public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(suite());
	}

	public void testFixed() throws Exception {
		double setBalanceUnits = 20.0;
		double fixedPayoutUnits = 5.0;
		double minutesPerUnit = 60.0;

		// prepare: calculate expected values
		double minutesPaidOut = (fixedPayoutUnits * minutesPerUnit);
		double remainingBalanceUnits = (setBalanceUnits - fixedPayoutUnits);

		// prepare: create balance and time code to be used
		BalanceData balanceData = insertBalance("TEST BALANCE", 2); // hours
		TimeCodeData timeCodeData = insertTimeCode("TEST TIME CODE");
		associateTimeCodeBalance(balanceData.getBalId(), timeCodeData.getTcodeId(), 5); // decrement hours

		// prepare: add rule to be executed
		Rule rule = new PremiumFromBalancePayoutRule();
		Parameters ruleParameters = getRuleParameters(
			  PremiumFromBalancePayoutRule.PARAM_PAYOUT_FIXED
			, fixedPayoutUnits
			, balanceData.getBalName()
			, timeCodeData.getTcodeName()
			, "REG"
			, minutesPerUnit
		);
		clearAndAddRule(empId, start, rule, ruleParameters);

		// prepare: increment balance
		OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
		ovrBuilder.setCreatesDefaultRecords(true);
		ovrBuilder.add(createBalanceOverride(empId, balanceData.getBalId(), setBalanceUnits, start));
		ovrBuilder.execute(true, false);

		// test: premium minutes paid out
		assertWorkPremiumTimeCodeMinutes(empId, start, timeCodeData.getTcodeName(), (int)minutesPaidOut);
		// test: balance new value
        assertTrue("Balance check", remainingBalanceUnits == getEmployeeBalanceValueAsOfEndOfDate(empId , start, balanceData.getBalId()));
	}

	public void testFixedMulti() throws Exception {
		double setBalanceUnits = 20.0;
		double fixedPayoutUnits = 5.0;
		double minutesPerUnit = 60.0;

		// prepare: calculate expected values
		// because of the balance association, two times the amount of units are deducted
		double minutesPaidOut = ((2 * fixedPayoutUnits) * minutesPerUnit);
		double remainingBalanceUnits = (setBalanceUnits - (2 * fixedPayoutUnits));

		// prepare: create balance and time code to be used
		BalanceData balanceData1 = insertBalance("TEST BALANCE 1", 2); // hours
		BalanceData balanceData2 = insertBalance("TEST BALANCE 2", 2); // hours
		TimeCodeData timeCodeData1 = insertTimeCode("TEST TIME CODE 1");
		associateTimeCodeBalance(balanceData1.getBalId(), timeCodeData1.getTcodeId(), 5); // decrement hours
		associateTimeCodeBalance(balanceData2.getBalId(), timeCodeData1.getTcodeId(), 5); // decrement hours

		// prepare: add rule to be executed
		Rule rule = new PremiumFromBalancePayoutRule();
		Parameters ruleParameters = getRuleParameters(
			  PremiumFromBalancePayoutRule.PARAM_PAYOUT_FIXED
			, fixedPayoutUnits
			, (balanceData1.getBalName() + "," + balanceData2.getBalName())
			, (timeCodeData1.getTcodeName())
			, "REG"
			, minutesPerUnit
		);
		clearAndAddRule(empId, start, rule, ruleParameters);

		// prepare: increment balances
		OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
		ovrBuilder.setCreatesDefaultRecords(true);
		ovrBuilder.add(createBalanceOverride(empId, balanceData1.getBalId(), setBalanceUnits, start));
		ovrBuilder.add(createBalanceOverride(empId, balanceData2.getBalId(), setBalanceUnits, start));
		ovrBuilder.execute(true, false);

		// test: premium minutes paid out
		assertWorkPremiumTimeCodeMinutes(empId, start, timeCodeData1.getTcodeName(), (int)minutesPaidOut);
		// test: balance new value
        assertTrue("Balance check " + remainingBalanceUnits + " " + getEmployeeBalanceValueAsOfEndOfDate(empId , start, balanceData1.getBalId()), remainingBalanceUnits == getEmployeeBalanceValueAsOfEndOfDate(empId , start, balanceData1.getBalId()));
        assertTrue("Balance check " + remainingBalanceUnits + " " + getEmployeeBalanceValueAsOfEndOfDate(empId , start, balanceData2.getBalId()), remainingBalanceUnits == getEmployeeBalanceValueAsOfEndOfDate(empId , start, balanceData1.getBalId()));
	}

	public void testToFinalWithWorkDetails() throws Exception {
		double setBalanceUnits = 20.0;
		double toFinalPayoutUnits = 0.0;
		double minutesPerUnit = 60.0;
		double workDetailBalanceUnits = 6.0; // 2 x 3 hour work detail records

		// prepare: calculate expected values
		double minutesPaidOut = ((setBalanceUnits - workDetailBalanceUnits - toFinalPayoutUnits ) * minutesPerUnit);
		double remainingBalanceUnits = toFinalPayoutUnits;

		// prepare: create balance and time code to be used
		BalanceData balanceData = insertBalance("TEST BALANCE 3", 2); // hours
		TimeCodeData timeCodeData = insertTimeCode("TEST TIME CODE 3");
		associateTimeCodeBalance(balanceData.getBalId(), timeCodeData.getTcodeId(), 5); // decrement hours

		// prepare: add rule to be executed
		Rule rule = new PremiumFromBalancePayoutRule();
		Parameters ruleParameters = getRuleParameters(
			  PremiumFromBalancePayoutRule.PARAM_PAYOUT_TO_FINAL
			, toFinalPayoutUnits
			, balanceData.getBalName()
			, timeCodeData.getTcodeName()
			, "REG"
			, minutesPerUnit
		);
		clearAndAddRule(empId, start, rule, ruleParameters);

		// prepare: increment balance
		OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
		ovrBuilder.setCreatesDefaultRecords(true);
		ovrBuilder.add(createBalanceOverride(empId, balanceData.getBalId(), setBalanceUnits, start));
		// prepare: add work details at 3 hour intervals
		ovrBuilder.add(createWrkdOverride(empId, timeCodeData.getTcodeName(), start, DateHelper.addMinutes(start, 9*60), DateHelper.addMinutes(start, 12*60)));
		ovrBuilder.add(createWrkdOverride(empId, timeCodeData.getTcodeName(), start, DateHelper.addMinutes(start, 12*60), DateHelper.addMinutes(start, 15*60)));
		ovrBuilder.execute(true, false);

		// test: premium minutes paid out
		assertWorkPremiumTimeCodeMinutes(empId, start, timeCodeData.getTcodeName(), (int)minutesPaidOut);
		// test: balance new value
        assertTrue("Balance check", remainingBalanceUnits == getEmployeeBalanceValueAsOfEndOfDate(empId , start, balanceData.getBalId()));
	}

	protected Parameters getRuleParameters(	  String payoutApproach
											, double payoutApproachValue
											, String balanceNames
											, String timeCodeNames
											, String hourTypeNames
											, double minutesPerUnit) throws Exception {
		Parameters ruleParams = new Parameters();
		ruleParams.addParameter(PremiumFromBalancePayoutRule.PARAM_BALANCE_NAMES, balanceNames);
		ruleParams.addParameter(PremiumFromBalancePayoutRule.PARAM_TIME_CODE_NAMES, timeCodeNames);
		ruleParams.addParameter(PremiumFromBalancePayoutRule.PARAM_HOUR_TYPE_NAMES, hourTypeNames);
		ruleParams.addParameter(PremiumFromBalancePayoutRule.PARAM_PAYOUT_APPROACH, payoutApproach);
		ruleParams.addParameter(PremiumFromBalancePayoutRule.PARAM_PAYOUT_APPROACH_VALUE, String.valueOf(payoutApproachValue));
		ruleParams.addParameter(PremiumFromBalancePayoutRule.PARAM_MINUTES_PER_UNIT, String.valueOf(minutesPerUnit));

		return ruleParams;
	}

	protected InsertEmployeeBalanceOverride createBalanceOverride(int empId, int balanceId, double balanceValue, Date overrideStartDate) throws Exception {
        InsertEmployeeBalanceOverride insBal = new InsertEmployeeBalanceOverride(getConnection());
        insBal.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insBal.setEmpId(empId);
        insBal.setStartDate(overrideStartDate);
        insBal.setEndDate(overrideStartDate);
        insBal.setEmpbalActionSET();
        insBal.setBalId(balanceId);
        insBal.setEmpbalValue(balanceValue);
        return insBal;
	}

	protected InsertWorkDetailOverride createWrkdOverride(int empId, String timeCodeName, Date wrksDate, Datetime start, Datetime end) throws Exception {
		InsertWorkDetailOverride insWrkd = new InsertWorkDetailOverride(getConnection());
        insWrkd.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insWrkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insWrkd.setEmpId(empId);
        insWrkd.setWrkdTcodeName(timeCodeName);
        insWrkd.setWrkdHtypeName("REG");
        insWrkd.setStartDate(wrksDate);
        insWrkd.setEndDate(wrksDate);
        insWrkd.setStartTime(start);
        insWrkd.setEndTime(end);
		return insWrkd;
	}

	protected BalanceData insertBalance(String balanceName, int balTypId) throws Exception {
		BalanceData balanceData = new BalanceData();
		int balId = getConnection().getDBSequence("seq_bal_id").getNextValue();
		balanceData.setBalId(balId);
		balanceData.setBaltypId(balTypId);
		balanceData.setBalName(balanceName);
		balanceData.setBalDesc("deleteme - only for JUnit testing");
		balanceData.setBalMax(99999);
		balanceData.setBalMin(0);
		BalanceAccess balanceAccess = new BalanceAccess(getConnection());
		balanceAccess.insertRecordData(balanceData, "BALANCE");
		return balanceData;
	}

	protected TimeCodeData insertTimeCode(String timeCodeName) throws Exception {
		TimeCodeData timeCodeData = new TimeCodeData();
		int tCodeId = getConnection().getDBSequence("seq_tcode_id").getNextValue();
		timeCodeData.setTcodeId(tCodeId);
		timeCodeData.setTcodeName(timeCodeName);
		timeCodeData.setTcodeAffectsBalances("Y");
		timeCodeData.setTcodeDesc("deleteme - only for JUnit testing");
		timeCodeData.setTcodeDow("YYYYYYY");
		timeCodeData.setTcodeOws("N");
		timeCodeData.setColrId(10);
		timeCodeData.setHtypeId(1);
		timeCodeData.setTcodeUnauthorize("N");
		timeCodeData.setTcodeSortorder(99999);
		timeCodeData.setTcodeEtmWdHide("Y");
		timeCodeData.setLmsId(1);
		timeCodeData.setTcodeIsLta("N");
		TimeCodeAccess timeCodeAccess = new TimeCodeAccess(getConnection());
		timeCodeAccess.insert(timeCodeData);
		return timeCodeData;
	}

	protected TimeCodeBalanceData associateTimeCodeBalance(int balanceId, int timeCodeId, int tcbtId) throws Exception {
		TimeCodeBalanceData timeCodeBalanceData = new TimeCodeBalanceData();
		int tCodeBalId = getConnection().getDBSequence("seq_tcode_id").getNextValue();
		timeCodeBalanceData.setTcodebalId(tCodeBalId);
		timeCodeBalanceData.setBalId(balanceId);
		timeCodeBalanceData.setTcodeId(timeCodeId);
		timeCodeBalanceData.setTcbtId(tcbtId);
		RecordAccess recordAccess = new RecordAccess(getConnection());
		recordAccess.insertRecordData(timeCodeBalanceData, "TIME_CODE_BALANCE");
		return timeCodeBalanceData;
	}
}
