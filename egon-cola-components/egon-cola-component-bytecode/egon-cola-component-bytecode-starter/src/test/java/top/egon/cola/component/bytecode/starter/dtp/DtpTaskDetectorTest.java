package top.egon.cola.component.bytecode.starter.dtp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtpTaskDetectorTest {

    @Test
    void detectsDtpWrappersWithoutLinkingTheDtpStarter() {
        DtpTaskDetector detector = new DtpTaskDetector();

        assertTrue(detector.instrumented(new top.egon.cola.component.dtp.context.DtpRunnable()));
        assertFalse(detector.instrumented((Runnable) () -> { }));
    }
}
