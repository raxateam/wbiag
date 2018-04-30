package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.util.*;

import com.workbrain.app.jsp.action.timesheet.ElapsedTimeLine;
import com.workbrain.app.jsp.action.timesheet.ElapsedTimePage;


public class CSMElapsedTimePage extends ElapsedTimePage implements java.io.Serializable, Cloneable {

    public boolean hasTime() {
        List lines = getElapsedTimeLines();
        for (int i = 0; i < lines.size(); i++) {
            if (((CSMElapsedTimeLine)lines.get(i)).hasTime()) return true;
        }
        return false;
    }

}
