package top.egon.cola.component.rpc.support;

import com.google.protobuf.StringValue;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

public final class RpcProviderTestFixtures {

    private RpcProviderTestFixtures() {
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0"
    )
    public interface EchoContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "2.0.0"
    )
    public interface EchoV2Contract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    @EgonRpcProvider
    public static class EchoProvider implements EchoContract {

        @Override
        public StringValue echo(StringValue request) {
            return StringValue.of("provider:" + request.getValue());
        }
    }

    public static class NonProvider implements EchoContract {

        @Override
        public StringValue echo(StringValue request) {
            return request;
        }
    }

    @EgonRpcProvider
    public static class EchoV2Provider implements EchoV2Contract {

        @Override
        public StringValue echo(StringValue request) {
            return StringValue.of("provider-v2:" + request.getValue());
        }
    }

    public interface PlainContract {

        StringValue echo(StringValue request);
    }

    @EgonRpcProvider
    public static class ContractlessProvider implements PlainContract {

        @Override
        public StringValue echo(StringValue request) {
            return request;
        }
    }

    @EgonRpcProvider
    public static class InterfacelessProvider {

        public StringValue echo(StringValue request) {
            return request;
        }
    }
}
