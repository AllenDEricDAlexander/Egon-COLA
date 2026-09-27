package top.egon.cola.component.outbox.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.outbox.persistence.OutboxTechnicalContextExecutor;
import top.egon.cola.component.outbox.persistence.converter.OutboxHeadersConverter;
import top.egon.cola.component.outbox.persistence.converter.OutboxMessageConverter;
import top.egon.cola.component.outbox.persistence.converter.OutboxMessageConverterImpl;
import top.egon.cola.component.outbox.store.NewOutboxRecord;
import top.egon.cola.component.outbox.store.OutboxRecord;
import top.egon.cola.component.outbox.store.OutboxStatus;
import top.egon.cola.component.outbox.common.exception.OutboxValidationException;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;
import top.egon.cola.component.outbox.persistence.po.OutboxMessagePO;

import java.time.Instant;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxMessageConverterTest {
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void projectsPersistedMessageWithoutTechnicalEgonModelFields() {
        OutboxMessagePO source = persistedMessage("{\"x-trace\":\"edge\",\"locale\":\"en\"}");

        OutboxRecord target = converter().toTarget(source);

        assertThat(target.id()).isEqualTo(101L);
        assertThat(target.messageId()).isEqualTo("message-101");
        assertThat(target.idempotencyKey()).isEqualTo("key-101");
        assertThat(target.messageFingerprint()).isEqualTo("a".repeat(64));
        assertThat(target.channel()).isEqualTo("state-machine");
        assertThat(target.destination()).isEqualTo("orders:42");
        assertThat(target.payload()).isEqualTo("{\"orderId\":42}");
        assertThat(target.contentType()).isEqualTo("application/json");
        assertThat(target.schemaVersion()).isEqualTo("1");
        assertThat(target.headers()).containsExactlyInAnyOrderEntriesOf(Map.of("x-trace", "edge", "locale", "en"));
        assertThatThrownBy(() -> target.headers().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(target.traceId()).isEqualTo("trace-101");
        assertThat(target.status()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(target.attemptCount()).isEqualTo(2);
        assertThat(target.maxAttempts()).isEqualTo(5);
        assertThat(target.nextAttemptAt()).isEqualTo(Instant.parse("2026-09-24T14:01:02.123456Z"));
        assertThat(target.lockedBy()).isEqualTo("worker-1");
        assertThat(target.lockedUntil()).isEqualTo(Instant.parse("2026-09-24T14:02:02.123456Z"));
        assertThat(target.lastErrorCode()).isEqualTo("OUTBOX_SM_TIMEOUT");
        assertThat(target.lastErrorMessage()).isEqualTo("delivery timed out");
        assertThat(target.createdAt()).isEqualTo(Instant.parse("2026-09-24T14:00:02.123456Z"));
        assertThat(target.updatedAt()).isEqualTo(Instant.parse("2026-09-24T14:00:22.123456Z"));
        assertThat(target.completedAt()).isNull();
        assertThat(converter().toTarget(null)).isNull();
    }

    @Test
    void mapsNewAndMigratedMessagesWithoutInventingAuditOrRewritingRawFields() throws Exception {
        OutboxMessageConverter mapper = converter();
        String rawHeadersJson = " { \"x-raw\" : \"kept\" } ";
        Instant availableAt = Instant.parse("2026-09-24T15:00:00.654321Z");
        NewOutboxRecord newRecord = new NewOutboxRecord("new-message", "new-key", "b".repeat(64),
                "rabbitmq", "orders:42", "{\"orderId\":42}", "application/json", "1",
                rawHeadersJson, "trace-new", availableAt, 7);

        OutboxMessagePO inserted = mapper.toInsertPO(newRecord);

        assertThat(inserted.getMessageId()).isEqualTo("new-message");
        assertThat(inserted.getIdempotencyKey()).isEqualTo("new-key");
        assertThat(inserted.getMessageFingerprint()).isEqualTo("b".repeat(64));
        assertThat(inserted.getPayload()).isEqualTo(newRecord.payload());
        assertThat(inserted.getHeadersJson()).isEqualTo(rawHeadersJson);
        assertThat(inserted.getNextAttemptAt()).isEqualTo(availableAt);
        assertThat(inserted.getStatus()).isNull();
        assertThat(inserted.getAttemptCount()).isZero();
        assertThat(inserted.getId()).isNull();
        assertThat(inserted.getTenantId()).isNull();
        assertThat(inserted.getCreateUserId()).isNull();
        assertThat(inserted.getCreateTime()).isNull();
        assertThat(inserted.getUpdateUserId()).isNull();
        assertThat(inserted.getUpdateTime()).isNull();
        assertThat(inserted.getDeletedAt()).isNull();
        assertThat(inserted.getVersion()).isNull();

        OutboxRecord legacyRecord = new OutboxRecord(202L, "legacy-message", "legacy-key", "c".repeat(64),
                "http", "legacy:destination", "{\"raw\":true}", "application/json", "1",
                Map.of("parsed", "headers"), "legacy-trace", OutboxStatus.DEAD, 6, 6,
                Instant.parse("2026-09-24T16:00:00.123456Z"), "old-worker", null, "OUTBOX_TIMEOUT",
                "legacy failure", Instant.parse("2026-09-24T15:59:00.123456Z"),
                Instant.parse("2026-09-24T15:59:30.123456Z"), Instant.parse("2026-09-24T15:59:45.123456Z"));

        OutboxMessagePO migrated = mapper.toMigrationPO(legacyRecord, legacyRecord.payload(), rawHeadersJson);

        assertThat(migrated.getId()).isEqualTo(202L);
        assertThat(migrated.getMessageId()).isEqualTo(legacyRecord.messageId());
        assertThat(migrated.getPayload()).isEqualTo(legacyRecord.payload());
        assertThat(migrated.getHeadersJson()).isEqualTo(rawHeadersJson);
        assertThat(migrated.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(migrated.getAttemptCount()).isEqualTo(6);
        assertThat(migrated.getCompletedAt()).isEqualTo(legacyRecord.completedAt());
        assertThat(migrated.getTenantId()).isNull();
        assertThat(migrated.getCreateUserId()).isNull();
        assertThat(migrated.getUpdateUserId()).isNull();
        assertThat(migrated.getVersion()).isZero();
        assertThat(migrated.getDeletedAt()).isNull();
    }

    @Test
    void rejectsMalformedOrNonStringHeadersInsteadOfSilentlyNormalizingThem() {
        OutboxMessageConverter mapper = converter();

        assertThatThrownBy(() -> mapper.toTarget(persistedMessage("{")))
                .isInstanceOf(OutboxValidationException.class);
        assertThatThrownBy(() -> mapper.toTarget(persistedMessage("{} true")))
                .isInstanceOf(OutboxValidationException.class);
        assertThatThrownBy(() -> mapper.toTarget(persistedMessage("[]")))
                .isInstanceOf(OutboxValidationException.class);
        assertThatThrownBy(() -> mapper.toTarget(persistedMessage("{\"authorization\":\"secret\"}")))
                .isInstanceOf(OutboxValidationException.class);
        assertThatThrownBy(() -> mapper.toTarget(persistedMessage("{\"count\":1}")))
                .isInstanceOf(OutboxValidationException.class);
        assertThatThrownBy(() -> mapper.toTarget(persistedMessage("{\"a\":\"1\",\"b\":\"2\",\"c\":\"3\"}")))
                .isInstanceOf(OutboxValidationException.class);
    }

    @Test
    void technicalContextRestoresBusinessMdcAfterNestedAndFailedOperations() {
        OutboxTechnicalContextExecutor executor = new OutboxTechnicalContextExecutor(
                new EgonColaMybatisPlusProperties());
        MDC.put("tenantId", "42");
        MDC.put("userId", "user-42");
        try {
            assertThat(executor.execute(() -> {
                assertThat(MDC.get("tenantId")).isEqualTo("0");
                assertThat(MDC.get("userId")).isEqualTo("system:outbox");
                return executor.execute(() -> {
                    assertThat(MDC.get("tenantId")).isEqualTo("0");
                    assertThat(MDC.get("userId")).isEqualTo("system:outbox");
                    return "nested";
                });
            })).isEqualTo("nested");
            assertThat(MDC.get("tenantId")).isEqualTo("42");
            assertThat(MDC.get("userId")).isEqualTo("user-42");

            assertThatThrownBy(() -> executor.execute(() -> {
                assertThat(MDC.get("tenantId")).isEqualTo("0");
                assertThat(MDC.get("userId")).isEqualTo("system:outbox");
                throw new IllegalStateException("mapper failed");
            })).hasMessage("mapper failed");
            assertThat(MDC.get("tenantId")).isEqualTo("42");
            assertThat(MDC.get("userId")).isEqualTo("user-42");
        } finally {
            MDC.remove("tenantId");
            MDC.remove("userId");
        }
    }

    @Test
    void mapperXmlBuildsItsExplicitStatementsWithoutSoftDeleteForTheTechnicalTable() throws IOException {
        Configuration configuration = new Configuration();
        configuration.addMapper(top.egon.cola.component.outbox.persistence.dao.OutboxMessageDAO.class);
        String resourcePath = "mapper/outbox/OutboxMessageMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(input).isNotNull();
            new XMLMapperBuilder(input, configuration, resourcePath, configuration.getSqlFragments()).parse();
        }

        String namespace = "top.egon.cola.component.outbox.persistence.dao.OutboxMessageDAO.";
        assertThat(configuration.hasStatement(namespace + "insertMessage", false)).isTrue();
        assertThat(configuration.hasStatement(namespace + "insertMigrationMessage", false)).isTrue();
        assertThat(configuration.hasStatement(namespace + "selectDueCandidates", false)).isTrue();
        assertThat(configuration.hasStatement(namespace + "selectByMessageIdsCandidates", false)).isTrue();
        assertThat(configuration.hasStatement(namespace + "tryAcquireTransactionLock", false)).isTrue();
        assertThat(configuration.hasStatement(namespace + "updateClaim", false)).isTrue();
        assertThat(configuration.hasStatement(namespace + "selectExpiredSucceededCandidates", false)).isTrue();
        assertThat(configuration.hasStatement(namespace + "deleteSucceededByIdVersion", false)).isTrue();
        assertThat(configuration.hasStatement(namespace + "selectActiveById", false)).isTrue();
        assertThat(configuration.hasStatement(namespace + "selectActiveByIds", false)).isTrue();
        assertThat(configuration.hasStatement(namespace + "deleteVersionedById", false)).isFalse();
    }

    private static OutboxMessagePO persistedMessage(String headersJson) {
        return OutboxMessagePO.builder()
                .id(101L)
                .tenantId(0L)
                .createUserId("system:outbox")
                .createTime(Instant.parse("2026-09-24T14:00:02.123456Z"))
                .updateUserId("system:outbox")
                .updateTime(Instant.parse("2026-09-24T14:00:22.123456Z"))
                .deletedAt(null)
                .version(8L)
                .messageId("message-101")
                .idempotencyKey("key-101")
                .messageFingerprint("a".repeat(64))
                .channel("state-machine")
                .destination("orders:42")
                .payload("{\"orderId\":42}")
                .contentType("application/json")
                .schemaVersion("1")
                .headersJson(headersJson)
                .traceId("trace-101")
                .status(OutboxStatus.PROCESSING)
                .attemptCount(2)
                .maxAttempts(5)
                .nextAttemptAt(Instant.parse("2026-09-24T14:01:02.123456Z"))
                .lockedBy("worker-1")
                .lockedUntil(Instant.parse("2026-09-24T14:02:02.123456Z"))
                .lastErrorCode("OUTBOX_SM_TIMEOUT")
                .lastErrorMessage("delivery timed out")
                .createdAt(Instant.parse("2026-09-24T14:00:02.123456Z"))
                .updatedAt(Instant.parse("2026-09-24T14:00:22.123456Z"))
                .completedAt(null)
                .build();
    }

    private static OutboxMessageConverter converter() {
        ObjectMapper objectMapper = new ObjectMapper();
        ValidationUtils validationUtils = new ValidationUtils(VALIDATOR_FACTORY.getValidator());
        OutboxMessageValidator validator = new OutboxMessageValidator(objectMapper, 1024 * 1024, 2,
                256, validationUtils);
        return new OutboxMessageConverterImpl(new OutboxHeadersConverter(objectMapper, validator));
    }
}
