package ai.agentrunr.channels;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChannelRegistry {

    private final Map<String, Channel> channels;
    private final AtomicReference<ChannelMessageReceivedEvent> lastChannelMessage;
    private final String defaultChannelName;

    public ChannelRegistry() {
        this("TelegramChannel");
    }

    public ChannelRegistry(String defaultChannelName) {
        this.channels = new HashMap<>();
        this.lastChannelMessage = new AtomicReference<>();
        this.defaultChannelName = defaultChannelName;
    }

    public void registerChannel(Channel channel) {
        channels.put(channel.getName(), channel);
    }

    public void unregisterChannel(Channel channel) {
        channels.remove(channel.getName());
    }

    public Channel getLatestChannel() {
        if (lastChannelMessage.get() != null) {
            return channels.get(lastChannelMessage.get().getChannel());
        }
        return channels.get(defaultChannelName);
    }

    public void publishMessageReceivedEvent(ChannelMessageReceivedEvent event) {
        lastChannelMessage.set(event);
    }
}
