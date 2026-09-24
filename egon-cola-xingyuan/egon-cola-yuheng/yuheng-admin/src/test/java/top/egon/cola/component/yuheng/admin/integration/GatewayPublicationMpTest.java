package top.egon.cola.component.yuheng.admin.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.yuheng.admin.openapi.converter.GatewayOpenApiSnapshotPersistenceConverter;
import top.egon.cola.component.yuheng.admin.openapi.converter.GatewayOpenApiSyncPersistenceConverter;
import top.egon.cola.component.yuheng.admin.openapi.dao.GatewayOpenApiSnapshotDAO;
import top.egon.cola.component.yuheng.admin.openapi.dao.GatewayOpenApiSyncDAO;
import top.egon.cola.component.yuheng.admin.openapi.domain.bo.GatewayOpenApiSnapshotBO;
import top.egon.cola.component.yuheng.admin.openapi.domain.bo.GatewayOpenApiSyncBO;
import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSnapshotRecordPO;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSyncRecordPO;
import top.egon.cola.component.yuheng.admin.openapi.repository.impl.MpGatewayOpenApiSnapshotRepository;
import top.egon.cola.component.yuheng.admin.openapi.repository.impl.MpGatewayOpenApiSyncRepository;
import top.egon.cola.component.yuheng.admin.openapi.repository.mp.GatewayOpenApiSnapshotPersistenceRepository;
import top.egon.cola.component.yuheng.admin.openapi.repository.mp.GatewayOpenApiSyncPersistenceRepository;
import top.egon.cola.component.yuheng.admin.release.converter.GatewayReleaseAttemptPersistenceConverter;
import top.egon.cola.component.yuheng.admin.release.converter.GatewayReleasePersistenceConverter;
import top.egon.cola.component.yuheng.admin.release.converter.GatewayReleasePublicationPersistenceConverter;
import top.egon.cola.component.yuheng.admin.release.converter.GatewayReleaseTargetPersistenceConverter;
import top.egon.cola.component.yuheng.admin.release.dao.GatewayReleaseAttemptDAO;
import top.egon.cola.component.yuheng.admin.release.dao.GatewayReleaseContentDAO;
import top.egon.cola.component.yuheng.admin.release.dao.GatewayReleaseDAO;
import top.egon.cola.component.yuheng.admin.release.dao.GatewayReleasePublicationDAO;
import top.egon.cola.component.yuheng.admin.release.dao.GatewayReleaseTargetDAO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayChunkCleanupCandidateBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseAttemptBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleasePublicationBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseTargetBO;
import top.egon.cola.component.yuheng.admin.release.domain.dto.GatewayPublicationScopeDTO;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationPhaseEnum;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseAttemptRecordPO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseContentPO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleasePublicationRecordPO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseRecordPO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetRecordPO;
import top.egon.cola.component.yuheng.admin.release.repository.impl.MpGatewayReleasePublicationRepository;
import top.egon.cola.component.yuheng.admin.release.repository.impl.MpGatewayReleaseRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleaseAttemptPersistenceRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleaseContentPersistenceRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleasePersistenceRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleasePublicationPersistenceRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleaseTargetPersistenceRepository;
import top.egon.cola.component.yuheng.admin.routing.dao.GatewayDraftDAO;
import top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayDraftRecordPO;
import top.egon.cola.component.yuheng.admin.routing.repository.mp.GatewayDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.rule.domain.vo.CompiledGatewayRelease;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleChunkRef;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleContent;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：{@code GatewayPublicationMpTest} 固定 Step 6 的发布/恢复journal 合同：
 * {@code upsertDiscovered} 只回写提供方观察列，既有 journal 行的生命周期状态与发布业务 {@code revision}
 * 必须原样保留；重复的活跃业务键（快照四列契约、同步三列唯一键）必须解析为库里既有的意图而不是伪造新建成功；
 * 影响 0 行的 CAS（{@code claim}/{@code transition}/{@code setValid}/{@code markFailure}/
 * {@code linkAllToDefinitionSet}）一律不得报告成功，且旧 SQL 的谓词、状态集合、{@code NULLS FIRST} 次序与
 * {@code LIMIT} 语义要在受守卫的 MyBatis-Plus 语句上逐条可见。
 * English summary: {@code GatewayPublicationMpTest} pins the Step 6 publication/recovery journal contract:
 * {@code upsertDiscovered} rewrites only the provider observation columns so an existing journal row keeps its lifecycle
 * state and release business {@code revision}; a duplicate active business key (the four-column snapshot contract, the
 * three-column synchronization unique key) must resolve to the intent already stored instead of a fabricated insert; every
 * zero-row compare-and-set ({@code claim}/{@code transition}/{@code setValid}/{@code markFailure}/
 * {@code linkAllToDefinitionSet}) may not report success, and the legacy predicates, state sets, {@code NULLS FIRST}
 * ordering and {@code LIMIT} semantics must be visible on the guarded MyBatis-Plus statements one by one.
 *
 * 用法 / Usage: 与 Step 5 的 {@code GatewayCatalogMpTest} 同构（starter 的 {@code EgonColaRepositoryTest} 与
 * {@code architecture/AiMpRepositoryContractTest} 的写法）：mock 两张表的 DAO、装配真实受守卫仓储与真实 MapStruct
 * 转换器、以 MDC 提供受信租户 9001 并用固定 Clock 驱动时间，断言捕获到的 SQL 与写回实体；不启动 Docker、数据库或
 * Spring 上下文（真实 PostgreSQL 验收属于后续 Step）。/ Runs exactly like the Step 5 twin
 * {@code GatewayCatalogMpTest} (itself modelled on the starter's {@code EgonColaRepositoryTest} and
 * {@code architecture/AiMpRepositoryContractTest}): the two table DAOs are mocked, the real guarded repositories and the
 * real MapStruct converters are assembled by hand, MDC supplies the trusted tenant 9001 and a fixed Clock drives time, and
 * the assertions target the captured SQL plus the written entities; no Docker, database or Spring context is started (real
 * PostgreSQL execution belongs to a later Step).
 */
class GatewayPublicationMpTest {

    /** 中文说明：计划的受信租户；所有正向读取与写入都在该租户上下文内发生。 English summary: the plan's trusted tenant; every positive read and write happens inside this tenant context. */
    private static final long TRUSTED_TENANT = 9001L;

    /** 中文说明：第二租户上下文，用于负向用例。 English summary: the second tenant context used by the negative case. */
    private static final long FOREIGN_TENANT = 9002L;

    /** 中文说明：受控时钟，保证 journal 时间列断言可重复。 English summary: the controlled clock that keeps the journal timestamp assertions repeatable. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-22T16:30:00Z"), ZoneOffset.UTC);

    /** 中文说明：受控时钟的当前时刻。 English summary: the controlled clock's current instant. */
    private static final Instant NOW = CLOCK.instant();

    /** 中文说明：快照行的技术主键（迁移后为 BIGINT，端口上以十进制文本暴露）。 English summary: the snapshot row's technical primary key (BIGINT after migration, exposed as decimal text on the port). */
    private static final long SNAPSHOT_ID = 9101L;

    /** 中文说明：同步 journal 行的技术主键。 English summary: the synchronization journal row's technical primary key. */
    private static final long SYNC_ID = 9201L;

    /** 中文说明：归属应用标识。 English summary: the owning application identifier. */
    private static final long APPLICATION_ID = 7001L;

    /** 中文说明：聚合 Definition Set 标识。 English summary: the aggregate Definition Set identifier. */
    private static final long DEFINITION_SET_ID = 9001L;

    /** 中文说明：另一聚合 Definition Set 标识，用于冲突用例。 English summary: a foreign aggregate Definition Set identifier used by the conflict case. */
    private static final long FOREIGN_DEFINITION_SET_ID = 9002L;

    /** 中文说明：不可变提供方构建标识。 English summary: the immutable provider build identifier. */
    private static final String BUILD_ID = "sha256:build-0001";

    /** 中文说明：来源 Group。 English summary: the source Group. */
    private static final String GROUP = "yuheng-core";

    /** 中文说明：第二个 Group，用于 {@code IN} 集合断言。 English summary: the second Group used for the {@code IN} collection assertion. */
    private static final String OTHER_GROUP = "payments";

    /** 中文说明：提供方制品版本。 English summary: the provider artifact version. */
    private static final String ARTIFACT = "1.4.0";

    /** 中文说明：新观察到的制品版本，用于 journal 观察列回写断言。 English summary: the newly observed artifact version used by the observation-write-back assertions. */
    private static final String OBSERVED_ARTIFACT = "1.5.0";

    /** 中文说明：文档原文哈希（64 位小写十六进制）。 English summary: the raw document hash (64 lowercase hex characters). */
    private static final String DOCUMENT_SHA = "a".repeat(64);

    /** 中文说明：规范文档哈希，同时是快照业务契约的第四列。 English summary: the canonical document hash, also the fourth column of the snapshot business contract. */
    private static final String CANONICAL_SHA = "b".repeat(64);

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 中文说明：快照的结构化文档载体。 English summary: the structured document carrier of a snapshot. */
    private static final Map<String, Object> DOCUMENT = Map.of(
            "openapi", "3.1.0",
            "info", Map.of("title", "Yuheng"));

    /** 中文说明：{@code document_json} 列值。 English summary: the {@code document_json} column value. */
    private static final JsonNode DOCUMENT_NODE = JSON.valueToTree(DOCUMENT);

    /** 中文说明：{@code validation_messages} 列值（空数组，非 SQL NULL）。 English summary: the {@code validation_messages} column value (an empty array, never SQL NULL). */
    private static final JsonNode MESSAGES_NODE = JSON.valueToTree(List.of());

    /** 中文说明：发布头的不透明编号（迁移后为 BIGINT）。 English summary: the release head's opaque identifier (BIGINT after migration). */
    private static final long RELEASE_ID = 9101L;

    /** 中文说明：同组后继发布编号，用于分块回收的后继判定。 English summary: the successor release identifier of the same Group used by the cleanup decision. */
    private static final long SUCCESSOR_RELEASE_ID = 9102L;

    /** 中文说明：发布所属网关组。 English summary: the gateway group the release belongs to. */
    private static final long GATEWAY_GROUP_ID = 8001L;

    /** 中文说明：尝试日志行的技术主键。 English summary: the technical primary key of an attempt journal row. */
    private static final long ATTEMPT_ID = 9301L;

    /** 中文说明：实例目标观测行的技术主键。 English summary: the technical primary key of an instance target observation row. */
    private static final long TARGET_ID = 9401L;

    /** 中文说明：发布日志行的技术主键。 English summary: the technical primary key of a publication journal row. */
    private static final long PUBLICATION_ID = 9501L;

    /** 中文说明：第二租户下等价发布日志行的技术主键，用于跨租户负向用例。 English summary: the technical primary key of the equivalent journal row of the second tenant, used by the cross-tenant negative case. */
    private static final long FOREIGN_PUBLICATION_ID = 9503L;

    /** 中文说明：规则内容摘要，与制品摘要一起冻结在内容快照列上。 English summary: the rule content digest, frozen next to the artifact digest on the content snapshot columns. */
    private static final String RULE_SHA = "c".repeat(64);

    /** 中文说明：编译制品摘要，同时是实例目标回写的 {@code applied_artifact_sha256}。 English summary: the compiled artifact digest, also the {@code applied_artifact_sha256} written back on the instance targets. */
    private static final String ARTIFACT_SHA = "d".repeat(64);

    /** 中文说明：分块内容摘要。 English summary: the checksum of one chunk value. */
    private static final String CHUNK_SHA = "e".repeat(64);

    /** 中文说明：外部引擎变更号。 English summary: the external engine change identifier. */
    private static final String CHANGE_ID = "chg-9101-1";

    /** 中文说明：分块配置键。 English summary: the configuration key of one chunk. */
    private static final String CHUNK_KEY = "yuheng.rules.chunk.0";

    /** 中文说明：激活配置键（与 {@code GatewayDdcRulePublisher.ACTIVE_CONFIG_KEY} 一致）。 English summary: the activation configuration key (as {@code GatewayDdcRulePublisher.ACTIVE_CONFIG_KEY}). */
    private static final String ACTIVE_KEY = "yuheng.rules.active";

    /** 中文说明：分块正文。 English summary: the chunk payload. */
    private static final String CHUNK_VALUE = "operations:\n  - operationId: op-1\n";

    /** 中文说明：发布目标的应用码。 English summary: the application code of a publication target. */
    private static final String APP_CODE = "xingyuan";

    /** 中文说明：发布目标的环境。 English summary: the environment of a publication target. */
    private static final String ENV = "prod";

    /** 中文说明：发布目标的业务码。 English summary: the business code of a publication target. */
    private static final String BIZ_CODE = "yuheng-biz";

    /** 中文说明：快照命名空间，也是内容快照 JSON 路径的投影值。 English summary: the snapshot namespace, also the value of the content snapshot JSON path projection. */
    private static final String NAMESPACE = "yuheng-core-runtime";

    /** 中文说明：{@code canonical_snapshot} 列的结构化值。 English summary: the structured value of the {@code canonical_snapshot} column. */
    private static final Map<String, Object> SNAPSHOT_DOCUMENT = Map.of(
            "releaseId", Long.toString(RELEASE_ID),
            "content", Map.of("namespace", NAMESPACE, "env", ENV));

    /** 中文说明：编译产物里与 {@link #SNAPSHOT_DOCUMENT} 等价的 JSON 文本。 English summary: the JSON text of the compiled artifact, equivalent to {@link #SNAPSHOT_DOCUMENT}. */
    private static final String SNAPSHOT_JSON =
            "{\"releaseId\":\"9101\",\"content\":{\"namespace\":\"yuheng-core-runtime\",\"env\":\"prod\"}}";

    /** 中文说明：{@code activation_content} 列的结构化值。 English summary: the structured value of the {@code activation_content} column. */
    private static final Map<String, Object> ACTIVATION_DOCUMENT = Map.of(
            "mode", "CHUNKED",
            "totalSize", 41);

    /** 中文说明：编译产物里与 {@link #ACTIVATION_DOCUMENT} 等价的 JSON 文本。 English summary: the JSON text of the compiled artifact, equivalent to {@link #ACTIVATION_DOCUMENT}. */
    private static final String ACTIVATION_JSON = "{\"mode\":\"CHUNKED\",\"totalSize\":41}";

    /** 中文说明：发布头的业务创建时刻。 English summary: the business creation instant of the release head. */
    private static final Instant RELEASE_CREATED_AT = NOW.minus(Duration.ofMinutes(6));

    private static final ValidatorFactory VALIDATORS =
            Validation.buildDefaultValidatorFactory();

    private GatewayOpenApiSnapshotDAO snapshotDAO;
    private GatewayOpenApiSyncDAO syncDAO;

    private MpGatewayOpenApiSnapshotRepository snapshots;
    private MpGatewayOpenApiSyncRepository syncStates;

    private GatewayReleaseDAO releaseDAO;
    private GatewayReleaseContentDAO releaseContentDAO;
    private GatewayReleaseAttemptDAO releaseAttemptDAO;
    private GatewayReleaseTargetDAO releaseTargetDAO;
    private GatewayReleasePublicationDAO releasePublicationDAO;
    private GatewayDraftDAO draftDAO;

    private MpGatewayReleaseRepository releases;
    private MpGatewayReleasePublicationRepository publications;

    /**
     * 中文说明：把 starter 的业务校验入口绑定到 jakarta 校验器，缺失时受守卫仓储会在写入前失败。
     * English summary: Binds the starter's business validation entry to a jakarta validator; without it the guarded
     * repository fails before any write.
     */
    @BeforeAll
    static void bindModelValidation() {
        if (EgonColaModelValidationUtils.current() == null) {
            EgonColaModelValidationUtils.initialize(
                    new ValidationUtils(VALIDATORS.getValidator()),
                    "GatewayPublicationMpTest"
            );
        }
    }

    /**
     * 中文说明：注册两张迁移表的 MP 行模型（列缓存）、装配真实受守卫仓储与门面，并放入受信租户上下文。
     * English summary: Registers the two migrated tables' MyBatis-Plus row models (the column cache), assembles the real
     * guarded repositories and facades, and installs the trusted tenant context.
     */
    @BeforeEach
    void assembleGuardedFacades() {
        registerRowModel(GatewayOpenApiSnapshotRecordPO.class);
        registerRowModel(GatewayOpenApiSyncRecordPO.class);
        snapshotDAO = mock(GatewayOpenApiSnapshotDAO.class);
        syncDAO = mock(GatewayOpenApiSyncDAO.class);
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        snapshots = new MpGatewayOpenApiSnapshotRepository(
                new GatewayOpenApiSnapshotPersistenceRepository(snapshotDAO, properties),
                new GatewayOpenApiSnapshotPersistenceConverter()
        );
        syncStates = new MpGatewayOpenApiSyncRepository(
                new GatewayOpenApiSyncPersistenceRepository(syncDAO, properties),
                new GatewayOpenApiSyncPersistenceConverter()
        );
        registerRowModel(GatewayReleaseRecordPO.class);
        registerRowModel(GatewayReleaseContentPO.class);
        registerRowModel(GatewayReleaseAttemptRecordPO.class);
        registerRowModel(GatewayReleaseTargetRecordPO.class);
        registerRowModel(GatewayReleasePublicationRecordPO.class);
        registerRowModel(GatewayDraftRecordPO.class);
        releaseDAO = mock(GatewayReleaseDAO.class);
        releaseContentDAO = mock(GatewayReleaseContentDAO.class);
        releaseAttemptDAO = mock(GatewayReleaseAttemptDAO.class);
        releaseTargetDAO = mock(GatewayReleaseTargetDAO.class);
        releasePublicationDAO = mock(GatewayReleasePublicationDAO.class);
        draftDAO = mock(GatewayDraftDAO.class);
        releases = new MpGatewayReleaseRepository(
                new GatewayReleasePersistenceRepository(releaseDAO, properties),
                new GatewayReleaseContentPersistenceRepository(releaseContentDAO, properties),
                new GatewayReleaseAttemptPersistenceRepository(releaseAttemptDAO, properties),
                new GatewayReleaseTargetPersistenceRepository(releaseTargetDAO, properties),
                new GatewayReleasePublicationPersistenceRepository(releasePublicationDAO, properties),
                new GatewayReleasePersistenceConverter(),
                new GatewayReleaseAttemptPersistenceConverter(),
                new GatewayReleaseTargetPersistenceConverter(),
                JSON
        );
        publications = new MpGatewayReleasePublicationRepository(
                new GatewayReleasePublicationPersistenceRepository(releasePublicationDAO, properties),
                new GatewayReleasePersistenceRepository(releaseDAO, properties),
                new GatewayReleaseContentPersistenceRepository(releaseContentDAO, properties),
                new GatewayDraftPersistenceRepository(draftDAO, properties),
                new GatewayReleasePublicationPersistenceConverter()
        );
        MDC.put("tenantId", Long.toString(TRUSTED_TENANT));
    }

    /**
     * 中文说明：清理租户上下文，避免线程复用泄漏。
     * English summary: Clears the tenant context so a reused thread cannot leak it.
     */
    @AfterEach
    void clearTenantContext() {
        MDC.remove("tenantId");
    }

    /**
     * 中文说明：{@code upsertDiscovered} 命中既有 journal 行时，只允许回写旧 {@code DO UPDATE SET} 列出的四列提供方
     * 观察值与时间戳；行状态、尝试计数、链接与发布业务 {@code revision} 必须逐列保持不变，返回值取自库内权威重读而
     * 不是入参，插入语句一次都不能出现。
     * English summary: When {@code upsertDiscovered} hits an existing journal row only the four provider observation
     * columns named by the legacy {@code DO UPDATE SET} plus the timestamp may be rewritten; the row's state, attempt
     * counter, links and release business {@code revision} must stay identical column by column, the value returned comes
     * from the authoritative reload rather than the argument, and no insert statement may appear at all.
     */
    @Test
    @DisplayName("journal rows keep their lifecycle state and release business revision")
    void journalObservationKeepsLifecycleStateAndBusinessRevision() {
        GatewayOpenApiSyncRecordPO stored = journalRow(
                SYNC_ID, GatewayOpenApiSyncStateEnum.VALID, 9L, 3, null, 11L);
        stored.setLatestSnapshotId(SNAPSHOT_ID);
        stored.setDefinitionSetId(DEFINITION_SET_ID);
        GatewayOpenApiSyncRecordPO authority = journalRow(
                SYNC_ID, GatewayOpenApiSyncStateEnum.VALID, 9L, 3, null, 11L);
        authority.setLatestSnapshotId(SNAPSHOT_ID);
        authority.setDefinitionSetId(DEFINITION_SET_ID);
        authority.setArtifactVersion(OBSERVED_ARTIFACT);
        authority.setProviderServiceName("yuheng-provider");
        when(syncDAO.selectList(any())).thenReturn(List.of(stored), List.of(authority));
        when(syncDAO.updateById(any(GatewayOpenApiSyncRecordPO.class))).thenReturn(1);

        GatewayOpenApiSyncBO current = syncStates.upsertDiscovered(observation(0L));

        ArgumentCaptor<GatewayOpenApiSyncRecordPO> written =
                ArgumentCaptor.forClass(GatewayOpenApiSyncRecordPO.class);
        verify(syncDAO).updateById(written.capture());
        GatewayOpenApiSyncRecordPO journal = written.getValue();
        assertThat(journal.getId()).isEqualTo(SYNC_ID);
        assertThat(journal.getVersion())
                .as("the read row's technical version is reused as the optimistic-lock token")
                .isEqualTo(11L);
        assertThat(journal.getStatus())
                .as("a discovery never downgrades the journal state")
                .isEqualTo(GatewayOpenApiSyncStateEnum.VALID.wireValue());
        assertThat(journal.getRevision())
                .as("the release business revision is carried over untouched")
                .isEqualTo(9L);
        assertThat(journal.getAttemptCount()).isEqualTo(3);
        assertThat(journal.getLatestSnapshotId()).isEqualTo(SNAPSHOT_ID);
        assertThat(journal.getDefinitionSetId()).isEqualTo(DEFINITION_SET_ID);
        assertThat(journal.getArtifactVersion()).isEqualTo(OBSERVED_ARTIFACT);
        assertThat(journal.getProviderServiceName()).isEqualTo("yuheng-provider");
        assertThat(journal.getUpdateTime())
                .as("the legacy updated_at write became the boundary's update_time")
                .isEqualTo(NOW);
        assertThat(current.getStatus()).isEqualTo(GatewayOpenApiSyncStateEnum.VALID);
        assertThat(current.getRevision())
                .as("the authoritative revision is reported back, not the caller's zero")
                .isEqualTo(9L);
        assertThat(current.getArtifactVersion()).isEqualTo(OBSERVED_ARTIFACT);
        verify(syncDAO, never()).insert(any(GatewayOpenApiSyncRecordPO.class));
        verify(syncDAO, times(2)).selectList(any());
    }

    /**
     * 中文说明：全新发现按受守卫插入落库，只带业务列（租户、审计、软删与 {@code version} 留给边界补齐），
     * 并且结果一律来自按业务键的权威重读；影响 0 行的插入与重读不到行都不得被报告为成功。
     * English summary: A fresh discovery goes through the guarded insert carrying business columns only (tenant, audit,
     * soft delete and {@code version} belong to the boundary), and the result always comes from the reload by business
     * key; neither a zero-row insert nor a missing reload may be reported as success.
     */
    @Test
    @DisplayName("a fresh discovery inserts business columns only and re-reads the journal row")
    void freshDiscoveryInsertsBusinessColumnsAndReReadsTheJournalRow() {
        GatewayOpenApiSyncRecordPO persisted = journalRow(
                SYNC_ID, GatewayOpenApiSyncStateEnum.DISCOVERED, 0L, 0, null, 0L);
        when(syncDAO.selectList(any())).thenReturn(List.of(), List.of(persisted));
        when(syncDAO.insert(any(GatewayOpenApiSyncRecordPO.class))).thenReturn(1);

        GatewayOpenApiSyncBO created = syncStates.upsertDiscovered(observation(0L));

        ArgumentCaptor<GatewayOpenApiSyncRecordPO> inserted =
                ArgumentCaptor.forClass(GatewayOpenApiSyncRecordPO.class);
        verify(syncDAO).insert(inserted.capture());
        assertThat(inserted.getValue().getId()).isEqualTo(SYNC_ID);
        assertThat(inserted.getValue().getApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(inserted.getValue().getBuildId()).isEqualTo(BUILD_ID);
        assertThat(inserted.getValue().getOpenapiGroup()).isEqualTo(GROUP);
        assertThat(inserted.getValue().getStatus())
                .isEqualTo(GatewayOpenApiSyncStateEnum.DISCOVERED.wireValue());
        assertThat(inserted.getValue().getRevision()).isEqualTo(0L);
        assertThat(inserted.getValue().getArtifactVersion()).isEqualTo(OBSERVED_ARTIFACT);
        assertThat(inserted.getValue().getTenantId())
                .as("the tenant column is owned by the guarded boundary, not the facade")
                .isNull();
        assertThat(inserted.getValue().getVersion()).isNull();
        assertThat(inserted.getValue().getCreateTime()).isNull();
        assertThat(created.getStatus()).isEqualTo(GatewayOpenApiSyncStateEnum.DISCOVERED);
        assertThat(created.getId()).isEqualTo(Long.toString(SYNC_ID));

        clearInvocations(syncDAO);
        when(syncDAO.selectList(any())).thenReturn(List.of(), List.of());
        when(syncDAO.insert(any(GatewayOpenApiSyncRecordPO.class))).thenReturn(0);
        assertThatThrownBy(() -> syncStates.upsertDiscovered(observation(0L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OpenAPI sync row disappeared after upsert");
    }

    /**
     * 中文说明：{@code upsertDiscovered} 的前置条件与非 DISCOVERED 载体都在触达持久边界之前按旧实现的异常抛出，
     * 被拒绝的调用不得触碰任何语句。
     * English summary: The precondition of {@code upsertDiscovered} and a non-DISCOVERED carrier are rejected with the
     * original exceptions before persistence is touched, and a rejected call may not reach any statement.
     */
    @Test
    @DisplayName("a non-discovered carrier is rejected before the journal is touched")
    void nonDiscoveredCarrierIsRejectedBeforeTouchingTheJournal() {
        GatewayOpenApiSyncBO fetched = observation(0L)
                .setStatus(GatewayOpenApiSyncStateEnum.FETCHING);

        assertThatThrownBy(() -> syncStates.upsertDiscovered(fetched))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("upsertDiscovered requires DISCOVERED state");
        assertThatThrownBy(() -> syncStates.upsertDiscovered(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("state");
        verify(syncDAO, never()).selectList(any());
        verify(syncDAO, never()).insert(any(GatewayOpenApiSyncRecordPO.class));
        verify(syncDAO, never()).updateById(any(GatewayOpenApiSyncRecordPO.class));
    }

    /**
     * 中文说明：快照四列业务契约上的重复键与影响 0 行的插入都进入“复用既有意图”分支：按旧
     * {@code findByContract} 重读并逐字段复核不可变契约后返回库里的快照，绝不伪造新建成功。
     * English summary: Both a duplicate key on the four-column snapshot contract and a zero-row insert enter the
     * “reuse the existing intent” branch: the row is reloaded through the legacy {@code findByContract} shape, the
     * immutable contract is re-verified field by field and the stored snapshot is returned instead of a fake insert.
     */
    @Test
    @DisplayName("a duplicate active snapshot key resolves to the stored intent")
    @SuppressWarnings("unchecked")
    void duplicateActiveSnapshotKeyResolvesToTheStoredIntent() {
        when(snapshotDAO.insert(any(GatewayOpenApiSnapshotRecordPO.class)))
                .thenThrow(new DuplicateKeyException(
                        "duplicate key value violates unique constraint \"uk_gateway_openapi_snapshot_contract\""
                ));
        when(snapshotDAO.selectList(any())).thenReturn(
                List.of(snapshotRow(TRUSTED_TENANT, DEFINITION_SET_ID, 11L))
        );

        GatewayOpenApiSnapshotBO reused = snapshots.insertOrReuse(snapshotCarrier(DEFINITION_SET_ID));

        ArgumentCaptor<LambdaQueryWrapper<GatewayOpenApiSnapshotRecordPO>> contract =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(snapshotDAO).selectList(contract.capture());
        assertThat(contract.getValue().getTargetSql())
                .contains("application_id = ?")
                .contains("build_id = ?")
                .contains("openapi_group = ?")
                .contains("canonical_sha256 = ?");
        assertThat(contract.getValue().getTargetSql())
                .as("the contract reload is a plain four-column lookup")
                .doesNotContain("ORDER BY");
        assertThat(reused.getId()).isEqualTo(Long.toString(SNAPSHOT_ID));
        assertThat(reused.getArtifactVersion()).isEqualTo(ARTIFACT);
        assertThat(reused.getDefinitionSetId())
                .as("the linked aggregate the caller did not create is reported back")
                .isEqualTo(Long.toString(DEFINITION_SET_ID));
        verify(snapshotDAO, times(1)).insert(any(GatewayOpenApiSnapshotRecordPO.class));

        clearInvocations(snapshotDAO);
        when(snapshotDAO.insert(any(GatewayOpenApiSnapshotRecordPO.class))).thenReturn(0);
        when(snapshotDAO.selectList(any())).thenReturn(
                List.of(snapshotRow(TRUSTED_TENANT, DEFINITION_SET_ID, 11L))
        );
        assertThat(snapshots.insertOrReuse(snapshotCarrier(DEFINITION_SET_ID)).getId())
                .as("a zero-row insert resolves to the stored snapshot as well")
                .isEqualTo(Long.toString(SNAPSHOT_ID));
    }

    /**
     * 中文说明：既有行的不可变契约与入参不一致时按旧实现抛出
     * {@code YUHENG_OPENAPI_SNAPSHOT_CONFLICT: immutable contract ...}；重读不到业务键时抛出
     * {@code ... snapshot identity ... could not be resolved}；两者都发生在一次插入尝试之后且绝不返回成功载体。
     * English summary: A stored row whose immutable contract differs from the argument raises the legacy
     * {@code YUHENG_OPENAPI_SNAPSHOT_CONFLICT: immutable contract ...}, and a business key that cannot be reloaded raises
     * {@code ... snapshot identity ... could not be resolved}; both happen after one insert attempt and neither returns a
     * successful carrier.
     */
    @Test
    @DisplayName("a drifted or unresolvable snapshot contract conflicts instead of succeeding")
    void driftedOrUnresolvableSnapshotContractConflicts() {
        GatewayOpenApiSnapshotRecordPO drifted = snapshotRow(TRUSTED_TENANT, DEFINITION_SET_ID, 11L);
        drifted.setArtifactVersion("9.9.9");
        when(snapshotDAO.insert(any(GatewayOpenApiSnapshotRecordPO.class))).thenReturn(0);
        when(snapshotDAO.selectList(any())).thenReturn(List.of(drifted));

        assertThatThrownBy(() -> snapshots.insertOrReuse(snapshotCarrier(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_OPENAPI_SNAPSHOT_CONFLICT: immutable contract "
                        + APPLICATION_ID + "/" + BUILD_ID + "/" + GROUP);

        clearInvocations(snapshotDAO);
        when(snapshotDAO.selectList(any())).thenReturn(List.of());
        assertThatThrownBy(() -> snapshots.insertOrReuse(snapshotCarrier(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_OPENAPI_SNAPSHOT_CONFLICT: snapshot identity "
                        + SNAPSHOT_ID + " could not be resolved");

        clearInvocations(snapshotDAO);
        GatewayOpenApiSnapshotBO incomplete = snapshotCarrier(null).setArtifactVersion("  ");
        assertThatThrownBy(() -> snapshots.insertOrReuse(incomplete))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("artifactVersion must not be blank");
        verify(snapshotDAO, never()).insert(any(GatewayOpenApiSnapshotRecordPO.class));
    }

    /**
     * 中文说明：{@code claim} 与旧 SQL 一样在同一语句里落认领状态、尝试计数加一、{@code last_attempt_at} 与三列清空
     * （{@code next_retry_at}/{@code last_error_code}/{@code last_error_message}），并只在业务 {@code revision} 与可认领
     * 状态、重试窗口同时匹配时把 {@code revision} 自增；行缺失、影响 0 行与负修订分别返回 {@code false}、
     * {@code false} 与旧的 {@code IllegalArgumentException}。
     * English summary: {@code claim} stores the claimed state, the incremented attempt counter and {@code last_attempt_at}
     * while clearing {@code next_retry_at}/{@code last_error_code}/{@code last_error_message} in the same statement, and
     * it only increments the business {@code revision} when the revision, a claimable state and the retry window all match;
     * a missing row, a zero-row effect and a negative revision return {@code false}, {@code false} and the legacy
     * {@code IllegalArgumentException} respectively.
     */
    @Test
    @DisplayName("claim writes the recovery journal only when the revision still matches")
    @SuppressWarnings("unchecked")
    void claimWritesTheRecoveryJournalOnlyOnARevisionHit() {
        when(syncDAO.selectActiveById(SYNC_ID)).thenReturn(
                journalRow(SYNC_ID, GatewayOpenApiSyncStateEnum.DISCOVERED, 3L, 2, null, 11L)
        );
        when(syncDAO.update(any(GatewayOpenApiSyncRecordPO.class), any())).thenReturn(1);

        assertThat(syncStates.claim(Long.toString(SYNC_ID), 3L, NOW)).isTrue();

        ArgumentCaptor<GatewayOpenApiSyncRecordPO> claimed =
                ArgumentCaptor.forClass(GatewayOpenApiSyncRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayOpenApiSyncRecordPO>> predicate =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(syncDAO).update(claimed.capture(), predicate.capture());
        GatewayOpenApiSyncRecordPO journal = claimed.getValue();
        assertThat(journal.getId()).isEqualTo(SYNC_ID);
        assertThat(journal.getVersion())
                .as("the read version is the optimistic-lock token of the CAS")
                .isEqualTo(11L);
        assertThat(journal.getStatus())
                .isEqualTo(GatewayOpenApiSyncStateEnum.FETCHING.wireValue());
        assertThat(journal.getAttemptCount())
                .as("attempt_count = attempt_count + 1 of the legacy UPDATE")
                .isEqualTo(3);
        assertThat(journal.getRevision())
                .as("the business revision only moves on a hit")
                .isEqualTo(4L);
        assertThat(journal.getLastAttemptAt()).isEqualTo(NOW);
        assertThat(journal.getNextRetryAt())
                .as("the column is cleared through the SET fragment, not the entity")
                .isNull();
        assertThat(predicate.getValue().getTargetSql())
                .contains("id = ?")
                .contains("revision = ?")
                .contains("status IN")
                .contains("next_retry_at IS NULL")
                .contains("next_retry_at <= ?");
        assertThat(predicate.getValue().getParamNameValuePairs().values())
                .contains("DISCOVERED", "FETCH_FAILED", "INGEST_FAILED", "STALE");
        assertThat(predicate.getValue().getSqlSet())
                .contains("next_retry_at=")
                .contains("last_error_code=")
                .contains("last_error_message=");

        clearInvocations(syncDAO);
        when(syncDAO.update(any(GatewayOpenApiSyncRecordPO.class), any())).thenReturn(0);
        assertThat(syncStates.claim(Long.toString(SYNC_ID), 3L, NOW))
                .as("a zero-row compare-and-set is never a successful claim")
                .isFalse();

        clearInvocations(syncDAO);
        when(syncDAO.selectActiveById(SYNC_ID)).thenReturn(null);
        assertThat(syncStates.claim(Long.toString(SYNC_ID), 3L, NOW)).isFalse();
        verify(syncDAO, never()).update(any(GatewayOpenApiSyncRecordPO.class), any());

        clearInvocations(syncDAO);
        assertThatThrownBy(() -> syncStates.claim(Long.toString(SYNC_ID), -1L, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("revision must not be negative");
        verify(syncDAO, never()).selectActiveById(any(Serializable.class));
    }

    /**
     * 中文说明：{@code transition} 与 {@code setValid} 保留旧 SQL 的 {@code WHERE id = ? AND revision = ? AND status = ?}
     * 三元 CAS；非法状态迁移在触达持久边界前抛出旧消息，{@code setValid} 额外清空重试与错误三列并在 0 行时返回
     * {@code false}。
     * English summary: {@code transition} and {@code setValid} keep the legacy three-part compare-and-set
     * {@code WHERE id = ? AND revision = ? AND status = ?}; an illegal move raises the original message before persistence
     * is touched, and {@code setValid} additionally clears the retry and error columns while returning {@code false} for a
     * zero-row effect.
     */
    @Test
    @DisplayName("state machine compare-and-set mirrors the legacy journal predicates")
    @SuppressWarnings("unchecked")
    void stateMachineCompareAndSetMirrorsTheLegacyPredicates() {
        when(syncDAO.selectActiveById(SYNC_ID)).thenReturn(
                journalRow(SYNC_ID, GatewayOpenApiSyncStateEnum.FETCHING, 3L, 1, null, 11L)
        );
        when(syncDAO.update(any(GatewayOpenApiSyncRecordPO.class), any())).thenReturn(1);

        assertThat(syncStates.transition(
                Long.toString(SYNC_ID),
                3L,
                GatewayOpenApiSyncStateEnum.FETCHING,
                GatewayOpenApiSyncStateEnum.VALIDATING,
                NOW
        )).isTrue();

        ArgumentCaptor<GatewayOpenApiSyncRecordPO> moved =
                ArgumentCaptor.forClass(GatewayOpenApiSyncRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayOpenApiSyncRecordPO>> movePredicate =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(syncDAO).update(moved.capture(), movePredicate.capture());
        assertThat(moved.getValue().getStatus())
                .isEqualTo(GatewayOpenApiSyncStateEnum.VALIDATING.wireValue());
        assertThat(moved.getValue().getRevision()).isEqualTo(4L);
        assertThat(movePredicate.getValue().getTargetSql())
                .contains("id = ?")
                .contains("revision = ?")
                .contains("status = ?");
        assertThat(movePredicate.getValue().getParamNameValuePairs().values())
                .contains(GatewayOpenApiSyncStateEnum.FETCHING.wireValue());
        assertThat(movePredicate.getValue().getSqlSet())
                .as("the legacy transition SET listed no extra columns")
                .isNull();

        assertThatThrownBy(() -> syncStates.transition(
                Long.toString(SYNC_ID),
                3L,
                GatewayOpenApiSyncStateEnum.VALID,
                GatewayOpenApiSyncStateEnum.FETCHING,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("illegal OpenAPI sync transition: VALID -> FETCHING");

        clearInvocations(syncDAO);
        when(syncDAO.selectActiveById(SYNC_ID)).thenReturn(
                journalRow(SYNC_ID, GatewayOpenApiSyncStateEnum.INGESTING, 5L, 1, null, 12L)
        );
        assertThat(syncStates.setValid(
                Long.toString(SYNC_ID),
                5L,
                Long.toString(SNAPSHOT_ID),
                Long.toString(DEFINITION_SET_ID),
                NOW
        )).isTrue();
        ArgumentCaptor<GatewayOpenApiSyncRecordPO> valid =
                ArgumentCaptor.forClass(GatewayOpenApiSyncRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayOpenApiSyncRecordPO>> validPredicate =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(syncDAO).update(valid.capture(), validPredicate.capture());
        assertThat(valid.getValue().getStatus())
                .isEqualTo(GatewayOpenApiSyncStateEnum.VALID.wireValue());
        assertThat(valid.getValue().getLatestSnapshotId()).isEqualTo(SNAPSHOT_ID);
        assertThat(valid.getValue().getDefinitionSetId()).isEqualTo(DEFINITION_SET_ID);
        assertThat(valid.getValue().getLastSuccessAt()).isEqualTo(NOW);
        assertThat(valid.getValue().getRevision())
                .as("the winning CAS is the only writer of the next revision")
                .isEqualTo(6L);
        assertThat(validPredicate.getValue().getTargetSql()).contains("status = ?");
        assertThat(validPredicate.getValue().getParamNameValuePairs().values())
                .contains(GatewayOpenApiSyncStateEnum.INGESTING.wireValue());
        assertThat(validPredicate.getValue().getSqlSet())
                .contains("next_retry_at=")
                .contains("last_error_code=")
                .contains("last_error_message=");

        clearInvocations(syncDAO);
        when(syncDAO.update(any(GatewayOpenApiSyncRecordPO.class), any())).thenReturn(0);
        assertThat(syncStates.setValid(
                Long.toString(SYNC_ID),
                5L,
                Long.toString(SNAPSHOT_ID),
                Long.toString(DEFINITION_SET_ID),
                NOW
        )).as("a lost ingest CAS must not be reported as VALID").isFalse();

        assertThatThrownBy(() -> syncStates.setValid(
                Long.toString(SYNC_ID),
                5L,
                "snapshot-legacy",
                Long.toString(DEFINITION_SET_ID),
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("snapshotId must be a numeric identifier");
    }

    /**
     * 中文说明：{@code markFailure} 的允许源状态集合与旧 SQL 逐一对应（{@code FETCH_FAILED} 只接受
     * {@code FETCHING}，{@code STALE} 接受九个状态），错误详情超界与不受支持目标状态按旧消息先抛出，
     * 影响 0 行返回 {@code false}。
     * English summary: The allowed source states of {@code markFailure} mirror the legacy SQL one by one
     * ({@code FETCH_FAILED} accepts only {@code FETCHING}, {@code STALE} accepts nine states), oversized failure details
     * and an unsupported target raise the original messages first, and a zero-row effect returns {@code false}.
     */
    @Test
    @DisplayName("markFailure accepts only the states the legacy journal allowed")
    @SuppressWarnings("unchecked")
    void markFailureAcceptsOnlyTheLegacyAllowedSourceStates() {
        when(syncDAO.selectActiveById(SYNC_ID)).thenReturn(
                journalRow(SYNC_ID, GatewayOpenApiSyncStateEnum.FETCHING, 4L, 2, null, 13L)
        );
        when(syncDAO.update(any(GatewayOpenApiSyncRecordPO.class), any())).thenReturn(1);
        Instant retryAt = NOW.plus(Duration.ofMinutes(5));

        assertThat(syncStates.markFailure(
                Long.toString(SYNC_ID),
                4L,
                GatewayOpenApiSyncStateEnum.FETCH_FAILED,
                "YUHENG_OPENAPI_FETCH_FAILED",
                "provider fetch failed",
                retryAt,
                NOW
        )).isTrue();

        ArgumentCaptor<GatewayOpenApiSyncRecordPO> failed =
                ArgumentCaptor.forClass(GatewayOpenApiSyncRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayOpenApiSyncRecordPO>> predicate =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(syncDAO).update(failed.capture(), predicate.capture());
        assertThat(failed.getValue().getStatus())
                .isEqualTo(GatewayOpenApiSyncStateEnum.FETCH_FAILED.wireValue());
        assertThat(failed.getValue().getLastErrorCode()).isEqualTo("YUHENG_OPENAPI_FETCH_FAILED");
        assertThat(failed.getValue().getLastErrorMessage()).isEqualTo("provider fetch failed");
        assertThat(failed.getValue().getRevision()).isEqualTo(5L);
        assertThat(predicate.getValue().getTargetSql())
                .contains("status IN")
                .contains("id = ?")
                .contains("revision = ?");
        assertThat(predicate.getValue().getParamNameValuePairs().values())
                .contains(GatewayOpenApiSyncStateEnum.FETCHING.wireValue());
        assertThat(predicate.getValue().getSqlSet()).contains("next_retry_at=");

        clearInvocations(syncDAO);
        when(syncDAO.selectActiveById(SYNC_ID)).thenReturn(
                journalRow(SYNC_ID, GatewayOpenApiSyncStateEnum.VALID, 7L, 0, null, 14L)
        );
        assertThat(syncStates.markFailure(
                Long.toString(SYNC_ID),
                7L,
                GatewayOpenApiSyncStateEnum.STALE,
                "YUHENG_OPENAPI_STALE",
                "provider observation is no longer current",
                null,
                NOW
        )).isTrue();
        ArgumentCaptor<LambdaUpdateWrapper<GatewayOpenApiSyncRecordPO>> stale =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(syncDAO).update(any(GatewayOpenApiSyncRecordPO.class), stale.capture());
        assertThat(stale.getValue().getParamNameValuePairs().values())
                .contains(
                        GatewayOpenApiSyncStateEnum.DISCOVERED.wireValue(),
                        GatewayOpenApiSyncStateEnum.FETCHING.wireValue(),
                        GatewayOpenApiSyncStateEnum.VALIDATING.wireValue(),
                        GatewayOpenApiSyncStateEnum.VALID.wireValue(),
                        GatewayOpenApiSyncStateEnum.INGEST_FAILED.wireValue())
                .doesNotContain(GatewayOpenApiSyncStateEnum.STALE.wireValue());

        assertThatThrownBy(() -> syncStates.markFailure(
                Long.toString(SYNC_ID),
                7L,
                GatewayOpenApiSyncStateEnum.VALID,
                "YUHENG_OPENAPI_STALE",
                "message",
                null,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported OpenAPI sync failure state: VALID");
        assertThatThrownBy(() -> syncStates.markFailure(
                Long.toString(SYNC_ID),
                7L,
                GatewayOpenApiSyncStateEnum.STALE,
                "YUHENG_OPENAPI_STALE",
                "x".repeat(1025),
                null,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OpenAPI sync failure details exceed database limits");
        assertThatThrownBy(() -> syncStates.markFailure(
                Long.toString(SYNC_ID),
                7L,
                GatewayOpenApiSyncStateEnum.STALE,
                "  ",
                "message",
                null,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("errorCode must not be blank");
    }

    /**
     * 中文说明：{@code findDue} 保留旧 SQL 的 {@code status IN claimable}、重试窗口谓词、
     * {@code ORDER BY next_retry_at NULLS FIRST, id} 与 {@code LIMIT}：受守卫 API 不表达 {@code NULLS FIRST}，
     * 门面以两次读取拼接并按 limit 截断，第二次的语句在结果已够时不得下发。
     * English summary: {@code findDue} keeps the legacy {@code status IN claimable}, the retry-window predicate, the
     * {@code ORDER BY next_retry_at NULLS FIRST, id} ordering and the {@code LIMIT}: the guarded API cannot express
     * {@code NULLS FIRST}, so the facade concatenates two reads and truncates to the limit, and the second statement must
     * not be issued once the first read already filled the batch.
     */
    @Test
    @DisplayName("findDue emulates NULLS FIRST ordering and honours the batch limit")
    @SuppressWarnings("unchecked")
    void findDueEmulatesNullsFirstOrderingAndTheLimit() {
        when(syncDAO.selectList(any())).thenReturn(
                List.of(journalRow(SYNC_ID, GatewayOpenApiSyncStateEnum.DISCOVERED, 0L, 0, null, 0L)),
                List.of(
                        journalRow(9202L, GatewayOpenApiSyncStateEnum.FETCH_FAILED, 4L, 3,
                                NOW.minus(Duration.ofMinutes(2)), 2L),
                        journalRow(9203L, GatewayOpenApiSyncStateEnum.STALE, 6L, 1,
                                NOW.minus(Duration.ofSeconds(30)), 4L)
                )
        );

        assertThat(syncStates.findDue(NOW, 2))
                .extracting(GatewayOpenApiSyncBO::getId)
                .containsExactly("9201", "9202");

        ArgumentCaptor<LambdaQueryWrapper<GatewayOpenApiSyncRecordPO>> reads =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(syncDAO, times(2)).selectList(reads.capture());
        assertThat(reads.getAllValues().get(0).getTargetSql())
                .contains("status IN")
                .contains("next_retry_at IS NULL")
                .contains("ORDER BY")
                .contains("id ASC");
        assertThat(reads.getAllValues().get(1).getTargetSql())
                .contains("next_retry_at <= ?")
                .contains("next_retry_at ASC")
                .contains("id ASC");

        clearInvocations(syncDAO);
        when(syncDAO.selectList(any())).thenReturn(
                List.of(
                        journalRow(SYNC_ID, GatewayOpenApiSyncStateEnum.DISCOVERED, 0L, 0, null, 0L),
                        journalRow(9204L, GatewayOpenApiSyncStateEnum.INGEST_FAILED, 2L, 5, null, 1L)
                )
        );
        assertThat(syncStates.findDue(NOW, 2))
                .extracting(GatewayOpenApiSyncBO::getId)
                .containsExactly("9201", "9204");
        verify(syncDAO, times(1)).selectList(any());

        clearInvocations(syncDAO);
        assertThat(syncStates.findDue(NOW, 0)).isEmpty();
        verify(syncDAO, never()).selectList(any());
    }

    /**
     * 中文说明：快照读取保留旧 SQL 的谓词与次序——{@code fetched_at DESC, id} 的最新快照、
     * {@code openapi_group IN (...) ORDER BY openapi_group, id} 的构建内 Group 作用域（含判空、去空白与保序去重）、
     * {@code definition_set_id} 的反查次序；迁移后的数值列在收到非十进制标识时按“无行”处理而不下发语句。
     * English summary: The snapshot reads keep the legacy predicates and orderings — the newest snapshot by
     * {@code fetched_at DESC, id}, the in-build Group scope {@code openapi_group IN (...) ORDER BY openapi_group, id}
     * (including the null check, trimming and order-preserving de-duplication), and the {@code definition_set_id} reverse
     * lookup order; the migrated numeric columns yield “no row” for a non-decimal identifier without issuing a statement.
     */
    @Test
    @DisplayName("snapshot reads keep the legacy contract predicates and orderings")
    @SuppressWarnings("unchecked")
    void snapshotReadsKeepTheLegacyPredicatesAndOrderings() {
        when(snapshotDAO.selectList(any())).thenReturn(
                List.of(snapshotRow(TRUSTED_TENANT, null, 11L))
        );

        assertThat(snapshots.findByApplicationGroupAndCanonicalSha256(
                Long.toString(APPLICATION_ID), GROUP, CANONICAL_SHA))
                .get()
                .extracting(GatewayOpenApiSnapshotBO::getCanonicalSha256)
                .isEqualTo(CANONICAL_SHA);

        ArgumentCaptor<LambdaQueryWrapper<GatewayOpenApiSnapshotRecordPO>> newest =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(snapshotDAO).selectList(newest.capture());
        assertThat(newest.getValue().getTargetSql())
                .contains("application_id = ?")
                .contains("openapi_group = ?")
                .contains("canonical_sha256 = ?")
                .contains("fetched_at DESC")
                .contains("id ASC");
        verify(snapshotDAO, never()).selectById(any(Serializable.class));

        clearInvocations(snapshotDAO);
        assertThat(snapshots.findByBuildGroups(
                Long.toString(APPLICATION_ID),
                BUILD_ID,
                List.of(GROUP, "  " + GROUP + "  ", OTHER_GROUP)
        )).extracting(GatewayOpenApiSnapshotBO::getOpenapiGroup).containsExactly(GROUP);
        ArgumentCaptor<LambdaQueryWrapper<GatewayOpenApiSnapshotRecordPO>> scope =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(snapshotDAO).selectList(scope.capture());
        assertThat(scope.getValue().getTargetSql())
                .contains("build_id = ?")
                .contains("openapi_group IN")
                .contains("openapi_group ASC")
                .contains("id ASC");
        assertThat(scope.getValue().getParamNameValuePairs().values())
                .as("the Group set is trimmed and de-duplicated like the legacy IN list")
                .contains(GROUP, OTHER_GROUP)
                .noneMatch(value -> value instanceof String text && !text.equals(text.trim()));

        clearInvocations(snapshotDAO);
        assertThat(snapshots.findByBuildGroups(Long.toString(APPLICATION_ID), BUILD_ID, List.of("  ")))
                .isEmpty();
        verify(snapshotDAO, never()).selectList(any());

        clearInvocations(snapshotDAO);
        assertThat(snapshots.findByDefinitionSetId("not-a-numeric-id")).isEmpty();
        assertThat(snapshots.findByApplicationGroupAndCanonicalSha256("legacy-id", GROUP, CANONICAL_SHA))
                .isEmpty();
        verify(snapshotDAO, never()).selectList(any());

        clearInvocations(snapshotDAO);
        assertThat(snapshots.findByDefinitionSetId(Long.toString(DEFINITION_SET_ID)))
                .extracting(GatewayOpenApiSnapshotBO::getId)
                .containsExactly(Long.toString(SNAPSHOT_ID));
        ArgumentCaptor<LambdaQueryWrapper<GatewayOpenApiSnapshotRecordPO>> bySet =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(snapshotDAO).selectList(bySet.capture());
        assertThat(bySet.getValue().getTargetSql())
                .contains("definition_set_id = ?")
                .contains("openapi_group ASC")
                .contains("id ASC");
    }

    /**
     * 中文说明：journal 读取保留旧 SQL 的三元业务键、{@code ORDER BY build_id, openapi_group, id} 与
     * {@code WHERE status = ? ORDER BY updated_at, id}（迁移后即 {@code update_time}），必填校验的异常消息原样保留。
     * English summary: The journal reads keep the legacy three-part business key, the
     * {@code ORDER BY build_id, openapi_group, id} ordering and {@code WHERE status = ? ORDER BY updated_at, id} (the
     * migrated {@code update_time}), with the required-value messages preserved verbatim.
     */
    @Test
    @DisplayName("journal reads keep the legacy scope and stable orderings")
    @SuppressWarnings("unchecked")
    void journalReadsKeepTheLegacyScopeAndOrderings() {
        GatewayOpenApiSyncRecordPO stored = journalRow(
                SYNC_ID, GatewayOpenApiSyncStateEnum.VALID, 9L, 3, null, 11L);
        stored.setLatestSnapshotId(SNAPSHOT_ID);
        stored.setDefinitionSetId(DEFINITION_SET_ID);
        when(syncDAO.selectList(any())).thenReturn(List.of(stored));

        assertThat(syncStates.findByKey(new top.egon.cola.component.yuheng.admin.openapi.domain.dto
                .GatewayOpenApiSyncKeyDTO(Long.toString(APPLICATION_ID), BUILD_ID, GROUP)))
                .get()
                .extracting(GatewayOpenApiSyncBO::getId)
                .isEqualTo(Long.toString(SYNC_ID));
        ArgumentCaptor<LambdaQueryWrapper<GatewayOpenApiSyncRecordPO>> key =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(syncDAO).selectList(key.capture());
        assertThat(key.getValue().getTargetSql())
                .contains("application_id = ?")
                .contains("build_id = ?")
                .contains("openapi_group = ?");

        clearInvocations(syncDAO);
        assertThat(syncStates.findByApplicationId(Long.toString(APPLICATION_ID)))
                .extracting(GatewayOpenApiSyncBO::getBuildId)
                .containsExactly(BUILD_ID);
        ArgumentCaptor<LambdaQueryWrapper<GatewayOpenApiSyncRecordPO>> scope =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(syncDAO).selectList(scope.capture());
        assertThat(scope.getValue().getTargetSql())
                .contains("application_id = ?")
                .contains("build_id ASC")
                .contains("openapi_group ASC")
                .contains("id ASC");

        clearInvocations(syncDAO);
        assertThat(syncStates.findByStatus(GatewayOpenApiSyncStateEnum.VALIDATING))
                .singleElement()
                .extracting(GatewayOpenApiSyncBO::getRevision)
                .isEqualTo(9L);
        ArgumentCaptor<LambdaQueryWrapper<GatewayOpenApiSyncRecordPO>> status =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(syncDAO).selectList(status.capture());
        assertThat(status.getValue().getTargetSql())
                .contains("status = ?")
                .contains("update_time ASC")
                .contains("id ASC");
        assertThat(status.getValue().getParamNameValuePairs().values())
                .contains(GatewayOpenApiSyncStateEnum.VALIDATING.wireValue());

        assertThatThrownBy(() -> syncStates.findByApplicationId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("applicationId must not be blank");
        assertThatThrownBy(() -> syncStates.findByStatus(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("state");
    }

    /**
     * 中文说明：链接 Definition Set 只对尚未链接的行下发带 {@code definition_set_id IS NULL} 守卫的 CAS；
     * 已链接到同一聚合的行按幂等计数且不写库；并发方先写入同一链接时（影响 0 行后重读一致）同样计数。
     * English summary: The Definition Set link issues the {@code definition_set_id IS NULL} guarded compare-and-set only
     * for unlinked rows; a row already linked to the same aggregate is counted idempotently without any write, and so is a
     * concurrent identical link discovered by the reload after a zero-row effect.
     */
    @Test
    @DisplayName("the definition set link guards the null slot and counts idempotent rows")
    @SuppressWarnings("unchecked")
    void definitionSetLinkGuardsTheNullSlotAndCountsIdempotentRows() {
        when(snapshotDAO.selectActiveById(SNAPSHOT_ID)).thenReturn(
                snapshotRow(TRUSTED_TENANT, null, 11L)
        );
        when(snapshotDAO.update(any(GatewayOpenApiSnapshotRecordPO.class), any())).thenReturn(1);

        assertThat(snapshots.linkAllToDefinitionSet(
                List.of(Long.toString(SNAPSHOT_ID)), Long.toString(DEFINITION_SET_ID)))
                .isEqualTo(1);

        ArgumentCaptor<GatewayOpenApiSnapshotRecordPO> linked =
                ArgumentCaptor.forClass(GatewayOpenApiSnapshotRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayOpenApiSnapshotRecordPO>> predicate =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(snapshotDAO).update(linked.capture(), predicate.capture());
        assertThat(linked.getValue().getId()).isEqualTo(SNAPSHOT_ID);
        assertThat(linked.getValue().getVersion())
                .as("the read version is the compare-and-set token")
                .isEqualTo(11L);
        assertThat(linked.getValue().getDefinitionSetId()).isEqualTo(DEFINITION_SET_ID);
        assertThat(predicate.getValue().getTargetSql())
                .contains("id = ?")
                .contains("definition_set_id IS NULL");

        clearInvocations(snapshotDAO);
        when(snapshotDAO.selectActiveById(SNAPSHOT_ID)).thenReturn(
                snapshotRow(TRUSTED_TENANT, DEFINITION_SET_ID, 12L)
        );
        assertThat(snapshots.linkAllToDefinitionSet(
                List.of(Long.toString(SNAPSHOT_ID)), Long.toString(DEFINITION_SET_ID)))
                .as("an identical link is counted without a write")
                .isEqualTo(1);
        verify(snapshotDAO, never()).update(any(GatewayOpenApiSnapshotRecordPO.class), any());

        clearInvocations(snapshotDAO);
        when(snapshotDAO.selectActiveById(SNAPSHOT_ID)).thenReturn(
                snapshotRow(TRUSTED_TENANT, null, 11L),
                snapshotRow(TRUSTED_TENANT, DEFINITION_SET_ID, 12L)
        );
        when(snapshotDAO.update(any(GatewayOpenApiSnapshotRecordPO.class), any())).thenReturn(0);
        assertThat(snapshots.linkAllToDefinitionSet(
                List.of(Long.toString(SNAPSHOT_ID)), Long.toString(DEFINITION_SET_ID)))
                .as("a concurrent identical link is still counted, never a silent failure")
                .isEqualTo(1);
        verify(snapshotDAO, times(2)).selectActiveById(SNAPSHOT_ID);

        assertThat(snapshots.linkAllToDefinitionSet(List.of(), Long.toString(DEFINITION_SET_ID)))
                .as("an empty candidate list links nothing")
                .isEqualTo(0);
    }

    /**
     * 中文说明：链接到另一聚合的行按旧实现抛出 {@code ... already links to another definition set}，
     * 行不存在（含跨租户与软删）抛出 {@code YUHENG_OPENAPI_SNAPSHOT_NOT_FOUND}，两者都不得写库；
     * 迁移后的数值列也拒绝非十进制聚合标识。
     * English summary: A row linked to another aggregate raises the legacy
     * {@code ... already links to another definition set} and a missing row (including a foreign-tenant or soft-deleted
     * one) raises {@code YUHENG_OPENAPI_SNAPSHOT_NOT_FOUND}; neither may write, and the migrated numeric column also
     * rejects a non-decimal aggregate identifier.
     */
    @Test
    @DisplayName("the definition set link rejects conflicts and missing rows")
    void definitionSetLinkRejectsConflictsAndMissingRows() {
        when(snapshotDAO.selectActiveById(SNAPSHOT_ID)).thenReturn(
                snapshotRow(TRUSTED_TENANT, FOREIGN_DEFINITION_SET_ID, 11L)
        );
        assertThatThrownBy(() -> snapshots.linkAllToDefinitionSet(
                List.of(Long.toString(SNAPSHOT_ID)), Long.toString(DEFINITION_SET_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_OPENAPI_SNAPSHOT_CONFLICT: snapshot "
                        + SNAPSHOT_ID + " already links to another definition set");
        verify(snapshotDAO, never()).update(any(GatewayOpenApiSnapshotRecordPO.class), any());

        clearInvocations(snapshotDAO);
        when(snapshotDAO.selectActiveById(SNAPSHOT_ID)).thenReturn(null);
        assertThatThrownBy(() -> snapshots.linkAllToDefinitionSet(
                List.of(Long.toString(SNAPSHOT_ID)), Long.toString(DEFINITION_SET_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_OPENAPI_SNAPSHOT_NOT_FOUND: " + SNAPSHOT_ID);
        verify(snapshotDAO, never()).update(any(GatewayOpenApiSnapshotRecordPO.class), any());

        clearInvocations(snapshotDAO);
        assertThatThrownBy(() -> snapshots.linkAllToDefinitionSet(
                List.of(Long.toString(SNAPSHOT_ID)), "legacy-set-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("definitionSetId must be a numeric identifier");
        assertThatThrownBy(() -> snapshots.linkAllToDefinitionSet(
                List.of(Long.toString(SNAPSHOT_ID)), "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("definitionSetId must not be blank");
        verify(snapshotDAO, never()).selectActiveById(any(Serializable.class));
    }

    /**
     * 中文说明：第二租户负向用例——mapper 返回一行归属另一租户的快照时，门面必须抛
     * {@code TENANT_CONTEXT_MISMATCH} 而不是泄漏该载体；租户上下文缺失时更必须在触达 SQL 前失败。
     * English summary: The second-context negative case — when the mapper hands back a snapshot owned by another tenant the
     * facade must raise {@code TENANT_CONTEXT_MISMATCH} instead of leaking that carrier, and with no tenant context at all
     * it must fail before any SQL is reached.
     */
    @Test
    @DisplayName("a second tenant context cannot escape the publication boundary")
    void secondTenantContextCannotEscapeThePublicationBoundary() {
        when(snapshotDAO.selectActiveById(SNAPSHOT_ID))
                .thenReturn(snapshotRow(FOREIGN_TENANT, DEFINITION_SET_ID, 11L));
        assertThatThrownBy(() -> snapshots.findById(Long.toString(SNAPSHOT_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISMATCH");
        verify(snapshotDAO, never()).selectById(any(Serializable.class));

        GatewayOpenApiSyncRecordPO foreignJournal = journalRow(
                SYNC_ID, GatewayOpenApiSyncStateEnum.VALID, 9L, 3, null, 11L);
        foreignJournal.setTenantId(FOREIGN_TENANT);
        when(syncDAO.selectActiveById(SYNC_ID)).thenReturn(foreignJournal);
        assertThatThrownBy(() -> syncStates.findById(Long.toString(SYNC_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISMATCH");
        verify(syncDAO, never()).selectById(any(Serializable.class));

        clearInvocations(snapshotDAO, syncDAO);
        MDC.remove("tenantId");
        assertThatThrownBy(() -> snapshots.findByDefinitionSetId(Long.toString(DEFINITION_SET_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISSING");
        assertThatThrownBy(() -> syncStates.findDue(NOW, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISSING");
        verify(snapshotDAO, never()).selectList(any());
        verify(syncDAO, never()).selectList(any());
    }

    /**
     * 中文说明：受守卫读取到的每一行都必须重新经过业务载体的构造不变量，库里坏数据不能绕过装载边界；
     * 同时不透明标识在数值列上按“无行”处理，与旧实现的空结果形状一致。
     * English summary: Every row read through the guarded boundary is passed back through the carrier's construction
     * invariants so corrupt stored data cannot bypass the load boundary, while an opaque identifier yields “no row” on the
     * numeric columns, matching the legacy empty-result shape.
     */
    @Test
    @DisplayName("every loaded carrier still passes its carrier invariants")
    void everyLoadedCarrierStillPassesItsCarrierInvariants() {
        GatewayOpenApiSnapshotRecordPO blankDocument = snapshotRow(TRUSTED_TENANT, DEFINITION_SET_ID, 11L);
        blankDocument.setArtifactVersion("   ");
        when(snapshotDAO.selectActiveById(SNAPSHOT_ID)).thenReturn(blankDocument);
        assertThatThrownBy(() -> snapshots.findById(Long.toString(SNAPSHOT_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("artifactVersion must not be blank");

        GatewayOpenApiSyncRecordPO unlinkedValid = journalRow(
                SYNC_ID, GatewayOpenApiSyncStateEnum.VALID, 9L, 3, null, 11L);
        when(syncDAO.selectActiveById(SYNC_ID)).thenReturn(unlinkedValid);
        assertThatThrownBy(() -> syncStates.findById(Long.toString(SYNC_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("VALID sync state requires snapshot and definition set");

        assertThat(snapshots.findById("legacy-snapshot-id")).isEmpty();
        assertThat(syncStates.findById("legacy-sync-id")).isEmpty();
        verify(snapshotDAO, never()).selectById(any(Serializable.class));
        verify(syncDAO, never()).selectById(any(Serializable.class));
    }

    /**
     * 中文说明：登记发布按旧实现的三条 INSERT 顺序落库——发布头、同一编译产物/摘要的内容快照、首个 {@code PENDING}
     * 尝试；父键取刚写入的发布技术编号，租户/审计/版本列留给受守卫边界。任一步影响 0 行都按旧冲突文案如实抛出，
     * 后续语句一次都不下发（半个发布绝不报告成功），非十进制编号在触达持久边界前按转换器文案拒绝。
     * English summary: Registering a release keeps the legacy three-INSERT order — the head, the content snapshot of the very
     * same compiled artifact/digests, and the first {@code PENDING} attempt; the parent key is the technical release identifier
     * just written while tenant, audit and version columns belong to the guarded boundary. Any zero-row step raises the legacy
     * conflict message and no later statement is issued at all (half a registration is never reported successful), and a
     * non-decimal identifier is refused by the converter before persistence is touched.
     */
    @Test
    @DisplayName("registering a release writes the head, the frozen content snapshot and the first attempt")
    @SuppressWarnings("unchecked")
    void releaseRegistrationWritesHeadContentSnapshotAndFirstAttemptUnderOneArtifactSha() {
        when(releaseDAO.insert(any(GatewayReleaseRecordPO.class))).thenReturn(1);
        when(releaseContentDAO.insert(any(GatewayReleaseContentPO.class))).thenReturn(1);
        when(releaseAttemptDAO.insert(any(GatewayReleaseAttemptRecordPO.class))).thenReturn(1);

        releases.insert(releaseCarrier(GatewayReleaseStatus.CREATED), compiledRelease(), 1);

        ArgumentCaptor<GatewayReleaseRecordPO> head =
                ArgumentCaptor.forClass(GatewayReleaseRecordPO.class);
        verify(releaseDAO).insert(head.capture());
        assertThat(head.getValue().getId()).isEqualTo(RELEASE_ID);
        assertThat(head.getValue().getGatewayGroupId()).isEqualTo(GATEWAY_GROUP_ID);
        assertThat(head.getValue().getDraftRevision()).isEqualTo(12L);
        assertThat(head.getValue().getStatus()).isEqualTo(GatewayReleaseStatus.CREATED.name());
        assertThat(head.getValue().getPartialApplied())
                .as("the legacy INSERT bound partial_applied = FALSE explicitly")
                .isFalse();
        assertThat(head.getValue().getChangeId())
                .as("the legacy INSERT bound change_id = NULL explicitly")
                .isNull();
        assertThat(head.getValue().getValidationReport())
                .isEqualTo(JSON.valueToTree(Map.of("valid", true)));
        assertThat(head.getValue().getStructuredDiff())
                .isEqualTo(JSON.valueToTree(Map.of("operations", List.of("op-1"))));
        assertThat(head.getValue().getTenantId())
                .as("tenant, audit, soft delete and version belong to the guarded boundary")
                .isNull();
        assertThat(head.getValue().getVersion()).isNull();
        assertThat(head.getValue().getCreateTime()).isNull();

        ArgumentCaptor<GatewayReleaseContentPO> content =
                ArgumentCaptor.forClass(GatewayReleaseContentPO.class);
        verify(releaseContentDAO).insert(content.capture());
        assertThat(content.getValue().getReleaseId())
                .as("the content snapshot hangs off the release key the head was just written with")
                .isEqualTo(head.getValue().getId());
        assertThat(content.getValue().getRuleContentSha256()).isEqualTo(RULE_SHA);
        assertThat(content.getValue().getArtifactSha256())
                .as("the artifact SHA is frozen from the same compiled release the head points at")
                .isEqualTo(ARTIFACT_SHA);
        assertThat(content.getValue().getCanonicalSnapshot())
                .isEqualTo(JSON.valueToTree(SNAPSHOT_DOCUMENT));
        assertThat(content.getValue().getActivationContent())
                .isEqualTo(JSON.valueToTree(ACTIVATION_DOCUMENT));
        assertThat(content.getValue().getChunkManifest())
                .isEqualTo(JSON.valueToTree(Map.of(CHUNK_KEY, CHUNK_VALUE)));
        assertThat(content.getValue().getSnapshotSize())
                .as("snapshot_size stays the UTF-8 byte length of the canonical JSON")
                .isEqualTo((long) SNAPSHOT_JSON.getBytes(StandardCharsets.UTF_8).length);

        ArgumentCaptor<GatewayReleaseAttemptRecordPO> attempt =
                ArgumentCaptor.forClass(GatewayReleaseAttemptRecordPO.class);
        verify(releaseAttemptDAO).insert(attempt.capture());
        assertThat(attempt.getValue().getReleaseId()).isEqualTo(head.getValue().getId());
        assertThat(attempt.getValue().getAttemptNo()).isEqualTo(1);
        assertThat(attempt.getValue().getStatus())
                .as("the attempt journal starts at the legacy PENDING literal")
                .isEqualTo("PENDING");
        assertThat(attempt.getValue().getStartedAt()).isEqualTo(RELEASE_CREATED_AT);
        assertThat(attempt.getValue().getCompletedAt()).isNull();
        assertThat(attempt.getValue().getErrorCode()).isNull();

        clearInvocations(releaseDAO, releaseContentDAO, releaseAttemptDAO);
        when(releaseContentDAO.insert(any(GatewayReleaseContentPO.class))).thenReturn(0);
        assertThatThrownBy(() -> releases.insert(
                releaseCarrier(GatewayReleaseStatus.CREATED), compiledRelease(), 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_ADMIN_RELEASE_CONTENT_INSERT_CONFLICT");
        verify(releaseAttemptDAO, never()).insert(any(GatewayReleaseAttemptRecordPO.class));

        clearInvocations(releaseDAO, releaseContentDAO, releaseAttemptDAO);
        when(releaseDAO.insert(any(GatewayReleaseRecordPO.class))).thenReturn(0);
        assertThatThrownBy(() -> releases.insert(
                releaseCarrier(GatewayReleaseStatus.CREATED), compiledRelease(), 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_ADMIN_RELEASE_INSERT_CONFLICT");
        verify(releaseContentDAO, never()).insert(any(GatewayReleaseContentPO.class));
        verify(releaseAttemptDAO, never()).insert(any(GatewayReleaseAttemptRecordPO.class));

        clearInvocations(releaseDAO, releaseContentDAO, releaseAttemptDAO);
        assertThatThrownBy(() -> releases.insert(
                releaseCarrier(GatewayReleaseStatus.CREATED).setId("release-legacy"),
                compiledRelease(),
                1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gateway release identifier must be decimal: release-legacy");
        verify(releaseDAO, never()).insert(any(GatewayReleaseRecordPO.class));
        verify(releaseContentDAO, never()).insert(any(GatewayReleaseContentPO.class));
        verify(releaseAttemptDAO, never()).insert(any(GatewayReleaseAttemptRecordPO.class));
    }

    /**
     * 中文说明：{@code insertAll} 把一次尝试的操作集整体登记：同一发布、同一尝试、同一内容摘要下冻结
     * {@code API_RPC} 与 {@code MCP} 两个目标角色列（{@code target_role/target_biz_code/target_env/target_app_code}），
     * 尚未解析的日志行不带期望版本与目标版本；批次中途 0 行时按旧文案抛出且不再登记后续行，空批次与 {@code null}
     * 元素在第一条语句之前就被拒绝。
     * English summary: {@code insertAll} registers the whole operation set of one attempt: under one release, one attempt and one
     * content digest it freezes the {@code API_RPC} and {@code MCP} target role columns
     * ({@code target_role/target_biz_code/target_env/target_app_code}), a not-yet-resolved journal row carries neither the expected
     * nor the target version, a zero-row effect mid-batch raises the legacy message without registering any later row, and an
     * empty batch or a {@code null} element is refused before the first statement.
     */
    @Test
    @DisplayName("the publication journal freezes both engine roles of one compiled release")
    void publicationJournalFreezesBothEngineRolesOfOneCompiledRelease() {
        when(releasePublicationDAO.insert(any(GatewayReleasePublicationRecordPO.class))).thenReturn(1);

        publications.insertAll(List.of(
                publicationCarrier(0, GatewayPublicationPhaseEnum.CHUNK, CHUNK_KEY, CHUNK_VALUE,
                        CHUNK_SHA, scope(GatewayEngineRoleEnum.API_RPC)),
                publicationCarrier(1, GatewayPublicationPhaseEnum.CHUNK, CHUNK_KEY, CHUNK_VALUE,
                        CHUNK_SHA, scope(GatewayEngineRoleEnum.MCP)),
                publicationCarrier(2, GatewayPublicationPhaseEnum.ACTIVATION, ACTIVE_KEY,
                        ACTIVATION_JSON, ARTIFACT_SHA, scope(GatewayEngineRoleEnum.API_RPC)),
                publicationCarrier(3, GatewayPublicationPhaseEnum.ACTIVATION, ACTIVE_KEY,
                        ACTIVATION_JSON, ARTIFACT_SHA, scope(GatewayEngineRoleEnum.MCP))
        ));

        ArgumentCaptor<GatewayReleasePublicationRecordPO> rows =
                ArgumentCaptor.forClass(GatewayReleasePublicationRecordPO.class);
        verify(releasePublicationDAO, times(4)).insert(rows.capture());
        assertThat(rows.getAllValues())
                .as("the two frozen roles are written side by side, never collapsed into one")
                .extracting(GatewayReleasePublicationRecordPO::getTargetRole)
                .containsExactly(
                        GatewayEngineRoleEnum.API_RPC.name(),
                        GatewayEngineRoleEnum.MCP.name(),
                        GatewayEngineRoleEnum.API_RPC.name(),
                        GatewayEngineRoleEnum.MCP.name());
        assertThat(rows.getAllValues())
                .extracting(GatewayReleasePublicationRecordPO::getPhaseType)
                .containsExactly(
                        GatewayPublicationPhaseEnum.CHUNK.name(),
                        GatewayPublicationPhaseEnum.CHUNK.name(),
                        GatewayPublicationPhaseEnum.ACTIVATION.name(),
                        GatewayPublicationPhaseEnum.ACTIVATION.name());
        rows.getAllValues().forEach(row -> {
            assertThat(row.getReleaseId())
                    .as("every target row is derived from the same release")
                    .isEqualTo(RELEASE_ID);
            assertThat(row.getAttemptNo())
                    .as("every target row belongs to the same attempt")
                    .isEqualTo(1L);
            assertThat(row.getTargetBizCode()).isEqualTo(BIZ_CODE);
            assertThat(row.getTargetEnv()).isEqualTo(ENV);
            assertThat(row.getTargetAppCode()).isEqualTo(APP_CODE);
            assertThat(row.getContentValue())
                    .as("the payload is frozen once, on the journal row")
                    .isNotBlank();
            assertThat(row.getContentSha256())
                    .as("the digest of that frozen payload is the one the release was compiled to")
                    .isIn(CHUNK_SHA, ARTIFACT_SHA);
            assertThat(row.getDdcStatus())
                    .isEqualTo(GatewayPublicationStatusEnum.PLANNED.name());
            assertThat(row.getExpectedVersion())
                    .as("a registered operation is not resolved yet")
                    .isNull();
            assertThat(row.getDdcTargetVersion()).isNull();
            assertThat(row.getTenantId())
                    .as("the guarded boundary owns tenant, audit, soft delete and version")
                    .isNull();
            assertThat(row.getVersion()).isNull();
        });

        clearInvocations(releasePublicationDAO);
        when(releasePublicationDAO.insert(any(GatewayReleasePublicationRecordPO.class)))
                .thenReturn(1, 0);
        assertThatThrownBy(() -> publications.insertAll(List.of(
                publicationCarrier(0, GatewayPublicationPhaseEnum.CHUNK, CHUNK_KEY, CHUNK_VALUE,
                        CHUNK_SHA, scope(GatewayEngineRoleEnum.API_RPC)),
                publicationCarrier(1, GatewayPublicationPhaseEnum.CHUNK, CHUNK_KEY, CHUNK_VALUE,
                        CHUNK_SHA, scope(GatewayEngineRoleEnum.MCP)),
                publicationCarrier(2, GatewayPublicationPhaseEnum.ACTIVATION, ACTIVE_KEY,
                        ACTIVATION_JSON, ARTIFACT_SHA, scope(GatewayEngineRoleEnum.API_RPC))
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_ADMIN_RELEASE_PUBLICATION_INSERT_CONFLICT");
        verify(releasePublicationDAO, times(2)).insert(any(GatewayReleasePublicationRecordPO.class));

        clearInvocations(releasePublicationDAO);
        assertThatThrownBy(() -> publications.insertAll(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("publication operations must not be empty");
        assertThatThrownBy(() -> publications.insertAll(Arrays.asList(
                null,
                publicationCarrier(0, GatewayPublicationPhaseEnum.CHUNK, CHUNK_KEY, CHUNK_VALUE,
                        CHUNK_SHA, scope(GatewayEngineRoleEnum.API_RPC))
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("publication operation must not be null");
        verify(releasePublicationDAO, never()).insert(any(GatewayReleasePublicationRecordPO.class));
    }

    /**
     * 中文说明：{@code nextAttempt} 保留旧 {@code COALESCE(MAX(attempt_no), 0) + 1} 语义——按发布整体读取日志求最大
     * 序号（谓词只有 {@code release_id}，绝不把重试钉在单个 attempt 上），无日志时从 1 开始；登记的新行是
     * {@code PENDING}，0 行插入按旧冲突文案如实抛出，{@code latestAttempt} 在无日志时沿用旧的「未找到」文案。
     * English summary: {@code nextAttempt} keeps the legacy {@code COALESCE(MAX(attempt_no), 0) + 1} semantics — the journal is read
     * per release to find the greatest number (the predicate is {@code release_id} only, so a retry is never pinned to one attempt
     * row), one starts the counting when the journal is empty, the registered row is {@code PENDING}, a zero-row insert surfaces as
     * the legacy conflict, and {@code latestAttempt} keeps the legacy "not found" message for an empty journal.
     */
    @Test
    @DisplayName("the next attempt is one past the greatest journal row")
    @SuppressWarnings("unchecked")
    void nextAttemptRegistersOnePastTheGreatestJournalRowAndNeverAFabricatedInsert() {
        when(releaseAttemptDAO.selectList(any())).thenReturn(
                List.of(
                        attemptRow(ATTEMPT_ID, 1, "SUCCESS", 11L),
                        attemptRow(9302L, 3, "PUBLISHING", 12L)
                )
        );
        when(releaseAttemptDAO.insert(any(GatewayReleaseAttemptRecordPO.class))).thenReturn(1);

        assertThat(releases.nextAttempt(Long.toString(RELEASE_ID), NOW)).isEqualTo(4);

        ArgumentCaptor<LambdaQueryWrapper<GatewayReleaseAttemptRecordPO>> journal =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releaseAttemptDAO).selectList(journal.capture());
        assertThat(journal.getValue().getTargetSql())
                .contains("release_id = ?")
                .doesNotContain("attempt_no")
                .doesNotContain("ORDER BY");
        assertThat(journal.getValue().getParamNameValuePairs().values())
                .as("the MAX(attempt_no) scan is scoped to the release only")
                .containsExactlyInAnyOrder(RELEASE_ID);

        ArgumentCaptor<GatewayReleaseAttemptRecordPO> registered =
                ArgumentCaptor.forClass(GatewayReleaseAttemptRecordPO.class);
        verify(releaseAttemptDAO).insert(registered.capture());
        assertThat(registered.getValue().getReleaseId()).isEqualTo(RELEASE_ID);
        assertThat(registered.getValue().getAttemptNo()).isEqualTo(4);
        assertThat(registered.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(registered.getValue().getStartedAt())
                .as("the retry starts at the caller's trusted instant, not the previous attempt's")
                .isEqualTo(NOW);
        assertThat(registered.getValue().getCompletedAt()).isNull();
        assertThat(registered.getValue().getVersion()).isNull();

        clearInvocations(releaseAttemptDAO);
        assertThat(releases.latestAttempt(Long.toString(RELEASE_ID))).isEqualTo(3);

        clearInvocations(releaseAttemptDAO);
        when(releaseAttemptDAO.selectList(any())).thenReturn(List.of());
        assertThat(releases.nextAttempt(Long.toString(RELEASE_ID), NOW))
                .as("an empty journal starts numbering at one, like the legacy COALESCE")
                .isEqualTo(1);

        clearInvocations(releaseAttemptDAO);
        when(releaseAttemptDAO.insert(any(GatewayReleaseAttemptRecordPO.class))).thenReturn(0);
        assertThatThrownBy(() -> releases.nextAttempt(Long.toString(RELEASE_ID), NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_ADMIN_RELEASE_ATTEMPT_INSERT_CONFLICT");
        assertThatThrownBy(() -> releases.latestAttempt(Long.toString(RELEASE_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("release attempt was not found");
    }

    /**
     * 中文说明：{@code beginAttempt} 保留旧两条 UPDATE：日志行置 {@code PUBLISHING}、{@code started_at} 并显式清空
     * {@code completed_at/error_code/error_message}，CAS 谓词是旧的 {@code release_id + attempt_no}（再加受守卫要求的
     * 技术主键与版本令牌）；发布头随后置 {@code PUBLISHING}，旧 {@code updated_at} 写在 CAS 令牌上。日志 0 行、日志
     * 缺失与发布头缺失都如实抛出冲突，且发布头一次都不写——半个状态推进不是成功。
     * English summary: {@code beginAttempt} keeps the legacy pair of UPDATE statements: the journal row turns {@code PUBLISHING} with
     * {@code started_at} while {@code completed_at/error_code/error_message} are cleared explicitly, the compare-and-set carries the
     * legacy {@code release_id + attempt_no} plus the technical key and version token the guard requires; the release head follows
     * with {@code PUBLISHING} and the legacy {@code updated_at} on the CAS token. A zero-row journal write, an absent journal row
     * and an absent head all raise the conflict truthfully and the head is never written — half a state move is not success.
     */
    @Test
    @DisplayName("beginAttempt clears the legacy completion columns under CAS")
    @SuppressWarnings("unchecked")
    void beginAttemptClearsTheLegacyCompletionColumnsUnderCas() {
        when(releaseAttemptDAO.selectOne(any(), anyBoolean())).thenReturn(
                attemptRow(ATTEMPT_ID, 2, "PENDING", 12L)
        );
        when(releaseAttemptDAO.update(any(GatewayReleaseAttemptRecordPO.class), any())).thenReturn(1);
        when(releaseDAO.selectActiveById(RELEASE_ID)).thenReturn(
                releaseHead(RELEASE_ID, GatewayReleaseStatus.READY, RELEASE_CREATED_AT, 31L)
        );
        when(releaseDAO.update(any(GatewayReleaseRecordPO.class), any())).thenReturn(1);

        releases.beginAttempt(Long.toString(RELEASE_ID), 2, NOW);

        ArgumentCaptor<LambdaQueryWrapper<GatewayReleaseAttemptRecordPO>> located =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releaseAttemptDAO).selectOne(located.capture(), anyBoolean());
        assertThat(located.getValue().getTargetSql())
                .contains("release_id = ?")
                .contains("attempt_no = ?");
        assertThat(located.getValue().getParamNameValuePairs().values())
                .as("the CAS is pinned to exactly the attempt being begun")
                .containsExactlyInAnyOrder(RELEASE_ID, 2);

        ArgumentCaptor<GatewayReleaseAttemptRecordPO> token =
                ArgumentCaptor.forClass(GatewayReleaseAttemptRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayReleaseAttemptRecordPO>> journal =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(releaseAttemptDAO).update(token.capture(), journal.capture());
        assertThat(token.getValue().getId()).isEqualTo(ATTEMPT_ID);
        assertThat(token.getValue().getVersion())
                .as("the read version is the optimistic-lock token of the CAS")
                .isEqualTo(12L);
        assertThat(journal.getValue().getTargetSql())
                .contains("id = ?")
                .contains("release_id = ?")
                .contains("attempt_no = ?");
        assertThat(journal.getValue().getSqlSet())
                .contains("status=")
                .contains("started_at=")
                .contains("completed_at=")
                .contains("error_code=")
                .contains("error_message=");
        assertThat(journal.getValue().getParamNameValuePairs().values())
                .contains(GatewayReleaseStatus.PUBLISHING.name(), NOW, 2)
                .containsNull();

        ArgumentCaptor<GatewayReleaseRecordPO> headToken =
                ArgumentCaptor.forClass(GatewayReleaseRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayReleaseRecordPO>> head =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(releaseDAO).update(headToken.capture(), head.capture());
        assertThat(headToken.getValue().getId()).isEqualTo(RELEASE_ID);
        assertThat(headToken.getValue().getVersion()).isEqualTo(31L);
        assertThat(headToken.getValue().getUpdateTime())
                .as("the legacy updated_at write rides on the CAS token")
                .isEqualTo(NOW);
        assertThat(head.getValue().getSqlSet())
                .as("the legacy head UPDATE listed status only, nothing else")
                .contains("status=")
                .doesNotContain("partial_applied")
                .doesNotContain("change_id");
        assertThat(head.getValue().getTargetSql()).contains("id = ?");
        assertThat(head.getValue().getParamNameValuePairs().values())
                .contains(GatewayReleaseStatus.PUBLISHING.name());

        clearInvocations(releaseDAO, releaseAttemptDAO);
        when(releaseAttemptDAO.update(any(GatewayReleaseAttemptRecordPO.class), any())).thenReturn(0);
        assertThatThrownBy(() -> releases.beginAttempt(Long.toString(RELEASE_ID), 2, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_ADMIN_RELEASE_ATTEMPT_WRITE_CONFLICT");
        verify(releaseDAO, never()).selectActiveById(any(Serializable.class));
        verify(releaseDAO, never()).update(any(GatewayReleaseRecordPO.class), any());

        clearInvocations(releaseDAO, releaseAttemptDAO);
        when(releaseAttemptDAO.selectOne(any(), anyBoolean())).thenReturn(null);
        assertThatThrownBy(() -> releases.beginAttempt(Long.toString(RELEASE_ID), 2, NOW))
                .as("an unmatchable attempt is a zero-row effect, not a silent success")
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_ADMIN_RELEASE_ATTEMPT_WRITE_CONFLICT");
        verify(releaseAttemptDAO, never()).update(any(GatewayReleaseAttemptRecordPO.class), any());
        verify(releaseDAO, never()).update(any(GatewayReleaseRecordPO.class), any());

        clearInvocations(releaseDAO, releaseAttemptDAO);
        when(releaseAttemptDAO.selectOne(any(), anyBoolean())).thenReturn(
                attemptRow(ATTEMPT_ID, 2, "PENDING", 12L)
        );
        when(releaseAttemptDAO.update(any(GatewayReleaseAttemptRecordPO.class), any())).thenReturn(1);
        when(releaseDAO.selectActiveById(RELEASE_ID)).thenReturn(null);
        assertThatThrownBy(() -> releases.beginAttempt(Long.toString(RELEASE_ID), 2, NOW))
                .as("the journal moved but the head is missing, so the state move is not a success")
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_ADMIN_RELEASE_WRITE_CONFLICT");
        verify(releaseDAO, never()).update(any(GatewayReleaseRecordPO.class), any());
    }

    /**
     * 中文说明：{@code completeAttempt} 沿用旧三步：日志行记终态（{@code change_id/error_code/error_message} 允许为
     * NULL，经构造器显式下推），发布头记状态、部分应用标记与变更号，最后按旧 {@code ON CONFLICT} 业务键
     * {@code (release_id, attempt_no, instance_id, lease_id)} 逐个上补实例目标：缺失时插入（父键与尝试序号由门面补齐、
     * 转换器不映射），存在时整列替换 DO UPDATE 列出的六列；任一 0 行都按旧冲突文案抛出而不是报告成功。
     * English summary: {@code completeAttempt} keeps the legacy three steps: the journal row records the terminal state (with
     * {@code change_id/error_code/error_message} allowed to be NULL and pushed explicitly by the wrapper), the head records status,
     * the partial-application flag and the change id, and every instance target is then upserted under the legacy
     * {@code ON CONFLICT} business key {@code (release_id, attempt_no, instance_id, lease_id)} — inserted when absent (the parent key
     * and attempt number supplied by the facade because the converter does not map them), replaced column by column for the six
     * {@code DO UPDATE} columns when present; any zero-row effect raises the legacy conflict instead of reporting success.
     */
    @Test
    @DisplayName("completeAttempt writes the terminal state and both frozen target observations")
    @SuppressWarnings("unchecked")
    void completeAttemptWritesTheTerminalStateAndBothFrozenTargetRows() {
        when(releaseAttemptDAO.selectOne(any(), anyBoolean())).thenReturn(
                attemptRow(ATTEMPT_ID, 2, "PUBLISHING", 13L)
        );
        when(releaseAttemptDAO.update(any(GatewayReleaseAttemptRecordPO.class), any())).thenReturn(1);
        when(releaseDAO.selectActiveById(RELEASE_ID)).thenReturn(
                releaseHead(RELEASE_ID, GatewayReleaseStatus.PUBLISHING, RELEASE_CREATED_AT, 32L)
        );
        when(releaseDAO.update(any(GatewayReleaseRecordPO.class), any())).thenReturn(1);
        when(releaseTargetDAO.selectOne(any(), anyBoolean())).thenReturn(
                null,
                targetRow(TARGET_ID, 2, GatewayEngineRoleEnum.MCP, 41L, 21L)
        );
        when(releaseTargetDAO.insert(any(GatewayReleaseTargetRecordPO.class))).thenReturn(1);
        when(releaseTargetDAO.update(any(GatewayReleaseTargetRecordPO.class), any())).thenReturn(1);

        releases.completeAttempt(
                Long.toString(RELEASE_ID),
                2,
                GatewayReleaseStatus.SUCCESS,
                true,
                CHANGE_ID,
                null,
                null,
                List.of(
                        targetObservation("node-1", "lease-1", GatewayEngineRoleEnum.API_RPC, 42L),
                        targetObservation("node-2", "lease-2", GatewayEngineRoleEnum.MCP, 41L)
                ),
                NOW
        );

        ArgumentCaptor<GatewayReleaseAttemptRecordPO> attemptToken =
                ArgumentCaptor.forClass(GatewayReleaseAttemptRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayReleaseAttemptRecordPO>> attemptCas =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(releaseAttemptDAO).update(attemptToken.capture(), attemptCas.capture());
        assertThat(attemptToken.getValue().getId()).isEqualTo(ATTEMPT_ID);
        assertThat(attemptToken.getValue().getVersion()).isEqualTo(13L);
        assertThat(attemptCas.getValue().getTargetSql())
                .contains("id = ?")
                .contains("release_id = ?")
                .contains("attempt_no = ?");
        assertThat(attemptCas.getValue().getSqlSet())
                .contains("status=")
                .contains("change_id=")
                .contains("completed_at=")
                .contains("error_code=")
                .contains("error_message=");
        assertThat(attemptCas.getValue().getParamNameValuePairs().values())
                .as("a successful attempt clears both error columns like the legacy SET NULL")
                .contains(GatewayReleaseStatus.SUCCESS.name(), CHANGE_ID, NOW)
                .containsNull();

        ArgumentCaptor<GatewayReleaseRecordPO> headToken =
                ArgumentCaptor.forClass(GatewayReleaseRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayReleaseRecordPO>> headCas =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(releaseDAO).update(headToken.capture(), headCas.capture());
        assertThat(headToken.getValue().getVersion()).isEqualTo(32L);
        assertThat(headToken.getValue().getUpdateTime()).isEqualTo(NOW);
        assertThat(headCas.getValue().getSqlSet())
                .contains("status=")
                .contains("partial_applied=")
                .contains("change_id=");
        assertThat(headCas.getValue().getTargetSql()).contains("id = ?");
        assertThat(headCas.getValue().getParamNameValuePairs().values())
                .contains(GatewayReleaseStatus.SUCCESS.name(), true, CHANGE_ID);

        ArgumentCaptor<GatewayReleaseTargetRecordPO> inserted =
                ArgumentCaptor.forClass(GatewayReleaseTargetRecordPO.class);
        verify(releaseTargetDAO).insert(inserted.capture());
        assertThat(inserted.getValue().getReleaseId()).isEqualTo(RELEASE_ID);
        assertThat(inserted.getValue().getAttemptNo())
                .as("the parent key and attempt number belong to the facade, not the converter")
                .isEqualTo(2L);
        assertThat(inserted.getValue().getInstanceId()).isEqualTo("node-1");
        assertThat(inserted.getValue().getLeaseId()).isEqualTo("lease-1");
        assertThat(inserted.getValue().getEngineRole())
                .as("the frozen engine role is written, never inferred from deployment")
                .isEqualTo(GatewayEngineRoleEnum.API_RPC.name());
        assertThat(inserted.getValue().getAppliedArtifactSha256())
                .as("the observation is reported against the same compiled artifact SHA the release froze")
                .isEqualTo(ARTIFACT_SHA);
        assertThat(inserted.getValue().getAppliedVersion()).isEqualTo(42L);
        assertThat(inserted.getValue().getStatus()).isEqualTo("APPLIED");
        assertThat(inserted.getValue().getObservedAt()).isEqualTo(NOW);
        assertThat(inserted.getValue().getTenantId()).isNull();

        ArgumentCaptor<LambdaQueryWrapper<GatewayReleaseTargetRecordPO>> located =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releaseTargetDAO, times(2)).selectOne(located.capture(), anyBoolean());
        assertThat(located.getAllValues().get(0).getTargetSql())
                .contains("release_id = ?")
                .contains("attempt_no = ?")
                .contains("instance_id = ?")
                .contains("lease_id = ?");
        assertThat(located.getAllValues().get(0).getParamNameValuePairs().values())
                .containsExactlyInAnyOrder(RELEASE_ID, 2L, "node-1", "lease-1");
        assertThat(located.getAllValues().get(1).getParamNameValuePairs().values())
                .containsExactlyInAnyOrder(RELEASE_ID, 2L, "node-2", "lease-2");

        ArgumentCaptor<GatewayReleaseTargetRecordPO> replaced =
                ArgumentCaptor.forClass(GatewayReleaseTargetRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayReleaseTargetRecordPO>> targetCas =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(releaseTargetDAO).update(replaced.capture(), targetCas.capture());
        assertThat(replaced.getValue().getId()).isEqualTo(TARGET_ID);
        assertThat(replaced.getValue().getVersion())
                .as("the read version is the optimistic-lock token of the upsert")
                .isEqualTo(21L);
        assertThat(targetCas.getValue().getTargetSql())
                .contains("id = ?")
                .contains("release_id = ?")
                .contains("attempt_no = ?")
                .contains("instance_id = ?")
                .contains("lease_id = ?");
        assertThat(targetCas.getValue().getSqlSet())
                .as("exactly the legacy DO UPDATE column set is replaced")
                .contains("status=", "applied_version=", "applied_artifact_sha256=",
                        "error_code=", "observed_at=", "engine_role=");
        assertThat(targetCas.getValue().getParamNameValuePairs().values())
                .contains(GatewayEngineRoleEnum.MCP.name(), ARTIFACT_SHA, 41L);

        clearInvocations(releaseDAO, releaseAttemptDAO, releaseTargetDAO);
        when(releaseTargetDAO.selectOne(any(), anyBoolean())).thenReturn(
                targetRow(TARGET_ID, 2, GatewayEngineRoleEnum.API_RPC, 42L, 22L)
        );
        when(releaseTargetDAO.update(any(GatewayReleaseTargetRecordPO.class), any())).thenReturn(0);
        assertThatThrownBy(() -> releases.completeAttempt(
                Long.toString(RELEASE_ID),
                2,
                GatewayReleaseStatus.SUCCESS,
                true,
                CHANGE_ID,
                null,
                null,
                List.of(
                        targetObservation("node-1", "lease-1", GatewayEngineRoleEnum.API_RPC, 42L),
                        targetObservation("node-2", "lease-2", GatewayEngineRoleEnum.MCP, 41L)
                ),
                NOW
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_ADMIN_RELEASE_TARGET_WRITE_CONFLICT");
        verify(releaseTargetDAO, times(1)).update(any(GatewayReleaseTargetRecordPO.class), any());
        verify(releaseTargetDAO, never()).insert(any(GatewayReleaseTargetRecordPO.class));
    }

    /**
     * 中文说明：{@code resolveDocument} 与 {@code markSubmitted} 保留旧单条 UPDATE 的谓词与列集——前者
     * {@code WHERE change_id = ? AND ddc_status <> 'SUCCESS'} 写 {@code expected_version/content_value/ddc_status}，后者
     * {@code ddc_status = 'RESOLVED' AND expected_version IS NOT NULL} 只写 {@code ddc_status}；受守卫实现把旧语句拆成
     * 「按同一谓词定位 + 乐观锁 CAS」，0 行与定位不到行都按旧文案抛出，参数守护在触达语句前生效。
     * English summary: {@code resolveDocument} and {@code markSubmitted} keep the predicates and column sets of the legacy single
     * UPDATE — the first carries {@code WHERE change_id = ? AND ddc_status <> 'SUCCESS'} and writes
     * {@code expected_version/content_value/ddc_status}, the second carries {@code ddc_status = 'RESOLVED' AND expected_version IS NOT
     * NULL} and writes only {@code ddc_status}; the guarded implementation splits the statement into locating by the same predicates
     * plus an optimistic-lock CAS, raises the legacy messages for a zero-row effect or a missed row, and validates its arguments
     * before any statement is reached.
     */
    @Test
    @DisplayName("resolveDocument and markSubmitted keep the legacy publication predicates")
    @SuppressWarnings("unchecked")
    void resolveDocumentAndMarkSubmittedKeepTheLegacyPredicatesAndColumnSets() {
        when(releasePublicationDAO.selectOne(any(), anyBoolean())).thenReturn(
                publicationRow(PUBLICATION_ID, GatewayPublicationStatusEnum.PLANNED)
        );
        when(releasePublicationDAO.update(any(GatewayReleasePublicationRecordPO.class), any())).thenReturn(1);

        publications.resolveDocument(CHANGE_ID, 41L, CHUNK_VALUE, NOW);

        ArgumentCaptor<LambdaQueryWrapper<GatewayReleasePublicationRecordPO>> location =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releasePublicationDAO).selectOne(location.capture(), anyBoolean());
        assertThat(location.getValue().getTargetSql())
                .contains("change_id = ?")
                .contains("ddc_status <> ?");
        assertThat(location.getValue().getParamNameValuePairs().values())
                .as("a successful publication is never resolved again")
                .containsExactlyInAnyOrder(CHANGE_ID, GatewayPublicationStatusEnum.SUCCESS.name());

        ArgumentCaptor<GatewayReleasePublicationRecordPO> resolvedToken =
                ArgumentCaptor.forClass(GatewayReleasePublicationRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayReleasePublicationRecordPO>> resolved =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(releasePublicationDAO).update(resolvedToken.capture(), resolved.capture());
        assertThat(resolvedToken.getValue().getId()).isEqualTo(PUBLICATION_ID);
        assertThat(resolvedToken.getValue().getVersion())
                .as("the read version is the optimistic-lock token of the CAS")
                .isEqualTo(31L);
        assertThat(resolvedToken.getValue().getUpdateTime())
                .as("the legacy updated_at write rides on the CAS token")
                .isEqualTo(NOW);
        assertThat(resolved.getValue().getTargetSql())
                .contains("id = ?")
                .contains("change_id = ?");
        assertThat(resolved.getValue().getSqlSet())
                .contains("expected_version=")
                .contains("content_value=")
                .contains("ddc_status=");
        assertThat(resolved.getValue().getParamNameValuePairs().values())
                .contains(41L, CHUNK_VALUE, GatewayPublicationStatusEnum.RESOLVED.name());

        clearInvocations(releasePublicationDAO);
        when(releasePublicationDAO.update(any(GatewayReleasePublicationRecordPO.class), any()))
                .thenReturn(0);
        assertThatThrownBy(() -> publications.resolveDocument(CHANGE_ID, 41L, CHUNK_VALUE, NOW))
                .as("a zero-row compare-and-set is never a resolved document")
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("successful publication cannot be resolved again");

        clearInvocations(releasePublicationDAO);
        when(releasePublicationDAO.update(any(GatewayReleasePublicationRecordPO.class), any()))
                .thenReturn(1);
        when(releasePublicationDAO.selectOne(any(), anyBoolean())).thenReturn(null);
        assertThatThrownBy(() -> publications.resolveDocument(CHANGE_ID, 41L, CHUNK_VALUE, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("successful publication cannot be resolved again");
        verify(releasePublicationDAO, never()).update(any(GatewayReleasePublicationRecordPO.class), any());

        clearInvocations(releasePublicationDAO);
        assertThatThrownBy(() -> publications.resolveDocument(CHANGE_ID, -1L, CHUNK_VALUE, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expectedVersion must not be negative");
        assertThatThrownBy(() -> publications.resolveDocument(CHANGE_ID, 41L, "  ", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("documentContent must not be blank");
        verify(releasePublicationDAO, never()).selectOne(any(), anyBoolean());

        clearInvocations(releasePublicationDAO);
        when(releasePublicationDAO.selectOne(any(), anyBoolean())).thenReturn(
                publicationRow(PUBLICATION_ID, GatewayPublicationStatusEnum.RESOLVED)
        );
        publications.markSubmitted(CHANGE_ID, NOW);
        ArgumentCaptor<LambdaQueryWrapper<GatewayReleasePublicationRecordPO>> submitted =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releasePublicationDAO).selectOne(submitted.capture(), anyBoolean());
        assertThat(submitted.getValue().getTargetSql())
                .contains("change_id = ?")
                .contains("ddc_status = ?")
                .contains("expected_version IS NOT NULL");
        assertThat(submitted.getValue().getParamNameValuePairs().values())
                .containsExactlyInAnyOrder(CHANGE_ID, GatewayPublicationStatusEnum.RESOLVED.name());
        ArgumentCaptor<LambdaUpdateWrapper<GatewayReleasePublicationRecordPO>> submittedCas =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(releasePublicationDAO).update(any(GatewayReleasePublicationRecordPO.class), submittedCas.capture());
        assertThat(submittedCas.getValue().getSqlSet())
                .as("the legacy submission SET listed ddc_status only")
                .contains("ddc_status=")
                .doesNotContain("content_value")
                .doesNotContain("expected_version");
        assertThat(submittedCas.getValue().getParamNameValuePairs().values())
                .contains(GatewayPublicationStatusEnum.SUBMITTED.name());

        clearInvocations(releasePublicationDAO);
        when(releasePublicationDAO.update(any(GatewayReleasePublicationRecordPO.class), any()))
                .thenReturn(0);
        assertThatThrownBy(() -> publications.markSubmitted(CHANGE_ID, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("publication must be RESOLVED before submission");
    }

    /**
     * 中文说明：{@code markResult} 与 {@code markChunkCleaned} 写回旧列集且允许 NULL 的列一律经构造器
     * {@code .set(..., null)} 显式清空：结果回写写 {@code ddc_target_version/ddc_status/error_code/error_message} 且只在
     * {@code ddc_status IN (可记录六态)} 时命中；清理标记只写 {@code error_code='CHUNK_GC_DELETED'} 与
     * {@code error_message=NULL} 且要求 {@code phase_type='CHUNK' AND ddc_status='SUCCESS' AND ddc_target_version IS NOT NULL}；
     * 非终态、成功缺目标版本、0 行与定位不到行都按旧文案失败，不报告成功。
     * English summary: {@code markResult} and {@code markChunkCleaned} write the legacy column sets and clear every NULL-able column
     * explicitly through {@code .set(..., null)}: the result write-back records {@code ddc_target_version/ddc_status/error_code/
     * error_message} and only matches {@code ddc_status IN (the six recordable states)}, while the cleanup marker writes only
     * {@code error_code='CHUNK_GC_DELETED'} plus {@code error_message=NULL} and requires {@code phase_type='CHUNK' AND ddc_status=
     * 'SUCCESS' AND ddc_target_version IS NOT NULL}; a non-terminal status, a missing target version on success, a zero-row effect
     * and a missed row all fail with the legacy messages instead of reporting success.
     */
    @Test
    @DisplayName("result and cleanup marks write the exact legacy column sets")
    @SuppressWarnings("unchecked")
    void resultAndCleanupMarksWriteTheExactLegacyColumnSets() {
        when(releasePublicationDAO.selectOne(any(), anyBoolean())).thenReturn(
                publicationRow(PUBLICATION_ID, GatewayPublicationStatusEnum.SUBMITTED)
        );
        when(releasePublicationDAO.update(any(GatewayReleasePublicationRecordPO.class), any())).thenReturn(1);

        publications.markResult(
                CHANGE_ID,
                77L,
                GatewayPublicationStatusEnum.SUCCESS,
                null,
                null,
                NOW
        );

        ArgumentCaptor<LambdaQueryWrapper<GatewayReleasePublicationRecordPO>> recordable =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releasePublicationDAO).selectOne(recordable.capture(), anyBoolean());
        assertThat(recordable.getValue().getTargetSql())
                .contains("change_id = ?")
                .contains("ddc_status IN");
        assertThat(recordable.getValue().getParamNameValuePairs().values())
                .contains(
                        GatewayPublicationStatusEnum.RESOLVED.name(),
                        GatewayPublicationStatusEnum.SUBMITTED.name(),
                        GatewayPublicationStatusEnum.FAILED.name(),
                        GatewayPublicationStatusEnum.PARTIAL_SUCCESS.name(),
                        GatewayPublicationStatusEnum.TIMEOUT.name(),
                        GatewayPublicationStatusEnum.UNKNOWN.name())
                .doesNotContain(GatewayPublicationStatusEnum.PLANNED.name());
        ArgumentCaptor<GatewayReleasePublicationRecordPO> resultToken =
                ArgumentCaptor.forClass(GatewayReleasePublicationRecordPO.class);
        ArgumentCaptor<LambdaUpdateWrapper<GatewayReleasePublicationRecordPO>> result =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(releasePublicationDAO).update(resultToken.capture(), result.capture());
        assertThat(resultToken.getValue().getVersion()).isEqualTo(31L);
        assertThat(resultToken.getValue().getUpdateTime()).isEqualTo(NOW);
        assertThat(result.getValue().getSqlSet())
                .contains("ddc_target_version=")
                .contains("ddc_status=")
                .contains("error_code=")
                .contains("error_message=");
        assertThat(result.getValue().getTargetSql())
                .contains("id = ?")
                .contains("change_id = ?");
        assertThat(result.getValue().getParamNameValuePairs().values())
                .as("a successful result clears both error columns explicitly, like the legacy SET NULL")
                .contains(77L, GatewayPublicationStatusEnum.SUCCESS.name())
                .containsNull();

        clearInvocations(releasePublicationDAO);
        when(releasePublicationDAO.update(any(GatewayReleasePublicationRecordPO.class), any()))
                .thenReturn(0);
        assertThatThrownBy(() -> publications.markResult(
                CHANGE_ID,
                77L,
                GatewayPublicationStatusEnum.FAILED,
                "YUHENG_RELEASE_PUBLISH_FAILED",
                "engine rejected the chunk",
                NOW
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("publication result cannot be recorded");

        assertThatThrownBy(() -> publications.markResult(
                CHANGE_ID,
                77L,
                GatewayPublicationStatusEnum.SUBMITTED,
                "CODE",
                "message",
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("publication result status must be terminal");
        assertThatThrownBy(() -> publications.markResult(
                CHANGE_ID,
                null,
                GatewayPublicationStatusEnum.SUCCESS,
                null,
                null,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("successful publication requires targetVersion");
        assertThatThrownBy(() -> publications.markResult(
                CHANGE_ID,
                77L,
                null,
                null,
                null,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("publication result status must be terminal");

        clearInvocations(releasePublicationDAO);
        when(releasePublicationDAO.selectOne(any(), anyBoolean())).thenReturn(
                publicationRow(PUBLICATION_ID, GatewayPublicationStatusEnum.SUCCESS)
        );
        when(releasePublicationDAO.update(any(GatewayReleasePublicationRecordPO.class), any())).thenReturn(1);
        publications.markChunkCleaned(CHANGE_ID, NOW);
        ArgumentCaptor<LambdaQueryWrapper<GatewayReleasePublicationRecordPO>> cleaned =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releasePublicationDAO).selectOne(cleaned.capture(), anyBoolean());
        assertThat(cleaned.getValue().getTargetSql())
                .contains("change_id = ?")
                .contains("phase_type = ?")
                .contains("ddc_status = ?")
                .contains("ddc_target_version IS NOT NULL");
        assertThat(cleaned.getValue().getParamNameValuePairs().values())
                .containsExactlyInAnyOrder(
                        CHANGE_ID,
                        GatewayPublicationPhaseEnum.CHUNK.name(),
                        GatewayPublicationStatusEnum.SUCCESS.name());
        ArgumentCaptor<LambdaUpdateWrapper<GatewayReleasePublicationRecordPO>> marker =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(releasePublicationDAO).update(any(GatewayReleasePublicationRecordPO.class), marker.capture());
        assertThat(marker.getValue().getSqlSet())
                .contains("error_code=")
                .contains("error_message=");
        assertThat(marker.getValue().getParamNameValuePairs().values())
                .as("the legacy SQL cleared error_message to NULL in the same statement")
                .contains("CHUNK_GC_DELETED")
                .containsNull();
        assertThat(marker.getValue().getTargetSql())
                .contains("id = ?")
                .contains("change_id = ?");

        clearInvocations(releasePublicationDAO);
        when(releasePublicationDAO.update(any(GatewayReleasePublicationRecordPO.class), any()))
                .thenReturn(0);
        assertThatThrownBy(() -> publications.markChunkCleaned(CHANGE_ID, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cleaned chunk publication was not found");

        clearInvocations(releasePublicationDAO);
        GatewayReleasePublicationRecordPO foreignRow =
                publicationRow(FOREIGN_PUBLICATION_ID, GatewayPublicationStatusEnum.SUCCESS);
        foreignRow.setTenantId(FOREIGN_TENANT);
        when(releasePublicationDAO.selectOne(any(), anyBoolean())).thenReturn(foreignRow);
        assertThatThrownBy(() -> publications.markChunkCleaned(CHANGE_ID, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISMATCH");
        verify(releasePublicationDAO, never()).update(any(GatewayReleasePublicationRecordPO.class), any());
    }

    /**
     * 中文说明：{@code findChunkCleanupCandidates} 保留旧三表联查的全部谓词：只有 {@code CHUNK} 阶段、带作用域、
     * 已 {@code SUCCESS} 且有目标版本、未被 {@code CHUNK_GC_DELETED} 标记的日志行才成为候选（回收标记的排除写在
     * 定位谓词上，{@code error_code <> 'CHUNK_GC_DELETED' OR error_code IS NULL}），且必须存在同组、创建更晚、
     * 在截止时间前成功激活<b>同一引擎角色</b>后继——同一次尝试里的另一角色不得互相顶替（重试与回收永不跨角色）。
     * English summary: {@code findChunkCleanupCandidates} keeps every predicate of the legacy three-table join: only a {@code CHUNK}
     * journal row that carries a scope, is {@code SUCCESS} with a target version and lacks the {@code CHUNK_GC_DELETED} mark becomes a
     * candidate (the marker exclusion stays on the locating predicate as {@code error_code <> 'CHUNK_GC_DELETED' OR error_code IS
     * NULL}), and it requires a successor of the same Group, created later, whose ACTIVATION for the very <b>same engine role</b>
     * succeeded before the deadline — the other role of the same attempt may never stand in for it (retry and GC never cross roles).
     */
    @Test
    @DisplayName("chunk cleanup requires an activated successor of the same engine role")
    @SuppressWarnings("unchecked")
    void chunkCleanupRequiresAnActivatedSuccessorOfTheSameEngineRole() {
        List<GatewayReleasePublicationRecordPO> chunks = List.of(
                cleanupRow(PUBLICATION_ID, RELEASE_ID, 0, GatewayEngineRoleEnum.API_RPC, "chg-chunk-api"),
                cleanupRow(9502L, RELEASE_ID, 1, GatewayEngineRoleEnum.MCP, "chg-chunk-mcp")
        );
        List<GatewayReleasePublicationRecordPO> successorActivations = List.of(
                cleanupRow(9503L, SUCCESSOR_RELEASE_ID, 0, GatewayEngineRoleEnum.API_RPC,
                        "chg-activation-successor")
                        .setPhaseType(GatewayPublicationPhaseEnum.ACTIVATION.name())
        );
        when(releasePublicationDAO.selectList(any())).thenReturn(chunks, successorActivations);
        when(releaseDAO.selectActiveByIds(any())).thenReturn(List.of(
                releaseHead(RELEASE_ID, GatewayReleaseStatus.SUPERSEDED,
                        NOW.minus(Duration.ofHours(2)), 41L)
        ));
        when(releaseDAO.selectList(any())).thenReturn(List.of(
                releaseHead(RELEASE_ID, GatewayReleaseStatus.SUPERSEDED,
                        NOW.minus(Duration.ofHours(2)), 41L),
                releaseHead(SUCCESSOR_RELEASE_ID, GatewayReleaseStatus.SUCCESS,
                        NOW.minus(Duration.ofHours(1)), 42L)
        ));
        when(draftDAO.selectList(any())).thenReturn(List.of());
        when(releaseContentDAO.selectList(any())).thenReturn(List.of(
                releaseContent(RELEASE_ID)
        ));

        List<GatewayChunkCleanupCandidateBO> candidates =
                publications.findChunkCleanupCandidates(NOW);

        assertThat(candidates)
                .as("the MCP chunk of the very same attempt has no activated MCP successor")
                .singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.getChangeId()).isEqualTo("chg-chunk-api");
                    assertThat(candidate.getReleaseId()).isEqualTo(Long.toString(RELEASE_ID));
                    assertThat(candidate.getConfigKey()).isEqualTo(CHUNK_KEY);
                    assertThat(candidate.getNamespace()).isEqualTo(NAMESPACE);
                    assertThat(candidate.getAppCode()).isEqualTo(APP_CODE);
                    assertThat(candidate.getEnv()).isEqualTo(ENV);
                    assertThat(candidate.getTargetVersion()).isEqualTo(41L);
                    assertThat(candidate.getTargetScope().engineRole())
                            .isEqualTo(GatewayEngineRoleEnum.API_RPC);
                });

        ArgumentCaptor<LambdaQueryWrapper<GatewayReleasePublicationRecordPO>> reads =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releasePublicationDAO, times(2)).selectList(reads.capture());
        assertThat(reads.getAllValues().get(0).getTargetSql())
                .contains("phase_type = ?")
                .contains("target_role IS NOT NULL")
                .contains("ddc_status = ?")
                .contains("ddc_target_version IS NOT NULL")
                .contains("error_code <> ?")
                .contains("error_code IS NULL");
        assertThat(reads.getAllValues().get(0).getParamNameValuePairs().values())
                .contains(
                        GatewayPublicationPhaseEnum.CHUNK.name(),
                        GatewayPublicationStatusEnum.SUCCESS.name(),
                        "CHUNK_GC_DELETED");
        assertThat(reads.getAllValues().get(1).getTargetSql())
                .contains("release_id IN")
                .contains("phase_type = ?")
                .contains("ddc_status = ?")
                .contains("target_role IS NOT NULL")
                .contains("update_time <=");
        assertThat(reads.getAllValues().get(1).getParamNameValuePairs().values())
                .as("the legacy activation side of the EXISTS keeps its deadline")
                .contains(
                        SUCCESSOR_RELEASE_ID,
                        GatewayPublicationPhaseEnum.ACTIVATION.name(),
                        GatewayPublicationStatusEnum.SUCCESS.name(),
                        NOW);
        ArgumentCaptor<List<Long>> heads = ArgumentCaptor.forClass(List.class);
        verify(releaseDAO).selectActiveByIds(heads.capture());
        assertThat(heads.getValue()).containsExactly(RELEASE_ID);
        ArgumentCaptor<LambdaQueryWrapper<GatewayReleaseRecordPO>> groupScope =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releaseDAO).selectList(groupScope.capture());
        assertThat(groupScope.getValue().getTargetSql()).contains("gateway_group_id IN");
        assertThat(groupScope.getValue().getParamNameValuePairs().values()).contains(GATEWAY_GROUP_ID);
        ArgumentCaptor<LambdaQueryWrapper<GatewayDraftRecordPO>> drafts =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(draftDAO).selectList(drafts.capture());
        assertThat(drafts.getValue().getTargetSql()).contains("based_on_release_id IN");
        assertThat(drafts.getValue().getParamNameValuePairs().values()).contains(Long.toString(RELEASE_ID));
        ArgumentCaptor<LambdaQueryWrapper<GatewayReleaseContentPO>> content =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releaseContentDAO).selectList(content.capture());
        assertThat(content.getValue().getTargetSql()).contains("release_id IN");

        clearInvocations(releaseDAO, releaseContentDAO, releasePublicationDAO, draftDAO);
        when(releasePublicationDAO.selectList(any())).thenReturn(chunks, successorActivations);
        when(releaseDAO.selectList(any())).thenReturn(List.of(
                releaseHead(RELEASE_ID, GatewayReleaseStatus.SUPERSEDED,
                        NOW.minus(Duration.ofHours(2)), 41L),
                releaseHead(SUCCESSOR_RELEASE_ID, GatewayReleaseStatus.PUBLISHING,
                        NOW.minus(Duration.ofHours(1)), 42L)
        ));
        assertThat(publications.findChunkCleanupCandidates(NOW))
                .as("a release still in flight in the same Group blocks every cleanup")
                .isEmpty();

        clearInvocations(releaseDAO, releaseContentDAO, releasePublicationDAO, draftDAO);
        when(releasePublicationDAO.selectList(any())).thenReturn(chunks, successorActivations);
        when(releaseDAO.selectList(any())).thenReturn(List.of(
                releaseHead(RELEASE_ID, GatewayReleaseStatus.SUPERSEDED,
                        NOW.minus(Duration.ofHours(2)), 41L),
                releaseHead(SUCCESSOR_RELEASE_ID, GatewayReleaseStatus.SUCCESS,
                        NOW.minus(Duration.ofHours(1)), 42L)
        ));
        when(draftDAO.selectList(any())).thenReturn(List.of(draftRow(6001L, Long.toString(RELEASE_ID))));
        assertThat(publications.findChunkCleanupCandidates(NOW))
                .as("a draft still based on the release blocks its chunk")
                .isEmpty();

        clearInvocations(releaseDAO, releaseContentDAO, releasePublicationDAO, draftDAO);
        when(releasePublicationDAO.selectList(any())).thenReturn(List.of());
        assertThat(publications.findChunkCleanupCandidates(NOW)).isEmpty();
        verify(releaseDAO, never()).selectActiveByIds(any());
        verify(releaseDAO, never()).selectList(any());
        verify(draftDAO, never()).selectList(any());
        verify(releaseContentDAO, never()).selectList(any());
    }

    /**
     * 中文说明：{@code attempts} 与 {@code latestAttempt} 一样按发布整体读取日志，再按 {@code attempt_no DESC} 归并，
     * 每个 attempt 只聚合自己 {@code (attempt_no, instance_id, lease_id)} 下的实例目标：重试不继承上一次的观测，
     * 目标行的引擎角色也只在读取到的行上呈现；非十进制编号如实返回空且不下发语句。
     * English summary: {@code attempts} reads the journal per release exactly like {@code latestAttempt} and merges it by
     * {@code attempt_no DESC}, and every attempt aggregates only the instance targets of its own
     * {@code (attempt_no, instance_id, lease_id)}: a retry never inherits the previous attempt's observations and the engine role is
     * reported only on the row that was actually read, while a non-decimal identifier truthfully yields nothing without a statement.
     */
    @Test
    @DisplayName("the attempt journal never mixes attempt numbers or engine roles")
    @SuppressWarnings("unchecked")
    void attemptJournalNeverMixesAttemptNumbersOrEngineRoles() {
        when(releaseTargetDAO.selectList(any())).thenReturn(List.of(
                targetRow(9401L, 1, GatewayEngineRoleEnum.API_RPC, 41L, 11L),
                targetRow(9404L, 2, GatewayEngineRoleEnum.API_RPC, 42L, 14L),
                targetRow(9405L, 2, GatewayEngineRoleEnum.MCP, 42L, 15L)
        ));
        when(releaseAttemptDAO.selectList(any())).thenReturn(List.of(
                attemptRow(9302L, 2, "PUBLISHING", 12L),
                attemptRow(ATTEMPT_ID, 1, "SUCCESS", 11L)
        ));

        List<GatewayReleaseAttemptBO> journal = releases.attempts(Long.toString(RELEASE_ID));

        assertThat(journal)
                .as("the legacy ORDER BY attempt_no DESC ordering is kept")
                .extracting(GatewayReleaseAttemptBO::getAttemptNo)
                .containsExactly(2, 1);
        assertThat(journal.getFirst().getTargets())
                .as("attempt 2 reports its own two observations, never attempt 1's")
                .hasSize(2)
                .extracting(GatewayReleaseTargetBO::getAppliedVersion)
                .containsExactly(42L, 42L);
        assertThat(journal.getFirst().getTargets())
                .extracting(GatewayReleaseTargetBO::getEngineRole)
                .containsExactly(GatewayEngineRoleEnum.API_RPC, GatewayEngineRoleEnum.MCP);
        assertThat(journal.getLast().getTargets())
                .as("a superseded attempt keeps exactly the roles it observed")
                .singleElement()
                .satisfies(target -> {
                    assertThat(target.getEngineRole()).isEqualTo(GatewayEngineRoleEnum.API_RPC);
                    assertThat(target.getAppliedVersion()).isEqualTo(41L);
                    assertThat(target.getAppliedArtifactSha256()).isEqualTo(ARTIFACT_SHA);
                });

        ArgumentCaptor<LambdaQueryWrapper<GatewayReleaseTargetRecordPO>> targetRead =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releaseTargetDAO).selectList(targetRead.capture());
        assertThat(targetRead.getValue().getTargetSql())
                .contains("release_id = ?")
                .doesNotContain("attempt_no = ?")
                .contains("attempt_no ASC")
                .contains("instance_id ASC")
                .contains("lease_id ASC")
                .contains("id ASC");
        assertThat(targetRead.getValue().getParamNameValuePairs().values())
                .containsExactlyInAnyOrder(RELEASE_ID);
        ArgumentCaptor<LambdaQueryWrapper<GatewayReleaseAttemptRecordPO>> journalRead =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releaseAttemptDAO).selectList(journalRead.capture());
        assertThat(journalRead.getValue().getTargetSql())
                .contains("release_id = ?")
                .contains("attempt_no DESC")
                .contains("id DESC");

        clearInvocations(releaseTargetDAO, releaseAttemptDAO);
        assertThat(releases.attempts("release-legacy")).isEmpty();
        verify(releaseTargetDAO, never()).selectList(any());
        verify(releaseAttemptDAO, never()).selectList(any());
    }

    /**
     * 中文说明：{@code findAttempt}/{@code nextIncomplete}（以及端口的两个默认委托）都保留旧
     * {@code WHERE release_id = ? AND attempt_no = ? ORDER BY phase_order} 与 {@code ddc_status <> 'SUCCESS'}：重试只读自己
     * 那一次尝试的操作集，绝不跨 attempt 复用日志行，也不跨角色挑下一个未完成的操作。
     * English summary: {@code findAttempt} and {@code nextIncomplete} (plus the port's two default delegations) keep the legacy
     * {@code WHERE release_id = ? AND attempt_no = ? ORDER BY phase_order} and {@code ddc_status <> 'SUCCESS'}: a retry reads exactly
     * the operation set of its own attempt, never reusing a journal row of another attempt and never picking the next incomplete
     * operation of another role.
     */
    @Test
    @DisplayName("publication reads pin the release and the attempt they retry")
    @SuppressWarnings("unchecked")
    void publicationReadsPinTheReleaseAndTheAttemptTheyRetry() {
        List<GatewayReleasePublicationRecordPO> attemptOne = List.of(
                publicationRow(PUBLICATION_ID, GatewayPublicationStatusEnum.PLANNED),
                publicationRow(9502L, GatewayPublicationStatusEnum.SUCCESS)
                        .setPhaseOrder(1)
                        .setChangeId("chg-9101-1-mcp")
                        .setTargetRole(GatewayEngineRoleEnum.MCP.name())
        );
        when(releasePublicationDAO.selectList(any())).thenReturn(attemptOne);

        assertThat(publications.findAttempt(Long.toString(RELEASE_ID), 1))
                .extracting(GatewayReleasePublicationBO::getPhaseOrder)
                .containsExactly(0, 1);
        assertThat(publications.findAttemptMetadata(Long.toString(RELEASE_ID), 1))
                .as("the metadata read keeps the same caller-visible shape")
                .extracting(GatewayReleasePublicationBO::getPhaseOrder)
                .containsExactly(0, 1);
        assertThat(publications.findOperation(Long.toString(RELEASE_ID), 1, 1))
                .get()
                .extracting(GatewayReleasePublicationBO::getChangeId)
                .isEqualTo("chg-9101-1-mcp");
        assertThat(publications.findOperation(Long.toString(RELEASE_ID), 1, 9)).isEmpty();
        assertThat(publications.nextIncomplete(Long.toString(RELEASE_ID), 1))
                .as("the first not-yet-successful operation of this attempt wins")
                .get()
                .extracting(GatewayReleasePublicationBO::getChangeId)
                .isEqualTo(CHANGE_ID);

        ArgumentCaptor<LambdaQueryWrapper<GatewayReleasePublicationRecordPO>> reads =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(releasePublicationDAO, times(5)).selectList(reads.capture());
        reads.getAllValues().forEach(read -> {
            assertThat(read.getTargetSql())
                    .contains("release_id = ?")
                    .contains("attempt_no = ?");
            assertThat(read.getParamNameValuePairs().values())
                    .as("a retry never reaches into another attempt's journal rows")
                    .contains(RELEASE_ID, 1L);
        });
        assertThat(reads.getAllValues().get(0).getTargetSql())
                .doesNotContain("ddc_status")
                .contains("phase_order ASC")
                .contains("id ASC");
        assertThat(reads.getAllValues().get(4).getTargetSql())
                .contains("ddc_status <> ?")
                .contains("phase_order ASC");
        assertThat(reads.getAllValues().get(4).getParamNameValuePairs().values())
                .contains(GatewayPublicationStatusEnum.SUCCESS.name());

        clearInvocations(releasePublicationDAO);
        assertThat(publications.findAttempt("release-legacy", 1)).isEmpty();
        assertThat(publications.nextIncomplete("release-legacy", 1)).isEmpty();
        verify(releasePublicationDAO, never()).selectList(any());
    }

    /**
     * 中文说明：注册 MP 行模型以填充 lambda 列缓存，使门面里的 {@code Wrappers.lambdaQuery()} 可渲染为 SQL。
     * English summary: Registers a MyBatis-Plus row model so the lambda column cache exists and the facade's
     * {@code Wrappers.lambdaQuery()} can be rendered into SQL.
     * @param rowModel 参数 行模型类型；parameter the row model type.
     */
    private static void registerRowModel(Class<?> rowModel) {
        if (TableInfoHelper.getTableInfo(rowModel) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), rowModel.getName()),
                    rowModel
            );
        }
    }

    /**
     * 中文说明：构造调用方提交的 DISCOVERED 观察载体（业务 revision 由入参给定）。
     * English summary: Builds the DISCOVERED observation carrier the caller submits, with the business revision supplied by
     * the argument.
     * @param revision 参数 业务 revision；parameter the business revision.
     * @return 返回 同步载体；returns the synchronization carrier.
     */
    private static GatewayOpenApiSyncBO observation(long revision) {
        return GatewayOpenApiSyncBO.validated(GatewayOpenApiSyncBO.builder()
                .id(Long.toString(SYNC_ID))
                .applicationId(Long.toString(APPLICATION_ID))
                .buildId(BUILD_ID)
                .artifactVersion(OBSERVED_ARTIFACT)
                .openapiGroup(GROUP)
                .providerServiceName("yuheng-provider")
                .providerGroup("infra")
                .providerVersion("2.0.0")
                .status(GatewayOpenApiSyncStateEnum.DISCOVERED)
                .lastInstanceId("instance-1")
                .attemptCount(0)
                .firstDiscoveredAt(NOW)
                .revision(revision)
                .updatedAt(NOW)
                .build());
    }

    /**
     * 中文说明：构造调用方提交的不可变快照载体（四列业务契约与库内行一致时才可复用）。
     * English summary: Builds the immutable snapshot carrier the caller submits, contract-compatible with the stored row
     * when the four business-contract columns match.
     * @param definitionSetId 参数 聚合 Definition Set 标识或 {@code null}；parameter the aggregate identifier, or {@code null}.
     * @return 返回 快照载体；returns the snapshot carrier.
     */
    private static GatewayOpenApiSnapshotBO snapshotCarrier(Long definitionSetId) {
        return GatewayOpenApiSnapshotBO.builder()
                .id(Long.toString(SNAPSHOT_ID))
                .applicationId(Long.toString(APPLICATION_ID))
                .definitionSetId(definitionSetId == null ? null : Long.toString(definitionSetId))
                .buildId(BUILD_ID)
                .artifactVersion(ARTIFACT)
                .openapiGroup(GROUP)
                .openapiVersion("3.1.0")
                .documentSha256(DOCUMENT_SHA)
                .canonicalSha256(CANONICAL_SHA)
                .documentJson(DOCUMENT)
                .validationStatus("VALID")
                .validationMessages(List.of())
                .operationCount(12)
                .schemaCount(4)
                .fetchedFromInstanceId("instance-1")
                .fetchedAt(NOW.minus(Duration.ofMinutes(5)))
                .validatedAt(NOW.minus(Duration.ofMinutes(4)))
                .createdAt(NOW.minus(Duration.ofMinutes(5)))
                .build();
    }

    /**
     * 中文说明：构造一条技术列齐备的活跃快照行，作为受守卫读取的 mapper 返回值。
     * English summary: Builds a fully-populated active snapshot row, the value the guarded read gets back from the mapper.
     * @param tenantId 参数 归属租户；parameter the owning tenant.
     * @param definitionSetId 参数 已链接的聚合标识或 {@code null}；parameter the linked aggregate identifier, or {@code null}.
     * @param version 参数 技术 version；parameter the technical version.
     * @return 返回 快照活跃行；returns the active snapshot row.
     */
    private static GatewayOpenApiSnapshotRecordPO snapshotRow(
            long tenantId,
            Long definitionSetId,
            long version) {
        return GatewayOpenApiSnapshotRecordPO.builder()
                .id(SNAPSHOT_ID)
                .tenantId(tenantId)
                .applicationId(APPLICATION_ID)
                .definitionSetId(definitionSetId)
                .buildId(BUILD_ID)
                .artifactVersion(ARTIFACT)
                .openapiGroup(GROUP)
                .openapiVersion("3.1.0")
                .documentSha256(DOCUMENT_SHA)
                .canonicalSha256(CANONICAL_SHA)
                .documentJson(DOCUMENT_NODE)
                .validationStatus("VALID")
                .validationMessages(MESSAGES_NODE)
                .operationCount(12)
                .schemaCount(4)
                .fetchedFromInstanceId("instance-1")
                .fetchedAt(NOW.minus(Duration.ofMinutes(5)))
                .validatedAt(NOW.minus(Duration.ofMinutes(4)))
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(NOW.minus(Duration.ofMinutes(5)))
                .updateTime(NOW.minus(Duration.ofMinutes(5)))
                .version(version)
                .build();
    }

    /**
     * 中文说明：构造一条技术列齐备的活跃同步 journal 行。
     * English summary: Builds a fully-populated active synchronization journal row.
     * @param id 参数 行主键；parameter the row identifier.
     * @param state 参数 生命周期状态；parameter the lifecycle state.
     * @param revision 参数 发布业务 revision；parameter the release business revision.
     * @param attemptCount 参数 尝试计数；parameter the attempt counter.
     * @param nextRetryAt 参数 下次重试时刻或 {@code null}；parameter the scheduled retry, or {@code null}.
     * @param version 参数 技术 version；parameter the technical version.
     * @return 返回 同步 journal 行；returns the active journal row.
     */
    private static GatewayOpenApiSyncRecordPO journalRow(
            long id,
            GatewayOpenApiSyncStateEnum state,
            long revision,
            int attemptCount,
            Instant nextRetryAt,
            long version) {
        return GatewayOpenApiSyncRecordPO.builder()
                .id(id)
                .tenantId(TRUSTED_TENANT)
                .applicationId(APPLICATION_ID)
                .buildId(BUILD_ID)
                .artifactVersion(ARTIFACT)
                .openapiGroup(GROUP)
                .providerServiceName("yuheng-provider")
                .providerGroup("infra")
                .providerVersion("2.0.0")
                .status(state.wireValue())
                .attemptCount(attemptCount)
                .firstDiscoveredAt(NOW.minus(Duration.ofHours(1)))
                .nextRetryAt(nextRetryAt)
                .revision(revision)
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(NOW.minus(Duration.ofHours(1)))
                .updateTime(NOW.minus(Duration.ofMinutes(10)))
                .version(version)
                .build();
    }

    /**
     * 中文说明：构造调用方提交的发布头载体；技术编号与组编号都按端口以十进制文本携带，两个 jsonb 列以结构化
     * 映射提交，创建时刻即首个尝试的 {@code started_at}。
     * English summary: Builds the release-head carrier the caller submits; both the technical and the group identifier travel as
     * decimal text exactly as the port exposes them, the two jsonb columns are submitted as structured maps, and the creation
     * instant is what the first attempt journals as {@code started_at}.
     * @param status 参数 发布状态；parameter the release status.
     * @return 返回 发布头载体；returns the release head carrier.
     */
    private static GatewayReleaseBO releaseCarrier(GatewayReleaseStatus status) {
        return GatewayReleaseBO.builder()
                .id(Long.toString(RELEASE_ID))
                .gatewayGroupId(Long.toString(GATEWAY_GROUP_ID))
                .draftRevision(12L)
                .status(status)
                .partialApplied(false)
                .validationReport(Map.of("valid", true))
                .structuredDiff(Map.of("operations", List.of("op-1")))
                .changeReason("release the compiled rules")
                .createdAt(RELEASE_CREATED_AT)
                .createdBy("actor-1")
                .updatedAt(RELEASE_CREATED_AT)
                .build();
    }

    /**
     * 中文说明：构造编译产物：规范快照 JSON、分块激活 JSON 与分块正文，三者共享同一规则摘要与制品摘要
     * （登记时正是这份摘要被冻结进内容快照列，并随后冻结成两个引擎角色的目标行）。
     * English summary: Builds the compiled artifact: the canonical snapshot JSON, the chunked activation JSON and the chunk payload,
     * all sharing one rule digest and one artifact digest (exactly the digests registration freezes onto the content snapshot and
     * that later become the frozen rows of both engine roles).
     * @return 返回 编译产物；returns the compiled release artifact.
     */
    private static CompiledGatewayRelease compiledRelease() {
        GatewayRuleContent content = new GatewayRuleContent(
                Long.toString(GATEWAY_GROUP_ID),
                "yuheng-core",
                ENV,
                NAMESPACE,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        GatewayRuleSnapshot snapshot = new GatewayRuleSnapshot(
                "1.0",
                Long.toString(RELEASE_ID),
                RELEASE_CREATED_AT,
                RULE_SHA,
                ARTIFACT_SHA,
                content);
        GatewayRuleActivation activation = new GatewayRuleActivation(
                "1.0",
                Long.toString(RELEASE_ID),
                GatewayRuleActivationMode.CHUNKED,
                "1.0",
                41,
                RULE_SHA,
                ARTIFACT_SHA,
                null,
                List.of(new GatewayRuleChunkRef(CHUNK_KEY, 0, 41, CHUNK_SHA)));
        return new CompiledGatewayRelease(
                snapshot,
                SNAPSHOT_JSON,
                activation,
                ACTIVATION_JSON,
                Map.of(CHUNK_KEY, CHUNK_VALUE));
    }

    /**
     * 中文说明：构造冻结的发布目标作用域（业务码、环境、应用码与引擎角色四列）。
     * English summary: Builds the frozen publication target scope, the four columns business code, environment, application code and
     * engine role.
     * @param engineRole 参数 引擎角色；parameter the frozen engine role.
     * @return 返回 目标作用域；returns the target scope.
     */
    private static GatewayPublicationScopeDTO scope(GatewayEngineRoleEnum engineRole) {
        return new GatewayPublicationScopeDTO(BIZ_CODE, ENV, APP_CODE, engineRole);
    }

    /**
     * 中文说明：构造调用方登记的一条发布操作：同一发布、同一尝试、尚无期望版本与目标版本（未解析），作用域由入参
     * 冻结。
     * English summary: Builds one publication operation the caller registers: one release, one attempt, neither an expected nor a
     * target version yet (unresolved), with the scope frozen from the argument.
     * @param phaseOrder 参数 阶段序号；parameter the phase order.
     * @param phaseType 参数 阶段类型；parameter the phase type.
     * @param configKey 参数 配置键；parameter the configuration key.
     * @param contentValue 参数 冻结正文；parameter the frozen payload.
     * @param contentSha256 参数 正文摘要；parameter the payload digest.
     * @param targetScope 参数 冻结目标作用域；parameter the frozen target scope.
     * @return 返回 发布操作载体；returns the publication operation carrier.
     */
    private static GatewayReleasePublicationBO publicationCarrier(
            int phaseOrder,
            GatewayPublicationPhaseEnum phaseType,
            String configKey,
            String contentValue,
            String contentSha256,
            GatewayPublicationScopeDTO targetScope) {
        return GatewayReleasePublicationBO.builder()
                .releaseId(Long.toString(RELEASE_ID))
                .attemptNo(1)
                .phaseOrder(phaseOrder)
                .phaseType(phaseType)
                .configKey(configKey)
                .contentValue(contentValue)
                .contentSha256(contentSha256)
                .changeId(CHANGE_ID + "-" + phaseOrder)
                .status(GatewayPublicationStatusEnum.PLANNED)
                .targetScope(targetScope)
                .build();
    }

    /**
     * 中文说明：构造一条技术列齐备的发布头活跃行（受守卫读取要求租户、审计与版本列齐备）。
     * English summary: Builds a fully-populated active release head; a guarded read demands the tenant, audit and version columns.
     * @param id 参数 发布技术编号；parameter the technical release identifier.
     * @param status 参数 发布状态；parameter the release status.
     * @param createTime 参数 创建时刻，后继判定与排序都读它；parameter the creation instant the successor test and the ordering read.
     * @param version 参数 技术 version，即 CAS 令牌；parameter the technical version, the compare-and-set token.
     * @return 返回 发布头活跃行；returns the active release head row.
     */
    private static GatewayReleaseRecordPO releaseHead(
            long id,
            GatewayReleaseStatus status,
            Instant createTime,
            long version) {
        return GatewayReleaseRecordPO.builder()
                .id(id)
                .gatewayGroupId(GATEWAY_GROUP_ID)
                .draftRevision(12L)
                .status(status.name())
                .partialApplied(false)
                .validationReport(JSON.valueToTree(Map.of("valid", true)))
                .structuredDiff(JSON.valueToTree(Map.of("operations", List.of("op-1"))))
                .changeReason("release the compiled rules")
                .tenantId(TRUSTED_TENANT)
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(createTime)
                .updateTime(createTime)
                .version(version)
                .build();
    }

    /**
     * 中文说明：构造一条内容快照活跃行；{@code canonical_snapshot} 带 {@code content.namespace}，正是旧 SQL JSON 路径
     * 投影读取的那一列。
     * English summary: Builds an active content snapshot row whose {@code canonical_snapshot} carries {@code content.namespace}, the
     * very member the legacy JSON-path projection read.
     * @param releaseId 参数 归属发布技术编号；parameter the owning technical release identifier.
     * @return 返回 内容快照活跃行；returns the active content snapshot row.
     */
    private static GatewayReleaseContentPO releaseContent(long releaseId) {
        return GatewayReleaseContentPO.builder()
                .id(releaseId)
                .releaseId(releaseId)
                .ruleContentSha256(RULE_SHA)
                .artifactSha256(ARTIFACT_SHA)
                .canonicalSnapshot(JSON.valueToTree(SNAPSHOT_DOCUMENT))
                .activationContent(JSON.valueToTree(ACTIVATION_DOCUMENT))
                .chunkManifest(JSON.valueToTree(Map.of(CHUNK_KEY, CHUNK_VALUE)))
                .snapshotSize((long) SNAPSHOT_JSON.getBytes(StandardCharsets.UTF_8).length)
                .tenantId(TRUSTED_TENANT)
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(RELEASE_CREATED_AT)
                .updateTime(RELEASE_CREATED_AT)
                .version(5L)
                .build();
    }

    /**
     * 中文说明：构造一条尝试日志活跃行；上一轮遗留的 {@code completed_at/error_*} 故意有值，以便 {@code beginAttempt}
     * 的显式清空可见。
     * English summary: Builds an active attempt-journal row that deliberately still carries the previous round's
     * {@code completed_at/error_*} so the explicit clearing of {@code beginAttempt} stays visible.
     * @param id 参数 日志行主键；parameter the journal row identifier.
     * @param attemptNo 参数 尝试序号；parameter the attempt number.
     * @param status 参数 尝试状态字面量；parameter the attempt status literal.
     * @param version 参数 技术 version；parameter the technical version.
     * @return 返回 尝试日志活跃行；returns the active attempt journal row.
     */
    private static GatewayReleaseAttemptRecordPO attemptRow(
            long id,
            int attemptNo,
            String status,
            long version) {
        return GatewayReleaseAttemptRecordPO.builder()
                .id(id)
                .releaseId(RELEASE_ID)
                .attemptNo(attemptNo)
                .status(status)
                .startedAt(RELEASE_CREATED_AT)
                .completedAt(RELEASE_CREATED_AT.plus(Duration.ofMinutes(1)))
                .errorCode("PREVIOUS_FAILURE")
                .errorMessage("the previous round failed")
                .tenantId(TRUSTED_TENANT)
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(RELEASE_CREATED_AT)
                .updateTime(RELEASE_CREATED_AT)
                .version(version)
                .build();
    }

    /**
     * 中文说明：构造一条实例目标观测活跃行，按 {@code (release_id, attempt_no, instance_id, lease_id)} 业务键归属。
     * English summary: Builds an active instance target observation row owned by the
     * {@code (release_id, attempt_no, instance_id, lease_id)} business key.
     * @param id 参数 目标行主键；parameter the target row identifier.
     * @param attemptNo 参数 所属尝试序号；parameter the owning attempt number.
     * @param engineRole 参数 引擎角色；parameter the frozen engine role.
     * @param appliedVersion 参数 已应用版本；parameter the applied version.
     * @param version 参数 技术 version；parameter the technical version.
     * @return 返回 实例目标观测活跃行；returns the active instance target row.
     */
    private static GatewayReleaseTargetRecordPO targetRow(
            long id,
            int attemptNo,
            GatewayEngineRoleEnum engineRole,
            long appliedVersion,
            long version) {
        return GatewayReleaseTargetRecordPO.builder()
                .id(id)
                .releaseId(RELEASE_ID)
                .attemptNo((long) attemptNo)
                .instanceId("instance-" + id)
                .leaseId("lease-" + id)
                .status("APPLIED")
                .appliedVersion(appliedVersion)
                .appliedArtifactSha256(ARTIFACT_SHA)
                .observedAt(NOW.minus(Duration.ofMinutes(3)))
                .engineRole(engineRole.name())
                .tenantId(TRUSTED_TENANT)
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(NOW.minus(Duration.ofMinutes(4)))
                .updateTime(NOW.minus(Duration.ofMinutes(3)))
                .version(version)
                .build();
    }

    /**
     * 中文说明：构造调用方上报的实例目标观测；制品摘要即发布登记时冻结的那一份。
     * English summary: Builds the instance target observation the caller reports, against the very artifact digest registration froze.
     * @param instanceId 参数 实例标识；parameter the instance identifier.
     * @param leaseId 参数 租约标识；parameter the lease identifier.
     * @param engineRole 参数 引擎角色；parameter the frozen engine role.
     * @param appliedVersion 参数 已应用版本；parameter the applied version.
     * @return 返回 目标观测载体；returns the target observation carrier.
     */
    private static GatewayReleaseTargetBO targetObservation(
            String instanceId,
            String leaseId,
            GatewayEngineRoleEnum engineRole,
            long appliedVersion) {
        return GatewayReleaseTargetBO.builder()
                .instanceId(instanceId)
                .leaseId(leaseId)
                .status("APPLIED")
                .appliedVersion(appliedVersion)
                .appliedArtifactSha256(ARTIFACT_SHA)
                .observedAt(NOW)
                .engineRole(engineRole)
                .build();
    }

    /**
     * 中文说明：构造一条发布日志活跃行；技术 version 固定为 31，正是每次 CAS 令牌携带的版本。
     * English summary: Builds an active publication journal row whose technical version stays 31, the exact token every
     * compare-and-set carries.
     * @param id 参数 日志行主键；parameter the journal row identifier.
     * @param status 参数 当前 DDC 状态；parameter the current DDC status.
     * @return 返回 发布日志活跃行；returns the active publication journal row.
     */
    private static GatewayReleasePublicationRecordPO publicationRow(
            long id,
            GatewayPublicationStatusEnum status) {
        return GatewayReleasePublicationRecordPO.builder()
                .id(id)
                .releaseId(RELEASE_ID)
                .attemptNo(1L)
                .phaseOrder(0)
                .phaseType(GatewayPublicationPhaseEnum.CHUNK.name())
                .configKey(CHUNK_KEY)
                .contentValue(CHUNK_VALUE)
                .contentSha256(CHUNK_SHA)
                .expectedVersion(41L)
                .changeId(CHANGE_ID)
                .ddcTargetVersion(77L)
                .ddcStatus(status.name())
                .targetRole(GatewayEngineRoleEnum.API_RPC.name())
                .targetBizCode(BIZ_CODE)
                .targetEnv(ENV)
                .targetAppCode(APP_CODE)
                .tenantId(TRUSTED_TENANT)
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(RELEASE_CREATED_AT)
                .updateTime(RELEASE_CREATED_AT)
                .version(31L)
                .build();
    }

    /**
     * 中文说明：构造一条 {@code SUCCESS} 的分块日志活跃行（分块回收候选的输入），角色与变更号由入参决定。
     * English summary: Builds an active {@code SUCCESS} chunk journal row, the input of the cleanup-candidate query, with the role and
     * the change id supplied by the arguments.
     * @param id 参数 日志行主键；parameter the journal row identifier.
     * @param releaseId 参数 归属发布技术编号；parameter the owning technical release identifier.
     * @param phaseOrder 参数 阶段序号；parameter the phase order.
     * @param engineRole 参数 冻结的引擎角色；parameter the frozen engine role.
     * @param changeId 参数 外部变更号；parameter the external change id.
     * @return 返回 分块日志活跃行；returns the active chunk journal row.
     */
    private static GatewayReleasePublicationRecordPO cleanupRow(
            long id,
            long releaseId,
            int phaseOrder,
            GatewayEngineRoleEnum engineRole,
            String changeId) {
        return GatewayReleasePublicationRecordPO.builder()
                .id(id)
                .releaseId(releaseId)
                .attemptNo(1L)
                .phaseOrder(phaseOrder)
                .phaseType(GatewayPublicationPhaseEnum.CHUNK.name())
                .configKey(CHUNK_KEY)
                .contentValue(CHUNK_VALUE)
                .contentSha256(CHUNK_SHA)
                .expectedVersion(41L)
                .changeId(changeId)
                .ddcTargetVersion(41L)
                .ddcStatus(GatewayPublicationStatusEnum.SUCCESS.name())
                .targetRole(engineRole.name())
                .targetBizCode(BIZ_CODE)
                .targetEnv(ENV)
                .targetAppCode(APP_CODE)
                .tenantId(TRUSTED_TENANT)
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(RELEASE_CREATED_AT)
                .updateTime(RELEASE_CREATED_AT)
                .version(41L)
                .build();
    }

    /**
     * 中文说明：构造一条仍以该发布为基线的草稿活跃行（旧 {@code NOT EXISTS} 子查询的阻挡侧）。
     * English summary: Builds an active draft still based on the release, the blocking side of the legacy {@code NOT EXISTS} sub-select.
     * @param id 参数 草稿主键；parameter the draft identifier.
     * @param basedOnReleaseId 参数 基线发布编号（仍为文本列）；parameter the baseline release identifier, still a textual column.
     * @return 返回 草稿活跃行；returns the active draft row.
     */
    private static GatewayDraftRecordPO draftRow(long id, String basedOnReleaseId) {
        return GatewayDraftRecordPO.builder()
                .id(id)
                .gatewayGroupId(GATEWAY_GROUP_ID)
                .revision(3L)
                .basedOnReleaseId(basedOnReleaseId)
                .status("EDITABLE")
                .changeSummary("work on the released baseline")
                .tenantId(TRUSTED_TENANT)
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(RELEASE_CREATED_AT)
                .updateTime(RELEASE_CREATED_AT)
                .version(7L)
                .build();
    }
}
