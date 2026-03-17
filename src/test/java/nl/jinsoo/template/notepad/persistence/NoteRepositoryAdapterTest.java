package nl.jinsoo.template.notepad.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import nl.jinsoo.template.TestcontainersConfiguration;
import nl.jinsoo.template.notepad.Note;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJdbcTest
@Import({NoteRepositoryAdapter.class, TestcontainersConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NoteRepositoryAdapterTest {

  @Autowired private NoteRepositoryAdapter adapter;

  @Test
  void saveAndFindRoundTrip() {
    var note = new Note(null, "Test Title", "Test Body", Instant.now());

    var saved = adapter.save(note);

    assertThat(saved.id()).isNotNull();
    assertThat(saved.title()).isEqualTo("Test Title");
    assertThat(saved.body()).isEqualTo("Test Body");
    assertThat(saved.createdAt()).isNotNull();

    var found = adapter.findById(saved.id());

    assertThat(found).isPresent();
    assertThat(found.get().title()).isEqualTo("Test Title");
  }

  @Test
  void findByIdReturnsEmptyForNonexistent() {
    var result = adapter.findById(999L);
    assertThat(result).isEmpty();
  }
}
