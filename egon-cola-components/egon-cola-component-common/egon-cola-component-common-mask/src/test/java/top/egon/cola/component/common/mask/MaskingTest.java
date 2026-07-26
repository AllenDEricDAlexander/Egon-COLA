package top.egon.cola.component.common.mask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskingTest {

    @Test
    void mobileMasksMiddleDigits() {
        assertEquals("138****8000", Masking.mobile("13812348000"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    void mobileNeverEchoesInputAtAnyLength(int length) {
        String input = "123456789012".substring(0, length);

        String masked = Masking.mobile(input);

        assertFalse(masked.contains(input), "masked output still contains the whole input: " + masked);
        assertTrue(masked.indexOf('*') >= 0, "masked output hides nothing: " + masked);
    }

    @Test
    void mobileFullyMasksTooShortInput() {
        assertEquals("**", Masking.mobile("12"));
    }

    @Test
    void mobileUsesConservativeWindowBelowCanonicalLength() {
        assertEquals("1*****7", Masking.mobile("1234567"));
    }

    @Test
    void keepAroundFullyMasksWhenWindowCoversWholeValue() {
        assertEquals("****", Masking.keepAround("abcd", MaskRule.keepAround(2, 2)));
        assertEquals("***", Masking.keepAround("abc", MaskRule.keepAround(3, 4)));
    }

    @Test
    void emailFullyMasksShortLocalPart() {
        assertEquals("**@example.com", Masking.email("ab@example.com"));
    }

    @Test
    void emailMasksLocalName() {
        assertEquals("m***o@example.com", Masking.email("mario@example.com"));
    }

    @Test
    void keepAroundMasksByRule() {
        assertEquals("ab****gh", Masking.keepAround("abcdefgh", MaskRule.keepAround(2, 2)));
    }

    @Test
    void nullInputReturnsNull() {
        assertNull(Masking.mobile(null));
    }
}
