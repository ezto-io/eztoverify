package com.qlikverify.marketplace;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum GoogleWorkspaceSupportedApps {

    GMAIL("Gmail", "https://mail.google.com/a/" ,"gmail_logo.svg"),
    GOOGLE_ADMIN_CONSOLE("Google Admin Console","https://admin.google.com","admin_console_logo.svg"),
    GOOGLE_CALENDAR("Google Calendar", "https://calendar.google.com/a/", "calendar_logo.svg"),
    GOOGLE_SITES("Google Sites", "https://sites.google.com/a/", "sites_logo.svg"),
    GOOGLE_DOCS("Google Docs", "https://docs.google.com/a/", "docs_logo.svg"),
    GOOGLE_DRIVE("Google Drive", "https://drive.google.com/a/", "drive_logo.svg"),
    GOOGLE_MEET("Google Meet", "https://meet.google.com", "meet_logo.svg"),
    GOOGLE_KEEP("Google Keep", "https://keep.google.com/a/", "keep_logo.svg"),
    GOOGLE_ACCOUNT("Google Account", "https://accounts.google.com", "logo.svg"),
    GOOGLE_ANALYTICS("Google Analytics","https://analytics.google.com","analytics_logo.svg"),
    GOOGLE_SEARCH("Google Search","https://search.google.com","search_logo.svg"),
    GOOGLE_MAPS("Google Maps","https://maps.google.com","maps_logo.svg"),
    GOOGLE_CLOUD("Google Cloud","https://cloud.google.com","cloud_logo.svg"),
    GOOGLE_FORMS("Google Forms","https://forms.google.com","forms_logo.svg"),
    GOOGLE_CHAT("Google Chat","https://chat.google.com","chat_logo.svg"),
    GOOGLE_SLIDES("Google Slides", "https://slides.google.com", "slides_logo.svg"),
    GOOGLE_SHEETS("Google Sheets", "https://sheets.google.com", "sheets_logo.svg"),
    YOUTUBE("Youtube", "https://www.youtube.com/", "youtube_logo.svg"),
    YOUTUBE_STUDIO("Youtube Studio","https://studio.youtube.com","youtube_studio_logo.svg"),
    FIREBASE("Firebase","https://firebase.google.com","firebase_logo.svg");


    private String name;
    private String relayStateLink;
    private String logoUrl;

    GoogleWorkspaceSupportedApps(String name, String relayStateLink, String logoUrl) {
        this.name = name;
        this.relayStateLink = relayStateLink;
        this.logoUrl = logoUrl;
    }

    public String getName(){
        return this.name;
    }
    public String getRelayStateLink(){
        return this.relayStateLink;
    }
    public String getLogoUrl(){
        return this.logoUrl;
    }

    public static List<String> getAllNames(){
        return Arrays.stream(GoogleWorkspaceSupportedApps.values()).map(x -> x.getName()).collect(Collectors.toList());
    }

    public static List<GoogleWorkspaceSupportedApps> getUrlByName(String name) {
        return Arrays.stream(GoogleWorkspaceSupportedApps.values())
                .filter(apps -> apps.name.equals(name)).collect(Collectors.toList());
    }
}
