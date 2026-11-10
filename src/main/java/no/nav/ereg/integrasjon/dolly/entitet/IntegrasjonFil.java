package no.nav.ereg.integrasjon.dolly.entitet;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "T_DOLLY_INTEGRASJON_FIL")
public class IntegrasjonFil {

    protected IntegrasjonFil() {

    }

    private IntegrasjonFil(final byte[] innhold) {
        this.innhold = innhold;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FIL_ID")
    private Long id;

    @Column(name = "TIDSPUNKT_LAGET", nullable = false, updatable = false, insertable = false)
    private Instant tidspunktLaget;

    @Lob
    @Column(name = "INNHOLD", nullable = false)
    private byte[] innhold;

    public static IntegrasjonFil fra(final InputStream strøm) throws IOException {
        return new IntegrasjonFil(strøm.readAllBytes());
    }
}