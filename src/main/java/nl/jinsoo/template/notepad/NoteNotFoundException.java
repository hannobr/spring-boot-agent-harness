package nl.jinsoo.template.notepad;

public class NoteNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final long noteId;

  public NoteNotFoundException(long noteId) {
    super("Note with ID " + noteId + " not found");
    this.noteId = noteId;
  }

  public long getNoteId() {
    return noteId;
  }
}
