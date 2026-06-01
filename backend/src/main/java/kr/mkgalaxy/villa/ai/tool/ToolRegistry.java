package kr.mkgalaxy.villa.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 자연어 어시스턴트가 사용할 읽기 전용 도구의 스키마 제공 + 실행 디스패처.
 * 실제 조회는 {@link ReservationToolService}(기존 Repository 재사용)에 위임한다.
 * 사용자/LLM 입력은 신뢰하지 않는다 — 파싱 오류는 예외가 아니라 tool_result 에 담는 error JSON 으로 돌려준다.
 */
@Component
public class ToolRegistry {

    private final ReservationToolService toolService;
    private final ObjectMapper mapper;

    public ToolRegistry(ReservationToolService toolService) {
        this.toolService = toolService;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // LocalDate → "YYYY-MM-DD"
    }

    /** Anthropic tools 배열에 넣을 도구 스키마 3종(읽기 전용). */
    public List<Map<String, Object>> toolSpecs() {
        List<Map<String, Object>> tools = new ArrayList<Map<String, Object>>();
        tools.add(tool("check_availability",
                "주어진 입실/퇴실 날짜 구간의 예약 가능 여부와 충돌 예약을 조회한다.",
                schemaCheckAvailability()));
        tools.add(tool("list_reservations",
                "특정 연/월의 진행중(ACTIVE) 예약 목록을 조회한다.",
                schemaListReservations()));
        tools.add(tool("active_today",
                "오늘 기준 진행중인 예약을 조회한다.",
                schemaEmpty()));
        return tools;
    }

    /** 도구 실행 → tool_result 에 넣을 JSON 문자열 반환. 오류는 error JSON 으로. */
    public String execute(String name, Map<String, Object> input) {
        try {
            if ("check_availability".equals(name)) {
                LocalDate in = LocalDate.parse(requireStr(input, "checkInDate"));
                LocalDate out = LocalDate.parse(requireStr(input, "checkOutDate"));
                return mapper.writeValueAsString(toolService.checkAvailability(in, out));
            } else if ("list_reservations".equals(name)) {
                int year = requireInt(input, "year");
                int month = requireInt(input, "month");
                return mapper.writeValueAsString(toolService.listReservations(year, month));
            } else if ("active_today".equals(name)) {
                return mapper.writeValueAsString(toolService.activeToday(LocalDate.now()));
            }
            return errorJson("알 수 없는 도구: " + name);
        } catch (Exception e) {
            return errorJson(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    // ===== 스키마 빌더 =====

    private Map<String, Object> tool(String name, String description, Map<String, Object> inputSchema) {
        Map<String, Object> t = new LinkedHashMap<String, Object>();
        t.put("name", name);
        t.put("description", description);
        t.put("input_schema", inputSchema);
        return t;
    }

    private Map<String, Object> schemaCheckAvailability() {
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("checkInDate", stringProp("입실일 (YYYY-MM-DD)"));
        props.put("checkOutDate", stringProp("퇴실일 (YYYY-MM-DD)"));
        return objectSchema(props, "checkInDate", "checkOutDate");
    }

    private Map<String, Object> schemaListReservations() {
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("year", intProp("연도 (예: 2026)"));
        props.put("month", intProp("월 (1~12)"));
        return objectSchema(props, "year", "month");
    }

    private Map<String, Object> schemaEmpty() {
        return objectSchema(new LinkedHashMap<String, Object>());
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        List<String> req = new ArrayList<String>();
        for (String r : required) {
            req.add(r);
        }
        schema.put("required", req);
        return schema;
    }

    private Map<String, Object> stringProp(String description) {
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("type", "string");
        p.put("description", description);
        return p;
    }

    private Map<String, Object> intProp(String description) {
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("type", "integer");
        p.put("description", description);
        return p;
    }

    // ===== 입력 방어 =====

    private String requireStr(Map<String, Object> input, String key) {
        Object v = (input == null) ? null : input.get(key);
        if (v == null || v.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("필수 입력 누락: " + key);
        }
        return v.toString().trim();
    }

    private int requireInt(Map<String, Object> input, String key) {
        Object v = (input == null) ? null : input.get(key);
        if (v == null) {
            throw new IllegalArgumentException("필수 입력 누락: " + key);
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return Integer.parseInt(v.toString().trim());
    }

    private String errorJson(String message) {
        Map<String, Object> err = new LinkedHashMap<String, Object>();
        err.put("error", message);
        try {
            return mapper.writeValueAsString(err);
        } catch (Exception e) {
            return "{\"error\":\"tool execution failed\"}";
        }
    }
}
