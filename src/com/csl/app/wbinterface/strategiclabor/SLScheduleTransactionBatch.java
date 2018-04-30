package com.csl.app.wbinterface.strategiclabor;

import java.util.*;
import java.sql.SQLException;
import com.workbrain.util.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.wbinterface.schedulein.ScheduleTransaction;

/**
 * Common functions for Strategic Labor related transactions.
 *
 * <p>Copyright: Copyright (c) 2009 Infor Global Solutions.</p>
 *
 **/
public class SLScheduleTransactionBatch extends ScheduleTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SLScheduleTransactionBatch.class);

    // Note: 
    // Current SLSchedule task parameter does not have BATCH_PROCESS_SIZE parameter, therefore
    // the batch size will be defaulted to 100. There is really no need for this value to be changed.
    //
    // Furthermore, batches are per employee, i.e. days recalc for one employee are grouped in same
    // batch (up to the batch size limit). 
    
    public static final String PARAM_BATCH_PROCESS_SIZE = "BATCH_PROCESS_SIZE";
    public static final int DEFAULT_BATCH_PROCESS_SIZE  = 100;
    private int batchCalculateCount = DEFAULT_BATCH_PROCESS_SIZE;

    private final Map empAndDates = new HashMap();
    private int numEmpAndDates = 0;
    protected RuleAccess ra = null;

    /**
     * Add employees and dates to the to-be-calculated list.
     *
     *@param    empId   empId
     *@param    dat     date to calculate
     */
    protected void addToEmpIdAndDates(int empId , Date dat)
        throws SQLException{

        if (dat == null) {
            throw new RuntimeException ("Date cannot be null");
        }
        dat = DateHelper.truncateToDays( dat );

        String key = String.valueOf(empId);
        Set dats = (Set) empAndDates.get( key );
        if ( dats == null ) {
            empAndDates.put( key , dats = new TreeSet() );
        }
        if ( dats.add( dat ) )
            ++numEmpAndDates;
    }

    /**
     * Calculates all employees and dates added by addToEmpIdAndDates.
     *
     *@param    recreatesSchedule   whether calculation needs rebuilding schedule
     *@throws   SQLException
     */
    protected void calculateEmpIdAndDates(boolean recreatesSchedule)
        throws SQLException{
        if ( numEmpAndDates==0 ) {
            return;
        }
        long oneStart = System.currentTimeMillis();
        if ( ra == null )
            ra = new RuleAccess( conn );

        try {
            Iterator i = empAndDates.entrySet().iterator();
            int calcCount = 0;
            while (i.hasNext()) {
                Map.Entry empAndDate = (Map.Entry) i.next();
                TreeSet dats = (TreeSet) empAndDate.getValue();
                if (dats == null || dats.size() == 0)
                    continue;

                int empId = Integer.parseInt((String) empAndDate.getKey());
                Date minDate = (Date) dats.first();
                Date maxDate = (Date) dats.last();
                if (recreatesSchedule) {
                    /* delete the employee schedule for overrides to be applied SO WEIRD */
                    EmployeeScheduleAccess esa = new EmployeeScheduleAccess(conn, null);
                    esa.deleteByDateRange(empId, minDate , maxDate);
                }
                log ("Calculating emp : " + empId + " for " + dats.size() + " day(s)");
                Iterator it = dats.iterator();
                while (it.hasNext()) {
                    Date dat = (Date) it.next();
                    ra.addRecordToCalculate(empId , dat);
                    if (++calcCount >= getBatchCalculateCount()) {
                        log ("Calculating " + calcCount + " days");
                        ra.calculateRecords(false);
                        calcCount = 0;
                    }
                }

            }
            /* if there are uncalculated recs */
            if ( calcCount>0 ) {
                log ("Calculating " + calcCount + " days");
                ra.calculateRecords(false);
            }
        } finally {
            empAndDates.clear();
            numEmpAndDates = 0;
        }
        meterTime("calculation of all days", oneStart);
    }

    protected int getBatchCalculateCount(){
        return batchCalculateCount;
    }

    protected void log( String message ) {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug( message );}
    }

    /**
     * Whether calculation will be done in batch manner.
     */
    protected boolean isBatchCalculation() {
        return true;
    }

    protected long meterTime(String what, long start){
        long l = System.currentTimeMillis();
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("\t"+what+" took: "+(l-start)+" millis");}
        return l;
    }
    
    /**
     * Returns the batch processing size.
     *
     * @param params  transaction parameters
     * @return
     */
    protected int getBatchProcessSize(HashMap params) {
    	if (params == null) {
    		batchCalculateCount = DEFAULT_BATCH_PROCESS_SIZE;
    		return batchCalculateCount;
    	}
    	
        String sBatchProcessSize = (String) params.get( PARAM_BATCH_PROCESS_SIZE);
        
        if ( !StringHelper.isEmpty(sBatchProcessSize) ) {
        	batchCalculateCount = Integer.parseInt(sBatchProcessSize);
        } else {
        	batchCalculateCount = DEFAULT_BATCH_PROCESS_SIZE;
        }
        
        return batchCalculateCount;
    }
}
