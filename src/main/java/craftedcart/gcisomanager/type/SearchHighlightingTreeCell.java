package craftedcart.gcisomanager.type;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;

import java.lang.ref.WeakReference;

/**
 * @author CraftedCart
 *         Created on 26/11/2016 (DD/MM/YYYY)
 */
public class SearchHighlightingTreeCell<T> extends TreeCell<T> {

    // must keep reference to binding to prevent premature garbage collection:
    private BooleanBinding matchesSearch;

    private HBox hbox;

    private WeakReference<TreeItem<T>> treeItemRef;

    private InvalidationListener treeItemGraphicListener = observable -> {
        updateDisplay(getItem(), isEmpty());
    };

    private InvalidationListener treeItemListener = new InvalidationListener() {
        @Override public void invalidated(Observable observable) {
            TreeItem<T> oldTreeItem = treeItemRef == null ? null : treeItemRef.get();
            if (oldTreeItem != null) {
                oldTreeItem.graphicProperty().removeListener(weakTreeItemGraphicListener);
            }

            TreeItem<T> newTreeItem = getTreeItem();
            if (newTreeItem != null) {
                newTreeItem.graphicProperty().addListener(weakTreeItemGraphicListener);
                treeItemRef = new WeakReference<TreeItem<T>>(newTreeItem);
            }
        }
    };

    private WeakInvalidationListener weakTreeItemGraphicListener =
            new WeakInvalidationListener(treeItemGraphicListener);

    private WeakInvalidationListener weakTreeItemListener =
            new WeakInvalidationListener(treeItemListener);

    void updateDisplay(T item, boolean empty) {
        if (item == null || empty) {
            hbox = null;
            setText(null);
            setGraphic(null);
        } else {
            // update the graphic if one is set in the TreeItem
            TreeItem<T> treeItem = getTreeItem();
            if (treeItem != null && treeItem.getGraphic() != null) {
                if (item instanceof Node) {
                    setText(null);

                    // the item is a Node, and the graphic exists, so
                    // we must insert both into an HBox and present that
                    // to the user (see RT-15910)
                    if (hbox == null) {
                        hbox = new HBox(3);
                    }
                    hbox.getChildren().setAll(treeItem.getGraphic(), (Node)item);
                    setGraphic(hbox);
                } else {
                    hbox = null;
                    setText(item.toString());
                    setGraphic(treeItem.getGraphic());
                }
            } else {
                hbox = null;
                if (item instanceof Node) {
                    setText(null);
                    setGraphic((Node)item);
                } else {
                    setText(item.toString());
                    setGraphic(null);
                }
            }
        }
    }

    @Override public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        updateDisplay(item, empty);
    }

    public SearchHighlightingTreeCell(ObservableSet<TreeItem<T>> searchMatches) {

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

}
