package ai.agentrunr.tasks;

public class RecurringTask {

    private final String id;
    private final String name;
    private final String description;

    public RecurringTask(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static RecurringTask newRecurringTask(String name, String description) {
        return new RecurringTask(null, name, description);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Recurring Task '" + name + "'";
    }
}
