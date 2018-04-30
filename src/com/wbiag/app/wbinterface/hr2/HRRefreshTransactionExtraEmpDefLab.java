package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.wbinterface.hr2.handlers.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.tool.security.*;
/**
 * Customization for extra emp_def_lab entries
 *
 **/
public class HRRefreshTransactionExtraEmpDefLab extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionExtraEmpDefLab.class);

    private static final int EMP_DEF_LAB_EXTRA = 90;
    private static final int EMP_DEF_LAB_UDFFLAG = 91;

    private int[] custColInds = new int[] {EMP_DEF_LAB_EXTRA , EMP_DEF_LAB_UDFFLAG};
    private DBConnection conn = null;

    /**
     * Sets custom column indexes to be used by the customization. Indexes start at 0
     * @param inds
     */
    public void setCustomColInds(int[] inds) {
        custColInds = inds;
    }

    /**
     * Override this class to customize before process batch events.
     * At this time, all interface data has been converted to <code>HRRefreshTransactionData</code>
     * objects but not processed yet. All related employee data have been loaded
     * and is available through <code>HRRefreshCache</code>.
     *
     * @param conn              DBConnection
     * @param hrRefreshDataList List of <code>HRRefreshTransactionData</code>
     * @param process           HRRefreshProcessor
     * @throws Exception
     */
    public void preProcessBatch(DBConnection conn,
                                   List hrRefreshTransactionDataList,
                                   HRRefreshProcessor process) throws Exception {
        if (hrRefreshTransactionDataList == null || hrRefreshTransactionDataList.size() == 0) {
                return;
        }
        if (custColInds == null || custColInds.length == 0) {
            throw new WBInterfaceException ("Custom column index not supplied");
        }
        this.conn = conn;
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            processExtraDefLaborData(data);

        }
    }

    protected void processExtraDefLaborData( HRRefreshTransactionData data ) throws Exception,
            WBInterfaceException {
        // *** extra empdeflabs
        ArrayList newDlList = new ArrayList();
        String extra = data.getImportData().getField(EMP_DEF_LAB_EXTRA) ;
        String extras[] = StringHelper.detokenizeString(extra , "|");
        for( int i = 0; i < extras.length ; i++ ) {
            String dl = extras[i];
            if (!StringHelper.isEmpty(dl)) {
                newDlList.add( dl );
            }
        }
        if( newDlList.size() > 0 ) {
            new DefLaborHandler().process(data.getHRRefreshData(), newDlList);
        }
        // *** udf flags
        List empDefLabs = data.getHRRefreshData().getEmployeeDefaultLabor();
        String empDlabUdf = data.getImportData().getField(EMP_DEF_LAB_UDFFLAG) ;
        if (!StringHelper.isEmpty(empDlabUdf)) {
            String[] udfflags = StringHelper.detokenizeString(empDlabUdf, "|");
            if (udfflags.length > empDefLabs.size() ) {
                throw new WBInterfaceException ("Not enough employee default labor definitions for empdeflab udf/flag updates");
            }
            for (int i = 0; i < udfflags.length; i++) {
                if (StringHelper.isEmpty(udfflags[i])) {
                    continue;
                }
                EmployeeDefaultLaborData edla = (EmployeeDefaultLaborData)empDefLabs.get(i);
                List vals = StringHelper.detokenizeStringAsNameValueList(udfflags[i],
                    "~", "=", true);
                Iterator iter = vals.iterator();
                while (iter.hasNext()) {
                    NameValue item = (NameValue) iter.next();
                    edla.setField(item.getName(), item.getValue());
                }
            }
        }
    }


}