package ai.agentrunr.tools.brave;

import ai.agentrunr.tools.AutoDiscoveredTool;
import org.springaicommunity.agent.tools.BraveWebSearchTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class BraveWebSearchAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "agent.browser.brave.api-key")
    public AutoDiscoveredTool<BraveWebSearchTool> autoDiscoveredBraveWebSearchTool(@Value("${agent.browser.brave.api-key}") String braveApiKey) {
        return new AutoDiscoveredTool<>(BraveWebSearchTool.builder(braveApiKey).resultCount(15).build());
    }
}
