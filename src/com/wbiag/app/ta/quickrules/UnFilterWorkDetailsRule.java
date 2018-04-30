package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * UnFilterWorkDetailsRule

 */
public class UnFilterWorkDetailsRule extends Rule{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(UnFilterWorkDetailsRule.class);


    public List getParameterInfo(DBConnection dBConnection) {
        List result = new ArrayList();

        return result;
    }


    public String getComponentName() {
        return "WBIAG: UnFilter Work Details Rule";
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {
        Map wrk = getWorkDetailsPluckedMap(wbData);
        EmployeeIdAndDate ed = new EmployeeIdAndDate(wbData.getEmpId() , wbData.getWrksWorkDate());
        WorkDetailList workDetailsPlucked = (WorkDetailList)wrk.get(ed);
        if (workDetailsPlucked != null && workDetailsPlucked.size() > 0) {
            for (int i = workDetailsPlucked.size(); i > 0; i--) {
                unpluck(wbData , workDetailsPlucked, i - 1);
            }

        }

    }

    private void unpluck(WBData wbData, WorkDetailList workDetailsPlucked, int in_Index) {
        if (workDetailsPlucked.size() > in_Index) {
            WorkDetailData plucked = (WorkDetailData) workDetailsPlucked.get(
                in_Index);
            wbData.getRuleData().splitAt( (plucked).getWrkdStartTime());
            plucked.setWrkdId( -999);
            wbData.getRuleData().getWorkDetails().insert(plucked);
            workDetailsPlucked.remove(in_Index);

            for (int i = 0; i < wbData.getRuleData().getWorkDetailCount(); i++) {
                WorkDetailData wd = wbData.getRuleData().getWorkDetail(i);
                if (i > 0) {
                    wd.setWrkdStartTime( (Date) wbData.getRuleData().
                                        getWorkDetail(i - 1).getWrkdEndTime().
                                        clone());
                }
                wd.setWrkdEndTime(new java.util.Date(wd.getWrkdStartTime().
                    getTime() +
                    wd.getWrkdMinutes() * DateHelper.MINUTE_MILLISECODS));
}
        }

    }


    private Map getWorkDetailsPluckedMap(WBData wbData ) {
        Hashtable entity = wbData.getRuleData().getCalcDataCache().getEntityCache();

        Map wrk = (Map)entity.get("WORK_DETAIL_PLUCKED");
        if (wrk == null) {
            wrk = new HashMap();
            entity.put("WORK_DETAIL_PLUCKED", wrk);
        }
        return wrk;
    }

}
