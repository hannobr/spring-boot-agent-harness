package nl.jinsoo.template.notepad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class NoteTest {

  @Test
  void recordFieldsAreAccessible() {
    var now = Instant.now();
    var note = new Note(1L, "Title", "Body", now);

    assertThat(note.id()).isEqualTo(1L);
    assertThat(note.title()).isEqualTo("Title");
    assertThat(note.body()).isEqualTo("Body");
    assertThat(note.createdAt()).isEqualTo(now);
  }

  @Test
  void nullIdRepresentsNewNote() {
    var note = new Note(null, "Title", "Body", Instant.now());
    assertThat(note.id()).isNull();
  }
}
