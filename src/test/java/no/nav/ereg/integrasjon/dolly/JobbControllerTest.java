package no.nav.ereg.integrasjon.dolly;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@EnableWireMock(@ConfigureWireMock(
        baseUrlProperties = "nais.api.url"
))
@AutoConfigureTestRestTemplate
@Import({TestSikkerhetsKonfigurasjon.class, TestDbKonfigurasjon.class})
@ActiveProfiles("test")
class JobbControllerTest {
    public static final String TEST_DATABASE_SPØRRING = "SELECT COUNT(*) FROM T_DOLLY_INTEGRASJON_FIL";
    @Autowired
    TestRestTemplate testClient;

    @TempDir
    static Path tempDir;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    @ServiceConnection
    static void dynamicProperties(DynamicPropertyRegistry registry) throws IOException {
        final Path fil = tempDir.resolve("test.token");
        Files.writeString(fil, "test-token");
        registry.add("NAIS_SERVICE_ACCOUNT_TOKEN_PATH", fil::toString);
        registry.add("aura.environment", () -> "INTEGRASJONSTEST");
    }

    @BeforeAll
    static void lagTabellOmDenMangler(@Autowired JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE T_DOLLY_INTEGRASJON_FIL (
                        FIL_ID NUMBER(19,0) GENERATED ALWAYS AS IDENTITY,
                        TIDSPUNKT_LAGET TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
                        INNHOLD BLOB NOT NULL,
                        CONSTRAINT PK_FILE_ARCHIVE PRIMARY KEY (FIL_ID)
                    )
                    """);
        } catch (final DataAccessException e) {
            if (!(e.getCause() instanceof SQLException sqlEx &&
                    sqlEx.getErrorCode() == 955)) {
                throw e;
            }
        }
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
        final Long antallFør = jdbcTemplate.queryForObject(TEST_DATABASE_SPØRRING, Long.class);
        final ResponseEntity<Void> response = testClient.postForEntity("/api/start", new byte[1], Void.class);
        final Long antallEtter = jdbcTemplate.queryForObject(TEST_DATABASE_SPØRRING, Long.class);
        assertEquals(HttpStatusCode.valueOf(202), response.getStatusCode());
        assertEquals(antallFør + 1, antallEtter);
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