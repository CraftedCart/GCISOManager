package craftedcart.gcisomanager.task;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/**
 * @author CraftedCart
 *         Created on 28/11/2016 (DD/MM/YYYY)
 */
public class TaskListCell extends ListCell<Task> {

    @Override
    protected void updateItem(Task item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            VBox vbox = new VBox();
            vbox.setSpacing(2);
            setGraphic(vbox);

            Label label = new Label();
            label.setText(item.getLocalizedString());
            vbox.getChildren().add(label);

            ProgressBar progBar = new ProgressBar();
            progBar.prefWidthProperty().bind(vbox.widthProperty());
            progBar.progressProperty().bind(item.percentProperty());
            vbox.getChildren().add(progBar);
        }
    }

}
