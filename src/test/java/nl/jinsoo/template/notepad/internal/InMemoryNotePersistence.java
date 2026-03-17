package nl.jinsoo.template.notepad.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import nl.jinsoo.template.notepad.Note;

class InMemoryNotePersistence implements NotePersistencePort {

  private final Map<Long, Note> store = new HashMap<>();
  private final AtomicLong sequence = new AtomicLong(1);

  @Override
  public Note save(Note note) {
    var id = sequence.getAndIncrement();
    var saved = new Note(id, note.title(), note.body(), note.createdAt());
    store.put(id, saved);
    return saved;
  }

  @Override
  public Optional<Note> findById(long id) {
    return Optional.ofNullable(store.get(id));
  }
}
