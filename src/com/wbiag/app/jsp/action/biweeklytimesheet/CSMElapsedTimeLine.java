package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.util.*;
import java.io.Serializable;

import com.workbrain.app.jsp.action.timesheet.ElapsedTimeLine;

public class CSMElapsedTimeLine extends ElapsedTimeLine implements CSMBiWeeklyTimeSheetConstants, Cloneable, Serializable {

	protected boolean isPremium;

	public CSMElapsedTimeLine(){
		super();
		this.isPremium = false;
	}

	public boolean hasTime() {
        for (int j = 0; j < DAYS_ON_TIMESHEET; j++) {
            if (getHour(j) > 0) return true;
        }
        return false;
    }

    public Object clone() throws CloneNotSupportedException {
        CSMElapsedTimeLine cloned = new CSMElapsedTimeLine();
        cloned.setDistributed(getDistributed());
        cloned.setDel(getDel());
        cloned.setLineTotal(getLineTotal());

        List newFields = new ArrayList();
        Iterator i = getFields().iterator();
        while( i.hasNext() ) {
            newFields.add( i.next() );
        }
        cloned.setFields( newFields );

        List hours = new ArrayList();
        i = getHours().iterator();
        while( i.hasNext() ) {
            hours.add( i.next() );
        }
        cloned.setHours( hours );

        List ovrIds = new ArrayList();
        i = getOvrIds().iterator();
        while( i.hasNext() ) {
            ovrIds.add( i.next() );
        }
        cloned.setOvrIds( ovrIds );

        return cloned;
    }

	public boolean isPremium() {
		return isPremium;
	}

	public void setPremium(boolean isPremium) {
		this.isPremium = isPremium;
	}
}
