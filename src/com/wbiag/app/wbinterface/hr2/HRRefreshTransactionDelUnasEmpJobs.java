package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Customization for removing unassigned employee jobs
 *
 **/
public class HRRefreshTransactionDelUnasEmpJobs extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionDelUnasEmpJobs.class);
    private DBConnection conn = null;

    /**
     * Override this class to customize after process batch events.
     * At this time, all HRRefreshTransactionData data has been processed.
     * All processed is available through <code>HRRefreshCache</code>.
     *
     * @param conn                          DBConnection
     * @param hrRefreshTransactionDataList  List of <code>HRRefreshTransactionData</code>
     * @param process                       HRRefreshProcessor
     * @throws Exception
     */
    public void postProcessBatch(DBConnection conn,
                                 List hrRefreshTransactionDataList,
                                 HRRefreshProcessor process) throws Exception {
        if (hrRefreshTransactionDataList == null || hrRefreshTransactionDataList.size() == 0) {
                return;
        }
        this.conn = conn;
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            processDelUnassignedEmpJobs(data , conn);
        }
    }

    protected void processDelUnassignedEmpJobs(HRRefreshTransactionData data,
                               DBConnection c) throws SQLException, WBInterfaceException {
        Map empjobs = data.getHRRefreshData().getEmployeeJobs();
        // *** no action is taken if no empjobs are passed
        if (empjobs == null || empjobs.size() == 0) {
            return;
        }
        IntegerList jobIds = new IntegerList();
        CodeMapper cm = CodeMapper.createCodeMapper(c);
        Iterator iter = empjobs.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String jobName = (String)entry.getKey();
            JobData jobData = cm.getJobByName(jobName);
            if (jobData != null) {
                jobIds.add(jobData.getJobId());
            }
            else {
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug( "Job :" + jobName + " not found when deleting unassigned jobs");}
            }
        }
        if (jobIds.size() > 0) {
            delEmpJobs(data.getEmpId() ,  jobIds);
        }
    }

    protected void delEmpJobs(int empId, IntegerList jobIds) throws SQLException {
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM employee_job WHERE emp_id = ? ");
            if (jobIds != null && jobIds.size() > 0) {
                sb.append("AND job_id NOT IN (");
                for (int i = 0, k = jobIds.size(); i < k; i++) {
                    sb.append(i > 0 ? ",?" : "?");
                }
                sb.append(")");
            }
            ps = conn.prepareStatement(sb.toString());
            ps.setInt(1 , empId);
            if (jobIds != null && jobIds.size() > 0) {
                for (int i = 0, k = jobIds.size(); i < k; i++) {
                    ps.setInt(i + 2, jobIds.getInt(i));
                }
            }
            int upd = ps.executeUpdate();
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug( "Deleted " + upd + " unassigned jobs for empId : " + empId);}
        }
        finally {
            if (ps != null) ps.close();
        }
    }

}