package com.wbiag.app.wbinterface.schedulein;

import java.util.*;

import com.workbrain.app.modules.retailSchedule.db.*;
import com.workbrain.app.modules.retailSchedule.model.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.schedulein.*;
import com.workbrain.app.wbinterface.schedulein.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Transaction type for Shift Pattern data processing and marking SO_EMPLOYEE for employee as fixed shift.
 *
 * <p>Copyright: Copyright (c) 2002 Workbrain Inc.</p>
 *
 **/
public class ShiftPatternTransactionSO extends ShiftPatternTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ShiftPatternTransactionSO.class);

    public static final String PARAM_FIXED_SHIFT = "FixedShift";
    private int fixedShift;

    public ShiftPatternTransactionSO() {
    }

    /**
     * Override to get FixedShift parameter
     *
     */
    public void initializeTransaction(DBConnection conn) throws Exception{
        super.initializeTransaction(conn);
        String sFixedShift = (String)params.get(PARAM_FIXED_SHIFT);
        fixedShift = !StringHelper.isEmpty(sFixedShift)
                ? Integer.parseInt(sFixedShift)
                : 0 ;
        log ("fixedShift : " + fixedShift);
    }

    /**
     * Override to get FixedShift parameter
     * @return
     */
    public String getTaskUI() {
        return "/jobs/wbinterface/schedulein/shiftPatternParamsSO.jsp";
    }

    /**
     * Postprocesses import data. <p>
     * processedData is the manipulated import data and implementors
     * can make use of it to retrieve calculated/formatted values.
     *
     *@param  data          raw import data
     *@param  processedData processed import data
     *@param  conn          conn
     *@throws Exception
     */
    public void postProcess(ImportData data , Object processedData ,
            DBConnection conn) throws Exception{
        if (createsEmpOverride) {
            try {
                DBInterface.init(conn);
                com.workbrain.app.modules.retailSchedule.db.EmployeeAccess
                    soEmpAccess =
                    new com.workbrain.app.modules.retailSchedule.db.
                    EmployeeAccess(conn);
                Employee emp = loadSOEmployee(soEmpAccess, empId);
                if (emp != null) {
                    emp.setSempOnfixedSkd(fixedShift);
                    soEmpAccess.updateRecordData(emp,
                                                 Employee.TABLE_NAME,
                                                 Employee.
                                                 SO_EMPLOYEE_TABLE_PRI_KEY);
                    if (logger.isDebugEnabled())
                        logger.debug(
                            "SO_EMPLOYEE have been updated for fixed shift");
                }
            }
            finally {
                DBInterface.remove();
            }
        }
    }

    private Employee loadSOEmployee(
        com.workbrain.app.modules.retailSchedule.db.EmployeeAccess soEmpAccess,
        int empId) {
        List list = soEmpAccess.loadRecordData( new Employee(),Employee.TABLE_NAME, "emp_id" , empId);
        if (list.size() > 0) {
            return (Employee) list.get(0);
        } else {
            return null;
        }
    }

}



