package top.egon.cola.archetype.source.agent.infrastructure.knowledge;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.ChunkingStrategyEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.DocumentIngestStatusEnum;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeBaseBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeChunkConfigBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.model.KnowledgeDocumentBO;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeBaseRepository;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.dao.KnowledgeBaseDAO;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.dao.KnowledgeDocumentDAO;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.service.KnowledgeBaseRepositoryImpl;
import top.egon.cola.archetype.source.agent.infrastructure.knowledge.service.KnowledgeDocumentRepositoryImpl;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real MyBatis/H2 proof for the knowledge persistence adapters.
 *
 * <p>The tenant flows from the MDC into the shared MyBatis-Plus interceptor chain, so the captured
 * statements are the ones the driver receives, not a hand written SQL that happens to look scoped.
 */
class KnowledgeRepositoryTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    private static final List<String> EXECUTED_SQL = Collections.synchronizedList(new ArrayList<>());

    private static final long TENANT_A = 11L;

    private static final long TENANT_B = 22L;

    private static final long BASE_A = 101L;

    private static final long BASE_B = 202L;

    private static final String CONTENT_HASH =
            "a4e0f5f2c1b9d7e3a8c6b4d2e0f1a3c5b7d9e1f3a5c7b9d1e3f5a7c9b1d3e5f7";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EgonColaMybatisPlusAutoConfiguration.class,
                    MybatisPlusInnerInterceptorAutoConfiguration.class,
                    MybatisPlusAutoConfiguration.class))
            .withUserConfiguration(KnowledgePersistenceConfiguration.class)
            .withBean(Validator.class, VALIDATOR_FACTORY::getValidator);

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @BeforeEach
    void clearCapturedSql() {
        EXECUTED_SQL.clear();
    }

    @Test
    void scopes_every_query_by_tenant() {
        contextRunner.run(context -> {
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            insertBase(jdbc, BASE_A, TENANT_A, "product-docs", "2026-01-01T00:00:00Z");
            insertBase(jdbc, BASE_B, TENANT_B, "other-docs", "2026-01-02T00:00:00Z");
            KnowledgeBaseRepository repository = context.getBean(KnowledgeBaseRepository.class);

            useTenant(TENANT_A);
            EXECUTED_SQL.clear();
            assertThat(repository.page(0, 20, null, null))
                    .extracting(KnowledgeBaseBO::code)
                    .containsExactly("product-docs");
            assertThat(repository.count(null, null)).isEqualTo(1L);
            assertThat(repository.findById(BASE_B)).isEmpty();
            assertThat(repository.findById(BASE_A)).isPresent();

            assertThat(EXECUTED_SQL).isNotEmpty()
                    .allSatisfy(sql -> assertThat(sql).contains("tenant_id = " + TENANT_A));
        });
    }

    @Test
    void filters_soft_deleted_rows() {
        contextRunner.run(context -> {
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            insertBase(jdbc, BASE_A, TENANT_A, "kept", "2026-01-01T00:00:00Z");
            insertBase(jdbc, BASE_B, TENANT_A, "dropped", "2026-01-02T00:00:00Z");
            jdbc.update("update knowledge_base set is_deleted = true where id = ?", BASE_B);
            insertDocument(jdbc, 501L, TENANT_A, BASE_A, "kept.txt",
                    DocumentIngestStatusEnum.PENDING, false);
            insertDocument(jdbc, 502L, TENANT_A, BASE_A, "dropped.txt",
                    DocumentIngestStatusEnum.PENDING, false);
            KnowledgeBaseRepository baseRepository = context.getBean(KnowledgeBaseRepository.class);
            KnowledgeDocumentRepository documentRepository =
                    context.getBean(KnowledgeDocumentRepository.class);

            useTenant(TENANT_A);
            EXECUTED_SQL.clear();
            assertThat(baseRepository.page(0, 20, null, null))
                    .extracting(KnowledgeBaseBO::code)
                    .containsExactly("kept");
            assertThat(baseRepository.findById(BASE_B)).isEmpty();
            assertThat(documentRepository.countByKnowledgeBaseId(BASE_A)).isEqualTo(2L);

            baseRepository.softDelete(BASE_A);
            documentRepository.softDelete(501L);
            documentRepository.softDeleteByKnowledgeBaseId(BASE_A);

            assertThat(baseRepository.count(null, null)).isZero();
            assertThat(documentRepository.countByKnowledgeBaseId(BASE_A)).isZero();
            assertThat(documentRepository.page(BASE_A, 0, 20, null, null)).isEmpty();
            assertThat(deleted(jdbc, "knowledge_base")).isTrue();
            assertThat(deleted(jdbc, "knowledge_document")).isTrue();
        });
    }

    @Test
    void orders_by_creation_descending() {
        contextRunner.run(context -> {
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            insertBase(jdbc, BASE_A, TENANT_A, "oldest", "2026-01-01T00:00:00Z");
            insertBase(jdbc, BASE_B, TENANT_A, "newest", "2026-01-03T00:00:00Z");
            insertBase(jdbc, 303L, TENANT_A, "middle", "2026-01-02T00:00:00Z");
            insertDocument(jdbc, 501L, TENANT_A, BASE_A, "oldest.txt",
                    DocumentIngestStatusEnum.PENDING, false);
            insertDocument(jdbc, 502L, TENANT_A, BASE_A, "newest.txt",
                    DocumentIngestStatusEnum.PENDING, false);
            jdbc.update("update knowledge_document set create_time = ? where id = ?",
                    Timestamp.from(Instant.parse("2026-01-03T00:00:00Z")), 502L);
            KnowledgeBaseRepository baseRepository = context.getBean(KnowledgeBaseRepository.class);
            KnowledgeDocumentRepository documentRepository =
                    context.getBean(KnowledgeDocumentRepository.class);

            useTenant(TENANT_A);
            EXECUTED_SQL.clear();
            assertThat(baseRepository.page(0, 2, null, null))
                    .extracting(KnowledgeBaseBO::code)
                    .containsExactly("newest", "middle");
            assertThat(baseRepository.page(2, 2, null, null))
                    .extracting(KnowledgeBaseBO::code)
                    .containsExactly("oldest");
            assertThat(documentRepository.page(BASE_A, 0, 20, null, null))
                    .extracting(KnowledgeDocumentBO::displayName)
                    .containsExactly("newest.txt", "oldest.txt");
            assertThat(documentRepository.page(BASE_A, 0, 20, null, "newest"))
                    .extracting(KnowledgeDocumentBO::displayName)
                    .containsExactly("newest.txt");
            assertThat(documentRepository.page(BASE_B, 0, 20, null, null)).isEmpty();

            // The statement as the driver receives it: the soft-delete filter, the injected tenant
            // predicate and the requested window, in the documented order.
            assertThat(EXECUTED_SQL).isNotEmpty();
            assertThat(EXECUTED_SQL.get(0))
                    .contains("is_deleted = 0")
                    .contains("tenant_id = " + TENANT_A)
                    .contains("ORDER BY create_time DESC, id DESC LIMIT 2 OFFSET 0");
            assertThat(EXECUTED_SQL)
                    .anySatisfy(sql -> assertThat(sql).contains("lower(display_name) LIKE ?"));
        });
    }

    @Test
    void applies_document_status_transitions_conditionally() {
        contextRunner.run(context -> {
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            insertDocument(jdbc, 501L, TENANT_A, BASE_A, "report.pdf",
                    DocumentIngestStatusEnum.PENDING, false);
            insertDocument(jdbc, 502L, TENANT_A, BASE_A, "other.pdf",
                    DocumentIngestStatusEnum.SUCCEEDED, false);
            KnowledgeDocumentRepository repository = context.getBean(KnowledgeDocumentRepository.class);

            useTenant(TENANT_A);
            EXECUTED_SQL.clear();
            assertThat(repository.markSucceeded(501L, 7)).isFalse();
            assertThat(repository.markProcessing(501L, 1)).isTrue();
            assertThat(repository.markProcessing(501L, 2)).isFalse();
            assertThat(repository.markSucceeded(501L, 7)).isTrue();
            assertThat(document(jdbc, 501L).status()).isEqualTo(DocumentIngestStatusEnum.SUCCEEDED);
            assertThat(document(jdbc, 501L).chunkCount()).isEqualTo(7);

            assertThat(repository.resetForReingest(501L)).isTrue();
            assertThat(repository.resetForReingest(501L)).isFalse();
            assertThat(document(jdbc, 501L).status()).isEqualTo(DocumentIngestStatusEnum.PENDING);
            assertThat(document(jdbc, 501L).chunkCount()).isZero();
            assertThat(document(jdbc, 501L).errorCode()).isNull();

            assertThat(repository.markProcessing(501L, 1)).isTrue();
            assertThat(repository.markRetryPending(501L, 2, "INGEST_FAILED", "抽取失败")).isTrue();
            assertThat(repository.markDead(501L, "INGEST_EXHAUSTED", "重试耗尽")).isFalse();
            assertThat(document(jdbc, 501L).errorCode()).isEqualTo("INGEST_FAILED");

            assertThat(repository.markProcessing(502L, 3)).isFalse();
            assertThat(repository.findByIds(List.of(501L, 502L))).hasSize(2);
            assertThat(repository.findByIds(List.of())).isEmpty();
        });
    }

    @Test
    void stores_and_reads_back_both_aggregates() {
        contextRunner.run(context -> {
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            KnowledgeBaseRepository baseRepository = context.getBean(KnowledgeBaseRepository.class);
            KnowledgeDocumentRepository documentRepository =
                    context.getBean(KnowledgeDocumentRepository.class);

            useTenant(TENANT_A);
            EXECUTED_SQL.clear();
            KnowledgeBaseBO stored = baseRepository.insert(KnowledgeBaseBO.create("product-docs",
                    "产品文档库", "内部手册", "openai-small", ChunkingStrategyEnum.MARKDOWN_HEADING,
                    new KnowledgeChunkConfigBO(512, 64, 1, List.of(3, 2)), TENANT_A));

            assertThat(stored.knowledgeBaseId()).isNotNull();
            assertThat(stored.createdAt()).isNotNull();
            assertThat(stored.updatedAt()).isNotNull();
            assertThat(stored.chunkConfig().headingLevels()).containsExactly(2, 3);
            assertThat(stored.description()).isEqualTo("内部手册");

            KnowledgeDocumentBO uploaded = documentRepository.insert(KnowledgeDocumentBO.create(
                    TENANT_A, stored.knowledgeBaseId(), "report.pdf", "report.pdf", "application/pdf",
                    182734L, CONTENT_HASH, "LOCAL", "0/" + stored.knowledgeBaseId() + "/report.pdf",
                    "抽取文本"));

            assertThat(uploaded.documentId()).isNotNull();
            assertThat(uploaded.status()).isEqualTo(DocumentIngestStatusEnum.PENDING);
            assertThat(uploaded.content()).isEqualTo("抽取文本");

            // The row, not just the returned carrier: the request tenant and the audit columns are
            // stamped by the persistence layer, and chunk_config is stored as json.
            assertThat(column(jdbc, "knowledge_base", "tenant_id", stored.knowledgeBaseId()))
                    .isEqualTo(String.valueOf(TENANT_A));
            assertThat(column(jdbc, "knowledge_base", "create_user_id", stored.knowledgeBaseId()))
                    .isEqualTo("alice");
            assertThat(flag(jdbc, "knowledge_base", stored.knowledgeBaseId())).isFalse();
            assertThat(column(jdbc, "knowledge_base", "chunk_config", stored.knowledgeBaseId()))
                    .contains("\"maxTokensPerChunk\":512")
                    .contains("\"headingLevels\":[2,3]");
            assertThat(column(jdbc, "knowledge_document", "tenant_id", uploaded.documentId()))
                    .isEqualTo(String.valueOf(TENANT_A));
            assertThat(column(jdbc, "knowledge_document", "content", uploaded.documentId()))
                    .isEqualTo("抽取文本");
        });
    }

    private static void useTenant(long tenantId) {
        MDC.put("tenantId", Long.toString(tenantId));
        MDC.put("userId", "alice");
    }

    private static String column(JdbcTemplate jdbc, String table, String column, long id) {
        return jdbc.queryForObject("select " + column + " from " + table + " where id = ?",
                String.class, id);
    }

    private static boolean flag(JdbcTemplate jdbc, String table, long id) {
        Boolean value = jdbc.queryForObject("select is_deleted from " + table + " where id = ?",
                Boolean.class, id);
        return Boolean.TRUE.equals(value);
    }

    private static void insertBase(JdbcTemplate jdbc, long id, long tenantId, String code,
                                   String createdAt) {
        Instant instant = Instant.parse(createdAt);
        jdbc.update("insert into knowledge_base (id, tenant_id, create_user_id, create_time, "
                        + "update_user_id, update_time, is_deleted, code, name, description, "
                        + "embedding_model, chunk_strategy, chunk_config, status) "
                        + "values (?, ?, ?, ?, ?, ?, false, ?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, "seed", Timestamp.from(instant), "seed", Timestamp.from(instant),
                code, code, null, "openai-small", "TOKEN", "{\"maxTokensPerChunk\":512,"
                        + "\"overlapTokens\":64,\"minChunkChars\":1,\"headingLevels\":[]}", "ACTIVE");
    }

    private static void insertDocument(JdbcTemplate jdbc, long id, long tenantId, long baseId,
                                       String displayName, DocumentIngestStatusEnum status,
                                       boolean deleted) {
        Instant instant = Instant.parse("2026-01-01T00:00:00Z");
        jdbc.update("insert into knowledge_document (id, tenant_id, create_user_id, create_time, "
                        + "update_user_id, update_time, is_deleted, knowledge_base_id, display_name, "
                        + "file_name, mime_type, size_bytes, content_hash, storage_type, storage_key, "
                        + "content, status, chunk_count, attempt_count, error_code, error_message) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, null, null)",
                id, tenantId, "seed", Timestamp.from(instant), "seed", Timestamp.from(instant),
                deleted, baseId, displayName, displayName, "text/plain", 12L, CONTENT_HASH, "LOCAL",
                "0/" + baseId + "/" + displayName, "抽取文本", status.name());
    }

    private static boolean deleted(JdbcTemplate jdbc, String table) {
        Boolean deleted = jdbc.queryForObject("select min(cast(is_deleted as int)) = 1 from " + table,
                Boolean.class);
        return Boolean.TRUE.equals(deleted);
    }

    private static DocumentRow document(JdbcTemplate jdbc, long id) {
        return jdbc.queryForObject("select status, chunk_count, error_code from knowledge_document "
                        + "where id = ?",
                (resultSet, rowNumber) -> new DocumentRow(
                        DocumentIngestStatusEnum.valueOf(resultSet.getString("status")),
                        resultSet.getInt("chunk_count"), resultSet.getString("error_code")),
                id);
    }

    private record DocumentRow(DocumentIngestStatusEnum status, int chunkCount, String errorCode) {
    }

    @Configuration(proxyBeanMethods = false)
    @MapperScan(basePackageClasses = KnowledgeBaseDAO.class)
    static class KnowledgePersistenceConfiguration {

        @Bean
        DataSource dataSource() {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:knowledge_" + System.nanoTime()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false");
            dataSource.setUser("sa");
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            for (String statement : H2_SCHEMA) {
                jdbc.execute(statement);
            }
            return recording(dataSource);
        }

        @Bean
        KnowledgeBaseRepository knowledgeBaseRepository(KnowledgeBaseDAO knowledgeBaseDAO) {
            return new KnowledgeBaseRepositoryImpl(knowledgeBaseDAO);
        }

        @Bean
        KnowledgeDocumentRepository knowledgeDocumentRepository(
                KnowledgeDocumentDAO knowledgeDocumentDAO) {
            return new KnowledgeDocumentRepositoryImpl(knowledgeDocumentDAO);
        }
    }

    /**
     * The H2 projection of {@code V20260910_001}: the business columns are the contract, while the
     * PostgreSQL-only parts (jsonb, partial and expression indexes) stay in Flyway.
     */
    private static final List<String> H2_SCHEMA = List.of(
            "create table knowledge_base ("
                    + "id bigint primary key, tenant_id bigint not null, create_user_id varchar(64), "
                    + "create_time timestamp with time zone not null, update_user_id varchar(64), "
                    + "update_time timestamp with time zone not null, "
                    + "is_deleted boolean not null default false, code varchar(64) not null, "
                    + "name varchar(128) not null, description varchar(512), "
                    + "embedding_model varchar(32) not null, chunk_strategy varchar(32) not null, "
                    + "chunk_config varchar(2048) not null, status varchar(16) not null)",
            "create table knowledge_document ("
                    + "id bigint primary key, tenant_id bigint not null, create_user_id varchar(64), "
                    + "create_time timestamp with time zone not null, update_user_id varchar(64), "
                    + "update_time timestamp with time zone not null, "
                    + "is_deleted boolean not null default false, "
                    + "knowledge_base_id bigint not null, display_name varchar(255) not null, "
                    + "file_name varchar(255), mime_type varchar(128), size_bytes bigint not null, "
                    + "content_hash char(64) not null, storage_type varchar(16) not null, "
                    + "storage_key varchar(512) not null, content clob, status varchar(16) not null, "
                    + "chunk_count integer not null, attempt_count integer not null, "
                    + "error_code varchar(64), error_message varchar(512))");

    private static DataSource recording(DataSource delegate) {
        return (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class}, (proxy, method, arguments) -> {
                    Object result = invoke(delegate, method, arguments);
                    return result instanceof Connection connection
                            ? recording(connection) : result;
                });
    }

    private static Connection recording(Connection delegate) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
                    if (arguments != null && arguments.length > 0
                            && arguments[0] instanceof String sql
                            && (method.getName().equals("prepareStatement")
                            || method.getName().equals("prepareCall"))) {
                        EXECUTED_SQL.add(sql);
                    }
                    return invoke(delegate, method, arguments);
                });
    }

    private static Object invoke(Object target, java.lang.reflect.Method method, Object[] arguments)
            throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException failure) {
            throw failure.getTargetException();
        }
    }
}
