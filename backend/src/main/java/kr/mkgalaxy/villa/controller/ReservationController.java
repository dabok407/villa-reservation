package kr.mkgalaxy.villa.controller;

import kr.mkgalaxy.villa.dto.*;
import kr.mkgalaxy.villa.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/reservations")
    public List<ReservationResponse> getReservations(
            @RequestParam int year,
            @RequestParam int month) {
        return reservationService.getReservationsByMonth(year, month);
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse getReservation(@PathVariable Long id) {
        return reservationService.getReservation(id);
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/reservations/{id}/verify-password")
    public ResponseEntity<Map<String, Boolean>> verifyPassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordVerifyRequest request) {
        boolean valid = reservationService.verifyPassword(id, request.getPassword());
        return ResponseEntity.ok(Collections.singletonMap("valid", valid));
    }

    @PutMapping("/reservations/{id}")
    public ReservationResponse updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateRequest request) {
        return reservationService.updateReservation(id, request);
    }

    @PostMapping("/reservations/{id}/checkout")
    public ResponseEntity<Void> checkout(
            @PathVariable Long id,
            @Valid @RequestBody CheckoutRequest request) {
        reservationService.checkout(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id,
            @Valid @RequestBody PasswordVerifyRequest request) {
        reservationService.cancelReservation(id, request.getPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/checkout-memos/latest")
    public ResponseEntity<CheckoutMemoResponse> getLatestCheckoutMemo() {
        CheckoutMemoResponse memo = reservationService.getLatestCheckoutMemo();
        if (memo == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(memo);
    }

    @GetMapping("/reservations/active-today")
    public List<ReservationResponse> getActiveReservationsForToday() {
        return reservationService.getActiveReservationsForToday();
    }
}
