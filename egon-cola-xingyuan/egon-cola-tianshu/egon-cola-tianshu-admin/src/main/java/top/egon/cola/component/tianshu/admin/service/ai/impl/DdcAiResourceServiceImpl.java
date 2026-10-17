package top.egon.cola.component.tianshu.admin.service.ai.impl;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiDraftNodeBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiPublicationBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionNodeBO;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiSkillDraftUploadCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiSkillPublishCommand;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationReceiptResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiRevisionResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionHeadResult;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiCreateCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDeleteCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDraftCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPromotionCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPublishCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiRollbackCommand;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiDraftNodeConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiPublicationConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceResultConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiVersionConverter;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiVersionSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPublicationOperationEnum;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPublicationPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiVersionPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiVersionQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiResourceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiDraftNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiPublicationRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionRepository;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiResourceService;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiSystemActor;
import top.egon.cola.component.tianshu.admin.service.ai.notification.DdcAiPublicationNotifier;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiOptimizationService;
import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiContentStrategy.PreparedContent;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service("ddcAiResourceService")
@RequiredArgsConstructor
@Validated
public class DdcAiResourceServiceImpl implements DdcAiResourceService {

    private static final int AI_NOT_FOUND = 56302;
    private static final int AI_NOT_PUBLISHED = 56306;
    private static final int AI_KIND_MISMATCH = 56308;
    private static final int AI_REVISION_CONFLICT = 56303;
    private static final int AI_RESOURCE_KEY_CONFLICT = 56304;
    private static final int AI_CONTENT_INTEGRITY = 56309;
    private static final int AI_IDEMPOTENCY_CONFLICT = 56305;
    private static final int AI_VERSION_COUNTER_EXHAUSTED = 56314;
    private static final int AI_LIFECYCLE_TIMESTAMP_CONFLICT = 56316;
    private static final String AI_NOT_FOUND_STATUS = "TIANSHU_AI_NOT_FOUND";
    private static final String AI_REVISION_CONFLICT_STATUS = "TIANSHU_AI_REVISION_CONFLICT";
    private static final String AI_RESOURCE_KEY_CONFLICT_STATUS = "TIANSHU_AI_DUPLICATE_RESOURCE_KEY";
    private static final String AI_CONTENT_INTEGRITY_STATUS = "TIANSHU_AI_CONTENT_INTEGRITY";
    private static final String AI_IDEMPOTENCY_CONFLICT_STATUS = "TIANSHU_AI_IDEMPOTENCY_CONFLICT";
    private static final String AI_VERSION_COUNTER_EXHAUSTED_STATUS = "TIANSHU_AI_VERSION_COUNTER_EXHAUSTED";
    private static final String AI_COMMAND_COUNTER = "tianshu.ai.commands";
    private static final String AI_LIFECYCLE_TIMESTAMP_CONFLICT_STATUS = "TIANSHU_AI_LIFECYCLE_CONFLICT";

    @Qualifier("ddcAiResourceRepository")
    private final DdcAiResourceRepository resourceRepository;

    @Qualifier("ddcAiDraftNodeRepository")
    private final DdcAiDraftNodeRepository draftNodeRepository;

    @Qualifier("ddcAiScopeValidator")
    private final DdcAiScopeValidator scopeValidator;

    @Qualifier("ddcAiResourceConverterImpl")
    private final DdcAiResourceConverter resourceConverter;

    @Qualifier("ddcAiDraftNodeConverterImpl")
    private final DdcAiDraftNodeConverter draftNodeConverter;

    @Qualifier("ddcAiVersionRepository")
    private final DdcAiVersionRepository versionRepository;

    @Qualifier("ddcAiVersionNodeRepository")
    private final DdcAiVersionNodeRepository versionNodeRepository;

    @Qualifier("ddcAiPublicationRepository")
    private final DdcAiPublicationRepository publicationRepository;

    @Qualifier("ddcAiVersionConverterImpl")
    private final DdcAiVersionConverter versionConverter;

    @Qualifier("ddcAiPublicationConverterImpl")
    private final DdcAiPublicationConverter publicationConverter;

    @Qualifier("ddcAiResourceResultConverterImpl")
    private final DdcAiResourceResultConverter resultConverter;

    @Qualifier("simpleMeterRegistry")
    private final MeterRegistry meterRegistry;

    @Qualifier("ddcAiContentStrategyRegistry")
    private final Map<DdcAiKindEnum, DdcAiContentStrategy> contentStrategyRegistry;

    @Qualifier("ddcAiReadTransactionTemplate")
    private final TransactionTemplate readTransactionTemplate;

    @Qualifier("ddcAiCommandTransactionTemplate")
    private final TransactionTemplate commandTransactionTemplate;

    @Qualifier("ddcAiPublicationNotifier")
    private final DdcAiPublicationNotifier publicationNotifier;

    @Qualifier("ddcAiOptimizationService")
    private final ObjectProvider<DdcAiOptimizationService> optimizationService;

    @Override
    public DdcAiResourceResult create(DdcAiCreateCommand command) {
        return commandTransactionTemplate.execute(status -> createInTransaction(command));
    }

    @Override
    public DdcAiResourceResult get(DdcAiIdQuery query) {
        return readTransactionTemplate.execute(status -> getInTransaction(query));
    }

    @Override
    public DdcPageResult<DdcAiResourceSummaryDTO> page(DdcAiPageQuery query) {
        return readTransactionTemplate.execute(status -> pageInTransaction(query));
    }

    @Override
    public DdcAiResourceResult replaceDraft(DdcAiIdQuery query, DdcAiDraftCommand command) {
        return observeAiCommand(Long.toString(query.id()), "DRAFT_SAVE", null,
                () -> executeWithLifecycleRetries(() -> replaceDraftInTransaction(query, command)),
                result -> result.getResource().getCurrentVersionId());
    }

    @Override
    public void delete(DdcAiIdQuery query, DdcAiDeleteCommand command) {
        executeWithLifecycleRetries(() -> {
            deleteInTransaction(query, command);
            return null;
        });
    }

    @Override
    public DdcAiPublicationResult publish(DdcAiIdQuery query, String requestId, DdcAiPublishCommand command) {
        return observeAiCommand(Long.toString(query.id()), DdcAiPublicationOperationEnum.PUBLISH.getValue(), requestId,
                () -> {
                    try {
                        return commandTransactionTemplate.execute(
                                status -> publishInTransaction(query, requestId, command));
                    } catch (DataIntegrityViolationException failure) {
                        if (!isPublicationRequestConflict(failure)) throw failure;
                        return replayAfterPublicationConflict(query, requestId, DdcAiPublicationOperationEnum.PUBLISH,
                                command.getExpectedRevision(), null, command.getChangeNote(), failure);
                    }
                }, result -> Long.toString(result.getTargetVersionId()));
    }

    @Override
    public DdcAiPublicationResult rollback(DdcAiIdQuery query, String requestId, DdcAiRollbackCommand command) {
        return observeAiCommand(Long.toString(query.id()), DdcAiPublicationOperationEnum.ROLLBACK.getValue(), requestId,
                () -> {
                    try {
                        return commandTransactionTemplate.execute(
                                status -> rollbackInTransaction(query, requestId, command));
                    } catch (DataIntegrityViolationException failure) {
                        if (!isPublicationRequestConflict(failure)) throw failure;
                        return replayAfterPublicationConflict(query, requestId, DdcAiPublicationOperationEnum.ROLLBACK,
                                command.getExpectedRevision(), command.getVersionNo(), command.getChangeNote(), failure);
                    }
                }, result -> Long.toString(result.getTargetVersionId()));
    }

    @Override
    public DdcAiVersionResult getVersion(DdcAiVersionQuery query) {
        return readTransactionTemplate.execute(status -> {
            DdcAiResourceBO resource = readableResource(query.resourceId());
            DdcAiVersionBO version = versionRepository.findByResourceIdAndVersionNo(
                            resource.getId(), query.versionNo())
                    .orElseThrow(DdcAiResourceServiceImpl::notFound);
            if (!Objects.equals(resource.getId(), version.getResourceId())
                    || !Objects.equals(resource.getKind(), version.getKind())
                    || !Objects.equals(query.versionNo(), version.getVersionNo())) {
                throw contentIntegrityFailure();
            }
            PreparedContent prepared = preparePersistedVersion(version,
                    versionNodeRepository.findTreeByVersionId(version.getId()));
            DdcAiVersionSummaryDTO summary = versionConverter.toVersionSummary(version);
            if (summary == null || summary.getCreatedAt() == null) throw contentIntegrityFailure();
            return DdcAiVersionResult.builder().version(summary)
                    .content(DdcAiContentDTO.builder().promptText(prepared.promptText()).nodes(prepared.nodes()).build())
                    .build();
        });
    }

    @Override
    public DdcAiVersionResult getPublishedVersion(
            DdcAiResourceQuery query,
            DdcAiKindEnum expectedKind) {
        return readTransactionTemplate.execute(status -> {
            DdcAiResourceBO resource = readableResource(query.resourceId());
            if (!Objects.equals(resource.getNamespaceCode(), query.namespaceCode())
                    || !Objects.equals(resource.getResourceKey(), query.resourceKey())) {
                throw notFound();
            }
            if (resource.getKind() != expectedKind) throw kindMismatch();

            Long versionNo = query.versionNo();
            boolean current = versionNo == null;
            if (current) {
                if (resource.getCurrentVersionId() == null || resource.getCurrentVersionNo() == null
                        || resource.getCurrentVersionId() <= 0 || resource.getCurrentVersionNo() <= 0) {
                    throw notPublished();
                }
                versionNo = resource.getCurrentVersionNo();
            }
            DdcAiVersionBO version = versionRepository.findByResourceIdAndVersionNo(
                            resource.getId(), versionNo)
                    .orElseThrow(DdcAiResourceServiceImpl::notFound);
            if (!Objects.equals(resource.getId(), version.getResourceId())
                    || !Objects.equals(expectedKind, version.getKind())
                    || !Objects.equals(versionNo, version.getVersionNo())
                    || (current && !Objects.equals(resource.getCurrentVersionId(), version.getId()))) {
                throw contentIntegrityFailure();
            }
            PreparedContent prepared = preparePersistedVersion(version,
                    versionNodeRepository.findTreeByVersionId(version.getId()));
            DdcAiVersionSummaryDTO summary = versionConverter.toVersionSummary(version);
            if (summary == null || summary.getCreatedAt() == null) throw contentIntegrityFailure();
            return DdcAiVersionResult.builder().version(summary)
                    .content(DdcAiContentDTO.builder().promptText(prepared.promptText()).nodes(prepared.nodes()).build())
                    .build();
        });
    }

    @Override
    public DdcAiPublicationResult promote(DdcAiIdQuery sourceQuery, DdcAiPromotionCommand command, String requestId) {
        return observeAiCommand(Long.toString(sourceQuery.id()), DdcAiPublicationOperationEnum.PROMOTE.getValue(),
                requestId, () -> {
                    try {
                        return commandTransactionTemplate.execute(
                                status -> promoteInTransaction(sourceQuery, command, requestId));
                    } catch (DataIntegrityViolationException failure) {
                        if (!isPublicationRequestConflict(failure) && !isResourceKeyConflict(failure)) throw failure;
                        return replayAfterPromotionConflict(sourceQuery, command, requestId, failure);
                    }
                }, result -> Long.toString(result.getTargetVersionId()));
    }

    @Override
    public DdcPageResult<DdcAiVersionSummaryDTO> pageVersions(DdcAiVersionPageQuery query) {
        return readTransactionTemplate.execute(status -> {
            DdcAiResourceBO resource = readableResource(query.getResourceId());
            PageQuery page = new PageQuery(query.getPageNo(), query.getPageSize());
            DdcPageResult<DdcAiVersionBO> rows = versionRepository.pageByResourceId(resource.getId(), page);
            List<DdcAiVersionSummaryDTO> records = rows.getRecords().stream()
                    .map(versionConverter::toVersionSummary).toList();
            if (records.stream().anyMatch(summary -> summary == null || summary.getCreatedAt() == null)) {
                throw contentIntegrityFailure();
            }
            return DdcPageResult.<DdcAiVersionSummaryDTO>builder().records(records).total(rows.getTotal())
                    .pageNo(rows.getPageNo()).pageSize(rows.getPageSize()).build();
        });
    }

    @Override
    public DdcPageResult<DdcAiPublicationResult> pagePublications(DdcAiPublicationPageQuery query) {
        return readTransactionTemplate.execute(status -> {
            DdcAiResourceBO resource = readableResource(query.getResourceId());
            PageQuery page = new PageQuery(query.getPageNo(), query.getPageSize());
            DdcPageResult<DdcAiPublicationBO> rows = publicationRepository.pageByResourceId(
                    resource.getId(), query.getRequestId(), page);
            List<DdcAiPublicationResult> records = rows.getRecords().stream()
                    .map(this::publicationResult).toList();
            return DdcPageResult.<DdcAiPublicationResult>builder().records(records).total(rows.getTotal())
                    .pageNo(rows.getPageNo()).pageSize(rows.getPageSize()).build();
        });
    }

    @Override
    public DdcAiVersionHeadResult getVersionHead(DdcAiResourceQuery query) {
        if (query.versionNo() != null) throw badRequest("A version head query must not pin a version");
        return readTransactionTemplate.execute(status -> {
            DdcAiResourceBO resource = readableResource(query.resourceId());
            requireSyncIdentity(resource, query.namespaceCode(), query.resourceKey());
            DdcAiVersionHeadResult.DdcAiVersionHeadResultBuilder head = DdcAiVersionHeadResult.builder()
                    .resourceId(resource.getId()).resourceKey(resource.getResourceKey())
                    .resourceRevision(resource.getVersion());
            if (resource.getCurrentVersionId() == null) return head.published(false).build();
            DdcAiVersionBO version = currentVersion(resource.getId(), resource.getCurrentVersionId());
            return head.published(true).versionId(version.getId()).versionNo(version.getVersionNo())
                    .contentSha256(version.getContentSha256()).build();
        });
    }

    @Override
    public DdcAiRevisionResult replaceSkillDraft(DdcAiSkillDraftUploadCommand command) {
        DdcAiIdQuery query = new DdcAiIdQuery(command.getResourceId());
        DdcAiDraftCommand draft = DdcAiDraftCommand.builder().expectedRevision(command.getExpectedRevision())
                .displayName(command.getDisplayName()).description(command.getDescription())
                .draft(command.getContent()).build();
        return observeAiCommand(Long.toString(query.id()), "DRAFT_SAVE", null,
                () -> executeWithLifecycleRetries(() -> {
                    DdcAiResourceBO candidate = resourceRepository.findActiveById(query.id())
                            .orElseThrow(DdcAiResourceServiceImpl::notFound);
                    scopeValidator.assertCanWrite(candidate);
                    requireSkillSyncTarget(candidate, command.getNamespaceCode(), command.getResourceKey());
                    DdcAiResourceResult saved = replaceDraftInTransaction(query, draft);
                    String draftSha256 = strategy(DdcAiKindEnum.SKILL).prepare(command.getContent()).contentHash();
                    String currentVersionId = saved.getResource().getCurrentVersionId();
                    String currentSha256 = currentVersionId == null ? null
                            : currentVersion(query.id(), Long.parseLong(currentVersionId)).getContentSha256();
                    return DdcAiRevisionResult.builder().resourceId(query.id())
                            .resourceRevision(Long.parseLong(saved.getResource().getRevision()))
                            .draftSha256(draftSha256)
                            .draftMatchesCurrent(Objects.equals(draftSha256, currentSha256)).build();
                }), result -> null);
    }

    @Override
    public DdcAiPublicationReceiptResult publishSkillDraft(DdcAiSkillPublishCommand command) {
        DdcAiIdQuery query = new DdcAiIdQuery(command.getResourceId());
        DdcAiPublishCommand publish = DdcAiPublishCommand.builder()
                .expectedRevision(command.getExpectedRevision()).changeNote(command.getChangeNote()).build();
        String requestId = command.getRequestId();
        return observeAiCommand(Long.toString(query.id()), DdcAiPublicationOperationEnum.PUBLISH.getValue(), requestId,
                () -> {
                    DdcAiPublicationResult receipt;
                    try {
                        receipt = commandTransactionTemplate.execute(status -> {
                            DdcAiResourceBO candidate = resourceRepository.findActiveById(query.id())
                                    .orElseThrow(DdcAiResourceServiceImpl::notFound);
                            scopeValidator.assertCanPublish(candidate);
                            requireSkillSyncTarget(candidate, command.getNamespaceCode(), command.getResourceKey());
                            return publishInTransaction(query, requestId, publish);
                        });
                    } catch (DataIntegrityViolationException failure) {
                        if (!isPublicationRequestConflict(failure)) throw failure;
                        receipt = replayAfterPublicationConflict(query, requestId, DdcAiPublicationOperationEnum.PUBLISH,
                                publish.getExpectedRevision(), null, publish.getChangeNote(), failure);
                    }
                    DdcAiPublicationResult published = receipt;
                    String contentSha256 = readTransactionTemplate.execute(status ->
                            currentVersion(query.id(), published.getTargetVersionId()).getContentSha256());
                    return DdcAiPublicationReceiptResult.builder().publicationId(published.getPublicationId())
                            .resourceId(published.getResourceId()).targetVersionId(published.getTargetVersionId())
                            .targetVersionNo(published.getTargetVersionNo())
                            .resourceRevision(published.getResourceRevision()).contentSha256(contentSha256).build();
                }, result -> Long.toString(result.getTargetVersionId()));
    }

    /** 同步通道的资源身份：namespace 与 key 必须与持久化资源一致。 / Sync identity: namespace and key must match. */
    private void requireSyncIdentity(DdcAiResourceBO resource, String namespaceCode, String resourceKey) {
        if (!Objects.equals(resource.getNamespaceCode(), namespaceCode)
                || !Objects.equals(resource.getResourceKey(), resourceKey)) {
            throw notFound();
        }
    }

    private void requireSkillSyncTarget(DdcAiResourceBO resource, String namespaceCode, String resourceKey) {
        requireSyncIdentity(resource, namespaceCode, resourceKey);
        if (resource.getKind() != DdcAiKindEnum.SKILL) throw kindMismatch();
    }

    private DdcAiVersionBO currentVersion(Long resourceId, Long versionId) {
        DdcAiVersionBO version = versionRepository.findActiveById(versionId)
                .orElseThrow(DdcAiResourceServiceImpl::contentIntegrityFailure);
        if (!Objects.equals(resourceId, version.getResourceId())) throw contentIntegrityFailure();
        return version;
    }

    private DdcAiPublicationResult publishInTransaction(DdcAiIdQuery query, String requestId,
            DdcAiPublishCommand command) {
        DdcAiResourceBO candidate = resourceRepository.findActiveById(query.id())
                .orElseThrow(DdcAiResourceServiceImpl::notFound);
        scopeValidator.assertCanPublish(candidate);
        String requestSha256 = commandSha256(DdcAiPublicationOperationEnum.PUBLISH, authenticatedActor(), candidate,
                command.getExpectedRevision(), null, command.getChangeNote());
        Optional<DdcAiPublicationBO> committed = publicationRepository.findByTenantRequestId(requestId);
        if (committed.isPresent()) return replayOrConflict(committed.get(), requestSha256);

        DdcAiResourceBO resource = lockResourceAfterScope(candidate);
        committed = publicationRepository.findByTenantRequestId(requestId);
        if (committed.isPresent()) return replayOrConflict(committed.get(), requestSha256);
        requireVersionCommandRevision(command.getExpectedRevision(), resource.getVersion());

        long versionNo = nextVersionNo(resource.getLastVersionNo());
        List<DdcAiDraftNodeBO> draftNodes = draftNodeRepository.findActiveByResourceId(resource.getId());
        PreparedContent prepared = preparePersistedDraft(resource, draftNodes);
        DdcAiVersionBO version = DdcAiVersionBO.builder().resourceId(resource.getId()).versionNo(versionNo)
                .kind(resource.getKind()).displayName(resource.getDisplayName()).description(resource.getDescription())
                .promptText(prepared.promptText()).contentSha256(prepared.contentHash())
                .contentBytes(prepared.contentBytes()).nodeCount(prepared.nodeCount())
                .sourceVersionId(null).changeNote(command.getChangeNote()).build();
        DdcAiVersionBO insertedVersion = versionRepository.insertVersion(version);
        if (insertedVersion == null || insertedVersion.getId() == null) {
            throw new IllegalStateException("AI_VERSION_INSERT_RETURNED_NO_ID");
        }
        versionNodeRepository.insertAll(versionNodes(insertedVersion.getId(), prepared));

        Long previousVersionId = resource.getCurrentVersionId();
        resource.setLastVersionNo(versionNo).setCurrentVersionId(insertedVersion.getId())
                .setCurrentVersionNo(versionNo);
        DdcAiResourceBO updated = resourceRepository.update(resource)
                .orElseThrow(DdcAiResourceServiceImpl::revisionConflict);
        DdcAiPublicationBO publication = DdcAiPublicationBO.builder().resourceId(resource.getId())
                .operation(DdcAiPublicationOperationEnum.PUBLISH).requestId(requestId)
                .requestSha256(requestSha256).previousVersionId(previousVersionId)
                .targetVersionId(insertedVersion.getId()).targetVersionNo(versionNo).sourceVersionId(null)
                .resultRevision(updated.getVersion()).targetCreated(false).build();
        DdcAiPublicationBO receipt = publicationRepository.insertPublication(publication);
        publicationNotifier.enqueue(receipt, updated, prepared.contentHash(), authenticatedActor());
        return publicationResult(receipt);
    }

    private DdcAiPublicationResult rollbackInTransaction(DdcAiIdQuery query, String requestId,
            DdcAiRollbackCommand command) {
        DdcAiResourceBO candidate = resourceRepository.findActiveById(query.id())
                .orElseThrow(DdcAiResourceServiceImpl::notFound);
        scopeValidator.assertCanPublish(candidate);
        String requestSha256 = commandSha256(DdcAiPublicationOperationEnum.ROLLBACK, authenticatedActor(), candidate,
                command.getExpectedRevision(), command.getVersionNo(), command.getChangeNote());
        Optional<DdcAiPublicationBO> committed = publicationRepository.findByTenantRequestId(requestId);
        if (committed.isPresent()) return replayOrConflict(committed.get(), requestSha256);

        DdcAiResourceBO resource = lockResourceAfterScope(candidate);
        committed = publicationRepository.findByTenantRequestId(requestId);
        if (committed.isPresent()) return replayOrConflict(committed.get(), requestSha256);
        requireVersionCommandRevision(command.getExpectedRevision(), resource.getVersion());
        long versionNo = parsePositiveVersionNo(command.getVersionNo());
        DdcAiVersionBO target = versionRepository.findByResourceIdAndVersionNo(resource.getId(), versionNo)
                .orElseThrow(DdcAiResourceServiceImpl::notFound);
        if (!Objects.equals(resource.getId(), target.getResourceId())
                || !Objects.equals(resource.getKind(), target.getKind())
                || !Objects.equals(versionNo, target.getVersionNo())) {
            throw contentIntegrityFailure();
        }
        preparePersistedVersion(target, versionNodeRepository.findTreeByVersionId(target.getId()));

        Long previousVersionId = resource.getCurrentVersionId();
        resource.setCurrentVersionId(target.getId()).setCurrentVersionNo(target.getVersionNo());
        DdcAiResourceBO updated = resourceRepository.update(resource)
                .orElseThrow(DdcAiResourceServiceImpl::revisionConflict);
        DdcAiPublicationBO publication = DdcAiPublicationBO.builder().resourceId(resource.getId())
                .operation(DdcAiPublicationOperationEnum.ROLLBACK).requestId(requestId)
                .requestSha256(requestSha256).previousVersionId(previousVersionId)
                .targetVersionId(target.getId()).targetVersionNo(target.getVersionNo()).sourceVersionId(null)
                .resultRevision(updated.getVersion()).targetCreated(false).build();
        DdcAiPublicationBO receipt = publicationRepository.insertPublication(publication);
        publicationNotifier.enqueue(receipt, updated, target.getContentSha256(), authenticatedActor());
        return publicationResult(receipt);
    }

    private DdcAiPublicationResult promoteInTransaction(DdcAiIdQuery sourceQuery,
            DdcAiPromotionCommand command, String requestId) {
        DdcAiResourceBO sourceCandidate = resourceRepository.findActiveById(sourceQuery.id())
                .orElseThrow(DdcAiResourceServiceImpl::notFound);
        scopeValidator.assertCanRead(sourceCandidate);
        DdcAiScopeDTO targetScope = command.getTargetScope();
        Long targetNamespaceId = scopeValidator.resolveNamespaceIdForPublish(targetScope);
        long sourceVersionNo = parsePromotionSourceVersionNo(command.getSourceVersionNo());
        Long expectedTargetRevision = parseNullableTargetRevision(command.getExpectedTargetRevision());
        if (sameResourceIdentity(sourceCandidate, targetScope, targetNamespaceId,
                command.getTargetResourceKey())) {
            throw resourceKeyConflict();
        }

        String requestSha256 = promotionCommandSha256(authenticatedActor(), sourceCandidate, command);
        Optional<DdcAiPublicationBO> committed = publicationRepository.findByTenantRequestId(requestId);
        if (committed.isPresent()) return replayOrConflict(committed.get(), requestSha256);

        Optional<DdcAiResourceBO> targetCandidate = resourceRepository.findByBusinessKey(
                targetScope.bizCode(), targetNamespaceId, targetScope.env(), targetScope.appCode(),
                sourceCandidate.getKind(), command.getTargetResourceKey());
        if (targetCandidate.isPresent() && expectedTargetRevision == null) throw revisionConflict();
        if (targetCandidate.isEmpty() && expectedTargetRevision != null) throw revisionConflict();
        if (targetCandidate.isEmpty()) {
            Long writableNamespaceId = scopeValidator.resolveNamespaceIdForWrite(targetScope);
            if (!Objects.equals(targetNamespaceId, writableNamespaceId)) throw scopeForbidden();
        }

        DdcAiResourceBO source;
        DdcAiResourceBO target = null;
        if (targetCandidate.isEmpty()) {
            source = lockPromotionResource(sourceCandidate);
        } else if (sourceCandidate.getId() < targetCandidate.get().getId()) {
            source = lockPromotionResource(sourceCandidate);
            target = lockPromotionResource(targetCandidate.get());
        } else {
            target = lockPromotionResource(targetCandidate.get());
            source = lockPromotionResource(sourceCandidate);
        }

        committed = publicationRepository.findByTenantRequestId(requestId);
        if (committed.isPresent()) return replayOrConflict(committed.get(), requestSha256);
        scopeValidator.assertCanRead(source);
        if (target == null) {
            if (resourceRepository.findByBusinessKey(targetScope.bizCode(), targetNamespaceId, targetScope.env(),
                    targetScope.appCode(), source.getKind(), command.getTargetResourceKey()).isPresent()) {
                throw revisionConflict();
            }
        } else if (!Objects.equals(expectedTargetRevision, target.getVersion())) {
            throw revisionConflict();
        }

        DdcAiVersionBO sourceVersion = versionRepository.findByResourceIdAndVersionNo(source.getId(), sourceVersionNo)
                .orElseThrow(DdcAiResourceServiceImpl::notFound);
        if (!Objects.equals(source.getId(), sourceVersion.getResourceId())
                || !Objects.equals(source.getKind(), sourceVersion.getKind())
                || !Objects.equals(sourceVersionNo, sourceVersion.getVersionNo())) {
            throw contentIntegrityFailure();
        }
        PreparedContent snapshot = preparePersistedVersion(sourceVersion,
                versionNodeRepository.findTreeByVersionId(sourceVersion.getId()));
        boolean targetCreated = target == null;
        if (targetCreated) {
            target = resourceRepository.insert(DdcAiResourceBO.builder()
                    .bizCode(targetScope.bizCode()).namespaceId(targetNamespaceId)
                    .namespaceCode(targetScope.namespaceCode()).envCode(targetScope.env())
                    .appCode(targetScope.appCode()).kind(sourceVersion.getKind())
                    .resourceKey(command.getTargetResourceKey()).displayName(sourceVersion.getDisplayName())
                    .description(sourceVersion.getDescription()).draftPrompt(snapshot.promptText())
                    .version(0L).lastVersionNo(0L).currentVersionId(null).currentVersionNo(null).build());
            if (target == null || target.getId() == null) {
                throw new IllegalStateException("AI_PROMOTION_TARGET_INSERT_RETURNED_NO_ID");
            }
            if (!Objects.equals(source.getTenantId(), target.getTenantId())) throw contentIntegrityFailure();
            target.setNamespaceCode(targetScope.namespaceCode());
            insertDraftNodes(target.getId(), snapshot);
        } else if (!Objects.equals(source.getTenantId(), target.getTenantId())) {
            throw notFound();
        }

        long targetVersionNo = nextVersionNo(target.getLastVersionNo());
        DdcAiVersionBO promotedVersion = DdcAiVersionBO.builder().resourceId(target.getId())
                .versionNo(targetVersionNo).kind(sourceVersion.getKind())
                .displayName(sourceVersion.getDisplayName()).description(sourceVersion.getDescription())
                .promptText(snapshot.promptText()).contentSha256(snapshot.contentHash())
                .contentBytes(snapshot.contentBytes()).nodeCount(snapshot.nodeCount())
                .sourceVersionId(sourceVersion.getId()).changeNote(command.getChangeNote()).build();
        DdcAiVersionBO insertedVersion = versionRepository.insertVersion(promotedVersion);
        if (insertedVersion == null || insertedVersion.getId() == null) {
            throw new IllegalStateException("AI_PROMOTION_VERSION_INSERT_RETURNED_NO_ID");
        }
        versionNodeRepository.insertAll(versionNodes(insertedVersion.getId(), snapshot));

        Long previousVersionId = target.getCurrentVersionId();
        target.setLastVersionNo(targetVersionNo).setCurrentVersionId(insertedVersion.getId())
                .setCurrentVersionNo(targetVersionNo);
        DdcAiResourceBO updatedTarget = resourceRepository.update(target)
                .orElseThrow(DdcAiResourceServiceImpl::revisionConflict);
        DdcAiPublicationBO publication = DdcAiPublicationBO.builder().resourceId(updatedTarget.getId())
                .operation(DdcAiPublicationOperationEnum.PROMOTE).requestId(requestId)
                .requestSha256(requestSha256).previousVersionId(previousVersionId)
                .targetVersionId(insertedVersion.getId()).targetVersionNo(targetVersionNo)
                .sourceVersionId(sourceVersion.getId()).resultRevision(updatedTarget.getVersion())
                .targetCreated(targetCreated).build();
        DdcAiPublicationBO receipt = publicationRepository.insertPublication(publication);
        publicationNotifier.enqueue(receipt, updatedTarget, snapshot.contentHash(), authenticatedActor());
        return publicationResult(receipt);
    }

    private DdcAiResourceBO readableResource(Long resourceId) {
        DdcAiResourceBO resource = resourceRepository.findActiveForShareById(resourceId)
                .orElseThrow(DdcAiResourceServiceImpl::notFound);
        scopeValidator.assertCanRead(resource);
        return resource;
    }

    private DdcAiResourceBO lockResourceAfterScope(DdcAiResourceBO candidate) {
        DdcAiResourceBO resource = resourceRepository.findActiveForUpdateById(candidate.getId())
                .orElseThrow(DdcAiResourceServiceImpl::notFound);
        if (!Objects.equals(candidate.getTenantId(), resource.getTenantId())
                || !Objects.equals(candidate.getBizCode(), resource.getBizCode())
                || !Objects.equals(candidate.getNamespaceId(), resource.getNamespaceId())
                || !Objects.equals(candidate.getEnvCode(), resource.getEnvCode())
                || !Objects.equals(candidate.getAppCode(), resource.getAppCode())
                || !Objects.equals(candidate.getKind(), resource.getKind())
                || !Objects.equals(candidate.getResourceKey(), resource.getResourceKey())) {
            throw revisionConflict();
        }
        return resource;
    }

    private DdcAiResourceBO lockPromotionResource(DdcAiResourceBO candidate) {
        DdcAiResourceBO resource = resourceRepository.findActiveForUpdateById(candidate.getId())
                .orElseThrow(DdcAiResourceServiceImpl::notFound);
        if (!sameResourceIdentity(candidate, resource)) throw revisionConflict();
        return resource;
    }

    private boolean sameResourceIdentity(DdcAiResourceBO left, DdcAiResourceBO right) {
        return Objects.equals(left.getTenantId(), right.getTenantId())
                && Objects.equals(left.getBizCode(), right.getBizCode())
                && Objects.equals(left.getNamespaceId(), right.getNamespaceId())
                && Objects.equals(left.getEnvCode(), right.getEnvCode())
                && Objects.equals(left.getAppCode(), right.getAppCode())
                && Objects.equals(left.getKind(), right.getKind())
                && Objects.equals(left.getResourceKey(), right.getResourceKey());
    }

    private boolean sameResourceIdentity(DdcAiResourceBO source, DdcAiScopeDTO targetScope,
            Long targetNamespaceId, String targetResourceKey) {
        return Objects.equals(source.getBizCode(), targetScope.bizCode())
                && Objects.equals(source.getNamespaceId(), targetNamespaceId)
                && Objects.equals(source.getEnvCode(), targetScope.env())
                && Objects.equals(source.getAppCode(), targetScope.appCode())
                && Objects.equals(source.getResourceKey(), targetResourceKey);
    }

    private DdcAiPublicationResult replayAfterPublicationConflict(DdcAiIdQuery query, String requestId,
            DdcAiPublicationOperationEnum operation, String expectedRevision, String versionNo, String changeNote,
            DataIntegrityViolationException failure) {
        return commandTransactionTemplate.execute(status -> {
            DdcAiResourceBO candidate = resourceRepository.findActiveById(query.id())
                    .orElseThrow(DdcAiResourceServiceImpl::notFound);
            scopeValidator.assertCanPublish(candidate);
            String requestSha256 = commandSha256(operation, authenticatedActor(), candidate,
                    expectedRevision, versionNo, changeNote);
            return publicationRepository.findByTenantRequestId(requestId)
                    .map(receipt -> replayOrConflict(receipt, requestSha256))
                    .orElseThrow(() -> failure);
        });
    }

    private DdcAiPublicationResult replayAfterPromotionConflict(DdcAiIdQuery sourceQuery,
            DdcAiPromotionCommand command, String requestId, DataIntegrityViolationException failure) {
        return commandTransactionTemplate.execute(status -> {
            DdcAiResourceBO source = resourceRepository.findActiveById(sourceQuery.id())
                    .orElseThrow(DdcAiResourceServiceImpl::notFound);
            scopeValidator.assertCanRead(source);
            Long targetNamespaceId = scopeValidator.resolveNamespaceIdForPublish(command.getTargetScope());
            parsePromotionSourceVersionNo(command.getSourceVersionNo());
            if (sameResourceIdentity(source, command.getTargetScope(), targetNamespaceId,
                    command.getTargetResourceKey())) {
                throw resourceKeyConflict();
            }
            String requestSha256 = promotionCommandSha256(authenticatedActor(), source, command);
            Optional<DdcAiPublicationBO> committed = publicationRepository.findByTenantRequestId(requestId);
            if (committed.isPresent()) return replayOrConflict(committed.get(), requestSha256);
            if (isResourceKeyConflict(failure)) throw resourceKeyConflict(failure);
            throw failure;
        });
    }

    private DdcAiPublicationResult replayOrConflict(DdcAiPublicationBO receipt, String requestSha256) {
        if (!Objects.equals(receipt.getRequestSha256(), requestSha256)) throw idempotencyConflict();
        return publicationResult(receipt);
    }

    private DdcAiPublicationResult publicationResult(DdcAiPublicationBO publication) {
        if (publication == null || publication.getTargetVersionNo() == null
                || publication.getTargetVersionNo() <= 0 || publication.getCreateTime() == null) {
            throw contentIntegrityFailure();
        }
        DdcAiPublicationResult result = publicationConverter.toResult(publication);
        if (result == null) throw contentIntegrityFailure();
        return result;
    }

    private List<DdcAiVersionNodeBO> versionNodes(Long versionId, PreparedContent prepared) {
        return prepared.nodes().stream().map(node -> {
            boolean file = node.getNodeType() == DdcAiNodeTypeEnum.FILE;
            return DdcAiVersionNodeBO.builder().versionId(versionId).parentPath(parentPath(node.getPath()))
                    .path(node.getPath()).nodeType(node.getNodeType()).content(node.getContent())
                    .contentSha256(file ? prepared.fileSha256ByPath().get(node.getPath()) : null)
                    .contentBytes(file ? (long) node.getContent().getBytes(StandardCharsets.UTF_8).length : 0L)
                    .build();
        }).toList();
    }

    private PreparedContent preparePersistedVersion(DdcAiVersionBO version, List<DdcAiVersionNodeBO> persistedNodes) {
        if (version.getId() == null || version.getResourceId() == null || version.getKind() == null
                || version.getContentSha256() == null || version.getContentBytes() == null
                || version.getNodeCount() == null) {
            throw contentIntegrityFailure();
        }
        List<DdcAiNodeDTO> nodes = persistedNodes.stream().map(node -> DdcAiNodeDTO.builder()
                .path(node.getPath()).nodeType(node.getNodeType()).content(node.getContent()).build()).toList();
        PreparedContent prepared;
        try {
            prepared = strategy(version.getKind()).prepare(DdcAiContentDTO.builder()
                    .promptText(version.getPromptText()).nodes(nodes).build());
        } catch (ConstraintViolationException | IllegalArgumentException failure) {
            throw contentIntegrityFailure(failure);
        }
        if (!Objects.equals(version.getContentSha256(), prepared.contentHash())
                || !Objects.equals(version.getContentBytes(), prepared.contentBytes())
                || !Objects.equals(version.getNodeCount(), prepared.nodeCount())
                || persistedNodes.size() != prepared.nodes().size()) {
            throw contentIntegrityFailure();
        }
        Map<String, DdcAiVersionNodeBO> rowsByPath = new HashMap<>();
        persistedNodes.forEach(node -> rowsByPath.put(node.getPath(), node));
        for (DdcAiNodeDTO node : prepared.nodes()) {
            DdcAiVersionNodeBO persisted = rowsByPath.remove(node.getPath());
            if (persisted == null || persisted.getNodeType() != node.getNodeType()
                    || !Objects.equals(persisted.getContent(), node.getContent())
                    || !Objects.equals(persisted.getParentPath(), parentPath(node.getPath()))) {
                throw contentIntegrityFailure();
            }
            boolean file = node.getNodeType() == DdcAiNodeTypeEnum.FILE;
            long bytes = file ? node.getContent().getBytes(StandardCharsets.UTF_8).length : 0L;
            String hash = file ? prepared.fileSha256ByPath().get(node.getPath()) : null;
            if (!Objects.equals(persisted.getContentBytes(), bytes)
                    || !Objects.equals(persisted.getContentSha256(), hash)) {
                throw contentIntegrityFailure();
            }
        }
        if (!rowsByPath.isEmpty()) throw contentIntegrityFailure();
        return prepared;
    }

    private long nextVersionNo(Long lastVersionNo) {
        if (lastVersionNo == null || lastVersionNo < 0) throw contentIntegrityFailure();
        try {
            return Math.addExact(lastVersionNo, 1L);
        } catch (ArithmeticException failure) {
            throw versionCounterExhausted(failure);
        }
    }

    private long parsePositiveVersionNo(String versionNo) {
        try {
            long parsed = Long.parseLong(versionNo);
            if (parsed > 0) return parsed;
        } catch (NumberFormatException failure) {
            throw badRequest("Invalid version number");
        }
        throw badRequest("Invalid version number");
    }

    private long parsePromotionSourceVersionNo(String versionNo) {
        Long parsed;
        try {
            parsed = resourceConverter.toInternalLong(versionNo);
        } catch (NumberFormatException failure) {
            throw badRequest("Invalid source version number");
        }
        if (parsed == null || parsed <= 0) throw badRequest("Invalid source version number");
        return parsed;
    }

    private Long parseNullableTargetRevision(String expectedTargetRevision) {
        if (expectedTargetRevision == null) return null;
        try {
            Long parsed = resourceConverter.toInternalLong(expectedTargetRevision);
            if (parsed != null && parsed >= 0) return parsed;
        } catch (NumberFormatException failure) {
            throw badRequest("Invalid target resource revision");
        }
        throw badRequest("Invalid target resource revision");
    }

    private String authenticatedActor() {
        Optional<String> systemActor = DdcAiSystemActor.current();
        if (systemActor.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
            return systemActor.get();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Rbac3UserDetails user)
                || user.identitySub() == null || user.identitySub().isBlank()) {
            throw scopeForbidden();
        }
        return user.identitySub();
    }

    private String commandSha256(DdcAiPublicationOperationEnum operation, String actor, DdcAiResourceBO resource,
            String expectedRevision, String versionNo, String changeNote) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateLengthPrefixed(digest, "tianshu-ai-command-v1");
            updateLengthPrefixed(digest, operation.getValue());
            updateLengthPrefixed(digest, actor);
            updateLengthPrefixed(digest, Long.toString(resource.getTenantId()));
            updateLengthPrefixed(digest, Long.toString(resource.getId()));
            updateLengthPrefixed(digest, resource.getBizCode());
            updateLengthPrefixed(digest, Long.toString(resource.getNamespaceId()));
            updateLengthPrefixed(digest, resource.getEnvCode());
            updateLengthPrefixed(digest, resource.getAppCode());
            updateLengthPrefixed(digest, resource.getKind().name());
            updateLengthPrefixed(digest, resource.getResourceKey());
            updateNullableLengthPrefixed(digest, expectedRevision);
            updateNullableLengthPrefixed(digest, versionNo);
            updateNullableLengthPrefixed(digest, changeNote);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    private String promotionCommandSha256(String actor, DdcAiResourceBO source,
            DdcAiPromotionCommand command) {
        DdcAiScopeDTO targetScope = command.getTargetScope();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateLengthPrefixed(digest, "tianshu-ai-command-v1");
            updateLengthPrefixed(digest, DdcAiPublicationOperationEnum.PROMOTE.getValue());
            updateLengthPrefixed(digest, actor);
            updateLengthPrefixed(digest, Long.toString(source.getTenantId()));
            updateLengthPrefixed(digest, Long.toString(source.getId()));
            updateLengthPrefixed(digest, command.getSourceVersionNo());
            updateLengthPrefixed(digest, targetScope.bizCode());
            updateLengthPrefixed(digest, targetScope.namespaceCode());
            updateLengthPrefixed(digest, targetScope.env());
            updateLengthPrefixed(digest, targetScope.appCode());
            updateLengthPrefixed(digest, source.getKind().name());
            updateLengthPrefixed(digest, command.getTargetResourceKey());
            updateNullableLengthPrefixed(digest, command.getExpectedTargetRevision());
            updateNullableLengthPrefixed(digest, command.getChangeNote());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    private void updateNullableLengthPrefixed(MessageDigest digest, String value) {
        digest.update((byte) (value == null ? 0 : 1));
        if (value != null) updateLengthPrefixed(digest, value);
    }

    private void updateLengthPrefixed(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private boolean isPublicationRequestConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains("uk_ai_publication_request")) return true;
        }
        return false;
    }

    private <T> T observeAiCommand(String resourceId, String operation, String requestId, Supplier<T> command,
            Function<T, String> versionIdResolver) {
        long startedAt = System.nanoTime();
        String tenantId = MDC.get("tenantId");
        String requestIdHash = requestId == null ? null
                : DdcAiContentStrategy.sha256Hex(requestId.getBytes(StandardCharsets.UTF_8));
        try {
            T result = command.get();
            String versionId = result == null || versionIdResolver == null ? null : versionIdResolver.apply(result);
            recordAiCommand(operation, "success");
            log.info("AI command completed tenantId={} rootId={} versionId={} operation={} requestIdHash={} "
                            + "result=success durationMs={} traceId={}",
                    tenantId, resourceId, versionId, operation, requestIdHash,
                    (System.nanoTime() - startedAt) / 1_000_000L,
                    TraceContext.current().map(TraceContext::traceId).orElse(null));
            return result;
        } catch (RuntimeException failure) {
            recordAiCommand(operation, "failure");
            log.warn("AI command failed tenantId={} rootId={} versionId={} operation={} requestIdHash={} "
                            + "result=failure durationMs={} traceId={}",
                    tenantId, resourceId, null, operation, requestIdHash,
                    (System.nanoTime() - startedAt) / 1_000_000L,
                    TraceContext.current().map(TraceContext::traceId).orElse(null));
            throw failure;
        }
    }

    private void recordAiCommand(String operation, String result) {
        try {
            meterRegistry.counter(AI_COMMAND_COUNTER, "operation", operation, "result", result).increment();
        } catch (RuntimeException failure) {
            log.warn("AI command metric recording failed operation={} result={}", operation, result);
        }
    }

    private DdcAiResourceResult createInTransaction(DdcAiCreateCommand command) {
        DdcAiScopeDTO scope = command.getScope();
        Long namespaceId = scopeValidator.resolveNamespaceIdForWrite(scope);
        DdcAiKindEnum kind = command.getKind();
        if (resourceRepository.findByBusinessKey(scope.bizCode(), namespaceId, scope.env(), scope.appCode(),
                kind, command.getResourceKey()).isPresent()) {
            throw resourceKeyConflict();
        }

        PreparedContent prepared = strategy(kind).prepare(command.getDraft());
        DdcAiResourceBO resource = resourceConverter.toBusiness(command)
                .setNamespaceId(namespaceId)
                .setNamespaceCode(scope.namespaceCode())
                .setDisplayName(command.getDisplayName().trim())
                .setDraftPrompt(prepared.promptText())
                .setLastVersionNo(0L)
                .setCurrentVersionId(null)
                .setCurrentVersionNo(null);
        DdcAiResourceBO created;
        try {
            created = resourceRepository.insert(resource);
        } catch (DataIntegrityViolationException failure) {
            if (isResourceKeyConflict(failure)) throw resourceKeyConflict(failure);
            throw failure;
        }
        if (created == null || created.getId() == null) {
            throw new IllegalStateException("AI_RESOURCE_INSERT_RETURNED_NO_ID");
        }
        created.setNamespaceCode(scope.namespaceCode());
        insertDraftNodes(created.getId(), prepared);
        return result(created, prepared);
    }

    private DdcAiResourceResult getInTransaction(DdcAiIdQuery query) {
        DdcAiResourceBO resource = resourceRepository.findActiveForShareById(query.id())
                .orElseThrow(DdcAiResourceServiceImpl::notFound);
        scopeValidator.assertCanRead(resource);
        List<DdcAiDraftNodeBO> persistedNodes = draftNodeRepository.findActiveByResourceId(resource.getId());
        PreparedContent prepared = preparePersistedDraft(resource, persistedNodes);
        return result(resource, prepared);
    }

    private DdcPageResult<DdcAiResourceSummaryDTO> pageInTransaction(DdcAiPageQuery query) {
        DdcAiScopeDTO scope = query.toScope();
        Long namespaceId = scopeValidator.resolveNamespaceIdForRead(scope);
        PageQuery page = new PageQuery(query.getPageNo(), query.getPageSize());
        String prefix = query.getResourceKeyPrefix() == null || query.getResourceKeyPrefix().isEmpty()
                ? null : query.getResourceKeyPrefix();
        DdcPageResult<DdcAiResourceBO> rows = resourceRepository.search(scope.bizCode(), namespaceId,
                scope.env(), scope.appCode(), query.getKind(), prefix, page);
        return DdcPageResult.<DdcAiResourceSummaryDTO>builder()
                .records(resultConverter.toTargetList(rows.getRecords()))
                .total(rows.getTotal()).pageNo(rows.getPageNo()).pageSize(rows.getPageSize()).build();
    }

    private DdcAiResourceResult replaceDraftInTransaction(DdcAiIdQuery query, DdcAiDraftCommand command) {
        DdcAiResourceBO candidate = resourceRepository.findActiveById(query.id())
                .orElseThrow(DdcAiResourceServiceImpl::notFound);
        DdcAiResourceBO resource = lockWritableResource(candidate);
        requireExpectedRevision(command.getExpectedRevision(), resource.getVersion());
        PreparedContent prepared = strategy(resource.getKind()).prepare(command.getDraft());
        DdcAiResourceBO update = resourceConverter.toBusiness(command);
        resource.setDisplayName(update.getDisplayName().trim())
                .setDescription(update.getDescription())
                .setDraftPrompt(prepared.promptText());

        List<DdcAiDraftNodeBO> persistedNodes = draftNodeRepository.findActiveByResourceId(resource.getId());
        preparePersistedDraft(resource, persistedNodes);
        replaceDraftNodes(resource.getId(), persistedNodes, prepared);
        DdcAiResourceBO updated = resourceRepository.update(resource).orElseThrow(DdcAiResourceServiceImpl::revisionConflict);
        return result(updated, prepared);
    }

    private void deleteInTransaction(DdcAiIdQuery query, DdcAiDeleteCommand command) {
        Optional<DdcAiResourceBO> active = resourceRepository.findActiveById(query.id());
        if (active.isEmpty()) {
            Optional<DdcAiResourceBO> historical = resourceRepository.findIncludingDeletedById(query.id());
            if (historical.isPresent() && historical.get().getDeletedAt() != null) {
                scopeValidator.assertCanWrite(historical.get());
                return;
            }
            throw notFound();
        }
        DdcAiResourceBO resource = lockWritableResource(active.get());
        requireExpectedRevision(command.getExpectedRevision(), resource.getVersion());
        List<DdcAiDraftNodeBO> nodes = draftNodeRepository.findActiveByResourceId(resource.getId());
        for (DdcAiDraftNodeBO node : nodes) {
            if (!draftNodeRepository.delete(node)) throw revisionConflict();
        }
        if (!resourceRepository.delete(resource)) throw revisionConflict();
        optimizationService.ifAvailable(service -> service.onResourceDeleted(resource.getId()));
    }

    private DdcAiResourceBO lockWritableResource(DdcAiResourceBO candidate) {
        scopeValidator.assertCanWrite(candidate);
        DdcAiResourceBO locked = resourceRepository.findActiveForUpdateById(candidate.getId())
                .orElseThrow(DdcAiResourceServiceImpl::notFound);
        if (!Objects.equals(candidate.getTenantId(), locked.getTenantId())
                || !Objects.equals(candidate.getBizCode(), locked.getBizCode())
                || !Objects.equals(candidate.getNamespaceId(), locked.getNamespaceId())
                || !Objects.equals(candidate.getEnvCode(), locked.getEnvCode())
                || !Objects.equals(candidate.getAppCode(), locked.getAppCode())
                || !Objects.equals(candidate.getKind(), locked.getKind())
                || !Objects.equals(candidate.getResourceKey(), locked.getResourceKey())) {
            throw revisionConflict();
        }
        return locked;
    }

    private PreparedContent preparePersistedDraft(DdcAiResourceBO resource, List<DdcAiDraftNodeBO> persistedNodes) {
        List<DdcAiNodeDTO> nodes = persistedNodes.stream().map(draftNodeConverter::toDTO).toList();
        DdcAiContentDTO content = DdcAiContentDTO.builder()
                .promptText(resource.getDraftPrompt()).nodes(nodes).build();
        PreparedContent prepared;
        try {
            prepared = strategy(resource.getKind()).prepare(content);
        } catch (ConstraintViolationException | IllegalArgumentException failure) {
            throw contentIntegrityFailure(failure);
        }
        verifyPersistedNodes(persistedNodes, prepared);
        return prepared;
    }

    private void verifyPersistedNodes(List<DdcAiDraftNodeBO> persistedNodes, PreparedContent prepared) {
        Map<String, DdcAiDraftNodeBO> rowsByPath = new HashMap<>();
        persistedNodes.forEach(node -> rowsByPath.put(node.getPath(), node));
        List<DdcAiNodeDTO> nodes = prepared.nodes();
        if (nodes.size() != persistedNodes.size()) throw contentIntegrityFailure();
        for (DdcAiNodeDTO node : nodes) {
            DdcAiDraftNodeBO persisted = rowsByPath.remove(node.getPath());
            if (persisted == null || persisted.getNodeType() != node.getNodeType()
                    || !Objects.equals(persisted.getContent(), node.getContent())
                    || !Objects.equals(persisted.getParentPath(), parentPath(node.getPath()))) {
                throw contentIntegrityFailure();
            }
            long bytes = node.getContent() == null ? 0 : node.getContent().getBytes(StandardCharsets.UTF_8).length;
            String hash = prepared.fileSha256ByPath().get(node.getPath());
            if (persisted.getContentBytes() == null || persisted.getContentBytes() != bytes
                    || !Objects.equals(persisted.getContentSha256(), hash)) {
                throw contentIntegrityFailure();
            }
        }
        if (!rowsByPath.isEmpty()) throw contentIntegrityFailure();
    }

    private void replaceDraftNodes(Long resourceId, List<DdcAiDraftNodeBO> persistedNodes,
            PreparedContent prepared) {
        Map<String, DdcAiDraftNodeBO> oldByPath = new HashMap<>();
        persistedNodes.forEach(node -> oldByPath.put(node.getPath(), node));
        List<DdcAiNodeDTO> wantedNodes = prepared.nodes();
        Map<String, DdcAiNodeDTO> wantedByPath = new HashMap<>();
        wantedNodes.forEach(node -> wantedByPath.put(node.getPath(), node));

        for (DdcAiDraftNodeBO existing : persistedNodes) {
            DdcAiNodeDTO wanted = wantedByPath.get(existing.getPath());
            if (wanted == null || !sameNode(existing, wanted)) {
                if (!draftNodeRepository.delete(existing)) throw revisionConflict();
            }
        }
        for (DdcAiNodeDTO wanted : wantedNodes) {
            DdcAiDraftNodeBO old = oldByPath.get(wanted.getPath());
            if (old == null || !sameNode(old, wanted)) insertDraftNode(resourceId, wanted, prepared);
        }
    }

    private boolean sameNode(DdcAiDraftNodeBO existing, DdcAiNodeDTO wanted) {
        return existing.getNodeType() == wanted.getNodeType()
                && Objects.equals(existing.getContent(), wanted.getContent());
    }

    private void insertDraftNodes(Long resourceId, PreparedContent prepared) {
        for (DdcAiNodeDTO node : prepared.nodes()) insertDraftNode(resourceId, node, prepared);
    }

    private void insertDraftNode(Long resourceId, DdcAiNodeDTO node, PreparedContent prepared) {
        String path = node.getPath();
        boolean file = node.getNodeType() == DdcAiNodeTypeEnum.FILE;
        DdcAiDraftNodeBO persisted = draftNodeConverter.toBusiness(node)
                .setResourceId(resourceId)
                .setParentPath(parentPath(path))
                .setContentSha256(file ? prepared.fileSha256ByPath().get(path) : null)
                .setContentBytes(file ? (long) node.getContent().getBytes(StandardCharsets.UTF_8).length : 0L);
        if (draftNodeRepository.insert(persisted) == null) {
            throw new IllegalStateException("AI_DRAFT_NODE_INSERT_RETURNED_NO_ROW");
        }
    }

    private DdcAiResourceResult result(DdcAiResourceBO resource, PreparedContent prepared) {
        return DdcAiResourceResult.builder().resource(resultConverter.toTarget(resource))
                .draft(DdcAiContentDTO.builder().promptText(prepared.promptText()).nodes(prepared.nodes()).build())
                .build();
    }

    private <T> T executeWithLifecycleRetries(Supplier<T> operation) {
        for (int retry = 0; retry <= 3; retry++) {
            try {
                return commandTransactionTemplate.execute(status -> operation.get());
            } catch (DataIntegrityViolationException failure) {
                if (!isLifecycleTimestampConflict(failure)) throw failure;
                if (retry == 3) throw lifecycleTimestampConflict(failure);
            }
        }
        throw new IllegalStateException("AI_LIFECYCLE_RETRY_LOOP_TERMINATED_UNEXPECTEDLY");
    }

    private DdcAiContentStrategy strategy(DdcAiKindEnum kind) {
        DdcAiContentStrategy strategy = contentStrategyRegistry.get(kind);
        if (strategy == null || strategy.kind() != kind) {
            throw new IllegalStateException("AI_CONTENT_STRATEGY_NOT_REGISTERED:" + kind);
        }
        return strategy;
    }

    private String parentPath(String path) {
        if (path.isEmpty()) return null;
        int separator = path.lastIndexOf('/');
        return separator < 0 ? "" : path.substring(0, separator);
    }

    private void requireExpectedRevision(String expectedRevision, Long actualRevision) {
        long expected;
        try {
            expected = Long.parseLong(expectedRevision);
        } catch (NumberFormatException failure) {
            throw badRequest("Invalid resource revision");
        }
        if (expected < 0 || !Objects.equals(expected, actualRevision)) throw revisionConflict();
    }

    private void requireVersionCommandRevision(String expectedRevision, Long actualRevision) {
        Long expected;
        try {
            expected = resourceConverter.toInternalLong(expectedRevision);
        } catch (NumberFormatException failure) {
            throw badRequest("Invalid resource revision");
        }
        if (expected == null || expected < 0 || !Objects.equals(expected, actualRevision)) throw revisionConflict();
    }

    private boolean isResourceKeyConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && (message.contains("uk_ai_resource_live")
                    || message.contains("uk_ai_resource_lifecycle"))) return true;
        }
        return false;
    }

    private boolean isLifecycleTimestampConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && (message.contains("uk_ai_resource_lifecycle")
                    || message.contains("uk_ai_draft_node_lifecycle"))) return true;
        }
        return false;
    }

    private static CommonException notFound() {
        return new CommonException(AI_NOT_FOUND, AI_NOT_FOUND_STATUS, "Resource not found");
    }

    private static CommonException notPublished() {
        return new CommonException(AI_NOT_PUBLISHED, "TIANSHU_AI_NOT_PUBLISHED",
                "Resource has no published version");
    }

    private static CommonException kindMismatch() {
        return new CommonException(AI_KIND_MISMATCH, "TIANSHU_AI_KIND_MISMATCH",
                "Resource kind does not match the requested AI endpoint");
    }

    private static CommonException revisionConflict() {
        return new CommonException(AI_REVISION_CONFLICT, AI_REVISION_CONFLICT_STATUS, "Resource revision changed");
    }

    private static CommonException resourceKeyConflict() {
        return new CommonException(AI_RESOURCE_KEY_CONFLICT, AI_RESOURCE_KEY_CONFLICT_STATUS,
                "Resource key already exists in the requested scope");
    }

    private static CommonException resourceKeyConflict(Throwable cause) {
        return new CommonException(AI_RESOURCE_KEY_CONFLICT, AI_RESOURCE_KEY_CONFLICT_STATUS,
                "Resource key already exists in the requested scope", cause);
    }

    private static CommonException contentIntegrityFailure() {
        return new CommonException(AI_CONTENT_INTEGRITY, AI_CONTENT_INTEGRITY_STATUS,
                "AI content integrity check failed");
    }

    private static CommonException contentIntegrityFailure(Throwable cause) {
        return new CommonException(AI_CONTENT_INTEGRITY, AI_CONTENT_INTEGRITY_STATUS,
                "AI content integrity check failed", cause);
    }

    private static CommonException idempotencyConflict() {
        return new CommonException(AI_IDEMPOTENCY_CONFLICT, AI_IDEMPOTENCY_CONFLICT_STATUS,
                "Idempotency key was already used with a different request");
    }

    private static CommonException scopeForbidden() {
        return new CommonException(56307, "TIANSHU_AI_SCOPE_FORBIDDEN",
                "AI permission is required for the requested namespace");
    }

    private static CommonException versionCounterExhausted(Throwable cause) {
        return new CommonException(AI_VERSION_COUNTER_EXHAUSTED, AI_VERSION_COUNTER_EXHAUSTED_STATUS,
                "Resource version counter is exhausted", cause);
    }

    private static CommonException lifecycleTimestampConflict(Throwable cause) {
        return new CommonException(AI_LIFECYCLE_TIMESTAMP_CONFLICT, AI_LIFECYCLE_TIMESTAMP_CONFLICT_STATUS,
                "A lifecycle timestamp conflict remains after retrying the transaction", cause);
    }

    private static CommonException badRequest(String message) {
        return new CommonException(56301, "TIANSHU_AI_VALIDATION_ERROR", message);
    }
}
