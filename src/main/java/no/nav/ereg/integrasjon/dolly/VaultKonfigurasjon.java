package no.nav.ereg.integrasjon.dolly;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
@Profile("!test")
public final class VaultKonfigurasjon implements JdbcConnectionDetails {
    @Override
    public String getJdbcUrl() {
        return fil("/var/run/secrets/oracle_config/jdbc_url");
    }

    @Override
    public String getUsername() {
        return fil("/var/run/secrets/oracle_creds/username");
    }

    @Override
    public String getPassword() {
        return fil("/var/run/secrets/oracle_creds/password");
    }

    private String fil(final String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    JdbcConnectionDetails jdbcConnectionDetails() {
        return this;
    }
}
