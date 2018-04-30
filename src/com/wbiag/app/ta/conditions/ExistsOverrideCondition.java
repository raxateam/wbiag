package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.model.PayGroupData;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;

/**
 * Checks to see if there is an override with an Id in the given
 * range, and in Applied or Pending state.
 *
 * @author bviveiros
 * @see  com.workbrain.app.ta.ruleengine.Condition
 */
public class ExistsOverrideCondition extends Condition {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ExistsOverrideCondition.class);

	public static final String PARAM_RANGE_START = "OverrideIdRangeStart";
    public static final String PARAM_RANGE_END = "OverrideIdRangeEnd";
    public static final String PARAM_UNIT_PERIOD = "UnitPeriod";
    public static final String PARAM_UNIT_VALUE_START = "UnitValueStart";
    public static final String PARAM_UNIT_VALUE_END = "UnitValueEnd";
    public static final String PARAM_VAL_WORK_DATE = "W";
    public static final String PARAM_NEW_VALUE_TOKEN_NAME = "NewValueTokenName";
    public static final String PARAM_NEW_VALUE_TOKEN_VALUES = "NewValueTokenValues";

	public ExistsOverrideCondition() {
    }

    /**
     *
     * @param conn - database Connection Object
     * @return List - a list of RuleParameterInfo instances that describe the parameters used by the rule component
     *
     */
    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_RANGE_START,
                                         RuleParameterInfo.STRING_TYPE , false));
        result.add(new RuleParameterInfo(PARAM_RANGE_END,
                                         RuleParameterInfo.STRING_TYPE , false));
        RuleParameterInfo rpiPer = new RuleParameterInfo(PARAM_UNIT_PERIOD,
            RuleParameterInfo.CHOICE_TYPE);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_DAY);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_WEEK);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_PAYPERIOD);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_MONTH);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_QTR);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_YEAR);
        result.add(rpiPer);
        result.add(new RuleParameterInfo(PARAM_UNIT_VALUE_START, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_UNIT_VALUE_END, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_NEW_VALUE_TOKEN_NAME, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_NEW_VALUE_TOKEN_VALUES, RuleParameterInfo.STRING_TYPE));

        return result;
    }

    /**
     * Returns the name of the Rule
     *
     * @return String - the unique name of this rule component
     */
    public String getComponentName() {
        return "WBIAG: Exists Override Condition";
    }

    /**
     * Checks if there are overrides in given date range based on parameters
     *
     * @param wbData - all the information <BR>
     *  parameters - RuleData parameters
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {

        int ovrtypIdStart = parameters.getIntegerParameter(PARAM_RANGE_START );
        int ovrtypIdEnd = parameters.getIntegerParameter(PARAM_RANGE_END );

        // *** resolve start/end dates
        StartEndDate ste = getStarEndDate(wbData , parameters);
        if (logger.isDebugEnabled()) logger.debug("Checking overrides from : " + ste.start  + " to : " + ste.end + " for ovrtypes :" + ovrtypIdStart + "-" + ovrtypIdEnd);

        boolean found = false;
   		OverrideList ovrs=  wbData.getOverridesAppliedRange(
    					ste.start,
						ste.end,
						ovrtypIdStart,
						ovrtypIdEnd);
        if (logger.isDebugEnabled()) logger.debug("Found " + ovrs.size() + " override(s)");

        String ovrToken = parameters.getParameter(PARAM_NEW_VALUE_TOKEN_NAME );
        String ovrValues = parameters.getParameter(PARAM_NEW_VALUE_TOKEN_VALUES );
        if (!StringHelper.isEmpty(ovrToken) && ovrs.size() > 0) {
            Iterator iter = ovrs.iterator();
            while (iter.hasNext()) {
                OverrideData item = (OverrideData)iter.next();
                OverrideData.OverrideToken tok = item.getNewOverrideByName(ovrToken);
                if (tok != null) {
                    boolean tokCheck = RuleHelper.isCodeInList(ovrValues , tok.getValue());
                    if (!tokCheck) {
                        if (logger.isDebugEnabled()) logger.debug("OvrNewValue : " + item.getOvrNewValue() + " is not eligible for given token parameters");;
                        iter.remove();
                    }
                }
            }
        }
        found = ovrs.size() > 0;
        return found;
    }

    private StartEndDate getStarEndDate(WBData wbData , Parameters parameters) throws Exception {
        String punitValS = parameters.getParameter(PARAM_UNIT_VALUE_START , "0");
        String punitValE = parameters.getParameter(PARAM_UNIT_VALUE_END , "0");
        String unitPeriod = parameters.getParameter(PARAM_UNIT_PERIOD , DateHelper.APPLY_ON_UNIT_DAY);

        Date start = null , end = null;
        int unitValS = 0, unitValE = 0;
        if (PARAM_VAL_WORK_DATE.equals(punitValS)) {
            start = wbData.getWrksWorkDate();
        }
        else if (!StringHelper.isEmpty(punitValS)) {
            try {
                unitValS = Integer.parseInt(punitValS);
            }
            catch (NumberFormatException ex) {
                throw new RuleEngineException (PARAM_UNIT_VALUE_START + " must be integer or W");
            }
        }
        if (PARAM_VAL_WORK_DATE.equals(punitValE)) {
            end = wbData.getWrksWorkDate();
        }
        else if (!StringHelper.isEmpty(punitValE)) {
            try {
                unitValE = Integer.parseInt(punitValE);
            }
            catch (NumberFormatException ex) {
                throw new RuleEngineException (PARAM_UNIT_VALUE_END + " must be integer or W");
            }
        }

        if (start == null) {
            start = getUnit(wbData, unitPeriod, DateHelper.APPLY_ON_FIRST_DAY,
                            unitValS);
        }
        if (end == null) {
            end = getUnit(wbData, unitPeriod, DateHelper.APPLY_ON_LAST_DAY,
                          unitValE);
        }
        return new StartEndDate(start , end) ;
    }

    private Date getUnit(WBData wbData, String applyOnUnit ,
                           String applyOnValue, int unitVal) {
        Calendar c = DateHelper.getCalendar();

        if (DateHelper.APPLY_ON_UNIT_DAY.equals(applyOnUnit)) {
            c.setTime(wbData.getWrksWorkDate());
            c.add(Calendar.DATE  , unitVal);
        }
        else if (DateHelper.APPLY_ON_UNIT_MONTH.equals(applyOnUnit)) {
            c.setTime(DateHelper.getUnitMonth(applyOnValue , false, wbData.getWrksWorkDate()));
            c.add(Calendar.MONTH , unitVal);
        }
        else if (DateHelper.APPLY_ON_UNIT_PAYPERIOD.equals(applyOnUnit)) {
            PayGroupData pgd = wbData.getRuleData().getCodeMapper().getPayGroupById(wbData.getPaygrpId());
            int payPeriod = DateHelper.getDifferenceInDays( pgd.getPaygrpEndDate(),
                pgd.getPaygrpStartDate()) + 1;
            c.setTime( DateHelper.getUnitPayPeriod(applyOnValue , false, wbData.getWrksWorkDate() , pgd));
            c.add( Calendar.DAY_OF_YEAR, payPeriod * unitVal );
        }
        else if (DateHelper.APPLY_ON_UNIT_QTR.equals(applyOnUnit)) {
            c.setTime(DateHelper.getUnitQtr(applyOnValue , false, wbData.getWrksWorkDate()));
            c.add(Calendar.MONTH  , 3 * unitVal);
        }
        else if (DateHelper.APPLY_ON_UNIT_YEAR.equals(applyOnUnit)) {
            c.setTime(DateHelper.getUnitYear(applyOnValue , false, wbData.getWrksWorkDate()));
            c.add(Calendar.YEAR  , unitVal);
        }
        else if (DateHelper.APPLY_ON_UNIT_WEEK.equals(applyOnUnit)) {
            c.setTime(DateHelper.getUnitWeek(applyOnValue , false, wbData.getWrksWorkDate()));
            c.add(Calendar.WEEK_OF_YEAR  , unitVal);
        }
        else {
            throw new RuntimeException("ApplyonUnit not supported : " + applyOnUnit);
        }
        return new Datetime(c.getTimeInMillis());
    }

    class StartEndDate {

        Date start;
        Date end;

        public StartEndDate(Date start , Date end) {
            this.start = start;
            this.end = end;
        }

    }
}
