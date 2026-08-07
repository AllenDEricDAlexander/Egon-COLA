package top.egon.cola.component.rpc.test.mockgateway;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class MockRoundRobinSelector {

    private final AtomicInteger sequence = new AtomicInteger();

    MockProviderEndpoint select(List<MockProviderEndpoint> endpoints) {
        if (endpoints.isEmpty()) {
            return null;
        }
        return endpoints.get(Math.floorMod(
                sequence.getAndIncrement(),
                endpoints.size()
        ));
    }
}
