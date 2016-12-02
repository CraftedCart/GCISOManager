package craftedcart.gcisomanager.type;

import craftedcart.gcisomanager.FileEntry;
import craftedcart.gcisomanager.ISOManager;
import craftedcart.gcisomanager.task.ExtractMultipleTask;
import craftedcart.gcisomanager.task.ExtractTask;
import craftedcart.gcisomanager.util.LangManager;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CraftedCart
 *         Created on 26/11/2016 (DD/MM/YYYY)
 */
public class SearchHighlightingTreeCell extends TreeCell<FileEntry> {

    private Stage stage;
    private final ISOManager isoManager;

    // must keep reference to binding to prevent premature garbage collection:
    private BooleanBinding matchesSearch;

    private HBox hbox;

    private WeakReference<TreeItem<FileEntry>> treeItemRef;

    private InvalidationListener treeItemGraphicListener = observable -> {
        updateDisplay(getItem(), isEmpty());
    };

    private InvalidationListener treeItemListener = new InvalidationListener() {
        @Override public void invalidated(Observable observable) {
            TreeItem<FileEntry> oldTreeItem = treeItemRef == null ? null : treeItemRef.get();
            if (oldTreeItem != null) {
                oldTreeItem.graphicProperty().removeListener(weakTreeItemGraphicListener);
            }

            TreeItem<FileEntry> newTreeItem = getTreeItem();
            if (newTreeItem != null) {
                newTreeItem.graphicProperty().addListener(weakTreeItemGraphicListener);
                treeItemRef = new WeakReference<>(newTreeItem);
            }
        }
    };

    private WeakInvalidationListener weakTreeItemGraphicListener = new WeakInvalidationListener(treeItemGraphicListener);

    private WeakInvalidationListener weakTreeItemListener = new WeakInvalidationListener(treeItemListener);

    private void updateDisplay(FileEntry item, boolean empty) {
        if (item == null || empty) {
            hbox = null;
            setText(null);
            setGraphic(null);
        } else {
            // update the graphic if one is set in the TreeItem
            TreeItem<FileEntry> treeItem = getTreeItem();
            if (treeItem != null && treeItem.getGraphic() != null) {
                hbox = null;
                setText(item.toString());
                setGraphic(treeItem.getGraphic());
            } else {
                hbox = null;
                setText(item.toString());
                setGraphic(null);
            }
        }
    }

    @Override
    public void updateItem(FileEntry item, boolean empty) {
        super.updateItem(item, empty);
        updateDisplay(item, empty);

        setContextMenu(null);

        if (getTreeItem() != null) {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem importMenuItem = new MenuItem(LangManager.getItem("import"));
            MenuItem extractMenuItem = new MenuItem(LangManager.getItem("extract"));

            if (getTreeItem().getValue().isDir) {
                extractMenuItem.setOnAction((event) -> requestExtractDir());
            } else {
                extractMenuItem.setOnAction((event) -> requestExtractFile(item));
            }

            contextMenu.getItems().add(importMenuItem);
            contextMenu.getItems().add(extractMenuItem);

            setContextMenu(contextMenu);
        }
    }

    public SearchHighlightingTreeCell(ObservableSet<TreeItem<FileEntry>> searchMatches, Stage stage, ISOManager isoManager) {
        this.stage = stage;
        this.isoManager = isoManager;

        treeItemProperty().addListener(weakTreeItemListener);

        if (getTreeItem() != null) {
            getTreeItem().graphicProperty().addListener(weakTreeItemGraphicListener);
        }

        // pseudoclass for highlighting state
        // css can set style with selector
        // .tree-cell:search-match { ... }
        PseudoClass searchMatch = PseudoClass.getPseudoClass("search-match");

        // initialize binding. Evaluates to true if searchMatches
        // contains the current treeItem

        // note the binding observes both the treeItemProperty and searchMatches,
        // so it updates if either one changes:
        matchesSearch = Bindings.createBooleanBinding(() ->
                        searchMatches.contains(getTreeItem()),
                treeItemProperty(), searchMatches);

        // update the pseudoclass state if the binding value changes:
        matchesSearch.addListener((obs, didMatchSearch, nowMatchesSearch) ->
                pseudoClassStateChanged(searchMatch, nowMatchesSearch));
    }

    private void requestExtractFile(FileEntry entry) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LangManager.getItem("extract"));
        fileChooser.setInitialFileName(entry.filename);
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            isoManager.getTaskManager().queueTask(new ExtractTask(entry, file, isoManager));
        }
    }

    private void requestExtractDir() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(LangManager.getItem("extract"));

        File file = directoryChooser.showDialog(stage);

        if (file != null) {
            ExtractMultipleTask task = new ExtractMultipleTask(getTreeItem().getValue());
            isoManager.getTaskManager().queueTask(task);

            List<ExtractTask> taskList = new ArrayList<>();

            recurseQueueExtract(getTreeItem(), file, taskList);

            task.setExtractTasks(taskList);
        }
    }

    /**
     * @param item The file to extract
     * @param file The file to output to
     * @param allTasks Populated with a list of ExtractTasks
     */
    private void recurseQueueExtract(TreeItem<FileEntry> item, File file, List<ExtractTask> allTasks) {
        File currentFile = file;

        if (item.getValue().isDir) {
            currentFile = new File(file, item.getValue().index == 0 ? "root" : item.getValue().filename);
            currentFile.mkdirs();
        } else {
            ExtractTask task = new ExtractTask(item.getValue(), new File(currentFile, item.getValue().filename), isoManager);
            isoManager.getTaskManager().queueTask(task);
            allTasks.add(task);
        }

        for (TreeItem<FileEntry> childItem : item.getChildren()) {
            recurseQueueExtract(childItem, currentFile, allTasks);
        }
    }

}
