package uk.co.threebugs.analysis;

import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {
        // Utility class; prevent instantiation
    }
}
