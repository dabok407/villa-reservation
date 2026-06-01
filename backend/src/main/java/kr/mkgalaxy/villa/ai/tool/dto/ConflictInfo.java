package kr.mkgalaxy.villa.ai.tool.dto;

import java.time.LocalDate;

/**
 * 자연어 어시스턴트 도구가 LLM에게 돌려줄 충돌 예약 요약(슬림 DTO).
 * 누가/언제 겹치는지만 담는다. 비밀번호 등 민감정보는 포함하지 않는다.
 */
public class ConflictInfo {

    private final String reserverName;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;

    public ConflictInfo(String reserverName, LocalDate checkInDate, LocalDate checkOutDate) {
        this.reserverName = reserverName;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
    }

    public String getReserverName() { return reserverName; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
}
