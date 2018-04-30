<%@ taglib uri="/wbsys" prefix="wb" %>
<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.util.*" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.io.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="com.workbrain.sql.*" %>
<%@ page import="com.workbrain.util.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.workbrain.server.jsp.*" %>
<HTML>
<HEAD>
    <TITLE>Oracle Explain Plan</TITLE>
</HEAD>
<BODY>
<%!

    private static final String FIELDS = ""+
//          "STATEMENT_ID , TIMESTAMP , REMARKS ," +
            "ID ," +
            "PARENT_ID ," +
            //"POSITION ," +
            "OPERATION ," +
            "OPTIONS ," +
//          "OBJECT_NODE , OBJECT_OWNER ," +
            "OBJECT_NAME ," +
//          "OBJECT_INSTANCE ," +
            "OBJECT_TYPE ," +
            "OPTIMIZER ," +
            "SEARCH_COLUMNS ," +
            "COST ," +
            "CARDINALITY ," +
            "BYTES ";
//            "OTHER_TAG,PARTITION_START,PARTITION_STOP,PARTITION_ID,OTHER,DISTRIBUTION," +
//            "CPU_COST ,IO_COST ,TEMP_SPACE , ACCESS_PREDICATES , FILTER_PREDICATES ," +
//            "PROJECTION , TIME , QBLOCK_NAME ";

    private void explainPlan(JspWriter out, String sql, Connection conn) throws Exception{
        if (StringHelper.isEmpty(sql)) {
            return;
        }
        Statement stmt = null;
        ResultSet rs = null;
        try{
            stmt = conn.createStatement();
            String planId = "" + System.currentTimeMillis();
            stmt.executeUpdate("EXPLAIN PLAN SET STATEMENT_ID = '" + planId + "' INTO PLAN_TABLE FOR " + sql);

            rs = stmt.executeQuery("select " + FIELDS + " from plan_table "+
             "where statement_id = '" + planId + "' "+
             " CONNECT BY PRIOR id = parent_id" +
             "    AND statement_id = '" + planId + "' "+
             "   START WITH id = 0  " +
             "   AND statement_id = '" + planId + "' ORDER BY id, parent_id, position");

            out.println("<table border=1 class='contentTable'>");
            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();

            out.println("<tr>");
            for(int i = 1; i <= colCount;i++){
                out.println("<th>" + rsmd.getColumnName(i) + "</th>");
            }
            out.println("</tr>");

            Map spaces = new HashMap();
            final String ROOT_ID = "0";
            spaces.put(ROOT_ID , "1");
            while(rs.next()){
                String id = rs.getString("ID");
                String parent =   rs.getString("PARENT_ID");
                String fontColor = "";
                if(("CARTESIAN".equalsIgnoreCase(rs.getString("OPTIONS"))) ||
                   ("FULL".equalsIgnoreCase(rs.getString("OPTIONS"))))
                        fontColor = " bgcolor='#FF5555' ";

                int spaceCnt = 0;
                if (!id.equals(ROOT_ID)) {
                    if (spaces.containsKey(parent)) {
                        spaceCnt  = Integer.parseInt((String)spaces.get(parent)) + 1;
                    }
                    if (!spaces.containsKey(id)) {
                        spaces.put(id , String.valueOf(spaceCnt));
                    }
                }

                out.println("<tr " + fontColor + ">");
                for(int i = 1; i <= colCount;i++){
                    if (rsmd.getColumnName(i).equals("POSITION")) {
                        continue;
                    }
                    out.println("<td>");
                    if (rsmd.getColumnName(i).equals("OPERATION")) {
                        for (int k = 0 ; k < spaceCnt ; k++ ) {
                            out.println("&nbsp;");
                        }
                    }
                    out.println(rs.getString(i));
                    out.println("</td>");
                }
                out.println("</tr>");
            }
            out.println("</table>");
        }finally{
            SQLHelper.cleanUp(stmt);
        }
    }

    private void makeLast10SQLList(JspWriter out, HttpSession session, String currentSQL)throws Exception{

        String sessionAttribute = "PRIOR_SQLS";
        List lastSoManySQL = (List)session.getAttribute(sessionAttribute);
        if (lastSoManySQL == null){
            lastSoManySQL = new ArrayList();
            session.setAttribute(sessionAttribute, lastSoManySQL);
        }

        int indexOfOccurence = lastSoManySQL.indexOf(currentSQL);
        if (indexOfOccurence > -1){
            lastSoManySQL.remove(indexOfOccurence);
        }
        lastSoManySQL.add(0, currentSQL);
        out.print("<select onChange='document.forms[0].sql.value = this.options[this.selectedIndex].value;'>");
        out.print("<option value=\""+ currentSQL + "\" selected>" + currentSQL + "</option>");
        for(int i = 1; i < lastSoManySQL.size() ;i++){
            out.print("<option value=\"" + lastSoManySQL.get(i).toString() + "\">" + lastSoManySQL.get(i).toString() + "</option>");

        }
        out.print("</select>");

    }
%>
<wb:page>
<%=new Date()%>
<%
    Connection conn = JSPHelper.getConnection(request);
    String sql = request.getParameter("sql");
    if (sql == null) sql = "";
    session.getAttribute("");
%>
<br>
<input type=submit><%makeLast10SQLList(out, session, sql);%><br>

<textarea cols=100% rows=15 WRAP=SOFT name=sql><%=sql%></TextArea><br><br>
<%explainPlan(out, sql, conn);%>
</wb:page>
</BODY>
</HTML>