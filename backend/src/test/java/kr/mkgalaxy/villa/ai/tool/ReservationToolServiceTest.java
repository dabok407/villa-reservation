package kr.mkgalaxy.villa.ai.tool;

import kr.mkgalaxy.villa.ai.tool.dto.AvailabilityResult;
import kr.mkgalaxy.villa.ai.tool.dto.ReservationSummary;
import kr.mkgalaxy.villa.entity.Reservation;
import kr.mkgalaxy.villa.entity.ReservationStatus;
import kr.mkgalaxy.villa.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * check_availability 도구의 도메인 정확성 검증.
 * LLM/네트워크 없이 반개구간 충돌 규약과 상태 필터링을 못 박는다.
 * 내장 H2(in-memory)로 동작하므로 실데이터(villa-reservation.mv.db)를 건드리지 않는다.
 */
@DataJpaTest
class ReservationToolServiceTest {

    @Autowired
    private ReservationRepository reservationRepository;

    private ReservationToolService toolService;

    @BeforeEach
    void setUp() {
        toolService = new ReservationToolService(reservationRepository);
    }

    private void seed(String name, String in, String out, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setReserverName(name);
        r.setCheckInDate(LocalDate.parse(in));
        r.setCheckOutDate(LocalDate.parse(out));
        r.setAdultCount(2);
        r.setChildCount(0);
        r.setPasswordHash("x");      // nullable=false 라 더미값 필요
        r.setStatus(status);
        reservationRepository.save(r);
    }

    @Test
    @DisplayName("체크아웃일 == 다음 체크인일은 충돌 아님 → 가능")
    void boundaryCheckoutEqualsCheckinIsAvailable() {
        seed("황대한", "2026-12-05", "2026-12-07", ReservationStatus.ACTIVE);
        // 앞 예약은 12/7 퇴실 → 12/7 입실은 반개구간상 겹치지 않음
        AvailabilityResult result = toolService.checkAvailability(
                LocalDate.parse("2026-12-07"), LocalDate.parse("2026-12-08"));
        assertTrue(result.isAvailable());
        assertTrue(result.getConflicts().isEmpty());
    }

    @Test
    @DisplayName("기간이 겹치면 불가 + 충돌자 이름 반환")
    void overlapIsUnavailableWithConflictName() {
        seed("박정인", "2026-12-06", "2026-12-09", ReservationStatus.ACTIVE);
        AvailabilityResult result = toolService.checkAvailability(
                LocalDate.parse("2026-12-08"), LocalDate.parse("2026-12-10"));
        assertFalse(result.isAvailable());
        assertEquals(1, result.getConflicts().size());
        assertEquals("박정인", result.getConflicts().get(0).getReserverName());
    }

    @Test
    @DisplayName("취소된 예약은 충돌에서 제외")
    void cancelledReservationIsExcluded() {
        seed("황민국", "2026-12-10", "2026-12-12", ReservationStatus.CANCELLED);
        AvailabilityResult result = toolService.checkAvailability(
                LocalDate.parse("2026-12-10"), LocalDate.parse("2026-12-12"));
        assertTrue(result.isAvailable());
    }

    @Test
    @DisplayName("체크아웃된 예약도 충돌에서 제외")
    void checkedOutReservationIsExcluded() {
        seed("배지현", "2026-12-15", "2026-12-17", ReservationStatus.CHECKED_OUT);
        AvailabilityResult result = toolService.checkAvailability(
                LocalDate.parse("2026-12-15"), LocalDate.parse("2026-12-17"));
        assertTrue(result.isAvailable());
    }

    @Test
    @DisplayName("퇴실일이 입실일 이하이면 예외")
    void invalidDateRangeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                toolService.checkAvailability(LocalDate.parse("2026-12-10"), LocalDate.parse("2026-12-10")));
    }

    // ===== 슬라이스 2: list_reservations =====

    @Test
    @DisplayName("list_reservations: 해당 월 ACTIVE 예약만 반환")
    void listReservationsReturnsActiveInMonth() {
        seed("황민국", "2026-12-06", "2026-12-08", ReservationStatus.ACTIVE);
        seed("배지현", "2026-11-10", "2026-11-12", ReservationStatus.ACTIVE); // 다른 달
        List<ReservationSummary> dec = toolService.listReservations(2026, 12);
        assertEquals(1, dec.size());
        assertEquals("황민국", dec.get(0).getReserverName());
    }

    @Test
    @DisplayName("list_reservations: 취소/체크아웃 예약은 제외")
    void listReservationsExcludesNonActive() {
        seed("황대한", "2026-12-06", "2026-12-08", ReservationStatus.CANCELLED);
        seed("박정인", "2026-12-10", "2026-12-12", ReservationStatus.CHECKED_OUT);
        assertTrue(toolService.listReservations(2026, 12).isEmpty());
    }

    @Test
    @DisplayName("list_reservations: 잘못된 월은 예외")
    void listReservationsInvalidMonthThrows() {
        assertThrows(IllegalArgumentException.class, () -> toolService.listReservations(2026, 13));
    }

    // ===== 슬라이스 2: active_today =====

    @Test
    @DisplayName("active_today: 진행 중 예약 반환 (체크아웃 당일도 진행 중)")
    void activeTodayReturnsOngoingIncludingCheckoutDay() {
        seed("황용귀", "2026-12-05", "2026-12-10", ReservationStatus.ACTIVE);
        assertEquals(1, toolService.activeToday(LocalDate.parse("2026-12-07")).size()); // 중간
        assertEquals(1, toolService.activeToday(LocalDate.parse("2026-12-05")).size()); // 체크인 당일
        assertEquals(1, toolService.activeToday(LocalDate.parse("2026-12-10")).size()); // 체크아웃 당일(양 끝 포함)
    }

    @Test
    @DisplayName("active_today: 기준일 범위 밖이면 없음")
    void activeTodayExcludesOutOfRange() {
        seed("김경임", "2026-12-05", "2026-12-10", ReservationStatus.ACTIVE);
        assertTrue(toolService.activeToday(LocalDate.parse("2026-12-04")).isEmpty()); // 체크인 전날
        assertTrue(toolService.activeToday(LocalDate.parse("2026-12-11")).isEmpty()); // 체크아웃 다음날
    }

    @Test
    @DisplayName("active_today: null 기준일은 예외")
    void activeTodayNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> toolService.activeToday(null));
    }
}
