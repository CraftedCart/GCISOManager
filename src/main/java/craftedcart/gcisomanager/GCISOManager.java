package craftedcart.gcisomanager;

import craftedcart.gcisomanager.type.Tree;
import craftedcart.gcisomanager.util.LangManager;
import craftedcart.gcisomanager.util.LogHelper;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

/**
 * @author CraftedCart
 *         Created on 23/10/2016 (DD/MM/YYYY)
 */
public class GCISOManager extends Application {

    private ISOManager isoManager = new ISOManager();
    private TreeView<String> treeView;

    public static void main(String[] args) {
        Application.launch(GCISOManager.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        LogHelper.info(GCISOManager.class, "GC ISO Manager Launched");

        //The root of the scene shown in the main window
        BorderPane root = new BorderPane();
        HBox toolbar = new HBox();

        root.setTop(toolbar);

        Button button = new Button(LangManager.getItem("openISO"));
        button.setOnAction(e -> chooseISO(primaryStage));
        toolbar.getChildren().add(button);

        treeView = new TreeView<>();
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        root.setCenter(treeView);

        Scene scene = new Scene(root, 500, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle(LangManager.getItem("gcISOManager"));
        primaryStage.show();
    }

    private void chooseISO(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LangManager.getItem("openISO"));

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LangManager.getItem("isoFileFilter"), "*.iso"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LangManager.getItem("allFileFilter"), "*.*"));

        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {

                if (isoManager.openISO(file)) {
                    recursePopulateFileEntryTree(isoManager.getFileEntryTree().getRootNode(), null);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, LangManager.getItem("notGameCubeISO"));
                    alert.showAndWait();
                }

            } catch (IOException e) {
                LogHelper.error(getClass(), "Error while opening ISO");
                LogHelper.error(getClass(), LogHelper.stackTraceToString(e));

                Alert alert = new Alert(Alert.AlertType.ERROR, LangManager.getItem("fileReadError"));
                alert.showAndWait();
            }
        }
    }

    private void recursePopulateFileEntryTree(Tree.Node<FileEntry> node, TreeItem<String> parent) {
        TreeItem<String> item;
        if (parent == null) { //If the node is the root
            item = new TreeItem<>(node.getData().filename);
            item.setExpanded(true);
            treeView.setRoot(item);
        } else {
            item = new TreeItem<>(node.getData().filename);
            parent.getChildren().add(item);
        }

        for (Tree.Node<FileEntry> iNode : node.getChildren()) {
            parent = item;
            recursePopulateFileEntryTree(iNode, parent);
        }
    }

}
