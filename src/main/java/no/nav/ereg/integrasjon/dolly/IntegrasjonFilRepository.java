package no.nav.ereg.integrasjon.dolly;

import no.nav.ereg.integrasjon.dolly.entitet.IntegrasjonFil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrasjonFilRepository extends JpaRepository<IntegrasjonFil, Long> {
}
