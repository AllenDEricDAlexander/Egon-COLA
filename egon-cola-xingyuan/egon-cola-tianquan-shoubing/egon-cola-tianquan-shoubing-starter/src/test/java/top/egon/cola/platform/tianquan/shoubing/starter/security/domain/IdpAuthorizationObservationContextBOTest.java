package top.egon.cola.platform.tianquan.shoubing.starter.security.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

class IdpAuthorizationObservationContextBOTest {
    @Test
    void concurrentDeniesDeliverOnceAndWaitForConfirmation() throws Exception {
        var context = new IdpAuthorizationObservationContextBO();
        var calls = new AtomicInteger();
        var firstEntered = new CountDownLatch(1);
        var duplicateEntered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        BooleanSupplier handoff = () -> {
            if (calls.incrementAndGet() == 1) { firstEntered.countDown(); }
            else { duplicateEntered.countDown(); }
            try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
            return true;
        };
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> context.denyOnce(handoff));
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
            var second = workers.submit(() -> context.denyOnce(handoff));
            try {
                assertFalse(duplicateEntered.await(200, TimeUnit.MILLISECONDS));
                assertEquals(1, calls.get());
                assertFalse(first.isDone());
                assertFalse(second.isDone());
            } finally { release.countDown(); }
            assertTrue(first.get(5, TimeUnit.SECONDS));
            assertTrue(second.get(5, TimeUnit.SECONDS));
            assertTrue(context.isConfirmed());
        } finally { release.countDown(); }
    }

    @Test
    void failedCannotBecomeConfirmedOrRetryHandoff() {
        var context = new IdpAuthorizationObservationContextBO();
        var calls = new AtomicInteger();
        assertFalse(context.denyOnce(() -> { calls.incrementAndGet(); return false; }));
        assertFalse(context.denyOnce(() -> { calls.incrementAndGet(); return true; }));
        assertEquals(1, calls.get());
        assertTrue(context.isFailed());
        assertFalse(context.isConfirmed());
    }

    @Test
    void closedNewContextCannotPerformHandoff() {
        var context = new IdpAuthorizationObservationContextBO();
        var calls = new AtomicInteger();
        context.close();
        assertFalse(context.isActive());
        assertFalse(context.denyOnce(() -> { calls.incrementAndGet(); return true; }));
        assertEquals(0, calls.get());
        assertFalse(context.isConfirmed());
    }

    @Test
    void throwingOrReentrantHandoffIsPermanentlyFailed() {
        var throwing = new IdpAuthorizationObservationContextBO();
        assertFalse(throwing.denyOnce(() -> { throw new IllegalStateException("controlled transport failure"); }));
        assertTrue(throwing.isFailed());
        assertFalse(throwing.denyOnce(() -> true));
        var reentrant = new IdpAuthorizationObservationContextBO();
        assertFalse(reentrant.denyOnce(() -> { reentrant.denyOnce(() -> true); return true; }));
        assertTrue(reentrant.isFailed());
        assertFalse(reentrant.isConfirmed());
    }

    @Test
    void fatalHandoffErrorKeepsThePrimaryFailureAndCannotBeRetried() {
        var context = new IdpAuthorizationObservationContextBO();
        var failure = new AssertionError("controlled producer error");
        assertSame(failure, assertThrows(AssertionError.class, () -> context.denyOnce(() -> { throw failure; })));
        assertTrue(context.isFailed());
        assertFalse(context.denyOnce(() -> true));
    }

    @Test
    void invalidSupplierFailsClosedAndStateIsNotPublishedAsJson() throws Exception {
        var context = new IdpAuthorizationObservationContextBO();
        assertThrows(IllegalArgumentException.class, () -> context.denyOnce(null));
        assertTrue(context.isFailed());
        assertFalse(context.denyOnce(() -> true));
        assertThrows(com.fasterxml.jackson.databind.exc.InvalidDefinitionException.class,
                () -> new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(context));
    }
}
