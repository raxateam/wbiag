package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
/**
 *  Title:        Shift Premiums Rule Shift Zone Percentage
 * @deprecated As of fixpack after 4.1 Fixpack Mar-14-2005, use {@link #ShiftPremiumExt}
 */
public class ShiftPremiumShiftZonePctRule extends ShiftPremiumExtRule {

    public List getParameterInfo(DBConnection conn) {
        return super.getParameterInfo(conn);
    }

    public String getComponentName() {
        return "WBIAG: Shift Premiums Shift Zone Percentage Rule";
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {
        super.execute(wbData , parameters);
    }


}