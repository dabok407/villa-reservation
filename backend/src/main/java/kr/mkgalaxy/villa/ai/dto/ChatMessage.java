package kr.mkgalaxy.villa.ai.dto;

/**
 * 멀티턴 대화의 한 턴. 서버는 무상태이므로 클라이언트가 이전 대화를 history 로 함께 보낸다.
 * role 은 "user" 또는 "assistant".
 */
public class ChatMessage {

    private String role;
    private String content;

    public ChatMessage() {
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
