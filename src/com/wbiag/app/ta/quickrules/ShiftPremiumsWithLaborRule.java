package com.wbiag.app.ta.quickrules;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.server.registry.*;

import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;

import java.sql.SQLException;
import java.util.*;
/**
 */
public class ShiftPremiumsWithLaborRule extends Rule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ShiftPremiumsWithLaborRule.class);

    public final static String PARAM_MINIMUM_NUMBER_OF_MINUTES = "MinimumNumberOfMinutes";
    public final static String PARAM_SHIFT_PREMIUM_ZONE_ID = "ShiftPremiumZoneID";
    public final static String PARAM_UPDATE_WORKDETAIL_FIELDS = "UpdateWorkDetailFields";
    public final static String PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY = "ApplyToAllShiftsDuringADay";
    public final static String PARAM_APPLY_HOURTYPE_MULTIPLIER = "ApplyHourTypeMultiplier";
    public final static String REGISTRY_UNPAID_HTYPE = "/system/TA_CONSTANTS/UNPAID_HTYPE_NAME";
    public final static String PARAM_PREMIUM_LABOR_METHOD = "MethodLaborMetric";
    public final static String PARAM_PREMIUM_LABOR_VALUE = "MethodLaborValue";

    /**************************
     * DONOT PUT INSTANCE VARIABLES FOR QUICKRULES/RULES
     * ALL CLASS VARIABLES SHOULD BE STATIC and FINAL
     **************************/

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
        RuleParameterInfo rpiLaborMthd = new RuleParameterInfo(
            PARAM_PREMIUM_LABOR_METHOD, RuleParameterInfo.CHOICE_TYPE);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_DEFAULT_METHOD);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_FIRST_METHOD);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_LAST_METHOD);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_HOUR_FOR_HOUR_METHOD);
        //rpiLaborMthd.addChoice(PARAM_PLM_VAL_PRORATED_METHOD);
        rpiLaborMthd.addChoice(PremiumWithLabor.PARAM_PLM_VAL_FLAT_RATE_THRESHOLD_METHOD);
        result.add(rpiLaborMthd);
        result.add(new RuleParameterInfo(PARAM_PREMIUM_LABOR_VALUE , RuleParameterInfo.STRING_TYPE));
        return result;
    }

    public String getComponentName() {
        return "WBIAG : Shift Premiums Rule With Labor";
    }

    public void validateSPZoneHTypes(List spz, WBData wbData){
    	Iterator iter = spz.iterator();
    	int defaultUnpaidID = wbData.getCodeMapper().getHourTypeByName(Registry.getVarString(REGISTRY_UNPAID_HTYPE, "UNPAID")).getHtypeId();
    	while (iter.hasNext()){
    		ShiftPremiumZoneData zone = (ShiftPremiumZoneData)iter.next();
    		if (zone.getHtypeId() == Integer.MIN_VALUE){
    			zone.setHtypeId(defaultUnpaidID);
    		}
    	}
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {
        //if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Executing the Shift Premiums Rule !!!!");}
        // *** Parameters ***
        ShiftPremiumsWithLaborRuleVariables sprVars = new ShiftPremiumsWithLaborRuleVariables();
        sprVars.in_min_minutes = parameters.getIntegerParameter(PARAM_MINIMUM_NUMBER_OF_MINUTES, 0);
        sprVars.in_spzone_id = parameters.getParameter(PARAM_SHIFT_PREMIUM_ZONE_ID, "spzone_id");
        sprVars.in_wrkdFldsUpdate = parameters.getParameter(PARAM_UPDATE_WORKDETAIL_FIELDS, "false");
        sprVars.applyToAllDayShifts = parameters.getParameter(PARAM_APPLY_TO_ALLSHIFTS_DURINGADAY, "false");
        sprVars.useMultipleShifts = "true".equalsIgnoreCase(sprVars.applyToAllDayShifts);
        sprVars.tabWrkdDates = new Vector();
        sprVars.tabCandidates = new HashMap();
        sprVars.applyHourTypeMultiplier = parameters.getParameter(PARAM_APPLY_HOURTYPE_MULTIPLIER, "false");
        // *** Common things ***
        sprVars.allCalcGroups = wbData.getRuleData().getCodeMapper().getCalcGroupById(RuleHelper.getAllGroupsId(wbData.getRuleData().getCodeMapper()) );
        sprVars.allShiftPatterns = wbData.getRuleData().getCodeMapper().getShiftPatternById(RuleHelper.getAllPatternsId(wbData.getRuleData().getCodeMapper()));
        sprVars.allShifts = wbData.getRuleData().getCodeMapper().getShiftById(RuleHelper.getAllShiftsId(wbData.getRuleData().getCodeMapper()) );
        sprVars.methodLaborMetric = parameters.getParameter(PARAM_PREMIUM_LABOR_METHOD,
            PremiumWithLabor.PARAM_PLM_VAL_DEFAULT_METHOD );
        if (PremiumWithLabor.PARAM_PLM_VAL_PRORATED_METHOD.equals(sprVars.methodLaborMetric)) {
            throw new RuleEngineException ("MethodLaborMetric "
                                           + PremiumWithLabor.PARAM_PLM_VAL_PRORATED_METHOD + " not supported");
        }
        sprVars.methodLaborValue = parameters.getParameter(PARAM_PREMIUM_LABOR_VALUE);

        PremiumWithLabor pwl = new PremiumWithLabor();
        List spzRecords = new ArrayList();
        if( "spzone_id".equalsIgnoreCase(sprVars.in_spzone_id) ) {
            spzRecords = wbData.getRuleData().getCodeMapper().getAllShiftPremiumZones();
        } else {
            int id = Integer.parseInt( sprVars.in_spzone_id );
            ShiftPremiumZoneData spz = wbData.getRuleData().getCodeMapper().getShiftPremiumZoneById(id);
            if( spz != null ) {
                spzRecords.add(spz);
            }
            validateSPZoneHTypes(spzRecords, wbData);
        }
        for (int i = 0, j = spzRecords.size(); i < j; i++) {
            int timeInZone = 0;
            ShiftPremiumZoneData spz = (ShiftPremiumZoneData) spzRecords.get(i);
            sprVars.in_min_minutes = (spz.getSpzoneMinMinutes() != 0) ? spz.getSpzoneMinMinutes()
                                                              : sprVars.in_min_minutes;
            sprVars.finalWrkdFldsUpdate = (StringHelper.isEmpty(spz.getSpzoneUpdateFlds()))
                                  ? (sprVars.in_wrkdFldsUpdate != null && sprVars.in_wrkdFldsUpdate.equalsIgnoreCase("true")) ? "wrkd_udf8,wrkd_udf9,wrkd_udf10" : null
                                  : spz.getSpzoneUpdateFlds();
            if (isEligible(spz, wbData, sprVars)) {
                log("Day is Eligible");
                double numrate = (
                        "Y".equals(spz.getSpzonePercentage())
                         ? spz.getSpzoneRate() / 100 * wbData.getRuleData().getEmployeeData().getEmpBaseRate()
                         : spz.getSpzoneRate()
                        );


                if ("true".equalsIgnoreCase(sprVars.applyHourTypeMultiplier)) {
                     numrate *= wbData.getRuleData().getCodeMapper().getHourTypeById(spz.getHtypeId()).getHtypeMultiple();
                }

                //if (ShiftPremiumZoneData.APPLY_TYPE_ZONE.equals(spz.getSpzoneApplyType())) {
                    // split the workdetails according to the emp schedule so calcMinute(...)
                    // won't add the whole workdetails that spans over the zone time.
                    RuleHelper.splitAt(wbData.getEmployeeScheduleData().getEmpskdActStartTime(), wbData.getRuleData().getWorkDetails());
                    RuleHelper.splitAt(wbData.getEmployeeScheduleData().getEmpskdActEndTime(), wbData.getRuleData().getWorkDetails());

                    boolean bolOtMultiply = (spz.getSpzoneOtMultiply() != null) ? spz.getSpzoneOtMultiply().equalsIgnoreCase("Y") : false;
                    for (int k = 0, l = wbData.getRuleData().getWorkDetailCount(); k < l; k++) {
                        WorkDetailData wd = wbData.getRuleData().getWorkDetail(k);
                        if (RuleHelper.isCodeInList(spz.getSpzoneInclTcodeNames(), wd.getWrkdTcodeName()) &&
                                RuleHelper.isCodeInList(spz.getSpzoneInclHtypeNames(), wd.getWrkdHtypeName())) {
                            timeInZone += calcMinutes(pwl, wbData, spz, wd , bolOtMultiply, sprVars);
                            if (timeInZone > spz.getSpzoneMaxMinutes()) {
                                break;
                            }
                        }
                    }
                    if (timeInZone >= sprVars.in_min_minutes) {
                        timeInZone = Math.min(timeInZone, spz.getSpzoneMaxMinutes());
                        // *** Compare Generated Amount with MaxDollars for the Zone
                        if (timeInZone / 60 * numrate > spz.getSpzoneMaxDollars()) {
                            timeInZone = (int) (60 * spz.getSpzoneMaxDollars() / numrate);
                        }
                        timeInZone = (spz.getSpzoneUseMinutes() != 0) ? spz.getSpzoneUseMinutes()
                                                                      : timeInZone;
                        if (StringHelper.isEmpty(spz.getSpzoneBestOfGrp())) {
                            if (sprVars.finalWrkdFldsUpdate == null || sprVars.finalWrkdFldsUpdate.trim().length() == 0) {
                                // *** tt8075 ShiftPremiums adds empty premiums with period 0
                                if (timeInZone != 0) {
                                    log ("Adding premium for shift premium zone : " + spz.getSpzoneName() + " for " + timeInZone + " minutes");
                                    if (ShiftPremiumZoneData.APPLY_TYPE_FLAT_RATE.equals(spz.getSpzoneApplyType())
                                        && !sprVars.methodLaborMetric.equals(PremiumWithLabor.PARAM_PLM_VAL_FLAT_RATE_THRESHOLD_METHOD) ) {
                                        timeInZone = 60;
                                    }
                                    //wbData.insertWorkPremiumRecord(timeInZone, spz.getTcodeId(), spz.getHtypeId(), numrate);
                                    pwl.insertWorkPremiumRecords(sprVars.methodLaborMetric ,
                                         sprVars.methodLaborValue,
                                         wbData ,
                                         spz.getTcodeId(),
                                         spz.getHtypeId(),
                                         timeInZone,
                                         numrate,
                                         true, false);

                                }
                            } else {
                                log ("Affecting details for shift premium zone : " + spz.getSpzoneName());
                                applyToDetails(timeInZone, spz.getTcodeId(), spz.getHtypeId(), numrate, wbData, sprVars);
                            }
                        } else {
                            addCandidate(spz.getSpzoneBestOfGrp(),
                                     numrate,
                                     timeInZone,
                                     spz.getTcodeId(),
                                     spz.getHtypeId(), sprVars);
                        }
                    }
                // *** if flat rate, just insert a 1 hour premium with given rate,tcode and htype ***
            /*} else if (ShiftPremiumZoneData.APPLY_TYPE_FLAT_RATE.equals(spz.getSpzoneApplyType())) {
                if (numrate != 0) {
                    wbData.insertWorkPremiumRecordFlatRate(spz.getTcodeId() ,
                            spz.getHtypeId() ,
                            (numrate > spz.getSpzoneMaxDollars() ? spz.getSpzoneMaxDollars()
                                                                 : numrate)
                    );
                }
            }*/

            }
        }
        applyBestOfGroupEntries(wbData, sprVars);
    }

    private boolean isEligible(ShiftPremiumZoneData spz, WBData wbData, ShiftPremiumsWithLaborRule.ShiftPremiumsWithLaborRuleVariables sprVars) throws SQLException {
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
        ret &=  (sprVars.allCalcGroups != null && RuleHelper.isCodeInList(spz.getCalcgrpName(), sprVars.allCalcGroups.getCalcgrpName()))
                    || RuleHelper.isCodeInList(spz.getCalcgrpName(), wbData.getRuleData().getCodeMapper().getCalcGroupById(wbData.getRuleData().getWorkSummary().getCalcgrpId()).getCalcgrpName());
        if (!ret) {
            log ("Calc group check failed");
            return false;
        }
        // *** SHIFT PATTERNS ***
        ret &= (sprVars.allShiftPatterns != null && RuleHelper.isCodeInList(spz.getShftpatName(), sprVars.allShiftPatterns.getShftpatName()))
                    || RuleHelper.isCodeInList(spz.getShftpatName(), wbData.getRuleData().getCodeMapper().getShiftPatternById(wbData.getRuleData().getEmployeeData().getShftpatId()).getShftpatName());
        if (!ret) {
            log ("Shift Pattern check failed");
            return false;
        }
        // *** SHIFTS ***
        ret &= (sprVars.allShifts != null && RuleHelper.isCodeInList(spz.getShftName(), sprVars.allShifts.getShftName())) ||
                // *** Multiple Shifts ***
                RuleHelper.isCodeInList(spz.getShftName(), strShftName) ||
                  (sprVars.useMultipleShifts &&
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
                              double wrkpRate, int wrkpMinutes, int tcodeId, int htypeId, ShiftPremiumsWithLaborRule.ShiftPremiumsWithLaborRuleVariables sprVars) {
        double intMagicNumber;
        intMagicNumber = wrkpRate * wrkpMinutes;
        if (intMagicNumber == 0) {
           return;
        }

        if (sprVars.tabCandidates.containsKey(spzoneBestOfGroup)) {
            Candidate thisCandidate = (Candidate)sprVars.tabCandidates.get(spzoneBestOfGroup);
            if (intMagicNumber > thisCandidate.getMagicNumber()) {
                thisCandidate.setWrkpRate(wrkpRate);
                thisCandidate.setWrkpMinutes(wrkpMinutes);
                thisCandidate.setTcodeId(tcodeId);
                thisCandidate.setHtypeId(htypeId);
                thisCandidate.setMagicNumber(intMagicNumber);
                sprVars.tabCandidates.put(spzoneBestOfGroup , thisCandidate);
            }
        }
        else {
            Candidate newCandidate = new Candidate();
            newCandidate.setWrkpRate(wrkpRate);
            newCandidate.setWrkpMinutes(wrkpMinutes);
            newCandidate.setTcodeId(tcodeId);
            newCandidate.setHtypeId(htypeId);
            newCandidate.setMagicNumber(intMagicNumber);
            sprVars.tabCandidates.put(spzoneBestOfGroup , newCandidate);
        }
    }

    private void applyBestOfGroupEntries(WBData wbData, ShiftPremiumsWithLaborRule.ShiftPremiumsWithLaborRuleVariables sprVars) {

        Iterator it = sprVars.tabCandidates.entrySet().iterator();
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(sprVars.tabCandidates);}
        while (it.hasNext()) {
            Candidate oneCandidate =  (Candidate)(((Map.Entry)it.next()).getValue());
            if (oneCandidate.getWrkpMinutes() != 0) {
                  wbData.insertWorkPremiumRecord(oneCandidate.getWrkpMinutes() ,
                       oneCandidate.getTcodeId(),
                       oneCandidate.getHtypeId(),
                       oneCandidate.getWrkpRate());
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

    private void applyToDetails(int timeInZone, int tcodeId, int htypeId, double rate, WBData wbData, ShiftPremiumsWithLaborRule.ShiftPremiumsWithLaborRuleVariables sprVars) {
        int applyMinutes;
        String sFinalWrkdFldsUpdate = StringHelper.searchReplace(sprVars.finalWrkdFldsUpdate, " ", "").toLowerCase();
        boolean addPremium = sFinalWrkdFldsUpdate.indexOf("true") >= 0;
        String[] fields = StringHelper.detokenizeString(sFinalWrkdFldsUpdate , ",");
        if (fields.length < 3) {
            throw new RuntimeException ("Not enough fields in values spzone_update_fields in shift premium zone");
        }
        for (int i = 0, j = sprVars.tabWrkdDates.size(); i < j; i++) {
            WrkdDates wDates = (WrkdDates) sprVars.tabWrkdDates.get(i);
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
    private int calcMinutes(PremiumWithLabor pwl, WBData wbData, ShiftPremiumZoneData spz,
                            WorkDetailData wd, boolean bolOtMultiply, ShiftPremiumsWithLaborRule.ShiftPremiumsWithLaborRuleVariables sprVars) throws SQLException{
    	SimpleTimePeriod stp = new SimpleTimePeriod(spz.getSpzoneStartTime(),spz.getSpzoneEndTime());
    	TimePeriod newTP = TimePeriodHelper.getSetToDay(stp, wd.getWrkdStartTime());
    	long startZoneFraction = DateHelper.getDayFraction(newTP.retrieveStartDate());
    	long endZoneFraction = DateHelper.getDayFraction(newTP.retrieveEndDate());
        int minutes = 0;
        if (startZoneFraction == endZoneFraction) {
        	if (bolOtMultiply) {
        		minutes += (wd.getWrkdEndTime().getTime() - wd.getWrkdStartTime().getTime()) * wbData.getRuleData().getCodeMapper().getHourTypeById(wd.getHtypeId()).getHtypeMultiple() / DateHelper.MINUTE_MILLISECODS;
        	} else {
        		minutes += (wd.getWrkdEndTime().getTime() - wd.getWrkdStartTime().getTime()) / DateHelper.MINUTE_MILLISECODS;
        	}
        	addWrkdDates(pwl, wd.getWrkdStartTime(), wd.getWrkdEndTime(), minutes, sprVars, wd);
        } else {
            for (int i = -1; i < 2; i++) {
            	newTP = TimePeriodHelper.getSetToDay(stp, DateHelper.addDays(wd.getWrkdStartTime(), i));
            	Date diffStart = DateHelper.max(newTP.retrieveStartDate(), wd.getWrkdStartTime());
            	Date diffEnd = DateHelper.min(newTP.retrieveEndDate(), wd.getWrkdEndTime());
                if (!StringHelper.isEmpty(spz.getSpzoneSkdUnskd())
                    &&  !spz.getSpzoneSkdUnskd().equalsIgnoreCase("B")) {
                    if (spz.getSpzoneSkdUnskd().equalsIgnoreCase("S")) {
                        if (!(DateHelper.isBetween(wd.getWrkdStartTime() , wbData.getEmployeeScheduleData().getEmpskdActStartTime() , wbData.getEmployeeScheduleData().getEmpskdActEndTime()))
                            || (!DateHelper.isBetween(wd.getWrkdEndTime() , wbData.getEmployeeScheduleData().getEmpskdActStartTime() , wbData.getEmployeeScheduleData().getEmpskdActEndTime()))) {
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
                	addWrkdDates(pwl,
                			diffStart,
                			diffEnd,
                			(int) ((diffEnd.getTime() - diffStart.getTime()) / DateHelper.MINUTE_MILLISECODS),
                            sprVars,
                            wd);
                }
            }
        }
        return minutes;
    }

    private void addWrkdDates(PremiumWithLabor pwl,Date startDate, Date endDate,
                              int minutes,
                              ShiftPremiumsWithLaborRule.ShiftPremiumsWithLaborRuleVariables sprVars,
                              WorkDetailData wd) {
        addWrkdDates(pwl, startDate.getTime(), endDate.getTime(), minutes, sprVars, wd);
    }

    private void addWrkdDates(PremiumWithLabor pwl,
                              long startDate, long endDate,
                              int minutes,
                              ShiftPremiumsWithLaborRule.ShiftPremiumsWithLaborRuleVariables sprVars,
                              WorkDetailData wd) {
        WrkdDates wDates = new WrkdDates();
        wDates.setStartDate(startDate);
        wDates.setEndDate(endDate);
        wDates.setMinutes(minutes);
        sprVars.tabWrkdDates.add(wDates);

        pwl.addWrkdDates(
                new Date(startDate),
                new Date(endDate),
                minutes,
                wd
                );
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
            ShiftPremiumsWithLaborRule aRule = new ShiftPremiumsWithLaborRule();
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("\n\n\n\n\n\n\n\n\n\n\n\n\n\n");}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("BEFORE ShiftPremiumsWithLaborRule ... ");}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Work Details :");}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(ruleData.getWorkDetails().toDescription());}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Work Premiums :");}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(ruleData.getWorkPremiums().toDescription());}

            aRule.execute(new WBData(ruleData, c), parameters);
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("AFTER  ShiftPremiumsWithLaborRule ....");}
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

        public Candidate() {
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

    class ShiftPremiumsWithLaborRuleVariables {
        int in_wrks_id;
        int in_min_minutes;
        String in_spzone_id;
        String in_wrkdFldsUpdate;
        String finalWrkdFldsUpdate;
        String applyToAllDayShifts;
        boolean useMultipleShifts = false;
        Vector tabWrkdDates;
        String applyHourTypeMultiplier;

        CalcGroupData allCalcGroups = null;
        ShiftPatternData allShiftPatterns = null;
        ShiftData allShifts = null;
        Map tabCandidates = null;
        String methodLaborMetric = null;
        String methodLaborValue = null;
    }

    public boolean conditionSetExecutionIsMutuallyExclusive() {
      return false;
    }

}