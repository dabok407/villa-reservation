package kr.mkgalaxy.villa.ai.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * system 프롬프트에 검증의 핵심 사실(오늘 날짜·가구 매핑·충돌 규약·환각 차단)이 박혀 있는지 확인.
 */
class SystemPromptBuilderTest {

    @Test
    @DisplayName("프롬프트에 오늘 날짜·가구 매핑·충돌 규약·환각 차단이 포함")
    void buildContainsKeyFacts() {
        String p = new SystemPromptBuilder().build(LocalDate.of(2026, 6, 1));
        assertTrue(p.contains("2026-06-01"), "오늘 날짜 주입");
        assertTrue(p.contains("황대한") && p.contains("박정인"), "형네 매핑");
        assertTrue(p.contains("황민국") && p.contains("배지현"), "본인 매핑");
        assertTrue(p.contains("황용귀") && p.contains("김경임"), "부모님 매핑");
        assertTrue(p.contains("반개구간"), "충돌 규약");
        assertTrue(p.contains("ACTIVE") || p.contains("진행중"), "상태 필터");
        assertTrue(p.contains("환각"), "환각 차단");
    }
}
