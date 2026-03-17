package nl.jinsoo.template.notepad.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import nl.jinsoo.template.notepad.Note;
import nl.jinsoo.template.notepad.NoteNotFoundException;
import org.junit.jupiter.api.Test;

class FindNoteByIdUseCaseTest {

  private final InMemoryNotePersistence persistence = new InMemoryNotePersistence();
  private final FindNoteByIdUseCase useCase = new FindNoteByIdUseCase(persistence);

  @Test
  void returnsNoteWhenFound() {
    var saved = persistence.save(new Note(null, "Title", "Body", Instant.now()));

    var result = useCase.execute(saved.id());

    assertThat(result.title()).isEqualTo("Title");
  }

  @Test
  void throwsWhenNotFound() {
    assertThatThrownBy(() -> useCase.execute(999L))
        .isInstanceOf(NoteNotFoundException.class)
        .hasMessageContaining("999");
  }
}
