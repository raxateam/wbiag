package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.EmployeeGroupAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.SQLRetailException;
import com.workbrain.app.modules.retailSchedule.model.*;

/**
 * Customization for SO_EMPLOYEE extra data
 *
 **/
public class HRRefreshTransactionSOExtra extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionSOExtra.class);

    private static final int SO_EXTRA_COL = 90;
    public static final String SKDGRP_NAME = "SKDGRP_NAME";
    public static final String SKDGRP_ID = "SKDGRP_ID";    
    public static final String EMPGRP_NAME = "EMPGRP_NAME";
    public static final String EMPGRP_ID = "EMPGRP_ID"; 
    public static final String SEMP_EFF_DATE = "SEMP_EFF_DATE";
    public static final String SEMP_SKDMAX_HRS = "SEMP_SKDMAX_HRS";
    public static final String SEMP_SKDMIN_HRS ="SEMP_SKDMIN_HRS";
    public static final String SEMP_DAYMAX_HRS = "SEMP_DAYMAX_HRS";
    public static final String SEMP_ONFIXED_SKD = "SEMP_ONFIXED_SKD";
    public static final String SEMP_MAX2NT_RULE = "SEMP_MAX2NT_RULE";
    public static final String SEMP_MAXSHFTDAY = "SEMP_MAXSHFTDAY";
    public static final String SEMP_X_IN_DATE = "SEMP_X_IN_DATE";
    public static final String SEMP_X_OUT_DATE = "SEMP_X_OUT_DATE";
    public static final String SKDGRP_CLIENTKEY = "SKDGRP_CLIENTKEY";
    public static final String SEMP_SHFTMIN_HRS = "SEMP_SHFTMIN_HRS";
    public static final String SEMP_SEASONAL_REG = "SEMP_SEASONAL_REG";
    public static final String SEMP_IS_SALARY = "SEMP_IS_SALARY"; 
    public static final String SEMP_IS_MINOR = "SEMP_IS_MINOR";
    public static final String SEMP_EXEMPT_STAT = "SEMP_EXEMPT_STAT"; 
    public static final String PREFERRED_EMPJOB_NAME = "PREFERRED_EMPJOB_NAME";

    private static final String EMPJOBPREF_UPDATE_SQL
        = "UPDATE employee_job SET empjob_preferred = 'Y' WHERE job_id=? AND emp_id = ?";

    private int[] custColInds = new int[] {SO_EXTRA_COL};
    private DBConnection conn = null;
    private CodeMapper codeMapper = null;
    private Map skdgrpClient = new HashMap();

    /**
     * Sets custom column indexes to be used by the customization. Indexes start at 0
     * @param inds
     */
    public void setCustomColInds(int[] inds) {
        custColInds = inds;
    }

    /**
     * Override this class to customize after process batch events.
     * At this time, all HRRefreshTransactionData data has been processed.
     * All processed is available through <code>HRRefreshCache</code>.
     *
     * @param conn                          DBConnection
     * @param hrRefreshTransactionDataList  List of <code>HRRefreshTransactionData</code>
     * @param process                       HRRefreshProcessor
     * @throws Exception
     */
    public void postProcessBatch(DBConnection conn,
                                    List hrRefreshTransactionDataList,
                                    HRRefreshProcessor process) throws Exception {
        if (hrRefreshTransactionDataList == null || hrRefreshTransactionDataList.size() == 0) {
                return;
        }
        if (custColInds == null || custColInds.length != 1) {
            throw new WBInterfaceException ("Custom column index not supplied or too many, must be 1");
        }
        this.conn = conn;
        this.codeMapper = CodeMapper.createCodeMapper(conn);
        List updateSOEmployees = new ArrayList();
        List insertedSOEmployees = new ArrayList();        
        List updateEmpjobPref = new ArrayList();
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            try {
                String soExtraData = data.getImportData().getField(custColInds[0]);
                SOExtra soExtra = processSOExtra(soExtraData, data,  conn);
                if (soExtra != null) {
                    if (soExtra.soEmp != null) {
                        if (soExtra.isEmpUpdate    
                            && soExtra.soEmp.retrieveAssignedFields().size() > 0) {
                            updateSOEmployees.add(soExtra.soEmp);
                        }
                        else if (!soExtra.isEmpUpdate) {
                            insertedSOEmployees.add(soExtra.soEmp);
                        }
                    }
                    if (soExtra.prefJobId != null) {
                        updateEmpjobPref.add(soExtra);
                    }
                }
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionSOExtra." , ex);}
                data.error("Error in HRRefreshTransactionSOExtra." + ex.getMessage() );
            }
        }
        insertSOEmployee(insertedSOEmployees);
        updateSOEmployee(updateSOEmployees);
        updateEmpjobPref(updateEmpjobPref);
    }

    protected void updateSOEmployee(List updateSOEmployees) {
        if (updateSOEmployees == null || updateSOEmployees.size() == 0) {
            return;
        }
        try {
            DBInterface.init(conn);
            com.workbrain.app.modules.retailSchedule.db.EmployeeAccess
                soEmpAccess =
                new com.workbrain.app.modules.retailSchedule.db.
                EmployeeAccess(conn);
            soEmpAccess.updateRecordData(updateSOEmployees,
                                         Employee.TABLE_NAME,
                                         Employee.SO_EMPLOYEE_TABLE_PRI_KEY);
            if (logger.isDebugEnabled()) logger.debug(updateSOEmployees.size() + " SO_EMPLOYEE records have been updated for extra fields");
        }
        finally {
            DBInterface.remove();
        }
    }

    protected void insertSOEmployee(List insertedSOEmployees) {
        if (insertedSOEmployees == null || insertedSOEmployees.size() == 0) {
            return;
        }
        try {
            DBInterface.init(conn);
            com.workbrain.app.modules.retailSchedule.db.EmployeeAccess
                soEmpAccess =
                new com.workbrain.app.modules.retailSchedule.db.
                EmployeeAccess(conn);
            soEmpAccess.insertRecordData(insertedSOEmployees,
                                         Employee.TABLE_NAME);
            if (logger.isDebugEnabled()) logger.debug(insertedSOEmployees.size() + " SO_EMPLOYEE records have been created for extra fields");
        }
        finally {
            DBInterface.remove();
        }
    }
    
    protected void updateEmpjobPref(List updateEmpjobPref) throws Exception  {
        if (updateEmpjobPref == null || updateEmpjobPref.size() == 0) {
            return;
        }
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(EMPJOBPREF_UPDATE_SQL);
            Iterator iter = updateEmpjobPref.iterator();
            while (iter.hasNext()) {
                SOExtra item = (SOExtra)iter.next();
                if (item.prefJobId != null) {
                    ps.setInt(1, item.prefJobId.intValue());
                    ps.setInt(2, item.empId);
                    ps.addBatch();
                }
            }

            int[] upd = ps.executeBatch();
            if (logger.isDebugEnabled()) logger.debug(upd.length + " empjob_preferred have been updated");
        }
        finally {
            if (ps != null) ps.close();
        }

    }

    protected SOExtra processSOExtra(String soExtraValVal,
                               HRRefreshTransactionData data,
                               DBConnection c) throws Exception {
        // *** only do this if SO_EXTRA_COL is not blank in the file
        if (StringHelper.isEmpty(soExtraValVal)) {
            return null;
        }
        SOExtra soExtra = new SOExtra();
        soExtra.empId = data.getEmpId();
        soExtra.soEmp = loadSOEmployee(c , data.getEmpId());
        if (soExtra.soEmp != null) {
            try {
                soExtra.soEmp.setSempEffDate(data.getOvrStartDate());
                soExtra.isEmpUpdate = true;
                assignSOExtraValues(soExtra, soExtraValVal, c);
            }
            catch (Exception ex) {
                logger.error("Error in assigning SO Extra values" , ex);
                throw new NestedRuntimeException("Error in assigning SO Extra values" , ex);
            }
        }
        else {
            // SO_EMPLOYEE Create def record first and then assign values
            soExtra.soEmp = com.workbrain.app.modules.retailSchedule.db.EmployeeAccess.createDefaultSOEmployeeData();
            soExtra.soEmp.setSempEffDate(data.getOvrStartDate());
            soExtra.soEmp.setEmpId(data.getEmpId());
            soExtra.soEmp.setSempId(conn.getDBSequence(Employee.SEQ_PRIMARY_KEY).getNextValue());            
            soExtra.isEmpUpdate = false;            
            assignSOExtraValues(soExtra, soExtraValVal, c);            
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  {logger.debug("SO_EMPLOYEE record not found for empId :" + data.getEmpId() + ", created it"); }            
            data.appendWarning("SO_EMPLOYEE did not exist and was created for empId :" + data.getEmpId());
        }
        return soExtra;
    }

    protected void assignSOExtraValues(SOExtra soExtra ,
                                       String soExtraValVal,
                                       DBConnection c) throws Exception {
        List allValues = StringHelper.detokenizeStringAsList(soExtraValVal,
            String.valueOf(SEPARATOR_CHAR), true);
        String name = null;
        String value = null;
        String nameValuePair = null;

        for (int valueIndex = 0, sizeValues = allValues.size();
             valueIndex < sizeValues;
             valueIndex++) {
            nameValuePair = ( (String) allValues.get(valueIndex)).
                toUpperCase();
            int eqInd = nameValuePair.indexOf(EQUALS_CHAR);
            if (eqInd == -1) {
                throw new RuntimeException("Malformed SO_EXTRA string, missing = in token : " + nameValuePair);
            }
            name = nameValuePair.substring(0 , eqInd);
            if (!StringHelper.isEmpty(name)) {
                value = nameValuePair.substring(eqInd + 1);
                if (SKDGRP_NAME.equals(name) && !StringHelper.isEmpty(value)) {
                    ScheduleGroupData sgd = codeMapper.getScheduleGroupByName(value);
                    if (sgd == null) {
                        throw new RuntimeException("Schedule group name :" + value
                            + " does not exist for SKDGRP_NAME");
                    }
                    soExtra.soEmp.setSkdgrpId(sgd.getSkdgrpId().intValue());
                    soExtra.soEmp.addToAssignedFields(SKDGRP_ID);
                }
                else if (EMPGRP_NAME.equals(name) && !StringHelper.isEmpty(value)) {
                    List egds = new EmployeeGroupAccess(c).loadRecordData(
                            new EmployeeGroup(), EmployeeGroup.SO_EMPLOYEE_GROUP_TABLE, 
                            "empgrp_name", value);
                    if (egds.size() == 0) {
                        throw new RuntimeException("Staff group name :" + value
                            + " does not exist for EMPGRP_NAME");
                    }
                    EmployeeGroup egd = (EmployeeGroup )egds.get(0);
                    soExtra.soEmp.setEmpgrpId(egd.getEmpgrpId());
                    soExtra.soEmp.addToAssignedFields(EMPGRP_ID);
                }                
                else if (SKDGRP_CLIENTKEY.equals(name) && !StringHelper.isEmpty(value)) {
                    ScheduleGroupData sgd = getScheduleGroupDataByClientKey(value);
                    if (sgd == null) {
                        throw new RuntimeException("Schedule group clientkey :" + value
                            + " does not exist for SKDGRP_CLIENTKEY");
                    }
                    soExtra.soEmp.setSkdgrpId(sgd.getSkdgrpId().intValue());
                    soExtra.soEmp.addToAssignedFields(SKDGRP_CLIENTKEY);
                }
                else if (SEMP_EFF_DATE.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempEffDate(DateHelper.parseDate(value , HR_REFRESH_DATE_FMT));
                    soExtra.soEmp.addToAssignedFields(SEMP_EFF_DATE);
                }
                else if (SEMP_SKDMAX_HRS.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempSkdmaxHrs(Double.parseDouble(value));
                    soExtra.soEmp.addToAssignedFields(SEMP_SKDMAX_HRS);
                }
                else if (SEMP_SKDMIN_HRS.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempSkdminHrs(Double.parseDouble(value));
                    soExtra.soEmp.addToAssignedFields(SEMP_SKDMIN_HRS);
                }
                else if (SEMP_DAYMAX_HRS.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempDaymaxHrs(Double.parseDouble(value));
                    soExtra.soEmp.addToAssignedFields(SEMP_DAYMAX_HRS);
                }
                else if (SEMP_SHFTMIN_HRS.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempShftminHrs(Double.parseDouble(value));
                    soExtra.soEmp.addToAssignedFields(SEMP_SHFTMIN_HRS);
                }
                else if (SEMP_ONFIXED_SKD.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempOnfixedSkd(Integer.parseInt(value));
                    soExtra.soEmp.addToAssignedFields(SEMP_ONFIXED_SKD);
                }
                else if (SEMP_MAX2NT_RULE.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempMax2ntRule(Integer.parseInt(value));
                    soExtra.soEmp.addToAssignedFields(SEMP_MAX2NT_RULE);
                }
                else if (SEMP_MAXSHFTDAY.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempMaxshftday(Integer.parseInt(value));
                    soExtra.soEmp.addToAssignedFields(SEMP_MAXSHFTDAY);
                }
                else if (SEMP_X_IN_DATE.equals(name)) {
                    if (!StringHelper.isEmpty(value)) {
                        soExtra.soEmp.setSempXInDate(DateHelper.parseDate(
                            value, HR_REFRESH_DATE_FMT));
                    }
                    else {
                        soExtra.soEmp.setSempXInDate(null);
                    }
                    soExtra.soEmp.addToAssignedFields(SEMP_X_IN_DATE);
                }
                else if (SEMP_X_OUT_DATE.equals(name)) {
                    if (!StringHelper.isEmpty(value)) {
                        soExtra.soEmp.setSempXOutDate(DateHelper.parseDate(
                            value, HR_REFRESH_DATE_FMT));
                    }
                    else {
                        soExtra.soEmp.setSempXOutDate(null);
                    }
                    soExtra.soEmp.addToAssignedFields(SEMP_X_OUT_DATE);
                }
                else if (PREFERRED_EMPJOB_NAME.equals(name)  && !StringHelper.isEmpty(value)) {
                    JobData jd = CodeMapper.createCodeMapper(c).getJobByName(value);
                    if (jd == null) {
                        throw new RuntimeException("Job name :" + value
                            + " does not exist for PREFERRED_EMPJOB_NAME");
                    }
                    soExtra.prefJobId = new Integer(jd.getJobId());                    
                }
                else if (SEMP_SEASONAL_REG.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempSeasonalReg(value);
                    soExtra.soEmp.addToAssignedFields(SEMP_SEASONAL_REG);
                }
                else if (SEMP_IS_SALARY.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempIsSalary(Integer.parseInt(value));
                    soExtra.soEmp.addToAssignedFields(SEMP_IS_SALARY);
                }              
                else if (SEMP_IS_MINOR.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempIsMinor(Integer.parseInt(value));
                    soExtra.soEmp.addToAssignedFields(SEMP_IS_MINOR);
                }              
                else if (SEMP_EXEMPT_STAT.equals(name)  && !StringHelper.isEmpty(value)) {
                    soExtra.soEmp.setSempExemptStat(value);
                    soExtra.soEmp.addToAssignedFields(SEMP_EXEMPT_STAT);
                }                    
                else {
                    throw new WBInterfaceException("SO Field not supported :" + name);
                }
            }
        }
    }

    private Employee loadSOEmployee(DBConnection c,int empId) {
        List list = new com.workbrain.app.modules.retailSchedule.db.EmployeeAccess(c).
            loadRecordData( new Employee(),Employee.TABLE_NAME, "emp_id" , empId);
        if (list.size() > 0) {
            return (Employee) list.get(0);
        } else {
            return null;
        }
    }

    private ScheduleGroupData getScheduleGroupDataByClientKey(String clientKey) {
        if (!skdgrpClient.containsKey(clientKey)) {
            List l = new RecordAccess(conn).loadRecordData(new ScheduleGroupData(),
               ScheduleGroupData.TABLE_NAME , "skdgrp_clientkey", clientKey);
            skdgrpClient.put(clientKey , getScheduleGroupDataByClientKey(conn , clientKey));
        }
        return (ScheduleGroupData)skdgrpClient.get(clientKey);
    }

    public static ScheduleGroupData getScheduleGroupDataByClientKey(DBConnection conn , String clientKey) {
        List l = new RecordAccess(conn).loadRecordData(new ScheduleGroupData(),
           ScheduleGroupData.TABLE_NAME , "skdgrp_clientkey", clientKey);
       if (l.size() > 0) {
           return (ScheduleGroupData)l.get(0);
       }
       return null;

    }
    class SOExtra {
        int empId;
        Integer prefJobId;
        Employee soEmp;
        boolean isEmpUpdate = true;
    }

}