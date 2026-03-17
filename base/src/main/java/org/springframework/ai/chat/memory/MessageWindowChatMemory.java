package org.springframework.ai.chat.memory;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

/**
 * A copy of Spring's MessageWindowChatMemory that keeps the order of the messages
 */
public class MessageWindowChatMemory implements ChatMemory {
    private static final int DEFAULT_MAX_MESSAGES = 20;

    private final ChatMemoryRepository chatMemoryRepository;

    private final int maxMessages;

    private MessageWindowChatMemory(ChatMemoryRepository chatMemoryRepository, int maxMessages) {
        Assert.notNull(chatMemoryRepository, "chatMemoryRepository cannot be null");
        Assert.isTrue(maxMessages > 0, "maxMessages must be greater than 0");
        this.chatMemoryRepository = chatMemoryRepository;
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");

        List<Message> memoryMessages = this.chatMemoryRepository.findByConversationId(conversationId);
        List<Message> processedMessages = process(memoryMessages, messages);
        this.chatMemoryRepository.saveAll(conversationId, processedMessages);
    }

    @Override
    public List<Message> get(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        return this.chatMemoryRepository.findByConversationId(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        this.chatMemoryRepository.deleteByConversationId(conversationId);
    }

    private List<Message> process(List<Message> memoryMessages, List<Message> newMessages) {
        List<Message> processedMessages = new ArrayList<>();

        SequencedSet<Message> memoryMessagesSet = new LinkedHashSet<>(memoryMessages);
        boolean hasNewSystemMessage = newMessages.stream()
                .filter(SystemMessage.class::isInstance)
                .anyMatch(message -> !memoryMessagesSet.contains(message));

        memoryMessages.stream()
                .filter(message -> !(hasNewSystemMessage && message instanceof SystemMessage))
                .forEach(processedMessages::add);

        processedMessages.addAll(newMessages);

        if (processedMessages.size() <= this.maxMessages) {
            return processedMessages;
        }

        int messagesToRemove = processedMessages.size() - this.maxMessages;

        List<Message> trimmedMessages = new ArrayList<>();
        int removed = 0;
        for (Message message : processedMessages) {
            if (message instanceof SystemMessage || removed >= messagesToRemove) {
                trimmedMessages.add(message);
            } else {
                removed++;
            }
        }

        return trimmedMessages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();

        private int maxMessages = DEFAULT_MAX_MESSAGES;

        private Builder() {
        }

        public Builder chatMemoryRepository(ChatMemoryRepository chatMemoryRepository) {
            this.chatMemoryRepository = chatMemoryRepository;
            return this;
        }

        public Builder maxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        public MessageWindowChatMemory build() {
            return new MessageWindowChatMemory(this.chatMemoryRepository, this.maxMessages);
        }

    }
}
