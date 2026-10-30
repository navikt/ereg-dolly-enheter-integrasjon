package no.nav.ereg.integrasjon.dolly;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequiredArgsConstructor
public class JobbController {
    private final NaisApi naisApi;

    @PostMapping(value = "/api/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void start(@RequestPart MultipartFile opplastetFil) throws IOException {
        final String filData = Base64.getEncoder().encodeToString(opplastetFil.getBytes());
        naisApi.startBatch(filData, "dev-fss", "q2");
    }
}
