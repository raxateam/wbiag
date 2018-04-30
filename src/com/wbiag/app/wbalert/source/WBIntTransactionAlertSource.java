package com.wbiag.app.wbalert.source ;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.wbalert.*;
import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 *
 * Alert for wbint transaction and status
 */
public class WBIntTransactionAlertSource extends AbstractRowSource{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WBIntTransactionAlertSource.class);

    public static final String PARAM_WBINT_TYPE_NAMES = "WBINT_TYPE_NAMES";
    public static final String PARAM_WBITRAN_STATUS = "WBINT_STATUS";
    public static final String PARAM_LOOK_BACK_MINUTES = "LOOK_BACK_MINUTES";


    private RowDefinition rowDefinition;
    private DBConnection conn;

    private Date taskDateTime;
    private java.util.List rows = new ArrayList();

    public static final String COL_WBITYP_NAME= "WBITYP_NAME";
    public static final String COL_WBITRAN_FILENAME = "WBITRAN_FILENAME";
    public static final String COL_WBITRAN_STATUS = "WBITRAN_STATUS";
    public static final String COL_WBITRAN_MSG = "WBITRAN_MSG";
    public static final String COL_WBITRAN_START_DATE = "WBITRAN_START_DATE";
    public static final String COL_WBITRAN_END_DATE = "WBITRAN_END_DATE";

    {
        RowStructure rs = new RowStructure(20);
        rs.add(COL_WBITYP_NAME,CharType.get(100));
        rs.add(COL_WBITRAN_FILENAME,CharType.get(100));
        rs.add(COL_WBITRAN_STATUS,CharType.get(100));
        rs.add(COL_WBITRAN_MSG,CharType.get(100));
        rs.add(COL_WBITRAN_START_DATE,CharType.get(100));
        rs.add(COL_WBITRAN_END_DATE,CharType.get(100));
        rowDefinition = new RowDefinition(-1,rs);
    }

    /**
     *
     *@param  c                    Connection
     *@param  alertParams          alert parameters
     *@exception  AccessException  Description of Exception
     */
    public WBIntTransactionAlertSource(DBConnection c , HashMap alertParams) throws AccessException {
        this.conn = c;

        String lookBackMinutesS = (String)alertParams.get(PARAM_LOOK_BACK_MINUTES);
        int lookBackMinutes = 0;
        if (!StringHelper.isEmpty(lookBackMinutesS)) {
            try {
                lookBackMinutes = Integer.parseInt(lookBackMinutesS);
                lookBackMinutes = (lookBackMinutes < 0) ? 0 : lookBackMinutes;
            }
            catch (NumberFormatException ex) {
                throw new AccessException(PARAM_LOOK_BACK_MINUTES + " must be an Integer");
            }
        }
        String[] wbitypNames = StringHelper.detokenizeString((String)alertParams.get(PARAM_WBINT_TYPE_NAMES), ",", true);

        String[] wbitypStatus = StringHelper.detokenizeString((String)alertParams.get(PARAM_WBITRAN_STATUS) , ",", true);

        this.taskDateTime = (Date) alertParams.get(WBAlertProcess.TASK_PARAM_TASKDATETIME);

        try {
            loadRows(lookBackMinutes, wbitypNames, wbitypStatus);
        }
        catch (Exception e) {
            throw new NestedRuntimeException (e);
        }
    }


    /**
     *
     *@exception  AccessException  Description of Exception
     */
    private void loadRows(int lookBackMinutes, String[] wbitypNames, String[] wbitypStatus)
        throws AccessException, SQLException{
        rows.clear();
        Date lookbackDate = DateHelper.addMinutes(taskDateTime,  -1 * lookBackMinutes);
        StringBuffer sb = new StringBuffer(400);

        sb.append("SELECT wbityp_name, wbitran_filename, wbitran_status, ");
        sb.append(" wbitran_msg, wbitran_start_date, wbitran_end_date ");
        sb.append("FROM wbint_transaction ");
        sb.append(" WHERE ");
        sb.append(" wbitran_start_date > ? ");
        if (wbitypNames != null && wbitypNames.length > 0) {
            sb.append(" AND wbityp_name IN (");
            for (int i = 0; i < wbitypNames.length; i++) {
                sb.append(i > 0 ? ",?" : "?");
            }
            sb.append(")");
        }
        if (wbitypStatus != null && wbitypStatus.length > 0) {
            sb.append(" AND wbitran_status IN (");
            for (int i = 0; i < wbitypStatus.length; i++) {
                sb.append(i > 0 ? ",?" : "?");
            }
            sb.append(")");
        }

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sb.toString());
            int cnt = 1;
            ps.setTimestamp(cnt++ , new Timestamp(lookbackDate.getTime()) );
            if (wbitypNames != null && wbitypNames.length > 0) {
                for (int i = 0; i < wbitypNames.length; i++) {
                    ps.setString(cnt ++ , wbitypNames[i]);
                }
            }
            if (wbitypStatus != null && wbitypStatus.length > 0) {
                for (int i = 0; i < wbitypStatus.length; i++) {
                    ps.setString(cnt ++ , wbitypStatus[i]);
                }

            }

            rs = ps.executeQuery();
            while (rs.next()) {
                Row r = new BasicRow(getRowDefinition());
                r.setValue(COL_WBITYP_NAME , rs.getString(1));
                r.setValue(COL_WBITRAN_FILENAME , rs.getString(2) );
                r.setValue(COL_WBITRAN_STATUS , rs.getString(3));
                r.setValue(COL_WBITRAN_MSG , rs.getString(4) );
                r.setValue(COL_WBITRAN_START_DATE , rs.getString(5));
                r.setValue(COL_WBITRAN_END_DATE , rs.getString(6) );
                rows.add(r);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        if (logger.isDebugEnabled()) logger.debug("Loaded : " + rows.size());
    }

    public RowDefinition getRowDefinition() throws AccessException {
        return rowDefinition;
    }

    public RowCursor query(String queryString) throws AccessException{
        return queryAll();
    }

    public RowCursor query(String queryString, String orderByString) throws AccessException{
        return queryAll();
    }

    public RowCursor query(List keys) throws AccessException{
        return queryAll();
    }

    public RowCursor query(String[] fields, Object[] values) throws AccessException {
        return queryAll();
    }

    public RowCursor queryAll()  throws AccessException{
        return new AbstractRowCursor(getRowDefinition()){
            private int counter = -1;
            protected Row getCurrentRowInternal(){
                return counter >= 0 && counter < rows.size() ? (BasicRow)rows.get(counter) : null;
            }
            protected boolean fetchRowInternal() throws AccessException{
                return ++counter < rows.size();
            }
            public void close(){}
        };
    }

    public boolean isReadOnly(){
       return true;
    }

    public int count() {
        return rows.size();
    }

    public int count(String where) {
        return rows.size();
    }


}






