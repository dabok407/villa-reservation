package kr.mkgalaxy.villa.ai.tool.dto;

import java.time.LocalDate;

/**
 * list_reservations / active_today 도구가 LLM에게 돌려줄 예약 요약(슬림 DTO).
 * 비밀번호 등 민감정보는 포함하지 않는다.
 */
public class ReservationSummary {

    private final String reserverName;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final int adultCount;
    private final int childCount;

    public ReservationSummary(String reserverName, LocalDate checkInDate, LocalDate checkOutDate,
                              int adultCount, int childCount) {
        this.reserverName = reserverName;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.adultCount = adultCount;
        this.childCount = childCount;
    }

    public String getReserverName() { return reserverName; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public int getAdultCount() { return adultCount; }
    public int getChildCount() { return childCount; }
}
