package kr.mkgalaxy.villa.ai;

import kr.mkgalaxy.villa.ai.client.AnthropicClient;
import kr.mkgalaxy.villa.ai.client.AnthropicResult;
import kr.mkgalaxy.villa.ai.config.AnthropicProperties;
import kr.mkgalaxy.villa.ai.prompt.SystemPromptBuilder;
import kr.mkgalaxy.villa.ai.tool.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * tool_use 루프 오케스트레이션을 LLM/네트워크 없이 목(mock)으로 검증.
 * AnthropicClient(외부 호출)와 ToolRegistry(DB)를 목으로 대체해 루프 로직만 격리 테스트한다.
 */
class AiAssistantServiceTest {

    private AnthropicClient client;
    private ToolRegistry toolRegistry;
    private AnthropicProperties props;
    private AiAssistantService service;

    @BeforeEach
    void setUp() {
        client = mock(AnthropicClient.class);
        toolRegistry = mock(ToolRegistry.class);
        props = new AnthropicProperties();
        props.setApiKey("sk-ant-test");
        props.setMaxToolLoops(5);
        when(toolRegistry.toolSpecs()).thenReturn(new ArrayList<Map<String, Object>>());
        service = new AiAssistantService(client, toolRegistry, new SystemPromptBuilder(), props);
    }

    private AnthropicResult.ContentBlock text(String t) {
        return new AnthropicResult.ContentBlock("text", t, null, null, null);
    }

    private AnthropicResult.ContentBlock toolUse(String id, String name, Map<String, Object> input) {
        return new AnthropicResult.ContentBlock("tool_use", null, id, name, input);
    }

    @Test
    @DisplayName("tool_use → 도구 실행 → end_turn 최종 텍스트 반환")
    void toolUseLoopExecutesToolAndReturnsText() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("checkInDate", "2026-12-07");
        input.put("checkOutDate", "2026-12-08");
        AnthropicResult first = new AnthropicResult("tool_use",
                Arrays.asList(toolUse("t1", "check_availability", input)));
        AnthropicResult second = new AnthropicResult("end_turn",
                Arrays.asList(text("12/7-12/8 예약 가능합니다.")));
        when(client.createMessage(any(), any(), any())).thenReturn(first, second);
        when(toolRegistry.execute(eq("check_availability"), any())).thenReturn("{\"available\":true}");

        String reply = service.chat("12월 7일부터 1박 가능?", null);

        assertEquals("12/7-12/8 예약 가능합니다.", reply);
        verify(toolRegistry, times(1)).execute(eq("check_availability"), any());
        verify(client, times(2)).createMessage(any(), any(), any());
    }

    @Test
    @DisplayName("도구 없이 end_turn 이면 바로 텍스트 반환")
    void directEndTurnReturnsText() {
        when(client.createMessage(any(), any(), any()))
                .thenReturn(new AnthropicResult("end_turn", Arrays.asList(text("안녕하세요!"))));
        String reply = service.chat("안녕", null);
        assertEquals("안녕하세요!", reply);
        verify(toolRegistry, never()).execute(any(), any());
    }

    @Test
    @DisplayName("tool_use 무한 반복 시 루프 상한에서 fallback")
    void infiniteToolUseStopsAtMaxLoops() {
        props.setMaxToolLoops(2);
        AnthropicResult loopResult = new AnthropicResult("tool_use",
                Arrays.asList(toolUse("t1", "active_today", new HashMap<String, Object>())));
        when(client.createMessage(any(), any(), any())).thenReturn(loopResult);
        when(toolRegistry.execute(any(), any())).thenReturn("{}");

        String reply = service.chat("계속", null);

        assertTrue(reply.contains("처리를 완료하지 못했"));
        verify(toolRegistry, times(2)).execute(any(), any());
    }

    @Test
    @DisplayName("API 키 없으면 호출 없이 안내 메시지")
    void noApiKeyReturnsNotice() {
        props.setApiKey("");
        String reply = service.chat("비어?", null);
        assertTrue(reply.contains("설정되지 않았"));
        verify(client, never()).createMessage(any(), any(), any());
    }

    @Test
    @DisplayName("외부 호출 예외 시 사용자 친화 fallback")
    void clientErrorReturnsFallback() {
        when(client.createMessage(any(), any(), any())).thenThrow(new RuntimeException("network"));
        String reply = service.chat("비어?", null);
        assertTrue(reply.contains("오류가 발생"));
    }
}
