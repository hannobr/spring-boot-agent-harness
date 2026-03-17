package nl.jinsoo.template.notepad.persistence;

import java.time.Instant;
import nl.jinsoo.template.notepad.Note;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("notes")
record NoteEntity(@Id Long id, String title, String body, Instant createdAt) {

  static NoteEntity from(Note note) {
    return new NoteEntity(note.id(), note.title(), note.body(), note.createdAt());
  }

  Note toDomain() {
    return new Note(id, title, body, createdAt);
  }
}
