package no.nav.ereg.integrasjon.dolly;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class Feilhåndtering {
    @ExceptionHandler(JobbKjørerAlleredeException.class)
    public ResponseEntity<String> håndterDobbelJobb() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "30").body("Jobb for entiteter kjører allerede, prøv igjen om litt");
    }
}
