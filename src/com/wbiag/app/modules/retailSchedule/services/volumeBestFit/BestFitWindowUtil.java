package com.wbiag.app.modules.retailSchedule.services.volumeBestFit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

import java.util.*;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager;
import com.workbrain.app.modules.retailSchedule.model.Schedule;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager.Record;
import com.workbrain.app.modules.retailSchedule.services.IntervalRequirementsHelper;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCoverage;
import com.workbrain.app.modules.retailSchedule.utils.SOHashtable;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;

import com.wbiag.app.modules.retailSchedule.services.volumeBestFit.model.*;
import com.wbiag.app.modules.retailSchedule.services.volumeBestFit.db.*;

/**
 * Utility class for retrieving best fit volume windows
 *
 * */
public class BestFitWindowUtil {

    public static Logger logger = Logger.getLogger(BestFitWindowUtil.class);
    public static final String WINDOW_START = "window_start";
    public static final String WINDOW_END = "window_end";

    /**
     * Create a vector of maps that contain windows
     *
     * @param skdgrp_id
     * @param job_id
     * @param skill_id
     * @param act_id
     * @param conn
     * @return Vector
     * @throws RetailException
     */
    public static Vector getBFWindow(int skdgrp_id, int job_id, int skill_id, int act_id, DBConnection conn) throws RetailException{
    	Vector result = new Vector();

    	try{
    		Vector allCSD_ID = getClientStfDefId(skdgrp_id, job_id, skill_id, act_id, conn);
    		Iterator allCSD_IDit = allCSD_ID.iterator();
    		while (allCSD_IDit.hasNext()){
    			Integer curCSD_ID = (Integer)allCSD_IDit.next();
    			Map cur = getBFWindow(curCSD_ID.intValue(), skdgrp_id, conn);
    			if (cur.size() > 0){
    				result.add(cur);
    			}
    		}

    	} catch (Exception e){
    		throw new RetailException("Error retrieving best fit window",e);
    	}

    	return result;
    }

    /**
     * Create a map of maps with the key being the day index
     *
     * @param csd_id
     * @param conn
     * @return Map
     * @throws RetailException
     */
    public static Map getBFWindow(int csd_id, int driverId, DBConnection conn) throws RetailException{
    	Map result = new HashMap();

    	try{
    		for (int i=0; i<7; i++){
    			List cur = getBFWindowByDay(csd_id, driverId, i, conn);

    			if (cur.size() > 0){
    				result.put(new Integer(i), cur);
    			}
    		}
    	} catch (Exception e){
    		throw new RetailException("Error retrieving best fit window",e);
    	}

    	return result;
    }

    /**
     * Create a List containing maps of the windows for the given staff requirement and day index
     *
     * @param csd_id
     * @param dayIndex
     * @param conn
     * @return List
     * @throws RetailException
     */
    public static List getBFWindowByDay(int csd_id, int driverId, int dayIndex, DBConnection conn) throws RetailException{

        List result;
    	BestFitWindowCache cBFWindowByDay = BestFitWindowCache.getInstance();
    	result =  cBFWindowByDay.getBFWindowByDay(csd_id, driverId, dayIndex, conn);

        return result;
    }

    /**
     *
     * get the drivers belonging to a particular department
     * from the staff requirement defined.
     * In 5.0, even drivers do not belong to a particular department,
     * in Woolworth's the drivers are, and they all assigned to only one
     * department's staffing requirement
     *
     * @param csd_id
     * @param dayIndex
     * @param conn
     * @return List
     * @throws RetailException
     */
    public static List getDriversOfDepartmentFromStfRequirement(int departmentId, DBConnection conn, List drivers, Map volumeBestFitMap) throws RetailException{
        List result = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql = " SELECT c.vbf_id, c.csd_id, c.DRIVER_SKDGRP_ID, c.wrkld_stdvol_hour, c.vbf_start_time, c.vbf_end_time, c.vbf_day_index "+
                    " FROM so_client_stfdef b, wbiag_volume_bestfit c" +
                    " WHERE  b.csd_id = c.csd_id " +
                    " AND b.skdgrp_id = ? "+
                    " AND b.csd_nonvlm_flag = 1";

        try{
            ps = conn.prepareStatement(sql);
            ps.setInt(1, departmentId);

            rs = ps.executeQuery();

            List addedSkdgrpIds = new ArrayList();
            while (rs.next()) {
                int driverSkdgrpId = rs.getInt("driver_skdgrp_id");
                double stdVolHours = rs.getDouble("wrkld_stdvol_hour");
                Date startTime = rs.getTimestamp("vbf_start_time");
                Date endTime = rs.getTimestamp("vbf_end_time");
                int dayIndex = rs.getInt("vbf_day_index");
                int vbfId = rs.getInt("vbf_id");
                int csdId = rs.getInt("csd_id");
                for (Iterator iter = drivers.iterator(); iter.hasNext();){
                    CorporateEntity driver = (CorporateEntity) iter.next();
                    if (driver.getID().intValue() == driverSkdgrpId && !addedSkdgrpIds.contains(new Integer(driverSkdgrpId))) {
                        addedSkdgrpIds.add(new Integer(driverSkdgrpId));
                        result.add(driver);
                        break; //break the for loop
                    }
                }

                //create the map if not exists

                Map oneNonVolumeCSD = (Map) volumeBestFitMap.get(new Integer(csdId));
                if (oneNonVolumeCSD == null) {
                    oneNonVolumeCSD = new HashMap();
                    volumeBestFitMap.put(new Integer(csdId), oneNonVolumeCSD);
                }

                Map oneDriverBFData = (Map) oneNonVolumeCSD.get(new Integer(driverSkdgrpId));
                if (oneDriverBFData == null) {
                    oneDriverBFData = new HashMap();
                    oneNonVolumeCSD.put(new Integer(driverSkdgrpId), oneDriverBFData);
                }

                //create a new object and put into the map
                VolumeBestFitData volumeBestFitData = new VolumeBestFitData();
                volumeBestFitData.setCsdId(csdId);
                volumeBestFitData.setDriverSkdgrpId(driverSkdgrpId);
                volumeBestFitData.setStartTime(startTime);
                volumeBestFitData.setEndTime(endTime);
                volumeBestFitData.setVbfDayIndex(dayIndex);
                volumeBestFitData.setWrkldStdvolHour(stdVolHours);
                volumeBestFitData.setVbfId(vbfId);

                //put into the map
                oneDriverBFData.put(new Integer(dayIndex), volumeBestFitData);

            }

        } catch (SQLException sqle) {
            throw new RetailException(sqle);
        } finally {
            SQLHelper.cleanUp(ps);
            SQLHelper.cleanUp(rs);
        }
        return result;
    }

    /**
     * Retrieve the corresponding staff requirement ID for the given driverId/job/skill/activity
     *
     * @param skdgrp_id
     * @param job_id
     * @param skill_id
     * @param act_id
     * @param conn
     * @return Vector
     * @throws RetailException
     */
    public static Vector getClientStfDefId(int skdgrp_id, int job_id, int skill_id, int act_id, DBConnection conn) throws RetailException{
    	Vector result = new Vector();

    	ClientStfDefCache cStfdefCache = ClientStfDefCache.getInstance();
    	result =  cStfdefCache.getClientStfDefId(skdgrp_id, job_id, skill_id, act_id, conn);

        return result;
    }

    /**
     * Retrieves the department list from the hashmap for this location id,
     * creating an empty department list and inserting it if it does not yet exist.
     *
     * @param scheduleAreaToDeptsMap
     * @param locationId
     * @return List
     */
    public static List getDepartments(SOHashtable scheduleAreaToDeptsMap, Integer locationId)
    {
        List departments = (List) scheduleAreaToDeptsMap.get(locationId);
        if (departments == null)
        {
            departments = new ArrayList(5);
            scheduleAreaToDeptsMap.put(locationId, departments);
        }
        return departments;
    }

    /**
     * Retrieve a list of distinct skdgrpIds that have the same job-skill-activity (staff requirement)
     * but has different windows defined
     *
     * @param skdgrp_id
     * @param job_id
     * @param skill_id
     * @param act_id
     * @param dayIndex
     * @param startTime
     * @param endTime
     * @param conn
     * @return Vector
     * @throws RetailException
     */
    public static Vector getOtherClientStfDef(int skdgrp_id,
    									int job_id,
    									int skill_id,
    									int act_id,
    									int dayIndex,
    									Date startTime,
    									Date endTime,
    									DBConnection conn) throws RetailException{
    	Vector result = new Vector();

    	PreparedStatement ps = null;
        ResultSet rs = null;
    	String sql = " SELECT DISTINCT s.skdgrp_id FROM VOLUME_BESTFIT w, so_client_stfdef s " +
    			     " WHERE w.csd_id = s.csd_id " +
    			     //"   AND s.csd_id != ? " +
    			     "   AND s.job_id = ? " +
    			     "   AND s.stskl_id = ? " +
    			     "   AND s.act_id = ? " +
	     			 "   AND w.vbf_day_index = ? " +
	     			 "   AND (w.vbf_start_time != ? " +
	     			 "    OR w.vbf_end_time != ? ) ";

    	try{
    		//Vector csd_ids = getClientStfDefId(skdgrp_id, job_id, skill_id, act_id, conn);

    		//if (csd_ids.size() == 0) return result;

    		//Integer csd_id = (Integer)csd_ids.get(0);

    		ps = conn.prepareStatement(sql);
            //ps.setInt(1, csd_id.intValue());
            ps.setInt(1, job_id);
            ps.setInt(2, skill_id);
            ps.setInt(3, act_id);
            ps.setInt(4, dayIndex);
            ps.setTime(5, new Time(startTime.getTime()));
            ps.setTime(6, new Time(endTime.getTime()));

            rs = ps.executeQuery();

            while (rs.next()) {
                result.add(new Integer(rs.getInt(1)));
            }

        } catch (SQLException sqle) {
            throw new RetailException(sqle);
        } finally {
            SQLHelper.cleanUp(ps);
            SQLHelper.cleanUp(rs);
        }

    	return result;
    }

    public static void modifyIRForBestFitLocations(Schedule m_oSchedule, List bestFitInfo) throws RetailException
    {
        IntervalRequirementsManager IRM = m_oSchedule.getIntervalRequirements();

        List bestFit = new ArrayList(32);
        List nonBestFit = new ArrayList(32);

        List IRRecords = IRM.getRequirementsList();

        for (Iterator iter = IRRecords.iterator(); iter.hasNext();) {
            Record rec = (Record) iter.next();
            if(rec.m_isBestFit){
                bestFit.add(rec);
            }else{
                nonBestFit.add(rec);
            }
        }
        if (bestFit.size() == 0) {
            return;
        }
        /*
        List bestFitInfo = bestFitRecordsToBestFitInfo(bestFit, requirementDataMap);
       */
        Map consolidatedReqs = IntervalRequirementsHelper.consolidateIgnoreSkills(nonBestFit);

        for (Iterator iter = bestFit.iterator(); iter.hasNext();) {
            Record rec = (Record) iter.next();
            rec.resetTotalNeeded();
        }
        ScheduleCoverage scheduleCoverage = new ScheduleCoverage(m_oSchedule, true);
        IntervalRequirementsHelper.calcDeltaCoverage(consolidatedReqs, scheduleCoverage, IRM);

        for (Iterator iterator = bestFitInfo.iterator(); iterator.hasNext();)
        {
            IntervalRequirementsHelper.BestFitInfo info = (IntervalRequirementsHelper.BestFitInfo)iterator.next();
            IntervalRequirementsManager.Record irRecord = info.getRecord();
            String deltaKey = IntervalRequirementsManager.buildKey(irRecord.m_skdgrp.intValue(),irRecord.job.intValue(),null,irRecord.m_Activity.toString());
            IntervalRequirementsManager.Record deltaRecord = (IntervalRequirementsManager.Record)consolidatedReqs.get(deltaKey);

            if (deltaRecord == null) {
                // no volume driven requirements for the key. Create a Record with zero req arrays
                deltaRecord = IRM.new Record(
                        irRecord.m_skdgrp.intValue(), irRecord.job.intValue(),
                        irRecord.m_Skill.toString(), irRecord.m_SkillLevel.toString(),irRecord.m_Activity.toString(),
                        irRecord.m_IntervalType, deltaKey);
            }

            //***int totalValueToAdjust = (int) Math.round(info.getWrkldHrs()); // * 60 / IntervalRequirementsManager.MIN_SLOT_SIZE) ;
            int totalValueToAdjust = (int) Math.round(info.getTotal());

            if (logger.isDebugEnabled())
            {
                logger.debug("Adjusting NVD=("+info+") value="+totalValueToAdjust);
            }

            int totalAdjusted = 0;

            for (int i = info.getFromInterval(); i <= info.getToInterval(); i++)
            {
                 if (deltaRecord.m_TotalNeeded[i] < 0)
                 {
                     int toAdjust = (totalAdjusted + (-deltaRecord.m_TotalNeeded[i])) <= totalValueToAdjust ?
                                     -deltaRecord.m_TotalNeeded[i] : totalValueToAdjust - totalAdjusted;

                     totalAdjusted += toAdjust;

                     irRecord.m_TotalNeeded[i] += toAdjust;
                     deltaRecord.m_TotalNeeded[i] += toAdjust;
                     if (totalAdjusted >= totalValueToAdjust){
                         break;
                     }
                 }
            }
            if(totalAdjusted < totalValueToAdjust){
                /**
                 *  Best fit reqs not covered. Evenly distribute uncovered amount of best fit reqs
                 */
                int toAdjust = (totalValueToAdjust - totalAdjusted ) / (info.getToInterval() - info.getFromInterval()+1);
                if(toAdjust == 0){
                    toAdjust =1;
                }
                for (int i = info.getFromInterval(); i <= info.getToInterval(); i++) {
                    totalAdjusted += toAdjust;

                    irRecord.m_TotalNeeded[i] += toAdjust;
                    if (totalAdjusted >= totalValueToAdjust){
                        break;
                    }
                }
                // evenly distribute remaining
                if (totalAdjusted < totalValueToAdjust){
                    toAdjust = 1;
                    for (int i = info.getFromInterval(); i <= info.getToInterval(); i++) {
                        totalAdjusted += toAdjust;

                        irRecord.m_TotalNeeded[i] += toAdjust;

                        if (totalAdjusted >= totalValueToAdjust){
                            break;
                        }
                    }
                }
            }
        }
    }



}