package com.poc.assistant.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.util.List;

@Component
public class GoogleApiFactory {

    private static final Logger log = LoggerFactory.getLogger(GoogleApiFactory.class);
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(
            CalendarScopes.CALENDAR,
            GmailScopes.GMAIL_SEND,
            GmailScopes.GMAIL_READONLY
    );
    private static final String APP_NAME = "Personal AI Assistant";

    @Value("${google.credentials.file:credentials.json}")
    private String credentialsFile;

    @Value("${google.tokens.directory:tokens}")
    private String tokensDirectory;

    @Value("${google.user.email:me}")
    private String userEmail;

    private NetHttpTransport httpTransport;
    private Credential credential;
    private boolean configured = false;

    public void initialize() {
        var file = new File(credentialsFile);
        if (!file.exists()) {
            log.warn("credentials.json not found — Google Calendar/Gmail integration disabled. " +
                    "See README for setup instructions.");
            return;
        }
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            var secrets = GoogleClientSecrets.load(JSON_FACTORY, new FileReader(file));
            var flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, secrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new File(tokensDirectory)))
                    .setAccessType("offline")
                    .build();
            var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
            configured = true;
            log.info("Google API initialized successfully.");
        } catch (Exception ex) {
            log.error("Failed to initialize Google API: {}", ex.getMessage());
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Calendar calendar() throws Exception {
        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APP_NAME)
                .build();
    }

    public Gmail gmail() throws Exception {
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APP_NAME)
                .build();
    }
}
