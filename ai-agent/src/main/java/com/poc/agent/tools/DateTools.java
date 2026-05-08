package com.poc.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class DateTools {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Tool(description = """
            Returns today's date, day of the week, and exact dates for the next 14 days.
            Call this before scheduling or when the user references relative dates like
            'next Wednesday', 'this Friday', or 'in 3 days'.
            """)
    public String getCurrentDateInfo() {
        var today = LocalDate.now();
        var dayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        var upcoming = IntStream.rangeClosed(1, 14)
                .mapToObj(today::plusDays)
                .map(d -> "%s — %s".formatted(
                        d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        d.format(FMT)))
                .collect(Collectors.joining("\n"));

        return """
                Today: %s (%s)

                Upcoming dates:
                %s
                """.formatted(today.format(FMT), dayName, upcoming);
    }
}
