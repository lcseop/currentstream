package lee.mjc.current_stream_app;

import android.os.Build;

import org.json.JSONArray;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

// 날짜·시간 표시 유틸 (팀 로그 상대 시간, 목표 D-day 등)
public final class DateTimeUtil {

    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Seoul");

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

    private DateTimeUtil() {
    }

    // 서버 createdAt(JSON 배열·문자열·숫자)을 epoch ms로 변환
    public static long parseCreatedAtMillis(Object raw) {
        if (raw == null) return 0L;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return 0L;

        try {
            if (raw instanceof JSONArray) {
                JSONArray arr = (JSONArray) raw;
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

            if (raw instanceof Number) {
                return ((Number) raw).longValue();
            }

            String text = String.valueOf(raw).trim();
            if (text.isEmpty() || "null".equals(text)) return 0L;

            LocalDateTime time;
            if (text.endsWith("Z")) {
                return java.time.Instant.parse(text).toEpochMilli();
            }
            if (text.contains(" ")) {
                text = text.replace(" ", "T");
            }
            time = LocalDateTime.parse(text, FLEXIBLE_LOCAL);
            return time.atZone(SERVER_ZONE).toInstant().toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    // 생성 시각 기준 상대 시간 표시 (방금 전, 3분 전, 2월 5일 등)
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
        if (days < 7) return days + "일 전";

        LocalDateTime time = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(createdAtMillis),
                SERVER_ZONE
        );
        return time.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREA));
    }

    // 팀 선택/진행률 카드용 D-day (예: D-15, D+3)
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
        }
        return "D-?";
    }

    // 목표 마감까지 남은 일수 (예: 12일 남음, 3일 초과)
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
        }
        return "";
    }

    // 목표 마감일이 오늘보다 이전이면 true
    public static boolean isGoalOverdue(String endDate) {
        if (endDate == null || endDate.isEmpty()) return false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return LocalDate.parse(endDate).isBefore(LocalDate.now());
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
