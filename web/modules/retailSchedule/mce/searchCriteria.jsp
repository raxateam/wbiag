<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*,com.workbrain.server.jsp.taglib.util.*"%>
<%@ page import="com.wbiag.app.modules.retailSchedule.db.*"%>
<%@ page import="com.wbiag.app.modules.retailSchedule.model.*"%>
<%@ page import="com.wbiag.app.modules.retailSchedule.mce.MCEProcess"%>

<%!
	private static final String MODE_NEW = "NEW";
	private static final String MODE_SAVE = "SAVE";
    private static final String MODE_DELETE = "DELETE";
%>

<wb:page login='true'>
<script language="JavaScript">
   function validate(){
	  if (document.page_form.MCESS_TYPE[0].checked && document.page_form.record_id.value != "" ) {
	     for(var i=1; i<= 5; i++) {
		    if (document.page_form.elements[ "SEARCH_SET_" + i ].value == document.page_form.record_id.value ) {
			   alert("Change Search Set " + i + " to null or another existing search set.");
			   return false;
			}
		 }
	  }
	  var existingSetSelected = false;
      for(var i=1; i<= 5; i++) {
	     if (document.page_form.elements[ "SEARCH_SET_" + i ].value != "") {
		    existingSetSelected = true;	  
		 }
	  }
	  if (document.page_form.MCESS_TYPE[0].checked && !existingSetSelected) {
	     alert("At least one existing search set is required when aggregating multiple Search Sets.");
         return false;
	  }
	  if (document.page_form.MCESS_TYPE[1].checked
          && document.page_form.WIMCESS_HIERNODE_label.value == "" 
		  && document.page_form.WIMCESS_LOCLIST.value == "") {
          alert("Please enter either a Hierarchy Node or at least one Specific Location.");
          return false;
      }
	  if (document.page_form.MCESS_TYPE[1].checked) {
		  if (document.page_form.WIMCESS_HIERNODE_label.value != "" && document.page_form.WIMCESS_LOCLIST.value != "") {
             alert("Hierarchy Node and Specific Location can't be selected together.");
             return false;
          }
	  }
      return true;
  }

  function validateProperty(){
     //alert(document.page_form.PROPERTY_STRING.value);
	 var strPropClass = "java.lang.String";
	 var intPropClass = "java.lang.Integer"; 
	 var booPropClass = "java.lang.Boolean";
	 var propertyStr = document.page_form.PROPERTY_STRING.value;

	 for(var i=1; i<= 7; i++) {
        var selectedPropertyId = document.page_form.elements["MCESS_PROP_" + i].value;
		var selectedPropertyLabel = document.page_form.elements["MCESS_PROP_" + i + "_label"].value;
		var min = document.page_form.elements["MCESS_PROP_" + i + "_MIN"].value;
		var max = document.page_form.elements["MCESS_PROP_" + i + "_MAX"].value;

		if (selectedPropertyId == "") {
		   document.page_form.elements["MCESS_PROP_" + i + "_MIN"].value = "";
		   document.page_form.elements["MCESS_PROP_" + i + "_MAX"].value = "";
		}
		if (selectedPropertyId != "" && min =="") {
		   alert("When selecting a store property, a minimum (or matching) value has to be specified as well.");
           return false;
		} 
		if (selectedPropertyId != "") {
		   var array = propertyStr.split("~");
	       for(var j=0; j<array.length; j++) {
              if(array[j] != "") {
		         var propId = array[j].substring(0, array[j].indexOf("="));
			     var propValue = array[j].substring(array[j].indexOf("=") + 1);
                 if (selectedPropertyId == propId) {
			        if (propValue == strPropClass) {
			           document.page_form.elements["MCESS_PROP_" + i + "_MAX"].value = "";
			        }
			        else if (propValue == intPropClass) {
			           if (!isInt(min)) {
				          alert("Min Value for property " + selectedPropertyLabel + " must be an integer.");
                          return false;
				       }
					   else if ( max != "" && !isInt(max)) {
					      alert("Max Value for property " + selectedPropertyLabel + " must be an integer or null.");
                          return false;
					   }
			        }
					else if (propValue == booPropClass) {
					   document.page_form.elements["MCESS_PROP_" + i + "_MAX"].value = "";
			           if ( !(min == "Y" || min == "N")) {
				          alert("Min Value for property " + selectedPropertyLabel + " must be 'Y' or 'N'.");
                          return false;
				       }  
			        }
					else {
					   //Only String, Integer or Boolean property classes are allowed.(ref. so_property table....)
					   alert("Property class for selected property " + selectedPropertyLabel + " must be java.lang.String or java.lang.Integer or java.lang.Boolean.");
                       return false;
					}
			     }  
		      }
           }
	    }
     }
     return true;
  }

  function validateName(){
     if (document.page_form.WIMCESS_NAME.value == "") {
        alert("Search Set Name is required when saving a search.");
        return false;
     }
     return true;
  }

  function isInt(str){
    var s = str.trimBothSides();
    for(var i = 0, c; i != s.length; i++){
       c = s.substring(i,i+1);
       if((i>0 || c!='-') && (c < '0' || c > '9')) return false;
    }
    return true;
  }

  function checkSave() {
     if( validateName() && validate() && validateProperty() ) {
         document.page_form.MODE.value  = "SAVE";
         disableAllButtons();
         document.page_form.submit();
     }
  }
  function checkDelete() {
     document.page_form.MODE.value  = "DELETE";
     disableAllButtons();
     document.page_form.submit();
  }
  function checkSearch() {
     if( validate() && validateProperty() ) {
		document.forms[0].action = '<%=request.getContextPath()%>/modules/retailSchedule/mce/usageMode.jsp';
        disableAllButtons();
        document.page_form.submit();
     }
  }
</script>
   <wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" /></wb:define>
   <wb:submit id="mfrm_id"><wb:get id="mfrm_id"/></wb:submit>
   <wb:define id="record_id"><wb:get id="record_id" scope="parameter" default=""/></wb:define>
   <wb:define id="MODE"><wb:get id="MODE" scope="parameter" default=""/></wb:define>

   <style>.box { border-color: #909090; border-style: solid; border-width: 1px;} </style>
<%  
    DBConnection conn = JSPHelper.getConnection(request);
    MCEProcess mcep = new MCEProcess(conn);
	LocalizationHelper lh = new LocalizationHelper(pageContext);
    String  propValidationString = mcep.getSoPropertyString();
	
    String mfrmIdStr = mfrm_id.toString();
    if (StringHelper.isEmpty(mfrmIdStr)) {
        throw new RuntimeException("mfrm_id must be supplied in querystring, check mfrm_parameter_jps setting for this maintenance form");
    }
	String goBack =  "location.href = '" + request.getContextPath() + "/maintenance/mntForms.jsp?mfrm_id=" + mfrmIdStr + "'; return false;";
	int mfrmId = Integer.parseInt(mfrmIdStr);

	String mode = MODE.toString();

	if (StringHelper.isEmpty(mode)) {
        mode = MODE_NEW;
    }
	String recordId = request.getParameter("record_id");
	try {
	   if (MODE_SAVE.equals(mode)) {
		  mcep = new MCEProcess(conn, request);
		  if (StringHelper.isEmpty(recordId) || "null".equals(recordId)) {
             recordId = Integer.toString(mcep.processInsert());
		  }
		  else {
		     recordId = Integer.toString(mcep.processUpdate());
		  }
       }
	   else if (MODE_DELETE.equals(mode)) {
          if (!StringHelper.isEmpty(recordId) && !"null".equals(recordId)) {
			  mcep.processDelete(Integer.parseInt(recordId));
		  }
		  response.sendRedirect(request.getContextPath() + "/maintenance/mntForms.jsp?mfrm_id=" + mfrmIdStr);
	   }
	}catch(Exception e) {
           
%>
        <span>Search Set Data : <%=request.getParameter("WIMCESS_NAME")%> was not processed.  The following error occured:</span>
        <P><%=e%></P>

<%  }

    //Initilizing veribales
	String mcessNameStr = "";
	String mcessDescStr = "";
	String mcessSub1IdStr = "";
	String mcessSub2IdStr = "";
	String mcessSub3IdStr = "";
	String mcessSub4IdStr = "";
	String mcessSub5IdStr = "";
	String mcessHiernodeStr = "";
	String clientTypIdStr = "";
	String mcessProp1IdStr = "";
	String mcessProp2IdStr = "";
	String mcessProp3IdStr = "";
	String mcessProp4IdStr = "";
	String mcessProp5IdStr = "";
	String mcessProp6IdStr = "";
	String mcessProp7IdStr = "";
	String mcessLoclistStr = "";
	String mcessMin1Str = "";
	String mcessMax1Str = "";
	String mcessMin2Str = "";
	String mcessMax2Str = "";
	String mcessMin3Str = "";
	String mcessMax3Str = "";
	String mcessMin4Str = "";
	String mcessMax4Str = "";
	String mcessMin5Str = "";
	String mcessMax5Str = "";
	String mcessMin6Str = "";
	String mcessMax6Str = "";
	String mcessMin7Str = "";
	String mcessMax7Str = "";
	boolean isType1 = false;
	boolean isType2 = true;
	
	if (!StringHelper.isEmpty(recordId) && !MODE_DELETE.equals(mode)) {
	   MCESearchSetAccess mceSearchSetAccess = new MCESearchSetAccess(JSPHelper.getConnection(request));
	   MCESearchSetData mceSearchSetData = mceSearchSetAccess.loadByMcessId(Integer.parseInt(recordId));
	   String mcessName = mceSearchSetData.getWimcessName();
	   String mcessDesc = mceSearchSetData.getWimcessDesc();
	   Integer mcessType = mceSearchSetData.getWimcessType();
	   Integer mcessSub1Id = mceSearchSetData.getWimcessSub1Id();
	   Integer mcessSub2Id = mceSearchSetData.getWimcessSub2Id();
	   Integer mcessSub3Id = mceSearchSetData.getWimcessSub3Id();
	   Integer mcessSub4Id = mceSearchSetData.getWimcessSub4Id();
	   Integer mcessSub5Id = mceSearchSetData.getWimcessSub5Id();
	   Integer mcessHiernode = mceSearchSetData.getWimcessHiernode();
	   String mcessLoclist = mceSearchSetData.getWimcessLoclist();
	   Integer clientTypId = mceSearchSetData.getClnttypId();
	   Integer mcessProp1Id = mceSearchSetData.getWimcessProp1Id();
       String mcessMin1 = mceSearchSetData.getWimcessMin1();
       String mcessMax1 = mceSearchSetData.getWimcessMax1();
	   Integer mcessProp2Id = mceSearchSetData.getWimcessProp2Id();
       String mcessMin2 = mceSearchSetData.getWimcessMin2();
       String mcessMax2 = mceSearchSetData.getWimcessMax2();
	   Integer mcessProp3Id = mceSearchSetData.getWimcessProp3Id();
       String mcessMin3 = mceSearchSetData.getWimcessMin3();
       String mcessMax3 = mceSearchSetData.getWimcessMax3();
	   Integer mcessProp4Id = mceSearchSetData.getWimcessProp4Id();
       String mcessMin4 = mceSearchSetData.getWimcessMin4();
       String mcessMax4 = mceSearchSetData.getWimcessMax4();
	   Integer mcessProp5Id = mceSearchSetData.getWimcessProp5Id();
       String mcessMin5 = mceSearchSetData.getWimcessMin5();
       String mcessMax5 = mceSearchSetData.getWimcessMax5();
	   Integer mcessProp6Id = mceSearchSetData.getWimcessProp6Id();
       String mcessMin6 = mceSearchSetData.getWimcessMin6();
       String mcessMax6 = mceSearchSetData.getWimcessMax6();
	   Integer mcessProp7Id = mceSearchSetData.getWimcessProp7Id();
       String mcessMin7 = mceSearchSetData.getWimcessMin7();
       String mcessMax7 = mceSearchSetData.getWimcessMax7();
	   isType1 = (mcessType.intValue() == 1);
	   isType2 = (mcessType.intValue() == 2);  
       
	   //form variables
	   mcessSub1IdStr = mcessSub1Id == null ? "" : mcessSub1Id.toString();
	   mcessSub2IdStr = mcessSub2Id == null ? "" : mcessSub2Id.toString();
	   mcessSub3IdStr = mcessSub3Id == null ? "" : mcessSub3Id.toString();
	   mcessSub4IdStr = mcessSub4Id == null ? "" : mcessSub4Id.toString();
	   mcessSub5IdStr = mcessSub5Id == null ? "" : mcessSub5Id.toString();
	   mcessProp1IdStr = mcessProp1Id == null ? "" : mcessProp1Id.toString();
	   mcessProp2IdStr = mcessProp2Id == null ? "" : mcessProp2Id.toString();
	   mcessProp3IdStr = mcessProp3Id == null ? "" : mcessProp3Id.toString();
	   mcessProp4IdStr = mcessProp4Id == null ? "" : mcessProp4Id.toString();
	   mcessProp5IdStr = mcessProp5Id == null ? "" : mcessProp5Id.toString();
	   mcessProp6IdStr = mcessProp6Id == null ? "" : mcessProp6Id.toString();
	   mcessProp7IdStr = mcessProp7Id == null ? "" : mcessProp7Id.toString();
	   mcessHiernodeStr = mcessHiernode == null ? "" : mcessHiernode.toString(); 
	   clientTypIdStr = clientTypId == null ? "" : clientTypId.toString();
	   mcessLoclistStr = mcessLoclist == null ? "" : mcessLoclist;
	   mcessNameStr = mcessName == null ? "" : mcessName;
	   mcessDescStr = mcessDesc == null ? "" : mcessDesc;
	   mcessMin1Str = mcessMin1 == null ? "" : mcessMin1;
	   mcessMin2Str = mcessMin2 == null ? "" : mcessMin2;
	   mcessMin3Str = mcessMin3 == null ? "" : mcessMin3;
	   mcessMin4Str = mcessMin4 == null ? "" : mcessMin4;
	   mcessMin5Str = mcessMin5 == null ? "" : mcessMin5;
	   mcessMin6Str = mcessMin6 == null ? "" : mcessMin6;
	   mcessMin7Str = mcessMin7 == null ? "" : mcessMin7;
	   mcessMax1Str = mcessMax1 == null ? "" : mcessMax1;
	   mcessMax2Str = mcessMax2 == null ? "" : mcessMax2;
	   mcessMax3Str = mcessMax3 == null ? "" : mcessMax3;
	   mcessMax4Str = mcessMax4 == null ? "" : mcessMax4;
	   mcessMax5Str = mcessMax5 == null ? "" : mcessMax5;
	   mcessMax6Str = mcessMax6 == null ? "" : mcessMax6;
	   mcessMax7Str = mcessMax7 == null ? "" : mcessMax7;  
	}

	String searchSetSql = "SELECT WIMCESS_ID, WIMCESS_NAME, WIMCESS_DESC " +
                          "FROM WBIAG_MCE_SRCHSET " +
				          "WHERE WIMCESS_TYPE = 2 ";
    String searchSetUiParams = "width=25 multiChoice=false pageSize=10 title='Search Set' sourceType=SQL source=\"" + searchSetSql + "\"";
	String storePropertySql = "SELECT PROP_ID, PROP_NAME, PROP_DESC " +
                              "FROM SO_PROPERTY ";
    String storePropertyUiParams = "width=25 multiChoice=false pageSize=10 title='Store Property' sourceType=SQL source=\"" + storePropertySql + "\"";
	String clientTypSql = "SELECT CLNTTYP_ID, CLNTTYP_NAME FROM SO_CLIENT_TYPE ";
	String clientTypUiParams = "width=25 multiChoice=false pageSize=10 title='Client Type' sourceType=SQL source=\"" + clientTypSql + "\"";	
%>

   <wba:table caption="Mass Configuration Search Criteria" captionLocalizeIndex="Mass_Configuration_Search_Criteria">
   <tr>
      <td width='20%'>
         <%out.print(lh.getCaption("WIMCESS_NAME", LocalizableTag.CONFIG_FIELD, mfrmId, "Search Set Name"));%>
         <span>:</span>
	  </td>
	  <td width='80%' align='left'>
         <wb:controlField submitName="WIMCESS_NAME" ui="StringUI" uiParameter="width=30"><%=mcessNameStr%></wb:controlField>
      </td>
   </tr>

   <tr>
      <td width='20%'>
         <%out.print(lh.getCaption("WIMCESS_DESC", LocalizableTag.CONFIG_FIELD, mfrmId, "Search Set Description"));%>
         <span>:</span>
	  </td>
	  <td width='80%' align='left'>
	    <wb:controlField submitName="WIMCESS_DESC" ui="StringUI" uiParameter="width=40"><%=mcessDescStr%></wb:controlField>
      </td>
   </tr>

   <tr>
      <td colspan='2'>
         <div class='box'>
         <table>
            <tr>
			   <td colspan='5'>
                  <input type='radio' name='MCESS_TYPE' value='1' <%=isType1 ? "checked" : ""%>><wb:localize id='Select_up_to_five_existing_search_sets_to_be_combined_in_an_OR_query'>Select up to five existing search sets to be combined in an OR query</wb:localize>
	           </td>
	        </tr>
            <tr>
	           <td>
			      <wb:localize id="Search_Set_1">Search Set 1</wb:localize>
				  <wb:controlField ui='DBLookupUI' submitName="SEARCH_SET_1" uiParameter='<%=searchSetUiParams%>'><%=mcessSub1IdStr%></wb:controlField>
	           </td>

	           <td>
			      <wb:localize id="Search_Set_2">Search Set 2</wb:localize>
				  <wb:controlField ui='DBLookupUI' submitName="SEARCH_SET_2" uiParameter='<%=searchSetUiParams%>'><%=mcessSub2IdStr%></wb:controlField>
	           </td>

	           <td>
			      <wb:localize id="Search_Set_3">Search Set 3</wb:localize>
				  <wb:controlField ui='DBLookupUI' submitName="SEARCH_SET_3" uiParameter='<%=searchSetUiParams%>'><%=mcessSub3IdStr%></wb:controlField>
	           </td>

	           <td>
			      <wb:localize id="Search_Set_4">Search Set 4</wb:localize>
				  <wb:controlField ui='DBLookupUI' submitName="SEARCH_SET_4" uiParameter='<%=searchSetUiParams%>'><%=mcessSub4IdStr%></wb:controlField>
	           </td>

	           <td>
			      <wb:localize id="Search_Set_5">Search Set 5</wb:localize>
				  <wb:controlField ui='DBLookupUI' submitName="SEARCH_SET_5" uiParameter='<%=searchSetUiParams%>'><%=mcessSub5IdStr%></wb:controlField>
	           </td>
	        </tr>
         </table>
         </div>
      </td>
   </tr>

   <tr>
      <td>
         <wb:localize id="OR">OR</wb:localize>
      </td>
   </tr>

   <tr>
      <td colspan='2'>
          <div class='box'>
	      <table>
		     <tr>
			    <td colspan='4'>
				    <input type='radio' name='MCESS_TYPE' value='2' <%=isType2 ? "checked" : ""%>><wb:localize id='Select_any_number_of_search_criteria_to_be_combined_in_an_AND_query'>Select any number of search criteria to be combined in an AND query</wb:localize>
				</td>
			 </tr>

			 <tr>
			    <td>
                   <% out.print(lh.getCaption("WIMCESS_HIERNODE", LocalizableTag.CONFIG_FIELD, mfrmId, "Hierarchy Node"));%>
				   <span>:</span>
				</td>
				<td>
				   <wb:controlField ui='LocationTreeUI' submitName="WIMCESS_HIERNODE" ><%=mcessHiernodeStr%></wb:controlField>
				</td>
				<td>
				   <span> OR </span>
                   <% out.print(lh.getCaption("WIMCESS_LOCLIST", LocalizableTag.CONFIG_FIELD, mfrmId, "Specific Location"));%>
				   <span>:</span>
				</td>
				<td>
                   <%out.print(lh.getUI("WIMCESS_LOCLIST", "WIMCESS_LOCLIST", mcessLoclistStr, null, "edit", mfrmId));%>
				</td>
			 </tr>

			 <tr>
			    <td>
                   <% out.print(lh.getCaption("CLNTTYP_ID", LocalizableTag.CONFIG_FIELD, mfrmId, "Department Type"));%>
				   <span>:</span>
				</td>
				<td>
				   <wb:controlField ui='DBLookupUI' submitName="CLNTTYP_ID" uiParameter='<%=clientTypUiParams%>'><%=clientTypIdStr%></wb:controlField>
				</td>
				   &nbsp;
				</td>
				<td>
                   &nbsp;
				</td>
			 </tr>

			 <tr>
			    <td colspan='4'>
				   <wba:table>
				      <tr>
					     <th>
						    <wb:localize id="Store_Property">Store Property</wb:localize>
						 </th>
						 <th>
						    <wb:localize id="Min_Value">Min Value<br>(or Match)</wb:localize>
						 </th>
						 <th>
						    <wb:localize id="Max_Value">Max Value<br>(optional)</wb:localize>
						 </th>
					  </tr>
					  <tr>
					     <td>
						    <wb:controlField ui='DBLookupUI' submitName="MCESS_PROP_1" uiParameter='<%=storePropertyUiParams%>'><%=mcessProp1IdStr%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_1_MIN"><%=mcessMin1Str%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_1_MAX"><%=mcessMax1Str%></wb:controlField>
						 </td>
					  </tr>
					  <tr>
					     <td>
						    <wb:controlField ui='DBLookupUI' submitName="MCESS_PROP_2" uiParameter='<%=storePropertyUiParams%>'><%=mcessProp2IdStr%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_2_MIN"><%=mcessMin2Str%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_2_MAX"><%=mcessMax2Str%></wb:controlField>
						 </td>
					  </tr>
					  <tr>
					     <td>
						    <wb:controlField ui='DBLookupUI' submitName="MCESS_PROP_3" uiParameter='<%=storePropertyUiParams%>'><%=mcessProp3IdStr%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_3_MIN"><%=mcessMin3Str%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_3_MAX"><%=mcessMax3Str%></wb:controlField>
						 </td>
					  </tr>
					  <tr>
					     <td>
						    <wb:controlField ui='DBLookupUI' submitName="MCESS_PROP_4" uiParameter='<%=storePropertyUiParams%>'><%=mcessProp4IdStr%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_4_MIN"><%=mcessMin4Str%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_4_MAX"><%=mcessMax4Str%></wb:controlField>
						 </td>
					  </tr>
					  <tr>
					     <td>
						    <wb:controlField ui='DBLookupUI' submitName="MCESS_PROP_5" uiParameter='<%=storePropertyUiParams%>'><%=mcessProp5IdStr%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_5_MIN"><%=mcessMin5Str%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_5_MAX"><%=mcessMax5Str%></wb:controlField>
						 </td>
					  </tr>
					  <tr>
					     <td>
						    <wb:controlField ui='DBLookupUI' submitName="MCESS_PROP_6" uiParameter='<%=storePropertyUiParams%>'><%=mcessProp6IdStr%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_6_MIN"><%=mcessMin6Str%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_6_MAX"><%=mcessMax6Str%></wb:controlField>
						 </td>
					  </tr>
					  <tr>
					     <td>
						    <wb:controlField ui='DBLookupUI' submitName="MCESS_PROP_7" uiParameter='<%=storePropertyUiParams%>'><%=mcessProp7IdStr%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_7_MIN"><%=mcessMin7Str%></wb:controlField>
						 </td>
						 <td>
						    <wb:controlField submitName="MCESS_PROP_7_MAX"><%=mcessMax7Str%></wb:controlField>
						 </td>
					  </tr>
				   </wba:table>  
				</td>
			 </tr>
		  </table>
	      </div>
      </td>
   </tr>
 </wba:table>

 <wb:set id="record_id"><%=recordId%></wb:set>
 <wb:submit id="record_id"><wb:get id="record_id"/></wb:submit>
 <input type='hidden' name='MODE'>
 <input type='hidden' name='PROPERTY_STRING' value = '<%=propValidationString%>'>

 <div class="separatorSmall"/>
 <wba:button label='Save Search' labelLocalizeIndex="save_search" onClick='checkSave();'/><span>&nbsp;</span>
 <wba:button label='Delete Search' labelLocalizeIndex="delete_search" onClick='checkDelete();'/><span>&nbsp;</span>
 <wba:button label="Cancel" labelLocalizeIndex="cancel_search" onClick="<%=goBack%>"/> </br>
 <div class="separatorSmall"/>
 <wba:button label='Search' labelLocalizeIndex="search" onClick='checkSearch();'/>
</wb:page>