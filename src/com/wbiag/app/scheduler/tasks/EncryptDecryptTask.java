package com.wbiag.app.scheduler.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.util.PwdConfig;

/**
 * Title: EncryptDecryptTask.java <br>
 * Description: Encrypts/decrypts files on BASE64 algorithm based on a key stored in a file
 */
public class EncryptDecryptTask extends AbstractScheduledJob {
    private static final Logger logger = Logger.getLogger(EncryptDecryptTask.class);

    public static final String PARAM_KEY_FILE = "KEY_FILE";
    public static final String PARAM_OPERATION = "OPERATION";        
    public static final String PARAM_INPUT_FILE = "INPUT_FILE";        
    public static final String PARAM_OUTPUT_FILE = "OUTPUT_FILE";       
  
    /*
     * (non-Javadoc)
     *
     * @see com.workbrain.app.scheduler.enterprise.ScheduledJob#run(int,
     *      java.util.Map)
     */
    public Status run( int taskID, Map parameters ) throws Exception {
        try {
            execute(parameters);
        } catch (Exception e) {
            if (logger.isEnabledFor(Level.ERROR)) {  logger.error("EncryptDecryptTask error", e);            }
            throw e;
        }
        return jobOk(" Job Scheduled OK ");
    }

    public void execute(Map params) throws Exception {

        String keyFile = (String)params.get(PARAM_KEY_FILE);
        String operation = (String)params.get(PARAM_OPERATION);        
        String inputFile = (String)params.get(PARAM_INPUT_FILE);        
        String outputFile = (String)params.get(PARAM_OUTPUT_FILE);        
        byte[] key = readFileAsByte(keyFile);   

        byte[] input = readFileAsByte(inputFile);    
        byte[] data = null;
        if ("ENCRYPT".equals(operation)) {            
            data = PwdConfig.encrypt(key, input);
        }
        else if ("DECRYPT".equals(operation)) {
            data = PwdConfig.decrypt(key, input);            
        }
        else {
            throw new RuntimeException("Operation : " + operation + " not supported");
        }
        writeFile(outputFile , data);
        
    }
    
    private byte[] readFileAsByte(String file) throws IOException {
        File orgFile = new File (file);
        BufferedInputStream org =
            new BufferedInputStream(new FileInputStream(orgFile));

        byte[] data = new byte[(int)orgFile.length()];
        org.read(data);
        org.close();   
        
        return data;
    }
    
    private void writeFile(String file , byte[] data ) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        out.write(data);
        out.close();        
    }

    public String getTaskUI() {
        return null;
    }    
}

