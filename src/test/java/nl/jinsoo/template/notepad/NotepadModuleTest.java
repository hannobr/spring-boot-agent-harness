package nl.jinsoo.template.notepad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import nl.jinsoo.template.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class NotepadModuleTest {

  @Autowired private NotepadAPI notepadAPI;

  @Test
  void createAndFindById() {
    var note = new Note(null, "Module Test", "Testing wiring", null);

    var created = notepadAPI.create(note);

    assertThat(created.id()).isNotNull();
    assertThat(created.title()).isEqualTo("Module Test");
    assertThat(created.createdAt()).isNotNull();

    var found = notepadAPI.findById(created.id());

    assertThat(found.title()).isEqualTo("Module Test");
  }

  @Test
  void findByIdThrowsWhenNotFound() {
    assertThatThrownBy(() -> notepadAPI.findById(999L)).isInstanceOf(NoteNotFoundException.class);
  }
}
