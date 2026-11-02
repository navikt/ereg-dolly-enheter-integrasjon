package no.nav.ereg.integrasjon.dolly;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock(@ConfigureWireMock(
        baseUrlProperties = "nais.api.url"
))
@AutoConfigureTestRestTemplate
class JobbControllerTest {
    @Autowired
    TestRestTemplate testClient;

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
        final ResponseEntity<Void> response = testClient.postForEntity("/api/start", new byte[1], Void.class);
        assertTrue(response.getStatusCode().isError());
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
                      "message": "Temporary error"
                    }
                  ]
                }
                """)));
        final ResponseEntity<Void> response = testClient.postForEntity("/api/start", new byte[1], Void.class);
        assertTrue(response.getStatusCode().isError());
    }
}