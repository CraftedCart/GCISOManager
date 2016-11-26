package craftedcart.gcisomanager;

import craftedcart.gcisomanager.type.SearchHighlightingTreeCell;
import craftedcart.gcisomanager.type.Tree;
import craftedcart.gcisomanager.util.LangManager;
import craftedcart.gcisomanager.util.LogHelper;
import craftedcart.gcisomanager.util.MathUtils;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author CraftedCart
 *         Created on 23/10/2016 (DD/MM/YYYY)
 */
public class GCISOManager extends Application {

    private ISOManager isoManager = new ISOManager();

    private TreeView<FileEntry> treeView;
    private ObservableSet<TreeItem<FileEntry>> searchMatches = FXCollections.observableSet(new HashSet<TreeItem<FileEntry>>());
    //Toolbar
    private TextField searchTextField;
    //Status Bar
    private Label indexLabel;
    private Label offsetLabel;
    private Label lengthLabel;

    private Map<String, Image> imageMap;

    public static void main(String[] args) {
        Application.launch(GCISOManager.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        LogHelper.info(GCISOManager.class, "GC ISO Manager Launched");

        cacheImages();

        //Spacers
        Region horizSpacer = new Region();
        HBox.setHgrow(horizSpacer, Priority.ALWAYS);

        //The root of the scene shown in the main window
        BorderPane root = new BorderPane();

        HBox toolbar = new HBox();
        toolbar.setPadding(new Insets(4));
        toolbar.setSpacing(4);
        root.setTop(toolbar);

        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(2, 4, 2, 4));
        statusBar.setSpacing(8);
        root.setBottom(statusBar);

        indexLabel = new Label();
        statusBar.getChildren().add(indexLabel);
        offsetLabel = new Label();
        statusBar.getChildren().add(offsetLabel);
        lengthLabel = new Label();
        statusBar.getChildren().add(lengthLabel);

        ImageView diskImageView = new ImageView(imageMap.get("disk"));
        diskImageView.setFitWidth(17);
        diskImageView.setFitHeight(17);
        Button button = new Button(LangManager.getItem("openISO"), diskImageView);
        button.setOnAction(e -> chooseISO(primaryStage));
        toolbar.getChildren().add(button);

        toolbar.getChildren().add(horizSpacer);

        searchTextField = new TextField();
        searchTextField.setPromptText(LangManager.getItem("search"));
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                searchMatches.clear();
                return;
            }

            Set<TreeItem<FileEntry>> matches = new HashSet<>();
            searchMatchingItems(treeView.getRoot(), matches, newValue);
            searchMatches.clear();
            searchMatches.addAll(matches);
        });
        toolbar.getChildren().add(searchTextField);

        treeView = new TreeView<>();
        treeView.setCellFactory(tv -> new SearchHighlightingTreeCell(searchMatches));
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> onSelectionChanged(newValue));
        root.setCenter(treeView);

        Scene scene = new Scene(root, 500, 300);

        //Load CSS styling
        String css = getClass().getResource("/style.css").toExternalForm();
        scene.getStylesheets().add(css);

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

                    //Search for whatever's in the text field
                    if (searchTextField.getText().isEmpty()) {
                        searchMatches.clear();
                        return;
                    }

                    Set<TreeItem<FileEntry>> matches = new HashSet<>();
                    searchMatchingItems(treeView.getRoot(), matches, searchTextField.getText());
                    searchMatches.clear();
                    searchMatches.addAll(matches);

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

    private void recursePopulateFileEntryTree(Tree.Node<FileEntry> node, TreeItem<FileEntry> parent) {
        TreeItem<FileEntry> item;
        if (parent == null) { //If the node is the root
            item = new TreeItem<>(node.getData());
            item.setExpanded(true);
            treeView.setRoot(item);
        } else {
            item = new TreeItem<>(node.getData());
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

    private void onSelectionChanged(TreeItem<FileEntry> newValue) {
        if (newValue != null) {
            FileEntry entry = newValue.getValue();

            indexLabel.setText(String.format(LangManager.getItem("index"), entry.index));

            if (entry.isDir) {
                if (entry.index == 0) { //If it's the root
                    offsetLabel.setText("");
                    lengthLabel.setText(String.format(LangManager.getItem("entryCount"), entry.length));
                } else {
                    offsetLabel.setText(String.format(LangManager.getItem("parentIndex"), entry.offset, newValue.getParent().getValue().filename));
                    lengthLabel.setText(String.format(LangManager.getItem("nextIndex"), entry.length));
                }
            } else {
                offsetLabel.setText(String.format(LangManager.getItem("offset"), entry.offset, MathUtils.prettifyByteCount(entry.offset, true), MathUtils.prettifyByteCount(entry.offset, false)));
                lengthLabel.setText(String.format(LangManager.getItem("length"), entry.length, MathUtils.prettifyByteCount(entry.length, true), MathUtils.prettifyByteCount(entry.length, false)));
            }

        } else {
            indexLabel.setText("");
            offsetLabel.setText("");
            lengthLabel.setText("");
        }
    }

    private void searchMatchingItems(TreeItem<FileEntry> searchNode, Set<TreeItem<FileEntry>> matches, String searchValue) {
        if (searchNode == null) return; //If no root node, get outta here

        if (searchNode.getValue().filenameContainsIgnoreCase(searchValue)) {
            recurseAddParentsToSet(searchNode, matches);
        }

        for (TreeItem<FileEntry> child : searchNode.getChildren()) {
            searchMatchingItems(child, matches, searchValue);
        }
    }

    private void recurseAddParentsToSet(TreeItem<FileEntry> item, Set<TreeItem<FileEntry>> matches) {
        matches.add(item);

        if (item.getParent() != null) {
            recurseAddParentsToSet(item.getParent(), matches);
        }
    }

}
