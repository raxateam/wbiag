/*---------------------------------------------------------------------------
  (C) Copyright Workbrain Inc. 2005
 --------------------------------------------------------------------------*/
package com.wbiag.app.modules.retailSchedule.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Hashtable;

import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.wbiag.app.modules.retailSchedule.db.CsdDetailAccess;
import com.wbiag.app.modules.retailSchedule.model.CorporateEntityStaffRequirementDetail;
import com.wbiag.app.modules.retailSchedule.services.model.CompWorkFactorMoselData;
import com.workbrain.app.modules.retailSchedule.services.rules.RulesFile;
//import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.EmployeeGroupAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntityStaffRequirement;
import com.workbrain.app.modules.retailSchedule.model.EmployeeGroup;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirement;
//import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager;
import com.workbrain.app.modules.retailSchedule.model.ScheduleEmployeeGroup;
//import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager.Record;
import com.workbrain.app.modules.retailSchedule.services.FileManager;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.model.CalloutHelper;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;

/**
 * This class contains the implementation code related to the Compressible Task 
 * rule to be called by the Schedule Callout implementation.
 *
 * @author James Tam
 */
public class CompressibleTaskScheduleCallout {
    private static Logger logger = Logger.getLogger(CompressibleTaskScheduleCallout.class);

    private static final String PREFIX = "CompressibleTask_";

    /**
     * The key used for storing and retrieving the list of staffing requirements
     * from the custom data in the SOData object.
     */
    public static final String SA_STAFF_REQ_LIST = PREFIX + "staffRequirementList";

    /**
     * The registry key used to specify whether of not the Compressible Task
     * rule is to be used.
     */
    public static final String USE_COMPRESSIBLE_TASK_REGISTRY_KEY =
            "system/modules/scheduleOptimization/USE_COMPRESSIBLE_TASKS";

    /**
     * Retrieves the current SA staffing requirement being processed and adds it
     * to a list, which is stored in the context.
     *
     * <p><b>NOTE: This method should be called from
     * <code>ScheduleCallout.distributeScheduleAreaWorkloadPreAction()</code>.
     * </b></p>
     *
     * @param soContext Context object containing objects related to schedule
     * callouts
     */
    /*public void storeSAStaffRequirement(SOData soContext) {
        CorporateEntityStaffRequirement staffReq = CalloutHelper.getCorporateEntityStaffRequirement(
            soContext, ScheduleCallout.DISTRIBUTE_SCHEDULE_AREA_WORKLOAD);
        List staffReqList = (List)soContext.getCustomData(SA_STAFF_REQ_LIST);
        if (staffReqList==null) {
            staffReqList = new ArrayList();
        }
        staffReqList.add(staffReq);
        if(logger.isDebugEnabled())
        	logger.debug("Staff req list size: " + staffReqList.size());
        soContext.addCustomData(SA_STAFF_REQ_LIST, staffReqList);
    }*/

    /**
     * Activates the <i>Compressible Task</i> staff rule, if so designated by 
     * {@link #USE_COMPRESSIBLE_TASK_REGISTRY_KEY USE_COMPRESSIBLE_TASK_REGISTRY_KEY},
     * by writing the appropriate entries to the staff rules file. If the rule is
     * activated then the compression factors for the staffing requirements are
     * determined and the compress work factor Mosel temp input file is written.
     * In the case that no compression factor has been configured for a given
     * staffing requirement, the factor of 100% (i.e. no compression) will be
     * used.
     *
     * <p><b>NOTE: This method should only be called from
     * <code>ScheduleCallout.xxxMethod()</code> that occurs before the end of
     * the <code>PreProcessor.start()</code> method where the FileManager closes
     * all additional temp files. The method must also occur after the
     * determination of whether or not the <i>Honor Skill</i> rule is activated
     * and the corresponding flag is set in the Schedule objext, for example:
     * <code>ScheduleCallout.addIRToMatrixPreLoop()</code></b></p>
     *
     * @param soContext Context object containing objects related to schedule
     * callouts
     * @throws RetailException If an error occurs while attempting to build
     * or write entries to the staff rule file or the compress work factor file
     */
    public void activateCompressibleTaskRule(SOData soContext) throws RetailException {
        RulesFile rulesFile = RulesFile.getRulesFile(soContext);
        
        if (useRule()) {
            Vector empGroupList = soContext.getScheduleEmployeeGroups();
            ScheduleEmployeeGroup skdEmpGroup = null;
            EmployeeGroupAccess ega = new EmployeeGroupAccess(soContext.getDBconnection());
            EmployeeGroup empGroup = new EmployeeGroup();
            String ruleGroupId = "";
            // Write a rule entry for each staff group in the staff rule file
            for (int i = 0; i<empGroupList.size(); i++) {
            	skdEmpGroup = (ScheduleEmployeeGroup)(empGroupList.elementAt(i));
                empGroup = (EmployeeGroup)ega.loadRecordDataByPrimaryKey(empGroup, skdEmpGroup.getEmpgrpId().intValue());
                ruleGroupId = "" + empGroup.getRulegrpId();
                rulesFile.writeRule(CompWorkFactorMoselData.RULE_ID, ruleGroupId, "1", "", "", "");
            }
//       	 Write out the compress work factor file
            /*boolean honourSkill = soContext.getSchedule().ishonourSkillsRuleOn();
            FileManager fileManager = soContext.getLocalFileManager();
            
            Integer fileIndex = fileManager.registerFile(CompWorkFactorMoselData.FILENAME);
            List staffReqList = (List)soContext.getCustomData(SA_STAFF_REQ_LIST);
            Hashtable m_tblAllAssignmentOptions = (Hashtable)soContext.getLocalVariable(ScheduleCallout.ADD_IR_TO_MATRIX+CalloutHelper.TYPE_HASHTABLE_PARAM1);
            if (staffReqList!=null) {
            	if(logger.isDebugEnabled())
            		logger.debug("Staff req list size: " + staffReqList.size());
            	
                List rowList = getReqCompFactorMoselDataList(staffReqList, honourSkill, m_tblAllAssignmentOptions);
                for (int i = 0; i<rowList.size(); i++) {
                    fileManager.write(fileIndex, ((CompWorkFactorMoselData)rowList.get(i)).toString());
                }
            } else { //else statement added by tyoung
            	if(logger.isDebugEnabled())
            		logger.debug("No staffing requirements found for task compression. Checking IR manager...");
                
                staffReqList = new ArrayList(); 
                
                IntervalRequirementsManager irm = soContext.getSchedule().getIntervalRequirements();
                List irList = irm.getRequirementsList();
                for(int i = 0; i < irList.size(); i++) {
                	Record rec = (Record)irList.get(i);
                	CorporateEntityStaffRequirement cesr = rec.m_staffReq;
                	
                	if(logger.isDebugEnabled())
                		logger.debug("requirement " + i + " id: " + cesr.getCsdId());
                	
                	staffReqList.add(cesr);
                }
                
                if(logger.isDebugEnabled())
            		logger.debug("Staff req list size: " + staffReqList.size());
                List rowList = getReqCompFactorMoselDataList(staffReqList, honourSkill, m_tblAllAssignmentOptions);
                for (int i = 0; i<rowList.size(); i++) {
                    fileManager.write(fileIndex, ((CompWorkFactorMoselData)rowList.get(i)).toString());
                }
            }*/
            boolean honourSkill = soContext.getSchedule().ishonourSkillsRuleOn();
            FileManager fileManager = soContext.getLocalFileManager();
            
            Integer fileIndex = fileManager.registerFile(CompWorkFactorMoselData.FILENAME);
            List staffReqList = new ArrayList();
            
            Hashtable m_tblAllAssignmentOptions = (Hashtable)soContext.getLocalVariable(ScheduleCallout.ADD_IR_TO_MATRIX+CalloutHelper.TYPE_HASHTABLE_PARAM1);
            
            DBConnection conn = soContext.getDBconnection();
            PreparedStatement stmt = null;
            ResultSet rs = null;
            
            try {
            	StringBuffer query = new StringBuffer("SELECT CSD_ID ");
				query.append("FROM SO_CLIENT_STFDEF CS, (SELECT SKDGRP_ID, SKDGRP_NAME ");  
				query.append("    FROM SO_SCHEDULE_GROUP, WORKBRAIN_TEAM   ");
				query.append("    WHERE WORKBRAIN_TEAM.WBT_ID = SO_SCHEDULE_GROUP.WBT_ID AND ");  
				query.append("        SO_SCHEDULE_GROUP.WBT_ID IN   ");
				query.append("            (SELECT CHILD_WBT_ID   ");
				query.append("            FROM SEC_WB_TEAM_CHILD_PARENT ");  
				query.append("            WHERE PARENT_WBT_ID =   ");
				query.append("                (SELECT WBT_ID   ");
				query.append("                FROM SO_SCHEDULE_GROUP ");  
				query.append("                WHERE SKDGRP_ID = ?)) ");  
				query.append("    ORDER BY WBT_LEVEL DESC) G ");
				query.append("WHERE CS.SKDGRP_ID = G.SKDGRP_ID AND ");
				query.append("    (CSD_EFF_START_DATE < ? OR CSD_EFF_START_DATE IS NULL) AND ");
				query.append("    (CSD_EFF_END_DATE > ? OR CSD_EFF_END_DATE IS NULL) ");
				
				stmt = conn.prepareStatement(query.toString());
				stmt.setInt(1, soContext.getScheduleCE().getID().intValue());
				stmt.setTimestamp(2, new Timestamp(soContext.getScheduleEndDate().toDateObject().getTime()));
				stmt.setTimestamp(3, new Timestamp(soContext.getScheduleStartDate().toDateObject().getTime()));
				
				rs = stmt.executeQuery();
				
				while(rs.next()) {
					CorporateEntityStaffRequirement cesr = new CorporateEntityStaffRequirement();
					cesr.dbLoad(new Integer(rs.getInt("CSD_ID")));
					staffReqList.add(cesr);
					if(logger.isDebugEnabled())
						logger.debug("csd_id = " + rs.getInt("CSD_ID") + "; cesr.isLoaded = " + cesr.isLoaded());
				}
            }
            catch(SQLException e) {
            	throw new RetailException(e);
            }
            finally {
            	SQLHelper.cleanUp(stmt, rs);
            }
            
            if (staffReqList!=null) {
            	if(logger.isDebugEnabled())
            		logger.debug("Staff req list size: " + staffReqList.size());
            	
                List rowList = getReqCompFactorMoselDataList(staffReqList, honourSkill, m_tblAllAssignmentOptions, conn);
                for (int i = 0; i<rowList.size(); i++) {
                    fileManager.write(fileIndex, ((CompWorkFactorMoselData)rowList.get(i)).toString());
                }
            } else {
            	if(logger.isDebugEnabled())
            		logger.debug("No staffing requirements found for task compression.");
            }
        }
    }

    /* Builds and returns a list of entries (CompWorkFactorMoselData instances) for
     * the compress work factor file */
    private List getReqCompFactorMoselDataList(List staffReqList, boolean honourSkills, Hashtable m_tblAllAssignmentOptions, DBConnection conn) throws RetailException {
        List dataList = new ArrayList();
        int numStaffReq = staffReqList.size();

        if (numStaffReq>0) {
            CompWorkFactorMoselData data = null;
            CorporateEntityStaffRequirement staffReq = null;
            CorporateEntityStaffRequirementDetail staffReqDetail = null;
            Double compFactor = null;
            HashMap staffReqDetailMap = new HashMap();
            HashMap staffReqIdMap = new HashMap();
            Integer nonVolFlag = null;
            Integer [] staffReqIds = new Integer [numStaffReq];
            String identifier = "";

            // Build map of unique staffing requirement identifiers
            for (int i = 0; i<staffReqList.size(); i++) {
                staffReq = (CorporateEntityStaffRequirement)staffReqList.get(i);
                staffReqIds[i] = staffReq.getCsdId();
                identifier = IntervalRequirement.getIdentifier(staffReq.getSkdgrpId(),
                                 staffReq.getJobId().intValue(), staffReq.getStsklId(),
                                 staffReq.getActId(), honourSkills);
                
                // Volume driven staffing requirements take precedence over not-volume ones
                nonVolFlag = staffReq.getCsdNonvlmFlag();
                if (!staffReqIdMap.containsKey(identifier) || (nonVolFlag!=null && nonVolFlag.intValue()==0)) {
                	if (m_tblAllAssignmentOptions.containsKey(identifier))
                        staffReqIdMap.put(identifier, staffReq.getCsdId());
                }
                if(logger.isDebugEnabled())
                	logger.debug("staffReqId[" + i + "] = " + staffReqIds[i]);
            }

            // Build map of compression factors for the staffing requirements
            CsdDetailAccess access = new CsdDetailAccess(conn);
            List staffReqDetailList = access.loadRecordDataINClause(new CorporateEntityStaffRequirementDetail(),
                                          CsdDetailAccess.TABLE_NAME, "csd_id", staffReqIds, true);
            for (int j = 0; j<staffReqDetailList.size(); j++) {
                staffReqDetail = (CorporateEntityStaffRequirementDetail)staffReqDetailList.get(j);
                double compVal = staffReqDetail.getCsddetCompressVl();
                // Compression Factor is range bounded from 0-100
                if (compVal<0d) {
                    compVal = 0d;
                } else if (compVal>100d) {
                    compVal = 100d;
                }
                staffReqDetailMap.put(new Integer(staffReqDetail.getCsdId()), new Double(compVal));
            }

            // Create a data entry for each unique staffing requirement 
            for (Iterator iter = staffReqIdMap.keySet().iterator(); iter.hasNext();) {
                identifier = (String)iter.next();
                compFactor = (Double)staffReqDetailMap.get(((Integer)(staffReqIdMap.get(identifier))));
                data = new CompWorkFactorMoselData(identifier, 100d); // Default to no compression
                if (compFactor!=null) {
                    data.setCompressFacVal(compFactor.doubleValue());
                }
                dataList.add(data);
            }
        }

        return dataList;
    }

    /* Check the registry to determine if the Compressible Task rule is on */
    private boolean useRule() {
        boolean bRule = false;
        try {
            bRule = Registry.getVarBoolean(USE_COMPRESSIBLE_TASK_REGISTRY_KEY);
        } catch (Exception e) {
            logger.error("Could not find registry variable: " + e.getMessage(), e);
        }
        return bRule;
    }

}