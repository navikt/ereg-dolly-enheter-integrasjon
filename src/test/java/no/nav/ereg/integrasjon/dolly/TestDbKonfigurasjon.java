package no.nav.ereg.integrasjon.dolly;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.oracle.OracleContainer;

@TestConfiguration
public class TestDbKonfigurasjon {
    @Bean
    @ServiceConnection
    public OracleContainer oracleContainer() {
        return new OracleContainer("gvenzl/oracle-free:23-slim").withReuse(true);
    }
}
