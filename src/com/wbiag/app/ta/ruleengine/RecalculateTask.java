package com.wbiag.app.ta.ruleengine;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

import com.wbiag.app.ta.db.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.scheduler.*;
import com.workbrain.app.scheduler.enterprise.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.security.*;
import com.workbrain.server.sql.*;
import com.workbrain.server.registry.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Recalculate task for
 * <ul>
 *  <li> recalculating  payperiod for making sure dependant days are calculated correctly
 *  <li> recalculating  future days where balance logs exist
 * </ul>
 * @deprecated    As of 4.1 FP28, use core classes
 */
public class RecalculateTask extends  com.wbiag.util.AbstractLastRunTask {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecalculateTask.class);
    private DBConnection conn = null;
    private StringBuffer taskLogMessage = new StringBuffer("Scheduled OK.");

    public static final String REG_EMPCOLS_TO_INV_CALC = "/system/ruleEngine/overrides/EMPLOYEE_COLUMNS_TO_INVOKE_CALC";
    public static final String REGVAL_EMPCOLS_TO_INV_CALC_ALL = "ALL";

    public static final String UDF_DATE_FORMAT = "yyyyMMdd HHmmss";
    public static final String ROLLOUT_DATE_FORMAT = "yyyyMMdd HHmmss";
    public static final int MAX_BIND_SIZE =  com.workbrain.tool.jdbc.proxy.ProxyPreparedStatement.MAX_PARAMETER;
    public static final int MAX_CALC_BATCH_SIZE = RuleAccess.BATCH_PROCESS_SIZE_DEFAULT;

    public static final String WBITYP_RECALC_NAME = "RECALCULATE TASK";
    public static final String  PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC = "OVRTYP_IDS_BUSINESS_RULES_RECALC";
    public static final String  PARAM_BUSINESS_RULE_RECALC_RANGE_TYPE = "CALC_RANGE_TYPE";
    public static final String  PARAM_BUSINESS_RULE_RECALC_TYPE_WEEK = "WEEK";
    public static final String  PARAM_BUSINESS_RULE_RECALC_TYPE_PAYPERIOD = "PAYPERIOD";
    public static final String  PARAM_BUSINESS_RULE_RECALC_INFINITE = "CALC_RANGE_INFINITE";
    public static final String  PARAM_CALCULATION_THREAD_COUNT = "CALCULATION_THREAD_COUNT";
    public static final String  PARAM_CHECK_PENDING_OVERRIDES = "CHECK_PENDING_OVERRIDES";
    public static final String  PARAM_CLIENT_ID = "CLIENT_ID";
    public static final String  PARAM_ROLLOUT_DATE = "ROLLOUT_DATE";
    public static final String  PARAM_BATCH_SIZE = "BATCH_SIZE";
    public static final String  PARAM_PROCESS_CALC_EMP_DATE_TABLE = "PROCESS_CALC_EMP_DATE_TABLE";
    public static final String  PARAM_PROCESS_NO_SHOW_EMPLOYEES = "PROCESS_NO_SHOW_EMPLOYEES";
    public static final String  PARAM_PROCESS_JOB_RATE_UPDATES = "PROCESS_JOB_RATE_UPDATES";
    public static final String  PARAM_PROCESS_CALCGRP_DEF_CHANGES = "PROCESS_CALCGRP_DEF_CHANGES";
    public static final String  PARAM_PROCESS_ENTEMPPOLICY_CHANGES = "PROCESS_ENTEMPPOLICY_CHANGES";
    public static final String  PARAM_PROCESS_CLOCKS_PROCESSED = "PROCESS_CLOCKS_PROCESSED";
    public static final String  PARAM_APPLY_TO_CALCGRPS = "APPLY_TO_CALCGRPS";
    public static final String  PARAM_APPLY_TO_CALCGRPS_INCLUSIVE = "APPLY_TO_CALCGRPS_INCLUSIVE";
    public static final String  PARAM_AUTO_RECALC = "AUTO_RECALC";
    public static final String  PARAM_FUTURE_BALANCE_RECALC = "FUTURE_BALANCE_RECALC";

    public static final String  PARAM_DS_LOGIN = "DS_LOGIN";
    public static final String  PARAM_DS_PASSWORD = "DS_PASSWORD";
    public static final String  PARAM_DS_DRIVER = "DS_DRIVER";
    public static final String  PARAM_DS_URL = "DS_URL";

    private String rolloutDate;
    private int batchSize;
    private boolean shouldCommit = true;
    private InterruptCheck intCheck;

    protected RecalculateTaskFilter filter = null;

    public RecalculateTask() {
    }

    /**
     * Runs scheduled task
     * @param taskID taskID
     * @param param params
     * @param schedule
     * @return
     * @throws Exception
     */
    public Status run(int taskID, Map param) throws Exception {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug("RecalculateTask.run(" + taskID + ", " + param + ")");

        String currentClientId = null;
        try {
            // *** store current client id
            currentClientId = SecurityService.getCurrentClientId();

            conn = getConnection();
            conn.turnTraceOff();
            conn.setAutoCommit(false);

            if (StringHelper.isEmpty((String)param.get( PARAM_CLIENT_ID ))) {
                throw new RuntimeException ("Client_id must be specified");
            }
            String clientId = (String) param.get( PARAM_CLIENT_ID );

            SecurityService.setCurrentClientId(clientId);
            execute(conn , param);

            conn.commit();
        } catch (Exception e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("com.wbiag.app.ta.ruleengine.RecalculateTask.class", e);}
            if( conn != null ) conn.rollback();
            throw e;
        } finally {
            SecurityService.setCurrentClientId(currentClientId);
            if( conn != null ) conn.close();
        }
        return jobOk(taskLogMessage.toString());

    }

    /**
     * TaskUI for parameters
     * @return
     */
    public String getTaskUI() {
        return "/jobs/recalculateTaskParams.jsp";
    }


    /**
     * Executes recalculate task based on parameters
     * @param c
     * @param param
     * @throws Exception
     */
    public void execute(DBConnection c , Map param) throws Exception {
        this.conn = c;

        rolloutDate = (String)param.get(PARAM_ROLLOUT_DATE);
        if (StringHelper.isEmpty(rolloutDate)) {
            throw new RuntimeException("Rollout date must be specified");
        }
        batchSize = MAX_CALC_BATCH_SIZE ;
        if (param.get(PARAM_BATCH_SIZE) != null) {
            batchSize = Integer.parseInt((String)param.get(PARAM_BATCH_SIZE));
        }
        if (batchSize > MAX_CALC_BATCH_SIZE) {
            throw new RuntimeException("Calculation batch size cannot be more than " + MAX_CALC_BATCH_SIZE);
        }
        int threadCnt = 1;
        if (param.get(PARAM_CALCULATION_THREAD_COUNT) != null) {
            threadCnt = Integer.parseInt((String)param.get(PARAM_CALCULATION_THREAD_COUNT));
        }
        // *** create filter based on params
        RecalculateTaskFilterContext context = new RecalculateTaskFilterContext();
        context.taskDateTime = new java.util.Date();
        context.empColsToInvokeCalc = Registry.getVarString(REG_EMPCOLS_TO_INV_CALC, REGVAL_EMPCOLS_TO_INV_CALC_ALL) ;

        context.lastRun = getLastRunDate(WBITYP_RECALC_NAME ,
                                         DateHelper.parseDate(rolloutDate, ROLLOUT_DATE_FORMAT));
        appendToTaskLogMessage("STARTED RecalculateTask since last run : " + context.lastRun + " at " + context.taskDateTime);

        context.brOvrtypIds = StringHelper.detokenizeStringAsIntArray(
            (String) param.get(
            PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC),
            "," , true);
        context.sRangeType = (String)param.get(PARAM_BUSINESS_RULE_RECALC_RANGE_TYPE);
        if (StringHelper.isEmpty(context.sRangeType)) {
            context.sRangeType = PARAM_BUSINESS_RULE_RECALC_TYPE_PAYPERIOD;
        }
        else if (!context.sRangeType.equals(PARAM_BUSINESS_RULE_RECALC_TYPE_PAYPERIOD)
                 && !context.sRangeType.equals(PARAM_BUSINESS_RULE_RECALC_TYPE_WEEK)) {
            throw new RuntimeException("Recalc range type must be :" +  PARAM_BUSINESS_RULE_RECALC_TYPE_PAYPERIOD + " or " + PARAM_BUSINESS_RULE_RECALC_TYPE_WEEK);
        }
        String sBusRecInf = (String)param.get(PARAM_BUSINESS_RULE_RECALC_INFINITE);
        context.isRangeInfinite = StringHelper.isEmpty(sBusRecInf)
            ? false : "Y".equals(sBusRecInf) ? true : false;

        String sChecksForPendingOvrs = (String)param.get(PARAM_CHECK_PENDING_OVERRIDES);
        context.checksForPendingOvrs = StringHelper.isEmpty(sChecksForPendingOvrs)
            ? false : "Y".equals(sChecksForPendingOvrs) ? true : false;

        String sProcessNoShowEmps = (String)param.get(PARAM_PROCESS_NO_SHOW_EMPLOYEES);
        context.processesNoShowEmps = StringHelper.isEmpty(sProcessNoShowEmps)
            ? false : "Y".equals(sProcessNoShowEmps) ? true : false;

        String sProcessJobRate = (String)param.get(PARAM_PROCESS_JOB_RATE_UPDATES);
        context.processesJobRateUpdates = StringHelper.isEmpty(sProcessJobRate)
            ? false : "Y".equals(sProcessJobRate) ? true : false;

        String sProcessEmpDateRecs = (String)param.get(PARAM_PROCESS_CALC_EMP_DATE_TABLE);
        context.processesEmpDateRecs = StringHelper.isEmpty(sProcessEmpDateRecs)
            ? false : "Y".equals(sProcessEmpDateRecs) ? true : false;


        String sProcessCgDefChanges = (String)param.get(PARAM_PROCESS_CALCGRP_DEF_CHANGES);
        context.processesCgDefChanges = StringHelper.isEmpty(sProcessCgDefChanges)
            ? false : "Y".equals(sProcessCgDefChanges) ? true : false;

        String sProcessEntemppolChanges = (String)param.get(PARAM_PROCESS_ENTEMPPOLICY_CHANGES);
        context.processesEntEmpPolChanges = StringHelper.isEmpty(sProcessEntemppolChanges)
            ? false : "Y".equals(sProcessEntemppolChanges) ? true : false;

        String sProcessClk = (String)param.get(PARAM_PROCESS_CLOCKS_PROCESSED);
        context.processesClocksProcessed = StringHelper.isEmpty(sProcessClk)
            ? false : "Y".equals(sProcessClk) ? true : false;

        context.applyCalcgrpIds = StringHelper.detokenizeStringAsIntArray(
            (String) param.get(PARAM_APPLY_TO_CALCGRPS),  "," , true);

        String sApplyCalcgrpIdsInclusive = (String)param.get(PARAM_APPLY_TO_CALCGRPS_INCLUSIVE);
        context.applyCalcgrpIdsInclusive = StringHelper.isEmpty(sApplyCalcgrpIdsInclusive)
            ? true : "Y".equals(sApplyCalcgrpIdsInclusive) ? true : false;

        String sAutoRecalc =  (String)param.get(PARAM_AUTO_RECALC);
        if (!StringHelper.isEmpty(sAutoRecalc)) {
            context.autoRecalc = Boolean.valueOf(sAutoRecalc).booleanValue();
        }
        String sFutBalRecalc =  (String)param.get(PARAM_FUTURE_BALANCE_RECALC);
        if (!StringHelper.isEmpty(sFutBalRecalc)) {
            context.futureBalanceRecalc = Boolean.valueOf(sFutBalRecalc);
        }

        if (intCheck != null && isInterrupted()) {
            logger.warn("Interrupting Recalculate Task");
            return;
        }

        filter = new RecalculateTaskFilter(context);

        doRecalcs(threadCnt , filter);

        finalizeTask(context.taskDateTime );

        appendToTaskLogMessage("FINISHED in : " + (System.currentTimeMillis() - context.taskDateTime.getTime()) + " .ms");
    }

    protected String getTaskLogMessage() {
        return taskLogMessage.toString();
    }

    protected void doRecalcs(int threadCnt , RecalculateTaskFilter filter) throws SQLException{
        // *** if anything to calculate
        if (filter.getEmployeeIdAndDateListToRecalc().size() > 0) {
            appendToTaskLogMessage ("Calculating : " + filter.getEmployeeIdAndDateListToRecalc().size() + " employee dates");
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug ("Recalcing emp/Dates : \n" + filter.getEmployeeIdAndDateListToRecalc());
            filter.getEmployeeIdAndDateListToRecalc().sort();

            recalcRecords(filter , threadCnt);
        }
        else {
            appendToTaskLogMessage ("Found no employee dates to recalculate");
        }
    }

    protected void recalcRecords(RecalculateTaskFilter filter , int threadCnt) throws SQLException{
        RecalculateRecords rec = new RecalculateRecords(conn);
        if (filter.getCalcEmployeeDateTableRecords() != null
            && filter.getCalcEmployeeDateTableRecords().size() > 0) {
            rec.setDeletesCalcEmployeeDateTable(true);
        }
        rec.setProcessLevelAutoRecalc(filter.getContext().autoRecalc);
        rec.setProcessLevelFutureBalanceRecalc(filter.getContext().futureBalanceRecalc);
        rec.setInterruptCheck(this);
        rec.setAuditsCalculationErrorEmpDates(true);
        rec.setCalculationBatchSize(batchSize);
        rec.setSlaveThreadCount(threadCnt);
        rec.setCommitsEveryCalculationBatch(shouldCommit);
        rec.setCreatesDefaultRecords(false);
        Iterator iter = filter.getEmployeeIdAndDateListToRecalc().iterator();
        while (iter.hasNext()) {
            EmployeeIdAndDate item = (EmployeeIdAndDate)iter.next();
            rec.addEmployeeDate(item.getEmpId() , item.getDate());
        }
        rec.execute();
        if (rec.getCalculationEmployeeDatesErrors().size() > 0) {
            Iterator iterErr = rec.getCalculationEmployeeDatesErrors().iterator();
            logger.error("Errors occurred during recalculation of below employee dates");
            while (iterErr.hasNext()) {
                RecalculateRecords.CalcEmployeeDatesError item = (RecalculateRecords.CalcEmployeeDatesError)iterErr.next();
                logger.error(item);
            }
        }
    }

    protected void finalizeTask(Date taskDateTime) throws Exception{
        // *** delete processed CALC_EMPLOYEE_DATE recs
        // *** TT 587 moved to RecalculateRecords to be done at every batch
        //if (calcEmpDateRecs != null && calcEmpDateRecs.size() > 0) {
        //    deleteCalcEmployeeDate(conn , calcEmpDateRecs);
        //}
        updateLastRunDate(taskDateTime, WBITYP_RECALC_NAME);
    }


    /**
     * Returns list of dates recalculated
     * @return
     */
    public EmployeeIdAndDateList getEmpDatesToRecalc() {
        return filter.getEmployeeIdAndDateListToRecalc();
    }

    /**
     * Used for unit testing. Task always commits from scheduler
     * @param v
     */
    public void setShouldCommit(boolean v) {
        shouldCommit = v;
    }

    protected void appendToTaskLogMessage(String s) {
        taskLogMessage.append("<br>" + s);
        if (logger.isDebugEnabled()) logger.debug(s);
    }

    private int[] getEmpIdsFromEmpDateList(EmployeeIdAndDateList empDates) {
        IntegerList list = new IntegerList();
        if (empDates == null || empDates.size() == 0) {
            return list.toIntArray();
        }
        // *** contains is too expensive, keep a temp HashSet
        Set internalList = new HashSet();
        Iterator iter = empDates.iterator();
        while (iter.hasNext()) {
            EmployeeIdAndDate item = (EmployeeIdAndDate)iter.next();
            Integer empId = new Integer(item.getEmpId());
            if (!internalList.contains(empId)) {
                list.add(item.getEmpId());
                internalList.add(empId);
            }
        }
        return list.toIntArray();
    }

    /**
     * Runs task from properties file
     * @param args
     * @throws Exception
     */
    public static void main( String[] args ) throws Exception {
        //String propFile = "C:\\source\\4.0_SP4\\Java\\src\\com\\wbiag\\app\\ta\\ruleengine\\RecalculateTask.properties";
        String propFile = args[0];
        if (args.length == 0) {
            throw new RuntimeException("Property file path must be supplied");
        }
        if (!com.workbrain.util.FileUtil.fileExists(args[0])) {
            throw new RuntimeException("Property file not found");
        }

        Properties props = new Properties();
        props.load(new FileInputStream(propFile));

        System.setProperty("junit.db.username" , (String)props.get(PARAM_DS_LOGIN));
        System.setProperty("junit.db.password" , (String)props.get(PARAM_DS_PASSWORD));
        System.setProperty("junit.db.url" , (String)props.get(PARAM_DS_URL));
        System.setProperty("junit.db.driver" , (String)props.get(PARAM_DS_DRIVER));

        DBConnection c = SQLHelper.connectTo();
        c.setAutoCommit( false );
        c.turnTraceOff();


        RecalculateTask task = new RecalculateTask();
        Map params = new HashMap();
        params.put(PARAM_CALCULATION_THREAD_COUNT ,
                   (String)props.get(PARAM_CALCULATION_THREAD_COUNT));
        params.put(PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC ,
                   (String)props.get(PARAM_OVRTYP_IDS_BUSINESS_RULES_RECALC));
        params.put(PARAM_BUSINESS_RULE_RECALC_RANGE_TYPE ,
                   (String)props.get(PARAM_BUSINESS_RULE_RECALC_RANGE_TYPE));
        params.put(PARAM_BUSINESS_RULE_RECALC_INFINITE,
                   (String)props.get(PARAM_BUSINESS_RULE_RECALC_INFINITE));
        params.put(PARAM_CHECK_PENDING_OVERRIDES ,
                   (String)props.get(PARAM_CHECK_PENDING_OVERRIDES));
        params.put(PARAM_CLIENT_ID ,(String)props.get(PARAM_CLIENT_ID));
        params.put(PARAM_ROLLOUT_DATE,
                   (String)props.get(PARAM_ROLLOUT_DATE));
        params.put(PARAM_BATCH_SIZE,
                   (String)props.get(PARAM_BATCH_SIZE));
        params.put(PARAM_PROCESS_CALC_EMP_DATE_TABLE,
                   (String)props.get(PARAM_PROCESS_CALC_EMP_DATE_TABLE));
        params.put(PARAM_PROCESS_JOB_RATE_UPDATES,
                   (String)props.get(PARAM_PROCESS_JOB_RATE_UPDATES));
        params.put(PARAM_PROCESS_CALCGRP_DEF_CHANGES,
                   (String)props.get(PARAM_PROCESS_CALCGRP_DEF_CHANGES));
        params.put(PARAM_PROCESS_ENTEMPPOLICY_CHANGES,
                   (String)props.get(PARAM_PROCESS_ENTEMPPOLICY_CHANGES));
        params.put(PARAM_PROCESS_CLOCKS_PROCESSED,
                   (String)props.get(PARAM_PROCESS_CLOCKS_PROCESSED));
        params.put(PARAM_APPLY_TO_CALCGRPS,
                   (String)props.get(PARAM_APPLY_TO_CALCGRPS));
        params.put(PARAM_APPLY_TO_CALCGRPS_INCLUSIVE,
                   (String)props.get(PARAM_APPLY_TO_CALCGRPS_INCLUSIVE));
        com.workbrain.security.SecurityService.setCurrentClientId((String)params.get(PARAM_CLIENT_ID));

        task.execute(c , params);
        if (logger.isDebugEnabled()) logger.debug(task.getTaskLogMessage());
        c.commit();
    }

    class RecalculateTaskFilterContext {
        public int[] brOvrtypIds;
        public boolean checksForPendingOvrs;
        public boolean processesNoShowEmps;
        public boolean processesJobRateUpdates;
        public boolean processesEmpDateRecs;
        public boolean processesCgDefChanges;
        public boolean processesEntEmpPolChanges;
        public boolean processesClocksProcessed;
        public String sRangeType;
        public boolean isRangeInfinite;
        public Date lastRun;
        public Date taskDateTime;
        public int[] applyCalcgrpIds;
        public boolean applyCalcgrpIdsInclusive;
        public String empColsToInvokeCalc;
        public boolean autoRecalc;
        public Boolean futureBalanceRecalc;
    }

    class RecalculateTaskFilter {

        private EmployeeIdAndDateList empDatesRecalcFilter = new EmployeeIdAndDateList(false);
        private List calcEmpDaterecs = null;
        private RecalculateTaskFilterContext context;

        public RecalculateTaskFilter(RecalculateTaskFilterContext context) throws Exception{

            this.context = context;
            if ((context.brOvrtypIds != null && context.brOvrtypIds.length > 0)
                || context.processesClocksProcessed) {
                processBusinessRecalcs(context) ;
            }
            if (context.checksForPendingOvrs) {
                processPendingOverrides(context.lastRun ,
                                        context.applyCalcgrpIds,
                                        context.applyCalcgrpIdsInclusive);
            }
            if (context.processesNoShowEmps) {
                processNoShowEmps(context.lastRun, context.taskDateTime,
                                  context.applyCalcgrpIds,
                                  context.applyCalcgrpIdsInclusive);
            }
            if (context.processesEmpDateRecs) {
               calcEmpDaterecs = processCalcEmployeeDateTable();
            }
            if (context.processesJobRateUpdates) {
                processJobRateChanges(context.lastRun, context.taskDateTime,
                                      context.applyCalcgrpIds,
                                      context.applyCalcgrpIdsInclusive);
            }
            if (context.processesCgDefChanges) {
                processCalcgrpDefChanges(context.lastRun ,
                                         context.applyCalcgrpIds,
                                         context.applyCalcgrpIdsInclusive);
            }
            if (context.processesEntEmpPolChanges) {
                processEntEmpPolChanges(context.lastRun ,
                                        context.applyCalcgrpIds,
                                        context.applyCalcgrpIdsInclusive);
            }

        }

        public RecalculateTaskFilterContext getContext() {
            return this.context;
        }

        public EmployeeIdAndDateList getEmployeeIdAndDateListToRecalc() {
            return empDatesRecalcFilter;
        }

        public List getCalcEmployeeDateTableRecords() {
            return calcEmpDaterecs;
        }

        private void processBusinessRecalcs(RecalculateTaskFilterContext context) throws SQLException{

            EmployeeIdAndDateList empIdDatesChangedBusinessRulePeriod = new EmployeeIdAndDateList();
            EmployeeIdAndDateList empIdDatesChangedBusinessRuleInfinite = new EmployeeIdAndDateList();
            if (context.brOvrtypIds != null && context.brOvrtypIds.length > 0) {
                empIdDatesChangedBusinessRulePeriod =
                    getEmpIdDatesChngFutBusRuleDaysPeriod(context);
                empIdDatesChangedBusinessRuleInfinite =
                    getEmpIdDatesChngFutBusRuleDaysInfinite(context);

                if (empIdDatesChangedBusinessRulePeriod.size() == 0) {
                    appendToTaskLogMessage("No business rule changes since last transaction");
                }
                else {
                    appendToTaskLogMessage("Found " +  empIdDatesChangedBusinessRulePeriod.size() +
                                           " business rule changes");
                    if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug("Business rule change empDates : \n " + empIdDatesChangedBusinessRulePeriod);
                }
                if (empIdDatesChangedBusinessRuleInfinite.size() == 0) {
                    appendToTaskLogMessage("No employee,empdeflab changes since last transaction");
                }
                else {
                    appendToTaskLogMessage("Found " +  empIdDatesChangedBusinessRuleInfinite.size() +
                                           " employee,empdeflab changes");
                    if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug("employee,empdeflab  change empDates : \n " + empIdDatesChangedBusinessRuleInfinite);
                }

            }
            EmployeeIdAndDateList empIdDatesChangedClock = new EmployeeIdAndDateList();
            if (context.processesClocksProcessed) {
                empIdDatesChangedClock = getEmpIdDatesChangedClock(context.lastRun,
                    context.applyCalcgrpIds, context.applyCalcgrpIdsInclusive);
                if (empIdDatesChangedClock.size() == 0) {
                    appendToTaskLogMessage("No clocks processed since last transaction");
                }
                else {
                    appendToTaskLogMessage("Found " +
                                           empIdDatesChangedClock.size() +
                                           " clocks processed");
                    if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug("Clocks processed empDates : \n " +   empIdDatesChangedClock);
                }
            }

            findRecalcBusinessRulesDays(empIdDatesChangedBusinessRulePeriod ,
                                        context, false);
            findRecalcBusinessRulesDays(empIdDatesChangedBusinessRuleInfinite ,
                                        context, true);
            findRecalcBusinessRulesDays(empIdDatesChangedClock ,
                                        context, false);

        }

        private void processPendingOverrides(Date lastRun ,
                                             int[] calcgrpIds,
                                             boolean calcgrpIdsInclusive) throws SQLException{
            int[] pOvrtypids = new int[] {
                OverrideData.WORK_SUMMARY_TYPE_START,
                OverrideData.WORK_DETAIL_TYPE_START,
                OverrideData.POSTCALC_WORKDETAIL_TYPE_START,
                OverrideData.PRECALC_WORK_PREMIUM_TYPE_START,
                OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START,
                OverrideData.SCHEDULE_TYPE_START,
                OverrideData.LTA_TYPE_START,
                OverrideData.TIMESHEET_TYPE_START,
                OverrideData.HOLIDAY_TYPE_START
            };
            EmployeeIdAndDateList empIdDatesPending =
                getEmpIdDatesChanged(lastRun, pOvrtypids , OverrideData.PENDING,
                                     calcgrpIds, calcgrpIdsInclusive, null);
            if (empIdDatesPending.size() == 0) {
                appendToTaskLogMessage("No pending overrides transaction");
            }
            else {
                appendToTaskLogMessage("Found " +  empIdDatesPending.size() +   " pending override days");
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug("Pending empDates : \n " +  empIdDatesPending);
                addToEmpDatesToRecalc(empIdDatesPending);
            }
        }

        /**
          * Finds business rules days based on paygroup start/end dates on the date of changes
          * @param empIdDatesChanged
          * @return
          * @throws SQLException
          */
        private void findRecalcBusinessRulesDays(EmployeeIdAndDateList empIdDatesChanged,
                                                 RecalculateTaskFilterContext context,
                                                 boolean doesInfinity) throws SQLException{

             if (empIdDatesChanged.size() == 0) {
                 return;
             }
             CodeMapper cm = CodeMapper.createCodeMapper(conn);

             int[] empIds = getEmpIdsFromEmpDateList(empIdDatesChanged);

             Map pgIds = null;
             boolean isPP = PARAM_BUSINESS_RULE_RECALC_TYPE_PAYPERIOD.equals(context.sRangeType);
             boolean isWeek = PARAM_BUSINESS_RULE_RECALC_TYPE_WEEK.equals(context.sRangeType);
             if (isPP) {
                 pgIds = getEmpPaygroupIds(empIds);
             }
             Map infinityDates = null;
             if (doesInfinity) {
                 infinityDates = getEmpInfinityDates(pgIds , empIdDatesChanged, context);
             }
             // *** cache for date determination
             Map stEndDates = new HashMap();

             Iterator iter = empIdDatesChanged.iterator();
             while (iter.hasNext()) {
                 EmployeeIdAndDate item = (EmployeeIdAndDate)iter.next();

                 Date start = null;
                 Date end = null;
                 if (!stEndDates.containsKey(item.getDate())) {
                     if (isPP) {
                         Integer pgId = (Integer)pgIds.get(new Integer(item.getEmpId()));
                         PayGroupData pgd = cm.getPayGroupById(pgId.intValue());
                         // *** find the paygroup start/end date relative to this date
                         start = DateHelper.getUnitPayPeriod(DateHelper.APPLY_ON_FIRST_DAY,
                             false, item.getDate(), pgd);
                         end = DateHelper.getUnitPayPeriod(DateHelper.APPLY_ON_LAST_DAY,
                             false, item.getDate(), pgd);
                     }
                     else if (isWeek) {
                         start = DateHelper.getUnitWeek(DateHelper.APPLY_ON_FIRST_DAY ,
                             false, item.getDate() );
                         end = DateHelper.getUnitWeek(DateHelper.APPLY_ON_LAST_DAY ,
                             false, item.getDate() );
                     }
                     if (start != null && end != null) {
                         StartEndDate st = new StartEndDate(start, end);
                         stEndDates.put(item.getDate(), st);
                     }
                 } else {
                     StartEndDate st = (StartEndDate)stEndDates.get(item.getDate());
                     start = st.startDate;
                     end = st.endDate;
                 }
                 // *** if infinity, find all infinity dates for emp and add them from start
                 if (!doesInfinity) {
                     addToEmpDatesToRecalc(item.getEmpId(), start, end);
                 }
                 else {
                     List infDates = (List)infinityDates.get(String.valueOf(item.getEmpId()));
                     if (infDates == null || infDates.size() == 0) {
                         continue;
                     }
                     Collections.sort(infDates);
                     addToEmpDatesToRecalc(item.getEmpId(), start, (Date)infDates.get(0));
                     addToEmpDatesToRecalc(item.getEmpId(), infDates);
                 }
             }

        }

        class StartEndDate {
             Date startDate;
             Date endDate;

             public StartEndDate(Date s, Date e) {
                 startDate = DateHelper.truncateToDays(s);
                 endDate = DateHelper.truncateToDays(e);
             }

             public String toString() {
                 return "StartDate: " + startDate + " - EndDate: " + endDate;
             }
        }

         /**
          * @param empIds
          * @return
          * @throws SQLException
          */
        private Map getEmpInfinityDates(Map employeePaygroups,
                                        EmployeeIdAndDateList empDates,
                                        RecalculateTaskFilterContext context) throws SQLException {
            Map ret = new HashMap();
            if (empDates == null || empDates.size() == 0) {
                return ret;
            }
            EmployeeIdAndDateList empMinDates = getMinDatesFromEmpDates(empDates);
            EmployeeIdAndDateList empInfDates = getWorkSummaryInfinityDates(
                employeePaygroups,
                empMinDates ,
                context);
            ret = empInfDates.getMapOfEmployeeDates();
            return ret;
        }

        /**
         * Gets all empId. dates for given employee dates in chunks of 50s for performance
         * if  (context.isRangeInfinite). it will find all summaries since the earliest summary.
         * Otherwise, it will stop at an end date based on week or payperiod definition
         *
         * @param employeePaygroups  required if not  (context.isRangeInfinite) and range is payPeriod
         * @param empDates
         * @param context
         * @return
         * @throws SQLException
         */
        private EmployeeIdAndDateList getWorkSummaryInfinityDates(
            Map employeePaygroups,
            EmployeeIdAndDateList empDates,
            RecalculateTaskFilterContext context) throws SQLException {

            EmployeeIdAndDateList ret = new EmployeeIdAndDateList();
            if (empDates == null || empDates.size() == 0) {
                return ret;
            }

            int start = 0;
            int length = com.workbrain.tool.jdbc.proxy.ProxyPreparedStatement.MAX_PARAMETER / 4;
            while (start < empDates.size()) {
                ret.addAll(getWorkSummaryInfinityDates(employeePaygroups,
                    empDates, start, length,
                    context));
                start += length;
            }
            return ret;
        }

         private EmployeeIdAndDateList getWorkSummaryInfinityDates(
             Map employeePaygroups,
             EmployeeIdAndDateList empDates, int start, int length,
             RecalculateTaskFilterContext context) throws SQLException {

             EmployeeIdAndDateList ret = new EmployeeIdAndDateList();
             if (empDates == null || empDates.size() == 0) {
                 return ret;
             }

             StringBuffer sb = new StringBuffer(200);
             sb.append("SELECT emp_id , wrks_work_date FROM work_summary WHERE ");

             Iterator iter = empDates.iterator();
             for (int i=start; i < (start+length) && i<empDates.size(); i++) {
                 if (context.isRangeInfinite) {
                     sb.append(" (emp_id = ? AND wrks_work_date >= ?)");
                 }
                 else {
                     sb.append(" (emp_id = ? AND wrks_work_date BETWEEN ? AND ?)");
                 }
                 int t = i + 1;
                 if (t < (start+length) && t<empDates.size()) {
                     sb.append(" OR ");
                 }
             }

             CodeMapper cm = CodeMapper.createCodeMapper(conn);
             PreparedStatement ps = null;
             ResultSet rs = null;
             try {
                 ps = conn.prepareStatement(sb.toString());
                 int ind = 1;

                 for (int i=start; i < (start+length) && i<  empDates.size(); i++) {
                     EmployeeIdAndDate se = (EmployeeIdAndDate) empDates.get(i);
                     ps.setInt(ind++, se.getEmpId());
                     if (context.isRangeInfinite) {
                         ps.setTimestamp(ind++,
                                         new Timestamp(se.getDate().getTime()));
                     }
                     else {
                         ps.setTimestamp(ind++,
                                         new Timestamp(se.getDate().getTime()));
                         Date rangeEnd = se.getDate();
                         // *** find this week's end date
                         if (PARAM_BUSINESS_RULE_RECALC_TYPE_WEEK.equals(context.sRangeType)) {
                             rangeEnd = DateHelper.getUnitWeek(
                                DateHelper.APPLY_ON_LAST_DAY,
                                false, context.taskDateTime) ;
                         }
                         else if (PARAM_BUSINESS_RULE_RECALC_TYPE_PAYPERIOD.equals(context.sRangeType)) {
                             Integer pgId = (Integer)employeePaygroups.get(new Integer(se.getEmpId()));
                             PayGroupData pgd = cm.getPayGroupById(pgId.intValue());
                             // *** find current paygroup end date
                             rangeEnd = DateHelper.getUnitPayPeriod(DateHelper.APPLY_ON_LAST_DAY,
                                 false, context.taskDateTime, pgd);
                         }
                         ps.setTimestamp(ind++,
                                         new Timestamp(rangeEnd.getTime()));
                     }
                 }
                 rs = ps.executeQuery();
                 while (rs.next()) {
                     ret.add(rs.getInt(1), rs.getDate(2));
                 }
             }
             finally {
                 if (rs != null)
                     rs.close();
                 if (ps != null)
                     ps.close();
             }
             return ret;
         }

         /**
          * Returns maximum dates for each employee in the list.
          *
          * @return EmployeeIdAndDateList
          */
         public EmployeeIdAndDateList getMinDatesFromEmpDates(EmployeeIdAndDateList empDates) {
             Map map = empDates.getMapOfEmployeeDates();
             EmployeeIdAndDateList ret = new EmployeeIdAndDateList(false);
             Iterator iter = map.entrySet().iterator();
             while (iter.hasNext()) {
                 Map.Entry entry = (Map.Entry) iter.next();
                 int empId = Integer.parseInt((String)entry.getKey());
                 List dates = (List)entry.getValue();
                 if (dates == null || dates.size() == 0) {
                     continue;
                 }
                 Collections.sort(dates);
                 ret.add(empId , (Date)dates.get(0));
             }
             return ret;
         }

         /**
          * Finds employee paygroups and returns them in a hashmap keyed by empId<Integer>
          * @param empIds
          * @return
          * @throws SQLException
          */
         private Map getEmpPaygroupIds(int[] empIds) throws SQLException {
             Map ret = new HashMap();

             IntArrayIterator iter = new IntArrayIterator(empIds , MAX_BIND_SIZE / 2);
             while (iter.hasNext()) {
                 int[] thisEmpIds = iter.next();
                 PreparedStatement ps = null;
                 ResultSet rs = null;
                 try {
                     StringBuffer sb = new StringBuffer(200);
                     sb.append("SELECT emp_id , paygrp_id FROM employee WHERE emp_id IN (");
                     for (int i = 0; i < thisEmpIds.length; i++) {
                         sb.append( i > 0 ? ",?" : "?");
                     }
                     sb.append(")");
                     ps = conn.prepareStatement(sb.toString());
                     for (int i = 0; i < thisEmpIds.length; i++) {
                     ps.setInt(i+1, thisEmpIds[i]);
                     }

                     rs = ps.executeQuery();
                     while (rs.next()) {
                         ret.put(new Integer(rs.getInt(1)) , new Integer(rs.getInt(2)));
                     }
                 }
                 finally {
                     if (rs != null)
                         rs.close();
                     if (ps != null)
                         ps.close();
                 }
             }
             return ret;
         }

        /**
         * Finds emp dates for business rules overrides except EMP and EMP DEF LAB OVERRIDES since they are calculated up to infinity.
         * Do it ovrTyp by ovrType since OR is causing a fullscan
         * Make sure IDX_OVRTYPST_DATES in PSVSS/wbiag is created in the instance for avoiding poor performance
         * @param lastRun
         * @return
         * @throws SQLException
         */
        private EmployeeIdAndDateList getEmpIdDatesChngFutBusRuleDaysPeriod(
            RecalculateTaskFilterContext context) throws SQLException {
            IntegerList temp = new IntegerList();
            for (int i = 0; i < context.brOvrtypIds.length; i++) {
                if (context.brOvrtypIds[i] != OverrideData.EMPLOYEE_TYPE_START
                    && context.brOvrtypIds[i] != OverrideData.EMP_DLA_TYPE_START) {
                    temp.add(context.brOvrtypIds[i] );
                }
            }

            Map toks = new HashMap();
            OvrtypToken tok =
                new OvrtypToken(new String[] {WorkSummaryData.WRKS_AUTHORIZED}, false);
            toks.put(new Integer(OverrideData.WORK_SUMMARY_TYPE_START) , tok);
            EmployeeIdAndDateList edFinal = new EmployeeIdAndDateList ();
            // *** get APPLIED and CANCELLED seperate to avoid OR inefficiency
            edFinal.addAll(getEmpIdDatesChanged(context.lastRun, temp.toIntArray() ,
                                        OverrideData.APPLIED, context.applyCalcgrpIds,
                                        context.applyCalcgrpIdsInclusive,
                                        toks));
            edFinal.addAll(getEmpIdDatesChanged(context.lastRun, temp.toIntArray() ,
                                OverrideData.CANCELLED, context.applyCalcgrpIds,
                                context.applyCalcgrpIdsInclusive,
                                toks));
            return edFinal;
        }

        /**
         * Finds emp dates for business rules overrides for EMP and EMP DEF LAB OVERRIDES
         * Do it ovrTyp by ovrType since OR is causing a fullscan
         * Make sure IDX_OVRTYPST_DATES in PSVSS/wbiag is created in the instance for avoiding poor performance
         * @param lastRun
         * @return
         * @throws SQLException
         */
        private EmployeeIdAndDateList getEmpIdDatesChngFutBusRuleDaysInfinite(
            RecalculateTaskFilterContext context) throws SQLException {

            IntegerList temp = new IntegerList();
            for (int i = 0; i < context.brOvrtypIds.length; i++) {
                if (context.brOvrtypIds[i] == OverrideData.EMPLOYEE_TYPE_START
                    || context.brOvrtypIds[i] == OverrideData.EMP_DLA_TYPE_START) {
                    temp.add(context.brOvrtypIds[i] );
                }
            }
            // *** exclude wrks_authorized and include on empColsToInvokeCalc, if not ALL
            Map toks = new HashMap();
            OvrtypToken tok =
                new OvrtypToken(new String[] {WorkSummaryData.WRKS_AUTHORIZED}, false);
            toks.put(new Integer(OverrideData.WORK_SUMMARY_TYPE_START) , tok);
            if (!REGVAL_EMPCOLS_TO_INV_CALC_ALL.equalsIgnoreCase(context.empColsToInvokeCalc)
                && !StringHelper.isEmpty(context.empColsToInvokeCalc) ) {
                OvrtypToken tok2 =
                    new OvrtypToken(
                    StringHelper.detokenizeString(context.empColsToInvokeCalc,
                                                  ","), true);
                toks.put(new Integer(OverrideData.EMPLOYEE_TYPE_START), tok2);
                if (logger.isDebugEnabled()) logger.debug("Only including emp overrides with columns :" + context.empColsToInvokeCalc);
            }
            EmployeeIdAndDateList edFinal = new EmployeeIdAndDateList ();
            // *** get APPLIED and CANCELLED seperate to avoid OR inefficiency
            edFinal.addAll(getEmpIdDatesChanged(context.lastRun, temp.toIntArray()  ,
                                        OverrideData.APPLIED, context.applyCalcgrpIds,
                                        context.applyCalcgrpIdsInclusive,
                                        toks));
            edFinal.addAll(getEmpIdDatesChanged(context.lastRun, temp.toIntArray()  ,
                                OverrideData.CANCELLED, context.applyCalcgrpIds,
                                context.applyCalcgrpIdsInclusive,
                                toks));

            return edFinal;
        }

        /**
         *
         * @param lastRun
         * @param baseOvrtypIds
         * @param checksForPendingOvrs
         * @param ovrTokens   Map of OvrtypToken objects keyed by ovrtypId to determine what override type tokens are eligible. Empty means all types are eligible
         * @return
         * @throws SQLException
         */
        private EmployeeIdAndDateList getEmpIdDatesChanged(Date lastRun ,
            int[] baseOvrtypIds , String ovrStatus,
            int[] calcgrpIds,
            boolean calcgrpIdsInclusive,
            Map ovrTokens) throws SQLException {

            EmployeeIdAndDateList ret = new EmployeeIdAndDateList(false);
            for (int i = 0; i < baseOvrtypIds.length; i++) {
                EmployeeIdAndDateList temp = getEmpIdDatesChanged(lastRun,
                    baseOvrtypIds[i], ovrStatus, calcgrpIds,
                    calcgrpIdsInclusive , ovrTokens);
                Iterator iter = temp.iterator();
                while (iter.hasNext()) {
                    EmployeeIdAndDate ed = (EmployeeIdAndDate)iter.next();
                    ret.add(ed);
                }
            }
            return ret;

        }

        /**
         *
         * @param lastRun
         * @param baseOvrtypId
         * @param ovrStatus
         * @param calcgrpIds
         * @param calcgrpIdsInclusive
         * @param ovrTokens   Map of OvrtypToken objects keyed by ovrtypId to determine what override type tokens are eligible. Empty means all types are eligible
         * @return
         * @throws SQLException
         */
        private EmployeeIdAndDateList getEmpIdDatesChanged(Date lastRun ,
            int baseOvrtypId, String ovrStatus,
            int[] calcgrpIds,
            boolean calcgrpIdsInclusive,
            Map ovrTokens) throws SQLException {

            EmployeeIdAndDateList ret = new EmployeeIdAndDateList(false);
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                StringBuffer sb = new StringBuffer(200);
                sb.append("SELECT override.emp_id, ovr_start_date, ovr_new_value ");
                sb.append(" FROM override ");

                if (calcgrpIds != null && calcgrpIds.length > 0) {
                    sb.append(" , employee ");
                    sb.append(" WHERE employee.emp_id = override.emp_id ");
                    sb.append(" AND calcgrp_id ");
                    sb.append(calcgrpIdsInclusive ? " IN " : " NOT IN ");
                    sb.append(" (");
                    for (int i = 0; i < calcgrpIds.length; i++) {
                        sb.append(i > 0 ? ",?" : "?");
                    }
                    sb.append(") AND");
                }
                else {
                    sb.append(" WHERE");
                }

                sb.append(" (ovrtyp_id BETWEEN ? and ? ) ");
                sb.append(" AND ");
                if (OverrideData.CANCELLED.equalsIgnoreCase(ovrStatus)) {
                     sb.append(" (ovr_cancelled_date > ? AND ovr_status=?)");
                }
                else {
                    sb.append(" (ovr_create_date > ? AND ovr_status=?)");
                }


                int ind = 1;
                ps = conn.prepareStatement(sb.toString());

                if (calcgrpIds != null && calcgrpIds.length > 0) {
                    for (int i = 0; i < calcgrpIds.length; i++) {
                        ps.setInt(ind++, calcgrpIds[i]);
                    }
                }

                ps.setInt(ind++ , baseOvrtypId);
                ps.setInt(ind++ , baseOvrtypId + 99);
                ps.setTimestamp(ind++ , new Timestamp(lastRun.getTime()));
                ps.setString(ind++  , ovrStatus);

                rs = ps.executeQuery();
                while (rs.next()) {
                    // *** check ignore tokens
                    String ovrNewValue = rs.getString(3);
                    if (!isOvrTokenEligible(ovrTokens, ovrNewValue, baseOvrtypId)) {
                        continue;
                    }
                    EmployeeIdAndDate ed = new EmployeeIdAndDate(rs.getInt(1) , rs.getDate(2));
                    ret.add(ed);
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }
            return ret;
        }

        private boolean isOvrTokenEligible(Map ovrTokens,
                                           String ovrNewValue, int ovrtypId) {
            if (ovrTokens == null || ovrTokens.size() == 0) {
                return true;
            }

            Integer key = new Integer(ovrtypId);
            if (!ovrTokens.containsKey(key)) {
                return true;
            }
            else {
                boolean fnd = false;
                OvrtypToken tok = (OvrtypToken) ovrTokens.get(key);
                for (int i = 0; i < tok.tokens.length; i++) {
                    if (ovrNewValue.indexOf(tok.tokens[i]) > -1) {
                        fnd = true;
                        break;
                    }
                }
                return tok.inclusive ? fnd : !fnd;
            }

        }
        private class OvrtypToken {
            String[] tokens;
            boolean inclusive;

            public OvrtypToken(String[] tokens, boolean inclusive) {
                this.tokens = tokens;
                this.inclusive = inclusive;
            }
        }

        private final String CLOCKING_EMP_SQL =
            "SELECT clock_tran_processed.emp_id, wrks_work_date " +
            " FROM clock_tran_processed, work_summary " +
            " WHERE work_summary.wrks_id = clock_tran_processed.wrks_id " +
            " AND clktranpro_proc_dt > ?";

        private EmployeeIdAndDateList getEmpIdDatesChangedClock(Date compareDate,
            int[] calcgrpIds,    boolean calcgrpIdsInclusive) throws SQLException {

            EmployeeIdAndDateList ret = new EmployeeIdAndDateList();
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                StringBuffer sb = new StringBuffer(CLOCKING_EMP_SQL);
                if (calcgrpIds != null && calcgrpIds.length > 0) {
                    sb.append(" AND calcgrp_id ");
                    sb.append(calcgrpIdsInclusive ? " IN " : " NOT IN ");
                    sb.append(" (");
                    for (int i = 0; i < calcgrpIds.length; i++) {
                        sb.append(i > 0 ? ",?" : "?");
                    }
                    sb.append(")");
                }

                ps = conn.prepareStatement(sb.toString());
                int ind = 1;
                ps.setTimestamp(ind++ , new Timestamp(compareDate.getTime()));
                if (calcgrpIds != null && calcgrpIds.length > 0) {
                    for (int i = 0; i < calcgrpIds.length; i++) {
                        ps.setInt(ind++ , calcgrpIds[i]);
                    }
                }

                rs = ps.executeQuery();
                while (rs.next()) {
                    int empId = rs.getInt(1);
                    ret.add(empId , rs.getDate(2));
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }

            return ret;
        }

        private List processCalcEmployeeDateTable() throws SQLException{

            List list = CalcEmployeeDateAccess.loadCalcEmployeeDate(conn);
            if (list.size() == 0) {
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug ("Found NO emp/Dates in Calc_Employee_Date table");
            }
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug ("Found emp/Dates in Calc_Employee_Date table : \n" +list);
            Iterator iter = list.iterator();
            while (iter.hasNext()) {
                CalcEmployeeDateData item = (CalcEmployeeDateData)iter.next();
                EmployeeIdAndDate ed = new EmployeeIdAndDate(item.getEmpId() , item.getCedWorkDate());
                addToEmpDatesToRecalc(ed);
            }
            return list;
        }

        private static final String NO_SHOW_SQL =
            "SELECT work_summary.emp_id, wrks_work_date " +
            "FROM work_summary , employee_schedule " +
            "WHERE employee_schedule.work_date = work_summary.wrks_work_date " +
            "AND employee_schedule.emp_id = work_summary.emp_id " +
            "AND wrks_work_date BETWEEN ? and ? " +
            "AND employee_schedule.empskd_act_start_time <> employee_schedule.empskd_act_end_time " +
            "AND wrks_clocks is null ";

        private void processNoShowEmps (Date lastRun, Date taskDate,
                                        int[] calcgrpIds,
                                        boolean calcgrpIdsInclusive) throws Exception {
            PreparedStatement ps = null;
            ResultSet rs = null;
            int cnt = 0;
            try {
                StringBuffer sb = new StringBuffer(NO_SHOW_SQL);
                if (calcgrpIds != null && calcgrpIds.length > 0) {
                    sb.append(" AND calcgrp_id ");
                    sb.append(calcgrpIdsInclusive ? " IN " : " NOT IN ");
                    sb.append(" (");
                    for (int i = 0; i < calcgrpIds.length; i++) {
                        sb.append(i > 0 ? ",?" : "?");
                    }
                    sb.append(")");
                }

                ps = conn.prepareStatement(sb.toString());
                int ind = 1;
                ps.setTimestamp(ind++ , new Timestamp(DateHelper.truncateToDays(lastRun).getTime()));
                ps.setTimestamp(ind++ , new Timestamp(DateHelper.truncateToDays(taskDate).getTime()));
                if (calcgrpIds != null && calcgrpIds.length > 0) {
                    for (int i = 0; i < calcgrpIds.length; i++) {
                        ps.setInt(ind++, calcgrpIds[i]);
                    }
                }
                rs = ps.executeQuery();
                while (rs.next()) {
                    EmployeeIdAndDate ed = new EmployeeIdAndDate(rs.getInt(1), rs.getDate(2));
                    addToEmpDatesToRecalc(ed);
                    cnt++;
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }
            appendToTaskLogMessage( cnt + " no-show employee change(s)");
        }

        private void processJobRateChanges (Date lastRun, Date taskDate,
                                            int[] calcgrpIds,
                                            boolean calcgrpIdsInclusive) throws Exception {
            List jobDates = getJobDateChanges(lastRun);
            appendToTaskLogMessage(jobDates.size() + " job rate date change(s)");
            if (jobDates.size() == 0) {
                return;
            }
            Iterator iter = jobDates.iterator();
            while (iter.hasNext()) {
                IdDate item = (IdDate)iter.next();
                addWorkSummaryJobDates(item.id , item.date, calcgrpIds, calcgrpIdsInclusive);
            }
        }

        private List getJobDateChanges(Date lastRun) throws Exception {
            List ret = new ArrayList();
            StringBuffer sb = new StringBuffer(200);
            sb.append("  SELECT chnghist_record_id , chnghist_rec_name ");
            sb.append("     FROM change_history ");
            sb.append("     WHERE chnghist_table_name = ? ");
            sb.append("     AND chnghist_change_date >= ? ");
            sb.append("     AND change_history.chnghist_change_type = ? ");
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = conn.prepareStatement(sb.toString());
                ps.setString(1, JobRateAccess.JOB_RATE_TABLE.toUpperCase());
                ps.setTimestamp(2, new Timestamp(lastRun.getTime()));
                ps.setString(3, "U");
                rs = ps.executeQuery();
                while (rs.next()) {
                    int jobrateId = rs.getInt(1);
                    String sName = rs.getString(2);
                    Date effDate = null;
                    int jobId = Integer.MIN_VALUE ;
                    if (!StringHelper.isEmpty(sName)) {
                        int com = sName.indexOf(",");
                        if (com > 0) {
                            try {
                                jobId = Integer.parseInt(sName.substring(0, com));
                                String sDate = sName.substring(com + 1);
                                effDate = DateHelper.parseDate(sDate, "MM/dd/yyyy");
                            }
                            catch (NumberFormatException ex) {
                                logger.error("Error in parsing change history value :" + sName);
                            }
                        }
                    }
                    if (jobId != Integer.MIN_VALUE && sName != null) {
                        ret.add(new IdDate (jobId , effDate));
                    }
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }
            return ret;
        }

        private void addWorkSummaryJobDates(int jobId, Date date,
                                            int[] calcgrpIds,
                                            boolean calcgrpIdsInclusive) throws Exception {
            PreparedStatement ps = null;
            ResultSet rs = null;
            StringBuffer sb = new StringBuffer(200);
            sb.append(" SELECT work_summary.emp_id, wrks_work_date");
            sb.append(" FROM work_summary, work_detail");
            sb.append(" WHERE work_summary.wrks_id =  work_detail.wrks_id");
            sb.append(" AND wrks_work_date > ? and job_id = ?");
            if (calcgrpIds != null && calcgrpIds.length > 0) {
                sb.append(" AND calcgrp_id ");
                sb.append(calcgrpIdsInclusive ? " IN " : " NOT IN ");
                sb.append(" (");
                for (int i = 0; i < calcgrpIds.length; i++) {
                    sb.append(i > 0 ? ",?" : "?");
                }
                sb.append(")");
            }

            try {
                ps = conn.prepareStatement(sb.toString() );
                int ind = 1;
                ps.setTimestamp(ind++ , new Timestamp(date.getTime()));
                ps.setInt(ind++ , jobId);
                if (calcgrpIds != null && calcgrpIds.length > 0) {
                    for (int i = 0; i < calcgrpIds.length; i++) {
                        ps.setInt(ind++ , calcgrpIds[i]);
                    }
                }

                rs = ps.executeQuery();
                int cnt = 0;
                while (rs.next()) {
                    EmployeeIdAndDate ed = new EmployeeIdAndDate(rs.getInt(1), rs.getDate(2));
                    addToEmpDatesToRecalc(ed);
                    cnt++;
                }
                if (logger.isDebugEnabled()) logger.debug("Found " + cnt + " employee date(s) for jobid:" + jobId);
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }

        }

        private void processCalcgrpDefChanges (Date lastRun ,
                                               int[] calcgrpIds,
                                               boolean calcgrpIdsInclusive) throws Exception {

            Map cgChanges = getCalcgrpDefChanges(lastRun , calcgrpIds, calcgrpIdsInclusive);

            EmployeeIdAndDateList empDates = getCalcgprDefChngEmpDates(cgChanges);
            addToEmpDatesToRecalc(empDates);
            appendToTaskLogMessage( empDates.size() + " employee recalc(s) due to calcgroup def changes");
        }

        private static final String CALCGRP_DEF_CHANGE_SQL =
            "SELECT calcgrp_id, cga_eff_start_date, cga_eff_end_date " +
            "FROM calc_group_audit " +
            "WHERE cga_create_date > ? ";

        /**
         * Find the changed calcgrp definitions
         * @param lastRun
         * @param calcgrpIds
         * @return
         * @throws Exception
         */
        private Map getCalcgrpDefChanges (Date lastRun, int[] calcgrpIds,
                                          boolean calcgrpIdsInclusive) throws Exception {
            Map ret = new HashMap();
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                StringBuffer sb = new StringBuffer(CALCGRP_DEF_CHANGE_SQL);
                if (calcgrpIds != null && calcgrpIds.length > 0) {
                    sb.append(" AND calcgrp_id ");
                    sb.append(calcgrpIdsInclusive ? " IN " : " NOT IN ");
                    sb.append(" (");
                    for (int i = 0; i < calcgrpIds.length; i++) {
                        sb.append(i > 0 ? ",?" : "?");
                    }
                    sb.append(")");
                }

                ps = conn.prepareStatement(sb.toString());
                int ind = 1;
                ps.setTimestamp(ind++ , new Timestamp(DateHelper.truncateToDays(lastRun).getTime()));
                if (calcgrpIds != null && calcgrpIds.length > 0) {
                    for (int i = 0; i < calcgrpIds.length; i++) {
                        ps.setInt(ind++ , calcgrpIds[i]);
                    }
                }

                rs = ps.executeQuery();
                while (rs.next()) {
                    Integer cgId = new Integer(rs.getInt(1));
                    StartEndDate se = new StartEndDate(rs.getDate(2) , rs.getDate(3));
                    // *** update existing if earlier and/or later
                    if (ret.containsKey(cgId)) {
                        StartEndDate seEx = (StartEndDate)ret.get(cgId);
                        se.startDate = DateHelper.truncateToDays(DateHelper.min(se.startDate , seEx.startDate));
                        se.endDate = DateHelper.truncateToDays(DateHelper.max(se.endDate , seEx.endDate));
                    }
                    ret.put(cgId , se);
                    if (logger.isDebugEnabled()) logger.debug("Found calcgrp def change. CalcgrpId :" + cgId + " " + se);
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }
            return ret;
        }

        /**
         * Do onebyone for each calcgroups as query is expensive with OR
         * @param eeChanges
         * @return
         * @throws SQLException
         */
        private EmployeeIdAndDateList getCalcgprDefChngEmpDates(Map eeChanges) throws SQLException{
            EmployeeIdAndDateList ret = new EmployeeIdAndDateList();

            int cnt = 0;
            Iterator iter2 = eeChanges.entrySet().iterator();
            while (iter2.hasNext()) {
                Map.Entry entry = (Map.Entry) iter2.next();
                int cgId = ( (Integer) entry.getKey()).intValue();
                StartEndDate se = (StartEndDate) entry.getValue();
                ret.addAll(getCalcgprDefChngEmpDates(cgId, se.startDate, se.endDate));
            }

            return ret;
        }

        private static final String CALCGRP_DATE_SQL =
            "SELECT emp_id , wrks_work_date FROM work_summary WHERE  " +
            " calcgrp_id = ? AND wrks_work_date BETWEEN ? and ?";

        private EmployeeIdAndDateList getCalcgprDefChngEmpDates(int cgId,
            Date start, Date end) throws SQLException{
            EmployeeIdAndDateList ret = new EmployeeIdAndDateList();
            if (start == null || end == null) {
                return ret;
            }

            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = conn.prepareStatement(CALCGRP_DATE_SQL);
                ps.setInt(1 , cgId);
                ps.setTimestamp(2 , new Timestamp(start.getTime()));
                ps.setTimestamp(3 , new Timestamp(end.getTime()));
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.add(rs.getInt(1) , rs.getDate(2));
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }
            return ret;
        }

        private void processEntEmpPolChanges (Date lastRun ,
                                              int[] calcgrpIds,
                                              boolean calcgrpIdsInclusive) throws Exception {
            Map eepDates = getEntEmpPolChanges(lastRun);

            EmployeeIdAndDateList empDates = getEntemppolChngEmpDates(eepDates ,
                calcgrpIds, calcgrpIdsInclusive);
            addToEmpDatesToRecalc(empDates);
            appendToTaskLogMessage( empDates.size() + " employee recalc(s) due to entemp policy date change(s)");
        }

        /**
         * Divide into chunks since empStartEndDates could be too many.
         * @param eeChanges
         * @param calcgrpIds
         * @return
         * @throws SQLException
         */
        private EmployeeIdAndDateList getEntemppolChngEmpDates(Map eeChanges,
            int[] calcgrpIds,  boolean calcgrpIdsInclusive) throws SQLException{
            EmployeeIdAndDateList ret = new EmployeeIdAndDateList();

            final int chunkSize = com.workbrain.tool.jdbc.proxy.ProxyPreparedStatement.MAX_PARAMETER / 3;
            EmployeeIdStartEndDateList seList = new EmployeeIdStartEndDateList();
            int cnt = 0;
            Iterator iter2 = eeChanges.entrySet().iterator();
            while (iter2.hasNext()) {
                Map.Entry entry = (Map.Entry) iter2.next();
                int empId = ( (Integer) entry.getKey()).intValue();
                StartEndDate se = (StartEndDate) entry.getValue();
                seList.add(empId, se.startDate, se.endDate)  ;
                if (cnt % chunkSize == 0) {
                    ret.addAll(getWorkSummaryEmpDates(seList, calcgrpIds, calcgrpIdsInclusive));
                    seList.clear();
                }
            }

            return ret;
        }

        private EmployeeIdAndDateList getWorkSummaryEmpDates(EmployeeIdStartEndDateList eeChanges,
            int[] calcgrpIds, boolean calcgrpIdsInclusive) throws SQLException{
            EmployeeIdAndDateList ret = new EmployeeIdAndDateList();
            if (eeChanges == null || eeChanges.size() == 0) {
                return ret;
            }

            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT emp_id , wrks_work_date FROM work_summary WHERE ");
            if (calcgrpIds != null && calcgrpIds.length > 0) {
                sb.append(" calcgrp_id ");
                sb.append(calcgrpIdsInclusive ? " IN " : " NOT IN ");
                sb.append(" (");
                for (int i = 0; i < calcgrpIds.length; i++) {
                    sb.append(i > 0 ? ",?" : "?");
                }
                sb.append(") AND (");
            }

            Iterator iter = eeChanges.iterator();
            int cnt = 0;
            while (iter.hasNext()) {
                Object item = iter.next();
                if (cnt++ > 0) {
                    sb.append(" OR ");
                }
                sb.append(" (emp_id = ? AND wrks_work_date BETWEEN ? and ?)");
            }
            if (calcgrpIds != null && calcgrpIds.length > 0) {
                sb.append(")");
            }
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = conn.prepareStatement(sb.toString());
                int ind = 1;
                if (calcgrpIds != null && calcgrpIds.length > 0) {
                    for (int i = 0; i < calcgrpIds.length; i++) {
                        ps.setInt(ind++ , calcgrpIds[i]);
                    }
                }

                Iterator iter2 = eeChanges.iterator();
                while (iter2.hasNext()) {
                    EmployeeIdStartEndDate se = (EmployeeIdStartEndDate) iter2.next();
                    ps.setInt(ind++ , se.getEmpId());
                    ps.setTimestamp(ind++ , new Timestamp(se.getStartDate().getTime()));
                    ps.setTimestamp(ind++ , new Timestamp(se.getEndDate().getTime()));
                }
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.add(rs.getInt(1) , rs.getDate(2));
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }
            return ret;
        }

        private Map getEntEmpPolChanges(Date lastRun) throws Exception {
            Map ret = new HashMap();
            StringBuffer sb = new StringBuffer(200);
            sb.append("  SELECT chnghist_record_id , chnghist_rec_name ");
            sb.append("     FROM change_history ");
            sb.append("     WHERE chnghist_table_name = ? ");
            sb.append("     AND chnghist_change_date >= ? ");
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = conn.prepareStatement(sb.toString());
                ps.setString(1, EntEmpPolicyAccess.ENT_EMP_POLICY_TABLE.toUpperCase());
                ps.setTimestamp(2, new Timestamp(lastRun.getTime()));
                rs = ps.executeQuery();
                while (rs.next()) {
                    Integer empId = new Integer(rs.getInt(1));
                    String sName = rs.getString(2);
                    Date startDate = null, endDate = null;
                    if (!StringHelper.isEmpty(sName)) {
                        int com = sName.indexOf(",");
                        if (com > 0) {
                            try {
                                String sDate = sName.substring(0, com);
                                startDate = DateHelper.parseDate(sDate, "MM/dd/yyyy") ;
                                sDate = sName.substring(com + 1);
                                endDate = DateHelper.parseDate(sDate, "MM/dd/yyyy");
                            }
                            catch (NumberFormatException ex) {
                                logger.error("Error in parsing change history value :" + sName);
                            }
                        }
                    }
                    if (startDate != null && endDate != null) {
                        StartEndDate se = new StartEndDate(startDate , endDate);
                        // *** update existing if earlier and/or later
                        if (ret.containsKey(empId)) {
                            StartEndDate seEx = (StartEndDate)ret.get(empId);
                            se.startDate = DateHelper.truncateToDays(DateHelper.min(se.startDate , seEx.startDate));
                            se.endDate = DateHelper.truncateToDays(DateHelper.min(se.endDate , seEx.endDate));
                        }
                        ret.put(empId , se);
                        if (logger.isDebugEnabled()) logger.debug("Found entemppol change. EmpId :" + empId + " " + se);

                    }
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }
            return ret;
        }


        private class IdDate {
            int id;
            Date date;

            public IdDate(int id, Date date) {
                this.id = id;
                this.date = date;
            }
        }

        private void addToEmpDatesToRecalc(int empId, Date start, Date end) {
            if (start != null && end != null) {
                empDatesRecalcFilter.add(empId , start, end);
            }
        }

        private void addToEmpDatesToRecalc(int empId, List dates) {
            if (dates != null && dates.size() > 0) {
                Iterator iter = dates.iterator();
                while (iter.hasNext()) {
                    Date item = (Date)iter.next();
                    empDatesRecalcFilter.add(empId , item);
                }

            }
        }

        private void addToEmpDatesToRecalc(EmployeeIdAndDate empDate) {
            if (empDate != null) {
                empDatesRecalcFilter.add(empDate);
            }
        }

        private void addToEmpDatesToRecalc(EmployeeIdAndDateList empDates) {
            if (empDates != null && empDates.size() > 0) {
                Iterator iter = empDates.iterator();
                while (iter.hasNext()) {
                    EmployeeIdAndDate item = (EmployeeIdAndDate)iter.next();
                    empDatesRecalcFilter.add(item);
                }
            }
        }


    }

}
