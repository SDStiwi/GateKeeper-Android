package com.sdstiwi.gatekeeper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Centralized time handling for database timestamps and display formatting.
 * Stored timestamps are epoch milliseconds whenever a numeric value is used.
 */
public final class TimeUtils {
    private TimeUtils() {}

    public static long now() {
        return System.currentTimeMillis();
    }

    /**
     * Reads a timestamp that may be stored as epoch milliseconds or as one of
     * the legacy SQLite text timestamp formats used by the bundled database.
     */
    public static long parseTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) return -1L;
        String trimmed = value.trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
            String[] formats = {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd HH:mm",
                    "yyyy-MM-dd"
            };
            for (String format : formats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
                    sdf.setLenient(false);
                    Date date = sdf.parse(trimmed);
                    if (date != null) return date.getTime();
                } catch (ParseException ignoredFormat) {
                    // Try the next supported legacy format.
                }
            }
        }
        return -1L;
    }

    public static String format(long timestamp) {
        if (timestamp < 0) return "Unknown time";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(timestamp));
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date(now()));
    }

    public static long combineTodayWithTime(String hhmm) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        sdf.setLenient(false);
        return sdf.parse(today() + " " + hhmm).getTime();
    }
}
