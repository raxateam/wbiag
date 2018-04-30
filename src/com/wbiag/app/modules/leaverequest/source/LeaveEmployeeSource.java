package com.wbiag.app.modules.leaverequest.source;

import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;

public class LeaveEmployeeSource extends AbstractRowSource {
  private RowDefinition rowDefinition;
  private int empId;
  private DBConnection connection = null;
  private java.util.List rows = new ArrayList();
  private final int COLUMNS_COUNT = 3;
  private final String DATE_FORMAT =  "MM/dd/yyyy";
  private SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

  private static String FULL_NAME = "FULL_NAME";
  private static String HIRE_DATE = "HIRE_DATE";
  private static String SEN_DATE = "SEN_DATE";

  // *** Define column names here
  {
    RowStructure rs = new RowStructure(COLUMNS_COUNT);
    rs.add(FULL_NAME,CharType.get(100));
    rs.add(HIRE_DATE,CharType.get(50));
    rs.add(SEN_DATE,CharType.get(50));
    rowDefinition = new RowDefinition(-1,rs);
  }

  /** returns the LTA ovrIds for empId starting with the current date
   *@param  empId                a valid empId String type
   *@param  connection           DBConnection
   *@param  list                 Paramter List as described
   *@exception  AccessException  Description of Exception
   */
  public LeaveEmployeeSource(DBConnection connection,
      com.workbrain.server.data.ParameterList list) throws AccessException {
    String empIdParam = (String)list.findParam ("empId").getValue() ;
    // *** do not attempt if params are null or strings like #request.TextBox#
    if ((empIdParam == null) || (empIdParam.indexOf("#") != -1)) {
      return;
    }
    this.empId = Integer.parseInt(empIdParam.trim()) ;
    this.connection = connection;
    loadRows();
  }

  private void loadRows() throws AccessException{
    rows.clear();
    try {
      EmployeeAccess empAccess = new EmployeeAccess(this.connection,
          CodeMapper.createCodeMapper(this.connection));
      EmployeeData employee = empAccess.loadRawData(this.empId);
      if (employee == null) return;
      Row row = new BasicRow(getRowDefinition());
      row.setValue(FULL_NAME,employee.getEmpFirstname() + " " +
          employee.getEmpLastname());
      row.setValue(HIRE_DATE, this.formatDate(employee.getEmpHireDate()));
      row.setValue(SEN_DATE, this.formatDate(employee.getEmpSeniorityDate()));
      this.rows.add(row);
    } catch (Exception e) {
      throw new AccessException(e);
    }
  }
  private String formatDate(java.util.Date date){
    return this.sdf.format(date);
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
