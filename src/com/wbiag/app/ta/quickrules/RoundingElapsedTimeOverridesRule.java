package com.wbiag.app.ta.quickrules;

import java.util.ArrayList;
import java.util.List;

import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.OverrideList;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.ruleengine.CalcDataCache;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

public class RoundingElapsedTimeOverridesRule extends Rule {

	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger
			.getLogger(RoundingElapsedTimeOverridesRule.class);

	public static final String PARAM_MULTIPLE = "Multiple";
	public static final String PARAM_SPLIT = "Split";

	public RoundingElapsedTimeOverridesRule() {
		super();
	}

	public void execute(WBData wbData, Parameters param) throws Exception {
		OverrideList elapseTimeOvrList = wbData
				.getRuleData().getCalcDataCache()
				.getTempOverrides(wbData.getEmpId(), wbData.getWrksWorkDate()  )
				.filter(
						wbData.getRuleData().getWorkSummary().getWrksWorkDate(),
						wbData.getRuleData().getWorkSummary().getWrksWorkDate(),
						OverrideData.PENDING_APPLIED,
						OverrideData.TIMESHEET_TYPE_START,
						OverrideData.TIMESHEET_TYPE_START);


        int multiple = param.getIntegerParameter(PARAM_MULTIPLE);
        int split = param.getIntegerParameter(PARAM_SPLIT);

		for (int i = 0, j = elapseTimeOvrList.size(); i < j; i++) {
			OverrideData overrideData = elapseTimeOvrList.getOverrideData(i);
			if (overrideData != null) {
				String wrkType = WorkDetailData.WRKD_MINUTES;

				OverrideData.OverrideToken token = overrideData
						.getNewOverrideByName(WorkDetailData.WRKD_MINUTES);

				if (!StringHelper.isEmpty(token.getValue())) {
                    String value = token.getValue();
                    int rawMinutes = Integer.parseInt(value);
                    int roundedMinutes = DateHelper.roundDuration(
                        rawMinutes, multiple, split);
                    String ovrValue = overrideData.getOvrNewValue();
                    String newOvrValue = ovrValue.replaceAll(wrkType + "="
                        + rawMinutes, wrkType + "=" + roundedMinutes);
                    overrideData.setOvrNewValue(newOvrValue);
                    if (logger.isDebugEnabled()) logger.debug("Rounded ovrNewValue from : " + ovrValue + " to : " + newOvrValue);
				}

			}
		}

	}

	public List getParameterInfo(DBConnection conn) {
		List paramList = new ArrayList();
		paramList.add(new RuleParameterInfo(PARAM_MULTIPLE,	RuleParameterInfo.INT_TYPE, false));
		paramList.add(new RuleParameterInfo(PARAM_SPLIT, RuleParameterInfo.INT_TYPE, false));
		return paramList;
	}

	public String getComponentName() {
		return "WBIAG: Round Elapsed Time Overrides Rule";
	}

}
