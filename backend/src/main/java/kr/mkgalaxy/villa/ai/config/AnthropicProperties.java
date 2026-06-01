package kr.mkgalaxy.villa.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Anthropic Claude API 연계 설정. application.yml 의 anthropic.* 를 바인딩한다.
 * API 키는 코드/yml 평문이 아니라 환경변수(${ANTHROPIC_API_KEY:})로 주입한다.
 */
@Component
@ConfigurationProperties(prefix = "anthropic")
public class AnthropicProperties {

    private String apiKey = "";
    private String model = "claude-opus-4-8";
    private String baseUrl = "https://api.anthropic.com";
    private String version = "2023-06-01";
    private int maxTokens = 1024;
    private int maxToolLoops = 5;
    private int timeoutMs = 30000;

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int getMaxToolLoops() { return maxToolLoops; }
    public void setMaxToolLoops(int maxToolLoops) { this.maxToolLoops = maxToolLoops; }

    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}
