package top.egon.cola.component.tianshu.admin.rpc.provider;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.tianshu.contract.DdcAiResourceRpc;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPromptResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPublicationReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiRevisionResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillDraftUpload;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillPublishRequest;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportBatch;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiVersionHead;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiResourceQuery;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillResult;
import top.egon.cola.component.rpc.tianshu.client.ai.DdcAiProtoConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionResult;
import top.egon.cola.component.tianshu.admin.security.rpc.DdcRpcProviderExceptionMapper;
import top.egon.cola.component.tianshu.admin.security.rpc.DdcServicePrincipal;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiResourceService;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiSyncCommandConverter;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcPromptResult;
import top.egon.cola.component.tianshu.model.ai.DdcSkillResult;

import java.util.regex.Pattern;

/** Serves immutable published Prompt and Skill snapshots through the read-only AI RPC contract. */
@Component("ddcAiResourceRpcProvider")
@EgonRpcProvider
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Validated
public class DdcAiResourceRpcProvider implements DdcAiResourceRpc {

    private static final Pattern POSITIVE_DECIMAL = Pattern.compile("[1-9][0-9]{0,18}");

    private static final int MAX_REPORT_BATCH = 100;

    @Qualifier("ddcAiResourceService")
    private final DdcAiResourceService resourceService;

    @Qualifier("ddcAiProtoConverterImpl")
    private final DdcAiProtoConverter protoConverter;

    @Qualifier("ddcAiFeedbackService")
    private final DdcAiFeedbackService feedbackService;

    @Qualifier("ddcAiSyncCommandConverterImpl")
    private final DdcAiSyncCommandConverter syncConverter;

    @Override
    public AiPromptResult getPrompt(@NotNull @Valid AiResourceQuery query) {
        return invoke("GetPrompt", () -> {
            requireServicePrincipal();
            DdcAiResourceQuery selector = selector(query);
            DdcAiVersionResult published = resourceService.getPublishedVersion(selector, DdcAiKindEnum.PROMPT);
            if (published.getVersion().getKind() == null
                    || !DdcAiKindEnum.PROMPT.name().equals(published.getVersion().getKind())
                    || published.getContent().getPromptText() == null) {
                throw contentIntegrityFailure();
            }
            DdcPromptResult response = DdcPromptResult.builder()
                    .resourceId(published.getVersion().getResourceId())
                    .resourceKey(selector.resourceKey())
                    .versionId(published.getVersion().getId())
                    .versionNo(published.getVersion().getVersionNo())
                    .contentSha256(published.getVersion().getContentSha256())
                    .template(published.getContent().getPromptText())
                    .build();
            return protoConverter.toProto(response);
        });
    }

    @Override
    public AiSkillResult getSkill(@NotNull @Valid AiResourceQuery query) {
        return invoke("GetSkill", () -> {
            requireServicePrincipal();
            DdcAiResourceQuery selector = selector(query);
            DdcAiVersionResult published = resourceService.getPublishedVersion(selector, DdcAiKindEnum.SKILL);
            if (published.getVersion().getKind() == null
                    || !DdcAiKindEnum.SKILL.name().equals(published.getVersion().getKind())
                    || published.getContent().getNodes() == null) {
                throw contentIntegrityFailure();
            }
            DdcSkillResult response = DdcSkillResult.builder()
                    .resourceId(published.getVersion().getResourceId())
                    .resourceKey(selector.resourceKey())
                    .versionId(published.getVersion().getId())
                    .versionNo(published.getVersion().getVersionNo())
                    .contentSha256(published.getVersion().getContentSha256())
                    .files(published.getContent().getNodes())
                    .build();
            return protoConverter.toProto(response);
        });
    }

    @Override
    public AiUsageReportReceipt reportUsage(@NotNull @Valid AiUsageReportBatch batch) {
        return invoke("ReportUsage", () -> {
            requireServicePrincipal();
            if (batch.getReportsCount() < 1 || batch.getReportsCount() > MAX_REPORT_BATCH) {
                throw new CommonException(56320, "TIANSHU_AI_FEEDBACK_INVALID",
                        "AI usage report batch must contain 1..100 reports");
            }
            return syncConverter.toProto(feedbackService.accept(syncConverter.toTarget(batch)));
        });
    }

    @Override
    public AiVersionHead getVersionHead(@NotNull @Valid AiResourceQuery query) {
        return invoke("GetVersionHead", () -> {
            requireServicePrincipal();
            if (query.hasVersionNo()) {
                throw new CommonException(56301, "TIANSHU_AI_VALIDATION_ERROR",
                        "A version head query must not set version_no");
            }
            return syncConverter.toProto(resourceService.getVersionHead(selector(query)));
        });
    }

    @Override
    public AiRevisionResult replaceSkillDraft(@NotNull @Valid AiSkillDraftUpload upload) {
        return invoke("ReplaceSkillDraft", () -> {
            requireServicePrincipal();
            return syncConverter.toProto(resourceService.replaceSkillDraft(syncConverter.toUploadCommand(upload)));
        });
    }

    @Override
    public AiPublicationReceipt publishSkillDraft(@NotNull @Valid AiSkillPublishRequest request) {
        return invoke("PublishSkillDraft", () -> {
            requireServicePrincipal();
            return syncConverter.toProto(resourceService.publishSkillDraft(syncConverter.toPublishCommand(request)));
        });
    }

    private DdcAiResourceQuery selector(AiResourceQuery query) {
        if (query == null || query.getNamespaceCode().isBlank() || query.getResourceKey().isBlank()
                || !query.hasResourceId()
                || (query.hasVersionNo() && query.getVersionNo() <= 0)) {
            throw new IllegalArgumentException("A root-bound AI read query is required");
        }
        return new DdcAiResourceQuery(query.getNamespaceCode(), query.getResourceKey(),
                query.hasVersionNo() ? query.getVersionNo() : null,
                positiveLong(query.getResourceId()));
    }

    private long positiveLong(String value) {
        if (value == null || !POSITIVE_DECIMAL.matcher(value).matches()) {
            throw new IllegalArgumentException("resourceId must be a positive decimal Long");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("resourceId exceeds Long range", invalid);
        }
    }

    private <T> T invoke(String method, Invocation<T> invocation) {
        try {
            return invocation.get();
        } catch (CommonException failure) {
            throw mapAiFailure(failure);
        } catch (ConstraintViolationException failure) {
            throw DdcRpcProviderExceptionMapper.status(Status.INVALID_ARGUMENT,
                    "TIANSHU_AI_VALIDATION_ERROR", "AI read query is invalid", false);
        } catch (IllegalArgumentException failure) {
            throw DdcRpcProviderExceptionMapper.status(Status.INVALID_ARGUMENT,
                    "TIANSHU_INVALID_REQUEST", "Tianshu request is invalid", false);
        } catch (TransientDataAccessException failure) {
            log.warn("Tianshu AI RPC temporarily unavailable method={} failureType={}",
                    method, failure.getClass().getSimpleName());
            throw DdcRpcProviderExceptionMapper.status(Status.UNAVAILABLE,
                    "TIANSHU_SERVICE_UNAVAILABLE", "Tianshu service is temporarily unavailable", true);
        } catch (RuntimeException failure) {
            log.error("Tianshu AI RPC failed method={} failureType={}",
                    method, failure.getClass().getSimpleName());
            throw DdcRpcProviderExceptionMapper.status(Status.INTERNAL,
                    "TIANSHU_INTERNAL_ERROR", "Tianshu operation failed", false);
        }
    }

    private StatusRuntimeException mapAiFailure(CommonException failure) {
        Status status = switch (failure.getCode()) {
            case 56301, 56308, 56320, 56321 -> Status.INVALID_ARGUMENT;
            case 56302, 56306, 56325 -> Status.NOT_FOUND;
            case 56303, 56314, 56316 -> Status.ABORTED;
            case 56304, 56305 -> Status.ALREADY_EXISTS;
            case 56307 -> Status.PERMISSION_DENIED;
            case 56309 -> Status.INTERNAL;
            case 56310, 56313, 56322 -> Status.UNAVAILABLE;
            case 56323, 56324 -> Status.FAILED_PRECONDITION;
            case 56311 -> Status.FAILED_PRECONDITION;
            case 56312 -> Status.RESOURCE_EXHAUSTED;
            default -> Status.INTERNAL;
        };
        return DdcRpcProviderExceptionMapper.status(status, failure.getStatus(),
                failure.getMessage(), failure.isRetryable());
    }

    private void requireServicePrincipal() {
        try {
            DdcServicePrincipal.current();
        } catch (IllegalStateException missingPrincipal) {
            throw new CommonException(56307, "TIANSHU_AI_SCOPE_FORBIDDEN",
                    "AI permission is required for the requested namespace");
        }
    }

    private CommonException contentIntegrityFailure() {
        return new CommonException(56309, "TIANSHU_AI_CONTENT_INTEGRITY",
                "AI content integrity check failed");
    }

    @FunctionalInterface
    private interface Invocation<T> {
        T get();
    }
}
