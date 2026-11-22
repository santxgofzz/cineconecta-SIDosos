package co.edu.icesi.cineconecta.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcAccountDataGatewayTest {
    @ParameterizedTest
    @CsvSource(value = {"1,true", "Y,true", "s,true", "T,true", "0,false", "N,false", "NULL,false"},
            nullValues = "NULL")
    void parsesActiveFlag(String raw, boolean expected) {
        assertEquals(expected, JdbcAccountDataGateway.isActive(raw));
    }

    @ParameterizedTest
    @CsvSource({"STARTS_AT <= ?", "ENDS_AT > ?"})
    void subscriptionWindowIsStartInclusiveEndExclusive(String fragment) {
        assertTrue(JdbcAccountDataGateway.SQL_ACTIVE_SUBSCRIPTION.contains(fragment));
    }
}
