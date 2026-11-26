package co.edu.icesi.cineconecta.report;

import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Date;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;

@Repository
public class MongoConsumptionReportRepository {
    private final MongoTemplate mongo;

    public MongoConsumptionReportRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Agrega en Mongo para evitar transferir todas las sesiones a la aplicación. */
    public List<SubscriptionConsumptionBucket> summarizeBySubscriptionAndType(
            Instant desdeIncluido, Instant hastaExclusivo) {
        var aggregation = Aggregation.newAggregation(
                match(Criteria.where("startedAt")
                        .gte(Date.from(desdeIncluido))
                        .lt(Date.from(hastaExclusivo))),
                group("subscriptionId", "contentTypeSnapshot")
                        .count().as("sessionCount")
                        .sum("watchedSeconds").as("watchedSeconds")
                        .addToSet("profileId").as("profileIds"),
                project("sessionCount", "watchedSeconds", "profileIds")
                        .and("_id.subscriptionId").as("subscriptionId")
                        .and("_id.contentTypeSnapshot").as("contentType")
                        .andExclude("_id"));

        var documents = mongo.aggregate(aggregation, ViewingSessionDocument.COLLECTION, Document.class)
                .getMappedResults();
        List<SubscriptionConsumptionBucket> buckets = new ArrayList<>(documents.size());
        for (Document document : documents) {
            Object rawProfiles = document.get("profileIds");
            Set<Long> profileIds = new HashSet<>();
            if (rawProfiles instanceof List<?> profiles) {
                for (Object profile : profiles) {
                    if (profile instanceof Number number) {
                        profileIds.add(number.longValue());
                    }
                }
            }

            buckets.add(new SubscriptionConsumptionBucket(
                    number(document.get("subscriptionId")),
                    document.getString("contentType"),
                    number(document.get("sessionCount")),
                    number(document.get("watchedSeconds")),
                    Set.copyOf(profileIds)));
        }
        return buckets;
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
