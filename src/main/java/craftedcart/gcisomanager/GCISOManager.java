package craftedcart.gcisomanager;

import craftedcart.gcisomanager.type.Tree;
import craftedcart.gcisomanager.util.LangManager;
import craftedcart.gcisomanager.util.LogHelper;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author CraftedCart
 *         Created on 23/10/2016 (DD/MM/YYYY)
 */
public class GCISOManager extends Application {

    private ISOManager isoManager = new ISOManager();

    private TreeView<String> treeView;

    private Map<String, Image> imageMap;

    public static void main(String[] args) {
        Application.launch(GCISOManager.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        LogHelper.info(GCISOManager.class, "GC ISO Manager Launched");

        cacheImages();

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

    private void cacheImages() {
        imageMap = new HashMap<>();

        imageMap.put("code-braces", new Image(getClass().getResourceAsStream("/image/code-braces.png")));
        imageMap.put("cube", new Image(getClass().getResourceAsStream("/image/cube.png")));
        imageMap.put("disk", new Image(getClass().getResourceAsStream("/image/disk.png")));
        imageMap.put("file-document", new Image(getClass().getResourceAsStream("/image/file-document.png")));
        imageMap.put("file-image", new Image(getClass().getResourceAsStream("/image/file-image.png")));
        imageMap.put("file", new Image(getClass().getResourceAsStream("/image/file.png")));
        imageMap.put("folder-outline", new Image(getClass().getResourceAsStream("/image/folder-outline.png")));
        imageMap.put("human", new Image(getClass().getResourceAsStream("/image/human.png")));
        imageMap.put("image-multiple", new Image(getClass().getResourceAsStream("/image/image-multiple.png")));
        imageMap.put("music-box", new Image(getClass().getResourceAsStream("/image/music-box.png")));
        imageMap.put("zip-box", new Image(getClass().getResourceAsStream("/image/zip-box.png")));
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

        ImageView image = new ImageView(getImageForFilename(node.getData().filename, node.getData().isDir));
        image.setFitHeight(20);
        image.setFitWidth(20);

        item.setGraphic(image);

        for (Tree.Node<FileEntry> iNode : node.getChildren()) {
            parent = item;
            recursePopulateFileEntryTree(iNode, parent);
        }
    }

    private Image getImageForFilename(String filename, boolean isDir) {
        if (filename.equals("/")) return imageMap.get("disk");
        if (filename.toUpperCase().endsWith(".REL")) return imageMap.get("code-braces");
        if (filename.toUpperCase().endsWith(".DOL")) return imageMap.get("code-braces");
        if (filename.toUpperCase().endsWith(".ELF")) return imageMap.get("code-braces");
        if (filename.toUpperCase().endsWith(".ADP")) return imageMap.get("music-box");
        if (filename.toUpperCase().endsWith(".DSP")) return imageMap.get("music-box");
        if (filename.toUpperCase().endsWith(".GMA")) return imageMap.get("cube");
        if (filename.toUpperCase().endsWith(".TPL")) return imageMap.get("image-multiple");
        if (filename.toUpperCase().endsWith(".LZ")) return imageMap.get("zip-box");
        if (filename.toUpperCase().endsWith(".SKL")) return imageMap.get("human");
        if (filename.toUpperCase().endsWith(".BNR")) return imageMap.get("file-image");
        if (filename.toUpperCase().endsWith(".STR")) return imageMap.get("file-document");

        //No other icon found
        if (isDir) {
            return imageMap.get("folder-outline");
        } else {
            return imageMap.get("file");
        }
    }

}
