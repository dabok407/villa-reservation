package kr.mkgalaxy.villa.ai.tool;

import kr.mkgalaxy.villa.ai.tool.dto.AvailabilityResult;
import kr.mkgalaxy.villa.ai.tool.dto.ConflictInfo;
import kr.mkgalaxy.villa.ai.tool.dto.ReservationSummary;
import kr.mkgalaxy.villa.entity.Reservation;
import kr.mkgalaxy.villa.entity.ReservationStatus;
import kr.mkgalaxy.villa.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 자연어 예약 어시스턴트의 <b>읽기 전용</b> 도구 모음.
 *
 * <p>핵심 원칙: 예약 충돌 검증식을 복제하지 않는다. 기존
 * {@link ReservationRepository#findByStatusAndDateRange}(= ACTIVE 예약 중
 * {@code checkInDate < :endDate AND checkOutDate > :startDate} 로 겹치는 것)을
 * 그대로 재사용한다. 따라서 반개구간 규약(<b>체크아웃일 == 다음 체크인일은 충돌 아님</b>)이
 * 단일 진실 원천에서 자동 보장된다.
 *
 * <p>쓰기(예약 생성/수정/취소) 도구는 의도적으로 제공하지 않는다(MVP 사이드이펙트 0).
 */
@Service
@Transactional(readOnly = true)
public class ReservationToolService {

    private final ReservationRepository reservationRepository;

    public ReservationToolService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * 주어진 기간 [checkInDate, checkOutDate) 의 예약 가능 여부와 충돌 예약을 반환한다.
     *
     * @throws IllegalArgumentException 날짜가 null 이거나 퇴실일이 입실일 이후가 아닐 때
     */
    public AvailabilityResult checkAvailability(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("입실일과 퇴실일은 필수입니다");
        }
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("퇴실일은 입실일 이후여야 합니다");
        }

        List<Reservation> overlaps = reservationRepository.findByStatusAndDateRange(
                ReservationStatus.ACTIVE, checkInDate, checkOutDate);

        List<ConflictInfo> conflicts = overlaps.stream()
                .map(r -> new ConflictInfo(r.getReserverName(), r.getCheckInDate(), r.getCheckOutDate()))
                .collect(Collectors.toList());

        return new AvailabilityResult(conflicts.isEmpty(), conflicts);
    }

    /**
     * 특정 연/월의 ACTIVE 예약 목록을 반환한다(캘린더 조회와 동일 기준).
     * 기존 {@link ReservationRepository#findByStatusAndDateRange}(월 구간과 겹치는 ACTIVE)을 재사용한다.
     *
     * @throws IllegalArgumentException month 가 1~12 범위를 벗어날 때
     */
    public List<ReservationSummary> listReservations(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("월은 1~12 사이여야 합니다");
        }
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);
        return reservationRepository
                .findByStatusAndDateRange(ReservationStatus.ACTIVE, startDate, endDate)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * 기준일(today) 현재 진행 중인 ACTIVE 예약을 반환한다(체크인일 ≤ today ≤ 체크아웃일, 양 끝 포함).
     * 기존 Repository 메서드를 재사용한다. today 를 인자로 받아 테스트 결정성을 보장한다.
     *
     * @throws IllegalArgumentException today 가 null 일 때
     */
    public List<ReservationSummary> activeToday(LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("기준 날짜는 필수입니다");
        }
        return reservationRepository
                .findByStatusAndCheckInDateLessThanEqualAndCheckOutDateGreaterThanEqual(
                        ReservationStatus.ACTIVE, today, today)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private ReservationSummary toSummary(Reservation r) {
        return new ReservationSummary(r.getReserverName(), r.getCheckInDate(), r.getCheckOutDate(),
                r.getAdultCount(), r.getChildCount());
    }
}
