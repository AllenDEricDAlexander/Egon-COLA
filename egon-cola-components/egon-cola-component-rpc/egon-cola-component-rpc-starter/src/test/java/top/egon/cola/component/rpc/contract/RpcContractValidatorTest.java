package top.egon.cola.component.rpc.contract;

import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.MissingDescriptorGrpc;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.StreamingFixtureGrpc;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcContractValidatorTest {

    private final RpcContractValidator validator = new RpcContractValidator();

    @Test
    void shouldValidateAndCacheUnaryProtobufContract() throws Exception {
        RpcContractDescriptor first = validator.validate(ValidContract.class);
        RpcContractDescriptor second = validator.validate(ValidContract.class);

        assertThat(first).isSameAs(second);
        assertThat(first.serviceName())
                .isEqualTo("egon.rpc.fixture.v1.UnaryFixtureService");
        assertThat(first.group()).isEqualTo("test");
        assertThat(first.version()).isEqualTo("1.0.0");
        assertThat(first.method(
                ValidContract.class.getMethod("echo", StringValue.class)
        ).fullMethodName()).isEqualTo(
                "egon.rpc.fixture.v1.UnaryFixtureService/Echo"
        );
    }

    @Test
    void shouldRejectMissingAnnotationsAndGeneratedDescriptor() {
        assertInvalid(MissingServiceAnnotation.class);
        assertInvalid(MissingMethodAnnotation.class);
        assertInvalid(MissingGeneratedDescriptor.class);
        assertInvalid(UnknownProtoMethod.class);
    }

    @Test
    void shouldRejectUnsupportedMethodShapes() {
        assertInvalid(StreamingContract.class);
        assertInvalid(ZeroArgumentContract.class);
        assertInvalid(TwoArgumentContract.class);
        assertInvalid(NonMessageContract.class);
        assertInvalid(OverloadedContract.class);
    }

    @Test
    void shouldRejectMessageTypeMismatch() {
        assertInvalid(RequestMismatchContract.class);
        assertInvalid(ResponseMismatchContract.class);
    }

    @Test
    void shouldRejectBlankServiceIdentity() {
        assertInvalid(BlankGroupContract.class);
        assertInvalid(BlankVersionContract.class);
    }

    private void assertInvalid(Class<?> contractType) {
        assertThatThrownBy(() -> validator.validate(contractType))
                .isInstanceOfSatisfying(EgonRpcException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo(EgonRpcErrorCode.RPC_INVALID_CONTRACT)
                );
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0"
    )
    interface ValidContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    interface MissingServiceAnnotation {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class)
    interface MissingMethodAnnotation {

        StringValue echo(StringValue request);
    }

    @EgonRpcService(grpcClass = MissingDescriptorGrpc.class)
    interface MissingGeneratedDescriptor {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class)
    interface UnknownProtoMethod {

        @EgonRpcMethod(name = "Missing")
        StringValue echo(StringValue request);
    }

    @EgonRpcService(grpcClass = StreamingFixtureGrpc.class)
    interface StreamingContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class)
    interface ZeroArgumentContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo();
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class)
    interface TwoArgumentContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue first, StringValue second);
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class)
    interface NonMessageContract {

        @EgonRpcMethod(name = "Echo")
        String echo(String request);
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class)
    interface RequestMismatchContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(Int32Value request);
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class)
    interface ResponseMismatchContract {

        @EgonRpcMethod(name = "Echo")
        Int32Value echo(StringValue request);
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class)
    interface OverloadedContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);

        @EgonRpcMethod(name = "Echo")
        Int32Value echo(Int32Value request);
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class, group = " ")
    interface BlankGroupContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    @EgonRpcService(grpcClass = UnaryFixtureGrpc.class, version = "")
    interface BlankVersionContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }
}
