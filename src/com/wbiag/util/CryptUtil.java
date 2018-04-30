package com.wbiag.util;

import java.io.*;

import org.apache.log4j.*;
import com.workbrain.server.jsp.*;
import com.workbrain.util.*;

/**
 * Utility class for en/decrypting files based on Workbrain system_key
 */
public class CryptUtil{
    private static Logger logger = org.apache.log4j.Logger.getLogger(CryptUtil.class);
    public static final String MODE_ENCRYPT = "ENCRYPT";
    public static final String MODE_DECRYPT = "DECRYPT";

    private CryptUtil(){
    }

    /**
     * Encrypt/decrypts file based on workbrain parameter SYSTEM_KEY
     * @param srcFileName  full file path
     * @param destFileName full file path
     * @param mode         ENCRYPT or DECRYPT
     * @throws Exception
     */
    public static void encryptDecryptFile(String srcFileName,
                                          String destFileName,
                                          String mode) throws Exception{
        if (!MODE_ENCRYPT.equals(mode) &&
            !MODE_ENCRYPT.equals(mode)){
            throw new RuntimeException ("MODE_ENCRYPT can be ENCRYPT or DECRYPT");
        }
        if (!FileUtil.fileExists(srcFileName)) {
             throw new RuntimeException ("Source file not found :" + srcFileName);
        }
        OutputStream outputWriter = null;
        InputStream inputReader = null;
        try {
            String textLine = null;
            byte[] buf = MODE_ENCRYPT.equals(mode) ? new byte[100] : new byte[128];
            int bufl;

            // start FileIO
            outputWriter = new FileOutputStream(destFileName);
            inputReader = new FileInputStream(srcFileName);
            while ( (bufl = inputReader.read(buf)) != -1){
                byte[] encText = null;
                if (MODE_ENCRYPT.equals(mode)){
                      encText = SecurityHelper.encrypt(
                        copyBytes(buf,bufl),
                        com.workbrain.server.WebSystem.getKey() );
                }
                else {
                    if (logger.isDebugEnabled())     logger.debug("buf = " + new String(buf));
                    encText = SecurityHelper.decrypt(
                        copyBytes(buf,bufl),
                        com.workbrain.server.WebSystem.getKey() );
                }
                outputWriter.write(encText);
                if (logger.isDebugEnabled())   logger.debug("encText = " + new String(encText));
            }
            outputWriter.flush();

        }
        catch (Exception e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR))  logger.error("Error in encrypt/decrypy file" , e);
            throw e;
        }
        finally{
            if (outputWriter != null) {
                outputWriter.close();
            }
            if (inputReader != null) {
                inputReader.close();
            }
        }
    }

    /**
     * Copies byte array
     * @param arr
     * @param length
     * @return
     */
    public static byte[] copyBytes(byte[] arr, int length)
    {
        byte[] newArr = null;
        if (arr.length == length)
        {
            newArr = arr;
        }
        else
        {
            newArr = new byte[length];
            for (int i = 0; i < length; i++)
            {
                newArr[i] = (byte) arr[i];
            }
        }
        return newArr;
    }

}
