package com.wbiag.app.modules.leaverequest.source;

import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.wbiag.app.ta.model.ReasonData;
import com.workbrain.server.data.ParameterList;


/**
 * This class retrieves the timecode and balances that are requestable through
 * the leave request form.
 *
 * @author crector
 *
 */
public class ReasonSource extends AbstractRowSource {

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(ReasonSource.class);

  public static String CONNECTION = "connection";
  // public static String LEAVEID = "leaveId";

  public static String REASON_TABLE = ReasonData.TABLE_NAME;
  public static String WGRSN_ID = ReasonData.WGRSN_ID;
  public static String WGRSN_NAME = ReasonData.WGRSN_NAME;
  public static String WGRSN_DESC = ReasonData.WGRSN_DESC;
  public static String WGLVE_ID = ReasonData.WGLVE_ID;

  private RowDefinition rowDefinition;
  private int leaveId;
  private DBConnection connection = null;
  private java.util.List rows = new ArrayList();
  private final int COLUMNS_COUNT = 3;

  {
    RowStructure rs = new RowStructure(COLUMNS_COUNT);
    rs.add(WGRSN_ID, CharType.get(100));
    rs.add(WGRSN_NAME, CharType.get(100));
    rs.add(WGRSN_DESC, CharType.get(100));
    rs.add(WGLVE_ID, CharType.get(100));
    rowDefinition = new RowDefinition(-1,rs);
  }



  /**
   *
   * @param connection
   * @param list - expected to contain an employee ID
   * @throws AccessException
   */
  public ReasonSource(DBConnection connection, ParameterList list)
      throws AccessException {

    this.connection = connection;

    try {
      loadRows();
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Error loading rows");
      throw new AccessException("Error loading rows", e);
    }
  }

  private void loadRows() throws Exception{

    logger.debug("Retrieving leave reason rows");
    rows.clear();
    RecordAccess ra = new RecordAccess(connection);

    List reasonList =
      ra.loadRecordData(new ReasonData(), REASON_TABLE, "1=1");

    if (reasonList.isEmpty()) {
      logger.debug("No reason entries found");
      return;
    }

    Iterator iter = reasonList.iterator();

    while ( iter.hasNext() ) {

      ReasonData leaveData = (ReasonData) iter.next();
      Row row = new BasicRow(getRowDefinition());

      row.setValue(WGRSN_ID, Integer.toString(leaveData.getWgrsnId()));
      row.setValue(WGRSN_NAME, leaveData.getWgrsnName());
      row.setValue(WGRSN_DESC, leaveData.getWgrsnDesc());
      row.setValue(WGLVE_ID, Integer.toString(leaveData.getWglveId()));
      rows.add(row);
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

