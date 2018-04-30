package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;

import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;

import java.sql.SQLException;
import java.util.*;
/**
 *  Title:        Shift Premiums Rule With Dates
 *  Description:	WBIAG RULE
 *  Copyright:    Copyright (c) 2002
 *  Company:      Workbrain Inc
 *
 *@author     Andriy Kaspersky / Chris Leigh
 *@version    1.0
 */

public class ShiftPremiumsWDatesRule extends Rule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ShiftPremiumsWDatesRule.class);

    public final static String PARAM_MINIMUM_NUMBER_OF_MINUTES = "MinimumNumberOfMinutes";
    public final static String PARAM_SHIFT_PREMIUM_ZONE_ID = "ShiftPremiumZoneID";
    public final static String PARAM_UPDATE_WORKDETAIL_FIELDS = "UpdateWorkDetailFields";
    public final static String PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY = "ApplyToAllShiftsDuringADay";
    public final static String PARAM_APPLY_HOURTYPE_MULTIPLIER = "ApplyHourTypeMultiplier";


    public List getParameterInfo(DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_MINIMUM_NUMBER_OF_MINUTES, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_SHIFT_PREMIUM_ZONE_ID, RuleParameterInfo.STRING_TYPE));
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_UPDATE_WORKDETAIL_FIELDS, RuleParameterInfo.CHOICE_TYPE);
        rpi.addChoice("true");
        rpi.addChoice("false");
        result.add(rpi);
        RuleParameterInfo rpi1 = new RuleParameterInfo(PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY, RuleParameterInfo.CHOICE_TYPE);
        rpi1.addChoice("true");
        rpi1.addChoice("false");
        result.add(rpi1);
        RuleParameterInfo rpiApplyHrTypeMult = new RuleParameterInfo(PARAM_APPLY_HOURTYPE_MULTIPLIER, RuleParameterInfo.CHOICE_TYPE);
        rpiApplyHrTypeMult.addChoice("true");
        rpiApplyHrTypeMult.addChoice("false");
        result.add(rpiApplyHrTypeMult);

        return result;
    }

    public String getComponentName() {
        return "WBIAG Shift Premiums with Dates Rule ";
    }

    public String getComponentUI() {
        return "/quickrules/qShiftPremiumsWDatesParams.jsp";
  }

    public void execute(WBData wbData, Parameters parameters) throws Exception {
        //if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Executing the Shift Premiums Rule !!!!");}
        // *** Parameters ***
        ParametersResolved pars = new ParametersResolved();
        pars.in_min_minutes = parameters.getIntegerParameter(PARAM_MINIMUM_NUMBER_OF_MINUTES, 0);
        pars.in_spzone_id = parameters.getParameter(PARAM_SHIFT_PREMIUM_ZONE_ID, "spzone_id");
        pars.in_wrkdFldsUpdate = parameters.getParameter(PARAM_UPDATE_WORKDETAIL_FIELDS, "false");
        pars.applyToAllDayShifts = parameters.getParameter(PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY, "false");
        pars.useMultipleShifts = "true".equalsIgnoreCase(pars.applyToAllDayShifts);
        pars.tabWrkdDates = new Vector();
        pars.tabCandidates = new HashMap();
        pars.applyHourTypeMultiplier = parameters.getParameter(PARAM_APPLY_HOURTYPE_MULTIPLIER, "false");
        // *** Common things ***
        pars.allCalcGroups = wbData.getRuleData().getCodeMapper().getCalcGroupById(RuleHelper.getAllGroupsId(wbData.getRuleData().getCodeMapper()) );
        pars.allShiftPatterns = wbData.getRuleData().getCodeMapper().getShiftPatternById(RuleHelper.getAllPatternsId(wbData.getRuleData().getCodeMapper()));
        pars.allShifts = wbData.getRuleData().getCodeMapper().getShiftById(RuleHelper.getAllShiftsId(wbData.getRuleData().getCodeMapper()) );

        List spzRecords = new ArrayList();
        if( "spzone_id".equalsIgnoreCase(pars.in_spzone_id) ) {
            spzRecords = wbData.getRuleData().getCodeMapper().getAllShiftPremiumZones();
        } else {
            int id = Integer.parseInt( pars.in_spzone_id );
            ShiftPremiumZoneData spz = wbData.getRuleData().getCodeMapper().getShiftPremiumZoneById(id);
            if( spz != null ) {
                spzRecords.add(spz);
            }
        }
        for (int i = 0, j = spzRecords.size(); i < j; i++) {
            int timeInZone = 0;
            ShiftPremiumZoneData spz = (ShiftPremiumZoneData) spzRecords.get(i);
            pars.in_min_minutes = (spz.getSpzoneMinMinutes() != 0) ? spz.getSpzoneMinMinutes()
                                                              : pars.in_min_minutes;
            pars.finalWrkdFldsUpdate = (StringHelper.isEmpty(spz.getSpzoneUpdateFlds()))
                                  ? (pars.in_wrkdFldsUpdate != null && pars.in_wrkdFldsUpdate.equalsIgnoreCase("true")) ? "wrkd_udf8,wrkd_udf9,wrkd_udf10" : null
                                  : spz.getSpzoneUpdateFlds();
            if (isEligible(spz, wbData, pars)) {
                log("Day is Eligible");
                double numrate = (
                        "Y".equals(spz.getSpzonePercentage())
                         ? spz.getSpzoneRate() / 100 * wbData.getRuleData().getEmployeeData().getEmpBaseRate()
                         : spz.getSpzoneRate()
                        );


                if ("true".equalsIgnoreCase(pars.applyHourTypeMultiplier)) {
                     numrate *= wbData.getRuleData().getCodeMapper().getHourTypeById(spz.getHtypeId()).getHtypeMultiple();
                }

                if (ShiftPremiumZoneData.APPLY_TYPE_ZONE.equals(spz.getSpzoneApplyType())) {
                    // split the workdetails according to the emp schedule so calcMinute(...)
                    // won't add the whole workdetails that spans over the zone time.
                    RuleHelper.splitAt(wbData.getEmployeeScheduleData().getEmpskdActStartTime(), wbData.getRuleData().getWorkDetails());
                    RuleHelper.splitAt(wbData.getEmployeeScheduleData().getEmpskdActEndTime(), wbData.getRuleData().getWorkDetails());

                    boolean bolOtMultiply = (spz.getSpzoneOtMultiply() != null) ? spz.getSpzoneOtMultiply().equalsIgnoreCase("Y") : false;
                    for (int k = 0, l = wbData.getRuleData().getWorkDetailCount(); k < l; k++) {
                        WorkDetailData wd = wbData.getRuleData().getWorkDetail(k);
                        if (RuleHelper.isCodeInList(spz.getSpzoneInclTcodeNames(), wd.getWrkdTcodeName()) &&
                                RuleHelper.isCodeInList(spz.getSpzoneInclHtypeNames(), wd.getWrkdHtypeName())) {
                            timeInZone += calcMinutes(wbData, spz, wd ,
                                bolOtMultiply, pars);
                            if (timeInZone > spz.getSpzoneMaxMinutes()) {
                                break;
                            }
                        }
                    }
                    if (timeInZone >= pars.in_min_minutes) {
                        timeInZone = Math.min(timeInZone, spz.getSpzoneMaxMinutes());
                        // *** Compare Generated Amount with MaxDollars for the Zone
                        if (timeInZone / 60 * numrate > spz.getSpzoneMaxDollars()) {
                            timeInZone = (int) (60 * spz.getSpzoneMaxDollars() / numrate);
                        }
                        timeInZone = (spz.getSpzoneUseMinutes() != 0) ? spz.getSpzoneUseMinutes()
                                                                      : timeInZone;
                        if (StringHelper.isEmpty(spz.getSpzoneBestOfGrp())) {
                            if (pars.finalWrkdFldsUpdate == null || pars.finalWrkdFldsUpdate.trim().length() == 0) {
                                // *** tt8075 ShiftPremiums adds empty premiums with period 0
                                if (timeInZone != 0) {
                                    log ("Adding premium for shift premium zone : " + spz.getSpzoneName() + " for " + timeInZone + " minutes");

                                    WrkdDates premDates = getStartEnd(timeInZone, pars);
                                    log(new StringBuffer().append("Start - End Dates: ").append(premDates.getStartDate())
                                            .append(" - ").append(premDates.getEndDate()).toString());
                                    insertWorkPremiumRecord(timeInZone, spz.getTcodeId(), spz.getHtypeId(), numrate, premDates, wbData);
                                }
                            } else {
                                log ("Affecting details for shift premium zone : " + spz.getSpzoneName());
                                applyToDetails(timeInZone, spz.getTcodeId(),
                                               spz.getHtypeId(), numrate,
                                               wbData, pars);
                            }
                        } else {
                            addCandidate(spz.getSpzoneBestOfGrp(),
                                     numrate,
                                     timeInZone,
                                     spz.getTcodeId(),
                                     spz.getHtypeId(), pars);
                        }
                    }
                // *** if flat rate, just insert a 1 hour premium with given rate,tcode and htype ***
                } else if (ShiftPremiumZoneData.APPLY_TYPE_FLAT_RATE.equals(spz.getSpzoneApplyType())) {
                    if (numrate != 0) {
                        wbData.insertWorkPremiumRecordFlatRate(spz.getTcodeId() ,
                                spz.getHtypeId() ,
                                (numrate > spz.getSpzoneMaxDollars() ? spz.getSpzoneMaxDollars()
                                                                     : numrate)
                        );
                    }
                }
            }
        }
        applyBestOfGroupEntries(wbData, pars);
    }

    private boolean isEligible(ShiftPremiumZoneData spz, WBData wbData,
                               ParametersResolved pars) throws SQLException {
        if (spz == null) {
            return false;
        }
        boolean ret = true;

        long emp_act_start_time_fraction=0;
        long emp_act_end_time_fraction=0;
        long zone_szs_time = (spz.getSpzoneSzsTime() == null ? Long.MIN_VALUE : DateHelper.getDayFraction(spz.getSpzoneSzsTime()));
        long zone_sze_time = (spz.getSpzoneSzeTime() == null ? Long.MAX_VALUE : DateHelper.getDayFraction(spz.getSpzoneSzeTime()));
        long zone_ezs_time = (spz.getSpzoneEzsTime() == null ? Long.MIN_VALUE : DateHelper.getDayFraction(spz.getSpzoneEzsTime()));
        long zone_eze_time = (spz.getSpzoneEzeTime() == null ? Long.MAX_VALUE : DateHelper.getDayFraction(spz.getSpzoneEzeTime()));
        if (zone_sze_time < zone_szs_time) {
            zone_sze_time += DateHelper.DAY_MILLISECODS;
        }
        if (zone_eze_time < zone_ezs_time) {
            zone_eze_time += DateHelper.DAY_MILLISECODS;
        }
        Integer shift2 = wbData.getRuleData().getEmployeeScheduleData().getEmpskdActShiftId2();
        Integer shift3 = wbData.getRuleData().getEmployeeScheduleData().getEmpskdActShiftId3();
        Integer shift4 = wbData.getRuleData().getEmployeeScheduleData().getEmpskdActShiftId4();
        Integer shift5 = wbData.getRuleData().getEmployeeScheduleData().getEmpskdActShiftId5();

        String strShftName = null;
        if (spz.getSpzoneShftToChk().equalsIgnoreCase("WRKS")) {
            strShftName = wbData.getRuleData().getCodeMapper().getShiftById(wbData.getRuleData().getWorkSummary().getShftId()).getShftName();
            emp_act_start_time_fraction = DateHelper.getDayFraction(getShiftStartTime(spz,wbData));
            emp_act_end_time_fraction= DateHelper.getDayFraction(getShiftEndTime(spz,wbData));
        } else if (spz.getSpzoneShftToChk().equalsIgnoreCase("ACT")) {
            strShftName = wbData.getRuleData().getCodeMapper().getShiftById(wbData.getEmployeeScheduleData().getEmpskdActShiftId()).getShftName();
            emp_act_start_time_fraction = DateHelper.getDayFraction(getShiftStartTime(spz,wbData));
            emp_act_end_time_fraction = DateHelper.getDayFraction(getShiftEndTime(spz,wbData));
        } else if (spz.getSpzoneShftToChk().equalsIgnoreCase("DEF")) {
            strShftName = wbData.getRuleData().getCodeMapper().getShiftById(wbData.getEmployeeScheduleData().getEmpskdDefShiftId()).getShftName();
            emp_act_start_time_fraction = DateHelper.getDayFraction(getShiftStartTime(spz,wbData));
            emp_act_end_time_fraction = DateHelper.getDayFraction(getShiftEndTime(spz,wbData));
        }
        // *** CALC GROUPS ***
        ret &=  (pars.allCalcGroups != null && RuleHelper.isCodeInList(spz.getCalcgrpName(), pars.allCalcGroups.getCalcgrpName()))
                    || RuleHelper.isCodeInList(spz.getCalcgrpName(), wbData.getRuleData().getCodeMapper().getCalcGroupById(wbData.getRuleData().getWorkSummary().getCalcgrpId()).getCalcgrpName());
        if (!ret) {
            log ("Calc group check failed");
            return false;
        }
        // *** SHIFT PATTERNS ***
        ret &= (pars.allShiftPatterns != null && RuleHelper.isCodeInList(spz.getShftpatName(), pars.allShiftPatterns.getShftpatName()))
                    || RuleHelper.isCodeInList(spz.getShftpatName(), wbData.getRuleData().getCodeMapper().getShiftPatternById(wbData.getRuleData().getEmployeeData().getShftpatId()).getShftpatName());
        if (!ret) {
            log ("Shift Pattern check failed");
            return false;
        }
        // *** SHIFTS ***
        ret &= (pars.allShifts != null && RuleHelper.isCodeInList(spz.getShftName(), pars.allShifts.getShftName())) ||
                // *** Multiple Shifts ***
                RuleHelper.isCodeInList(spz.getShftName(), strShftName) ||
                  (pars.useMultipleShifts &&
                    (
                      (shift2 != null && RuleHelper.isCodeInList(spz.getShftName(), wbData.getRuleData().getCodeMapper().getShiftById(shift2.intValue()).getShftName())) ||
                      (shift3 != null && RuleHelper.isCodeInList(spz.getShftName(), wbData.getRuleData().getCodeMapper().getShiftById(shift3.intValue()).getShftName())) ||
                      (shift4 != null && RuleHelper.isCodeInList(spz.getShftName(), wbData.getRuleData().getCodeMapper().getShiftById(shift4.intValue()).getShftName())) ||
                      (shift5 != null && RuleHelper.isCodeInList(spz.getShftName(), wbData.getRuleData().getCodeMapper().getShiftById(shift5.intValue()).getShftName()))
                    )
                  );
        if (!ret) {
            log ("Shift name check failed");
            return false;
        }
        ret &= isDayTypeEligible(spz, wbData);
        if (!ret) {
            log ("Day type check failed");
            return false;
        }
        ret &= (emp_act_start_time_fraction >= zone_szs_time) &&
                (emp_act_start_time_fraction <= zone_sze_time) &&
                (emp_act_end_time_fraction >= zone_ezs_time) &&
                (emp_act_end_time_fraction <= zone_eze_time);
        if (!ret) {
            log ("Must start/end before/after check failed");
            return false;
        }
        ret &= workedBeforeAfter (spz , wbData);
        if (!ret) {
            log ("Worked before/after check failed");
            return false;
        }
        ret &= workedMinutes (spz , wbData) ;
        if (!ret) {
            log ("Worked minutes check failed");
            return false;
        }

        return ret;
    }


    private void addCandidate(String spzoneBestOfGroup,
            double wrkpRate, int wrkpMinutes, int tcodeId, int htypeId,
            ParametersResolved pars) {
        double intMagicNumber;
        intMagicNumber = wrkpRate * wrkpMinutes;
        if (intMagicNumber == 0) {
           return;
        }

        if (pars.tabCandidates.containsKey(spzoneBestOfGroup)) {
            Candidate thisCandidate = (Candidate)pars.tabCandidates.get(spzoneBestOfGroup);
            if (intMagicNumber > thisCandidate.getMagicNumber()) {
                thisCandidate.setWrkdDates(getStartEnd(wrkpMinutes, pars));
                thisCandidate.setWrkpRate(wrkpRate);
                thisCandidate.setWrkpMinutes(wrkpMinutes);
                thisCandidate.setTcodeId(tcodeId);
                thisCandidate.setHtypeId(htypeId);
                thisCandidate.setMagicNumber(intMagicNumber);
                pars.tabCandidates.put(spzoneBestOfGroup , thisCandidate);
            }
        }
        else {
            Candidate newCandidate = new Candidate();
            newCandidate.setWrkdDates(getStartEnd(wrkpMinutes, pars));
            newCandidate.setWrkpRate(wrkpRate);
            newCandidate.setWrkpMinutes(wrkpMinutes);
            newCandidate.setTcodeId(tcodeId);
            newCandidate.setHtypeId(htypeId);
            newCandidate.setMagicNumber(intMagicNumber);
            pars.tabCandidates.put(spzoneBestOfGroup , newCandidate);
        }
    }

    private void applyBestOfGroupEntries(WBData wbData,ParametersResolved pars) {

        Iterator it = pars.tabCandidates.entrySet().iterator();
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(pars.tabCandidates);}
        while (it.hasNext()) {
            Candidate oneCandidate =  (Candidate)(((Map.Entry)it.next()).getValue());
            if (oneCandidate.getWrkpMinutes() != 0) {

                insertWorkPremiumRecord(oneCandidate.getWrkpMinutes() ,
                       oneCandidate.getTcodeId(),
                       oneCandidate.getHtypeId(),
                       oneCandidate.getWrkpRate(), oneCandidate.getWrkdDates(), wbData);
            }
       }
   }

    private boolean workedBeforeAfter(ShiftPremiumZoneData spz, WBData wbData) throws SQLException {
        if (!StringHelper.isEmpty(spz.getSpzoneMwBefore())) {
            Date datBefore = new Date (wbData.getRuleData().getWorkSummary().getWrksWorkDate().getTime() +
                    DateHelper.getDayFraction(spz.getSpzoneMwBefore()));
            if (DateHelper.getDifferenceInDays(datBefore, wbData.getEmployeeScheduleData().getEmpskdActStartTime()) > 0.5){
                DateHelper.addDays(datBefore , -1);
            } else {
                DateHelper.addDays(datBefore , 1);
            }
            int wrkMins = wbData.getMinutesWorkDetail(null , datBefore,
                    spz.getSpzoneInclTcodeNames(), true, spz.getSpzoneInclHtypeNames() , true, null);
            if (wrkMins == 0) {
                return false;
            }
        }
        if (!StringHelper.isEmpty(spz.getSpzoneMwAfter())) {
            Date datAfter = new Date (wbData.getRuleData().getWorkSummary().getWrksWorkDate().getTime() +
                    DateHelper.getDayFraction(spz.getSpzoneMwAfter()));
            if (DateHelper.getDifferenceInDays(datAfter, wbData.getEmployeeScheduleData().getEmpskdActEndTime()) > 0.5){
                datAfter = DateHelper.addDays(datAfter , -1);
            } else {
                datAfter = DateHelper.addDays(datAfter , 1);
            }
            int wrkMins = wbData.getMinutesWorkDetail(datAfter , null,
                    spz.getSpzoneInclTcodeNames(), true, spz.getSpzoneInclHtypeNames() , true, null);
            if (wrkMins == 0) {
                return false;
            }
        }
        return true;
    }


    private boolean workedMinutes(ShiftPremiumZoneData spz, WBData wbData) throws SQLException{
        if (!StringHelper.isEmpty(spz.getSpzoneMwMinutes())) {
            // *** percentage shift
            if (spz.getSpzoneMwMinutes().endsWith("%S")) {
                int skdWrkMins = wbData.getMinutesWorkDetail(
                        wbData.getEmployeeScheduleData().getEmpskdActStartTime() ,
                        wbData.getEmployeeScheduleData().getEmpskdActEndTime(), spz.getSpzoneInclTcodeNames(), true,
                        spz.getSpzoneInclHtypeNames() , true, null);
                double wrkPercentage = ((double)skdWrkMins / wbData.getScheduleDuration()) * 100;
                int spPercentage = Integer.parseInt(spz.getSpzoneMwMinutes().substring(0,spz.getSpzoneMwMinutes().length() - 2));
                //if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("%S" + wrkPercentage + "-" + spPercentage);}
                return ( wrkPercentage > spPercentage);

            // *** percentage time
            } else if (spz.getSpzoneMwMinutes().endsWith("%T")) {
                ShiftData sd = ShiftAccess.getNormalizedShiftTimes(spz.getSpzoneMwminStart() ,
                    spz.getSpzoneMwminEnd(),
                    wbData.getRuleData().getWorkSummary().getWrksWorkDate(),
                    wbData.getEmpDayStartTime() );
                sd.setShftStartTime((spz.getSpzoneMwminStart() == null)  ? null : sd.getShftStartTime() );
                sd.setShftEndTime((spz.getSpzoneMwminEnd() == null)  ? null : sd.getShftEndTime() );

                int startEndWrkMins = wbData.getMinutesWorkDetail(
                        sd.getShftStartTime() , sd.getShftEndTime(), spz.getSpzoneInclTcodeNames(), true,
                        spz.getSpzoneInclHtypeNames() , true, null);
                // *** if both start/end time supplied, look at tomorrow and yesterday for possible matches
                if (sd.getShftStartTime() != null && sd.getShftEndTime() != null) {
                    startEndWrkMins += wbData.getMinutesWorkDetail(
                            DateHelper.addDays(sd.getShftStartTime() , -1),
                            DateHelper.addDays(sd.getShftEndTime(), -1),
                            spz.getSpzoneInclTcodeNames(), true,
                            spz.getSpzoneInclHtypeNames() , true, null);
                    startEndWrkMins += wbData.getMinutesWorkDetail(
                            DateHelper.addDays(sd.getShftStartTime() , 1) ,
                            DateHelper.addDays(sd.getShftEndTime() , 1) ,
                            spz.getSpzoneInclTcodeNames(), true,
                            spz.getSpzoneInclHtypeNames() , true, null);
                }

                int allWrkMins = wbData.getMinutesWorkDetail(
                        null,null, spz.getSpzoneInclTcodeNames() , true,
                        spz.getSpzoneInclHtypeNames() , true, null);
                double wrkPercentage = ((double)startEndWrkMins / allWrkMins) * 100;
                int spPercentage = Integer.parseInt(spz.getSpzoneMwMinutes().substring(0,spz.getSpzoneMwMinutes().length() - 2));
                //if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("%T" + wrkPercentage + "-" + spPercentage);}
                return ( wrkPercentage > spPercentage);

            // *** minutes worked scheduled
            } else if (spz.getSpzoneMwMinutes().endsWith("S")){
                int skdWrkMins = wbData.getMinutesWorkDetail(
                        wbData.getEmployeeScheduleData().getEmpskdActStartTime() ,
                        wbData.getEmployeeScheduleData().getEmpskdActEndTime(), spz.getSpzoneInclTcodeNames(), true,
                        spz.getSpzoneInclHtypeNames() , true, null);
                int spMins = Integer.parseInt(spz.getSpzoneMwMinutes().substring(0,spz.getSpzoneMwMinutes().length() - 1));
                //if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("%S" + skdWrkMins + "-" + spMins);}
                return skdWrkMins > spMins;

            // *** minutes worked time
            } else if (spz.getSpzoneMwMinutes().endsWith("T")){
                ShiftData sd = ShiftAccess.getNormalizedShiftTimes(spz.getSpzoneMwminStart() ,
                    spz.getSpzoneMwminEnd(),
                    wbData.getRuleData().getWorkSummary().getWrksWorkDate(),
                    wbData.getEmpDayStartTime() );
                sd.setShftStartTime((spz.getSpzoneMwminStart() == null)  ? null : sd.getShftStartTime() );
                sd.setShftEndTime((spz.getSpzoneMwminEnd() == null)  ? null : sd.getShftEndTime() );

                int startEndWrkMins = wbData.getMinutesWorkDetail(
                        sd.getShftStartTime() , sd.getShftEndTime(), spz.getSpzoneInclTcodeNames(), true,
                        spz.getSpzoneInclHtypeNames() , true, null);
                // *** if both start/end time supplied, look at tomorrow and yesterday for possible matches
                if (sd.getShftStartTime() != null && sd.getShftEndTime() != null) {
                    startEndWrkMins += wbData.getMinutesWorkDetail(
                            DateHelper.addDays(sd.getShftStartTime() , -1),
                            DateHelper.addDays(sd.getShftEndTime(), -1),
                            spz.getSpzoneInclTcodeNames(), true,
                            spz.getSpzoneInclHtypeNames() , true, null);
                    startEndWrkMins += wbData.getMinutesWorkDetail(
                            DateHelper.addDays(sd.getShftStartTime() , 1) ,
                            DateHelper.addDays(sd.getShftEndTime() , 1) ,
                            spz.getSpzoneInclTcodeNames(), true,
                            spz.getSpzoneInclHtypeNames() , true, null);
                }

                int spMins = Integer.parseInt(spz.getSpzoneMwMinutes().substring(0,spz.getSpzoneMwMinutes().length() - 1));
                //if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("%W" + startEndWrkMins + "-" + spMins);}
                return startEndWrkMins > spMins;
            }
        }
        return true;
    }


    /**
     *  Specific check for WEEKDAY, WEEKEND DAY daytypes
     *
     *@param  spz     Description of Parameter
     *@param  wbData  Description of Parameter
     *@return         The DayEligible value
     */
    private boolean isDayTypeEligible(ShiftPremiumZoneData spz, WBData wbData) throws SQLException{
        // *** if allDayTypes in the list, no need to check further eligibility
        if (RuleHelper.isCodeInList(spz.getDaytypName(),
                Integer.toString(DayTypeData.ALL_DAYTYPES_ID))) {
            return true;
        }

        Date dateMajorityMinutesWorked = wbData.getDateMajorityMinutesWorked(spz.getSpzoneInclTcodeNames(), false);
        if (dateMajorityMinutesWorked == null){
            return false;
        }
        int dayOffIndex = wbData.getDayOffIndex(wbData.getRuleData().getEmployeeData(),
                wbData.getRuleData().getWorkSummary().getWrksWorkDate());

        // *** 11583 dayType access has been moved to DayTypeAccess
        return DayTypeAccess.isDayTypeEligible(spz.getDaytypName() ,
                dateMajorityMinutesWorked , dayOffIndex);
    }

    private void applyToDetails(int timeInZone, int tcodeId,
                                int htypeId, double rate, WBData wbData,
                                ParametersResolved pars) {
        int applyMinutes;
        String sFinalWrkdFldsUpdate = StringHelper.searchReplace(pars.finalWrkdFldsUpdate, " ", "").toLowerCase();
        boolean addPremium = sFinalWrkdFldsUpdate.indexOf("true") >= 0;
        String[] fields = StringHelper.detokenizeString(sFinalWrkdFldsUpdate , ",");
        if (fields.length < 3) {
            throw new RuntimeException ("Not enough fields in values spzone_update_fields in shift premium zone");
        }
        for (int i = 0, j = pars.tabWrkdDates.size(); i < j; i++) {
            WrkdDates wDates = (WrkdDates) pars.tabWrkdDates.get(i);
            if (timeInZone >= wDates.getMinutes()) {
                applyMinutes = wDates.getMinutes();
                timeInZone -= wDates.getMinutes();
            } else {
                applyMinutes = timeInZone;
                wDates.setEndDate(wDates.getStartDate().getTime() + (long) timeInZone * DateHelper.MINUTE_MILLISECODS);
                timeInZone = 0;
            }
            // ** adding stuff
            RuleHelper.splitAt(wDates.getStartDate(), wbData.getRuleData().getWorkDetails());
            RuleHelper.splitAt(wDates.getEndDate(), wbData.getRuleData().getWorkDetails());
            for (int k = 0, l = wbData.getRuleData().getWorkDetailCount(); k < l; k++) {
                WorkDetailData wd = wbData.getRuleData().getWorkDetail(k);
                if ((!wd.getWrkdStartTime().before(wDates.getStartDate())) &&
                        (!wd.getWrkdEndTime().after(wDates.getEndDate()))) {
                    if (!fields[0].equalsIgnoreCase("null")) {
                        wd.setField(fields[0] , wbData.getRuleData().getCodeMapper().getTimeCodeById(tcodeId).getTcodeName());
                    }
                    if (!fields[1].equalsIgnoreCase("null")) {
                        wd.setField(fields[1] , wbData.getRuleData().getCodeMapper().getHourTypeById(htypeId).getHtypeName());
                    }
                    if (!fields[2].equalsIgnoreCase("null")) {
                        wd.setField(fields[2] , Double.toString(rate));
                    }
                    k = l;
                    // *** get out of the loop
                }
            }
            if (addPremium) {
                // *** tt8075 ShiftPremiums adds empty premiums with period 0
                if (applyMinutes != 0) {
                    wbData.insertWorkPremiumRecord(applyMinutes, tcodeId, htypeId, rate);
                }
            }
            if (timeInZone == 0) {
                break;
            }
        }
    }

    /* TT 45350 MYassa
     * Changed the function below to compensate for Daylight Savings Time by calculating actual
     * difference instead of zone to zone
     */
    private int calcMinutes(WBData wbData, ShiftPremiumZoneData spz,
            WorkDetailData wd, boolean bolOtMultiply,
            ParametersResolved pars) throws SQLException{

        SimpleTimePeriod stp = new SimpleTimePeriod(spz.getSpzoneStartTime(), spz.getSpzoneEndTime());
        TimePeriod newTP = TimePeriodHelper.getSetToDay(stp, wd.getWrkdStartTime());
        long startZoneFraction = DateHelper.getDayFraction(newTP.retrieveStartDate());
        long endZoneFraction = DateHelper.getDayFraction(newTP.retrieveEndDate());
        int minutes = 0;
        if (startZoneFraction == endZoneFraction) {
            minutes = (int) ((wd.getWrkdEndTime().getTime() - wd.getWrkdStartTime().getTime()) / DateHelper.MINUTE_MILLISECODS);
            addWrkdDates(wd.getWrkdStartTime(), wd.getWrkdEndTime(), minutes, pars);
        } else {
            for (int i = -1; i < 2; i++) {
            	newTP = TimePeriodHelper.getSetToDay(stp, DateHelper.addDays(wd.getWrkdStartTime(), i));
            	Date diffStart = DateHelper.max(newTP.retrieveStartDate(), wd.getWrkdStartTime());
            	Date diffEnd = DateHelper.min(newTP.retrieveEndDate(), wd.getWrkdEndTime());
                if (!StringHelper.isEmpty(spz.getSpzoneSkdUnskd())
                    &&  !spz.getSpzoneSkdUnskd().equalsIgnoreCase("B")) {
                    if (spz.getSpzoneSkdUnskd().equalsIgnoreCase("S")) {
                        if (!(DateHelper.isBetween(wd.getWrkdStartTime() , wbData.getEmployeeScheduleData().getEmpskdActStartTime() , wbData.getEmployeeScheduleData().getEmpskdActEndTime()))
                            && (DateHelper.isBetween(wd.getWrkdEndTime() , wbData.getEmployeeScheduleData().getEmpskdActStartTime() , wbData.getEmployeeScheduleData().getEmpskdActEndTime()))) {
                            // *** outside the schedule, trick the algorithm into not using the time
                            continue;
                        }
                    } else if (spz.getSpzoneSkdUnskd().equalsIgnoreCase("U")) {
                        if ((DateHelper.isBetween(wd.getWrkdStartTime() , wbData.getEmployeeScheduleData().getEmpskdActStartTime() , wbData.getEmployeeScheduleData().getEmpskdActEndTime()))
                            && (DateHelper.isBetween(wd.getWrkdEndTime() , wbData.getEmployeeScheduleData().getEmpskdActStartTime() , wbData.getEmployeeScheduleData().getEmpskdActEndTime()))) {
                            // *** inside the schedule, trick the algorithm into not using the time
                            continue;
                        }
                    }
                }
                if (diffEnd.getTime() > diffStart.getTime()) {
                	int minStart = minutes;
                	if (bolOtMultiply) {
                		minutes += ((diffEnd.getTime() - diffStart.getTime()) * wbData.getRuleData().getCodeMapper().getHourTypeById(wd.getHtypeId()).getHtypeMultiple()) / DateHelper.MINUTE_MILLISECODS;
                	} else {
                		minutes += (diffEnd.getTime() - diffStart.getTime()) / DateHelper.MINUTE_MILLISECODS;
                	}
                	addWrkdDates(
                			diffStart,
                			diffEnd,
                			(int) ((diffEnd.getTime() - diffStart.getTime()) / DateHelper.MINUTE_MILLISECODS)
                            , pars
                			);
                }
            }
        }
        return minutes;
    }

    private void addWrkdDates(Date startDate, Date endDate,
                              int minutes,ParametersResolved pars) {
        addWrkdDates(startDate.getTime(), endDate.getTime(), minutes, pars);
    }

    private void addWrkdDates(long startDate, long endDate, int minutes,
                              ParametersResolved pars) {
        WrkdDates wDates = new WrkdDates();
        wDates.setStartDate(startDate);
        wDates.setEndDate(endDate);
        wDates.setMinutes(minutes);
        pars.tabWrkdDates.add(wDates);
    }

    private java.util.Date getShiftStartTime(ShiftPremiumZoneData spz, WBData wbData) throws SQLException{
        if (spz.getSpzoneShftToChk().equalsIgnoreCase("WRKS"))
            return wbData.getRuleData().getCodeMapper().getShiftById(wbData.getRuleData().getWorkSummary().getShftId()).getShftStartTime();
        if (spz.getSpzoneShftToChk().equalsIgnoreCase("ACT"))
            return wbData.getEmployeeScheduleData().getEmpskdActStartTime();
        if (spz.getSpzoneShftToChk().equalsIgnoreCase("DEF"))
            return wbData.getEmployeeScheduleData().getEmpskdDefStartTime();
        throw new SQLException ("Unknown Shift to check parameter in shift premium zone"+spz.getSpzoneName());
    }
    private java.util.Date getShiftEndTime(ShiftPremiumZoneData spz, WBData wbData) throws SQLException {
        if (spz.getSpzoneShftToChk().equalsIgnoreCase("WRKS"))
            return wbData.getRuleData().getCodeMapper().getShiftById(wbData.getRuleData().getWorkSummary().getShftId()).getShftEndTime();
        if (spz.getSpzoneShftToChk().equalsIgnoreCase("ACT"))
            return wbData.getEmployeeScheduleData().getEmpskdActEndTime();
        if (spz.getSpzoneShftToChk().equalsIgnoreCase("DEF"))
            return wbData.getEmployeeScheduleData().getEmpskdDefEndTime();
        throw new SQLException ("Unknown Shift to check parameter."+spz.getSpzoneName());
    }

    private WrkdDates getStartEnd(int timeInZone,ParametersResolved pars){
        int applyMinutes;

        WrkdDates result = null;

        //The start Date
        if (pars.tabWrkdDates.size() > 0){
            result = (WrkdDates) pars.tabWrkdDates.get(0);
        }

        for (int i = 0, j = pars.tabWrkdDates.size(); i < j; i++) {
            WrkdDates wDates = (WrkdDates) pars.tabWrkdDates.get(i);
            if (timeInZone > wDates.getMinutes()) {
                applyMinutes = wDates.getMinutes();
                timeInZone -= wDates.getMinutes();
            } else {
                applyMinutes = timeInZone;
                result.setEndDate(wDates.getStartDate().getTime() + (long) timeInZone * DateHelper.MINUTE_MILLISECODS);
                timeInZone = 0;
            }

            if (timeInZone == 0) {
                break;
            }
        }

        return result;
    }

    private void insertWorkPremiumRecord(int minutes, int tcodeId, int htypeId,
            double rate, WrkdDates pDate, WBData wbdata) {
        WorkDetailData wd = insertBlankWorkPremiumRecord(pDate, wbdata);
        wd.setWrkdMinutes(minutes);
        wd.setTcodeId(tcodeId);
        wd.setHtypeId(htypeId);
        wd.setWrkdRate(rate);
    }

    private WorkDetailData insertBlankWorkPremiumRecord(WrkdDates Dates, WBData wbdata) {
        Date startDate = Dates.getStartDate();
        Date endDate = Dates.getStartDate();
        WorkDetailData wd = wbdata.getRuleData().getWorkPremiums().add(startDate, endDate, wbdata.getRuleData().getEmpDefaultLabor(0));
        wd.setWrkdType(WorkDetailData.PREMIUM_TYPE);
        return wd;
    }



    protected void log(String msg) {
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(msg);}
    }

    /*public static void main(String[] args) {
        DBConnection c = null;
        try {
            c = SQLHelper.connectToDevl();
            RuleAccess ruleAcceess = new RuleAccess(c);
            RuleData ruleData = ruleAcceess.initialize() load(412795);
            Parameters parameters = new Parameters();
            parameters.addParameter(PARAM_MINIMUM_NUMBER_OF_MINUTES, "0");
            parameters.addParameter(PARAM_SHIFT_PREMIUM_ZONE_ID, "spzone_id");
            parameters.addParameter(PARAM_UPDATE_WORKDETAIL_FIELDS, "false");
            ShiftPremiumsRule aRule = new ShiftPremiumsRule();
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("\n\n\n\n\n\n\n\n\n\n\n\n\n\n");}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("BEFORE ShiftPremiumsRule ... ");}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Work Details :");}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(ruleData.getWorkDetails().toDescription());}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Work Premiums :");}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(ruleData.getWorkPremiums().toDescription());}

            aRule.execute(new WBData(ruleData, c), parameters);
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("AFTER  ShiftPremiumsRule ....");}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Work Details :");}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(ruleData.getWorkDetails().toDescription());}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Work Premiums :");}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(ruleData.getWorkPremiums().toDescription());}
        } catch (Exception e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(e);}
        } finally {
            try {
                c.close();
            } catch (Exception ee) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(ee);}
            }
        }
    }*/

    // ======================================================
    class WrkdDates {
        private Date startDate;
        private Date endDate;
        private int minutes;

        public WrkdDates() {
            startDate = new Date();
            endDate = new Date();
        }

        public void setStartDate(Date date) {
            startDate.setTime(date.getTime());
        }

        public void setEndDate(Date date) {
            endDate.setTime(date.getTime());
        }

        public void setStartDate(long ldate) {
            startDate.setTime(ldate);
        }

        public void setEndDate(long ldate) {
            endDate.setTime(ldate);
        }

        public void setMinutes(int mins) {
            minutes = mins;
        }

        public Date getStartDate() {
            return startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public int getMinutes() {
            return minutes;
        }
    }

    class Candidate {
        private int tcodeId;
        private int htypeId;
        private double wrkpRate;
        private int wrkpMinutes;
        private double magicNumber;
        private WrkdDates wrkdDates;

        public Candidate() {
        }

        public void setWrkdDates(WrkdDates wDates ){
            wrkdDates = wDates;
        }

        public void setTcodeId(int tcodeIdIn) {
            tcodeId = tcodeIdIn;
        }

        public void setHtypeId(int htypeIdIn) {
            htypeId = htypeIdIn;
        }

        public void setWrkpRate(double wrkpRateIn) {
            wrkpRate = wrkpRateIn;
        }

        public void setWrkpMinutes(int wrkpMinutesIn) {
            wrkpMinutes = wrkpMinutesIn;
        }

        public void setMagicNumber(double magicNumberIn) {
            magicNumber = magicNumberIn;
        }

        public WrkdDates getWrkdDates(){
            return wrkdDates;
        }

        public int getTcodeId() {
            return tcodeId;
        }

        public int getHtypeId() {
            return htypeId;
        }

        public double getWrkpRate() {
            return wrkpRate;
        }

        public int getWrkpMinutes() {
            return wrkpMinutes;
        }

        public double getMagicNumber() {
            return magicNumber;
        }
    }

    class ParametersResolved {
        int in_wrks_id;
        int in_min_minutes;
        String in_spzone_id;
        String in_wrkdFldsUpdate;
        String finalWrkdFldsUpdate;
        String applyToAllDayShifts;
        boolean useMultipleShifts = false;
        Vector tabWrkdDates;
        String applyHourTypeMultiplier;

        Date premStart = null;
        Date premEnd = null;

        CalcGroupData allCalcGroups = null;
        ShiftPatternData allShiftPatterns = null;
        ShiftData allShifts = null;
        Map tabCandidates = null;

    }
}