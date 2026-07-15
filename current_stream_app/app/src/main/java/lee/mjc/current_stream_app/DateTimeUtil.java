package lee.mjc.current_stream_app;

import android.os.Build;

import org.json.JSONArray;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * 서버·UI에서 쓰는 날짜·시간 표시 유틸임.
 * 팀 로그 상대 시간, 목표 D-day 등을 서버 타임존 기준으로 맞춤.
 */
public final class DateTimeUtil {

    /** 서버·DB 기준 타임존 */
    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Seoul");

    /** ISO 비슷한 날짜 문자열 파싱용 포맷터 */
    private static final DateTimeFormatter FLEXIBLE_LOCAL = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalEnd()
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .toFormatter();

    /** new 막음 */
    private DateTimeUtil() {
    }

    /**
     * 백엔드 createdAt 필드를 밀리초로 바꿈.
     * 배열·숫자·문자열 형태 다 섞여 올 수 있어서 분기 처리함
     */
    public static long parseCreatedAtMillis(Object raw) {
        // null이면 0
        if (raw == null) return 0L;
        // API 26 미만은 java.time 못 씀
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return 0L;

        try {
            // [year, month, day, hour, minute, ...] 배열 형태
            if (raw instanceof JSONArray) {
                JSONArray arr = (JSONArray) raw;
                // 최소 5개(년월일시분) 없으면 파싱 불가
                if (arr.length() < 5) return 0L;
                int year = arr.getInt(0);
                int month = arr.getInt(1);
                int day = arr.getInt(2);
                int hour = arr.getInt(3);
                int minute = arr.getInt(4);
                int second = arr.length() > 5 ? arr.getInt(5) : 0;
                LocalDateTime time = LocalDateTime.of(year, month, day, hour, minute, second);
                return time.atZone(SERVER_ZONE).toInstant().toEpochMilli();
            }

            // 숫자(epoch) 형태
            if (raw instanceof Number) {
                long value = ((Number) raw).longValue();
                // [중요] 10자리 미만이면 초 단위라 ms로 곱함. 13자리는 이미 ms
                if (value > 0L && value < 1_000_000_000_000L) {
                    return value * 1000L;
                }
                return value;
            }

            String text = String.valueOf(raw).trim();
            // 빈 값·"null" 문자열은 무시
            if (text.isEmpty() || "null".equals(text)) return 0L;

            // UTC Z 끝나는 ISO
            if (text.endsWith("Z")) {
                return java.time.Instant.parse(text).toEpochMilli();
            }
            // +09:00 같은 오프셋 포함
            if (text.contains("+") || text.matches(".*-\\d{2}:\\d{2}$")) {
                return OffsetDateTime.parse(text).toInstant().toEpochMilli();
            }
            // 공백을 T로 바꿔서 로컬 datetime 파싱
            if (text.contains(" ")) {
                text = text.replace(" ", "T");
            }
            LocalDateTime time = LocalDateTime.parse(text, FLEXIBLE_LOCAL);
            return time.atZone(SERVER_ZONE).toInstant().toEpochMilli();
        } catch (Exception ignored) {
            // 형식 안 맞으면 0
            return 0L;
        }
    }

    /** 생성 시각(ms)을 "방금 전", "3분 전", "2월 5일" 같은 문자열로 바꿈 */
    public static String formatRelativeTime(long createdAtMillis) {
        if (createdAtMillis <= 0L) return "";
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return "";

        long nowMillis = System.currentTimeMillis();
        long diffMillis = Math.max(0L, nowMillis - createdAtMillis);
        long minutes = diffMillis / 60_000L;
        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";
        long hours = minutes / 60L;
        if (hours < 24) return hours + "시간 전";
        long days = hours / 24L;
        // 7일 넘으면 월·일만 표시
        if (days < 7) return days + "일 전";

        LocalDateTime time = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(createdAtMillis),
                SERVER_ZONE
        );
        return time.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREA));
    }

    /** 목표 endDate 기준 D-day 문자열 만듦. 남으면 D-N, 지났으면 D+N */
    public static String formatDday(String endDate) {
        if (endDate == null || endDate.isEmpty()) return "D-?";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate end = LocalDate.parse(endDate);
                LocalDate today = LocalDate.now();
                long days = ChronoUnit.DAYS.between(today, end);
                return days >= 0 ? ("D-" + days) : ("D+" + Math.abs(days));
            }
        } catch (Exception ignored) {
            // 파싱 실패면 D-?
        }
        return "D-?";
    }

    /** 마감일까지 "12일 남음", "3일 초과" 같은 문구로 바꿈 */
    public static String formatRemainingDays(String endDate) {
        if (endDate == null || endDate.isEmpty()) return "";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate end = LocalDate.parse(endDate);
                LocalDate today = LocalDate.now();
                long days = ChronoUnit.DAYS.between(today, end);
                return days >= 0 ? (days + "일 남음") : (Math.abs(days) + "일 초과");
            }
        } catch (Exception ignored) {
            // 파싱 실패면 빈 문자열
        }
        return "";
    }

    /** 목표 마감일이 오늘보다 이전이면 true. 연체 스타일 쓸 때 봄 */
    public static boolean isGoalOverdue(String endDate) {
        if (endDate == null || endDate.isEmpty()) return false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return LocalDate.parse(endDate).isBefore(LocalDate.now());
            }
        } catch (Exception ignored) {
            // 파싱 실패면 연체 아님
        }
        return false;
    }
}
