package com.wbiag.app.wbinterface.forecastimport;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.text.*;
import com.workbrain.app.wbinterface.TransactionTypeBatch;
import com.workbrain.app.wbinterface.WBInterfaceException;
import com.workbrain.app.wbinterface.db.ImportData;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.DateHelper;

/**
 *Processor for forecast import
 *
 *@author     Neshan Kumar
 *@created    April 03, 2006
 */
public class ForecastImportTransaction extends TransactionTypeBatch {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ForecastImportTransaction.class);

    private final static int SKDGRP_NAME_COL = 0;
    private final static int VOLTYP_NAME_COL = 1;
    private final static int FCAST_TO_DATE_COL = 2;
    private final static int VALUE_COL = 3;
    private final static int ACTION_COL = 4;
    private final static int TYPE_COL = 5;
    private final static String PLUS_ADJUST_ACTION = "+";
    private final static String MINUS_ADJUST_ACTION = "-";
    private final static String STATIC_EVEN_ADJUST_TYPE = "STATIC EVEN";
    private final static String PERCENT_ADJUST_TYPE = "PERCENT";
    private final static String STATIC_PERCENT_ADJUST_TYPE = "STATIC PERCENT";
    //Assumed that the forecast is being done for one week
    private static final int NUMBER_OF_DAYS_FORECAST = 7;
    private static final int DEFAULT_BATCH_PROCESS_SIZE  = 100;
    //If this entry is not in db, the value of DEFAULT_BATCH_PROCESS_SIZE(=100) will be used
	private String BATCH_MAX_RECORDS = "/system/WORKBRAIN_PARAMETERS/BATCH_MAX_RECORDS";
	private static String DATE_FMT = "MM/dd/yyyy";
    //Error messages
    private static final String INVALID_STORE_ERROR_MESSAGE = "Store does not exist.";
    private static final String INVALID_VOLUME_TYPE_ERROR_MESSAGE = "Volume type does not exist.";
    private static final String INVALID_DATE_FORMAT_ERROR_MESSAGE = "Invalid date format. Date format is MM/dd/yyyy";
    private static final String INVALID_PRODUCTIVITY_ERROR_MESSAGE = "Invalid productivity value";
    private static final String INVALID_DATA_ERROR_MESSAGE = "Invalid Data";
	private String message = "";
	private String status  = ImportData.STATUS_APPLIED;
	private String skdGrpName;
	private String volTypeName;
	private String value;
	private String action;
	private String type;
	private java.util.Date fcast_to_date;
	private java.util.Date fcast_from_date;
	private double doubleValue;
	
	
    protected void preProcessBatch(DBConnection c, List importDataList)
	throws SQLException, WBInterfaceException 
	{
	}
	
	protected void postProcessBatch(DBConnection c, List importDataList)
	throws SQLException, WBInterfaceException 
	{
	}

    public List processBatch(DBConnection conn, List importDataList)
             throws Exception {
       List results;		
	   preProcessBatch(conn, importDataList);
	   results = processList(conn, importDataList);
	   postProcessBatch(conn, results);
	   return results;
    }
    
    public List processList(DBConnection conn, List importDataList) throws Exception {
	   Iterator it = importDataList.iterator();			
	   ImportData data;
	   PreparedStatement ps = null;
	   PreparedStatement pstmt = null;
	   ResultSet rs = null;
	   StringBuffer sql = null;
	   int fcastDetId = -1;
	   double fcastCalls = 0;
	   double fcastAdjval = 0;
	   double newAdjustValue = 0;
	   double totalForecast = 0;
	   
	   int driverId = -1;
	   String driverName = null;
	   ForecastDetailData forecastDetailData = null;
	   DriverData driverData = null;
	   Map forecastDetailMap = null;
	   ArrayList driverList = new ArrayList();
	   
	   sql = new StringBuffer();
	   sql.append(" SELECT ");
	   sql.append(" FD.FCASTDET_ID, ");
	   sql.append(" FD_SKDGRP.SKDGRP_ID DRIVER_SKDGRP_ID, ");
	   sql.append(" FD_SKDGRP.SKDGRP_NAME DRIVER_SKDGRP_NAME, ");
	   sql.append(" ST_SKDGRP.SKDGRP_NAME STORE_SKDGRP_NAME, ");
	   sql.append(" FD.FCAST_DATE, ");
	   sql.append(" FD.FCAST_CALLS, ");
	   sql.append(" FD.FCAST_ADJVAL, ");
	   sql.append(" VT.VOLTYP_NAME ");

	   sql.append(" FROM ");
	   sql.append(" SO_SCHEDULE_GROUP FD_SKDGRP, ");
	   sql.append(" SO_SCHEDULE_GROUP ST_SKDGRP, ");
	   sql.append(" WORKBRAIN_TEAM FD_WBT, ");
	   sql.append(" WORKBRAIN_TEAM ST_WBT, ");
	   sql.append(" SO_VOLUME_TYPE VT, ");
	   sql.append(" SO_FCAST F, ");
	   sql.append(" SO_FCAST_DETAIL FD ");

	   sql.append(" WHERE ");
	   sql.append(" FD_SKDGRP.WBT_ID = FD_WBT.WBT_ID ");
	   sql.append(" AND ");
	   sql.append(" ST_SKDGRP.WBT_ID = ST_WBT.WBT_ID ");
	   sql.append(" AND ");
	   sql.append(" FD_WBT.WBT_LFT BETWEEN ST_WBT.WBT_LFT AND ST_WBT.WBT_RGT ");
	   sql.append(" AND ");
	   sql.append(" FD_WBT.WBTT_ID = 2 ");
	   sql.append(" AND ");
	   sql.append(" ST_SKDGRP.SKDGRP_NAME = ? ");
	   sql.append(" AND ");
	   sql.append(" FD_SKDGRP.VOLTYP_ID = VT.VOLTYP_ID ");
	   sql.append(" AND ");
	   sql.append(" F.FCAST_ID = FD.FCAST_ID ");
	   sql.append(" AND ");
	   sql.append(" FD_SKDGRP.SKDGRP_ID = F.SKDGRP_ID ");
	   sql.append(" AND ");
	   sql.append(" FD_SKDGRP.VOLTYP_ID = (SELECT VOLTYP_ID FROM SO_VOLUME_TYPE WHERE VOLTYP_NAME= ? ) ");
	   sql.append(" AND ");
	   sql.append(" FD.FCAST_DATE BETWEEN ? AND ? ");	    	   
	   sql.append(" AND ");
	   sql.append(" NOT EXISTS( ");
	   sql.append(" SELECT 0 ");
	   sql.append(" FROM ");
	   sql.append(" WORKBRAIN_TEAM I_WBT ");
	   sql.append(" WHERE ");
	   sql.append(" I_WBT.WBT_LFT ");
	   sql.append(" BETWEEN ST_WBT.WBT_LFT AND ST_WBT.WBT_RGT ");
	   sql.append(" AND ");
	   sql.append(" I_WBT.WBTT_ID = (SELECT WBTT_ID FROM WORKBRAIN_TEAM_TYPE WHERE WBTT_NAME='STORE') ");
	   sql.append(" AND ");
	   sql.append(" I_WBT.WBT_ID != ST_WBT.WBT_ID) ");
	   sql.append(" ORDER BY  DRIVER_SKDGRP_ID, FD.FCAST_DATE,  DRIVER_SKDGRP_NAME ");
	    
	   // bchan TT2113 - adjustment type is required to be set
	   String updateSql = "UPDATE SO_FCAST_DETAIL SET FCAST_ADJVAL = ?, FCAST_ADJTYP = 2 " +
	   						"WHERE FCASTDET_ID = ? "; 
	   
	   try {		
	      while(it.hasNext()){																													
		     data = (ImportData)it.next();
			 data.setStatus(ImportData.STATUS_ERROR);
			 
			 if ( validateData( conn, data ) ) {	
			    ps = conn.prepareStatement(sql.toString());
				pstmt = conn.prepareStatement(updateSql);
				ps.setString(1, skdGrpName);
			    ps.setString(2, volTypeName);
			    ps.setDate(3, new java.sql.Date(fcast_from_date.getTime()));
			    ps.setDate(4, new java.sql.Date(fcast_to_date.getTime()));    
			    rs = ps.executeQuery();
			    
			    //Loading records
			    int lastDriverId = -1;
			    Map fcastDetailMap = new HashMap();
			    while(rs.next()) {
			       driverId = rs.getInt("DRIVER_SKDGRP_ID");
			    	
			       if( driverId != lastDriverId && lastDriverId != -1 ) {
			          driverData = new DriverData(lastDriverId, driverName, totalForecast,fcastDetailMap );
			    	  driverList.add(driverData); 
			    	  fcastDetailMap = new HashMap();
			    	  totalForecast = 0;
			        }
			    	driverName = rs.getString("DRIVER_SKDGRP_NAME");
			    	
			    	fcastDetId = rs.getInt("FCASTDET_ID");
			    	fcastCalls = rs.getDouble("FCAST_CALLS");
				   	fcastAdjval = rs.getDouble("FCAST_ADJVAL");
			    	forecastDetailData = new ForecastDetailData( fcastDetId, fcastCalls, fcastAdjval );
			    	fcastDetailMap.put( new Integer(fcastDetId), forecastDetailData );
			    	totalForecast += fcastCalls + fcastAdjval;
			    	lastDriverId = driverId;	  
			    }
                 //Loading last record
			    if ( lastDriverId != -1 ) {
			       driverData = new DriverData(lastDriverId, driverName, totalForecast, fcastDetailMap );
		    	   driverList.add(driverData); 
			    }
			    
                //Processing records
			    for ( int i = 0; i < driverList.size(); i++ ) {
			       driverData = (DriverData) driverList.get( i );
			       totalForecast = driverData.getTotalForecast();
			       fcastDetailMap = driverData.fcastDetailMap;
			       Object[] fcastDetailKeyArray = fcastDetailMap.keySet().toArray();
			       
			       for( int j = 0; j < fcastDetailKeyArray.length; j ++ ) {
			          forecastDetailData = (ForecastDetailData) fcastDetailMap.get(fcastDetailKeyArray[ j ]);
			          fcastDetId = forecastDetailData.getFcastDetId();
			          fcastCalls = forecastDetailData.getfCastCalls();
			          fcastAdjval = forecastDetailData.getFcastAdjval();
			          
			          //Calculating the new adjust value
			          newAdjustValue = calculateAdjustValue( data.getField(ACTION_COL),
						           				             data.getField(TYPE_COL),
						           				             totalForecast,
															 fcastCalls,
															 fcastAdjval );
			          pstmt.setDouble(1, newAdjustValue);
				      pstmt.setInt(2, fcastDetId);    
				      pstmt.addBatch();   
			       }
			    }
			       pstmt.executeBatch();
			       pstmt.clearBatch();
				   data.setStatus(ImportData.STATUS_APPLIED);	
			 }	
	      }						
	   }catch(Exception e){
		   if(conn != null){
		      conn.rollback();
		   }
		   throw e;
	   }finally {
	 	   SQLHelper.cleanUp(rs);
	 	   SQLHelper.cleanUp(ps);
	 	   SQLHelper.cleanUp(pstmt);
	 	}
		return importDataList;
	}
    
    private double calculateAdjustValue(String action,
									    String type,
										double totalForecast,
									    double fcastCalls,
									    double fcastAdjval) { 
       double adjustValPerDay = 0;
       double forecastPerDay = 0;
       
       try {
          forecastPerDay = fcastCalls + fcastAdjval;
          
          //STATIC EVEN TYPE
          if (type.equalsIgnoreCase(STATIC_EVEN_ADJUST_TYPE)) {
             adjustValPerDay = doubleValue/NUMBER_OF_DAYS_FORECAST;  
          }
          //STATIC PERCENT TYPE
          else if (type.equalsIgnoreCase(STATIC_PERCENT_ADJUST_TYPE)){
             //avoiding division by zero error
             if(totalForecast > 0) { 
                adjustValPerDay = (doubleValue/totalForecast) * forecastPerDay;
             }
          }
          //PERCENT TYPE
          else if (type.equalsIgnoreCase(PERCENT_ADJUST_TYPE)){
       	     adjustValPerDay = (doubleValue/100)* forecastPerDay;
          }
          //rounding to one decimal 
          adjustValPerDay = Math.round(adjustValPerDay * 10)/10.0;
       
          //Do the action
          if (action.equalsIgnoreCase(PLUS_ADJUST_ACTION)) {
             adjustValPerDay = fcastAdjval + adjustValPerDay;
          }
          else if (action.equalsIgnoreCase(MINUS_ADJUST_ACTION)) {
             adjustValPerDay = fcastAdjval - adjustValPerDay;
          }
          //If new forecast goes negative, make it to 0
          if (fcastCalls + adjustValPerDay < 0 ) {
             adjustValPerDay = -1 * fcastCalls;
          }
       
       }catch (Exception e) {
          e.printStackTrace();
       }
       return adjustValPerDay;
    }
    
    private boolean validateData(DBConnection conn, ImportData data) {
       skdGrpName = null;
       volTypeName = null;
       value = null;
       action = null;
       type = null;
       fcast_to_date = null;
       fcast_from_date = null;
       String fcastToDate = null;
       boolean errorFlag = true;
       
       try{
          skdGrpName = data.getField(SKDGRP_NAME_COL);
	      volTypeName = data.getField(VOLTYP_NAME_COL);
	      fcastToDate = data.getField(FCAST_TO_DATE_COL);
	      value = data.getField(VALUE_COL);
	      action = data.getField(ACTION_COL);
	      type = data.getField(TYPE_COL);
	   
          if ( skdGrpName == null || "".equals(skdGrpName) ||
          	   volTypeName == null || "".equals(volTypeName) ||
          	   fcastToDate == null || "".equals(fcastToDate) ||
          	   value == null || "".equals(value) ||
          	   action == null || "".equals(action) ||
          	   type == null || "".equals(type)) {
          	   
          	   data.setMessage(INVALID_DATA_ERROR_MESSAGE);
			   errorFlag = false;
          }
          //check if schedule group exist
	      else if (!isSchedGroupExists(conn, skdGrpName)){
		     data.setMessage(INVALID_STORE_ERROR_MESSAGE);
			 errorFlag = false;
		  }
          //check if volume type exist
	      else if (!isVolumeTypeExists(conn, volTypeName)) {
		     data.setMessage(INVALID_VOLUME_TYPE_ERROR_MESSAGE);
			 errorFlag = false;
		  }
          //check if right action
	      else if (!action.equals(PLUS_ADJUST_ACTION) && !action.equals(MINUS_ADJUST_ACTION) ) {
		     data.setMessage(INVALID_DATA_ERROR_MESSAGE);
			 errorFlag = false;
		  }
          //check if right adjust type
	      else if (!type.equals(STATIC_EVEN_ADJUST_TYPE) && 
	      		   !type.equals(PERCENT_ADJUST_TYPE) &&
				   !type.equals(STATIC_PERCENT_ADJUST_TYPE) ) {
		     data.setMessage(INVALID_DATA_ERROR_MESSAGE);
			 errorFlag = false;
		  }
	      else {
             //check if right date format
	         try {
	   	        DateFormat formatter = new SimpleDateFormat(DATE_FMT); 
	            fcast_to_date = formatter.parse( fcastToDate);
		        fcast_from_date = DateHelper.addDays(fcast_to_date, (-1 * NUMBER_OF_DAYS_FORECAST) );
	         }catch(ParseException e) {
	   	        data.setMessage(INVALID_DATE_FORMAT_ERROR_MESSAGE);
		        errorFlag = false;
	         }
             //check if the the productivity value is in double format
	         try {
	            doubleValue = Double.parseDouble(value);
	         }catch(NumberFormatException nfe) {
	         	data.setMessage(INVALID_PRODUCTIVITY_ERROR_MESSAGE);
		        errorFlag = false;	
	         }
	      }
       }catch(Exception e){
       	   data.setMessage(INVALID_DATA_ERROR_MESSAGE);
		   errorFlag = false;
       }
          return errorFlag;
    }
    
    private boolean isSchedGroupExists(DBConnection c, String skdGrpName){
       int skdgrp_id = -1;
	   PreparedStatement ps = null;
	   ResultSet rs = null;
	   boolean existFlag = false;
	   
	   try {
		ps = c.prepareStatement("SELECT SKDGRP_ID FROM SO_SCHEDULE_GROUP WHERE SKDGRP_NAME = ? ");
		ps.setString(1, skdGrpName);
		rs = ps.executeQuery();
		if (rs.next()) {
			skdgrp_id = rs.getInt("SKDGRP_ID");
		}
		if (skdgrp_id != -1) {
		   existFlag = true;	
		}
	}
	catch (SQLException e) {
		e.printStackTrace();	
	}
	finally {
		SQLHelper.cleanUp(rs);
		SQLHelper.cleanUp(ps);
	} 
       return existFlag;
    }
    
    private boolean isVolumeTypeExists(DBConnection c, String volTypeName){
       int voltyp_id = -1;
 	   PreparedStatement ps = null;
 	   ResultSet rs = null;
 	   boolean existFlag = false;
 	   
 	   try {
 	      ps = c.prepareStatement("SELECT VOLTYP_ID FROM SO_VOLUME_TYPE WHERE VOLTYP_NAME = ? ");
 		  ps.setString(1, volTypeName);
 		  rs = ps.executeQuery();
 		  if (rs.next()) {
 		     voltyp_id = rs.getInt("VOLTYP_ID");
 		  }
 		  if (voltyp_id != -1) {
 		     existFlag = true;	
 		  }
 	   }catch (SQLException e) {
 		   e.printStackTrace();	
 	   }finally {
 		   SQLHelper.cleanUp(rs);
 		   SQLHelper.cleanUp(ps);
 	   }
        return existFlag;
     }
  
    public void process(ImportData data, DBConnection c) throws WBInterfaceException, SQLException {
	}
	
	public int getBatchProcessSize(HashMap params) {
		int size = 0;
		try {
			size = Integer.parseInt(getRegistryValue(BATCH_MAX_RECORDS));
		}
		catch (NumberFormatException e) {
			size = DEFAULT_BATCH_PROCESS_SIZE;
		}		
		return size;		
	}

	/* (non-Javadoc)
	 * @see com.workbrain.app.wbinterface.TransactionType#getStatus()
	 */
	
	public String getStatus() {
		if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
			logger.debug("ForecastImportTransaction.getStatus() returns " + status);
		}
		return status;
	}
	
	/* (non-Javadoc)
	 * @see com.workbrain.app.wbinterface.TransactionType#getMessage()
	 */

	public String getMessage() {
		if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
			logger.debug("ForecastImportTransaction.getMessage");
		}
		return message;
	}

	/* (non-Javadoc)
	 * @see com.workbrain.app.wbinterface.TransactionType#reset()
	 */
	public void reset() {			
		message = "";
		status  = ImportData.STATUS_APPLIED;
	}
	
	/* (non-Javadoc)
	 * @see com.workbrain.app.wbinterface.TransactionType#getTaskUI()
	 */
	public String getTaskUI() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.workbrain.app.wbinterface.TransactionType#setTransactionParameters(java.util.HashMap)
	 */
	public void setTransactionParameters(HashMap param) {
		
	}
	
	/* (non-Javadoc)
	 * @see com.workbrain.app.wbinterface.TransactionType#initializeTransaction(com.workbrain.sql.DBConnection)
	 */
	public void initializeTransaction(DBConnection conn) throws Exception {		
	}
	
	/* (non-Javadoc)
	 * @see com.workbrain.app.wbinterface.TransactionType#finalizeTransaction(com.workbrain.sql.DBConnection)
	 */
	public void finalizeTransaction(DBConnection conn) throws Exception {	
	}
	
    public String getRegistryValue(String registryName)
    {
        String registyValue = "";
        try
        {
            registyValue = (String)Registry.getVar(registryName);
        }
        catch(Exception e)
        {
            logger.debug(e);
        }               
        return registyValue;
    }

	public void resetBatch()
	{		
		message = "";
		status = ImportData.STATUS_APPLIED;		
	}
	
	/**
     * ForecastDetailData is a helper class to store the forecast detail record
     * 
     * @author Neshan Kumar
     *  
     */
	class ForecastDetailData {
	   private int fcastDetId;
	   private double fcastCalls;
	   private double fcastAdjval;
	       
	   public ForecastDetailData( int fcastDetId, double fcastCalls, double fcastAdjval ) {
	      this.fcastDetId = fcastDetId;
	      this.fcastCalls = fcastCalls;
	      this.fcastAdjval = fcastAdjval;
	   }
	   private int getFcastDetId() {
          return fcastDetId;
	   }
       private double getfCastCalls() {
          return fcastCalls;
	   }
       private double getFcastAdjval() {
          return fcastAdjval;
	   }
	} 
	
	/**
     * DriverData is a helper class to store the driver detail record
     * 
     * @author Neshan Kumar
     *  
     */
	class DriverData {
	   private int driverId;
	   private String driverName;
	   private double totalForecast;
	   private Map fcastDetailMap;
	       
	   public DriverData( int driverId, String driverName, double totalForecast, Map fcastDetailMap ) {
	      this.driverId = driverId;
	      this.driverName = driverName;
	      this.totalForecast = totalForecast;
	      this.fcastDetailMap = fcastDetailMap;
	   }
	   private int getDriverId() {
          return driverId;
	   }
       private String getDriverName() {
          return driverName;
	   }
       private double getTotalForecast() {
          return totalForecast;
	   }
	   private Map getFcastDetailMap() {
          return fcastDetailMap;
	   }
	}
}
