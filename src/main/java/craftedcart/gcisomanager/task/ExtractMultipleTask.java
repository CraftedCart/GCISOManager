package craftedcart.gcisomanager.task;

import craftedcart.gcisomanager.FileEntry;
import craftedcart.gcisomanager.GCISOManager;
import craftedcart.gcisomanager.ISOManager;
import craftedcart.gcisomanager.type.CallbackAction1;
import craftedcart.gcisomanager.util.LangManager;
import craftedcart.gcisomanager.util.LogHelper;
import craftedcart.gcisomanager.util.MathUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CraftedCart
 *         Created on 28/11/2016 (DD/MM/YYYY)
 */
public class ExtractMultipleTask extends Task {

    private FileEntry entry;
    private List<ExtractTask> extractTasks = new ArrayList<>();

    private long bytesDone = 0;
    private long bytesTotal = 0;

    public ExtractMultipleTask(FileEntry rootEntry) {
        entry = rootEntry;
    }

    @Override
    public String getLocalizedString() {
        calcValues();
        return String.format(LangManager.getItem("extractingFileFormat"), entry.filename, MathUtils.prettifyByteCount(bytesDone, true), MathUtils.prettifyByteCount(bytesTotal, true));
    }

    public void setExtractTasks(List<ExtractTask> extractTasks) {
        this.extractTasks = extractTasks;
    }

    @Override
    public SimpleDoubleProperty percentProperty() {
        calcValues();
        return percent;
    }

    private void calcValues() {
        percent.set(0);
        bytesDone = 0;
        bytesTotal = 0;

        for (ExtractTask task : extractTasks) {
            percent.set(percent.get() + task.percent.get());
            bytesDone += task.getBytesDone();
            bytesTotal += task.getBytesTotal();
        }

        percent.set(percent.get() / extractTasks.size());

        if (bytesDone == bytesTotal) { //Done - Run the finish actions
            for (CallbackAction1<Task> action : onTaskFinishActions) {
                action.execute(this);
            }
        }
    }

    @Override
    public Runnable getRunnable() {
        return null;
    }

}
