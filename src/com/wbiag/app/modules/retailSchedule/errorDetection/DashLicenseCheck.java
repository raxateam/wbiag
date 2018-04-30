package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.connection.ChannelState;
import com.sshtools.j2ssh.transport.TransportProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.connection.ChannelOutputStream;
import com.sshtools.j2ssh.io.IOStreamConnector;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;
import com.workbrain.server.registry.Registry;

public class DashLicenseCheck extends ErrorDetectionRule {


	private static Logger logger = Logger.getLogger(DashLicenseCheck.class);
	  
	public Integer getRuleType(){
		return SYSTEM_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		String moselClass = context.getMoselClass();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Dash License Check");
		actionResult.setHelpTip("Checks to see if the Dash server has a valid license");
		actionResult.setHelpDesc("Each Dash server must be licensed. This will check to see if the Dash server is configured with a valid Dash license");
		actionResult.setErrorMsg("FAILED:\n");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.4.4...");
		
		if(moselClass.equals("com.workbrain.app.modules.retailSchedule.services.SingleServerExecute")) {
			BufferedWriter out = null;
			BufferedReader procReader = null;
			
			try {
				Process proc = Runtime.getRuntime().exec("mosel");
				procReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				out = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
				out.write("exit");
				
				proc.waitFor();
				int status = proc.exitValue();

				if(status == 3)
					result += "-License Error: " + procReader.readLine() + "\n";
			}
			catch(InterruptedException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());
				
				result += "-License check failed: Wait interrupted!";
			}
			catch(IOException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());
				
				result += "-License check failed: Mosel command does not exist on system PATH\n";
			} 
		}
		else if(moselClass.equals("com.workbrain.app.modules.retailSchedule.services.RemoteServerExecute")) {
			try {
				String strDestHost = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_HOST");
				String strDestHomeFolder = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_HOME_FOLDER");
				String strDestUName = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_USER_NAME");
				String strDestPwd = (String) Registry.getVar("system/modules/scheduleOptimization/ssh/DESTINATION_PASS");
	
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
					IOStreamConnector output = new IOStreamConnector();
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					SessionChannelClient ssc = null;
					String outputText;
					int loc = 0;
					
					try {
						ssc = ssh.openSessionChannel();
						
						if (!ssc.requestPseudoTerminal("vt100", 80, 24, 0, 0, "")) {
							result += "Failed to allocate a pseudo terminal";                
			            }
						if( ssc.startShell() == false ) {
							result +=  "Failed to start Shell"; 
						}
						
						output.connect(ssc.getInputStream(), bos);
						waitForMsg(ssc, bos, "$", loc);
						loc = bos.toString().length();
						
						ssc.getOutputStream().write(("cd " + strDestHomeFolder + "\n").getBytes());
						waitForMsg(ssc, bos, "$", loc);
						loc = bos.toString().length();
						
						String cmd = "dir C: \n";
						ssc.getOutputStream().write(cmd.getBytes());
		                waitForMsg(ssc, bos, "$", loc);
		                outputText = bos.toString();
		                
		                if (!exceptionDetected(outputText.substring(loc))){
		                	//windows
		                	result += "WARNING: DASH setup not verifiable with this rule, manually check License \n";
		                } else {
		                	
		                	loc = outputText.length();	                
		                	cmd = "cd " + strDestHomeFolder + "/wbscheduleOptimization \n";
		                	ssc.getOutputStream().write(cmd.getBytes());
		                	waitForMsg(ssc, bos, "$", loc);
		                	outputText = bos.toString();

			                if (exceptionDetected(outputText.substring(loc))){
			                	result += "-Error changing to \"wbscheduleOptimization\" directory. Improper setup on DASH server?\n";
			                } else {
			                	
			                	cmd = "cat ../models/invokeMoselViaJava.sh | grep \"XPRESSDIR=\"; echo DONE \n";
			                	loc = outputText.length() + cmd.length();
			                	ssc.getOutputStream().write(cmd.getBytes());
			                	if (waitForMsg(ssc, bos, "DONE", loc)){
			                		outputText = bos.toString();
			                		
			                		int start =  outputText.substring(loc).indexOf("XPRESSDIR=") + loc + "XPRESSDIR=".length();
				                	int end =outputText.substring(start).indexOf("\n") + start -1;
				                	String xpressdir= outputText.substring(start, end) ;
				                	loc = outputText.length();
				                	
					                cmd = "java -classpath wbscheduleoptimization.jar:" 
					                	+ xpressdir + "/lib/xprb.jar:" 
					                	+ xpressdir 
					                	+ "/lib/xprs.jar com.workbrain.app.modules.retailSchedule.services.MoselLicense  \n";
					                ssc.getOutputStream().write(cmd.getBytes());
					                
					                waitForMsg(ssc, bos, "$", loc);
					                outputText = bos.toString();
					                if (exceptionDetected(outputText.substring(loc))){
					                	result += "-DASH License error. Check your license setup: " + outputText.substring(loc);
					                }
			                		
			                	} else {
			                		result += "WARNING: Could not determine XPRESSDIR folder, manually check License. \n";
			                	}
			                	
			                	
			                	
			                }
		                }
		               
					} catch (Exception e) {
						if(logger.isDebugEnabled())
							logger.debug(e.getStackTrace());
						
						result += "-License check failed: " + e.getMessage() + "\n";
					} finally  {
		                try {
		                	if(ssc != null)
		                		ssc.close();
		                } catch (Exception e){
		                	result +=e.getMessage(); 
		                }
						output.setCloseOutput(true);                
			            output.setCloseInput(true);
			            output.close();
			            bos.close();
					}
					
					ssh.disconnect();
					
				}
				else {	//AUTHENTICATION FAILED
					result += "-Error checking license: Authentication Failed. Please check your configuration in the Workbrain registry\n";
				}
			}
			catch(NamingException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());
				
				result += "-License check failed: " + e.getMessage() + "\n";
			}
			catch(IOException e) {
				if(logger.isDebugEnabled())
					logger.debug(e.getStackTrace());
				
				result += "-License check failed: " + e.getMessage() + "\n";
			}
		}
		else
			result += "-Error: Mosel execution type not set in the Workbrain registry! \n";
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.4.4");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		
		return "Dash License Check";
	}

    protected boolean exceptionDetected(String msg) {
        if( msg.toLowerCase().indexOf("command not found") >= 0 ||                            
            msg.toLowerCase().indexOf("exception") >= 0 ||
            msg.toLowerCase().indexOf("jarfile") >= 0 ||
            msg.toLowerCase().indexOf("not found") >= 0 ||                       
            msg.toLowerCase().indexOf("not recognized") >= 0 ||
            msg.toLowerCase().indexOf("unrecognized") >= 0 ||
            msg.toLowerCase().indexOf("no such file or directory") >= 0 ||
            msg.toLowerCase().indexOf("invalid") >= 0)
            return true;
        return false;
    }
    
    protected boolean waitForMsg(SessionChannelClient ssh, ByteArrayOutputStream bos, String msg, int loc) throws Exception {
    	while ( !ssh.getState().waitForState(ChannelState.CHANNEL_CLOSED, 100) ) {
            try {
	    		if(bos.toString().substring(loc).indexOf(msg) >= 0)
	                return true;
       
	            if( exceptionDetected(bos.toString().substring(loc)) )
	            	return false;
            } catch (Exception e){
            	
            }
        }
    	return false;
    	
    }
}
