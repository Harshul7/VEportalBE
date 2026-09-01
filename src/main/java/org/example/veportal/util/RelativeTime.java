package org.example.veportal.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class RelativeTime {

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private RelativeTime() {
    }

    public static String format(LocalDateTime value) {
        LocalDate today = LocalDate.now();
        LocalDate date = value.toLocalDate();
        if (date.equals(today)) {
            return String.format("Today, %s", value.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)));
        }
        if (date.equals(today.minusDays(1))) {
            return "Yesterday";
        }
        return date.format(DAY_MONTH);
    }
}
