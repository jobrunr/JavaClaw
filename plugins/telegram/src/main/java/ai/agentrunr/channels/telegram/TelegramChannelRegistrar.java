package ai.agentrunr.channels.telegram;

import ai.agentrunr.agent.Agent;
import ai.agentrunr.channels.ChannelRegistry;
import ai.agentrunr.configuration.ConfigurationChangedEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;

import static java.util.Optional.ofNullable;

public class TelegramChannelRegistrar {

    private static final Logger log = LoggerFactory.getLogger(TelegramChannelRegistrar.class);
    private final ConfigurableApplicationContext context;
    private final Agent agent;
    private final ChannelRegistry channelRegistry;

    public TelegramChannelRegistrar(ConfigurableApplicationContext context, Agent agent, ChannelRegistry channelRegistry) {
        this.context = context;
        this.agent = agent;
        this.channelRegistry = channelRegistry;
    }

    @EventListener
    public void on(ConfigurationChangedEvent configurationChangedEvent) {
        String token = ofNullable(configurationChangedEvent.getConfiguration("agent.channels.telegram.token")).map(Object::toString).orElse(null);
        String username = ofNullable(configurationChangedEvent.getConfiguration("agent.channels.telegram.username")).map(Object::toString).orElse(null);
        registerTelegramChannelIfPossible(token, username);
    }

    @PostConstruct
    public void registerTelegramChannelIfPossible() {
        ConfigurableEnvironment env = context.getEnvironment();
        String token = env.getProperty("agent.channels.telegram.token");
        String username = env.getProperty("agent.channels.telegram.username");
        registerTelegramChannelIfPossible(token, username);
    }

    private void registerTelegramChannelIfPossible(String token, String username) {
        if (token == null || username == null) {
            log.debug("Telegram channel can not be initialized due to missing bot token and allowed username");
            return;
        } else if (context.containsBean("telegramChannel")) {
            log.trace("Telegram channel is already intialized");
            return; // already registered
        }

        // Manually instantiate and register — bypasses @ConditionalOnProperty entirely
        ConfigurableListableBeanFactory factory = context.getBeanFactory();
        TelegramChannel channel = new TelegramChannel(token, username, agent, channelRegistry);
        factory.registerSingleton("telegramChannel", channel);
    }
}
