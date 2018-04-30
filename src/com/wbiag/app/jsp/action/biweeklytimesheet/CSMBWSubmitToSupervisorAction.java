package com.wbiag.app.jsp.action.biweeklytimesheet;

import com.workbrain.tool.overrides.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.jsp.*;
import com.workbrain.server.WebSession;
import com.workbrain.server.jsp.locale.LocalizationDictionary;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;
import com.workbrain.tool.mail.*;
import com.workbrain.tool.security.*;
import com.workbrain.tool.security.SecurityException;
import com.workbrain.app.jsp.action.AbstractActionFactory;

import javax.servlet.*;

import java.sql.SQLException;
import java.util.*;


public class CSMBWSubmitToSupervisorAction extends CSMSaveBWTimeSheetAction implements CSMBiWeeklyTimeSheetConstants {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CSMBWSubmitToSupervisorAction.class);

    class GetSupervisorListCmd extends EmployeeIdStartEndDateSet.BasicAbstractCommand {
        private DBConnection conn;
        private CodeMapper codeMapper;
        private Set supervisorWbuIdSet = new HashSet();

        public GetSupervisorListCmd(DBConnection conn) throws SQLException {
            this.conn = conn;
            this.codeMapper = CodeMapper.createCodeMapper(conn);
        }
        /*
         * accumulate a list of supervisorWbuId
         */
        public void execute(EmployeeIdStartEndDate empIdSEDate)
                throws Exception {
            int supervisorId;
            try {
                supervisorId = SecurityEmployee.getUserForEmployeeRole(
                        conn,empIdSEDate.getEmpId(),
                        SecurityHelper.getSupervisorRoleId(this.codeMapper),
                        new java.sql.Date(empIdSEDate.getStartDate().getTime()));
                this.supervisorWbuIdSet.add(new Integer(supervisorId));
            } catch (SecurityException se) {
                logger.error(se);
                throw se;
            }
        }

      public List getSupervisorNames() throws Exception {
            ArrayList retVal = new ArrayList();

            if (this.supervisorWbuIdSet.size() == 0) {
                retVal.add("workbrain");
                return retVal;
            }

            WorkbrainUserAccess userAccess = new WorkbrainUserAccess(conn);
            int supervisorId = -1;
            for (Iterator iter = this.supervisorWbuIdSet.iterator(); iter.hasNext(); ) {
                supervisorId = ((Integer)iter.next()).intValue();
                try {
                    WorkbrainUserData user = userAccess.loadByWbuId(supervisorId);
                    String userName = (String) user.getField("WBU_NAME");
                    retVal.add(userName);
                } catch (SQLException se) {
                    logger.error(se);
                }
            } // end for
            return retVal;
        }

    } // end of GetSupervisorListCmd




    public final Object createRequest( ServletRequest request, DBConnection conn )
            throws Exception {
        String addComment = request.getParameter( "SUP_COMMENT" );
        return new Object[] {addComment, request};
    }

    public final String process( DBConnection conn, ActionContext context,
                           Object requestObj )
            throws Exception {

        String addComment = ((String)(((Object[])requestObj)[0])) + "<br><br>";
        // retreive employee Id and Week Start Date
        CSMBiWeeklyTimeSheetPage model = (CSMBiWeeklyTimeSheetPage)context.getAttribute(
                "timesheet" );

        model.setSubmitted(true);
        AbstractOverrideBuilder ovrBuilder = new OverrideBuilder(conn);
        setSubmitted( model, ovrBuilder, true);
        Object saveRequest = super.createRequest((ServletRequest)(((Object[])requestObj)[1]), conn);
        ovrBuilder = super.doProcess( conn, context, saveRequest, ovrBuilder );

        ovrBuilder.setDirection("TS");
        List empIdList = new ArrayList();
        empIdList.add( String.valueOf( model.getEmployee().getEmpId() ) );
        ovrBuilder.setEmpIdList( empIdList );
        ovrBuilder.setFirstStartDate( model.getWeekStartDate() );
        ovrBuilder.setLastEndDate( model.getWeekEndDate() );
        ovrBuilder.execute(true);

        WebSession ws = (WebSession) context.getAttribute(com.workbrain.server.WebConstants.WEB_SESSION_ATTRIBUTE_NAME);
        ws.setActionStatus("Your timesheet has been submitted to your supervisor.", "ACTION_STATUS_SUBMIT_TO_SUPERVISOR");

        int empId = model.getEmployee().getEmpId();
        String startDateStr = model.getWeekStartDateString();
        startDateStr = StringHelper.searchReplace(startDateStr, " " , "%20");

        // find employee's supervisor(s)
        GetSupervisorListCmd getSupLstCmd = new GetSupervisorListCmd(conn);
        getSupLstCmd.addEmp(empId, model.getWeekStartDate(), model.getWeekEndDate());
        model.getVisibleTeamDateSet().runCommandToOrigRange(getSupLstCmd);

        List supUserNameLst = getSupLstCmd.getSupervisorNames();
        for (Iterator iter=supUserNameLst.iterator(); iter.hasNext(); ) {
            String supervisorName = (String)iter.next();
            sendMessage(supervisorName, model, conn, addComment, startDateStr, context.getRequest().getContextPath());
        }

        //TODO: Replace with real action URL
        return CSMWTShelper.getActionURL(ACTION_VIEW_TIMESHEET);
    }


    protected void sendMessage(String userName,
    		CSMBiWeeklyTimeSheetPage model,
                             DBConnection conn,
                             String addComment,
                             String date,
                             String contextPath) throws Exception{
        Message message = new Message (conn);

        /* Find locale of the user receiving this message */
        WorkbrainUserAccess recp = new WorkbrainUserAccess(conn);
        WorkbrainUserData wud = recp.loadByWbuName(userName);
        int localeId = wud.getWbllId(); //set the locale id of the recipient

        message.setTo(userName);
        message.setSenderName(model.getWbuName());

        String defaultSubj = model.getEmployee().getFirstName()+ " " +
                             model.getEmployee().getLastName() + " (" +
                             model.getEmployee().getEmpName() + ") " +
                             " has submitted his/her timesheet";
        List args = new ArrayList();
        args.add(model.getEmployee().getFirstName());
        args.add(model.getEmployee().getLastName());
        args.add(model.getEmployee().getEmpName());
        String subject = LocalizationDictionary.localizeMessage(conn, "SUBMIT_TIMESHEET",
                         defaultSubj, args, localeId);

        message.setMessageSubject(subject);

        String bodyStr = LocalizationDictionary.localizeMessage(conn, "CLICK_VIEW_TIMESHEET",
                         "Click here to view timesheet", new ArrayList(), localeId);
        message.setMessageBody(addComment + "<a href='" + contextPath + CSMWTShelper.getActionURL(ACTION_LOAD_EMPLOYEE) +
                               "&WEEK_START_DATE=-99&AUTH_SELECT=0&FLAG=M&INC_EMPS=" +
                               model.getEmployee().getEmpId() + "&START_DATE=" +
                               date +
                               "' >" + bodyStr + "</a>");
        message.setMessageType(Util.MESSAGE_TYPE_MAIL);
        message.send();
    }

}
