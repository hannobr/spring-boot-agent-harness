package nl.jinsoo.template.notepad.rest;

import static org.assertj.core.api.Assertions.assertThat;

import nl.jinsoo.template.notepad.NoteNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class NoteExceptionHandlerTest {

  private final NoteExceptionHandler handler = new NoteExceptionHandler();

  @Test
  void handleNoteNotFoundReturnsProblemDetail() {
    var problem = handler.handleNoteNotFound(new NoteNotFoundException(42L));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getDetail()).contains("42");
    assertThat(problem.getTitle()).isEqualTo("Note Not Found");
  }
}
