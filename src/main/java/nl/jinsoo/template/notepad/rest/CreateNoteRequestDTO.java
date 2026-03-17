package nl.jinsoo.template.notepad.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import nl.jinsoo.template.notepad.Note;

@Schema(description = "Request to create a note")
record CreateNoteRequestDTO(
    @NotBlank @Size(max = 200) @Schema(description = "Note title", example = "My first note")
        String title,
    @NotBlank @Schema(description = "Note body", example = "Hello, world!") String body) {

  Note toDomain() {
    return new Note(null, title, body, null);
  }
}
