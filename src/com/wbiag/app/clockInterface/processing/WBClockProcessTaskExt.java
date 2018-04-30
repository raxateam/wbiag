package com.wbiag.app.clockInterface.processing;

import com.workbrain.sql.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.clockInterface.processing.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.server.*;
import com.workbrain.server.sql.*;
import com.workbrain.server.registry.*;
import com.workbrain.tool.jdbc.proxy.*;
import com.workbrain.util.*;
import com.workbrain.security.*;

import java.sql.*;
import java.util.*;

import javax.naming.*;
import org.apache.log4j.Logger;

/**
 * Low level class for performing actual import, the import engine if you will.
 * 
 * Change History
 * 
 * bchan 01/30/2006 - Changed task to be multi-threaded when configured.  
 * 
 */
public class WBClockProcessTaskExt extends WBClockProcessTask {
    private static final Logger logger = Logger.getLogger(WBClockProcessTaskExt.class);

    protected int clockCountProcessed = 0;
    private boolean shouldCommit = true;
    private boolean autoRecalc = false; //default value

    public static final String PARAM_RDRGRP_NAMES = "rdrgrpNames";
    public static final String PARAM_RDRGRP_NAMES_INCL = "rdrgrpNamesInclusive";
    private Map param;
    private String[] rdrgrpNames;
    private boolean rdrgrpNamesInclusive;
    
    private String CLOCK_TRAN_PEND_J = "CLOCK_TRAN_PEND_J";
    private String clockTranPendJTbl = null;
    
    /**
     * Constructor.
     */
    public WBClockProcessTaskExt() {
    }

    /**
     * Called by the Scheduler to run this task.
     *
     * @param   taskID      id of the task to run
     * @param   params      parameter that this task was scheduled with.
     * @see com.workbrain.app.scheduler.enterprise.ScheduledJob#run(int, java.util.Map)
     */
    public Status run(int taskID, Map param) throws Exception {
        this.param = param;
        String srdrgrpNames = (String)this.param.get(PARAM_RDRGRP_NAMES);
        if (!StringHelper.isEmpty(srdrgrpNames)) {
            this.rdrgrpNames = StringHelper.detokenizeString(srdrgrpNames, ",");
        }
        String srdrgrpNamesInclusive = (String)this.param.get(PARAM_RDRGRP_NAMES_INCL);
        this.rdrgrpNamesInclusive = StringHelper.isEmpty(srdrgrpNamesInclusive)
            ? false : "Y".equals(srdrgrpNamesInclusive) ? true : false;

        this.clockTranPendJTbl = getClockTranPendJSql();
        
        return super.run(taskID , param );
    }

    /**
     * Start the import process, loads all pending records and then starts matching
     * them with appropriate transaction types
     * @param c The DB connection.
     * @param slaveThreadCount
     */
    public void execute( DBConnection c, int slaveThreadCount ) throws WBInterfaceException, SQLException {
        clockCountProcessed = 0;

        if (slaveThreadCount > 0) {
            new ThreadedWBClockProcessTaskExt(this).execute(c, slaveThreadCount);
        }
        else {
            List clients = loadClocks(c);
            if (clients.size() > 0) {
                clockCountProcessed = processClocksFor(clients, c);
            }
        }
    }

    /**
     * Called by the scheduler configuration to enable a task to specify
     * a URL to use for configuration. The URL will be passed the task id
     * as a parameter called TASK_ID
     *
     * @return String indicates the URL to use for config, or null if no config
     * @see com.workbrain.app.scheduler.enterprise.ScheduledJob#getTaskUI()
     */
    public String getTaskUI() {
        return "/jobs/wbiag/clockProcessTaskParamsExt.jsp";
    }

    private String getClockTranPendJSql() {
    	if (rdrgrpNames == null || rdrgrpNames.length == 0) {
    		return CLOCK_TRAN_PEND_J;
    	}
    	
        StringBuffer sql = new StringBuffer();
        sql.append("(SELECT * FROM CLOCK_TRAN_PEND_J ");
		sql.append("WHERE CTPJ_RDR_NAME IN (SELECT RDR_NAME FROM READER A, READER_GROUP B "); 
		sql.append("WHERE A.RDRGRP_ID = B.RDRGRP_ID ");
		sql.append("AND B.RDRGRP_NAME ");
		sql.append(this.rdrgrpNamesInclusive ? " IN " : " NOT IN ");
		sql.append("(");
        for (int i = 0; i < this.rdrgrpNames.length; i++) {
        	sql.append(i > 0 ? "," : "");
        	sql.append(SQLHelper.encode(this.rdrgrpNames[i])) ;
        }
		sql.append("))) X"); 
		
        if (logger.isDebugEnabled()) logger.debug("getClockTranPendJSql: " + sql.toString());
		return sql.toString();
    }
    
    /**
     * Get the value of the system/clockprocessing/SLAVE_THREAD_COUNT key in
     * the registry.
     * @return The value of the Ssystem/clockprocessing/LAVE_THREAD_COUNT key in
     * the registry.  Defaults to 0 if the key isn't found.
     */
    protected int getSlaveThreadCount() {
        return Registry.getVarInt(REG_SLAVE_THREAD_COUNT , 0);
    }

    /**
     * Get the maximum number of clocks to process during a single clock process task.
     * @return The maximum number of clocks to process during a single clock process
     * task.
     */
    protected int getMaxClocks() {
        int maxClocks = 1000; // maximum number of clocks to process during a
        // single clock process task
        try {
            String maxClocksVar = (String) Registry.getVar(REG_MAX_CLOCKS_VAR_NAME);
            maxClocks = Integer.parseInt(maxClocksVar);
        }
        catch (NamingException exc) {
            // error retrieving parameter, allow it to default
            getLogger().warn("Error getting registry variable: " +
                             REG_MAX_CLOCKS_VAR_NAME, exc);
        }
        catch (NumberFormatException exc) {
            // error parsing parameter - log the error
            getLogger().warn("Error parsing registry variable: " +
                             REG_MAX_CLOCKS_VAR_NAME +
                             ".  Expected numeric value.");
        }

        return maxClocks;
    }

    /**
     * Load all pending clocks from CLOCK_TRAN_PEND_J and return a
     * list of lists of ImportData objects for each client
     * @param conn The DB connection
     * @return A list for each client, where each element is a list
     * of ImportData objects.
     * @throws WBInterfaceException
     */
    protected List loadClocks(DBConnection conn) throws WBInterfaceException {
        ProxyStatement stmt = null;
        ResultSet rs = null;
        List clients = new ArrayList();
        int recCount = 0;
        int maxClocks = getMaxClocks();

        List importDataList = new ArrayList( maxClocks );
        ImportData importData;

        try {
            stmt = (ProxyStatement)conn.createStatement();
            String sql = getSelectorSql(conn);
            
            rs = stmt.executeQuery(sql,true,true);

            String thisClientId = "";
            String lastClientId = "";
            while( rs.next() && recCount < maxClocks ) {
                importData =  createImportData( rs );
                thisClientId = importData.getField(ClockTransactionMapper.FIELD_CLIENT_ID);
                if (recCount == 0) {
                    lastClientId = thisClientId;
                }
                if (!thisClientId.equals(lastClientId)) {
                    if (importDataList.size() > 0) {
                        clients.add(importDataList);
                    }
                    importDataList = new ArrayList(maxClocks);
                }
                importDataList.add( importData );
                lastClientId = thisClientId;
                recCount++;
            }

            if (importDataList.size() > 0) {
                clients.add(importDataList);
            }

        } catch( SQLException e ) {
            throw new WBInterfaceException(e);
        } finally {
            SQLHelper.cleanUp(stmt, rs);
        }

        return clients;
    }

    /**
     * Get a String containing an sql query that can retrieve all pending clocks.
     * @return An sql query String that can retrieve all pending clocks.
     * @deprecated  use {@link #getSelectorSql(DBConnection conn)}
     */
    protected String getSelectorSql() {
        return "SELECT * FROM " + this.clockTranPendJTbl + " ORDER BY CLIENT_ID, CTPJ_IDENTIFIER, CTPJ_ID";
    }

    /**
     * Get a String containing an sql query that can retrieve pending clocks limited by the getMaxClocks.
     *
     * @param DBConnection - connection to determine which DB is being access
     * @return An sql query String that can retrieve pending clocks - limited by the getMaxClocks
     */
    protected String getSelectorSql(DBConnection conn) {
        String fromClause = " FROM " + this.clockTranPendJTbl + " "; //ensure trailing spaces 
        String orderBy = " ORDER BY CLIENT_ID, CTPJ_IDENTIFIER, CTPJ_ID ";
        int numClocksToProcess = getMaxClocks();

        if (conn.getDBServer().isOracle()) {
            if (logger.isDebugEnabled()) logger.debug("Getting max " + numClocksToProcess + " clocks from Oracle DB");
            return "SELECT * " + fromClause
                    + " WHERE rownum < " + numClocksToProcess
                    + orderBy;
        }

        if (conn.getDBServer().isDB2()) {
            if (logger.isDebugEnabled()) logger.debug("Getting max " + numClocksToProcess + " clocks from DB2 DB");
            return "SELECT * " + fromClause + orderBy
                    + "FETCH FIRST " + numClocksToProcess + " ROWS ONLY";
        }

        if (conn.getDBServer().isMSSQL()) {
            if (logger.isDebugEnabled()) logger.debug("Getting max " + numClocksToProcess + " clocks from MSSQL DB");
            return "SELECT TOP " + numClocksToProcess + " * "
                    + fromClause + orderBy;
        }

        if (logger.isDebugEnabled()) logger.debug("Getting all clocks from pending table");

        return "SELECT * " + fromClause + orderBy;

    }
    
    /**
     * Process clocks for the list of clients.
     * @param clients A list for each client, where each element is a list of ImportData
     * objects from the imported clocks.
     * @param conn The DB connection.
     * @return The number of clocks processed.
     * @throws SQLException
     */
    protected int processClocksFor(List clients, DBConnection conn) throws SQLException {
        if(getLogger().isDebugEnabled()) getLogger().debug("WBClockProcessTask.processClocksFor()");
        int clocksProcessed = 0;

        ClockTransaction type = new ClockTransaction();
        BatchedClockProcessor batchedClockProcessor = new BatchedClockProcessor( conn );
        int numClocksToProcessBeforeCalc = getNumClocksToProcessBeforeCalc();  //number of clocks to process before calcing
        //TT56935 check lock is moved to BatchedClockProcessor class
        //int upd_after = 10000; //update lock after each 1000 records processed

        String currentClientId = SecurityService.getCurrentClientId();
        for (int k = 0; k < clients.size(); k ++) {
            int i = 0;
            List importDataList = (List) clients.get(k);
            ImportData importData = (ImportData) importDataList.get(0);
            // setting client ID for processing
            SecurityService.setCurrentClientId(importData.getField(ClockTransactionMapper.FIELD_CLIENT_ID));
            RuleAccess ra = new RuleAccess(conn);
            
            //Based on the task parameter AUTO_RECALC (Default = false)
            ra.setProcessLevelAutoRecalc(autoRecalc);
            logger.info("AUTO_RECALC: " + ra.getProcessLevelAutoRecalc());
            
            CodeMapper mapper = CodeMapper.createCodeMapper(conn);

            while (i < importDataList.size()) {
                int startIndex = i;
                int endIndex = startIndex + numClocksToProcessBeforeCalc;
                /* to prevent EmployeeScheduleAccess from clearing the entity cache prematurely,
                 * the clock processor chain execution is surrounded with code to set the 
                 * "ra.getCalcDataCache().setCreatedByRuleEngine(true)" */
				ra.getCalcDataCache().setCreatedByRuleEngine(true); 
                //clocksProcessed += batchedClockProcessor.processClocksToCalc(
                    //importDataList, startIndex, endIndex, ra, mapper, type);
                clocksProcessed += batchedClockProcessor.processClocksToCalc(
                    importDataList, startIndex, endIndex, ra, mapper, type);
                
		ra.getCalcDataCache().setCreatedByRuleEngine(false); 
                if (shouldCommit) {
                    conn.commit();
                }
                ra.getCalcDataCache().clear();
                i = endIndex;
                if ( isInterrupted() )
                {
                    k = i = Integer.MAX_VALUE;
                }
            }
          }
        // restore original client ID
        SecurityService.setCurrentClientId(currentClientId);

        return clocksProcessed;
    }

    /**
     * Returns number of days to calculate for each clock batch. Has to be less
     * then RuleAccess.REG_RULEENGINE_BATCH_PROCESS_SIZE since all summaries are loaded
     * by this task and loading more than this value will be too slow and inaccurate due to
     * the nature of batch processing in RuleAccess
     * @return NumClocksToProcessBeforeCalc
     */
    private int getNumClocksToProcessBeforeCalc() {
        int ret = WorkbrainParametersRetriever.getInt(WBPARM_NUM_CLOCKS_TO_PROCESS_BEFORE_CALC, 1);
        int maxSize = Registry.getVarInt(RuleAccess.REG_RULEENGINE_BATCH_PROCESS_SIZE ,
                                         RuleAccess.BATCH_PROCESS_SIZE_DEFAULT);
        if (ret > maxSize) {
            throw new RuntimeException(WBPARM_NUM_CLOCKS_TO_PROCESS_BEFORE_CALC
                + " registry value cannot exceed "
                + RuleAccess.REG_RULEENGINE_BATCH_PROCESS_SIZE + " registry value");
        }
        return ret;
    }

    private ImportData createImportData( ResultSet rs ) throws SQLException {
        ImportData importData = new ImportData();

        importData.setField( ClockTransactionMapper.FIELD_CLOCK_TYPE, rs.getString("CTPJ_TYPE") );
        importData.setField( ClockTransactionMapper.FIELD_READER_NAME, rs.getString("CTPJ_RDR_NAME") );
        importData.setField( ClockTransactionMapper.FIELD_IDENT_TYPE, rs.getString("CTPJ_IDENT_TYPE") );
        importData.setField( ClockTransactionMapper.FIELD_IDENTIFIER, rs.getString("CTPJ_IDENTIFIER") );
        importData.setField( ClockTransactionMapper.FIELD_TIME, rs.getString("CTPJ_TIME") );
        importData.setField( ClockTransactionMapper.FIELD_DEPT, rs.getString("CTPJ_DEPT") );
        importData.setField( ClockTransactionMapper.FIELD_JOB, rs.getString("CTPJ_JOB") );
        importData.setField( ClockTransactionMapper.FIELD_DOCKET, rs.getString("CTPJ_DKT") );
        importData.setField( ClockTransactionMapper.FIELD_PROJECT, rs.getString("CTPJ_PRJ") );
        importData.setField( ClockTransactionMapper.FIELD_TCODE, rs.getString("CTPJ_TCODE") );
        importData.setField( ClockTransactionMapper.FIELD_DKT_TIME, rs.getString("CTPJ_DKTTIME") );
        importData.setField( ClockTransactionMapper.FIELD_DKT_QTY, rs.getString("CTPJ_DKTQTY") );
        importData.setField( ClockTransactionMapper.FIELD_FINGER, rs.getString("CTPJ_FINGER") );
        importData.setField( ClockTransactionMapper.FIELD_QUALITY, rs.getString("CTPJ_QUALITY") );
        importData.setField( ClockTransactionMapper.FIELD_CONTENT, rs.getString("CTPJ_CONTENT") );
        importData.setField( ClockTransactionMapper.FIELD_TEMPLATE, rs.getString("CTPJ_TEMPLATE") );
        importData.setField( ClockTransactionMapper.FIELD_EXTRADATA, rs.getString("CTPJ_EXTRADATA") );
        importData.setField( ClockTransactionMapper.FIELD_CLK_ID, rs.getString("CTPJ_ID") );
        importData.setField( ClockTransactionMapper.FIELD_CLIENT_ID, rs.getString("CLIENT_ID") );

        return importData;
    }

    class ThreadedWBClockProcessTaskExt extends WBClockProcessTaskExt implements Runnable {
    	private final Logger logger = Logger.getLogger(ThreadedWBClockProcessTaskExt.class); 
    	
        // members for the master thread
        private WBClockProcessTaskExt parent;
        private Throwable exceptionFromSlaves;

        // members for the slave threads
        private final ThreadedWBClockProcessTaskExt master;
        private String sql;
        private int numClocksToProcessBeforeCalc;
        private int slaveNo;

        private DBConnection conn;

        public ThreadedWBClockProcessTaskExt(WBClockProcessTaskExt parent) {
            this.parent = parent;
            this.master = this;
            this.setTaskId(parent.getTaskId());
            this.setParentJob(parent);
            this.setClockTranPendJTbl(parent.getClockTranPendJTbl()); 
        }

        public ThreadedWBClockProcessTaskExt(ThreadedWBClockProcessTaskExt master,
                                          int slaveNo,
                                          String sql,
                                          int numClocksToProcessBeforeCalc) {
            this.master = master;
            this.slaveNo = slaveNo;
            this.sql = sql;
            this.numClocksToProcessBeforeCalc = numClocksToProcessBeforeCalc;
            this.setTaskId(master.getTaskId());
            this.setParentJob(master);
            this.setClockTranPendJTbl(master.getClockTranPendJTbl());
            
        }

        public void execute(DBConnection conn, int slaveThreadCount) throws
            WBInterfaceException, SQLException {

            this.conn = conn;
            List sqls = getDistinctCTPJIdSql(conn, slaveThreadCount + 1);
            
            if (sqls.isEmpty()) {
                return;
            }
            slaveThreadCount = sqls.size() - 1;
            Thread[] threadList = new Thread[slaveThreadCount];
            try {
                try {
                    numClocksToProcessBeforeCalc = parent.getNumClocksToProcessBeforeCalc();
                    
                    for (int i = 0; i < slaveThreadCount; i++) {
                        threadList[i] = new Thread(new ThreadedWBClockProcessTaskExt(this, i,
                                (String) sqls.get(i), numClocksToProcessBeforeCalc));
                            threadList[i].start();   
                    }
                    sql = (String) sqls.get(slaveThreadCount);
                    
                    List clients = loadClocks(conn);
                    if (clients.size() > 0) {
                        int clocksProcessed = processClocksFor(clients, conn);
                        synchronized (master) {
                            master.clockCountProcessed += clocksProcessed;
                        }
                    }
                }
                catch (Throwable t) {
                    // a few slave threads might be running, log the exception and wait
                    // for the slave threads
                    exceptionFromSlaves = t;
                }
                
                for (int i = 0; i < threadList.length; i++) {
                    try {
                        threadList[i].join();
                    } catch (InterruptedException e) {
                        //do nothing
                    }                        
                }    
                
                if (exceptionFromSlaves != null) {
                    throw new NestedRuntimeException(exceptionFromSlaves);
                }
            }            
            finally {
                parent.clockCountProcessed = clockCountProcessed;
            }
        }

        public void run() {
            Thread.currentThread().setName("WBClockProcessTaskExt-SLAVE" + slaveNo);
            try {
                this.conn = getDBConnection();
                
                List clients = loadClocks(conn);
                if (clients.size() > 0) {
                    int clocksProcessed = processClocksFor(clients, conn);
                    synchronized (master) {
                        master.clockCountProcessed += clocksProcessed;
                    }
                }
            }
            catch (Throwable t) {
                // the exception from the last failed thread will be thrown back
            	logger.error(t);
                master.exceptionFromSlaves = t;
            }
            finally {
                try {
                	SQLHelper.cleanUp(conn);
                }
                catch (Throwable t) {
                    master.exceptionFromSlaves = t;
                }
                
                if (logger.isDebugEnabled())
                    logger.debug(Thread.currentThread().getName() + "Completed.");                
                
            }
        }
        
        protected String getSelectorSql() {
            return getSelectorSql(conn);
        }

        protected String getSelectorSql(DBConnection conn) {
            return sql;
        }
        
        protected int getNumClocksToProcessBeforeCalc() {
            return numClocksToProcessBeforeCalc;
        }

        private List getDistinctCTPJIdSql(DBConnection conn, int setSize) throws
            SQLException {
            List sqls = new ArrayList();

            List idList = new ArrayList();
            String sql = "SELECT DISTINCT CTPJ_IDENTIFIER FROM " + parent.getClockTranPendJTbl() + " ORDER BY CTPJ_IDENTIFIER";

            ProxyStatement ps = null;
            try {
                ps = (ProxyStatement) conn.createStatement();
                ResultSet rs = ps.executeQuery(sql, true, true);
                while (rs.next()) {
                    idList.add(rs.getString(1));
                }
            }
            finally {
                if (ps != null) {
                    ps.close();
                }
            }

            setSize = Math.min(setSize, idList.size());
            
            if (setSize > 0) {
                int subSize = idList.size() / setSize;
                int more = idList.size() - subSize * setSize;
                int from = 0;
                for (int i = 0; i < setSize; i++) {
                    int subLength = subSize + (more-- > 0 ? 1 : 0);
                    String fromSql = (String) idList.get(from);
                    int to = from + subLength - 1;
                    String selector =
                        "SELECT * FROM " + parent.getClockTranPendJTbl() + " WHERE CTPJ_IDENTIFIER >= '" +
                        (String) idList.get(from) +
                        "' AND CTPJ_IDENTIFIER <= '" +
                        (String) idList.get(to) +
                        "' ORDER BY CLIENT_ID, CTPJ_IDENTIFIER, CTPJ_ID";
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("ThreadedWBClockProcessTaskExt" +
                                          i + ".select = " + selector);
                    sqls.add(selector);
                    from = to + 1;
                }
            }
            return sqls;
        }

        private DBConnection getDBConnection() throws SQLException {
            DBConnection conn = new DBConnection(ConnectionManager.
                                                 getConnection());
            conn.setAutoCommit(false);
            conn.turnTraceOff();
            return conn;
        }

    } // ThreadedWBClockProcessTaskExt


    public static void main(String[] args) throws Exception{

        System.setProperty("junit.db.username" , "workbrain");
        System.setProperty("junit.db.password" , "workbrain");
        System.setProperty("junit.db.url" , "jdbc:oracle:oci8:@rowb1devldv");
        System.setProperty("junit.db.driver" , "oracle.jdbc.driver.OracleDriver");

        final DBConnection c = com.workbrain.sql.SQLHelper.connectTo();
        c.setAutoCommit( false );
        com.workbrain.security.SecurityService.setCurrentClientId("1");
        WorkbrainSystem.bindDefault(
            new com.workbrain.sql.SQLSource () {
                public java.sql.Connection getConnection() throws SQLException {
                    return c;
                }
            }
        );

        WBClockProcessTaskExt task = new WBClockProcessTaskExt();
        task.execute(c);
    }

	public void setClockTranPendJTbl(String clockTranPendJTbl) {
		this.clockTranPendJTbl = clockTranPendJTbl;
	}

	public String getClockTranPendJTbl() {
		return clockTranPendJTbl;
	}

}