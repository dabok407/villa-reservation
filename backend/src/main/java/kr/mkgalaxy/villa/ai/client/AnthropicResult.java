package kr.mkgalaxy.villa.ai.client;

import java.util.List;
import java.util.Map;

/**
 * Anthropic 응답을 tool_use 루프가 필요로 하는 형태로 파싱한 결과.
 */
public class AnthropicResult {

    private final String stopReason;
    private final List<ContentBlock> content;

    public AnthropicResult(String stopReason, List<ContentBlock> content) {
        this.stopReason = stopReason;
        this.content = content;
    }

    public String getStopReason() { return stopReason; }
    public List<ContentBlock> getContent() { return content; }

    /** content 안에 tool_use 블록이 하나라도 있으면 true. */
    public boolean hasToolUse() {
        if (content == null) {
            return false;
        }
        for (ContentBlock b : content) {
            if (b.isToolUse()) {
                return true;
            }
        }
        return false;
    }

    /** 응답 콘텐츠 블록(text 또는 tool_use). */
    public static class ContentBlock {

        private final String type;          // "text" | "tool_use"
        private final String text;          // text 블록
        private final String id;            // tool_use 블록 id
        private final String name;          // tool_use 도구명
        private final Map<String, Object> input; // tool_use 입력

        public ContentBlock(String type, String text, String id, String name, Map<String, Object> input) {
            this.type = type;
            this.text = text;
            this.id = id;
            this.name = name;
            this.input = input;
        }

        public boolean isToolUse() { return "tool_use".equals(type); }
        public boolean isText() { return "text".equals(type); }

        public String getType() { return type; }
        public String getText() { return text; }
        public String getId() { return id; }
        public String getName() { return name; }
        public Map<String, Object> getInput() { return input; }
    }
}
