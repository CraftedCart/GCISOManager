package craftedcart.gcisomanager;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author CraftedCart
 *         Created on 23/11/2016 (DD/MM/YYYY)
 */
public class FileEntry {

    public int index;

    public boolean isDir;
    public long filenameOffset;
    public long offset;
    public long length;

    public char data; //TODO char? or byte array?
    public String filename;

    /**
     * Converts the rawEntry into a FileEntry<br>
     * Doesn't fetch the filename or data to speed things up<br>
     * Use fetchFilenameForFileEntry to get the filename<br>
     * <br>
     * <b>Format of a raw file entry:</b><br>
     * Offset 0x00 | Size 1 | Flags: 0 = File / 1 = Directory<br>
     * Offset 0x01 | Size 3 | Filename Offset (Relative to string table)<br>
     * Offset 0x04 | Size 4 | File: File Offset / Directory: Parent Index<br>
     * Offset 0x08 | Size 4 | File: File Length / Directory: Next Index / Root: Number of entries<br>
     *
     * @param rawEntry 12 byte array
     * @param index Index
     * @return File Entry
     */
    public static FileEntry getFileEntry(byte[] rawEntry, int index) {
        if (rawEntry == null) throw new IllegalArgumentException("rawEntry was null");

        FileEntry fe = new FileEntry();

        //First byte == 1 = Dir / First byte == 0 = File
        fe.isDir = rawEntry[0] == 1;

        //Get the filename offset
        fe.filenameOffset = (Byte.toUnsignedInt(rawEntry[1]) << 16) +
                            (Byte.toUnsignedInt(rawEntry[2]) << 8) +
                            (Byte.toUnsignedInt(rawEntry[3]));

        //Get the file offset / parent offset
        fe.offset = ByteBuffer.wrap(rawEntry, 4, 4).getInt();

        //Get the file length / next offset
        fe.length = ByteBuffer.wrap(rawEntry, 8, 4).getInt();

        fe.index = index;

        return fe;
    }

    @Override
    public String toString() {
        return filename;
    }

    public boolean filenameContainsIgnoreCase(String filename) {
        return this.filename.toUpperCase().contains(filename.toUpperCase());
    }
}
