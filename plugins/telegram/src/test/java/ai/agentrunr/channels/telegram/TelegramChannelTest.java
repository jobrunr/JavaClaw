package ai.agentrunr.channels.telegram;

import ai.agentrunr.agent.Agent;
import ai.agentrunr.channels.ChannelRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramChannelTest {

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private Agent agent;

    @Test
    void ignoresMessagesFromUnauthorizedUser() {
        TelegramChannel telegramChannel = new TelegramChannel("token", "allowed_user", telegramClient, agent, new ChannelRegistry());

        telegramChannel.consume(unauthorizedUpdateFrom("other_user"));

        verify(agent, never()).respondTo(anyString());
        verifyNoInteractions(telegramClient);
    }

    @Test
    void respondsOnlyToConfiguredUsername() throws TelegramApiException {
        TelegramChannel telegramChannel = new TelegramChannel("token", "@Allowed_User", telegramClient, agent, new ChannelRegistry());
        when(agent.respondTo("hello")).thenReturn("hi");

        telegramChannel.consume(updateFrom("allowed_user", "hello", 42L));

        verify(agent).respondTo("hello");
        verify(telegramClient).execute(argThat((SendMessage message) ->
                "42".equals(message.getChatId()) && "hi".equals(message.getText())));
    }

    private Update updateFrom(String username, String text, long chatId) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        User user = mock(User.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getFrom()).thenReturn(user);
        when(user.getUserName()).thenReturn(username);

        return update;
    }

    private Update unauthorizedUpdateFrom(String username) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        User user = mock(User.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getFrom()).thenReturn(user);
        when(user.getUserName()).thenReturn(username);

        return update;
    }
}