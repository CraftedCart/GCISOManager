package craftedcart.gcisomanager;


import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author CraftedCart
 *         Created on 23/10/2016 (DD/MM/YYYY)
 */
public class FileUtils {

    public static int readIntLittleEndian(RandomAccessFile raf) throws IOException {
        int ch1 = raf.read();
        int ch2 = raf.read();
        int ch3 = raf.read();
        int ch4 = raf.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24));
    }

}
