package com.wbiag.util.ftp;

import com.workbrain.app.clockInterface.lib.*;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.util.*;
/**
 * <p>Title: FTP File Transport New</p>
 * <p>Description: Moves files from a location to another using (if necessary) the FTP protocol</p>
 * <p>Copyright: Workbrain (c) 2004</p>
 * <p>Company: Workbrain Inc.</p>
 * @author
 * @version 1.0
 */
import java.util.*;
import java.io.*;
import com.enterprisedt.net.ftp.*;
import org.apache.log4j.*;

public class FtpFileTransportJobExt extends AbstractScheduledJob{
    private static Logger packageLogger = Logger.getLogger("com.workbrain.app.clockInterface");

    public static final String LOG_FOLDER = "logFolder";
    public static final String LOG_LEVEL = "logLevel";

    public static final String EXPORT_IMPORT = "exportImport";
    public static final String TRANSFER_TYPE = "transferType";
    public static final String IP_DOMAIN = "ipDomain";
    public static final String PORT = "port";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String DESTINATION_FULLNAME = "destinationFullName";
    public static final String TARGET_FULLNAME = "targetFullName";
    public static final String PURGE_SOURCE = "purgeSource";
    public static final int ELEMENTS_IN_ONE_ENTRY = 9;

    private String TARGETFILE = "TARGET";
    private String DESTINATIONFILE = "DESTINATION";
    private String logFolder;
    private Map taskParams;

    private String errorString = "";
    private DailyRollingFileAppender dailyRollFileAppender;
    private Level oldLogLevel;

    public FtpFileTransportJobExt() {
    }

    /**
     * This is the method that will get executed by the scheduler.
     */
    public Status run(int taskID, Map param) throws Exception {
        try {
            getLogger().info("Started the FTP task at: " + new Date().toString());
            taskParams = param;
            oldLogLevel = packageLogger.getLevel();
            logFolder = FileHandler.addFinalSlash( (String) taskParams.get(LOG_FOLDER));
            setNewLogOptions();
            getLogger().info("right before exectute");
            execute();
            getLogger().info("after the exectute");
            setOldLogOptions();
        } catch (Exception e) {
            e.printStackTrace();
            errorString = e.getMessage();
            getLogger().error("Error scheduling the FTP task", e);
            return jobFailed(e.getMessage());
        }
        getLogger().info("Finished the FTP task at: " + new Date().toString());
        if (isInterrupted())
            return jobInterrupted("FTP task has been interrupted.");

        if ( errorString.equalsIgnoreCase("") ) {
            return jobOk( "FTP task finished successfully." );
        } else {
            return jobOk(errorString);
        }
    }

    private void setNewLogOptions(){
        if (!logFolder.equalsIgnoreCase("")) {
            dailyRollFileAppender = new DailyRollingFileAppender();
            dailyRollFileAppender.setLayout(new PatternLayout(
                "%d [%t] %-5p - %m%n"));
            dailyRollFileAppender.setDatePattern("'.'yyyy-MM-dd");
            dailyRollFileAppender.setAppend(true);
            dailyRollFileAppender.setName("(Timed) Roll File Appender");
            dailyRollFileAppender.setFile(logFolder + "Ftp.log");
            dailyRollFileAppender.activateOptions();
            packageLogger.addAppender(dailyRollFileAppender);
            packageLogger.setLevel(Level.toLevel((String) taskParams.get(LOG_LEVEL)));
        }
    }

    private void setOldLogOptions(){
        if (!logFolder.equalsIgnoreCase("")) {
            packageLogger.removeAppender(dailyRollFileAppender);
            packageLogger.setLevel(oldLogLevel);
        }
    }

    private String stripFileName(String fileName)
    {
    	return StringHelper.trimBraces(fileName, '[', ']');

    }

    /**
     *  This is the jsp page that let's you set up the parameters for this task.
     */
    public String getTaskUI() {
        return "/jobs/ent/jobs/ftpFileTransport.jsp";
    }

    private void execute() throws Exception {
        for (int i = 0; i < (taskParams.size() / ELEMENTS_IN_ONE_ENTRY); i++){
            try {
                if (isInterrupted())
                {
                    break;
                }
                OneEntry aVectorEntry = new OneEntry(taskParams, i);
                packageLogger.info(aVectorEntry.toString());

                // if the local file is directory then the remote file must also be a directory
                // also if the local file is not a directory then the remote file cannot be a directory either
                if (aVectorEntry.isLocalFileDirectory() ^ aVectorEntry.isRemoteFileDirectory())
                {
                	errorString+= "ERROR : The local source, and the remote target must both be directories, or niether of them should be directories \n";
                	throw new FileNotFoundException();
                }
                if (aVectorEntry.isTypeImport()) {
                	packageLogger.info("Before initlializer");
                    FTPClient ftp = initializeFtpClient(aVectorEntry, TARGETFILE);
                    packageLogger.info("after initializer");
                    List fileNamesToBeDownloaded = getRemoteFileNames(ftp);
                    Iterator iter = fileNamesToBeDownloaded.iterator();
                    while (iter.hasNext()) {
                        String fileNameToBeDownloaded = (String) iter.next();
                        packageLogger.info("file that we are dealing with " + fileNameToBeDownloaded );
                        try {
                            String localFolder = aVectorEntry.getLocalFolder();
                            if (!StringHelper.isEmpty(localFolder)
                                && !localFolder.endsWith("/")) {
                                localFolder = localFolder + "/";
                                packageLogger.info("this is the local folder " + localFolder);
                            }
                            packageLogger.info("this is the local folder " + localFolder);

                            // if the remote file is a directory that the contents will be moved to another directory
                            // then the source and destination files will have the same name
                            if (aVectorEntry.isRemoteFileDirectory())
                            {
                            	ftp.get(localFolder + fileNameToBeDownloaded,
                                        stripFileName(fileNameToBeDownloaded));
                            }
                            else
                            {
                            	ftp.get(localFolder + stripFileName(aVectorEntry.getLocalFile().toString()),
                                    stripFileName(fileNameToBeDownloaded));
                            }

                            if (aVectorEntry.isPurgeSource()) {
                                try {
                                    packageLogger.info(
                                        "Purging the source file "
                                        + fileNameToBeDownloaded + "...");
                                    ftp.delete(fileNameToBeDownloaded);
                                    packageLogger.info(
                                        "Purged the source file "
                                        + fileNameToBeDownloaded + ".");
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                    errorString += "\n\nERROR: \n\n" +
                                        e.getMessage();
                                    packageLogger.error("The file " +
                                        fileNameToBeDownloaded +
                                        " could not be purged. ", e);
                                }
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            errorString += "\n\nERROR: \n\n" + e.getMessage();
                            packageLogger.error("The file " +
                                                fileNameToBeDownloaded +
                                " could not be transfered. Skipping to the next file...",
                                e);
                        }
                    }
                    packageLogger.info("Closing the connection...");
                    ftp.quit();
                } else { //export
                	packageLogger.info("before initilizer");
                    FTPClient ftp = initializeFtpClient(aVectorEntry, DESTINATIONFILE);
                    packageLogger.info("after initilizer");
                    List fileNamesToBeUploaded = aVectorEntry.getLocalFile();
                    Iterator iter = fileNamesToBeUploaded.iterator();
                    try
                    {
                        String localFolder = aVectorEntry.getLocalFolder();
                        if (!StringHelper.isEmpty(localFolder)
                            && !localFolder.endsWith("/")) {
                            localFolder = localFolder + "/";
                        }
                        packageLogger.info("This is the Local Folder : " + localFolder);
                        String remoteFolder = aVectorEntry.getRemoteFolder();
                        if (!StringHelper.isEmpty(remoteFolder)
                            && !remoteFolder.endsWith("/")) {
                            remoteFolder = remoteFolder + "/";
                        }
                        packageLogger.info("This is the remote Folder: " + remoteFolder );

                        while (iter.hasNext())
                        {
                        	String fileNameToBeUploaded = (String) iter.next();


                            packageLogger.info("This is the local filename " + aVectorEntry.getLocalFile().toString());
                            packageLogger.info("This is the filename to be uploaded" + fileNameToBeUploaded);

                            // if the local file is a directory that the contents will be moved to another directory
                            // then the source and destination files will have the same name
                            if (aVectorEntry.isLocalFileDirectory())
                            {
                            	ftp.put(localFolder +  fileNameToBeUploaded,
                                       remoteFolder + fileNameToBeUploaded);
                        	 }
                            // if a single file is being uploaded then use the name of the source and the target
                            else
                            {
                            	ftp.put(localFolder +  stripFileName(aVectorEntry.getLocalFile().toString()),
                        			   remoteFolder + fileNameToBeUploaded);
                            }
                            // let go of the file

                        }

                        ftp.quit();

                        if (aVectorEntry.isPurgeSource())
                        {
	                        try
	                        {
		                        List fileNamesToBePurged = aVectorEntry.getLocalFile();
		                        Iterator pIter = fileNamesToBePurged.iterator();
		                        while (pIter.hasNext())
		                        {
		                            String fileNameToBePurged = (String) pIter.next();
		                            packageLogger.info(
		                                "Purging the source file " +
		                            		localFolder  + fileNameToBePurged + "...");
		                            File localFile = new File(localFolder +
		                            		fileNameToBePurged);

		                            if (!localFile.delete())
		                            {
		                                packageLogger.error("The file " +
		                                    aVectorEntry.getLocalFile()
		                                    + " could not be purged. ");
		                            }
		                            else
		                            {
		                                packageLogger.info(
		                                    "Purged the source file "
		                                    + aVectorEntry.getLocalFile() + ".");
		                            }
		                        }

	                        }
	                        catch (Exception e)
	                        {
	                        	e.printStackTrace();
	                        	errorString += "\n\nERROR: \n\n" +
	                            e.getMessage();
	                        	packageLogger.error("The file " +
	                        			aVectorEntry.getLocalFile() +
	                        			" could not be purged. ", e);
	                        }
                        }
                    }
                      catch (Exception e)
                      {
                          e.printStackTrace();
                          errorString += "\n\nERROR: \n\n" + e.getMessage();
                          packageLogger.error("The file " +
                                              aVectorEntry.getLocalFile() +
                              " could not be transfered. Skipping to the next file...",
                              e);
                      }
                }

              packageLogger.info("Closing the connection...");
            } catch (Exception e)
            {
                e.printStackTrace();
                errorString += "\n\nERROR: \n\n" + e.getMessage();
                packageLogger.error("Error while FTP-ing. Skipping entry", e);
            }
        }
    }
        private List getRemoteFileNames(FTPClient pFTP) throws IOException, FTPException
        {
        	List retList = new ArrayList();
            String[] fileNamesList = pFTP.dir();

            for (int i=0;i<fileNamesList.length; i++)
            {
            	String name = fileNamesList[i];

            	// if the name contains a '.' it will be assumed that it is a file
            	if (name.indexOf(".") >= 0)
            	{
            		retList.add(name);
            		packageLogger.info("adding the file " + name);
            	}
            }

            return retList;
        }


    private FTPClient initializeFtpClient(OneEntry anEntry, String fileType) throws IOException, FTPException
    {
        FTPClient anFtpClient = null;
        packageLogger.info("Creating the FTP Client...");
        anFtpClient = new FTPClient(anEntry.getHost(),anEntry.getPort());
        packageLogger.info("Connecting the FTP Client to the " + fileType + " directory...");
        anFtpClient.login(anEntry.getUsername(),anEntry.getPassword());
        packageLogger.info("Setting transfer type to " + anEntry.transferType + "...");
        anFtpClient.setType(anEntry.isTransferAscii() ? FTPTransferType.ASCII : FTPTransferType.BINARY);
        packageLogger.info("Setting connection mode to ACTIVE...");
        anFtpClient.setConnectMode(FTPConnectMode.ACTIVE);
        if (!StringHelper.isEmpty(anEntry.getRemoteFolder())) {
            packageLogger.info("Changing to the remote folder: " + anEntry.getRemoteFolder());
            anFtpClient.chdir(anEntry.getRemoteFolder());
        }
        return anFtpClient;
    }

    private class OneEntry
    {
        private String exportImport;
        private String transferType;
        private String ipDomain;
        private String port;
        private String username;
        private String password;
        private String targetFullName;
        private String destinationFullName;
        private String purgeSource;

        OneEntry(Map params, int i)
        {
            exportImport = params.get(EXPORT_IMPORT + (i + 1)) != null ?
                (String)params.get(EXPORT_IMPORT + (i + 1))  : "";
            transferType = params.get(TRANSFER_TYPE + (i + 1)) != null ?
                (String)params.get(TRANSFER_TYPE + (i + 1))  : "";
            ipDomain = params.get(IP_DOMAIN + (i + 1)) != null ?
                (String)params.get(IP_DOMAIN + (i + 1))  : "";
            port = params.get(PORT + (i + 1)) != null ?
                (String)params.get(PORT + (i + 1))  : "";
            username = params.get(USERNAME + (i + 1)) != null ?
                (String)params.get(USERNAME + (i + 1))  : "";
            password = params.get(PASSWORD + (i + 1)) != null ?
                (String)params.get(PASSWORD + (i + 1))  : "";
            targetFullName = params.get(TARGET_FULLNAME + (i + 1)) != null ?
                (String)params.get(TARGET_FULLNAME + (i + 1))  : "";
            destinationFullName = params.get(DESTINATION_FULLNAME + (i + 1)) != null ?
                (String)params.get(DESTINATION_FULLNAME + (i + 1))  : "";
            purgeSource = params.get(PURGE_SOURCE + (i + 1)) != null ?
                (String)params.get(PURGE_SOURCE + (i + 1))  : "";
        }

        public String getUsername()
        {
            return username;
        }


        public String getPassword()
        {
            return password;
        }

        public String getHost()
        {
            return ipDomain;
        }

        public int getPort()
        {
            try
            {
                if (!StringHelper.isEmpty(port)){
                    return Integer.parseInt(port);
                } else {
                    return 21;
                }
            } catch (Exception e)
            {
                e.printStackTrace();
                packageLogger.error("Port number is not an integer. Defaulting to 21", e);
                return 21;
            }
        }

        public String getLogFolder(){
            return logFolder;
        }

        public boolean isLocalFileDirectory()
        {
        	String fileName = targetFullName;
        	File file = new File(fileName);

        	return file.isDirectory();
        }

        public boolean isRemoteFileDirectory()
        {
        	String fileName = destinationFullName;
        	File file = new File(fileName);

        	return file.isDirectory();
        }

        public String getRemoteFolder(){
            String fileName = targetFullName;
            if (isTypeExport()) {
                fileName = destinationFullName;
            }
            File file = new File(fileName);

            if (!file.isDirectory())
            {
            	packageLogger.info("getRemoteFolder returning " + ensureForwardSlashes(file.getParent()));
            	return file.getParent() == null ? "" : ensureForwardSlashes(file.getParent());
            }
            else
            {
            	packageLogger.info("getRemoteFolder is directory returning " + ensureForwardSlashes(file.getPath()));
            	return ensureForwardSlashes(file.getPath());

            }
        }

        public String getLocalFolder()
        {
            String fileName = destinationFullName;

            if (isTypeExport())
            {
                fileName = targetFullName;
            }
            File file = new File(fileName);

            if (!file.isDirectory())
            {
            	packageLogger.info("getLocalFolder returning " + ensureForwardSlashes(file.getParent()));
            	return file.getParent() == null ? "" : ensureForwardSlashes(file.getParent());
            }
            else
            {
            	packageLogger.info("getLocalFolder is directory returning " + ensureForwardSlashes(file.getPath()));
            	return ensureForwardSlashes(file.getPath());
            }
        }

        public List getRemoteFile() throws IOException
        {
            List ret = new ArrayList();
            String fileName = targetFullName;

            if (isTypeExport())
            {
                fileName = destinationFullName;
            }
            File file = new File(fileName);
            packageLogger.info("INSIDE GET REMOTE FILE - remote filename : " + file);

            if (file.isDirectory())
            {
            	packageLogger.info("REMOTE file is a directory, so we are going to add files under it");
                File[] files = FileUtil.getFilesUnderDir(fileName);

                for( int i = 0; i < files.length; i++ )
                {
                	if (files[i].isFile())
                	{
                		packageLogger.info("in remote folder adding " + files[i].getName());
                		ret.add(files[i].getName() == null ? "" : files[i].getName()) ;
                	}
                }
            }
            else
            {
            	packageLogger.info("getremoteFolder is returning " + ensureForwardSlashes(file.getPath()));
                ret.add(file.getName() == null ? "" : file.getName()) ;
            }
            return ret;
        }

        public List getLocalFile() throws IOException
        {
            List ret = new ArrayList();
            String fileName = destinationFullName;

            if (isTypeExport())
            {
                fileName = targetFullName;
            }
            File file = new File(fileName);
            packageLogger.info("INSIDE GET LOCAL FILE - local filename : " + file);
            if (file.isDirectory())
            {
            	packageLogger.info("in getlocalfile, this file is a directory");
                File[] files = FileUtil.getFilesUnderDir(fileName);
                for( int i = 0; i < files.length; i++ )
                {

                	if (files[i].isFile())
                	{
                		packageLogger.info("file name is " + files[i].getName());
                		ret.add(files[i].getName() == null ? "" : files[i].getName()) ;
                	}
                }
            }
            else
            {
            	packageLogger.info("file name is " + file.getName());
                ret.add(file.getName() == null ? "" : file.getName()) ;

            }
            return ret;
        }

        public boolean isTypeExport()
        {
            return "EXPORT".equalsIgnoreCase(exportImport);
        }
        public boolean isTypeImport()
        {
            return !isTypeExport();

        }

        public boolean isTransferAscii()
        {
            return "ASCII".equalsIgnoreCase(transferType);
        }
        public boolean isTransferBinary()
        {
            return !isTransferAscii();
        }

        public boolean isPurgeSource()
        {
            return "TRUE".equalsIgnoreCase(purgeSource)
                        || "Y".equalsIgnoreCase(purgeSource);
        }

        private String ensureForwardSlashes(String name)
        {
            return StringHelper.searchReplace(name, "\\", "/");
        }

        public String toString()
        {
            return "RemoteFolder is: " + getRemoteFolder() + " Username is: " + username +
                    " Password is: " + password + " Port is: " + port +
                    " LocalFolder is: " +  getLocalFolder();
        }



    }
}
