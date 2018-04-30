package com.wbiag.app.wbalert.source ;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbalert.*;
import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * EmployeeNewUpdatedAlertSource
 */
public class EmployeeNewUpdatedAlertSource extends AbstractRowSource{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmployeeNewUpdatedAlertSource.class);

    public static final String PARAM_CHECK_NEW_EMPS = "CHECK_NEW_EMPS";
    public static final String PARAM_CHECK_UPDATED_EMPS = "CHECK_UPDATED_EMPS";
    public static final String PARAM_LOOK_BACK_DAYS = "LOOK_BACK_DAYS";
    public static final String PARAM_EMPLOYEE_COLUMNS = "EMPLOYEE_COLUMNS";
    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_CHANGED = "CHANGED";

    public static final String EMP_ID_COL = "EMP_ID";
    public static final String EMP_NAME_COL = "EMP_NAME";
    public static final String EMP_LASTNAME_COL = "EMP_LASTNAME";
    public static final String EMP_FIRSTNAME_COL = "EMP_FIRSTNAME";
    public static final String EMP_DAY_START_TIME_COL = "EMP_DAY_START_TIME";
    public static final String EMP_SHFTPAT_NAME_COL = "SHFTPAT_NAME";
    public static final String EMP_CALCGRP_NAME_COL = "CALCGRP_NAME";
    public static final String EMP_BASE_RATE_COL = "EMP_BASE_RATE";
    public static final String EMP_PAYGRP_NAME_COL = "PAYGRP_NAME";
    public static final String EMP_HIRE_DATE_COL = "EMP_HIRE_DATE";
    public static final String EMP_SENIORITY_DATE_COL = "EMP_SENIORITY_DATE";
    public static final String EMP_BIRTH_DATE_COL = "EMP_BIRTH_DATE";
    public static final String EMP_TERMINATION_DATE_COL = "EMP_TERMINATION_DATE";
    public static final String EMP_STATUS_COL = "EMP_STATUS";
    public static final String EMP_SIN_COL = "EMP_SIN";
    public static final String EMP_SHFTPAT_OFFSET_COL = "EMP_SHFTPAT_OFFSET";
    public static final String EMP_FLAG1_COL = "EMP_FLAG1";
    public static final String EMP_FLAG2_COL = "EMP_FLAG2";
    public static final String EMP_FLAG3_COL = "EMP_FLAG3";
    public static final String EMP_FLAG4_COL = "EMP_FLAG4";
    public static final String EMP_FLAG5_COL = "EMP_FLAG5";
    public static final String EMP_FLAG6_COL = "EMP_FLAG6";
    public static final String EMP_FLAG7_COL = "EMP_FLAG7";
    public static final String EMP_FLAG8_COL = "EMP_FLAG8";
    public static final String EMP_FLAG9_COL = "EMP_FLAG9";
    public static final String EMP_FLAG10_COL = "EMP_FLAG10";
    public static final String EMP_FLAG11_COL = "EMP_FLAG11";
    public static final String EMP_FLAG12_COL = "EMP_FLAG12";
    public static final String EMP_FLAG13_COL = "EMP_FLAG13";
    public static final String EMP_FLAG14_COL = "EMP_FLAG14";
    public static final String EMP_FLAG15_COL = "EMP_FLAG15";
    public static final String EMP_FLAG16_COL = "EMP_FLAG16";
    public static final String EMP_FLAG17_COL = "EMP_FLAG17";
    public static final String EMP_FLAG18_COL = "EMP_FLAG18";
    public static final String EMP_FLAG19_COL = "EMP_FLAG19";
    public static final String EMP_FLAG20_COL = "EMP_FLAG20";
    public static final String EMP_VAL1_COL = "EMP_VAL1";
    public static final String EMP_VAL2_COL = "EMP_VAL2";
    public static final String EMP_VAL3_COL = "EMP_VAL3";
    public static final String EMP_VAL4_COL = "EMP_VAL4";
    public static final String EMP_VAL5_COL = "EMP_VAL5";
    public static final String EMP_VAL6_COL = "EMP_VAL6";
    public static final String EMP_VAL7_COL = "EMP_VAL7";
    public static final String EMP_VAL8_COL = "EMP_VAL8";
    public static final String EMP_VAL9_COL = "EMP_VAL9";
    public static final String EMP_VAL10_COL = "EMP_VAL10";
    public static final String EMP_VAL11_COL = "EMP_VAL11";
    public static final String EMP_VAL12_COL = "EMP_VAL12";
    public static final String EMP_VAL13_COL = "EMP_VAL13";
    public static final String EMP_VAL14_COL = "EMP_VAL14";
    public static final String EMP_VAL15_COL = "EMP_VAL15";
    public static final String EMP_VAL16_COL = "EMP_VAL16";
    public static final String EMP_VAL17_COL = "EMP_VAL17";
    public static final String EMP_VAL18_COL = "EMP_VAL18";
    public static final String EMP_VAL19_COL = "EMP_VAL19";
    public static final String EMP_VAL20_COL = "EMP_VAL20";
    public static final String EMP_DEF_MINUTES_COL = "EMP_DEF_MINUTES";
    public static final String EMP_FULLTIME = "EMP_FULLTIME";
    public static final String EMP_RETAIL_AVAIL = "EMP_RETAIL_AVAIL";
    public static final String EMP_WBT_NAME = "WBT_NAME";
    public static final String COL_STATUS = "CHANGE_STATUS";
    public static final String EMP_OVR_NEW_VALUE = "OVR_NEW_VALUE";

    public static final int EMP_FLAG_CNT = 20;
    public  static final int EMP_VAL_CNT = 20;
    public  static final String EMP_FLAG_PREFIX = "EMP_FLAG";
    public  static final String EMP_VAL_PREFIX = "EMP_VAL";

    private RowDefinition rowDefinition;
    private DBConnection conn;
    private boolean checkNewEmps;
    private boolean checkUpdatedEmps;
    private List empColumns = new ArrayList();
    private static String empColumnsString;
    private int lookBackDays = 0;
    private Date taskDateTime;
    private java.util.List rows = new ArrayList();
    private static List allEmpCols = new ArrayList();
    private static String allEmpColsString;
    static {
        allEmpCols.add(EMP_ID_COL );
        allEmpCols.add(EMP_NAME_COL );
        allEmpCols.add(EMP_LASTNAME_COL );
        allEmpCols.add(EMP_FIRSTNAME_COL );
        allEmpCols.add(EMP_DAY_START_TIME_COL );
        allEmpCols.add(EMP_SHFTPAT_NAME_COL );
        allEmpCols.add(EMP_CALCGRP_NAME_COL );
        allEmpCols.add(EMP_BASE_RATE_COL );
        allEmpCols.add(EMP_PAYGRP_NAME_COL );
        allEmpCols.add(EMP_HIRE_DATE_COL );
        allEmpCols.add(EMP_SENIORITY_DATE_COL );
        allEmpCols.add(EMP_BIRTH_DATE_COL );
        allEmpCols.add(EMP_TERMINATION_DATE_COL );
        allEmpCols.add(EMP_STATUS_COL );
        allEmpCols.add(EMP_SIN_COL );
        allEmpCols.add(EMP_SHFTPAT_OFFSET_COL );
        allEmpCols.add(EMP_FLAG1_COL );
        allEmpCols.add(EMP_FLAG2_COL );
        allEmpCols.add(EMP_FLAG3_COL );
        allEmpCols.add(EMP_FLAG4_COL );
        allEmpCols.add(EMP_FLAG5_COL );
        allEmpCols.add(EMP_FLAG6_COL );
        allEmpCols.add(EMP_FLAG7_COL );
        allEmpCols.add(EMP_FLAG8_COL );
        allEmpCols.add(EMP_FLAG9_COL );
        allEmpCols.add(EMP_FLAG10_COL );
        allEmpCols.add(EMP_FLAG11_COL );
        allEmpCols.add(EMP_FLAG12_COL );
        allEmpCols.add(EMP_FLAG13_COL );
        allEmpCols.add(EMP_FLAG14_COL );
        allEmpCols.add(EMP_FLAG15_COL );
        allEmpCols.add(EMP_FLAG16_COL );
        allEmpCols.add(EMP_FLAG17_COL );
        allEmpCols.add(EMP_FLAG18_COL );
        allEmpCols.add(EMP_FLAG19_COL );
        allEmpCols.add(EMP_FLAG20_COL );
        allEmpCols.add(EMP_VAL1_COL );
        allEmpCols.add(EMP_VAL2_COL );
        allEmpCols.add(EMP_VAL3_COL );
        allEmpCols.add(EMP_VAL4_COL );
        allEmpCols.add(EMP_VAL5_COL );
        allEmpCols.add(EMP_VAL6_COL );
        allEmpCols.add(EMP_VAL7_COL );
        allEmpCols.add(EMP_VAL8_COL );
        allEmpCols.add(EMP_VAL9_COL );
        allEmpCols.add(EMP_VAL10_COL );
        allEmpCols.add(EMP_VAL11_COL );
        allEmpCols.add(EMP_VAL12_COL );
        allEmpCols.add(EMP_VAL13_COL );
        allEmpCols.add(EMP_VAL14_COL );
        allEmpCols.add(EMP_VAL15_COL );
        allEmpCols.add(EMP_VAL16_COL );
        allEmpCols.add(EMP_VAL17_COL );
        allEmpCols.add(EMP_VAL18_COL );
        allEmpCols.add(EMP_VAL19_COL );
        allEmpCols.add(EMP_VAL20_COL );
        allEmpCols.add(EMP_DEF_MINUTES_COL );
        allEmpCols.add(EMP_WBT_NAME );
        allEmpCols.add(EMP_FULLTIME );
        allEmpCols.add(EMP_RETAIL_AVAIL );
        allEmpCols.add(COL_STATUS );
        allEmpCols.add(EMP_OVR_NEW_VALUE );
        StringBuffer sb = new StringBuffer(200);
        for (int i=0 , k=allEmpCols.size() ; i<k ; i++) {
            String item = (String)allEmpCols.get(i);
            sb.append(i > 0 ? "," : "");
            sb.append(item);
        }
        allEmpColsString = sb.toString();
    }

    /**
     *
     *@param  c                    Connection
     *@param  alertParams          alert parameters
     *@exception  AccessException  Description of Exception
     */
    public EmployeeNewUpdatedAlertSource(DBConnection c , HashMap alertParams) throws AccessException {
        this.conn = c;
        String sCheckNewEmps = (String)alertParams.get(PARAM_CHECK_NEW_EMPS);
        this.checkNewEmps = "Y".equals(sCheckNewEmps) ? true : false;

        String sCheckUpdatedEmps = (String)alertParams.get(PARAM_CHECK_UPDATED_EMPS);
        this.checkUpdatedEmps = "Y".equals(sCheckUpdatedEmps) ? true : false;

        if (!checkNewEmps && !checkUpdatedEmps) {
            throw new AccessException("PARAM_CHECK_NEW_EMPS and/or PARAM_CHECK_UPDATED_EMPS must be true");
        }
        String lookBackDaysS = (String)alertParams.get(PARAM_LOOK_BACK_DAYS);
        if (!StringHelper.isEmpty(lookBackDaysS)) {
            try {
                this.lookBackDays = Integer.parseInt(lookBackDaysS);
                this.lookBackDays = (lookBackDays < 0) ? 0 : lookBackDays;
            }
            catch (NumberFormatException ex) {
                throw new AccessException(PARAM_LOOK_BACK_DAYS + " must be an Integer");
            }
        }
        this.taskDateTime = (Date) alertParams.get(WBAlertProcess.TASK_PARAM_TASKDATETIME);
        this.empColumnsString = (String)alertParams.get(PARAM_EMPLOYEE_COLUMNS);
        if (StringHelper.isEmpty(this.empColumnsString)) {
            throw new AccessException("PARAM_EMPLOYEE_COLUMNS cannot be empty");
        }
        this.empColumns = detokenizeStringAsList(this.empColumnsString , ",");
        initSourceDefinition();
        try {
            loadRows();
        }
        catch (Exception e) {
            throw new NestedRuntimeException (e);
        }
    }

    /**
     * Returns all supported employee related columns in a comma-delimited string
     * @return
     */
    public static String getAllEmployeeColumnString() {
        return allEmpColsString;
    }

    /**
     * Returns all supported employee related columns in a list
     * @return
     */
    public static List getAllEmployeeColumns() {
        return allEmpCols;
    }

    private void initSourceDefinition() throws AccessException {
        //**** create row structure and definiton
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("creating row structure and definition");}
        RowStructure rs = new RowStructure();
        Iterator iter = empColumns.iterator();
        while (iter.hasNext()) {
            String item = (String)iter.next();
            if (allEmpCols.contains(item)) {
                rs.add(item, StringType.get());
            }
        }
        rowDefinition = new RowDefinition(-1,rs);
    }


    /**
     *
     *@exception  AccessException  Description of Exception
     */
    private void loadRows() throws AccessException, SQLException{
        rows.clear();
        Date lookbackDate = DateHelper.addDays(taskDateTime, -1 * lookBackDays);
        if (checkNewEmps) {
            loadNewRows(lookbackDate , this.empColumnsString );
        }
        if (checkUpdatedEmps) {
            loadChangedRows(lookbackDate , this.empColumnsString) ;
        }
        if (logger.isDebugEnabled()) logger.debug("Loaded : " + rows.size());
    }

    /**
     *
     *@exception  AccessException  Description of Exception
     */
    private void loadNewRows(Date lookbackDate, String empColsString) throws AccessException, SQLException{
        boolean containsTeam = empColumns.contains(EMP_WBT_NAME);
        boolean containsShftPat = empColumns.contains(EMP_SHFTPAT_NAME_COL);
        boolean containsPaygrp = empColumns.contains(EMP_PAYGRP_NAME_COL);
        boolean containsCalcgrp = empColumns.contains(EMP_CALCGRP_NAME_COL);
        empColsString = StringHelper.searchReplace(empColsString , EMP_ID_COL, "EMPLOYEE.EMP_ID");
        empColsString = StringHelper.searchReplace(empColsString , COL_STATUS, "'" + STATUS_NEW + "' " + COL_STATUS);
        empColsString = StringHelper.searchReplace(empColsString , EMP_OVR_NEW_VALUE,  "'N/A' " + EMP_OVR_NEW_VALUE);
        StringBuffer sb = new StringBuffer(400);
        sb.append("SELECT ");
        sb.append(empColsString);
        sb.append(" FROM employee ");
        if (containsTeam) {
            sb.append(", employee_team, workbrain_team ");
        }
        if (containsShftPat) {
            sb.append(", shift_pattern");
        }
        if (containsCalcgrp) {
            sb.append(", calc_group");
        }
        if (containsPaygrp) {
            sb.append(", pay_group");
        }

        sb.append("  WHERE 1=1 ");
        if (containsTeam) {
            sb.append("AND employee.emp_id = employee_team.emp_id ");
            sb.append("AND employee_team.wbt_id = workbrain_team.wbt_id ");
            sb.append("AND employee_team.empt_home_team = 'Y' ");
            sb.append("AND ? BETWEEN empt_start_date AND  empt_end_date ");
        }
        if (containsShftPat) {
            sb.append("AND employee.shftpat_id = shift_pattern.shftpat_id ");
        }
        if (containsCalcgrp) {
            sb.append("AND employee.calcgrp_id = calc_group.calcgrp_id ");
        }
        if (containsPaygrp) {
            sb.append("AND employee.paygrp_id = pay_group.paygrp_id ");
        }

        sb.append("  AND employee.emp_id IN ");
        sb.append("    (SELECT chnghist_record_id ");
        sb.append("     FROM change_history ");
        sb.append("     WHERE chnghist_table_name = ? ");
        sb.append("     AND chnghist_change_date >= ? ");
        sb.append("     AND change_history.chnghist_change_type = ?) ");
        sb.append("  ORDER BY employee.emp_id");
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sb.toString());
            int cnt = 1;
            if (containsTeam) {
                ps.setTimestamp(cnt++ , new Timestamp(DateHelper.getCurrentDate().getTime()) );
            }
            ps.setString(cnt++,  EmployeeAccess.EMPLOYEE_TABLE.toUpperCase());
            ps.setTimestamp(cnt++ , new Timestamp(lookbackDate.getTime()) );
            ps.setString(cnt++,  "I");
            rs = ps.executeQuery();
            while (rs.next()) {
                Row r = new BasicRow(getRowDefinition());
                Iterator iter = empColumns.iterator();
                while (iter.hasNext()) {
                    String item = (String)iter.next();
                    r.setValue(item, rs.getString(item));
                }
                rows.add(r);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    /**
     *
     *@exception  AccessException  Description of Exception
     */
    private void loadChangedRows(Date lookbackDate , String empColsString) throws AccessException, SQLException{

        boolean containsOvr = empColumns.contains(EMP_OVR_NEW_VALUE);
        empColsString = StringHelper.searchReplace(empColsString , EMP_ID_COL, "EMPLOYEE.EMP_ID");
        empColsString = StringHelper.searchReplace(empColsString , COL_STATUS, "'" + STATUS_CHANGED + "' " + COL_STATUS);
        if (containsOvr) {
            loadChangedRowsOvr(lookbackDate , empColsString);
        }
        else {
            loadChangedRowsNoOvr(lookbackDate , empColsString);
        }
    }


    /**
     *
     *@exception  AccessException  Description of Exception
     */
    private void loadChangedRowsOvr(Date lookbackDate , String empColsString) throws AccessException, SQLException{

        boolean containsTeam = empColumns.contains(EMP_WBT_NAME);
        boolean containsShftPat = empColumns.contains(EMP_SHFTPAT_NAME_COL);
        boolean containsPaygrp = empColumns.contains(EMP_PAYGRP_NAME_COL);
        boolean containsCalcgrp = empColumns.contains(EMP_CALCGRP_NAME_COL);

        StringBuffer sb = new StringBuffer(400);
        sb.append("SELECT ");
        sb.append(empColsString);
        sb.append(" FROM employee,  ");
        sb.append("    (SELECT emp_id, ovr_new_value ");
        sb.append("     FROM override ");
        sb.append("     WHERE ovrtyp_id BETWEEN ? and ? ");
        sb.append("     AND ovr_create_date >= ?  ");
        sb.append("     AND ovr_status = ?  ");
        sb.append("     UNION ALL ");
        sb.append("     SELECT emp_id, ovr_new_value  ");
        sb.append("     FROM override ");
        sb.append("     WHERE ovrtyp_id BETWEEN ? and ? ");
        sb.append("     AND ovr_cancelled_date >= ? ");
        sb.append("     AND ovr_status = ?  ");
        sb.append("     ) ovrs ");

        if (containsTeam) {
            sb.append(", employee_team, workbrain_team ");
        }
        if (containsShftPat) {
            sb.append(", shift_pattern");
        }
        if (containsCalcgrp) {
            sb.append(", calc_group");
        }
        if (containsPaygrp) {
            sb.append(", pay_group");
        }

        sb.append("  WHERE 1=1 ");
        if (containsTeam) {
            sb.append("AND employee.emp_id = employee_team.emp_id ");
            sb.append("AND employee_team.wbt_id = workbrain_team.wbt_id ");
            sb.append("AND employee_team.empt_home_team = 'Y' ");
            sb.append("AND ? BETWEEN empt_start_date AND  empt_end_date ");
        }
        if (containsShftPat) {
            sb.append("AND employee.shftpat_id = shift_pattern.shftpat_id ");
        }
        if (containsCalcgrp) {
            sb.append("AND employee.calcgrp_id = calc_group.calcgrp_id ");
        }
        if (containsPaygrp) {
            sb.append("AND employee.paygrp_id = pay_group.paygrp_id ");
        }

        sb.append("  AND employee.emp_id = ovrs.emp_id ");
        sb.append("  ORDER BY employee.emp_id");

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sb.toString());
            int cnt = 1;
            ps.setInt(cnt++,  OverrideData.EMPLOYEE_TYPE_START);
            ps.setInt(cnt++,  OverrideData.EMPLOYEE_TYPE_END);
            ps.setTimestamp(cnt++ , new Timestamp(lookbackDate.getTime()) );
            ps.setString(cnt++,  OverrideData.APPLIED);
            ps.setInt(cnt++,  OverrideData.EMPLOYEE_TYPE_START);
            ps.setInt(cnt++,  OverrideData.EMPLOYEE_TYPE_END);
            ps.setTimestamp(cnt++ , new Timestamp(lookbackDate.getTime()) );
            ps.setString(cnt++,  OverrideData.CANCELLED);
            if (containsTeam) {
                ps.setTimestamp(cnt++ , new Timestamp(DateHelper.getCurrentDate().getTime()) );
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                Row r = new BasicRow(getRowDefinition());
                Iterator iter = empColumns.iterator();
                while (iter.hasNext()) {
                    String item = (String)iter.next();
                    r.setValue(item, rs.getString(item));
                }
                rows.add(r);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    /**
     *
     *@exception  AccessException  Description of Exception
     */
    private void loadChangedRowsNoOvr(Date lookbackDate, String empColsString) throws AccessException, SQLException{

        boolean containsTeam = empColumns.contains(EMP_WBT_NAME);
        boolean containsShftPat = empColumns.contains(EMP_SHFTPAT_NAME_COL);
        boolean containsPaygrp = empColumns.contains(EMP_PAYGRP_NAME_COL);
        boolean containsCalcgrp = empColumns.contains(EMP_CALCGRP_NAME_COL);

        StringBuffer sb = new StringBuffer(400);
        sb.append("SELECT ");
        sb.append(empColsString);
        sb.append(" FROM employee  ");
        if (containsTeam) {
            sb.append(", employee_team, workbrain_team ");
        }
        if (containsShftPat) {
            sb.append(", shift_pattern");
        }
        if (containsCalcgrp) {
            sb.append(", calc_group");
        }
        if (containsPaygrp) {
            sb.append(", pay_group");
        }

        sb.append("  WHERE 1=1 ");
        if (containsTeam) {
            sb.append("AND employee.emp_id = employee_team.emp_id ");
            sb.append("AND employee_team.wbt_id = workbrain_team.wbt_id ");
            sb.append("AND employee_team.empt_home_team = 'Y' ");
            sb.append("AND ? BETWEEN empt_start_date AND  empt_end_date ");
        }
        if (containsShftPat) {
            sb.append("AND employee.shftpat_id = shift_pattern.shftpat_id ");
        }
        if (containsCalcgrp) {
            sb.append("AND employee.calcgrp_id = calc_group.calcgrp_id ");
        }
        if (containsPaygrp) {
            sb.append("AND employee.paygrp_id = pay_group.paygrp_id ");
        }

        sb.append("  AND employee.emp_id IN ");
        sb.append("    (SELECT emp_id ");
        sb.append("     FROM override ");
        sb.append("     WHERE ovrtyp_id BETWEEN ? and ? ");
        sb.append("     AND ovr_create_date >= ?  ");
        sb.append("     AND ovr_status = ?  ");
        sb.append("     UNION ALL ");
        sb.append("     SELECT emp_id  ");
        sb.append("     FROM override ");
        sb.append("     WHERE ovrtyp_id BETWEEN ? and ? ");
        sb.append("     AND ovr_cancelled_date >= ? ");
        sb.append("     AND ovr_status = ?  ");
        sb.append("     )");
        sb.append("  ORDER BY employee.emp_id");

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sb.toString());
            int cnt = 1;
            if (containsTeam) {
                ps.setTimestamp(cnt++ , new Timestamp(DateHelper.getCurrentDate().getTime()) );
            }
            ps.setInt(cnt++,  OverrideData.EMPLOYEE_TYPE_START);
            ps.setInt(cnt++,  OverrideData.EMPLOYEE_TYPE_END);
            ps.setTimestamp(cnt++ , new Timestamp(lookbackDate.getTime()) );
            ps.setString(cnt++,  OverrideData.APPLIED);
            ps.setInt(cnt++,  OverrideData.EMPLOYEE_TYPE_START);
            ps.setInt(cnt++,  OverrideData.EMPLOYEE_TYPE_END);
            ps.setTimestamp(cnt++ , new Timestamp(lookbackDate.getTime()) );
            ps.setString(cnt++,  OverrideData.CANCELLED);

            rs = ps.executeQuery();
            while (rs.next()) {
                Row r = new BasicRow(getRowDefinition());
                Iterator iter = empColumns.iterator();
                while (iter.hasNext()) {
                    String item = (String)iter.next();
                    r.setValue(item, rs.getString(item));
                }
                rows.add(r);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }

    public RowDefinition getRowDefinition() throws AccessException {
        return rowDefinition;
    }

    public RowCursor query(String queryString) throws AccessException{
        return queryAll();
    }

    public RowCursor query(String queryString, String orderByString) throws AccessException{
        return queryAll();
    }

    public RowCursor query(List keys) throws AccessException{
        return queryAll();
    }

    public RowCursor query(String[] fields, Object[] values) throws AccessException {
        return queryAll();
    }

    public RowCursor queryAll()  throws AccessException{
        return new AbstractRowCursor(getRowDefinition()){
            private int counter = -1;
            protected Row getCurrentRowInternal(){
                return counter >= 0 && counter < rows.size() ? (BasicRow)rows.get(counter) : null;
            }
            protected boolean fetchRowInternal() throws AccessException{
                return ++counter < rows.size();
            }
            public void close(){}
        };
    }

    public boolean isReadOnly(){
       return true;
    }

    public int count() {
        return rows.size();
    }

    public int count(String where) {
        return rows.size();
    }

    private List detokenizeStringAsList(String input,
                                        String separator) {
        String[] stArray = StringHelper.detokenizeString(input , separator);
        return stArray == null ? null : Arrays.asList(stArray);
    }

}






