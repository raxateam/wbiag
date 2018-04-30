package com.wbiag.clocks.clients.sols.lib;

/**
 * CRC interface, to build common crc checking classes.
 * @author rbujko
 *
 * @version %I% %G%
 */
public interface CRCBuilder {
    /**
     * This method returns the formatted command (appends the CRC and other pre/post needed bytes).
     * @param inputBytes array of byte
     * @return array of byte
     */
    public byte[] formatBytes(byte[] inputBytes);

    /**
     * Same as formatBytes(inputString.getBytes()).
     * @param inputString String
     * @return array of byte
     */
    public byte[] formatString(String inputString);

    /**
     * This method returns appends the CRC bytes to given byte array.
     * @param inputBytes array of byte
     * @return array of byte
     */
    public byte[] appendCRCBytes(byte[] inputBytes);

    /**
     * Same as appendCRCBytes(inputString.getBytes()).
     * @param inputString String
     * @return array of byte
     */
    public byte[] appendCRCBytes(String inputString);

    /**
     * This method returns the calculated CRC bytes.
     * @param inputBytes array of byte
     * @return array of byte
     */
    public byte[] getCRCBytes(byte[] inputBytes);

    /**
     * Same as getCRCBytes(inputString.getBytes()).
     * @param inputString a String
     * @return array of byte
     */
    public byte[] getCRCBytes(String inputString);
}