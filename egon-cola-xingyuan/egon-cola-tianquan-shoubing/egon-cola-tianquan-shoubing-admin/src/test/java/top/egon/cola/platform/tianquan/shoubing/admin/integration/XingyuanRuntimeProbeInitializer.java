package top.egon.cola.platform.tianquan.shoubing.admin.integration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.client.HttpRbac3AuthorizationClient;
import top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcSecurityContext;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;

/** Explicit task-only adapter loaded from a frozen thin JAR; never included in a production artifact. */
@Slf4j
public final class XingyuanRuntimeProbeInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    static final String PREFIX = "xingyuan.isolated.runtime-probe.";
    static final String EDGE_NONCE_HEADER = "X-Xingyuan-Probe-Nonce";
    static final String UUID_PATTERN = "[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}";
    static final String SHA_PATTERN = "[0-9a-f]{64}";
    private static final Map<String, String> PLATFORM_MAINS = Map.of(
            "shoubing", "top.egon.cola.platform.tianquan.shoubing.admin.IdpAdminApplication",
            "jianshen", "top.egon.cola.platform.tianquan.jianshen.admin.Rbac3AdminApplication",
            "tianshu", "top.egon.cola.component.tianshu.admin.DynamicConfigCenterAdminApplication",
            "yuheng", "top.egon.cola.component.yuheng.admin.bootstrap.GatewayAdminApplication",
            "llm", "top.egon.cola.component.yuheng.llm.bootstrap.LlmGatewayApplication");
    private static final Set<String> ROLES = Set.of("edge", "native-light", "native-web", "native-service", "shoubing", "jianshen", "tianshu", "yuheng", "llm");
    private static final Set<String> COMMAND_FIELDS = Set.of("schemaVersion", "runId", "manifestSha256", "role", "fixtureSha256", "artifactSha256",
            "nonce", "operation", "tenantCode", "entityId", "issuedAt", "expiresAt");
    private static final String EDGE_OWNER = "top.egon.cola.component.yuheng.edge.service.impl.EdgeSessionBindingComponent";
    private static final String EDGE_BINDING = "top.egon.cola.component.yuheng.edge.service.domain.bo.EdgeSessionBindingBO";
    private static final String KNOWLEDGE_OBSERVATION = "xingyuanKnowledgeInvocationObservationComponent";
    private static final String GRAPHQL_OBSERVATION = "xingyuanGraphQlAsyncObservationComponent";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        if (!"true".equals(context.getEnvironment().getProperty(PREFIX + "enabled"))) { return; }
        if (!"true".equals(System.getenv("XINGYUAN_ISOLATED_INTEGRATION"))) { throw rejected(); }
        try {
            URI location = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            var inputs = verifiedInputs(context.getEnvironment(), location);
            var observation = businessObservation(inputs);
            if (observation != null) { context.getBeanFactory().registerSingleton("xingyuanInvocationObservationComponent", observation); }
            if ("yuheng".equals(inputs.role())) {
                context.getBeanFactory().registerSingleton(KNOWLEDGE_OBSERVATION, new InvocationObservationComponent("knowledgeController",
                        "top.egon.cola.component.yuheng.admin.knowledge.controller.KnowledgeController",
                        Map.of("listKnowledgeBases", 3, "createKnowledgeBase", 3, "getKnowledgeBase", 2)));
            }
            GraphQlAsyncObservationComponent graphQl = null;
            if (Set.of("native-light", "native-web").contains(inputs.role())) {
                graphQl = new GraphQlAsyncObservationComponent(); graphQl.configure(inputs, context.getClassLoader());
                context.getBeanFactory().registerSingleton(GRAPHQL_OBSERVATION, graphQl);
                observation.contextObservation = graphQl::observeBusinessContext;
            }
            final var selectedGraphQl = graphQl;
            var runtime = new RuntimeProbeComponent(context, inputs, observation);
            context.getBeanFactory().registerSingleton("xingyuanRuntimeProbeComponent", runtime);
            if ("edge".equals(inputs.role())) {
                context.getBeanFactory().registerSingleton("xingyuanRuntimeProbeEdgeWebFilter", new EdgeObservationWebFilter(runtime));
            }
            context.addApplicationListener(event -> {
                if (event instanceof ApplicationReadyEvent ready && ready.getApplicationContext() == context) { runtime.start(); }
                if (event instanceof ContextClosedEvent closed && closed.getApplicationContext() == context) {
                    runtime.close(); if (selectedGraphQl != null) { selectedGraphQl.close(); }
                }
            });
        } catch (Exception failure) {
            throw new IllegalStateException("XINGYUAN_RUNTIME_PROBE_REJECTED", failure);
        }
    }

    static ProbeInputsVO verifiedInputs(Environment environment, URI fixtureLocation) throws Exception {
        String role = environment.getRequiredProperty(PREFIX + "role");
        String fixtureSha = environment.getRequiredProperty(PREFIX + "fixture-sha256");
        if (!ROLES.contains(role) || !fixtureSha.matches(SHA_PATTERN) || !"file".equals(fixtureLocation.getScheme())) { throw rejected(); }
        Path manifestFile = Path.of(environment.getRequiredProperty(PREFIX + "manifest-file"));
        requireAbsolute(manifestFile);
        byte[] manifestBytes = readPrivate(manifestFile, manifestFile.getParent(), 128 * 1024);
        var manifest = mapper().readTree(manifestBytes);
        String runId = text(manifest, "runId");
        if (!runId.matches(UUID_PATTERN)) { throw rejected(); }
        Path repository = Path.of(text(manifest, "repoRoot")); Path root = Path.of(text(manifest, "runtimeRoot"));
        requireAbsolute(repository); requireDirectory(root);
        if (!root.equals(repository.resolve("target/xingyuan-integration-" + runId)) || !manifestFile.startsWith(root)) { throw rejected(); }
        Path fixture = root.resolve("artifacts/task-runtime-probe.jar");
        if (!fixture.equals(Path.of(fixtureLocation)) || !fixtureSha.equals(sha256(readPrivate(fixture, root, 8 * 1024 * 1024)))) { throw rejected(); }
        boundFile(manifest, "policyFiles", fixture, fixtureSha);
        var artifactReference = manifest.path("artifacts").path(role);
        Path artifact = Path.of(text(artifactReference, "path")); String artifactSha = text(artifactReference, "sha256");
        if (!artifactSha.matches(SHA_PATTERN) || !artifactSha.equals(hashFile(artifact))
                || artifact.startsWith(repository.resolve("egon-cola-archetypes/source-projects"))) { throw rejected(); }
        String namespace;
        try (var archive = new JarFile(artifact.toFile())) {
            if (archive.getManifest() == null) { throw rejected(); }
            String startClass = archive.getManifest().getMainAttributes().getValue("Start-Class");
            namespace = generatedNamespace(role, startClass);
        }
        Path registryFile = root.resolve("secrets/runtime-registry.json");
        byte[] registryBytes = readPrivate(registryFile, root, 2 * 1024 * 1024);
        var registry = mapper().readTree(registryBytes); String manifestSha = sha256(manifestBytes);
        if (!runId.equals(text(registry, "runId")) || !manifestSha.equals(text(registry, "manifestSha256"))) { throw rejected(); }
        String applicationCode = "edge".equals(role) ? "yuheng-edge-gateway" : role.startsWith("native-") ? nativeApplicationCode(root, manifest, role) : null;
        return new ProbeInputsVO(root, manifestFile, manifestSha, runId, role, fixture, fixtureSha, artifact, artifactSha,
                namespace, applicationCode, registryFile, sha256(registryBytes));
    }

    static String generatedNamespace(String role, String startClass) {
        if (PLATFORM_MAINS.containsKey(role)) {
            if (!PLATFORM_MAINS.get(role).equals(startClass)) { throw rejected(); }
            return startClass.substring(0, startClass.lastIndexOf('.'));
        }
        if ("edge".equals(role)) {
            if (!"top.egon.cola.component.yuheng.edge.GatewayEdgeApplication".equals(startClass)) { throw rejected(); }
            return "top.egon.cola.component.yuheng.edge";
        }
        String suffix = switch (role) {
            case "native-light" -> ".start.StudentManagementApplication";
            case "native-web" -> ".starter.OrganizationApplication";
            case "native-service" -> ".starter.EvaluationServiceApplication";
            default -> throw rejected();
        };
        if (startClass == null || !startClass.endsWith(suffix) || startClass.startsWith("top.egon.cola.archetype.source.")) { throw rejected(); }
        String namespace = startClass.substring(0, startClass.length() - suffix.length());
        if (!namespace.matches("[a-z][A-Za-z0-9_]*(?:\\.[a-z][A-Za-z0-9_]*)+")) { throw rejected(); }
        return namespace;
    }

    private static String nativeApplicationCode(Path root, JsonNode manifest, String role) throws Exception {
        Path recipeFile = root.resolve("policies/native-authorization.json");
        byte[] recipeBytes = readPrivate(recipeFile, root, 128 * 1024); boundFile(manifest, "policyFiles", recipeFile, sha256(recipeBytes));
        var recipe = mapper().readTree(recipeBytes);
        if (!text(manifest, "runId").equals(text(recipe, "runId"))) { throw rejected(); }
        var applications = recipe.path("applications");
        var selected = java.util.stream.StreamSupport.stream(applications.spliterator(), false).filter(value -> role.equals(value.path("processId").asText())).toList();
        if (selected.size() != 1) { throw rejected(); }
        Path catalog = Path.of(text(selected.getFirst(), "catalogFile")); byte[] bytes = readPrivate(catalog, root, 2 * 1024 * 1024);
        boundFile(manifest, "catalogFiles", catalog, sha256(bytes));
        String code = text(mapper().readTree(bytes), "applicationCode");
        if (!code.matches("[a-z][a-z0-9-]{2,127}")) { throw rejected(); }
        return code;
    }

    private static void boundFile(JsonNode manifest, String collection, Path path, String hash) {
        var matches = java.util.stream.StreamSupport.stream(manifest.path(collection).spliterator(), false)
                .filter(value -> path.toString().equals(value.path("path").asText())).toList();
        if (matches.size() != 1 || !hash.equals(matches.getFirst().path("sha256").asText())) { throw rejected(); }
    }

    /** Fixed task worker; callback/context receipts contain counts only. */
    @Slf4j
    static final class GraphQlAsyncObservationComponent implements AutoCloseable, org.springframework.beans.factory.config.BeanPostProcessor {
        private final java.util.concurrent.ThreadPoolExecutor worker = new java.util.concurrent.ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new java.util.concurrent.ArrayBlockingQueue<>(16), runnable -> {
                    var thread = new Thread(runnable, "xingyuan-graphql-probe"); thread.setDaemon(true); return thread;
                }, new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        private final java.util.concurrent.atomic.AtomicLong differentThread = new java.util.concurrent.atomic.AtomicLong();
        private final java.util.concurrent.atomic.AtomicLong matchedContext = new java.util.concurrent.atomic.AtomicLong();
        private final java.util.concurrent.atomic.AtomicLong finallyRestored = new java.util.concurrent.atomic.AtomicLong();
        private final java.util.concurrent.atomic.AtomicLong failure = new java.util.concurrent.atomic.AtomicLong();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean poisoned = new AtomicBoolean();
        private java.util.function.Supplier<?> businessContext = java.util.Optional::empty;
        private final ThreadLocal<Object[]> expectedContext = new ThreadLocal<>();
        private String namespace;
        private Class<?> fetcherType;
        private Class<?> environmentType;
        private Class<?> snapshotType;
        private java.lang.reflect.Method fetch;
        private volatile Object installed;
        private org.springframework.aop.Advisor[] originalAdvisors;
        private org.springframework.aop.Advisor decorator;

        void configure(ProbeInputsVO inputs, ClassLoader loader) throws ReflectiveOperationException {
            if (namespace != null || !Set.of("native-light", "native-web").contains(inputs.role())) { throw rejected(); }
            namespace = inputs.namespace();
            fetcherType = Class.forName("graphql.schema.DataFetcher", false, loader);
            environmentType = Class.forName("graphql.schema.DataFetchingEnvironment", false, loader);
            snapshotType = Class.forName(namespace + ".adapter.graphql.NativeGraphQlSecuritySnapshotBO", false, loader);
            fetch = fetcherType.getMethod("get", environmentType);
            var holder = Class.forName(namespace + (inputs.role().equals("native-light")
                    ? ".adapter.filter.RequestContextHolder" : ".application.context.OrganizationRequestContextHolder"), false, loader);
            var current = holder.getMethod(inputs.role().equals("native-light") ? "get" : "current");
            businessContext = () -> {
                try { return current.invoke(null); }
                catch (ReflectiveOperationException failure) { throw rejected(); }
            };
        }

        @Override
        public synchronized Object postProcessAfterInitialization(Object bean, String name) {
            if (!"nativeGraphQlSecurityInstrumentation".equals(name)) { return bean; }
            if (installed != null || namespace == null
                    || !AopUtils.getTargetClass(bean).getName().equals(namespace + ".adapter.graphql.NativeGraphQlSecurityInstrumentation")) { throw rejected(); }
            Object result = bean;
            org.springframework.aop.framework.Advised advised;
            if (bean instanceof org.springframework.aop.framework.Advised proxy) { advised = proxy; }
            else {
                if (AopUtils.isAopProxy(bean)) { throw rejected(); }
                var factory = new org.springframework.aop.framework.ProxyFactory(bean); factory.setProxyTargetClass(true);
                result = factory.getProxy(bean.getClass().getClassLoader()); advised = (org.springframework.aop.framework.Advised) result;
            }
            if (advised.isFrozen()) { throw rejected(); }
            originalAdvisors = advised.getAdvisors();
            decorator = new org.springframework.aop.support.DefaultPointcutAdvisor((org.aopalliance.intercept.MethodInterceptor) invocation -> {
                Object secured = invocation.proceed();
                if (!invocation.getMethod().getName().equals("instrumentDataFetcher")) { return secured; }
                if (!fetcherType.isInstance(secured) || invocation.getArguments().length != 3) { throw rejected(); }
                return java.lang.reflect.Proxy.newProxyInstance(fetcherType.getClassLoader(), new Class<?>[]{fetcherType}, (proxy, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) { return method.invoke(secured, arguments); }
                    if (!method.getName().equals("get") || arguments == null || arguments.length != 1 || !environmentType.isInstance(arguments[0])) { throw rejected(); }
                    Object environment = arguments[0];
                    Object parent = environmentType.getMethod("getParentType").invoke(environment);
                    Object field = environmentType.getMethod("getFieldDefinition").invoke(environment);
                    if (!"Query".equals(parent.getClass().getMethod("getName").invoke(parent))
                            || !"user".equals(field.getClass().getMethod("getName").invoke(field))) { return invokeFetcher(secured, environment); }
                    Object graphQlContext = environmentType.getMethod("getGraphQlContext").invoke(environment);
                    var contextGet = graphQlContext.getClass().getMethod("get", Object.class);
                    Object snapshot = contextGet.invoke(graphQlContext, namespace + ".adapter.graphql.NativeGraphQlSecurityInterceptor.snapshot");
                    Object request = contextGet.invoke(graphQlContext, namespace + ".adapter.graphql.NativeGraphQlSecurityInterceptor.request");
                    if (!snapshotType.isInstance(snapshot) || !(request instanceof jakarta.servlet.http.HttpServletRequest)) { throw rejected(); }
                    return dispatchOnWorker(() -> {
                        if (expectedContext.get() != null) { throw rejected(); }
                        expectedContext.set(new Object[]{snapshot, request});
                        try { return invokeFetcher(secured, environment); }
                        finally { expectedContext.remove(); }
                    }).thenCompose(value -> value instanceof java.util.concurrent.CompletionStage<?> stage
                            ? stage.thenApply(completed -> (Object) completed) : java.util.concurrent.CompletableFuture.completedFuture(value));
                });
            });
            advised.addAdvisor(originalAdvisors.length, decorator); installed = result; validateInstalled(result); return result;
        }

        void validateInstalled(Object bean) {
            if (bean != installed || !(bean instanceof org.springframework.aop.framework.Advised advised)) { throw rejected(); }
            var advisors = advised.getAdvisors();
            if (advisors.length != originalAdvisors.length + 1 || advisors[originalAdvisors.length] != decorator) { throw rejected(); }
            for (int index = 0; index < originalAdvisors.length; index++) { if (advisors[index] != originalAdvisors[index]) { throw rejected(); } }
        }

        private Object invokeFetcher(Object secured, Object environment) throws Exception {
            try { return fetch.invoke(secured, environment); }
            catch (java.lang.reflect.InvocationTargetException wrapped) {
                if (wrapped.getCause() instanceof Error error) { throw error; }
                if (wrapped.getCause() instanceof Exception original) { throw original; }
                throw rejected();
            }
        }

        void observeBusinessContext() {
            Object[] expected = expectedContext.get(); if (expected == null) { return; }
            try {
                Object snapshot = expected[0];
                var authorization = (top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeAuthorizationContext)
                        snapshotType.getMethod("authorizationContext").invoke(snapshot);
                var current = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (!(current instanceof top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3AuthenticationToken token)
                        || !token.isAuthenticated() || token.context().fenced()
                        || !token.context().identity().equals(authorization.identity()) || token.context().snapshot() != authorization.snapshot()
                        || token.getDetails() != snapshotType.getMethod("auditProof").invoke(snapshot)
                        || !(businessContext.get() instanceof java.util.Optional<?> business)
                        || business.orElse(null) != snapshotType.getMethod("businessContext").invoke(snapshot)
                        || !(org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes attributes)
                        || attributes.getRequest() != expected[1]
                        || !top.egon.cola.component.common.trace.TraceContext.capture().mdcContext().equals(
                                ((top.egon.cola.component.common.trace.TraceContext) snapshotType.getMethod("traceContext").invoke(snapshot)).mdcContext())) { throw rejected(); }
                var scopes = token.getAuthorities().stream().map(org.springframework.security.core.GrantedAuthority::getAuthority)
                        .filter(value -> value.startsWith("SCOPE_")).map(value -> value.substring(6)).collect(java.util.stream.Collectors.toSet());
                if (!scopes.equals(snapshotType.getMethod("verifiedScopes").invoke(snapshot))) { throw rejected(); }
                matchedContext.incrementAndGet();
            } catch (ReflectiveOperationException | RuntimeException invalid) {
                failure.incrementAndGet(); poisoned.set(true); throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE", invalid);
            }
        }

        java.util.concurrent.CompletableFuture<Object> dispatchOnWorker(java.util.concurrent.Callable<Object> callback) {
            if (closed.get() || poisoned.get()) { throw rejected(); }
            long caller = Thread.currentThread().threadId(); var result = new java.util.concurrent.CompletableFuture<Object>();
            var task = worker.submit(() -> {
                try { result.complete(invokeCallback(callback, caller)); }
                catch (Exception | Error original) { result.completeExceptionally(original); }
            });
            result.orTimeout(30, TimeUnit.SECONDS).whenComplete((value, original) -> {
                if (original instanceof java.util.concurrent.TimeoutException || result.isCancelled()) {
                    task.cancel(true); failure.incrementAndGet(); poisoned.set(true); close();
                }
            });
            return result;
        }

        Object callOnWorker(java.util.concurrent.Callable<Object> callback) throws Exception {
            var pending = dispatchOnWorker(callback);
            try { return pending.get(30, TimeUnit.SECONDS); }
            catch (java.util.concurrent.ExecutionException wrapped) {
                Throwable cause = wrapped.getCause();
                if (cause instanceof Error error) { throw error; }
                if (cause instanceof Exception checked) { throw checked; }
                throw rejected();
            } catch (InterruptedException interrupted) {
                pending.cancel(true); Thread.currentThread().interrupt(); throw interrupted;
            } catch (java.util.concurrent.TimeoutException timeout) {
                pending.cancel(true); failure.incrementAndGet(); poisoned.set(true); close(); throw timeout;
            }
        }

        private Object invokeCallback(java.util.concurrent.Callable<Object> callback, long caller) throws Exception {
            var security = org.springframework.security.core.context.SecurityContextHolder.getContext();
            var authentication = security.getAuthentication();
            var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            var business = businessContext.get(); var mdc = org.slf4j.MDC.getCopyOfContextMap();
            var trace = top.egon.cola.component.common.trace.TraceContext.current();
            if (Thread.currentThread().threadId() == caller || authentication != null || attributes != null
                    || !(business instanceof java.util.Optional<?> value) || value.isPresent() || trace.isPresent() || mdc != null && !mdc.isEmpty()) {
                failure.incrementAndGet(); poisoned.set(true); throw rejected();
            }
            differentThread.incrementAndGet(); Throwable primary = null;
            try { return callback.call(); }
            catch (Exception | Error original) { primary = original; throw original; }
            finally {
                boolean restored = org.springframework.security.core.context.SecurityContextHolder.getContext() == security
                        && security.getAuthentication() == authentication
                        && org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() == attributes
                        && Objects.equals(businessContext.get(), business) && Objects.equals(org.slf4j.MDC.getCopyOfContextMap(), mdc)
                        && top.egon.cola.component.common.trace.TraceContext.current().equals(trace);
                if (restored) { finallyRestored.incrementAndGet(); }
                else {
                    failure.incrementAndGet(); poisoned.set(true); close(); var rejected = rejected();
                    if (primary == null) { throw rejected; } else { primary.addSuppressed(rejected); }
                }
            }
        }

        Map<String, Long> snapshot() {
            return Map.of("differentThread", differentThread.get(), "matchedContext", matchedContext.get(),
                    "finallyRestored", finallyRestored.get(), "failure", failure.get());
        }

        @Override public void close() {
            if (!closed.compareAndSet(false, true)) { return; }
            for (Runnable pending : worker.shutdownNow()) {
                if (pending instanceof java.util.concurrent.Future<?> future) { future.cancel(false); }
            }
        }
    }

    /** Counts only the reviewed original business boundary; never records arguments or results. */
    @Slf4j
    @RequiredArgsConstructor
    static final class InvocationObservationComponent implements org.springframework.beans.factory.config.BeanPostProcessor {
        private final String beanName;
        private final String implementationName;
        private final Map<String, Integer> methods;
        private final Map<String, java.util.concurrent.atomic.AtomicLong> counts = new java.util.concurrent.ConcurrentHashMap<>();
        private volatile Object observed;
        private org.springframework.aop.Advisor[] originalAdvisors;
        private org.springframework.aop.Advisor counter;
        private Runnable contextObservation;

        @Override
        public synchronized Object postProcessAfterInitialization(Object bean, String name) {
            if (!beanName.equals(name)) { return bean; }
            Class<?> target = AopUtils.getTargetClass(bean);
            if (observed != null || !implementationName.equals(target.getName()) || methods.isEmpty()) { throw rejected(); }
            for (String method : methods.keySet()) {
                var matches = Arrays.stream(target.getMethods()).filter(value -> value.getName().equals(method) && !value.isBridge() && !value.isSynthetic()).toList();
                if (matches.size() != 1 || methods.get(method) == null || methods.get(method) < 0 || methods.get(method) > 12
                        || matches.getFirst().getParameterCount() != methods.get(method)) { throw rejected(); }
            }
            Object result = bean;
            org.springframework.aop.framework.Advised advised;
            if (bean instanceof org.springframework.aop.framework.Advised proxy) { advised = proxy; }
            else {
                if (AopUtils.isAopProxy(bean)) { throw rejected(); }
                var factory = new org.springframework.aop.framework.ProxyFactory(bean);
                factory.setProxyTargetClass(true);
                result = factory.getProxy(bean.getClass().getClassLoader());
                advised = (org.springframework.aop.framework.Advised) result;
            }
            if (advised.isFrozen()) { throw rejected(); }
            originalAdvisors = advised.getAdvisors();
            counter = new org.springframework.aop.support.DefaultPointcutAdvisor((org.aopalliance.intercept.MethodInterceptor) invocation -> {
                if (methods.containsKey(invocation.getMethod().getName())) {
                    counts.computeIfAbsent(invocation.getMethod().getName(), ignored -> new java.util.concurrent.atomic.AtomicLong()).incrementAndGet();
                    if (contextObservation != null && Set.of("get", "getUser").contains(invocation.getMethod().getName())) { contextObservation.run(); }
                }
                return invocation.proceed();
            });
            advised.addAdvisor(originalAdvisors.length, counter);
            observed = result;
            snapshot(result);
            return result;
        }

        Map<String, Long> snapshot(Object current) {
            if (current != observed || !(current instanceof org.springframework.aop.framework.Advised advised)) { throw rejected(); }
            var actual = advised.getAdvisors();
            if (actual.length != originalAdvisors.length + 1 || actual[originalAdvisors.length] != counter) { throw rejected(); }
            for (int index = 0; index < originalAdvisors.length; index++) { if (actual[index] != originalAdvisors[index]) { throw rejected(); } }
            Map<String, Long> result = new java.util.TreeMap<>();
            methods.keySet().forEach(method -> result.put(method, counts.getOrDefault(method, new java.util.concurrent.atomic.AtomicLong()).get()));
            if (result.values().stream().anyMatch(value -> value < 0)) { throw rejected(); }
            return java.util.Collections.unmodifiableMap(result);
        }
    }

    private static InvocationObservationComponent businessObservation(ProbeInputsVO inputs) {
        return switch (inputs.role()) {
            case "native-light" -> new InvocationObservationComponent("userManageImpl", inputs.namespace() + ".application.user.manage.impl.UserManageImpl", Map.of("create", 1, "get", 1));
            case "native-web" -> new InvocationObservationComponent("userManage", inputs.namespace() + ".application.user.manage.impl.UserManageImpl", Map.of("createUser", 1, "getUser", 1));
            case "native-service" -> new InvocationObservationComponent("courseManage", inputs.namespace() + ".application.course.manage.impl.CourseManageImpl", Map.of("create", 1, "get", 1, "page", 1, "schedule", 1));
            case "shoubing" -> new InvocationObservationComponent("OAuthClientServiceImpl", "top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthClientServiceImpl", Map.of("list", 1));
            case "jianshen" -> new InvocationObservationComponent("roleController", "top.egon.cola.platform.tianquan.jianshen.admin.iam.role.controller.RoleController", Map.of("roles", 1));
            case "tianshu" -> new InvocationObservationComponent("ddcRegistryAdminPageService", "top.egon.cola.component.tianshu.admin.service.management.DdcRegistryAdminPageService", Map.of("pageServices", 2));
            case "yuheng" -> new InvocationObservationComponent("gatewayScopeService", "top.egon.cola.component.yuheng.admin.scope.service.GatewayScopeService", Map.of("list", 0));
            case "llm" -> new InvocationObservationComponent("llmApiController", "top.egon.cola.component.yuheng.llm.proxy.controller.LlmApiController", Map.of("listLlmModels", 2, "createLlmChatCompletion", 2,
                    "createLlmEmbeddings", 2, "createLlmResponse", 2, "createLlmMessage", 2));
            default -> null;
        };
    }

    record ProbeInputsVO(Path root, Path manifestFile, String manifestSha256, String runId, String role, Path fixture, String fixtureSha256,
            Path artifact, String artifactSha256, String namespace, String applicationCode, Path registryFile, String registrySha256) {
        Path directory() { return root.resolve("evidence/runtime-probe/" + role); }
    }

    record ProbeCommandDTO(@Min(1) @Max(1) int schemaVersion, @NotBlank String runId, @NotBlank String manifestSha256,
            @NotBlank String role, @NotBlank String fixtureSha256, @NotBlank String artifactSha256,
            @NotBlank @Pattern(regexp = UUID_PATTERN) String nonce, @NotBlank String operation,
            @NotBlank @Pattern(regexp = "default|tenant-b") String tenantCode,
            @Pattern(regexp = "[1-9][0-9]{0,18}") String entityId, @NotNull Instant issuedAt, @NotNull Instant expiresAt) { }

    static ProbeCommandDTO readCommand(ProbeInputsVO inputs, Path file, Validator validator, Instant now) throws IOException {
        var json = mapper().readTree(readPrivate(file, inputs.root(), 16 * 1024));
        Set<String> fields = new HashSet<>(); json.fieldNames().forEachRemaining(fields::add);
        if (!fields.equals(COMMAND_FIELDS) || !json.path("schemaVersion").isInt()) { throw rejected(); }
        for (String key : List.of("issuedAt", "expiresAt")) {
            if (!json.path(key).isTextual() || !json.path(key).asText().endsWith("Z")) { throw rejected(); }
        }
        if (!json.path("entityId").isNull() && !json.path("entityId").isTextual()) { throw rejected(); }
        var command = mapper().treeToValue(json, ProbeCommandDTO.class);
        new ValidationUtils(validator).validate(command);
        validateCommand(inputs, command, file, now);
        return command;
    }

    static void validateCommand(ProbeInputsVO inputs, ProbeCommandDTO command, Path file, Instant now) {
        if (!inputs.directory().resolve("requests/" + command.nonce() + ".json").equals(file)
                || !inputs.runId().equals(command.runId()) || !inputs.manifestSha256().equals(command.manifestSha256())
                || !inputs.role().equals(command.role()) || !inputs.fixtureSha256().equals(command.fixtureSha256())
                || !inputs.artifactSha256().equals(command.artifactSha256()) || command.issuedAt().isAfter(now)
                || !command.expiresAt().isAfter(now) || !command.expiresAt().isAfter(command.issuedAt())
                || Duration.between(command.issuedAt(), command.expiresAt()).compareTo(Duration.ofSeconds(60)) > 0) { throw rejected(); }
        Set<String> allowed = switch (inputs.role()) {
            case "edge" -> Set.of("EDGE_BINDING");
            case "native-light" -> Set.of("BUSINESS_COUNTS", "GRAPHQL_CONTEXT_COUNTS");
            case "yuheng" -> Set.of("BUSINESS_COUNTS", "KNOWLEDGE_COUNTS", "SECURITY_METRICS", "SHARED_STATE_PAUSE", "SHARED_STATE_RESUME");
            case "shoubing", "jianshen", "tianshu", "llm" -> Set.of("BUSINESS_COUNTS");
            case "native-web" -> Set.of("WEB_COURSE_READ", "PLATFORM_SNAPSHOT", "BUSINESS_COUNTS", "GRAPHQL_CONTEXT_COUNTS");
            case "native-service" -> Set.of("COURSE_CREATE", "SERVICE_USER_READ", "SERVICE_AUTHORIZATION", "BUSINESS_COUNTS");
            default -> throw rejected();
        };
        boolean readsEntity = Set.of("WEB_COURSE_READ", "SERVICE_USER_READ").contains(command.operation());
        if (!allowed.contains(command.operation()) || readsEntity != (command.entityId() != null)
                || command.entityId() != null && (!command.entityId().matches("[1-9][0-9]{0,18}") || Long.parseLong(command.entityId()) <= 0)) { throw rejected(); }
    }

    record SecurityMetricProofVO(String signal, String result, String receiver, String type, long count, double value) { }

    static List<SecurityMetricProofVO> securityMetrics(io.micrometer.core.instrument.MeterRegistry registry, Validator validator) {
        Set<String> signals = Set.of("jwt.verify.duration", "jwks.cache.access", "token.cache.access", "token.acquire.duration", "oauth.authentication",
                "rbac.authorization", "oauth.audit.delivery", "receipt.age", "spool.bytes");
        Set<String> timers = Set.of("jwt.verify.duration", "token.acquire.duration", "receipt.age");
        var output = new ArrayList<SecurityMetricProofVO>(); var identities = new java.util.HashSet<String>();
        if (registry == null || registry.isClosed() || registry.getMeters().size() > 10000) { throw rejected(); }
        for (var meter : registry.getMeters()) {
            var id = meter.getId(); String name = id.getName();
            if (!signals.contains(name)) {
                if (signals.stream().anyMatch(signal -> name.startsWith(signal + '.'))) { throw rejected(); }
                continue;
            }
            if (id.getTags().size() != 2 || !id.getTags().stream().map(io.micrometer.core.instrument.Tag::getKey).collect(java.util.stream.Collectors.toSet()).equals(Set.of("receiver", "result"))
                    || !"YUHENG_ADMIN".equals(id.getTag("receiver")) || !identities.add(name + ':' + id.getTag("result"))) { throw rejected(); }
            new ValidationUtils(validator).validate(top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO.builder()
                    .signal(name).result(id.getTag("result")).duration(timers.contains(name) ? Duration.ZERO : null).build());
            long count; double value; String type;
            if (timers.contains(name) && meter instanceof io.micrometer.core.instrument.Timer timer) {
                count = timer.count(); value = timer.totalTime(TimeUnit.SECONDS); type = "TIMER";
            } else if ("spool.bytes".equals(name) && meter instanceof io.micrometer.core.instrument.Gauge gauge) {
                count = 0; value = gauge.value(); type = "GAUGE";
            } else if (!timers.contains(name) && !"spool.bytes".equals(name) && meter instanceof io.micrometer.core.instrument.Counter counter) {
                double observed = counter.count();
                if (!Double.isFinite(observed) || observed < 0 || observed > 9007199254740991L || observed != Math.rint(observed)) { throw rejected(); }
                count = (long) observed; value = 0; type = "COUNTER";
            } else { throw rejected(); }
            if (count < 0 || !Double.isFinite(value) || value < 0) { throw rejected(); }
            output.add(new SecurityMetricProofVO(name, id.getTag("result"), id.getTag("receiver"), type, count, value));
        }
        if (output.size() > 64) { throw rejected(); }
        return output.stream().sorted(Comparator.comparing(SecurityMetricProofVO::signal).thenComparing(SecurityMetricProofVO::result)).toList();
    }

    /** A bounded pause of the already-owned client IO; no endpoint, ACL, Redis server or global event loop is modified. */
    @RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    static final class SharedStatePauseLease implements AutoCloseable {
        private final java.util.concurrent.CountDownLatch release;
        private final long deadlineNanos;
        private final Instant resumesAt;
        private final int eventLoopCount;

        static SharedStatePauseLease start(io.netty.channel.EventLoopGroup group, Duration duration) throws InterruptedException {
            if (group == null || group.isShuttingDown() || duration == null || duration.isNegative() || duration.isZero() || duration.compareTo(Duration.ofSeconds(30)) > 0) { throw rejected(); }
            var loops = new ArrayList<io.netty.util.concurrent.EventExecutor>();
            for (var loop : group) { if (loops.size() == 64) { throw rejected(); } loops.add(loop); }
            if (loops.isEmpty() || loops.stream().anyMatch(io.netty.util.concurrent.EventExecutor::inEventLoop)) { throw rejected(); }
            var lease = new SharedStatePauseLease(new java.util.concurrent.CountDownLatch(1), System.nanoTime() + duration.toNanos(), Instant.now().plus(duration), loops.size());
            var entered = new java.util.concurrent.CountDownLatch(loops.size()); boolean ready = false;
            try {
                for (var loop : loops) {
                    loop.execute(() -> {
                        entered.countDown();
                        try { lease.release.await(Math.max(0, lease.deadlineNanos - System.nanoTime()), TimeUnit.NANOSECONDS); }
                        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                    });
                }
                if (!entered.await(Math.min(duration.toNanos(), Duration.ofSeconds(2).toNanos()), TimeUnit.NANOSECONDS) || lease.expired()) { throw rejected(); }
                ready = true; return lease;
            } finally { if (!ready) { lease.close(); } }
        }

        boolean expired() { return deadlineNanos - System.nanoTime() <= 0 || release.getCount() == 0; }
        @Override public void close() { release.countDown(); }
    }

    static io.netty.channel.EventLoopGroup ownedSharedStateGroup(ConfigurableApplicationContext context, ProbeInputsVO inputs) throws Exception {
        if (!"yuheng".equals(inputs.role())) { throw rejected(); }
        var client = context.getBean("rbac3RuntimeRedissonClient", org.redisson.api.RedissonClient.class);
        if (!(client instanceof org.redisson.Redisson actual) || context.getBean("idpAuditRedissonClient", org.redisson.api.RedissonClient.class) != client
                || client.isShutdown() || client.isShuttingDown()) { throw rejected(); }
        byte[] manifestBytes = readPrivate(inputs.manifestFile(), inputs.root(), 128 * 1024);
        if (!inputs.manifestSha256().equals(sha256(manifestBytes))) { throw rejected(); }
        var manifest = mapper().readTree(manifestBytes);
        var database = manifest.path("redis").path("database"); var config = actual.getConfig();
        if (!database.isInt() || database.intValue() < 1 || database.intValue() > 15
                || !"redis://127.0.0.1:6379".equals(manifest.path("redis").path("endpoint").asText()) || !config.isSingleConfig() || config.getEventLoopGroup() != null) { throw rejected(); }
        var manager = actual.getConnectionManager().getServiceManager(); var configured = manager.getConfig();
        if (configured.getDatabase() != database.intValue() || !"redis://127.0.0.1:6379".equals(configured.getMasterAddress())) { throw rejected(); }
        String marker = client.<String>getBucket("xingyuan-it:owner", org.redisson.client.codec.StringCodec.INSTANCE).get();
        if (!("xingyuan-it:" + inputs.runId() + ':' + inputs.manifestSha256()).equals(marker)) { throw rejected(); }
        return manager.getGroup();
    }

    /** Programmatic composition belongs to the initializer; original application beans are resolved only after readiness. */
    @Slf4j
    @RequiredArgsConstructor
    static final class RuntimeProbeComponent implements AutoCloseable {
        private final ConfigurableApplicationContext context;
        private final ProbeInputsVO inputs;
        private final InvocationObservationComponent observation;
        private final AtomicBoolean started = new AtomicBoolean();
        private ScheduledExecutorService executor;
        private volatile SharedStatePauseLease sharedStatePause;

        void start() {
            if (!started.compareAndSet(false, true)) { return; }
            try {
                stableInputs();
                if (observation != null) { observation.snapshot(context.getBean(observation.beanName)); }
                if (Set.of("native-light", "native-web").contains(inputs.role())) {
                    context.getBean(GRAPHQL_OBSERVATION, GraphQlAsyncObservationComponent.class)
                            .validateInstalled(context.getBean("nativeGraphQlSecurityInstrumentation"));
                }
                if ("yuheng".equals(inputs.role())) {
                    var knowledge = context.getBean(KNOWLEDGE_OBSERVATION, InvocationObservationComponent.class);
                    knowledge.snapshot(context.getBean(knowledge.beanName));
                }
                for (Path path : List.of(inputs.root().resolve("evidence/runtime-probe"), inputs.directory(), inputs.directory().resolve("requests"),
                        inputs.directory().resolve("started"), inputs.directory().resolve("results"))) { privateDirectory(path); }
                writeNew(inputs.directory().resolve("ready.json"), Map.of("schemaVersion", 1, "runId", inputs.runId(), "manifestSha256", inputs.manifestSha256(),
                        "role", inputs.role(), "fixtureSha256", inputs.fixtureSha256(), "artifactSha256", inputs.artifactSha256(), "status", "READY",
                        "pid", ProcessHandle.current().pid(), "processStartedAt", ProcessHandle.current().info().startInstant().orElseThrow().truncatedTo(java.time.temporal.ChronoUnit.SECONDS), "recordedAt", Instant.now()));
                if (!"edge".equals(inputs.role())) {
                    executor = Executors.newSingleThreadScheduledExecutor(task -> { var thread = new Thread(task, "xingyuan-task-runtime-probe"); thread.setDaemon(true); return thread; });
                    executor.scheduleWithFixedDelay(this::poll, 0, 200, TimeUnit.MILLISECONDS);
                }
            } catch (IOException failure) { throw new IllegalStateException("XINGYUAN_RUNTIME_PROBE_REJECTED", failure); }
        }

        private void poll() {
            try (var files = Files.list(inputs.directory().resolve("requests"))) {
                var requests = files.limit(129).toList();
                if (requests.size() > 128) { throw rejected(); }
                for (Path request : requests) {
                    if (!request.getFileName().toString().matches(UUID_PATTERN + "\\.json")) { throw rejected(); }
                    String nonce = request.getFileName().toString().replace(".json", "");
                    if (Files.exists(inputs.directory().resolve("started/" + nonce + ".json"), LinkOption.NOFOLLOW_LINKS)) { continue; }
                    execute(request);
                }
            } catch (IOException | RuntimeException failure) { log.warn("Task runtime probe rejected an input; role={}", inputs.role()); }
        }

        void stableInputs() throws IOException {
            if (!inputs.manifestSha256().equals(sha256(readPrivate(inputs.manifestFile(), inputs.root(), 128 * 1024)))
                    || !inputs.registrySha256().equals(sha256(readPrivate(inputs.registryFile(), inputs.root(), 2 * 1024 * 1024)))
                    || !inputs.fixtureSha256().equals(sha256(readPrivate(inputs.fixture(), inputs.root(), 8 * 1024 * 1024)))
                    || !inputs.artifactSha256().equals(hashFile(inputs.artifact()))) { throw rejected(); }
        }

        ProbeCommandDTO claim(Path request) throws IOException {
            stableInputs();
            var command = readCommand(inputs, request, context.getBean("defaultValidator", Validator.class), Instant.now());
            writeNew(inputs.directory().resolve("started/" + command.nonce() + ".json"), Map.of("schemaVersion", 1, "runId", inputs.runId(),
                    "manifestSha256", inputs.manifestSha256(), "role", inputs.role(), "nonce", command.nonce(), "operation", command.operation(),
                    "commandSha256", sha256(readPrivate(request, inputs.root(), 16 * 1024)), "status", "STARTED", "recordedAt", Instant.now()));
            return command;
        }

        void execute(Path request) throws IOException {
            var command = claim(request); var ownerEntered = new AtomicBoolean();
            try {
                if ("GRAPHQL_CONTEXT_COUNTS".equals(command.operation())) {
                    var graphQl = context.getBean(GRAPHQL_OBSERVATION, GraphQlAsyncObservationComponent.class);
                    graphQl.validateInstalled(context.getBean("nativeGraphQlSecurityInstrumentation"));
                    var counts = graphQl.snapshot(); stableInputs();
                    if (!command.expiresAt().isAfter(Instant.now())) { throw rejected(); }
                    complete(command, "COMPLETED", Map.of("counts", counts)); return;
                }
                if (Set.of("BUSINESS_COUNTS", "KNOWLEDGE_COUNTS").contains(command.operation())) {
                    var selected = "KNOWLEDGE_COUNTS".equals(command.operation()) ? context.getBean(KNOWLEDGE_OBSERVATION, InvocationObservationComponent.class) : observation;
                    if (selected == null) { throw rejected(); }
                    var counts = selected.snapshot(context.getBean(selected.beanName));
                    stableInputs();
                    if (!command.expiresAt().isAfter(Instant.now())) { throw rejected(); }
                    complete(command, "COMPLETED", Map.of("counts", counts));
                    return;
                }
                if ("yuheng".equals(inputs.role()) && Set.of("SECURITY_METRICS", "SHARED_STATE_PAUSE", "SHARED_STATE_RESUME").contains(command.operation())) {
                    if (!command.expiresAt().isAfter(Instant.now())) { throw rejected(); }
                    Object result;
                    if ("SECURITY_METRICS".equals(command.operation())) {
                        var supplier = context.getBean("idpMeterRegistrySupplier", java.util.function.Supplier.class);
                        if (!(supplier.get() instanceof io.micrometer.core.instrument.MeterRegistry registry)) { throw rejected(); }
                        result = Map.of("samples", securityMetrics(registry, context.getBean("defaultValidator", Validator.class)));
                    } else if ("SHARED_STATE_PAUSE".equals(command.operation())) {
                        if (sharedStatePause != null && !sharedStatePause.expired()) { throw rejected(); }
                        if (sharedStatePause != null) { sharedStatePause.close(); }
                        sharedStatePause = SharedStatePauseLease.start(ownedSharedStateGroup(context, inputs), Duration.ofSeconds(30));
                        result = Map.of("state", "PAUSED", "eventLoopCount", sharedStatePause.eventLoopCount, "resumesAt", sharedStatePause.resumesAt);
                    } else {
                        int count = sharedStatePause == null ? 0 : sharedStatePause.eventLoopCount;
                        if (sharedStatePause != null) { sharedStatePause.close(); sharedStatePause = null; }
                        result = Map.of("state", "RUNNING", "eventLoopCount", count);
                    }
                    stableInputs();
                    if (!command.expiresAt().isAfter(Instant.now())) {
                        if (sharedStatePause != null) { sharedStatePause.close(); sharedStatePause = null; }
                        throw rejected();
                    }
                    complete(command, "COMPLETED", result); return;
                }
                var verified = "native-service".equals(inputs.role()) ? verifiedService(command) : verifiedUser(command);
                var result = IdpRpcSecurityContext.with(verified.getVerification().principal(), verified.getToken(), verified.getVerification().proof(), null,
                        verified.getVerification().scopes()).call(() -> {
                    var outcome = new AtomicReference<Object>();
                    IdpRpcSecurityContext.runWithSpringSecurity(verified.getVerification().principal(), verified.getVerification().scopes(), verified.getVerification().proof(), () -> {
                        try { outcome.set(executeOwner(command, verified, ownerEntered)); }
                        catch (Exception failure) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED", failure); }
                    });
                    return outcome.get();
                });
                stableInputs();
                if (!command.expiresAt().isAfter(Instant.now())) { throw rejected(); }
                complete(command, "COMPLETED", result);
            } catch (Exception failure) {
                if (failure instanceof InterruptedException) { Thread.currentThread().interrupt(); }
                String status = ownerEntered.get() ? "UNKNOWN_COMMIT" : "FAILED";
                complete(command, status, Map.of("code", ownerEntered.get() ? "XINGYUAN_COMMIT_UNKNOWN" : "XINGYUAN_VERIFICATION_FAILED"));
            }
        }

        private Object executeOwner(ProbeCommandDTO command, VerifiedActorBO verified, AtomicBoolean ownerEntered) throws Exception {
            if ("PLATFORM_SNAPSHOT".equals(command.operation())) {
                return WebPlatformSnapshotOwner.fetch(context, inputs, verified);
            }
            if ("SERVICE_AUTHORIZATION".equals(command.operation())) {
                var principal = (top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal) verified.getVerification().principal();
                return Map.of("tokenReference", sha256(verified.getToken().getBytes(StandardCharsets.US_ASCII)),
                        "clientReference", sha256(principal.clientId().getBytes(StandardCharsets.UTF_8)),
                        "issuer", mapper().readTree(readPrivate(inputs.registryFile(), inputs.root(), 2 * 1024 * 1024))
                                .path("realms").path(command.tenantCode()).path("issuer").asText(), "expiresAt", principal.expiresAt());
            }
            Object returned;
            String resultType;
            if ("COURSE_CREATE".equals(command.operation())) {
                var owner = fixedBean("courseManage", ".application.course.manage.impl.CourseManageImpl");
                Class<?> commandType = generatedClass(".application.course.pojo.command.CreateCourseCommand");
                String marker = "IT_" + inputs.runId().replace("-", "").substring(0, 12) + "_" + command.nonce().replace("-", "").substring(0, 12);
                Object creation = commandType.getConstructor(String.class, String.class, int.class).newInstance(marker, "Xingyuan integration " + marker.substring(3), 1);
                new ValidationUtils(context.getBean("defaultValidator", Validator.class)).validate(creation);
                var method = generatedClass(".application.course.manage.CourseManage").getMethod("create", commandType);
                ownerEntered.set(true);
                returned = method.invoke(owner, creation);
                resultType = ".application.course.pojo.result.CourseResult";
            } else if ("WEB_COURSE_READ".equals(command.operation())) {
                var owner = fixedBean("evaluationQueryService", ".infrastructure.teaching.service.impl.EvaluationQueryServiceImpl");
                returned = generatedClass(".domain.teaching.service.EvaluationQueryService").getMethod("getCourse", Long.class).invoke(owner, Long.valueOf(command.entityId()));
                resultType = ".domain.teaching.vos.EvaluationCourseBO";
            } else if ("SERVICE_USER_READ".equals(command.operation())) {
                var owner = fixedBean("organizationDirectoryService", ".infrastructure.course.service.impl.OrganizationDirectoryServiceImpl");
                returned = generatedClass(".domain.course.service.OrganizationDirectoryService").getMethod("getUser", Long.class).invoke(owner, Long.valueOf(command.entityId()));
                resultType = ".domain.course.vos.OrganizationUserBO";
            } else { throw rejected(); }
            if (returned == null || returned.getClass() != generatedClass(resultType)) { throw rejected(); }
            Map<String, Object> data = new LinkedHashMap<>();
            for (String field : command.operation().equals("SERVICE_USER_READ") ? List.of("id", "name", "status") : List.of("id", "code", "name", "credit", "status")) {
                Object value = returned.getClass().getMethod(field).invoke(returned);
                if (value == null) { throw rejected(); }
                data.put(field, field.equals("id") ? value.toString() : value);
            }
            String id = data.get("id").toString();
            if (!id.matches("[1-9][0-9]{0,18}") || Long.parseLong(id) <= 0 || command.entityId() != null && !command.entityId().equals(id)) { throw rejected(); }
            return Map.of("entityId", id, "dataSha256", sha256(mapper().writeValueAsBytes(data)));
        }

        private Class<?> generatedClass(String suffix) throws ClassNotFoundException {
            return Class.forName(inputs.namespace() + suffix, false, context.getClassLoader());
        }

        private Object fixedBean(String name, String implementationSuffix) throws ClassNotFoundException {
            Object bean = context.getBean(name);
            if (AopUtils.getTargetClass(bean) != generatedClass(implementationSuffix)) { throw rejected(); }
            return bean;
        }

        private VerifiedActorBO verifiedService(ProbeCommandDTO command) throws Exception {
            var manifest = mapper().readTree(readPrivate(inputs.manifestFile(), inputs.root(), 128 * 1024));
            var registry = mapper().readTree(readPrivate(inputs.registryFile(), inputs.root(), 2 * 1024 * 1024));
            String tenant = text(registry.path("tenantIds"), command.tenantCode());
            String web = nativeApplicationCode(inputs.root(), manifest, "native-web");
            var properties = context.getBean("idpStarterProperties", top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class);
            var bindings = properties.getRpc().getConsumer().getBindings().values().stream()
                    .filter(value -> value.getFullMethodName() != null && value.getFullMethodName().endsWith("/GetUser")).toList();
            if (bindings.size() != 1 || properties.getServiceClient().getAppId() == null) { throw rejected(); }
            var binding = bindings.getFirst();
            new ValidationUtils(context.getBean("defaultValidator", Validator.class)).validate(binding);
            if (binding.getMode() != top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcConsumerModeEnum.SERVICE
                    || binding.getScopeContext() != top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.TENANT
                    || !properties.getServiceClient().getAppId().equals(binding.getAppId()) || !Set.of(web + ":user:read").equals(binding.getScopes())
                    || !Objects.equals(context.getEnvironment().getProperty("spring.application.name"), binding.getProcessIdentity().get("applicationName"))) { throw rejected(); }
            var resources = java.util.stream.StreamSupport.stream(registry.path("resources").spliterator(), false)
                    .filter(value -> web.equals(value.path("rbacApplicationCode").asText()) && "ACTIVE".equals(value.path("status").asText())
                            && binding.getResourceUri().toASCIIString().equals(value.path("resourceUri").asText())).toList();
            String registration = binding.getTenantRegistrationIds().get(tenant);
            var clients = java.util.stream.StreamSupport.stream(registry.path("clients").spliterator(), false)
                    .filter(value -> Objects.equals(registration, value.path("registrationId").asText())
                            && command.tenantCode().equals(value.path("realmCode").asText())).toList();
            if (resources.size() != 1 || clients.size() != 1 || !"CONFIDENTIAL".equals(clients.getFirst().path("clientType").asText())
                    || !"SERVICE".equals(clients.getFirst().path("purpose").asText()) || !"ACTIVE".equals(clients.getFirst().path("status").asText())
                    || !properties.getServiceClient().getAppId().equals(clients.getFirst().path("appId").asText())) { throw rejected(); }
            var token = context.getBean("idpServiceOAuth2Client", top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client.class).authorize(
                    new top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest(registration, binding.getAppId(), binding.getResourceUri(),
                            top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.TENANT, tenant, binding.getScopes()));
            var verifier = IdpJwtVerifier.createConfigured(null,
                    context.getBean("identityResourceServerStateReader", top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader.class),
                    context.getBean("identityOAuthClientStateReader", top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader.class),
                    text(resources.getFirst(), "resourceServerId"), binding.getResourceUri(), properties.getPlatformAudience(), context.getBean("idpClock", java.time.Clock.class),
                    context.getBean("tenantIssuerAuthenticationManagerComponent", top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent.class),
                    context.getBean("oauthRevocationReader", top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader.class),
                    context.getBean("verifiedPrincipalConverterImpl", top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter.class), null);
            var observed = new AtomicReference<VerifiedJwtBO>();
            var verification = verifier.verifyAny(token.getTokenValue(), observed::set);
            if (!(verification instanceof AccessTokenVerification.Valid<?> valid)
                    || !(valid.principal() instanceof top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal principal) || observed.get() == null
                    || !tenant.equals(principal.tenantId()) || !binding.getAppId().equals(principal.appId()) || !binding.getResourceUri().equals(principal.resourceUri())
                    || !binding.getScopes().equals(principal.scopes()) || !text(clients.getFirst(), "clientId").equals(principal.clientId())
                    || !text(clients.getFirst(), "clientPk").equals(observed.get().getJwt().getClaimAsString("client_registration_id"))
                    || !text(clients.getFirst(), "issuer").equals(observed.get().getJwt().getIssuer().toString()) || !principal.expiresAt().isAfter(Instant.now())) { throw rejected(); }
            return new VerifiedActorBO(token.getTokenValue(), new AccessTokenVerification.Valid<>(principal, valid.scopes(), valid.proof()));
        }

        private VerifiedActorBO verifiedUser(ProbeCommandDTO command) throws Exception {
            var registry = mapper().readTree(readPrivate(inputs.registryFile(), inputs.root(), 2 * 1024 * 1024));
            var index = mapper().readTree(readPrivate(inputs.root().resolve("secrets/token-index.json"), inputs.root(), 2 * 1024 * 1024));
            if (!inputs.runId().equals(text(index, "runId")) || !inputs.manifestSha256().equals(text(index, "manifestSha256"))) { throw rejected(); }
            var resources = java.util.stream.StreamSupport.stream(registry.path("resources").spliterator(), false)
                    .filter(value -> inputs.applicationCode().equals(value.path("rbacApplicationCode").asText()) && "ACTIVE".equals(value.path("status").asText())).toList();
            if (resources.size() != 1) { throw rejected(); }
            String audience = text(resources.getFirst(), "resourceUri");
            var tokens = java.util.stream.StreamSupport.stream(index.path("tokens").spliterator(), false)
                    .filter(value -> command.tenantCode().equals(value.path("tenantCode").asText()) && "admin".equals(value.path("userKey").asText())
                            && audience.equals(value.path("resourceUri").asText())).toList();
            if (tokens.size() != 1) { throw rejected(); }
            var tokenFact = tokens.getFirst();
            var registered = java.util.stream.StreamSupport.stream(registry.path("clients").spliterator(), false)
                    .filter(value -> text(tokenFact, "clientId").equals(value.path("clientId").asText())
                            && text(tokenFact, "clientPk").equals(value.path("clientPk").asText())
                            && command.tenantCode().equals(value.path("realmCode").asText())).toList();
            if (registered.size() != 1 || !"PUBLIC".equals(registered.getFirst().path("clientType").asText())
                    || !"USER".equals(registered.getFirst().path("purpose").asText()) || !"ACTIVE".equals(registered.getFirst().path("status").asText())) { throw rejected(); }
            byte[] material = readPrivate(Path.of(text(tokenFact, "tokenFile")), inputs.root().resolve("secrets"), 16 * 1024);
            String token;
            try {
                if (!text(tokenFact, "tokenSha256").equals(sha256(material))) { throw rejected(); }
                token = new String(material, StandardCharsets.US_ASCII);
                if (!token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")) { throw rejected(); }
            } finally { Arrays.fill(material, (byte) 0); }
            var observed = new AtomicReference<VerifiedJwtBO>();
            var verification = context.getBean("idpJwtVerifier", IdpJwtVerifier.class).verifyAny(token, observed::set);
            if (!(verification instanceof AccessTokenVerification.Valid<?> valid) || !(valid.principal() instanceof IdentityPrincipal identity) || observed.get() == null) { throw rejected(); }
            String tenant = text(registry.path("tenantIds"), command.tenantCode());
            var jwt = observed.get().getJwt();
            if (!tenant.equals(identity.tenantId()) || !tenant.equals(text(tokenFact, "tenantId"))
                    || !text(registry.path("subjects"), "admin").equals(identity.subject()) || !Set.of(audience).equals(identity.audience())
                    || !text(tokenFact, "issuer").equals(jwt.getIssuer().toString()) || !text(tokenFact, "clientId").equals(jwt.getClaimAsString("client_id"))
                    || !text(tokenFact, "clientPk").equals(jwt.getClaimAsString("client_registration_id")) || !identity.expiresAt().isAfter(Instant.now())) { throw rejected(); }
            return new VerifiedActorBO(token, new AccessTokenVerification.Valid<>(identity, valid.scopes(), valid.proof()));
        }

        Mono<Void> observeEdge(ServerWebExchange exchange, String nonce) {
            return Mono.fromCallable(() -> claim(inputs.directory().resolve("requests/" + nonce + ".json"))).subscribeOn(Schedulers.boundedElastic())
                    .flatMap(command -> exchange.getPrincipal().ofType(Authentication.class)
                            .filter(authentication -> authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken))
                            .switchIfEmpty(Mono.error(rejected())).flatMap(authentication -> loadEdgeBinding(exchange))
                            .flatMap(binding -> Mono.fromRunnable(() -> {
                                try {
                                    stableInputs();
                                    if (!command.expiresAt().isAfter(Instant.now())) { throw rejected(); }
                                    var proof = edgeBindingProof(binding, command, mapper().readTree(readPrivate(inputs.registryFile(), inputs.root(), 2 * 1024 * 1024)));
                                    complete(command, "COMPLETED", proof);
                                } catch (Exception failure) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED", failure); }
                            }).subscribeOn(Schedulers.boundedElastic()))
                            .onErrorResume(failure -> Mono.fromRunnable(() -> {
                                try { complete(command, "FAILED", Map.of("code", "XINGYUAN_VERIFICATION_FAILED")); }
                                catch (IOException rejected) { log.warn("Task Edge observation could not publish its result"); }
                            }).subscribeOn(Schedulers.boundedElastic())).then())
                    .onErrorResume(failure -> { log.warn("Task Edge observation rejected a command"); return Mono.empty(); });
        }

        private Mono<?> loadEdgeBinding(ServerWebExchange exchange) {
            try {
                Object owner = context.getBean("edgeSessionBindingComponent");
                Class<?> type = Class.forName(EDGE_OWNER, false, context.getClassLoader());
                if (AopUtils.getTargetClass(owner) != type) { throw rejected(); }
                Object result = type.getMethod("load", ServerWebExchange.class).invoke(owner, exchange);
                if (!(result instanceof Mono<?> mono)) { throw rejected(); }
                return mono;
            } catch (ReflectiveOperationException failure) { return Mono.error(rejected()); }
        }

        private EdgeBindingProofVO edgeBindingProof(Object binding, ProbeCommandDTO command, JsonNode registry) throws Exception {
            if (!binding.getClass().getName().equals(EDGE_BINDING) || !Boolean.TRUE.equals(binding.getClass().getMethod("isStateValid").invoke(binding))
                    || !"ACTIVE".equals(binding.getClass().getMethod("getState").invoke(binding).toString())) { throw rejected(); }
            String tenant = text(registry.path("tenantIds"), command.tenantCode());
            URI issuer = (URI) binding.getClass().getMethod("getIssuer").invoke(binding);
            if (!tenant.equals(binding.getClass().getMethod("getTenantId").invoke(binding))
                    || !text(registry.path("subjects"), "admin").equals(binding.getClass().getMethod("getSubject").invoke(binding))
                    || !text(registry.path("realms").path(command.tenantCode()), "issuer").equals(issuer.toString())) { throw rejected(); }
            var facts = new EdgeBindingFactsBO((String) binding.getClass().getMethod("getBindingId").invoke(binding), issuer, tenant,
                    (Long) binding.getClass().getMethod("getGeneration").invoke(binding), (Instant) binding.getClass().getMethod("getOriginalCreatedAt").invoke(binding),
                    (Instant) binding.getClass().getMethod("getAbsoluteExpiresAt").invoke(binding));
            if (facts.getGeneration() <= 0 || !facts.getBindingId().matches(UUID_PATTERN) || !facts.getAbsoluteExpiresAt().isAfter(Instant.now())
                    || !facts.getAbsoluteExpiresAt().isAfter(facts.getOriginalCreatedAt())) { throw rejected(); }
            return new XingyuanRuntimeProbeEdgeBindingConverterImpl().toTarget(facts);
        }

        void complete(ProbeCommandDTO command, String status, Object result) throws IOException {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("schemaVersion", 1); envelope.put("runId", inputs.runId()); envelope.put("manifestSha256", inputs.manifestSha256()); envelope.put("role", inputs.role());
            envelope.put("fixtureSha256", inputs.fixtureSha256()); envelope.put("artifactSha256", inputs.artifactSha256()); envelope.put("nonce", command.nonce());
            envelope.put("operation", command.operation()); envelope.put("status", status); envelope.put("completedAt", Instant.now()); envelope.put("result", result);
            writeNew(inputs.directory().resolve("results/" + command.nonce() + ".json"), envelope);
        }

        @Override public void close() {
            var pause = sharedStatePause; if (pause != null) { pause.close(); }
            if (executor != null) { executor.shutdownNow(); }
        }
    }

    /** Only Web has the existing USER/PDP integration; Service keeps its original RPC/SERVICE boundary. */
    @Slf4j
    private static final class WebPlatformSnapshotOwner {
        private static Object fetch(ConfigurableApplicationContext context, ProbeInputsVO inputs, VerifiedActorBO verified) throws InterruptedException {
            var client = context.getBean("tenantAwareRbac3AuthorizationClient", Rbac3AuthorizationClient.class);
            if (AopUtils.getTargetClass(client) != HttpRbac3AuthorizationClient.class
                    || !(verified.getVerification().principal() instanceof IdentityPrincipal identity)) { throw rejected(); }
            var properties = context.getBean(Rbac3StarterProperties.class);
            if (!inputs.applicationCode().equals(properties.getSystemCode())
                    || !"https://127.0.0.1:28130".equals(properties.getAuthorization().getEndpoint())) { throw rejected(); }
            var snapshot = client.fetch(inputs.applicationCode(), identity, verified.getToken());
            if (!identity.tenantId().equals(snapshot.tenantId()) || !identity.subject().equals(snapshot.identitySub())
                    || !inputs.applicationCode().equals(snapshot.systemCode()) || !snapshot.expiresAt().isAfter(Instant.now())) { throw rejected(); }
            return Map.of("snapshotChecksum", snapshot.checksum(), "authVersion", snapshot.authVersion(), "policyVersion", snapshot.policyVersion());
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    private static class VerifiedActorBO {
        @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude private String token;
        @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude private AccessTokenVerification.Valid<top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal> verification;
    }

    @RequiredArgsConstructor
    static final class EdgeObservationWebFilter implements WebFilter, Ordered {
        private final RuntimeProbeComponent runtime;
        @Override public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
        @Override public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            List<String> values = exchange.getRequest().getHeaders().get(EDGE_NONCE_HEADER);
            if (exchange.getRequest().getMethod() != HttpMethod.GET || !"/bff/session".equals(exchange.getRequest().getPath().value())
                    || exchange.getRequest().getURI().getRawQuery() != null || values == null || values.size() != 1 || !values.getFirst().matches(UUID_PATTERN)) { return chain.filter(exchange); }
            return chain.filter(exchange).then(Mono.defer(() -> exchange.getResponse().getStatusCode() != null && exchange.getResponse().getStatusCode().is2xxSuccessful()
                    ? runtime.observeEdge(exchange, values.getFirst()) : Mono.empty()));
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
    public static class EdgeBindingFactsBO {
        @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude private String bindingId;
        private URI issuer;
        private String tenantId;
        private long generation;
        private Instant originalCreatedAt;
        private Instant absoluteExpiresAt;
    }

    public record EdgeBindingProofVO(String bindingReference, String issuer, String tenantId, long generation,
            Instant originalCreatedAt, Instant absoluteExpiresAt) { }

    @Mapper(implementationName = "XingyuanRuntimeProbeEdgeBindingConverterImpl", unmappedTargetPolicy = ReportingPolicy.ERROR)
    public interface EdgeBindingConverter extends BaseForwardConverter<EdgeBindingFactsBO, EdgeBindingProofVO> {
        @Override
        @Mapping(target = "bindingReference", source = "bindingId", qualifiedByName = "bindingHash")
        @Mapping(target = "issuer", source = "issuer", qualifiedByName = "issuerText")
        EdgeBindingProofVO toTarget(EdgeBindingFactsBO source);
        @Named("bindingHash") default String bindingHash(String value) { return sha256(value.getBytes(StandardCharsets.UTF_8)); }
        @Named("issuerText") default String issuerText(URI value) { return value.toASCIIString(); }
    }

    static ObjectMapper mapper() {
        return JsonMapper.builder().addModule(new JavaTimeModule()).enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
    }

    static byte[] readPrivate(Path file, Path root, int maximum) throws IOException {
        requireAbsolute(file); requireDirectory(root);
        if (!file.startsWith(root) || file.equals(root)) { throw rejected(); }
        for (Path directory = file.getParent(); directory != null && directory.startsWith(root); directory = directory.getParent()) { requireDirectory(directory); }
        var before = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.size() < 1 || before.size() > maximum || !Files.getOwner(root).equals(Files.getOwner(file, LinkOption.NOFOLLOW_LINKS))
                || !Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS).equals(PosixFilePermissions.fromString("rw-------"))
                || ((Number) Files.getAttribute(file, "unix:nlink", LinkOption.NOFOLLOW_LINKS)).longValue() != 1) { throw rejected(); }
        byte[] bytes;
        try (var channel = FileChannel.open(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            var content = ByteBuffer.allocate((int) before.size());
            while (content.hasRemaining() && channel.read(content) >= 0) { }
            if (content.hasRemaining() || channel.read(ByteBuffer.allocate(1)) != -1) { throw rejected(); }
            bytes = content.array();
        }
        var after = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!Objects.equals(before.fileKey(), after.fileKey()) || before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime())) { throw rejected(); }
        return bytes;
    }

    static void writeNew(Path file, Object value) throws IOException {
        requireDirectory(file.getParent());
        byte[] bytes = mapper().writeValueAsBytes(value);
        Path temporary = Files.createTempFile(file.getParent(), ".runtime-probe-", ".tmp", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            try (var output = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer content = ByteBuffer.wrap(bytes); while (content.hasRemaining()) { output.write(content); } output.force(true);
            }
            Files.createLink(file, temporary);
            Files.delete(temporary);
            try (var directory = FileChannel.open(file.getParent(), StandardOpenOption.READ)) { directory.force(true); }
        } finally { Files.deleteIfExists(temporary); }
    }

    static void requireDirectory(Path directory) throws IOException {
        requireAbsolute(directory);
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || !directory.toRealPath().equals(directory)
                || !Files.getPosixFilePermissions(directory).equals(PosixFilePermissions.fromString("rwx------"))) { throw rejected(); }
    }

    private static void privateDirectory(Path directory) throws IOException {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) { Files.createDirectory(directory, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))); }
        requireDirectory(directory);
    }

    private static void requireAbsolute(Path path) {
        if (!path.isAbsolute() || !path.normalize().equals(path)) { throw rejected(); }
    }

    static String hashFile(Path file) throws IOException {
        requireAbsolute(file);
        if (Files.isSymbolicLink(file) || !Files.isRegularFile(file) || !file.toRealPath().equals(file)) { throw rejected(); }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var stream = new java.security.DigestInputStream(Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS), digest)) { stream.transferTo(java.io.OutputStream.nullOutputStream()); }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException unavailable) { throw new IllegalStateException(unavailable); }
    }

    static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException unavailable) { throw new IllegalStateException(unavailable); }
    }

    private static String text(JsonNode node, String field) {
        if (!node.path(field).isTextual() || node.path(field).asText().isBlank()) { throw rejected(); }
        return node.path(field).asText();
    }

    static IllegalArgumentException rejected() { return new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
}
