package ai.agentrunr.channels.telegram;


import ai.agentrunr.agent.Agent;
import ai.agentrunr.channels.ChannelRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class TelegramChannelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TelegramChannelRegistrar telegramChannelRegistrar(ConfigurableApplicationContext context, Agent agent, ChannelRegistry channelRegistry) {
        return new TelegramChannelRegistrar(context, agent, channelRegistry);
    }
}
