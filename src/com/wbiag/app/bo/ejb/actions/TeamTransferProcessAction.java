/*
 * Created on Jun 2, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.wbiag.app.bo.ejb.actions;

import java.util.List;
import java.util.StringTokenizer;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.app.workflow.*;
import com.workbrain.sql.*;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;


import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;

import com.workbrain.app.modules.retailSchedule.db.*;
import com.workbrain.app.modules.retailSchedule.model.*;

import com.workbrain.tool.security.SecurityEmployee;
import com.workbrain.tool.security.SecurityUser;




/**
 * @author bhacko
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TeamTransferProcessAction extends AbstractActionProcess {
    private static org.apache.log4j.Logger logger =
        org.apache.log4j.Logger.getLogger(TeamTransferProcessAction.class);

    public static final String PARAM_ROLE = "dblAssignRole";
    public static final String PARAM_ROLE_TEAMS = "dblAssignRoleToTeam";
    public static final String PARAM_EMP = "dblEmpToTransfer";
    public static final String PARAM_STAFF_GRP = "dblStaffGroup";
    public static final String PARAM_DEST_TEAM = "dblTransferToTeam";
    public static final String PARAM_ROLE_EFF_DATE = "dpRoleEffDate";
    public static final String PARAM_DEST_EFF_DATE = "dpTeamEffDate";

    public static final String LABEL_ROLE = "lblAssignRole";
    public static final String LABEL_ROLE_TEAMS = "lblAssignRoleToTeam";
    public static final String LABEL_EMP = "lblEmpToTransfer";
    public static final String LABEL_STAFF_GRP = "lblStaffGroup";
    public static final String LABEL_DEST_TEAM = "lblTransferToTeam";
    public static final String LABEL_ROLE_EFF_DATE = "lblRoleEffDate";
    public static final String LABEL_DEST_EFF_DATE = "lblTeamEffDate";

    public static final String PARAM_IS_CUR_SUPER_TEAM = "hidIsCurMgrsTeam";
    public static final String PARAM_IS_LFSO_ENABLED = "hidIsLFSOEnabled";
    public static final String PARAM_SUPER_ROLE = "hidSuperRoleId";

    public static final String PARAM_WBU_EMP = "wbuEmpToTransfer";
    public static final String PARAM_WBU_USR = "wbuSender";

    public static final String OUT_INTERNAL = "Internal";
    public static final String OUT_EXTERNAL = "External";
    public static final String OUT_FAILURE = "Failure";

    private static final String ERR_BEFORE = " - ";
    private static final String ERR_AFTER = "\n";
    private static final String ERR_NULL = "null";

    private static final String HOME_TEAM = "Y";

    private static final Date SQL_DATE_3000 = new Date(DateHelper.DATE_3000.getTime());

    protected Action data = null;
    protected WBObject object = null;
    protected DBConnection conn = null;
    protected CodeMapper cm = null;
	protected String retVal = OUT_INTERNAL;

	protected com.workbrain.app.ta.db.EmployeeAccess empA = null;
    protected WorkbrainTeamAccess wbteamA = null;
    protected WorkbrainRoleAccess wbroleA = null;
    protected WorkbrainUserAccess wbuserA = null;
    protected EmployeeGroupAccess so_empgrpA = null;
    protected com.workbrain.app.modules.retailSchedule.db.EmployeeAccess so_empA = null;



	/* (non-Javadoc)
	 * @see com.workbrain.app.workflow.ActionProcess#processObject(com.workbrain.app.workflow.Action, com.workbrain.app.workflow.WBObject, com.workbrain.app.workflow.Branch[], com.workbrain.app.workflow.ActionResponse)
	 */
	public ActionResponse processObject(Action data, WBObject object,
			Branch[] outputs, ActionResponse previous) throws WorkflowEngineException {

		StringBuffer errMissing = new StringBuffer();
		StringBuffer errInputs = new StringBuffer();


		this.data = data;
		this.object = object;
		this.conn = this.getConnection();

        try {
			this.cm = CodeMapper.createCodeMapper(this.conn);

			boolean blIsCurSuperTeam = getIsCurSuperTeam();

	        if (!blIsCurSuperTeam) {
	        	return WorkflowUtil.createActionResponse(outputs, OUT_EXTERNAL);
	        }

			boolean blIsLFSOEnabled = getIsLFSOEnabled();

			Integer intRole = null;
			Integer[] intaRoleTeams = null;
			Integer intEmp = null;
			Integer intStaffGroup = null;
			Integer intDestTeam = null;
			Date dtRoleEffDate = null;
			Date dtDestEffDate = null;
			Integer intSuperRole = null;

			String lblRole = getLblRole();
	        String lblRoleTeams = getLblRoleTeams();
	        String lblEmp = getLblEmp();
	        String lblStaffGroup = getLblStaffGroup();
	        String lblDestTeam = getLblDestTeam();
	        String lblRoleEffDate = getLblRoleEffDate();
	        String lblDestEffDate = getLblDestEffDate();

	        EmployeeTeamAccess empTA = null;
	        EmployeeTeamData curEmpHomeTD = null;
	        List curEmpTeamList = null;
	        java.sql.Date curEmpHomeEndDate = SQL_DATE_3000;

	        Integer intUserID = null;

        	Integer so_intSkdgrpID = null;
        	Employee so_empD = null;

	        boolean blHomeTeamFound = false;
	        boolean blDestTeamExists = false;

	        intSuperRole = getSuperRole();

	        dtDestEffDate = getDestEffDate();
        	if (dtDestEffDate == null) {
        		appendMissing(errMissing, lblDestEffDate, dtDestEffDate);
        	}

	        intDestTeam = getDestTeam();
        	blDestTeamExists = teamExists(intDestTeam);
	        if (intDestTeam == null
	        		|| !blDestTeamExists) {
	        	appendMissing(errMissing, lblDestTeam, intDestTeam);
	        }

	        intEmp = getEmp();
        	if (intEmp == null
            		|| !empExists(intEmp, dtDestEffDate)) {
            		appendMissing(errMissing, lblEmp, intEmp);
            }

			intRole = getRole();
			if (intRole != null) {
				if (!roleExists(intRole)) {
		        	appendMissing(errMissing, lblRole, intRole);
		        } else {
		        	intUserID = getUserID(intEmp);
		        	if (intUserID == null) {
		        		errInputs.append("Employee (" ).append(intEmp)
						.append(") has no user.  Role not assigned!  ");
		        	}
					intaRoleTeams = getRoleTeams();
					if (intaRoleTeams.length > 0) {
			        	for (int rtIndex = 0; rtIndex < intaRoleTeams.length;rtIndex++) {
			        		if (!teamExists(intaRoleTeams[rtIndex])) {
			        			appendMissing(errMissing, lblRoleTeams, intaRoleTeams[rtIndex]);
			        		}
			        	}
					} else {
			        	appendMissing(errMissing, lblRoleTeams, "None Selected");
					}
			        dtRoleEffDate = getRoleEffDate();
		        	if (dtRoleEffDate == null) {
		        		appendMissing(errMissing, lblRoleEffDate, dtRoleEffDate);
		        	}
		        }
	        }

	        if (blIsLFSOEnabled) {
		        intStaffGroup = blIsLFSOEnabled ? getStaffGroup() : null;
	        	if (intStaffGroup == null || !staffGroupExists(intStaffGroup)) {
		        	appendMissing(errMissing, lblStaffGroup, intStaffGroup);
	        	}
	        	if (blDestTeamExists) {
		        	so_intSkdgrpID = getSkdGrpIDFromTeam(intDestTeam);
	        		if (so_intSkdgrpID == null)
		        		errInputs.append("Team (" ).append(intDestTeam)
						.append(") has no Schedule Group. None assigned to ")
						.append("Employee (").append(intEmp).append(").  ");
	        	}
	        	so_empD = getSOEmp(intEmp);
	        	if (so_empD == null) {
	        		errInputs.append("Employee (" ).append(intEmp)
					.append(") has no SO record.");
	        	}
	        }

        	if (errMissing.length() != 0 || errInputs.length() != 0) {
        		throw new WorkflowEngineException(errInputs.insert(0,
					"The following problems with the form inputs were found:\n")
					.append(errMissing.insert(0,
					"The following fields were missing or invalid:\n").toString())
					.toString());
        	}

        	empTA = new EmployeeTeamAccess(this.conn);
			curEmpTeamList = empTA.load(intEmp.intValue(), dtDestEffDate);

	        int tlCount = curEmpTeamList.size();
	        int tlIndex = 0;

        	/*codemapper?*/

	        while (!blHomeTeamFound && tlIndex < tlCount) {
	        	curEmpHomeTD = (EmployeeTeamData) curEmpTeamList.get(tlIndex);
	        	if (HOME_TEAM.equals(curEmpHomeTD.getEmptHomeTeam())) {
	        		curEmpHomeEndDate = new Date(curEmpHomeTD.getEmptEndDate().getTime());
	        		if (curEmpHomeEndDate.compareTo(dtDestEffDate) != -1) {
		        		blHomeTeamFound = true;
	        		}
	        	}
	        	tlIndex++;
	        }

	        SecurityEmployee.setHomeTeam(
				this.conn,  		//c
				intEmp,				//empId
				intDestTeam,		//wbtId
				dtDestEffDate,		//startDate
				curEmpHomeEndDate,	//endDate
				true,				//deleteHomeTeams
				"","","","","",		//flag
				"","","","",""		//udf
	        );

		    if (intRole != null) {
		        for (int rtIndex = 0; rtIndex < intaRoleTeams.length;
		        	rtIndex++) {
		        	SecurityUser.addTeam(
		        			this.conn, 			   	//c
		        			intaRoleTeams[rtIndex],	//wbtId
		        			intUserID,				//wbuId
							intRole,				//wbroleId
							null,					//wbutId
							true,					//isRecursive
							false,					//overrideExists
							dtRoleEffDate,			//startDate
							SQL_DATE_3000,			//endDate
							"","","","","",			//flag
							"","","","",""			//udf
	       			);
	        	}
		    }

	        if (blIsLFSOEnabled) {
        		so_empD.setEmpgrpId(intStaffGroup.intValue());
        		so_empD.setSkdgrpId(so_intSkdgrpID.intValue());
	        	so_empA.update(so_empD);
	        }

	        conn.commit();

        } catch (Throwable t) {
        	try {
        		if (conn != null) conn.rollback();
        	} catch (SQLException e) {
                logger.error("Failed to rollback in Team Transfer Process Action", e);
        	}

            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger.error("Error processing Team Transfer Request", t);
            }
            throw new WorkflowEngineException (t);
        } finally {
       		this.close(conn);
        }

        return WorkflowUtil.createActionResponse(outputs, OUT_INTERNAL);

	}

	private Integer getRole()
	throws WorkflowEngineException {
		String tmp = WorkflowUtil.getFieldValueId(this.object,
				PARAM_ROLE);
		return StringHelper.isEmpty(tmp) ? null : new Integer(tmp);
	}

	private Integer[] getRoleTeams ()
	throws WorkflowEngineException {
		String tmp = WorkflowUtil.getFieldValueId(this.object, PARAM_ROLE_TEAMS);
		Integer[] intRoleTeams = null;
		if (!StringHelper.isEmpty(tmp)) {
			StringTokenizer strRoleTeams = new StringTokenizer(tmp, ",");
			intRoleTeams = new Integer[strRoleTeams.countTokens()];
			int rtIndex = 0;
			while (strRoleTeams.hasMoreTokens()) {
				intRoleTeams[rtIndex] = new Integer(strRoleTeams.nextToken());
				rtIndex++;
			}
		}
		return intRoleTeams;
	}

	private Integer getEmp ()
	throws WorkflowEngineException {
		String tmp = WorkflowUtil.getFieldValueId(this.object,
				PARAM_EMP);
		return StringHelper.isEmpty(tmp) ? null : new Integer(tmp);
	}

	private Integer getStaffGroup ()
	throws WorkflowEngineException {
		String tmp = WorkflowUtil.getFieldValueId(this.object, PARAM_STAFF_GRP);
		return StringHelper.isEmpty(tmp) ? null : new Integer(tmp);
	}

	private Integer getDestTeam ()
	throws WorkflowEngineException {
		String tmp = WorkflowUtil.getFieldValueId(this.object, PARAM_DEST_TEAM);
		return StringHelper.isEmpty(tmp) ? null : new Integer(tmp);
	}

	private java.sql.Date getRoleEffDate ()
	throws WorkflowEngineException {
		String tmp = WorkflowUtil.getFieldValueAsString(this.object,
				PARAM_ROLE_EFF_DATE);
		return StringHelper.isEmpty(tmp) ? null :
			Date.valueOf(tmp.substring(0,4) + "-" + tmp.substring(4,6) + "-" +
					tmp.substring(6,8));
	}

	private java.sql.Date getDestEffDate ()
	throws WorkflowEngineException {
		String tmp = WorkflowUtil.getFieldValueAsString(this.object,
				PARAM_DEST_EFF_DATE);
		return StringHelper.isEmpty(tmp) ? null :
			Date.valueOf(tmp.substring(0,4) + "-" + tmp.substring(4,6) + "-" +
					tmp.substring(6,8));

	}

	private boolean getIsCurSuperTeam ()
	throws WorkflowEngineException {
		return (boolean) "true".equals(WorkflowUtil.getFieldValueAsString(
				this.object, PARAM_IS_CUR_SUPER_TEAM));
	}

	private boolean getIsLFSOEnabled ()
	throws WorkflowEngineException {
		return (boolean) "true".equals(WorkflowUtil.getFieldValueAsString(
				this.object, PARAM_IS_LFSO_ENABLED));
	}

	private Integer getSuperRole ()
	throws WorkflowEngineException {
		String tmp = WorkflowUtil.getFieldValueAsString(this.object,
				PARAM_SUPER_ROLE);
		return StringHelper.isEmpty(tmp) ? null : new Integer(tmp);
	}

	private String getLblRole ()
	throws WorkflowEngineException {
		return WorkflowUtil.getFieldValueAsString(this.object,LABEL_ROLE);
	}

	private String getLblRoleTeams ()
	throws WorkflowEngineException {
		return WorkflowUtil.getFieldValueAsString(this.object,
				LABEL_ROLE_TEAMS);
	}

	private String getLblEmp ()
	throws WorkflowEngineException {
		return WorkflowUtil.getFieldValueAsString(this.object, LABEL_EMP);
	}

	private String getLblStaffGroup ()
	throws WorkflowEngineException {
		return WorkflowUtil.getFieldValueAsString(this.object, LABEL_STAFF_GRP);
	}

	private String getLblDestTeam ()
	throws WorkflowEngineException {
		return WorkflowUtil.getFieldValueAsString(this.object, LABEL_DEST_TEAM);
	}

	private String getLblRoleEffDate ()
	throws WorkflowEngineException {
		return WorkflowUtil.getFieldValueAsString(this.object,
				LABEL_ROLE_EFF_DATE);
	}

	private String getLblDestEffDate ()
	throws WorkflowEngineException {
		return WorkflowUtil.getFieldValueAsString(this.object,
				LABEL_DEST_EFF_DATE);
	}

	private void appendMissing (StringBuffer errMissing, String strLabel,
			Object objField) {
		errMissing.append(ERR_BEFORE).append(strLabel).append(objField != null
				? objField : ERR_NULL).append(ERR_AFTER);
	}

	private boolean empExists (Integer intEmp, Date dtDestEffDate) {
        empA = empA == null ? new
		com.workbrain.app.ta.db.EmployeeAccess(this.conn, cm) : empA;
		try {
			return empA.load(intEmp.intValue(), dtDestEffDate) != null;
		} catch (Throwable t) {
			return false;
		}
	}

	private boolean teamExists (Integer intDestTeam) {
		wbteamA = wbteamA == null ? new WorkbrainTeamAccess(this.conn)
				: wbteamA;
		try {
			return 	wbteamA.load(intDestTeam.intValue()) != null;
		} catch (Throwable t) {
			return false;
		}
	}

	private boolean roleExists (Integer intRole) {
		wbroleA = wbroleA == null ? new WorkbrainRoleAccess(this.conn)
				: wbroleA;
		try {
			return wbroleA.load(intRole.intValue()) != null;
		} catch (Throwable t) {
			return false;
		}
	}

	private Integer getUserID (Integer intEmp) {
        WorkbrainUserData wbuD = null;
		wbuserA = wbuserA == null ? new WorkbrainUserAccess(this.conn)
				: wbuserA;
		try {
			wbuD = wbuserA.loadByEmpId(intEmp.intValue());
			return new Integer(wbuD.getWbuId());
		} catch (Throwable t) {
			return null;
		}
	}

	private boolean staffGroupExists (Integer intStaffGroup) {
		so_empgrpA = so_empgrpA == null ? new EmployeeGroupAccess(this.conn)
				: so_empgrpA;
		try {
			return so_empgrpA.loadRecordDataByPrimaryKey(
					new EmployeeGroup(),
					intStaffGroup.intValue()) != null;
		} catch (Throwable t) {
			return false;
		}
	}

	private Integer getSkdGrpIDFromTeam( Integer intDestTeam) {
		try {
			return ScheduleGroupAccess.getScheduleGroupFromTeam(
					intDestTeam.intValue());
		} catch (Throwable t) {
			return null;
		}
	}

	private Employee getSOEmp(Integer intEmp) {
		so_empA = so_empA == null ? new com.workbrain.app.modules.
				retailSchedule.db.EmployeeAccess(this.conn)
				: so_empA;
		try {
			return so_empA.loadByEmpId(intEmp.intValue());
		} catch (Throwable t) {
			return null;
		}
	}

}

