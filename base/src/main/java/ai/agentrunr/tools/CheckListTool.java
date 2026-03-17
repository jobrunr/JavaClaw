package ai.agentrunr.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

/**
 * Creates and manages structured checklists for AI assistants.
 * <p>
 * This is a Spring AI implementation of Agent Tasks, enabling AI agents to track progress, organize complex checklists, and provide visibility into
 * execution. The tool validates checklist item states to ensure only one checklist item is in progress at a time and that all checklist items data is properly formatted.
 *
 * @author Christian Tzolov
 */
public class CheckListTool {

    private static final Logger logger = LoggerFactory.getLogger(CheckListTool.class);

    private final CheckListEventHandler checkListEventHandler;

    @FunctionalInterface
    public interface CheckListEventHandler {

        void handle(CheckList checkList);

    }

    protected CheckListTool(CheckListEventHandler checkListEventHandler) {
        this.checkListEventHandler = checkListEventHandler;
    }


    @Tool(name = "CheckListTool", description = """
            Use this tool to create and manage a structured checklist for a single complex task, goal or assignment. This helps you track progress, organize multi-step plans, and demonstrate thoroughness to the user. It also helps the user understand the overall progress of their request.
            
                  ## When to Use This Tool
                  Use this tool proactively in these scenarios:
            
                  1. Complex multi-step tasks — When a goal requires 3 or more distinct steps or actions
                  2. Non-trivial planning tasks — Tasks that require careful sequencing or multiple operations
                  3. User explicitly requests a todo list — When the user directly asks you to track tasks
                  4. After receiving new instructions — Immediately capture requirements as a checklist
                  5. When you start working on an item of the checklist — Mark it as in_progress BEFORE beginning work. Only one item should be in_progress at a time
                  6. After completing an item — Mark it as completed and add any follow-up checklist items discovered along the way
            
                  ## When NOT to Use This Tool
            
                  Skip using this tool when:
                  1. There is only a single, straightforward task
                  2. The task is trivial and tracking it provides no organizational benefit
                  3. The task can be completed in fewer than 3 trivial steps
                  4. The task is purely conversational or informational
                  5. User provides multiple goals or assignments — When users provide a list of things to be done (e.g. numbered or comma-separated), create separate tasks instead of a checklist
            
                  ## Examples of When to Use the Checklist
            
                  <example>
                  User: Help me plan a holiday to Japan for two weeks in April.
                  Assistant: I'll help plan your Japan trip. Let me create a checklist to organize everything.
                  *Creates checklist with items like:*
                  1. Define trip dates and budget
                  2. Research and book round-trip flights
                  3. Plan a day-by-day itinerary (Tokyo, Kyoto, Osaka, etc.)
                  4. Book hotels or accommodation for each city
                  5. Research JR Pass and local transport options
                  6. Book key experiences (Fuji, tea ceremony, Nara deer park, etc.)
                  7. Check visa requirements and travel insurance
                  8. Prepare packing list and travel documents
            
                  <reasoning>
                  Holiday planning is a multi-step, non-trivial task involving research, bookings, logistics, and preparation across many domains. The checklist keeps all threads organized and visible.
                  </reasoning>
                  </example>
            
                  ## Examples of When NOT to Use the Checklist
            
                  <example>
                  User: What's the capital of Australia?
                  Assistant: The capital of Australia is Canberra.
            
                  <reasoning>
                  Single, trivial factual question. No chechlist tracking needed.
                  </reasoning>
                  </example>
            
                  <example>
                  User: Add a comment to the calculateTotal function.
                  Assistant: *Edits the file to add the comment.*
            
                  <reasoning>
                  Single, contained action. No list needed.
                  </reasoning>
                  </example>
            
                  ## Checklist Item States and Management
            
                  1. **Checklist Item States**:
                   - pending: Not yet started
                   - in_progress: Currently being worked on (limit to ONE at a time)
                   - completed: Finished successfully
            
                  **IMPORTANT**: Each Checklist Item must have two forms:
                  - content: Imperative form (e.g. "Book round-trip flights")
                  - activeForm: Present continuous form shown while active (e.g. "Booking round-trip flights")
            
                  2. **Checklist Item Management**:
                   - Update checklist items status in real time as you work
                   - Mark checklist item complete IMMEDIATELY after finishing — don't batch completions
                   - Exactly ONE checklist item must be in_progress at any time
                   - Complete the current checklist item before starting the next one
                   - Remove checklist items that are no longer relevant
            
                  3. **Checklist Item Completion Requirements**:
                   - ONLY mark a checklist item completed when it is FULLY done
                   - If blocked or encountering errors, keep it in_progress and create a new checklist item to resolve the blocker
                   - Never mark a checklist item complete if it is partial or unresolved
            
                  4. **Checklist Item Breakdown**:
                   - Create specific, actionable items appropriate to the domain (travel, events, software, life admin, etc.)
                   - Break complex tasks into smaller, manageable steps
                   - Always provide both content and activeForm fields
            
                  When in doubt, use this tool. Proactive use the CheckList Tool — across any domain — demonstrates attentiveness and ensures nothing falls through the cracks.
            """)
    public String manageCheckList(CheckList checkList) { // @formatter:on

        // Validate the checkList
        this.validateCheckList(checkList);

        this.checkListEventHandler.handle(checkList);

        return "Checklist has been modified successfully. Ensure that you continue to use the checklist to track your progress. Please proceed with the current checklist if applicable";
    }
    // @formatter:off

    /**
     * Validates the checklist according to the following rules: - Only one checklist can be
     * in_progress at a time - Checklist content and activeForm must not be empty or blank -
     * All checklist items must have valid status values
     *
     * @param checkList the checklist to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCheckList(CheckList checkList) {
        if (checkList == null || checkList.checkList() == null) {
            throw new IllegalArgumentException("Checklist cannot be null");
        }

        List<CheckList.CheckListItem> items = checkList.checkList();

        // Validate each checkListItem first (before counting in_progress checkList)
        for (int i = 0; i < items.size(); i++) {
            CheckList.CheckListItem checkListItem = items.get(i);

            if (checkListItem == null) {
                throw new IllegalArgumentException("Checklist item at index " + i + " is null");
            }

            if (checkListItem.content() == null || checkListItem.content().isBlank()) {
                throw new IllegalArgumentException(
                        "Checklist item at index " + i + " has empty or blank content. All taskList must have meaningful content.");
            }

            if (checkListItem.activeForm() == null || checkListItem.activeForm().isBlank()) {
                throw new IllegalArgumentException("Checklist item at index " + i + " has empty or blank activeForm. "
                        + "All checklist items must have an activeForm (present continuous tense).");
            }

            if (checkListItem.status() == null) {
                throw new IllegalArgumentException("Checklist item at index " + i
                        + " has null status. Status must be one of: pending, in_progress, completed");
            }
        }

        // Count in_progress checkList after validating all items
        long inProgressCount = items.stream().filter(item -> item.status() == CheckList.Status.in_progress).count();

        if (inProgressCount > 1) {
            throw new IllegalArgumentException("Only ONE checklist item can be in_progress at a time. Found " + inProgressCount
                    + " in_progress checklist items. " + "Please mark the current checklist item as completed before starting a new one.");
        }
    }

    public record CheckList(List<CheckListItem> checkList) {
        public record CheckListItem(String content, Status status, String activeForm) {
        }

        public enum Status {
            pending, in_progress, completed
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private CheckListEventHandler checkListEventHandler = checkList -> logger.debug("Updated checklist: {}", checkList);

        public CheckListTool.Builder agentCheckListEventHandler(CheckListEventHandler checkListEventHandler) {
            this.checkListEventHandler = checkListEventHandler;
            return this;
        }

        public CheckListTool build() {
            return new CheckListTool(this.checkListEventHandler);
        }

    }

}

