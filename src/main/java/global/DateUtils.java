package global;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class DateUtils
{
    /**
     * Converts "2026-04-14 19:55:54" into something like "Apr 14, 2026, 7:55 PM"
     */
    public static String formatToProfessional(String rawDate)
    {
        if (rawDate == null || rawDate.isEmpty()) return "Unknown";

        try
        {
            // 1. Parse the SQLite default format
            DateTimeFormatter sqliteFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(rawDate, sqliteFormatter);

            // 2. Format it using the user's system locale (Medium length is very 'Pro' looking)
            DateTimeFormatter proFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
            return dateTime.format(proFormatter);
        }
        catch (Exception e) {
            return rawDate; // Fallback to raw if parsing fails
        }
    }

    public static String getRelativeTime(String rawDate)
    {
        DateTimeFormatter sqliteFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(rawDate, sqliteFormatter);

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now);

        if (duration.toMinutes() < 1) return "Just now";
        if (duration.toMinutes() < 60) return duration.toMinutes() + "m ago";
        if (duration.toHours() < 24) return duration.toHours() + "h ago";

        return formatToProfessional(rawDate);
    }
}
