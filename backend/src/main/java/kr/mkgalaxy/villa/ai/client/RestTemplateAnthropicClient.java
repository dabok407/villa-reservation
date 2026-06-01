package kr.mkgalaxy.villa.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.mkgalaxy.villa.ai.config.AnthropicProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RestTemplate 기반 Anthropic Messages API 클라이언트(외부 SDK 없음).
 * 키가 없으면 호출되지 않는다(서비스가 사전 차단). 응답은 tool_use 루프가 쓰는 형태로 파싱한다.
 */
@Component
public class RestTemplateAnthropicClient implements AnthropicClient {

    private final AnthropicProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public RestTemplateAnthropicClient(AnthropicProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getTimeoutMs());
        factory.setReadTimeout(props.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
        this.mapper = new ObjectMapper();
    }

    @Override
    public AnthropicResult createMessage(String system,
                                         List<Map<String, Object>> messages,
                                         List<Map<String, Object>> tools) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", props.getModel());
        body.put("max_tokens", props.getMaxTokens());
        body.put("system", system);
        body.put("messages", messages);
        body.put("tools", tools);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", props.getApiKey());
        headers.set("anthropic-version", props.getVersion());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(body, headers);
        String url = props.getBaseUrl() + "/v1/messages";

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return parse(response.getBody());
    }

    private AnthropicResult parse(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            String stopReason = root.path("stop_reason").asText(null);

            List<AnthropicResult.ContentBlock> blocks = new ArrayList<AnthropicResult.ContentBlock>();
            JsonNode content = root.path("content");
            if (content.isArray()) {
                for (JsonNode b : content) {
                    String type = b.path("type").asText("");
                    if ("text".equals(type)) {
                        blocks.add(new AnthropicResult.ContentBlock(
                                "text", b.path("text").asText(""), null, null, null));
                    } else if ("tool_use".equals(type)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> input = mapper.convertValue(b.path("input"), Map.class);
                        blocks.add(new AnthropicResult.ContentBlock(
                                "tool_use", null,
                                b.path("id").asText(null),
                                b.path("name").asText(null),
                                input));
                    }
                }
            }
            return new AnthropicResult(stopReason, blocks);
        } catch (Exception e) {
            throw new RuntimeException("Anthropic 응답 파싱 실패", e);
        }
    }
}
