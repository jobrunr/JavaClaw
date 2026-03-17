package ai.agentrunr.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

@Component
public class DefaultAgent implements Agent {

    private final ChatClient chatClient;

    public DefaultAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String respondTo(String conversationId, String question) {
        return chatClient
                .prompt(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    @Override
    public <T> T prompt(String conversationId, String input, Class<T> result) {
        return chatClient
                .prompt(input)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(result);
    }
}
