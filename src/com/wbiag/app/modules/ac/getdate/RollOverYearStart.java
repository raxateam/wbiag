package com.wbiag.app.modules.ac.getdate;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.app.modules.ac.*;
import com.workbrain.app.modules.ac.db.*;
import com.workbrain.app.modules.ac.getdate.*;

/*
 * RollOverYearStart.java
 *
 * Created on June 8, 2004, 11:22 AM
 */
public class RollOverYearStart implements GetACPeriodDate {

    public Date get( Date date, int empId, ACAccessHelper accessHelper ) throws ACException {
        Calendar c = DateHelper.getCalendar();
        c.setTime(date);
        c.set(Calendar.YEAR, c.get(Calendar.YEAR)-1);
        //c.set(Calendar.MONTH, c.get(Calendar.MONTH)-1);
        //c.set(Calendar.DATE, c.get(Calendar.DATE)+1);

        return c.getTime();

    }

}
