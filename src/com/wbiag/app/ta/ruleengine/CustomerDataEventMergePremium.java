package com.wbiag.app.ta.ruleengine;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.server.registry.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.wbinterface.db.*;
import java.util.*;
/**
 * Custom data event to merge similar premium records that are equal
 * except for the the ids and dates.
 */
public class CustomerDataEventMergePremium extends DataEvent {

    public void beforeSave(WBData data, DBConnection c)   {
        mergePremiums(data);
    }
    
    protected static void mergePremiums(WBData data) {
        WorkDetailList wps = data.getRuleData().getWorkPremiums();
        for (int i=1; i<wps.size();) {
            WorkDetailData prevPrem = wps.getWorkDetail(i - 1);
            WorkDetailData currPrem = wps.getWorkDetail(i);
            
            if (currPrem.equalWithoutIdsOrDates(prevPrem)) {
                prevPrem.setWrkdMinutes(prevPrem.getWrkdMinutes()
                                        + currPrem.getWrkdMinutes());
                //if equal, remove and keep "i" the same for next loop iteration
                wps.remove(i);
            }
            else {
                //move to next detail
                i++;
            }
        }
    }

}
