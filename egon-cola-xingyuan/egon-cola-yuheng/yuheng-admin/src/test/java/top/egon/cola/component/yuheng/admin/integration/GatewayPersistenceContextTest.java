package top.egon.cola.component.yuheng.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.yuheng.admin.config.GatewayPersistenceConfiguration;
import top.egon.cola.component.yuheng.admin.config.GatewayPersistenceContextComponent;
import top.egon.cola.component.yuheng.admin.config.GatewayPersistenceContextFilter;
import top.egon.cola.component.yuheng.admin.config.GatewayPersistenceProperties;

/**
 * 中文说明：{@code GatewayPersistenceContextTest} 固定 Admin 持久化身份上下线的运行期合同，口径全部来自功能 Spec 的
 * 「单企业安装域 / 声明只做门禁 / finally 原样恢复」而不是实现细节：（一）真正执行 SQL 的线程上看到的租户只能是部署
 * {@code yuheng.persistence.tenant-id}，审计主体只能是 {@code service-user-id}；（二）身份声明与部署受信任身份不一致
 * 或不可解析时一律失败关闭，受守卫的工作根本不运行；（三）声明只能门禁、绝不能选库；（四）MDC 先前值必须原样恢复，
 * 没有先前值就删除键，异常路径同样恢复且异常不被改写；（五）过滤器在认证链之后包住整条下游链，请求头/参数/请求体里的
 * 租户值永远不是事实来源；（六）部署身份没有任何默认值（尤其不是 {@code tenantId=1}），缺项在绑定阶段即违规；
 * （七）本进程只有这一套 MDC 写入机制。测试不启动 Spring 上下文、不连数据库、也不开容器；时刻只用固定的
 * {@link Instant} 夹具。
 * English summary: {@code GatewayPersistenceContextTest} pins the runtime contract of Admin's persistence identity
 * context from the functional Spec — a single-enterprise installation domain, claims that only gate entry, exact
 * restoration in a finally — rather than from implementation details: the SQL-executing thread can only ever observe the
 * deployment {@code yuheng.persistence.tenant-id} plus the {@code service-user-id} audit principal; a claim that
 * disagrees with the trusted identity or cannot be parsed fails closed before the guarded work runs; a claim gates but
 * never selects the persisted tenant; previous MDC values are restored exactly and keys with no prior value are removed,
 * including on the failure path where the exception propagates unchanged; the filter wraps the whole downstream chain
 * after the authentication chain and never treats a header, parameter or body tenant value as a source of truth; the
 * deployment identity carries no default (in particular not {@code tenantId=1}) and violates binding when anything is
 * missing; and this process owns exactly one MDC-writing mechanism. No Spring context, database or container is started,
 * and time only ever appears as a fixed {@link Instant} fixture.
 *
 * 用法 / Usage: 纯 JVM 测试，一律走公开入口 {@code context.call(claimedTenantId, work)} 与
 * {@code filter.doFilter(request, response, chain)}，不伪造成功。/ A plain JVM test that exercises the public entries
 * only and never substitutes a fake success.
 */
class GatewayPersistenceContextTest {

    /** 中文说明：部署安装域租户，唯一可能被写入 MDC 的租户值。 English summary: the deployment installation tenant, the only tenant value that may be installed. */
    private static final Long INSTALLATION_TENANT_ID = 7001L;

    /** 中文说明：被信任的身份租户，刻意与安装域不同，用来证明「声明只门禁、不选库」。 English summary: the trusted identity tenant, deliberately different from the installation tenant so that claims can only gate. */
    private static final Long TRUSTED_IDENTITY_TENANT_ID = 9001L;

    /** 中文说明：服务侧审计主体。 English summary: the service-side audit principal. */
    private static final String SERVICE_USER_ID = "yuheng-admin-service";

    /** 中文说明：与本部署无关的租户，出现在线程上即代表越权。 English summary: an unrelated tenant id; its presence on a thread would mean a tenancy breach. */
    private static final String FOREIGN_TENANT_ID = "9002";

    private static final Instant TOKEN_ISSUED_AT = Instant.parse("2026-09-22T08:30:00Z");

    private static final Pattern MDC_WRITE = Pattern.compile(
            "\\bMDC\\s*\\.\\s*(put|remove|clear|setContextMap|putCloseable)\\s*\\(");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");

    /** 中文说明：Admin 主源码根，用于「唯一 MDC 机制」源码门禁。 English summary: the Admin main source root used by the single-MDC-mechanism gate. */
    private static final Path ADMIN_MAIN = Path.of("src/main/java/top/egon/cola/component/yuheng/admin");

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private GatewayPersistenceProperties deploymentProperties;
    private EgonColaMybatisPlusProperties componentProperties;
    private GatewayPersistenceContextComponent context;

    @BeforeEach
    void bindDeploymentIdentity() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        deploymentProperties = new GatewayPersistenceProperties()
                .setTenantId(INSTALLATION_TENANT_ID)
                .setIdentityTenantId(TRUSTED_IDENTITY_TENANT_ID)
                .setServiceUserId(SERVICE_USER_ID)
                .setExpectedSchemaVersion("20260922_001")
                .setExpectedSchemaSha256("a".repeat(64))
                .setManagedDdlEnabled(true);
        componentProperties = new EgonColaMybatisPlusProperties();
        context = new GatewayPersistenceContextComponent(deploymentProperties, componentProperties);
    }

    @AfterEach
    void releaseThreadLocals() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("受守卫的工作在部署租户与服务审计主体下执行")
    void workRunsInsideTheDeploymentIdentity() throws Exception {
        Map<String, String> observed = new LinkedHashMap<>();
        String result = context.call(null, () -> {
            observed.put("tenant", MDC.get(tenantKey()));
            observed.put("user", MDC.get(userKey()));
            return "published";
        });

        assertThat(result).isEqualTo("published");
        assertThat(observed).containsEntry("tenant", String.valueOf(INSTALLATION_TENANT_ID))
                .containsEntry("user", SERVICE_USER_ID);
        assertThat(context.installationTenantId()).isEqualTo(INSTALLATION_TENANT_ID);
    }

    @Test
    @DisplayName("身份声明只门禁，被写入的租户永远是安装域而不是声明值")
    void aClaimGatesEntryButNeverSelectsThePersistedTenant() throws Exception {
        Map<String, String> observed = new LinkedHashMap<>();
        context.call(String.valueOf(TRUSTED_IDENTITY_TENANT_ID), () -> {
            observed.put("tenant", MDC.get(tenantKey()));
            return null;
        });

        assertThat(observed).containsEntry("tenant", String.valueOf(INSTALLATION_TENANT_ID));
        assertThat(observed).doesNotContainValue(String.valueOf(TRUSTED_IDENTITY_TENANT_ID));
    }

    @Test
    @DisplayName("与受信任身份不一致的声明先失败关闭，工作一行都不执行")
    void aMismatchedClaimIsRefusedBeforeTheWorkRuns() {
        AtomicBoolean ran = new AtomicBoolean();

        assertThatThrownBy(() -> context.call(FOREIGN_TENANT_ID, recording(ran)))
                .isInstanceOfSatisfying(EgonColaMybatisPlusConfigurationException.class,
                        failure -> assertThat(failure.getStatus())
                                .isEqualTo("PERSISTENCE_IDENTITY_TENANT_MISMATCH"));

        assertThat(ran).isFalse();
        assertThat(MDC.get(tenantKey())).isNull();
        assertThat(MDC.get(userKey())).isNull();
    }

    @Test
    @DisplayName("不可解析的声明按不一致处理，绝不猜一个租户")
    void anUnparseableClaimIsRefusedInsteadOfGuessed() {
        for (String claim : List.of("not-a-tenant", "9001x", "9001.5", "-", "0x9001")) {
            AtomicBoolean ran = new AtomicBoolean();
            Throwable failure = catchThrowable(() -> context.call(claim, recording(ran)));

            assertThat(failure).isInstanceOfSatisfying(EgonColaMybatisPlusConfigurationException.class,
                    rejected -> assertThat(rejected.getStatus())
                            .isEqualTo("PERSISTENCE_IDENTITY_TENANT_MISMATCH"));
            assertThat(ran).as("claim '%s' must never reach the guarded work", claim).isFalse();
            assertThat(MDC.get(tenantKey())).isNull();
        }
    }

    @Test
    @DisplayName("不带租户声明的身份按单企业安装域放行")
    void anIdentityCarryingNoTenantClaimIsServedByTheInstallation() throws Exception {
        for (String claim : Arrays.asList(null, "", "  ", "\t")) {
            Map<String, String> observed = new LinkedHashMap<>();
            context.call(claim, () -> {
                observed.put("tenant", MDC.get(tenantKey()));
                return null;
            });

            assertThat(observed).containsEntry("tenant", String.valueOf(INSTALLATION_TENANT_ID));
        }
    }

    @Test
    @DisplayName("写入的 MDC 键取自组件配置而不是硬编码")
    void theInstalledKeysComeFromTheComponentConfiguration() throws Exception {
        componentProperties.getTenantId().setMdcKey("egonTenant");
        componentProperties.getAudit().setUserIdMdcKey("egonPrincipal");
        Map<String, String> observed = new LinkedHashMap<>();

        context.call(null, () -> {
            observed.put("customTenant", MDC.get("egonTenant"));
            observed.put("customUser", MDC.get("egonPrincipal"));
            observed.put("defaultTenant", MDC.get("tenantId"));
            observed.put("defaultUser", MDC.get("userId"));
            return null;
        });

        assertThat(observed).containsEntry("customTenant", String.valueOf(INSTALLATION_TENANT_ID))
                .containsEntry("customUser", SERVICE_USER_ID);
        assertThat(observed.get("defaultTenant")).isNull();
        assertThat(observed.get("defaultUser")).isNull();
    }

    @Test
    @DisplayName("嵌套上下文逐层原样恢复先前身份")
    void nestedContextsRestoreThePreviousIdentityExactly() throws Exception {
        MDC.put(tenantKey(), "outer-tenant");
        MDC.put(userKey(), "outer-user");

        Map<String, String> seen = new LinkedHashMap<>();
        context.call(null, () -> {
            seen.put("outerInstalledTenant", MDC.get(tenantKey()));
            seen.put("outerInstalledUser", MDC.get(userKey()));
            context.call(String.valueOf(TRUSTED_IDENTITY_TENANT_ID), () -> {
                seen.put("innerInstalledTenant", MDC.get(tenantKey()));
                return null;
            });
            seen.put("afterInnerTenant", MDC.get(tenantKey()));
            return null;
        });

        assertThat(seen).containsEntry("outerInstalledTenant", String.valueOf(INSTALLATION_TENANT_ID))
                .containsEntry("outerInstalledUser", SERVICE_USER_ID)
                .containsEntry("innerInstalledTenant", String.valueOf(INSTALLATION_TENANT_ID))
                .containsEntry("afterInnerTenant", String.valueOf(INSTALLATION_TENANT_ID));
        assertThat(MDC.get(tenantKey())).isEqualTo("outer-tenant");
        assertThat(MDC.get(userKey())).isEqualTo("outer-user");
    }

    @Test
    @DisplayName("没有先前值的键在退出时被删除，线程状态不会泄漏给下一个请求")
    void keysWithoutAPriorValueAreRemovedOnExit() throws Exception {
        context.call(null, () -> null);

        assertThat(MDC.get(tenantKey())).isNull();
        assertThat(MDC.get(userKey())).isNull();
        Map<String, String> copy = MDC.getCopyOfContextMap();
        assertThat(copy == null ? Map.<String, String>of() : copy)
                .doesNotContainKey(tenantKey())
                .doesNotContainKey(userKey());
    }

    @Test
    @DisplayName("工作失败时身份照样恢复且异常原样传播")
    void aFailingWorkStillRestoresTheIdentityAndKeepsItsOwnFailure() {
        IllegalStateException conflict = new IllegalStateException("WIKI_STATE_CONFLICT");
        MDC.put(tenantKey(), "outer-tenant");

        assertThatThrownBy(() -> context.call(null, () -> {
            assertThat(MDC.get(tenantKey())).isEqualTo(String.valueOf(INSTALLATION_TENANT_ID));
            throw conflict;
        })).isSameAs(conflict);

        assertThat(MDC.get(tenantKey())).isEqualTo("outer-tenant");
        assertThat(MDC.get(userKey())).isNull();
    }

    @Test
    @DisplayName("受检异常也如实传播，不被包装成伪成功")
    void aCheckedFailurePropagatesUnchanged() {
        IOException io = new IOException("upstream stream closed");

        assertThatThrownBy(() -> context.call(null, () -> {
            throw io;
        })).isSameAs(io);

        assertThat(MDC.get(tenantKey())).isNull();
    }

    @Test
    @DisplayName("缺少工作载荷的调用立即被拒绝，不安装任何身份")
    void aMissingWorkIsRejectedBeforeAnyIdentityIsInstalled() {
        assertThatThrownBy(() -> context.call(String.valueOf(TRUSTED_IDENTITY_TENANT_ID), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("work");

        assertThat(MDC.get(tenantKey())).isNull();
    }

    @Test
    @DisplayName("每个执行线程各自安装并恢复自己的身份")
    void everyExecutingThreadInstallsAndRestoresItsOwnIdentity() throws Exception {
        ExecutorService servletThread = Executors.newSingleThreadExecutor(named("servlet-thread"));
        ExecutorService workerThread = Executors.newSingleThreadExecutor(named("worker-thread"));
        try {
            servletThread.submit(() -> MDC.put(tenantKey(), "servlet-prior")).get();
            workerThread.submit(() -> MDC.put(tenantKey(), "worker-prior")).get();

            Future<Map<String, String>> servletObservation = servletThread.submit(this::observeOneContextCycle);
            Future<Map<String, String>> workerObservation = workerThread.submit(this::observeOneContextCycle);

            Map<String, String> onServletThread = servletObservation.get(5, TimeUnit.SECONDS);
            Map<String, String> onWorkerThread = workerObservation.get(5, TimeUnit.SECONDS);

            assertThat(onServletThread).containsEntry("installed", String.valueOf(INSTALLATION_TENANT_ID))
                    .containsEntry("installedUser", SERVICE_USER_ID)
                    .containsEntry("restored", "servlet-prior");
            assertThat(onWorkerThread).containsEntry("installed", String.valueOf(INSTALLATION_TENANT_ID))
                    .containsEntry("restored", "worker-prior");
            assertThat(onServletThread.get("restoredUser")).isNull();
            assertThat(onWorkerThread.get("restoredUser")).isNull();
            assertThat(MDC.get(tenantKey())).isNull();
        } finally {
            servletThread.shutdownNow();
            workerThread.shutdownNow();
        }
    }

    @Test
    @DisplayName("部署身份没有任何默认值，缺项在绑定阶段即违规而不是退化成 tenantId=1")
    void theDeploymentIdentityFailsClosedWithoutAnyDefaultTenant() {
        GatewayPersistenceProperties nothing = new GatewayPersistenceProperties();
        assertThat(nothing.getTenantId()).isNull();
        assertThat(nothing.getIdentityTenantId()).isNull();
        assertThat(nothing.getServiceUserId()).isNull();
        assertThat(nothing.getExpectedSchemaVersion()).isNull();
        assertThat(nothing.getExpectedSchemaSha256()).isNull();
        assertThat(nothing.isManagedDdlEnabled()).isFalse();
        assertThat(violatedProperties(nothing)).containsExactlyInAnyOrder("tenantId", "identityTenantId",
                "serviceUserId", "expectedSchemaVersion", "expectedSchemaSha256");

        GatewayPersistenceProperties hostile = new GatewayPersistenceProperties()
                .setTenantId(0L)
                .setIdentityTenantId(-1L)
                .setServiceUserId(" ")
                .setExpectedSchemaVersion(" ")
                .setExpectedSchemaSha256("75e0acba-not-sixty-four-hex");
        assertThat(violatedProperties(hostile)).contains("tenantId", "identityTenantId", "serviceUserId",
                "expectedSchemaVersion", "expectedSchemaSha256");

        ConfigurationProperties binding = GatewayPersistenceProperties.class
                .getAnnotation(ConfigurationProperties.class);
        assertThat(binding.prefix()).isEqualTo("yuheng.persistence");
        assertThat(binding.ignoreUnknownFields()).isFalse();
        assertThat(VALIDATOR.validate(wellFormedIdentity())).isEmpty();
    }

    @Test
    @DisplayName("过滤器把整条下游链放进部署身份，并无视请求自带的租户值")
    void theFilterWrapsTheChainAndIgnoresAnyRequestedTenant() throws Exception {
        RecordingChain chain = new RecordingChain();

        new GatewayPersistenceContextFilter(context)
                .doFilter(hostileRequest(), new MockHttpServletResponse(), chain);

        assertThat(chain.invoked).isTrue();
        assertThat(chain.tenant).isEqualTo(String.valueOf(INSTALLATION_TENANT_ID));
        assertThat(chain.user).isEqualTo(SERVICE_USER_ID);
        assertThat(MDC.get(tenantKey())).isNull();
    }

    @Test
    @DisplayName("已验证身份带着别的租户时在链之前失败关闭，异常也不被改写")
    void theMismatchingVerifiedIdentityNeverReachesTheChain() {
        SecurityContextHolder.getContext()
                .setAuthentication(verifiedIdentityWithClaim("tenant_id", FOREIGN_TENANT_ID));
        RecordingChain chain = new RecordingChain();

        Throwable failure = catchThrowable(() -> new GatewayPersistenceContextFilter(context)
                .doFilter(hostileRequest(), new MockHttpServletResponse(), chain));

        assertThat(failure).isInstanceOfSatisfying(EgonColaMybatisPlusConfigurationException.class,
                rejected -> assertThat(rejected.getStatus())
                        .isEqualTo("PERSISTENCE_IDENTITY_TENANT_MISMATCH"));
        assertThat(chain.invoked).isFalse();
        assertThat(MDC.get(tenantKey())).isNull();
    }

    @Test
    @DisplayName("签发方三种租户声明写法都被读取，命中受信任身份才放行")
    void everyClaimSpellingIsReadFromTheVerifiedIdentity() throws Exception {
        for (String claim : List.of("tenantId", "tenant_id", "tenant")) {
            for (Object value : List.of(String.valueOf(TRUSTED_IDENTITY_TENANT_ID), TRUSTED_IDENTITY_TENANT_ID)) {
                SecurityContextHolder.getContext().setAuthentication(verifiedIdentityWithClaim(claim, value));
                RecordingChain chain = new RecordingChain();

                new GatewayPersistenceContextFilter(context)
                        .doFilter(hostileRequest(), new MockHttpServletResponse(), chain);

                assertThat(chain.invoked).as("claim %s must be read", claim).isTrue();
                assertThat(chain.tenant).isEqualTo(String.valueOf(INSTALLATION_TENANT_ID));
            }
        }
    }

    @Test
    @DisplayName("已验证但不带租户的身份与匿名请求都由安装域服务")
    void aVerifiedIdentityWithoutAnyClaimAndAnAnonymousRequestAreServed() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(verifiedIdentityWithoutClaims());
        RecordingChain authenticated = new RecordingChain();
        new GatewayPersistenceContextFilter(context)
                .doFilter(hostileRequest(), new MockHttpServletResponse(), authenticated);

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key",
                "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        RecordingChain anonymous = new RecordingChain();
        new GatewayPersistenceContextFilter(context)
                .doFilter(hostileRequest(), new MockHttpServletResponse(), anonymous);

        assertThat(authenticated.invoked).isTrue();
        assertThat(anonymous.invoked).isTrue();
        assertThat(authenticated.tenant).isEqualTo(String.valueOf(INSTALLATION_TENANT_ID));
        assertThat(anonymous.tenant).isEqualTo(String.valueOf(INSTALLATION_TENANT_ID));
    }

    @Test
    @DisplayName("过滤器注册在认证链之后、异步派发上仍执行且自身不是 @Component")
    void theFilterRegistersAfterSecurityAndStillRunsOnAsyncDispatch() {
        GatewayPersistenceContextFilter filter = new GatewayPersistenceContextFilter(context);

        assertThat(GatewayPersistenceContextFilter.FILTER_ORDER)
                .isGreaterThan(SecurityProperties.DEFAULT_FILTER_ORDER)
                .isLessThan(Ordered.LOWEST_PRECEDENCE);
        Boolean skipsAsyncDispatch = ReflectionTestUtils.invokeMethod(filter, "shouldNotFilterAsyncDispatch");
        assertThat(skipsAsyncDispatch).isFalse();
        assertThat(GatewayPersistenceContextFilter.class.isAnnotationPresent(Component.class)).isFalse();
    }

    @Test
    @DisplayName("过滤器由持久化装配点以显式 bean 名与容器顺序注册")
    void theConfigurationRegistersTheFilterWithAnExplicitOrder() throws NoSuchMethodException {
        GatewayPersistenceConfiguration configuration =
                new GatewayPersistenceConfiguration(componentProperties, null);

        var registration = configuration.gatewayPersistenceContextFilterRegistration(context);

        assertThat(registration.getOrder()).isEqualTo(GatewayPersistenceContextFilter.FILTER_ORDER);
        assertThat(registration.getUrlPatterns()).containsExactly("/*");
        assertThat(registeredBeanName("gatewayPersistenceContextFilterRegistration",
                GatewayPersistenceContextComponent.class))
                .isEqualTo("gatewayPersistenceContextFilterRegistration");
        assertThat(registeredBeanName("gatewayPersistenceContextComponent", GatewayPersistenceProperties.class))
                .isEqualTo("gatewayPersistenceContextComponent");
    }

    @Test
    @DisplayName("Admin 主源码里只有持久化上下文组件会写 MDC")
    void thePersistenceContextIsTheOnlyMdcWriterInAdminMainSources() throws IOException {
        List<Path> writers;
        try (Stream<Path> paths = Files.walk(ADMIN_MAIN)) {
            writers = paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(this::writesTheMdc)
                    .sorted()
                    .toList();
        }

        assertThat(writers).containsExactly(ADMIN_MAIN.resolve("config/GatewayPersistenceContextComponent.java"));
    }

    private Map<String, String> observeOneContextCycle() throws Exception {
        Map<String, String> seen = new LinkedHashMap<>();
        context.call(null, () -> {
            seen.put("installed", MDC.get(tenantKey()));
            seen.put("installedUser", MDC.get(userKey()));
            return null;
        });
        seen.put("restored", MDC.get(tenantKey()));
        seen.put("restoredUser", MDC.get(userKey()));
        return seen;
    }

    /**
     * 中文说明：把「工作是否被执行」记进标志位；被拒绝的声明必须让该标志保持 false。
     * English summary: Records whether the guarded work actually ran; a refused claim must leave the flag false.
     * @param ran 参数 执行标志；parameter the execution flag.
     * @return 返回 只做标记的工作载荷；returns a work payload that only marks execution.
     */
    private static Callable<String> recording(AtomicBoolean ran) {
        return () -> {
            ran.set(true);
            return "executed";
        };
    }

    private boolean writesTheMdc(Path path) {
        try {
            return MDC_WRITE.matcher(codeOnly(Files.readString(path, StandardCharsets.UTF_8))).find();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot inspect " + path, failure);
        }
    }

    private Set<String> violatedProperties(GatewayPersistenceProperties properties) {
        return VALIDATOR.validate(properties).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static GatewayPersistenceProperties wellFormedIdentity() {
        return new GatewayPersistenceProperties()
                .setTenantId(INSTALLATION_TENANT_ID)
                .setIdentityTenantId(TRUSTED_IDENTITY_TENANT_ID)
                .setServiceUserId(SERVICE_USER_ID)
                .setExpectedSchemaVersion("20260922_001")
                .setExpectedSchemaSha256("b".repeat(64))
                .setManagedDdlEnabled(true);
    }

    /**
     * 中文说明：带着自报租户的请求（头/参数/属性/请求体四种写法全部指向越权租户）。
     * English summary: A request that self-reports a tenant under all four spellings — header, parameter, attribute and
     * body — pointing at the overreaching tenant.
     * @return 返回 越权请求夹具；returns the overreaching request fixture.
     */
    private static MockHttpServletRequest hostileRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/knowledge-bases");
        request.addHeader("X-Tenant-Id", FOREIGN_TENANT_ID);
        request.addHeader("tenantId", FOREIGN_TENANT_ID);
        request.setParameter("tenantId", FOREIGN_TENANT_ID);
        request.setAttribute("tenantId", FOREIGN_TENANT_ID);
        request.setContent(("{\"tenantId\":" + FOREIGN_TENANT_ID + "}").getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private static JwtAuthenticationToken verifiedIdentityWithClaim(String claim, Object value) {
        return verified(claimToken(claim, value));
    }

    private static JwtAuthenticationToken verifiedIdentityWithoutClaims() {
        return verified(claimToken(null, null));
    }

    private static Jwt claimToken(String claim, Object value) {
        Jwt.Builder builder = Jwt.withTokenValue("verified-access-token")
                .header("alg", "RS256")
                .subject("operator-1")
                .issuedAt(TOKEN_ISSUED_AT)
                .expiresAt(TOKEN_ISSUED_AT.plusSeconds(300));
        if (claim != null) {
            builder.claim(claim, value);
        }
        return builder.build();
    }

    /**
     * 中文说明：构造「已被认证链验证过」的身份：过滤器观察的就是这种上下文，因此夹具必须显式是 trusted 的。
     * English summary: Builds an identity the authentication chain has already verified; that is exactly what the filter
     * observes, so the fixture must explicitly be trusted.
     * @param jwt 参数 已解析令牌；parameter the parsed token.
     * @return 返回 已认证身份；returns the authenticated identity.
     */
    private static JwtAuthenticationToken verified(Jwt jwt) {
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES);
        token.setAuthenticated(true);
        return token;
    }

    private static ThreadFactory named(String name) {
        return runnable -> new Thread(runnable, name);
    }

    /**
     * 中文说明：读取 {@code @Bean} 上显式声明的 bean 名，用来钉住「单例组件必须指名」这条约定。
     * English summary: Reads the bean name declared explicitly on {@code @Bean}, pinning the convention that every
     * singleton component must be named.
     * @param methodName 参数 装配方法名；parameter the wiring method name.
     * @param parameterType 参数 唯一入参类型；parameter the method's single parameter type.
     * @return 返回 显式 bean 名；returns the explicit bean name.
     */
    private static String registeredBeanName(String methodName, Class<?> parameterType)
            throws NoSuchMethodException {
        Method method = GatewayPersistenceConfiguration.class
                .getMethod(methodName, parameterType);
        Bean annotation = method.getAnnotation(Bean.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).hasSize(1);
        return annotation.value()[0];
    }

    private String tenantKey() {
        return componentProperties.getTenantId().getMdcKey();
    }

    private String userKey() {
        return componentProperties.getAudit().getUserIdMdcKey();
    }

    private static String codeOnly(String text) {
        return LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(text).replaceAll(" ")).replaceAll(" ");
    }

    /**
     * 中文说明：下游链替身，只记录「自己被调用了吗」与调用那一刻执行线程上的持久化身份，不伪造任何响应。
     * English summary: A downstream-chain stand-in recording only whether it was invoked and which persistence identity
     * the executing thread carried at that moment; it fabricates no response.
     */
    private final class RecordingChain implements FilterChain {

        private boolean invoked;
        private String tenant;
        private String user;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            invoked = true;
            tenant = MDC.get(tenantKey());
            user = MDC.get(userKey());
        }
    }
}
