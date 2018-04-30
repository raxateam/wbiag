package com.wbiag.app.modules.employeeborrow ;

import java.util.*;


import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.workflow.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.exceptions.SQLRetailException;
import com.workbrain.app.modules.retailSchedule.model.*;


/**
 *  Employee Loan Process Action
 *  Processes the Employee Loan Request
 * @deprecated Use core functionality as of 5.0.3.2
 */
public class EmployeeBorrowProcess extends AbstractActionProcess {

	 private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmployeeBorrowProcess.class);

     public static final String PROP_PERFORM_SCHEDULE_DETAIL_OVERRIDES = "PERFORM_SCHEDULE_DETAIL_OVERRIDES";
     public static final String PROP_PERFORM_STAFF_GROUP_ASSIGNMENT = "PERFORM_STAFF_GROUP_ASSIGNMENT";

	 public ActionResponse processObject (Action data, WBObject object, Branch[] outputs, ActionResponse previous) throws WorkflowEngineException {

         DBConnection conn = this.getConnection();

         try {
             CodeMapper cm = CodeMapper.createCodeMapper(conn);
             // Grab values from form
             String storeName = WorkflowUtil.getFieldValueAsString(object,
                 EmployeeBorrowValidator.FLD_STORE);
             int storeId = Integer.parseInt(WorkflowUtil.getFieldValueId(object,
                 EmployeeBorrowValidator.FLD_STORE));

             int empId = Integer.parseInt(WorkflowUtil.getFieldValueId(
                 object, EmployeeBorrowValidator.FLD_EMPLOYEE));
             Date startDate = WorkflowUtil.getFieldValueAsDate(object,
                 EmployeeBorrowValidator.FLD_START_DATE);
             Date endDate = WorkflowUtil.getFieldValueAsDate(object, EmployeeBorrowValidator.FLD_END_DATE);
             if (DateHelper.equals(startDate, endDate)) {
                 endDate = DateHelper.addDays(startDate, 1);
             }
             Date submitDate = DateHelper.getCurrentDate();

             String sPerformsSkdDetail = WorkflowUtil.getDataPropertyAsString(data,
                PROP_PERFORM_SCHEDULE_DETAIL_OVERRIDES);
             boolean performsSkdDetail = "true".equalsIgnoreCase(sPerformsSkdDetail);
             String sPerformsStaffGrp = WorkflowUtil.getDataPropertyAsString(data,
                PROP_PERFORM_STAFF_GROUP_ASSIGNMENT);
             boolean performsStaffGrp = "true".equalsIgnoreCase(sPerformsStaffGrp);

             EmployeeAccess ea = new EmployeeAccess(conn, cm);
             EmployeeData ed = ea.load(empId, submitDate);

             int wbTeamID = cm.getWBTeamByName(storeName).getWbtId();

             createTempTeam(conn, empId, startDate, endDate, wbTeamID);

             if (performsSkdDetail) {
                 // create team overrides
                 createSkdDetailOvrs(conn, empId, storeId,
                                     startDate, endDate);
             }

             if (performsStaffGrp) {
                 processStaffGroup(conn, empId, startDate, endDate, object);
             }

             runRecalc(conn, empId ,startDate , endDate);

             this.commit(conn);
         }
         catch (Exception e) {
             if (logger.isEnabledFor(org.apache.log4j.Level.ERROR))   logger.error("com.wbiag.app.modules.employeeloan.EmployeeBorrowProcess",  e);
             this.rollback(conn);
             return WorkflowUtil.createActionResponseException(e.getMessage());
         }
         finally {
             this.close(conn);
         }
         return WorkflowUtil.createActionResponse(outputs, "Success");
    }


    protected void runRecalc(DBConnection conn, int empLoanedID,
                                   Date startDate,
                                   Date endDate) throws Exception {
        RuleEngine.runCalcGroup(conn, empLoanedID, startDate , endDate, false);
    }

    protected void createTempTeam(DBConnection conn, int empId,
                                  Date startDate,
                                  Date endDate,
                                  int wbTeamID) throws Exception {
        EmployeeTeamAccess eta = new EmployeeTeamAccess(conn);
        EmployeeTeamData etd   = new EmployeeTeamData();
        etd.setEmpId(empId);
        etd.setEmptEndDate(endDate);
        etd.setEmptStartDate(startDate);
        etd.setEmptHomeTeam("N");
        etd.setWbtId(wbTeamID);
        eta.insert(etd);
        if (logger.isDebugEnabled()) logger.debug("Created temp team:" + wbTeamID + " for employee:" + empId);
    }

    protected void createSkdDetailOvrs(DBConnection conn, int empId,
                                       int wbtId,
                                       Date startDate,
                                       Date endDate) throws Exception {

        CalcSimulationContext calcContext = new CalcSimulationContext();

        CalcSimulationAccess calcAccess = new CalcSimulationAccess(calcContext);
        calcAccess.addEmployeeDate(empId, startDate, endDate);
        calcAccess.load(conn);
        CalcSimulationEmployee calcEmployee = calcAccess.getResultForEmp(empId);

        for (Date date = startDate; date.compareTo(endDate) <= 0;
             date = DateHelper.addDays(date, 1)) {

            List skdDetails = calcEmployee.getScheduleDetails(date);

            Iterator iter = skdDetails.iterator();
            while (iter.hasNext()) {
                EmployeeScheduleDetailData item = (EmployeeScheduleDetailData)
                    iter.next();
                EmployeeScheduleDetailData esdd = calcEmployee.
                    createDefaultScheduleDetailData(item.getWrkdStartTime(),
                    item.getWrkdEndTime());
                esdd.setWbtId(wbtId);

                calcEmployee.applyEmployeeScheduleDetailData(esdd,
                    CalcSimulationEmployee.INSERT, 0);

            }
        }
        calcAccess.save(conn, calcEmployee);
        if (logger.isDebugEnabled()) logger.debug("Created skd detail overrides for employee:" + empId);
    }

    protected static final String SQL_CHNG_HIST_INSERT =
        "INSERT INTO change_history (chnghist_id, chnghist_change_date, "  +
        " chnghist_table_name, chnghist_change_type, chnghist_record_id,chnghist_rec_name)"
        + " VALUES(?,?,?,?,?,?)";

    /**
     * Adds move and return date staff group assignment to CHANGE_HISTORY table
     * to be processes by EmployeeBorrowEmpGroupAssignTask
     * @param conn
     * @param empId
     * @param startDate
     * @param endDate
     * @param object
     * @throws Exception
     */
    protected void processStaffGroup(DBConnection conn,
                                     int empId,
                                     Date startDate,
                                     Date endDate,
                                     WBObject object) throws Exception{
        String sStaffGrpId = WorkflowUtil.getFieldValueId(object,
                 EmployeeBorrowValidator.FLD_STAFF_GROUP );
        if (StringHelper.isEmpty(sStaffGrpId)) {
            if (logger.isDebugEnabled()) logger.debug("Staff group not selected");
            return;
        }
        int staffGrpId = Integer.parseInt(sStaffGrpId);
        Date moveDate = WorkflowUtil.getFieldValueAsDate(object,
            EmployeeBorrowValidator.FLD_SGMOVE_DATE);
        Date returnDate = WorkflowUtil.getFieldValueAsDate(object, EmployeeBorrowValidator.FLD_SGRETURN_DATE);
        moveDate = moveDate == null ? startDate : moveDate;
        returnDate = returnDate == null ? endDate : endDate;
        // *** move to staff group
        int submitUserId =  WorkflowUtil.getCreatorUserId(object , conn);
        int manUserId =  Integer.parseInt(WorkflowUtil.getFieldValueAsString(object,
            EmployeeBorrowValidator.FLD_LBL_STORE_MANAGER));
        String recValMove = EmployeeBorrowEmpGroupTask.makeRecNameString(
            staffGrpId, submitUserId , manUserId);
        // *** move back to current staff group on return date
        Integer iCurStaffGrp = getStaffGrpId(conn, empId);
        if (iCurStaffGrp == null) {
            if (logger.isDebugEnabled()) logger.debug("Staff group could not be found for empId:" + empId );
            return;
        }
        int currentStaffGrpId = iCurStaffGrp.intValue();

        String recValReturn = EmployeeBorrowEmpGroupTask.makeRecNameString(
            currentStaffGrpId, submitUserId , manUserId);

        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            ps = conn.prepareStatement(SQL_CHNG_HIST_INSERT );
            // *** for move
            ps.setInt(1  , conn.getDBSequence("seq_chnghist_id").getNextValue()  );
            ps.setTimestamp(2, new Timestamp(moveDate.getTime()));
            ps.setString(3, EmployeeBorrowEmpGroupTask.CHNGHIST_TABLE_NAME);
            ps.setString(4, EmployeeBorrowEmpGroupTask.CHNGHIST_TYPE_ASSIGN_TO);
            ps.setInt(5, empId);
            ps.setString(6, recValMove);
            ps.addBatch();

            // *** for return
            ps.setInt(1  , conn.getDBSequence("seq_chnghist_id").getNextValue()  );
            ps.setTimestamp(2, new Timestamp(returnDate.getTime()));
            ps.setString(3, EmployeeBorrowEmpGroupTask.CHNGHIST_TABLE_NAME);
            ps.setString(4, EmployeeBorrowEmpGroupTask.CHNGHIST_TYPE_MOVE_BACK_TO);
            ps.setInt(5, empId);
            ps.setString(6, recValReturn);
            ps.addBatch();

            int[] upd = ps.executeBatch();
            if (logger.isDebugEnabled()) logger.debug("Added " + upd.length + " staff group assignments for empId :" + empId );
        }
        finally {
            if (ps != null) ps.close();
        }

    }


    public static Integer getStaffGrpId(DBConnection c,int empId) throws Exception{
        Integer ret = null;
        List list = new com.workbrain.app.modules.retailSchedule.db.EmployeeAccess(c).
            loadRecordData( new Employee(),Employee.TABLE_NAME, "emp_id" , empId);
        if (list.size() > 0) {
            Employee emp = (Employee) list.get(0);
            ret = new Integer(emp.getEmpgrpId());
        }

        return ret;
    }

}