package kr.mkgalaxy.villa.repository;

import kr.mkgalaxy.villa.entity.Reservation;
import kr.mkgalaxy.villa.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r WHERE r.status = :status " +
           "AND r.checkInDate < :endDate AND r.checkOutDate > :startDate " +
           "ORDER BY r.checkInDate")
    List<Reservation> findByStatusAndDateRange(
            @Param("status") ReservationStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = 'ACTIVE' " +
           "AND r.id <> :excludeId " +
           "AND r.checkInDate < :checkOut AND r.checkOutDate > :checkIn")
    long countConflicting(
            @Param("excludeId") Long excludeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = 'ACTIVE' " +
           "AND r.checkInDate < :checkOut AND r.checkOutDate > :checkIn")
    long countConflictingNew(
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    List<Reservation> findByStatusAndCheckInDateLessThanEqualAndCheckOutDateGreaterThanEqual(
            ReservationStatus status, LocalDate date1, LocalDate date2);
}
