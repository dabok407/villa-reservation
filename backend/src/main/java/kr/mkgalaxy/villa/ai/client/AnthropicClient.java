package kr.mkgalaxy.villa.ai.client;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API 호출 추상화. 네트워크 호출을 이 인터페이스 뒤로 격리해
 * AiAssistantService 의 tool_use 루프를 LLM/네트워크 없이 목(mock)으로 단위 테스트할 수 있게 한다.
 */
public interface AnthropicClient {

    /**
     * @param system   system 프롬프트
     * @param messages Anthropic 메시지 배열(역할/콘텐츠 블록)
     * @param tools    도구 스키마 배열
     * @return 파싱된 응답(stop_reason + content 블록)
     */
    AnthropicResult createMessage(String system,
                                  List<Map<String, Object>> messages,
                                  List<Map<String, Object>> tools);
}
