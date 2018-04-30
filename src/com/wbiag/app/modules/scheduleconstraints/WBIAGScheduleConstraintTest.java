package com.wbiag.app.modules.scheduleconstraints;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import junit.framework.TestSuite;

import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.model.EmployeeScheduleData;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.ruleengine.CalcSimulationAccess;
import com.workbrain.app.ta.ruleengine.CalcSimulationContext;
import com.workbrain.app.ta.ruleengine.CalcSimulationEmployee;
import com.workbrain.app.ta.ruleengine.CalcSimulationException;
import com.workbrain.app.ta.ruleengine.TATestCase;
import com.workbrain.test.TestUtil;
import com.workbrain.tool.overrides.InsertEmployeeScheduleOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.DateHelper;
import com.workbrain.util.NestedRuntimeException;

public class WBIAGScheduleConstraintTest extends TATestCase 
{
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger
	.getLogger(WBIAGScheduleConstraintTest.class);

public WBIAGScheduleConstraintTest(String testName) throws Exception {
super(testName);
}

public static TestSuite suite() {
TestSuite result = new TestSuite();
result.addTestSuite(WBIAGScheduleConstraintTest.class);
return result;
}

protected void setUp() throws Exception 
{
super.setUp();
init();
CodeMapper.invalidateSCConstraint();
}

/**
* Tests case 1: Constraint applied from work summary
*/
public void testScheduledTimeOverrideWillError() throws Exception {

OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(
		getConnection());
final int shftStartMinutes = 9 * 60, shftEndMinutes = 21 * 60;
final int empId = 3;

//Setup 12 hour schedules for two days back

Date dayBack = DateHelper.addDays(DateHelper.getCurrentDate(), -1);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayBack);
ins.setEndDate(dayBack);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayBack,
		shftStartMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayBack, shftEndMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

dayBack = DateHelper.addDays(DateHelper.getCurrentDate(), -2);
Date dayBack2 = DateHelper.addDays(DateHelper.getCurrentDate(), -1);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayBack);
ins.setEndDate(dayBack);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayBack,
		shftEndMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayBack2, shftStartMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

//Setup 12 hour schedules for one day forward
Date dayForward = DateHelper.addDays(DateHelper.getCurrentDate(), 1);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayForward);
ins.setEndDate(dayForward);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayForward,
		shftStartMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayForward,
		shftEndMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

TestUtil
		.getInstance()
		.setVarTemp(
				"/"
						+ com.workbrain.app.modules.ModuleHelper.MODULE_SCHEDULE_CONSTRAINTS,
				"true");
createEarlyWorkScheduleConstraint("X CONSECUTIVE Y HOUR SHIFTS",
		"X CONSECUTIVE Y HOUR SHIFTS", "4,12");

Date targetDate = DateHelper.getCurrentDate();
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(targetDate);
ins.setEndDate(targetDate);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(targetDate,
		shftStartMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(targetDate,
		shftEndMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

// *** compliance error
// *** error ovr
assertTrue(0 < ovrBuilder.getCalculationResult()
		.getScheduleComplianceErrors().size());
assertOverrideStatusCount(ovrBuilder, OverrideData.ERROR, 4);
// *** skds must be updated.
EmployeeScheduleData skd = getEmployeeScheduleData(empId, targetDate);
assertFalse(shftStartMinutes * DateHelper.MINUTE_MILLISECODS == DateHelper
		.getDayFraction(skd.getEmpskdActStartTime()));
assertFalse(shftEndMinutes * DateHelper.MINUTE_MILLISECODS == DateHelper
		.getDayFraction(skd.getEmpskdActEndTime()));


}

/**Constraint being tested using calc simulation context: this simulates eligibility condition check*/

public void testScheduledTimeOverrideWillError2() throws Exception {

OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(
		getConnection());
final int shftStartMinutes = 9 * 60, shftEndMinutes = 21 * 60;
final int empId = 3;

//Setup 12 hour schedules for two days back

Date dayBack = DateHelper.addDays(DateHelper.getCurrentDate(), -1);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayBack);
ins.setEndDate(dayBack);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayBack,
		shftStartMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayBack, shftEndMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

dayBack = DateHelper.addDays(DateHelper.getCurrentDate(), -2);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayBack);
ins.setEndDate(dayBack);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayBack,
		shftStartMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayBack, shftEndMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

//Setup 12 hour schedules for one day forward
Date dayForward = DateHelper.addDays(DateHelper.getCurrentDate(), 1);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayForward);
ins.setEndDate(dayForward);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayForward,
		shftStartMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayForward,
		shftEndMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

CalcSimulationContext ctx = new CalcSimulationContext(getConnection(),
		getCodeMapper());

TestUtil
		.getInstance()
		.setVarTemp(
				"/"
						+ com.workbrain.app.modules.ModuleHelper.MODULE_SCHEDULE_CONSTRAINTS,
				"true");
createEarlyWorkScheduleConstraint("X CONSECUTIVE Y HOUR SHIFTS",
		"X CONSECUTIVE Y HOUR SHIFTS", "4,720");

ctx.setAppliesScheduleConstraints(true);
ctx.setExecutesCalcDataCacheSave(false);
CalcSimulationAccess csa = new CalcSimulationAccess(ctx);

csa.addEmployeeDate(empId, DateHelper.addDays(DateHelper
		.getCurrentDate(), -2), DateHelper.addDays(DateHelper
		.getCurrentDate(), -1));
csa.addEmployeeDate(empId, DateHelper.addDays(DateHelper
		.getCurrentDate(), 1), DateHelper.addDays(DateHelper
		.getCurrentDate(), 1));
try {
	csa.load();
} catch (CalcSimulationException ex) {
	throw new NestedRuntimeException(ex);
}
csa.addEmployeeDate(empId, DateHelper.addDays(DateHelper
		.getCurrentDate(), 0), DateHelper.addDays(DateHelper
		.getCurrentDate(), 0));

try {
	csa.load();
} catch (CalcSimulationException ex) {
	throw new NestedRuntimeException(ex);
}
CalcSimulationEmployee csemp = csa.getResultForEmp(empId);
assertTrue(csemp.getFirstScheduleConstraintException() != null);

}

/**
* Tests case 3: Constraint applied from work summary: No error
*/
public void testScheduledTimeOverrideWillNotError() throws Exception {

OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(
		getConnection());
final int shftStartMinutes = 9 * 60, shftEndMinutes = 21 * 60;
final int shftStartMinutes2 = 10 * 60, shftEndMinutes2 = 11 * 60;
final int empId = 3;

//Setup 12 hour schedules for two days back

Date dayBack = DateHelper.addDays(DateHelper.getCurrentDate(), -1);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayBack);
ins.setEndDate(dayBack);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayBack,
		shftStartMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayBack, shftEndMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

dayBack = DateHelper.addDays(DateHelper.getCurrentDate(), -2);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayBack);
ins.setEndDate(dayBack);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayBack,
		shftStartMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayBack, shftEndMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

//Setup 12 hour schedules for one day forward
Date dayForward = DateHelper.addDays(DateHelper.getCurrentDate(), 1);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayForward);
ins.setEndDate(dayForward);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayForward,
		shftStartMinutes2));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayForward,
		shftEndMinutes2));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

TestUtil
		.getInstance()
		.setVarTemp(
				"/"
						+ com.workbrain.app.modules.ModuleHelper.MODULE_SCHEDULE_CONSTRAINTS,
				"true");
createEarlyWorkScheduleConstraint("X CONSECUTIVE Y HOUR SHIFTS",
		"X CONSECUTIVE Y HOUR SHIFTS", "4,720");

Date targetDate = DateHelper.getCurrentDate();
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(targetDate);
ins.setEndDate(targetDate);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(targetDate,
		shftStartMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(targetDate,
		shftEndMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

// *** compliance error
// *** error ovr
assertTrue(0 == ovrBuilder.getCalculationResult()
		.getScheduleComplianceErrors().size());
EmployeeScheduleData skd = getEmployeeScheduleData(empId, targetDate);
assertTrue(shftStartMinutes * DateHelper.MINUTE_MILLISECODS == DateHelper
		.getDayFraction(skd.getEmpskdActStartTime()));
assertTrue(shftEndMinutes * DateHelper.MINUTE_MILLISECODS == DateHelper
		.getDayFraction(skd.getEmpskdActEndTime()));

}

/**Constraint being tested using calc simulation context: this simulates eligibility condition check*/

public void testScheduledTimeOverrideWillNotError2() throws Exception {

OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(
		getConnection());
final int shftStartMinutes = 9 * 60, shftEndMinutes = 21 * 60;
final int shftStartMinutes2 = 10 * 60, shftEndMinutes2 = 11 * 60;
final int empId = 3;

//Setup 12 hour schedules for two days back

Date dayBack = DateHelper.addDays(DateHelper.getCurrentDate(), -1);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayBack);
ins.setEndDate(dayBack);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayBack,
		shftStartMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayBack, shftEndMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

dayBack = DateHelper.addDays(DateHelper.getCurrentDate(), -2);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayBack);
ins.setEndDate(dayBack);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayBack,
		shftStartMinutes));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayBack, shftEndMinutes));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

//Setup 12 hour schedules for one day forward
Date dayForward = DateHelper.addDays(DateHelper.getCurrentDate(), 1);
ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
ins.setEmpId(empId);
ins.setStartDate(dayForward);
ins.setEndDate(dayForward);
ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
ins.setEmpskdActStartTime(DateHelper.addMinutes(dayForward,
		shftStartMinutes2));
ins.setEmpskdActEndTime(DateHelper.addMinutes(dayForward,
		shftEndMinutes2));
ovrBuilder.add(ins);
ovrBuilder.execute(true, false);

CalcSimulationContext ctx = new CalcSimulationContext(getConnection(),
		getCodeMapper());

TestUtil
		.getInstance()
		.setVarTemp(
				"/"
						+ com.workbrain.app.modules.ModuleHelper.MODULE_SCHEDULE_CONSTRAINTS,
				"true");
createEarlyWorkScheduleConstraint("X CONSECUTIVE Y HOUR SHIFTS",
		"X CONSECUTIVE Y HOUR SHIFTS", "4,720");

ctx.setAppliesScheduleConstraints(true);
ctx.setExecutesCalcDataCacheSave(false);
CalcSimulationAccess csa = new CalcSimulationAccess(ctx);

csa.addEmployeeDate(empId, DateHelper.addDays(DateHelper
		.getCurrentDate(), -2), DateHelper.addDays(DateHelper
		.getCurrentDate(), -1));
csa.addEmployeeDate(empId, DateHelper.addDays(DateHelper
		.getCurrentDate(), 1), DateHelper.addDays(DateHelper
		.getCurrentDate(), 1));
try {
	csa.load();
} catch (CalcSimulationException ex) {
	throw new NestedRuntimeException(ex);
}
csa.addEmployeeDate(empId, DateHelper.addDays(DateHelper
		.getCurrentDate(), 0), DateHelper.addDays(DateHelper
		.getCurrentDate(), 0));

try {
	csa.load();
} catch (CalcSimulationException ex) {
	throw new NestedRuntimeException(ex);
}
CalcSimulationEmployee csemp = csa.getResultForEmp(empId);
assertTrue(csemp.getFirstScheduleConstraintException() == null);

}

protected void createEarlyWorkScheduleConstraint(String scName,
	String scMsg, String param) throws SQLException {

PreparedStatement stm = null;
try {
	String sql = "INSERT INTO SC_CONSTRAINT ( "
			+ " SC_ID, SC_NAME, SC_DESC, SC_IS_ENABLED, SC_CLASS, SC_UDF_CHK_1_FIRE, SC_UDF_CHK_1_PARM, SC_UDF_CHK_1_MSG) "
			+ " VALUES ( ?,?,?,?,?,?,?,?)";
	stm = getConnection().prepareStatement(sql);
	stm.setInt(1, getConnection().getDBSequence("seq_sc_id")
			.getNextValue());
	stm.setString(2, scName);
	stm.setString(3, scName);
	stm.setString(4, "Y");
	stm
			.setString(
					5,
					"com.vanderbilt.workbrain.scibo.app.modules.scheduleconstraints.VBExtendedScheduleConstraint");
	stm.setString(6, "Y");
	stm.setString(7, param);
	stm.setString(8, scMsg);
	int upd = stm.executeUpdate();
	assertEquals("1 Constraint added", 1, upd);
} finally {
	if (stm != null) {
		stm.close();
	}
}
}

}
