package nl.jinsoo.template.notepad.persistence;

import org.springframework.data.repository.ListCrudRepository;

interface NoteRepository extends ListCrudRepository<NoteEntity, Long> {}
