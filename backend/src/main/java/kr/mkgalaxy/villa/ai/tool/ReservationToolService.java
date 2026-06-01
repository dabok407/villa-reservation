package kr.mkgalaxy.villa.ai.tool;

import kr.mkgalaxy.villa.ai.tool.dto.AvailabilityResult;
import kr.mkgalaxy.villa.ai.tool.dto.ConflictInfo;
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
}
