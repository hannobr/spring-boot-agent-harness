package nl.jinsoo.template.notepad.internal;

import java.util.Optional;
import nl.jinsoo.template.notepad.Note;

public interface NotePersistencePort {

  Note save(Note note);

  Optional<Note> findById(long id);
}
