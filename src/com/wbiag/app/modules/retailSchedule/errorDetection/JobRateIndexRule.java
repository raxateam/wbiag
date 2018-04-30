package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

public class JobRateIndexRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(JobRateIndexRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}

	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List allEmployees = context.getAllEmployeesList();
		DBConnection conn = context.getConnection();
		SimpleDateFormat sdf = context.getDateFormat();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Job Rate Indexes");
		actionResult.setHelpTip("Ensure that Employee Job Rate Indexes can map to a job index in the job table");
		actionResult.setHelpDesc("The script will check consistency of this data and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.8...");
		
		int empjob_rate_index;
		java.util.Date empjob_start_date;
		java.util.Date empjob_end_date;
		String job_name;
		int jobrate_index;
		java.util.Date jobrate_effective_date;

		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		for(Iterator empIter = allEmployees.iterator(); empIter.hasNext();)
		{
			try {
				Employee employee = (Employee)empIter.next();
				if (employee.getEmpId() != -3) {
					String strSelect = "select ej.EMPJOB_RATE_INDEX, ej.EMPJOB_START_DATE, ej.EMPJOB_END_DATE, ";
					strSelect += "j.job_name, j.JOB_START_DATE, j.job_end_date, jr.jobrate_index, jr.JOBRATE_EFFECTIVE_DATE ";
					strSelect += "from employee_job ej, job j, job_rate jr where ej.JOB_ID = j.JOB_ID and j.JOB_ID = jr.JOB_ID ";
					strSelect += "and ej.emp_id = ? " ;
				
					stmt = conn.prepareStatement(strSelect);
					stmt.setInt(1 , employee.getEmpId());
					rs = stmt.executeQuery();
				
					while (rs.next()) {
						empjob_rate_index = rs.getInt("EMPJOB_RATE_INDEX");
						empjob_start_date = rs.getDate("EMPJOB_START_DATE");
						empjob_end_date = rs.getDate("EMPJOB_END_DATE");
					
						job_name = rs.getString("JOB_NAME");
						//job_start_date = rs.getDate("JOB_START_DATE");
						//job_end_date = rs.getDate("JOB_END_DATE");
					
						jobrate_index = rs.getInt("JOBRATE_INDEX");
						jobrate_effective_date = rs.getDate("JOBRATE_EFFECTIVE_DATE");

						//check empjob_rate_index equals jobrate_index
						if (empjob_rate_index == -999)
							result += "Job " + job_name + " has a NULL empjob_rate_index for employee " + employee.getWBEmployee().getEmpName() + "\n";
						else if (empjob_rate_index != jobrate_index)
							result += "Job " + job_name + " for employee " + employee.getWBEmployee().getEmpName() + " does not have empjob_rate_index = jobrate_index\n";

						//check dates
						if (!empjob_start_date.equals(DateHelper.DATE_1900) || !empjob_end_date.equals(DateHelper.DATE_3000))
							result += "FAILED: job " + job_name + " for employee " + employee.getWBEmployee().getEmpName() + " may not be dated correctly (" + sdf.format(empjob_start_date) + " - " + sdf.format(empjob_end_date) + ")\n";
						else if (!jobrate_effective_date.equals(DateHelper.DATE_1900))
							result += "FAILED: job " + job_name + " for job rate index " + jobrate_index + " may not be dated correctly (" + sdf.format(jobrate_effective_date) + ")\n";
					}
				} else {
            		if (logger.isDebugEnabled())
            			logger.debug("Workbrain Admin should not be in scheduled group");
                	
            		result += ", Workbrain Admin Should not be in scheduled group";
            	}
			}
			catch(SQLException e) {
				result = e.getMessage();
			}
			catch(RetailException e) {
				result = e.getMessage();
			}
		    finally
		    {
		        SQLHelper.cleanUp(rs);
		        SQLHelper.cleanUp(stmt);
		    }
		}
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.8");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Job Rate Indexes";
	}

}
