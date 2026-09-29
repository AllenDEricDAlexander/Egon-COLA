package top.egon.cola.component.tianshu.admin.service.ai.impl;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiDraftNodeBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiCreateCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDeleteCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDraftCommand;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiDraftNodeConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceResultConverter;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiResourceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiDraftNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiResourceService;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiContentStrategy.PreparedContent;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service("ddcAiResourceService")
@RequiredArgsConstructor
@Validated
public class DdcAiResourceServiceImpl implements DdcAiResourceService {

    private static final int AI_NOT_FOUND = 56302;
    private static final int AI_REVISION_CONFLICT = 56303;
    private static final int AI_RESOURCE_KEY_CONFLICT = 56304;
    private static final int AI_CONTENT_INTEGRITY = 56309;
    private static final int AI_LIFECYCLE_TIMESTAMP_CONFLICT = 56316;
    private static final String AI_NOT_FOUND_STATUS = "TIANSHU_AI_NOT_FOUND";
    private static final String AI_REVISION_CONFLICT_STATUS = "TIANSHU_AI_REVISION_CONFLICT";
    private static final String AI_RESOURCE_KEY_CONFLICT_STATUS = "TIANSHU_AI_DUPLICATE_RESOURCE_KEY";
    private static final String AI_CONTENT_INTEGRITY_STATUS = "TIANSHU_AI_CONTENT_INTEGRITY";
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

    @Qualifier("ddcAiResourceResultConverterImpl")
    private final DdcAiResourceResultConverter resultConverter;

    @Qualifier("ddcAiContentStrategyRegistry")
    private final Map<DdcAiKindEnum, DdcAiContentStrategy> contentStrategyRegistry;

    @Qualifier("ddcAiReadTransactionTemplate")
    private final TransactionTemplate readTransactionTemplate;

    @Qualifier("ddcAiCommandTransactionTemplate")
    private final TransactionTemplate commandTransactionTemplate;

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
        return executeWithLifecycleRetries(() -> replaceDraftInTransaction(query, command));
    }

    @Override
    public void delete(DdcAiIdQuery query, DdcAiDeleteCommand command) {
        executeWithLifecycleRetries(() -> {
            deleteInTransaction(query, command);
            return null;
        });
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

    private static CommonException lifecycleTimestampConflict(Throwable cause) {
        return new CommonException(AI_LIFECYCLE_TIMESTAMP_CONFLICT, AI_LIFECYCLE_TIMESTAMP_CONFLICT_STATUS,
                "A lifecycle timestamp conflict remains after retrying the transaction", cause);
    }

    private static CommonException badRequest(String message) {
        return new CommonException(56301, "TIANSHU_AI_VALIDATION_ERROR", message);
    }
}
