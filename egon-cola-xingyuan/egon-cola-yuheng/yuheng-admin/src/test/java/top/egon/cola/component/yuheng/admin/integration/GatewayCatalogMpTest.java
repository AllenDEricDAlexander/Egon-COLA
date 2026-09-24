package top.egon.cola.component.yuheng.admin.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.yuheng.admin.application.converter.GatewayApplicationPersistenceConverter;
import top.egon.cola.component.yuheng.admin.application.dao.GatewayApplicationDAO;
import top.egon.cola.component.yuheng.admin.application.domain.bo.GatewayApplicationBO;
import top.egon.cola.component.yuheng.admin.application.domain.po.GatewayApplicationRecordPO;
import top.egon.cola.component.yuheng.admin.application.repository.impl.MpGatewayApplicationRepository;
import top.egon.cola.component.yuheng.admin.application.repository.mp.GatewayApplicationPersistenceRepository;
import top.egon.cola.component.yuheng.admin.credential.converter.GatewayCredentialPersistenceConverter;
import top.egon.cola.component.yuheng.admin.credential.dao.GatewayCredentialDAO;
import top.egon.cola.component.yuheng.admin.credential.domain.po.GatewayCredentialRecordPO;
import top.egon.cola.component.yuheng.admin.credential.repository.impl.MpGatewayCredentialRepository;
import top.egon.cola.component.yuheng.admin.credential.repository.mp.GatewayCredentialPersistenceRepository;
import top.egon.cola.component.yuheng.admin.group.converter.GatewayGroupPersistenceConverter;
import top.egon.cola.component.yuheng.admin.group.dao.GatewayGroupDAO;
import top.egon.cola.component.yuheng.admin.group.domain.bo.GatewayGroupBO;
import top.egon.cola.component.yuheng.admin.group.domain.po.GatewayGroupRecordPO;
import top.egon.cola.component.yuheng.admin.group.repository.impl.MpGatewayGroupRepository;
import top.egon.cola.component.yuheng.admin.group.repository.mp.GatewayGroupPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：{@code GatewayCatalogMpTest} 固定 Step 5 应用/分组/凭据目录侧的 MyBatis-Plus 合同：
 * 业务唯一键谓词（{@code biz_code + application_code + env}、{@code env + namespace}、
 * {@code application_id + (id OR access_key)}）必须原样落在受守卫的 MP mapper 上，活跃过滤（{@code deleted_at IS NULL}）
 * 与租户上下文必须在持久边界成立，替换写入必须把原行业务 {@code revision} 递增并回写载体，0 行写入与第二租户上下文
 * 一律不得被报告为成功。
 * English summary: {@code GatewayCatalogMpTest} pins the Step 5 application/group/credential MyBatis-Plus contract: the
 * business unique-key predicates ({@code biz_code + application_code + env}, {@code env + namespace},
 * {@code application_id + (id OR access_key)}) must reach the guarded MyBatis-Plus mapper unchanged, active filtering
 * ({@code deleted_at IS NULL}) and the tenant context must hold at the persistence boundary, a replace must increment the
 * original row's business {@code revision} and sync it back into the carrier, and neither a zero-row effect nor a second
 * tenant context may be reported as success.
 *
 * 用法 / Usage: 与模块既有非数据库合同测试（starter 的 {@code EgonColaRepositoryTest} 与
 * {@code architecture/AiMpRepositoryContractTest}）同构：mock 各表的 DAO、装配真实 {@code EgonColaRepository} 与真实
 * MapStruct 转换器、以 MDC 提供可信租户 9001 并用固定 Clock 驱动时间；不启动 Docker、数据库或 Spring 上下文
 * （真实 PostgreSQL 验收属于后续 Step）。/ Runs like the module's existing non-database contract tests (the starter's
 * {@code EgonColaRepositoryTest} and {@code architecture/AiMpRepositoryContractTest}): the table DAOs are mocked, the real
 * {@code EgonColaRepository} instances and the real MapStruct converters are assembled by hand, MDC supplies the trusted
 * tenant 9001 and a fixed Clock drives time; no Docker, database or Spring context is started (real PostgreSQL execution
 * belongs to a later Step).
 */
class GatewayCatalogMpTest {

    /** 中文说明：计划的受信租户；所有正向读取与写入都在该租户上下文内发生。 English summary: the plan's trusted tenant; every positive read and write happens inside this tenant context. */
    private static final long TRUSTED_TENANT = 9001L;

    /** 中文说明：第二租户上下文，用于负向用例。 English summary: the second tenant context used by the negative case. */
    private static final long FOREIGN_TENANT = 9002L;

    /** 中文说明：受控时钟，替代旧的 {@code Clock.systemUTC()} 以保证时间断言可重复。 English summary: the controlled clock replacing the former {@code Clock.systemUTC()} so time assertions repeat. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-22T16:30:00Z"), ZoneOffset.UTC);

    /** 中文说明：受控时钟的当前时刻。 English summary: the controlled clock's current instant. */
    private static final Instant NOW = CLOCK.instant();

    private static final ValidatorFactory VALIDATORS =
            Validation.buildDefaultValidatorFactory();

    private GatewayApplicationDAO applicationDAO;
    private GatewayGroupDAO groupDAO;
    private GatewayCredentialDAO credentialDAO;

    private MpGatewayApplicationRepository applications;
    private MpGatewayGroupRepository groups;
    private MpGatewayCredentialRepository credentials;

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
                    "GatewayCatalogMpTest"
            );
        }
    }

    /**
     * 中文说明：注册三张表的 MP 行模型（列缓存）、装配真实持久化仓储与门面，并放入受信租户上下文。
     * English summary: Registers the three tables' MyBatis-Plus row models (the column cache), assembles the real
     * persistence repositories and facades, and installs the trusted tenant context.
     */
    @BeforeEach
    void assembleGuardedFacades() {
        registerRowModel(GatewayApplicationRecordPO.class);
        registerRowModel(GatewayGroupRecordPO.class);
        registerRowModel(GatewayCredentialRecordPO.class);
        applicationDAO = mock(GatewayApplicationDAO.class);
        groupDAO = mock(GatewayGroupDAO.class);
        credentialDAO = mock(GatewayCredentialDAO.class);
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        applications = new MpGatewayApplicationRepository(
                new GatewayApplicationPersistenceRepository(applicationDAO, properties),
                new GatewayApplicationPersistenceConverter()
        );
        groups = new MpGatewayGroupRepository(
                new GatewayGroupPersistenceRepository(groupDAO, properties),
                new GatewayGroupPersistenceConverter()
        );
        credentials = new MpGatewayCredentialRepository(
                new GatewayCredentialPersistenceRepository(credentialDAO, properties),
                new GatewayCredentialPersistenceConverter()
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
     * 中文说明：应用业务唯一键三元组必须作为三个独立谓词下推到受守卫 mapper，并且只经 {@code selectOne}，
     * 绝不回退到未守卫的 {@code selectById}。
     * English summary: The application business unique key must reach the guarded mapper as three separate predicates via
     * {@code selectOne} only, and never fall back to the unguarded {@code selectById}.
     */
    @Test
    @DisplayName("application business unique key is routed through the guarded mapper")
    @SuppressWarnings("unchecked")
    void applicationBusinessUniqueKeyIsRoutedThroughTheGuardedMapper() {
        when(applicationDAO.selectOne(any(), anyBoolean()))
                .thenReturn(persistedApplication(TRUSTED_TENANT, 3L, 7L));

        assertThat(applications.findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(
                "xingyuan", "yuheng-admin", "prod"))
                .get()
                .extracting(GatewayApplicationBO::getId)
                .isEqualTo("7001");

        ArgumentCaptor<LambdaQueryWrapper<GatewayApplicationRecordPO>> query =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(applicationDAO).selectOne(query.capture(), anyBoolean());
        String sql = query.getValue().getTargetSql();
        assertThat(sql)
                .contains("biz_code = ?")
                .contains("application_code = ?")
                .contains("env = ?");
        assertThat(sql.split("= \\?", -1).length - 1).isEqualTo(3);
        verify(applicationDAO, never()).selectById(any(Serializable.class));
    }

    /**
     * 中文说明：分组作用域查询保留 {@code env}/{@code namespace} 谓词并按迁移后的 {@code create_time} 倒序返回，
     * 活跃行过滤由 MP 逻辑删除谓词结构性保证。
     * English summary: The group scope query keeps the {@code env}/{@code namespace} predicates and returns rows ordered by
     * the migrated {@code create_time} descending, active-row filtering being structural through the MyBatis-Plus logical
     * delete predicate.
     */
    @Test
    @DisplayName("group scope query keeps env/namespace predicates and create_time ordering")
    @SuppressWarnings("unchecked")
    void groupScopeQueryKeepsEnvAndNamespacePredicatesAndActiveOrdering() {
        when(groupDAO.selectList(any())).thenReturn(List.of(persistedGroup(TRUSTED_TENANT, 2L, 1L)));

        assertThat(groups.findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(
                "prod", "infra-core"))
                .singleElement()
                .extracting(GatewayGroupBO::getId)
                .isEqualTo("6001");

        ArgumentCaptor<LambdaQueryWrapper<GatewayGroupRecordPO>> query =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(groupDAO).selectList(query.capture());
        String sql = query.getValue().getTargetSql();
        assertThat(sql)
                .contains("env = ?")
                .contains("namespace = ?")
                .contains("ORDER BY")
                .contains("create_time DESC")
                .contains("id DESC");
    }

    /**
     * 中文说明：三张迁移表的行模型都必须声明租户列与逻辑删除列，且活跃谓词恰为
     * {@code deleted_at IS NULL}；同时各自的守卫 XML 在按主键活跃读取里显式带同一谓词——这两点共同构成
     * “软删行不可见”的可观测证据。
     * English summary: All three migrated row models must declare the tenant and logical-delete columns with the active
     * predicate being exactly {@code deleted_at IS NULL}, and each guarded mapper XML repeats that predicate in the active
     * read by primary key; together these are the observable evidence that soft-deleted rows cannot be seen.
     */
    @Test
    @DisplayName("active filtering and the tenant column are structural for every migrated row model")
    void activeFilteringAndTenantScopeAreStructural() throws IOException {
        for (Class<?> rowModel : List.of(
                GatewayApplicationRecordPO.class,
                GatewayGroupRecordPO.class,
                GatewayCredentialRecordPO.class)) {
            TableInfo table = TableInfoHelper.getTableInfo(rowModel);
            assertThat(table).as(rowModel.getSimpleName() + " row model registration").isNotNull();
            assertThat(table.isWithLogicDelete())
                    .as(rowModel.getSimpleName() + " logical delete")
                    .isTrue();
            assertThat(table.getLogicDeleteSql(false, true))
                    .as(rowModel.getSimpleName() + " active predicate")
                    .isEqualTo("deleted_at IS NULL");
            assertThat(table.getFieldList())
                    .as(rowModel.getSimpleName() + " tenant column")
                    .anyMatch(field -> "tenant_id".equals(field.getColumn()));
        }
        for (String xml : List.of(
                "mybatis/mapper/application/GatewayApplicationDAO.xml",
                "mybatis/mapper/group/GatewayGroupDAO.xml",
                "mybatis/mapper/credential/GatewayCredentialDAO.xml")) {
            assertThat(read(xml))
                    .as(xml + " active read by primary key")
                    .contains("selectActiveById")
                    .contains("deleted_at IS NULL");
        }
    }

    /**
     * 中文说明：第二租户上下文负向用例——mapper 返回一行归属另一租户的记录时，门面必须把
     * {@code TENANT_CONTEXT_MISMATCH} 抛出而不是泄漏该载体；租户上下文缺失时更是必须在触达 SQL 前失败。
     * English summary: The second-context negative case — when the mapper hands back a row owned by another tenant the
     * facade must raise {@code TENANT_CONTEXT_MISMATCH} instead of leaking that carrier, and with no tenant context at all
     * it must fail before any SQL is reached.
     */
    @Test
    @DisplayName("a second tenant context cannot escape the persistence boundary")
    void secondTenantContextCannotEscapeTheBoundary() {
        when(applicationDAO.selectActiveById(7001L))
                .thenReturn(persistedApplication(FOREIGN_TENANT, 3L, 7L));
        assertThatThrownBy(() -> applications.findByIdAndDeletedFalse("7001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISMATCH");
        verify(applicationDAO, never()).selectById(any(Serializable.class));

        clearInvocations(applicationDAO);
        MDC.remove("tenantId");
        assertThatThrownBy(() -> applications.findAllByDeletedFalseOrderByCreatedAtDesc())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISSING");
        verify(applicationDAO, never()).selectList(any());
    }

    /**
     * 中文说明：draft/记录替换必须按“原行业务 revision + 1”写回，并沿用原行的技术 id/version 作为乐观锁 CAS 依据，
     * 同时把权威 revision 回写入参载体（替代旧托管实体脏检查）。
     * English summary: A draft/record replace must write the original row's business revision plus one, reuse the original
     * row's technical id and version as the optimistic-lock CAS token, and sync the authoritative revision back into the
     * incoming carrier instead of relying on managed-entity dirty checking.
     */
    @Test
    @DisplayName("replace increments the original business revision and syncs the carrier")
    void replaceIncrementsTheOriginalBusinessRevision() {
        when(applicationDAO.selectActiveById(7001L))
                .thenReturn(persistedApplication(TRUSTED_TENANT, 3L, 7L));
        when(applicationDAO.updateById(any(GatewayApplicationRecordPO.class))).thenReturn(1);
        GatewayApplicationBO carrier = applicationCarrier(3L);

        GatewayApplicationBO saved = applications.save(carrier);

        ArgumentCaptor<GatewayApplicationRecordPO> written =
                ArgumentCaptor.forClass(GatewayApplicationRecordPO.class);
        verify(applicationDAO).updateById(written.capture());
        assertThat(written.getValue().getRevision()).isEqualTo(4L);
        assertThat(written.getValue().getVersion()).isEqualTo(7L);
        assertThat(written.getValue().getId()).isEqualTo(7001L);
        assertThat(written.getValue().getDisplayName()).isEqualTo("renamed");
        assertThat(saved.getRevision()).isEqualTo(4L);
        assertThat(carrier.getRevision())
                .as("the authoritative revision is synced back into the caller's carrier")
                .isEqualTo(4L);
        verify(applicationDAO, never()).insert(any(GatewayApplicationRecordPO.class));
    }

    /**
     * 中文说明：分组新建必须写入业务 revision 0；业务 revision 不匹配或影响 0 行都不得被报告为成功，
     * 且被拒绝的请求不得触碰任何写语句。
     * English summary: A group insert must store business revision 0; a business revision mismatch and a zero-row effect may
     * not be reported as success, and a rejected request must not touch any write statement.
     */
    @Test
    @DisplayName("zero-row and stale-revision writes are not reported as success")
    void zeroRowAndStaleRevisionWritesAreNotReportedAsSuccess() {
        when(groupDAO.insert(any(GatewayGroupRecordPO.class))).thenReturn(1);
        groups.save(groupCarrier(0L));
        ArgumentCaptor<GatewayGroupRecordPO> inserted =
                ArgumentCaptor.forClass(GatewayGroupRecordPO.class);
        verify(groupDAO).insert(inserted.capture());
        assertThat(inserted.getValue().getRevision())
                .as("a fresh group starts at business revision 0")
                .isEqualTo(0L);

        clearInvocations(groupDAO);
        when(groupDAO.selectActiveById(6001L))
                .thenReturn(persistedGroup(TRUSTED_TENANT, 5L, 2L));
        assertThatThrownBy(() -> groups.save(groupCarrier(4L)))
                .isInstanceOf(GatewayAdminRevisionConflictException.class)
                .hasMessage("YUHENG_ADMIN_REVISION_CONFLICT");
        verify(groupDAO, never()).updateById(any(GatewayGroupRecordPO.class));
        verify(groupDAO, never()).insert(any(GatewayGroupRecordPO.class));

        clearInvocations(groupDAO);
        when(groupDAO.updateById(any(GatewayGroupRecordPO.class))).thenReturn(0);
        assertThatThrownBy(() -> groups.save(groupCarrier(5L)))
                .as("a zero-row compare-and-set must not be reported as success")
                .isInstanceOf(GatewayAdminRevisionConflictException.class);
        verify(groupDAO).updateById(any(GatewayGroupRecordPO.class));
    }

    /**
     * 中文说明：{@code overlap} 保留旧 SQL 的状态集合守卫，并把固定 Clock 的 {@code validUntil} 落到业务列；
     * {@code revoke} 与旧实现一样不带状态谓词。读取为空时按状态冲突抛出，不伪造成功。
     * English summary: {@code overlap} keeps the legacy status-set guard and stores the fixed clock's {@code validUntil} in
     * the business column, while {@code revoke} stays free of a status predicate like the legacy statement. An empty read
     * raises a state conflict instead of a fake success.
     */
    @Test
    @DisplayName("credential overlap keeps the status guard and revoke keeps it free")
    @SuppressWarnings("unchecked")
    void credentialOverlapKeepsTheStatusGuardAndRevokeDoesNot() {
        GatewayCredentialRecordPO active = persistedCredential();
        when(credentialDAO.selectOne(any(), anyBoolean())).thenReturn(active);
        when(credentialDAO.updateById(any(GatewayCredentialRecordPO.class))).thenReturn(1);
        Instant rotationEnd = NOW.plus(Duration.ofDays(7));

        credentials.overlap("8001", rotationEnd, NOW);

        ArgumentCaptor<LambdaQueryWrapper<GatewayCredentialRecordPO>> overlapQuery =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(credentialDAO).selectOne(overlapQuery.capture(), anyBoolean());
        assertThat(overlapQuery.getValue().getTargetSql())
                .contains("id = ?")
                .contains("status IN");
        ArgumentCaptor<GatewayCredentialRecordPO> rotated =
                ArgumentCaptor.forClass(GatewayCredentialRecordPO.class);
        verify(credentialDAO).updateById(rotated.capture());
        assertThat(rotated.getValue().getStatus()).isEqualTo("ROTATING");
        assertThat(rotated.getValue().getValidUntil())
                .as("the controlled clock's value lands in the business valid_until column")
                .isEqualTo(rotationEnd);

        clearInvocations(credentialDAO);
        when(credentialDAO.selectOne(any(), anyBoolean())).thenReturn(active);
        when(credentialDAO.updateById(any(GatewayCredentialRecordPO.class))).thenReturn(1);
        credentials.revoke("8001", NOW);
        ArgumentCaptor<LambdaQueryWrapper<GatewayCredentialRecordPO>> revokeQuery =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(credentialDAO).selectOne(revokeQuery.capture(), anyBoolean());
        assertThat(revokeQuery.getValue().getTargetSql())
                .as("the legacy revoke carried no status predicate")
                .doesNotContain("status");
        ArgumentCaptor<GatewayCredentialRecordPO> revoked =
                ArgumentCaptor.forClass(GatewayCredentialRecordPO.class);
        verify(credentialDAO).updateById(revoked.capture());
        assertThat(revoked.getValue().getStatus()).isEqualTo("REVOKED");
        assertThat(revoked.getValue().getValidUntil()).isEqualTo(NOW);

        clearInvocations(credentialDAO);
        when(credentialDAO.selectOne(any(), anyBoolean())).thenReturn(null);
        assertThatThrownBy(() -> credentials.overlap("8001", rotationEnd, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YUHENG_ADMIN_CREDENTIAL_OVERLAP_CONFLICT");
        verify(credentialDAO, never()).updateById(any(GatewayCredentialRecordPO.class));
    }

    /**
     * 中文说明：本 Step 计划（File 9/File 10 pseudocode）要求“受守卫插入设置技术元数据，随后在新的只读事务里
     * 重新加载并按业务意图确认重复键”。当前门面只让 {@code DuplicateKeyException} 原样抛出，因此这条断言按
     * 缺失行为为 RED（不是夹具故障：mapper 与后续只读加载都已就位）。
     * English summary: This Step's plan (the File 9/File 10 pseudocode) requires the guarded insert to set the technical
     * metadata and a duplicate key to be resolved by reloading in a NEW read transaction that verifies the exact business
     * intent. The facade currently lets {@code DuplicateKeyException} escape, so this assertion is RED for missing
     * behaviour rather than a broken fixture: both the mapper stub and the follow-up read stub are already in place.
     */
    @Test
    @DisplayName("duplicate-key insert reloads the existing business intent (expected RED)")
    void duplicateKeyInsertReloadsTheExistingBusinessIntent() {
        when(applicationDAO.selectActiveById(7001L)).thenReturn(null);
        when(applicationDAO.insert(any(GatewayApplicationRecordPO.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException(
                        "duplicate key value violates unique constraint \"uk_gateway_application_scope\""
                ));
        when(applicationDAO.selectOne(any(), anyBoolean()))
                .thenReturn(persistedApplication(TRUSTED_TENANT, 0L, 0L));

        assertThatCode(() -> assertThat(applications.save(applicationCarrier(0L)))
                .as("the plan requires the conflicting active row to be reloaded and verified")
                .extracting(GatewayApplicationBO::getId)
                .isEqualTo("7001"))
                .doesNotThrowAnyException();
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
     * 中文说明：读取受守卫 mapper XML 资源文本。
     * English summary: Reads a guarded mapper XML resource as text.
     * @param resource 参数 classpath 资源路径；parameter the classpath resource path.
     * @return 返回 资源全文；returns the resource text.
     * @throws IOException 参数 读取失败；parameter when the resource cannot be read.
     */
    private static String read(String resource) throws IOException {
        try (InputStream in = GatewayCatalogMpTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 中文说明：构造调用方提交的应用载体（业务 revision 由入参给定，模拟服务层已解析过当前视图）。
     * English summary: Builds the application carrier the caller submits, with the business revision supplied by the
     * argument to imitate the service layer having resolved the current view.
     * @param revision 参数 业务 revision；parameter the business revision.
     * @return 返回 应用载体；returns the application carrier.
     */
    private static GatewayApplicationBO applicationCarrier(long revision) {
        return GatewayApplicationBO.builder()
                .id("7001")
                .bizCode("xingyuan")
                .applicationCode("yuheng-admin")
                .displayName("renamed")
                .env("prod")
                .namespace("infra-core")
                .description("step 5 contract carrier")
                .revision(revision)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    /**
     * 中文说明：构造调用方提交的分组载体。
     * English summary: Builds the group carrier the caller submits.
     * @param revision 参数 业务 revision；parameter the business revision.
     * @return 返回 分组载体；returns the group carrier.
     */
    private static GatewayGroupBO groupCarrier(long revision) {
        return GatewayGroupBO.builder()
                .id("6001")
                .gatewayGroupCode("yuheng-core")
                .displayName("renamed")
                .env("prod")
                .namespace("infra-core")
                .description("step 5 contract carrier")
                .enabled(true)
                .revision(revision)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    /**
     * 中文说明：构造一条完整的技术列齐备的应用活跃行，作为受守卫读取的 mapper 返回值。
     * English summary: Builds a fully-populated active application row, the value the guarded read gets back from the mapper.
     * @param tenantId 参数 归属租户；parameter the owning tenant.
     * @param revision 参数 业务 revision；parameter the business revision.
     * @param version 参数 技术 version；parameter the technical version.
     * @return 返回 应用活跃行；returns the active application row.
     */
    private static GatewayApplicationRecordPO persistedApplication(
            long tenantId,
            long revision,
            long version) {
        return GatewayApplicationRecordPO.builder()
                .id(7001L)
                .tenantId(tenantId)
                .applicationCode("yuheng-admin")
                .bizCode("xingyuan")
                .displayName("stored")
                .env("prod")
                .namespace("infra-core")
                .description("stored row")
                .revision(revision)
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(NOW.minus(Duration.ofDays(1)))
                .updateTime(NOW.minus(Duration.ofDays(1)))
                .version(version)
                .build();
    }

    /**
     * 中文说明：构造一条完整的技术列齐备的分组活跃行。
     * English summary: Builds a fully-populated active group row.
     * @param tenantId 参数 归属租户；parameter the owning tenant.
     * @param revision 参数 业务 revision；parameter the business revision.
     * @param version 参数 技术 version；parameter the technical version.
     * @return 返回 分组活跃行；returns the active group row.
     */
    private static GatewayGroupRecordPO persistedGroup(long tenantId, long revision, long version) {
        return GatewayGroupRecordPO.builder()
                .id(6001L)
                .tenantId(tenantId)
                .gatewayGroupCode("yuheng-core")
                .displayName("stored")
                .env("prod")
                .namespace("infra-core")
                .description("stored row")
                .enabled(true)
                .revision(revision)
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(NOW.minus(Duration.ofDays(1)))
                .updateTime(NOW.minus(Duration.ofDays(1)))
                .version(version)
                .build();
    }

    /**
     * 中文说明：构造一条活跃的网关凭证行。
     * English summary: Builds an active gateway credential row.
     * @return 返回 凭证活跃行；returns the active credential row.
     */
    private static GatewayCredentialRecordPO persistedCredential() {
        return GatewayCredentialRecordPO.builder()
                .id(8001L)
                .tenantId(TRUSTED_TENANT)
                .applicationId(7001L)
                .accessKey("ak-8001")
                .secretCiphertext("ciphertext")
                .keyVersion("v1")
                .status("ACTIVE")
                .validFrom(NOW.minus(Duration.ofDays(30)))
                .validUntil(NOW.plus(Duration.ofDays(60)))
                .createUserId("actor-1")
                .updateUserId("actor-1")
                .createTime(NOW.minus(Duration.ofDays(30)))
                .updateTime(NOW.minus(Duration.ofDays(30)))
                .version(0L)
                .build();
    }
}
