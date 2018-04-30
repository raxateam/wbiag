package com.wbiag.app.wbinterface.labor;

import java.sql.*;
import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.labor.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 *  JobImportTransactionSOExtra for job_team table
 *
 */
public class JobImportTransactionSOExtra extends JobImportTransaction  {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JobImportTransactionSOExtra.class);

    public final static String TYPE_JOB = "J";
    public final static String TYPE_RATE = "R";
    public final static String TYPE_TEAM = "T";

    public final static String JOB_TEAM_TABLE = "JOB_TEAM";

    public final static int TYPE = 0;
    public final static int JOB_NAME = 1;
    public final static int WBT_NAME = 2;
    public final static int JOBTEAM_DOT_THRESH = 3;
    public final static int JOBTEAM_WOT_THRESH = 4;
    public final static int JOBTEAM_AV_HR_RATE = 5;
    public final static int JOBTEAM_OT_MULT = 6;
    public final static int JOBTEAM_OT_INC = 7;
    public static String DATE_FMT = "yyyyMMdd";

    public JobImportTransactionSOExtra() {
    }



    /**
     *  Processes a given row from WBINT_IMPORT.
     *
     *@param  data                      Description of Parameter
     *@param  conn                      Description of Parameter
     *@exception  WBInterfaceException  Description of Exception
     *@exception  SQLException          Description of Exception
     */
    public void process(ImportData data, DBConnection conn)
             throws WBInterfaceException, SQLException {

        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("JobImportTransactionSOExtra.process");}
        this.conn = conn;
        this.data = data;
        try {
            preProcess(data , conn);
            if (data.getField(TYPE).equals(TYPE_JOB)) {
                jd = validateJobData();
                processJobData(jd);
                postProcess(data , jd, conn);
            }
            else if (data.getField(TYPE).equals(TYPE_RATE)) {
                jrd = validateJobRateData();
                processJobRateData(jrd);
                postProcess(data , jrd, conn);
            } else if (data.getField(TYPE).equals(TYPE_TEAM)) {
                JobTeamData jtd = validateJobTeamData();
                processJobTeamData(jtd);
                postProcess(data , jtd, conn);
            }
            else {
                throw new WBInterfaceException ("Record type not found : " + data.getField(TYPE));
            }
        }
        catch (Throwable t) {
            status = ImportData.STATUS_ERROR;
            // TT#24789 - the message was 'null'
            //message = t.getMessage();
            message = StringHelper.getStackTrace( t );
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(t,t);}
            WBInterfaceException.throwWBInterfaceException(t);
        }
    }


    protected JobTeamData validateJobTeamData()
            throws WBInterfaceException , SQLException {

        JobTeamData jtd = null;

        WBInterfaceUtil.checkForNull(data.getField(JOB_NAME) , "JOB_NAME cannot be null");
        JobData jd = getCodeMapper().getJobByName(data.getField(JOB_NAME).toUpperCase());
        if (jd == null) {
            throw new WBInterfaceException ("JOB_NAME not defined : "  + data.getField(JOB_NAME));
        }

        WBInterfaceUtil.checkForNull(data.getField(WBT_NAME) , "WBT_NAME cannot be null");
        WorkbrainTeamData wt = getCodeMapper().getWBTeamByName(data.getField(WBT_NAME).toUpperCase());
        if (wt == null) {
            throw new WBInterfaceException ("WBT_NAME not defined : "  + data.getField(WBT_NAME));
        }

        WBInterfaceUtil.checkForNull(data.getField(JOBTEAM_AV_HR_RATE) , "JOBTEAM_AV_HR_RATE cannot be null");
        double jobTeamAvrHrRate =
            WBInterfaceUtil.getDouble(data.getField(JOBTEAM_AV_HR_RATE) , "Error parsing JOBTEAM_AV_HR_RATE");
        WBInterfaceUtil.checkForNull(data.getField(JOBTEAM_DOT_THRESH) , "JOBTEAM_DOT_THRESH cannot be null");
        double jobTeamDotThresh =
            WBInterfaceUtil.getDouble(data.getField(JOBTEAM_DOT_THRESH) , "Error parsing JOBTEAM_DOT_THRESH");
        WBInterfaceUtil.checkForNull(data.getField(JOBTEAM_OT_INC) , "JOBTEAM_OT_INC cannot be null");
        double jobTeamOtInc =
            WBInterfaceUtil.getDouble(data.getField(JOBTEAM_OT_INC) , "Error parsing JOBTEAM_OT_INC");
        WBInterfaceUtil.checkForNull(data.getField(JOBTEAM_OT_MULT) , "JOBTEAM_OT_MULT cannot be null");
        double jobTeamOtMult =
            WBInterfaceUtil.getDouble(data.getField(JOBTEAM_OT_MULT) , "Error parsing JOBTEAM_OT_MULT");
        WBInterfaceUtil.checkForNull(data.getField(JOBTEAM_WOT_THRESH) , "JOBTEAM_WOT_THRESH cannot be null");
        double jobTeamWotThreash =
            WBInterfaceUtil.getDouble(data.getField(JOBTEAM_WOT_THRESH) , "Error parsing JOBTEAM_WOT_THRESH");

        jtd = loadJobTeamDataByJobId(conn ,jd.getJobId() , wt.getWbtId());

        if (jtd == null) {
            jtd = new JobTeamData();
            jtd.setJobId(jd.getJobId());
            jtd.setJobteamId(-1);
        }
        jtd.setWbtId(wt.getWbtId());
        jtd.setJobteamAvHrRate(jobTeamAvrHrRate);
        jtd.setJobteamDotThresh(jobTeamDotThresh);
        jtd.setJobteamOtInc(jobTeamOtInc);
        jtd.setJobteamOtMult(jobTeamOtMult);
        jtd.setJobteamWotThresh(jobTeamWotThreash);

        return jtd;
    }

    protected void processJobTeamData(JobTeamData jtd) throws SQLException  {
        if (jtd.getJobteamId() == -1) {
            jtd.setJobteamId(conn.getDBSequence("seq_jobteam_id").getNextValue());
            new RecordAccess(conn).insertRecordData(jtd , JOB_TEAM_TABLE);
            message = "Job Team INSERTED Successfully";
        } else {
            new RecordAccess(conn).updateRecordData(jtd , JOB_TEAM_TABLE , "jobteam_id");
            message = "Job Team UPDATED Successfully";
        }
    }

    protected static JobTeamData loadJobTeamDataByJobId(DBConnection conn ,
        int jobId , int wbtId) {
        List recs = new RecordAccess(conn).loadRecordData(new JobTeamData(), JOB_TEAM_TABLE,
                                           "job_id", jobId , "wbt_id", wbtId);
        if (recs.size() > 0) {
            return (JobTeamData)recs.get(0);
        }
        return null;
    }

    public static class JobTeamData extends RecordData {
        private int jobteamId;
        private int wbtId;
        private int jobId;
        private double jobteamDotThresh;
        private double jobteamWotThresh;
        private double jobteamAvHrRate;
        private double jobteamOtMult;
        private double jobteamOtInc;

        public RecordData newInstance() {
            return new JobTeamData ();
        }

        public int getJobteamId(){
            return jobteamId;
        }

        public void setJobteamId(int v){
            jobteamId=v;
        }

        public int getWbtId(){
            return wbtId;
        }
        public void setWbtId(int v){
            wbtId=v;
        }

        public int getJobId(){
            return jobId;
        }
        public void setJobId(int v){
            jobId=v;
        }

        public double getJobteamDotThresh(){
            return jobteamDotThresh;
        }

        public void setJobteamDotThresh(double v){
            jobteamDotThresh=v;
        }

        public double getJobteamWotThresh(){
            return jobteamWotThresh;
        }

        public void setJobteamWotThresh(double v){
            jobteamWotThresh=v;
        }

        public double getJobteamAvHrRate(){
            return jobteamAvHrRate;
        }

        public void setJobteamAvHrRate(double v){
            jobteamAvHrRate=v;
        }

        public double getJobteamOtMult(){
            return jobteamOtMult;
        }

        public void setJobteamOtMult(double v){
            jobteamOtMult=v;
        }

        public double getJobteamOtInc(){
            return jobteamOtInc;
        }

        public void setJobteamOtInc(double v){
            jobteamOtInc=v;
        }


        public String toString() {
            String s = "JobTeamData:\n" +
                "  jobteamId = " + jobteamId + "\n" +
                "  wbtId = " + wbtId + "\n" +
                "  jobId = " + jobId + "\n" +
                "  jobteamDotThresh = " + jobteamDotThresh + "\n" +
                "  jobteamWotThresh = " + jobteamWotThresh + "\n" +
                "  jobteamAvHrRate = " + jobteamAvHrRate + "\n" +
                "  jobteamOtMult = " + jobteamOtMult + "\n" +
                "  jobteamOtInc = " + jobteamOtInc + "\n" ;
            return s;
        }
    }

}

