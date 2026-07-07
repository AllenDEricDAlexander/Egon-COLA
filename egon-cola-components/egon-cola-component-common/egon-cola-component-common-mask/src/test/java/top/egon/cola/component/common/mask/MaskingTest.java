package top.egon.cola.component.common.mask;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MaskingTest {

    @Test
    void mobileMasksMiddleDigits() {
        assertEquals("138****8000", Masking.mobile("13812348000"));
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
