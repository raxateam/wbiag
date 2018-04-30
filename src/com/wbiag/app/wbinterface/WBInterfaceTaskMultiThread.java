package com.wbiag.app.wbinterface;

import java.sql.*;
import java.util.*;

import com.jamonapi.*;
import com.workbrain.app.scheduler.enterprise.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.security.*;
import com.workbrain.server.registry.*;
import com.workbrain.server.sql.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.util.monitor.*;

/**
 * Extended WBInterfacTask with MultiThreading option by grouping records based on a partitionColumnName
 */
public class WBInterfaceTaskMultiThread extends AbstractScheduledJob {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WBInterfaceTaskMultiThread.class);

    public static final String TRANSACTION_TYPE_PARAM_NAME = "transactionType";
    public static final String MNTR_WBINTTASK = "WBInterfaceTaskMultiThread";
    public static final String MNTR_PROCESSONEREC = "ProcessOneRecord";
    public static final String MNTR_PROCESSONEBATCH = "ProcessOneBatch";
    public static final String PARAM_PARTITION_COLUMN_NAME = "partitionColumnName";
    public static final String PARAM_THREAD_COUNT = "threadCount";
    public static final int THREAD_COUNT_DEFAULT = 1;
    protected static final String WBITYPNAME_PARAM_NAME = "wbitypName";
    protected static final List WBINT_FIELDS = Arrays.asList(ImportData.FIELDS);
    protected static final int DEFAULT_IMPORT_BATCH_SIZE = 1000;
    
    private HashMap tp = new HashMap();
    private boolean taskFailed = false;

    /**
     * Default constructor.
     */
    public WBInterfaceTaskMultiThread() {
    }

    /**
     * Task execution method.  Called by the scheduled task framework.
     *
     * @param taskID    Primary key of the scheduled task table (unused)
     * @param param     Map of parameters for the task.
     * @return  Status object indicating success/fail and a message
     * @throws Exception
     * @see com.workbrain.app.scheduler.ScheduledTask
     * @see com.workbrain.app.scheduler.ScheduledTask.Status
     */
    public Status run(int taskID, Map param) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("WBInterfaceTaskMultiThread.run(" + taskID + ", " + param + ")");
        }
        if (param == null) {
            throw new RuntimeException("Task Parameters cannot be null");
        }
        // must wrap params (a TreeMap implementation) with HashMap because of TransactionType public
        // interface expects a HashMap implementation. Sometimes, it's a TreeMap object which can't
        // be casted into HashMap.
        tp = new HashMap(param);
        DBConnection conn = null;

        try {
            conn = getConnection();
            conn.turnTraceOff();
            conn.setAutoCommit(false);

            SecurityService.setCurrentClientId("1");
            SecurityService.setUserNameActual("WORKBRAIN");
            SecurityService.setCurrentUser(SecurityService.createSystemUser());

            String transactionTypeStr = String.valueOf( tp.get( TRANSACTION_TYPE_PARAM_NAME ) );
            int transactionType = -1;
            try {
                transactionType = Integer.parseInt( transactionTypeStr );
            } catch(NumberFormatException nex) {
                if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) {
                    getLogger().error("com.wbiag.app.wbinterface.WBInterfaceTaskMultiThread.class", nex);
                }
            }
            if (transactionType <= 0) {
                return jobFailed("WBInterfaceTaskMultiThread task failed due to interface type not being specified.");
            }

            execute( conn, transactionType );
            if (!isInterrupted()){
                conn.commit();
                if (taskFailed) {
                    return jobFailed("WBInterfaceTaskMultiThread task failed. ");
                } else {
                    return jobOk( "WBInterfaceTaskMultiThread task finished successfully. ");
                }
            }
            else{
                conn.rollback();
                return jobInterrupted("WBInterfaceTaskMultiThread task has been interrupted.");
            }
        } catch (Exception e) {
            if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) { getLogger().error("com.workbrain.app.wbinterface.WBInterfaceTaskMultiThread.class", e);}
            if( conn != null ) conn.rollback();
            throw e;
        } finally {
            releaseConnection();
        }
    }

    /**
     *
     * @return  URI of custom parameters page
     * @see com.workbrain.app.scheduler.ScheduledTask
     */
    public String getTaskUI() {
        return "/jobs/wbiag/wbintParamsMultiThread.jsp";
    }

    /**
    * Executes the given transaction iff it is pending.
    *
    * @param conn       database connection
    * @param typeID     transactionId
    * @return int number of transactions processed
    * @throws SQLException
    */
    public int executeByTransactionId( DBConnection conn, int transactionId) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("WBInterfaceTaskMultiThread.executeByTransactionId(" + transactionId + ")");
        }

        List transactionList = new TransactionAccess(conn).loadPendingByTransactionId(transactionId);
        int numTransactions = execute(conn , transactionList);
        if (logger.isDebugEnabled()) {
            logger.debug("Processed " + numTransactions + " transactions for transaction id" + transactionId);
        }
        return numTransactions;
    }

    /**
    * Start the import process, loads all pending transaction and then starts matching
    * them with appropriate transaction types
    *
    * @param conn       database connection
    * @param typeID     task type identifier
    * @return int number of transactions processed
    * @throws SQLException
    */
    public int execute( DBConnection conn, int typeID ) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("WBInterfaceTaskMultiThread.execute(" + typeID + ")");
        }

        // *** get the first pending transaction
        List transactionList = new TransactionAccess(conn).loadPendingByTypeIdWithFetchSize(typeID , 1);
        int numTransactions = 0;
        while (transactionList != null && transactionList.size() > 0) {
            numTransactions += execute(conn , transactionList);
            transactionList = new TransactionAccess(conn).loadPendingByTypeIdWithFetchSize(typeID , 1);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Processed " + numTransactions + " transactions for type id " +  typeID);
        }
        return numTransactions;
    }

    /**
    * Processes all pending transactions in transactionList
    *
    * @param conn       database connection
    * @param typeID     task type identifier
    * @throws SQLException
    */
    private int execute( DBConnection conn , List transactionList ) throws SQLException {
        if (transactionList == null || transactionList.size() == 0) {
            logError("No pending transaction to execute.");
            return 0;
        }

        // save current client id
    	int numTransactions = 0;
        String currentClientId = SecurityService.getCurrentClientId();
        try {
        	int threadCount = THREAD_COUNT_DEFAULT;
        	String sThread = (String)tp.get(PARAM_THREAD_COUNT);
        	if (!StringHelper.isEmpty(sThread)) {
        		threadCount = Integer.parseInt(sThread) ;
        	}
        	String partitionColumnName = (String) this.tp.get(WBInterfaceTaskMultiThread.PARAM_PARTITION_COLUMN_NAME);
        	if (StringHelper.isEmpty(partitionColumnName)) {
        		throw new RuntimeException ("Partition column must be supplied");
        	}
        	if (!WBInterfaceTaskMultiThread.WBINT_FIELDS.contains(partitionColumnName.toUpperCase().trim())) {
        		throw new RuntimeException ("Partition column name must be one of wbint_import fields");
        	}

        	Iterator it = transactionList.iterator();
        	while (it.hasNext()) {
        		if (isInterrupted())
        			break ;

        		TransactionData td = (TransactionData) it.next();
        		numTransactions++;

        		String clientIdForTran = String.valueOf( td.retrieveClientId() );
            	SecurityService.setCurrentClientId(clientIdForTran);

            	int typeId = td.retrieveWbitypId();
            	int transactionId = td.getWbitranId() ;
            	if (logger.isDebugEnabled()) {
            		logger.debug("WBInterfaceTaskMultiThread executing WBITRAN, " + transactionId);
            	}

            	//*** put wbitypname in map for further reference
            	tp.put(WBITYPNAME_PARAM_NAME , td.getWbitypName());
            	TransactionType type = getTransactionType(conn,typeId); // just testing if its registered.
            	java.util.Date startDate = new java.util.Date();
            	if( type == null ) {
            		updateTransaction( conn, transactionId, TransactionType.FAILURE, "No transaction type registered for " + typeId, startDate , startDate);
            		continue;
            	}

            	try {
            		setTransactionInProgress( conn, transactionId, startDate );

            		ArrayList listOfPartitionRowValues = getListOfImportRows(conn, transactionId, partitionColumnName);

                	if (listOfPartitionRowValues.size() == 0 ||
                			(listOfPartitionRowValues.size() == 1 && "null".equalsIgnoreCase((String) listOfPartitionRowValues.get(0))) ||
                			(listOfPartitionRowValues.size() == 1 && StringHelper.isEmpty((String) listOfPartitionRowValues.get(0)))) {
                		updateTransaction( conn, transactionId, TransactionType.FAILURE, "No value was found in column " + partitionColumnName, startDate , startDate);
                		continue;
                	}

            		// create and kick off the threaded transactions
                	ThreadGroup wbIntThreadGrp = new ThreadGroup("WBInterfaceTaskMultiThread thread group");
            		ThreadedTransaction[] threadedRuns = new ThreadedTransaction[threadCount];
            		for (int threadIdx=0; threadIdx < threadCount; threadIdx++) {
            			type = getTransactionType(conn,typeId);
            			type.setTransactionParameters( tp );
            			threadedRuns[threadIdx] = new ThreadedTransaction (this, transactionId, type, (Map)tp.clone(), clientIdForTran, threadIdx, threadCount, listOfPartitionRowValues, partitionColumnName);
            			String threadName = td.getWbitypName() +  "-thread-" + threadIdx;
            			new Thread(wbIntThreadGrp, threadedRuns[threadIdx], threadName).start();
            			if (isInterrupted())
            				break;
            		}

            		/* committing this dbConn to avoid our WB timeout exception,
            		 * individual transactionType are running under separate dbConn.
            		 */
            		conn.commit();

            		// wait on the threadGrp's active count to go down to zero
            		// (might be better to use join??
            		while (wbIntThreadGrp.activeCount() > 0) {
            			try {
            				Thread.sleep(30000); //sleep 30 sec
            			}
            			catch (InterruptedException ie) {
            				logger.error(ie);
            			}
            			if (logger.isDebugEnabled()) {
            				logger.debug("Active Thread Count : " + wbIntThreadGrp.activeCount());
            			}
            			if (isInterrupted())
            				break;
            		}

            		// all ThreadedTransactions are done, check the status
            		String status = "";
            		String msg = "";
            		for (int i=0; i < threadedRuns.length && !taskFailed; i++) {
            			status = threadedRuns[i].getStatus();
            			if (status.equalsIgnoreCase(TransactionType.FAILURE)) {
            				msg = "At least one of the import records is in ERROR status";
            				taskFailed = true;
            			}
            		}

            		if (isInterrupted())
            			break ;

            		updateTransaction( conn, transactionId, status, msg, startDate , new java.util.Date());
                    conn.commit();
            	} catch( Throwable t ) {
            		// catch all Throwable object including Runtime exception or error
            		// to complete the transaction record
            		if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) {
            			getLogger().error("com.workbrain.app.wbinterface.WBInterfaceTaskMultiThread.class", t);
            		}
            		conn.rollback();
            		updateTransaction( conn, transactionId,TransactionType.FAILURE, StringHelper.getStackTrace( t ), startDate , new java.util.Date());
            	}
        	}
    	} finally {
    		// restore current client id
    		SecurityService.setCurrentClientId(currentClientId);
    	}

        return numTransactions;
    }

    /**
     * @throws WBInterfaceException
     */
    protected ArrayList getListOfImportRows(DBConnection conn, int transId, String partitionColumnName) throws WBInterfaceException {
    	String sql = "SELECT DISTINCT " + partitionColumnName + " FROM WBINT_IMPORT WHERE WBITRAN_ID = ?";
        ArrayList list = new ArrayList();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
        	stmt = conn.prepareStatement(sql);
        	stmt.setInt( 1, transId );
            rs = stmt.executeQuery();

            while( rs.next() ) {
            	list.add(rs.getString(1));
            }
        } catch ( SQLException ex ) {
        	throw new WBInterfaceException(ex);
        } finally {
        	SQLHelper.cleanUp(stmt, rs);
        }
        return list;
    }

    /**
     * Updates the transaction record in the database
     */
    private void updateTransaction( DBConnection conn, int transactionId, String status, String message, java.util.Date startDate , java.util.Date endDate) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("updateTransaction(" + transactionId + "," + status + ",'" + message + "')");
        }
        if( transactionId < 0 ) {
        	return;
        }
        new TransactionAccess(conn).save(transactionId, status, message, startDate, endDate);
        conn.commit();
    }

    /**
     * Updates the transaction record in the database with 'IN PROGRESS' status
     */
    private void setTransactionInProgress( DBConnection conn, int transactionId,
            java.util.Date startDate ) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("setTransactionInProgress(" + transactionId + "')");
        }
        if( transactionId < 0 ) return;
        new TransactionAccess(conn).setTransactionInProgress(transactionId, startDate);
        conn.commit();
    }


    /**
     * always return a newly instantiated TransactionType for multi-threading purpose.
     */
    private TransactionType getTransactionType(DBConnection conn, int typeId) throws SQLException {
        TransactionType type = new TypeAccess(conn).getTransactionType(typeId);
        return type;
    }

    /**
     * DO NOT USE - this is a backdoor for testing only
     *
     * @param param map of parameters
     */
    public void setTransactionParam( HashMap param ) {
        if (param == null) {
            throw new RuntimeException("Task Parameters cannot be null");
        }
        tp = param;
    }

    public static org.apache.log4j.Logger getLogger() {
        return logger;
    }

    public static void main( String[] args ) throws Exception {

        final DBConnection c = com.workbrain.sql.SQLHelper.connectToMaggie();
        c.setAutoCommit( false );
        c.turnTraceOff();

        SecurityService.setCurrentClientId("1");
        SecurityService.setUserNameActual("WORKBRAIN");
        SecurityService.setCurrentUser(SecurityService.createSystemUser());

        WorkbrainSystem.bindDefault(
            new com.workbrain.sql.SQLSource () {
                public java.sql.Connection getConnection() throws SQLException {
                    return c;
                }
            }
        );

        WBInterfaceTaskMultiThread task = new WBInterfaceTaskMultiThread();
        HashMap param = new HashMap();
        param.put( TRANSACTION_TYPE_PARAM_NAME, new Integer( 60 ) );
        task.setTransactionParam( param );
        task.execute( c, 60 );
        c.commit();
    }
}

class ThreadedTransaction implements ScheduledJob, Runnable {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ThreadedTransaction.class);

    private AbstractScheduledJob scheduledJob; // a delegate, all access should be synchronized.
    private Map tranParam;
    private int threadIdx;
    private int threadCount;
    private String status = "";
    private TransactionType tranType;
    private int tranId;
    private String clientIdForTran;
    private final ArrayList partitionRowValues;
    private String partitionColumnName;


    public ThreadedTransaction(AbstractScheduledJob scheduledJob,
                               int transactionId,
                               TransactionType type,
                               Map transactionParam,
                               String clientId,
                               int threadIdx,
                               int threadCount,
                               ArrayList partitionRowValues,
                               String partitionColumnName) {
        this.scheduledJob = scheduledJob;
        this.tranParam = transactionParam;
        this.threadIdx = threadIdx;
        this.threadCount = threadCount;
        this.tranType = type;
        this.tranId = transactionId;
        this.clientIdForTran = clientId;
        this.partitionRowValues = partitionRowValues;
        this.partitionColumnName = partitionColumnName;
    }

    public void run() {
        if (logger.isDebugEnabled()) {
        	logger.debug("Thread " + threadIdx + " Started.");
        }

    	DBConnection conn = null;
        String currClientId = SecurityService.getCurrentClientId();
        try {
            // each thread should run under a separate connection.
            conn = new DBConnection(ConnectionManager.getConnection());
            conn.turnTraceOff();

            SecurityService.setCurrentClientId(this.clientIdForTran);
            SecurityService.setUserNameActual("WORKBRAIN");
            SecurityService.setCurrentUser(SecurityService.createSystemUser());

            this.tranType.initializeTransaction(conn);

            if (isInterrupted())
                return;

            this.status = processTransaction(conn);

            if (isInterrupted())
                return;

            this.tranType.finalizeTransaction(conn);

            if (logger.isDebugEnabled()) logger.debug("ENDED.");
            conn.commit();
        }
        catch (WBInterfaceException wbIE) {
            logger.error(wbIE);
            try {
                conn.rollback();
            }
            catch (SQLException e) {
                logger.error(e);
            }
        }
        catch (SQLException sqlE) {
            logger.error(sqlE);
            try {
                conn.rollback();
            }
            catch (SQLException e) {
                logger.error(e);
            }
        }
        catch (Exception e) {
            logger.error(e);
            try {
                conn.rollback();
            }
            catch (SQLException sqlE) {
                logger.error(sqlE);
            }
        }
        finally {
            SQLHelper.cleanUp(conn);
            SecurityService.setCurrentClientId(currClientId);

            if (logger.isDebugEnabled()) {
            	logger.debug("Thread " + threadIdx + " Finished.");
            }
        }
    }

    private String processTransaction(DBConnection conn) throws WBInterfaceException, SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("WBInterfaceTaskMultiThread.processTransaction(" + this.tranId + "," + this.tranType + ") Started.");
        }

        String status = TransactionType.SUCCESS;
        while (true) {
        	List importList = getImportList(conn);
        	if (logger.isDebugEnabled()) {
        		logger.debug("picked up " + importList.size() + " records.");
        	}

        	if( importList.size() == 0 ) {
        		break;
        	}

        	if (isInterrupted())
        		return status;

        	String thisStatus = null;

        	if (this.tranType instanceof TransactionTypeBatchInterface) {
        		thisStatus = processTransactionBatch(conn, importList, (TransactionTypeBatchInterface) this.tranType, (HashMap)this.tranParam);
        	}
        	else if (this.tranType instanceof TransactionType) {
        		thisStatus = processTransaction(conn, importList, this.tranType);
        	}

        	// *** update status if not already failure
        	if (!TransactionType.FAILURE.equals(status) && !StringHelper.isEmpty(thisStatus)) {
        		status = thisStatus;
        	}

        	if (logger.isDebugEnabled()) {
        		logger.debug("WBInterfaceTaskMultiThread.processTransaction(" + this.tranId + "," + this.tranType + ") Finished.");
        	}

        }

        return status;
    }

    /**
     */
    private List getImportList( DBConnection conn) throws WBInterfaceException , SQLException {
    	if (logger.isDebugEnabled()) {
             logger.debug("WBInterfaceTaskMultiThread.getImportList Start.");
        }

    	int fetchSize = Registry.getVarInt("system/interfaces/settings/ImportFetchSize", WBInterfaceTaskMultiThread.DEFAULT_IMPORT_BATCH_SIZE);
    	
    	if (logger.isDebugEnabled()) {
            logger.debug("fetchSize :" + fetchSize);
       }
    	
    	ImportAccess iaccess = new ImportAccess(conn);
        List ret = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM WBINT_IMPORT WHERE " + partitionColumnName + " IN (";
            int numToProcess = partitionRowValues.size() < threadCount ? partitionRowValues.size() : (partitionRowValues.size() / threadCount);
            int endIdx = numToProcess * (threadIdx + 1);
            if (partitionRowValues.size() < threadCount) {
            	if(threadIdx >= partitionRowValues.size()){
            	    return ret;
            	}
            }
            int remain = partitionRowValues.size() % threadCount;
            if (remain != 0 && threadIdx == threadCount -1){
            	endIdx = endIdx + remain;
            }
            for(int i = numToProcess * threadIdx; i < endIdx; ++i) {
            	sql += "?";
            	if(i != (endIdx - 1)) {
            		sql += ",";
            	}
            }

            sql += ") AND WBIMP_STATUS = '" + ImportData.STATUS_PENDING +"' AND WBITRAN_ID = " + this.tranId;

            ps = conn.prepareStatement(sql);
            ps.setFetchSize(fetchSize);
            
            for(int i = numToProcess * threadIdx, j = 1; i < endIdx; ++i, ++j) {
            	ps.setString(j, (String) partitionRowValues.get(i));
            }

            int count = 0;
            rs = ps.executeQuery();
            while (rs.next() && count < fetchSize ) {
            	ImportData data = new ImportData();
                iaccess.mapToData(data , rs);
                ret.add(data);
                count++;
            }
        }
        finally {
            if (ps != null) ps.close();
            if (rs != null) rs.close();
        }
        
        if (logger.isDebugEnabled()) {
        	logger.debug("WBInterfaceTaskMultiThread.getImportList.size :" + ret.size());
            logger.debug("WBInterfaceTaskMultiThread.getImportList end.");
       }
        
        return ret;
    }

    private String processTransaction(DBConnection conn, List importList, TransactionType type) throws WBInterfaceException, SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("processTransaction() Start");
        }

        String status = TransactionType.SUCCESS;
        if (importList.size() == 0) {
            return status;
        }

        Iterator it = importList.iterator();
        while (it.hasNext()) {
            if (isInterrupted()) {
                break;
            }

            ImportData td = (ImportData) it.next();
            Monitor processMonitor = null;
            try {
                if (WBMonitorFactory.isMonitorEnabled()) {
                    processMonitor = WBMonitorFactory.start(WBInterfaceTaskMultiThread.MNTR_WBINTTASK + "." + this.tranParam.get(WBInterfaceTaskMultiThread.WBITYPNAME_PARAM_NAME) + "." + WBInterfaceTaskMultiThread.MNTR_PROCESSONEREC);
                }

                type.process(td, conn);

                /**
                 * the error is swallowed by the transaction, need to rollback
                 * as well although this is not the preferred practice
                 */
                if (ImportData.STATUS_ERROR.equalsIgnoreCase(type.getStatus())) {
                    status = TransactionType.FAILURE;
                    conn.rollback();
                }
                updateImport(conn, td.getId(), type.getStatus(), type.getMessage());
            }
            catch (WBInterfaceException e) {
                logger.error("updateImport() failed, persisting the failed transaction", e);

                // get the status and message from type for
                // WBInterfaceExceptions
                conn.rollback();
                updateImport(conn, td.getId(), type.getStatus(), type.getMessage());
                status = TransactionType.FAILURE;
            }
            catch (Throwable t) {
                logger.error("updateImport() failed, persisting the failed transaction", t);

                conn.rollback();
                updateImport(conn, td.getId(), ImportData.STATUS_ERROR, StringHelper.getStackTrace(t));
                status = TransactionType.FAILURE;
            }
            finally {
                type.reset();
                if (processMonitor != null)
                    processMonitor.stop();
            }
            conn.commit();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("processTransaction() End");
        }
        return status;
    }

    private String processTransactionBatch(DBConnection conn, List importList, TransactionTypeBatchInterface type, HashMap tp) throws WBInterfaceException, SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("processTransactionBatch() Start");
        }

        String status = TransactionType.SUCCESS;
        if (importList.size() == 0) {
            return status;
        }

        int batchProcessSize = type.getBatchProcessSize(tp);
        
        if (logger.isDebugEnabled()) {
            logger.debug("WBInterfaceTaskMultiThread.processTransactionBatch.batchProcessSize :" + batchProcessSize);
        }
        
        int batchStartInd = 0;
        int batchEndInd = Math.min(batchStartInd + batchProcessSize, importList.size());
        while (batchStartInd < batchEndInd) {
            if (isInterrupted()) {
                break;
            }

            List listBatch = importList.subList(batchStartInd, batchEndInd);
            
            if (logger.isDebugEnabled()) {
                logger.debug("WBInterfaceTaskMultiThread.processTransactionBatch.batchStartInd :" + batchStartInd);
                logger.debug("WBInterfaceTaskMultiThread.processTransactionBatch.batchEndInd :" + batchEndInd);
                logger.debug("WBInterfaceTaskMultiThread.processTransactionBatch.listBatch.size :" + listBatch.size());
            }
            
            Iterator it = listBatch.iterator();
            while (it.hasNext()) {
                ImportData td = (ImportData) it.next();
                try {
                    type.process(td, conn);
                    td.setStatus(type.getStatus());
                    td.setMessage(type.getMessage());
                }
                catch (Throwable t) {
                    logger.error("updateImport() failed, persisting the failed transaction", t);
                    td.setStatus(type.getStatus());
                    td.setMessage(type.getMessage());
                    status = TransactionType.FAILURE;
                }
                finally {
                    type.reset();
                }
            }

            if (isInterrupted()) {
                break;
            }

            Monitor processMonitor = null;
            try {
                if (WBMonitorFactory.isMonitorEnabled()) {
                    processMonitor = WBMonitorFactory.start(WBInterfaceTaskMultiThread.MNTR_WBINTTASK + "." + tp.get(WBInterfaceTaskMultiThread.WBITYPNAME_PARAM_NAME) + "." + WBInterfaceTaskMultiThread.MNTR_PROCESSONEBATCH);
                }

                // *** update all imports based on each process
                ImportAccess.updateImport(conn, listBatch);
                // *** processBatch returns list of import data that have been
                // updated
                // *** during batch processing
                List updatedImports = type.processBatch(conn, listBatch);
                if (updatedImports != null && updatedImports.size() > 0) {
                    ImportAccess.updateImport(conn, updatedImports);
                    // *** if any of updatedImports are in ERROR, transaction is
                    // failure
                    Iterator iter = updatedImports.iterator();
                    while (iter.hasNext()) {
                        ImportData item = (ImportData) iter.next();
                        if (item.isError()) {
                            status = TransactionType.FAILURE;
                        }
                    }
                }
                conn.commit();
            }
            catch (Throwable t) {
                logger.error("Batch Process failed", t);
                conn.rollback();
                // *** if any error occurs during batch processing, all import data
                // *** has to be marked as error
                ImportAccess.updateImportError(conn, listBatch, StringHelper.getStackTrace(t));
                conn.commit();
                status = TransactionType.FAILURE;
            }
            finally {
                type.resetBatch();
                if (processMonitor != null)
                    processMonitor.stop();
                batchStartInd += batchProcessSize;
                batchEndInd = Math.min(batchStartInd + batchProcessSize, importList.size());
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("processTransactionBatch() End");
        }
        return status;
    }


    /**
     * Updates the import record in the database
     */
    private void updateImport( DBConnection conn, int id, String status, String message) throws SQLException {
        new ImportAccess(conn).updateImport(id , status , message);
        conn.commit();
    }

    public String getStatus() {
        return this.status;
    }

    ///----------------- delegated methods ---------------------
    public Status run(int taskID, Map param) throws Exception {
        throw new Exception("run(int taskID, Map param) should not be called directly on ThreadedTransaction.");
    }

    public boolean isInterrupted() throws SQLException {
        synchronized (this.scheduledJob) {
            return this.scheduledJob.isInterrupted();
        }
    }

    public void updateLock() {
        synchronized (this.scheduledJob) {
            this.scheduledJob.updateLock();
        }
    }

    public String getTaskUI() {
        throw new RuntimeException("getTaskUI() should not be called directly on ThreadedTransaction.");
    }
}