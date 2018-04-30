package com.wbiag.app.modules.availability;

import com.workbrain.app.ta.db.*;
import com.workbrain.sql.*;
import com.workbrain.tool.locale.DataLocUtil;
import com.workbrain.util.*;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import com.workbrain.app.modules.availability.model.*;
import com.workbrain.app.modules.availability.*;

import java.sql.SQLException;
import java.util.*;

/**
 * @deprecated As of 5.0, use core Make Availability Reportable Task.
 */
public class WbiagAvailabilityAccess extends RecordAccess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WbiagAvailabilityAccess.class);

    public final static String WBIAG_AVAL_SUMMARY_TABLE  = "WBIAG_AVAL_SUMMARY";
    public final static String WBIAG_AVAL_SUMMARY_SEQ    = "SEQ_WAVS_ID";
    public final static String WBIAG_AVAL_SUMMARY_PRIKEY = "WAVS_ID";
    public final static String WBIAG_AVAL_DETAIL_TABLE  = "WBIAG_AVAL_DETAIL";
    public final static String WBIAG_AVAL_DETAIL_SEQ    = "SEQ_WAVD_ID";
    public final static String WBIAG_AVAL_DETAIL_PRIKEY = "WAVD_ID";

    public WbiagAvailabilityAccess(DBConnection conn) {
        super(conn);
    }

    /**
     * Delete summaries for given empids and date range
     * @param empIds
     * @param startDate
     * @param endDate
     * @throws SQLException
     */
    public void deleteSummaries(int[] empIds, Date startDate, Date endDate) throws SQLException{
        IntArrayIterator iter = new IntArrayIterator(empIds, 100);
        while (iter.hasNext()) {
            int[] empIdsThis = iter.next();
            PreparedStatement ps = null;
            try {
                StringBuffer sb = new StringBuffer(200);
                sb.append("DELETE FROM wbiag_aval_summary WHERE emp_id IN (");
                for (int i = 0; i < empIdsThis.length; i++) {
                    sb.append(i > 0 ? ",?" : "?");
                }
                sb.append(")");
                sb.append(" AND wavs_date BETWEEN ? and ?");
                int cnt = 1;
                ps = getDBConnection().prepareStatement(sb.toString());
                for (int i = 0; i < empIdsThis.length; i++) {
                    ps.setInt(cnt++, empIdsThis[i]);
                }
                ps.setTimestamp(cnt++ , new Timestamp(startDate.getTime()));
                ps.setTimestamp(cnt++ , new Timestamp(endDate.getTime()));
                int upd = ps.executeUpdate();
            }
            finally {
                if (ps != null) ps.close();
            }

        }
    }

    /**
     * Loads summary by empid and date
     * @param empId
     * @return
     * @throws SQLException
     */
    public WbiagAvalSummaryData loadSummaryByEmpIdDate( int empId, Date date ) throws SQLException {
        List list = this.loadRecordData(
                new WbiagAvalSummaryData(),
                WBIAG_AVAL_SUMMARY_TABLE,
                "emp_id",  empId,
                "wavs_date",  date
        );
        if( list.size() > 0 ) {
            return (WbiagAvalSummaryData) list.get( 0 );
        } else {
            return null;
        }
    }

    /**
     * Loads details by summary id
     * @param wavs_id
     * @return
     * @throws SQLException
     */
    public List loadDetailwBySummaryId( int wavs_id ) throws SQLException {
        return loadRecordData(
            new WbiagAvalDetailData(),
            WBIAG_AVAL_DETAIL_TABLE,
            "wavs_id",
            wavs_id);

    }


}

