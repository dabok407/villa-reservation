package kr.mkgalaxy.villa.ai.tool.dto;

import java.util.List;

/**
 * check_availability 도구의 결과. available=true 면 해당 기간 예약 가능,
 * false 면 conflicts 에 겹치는 ACTIVE 예약 목록이 담긴다.
 */
public class AvailabilityResult {

    private final boolean available;
    private final List<ConflictInfo> conflicts;

    public AvailabilityResult(boolean available, List<ConflictInfo> conflicts) {
        this.available = available;
        this.conflicts = conflicts;
    }

    public boolean isAvailable() { return available; }
    public List<ConflictInfo> getConflicts() { return conflicts; }
}
