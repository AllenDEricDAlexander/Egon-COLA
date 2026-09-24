package top.egon.cola.component.outbox.persistence.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.outbox.persistence.po.OutboxMessagePO;
import top.egon.cola.component.outbox.store.NewOutboxRecord;
import top.egon.cola.component.outbox.store.OutboxRecord;

/** Maps the storage model to the stable outbox record and to explicit insert projections. */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR, uses = OutboxHeadersConverter.class)
public interface OutboxMessageConverter extends BaseForwardConverter<OutboxMessagePO, OutboxRecord> {

    @Override
    @BeanMapping(ignoreUnmappedSourceProperties = {
            "tenantId", "createUserId", "createTime", "updateUserId", "updateTime", "deletedAt", "version"
    })
    @Mapping(target = "headers", source = "headersJson", qualifiedByName = "parseHeaders")
    OutboxRecord toTarget(OutboxMessagePO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "messageId", source = "messageId")
    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    @Mapping(target = "messageFingerprint", source = "messageFingerprint")
    @Mapping(target = "channel", source = "channel")
    @Mapping(target = "destination", source = "destination")
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "contentType", source = "contentType")
    @Mapping(target = "schemaVersion", source = "schemaVersion")
    @Mapping(target = "headersJson", source = "headersJson")
    @Mapping(target = "traceId", source = "traceId")
    @Mapping(target = "maxAttempts", source = "maxAttempts")
    @Mapping(target = "nextAttemptAt", source = "availableAt")
    OutboxMessagePO toInsertPO(NewOutboxRecord source);

    @BeanMapping(ignoreUnmappedSourceProperties = "headers")
    @Mapping(target = "id", source = "record.id")
    @Mapping(target = "messageId", source = "record.messageId")
    @Mapping(target = "idempotencyKey", source = "record.idempotencyKey")
    @Mapping(target = "messageFingerprint", source = "record.messageFingerprint")
    @Mapping(target = "channel", source = "record.channel")
    @Mapping(target = "destination", source = "record.destination")
    @Mapping(target = "payload", source = "rawPayload")
    @Mapping(target = "contentType", source = "record.contentType")
    @Mapping(target = "schemaVersion", source = "record.schemaVersion")
    @Mapping(target = "headersJson", source = "rawHeadersJson")
    @Mapping(target = "traceId", source = "record.traceId")
    @Mapping(target = "status", source = "record.status")
    @Mapping(target = "attemptCount", source = "record.attemptCount")
    @Mapping(target = "maxAttempts", source = "record.maxAttempts")
    @Mapping(target = "nextAttemptAt", source = "record.nextAttemptAt")
    @Mapping(target = "lockedBy", source = "record.lockedBy")
    @Mapping(target = "lockedUntil", source = "record.lockedUntil")
    @Mapping(target = "lastErrorCode", source = "record.lastErrorCode")
    @Mapping(target = "lastErrorMessage", source = "record.lastErrorMessage")
    @Mapping(target = "createdAt", source = "record.createdAt")
    @Mapping(target = "updatedAt", source = "record.updatedAt")
    @Mapping(target = "completedAt", source = "record.completedAt")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", expression = "java(0L)")
    OutboxMessagePO toMigrationPO(OutboxRecord record, String rawPayload, String rawHeadersJson);
}
