package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiPublicationBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.model.event.DdcAiPublicationEvent;

import java.time.Instant;

/**
 * 发布回执与资源行到 EVENT-103 的不可逆投影。
 * / Irreversible projection of a publication receipt and its resource row into EVENT-103.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DdcAiPublicationEventConverter {

    default DdcAiPublicationEvent toEvent(DdcAiPublicationBO publication, DdcAiResourceBO resource,
            String contentSha256, String actor, Instant occurredAt, Long tenantId) {
        if (publication == null || publication.getId() == null || publication.getTargetVersionId() == null
                || publication.getTargetVersionNo() == null || resource == null || resource.getKind() == null) {
            throw new IllegalArgumentException("A committed publication and its resource are required");
        }
        return DdcAiPublicationEvent.builder()
                .eventId("ai-pub-" + publication.getId())
                .tenantId(tenantId)
                .resourceId(resource.getId())
                .kind(resource.getKind().getCode())
                .bizCode(resource.getBizCode())
                .namespaceCode(resource.getNamespaceCode())
                .env(resource.getEnvCode())
                .appCode(resource.getAppCode())
                .resourceKey(resource.getResourceKey())
                .operation(publication.getOperation().getValue())
                .versionId(publication.getTargetVersionId())
                .versionNo(publication.getTargetVersionNo())
                .contentSha256(contentSha256)
                .resourceRevision(publication.getResultRevision())
                .actor(actor)
                .occurredAt(occurredAt)
                .build();
    }
}
