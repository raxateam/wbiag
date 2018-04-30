package com.wbiag.app.export.payroll;

import com.workbrain.app.export.payroll.*;
import com.workbrain.app.export.payroll.data.*;
/**
 * Formats payroll file in following schema. Assumes grouping is based on employee
 * and first field in format is emp_name
 * <allrecords>
 *   <oneemployee>
 *     <onerecord>
 *       ..
 *     </onerecord>
 *   </oneemployee>
 * </allrecords>
 *
 *
 * Pet_xml format section would look like
   <format>
       <body>
              <string field='emp_name'/>

              <constant>&lt;pg&gt;</constant>
              <string field='paygrp_name' />
              <constant>&lt;/pg&gt;</constant>

              <constant>&lt;earnCode&gt;</constant>
              <string field='earn_code'/>
              <constant>&lt;/earnCode&gt;</constant>

              <constant>&lt;minutes&gt;</constant>
              <number field='wrkd_minutes' format='#######0.00' divide='60' null='***'/>
              <constant>&lt;/minutes&gt;&lt;/oneRecord&gt;</constant>
              <new_line/>
        </body>
    </format>
 */
public class PayrollExportPluginXMLSample extends PayrollExportPlugin{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PayrollExportPluginXMLSample.class);

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
    private static final String XML_ROOT_TAG = "allRecords";
    private static final String XML_GROUP_TAG = "oneEmployee";
    private static final String XML_RECORD_TAG = "oneRecord";
    private String lastEmpName = "XXX";
    private int recordCnt = 0;

    /**
     * This event is called once per every data row, which was produced by logic section,
     * and before is is passed to format.  Any groupping will be already reflected.
     * @return false - if the Row has to be taken out of further processing.
     */

    public boolean beforeRowFormat(Row r){
        int empNameInd = this.getFieldIndex("emp_name");
        String empName = (String)r.get(empNameInd);

        recordCnt++;
        if (!empName.equals(lastEmpName)) {
            String groupTag = recordCnt == 1
                ? XML_HEADER + "<" + XML_ROOT_TAG + ">" + "<" + XML_GROUP_TAG +">"
                : "</" + XML_GROUP_TAG + "><" + XML_GROUP_TAG + ">";
            r.set(empNameInd,
                  groupTag
                  + "<" + XML_RECORD_TAG + "><emp_name>" + r.get(empNameInd) + "</emp_name>");
            lastEmpName = empName;
        }
        else {
            r.set(empNameInd,
                  "<" + XML_RECORD_TAG + "><emp_name>" + r.get(empNameInd) + "</emp_name>");
        }
        return true;
    }

    /**
     * This event is called after all export is done. The value returned will be appended
     * to the output.
     * @return String to append to the export.
     */
    public String appendExport(){
        if (recordCnt > 0) {
            return "</" + XML_GROUP_TAG + "></" + XML_ROOT_TAG + ">";
        }
        return null;
    }

}



