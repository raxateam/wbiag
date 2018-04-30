package com.wbiag.app.wbinterface;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.naming.*;

import com.workbrain.util.*;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.sql.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.server.registry.*;
import com.workbrain.security.SecurityService;
import com.jamonapi.*;
import com.workbrain.util.monitor.*;
import com.workbrain.app.wbinterface.*;

/**
 * Extenstion to Core WBInterfaceTask
 *   * allows defining ImportFetchSize on wbint_type.udf2. If not defined there, it will use the registry
 */
public class WBInterfaceTaskExt extends AbstractScheduledJob {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WBInterfaceTaskExt.class);

    public static final String TRANSACTION_TYPE_PARAM_NAME = "transactionType";
    private static final String WBITYPNAME_PARAM_NAME = "wbitypName";
    public static final String MNTR_WBINTTASK = "WBInterfaceTaskExt";
    public static final String MNTR_PROCESSONEREC = "ProcessOneRecord";
    public static final String MNTR_PROCESSONEBATCH = "ProcessOneBatch";

    private static final int DEFAULT_IMPORT_BATCH_SIZE = 1000;
    private HashMap types = new HashMap();
    private HashMap tp = new HashMap();
    private boolean taskFailed = false;
    private int lastProcessedId = Integer.MIN_VALUE;

    /**
     * Default constructor.
     */
    public WBInterfaceTaskExt() {
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
            logger.debug("WBInterfaceTaskExt.run(" + taskID + ", " + param + ")");
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

            String transactionTypeStr = String.valueOf( tp.get( TRANSACTION_TYPE_PARAM_NAME ) );
            int transactionType = -1;
            try {
                transactionType = Integer.parseInt( transactionTypeStr );
            } catch(NumberFormatException nex) {
                if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) {
                    getLogger().error("com.workbrain.app.wbinterfaceWBInterfaceTaskExt.class", nex);
                }
            }
            if (transactionType <= 0) {
                return jobFailed("WBInterfaceTaskExt task failed due to interface type not being specified.");
            }
            execute( conn, transactionType );
            if (!isInterrupted()){
                conn.commit();
                if (taskFailed) {
                    return jobFailed("WBInterfaceTaskExt task failed. ");
                } else {
                    return jobOk( "WBInterfaceTaskExt task finished successfully. ");
                }
            }
            else{
                conn.rollback();
                return jobInterrupted("WBInterfaceTaskExt task has been interrupted.");
            }
        } catch (Exception e) {
            logger.error("com.workbrain.app.wbinterfaceWBInterfaceTaskExt.class", e);
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
        return "/jobs/wbinterface/wbintParams.jsp";
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
            logger.debug("WBInterfaceTaskExt.executeByTransactionId(" + transactionId + ")");
        }

        List transactionList = new TransactionAccess(conn).loadPendingByTransactionId(transactionId);
        int numTransactions = execute(conn , transactionList);
        if (logger.isDebugEnabled()) {
            logger.debug("Processed " + numTransactions +
                    " transactions for transaction id" + transactionId);
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
            logger.debug("WBInterfaceTaskExt.execute(" + typeID + ")");
        }

        // *** get the first pending transaction
        TransactionAccess transactionAccess = new TransactionAccess( conn );
        List transactionList = transactionAccess.loadPendingByTypeIdWithFetchSize(typeID , 1);
        int numTransactions = 0;
        while (transactionList != null && transactionList.size() > 0) {
            numTransactions += execute(conn , transactionList);
            transactionList = transactionAccess.loadPendingByTypeIdWithFetchSize(typeID , 1);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Processed " + numTransactions +
                    " transactions for type id " +  typeID);
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
        Iterator it = transactionList.iterator();
        int numTransactions = 0;
        // save current client id
        String currentClientId = SecurityService.getCurrentClientId();

        while (it.hasNext()) {
            if (isInterrupted())
                break ;
            TransactionData td = (TransactionData) it.next();
            numTransactions++;
            SecurityService.setCurrentClientId(String.valueOf( td.retrieveClientId()));
            int id = td.getWbitranId() ;
            if (logger.isDebugEnabled()) {
                logger.debug("WBInterfaceTaskExt executing WBITRAN, " + id);
            }
            int typeId = td.retrieveWbitypId();
            //*** put wbitypname in map for further reference
            tp.put(WBITYPNAME_PARAM_NAME , td.getWbitypName());
            TransactionType type = getTransactionType(conn,typeId);
            java.util.Date startDate = new java.util.Date();
            if( type == null ) {
              updateTransaction( conn, id, TransactionType.FAILURE,
                      "No transaction type registered for " + typeId,
                       startDate , startDate);
            } else {
                try {
                    setTransactionInProgress( conn, id, startDate );
                    type.setTransactionParameters( tp );
                    type.initializeTransaction(conn);
                    String status = processTransaction( conn, id, type , tp);
                    String msg = "";
                    if (status.equalsIgnoreCase(TransactionType.FAILURE)) {
                        msg = "At least one of the import records is in ERROR status";
                        taskFailed = true;
                    }
                    if (isInterrupted()) {
                        setTransactionInterrupted( conn, id, startDate );
                        break ;
                    }
                    try {
                        type.finalizeTransaction(conn);
                    } catch(Exception e) {
                        logError(e.getMessage(), e);
                        throw e;
                    }
                    updateTransaction( conn, id, status,
                            msg, startDate , new java.util.Date());
                    conn.commit();
                } catch (WBInterfaceException e) {
                    // catch all Throwable object including Runtime exception or error
                    // to complete the transaction record
                    if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) { getLogger().error("com.workbrain.app.wbinterface.WBInterfaceTaskExt.class", e);}
                    conn.rollback();
                    taskFailed = true;
                    updateTransaction( conn, id,TransactionType.FAILURE,
                            e.getMessage(), startDate , new java.util.Date());
                } catch( Throwable t ) {
                    // catch all Throwable object including Runtime exception or error
                    // to complete the transaction record
                    if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) { getLogger().error("com.workbrain.app.wbinterface.WBInterfaceTaskExt.class", t);}
                    conn.rollback();
                    taskFailed = true;
                    updateTransaction( conn, id,TransactionType.FAILURE,
                            StringHelper.getStackTrace( t ), startDate , new java.util.Date());
                }
            }
        }
        // restore current client id
        SecurityService.setCurrentClientId(currentClientId);

        return numTransactions;
    }


    private String processTransaction( DBConnection conn,
                                       int transactionId,
                                       TransactionType type,
                                       HashMap tp)
            throws WBInterfaceException, SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("WBInterfaceTaskExt.processTransaction(" +
                    transactionId + "," + type + ")");
        }

        String status = TransactionType.SUCCESS;
        List importList = null;

        boolean foundOneRecord = true;

        while( foundOneRecord ) {
            foundOneRecord = false;
            importList = getImportListWithFetchSize( conn, transactionId, tp );
            foundOneRecord = importList.size() > 0;

            if ( isInterrupted() )
                break;

            String thisStatus = null;
            if (type instanceof TransactionTypeBatch) {
                thisStatus = processTransactionBatch(conn , importList,
                    (TransactionTypeBatch)type,
                    tp);
            }
            else if (type instanceof TransactionType) {
                thisStatus = processTransaction(conn , importList, type);
            }
            // *** update status if not already failure
            if (!TransactionType.FAILURE.equals(status)
                && !StringHelper.isEmpty(thisStatus)) {
                status = thisStatus;

                // if the status is not already set to failure
                // check for any failed rows from a previously
                // interrupted run

                if(!status.equals(TransactionType.FAILURE) &&
                   getImportFailedCount(conn, transactionId) > 0) {
                    status = TransactionType.FAILURE;
                }

            }
            if ( foundOneRecord )
                lastProcessedId = ((ImportData) importList.get( importList.size()-1 )).getId();
        }
        return status;
    }

    private String processTransaction(DBConnection conn , List importList,
                                      TransactionType type)
        throws WBInterfaceException, SQLException {

        if (logger.isDebugEnabled()) {
            logger.debug("Running transaction type");
        }
        String status = TransactionType.SUCCESS;
        if (importList.size() == 0) {
            return status;
        }

        Iterator it = importList.iterator();
        while (it.hasNext()) {
            if ( isInterrupted() )
                break;
            ImportData td = (ImportData) it.next();
            Monitor processMonitor = null;
            try {
                if (WBMonitorFactory.isMonitorEnabled()) processMonitor = WBMonitorFactory.start(MNTR_WBINTTASK + "." + tp.get(WBITYPNAME_PARAM_NAME) + "." + MNTR_PROCESSONEREC);
                type.process(td, conn);
                /**
                 * the error is swallowed by the transaction, need to rollback as well
                 * although this is not the preferred practice
                 */
                if (ImportData.STATUS_ERROR.equalsIgnoreCase(type.getStatus())) {
                    status = TransactionType.FAILURE;
                    conn.rollback();
                }
                updateImport(conn, td.getId(), type.getStatus(), type.getMessage());
            }
            catch (WBInterfaceException e) {
                logError("updateImport() failed, persisting the failed transaction", e);
                // get the status and message from type for WBInterfaceExceptions
                conn.rollback();
                updateImport(conn, td.getId(), type.getStatus(), type.getMessage());
                status = TransactionType.FAILURE;
            }
            catch (Throwable t) {
                logError("updateImport() failed, persisting the failed transaction", t);
                conn.rollback();
                updateImport(conn, td.getId(), ImportData.STATUS_ERROR, StringHelper.getStackTrace(t));
                status = TransactionType.FAILURE;
            }
            finally {
                type.reset();
                if (processMonitor!= null) processMonitor.stop();
            }
            conn.commit();
        }
        return status;
    }

    private String processTransactionBatch( DBConnection conn,
                                            List importList,
                                            TransactionTypeBatch type,
                                            HashMap tp)
            throws WBInterfaceException, SQLException {

        if (logger.isDebugEnabled()) {
            logger.debug("Running transaction type batch");
        }
        String status = TransactionType.SUCCESS;
        if (importList.size() == 0) {
            return status;
        }

        int batchProcessSize = type.getBatchProcessSize(tp);
        int batchStartInd = 0;
        int batchEndInd = Math.min(batchStartInd + batchProcessSize , importList.size());
        while (batchStartInd < batchEndInd) {
            List listBatch = importList.subList(batchStartInd, batchEndInd);
            Iterator it = listBatch.iterator();
            while (it.hasNext()) {
               if ( isInterrupted() )
                   break;
               ImportData td = (ImportData) it.next();
               try {
                    type.process( td, conn );
                    td.setStatus(type.getStatus());
                    td.setMessage(type.getMessage());
                } catch( Throwable t ) {
                    logError("updateImport() failed, persisting the failed transaction", t);
                    td.setStatus(type.getStatus());
                    td.setMessage(type.getMessage());
                    status = TransactionType.FAILURE;
                } finally {
                    type.reset();
                }
            }
            if ( isInterrupted() )
                break;
            Monitor processMonitor = null;
            try {
                if (WBMonitorFactory.isMonitorEnabled()) processMonitor = WBMonitorFactory.start(MNTR_WBINTTASK + "." + tp.get(WBITYPNAME_PARAM_NAME) + "." + MNTR_PROCESSONEBATCH );
                // *** update all imports based on each process
                ImportAccess.updateImport(conn , listBatch);
                // *** processBatch returns list of import data that have been updated
                // *** during batch processing
                List updatedImports = type.processBatch(conn , listBatch);
                if (updatedImports != null && updatedImports.size() > 0) {
                    ImportAccess.updateImport(conn, updatedImports);
                    // *** if any of updatedImports are in ERROR, transaction is failure
                    Iterator iter = updatedImports.iterator();
                    while (iter.hasNext()) {
                        ImportData item = (ImportData) iter.next();
                        if (item.isError()) {
                            status = TransactionType.FAILURE;
                        }
                    }
                }
                conn.commit();
            } catch( Throwable t ) {
                logError("Batch Process failed", t);
                conn.rollback();
                // *** if any error occurs during batch processing, all import data
                // *** has to be marked as error
                ImportAccess.updateImportError(conn , listBatch , StringHelper.getStackTrace(t));
                conn.commit();
                status = TransactionType.FAILURE;
            } finally {
                type.resetBatch();
                if (processMonitor!= null) processMonitor.stop();
                batchStartInd += batchProcessSize;
                batchEndInd = Math.min(batchStartInd + batchProcessSize , importList.size());
            }
        }

        return status;
    }

    /**
     * Returns import records based on fetch size defined on transaction type if defined. Default is system/interfaces/settings/ImportFetchSize
     * @param conn
     * @param transactionId
     * @param tp
     * @return
     * @throws SQLException
     */
    protected List getImportListWithFetchSize( DBConnection conn,
                                             int transactionId,
                                             HashMap tp  ) throws SQLException{
        int recordsToProcess = Registry.getVarInt(
            "system/interfaces/settings/ImportFetchSize" , DEFAULT_IMPORT_BATCH_SIZE);

        String wbitypName = (String )tp.get(WBITYPNAME_PARAM_NAME);
        TypeData data = new TypeAccess(conn).load(wbitypName);
        if (!StringHelper.isEmpty(data.getWbitypUdf2() )) {
            try {
                recordsToProcess = Integer.parseInt(data.getWbitypUdf2());
            }
            catch (NumberFormatException ex) {
                throw new NestedRuntimeException ("Import fetch size for transaction type :" + wbitypName + " could not be parsed");
            }
        }
        return new ImportAccess(conn).loadByTransactionIdWithFetchSizeFiltered(transactionId ,
            recordsToProcess,
            lastProcessedId,
            ImportData.STATUS_PENDING);
    }

    private int getImportFailedCount( DBConnection conn,
                                       int transactionId ) throws SQLException{
         return new ImportAccess(conn).loadCountByTransactionId(transactionId ,
             ImportData.STATUS_ERROR);
     }


    /**
     * Updates the transaction record in the database
     */
    private void updateTransaction( DBConnection conn, int transactionId,
                                    String status, String message,
                                    java.util.Date startDate , java.util.Date endDate) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("updateTransaction(" + transactionId + "," +
                    status + ",'" + message + "')");
        }
        if( transactionId < 0 ) return;
        new TransactionAccess(conn).save(transactionId , status , message ,
                                         startDate , endDate);
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
     * Updates the transaction record in the database with 'INTERRUPTED' status
     */
    private void setTransactionInterrupted( DBConnection conn, int transactionId,
            java.util.Date startDate ) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("setTransactionInterrupted(" + transactionId + "')");
        }
        if( transactionId < 0 ) return;
        new TransactionAccess(conn).setTransactionInterrupted(transactionId, startDate);
        conn.commit();
    }

    /**
     * Updates the import record in the database
     */
    private void updateImport( DBConnection conn, int id, String status,
                               String message) throws SQLException {
        new ImportAccess(conn).updateImport(id , status , message);
        conn.commit();
    }

    /**
     * Returns a transaction type for a given id. Caches in <code>types</code>.
     */
    private TransactionType getTransactionType(DBConnection conn, int typeId) throws SQLException {
        Integer id = new Integer(typeId);
        TransactionType typeExisting = (TransactionType)types.get(id);
        if(typeExisting!=null) return typeExisting;
        TransactionType type = new TypeAccess(conn).getTransactionType(typeId);
        types.put(id,type);
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

        WorkbrainSystem.bindDefault(
            new com.workbrain.sql.SQLSource () {
                public java.sql.Connection getConnection() throws SQLException {
                    return c;
                }
            }
        );

        WBInterfaceTaskExt task = new WBInterfaceTaskExt();
        HashMap param = new HashMap();
        param.put( TRANSACTION_TYPE_PARAM_NAME, new Integer( 60 ) );
        task.setTransactionParam( param );
        task.execute( c, 60 );
        c.commit();
    }
}
