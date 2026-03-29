package nl.jinsoo.template.notepad;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import nl.jinsoo.template.OpenApiContractValidator;
import nl.jinsoo.template.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class NotepadIT {

  @Autowired private RestTestClient restTestClient;
  @Autowired private JdbcTemplate jdbc;

  @Value("${jwt.secret-key}")
  private String jwtSecretKey;

  @Value("${jwt.audience:}")
  private String jwtAudience;

  @BeforeEach
  void cleanDatabase() {
    jdbc.execute("TRUNCATE TABLE notes RESTART IDENTITY CASCADE");
  }

  private String generateToken() {
    try {
      var signer = new MACSigner(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
      var claimsBuilder =
          new JWTClaimsSet.Builder()
              .subject("test-user")
              .issueTime(Date.from(Instant.now()))
              .expirationTime(Date.from(Instant.now().plusSeconds(3600)));
      if (!jwtAudience.isBlank()) {
        claimsBuilder.audience(jwtAudience);
      }
      var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build());
      jwt.sign(signer);
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new RuntimeException("Failed to generate test JWT", e);
    }
  }

  private String createNote(String token, String title, String body) {
    var createResponse =
        restTestClient
            .post()
            .uri("/api/notes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {"title": "%s", "body": "%s"}
                """
                    .formatted(title, body))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody()
            .jsonPath("$.id")
            .isNotEmpty()
            .returnResult();

    OpenApiContractValidator.assertResponseMatchesSpec(
        "POST", "/api/notes", 201, createResponse.getResponseBody(), "application/json");

    var tree = new ObjectMapper().readTree(createResponse.getResponseBody());
    return tree.get("id").asString();
  }

  @Test
  void createAndRetrieveNote() {
    var token = generateToken();
    var noteId = createNote(token, "Integration", "Test body");

    var result =
        restTestClient
            .get()
            .uri("/api/notes/" + noteId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.title")
            .isEqualTo("Integration")
            .returnResult();

    OpenApiContractValidator.assertResponseMatchesSpec(
        "GET", "/api/notes/" + noteId, 200, result.getResponseBody(), "application/json");
  }

  @Test
  void getNonexistentNoteReturns404() {
    var token = generateToken();

    var result =
        restTestClient
            .get()
            .uri("/api/notes/999999")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("$.title")
            .isEqualTo("Note Not Found")
            .returnResult();

    OpenApiContractValidator.assertResponseMatchesSpec(
        "GET", "/api/notes/999999", 404, result.getResponseBody(), "application/problem+json");
  }

  @Test
  void listNotesWithPagination() {
    var token = generateToken();
    createNote(token, "List Test 1", "Body 1");
    createNote(token, "List Test 2", "Body 2");

    var result =
        restTestClient
            .get()
            .uri("/api/notes?page=0&size=10")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.content")
            .isArray()
            .jsonPath("$.totalElements")
            .isNumber()
            .jsonPath("$.page")
            .isEqualTo(0)
            .returnResult();

    OpenApiContractValidator.assertResponseMatchesSpec(
        "GET", "/api/notes?page=0&size=10", 200, result.getResponseBody(), "application/json");
  }

  @Test
  void updateNote() {
    var token = generateToken();
    var noteId = createNote(token, "Before Update", "Old body");

    var result =
        restTestClient
            .put()
            .uri("/api/notes/" + noteId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {"title": "After Update", "body": "New body"}
                """)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.title")
            .isEqualTo("After Update")
            .jsonPath("$.body")
            .isEqualTo("New body")
            .jsonPath("$.updatedAt")
            .isNotEmpty()
            .returnResult();

    OpenApiContractValidator.assertResponseMatchesSpec(
        "PUT", "/api/notes/" + noteId, 200, result.getResponseBody(), "application/json");

    restTestClient
        .get()
        .uri("/api/notes/" + noteId)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.title")
        .isEqualTo("After Update");
  }

  @Test
  void deleteNote() {
    var token = generateToken();
    var noteId = createNote(token, "To Delete", "Body");

    restTestClient
        .delete()
        .uri("/api/notes/" + noteId)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus()
        .isNoContent();

    OpenApiContractValidator.assertResponseMatchesSpec(
        "DELETE", "/api/notes/" + noteId, 204, null, null);

    restTestClient
        .get()
        .uri("/api/notes/" + noteId)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void deleteNonexistentNoteReturns404() {
    var token = generateToken();

    var result =
        restTestClient
            .delete()
            .uri("/api/notes/999999")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("$.title")
            .isEqualTo("Note Not Found")
            .returnResult();

    OpenApiContractValidator.assertResponseMatchesSpec(
        "DELETE", "/api/notes/999999", 404, result.getResponseBody(), "application/problem+json");
  }
}
