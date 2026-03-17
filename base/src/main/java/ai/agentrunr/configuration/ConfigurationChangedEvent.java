package ai.agentrunr.configuration;

import java.util.Map;

public record ConfigurationChangedEvent(Map<String, Object> allConfig) {

    public Object getConfiguration(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> map = allConfig;
        for (int i = 0; i < keys.length - 1; i++) {
            Object configItem = map.get(keys[i]);
            if (configItem == null) return null;
            else if (configItem instanceof Map<?, ?> nestedMap) {
                map = (Map<String, Object>) nestedMap;
            }
        }
        return map.get(keys[keys.length - 1]);
    }

}
