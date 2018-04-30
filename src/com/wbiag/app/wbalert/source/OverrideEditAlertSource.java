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
 * OverrideEditAlertSource
 */
public class OverrideEditAlertSource extends AbstractRowSource{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(OverrideEditAlertSource.class);


    public static final String PARAM_LOOK_BACK_MINUTES = "LOOK_BACK_MINUTES";
    public static final String PARAM_OVR_TYPES = "OVR_TYPES";
    public static final String PARAM_OVR_COLUMNS = "OVR_COLUMNS";

    public static final String EMP_ID_COL = "EMP_ID";
    public static final String WBU_NAME_COL = "WBU_NAME";
    public static final String OVR_CREATE_DATE_COL = "OVR_CREATE_DATE";
    public static final String OVR_START_DATE_COL = "OVR_START_DATE";
    public static final String OVR_END_DATE_COL = "OVR_END_DATE";
    public static final String OVR_START_TIME_COL = "OVR_START_TIME";
    public static final String OVR_END_TIME_COL = "OVR_END_TIME";
    public static final String OVRTYP_ID_COL = "OVRTYP_ID";
    public static final String OVR_STATUS_COL = "OVR_STATUS";
    public static final String OVR_NEW_VALUE_COL = "OVR_NEW_VALUE";
    public static final String OVR_COMMENT_COL = "OVR_COMMENT";

    public static final String DATE_FMT = "MM/dd/yyyy";
    public static final String TIME_FMT = "hh:mm";

    private RowDefinition rowDefinition;
    private DBConnection conn;

    private java.util.List rows = new ArrayList();
    private static List allOvrCols = new ArrayList();
    private static String allOvrColsString;

    private static String ovrColsString;
    private List ovrColumns = new ArrayList();
    private int[] ovrTypIds;
    private int lookBackMins = 0;
    private Date taskDateTime;

    static {
        allOvrCols.add(EMP_ID_COL );
        allOvrCols.add(WBU_NAME_COL );
        allOvrCols.add(OVR_CREATE_DATE_COL );
        allOvrCols.add(OVR_START_DATE_COL );
        allOvrCols.add(OVR_END_DATE_COL );
        allOvrCols.add(OVR_START_TIME_COL );
        allOvrCols.add(OVR_END_TIME_COL );
        allOvrCols.add(OVRTYP_ID_COL );
        allOvrCols.add(OVR_STATUS_COL );
        allOvrCols.add(OVR_NEW_VALUE_COL );
        allOvrCols.add(OVR_COMMENT_COL );
        StringBuffer sb = new StringBuffer(200);
        for (int i=0 , k=allOvrCols.size() ; i<k ; i++) {
            String item = (String)allOvrCols.get(i);
            sb.append(i > 0 ? "," : "");
            sb.append(item);
        }
        allOvrColsString = sb.toString();

    }

    /**
     *
     *@param  c                    Connection
     *@param  alertParams          alert parameters
     *@exception  AccessException  Description of Exception
     */
    public OverrideEditAlertSource(DBConnection c , HashMap alertParams) throws AccessException {
        this.conn = c;
        String lookBackMinsS = (String)alertParams.get(PARAM_LOOK_BACK_MINUTES);
        if (!StringHelper.isEmpty(lookBackMinsS)) {
            try {
                this.lookBackMins = Integer.parseInt(lookBackMinsS);
                this.lookBackMins = (lookBackMins < 0) ? 0 : lookBackMins;
            }
            catch (NumberFormatException ex) {
                throw new AccessException(PARAM_LOOK_BACK_MINUTES + " must be an Integer");
            }
        }
        this.taskDateTime = (Date) alertParams.get(WBAlertProcess.TASK_PARAM_TASKDATETIME);
        this.ovrColsString = (String)alertParams.get(PARAM_OVR_COLUMNS);
        if (StringHelper.isEmpty(this.ovrColsString)) {
            throw new AccessException("PARAM_OVR_COLUMNS cannot be empty");
        }
        this.ovrColumns = StringHelper.detokenizeStringAsList(this.ovrColsString , ",");
        this.ovrTypIds = StringHelper.detokenizeStringAsIntArray(
            (String)alertParams.get(PARAM_OVR_TYPES), ",", true);
        initSourceDefinition();

        try {
            loadRows();
        }
        catch (Exception e) {
            throw new NestedRuntimeException (e);
        }
    }

    /**
     * Returns all supported override related columns in a list
     * @return
     */
    public static List getAllOverrideColumns() {
        return allOvrCols;
    }

    /**
     * Returns all supported override related columns in a comma-delimited string
     * @return
     */
    public static String getAllOverrideColumnString() {
        return allOvrColsString;
    }

    private void initSourceDefinition() throws AccessException {
        //**** create row structure and definiton
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("creating row structure and definition");}
        RowStructure rs = new RowStructure();
        Iterator iter = ovrColumns.iterator();
        while (iter.hasNext()) {
            String item = (String)iter.next();
            if (allOvrCols.contains(item)) {
                rs.add(item, StringType.get());
            }
        }
        rowDefinition = new RowDefinition(-1,rs);
    }

    /**
     *
     *@exception  AccessException  Description of Exception
     */
    private void loadRows() throws AccessException, SQLException{
        rows.clear();
        Date lookbackDate = DateHelper.addMinutes(taskDateTime, -1 * lookBackMins);
        loadRows(lookbackDate);
        if (logger.isDebugEnabled()) logger.debug("Loaded : " + rows.size());
    }

    /**
     *
     *@exception  AccessException  Description of Exception
     */
    private void loadRows(Date lookbackDate) throws AccessException, SQLException{

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT ").append(this.ovrColsString) ;
            sb.append(" FROM override ");

            sb.append(" WHERE ");

            if (this.ovrTypIds != null && this.ovrTypIds.length > 0) {
                sb.append(" ( ");
                for (int i = 0; i < this.ovrTypIds.length; i++) {
                    sb.append( i > 0 ? " OR " : "");
                     sb.append(" (ovrtyp_id BETWEEN ? and ? ) ");
                }
                sb.append(" ) AND");
            }
            sb.append(" (ovr_cancelled_date > ? OR ovr_create_date > ?)");

            int ind = 1;
            ps = conn.prepareStatement(sb.toString());

            if (this.ovrTypIds != null && this.ovrTypIds.length > 0) {
                sb.append(" ( ");
                for (int i = 0; i < this.ovrTypIds.length; i++) {
                    ps.setInt(ind++ , this.ovrTypIds[i]);
                    ps.setInt(ind++ , this.ovrTypIds[i] + 99);
                }
            }


            ps.setTimestamp(ind++ , new Timestamp(lookbackDate.getTime()));
            ps.setTimestamp(ind++ , new Timestamp(lookbackDate.getTime()));

            rs = ps.executeQuery();
            while (rs.next()) {
                Row r = new BasicRow(getRowDefinition());
                Iterator iter = this.ovrColumns.iterator();
                while (iter.hasNext()) {
                    String item = (String)iter.next();
                    String val = null;
                    if (OVR_START_DATE_COL.equals(item)
                        || OVR_END_DATE_COL.equals(item)) {
                        Date valD = rs.getTimestamp(item);
                        if (valD != null) {
                            val = DateHelper.convertDateString(valD, DATE_FMT);
                        }
                    }
                    else  if (OVR_START_TIME_COL.equals(item)
                        || OVR_END_TIME_COL.equals(item)) {
                        Date valD = rs.getTimestamp(item);
                        if (valD != null) {
                            val = DateHelper.convertDateString(valD, TIME_FMT);
                        }
                    }
                    else {
                        val = rs.getString(item);
                    }

                    r.setValue(item, val);
                }
                rows.add(r);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

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






