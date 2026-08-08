package top.egon.cola.component.ddc.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.environment.DdcDynamicPropertySource;
import top.egon.cola.component.ddc.format.DdcConfigFormatStrategyRegistry;
import top.egon.cola.component.ddc.format.DdcYamlConfigFormatStrategy;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.refresh.DdcConfigurationChangedEvent;
import top.egon.cola.component.ddc.refresh.DdcConfigurationPropertiesRebinder;
import top.egon.cola.component.ddc.refresh.DdcYamlConfigApplier;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcRefreshServiceTest {

    private static final String INITIAL_YAML = """
            feature:
              enabled: false
              label: old
            """;

    @Test
    void appliesYamlLeavesAndReportsChangeClassification() throws Exception {
        AtomicReference<String> applied = new AtomicReference<>();
        Harness harness = harness(registry -> registry.registerExact(
                "feature.enabled",
                (key, value, version) -> applied.set(value)
        ));

        harness.service.refresh(harness.message("""
                feature:
                  enabled: true
                  label: new
                """, 2L));

        assertThat(harness.environment.getProperty(
                "feature.enabled",
                Boolean.class
        )).isTrue();
        assertThat(applied).hasValue("true");
        assertThat(harness.client.lastAck()).satisfies(ack -> {
            assertThat(ack.getStatus()).isEqualTo(DdcAckStatus.SUCCESS);
            assertThat(ack.getCurrentVersion()).isEqualTo(2L);
            assertThat(ack.getConfigKey()).isEqualTo("application.yml");
        });
        assertThat(harness.events).singleElement().satisfies(event -> {
            assertThat(event.changedKeys()).containsExactlyInAnyOrder(
                    "feature.enabled",
                    "feature.label"
            );
            assertThat(event.refreshedKeys()).containsExactly(
                    "feature.enabled"
            );
            assertThat(event.restartRequiredKeys()).containsExactly(
                    "feature.label"
            );
            assertThat(event.changeId()).isEqualTo("change-1");
        });
    }

    @Test
    void configDataSnapshotSeedsMetadataAndPreventsDuplicateReconcile()
            throws Exception {
        AtomicInteger applyCount = new AtomicInteger();
        Harness harness = harness(registry -> registry.registerExact(
                "feature.enabled",
                (key, value, version) -> applyCount.incrementAndGet()
        ));

        harness.service.applySnapshots(List.of(
                harness.config(INITIAL_YAML, 1L)
        ));

        assertThat(applyCount).hasValue(0);
        assertThat(harness.events).isEmpty();
        assertThat(harness.repository.version("application.yml"))
                .isEqualTo(1L);
        assertThat(harness.repository.checksum("application.yml"))
                .isEqualTo(DdcChecksum.content(INITIAL_YAML));
    }

    @Test
    void initializesExplicitAppliersFromTheConfigDataSnapshot()
            throws Exception {
        AtomicReference<String> applied = new AtomicReference<>();
        Harness harness = harness(registry -> registry.registerExact(
                "feature.enabled",
                (key, value, version) -> applied.set(value + ':' + version)
        ));

        harness.yamlConfigApplier.afterSingletonsInstantiated();

        assertThat(applied).hasValue("false:1");
        assertThat(harness.events).isEmpty();
    }

    @Test
    void sameVersionConflictFailsWithoutChangingLastKnownGood()
            throws Exception {
        AtomicInteger applyCount = new AtomicInteger();
        Harness harness = harness(registry -> registry.registerExact(
                "feature.enabled",
                (key, value, version) -> applyCount.incrementAndGet()
        ));

        harness.service.refresh(harness.message("""
                feature:
                  enabled: true
                """, 1L));

        assertThat(applyCount).hasValue(0);
        assertThat(harness.client.lastAck().getStatus())
                .isEqualTo(DdcAckStatus.FAILED);
        assertThat(harness.environment.getProperty(
                "feature.enabled",
                Boolean.class
        )).isFalse();
        assertThat(harness.events).isEmpty();
    }

    @Test
    void reservedKeyRejectsWholeDocumentAndRestoresMetadata()
            throws Exception {
        Harness harness = harness(registry -> {
        });

        harness.service.refresh(harness.message("""
                feature:
                  enabled: true
                egon:
                  cola:
                    component:
                      ddc:
                        enabled: false
                """, 2L));

        assertThat(harness.client.lastAck().getStatus())
                .isEqualTo(DdcAckStatus.FAILED);
        assertThat(harness.client.lastAck().getErrorMessage())
                .contains("reserved key")
                .doesNotContain("enabled: false");
        assertThat(harness.environment.getProperty(
                "feature.enabled",
                Boolean.class
        )).isFalse();
        assertThat(harness.repository.version("application.yml"))
                .isEqualTo(1L);
        assertThat(harness.events).isEmpty();
    }

    @Test
    void leafApplyFailureRestoresYamlSnapshotAndMetadata()
            throws Exception {
        Harness harness = harness(registry -> registry.registerExact(
                "feature.enabled",
                (key, value, version) -> {
                    throw new IllegalStateException("domain apply failed");
                }
        ));

        harness.service.refresh(harness.message("""
                feature:
                  enabled: true
                  label: new
                """, 2L));

        assertThat(harness.client.lastAck().getStatus())
                .isEqualTo(DdcAckStatus.FAILED);
        assertThat(harness.client.lastAck().getErrorMessage())
                .contains("domain apply failed");
        assertThat(harness.environment.getProperty(
                "feature.enabled",
                Boolean.class
        )).isFalse();
        assertThat(harness.environment.getProperty("feature.label"))
                .isEqualTo("old");
        assertThat(harness.repository.version("application.yml"))
                .isEqualTo(1L);
        assertThat(harness.events).isEmpty();
    }

    @Test
    void rebinderResultParticipatesInRefreshClassification()
            throws Exception {
        DdcConfigurationPropertiesRebinder rebinder =
                mock(DdcConfigurationPropertiesRebinder.class);
        when(rebinder.rebind(any(), any()))
                .thenReturn(Set.of("feature.label"));
        Harness harness = harness(registry -> {
        }, rebinder, event -> {
        });

        harness.service.refresh(harness.message("""
                feature:
                  enabled: false
                  label: rebound
                """, 2L));

        assertThat(harness.events).singleElement().satisfies(event -> {
            assertThat(event.refreshedKeys()).containsExactly(
                    "feature.label"
            );
            assertThat(event.restartRequiredKeys()).isEmpty();
        });
    }

    @Test
    void eventListenerFailureDoesNotFailAcceptedConfiguration()
            throws Exception {
        Harness harness = harness(
                registry -> {
                },
                mock(DdcConfigurationPropertiesRebinder.class),
                event -> {
                    throw new IllegalStateException("listener failed");
                }
        );

        harness.service.refresh(harness.message("""
                feature:
                  enabled: true
                """, 2L));

        assertThat(harness.client.lastAck().getStatus())
                .isEqualTo(DdcAckStatus.SUCCESS);
        assertThat(harness.repository.version("application.yml"))
                .isEqualTo(2L);
    }

    @Test
    void nonTargetAndMalformedResourcesDoNotApplyOrAck()
            throws Exception {
        Harness harness = harness(registry -> {
        });
        DdcPublishMessage nonTarget = harness.message(
                "feature:\n  enabled: true\n",
                2L
        );
        nonTarget.setTargets(List.of(
                new DdcPublishTarget("other", "other-lease")
        ));
        harness.service.refresh(nonTarget);

        DdcPublishMessage wrongType = harness.message(
                "feature:\n  enabled: true\n",
                2L
        );
        wrongType.setValueType("TXT");
        harness.service.refresh(wrongType);

        assertThat(harness.client.ackCount).isZero();
        assertThat(harness.events).isEmpty();
    }

    @Test
    void ackTransportFailureDoesNotRollbackAcceptedYaml()
            throws Exception {
        Harness harness = harness(registry -> {
        });
        harness.client.failAck = true;

        harness.service.refresh(harness.message("""
                feature:
                  enabled: true
                """, 2L));

        assertThat(harness.environment.getProperty(
                "feature.enabled",
                Boolean.class
        )).isTrue();
        assertThat(harness.repository.version("application.yml"))
                .isEqualTo(2L);
    }

    private Harness harness(RegistryConfigurer configurer)
            throws Exception {
        DdcConfigurationPropertiesRebinder rebinder =
                mock(DdcConfigurationPropertiesRebinder.class);
        when(rebinder.rebind(any(), any())).thenReturn(Set.of());
        return harness(configurer, rebinder, event -> {
        });
    }

    private Harness harness(
            RegistryConfigurer configurer,
            DdcConfigurationPropertiesRebinder rebinder,
            ApplicationEventPublisher additionalPublisher)
            throws Exception {
        ConfigurableEnvironment environment = new MockEnvironment();
        DdcDynamicPropertySource source =
                new DdcYamlConfigFormatStrategy().load(
                        "application.yml",
                        INITIAL_YAML,
                        1L
                );
        environment.getPropertySources().addFirst(source);
        DdcLocalConfigRepository repository =
                new DdcLocalConfigRepository();
        DdcFieldBindingService fieldBindingService =
                mock(DdcFieldBindingService.class);
        DefaultDdcConfigApplierRegistry registry =
                new DefaultDdcConfigApplierRegistry(
                        (key, value, version) -> {
                        }
                );
        configurer.configure(registry);
        registry.freeze();
        List<DdcConfigurationChangedEvent> events =
                new ArrayList<>();
        DdcYamlConfigApplier applier = new DdcYamlConfigApplier(
                environment,
                registry,
                fieldBindingService,
                rebinder,
                event -> {
                    events.add((DdcConfigurationChangedEvent) event);
                    additionalPublisher.publishEvent(event);
                },
                1024 * 1024,
                DdcConfigFormatStrategyRegistry.defaults()
        );
        RecordingAdminClient client = new RecordingAdminClient();
        DdcRefreshService service = new DdcRefreshService(
                repository,
                applier,
                client,
                sessionHolder()
        );
        return new Harness(
                environment,
                repository,
                client,
                service,
                applier,
                events
        );
    }

    private DdcLeaseSessionHolder sessionHolder() {
        DdcLeaseSessionHolder holder = new DdcLeaseSessionHolder();
        Instant registeredAt = Instant.parse("2026-07-24T12:00:00Z");
        holder.replace(new DdcLeaseSession(
                "instance-1",
                "lease-1",
                top.egon.cola.component.ddc.model.enums.DdcLeaseRole.CONFIG_CLIENT,
                30,
                10,
                registeredAt,
                registeredAt.plusSeconds(30)
        ));
        return holder;
    }

    @FunctionalInterface
    private interface RegistryConfigurer {

        void configure(DefaultDdcConfigApplierRegistry registry);
    }

    private record Harness(
            ConfigurableEnvironment environment,
            DdcLocalConfigRepository repository,
            RecordingAdminClient client,
            DdcRefreshService service,
            DdcYamlConfigApplier yamlConfigApplier,
            List<DdcConfigurationChangedEvent> events
    ) {

        private DdcPublishMessage message(String yaml, long version) {
            DdcPublishMessage message = new DdcPublishMessage();
            message.setChangeId("change-1");
            message.setBizCode("retail");
            message.setAppCode("demo");
            message.setEnv("dev");
            message.setConfigKey("application.yml");
            message.setConfigValue(yaml);
            message.setValueType("YAML");
            message.setTargetVersion(version);
            message.setContentChecksum(DdcChecksum.content(yaml));
            message.setTargets(List.of(
                    new DdcPublishTarget("instance-1", "lease-1")
            ));
            return message;
        }

        private DdcConfigValue config(String yaml, long version) {
            DdcConfigValue config = new DdcConfigValue();
            config.setConfigKey("application.yml");
            config.setConfigValue(yaml);
            config.setValueType("YAML");
            config.setVersion(version);
            return config;
        }
    }

    private static class RecordingAdminClient implements DdcAdminClient {

        private DdcAckRequest lastAck;

        private int ackCount;

        private boolean failAck;

        @Override
        public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
            return null;
        }

        @Override
        public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
            return null;
        }

        @Override
        public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
            return null;
        }

        @Override
        public List<DdcConfigValue> pull() {
            return List.of();
        }

        @Override
        public void ack(DdcAckRequest request) {
            lastAck = request;
            ackCount++;
            if (failAck) {
                throw new IllegalStateException("ack unavailable");
            }
        }

        private DdcAckRequest lastAck() {
            return lastAck;
        }
    }
}
