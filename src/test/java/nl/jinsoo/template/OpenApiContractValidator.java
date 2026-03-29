package nl.jinsoo.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Validates HTTP responses against the committed OpenAPI spec ({@code
 * docs/generated/openapi.json}).
 *
 * <p>Use in integration tests ({@code @SpringBootTest(RANDOM_PORT)}) to verify that actual API
 * responses match the contract. This complements {@code scripts/harness/check-openapi-drift} which
 * checks that the spec matches the running app — this validator checks that individual responses
 * match the spec.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var result = restTestClient.get().uri("/api/notes/1")
 *     .exchange()
 *     .expectStatus().isOk()
 *     .expectBody()
 *     .returnResult();
 *
 * OpenApiContractValidator.assertResponseMatchesSpec(
 *     "GET", "/api/notes/1", 200, result.getResponseBody(), "application/json");
 * }</pre>
 */
@NullMarked
public final class OpenApiContractValidator {

  private static final OpenApiInteractionValidator VALIDATOR =
      OpenApiInteractionValidator.createForSpecificationUrl(
              Path.of("docs/generated/openapi.json").toUri().toString())
          .withLevelResolver(
              LevelResolver.create()
                  .withLevel("validation.request", ValidationReport.Level.IGNORE)
                  .build())
          .build();

  private OpenApiContractValidator() {}

  /**
   * Asserts that an HTTP response matches the OpenAPI spec for the given endpoint.
   *
   * @param method HTTP method (GET, POST, PUT, DELETE, PATCH)
   * @param path request path, optionally with query string (e.g. {@code /api/notes?page=0&size=10})
   * @param status HTTP status code of the response
   * @param body response body bytes, or {@code null} for empty responses (e.g. 204)
   * @param contentType response Content-Type, or {@code null} for empty responses
   */
  public static void assertResponseMatchesSpec(
      String method, String path, int status, @Nullable byte[] body, @Nullable String contentType) {
    var uri = URI.create("http://localhost" + path);

    var requestBuilder = requestBuilder(method, uri.getPath());

    var query = uri.getQuery();
    if (query != null) {
      for (var param : query.split("&")) {
        var parts = param.split("=", 2);
        requestBuilder.withQueryParam(parts[0], parts.length > 1 ? parts[1] : "");
      }
    }

    var responseBuilder = SimpleResponse.Builder.status(status);
    if (body != null && body.length > 0) {
      responseBuilder.withBody(new String(body, StandardCharsets.UTF_8));
    }
    if (contentType != null) {
      responseBuilder.withContentType(contentType);
    }

    var report = VALIDATOR.validate(requestBuilder.build(), responseBuilder.build());

    assertThat(report.hasErrors())
        .withFailMessage(
            () ->
                "OpenAPI contract violations for %s %s (status %d):\n%s"
                    .formatted(
                        method,
                        path,
                        status,
                        report.getMessages().stream()
                            .map(Object::toString)
                            .collect(Collectors.joining("\n"))))
        .isFalse();
  }

  private static SimpleRequest.Builder requestBuilder(String method, String path) {
    return switch (method) {
      case "GET" -> SimpleRequest.Builder.get(path);
      case "POST" -> SimpleRequest.Builder.post(path);
      case "PUT" -> SimpleRequest.Builder.put(path);
      case "DELETE" -> SimpleRequest.Builder.delete(path);
      case "PATCH" -> SimpleRequest.Builder.patch(path);
      default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
    };
  }
}
