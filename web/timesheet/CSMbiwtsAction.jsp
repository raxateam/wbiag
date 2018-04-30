<%@ include file="/system/wbheader.jsp"%>
<%@ page import="org.apache.log4j.*"%>
<%@ page import="com.workbrain.util.NestedRuntimeException"%>
<%@ page import="java.util.*" %>
<%@ page import="com.workbrain.app.jsp.*" %>
<%@ page import="com.workbrain.app.jsp.action.*" %>
<%@ page import="com.workbrain.server.jsp.JSPHelper" %>
<%@ page import="com.workbrain.sql.DBConnection" %>
<%@ page import="com.workbrain.util.StringHelper" %>
<%@ page import="com.workbrain.server.data.AccessException"%>
<%@ page import="javax.servlet.*" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="javax.naming.*" %>
<%@ page import="javax.naming.*"%>
<%@ page import="java.io.*" %>
<%@ page import="java.sql.SQLException"%>
<%@ page import="com.workbrain.server.jsp.proxy.*" %>
<%@ page import="com.workbrain.util.callouts.*" %>
<%!
    private static Map factories = new HashMap();
    private static final Logger actionLogger = Logger.getLogger("jsp.action");

    private boolean isDebug( ServletConfig config ) throws ServletException {
	String logLevel = config.getInitParameter( "log.level" );
	if( logLevel == null ) {
		logLevel = "error";
	}

	if( "debug".equalsIgnoreCase( logLevel ) ) {
		return true;
	} else {
		return false;
	}
    }

    protected String process( HttpServletRequest request, HttpServletResponse response, PageContext pageContext )
              throws IOException {
        String forwardUrl = null;
        Object reqModel = null;
        Object customReqModel = null;
        DBConnection dbConn = null;
        try {
            dbConn = JSPHelper.getConnection( request );

    	    actionLogger.debug( "dbConn = " + dbConn );

            Action action = null;
            String factoryClassPath = "com.wbiag.app.jsp.action.biweeklytimesheet.CSMActionFactory";
            ActionFactory factory = (ActionFactory)factories.get( factoryClassPath );
            if( factory == null ) {
                factory = (ActionFactory)Class.forName( factoryClassPath ).newInstance();
                factories.put( factoryClassPath, factory );
            }
            // always reset the prefix and suffix - needed when this action.jsp servlet is being
            // used by more than one module
            factory.initialize( pageContext.getServletConfig() );

            action = factory.createAction(request);
            String actionName = TimesheetUtil.getFullActionName(pageContext.getServletConfig(), request);

            ProxyHttpServletRequest proxyreq = new ProxyHttpServletRequest(request, "UTF-8", true);
            ActionContext actCtx= new ActionContext(proxyreq);
            TimeSheetUICallout tsCallOut = CalloutFactory.getInstance().getTimesheetUICallout();
            reqModel = proxyreq.getAttribute( ActionContext.REQUEST_ATTR );
            if( reqModel == null ) {
				boolean isDebug = isDebug( pageContext.getServletConfig() );
		        if( isDebug ) {
					actionLogger.debug( "*** URI = " + proxyreq.getRequestURI() );
					//JSPHelper.printParameters( request );
				}
				tsCallOut.preCreateRequest(actionName, proxyreq);
                reqModel = action.createRequest( proxyreq, dbConn );
				customReqModel = tsCallOut.postCreateRequest(actionName,proxyreq,reqModel);
				if( isDebug ) {
					actionLogger.debug("*** REQ = " + reqModel);
					//actionLogger.debug( className + "()" );
				}
            }else{
            	customReqModel = reqModel;
            }
	    	tsCallOut.preActionProcess(actionName, actCtx, customReqModel);
		    forwardUrl = action.process( dbConn, actCtx, customReqModel );
			String customUrl = tsCallOut.postActionProcess(actionName, actCtx, customReqModel, forwardUrl);

            if(!StringHelper.isEmpty(customUrl)){
            	forwardUrl = customUrl;
            }

            dbConn.commit();
        } catch( SQLException exc ) {
            try {
                dbConn.rollback();
            } catch (Exception e) {
                actionLogger.error(e);
            }
            throw new NestedRuntimeException( "Error during database access: " + exc.getMessage(), exc );
        } catch( AccessException exc ) {
            try {
                dbConn.rollback();
            } catch (Exception e) {
                actionLogger.error(e);
            }
            throw new NestedRuntimeException( "Error during database access: " + exc.getMessage(), exc );
        } catch( Throwable t ) {
            try {
                dbConn.rollback();
            } catch (Throwable t2) {
                actionLogger.error(t2);
            }
            throw new NestedRuntimeException( "Error: " + t.getMessage(), t );
        } finally {
            try {
                dbConn.close();
            } catch (Exception e) {
                actionLogger.error(e);
            }
        }

	if( forwardUrl != null ) {
	    request.setAttribute( ActionContext.REQUEST_ATTR, reqModel );
	}

	return forwardUrl;
    }
%>
    <wb:page emitHtml='false' subsidiaryPage="true" securityValidation="none">
<%
	String forwardUrl = process( request, response, pageContext );
        try {
            actionLogger.debug("*** Forwarding to " + forwardUrl );
            if( forwardUrl != null ) {
				out.print(JSPHelper.getSecurityFactorHiddenField());
%>
    <wb:forward page="<%=forwardUrl%>"/>
<%
	    }
        } catch( Exception exc ) {
            throw new NestedRuntimeException( "Error during redirecting to " + forwardUrl, exc );
        }
%>
</wb:page>
