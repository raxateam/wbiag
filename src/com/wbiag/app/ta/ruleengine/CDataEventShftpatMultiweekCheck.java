package com.wbiag.app.ta.ruleengine;

import java.sql.*;
import java.text.*;
import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
/**
 * Custom event for CDataEventShftpatMultiweekCheck
 */
public class CDataEventShftpatMultiweekCheck extends DataEvent {

    private static Logger logger = org.apache.log4j.Logger.getLogger(CDataEventShftpatMultiweekCheck.class);

    private static final MessageFormat multiWeekMsg =
        new MessageFormat("Override start date falls into week {0} of shift pattern, please review");
    private static final MessageFormat shftpatNotFound =
        new MessageFormat("Shift pattern : {0} not found");

    public void beforeEmployeeOverrideInsert(OverrideData od, DBConnection c)
        throws RuleEngineException {
        try {
            doShftPatMultiWeekCheck(od, c);
        }
        catch (SQLException ex) {
            if (logger.isEnabledFor(Level.ERROR)) logger.error("Error in beforeEmployeeOverrideInsert" , ex);
            throw new RuleEngineException(ex.getMessage());
        }
    }

    protected void doShftPatMultiWeekCheck(OverrideData od, DBConnection c)
        throws SQLException , RuleEngineException{
        OverrideData.OverrideToken ot = od.getNewOverrideByName(EmployeeData.EMP_SHFTPAT_NAME);
        if (ot != null) {
            String shftpatName = ot.getValue();
            CodeMapper cm = CodeMapper.createCodeMapper(c);
            ShiftPatternData spd = cm.getShiftPatternByName(shftpatName);
            if (spd != null) {
                ShiftPatternShiftsAccess spsa = new ShiftPatternShiftsAccess(c);
                List spsList = spsa.loadByShftPatId(spd.getShftpatId());
                if (spsList.size() > 7) {
                    EmployeeData ed = new EmployeeAccess(c , cm).load(od.getEmpId() ,
                        od.getOvrStartDate());
                    ShiftPatternShiftsData spsd =
                        spsa.loadByShiftPatDatOffset(spd.getShftpatId(),
                        od.getOvrStartDate(),
                        ed.getEmpShftpatOffset());
                    if (spsd.getShftpatshftDay() > 7) {
                        int week = (int)Math.ceil((double)spsd.getShftpatshftDay()/(double)7);
                        String msg = multiWeekMsg.format(new String[] {String.valueOf(week)});
                        throw new RuleEngineException(msg);
                    }
                }
                else {
                    if (logger.isDebugEnabled()) logger.debug("Shift pattern : " + shftpatName + " is 7 days");
                }
            }
            else {
                throw new RuleEngineException(shftpatNotFound.format(new String[] {shftpatName}));
            }
        }
    }
}
