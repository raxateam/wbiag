package com.wbiag.app.modules.employeeborrow;


import java.util.*;
import java.text.*;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.util.*;
import com.workbrain.security.SecurityService;
import com.workbrain.sql.*;
import com.workbrain.server.WorkbrainParametersRetriever;
import com.workbrain.server.registry.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.tool.mail.*;
/**
 * EmployeeBorrowEmpGroupTaskTask to update staff_group assignments resulting
 * from EmployeeBorrow interaction.
 * @deprecated Use core functionality as of 5.0.3.2
 */
public class EmployeeBorrowEmpGroupTask extends AbstractScheduledJob {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmployeeBorrowEmpGroupTask.class);

    public static final String PARAM_SENDS_MESSAGE = "SENDS_MESSAGE";
    public static final String PARAM_CLIENT_ID = "CLIENT_ID";
    public static final String PARAM_BATCH_SIZE = "BATCH_SIZE";

    public static final String CHNGHIST_TABLE_NAME = "EMPLOYEE_BORROW";
    public static final String CHNGHIST_TYPE_ASSIGN_TO = "I";
    public static final String CHNGHIST_TYPE_MOVE_BACK_TO = "U";
    public static final String CHNGHIST_TYPE_FINISHED = "D";
    public static final String CHNGHIST_REC_NAME_DELIM = ",";
    public static final int DEFAULT_BATCH_SIZE = 100;

    public static final MessageFormat MF_ASSIGN_TO =
        new MessageFormat("Employee {0} has been assigned to Staff Group: {1} as a result of Employee Borrow");
    public static final MessageFormat MF_MOVE_BACK =
        new MessageFormat("Employee {0} has been moved back to Staff Group: {1} as a result of Employee Borrow");

    public EmployeeBorrowEmpGroupTask() {
    }

    public Status run(int taskID, Map param) throws Exception {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug("EmployeeBorrowEmpGroupTask.run(" + taskID + ", " + param + ")");

        String currentClientId = null;

        StringBuffer sb = new StringBuffer(200);
        sb.append("Scheduled OK <br>");
        try {
            // *** store current client id
            currentClientId = SecurityService.getCurrentClientId();

            getConnection().turnTraceOff();
            getConnection().setAutoCommit(false);

            if (StringHelper.isEmpty((String)param.get( PARAM_CLIENT_ID ))) {
                throw new RuntimeException ("Client_id must be specified");
            }
            String clientId = (String) param.get( PARAM_CLIENT_ID );

            SecurityService.setCurrentClientId(clientId);
            String msg = execute(getConnection() , param, true);
            sb.append(msg);

            if (!isInterrupted()) {
                getConnection().commit();
                return jobOk( sb.toString() );
            } else {
                getConnection().rollback();
                return jobInterrupted("EmployeeBorrowEmpGroupTask task has been interrupted.");
            }

        } catch (Exception e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in running EmployeeBorrowEmpGroupTask", e);}
            if( getConnection() != null ) getConnection().rollback();
            throw e;
        } finally {
            SecurityService.setCurrentClientId(currentClientId);
            releaseConnection();
        }
    }


    private static final String SQL_EMPGRP_UPDATE
        = "UPDATE so_employee SET empgrp_id = ? WHERE emp_id = ?";
    private static final String SQL_CHNGHIST_TYPE_UPDATE
        = "UPDATE change_history SET chnghist_change_type = ? WHERE chnghist_id = ?";


    public String execute(DBConnection conn, Map param,
                          boolean shouldCommit) throws Exception {

        String sSendsMessage = (String)param.get(PARAM_SENDS_MESSAGE);
        boolean sendsMessage = StringHelper.isEmpty(sSendsMessage)
            ? false : "Y".equals(sSendsMessage) ? true : false;
        String sBatchSize = (String)param.get(PARAM_BATCH_SIZE);
        int batchSize = StringHelper.isEmpty(sBatchSize)
            ? DEFAULT_BATCH_SIZE : Integer.parseInt(sBatchSize) ;
        CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);

        PreparedStatement psEmpGroup = null;
        PreparedStatement psChngHist = null;
        Date taskDatetime = new Date();
        List changes = getChanges(conn, taskDatetime);
        if (logger.isDebugEnabled()) logger.debug("Found:" + changes.size() + " change_history records for staff group");

        int cnt = 0;
        try {
            psEmpGroup = conn.prepareStatement(SQL_EMPGRP_UPDATE);
            psChngHist  = conn.prepareStatement(SQL_CHNGHIST_TYPE_UPDATE);

            Iterator iter = changes.iterator();
            while (iter.hasNext()) {
                Change item = (Change) iter.next();
                String[] recName = StringHelper.detokenizeString(item.chnghist_rec_name , CHNGHIST_REC_NAME_DELIM);
                if (recName.length != 3) {
                    throw new RuntimeException ("Malformed change_history record. chnghist_id = " + item.chnghist_id);
                }
                int stffGrpId = Integer.parseInt(recName[0]);
                psEmpGroup.setInt(1 , stffGrpId) ;
                psEmpGroup.setInt(2, item.empId );
                psEmpGroup.addBatch();
                if (logger.isDebugEnabled()) logger.debug("Changed staff group to:" + stffGrpId + " forempid:" + item.empId );

                psChngHist.setString(1 , CHNGHIST_TYPE_FINISHED) ;
                psChngHist.setInt(2, item.chnghist_id );
                psChngHist.addBatch();

                if (sendsMessage) {
                    int userId1 = Integer.parseInt(recName[1]);
                    String userName1 = WorkbrainUserAccess.retrieveWbuNameByWbuId(conn, userId1);
                    int userId2 = Integer.parseInt(recName[2]);
                    String userName2 = WorkbrainUserAccess.retrieveWbuNameByWbuId(conn, userId2);
                    String stfGrpName = codeMapper.getSOEmployeeGroupById(stffGrpId).getEmpgrpName();
                    String empName = EmployeeAccess.retrieveEmpNameById(conn, item.empId);
                    String subject = null;
                    if (CHNGHIST_TYPE_ASSIGN_TO.equals(item.chnghist_change_type)) {
                        subject = MF_ASSIGN_TO.format(new String[] {empName, stfGrpName} );
                    }
                    else if (CHNGHIST_TYPE_MOVE_BACK_TO.equals(item.chnghist_change_type)) {
                        subject = MF_MOVE_BACK.format(new String[] {empName, stfGrpName} );
                    }
                    if (!StringHelper.isEmpty(subject)) {
                        sendMail(conn, userName1, "SYSTEM", subject, "");
                        sendMail(conn, userName2, "SYSTEM", subject, "");
                        if (logger.isDebugEnabled()) logger.debug("Sent messages to :" + userName1 + "," + userName2);
                    }
                }

                if (isInterrupted()) {
                    break;
                }
                if (++cnt % batchSize == 0) {
                    psEmpGroup.executeBatch();
                    psChngHist.executeBatch();
                    if (shouldCommit) conn.commit();
                }
            }
            if (cnt % batchSize != 0) {
                psEmpGroup.executeBatch();
                psChngHist.executeBatch();
                if (shouldCommit) conn.commit();
            }
        }
        finally {
            if (psEmpGroup != null) psEmpGroup.close();
            if (psChngHist != null) psChngHist.close();
        }

        StringBuffer sb = new StringBuffer(200);
        sb.append(cnt + " staff group assignments have been processed") ;
        return sb.toString();
    }


    private static final String SQL_CHNGHIST_SELECT =
        "SELECT chnghist_id, chnghist_change_date, "
        + " chnghist_record_id,chnghist_rec_name, chnghist_change_type"
        + " FROM change_history"
        + " WHERE chnghist_table_name = ? "
        + " AND chnghist_change_type IN (?,?)"
        + " AND  chnghist_change_date <= ?";

    private List getChanges(DBConnection conn, Date taskDatetime) throws Exception{
        List ret = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(SQL_CHNGHIST_SELECT);
            ps.setString(1 , CHNGHIST_TABLE_NAME);
            ps.setString(2 , CHNGHIST_TYPE_ASSIGN_TO);
            ps.setString(3 , CHNGHIST_TYPE_MOVE_BACK_TO);
            ps.setTimestamp(4 , new java.sql.Timestamp(taskDatetime.getTime()));
            rs = ps.executeQuery();
            while (rs.next()) {
                Change ch = new Change();
                ch.chnghist_id = rs.getInt(1);
                ch.empId = rs.getInt(3);
                ch.chnghist_change_date = rs.getDate(2);
                ch.chnghist_rec_name = rs.getString(4);
                ch.chnghist_change_type = rs.getString(5);
                ret.add(ch);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return ret;
    }

    class Change{
        int chnghist_id;
        Date chnghist_change_date;
        int empId;
        String chnghist_rec_name;
        String chnghist_change_type;
    }

    public String getTaskUI() {
        return "/jobs/wbiag/employeeBorrowEmpGroupTaskParams.jsp";
    }

    private boolean sendMail(DBConnection conn, String to,
                     String from, String subject, String msg) throws Exception{
        Message m = new Message( conn );
        m.setTo(to);
        m.setMessageBody(msg);
        m.setMessageSubject(subject);
        m.setMessageType(Util.MESSAGE_TYPE_MAIL);
        m.setSenderName(from);
        m.setSaveMessageCopy(false);
        m.send();

        return true;
    }

    public static String makeRecNameString(int stffgrpId, int submitUserId, int manUserId) {
        StringBuffer sb = new StringBuffer(200);
        sb.append(String.valueOf(stffgrpId)).append(CHNGHIST_REC_NAME_DELIM) ;
        sb.append(String.valueOf(submitUserId)).append(CHNGHIST_REC_NAME_DELIM) ;
        sb.append(String.valueOf(manUserId)) ;

        return sb.toString() ;
    }

}
