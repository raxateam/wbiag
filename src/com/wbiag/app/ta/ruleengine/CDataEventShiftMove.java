package com.wbiag.app.ta.ruleengine;


import org.apache.log4j.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import java.sql.SQLException;
import java.util.Date;
/**
 * Converts shift override with yesterday token to EMPLOYEE_SCHEDULED_TIMES override temporarily
 */
public class CDataEventShiftMove extends DataEvent {

    private static Logger logger = org.apache.log4j.Logger.getLogger(CDataEventShiftMove.class);

    public static final String TOKEN_EMPSKDACT_YESTERDAY = "EMPSKD_ACT_YESTERDAY";

    public void beforeOneEmployeeScheduleOverride(OverrideData od, DBConnection c,
                                                  EmployeeData empData,
                                                  EmployeeScheduleData empSchedData)   {
        OverrideData.OverrideToken otS = od.getNewOverrideByName(
            EmployeeScheduleData.EMPSKD_ACT_SHIFT_NAME) ;
        String shftName = otS.getValue();
        if (StringHelper.isEmpty(shftName)) {
            throw new RuntimeException ("Shift was not supplied");
        }
        OverrideData.OverrideToken otY = od.getNewOverrideByName(TOKEN_EMPSKDACT_YESTERDAY) ;
        boolean isYest = otY != null && "Y".equals(otY.getValue() );
        od.setOvrUdf1(shftName);
        od.setOvrUdf2(otY.getValue());
        od.removeTokenName(EmployeeScheduleData.EMPSKD_ACT_SHIFT_NAME);
        od.removeTokenName(TOKEN_EMPSKDACT_YESTERDAY);
        CodeMapper cm = null;
        try {
            cm = CodeMapper.createCodeMapper(c);
        }
        catch (SQLException ex) {
            throw new NestedRuntimeException("Could not create codemapper", ex);
        }
        ShiftData sd = cm.getShiftByName(shftName);
        Date dat = isYest ? (DateHelper.addDays(od.getOvrStartDate() , -1))
            :  od.getOvrStartDate();
        Date st = DateHelper.setTimeValues(dat , sd.getShftStartTime() );
        Date end = DateHelper.setTimeValues(dat , sd.getShftEndTime() );
        StringBuffer sb = new StringBuffer(200);
        sb.append(od.formatToken(EmployeeScheduleData.EMPSKD_ACT_START_TIME,
                                 DateHelper.convertDateString(st , OverrideData.OVERRIDE_TIME_FORMAT_STR))  ); ;
        sb.append(od.formatToken(EmployeeScheduleData.EMPSKD_ACT_END_TIME,
                                 DateHelper.convertDateString(end , OverrideData.OVERRIDE_TIME_FORMAT_STR))  ) ;
        if (logger.isDebugEnabled()) logger.debug("Temporarily setting ovrnewvalue to:" + sb.toString() );

        od.setOvrNewValue(sb.toString());

    }

    public void afterOneEmployeeScheduleOverride(OverrideData od, DBConnection c,
                                                  EmployeeData empData,
                                                  EmployeeScheduleData empSchedData)   {

        if (!StringHelper.isEmpty(od.getOvrUdf1())) {
            StringBuffer sb = new StringBuffer(200);
            sb.append(od.getOvrNewValue());
            if (sb.length() > 0) {
                sb.append(OverrideData.OVR_DELIM);
            }
            sb.append(OverrideData.formatToken( EmployeeScheduleData.EMPSKD_ACT_START_TIME ,od.getOvrUdf1()) );
            sb.append(OverrideData.formatToken( TOKEN_EMPSKDACT_YESTERDAY ,od.getOvrFlag1()) );
        }

    }




}
