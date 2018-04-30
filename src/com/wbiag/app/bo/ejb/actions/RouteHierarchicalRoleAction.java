/**
 * RouteHierarchicalRoleAction.java <p>
 * This custom workbrain node is based on the core workbrain node, 
 * com.workbrain.app.bo.ejb.actions.RoleAction.  It was created to
 * handle the case where the submitted user is the supervisor of their
 * home team.  This node will find the supervisor
 * of the submitted user by searching up the team hierarchy to find the 
 * submitted user's supervisor. <p>
 * Created on 28-Jun-2006<p>
 * @author ghartl
 * @version 1.0
 */

package com.wbiag.app.bo.ejb.actions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.workbrain.app.bo.BOFormInstance;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.WorkbrainUserAccess;
import com.workbrain.app.ta.model.WorkbrainUserData;
import com.workbrain.app.workflow.Action;
import com.workbrain.app.workflow.ActionProcess;
import com.workbrain.app.workflow.ActionResponse;
import com.workbrain.app.workflow.Branch;
import com.workbrain.app.workflow.TickPeriodHandler;
import com.workbrain.app.workflow.WBObject;
import com.workbrain.app.workflow.Workflow;
import com.workbrain.app.workflow.WorkflowEngineException;
import com.workbrain.app.workflow.WorkflowProcess;
import com.workbrain.server.WebLocale;
import com.workbrain.server.jsp.locale.LocalizationDictionary;
import com.workbrain.server.registry.Registry;
import com.workbrain.server.sql.ConnectionManager;
import com.workbrain.sql.DBConnection;
import com.workbrain.tool.mail.Message;
import com.workbrain.tool.mail.Util;
import com.workbrain.tool.security.SecurityEmployee;
import com.workbrain.util.IntegerList;
import com.workbrain.util.PackedLocalizedString;
import com.workbrain.util.StringHelper;
import com.workbrain.util.UserDisplayHelper;
import com.workbrain.app.bo.ejb.actions.ActionProcessHelper;



public class RouteHierarchicalRoleAction implements ActionProcess,TickPeriodHandler {
    private static Logger logger = Logger.getLogger(RouteHierarchicalRoleAction.class);

    public ActionResponse processObject(Action data, WBObject object, Branch[] outputs, ActionResponse previous) throws WorkflowEngineException {
        String senderName = null;
        String to = null;
        DBConnection c = null;
        String roleName = null;
        Object desc = null;
        String routedFrom = null;
        String routedTo = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int[] userIDRefList;
        String empID;
       

        try {
            c = new DBConnection (ConnectionManager.getConnection());
            // checking if object_field is selected
            String objField = (String) data.getProperty("OBJECT_FIELD");
            if (objField.equalsIgnoreCase("TRUE")) {
                BOFormInstance boFI = (BOFormInstance) object;
                Map valObj = boFI.getFieldValue((String) data.getProperty("ROLE"));
                if (valObj == null || valObj.isEmpty() || valObj.get("valueId") == null){
                    throw new Exception("The field does not contain any reference to a role.");
                }
                else {
                    roleName = (String) valObj.get("valueId");
                }
             } 
             else {
                roleName = (String) data.getProperty("ROLE");

                if (roleName.equals("null")){
                    throw new Exception("Role property is not set.");
                }
            }

            // get role ID
            ps = c.prepareStatement(" SELECT WBROLE_ID FROM WORKBRAIN_ROLE WHERE WBROLE_NAME  = ?" );
            ps.setString(1, roleName.trim());
            rs = ps.executeQuery();
            if (!rs.next()){
                throw new Exception("Invalid role name : " + roleName);
            }
            int roleID = rs.getInt(1);
            rs.close();
            rs = null;

            // get description
            desc = data.getProperty("DESCRIPTION");

            ps = c.prepareStatement(
                    " SELECT BUSOBJ_NAME, WBU_ID_ORIGIN, WBU_NAME  FROM " +
                    " ( SELECT BUSOBJ_NAME, WBU_ID_ORIGIN, WBU_ID FROM BUSINESS_OBJECT WHERE BUSOBJ_ID = ? ) BO, WORKBRAIN_USER" +
                    " WHERE BO.WBU_ID = WORKBRAIN_USER.WBU_ID ");
            ps.setInt(1, object.getID());
            rs = ps.executeQuery();
            if (!rs.next()){
                throw new Exception("Invalid interaction ID : " + object.getID());
            }
            String boName = rs.getString(1);
            int originID = rs.getInt(2);
            senderName = rs.getString(3);
            rs.close();
            rs = null;



            String firstRecipient = "WORKBRAIN"; // use langid of workbrain as default
            int userIDRef;
            StringBuffer origWbuNames = new StringBuffer();
            String toAddress = null;
            String userNameRef = null;
            boolean delegationOrAlternationOccurred = false;
            if (Registry.getVarString("system/WORKBRAIN_PARAMETERS/SEND_TO_VIRTUAL_ROLES", "").equals("true"))
            {
                List list = SecurityEmployee.getAllUsersForUserRole(
                        c,
                        originID,
                        roleID,
                        new java.sql.Date(new java.util.Date().getTime()));
                int []usersIdRefs = ((IntegerList)list).toIntArray();
                StringBuffer toAddressBuf = new StringBuffer();

                String [] wbuNames = new String[usersIdRefs.length];

                int[] userIds = new int[usersIdRefs.length];
                for (int i = 0; i < usersIdRefs.length; i++)
                {
                    userIDRef = usersIdRefs[i];
                    userNameRef = ActionProcessHelper.getUserNameByID(
                                         userIDRef,c,ps,rs);
                    // alternation + delegation
                    int userId = ActionProcessHelper.routeBO(c,
                                                        CodeMapper.createCodeMapper(c),
                                                        new java.util.Date(),
                                                        boName,
                                                        userIDRef,
                                                        userNameRef,
                                                        new Integer(roleID));
                    if (userId != userIDRef)
                    {
                        if (delegationOrAlternationOccurred){
                            origWbuNames.append(",");
                        }
                        origWbuNames.append(userNameRef);

                        delegationOrAlternationOccurred = true;

                    }
                    ActionProcessHelper.EmployeeFullInfo fullInfo =
                            ActionProcessHelper.getFullInfo(null, new Integer(userId), null, c);

                    wbuNames[i] = fullInfo.getWbuName(); //delegated/alternate wbuNames
                    userIds[i] = userId; // for normalization
                }

                for (int i = 0; i < wbuNames.length; i++) {
                    try {
                        if (i > 0)
                        {
                            toAddressBuf.append(",");
                        }
                        else
                        {
                            firstRecipient = wbuNames[i];
                        }
                        toAddressBuf.append(wbuNames[i]);
                    } catch (Exception e) {
                        if (logger.isEnabledFor(org.apache.log4j.Level.INFO)) {
                            logger.info(e);
                        }
                    }
                }
                toAddress = toAddressBuf.toString();

                // need to generate list that will be displayed on interaction history
                to = UserDisplayHelper.denormalizeReceivers( c, UserDisplayHelper.WBU_ID, getUserList(userIds) );

            }
            else{
            
               //get hierarchical listing of role requested (e.g. supervisors)
                empID = WorkbrainUserAccess.retrieveEmpIdByWbuId(c, originID);
                userIDRefList = SecurityEmployee.getUsersForEmployeeRole(
                                            c, 
                                            Integer.parseInt(empID), 
                                            roleID, 
                                            new java.sql.Date(new java.util.Date().getTime()));
                /* find first user with the role (lowest in the hierarchy) 
                 * who is not the submitted user
                 */
                userIDRef = userIDRefList[0];
                for(int i = 0; i < userIDRefList.length; i++){
                    if(userIDRefList[i] != originID){
                        userIDRef = userIDRefList[i]; 
                        break;
                    }
                }
                                                   
                userNameRef = ActionProcessHelper.getUserNameByID(
                    userIDRef,c,ps,rs);
                int userId = ActionProcessHelper.routeBO(c,
                                                    CodeMapper.createCodeMapper(c),
                                                    new java.util.Date(),
                                                    boName,
                                                    userIDRef,
                                                    userNameRef,
                                                    new Integer(roleID));
                if (userId != userIDRef) {
                    delegationOrAlternationOccurred = true;
                    origWbuNames.append(userNameRef);
                }
                ActionProcessHelper.EmployeeFullInfo fullInfo =
                        ActionProcessHelper.getFullInfo(null, new Integer(userId), null, c);
                toAddress = fullInfo.getWbuName();
                firstRecipient = toAddress;
                to = UserDisplayHelper.denormalizeReceivers( c, UserDisplayHelper.WBU_ID, String.valueOf(userId) );
            }

            String senderSubjectName = ActionProcessHelper.getInteractionSubjectNameDisplay(null, senderName, null, c);

            WorkbrainUserAccess wua = new WorkbrainUserAccess(c);
            WorkbrainUserData wud = wua.loadByWbuName(firstRecipient);
            int langId = wud.getWbllId();

            Message m = new Message(c);
            m.setTo(toAddress);
            m.setMessageBody("");
            java.util.Date now = new java.util.Date();
            String date = now.toString();
            String defaultSubject =  "Interaction ''" + boName + "'' from " + senderSubjectName + " on " + date;

            List args = new ArrayList();
            String localizedFormName = LocalizationDictionary.get().localizeData(c, boName, "WORKFLOW_FLOW", "WFFLOW_NAME");
            if (localizedFormName == null){
                localizedFormName = boName;
            }
            args.add(localizedFormName);
            args.add(senderSubjectName);

            Locale loc = (WebLocale.get(c,langId)).getLocale();
            SimpleDateFormat df = new SimpleDateFormat("EEE MMMMM d h:mm:ss a z yyyy", loc);
            args.add(df.format(now));

            String subject = LocalizationDictionary.localizeMessage(c, "AP_ROLE_ACTION_SUBJECT",
                    defaultSubject, args, langId);
            m.setMessageSubject(subject);
            m.setMessageType(Util.MESSAGE_TYPE_BO);
            m.setSenderName(senderName);
            m.setSaveMessageCopy(false);
            m.send();
            c.commit();
            //update message
            ActionProcessHelper.updateMessage(
                    m.getMessageId(),
                    object.getID(),
                    ActionProcessHelper.MESSAGE_TYPE_REGULAR,
                    c,ps,rs);
            // routing message
            if (delegationOrAlternationOccurred) {
                routedFrom = origWbuNames.toString(); //  list of users that delegated/alerternated
                routedTo = toAddress;
            }
            c.commit();
        } catch (Exception e) {
            try {
                if (c!=null){ 
                    c.rollback();
                }
            } catch (SQLException sqle) {
                if (logger.isEnabledFor(Level.ERROR)) { 
                    logger.error(sqle);
                }
                throw new WorkflowEngineException(sqle.getMessage());
            }
            if (logger.isEnabledFor(Level.DEBUG)) { 
                logger.debug("Exception at the Role Node: " + e.getMessage());
            }
            String errMessage = e.getMessage();
            if( e instanceof com.workbrain.tool.security.SecurityException ) {
                errMessage = roleName + " role not found for user " + senderName;
            }
            return new ActionResponse (ActionResponse.EXCEPTION, errMessage, senderName);
        } finally {
            try {
                if (rs!=null){ 
                    rs.close();
                }
                if (ps!=null){ 
                    ps.close();
                }
                if (c!=null){ 
                    c.close();
                }
            } catch (SQLException sqle) {
                throw new WorkflowEngineException(sqle.getMessage());
            }
        }
        return new ActionResponse (ActionResponse.PAUSE, null, (StringHelper.isEmpty(desc)
                                                                    ? ""
                                                                    : "Node Name : " + new PackedLocalizedString(desc.toString()) + "<br>") +
                                                               "Role : " + roleName + "<br>Employee : " + to, routedFrom, routedTo);
    }


    /**
     * Returns a string containts the comma delimited ints
     * @param usersIdRefs
     * @return
     */
    private String getUserList(int[] usersIdRefs) {
        StringBuffer userList = new StringBuffer();

        for (int i = 0; i < usersIdRefs.length; i++)
        {
            if (i != 0){
                userList.append(",");
            }
            userList.append(usersIdRefs[i]);
        }

        return userList.toString();
    }


    public boolean tickExpired(WBObject object, WorkflowProcess process, Workflow workflow) throws WorkflowEngineException {
        ActionProcessHelper.deleteMessageAndExcept(object);
        ((BOFormInstance)object).setStatusName("EXCEPTION");
        //Let engine take care of rest
        return false;
    }

}
