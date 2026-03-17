package nl.jinsoo.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

  @Test
  void handleUnexpectedReturnsGenericProblemDetail() {
    var problemDetail =
        new GlobalExceptionHandler().handleUnexpected(new IllegalStateException("boom"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
    assertEquals("An unexpected error occurred", problemDetail.getDetail());
  }
}
