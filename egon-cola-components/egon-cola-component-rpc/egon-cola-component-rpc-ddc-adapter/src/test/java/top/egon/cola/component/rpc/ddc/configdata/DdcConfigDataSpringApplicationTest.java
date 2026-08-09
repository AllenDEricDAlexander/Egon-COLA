package top.egon.cola.component.rpc.ddc.configdata;

import io.grpc.Attributes;
import io.grpc.Server;
import io.grpc.ServerTransportFilter;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertySource;
import top.egon.cola.component.ddc.autoconfigure.DdcAutoConfiguration;
import top.egon.cola.component.ddc.autoconfigure.DdcRegistryAutoConfiguration;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigRuntimeServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigResponse;
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcConfigProtoMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DdcConfigDataSpringApplicationTest {

    @TempDir
    Path tempDirectory;

    @Test
    void remoteYamlOverridesLocalConfigDataBeforeBeanBinding()
            throws Exception {
        try (RpcFixture fixture = fixture("remote")) {
            Path bootstrap = bootstrapFile(fixture.port(), false);

            try (ConfigurableApplicationContext context = run(bootstrap)) {
                SampleProperties properties = context.getBean(
                        SampleProperties.class
                );
                assertThat(properties.getValue()).isEqualTo("remote");
                assertThat(context.getEnvironment().getProperty("sample.value"))
                        .isEqualTo("remote");
                assertThat(context.getBeansOfType(DdcRpcClientFactory.class))
                        .isEmpty();

                List<String> names = propertySourceNames(context);
                int ddcIndex = names.indexOf("ddc:application.yml");
                int bootstrapIndex = indexContaining(names, "bootstrap.yml");
                assertThat(ddcIndex).isGreaterThanOrEqualTo(0);
                assertThat(bootstrapIndex).isGreaterThan(ddcIndex);
            }

            assertThat(fixture.request().getScope().getBizCode())
                    .isEqualTo("orders");
            assertThat(fixture.awaitChannelClose()).isTrue();
        }
    }

    @Test
    void commandLineRetainsOfficialPrecedenceOverRemoteYaml()
            throws Exception {
        try (RpcFixture fixture = fixture("remote")) {
            Path bootstrap = bootstrapFile(fixture.port(), false);

            try (ConfigurableApplicationContext context = run(
                    bootstrap,
                    "--sample.value=command-line"
            )) {
                assertThat(context.getBean(SampleProperties.class).getValue())
                        .isEqualTo("command-line");
                assertThat(context.getEnvironment().getProperty("sample.value"))
                        .isEqualTo("command-line");
                assertThat(propertySourceNames(context))
                        .contains("ddc:application.yml");
            }
            assertThat(fixture.awaitChannelClose()).isTrue();
        }
    }

    @Test
    void optionalUnavailableRpcKeepsLocalConfigData() throws Exception {
        Path bootstrap = bootstrapFile(1, true);

        try (ConfigurableApplicationContext context = run(bootstrap)) {
            assertThat(context.getBean(SampleProperties.class).getValue())
                    .isEqualTo("bootstrap");
            assertThat(context.getEnvironment().getProperty("sample.value"))
                    .isEqualTo("bootstrap");
        }
    }

    private ConfigurableApplicationContext run(
            Path bootstrap,
            String... extraArguments) {
        SpringApplication application = new SpringApplication(
                TestApplication.class
        );
        application.setWebApplicationType(WebApplicationType.NONE);
        List<String> arguments = new ArrayList<>();
        arguments.add(
                "--spring.config.additional-location=" + bootstrap.toUri()
        );
        arguments.add(
                "--spring.autoconfigure.exclude="
                        + DdcAutoConfiguration.class.getName() + ','
                        + DdcRegistryAutoConfiguration.class.getName()
        );
        arguments.addAll(List.of(extraArguments));
        return application.run(arguments.toArray(String[]::new));
    }

    private Path bootstrapFile(int port, boolean optional) throws Exception {
        Path bootstrap = tempDirectory.resolve(
                optional ? "optional-bootstrap.yml" : "bootstrap.yml"
        );
        String importLocation = optional
                ? "optional:ddc:application.yml"
                : "ddc:application.yml";
        Files.writeString(bootstrap, """
                spring:
                  config:
                    import: %s
                egon:
                  cola:
                    component:
                      ddc:
                        enabled: true
                        biz-code: orders
                        env: test
                        namespace: default
                        app-code: order-service
                        rpc:
                          target: localhost:%d
                          connect-timeout: 100ms
                          default-timeout: 500ms
                          shutdown-timeout: 500ms
                          auth:
                            enabled: false
                          tls:
                            enabled: false
                            development-plaintext: true
                sample:
                  value: bootstrap
                """.formatted(importLocation, port));
        return bootstrap;
    }

    private RpcFixture fixture(String remoteValue) throws Exception {
        AtomicReference<PullConfigRequest> request = new AtomicReference<>();
        CountDownLatch channelClosed = new CountDownLatch(1);
        DdcConfigValue value = new DdcConfigValue();
        value.setResourceName("application.yml");
        value.setFormat("YAML");
        value.setContent("sample:\n  value: " + remoteValue + "\n");
        value.setVersion(1L);
        DdcConfigProtoMapper mapper = new DdcConfigProtoMapper(
                new DdcCommonProtoMapper(1024 * 1024),
                1024 * 1024
        );
        Server server = NettyServerBuilder.forPort(0)
                .addService(new DdcConfigRuntimeServiceGrpc
                        .DdcConfigRuntimeServiceImplBase() {
                    @Override
                    public void pullConfig(
                            PullConfigRequest current,
                            StreamObserver<PullConfigResponse> observer) {
                        request.set(current);
                        observer.onNext(mapper.toPullResponse(List.of(value)));
                        observer.onCompleted();
                    }
                })
                .addTransportFilter(new ServerTransportFilter() {
                    @Override
                    public void transportTerminated(Attributes attributes) {
                        channelClosed.countDown();
                    }
                })
                .build()
                .start();
        return new RpcFixture(server, request, channelClosed);
    }

    private List<String> propertySourceNames(
            ConfigurableApplicationContext context) {
        List<String> names = new ArrayList<>();
        for (PropertySource<?> propertySource
                : context.getEnvironment().getPropertySources()) {
            names.add(propertySource.getName());
        }
        return names;
    }

    private int indexContaining(List<String> values, String expected) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).contains(expected)) {
                return index;
            }
        }
        return -1;
    }

    private record RpcFixture(
            Server server,
            AtomicReference<PullConfigRequest> observed,
            CountDownLatch channelClosed) implements AutoCloseable {

        int port() {
            return server.getPort();
        }

        PullConfigRequest request() {
            return observed.get();
        }

        boolean awaitChannelClose() throws InterruptedException {
            return channelClosed.await(2, TimeUnit.SECONDS);
        }

        @Override
        public void close() throws InterruptedException {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @SpringBootConfiguration
    @EnableConfigurationProperties(SampleProperties.class)
    static class TestApplication {
    }

    @ConfigurationProperties("sample")
    public static class SampleProperties {

        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
