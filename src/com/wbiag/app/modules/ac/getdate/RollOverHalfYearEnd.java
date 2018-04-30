package com.wbiag.app.modules.ac.getdate;

import com.workbrain.app.modules.ac.*;
import com.workbrain.app.modules.ac.getdate.*;
import com.workbrain.app.modules.ac.db.ACAccessHelper;
import java.util.Date;

/*
 * RollOverHalfYearEnd.java
 *
 */
public class RollOverHalfYearEnd implements GetACPeriodDate {

    public Date get( Date date, int empId, ACAccessHelper accessHelper ) throws ACException {
        return date;
    }

}
