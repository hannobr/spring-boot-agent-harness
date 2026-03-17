package nl.jinsoo.template.notepad.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import nl.jinsoo.template.notepad.NotepadAPI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/notes")
@Tag(name = "Notepad", description = "Reference implementation — delete when starting real work")
class NoteController {

  private final NotepadAPI notepadAPI;

  NoteController(NotepadAPI notepadAPI) {
    this.notepadAPI = notepadAPI;
  }

  @PostMapping
  @Operation(summary = "Create a note")
  @ApiResponse(responseCode = "201", description = "Note created")
  ResponseEntity<NoteResponseDTO> create(@Valid @RequestBody CreateNoteRequestDTO request) {
    log.info("[NoteController.create] Creating note with title={}", request.title());
    var note = notepadAPI.create(request.toDomain());
    log.info("[NoteController.create] Created note id={}", note.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(NoteResponseDTO.from(note));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Find a note by ID")
  @ApiResponse(responseCode = "200", description = "Note found")
  @ApiResponse(responseCode = "404", description = "Note not found")
  NoteResponseDTO findById(@PathVariable long id) {
    log.info("[NoteController.findById] Looking up note id={}", id);
    var note = notepadAPI.findById(id);
    log.info("[NoteController.findById] Found note id={}", note.id());
    return NoteResponseDTO.from(note);
  }
}
