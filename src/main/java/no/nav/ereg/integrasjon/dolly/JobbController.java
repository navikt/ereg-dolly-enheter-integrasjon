package no.nav.ereg.integrasjon.dolly;

import lombok.RequiredArgsConstructor;
import no.nav.ereg.integrasjon.dolly.entitet.IntegrasjonFil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class JobbController {
    private final EregEnheterJobbService eregEnheterJobbService;
    private final String miljø;

    public JobbController(final EregEnheterJobbService eregEnheterJobbService, @Value("${aura.environment}") final String miljø) {
        this.eregEnheterJobbService = eregEnheterJobbService;
        this.miljø = miljø;
    }

    @PostMapping(value = "/api/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void start(final java.io.InputStream inputStream) throws IOException, JobbKjørerAlleredeException {
        eregEnheterJobbService.startBatch(IntegrasjonFil.fra(inputStream), "dev-fss", miljø);
    }
}
