package nl.jinsoo.template.notepad.persistence;

import java.util.Optional;
import nl.jinsoo.template.notepad.Note;
import nl.jinsoo.template.notepad.internal.NotePersistencePort;
import org.springframework.stereotype.Component;

@Component
class NoteRepositoryAdapter implements NotePersistencePort {

  private final NoteRepository repository;

  NoteRepositoryAdapter(NoteRepository repository) {
    this.repository = repository;
  }

  @Override
  public Note save(Note note) {
    return repository.save(NoteEntity.from(note)).toDomain();
  }

  @Override
  public Optional<Note> findById(long id) {
    return repository.findById(id).map(NoteEntity::toDomain);
  }
}
