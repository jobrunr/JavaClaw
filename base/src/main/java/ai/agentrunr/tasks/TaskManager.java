package ai.agentrunr.tasks;

import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class TaskManager {

    private static final Logger log = LoggerFactory.getLogger(TaskManager.class);
    private final JobScheduler jobScheduler;
    private final TaskRepository taskRepository;

    public TaskManager(JobScheduler jobScheduler, TaskRepository taskRepository) {
        this.jobScheduler = jobScheduler;
        this.taskRepository = taskRepository;
    }

    public void create(String name, String description) {
        Task task = taskRepository.save(Task.newTask(name, description));
        jobScheduler.<TaskHandler>enqueue(x -> x.executeTask(task.getId()));
        log.info("Task '{}' ({}) has been created.", task.getName(), task.getId());
    }

    public void schedule(LocalDateTime executionTime, String name, String description) {
        Instant createdAt = executionTime.atZone(ZoneId.systemDefault()).toInstant();
        Task task = taskRepository.save(Task.newTask(name, createdAt, description));
        jobScheduler.<TaskHandler>schedule(executionTime, x -> x.executeTask(task.getId()));
        log.info("Task '{}' ({}) has been scheduled at {}.", task.getName(), task.getId(), executionTime);
    }

    public void scheduleRecurrently(String cronExpression, String name, String description) {
        RecurringTask recurringTask = taskRepository.save(RecurringTask.newRecurringTask(name, description));
        jobScheduler.<RecurringTaskHandler>scheduleRecurrently(recurringTask.getName(), cronExpression, x -> x.executeTask(recurringTask.getId()));
        log.info("Task '{}' ({}) has been scheduled recurrently with cronExpression {}.", name, recurringTask.getId(), cronExpression);
    }

    public void createTaskFromRecurringTask(RecurringTask recurringTask) {
        Task task = taskRepository.save(Task.newTask(recurringTask.getName(), recurringTask.getDescription()));
        jobScheduler.<TaskHandler>enqueue(x -> x.executeTask(task.getId()));
        log.info("Task '{}' ({}) has been created from recurring task.", task.getName(), task.getId());
    }
}
