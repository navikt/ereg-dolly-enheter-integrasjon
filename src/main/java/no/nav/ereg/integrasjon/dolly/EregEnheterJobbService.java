package no.nav.ereg.integrasjon.dolly;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.ereg.integrasjon.dolly.entitet.IntegrasjonFil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EregEnheterJobbService {

    private static final String JOBB_PREFIKS = "ereg-ajourholdsfiler-lasting-enheter-";

    private final NaisApi naisApi;
    private final IntegrasjonFilRepository integrasjonFilRepository;

    public void startBatch(final IntegrasjonFil filData, final String cluster, final String auraMiljø) throws JobbKjørerAlleredeException {
        final Map<String, Object> argumenter = new HashMap<>();
        argumenter.put("environment", cluster);
        argumenter.put("job", JOBB_PREFIKS + auraMiljø);
        naisApi.sjekkAtJobbIkkeKjører(argumenter);
        log.info("Før save er data {}, innhold lengde er {}", filData, filData.getInnhold().length);
        final IntegrasjonFil lagretFilData = integrasjonFilRepository.save(filData);
        argumenter.put("dataUri", "ereg-dolly-db:" + lagretFilData.getId());
        naisApi.oppdaterOgStartJobb(argumenter);
    }
}
