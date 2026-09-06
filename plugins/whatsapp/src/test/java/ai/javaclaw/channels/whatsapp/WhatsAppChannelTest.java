package ai.javaclaw.channels.whatsapp;

import ai.javaclaw.agent.Agent;
import ai.javaclaw.channels.ChannelRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppChannelTest {

    private static final String ALLOWED_JID = "1234567890@s.whatsapp.net";

    @Mock
    private WhatsApp whatsApp;

    @Mock
    private Agent agent;

    private final ChannelRegistry channelRegistry = new ChannelRegistry();

    private WhatsAppChannel channel;

    @BeforeEach
    void setUp() {
        channel = new WhatsAppChannel(whatsApp, channelRegistry, agent);
    }

    @Test
    void startRegistersItselfAsReceiverAndChannel() {
        when(whatsApp.start()).thenReturn(true);

        channel.start();

        verify(whatsApp).registerMessageReceiver(any());
        assertThat(channelRegistry.getLatestChannel()).isSameAs(channel);
    }

    @Test
    void doesNotRegisterWhenWhatsAppCannotStart() {
        when(whatsApp.start()).thenReturn(false);

        channel.start();

        verify(whatsApp, never()).registerMessageReceiver(any());
        assertThat(channelRegistry.getLatestChannel()).isNull();
    }

    @Test
    void stopUnregistersChannelFromRegistry() {
        when(whatsApp.start()).thenReturn(true);
        channel.start();

        channel.stop();

        verify(whatsApp).stop();
        assertThat(channelRegistry.getLatestChannel()).isNull();
    }

    @Test
    void sendMessageGoesToWhatsApp() {
        channel.sendMessage("hi there");

        verify(whatsApp).sendMessage("hi there");
    }

    @Test
    void answersWhatTheAgentReplies() {
        when(agent.respondTo(ALLOWED_JID, "hello")).thenReturn("hi there");

        channel.onMessage(ALLOWED_JID, "hello");

        verify(whatsApp).sendMessage("hi there");
    }

    @Test
    void publishesMessageReceivedEventForTheAllowedChat() {
        when(whatsApp.start()).thenReturn(true);
        channel.start();
        when(agent.respondTo(ALLOWED_JID, "hello")).thenReturn("hi there");

        channel.onMessage(ALLOWED_JID, "hello");

        assertThat(channelRegistry.getLatestChannel()).isSameAs(channel);
    }

    @Test
    void swallowsAgentFailures() {
        when(agent.respondTo(ALLOWED_JID, "hello")).thenThrow(new RuntimeException("boom"));

        channel.onMessage(ALLOWED_JID, "hello");

        verify(whatsApp, never()).sendMessage(anyString());
    }
}
