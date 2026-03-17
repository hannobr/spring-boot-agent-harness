package nl.jinsoo.template.notepad.internal;

import nl.jinsoo.template.notepad.Note;
import nl.jinsoo.template.notepad.NoteNotFoundException;

class FindNoteByIdUseCase {

  private final NotePersistencePort persistence;

  FindNoteByIdUseCase(NotePersistencePort persistence) {
    this.persistence = persistence;
  }

  Note execute(long id) {
    return persistence.findById(id).orElseThrow(() -> new NoteNotFoundException(id));
  }
}
