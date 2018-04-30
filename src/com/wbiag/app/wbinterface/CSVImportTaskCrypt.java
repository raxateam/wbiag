package com.wbiag.app.wbinterface;

import java.io.*;
import java.sql.*;

import com.wbiag.util.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * A scheduled task that decrypts a file that was encrypted with workbrain.SYSTEM_KEY
 */
public class CSVImportTaskCrypt extends CSVImportTask {


    public TransactionData prepareOneFileForProcess(DBConnection conn , String typeName,
            String filePath, File file, boolean performsDifference) throws SQLException, IOException{
        String tempFilePath = filePath + ".DEC" ;
        try {
            if (logger.isDebugEnabled()) logger.debug("Decrypting file:" + filePath);
            CryptUtil.encryptDecryptFile(filePath, tempFilePath,
                                         CryptUtil.MODE_DECRYPT);
            FileUtil.copyFile(tempFilePath , filePath, false);
        }
        catch (Exception ex) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in decrypting file" , ex);}
            throw new NestedRuntimeException("Error in decrypting file", ex);
        }
        finally {
            if (FileUtil.fileExists(tempFilePath)) {
                FileUtil.deleteFile(tempFilePath);
            }
        }
        return super.prepareOneFileForProcess(conn, typeName, filePath,
                                              file, performsDifference);
    }

}
