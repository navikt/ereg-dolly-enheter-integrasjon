package no.nav.ereg.integrasjon.dolly;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequiredArgsConstructor
public class JobbController {
    private final NaisApi naisApi;

    @PostMapping(value = "/api/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void start(@RequestBody final byte[] inputFile) throws IOException {
        final String filData = Base64.getEncoder().encodeToString(inputFile);
        naisApi.startBatch(filData, "dev-fss", "q2");
    }
}
