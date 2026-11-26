package co.edu.icesi.cineconecta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Configuration
public class MongoTimeoutConfig {
    @Bean
    MongoClientSettingsBuilderCustomizer mongoTimeouts(
            @Value("${cineconecta.mongo.server-selection-timeout-ms:3000}") long serverSelectionMillis,
            @Value("${cineconecta.mongo.connect-timeout-ms:3000}") int connectMillis,
            @Value("${cineconecta.mongo.read-timeout-ms:5000}") int readMillis) {
        return builder -> builder
                .applyToClusterSettings(c -> c.serverSelectionTimeout(serverSelectionMillis, MILLISECONDS))
                .applyToSocketSettings(s -> s.connectTimeout(connectMillis, MILLISECONDS)
                        .readTimeout(readMillis, MILLISECONDS))
                .applyToConnectionPoolSettings(p -> p.maxWaitTime(serverSelectionMillis, MILLISECONDS));
    }
}
