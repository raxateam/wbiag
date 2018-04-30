package com.wbiag.app.modules.ac.getdate;

import java.util.*;

import com.workbrain.app.modules.ac.*;
import com.workbrain.app.modules.ac.db.*;
import com.workbrain.app.modules.ac.getdate.*;
import com.workbrain.util.*;

/*
 * RollOverHalfYearStart.java
 *
 */
public class RollOverHalfYearStart  implements GetACPeriodDate{

    public Date get( Date date, int empId, ACAccessHelper accessHelper ) throws ACException {
        Calendar c = DateHelper.getCalendar();
        c.setTime(date);
        c.set(Calendar.MONTH, c.get(Calendar.MONTH)-6);
        c.set(Calendar.DATE, c.get(Calendar.DATE)+1);

        return c.getTime();
    }

}
