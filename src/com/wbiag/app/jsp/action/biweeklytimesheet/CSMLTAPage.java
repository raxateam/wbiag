package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.io.Serializable;
import java.util.List;

import com.workbrain.app.jsp.action.timesheet.LTAPage;

public class CSMLTAPage extends LTAPage implements CSMBiWeeklyTimeSheetConstants, Serializable, Cloneable
{
    public CSMLTAPage(){
    	setHours(new int[DAYS_ON_TIMESHEET]);
    }

    public Object clone() {
        CSMLTAPage cloned = new CSMLTAPage();
        cloned.setTotal(getTotal());

        int[] _hours = getHours();
        int[] clonedHours = cloned.getHours();
        for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
        	clonedHours[i] = _hours[i];
        }
        cloned.setHours(clonedHours);

        List _lines = getLines();
        List clonedLines = cloned.getLines();
        for (int i = 0; i < _lines.size(); i++) {
        	clonedLines.add((LTALine)((LTALine)_lines.get(i)).clone());
        }
        cloned.setLines(clonedLines);

        return cloned;
    }

}