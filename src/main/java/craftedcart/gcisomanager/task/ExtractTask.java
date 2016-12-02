package craftedcart.gcisomanager.task;

import craftedcart.gcisomanager.FileEntry;
import craftedcart.gcisomanager.GCISOManager;
import craftedcart.gcisomanager.ISOManager;
import craftedcart.gcisomanager.util.LangManager;
import craftedcart.gcisomanager.util.LogHelper;
import craftedcart.gcisomanager.util.MathUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.IOException;

/**
 * @author CraftedCart
 *         Created on 28/11/2016 (DD/MM/YYYY)
 */
public class ExtractTask extends Task {

    private FileEntry entry;
    private File outFile;
    private ISOManager isoManager;

    private SimpleLongProperty bytesDone = new SimpleLongProperty();
    private SimpleLongProperty bytesTotal = new SimpleLongProperty();

    public ExtractTask(FileEntry entry, File outFile, ISOManager isoManager) {
        this.entry = entry;
        this.outFile = outFile;
        this.isoManager = isoManager;

        bytesTotal.set(entry.length);
    }

    @Override
    public String getLocalizedString() {
        return String.format(LangManager.getItem("extractingFileFormat"), entry.filename,
                MathUtils.prettifyByteCount(bytesDone.get(), true), MathUtils.prettifyByteCount(bytesTotal.get(), true));
    }

    public long getBytesDone() {
        return bytesDone.get();
    }

    public long getBytesTotal() {
        return bytesTotal.get();
    }

    @Override
    public Runnable getRunnable() {
        return () -> {
            try {
                isoManager.extractFile(entry, outFile, percent, bytesDone);
            } catch (IOException e) {
                LogHelper.error(getClass(), LogHelper.stackTraceToString(e));

                if (GCISOManager.isUsingGUI()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, LangManager.getItem("extractError"));
                        alert.showAndWait();
                    });
                }
            }
        };
    }

}
