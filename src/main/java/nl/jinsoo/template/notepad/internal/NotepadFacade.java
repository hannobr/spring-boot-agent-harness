package nl.jinsoo.template.notepad.internal;

import lombok.extern.slf4j.Slf4j;
import nl.jinsoo.template.notepad.Note;
import nl.jinsoo.template.notepad.NotepadAPI;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
class NotepadFacade implements NotepadAPI {

  private final CreateNoteUseCase createNote;
  private final FindNoteByIdUseCase findNote;

  NotepadFacade(CreateNoteUseCase createNote, FindNoteByIdUseCase findNote) {
    this.createNote = createNote;
    this.findNote = findNote;
  }

  @Override
  @Transactional
  public Note create(Note note) {
    log.info("[NotepadFacade.create] Creating note title={}", note.title());
    var result = createNote.execute(note);
    log.info("[NotepadFacade.create] Created note id={}", result.id());
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public Note findById(long id) {
    log.info("[NotepadFacade.findById] Finding note id={}", id);
    return findNote.execute(id);
  }
}
