package co.edu.icesi.cineconecta.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/** Evita mezclar repositorios de JPA y MongoDB. */
@Configuration
@EntityScan(basePackages = "co.edu.icesi.cineconecta.persistence.oracle.entity")
@EnableJpaRepositories(basePackages = "co.edu.icesi.cineconecta.persistence.oracle.repository")
@EnableMongoRepositories(basePackages = "co.edu.icesi.cineconecta.persistence.mongo.repository")
public class PersistenceConfig {
}
