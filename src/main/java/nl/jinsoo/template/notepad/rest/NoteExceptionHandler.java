package nl.jinsoo.template.notepad.rest;

import nl.jinsoo.template.notepad.NoteNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = NoteController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class NoteExceptionHandler {

  @ExceptionHandler(NoteNotFoundException.class)
  ProblemDetail handleNoteNotFound(NoteNotFoundException ex) {
    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, "Note with ID " + ex.getNoteId() + " not found");
    problem.setTitle("Note Not Found");
    return problem;
  }
}
