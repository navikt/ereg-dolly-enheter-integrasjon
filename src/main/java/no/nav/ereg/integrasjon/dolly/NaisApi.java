package no.nav.ereg.integrasjon.dolly;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
@Slf4j
public class NaisApi {
    public static final String GRAPHQL_SEGMENT = "/graphql";
    private final HttpGraphQlClient graphqlClient;
    public static final String SETT_VARIABLER = """
            mutation setUpDollyJob($job: String!, $dataUri: String!, $environment: String!) {
              updateJob (input: {
                name: $job
                teamSlug:"arbeidsforhold"
                environmentName: $environment
                environmentVariables: [
                  {
                    name: "inputFileLocation",
                    value: $dataUri
                  }
                ]
              }) {
                job {
                  id
                }
              }
            }
            """;
    public static final String LIST_EKSISTERENDE_KJØRINGER = """
            query JobRunsList($job: String!, $environment: String!) {
                team(slug: "arbeidsforhold") {
                    environment(name: $environment) {
                        job(name: $job) {
                            runs {
                                edges {
                                    node {
                                        status {
                                            state
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """;

    public NaisApi(@Value("${nais.api.url}") final String naisUrl, @Value("${NAIS_SERVICE_ACCOUNT_TOKEN_PATH}") final String naisTokenSti) {
        graphqlClient = HttpGraphQlClient.builder(WebClient.builder().baseUrl(naisUrl + NaisApi.GRAPHQL_SEGMENT).filter((request, next) -> next.exchange(ClientRequest.from(request).headers(headers -> headers.setBearerAuth(hentToken(naisTokenSti))).build())).clientConnector(new ReactorClientHttpConnector(HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000).responseTimeout(Duration.ofSeconds(30)))).build()).build();
    }

    private String hentToken(final String naisTokenSti) {
        try {
            try (final Stream<String> linjer = Files.lines(Path.of(naisTokenSti))) {
                return linjer.findFirst().orElseThrow();
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<JobbKjøringStatus> fraJson(final JsonNode jsonNode) {
        try {
            return jsonNode.map(array -> array.asArray().elements().stream().map(node -> JobbKjøringStatus.valueOf(node.get("node").get("status").get("state").stringValue(JobbKjøringStatus.UNKNOWN.name()))).toList());
        } catch (RuntimeException e) {
            log.warn("Fant ikke forventede elementer i {}", jsonNode);
            throw e;
        }
    }

    public void oppdaterOgStartJobb(final Map<String, Object> argumenter) {
        final ClientGraphQlResponse oppdatering = graphqlClient.document(SETT_VARIABLER).variables(argumenter).execute().block();
        if (oppdatering == null || !oppdatering.isValid() || !oppdatering.getErrors().isEmpty()) {
            throw new RuntimeException("Feil i kall til Nais, " + (oppdatering == null ? null : oppdatering.getErrors()));
        }
    }

    public void sjekkAtJobbIkkeKjører(final Map<String, Object> argumenter) throws JobbKjørerAlleredeException {
        final List<JobbKjøringStatus> respons = graphqlClient.document(LIST_EKSISTERENDE_KJØRINGER).variables(argumenter).retrieve("team.environment.job.runs.edges").toEntity(JsonNode.class).map(NaisApi::fraJson).block();
        if (respons != null && respons.stream().anyMatch(JobbKjøringStatus::kanVæreIGang)) {
            throw new JobbKjørerAlleredeException();
        }
    }
}