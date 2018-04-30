package com.wbiag.app.ta.ruleengine;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.server.registry.*;
/**
 * Custom event for CDataEventLTABreakCheck
 */
public class CDataEventLTABreakCheck extends DataEvent {

    //private static Logger logger = org.apache.log4j.Logger.getLogger(CDataEventLTABreakCheck.class);

    public static final String REG_LTA_PRESERVE_BREAKS = "/system/ruleEngine/overrides/LTA_PRESERVE_BREAKS";
    public static final String REG_LTA_BRK_TIMECODES = "/system/ruleEngine/overrides/LTA_BRK_TIMECODES";

    /**
     * This process registry breaks if defined.
     * This is due to 45841 Schedule optimizer must create breaks with EmployeeScheduleDetailData.setShiftBreak(boolean)
     *  and can be removed when fix is there
     * @param data
     * @param ol
     * @param c
     */
    public void beforeLongTermAbsenceOverrides(WBData data, OverrideList ol, DBConnection c) {
        doLTABreakCheck(data, ol, c);
    }

    /**
     * This massages each override if preserve breaks is true
     * @param data
     * @param od
     * @param c
     */
    public void beforeOneLongTermAbsenceOverride(WBData data, OverrideData od,
                                                 DBConnection c) {
        doLTABreakCheck(data, od, c);
    }

    protected void doLTABreakCheck(WBData data, OverrideList ol,
                                   DBConnection c) {
        String brkTcodes = getShiftBreakTcodes(data);
        if (StringHelper.isEmpty(brkTcodes)) {
            brkTcodes = Registry.getVarString(REG_LTA_BRK_TIMECODES, null) ;
            if (!StringHelper.isEmpty(brkTcodes)) {
                processRegShiftBreaks(data, brkTcodes);
            }
        }
    }

    protected void doLTABreakCheck(WBData data, OverrideData od,
                                   DBConnection c) {
        String brkTcodes = getShiftBreakTcodes(data);
        if (StringHelper.isEmpty(brkTcodes)) {
            brkTcodes = Registry.getVarString(REG_LTA_BRK_TIMECODES, null) ;
        }
        boolean shouldCheck = !StringHelper.isEmpty(brkTcodes);
        if (shouldCheck) {
            boolean preservesBrks = Registry.getVarBoolean(REG_LTA_PRESERVE_BREAKS, false) ;
            // *** preserves BRK code, default false
            if (preservesBrks) {
                String sExcl = OverrideData.formatToken(OverrideData.
                    WRKD_ELIGIBLE_TCODELIST, brkTcodes);
                // *** remove last comma
                sExcl = sExcl.substring(0, sExcl.length() - 1);
                
                // TT 47756
                // If the day gets calculated more than once, ensure
                // that we do not append the break code again.  It will
                // already be there from the first calc and therefore cause the ovr to error.
                if (od.getOvrNewValue().indexOf(sExcl) < 0) {
                	od.setOvrNewValue(od.getOvrNewValue() + "," + sExcl);
                }
            }
        }
    }

    /**
     * Adds breaks to ruleData collections if not already added
     * @param data
     * @param brkTcodes
     */
    protected void processRegShiftBreaks(WBData data , String brkTcodes) {
        WorkDetailList wdlist = data.getRuleData().getWorkDetails();
        for (int i=0 , k=wdlist.size() ; i<k ; i++) {
            WorkDetailData wd = wdlist.getWorkDetail(i);
            if (RuleHelper.isCodeInList(brkTcodes , wd.getWrkdTcodeName())) {
                int shiftInd = findShiftIndex(data, wd);
                // *** not in a shift
                if (shiftInd == -1) {
                    continue;
                }
                ShiftWithBreaks swb = data.getRuleData().getShiftWithBreaks(shiftInd);
                List brks = swb.getShiftBreaks();
                ShiftBreakData sb = new ShiftBreakData();
                sb.setShftbrkStartTime(wd.getWrkdStartTime());
                sb.setShftbrkEndTime(wd.getWrkdEndTime());
                sb.setShftbrkMinutes(wd.getWrkdMinutes());
                sb.setTcodeId(wd.getTcodeId());
                sb.setHtypeId(wd.getHtypeId());
                if (!existsBreak(brks, sb)) {
                    brks.add(sb);
                    swb.setShiftBreaks(brks);
                }
            }
        }
    }

    private int findShiftIndex(WBData data , WorkDetailData wd) {
        int ret = -1;
        List swbs = data.getShiftsWithBreaks();
        for (int i=0; i < swbs.size() ; i++) {
            ShiftWithBreaks swb = (ShiftWithBreaks)swbs.get(i);
            if (DateHelper.isBetween(wd.getWrkdStartTime() , swb.getShftStartTime(), swb.getShftEndTime())
                && DateHelper.isBetween(wd.getWrkdEndTime() , swb.getShftStartTime(), swb.getShftEndTime())) {
                ret = i;
                break;
            }
        }
        return ret;
    }

    private boolean existsBreak(List brks, ShiftBreakData sb) {
        boolean ret = false;
        if (brks == null || brks.size() == 0) {
            return false;
        }
        Iterator iter = brks.iterator();
        while (iter.hasNext()) {
            ShiftBreakData item = (ShiftBreakData)iter.next();
            if (DateHelper.equals(item.getShftbrkStartTime() , sb.getShftbrkStartTime())
                && DateHelper.equals(item.getShftbrkStartTime() , sb.getShftbrkStartTime())
                && item.getTcodeId() == sb.getTcodeId()) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    protected String getShiftBreakTcodes(WBData data) {
        StringBuffer sb = new StringBuffer(200);
        List shiftsWithBreaks = data.getShiftsWithBreaks();
        for (int k=0; k < shiftsWithBreaks.size() ; k++) {
            ShiftWithBreaks shiftWithBreaks = (ShiftWithBreaks) shiftsWithBreaks.get(k);
            List shiftBreaks = shiftWithBreaks.getShiftBreaks();
            for (int l=0 , m=shiftBreaks.size() ; l<m ; l++) {
                ShiftBreakData item = (ShiftBreakData)shiftBreaks.get(l);
                if (l >0) sb.append(",");
                sb.append(data.getCodeMapper().getTimeCodeById(item.getTcodeId()).getTcodeName());
            }
        }
        return sb.toString();
    }

}

