package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import java.sql.Timestamp;

/**
 * Compares employee attributes between current calc date and previous day and returns
 * true if any if attributes specified has changed <br>
 * EmployeeFields - comma delimited list of empprops i,e empVal1, empBaseRate
 *                  If any of props has changed, it will return true <br>
 * EmployeeTeamFields - comma delimited list of empteamprops i,e wbtId, emptVal1. Only home team is used in comparison <br>
 * EmployeeDefLabFields - comma delimited list of empdeflab proprs . Compare is only done iff empdeflabs have 1 record <br>
 *
 */
public class IsEmployeeChangedCondition    extends Condition {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsEmployeeChangedCondition.class);


    public static final String PARAM_EMPLOYEE_ATR_STRING = "EmployeeFields";
    public static final String PARAM_EMPLOYEE_TEAM_ATR_STRING = "EmployeeTeamFields";
    public static final String PARAM_EMPLOYEE_DEF_LAB_ATR_STRING = "EmployeeDefLabFields";


    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_EMPLOYEE_ATR_STRING,
            RuleParameterInfo.STRING_TYPE, true));

        result.add(new RuleParameterInfo(PARAM_EMPLOYEE_TEAM_ATR_STRING,
            RuleParameterInfo.STRING_TYPE, true));

        result.add(new RuleParameterInfo(PARAM_EMPLOYEE_DEF_LAB_ATR_STRING,
            RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    /**
     * Returns the name of the Rule
     *
     * @return String - the unique name of this rule component
     */
    public String getComponentName() {
        return "WBIAG: Is Employee Changed Condition";
    }

    /**
     * This method compares the number of days employeed
     * with the given number.
     *
     * @param wbData
     * @param parameters
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws
        Exception {

        Date employed = null;
        String empAtr = parameters.getParameter(PARAM_EMPLOYEE_ATR_STRING);
        String teamAtr = parameters.getParameter(PARAM_EMPLOYEE_TEAM_ATR_STRING);
        String defLabAtr  = parameters.getParameter(PARAM_EMPLOYEE_DEF_LAB_ATR_STRING);
        boolean bolResult = false;
        if (!StringHelper.isEmpty(empAtr)) {
            EmployeeData edCurrent = wbData.getRuleData().getEmployeeData();
            EmployeeData edYest  = wbData.getRuleData().getCalcDataCache()
                .getEmployeeData(wbData.getEmpId(), DateHelper.addDays(wbData.getWrksWorkDate() , -1),
                                 wbData.getDBconnection() , wbData.getCodeMapper() )  ;
            bolResult = isChanged(edCurrent , edYest, StringHelper.detokenizeStringAsList(empAtr , ","));
            if (logger.isDebugEnabled()) logger.debug("Compare result :" + bolResult + "\n employee current \n" + edCurrent + "\n with yesterday \n" + edYest + "\n for properties:" + empAtr);
        }
        if (!StringHelper.isEmpty(teamAtr)) {
            List teamsCurrent = wbData.getRuleData().getCalcDataCache()
                .getEmployeeTeams(wbData.getEmpId(), wbData.getWrksWorkDate(),
                                  wbData.getWrksWorkDate(),
                                  wbData.getDBconnection());
            List teamsYest = wbData.getRuleData().getCalcDataCache()
                .getEmployeeTeams(wbData.getEmpId(), DateHelper.addDays(wbData.getWrksWorkDate() , -1),
                                  wbData.getWrksWorkDate(),
                                  wbData.getDBconnection());
            EmployeeTeamData teamCurr = getHomeTeam(teamsCurrent);
            EmployeeTeamData teamYest = getHomeTeam(teamsYest);
            if (teamCurr == null || teamYest == null) {
                throw new RuntimeException ("Home team not found");
            }
            bolResult = isChanged(teamCurr , teamYest, StringHelper.detokenizeStringAsList(teamAtr , ","));
            if (logger.isDebugEnabled()) logger.debug("Compare result :" + bolResult + "\n employee team current \n" + teamCurr + "\n with yesterday team \n" + teamYest + "\n for properties:" + teamAtr);
        }
        if (!StringHelper.isEmpty(defLabAtr)) {
            List edlCurrent = wbData.getRuleData().getEmpDefaultLabor();
            List edlYest  = wbData.getRuleData().getCalcDataCache()
                .getEmployeeDefaultLaborRecords(wbData.getEmpId(), DateHelper.addDays(wbData.getWrksWorkDate() , -1),
                                 wbData.getDBconnection() , wbData.getCodeMapper() )  ;
            if (edlCurrent.size() == 1 && edlYest.size() == 1) {
                bolResult = isChanged((EmployeeDefaultLaborData)edlCurrent.get(0),
                                    (EmployeeDefaultLaborData)edlYest.get(0) ,
                                    StringHelper.
                                    detokenizeStringAsList(defLabAtr, ","));
                if (logger.isDebugEnabled()) logger.debug("Compare result :" + bolResult + "\n employee deflab current \n" + (EmployeeDefaultLaborData)edlCurrent.get(0) + "\n with yesterday deflab \n" + (EmployeeDefaultLaborData)edlYest.get(0) + "\n for properties:" + defLabAtr);

            }
        }


        return bolResult ;
    }

    private boolean isChanged(RecordData rd1, RecordData rd2, List props) {
        Iterator iter = props.iterator();
        boolean eval = false;
        while (iter.hasNext()) {
            String item = (String)iter.next();
            Class cls = (Class) rd1.getPropertyClass(item);
            if (cls == null) {
                throw new RuntimeException ("Property not found : " + item);
            }
            if (Integer.class.isAssignableFrom(cls) ||
                int.class.isAssignableFrom(cls) ||
                Double.class.isAssignableFrom(cls) ||
                double.class.isAssignableFrom(cls)) {
                Double r1 = (Double)rd1.getProperty(item) ;
                Double r2 = (Double)rd2.getProperty(item) ;
                eval = r1 == r2 || !(r1 == null || r2 == null) && r1.doubleValue() == r2.doubleValue();
            }
            else if (Timestamp.class.isAssignableFrom(cls)) {
                eval = DateHelper.equals((Timestamp)rd1.getProperty(item) , (Timestamp)rd2.getProperty(item)) ;
            }
            else if( Date.class.isAssignableFrom(cls)) {
                eval = DateHelper.equals((Date)rd1.getProperty(item) , (Date)rd2.getProperty(item)) ;
            }
            else if( String.class.isAssignableFrom(cls)){
                eval = StringHelper.equals((String)rd1.getProperty(item) , (String)rd2.getProperty(item)) ;
            }
            if (!eval) {
                return true;
            }
        }
        return false;
    }

    private EmployeeTeamData getHomeTeam(List teams) {
        Iterator iter = teams.iterator();
        while (iter.hasNext()) {
            EmployeeTeamData etd = (EmployeeTeamData) iter.next();
            if (("Y".equals(etd.getEmptHomeTeam()))) {
                return etd;
            }
        }
        return null;
    }
}

