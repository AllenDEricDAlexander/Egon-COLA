package top.egon.cola.platform.idp.core.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsernameNormalizerTest {

    private final UsernameNormalizer normalizer = new UsernameNormalizer();

    @Test
    void normalizesUnicodeWhitespaceAndCaseForStableLookup() {
        assertEquals("alice", normalizer.normalize("  ＡLiCe  "));
    }

    @Test
    void rejectsBlankNormalizedUsername() {
        assertThrows(IllegalArgumentException.class, () ->
                normalizer.normalize("　 "));
    }
}
