package kr.mkgalaxy.villa.dto;

import kr.mkgalaxy.villa.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReservationResponse {

    private Long id;
    private String reserverName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int adultCount;
    private int childCount;
    private String description;
    private String status;
    private LocalDateTime createdAt;

    public static ReservationResponse from(Reservation r) {
        ReservationResponse resp = new ReservationResponse();
        resp.id = r.getId();
        resp.reserverName = r.getReserverName();
        resp.checkInDate = r.getCheckInDate();
        resp.checkOutDate = r.getCheckOutDate();
        resp.adultCount = r.getAdultCount();
        resp.childCount = r.getChildCount();
        resp.description = r.getDescription();
        resp.status = r.getStatus().name();
        resp.createdAt = r.getCreatedAt();
        return resp;
    }

    public Long getId() { return id; }
    public String getReserverName() { return reserverName; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public int getAdultCount() { return adultCount; }
    public int getChildCount() { return childCount; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
