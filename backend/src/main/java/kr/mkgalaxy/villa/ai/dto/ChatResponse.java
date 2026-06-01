package kr.mkgalaxy.villa.ai.dto;

/**
 * 자연어 어시스턴트 응답.
 */
public class ChatResponse {

    private final String reply;

    public ChatResponse(String reply) {
        this.reply = reply;
    }

    public String getReply() { return reply; }
}
