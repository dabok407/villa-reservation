package kr.mkgalaxy.villa.ai;

import kr.mkgalaxy.villa.ai.dto.ChatRequest;
import kr.mkgalaxy.villa.ai.dto.ChatResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 자연어 예약 어시스턴트 공개 엔드포인트.
 * 전체 경로: {@code POST /villa/api/ai/chat} (운영: https://mkgalaxy.kr/villa/api/ai/chat)
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAssistantService assistantService;

    public AiController(AiAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String reply = assistantService.chat(request.getMessage(), request.getHistory());
        return new ChatResponse(reply);
    }
}
