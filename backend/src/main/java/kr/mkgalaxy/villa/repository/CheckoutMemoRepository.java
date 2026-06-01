package kr.mkgalaxy.villa.repository;

import kr.mkgalaxy.villa.entity.CheckoutMemo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CheckoutMemoRepository extends JpaRepository<CheckoutMemo, Long> {
    Optional<CheckoutMemo> findTopByOrderByCreatedAtDesc();
    Optional<CheckoutMemo> findTopByReservationStatusOrderByCreatedAtDesc(kr.mkgalaxy.villa.entity.ReservationStatus status);
    void deleteByReservationId(Long reservationId);
}
