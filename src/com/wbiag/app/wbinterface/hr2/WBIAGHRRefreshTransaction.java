package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.WBInterfaceException;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.wbiag.app.wbinterface.hr2.*;
import com.wbiag.app.wbinterface.mapping.*;

/**
 * WBIAGHRRefreshTransaction that runs specified post/pre customizations.
 * customizations should be defined as
 * [[classname]~[c1][,cn]+[space]]+
 * i.e
 * HRRefreshTransactionDefAvail~91 HRRefreshTransactionDelUnasEmpJobs
 *
 **/
public class WBIAGHRRefreshTransaction extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WBIAGHRRefreshTransaction.class);

    public static final String PARAM_WBIAG_EXTENSIONS = "WBIAG_EXTENSIONS";
    public static final String PARAM_HEADER_PREFIX = "HEADER_PREFIX";
    public static final String PARAM_FOOTER_PREFIX = "FOOTER_PREFIX";
    public static final String PARAM_HEADER_CHECKSUM = "HEADER_CHECKSUM";
    public static final String PARAM_FOOTER_CHECKSUM = "FOOTER_CHECKSUM";
    public static final String PARAM_EXTRA_MAPPINGS = "EXTRA_MAPPINGS";

    public static final String EXEC_POINT_PRE = "PRE";
    public static final String EXEC_POINT_POST = "POST";
    public static final List wbiagExtClassNames = new ArrayList();

    public static final String CLASS_HRRefreshTransactionDefAvail =
        "HRRefreshTransactionDefAvail";
    public static final String CLASS_HRRefreshTransactionDelUnasEmpJobs =
        "HRRefreshTransactionDelUnasEmpJobs";
    public static final String CLASS_HRRefreshTransactionEmpRdrGroup =
        "HRRefreshTransactionEmpRdrGroup";
    public static final String CLASS_HRRefreshTransactionTeamExtra =
        "HRRefreshTransactionTeamExtra";
    public static final String CLASS_HRRefreshTransactionEndEntPols =
        "HRRefreshTransactionEndEntPols";
    public static final String CLASS_HRRefreshTransactionMultiTeamExtra =
        "HRRefreshTransactionMultiTeamExtra";
    public static final String CLASS_HRRefreshTransactionSTEmpSkill =
        "HRRefreshTransactionSTEmpSkill";
    public static final String CLASS_HRRefreshTransactionSOExtra =
        "HRRefreshTransactionSOExtra";
    public static final String CLASS_HRRefreshTransactionTempTeam =
        "HRRefreshTransactionTempTeam";
    public static final String CLASS_HRRefreshTransactionTSUser =
        "HRRefreshTransactionTSUser";
    public static final String CLASS_HRRefreshTransactionEmpFulltime =
        "HRRefreshTransactionEmpFulltime";
    public static final String CLASS_HRRefreshTransactionEmpTzone =
        "HRRefreshTransactionEmpTzone";
    public static final String CLASS_HRRefreshTransactionExtraEmpDefLab =
        "HRRefreshTransactionExtraEmpDefLab";
    public static final String CLASS_HRRefreshTransactionWbuActive =
        "HRRefreshTransactionWbuActive";
    public static final String CLASS_HRRefreshTransactionTempEmpName =
        "HRRefreshTransactionTempEmpName";
    public static final String CLASS_HRRefreshTransactionSCEmpSchool =
        "HRRefreshTransactionSCEmpSchool";
    public static final String CLASS_HRRefreshTransactionEmpBadgeExtra =
        "HRRefreshTransactionEmpBadgeExtra";
    public static final String CLASS_HRRefreshTransactionWbuExtra =
        "HRRefreshTransactionWbuExtra";

    static {
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionDefAvail);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionDelUnasEmpJobs);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionEmpRdrGroup);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionTeamExtra);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionEndEntPols);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionMultiTeamExtra);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionSTEmpSkill);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionSOExtra);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionTempTeam);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionTSUser);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionEmpFulltime);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionEmpTzone);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionExtraEmpDefLab);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionWbuActive);
        wbiagExtClassNames.add(CLASS_HRRefreshTransactionWbuExtra);
    }

    private String extensions = null;
    private String headerPrefix = null;
    private String footerPrefix = null;
    private boolean headerChkSumCount = false;
    private int headerLineCount = 0;
    private int checkSumCnt = Integer.MIN_VALUE;
    private boolean footerChkSumCount = false;
    private int footerLineCount = 0;
    private String extraMappings = null;

    /**
     * This method will be called each time a transaction is initialized
     *
     */
    public void initializeTransaction(DBConnection connection) throws Exception{
        super.initializeTransaction(connection );
        extensions = (String)this.params.get(PARAM_WBIAG_EXTENSIONS);
        headerPrefix = (String)this.params.get(PARAM_HEADER_PREFIX);
        footerPrefix = (String)this.params.get(PARAM_FOOTER_PREFIX);
        extraMappings = (String)this.params.get(PARAM_EXTRA_MAPPINGS);
        headerChkSumCount = StringHelper.isEmpty((String)this.params.get(PARAM_HEADER_CHECKSUM)) ? false : Boolean.valueOf((String)this.params.get(PARAM_HEADER_CHECKSUM)).booleanValue()   ;
        footerChkSumCount = StringHelper.isEmpty((String)this.params.get(PARAM_FOOTER_CHECKSUM)) ? false : Boolean.valueOf((String)this.params.get(PARAM_FOOTER_CHECKSUM)).booleanValue()   ;
    }

    /**
     * Override this to check header/footers
     *
     * @param data
     * @param c
     * @throws WBInterfaceException
     * @throws SQLException
     */
    public void process( HRElementSource data, DBConnection c )
        throws WBInterfaceException, SQLException
    {
        if (!StringHelper.isEmpty(headerPrefix)
            && headerPrefix.equals(data.getRawValue(0))) {
            if (logger.isDebugEnabled()) logger.debug("Header record, ignored");
            headerLineCount ++;
            if (headerChkSumCount) {
                try {
                    checkSumCnt = Integer.parseInt(data.getRawValue(1));
                }
                catch (NumberFormatException ex) {
                    if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in parsing header check count" , ex);}
                }
            }
            return;
        }
        if (!StringHelper.isEmpty(footerPrefix)
            && footerPrefix.equals(data.getRawValue(0))) {
            if (logger.isDebugEnabled()) logger.debug("Footer record, ignored");
            footerLineCount++;
            if (footerChkSumCount) {
                try {
                    checkSumCnt = Integer.parseInt(data.getRawValue(1));
                }
                catch (NumberFormatException ex) {
                    if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in parsing footer check count" , ex);}
                }
            }
            return;
        }

        if (!StringHelper.isEmpty(extraMappings)) {
            try {
                processExtraMappings(conn, data, extraMappings);
            }
            catch (Exception ex1) {
                throw new WBInterfaceException ("Error in doing extra mapping for :" + extraMappings);
            }
        }

        super.process(data , c);
    }

    /**
     * This method will be called each time a transaction is finalized
     * Do check sums here, if specified
     *
     */
    public void finalizeTransaction(DBConnection conn) throws Exception{
        super.finalizeTransaction(conn);
        if (checkSumCnt != Integer.MIN_VALUE) {
            int finalRecCount = getRecordCount() - (headerLineCount + footerLineCount);
            if (finalRecCount != checkSumCnt) {
                throw new WBInterfaceException ("Check sum check failed. Expected :" + checkSumCnt + " , was : "+  finalRecCount);
            }
        }
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
    protected void preProcessBatch(DBConnection conn,
                                   List hrRefreshTransactionDataList,
                                   HRRefreshProcessor process) throws Exception {

         processExtension(conn, hrRefreshTransactionDataList,
                          process, extensions,
                          EXEC_POINT_PRE);
    }

    /**
     * Override this class to customize after process batch events.
     * At this time, all HRRefreshTransactionData data has been processed.
     * All processed is available through <code>HRRefreshCache</code>.
     *
     * @param conn                          DBConnection
     * @param hrRefreshTransactionDataList  List of <code>HRRefreshTransactionData</code>
     * @param process                       HRRefreshProcessor
     * @throws Exception
     */
    protected void postProcessBatch(DBConnection conn,
                                    List hrRefreshTransactionDataList,
                                    HRRefreshProcessor process) throws Exception {

        processExtension(conn, hrRefreshTransactionDataList,
                         process,
                         extensions,
                         EXEC_POINT_POST);

    }

    private void processExtension(DBConnection conn,
                                  List hrRefreshTransactionDataList,
                                  HRRefreshProcessor process,
                                  String extString,
                                  String execPoint) throws Exception{
        if (!StringHelper.isEmpty(extString)) {
            String[] postExtensionsA = StringHelper.detokenizeString(extString, " ");
            for (int i = 0; i < postExtensionsA.length; i++) {
                String oneExtension = postExtensionsA[i];
                if (StringHelper.isEmpty(oneExtension)) {
                    continue;
                }
                String oneExtensionA[] = StringHelper.detokenizeString(oneExtension, "~");
                String className = oneExtensionA[0];
                String cols = null; int[] colsA = null;
                if (oneExtensionA.length == 2) {
                    cols = oneExtensionA[1];
                    colsA = StringHelper.detokenizeStringAsIntArray(cols , ",", true);
                }
                if (!wbiagExtClassNames.contains(className)) {
                    logger.error("Class name is not a valid Solution Center Extension :" +  className);
                    throw new WBInterfaceException ("Class name is not a valid Solution Center Extension :" +  className);
                }
                if (EXEC_POINT_POST.equals(execPoint)) {
                    if (CLASS_HRRefreshTransactionDefAvail.equals(className)) {
                        HRRefreshTransactionDefAvail tran = new
                            HRRefreshTransactionDefAvail();
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionDelUnasEmpJobs.equals(className)) {
                        HRRefreshTransactionDelUnasEmpJobs tran = new
                            HRRefreshTransactionDelUnasEmpJobs();
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionEmpRdrGroup.equals(className)) {
                        HRRefreshTransactionEmpRdrGroup tran = new
                            HRRefreshTransactionEmpRdrGroup();
                        tran.setCustomColInds(colsA);
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionTeamExtra.equals(className)) {
                        HRRefreshTransactionTeamExtra tran = new
                            HRRefreshTransactionTeamExtra();
                        tran.setCustomColInds(colsA);
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionEndEntPols.equals(className)) {
                        HRRefreshTransactionEndEntPols tran = new
                            HRRefreshTransactionEndEntPols();
                        tran.setCustomColInds(colsA);
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionMultiTeamExtra.equals(className)) {
                        HRRefreshTransactionMultiTeamExtra tran = new
                            HRRefreshTransactionMultiTeamExtra();
                        tran.setCustomColInds(colsA);
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionSTEmpSkill.equals(className)) {
                        HRRefreshTransactionSTEmpSkill tran = new
                            HRRefreshTransactionSTEmpSkill();
                        tran.setCustomColInds(colsA);
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionSOExtra.equals(className)) {
                        HRRefreshTransactionSOExtra tran = new
                            HRRefreshTransactionSOExtra();
                        tran.setCustomColInds(colsA);
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionTempTeam.equals(className)) {
                        HRRefreshTransactionTempTeam tran = new
                            HRRefreshTransactionTempTeam();
                        tran.setCustomColInds(colsA);
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }

                    else if (CLASS_HRRefreshTransactionTSUser.equals(className)) {
                        HRRefreshTransactionTSUser tran = new
                            HRRefreshTransactionTSUser();
                        tran.setCustomColInds(colsA);
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionTempEmpName.equals(className)) {
                        HRRefreshTransactionTempEmpName tran = new
                            HRRefreshTransactionTempEmpName();
                        tran.setCustomColInds(colsA);
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);

                    }
                    else if (CLASS_HRRefreshTransactionSCEmpSchool.equals(className)) {
                        HRRefreshTransactionSCEmpSchool tran = new
                            HRRefreshTransactionSCEmpSchool();
                        tran.setCustomColInds(colsA);
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);

                    }
                    else if (CLASS_HRRefreshTransactionEmpBadgeExtra.equals(className)) {
                        HRRefreshTransactionEmpBadgeExtra tran = new
                            HRRefreshTransactionEmpBadgeExtra();
                        tran.setCustomColInds(colsA);
                        tran.postProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);

                    }


                }
                else if (EXEC_POINT_PRE.equals(execPoint)) {
                    if (CLASS_HRRefreshTransactionEmpFulltime.equals(className)) {
                        HRRefreshTransactionEmpFulltime tran = new
                            HRRefreshTransactionEmpFulltime();
                        tran.setCustomColInds(colsA);
                        tran.preProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionEmpTzone.equals(className)) {
                        HRRefreshTransactionEmpTzone tran = new
                            HRRefreshTransactionEmpTzone();
                        tran.setCustomColInds(colsA);
                        tran.preProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionExtraEmpDefLab.equals(className)) {
                        HRRefreshTransactionExtraEmpDefLab tran = new
                            HRRefreshTransactionExtraEmpDefLab();
                        tran.setCustomColInds(colsA);
                        tran.preProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionWbuActive.equals(className)) {
                        HRRefreshTransactionWbuActive tran = new
                            HRRefreshTransactionWbuActive();
                        tran.setCustomColInds(colsA);
                        tran.preProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }
                    else if (CLASS_HRRefreshTransactionWbuExtra.equals(className)) {
                        HRRefreshTransactionWbuExtra tran = new
                            HRRefreshTransactionWbuExtra();
                        tran.setCustomColInds(colsA);
                        tran.preProcessBatch(conn,
                                              hrRefreshTransactionDataList,
                                              process);
                    }

                }
                else {
                    throw new WBInterfaceException ("Execution point not supported :" + execPoint);
                }
            }
        }
    }

    private void processExtraMappings(DBConnection conn,
                                      HRElementSource data,
                                      String extMappings) throws Exception{

        String[] extMappingsA = StringHelper.detokenizeString(extMappings, "~");
        for (int i = 0; i < extMappingsA.length; i++) {
            String onext = extMappingsA[i];
            String[] onextA = StringHelper.detokenizeString(onext, ",");
            int col = Integer.parseInt(onextA[0]);
            String mapName = onextA[1];
            MappingFactory mf = new MappingFactory(conn, mapName);
            String val = data.getImportData().getField(col) ;
            String valMapped = mf.mapForOneOutputValue(val) ;
            System.out.println("Mapped (" + val + ") to (" + valMapped + ") by mapping :" + mapName);
            data.setField(col, valMapped)  ;
        }


    }

    public String getTaskUI() {
        return "/jobs/wbiag/wbiagHRTransParams.jsp";
    }

}