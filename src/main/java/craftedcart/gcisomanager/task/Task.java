package craftedcart.gcisomanager.task;

import craftedcart.gcisomanager.type.CallbackAction1;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;

import java.util.Set;

/**
 * @author CraftedCart
 *         Created on 28/11/2016 (DD/MM/YYYY)
 */
public abstract class Task {

    protected SimpleDoubleProperty percent = new SimpleDoubleProperty(0);

    protected Set<CallbackAction1<Task>> onTaskFinishActions;

    public abstract String getLocalizedString();

    public double getPercent() {
        return percent.get();
    }

    public ObservableValue<? extends Number> percentProperty() {
        return percent;
    }

    public abstract Runnable getRunnable();

    public void setOnTaskFinishActions(Set<CallbackAction1<Task>> onFinishActions) {
        this.onTaskFinishActions = onFinishActions;
    }
}
