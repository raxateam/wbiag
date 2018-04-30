package com.wbiag.app.bo.ejb.actions;

import java.sql.*;
import java.util.Date;

import com.wbiag.app.export.payroll.PayrollExporter;
import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.EmployeeAccess;
import com.workbrain.app.workflow.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

public class ResetEmpPaidAction
    extends AbstractActionProcess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(ResetEmpPaidAction.class);

    private static final String CYCLE_NEW = "New Employee";
    private static final String CYCLE_TERM = "Terminated Employee";

    private static final String MODE = PayrollExporter.MODE_REGULAR;

    //Params
    private static final String PARAM_EMP_ID    = "emp_ids";
    private static final String PARAM_EMP_NAMES = "emp_names";
    private static final String PARAM_START_DATE = "start_date";
    private static final String PARAM_END_DATE = "end_date";


   //Export Variables
    private int [] empIds = null;
    protected Date startDate = null;
    protected Date endDate = null;
    protected String cycle = PayrollExporter.CYCLE_REGULAR;

    
    public ActionResponse processObject(Action data, WBObject object,
                                        Branch[] outputs,
                                        ActionResponse previous) throws
        WorkflowEngineException {
        if (logger.isInfoEnabled()){
            logger.info("ResetEmpPaidAction.processObject");
        }

        DBConnection conn = null;
        try {
            conn = this.getConnection();
            getParams(object, data);
            
            unpayRecords(conn);
            conn.commit();
        }
        catch (Throwable t) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger.error(
                    "com.workbrain.app.bo.ejb.actions.ResetEmpPaidAction.class",
                    t);
            }
            this.rollback(conn);
            throw new WorkflowEngineException(t);
        }
        finally {
            this.close(conn);
        }

        return WorkflowUtil.createActionResponse(outputs, "Success");

    }
    
    public void unpayRecords(DBConnection conn) throws Exception {
        
        PayrollExporter payExp = null;
        
        if ( (empIds != null) && (empIds.length != 0) ) {
            payExp = new PayrollExporter(conn, MODE);
            payExp.setCycle(PayrollExporter.CYCLE_UNPAY);
            payExp.setEmpIds(empIds);
            payExp.setDates(startDate, endDate);
            payExp.unpayRecords();
        }
    }

    private void getParams(WBObject object, Action data) throws WorkflowEngineException, Exception {
//      Required Parameters
        

        //Semi Optional Parameters

        if (WorkflowUtil.fieldExists(object, PARAM_EMP_ID)) {
            empIds = detokenizeStringAsIntArray(
                WorkflowUtil.getFieldValueId(object, PARAM_EMP_ID),
                ",");
        }
        else if (WorkflowUtil.fieldExists(object, PARAM_EMP_NAMES)) {
            empIds = detokenizeStringEmpNameAsIntEmpIdArray(
                WorkflowUtil.getFieldValueId(object, PARAM_EMP_NAMES),
                ",");
        }
        else  {
            throw new WorkflowEngineException(new StringBuffer().append(PARAM_EMP_ID)
                    .append(" must be provided on form ").toString());
        }
  
        //Optional Params
        if (WorkflowUtil.fieldExists(object, PARAM_START_DATE)) {
            if (WorkflowUtil.isFieldEmpty(object, PARAM_START_DATE)) {
                throw new WorkflowEngineException(new StringBuffer()
                        .append(PARAM_END_DATE).append(" is empty. ")
                        .toString());
            }
            startDate = WorkflowUtil.getFieldValueAsDate(object,
                    PARAM_START_DATE);
        } else {
            throw new WorkflowEngineException(new StringBuffer().append(
                    PARAM_START_DATE).append(" not provided on form. ")
                    .toString());
        }
        if (WorkflowUtil.fieldExists(object, PARAM_END_DATE)) {
            if (WorkflowUtil.isFieldEmpty(object, PARAM_END_DATE)) {
                throw new WorkflowEngineException(new StringBuffer()
                        .append(PARAM_END_DATE).append(" is empty. ")
                        .toString());
            }
            endDate = WorkflowUtil.getFieldValueAsDate(object,
                    PARAM_END_DATE);
        } else {
            throw new WorkflowEngineException(new StringBuffer().append(
                    PARAM_END_DATE).append(" not provided on form. ")
                    .toString());
        }
    }
    
    private int[] detokenizeStringAsIntArray(String input, String separator) {
        String[] st = StringHelper.detokenizeString(input, separator);
        if (st == null || st.length == 0) {
            return null;
        }
        int[] stArray = new int[st.length];
        for (int i = 0; i < st.length; i++) {
            stArray[i] = Integer.parseInt(st[i]);
        }
        return stArray;
    }
    
    private int[] detokenizeStringEmpNameAsIntEmpIdArray(String input,
            String separator) throws Exception {
            String[] st = StringHelper.detokenizeString(input, separator);
            EmployeeAccess ea = new EmployeeAccess(this.getConnection(),
                                                   CodeMapper.createCodeMapper(this.getConnection()));
            if (st == null || st.length == 0) {
                return null;
            }
            int[] stArray = new int[st.length];
            for (int i = 0; i < st.length; i++) {
                int empId = ea.loadByName(st[i], DateHelper.getCurrentDate()).
                    getEmpId();
                stArray[i] = empId;
            }
            return stArray;
        }
    
}
