package no.nav.ereg.integrasjon.dolly;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequiredArgsConstructor
public class JobbController {
    private final NaisApi naisApi;

    @PostMapping(value = "/api/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void start(final java.io.InputStream inputStream) throws IOException {
        final byte[] inputFile = inputStream.readNBytes(50_001);
        if (inputFile.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (inputFile.length > 50_000) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Tar for nå kun imot mindre bestillinger");
        }
        final String filData = Base64.getEncoder().encodeToString(inputFile);
        naisApi.startBatch(filData, "dev-fss", "q2");
    }
}
