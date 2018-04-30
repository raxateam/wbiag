package com.wbiag.app.ta.ruleengine;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.server.registry.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * Custom event for CDataEventShftpatDayStartTime
 * Returns custom day start time based on shift pattern udf overriding employee day start time setting
 */
public class CDataEventShftpatDayStartTime extends DataEvent {

    private static Logger logger = org.apache.log4j.Logger.getLogger(CDataEventShftpatDayStartTime.class);
    public static final String REG_DAY_START_TIME_SHFTPAT_UDF =
        "system/customer/DAY_START_TIME_SHFTPAT_UDF";
    public static final String SHFPAT_UDF_VAL_YESTERDAY = "YESTERDAY";
    public static final String SHFPAT_UDF_VAL_TODAY = "TODAY";
    public static final String SHFPAT_UDF_VAL_TOMORROW = "TOMORROW";

    public static final Date DATE_1200 = DateHelper.convertStringToDate("12:00", "HH:mm") ;
    public static final Date DATE_1201 = DateHelper.convertStringToDate("12:01", "HH:mm") ;
    public static final Date DATE_2359 = DateHelper.convertStringToDate("23:59", "HH:mm") ;
    public static final Date DATE_0000 = DateHelper.convertStringToDate("00:00", "HH:mm") ;
    public static final Date DATE_1159 = DateHelper.convertStringToDate("11:59", "HH:mm") ;

    public static final String ERR_YESTERDAY = "Shift start time must be between 12:01 and 23:59 to apply YESTERDAY setting";
    public static final String ERR_TOMORROW = "Shift start time must be between 00:00 and 11:59  to apply TOMORROW setting";

    private static final List eligibleUdfs = new ArrayList();
    static {
        eligibleUdfs.add("SHFTPATSHFT_UDF1");
        eligibleUdfs.add("SHFTPATSHFT_UDF2");
        eligibleUdfs.add("SHFTPATSHFT_UDF3");
        eligibleUdfs.add("SHFTPATSHFT_UDF4");
        eligibleUdfs.add("SHFTPATSHFT_UDF5");
    }

    public java.util.Date getDayStartTime(EmployeeData ed,
                                          java.util.Date workDate,
                                          DBConnection conn) throws Exception {
        return processSpsdDayStartTime(ed, workDate , conn);
    }

    protected Date processSpsdDayStartTime(EmployeeData ed,
                                          java.util.Date workDate,
                                          DBConnection conn)  throws Exception {
        Date ret = null;
        CodeMapper cm = CodeMapper.createCodeMapper(conn);
        ShiftPatternShiftsAccess spsa = new ShiftPatternShiftsAccess(conn);
        ShiftPatternShiftsData spsd = spsa.loadByShiftPatDatOffset(
               ed.getShftpatId(),
               workDate,
               ed.getEmpShftpatOffset());
        String shftpatUdf = getShftpatshftUdf();
        String udfVal = (String)spsd.getField(shftpatUdf);
        if (!StringHelper.isEmpty(udfVal)) {
            if (SHFPAT_UDF_VAL_YESTERDAY.equals(udfVal)) {
                ShiftData sd = cm.getShiftById(spsd.getShftId()) ;
                if (sd != null) {
                    if (!(DateHelper.getDayFraction(sd.getShftStartTime())
                        >= DateHelper.getDayFraction(DATE_1201)
                        && DateHelper.getDayFraction(sd.getShftStartTime())
                        <= DateHelper.getDayFraction(DATE_2359))) {
                      if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(ERR_YESTERDAY);}
                      throw new RuntimeException(ERR_YESTERDAY);
                    }
                    ret = DATE_1201;
                }
            }
            else if (SHFPAT_UDF_VAL_TODAY.equals(udfVal)) {
                ret = DateHelper.getCurrentDate();
            }
            else if (SHFPAT_UDF_VAL_TOMORROW.equals(udfVal)) {
                ShiftData sd = cm.getShiftById(spsd.getShftId()) ;
                if (sd != null) {
                    if (!(DateHelper.getDayFraction(sd.getShftStartTime())
                        >= DateHelper.getDayFraction(DATE_0000)
                        && DateHelper.getDayFraction(sd.getShftStartTime())
                        <= DateHelper.getDayFraction(DATE_1159))) {
                      if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(ERR_TOMORROW);}
                      throw new RuntimeException(ERR_TOMORROW);
                    }
                    ret = DATE_1200;
                }

            }
            // *** check HH:mm
            else {
                try {
                    ret = DateHelper.convertStringToDate(spsd.
                        getShftpatshftUdf1(),  "HH:mm");
                }
                catch (Exception ex) {
                    if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in formatting shift pattern udf :"   + spsd.getShftpatshftUdf1(), ex);}
                    throw new NestedRuntimeException(
                        "Error in formatting shift pattern (" +
                        ed.getEmpShftpatName() + ") udf (" +
                        spsd.getShftpatshftUdf1() + ") for dayStartTime", ex);
                }
            }
        }
        else {
            // *** check shift pattern, employee , global
            ShiftPatternData spd = cm.getShiftPatternById(ed.getShftpatId());
            if (spd!=null && spd.getShftpatDayStartTime() != null) {
                ret = spd.getShftpatDayStartTime();
            }
            else {
                Date d = ed.getEmpDayStartTime();
                if (d != null) {
                    ret = d;
                }
                else {
                    String dayStartTimeString = com.workbrain.server.
                        WorkbrainParametersRetriever.getString("DAY_START_TIME",
                        "00:00");
                    ret = DateHelper.convertStringToDate(dayStartTimeString,
                        "HH:mm");
                }
            }

        }
        if (logger.isDebugEnabled()) logger.debug("Setting day start time in data event to :" + DateHelper.convertDateString(ret, "HH:mm"));
        if (ret == null) {
            throw new RuntimeException("Day start time could not be found for employee.emp_name :" + ed.getEmpName() + " at date:" + workDate);
        }
        return ret;
    }

    private String getShftpatshftUdf() {
        String reg = Registry.getVarString(REG_DAY_START_TIME_SHFTPAT_UDF , "");
        if (!eligibleUdfs.contains(reg.toUpperCase())) {
            throw new RuntimeException ("DAY_START_TIME_SHFTPAT_UDF registry must be shftpatshft_udf[1..5]");
        }

        return reg;
    }
}
