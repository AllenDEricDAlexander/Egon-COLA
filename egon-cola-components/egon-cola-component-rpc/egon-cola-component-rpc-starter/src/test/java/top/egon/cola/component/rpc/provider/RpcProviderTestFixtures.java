package top.egon.cola.component.rpc.provider;

import com.google.protobuf.StringValue;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

final class RpcProviderTestFixtures {

    private RpcProviderTestFixtures() {
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0"
    )
    interface EchoContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    @EgonRpcProvider
    static class EchoProvider implements EchoContract {

        @Override
        public StringValue echo(StringValue request) {
            return StringValue.of("provider:" + request.getValue());
        }
    }

    static class NonProvider implements EchoContract {

        @Override
        public StringValue echo(StringValue request) {
            return request;
        }
    }
}
