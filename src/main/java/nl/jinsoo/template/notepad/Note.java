package nl.jinsoo.template.notepad;

import java.time.Instant;

public record Note(Long id, String title, String body, Instant createdAt) {}
