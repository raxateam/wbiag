package com.wbiag.app.bo ;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.bo.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.workflow.*;
import com.workbrain.app.workflow.properties.*;
import com.workbrain.app.workflow.properties.types.*;
import com.workbrain.test.*;
import com.workbrain.util.*;
import junit.framework.*;

/**
 * Test case for Interaction Actions
 */
public class InteractionTestCase extends TestCaseHW {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(InteractionTestCase.class);

    public InteractionTestCase(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(InteractionTestCase.class);
        return result;
    }

    /**
     * Given a map of workflow node parameter values, returns an Action
     * object required by the framework.
     *
     * @param parameters
     * @return
     * @throws Exception
     */
    protected Action getAction(Map parameters) throws Exception {
        if (parameters == null)
            return null;
        Action retval = new Action();
        Iterator it = parameters.keySet().iterator();
        while (it.hasNext()) {
            String curName = (String) it.next();
            Object curVal = parameters.get(curName);
            FieldType type = null;
            if (curVal instanceof String)
                type = StringType.get();
            else if (curVal instanceof Boolean)
                type = BooleanType.get();
            else if (curVal instanceof Number)
                type = NumberType.get();
            else if (curVal instanceof Map)
                type = MapType.get();
            else if (curVal instanceof List)
                type = ListType.get();
            else
                throw new Exception("Unsupported data type: " +
                                    curVal.getClass().getName());
            retval.addProperty(curName, curVal, type);
        }
        return retval;
    }

    /**
     * Given a list of output workflow branch names, returns an array
     * of Branch objects suitable for passing to the interactions framework.
     * @param branchNames
     * @return
     */
    protected Branch[] getBranches(String[] branchNames)  {
        if (branchNames == null)
            return new Branch[0];
        Branch[] retval = new Branch[branchNames.length];
        for (int i = 0; i < branchNames.length; i++)
        {
            retval[i] = new Branch();
            retval[i].setLabel(branchNames[i]);
        }
        return retval;
    }

    private static final String GET_BUSOBJTYP_XML_SQL =
        "SELECT a.botvr_xml FROM busobjtyp_version a, business_object_type b WHERE "
        + "a.busobjtyp_id = b.busobjtyp_id AND b.busobjtyp_name=? "
        + "ORDER BY botvr_date DESC";

    private static int BUSINESS_OBJECT_TYPE = 7;

    protected BOFormInstance createInteraction(
        String userName,
        String workflowName,
        String formName,
        boolean batchApprovable,
        boolean cancelable,
        Date createDate,
        Integer elapsedDays) throws Exception {
        try {
            BOFormInstance retval = new BOFormInstance();
            String xml = getXML(formName);
            retval.setXML(xml);
            retval.setBatchApprovable(batchApprovable);
            retval.setCancelable(cancelable);
            retval.setCreationDate(createDate == null ? new Date() : createDate);
            WorkbrainUserAccess wbua = new WorkbrainUserAccess(getConnection());
            WorkbrainUserData wbud = wbua.loadByWbuName(userName);
            if (wbud == null)
                throw new NestedRuntimeException("Unknown username: " +
                                                 userName);
            retval.setCreatorUserID(wbud.getWbuId());
            retval.setDescription("Unit test framework business object");
            retval.setElapsedPeriod(elapsedDays == null ? 0 :
                                    elapsedDays.intValue());
            retval.setFlowId(1);
            retval.setFormType(BUSINESS_OBJECT_TYPE);
            retval.setInitiationDate(null);
            retval.setLocationInFlow("undefined");
            retval.setName(formName);
            retval.setPage(1);
            retval.setStatusName("test status name");
            retval.setUserName(userName);
            return retval;
        }
        catch (Exception e) {
            String msg = "Error creating BOFormInstance";
            logger.error(msg, e);
            throw new NestedRuntimeException(msg + ": [" + e.getMessage() + "]");
        }
    }

    private String getXML(String busObjTypeName) throws Exception {
        ResultSet rs = null;
        PreparedStatement ps = null;
        try
        {
            ps = getConnection().prepareStatement(GET_BUSOBJTYP_XML_SQL);
            ps.setString(1, busObjTypeName);
            rs = ps.executeQuery();
            if (rs != null && rs.next())
                return clobToString(rs.getClob(1));
            else
                throw new NestedRuntimeException("Business object type: " + busObjTypeName
                        + " not found.");
        } finally {
            if (rs != null)  rs.close();
            if (ps != null)  ps.close();
        }
    }

    private String clobToString(Clob clob) throws Exception {
        if (clob == null)
            return "";
        BufferedReader br = new BufferedReader(clob.getCharacterStream());
        StringBuffer retval = new StringBuffer();
        while (true) {
            try {
                String curLine = br.readLine();
                if (curLine == null)
                    return retval.toString();
                retval.append(curLine).append("\n");
            }
            catch (IOException e) {
                logger.error(e);
                throw new NestedRuntimeException("error reading clob: " +
                                                 e.getMessage());
            }
        }
    }

}