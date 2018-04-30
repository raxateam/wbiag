package com.wbiag.app.ta.ruleengine;

import com.workbrain.sql.*;
import com.workbrain.server.sql.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.util.*;
import com.workbrain.app.scheduler.*;
import com.workbrain.security.*;
import com.workbrain.server.registry.Registry;
import com.workbrain.app.scheduler.*;
import com.wbiag.app.ta.db.*;

import javax.naming.*;
import java.util.Date;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.text.*;
import com.workbrain.app.scheduler.enterprise.ScheduledJob;

/**
 * Recalculates records for added employees and dates in given batch sizes
 * and thread count.
 * Although com.workbrain.app.ta.rulengine has the same class, there are shortcomings at this times
 *   as core class is not interruptible and has issues with multithreading
 * @deprecated    As of 4.1 FP28, use core classes
 */
public class RecalculateRecords implements Runnable {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(RecalculateRecords.class);

    public static final int CALC_BATCH_SIZE_MIN = 10;
    public static final int CALC_BATCH_SIZE_MAX = RuleAccess.BATCH_PROCESS_SIZE_DEFAULT ;
    public static final int THREAD_CNT_MAX = 24;

    private DBConnection conn;
    private boolean isConnectionOwner = false;
    private int batchSize = CALC_BATCH_SIZE_MAX;
    private boolean doCommitEveryBatch = true;
    private boolean processLevelAutoRecalc = false;
    private boolean auditsCalculationErrorEmpDates = false;
    private boolean deletesCalcEmployeeDateTable = false;
    private boolean createsDefaultRecords = true;
    private Boolean processLevelFutureBalanceRecalc = null;
    private List calculationErrorEmpDates = new ArrayList();
    private int processedCount;

    private Throwable exceptionFromSlaves = null;
    private int slaveThreadCount = 0;
    private int threadCountStarted;
    private ScheduledJob intCheck;

    private RecalculateRecords parent;
    private String clientId;
    /**
     * Map of employee dates keyed by empId as String.
     */
    private final Map empDates;
    private Iterator empDatesIterator;
    private int approxEmpDatesToCalc;

    /**
     * Constructor
     * @param conn DBConnection
     */
    public RecalculateRecords( DBConnection conn) {
        this.conn = conn;
        this.clientId = SecurityService.getCurrentClientId();
        empDates = new HashMap();
    }

    /**
     * Sets the number of threads to run the recalc.
     *
     * @param v number of threads
     */
    public void setSlaveThreadCount(int v) {
        if (v > THREAD_CNT_MAX || v < 0) {
            throw new RuntimeException("Thread count must be between 0 "
                                       +" and " + THREAD_CNT_MAX);
        }
        this.slaveThreadCount = v;
    }

    /**
     * Sets the number of employee dates that calculation will be done.
     *
     * @param v CalculationBatchSize
     */
    public void setCalculationBatchSize(int v) {
        if (v > CALC_BATCH_SIZE_MAX || v < CALC_BATCH_SIZE_MIN) {
            throw new RuntimeException("Batch size must be between " + CALC_BATCH_SIZE_MIN
                                       +" and " + CALC_BATCH_SIZE_MAX);
        }
        this.batchSize = v;
    }

    /**
     * Sets processLevelAutoRecalc which checks calculation group
     * Calculation Period and Auto Recalculate settings.
     *
     * @param autoRecalc boolean
     */
    public void setProcessLevelAutoRecalc(boolean autoRecalc) {
        processLevelAutoRecalc = autoRecalc;
    }

    /**
     * Sets whether commit is issues at every batch.
     *
     * @param v boolean
     */
    public void setCommitsEveryCalculationBatch(boolean v) {
        this.doCommitEveryBatch = v;
    }

    /**
     * Sets whether employee/dates from calculation unxpected errors will be logged.
     * If not set, the exception will be thrown.
     *
     * @param v boolean
     */
    public void setAuditsCalculationErrorEmpDates(boolean v) {
        this.auditsCalculationErrorEmpDates = v;
    }

    /**
     * Sets whether employee/dates from CALC_EMPLOYEE_DATE will be deleted
     *
     * @param v boolean
     */
    public void setDeletesCalcEmployeeDateTable(boolean v) {
        this.deletesCalcEmployeeDateTable = v;
    }

    /**
     * Sets processLevelFutureBalanceRecalc.
     *
     */
    public void setProcessLevelFutureBalanceRecalc( Boolean futureBalanceRecalc ) {
        processLevelFutureBalanceRecalc = futureBalanceRecalc;
    }

    public void setInterruptCheck( ScheduledJob check ) {
        intCheck = check;
    }

    /**
     * Sets whether default records need to be created.
     *
     * @param v boolean
     */
    public void setCreatesDefaultRecords(boolean v) {
        this.createsDefaultRecords = v;
    }

    /**
     * Add employee date to be recalculated.
     *
     * @param empId empId
     * @param date date
     */
    public void addEmployeeDate(int empId, Date date) {
        String key = String.valueOf(empId);
        List dates;
        if (empDates.containsKey(key)) {
            dates = (List)empDates.get(key);
        }
        else {
            dates = new ArrayList();
        }
        if (!dates.contains(date)) {
            dates.add(date);
        }
        empDates.put(key , dates);
    }


    /**
     *
     * Add employee start end date to be recalculated.
     *
     * @param empId empId
     * @param startDate startDate
     * @param endDate endDate
     */
    public void addEmployeeDate(int empId, Date startDate, Date endDate) {
        for (Date date = startDate; date.compareTo(endDate) <= 0;
             date = DateHelper.addDays(date, 1)) {
            addEmployeeDate(empId , date);
        }
    }

    /**
     * Add employees start end date to be recalculated.
     *
     * @param empIds empIds
     * @param startDate startDate
     * @param endDate endDate
     */
    public void addEmployeeDate(int[] empIds, Date startDate, Date endDate) {
        if (empIds == null || empIds.length == 0) {
            return;
        }
        for (int i = 0; i < empIds.length; i++) {
            addEmployeeDate(empIds[i] , startDate , endDate);
        }
    }

    /**
     * Adds employees qualified fo given empIds, paygrps, teams and calcgrps
     * for given start and end dates.
     *
     * @param empIds empIds
     * @param teamIds teamIds
     * @param calcgrpIds calcgrpIds
     * @param paygrpIds paygrpIds
     * @param subteams whether subteams will be conmsidered for teamIDs
     * @param startDate startDate
     * @param endDate endDate
     * @throws SQLException
     */
    public void addEmployeeDate(int[] empIds,
                                int[] teamIds,
                                int[] calcgrpIds,
                                int[] paygrpIds,
                                boolean subteams,
                                Date startDate,
                                Date endDate) throws SQLException{
        addEmployeeDate(empIds, teamIds, calcgrpIds , paygrpIds ,
                        subteams , false , startDate , endDate);
    }

    /**
     * Adds employees qualified fo given empIds, paygrps, teams and calcgrps
     * based on their current paygroup start and end dates.
     *
     * @param empIds empIds
     * @param teamIds teamIds
     * @param calcgrpIds calcgrpIds
     * @param paygrpIds paygrpIds
     * @param subteams whether subteams will be conmsidered for teamIDs
     * @throws SQLException
     */
    public void addEmployeeDate(int[] empIds,
                                int[] teamIds,
                                int[] calcgrpIds,
                                int[] paygrpIds,
                                boolean subteams) throws SQLException{
        addEmployeeDate(empIds, teamIds, calcgrpIds , paygrpIds ,
                        subteams , true, null , null);
    }

    /**
     * Adds employees qualified fo given empIds, paygrps, teams and calcgrps
     * based on their current paygroup start and end dates or start/end dates.
     * If basedOnPayGroupDates, start/end dates are ignored.
     *
     * @param empIds empIds
     * @param teamIds teamIds
     * @param calcgrpIds calcgrpIds
     * @param paygrpIds paygrpIds
     * @param subteams whether subteams will be conmsidered for teamIDs
     * @param basedOnPayGroupDates whether date selection will; be based on paygroup dates
     * @param startDate startDate
     * @param endDate endDate
     * @throws SQLException
     */
    private void addEmployeeDate(int[] empIds,
                                int[] teamIds,
                                int[] calcgrpIds,
                                int[] paygrpIds,
                                boolean subteams,
                                boolean basedOnPayGroupDates,
                                Date startDate,
                                Date endDate) throws SQLException{

        if (!basedOnPayGroupDates) {
            if (startDate == null || endDate == null) {
                throw new RuntimeException ("Both start and end date must be supplied when not basedOnPayGroupDates");
            }
        }
        StringBuffer sb = new StringBuffer(400);
        sb.append(" SELECT employee.emp_id");
        if (basedOnPayGroupDates) {
            sb.append(" ,paygrp_start_date, paygrp_end_date ");
        }
        sb.append(" FROM employee ");
        if (basedOnPayGroupDates) {
            sb.append(" ,pay_group");
        }
        if (teamIds != null && teamIds.length > 0) {
            sb.append(" , employee_team");
        }
        if (basedOnPayGroupDates) {
            sb.append(" WHERE pay_group.paygrp_id = employee.paygrp_id ");
        }
        else {
            sb.append(" WHERE 1=1 ");
        }
        if (empIds != null && empIds.length > 0) {
            sb.append ( " AND employee.emp_id IN ( ");
            for (int i = 0; i < empIds.length; i++) {
                sb.append(i > 0 ? ",?" : "?");
            }
            sb.append (" ) ");
        }
        if (teamIds != null && teamIds.length > 0) {
            sb.append(" AND wbt_id IN ( ");
            if (!subteams) {
                for (int i = 0; i < teamIds.length; i++) {
                    sb.append(i > 0 ? ",?" : "?");
                }
                sb.append(" ) ");
            }
            else {
                sb.append("   SELECT child_wbt_id FROM sec_wb_team_child_parent ");
                sb.append("   WHERE parent_wbt_id IN ( ");
                for (int i = 0; i < teamIds.length; i++) {
                    sb.append(i > 0 ? ",?" : "?");
                }
                sb.append("   ) ");
                sb.append(" ) ");
            }
            sb.append(" AND empt_home_team = 'Y' ");
            sb.append(" AND employee_team.emp_id = employee.emp_id ");
        }
        if (calcgrpIds != null && calcgrpIds.length > 0) {
            sb.append ( " AND calcgrp_id IN ( ");
            for (int i = 0; i < calcgrpIds.length; i++) {
                sb.append(i > 0 ? ",?" : "?");
            }
            sb.append (" ) ");
        }
        if (paygrpIds != null && paygrpIds.length > 0) {
            if (basedOnPayGroupDates) {
                sb.append ( " AND pay_group.paygrp_id IN ( ");
            }
            else {
                sb.append ( " AND paygrp_id IN ( ");
            }
            for (int i = 0; i < paygrpIds.length; i++) {
                sb.append(i > 0 ? ",?" : "?");
            }
            sb.append (" ) ");
        }


        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            log("Sql " + sb.toString());
            ps = conn.prepareStatement(sb.toString());
            int cnt = 1;
            if (empIds != null && empIds.length > 0) {
                for (int i = 0; i < empIds.length; i++) {
                    ps.setInt(cnt++ , empIds[i]);
                }
            }
            if (teamIds != null && teamIds.length > 0) {
                for (int i = 0; i < teamIds.length; i++) {
                    ps.setInt(cnt++ , teamIds[i]);
                }
            }
            if (calcgrpIds != null && calcgrpIds.length > 0) {
                for (int i = 0; i < calcgrpIds.length; i++) {
                    ps.setInt(cnt++ , calcgrpIds[i]);
                }
            }
            if (paygrpIds != null && paygrpIds.length > 0) {
                for (int i = 0; i < paygrpIds.length; i++) {
                    ps.setInt(cnt++ , paygrpIds[i]);
                }
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                if (basedOnPayGroupDates) {
                    addEmployeeDate(rs.getInt(1), rs.getDate(2), rs.getDate(3));
                }
                else {
                    addEmployeeDate(rs.getInt(1), startDate , endDate);
                }
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

    }

    /**
     * Clears emploeye dates added by <code>addEmployeeDate</code>  methods.
     */
    public void clear() {
        if (empDates != null) {
            empDates.clear();
        }
    }

    /**
     * Returns number of employees loaded.
     *
     * @return number of employee
     */
    public int getEmployeeCount() {
        if (empDates == null || empDates.size() == 0) {
            return 0;
        }
        return empDates.size();
    }

    /**
     * Returns map of employee and dates loaded.
     *
     * @return map of employee and dates
     */
    public Map getEmployeeDates() {
        return empDates ;
    }

    /**
     * Returns number of employee and dates loaded.
     *
     * @return number of employee and dates
     */
    public int getEmployeeDatesCount() {
        if (empDates == null || empDates.size() == 0) {
            return 0;
        }
        int cnt = 0;
        Iterator iter = empDates.values().iterator();
        while (iter.hasNext()) {
            List dates = (List)iter.next();
            cnt += dates != null ? dates.size() : 0;
        }
        return cnt;
    }

    /**
     * Returns list of <code>CalcEmployeeDatesError</code> objects if  auditsCalculationErrorEmpDates is set to true.
     *
     * @return list of <code>CalcEmployeeDatesError</code>
     */
    public List getCalculationEmployeeDatesErrors() {
        return calculationErrorEmpDates;
    }

    private RecalculateRecords(RecalculateRecords parent, int approxEmpDatesToCalc)
    {
        this.parent = parent;
        this.conn = null;
        this.empDates = null;
        this.empDatesIterator = parent.empDatesIterator;
        this.batchSize = parent.batchSize;
        this.doCommitEveryBatch = parent.doCommitEveryBatch;
        this.deletesCalcEmployeeDateTable = parent.deletesCalcEmployeeDateTable;
        this.processLevelFutureBalanceRecalc   = parent.processLevelFutureBalanceRecalc;
        this.intCheck = parent.intCheck;
        this.clientId = SecurityService.getCurrentClientId();
        this.approxEmpDatesToCalc = approxEmpDatesToCalc;
        this.processLevelAutoRecalc = parent.processLevelAutoRecalc;
        this.auditsCalculationErrorEmpDates = parent.auditsCalculationErrorEmpDates;
    }

    /**
     *
     * @throws SQLException
     */
    public void execute() throws SQLException {
        if (slaveThreadCount > 1) {
            execute(slaveThreadCount);
        }
        else {
            RuleAccess ra = new RuleAccess(conn);
            ra.setCreatesDefaultRecords(createsDefaultRecords);
            ra.setProcessLevelAutoRecalc(processLevelAutoRecalc);
            ra.setProcessLevelFutureBalanceRecalc(processLevelFutureBalanceRecalc);
            int daysAdded = 0;
            int calcedEmployees = 0;
            if (parent == null) {
                this.empDatesIterator = empDates.entrySet().iterator();
            }
            RecalculateRecords newThis = parent!=null ? parent : this;
            Iterator it = newThis.empDatesIterator;
            if (it == null) return;
            while ( true )
            {
                Map.Entry entry;
                synchronized ( it )
                {
                    if ( !it.hasNext() )
                        break;
                    entry = (Map.Entry) it.next();
                }

                int empId = Integer.parseInt((String)entry.getKey());
                List dates = (List) entry.getValue();
                if (dates == null || dates.size() == 0) {
                    continue;
                }
                Collections.sort(dates);
                Iterator iterD = dates.iterator();
                SimpleDateFormat fmt = new SimpleDateFormat( "yyyy-MM-dd" );
                while (iterD.hasNext()) {
                    Date date = (Date) iterD.next();
                    if (logger.isDebugEnabled()) logger.debug( Thread.currentThread().getName() + " recalcing empId: " + empId + " for: " + fmt.format(date) );
                    ra.addRecordToCalculate(empId , date);
                    if (++daysAdded % batchSize == 0) {
                        log ("Calculating " + batchSize + " days");
                        try {
                            if (deletesCalcEmployeeDateTable) {
                                deleteCalcEmployeeDate(conn,  ra.getEmpIdAndDatesList());
                            }
                            ra.calculateRecords(false, false);
                            if (doCommitEveryBatch) {
                                conn.commit();
                            }
                            if( intCheck != null ) {
                                synchronized ( intCheck ) {
                                    try {
                                        if( intCheck.isInterrupted() ) {
                                            logger.warn( "Recalculate Records interrupted");
                                            break;
                                        }
                                    } catch( ScheduleException exc ) {
                                        logger.warn( "Error checking interrupt status", exc );
                                    }
                                }
                            }
                            // removed 2005-06-24 by glewis: these destroy caching although they prevent Oracle leaks.  Resultion: workaround Oracle leaks instead.
                            // reinitDBConnection();
                            // ra = new RuleAccess(conn);
                        }
                        catch (Throwable t) {
                            logger.error("Thread :" + Thread.currentThread().getName() + " error: " + t.getMessage());
                            t.printStackTrace();
                            if (!this.auditsCalculationErrorEmpDates) {
                                throw new NestedRuntimeException(t);
                            }
                            else {
                                addCalcEmpDatesError(ra.getEmpIdAndDatesList() , t);
                            }
                        }
                    }
                }
                addProcessedCount(dates.size());
                if ( ++calcedEmployees % 250 == 0 && approxEmpDatesToCalc > 0 ) {
                    if (logger.isDebugEnabled()) logger.debug(Thread.currentThread().getName() +                                       " has  completed " +                                       (100 * calcedEmployees /                                        approxEmpDatesToCalc) +   "% of its load.");
                }
            }
            if (daysAdded % batchSize != 0) {
                log ("Calculating " + (daysAdded % batchSize) + " days");
                try {
                    if (deletesCalcEmployeeDateTable) {
                        deleteCalcEmployeeDate(conn,  ra.getEmpIdAndDatesList());
                    }
                    ra.calculateRecords(false, false);
                    if (doCommitEveryBatch) {
                        conn.commit();
                    }
                }
                catch (Throwable t) {
                    if (!this.auditsCalculationErrorEmpDates) {
                        throw new NestedRuntimeException(t);
                    }
                    else {
                        addCalcEmpDatesError(ra.getEmpIdAndDatesList() , t);
                    }
                }
            }

        }
    }

    /**
     * Returns initial count
     * @return initial count
     */
    public int getInitialCount() {
        return 0;
    }

    /**
     * Returns number of days processed
     * @return count
     */
    public int getCurrentCount() {
        return processedCount;
    }

    /**
     * Returns number of employee dates processed
     * @return number of employee dates
     */
    public int getFinalCount() {
        return getEmployeeDatesCount();
    }

    private synchronized void addProcessedCount(int count) {
        if (parent == null) {
            processedCount += count;
        }
        else {
            parent.addProcessedCount(count);
        }
    }

    private synchronized void addCalcEmpDatesError(List empDates,
        Throwable t) {
        if (parent == null) {
            CalcEmployeeDatesError err = new CalcEmployeeDatesError();
            err.setEmpDates(empDates);
            err.setThrowable(t);
            calculationErrorEmpDates.add(err);
        }
        else {
            parent.addCalcEmpDatesError(empDates , t);
        }
    }

    private void execute(int slaveThreadCount) throws SQLException {
        if (empDates == null || empDates.size() == 0) {
            return;
        }

        log("execute(" + slaveThreadCount + ")");
        int  setSize = Math.min(slaveThreadCount, empDates.size());

        try {
            int subSize = empDates.size() / setSize;
            this.empDatesIterator = empDates.entrySet().iterator();
            try {
                for (int j = 0; j < setSize; j++) {
                    new Thread(
                        new RecalculateRecords(this, subSize ),
                        "RecalculateRecords-" + j).start();
                    threadCountStarted++;
                }
            }
            catch (Throwable t) {
                exceptionFromSlaves = t;
            }
            while (threadCountStarted > 0) {
                Thread.sleep( 3000 );
                if (exceptionFromSlaves != null) {
                    throw new NestedRuntimeException(exceptionFromSlaves);
                }

                // connection keep-alive
                PreparedStatement pstmt = null;
                ResultSet rs = null;
                try
                {
                    pstmt = conn.prepareStatement( "select * from WB_DUMMY" );
                    rs = pstmt.executeQuery();
                    while ( rs.next() )
                    {
                        String str = rs.getString(1);
                    }
                }
                finally
                {
                    SQLHelper.cleanUp( pstmt, rs );
                    if (doCommitEveryBatch) {
                        conn.commit();
                    }
                }
            }
            if (exceptionFromSlaves != null) {
                throw new NestedRuntimeException(exceptionFromSlaves);
            }
        }
        catch (InterruptedException ie) {
            throw new NestedRuntimeException(ie);
        }
    }

    /**
     *
     */
    public void run() {
        try {
            log("RecalculateRecords.SlaveThread started.");

            initDBConnection();
            SecurityService.setCurrentClientId(clientId);
            execute();
            if (!doCommitEveryBatch) {
                log("RecalculateRecords.SlaveThread committing the transaction. Use no slave threads if this is not desirable.");
                conn.commit();
            }
        }
        catch (Throwable t) {
            parent.exceptionFromSlaves = t;
            logger.error("WachRecalculateRecords thread " + Thread.currentThread().getName() + " error:");
            t.printStackTrace();
            if (parent != null) {
                parent.exceptionFromSlaves = t;
            }
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e) {
                // ignore
            }
        }
        finally {
            try {
                if ( isConnectionOwner && conn != null ) {
                    conn.close();
                }
            }
            catch (Throwable t) {
                t.printStackTrace();
                if ( parent != null ) {
                    parent.exceptionFromSlaves = t;
                }
            }
            if ( parent != null ) {
                synchronized (parent) {
                    parent.threadCountStarted--;
                    parent.notify();
                }
            }
        }
    }

    private DBConnection initDBConnection() throws SQLException {
        if ( conn != null )
            return conn;

        conn = new DBConnection( ConnectionManager.getConnection() );
        isConnectionOwner = true;
        //DBConnection conn = SQLHelper.connectToDevl();
        conn.setAutoCommit(false);
        conn.turnTraceOff();
        return conn;
    }

    private DBConnection reinitDBConnection()
        throws SQLException
    {
        if ( conn != null )
        {
            conn.commit();
            conn.close();
            conn = null;
        }
        return initDBConnection();
    }

    private void log( String message )  {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug( message );}
    }

    private long meterTime(String what, long start){
        long l = System.currentTimeMillis();
        log("\t"+what+" took: "+(l-start)+" millis");
        return l;
    }

    /**
     * Delete EmployeeIdAndDateList by BETWEEN statements to avoid full table scans
     * @param conn
     * @param edList
     * @return
     * @throws SQLException
     */
    private boolean deleteCalcEmployeeDate(DBConnection conn,
                                           EmployeeIdAndDateList edList ) throws SQLException{
        if (edList == null || edList.size() == 0) {
            return true;
        }
        Map empDates = edList.getMapOfEmployeeDates() ;
        StringBuffer sb = new StringBuffer(200);
        sb.append("DELETE FROM calc_employee_date WHERE ");
        Iterator iter = empDates.entrySet().iterator();
        int cnt = 1;
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            List dates = (List) entry.getValue();
            if (dates != null && dates.size() > 0) {
                sb.append( cnt++ > 1 ? " OR " : "");
                sb.append(" (emp_id = ? AND ced_work_date BETWEEN ? AND ?) ");
            }
        }
        if (cnt > 1) {
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(sb.toString());
                Iterator iter2 = empDates.entrySet().iterator();
                int setCnt = 1;
                while (iter2.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter2.next();
                    String key = (String)entry.getKey();
                    int empId = Integer.parseInt(key) ;
                    List dates = (List) entry.getValue();
                    if (dates != null && dates.size() > 0) {
                        Date minDate = (Date)dates.get(0);
                        Date maxDate = (Date)dates.get(dates.size() - 1);
                        ps.setInt(setCnt++, empId) ;
                        ps.setTimestamp(setCnt++, new java.sql.Timestamp(minDate.getTime()) ) ;
                        ps.setTimestamp(setCnt++, new java.sql.Timestamp(maxDate.getTime()) ) ;
                        if (logger.isDebugEnabled()) logger.debug("Deleting empId:" + empId + " from : " + minDate + " to : " + maxDate );
                    }
                }
                if (logger.isDebugEnabled()) logger.debug("Executing delete sql:" + sb.toString());
                int upd = ps.executeUpdate();
                if (logger.isDebugEnabled()) logger.debug("Deleted: " + upd + " records from calc_employee_date");
            }
            finally {
                if (ps != null)
                    ps.close();
            }
        }

        return true;
    }

    public static class CalcEmployeeDatesError {
        private List empDates;
        private Throwable throwable;

        public List getEmpDates(){
            return empDates;
        }

        public void setEmpDates(List v){
            empDates=v;
        }

        public Throwable getThrowable(){
            return throwable;
        }

        public void setThrowable(Throwable v){
            throwable=v;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer(200);
            sb.append("Error : " + throwable.getMessage());
            sb.append("\n StackTrace : " + StringHelper.getStackTrace(throwable));
            sb.append("\n Employee Dates :\n");
            Iterator iter = empDates.iterator();
            while (iter.hasNext()) {
                EmployeeIdAndDate item = (EmployeeIdAndDate)iter.next();
                sb.append(item);
            }
            return sb.toString();
        }

    }
}