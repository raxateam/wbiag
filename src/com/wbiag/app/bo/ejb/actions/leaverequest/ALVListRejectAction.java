
package com.wbiag.app.bo.ejb.actions.leaverequest;

import com.workbrain.app.workflow.AbstractActionProcess;
import com.workbrain.app.workflow.Action;
import com.workbrain.app.workflow.ActionResponse;
import com.workbrain.app.workflow.Branch;
import com.workbrain.app.workflow.WBObject;
import com.workbrain.app.workflow.WorkflowEngineException;

/**
 * Title: ALVListRejectAction.java <br>
 * Description: <br>
 * 
 * Created: Nov 28, 2005
 * @author cleigh
 * <p>
 * Revision History <br>
 * Nov 28, 2005 -  <br>
 * <p>
 * */
public class ALVListRejectAction extends AbstractActionProcess {

    /* (non-Javadoc)
     * @see com.workbrain.app.workflow.ActionProcess#processObject(com.workbrain.app.workflow.Action, com.workbrain.app.workflow.WBObject, com.workbrain.app.workflow.Branch[], com.workbrain.app.workflow.ActionResponse)
     */
    public ActionResponse processObject(Action data, WBObject object,
            Branch[] outputs, ActionResponse previous)
            throws WorkflowEngineException {
        // TODO Auto-generated method stub
        return null;
    }

}
