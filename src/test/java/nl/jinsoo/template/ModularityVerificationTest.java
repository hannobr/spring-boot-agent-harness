package nl.jinsoo.template;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityVerificationTest {

  @Test
  void verifyModularStructure() {
    ApplicationModules.of(TemplateApplication.class).verify();
  }
}
