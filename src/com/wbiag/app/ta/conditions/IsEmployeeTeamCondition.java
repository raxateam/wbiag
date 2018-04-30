package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;

/**
 * The condition compares the employee's home/temp team name as of calculation date with given team name and operator <br>
 * If TEMP or ALL team option is selected, then condition is satisfied if any of team satisfy the pattern<br>
 * i.e Emp. Home Team = NW TRAIN , HomeTeam=Y <br>
 *     TeamString = *TRAIN* Operator =  WILDCARD Returns true
 *     TeamString = TRAIN,ADMIN Operator =  IN Returns false
 *     TeamString = TRAIN Operator =  EQUALS Returns false
 *
 */
public class IsEmployeeTeamCondition    extends Condition {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsEmployeeTeamCondition.class);


    public static final String PARAM_TEAM_STRING = "TeamString";
    public static final String PARAM_HOME_TEAM = "HomeTeam";
    public static final String PARAM_VAL_TEMP = "N";
    public static final String PARAM_VAL_HOME = "Y";
    public static final String PARAM_OPERATOR = "Operator";
    public static final String PARAM_VAL_WILDCARD = "WILDCARD";
    public static final String PARAM_VAL_IN = "IN";
    public static final String PARAM_VAL_EQUALS = "EQUALS";


    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        RuleParameterInfo rpiTeam = new RuleParameterInfo(PARAM_TEAM_STRING,
            RuleParameterInfo.STRING_TYPE, false);
        result.add(rpiTeam);

        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_OPERATOR,
            RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(PARAM_VAL_WILDCARD);
        rpi.addChoice(PARAM_VAL_EQUALS);
        rpi.addChoice(PARAM_VAL_IN);
        result.add(rpi);

        RuleParameterInfo rpth = new RuleParameterInfo(PARAM_HOME_TEAM,
            RuleParameterInfo.CHOICE_TYPE, true);
        rpth.addChoice(PARAM_VAL_TEMP);
        rpth.addChoice(PARAM_VAL_HOME);
        result.add(rpth);


        return result;
    }

    /**
     * Returns the name of the Rule
     *
     * @return String - the unique name of this rule component
     */
    public String getComponentName() {
        return "WBIAG: Is Employee Team Condition";
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
        String teamStr = parameters.getParameter(PARAM_TEAM_STRING);
        String operator = parameters.getParameter(PARAM_OPERATOR);
        String tempHT  = parameters.getParameter(PARAM_HOME_TEAM);
        boolean bolResult = false;
        if (PARAM_VAL_HOME.equals(tempHT)) {
            int teamId = wbData.getRuleData().getCalcDataCache()
                .getEmployeeHomeTeam(wbData.getEmpId(), wbData.getWrksWorkDate(),
                                  wbData.getDBconnection());
            String teamName = wbData.getCodeMapper().getWBTeamById(teamId).getWbtName();
            bolResult = evaluate(teamStr , teamName , operator);
            if (logger.isDebugEnabled()) logger.debug("Home team :" + teamName + ", Team String : " + teamStr + ", returning :" + bolResult);
        }
        else {
            List teams = wbData.getRuleData().getCalcDataCache()
                .getEmployeeTeams(wbData.getEmpId(), wbData.getWrksWorkDate(),
                                  wbData.getWrksWorkDate(),
                                  wbData.getDBconnection());
            Iterator iter = teams.iterator();
            while (iter.hasNext()) {
                EmployeeTeamData etd = (EmployeeTeamData) iter.next();
                if (("N".equals(etd.getEmptHomeTeam()) && PARAM_VAL_TEMP.equals(PARAM_HOME_TEAM))
                    || (StringHelper.isEmpty(PARAM_HOME_TEAM))
                    ) {
                    String teamName = wbData.getCodeMapper().getWBTeamById(etd.getWbtId()).getWbtName();
                    bolResult |= evaluate(teamStr , teamName , operator);
                    if (logger.isDebugEnabled()) logger.debug("Team :" + teamName + ", Team String : " + teamStr + ", evaluated :" + bolResult);
                    if (bolResult) break;
                }
            }
        }

        return bolResult ;
    }

    private boolean evaluate(String teamString, String teamName, String opr) {
        if (PARAM_VAL_WILDCARD.equals(opr)) {
            return StringHelper.isWildcardMatch(teamString , teamName  ) ;
        }
        else if (PARAM_VAL_EQUALS.equals(opr)) {
            return StringHelper.equals(teamString , teamName);
        }
        else if (PARAM_VAL_IN.equals(opr)) {
            return StringHelper.isItemInList(teamString , teamName);
        }
        else {
            throw new RuntimeException("Operator not supported :" + opr);
        }
    }
}

