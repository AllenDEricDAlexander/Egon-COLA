package top.egon.cola.component.rpc.test.mockgateway;

import io.grpc.Context;
import io.grpc.Metadata;

final class MockGatewayCallContext {

    static final Context.Key<Metadata> METADATA =
            Context.key("mock-gateway-metadata");

    private MockGatewayCallContext() {
    }
}
