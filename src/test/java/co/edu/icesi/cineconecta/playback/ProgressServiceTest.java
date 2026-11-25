package co.edu.icesi.cineconecta.playback;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressServiceTest {
    @ParameterizedTest(name = "{0}/{1} -> {2}")
    @CsvSource({
            "0, 1000, false",
            "899, 1000, false",
            "900, 1000, true",
            "1000, 1000, true",
            "5237, 5820, false",  // 89.98 %
            "5238, 5820, true",   // 90.0 %
            "10, 0, false"
    })
    void ninetyPercentBoundary(int position, int duration, boolean expected) {
        assertEquals(expected, ProgressService.reachesCompletion(position, duration));
    }
}
