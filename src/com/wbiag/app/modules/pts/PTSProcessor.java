package com.wbiag.app.modules.pts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import com.wbiag.app.ta.model.PTSData;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.ScheduleGroupAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;
import com.workbrain.util.TimeZoneUtil;

/**
 * Title: PTS Processor Description: performs the necessary PTS calculations and
 * updates the PTS table Copyright: Copyright (c) 2005 Company: Workbrain Inc
 * Created: Apr 23, 2005
 * 
 * @author Kevin Tsoi
 */
public class PTSProcessor {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PTSProcessor.class);

    private DBConnection conn = null;
    private CodeMapper codeMapper = null;
    private HashMap PTSCache = null;
    private Date calculationDate = null;
    private String volumeType = null;

    public static final String KEY_DELIMITER = ";";

    //constructor
    public PTSProcessor(DBConnection conn) {
        this.conn = conn;
        DBInterface.init(conn);
        PTSCache = new HashMap();
        calculationDate = new Date();
    }

    //Performs the necessary PTS calculations and updates the PTS table.
    public void updatePTS(int scheduleGroupId, int offset, int dailyOffset, boolean dayOrWeek,
            boolean actualCost, boolean actualEarnings, boolean budgetCost, boolean budgetEarnings,
            String timeCodes, boolean timeCodesInclusive, String hourTypes,
            boolean hourTypesInclusive) throws SQLException, RetailException,
            CloneNotSupportedException {
        HashMap unassignedShifts = null;
        ScheduleGroupData scheduleGroupData = null;
        List costAndEarningsList = null;
        List scheduleGroupDataList = null;
        List departmentIdList = null;
        List teamIdList = null;
        Object[] departmentAndStore = null;
        Date startDate = null;
        Date endDate = null;
        Date minStartDate = null;
        Date maxEndDate = null;
        Iterator it = null;
        String deptSkdGrpName = null;
        String storeSkdGrpName = null;
        int deptSkdGrpId = 0;
        int daysForecasted = 0;

        codeMapper = CodeMapper.createCodeMapper(conn);

        costAndEarningsList = new ArrayList();
        departmentIdList = new ArrayList();
        teamIdList = new ArrayList();

        scheduleGroupDataList = getScheduleGroupDataList(scheduleGroupId);

        if (scheduleGroupDataList != null) {
            //iterator through all departments under scheduleGroup
            it = scheduleGroupDataList.iterator();
            while (it.hasNext()) {
                scheduleGroupData = (ScheduleGroupData) it.next();

                //if schedule group is a store, assign storeSkdGrpId and
                // iterate to next in list
                if (scheduleGroupData.getSkdgrpIntrnlType() == PTSHelper.INTRNL_TYPE_STORE) {
                    storeSkdGrpName = scheduleGroupData.getSkdgrpName();
                }
                //process if schedule group is a schedule area (department)
                else if (scheduleGroupData.getSkdgrpIntrnlType() == PTSHelper.INTRNL_TYPE_SCHEDULE_AREA) {
                    //add department team id to team id list
                    teamIdList.add(new Integer(scheduleGroupData.getWbtId()));

                    deptSkdGrpId = scheduleGroupData.getSkdgrpId().intValue();
                    deptSkdGrpName = scheduleGroupData.getSkdgrpName();

                    //add to departmentIdList to cache
                    departmentIdList.add(scheduleGroupData.getSkdgrpId());

                    //update for week
                    if (dayOrWeek) {
                        daysForecasted = PTSHelper.getDaysForecasted(scheduleGroupData);
                        if (daysForecasted <= 0) {
                            continue;
                        }
                        startDate = PTSHelper.getForecastStartDate(scheduleGroupData, DateHelper
                                .truncateToDays(calculationDate), offset);
                        endDate = DateHelper.addDays(startDate, daysForecasted);
                    }
                    //update for day
                    else {
                        startDate = DateHelper.addDays(DateHelper.truncateToDays(calculationDate),
                                dailyOffset);
                        endDate = startDate;
                    }
                    //calculate cost
                    if (actualCost || budgetCost) {
                        unassignedShifts = cacheUnassignedShifts(deptSkdGrpId, startDate, endDate);
                        if (dayOrWeek) {
                            costAndEarningsList.addAll(calculateCostWeek(unassignedShifts,
                                    deptSkdGrpId, deptSkdGrpName, storeSkdGrpName, timeCodes,
                                    timeCodesInclusive, hourTypes, hourTypesInclusive, startDate,
                                    endDate, budgetCost));
                        }
                        else {
                            costAndEarningsList.addAll(calculateCostDay(deptSkdGrpId,
                                    deptSkdGrpName, storeSkdGrpName, timeCodes, timeCodesInclusive,
                                    hourTypes, hourTypesInclusive, startDate, budgetCost));
                        }
                    }
                    //calculate earnings
                    if (actualEarnings || budgetEarnings) {
                        volumeType = getVolumeType();
                        costAndEarningsList.addAll(calculateEarnings(deptSkdGrpId, storeSkdGrpName,
                                budgetEarnings, startDate, endDate));
                    }
                    //find the min start date and max end date, used to load
                    // pts data into cache
                    if (minStartDate == null && maxEndDate == null) {
                        minStartDate = startDate;
                        maxEndDate = endDate;
                    }
                    else {
                        minStartDate = DateHelper.min(minStartDate, startDate);
                        maxEndDate = DateHelper.max(maxEndDate, endDate);
                    }
                }
            }
            //loads PTS data into cache
            cachePTS(departmentIdList, minStartDate, maxEndDate);

            //update PTS table
            if (actualCost || actualEarnings || budgetCost || budgetEarnings) {
                populate(costAndEarningsList);
            }
            updateTeamsWithCalcDate(teamIdList);
        }
    }

//    //Determines the scheduling cost for a specified department, date range and
//    // timecodes to consider as worked.
//    public List calculateCostWeek(HashMap unassignedShifts, int deptSkdGrpId,
//            String deptSkdGrpName, String storeSkdGrpName, String timeCodes,
//            boolean timeCodesInclusive, String hourTypes, boolean hourTypesInclusive,
//            Date startDate, Date endDate, boolean budget) throws SQLException,
//            CloneNotSupportedException {
//        PTSData ptsData = null;
//        List ptsList = null;
//        List empList = null;
//        WorkDetailList workDetailList = null;
//        WorkDetailData workDetailData = null;
//        Date oldDate = null;
//        Date currentDate = null;
//        Iterator it = null;
//        StringTokenizer timeCodeTokens = null;
//        StringTokenizer hourTypeTokens = null;
//        String currentTCode = null;
//        String currentHType = null;
//        String key = null;
//        boolean matchTcode = false;
//        boolean matchHtype = false;
//        int[] empArray = null;
//        int i = 0;
//        double totalCost = 0;
//
//        ptsList = new ArrayList();
//
//        if (codeMapper == null) {
//            codeMapper = CodeMapper.createCodeMapper(conn);
//        }
//        //load by empIds and date range, ordered by work summary date
//        workDetailList = loadByDeptNameAndDateRange(deptSkdGrpName, startDate, endDate, null, null);
//
//        if (workDetailList.size() > 0) {
//            //total up the cost of details for each work date having an
//            // appropriate time code and hour type
//            it = workDetailList.iterator();
//            while (it.hasNext()) {
//                workDetailData = (WorkDetailData) it.next();
//                workDetailData.setCodeMapper(codeMapper);
//                timeCodeTokens = new StringTokenizer(timeCodes, PTSHelper.REG_DELIMITER);
//                hourTypeTokens = new StringTokenizer(hourTypes, PTSHelper.REG_DELIMITER);
//                currentDate = workDetailData.getWrksWorkDate();
//                matchTcode = false;
//                matchHtype = false;
//
//                //if new date, add ptsData object to list and reset cost
//                if (oldDate == null || !DateHelper.equals(currentDate, oldDate)) {
//                    if (oldDate != null) {
//                        //check for unassigned shifts
//                        key = generateKey(deptSkdGrpId, oldDate, PTSHelper.COST, PTSHelper.ACTUAL);
//                        if (unassignedShifts.containsKey(key)) {
//                            totalCost += Double.parseDouble((String) unassignedShifts.get(key));
//                        }
//                        ptsData.setSkdgrpId(deptSkdGrpId);
//                        ptsData.setPtsStoreSkdgrpName(storeSkdGrpName);
//                        ptsData.setPtsValue(totalCost);
//                        ptsData.setPtsWorkdate(oldDate);
//                        ptsData.setPtsType(PTSHelper.COST);
//                        //add budget ptsData object
//                        if (budget) {
//                            ptsData.setPtsCategory(PTSHelper.BUDGET);
//                            ptsList.add(ptsData);
//                            ptsData = (PTSData) ptsData.clone();
//                        }
//                        //always add actual ptsData object
//                        ptsData.setPtsCategory(PTSHelper.ACTUAL);
//                        ptsList.add(ptsData);
//                    }
//                    ptsData = new PTSData();
//                    oldDate = (Date) currentDate.clone();
//                    totalCost = 0;
//                }
//                while (timeCodeTokens.hasMoreTokens()) {
//                    currentTCode = timeCodeTokens.nextToken();
//                    if (currentTCode.equalsIgnoreCase(workDetailData.getWrkdTcodeName())) {
//                        matchTcode = true;
//                        break;
//                    }
//                }
//                while (hourTypeTokens.hasMoreTokens()) {
//                    currentHType = hourTypeTokens.nextToken();
//                    if (currentHType.equalsIgnoreCase(workDetailData.getWrkdHtypeName())) {
//                        matchHtype = true;
//                        break;
//                    }
//                }
//                if (((timeCodesInclusive && matchTcode) || (!timeCodesInclusive && !matchTcode))
//                        && ((hourTypesInclusive && matchHtype) || (!hourTypesInclusive && !matchHtype))) {
//                    totalCost += Math.max(
//                            (double) (workDetailData.getWrkdMinutes() * workDetailData
//                                    .getWrkdRate()) / (double) 60, 0);
//                }
//            }
//            //check for unassigned shifts
//            key = generateKey(deptSkdGrpId, oldDate, PTSHelper.COST, PTSHelper.ACTUAL);
//            if (unassignedShifts.containsKey(key)) {
//                totalCost += Double.parseDouble((String) unassignedShifts.get(key));
//            }
//            //add last day to list
//            ptsData.setSkdgrpId(deptSkdGrpId);
//            ptsData.setPtsStoreSkdgrpName(storeSkdGrpName);
//            ptsData.setPtsValue(totalCost);
//            ptsData.setPtsWorkdate(oldDate);
//            ptsData.setPtsType(PTSHelper.COST);
//            //add buget ptsData object
//            if (budget) {
//                ptsData.setPtsCategory(PTSHelper.BUDGET);
//                ptsList.add(ptsData);
//                ptsData = (PTSData) ptsData.clone();
//            }
//            //always add actual ptsData object
//            ptsData.setPtsCategory(PTSHelper.ACTUAL);
//            ptsList.add(ptsData);
//        }
//        return ptsList;
//    }

    //Determines the scheduling cost for a specified department, date range and
    // timecodes to consider as worked.
    public List calculateCostWeek(HashMap unassignedShifts, int deptSkdGrpId,
            String deptSkdGrpName, String storeSkdGrpName, String timeCodes,
            boolean timeCodesInclusive, String hourTypes, boolean hourTypesInclusive,
            Date startDate, Date endDate, boolean budget) throws SQLException,
            CloneNotSupportedException {
        PTSData ptsData = null;
        List ptsList = null;
        List empList = null;
        WorkDetailList workDetailList = null;
        WorkDetailData workDetailData = null;
        Date currentDate = null;
        Iterator it = null;
        StringTokenizer timeCodeTokens = null;
        StringTokenizer hourTypeTokens = null;
        String currentTCode = null;
        String currentHType = null;
        String key = null;
        boolean matchTcode = false;
        boolean matchHtype = false;
        int[] empArray = null;
        int i = 0;
        double totalCost = 0;

        ptsList = new ArrayList();

        if (codeMapper == null) {
            codeMapper = CodeMapper.createCodeMapper(conn);
        }
        //load by empIds and date range, ordered by work summary date
        workDetailList = loadByDeptNameAndDateRange(deptSkdGrpName, startDate, endDate, null, null);

        currentDate = startDate;
        GregorianCalendar calCurrentDate = new GregorianCalendar();
        calCurrentDate.setTime(currentDate);
        while (currentDate.compareTo(endDate) <= 0) {
            
            ptsData = new PTSData();
            totalCost = 0;

            if (workDetailList.size() > 0) {
                //total up the cost of details having an appropriate time code and hour type
                it = workDetailList.iterator();
                while (it.hasNext()) {
                    workDetailData = (WorkDetailData) it.next();
                    
                    if (workDetailData.getWrkdWorkDate().equals(currentDate)) {
                        
	                    workDetailData.setCodeMapper(codeMapper);
	                    timeCodeTokens = new StringTokenizer(timeCodes, PTSHelper.REG_DELIMITER);
	                    hourTypeTokens = new StringTokenizer(hourTypes, PTSHelper.REG_DELIMITER);
	                    matchTcode = false;
	                    matchHtype = false;
	
	                    while (timeCodeTokens.hasMoreTokens()) {
	                        currentTCode = timeCodeTokens.nextToken();
	                        if (currentTCode.equalsIgnoreCase(workDetailData.getWrkdTcodeName())) {
	                            matchTcode = true;
	                            break;
	                        }
	                    }
	                    while (hourTypeTokens.hasMoreTokens()) {
	                        currentHType = hourTypeTokens.nextToken();
	                        if (currentHType.equalsIgnoreCase(workDetailData.getWrkdHtypeName())) {
	                            matchHtype = true;
	                            break;
	                        }
	                    }
	                    if (((timeCodesInclusive && matchTcode) || (!timeCodesInclusive && !matchTcode))
	                            && ((hourTypesInclusive && matchHtype) || (!hourTypesInclusive && !matchHtype))) {
	                        totalCost += Math.max(
	                                (double) (workDetailData.getWrkdMinutes() * workDetailData
	                                        .getWrkdRate()) / (double) 60, 0);
	                    }
                    }
                    // if we haven't reached current date
	                else if (workDetailData.getWrkdWorkDate().before(currentDate)) {
	                    continue;	                    
	                }
	                // we passed current date - we need to move on to next day
	                else {
	                    break;
	                }
                }
            }
            //check for unassigned shifts
            key = generateKey(deptSkdGrpId, currentDate, PTSHelper.COST, PTSHelper.ACTUAL);
            if (unassignedShifts.containsKey(key)) {
                totalCost += Double.parseDouble((String) unassignedShifts.get(key));
            }
            ptsData.setSkdgrpId(deptSkdGrpId);
            ptsData.setPtsStoreSkdgrpName(storeSkdGrpName);
            ptsData.setPtsValue(totalCost);
            ptsData.setPtsWorkdate(currentDate);
            ptsData.setPtsType(PTSHelper.COST);
            //add budget ptsData object
            if (budget) {
                ptsData.setPtsCategory(PTSHelper.BUDGET);
                ptsList.add(ptsData);
                ptsData = (PTSData) ptsData.clone();
            }
            //always add actual ptsData object
            ptsData.setPtsCategory(PTSHelper.ACTUAL);
            ptsList.add(ptsData);

            //advance one day
            calCurrentDate.add(Calendar.DATE, 1);
            currentDate = calCurrentDate.getTime();
        }
        return ptsList;
    }

    //Determines the scheduling cost for calculation day
    public List calculateCostDay(int deptSkdGrpId, String deptSkdGrpName, String storeSkdGrpName,
            String timeCodes, boolean timeCodesInclusive, String hourTypes,
            boolean hourTypesInclusive, Date workDate, boolean budget) throws SQLException,
            CloneNotSupportedException {
        PTSData ptsData = null;
        List ptsList = null;
        List empList = null;
        WorkDetailList workDetailList = null;
        WorkDetailData workDetailData = null;
        Date startTime = null;
        Date endTime = null;
        Date adjustedEndTime = null;
        Iterator it = null;
        StringTokenizer timeCodeTokens = null;
        StringTokenizer hourTypeTokens = null;
        String currentTCode = null;
        String currentHType = null;
        boolean matchTcode = false;
        boolean matchHtype = false;
        int[] empArray = null;
        int minutes = 0;
        int i = 0;
        double totalCost = 0;

        ptsList = new ArrayList();

        if (codeMapper == null) {
            codeMapper = CodeMapper.createCodeMapper(conn);
        }

        //Load by empIds and date, ordered by work summary date. Also gets employee's timezone
        workDetailList = loadByDeptNameAndDate(deptSkdGrpName, workDate, null, null);

        ptsData = new PTSData();

        if (workDetailList.size() > 0) {
            //total up the cost of details having an appropriate time code and hour type
            it = workDetailList.iterator();
            while (it.hasNext()) {
                workDetailData = (WorkDetailData) it.next();
                workDetailData.setCodeMapper(codeMapper);
                timeCodeTokens = new StringTokenizer(timeCodes, PTSHelper.REG_DELIMITER);
                hourTypeTokens = new StringTokenizer(hourTypes, PTSHelper.REG_DELIMITER);
                matchTcode = false;
                matchHtype = false;

                while (timeCodeTokens.hasMoreTokens()) {
                    currentTCode = timeCodeTokens.nextToken();
                    if (currentTCode.equalsIgnoreCase(workDetailData.getWrkdTcodeName())) {
                        matchTcode = true;
                        break;
                    }
                }
                while (hourTypeTokens.hasMoreTokens()) {
                    currentHType = hourTypeTokens.nextToken();
                    if (currentHType.equalsIgnoreCase(workDetailData.getWrkdHtypeName())) {
                        matchHtype = true;
                        break;
                    }
                }
                if (((timeCodesInclusive && matchTcode) || (!timeCodesInclusive && !matchTcode))
                        && ((hourTypesInclusive && matchHtype) || (!hourTypesInclusive && !matchHtype))) {
                    //gets the calculation time for the employee's timezone
                    startTime = workDetailData.getWrkdStartTime();
                    endTime = workDetailData.getWrkdEndTime();
                    adjustedEndTime = TimeZoneUtil.getAdjustedDateForTimeZone(calculationDate,
                            TimeZone.getTimeZone(workDetailData.getWrkdUdf1()));

                    //sets the end time to calculation time
                    if (adjustedEndTime.before(startTime) || adjustedEndTime.equals(startTime)) {
                        minutes = 0;
                    }
                    else if (adjustedEndTime.after(startTime) && adjustedEndTime.before(endTime)) {
                        minutes = (int) DateHelper.getMinutesBetween(adjustedEndTime,
                                workDetailData.getWrkdStartTime());
                    }
                    else {
                        minutes = workDetailData.getWrkdMinutes();
                    }
                    totalCost += Math.max((double) (minutes * workDetailData.getWrkdRate()) / (double) 60, 0);
                }
            }
        }
        ptsData.setSkdgrpId(deptSkdGrpId);
        ptsData.setPtsStoreSkdgrpName(storeSkdGrpName);
        ptsData.setPtsValue(totalCost);
        ptsData.setPtsWorkdate(workDate);
        ptsData.setPtsType(PTSHelper.COST);
        //add budget ptsData object
        if (budget) {
            ptsData.setPtsCategory(PTSHelper.BUDGET);
            ptsList.add(ptsData);
            ptsData = (PTSData) ptsData.clone();
        }
        //always add actual ptsData object
        ptsData.setPtsCategory(PTSHelper.ACTUAL);
        ptsList.add(ptsData);
        return ptsList;
    }

    //Determines the amount earned for a specified department, category, and date.
    public List calculateEarnings(int deptSkdGrpId, String storeSkdGrpName, boolean budget,
            Date startDate, Date endDate) throws SQLException, CloneNotSupportedException {
        List earningsList = null;

        //if budget, then add forecast and actual earnings to list (loadForecastEarnings does both)
        if (budget) {
            earningsList = loadForecastEarnings(deptSkdGrpId, storeSkdGrpName, startDate, endDate);
        }
        //otherwise, just add forecast to list
        else {
            earningsList = loadActualEarnings(deptSkdGrpId, storeSkdGrpName, startDate, endDate);
        }
        return earningsList;
    }

    //Populates the PTS table accordingly
    public void populate(List PTSDataList) throws SQLException {
        RecordAccess ptsAccess = null;
        PTSData ptsData = null;
        List inserts = null;
        List updates = null;
        Iterator it = null;
        StringTokenizer hashValue = null;
        String key = null;
        double value = 0;
        int ptsId = 0;
        int insertCount = 0;
        int sequenceNumber = 0;

        inserts = new ArrayList();
        updates = new ArrayList();

        ptsAccess = new RecordAccess(conn);

        //compare with cache to determine what needs to be inserted and what needs to be updated
        it = PTSDataList.iterator();
        while (it.hasNext()) {
            ptsData = (PTSData) it.next();
            key = generateKey(ptsData.getSkdgrpId(), ptsData.getPtsWorkdate(),
                    ptsData.getPtsType(), ptsData.getPtsCategory());
            //if already exist in cache
            if (PTSCache.containsKey(key)) {
                hashValue = new StringTokenizer((String) PTSCache.get(key), KEY_DELIMITER);
                value = Double.parseDouble((String) hashValue.nextToken());
                ptsId = Integer.parseInt((String) hashValue.nextToken());
                //update if value has changed
                if (value != ptsData.getPtsValue()) {
                    ptsData.setPtsId(ptsId);
                    updates.add(ptsData);
                }
            }
            //not in cache, so insert
            else {
                inserts.add(ptsData);
            }
        }

        //getting the sequence numbers from the database
        insertCount = inserts.size();
        if (insertCount > 0) {
            sequenceNumber = conn.getDBSequence("SEQ_PTS_ID").getNextValue(insertCount);
        }

        //setting pts_ids for the inserts
        it = inserts.iterator();
        while (it.hasNext()) {
            ptsData = (PTSData) it.next();
            ptsData.setPtsId(sequenceNumber++);
        }

        ptsAccess.updateRecordData(updates, PTSData.TABLE_NAME, PTSData.PTS_ID);
        ptsAccess.insertRecordData(inserts, PTSData.TABLE_NAME);
    }

    //returns a list of scheduleGroupData under the given scheduleGroupId
    public List getScheduleGroupDataList(int skdGrpId) throws RetailException {
        List scheduleGroupDataList = null;
        List corporateEntityIdsList = null;
        ScheduleGroupAccess scheduleGroupAccess = null;
        ScheduleGroupData scheduleGroupData = null;
        CorporateEntity corporateEntity = null;
        Iterator it = null;
        int corporateEntityId = 0;

        scheduleGroupDataList = new ArrayList();

        scheduleGroupAccess = new ScheduleGroupAccess(conn);
        scheduleGroupData = (ScheduleGroupData) scheduleGroupAccess.loadRecordDataByPrimaryKey(
                new ScheduleGroupData(), skdGrpId);

        if (scheduleGroupData != null) {
            corporateEntity = new CorporateEntity(scheduleGroupData);

            //get corporate entity ids under the given skdgrpId
            corporateEntityIdsList = corporateEntity
                    .getCorporateEntityIDList(CorporateEntity.ALL_SUBTREE);

            if (corporateEntityIdsList != null) {
                //loads schedule group data for each corporate entity id and
                // add to list. Need to load 1 at a time to preserve order.
                it = corporateEntityIdsList.iterator();
                while (it.hasNext()) {
                    corporateEntityId = Integer.parseInt((String) it.next());
                    scheduleGroupData = (ScheduleGroupData) scheduleGroupAccess
                            .loadRecordDataByPrimaryKey(new ScheduleGroupData(), corporateEntityId);
                    scheduleGroupDataList.add(scheduleGroupData);
                }
            }
        }
        return scheduleGroupDataList;
    }

    //Gets list of ptsData objects for forecasted earnings.
    public List loadForecastEarnings(int deptSkdGrpId, String storeSkdGrpName, Date startDate,
            Date endDate) throws SQLException, CloneNotSupportedException {
        List earningsList = null;
        PTSData ptsData = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringTokenizer volumeTypeTokens = null;
        StringBuffer sql = null;
        Date previousDate = null;
        Date currentDate = null;
        boolean empty = true;
        double fcastCalls = 0;
        double fcastAdjVal = 0;
        double value = 0;
        int fcastAdjTyp = 0;
        int countTokens = 0;
        int param = 0;

        earningsList = new ArrayList();

        volumeTypeTokens = new StringTokenizer(volumeType, PTSHelper.REG_DELIMITER);
        countTokens = volumeTypeTokens.countTokens();

        sql = new StringBuffer();
        sql.append("SELECT FD.fcast_date, FD.fcast_adjtyp, FD.fcast_calls, FD.fcast_adjval ");
        sql.append("FROM so_fcast_detail FD, so_fcast F, so_schedule_group SG, so_volume_type VT ");
        sql.append("WHERE FD.fcast_id = F.fcast_id ");
        sql.append("AND F.skdgrp_id = SG.skdgrp_id ");
        sql.append("AND SG.voltyp_id=VT.voltyp_id ");
        sql.append("AND SG.skdgrp_parent_id = ? ");
        sql.append("AND FD.fcast_date BETWEEN ? AND ? ");
        sql.append("AND VT.voltyp_name in ( ");
        for (int i = 0; i < countTokens; i++) {
            sql.append(i > 0 ? ",?" : "?");
        }
        sql.append(") ");
        sql.append("group by FD.fcast_date, FD.fcast_adjtyp, FD.fcast_calls, FD.fcast_adjval ");

        try {
            param = 1;
            ps = conn.prepareStatement(sql.toString());
            ps.setInt(param++, deptSkdGrpId);
            ps.setTimestamp(param++, DateHelper.toDatetime(startDate));
            ps.setTimestamp(param++, DateHelper.toDatetime(endDate));
            while (volumeTypeTokens.hasMoreTokens()) {
                ps.setString(param++, volumeTypeTokens.nextToken());
            }
            rs = ps.executeQuery();

            //creates earnings list
            while (rs.next()) {
                empty = false;
                currentDate = rs.getTimestamp(1);

                if (previousDate == null) {
                    previousDate = currentDate;
                }
                else if (!previousDate.equals(currentDate)) {
                    //create pts object for previous date
                    ptsData = new PTSData();
                    ptsData.setSkdgrpId(deptSkdGrpId);
                    ptsData.setPtsStoreSkdgrpName(storeSkdGrpName);
                    ptsData.setPtsWorkdate(previousDate);
                    ptsData.setPtsValue(value);
                    ptsData.setPtsType(PTSHelper.EARNED);
                    ptsData.setPtsCategory(PTSHelper.BUDGET);
                    earningsList.add(ptsData);
                    ptsData = (PTSData) ptsData.clone();
                    ptsData.setPtsCategory(PTSHelper.ACTUAL);
                    earningsList.add(ptsData);

                    //reset value
                    value = 0;
                    previousDate = currentDate;
                }
                //calculate and set pts value
                fcastAdjTyp = rs.getInt(2);
                fcastCalls = rs.getDouble(3);
                fcastAdjVal = rs.getDouble(4);
                if (fcastAdjTyp == 1) {
                    value += fcastCalls + fcastAdjVal * fcastCalls * 0.01;
                }
                else {
                    value += fcastCalls + fcastAdjVal;
                }
            }
            //add pts data for last date
            if (!empty) {
                ptsData = new PTSData();
                ptsData.setSkdgrpId(deptSkdGrpId);
                ptsData.setPtsStoreSkdgrpName(storeSkdGrpName);
                ptsData.setPtsWorkdate(previousDate);
                ptsData.setPtsValue(value);
                ptsData.setPtsType(PTSHelper.EARNED);
                ptsData.setPtsCategory(PTSHelper.BUDGET);
                earningsList.add(ptsData);
                ptsData = (PTSData) ptsData.clone();
                ptsData.setPtsCategory(PTSHelper.ACTUAL);
                earningsList.add(ptsData);
            }
        }
        finally {
            SQLHelper.cleanUp(ps, rs);
        }
        return earningsList;
    }

    //Gets list of ptsData objects for actual earnings.
    public List loadActualEarnings(int deptSkdGrpId, String storeSkdGrpName, Date startDate,
            Date endDate) throws SQLException {
        List earningsList = null;
        PTSData ptsData = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringTokenizer volumeTypeTokens = null;
        StringBuffer sql = null;
        int countTokens = 0;
        int param = 0;

        earningsList = new ArrayList();

        volumeTypeTokens = new StringTokenizer(volumeType, PTSHelper.REG_DELIMITER);
        countTokens = volumeTypeTokens.countTokens();

        sql = new StringBuffer();
        sql.append("SELECT sum(RD.resdet_volume), RD.resdet_date ");
        sql.append("FROM so_results_detail RD, so_schedule_group SG, so_volume_type VT ");
        sql.append("WHERE RD.skdgrp_id = SG.skdgrp_id ");
        sql.append("AND SG.voltyp_id = VT.voltyp_id ");
        sql.append("AND SG.skdgrp_parent_id = ? ");
        sql.append("AND RD.resdet_date BETWEEN ? AND ? ");
        sql.append("AND VT.voltyp_name in ( ");
        for (int i = 0; i < countTokens; i++) {
            sql.append(i > 0 ? ",?" : "?");
        }
        sql.append(")");
        sql.append("group by RD.resdet_date ");

        try {
            param = 1;
            ps = conn.prepareStatement(sql.toString());
            ps.setInt(param++, deptSkdGrpId);
            ps.setTimestamp(param++, DateHelper.toDatetime(startDate));
            ps.setTimestamp(param++, DateHelper.toDatetime(endDate));
            while (volumeTypeTokens.hasMoreTokens()) {
                ps.setString(param++, volumeTypeTokens.nextToken());
            }
            rs = ps.executeQuery();

            //create earnings list
            while (rs.next()) {
                ptsData = new PTSData();
                ptsData.setSkdgrpId(deptSkdGrpId);
                ptsData.setPtsStoreSkdgrpName(storeSkdGrpName);
                ptsData.setPtsValue(rs.getDouble(1));
                ptsData.setPtsWorkdate(rs.getTimestamp(2));
                ptsData.setPtsType(PTSHelper.EARNED);
                ptsData.setPtsCategory(PTSHelper.ACTUAL);
                earningsList.add(ptsData);
            }
        }
        finally {
            SQLHelper.cleanUp(ps, rs);
        }
        return earningsList;
    }

    //cache PTS data
    public void cachePTS(List departmentIdList, Date startDate, Date endDate) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Iterator it = null;
        StringBuffer sql = null;
        String hashKey = null;
        String hashValue = null;
        Date workDate = null;
        String type = null;
        String category = null;
        int ptsId = 0;
        int deptSkdGrpId = 0;
        int params = 0;
        double value = 0;

        sql = new StringBuffer();
        sql.append("SELECT pts_id, skdgrp_id, pts_workdate, pts_type, pts_category, pts_value ");
        sql.append("FROM payroll_to_sales ");
        sql.append("WHERE pts_workdate BETWEEN ? AND ? ");
        sql.append("AND skdgrp_id in (");
        for (int i = 0; i < departmentIdList.size(); i++) {
            sql.append(i > 0 ? ",?" : "?");
        }
        sql.append(")");

        try {
            params = 1;

            ps = conn.prepareStatement(sql.toString());
            ps.setTimestamp(params++, DateHelper.toDatetime(startDate));
            ps.setTimestamp(params++, DateHelper.toDatetime(endDate));

            it = departmentIdList.iterator();
            while (it.hasNext()) {
                ps.setInt(params++, ((Integer) it.next()).intValue());
            }
            rs = ps.executeQuery();

            while (rs.next()) {
                ptsId = rs.getInt("pts_id");
                deptSkdGrpId = rs.getInt("skdgrp_id");
                workDate = rs.getTimestamp("pts_workdate");
                type = rs.getString("pts_type");
                category = rs.getString("pts_category");
                value = rs.getDouble("pts_value");
                hashKey = generateKey(deptSkdGrpId, workDate, type, category);
                hashValue = generateValue(value, ptsId);
                PTSCache.put(hashKey, hashValue);
            }
        }
        finally {
            SQLHelper.cleanUp(ps, rs);
        }
    }

    //Returns map of work details/premiums by empId, date range keyed by
    // EmployeeIdAndDate, ordered by wrks_work_date.
    public WorkDetailList loadByDeptNameAndDate(String deptSkdGrpName, java.util.Date workDate,
            String detailPremium, String authorized) throws SQLException {

        StringBuffer sql = new StringBuffer(200);
        sql.append(" SELECT WD.* , WS.wrks_work_date, WS.emp_id, WS.wrks_authorized, TZ.tz_java_name ");
        sql.append(" FROM work_detail WD, work_summary WS, ");
        sql.append(" employee_history EH, timezone TZ, department D ");
        sql.append(" WHERE  WS.wrks_id =  WD.wrks_id ");
        sql.append(" AND EH.emp_id = WS.emp_id ");
        sql.append(" AND EH.tz_id = TZ.tz_id ");
        sql.append(" AND WD.dept_id = D.dept_id ");
        sql.append(" AND (WS.wrks_orig_clocks IS NOT null OR WS.wrks_clocks IS NOT null) ");
        sql.append(" AND D.dept_name = ? ");
        sql.append(" AND WS.wrks_work_date BETWEEN EH.emphist_start_date AND EH.emphist_end_date ");
        sql.append(" AND WS.wrks_work_date = ? ");
        if (!StringHelper.isEmpty(detailPremium)) {
            sql.append(" AND WD.wrkd_type = ?");
        }
        if (!StringHelper.isEmpty(authorized)) {
            sql.append(" AND WS.wrks_authorized = ? ");
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        WorkDetailList list = new WorkDetailList();
        try {
            stmt = conn.prepareStatement(sql.toString());
            int fieldCount = 1;
            stmt.setString(fieldCount++, deptSkdGrpName);
            stmt.setTimestamp(fieldCount++, new java.sql.Timestamp(workDate.getTime()));
            if (!StringHelper.isEmpty(detailPremium)) {
                stmt.setString(fieldCount, detailPremium);
                fieldCount++;
            }
            if (!StringHelper.isEmpty(authorized)) {
                stmt.setString(fieldCount, authorized);
                fieldCount++;
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                WorkDetailData wd = new WorkDetailData();
                wd.assignByName(rs);
                wd.setEmpId(rs.getInt("emp_id"));
                wd.setWrksWorkDate(rs.getDate("wrks_work_date"));
                wd.setWrkdUdf1(rs.getString("tz_java_name"));
                list.add(wd);

            }
        }
        finally {
            SQLHelper.cleanUp(stmt, rs);
        }
        return list;
    }

    //Returns map of work details/premiums by empId, date range keyed by
    // EmployeeIdAndDate.
    public WorkDetailList loadByDeptNameAndDateRange(String deptSkdGrpName,
            java.util.Date startDate, java.util.Date endDate, String detailPremium,
            String authorized) throws SQLException {

        StringBuffer sql = new StringBuffer(200);
        sql.append(" SELECT WD.* , WS.wrks_work_date, WS.emp_id, WS.wrks_authorized ");
        sql.append(" FROM work_detail WD, work_summary WS, department D");
        sql.append(" WHERE  WS.wrks_id =  WD.wrks_id ");
        sql.append(" AND WD.dept_id = D.dept_id ");
        sql.append(" AND D.dept_name = ? ");
        sql.append("  AND WS.wrks_work_date BETWEEN ? AND ? ");
        if (!StringHelper.isEmpty(detailPremium)) {
            sql.append(" AND WD.wrkd_type = ?");
        }
        if (!StringHelper.isEmpty(authorized)) {
            sql.append(" AND WS.wrks_authorized = ? ");
        }

        sql.append("ORDER BY WS.wrks_work_date");

        PreparedStatement stmt = null;
        ResultSet rs = null;
        WorkDetailList list = new WorkDetailList();
        try {
            stmt = conn.prepareStatement(sql.toString());
            int fieldCount = 1;

            stmt.setString(fieldCount, deptSkdGrpName);
            fieldCount++;
            stmt.setTimestamp(fieldCount, new java.sql.Timestamp(startDate.getTime()));
            fieldCount++;
            stmt.setTimestamp(fieldCount, new java.sql.Timestamp(endDate.getTime()));
            fieldCount++;
            if (!StringHelper.isEmpty(detailPremium)) {
                stmt.setString(fieldCount, detailPremium);
                fieldCount++;
            }
            if (!StringHelper.isEmpty(authorized)) {
                stmt.setString(fieldCount, authorized);
                fieldCount++;
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                WorkDetailData wd = new WorkDetailData();
                wd.assignByName(rs);
                wd.setEmpId(rs.getInt("emp_id"));
                wd.setWrksWorkDate(rs.getDate("wrks_work_date"));
                list.add(wd);

            }
        }
        finally {
            SQLHelper.cleanUp(stmt, rs);
        }
        return list;
    }

    //Gets the volume type from the registry.
    public String getVolumeType() {
        if (volumeType == null) {
            volumeType = PTSHelper.getRegistryValue(PTSHelper.REG_PTS_VOLUME_TYPE);
        }
        return volumeType;
    }

    public String generateKey(int deptSkdGrpId, Date workDate, String type, String category) {
        StringBuffer key = null;

        key = new StringBuffer();
        key.append(deptSkdGrpId);
        key.append(KEY_DELIMITER);
        key.append(DateHelper.convertDateString(workDate, PTSHelper.DATE_FORMAT));
        key.append(KEY_DELIMITER);
        key.append(type);
        key.append(KEY_DELIMITER);
        key.append(category);

        return key.toString();
    }

    public String generateValue(double value, int ptsId) {
        StringBuffer key = null;

        key = new StringBuffer();
        key.append(value);
        key.append(KEY_DELIMITER);
        key.append(ptsId);

        return key.toString();
    }

    public String getCalculationDate() {
        return DateHelper.convertDateString(calculationDate, PTSHelper.DATE_TIME_FORMAT);
    }

    //updates the team field with the calculation time
    public void updateTeamsWithCalcDate(List wbtIdList) throws SQLException {
        PreparedStatement ps = null;
        Iterator it = null;
        StringBuffer sql = null;
        String wbtField = null;
        int params = 0;

        wbtField = PTSHelper.getRegistryValue(PTSHelper.REG_PTS_WBT_FIELD_FOR_CALC_DATE);

        sql = new StringBuffer();
        sql.append("UPDATE ");
        sql.append("workbrain_team ");
        sql.append("SET ");
        sql.append(wbtField);
        sql.append(" = ? ");
        sql.append("WHERE ");
        sql.append("wbt_id in (");
        for (int i = 0; i < wbtIdList.size(); i++) {
            sql.append(i > 0 ? ",?" : "?");
        }
        sql.append(")");

        try {
            params = 1;
            ps = conn.prepareStatement(sql.toString());
            ps.setString(params++, getCalculationDate());

            it = wbtIdList.iterator();
            while (it.hasNext()) {
                ps.setInt(params++, ((Integer) it.next()).intValue());
            }
            ps.executeUpdate();
        }
        finally {
            SQLHelper.cleanUp(ps);
        }
    }

    public HashMap cacheUnassignedShifts(int departmentId, Date startDate, Date endDate)
            throws SQLException {
        HashMap unassignedShifts = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuffer sql = null;
        String jobRateIndexStr = null;
        Date oldShiftDate = null;
        boolean hasResults = false;
        int oldSkdGrpId = 0;
        int jobRateIndex = 0;

        //params
        Date shiftDate = null;
        Date startTime = null;
        Date endTime = null;
        String key = null;
        int skdGrpId = 0;
        double rate = 0;
        double value = 0;

        unassignedShifts = new HashMap();

        jobRateIndexStr = PTSHelper.getRegistryValue(PTSHelper.REG_PTS_JOB_RATE_INDEX);

        if (!StringHelper.isEmpty(jobRateIndexStr)) {
            jobRateIndex = Integer.parseInt(jobRateIndexStr);
        }

        sql = new StringBuffer();
        sql.append(" SELECT ");
        sql.append(" SD.shftdet_wrk_loc, ");
        sql.append(" SS.skdshft_date, ");
        sql.append(" SS.skdshft_start_time, ");
        sql.append(" SS.skdshft_end_time, ");
        sql.append(" JR.jobrate_rate ");

        sql.append(" FROM ");
        sql.append(" so_shift_detail SD, ");
        sql.append(" so_scheduled_shift SS, ");
        sql.append(" job_rate JR ");

        sql.append(" WHERE ");
        sql.append(" SD.skdshft_id = SS.skdshft_id ");
        sql.append(" AND ");
        sql.append(" SD.job_id = JR.job_id ");
        sql.append(" AND ");
        sql.append(" JR.jobrate_index = ? ");
        sql.append(" AND ");
        sql.append(" SS.skdshft_date between ? AND ? ");
        sql.append(" AND ");
        sql.append(" SD.shftdet_wrk_loc = ? ");
        sql.append(" AND ");
        sql.append(" SD.emp_id is null ");

        sql.append(" ORDER BY ");
        sql.append(" SD.shftdet_wrk_loc, ");
        sql.append(" SS.skdshft_date, ");
        sql.append(" SS.skdshft_start_time, ");
        sql.append(" SS.skdshft_end_time, ");
        sql.append(" JR.jobrate_rate ");

        try {
            ps = conn.prepareStatement(sql.toString());

            ps.setInt(1, jobRateIndex);
            ps.setTimestamp(2, DateHelper.toTimestamp(startDate));
            ps.setTimestamp(3, DateHelper.toTimestamp(endDate));
            ps.setInt(4, departmentId);
            rs = ps.executeQuery();

            while (rs.next()) {
                hasResults = true;
                skdGrpId = rs.getInt("shftdet_wrk_loc");
                shiftDate = rs.getTimestamp("skdshft_date");
                startTime = rs.getTimestamp("skdshft_start_time");
                endTime = rs.getTimestamp("skdshft_end_time");
                rate = rs.getDouble("jobrate_rate");

                //first pass
                if (oldShiftDate == null) {
                    oldShiftDate = shiftDate;
                    oldSkdGrpId = skdGrpId;
                }
                //cache data if department or date changed
                else if (!oldShiftDate.equals(shiftDate) || oldSkdGrpId != skdGrpId) {
                    key = generateKey(skdGrpId, oldShiftDate, PTSHelper.COST, PTSHelper.ACTUAL);
                    unassignedShifts.put(key, String.valueOf(value));
                    value = 0;
                    oldShiftDate = shiftDate;
                    oldSkdGrpId = skdGrpId;
                }
                value += rate * DateHelper.getHoursBetween(endTime, startTime);
            }
            if (hasResults) {
                key = generateKey(skdGrpId, oldShiftDate, PTSHelper.COST, PTSHelper.ACTUAL);
                unassignedShifts.put(key, String.valueOf(value));
            }
        }
        finally {
            SQLHelper.cleanUp(ps, rs);
        }
        return unassignedShifts;
    }
}
