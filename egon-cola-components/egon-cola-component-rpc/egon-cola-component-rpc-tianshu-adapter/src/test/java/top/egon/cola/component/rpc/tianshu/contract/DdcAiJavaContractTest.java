package top.egon.cola.component.rpc.tianshu.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiValidationGroups;
import top.egon.cola.component.tianshu.model.ai.DdcPromptResult;
import top.egon.cola.component.tianshu.model.ai.DdcSkillResult;
import top.egon.cola.component.tianshu.model.ai.validation.PublishableAiContent;
import top.egon.cola.component.tianshu.model.ai.validation.ValidAiContent;
import top.egon.cola.component.tianshu.model.ai.validation.ValidAiNode;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiResourceQuery;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DdcAiJavaContractTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void keepsMutableTransportObjectsAsClassesAndScopeAsAnImmutableValue() {
        assertThat(DdcAiScopeDTO.class.isRecord()).isTrue();
        assertThat(DdcAiNodeDTO.class.isRecord()).isFalse();
        assertThat(DdcAiContentDTO.class.isRecord()).isFalse();
        assertThat(DdcPromptResult.class.isRecord()).isFalse();
        assertThat(DdcSkillResult.class.isRecord()).isFalse();
        assertThat(new DdcAiScopeDTO(" biz ", " namespace ", " dev ", " app "))
                .extracting(DdcAiScopeDTO::bizCode, DdcAiScopeDTO::namespaceCode,
                        DdcAiScopeDTO::env, DdcAiScopeDTO::appCode)
                .containsExactly("biz", "namespace", "dev", "app");
        assertThat(validator.validate(new DdcAiScopeDTO(" ", "namespace", "dev", "app")))
                .isNotEmpty();
    }

    @Test
    void validatesNodeShapeAndTextLimitsWithoutNormalizingContent() {
        DdcAiNodeDTO invalidDirectory = node("assets", DdcAiNodeTypeEnum.DIRECTORY, "not-null");
        assertViolation(invalidDirectory, ValidAiNode.class);

        DdcAiNodeDTO invalidPath = node("../secret", DdcAiNodeTypeEnum.FILE, "text");
        assertViolation(invalidPath, ValidAiNode.class);

        DdcAiNodeDTO invalidUnicode = node("file.txt", DdcAiNodeTypeEnum.FILE, "\u0000");
        assertViolation(invalidUnicode, ValidAiNode.class);
        assertViolation(node("file.txt", DdcAiNodeTypeEnum.FILE, "\uD800"), ValidAiNode.class);

        String oversizedFile = "x".repeat(262_145);
        DdcAiNodeDTO tooLarge = node("file.txt", DdcAiNodeTypeEnum.FILE, oversizedFile);
        assertViolation(tooLarge, ValidAiNode.class);

        DdcAiNodeDTO crlf = node("file.txt", DdcAiNodeTypeEnum.FILE, "line1\r\nline2");
        assertThat(validator.validate(crlf)).isEmpty();
        assertThat(crlf.getContent()).isEqualTo("line1\r\nline2");
    }

    @Test
    void validatesTreeShapeAndPublishabilityByGroups() {
        DdcAiContentDTO mixedPromptAndTree = DdcAiContentDTO.builder()
                .promptText("prompt")
                .nodes(skillTree(node("SKILL.md", DdcAiNodeTypeEnum.FILE, "body")))
                .build();
        assertViolation(mixedPromptAndTree, ValidAiContent.class, DdcAiValidationGroups.Save.class);

        DdcAiContentDTO missingParent = DdcAiContentDTO.builder()
                .nodes(List.of(
                        node("", DdcAiNodeTypeEnum.DIRECTORY, null),
                        node("docs/readme.txt", DdcAiNodeTypeEnum.FILE, "body")))
                .build();
        assertViolation(missingParent, ValidAiContent.class, DdcAiValidationGroups.Save.class);

        DdcAiContentDTO duplicatePath = DdcAiContentDTO.builder()
                .nodes(List.of(
                        node("", DdcAiNodeTypeEnum.DIRECTORY, null),
                        node("README.md", DdcAiNodeTypeEnum.FILE, "first"),
                        node("README.md", DdcAiNodeTypeEnum.FILE, "second")))
                .build();
        assertViolation(duplicatePath, ValidAiContent.class, DdcAiValidationGroups.Save.class);

        List<DdcAiNodeDTO> tooManyNodes = new java.util.ArrayList<>();
        tooManyNodes.add(node("", DdcAiNodeTypeEnum.DIRECTORY, null));
        for (int index = 0; index < 257; index++) {
            tooManyNodes.add(node("file" + index, DdcAiNodeTypeEnum.FILE, ""));
        }
        assertViolation(
                DdcAiContentDTO.builder().nodes(tooManyNodes).build(),
                ValidAiContent.class,
                DdcAiValidationGroups.Save.class);

        String largestFile = "x".repeat(262_144);
        DdcAiContentDTO oversizedSkill = DdcAiContentDTO.builder()
                .nodes(skillTree(
                        node("one.txt", DdcAiNodeTypeEnum.FILE, largestFile),
                        node("two.txt", DdcAiNodeTypeEnum.FILE, largestFile),
                        node("three.txt", DdcAiNodeTypeEnum.FILE, largestFile),
                        node("four.txt", DdcAiNodeTypeEnum.FILE, largestFile),
                        node("five.txt", DdcAiNodeTypeEnum.FILE, largestFile)))
                .build();
        assertViolation(oversizedSkill, ValidAiContent.class, DdcAiValidationGroups.Save.class);

        DdcAiContentDTO blankPrompt = DdcAiContentDTO.builder().promptText(" \n ").build();
        assertViolation(blankPrompt, PublishableAiContent.class, DdcAiValidationGroups.Publish.class);

        DdcAiContentDTO directoryOnly = DdcAiContentDTO.builder()
                .nodes(skillTree(node("docs", DdcAiNodeTypeEnum.DIRECTORY, null)))
                .build();
        assertViolation(directoryOnly, PublishableAiContent.class, DdcAiValidationGroups.Publish.class);

        DdcAiContentDTO emptyFileIsPublishable = DdcAiContentDTO.builder()
                .nodes(skillTree(node("SKILL.md", DdcAiNodeTypeEnum.FILE, "")))
                .build();
        assertThat(validator.validate(emptyFileIsPublishable, DdcAiValidationGroups.Publish.class))
                .isEmpty();
    }

    @Test
    void manualAndSpringProxyValidationUseTheSameConstraintContract() {
        DdcAiContentDTO invalid = new DdcAiContentDTO();
        ValidationUtils validationUtils = new ValidationUtils(validator);
        Set<ConstraintViolation<DdcAiContentDTO>> manualViolations =
                validationUtils.violations(invalid, DdcAiValidationGroups.Save.class);

        ProxyFactory proxyFactory = new ProxyFactory(new ValidatedBoundary());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new MethodValidationInterceptor(validator));
        ValidatedBoundary proxy = (ValidatedBoundary) proxyFactory.getProxy();

        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class, () -> proxy.save(invalid));
        assertThat(annotationTypes(manualViolations)).contains(ValidAiContent.class);
        assertThat(annotationTypes(exception.getConstraintViolations()))
                .containsExactlyInAnyOrderElementsOf(annotationTypes(manualViolations));
    }

    @Test
    void exposesStableJsonEnumsAndStringIdsWithoutChangingTheResultShape() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        assertThat(objectMapper.writeValueAsString(DdcAiKindEnum.PROMPT)).isEqualTo("\"PROMPT\"");
        assertThat(objectMapper.readValue("\"SKILL\"", DdcAiKindEnum.class))
                .isEqualTo(DdcAiKindEnum.SKILL);
        assertThat(objectMapper.writeValueAsString(DdcAiNodeTypeEnum.FILE)).isEqualTo("\"FILE\"");
        assertThatThrownBy(() -> objectMapper.readValue("\"UNKNOWN\"", DdcAiKindEnum.class))
                .isInstanceOf(JsonProcessingException.class);
        assertThatThrownBy(() -> objectMapper.readValue("\"UNKNOWN\"", DdcAiNodeTypeEnum.class))
                .isInstanceOf(JsonProcessingException.class);

        DdcPromptResult prompt = DdcPromptResult.builder()
                .resourceId(41L)
                .resourceKey("review-order")
                .versionId(87L)
                .versionNo(3L)
                .contentSha256("a".repeat(64))
                .template("retain\r\ntext")
                .build();
        JsonNode promptJson = objectMapper.valueToTree(prompt);
        assertThat(promptJson.get("resourceId").asText()).isEqualTo("41");
        assertThat(promptJson.get("resourceKey").asText()).isEqualTo("review-order");
        assertThat(promptJson.get("versionId").asText()).isEqualTo("87");
        assertThat(promptJson.get("versionNo").asText()).isEqualTo("3");
        assertThat(promptJson.get("template").asText()).isEqualTo("retain\r\ntext");

        DdcSkillResult skill = DdcSkillResult.builder()
                .resourceId(42L)
                .resourceKey("fulfillment")
                .versionId(88L)
                .versionNo(4L)
                .contentSha256("b".repeat(64))
                .files(skillTree(node("empty.txt", DdcAiNodeTypeEnum.FILE, "")))
                .build();
        JsonNode skillJson = objectMapper.valueToTree(skill);
        assertThat(skillJson.get("resourceId").asText()).isEqualTo("42");
        assertThat(skillJson.get("files").get(1).get("nodeType").asText()).isEqualTo("FILE");
        assertThat(skillJson.get("files").get(1).get("content").asText()).isEmpty();
    }

    @Test
    void appendsResourceIdentityWithoutRenumberingExistingProtoFields() {
        assertThat(AiResourceQuery.getDescriptor().findFieldByNumber(1).getName())
                .isEqualTo("biz_code");
        assertThat(AiResourceQuery.getDescriptor().findFieldByNumber(6).getName())
                .isEqualTo("version_no");
        assertThat(AiResourceQuery.getDescriptor().findFieldByNumber(7).getName())
                .isEqualTo("resource_id");
        assertThat(AiResourceQuery.getDescriptor().getFields())
                .extracting(field -> field.getName())
                .containsExactly("biz_code", "namespace_code", "env", "app_code", "resource_key",
                        "version_no", "resource_id");
        assertThat(top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPromptResult.getDescriptor()
                        .getFields())
                .extracting(field -> field.getName())
                .containsExactly("resource_id", "resource_key", "version_id", "version_no",
                        "content_sha256", "template");
        assertThat(top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillResult.getDescriptor()
                        .getFields())
                .extracting(field -> field.getName())
                .containsExactly("resource_id", "resource_key", "version_id", "version_no",
                        "content_sha256", "files");
        assertThat(AiResourceQuery.newBuilder().setResourceId("41").build().hasResourceId())
                .isTrue();
        assertThat(AiResourceQuery.newBuilder().setVersionNo(0L).build().hasVersionNo())
                .isTrue();
        assertThat(top.egon.cola.component.rpc.tianshu.contract.proto.v1.DdcAiResourceServiceGrpc
                        .getServiceDescriptor().getMethods())
                .extracting(method -> method.getBareMethodName())
                .containsExactly("GetPrompt", "GetSkill");
    }

    private static DdcAiNodeDTO node(String path, DdcAiNodeTypeEnum nodeType, String content) {
        return DdcAiNodeDTO.builder().path(path).nodeType(nodeType).content(content).build();
    }

    @SafeVarargs
    private static List<DdcAiNodeDTO> skillTree(DdcAiNodeDTO... children) {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(node("", DdcAiNodeTypeEnum.DIRECTORY, null)),
                        java.util.Arrays.stream(children))
                .toList();
    }

    private static void assertViolation(Object bean, Class<?> annotationType, Class<?>... groups) {
        assertThat(validator.validate(bean, groups))
                .anyMatch(violation -> violation.getConstraintDescriptor().getAnnotation()
                        .annotationType().equals(annotationType));
    }

    private static Set<Class<?>> annotationTypes(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getConstraintDescriptor().getAnnotation().annotationType())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Validated(DdcAiValidationGroups.Save.class)
    static class ValidatedBoundary {
        public void save(@jakarta.validation.Valid DdcAiContentDTO content) {
        }
    }
}
