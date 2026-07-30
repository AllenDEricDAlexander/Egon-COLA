package top.egon.cola.component.gateway.engine.transport;

import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayCancellationTest {

    @Test
    void cancelsOwnedResourcesAndReleasesUntransferredBuffersOnce() {
        GatewayCancellation cancellation = new GatewayCancellation();
        AtomicInteger disposed = new AtomicInteger();
        Disposable resource = disposed::incrementAndGet;
        NettyDataBufferFactory factory = new NettyDataBufferFactory(
                PooledByteBufAllocator.DEFAULT
        );
        DataBuffer abandoned = factory.wrap(new byte[]{1});
        DataBuffer transferred = factory.wrap(new byte[]{2});

        cancellation.register(resource);
        cancellation.own(abandoned);
        cancellation.own(transferred);
        assertTrue(cancellation.transfer(transferred));

        assertTrue(cancellation.cancel());
        assertFalse(cancellation.cancel());

        assertEquals(1, disposed.get());
        assertFalse(((PooledDataBuffer) abandoned).isAllocated());
        assertTrue(((PooledDataBuffer) transferred).isAllocated());
        ((PooledDataBuffer) transferred).release();
    }

    @Test
    void immediatelyCancelsResourcesRegisteredAfterCancellation() {
        GatewayCancellation cancellation = new GatewayCancellation();
        AtomicInteger disposed = new AtomicInteger();
        cancellation.cancel();

        cancellation.register(disposed::incrementAndGet);

        assertEquals(1, disposed.get());
    }
}
