package ai.javaclaw.channels.telegram;

import ai.javaclaw.agent.Agent;
import ai.javaclaw.channels.Channel;
import ai.javaclaw.channels.ChannelMessageReceivedEvent;
import ai.javaclaw.channels.ChannelRegistry;
import ai.javaclaw.speech.SpeechToTextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static java.util.Optional.ofNullable;

public class TelegramChannel implements Channel, SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelegramChannel.class);
    private final String botToken;
    private final String allowedUsername;
    private final TelegramClient telegramClient;
    private final Agent agent;
    private final ChannelRegistry channelRegistry;
    private final SpeechToTextService speechToTextService;
    private Long chatId;
    private Integer messageThreadId;

    public TelegramChannel(String botToken, String allowedUsername, Agent agent, ChannelRegistry channelRegistry, SpeechToTextService speechToTextService) {
        this(botToken, allowedUsername, new OkHttpTelegramClient(botToken), agent, channelRegistry, speechToTextService);
    }

    TelegramChannel(String botToken, String allowedUsername, TelegramClient telegramClient, Agent agent, ChannelRegistry channelRegistry, SpeechToTextService speechToTextService) {
        this.botToken = botToken;
        this.allowedUsername = normalizeUsername(allowedUsername);
        this.telegramClient = telegramClient;
        this.agent = agent;
        this.channelRegistry = channelRegistry;
        this.speechToTextService = speechToTextService;
        channelRegistry.registerChannel(this);
        log.info("Started Telegram integration");
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) return;

        Message requestMessage = update.getMessage();
        String userName = requestMessage.getFrom() == null ? null : requestMessage.getFrom().getUserName();
        if (!isAllowedUser(userName)) {
            log.warn("Ignoring Telegram message from unauthorized username '{}'", userName);
            sendMessage("I'm sorry, I don't accept instructions from you.");
            return;
        }

        if (requestMessage.hasText()) {
            // Check text message
            String messageText = requestMessage.getText();
            this.chatId = requestMessage.getChatId();
            this.messageThreadId = requestMessage.getMessageThreadId();
            channelRegistry.publishMessageReceivedEvent(new TelegramChannelMessageReceivedEvent(getName(), messageText, chatId, messageThreadId));
            String response = agent.respondTo(getConversationId(chatId, messageThreadId), messageText);
            sendMessage(chatId, messageThreadId, response);
        } else if (requestMessage.hasVoice()) {
            // Check voice message & download it
            log.info("Voice message received");
            try (InputStream voiceStream = downloadVoice(requestMessage)) {
                String transcribedText = speechToTextService.transcribe(voiceStream);
                log.info("Voice message transcribed: {}", transcribedText);
                this.chatId = requestMessage.getChatId();
                this.messageThreadId = requestMessage.getMessageThreadId();
                channelRegistry.publishMessageReceivedEvent(new TelegramChannelMessageReceivedEvent(getName(), transcribedText, chatId, messageThreadId));
                String response = agent.respondTo(getConversationId(chatId, messageThreadId), transcribedText);
                sendMessage(chatId, messageThreadId, response);
            } catch (IOException | TelegramApiException e) {
                log.error("Failed to process voice message", e);
            }
        }
    }

    @Override
    public void sendMessage(String message) {
        if (chatId == null) {
            log.error("No known chatId, cannot send message '{}'", message);
            return;
        }
        sendMessage(chatId, null, message);
    }

    public void sendMessage(long chatId, Integer messageThreadId, String message) {
        SendMessage messageMessage = SendMessage.builder()
                .chatId(chatId)
                .messageThreadId(messageThreadId)
                .text(message)
                .build();
        try {
            telegramClient.execute(messageMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream downloadVoice(Message message) throws TelegramApiException, IOException {
        String fileId = message.getVoice().getFileId();
        GetFile getFile = new GetFile(fileId);
        String filePath = telegramClient.execute(getFile).getFilePath();
        String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
        return URI.create(fileUrl).toURL().openStream();
    }

    private boolean isAllowedUser(String userName) {
        String normalizedUserName = normalizeUsername(userName);
        return normalizedUserName != null && normalizedUserName.equalsIgnoreCase(allowedUsername);
    }

    private static String normalizeUsername(String userName) {
        if (userName == null) {
            return null;
        }

        String normalizedUserName = userName.trim();
        if (normalizedUserName.startsWith("@")) {
            normalizedUserName = normalizedUserName.substring(1);
        }

        return normalizedUserName.isBlank() ? null : normalizedUserName;
    }

    private String getConversationId(Long chatId, Integer messageThreadId) {
        return "telegram-" + chatId + ofNullable(messageThreadId).map(i -> "-" + i).orElse("");
    }

    static class TelegramChannelMessageReceivedEvent extends ChannelMessageReceivedEvent {

        private final long chatId;
        private final Integer messageThreadId;

        public TelegramChannelMessageReceivedEvent(String channel, String message, long chatId, Integer messageThreadId) {
            super(channel, message);
            this.chatId = chatId;
            this.messageThreadId = messageThreadId;
        }

        public long getChatId() {
            return chatId;
        }

        public Integer getMessageThreadId() {
            return messageThreadId;
        }
    }
}
