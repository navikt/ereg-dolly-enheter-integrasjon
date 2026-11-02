package no.nav.ereg.integrasjon.dolly;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NaisApi {
    private static final String GRAPHQL_SEGMENT = "/graphql";

    private final HttpGraphQlClient graphqlClient;
    private static final String JOBB_PREFIKS = "ereg-ajourholdsfiler-lasting-enheter-";
    private static final String settVariabler = """
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

    private static final String listeEksisterendekjøringer = """
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

    public NaisApi(@Value("${nais.api.url}") String naisUrl) {
        graphqlClient = HttpGraphQlClient.builder(WebClient.builder().baseUrl(naisUrl + GRAPHQL_SEGMENT).build()).build();
    }

    public static List<JobbKjøringStatus> fraJson(final JsonNode jsonNode) {
        try {
            return jsonNode.map(array -> array.asArray().elements().stream().map(node -> JobbKjøringStatus.valueOf(node.get("node").get("status").get("state").stringValue(JobbKjøringStatus.UNKNOWN.name()))).toList());
        } catch (RuntimeException e) {
            log.warn("Fant ikke forventede elementer i {}", jsonNode);
            throw e;
        }
    }

    public void startBatch(final String filData, final String cluster, final String auraMiljø) {
        final Map<String, Object> argumenter = new HashMap<>();
        argumenter.put("environment", cluster);
        argumenter.put("job", JOBB_PREFIKS + auraMiljø);
        final List<JobbKjøringStatus> respons = graphqlClient.document(listeEksisterendekjøringer).variables(argumenter).retrieve("team.environment.job.runs.edges").toEntity(JsonNode.class).map(NaisApi::fraJson).block();
        if (respons != null && respons.stream().anyMatch(JobbKjøringStatus::kanVæreIGang)) {
            throw new IllegalStateException("Jobb er allerede i gang");
        }
        argumenter.put("dataUri", "data:application/octet-stream;base64," + filData);
        final ClientGraphQlResponse oppdatering = graphqlClient.document(settVariabler).variables(argumenter).execute().block();
        if (oppdatering == null || !oppdatering.isValid() || !oppdatering.getErrors().isEmpty()) {
            throw new RuntimeException("Feil i kall til Nais, " + (oppdatering == null ? null : oppdatering.getErrors()));
        }
    }
}
