package no.nav.ereg.integrasjon.dolly;

import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NaisApi {
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
              }) {
                job {
                  id
                }
              }
            }
            """;
    private static final String kjøringsInstruks = """
            mutation startDollyJob($job: String!, $runName: String!, $environment: String!) {
              triggerJob (input: {
                name: $job,
                teamSlug: "arbeidsforhold",
                environmentName: $environment,
                runName: $runName
              }) {
                jobRun {
                  name
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

    public NaisApi() {
        graphqlClient = HttpGraphQlClient.builder(WebClient.builder().baseUrl("https://console.nav.cloud.nais.io/graphql").build()).build();
    }

    public void startBatch(final String filData, final String cluster, final String auraMiljø) {
        final Map<String, Object> argumenter = new HashMap<>();
        argumenter.put("environment", cluster);
        argumenter.put("job", JOBB_PREFIKS + auraMiljø);
        final List<JobbKjøringStatus> respons = graphqlClient.document(listeEksisterendekjøringer).variables(argumenter).retrieve("team.environment.job.runs.edges[*].node.status.state").toEntityList(JobbKjøringStatus.class).block();
        if (respons != null && respons.stream().anyMatch(JobbKjøringStatus::kanVæreIGang)) {
            throw new IllegalStateException("Jobb er allerede i gang");
        }
        argumenter.put("dataUri", "data:application/octet-stream;base64," + filData);
        graphqlClient.document(settVariabler).variables(argumenter).execute().block();
        argumenter.remove("dataUri");
        argumenter.put("runName", JOBB_PREFIKS + auraMiljø + "for-dolly-" + Instant.now().getEpochSecond());
        graphqlClient.document(kjøringsInstruks).variables(argumenter).execute().block();
    }
}
