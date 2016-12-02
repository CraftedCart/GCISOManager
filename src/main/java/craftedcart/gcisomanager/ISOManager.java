package craftedcart.gcisomanager;

import craftedcart.gcisomanager.task.TaskManager;
import craftedcart.gcisomanager.type.CallbackAction1;
import craftedcart.gcisomanager.type.EnumErrorFlag;
import craftedcart.gcisomanager.type.Tree;
import craftedcart.gcisomanager.util.GCMUtils;
import craftedcart.gcisomanager.util.LogHelper;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author CraftedCart
 *         Created on 23/10/2016 (DD/MM/YYYY)
 */
public class ISOManager {

    private File gcmFileLoc;
    private RandomAccessFile gcmFile;

    private int dirDepth = 0;
    private int recursiveIndex = 0;

    private Tree<FileEntry> fileEntryTree;
    private Map<Integer, Tree.Node<FileEntry>> fileEntryDepthMap;

    private TaskManager taskManager = new TaskManager();

    public boolean openISO(File file) throws IOException {
        RandomAccessFile oldGCMFile = gcmFile;

        class ErrorFlagContainer {
            private EnumErrorFlag errorFlag = EnumErrorFlag.NO_ERROR;
        }

        final ErrorFlagContainer errorFlagContainer = new ErrorFlagContainer();

        gcmFile = new RandomAccessFile(file, "r");

        if (!checkImage(gcmFile)) {
            LogHelper.error(getClass(), "Invalid disk image");

            close();
            return false;
        } else {
            LogHelper.info(getClass(), "Initial disk image check seems ok");
        }

        fileEntryDepthMap = new HashMap<>();


        FileEntry rootFileEntry = getRootFileEntry(gcmFile);
        recurseFileEntry(rootFileEntry, (entry) -> {
            try {
                Tree.Node<FileEntry> newNode = new Tree.Node<>(entry);

                fetchFilenameForFileEntry(gcmFile, entry);

                if (entry.index == 0) { //If it's the root node
                    fileEntryTree = new Tree<>(entry); //create a new fileEntryTree
                    fileEntryDepthMap.put(dirDepth, fileEntryTree.getRootNode()); //Set the last node
                } else {
                    fileEntryDepthMap.get(dirDepth - 1).addChild(newNode);
                    if (entry.isDir) {
                        fileEntryDepthMap.put(dirDepth, newNode);
                    }
                }

            } catch (IOException e) {
                LogHelper.error(getClass(), LogHelper.stackTraceToString(e));
                errorFlagContainer.errorFlag = EnumErrorFlag.IO_EXCEPTION;
            }
        });

        fileEntryDepthMap = null; //Free up memory

        if (errorFlagContainer.errorFlag == EnumErrorFlag.IO_EXCEPTION) {
            throw new IOException();
        }

        if (oldGCMFile != null) { //If the previously open file exists, close it
            oldGCMFile.close();
        }

        gcmFileLoc = file;

        return true;
    }

    public void close() throws IOException {
        gcmFile.close();
    }

    private boolean checkImage(RandomAccessFile file) throws IOException {
        file.seek(28L);
        int in = FileUtils.readIntLittleEndian(file);

        return in == 1033843650;
    }

    /**
     * @param file The file
     * @return The root file entry (Entry 0)
     * @throws IOException When something went wrong reading from the file
     */
    private FileEntry getRootFileEntry(RandomAccessFile file) throws IOException {
        return getFileEntryByIndex(file, 0);
    }

    /**
     * @param file The file
     * @param index The index
     * @return The index file entry as a FileEntry object
     * @throws IOException When something went wrong reading from the file
     */
    private FileEntry getFileEntryByIndex(RandomAccessFile file, int index) throws IOException {
        byte[] rawEntry = getRawFileEntryByIndex(file, index);
        return FileEntry.getFileEntry(rawEntry, index);
    }

    /**
     * @param file The disk image file
     * @param index Index
     * @return The raw file entry corresponding with the index
     * @throws IOException When something went wrong reading from the file
     */
    private byte[] getRawFileEntryByIndex(RandomAccessFile file, int index) throws IOException {
        if (file == null) throw new IllegalArgumentException("File was null");
        if (index < 0) throw new IllegalArgumentException("Index was less than 0");

        file.seek(getFileEntryOffsetByIndex(file, index));
        byte[] bytes = new byte[GCMUtils.FST_ENTRY_LENGTH];
        for (int i = 0; i < GCMUtils.FST_ENTRY_LENGTH; i++) {
            bytes[i] = file.readByte();
        }

        return bytes;
    }

    /**
     * @param file The file
     * @param index The index of the file to get
     * @return The offset to the file entry in the FST
     * @throws IOException When something went wrong reading from the file
     */
    private long getFileEntryOffsetByIndex(RandomAccessFile file, int index) throws IOException {
        if (file == null) throw new IllegalArgumentException("File was null");

        return getFSTOffset(file) + (GCMUtils.FST_ENTRY_LENGTH * index);
    }

    /**
     * @param file The file
     * @return The FST offset (Found on the disk header)
     * @throws IOException When something went wrong reading from the file
     */
    private int getFSTOffset(RandomAccessFile file) throws IOException {
        if (file == null) throw new IllegalArgumentException("File was null");

        file.seek(GCMUtils.DISK_HEADER_OFFSET + GCMUtils.FST_OFFSET_OFFSET + GCMUtils.gDataOffset);
        return file.readInt();
    }

    /**
     * Recurse through the file entry. If it's a directory, recurse through all its children
     * otherwise, call the callback
     *
     * @param e The file entry to recurse
     * @param callback The callback to call when a file is found
     * @throws IOException When something went wrong reading from the file
     */
    private void recurseFileEntry(FileEntry e, CallbackAction1<FileEntry> callback) throws IOException {
        if (e == null) throw new IllegalArgumentException("FileEntry e was null");

        callback.execute(e);

        if (e.isDir) {
            dirDepth++;
            FileEntry next;

            for (recursiveIndex = e.index + 1; recursiveIndex < e.length; recursiveIndex++) {
                next = getFileEntryByIndex(gcmFile, recursiveIndex);

                if (next != null) {
                    recurseFileEntry(next, callback);
                }
            }

            recursiveIndex--;
            dirDepth--;
        }
    }

    /**
     * Inspects entry, looks up the filename, and sets entry.filename to the filename found in the file
     *
     * @param file The disk image file
     * @param entry The file entry
     * @throws IOException When something went wrong reading from the file
     */
    private void fetchFilenameForFileEntry(RandomAccessFile file, FileEntry entry) throws IOException {
        if (file == null) throw new IllegalArgumentException("File was null");
        if (entry == null) throw new IllegalArgumentException("Entry was null");

        if (entry.index == 0) { //If it's the root entry
            entry.filename = "/";
            return;
        }

        file.seek(getStringTableOffset(file) + entry.filenameOffset);

        List<Character> nameCharList = new ArrayList<>();

        for (int i = 0; i < GCMUtils.MAX_FILENAME_LENGTH; i++) {
            char nameChar = (char) file.read();
            if (nameChar == '\0') { //If nameChar is a null character (End of string)
                break;
            }

            nameCharList.add(nameChar);
        }

        StringBuilder nameSB = new StringBuilder(nameCharList.size());
        nameCharList.forEach(nameSB::append);
        entry.filename = nameSB.toString();
    }

    /**
     * @param file The disk image file
     * @return The offset to the string table
     * @throws IOException When something went wrong reading from the file
     */
    private long getStringTableOffset(RandomAccessFile file) throws IOException {
        if (file == null) throw new IllegalArgumentException("File was null");

        return getFSTOffset(file) + getFileEntryCount(file) * GCMUtils.FST_ENTRY_LENGTH;
    }

    /**
     * @param file The disk image file
     * @return The number of file entries
     * @throws IOException When something went wrong reading from the file
     */
    private int getFileEntryCount(RandomAccessFile file) throws IOException {
        if (file == null) throw new IllegalArgumentException("File was null");

        //Grab the root entry, and return how many files follow it
        FileEntry e = getRootFileEntry(file);
        return (int) e.length;
    }

    /**
     * Extract files from the disk image to the destinationFile (In the local filesystem)
     *
     * @param entry The source file in the disk image
     * @param destinationFile The location to extract to
     */
    public void extractFile(FileEntry entry, File destinationFile, SimpleDoubleProperty percent, SimpleLongProperty bytesDone) throws IOException {
        if (entry.isDir) throw new IllegalArgumentException("Entry is a directory");

        if (destinationFile.exists()) {
            if (!destinationFile.delete()) throw new IOException("Failed to delete destinationFile");
        }

        DataInputStream gcmDIS = new DataInputStream(new BufferedInputStream(new FileInputStream(gcmFileLoc)));
        DataOutputStream outDOS = new DataOutputStream(new FileOutputStream(destinationFile));

        gcmDIS.skip(entry.offset);

        for (long i = 0; i < entry.length; i += 32768) {
            byte[] contents = new byte[32768];
            int bytesRead = gcmDIS.read(contents);

            outDOS.write(contents, 0, bytesRead);

            if (GCISOManager.isUsingGUI()) {
                final long iClone = i;

                Platform.runLater(() -> {
                    percent.set(iClone / (double) entry.length);
                    bytesDone.setValue(iClone);
                });
            } else {
                percent.set(i / (double) entry.length);
                bytesDone.setValue(i);
            }

        }

        outDOS.close();

        if (GCISOManager.isUsingGUI()) {
            Platform.runLater(() -> {
                percent.set(1);
                bytesDone.setValue(entry.length);
            });
        } else {
            percent.set(1);
            bytesDone.setValue(entry.length);
        }
    }

    public Tree<FileEntry> getFileEntryTree() {
        return fileEntryTree;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }
}
