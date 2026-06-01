package kr.mkgalaxy.villa.dto;

import kr.mkgalaxy.villa.entity.CheckoutMemo;

import java.time.LocalDate;

public class CheckoutMemoResponse {

    private Long id;
    private String reserverName;
    private String memo;
    private LocalDate checkoutDate;

    public static CheckoutMemoResponse from(CheckoutMemo m) {
        CheckoutMemoResponse resp = new CheckoutMemoResponse();
        resp.id = m.getId();
        resp.reserverName = m.getReservation().getReserverName();
        resp.memo = m.getMemo();
        resp.checkoutDate = m.getCheckoutDate();
        return resp;
    }

    public Long getId() { return id; }
    public String getReserverName() { return reserverName; }
    public String getMemo() { return memo; }
    public LocalDate getCheckoutDate() { return checkoutDate; }
}
