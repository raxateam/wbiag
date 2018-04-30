package com.wbiag.app.modules.retailSchedule.db;


	import com.wbiag.app.modules.retailSchedule.model.MCESearchSetData;
	import com.workbrain.sql.DBConnection;
	import com.workbrain.app.ta.db.RecordAccess;
	import java.sql.*;
	import java.util.ArrayList;
	import java.util.List;

	public class MCESearchSetAccess extends RecordAccess 
	{
	    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MCESearchSetAccess.class);
	    
	    private static final String TABLE = "WBIAG_MCE_SRCHSET";
	    private static final String PKEY	= "wimcess_id";
	    //private static final String NAME	= "wimcess_name";
	    //private static final String DESC	= "wimcess_desc";
	    private static final String TYPE	= "wimcess_type";
	    private static final String HIERNODE	= "wimcess_hiernode";
	    private static final String CLIENT	= "clnttyp_id";
	    /*private static final String SUBID1	= "wimcess_sub1_id";
	    private static final String SUBID2	= "wimcess_sub1_id";
	    private static final String SUBID3	= "wimcess_sub1_id";
	    private static final String SUBID4	= "wimcess_sub1_id";
	    private static final String SUBID5	= "wimcess_sub1_id";
	    private static final String PROPID1	= "wimcess_prop1_id";
	    private static final String MIN1	= "wimcess_min1";
	    private static final String MAX1	= "wimcess_max1";
	    private static final String PROPID2	= "wimcess_prop2_id";
	    private static final String MIN2	= "wimcess_min2";
	    private static final String MAX2	= "wimcess_max2";
	    private static final String PROPID3	= "wimcess_prop3_id";
	    private static final String MIN3	= "wimcess_min3";
	    private static final String MAX3	= "wimcess_max3";
	    private static final String PROPID4	= "wimcess_prop4_id";
	    private static final String MIN4	= "wimcess_min4";
	    private static final String MAX4	= "wimcess_max4";
	    private static final String PROPID5	= "wimcess_prop5_id";
	    private static final String MIN5 = "wimcess_min5";
	    private static final String MAX5	= "wimcess_max5";*/

	    private static final String SELECT_ALL_FOR_MCESSID_SQL = "select * from " + TABLE + " where " + PKEY + " = ?";

	    public final static String PRI_KEY_SEQ = "seq_wimcess_id";

	    	
	       	    
	        
		public MCESearchSetAccess(DBConnection c) {
			super(c);
		}
		
		//LOAD, return LIST
	    public List loadAll()throws SQLException {
	        return this.loadRecordData(new MCESearchSetData(),TABLE,"");
	    }
	    public List loadByMcessType(int mcess_type)throws SQLException {
	        return this.loadRecordData(new MCESearchSetData(),TABLE,TYPE, mcess_type);
	    }
	    public List loadByMcessHiernode(int mcess_hiernode)throws SQLException {
	        return this.loadRecordData(new MCESearchSetData(),TABLE,HIERNODE, mcess_hiernode);
	    }

	    public List loadByClnttypId(int clnttyp_id)throws SQLException {
	        return this.loadRecordData(new MCESearchSetData(),TABLE,CLIENT, clnttyp_id);
	    }
	    
	    //LOAD, return MCESearchSetData
	    public MCESearchSetData loadByMcessId(int mcess_id)throws SQLException {
	    	MCESearchSetData data= new MCESearchSetData();
	        return (MCESearchSetData) this.loadRecordDataByPrimaryKey(data,mcess_id);
	    }
	    
	    //UPDATE ALL Fields
	    public void updateMCESearchSetData(MCESearchSetData record) throws SQLException {
	        List recordList = new ArrayList();
	        recordList.add(record);	        	      
	        this.updateMCESearchSetData(recordList);
	    }
	    public void updateMCESearchSetData(List recordList) throws SQLException {
	    	String[] keyField = new String[1];
	        keyField[0] = PKEY;	        	        
	    	this.updateRecordData(recordList, TABLE,keyField,PKEY);
	    }

	    //INSERT
	    public void insertMCESearchSetData(MCESearchSetData record) throws SQLException {
	    	if (record.getWimcessId() == -1000) {
	            record.setWimcessId(getDBConnection().getDBSequence(
	                PRI_KEY_SEQ).getNextValue());
	        }
	    	this.insertRecordData(record, TABLE);
	    }
	    public void insertMCESearchSetData(List recordList) throws SQLException {
	        for(int i = 0; i < recordList.size(); i++){	        	
	        	this.insertMCESearchSetData((MCESearchSetData) recordList.get(i));	
	        }
	    }
	    	    
	    //DELETE
	    public void deleteByMcessId(int mcess_id) throws SQLException {
	        this.deleteRecordData(TABLE, PKEY, mcess_id);	       
	    }
    
	    
	    public String getPKSelectSQL(){
	       return SELECT_ALL_FOR_MCESSID_SQL;
	    }


	 
	    
	}