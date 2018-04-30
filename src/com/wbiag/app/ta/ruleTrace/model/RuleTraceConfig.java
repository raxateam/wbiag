package com.wbiag.app.ta.ruleTrace.model;

import java.util.*;
import com.workbrain.app.ta.ruleengine.RuleHelper;
import com.workbrain.app.ta.db.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.wbinterface.*;
import com.workbrain.util.*;
import com.workbrain.sql.SQLHelper;
import java.sql.*;
/**
 *  @deprecated Core as of 5.0.3.0
 */
public class RuleTraceConfig implements MappingObject{

    public static final String WBINT_MAP_RULE_TRACE_CONFIG =  "RULE TRACE CONFIG";
    private boolean isEnabled = false;
    private String applyToCalcGroups;
    private List employeeFields;
    private List workDetailFields;
    private List workPremiumFields;
    private List workSummaryFields;
    private List employeeBalances;

    public RuleTraceConfig() {

    }

    public boolean isEnabled(){
        return isEnabled;
    }

    public boolean isEnabled(String calcgrpName){
        return
            isEnabled()
            &&
            RuleHelper.isCodeInList(getApplyToCalcGroups() , calcgrpName)
            ;

    }

    public boolean isEnabled(DBConnection conn, int wrksId) throws SQLException{
        String calcgrpName = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT calcgrp_name FROM work_summary,calc_group WHERE work_summary.calcgrp_id = calc_group.calcgrp_id AND wrks_id = ?");
            ps = conn.prepareStatement(sb.toString());
            ps.setInt(1, wrksId);

            rs = ps.executeQuery();
            if  (rs.next()) {
                calcgrpName = rs.getString(1);
            }
            else {
                throw new RuntimeException ("Work summary not found:" +  wrksId);
            }
        }
        finally {
            SQLHelper.cleanUp(ps, rs);
        }

        return
            isEnabled()
            &&
            RuleHelper.isCodeInList(getApplyToCalcGroups() , calcgrpName)
            ;

    }

    public void setEnabled(boolean v){
        isEnabled=v;
    }

    public String getApplyToCalcGroups(){
        return applyToCalcGroups;
    }
    public void setApplyToCalcGroups(String v){
        applyToCalcGroups=v;
    }

    public List getEmployeeFields(){
        return employeeFields;
    }
    public void setEmployeeFields(List v){
        employeeFields=v;
    }

    public List getWorkSummaryFields(){
        return workSummaryFields;
    }
    public void setWorkSummaryFields(List v){
        workSummaryFields=v;
    }

    public List getWorkDetailFields(){
        return workDetailFields;
    }

    public void setWorkDetailFields(List v){
        workDetailFields=v;
    }

    public List getWorkPremiumFields(){
        return workPremiumFields;
    }

    public void setWorkPremiumFields(List v){
        workPremiumFields=v;
    }

    public List getEmployeeBalances(){
        return employeeBalances;
    }

    public void setEmployeBalances(List v){
        employeeBalances=v;
    }


    public String toString(){
          return
            "employeeFields=" + employeeFields + "\n" +
            "workSummaryFields=" + workSummaryFields + "\n" +
            "workDetailFields=" + workDetailFields + "\n" +
            "workPremiumFields=" + workPremiumFields + "\n" +
            "employeeBalances=" + employeeBalances + "\n"
            ;
    }

    /**
     * For 4.1 compatibility
     * @param conn
     * @return
     * @throws Exception
     */
    public static RuleTraceConfig getRuleTraceConfig(DBConnection conn) throws Exception{
        return getRuleTraceConfig(CodeMapper.createCodeMapper(conn) ) ;
    }

    public static RuleTraceConfig getRuleTraceConfig(CodeMapper cm) {
        RuleTraceConfig mapping = null;
        try {
            mapping = (RuleTraceConfig) cm.getWBIntMappingByName(new
                RuleTraceConfigMapping(), WBINT_MAP_RULE_TRACE_CONFIG);
        }
        catch (Exception ex) {
            throw new NestedRuntimeException("Error in parsing RULE TRACE CONFIG definition" , ex);
        }
        if (mapping == null) {
            throw new RuntimeException("RULE TRACE CONFIG not found in wbint_mapping table");
        }
        return mapping;
    }

}
