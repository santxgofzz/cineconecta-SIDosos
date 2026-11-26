package co.edu.icesi.cineconecta.config;

import com.mongodb.MongoClientSettings;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MongoTimeoutConfigTest {
    @Test
    void appliesShortTimeoutsSoAnUnavailableMongoFailsFast() {
        MongoClientSettings.Builder builder = MongoClientSettings.builder();
        new MongoTimeoutConfig().mongoTimeouts(3000, 3000, 5000).customize(builder);
        MongoClientSettings settings = builder.build();

        assertEquals(3000, settings.getClusterSettings().getServerSelectionTimeout(MILLISECONDS));
        assertEquals(3000, settings.getSocketSettings().getConnectTimeout(MILLISECONDS));
        assertEquals(5000, settings.getSocketSettings().getReadTimeout(MILLISECONDS));
        assertEquals(3000, settings.getConnectionPoolSettings().getMaxWaitTime(MILLISECONDS));
    }
}
