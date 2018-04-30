package com.wbiag.app.export.payroll;

import com.workbrain.app.export.payroll.*;
import com.workbrain.app.export.payroll.data.*;
import com.workbrain.util.*;

import java.util.Date;

public class PayrollExportPluginSplitWork extends PayrollExportPlugin{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PayrollExportPluginSplitWork.class);

    private final String SPLIT_TIME_FORMAT = "yyyyMMdd HHmmss";
    /**
     * This event is called once per every data row, before it was passed to
     * logic section.
     * @return false - if the Row has to be taken out of further processing.
     */
    public boolean beforeRowLogic(Row r){
        boolean isRetro = "Y".equals((String)r.get(getFieldIndex("retro")));
        return isRetro ? true : checkSplit(r);
    }

    /**
     * <ul>
     * <li>Reads split time from calcgrp_udf1. This field must be defined both in view_payexp_current
     * and view_payexp_adjustment.
     * <li>If calcgrp_udf1 is empty, it assumes that there is no split for the record
     * <li>wrkd_work_date must also be included in view_payexp_current
     * and view_payexp_adjustment.
     * </ul>
     * @param r
     * @return
     */
    protected boolean checkSplit(Row r) {
        String sSplitTime = (String)r.get(getFieldIndex("calcgrp_udf1"));
        if (StringHelper.isEmpty(sSplitTime)) {
            return true;
        }
        Date splitTime = DateHelper.parseDate(sSplitTime , SPLIT_TIME_FORMAT);
        long splitMins = DateHelper.getDayFraction(splitTime);

        Date workDate = (Date)r.get(getFieldIndex("wrkd_work_date"));
        Date currentCutoffStart = new Date(DateHelper.addDays(getExport().getFromDate() , 1).getTime()
                                           + splitMins);
        Date currentCutoffEnd = new Date(getExport().getToDate().getTime() + splitMins);
        boolean eligible = workDate.compareTo(currentCutoffStart) >= 0
                && workDate.compareTo(currentCutoffEnd) <= 0;
        return eligible;
    }
}



