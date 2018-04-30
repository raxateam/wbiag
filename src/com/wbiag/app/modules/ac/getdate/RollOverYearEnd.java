package com.wbiag.app.modules.ac.getdate;

import java.util.*;

import com.workbrain.app.modules.ac.*;
import com.workbrain.app.modules.ac.db.*;
import com.workbrain.app.modules.ac.getdate.*;
/*
 * RollOverYearEnd.java
 *
 * Created on June 8, 2004, 11:22 AM
 */
public class RollOverYearEnd implements GetACPeriodDate {

    public Date get( Date date, int empId, ACAccessHelper accessHelper ) throws ACException {
        return date;
    }

}
