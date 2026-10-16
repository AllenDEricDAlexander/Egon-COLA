package top.egon.cola.platform.tianquan.shoubing.admin.integration;

import jakarta.validation.Validation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XingyuanRuntimeProbeInitializerTest {
    @TempDir Path temporary;
    private static final Instant NOW = Instant.parse("2026-10-15T02:00:00Z");

    @Test
    void fixedGraphQlWorkerChangesThreadCallsTheOriginalOnceAndReportsOnlyCounters() throws Exception {
        long caller = Thread.currentThread().threadId(); var calls = new AtomicInteger();
        try (var component = new XingyuanRuntimeProbeInitializer.GraphQlAsyncObservationComponent()) {
            Object expected = new Object();
            Object actual = component.callOnWorker(() -> {
                assertThat(Thread.currentThread().threadId()).isNotEqualTo(caller);
                calls.incrementAndGet(); return expected;
            });
            assertThat(actual).isSameAs(expected); assertThat(calls).hasValue(1);
            assertThat(component.snapshot()).containsExactlyInAnyOrderEntriesOf(Map.of(
                    "differentThread", 1L, "matchedContext", 0L, "finallyRestored", 1L, "failure", 0L));
        }
    }

    @Test
    void fixedGraphQlWorkerPreservesTheOriginalExceptionAndRejectsActualContextLeakage() throws Exception {
        var original = new IllegalStateException("original-business-failure");
        try (var component = new XingyuanRuntimeProbeInitializer.GraphQlAsyncObservationComponent()) {
            assertThatThrownBy(() -> component.callOnWorker(() -> { throw original; })).isSameAs(original);
            assertThat(component.snapshot()).containsEntry("finallyRestored", 1L).containsEntry("failure", 0L);
            assertThatThrownBy(() -> component.callOnWorker(() -> {
                org.slf4j.MDC.put("unexpected", "controlled-test"); return null;
            })).isInstanceOf(IllegalArgumentException.class);
            assertThat(component.snapshot()).containsEntry("failure", 1L).containsEntry("finallyRestored", 1L);
            assertThatThrownBy(() -> component.callOnWorker(() -> null)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(org.slf4j.MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void securityMetricsUseTheOriginalRecorderAndRejectIdentityTags() throws Exception {
        var registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var usage = new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties(true, URI.create("https://127.0.0.1:28443/tenants/1001"), true,
                    top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum.YUHENG_ADMIN);
            var original = new top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityMetricsComponent(() -> registry, validators.getValidator(), usage);
            original.record(top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO.builder().signal("oauth.authentication").result("VALID").build());
            original.record(top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO.builder().signal("jwt.verify.duration").result("SUCCESS").duration(java.time.Duration.ofNanos(1234)).build());
            original.record(top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO.builder().signal("spool.bytes").result("CURRENT").value(321).build());
            var samples = XingyuanRuntimeProbeInitializer.securityMetrics(registry, validators.getValidator());
            assertThat(samples).hasSize(3);
            assertThat(samples.stream().filter(value -> value.signal().equals("oauth.authentication")).findFirst().orElseThrow().count()).isEqualTo(1);
            assertThat(samples.stream().filter(value -> value.signal().equals("spool.bytes")).findFirst().orElseThrow().value()).isEqualTo(321);
            io.micrometer.core.instrument.Counter.builder("oauth.authentication").tags("receiver", "YUHENG_ADMIN", "result", "VALID", "subject", "forbidden").register(registry).increment();
            assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.securityMetrics(registry, validators.getValidator())).isInstanceOf(IllegalArgumentException.class);
        } finally { registry.close(); }
    }

    @Test
    void boundedClientEventLoopPauseReleasesOnCloseAndOnItsOwnDeadline() throws Exception {
        var loops = new io.netty.channel.DefaultEventLoopGroup(2);
        try {
            try (var pause = XingyuanRuntimeProbeInitializer.SharedStatePauseLease.start(loops, java.time.Duration.ofSeconds(2))) {
                var queued = loops.next().submit(() -> true);
                assertThat(queued.isDone()).isFalse(); assertThat(pause.expired()).isFalse();
                pause.close(); assertThat(queued.get(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); assertThat(pause.expired()).isTrue();
            }
            try (var pause = XingyuanRuntimeProbeInitializer.SharedStatePauseLease.start(loops, java.time.Duration.ofSeconds(1))) {
                var queued = loops.next().submit(() -> true);
                assertThat(queued.get(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); assertThat(pause.expired()).isTrue();
            }
            assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.SharedStatePauseLease.start(loops, java.time.Duration.ofSeconds(31))).isInstanceOf(IllegalArgumentException.class);
        } finally { loops.shutdownGracefully(0, 2, java.util.concurrent.TimeUnit.SECONDS).get(3, java.util.concurrent.TimeUnit.SECONDS); }
    }

    @Test
    void onlyLightAndWebAcceptTheReadOnlyGraphQlContextCounterCommand() throws Exception {
        var original = files().getInputs();
        for (String role : List.of("native-light", "native-web", "native-service", "edge", "yuheng")) {
            var inputs = new XingyuanRuntimeProbeInitializer.ProbeInputsVO(original.root(), original.manifestFile(), original.manifestSha256(), original.runId(), role,
                    original.fixture(), original.fixtureSha256(), original.artifact(), original.artifactSha256(), original.namespace(), original.applicationCode(), original.registryFile(), original.registrySha256());
            var request = command(inputs, "GRAPHQL_CONTEXT_COUNTS", null, NOW); var file = inputs.directory().resolve("requests/" + request.nonce() + ".json");
            if (Set.of("native-light", "native-web").contains(role)) {
                assertThatCode(() -> XingyuanRuntimeProbeInitializer.validateCommand(inputs, request, file, NOW)).doesNotThrowAnyException();
            } else { assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.validateCommand(inputs, request, file, NOW)).isInstanceOf(IllegalArgumentException.class); }
        }
    }

    @Test
    void onlyYuhengAcceptsTheFixedMetricsAndBoundedPauseCommands() throws Exception {
        var original = files().getInputs();
        for (String role : List.of("yuheng", "native-light", "shoubing", "edge")) {
            var inputs = new XingyuanRuntimeProbeInitializer.ProbeInputsVO(original.root(), original.manifestFile(), original.manifestSha256(), original.runId(), role,
                    original.fixture(), original.fixtureSha256(), original.artifact(), original.artifactSha256(), original.namespace(), original.applicationCode(), original.registryFile(), original.registrySha256());
            for (String operation : List.of("SECURITY_METRICS", "SHARED_STATE_PAUSE", "SHARED_STATE_RESUME")) {
                var request = command(inputs, operation, null, NOW); var path = inputs.directory().resolve("requests/" + request.nonce() + ".json");
                if (role.equals("yuheng")) { assertThatCode(() -> XingyuanRuntimeProbeInitializer.validateCommand(inputs, request, path, NOW)).doesNotThrowAnyException(); }
                else { assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.validateCommand(inputs, request, path, NOW)).isInstanceOf(IllegalArgumentException.class); }
            }
        }
    }

    @Test
    void sharedStatePauseChecksTheActualSingleTaskDatabaseAndOwnerBeforeTouchingTheGroup() throws Exception {
        var original = files().getInputs();
        privateJson(original.manifestFile(), Map.of("redis", Map.of("endpoint", "redis://127.0.0.1:6379", "database", 15)));
        String hash = XingyuanRuntimeProbeInitializer.hashFile(original.manifestFile());
        var inputs = new XingyuanRuntimeProbeInitializer.ProbeInputsVO(original.root(), original.manifestFile(), hash, original.runId(), "yuheng",
                original.fixture(), original.fixtureSha256(), original.artifact(), original.artifactSha256(), original.namespace(), null, original.registryFile(), original.registrySha256());
        var client = org.mockito.Mockito.mock(org.redisson.Redisson.class); var manager = org.mockito.Mockito.mock(org.redisson.connection.ConnectionManager.class);
        var service = org.mockito.Mockito.mock(org.redisson.connection.ServiceManager.class); var loops = org.mockito.Mockito.mock(io.netty.channel.EventLoopGroup.class);
        var bucket = org.mockito.Mockito.mock(org.redisson.api.RBucket.class); var config = new org.redisson.config.Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setDatabase(15);
        var runtime = new org.redisson.config.MasterSlaveServersConfig().setMasterAddress("redis://127.0.0.1:6379").setDatabase(15);
        org.mockito.Mockito.when(client.getConfig()).thenReturn(config); org.mockito.Mockito.when(client.getConnectionManager()).thenReturn(manager);
        org.mockito.Mockito.when(manager.getServiceManager()).thenReturn(service); org.mockito.Mockito.when(service.getConfig()).thenReturn(runtime);
        org.mockito.Mockito.when(service.getGroup()).thenReturn(loops);
        org.mockito.Mockito.when(client.getBucket("xingyuan-it:owner", org.redisson.client.codec.StringCodec.INSTANCE)).thenReturn(bucket);
        org.mockito.Mockito.when(bucket.get()).thenReturn("xingyuan-it:" + inputs.runId() + ':' + hash);
        try (var context = new GenericApplicationContext()) {
            context.getBeanFactory().registerSingleton("rbac3RuntimeRedissonClient", client); context.getBeanFactory().registerSingleton("idpAuditRedissonClient", client); context.refresh();
            assertThat(XingyuanRuntimeProbeInitializer.ownedSharedStateGroup(context, inputs)).isSameAs(loops);
            org.mockito.Mockito.clearInvocations(client, bucket, service);
            runtime.setDatabase(0);
            assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.ownedSharedStateGroup(context, inputs)).isInstanceOf(IllegalArgumentException.class);
            org.mockito.Mockito.verifyNoInteractions(bucket); org.mockito.Mockito.verify(service, org.mockito.Mockito.never()).getGroup();
            runtime.setDatabase(15).setMasterAddress("redis://foreign.invalid:6379");
            assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.ownedSharedStateGroup(context, inputs)).isInstanceOf(IllegalArgumentException.class);
            org.mockito.Mockito.verifyNoInteractions(bucket);
            runtime.setMasterAddress("redis://127.0.0.1:6379"); org.mockito.Mockito.when(bucket.get()).thenReturn("foreign-owner");
            assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.ownedSharedStateGroup(context, inputs)).isInstanceOf(IllegalArgumentException.class);
            org.mockito.Mockito.verify(service, org.mockito.Mockito.never()).getGroup();
        }
    }

    @Test
    void runtimeCommandUsesTheOriginalQualifiedValidatorEvenWithAnotherHostValidator() throws Exception {
        var inputs = files().getInputs();
        try (var validators = Validation.buildDefaultValidatorFactory(); var context = new GenericApplicationContext()) {
            context.getBeanFactory().registerSingleton("defaultValidator", validators.getValidator());
            context.getBeanFactory().registerSingleton("alternateHostValidator", validators.getValidator()); context.refresh();
            var runtime = new XingyuanRuntimeProbeInitializer.RuntimeProbeComponent(context, inputs, null);
            var request = command(inputs, "EDGE_BINDING", null, Instant.now()); Path file = inputs.directory().resolve("requests/" + request.nonce() + ".json");
            privateJson(file, request);
            assertThatCode(() -> runtime.claim(file)).doesNotThrowAnyException();
        }
    }

    @Test
    void springFactoriesDiscoversTheInitializerButDefaultModeRegistersNothing() throws Exception {
        var files = files();
        try (var loader = new URLClassLoader(new java.net.URL[]{files.getInputs().fixture().toUri().toURL()}, getClass().getClassLoader());
             var context = new GenericApplicationContext()) {
            var initializers = SpringFactoriesLoader.loadFactories(ApplicationContextInitializer.class, loader);
            assertThat(initializers).anyMatch(value -> value instanceof XingyuanRuntimeProbeInitializer);
            new XingyuanRuntimeProbeInitializer().initialize(context);
            assertThat(context.getBeanFactory().getSingletonNames()).doesNotContain("xingyuanRuntimeProbeComponent", "xingyuanRuntimeProbeEdgeWebFilter");
            assertThat(Files.exists(files.getInputs().directory().resolve("ready.json"))).isFalse();
        }
    }

    @Test
    void enablingRequiresTheActualFixedFixtureAndBoundOriginalArtifact() throws Exception {
        var files = files(); var inputs = files.getInputs();
        assertThat(XingyuanRuntimeProbeInitializer.verifiedInputs(files.getEnvironment(), inputs.fixture().toUri())).isEqualTo(inputs);
        assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.verifiedInputs(files.getEnvironment(), inputs.artifact().toUri())).isInstanceOf(IllegalArgumentException.class);
        files.getProperties().put(XingyuanRuntimeProbeInitializer.PREFIX + "role", "native-agent");
        assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.verifiedInputs(files.getEnvironment(), inputs.fixture().toUri())).isInstanceOf(IllegalArgumentException.class);
        files.getProperties().put(XingyuanRuntimeProbeInitializer.PREFIX + "role", "edge");
        files.getProperties().put(XingyuanRuntimeProbeInitializer.PREFIX + "fixture-sha256", "0".repeat(64));
        assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.verifiedInputs(files.getEnvironment(), inputs.fixture().toUri())).isInstanceOf(IllegalArgumentException.class);
        files.getProperties().put(XingyuanRuntimeProbeInitializer.PREFIX + "fixture-sha256", inputs.fixtureSha256());
        var manifest = XingyuanRuntimeProbeInitializer.mapper().readTree(Files.readAllBytes(inputs.manifestFile()));
        ((com.fasterxml.jackson.databind.node.ObjectNode) manifest).set("policyFiles", XingyuanRuntimeProbeInitializer.mapper().createArrayNode());
        privateJson(inputs.manifestFile(), manifest);
        assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.verifiedInputs(files.getEnvironment(), inputs.fixture().toUri())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generatedNamespacesCannotSelectSourceProjectsForeignRolesOrArbitraryClasses() {
        assertThat(XingyuanRuntimeProbeInitializer.generatedNamespace("native-service", "example.owned.starter.EvaluationServiceApplication")).isEqualTo("example.owned");
        assertThat(XingyuanRuntimeProbeInitializer.generatedNamespace("native-web", "example.web.starter.OrganizationApplication")).isEqualTo("example.web");
        for (String value : List.of("top.egon.cola.archetype.source.service.starter.EvaluationServiceApplication", "java.lang.Runtime", "example.bad.starter.OrganizationApplication")) {
            assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.generatedNamespace("native-service", value)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.generatedNamespace("native-agent", "example.agent.starter.AgentApplication")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lightObservationUsesOnlyTheGeneratedLightMainAndFixedCounterAction() throws Exception {
        assertThat(org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> XingyuanRuntimeProbeInitializer.generatedNamespace("native-light", "example.light.start.StudentManagementApplication"))).isEqualTo("example.light");
        var edge = files().getInputs();
        var light = new XingyuanRuntimeProbeInitializer.ProbeInputsVO(edge.root(), edge.manifestFile(), edge.manifestSha256(), edge.runId(), "native-light",
                edge.fixture(), edge.fixtureSha256(), edge.artifact(), edge.artifactSha256(), "example.light", "example-light", edge.registryFile(), edge.registrySha256());
        var request = command(light, "BUSINESS_COUNTS", null, NOW);
        assertThatCode(() -> XingyuanRuntimeProbeInitializer.validateCommand(light, request, light.directory().resolve("requests/" + request.nonce() + ".json"), NOW)).doesNotThrowAnyException();
        var forbidden = command(light, "COURSE_CREATE", null, NOW);
        assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.validateCommand(light, forbidden, light.directory().resolve("requests/" + forbidden.nonce() + ".json"), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void knowledgeObservationIsAReadOnlyYuhengActionAndNeverAnotherHostsOwnerCall() throws Exception {
        var edge = files().getInputs();
        var yuheng = new XingyuanRuntimeProbeInitializer.ProbeInputsVO(edge.root(), edge.manifestFile(), edge.manifestSha256(), edge.runId(), "yuheng",
                edge.fixture(), edge.fixtureSha256(), edge.artifact(), edge.artifactSha256(), null, "controlled-yuheng", edge.registryFile(), edge.registrySha256());
        var request = command(yuheng, "KNOWLEDGE_COUNTS", null, NOW);
        assertThatCode(() -> XingyuanRuntimeProbeInitializer.validateCommand(yuheng, request, yuheng.directory().resolve("requests/" + request.nonce() + ".json"), NOW)).doesNotThrowAnyException();
        var forbidden = command(edge, "KNOWLEDGE_COUNTS", null, NOW);
        assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.validateCommand(edge, forbidden, edge.directory().resolve("requests/" + forbidden.nonce() + ".json"), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invocationObservationStaysInsideExistingGuardsAndKeepsOriginalOutcomes() {
        var target = new ControlledInvocationService();
        var factory = new org.springframework.aop.framework.ProxyFactory(target);
        factory.setProxyTargetClass(true);
        var entered = new java.util.ArrayList<String>();
        factory.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation -> {
            entered.add("guard");
            if ("deny".equals(invocation.getArguments()[0])) { throw new org.springframework.security.access.AccessDeniedException("denied"); }
            return invocation.proceed();
        });
        Object proxy = factory.getProxy();
        var original = ((org.springframework.aop.framework.Advised) proxy).getAdvisors();
        var observer = new XingyuanRuntimeProbeInitializer.InvocationObservationComponent("controlledOwner", ControlledInvocationService.class.getName(), Map.of("get", 1));
        var decorated = (ControlledInvocationService) observer.postProcessAfterInitialization(proxy, "controlledOwner");
        assertThat(decorated).isSameAs(proxy);
        assertThat(decorated.get("allowed")).isEqualTo("allowed");
        assertThat(observer.snapshot(decorated)).containsExactlyEntriesOf(Map.of("get", 1L));
        assertThatThrownBy(() -> decorated.get("deny")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(observer.snapshot(decorated)).containsExactlyEntriesOf(Map.of("get", 1L));
        assertThatThrownBy(() -> decorated.get("fail")).isSameAs(target.failure);
        assertThat(observer.snapshot(decorated)).containsExactlyEntriesOf(Map.of("get", 2L));
        assertThat(target.calls.get()).isEqualTo(2);
        assertThat(entered).containsExactly("guard", "guard", "guard");
        var actual = ((org.springframework.aop.framework.Advised) decorated).getAdvisors();
        assertThat(actual).hasSize(original.length + 1);
        for (int index = 0; index < original.length; index++) { assertThat(actual[index]).isSameAs(original[index]); }
        assertThatThrownBy(() -> observer.snapshot(decorated).put("get", 0L)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> observer.postProcessAfterInitialization(decorated, "controlledOwner")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> observer.snapshot(target)).isInstanceOf(IllegalArgumentException.class);
    }

    @lombok.extern.slf4j.Slf4j
    public static class ControlledInvocationService {
        final AtomicInteger calls = new AtomicInteger();
        final IllegalStateException failure = new IllegalStateException("controlled failure");
        public String get(String value) { calls.incrementAndGet(); if ("fail".equals(value)) { throw failure; } return value; }
    }

    @Test
    void invocationObservationRejectsUnknownOrFrozenBoundariesAndRetainsMonotonicCounts() {
        var target = new ControlledInvocationService();
        var observer = new XingyuanRuntimeProbeInitializer.InvocationObservationComponent("controlledOwner", ControlledInvocationService.class.getName(), Map.of("get", 1));
        assertThat(observer.postProcessAfterInitialization(target, "unrelatedOwner")).isSameAs(target);
        assertThatThrownBy(() -> observer.postProcessAfterInitialization(new Object(), "controlledOwner")).isInstanceOf(IllegalArgumentException.class);
        var frozen = new org.springframework.aop.framework.ProxyFactory(target); frozen.setProxyTargetClass(true); frozen.setFrozen(true);
        assertThatThrownBy(() -> observer.postProcessAfterInitialization(frozen.getProxy(), "controlledOwner")).isInstanceOf(IllegalArgumentException.class);
        var decorated = (ControlledInvocationService) observer.postProcessAfterInitialization(target, "controlledOwner");
        assertThat(observer.snapshot(decorated)).containsExactlyEntriesOf(Map.of("get", 0L));
        java.util.stream.IntStream.range(0, 1000).parallel().forEach(index -> assertThat(decorated.get("allowed")).isEqualTo("allowed"));
        assertThat(observer.snapshot(decorated)).containsExactlyEntriesOf(Map.of("get", 1000L));
        assertThat(target.calls.get()).isEqualTo(1000);
        ((org.springframework.aop.framework.Advised) decorated).addAdvice((org.aopalliance.intercept.MethodInterceptor) org.aopalliance.intercept.MethodInvocation::proceed);
        assertThatThrownBy(() -> observer.snapshot(decorated)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void contextInstallsTheNamedObserverBeforePublishingTheOriginalBean() {
        var observer = new XingyuanRuntimeProbeInitializer.InvocationObservationComponent("controlledOwner", ControlledInvocationService.class.getName(), Map.of("get", 1));
        try (var context = new GenericApplicationContext()) {
            context.getBeanFactory().registerSingleton("controlledInvocationObservationComponent", observer);
            context.registerBean("controlledOwner", ControlledInvocationService.class);
            context.refresh();
            var owner = context.getBean("controlledOwner", ControlledInvocationService.class);
            assertThat(observer.snapshot(owner)).containsExactlyEntriesOf(Map.of("get", 0L));
            assertThat(owner.get("allowed")).isEqualTo("allowed");
            assertThat(observer.snapshot(owner)).containsExactlyEntriesOf(Map.of("get", 1L));
        }
        assertThatCode(() -> XingyuanRuntimeProbeInitializer.generatedNamespace("shoubing", "top.egon.cola.platform.tianquan.shoubing.admin.IdpAdminApplication")).doesNotThrowAnyException();
        assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.generatedNamespace("shoubing", "other.IdpAdminApplication")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void commandsRequireExactRoleRunHashesNonceWindowAndAllowedOperation() throws Exception {
        var inputs = files().getInputs(); var good = command(inputs, "EDGE_BINDING", null, NOW);
        Path file = inputs.directory().resolve("requests/" + good.nonce() + ".json");
        assertThatCode(() -> XingyuanRuntimeProbeInitializer.validateCommand(inputs, good, file, NOW)).doesNotThrowAnyException();
        var invalid = List.of(
                copy(good, UUID.randomUUID().toString(), good.manifestSha256(), good.role(), good.fixtureSha256(), good.artifactSha256(), good.nonce(), good.operation(), null, good.issuedAt(), good.expiresAt()),
                copy(good, good.runId(), "0".repeat(64), good.role(), good.fixtureSha256(), good.artifactSha256(), good.nonce(), good.operation(), null, good.issuedAt(), good.expiresAt()),
                copy(good, good.runId(), good.manifestSha256(), "native-web", good.fixtureSha256(), good.artifactSha256(), good.nonce(), good.operation(), null, good.issuedAt(), good.expiresAt()),
                copy(good, good.runId(), good.manifestSha256(), good.role(), "0".repeat(64), good.artifactSha256(), good.nonce(), good.operation(), null, good.issuedAt(), good.expiresAt()),
                copy(good, good.runId(), good.manifestSha256(), good.role(), good.fixtureSha256(), "0".repeat(64), good.nonce(), good.operation(), null, good.issuedAt(), good.expiresAt()),
                copy(good, good.runId(), good.manifestSha256(), good.role(), good.fixtureSha256(), good.artifactSha256(), UUID.randomUUID().toString(), good.operation(), null, good.issuedAt(), good.expiresAt()),
                copy(good, good.runId(), good.manifestSha256(), good.role(), good.fixtureSha256(), good.artifactSha256(), good.nonce(), "COURSE_CREATE", null, good.issuedAt(), good.expiresAt()),
                copy(good, good.runId(), good.manifestSha256(), good.role(), good.fixtureSha256(), good.artifactSha256(), good.nonce(), good.operation(), "12", good.issuedAt(), good.expiresAt()),
                copy(good, good.runId(), good.manifestSha256(), good.role(), good.fixtureSha256(), good.artifactSha256(), good.nonce(), good.operation(), null, NOW.plusSeconds(1), NOW.plusSeconds(60)),
                copy(good, good.runId(), good.manifestSha256(), good.role(), good.fixtureSha256(), good.artifactSha256(), good.nonce(), good.operation(), null, NOW.minusSeconds(60), NOW),
                copy(good, good.runId(), good.manifestSha256(), good.role(), good.fixtureSha256(), good.artifactSha256(), good.nonce(), good.operation(), null, NOW, NOW.plusSeconds(61)));
        for (var changed : invalid) {
            assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.validateCommand(inputs, changed, file, NOW)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void typedCommandTransportRejectsExtraInstructionsNumericIdsAndNonUtcTimes() throws Exception {
        var inputs = files().getInputs(); var command = command(inputs, "EDGE_BINDING", null, NOW);
        Path file = inputs.directory().resolve("requests/" + command.nonce() + ".json");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            privateJson(file, command);
            assertThat(XingyuanRuntimeProbeInitializer.readCommand(inputs, file, factory.getValidator(), NOW)).isEqualTo(command);
            var base = (com.fasterxml.jackson.databind.node.ObjectNode) XingyuanRuntimeProbeInitializer.mapper().valueToTree(command);
            for (var changed : List.of(base.deepCopy().put("bean", "arbitraryOwner"), base.deepCopy().put("entityId", 12L),
                    base.deepCopy().put("issuedAt", "2026-10-15T10:00:00+08:00"), base.deepCopy().put("schemaVersion", "1"),
                    base.deepCopy().put("operation", "EXECUTE_SQL"))) {
                privateJson(file, changed);
                assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.readCommand(inputs, file, factory.getValidator(), NOW)).isInstanceOf(Exception.class);
            }
        }
    }

    @Test
    void aClaimIsDurableBeforeExecutionAndCannotBeReplayedOrOverwriteAResult() throws Exception {
        var files = files(); var inputs = files.getInputs(); var command = command(inputs, "EDGE_BINDING", null, Instant.now());
        Path request = inputs.directory().resolve("requests/" + command.nonce() + ".json"); privateJson(request, command);
        try (var factory = Validation.buildDefaultValidatorFactory(); var context = new GenericApplicationContext()) {
            context.getBeanFactory().registerSingleton("defaultValidator", factory.getValidator());
            context.refresh();
            var runtime = new XingyuanRuntimeProbeInitializer.RuntimeProbeComponent(context, inputs, null);
            assertThat(runtime.claim(request)).isEqualTo(command);
            assertThat(Files.exists(inputs.directory().resolve("started/" + command.nonce() + ".json"))).isTrue();
            assertThatThrownBy(() -> runtime.claim(request)).isInstanceOf(java.nio.file.FileAlreadyExistsException.class);
            runtime.complete(command, "FAILED", Map.of("code", "XINGYUAN_VERIFICATION_FAILED"));
            Path result = inputs.directory().resolve("results/" + command.nonce() + ".json"); byte[] original = Files.readAllBytes(result);
            assertThatThrownBy(() -> runtime.complete(command, "COMPLETED", Map.of())).isInstanceOf(java.nio.file.FileAlreadyExistsException.class);
            assertThat(Files.readAllBytes(result)).isEqualTo(original);
            assertThat(Files.getPosixFilePermissions(result)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
        }
    }

    @Test
    void changedFrozenInputsAndUnsafeFilePermissionsNeverReachAnOwner() throws Exception {
        var inputs = files().getInputs(); var command = command(inputs, "EDGE_BINDING", null, Instant.now());
        Path request = inputs.directory().resolve("requests/" + command.nonce() + ".json"); privateJson(request, command);
        try (var factory = Validation.buildDefaultValidatorFactory(); var context = new GenericApplicationContext()) {
            context.getBeanFactory().registerSingleton("defaultValidator", factory.getValidator());
            context.refresh();
            var runtime = new XingyuanRuntimeProbeInitializer.RuntimeProbeComponent(context, inputs, null);
            Files.setPosixFilePermissions(request, PosixFilePermissions.fromString("rw-r--r--"));
            assertThatThrownBy(() -> runtime.claim(request)).isInstanceOf(IllegalArgumentException.class);
            Files.setPosixFilePermissions(request, PosixFilePermissions.fromString("rw-------"));
            privateJson(inputs.registryFile(), Map.of("runId", "foreign"));
            assertThatThrownBy(() -> runtime.claim(request)).isInstanceOf(IllegalArgumentException.class);
            try (var filesInProgress = Files.list(inputs.directory().resolve("started"))) { assertThat(filesInProgress.count()).isZero(); }
        }
    }

    @Test
    void edgeObservationLeavesRequestsAloneAndCannotTreatAnAnonymousResponseAsCanonical() throws Exception {
        var inputs = files().getInputs(); var command = command(inputs, "EDGE_BINDING", null, Instant.now());
        privateJson(inputs.directory().resolve("requests/" + command.nonce() + ".json"), command);
        try (var factory = Validation.buildDefaultValidatorFactory(); var context = new GenericApplicationContext()) {
            context.getBeanFactory().registerSingleton("defaultValidator", factory.getValidator());
            context.refresh();
            var filter = new XingyuanRuntimeProbeInitializer.EdgeObservationWebFilter(new XingyuanRuntimeProbeInitializer.RuntimeProbeComponent(context, inputs, null));
            var calls = new AtomicInteger();
            var irrelevant = MockServerWebExchange.from(MockServerHttpRequest.get("/bff/session").header(XingyuanRuntimeProbeInitializer.EDGE_NONCE_HEADER, "invalid"));
            filter.filter(irrelevant, exchange -> { calls.incrementAndGet(); return Mono.empty(); }).block();
            assertThat(calls.get()).isEqualTo(1);
            assertThat(Files.exists(inputs.directory().resolve("started/" + command.nonce() + ".json"))).isFalse();
            var selected = MockServerWebExchange.from(MockServerHttpRequest.get("/bff/session").header(XingyuanRuntimeProbeInitializer.EDGE_NONCE_HEADER, command.nonce()));
            filter.filter(selected, exchange -> { exchange.getResponse().setStatusCode(HttpStatus.OK); calls.incrementAndGet(); return Mono.empty(); }).block();
            var result = XingyuanRuntimeProbeInitializer.mapper().readTree(Files.readAllBytes(inputs.directory().resolve("results/" + command.nonce() + ".json")));
            assertThat(result.path("status").asText()).isEqualTo("FAILED");
            var fields = new java.util.HashSet<String>(); result.path("result").fieldNames().forEachRemaining(fields::add);
            assertThat(fields).containsExactly("code");
            assertThat(selected.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(calls.get()).isEqualTo(2);
        }
    }

    @Test
    void serviceCommandsUseTheExistingServiceBoundaryWithoutAUserSnapshotAction() throws Exception {
        var edge = files().getInputs();
        var service = new XingyuanRuntimeProbeInitializer.ProbeInputsVO(edge.root(), edge.manifestFile(), edge.manifestSha256(), edge.runId(), "native-service",
                edge.fixture(), edge.fixtureSha256(), edge.artifact(), edge.artifactSha256(), "owned.nativeapp", "owned-native-service", edge.registryFile(), edge.registrySha256());
        for (String operation : List.of("COURSE_CREATE", "SERVICE_AUTHORIZATION")) {
            var request = command(service, operation, null, NOW);
            assertThatCode(() -> XingyuanRuntimeProbeInitializer.validateCommand(service, request, service.directory().resolve("requests/" + request.nonce() + ".json"), NOW))
                    .doesNotThrowAnyException();
        }
        var forbidden = command(service, "PLATFORM_SNAPSHOT", null, NOW);
        assertThatThrownBy(() -> XingyuanRuntimeProbeInitializer.validateCommand(service, forbidden, service.directory().resolve("requests/" + forbidden.nonce() + ".json"), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serviceProbeTypesLoadWhenTheHostHasNoJianshenIntegration() throws Exception {
        String fixture = XingyuanRuntimeProbeInitializer.class.getName();
        var classes = XingyuanRuntimeProbeInitializer.class.getProtectionDomain().getCodeSource().getLocation();
        try (var parent = new org.springframework.boot.test.context.FilteredClassLoader("top.egon.cola.platform.tianquan.jianshen");
             var loader = new URLClassLoader(new java.net.URL[]{classes}, parent) {
                 @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                     synchronized (getClassLoadingLock(name)) {
                         if (name.equals(fixture) || name.startsWith(fixture + "$")) {
                             Class<?> selected = findLoadedClass(name);
                             if (selected == null) { selected = findClass(name); }
                             if (resolve) { resolveClass(selected); }
                             return selected;
                         }
                         return super.loadClass(name, resolve);
                     }
                 }
             }; var context = new GenericApplicationContext()) {
            assertThatThrownBy(() -> loader.loadClass("top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties"))
                    .isInstanceOf(ClassNotFoundException.class);
            var initializer = loader.loadClass(fixture); assertThat(initializer.getClassLoader()).isSameAs(loader);
            initializer.getMethod("initialize", org.springframework.context.ConfigurableApplicationContext.class)
                    .invoke(initializer.getConstructor().newInstance(), context);
            assertThat(loader.loadClass(fixture + "$RuntimeProbeComponent").getDeclaredMethods()).isNotEmpty();
        }
    }

    @Test
    void canonicalBindingProjectionExportsOnlyTheApprovedFieldsAndOriginalInstants() throws Exception {
        String id = UUID.randomUUID().toString();
        var facts = new XingyuanRuntimeProbeInitializer.EdgeBindingFactsBO(id, URI.create("https://127.0.0.1:28443/tenants/1001"), "1001", 2L, NOW, NOW.plusSeconds(3600));
        var proof = new XingyuanRuntimeProbeEdgeBindingConverterImpl().toTarget(facts);
        var json = XingyuanRuntimeProbeInitializer.mapper().valueToTree(proof);
        var fields = new java.util.HashSet<String>(); json.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("bindingReference", "issuer", "tenantId", "generation", "originalCreatedAt", "absoluteExpiresAt");
        assertThat(proof.bindingReference()).isEqualTo(XingyuanRuntimeProbeInitializer.sha256(id.getBytes(StandardCharsets.UTF_8)));
        assertThat(json.path("originalCreatedAt").asText()).isEqualTo(NOW.toString());
        assertThat(json.path("absoluteExpiresAt").asText()).isEqualTo(NOW.plusSeconds(3600).toString());
        assertThat(json.toString()).doesNotContain(id, "subject", "cookie", "token", "cipher");
    }

    private ProbeTestFilesBO files() throws Exception {
        Path repository = temporary.toRealPath().resolve(UUID.randomUUID().toString()); Files.createDirectory(repository);
        String runId = UUID.randomUUID().toString(); Path root = repository.resolve("target/xingyuan-integration-" + runId);
        Files.createDirectories(root); Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        for (String name : List.of("artifacts", "secrets", "evidence", "evidence/runtime-probe", "evidence/runtime-probe/edge",
                "evidence/runtime-probe/edge/requests", "evidence/runtime-probe/edge/started", "evidence/runtime-probe/edge/results")) {
            Files.createDirectory(root.resolve(name), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        }
        Path fixture = root.resolve("artifacts/task-runtime-probe.jar");
        try (var archive = new JarOutputStream(Files.newOutputStream(fixture))) {
            archive.putNextEntry(new JarEntry("META-INF/spring.factories"));
            archive.write(("org.springframework.context.ApplicationContextInitializer=" + XingyuanRuntimeProbeInitializer.class.getName() + "\n").getBytes(StandardCharsets.UTF_8));
            archive.closeEntry();
        }
        Files.setPosixFilePermissions(fixture, PosixFilePermissions.fromString("rw-------"));
        Path artifact = repository.resolve("owned-edge.jar"); var jarManifest = new Manifest();
        jarManifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        jarManifest.getMainAttributes().putValue("Start-Class", "top.egon.cola.component.yuheng.edge.GatewayEdgeApplication");
        try (var archive = new JarOutputStream(Files.newOutputStream(artifact), jarManifest)) { }
        String fixtureSha = XingyuanRuntimeProbeInitializer.hashFile(fixture), artifactSha = XingyuanRuntimeProbeInitializer.hashFile(artifact);
        Path manifestFile = root.resolve("intent.json");
        privateJson(manifestFile, Map.of("schemaVersion", 1, "runId", runId, "runtimeRoot", root.toString(), "repoRoot", repository.toString(),
                "artifacts", Map.of("edge", Map.of("path", artifact.toString(), "sha256", artifactSha)),
                "policyFiles", List.of(Map.of("path", fixture.toString(), "sha256", fixtureSha))));
        String digest = XingyuanRuntimeProbeInitializer.hashFile(manifestFile); Path registry = root.resolve("secrets/runtime-registry.json");
        privateJson(registry, Map.of("schemaVersion", 1, "runId", runId, "manifestSha256", digest));
        var properties = new LinkedHashMap<String, Object>();
        properties.put(XingyuanRuntimeProbeInitializer.PREFIX + "manifest-file", manifestFile.toString());
        properties.put(XingyuanRuntimeProbeInitializer.PREFIX + "role", "edge");
        properties.put(XingyuanRuntimeProbeInitializer.PREFIX + "fixture-sha256", fixtureSha);
        var environment = new StandardEnvironment(); environment.getPropertySources().addFirst(new MapPropertySource("controlled-probe", properties));
        var inputs = XingyuanRuntimeProbeInitializer.verifiedInputs(environment, fixture.toUri());
        return new ProbeTestFilesBO(inputs, environment, properties);
    }

    private static XingyuanRuntimeProbeInitializer.ProbeCommandDTO command(XingyuanRuntimeProbeInitializer.ProbeInputsVO inputs, String operation, String id, Instant now) {
        return new XingyuanRuntimeProbeInitializer.ProbeCommandDTO(1, inputs.runId(), inputs.manifestSha256(), inputs.role(), inputs.fixtureSha256(), inputs.artifactSha256(),
                UUID.randomUUID().toString(), operation, "default", id, now, now.plusSeconds(60));
    }

    private static XingyuanRuntimeProbeInitializer.ProbeCommandDTO copy(XingyuanRuntimeProbeInitializer.ProbeCommandDTO original, String runId, String hash, String role,
            String fixture, String artifact, String nonce, String operation, String id, Instant issued, Instant expires) {
        return new XingyuanRuntimeProbeInitializer.ProbeCommandDTO(original.schemaVersion(), runId, hash, role, fixture, artifact, nonce, operation, original.tenantCode(), id, issued, expires);
    }

    private static void privateJson(Path file, Object value) throws Exception {
        Files.write(file, XingyuanRuntimeProbeInitializer.mapper().writeValueAsBytes(value));
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    private static class ProbeTestFilesBO {
        private XingyuanRuntimeProbeInitializer.ProbeInputsVO inputs;
        private StandardEnvironment environment;
        private Map<String, Object> properties;
    }
}
