package com.wbiag.app.modules.reports.hcprintschedule;

import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.modules.launchpads.staffingcoverage.*;
import java.util.*;

/**
 * @author          Ali Ajellu
 * @version         1.0
 * Date:            Friday, June 16 206
 * Copyright:       Workbrain Corp.
 * TestTrack:       1630
 *
 * TeamDaypartsSource is needed because it's not a trivial matter to get a team's day parts
 * using SQL. The reason for that is that the table that connects a Workbrain Team with its
 * Day Part Set, name Team_Day_Part_Set, does not contain teams that inherit a default day
 * part from their parent teams.
 *
 * TeamDaypartsSource takes a comma-separated list of wbt ids in the "where" parameter and
 * returns a unique list of day parts associated with at least one team in the argument list
 */
public class TeamDaypartsSource extends AbstractRowSource {

	private static org.apache.log4j.Logger logger =
		                      org.apache.log4j.Logger.getLogger(TeamDaypartsSource.class);
	private RowDefinition rowDefinition;
	private DBConnection conn;
	private java.util.List rows = new ArrayList();
	private java.util.List teamIds = new ArrayList();
	private Map daypartsFound = new HashMap();

	{
		RowStructure rs = new RowStructure(5);
		rs.add("DP_ID", CharType.get(100));
		rs.add("DP_NAME", CharType.get(100));
		rs.add("DP_DESC", CharType.get(100));
		rs.add("DP_START_TIME", CharType.get(100));
		rs.add("DP_END_TIME", CharType.get(100));
		rowDefinition = new RowDefinition(-1, rs);
	}

    /**
     * Default constructor.
     * If conn==null, no work is done.
     *
     * @param conn              an open, active db connection
     * @param paramList         list of parameters
     * @throws AccessException  is thrown if a SQL or other Row exceptions are thrown
     */
	public TeamDaypartsSource(DBConnection conn, com.workbrain.server.data.ParameterList paramList)
									throws AccessException{
		this.conn = conn;
		if (this.conn == null){
			log("The dbconnection passed to a rowsource cannot be null");
			return;
		}

		String teamIdsStr = (String)paramList.findParam("where").getValue();
		if (teamIdsStr == null || "ALL".equals(teamIdsStr)){
			teamIdsStr = "";
		}
		log("teamIdsStr: " + teamIdsStr);

		StringTokenizer teamIdsTokenizer = new StringTokenizer(teamIdsStr.trim(),",");
		while(teamIdsTokenizer.hasMoreTokens()){
			this.teamIds.add(teamIdsTokenizer.nextToken().trim());
		}
		log("teamIds: " + teamIds);

		loadRows();
	}

    /**
     * Find and load the data into a list of Rows.
     * @throws AccessException  if a SQL or other Row exception is thrown
     */
	private void loadRows() throws AccessException{
		rows.clear();
		log("inside loadRows");

		DayPartSetAccess daypartSetAccess = new DayPartSetAccess(conn);
		DayPartAccess daypartAccess = new DayPartAccess(conn);

		//if there are no team ids present, get all day parts in system
		if (this.teamIds.size() == 0){
			log("teamIds is empty, so load all dayparts");
			List allDayparts = daypartAccess.load();
			Iterator allDaypartsIter = allDayparts.iterator();
			while(allDaypartsIter.hasNext()){
				DayPartData currDaypartData = (DayPartData) allDaypartsIter.next();
				log("adding daypart " + currDaypartData.getDpName());
				rows.add(createRow(currDaypartData));
			}


		}else{
			log("teamIds is not empty, so load dayparts selectively");
			//find dayparts for each team and add them to the daypartsFound map.
			for (int i=0;i<this.teamIds.size();i++){
				try{
					int currTeamId = Integer.parseInt(((String)this.teamIds.get(i)).trim());
					int currTeamDaypartSetId = StaffingCoverageHelper.getTeamDayPartSetId(currTeamId, conn);
					DayPartSetData currDaypartSetData = daypartSetAccess.load(currTeamDaypartSetId);

					List currDayparts = daypartAccess.loadByDpsetId(currDaypartSetData.getDpsetId());
					for (int j=0;j<currDayparts.size();j++){
						DayPartData currDaypartData = (DayPartData)currDayparts.get(j);
						if (this.daypartsFound.get(currDaypartData.getDpName()) == null){
							this.daypartsFound.put(currDaypartData.getDpName(), currDaypartData);
						}
					}
				}catch(NumberFormatException e){
					//just print an error
					logger.error("The team id '" + this.teamIds.get(i) + "' is not an integer");
				}
			}

			//extract all of daypartsFound map, create Row objects from them and add them to rows list.
			Set daypartsFoundKeys = this.daypartsFound.keySet();
			Iterator keysIter = daypartsFoundKeys.iterator();
			while(keysIter.hasNext()){
				DayPartData currDaypartData = (DayPartData)this.daypartsFound.get(keysIter.next());
				log("adding daypart " + currDaypartData.getDpName());
				rows.add(createRow(currDaypartData));
			}
		}
	}

    /**
     * Create a row given the Day Part Data.
     * @param dpData The Day Part data object that the returned row will be based on.
     * @return a Row filled with data extracted from dpData
     * @throws AccessException is thrown if a Row error occures.
     */
	private Row createRow(DayPartData dpData) throws AccessException{
		Row r = new BasicRow(getRowDefinition());
		r.setValue("DP_ID", String.valueOf(dpData.getDpId()));
		r.setValue("DP_NAME", dpData.getDpName());
		r.setValue("DP_DESC", dpData.getDpDesc());
		r.setValue("DP_START_TIME", dpData.getDpStartTime());
		r.setValue("DP_END_TIME", dpData.getDpEndTime());
		log("creating row: " + r);
		return r;
	}

	/* (non-Javadoc)
	 * @see com.workbrain.server.data.RowSource#getRowDefinition()
	 */
	public RowDefinition getRowDefinition() throws AccessException {
		return this.rowDefinition;
	}

	/* (non-Javadoc)
	 * @see com.workbrain.server.data.RowSource#queryAll()
	 */
	public RowCursor queryAll() throws AccessException {
		return new AbstractRowCursor(getRowDefinition()){
			private int counter = -1;
			protected Row getCurrentRowInternal(){
			return counter >= 0 && counter < rows.size()
					?(BasicRow)rows.get(counter)
					: null;
			}

			protected boolean fetchRowInternal() throws AccessException{
				return ++counter < rows.size();
			}

			public void close(){}
		};
	}

	/* (non-Javadoc)
	 * @see com.workbrain.server.data.RowSource#query(java.lang.String)
	 */
	public RowCursor query(String arg0) throws AccessException {
		return queryAll();
	}

	/* (non-Javadoc)
	 * @see com.workbrain.server.data.RowSource#query(java.lang.String, java.lang.String)
	 */
	public RowCursor query(String arg0, String arg1) throws AccessException {
		return queryAll();
	}

	/* (non-Javadoc)
	 * @see com.workbrain.server.data.RowSource#query(java.util.List)
	 */
	public RowCursor query(List arg0) throws AccessException {
		return queryAll();
	}

	/* (non-Javadoc)
	 * @see com.workbrain.server.data.RowSource#query(java.lang.String[], java.lang.Object[])
	 */
	public RowCursor query(String[] arg0, Object[] arg1) throws AccessException {
		return queryAll();
	}

	/* (non-Javadoc)
	 * @see com.workbrain.server.data.RowSource#count()
	 */
	public int count() throws AccessException {
		return rows.size();
	}

	/* (non-Javadoc)
	 * @see com.workbrain.server.data.RowSource#count(java.lang.String)
	 */
	public int count(String arg0) throws AccessException {
		return rows.size();
	}

	public boolean isReadOnly(){
		return true;
	}

    private void log(String msg) {
        if (logger.isDebugEnabled()) logger.debug(msg);
    }

}
