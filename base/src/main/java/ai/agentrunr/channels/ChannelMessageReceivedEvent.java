package ai.agentrunr.channels;

public class ChannelMessageReceivedEvent {
    private final String channel;
    private final String message;

    public ChannelMessageReceivedEvent(String channel, String message) {
        this.channel = channel;
        this.message = message;
    }

    public String getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }
}