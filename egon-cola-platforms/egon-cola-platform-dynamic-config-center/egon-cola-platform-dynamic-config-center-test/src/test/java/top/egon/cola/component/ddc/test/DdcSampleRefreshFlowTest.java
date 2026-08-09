package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.format.DdcChecksum;
import top.egon.cola.component.ddc.service.binding.DdcBeanPostProcessor;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.config.DdcPublishMessage;
import top.egon.cola.component.ddc.model.config.DdcPublishTarget;
import top.egon.cola.component.ddc.model.config.DdcAckStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.config.DdcConfigValue;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.format.DdcConfigFormatStrategyRegistry;
import top.egon.cola.component.ddc.format.DdcYamlConfigFormatStrategy;
import top.egon.cola.component.ddc.service.refresh.DdcConfigurationPropertiesRebinder;
import top.egon.cola.component.ddc.service.refresh.DdcYamlConfigApplier;
import top.egon.cola.component.ddc.state.DdcLocalConfigState;
import top.egon.cola.component.ddc.service.binding.DdcValueBindingRegistry;
import top.egon.cola.component.ddc.service.refresh.DefaultDdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.binding.DdcFieldBindingService;
import top.egon.cola.component.ddc.state.DdcLeaseSessionHolder;
import top.egon.cola.component.ddc.service.refresh.DdcRefreshService;
import top.egon.cola.component.ddc.test.service.SampleConfigService;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcSampleRefreshFlowTest {

    @Test
    void refreshUpdatesBoundFieldAndReportsSuccessAck() throws Exception {
        RecordingAdminClient adminClient = new RecordingAdminClient();
        DdcLocalConfigState repository = new DdcLocalConfigState();
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();
        context.getBeanFactory().setConversionService(
                ApplicationConversionService.getSharedInstance()
        );
        context.getBeanFactory().addEmbeddedValueResolver(
                context.getEnvironment()::resolveRequiredPlaceholders
        );
        DdcDynamicPropertySource source = new DdcYamlConfigFormatStrategy()
                .load(
                        "application.yml",
                        "order:\n  rate-limit:\n    permits-per-second: 100\n"
                                + "downgrade-switch: false\n",
                        1L
                );
        context.getEnvironment().getPropertySources().addFirst(source);
        context.registerBean(DdcValueBindingRegistry.class);
        context.registerBean(
                DdcFieldBindingService.class,
                () -> new DdcFieldBindingService(
                        context.getBean(DdcValueBindingRegistry.class),
                        context.getBeanFactory()
                )
        );
        context.registerBean(DdcBeanPostProcessor.class);
        context.registerBean(SampleConfigService.class);
        context.refresh();
        DdcFieldBindingService bindingService = context.getBean(
                DdcFieldBindingService.class
        );
        SampleConfigService sample = context.getBean(
                SampleConfigService.class
        );
        DefaultDdcConfigApplierRegistry registry =
                new DefaultDdcConfigApplierRegistry(
                        (key, value, version) -> {
                        }
                );
        registry.freeze();
        DdcConfigurationPropertiesRebinder rebinder =
                mock(DdcConfigurationPropertiesRebinder.class);
        when(rebinder.rebind(any(), any())).thenReturn(java.util.Set.of());
        DdcYamlConfigApplier yamlConfigApplier = new DdcYamlConfigApplier(
                context.getEnvironment(),
                registry,
                bindingService,
                rebinder,
                event -> {
                },
                1024,
                DdcConfigFormatStrategyRegistry.defaults()
        );
        DdcLeaseSessionHolder sessionHolder = new DdcLeaseSessionHolder();
        sessionHolder.replace(adminClient.session());
        DdcRefreshService refreshService =
                new DdcRefreshService(
                        repository,
                        yamlConfigApplier,
                        adminClient,
                        sessionHolder
                );

        refreshService.refresh(message("""
                order:
                  rate-limit:
                    permits-per-second: 200
                downgrade-switch: false
                """, 2L));

        assertThat(sample.getRateLimit()).isEqualTo(200);
        assertThat(adminClient.lastAck().getStatus()).isEqualTo(DdcAckStatus.SUCCESS);
        context.close();
    }

    private DdcPublishMessage message(String value, long version) {
        DdcPublishMessage message = new DdcPublishMessage();
        message.setChangeId("c1");
        message.setAppCode("demo-app");
        message.setEnv("dev");
        message.setNamespace("default");
        message.setResourceName("application.yml");
        message.setContent(value);
        message.setFormat("YAML");
        message.setTargetVersion(version);
        message.setResourceChecksum(DdcChecksum.resource(
                "application.yml",
                "YAML",
                value
        ));
        message.setTargets(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        return message;
    }

    static class RecordingAdminClient implements DdcConfigClient {

        private DdcAckRequest lastAck;

        @Override
        public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
            Instant registeredAt = Instant.parse("2026-07-24T12:00:00Z");
            return new DdcLeaseSession(
                    request.getInstanceId(),
                    "lease-1",
                    DdcLeaseRole.CONFIG_CLIENT,
                    30,
                    10,
                    registeredAt,
                    registeredAt.plusSeconds(30)
            );
        }

        @Override
        public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.RENEWED,
                    Instant.parse("2026-07-24T12:00:30Z")
            );
        }

        @Override
        public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, null);
        }

        @Override
        public List<DdcConfigValue> pull() {
            return Collections.emptyList();
        }

        @Override
        public void ack(DdcAckRequest request) {
            this.lastAck = request;
        }

        DdcAckRequest lastAck() {
            return lastAck;
        }

        DdcLeaseSession session() {
            Instant registeredAt = Instant.parse("2026-07-24T12:00:00Z");
            return new DdcLeaseSession(
                    "instance-1",
                    "lease-1",
                    DdcLeaseRole.CONFIG_CLIENT,
                    30,
                    10,
                    registeredAt,
                    registeredAt.plusSeconds(30)
            );
        }
    }
}
