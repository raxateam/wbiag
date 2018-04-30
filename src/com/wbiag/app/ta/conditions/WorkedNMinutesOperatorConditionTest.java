package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.util.*;
import com.workbrain.tool.overrides.*;
import java.util.*;

import junit.framework.*;

/**
 * Test for WorkedNMinutesOperatorCondition.
 */

public class WorkedNMinutesOperatorConditionTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WorkedNMinutesOperatorConditionTest.class);

    public WorkedNMinutesOperatorConditionTest(String testName)
        throws Exception
    {
        super(testName);
    }

    public static TestSuite suite()
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkedNMinutesOperatorConditionTest.class);
        return result;
    }

    public void testWorkedNMinsOperator()
        throws Exception
    {
        final int empId = 15;
        Date fri = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "FRI");
        Date mon = DateHelper.addDays(fri , -4);

        new CreateDefaultRecords(getConnection(), new int[] {empId}, mon, fri).execute(false);

        Condition condition = new WorkedNMinutesOperatorCondition();
        Parameters condParams = new Parameters();

        // *** create condition to evaluate TRUE
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_WORK_MINUTES , "480");
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        assertConditionTrue(empId, fri, condition, condParams);

        // *** create condition to evaluate FALSE
        condParams.removeAllParameters();
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_WORK_MINUTES , "480");
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_OPERATOR,RuleHelper.LESS);
        assertConditionFalse(empId, fri, condition, condParams);

        // *** create condition to evaluate TRUE
        condParams.removeAllParameters();
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_WORK_MINUTES , "2400");
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_OPERATOR,RuleHelper.EQ);
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_FROM_DAY,"MON");
        assertConditionTrue(empId, fri, condition, condParams);

    }

    public void testWorkedNMinsOperatorAnyOccurenceTrue()
        throws Exception
    {
        final int empId = 15;
        Date mon =  DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        new CreateDefaultRecords(getConnection(), new int[] {empId}, mon, mon).execute(false);



        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        InsertWorkDetailOverride insWrkd = new InsertWorkDetailOverride(getConnection());
        insWrkd.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insWrkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insWrkd.setEmpId(empId);
        insWrkd.setStartDate(mon);
        insWrkd.setEndDate(mon);
        insWrkd.setStartTime(DateHelper.addMinutes(mon, 12*60));
        insWrkd.setEndTime(DateHelper.addMinutes(mon, 13*60));
        insWrkd.setTcodeId(getCodeMapper().getTimeCodeByName("TRN").getTcodeId());
        ovrBuilder.add(insWrkd);

        ovrBuilder.execute(true, false);

        Condition condition = new WorkedNMinutesOperatorCondition();
        Parameters condParams = new Parameters();
        // *** create condition to evaluate TRUE
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_WORK_MINUTES , "240");
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_OPERATOR,RuleHelper.LESSEQ);
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_ANY_OCCURENCE, "true");
        assertConditionTrue(empId, mon, condition, condParams);
        // *** create condition to evaluate FALSE
        condParams.removeAllParameters();
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_WORK_MINUTES , "240");
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_OPERATOR,RuleHelper.LESSEQ);
        condParams.addParameter(WorkedNMinutesOperatorCondition.PARAM_ANY_OCCURENCE, "false");
        assertConditionFalse(empId, mon, condition, condParams);


    }

    public static void main(String[] args)
        throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }
}
