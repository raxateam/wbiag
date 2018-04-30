package com.wbiag.app.ta.ruleengine;

import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;

import com.wbiag.app.ta.ruleengine.CDataEventPayrollSplitAdj;
import com.workbrain.app.modules.retailSchedule.model.Activity;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;

public class CDataEventSOActivityTcode extends DataEvent {
	private static Logger logger = Logger.getLogger(CDataEventSOActivityTcode.class);

	public void afterApplyClocks(WBData data, DBConnection c) {
		try {
			copySchedDetailToWrkd(data,c);
		} catch (Exception e) {
			if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("Error in copySchedDetailToWrkd" , e);
            throw new NestedRuntimeException("Error in copySchedDetailToWrkd", e);
		}
	}

	private void copySchedDetailToWrkd(WBData wbData, DBConnection c)
	throws SQLException {
		if (logger.isDebugEnabled()) { logger.debug("copySchedDetailToWrkd. empId:" + wbData.getEmpId() + " date:" + wbData.getWrksWorkDate());		}

        for (int i = 0, j = wbData.getRuleData().getWorkDetails().size(); i < j; i++) {
            WorkDetailData wd = wbData.getRuleData().getWorkDetails().
                getWorkDetail(i);
            if (checkIfCreatedByTcodeClock(wbData , wd)) {
                if (logger.isDebugEnabled()) logger.debug("Work detail data was created by a time code clock, skipping \n" + wd);
                continue;
            }
            int actId = wd.getActId();
            Activity aData = wbData.getCodeMapper().getSOActivityById(actId);
            if (aData != null) {
                if (logger.isDebugEnabled()) logger.debug("Setting tcode to:" + aData.getTcodeId().intValue() + " for activity:" + aData.getActName());
                wd.setTcodeId(aData.getTcodeId().intValue());
            }
            else {
                if (logger.isDebugEnabled()) logger.debug("Activity not found, id :" + actId);
            }
        }

	}

    private boolean checkIfCreatedByTcodeClock(WBData wbData, WorkDetailData wd){
        boolean ret = false;

        List tcodeClocks = wbData.getClocks(Clock.CLOCKDATA_TIMECODE);
        if (tcodeClocks.size() == 0) {
            return false;
        }
        wd.setCodeMapper(wbData.getCodeMapper()  );
        Iterator iter = tcodeClocks.iterator();
        while (iter.hasNext()) {
            Clock item = (Clock)iter.next();
            if (StringHelper.equals(wd.getWrkdTcodeName()
                                    ,item.getClockDataName(Clock.CLOCKDATA_TIMECODE))
                && wd.getWrkdStartTime().compareTo(item.getClockDate()) >= 0) {
                ret = true; break;
            }
        }

        return ret;
    }

}
