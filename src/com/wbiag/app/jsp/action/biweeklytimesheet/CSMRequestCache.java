package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.sql.SQLException;

import com.workbrain.app.jsp.ActionContext;
import com.workbrain.app.jsp.action.timesheet.ElapsedTimeView;
import com.workbrain.app.jsp.action.timesheet.RequestCache;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.sql.DBConnection;

/**
 * @author vlo
 * This class is the exact replica as core's RequestCache class, except that
 * it returns a CSMElapsedTimeView instead of core's ElapsedTimeView.
 * ElapsedTimeView only handles elapsed time overrides.
 * CSMElapsedTimeView handles elapsed time overrides AND premiums.
 */
public class CSMRequestCache extends RequestCache {

	public static ElapsedTimeView getElapsedTimeView(DBConnection conn,
			ActionContext context, boolean doReload) throws SQLException {
		ElapsedTimeView elapsedTimeView = (ElapsedTimeView) context
				.getFromRequest("timesheet.elaspedTimeView");
		if (doReload || elapsedTimeView == null) {
			elapsedTimeView = new CSMElapsedTimeView(conn, CodeMapper
					.createCodeMapper(conn));
			context.setToRequest("timesheet.elapsedTimeView", elapsedTimeView);
		}

		return elapsedTimeView;
	}
}
