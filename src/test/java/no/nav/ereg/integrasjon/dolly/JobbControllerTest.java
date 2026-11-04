package no.nav.ereg.integrasjon.dolly;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@EnableWireMock(@ConfigureWireMock(
        baseUrlProperties = "nais.api.url"
))
@AutoConfigureTestRestTemplate
@Import(TestSikkerhetsKonfigurasjon.class)
@Sql("classpath:db/create-tables.sql")
@ActiveProfiles("test")
class JobbControllerTest {

    @Container
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:23-slim").withInitScript("db/create-tables.sql");

    @Autowired
    TestRestTemplate testClient;

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    @ServiceConnection
    static void dynamicProperties(DynamicPropertyRegistry registry) throws IOException {
        final Path fil = tempDir.resolve("test.token");
        Files.writeString(fil, "test-token");
        registry.add("NAIS_SERVICE_ACCOUNT_TOKEN_PATH", fil::toString);
    }

    @Test
    public void kjørerJobbHvisIkkeAlleredeKjører() {
        stubFor(post("/graphql")
                .withRequestBody(containing("JobRunsList"))
                .willReturn(okJson("""
                        {
                          "data": {
                            "team": {
                              "environment": {
                                "job": {
                                  "runs": {
                                    "edges": [
                                      {
                                        "node": {
                                          "status": {
                                            "state": "SUCCEEDED"
                                          }
                                        }
                                      },
                                      {
                                        "node": {
                                          "status": {
                                            "state": "SUCCEEDED"
                                          }
                                        }
                                      },
                                      {
                                        "node": {
                                          "status": {
                                            "state": "FAILED"
                                          }
                                        }
                                      }
                                    ]
                                  }
                                }
                              }
                            }
                          }
                        }
                        """)));
        stubFor(post("/graphql").withRequestBody(containing("setUpDollyJob")).willReturn(okJson("""
                {
                  "data": {
                    "job": {
                      "id": "abcde"
                    }
                  }
                }
                """)));
        final ResponseEntity<Void> response = testClient.postForEntity("/api/start", new byte[1], Void.class);
        assertEquals(HttpStatusCode.valueOf(202), response.getStatusCode());
    }

    @Test
    public void kjørerIkkeHvisAlleredeKjører() {
        stubFor(post("/graphql").withRequestBody(containing("JobRunsList")).willReturn(okJson("""
                {
                  "data": {
                    "team": {
                      "environment": {
                        "job": {
                          "runs": {
                            "edges": [
                              {
                                "node": {
                                  "status": {
                                    "state": "PENDING"
                                  }
                                }
                              },
                              {
                                "node": {
                                  "status": {
                                    "state": "SUCCEEDED"
                                  }
                                }
                              },
                              {
                                "node": {
                                  "status": {
                                    "state": "SUCCEEDED"
                                  }
                                }
                              },
                              {
                                "node": {
                                  "status": {
                                    "state": "FAILED"
                                  }
                                }
                              }
                            ]
                          }
                        }
                      }
                    }
                  }
                }
                """)));
        resetAllRequests();
        final ResponseEntity<Void> response = testClient.postForEntity("/api/start", new byte[1], Void.class);
        assertTrue(response.getStatusCode().isError());
        verify(0, postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(containing("setUpDollyJob")));
    }

    @Test
    void feilSendesVidere() {
        stubFor(post("/graphql")
                .withRequestBody(containing("JobRunsList"))
                .willReturn(okJson("""
                        {
                          "data": {
                            "team": {
                              "environment": {
                                "job": {
                                  "runs": {
                                    "edges": [
                                      {
                                        "node": {
                                          "status": {
                                            "state": "SUCCEEDED"
                                          }
                                        }
                                      },
                                      {
                                        "node": {
                                          "status": {
                                            "state": "SUCCEEDED"
                                          }
                                        }
                                      },
                                      {
                                        "node": {
                                          "status": {
                                            "state": "FAILED"
                                          }
                                        }
                                      }
                                    ]
                                  }
                                }
                              }
                            }
                          }
                        }
                        """)));
        stubFor(post("/graphql").withRequestBody(containing("setUpDollyJob")).willReturn(okJson("""
                {
                  "data": {
                    "job": {
                      "id": "abcde"
                    }
                  },
                  "errors": [
                    {
                      "message": "Temporary error for testing purposes"
                    }
                  ]
                }
                """)));
        final ResponseEntity<Void> response = testClient.postForEntity("/api/start", new byte[1], Void.class);
        assertTrue(response.getStatusCode().isError());
    }
}