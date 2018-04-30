package com.wbiag.app.modules.retailSchedule.mce;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.db.DistributionAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.exceptions.SQLRetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.Distribution;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.db.EmployeeGroupAccess;
import com.workbrain.app.modules.retailSchedule.model.EmployeeGroup;
import com.workbrain.app.modules.retailSchedule.model.ForecastSpecialDay;
import com.workbrain.app.modules.retailSchedule.db.SpecialDayAccess;
import com.workbrain.app.modules.retailSchedule.model.ForecastSpecialDayDetail;
import com.workbrain.app.modules.retailSchedule.db.SpecialDayDetailAccess;
import com.workbrain.app.modules.retailSchedule.db.ScheduleGroupAccess;
import com.workbrain.app.modules.retailSchedule.type.DistributionType;
import com.workbrain.app.modules.retailSchedule.type.IntervalType;
import com.workbrain.app.modules.retailSchedule.model.HoursOfOperation;
import com.workbrain.app.modules.retailSchedule.db.HoursOfOperationAccess;
import com.workbrain.app.modules.retailSchedule.db.HrSopDayAccess;
import com.workbrain.app.modules.retailSchedule.model.HrSopDay;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntityHours;
import com.workbrain.app.modules.retailSchedule.db.CorporateEntityHourAccess;
import com.workbrain.app.modules.retailSchedule.utils.SODate;
import com.workbrain.app.modules.retailSchedule.utils.SODateInterval;
import com.workbrain.app.modules.retailSchedule.utils.SODaySet;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.StringHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.server.cache.WorkbrainCache;
import com.workbrain.server.data.*;
import com.workbrain.server.data.sql.QueryRowSource;
import java.sql.Timestamp;


public class MCEConfiguration {

	public static Logger logger = Logger.getLogger(MCEConfiguration.class);
	private static final String DELIMITER = ",";

	private static final String CHKSECTION_1_SELECTED = "chkSection1";
	private static final String CHKSECTION_2_SELECTED = "chkSection2";
	private static final String CHKSECTION_3_SELECTED = "chkSection3";
	private static final String CHKSECTION_4_SELECTED = "chkSection4";
	private static final String CHKSECTION_5_SELECTED = "chkSection5";
	private static final String CHKSECTION_6_SELECTED = "chkSection6";
	private static final String CHKSECTION_7_SELECTED = "chkSection7";
	private static final String FULL_TIME_STF_GROUP_NAME = "FULL TIME";
	private static final String PART_TIME_STF_GROUP_NAME = "PART TIME";
	private static final String MINOR_STF_GROUP_NAME = "MINOR";
	private static final String ACTION_TYPE = "actionType";
	private static final String CHK_HOUR_TYPE = "chkHourType";
	private static final int HOUR_SRC_TYPE_SAME_AS_PARENT = 2;
	private static final int HOUR_SRC_TYPE_DEFINED_SETS = 1;

	//AUDIT START
	public static final int UPDATE = 1;
	public static final int DELETE = 2;
	public static final int INSERT = 3;
	public static final String AUDIT_SQL = "INSERT INTO audit_log (audlog_id,wbu_name,wbu_name_actual,audlog_change_date,"
		+ " audlog_tablename, audlog_action,audlog_key_id, audlog_fieldname,audlog_old_value,audlog_new_value"
		+") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	protected String userName;
	protected String actualUserName;
	public List oldRows = new ArrayList();
	public List newRows = new ArrayList();
	//AUDIT END

	public MCEConfiguration(HttpServletRequest request) {    //AUDIT START
		this.userName = request.getParameter("userName");
		this.actualUserName = request.getParameter("actualUserName");
		if(StringHelper.isEmpty(this.userName)){
			this.userName = "auditGhost";
		}
		if(StringHelper.isEmpty(this.actualUserName)){
			this.actualUserName = "auditGhost";
		}

	}

	public MCEConfiguration(){

	}


	public String process(DBConnection conn, HttpServletRequest request, Object requestObj) throws Exception {
		String actName = (String)requestObj;
		String nextPage = null;

		if ("SubmitForm".equalsIgnoreCase(actName)) {



			//System.out.println( "CHKSECTION_1_SELECTED" + request.getParameter( CHKSECTION_1_SELECTED ) );
			String locListStr = request.getParameter( "locList" );
			StringHelper.detokenizeStringAsList(locListStr, DELIMITER);



			//nextPage=FSubmitForm(conn);
		} else if ("Init".equalsIgnoreCase(actName)) {
			//nextPage=AInit(conn);
		} else if ("Cancel".equalsIgnoreCase(actName)) {
			//nextPage = ACancel();
		}

		return nextPage;
	}	

	public void processHoursOfOperation( DBConnection conn, HttpServletRequest request ) throws Exception {

		List locList = StringHelper.detokenizeStringAsList( request.getParameter( "locList" ), DELIMITER );

		if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_1_SELECTED ) ) )  {

			updateDeptHrsSrcHrsOp (conn, locList, request);

		} else {

			if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_2_SELECTED ) ) )  {
				updateDeptHrsSrcHrsOp ( conn, locList, request );
			}

			if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_3_SELECTED ) ) )  {
				updateDeptHrsSrcHrsOp ( conn, locList, request );
			}

		}

	}

	public void deleteCorpEntHours (DBConnection conn,Integer locId, Date effDateStart, Date effDateEnd) {

		String deleteSql = 	"DELETE FROM SO_CORP_ENT_HOUR WHERE " +
		"CORPENTHR_FROMDATE = ? AND " +
		"CORPENTHR_TODATE = ? AND " +
		"SKDGRP_ID = ? ";

		PreparedStatement stmt = null;

		try {
			//AUDIT START
			StringBuffer select = new StringBuffer();			
			select.append("SELECT * FROM SO_CORP_ENT_HOUR WHERE CORPENTHR_FROMDATE = ");
			select.append(conn.encodeDate(effDateStart));				
			select.append(" AND CORPENTHR_TODATE = ");
			select.append(conn.encodeDate(effDateEnd));			
			select.append(" AND SKDGRP_ID = ");
			select.append(locId.intValue());									
			String tableName = "SO_CORP_ENT_HOUR";
			int KeyField = 0;
			QueryRowSource rs = new QueryRowSource(conn, select.toString(), KeyField); 
			RowCursor rc = rs.queryAll();
			while (rc.fetchRow()) {            	              
				this.addAuditEntries(conn,tableName, DELETE, (Row) new BasicRow(rc), null);
			}
			rc.close();			
			//AUDIT END
			stmt = conn.prepareStatement(deleteSql);		
			stmt.setTimestamp( 1, DateHelper.toTimestamp(effDateStart) );
			stmt.setTimestamp( 2, DateHelper.toTimestamp(effDateEnd) );
			stmt.setInt( 3, locId.intValue() );
			stmt.execute();	



		} catch( Exception e ) {
			try{
				conn.rollback();
			}
			catch(SQLException ex){
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);
			}
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} finally {
			SQLHelper.cleanUp(stmt);
		}	


	}

	public void updateCorpEntHours (DBConnection conn, List locList, HttpServletRequest request) { 

		Hashtable daysOfWeek = new Hashtable();
		daysOfWeek.put("1", "Sun");
		daysOfWeek.put("2", "Mon");
		daysOfWeek.put("3", "Tue");
		daysOfWeek.put("4", "Wed");
		daysOfWeek.put("5", "Thu");
		daysOfWeek.put("6", "Fri");
		daysOfWeek.put("7", "Sat");

		DBInterface.init(conn);
		CorporateEntityHourAccess cEHrAccess = new CorporateEntityHourAccess(conn);
		HoursOfOperationAccess hrsOpAccess = new HoursOfOperationAccess(conn);
		List cEHrsList = new ArrayList();

		Iterator locListItr = locList.iterator();

		try {

			boolean hoursNotEntered = true;
			int hrsOpSeq = conn.getDBSequence( HoursOfOperation.SO_HOURS_OF_OP_TABLE_PRI_KEY_SEQ ).getNextValue();
			HashMap dayMap = new HashMap();
			HrSopDayAccess dayAccess = new HrSopDayAccess(conn);
			List dayList = new ArrayList();

			//Creating HRSOP Set
			String timeStamp = DateHelper.convertDateString(new Date(), "MMddyyyy_HHmmss_SSS");

			HoursOfOperation hrsOp = new HoursOfOperation();
			hrsOp.assignIsNew(true);
			hrsOp.setHrsopId(new Integer(hrsOpSeq));
			if (!hoursNotEntered) hrsOp.setHrSopDays( dayMap );
			hrsOp.setHrsopDesc("Creating by Mass Config Editor");
			hrsOp.setHrsopName( timeStamp );
			hrsOp.setWbtId( new Integer(-1) ); //Workbrain Root
			hrsOpAccess.insert(hrsOp);    	

			//AUDIT START  do audit after insert
			this.auditInsert(conn,hrsOp.getTableName(),hrsOp.getPrimaryKey(), hrsOp.getID().intValue());
			//AUDIT END    			

			//Creating HRSOP Day
			for (int i=1; i<8; i++) {

				String fieldNameOpen = "txtEffDateHours" + (String) daysOfWeek.get(Integer.toString(i)) + "Open";
				String fieldNameClose = "txtEffDateHours" + (String) daysOfWeek.get(Integer.toString(i)) + "Close";

				String openTime = request.getParameter( fieldNameOpen );
				String closeTime = request.getParameter( fieldNameClose );

				if ( "".equals(openTime) && "".equals(closeTime) ) continue;

				hoursNotEntered = false;
				if ( "".equals(openTime) ) openTime = "0000";
				if ( "".equals(closeTime) ) closeTime = "0000";

				HrSopDay day = new HrSopDay();
				day.assignIsNew(true);
				day.setHrsopdDay(new Integer(i));
				day.setHrsopdOpenTime( DateHelper.convertStringToDate(openTime, "HHmm") );
				day.setHrsopdCloseTime( DateHelper.convertStringToDate(closeTime, "HHmm") );
				day.setHrsopId(new Integer( hrsOpSeq ) );
				day.setHrsopdId( new Integer (conn.getDBSequence( "SEQ_HRSOPD_ID" ).getNextValue() ));

				dayList.add(day);

				//AUDIT START  do audit after insert
				this.auditInsert(conn,day.getTableName(),day.getPrimaryKey(), day.getID().intValue());
				//AUDIT END 


			}

			dayAccess.batchInsert(dayList);

			Date effDateStart = DateHelper.convertStringToDate( request.getParameter( "txtEffDateStart" ), "yyyyMMdd HHmmss" );
			Date effDateEnd = DateHelper.convertStringToDate( request.getParameter( "txtEffDateEnd" ), "yyyyMMdd HHmmss" );

			while ( locListItr.hasNext() ) {    	

				Integer locId = new Integer ((String) locListItr.next());
				CorporateEntityHours cEHrs = new CorporateEntityHours();

				//Adjust surrounding Corp Ent Hour Set, if any, to avoid overlapping
				adjustSurroundingCorpEntHourSet(conn, locId, effDateStart, effDateEnd);

				cEHrs.assignIsNew(true);
				cEHrs.setSkdgrpId( locId );
				cEHrs.setHrsopId(new Integer(hrsOpSeq));
				cEHrs.setCorpenthrId( new Integer (conn.getDBSequence( CorporateEntityHours.SO_CORP_ENT_HOUR_TABLE_PRI_KEY_SEQ ).getNextValue()) );
				cEHrs.setCorpenthrFromdate( effDateStart );
				cEHrs.setCorpenthrTodate( effDateEnd );

				cEHrsList.add(cEHrs);

				//AUDIT START  do audit after insert
				this.auditInsert(conn,cEHrs.getTableName(),cEHrs.getPrimaryKey(), cEHrs.getID().intValue());
				//AUDIT END 

			}

			cEHrAccess.batchInsert(cEHrsList);

		} catch( Exception e ) {
			try{
				conn.rollback();
			}
			catch(SQLException ex){
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);
			}
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} 		

	}

	protected void adjustSurroundingCorpEntHourSet(DBConnection conn, Integer locId, Date effDateStart, Date effDateEnd) throws RetailException {

		try { 

			CorporateEntityHourAccess cEHrAccess = new CorporateEntityHourAccess(conn);
			List corpHrList = cEHrAccess.loadRecordData(       new CorporateEntityHours(), 
					CorporateEntityHours.SO_CORP_ENT_HOUR_TABLE,
					"SKDGRP_ID = " + locId.toString());

			Iterator corpHrItr = corpHrList.iterator();

			while (corpHrItr.hasNext()) {

				CorporateEntityHours cEHrs = (CorporateEntityHours) corpHrItr.next();            
				Date existingStartDate = cEHrs.getCorpenthrFromdate();
				Date existingEndDate = cEHrs.getCorpenthrTodate();

				if ( newHoursEntirelyCoversExistingHours(effDateStart, effDateEnd, existingStartDate, existingEndDate) ) {

					//Remove existing hour set
					cEHrAccess.delete(cEHrs);

				} else if ( newHoursIsWithinExistingHours(effDateStart, effDateEnd, existingStartDate, existingEndDate) ) {

					/* Need to configure hours set to the following configuration
					 * new hours        :                 |-----B-----|
					 * existing hours   :   |----A----|               |-----C----|      
					 * 
					 */

					//Configuring A
					cEHrs.setCorpenthrTodate(DateHelper.addDays(effDateStart,-1));  //Set existing end hours to new start hours
					cEHrAccess.update(cEHrs);

					//Creating C
					CorporateEntityHours cEHrsNew = new CorporateEntityHours(); 
					cEHrsNew.assignIsNew(true);
					cEHrsNew.setSkdgrpId( locId );
					cEHrsNew.setHrsopId(cEHrs.getHrsopId());
					cEHrsNew.setCorpenthrId( new Integer (conn.getDBSequence( CorporateEntityHours.SO_CORP_ENT_HOUR_TABLE_PRI_KEY_SEQ ).getNextValue()) );
					cEHrsNew.setCorpenthrFromdate( DateHelper.addDays(effDateEnd, 1) );
					cEHrsNew.setCorpenthrTodate( existingEndDate  );                      
					cEHrAccess.insert(cEHrsNew);

				} else if ( newEndHourOverlapExistingStartHour(effDateStart, effDateEnd, existingStartDate, existingEndDate) ) {

					cEHrs.setCorpenthrFromdate(DateHelper.addDays(effDateEnd, 1));
					cEHrAccess.update(cEHrs);

				} else if ( newStartHourOverlapExistingEndHour(effDateStart, effDateEnd, existingStartDate, existingEndDate) ) {

					cEHrs.setCorpenthrTodate(DateHelper.addDays(effDateStart,-1));
					cEHrAccess.update(cEHrs);

				}


			}

		} catch (SQLException ex) {

			if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);

		} 

	}    

	protected void XadjustSurroundingCorpEntHourSetX(DBConnection conn, Integer locId, Date effDateStart, Date effDateEnd) {

		PreparedStatement stmt = null;
		PreparedStatement delStmt = null;
		ResultSet rs = null;
		String hourSQL = "SELECT * FROM SO_CORP_ENT_HOUR WHERE SKDGRP_ID = ?";
		String deleteSQL = "delete FROM SO_CORP_ENT_HOUR WHERE CORPENTHR_ID = ?";

		try { 

			stmt = conn.prepareStatement(hourSQL);
			stmt.setInt(1, locId.intValue());
			rs = stmt.executeQuery();

			delStmt = conn.prepareStatement(deleteSQL);


			while (rs.next()) {

				Date existingStartDate = rs.getDate("CORPENTHR_FROMDATE");
				Date existingEndDate = rs.getDate("CORPENTHR_TODATE");

				if ( newHoursEntirelyCoversExistingHours(effDateStart, effDateEnd, existingStartDate, existingEndDate) ) {

					//Remove existing hours
					delStmt.setString(1, rs.getString("CORPENTHR_ID"));
					delStmt.execute();

				} else if ( newHoursIsWithinExistingHours(effDateStart, effDateEnd, existingStartDate, existingEndDate) ) {



				} else if ( newEndHourOverlapExistingStartHour(effDateStart, effDateEnd, existingStartDate, existingEndDate) ) {

				} else if ( newStartHourOverlapExistingEndHour(effDateStart, effDateEnd, existingStartDate, existingEndDate) ) {

				}


			}

		} catch (SQLException ex) {

			if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);

		} finally {
			SQLHelper.cleanUp(stmt, rs);
			SQLHelper.cleanUp(delStmt);
		}

	}

	/*
	 * new hours        :   |-------------------------------|
	 * existing hours   :                |------------|            
	 */      
	protected boolean newHoursEntirelyCoversExistingHours(Date newStartDate, Date newEndDate, Date existingStartDate, Date existingEndDate) {

		if ( DateHelper.isBetween(existingStartDate, newStartDate, newEndDate) 
				&& DateHelper.isBetween(existingEndDate, newStartDate, newEndDate)) {

			if (logger.isDebugEnabled()) logger.debug("New Hours / Existing Hours / ... " + newStartDate + ", " +  newEndDate + " / " + existingStartDate + ", " +  existingEndDate + " /  new Hours entirely covers existing hours." );

			return true;

		}

		return false;
	}

	/*
	 * new hours        :          |-------------|
	 * existing hours   :   |-----------------------|            
	 */        
	protected boolean newHoursIsWithinExistingHours(Date newStartDate, Date newEndDate, Date existingStartDate, Date existingEndDate) {

		if ( DateHelper.isBetween(newStartDate, existingStartDate, existingEndDate) 
				&& DateHelper.isBetween(newEndDate, existingStartDate, existingEndDate)) {

			if (logger.isDebugEnabled()) logger.debug("New Hours / Existing Hours / ... " + newStartDate + ", " +  newEndDate + " / " + existingStartDate + ", " +  existingEndDate + " /  new Hours is within existing hours." );

			return true;

		}

		return false;
	}    

	/*
	 * new hours        :   |-------------|
	 * existing hours   :               |------------|            
	 */    
	protected boolean newEndHourOverlapExistingStartHour(Date newStartDate, Date newEndDate, Date existingStartDate, Date existingEndDate) {

		if ( newStartDate.before(existingStartDate) && DateHelper.isBetween(newEndDate, existingStartDate, existingEndDate) ) {

			if (logger.isDebugEnabled()) logger.debug("New Hours / Existing Hours / ... " + newStartDate + ", " +  newEndDate + " / " + existingStartDate + ", " +  existingEndDate + " /  new end Hour overlaps existing start hour" );

			return true;

		}

		return false;
	}      

	/*
	 * new hours        :            |-------------|
	 * existing hours   :   |------------|            
	 */
	protected boolean newStartHourOverlapExistingEndHour(Date newStartDate, Date newEndDate, Date existingStartDate, Date existingEndDate) {

		if ( DateHelper.isBetween(newStartDate, existingStartDate, existingEndDate) && newEndDate.after(existingEndDate) ) {

			if (logger.isDebugEnabled()) logger.debug("New Hours / Existing Hours / ... " + newStartDate + ", " +  newEndDate + " / " + existingStartDate + ", " +  existingEndDate + " /  new start Hour overlaps existing end hour" );

			return true;

		}

		return false;
	}      



	public void updateDeptHrsSrcHrsOp (DBConnection conn, List locList, HttpServletRequest request) throws Exception {

		DBInterface.init(conn);
		ScheduleGroupAccess skdgrpAccess = new ScheduleGroupAccess(conn);  
		HoursOfOperationAccess hrsOpAccess = new HoursOfOperationAccess(conn);
		WorkbrainTeamAccess wbtAccess = new WorkbrainTeamAccess(conn);

		Iterator locListItr = locList.iterator();
		CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
		List skdgrpList = new ArrayList();

		Hashtable daysOfWeek = new Hashtable();
		daysOfWeek.put("1", "Sun");
		daysOfWeek.put("2", "Mon");
		daysOfWeek.put("3", "Tue");
		daysOfWeek.put("4", "Wed");
		daysOfWeek.put("5", "Thu");
		daysOfWeek.put("6", "Fri");
		daysOfWeek.put("7", "Sat");

		boolean chkHourTypeDefaultSets = false;

		if ( "false".equalsIgnoreCase( request.getParameter("dept_search") ) ) {

			chkHourTypeDefaultSets = true;

		} else if ("chkHourTypeDefaultSets".equalsIgnoreCase( request.getParameter( CHK_HOUR_TYPE ) ) ) {

			chkHourTypeDefaultSets = true;

		} else {

			chkHourTypeDefaultSets = false;

		}


		if ( "chkHourTypeInherit".equalsIgnoreCase( request.getParameter( CHK_HOUR_TYPE ) ) ) {

			String offSetStartStr = request.getParameter( "txtOffsetStart" );
			String offSetEndStr = request.getParameter( "txtOffsetEnd" );

			boolean startsEarly = "earlier".equalsIgnoreCase( request.getParameter( "ddlOffsetStart" ) );
			boolean endsEarly = "earlier".equalsIgnoreCase(  request.getParameter( "ddlOffsetEnd" ) );

			String stfOffSetStartStr = request.getParameter( "txtStaffOffsetStart" );
			String stfOffSetEndStr = request.getParameter( "txtStaffOffsetEnd" );

			Double offSetStart = ( !"".equals( offSetStartStr ) )?new Double(offSetStartStr):new Double(0);
			Double offSetEnd = ( !"".equals( offSetEndStr ) )?new Double(offSetEndStr):new Double(0);
			Double stfOffSetStart = ( !"".equals( stfOffSetStartStr ) )?new Double(stfOffSetStartStr):new Double(0);
			Double stfOffSetEnd = ( !"".equals( stfOffSetEndStr ) )?new Double(stfOffSetEndStr):new Double(0);



			while ( locListItr.hasNext() ) {    	

				Integer locId = new Integer ((String) locListItr.next());
				ScheduleGroupData skdgrpData = codeMapper.getScheduleGroupById(locId.intValue());



				skdgrpData.setSkdgrpHropstrOfs( (startsEarly)?-offSetStart.doubleValue():offSetStart.doubleValue() );
				skdgrpData.setSkdgrpHropendOfs( (endsEarly)?-offSetEnd.doubleValue():offSetEnd.doubleValue());
				skdgrpData.setSkdgrpStfstrOfs( -stfOffSetStart.doubleValue() );
				skdgrpData.setSkdgrpStfendOfs( stfOffSetEnd.doubleValue() );
				skdgrpData.setSkdgrpHrsOpSrc( HOUR_SRC_TYPE_SAME_AS_PARENT );

				skdgrpList.add(skdgrpData);


				//	AUDIT START  do audit before the update
				String tableName = skdgrpData.getTableName();
				QueryRowSource rs = this.getQueryRowSource(conn,tableName,skdgrpData.getPrimaryKey(), skdgrpData.getID().intValue(), false);
				RowCursor rc = rs.queryAll();	
				Row newRow = null;					
				this.newRows.clear();
				this.oldRows.clear();
				while (rc.fetchRow()) {
					oldRows.add(new BasicRow(rc));
					newRow = new BasicRow(rc);		             
					newRow.setValue("SKDGRP_HROPSTR_OFS", new Double(skdgrpData.getSkdgrpHropstrOfs()));
					newRow.setValue("SKDGRP_HROPEND_OFS", new Double(skdgrpData.getSkdgrpHropendOfs()));
					newRow.setValue("SKDGRP_STFSTR_OFS", new Double(skdgrpData.getSkdgrpStfstrOfs()));
					newRow.setValue("SKDGRP_STFEND_OFS", new Double (skdgrpData.getSkdgrpStfendOfs()));
					newRow.setValue("SKDGRP_HRS_OP_SRC", new Integer(skdgrpData.getSkdgrpHrsOpSrc()));
					newRows.add(newRow);                
				}
				rc.close();			
				for (int i = 0;i < oldRows.size();i++) {
					this.addAuditEntries(conn,tableName, UPDATE, (Row) oldRows.get(i), (Row) newRows.get(i));
				}
				//AUDIT END    			    			
			}

			skdgrpAccess.batchUpdate(skdgrpList);

		} else if ( chkHourTypeDefaultSets ) {

			updateStoreHrsOps(conn, locList, request, skdgrpAccess, hrsOpAccess, wbtAccess, locListItr, codeMapper, skdgrpList, daysOfWeek);   

		}



	}

	protected void updateStoreHrsOps(DBConnection conn, List locList, HttpServletRequest request, ScheduleGroupAccess skdgrpAccess, HoursOfOperationAccess hrsOpAccess, WorkbrainTeamAccess wbtAccess, Iterator locListItr, CodeMapper codeMapper, List skdgrpList, Hashtable daysOfWeek) throws SQLException, RetailException, SQLRetailException, AccessException, ParseException {
		boolean hoursNotEntered = true;
		int hrsOpSeq = conn.getDBSequence( HoursOfOperation.SO_HOURS_OF_OP_TABLE_PRI_KEY_SEQ ).getNextValue();
		HashMap dayMap = new HashMap();
		HrSopDayAccess dayAccess = new HrSopDayAccess(conn);
		List dayList = new ArrayList();

		//Creating HRSOP Set
		String timeStamp = DateHelper.convertDateString(new Date(), "MMddyyyy_HHmmss_SSS");

		if ("Y".equalsIgnoreCase( request.getParameter( CHKSECTION_2_SELECTED )) ) {

			HoursOfOperation hrsOp = new HoursOfOperation();
			hrsOp.assignIsNew(true);
			hrsOp.setHrsopId(new Integer(hrsOpSeq));
			if (!hoursNotEntered) hrsOp.setHrSopDays( dayMap );
			hrsOp.setHrsopDesc("Creating by Mass Config Editor");
			hrsOp.setHrsopName( timeStamp );
			hrsOp.setWbtId( new Integer(-1) ); //Workbrain Root			
			hrsOpAccess.insert(hrsOp);    	

			//AUDIT START  do audit after insert
			this.auditInsert(conn,hrsOp.getTableName(),hrsOp.getPrimaryKey(), hrsOp.getID().intValue());
			//AUDIT END    			




			//Creating HRSOP Day
			for (int i=1; i<8; i++) {

				String fieldNameOpen = "txtDefHours" + (String) daysOfWeek.get(Integer.toString(i)) + "Open";
				String fieldNameClose = "txtDefHours" + (String) daysOfWeek.get(Integer.toString(i)) + "Close";

				String openTime = request.getParameter( fieldNameOpen );
				String closeTime = request.getParameter( fieldNameClose );

				if ( "".equals(openTime) && "".equals(closeTime) ) continue;

				hoursNotEntered = false;
				if ( "".equals(openTime) ) openTime = "0000";
				if ( "".equals(closeTime) ) closeTime = "0000";

				HrSopDay day = new HrSopDay();
				day.assignIsNew(true);
				day.setHrsopdDay(new Integer(i));
				day.setHrsopdOpenTime( DateHelper.convertStringToDate(openTime, "HHmm") );
				day.setHrsopdCloseTime( DateHelper.convertStringToDate(closeTime, "HHmm") );
				day.setHrsopId(new Integer( hrsOpSeq ) );
				day.setHrsopdId( new Integer (conn.getDBSequence( "SEQ_HRSOPD_ID" ).getNextValue() ));

				dayList.add(day);
				//AUDIT START  do audit after insert
				this.auditInsert(conn,day.getTableName(),day.getPrimaryKey(), day.getID().intValue());
				//AUDIT END 	    	

			}

			dayAccess.batchInsert(dayList);


			while ( locListItr.hasNext() ) {    	

				Integer locId = new Integer ((String) locListItr.next());
				ScheduleGroupData skdgrpData = codeMapper.getScheduleGroupById(locId.intValue());    			
				skdgrpData.setSkdgrpHrsOpSrc( HOUR_SRC_TYPE_DEFINED_SETS );    			
				skdgrpList.add(skdgrpData);

				//	AUDIT START  do audit before the update
				String tableName = skdgrpData.getTableName();
				QueryRowSource rs = this.getQueryRowSource(conn,tableName,skdgrpData.getPrimaryKey(), skdgrpData.getID().intValue(), false);
				RowCursor rc = rs.queryAll();	
				Row newRow = null;					
				this.newRows.clear();
				this.oldRows.clear();
				while (rc.fetchRow()) {
					oldRows.add(new BasicRow(rc));
					newRow = new BasicRow(rc);		             
					newRow.setValue("SKDGRP_HRS_OP_SRC", new Integer(skdgrpData.getSkdgrpHrsOpSrc()));
					newRows.add(newRow);                
				}
				rc.close();			
				for (int i = 0;i < oldRows.size();i++) {
					this.addAuditEntries(conn,tableName, UPDATE, (Row) oldRows.get(i), (Row) newRows.get(i));
				}
				//AUDIT END  

				//Update Workbrain Team to use this HRSOP set
				NumberFormat number = NumberFormat.getInstance();

				WorkbrainTeamData wbtData = codeMapper.getWBTeamById( skdgrpData.getWbtId() );
				wbtData.setHrsopId( number.parse(Integer.toString(hrsOpSeq) ) );

				//	AUDIT START
				tableName = "WORKBRAIN_TEAM";
				rs = this.getQueryRowSource(conn,tableName,"WBT_ID", wbtData.getWbtId(), false);
				rc = rs.queryAll();	
				newRow = null;					
				this.newRows.clear();
				this.oldRows.clear();
				while (rc.fetchRow()) {
					oldRows.add(new BasicRow(rc));
					newRow = new BasicRow(rc);		             
					newRow.setValue("HRSOP_ID", wbtData.getHrsopId());
					newRows.add(newRow);                
				}
				rc.close();

				for (int i = 0;i < oldRows.size();i++) {
					this.addAuditEntries(conn,tableName, UPDATE, (Row) oldRows.get(i), (Row) newRows.get(i));
				}
				//AUDIT END
				wbtAccess.update(wbtData);

			}

			skdgrpAccess.batchUpdate(skdgrpList);

		}

		if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_3_SELECTED ) ) ) {

			if ( "chkAdd".equalsIgnoreCase( request.getParameter( "chkEffDate" ) ) ) { 

				updateCorpEntHours ( conn, locList, request );

			} else if ( "chkDelete".equalsIgnoreCase( request.getParameter( "chkEffDate" ) ) ) {

				Date effDateStart = DateHelper.convertStringToDate( request.getParameter( "txtEffDateStart" ), "yyyyMMdd HHmmss" );
				Date effDateEnd = DateHelper.convertStringToDate( request.getParameter( "txtEffDateEnd" ), "yyyyMMdd HHmmss" );

				Iterator locLstItr = locList.iterator();
				while ( locLstItr.hasNext() ) {   
					Integer locId = new Integer ((String) locLstItr.next());
					deleteCorpEntHours (conn, locId, effDateStart, effDateEnd );
				}
			}

		}
	}

	/*
    private boolean hoursNotEntered(HttpServletRequest request, String prefix) {

    	String close = "Close";
    	String open = "Open";

    }*/

	public void processSpecialDays(DBConnection conn, HttpServletRequest request) throws Exception {

		List locList = StringHelper.detokenizeStringAsList( request.getParameter( "locList" ), DELIMITER );

		if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_1_SELECTED ) ) && 
				"ADD".equalsIgnoreCase( request.getParameter( ACTION_TYPE ) ) )  {

			processSpecialDaysAdd(conn, locList, request);

		} else if ( "EDIT".equalsIgnoreCase( request.getParameter( ACTION_TYPE ) ) )  {

			processSpecialDaysEdit(conn, locList, request);

		}

	}

	public void processSpecialDaysAdd(DBConnection conn, List locList, HttpServletRequest request) throws Exception {

		String name = request.getParameter( "txtName" );
		String desc = request.getParameter( "txtDescr" );
		String dist = request.getParameter( "dblDistribution" );
		String ignoreRes = request.getParameter( "chkSkip" );
		String type = request.getParameter( "chkSpecDayType" );
		String adjPctString = request.getParameter( "txtPercent" );

		DBInterface.init(conn);

		Iterator locListItr = locList.iterator();

		while ( locListItr.hasNext() ) {    	

			Integer locId = new Integer ((String) locListItr.next());

			ForecastSpecialDay specDay = new ForecastSpecialDay();
			specDay.assignIsNew(true);

			int skipSpecDay = ("Y".equals(ignoreRes))?1:0;
			Double adjPct = (!"".equals(ignoreRes) && "3".equals(type))?new Double(adjPctString):new Double(0);

			int specDaySeq = conn.getDBSequence( ForecastSpecialDay.SPECIAL_DAY_PRI_KEY_SEQ ).getNextValue();
			specDay.setSpecdayId( specDaySeq );
			specDay.setSpecdayName( name );
			specDay.setSpecdayDesc( desc  );
			specDay.setSkdgrpId( locId.intValue() );
			if (!"".equals(dist)) specDay.setDistId( new Integer(dist) );
			specDay.setSpecdayIgnoreRes( skipSpecDay );
			specDay.setSpecdayType( Integer.parseInt( type ) );
			specDay.setSpecdayAdjpct( adjPct );

			SpecialDayAccess specDayAccess = new SpecialDayAccess(conn);
			specDayAccess.insert( specDay );
			//AUDIT START  do audit after insert
			this.auditInsert(conn,specDay.getTableName(),specDay.getPrimaryKey(), specDay.getID().intValue());
			//AUDIT END    			

			addSpecialDayDetail(conn, request, specDaySeq);


		}

	}

	private void addSpecialDayDetail(DBConnection conn, HttpServletRequest request, int specDayId) throws RetailException, SQLException, SQLRetailException, AccessException {
		SpecialDayDetailAccess specDayDetAcc = new SpecialDayDetailAccess(conn);
		List specDayDetailList = new ArrayList();


		//Adding Special Day Detail
		for (int i=1;i<6;i++) {

			String specDayDetString = request.getParameter( "txtNewDate"+i );

			if (!"".equals(specDayDetString)) {

				ForecastSpecialDayDetail specDayDet = new ForecastSpecialDayDetail(); 
				specDayDet.assignIsNew(true);

				Date specDayDate = DateHelper.parseDate(specDayDetString, "yyyyMMdd HHmmss");

				specDayDet.setSpecdetId( new Integer(conn.getDBSequence( ForecastSpecialDayDetail.SPECIAL_DAY_DETAIL_PRI_KEY_SEQ ).getNextValue()) );
				specDayDet.setSpecdayId( new Integer (specDayId) );
				specDayDet.setSpecdetDate( specDayDate );
				specDayDetailList.add(specDayDet);

				//AUDIT START  do audit after insert
				this.auditInsert(conn,specDayDet.getTableName(),specDayDet.getPrimaryKey(), specDayDet.getID().intValue());
				//AUDIT END 


			}

		}

		if (specDayDetailList.size()>0) specDayDetAcc.batchInsert(specDayDetailList);
	}

	public void deleteSpecialDay( DBConnection conn, HttpServletRequest request ) {

		Integer specDayCount = (!"".equals(request.getParameter( "specDayCount" )))?new Integer(request.getParameter( "specDayCount" )):new Integer(-1);

		String deleteDetailSql = 	"DELETE FROM SO_FCAST_SPEC_DET WHERE SPECDAY_ID " +
		"IN (SELECT SPECDAY_ID FROM SO_FCAST_SPEC_DAY WHERE SPECDAY_NAME = ?)";

		String deleteDaySql = "DELETE FROM SO_FCAST_SPEC_DAY WHERE SPECDAY_NAME = ?";    	

		PreparedStatement delSpecDayStmt = null;
		PreparedStatement delSpecDayDetStmt = null;

		try { 

			delSpecDayStmt = conn.prepareStatement(deleteDaySql);
			delSpecDayDetStmt = conn.prepareStatement(deleteDetailSql);


			for (int i=1; i<specDayCount.intValue() && specDayCount.intValue()!=-1 ; i++) {

				if ( "Y".equals( request.getParameter("chkSpecDay"+i) ) ) { 

					String specDay = request.getParameter("specDayName"+i);

					//AUDIT START
					StringBuffer select = new StringBuffer();			
					select.append("SELECT * FROM SO_FCAST_SPEC_DET WHERE SPECDAY_ID IN ");
					select.append(" (SELECT SPECDAY_ID FROM SO_FCAST_SPEC_DAY WHERE SPECDAY_NAME = ");		
					select.append(conn.encode(specDay));
					select.append(")");    
					String tableName = "SO_FCAST_SPEC_DET";
					int KeyField = 0;
					QueryRowSource rs = new QueryRowSource(conn, select.toString(), KeyField); 
					RowCursor rc = rs.queryAll();
					while (rc.fetchRow()) {            	              
						this.addAuditEntries(conn,tableName, DELETE, (Row) new BasicRow(rc), null);
					}
					rc.close();					

					StringBuffer select2 = new StringBuffer();			
					select2.append("SELECT * FROM SO_FCAST_SPEC_DAY WHERE SPECDAY_NAME = ");
					select2.append(conn.encode(specDay));				
					tableName = "SO_FCAST_SPEC_DAY";
					KeyField = 0;
					rs = new QueryRowSource(conn, select.toString(), KeyField); 
					rc = rs.queryAll();
					while (rc.fetchRow()) {            	              
						this.addAuditEntries(conn,tableName, DELETE, (Row) new BasicRow(rc), null);
					}
					rc.close();					
					//AUDIT END

					delSpecDayDetStmt.clearParameters();
					delSpecDayStmt.clearParameters();

					delSpecDayDetStmt.setString( 1, specDay );
					delSpecDayDetStmt.execute();

					delSpecDayStmt.setString( 1, specDay );
					delSpecDayStmt.execute();
				}

			}


		} catch( Exception e ) {
			try{
				conn.rollback();
			}
			catch(SQLException ex){
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);
			}
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} finally {
			SQLHelper.cleanUp(delSpecDayStmt);
			SQLHelper.cleanUp(delSpecDayDetStmt);
		}	    	


	}

	public void deleteSpecialDayDetail( DBConnection conn, Integer specDayId, Date specDateDetailToDelete ) {

		String deleteSql = 	"DELETE FROM SO_FCAST_SPEC_DET " +
		"WHERE SPECDAY_ID = ? " +  
		"AND SPECDET_DATE  = ?";


		PreparedStatement stmt = null;

		try { 

			//AUDIT START
			StringBuffer select = new StringBuffer();			
			select.append("SELECT * FROM SO_FCAST_SPEC_DET WHERE SPECDAY_ID = "); 
			select.append(specDayId.intValue());
			select.append(" AND SPECDET_DATE  = ");
			select.append(conn.encodeDate(specDateDetailToDelete));								
			String tableName = "SO_FCAST_SPEC_DET";
			int KeyField = 0;
			QueryRowSource rs = new QueryRowSource(conn, select.toString(), KeyField); 
			RowCursor rc = rs.queryAll();
			while (rc.fetchRow()) {            	              
				this.addAuditEntries(conn,tableName, DELETE, (Row) new BasicRow(rc), null);
			}
			rc.close();
			//AUDIT END

			stmt = conn.prepareStatement(deleteSql);		
			stmt.setInt( 1, specDayId.intValue() );
			stmt.setTimestamp( 2, DateHelper.toTimestamp(specDateDetailToDelete) );
			stmt.execute();


		} catch( Exception e ) {
			try{
				conn.rollback();
			}
			catch(SQLException ex){
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);
			}
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} finally {
			SQLHelper.cleanUp(stmt);
		}	    	

	}



	public void processSpecialDaysEdit(DBConnection conn, List locList, HttpServletRequest request) throws Exception {

		SpecialDayAccess specDayAccess = new SpecialDayAccess(conn);
		String nameOfSpecDayToChange = request.getParameter( "specDayNameIn" );

		SpecialDayDetailAccess specDayDetAcc = new SpecialDayDetailAccess(conn);

		String name = request.getParameter( "txtName" );
		String desc = request.getParameter( "txtDescr" );
		String dist = request.getParameter( "dblDistribution" );
		String ignoreRes = request.getParameter( "chkSkip" );
		String type = request.getParameter( "chkSpecDayType" );
		String adjPctString = request.getParameter( "txtPercent" );


		DBInterface.init(conn);

		Iterator locListItr = locList.iterator();

		//For each Location
		while ( locListItr.hasNext() ) {    	

			Integer locId = new Integer ((String) locListItr.next());

			String whereCondition = "SPECDAY_NAME = '" + nameOfSpecDayToChange + "' AND SKDGRP_ID = " + locId.toString();
			List specDayList = specDayAccess.loadRecordData(new ForecastSpecialDay(), ForecastSpecialDay.SPECIAL_DAY_TABLE, whereCondition );

			Iterator specDayItr = specDayList.iterator();
			List specDayUpdateList = new ArrayList();


			//For each Special Day
			while ( specDayItr.hasNext() ) {

				boolean toUpdate = false;

				ForecastSpecialDay specDay = (ForecastSpecialDay) specDayItr.next();

				int skipSpecDay = ("Y".equals(ignoreRes))?1:0;
				Double adjPct = (!"".equals(ignoreRes) && "3".equals(type))?new Double(adjPctString):new Double(0);

				if ("Y".equalsIgnoreCase( request.getParameter( CHKSECTION_1_SELECTED ) ) ) {
					specDay.setSpecdayName( name );
					specDay.setSpecdayDesc( desc  );
					if (!"".equals(dist)) specDay.setDistId( new Integer(dist) );
					toUpdate = true;
				}

				if ("Y".equalsIgnoreCase( request.getParameter( CHKSECTION_2_SELECTED ) ) ) {
					specDay.setSpecdayIgnoreRes( skipSpecDay );
					toUpdate = true;
				}

				if ("Y".equalsIgnoreCase( request.getParameter( CHKSECTION_3_SELECTED ) ) ) {
					if (!"".equals(type) && null!=type) specDay.setSpecdayType( Integer.parseInt( type ) );
					specDay.setSpecdayAdjpct( adjPct );
					toUpdate = true;
				}                

				if (toUpdate) {
					specDayUpdateList.add(specDay);
					specDay.setSkdgrpId( locId.intValue() );
				}

				//	AUDIT START  do audit before the batchUpdate
				String tableName = specDay.getTableName();
				QueryRowSource rs = this.getQueryRowSource(conn,tableName,specDay.getPrimaryKey(), specDay.getID().intValue(), false);
				RowCursor rc = rs.queryAll();	
				Row newRow = null;					
				this.newRows.clear();
				this.oldRows.clear();
				while (rc.fetchRow()) {
					oldRows.add(new BasicRow(rc));
					newRow = new BasicRow(rc);		             
					newRow.setValue("SPECDAY_NAME", specDay.getSpecdayName());
					newRow.setValue("SPECDAY_DESC", specDay.getSpecdayDesc());
					newRow.setValue("SKDGRP_ID", new Integer(specDay.getSkdgrpId()));
					newRow.setValue("DIST_ID", specDay.getDistId());
					newRow.setValue("SPECDAY_IGNORE_RES", new Integer(specDay.getSpecdayIgnoreRes()));
					newRow.setValue("SPECDAY_TYPE", new Integer(specDay.getSpecdayType()));
					newRow.setValue("SPECDAY_ADJPCT", specDay.getSpecdayAdjpct());			             		             
					newRows.add(newRow);                
				}
				rc.close();			
				for (int i = 0;i < oldRows.size();i++) {
					this.addAuditEntries(conn,tableName, UPDATE, (Row) oldRows.get(i), (Row) newRows.get(i));
				}
				//AUDIT END    

				//Delete Special Day Occurrence
				int specDayId = specDay.getSpecdayId();

				Integer specDayOccurToDeleteCount = (!"".equals(request.getParameter( "specDayOccurCount" )))?new Integer(request.getParameter( "specDayOccurCount" )):new Integer(-1);

				for (int i=1; i<specDayOccurToDeleteCount.intValue() && specDayOccurToDeleteCount.intValue()!=-1 ; i++) {

					if ( "Y".equals(request.getParameter("chkDate"+i)) ) {
						String specDayDeleteString = request.getParameter( "dateToDelete"+i );
						Date specDayDateToDelete = DateHelper.parseDate(specDayDeleteString, "yyyyMMdd HHmmss");
						deleteSpecialDayDetail( conn, new Integer(specDayId), specDayDateToDelete );
					}

				}			    	


				addSpecialDayDetail(conn, request, specDay.getSpecdayId());

			}

			if (specDayUpdateList.size()>0) specDayAccess.batchUpdate(specDayUpdateList);





		}


	}	



	public void processBasicConfig(DBConnection conn, HttpServletRequest request) throws Exception {

		List locList = StringHelper.detokenizeStringAsList( request.getParameter( "locList" ), DELIMITER );

		if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_1_SELECTED ) ) ) {

			updateHideLocationFromAllUsers(conn, locList, request.getParameter( "chkHideForAllUsers" ) );

		} 

		if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_2_SELECTED ) ) ) {

			updateHideLocationFromNonAdminUsers( conn, locList, request.getParameter( "chkHideForNonAdminUsers" ) );

		} 

		if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_3_SELECTED ) ) ) {

			updateStoreProperties( conn, locList, request );

		}

		if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_4_SELECTED ) ) ) {

			addVolumeDrivers( conn, locList, request );

		}


		if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_5_SELECTED ) ) ) {

			addStoreDept( conn, locList, request );

		}

		if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_6_SELECTED ) ) ) {

			addNewStaffGroup (conn, locList, request);

		}

		if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_7_SELECTED ) ) ) {


			if ( "Y".equalsIgnoreCase( request.getParameter( "chkDeleteFullTime" ) ) ) {

				deleteNewStaffGroup( conn, locList, FULL_TIME_STF_GROUP_NAME );

			}

			if ( "Y".equalsIgnoreCase( request.getParameter( "chkDeletePartTime" ) ) ) {

				deleteNewStaffGroup( conn, locList, PART_TIME_STF_GROUP_NAME );

			}

			if ( "Y".equalsIgnoreCase( request.getParameter( "chkDeleteMinor" ) ) ) {

				deleteNewStaffGroup( conn, locList, MINOR_STF_GROUP_NAME );

			}        	


		}        



	}	


	private void updateHideLocationFromAllUsers (DBConnection conn, List locList, String valueToUpdate) {

		String sql = "UPDATE WORKBRAIN_TEAM SET WBT_FLAG4 = ? WHERE WBT_ID = ?";

		Iterator locListItr = locList.iterator();
		PreparedStatement stmt = null;

		try { 

			stmt = conn.prepareStatement(sql);		
			while ( locListItr.hasNext() ) {



				Integer locId = new Integer ((String) locListItr.next());
				CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
				ScheduleGroupData skdgrpData = codeMapper.getScheduleGroupById(locId.intValue());

				stmt.setString( 1, valueToUpdate );
				stmt.setInt( 2, skdgrpData.getWbtId() );
				stmt.addBatch();

				//AUDIT START
				String tableName = "WORKBRAIN_TEAM";
				QueryRowSource rs = this.getQueryRowSource(conn,tableName, "WBT_ID", skdgrpData.getWbtId(), false);
				RowCursor rc = rs.queryAll();	
				this.newRows.clear();
				this.oldRows.clear();
				Row newRow = null;
				while (rc.fetchRow()) {
					oldRows.add(new BasicRow(rc));
					newRow = new BasicRow(rc);
					newRow.setValue("WBT_FLAG4", valueToUpdate);
					newRows.add(newRow);

				}
				rc.close();
				for (int i = 0;i < oldRows.size();i++) {
					this.addAuditEntries(conn,tableName, UPDATE, (Row) oldRows.get(i), (Row) newRows.get(i));
				}
				//AUDIT END

			}

			stmt.executeBatch();

		} catch( Exception e ) {
			try{
				conn.rollback();
			}
			catch(SQLException ex){
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);
			}
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} finally {
			SQLHelper.cleanUp(stmt);
		}
	}


	private void updateHideLocationFromNonAdminUsers (DBConnection conn, List locList, String valueToUpdate) {

		String sql = "UPDATE WORKBRAIN_TEAM SET WBT_FLAG5 = ? WHERE WBT_ID = ?";

		Iterator locListItr = locList.iterator();
		PreparedStatement stmt = null;

		try { 

			stmt = conn.prepareStatement(sql);		
			while ( locListItr.hasNext() ) {


				Integer locId = new Integer ((String) locListItr.next());
				CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
				ScheduleGroupData skdgrpData = codeMapper.getScheduleGroupById(locId.intValue());


				stmt.setString( 1, valueToUpdate );
				stmt.setInt( 2, skdgrpData.getWbtId() );
				stmt.addBatch();

				//AUDIT START
				String tableName = "WORKBRAIN_TEAM";
				QueryRowSource rs = this.getQueryRowSource(conn,tableName,"WBT_ID", skdgrpData.getWbtId(), false);			
				RowCursor rc = rs.queryAll();	
				this.newRows.clear();
				this.oldRows.clear();
				Row newRow = null;
				while (rc.fetchRow()) {
					oldRows.add(new BasicRow(rc));
					newRow = new BasicRow(rc);
					newRow.setValue("WBT_FLAG5", valueToUpdate);
					newRows.add(newRow);                
				}
				rc.close();
				for (int i = 0;i < oldRows.size();i++) {
					this.addAuditEntries(conn,tableName, UPDATE, (Row) oldRows.get(i), (Row) newRows.get(i));
				}
				//AUDIT END

			}

			stmt.executeBatch();

		} catch( Exception e ) {
			try{
				conn.rollback();
			}
			catch(SQLException ex){
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);
			}
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} finally {
			SQLHelper.cleanUp(stmt);
		}
	}    

	private void updateStoreProperties(DBConnection conn, List locList, HttpServletRequest request) {


		Integer propertyCount = new Integer( (String) request.getParameter("propCount") );

		for (int i=1; i<propertyCount.intValue(); i++) {

			Integer propId = new Integer( (String) request.getParameter( "propId" + i ));
			String propertyName = (String) request.getParameter( "propName" + i );

			if ( "Y".equalsIgnoreCase( request.getParameter( "chkChgPropVal" + i ) ) ) {

				assignStoreProperty( conn, locList, propertyName, propId.toString(), (String) request.getParameter( "txtPropVal"+i ) );

			} else if ( "Y".equalsIgnoreCase( request.getParameter( "chkDelProp" + i ) ) ) {

				deleteStoreProperty ( conn, locList, propId.toString() );

			}

		}

		//New Properties
		for (int i=1; i<3; i++) {

			String prodIdString = (String) request.getParameter( "dblNewPropertyVal" + i );
			if ( !"".equals(prodIdString) && null != prodIdString )  {

				Integer propId = new Integer ( prodIdString );
				String valueToUpdate = (String) request.getParameter( "txtNewPropVal" + i );
				String propertyName = (String) request.getParameter( "dblNewPropertyVal"+ i + "_label" );

				assignStoreProperty( conn, locList, propertyName, propId.toString(), valueToUpdate );

			}
		}



	}

	private void assignStoreProperty( DBConnection conn, List locList, String propertyName, String propId, String valueToUpdate ) {

		String updateSql = "UPDATE SO_LOCATION_PROP SET LOCPROP_VALUE = ? WHERE SKDGRP_ID = ? AND PROP_ID = ?";

		String insertSql = 	"INSERT INTO SO_LOCATION_PROP " +
		"(LOCPROP_ID, PROP_ID, SKDGRP_ID, LOCPROP_VALUE, LOCPROP_DESC ) " +
		"VALUES " +
		"(?, ?, ?, ?, ?)";

		Iterator locListItr = locList.iterator();
		PreparedStatement updateStmt = null;
		PreparedStatement insertStmt = null;

		try { 

			updateStmt = conn.prepareStatement(updateSql);		
			while ( locListItr.hasNext() ) {

				Integer locId = new Integer ((String) locListItr.next());

				updateStmt.setString( 1, valueToUpdate );
				updateStmt.setInt( 2, locId.intValue() );
				updateStmt.setInt( 3, Integer.parseInt( propId ) );

				//AUDIT START
				StringBuffer select = new StringBuffer();			
				select.append("SELECT * FROM SO_LOCATION_PROP WHERE SKDGRP_ID = ");
				select.append(locId.intValue());	
				select.append(" AND PROP_ID = ");
				select.append(propId);
				String tableName = "SO_LOCATION_PROP";
				int KeyField = 0;
				QueryRowSource rs = new QueryRowSource(conn, select.toString(), KeyField); 
				RowCursor rc = rs.queryAll();	
				this.newRows.clear();
				this.oldRows.clear();
				Row newRow = null;
				while (rc.fetchRow()) {
					oldRows.add(new BasicRow(rc));
					newRow = new BasicRow(rc);
					newRow.setValue("LOCPROP_VALUE", valueToUpdate);
					newRows.add(newRow);
				}
				rc.close();
				for (int i = 0;i < oldRows.size();i++) {
					this.addAuditEntries(conn,tableName, UPDATE, (Row) oldRows.get(i), (Row) newRows.get(i));
				}
				//AUDIT END

				int result = updateStmt.executeUpdate();

				if (logger.isDebugEnabled()) logger.debug("update " + valueToUpdate + " " + locId.intValue() + " " + propId );
				if (logger.isDebugEnabled()) logger.debug("updateStmt result: " + result);

				if ( result == 0 ) {

					insertStmt = null;
					insertStmt = conn.prepareStatement(insertSql);
					int KeyValue = conn.getDBSequence("seq_locprop_id").getNextValue(); //AUDIT change
					insertStmt.setInt(1, KeyValue);						
					insertStmt.setInt(2, Integer.parseInt(propId));
					insertStmt.setInt(3, locId.intValue());
					insertStmt.setString(4, valueToUpdate);
					insertStmt.setString(5, propertyName);						
					insertStmt.execute();


					//AUDIT START						
					this.auditInsert(conn,"SO_LOCATION_PROP","LOCPROP_ID", KeyValue);
					//AUDIT END						
					if (logger.isDebugEnabled()) logger.debug("insert " + propertyName + " " + valueToUpdate + " " + locId.intValue() + " " + propId );

				}

			}

		} catch( Exception e ) {
			try{
				conn.rollback();
			}
			catch(SQLException ex){
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);
			}
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} finally {
			SQLHelper.cleanUp(updateStmt);   
			SQLHelper.cleanUp(insertStmt);
		}		 

	}

	private void deleteStoreProperty ( DBConnection conn, List locList, String propIdToDelete ) {


		String sql = "DELETE FROM SO_LOCATION_PROP WHERE SKDGRP_ID = ? AND PROP_ID = ?";

		Iterator locListItr = locList.iterator();
		PreparedStatement stmt = null;

		try { 

			stmt = conn.prepareStatement(sql);		
			while ( locListItr.hasNext() ) {



				Integer locId = new Integer ((String) locListItr.next());

				//AUDIT START
				StringBuffer select = new StringBuffer();			
				select.append("SELECT * FROM SO_LOCATION_PROP WHERE SKDGRP_ID = ");
				select.append(locId.intValue());
				select.append(" AND PROP_ID = ");		
				select.append(conn.encode(propIdToDelete));				
				String tableName = "SO_LOCATION_PROP";
				int KeyField = 0;
				QueryRowSource rs = new QueryRowSource(conn, select.toString(), KeyField); 
				RowCursor rc = rs.queryAll();
				while (rc.fetchRow()) {            	              
					this.addAuditEntries(conn,tableName, DELETE, (Row) new BasicRow(rc), null);
				}
				rc.close();										
				//AUDIT END

				stmt.setInt( 1, locId.intValue() );
				stmt.setInt( 2, Integer.parseInt( propIdToDelete ) );
				stmt.addBatch();

			}

			stmt.executeBatch();

		} catch( Exception e ) {
			try{
				conn.rollback();
			}
			catch(SQLException ex){
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);
			}
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} finally {
			SQLHelper.cleanUp(stmt);
		}				 
	}

	public void addVolumeDrivers( DBConnection conn, List locList, HttpServletRequest request ) {

		Iterator locListItr = locList.iterator();
		String insertSql = 				"insert into WORKBRAIN.SO_SCHEDULE_GROUP " +
		"(SKDGRP_ID, SKDGRP_PARENT_ID, CLNTTYP_ID, " +
		"WBT_ID, SKDGRP_NAME, SKDGRP_CLIENTKEY, " +
		"SKDGRP_INTRNL_TYPE " +
		") VALUES ( " +
		"?, ?, 1, " +
		"?, ?, ?, " +
		"12)";

		PreparedStatement insertStmt = null;


		String volDriverIdString = (String) request.getParameter( "dblVolDriver" );

		if ( !"".equals(volDriverIdString) && null != volDriverIdString )  {

			try {
				Integer volDriverId = null;
				CorporateEntity oDriverCE = null;
				CorporateEntity parentCE = null;
				String newName = null;
				String oldPrefix = null;
				String newPrefix = null;
				int copySubTree = 0;
				HashMap newCEIdMap = new HashMap();
				HashMap oldCEIdMap = new HashMap(); 
				
				CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
				try {
					DBInterface.getCurrentConnection();
				} catch (Exception e){
					DBInterface.init(conn);
				}
				
				volDriverId = new Integer( volDriverIdString );
				oDriverCE = codeMapper.getScheduleGroupById(volDriverId.intValue()).getCorporateEntity();
				
				
				while (locListItr.hasNext()) {

					boolean success = true;
					
					Integer locId = new Integer ((String) locListItr.next());
					parentCE = codeMapper.getScheduleGroupById(locId.intValue()).getCorporateEntity();
					
					oldPrefix = oDriverCE.getSkdgrpData().getSkdgrpName().substring(0,8);
					newPrefix = parentCE.getSkdgrpData().getSkdgrpName().substring(0,8);
					
					newName = newPrefix + oDriverCE.getSkdgrpData().getSkdgrpName().substring(8);
					
					try {
						CorporateEntity newCE = oDriverCE.copyCE(parentCE, newName, oldPrefix, newPrefix, copySubTree, newCEIdMap, oldCEIdMap);
						
						ScheduleGroupData skdgrpData = newCE.getSkdgrpData();
						ScheduleGroupAccess skdgrpAccess = new ScheduleGroupAccess(conn);
						String oldDesc = skdgrpData.getSkdgrpDesc();
						String newDesc = newPrefix + oldDesc.substring(8);
						skdgrpData.setSkdgrpDesc(newDesc);
						skdgrpAccess.update(skdgrpData);
						
					} catch (Exception e){
						if (e.getMessage().indexOf("unique constraint") > 0){
							if (logger.isEnabledFor(Priority.WARN)){
								logger.warn("Driver data already exists for: " + newPrefix + newName);
							}
							success = false;
						} else {
							throw e;
						}
						
					}

					if ( "Y".equals( request.getParameter( "chkAutoDistrib" ) )  && success) {
						String driverName = newPrefix + newName;
						
						Distribution dist = new Distribution();
						dist.setDistName(driverName);
						dist.setDistDesc(driverName);
						dist.setDistMode(new Integer(1));
						dist.setDistType(new Integer(3));
						dist.setDistType(new Integer(4));

						DistributionAccess distAcc = new DistributionAccess(conn);
						distAcc.insert(dist);

					}


				}

			} catch( Exception e ) {
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
			} finally {
				SQLHelper.cleanUp(insertStmt);
			}		


		}

	}

	public void addStoreDept( DBConnection conn, List locList, HttpServletRequest request ) {

		Iterator locListItr = locList.iterator();
		String insertSql = 				"insert into WORKBRAIN.SO_SCHEDULE_GROUP " +
		"(SKDGRP_ID, SKDGRP_PARENT_ID, CLNTTYP_ID, " +
		"WBT_ID, SKDGRP_NAME, SKDGRP_CLIENTKEY, " +
		"SKDGRP_INTRNL_TYPE " +
		") VALUES ( " +
		"?, ?, 1, " +
		"?, ?, ?, " +
		"11)";

		PreparedStatement insertStmt = null;


		String storeDeptIdString = (String) request.getParameter( "dblDepartment" );

		if ( !"".equals(storeDeptIdString) && null != storeDeptIdString )  {

			try {
				Integer storeDeptId = new Integer( storeDeptIdString );
				insertStmt = conn.prepareStatement(insertSql);
				CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
				ScheduleGroupData driverData = codeMapper.getScheduleGroupById(storeDeptId.intValue());
				ScheduleGroupData skdgrpData = codeMapper.getScheduleGroupById(storeDeptId.intValue());

				while (locListItr.hasNext()) {

					Integer locId = new Integer ((String) locListItr.next());
					int skdgrpIdSeq = conn.getDBSequence(ScheduleGroupData.SCHEDULE_GROUP_PRI_KEY_SEQ).getNextValue(); 
					String driverName = skdgrpData.getSkdgrpName() + " " + driverData.getSkdgrpName();

					insertStmt.setInt(1, skdgrpIdSeq);
					insertStmt.setInt(2, locId.intValue());
					insertStmt.setInt(3, driverData.getWbtId() );
					insertStmt.setString(4, driverName);
					insertStmt.setInt(5, skdgrpIdSeq);

					insertStmt.execute();

					insertStmt.clearParameters();

				}

			} catch( Exception e ) {
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
			} finally {
				SQLHelper.cleanUp(insertStmt);
			}		


		}

	}	 

	private void doesDriverExistForLocation (DBConnection conn, Integer locId, Integer volDriverId ) {

		String driverSql = 	"SELECT V1.SKDGRP_ID, V2.SKDGRP_ID AS \"PARENT_ID\" " +
		"from VIEW_SO_SCHED_GRP V1, VIEW_SO_SCHED_GRP V2 " +
		"WHERE V1.SKDGRP_PARENT_ID = V2.SKDGRP_ID " +
		"AND V1.SKDGRP_PARENT_ID = ? " +
		"AND V1.SKDGRP_INTRNL_TYPE=12 " +
		"AND V1.SKDGRP_ID = ? ";	

		PreparedStatement stmt = null;

		try { 

			stmt = conn.prepareStatement(driverSql);





		} catch( Exception e ) {
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} finally {
			SQLHelper.cleanUp(stmt);
		}	


	}

	public void deleteNewStaffGroup( DBConnection conn, List locList, String groupNameToDelete ) {

		String deleteSql = "DELETE FROM SO_EMPLOYEE_GROUP WHERE EMPGRP_ID = ?";	
		String deleteSqlChild1 = "DELETE FROM SO_SKDPROF_EMPGRP WHERE EMPGRP_ID = ?";
		String deleteSqlChild2 = "DELETE FROM SO_SKD_EMPGRP WHERE EMPGRP_ID = ?";
		String deleteSqlChild3 = "UPDATE SO_EMPLOYEE SET EMPGRP_ID = 1 WHERE EMPGRP_ID = ?";
		String selectSql = "SELECT EMPGRP_ID FROM SO_EMPLOYEE_GROUP " +
		"WHERE SKDGRP_ID = ? " +
		"AND EMPGRP_NAME LIKE '%" + groupNameToDelete + "%'";            

		PreparedStatement stmt = null;
		PreparedStatement delStmt = null;
		PreparedStatement delStmtChild1 = null;
		PreparedStatement delStmtChild2 = null;
		PreparedStatement delStmtChild3 = null;
		ResultSet rsSelect = null; // NOPMD by A567507 on 11/17/06 1:28 PM
		Iterator locListItr = locList.iterator();

		try { 
			//DBInterface.init(conn);
			stmt = conn.prepareStatement(selectSql);
			delStmt = conn.prepareStatement(deleteSql);
			delStmtChild1 = conn.prepareStatement(deleteSqlChild1);
			delStmtChild2 = conn.prepareStatement(deleteSqlChild2);
			delStmtChild3 = conn.prepareStatement(deleteSqlChild3);

			while ( locListItr.hasNext() ) {

				Integer locId = new Integer ((String) locListItr.next());

				//AUDIT START
				StringBuffer select = new StringBuffer();			
				select.append("SELECT * FROM SO_EMPLOYEE_GROUP WHERE SKDGRP_ID = ");
				select.append(locId.intValue());					
				select.append(" AND EMPGRP_NAME LIKE '%" + groupNameToDelete + "%'");
				String tableName = "SO_EMPLOYEE_GROUP";
				int KeyField = 0;
				QueryRowSource rs = new QueryRowSource(conn, select.toString(), KeyField); 
				RowCursor rc = rs.queryAll();
				while (rc.fetchRow()) {            	              
					this.addAuditEntries(conn,tableName, DELETE, (Row) new BasicRow(rc), null);
				}
				rc.close();		
				//AUDIT END


				stmt.setInt(1, locId.intValue());
				rsSelect = stmt.executeQuery();

				while (rsSelect.next()) { 

					try {
						delStmtChild1.setString(1, rsSelect.getString(1));
						delStmtChild2.setString(1, rsSelect.getString(1));
						delStmtChild3.setString(1, rsSelect.getString(1));
						delStmt.setString(1, rsSelect.getString(1));
						delStmtChild1.executeUpdate();
						delStmtChild2.executeUpdate();
						delStmtChild3.executeUpdate();
						delStmt.executeUpdate();

					} catch (Exception e){

						if (logger.isEnabledFor(Priority.ERROR)) logger.debug("Cannot delete empgrp for skdgrp");

					}

				}

			}

			//stmt.executeBatch();

		} catch( Exception e ) {
			try{
				conn.rollback();
			}
			catch(SQLException ex){
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);
			}
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} finally {
			SQLHelper.cleanUp(stmt);
			SQLHelper.cleanUp(delStmtChild1);
			SQLHelper.cleanUp(delStmtChild2);
			SQLHelper.cleanUp(delStmtChild3);
			SQLHelper.cleanUp(delStmt, rsSelect);
		}					

	}

	public void addNewStaffGroup( DBConnection conn, List locList, HttpServletRequest request ) {

		/*
		 String stfGrpName = (String) request.getParameter( "txtGroupName" );
		 String stfGrpDesc = (!"".equals(request.getParameter( "txtGroupDesc" )))?(String) request.getParameter( "txtGroupDesc" ):null;
		 String schedRuleGrpId = (!"".equals((String) request.getParameter( "dblSchedRules" )))?(String) request.getParameter( "dblSchedRules" ):null;
		 String agesFrom = (!"".equals((String) request.getParameter( "txtGroupMinAge" )))?(String) request.getParameter( "txtGroupMinAge" ):null;
		 String agesTo = (!"".equals((String) request.getParameter( "txtGroupMaxAge" )))?(String) request.getParameter( "txtGroupMaxAge" ):null;
		 String schoolDay = (!"".equals((String) request.getParameter( "ddlGroupSchool" )))?(String) request.getParameter( "ddlGroupSchool" ):null;
		 String students = (!"".equals((String) request.getParameter( "ddlGroupStudents" )))?(String) request.getParameter( "ddlGroupStudents" ):null;
		 */

		String stfGrpName = (String) request.getParameter( "txtGroupName" );
		String stfGrpDesc = (String) request.getParameter( "txtGroupDesc" );
		String schedRuleGrpId = (String) request.getParameter( "dblSchedRules" );
		String agesFrom = (String) request.getParameter( "txtGroupMinAge" );
		String agesTo = (String) request.getParameter( "txtGroupMaxAge" );
		String schoolDay = (String) request.getParameter( "ddlGroupSchool" );
		String students = (String) request.getParameter( "ddlGroupStudents" );		 

		String stfGrpSql = "SELECT SKDGRP_ID FROM SO_EMPLOYEE_GROUP " +
		"WHERE SKDGRP_ID = ? " +
		"AND EMPGRP_NAME = ? ";	
		/*
		 String insertSql = "insert into WORKBRAIN.SO_EMPLOYEE_GROUP (" +
		 					"EMPGRP_ID, EMPGRP_NAME, EMPGRP_DESC, SKDGRP_ID, RULEGRP_ID, " +
		 					"EMPGRP_AGES_FROM, EMPGRP_AGES_TO, EMPGRP_SCHOOL_DAY, EMPGRP_STUDENTS) " +
		 					"values (" +
		 					"SEQ_EMPGRP_ID.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?)";
		 */				 
		PreparedStatement stmt = null;
		PreparedStatement insertStmt = null;
		Iterator locListItr = locList.iterator();
		EmployeeGroupAccess empGrpAccess = new EmployeeGroupAccess(conn);

		try { 
			//DBInterface.init(conn);
			stmt = conn.prepareStatement(stfGrpSql);
			//insertStmt = conn.prepareStatement(insertSql);

			while ( locListItr.hasNext() ) {

				Integer locId = new Integer ((String) locListItr.next());

				stmt.setInt(1, locId.intValue());
				stmt.setString(2, stfGrpName);
				ResultSet rs = stmt.executeQuery(); // NOPMD by A567507 on 11/17/06 1:29 PM
				stmt.clearParameters();
				/*
					if (!rs.next()) {

						insertStmt.setString(1, stfGrpName);
						insertStmt.setString(2, stfGrpDesc);
						insertStmt.setInt(3, locId.intValue());
						insertStmt.setString(4, schedRuleGrpId);
						insertStmt.setString(5, agesFrom);
						insertStmt.setString(6, agesTo);
						insertStmt.setString(7, schoolDay);
						insertStmt.setString(8, students);
						//insertStmt.setString(9, SecurityService.getCurrentClientId());

						insertStmt.execute();
					}
				 */

				if (!rs.next()) {

					CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
					ScheduleGroupData skdgrpData = codeMapper.getScheduleGroupById(locId.intValue());

					EmployeeGroup empGrp = new EmployeeGroup();

					empGrp.assignIsNew(true);
					empGrp.setEmpgrpId( conn.getDBSequence( EmployeeGroup.SO_EMPLOYEE_GROUP_TABLE_PRI_KEY_SEQ ).getNextValue() );
					empGrp.setEmpgrpName( stfGrpName + " - " + skdgrpData.getSkdgrpName() );
					empGrp.setSkdgrpId( locId.intValue() );
					empGrp.setRulegrpId( Integer.parseInt(schedRuleGrpId) );
					if (!"".equals(stfGrpDesc)) empGrp.setEmpgrpDesc( stfGrpDesc );
					if (!"".equals(agesFrom)) empGrp.setEmpgrpAgesFrom( Integer.parseInt(agesFrom) );
					if (!"".equals(agesTo)) empGrp.setEmpgrpAgesTo( Integer.parseInt(agesTo) );
					if (!"".equals(schoolDay)) empGrp.setEmpgrpSchoolDay( Integer.parseInt(schoolDay) );
					if (!"".equals(students)) empGrp.setEmpgrpStudents( Integer.parseInt(students) );

					empGrpAccess.insert( empGrp );
					//AUDIT START  do audit after insert
					this.auditInsert(conn,empGrp.getTableName(),empGrp.getPrimaryKey(), empGrp.getID().intValue());
					//AUDIT END 


				}
				SQLHelper.cleanUp(rs);

			}

			//insertStmt.executeBatch();


		} catch( Exception e ) {

			try{
				conn.rollback();
			}
			catch(SQLException ex){
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);
			}
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);

		} finally {
			SQLHelper.cleanUp(stmt);
			SQLHelper.cleanUp(insertStmt);
		}	

	}


	public void processDistribution (DBConnection conn, HttpServletRequest request) {

		List locList = StringHelper.detokenizeStringAsList( request.getParameter( "locList" ), DELIMITER );
		if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_1_SELECTED ) ) && 
				"DIST_ADD".equalsIgnoreCase( request.getParameter( ACTION_TYPE) ) )  {

			addDistribution(conn, locList, request);

		} else if ( "Y".equalsIgnoreCase( request.getParameter( CHKSECTION_1_SELECTED ) ) && 
				"DIST_EDIT".equalsIgnoreCase( request.getParameter( ACTION_TYPE ) ) )  {

			addDistribution(conn, locList, request);

		}  

	}

	public void deleteDistribution(DBConnection conn, HttpServletRequest request) {

		Integer distCount = (!"".equals(request.getParameter( "distCount" )))?new Integer(request.getParameter( "distCount" )):new Integer(-1);

		String deleteDetailSql = "DELETE FROM SO_DISTRIB_DETAIL WHERE DIST_ID " +
		"= (SELECT DIST_ID FROM so_distribution where DIST_NAME = ?)"; 	

		String deleteDistSql = "DELETE FROM so_distribution where DIST_NAME = ?";  

		String updateDistSoStfDefSql = "UPDATE so_client_stfdef SET dist_id = null where DIST_ID " +
		"= (SELECT dist_id FROM so_distribution WHERE dist_name = ?)";

		String updateDistSoVolWrkldSql = "UPDATE so_volume_workload SET dist_id = null where DIST_ID " +
		"= (SELECT dist_id FROM so_distribution WHERE dist_name = ?)";

		String updateDistSoSchdGrpSql1 = "UPDATE so_schedule_group SET skdgrp_";
		String updateDistSoSchdGrpSql2 = "_dist_id = 1 where skdgrp_";
		String updateDistSoSchdGrpSql3 = "_dist_id = (SELECT dist_id FROM so_distribution WHERE dist_name = ?)";

		String deleteWkldSql = "DELETE FROM SO_VOLUME_WORKLOAD WHERE DIST_ID " +
		"= (SELECT DIST_ID FROM so_distribution where DIST_NAME = ?)"; 

		SimpleDateFormat sdf = new SimpleDateFormat("EEE");
		PreparedStatement delDistStmt = null;
		PreparedStatement delDistDetStmt = null;
		PreparedStatement delWkldStmt = null;

		try { 
			delDistStmt = conn.prepareStatement(deleteDistSql);
			delDistDetStmt = conn.prepareStatement(deleteDetailSql);
			delWkldStmt = conn.prepareStatement(deleteWkldSql);

			for (int i=1; i<distCount.intValue() && distCount.intValue()!=-1 ; i++) {
				if ( "Y".equals( request.getParameter("chkDistrib"+i) ) ) { 
					String dist = request.getParameter("distName"+i);

					delWkldStmt.clearParameters();
					delDistDetStmt.clearParameters();
					delDistStmt.clearParameters();

					cleanupDistributionChildRecords(conn,updateDistSoStfDefSql,dist);
					cleanupDistributionChildRecords(conn,updateDistSoVolWrkldSql,dist);

					Date date = DateHelper.getCurrentDate();
					for (int j=0; j<7; j++) {
						date = DateHelper.addDays(date, 1);
						cleanupDistributionChildRecords(conn,
								updateDistSoSchdGrpSql1+sdf.format(date)+
								updateDistSoSchdGrpSql2+sdf.format(date)+
								updateDistSoSchdGrpSql3,
								dist);
					}

					delWkldStmt.setString( 1, dist );
					delWkldStmt.execute();

					delDistDetStmt.setString( 1, dist );
					delDistDetStmt.execute();

					delDistStmt.setString( 1, dist );
					delDistStmt.execute();
				}
			}
			WorkbrainCache.incrementAllCacheVersions();
		} catch( Exception e ) {
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} finally {
			SQLHelper.cleanUp(delDistStmt);
			SQLHelper.cleanUp(delDistDetStmt);
			SQLHelper.cleanUp(delWkldStmt);
		}	    			 
	}

	private void cleanupDistributionChildRecords(DBConnection conn, String sql, String distName) 
	throws SQLException {
		PreparedStatement childPs = null;
		try {
			childPs = conn.prepareStatement(sql);
			childPs.clearParameters();
			childPs.setString(1, distName);
			childPs.execute();
		} finally {
			SQLHelper.cleanUp(childPs);
		}
	}

	public void addDistribution(DBConnection conn, List locList, HttpServletRequest request) {

		Distribution dist = new Distribution();
		DBInterface.init(conn);
		Iterator locListItr = locList.iterator();
		ScheduleGroupAccess skdgrpAccess = new ScheduleGroupAccess(conn); 

		try {

			CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
			SODaySet oDaySet = new SODaySet( request.getParameter("ddlDays") );

			while ( locListItr.hasNext() ) {    	

				Integer locId = new Integer ((String) locListItr.next());
				int skdgrpId = determineDriverParentSkdgrpId( conn, request.getParameter("dblDriver"), locId );
				if (skdgrpId==0) continue;
				ScheduleGroupData skdgrpData = codeMapper.getScheduleGroupById(locId.intValue());

				String timeStamp = DateHelper.convertDateString(new Date(), "MMddyyyy_HHmmss_SSS");

				dist.setDistName( request.getParameter("txtName") );
				dist.setDistDesc( "Distribution for " + skdgrpData.getSkdgrpName() );
				dist.setSkdgrpId(skdgrpData.getSkdgrpId());
				dist.setDistId( new Integer(conn.getDBSequence( Distribution.DISTRIBUTION_PRI_KEY_SEQ  ).getNextValue() ));
				dist.setDistMode( new Integer (request.getParameter("ddlMode")) );
				dist.setDistType( DistributionType.STATICALLY_GENERATED );
				dist.setInvtypId(IntervalType.INTERVALTYPE_QUARTER_HOUR.integerValue());

				// Loop around here for all 5 dates.
				SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyyMMdd HHmmss");
				SODate oResultsFrom = new SODate();
				SODate oResultsTo = new SODate();

				SODateInterval oResultsInterval1 = new SODateInterval(new SODate(), new SODate());
				SODateInterval oResultsInterval2 = new SODateInterval(new SODate(), new SODate());
				SODateInterval oResultsInterval3 = new SODateInterval(new SODate(), new SODate());
				SODateInterval oResultsInterval4 = new SODateInterval(new SODate(), new SODate());
				SODateInterval oResultsInterval5 = new SODateInterval(new SODate(), new SODate());

				if (!"".equalsIgnoreCase(request.getParameter("txtDateFrom1")) && !"".equalsIgnoreCase(request.getParameter("txtDateTo1")))
				{
					oResultsFrom.setDate(request.getParameter("txtDateFrom1"), oDateFormat);
					oResultsTo.setDate(request.getParameter("txtDateTo1"), oDateFormat);
					oResultsInterval1 = new SODateInterval(oResultsFrom, oResultsTo);
				}
				else
				{
					oResultsInterval1 = null;
				}
				oResultsFrom = new SODate();
				oResultsTo = new SODate();
				if (!"".equalsIgnoreCase(request.getParameter("txtDateFrom2")) && !"".equalsIgnoreCase(request.getParameter("txtDateTo2")))
				{
					oResultsFrom.setDate(request.getParameter("txtDateFrom2"), oDateFormat);
					oResultsTo.setDate(request.getParameter("txtDateTo2"), oDateFormat);
					oResultsInterval2 = new SODateInterval(oResultsFrom, oResultsTo);
				}
				else
				{
					oResultsInterval2 = null;
				}
				oResultsFrom = new SODate();
				oResultsTo = new SODate();
				if (!"".equalsIgnoreCase(request.getParameter("txtDateFrom3")) && !"".equalsIgnoreCase(request.getParameter("txtDateTo2")))
				{
					oResultsFrom.setDate(request.getParameter("txtDateFrom3"), oDateFormat);
					oResultsTo.setDate(request.getParameter("txtDateTo3"), oDateFormat);
					oResultsInterval3 = new SODateInterval(oResultsFrom, oResultsTo);
				}
				else
				{
					oResultsInterval3 = null;
				}
				oResultsFrom = new SODate();
				oResultsTo = new SODate();
				if (!"".equalsIgnoreCase(request.getParameter("txtDateFrom4")) && !"".equalsIgnoreCase(request.getParameter("txtDateTo4")))
				{
					oResultsFrom.setDate(request.getParameter("txtDateFrom4"), oDateFormat);
					oResultsTo.setDate(request.getParameter("txtDateTo4"), oDateFormat);
					oResultsInterval4 = new SODateInterval(oResultsFrom, oResultsTo);
				}
				else
				{
					oResultsInterval4 = null;
				}

				oResultsFrom = new SODate();
				oResultsTo = new SODate();
				if (!"".equalsIgnoreCase(request.getParameter("txtDateFrom5")) && !"".equalsIgnoreCase(request.getParameter("txtDateTo5")))
				{
					oResultsFrom.setDate(request.getParameter("txtDateFrom5"), oDateFormat);
					oResultsTo.setDate(request.getParameter("txtDateTo5"), oDateFormat);
					oResultsInterval5 = new SODateInterval(oResultsFrom, oResultsTo);
				}
				else
				{
					oResultsInterval5 = null;
				}		

				List resultList = new ArrayList();

				if (oResultsInterval1 != null)
				{
					resultList.add(oResultsInterval1);
				}
				if (oResultsInterval2 != null)
				{
					resultList.add(oResultsInterval2);
				}
				if (oResultsInterval3 != null)
				{
					resultList.add(oResultsInterval3);
				}
				if (oResultsInterval4 != null)
				{
					resultList.add(oResultsInterval4);
				}
				if (oResultsInterval5 != null)
				{
					resultList.add(oResultsInterval5);
				}		            

				ScheduleGroupData historicSkdgrpData = codeMapper.getScheduleGroupById( skdgrpId );
				CorporateEntity cE = new CorporateEntity(historicSkdgrpData);
				dist.generateDetailsByRange( resultList, oDaySet, cE );
				dist.dbInsert();

				skdgrpData.setSkdgrpMonDistId( dist.getDistId() );
				skdgrpData.setSkdgrpTueDistId( dist.getDistId() );
				skdgrpData.setSkdgrpWedDistId( dist.getDistId() );
				skdgrpData.setSkdgrpThuDistId( dist.getDistId() );
				skdgrpData.setSkdgrpFriDistId( dist.getDistId() );
				skdgrpData.setSkdgrpSatDistId( dist.getDistId() );
				skdgrpData.setSkdgrpSunDistId( dist.getDistId() );


				//	AUDIT START
				String tableName = skdgrpData.getTableName();
				QueryRowSource rs = this.getQueryRowSource(conn,tableName,skdgrpData.getPrimaryKey(), skdgrpData.getID().intValue(), false);
				RowCursor rc = rs.queryAll();	
				Row newRow = null;					
				this.newRows.clear();
				this.oldRows.clear();
				while (rc.fetchRow()) {
					oldRows.add(new BasicRow(rc));
					newRow = new BasicRow(rc);		             
					newRow.setValue("SKDGRP_MON_DIST_ID", skdgrpData.getSkdgrpMonDistId());
					newRow.setValue("SKDGRP_TUE_DIST_ID", skdgrpData.getSkdgrpTueDistId());
					newRow.setValue("SKDGRP_WED_DIST_ID", skdgrpData.getSkdgrpWedDistId());
					newRow.setValue("SKDGRP_THU_DIST_ID", skdgrpData.getSkdgrpThuDistId());
					newRow.setValue("SKDGRP_FRI_DIST_ID", skdgrpData.getSkdgrpFriDistId());
					newRow.setValue("SKDGRP_SAT_DIST_ID", skdgrpData.getSkdgrpSatDistId());
					newRow.setValue("SKDGRP_SUN_DIST_ID", skdgrpData.getSkdgrpSunDistId());		             
					newRows.add(newRow);                
				}
				rc.close();

				for (int i = 0;i < oldRows.size();i++) {
					this.addAuditEntries(conn,tableName, UPDATE, (Row) oldRows.get(i), (Row) newRows.get(i));
				}
				//AUDIT END
				skdgrpAccess.update( skdgrpData );

			}
		} catch( Exception e ) {
			try{
				e.printStackTrace();
				conn.rollback();
			}
			catch(SQLException ex){
				if (logger.isEnabledFor(Priority.ERROR)) logger.error(ex, ex);
			}
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} 		



	}

	public int determineDriverParentSkdgrpId( DBConnection conn, String clientTypeId, Integer locId ) {

		//String locListStr = locList.toString().substring(1,locList.toString().length()-1);
		String driverSql = 	"SELECT SKDGRP_ID " +
		"FROM SO_SCHEDULE_GROUP SG, SO_CLIENT_TYPE CT " +
		"WHERE SG.CLNTTYP_ID = CT.CLNTTYP_ID " +
		"AND CT.CLNTTYP_ID = ? " +
		"AND SKDGRP_PARENT_ID = ? ";	

		PreparedStatement stmt = null;
		ResultSet rs = null; // NOPMD by A567507 on 11/17/06 1:29 PM

		try { 

			stmt = conn.prepareStatement(driverSql);
			stmt.setInt(1,Integer.parseInt(clientTypeId));
			stmt.setInt(2,locId.intValue());
			rs = stmt.executeQuery();

			if (rs.next()) return rs.getInt("SKDGRP_ID");  

		} catch( Exception e ) {
			if (logger.isEnabledFor(Priority.ERROR)) logger.error(e, e);
		} finally {
			SQLHelper.cleanUp(stmt);
			SQLHelper.cleanUp(rs);
		}			 

		return 0;

	}

	public void deleteStaffRequirement( DBConnection conn, String reqNameList, String locIdList) throws Exception {
		PreparedStatement ps = null; 
		PreparedStatement psDelReq = null; 
		PreparedStatement psDelWrkld = null; 
		ResultSet rslt = null; // NOPMD by A567507 on 11/17/06 1:29 PM
		String[] reqNameArray = StringHelper.detokenizeString(reqNameList, ",");
		String selectSql = "SELECT CSD_ID FROM SO_CLIENT_STFDEF " +
		"WHERE CSD_DESC LIKE ? " +  
		"AND SKDGRP_ID IN ( " + locIdList + " )";

		String deleteReqSql =  "DELETE FROM SO_CLIENT_STFDEF " +
		"WHERE CSD_ID = ? ";

		String deleteWrkLdSql =  "DELETE FROM SO_VOLUME_WORKLOAD " +
		"WHERE CSD_ID = ? ";  

		ps = conn.prepareStatement(selectSql);
		psDelReq = conn.prepareStatement(deleteReqSql);
		psDelWrkld = conn.prepareStatement(deleteWrkLdSql);

		try { 

			for ( int i = 0; i < reqNameArray.length; i++ ) {
				String reqName = "%-DEPT" + reqNameArray[i];

				//AUDIT START
				StringBuffer select = new StringBuffer();			
				select.append("SELECT * FROM SO_CLIENT_STFDEF WHERE CSD_DESC LIKE ");
				select.append(conn.encode(reqName));					
				select.append("AND SKDGRP_ID IN ( " + locIdList + " )");
				String tableName = "SO_CLIENT_STFDEF";
				int KeyField = 0;
				QueryRowSource rs = new QueryRowSource(conn, select.toString(), KeyField); 
				RowCursor rc = rs.queryAll();
				while (rc.fetchRow()) {            	              
					this.addAuditEntries(conn,tableName, DELETE, (Row) new BasicRow(rc), null);
				}
				rc.close();		
				//AUDIT END

				ps.setString( 1, reqName );
				rslt = ps.executeQuery();
				while (rslt.next()) {
					psDelWrkld.setInt(1,rslt.getInt(1));
					psDelWrkld.executeUpdate();

					psDelReq.setInt(1,rslt.getInt(1));
					psDelReq.executeUpdate();
				}  
			}        
		} finally {
			SQLHelper.cleanUp(rslt);
			SQLHelper.cleanUp(ps);
			SQLHelper.cleanUp(psDelReq);
			SQLHelper.cleanUp(psDelWrkld);
		}               

	}
	//Returns QueryRowSource containing a single row
	public QueryRowSource getQueryRowSource(DBConnection conn, String tableName, String keyName, int keyValue, boolean getFirstRowOnly){

		int KeyField = 0;					
		StringBuffer select = new StringBuffer();			

		if (getFirstRowOnly){
			select.append("SELECT * FROM " + tableName);
			select.append(" WHERE rownum = 1 AND rowid NOT IN (SELECT rowid FROM " + tableName);
			select.append(" WHERE rownum < 10)");
		}
		else{
			select.append("SELECT * FROM " + tableName);
			select.append(" WHERE " + keyName + " = ");
			select.append(keyValue);	
		}

		return new QueryRowSource(conn, select.toString(), KeyField); 
	}
	//This method add audit records for the type INSERT
	public void auditInsert(DBConnection conn, String tableName, String keyName, int keyValue) throws SQLException, SQLException{

		RowCursor rc = null;
		QueryRowSource rs = null;

		try {
			rs = this.getQueryRowSource(conn,tableName,keyName, keyValue, false);	    	
			rc = rs.queryAll();
			while(rc.fetchRow()){
				this.addAuditEntries(conn,tableName, INSERT, null, (Row) new BasicRow(rc));
			}	

		} catch (AccessException acc) {

			acc.printStackTrace();
			throw new SQLException("Error getting RowDefinition");

		} finally {
			try {
				if (null!=rc) rc.close();

			} catch (AccessException acc) {
				acc.printStackTrace();
				throw new SQLException("Error getting RowDefinition");
			}
		}
	}

	/*
	 *  This method adds audit_log entries for insert,deletes and updates
	 *  Insert: new row
	 *  Delete: old row
	 *  Update: new and old row
	 */
	public void addAuditEntries(DBConnection conn, String tableName, int type, Row oldRow, Row newRow) throws SQLException {

		String op;
		String opLetter;
		RowDefinition rd;
		PreparedStatement ps = null;

		try {
			if (newRow != null)
				rd = newRow.getRowDefinition();
			else
				rd = oldRow.getRowDefinition();


			switch (type) {
			case UPDATE:
				op = "UPDATE";
				opLetter = "U";
				break;
			case DELETE:
				op = "DELETE";
				opLetter = "D";
				break;
			case INSERT:
				op = "INSERT";
				opLetter = "I";
				break;
			default:
				return;
			}
			ps = conn.prepareStatement(AUDIT_SQL);

			//public static final String AUDIT_SQL = "INSERT INTO audit_log (
			//1: audlog_id         //2: wbu_name
			//3: wbu_name_actual   //4: audlog_change_date
			//5: audlog_tablename  //6: audlog_action
			//7: audlog_key_i      //8: audlog_fieldname
			//9: audlog_old_value  //10: audlog_new_value

			for(int i=0;i<rd.getRowSize();i++) {
				FieldType ft = rd.getFieldType(i);

				if (type == INSERT) {
					ps.setString(7,newRow.getValue(rd.getKeyField()).toString()); //key id
					ps.setString(9,op); //old value
					ps.setString(10, getAuditValue(rd.getFieldType(i),newRow.getValue(i), false)); //new value
				}
				else if (type == DELETE) {
					try {
						ft.convert(oldRow.getValue(i));
					} catch(NotValidDataException e) {
						continue;
					}
					ps.setString(7,oldRow.getValue(rd.getKeyField()).toString()); //key id
					ps.setString(9,getAuditValue(rd.getFieldType(i),oldRow.getValue(i), false)); //old value
					ps.setString(10, op); //new value
				}
				else if (type == UPDATE) {
					try {
						ft.convert(oldRow.getValue(i));
					} catch(NotValidDataException e) {
						continue;
					}
					if (oldRow.getValue(i) == null &&  newRow.getValue(i) == null)
						continue;
					if (oldRow.getValue(i) != null)
						if (oldRow.getValue(i).equals(newRow.getValue(i)))
							continue;

					ps.setString(7,oldRow.getValue(rd.getKeyField()).toString()); //key id
					ps.setString(9,getAuditValue(rd.getFieldType(i),oldRow.getValue(i), false)); //old value
					ps.setString(10, getAuditValue(rd.getFieldType(i),newRow.getValue(i), false)); //new value
				}
				ps.setInt(1,conn.getDBSequence("seq_audlog_id").getNextValue());
				ps.setString(2, this.userName);
				ps.setString(3, this.actualUserName);
				ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

				ps.setString(5,tableName);
				ps.setString(6, opLetter);
				ps.setString(8, rd.getName(i));
				ps.executeUpdate();
			}
		} catch (AccessException acc) {
			throw new SQLException("Error getting RowDefinition");
		}
		finally {
			SQLHelper.cleanUp(ps);
		}
	}
	protected String getAuditValue(FieldType ft, Object value, boolean quoted)
	{
		String auditValue = getAuditValueChunk(ft,value,0);
		if (quoted)
			auditValue = "'" + auditValue + "'";

		return auditValue;
	}
	//	  Returns the value of a field in a format suitable for inserting into the audit table
	protected String getAuditValueChunk(FieldType ft, Object value, int chunkNumber) {
		if (value==null) return chunkNumber==0 ? "NULL" : null;
		String str = StringHelper.searchReplace(value.toString(),"'","''");
		if(str.length() < 3900){
			if(chunkNumber>0) return null;
		} else {
			if(str.length() < 3900 * (chunkNumber)) return null;
			str = str.substring(chunkNumber*3900,Math.min((chunkNumber+1)*3900, str.length()));
			int l = str.length();
			if(l>0){
				if(str.charAt(0)=='\'' && (l==1 || str.charAt(1)!='\'')){
					str = "'"+str;
					l++;
				}
				if(l>1 && str.charAt(l-1)=='\'' && str.charAt(l-2)!='\''){
					str = str.substring(0,l-1);
				}
				if(chunkNumber>0){
					str = "Section "+(chunkNumber+1)+":\n"+str;
				}
			}
		}
		return str;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub\
		String locListStr = "12327213,123231,123123,1312329";
		List locList = StringHelper.detokenizeStringAsList(locListStr, ",");
		//System.out.print("X"+locList.toString().substring(1,locList.toString().length()-1)+"X");
	}
}
