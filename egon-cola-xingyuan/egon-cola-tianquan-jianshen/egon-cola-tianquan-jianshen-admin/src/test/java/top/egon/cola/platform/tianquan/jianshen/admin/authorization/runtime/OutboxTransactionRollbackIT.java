package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxAutoConfiguration;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.AuthorizationEventPublisher;
import top.egon.cola.platform.tianquan.jianshen.admin.config.flyway.Rbac3FlywayConfiguration;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.controller.message.Rbac3RuntimeProjectionDeliveryHandler;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.outbox.TransactionalOutboxAuthorizationEventPublisher;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.SqlSessionFactory;
import org.hibernate.Session;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.cola.component.outbox.autoconfigure.OutboxInfrastructure;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.service.RoleResourceGrantService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.integration.IsolatedAuthorizationProvisioningIT;
import top.egon.cola.platform.tianquan.jianshen.admin.integration.IsolatedAuthorizationProvisioningIT.AuthorizationTransactionProbeDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.AuthorizationEventVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.enums.Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum;

@lombok.extern.slf4j.Slf4j
class OutboxTransactionRollbackIT {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_INTEGRATION_MANIFEST", matches = ".+")
    void jpaRoleResourceAndMpOutboxRollback() throws Exception {
        verifyJpaRoleResourceTransaction("ROLLBACK");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_INTEGRATION_MANIFEST", matches = ".+")
    void jpaRoleResourceAndMpOutboxAppendFault() throws Exception {
        verifyJpaRoleResourceTransaction("APPEND_FAULT");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_INTEGRATION_MANIFEST", matches = ".+")
    void jpaRoleResourceAndMpOutboxCommit() throws Exception {
        verifyJpaRoleResourceTransaction("COMMIT");
    }

    private void verifyJpaRoleResourceTransaction(String outcome) throws Exception {
        var inputs = IsolatedAuthorizationProvisioningIT.readTransactionInputs();
        var probe = IsolatedAuthorizationProvisioningIT.readTransactionProbe(inputs);
        Path root = Path.of(inputs.getManifest().getRuntimeRoot());
        Path manifest = Path.of(System.getenv("XINGYUAN_INTEGRATION_MANIFEST"));
        Path receiptFile = transactionEvidencePath(root, System.getProperty("xingyuan.isolated.evidence-directory"),
                inputs.getManifest().getRunId(), inputs.getManifestSha256(), outcome);
        Instant startedAt = Instant.now();
        JpaMpTransactionProofVO proof;
        try (var context = IsolatedAuthorizationProvisioningIT.openAuthorizationContext(
                manifest, root.resolve("evidence/db-rbac.json"), Path.of(inputs.getManifest().getRepoRoot()))) {
            var manager = context.getBean("transactionManager", JpaTransactionManager.class);
            assertThat(context.getBeansOfType(PlatformTransactionManager.class)).hasSize(1);
            var definition = context.getBeanFactory().getBeanDefinition("transactionManager");
            assertThat(definition.getFactoryBeanName()).contains("HibernateJpaConfiguration");
            assertThat(definition.getFactoryMethodName()).isEqualTo("transactionManager");
            DataSource logical = context.getBean("dataSource", DataSource.class);
            var factory = context.getBean("entityManagerFactory", EntityManagerFactory.class);
            var sessions = context.getBean("sqlSessionFactory", SqlSessionFactory.class);
            assertThat(manager.getDataSource()).isSameAs(logical);
            assertThat(((EntityManagerFactoryInfo) factory).getDataSource()).isSameAs(logical);
            assertThat(sessions.getConfiguration().getEnvironment().getDataSource()).isSameAs(logical);
            assertThat(sessions.getConfiguration().getEnvironment().getTransactionFactory())
                    .isInstanceOf(org.mybatis.spring.transaction.SpringManagedTransactionFactory.class);
            var infrastructure = context.getBean("outboxInfrastructure", OutboxInfrastructure.class);
            assertThat(infrastructure.dataSource()).isSameAs(logical);
            assertThat(infrastructure.transactionManager()).isSameAs(manager);

            Runnable ownership = context.getBean("isolatedOwnershipGuard", Runnable.class);
            ownership.run();
            DataSource physical = context.getBean("isolatedPhysicalDataSource", DataSource.class);
            var independent = new JdbcTemplate(physical);
            String expectedRoleCode = "XINGYUAN_TX_" + inputs.getManifest().getRunId().replace("-", "").toUpperCase(Locale.ROOT);
            assertThat(independent.queryForObject("select role_code from public.rbac3_role where tenant_id = ? and id = ?",
                    String.class, Long.valueOf(probe.getTenantId()), Long.valueOf(probe.getRoleId())))
                    .isEqualTo(expectedRoleCode);
            assertThat(independent.queryForObject("select count(*) from public.rbac3_user_role_assignment where tenant_id = ? and role_id = ?",
                    Long.class, Long.valueOf(probe.getTenantId()), Long.valueOf(probe.getRoleId())))
                    .as("the transaction probe must never revoke an assigned application role").isZero();
            var roles = context.getBean("roleResourceGrantService", RoleResourceGrantService.class);
            Instant now = context.getBean("isolatedAuthorizationClock", Clock.class).instant();
            var original = roles.tree(probe.getTenantId(), probe.getRoleId(), now);
            assertThat(original.directResourceIds()).isNotEmpty();
            var baseline = independentTransactionObservation(physical, probe, inputs.getReceipt().getOid());
            IndependentTransactionObservationVO observed;
            IndependentTransactionObservationVO restored;
            var sameConnectionConfirmed = new java.util.concurrent.atomic.AtomicBoolean();
            EntityManager entityManager = context.getBean("jpaSharedEM_entityManagerFactory", EntityManager.class);
            var append = new ActualOutboxAppendProbe();
            var identity = new AtomicReference<List<Long>>();
            var enlisted = new AtomicReference<Connection>();
            append.afterInsert = () -> {
                entityManager.flush();
                entityManager.unwrap(Session.class).doWork(connection -> {
                    assertThat(connection).isSameAs(enlisted.get());
                    assertThat(append.connection.get()).isSameAs(connection);
                    assertThat(databaseTransactionIdentity(connection)).isEqualTo(identity.get());
                    assertThat(databaseTransactionIdentity(append.connection.get())).isEqualTo(identity.get());
                    sameConnectionConfirmed.set(true);
                });
                if ("APPEND_FAULT".equals(outcome)) {
                    throw new IllegalStateException("XINGYUAN_TEST_APPEND_FAULT");
                }
            };
            sessions.getConfiguration().addInterceptor(append);
            var transaction = new TransactionTemplate(manager);
            Runnable mutate = () -> transaction.executeWithoutResult(status -> {
                ownership.run();
                requireUnpublishedTransactionEvidence(root, receiptFile, inputs.getManifest().getRunId(), inputs.getManifestSha256(), outcome);
                Connection connection = DataSourceUtils.getConnection(logical);
                enlisted.set(connection);
                identity.set(databaseTransactionIdentity(connection));
                try {
                    roles.replace(probe.getTenantId(), probe.getRoleId(),
                            new ReplaceRoleResourcesRequestDTO(List.of(), now, null, original.roleVersion()),
                            probe.getActorId(), now);
                    entityManager.flush();
                    assertThat(append.insertCount).hasValue(1);
                    if ("ROLLBACK".equals(outcome)) {
                        throw new IllegalStateException("XINGYUAN_TEST_FORCED_ROLLBACK");
                    }
                } finally {
                    DataSourceUtils.releaseConnection(connection, logical);
                }
            });
            if ("COMMIT".equals(outcome)) {
                mutate.run();
                assertThat(append.insertCount).hasValue(1);
                ownership.run();
                observed = independentTransactionObservation(physical, probe, inputs.getReceipt().getOid());
                assertThat(observed.roleSha256()).isNotEqualTo(baseline.roleSha256());
                assertThat(observed.grantsSha256()).isNotEqualTo(baseline.grantsSha256());
                assertThat(observed.epochSha256()).isNotEqualTo(baseline.epochSha256());
                assertThat(observed.state().outboxCount()).isEqualTo(baseline.state().outboxCount() + 1L);
                assertThat(roles.tree(probe.getTenantId(), probe.getRoleId(), now).directResourceIds()).isEmpty();
                // Restore this confirmed committed test role through the same real owner and a fresh CAS version.
                append.afterInsert = () -> { };
                var current = roles.tree(probe.getTenantId(), probe.getRoleId(), now);
                ownership.run();
                requireUnpublishedTransactionEvidence(root, receiptFile, inputs.getManifest().getRunId(), inputs.getManifestSha256(), outcome);
                roles.replace(probe.getTenantId(), probe.getRoleId(),
                        new ReplaceRoleResourcesRequestDTO(original.directResourceIds(), now, null, current.roleVersion()),
                        probe.getActorId(), now);
                assertThat(roles.tree(probe.getTenantId(), probe.getRoleId(), now).directResourceIds())
                        .containsExactlyElementsOf(original.directResourceIds());
                ownership.run();
                restored = independentTransactionObservation(physical, probe, inputs.getReceipt().getOid());
            } else {
                Throwable failure = org.assertj.core.api.Assertions.catchThrowable(mutate::run);
                assertThat(failure).isNotNull();
                while (failure.getCause() != null && failure.getCause() != failure) {
                    failure = failure.getCause();
                }
                assertThat(failure).hasMessage("APPEND_FAULT".equals(outcome)
                        ? "XINGYUAN_TEST_APPEND_FAULT" : "XINGYUAN_TEST_FORCED_ROLLBACK");
                assertThat(append.insertCount).hasValue(1);
                ownership.run();
                observed = independentTransactionObservation(physical, probe, inputs.getReceipt().getOid());
                assertThat(observed.state().rowsSha256()).isEqualTo(baseline.state().rowsSha256());
                restored = observed;
                assertThat(roles.tree(probe.getTenantId(), probe.getRoleId(), now).directResourceIds())
                        .containsExactlyElementsOf(original.directResourceIds());
            }
            assertThat(sameConnectionConfirmed).isTrue();
            List<Long> writer = identity.get();
            assertThat(writer).hasSize(2).allSatisfy(value -> assertThat(value).isPositive());
            proof = new JpaMpTransactionProofVO(1, inputs.getManifest().getRunId(), inputs.getManifestSha256(),
                    inputs.getReceipt().getOid(), "JPA_MP_SAME_TRANSACTION_ONLY", outcome, "PASSED", writer.get(0), writer.get(1),
                    true, baseline.state(), observed.state(), restored.state(), 1, append.insertCount.get() - 1,
                    observed.state().outboxCount() - baseline.state().outboxCount(), restored.state().outboxCount() - observed.state().outboxCount(),
                    "COMMIT".equals(outcome), true, startedAt, Instant.now());
            validateTransactionProof(proof, inputs.getManifest().getRunId(), inputs.getManifestSha256(), inputs.getReceipt().getOid(), outcome, Instant.now());
        }
        var current = IsolatedAuthorizationProvisioningIT.readTransactionInputs();
        if (!inputs.getManifestSha256().equals(current.getManifestSha256()) || !inputs.getReceipt().equals(current.getReceipt())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        publishTransactionProof(root, receiptFile, proof);
    }

    private static Map<String, List<Map<String, Object>>> transactionSnapshot(
            JdbcTemplate independent, AuthorizationTransactionProbeDTO probe) {
        Long tenant = Long.valueOf(probe.getTenantId());
        Long role = Long.valueOf(probe.getRoleId());
        return Map.of(
                "role", independent.queryForList("select id, version from public.rbac3_role where tenant_id = ? and id = ?", tenant, role),
                "grants", independent.queryForList("select id, resource_id, status, version, valid_from, valid_to from public.rbac3_role_resource_grant where tenant_id = ? and role_id = ? order by id", tenant, role),
                "epoch", independent.queryForList("select policy_version, version from public.rbac3_tenant_authorization_state where tenant_id = ?", tenant),
                "events", independent.queryForList("select message_id, message_fingerprint, payload from egon_outbox.egon_cola_outbox_message "
                        + "where channel = 'tianquan-jianshen-runtime' and payload::jsonb ->> 'tenantId' = ? "
                        + "and payload::jsonb ->> 'aggregateType' = 'ROLE' and payload::jsonb ->> 'aggregateId' = ? order by message_id",
                        probe.getTenantId(), probe.getRoleId()));
    }

    record JpaMpTransactionStateVO(@jakarta.validation.constraints.Positive long independentBackendPid,
                                  @jakarta.validation.constraints.PositiveOrZero long roleVersion,
                                  @jakarta.validation.constraints.PositiveOrZero long policyVersion,
                                  @jakarta.validation.constraints.PositiveOrZero long authorizationStateVersion,
                                  @jakarta.validation.constraints.PositiveOrZero long outboxCount,
                                  @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Pattern(regexp = "[0-9a-f]{64}") String rowsSha256) { }

    record JpaMpTransactionProofVO(@jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(1) int schemaVersion,
                                  @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") String runId,
                                  @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Pattern(regexp = "[0-9a-f]{64}") String manifestSha256,
                                  @jakarta.validation.constraints.Positive long databaseOid,
                                  @jakarta.validation.constraints.Pattern(regexp = "JPA_MP_SAME_TRANSACTION_ONLY") @jakarta.validation.constraints.NotNull String scope,
                                  @jakarta.validation.constraints.Pattern(regexp = "ROLLBACK|APPEND_FAULT|COMMIT") @jakarta.validation.constraints.NotNull String outcome,
                                  @jakarta.validation.constraints.Pattern(regexp = "PASSED") @jakarta.validation.constraints.NotNull String status,
                                  @jakarta.validation.constraints.Positive long backendPid,
                                  @jakarta.validation.constraints.Positive long transactionId,
                                  boolean sameConnectionConfirmed,
                                  @jakarta.validation.Valid @jakarta.validation.constraints.NotNull JpaMpTransactionStateVO baseline,
                                  @jakarta.validation.Valid @jakarta.validation.constraints.NotNull JpaMpTransactionStateVO observed,
                                  @jakarta.validation.Valid @jakarta.validation.constraints.NotNull JpaMpTransactionStateVO restored,
                                  @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(1) int attemptedInsertCount,
                                  @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(1) int restoreInsertCount,
                                  @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(1) long committedOutboxDelta,
                                  @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(1) long restoreOutboxDelta,
                                  boolean restorePerformed, boolean originalGrantsRestored,
                                  @jakarta.validation.constraints.NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant startedAt,
                                  @jakarta.validation.constraints.NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant recordedAt) { }

    private record IndependentTransactionObservationVO(JpaMpTransactionStateVO state, String roleSha256, String grantsSha256, String epochSha256) { }

    private static IndependentTransactionObservationVO independentTransactionObservation(DataSource physical, AuthorizationTransactionProbeDTO probe, long expectedOid) throws Exception {
        try (var connection = physical.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            var jdbc = new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true));
            jdbc.setQueryTimeout(5);
            var rows = transactionSnapshot(jdbc, probe);
            var state = jdbc.queryForObject("""
                    select pg_backend_pid()::bigint, d.oid::bigint, r.version, a.policy_version, a.version,
                           (select count(*) from egon_outbox.egon_cola_outbox_message
                            where channel='tianquan-jianshen-runtime' and payload::jsonb ->> 'tenantId'=?
                              and payload::jsonb ->> 'aggregateType'='ROLE' and payload::jsonb ->> 'aggregateId'=?)
                    from public.rbac3_role r join public.rbac3_tenant_authorization_state a on a.tenant_id=r.tenant_id
                    join pg_database d on d.datname=current_database() where r.tenant_id=? and r.id=?
                    """, (row, ignored) -> {
                if (row.getLong(2) != expectedOid) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                return new JpaMpTransactionStateVO(row.getLong(1), row.getLong(3), row.getLong(4), row.getLong(5), row.getLong(6), transactionRowsSha256(rows));
            }, probe.getTenantId(), probe.getRoleId(), Long.valueOf(probe.getTenantId()), Long.valueOf(probe.getRoleId()));
            var observation = new IndependentTransactionObservationVO(state, transactionRowsSha256(rows.get("role")), transactionRowsSha256(rows.get("grants")), transactionRowsSha256(rows.get("epoch")));
            connection.rollback();
            return observation;
        }
    }

    private static ObjectMapper transactionProofMapper() {
        return org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json()
                .featuresToEnable(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
                        com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION,
                        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        com.fasterxml.jackson.databind.MapperFeature.ALLOW_COERCION_OF_SCALARS).build();
    }

    private static String transactionRowsSha256(Object rows) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(transactionProofMapper().writeValueAsBytes(rows)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException | java.security.NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED", unavailable);
        }
    }

    static void validateTransactionProof(JpaMpTransactionProofVO proof, String runId, String digest, long oid, String outcome, Instant now) {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            new top.egon.cola.component.common.core.validation.ValidationUtils(factory.getValidator()).validate(proof);
        }
        if (!runId.equals(proof.runId()) || !digest.equals(proof.manifestSha256()) || oid != proof.databaseOid() || !outcome.equals(proof.outcome())
                || !proof.sameConnectionConfirmed() || !proof.originalGrantsRestored() || proof.backendPid() > Integer.MAX_VALUE
                || proof.startedAt().isAfter(proof.recordedAt()) || proof.recordedAt().isAfter(now)
                || List.of(proof.baseline(), proof.observed(), proof.restored()).stream().anyMatch(value -> value.independentBackendPid() == proof.backendPid() || value.independentBackendPid() > Integer.MAX_VALUE)
                || proof.committedOutboxDelta() != proof.observed().outboxCount() - proof.baseline().outboxCount()
                || proof.restoreOutboxDelta() != proof.restored().outboxCount() - proof.observed().outboxCount()) {
            throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
        }
        if ("COMMIT".equals(outcome)) {
            if (!proof.restorePerformed() || proof.restoreInsertCount() != 1 || proof.committedOutboxDelta() != 1 || proof.restoreOutboxDelta() != 1
                    || proof.observed().roleVersion() <= proof.baseline().roleVersion() || proof.restored().roleVersion() <= proof.observed().roleVersion()
                    || proof.observed().policyVersion() <= proof.baseline().policyVersion() || proof.restored().policyVersion() <= proof.observed().policyVersion()
                    || proof.observed().authorizationStateVersion() <= proof.baseline().authorizationStateVersion()
                    || proof.restored().authorizationStateVersion() <= proof.observed().authorizationStateVersion()) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
        } else if (proof.restorePerformed() || proof.restoreInsertCount() != 0 || proof.committedOutboxDelta() != 0 || proof.restoreOutboxDelta() != 0
                || !sameTransactionState(proof.baseline(), proof.observed()) || !sameTransactionState(proof.observed(), proof.restored())) {
            throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    private static boolean sameTransactionState(JpaMpTransactionStateVO first, JpaMpTransactionStateVO second) {
        return first.roleVersion() == second.roleVersion() && first.policyVersion() == second.policyVersion()
                && first.authorizationStateVersion() == second.authorizationStateVersion() && first.outboxCount() == second.outboxCount() && first.rowsSha256().equals(second.rowsSha256());
    }

    static Path transactionEvidencePath(Path root, String directory, String runId, String digest, String outcome) throws java.io.IOException {
        if (root == null || directory == null || runId == null || digest == null || outcome == null || !runId.matches("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}") || !digest.matches("[0-9a-f]{64}")
                || !java.util.Set.of("ROLLBACK", "APPEND_FAULT", "COMMIT").contains(outcome)) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
        Path evidence = Path.of(directory);
        if (!root.isAbsolute() || !root.normalize().equals(root) || root.getFileName() == null || !root.getFileName().toString().equals("xingyuan-integration-" + runId)
                || !evidence.isAbsolute() || !evidence.normalize().equals(evidence) || !root.resolve("evidence").equals(evidence.getParent())
                || !evidence.getFileName().toString().matches("verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var owner = root.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
        for (Path path = evidence; path != null; path = path.getParent()) {
            if (Files.isSymbolicLink(path)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            if (path.startsWith(root) && (!Files.isDirectory(path, java.nio.file.LinkOption.NOFOLLOW_LINKS) || !owner.equals(Files.getOwner(path))
                    || !Files.getPosixFilePermissions(path).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        Path file = evidence.resolve("jpa-mp-" + outcome.toLowerCase(Locale.ROOT).replace('_', '-') + ".json");
        if (Files.isSymbolicLink(file)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        if (Files.exists(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(file, java.nio.file.LinkOption.NOFOLLOW_LINKS) || !owner.equals(Files.getOwner(file))
                    || !Files.getPosixFilePermissions(file).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
        }
        return file;
    }

    private static void requireUnpublishedTransactionEvidence(Path root, Path file, String runId, String digest, String outcome) {
        try {
            if (!file.equals(transactionEvidencePath(root, file.getParent().toString(), runId, digest, outcome))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        } catch (java.io.IOException unavailable) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE", unavailable); }
    }

    private static void publishTransactionProof(Path root, Path file, JpaMpTransactionProofVO proof) throws Exception {
        validateTransactionProof(proof, proof.runId(), proof.manifestSha256(), proof.databaseOid(), proof.outcome(), Instant.now());
        requireUnpublishedTransactionEvidence(root, file, proof.runId(), proof.manifestSha256(), proof.outcome());
        Path temporary = Files.createTempFile(file.getParent(), ".jpa-mp-", ".tmp", java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
        try {
            try (var output = java.nio.channels.FileChannel.open(temporary, java.nio.file.StandardOpenOption.WRITE)) {
                var bytes = java.nio.ByteBuffer.wrap(transactionProofMapper().writeValueAsBytes(proof));
                while (bytes.hasRemaining()) { output.write(bytes); }
                output.force(true);
            }
            requireUnpublishedTransactionEvidence(root, file, proof.runId(), proof.manifestSha256(), proof.outcome());
            Files.createLink(file, temporary);
            try (var directory = java.nio.channels.FileChannel.open(file.getParent(), java.nio.file.StandardOpenOption.READ)) { directory.force(true); }
        } finally { Files.deleteIfExists(temporary); }
    }

    @Test
    void transactionProofRequiresActualIndependentReadbacksAndSeparatesRestoreEvents() throws Exception {
        String runId = UUID.randomUUID().toString(), digest = "a".repeat(64);
        for (String outcome : List.of("ROLLBACK", "APPEND_FAULT", "COMMIT")) {
            var proof = transactionProofFixture(runId, digest, outcome);
            org.assertj.core.api.Assertions.assertThatCode(() -> validateTransactionProof(proof, runId, digest, 71L, outcome, NOW)).doesNotThrowAnyException();
            String json = transactionProofMapper().writeValueAsString(proof);
            assertThat(json).contains("JPA_MP_SAME_TRANSACTION_ONLY", "2026-07-30T12:00:00Z").doesNotContain("actorId", "payload", "subject", "token");
            assertThat(transactionProofMapper().readValue(json, JpaMpTransactionProofVO.class)).isEqualTo(proof);
            assertThatThrownBy(() -> validateTransactionProof(proof, UUID.randomUUID().toString(), digest, 71L, outcome, NOW)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
            assertThatThrownBy(() -> validateTransactionProof(proof, runId, digest, 72L, outcome, NOW)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        var valid = transactionProofMapper().valueToTree(transactionProofFixture(runId, digest, "COMMIT"));
        for (String fault : List.of("sameBackend", "versionRollback", "missingRestoreEvent", "changedOutcome", "futureTime")) {
            var document = (com.fasterxml.jackson.databind.node.ObjectNode) valid.deepCopy();
            if (fault.equals("sameBackend")) { document.withObject("observed").put("independentBackendPid", 301); }
            else if (fault.equals("versionRollback")) { document.withObject("restored").put("roleVersion", 2); }
            else if (fault.equals("missingRestoreEvent")) { document.put("restoreOutboxDelta", 0); }
            else if (fault.equals("changedOutcome")) { document.put("outcome", "ROLLBACK"); }
            else { document.put("recordedAt", NOW.plusSeconds(1).toString()); }
            var changed = transactionProofMapper().treeToValue(document, JpaMpTransactionProofVO.class);
            assertThatThrownBy(() -> validateTransactionProof(changed, runId, digest, 71L, "COMMIT", NOW)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        var unknown = (com.fasterxml.jackson.databind.node.ObjectNode) valid.deepCopy(); unknown.put("outcome", "RETRY_UNKNOWN");
        var invalid = transactionProofMapper().treeToValue(unknown, JpaMpTransactionProofVO.class);
        assertThatThrownBy(() -> validateTransactionProof(invalid, runId, digest, 71L, "RETRY_UNKNOWN", NOW)).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        var overflow = (com.fasterxml.jackson.databind.node.ObjectNode) valid.deepCopy(); overflow.put("transactionId", new java.math.BigInteger("18446744073709551616"));
        assertThatThrownBy(() -> transactionProofMapper().treeToValue(overflow, JpaMpTransactionProofVO.class)).isInstanceOf(com.fasterxml.jackson.databind.JsonMappingException.class);
    }

    @Test
    void transactionEvidenceIsPrivateAttemptBoundAndNeverOverwrites(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        String runId = UUID.randomUUID().toString(), digest = "a".repeat(64);
        Path root = Files.createDirectory(temporary.toRealPath().resolve("xingyuan-integration-" + runId));
        Path base = Files.createDirectory(root.resolve("evidence")); Path attempt = Files.createDirectory(base.resolve("verify-" + UUID.randomUUID()));
        for (Path path : List.of(root, base, attempt)) { Files.setPosixFilePermissions(path, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")); }
        Path file = transactionEvidencePath(root, attempt.toString(), runId, digest, "ROLLBACK");
        assertThat(file).isEqualTo(attempt.resolve("jpa-mp-rollback.json"));
        assertThatThrownBy(() -> transactionEvidencePath(root, base.toString(), runId, digest, "ROLLBACK")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> transactionEvidencePath(root, attempt.toString(), runId, digest, "UNKNOWN_COMMIT")).hasMessage("XINGYUAN_INPUT_INVALID");
        Files.setPosixFilePermissions(attempt, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
        assertThatThrownBy(() -> transactionEvidencePath(root, attempt.toString(), runId, digest, "ROLLBACK")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Files.setPosixFilePermissions(attempt, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        var proof = transactionProofFixture(runId, digest, "ROLLBACK");
        publishTransactionProof(root, file, proof);
        byte[] first = Files.readAllBytes(file);
        assertThat(Files.getPosixFilePermissions(file)).isEqualTo(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        assertThatThrownBy(() -> publishTransactionProof(root, file, proof)).hasMessage("XINGYUAN_STATE_CONFLICT");
        assertThat(Files.readAllBytes(file)).isEqualTo(first);
        Path linked = base.resolve("verify-" + UUID.randomUUID()); Files.createSymbolicLink(linked, attempt);
        assertThatThrownBy(() -> transactionEvidencePath(root, linked.toString(), runId, digest, "COMMIT")).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    private static JpaMpTransactionProofVO transactionProofFixture(String runId, String digest, String outcome) {
        var baseline = new JpaMpTransactionStateVO(302, 2, 7, 5, 4, "b".repeat(64));
        var observed = "COMMIT".equals(outcome) ? new JpaMpTransactionStateVO(303, 3, 8, 6, 5, "c".repeat(64))
                : new JpaMpTransactionStateVO(303, 2, 7, 5, 4, "b".repeat(64));
        var restored = "COMMIT".equals(outcome) ? new JpaMpTransactionStateVO(304, 4, 9, 7, 6, "d".repeat(64)) : observed;
        return new JpaMpTransactionProofVO(1, runId, digest, 71L, "JPA_MP_SAME_TRANSACTION_ONLY", outcome, "PASSED", 301, 701,
                true, baseline, observed, restored, 1, "COMMIT".equals(outcome) ? 1 : 0, "COMMIT".equals(outcome) ? 1L : 0L,
                "COMMIT".equals(outcome) ? 1L : 0L, "COMMIT".equals(outcome), true, NOW.minusSeconds(1), NOW);
    }

    private static List<Long> databaseTransactionIdentity(Connection connection) {
        try (var query = connection.prepareStatement("select pg_backend_pid()::bigint, txid_current()::bigint")) {
            query.setQueryTimeout(5);
            try (var row = query.executeQuery()) {
                if (!row.next()) {
                    throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE");
                }
                return List.of(row.getLong(1), row.getLong(2));
            }
        } catch (SQLException unavailable) {
            throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE");
        }
    }

    /** Fault injection runs after the real MP insert and flushes the real JPA change before throwing. */
    @Intercepts({
            @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
            @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
    private static class ActualOutboxAppendProbe implements Interceptor {
        private final AtomicReference<Connection> connection = new AtomicReference<>();
        private final AtomicInteger insertCount = new AtomicInteger();
        private Runnable afterInsert;

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            if (invocation.getTarget() instanceof StatementHandler statement
                    && statement.getBoundSql().getSql().stripLeading().toUpperCase(Locale.ROOT)
                    .startsWith("INSERT INTO EGON_OUTBOX.EGON_COLA_OUTBOX_MESSAGE")) {
                connection.set((Connection) invocation.getArgs()[0]);
            }
            Object result = invocation.proceed();
            if (invocation.getTarget() instanceof Executor
                    && ((MappedStatement) invocation.getArgs()[0]).getId().equals(
                    "top.egon.cola.component.outbox.persistence.dao.OutboxMessageDAO.insertMessage")) {
                assertThat(result).isEqualTo(1);
                insertCount.incrementAndGet();
                afterInsert.run();
            }
            return result;
        }
    }

    @Test
    void mapsTheStableRbac3EnvelopeAndReturnsTheExistingReceiptOnReplay() {
        AtomicReference<OutboxMessage> captured = new AtomicReference<>();
        TransactionalOutbox outbox = message -> {
            captured.set(message);
            return new OutboxReceipt("persisted-event", message.idempotencyKey(), false);
        };
        var adapter = new TransactionalOutboxAuthorizationEventPublisher(
                outbox, Clock.fixed(NOW, ZoneOffset.UTC));

        String messageId = adapter.enqueue(event());

        assertThat(messageId).isEqualTo("persisted-event");
        assertThat(captured.get().channel()).isEqualTo("tianquan-jianshen-runtime");
        assertThat(captured.get().destination())
                .isEqualTo("tianquan-jianshen.role-activation.changed.v1");
        assertThat(captured.get().idempotencyKey())
                .isEqualTo("7:tianquan-jianshen.role-activation.changed.v1:99:4");
        assertThat(captured.get().payload().toString())
                .contains("eventId", "aggregateVersion=4")
                .doesNotContain("token", "password", "secret");
    }

    @Test
    void retiredSessionRevocationNeverReachesTheOutboxWriter() {
        AtomicReference<OutboxMessage> captured = new AtomicReference<>();
        var adapter = new TransactionalOutboxAuthorizationEventPublisher(
                message -> {
                    captured.set(message);
                    return new OutboxReceipt(message.messageId(), message.idempotencyKey(), true);
                }, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> adapter.enqueue(new AuthorizationEventVO(
                "7", "SESSION", "99", "SESSION_REVOKED",
                Map.of("contextVersion", "5", "reason", "ADMIN_REVOKE"),
                "session:99:5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported Tianquan-Jianshen authorization event type");
        assertThat(captured.get()).isNull();
    }

    @Test
    void roleActivationUsesTheChangingContextVersionBeforeStaticPolicyVersions() {
        AtomicReference<OutboxMessage> captured = new AtomicReference<>();
        var adapter = new TransactionalOutboxAuthorizationEventPublisher(
                message -> {
                    captured.set(message);
                    return new OutboxReceipt(
                            message.messageId(), message.idempotencyKey(), true);
                }, Clock.fixed(NOW, ZoneOffset.UTC));

        adapter.enqueue(new AuthorizationEventVO(
                "7", "USER", "99", "TIANQUAN_JIANSHEN_USER_ACTIVE_ROLES_REPLACED",
                Map.of(
                        "contextVersion", "6",
                        "authVersion", "1",
                        "policyVersion", "1"),
                "role-activation:99:6"));

        assertThat(captured.get().idempotencyKey())
                .isEqualTo("7:tianquan-jianshen.role-activation.changed.v1:99:6");
    }

    @Test
    void deliveryRejectsUnknownDestinationsAndTreatsDuplicateProjectionAsSuccess()
            throws Exception {
        AtomicReference<String> applied = new AtomicReference<>();
        var handler = new Rbac3RuntimeProjectionDeliveryHandler(envelope -> {
            applied.set(envelope.eventId() + ':' + envelope.aggregateVersion());
            return Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum.ALREADY_APPLIED;
        });

        assertThatThrownBy(() -> handler.validateDestination("tianquan-jianshen.unknown.v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported Tianquan-Jianshen runtime destination");
        DeliveryResult result = handler.deliver(context(
                "tianquan-jianshen.role-activation.changed.v1",
                """
                        {"eventId":"event-1","eventType":"tianquan-jianshen.role-activation.changed.v1",
                         "schemaVersion":1,"occurredAt":"2026-07-30T12:00:00Z",
                         "tenantId":"7","aggregateType":"SESSION","aggregateId":"99",
                         "aggregateVersion":4,"traceId":"trace-1","payload":{}}
                        """));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        assertThat(applied.get()).isEqualTo("event-1:4");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TIANQUAN_JIANSHEN_IT_POSTGRES_URL", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "TIANQUAN_JIANSHEN_IT_POSTGRES_USER", matches = ".+")
    @EnabledIfEnvironmentVariable(
            named = "TIANQUAN_JIANSHEN_IT_POSTGRES_PASSWORD_FILE", matches = ".+")
    void twoFlywaysAndBusinessOutboxWriteShareOnePhysicalTransaction()
            throws Exception {
        String baseUrl = requiredEnvironment("TIANQUAN_JIANSHEN_IT_POSTGRES_URL");
        String user = requiredEnvironment("TIANQUAN_JIANSHEN_IT_POSTGRES_USER");
        String password = Files.readString(Path.of(requiredEnvironment(
                "TIANQUAN_JIANSHEN_IT_POSTGRES_PASSWORD_FILE"))).trim();
        String schema = "rbac3_it_" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16);
        try {
            execute(baseUrl, user, password, "create schema " + schema);
            String schemaUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?")
                    + "currentSchema=" + schema;
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    schemaUrl, user, password);
            Flyway rbac3 = Rbac3FlywayConfiguration.buildRbac3Flyway(dataSource);
            Flyway outbox = Rbac3FlywayConfiguration.buildOutboxFlyway(dataSource);
            assertThat(rbac3.migrate().migrationsExecuted).isEqualTo(4);
            assertThat(outbox.migrate().migrationsExecuted).isEqualTo(1);

            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("create table rbac3_it_business_tx (id bigint primary key)");
            DataSourceTransactionManager transactionManager =
                    new DataSourceTransactionManager(dataSource);
            try (AnnotationConfigApplicationContext context = outboxContext(
                    dataSource, transactionManager)) {
                TransactionalOutbox transactionalOutbox = context.getBean(
                        TransactionalOutbox.class);
                var adapter = new TransactionalOutboxAuthorizationEventPublisher(
                        transactionalOutbox, Clock.fixed(NOW, ZoneOffset.UTC));
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);

                assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    jdbc.update("insert into rbac3_it_business_tx (id) values (1)");
                    adapter.enqueue(event());
                    throw new IllegalStateException("rollback requested");
                })).isInstanceOf(IllegalStateException.class);
                assertThat(count(jdbc, "rbac3_it_business_tx")).isZero();
                assertThat(count(jdbc, "egon_cola_outbox_message")).isZero();

                transaction.executeWithoutResult(status -> {
                    jdbc.update("insert into rbac3_it_business_tx (id) values (2)");
                    adapter.enqueue(event());
                });
                assertThat(count(jdbc, "rbac3_it_business_tx")).isEqualTo(1);
                assertThat(count(jdbc, "egon_cola_outbox_message")).isEqualTo(1);
                assertThat(count(jdbc, "flyway_schema_history_rbac3")).isPositive();
                assertThat(count(jdbc, "flyway_schema_history_outbox")).isPositive();
            }
        } finally {
            execute(baseUrl, user, password,
                    "drop schema if exists " + schema + " cascade");
        }
    }

    private AnnotationConfigApplicationContext outboxContext(
            DriverManagerDataSource dataSource,
            DataSourceTransactionManager transactionManager) {
        var context = new AnnotationConfigApplicationContext();
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("egon.cola.component.transactional-outbox.enabled", "true");
        properties.put("egon.cola.component.transactional-outbox.node-id", "tianquan-jianshen-it");
        properties.put("egon.cola.component.transactional-outbox.polling.enabled", "false");
        properties.put("egon.cola.component.transactional-outbox.storage.validate-schema", "true");
        properties.put("egon.cola.component.transactional-outbox.storage.data-source-bean-name", "dataSource");
        properties.put("egon.cola.component.transactional-outbox.storage.transaction-manager-bean-name", "transactionManager");
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("tianquan-jianshen-outbox-it", properties));
        context.registerBean("dataSource", javax.sql.DataSource.class, () -> dataSource);
        context.registerBean("transactionManager",
                org.springframework.transaction.PlatformTransactionManager.class,
                () -> transactionManager);
        context.registerBean(ObjectMapper.class,
                () -> new ObjectMapper().findAndRegisterModules());
        context.registerBean(Rbac3RuntimeProjectionDeliveryHandler.class,
                () -> new Rbac3RuntimeProjectionDeliveryHandler(
                        envelope -> Rbac3RuntimeProjectionDeliveryHandlerProjectionOutcomeEnum.APPLIED));
        context.register(TransactionalOutboxAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private long count(JdbcTemplate jdbc, String table) {
        Long value = jdbc.queryForObject("select count(*) from " + table, Long.class);
        return value == null ? 0L : value;
    }

    private void execute(
            String url, String user, String password, String sql) throws Exception {
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value.trim();
    }

    private AuthorizationEventVO event() {
        return new AuthorizationEventVO(
                "7", "USER", "99", "TIANQUAN_JIANSHEN_USER_ACTIVE_ROLES_REPLACED",
                Map.of("mutationId", "700", "contextVersion", "4"), "trace-1");
    }

    private DeliveryContext context(String destination, String payload) {
        return new DeliveryContext(
                "message-1", "tianquan-jianshen-runtime", destination, payload,
                "application/json", "1", Map.of(), "trace-1",
                1, 10, NOW.plusSeconds(30));
    }
}
