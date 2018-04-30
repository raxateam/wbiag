
package com.wbiag.app.export.payroll;

import java.util.*;
import java.sql.*;

import com.workbrain.app.scheduler.InterruptCheck;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.app.scheduler.enterprise.ScheduledJob;
import com.workbrain.security.SecurityService;
import com.workbrain.server.sql.ConnectionManager;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.Datetime;
import com.workbrain.util.StringHelper;
import com.workbrain.util.DateHelper;
import org.apache.log4j.*;

/**
 * Title: HighVolumeExportTask.java <br>
 * Description: TT49356 - High Volume Payroll Export; Thread class. See
 * Technical documentation off WBIAG website for more informaiton. <br>
 * 
 * Created: May 17, 2005
 * @author cleigh
 * <p>
 * Revision History <br>
 * May 17, 2005 -  File Created according to Technical documentation <br>
 * <p>
 * */
public class HighVolumeExportTask extends AbstractScheduledJob {
    private static Logger logger = Logger.getLogger(HighVolumeExportTask.class);
    
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd hh:mm:ss";
    

    private Map taskParam = null;
    private StringBuffer taskLogMessage = new StringBuffer("Scheduled OK.");
    private int threadCnt;
    private String clientId;
    private InterruptCheck intCheck;
    
    public static final String PARAM_CLIENT_ID = "CLIENT_ID";
    public static final String PARAM_MAX_IN_PROGRESS = "MAX_IN_PROGRESS";
    
    
    /* (non-Javadoc)
     * @see com.workbrain.app.scheduler.enterprise.ScheduledJob#getTaskUI()
     */
    public String getTaskUI() {
        return "/jobs/highVolumePayrollExportTaskParams.jsp";
    }

    /* (non-Javadoc)
     * @see com.workbrain.app.scheduler.enterprise.ScheduledJob#run(int, java.util.Map)
     */
    public Status run(int taskID, Map param) throws Exception {
        
        if (logger.isDebugEnabled()) {
            logger.debug("HighVolumeExportTask.run(" + taskID + ", " + param + ")");
        }
        String currentClientId = null;
        try {
//          *** store current client id
            currentClientId = SecurityService.getCurrentClientId();

            
            
            taskParam = param;

            if (StringHelper.isEmpty( (String) taskParam.get(PARAM_CLIENT_ID))) {
                throw new RuntimeException("Client_id must be specified");
            }
            clientId = (String) taskParam.get(PARAM_CLIENT_ID);

            SecurityService.setCurrentClientId(clientId);
           
            execute();

      
        }
        catch (Exception e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error(
                    "com.wbiag.app.export.payroll.HighVolumeExportTask", e);
            }
            throw e;
        }
        finally {
            //SecurityService.setCurrentClientId(currentClientId);
        }
        return new ScheduledJob.Status(false, taskLogMessage.toString());
    }
    
    private void execute() throws SQLException {
        if (logger.isDebugEnabled()){
            logger.debug("com.wbiag.app.export.payroll.HighVolumeExportTask: execute() begin");
        }
        threadCnt = Integer.parseInt((String) taskParam.get(PARAM_MAX_IN_PROGRESS));
        
        if (logger.isDebugEnabled()){
            logger.debug("Creating Thread, max number of threads: " + threadCnt);
        }
        if (intCheck != null && isInterrupted()) {
            logger.warn("Interrupting Recalculate Task");
            return;
        }
        HighVolumePayrollExportThread peThread = new HighVolumePayrollExportThread( clientId );
        peThread.setInterruptCheck(this);
        peThread.setSlaveThreadCount(threadCnt);
        if (logger.isDebugEnabled()){
            logger.debug("Starting Thread");
        }
        peThread.execute();
        
        if (logger.isDebugEnabled()){
            logger.debug("Thread started");
            logger.debug("com.wbiag.app.export.payroll.HighVolumeExportTask: execute() end");
        }
    }
}
