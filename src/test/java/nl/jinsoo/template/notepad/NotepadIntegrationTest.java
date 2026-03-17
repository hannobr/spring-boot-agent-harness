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
import nl.jinsoo.template.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class NotepadIntegrationTest {

  @Autowired private RestTestClient restTestClient;

  @Value("${jwt.secret-key}")
  private String jwtSecretKey;

  private String generateToken() {
    try {
      var signer = new MACSigner(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
      var claims =
          new JWTClaimsSet.Builder()
              .subject("test-user")
              .issueTime(Date.from(Instant.now()))
              .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
              .build();
      var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(signer);
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new RuntimeException("Failed to generate test JWT", e);
    }
  }

  @Test
  void createAndRetrieveNote() {
    var token = generateToken();
    var createResponse =
        restTestClient
            .post()
            .uri("/api/notes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {"title": "Integration", "body": "Test body"}
                """)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody()
            .jsonPath("$.id")
            .isNotEmpty()
            .jsonPath("$.title")
            .isEqualTo("Integration")
            .returnResult();

    var body = new String(createResponse.getResponseBody());
    var idStart = body.indexOf("\"id\":") + 5;
    var idEnd = body.indexOf(",", idStart);
    var noteId = body.substring(idStart, idEnd).trim();

    restTestClient
        .get()
        .uri("/api/notes/" + noteId)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.title")
        .isEqualTo("Integration");
  }

  @Test
  void getNonexistentNoteReturns404() {
    var token = generateToken();
    restTestClient
        .get()
        .uri("/api/notes/999999")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.title")
        .isEqualTo("Note Not Found");
  }
}
