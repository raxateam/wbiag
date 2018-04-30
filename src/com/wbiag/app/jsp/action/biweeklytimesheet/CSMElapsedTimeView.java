package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.workbrain.app.jsp.action.timesheet.ElapsedTimeModel;
import com.workbrain.app.jsp.action.timesheet.ElapsedTimeView;
import com.workbrain.app.jsp.action.timesheet.StartStopTimeModel;
import com.workbrain.app.jsp.action.timesheet.StartStopTimeView;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.OverrideAccess;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.server.WorkbrainParametersRetriever;
import com.workbrain.sql.DBConnection;

/**
 * @author vlo
 *
 * Originally, ElapsedTimeView.loadDetailsFromOverride only loads
 * elapseTimeOverrides. However, CSM also wants to work with premium overrides
 * on the WeeklyTimesheet. Hence this class is created to override the
 * loadDetailsFromOverride method, such that it will load elapsed time
 * overrides(ovrtyp=600) as well as premium overrides(ovrtyp=200).
 */

public class CSMElapsedTimeView extends ElapsedTimeView {

	private DBConnection conn = null;

	public CSMElapsedTimeView(DBConnection conn, CodeMapper codeMapper) {
		super(conn, codeMapper);
		this.conn = conn;
	}

	protected Date loadDetailsFromOverride(int empId, Date date,
			List startStopTimes, List elapsedTimes, int shiftIndex)
			throws Exception {
		// Returned date is null when there is no elapsed time overrides,
		// will be Date(0) when no override specifies ovr start time.

		OverrideAccess ovrAcc = new OverrideAccess(conn);
		List ovrList = new ArrayList();

		Iterator itr_600 = ovrAcc.loadByRangeAndType(empId, date, date, 600,
				600).iterator();

		Iterator itr_200 = ovrAcc.loadByRangeAndType(empId, date, date, 200,
				200).iterator();

		while (itr_600.hasNext()) {
			OverrideData ovr = (OverrideData) itr_600.next();
			if (ovr.getShiftIndex() == shiftIndex) {
				ovrList.add(ovr);
			}
		}
		while (itr_200.hasNext()) {
			OverrideData ovr = (OverrideData) itr_200.next();
			if (ovr.getShiftIndex() == shiftIndex) {
				ovrList.add(ovr);
			}
		}

		if (ovrList.size() == 0) {
			return null;
		}

		// sortByOvrOrders( ovrList );
		Date startTime = ((OverrideData) ovrList.get(0)).getOvrStartTime();
		if (startTime == null) {
			startTime = new Date(0);
		}
		Iterator ovrs = ovrList.iterator();
		while (ovrs.hasNext()) {
			List fields = new ArrayList();
			for (int i = 0; i < getFieldNames().size(); i++) {
				fields.add("");
			}
			OverrideData ovr = (OverrideData) ovrs.next();
			if (ovr.getShiftIndex() == shiftIndex) {
				ElapsedTimeModel elapsedTime = null;
				if (ovr.retrieveIsTimeSheetStartStop()) {
					StartStopTimeModel ssTime = new StartStopTimeModel(
							StartStopTimeView.isBrk(ovr));
					ssTime.setStartTime(ovr.getOvrStartTime());
					ssTime.setStopTime(ovr.getOvrEndTime());
					elapsedTime = ssTime;
				} else {
					elapsedTime = new ElapsedTimeModel();
				}
				elapsedTime.setOvrId(ovr.getOvrId());

				Iterator ovrTokens = ovr.getNewOverrides().iterator();
				while (ovrTokens.hasNext()) {
					OverrideData.OverrideToken token = (OverrideData.OverrideToken) ovrTokens
							.next();
					String name = token.getName();
					if ("WRKD_MINUTES".equalsIgnoreCase(name)
							|| "WRKP_MINUTES".equalsIgnoreCase(name)) {
						elapsedTime.setMinutes(Integer.parseInt(token
								.getValue()));
					} else {
						int fieldIndex = getFieldIndex(name);
						if (fieldIndex != -1) {
							fields.set(fieldIndex, token.getValue());
						}
					}
				}

				elapsedTime.setFields(fields);
				if (elapsedTime instanceof StartStopTimeModel) {
					startStopTimes.add(elapsedTime);
				} else {
					elapsedTimes.add(elapsedTime);
				}
			}
		}
		return startTime;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.workbrain.app.jsp.action.timesheet.ElapsedTimeView#getFieldIndex(java.lang.String)
	 *      This method overrides ElapsedTimeView.getFieldIndex. The method
	 *      originally only searches through the list of elapsed time fields.
	 *      This method now also searches premium fields.
	 */
	public int getFieldIndex(String fieldName) throws Exception {
		// work premium field index is the same as work detail field index,
		// so just replace the string before the search
		fieldName = fieldName.replaceAll("WRKP", "WRKD");

		return super.getFieldIndex(fieldName);

	}

}
