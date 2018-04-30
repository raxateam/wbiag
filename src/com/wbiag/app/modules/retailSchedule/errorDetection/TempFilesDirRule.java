package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.io.IOException;

import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.sftp.FileAttributes;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.services.MoselRegistryKeys;
import com.workbrain.server.registry.Registry;

public class TempFilesDirRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(TempFilesDirRule.class);

	public Integer getRuleType(){
		return SYSTEM_TYPE;
	}

	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {

		String moselClass = context.getMoselClass();

		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Temp Files Directory");
		actionResult.setHelpTip("Directory for temp files on application server not present / out of space");
		actionResult.setHelpDesc("A system check will be done to ensure that these directories exist and that the server is not out of hard drive space that is required to run the Schedule Optimizer. The script will check for this requirement and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("FAILED: ");

		String result = new String("");

		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.4.1...");

		ScheduleGroupData skdgrpData = null;
		try {
			skdgrpData = ScheduleGroupData.getScheduleGroupData( Integer.parseInt(context.getSkdgrpId()));
		}
		catch(RetailException e) {
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());

			result += e.getMessage() + "\n";
		}

		if(moselClass.equals("com.workbrain.app.modules.retailSchedule.services.SingleServerExecute")) {
			String modelName = getBaseDirectory(moselClass) + "/models/" + skdgrpData.getSkdgrpScriptName() +".bim";
	        String tempDir = getBaseDirectory(moselClass) + "/temp";

	        try {
				Process proc = Runtime.getRuntime().exec("mosel");
				proc.destroy() ;
			}
			catch(IOException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());

				result += "Mosel command does not exist on system PATH\n";
			}

    		java.io.File f = new java.io.File(modelName);
    		if(!f.exists()) {
    			result += "File " + modelName + " does not exist\n";
    		}
    		if(!f.canWrite()) {
    			if(!f.exists()) {
    				result += "File " + modelName + " does not have writable permissions\n";
    			}
    			else {
    				result += "File " + modelName + " does not have writable permissions\n";
    			}
    		}

	    	f = new java.io.File(tempDir);
	    	if(!f.exists()) {
	    		result += "Directory " + tempDir + " does not exist\n";
	    	}
		}

		else if(moselClass.equals("com.workbrain.app.modules.retailSchedule.services.RemoteServerExecute")) {
			try {
				String strDestHost = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_HOST");
				String strDestHomeFolder = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_HOME_FOLDER");
				String strDestUName = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_USER_NAME");
				String strDestPwd = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_PASS");

		        //String tempDir = PreProcessorUtils.getBaseDirectory(moselClass) + "/temp";
		        //String tempDir = getBaseDirectory(moselClass) + "/temp";
		        String modelName = strDestHomeFolder + "/models/" + skdgrpData.getSkdgrpScriptName() +".bim";

				SshClient ssh = new SshClient();
				SshConnectionProperties properties = new SshConnectionProperties();
				properties.setHost(strDestHost);

				//Connect to the host
				ssh.connect(properties, new IgnoreHostKeyVerification());

				//Create a password authentication instance
				PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();

				//set user name
				pwd.setUsername(strDestUName);

				//Get the users password from the gatework.xml file
				pwd.setPassword(strDestPwd);

				//Try the authentication
				int xresult = ssh.authenticate(pwd);

				//Evaluate the result
				if(xresult == AuthenticationProtocolState.COMPLETE) {
					//removed check of mosel executable
					SftpClient sftp = null;
					try {
						sftp = ssh.openSftpClient();

		    			try {
			    			sftp.cd(strDestHomeFolder + "/models/");
			    			FileAttributes fa = sftp.stat(skdgrpData.getSkdgrpScriptName() +".bim");
			    			String perm = fa.getPermissionsString();
							//"-rwxrwxrwx"
			    			if(perm.charAt(1)!='r') {
		    					result += "File " + modelName + " does not have writable permissions\n";
			    			}
			    		}
			    		catch(IOException e) {
			    			if(logger.isDebugEnabled())
			    				logger.debug(e.getStackTrace());

			    			result += "Error " + e.getMessage() + " when checking for file " + modelName +"\n";
			    		}

			    		try {
			    			sftp.cd(strDestHomeFolder + "/temp/");
			    		}
			    		catch(IOException e) {
			    			if(logger.isDebugEnabled())
			    				logger.debug(e.getStackTrace());

							result += e.getMessage() + "\n";
						}
					} finally {
						if (sftp != null) {
							try {
								sftp.quit();
							} catch (Exception e){
								result += "ERROR: " + e.getMessage();
							}
						}
					}
					ssh.disconnect();
				}
				else {	//AUTHENTICATION FAILED
					result += "Authentication Failed\n";
				}
			}
			catch(NamingException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());

				result += e.getMessage() + "\n";
			}
			catch(IOException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());

				result += e.getMessage() + "\n";
			}
		}

		if("".compareTo(result) == 0) {
			result = "OK";
		}

		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.4.1");

		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Temp Files Directory";
	}

	 private String getBaseDirectory(String classname)
	    {
	        String strRet = null;
	        try
	        {
	            if (classname.indexOf("Remote")==-1)
	                strRet = (String)Registry.getVar(MoselRegistryKeys.REGKEY_SO_BASE_DIR);
	            else
	                strRet = (String)Registry.getVar(MoselRegistryKeys.REGKEY_REMOTE_HOME_FOLDER);
	        }
	        catch (NamingException e)
	        {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());

	            strRet = e.getMessage();
	        }

	        if (strRet == null || strRet.length() == 0)
	        {
	            strRet = "/scheduleOptimization";
	        }

	        return strRet;
	    }

}
