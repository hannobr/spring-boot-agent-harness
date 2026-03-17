package nl.jinsoo.template.notepad;

public interface NotepadAPI {

  Note create(Note note);

  Note findById(long id);
}
