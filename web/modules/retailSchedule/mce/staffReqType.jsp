<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.util.*"%> 
<%@ page import="java.sql.*"%> 
<%@ page import="com.workbrain.util.*" %> 
<%@ page import="com.workbrain.sql.*"%> 
<%@ page import="com.workbrain.server.jsp.*"%> 

<wb:page login="true" popupPage="false" subsidiaryPage="false" title="Staffing Requirement" submitMethod="post">
   <wb:define id="dept_search"><wb:get id="dept_search" scope="parameter" default='true'/></wb:define>
   <wb:submit id="dept_search"><wb:get id="dept_search"/></wb:submit>
   <wb:define id="selected_skdgrp_list"><wb:get id="selected_skdgrp_list" scope="parameter" default=''/></wb:define>
   <wb:submit id="selected_skdgrp_list"><wb:get id="selected_skdgrp_list"/></wb:submit>
   <wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" /></wb:define>
   <wb:submit id="mfrm_id"><wb:get id="mfrm_id"/></wb:submit>
   <wb:define id="searched_skdgrp_list"><wb:get id="searched_skdgrp_list" scope="parameter" default=''/></wb:define>
   <wb:submit id="searched_skdgrp_list"><wb:get id="searched_skdgrp_list"/></wb:submit>
   <wb:define id="MCESS_TYPE"><wb:get id="MCESS_TYPE" scope="parameter" default=''/></wb:define>
   <wb:submit id="MCESS_TYPE"><wb:get id="MCESS_TYPE"/></wb:submit>
   <wb:define id="actionType"><wb:get id="actionType" scope="parameter" default=''/></wb:define>
   <wb:submit id="actionType"><wb:get id="actionType"/></wb:submit>
   <wb:define id="client_type_id"><wb:get id="client_type_id" scope="parameter" default=''/></wb:define>
   <wb:submit id="client_type_id"><wb:get id="client_type_id"/></wb:submit>

   <wba:table class="contentTable form" caption="Select which type of Staffing requirement to add" captionLocalizeIndex="Select_which_type_of_Staffing_requirement_to_add"> 

      <tr>
	     <td>
		    <input type='radio' name='REQ_TYPE' value='volume-driven' checked >
			<wb:localize id='volume-driven'>Volume-driven</wb:localize>
		 </td>
	  </tr>
	  <tr>
	     <td>
		    <input type='radio' name='REQ_TYPE' value='Non-volume-driven'>
			<wb:localize id='Non-volume-driven'>Non-volume-driven</wb:localize>
		 </td>
	  </tr>
	  <tr>
	     <td>
		    <input type='radio' name='REQ_TYPE' value='Store property-driven'>
			<wb:localize id='Store property-driven'>Store property-driven</wb:localize>
		 </td>
	  </tr>
	  
   </wba:table>
   <wba:button name="btnContinue" label="Continue" labelLocalizeIndex="Continue" onClick="submitForm();"></wba:button>
   <INPUT TYPE="hidden" name="actionType"> 

<script type='text/javascript'>

   function submitForm() {
      document.forms[0].action="./staffReqEdit.jsp";
      document.forms[0].submit();
   }

</script>
</wb:page>