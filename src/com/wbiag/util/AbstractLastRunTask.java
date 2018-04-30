package com.wbiag.util;

import java.sql.*;
import java.util.Date;

import com.workbrain.app.scheduler.enterprise.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.util.*;

/**
 * Abstract Task that remembers and updates last run datetime
 */
public abstract class AbstractLastRunTask extends  AbstractScheduledJob{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AbstractLastRunTask.class);

    public static final String UDF_DATE_FORMAT = "yyyyMMdd HHmmss";

    /**
     * Returns the last run date for given unique taskname.
     * It stores last run dates in wbint_type.wbityp_udf1 in UDF_DATE_FORMAT
     * and will create this record if not already created
     *
     * @param taskName unique task name
     * @param defaultDate default date if record is not found
     * @return last run date
     * @throws Exception
     */
    protected Date getLastRunDate(String taskName, Date defaultDate) throws Exception {
        Date ret = null;

        boolean typeExists = false;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT wbityp_id FROM wbint_type WHERE wbityp_name = ?";
            ps = getConnection().prepareStatement(sql);
            ps.setString(1 , taskName);
            rs = ps.executeQuery();
            if (rs.next()) {
                typeExists = true;
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

        if (!typeExists ) {
            PreparedStatement ps1 = null;
            try {
                String sql = "INSERT INTO wbint_type (wbityp_id, wbityp_name, wbityp_type, wbityp_del_older_than) VALUES (?,?,?,?)";
                ps1 = getConnection().prepareStatement(sql);
                ps1.setInt(1 , getConnection().getDBSequence("seq_wbityp_id").getNextValue());
                ps1.setString(2 , taskName);
                ps1.setString(3 , "IMPORT");
                ps1.setInt(4, Integer.MAX_VALUE);
                int upd = ps1.executeUpdate();
            }
            finally {
                if (ps1 != null) ps1.close();
            }
            ret = defaultDate;

        }
        else {
            PreparedStatement ps2 = null;
            ResultSet rs2 = null;
            try {
                String sql = "SELECT wbityp_udf1 FROM wbint_type WHERE wbityp_name = ?";
                ps2 = getConnection().prepareStatement(sql);
                ps2.setString(1 , taskName);
                rs2 = ps2.executeQuery();
                if (rs2.next()) {
                    String udfVal = rs2.getString(1);
                    if (!com.workbrain.util.StringHelper.isEmpty(udfVal)) {
                        ret = WBInterfaceUtil.parseDateTime(rs2.getString(1),
                            UDF_DATE_FORMAT,
                            "Error in getting last run date");
                    }
                    else {
                        ret = defaultDate;
                    }
                }
            }
            finally {
                if (rs2 != null) rs2.close();
                if (ps2 != null) ps2.close();
            }

        }
        return ret;
    }

    /**
     * Returns the last run date for given unique taskname.
     * It updates last run dates in wbint_type.wbityp_udf1 in UDF_DATE_FORMAT
     * @param taskDateTime last run time
     * @param taskName  unique task name
     * @throws Exception
     */
    protected void updateLastRunDate(Date taskDateTime, String taskName) throws Exception {
        int upd = 0;
        PreparedStatement ps = null;
        try {
            String sql = "UPDATE wbint_type SET wbityp_udf1=? WHERE wbityp_name = ?";
            ps = getConnection().prepareStatement(sql);
            ps.setString(1  , DateHelper.convertDateString(taskDateTime, UDF_DATE_FORMAT));
            ps.setString(2 , taskName);
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        if (upd == 0) {
            throw new RuntimeException ("Task name couldn't be updated");
        }
    }



}
