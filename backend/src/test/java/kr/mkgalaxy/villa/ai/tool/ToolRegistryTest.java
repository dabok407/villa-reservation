package kr.mkgalaxy.villa.ai.tool;

import kr.mkgalaxy.villa.entity.Reservation;
import kr.mkgalaxy.villa.entity.ReservationStatus;
import kr.mkgalaxy.villa.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ToolRegistry 가 도구 스키마를 노출하고, 입력을 받아 ReservationToolService 로 위임해
 * JSON 결과를 만들며, 잘못된 입력은 예외가 아니라 error JSON 으로 방어하는지 검증.
 */
@DataJpaTest
class ToolRegistryTest {

    @Autowired
    private ReservationRepository repo;

    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry(new ReservationToolService(repo));
    }

    private void seed(String name, String in, String out, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setReserverName(name);
        r.setCheckInDate(LocalDate.parse(in));
        r.setCheckOutDate(LocalDate.parse(out));
        r.setAdultCount(2);
        r.setChildCount(0);
        r.setPasswordHash("x");
        r.setStatus(status);
        repo.save(r);
    }

    @Test
    @DisplayName("toolSpecs: 읽기전용 도구 3종 노출")
    void toolSpecsExposesThreeTools() {
        assertEquals(3, registry.toolSpecs().size());
    }

    @Test
    @DisplayName("execute check_availability: 가능 시 available:true 포함")
    void executeCheckAvailability() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("checkInDate", "2026-12-07");
        input.put("checkOutDate", "2026-12-08");
        String json = registry.execute("check_availability", input);
        assertTrue(json.contains("\"available\":true"), json);
    }

    @Test
    @DisplayName("execute list_reservations: 예약자명 포함")
    void executeListReservations() {
        seed("황민국", "2026-12-06", "2026-12-08", ReservationStatus.ACTIVE);
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("year", 2026);
        input.put("month", 12);
        String json = registry.execute("list_reservations", input);
        assertTrue(json.contains("황민국"), json);
    }

    @Test
    @DisplayName("execute 필수 입력 누락: 예외 아닌 error JSON")
    void executeBadInputReturnsErrorJson() {
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("checkOutDate", "2026-12-08"); // checkInDate 누락
        String json = registry.execute("check_availability", input);
        assertTrue(json.contains("error"), json);
    }

    @Test
    @DisplayName("execute 알 수 없는 도구: error JSON")
    void executeUnknownTool() {
        String json = registry.execute("delete_everything", new HashMap<String, Object>());
        assertTrue(json.contains("error"), json);
    }
}
