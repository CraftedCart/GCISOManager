package craftedcart.gcisomanager.task;

import craftedcart.gcisomanager.type.CallbackAction1;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author CraftedCart
 *         Created on 28/11/2016 (DD/MM/YYYY)
 */
public class TaskManager {

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    private Set<CallbackAction1<Task>> onTaskSubmitActions = new HashSet<>();
    private Set<CallbackAction1<Task>> onTaskFinishActions = new HashSet<>();

    public void queueTask(Task task) {
        for (CallbackAction1<Task> action : onTaskSubmitActions) {
            action.execute(task);
        }

        Runnable runnable = task.getRunnable();

        task.setOnTaskFinishActions(onTaskFinishActions);

        if (runnable != null) {
            executor.submit(() -> {
                runnable.run();

                for (CallbackAction1<Task> action : onTaskFinishActions) {
                    action.execute(task);
                }
            });
        }
    }

    public void addOnTaskSubmitAction(CallbackAction1<Task> action) {
        onTaskSubmitActions.add(action);
    }

    public void addOnTaskFinishAction(CallbackAction1<Task> action) {
        onTaskFinishActions.add(action);
    }

    public void shutdownNow() {
        executor.shutdownNow();
    }

}
