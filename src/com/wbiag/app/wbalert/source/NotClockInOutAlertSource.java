/**
 * Created on May 13, 2005
 *
 * Title: NotClockInOutAlertSource
 * Description:
 * <p>Copyright:  Copyright (c) 2004</p>
 * <p>Company:    Workbrain Inc.</p>
 */
package com.wbiag.app.wbalert.source;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.workbrain.server.data.AbstractRowCursor;
import com.workbrain.server.data.AbstractRowSource;
import com.workbrain.server.data.AccessException;
import com.workbrain.server.data.BasicRow;
import com.workbrain.server.data.Row;
import com.workbrain.server.data.RowCursor;
import com.workbrain.server.data.RowDefinition;
import com.workbrain.server.data.RowStructure;
import com.workbrain.server.data.type.CharType;
import com.workbrain.server.data.type.StringType;
import com.workbrain.sql.DBConnection;

import com.workbrain.app.ta.model.Clock;
import com.workbrain.util.*;

/**
 * @author BLi
 *
 * @version  1.0
 */
public class NotClockInOutAlertSource extends AbstractRowSource {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(NotClockInOutAlertSource.class);
    public static final String PARAM_CHECK_NO_CLOCK_IN = "CHECK_NO_CLOCK_IN";
    public static final String PARAM_CHECK_NO_CLOCK_OUT = "CHECK_NO_CLOCK_OUT";
    public static final String PARAM_CLOCK_IN_TYPES = "CLOCK_IN_TYPES";
    public static final String PARAM_CLOCK_OUT_TYPES = "CLOCK_OUT_TYPES";

    private RowDefinition rowDefinition;
    private DBConnection conn;
    private java.util.List rows = new ArrayList();
    private boolean checkNoClockIn;
    private boolean checkNoClockOut;
    private List clocks  = new ArrayList();
    private String clockInTypes;
    private String clockOutTypes;

    public static final String COL_EMP_ID = "EMP_ID";
    public static final String COL_EMP_NAME = "EMP_NAME" ;
    public static final String COL_EMP_FIRSTNAME = "EMP_FIRSTNAME";
    public static final String COL_EMP_LASTNAME = "EMP_LASTNAME";
    public static final String COL_NO_CLOCK_IN = "NOT_CLOCK_IN";
    public static final String COL_NO_CLOCK_OUT = "NOT_CLOCK_OUT";

    {
        RowStructure rs = new RowStructure(10);
        rs.add(COL_EMP_ID,StringType.get());
        rs.add(COL_EMP_NAME,StringType.get());
        rs.add(COL_EMP_FIRSTNAME,StringType.get());
        rs.add(COL_EMP_LASTNAME,StringType.get());
        rs.add(COL_NO_CLOCK_IN,CharType.get(1));
        rs.add(COL_NO_CLOCK_OUT,CharType.get(1));
        rowDefinition = new RowDefinition(-1,rs);
    }


    /**
     *
     */
    public NotClockInOutAlertSource(DBConnection c , HashMap alertParams) throws AccessException  {
        this.conn = c;

        String sCheckNoClockIn = (String)alertParams.get(PARAM_CHECK_NO_CLOCK_IN);
        String sCheckNoClockOut = (String)alertParams.get(PARAM_CHECK_NO_CLOCK_OUT);
        clockInTypes = (String)alertParams.get(PARAM_CLOCK_IN_TYPES);
        clockOutTypes = (String)alertParams.get(PARAM_CLOCK_OUT_TYPES);
        this.checkNoClockIn = "Y".equalsIgnoreCase(sCheckNoClockIn) ? true : false;
        this.checkNoClockOut = "Y".equalsIgnoreCase(sCheckNoClockOut)? true : false;

        if (!checkNoClockIn && !checkNoClockOut) {
            throw new AccessException("PARAM_CHECK_NO_CLOCK_IN and/or PARAM_CHECK_NO_CLOCK_OUT must be true");
        }
        try {
            loadRows();
        } catch (SQLException e) {
            throw new AccessException("Error in loading rows" , e);
        }
    }

    /**
    *
    *@exception  AccessException  Description of Exception
    */
   private void loadRows() throws AccessException, SQLException{
       rows.clear();
       Date workDate = DateHelper.truncateToDays(new Date());
       StringBuffer sb = new StringBuffer(400);

       sb.append("SELECT EMPLOYEE.EMP_ID, EMP_NAME, EMP_FIRSTNAME, EMP_LASTNAME, WRKS_CLOCKS ");
       sb.append(" FROM WORK_SUMMARY, EMPLOYEE ");
       sb.append(" WHERE WORK_SUMMARY.EMP_ID = EMPLOYEE.EMP_ID ");
       sb.append(" AND WORK_SUMMARY.WRKS_WORK_DATE = ? ");

       boolean noClockIn = true;
       boolean noClockOut = true;
       PreparedStatement ps = null;
       ResultSet rs = null;
       try {
           ps = conn.prepareStatement(sb.toString());
           int cnt = 1;
           ps.setTimestamp(cnt++ , new Timestamp(workDate.getTime()) );
           rs = ps.executeQuery();
		   if(logger.isDebugEnabled()) { logger.debug("Query executed."); }

           while (rs.next()) {
			   if(logger.isDebugEnabled()) { logger.debug("Retrieved: " + rs.getString(1)); }
               Row r = new BasicRow(getRowDefinition());
               if ( checkNoClockIn && ("".equals(rs.getString(5)) || rs.getString(5) == null)){
	               r.setValue(COL_EMP_ID , rs.getString(1));
	               r.setValue(COL_EMP_NAME , rs.getString(2));
	               r.setValue(COL_EMP_FIRSTNAME , rs.getString(3));
	               r.setValue(COL_NO_CLOCK_IN , "Y");
	               r.setValue(COL_NO_CLOCK_OUT , null);
               } else {
        	       try {
        		       	 clocks = Clock.createClockListFromString(rs.getString(5));
        		       } catch (Exception e){
        		           if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Can not create clock list from a String: " + e.getMessage() , e);}
        		           throw new NestedRuntimeException(e);
        		       }

        		   if (clocks.size() > 0) {
        		       for (int i=0; i < clocks.size(); i ++) {
        		           Clock theClock = (Clock)clocks.get(i);
        		           if (clockInTypes.indexOf(Integer.toString(theClock.getClockType())) > 0 ) {
        		               noClockIn = false;
        		           }
        		           if (clockOutTypes.indexOf(Integer.toString(theClock.getClockType())) > 0 &&
        		                   clockOutTypes.indexOf(Integer.toString(((Clock)clocks.get(clocks.size())).getClockType())) > 0  ) {
        		               noClockOut = false;
        		           }
        		       }
        		   }

	               if ( checkNoClockIn && noClockIn && checkNoClockOut && noClockOut ) {
		               r.setValue(COL_EMP_ID , rs.getString(1));
		               r.setValue(COL_EMP_NAME , rs.getString(2));
		               r.setValue(COL_EMP_FIRSTNAME , rs.getString(3));
		               r.setValue(COL_NO_CLOCK_IN , "Y");
		               r.setValue(COL_NO_CLOCK_OUT , "Y");
	               } else if ( checkNoClockOut && noClockOut ){
		               r.setValue(COL_EMP_ID , rs.getString(1));
		               r.setValue(COL_EMP_NAME , rs.getString(2));
		               r.setValue(COL_EMP_FIRSTNAME , rs.getString(3));
		               r.setValue(COL_NO_CLOCK_IN , null);
		               r.setValue(COL_NO_CLOCK_OUT , "Y");
	               } else if ( checkNoClockIn && noClockIn ) {
		               r.setValue(COL_EMP_ID , rs.getString(1));
		               r.setValue(COL_EMP_NAME , rs.getString(2));
		               r.setValue(COL_EMP_FIRSTNAME , rs.getString(3));
		               r.setValue(COL_NO_CLOCK_IN , "Y");
		               r.setValue(COL_NO_CLOCK_OUT , null);
	               }
               }
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

