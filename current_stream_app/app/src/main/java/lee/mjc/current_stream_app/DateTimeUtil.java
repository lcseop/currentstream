package lee.mjc.current_stream_app;

import android.os.Build;

import org.json.JSONArray;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;

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
}
