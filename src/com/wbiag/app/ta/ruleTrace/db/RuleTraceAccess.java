package com.wbiag.app.ta.ruleTrace.db;

import java.io.*;
import java.sql.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.sql.*;
/**
 *  @deprecated Core as of 5.0.3.0
 */

public class RuleTraceAccess extends RecordAccess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(RuleTraceAccess.class);

    public static final String RULE_TRACE_TABLE = "wbiag_rule_trace";
    public static final String RULE_TRACE_PRI_KEY = "wrt_id";
    public static final String RULE_TRACE_PRI_KEY_SEQ = "seq_wrt_id";


    /** Constructor for RuleTraceAccess
     *
     * @param c DBConnection
     */
    public RuleTraceAccess(DBConnection c) {
        super(c);
    }

    public void save(int wrksId, String trace) throws SQLException, IOException {
        deleteByWrksId(wrksId);
        insertTrace(wrksId ,trace);
    }


    private int insertTrace(int wrksId, String trace) throws SQLException, IOException {
        int wrtId = getDBConnection().getDBSequence(
            RULE_TRACE_PRI_KEY_SEQ).getNextValue();

        String sql =
            "insert into wbiag_rule_trace(wrt_id, wrks_id, wrt_trace) values(?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = getDBConnection().prepareStatement(sql);
            ps.setInt(1, wrtId);
            ps.setInt(2, wrksId);
            ps.setNull(3, Types.CLOB);
            ps.executeUpdate();

            updateTrace(wrtId, trace);
        }
        finally {
            if (ps != null)
                ps.close();
        }

        return wrtId;
    }


    public void updateTrace(int wrtId, String xml) throws SQLException, IOException {
        if (xml != null) {
            getDBConnection().updateClob(xml, RULE_TRACE_TABLE,
                                         "wrt_trace",
                                         RULE_TRACE_PRI_KEY,
                                         String.valueOf(wrtId));
        }
    }

    public void deleteByWrksId(int wrksId) throws SQLException {
        deleteRecordData(RULE_TRACE_TABLE, "wrks_id", wrksId);
    }

    public String getTraceByWrksId(int wrksId) throws Exception{
      StringBuffer xml = new StringBuffer(200);
      PreparedStatement ps = null;
      ResultSet rs = null;
      try {
          ps = getDBConnection().prepareStatement("SELECT wrt_trace FROM wbiag_rule_trace WHERE wrks_id = ?");
          ps.setInt(1, wrksId);

          rs = ps.executeQuery();
          while (rs.next()) {
              Clob clob = rs.getClob(1);
              int clobLength = (int) clob.length();
              int cnt = clobLength / 100;
              String clobS = clob.getSubString(1L, clobLength);
              for (int i = 0; i <= cnt; i++) {
                  xml.append(clobS.substring(i* 100 , Math.min(i*100 + 100 ,clobLength)) );
              }

          }
      }
      finally {
          SQLHelper.cleanUp(ps, rs);
      }
      return xml.toString();
    }


}