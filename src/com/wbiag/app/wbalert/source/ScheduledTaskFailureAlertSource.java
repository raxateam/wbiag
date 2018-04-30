package com.wbiag.app.wbalert.source ;

import java.util.*;

import com.workbrain.app.wbalert.*;
import com.workbrain.server.data.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.server.data.type.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.SQLException;
/**
 * Created on Mar 6, 2005
 *
 * Title: ScheduledTaskFailureAlertSource
 * Description: The main source for the ScheduledTaskFailureAlert
 * <p>Copyright:  Copyright (c) 2004</p>
 * <p>Company:    Workbrain Inc.</p>
 */

public class ScheduledTaskFailureAlertSource extends AbstractRowSource{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ScheduledTaskFailureAlertSource.class);

    public static final String PARAM_SKD_TASK_IDS = "SKD_TASK_IDS";
    public static final String PARAM_LOOK_BACK_MINUTES = "LOOK_BACK_MINUTES";
    public static final String PARAM_LOG_STATUS = "LOG_STATUS";
    public static final String PARAM_DEFLT_LOG_STATUS =  "INTERRUPTED,ERROR,FAILED";

    private RowDefinition rowDefinition;
    private DBConnection conn;
    private String selectedTasks;

    private int lookBackMinutes = 0;
    private Date taskDateTime;
    private String[] logStatus;
    private java.util.List rows = new ArrayList();

    public static final String COL_JSTSK_ID = "JSTSK_ID";
    public static final String COL_JSTSK_NAME= "JSTSK_DESC";
    //public static final String COL_JSTSK_FAILURES = "JSTSK_FAILURES";
    public static final String COL_JSLOG_TIME = "JSLOG_TIME";
    public static final String COL_JSLOG_ERROR = "JSLOG_ERROR";
    public static final String COL_JSLOG_STATUS = "JSLOG_STATUS";

    {
        RowStructure rs = new RowStructure(20);
        rs.add(COL_JSTSK_ID,CharType.get(100));
        rs.add(COL_JSTSK_NAME,CharType.get(100));
        rs.add(COL_JSLOG_TIME,CharType.get(100));
        rs.add(COL_JSLOG_ERROR,CharType.get(100));
        rs.add(COL_JSLOG_STATUS,CharType.get(100));
        rowDefinition = new RowDefinition(-1,rs);
    }

    /**
     *
     *@param  c                    Connection
     *@param  alertParams          alert parameters
     *@exception  AccessException  Description of Exception
     */
    public ScheduledTaskFailureAlertSource(DBConnection c , HashMap alertParams) throws AccessException {
        this.conn = c;

        String lookBackMinutesS = (String)alertParams.get(PARAM_LOOK_BACK_MINUTES);
        selectedTasks = (String)alertParams.get(PARAM_SKD_TASK_IDS);
        if (!StringHelper.isEmpty(lookBackMinutesS)) {
            try {
                this.lookBackMinutes = Integer.parseInt(lookBackMinutesS);
                this.lookBackMinutes = (lookBackMinutes < 0) ? 0 : lookBackMinutes;
            }
            catch (NumberFormatException ex) {
                throw new AccessException(PARAM_LOOK_BACK_MINUTES + " must be an Integer");
            }
        }
        this.taskDateTime = (Date) alertParams.get(WBAlertProcess.TASK_PARAM_TASKDATETIME);
        String sLogStat = (String)alertParams.get(PARAM_LOG_STATUS);
        if (StringHelper.isEmpty(sLogStat)) {
            sLogStat = PARAM_DEFLT_LOG_STATUS;
        }
        this.logStatus = StringHelper.detokenizeString(sLogStat , ",");
        try {
            loadRows();
        }
        catch (Exception e) {
            throw new NestedRuntimeException (e);
        }
    }


    /**
     *
     *@exception  AccessException  Description of Exception
     */
    private void loadRows() throws AccessException, SQLException{
        rows.clear();
        Date lookbackDate = DateHelper.addMinutes(taskDateTime,  -1 * lookBackMinutes);
        StringBuffer sb = new StringBuffer(400);

        sb.append("SELECT jobskd_task.JSTSK_DESC, jobskd_log.JSLOG_TIME, jobskd_log.JSLOG_ERROR, jobskd_log.JSLOG_STATUS ");
        sb.append("FROM jobskd_log, jobskd_task ");
        sb.append(" WHERE jobskd_log.JSTSK_ID = jobskd_task.JSTSK_ID ");
        sb.append(" AND jobskd_task.JSTSK_DELETED <> 'Y' ");
        if (this.logStatus.length > 0) {
            sb.append(" AND jobskd_log.JSLOG_STATUS IN (");
            for (int i = 0; i < this.logStatus.length; i++) {
                sb.append(i > 0 ? ",?" : "?");
            }
            sb.append(" )");
        }
        sb.append(" AND jobskd_log.JSLOG_TIME > ? ");
        if (!StringHelper.isEmpty(selectedTasks)) {
            sb.append(" AND jobskd_task.jstsk_id IN (");
            int[] taskIds = StringHelper.detokenizeStringAsIntArray(selectedTasks, ",", true);
            for (int i = 0; i < taskIds.length; i++) {
                sb.append(i > 0 ? ",?" : "?");
            }
            sb.append(")");
        }

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sb.toString());
            int cnt = 1;
            if (this.logStatus.length > 0) {
                for (int i = 0; i < this.logStatus.length; i++) {
                    ps.setString(cnt++, this.logStatus[i]);
                }
            }

            ps.setTimestamp(cnt++ , new Timestamp(lookbackDate.getTime()) );
            if (!StringHelper.isEmpty(selectedTasks)) {
                int[] taskIds = StringHelper.detokenizeStringAsIntArray(selectedTasks, ",", true);
                for (int i = 0; i < taskIds.length; i++) {
                    ps.setInt(cnt++, taskIds[i] );
                }
            }


            rs = ps.executeQuery();
            while (rs.next()) {
                Row r = new BasicRow(getRowDefinition());
                r.setValue(COL_JSTSK_NAME , rs.getString(1));
                r.setValue(COL_JSLOG_TIME , rs.getString(2) );
                r.setValue(COL_JSLOG_ERROR , rs.getString(3));
                r.setValue(COL_JSLOG_STATUS , rs.getString(4));
                rows.add(r);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        if (logger.isDebugEnabled()) logger.debug("Loaded : " + rows.size());
    }

    public RowDefinition getRowDefinition() throws AccessException {
        return rowDefinition;
    }

    public RowCursor query(String queryString) throws AccessException{
        return queryAll();
    }

    public RowCursor query(String queryString, String orderByString) throws AccessException{
        return queryAll();
    }

    public RowCursor query(List keys) throws AccessException{
        return queryAll();
    }

    public RowCursor query(String[] fields, Object[] values) throws AccessException {
        return queryAll();
    }

    public RowCursor queryAll()  throws AccessException{
        return new AbstractRowCursor(getRowDefinition()){
            private int counter = -1;
            protected Row getCurrentRowInternal(){
                return counter >= 0 && counter < rows.size() ? (BasicRow)rows.get(counter) : null;
            }
            protected boolean fetchRowInternal() throws AccessException{
                return ++counter < rows.size();
            }
            public void close(){}
        };
    }

    public boolean isReadOnly(){
       return true;
    }

    public int count() {
        return rows.size();
    }

    public int count(String where) {
        return rows.size();
    }


}






