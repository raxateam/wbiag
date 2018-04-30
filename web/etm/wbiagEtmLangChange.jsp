<%@ include file="/system/wbheader.jsp" %>

<%@ page import='java.sql.ResultSet' %>
<%@ page import='java.sql.PreparedStatement' %>
<%@ page import='java.sql.Statement' %>
<%@ page import='javax.servlet.jsp.PageContext' %>

<%@ page import='com.workbrain.sql.DBConnection' %>
<%@ page import='com.workbrain.util.StringHelper' %>
<%@ page import='com.workbrain.server.data.AccessException' %>
<%@ page import='com.workbrain.server.jsp.taglib.sys.PagePropertyTag' %>
<%@ page import='com.workbrain.util.StringHelper' %>
<%@ page import='com.workbrain.server.jsp.taglib.util.WebPage' %>

<%@ page import='org.apache.log4j.*' %>

<wb:page type="VR" domain="VR" fillCurrentLocaleDomainWithDefaults="false">

  <%-- Generate the top of screen menus and the tabs according to parent/child relation in table vr_toc --%>
  <jsp:include page="/etm/etmMenu.jsp" flush="true">
    <jsp:param name="selectedTocID" value="10004" />
    <jsp:param name="parentID" value="10003" />
  </jsp:include>

  <wb:define id='clickLink' />

  <wb:define id='locale'>
    <%=JSPHelper.getWebLocale(request).getLanguage()%>
  </wb:define>

  <%-- Don't break this line, it will break the link --%>
  <wb:define id="thisPage"><%=request.getContextPath()%>/etm/wbiagEtmLangChange.jsp</wb:define>

  <body>

<%!
    private static PageContext pageContext = null;
    private static final Logger logger = Logger.getLogger("jsp.changeLang.etm");


    /*
      saves user's selected locale preference in the workbrain_user table.
      this method has been copied directly out of PageTag.java.
      It has been modified so that it extracts the userId from another source.
    */
    protected void saveSelectedLocale(String localeId) throws AccessException{
      Statement s = null;
      try {
        s = JSPHelper.getWebContext(pageContext).getConnection().createStatement();
        s.executeUpdate("update workbrain_user "+
                "set workbrain_user.wbll_id="+ JSPHelper.getWebContext(pageContext).getWebLocale().getId() +
                "  where wbu_id    = " + JSPHelper.getWebContext(pageContext).getLogin().getUserId());
      } catch (Exception e) {
        if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(e, e);}
        throw new AccessException ("Could not update user locale information");
      } finally {
        if (s!=null) {
          try {
            s.close();
          } catch (Exception e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(e.getMessage()); }
            throw new AccessException ("Could not close update statement used to write usre locale information");
          }
        }
      }
    }

%>

<%
    //check to see if the user wants to change the locale
    if ("true".equals(request.getParameter("localeSelected"))) {
      if (null != request.getParameter("locale")) {
        saveSelectedLocale(request.getParameter("locale"));
      }
    }


    //set the page context so other methods can use it.
    this.pageContext = pageContext;

    //this has been copied directly from /etm/login.java
    DBConnection dbc = (DBConnection) JSPHelper.getWebContext(pageContext)
              .getConnection();
    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      String localeUpdateQuery = "SELECT wbll_loc_name, wbll_language, wbll_country FROM vl_workbrain_locale";
      ps = dbc
          .prepareStatement(localeUpdateQuery);
      logger.debug("Locale Update Query" + localeUpdateQuery);
      rs = ps.executeQuery();
      while (rs.next()) {
        //is the curr loop lang the ETM's curr lang?
        boolean isSelected = StringHelper.trimAll(locale.toString()).equalsIgnoreCase(rs.getString(2));

        //don't create a button for the language currently being used.
        if (isSelected) continue;

        String country = rs.getString(3);
        String newlocale = rs.getString(2)
            + (StringHelper.isEmpty(country) ? "" : "_"
                + country);

%>
        <%-- The locale parameter has to be set to the locale name and localeSelected must be set to true--%>
        <wb:set id='clickLink'>window.location='#thisPage#?locale=<%=newlocale%>&localeSelected=true'</wb:set>
        <wb:vrbutton imageBased="true" selected='false' label='<%=rs.getString(1)%>' onClick="#clickLink#" />
<%
      }
    } catch (Exception e) {
      logger.error("wbiagEtmLangChange.jsp", e);
    } finally {
      if (rs != null) {
        rs.close();
        rs = null;
      }
      if (ps != null)
        ps.close();
    }
%>

  </body>

</wb:page>