package ai.agentrunr.agent;

import org.springframework.ai.chat.memory.ChatMemory;

public interface Agent {

    default String respondTo(String question) {
        return respondTo(ChatMemory.CONVERSATION_ID, question);
    }

    String respondTo(String conversationId, String question);

    <T> T prompt(String conversationId, String input, Class<T> result);

}
