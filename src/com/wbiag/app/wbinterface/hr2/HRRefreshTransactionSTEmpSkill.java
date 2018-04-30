package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Customization for ST_EMP_SKILL
 *
 **/
public class HRRefreshTransactionSTEmpSkill extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionSTEmpSkill.class);

    public static final String ST_EMP_SKILL_TABLE = "ST_EMP_SKILL";
    public static final int ST_EMP_SKILL_COL = 90;
    public static final String ST_SKILL_SELECT_SQL
        = "SELECT stskl_id, stskl_name FROM st_skill";

    public static final String SKILL_SEP = "|";
    public static final String SKILL_ATTR_SEP = "~";
    public static final String SKILL_ABSOLUTE_PREFIX = "~~";

    private Map skills = null;
    private DBConnection conn = null;
    private int[] custColInds = new int[] {ST_EMP_SKILL_COL};

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
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            try {
                processEmpSkill(data.getImportData().getField(custColInds[0]),
                                data,
                                conn);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionSTEmpSkill." , ex);}
                data.error("Error in HRRefreshTransactionSTEmpSkill." + ex.getMessage() );
            }

        }
    }

    public void processEmpSkill(String val,
                               HRRefreshTransactionData data,
                               DBConnection c) throws SQLException, WBInterfaceException {
        // *** only do this if ST_EMP_SKILL_COL is not blank in the file i.e skillA~1|skillB~2.
        if (StringHelper.isEmpty(val)) {
            return;
        }
        boolean isAbsolute = val.startsWith(SKILL_ABSOLUTE_PREFIX);
        if (isAbsolute) {
            val = val.substring(SKILL_ABSOLUTE_PREFIX.length());
        }
        List empSkillsAll = null;
        // *** load all skills for updated employee
        if (!data.isNewEmployee()) {
            empSkillsAll = getEmpSkills(data.getEmpId());
        }
        String[] skills = StringHelper.detokenizeString(val , SKILL_SEP);
        Set processedSkillIds = new HashSet();
        for (int i = 0; i < skills.length; i++) {
            if (StringHelper.isEmpty(skills[i])) {
                continue;
            }
            String[] skillAttrs = StringHelper.detokenizeString(skills[i] , SKILL_ATTR_SEP);
            String skillName = skillAttrs[0];
            int skillId = getSkillId(skillName);
            if (skillId == -1) {
                throw new RuntimeException ("Skill name not found :" + skillName);
            }
            processedSkillIds.add(new Integer(skillId));
            int wght = 0;
            if (skillAttrs.length == 2) {
                wght = Integer.parseInt(skillAttrs[1]);
            }
            if (data.isNewEmployee()) {
                insertEmpSkill(data.getEmpId() ,
                               data.getOvrStartDate(),
                               data.getOvrEndDate(),
                               skillId , wght );
            }
            else {
                List empSkills = getEmpSkills(empSkillsAll , data.getEmpId() , skillId);
                if (empSkills.size() == 0) {
                    insertEmpSkill(data.getEmpId() ,
                                   data.getOvrStartDate(),
                                   data.getOvrEndDate(),
                                   skillId , wght );
                    if (logger.isDebugEnabled()) logger.debug("Inserted skill : " + skillName + " for employee : " + data.getEmpName());
                }
                else if (empSkills.size() == 1) {
                    StEmpSkillData empSkData = (StEmpSkillData)empSkills.get(0);
                    // *** only update is smt changed
                    if (empSkData.getStempsklWeight().intValue() != wght) {
                        updateEmpSkill(empSkData,
                                   data.getOvrStartDate(),
                                   data.getOvrEndDate(),
                                   skillId , wght);
                        if (logger.isDebugEnabled()) logger.debug("Updated skill : " + skillName + " for employee : " + data.getEmpName());
                    }
                }
                else {
                    data.appendWarning("More than one skill existed for skill : " + skillName + " . No update was done.");
                }
            }
        }
        if (isAbsolute) {
            processAbsolute(empSkillsAll, processedSkillIds, data);
        }
    }

    protected void processAbsolute(List empSkills,
                                   Set empSkillsProcessed,
                                   HRRefreshTransactionData data) {
        if ((empSkills == null || empSkills.size() == 0)
            || (empSkillsProcessed == null || empSkillsProcessed.size() == 0)){
            return;
        }
        IntegerList empSkillsToDelete = new IntegerList();
        Iterator iter = empSkills.iterator();
        while (iter.hasNext()) {
            StEmpSkillData item = (StEmpSkillData)iter.next();
            if (!empSkillsProcessed.contains(item.getStsklId())) {
                empSkillsToDelete.add(item.getStempsklId());
            }
        }
        if (empSkillsToDelete.size() > 0) {
            for (int i=0, k=empSkillsToDelete.size() ; i < k ; i++) {
                int id = empSkillsToDelete.getInt(i);
                new RecordAccess(conn).deleteRecordData(ST_EMP_SKILL_TABLE,
                    "stempskl_id", id);
            }
            if (logger.isDebugEnabled()) logger.debug("Deleted " + empSkillsToDelete.size() + " employee skill(s) for expiration");
        }
    }

    protected void insertEmpSkill(int empId, Date start, Date end, int skillId, int wght) throws SQLException{
        StEmpSkillData data = new StEmpSkillData();
        data.setEmpId(new Integer(empId));
        data.setStempsklStartDate(start);
        data.setStempsklEndDate(end);
        data.setStempsklWeight(new Integer(wght));
        data.setStsklId(new Integer(skillId));
        data.setStempsklId(new Integer(conn.getDBSequence("seq_stempskl_id").getNextValue()));
        new RecordAccess(conn).insertRecordData(data , ST_EMP_SKILL_TABLE);
    }

    protected void updateEmpSkill(StEmpSkillData data, Date start, Date end,
                                  int skillId, int wght) throws SQLException{
        data.setStempsklStartDate(start);
        data.setStempsklEndDate(end);
        data.setStempsklWeight(new Integer(wght));
        data.setStsklId(new Integer(skillId));
        new RecordAccess(conn).updateRecordData(data , ST_EMP_SKILL_TABLE , "stempskl_id");
    }

    protected List getEmpSkills(int empId) throws SQLException{
        return new RecordAccess(conn).loadRecordData(new StEmpSkillData() ,
            ST_EMP_SKILL_TABLE,
            "emp_id" , empId);
    }

    protected List getEmpSkills(List skills, int empId , int skillId) throws SQLException{
        List ret = new ArrayList();
        if (skills == null || skills.size() == 0) {
            return ret;
        }
        Iterator iter = skills.iterator();
        while (iter.hasNext()) {
            StEmpSkillData item = (StEmpSkillData)iter.next();
            if (item.getStsklId().intValue() == skillId) {
                ret.add(item);
            }
        }
        return ret;
    }

    protected int getSkillId(String skillName) throws SQLException{
        int ret = -1;
        if (skills == null) {
            skills = new HashMap();
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = conn.prepareStatement(ST_SKILL_SELECT_SQL);
                rs = ps.executeQuery();
                while (rs.next()) {
                    skills.put(rs.getString(2) , new Integer(rs.getInt(1)));
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }

        }
        if (skills.containsKey(skillName)) {
            ret = ((Integer)skills.get(skillName)).intValue();
        }
        return ret;
    }


}