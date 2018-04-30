package com.wbiag.app.modules.employeeborrow ;

import java.util.*;

import com.workbrain.app.bo.*;
import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.workflow.*;
import com.workbrain.security.team.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.tool.security.*;

/**
 *  Ensure that the employee being loaned has not already been loaned to another store
 * @deprecated Use core functionality as of 5.0.3.2
 */
public class EmployeeBorrowValidator extends AbstractActionProcess {

    public static final String FLD_START_DATE = "dpStartDate";
    public static final String FLD_END_DATE = "dpEndDate";
    public static final String FLD_EMPLOYEE = "dbEmployee";
    public static final String FLD_STORE = "dbStore";
    public static final String FLD_STAFF_GROUP = "dbStaffGroup";
    public static final String FLD_SGMOVE_DATE = "dpSGMoveDate";
    public static final String FLD_SGRETURN_DATE = "dpSGReturnDate";
    public static final String FLD_STORE_MANAGER = "strManager";
    public static final String FLD_LBL_STORE_MANAGER = "lblStrManager";

    public static final String PROP_ROLE_NAME = "STORE_ROLE_NAME";


	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmployeeBorrowValidator.class);
	private static final String DATE_FMT = "MM/dd/yyyy";
	public static final String ERRMSG_TERM = "Employee is terminated as of Loan Date.";
    public static final String ERRMSG_STFGRP_NO_SOEMP
        = "Employee doesn't have an SO_EMPLOYEE record to define current staff group";
    public static final String ERRMSG_STFGRP_SAME =
        "Employee's current staff group is same as move-to staff group";

	public ActionResponse processObject(Action data, WBObject object,
                                        Branch[] outputs, ActionResponse previous) throws WorkflowEngineException {
		DBConnection conn = this.getConnection();


		try {
            Date  formStart = WorkflowUtil.getFieldValueAsDate(object , FLD_START_DATE);
            Date formEnd   = WorkflowUtil.getFieldValueAsDate(object , FLD_END_DATE);
            String sempId = WorkflowUtil.getFieldValueId(object , FLD_EMPLOYEE);
            String wbRoleName =  WorkflowUtil.getDataPropertyAsString(data,
                PROP_ROLE_NAME);
            String sStoreId = WorkflowUtil.getFieldValueId(object, FLD_STORE);
            if (formStart == null || formEnd == null
                || StringHelper.isEmpty(sempId) ||  StringHelper.isEmpty(sStoreId)
                || StringHelper.isEmpty(wbRoleName)) {
                throw new WorkflowEngineException ("One of start,end dates, employee, store, STORE_ROLE_NAME is empty");
            }

            int empId = Integer.parseInt(sempId) ;
            int storeId = Integer.parseInt(sStoreId);
            String sPerformsStaffGrp = WorkflowUtil.getDataPropertyAsString(data,
               EmployeeBorrowProcess.PROP_PERFORM_STAFF_GROUP_ASSIGNMENT);
            boolean performsStaffGrp = "true".equalsIgnoreCase(sPerformsStaffGrp);

            checkTermination(conn, empId, formStart, formEnd);

            checkEmpTeamAlready(conn, object, empId, formStart, formEnd);

            setStoreManager(conn, object, empId, storeId,  wbRoleName);

            if (performsStaffGrp) {
                checkStaffGroup(conn , empId, object );
            }

            this.commit(conn);
		}
        catch (Exception e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("com.wbiag.app.modules.employeeloan.EmployeeBorrowValidator",   e);
            this.rollback(conn);
            return WorkflowUtil.createActionResponseException(e.getMessage());
        }
        finally {
            this.close(conn);
        }
        return WorkflowUtil.createActionResponse(outputs , "Success") ;

	}

    protected void checkTermination(DBConnection conn, int empId,
                                       Date formStart,
                                       Date formEnd) throws Exception{
        CodeMapper cm = CodeMapper.createCodeMapper(conn);
        EmployeeAccess ea = new EmployeeAccess(conn, cm);
        EmployeeData edStart = ea.load(empId ,formStart);
        EmployeeData edEnd = ea.load(empId ,formEnd);


        if (DateHelper.isBetween(edStart.getEmpTerminationDate(), formStart, formEnd)
            || DateHelper.isBetween(edEnd.getEmpTerminationDate(), formStart, formEnd)) {
            throw new WorkflowEngineException (ERRMSG_TERM);
        }

    }

    protected void checkEmpTeamAlready(DBConnection conn, WBObject object, int empId,
                                       Date formStart,
                                       Date formEnd) throws Exception{
        EmployeeTeamManager etm = new EmployeeTeamManager(conn);
        CodeMapper cm = CodeMapper.createCodeMapper(conn);

        Collection empTeams = etm.findEmployeeTeamByEmpId(empId);
        Iterator it = empTeams.iterator();
        while (it.hasNext()) {
            EmployeeTeam et = (EmployeeTeam) it.next();
            // If this team is a home team, then simply continue to the next
            // iteration as we are not concerned with teams that are home teams
            if ("Y".equalsIgnoreCase(et.getEmptHomeTeam().trim())) {
                continue;
            }

            // Not a home team so lets see if there is a conflict with the times
            if (DateHelper.isBetween(et.getEmptStartDate(), formStart, formEnd) ||
                DateHelper.isBetween(et.getEmptEndDate(), formStart, formEnd)) {

                StringBuffer sb = new StringBuffer();
                sb.append("Reason for rejection:\n\nEmployee \"");
                sb.append(WorkflowUtil.getFieldValueAsString(object , "dbEmployee")).append("\" ");
                sb.append("has been scheduled to work at store ").append("\"");
                sb.append(cm.getWBTeamById(et.getWbtId()).getWbtName()).append("\" ");
                sb.append("on the dates of ").append(DateHelper.convertDateString(et.getEmptStartDate(), DATE_FMT));
                sb.append(" to ").append(DateHelper.convertDateString(et.getEmptStartDate(), DATE_FMT));
                sb.append(" and you are attempting to schedule them from ");
                sb.append(DateHelper.convertDateString(formStart , DATE_FMT)).append(" to ");
                sb.append(DateHelper.convertDateString(formEnd, DATE_FMT)).append(".");

                throw new WorkflowEngineException (sb.toString() );
            }
        }


    }

    protected void checkStaffGroup(DBConnection conn, int empId,
                                   WBObject object) throws Exception {
        String sStaffGrpId = WorkflowUtil.getFieldValueId(object,
                 FLD_STAFF_GROUP );
        if (StringHelper.isEmpty(sStaffGrpId)) {
            return;
        }
        Integer iCurStaffGrp = EmployeeBorrowProcess.getStaffGrpId(conn, empId);
        if (iCurStaffGrp == null) {
            throw new WorkflowEngineException (ERRMSG_STFGRP_NO_SOEMP);
        }
        int staffGrpId = Integer.parseInt(sStaffGrpId);
        int currentStaffGrpId = iCurStaffGrp.intValue();
        if (staffGrpId == currentStaffGrpId) {
            throw new WorkflowEngineException (ERRMSG_STFGRP_SAME);
        }

    }

    protected void setStoreManager(DBConnection conn, WBObject object, int empId,
                                   int wbtId, String wbroleName) throws Exception {
        WorkbrainRoleData wbr = CodeMapper.createCodeMapper(conn)
            .getWBRoleByName(wbroleName);
        if (wbr == null) {
            throw new WorkflowEngineException ("Store Role not found :" + wbroleName);
        }
        int wbroleId = wbr.getWbroleId() ;
        WorkbrainUserData wud = getImmediateUserForWbtRole(conn,
            wbtId, wbroleId, DateHelper.getCurrentDate());
        if (StringHelper.isEmpty(wud.getWbuName())) {
            throw new WorkflowEngineException ("Store " + wbroleName + " could not be found");
        }
        BOFormInstance instance = (BOFormInstance)object;
        instance.setValueId(FLD_STORE_MANAGER, wud.getWbuName() , wud.getWbuName());
        instance.setValue(FLD_LBL_STORE_MANAGER, String.valueOf(wud.getWbuId()));
        if (logger.isDebugEnabled()) logger.debug("Set store " + wbroleName + " to :" + wud.getWbuName());
    }

    private WorkbrainUserData getImmediateUserForWbtRole(DBConnection dbc,
                                     int wbtId,
                                     int roleId,
                                     Date date) throws Exception {
        WorkbrainUserData ret = new WorkbrainUserData();

        // *** find all users with this role up the hierarchy
        WorkbrainTeamAccess wbta = new WorkbrainTeamAccess(dbc);
        int cnt = 0;
        while (true) {
            Integer resultantUserId = SecurityEmployee.
                getWbuIdForWbtIdAndRoleIdNoHierarchy(
                dbc,
                wbtId, roleId, new java.sql.Date(date.getTime()));
            if (resultantUserId != null) {
                ret = new WorkbrainUserAccess(dbc).loadByWbuId(resultantUserId.intValue());
                break;
            }
            // *** traverse all up to Workbrain Root
            WorkbrainTeamData wbtd = wbta.load(wbtId);
            wbtId = wbtd.getWbtParentId();
            if (wbtd.getWbtId() == wbtd.getWbtParentId()) {
                break;
            }
            // *** sanity check, if it reached 100, smt is wrong
            if (cnt ++ == 100) {
                break;
            }
        }
        return ret;
    }

}