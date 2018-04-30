package com.wbiag.app.modules.reports.hcprintschedule;

import com.workbrain.sql.DBConnection;

import java.io.ByteArrayOutputStream;
import java.security.InvalidParameterException;
import java.util.*;
import java.sql.SQLException;

/**
 * @author          Ali Ajellu
 * @version         1.0
 * Date:            Friday, June 16 206
 * Copyright:       Workbrain Corp.
 * TestTrack:       1630
 *
 * The report uses the MVC (Model, View, Contoller) model.
 *
 * - You're currently lookinag at the Controller portion of this model.
 * - PrintingScheduleModel is the class that constructs the
 *   Model portion. The model is a complex structure built from the following classes:
 *   Team -> Daypart -> Job -> Employee -> Schedule
 * - The View must conform to the PrintingScheduleView interface. There
 *   is currently one View defined for representing the report in the PDF format
 *   (PrintingScheduleViewPDF)
 *
 * To create a report, the generateReport(conn) method must be called. The controller
 * validates and formats the params before using the Model and View to generate the
 * report.
 *
 * The following instance variables must be set (using setter methods) before calling
 * generateReport(conn):
 * team,
 * daypart,
 * daypartSortDir,
 * job,
 * jobSortDir,
 * emp,
 * userId,
 * reportOutputFormat,
 * outputStream
 *
 * If any of the above are null, this will throw InvalidParameterException. Notice that
 * startDate and endDate are optional. That's because their absence means that a manual
 * date range is not being used and that each team's current schedule period will be
 * used.
 */
public class PrintingScheduleController {
	private static org.apache.log4j.Logger logger =
		org.apache.log4j.Logger.getLogger(PrintingScheduleController.class);
	protected String 	teams_p,
						dayparts_p,
						daypartsSortDir_p,
						jobs_p,
						jobsSortDir_p,
						emps_p,
						reportOutputFormat_p;
	protected Date		startDate_p,
						endDate_p;
	protected ByteArrayOutputStream baos;
	protected int		userId_p;

    /**
     * The main method that must be called for generating a report. Many variables must be
     * set before this method can be called. See class header for more info.
     *
     * @param conn An open, active db connectoin
     * @return An object containing the report. The type of this object depends on reportOutputFormat
     * @throws InvalidParameterException If any required vars have not been set or are null. Also thrown if
     *                                   reportOutputFormat set, is not supported. (currently only "PDF")
     * @throws SQLException If a querying error occures (e.g. conn == null)
     */
	public Object generateReport(DBConnection conn) throws InvalidParameterException, SQLException{
		log("Staring Controller");
		log("Received: " + teams_p + " " + startDate_p + " " + endDate_p + " " + dayparts_p
				+ " " + daypartsSortDir_p + " " + jobs_p + " " + jobsSortDir_p + " " + emps_p);

        boolean isParamsValid = validateParams();
        log("Are params valid? " + isParamsValid);
		if (isParamsValid){
			formatParams();

            PrintingScheduleModel model = new PrintingScheduleModel(conn);
			PrintingScheduleView view = null;

            if (reportOutputFormat_p.equals("PDF")){
				view = 	new PrintingScheduleViewPDF();
			}else{
				throw new InvalidParameterException("The report's output format can only be 'PDF'. You asked for "+reportOutputFormat_p);
			}

			//set all the model's params
			model.setTeams(teams_p);
			model.setDayparts(dayparts_p);
			model.setDaypartsSortDir(daypartsSortDir_p);
			model.setJobs(jobs_p);
			model.setJobsSortDir(jobsSortDir_p);
			model.setEmps(emps_p);
			model.setStartDate(startDate_p);
			model.setEndDate(endDate_p);

			log("Retrieving model");
			//retieve the data from the model
			List modelData = model.retrieveModelData();

			log("Retrieving report pdf");
			//retrieve the report from the view (given the data)
			Object generatedReport = view.retrieveReport(modelData, baos);

			return generatedReport;
		}else{
			throw new InvalidParameterException("One of the parameters is either not provided, or has an incorrect format.");
		}
	}

    /**
     * If any of teams_p, dayparts_p, jobs_p, or emp_p vars are "ALL",
     * empty them.
     */
	private void formatParams(){
		teams_p = teams_p.equals("ALL") ? "" : teams_p;
		dayparts_p = dayparts_p.equals("ALL") ? "" : dayparts_p;
		jobs_p = jobs_p.equals("ALL") ? "" : jobs_p;
		emps_p = emps_p.equals("ALL") ? "" : emps_p;
	}

    /**
     * Make sure all the required vars are not null
     * @return whether all vars have been set to something other than null
     */
	private boolean validateParams(){
		//start and end date can be null.
		boolean isAllParamsNonNull =
			teams_p != null &&
			dayparts_p != null &&
			jobs_p != null &&
			emps_p != null &&
			reportOutputFormat_p != null &&
			userId_p != 0 &&
			jobsSortDir_p != null &&
			daypartsSortDir_p != null;

		boolean finalResult = isAllParamsNonNull;

		return finalResult;
	}

	/**
	 * @return Returns the dayparts_p.
	 */
	public String getDayparts() {
		return dayparts_p;
	}

	/**
	 * @param dayparts_p The dayparts_p to set.
	 */
	public void setDayparts(String dayparts) {
		this.dayparts_p = dayparts;
	}

	/**
	 * @return Returns the emps_p.
	 */
	public String getEmps() {
		return emps_p;
	}

	/**
	 * @param emps_p The emps_p to set.
	 */
	public void setEmps(String emps) {
		this.emps_p = emps;
	}

	/**
	 * @return Returns the endDate_p.
	 */
	public Date getEndDate() {
		return endDate_p;
	}

	/**
	 * @param endDate_p The endDate_p to set.
	 */
	public void setEndDate(Date endDate) {
		this.endDate_p = endDate;
	}

	/**
	 * @return Returns the jobs_p.
	 */
	public String getJobs() {
		return jobs_p;
	}

	/**
	 * @param jobs_p The jobs_p to set.
	 */
	public void setJobs(String jobs) {
		this.jobs_p = jobs;
	}

	/**
	 * @return Returns the reportOutputFormat_p.
	 */
	public String getReportOutputFormat() {
		return reportOutputFormat_p;
	}

	/**
	 * @param reportOutputFormat_p The reportOutputFormat_p to set.
	 */
	public void setReportOutputFormat(String reportOutputFormat) {
		this.reportOutputFormat_p = reportOutputFormat;
	}

	/**
	 * @return Returns the startDate_p.
	 */
	public Date getStartDate() {
		return startDate_p;
	}

	/**
	 * @param startDate_p The startDate_p to set.
	 */
	public void setStartDate(Date startDate) {
		this.startDate_p = startDate;
	}

	/**
	 * @return Returns the teams_p.
	 */
	public String getTeams() {
		return teams_p;
	}

	/**
	 * @param teams_p The teams_p to set.
	 */
	public void setTeams(String teams) {
		this.teams_p = teams;
	}

	/**
	 * @return Returns the userId.
	 */
	public int getUserId() {
		return userId_p;
	}

	/**
	 * @param teams_p The teams_p to set.
	 */
	public void setUserId(int userId) {
		this.userId_p= userId;
	}

	/**
	 * @return Returns the daypartsSortDir.
	 */
	public String getDaypartsSortDir() {
		return daypartsSortDir_p;
	}

	/**
	 * @param daypartsSortDir The daypartsSortDir to set.
	 */
	public void setDaypartsSortDir(String daypartsSortDir) {
		this.daypartsSortDir_p = daypartsSortDir;
	}

	/**
	 * @return Returns the jobsSortDir.
	 */
	public String getJobsSortDir() {
		return jobsSortDir_p;
	}

	/**
	 * @param jobsSortDir The jobsSortDir to set.
	 */
	public void setJobsSortDir(String jobsSortDir) {
		this.jobsSortDir_p = jobsSortDir;
	}

	public ByteArrayOutputStream getOutputStream() {
		return baos;
	}

	public void setOutputStream(ByteArrayOutputStream baos) {
		this.baos = baos;
	}

    private void log(String msg) {
        if (logger.isDebugEnabled()) log(msg);
    }


}
