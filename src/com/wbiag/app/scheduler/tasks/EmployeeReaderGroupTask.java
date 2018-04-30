package com.wbiag.app.scheduler.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.sql.*;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.wbiag.app.ta.db.EmployeeReaderAccess;
import com.wbiag.app.ta.model.EmployeeReaderData;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

/**
 * Title: EmployeeReaderGroupTask.java <br>
 * Description: Check the custom table nightly and change/add reader group accordingly.
 * <br>
 * Created: Oct 18, 2005
 *
 * @author snimbkar
 *         <p>
 *         Revision History <br>
 *         Oct 18, 2005 - First Draft<br>
 *         <p>
 */
public class EmployeeReaderGroupTask extends AbstractScheduledJob {
    private static final Logger logger = Logger
    .getLogger(EmployeeReaderGroupTask.class);

    private String taskMessage;
    Date  currentDate= new Date(System.currentTimeMillis());

    public EmployeeReaderGroupTask()throws Exception{
    }
    /*
     * (non-Javadoc)
     *
     * @see com.workbrain.app.scheduler.enterprise.ScheduledJob#getTaskUI()
     */
    public String getTaskUI() {
        return null;
    }
    /*
     * (non-Javadoc)
     *
     * @see com.workbrain.app.scheduler.enterprise.ScheduledJob#run(int,
     *      java.util.Map)
     */
    public Status run(int arg0, Map arg1) throws Exception {

        if (logger.isDebugEnabled()){
            logger.debug("EmployeeReaderGroupTask.run begin");
        }
        DBConnection conn = getConnection();

        try {
            execute(conn , arg1);
            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("EmployeeReaderGroupTask error", e);
            }
            throw e;
        }
        if (logger.isDebugEnabled()){
            logger.debug("EmployeeReaderGroupTask.run end");
        }
        taskMessage = " Job Scheduled OK ";
        return jobOk(taskMessage);
    }

    protected void setCurrentDate(Date date){
        currentDate=date;
        if (logger.isDebugEnabled()){
            logger.debug("Setting Current Date="+currentDate);
        }
    }

    public void execute(DBConnection conn, Map params) throws SQLException {

        if (logger.isDebugEnabled()){
            logger.debug("execute begin");
        }
        deleteOldReaderGroups(conn);
        addReadergroup(conn);
    }
    /**
     * Deletes the Reader groups that are in the past.
     * @throws SQLException
     */
    private void deleteOldReaderGroups(DBConnection conn)throws SQLException{

        if (logger.isDebugEnabled()){
            logger.debug("Delete Reader Group");
        }
        String conOper= conn.getDBServer().getConcatOperator();
        StringBuffer deleteSql = new StringBuffer(200);
        deleteSql.append("DELETE FROM employee_reader_group WHERE " );
        deleteSql.append(" (rdrgrp_id, emp_id) IN ");
        deleteSql.append(" (SELECT wdrg_rdrgrp_id , emp_id FROM wbiag_dateeff_rdr_grp");
        deleteSql.append(" WHERE ? > wdrg_end_date) ");
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(deleteSql.toString());
            ps.setTimestamp(1, DateHelper.toTimestamp(currentDate));
            int count= ps.executeUpdate();
            if (logger.isDebugEnabled()) logger.debug("Deleted :" + count + " obsolete reader groups");

        } catch (SQLException e){
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Could not delete Reader Group");
            }
            throw e;
        } finally {
            if (ps != null)
                ps.close();
        }
    }
    /**
     * adds Reader groups as per entries in the WBIAG_DATEEFF_RDR_GRP. Associate Loan  Form.
     * @throws SQLException
     */
    private void addReadergroup(DBConnection conn)throws SQLException {

        String conOper= conn.getDBServer().getConcatOperator();


        StringBuffer selectSql = new StringBuffer(200);
        selectSql.append("SELECT DISTINCT emp_id, wdrg_rdrgrp_id FROM ");
        selectSql.append(" wbiag_dateeff_rdr_grp");
        selectSql.append(" WHERE ? BETWEEN wdrg_start_date AND wdrg_end_date");
        selectSql.append(" AND (emp_id, wdrg_rdrgrp_id) NOT IN ");
        selectSql.append("(SELECT emp_id, rdrgrp_id FROM employee_reader_group) ");

        PreparedStatement ps1 = null;
        ArrayList listRecordData=new ArrayList();
        try {
            EmployeeReaderAccess access=new EmployeeReaderAccess(conn);
            ps1 = conn.prepareStatement(selectSql.toString());
            if (logger.isDebugEnabled()) logger.debug("Running :" + selectSql.toString() + " with :" + currentDate);

            ps1.setTimestamp(1, DateHelper.toTimestamp(currentDate));

            ResultSet rs= ps1.executeQuery();
            while(rs.next()){

                EmployeeReaderData data=new EmployeeReaderData();
                data.setEmpId(rs.getInt(1));
                data.setRdrgrpId(rs.getInt(2));
                listRecordData.add(data);
            }
            if (logger.isDebugEnabled()) logger.debug("Found :" + listRecordData.size() + " records to insert");
            if (logger.isDebugEnabled()) logger.debug("Insert Records :\n" + listRecordData);
            access.addEmployeeReader(listRecordData);
        } catch (SQLException e){
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Could not add Reader Group");
            }
            throw e;
        } finally {
            if (ps1 != null)
                ps1.close();
        }
    }


}

