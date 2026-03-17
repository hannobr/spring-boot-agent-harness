package nl.jinsoo.template.notepad.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import nl.jinsoo.template.SecurityConfig;
import nl.jinsoo.template.notepad.Note;
import nl.jinsoo.template.notepad.NoteNotFoundException;
import nl.jinsoo.template.notepad.NotepadAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(NoteController.class)
@Import(SecurityConfig.class)
@TestPropertySource(
    properties = "jwt.secret-key=test-key-must-be-at-least-256-bits-for-hmac-sha256")
@WithMockUser
class NoteControllerSliceTest {

  @Autowired MockMvcTester mvc;
  @MockitoBean NotepadAPI notepadAPI;

  @Test
  void createReturns201WithResponseBody() {
    var note = new Note(1L, "Title", "Body", Instant.parse("2026-01-01T00:00:00Z"));
    Mockito.when(notepadAPI.create(Mockito.any())).thenReturn(note);

    assertThat(
            mvc.post()
                .uri("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "Title", "body": "Body"}
                    """))
        .hasStatus(201)
        .bodyJson()
        .extractingPath("$.title")
        .isEqualTo("Title");
  }

  @Test
  void findByIdReturns200WithNote() {
    var note = new Note(1L, "Title", "Body", Instant.parse("2026-01-01T00:00:00Z"));
    Mockito.when(notepadAPI.findById(1L)).thenReturn(note);

    assertThat(mvc.get().uri("/api/notes/1"))
        .hasStatus(200)
        .bodyJson()
        .extractingPath("$.id")
        .isEqualTo(1);
  }

  @Test
  void findByIdReturns404WhenNotFound() {
    Mockito.when(notepadAPI.findById(999L)).thenThrow(new NoteNotFoundException(999L));

    assertThat(mvc.get().uri("/api/notes/999")).hasStatus(404);
  }

  @Test
  void createReturns400WhenTitleBlank() {
    assertThat(
            mvc.post()
                .uri("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title": "", "body": "Body"}
                    """))
        .hasStatus(400);
  }
}
