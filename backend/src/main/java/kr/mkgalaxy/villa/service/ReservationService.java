package kr.mkgalaxy.villa.service;

import kr.mkgalaxy.villa.dto.*;
import kr.mkgalaxy.villa.entity.CheckoutMemo;
import kr.mkgalaxy.villa.entity.Reservation;
import kr.mkgalaxy.villa.entity.ReservationStatus;
import kr.mkgalaxy.villa.exception.ReservationConflictException;
import kr.mkgalaxy.villa.repository.CheckoutMemoRepository;
import kr.mkgalaxy.villa.repository.ReservationRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CheckoutMemoRepository checkoutMemoRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public ReservationService(ReservationRepository reservationRepository,
                              CheckoutMemoRepository checkoutMemoRepository) {
        this.reservationRepository = reservationRepository;
        this.checkoutMemoRepository = checkoutMemoRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public List<ReservationResponse> getReservationsByMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);

        return reservationRepository
                .findByStatusAndDateRange(ReservationStatus.ACTIVE, startDate, endDate)
                .stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    public ReservationResponse getReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다"));
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        validateDates(request.getCheckInDate(), request.getCheckOutDate());

        long conflicts = reservationRepository.countConflictingNew(
                request.getCheckInDate(), request.getCheckOutDate());
        if (conflicts > 0) {
            throw new ReservationConflictException("해당 기간에 이미 예약이 있습니다");
        }

        Reservation reservation = new Reservation();
        reservation.setReserverName(request.getReserverName());
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setAdultCount(request.getAdultCount());
        reservation.setChildCount(request.getChildCount());
        reservation.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        reservation.setDescription(request.getDescription());
        reservation.setStatus(ReservationStatus.ACTIVE);

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    public boolean verifyPassword(Long id, String password) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다"));
        return passwordEncoder.matches(password, reservation.getPasswordHash());
    }

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationUpdateRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다"));

        if (!passwordEncoder.matches(request.getPassword(), reservation.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        validateDates(request.getCheckInDate(), request.getCheckOutDate());

        long conflicts = reservationRepository.countConflicting(
                id, request.getCheckInDate(), request.getCheckOutDate());
        if (conflicts > 0) {
            throw new ReservationConflictException("해당 기간에 이미 예약이 있습니다");
        }

        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setAdultCount(request.getAdultCount());
        reservation.setChildCount(request.getChildCount());
        reservation.setDescription(request.getDescription());

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public void checkout(Long id, CheckoutRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다"));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalArgumentException("이미 체크아웃된 예약입니다");
        }

        if (!passwordEncoder.matches(request.getPassword(), reservation.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        LocalDate today = LocalDate.now();

        if (today.isBefore(reservation.getCheckInDate()) || today.isAfter(reservation.getCheckOutDate())) {
            throw new IllegalArgumentException("체크아웃은 예약 기간 내에서만 가능합니다");
        }

        // 퇴실일 당일이 아니면 퇴실일을 오늘로 변경
        if (!today.isEqual(reservation.getCheckOutDate())) {
            reservation.setCheckOutDate(today);
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.save(reservation);

        // 메모 저장
        if (request.getMemo() != null && !request.getMemo().trim().isEmpty()) {
            CheckoutMemo memo = new CheckoutMemo();
            memo.setReservation(reservation);
            memo.setMemo(request.getMemo().trim());
            memo.setCheckoutDate(today);
            checkoutMemoRepository.save(memo);
        }
    }

    @Transactional
    public void cancelReservation(Long id, String password) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다"));

        if (!passwordEncoder.matches(password, reservation.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        // 연관된 체크아웃 메모 삭제
        checkoutMemoRepository.deleteByReservationId(id);

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    public CheckoutMemoResponse getLatestCheckoutMemo() {
        // 취소되지 않은 예약의 메모만 반환
        return checkoutMemoRepository.findTopByReservationStatusOrderByCreatedAtDesc(ReservationStatus.CHECKED_OUT)
                .map(CheckoutMemoResponse::from)
                .orElse(null);
    }

    public List<ReservationResponse> getActiveReservationsForToday() {
        LocalDate today = LocalDate.now();
        return reservationRepository
                .findByStatusAndCheckInDateLessThanEqualAndCheckOutDateGreaterThanEqual(
                        ReservationStatus.ACTIVE, today, today)
                .stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new IllegalArgumentException("퇴실일은 입실일 이후여야 합니다");
        }
    }
}
