package com.wbiag.app.ta.ruleengine;

import java.util.*;

import com.wbiag.app.ta.db.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.server.registry.*;
import com.workbrain.sql.*;
import com.workbrain.tool.security.*;
import com.workbrain.util.*;
/**
 * DataEvent to create temp teams based on REG_TEMP_TEAM_ASSIGN_BY_WORK_DETAIL_FIELD.
 * DataEvent first gets all home/temp teams for the existing day and
 * creates an insert list for missing teams. EmployeeTeam Cache is utilized to avoid redundant
 * DB trips
 * @deprecated Core as of 5.0, wbt_id is now in work_Detail table and this is handled by core rule engine
 */
public class CDataEventTempTeam extends DataEvent {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventTempTeam.class);

    public void beforeSave(WBData data, DBConnection c)   {
        try {
            processTempTeam(data);
        }
        catch (Exception ex) {
            throw new NestedRuntimeException("Error in processing temp teams", ex);
        }
    }

    protected boolean processTempTeam(WBData data) throws Exception  {
        EmployeeTeamCache cache = new EmployeeTeamCache(data);
        Set existingTempTeamNames = cache.getEmpTeamNames(data.getEmpId(),
            data.getWrksWorkDate());
        String tempTeamWDField = Registry.getVarString(
            OverrideAccess.REG_TEMP_TEAM_ASSIGN_BY_WORK_DETAIL_FIELD , null);
        if (StringHelper.isEmpty(tempTeamWDField)) {
            return false;
        }
        Set tempTeamInserts = new HashSet();
        findMissingTempTeams(data, WorkDetailData.DETAIL_TYPE , tempTeamWDField,
                             existingTempTeamNames,
                             tempTeamInserts);
        findMissingTempTeams(data, WorkDetailData.PREMIUM_TYPE, tempTeamWDField,
                             existingTempTeamNames,
                             tempTeamInserts);

        Iterator iter = tempTeamInserts.iterator();
        while (iter.hasNext()) {
            String wbtName = (String)iter.next();
            WorkbrainTeamData wbt  = data.getCodeMapper().getWBTeamByName(wbtName) ;
            if (wbt == null) {
                throw new RuntimeException("Workbrain team : " + wbtName
                    + " derived from Work Detail field :" +  tempTeamWDField + " is not found");
            }
            SecurityEmployee.addTempTeam(data.getDBconnection(),
                                         data.getEmpId(), wbt.getWbtId() ,
                                         new java.sql.Date(data.getWrksWorkDate().getTime()) ,
                                         new java.sql.Date(data.getWrksWorkDate().getTime())
                                         );
            cache.updateCache(data.getEmpId(), wbt.getWbtId(), wbt.getWbtName(),
                              data.getWrksWorkDate(),
                              data.getWrksWorkDate(), "N");
        }
        if (logger.isDebugEnabled()) logger.debug("Added " + tempTeamInserts.size() + " temp teams for empId:" + data.getEmpId() + " date :" + data.getWrksWorkDate());
        return true;
    }

    private void findMissingTempTeams(WBData data, String detailPremium,
                                      String tempTeamWDField,
                                      Set tempTeamNames,
                                      Set tempTeamInserts) {
        WorkDetailList dets = WorkDetailData.DETAIL_TYPE.equals(detailPremium)
            ? data.getRuleData().getWorkDetails()
            : data.getRuleData().getWorkPremiums();
        Iterator iter = dets.iterator();
        while (iter.hasNext()) {
            WorkDetailData item = (WorkDetailData)iter.next();
            item.setCodeMapper(data.getCodeMapper());
            String wbtName = (String)item.getField(tempTeamWDField);
            // *** ignore empty and default entries
            if (StringHelper.isEmpty(wbtName)
                || isDefaultEntity(data, tempTeamWDField, wbtName)) {
                continue;
            }
            if (!tempTeamNames.contains(wbtName)) {
                tempTeamInserts.add(wbtName);
            }
        }
    }

    private boolean isDefaultEntity(WBData wbdata,
                                    String tempTeamWDField, String name) {

        boolean ret = false;
        if (WorkDetailData.WRKD_DEPT_NAME.equalsIgnoreCase(tempTeamWDField)) {
            DepartmentData data = wbdata.getCodeMapper().getDepartmentByName(name);
            if (data != null) {
                ret = RuleHelper.getDefaultDeptId(wbdata.getCodeMapper()) == data.getDeptId();
            }
        }
        else if (WorkDetailData.WRKD_JOB_NAME.equalsIgnoreCase(tempTeamWDField)) {
            JobData data = wbdata.getCodeMapper().getJobByName(name);
            if (data != null) {
                ret = RuleHelper.getDefaultJobId(wbdata.getCodeMapper()) == data.getJobId();
            }
        }
        else if (WorkDetailData.WRKD_DOCK_NAME.equalsIgnoreCase(tempTeamWDField)) {
            DocketData data = wbdata.getCodeMapper().getDocketByName(name);
            if (data != null) {
                ret = RuleHelper.getDefaultDockId(wbdata.getCodeMapper()) == data.getDockId();
            }
        }
        else if (WorkDetailData.WRKD_PROJ_NAME.equalsIgnoreCase(tempTeamWDField)) {
            ProjectData data = wbdata.getCodeMapper().getProjectByName(name);
            if (data != null) {
                ret = RuleHelper.getDefaultProjectId(wbdata.getCodeMapper()) == data.getProjId();
            }
        }
        else if (WorkDetailData.WRKP_TCODE_NAME.equalsIgnoreCase(tempTeamWDField)) {
            TimeCodeData data = wbdata.getCodeMapper().getTimeCodeByName(name);
            if (data != null) {
                ret = RuleHelper.getUATTcodeId(wbdata.getCodeMapper()) == data.getTcodeId();
            }
        }
        else if (WorkDetailData.WRKD_HTYPE_NAME.equalsIgnoreCase(tempTeamWDField)) {
            HourTypeData data = wbdata.getCodeMapper().getHourTypeByName(name);
            if (data != null) {
                ret = RuleHelper.getUnpaidHtypeId(wbdata.getCodeMapper()) == data.getHtypeId();
            }
        }

        return ret;
    }
}