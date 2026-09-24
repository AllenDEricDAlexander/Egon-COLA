package top.egon.cola.component.outbox.store;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.outbox.persistence.OutboxSchemaMetadataValidator;
import top.egon.cola.component.outbox.persistence.OutboxTechnicalContextExecutor;
import top.egon.cola.component.outbox.persistence.converter.OutboxMessageConverter;
import top.egon.cola.component.outbox.persistence.po.OutboxMessagePO;
import top.egon.cola.component.outbox.persistence.repository.OutboxMessageRepository;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleService;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleSignalEnum;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifies that the MP insert model keeps the existing positive Snowflake primary-key contract. */
class OutboxLongPrimaryKeyTest {

    private static final int MACHINE_ID = 733;
    private static final Duration CLOCK_BACKWARD = Duration.ofMillis(5);

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize((long) MACHINE_ID, CLOCK_BACKWARD);
    }

    @Test
    void mpInsertReceivesPositiveMonotonicLongIdsWithTheConfiguredMachineSegment() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        OutboxMessageConverter converter = mock(OutboxMessageConverter.class);
        when(converter.toInsertPO(any(NewOutboxRecord.class))).thenAnswer(invocation -> insertModel(
                invocation.getArgument(0)));
        when(repository.insertMessage(any(OutboxMessagePO.class))).thenReturn(1);

        MybatisPlusOutboxStore store = store(repository, converter);
        store.enqueue(record("message-1", "key-1", "a".repeat(64)));
        store.enqueue(record("message-2", "key-2", "b".repeat(64)));

        org.mockito.ArgumentCaptor<OutboxMessagePO> inserted =
                org.mockito.ArgumentCaptor.forClass(OutboxMessagePO.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(2)).insertMessage(inserted.capture());
        List<Long> ids = inserted.getAllValues().stream().map(OutboxMessagePO::getId).toList();

        assertThat(ids).allSatisfy(id -> {
            assertThat(id).isPositive();
            assertThat((id >>> 12) & 1023L).isEqualTo(MACHINE_ID);
        });
        assertThat(ids.get(1)).isGreaterThan(ids.get(0));
        assertThat(inserted.getAllValues()).extracting(OutboxMessagePO::getStatus)
                .containsExactly(OutboxStatus.PENDING, OutboxStatus.PENDING);
    }

    @Test
    void duplicateReceiptDoesNotReplaceTheOriginalPrimaryKey() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        OutboxMessageConverter converter = mock(OutboxMessageConverter.class);
        when(converter.toInsertPO(any(NewOutboxRecord.class))).thenAnswer(invocation -> insertModel(
                invocation.getArgument(0)));
        when(repository.insertMessage(any(OutboxMessagePO.class))).thenReturn(0);
        OutboxMessagePO existing = OutboxMessagePO.builder()
                .messageId("message-original")
                .idempotencyKey("key-original")
                .messageFingerprint("c".repeat(64))
                .build();
        when(repository.selectExistingByIdentity("message-retry", "key-original"))
                .thenReturn(List.of(existing));

        OutboxReceipt receipt = store(repository, converter)
                .enqueue(record("message-retry", "key-original", "c".repeat(64)));

        assertThat(receipt).isEqualTo(new OutboxReceipt("message-original", "key-original", false));
    }

    @Test
    void anAmbiguousExistingIdentityIsNeverReportedAsIdempotentSuccess() {
        OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
        OutboxMessageConverter converter = mock(OutboxMessageConverter.class);
        when(converter.toInsertPO(any(NewOutboxRecord.class))).thenAnswer(invocation -> insertModel(
                invocation.getArgument(0)));
        when(repository.insertMessage(any(OutboxMessagePO.class))).thenReturn(0);
        when(repository.selectExistingByIdentity("message-1", "key-1")).thenReturn(List.of(
                OutboxMessagePO.builder().messageId("message-1").messageFingerprint("a".repeat(64)).build(),
                OutboxMessagePO.builder().messageId("message-2").messageFingerprint("a".repeat(64)).build()));

        assertThatThrownBy(() -> store(repository, converter).enqueue(record("message-1", "key-1", "a".repeat(64))))
                .isInstanceOf(top.egon.cola.component.outbox.common.exception.OutboxIdempotencyConflictException.class);
    }

    @Test
    void theHistoricalIdentityDeclarationStaysUntouchedInV1() throws Exception {
        String sql = new ClassPathResource(
                "db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql.replaceAll("\\s+", " "))
                .contains("id bigint generated by default as identity primary key");
    }

    private static MybatisPlusOutboxStore store(
            OutboxMessageRepository repository,
            OutboxMessageConverter converter
    ) {
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        OutboxLifecycleService lifecycle = mock(OutboxLifecycleService.class);
        when(lifecycle.evaluate(anyString(), any(OutboxLifecycleSignalEnum.class), anyInt(), anyInt()))
                .thenReturn(OutboxStatus.PENDING);
        return new MybatisPlusOutboxStore(repository, converter,
                new OutboxTechnicalContextExecutor(properties), lifecycle,
                mock(org.springframework.transaction.support.TransactionTemplate.class),
                mock(OutboxSchemaMetadataValidator.class), Clock.systemUTC());
    }

    private static OutboxMessagePO insertModel(NewOutboxRecord record) {
        return OutboxMessagePO.builder()
                .messageId(record.messageId())
                .idempotencyKey(record.idempotencyKey())
                .messageFingerprint(record.messageFingerprint())
                .channel(record.channel())
                .destination(record.destination())
                .payload(record.payload())
                .contentType(record.contentType())
                .schemaVersion(record.schemaVersion())
                .headersJson(record.headersJson())
                .traceId(record.traceId())
                .nextAttemptAt(record.availableAt())
                .maxAttempts(record.maxAttempts())
                .build();
    }

    private static NewOutboxRecord record(String messageId, String idempotencyKey, String fingerprint) {
        return new NewOutboxRecord(messageId, idempotencyKey, fingerprint, "test", "orders", "{}",
                "application/json", "1", "{}", "trace-1", Instant.parse("2026-09-20T12:00:00Z"), 3);
    }
}
