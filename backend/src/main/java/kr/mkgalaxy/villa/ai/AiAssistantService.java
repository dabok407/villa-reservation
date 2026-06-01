package kr.mkgalaxy.villa.ai;

import kr.mkgalaxy.villa.ai.client.AnthropicClient;
import kr.mkgalaxy.villa.ai.client.AnthropicResult;
import kr.mkgalaxy.villa.ai.config.AnthropicProperties;
import kr.mkgalaxy.villa.ai.dto.ChatMessage;
import kr.mkgalaxy.villa.ai.prompt.SystemPromptBuilder;
import kr.mkgalaxy.villa.ai.tool.ToolRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 자연어 어시스턴트의 오케스트레이터. Claude Messages API + function calling 의 tool_use 루프를 돈다.
 *
 * <p>흐름: system 프롬프트 + 사용자 메시지 → Claude → stop_reason=tool_use 면 읽기전용 도구 실행 →
 * tool_result 를 messages 에 append 후 재호출 → end_turn 이면 자연어 응답 반환.
 * 루프는 maxToolLoops 로 상한을 두어 무한루프·비용 폭주를 막는다. 외부 호출 실패는 사용자 친화 fallback 으로.
 *
 * <p>멀티턴은 서버 무상태 — 클라이언트가 history 로 이전 대화를 전달한다.
 */
@Service
public class AiAssistantService {

    private final AnthropicClient client;
    private final ToolRegistry toolRegistry;
    private final SystemPromptBuilder promptBuilder;
    private final AnthropicProperties props;

    public AiAssistantService(AnthropicClient client,
                              ToolRegistry toolRegistry,
                              SystemPromptBuilder promptBuilder,
                              AnthropicProperties props) {
        this.client = client;
        this.toolRegistry = toolRegistry;
        this.promptBuilder = promptBuilder;
        this.props = props;
    }

    public String chat(String message, List<ChatMessage> history) {
        if (!props.hasApiKey()) {
            return "AI 어시스턴트가 아직 설정되지 않았습니다(서버에 ANTHROPIC_API_KEY 미설정).";
        }

        String system = promptBuilder.build(LocalDate.now());
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        if (history != null) {
            for (ChatMessage m : history) {
                if (m != null && m.getRole() != null && m.getContent() != null) {
                    messages.add(textMessage(m.getRole(), m.getContent()));
                }
            }
        }
        messages.add(textMessage("user", message));

        List<Map<String, Object>> tools = toolRegistry.toolSpecs();

        for (int loop = 0; loop < props.getMaxToolLoops(); loop++) {
            AnthropicResult result;
            try {
                result = client.createMessage(system, messages, tools);
            } catch (Exception e) {
                return "AI 응답 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
            }

            if (result.hasToolUse()) {
                // assistant 의 tool_use 콘텐츠를 그대로 echo 해서 다음 요청에 포함
                messages.add(assistantMessage(result.getContent()));
                // 각 tool_use 실행 → tool_result 블록 모음
                List<Map<String, Object>> toolResults = new ArrayList<Map<String, Object>>();
                for (AnthropicResult.ContentBlock b : result.getContent()) {
                    if (b.isToolUse()) {
                        String output = toolRegistry.execute(b.getName(), b.getInput());
                        toolResults.add(toolResultBlock(b.getId(), output));
                    }
                }
                messages.add(blocksMessage("user", toolResults));
            } else {
                return joinText(result.getContent());
            }
        }
        return "요청이 복잡해 처리를 완료하지 못했습니다. 더 구체적으로 질문해 주세요.";
    }

    // ===== 메시지 빌더 =====

    private Map<String, Object> textMessage(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private Map<String, Object> blocksMessage(String role, List<Map<String, Object>> blocks) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("role", role);
        m.put("content", blocks);
        return m;
    }

    /** Claude 가 돌려준 콘텐츠 블록을 Anthropic 메시지 형식으로 재구성(assistant 턴 echo). */
    private Map<String, Object> assistantMessage(List<AnthropicResult.ContentBlock> blocks) {
        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        for (AnthropicResult.ContentBlock b : blocks) {
            Map<String, Object> blk = new LinkedHashMap<String, Object>();
            if (b.isToolUse()) {
                blk.put("type", "tool_use");
                blk.put("id", b.getId());
                blk.put("name", b.getName());
                blk.put("input", b.getInput() == null ? new LinkedHashMap<String, Object>() : b.getInput());
            } else {
                blk.put("type", "text");
                blk.put("text", b.getText() == null ? "" : b.getText());
            }
            content.add(blk);
        }
        return blocksMessage("assistant", content);
    }

    private Map<String, Object> toolResultBlock(String toolUseId, String output) {
        Map<String, Object> blk = new LinkedHashMap<String, Object>();
        blk.put("type", "tool_result");
        blk.put("tool_use_id", toolUseId);
        blk.put("content", output);
        return blk;
    }

    private String joinText(List<AnthropicResult.ContentBlock> blocks) {
        StringBuilder sb = new StringBuilder();
        if (blocks != null) {
            for (AnthropicResult.ContentBlock b : blocks) {
                if (b.isText() && b.getText() != null) {
                    sb.append(b.getText());
                }
            }
        }
        String text = sb.toString().trim();
        return text.isEmpty() ? "응답을 생성하지 못했습니다." : text;
    }
}
