package kr.mkgalaxy.villa;

import kr.mkgalaxy.villa.ai.AiController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 전체 애플리케이션 컨텍스트가 기동되는지(모든 빈 와이어링) 확인하는 스모크 테스트.
 * 신규 ai/ 패키지(컨트롤러·서비스·클라이언트·프로퍼티)가 기존 빈들과 함께 정상 구성되는지 검증한다.
 * 내장 H2(in-memory)로 대체해 실데이터(villa-reservation.mv.db)는 건드리지 않는다.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class VillaReservationApplicationTests {

    @Autowired
    private AiController aiController;

    @Test
    void contextLoads() {
        assertNotNull(aiController, "AI 컨트롤러를 포함한 컨텍스트가 기동되어야 한다");
    }
}
