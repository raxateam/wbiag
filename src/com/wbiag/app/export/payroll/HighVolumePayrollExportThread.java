package com.wbiag.app.export.payroll;

import org.apache.log4j.*;

import com.workbrain.util.DateHelper;
import com.workbrain.util.Datetime;
import com.workbrain.util.NestedRuntimeException;
import com.workbrain.util.StringHelper;
import com.workbrain.app.scheduler.enterprise.ScheduledJob;
import com.workbrain.security.SecurityService;
import com.workbrain.security.AuthenticatedUser;
import com.workbrain.server.sql.ConnectionManager;
import com.workbrain.sql.*;
import com.workbrain.tool.mail.*;

import java.util.ArrayList;
import java.util.Date;
import java.sql.*;

/**
 * Title: HighVolumePayrollExportThread.java <br>
 * Description: TT49356 - High Volume Payroll Export; Thread class. See
 * Technical documentation off WBIAG website for more informaiton. <br>
 * 
 * Created: May 17, 2005
 * 
 * @author cleigh
 *         <p>
 *         Revision History <br>
 *         May 17, 2005 - File Created according to Technical documentation <br>
 *         <p>
 */
public class HighVolumePayrollExportThread implements Runnable {
    private static Logger logger = Logger
            .getLogger(HighVolumePayrollExportThread.class);

    private static final int THREAD_CNT_MAX = 5;

    public static String SEPARATOR = ",";
    public static String QUEUE_TABLE = "WBIAG_EXPORT_QUEUE";
    public static String QUEUE_SEQ = "SEQ_WIEQ_ID";
    public static String QUEUE_ID = "WIEQ_ID";
    public static String QUEUE_LOCK = "WIEQ_LOCK";
    public static String QUEUE_STATUS = "WIEQ_STATUS";
    public static String QUEUE_SUBMIT_DATE = "WIEQ_SUBMIT_DATE";
    public static String QUEUE_START_TIME = "WIEQ_START_TIME";
    public static String QUEUE_END_TIME = "WIEQ_END_TIME";
    public static String QUEUE_MESSAGE = "WIEQ_ERROR";
    public static String QUEUE_SUBMITTED_BY = "WIEQ_SUBMITTED_BY";
    public static String QUEUE_PAYGRP_IDS = "WIEQ_PAYGRP_ID";
    public static String QUEUE_EMP_IDS = "WIEQ_EMP_IDS";
    public static String QUEUE_PAYSYS_ID = "WIEQ_PAYSYS_ID";
    public static String QUEUE_START_DATE = "WIEQ_START_DATE";
    public static String QUEUE_END_DATE = "WIEQ_END_DATE";
    public static String QUEUE_USE_PAYPERIOD = "WIEQ_USE_PAY_PERIOD";
    public static String QUEUE_ALL_READY_WHERE = "WIEQ_ALL_READY_WHERE";
    public static String QUEUE_ALL_READY_PAYGRPS = "WIEQ_ALL_READY_PAYGRPS";
    public static String QUEUE_CYCLE = "WIEQ_CYCLE";
    public static String QUEUE_ADJUST_DATES = "WIEQ_ADJUST_DATES";
    public static String QUEUE_WRITE_TO_FILE = "WIEQ_WRITE_TO_FILE";
    public static String QUEUE_MERGE_FILES = "WIEQ_MERGE_FILES";
    public static String QUEUE_WRITE_TO_TABLE = "WIEQ_WRITE_TO_TABLE";
    public static String QUEUE_TERM_ADD_DAYS = "WIEQ_TERM_ADD_DAYS";
    public static String QUEUE_LOOK_BACK_DAYS = "WIEQ_LOOK_BACK_DAYS";
    public static String QUEUE_CHK_UNAUTH = "WIEQ_CHK_UNAUTH";
    public static String QUEUE_UDF1 = "WIEQ_UDF1";
    public static String QUEUE_UDF2 = "WIEQ_UDF2";
    public static String QUEUE_UDF3 = "WIEQ_UDF3";
    public static String QUEUE_UDF4 = "WIEQ_UDF4";
    public static String QUEUE_UDF5 = "WIEQ_UDF5";
    public static String QUEUE_FLAG1 = "WIEQ_FLAG1";
    public static String QUEUE_FLAG2 = "WIEQ_FLAG2";
    public static String QUEUE_FLAG3 = "WIEQ_FLAG3";
    public static String QUEUE_FLAG4 = "WIEQ_FLAG4";
    public static String QUEUE_FLAG5 = "WIEQ_FLAG5";
    public static String QUEUE_EXTRA = "WIEQ_EXTRA";

    public static String QUEUE_STATUS_IN_PROGRESS = "In Progress";
    public static String QUEUE_STATUS_READY = "Ready";
    public static String QUEUE_STATUS_COMPLETE = "Complete";
    public static String QUEUE_STATUS_ERROR = "Error";

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss";

    protected static String EXPORT_FINISHED_MSG = "Export has Finished";
    protected static String EXPORT_FAILD_MSG = "Export has Failed";

    protected DBConnection conn;
    private boolean isConnectionOwner = false;
    private String clientId;
    private AuthenticatedUser currentUser;
    private int slaveThreadCount = 0;
    private int threadCount;
    private ScheduledJob intCheck;
    private HighVolumePayrollExportThread parent;
    private Throwable exceptionFromSlaves = null;

    //Queue Variables
    protected int lock;
    protected int queueId = -1;
    protected String status = null;
    protected String error = null;
    protected String submittedBy = "workbrain";
    protected Date submitDate = null;
    protected Date queueStartTime = null;
    protected Date queueEndTime = null;

    //Export Variables
    private int[] payGrpIds;
    private int[] empIds;
    protected int paysysId;
    protected int lookBackDays;
    protected int termAddDays;
    protected boolean usePayPeriod;
    protected boolean allReadyPaygrps;
    protected boolean adjustDates;
    protected boolean writeToFile;
    protected boolean mergeFiles;
    protected boolean writeToTable;
    protected boolean chkUnauth;
    protected String allReadyWhere = null;
    protected Date startDate = null;
    protected Date endDate = null;
    protected String cycle = null;

    //Extra feilds
    protected boolean flag1;
    protected boolean flag2;
    protected boolean flag3;
    protected boolean flag4;
    protected boolean flag5;
    protected String udf1 = null;
    protected String udf2 = null;
    protected String udf3 = null;
    protected String udf4 = null;
    protected String udf5 = null;
    protected String extraField = null;

    /**
     * Thread Constructor.
     */
    HighVolumePayrollExportThread(String cId) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("HVPE Thread constructor : Creating parrent Thread.");
        }
        conn = new DBConnection(ConnectionManager.getConnection());
	isConnectionOwner = true;	
        conn.turnTraceOff();
        conn.setAutoCommit(false);

        currentUser = SecurityService.getCurrentUser();
        clientId = cId;
    }

    private HighVolumePayrollExportThread(HighVolumePayrollExportThread parent,
            String cId, AuthenticatedUser authUser) {
        if (logger.isDebugEnabled()) {
            logger.debug("HVPE Thread constructor : Creating Child Thread.");
        }
        this.parent = parent;
        clientId = cId;
        currentUser = authUser;
    }

    public void run() {
        try {

            initDBConnection();
            SecurityService.setCurrentClientId(clientId);
            SecurityService.setCurrentUser(currentUser);
            slaveThreadCount = 0;
            execute();

        } catch (Throwable t) {
            parent.exceptionFromSlaves = t;
        } finally {
            try {
                if (isConnectionOwner && conn != null) {
                    conn.close();
                }
            } catch (Throwable t) {
                parent.exceptionFromSlaves = t;
            }
            parent.subThreadCount(1);
            synchronized (parent) {
                parent.notify();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void execute() {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting client ID: " + clientId);
            }
            SecurityService.setCurrentClientId(clientId);

            if (slaveThreadCount > 1) {
                if (logger.isDebugEnabled()) {
                    logger.debug("This is a parrent thread.");
                }
                execute(slaveThreadCount);
            } else {

                if (logger.isDebugEnabled()) {
                    logger.debug("This is a child thread.");
                }

                queueId = selectExportFromQueue();

                beforeThreadRun();

                if (queueId != -1) {
                    process();
                }

                afterThreadRun();
            }

        } catch (Throwable e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Exception occured during thread execution.", e);
            }
            writeError(e);
            try {
                if (queueId != -1){
                    markRecords(queueId, QUEUE_STATUS_ERROR);
                }
            } catch (Exception sqlE) {
                if (logger.isEnabledFor(Level.ERROR)) {
                    logger.error(
                            "Could not write ERROR status to queue table.",
                            sqlE);
                }
                if (parent == null) {
                    throw new NestedRuntimeException(sqlE);
                } else {
                    parent.exceptionFromSlaves = sqlE;
                }
            }
            if (parent == null) {
                throw new NestedRuntimeException(e);
            } else {
                parent.exceptionFromSlaves = e;
            }
        }
    }

    public void execute(int slaveThreadCount) {

        try {
            boolean moreExports = true;

            if (this.checkQueue() == -1) {
                //Nothing in Queue
                moreExports = false;
            }

            while (moreExports || threadCount > 0) {
                //Start threads
                while ((threadCount < slaveThreadCount) && moreExports) {

                    new Thread(new HighVolumePayrollExportThread(this,
                            clientId, currentUser),
                            "HighVolumePayrollExportThread - " + threadCount)
                            .start();
                    this.addThreadCount(1);
                    Thread.sleep(3000);
                    if (this.checkQueue() == -1) {
                        //queue Empty
                        moreExports = false;
                    }
                }

                Thread.sleep(3000);

                if (exceptionFromSlaves != null) {
                    throw new NestedRuntimeException(exceptionFromSlaves);
                }
                if (this.checkQueue() == -1) {
                    //queue Empty
                    moreExports = false;
                }
            }
        } catch (SQLException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger
                        .error("Could not read from queue, can not run export",
                                e);
            }
            throw new NestedRuntimeException(e);
        } catch (InterruptedException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("This thread has been interupted", e);
            }
            throw new NestedRuntimeException(e);
        } finally {
            try{
                if (isConnectionOwner && conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                if (logger.isEnabledFor(Level.ERROR)) {
                    logger.error("Could not close the connection.", e);
                }
                throw new NestedRuntimeException(e);
            }
        }

    }

    public void setInterruptCheck(ScheduledJob check) {
        intCheck = check;
    }

    private synchronized void addThreadCount(int count) {
        if (parent == null) {
            threadCount += count;
        } else {
            parent.addThreadCount(count);
        }
    }

    private synchronized void subThreadCount(int count) {
        if (parent == null) {
            threadCount -= count;
        } else {
            parent.subThreadCount(count);
        }
    }

    private DBConnection initDBConnection() throws SQLException {

        if (logger.isDebugEnabled()) {
            logger.debug("InitDBConnection begin.");
        }
        if (conn != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("DB Already init'd.");
            }
            return conn;
        }

        conn = new DBConnection(ConnectionManager.getConnection());
        isConnectionOwner = true;
        //DBConnection conn = SQLHelper.connectToDevl();
        conn.setAutoCommit(false);
        conn.turnTraceOff();
        return conn;
    }

    /**
     * Sets the number of threads to run the recalc.
     * 
     * @param v
     *            number of threads
     */
    public void setSlaveThreadCount(int v) {

        if (v > THREAD_CNT_MAX || v < 0) {
            throw new RuntimeException("Thread count must be between 0 "
                    + " and " + THREAD_CNT_MAX);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Setting slave thread Cound to " + v);
        }
        this.slaveThreadCount = v;
    }

    /**
     * 
     * @return
     *  
     */
    public int getQueueId() {
        return queueId;
    }

    protected int selectExportFromQueue() throws Exception, SQLException {
        if (logger.isDebugEnabled()) {
            logger
                    .debug("HighVolumePayrollExportThread: selectExportFromQueue begin");
        }
        int selection = -1;
        int candidate = 0;

        while (candidate != -1) {
            candidate = checkQueue();
            if (candidate != -1) {
                if (markRecords(candidate, QUEUE_STATUS_IN_PROGRESS)) {
                    selection = candidate;
                    if (logger.isDebugEnabled()) {
                        logger.debug("Found queue id selection: " + selection);
                    }
                    break;
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger
                    .debug("HighVolumePayrollExportThread: selectExportFromQueue end");
        }
        return selection;
    }

    private int countInProgress() throws SQLException {
        if (logger.isDebugEnabled()) {
            logger
                    .debug("HighVolumePayrollExportThread: countInProgress begin");
        }

        int count = 0;

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer progressSql = new StringBuffer();
            progressSql.append("SELECT count(").append(QUEUE_ID).append(") ");
            progressSql.append(" FROM ").append(QUEUE_TABLE);
            progressSql.append(" WHERE ").append(QUEUE_STATUS);
            progressSql.append(" = ? ");

            if (logger.isDebugEnabled()) {
                StringBuffer message = new StringBuffer().append(
                        "Counting records with status ").append(
                        QUEUE_STATUS_IN_PROGRESS).append(" using SQL \n ")
                        .append(progressSql.toString());
                logger.debug(message.toString());
            }

            ps = conn.prepareStatement(progressSql.toString());
            ps.setString(1, QUEUE_STATUS_IN_PROGRESS);
            rs = ps.executeQuery();
            while (rs.next()) {

                count = rs.getInt(1);
                if (logger.isDebugEnabled()) {
                    logger.debug("Number of threads in progress: " + count);
                }
            }

        } catch (SQLException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Could not get Exports in progress", e);
            }
            throw e;
        } finally {
            if (ps != null) {
                ps.close();
            }
            if (rs != null) {
                rs.close();
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: countInProgress end");
        }
        return count;
    }

    protected int checkQueue() throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: checkQueue begin");
        }
        int wieqId = -1;
        int index = 0;

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer readySql = new StringBuffer();
            readySql.append("SELECT ").append(QUEUE_ID);
            readySql.append(" FROM ").append(QUEUE_TABLE);
            readySql.append(" WHERE ").append(QUEUE_STATUS);
            readySql.append(" = ? ORDER BY ");
            readySql.append(QUEUE_SUBMIT_DATE);

            if (logger.isDebugEnabled()) {
                StringBuffer message = new StringBuffer().append(
                        "Getting next Export in Queue w/ Status").append(
                        QUEUE_STATUS_READY).append(" using SQL \n ").append(
                        readySql.toString());
                logger.debug(message.toString());
            }

            ps = conn.prepareStatement(readySql.toString());
            ps.setString(1, QUEUE_STATUS_READY);
            rs = ps.executeQuery();
            if (rs.next()) {
                wieqId = rs.getInt(1);
                if (logger.isDebugEnabled()) {
                    logger.debug("Found ready export, queue id" + wieqId);
                }
            }

        } catch (SQLException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Could not get Ready Exports", e);
            }
            throw e;
        } finally {
            if (ps != null) {
                ps.close();
            }
            if (rs != null) {
                rs.close();
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: checkQueue end");
        }
        return wieqId;
    }

    private boolean markRecords(int wieqId, String status) throws Exception,
            SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: markRecords begin");
        }
        final DBServer dbs = DBServer.getServer(conn);
        int lockNumber = 0;

        boolean success = true;
        Time t = new Time(System.currentTimeMillis());
        String dateString = new StringBuffer().append(
                DateHelper.getCurrentDate().toString().substring(0, 11))
                .append(t.toString()).toString();
        Datetime readyTime = DateHelper.parseDate(dateString, TIMESTAMP_FORMAT);
        Timestamp readyTimeTS = Timestamp.valueOf(DateHelper.convertDateString(readyTime, TIMESTAMP_FORMAT));

        PreparedStatement ps = null;
        try {
            int index = 1;
            StringBuffer markSql = new StringBuffer();
            markSql.append("UPDATE ").append(QUEUE_TABLE);
            markSql.append(" SET ").append(QUEUE_STATUS).append(" = ? , ");
            if (QUEUE_STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
                markSql.append(QUEUE_LOCK).append(" = ?, ").append(
                        QUEUE_START_TIME);
            } else if (QUEUE_STATUS_COMPLETE.equalsIgnoreCase(status)) {
                markSql.append(QUEUE_END_TIME);
            } else if (QUEUE_STATUS_ERROR.equalsIgnoreCase(status)) {
                markSql.append(QUEUE_END_TIME);
            } else {
                throw new Exception("Invalid Status");
            }

            markSql.append(" = ? ");
            markSql.append(" WHERE ").append(QUEUE_ID);
            markSql.append(" = ? ");

            if (logger.isDebugEnabled()) {
                StringBuffer message = new StringBuffer().append(
                "Marking record with status ").append(
                        QUEUE_STATUS_IN_PROGRESS).append(", queue id ").append(
                        wieqId).append(" using SQL \n ").append(markSql.toString());
                logger.debug(message.toString());
            }

            ps = conn.prepareStatement(markSql.toString());

            ps.setString(index++, status);
            if (QUEUE_STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
                lockNumber = SQLHelper.getSequenceNumber(QUEUE_SEQ, conn);
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append(
                            "Queue status changing to ").append(
                            QUEUE_STATUS_IN_PROGRESS).append(
                            " so record must be locked ").append(
                            "\n Using Lock number: ").append(lockNumber)
                            .toString());
                }
                ps.setInt(index++, lockNumber);
            }
            if (dbs.isDB2()){
                ps.setDate(index++,  new java.sql.Date(readyTime.getTime()));
            }
            else {
                ps.setTimestamp(index++, readyTimeTS);
            }
            ps.setInt(index++, wieqId);
            ps.executeUpdate();
            conn.commit();

            if (QUEUE_STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Check Queue is locked ");
                }
                int testLock = checklock(wieqId);
                if (lockNumber != testLock) {
                    success = false;
                }
            }

        } catch (SQLException e) {
            conn.rollback();
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Could not mark Exports as in Progress", e);
            }
            throw e;
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: markRecords end");
        }
        return success;
    }

    private int checklock(int wieqId) throws SQLException {
        int comitLock = -1;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(new StringBuffer().append("SELECT ")
                    .append(QUEUE_LOCK).append(" FROM ").append(QUEUE_TABLE)
                    .append(" WHERE ").append(QUEUE_ID).append(" = ? ")
                    .toString());
            ps.setInt(1, wieqId);
            rs = ps.executeQuery();
            while (rs.next()) {
                comitLock = rs.getInt(1);
            }
        } catch (SQLException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Could not read Queue Lock number");
            }
            throw e;
        } finally {

            if (ps != null) {
                ps.close();
            }
            if (rs != null) {
                rs.close();
            }
        }

        return comitLock;
    }

    private void process() throws SQLException, Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: process begin");
        }

        ResultSet rs = getQueueRecord();
        setExportParams(rs);
        rs.close();
        doExport();

        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: process end");
        }
    }

    private ResultSet getQueueRecord() throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: getQueueRecord begin");
        }

        PreparedStatement ps = null;
        ResultSet rs = null;

        StringBuffer queueSql = new StringBuffer();
        queueSql.append("SELECT ");
        queueSql.append(QUEUE_PAYGRP_IDS).append(", ");
        queueSql.append(QUEUE_EMP_IDS).append(", ");
        queueSql.append(QUEUE_PAYSYS_ID).append(", ");
        queueSql.append(QUEUE_LOOK_BACK_DAYS).append(", ");
        queueSql.append(QUEUE_TERM_ADD_DAYS).append(", ");
        queueSql.append(QUEUE_USE_PAYPERIOD).append(", ");
        queueSql.append(QUEUE_ALL_READY_PAYGRPS).append(", ");
        queueSql.append(QUEUE_ADJUST_DATES).append(", ");
        queueSql.append(QUEUE_WRITE_TO_FILE).append(", ");
        queueSql.append(QUEUE_WRITE_TO_TABLE).append(", ");
        queueSql.append(QUEUE_MERGE_FILES).append(", ");
        queueSql.append(QUEUE_CHK_UNAUTH).append(", ");
        queueSql.append(QUEUE_ALL_READY_WHERE).append(", ");
        queueSql.append(QUEUE_START_DATE).append(", ");
        queueSql.append(QUEUE_END_DATE).append(", ");
        queueSql.append(QUEUE_CYCLE).append(", ");
        queueSql.append(QUEUE_SUBMITTED_BY);
        queueSql.append(" FROM ").append(QUEUE_TABLE);
        queueSql.append(" WHERE ").append(QUEUE_ID);
        queueSql.append(" = ? ");

        if (logger.isDebugEnabled()) {
            logger.debug(new StringBuffer().append(
                    "Getting Queue records with ID ").append(queueId).append(
                    " using SQL \n ").append(queueSql.toString()).toString());
        }

        ps = conn.prepareStatement(queueSql.toString());
        ps.setInt(1, queueId);
        rs = ps.executeQuery();

        if (logger.isDebugEnabled()) {
            logger.debug("Result set found " + rs.toString());
            logger.debug("HighVolumePayrollExportThread: getQueueRecord end");
        }
        return rs;
    }

    private void setExportParams(ResultSet rs) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger
                    .debug("HighVolumePayrollExportThread: setExportParams begin");
        }
        rs.next();
        int payGrpSt = rs.getInt(QUEUE_PAYGRP_IDS);
        String empIdsSt = rs.getString(QUEUE_EMP_IDS);

        if (empIdsSt == null) {
            payGrpIds = new int[1];
            payGrpIds[0] = payGrpSt;
        } else {
            empIds = detokenizeStringAsIntArray(empIdsSt);
        }

        paysysId = rs.getInt(QUEUE_PAYSYS_ID);
        lookBackDays = rs.getInt(QUEUE_LOOK_BACK_DAYS);
        termAddDays = rs.getInt(QUEUE_TERM_ADD_DAYS);
        if ("Y".equalsIgnoreCase(rs.getString(QUEUE_USE_PAYPERIOD))) {
            usePayPeriod = true;
        }
        if ("Y".equalsIgnoreCase(rs.getString(QUEUE_ALL_READY_PAYGRPS))) {
            allReadyPaygrps = true;
        }
        if ("Y".equalsIgnoreCase(rs.getString(QUEUE_ADJUST_DATES))) {
            adjustDates = true;
        }
        if ("Y".equalsIgnoreCase(rs.getString(QUEUE_WRITE_TO_FILE))) {
            writeToFile = true;
        }
        if ("Y".equalsIgnoreCase(rs.getString(QUEUE_WRITE_TO_TABLE))) {
            writeToTable = true;
        }
        if ("Y".equalsIgnoreCase(rs.getString(QUEUE_MERGE_FILES))) {
            mergeFiles = true;
        }
        if ("Y".equalsIgnoreCase(rs.getString(QUEUE_CHK_UNAUTH))) {
            chkUnauth = true;
        }

        allReadyWhere = rs.getString(QUEUE_ALL_READY_WHERE);
        if (!usePayPeriod) {
            startDate = rs.getDate(QUEUE_START_DATE);
            endDate = rs.getDate(QUEUE_END_DATE);
        }
        cycle = rs.getString(QUEUE_CYCLE);

        submittedBy = rs.getString(QUEUE_SUBMITTED_BY);

        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: setExportParams end");
        }
    }

    protected void doExport() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: doExport begin");
        }
        PayrollExporter payExp = new PayrollExporter(conn, null);
        if (empIds == null) {
            payExp.setPaygrpIds(payGrpIds);
        } else {
            payExp.setEmpIds(empIds);
        }
        payExp.setLookBackDays(lookBackDays);
        payExp.setTermAddDays(termAddDays);
        payExp.setCycle(cycle);
        if (usePayPeriod) {
            payExp.useDefaultDates();
        } else {
            payExp.setDates(startDate, endDate);
        }
        if (chkUnauth) {
            ArrayList failedPG = payExp.checkUnauth();
            if (failedPG != null) {
                if (!failedPG.isEmpty()) {
                    markRecords(queueId, QUEUE_STATUS_ERROR);
                    StringBuffer msg = new StringBuffer();
                    msg
                            .append("Error exporting records, some records not authorized. Check Authorization Report \n");
                    msg.append("The following pay group failed ");
                    msg.append(failedPG.toString());
                    sendMessage(EXPORT_FAILD_MSG, msg.toString());
                    return;
                }
            }
        }
        payExp.setPayExpTsk(paysysId);
        payExp.setWriteToFile(writeToFile);
        payExp.setWriteToTable(writeToTable);
        payExp.setAdjustDates(adjustDates);
        payExp.setMergeFiles(mergeFiles);
        payExp.process();
        markRecords(queueId, QUEUE_STATUS_COMPLETE);
        writeMessage(payExp.getMessage());
        sendMessage(EXPORT_FINISHED_MSG, payExp.getMessage());
        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: doExport end");
        }

    }

    protected void sendMessage(String message, String body) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: message begin");
            logger.debug(new StringBuffer().append("Creating message to ")
                    .append(submittedBy).append(" message: ").append(message)
                    .append(" body: ").append(body).toString());
            logger.debug("Client ID: " + SecurityService.getCurrentClientId());
            logger.debug("User : " + SecurityService.getUserNameActual());
        }

        Message m = new Message(conn);
        m.setTo(submittedBy);
        m.setSenderName("WORKBRAIN");
        m.setMessageBody(body);
        m.setMessageSubject(message);
        m.setMessageType(Util.MESSAGE_TYPE_MAIL);
        m.setSaveMessageCopy(false);
        m.send();
        conn.commit();
        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumePayrollExportThread: message end");
        }
    }

    private int[] detokenizeStringAsIntArray(String input) {
        String[] st = StringHelper.detokenizeString(input, SEPARATOR);
        if (st == null || st.length == 0) {
            return null;
        }
        int[] stArray = new int[st.length];
        for (int i = 0; i < st.length; i++) {
            stArray[i] = Integer.parseInt(st[i]);
        }
        return stArray;
    }

    private void writeError(Throwable e) {
        writeMessage(e.toString());
    }

    private void writeMessage(String msg) {
        try {

            if (queueId == -1) {
                if (logger.isDebugEnabled()) {
                    logger.debug(new StringBuffer().append(
                            "Export not yet selected from Queue").append(
                            "\n Sending message to default user: ").append(
                            submittedBy));
                }

                sendMessage("Payroll Export", msg);
            } else {
                PreparedStatement ps = null;

                StringBuffer addMsgSql = new StringBuffer().append("UPDATE ")
                        .append(QUEUE_TABLE).append(" SET ").append(
                                QUEUE_MESSAGE).append(" = ? ")
                        .append(" WHERE ").append(QUEUE_ID).append(" = ? ");

                int index = 1;
                ps = conn.prepareStatement(addMsgSql.toString());

                ps.setString(index++, msg);
                ps.setInt(index++, queueId);

                ps.executeUpdate();
                conn.commit();
            }

        } catch (SQLException t) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Could not write error to database record.", t);
            }
        }
    }

    /**
     * Method can be overriden for client customizations. This method is called
     * before any processing is done in the thread.
     * 
     * @throws Exception
     *  
     */
    protected void beforeThreadRun() throws Exception {

    }

    /**
     * Method can be overriden for client customizations. This method is called
     * after all processing is done in the thread.
     * 
     * @throws Exception
     *  
     */
    protected void afterThreadRun() throws Exception {

    }
}
