package com.wbiag.app.jsp.action.biweeklytimesheet;

import com.workbrain.app.jsp.Action;
import com.workbrain.app.jsp.action.ActionFactory;

import javax.servlet.*;
import javax.servlet.http.*;

import java.util.*;

public class CSMActionFactory implements ActionFactory {
  private String prefix;
  private Map actions = new HashMap();

  public void initialize( ServletConfig servletConfig ) throws Throwable {
    prefix = "com.wbiag.app.jsp.action.biweeklytimesheet.";
  }

  public Action createAction( HttpServletRequest request ) throws Throwable {
    String classPath = prefix + request.getParameter("action");
    Action action = (Action)actions.get( classPath );
    if( action == null ) {
      action = (Action)Class.forName(classPath).newInstance();
      actions.put( classPath, action );
    }

    return action;
  }
}