package com.wbiag.app.modules.reports.hcprintschedule;

import java.io.OutputStream;
import com.lowagie.text.*;

/**
 * @author          Ali Ajellu
 * @version         1.0
 * Date:            Friday, June 16 206
 * Copyright:       Workbrain Corp.
 * TestTrack:       1630
 *
 * The interface that all Views must implement to be used by the PrintingScheduleController.
 * For a more extensive explanation, please read the class header comments in that class.
 */
public interface PrintingScheduleView {

	public Document retrieveReport(java.util.List modelData, OutputStream baos) throws IllegalStateException;
    //HUB:42198  TT:2964
	public Document retrieveReport(java.util.List modelData, OutputStream baos, boolean showTotal) throws IllegalStateException;
	

}
