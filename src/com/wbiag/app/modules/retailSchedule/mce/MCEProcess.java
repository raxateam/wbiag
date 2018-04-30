/**
 * MCEProcess.java is the utility class used in the MCE process
 * 
 * @author Neshan Kumar
 *  
 */

package com.wbiag.app.modules.retailSchedule.mce;

import java.util.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;
import com.wbiag.app.modules.retailSchedule.db.*;
import com.wbiag.app.modules.retailSchedule.model.*;
import com.workbrain.app.modules.retailSchedule.db.*;
import java.sql.*; 
import java.sql.SQLException;
import java.sql.ResultSet;
import javax.servlet.http.*;

public class MCEProcess {
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MCEProcess.class);
	private DBConnection conn;
	private MCESearchSetData mceData;
	private MCESearchSetAccess mceAccess; 
	private static final int STORE_INTERNAL_TYPE = 10;
	private static final String INTEGER_CLASS = "java.lang.Integer";
	private static final String STRING_CLASS = "java.lang.String";
	private static final String BOOLEAN_CLASS = "java.lang.Boolean";
	
	public MCEProcess(DBConnection c) {
		this.conn = c;	
	}
	
	public MCEProcess(DBConnection c, HttpServletRequest request ) {
       this.conn = c;   
       this.mceData = new MCESearchSetData();
      
       //build MCESearchSetData from the request object
       String record_id = request.getParameter("record_id");
       String WIMCESS_NAME = request.getParameter("WIMCESS_NAME");
       String WIMCESS_DESC = request.getParameter("WIMCESS_DESC");
       String MCESS_TYPE = request.getParameter("MCESS_TYPE");
       String SEARCH_SET_1 = request.getParameter("SEARCH_SET_1");
       String SEARCH_SET_2 = request.getParameter("SEARCH_SET_2");
       String SEARCH_SET_3 = request.getParameter("SEARCH_SET_3");
       String SEARCH_SET_4 = request.getParameter("SEARCH_SET_4");
       String SEARCH_SET_5 = request.getParameter("SEARCH_SET_5");
       String WIMCESS_HIERNODE = request.getParameter("WIMCESS_HIERNODE");
       String WIMCESS_LOCLIST = request.getParameter("WIMCESS_LOCLIST");
       String CLNTTYP_ID = request.getParameter("CLNTTYP_ID");
       String MCESS_PROP_1 = request.getParameter("MCESS_PROP_1");
       String MCESS_PROP_2 = request.getParameter("MCESS_PROP_2");
       String MCESS_PROP_3 = request.getParameter("MCESS_PROP_3");
       String MCESS_PROP_4 = request.getParameter("MCESS_PROP_4");
       String MCESS_PROP_5 = request.getParameter("MCESS_PROP_5");
       String MCESS_PROP_6 = request.getParameter("MCESS_PROP_6");
       String MCESS_PROP_7 = request.getParameter("MCESS_PROP_7");
       String MCESS_PROP_1_MIN = request.getParameter("MCESS_PROP_1_MIN");
       String MCESS_PROP_2_MIN = request.getParameter("MCESS_PROP_2_MIN");
       String MCESS_PROP_3_MIN = request.getParameter("MCESS_PROP_3_MIN");
       String MCESS_PROP_4_MIN = request.getParameter("MCESS_PROP_4_MIN");
       String MCESS_PROP_5_MIN = request.getParameter("MCESS_PROP_5_MIN");
       String MCESS_PROP_6_MIN = request.getParameter("MCESS_PROP_6_MIN");
       String MCESS_PROP_7_MIN = request.getParameter("MCESS_PROP_7_MIN");
       String MCESS_PROP_1_MAX = request.getParameter("MCESS_PROP_1_MAX");
       String MCESS_PROP_2_MAX = request.getParameter("MCESS_PROP_2_MAX");
       String MCESS_PROP_3_MAX = request.getParameter("MCESS_PROP_3_MAX");
       String MCESS_PROP_4_MAX = request.getParameter("MCESS_PROP_4_MAX");
       String MCESS_PROP_5_MAX = request.getParameter("MCESS_PROP_5_MAX");
       String MCESS_PROP_6_MAX = request.getParameter("MCESS_PROP_6_MAX");
       String MCESS_PROP_7_MAX = request.getParameter("MCESS_PROP_7_MAX");
       
       if (!StringHelper.isEmpty(record_id) && !"null".equals(record_id)) {
          mceData.setWimcessId(Integer.parseInt(record_id));
       }
       if (!StringHelper.isEmpty(WIMCESS_NAME)) {
          mceData.setWimcessName(WIMCESS_NAME);
       }
       if (!StringHelper.isEmpty(WIMCESS_DESC)) {
          mceData.setWimcessDesc(WIMCESS_DESC);
       }
       if (!StringHelper.isEmpty(MCESS_TYPE)) {
          mceData.setWimcessType(new Integer(MCESS_TYPE));
       }
       
       if (Integer.parseInt(MCESS_TYPE) == 1) {
          if ( !StringHelper.isEmpty(SEARCH_SET_1)) {
             mceData.setWimcessSub1Id(new Integer(SEARCH_SET_1));
          }
          if (!StringHelper.isEmpty(SEARCH_SET_2)) {
            mceData.setWimcessSub2Id(new Integer(SEARCH_SET_2));
          }	
          if (!StringHelper.isEmpty(SEARCH_SET_3)) {
            mceData.setWimcessSub3Id(new Integer(SEARCH_SET_3));
          }	
          if (!StringHelper.isEmpty(SEARCH_SET_4)) {
            mceData.setWimcessSub4Id(new Integer(SEARCH_SET_4));
          }	
          if (!StringHelper.isEmpty(SEARCH_SET_5)) {
            mceData.setWimcessSub5Id(new Integer(SEARCH_SET_5));
          }	   
       }
       else {
          if (!StringHelper.isEmpty(WIMCESS_HIERNODE)) {
             mceData.setWimcessHiernode(new Integer(WIMCESS_HIERNODE));
          }
          if (!StringHelper.isEmpty(CLNTTYP_ID)) {
             mceData.setClnttypId(new Integer(CLNTTYP_ID));
          }
          if (!StringHelper.isEmpty(MCESS_PROP_1)) {
             mceData.setWimcessProp1Id(new Integer(MCESS_PROP_1));
          }
          if (!StringHelper.isEmpty(MCESS_PROP_2)) {
            mceData.setWimcessProp2Id(new Integer(MCESS_PROP_2));
          }
          if (!StringHelper.isEmpty(MCESS_PROP_3)) {
            mceData.setWimcessProp3Id(new Integer(MCESS_PROP_3));
          }
          if (!StringHelper.isEmpty(MCESS_PROP_4)) {
            mceData.setWimcessProp4Id(new Integer(MCESS_PROP_4));
          }
          if (!StringHelper.isEmpty(MCESS_PROP_5)) {
            mceData.setWimcessProp5Id(new Integer(MCESS_PROP_5));
          }
          if (!StringHelper.isEmpty(MCESS_PROP_6)) {
            mceData.setWimcessProp6Id(new Integer(MCESS_PROP_6));
          }
          if (!StringHelper.isEmpty(MCESS_PROP_7)) {
            mceData.setWimcessProp7Id(new Integer(MCESS_PROP_7));
          }
          if (!StringHelper.isEmpty(WIMCESS_LOCLIST)) {
          	mceData.setWimcessLoclist(WIMCESS_LOCLIST);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_1_MIN)) {
            mceData.setWimcessMin1(MCESS_PROP_1_MIN);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_2_MIN)) {
            mceData.setWimcessMin2(MCESS_PROP_2_MIN);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_3_MIN)) {
            mceData.setWimcessMin3(MCESS_PROP_3_MIN);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_4_MIN)) {
            mceData.setWimcessMin4(MCESS_PROP_4_MIN);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_5_MIN)) {
            mceData.setWimcessMin5(MCESS_PROP_5_MIN);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_6_MIN)) {
            mceData.setWimcessMin6(MCESS_PROP_6_MIN);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_7_MIN)) {
            mceData.setWimcessMin7(MCESS_PROP_7_MIN);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_1_MAX)) {
          	mceData.setWimcessMax1(MCESS_PROP_1_MAX);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_2_MAX)) {
          	mceData.setWimcessMax2(MCESS_PROP_2_MAX);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_3_MAX)) {
          	mceData.setWimcessMax3(MCESS_PROP_3_MAX);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_4_MAX)) {
          	mceData.setWimcessMax4(MCESS_PROP_4_MAX);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_5_MAX)) {
          	mceData.setWimcessMax5(MCESS_PROP_5_MAX);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_6_MAX)) {
          	mceData.setWimcessMax6(MCESS_PROP_6_MAX);
          }
          if (!StringHelper.isEmpty(MCESS_PROP_7_MAX)) {
          	mceData.setWimcessMax7(MCESS_PROP_7_MAX);
          }   
       }
   	    //build complete  
       /*
       log("record_id :" + mceData.getWimcessId() + ":");
       log("mcessName :" + mceData.getWimcessName() + ":");
       log("mcessDesc :" + mceData.getWimcessDesc()+ ":");
       log("mcessType :" + mceData.getWimcessType() + ":");
       log("searchSet1 :" + mceData.getWimcessSub1Id() + ":");
       log("searchSet2 :" + mceData.getWimcessSub2Id() + ":");
       log("searchSet3 :" + mceData.getWimcessSub3Id() + ":");
       log("searchSet4 :" + mceData.getWimcessSub4Id() + ":");
       log("searchSet5 :" + mceData.getWimcessSub5Id() + ":");
       log("mcessHierNode :" + mceData.getWimcessHiernode() + ":");
       log("mcessLocList :" + mceData.getWimcessLoclist()+ ":");
       log("clientTypId :" + mceData.getClnttypId() + ":");
       log("mcessProp1 :" + mceData.getWimcessProp1Id() + ":");
       log("mcessProp1Min :" + mceData.getWimcessMin1() + ":");
       log("mcessProp1Max :" + mceData.getWimcessMax1() + ":");
       log("mcessProp2 :" + mceData.getWimcessProp2Id() + ":");
       log("mcessProp2Min :" + mceData.getWimcessMin2() + ":");
       log("mcessProp2Max :" + mceData.getWimcessMax2() + ":");
       log("mcessProp3 :" + mceData.getWimcessProp3Id() + ":");
       log("mcessProp3Min :" + mceData.getWimcessMin3() + ":");
       log("mcessProp3Max :" + mceData.getWimcessMax3() + ":");
       log("mcessProp4 :" + mceData.getWimcessProp4Id() + ":");
       log("mcessProp4Min :" + mceData.getWimcessMin4() + ":");
       log("mcessProp4Max :" + mceData.getWimcessMax4() + ":");
       log("mcessProp5 :" + mceData.getWimcessProp5Id() + ":");
       log("mcessProp5Min :" + mceData.getWimcessMin5() + ":");
       log("mcessProp5Max :" + mceData.getWimcessMax5() + ":");
       log("mcessProp6 :" + mceData.getWimcessProp6Id() + ":");
       log("mcessProp6Min :" + mceData.getWimcessMin6()+ ":");
       log("mcessProp6Max :" + mceData.getWimcessMax6() + ":");
       log("mcessProp7 :" + mceData.getWimcessProp7Id() + ":");
       log("mcessProp7Min :" + mceData.getWimcessMin7() + ":");
       log("mcessProp7Max :" + mceData.getWimcessMax7() + ":"); 
       */
    }
	
	/**
     * Inserts mce search set data into table WBIAG_MCE_SRCHSET
     * @param null
     * @return record id
     * @throws Exception
     */
    public int processInsert() throws Exception{
       if (mceData.getWimcessName() == null || "".equals(mceData.getWimcessName()))
          throw new RuntimeException ("Search set name can't be null");
       mceAccess = new MCESearchSetAccess(conn);
       mceAccess.insertMCESearchSetData(mceData);
       return mceData.getWimcessId();
    }
    
    /**
     * Updates mce search set data in table WBIAG_MCE_SRCHSET
     * @param null
     * @return record id
     * @throws Exception
     */
    public int processUpdate() throws Exception{
       mceAccess = new MCESearchSetAccess(conn);
       mceAccess.updateMCESearchSetData(mceData);
       return mceData.getWimcessId();
     }
    
    /**
     * Deletes mce search set data from table WBIAG_MCE_SRCHSET
     * @param record_id
     * @return boolean
     * @throws Exception
     */
    public boolean processDelete(int recordId ) throws Exception{
       mceAccess = new MCESearchSetAccess(conn);
       mceAccess.deleteByMcessId(recordId);
       return true;
    }
    
    /**
     * Returns comma separated list of skd group ids
     * @param null
     * @return String
     * @throws Exception
     */
    public String processSearch() throws Exception{
       int searchSetType = mceData.getWimcessType().intValue();
       if (searchSetType == 1) {
          return StringHelper.getList(processType1Search(), ",");  
       }
       else if (searchSetType == 2) {
          return StringHelper.getList(processType2Search(this.mceData), ",");
       }
       return "";
     }
    
    /**
     * Returns list of skd group ids
     * @param null
     * @return List
     * @throws Exception
     */
    protected List processType1Search() throws Exception{
       List schedGrpIdList = new ArrayList();
       MCESearchSetData MCEdata = null;
       mceAccess = new MCESearchSetAccess(conn);
       if ( mceData.getWimcessSub1Id() != null ) {
          MCEdata = mceAccess.loadByMcessId(mceData.getWimcessSub1Id().intValue());
       	  schedGrpIdList = mergeList(schedGrpIdList,processType2Search(MCEdata));
       }
       if ( mceData.getWimcessSub2Id() != null ) {
          MCEdata = mceAccess.loadByMcessId(mceData.getWimcessSub2Id().intValue());
       	  schedGrpIdList = mergeList(schedGrpIdList,processType2Search(MCEdata));
       }
       if ( mceData.getWimcessSub3Id() != null ) {
          MCEdata = mceAccess.loadByMcessId(mceData.getWimcessSub3Id().intValue());
       	  schedGrpIdList = mergeList(schedGrpIdList,processType2Search(MCEdata));
       }
       if ( mceData.getWimcessSub4Id() != null ) {
          MCEdata = mceAccess.loadByMcessId(mceData.getWimcessSub4Id().intValue());
     	  schedGrpIdList = mergeList(schedGrpIdList,processType2Search(MCEdata));
       }
       if ( mceData.getWimcessSub5Id() != null ) {
          MCEdata = mceAccess.loadByMcessId(mceData.getWimcessSub5Id().intValue());
   	      schedGrpIdList = mergeList(schedGrpIdList,processType2Search(MCEdata));
       }
       return schedGrpIdList;
    }
    
    /**
     * Merging two List lists. It will add objects from fromList to toList if this object 
     * is not already in toList.
     * @param List toList, List fromList
     * @return List of unique elements
     * @throws Exception
     */
    protected List mergeList( List toList, List fromList) throws Exception{
       for (int i = 0; i < fromList.size(); i++) {
          Object skdId = fromList.get(i);
    	  if ( !toList.contains( skdId )) {
    	     toList.add(skdId);
    	  }
       }
       return toList;
    }
    
    /**
     * Returns List of skd group ids based of type2 search
     * @param MCESearchSetData MCEdata
     * @return List 
     * @throws Exception
     */
    protected List processType2Search(MCESearchSetData MCEdata) throws Exception{
       String hierNode = "";
       String specificLocList = "";
       String deptType = "";
       String propId = "";
       String propMin = "";
       String propMax = "";
       String propClass = "";
       PropertyData propertyData = null;
       List propertyList = new ArrayList();
       
       if (MCEdata.getWimcessHiernode() != null) {
          hierNode = MCEdata.getWimcessHiernode().toString();	
       }
       if (MCEdata.getWimcessLoclist() != null) {
          specificLocList = MCEdata.getWimcessLoclist();	
       }
       if (MCEdata.getClnttypId() != null) {
          deptType = MCEdata.getClnttypId().toString();	
       }
       if (MCEdata.getWimcessProp1Id() != null) {
          propId = MCEdata.getWimcessProp1Id().toString();
          propMin = MCEdata.getWimcessMin1();
          propMax = MCEdata.getWimcessMax1();
          propClass = getPropClass(propId);
          propertyData = new PropertyData( propId, propMin, propMax, propClass );
          propertyList.add(propertyData);
       }
       if (MCEdata.getWimcessProp2Id() != null) {
          propId = MCEdata.getWimcessProp2Id().toString();
          propMin = MCEdata.getWimcessMin2();
          propMax = MCEdata.getWimcessMax2();
          propClass = getPropClass(propId);
          propertyData = new PropertyData( propId, propMin, propMax, propClass );
          propertyList.add(propertyData);
       }
       if (MCEdata.getWimcessProp3Id() != null) {
          propId = MCEdata.getWimcessProp3Id().toString();
          propMin = MCEdata.getWimcessMin3();
          propMax = MCEdata.getWimcessMax3();
          propClass = getPropClass(propId);
          propertyData = new PropertyData( propId, propMin, propMax, propClass );
          propertyList.add(propertyData);
       }
       if (MCEdata.getWimcessProp4Id() != null) {
          propId = MCEdata.getWimcessProp4Id().toString();
          propMin = MCEdata.getWimcessMin4();
          propMax = MCEdata.getWimcessMax4();
          propClass = getPropClass(propId);
          propertyData = new PropertyData( propId, propMin, propMax, propClass );
          propertyList.add(propertyData);
       }
       if (MCEdata.getWimcessProp5Id() != null) {
          propId = MCEdata.getWimcessProp5Id().toString();
          propMin = MCEdata.getWimcessMin5();
          propMax = MCEdata.getWimcessMax5();
          propClass = getPropClass(propId);
          propertyData = new PropertyData( propId, propMin, propMax, propClass );
          propertyList.add(propertyData);
       }
       if (MCEdata.getWimcessProp6Id() != null) {
          propId = MCEdata.getWimcessProp6Id().toString();
          propMin = MCEdata.getWimcessMin6();
          propMax = MCEdata.getWimcessMax6();
          propClass = getPropClass(propId);
          propertyData = new PropertyData( propId, propMin, propMax, propClass );
          propertyList.add(propertyData);
       }
       if (MCEdata.getWimcessProp7Id() != null) {
          propId = MCEdata.getWimcessProp7Id().toString();
          propMin = MCEdata.getWimcessMin7();
          propMax = MCEdata.getWimcessMax7();
          propClass = getPropClass(propId);
          propertyData = new PropertyData( propId, propMin, propMax, propClass );
          propertyList.add(propertyData);
       }
       return searchForLocation( hierNode, specificLocList, deptType, propertyList);
    }
    
    /**
     * Returns List of skd group ids based of type2 search
     * @param String hierMode
     * @param String specificLocList
     * @param String deptType
     * @param List propertyList
     * @return List 
     * @throws Exception
     */
    protected List searchForLocation(String hierMode, String specificLocList, String deptType, List propertyList) throws Exception{
       List locList = new ArrayList();
       List schedGrpIdList = new ArrayList();;
       String locListStr = "";
       PreparedStatement ps = null;
       ResultSet rs = null;
       
       //specific location is specified
       if (!"".equals(hierMode)) {
          ScheduleGroupAccess sga = new ScheduleGroupAccess(conn);
          List childrenOfNode = sga.getDecendentsOf(new Integer(hierMode));
          for (int i = 0; i < childrenOfNode.size(); i++) {
             Integer skdGrpId = (Integer) childrenOfNode.get(i);
             if( skdGrpId.intValue() != STORE_INTERNAL_TYPE ) {
             	locList.add(skdGrpId.toString());	
             }
          }
          locListStr = StringHelper.getList(locList, ",");
       }
       else {
       	  //specific location is specified
          locListStr = specificLocList;
       }
       
       String sql = "select distinct sg.skdgrp_id " +
                    "from so_schedule_group sg, workbrain_team wt, so_client_type ct " +
	                "where sg.wbt_id = wt.wbt_id " +
	                "and sg.clnttyp_id = ct.clnttyp_id ";
       String propertySql = "";
       
       if(!"".equals(locListStr)){
          sql += "and sg.skdgrp_id in (" + locListStr + ") ";
       }
       
       if(!"".equals(deptType)){
          sql += "and sg.clnttyp_id in (" + deptType + ") ";
       }
       
       //property search
       for (int i = 0; i < propertyList.size(); i++) {
          propertySql = "and sg.skdgrp_id in ( " +
	                         "select sg.skdgrp_id " +
                             "from so_schedule_group sg, so_location_prop lp, so_property p " +
                             "where sg.skdgrp_id = lp.skdgrp_id " +
                             "and lp.prop_id = p.prop_id ";
          PropertyData propertyData = (PropertyData) propertyList.get(i);	
          //log(propertyData.getPropId() + ":" + propertyData.getPropClass());
          String propId = propertyData.getPropId();
          String propClass = propertyData.getPropClass();
          String min = propertyData.getPropMin();
          String max = propertyData.getPropMax();
          	
          if (STRING_CLASS.equalsIgnoreCase(propClass) || 
             BOOLEAN_CLASS.equalsIgnoreCase(propClass) ){
          	 propertySql += "and (lp.prop_id = " + propId + " and lp.locprop_value = '" + min + "' )) ";  	
          }
          else if (INTEGER_CLASS.equalsIgnoreCase(propClass)) {
             if ( max != null && !"".equals(max) ) {
          	    propertySql += "and (lp.prop_id = " + propId + " and lp.locprop_value  between " + min + " and " + max + " )) ";	
          	 }
          	 else {
          	    propertySql += "and (lp.prop_id = " + propId + " and lp.locprop_value = " + min + " )) ";	
          	 }
          }
          else {
             //Don't filter anything for any other prop_classes. Only String, Integer, Boolean are allowed.
             //This is being validated in the front end form as well
          	 propertySql += " )";	
          }
          	 sql += propertySql;
       }
       //log("sql :" + sql);
       try {
          ps = conn.prepareStatement(sql);
          rs = ps.executeQuery();
  	      while (rs.next()) {
  	         schedGrpIdList.add(rs.getString(1));  
          }
       }
       finally {
          if (rs != null) rs.close();
          if (ps != null) ps.close();
       }
       return schedGrpIdList;
    }
    
    /**
     * Returns prop_class value for a property
     * @param String propId
     * @return String 
     * @throws Exception
     */
    protected String getPropClass(String propId) throws Exception {
    	PreparedStatement ps = null;
        ResultSet rs = null;
        String propertySql = "select prop_class from so_property where prop_id = ?";
        String propertyClass = "";
        try {
           ps = conn.prepareStatement(propertySql);
           ps.setInt(1, Integer.parseInt(propId));
           rs = ps.executeQuery();
      	   while (rs.next()) {
      	      propertyClass = rs.getString(1);
           }
         }
         finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
         }
    	return propertyClass;
    }
    
    /**
     * @param null
     * @return boolean
     * @throws Exception
     */
    public boolean isDeptSearch() throws Exception{
       if (mceData.getClnttypId()!= null ) {
          return true;  	
       }
       return false;
    }
    
    /**
     * Returns the client type id
     * @param null
     * @return Integer
     * @throws Exception
     */
    public Integer getClnttypId() throws Exception{ 
       return mceData.getClnttypId();
    }
    
    //calling this method in the jsp since unable to use the api. 
    //The db connection is being lost if api is used in jsp
    public String getClnttypName(Integer clientTypId) throws Exception{ 
    	PreparedStatement ps = null;
        ResultSet rs = null;
        String clientTypSql = "SELECT CLNTTYP_NAME FROM SO_CLIENT_TYPE WHERE CLNTTYP_ID = ?";
        String clientTypName = ""; 
        try {
           ps = conn.prepareStatement(clientTypSql);
           ps.setInt(1, clientTypId.intValue());
           rs = ps.executeQuery();
     	   while (rs.next()) {
     	      clientTypName += rs.getString(1);
           }
        }
        finally {
           if (rs != null) rs.close();
           if (ps != null) ps.close();
        }
   	   return clientTypName;  	   
    }
    
    /**
     * calling this method in jsp for property validation
     * @param null
     * @return String
     * @throws Exception
     */
    public String getSoPropertyString() throws SQLException{
       Statement stmt = null;
       ResultSet rs = null;
       String propertySql = "select prop_id, prop_class from so_property ";
       String propertyString = "";
    	 
       try {
          stmt = conn.createStatement(); 
    	  rs = stmt.executeQuery(propertySql);
    	  while (rs.next()) {
    	     propertyString += rs.getString(1);
    		 propertyString += "=";
    		 propertyString += rs.getString(2);
    		 propertyString += "~";  
          }
       }
       finally {
          if (rs != null) rs.close();
          if (stmt != null) stmt.close();
       }
       return propertyString;
     }
    
    private void log( String msg ) {
       //System.out.println(msg);
       if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug( msg );}
    }
    
    /**
     * PropertyData is a helper class to store the property search detail
     * 
     * @author Neshan Kumar
     *  
     */
	class PropertyData {
	   private String propId;
	   private String propMin;
	   private String propMax;
	   private String propClass;
	        
	   public PropertyData( String propId, String propMin, String propMax, String propClass) {
	      this.propId = propId;
	      this.propMin = propMin;
	      this.propMax = propMax;
	      this.propClass = propClass;
	   }
	   private String getPropId() {
          return propId;
	   }
       private String getPropMin() {
          return propMin;
	   }
       private String getPropMax() {
          return propMax;
	   }
	   private String getPropClass() {
          return propClass;
	   }
	}
}