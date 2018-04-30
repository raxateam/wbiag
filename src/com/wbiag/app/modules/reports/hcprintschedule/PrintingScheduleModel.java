package com.wbiag.app.modules.reports.hcprintschedule;

import java.util.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.modules.launchpads.staffingcoverage.*;
import com.workbrain.app.modules.availability.db.AvOvrAccess;
import com.workbrain.app.modules.availability.model.AvOvrData;


/**
 * @author          Ali Ajellu
 * @version         1.0
 * Date:            Friday, June 16 206
 * Copyright:       Workbrain Corp.
 * TestTrack:       1630
 *
 * The Model in the report's MVC Model. See class header comments in PrintingScheduleController.java
 * for more information.
 *
 * The following variables must be set before the main method, retrieveModelData(), can be called:
 * teams,
 * dayparts
 * daypartsSortDir,
 * jobs
 * jobsSortDir,
 * emps,
 * startDate,
 * endDate
 */
public class PrintingScheduleModel {
	private static org.apache.log4j.Logger logger =
		org.apache.log4j.Logger.getLogger(PrintingScheduleModel.class);
	protected String 	teams,
						dayparts,
						daypartsSortDir,
						jobs,
						jobsSortDir,
						emps,
						reportOutputFormat;
	protected Date		startDate,
						endDate;
	protected int		userId;
	protected boolean	useTeamPayPeriod;            //indicates whether each individ. team's curr pay period must be used (true if start/end date empty)
	protected DBConnection conn;
	protected CodeMapper cm;
	protected Map 		tcodeMapper;                 //maps translatable tcode names into their abbreviation.

	private SchedulePeriodAccess schedPeriodAccess;
	private DayPartAccess daypartAccess;
	private DayPartSetAccess daypartSetAccess;
	private List currFloatingSchedules;
	private Map allDayparts;                         //maps a daypart's name to its data object (used because of bug in DayPartData)

	private EmployeeHistoryMatrix empHistoryMatrix;   				//added by Vlad
	private Hashtable empAvailability, empSchedDt, empSched;		//added by Vlad
	
    //HUB:42198  TT:2964
	private boolean showTotal;


	/**
     * Default constructor.
     *
     * @param conn          An open, active db connection
     * @throws SQLException
	 */
	public PrintingScheduleModel(DBConnection conn) throws SQLException{
		this.conn = conn;
		this.cm = CodeMapper.createBrandNewCodeMapper(conn);
		this.tcodeMapper = createTcodeMapper();
		this.schedPeriodAccess = new SchedulePeriodAccess(conn);
		this.daypartAccess = new DayPartAccess(conn);
		this.daypartSetAccess = new DayPartSetAccess(conn);
		this.allDayparts = retrieveAllDayparts();
	}

	/**
	 * The main method provoked by the Controller to get the data the model has put together
     *
     * @throws SQLException
	 */
	public List retrieveModelData() throws SQLException{
		fillParameterGaps();
		return buildInitialList();
	}

	/**
	 * build the object tree: [teams] -> [dayparts] -> [jobs] -> [emps] -> [schedule details]
     *
     * @throws SQLException
	 */
	private List buildInitialList() throws SQLException{
		preloadData();
		List dataList = buildTeamList();
		dataList = buildDaypartList(dataList);
		dataList = buildJobList(dataList);
		dataList = buildEmpList(dataList);
		return dataList;
	}

    /**
     * Creats the tails end of the model structure that contains the employees and their respective schedules (given
     * team, daypart and job filters)
     *
     * @param empAccess         Non-null employee access object.
     * @param empId             Employee DB ID
     * @param team              The team by which the schedules must be filtered
     * @param daypart           The daypart by which the shcedules must be filtered
     * @param job               The job by which the schedules must be filtered
     * @return                  An employee with all of its appropriate schedules
     * @throws SQLException
     */

	//Added by Vlad for performance improvement(hub #40076)
	private void preloadData() throws SQLException{
		StringTokenizer empIdsTokenizer = new StringTokenizer(this.emps, ",");
		int[] empIds = new int[empIdsTokenizer.countTokens()];
		int i=0, empId;
		Integer empIdObj;
		List availabilityData, schedDt, sched;
		AvOvrAccess availabilityAccess = new AvOvrAccess(conn, cm);
		EmployeeSchedDtlAccess schedDtAccess = new EmployeeSchedDtlAccess(conn);
		EmployeeScheduleAccess schedAccess = new EmployeeScheduleAccess(conn,cm);


		empAvailability = new Hashtable();
		empSchedDt = new Hashtable();
		empSched = new Hashtable();

		while(empIdsTokenizer.hasMoreElements()){
			empId = Integer.parseInt(empIdsTokenizer.nextToken().trim());
			empIds[i++] = empId;
			empIdObj =new Integer(empId);

			Date currentWRKDate = new Date(this.startDate.getTime());
			Hashtable availability = new Hashtable();

			while(currentWRKDate.before(DateHelper.addDays(this.endDate, 1))){
				StringBuffer whereCond = new StringBuffer();
		        whereCond.append("EMP_ID = " + empId + " AND AMXOVC_ID = 2");
		        whereCond.append(" AND " + conn.encodeDate(currentWRKDate) + " BETWEEN amxov_wd_st and amxov_wd_end");
		        StringBuffer orderBy = new StringBuffer();
		        orderBy.append("AMXOV_CRT_DT desc");
		        availabilityData = availabilityAccess.loadRecordDataWithSelectOrderBy(StringHelper.EMPTY, whereCond.toString(), orderBy.toString());
		        availability.put(currentWRKDate, availabilityData);
				currentWRKDate = DateHelper.addDays(currentWRKDate, 1);
			}
			empAvailability.put(empIdObj, availability);
			schedDt  = schedDtAccess.loadByDateRange(empId, this.startDate, DateHelper.addDays(this.endDate, 1));
		    empSchedDt.put(empIdObj, schedDt);
		    Iterator schedulesIter = schedDt.iterator();
			EmployeeScheduleData data= new EmployeeScheduleData();
			while(schedulesIter.hasNext()){
				EmployeeSchedDtlData currSchedData = (EmployeeSchedDtlData)schedulesIter.next();
				currSchedData.setCodeMapper(cm);
				sched = schedAccess.loadRecordData(data, "EMPLOYEE_SCHEDULE", "EMPSKD_ID", currSchedData.getEmpskdId());
				empSched.put(new Integer(currSchedData.getEmpskdId()), sched);
			}
		}
		EmployeeHistoryAccess eha = new EmployeeHistoryAccess(conn);
		empHistoryMatrix = eha.load(empIds, this.startDate);

	}

	public Employee createEmployeeAndSchedulesStructure(EmployeeAccess empAccess, int empId, Team team, Daypart daypart, Job job)
																							throws SQLException{
        Integer empIdObj = new Integer(empId);
		//get employee info
		EmployeeData empData = empHistoryMatrix.getEmployeeForDate(empId, team.startDate);

		Employee Emp = new Employee(empData.getEmpFullname(), empData.getEmpVal7());

		//will be used later for getting the home team
		EmployeeTeamAccess empTeamAccess = new EmployeeTeamAccess(conn);


		//HUB 35988 649	shlee: show employee availability on schedule as "A"
		Date currentWRKDate = team.startDate;
		while(currentWRKDate.before(DateHelper.addDays(team.endDate, 1))){
			List availabilityData = (List)((Hashtable)empAvailability.get(empIdObj)).get(currentWRKDate);

			if (availabilityData.size() > 0){

				String homeTeamName = cm.getWBTeamById(empTeamAccess.loadHomeTeam(empId, currentWRKDate).getWbtId()).getWbtName();
				Date schedStartTime =  ((AvOvrData) availabilityData.get(0)).getAmxovStTime();
				Date schedEndTime = ((AvOvrData) availabilityData.get(0)).getAmxovEndTime();

				if (doesIntervalConflict(schedStartTime, schedEndTime, daypart.startTime, daypart.endTime)){

					//create Schedule for matching schedules and add it to the Emp
					Schedule schedToAdd =
						new Schedule(currentWRKDate,
										 empData.getEmpFullname(),
										 job.name,
										 DateHelper.convertDateString(schedStartTime, "HHmm"),
										 DateHelper.convertDateString(schedEndTime, "HHmm"),
										 0,
										 "A",
										 team.name,
										 homeTeamName);

					Emp.addSchedule(schedToAdd);
		            log("Added Availability to above Emp: " + schedToAdd);

				}
			}
			currentWRKDate = DateHelper.addDays(currentWRKDate, 1);
		}

		//try to load schedules for the same date range.
		//if any schedules exist, filter each one based on team.id, daypart.startTime, daypart.endTime, & job.id
		List schedules  = (List)empSchedDt.get(empIdObj);

		Iterator schedulesIter = schedules.iterator();

		//HUB 35988 635 prevent duplicate WRK records on the same day
		currentWRKDate = DateHelper.addDays(team.startDate, -1);

		while(schedulesIter.hasNext()){
			EmployeeSchedDtlData currSchedData = (EmployeeSchedDtlData)schedulesIter.next();


			currSchedData.setCodeMapper(cm);
			Date schedWorkDate = currSchedData.getEschdWorkDate();
			Date schedStartTime =  currSchedData.getEschdStartTime();
			Date schedEndTime = currSchedData.getEschdEndTime();

		    //HUB 35988 635
			//Get EMPLOYEE_SCHEDULE record
			List list = (List)empSched.get(new Integer(currSchedData.getEmpskdId()));
			EmployeeScheduleData currEmpScheduleData = (EmployeeScheduleData)list.get(0);

			if (currSchedData.getJobId() == job.id &&
			doesIntervalConflict(schedStartTime, schedEndTime, daypart.startTime, daypart.endTime) &&
					currSchedData.getEschdStatus().equals("C")){


				String homeTeamName = cm.getWBTeamById(empTeamAccess.loadHomeTeam(empId, schedWorkDate).getWbtId()).getWbtName();

				if (!team.name.equals(homeTeamName) && currSchedData.getWbtId() != team.id){
					continue;
				}
				String finalSchedTcodeName = "";
				if(tcodeMapper.get(currSchedData.getEschdTcodeName()) == null){			//NO ABBREV. EXISTS


					//Hub 35988 : 635 Ignore all timecode except WRK
					if ("WRK".equalsIgnoreCase(currSchedData.getEschdTcodeName())){
						finalSchedTcodeName = currSchedData.getEschdTcodeName();
					}else{
						continue;
					}

				}else{																	//ABBREV. EXISTS
					finalSchedTcodeName = (String)tcodeMapper.get(currSchedData.getEschdTcodeName());
				}
				//Hub 35988 : 635
				if ("WRK".equalsIgnoreCase(finalSchedTcodeName)){
					schedStartTime =  currEmpScheduleData.getEmpskdActStartTime();
					schedEndTime = currEmpScheduleData.getEmpskdActEndTime();
					if (currentWRKDate.before(schedWorkDate)){
						currentWRKDate = schedWorkDate;
					}
					else{
						continue; //skip this WRK record
					}

				}
					//create Schedule for matching schedules and add it to the Emp
					Schedule schedToAdd =
						new Schedule(schedWorkDate,
										 empData.getEmpFullname(),
										 job.name,
										 DateHelper.convertDateString(schedStartTime, "HHmm"),
										 DateHelper.convertDateString(schedEndTime, "HHmm"),
										 currSchedData.getEschdMinutes(),
										 finalSchedTcodeName,
										 currSchedData.getEschdWbtName(),
										 homeTeamName);

					Emp.addSchedule(schedToAdd);
	                log("Added Schedule to above Emp: " + schedToAdd);

					if (team.name.equals(homeTeamName) && !currSchedData.getEschdWbtName().equals(team.name)){
						this.currFloatingSchedules.add(schedToAdd);
					}


			}
		}
		//Hub 36230: shlee add home team of employee on schedule start date
		Emp.setHomeTeam(empTeamAccess.loadHomeTeam(empId, team.startDate).getWbtId());
		return Emp;
	}

    /**
     * Determines whether the two ranges [start1, end1] and [start2, end2] conflict.
     * This also takes into consideration cross-midnight conflicts.
     * @param start1
     * @param end1
     * @param start2
     * @param end2
     * @return
     */
	private boolean doesIntervalConflict (Date start1, Date end1, Date start2, Date end2){
	    start1 = DateHelper.createTime(start1.getHours(), start1.getMinutes(), start1.getSeconds());
	    end1 = DateHelper.createTime(end1.getHours(), end1.getMinutes(), end1.getSeconds());
	    start2 = DateHelper.createTime(start2.getHours(), start2.getMinutes(), start2.getSeconds());
	    end2 = DateHelper.createTime(end2.getHours(), end2.getMinutes(), end2.getSeconds());

	    if(end1.before(start1)){
	      end1.setHours(end1.getHours() + 24);
	    }
	    if (end2.before(start2)){
	      end2.setHours(end2.getHours() + 24);
	    }


	    boolean conflict =
	      (DateHelper.compare(end2, start1) > 0 &&
	       DateHelper.compare(end1, start2) > 0)
	      ||
	      (DateHelper.compare(start1, start2) == 0 &&
	       DateHelper.compare(end1, end2) == 0);

	    if (!conflict){
	      if (start2.getDate() == end2.getDate()){
	        start2.setHours(start2.getHours() + 24);
	        end2.setHours(end2.getHours() + 24);

	        conflict =
	          (DateHelper.compare(end2, start1) > 0 &&
	           DateHelper.compare(end1, start2) > 0)
	          ||
	          (DateHelper.compare(start1, start2) == 0 &&
	           DateHelper.compare(end1, end2) == 0);
	      }
	    }

	    return conflict;
	}

    /**
     * Given a dataList that has been set up with jobs already, this method assigns employees
     * and schedules to the jobs.
     *
     * @param dataList
     * @return a dataList with employees and schedules
     * @throws SQLException
     */
	private List buildEmpList(List dataList) throws SQLException{

		TreeMap empMap = new TreeMap();


		//fill up the sorted map with the param dayparts (key=job name value=JobData object)
		StringTokenizer empIdsTokenizer = new StringTokenizer(this.emps, ",");
		EmployeeAccess empAccess = new EmployeeAccess(conn, cm);

		while(empIdsTokenizer.hasMoreTokens()){
			int currEmpId = Integer.parseInt(empIdsTokenizer.nextToken().trim());
			EmployeeData currEmpData = empHistoryMatrix.getEmployeeForDate(currEmpId, this.startDate);

			//HUB 35988: 649, shlee: check termination date
			if (currEmpData != null && this.startDate.before(currEmpData.getEmpTerminationDate())){
				//HUB 36021,shlee: sort employee by last name, first name
				empMap.put(String.valueOf(currEmpData.getEmpFullname() + currEmpData.getEmpName()), currEmpData);
			}

		}


		for (int teamIndex=0;teamIndex<dataList.size();teamIndex++){
			Team currTeam = (Team)dataList.get(teamIndex);
			List currTeamDayparts = currTeam.dayparts;
			for (int daypartIndex=0;daypartIndex<currTeamDayparts.size();daypartIndex++){
				Daypart currDaypart = (Daypart)currTeamDayparts.get(daypartIndex);
				List currDaypartJobs = currDaypart.jobs;

				this.currFloatingSchedules = new ArrayList();

				Set teamsSet = new TreeSet();
				teamsSet.add(String.valueOf(currTeam.id));

				for (int jobIndex=0;jobIndex<currDaypartJobs.size();jobIndex++){
					Job currJob = (Job)currDaypartJobs.get(jobIndex);
					Set jobsSet = new TreeSet();
					jobsSet.add(String.valueOf(currJob.id));
					//all employees who have specific jobs and were member of specific teams that coincided with the date range.
					List currJobEmps = EmployeeJobAccess.loadByJobIdsForDateRange(conn, teamsSet, jobsSet, this.startDate, this.endDate);


					Set sortedParamEmps = empMap.keySet();
					Iterator sortedParamEmpsIter = sortedParamEmps.iterator();
					while(sortedParamEmpsIter.hasNext()){
						int currParamEmpId = ((EmployeeData)empMap.get(sortedParamEmpsIter.next())).getEmpId();

						//if we can find currParamJobName in currTeamJobs, then we can add it to the currDaypart
						for (int j=0;j<currJobEmps.size();j++){
							int currEmpId = ((EmployeeJobData)currJobEmps.get(j)).getEmpId();

							if (currParamEmpId == currEmpId){
									Employee empToAdd =
										createEmployeeAndSchedulesStructure(empAccess, currEmpId, currTeam, currDaypart, currJob);
									//Hub 35988: 649 shlee: don't add employees that don't have any schedule in this job in this day part
									if(empToAdd.schedules.size() > 0){
										currJob.addEmployee(empToAdd);
		                                log("Added emp: " + empToAdd + "--- To Job: " + currJob + "--- in Daypart" + currDaypart + "--- in Team: " + currTeam);
		                                break;   //HUB 35988: 649 shlee: leave the loop, prevent duplicate employee in the same job

									}
							}
						}
					}
				}
				currDaypart.floatingSchedules = this.currFloatingSchedules;
			}
		}

		return dataList;
	}

    /**
     * Given a dataList that has been set up with dayparts already, this method assigns jobs
     * to the dayparts.
     *
     * @param dataList
     * @return
     * @throws SQLException
     */
	private List buildJobList(List dataList) throws SQLException{
		TreeMap jobMap = null;

		//depending on param, set the order of the day parts
		if (daypartsSortDir.equalsIgnoreCase("Descending")){
			jobMap = new TreeMap(Collections.reverseOrder());
		} else {
			jobMap = new TreeMap();
		}

		//fill up the sorted map with the param jobs (key=job name value=JobData object)
		StringTokenizer jobIdsTokenizer = new StringTokenizer(this.jobs, ",");
		JobAccess jobAccess = new JobAccess(conn);
		while(jobIdsTokenizer.hasMoreTokens()){
			int currJobId = Integer.parseInt(jobIdsTokenizer.nextToken().trim());
			JobData currJobData = jobAccess.load(currJobId);
			if (currJobData != null){
				jobMap.put(currJobData.getJobName(), currJobData);
			}
		}


		for (int teamIndex=0;teamIndex<dataList.size();teamIndex++){
			Team currTeam = (Team)dataList.get(teamIndex);
			List currTeamDayparts = currTeam.dayparts;

			for (int daypartIndex=0;daypartIndex<currTeamDayparts.size();daypartIndex++){
				Daypart currDaypart = (Daypart)currTeamDayparts.get(daypartIndex);
				List currTeamJobs = jobAccess.loadByTeamsWithOrderBy(new int[]{currTeam.id}, JobAccess.JOB_NAME_KEY);

				Set sortedParamJobs = jobMap.keySet();
				Iterator sortedParamJobsIter = sortedParamJobs.iterator();
				while(sortedParamJobsIter.hasNext()){
					String currParamJobName = (String) sortedParamJobsIter.next();

					//if we can find currParamJobName in currTeamJobs, then we can add it to the currDaypart
					for (int j=0;j<currTeamJobs.size();j++){
						if (currParamJobName.equals(((JobData)currTeamJobs.get(j)).getJobName())){
							JobData jobToAdd = (JobData)jobMap.get(currParamJobName);
							Job JobToAdd = new Job(jobToAdd.getJobId(), currParamJobName);
							JobToAdd.staffRequired = getStaffRequired(currTeam.id, currDaypart.id, JobToAdd.id, currTeam.startDate, currTeam.endDate);

							currDaypart.addJob(JobToAdd);
							log("Adding job: " + JobToAdd + "--- To daypart: " + currDaypart + "--- in team: " + currTeam);
							break;
						}
					}
				}
			}
		}

		return dataList;
	}

	/**
     * Given a dataList that has been set up with temas already, this method assigns dayparts
     * to the teams.
     *
     * @param dataList
     * @return
     * @throws SQLException
	 */
	private List buildDaypartList(List dataList) throws SQLException{
		TreeMap daypartMap = null;
		//depending on param, set the order of the day parts
		if (daypartsSortDir.equalsIgnoreCase("Descending")){
			daypartMap = new TreeMap(Collections.reverseOrder());
		} else {
			daypartMap = new TreeMap();
		}

		//fill up the sorted map with the param dayparts (key=daypart name value=DayPartData object)
		StringTokenizer daypartIdsTokenizer = new StringTokenizer(this.dayparts, ",");
		TeamDayPartSetAccess teamDaypartSetAccess = new TeamDayPartSetAccess(conn);
		while(daypartIdsTokenizer.hasMoreTokens()){
			int currDaypartId = Integer.parseInt(daypartIdsTokenizer.nextToken().trim());
			DayPartData currDaypartData = (DayPartData)this.allDayparts.get(new Integer(currDaypartId));
			if (currDaypartData != null){
				daypartMap.put(currDaypartData.getDpName(), currDaypartData);
			}
		}

		//go through each team in dataList and add the appropriate dayparts to each
		for (int i=0;i<dataList.size();i++){
			Team currTeam = (Team)dataList.get(i);
			List currTeamDayparts = teamDaypartSetAccess.loadDayParts(currTeam.id);

			Set sortedParamDayparts = daypartMap.keySet();
			Iterator sortedParamDaypartsIter = sortedParamDayparts.iterator();
			//run through each of the param dayparts (now sorted)
			while(sortedParamDaypartsIter.hasNext()){
				String currParamDaypart = (String)sortedParamDaypartsIter.next();
				//find the current param daypart in the team's dayparts
				for (int j=0;j<currTeamDayparts.size();j++){
					if (currParamDaypart.equals(((DayPartData)currTeamDayparts.get(j)).getDpName())){
						DayPartData daypartToAdd = (DayPartData)daypartMap.get(currParamDaypart);
                        Daypart DaypartToAdd =new Daypart(daypartToAdd.getDpId(), daypartToAdd.getDpName(), daypartToAdd.getDpStartTime(), daypartToAdd.getDpEndTime());
						currTeam.addDaypart(DaypartToAdd);
						log("Adding a Daypart: " + DaypartToAdd + " --- To: " + currTeam);
                        break;
					}
				}
			}
		}

		return dataList;
	}

	/**
	 * extracts teams names from Ids, keeping the team object in a map.
	 * sorts the object by team name and creates Team objects from them.
	 * All Team objects are then added to the
     *
     * @return
	 */
	private List buildTeamList() throws SQLException{
		TreeMap teamMap = new TreeMap();
		StringTokenizer teamIdsTokenizer = new StringTokenizer(this.teams, ",");

		while(teamIdsTokenizer.hasMoreTokens()){
			int currTeamId = Integer.parseInt(teamIdsTokenizer.nextToken().trim());
			WorkbrainTeamData tempWBTeamData = cm.getWBTeamById(currTeamId);
			if (tempWBTeamData != null){
				teamMap.put(tempWBTeamData.getWbtName(), tempWBTeamData);
			}
		}

		List returnList = new ArrayList();
		Iterator sortedTeamIterator = teamMap.keySet().iterator();
		while(sortedTeamIterator.hasNext()){
			String currKey = (String)sortedTeamIterator.next();
			WorkbrainTeamData currWBTeamData = (WorkbrainTeamData)teamMap.get(currKey);
			if (useTeamPayPeriod){
				List startAndEndDate = setStartEndDateToTeamsCurrSchedPeriod(currWBTeamData.getWbtId(), new Date());
				this.startDate = (Date) startAndEndDate.get(0);
				this.endDate = (Date) startAndEndDate.get(1);
			}
			Team currTeam = new Team(currWBTeamData.getWbtId(), currWBTeamData.getWbtName(), this.startDate, this.endDate);
            log("Adding team to structure: " + currTeam);
			returnList.add(currTeam);
		}

		return returnList;
	}

    /**
     * Rretrieves the staff requirements for teamId, daypartId, and jobId from startDate to endDate (inclusive)
     *
     * @param teamId            team to filter by
     * @param daypartId         day part to filter by
     * @param jobId             job to filter by
     * @param startDate         start of the date range
     * @param endDate           end of the date range
     * @return                  an array containing the staff requirements
     * @throws SQLException
     */
	private int[] getStaffRequired(int teamId, int daypartId, int jobId, Date startDate, Date endDate)
															throws SQLException {
		//range is inclusive, so +1
        int resultSize = DateHelper.getAbsDifferenceInDays(startDate, endDate)+1;
		int[] result = new int[resultSize];
		StringBuffer query = new StringBuffer();

		query
		.append("SELECT wbt_id, job_id, dp_id, srs_date, avg(srd_value) AS srs_value ")
		.append("FROM (")
		.append("SELECT wbt_id, job_id, dp_id, srs_date, srd_value ")
		.append("FROM Sr_Summary srs join Sr_Detail srd on srs.srs_id=srd.srs_id ")
		.append("WHERE srs.wbt_id = ").append(teamId).append(" AND ")
		.append("srd.dp_id = ").append(daypartId).append(" AND ")
		.append("srs.job_id = ").append(jobId).append(" AND ")
		.append("srs.srs_date BETWEEN ").append(conn.encodeDate(startDate)).append(" AND ").append(conn.encodeDate(endDate))
		//.append("AND act_id = 0 ") -- SH 40258
		.append("AND act_id = (select act_id from so_activity where act_name = '0') ")
		.append(") ")
		.append("GROUP BY wbt_id, job_id, dp_id, srs_date");

		log("Use for getting staff requirements: " + query.toString());

		PreparedStatement preStmnt = null;
		ResultSet rs = null;
		boolean rsEmpty = false;
		int resultIndex = 0;

		try{
			preStmnt = conn.prepareStatement(query.toString());
			rs = preStmnt.executeQuery();
			rsEmpty = !rs.next();
			Date tempDate = (Date)startDate.clone();

			while(resultIndex < resultSize && !rsEmpty){
				if(DateHelper.compare(tempDate, rs.getDate("srs_date")) == 0){
					result[resultIndex] = rs.getInt("srs_value");
					rsEmpty = !rs.next();
				}else{
					result[resultIndex] = 0;
				}

				tempDate = DateHelper.addDays(tempDate, 1);
				resultIndex ++;
			}
		}finally{
			SQLHelper.cleanUp(preStmnt, rs);

            //if there are no results, fill the array with default = 0
			if (rsEmpty){
				while(resultIndex < resultSize){
					result[resultIndex] = 0;
					resultIndex++;
				}
			}
		}

		return result;
	}

    /**
     * results a list, where
     * list.get(0) = the team's pay period start date on dateOfInterest
     * list.get(1) = the team's pay period start date on dateOfInterest
     *
     * @param wbtId             Team whose pay period start/end is being returned
     * @param dateOfInterest    The date for which the pay period start/end is returned
     * @return                  see method header
     * @throws SQLException
     */
	protected List setStartEndDateToTeamsCurrSchedPeriod(int wbtId, Date dateOfInterest) throws SQLException{
		List result = new ArrayList();

		TimePeriod schedPeriod = this.schedPeriodAccess.calculateSchedulePeriodDates(new int[]{wbtId}, dateOfInterest);
		result.add(schedPeriod.retrieveStartDate());
		result.add(schedPeriod.retrieveEndDate());


		return result;
	}


	/**
     * Makes sure that if any params is empty, it will be filled with the right info
     * @throws SQLException
	 */
	protected void fillParameterGaps() throws SQLException{
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		try{
            /*********************** TEAMS **********************/
			if (StringHelper.isEmpty(teams) || "ALL".equals(teams)){
				String teamsQuery = "SELECT wbt_id FROM sec_workbrain_team WHERE wbu_id="+userId;
                log("Teams param empty, so fill with: "+teamsQuery);
				stmnt = conn.prepareStatement(teamsQuery);
				rs = stmnt.executeQuery();
				this.teams = createCSListFromRS(rs, 1);
                log("Teams param empty, now it is: " + this.teams);
			}

            /*********************** DAY PARTS **********************/
			if (StringHelper.isEmpty(dayparts) || "ALL".equals(dayparts)){
				dayparts = "";
				//need to create a comma separated list of daypart ids based on teams selected.
				Map daypartsFound = new HashMap();

				StringTokenizer teamsTokenizer = new StringTokenizer(teams, ",");
				while(teamsTokenizer.hasMoreTokens()){
					try{
						int currTeamId = Integer.parseInt(teamsTokenizer.nextToken().trim());
						int currTeamDaypartSetId = StaffingCoverageHelper.getTeamDayPartSetId(currTeamId, conn);
						DayPartSetData currDaypartSetData = daypartSetAccess.load(currTeamDaypartSetId);

						List currDayparts = daypartAccess.loadByDpsetId(currDaypartSetData.getDpsetId());
						for (int j=0;j<currDayparts.size();j++){
							DayPartData currDaypartData = (DayPartData)currDayparts.get(j);
							if (daypartsFound.get(currDaypartData.getDpName()) == null){
								daypartsFound.put(currDaypartData.getDpName(), currDaypartData);
							}

						}
					}catch(NumberFormatException e){
                        logger.error("currTeamId is not an integers");
					}
				}

				//extract all of daypartsFound map, create Row objects from them and add them to rows list.
				Set daypartsFoundKeys = daypartsFound.keySet();
				Iterator keysIter = daypartsFoundKeys.iterator();
				while(keysIter.hasNext()){
					dayparts += ((DayPartData)daypartsFound.get(keysIter.next())).getDpId()+",";
				}

                log("Dayparts param empty, now it is: " + dayparts);
			}

            /*********************** JOBS **********************/
			if (StringHelper.isEmpty(jobs) || "ALL".equals(jobs)){
                String jobsQuery = getJobIdsQuery(teams,userId);
                log("Jobs param empty, fill with: " + jobsQuery);
				stmnt = conn.prepareStatement(jobsQuery);
				rs = stmnt.executeQuery();
				this.jobs = createCSListFromRS(rs, 1);
                log("Jobs param empty, now it is: " + this.jobs);
			}

            /*********************** EMPLOYEES **********************/
			if (StringHelper.isEmpty(emps) || "ALL".equals(emps)){
                String empsQuery = getEmpIdsQuery(teams,userId,startDate,endDate,conn);
                log("Employees param empty, fill with: " + empsQuery);
				stmnt = conn.prepareStatement(empsQuery);
				rs = stmnt.executeQuery();
				this.emps = createCSListFromRS(rs, 1);
                log("Employees param empty, now it is: " + this.emps);
			}

            /*********************** START & END DATES **********************/
			if (this.startDate == null && this.endDate == null){
				this.useTeamPayPeriod = true;
			    log("startDate & endDate params empty, so use teams' pay periods");
			}
		}finally{
			SQLHelper.cleanUp(stmnt, rs);
		}

	}

	/**
	 * creates a comma separated String from the indexth field of rs.
	 *
	 * @param rs           result set to be used
	 * @param index        index into rs's columns to use
	 * @return             a comma separated String from the indexth field of rs
	 */
	private String createCSListFromRS(ResultSet rs, int index) throws SQLException{
		StringBuffer csString = new StringBuffer();

		while (rs.next()) {
			csString.append(rs.getString(index)).append(",");
		}
		return csString.toString();
	}

    /**
     * Does exactly what it says.
     *
     * @return A map with key = daypart name, value = DayPartData objects
     * @throws SQLException
     */
	private Map retrieveAllDayparts() throws SQLException{
		Map result = new HashMap();
		String query = "SELECT * FROM Day_Part";
		PreparedStatement preStmnt = conn.prepareStatement(query);
		ResultSet rs = preStmnt.executeQuery();
		while(rs.next()){
			DayPartData currDpData = new DayPartData();
			currDpData.setDpId(rs.getInt("DP_ID"));
			currDpData.setDpName(rs.getString("DP_NAME"));
			currDpData.setDpDesc(rs.getString("DP_DESC"));
			currDpData.setDpStartTime(rs.getTimestamp("DP_START_TIME"));
			currDpData.setDpEndTime(rs.getTimestamp("DP_END_TIME"));
			result.put(new Integer(currDpData.getDpId()), currDpData);
		}
		return result;
	}

    /**
     * Builds a query for retrieving job information based on wbt_ids and userId
     *
     * @param wbt_ids       team ID's that are supposed to be associated with the jobs
     * @param userId        user ID of the person using the report
     * @return              see desc.
     */
	public static String getJobIdsQuery(String wbt_ids, int userId){
		StringBuffer query = new StringBuffer();

	    if (wbt_ids.equalsIgnoreCase("ALL") || StringHelper.isEmpty(wbt_ids)){
	      wbt_ids = "SELECT wbt_id FROM sec_workbrain_team WHERE wbu_id="+userId;
	    }

	    query
	    .append("SELECT UNIQUE j.job_id, j.job_name, j.job_desc ")
	    .append("FROM (Job j join Job_Team jt on j.job_id=jt.job_id) ")
	    .append("      join Workbrain_Team wt on jt.wbt_id=wt.wbt_id ")
	    .append("WHERE wt.wbt_id IN (").append(wbt_ids).append(") ");


	    return query.toString();
	}

    /**
     * Builds a query for retrieving employee information based on wbt_ids, userId, startDate and endDate
     *
     * @param wbt_ids       Employee must be member of one of these teams
     * @param userId        User with userId must have sufficient access rights to see the employee
     * @param startDate
     * @param endDate
     * @param conn
     * @return
     */
	public static String getEmpIdsQuery(String wbt_ids,int userId, Date startDate, Date endDate, DBConnection conn){
		StringBuffer query = new StringBuffer();

	    if (wbt_ids.equalsIgnoreCase("ALL") || StringHelper.isEmpty(wbt_ids)){
	      wbt_ids = "SELECT wbt_id FROM sec_workbrain_team WHERE wbu_id="+userId;
	    }

	    query
	   //.append("SELECT UNIQUE e.emp_id, e.emp_name, e.emp_fullname, t.wbt_name, et.empt_start_date, et.empt_end_date ")
	    .append("SELECT UNIQUE e.emp_id, e.emp_name, e.emp_fullname ")
	    .append("FROM (Employee e join Employee_Team et on e.emp_id=et.emp_id) join Workbrain_Team t on et.wbt_id=t.wbt_id ")
	    .append("WHERE t.wbt_id IN (").append(wbt_ids).append(")");

	    if (startDate != null && endDate != null){
	    	query
	    	.append("		AND (empt_end_date >= ").append(conn.encodeDate(startDate))
	    	.append("      	AND ").append(conn.encodeDate(endDate)).append(" >= empt_start_date)");
	    }


	    return query.toString();
	}

    /**
     * Does exactly what it says.
     *
     * @return map where key = tcode name, and value = tcode name abbreviation
     * @throws SQLException
     */
	public Map createTcodeMapper() throws SQLException{
		Map tcodeMapper = new HashMap();

		StringBuffer query = new StringBuffer();
		//HUB37034 tcode_udf4 check is only for printed legend, not printed schedule
		//query.append("SELECT tcode_name, tcode_udf2 FROM Time_Code WHERE tcode_udf4='Y'");

		//query.append("SELECT tcode_name, tcode_udf2 FROM Time_Code WHERE tcode_udf2 is not null");
		query.append("SELECT tcode_name, tcode_udf2, tcode_udf5 FROM Time_Code WHERE tcode_udf5 is null or tcode_udf5 != 'Y'");

		PreparedStatement ps = conn.prepareStatement(query.toString());
		ResultSet rs = ps.executeQuery();

		while(rs.next()){
			if(rs.getString("tcode_udf2") == null){
				tcodeMapper.put(rs.getString("tcode_name"), rs.getString("tcode_name"));
			}
			else{
				tcodeMapper.put(rs.getString("tcode_name"), rs.getString("tcode_udf2"));

			}
		}
        log("TcodeMapper: " + tcodeMapper);
		return tcodeMapper;
	}

	/**
	 * @return Returns the dayparts.
	 */
	public String getDayparts() {
		return dayparts;
	}

	/**
	 * @param dayparts The dayparts to set.
	 */
	public void setDayparts(String dayparts) {
		this.dayparts = dayparts;
	}

	/**
	 * @return Returns the emps.
	 */
	public String getEmps() {
		return emps;
	}

	/**
	 * @param emps The emps to set.
	 */
	public void setEmps(String emps) {
		this.emps = emps;
	}

	/**
	 * @return Returns the endDate.
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate The endDate to set.
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/**
	 * @return Returns the jobs.
	 */
	public String getJobs() {
		return jobs;
	}

	/**
	 * @param jobs The jobs to set.
	 */
	public void setJobs(String jobs) {
		this.jobs = jobs;
	}

	/**
	 * @return Returns the reportOutputFormat.
	 */
	public String getReportOutputFormat() {
		return reportOutputFormat;
	}

	/**
	 * @param reportOutputFormat The reportOutputFormat to set.
	 */
	public void setReportOutputFormat(String reportOutputFormat) {
		this.reportOutputFormat = reportOutputFormat;
	}

	/**
	 * @return Returns the startDate.
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate The startDate to set.
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return Returns the teams.
	 */
	public String getTeams() {
		return teams;
	}

	/**
	 * @param teams The teams to set.
	 */
	public void setTeams(String teams) {
		this.teams = teams;
	}

	/**
	 * @return Returns the user id.
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * @param userId The user idto set.
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}

	/**
	 * @return Returns the daypartsSortDir.
	 */
	public String getDaypartsSortDir() {
		return daypartsSortDir;
	}

	/**
	 * @param daypartsSortDir The daypartsSortDir to set.
	 */
	public void setDaypartsSortDir(String daypartsSortDir) {
		this.daypartsSortDir = daypartsSortDir;
	}

	/**
	 * @return Returns the jobSortDir.
	 */
	public String getJobsSortDir() {
		return jobsSortDir;
	}

	/**
	 * @param jobSortDir The jobSortDir to set.
	 */
	public void setJobsSortDir(String jobsSortDir) {
		this.jobsSortDir = jobsSortDir;
	}
	
    //HUB:42198  TT:2964
	public void setShowTotal(String isTotal) {
		this.showTotal = "TRUE".equalsIgnoreCase(isTotal);
	}
	public boolean getShowTotal() {
		return this.showTotal;
	}

    private void log(String msg) {
        if (logger.isDebugEnabled()) logger.debug(msg);
    }


}
