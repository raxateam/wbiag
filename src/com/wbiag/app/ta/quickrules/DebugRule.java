package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.server.registry.*;

/**
 * Adds messages to the Workbrain CALC_LOG table. Note that calcLog records
 * are saved to database by CDataEventCalcLog dataevent due to below reasons
 *   <ul>
 *   <li> as of 4.1, work summary is not saved to DB when creating def records, therefore trying
 *      to create records when no work summary is present creates errorr
 *   <li> optimized insert process by batching it through RecordAccess
 *   </ul>
 * @author  Charles Xu, Brian Viveiros
 */
public class DebugRule extends Rule{

	private static Logger logger = Logger.getLogger( DebugRule.class );

	public static final String PARAM_ADD_WORK_PREMIUM_FIELDS = "AdditionalWorkPremiumFields";
	public static final String PARAM_ADD_WORK_DETAIL_FIELDS = "AdditionalWorkDetailFields";
	public static final String PARAM_ADD_WORK_SUMMARY_FIELDS = "AdditionalWorkSummaryFields";
	public static final String PARAM_LOG_PREMIUMS = "LogWorkPremiums";
	public static final String PARAM_LOG_DETAILS = "LogWorkDetails";
	public static final String PARAM_LOG_WORK_SUMMARY = "LogWorkSummary";
	public static final String PARAM_MESSAGE = "Message";
	public static final String PARAM_CLEAR_LOG = "ClearLog";
	public static final String PARAM_LOG_BALANCE_NAMES = "LogBalanceNames";

	public static final String PARAM_VALUE_FALSE = "FALSE";
	public static final String PARAM_VAL_TRUE = "TRUE";

	public static final String DEFAULT_WORK_SUMMARY_FIELDS = "wrksWorkDate,wrksAuthorized,wrksAuthBy";
	public static final String DEFAULT_WORK_DETAIL_FIELDS = "wrkdStartTime,wrkdEndTime,wrkdMinutes,wrkdTcodeName,wrkdHtypeName";
	public static final String DEFAULT_WORK_PREMIUM_FIELDS = "wrkdMinutes,wrkdTcodeName,wrkdHtypeName";

    public static final String REG_CALC_LOG_ENABLED = "system/wbiag/CALC_LOG_ENABLED";
    public static final String  CALC_LOG_CACHE_NAME = "CALC_LOG";
    /**
     * Component Name
     */
    public String getComponentName() {
        return "WBIAG: Debug Rule";
    }

    /**
     * getParameterInfo
     */
    public List getParameterInfo(DBConnection dBConnection) {

    	List results = new ArrayList();

    	RuleParameterInfo rpInfo = new RuleParameterInfo(PARAM_CLEAR_LOG, RuleParameterInfo.CHOICE_TYPE, false);
        rpInfo.addChoice(PARAM_VAL_TRUE);
        rpInfo.addChoice(PARAM_VALUE_FALSE);
        rpInfo.setDefaultValue(PARAM_VALUE_FALSE);
        results.add(rpInfo);

        results.add(new RuleParameterInfo(PARAM_MESSAGE, RuleParameterInfo.STRING_TYPE, true));

        rpInfo = new RuleParameterInfo(PARAM_LOG_WORK_SUMMARY, RuleParameterInfo.CHOICE_TYPE, true);
        rpInfo.addChoice(PARAM_VAL_TRUE);
        rpInfo.addChoice(PARAM_VALUE_FALSE);
        rpInfo.setDefaultValue(PARAM_VALUE_FALSE);
        results.add(rpInfo);

        results.add(new RuleParameterInfo(PARAM_ADD_WORK_SUMMARY_FIELDS, RuleParameterInfo.STRING_TYPE, true));

        rpInfo = new RuleParameterInfo(PARAM_LOG_DETAILS, RuleParameterInfo.CHOICE_TYPE, true);
        rpInfo.addChoice(PARAM_VAL_TRUE);
        rpInfo.addChoice(PARAM_VALUE_FALSE);
        rpInfo.setDefaultValue(PARAM_VALUE_FALSE);
        results.add(rpInfo);

        results.add(new RuleParameterInfo(PARAM_ADD_WORK_DETAIL_FIELDS, RuleParameterInfo.STRING_TYPE, true));

        rpInfo = new RuleParameterInfo(PARAM_LOG_PREMIUMS, RuleParameterInfo.CHOICE_TYPE, true);
        rpInfo.addChoice(PARAM_VAL_TRUE);
        rpInfo.addChoice(PARAM_VALUE_FALSE);
        rpInfo.setDefaultValue(PARAM_VALUE_FALSE);
        results.add(rpInfo);

        results.add(new RuleParameterInfo(PARAM_ADD_WORK_PREMIUM_FIELDS, RuleParameterInfo.STRING_TYPE, true));

        results.add(new RuleParameterInfo(PARAM_LOG_BALANCE_NAMES, RuleParameterInfo.STRING_TYPE, true));


        return results;
    }

    /**
     * execute.
     */
    public void execute(WBData wbData, Parameters parameters) throws Exception {

        boolean isCalclogEnabled = Registry.getVarBoolean(REG_CALC_LOG_ENABLED, true);
        if (!isCalclogEnabled) {
            if (logger.isDebugEnabled()) logger.debug("Calc_log was not enabled by registry entry : " + REG_CALC_LOG_ENABLED);
            return;
        }
	    boolean clearLog = Boolean.valueOf(parameters.getParameter(PARAM_CLEAR_LOG, PARAM_VALUE_FALSE)).booleanValue();
	    String logMessage = parameters.getParameter(PARAM_MESSAGE, null);
	    boolean showWorkSummary = Boolean.valueOf(parameters.getParameter(PARAM_LOG_WORK_SUMMARY, PARAM_VALUE_FALSE)).booleanValue();
	    boolean showDetails = Boolean.valueOf(parameters.getParameter(PARAM_LOG_DETAILS, PARAM_VALUE_FALSE)).booleanValue();
	    boolean showPremiums = Boolean.valueOf(parameters.getParameter(PARAM_LOG_PREMIUMS, PARAM_VALUE_FALSE)).booleanValue();
	    String addWrksFields = parameters.getParameter(PARAM_ADD_WORK_SUMMARY_FIELDS, null);
	    String addWrkdFields = parameters.getParameter(PARAM_ADD_WORK_DETAIL_FIELDS, null);
	    String addWrkpFields = parameters.getParameter(PARAM_ADD_WORK_PREMIUM_FIELDS, null);
        String balanceNames  = parameters.getParameter(PARAM_LOG_BALANCE_NAMES, null);

	    int wrksId = wbData.getRuleData().getWorkSummary().getWrksId();
        List calcLogs = getCalcLogs(wbData , wrksId);
        // Check if we want to clear the log for the work summary record.
        if (clearLog) {
            clearCalcLog(calcLogs, wrksId);
        }

        // If a message was specified in the rule parameters then log it.
        if (logMessage != null) {
            addCalcLog(calcLogs, wrksId, logMessage);
        }

        // Log the work summary.
        if (showWorkSummary) {
            showWorkSummary(wbData, calcLogs, addWrksFields);
        }

        // Log the work details.
        if (showDetails) {
            showWorkDetails(wbData, calcLogs, addWrkdFields);
        }

        // If premiums are enabled the log them.
        if (showPremiums) {
            showPremiums(wbData, calcLogs, addWrkpFields);
        }

        if (!StringHelper.isEmpty(balanceNames)) {
            showBalances(wbData, calcLogs, balanceNames);
        }
    }

    /**
     * Deletes any entries in the calc_log table for the given work summary id.
     *
     * @param conn
     * @param wrksId
     * @throws SQLException
     */
    protected void clearCalcLog(List calcLogs, int wrksId)
    {
        calcLogs.clear();
    }

    /**
     * Inserts an entry to the calc_log table.
     *
     * @param conn
     * @param wrksId
     * @param msg
     * @throws SQLException
     */
    protected void addCalcLog(List calcLogs, int wrksId, String msg)
    {
        CalcLogData cld = new CalcLogData();
        cld.setWrksId(wrksId);
        cld.setCalclogMessage(msg);
        cld.setCalclogDate(new java.util.Date());
        cld.setGeneratesPrimaryKeyValue(true);
        calcLogs.add(cld) ;
    }

    private final String INSERT_CALC_LOG_SQL = "INSERT INTO calc_log (calclog_id, wrks_id, calclog_date, calclog_message) VALUES (?, ?, ?, ?)";


    /**
     * Generates a log message from the work summary, then logs the message.
     *
     * The next entry is "startTime-endTime (minutes) tcode htype".  If showLabourMetrics
     * was set to true then Rate, Job, Dept, Proj, Dock are appended.  If otherWorkDetailFields
     * where specified, then they are appended.
     *
     * @param wbData
     * @param msg
     * @param showLabourMetrics
     * @throws SQLException
     */
    protected void showWorkSummary(WBData wbData,
                                   List calcLogs,
                                   String otherFields)
    {
        RuleData ruleData = wbData.getRuleData();
        int wrksId = ruleData.getWorkSummary().getWrksId();
        StringBuffer wdString = null;
        StringBuffer columnList = null;

        WorkSummaryData ws = ruleData.getWorkSummary();

        // Log a message representing the work summary.

    	// Begin with the standard fields.
    	// startTime, endTime, minutes, tcode, htype.
    	columnList = new StringBuffer(DEFAULT_WORK_SUMMARY_FIELDS);

    	// Append other columns
    	if (otherFields != null) {
    		columnList.append(",");
    		columnList.append(otherFields);
    	}

    	// Generate the log message.
    	wdString = new StringBuffer("Work Summary: ");
    	wdString.append(getFieldData(ws, columnList.toString()));

    	// Insert the message to the log table.
        addCalcLog(calcLogs, wrksId, wdString.toString());
    }

    /**
     * Generates a log message from the work details, then logs the message.
     *
     * The first entry is the message specified in the rule parameter (if not null).
     *
     * The next entry is "startTime-endTime (minutes) tcode htype".  If showLabourMetrics
     * was set to true then Rate, Job, Dept, Proj, Dock are appended.  If otherWorkDetailFields
     * where specified, then they are appended.
     *
     * @param wbData
     * @param msg
     * @param showLabourMetrics
     * @throws SQLException
     */
    protected void showWorkDetails(WBData wbData,
                                   List calcLogs,
                                   String otherFields)
    {
        RuleData ruleData = wbData.getRuleData();
        int wrksId = ruleData.getWorkSummary().getWrksId();
        StringBuffer wdString = null;
        StringBuffer columnList = null;

        WorkDetailList wdList = ruleData.getWorkDetails();
        WorkDetailData wd = null;

        // Log a message representing each work detail.
        for (int i = 0; i < wdList.size(); i++) {

        	wd = (WorkDetailData) wdList.get(i);

        	// Begin with the standard work detail fields.
        	// startTime, endTime, minutes, tcode, htype.
        	columnList = new StringBuffer(DEFAULT_WORK_DETAIL_FIELDS);

        	// Append other columns
        	if (otherFields != null) {
        		columnList.append(",");
        		columnList.append(otherFields);
        	}

        	// Generate the log message.
        	wdString = new StringBuffer("Work Detail: ");
        	wdString.append(getFieldData(wd, columnList.toString()));

        	// Insert the message to the log table.
            addCalcLog(calcLogs, wrksId, wdString.toString());
        }
    }

    /**
     * Generates a log message from the work premiums, then logs the message.
     *
     * The first entry is the message specified in the rule parameter (if not null).
     *
     * The next entry is "Premium-(minutes) tcode htype".  If showLabourMetrics
     * was set to true then Rate, Job, Dept, Proj, Dock are appended.  If otherWorkDetailFields
     * where specified, then they are appended.
     *
     * @param wbData
     * @param msg
     * @param showLabourMetrics
     * @throws SQLException
     */
    protected void showPremiums(WBData wbData,
                                List calcLogs,
                                String otherFields)
    {
        RuleData ruleData = wbData.getRuleData();
        int wrksId = ruleData.getWorkSummary().getWrksId();
        StringBuffer wdString = null;
        StringBuffer columnList = null;

        WorkDetailList wdList = ruleData.getWorkPremiums();
        WorkDetailData wp = null;

        // Log a message representing each work detail.
        for (int i = 0; i < wdList.size(); i++) {

        	wp = (WorkDetailData) wdList.get(i);

        	// Begin with the standard work detail fields.
        	// startTime, endTime, minutes, tcode, htype.
        	columnList = new StringBuffer(DEFAULT_WORK_PREMIUM_FIELDS);

        	// Append other columns
        	if (otherFields != null) {
        		columnList.append(",");
        		columnList.append(otherFields);
        	}

        	// Generate the log message.
        	wdString = new StringBuffer("Work Premium: ");
        	wdString.append(getFieldData(wp, columnList.toString()));

        	// Insert the message to the log table.
            addCalcLog(calcLogs, wrksId, wdString.toString());
        }
    }

    /**
     * Adds balance values for given balances
     *
     * @param wbData
     * @param calcLogs
     * @param balNames
     * @throws Exception
     */
    protected void showBalances (WBData wbData,
                                List calcLogs,
                                String balNames) throws Exception {
       String[] bals = StringHelper.detokenizeString(balNames, ",");
       int wrksId = wbData.getRuleData().getWorkSummary().getWrksId();
       for (int i = 0; i < bals.length; i++) {
           String val = "Balance : " + bals[i] + ", value : " + wbData.getEmployeeBalanceValue(bals[i]);
           addCalcLog(calcLogs, wrksId, val);
       }

   }

    /**
     * Given a csv of field names, return a string in the
     * format "COLUMN_NAME1=value1,COLUMN_NAME2=value2"
     *
     * @param wd
     * @param columns
     * @return
     */
    protected String getFieldData(RecordData record, String fields) {

    	StringTokenizer tokenizer = new StringTokenizer(fields, ",");
    	StringBuffer fieldData = new StringBuffer();
    	String fieldName = null;
    	String fieldValue = null;

    	while (tokenizer.hasMoreTokens()) {

    		fieldName = tokenizer.nextToken();

    		try {
	    		fieldValue = String.valueOf(record.getProperty(fieldName));

	    		fieldData.append(fieldName);
	    		fieldData.append("=");
	    		fieldData.append(fieldValue);
	    		fieldData.append(", ");

    		} catch (Exception e) {
    			// Continue to the next token.
    			if (logger.isEnabledFor(Priority.ERROR)) {
    				logger.error("fieldName not found: " + fieldName);
    			}
    		}
    	}

    	return fieldData.toString();
    }

    /**
     * Returns the logs for this WrksId from CalcDataCache. This log is flushed to database
     *  by dataevent CDataEventCalcLog. See Javadoc for this class for explanation
     * @param wbData
     * @param wrksId
     * @return
     */
    private List getCalcLogs(WBData wbData, int wrksId) {
        List ret =null;
        TreeMap calcLogsMap = (TreeMap)wbData.getRuleData().getCalcDataCache()
            .getEntityCache().get(CALC_LOG_CACHE_NAME);
        Integer key = new Integer(wrksId);
        if (calcLogsMap ==  null) {
            calcLogsMap = new TreeMap();
            wbData.getRuleData().getCalcDataCache().getEntityCache()
                .put(CALC_LOG_CACHE_NAME, calcLogsMap);
        }
        List logs = (List)calcLogsMap.get(key);
        if (logs == null) {
            logs = new ArrayList();
            calcLogsMap.put(key,logs );
        }

        ret = (List)calcLogsMap.get(key);
        return ret;
    }
}
