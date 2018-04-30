package com.wbiag.app.modules.retailSchedule.errorDetection;

public class ErrorDetectionScriptResult {
	public String help_tip;
	public String help_title;
	public String help_title_id;
	public String help_desc;
	public String error_msg;
	public String message;
	public boolean fatal_error = false;
	
	public ErrorDetectionScriptResult() {
		help_tip = "<not set>";
		help_title = "<not set>";
		help_title_id = "not_set";
		help_desc = "<not set>";
		error_msg = "<not set>";
		message = "<not set>";
	}
	
	public ErrorDetectionScriptResult(String helpTitle,
			String helpTip, String helpDesc,
			String errorMsg, String resultMsg) {
		help_tip = helpTip;
		help_title = helpTitle;
		help_desc = helpDesc;
		error_msg = errorMsg;
		message = resultMsg;
	}
	
	public String getHelpTip() {
		return help_tip;
	}
	
	public String getHelpTitle() {
		return help_title;
	}
	
	public String getHelpTitleId() {
		return help_title_id;
	}
	
	public String getHelpDesc() {
		return help_desc;
	}
	
	public String getErrorMsg() {
		return error_msg;
	}
	
	public String getMessage() {
		return message;
	}
	
	public boolean getFatalError() {
		return fatal_error;
	}
	
	public String getFatalErrorAsString() {
		return (new Boolean(fatal_error)).toString();
	}
	
	public void setHelpTip(String val) {
		help_tip = val;
	}
	
	public void setHelpTitle(String val) {
		help_title = val;
	}
	
	public void setHelpTitleId(String val){
		help_title_id = val;
	}
	
	public void setHelpDesc(String val) {
		help_desc = val;
	}
	
	public void setErrorMsg(String val) {
		error_msg = val;
	}
	
	public void setMessage(String val) {
		message = val;
	}
	
	public void setFatalError(boolean val) {
		fatal_error = val;
	}
	
	public void setFatalErrorAsString(String val) {
		fatal_error = (new Boolean(val)).booleanValue();
	}
}