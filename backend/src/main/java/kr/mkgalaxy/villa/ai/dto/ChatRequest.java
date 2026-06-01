package kr.mkgalaxy.villa.ai.dto;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 자연어 어시스턴트 요청. message 는 이번 사용자 발화(필수),
 * history 는 이전 대화(선택, 멀티턴 — 서버 무상태라 클라이언트가 전달).
 */
public class ChatRequest {

    @NotBlank(message = "메시지는 필수입니다")
    private String message;

    private List<ChatMessage> history;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<ChatMessage> getHistory() { return history; }
    public void setHistory(List<ChatMessage> history) { this.history = history; }
}
