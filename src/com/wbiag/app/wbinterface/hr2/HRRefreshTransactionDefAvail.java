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
import com.workbrain.server.registry.*;
import com.workbrain.app.modules.availability.*;
import com.workbrain.app.modules.availability.model.*;
import com.workbrain.app.modules.availability.db.*;

/**
 * Customization for default AMX_AVAILABILITY
 *
 **/
public class HRRefreshTransactionDefAvail extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionDefAvail.class);

    public static final String REG_AMX_AMX_DEF_AVL = "system/modules/availability_mgmt/AMX_DEF_AVL";
    public static final String REG_AMX_LEN = "system/WORKBRAIN_PARAMETERS/AMX_LEN";
    public static final String REG_VAL_ALL_OFF = "ALL_OFF";
    public static final String REG_VAL_ALL_ON = "ALL_ON";
    public static final String REG_VAL_DAYS_START = "DAYS=";

    public static final int DEFAULT_MATRIX_LENGTH = 7;

    public static final String TIME_PREFIX_START = "amxavStime";
    public static final String TIME_PREFIX_END = "amxavEtime";
    public static final String MIDNIGHT_PREFIX_START  = "amxavMid";

    public static final String DAYS_ATTR_SEP = ",";
    public static final String DAYS_SEP = "~";
    public static final String DAY_OFF = "OFF";
    public static final String TIME_SHORT_FMT = "HH";
    public static final String TIME_LONG_FMT = "HHmm";

    public static final String ERR_PARSE = "Error parsing AMX_DEF_AVL entry when creating default availability";

    private DBConnection conn = null;


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
        this.conn = conn;
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            processDefaultAvail(data, conn);
        }
    }

    public void processDefaultAvail(HRRefreshTransactionData data,
                               DBConnection c) throws SQLException, WBInterfaceException {
        String regDef = Registry.getVarString(REG_AMX_AMX_DEF_AVL , REG_VAL_ALL_OFF);
        if (StringHelper.isEmpty(regDef) || REG_VAL_ALL_OFF.equals(regDef)) {
            if (logger.isDebugEnabled()) logger.debug("AMX_DEF_AVL was empty or ALL_OFF, no default availability created");
            return;
        }
        int availDays = Registry.getVarInt(REG_AMX_LEN , DEFAULT_MATRIX_LENGTH);
        boolean existsDefData = false;
        if (!data.isNewEmployee()) {
            existsDefData = getAvailabilityData(c , data.getEmpId()) != null;
        }
        if (existsDefData) {
            if (logger.isDebugEnabled()) logger.debug("Employee already has default availability data");
            return;
        }
        if (REG_VAL_ALL_ON.equals(regDef)) {
            processAllOn(data , c , availDays);
        }
        else if (regDef.startsWith(REG_VAL_DAYS_START)) {
            String dayString = regDef.substring(REG_VAL_DAYS_START.length());
            processDays(data , c , availDays , dayString);
        }
        else {
            data.appendWarning("AMX_DEF_AVL was not ALL_OFF,ALL_ON or DAYS, no default availability created");
        }
    }

    protected void processAllOn(HRRefreshTransactionData data,
                                DBConnection c,
                                int availDays) throws SQLException{
       AvailabilityData ad = new AvailabilityData();
       ad.setEmpId(data.getEmpId());
       ad.setAmxavStartDate(AvailabilityData.DEF_START_DATE);
       ad.setAmxavEndDate(AvailabilityData.DEF_END_DATE);
       for (int i = 1 ; i <= availDays ; i++ ) {
           ad.setProperty(TIME_PREFIX_START + i ,
                          AvailabilityData.DEF_START_DATE);
           ad.setProperty(TIME_PREFIX_END + i ,
                          AvailabilityData.DEF_START_DATE);
       }
       new AvailabilityAccess(c , CodeMapper.createCodeMapper(c)).insert(ad);
    }

    protected void processDays(HRRefreshTransactionData data,
                               DBConnection c,
                               int availDays,
                               String dayString ) throws SQLException{
        List allDays = null;
        try {
            allDays = parseDaysString(dayString, availDays);
        }
        catch (Exception ex) {
            data.error(ERR_PARSE + ". Error : " + ex.getMessage());
        }
        if (data.isError() || allDays == null) {
            if (logger.isDebugEnabled()) logger.debug("AMX_DEF_AVL setting has errors or is empty");
            return;
        }
        AvailabilityData ad = new AvailabilityData();
        ad.setEmpId(data.getEmpId());
        ad.setAmxavStartDate(AvailabilityData.DEF_START_DATE);
        ad.setAmxavEndDate(AvailabilityData.DEF_END_DATE);

        Iterator iter = allDays.iterator();
        while (iter.hasNext()) {
            Day item = (Day)iter.next();
            ad.setProperty(TIME_PREFIX_START + item.day ,
                           item.start);
            ad.setProperty(TIME_PREFIX_END + item.day ,
                           item.end);
            if (!StringHelper.isEmpty(item.midnight)) {
               ad.setProperty(MIDNIGHT_PREFIX_START + item.day ,
                              item.midnight);
            }
        }
        new AvailabilityAccess(c , CodeMapper.createCodeMapper(c)).insert(ad);
    }

    private List parseDaysString(String daysString, int availDays) {
        if (StringHelper.isEmpty(daysString)) {
            throw new RuntimeException("AMX_DEF_AVL cannot be empty");
        }
        String[] days = StringHelper.detokenizeString(daysString , DAYS_SEP , true);
        if (days.length != availDays) {
            throw new RuntimeException(availDays + " days expected in AMX_DEF_AVL");
        }
        List allDays = new ArrayList();
        for (int i = 0; i < days.length; i++) {
            String[] dayAttrs = StringHelper.detokenizeString(days[i]  , DAYS_ATTR_SEP , true);
            if (!(dayAttrs.length == 3 || dayAttrs.length == 4)) {
                throw new RuntimeException("3 or 4 attributes expected for day");
            }
            Day oneDay = new Day();
            oneDay.day = Integer.parseInt(dayAttrs[0]);
            if (oneDay.day < 1 || oneDay.day > availDays) {
                throw new RuntimeException("Day attribute must be between 1 and " + availDays);
            }
            if (!DAY_OFF.equals(dayAttrs[1]) && !StringHelper.isEmpty(dayAttrs[1])) {
                String formatS = dayAttrs[1].length() == 2 ? TIME_SHORT_FMT :
                    TIME_LONG_FMT;
                oneDay.start = DateHelper.parseDate(dayAttrs[1], formatS);
            }
            if (!DAY_OFF.equals(dayAttrs[2]) && !StringHelper.isEmpty(dayAttrs[2])) {
                String formatE = dayAttrs[2].length() == 2 ? TIME_SHORT_FMT :
                    TIME_LONG_FMT;
                oneDay.end = DateHelper.parseDate(dayAttrs[2], formatE);
            }
            if (dayAttrs.length == 4) {
                oneDay.midnight = String.valueOf(dayAttrs[3].charAt(0));
            }
            allDays.add(oneDay);
        }
        return allDays;
    }

    private class Day {
        int day;
        Date start;
        Date end;
        String midnight;
    }

    private AvailabilityData getAvailabilityData( DBConnection c, int empId) throws SQLException{
       List l = new RecordAccess(c).
           loadRecordData(new AvailabilityData() , AvailabilityData.EMPLOYEE_AVAILABILITY_TABLE, "emp_id", empId);
       if (l.size() > 0) {
           return (AvailabilityData)l.get(0);
       }
       return null;
    }

}