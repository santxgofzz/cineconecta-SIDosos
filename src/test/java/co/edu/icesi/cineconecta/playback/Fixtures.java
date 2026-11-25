package co.edu.icesi.cineconecta.playback;

import co.edu.icesi.cineconecta.integration.CatalogEntry;
import co.edu.icesi.cineconecta.integration.ProfileAccess;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

/** Datos sintéticos compartidos por las pruebas de reproducción. */
final class Fixtures {
    private Fixtures() {}

    static final long ACCOUNT = 1L;
    static final long OTHER_ACCOUNT = 2L;
    static final long PROFILE = 15L;          // máx. 13 años
    static final long KIDS_PROFILE = 16L;     // máx. 7 años
    static final long INACTIVE_PROFILE = 17L;
    static final long FOREIGN_PROFILE = 21L;  // de OTHER_ACCOUNT
    static final long SUBSCRIPTION = 8L;

    static ProfileAccess profile() { return new ProfileAccess(PROFILE, ACCOUNT, 13, true); }
    static ProfileAccess kids() { return new ProfileAccess(KIDS_PROFILE, ACCOUNT, 7, true); }
    static ProfileAccess inactive() { return new ProfileAccess(INACTIVE_PROFILE, ACCOUNT, 18, false); }
    static ProfileAccess foreign() { return new ProfileAccess(FOREIGN_PROFILE, OTHER_ACCOUNT, 18, true); }

    static CatalogEntry movie() {
        return new CatalogEntry("CNT-001", CatalogEntry.MOVIE, "La ruta del viento", "published", 12, 1000, null);
    }

    static CatalogEntry adultMovie() {
        return new CatalogEntry("CNT-018", CatalogEntry.MOVIE, "Sombras", "published", 18, 6000, null);
    }

    static CatalogEntry retiredMovie() {
        return new CatalogEntry("CNT-008", CatalogEntry.MOVIE, "Retirada", "retired", 0, 3000, null);
    }

    static CatalogEntry draftDoc() {
        return new CatalogEntry("CNT-007", CatalogEntry.DOCUMENTARY, "Borrador", "draft", 0, 3000, null);
    }

    static CatalogEntry series() {
        return new CatalogEntry("CNT-002", CatalogEntry.SERIES, "Caminos del mar", "published", 7, null, List.of(
                new CatalogEntry.Season(1, List.of(
                        new CatalogEntry.Episode("EP-002-01", 1, "La llegada", 2400),
                        new CatalogEntry.Episode("EP-002-02", 2, "El viaje", 2520))),
                new CatalogEntry.Season(2, List.of(
                        new CatalogEntry.Episode("EP-002-03", 1, "El regreso", 2280)))));
    }

    /** Reloj controlable para simular el paso del tiempo real. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) { this.now = start; }

        void advance(Duration d) { now = now.plus(d); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
