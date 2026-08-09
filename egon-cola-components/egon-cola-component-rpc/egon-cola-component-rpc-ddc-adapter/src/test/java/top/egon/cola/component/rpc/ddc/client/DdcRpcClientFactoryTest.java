package top.egon.cola.component.rpc.ddc.client;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigRuntimeServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcManagementServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceRegistryServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.FindConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.FindConfigResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServicesRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServicesResponse;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigResponse;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRpcClientFactoryTest {

    @Test
    void createsIndependentPlaintextClientsForAllThreeDdcCapabilities()
            throws Exception {
        AtomicReference<PullConfigRequest> pullRequest = new AtomicReference<>();
        AtomicReference<GetServicesRequest> servicesRequest = new AtomicReference<>();
        AtomicReference<FindConfigRequest> findRequest = new AtomicReference<>();
        Server server = NettyServerBuilder.forPort(0)
                .addService(configService(pullRequest))
                .addService(registryService(servicesRequest))
                .addService(managementService(findRequest))
                .build()
                .start();
        try {
            DdcRpcClientFactory factory = factory(server.getPort());
            try (var config = factory.configClient();
                 var registry = factory.registryClient();
                 var management = factory.managementClient()) {
                assertThat(config.client()).isNotNull();
                config.client().pull();
                registry.client().getServiceKeys(query());
                management.client().findConfig(
                        new DdcManagementConfigQuery("biz", "test", "app"));
            }

            assertThat(pullRequest.get().getScope().getBizCode())
                    .isEqualTo("biz");
            assertThat(servicesRequest.get().getQuery().getServiceKind())
                    .isEqualTo(top.egon.cola.component.rpc.ddc.contract.proto.v1
                            .DdcServiceKind.DDC_SERVICE_KIND_RPC_PROVIDER);
            assertThat(findRequest.get().getScope().getAppCode())
                    .isEqualTo("app");
        } finally {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private DdcRpcClientFactory factory(int port) {
        DdcRpcProperties rpc = new DdcRpcProperties();
        rpc.setTarget("localhost:" + port);
        rpc.getAuth().setEnabled(false);
        DdcProperties ddc = new DdcProperties();
        ddc.setBizCode("biz");
        ddc.setEnv("test");
        ddc.setAppCode("app");
        return new DdcRpcClientFactory(
                rpc,
                ddc,
                new RpcProcessIdentity(
                        "test", "test", "127.0.0.1", 1, "instance-1")
        );
    }

    private DdcServiceQuery query() {
        return new DdcServiceQuery(
                "biz", "test", "app", DdcServiceKind.RPC_PROVIDER,
                "grpc", null, null, null
        );
    }

    private DdcConfigRuntimeServiceGrpc.DdcConfigRuntimeServiceImplBase
            configService(AtomicReference<PullConfigRequest> observed) {
        return new DdcConfigRuntimeServiceGrpc
                .DdcConfigRuntimeServiceImplBase() {
            @Override
            public void pullConfig(
                    PullConfigRequest request,
                    StreamObserver<PullConfigResponse> observer) {
                observed.set(request);
                observer.onNext(PullConfigResponse.getDefaultInstance());
                observer.onCompleted();
            }
        };
    }

    private DdcServiceRegistryServiceGrpc.DdcServiceRegistryServiceImplBase
            registryService(AtomicReference<GetServicesRequest> observed) {
        return new DdcServiceRegistryServiceGrpc
                .DdcServiceRegistryServiceImplBase() {
            @Override
            public void getServices(
                    GetServicesRequest request,
                    StreamObserver<GetServicesResponse> observer) {
                observed.set(request);
                observer.onNext(GetServicesResponse.newBuilder()
                        .setQuery(request.getQuery())
                        .build());
                observer.onCompleted();
            }
        };
    }

    private DdcManagementServiceGrpc.DdcManagementServiceImplBase
            managementService(AtomicReference<FindConfigRequest> observed) {
        return new DdcManagementServiceGrpc.DdcManagementServiceImplBase() {
            @Override
            public void findConfig(
                    FindConfigRequest request,
                    StreamObserver<FindConfigResponse> observer) {
                observed.set(request);
                observer.onNext(FindConfigResponse.newBuilder()
                        .setFound(false)
                        .build());
                observer.onCompleted();
            }
        };
    }
}
