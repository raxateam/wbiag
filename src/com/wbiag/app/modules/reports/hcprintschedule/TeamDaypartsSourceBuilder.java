package com.wbiag.app.modules.reports.hcprintschedule;

import com.workbrain.server.data.*;
import com.workbrain.sql.*;
import com.workbrain.server.WebLogin;

/**
 * @author          Ali Ajellu
 * @version         1.0
 * Date:            Friday, June 16 206
 * Copyright:       Workbrain Corp.
 * TestTrack:       1630
 *
 * TeamDaypartsSourceBuilder is the builder class for TeamDaypartsSource. It receives
 * parameters from user, fixes and validates them and passes them along to TeamDaypartsSource.
 * Once TeamDaypartsSource has returned a result, this class returns its result as well.
 */
public class TeamDaypartsSourceBuilder implements RowSourceBuilder {

	private static org.apache.log4j.Logger logger =
		org.apache.log4j.Logger.getLogger(TeamDaypartsSourceBuilder.class);

    /**
     * Default constructor.
     */
	public TeamDaypartsSourceBuilder(){
	}

    /**
     * What is the name of the row source?
     * @return Name of the row source.
     */
	public String getName() {
		return "TEAM_DAYPARTS";
	}

    /**
     * Defines the parameters that are expected from user.
     * "connection" and "webLogin" are provided by system automatically if row source is used in a JSP.
     * "where" and "orderBy" parameters must explicitly be described in the sourceParams param in the
     * uiParameter of a wb:controlField.
     *
     * @return the param definition of this rowSource.
     */
	public ParameterList getParameters() {
		ParameterListImpl params = new ParameterListImpl();

        // We need to define the "connection", "where", "orderBy"and "webLogin" parameters since this rowSource will
        // mostly be used through a JSP page -- Their definition is a requirement.
		params.add (new ParameterImpl ("connection", DBConnection.class.getName(),true, true, "Database Connection"));
		params.add (new ParameterImpl ("where", String.class.getName(),true, false, "Team Ids"));
		params.add (new ParameterImpl ("orderBy", String.class.getName(),true, false, "TEAM_DAYPARTS rowsource doesn't use orderBY param"));
		params.add (new ParameterImpl ("webLogin", WebLogin.class.getName(),true, false, "TEAM_DAYPARTS rowsource doesn't use webLogin param"));

        log("Returning params: connection, where, orderBy, webLogin");

		return params;
	}

	/**
     * Creates a new instance of the Row Source.
     *
     * @param paramList A completed parameter list
     * @return A new instace of the Row Source (TeamDaypartsSource)
     * @throws InstantiationException Thrown if the Source throws an Access Exception (SQL or Row errors)
     * @throws IllegalArgumentException If the connection == null
     *
	 */
	public RowSource newInstance(ParameterList paramList) throws InstantiationException, IllegalArgumentException {
		RowSource rs = null;
		Parameter conn = paramList.findParam("connection");

		if (conn == null)
			throw new IllegalArgumentException ("Connection parameter not set");

		paramList.validateParams();
        log("validateParams is called for the TeamDayPartSetBuilder");

		try{
			rs = new TeamDaypartsSource ((DBConnection) conn.getValue(), paramList);
		}catch(AccessException e){
			throw new InstantiationException (e.toString());
		}

		return rs;
	}

    private void log(String msg) {
        if (logger.isDebugEnabled()) logger.debug(msg);
    }
}
