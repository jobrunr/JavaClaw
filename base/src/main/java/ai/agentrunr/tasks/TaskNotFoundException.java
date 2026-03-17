package ai.agentrunr.tasks;

import java.io.IOException;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String id, IOException e) {
        super("Task with id '" + id + "' was not found.", e);
    }
}
