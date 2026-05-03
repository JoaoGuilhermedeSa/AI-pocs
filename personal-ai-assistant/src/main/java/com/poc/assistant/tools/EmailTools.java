package com.poc.assistant.tools;

import com.google.api.services.gmail.model.MessagePartHeader;
import com.poc.assistant.google.GoogleApiFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
public class EmailTools {

    private final GoogleApiFactory google;

    public EmailTools(GoogleApiFactory google) {
        this.google = google;
    }

    @Tool(description = "Send an email via Gmail. to: recipient address, subject: email subject, body: plain text body.")
    public String sendEmail(String to, String subject, String body) {
        if (!google.isConfigured()) return notConfigured();
        try {
            var raw = "MIME-Version: 1.0\r\n" +
                    "Content-Type: text/plain; charset=utf-8\r\n" +
                    "From: " + google.getUserEmail() + "\r\n" +
                    "To: " + to + "\r\n" +
                    "Subject: " + subject + "\r\n\r\n" +
                    body;
            var message = new com.google.api.services.gmail.model.Message()
                    .setRaw(Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
            google.gmail().users().messages().send("me", message).execute();
            return "Email sent to " + to + ".";
        } catch (Exception ex) {
            return "Error sending email: " + ex.getMessage();
        }
    }

    @Tool(description = "Read the most recent emails from Gmail inbox. count: number of emails to fetch (max 10).")
    public String readEmails(int count) {
        if (!google.isConfigured()) return notConfigured();
        try {
            var resp = google.gmail().users().messages().list("me")
                    .setMaxResults((long) Math.min(count, 10))
                    .setLabelIds(List.of("INBOX"))
                    .execute();
            if (resp.getMessages() == null || resp.getMessages().isEmpty()) return "Inbox is empty.";
            var sb = new StringBuilder("Recent inbox emails:\n");
            for (var msg : resp.getMessages()) {
                var full = google.gmail().users().messages().get("me", msg.getId())
                        .setFormat("metadata")
                        .setMetadataHeaders(List.of("Subject", "From", "Date"))
                        .execute();
                var hdrs = full.getPayload().getHeaders();
                sb.append("- [%s] From: %s | Subject: %s | Date: %s\n".formatted(
                        msg.getId(), header(hdrs, "From"), header(hdrs, "Subject"), header(hdrs, "Date")));
            }
            return sb.toString().trim();
        } catch (Exception ex) {
            return "Error reading emails: " + ex.getMessage();
        }
    }

    @Tool(description = "Search Gmail using Gmail query syntax, e.g. 'from:someone@gmail.com subject:meeting after:2025/01/01'.")
    public String searchEmails(String query) {
        if (!google.isConfigured()) return notConfigured();
        try {
            var resp = google.gmail().users().messages().list("me").setQ(query).setMaxResults(5L).execute();
            if (resp.getMessages() == null || resp.getMessages().isEmpty())
                return "No emails found for query: " + query;
            var sb = new StringBuilder("Search results for \"" + query + "\":\n");
            for (var msg : resp.getMessages()) {
                var full = google.gmail().users().messages().get("me", msg.getId())
                        .setFormat("metadata")
                        .setMetadataHeaders(List.of("Subject", "From", "Date"))
                        .execute();
                var hdrs = full.getPayload().getHeaders();
                sb.append("- [%s] From: %s | Subject: %s\n".formatted(
                        msg.getId(), header(hdrs, "From"), header(hdrs, "Subject")));
            }
            return sb.toString().trim();
        } catch (Exception ex) {
            return "Error searching emails: " + ex.getMessage();
        }
    }

    private String header(List<MessagePartHeader> headers, String name) {
        return headers.stream()
                .filter(h -> h.getName().equalsIgnoreCase(name))
                .map(MessagePartHeader::getValue)
                .findFirst().orElse("N/A");
    }

    private String notConfigured() {
        return "Gmail is not configured. Place credentials.json in the project root and restart.";
    }
}
