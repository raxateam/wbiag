
package com.wbiag.app.clockInterface.testHarness;

/**
 * Helper for the Synel CRC related tasks. 
 * In order to make the CRC work, the following Delphi function has been translated to Java:
 *<BR><BR>
 *<HR>
 *<BR>   function CRC86(s: string): string;
 *<BR>    //this algoorithm was supplied by synel specifically
 *<BR>    //for their readers
 *<BR>    //i have no idea how it works only that it works
 *<BR>   var
 *<BR>      ax: word;
 *<BR>      bx: word;
 *<BR>      cx: word;
 *<BR>      dx: word;
 *<BR>      bp: word;
 *<BR>      c1, c2, c3, c4: char;
 *<BR>      b1, b2, b3, b4: word;
 *<BR>      c1, c2, c3, c4: char;
 *<BR>      b1, b2, b3, b4: word;
 *<BR>   begin
 *<BR>      ax := 0;
 *<BR>      for bp := 1 to Length(s) do begin
 *<BR>         cx := Ord(s[bp]);
 *<BR>         dx := ax;
 *<BR>         ax := ax shr 8;
 *<BR>         ax := ax xor cx;
 *<BR>         bx := ax;
 *<BR>         ax := ax shr 4;
 *<BR>         ax := ax xor bx;
 *<BR>         bx := ax;
 *<BR>         ax := ax shl 5;
 *<BR>         ax := ax xor bx;
 *<BR>         dx := dx shl 8;
 *<BR>         ax := ax xor dx;
 *<BR>         bx := ax;
 *<BR>         ax := ax shl 12;
 *<BR>         ax := ax xor bx;
 *<BR>      end;
 *<BR>   
 *<BR>      b1 := ax shr 12;
 *<BR>      b1 := b1 and 15;
 *<BR>      Inc(b1, 48);
 *<BR>      c1 := Char(b1);
 *<BR>   
 *<BR>      b2 := ax shr 8;
 *<BR>      b2 := b2 and 15;
 *<BR>      Inc(b2, 48);
 *<BR>      c2 := Char(b2);
 *<BR>   
 *<BR>      b3 := ax shr 4;
 *<BR>      b3 := b3 and 15;
 *<BR>      Inc(b3, 48);
 *<BR>      c3 := Char(b3);
 *<BR>   
 *<BR>      b4 := ax shr 0;
 *<BR>      b4 := b4 and 15;
 *<BR>      Inc(b4, 48);
 *<BR>      c4 := Char(b4);
 *<BR>   
 *<BR>      result := c1 + c2 + c3 + c4;
 *<BR>   end;
 *<BR>
 *<HR>
 *
 * @author Octavian Tarcea
 */
public class SynelCRC {
    private final int MSB_MASK = 0xffff;
    private final byte EOT = 4;

    /**
     * Use this function when you want to check that the last 4 digits (before the very last one which is EOT)
     * are the proper CRC digits. If the check succeeds, the method returns otherwise it thwrows a BadCRCException
     * @param input String in the following format "xxxxxxxxxxx" + "CCCC" + "EOT" where CCCC are the CRC bytes that will be verified
     * @throws BadCRCException When the calculated CRC is not the same as the CRC suplied.
     */
    public void checkCRC(String input) throws Exception{
        String iCrc = input.substring(input.length() - 5, input.length() -1);
        byte[] inputCRC = iCrc.getBytes();
        SynelCRC crcHelper = new SynelCRC();
        byte[] calculatedCRC = crcHelper.getCRCBytes(input.substring(0, input.length() - 5));
        boolean result = true; //assume the crc's match
        StringBuffer cCrc = new StringBuffer();
        for (int i = 0; i < inputCRC.length; i++){
            cCrc.append((char) calculatedCRC[i]);
            if (inputCRC[i] != calculatedCRC[i]) {
                result = false;
            }
        }
        if (!result) {
            throw new Exception("Input CRC: \"" + iCrc + "\" Calculated CRC: \"" + cCrc.toString() + "\"");
        }
    }

    public byte[] formatBytes(byte[] inputBytes){
        int inputLength = inputBytes.length;
        byte[] returnBytes = new byte[inputLength + 5];
        byte[] inputPlusCrcBytes = appendCRCBytes(inputBytes);

        for (int i = 0; i < inputPlusCrcBytes.length; i++){
            returnBytes[i] = inputPlusCrcBytes[i];
        }
        returnBytes[inputLength + 4] = EOT;
        return returnBytes;
    }

    public byte[] formatString(String inputString){
        return formatBytes(inputString.getBytes());
    }

    public byte[] appendCRCBytes(byte[] inputBytes){
        int inputLength = inputBytes.length;
        byte[] returnBytes = new byte[inputLength + 4];
        byte[] crcBytes = getCRCBytes(inputBytes);

        for (int i = 0; i < inputLength; i++){
            returnBytes[i] = inputBytes[i];
        }

        returnBytes[inputLength] = crcBytes[0];
        returnBytes[inputLength + 1] = crcBytes[1];
        returnBytes[inputLength + 2] = crcBytes[2];
        returnBytes[inputLength + 3] = crcBytes[3];

        return returnBytes;
    }

    public byte[] appendCRCBytes(String inputString){
        return appendCRCBytes(inputString.getBytes());    
    }
    
    public byte[] getCRCBytes(String inputString){
        return getCRCBytes(inputString.getBytes());
    }

    //see the Delphi function below (This is its translation)
    public byte[] getCRCBytes(byte[] inputBytes){
        int ax, bx, cx, dx;
        int b1, b2, b3, b4;

        int inputLength = inputBytes.length;
        ax = 0;
        for (int i = 0; i < inputLength; i++) {
            cx = inputBytes[i];
            dx = ax;
            ax = (ax >>> 8) & MSB_MASK;
            ax = ax ^ cx;
            bx = ax;
            ax = (ax >>> 4) & MSB_MASK;
            ax = ax ^ bx;
            bx = ax;
            ax = (ax << 5) & MSB_MASK;
            ax = ax ^ bx;
            dx = (dx << 8) & MSB_MASK;
            ax = ax ^ dx;
            bx = ax;
            ax = (ax << 12) & MSB_MASK;
            ax = ax ^ bx;
        }

        b1 = (ax >>> 12) & MSB_MASK;
        b1 = b1 & 15;
        b1 += 48;

        b2 = (ax >>> 8) & MSB_MASK;
        b2 = b2 & 15;
        b2 += 48;

        b3 = (ax >>> 4) & MSB_MASK;
        b3 = (b3 & 15);
        b3 += 48;

        b4 = (ax >>> 0) & MSB_MASK;
        b4 = b4 & 15;
        b4 += 48;

        byte[] returnBytes = new byte[4];
        returnBytes[0] = (byte)b1;
        returnBytes[1] = (byte)b2;
        returnBytes[2] = (byte)b3;
        returnBytes[3] = (byte)b4;
        return returnBytes;
    }

}
