package top.egon.cola.component.tianshu.model.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.model.event.DdcAiPublicationEvent;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcAiSdkContractTest {

    private static final String SHA = "a".repeat(64);

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void usageReportSerializesLongsAsStringsAndInstantAsUtc() throws Exception {
        DdcAiUsageReportDTO report = report();

        JsonNode json = mapper.valueToTree(report);

        assertThat(json.get("resourceId").isTextual()).isTrue();
        assertThat(json.get("resourceId").asText()).isEqualTo("1");
        assertThat(json.get("versionId").asText()).isEqualTo("2");
        assertThat(json.get("versionNo").asText()).isEqualTo("4");
        assertThat(json.get("occurredAt").asText()).isEqualTo("2026-10-17T04:58:12.345Z");
        assertThat(json.get("outcome").asText()).isEqualTo("FAILURE");
        assertThat(json.has("explicitScore")).isTrue();
        assertThat(json.get("explicitScore").isNull()).isTrue();
        assertThat(mapper.treeToValue(json, DdcAiUsageReportDTO.class)).isEqualTo(report);
        assertThat(validator.validate(report)).isEmpty();
    }

    @Test
    void usageReportRejectsInvalidUsageIdScoreAndMissingInstant() {
        DdcAiUsageReportDTO invalid = report().toBuilder()
                .usageId("short")
                .explicitScore(101)
                .occurredAt(null)
                .build();

        assertThat(propertyPaths(validator.validate(invalid)))
                .containsExactlyInAnyOrder("usageId", "explicitScore", "occurredAt");
    }

    @Test
    void usageBatchRequiresOneToOneHundredValidReports() {
        assertThat(validator.validate(DdcAiUsageReportBatchDTO.builder().reports(List.of()).build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("reports");

        List<DdcAiUsageReportDTO> tooMany = new ArrayList<>();
        for (int index = 0; index < 101; index++) {
            tooMany.add(report());
        }
        assertThat(validator.validate(DdcAiUsageReportBatchDTO.builder().reports(tooMany).build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("reports");

        DdcAiUsageReportDTO nested = report().toBuilder().usageId("bad").build();
        assertThat(validator.validate(DdcAiUsageReportBatchDTO.builder().reports(List.of(nested)).build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("reports[0].usageId");
    }

    @Test
    void receiptRoundTripsWithNestedItems() throws Exception {
        DdcAiUsageReportReceiptDTO receipt = DdcAiUsageReportReceiptDTO.builder()
                .items(List.of(
                        DdcAiUsageReportReceiptDTO.ItemDTO.builder()
                                .usageId("01JAX0M7Q9K2R5T8V1W4X7Y0ZB")
                                .status(DdcAiUsageReportStatusEnum.ACCEPTED_DUPLICATE)
                                .build(),
                        DdcAiUsageReportReceiptDTO.ItemDTO.builder()
                                .usageId("01JAX0M7Q9K2R5T8V1W4X7Y0ZC")
                                .status(DdcAiUsageReportStatusEnum.REJECTED)
                                .reason("OCCURRED_AT_OUT_OF_WINDOW")
                                .build()))
                .build();

        String json = mapper.writeValueAsString(receipt);

        assertThat(json).contains("\"status\":\"ACCEPTED_DUPLICATE\"", "\"reason\":\"OCCURRED_AT_OUT_OF_WINDOW\"");
        assertThat(mapper.readValue(json, DdcAiUsageReportReceiptDTO.class)).isEqualTo(receipt);
        assertThat(validator.validate(receipt)).isEmpty();
    }

    @Test
    void syncContractsSerializeLongsAsStringsAndValidateDigests() throws Exception {
        DdcAiVersionHeadDTO head = DdcAiVersionHeadDTO.builder()
                .resourceId(7L).resourceKey("refund").versionId(9L).versionNo(3L)
                .contentSha256(SHA).resourceRevision(11L).published(true).build();
        DdcAiVersionHeadDTO unpublished = DdcAiVersionHeadDTO.builder()
                .resourceId(7L).resourceKey("refund").resourceRevision(0L).published(false).build();
        DdcAiRevisionDTO revision = DdcAiRevisionDTO.builder()
                .resourceId(7L).resourceRevision(12L).draftSha256(SHA).draftMatchesCurrent(false).build();
        DdcAiPublicationReceiptDTO receipt = DdcAiPublicationReceiptDTO.builder()
                .publicationId(555L).resourceId(7L).targetVersionId(10L).targetVersionNo(4L)
                .resourceRevision(13L).contentSha256(SHA).build();

        assertThat(mapper.valueToTree(head).get("resourceRevision").asText()).isEqualTo("11");
        assertThat(mapper.valueToTree(unpublished).get("versionNo").isNull()).isTrue();
        assertThat(mapper.readValue(mapper.writeValueAsString(revision), DdcAiRevisionDTO.class)).isEqualTo(revision);
        assertThat(mapper.readValue(mapper.writeValueAsString(receipt), DdcAiPublicationReceiptDTO.class))
                .isEqualTo(receipt);
        assertThat(validator.validate(head)).isEmpty();
        assertThat(validator.validate(unpublished)).isEmpty();
        assertThat(validator.validate(revision)).isEmpty();
        assertThat(validator.validate(receipt)).isEmpty();
        assertThat(propertyPaths(validator.validate(head.toBuilder().contentSha256("ABC").build())))
                .containsExactly("contentSha256");
    }

    @Test
    void uploadAndPublishContractsRejectInvalidInputs() {
        DdcAiSkillDraftUploadDTO upload = DdcAiSkillDraftUploadDTO.builder()
                .namespaceCode("default").resourceKey("refund-skill").resourceId(7L).expectedRevision(3L)
                .displayName("Refund").description("")
                .files(List.of(DdcAiNodeDTO.builder().path("SKILL.md").nodeType(DdcAiNodeTypeEnum.FILE)
                        .content("# refund").build()))
                .build();
        DdcAiSkillPublishDTO publish = DdcAiSkillPublishDTO.builder()
                .namespaceCode("default").resourceKey("refund-skill").resourceId(7L).expectedRevision(4L)
                .requestId("auth-0123456789abcdef").changeNote("from authoring").build();

        assertThat(validator.validate(upload)).isEmpty();
        assertThat(validator.validate(publish)).isEmpty();
        assertThat(propertyPaths(validator.validate(upload.toBuilder().files(List.of()).expectedRevision(-1L).build())))
                .containsExactlyInAnyOrder("files", "expectedRevision");
        assertThat(propertyPaths(validator.validate(publish.toBuilder().requestId("short").changeNote(null).build())))
                .containsExactlyInAnyOrder("requestId", "changeNote");
    }

    @Test
    void publicationEventRoundTripsWithinTenantEnvelope() throws Exception {
        DdcAiPublicationEvent event = DdcAiPublicationEvent.builder()
                .eventId("ai-pub-555").tenantId(42L).resourceId(7L).kind("SKILL")
                .bizCode("xingyuan").namespaceCode("default").env("dev").appCode("order-service")
                .resourceKey("refund-skill").operation("PUBLISH").versionId(10L).versionNo(4L)
                .contentSha256(SHA).resourceRevision(13L).actor("system:ai-optimizer")
                .occurredAt(Instant.parse("2026-10-17T05:00:00Z"))
                .build();
        DdcTenantMessageDTO<DdcAiPublicationEvent> envelope = new DdcTenantMessageDTO<>(42L, event);

        String json = mapper.writeValueAsString(envelope);
        DdcTenantMessageDTO<DdcAiPublicationEvent> restored =
                mapper.readValue(json, new TypeReference<DdcTenantMessageDTO<DdcAiPublicationEvent>>() { });

        assertThat(json).contains("\"tenantId\":\"42\"", "\"versionNo\":\"4\"", "\"occurredAt\":\"2026-10-17T05:00:00Z\"");
        assertThat(restored.getPayload()).isEqualTo(event);
        assertThat(restored.belongsToTenant(42L)).isTrue();
        assertThat(restored.getPayload().hasValidIdentity()).isTrue();
        assertThat(validator.validate(restored)).isEmpty();
        assertThat(propertyPaths(validator.validate(event.toBuilder().eventId("pub-1").operation("DELETE").build())))
                .containsExactlyInAnyOrder("eventId", "operation");
        assertThat(event.toBuilder().tenantId(null).build().hasValidIdentity()).isFalse();
    }

    @Test
    void enumsExposeStableJsonValues() throws Exception {
        assertThat(mapper.writeValueAsString(DdcAiOutcomeEnum.FAILURE)).isEqualTo("\"FAILURE\"");
        assertThat(mapper.readValue("\"UNKNOWN\"", DdcAiOutcomeEnum.class)).isEqualTo(DdcAiOutcomeEnum.UNKNOWN);
        assertThat(mapper.writeValueAsString(DdcAiUsageReportStatusEnum.ACCEPTED_DUPLICATE))
                .isEqualTo("\"ACCEPTED_DUPLICATE\"");
        assertThatThrownBy(() -> DdcAiOutcomeEnum.fromCode("0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DdcAiUsageReportStatusEnum.fromCode("accepted"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static DdcAiUsageReportDTO report() {
        return DdcAiUsageReportDTO.builder()
                .usageId("01JAX0M7Q9K2R5T8V1W4X7Y0ZB")
                .namespaceCode("default")
                .resourceKey("refund")
                .resourceId(1L)
                .versionId(2L)
                .versionNo(4L)
                .outcome(DdcAiOutcomeEnum.FAILURE)
                .occurredAt(Instant.parse("2026-10-17T04:58:12.345Z"))
                .build();
    }

    private static <T> Set<String> propertyPaths(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
