package com.poc.assistant.tools;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.poc.assistant.google.GoogleApiFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class CalendarTools {

    private final GoogleApiFactory google;

    public CalendarTools(GoogleApiFactory google) {
        this.google = google;
    }

    @Tool(description = "List upcoming Google Calendar events for the next N days (max 20 results).")
    public String listEvents(int days) {
        if (!google.isConfigured()) return notConfigured();
        try {
            var now = new DateTime(System.currentTimeMillis());
            var future = new DateTime(System.currentTimeMillis() + (long) days * 86_400_000);
            var events = google.calendar().events().list("primary")
                    .setMaxResults(20).setTimeMin(now).setTimeMax(future)
                    .setOrderBy("startTime").setSingleEvents(true).execute();
            if (events.getItems().isEmpty()) return "No events in the next " + days + " days.";
            return events.getItems().stream()
                    .map(e -> "- [%s] %s (%s)".formatted(
                            e.getId(), e.getSummary(),
                            e.getStart().getDateTime() != null
                                    ? e.getStart().getDateTime() : e.getStart().getDate()))
                    .collect(Collectors.joining("\n", "Upcoming events:\n", ""));
        } catch (Exception ex) {
            return "Error listing events: " + ex.getMessage();
        }
    }

    @Tool(description = """
            Create a Google Calendar event. Parameters:
            - title: event title
            - startDateTime: ISO-8601 with offset, e.g. 2025-06-15T10:00:00-03:00
            - endDateTime: ISO-8601 with offset
            - attendees: comma-separated email addresses, or empty string
            - description: optional event description
            """)
    public String createEvent(String title, String startDateTime, String endDateTime,
                              String attendees, String description) {
        if (!google.isConfigured()) return notConfigured();
        try {
            var event = new Event().setSummary(title).setDescription(description)
                    .setStart(new EventDateTime().setDateTime(new DateTime(startDateTime)).setTimeZone("America/Sao_Paulo"))
                    .setEnd(new EventDateTime().setDateTime(new DateTime(endDateTime)).setTimeZone("America/Sao_Paulo"));
            if (attendees != null && !attendees.isBlank()) {
                event.setAttendees(Arrays.stream(attendees.split(","))
                        .map(String::trim)
                        .map(e -> new EventAttendee().setEmail(e))
                        .collect(Collectors.toList()));
            }
            var created = google.calendar().events().insert("primary", event).execute();
            return "Event created: %s (ID: %s)".formatted(created.getSummary(), created.getId());
        } catch (Exception ex) {
            return "Error creating event: " + ex.getMessage();
        }
    }

    @Tool(description = "Delete a Google Calendar event by its event ID.")
    public String deleteEvent(String eventId) {
        if (!google.isConfigured()) return notConfigured();
        try {
            google.calendar().events().delete("primary", eventId).execute();
            return "Event " + eventId + " deleted.";
        } catch (Exception ex) {
            return "Error deleting event: " + ex.getMessage();
        }
    }

    private String notConfigured() {
        return "Google Calendar is not configured. Place credentials.json in the project root and restart.";
    }
}
