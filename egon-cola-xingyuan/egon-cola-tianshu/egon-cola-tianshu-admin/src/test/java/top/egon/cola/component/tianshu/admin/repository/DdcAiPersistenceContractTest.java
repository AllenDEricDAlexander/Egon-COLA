package top.egon.cola.component.tianshu.admin.repository;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiDraftNodeBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiPublicationBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionNodeBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiDraftNodeConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiEnumConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiPublicationConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceResultConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiVersionConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiVersionNodeConverter;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiKindPersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiNodePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPublicationOperationEnum;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiDraftNodePO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiPublicationPO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiResourcePO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiVersionNodePO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiVersionPO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcAiPersistenceContractTest {

    private static final String BASE = "top.egon.cola.component.tianshu.admin.";
    private static final String AI_BASE = "top.egon.cola.component.tianshu.model.ai.";
    private static final String AI_DDL = "db/egon-mp/V20260922_001__tianshu_platform.sql";
    private static final String PERSISTENCE_YAML = "egon-tianshu-persistence.yml";
    private static final Set<String> EGON_MODEL_COLUMNS = Set.of(
            "id", "tenant_id", "create_user_id", "create_time", "update_user_id", "update_time", "deleted_at", "version");
    private static final List<TableContract> TABLES = List.of(
            new TableContract("DdcAiResource", "ddc_ai_resource", true),
            new TableContract("DdcAiVersion", "ddc_ai_version", false),
            new TableContract("DdcAiDraftNode", "ddc_ai_draft_node", true),
            new TableContract("DdcAiVersionNode", "ddc_ai_version_node", false),
            new TableContract("DdcAiPublication", "ddc_ai_publication", false));

    @Test
    void aiRowsMatchTheFiveV001TablesAndKeepTechnicalIdentityInternal() throws Exception {
        String ddl = resource(AI_DDL);
        for (TableContract table : TABLES) {
            Class<?> po = requiredType("model.po." + table.modelName() + "PO");
            Class<?> bo = requiredType("model.bo." + table.modelName() + "BO");
            Class<?> converter = requiredType("model.convertor." + table.modelName() + "Converter");
            Class<?> mapper = requiredType("dao." + table.modelName() + "Mapper");
            Class<?> repository = requiredType("repository." + table.modelName() + "Repository");
            Class<?> repositoryImpl = requiredType("repository.impl." + table.modelName() + "RepositoryImpl");

            assertThat(po.getSuperclass()).isEqualTo(EgonModel.class);
            assertThat(po.getGenericSuperclass()).isInstanceOf(ParameterizedType.class);
            assertThat(((ParameterizedType) po.getGenericSuperclass()).getActualTypeArguments()[0]).isEqualTo(po);
            assertThat(po.getAnnotation(TableName.class).value()).isEqualTo(table.tableName());
            assertThat(EgonModel.class.isAssignableFrom(bo)).isFalse();
            assertThat(mapper).isInterface().isAssignableTo(EgonColaMapper.class);
            assertThat(repositoryImpl).isAssignableTo(repository).isAssignableTo(EgonColaRepository.class);
            assertThat(converter).isInterface().isAssignableTo(BaseConverter.class);
            assertThat(requiredType(converter.getName() + "Impl")).isAssignableTo(converter);

            Set<String> ddlColumns = tableColumns(ddl, table.tableName());
            Set<String> businessColumns = new LinkedHashSet<>(ddlColumns);
            businessColumns.removeAll(EGON_MODEL_COLUMNS);
            Set<String> poColumns = Arrays.stream(po.getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()) && !field.isSynthetic())
                    .filter(this::isPersistedField)
                    .map(this::databaseColumn)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            assertThat(ddlColumns).doesNotContain("public_id");
            assertThat(poColumns).containsExactlyInAnyOrderElementsOf(businessColumns);

            Set<String> boFields = declaredFieldNames(bo);
            assertThat(boFields).containsAll(poFieldNames(po))
                    .contains("id", "tenantId", "createUserId", "createTime", "updateUserId", "updateTime", "deletedAt", "version");
            assertThat(poFieldNames(po)).doesNotContain("publicId");
            assertThat(boFields).doesNotContain("publicId");
            assertThat(fieldType(bo, "id")).isEqualTo(Long.class);

            assertGeneratedPojoContract(po);
            assertGeneratedPojoContract(bo);
            assertThat(repositoryImpl.getAnnotation(Repository.class)).isNotNull();
        }

        assertFieldTypes("model.po.DdcAiResourcePO", Map.of("kind", "model.enums.DdcAiKindPersistenceEnum"));
        assertFieldTypes("model.po.DdcAiVersionPO", Map.of("kind", "model.enums.DdcAiKindPersistenceEnum"));
        assertFieldTypes("model.po.DdcAiDraftNodePO", Map.of("nodeType", "model.enums.DdcAiNodePersistenceEnum"));
        assertFieldTypes("model.po.DdcAiVersionNodePO", Map.of("nodeType", "model.enums.DdcAiNodePersistenceEnum"));
        assertFieldTypes("model.po.DdcAiPublicationPO", Map.of("operation", "model.enums.DdcAiPublicationOperationEnum"));
        assertFieldTypes("model.bo.DdcAiResourceBO", Map.of("kind", AI_BASE + "DdcAiKindEnum"));
        assertFieldTypes("model.bo.DdcAiVersionBO", Map.of("kind", AI_BASE + "DdcAiKindEnum"));
        assertFieldTypes("model.bo.DdcAiDraftNodeBO", Map.of("nodeType", AI_BASE + "DdcAiNodeTypeEnum"));
        assertFieldTypes("model.bo.DdcAiVersionNodeBO", Map.of("nodeType", AI_BASE + "DdcAiNodeTypeEnum"));
    }

    @Test
    void eachRepositoryAndMapperQueryValidatesPositiveIdsInTheQueryGroup() throws Exception {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ExecutableValidator validator = factory.getValidator().forExecutables();
            for (TableContract table : TABLES) {
                Class<?> repository = requiredType("repository." + table.modelName() + "Repository");
                Object repositoryProxy = interfaceProxy(repository);
                Method findActiveById = repository.getMethod("findActiveById", Long.class);
                assertThat(validator.validateParameters(repositoryProxy, findActiveById, new Object[] {0L},
                        EgonColaModelValidationGroups.Query.class)).isNotEmpty();
                assertThat(validator.validateParameters(repositoryProxy, findActiveById, new Object[] {42L},
                        EgonColaModelValidationGroups.Query.class)).isEmpty();

                Class<?> mapper = requiredType("dao." + table.modelName() + "Mapper");
                Object mapperProxy = interfaceProxy(mapper);
                Method selectActiveById = mapper.getMethod("selectActiveById", Long.class, Long.class);
                assertThat(validator.validateParameters(mapperProxy, selectActiveById, new Object[] {41L, 0L},
                        EgonColaModelValidationGroups.Query.class)).isNotEmpty();
                assertThat(validator.validateParameters(mapperProxy, selectActiveById, new Object[] {41L, 42L},
                        EgonColaModelValidationGroups.Query.class)).isEmpty();
            }

            DdcAiDraftNodePO validDraftRoot = DdcAiDraftNodePO.builder().resourceId(91L).parentPath(null).path("")
                    .nodeType(DdcAiNodePersistenceEnum.DIRECTORY).content(null).contentSha256(null).contentBytes(0L).build();
            assertThat(factory.getValidator().validate(validDraftRoot, EgonColaModelValidationGroups.Insert.class)).isEmpty();

            DdcAiDraftNodePO oversizedUtf8Path = DdcAiDraftNodePO.builder().resourceId(91L).parentPath("")
                    .path("é".repeat(513)).nodeType(DdcAiNodePersistenceEnum.FILE).content("")
                    .contentSha256("a".repeat(64)).contentBytes(0L).build();
            assertThat(factory.getValidator().validate(oversizedUtf8Path, EgonColaModelValidationGroups.Insert.class))
                    .isNotEmpty();

            DdcAiVersionNodePO invalidSnapshotNode = DdcAiVersionNodePO.builder().versionId(92L)
                    .parentPath("").path("README.md").nodeType(DdcAiNodePersistenceEnum.FILE)
                    .content("é").contentSha256("b".repeat(64)).contentBytes(1L).build();
            assertThat(factory.getValidator().validate(invalidSnapshotNode, EgonColaModelValidationGroups.Insert.class))
                    .isNotEmpty();

            DdcAiResourcePO validPrompt = DdcAiResourcePO.builder().bizCode("biz").namespaceId(51L).envCode("dev")
                    .appCode("agent").kind(DdcAiKindPersistenceEnum.PROMPT).resourceKey("prompt")
                    .displayName("Prompt").description("").draftPrompt("").lastVersionNo(0L).build();
            assertThat(factory.getValidator().validate(validPrompt, EgonColaModelValidationGroups.Insert.class)).isEmpty();
            DdcAiResourcePO promptWithoutDraft = DdcAiResourcePO.builder().bizCode("biz").namespaceId(51L).envCode("dev")
                    .appCode("agent").kind(DdcAiKindPersistenceEnum.PROMPT).resourceKey("prompt")
                    .displayName("Prompt").description("").draftPrompt(null).lastVersionNo(0L).build();
            assertThat(factory.getValidator().validate(promptWithoutDraft, EgonColaModelValidationGroups.Insert.class))
                    .isNotEmpty();

            DdcAiVersionPO validPromptVersion = DdcAiVersionPO.builder().resourceId(91L).versionNo(1L)
                    .kind(DdcAiKindPersistenceEnum.PROMPT).displayName("Prompt v1").description("")
                    .promptText("").contentSha256("c".repeat(64)).contentBytes(0L).nodeCount(0).changeNote("").build();
            assertThat(factory.getValidator().validate(validPromptVersion, EgonColaModelValidationGroups.Insert.class))
                    .isEmpty();
            DdcAiVersionPO promptVersionWithoutContent = DdcAiVersionPO.builder().resourceId(91L).versionNo(1L)
                    .kind(DdcAiKindPersistenceEnum.PROMPT).displayName("Prompt v1").description("")
                    .promptText(null).contentSha256("c".repeat(64)).contentBytes(0L).nodeCount(0).changeNote("").build();
            assertThat(factory.getValidator().validate(promptVersionWithoutContent,
                    EgonColaModelValidationGroups.Insert.class)).isNotEmpty();

            DdcAiPublicationPO validPromotion = DdcAiPublicationPO.builder().resourceId(91L)
                    .operation(DdcAiPublicationOperationEnum.PROMOTE).requestId("request-000000001")
                    .requestSha256("d".repeat(64)).targetVersionId(92L).sourceVersionId(93L)
                    .resultRevision(0L).targetCreated(false).build();
            assertThat(factory.getValidator().validate(validPromotion, EgonColaModelValidationGroups.Insert.class))
                    .isEmpty();
            DdcAiPublicationPO promotionWithoutSource = DdcAiPublicationPO.builder().resourceId(91L)
                    .operation(DdcAiPublicationOperationEnum.PROMOTE).requestId("request-000000001")
                    .requestSha256("d".repeat(64)).targetVersionId(92L).sourceVersionId(null)
                    .resultRevision(0L).targetCreated(false).build();
            assertThat(factory.getValidator().validate(promotionWithoutSource,
                    EgonColaModelValidationGroups.Insert.class)).isNotEmpty();
        }
    }

    @Test
    void repositoryBeansKeepNamedQualifiedInjectionAndSafeSlf4jLoggers() throws Exception {
        String propertiesBean = "egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties";
        for (TableContract table : TABLES) {
            Class<?> implementation = requiredType("repository.impl." + table.modelName() + "RepositoryImpl");
            Repository repository = implementation.getAnnotation(Repository.class);
            assertThat(repository).isNotNull();
            assertThat(repository.value()).isEqualTo(beanPrefix(table.modelName()) + "Repository");

            Field logger = implementation.getDeclaredField("log");
            assertThat(logger.getType()).isEqualTo(Logger.class);
            assertThat(Modifier.isStatic(logger.getModifiers())).isTrue();
            assertThat(Modifier.isFinal(logger.getModifiers())).isTrue();

            Set<String> fieldQualifiers = Arrays.stream(implementation.getDeclaredFields())
                    .filter(field -> Modifier.isFinal(field.getModifiers()))
                    .map(field -> field.getAnnotation(Qualifier.class))
                    .filter(java.util.Objects::nonNull)
                    .map(Qualifier::value)
                    .collect(Collectors.toSet());
            assertThat(fieldQualifiers).contains(
                    beanPrefix(table.modelName()) + "Mapper",
                    beanPrefix(table.modelName()) + "ConverterImpl",
                    propertiesBean);

            Constructor<?> constructor = Arrays.stream(implementation.getDeclaredConstructors())
                    .filter(candidate -> candidate.getParameterCount() == 3)
                    .findFirst().orElseThrow();
            Set<String> parameterQualifiers = Arrays.stream(constructor.getParameters())
                    .map(parameter -> parameter.getAnnotation(Qualifier.class))
                    .filter(java.util.Objects::nonNull)
                    .map(Qualifier::value)
                    .collect(Collectors.toSet());
            assertThat(parameterQualifiers).containsExactlyInAnyOrderElementsOf(fieldQualifiers);
        }
    }

    @Test
    void allFiveMapStructConvertersPreservePersistenceFieldsAndEnumValues() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(requiredType("model.convertor.DdcAiEnumConverterImpl"),
                    requiredType("model.convertor.DdcAiResourceConverterImpl"),
                    requiredType("model.convertor.DdcAiVersionConverterImpl"),
                    requiredType("model.convertor.DdcAiDraftNodeConverterImpl"),
                    requiredType("model.convertor.DdcAiVersionNodeConverterImpl"),
                    requiredType("model.convertor.DdcAiPublicationConverterImpl"));
            context.refresh();
            assertAiConverterRoundTrips(context);
        }
    }

    @Test
    void resourceSummaryConverterUsesDecimalStringIdsAndNeverSerializesStorageIdentity() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(requiredType("model.convertor.DdcAiEnumConverterImpl"),
                    requiredType("model.convertor.DdcAiResourceResultConverterImpl"));
            context.refresh();
            DdcAiResourceResultConverter converter = context.getBean(DdcAiResourceResultConverter.class);
            DdcAiResourceBO resource = DdcAiResourceBO.builder().id(810001L).tenantId(1001L).version(4L)
                    .bizCode("retail").namespaceId(71L).namespaceCode("dev-space").envCode("dev")
                    .appCode("assistant-app").kind(DdcAiKindEnum.PROMPT).resourceKey("review-order")
                    .displayName("Review Order").description("").draftPrompt("internal prompt")
                    .lastVersionNo(2L).currentVersionId(820001L).currentVersionNo(2L)
                    .updateTime(Instant.parse("2026-09-29T09:00:00Z")).build();

            DdcAiResourceSummaryDTO summary = converter.toTarget(resource);

            assertThat(summary.getId()).isEqualTo("810001");
            assertThat(summary.getRevision()).isEqualTo("4");
            assertThat(summary.getLastVersionNo()).isEqualTo("2");
            assertThat(summary.getCurrentVersionId()).isEqualTo("820001");
            assertThat(summary.getCurrentVersionNo()).isEqualTo("2");
            assertThat(summary.getScope().namespaceCode()).isEqualTo("dev-space");
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            String summaryJson = mapper.writeValueAsString(summary);
            String internalJson = mapper.writeValueAsString(resource);
            assertThat(summaryJson).contains("\"id\":\"810001\"", "\"revision\":\"4\"")
                    .doesNotContain("internal prompt", "tenantId", "namespaceId");
            assertThat(internalJson).doesNotContain("tenantId", "namespaceId", "draftPrompt", "lastVersionNo",
                    "currentVersionId", "namespaceCode", "currentVersionNo");
        }
    }

    private void assertAiConverterRoundTrips(AnnotationConfigApplicationContext context) {
        DdcAiEnumConverter enums = context.getBean(DdcAiEnumConverter.class);
        DdcAiResourceConverter resources = context.getBean(DdcAiResourceConverter.class);
        DdcAiVersionConverter versions = context.getBean(DdcAiVersionConverter.class);
        DdcAiDraftNodeConverter draftNodes = context.getBean(DdcAiDraftNodeConverter.class);
        DdcAiVersionNodeConverter versionNodes = context.getBean(DdcAiVersionNodeConverter.class);
        DdcAiPublicationConverter publications = context.getBean(DdcAiPublicationConverter.class);

        DdcAiResourcePO resource = DdcAiResourcePO.builder().id(11L).tenantId(41L)
                .createUserId("actor").createTime(Instant.parse("2026-09-29T00:00:00Z"))
                .updateUserId("actor").updateTime(Instant.parse("2026-09-29T00:00:01Z"))
                .deletedAt(null).version(0L).bizCode("biz").namespaceId(51L).envCode("dev").appCode("agent")
                .kind(DdcAiKindPersistenceEnum.SKILL).resourceKey("skill").displayName("Skill")
                .description("").draftPrompt(null).lastVersionNo(0L).currentVersionId(null).build();
        DdcAiResourceBO resourceBO = resources.toTarget(resource);
        assertThat(resourceBO.getKind()).isEqualTo(DdcAiKindEnum.SKILL);
        assertThat(resources.toSource(resourceBO)).usingRecursiveComparison().isEqualTo(resource);

        DdcAiVersionPO version = DdcAiVersionPO.builder().id(12L).tenantId(41L)
                .createUserId("actor").createTime(Instant.parse("2026-09-29T00:00:00Z"))
                .updateUserId("actor").updateTime(Instant.parse("2026-09-29T00:00:01Z"))
                .deletedAt(null).version(0L).resourceId(11L).versionNo(1L).kind(DdcAiKindPersistenceEnum.SKILL)
                .displayName("Skill v1").description("").promptText(null).contentSha256("a".repeat(64))
                .contentBytes(8L).nodeCount(1).sourceVersionId(null).changeNote("").build();
        DdcAiVersionBO versionBO = versions.toTarget(version);
        assertThat(versionBO.getKind()).isEqualTo(DdcAiKindEnum.SKILL);
        assertThat(versions.toSource(versionBO)).usingRecursiveComparison().isEqualTo(version);

        DdcAiDraftNodePO draftNode = DdcAiDraftNodePO.builder().id(13L).tenantId(41L)
                .createUserId("actor").createTime(Instant.parse("2026-09-29T00:00:00Z"))
                .updateUserId("actor").updateTime(Instant.parse("2026-09-29T00:00:01Z"))
                .deletedAt(null).version(0L).resourceId(11L).parentPath(null).path("")
                .nodeType(DdcAiNodePersistenceEnum.DIRECTORY).content(null).contentSha256(null).contentBytes(0L).build();
        DdcAiDraftNodeBO draftNodeBO = draftNodes.toTarget(draftNode);
        assertThat(draftNodeBO.getNodeType()).isEqualTo(DdcAiNodeTypeEnum.DIRECTORY);
        assertThat(draftNodes.toSource(draftNodeBO)).usingRecursiveComparison().isEqualTo(draftNode);

        DdcAiVersionNodePO versionNode = DdcAiVersionNodePO.builder().id(14L).tenantId(41L)
                .createUserId("actor").createTime(Instant.parse("2026-09-29T00:00:00Z"))
                .updateUserId("actor").updateTime(Instant.parse("2026-09-29T00:00:01Z"))
                .deletedAt(null).version(0L).versionId(12L).parentPath(null).path("")
                .nodeType(DdcAiNodePersistenceEnum.DIRECTORY).content(null).contentSha256(null).contentBytes(0L).build();
        DdcAiVersionNodeBO versionNodeBO = versionNodes.toTarget(versionNode);
        assertThat(versionNodeBO.getNodeType()).isEqualTo(DdcAiNodeTypeEnum.DIRECTORY);
        assertThat(versionNodes.toSource(versionNodeBO)).usingRecursiveComparison().isEqualTo(versionNode);

        DdcAiPublicationPO publication = DdcAiPublicationPO.builder().id(15L).tenantId(41L)
                .createUserId("actor").createTime(Instant.parse("2026-09-29T00:00:00Z"))
                .updateUserId("actor").updateTime(Instant.parse("2026-09-29T00:00:01Z"))
                .deletedAt(null).version(0L).resourceId(11L).operation(DdcAiPublicationOperationEnum.PUBLISH)
                .requestId("request-000000001").requestSha256("b".repeat(64)).previousVersionId(null)
                .targetVersionId(12L).sourceVersionId(null).resultRevision(1L).targetCreated(false).build();
        DdcAiPublicationBO publicationBO = publications.toTarget(publication);
        assertThat(publicationBO.getOperation()).isEqualTo(DdcAiPublicationOperationEnum.PUBLISH);
        assertThat(publications.toSource(publicationBO)).usingRecursiveComparison().isEqualTo(publication);
    }

    @Test
    void aiMapperXmlIsTenantScopedAndSeparatesMutableRowsFromImmutableHistory() throws Exception {
        Configuration configuration = aiMappers();
        for (TableContract table : TABLES) {
            String mapperName = table.modelName() + "Mapper";
            String namespace = BASE + "dao." + mapperName;
            String xml = mapperXml(mapperName + ".xml").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            assertThat(configuration.hasStatement(namespace + ".selectActiveById")).isTrue();
            assertThat(configuration.hasStatement(namespace + ".selectActiveByIds")).isTrue();
            assertThat(xml).contains("tenant_id", "deleted_at is null")
                    .doesNotContain("_t0", "<delete", "delete from");
            assertAllSelectStatementsAreTenantAndLifecycleScoped(configuration, table);
            String activeById = mappedSql(configuration, namespace + ".selectActiveById",
                    Map.of("tenantId", 41L, "id", 83L));
            assertThat(activeById).contains("tenant_id = ?", "id = ?", "deleted_at is null");
            String activeByIds = mappedSql(configuration, namespace + ".selectActiveByIds",
                    Map.of("tenantId", 41L, "ids", List.of(83L, 84L)));
            assertThat(activeByIds).contains("tenant_id = ?", "deleted_at is null", "in ( ? , ? )");
            if (table.mutable()) {
                assertThat(configuration.hasStatement(namespace + ".deleteVersionedById")).isTrue();
                assertThat(xml).contains("update " + table.tableName() + " set deleted_at = current_timestamp at time zone 'utc'")
                        .contains("#{et.version}");
            } else {
                assertThat(configuration.hasStatement(namespace + ".deleteVersionedById")).isFalse();
                assertThat(xml).doesNotContain("<update", "update " + table.tableName() + " set");
                assertOverrideThrows(mapperName, "deleteVersionedById");
                assertOverrideThrows(mapperName, "restoreVersionedById");
            }
        }

        assertThat(configuration.hasStatement(BASE + "dao.DdcAiResourceMapper.selectIncludingDeletedById")).isTrue();
        assertThat(configuration.hasStatement(BASE + "dao.DdcAiResourceMapper.selectForUpdateById")).isTrue();
        assertThat(configuration.hasStatement(BASE + "dao.DdcAiResourceMapper.selectByBusinessKey")).isTrue();
        assertThat(configuration.hasStatement(BASE + "dao.DdcAiResourceMapper.selectPage")).isTrue();
        assertThat(configuration.hasStatement(BASE + "dao.DdcAiDraftNodeMapper.selectTree")).isTrue();
        assertThat(configuration.hasStatement(BASE + "dao.DdcAiVersionMapper.selectPage")).isTrue();
        assertThat(configuration.hasStatement(BASE + "dao.DdcAiVersionNodeMapper.selectTree")).isTrue();
        assertThat(configuration.hasStatement(BASE + "dao.DdcAiPublicationMapper.selectByRequestId")).isTrue();
        assertThat(configuration.hasStatement(BASE + "dao.DdcAiPublicationMapper.selectPage")).isTrue();

        Map<String, Object> activeParameters = Map.of("tenantId", 41L, "id", 83L);
        String rootSql = mappedSql(configuration, BASE + "dao.DdcAiResourceMapper.selectActiveById", activeParameters);
        assertThat(rootSql).contains("tenant_id = ?", "id = ?", "deleted_at is null");
        String historySql = mappedSql(configuration, BASE + "dao.DdcAiResourceMapper.selectIncludingDeletedById", activeParameters);
        assertThat(historySql).contains("r.tenant_id = ?", "r.id = ?", "r.deleted_at is not null")
                .doesNotContain("r.deleted_at is null", "join ddc_namespace", "join ddc_ai_version");
        assertThatCode(() -> CCJSqlParserUtil.parse(historySql)).doesNotThrowAnyException();
        String lockSql = mappedSql(configuration, BASE + "dao.DdcAiResourceMapper.selectForUpdateById", activeParameters);
        assertThat(lockSql).contains("tenant_id = ?", "id = ?", "deleted_at is null", "for update");
        assertThatCode(() -> CCJSqlParserUtil.parse(lockSql)).doesNotThrowAnyException();
        String businessKeySql = mappedSql(configuration, BASE + "dao.DdcAiResourceMapper.selectByBusinessKey",
                Map.of("tenantId", 41L, "bizCode", "biz", "namespaceId", 51L, "envCode", "dev",
                        "appCode", "agent", "kind", DdcAiKindPersistenceEnum.SKILL, "resourceKey", "skill"));
        assertThat(businessKeySql).contains("tenant_id = ?", "namespace_id = ?", "resource_key = ?", "deleted_at is null");

        Map<String, Object> treeParameters = Map.of("tenantId", 41L, "resourceId", 83L);
        String draftTreeSql = mappedSql(configuration, BASE + "dao.DdcAiDraftNodeMapper.selectTree", treeParameters);
        assertThat(draftTreeSql).contains("tenant_id = ?", "resource_id = ?", "deleted_at is null")
                .contains("order by n.path, n.id");
        Map<String, Object> versionTreeParameters = Map.of("tenantId", 41L, "versionId", 97L);
        String versionTreeSql = mappedSql(configuration, BASE + "dao.DdcAiVersionNodeMapper.selectTree", versionTreeParameters);
        assertThat(versionTreeSql).contains("tenant_id = ?", "version_id = ?", "deleted_at is null",
                "order by n.path, n.id");

        Map<String, Object> versionPageParameters = Map.of("tenantId", 41L, "resourceId", 83L, "offset", 0L, "limit", 10);
        String versionPageSql = mappedSql(configuration, BASE + "dao.DdcAiVersionMapper.selectPage", versionPageParameters);
        assertThat(versionPageSql).contains("v.tenant_id = ?", "v.resource_id = ?", "v.deleted_at is null",
                "r.deleted_at is null", "order by v.version_no desc");
        Map<String, Object> publicationParameters = Map.of("tenantId", 41L, "resourceId", 83L,
                "requestId", "request-000000001", "offset", 0L, "limit", 10);
        String publicationSql = mappedSql(configuration, BASE + "dao.DdcAiPublicationMapper.selectByRequestId",
                publicationParameters);
        assertThat(publicationSql).contains("p.tenant_id = ?", "p.resource_id = ?", "p.request_id = ?",
                "p.deleted_at is null", "r.deleted_at is null");

        Map<String, Object> deleteParameters = Map.of("et", new LifecycleParameters(41L, 83L, 7L));
        for (String mapperName : List.of("DdcAiResourceMapper", "DdcAiDraftNodeMapper")) {
            String sql = mappedSql(configuration, BASE + "dao." + mapperName + ".deleteVersionedById", deleteParameters);
            assertThat(sql).contains("tenant_id = ?", "id = ?", "version = ?", "deleted_at is null");
        }
    }

    @Test
    void resourceReadProjectionIsTransientAndItsShareLockAndPageJoinsStayTenantScoped() throws Exception {
        Field namespaceCode = DdcAiResourcePO.class.getDeclaredField("namespaceCode");
        Field currentVersionNo = DdcAiResourcePO.class.getDeclaredField("currentVersionNo");
        assertThat(namespaceCode.getType()).isEqualTo(String.class);
        assertThat(namespaceCode.getAnnotation(TableField.class).exist()).isFalse();
        assertThat(currentVersionNo.getType()).isEqualTo(Long.class);
        assertThat(currentVersionNo.getAnnotation(TableField.class).exist()).isFalse();

        Field boNamespaceCode = DdcAiResourceBO.class.getDeclaredField("namespaceCode");
        Field boCurrentVersionNo = DdcAiResourceBO.class.getDeclaredField("currentVersionNo");
        assertThat(boNamespaceCode.isAnnotationPresent(JsonIgnore.class)).isTrue();
        assertThat(boCurrentVersionNo.isAnnotationPresent(JsonIgnore.class)).isTrue();

        Configuration configuration = aiMappers();
        String mapper = BASE + "dao.DdcAiResourceMapper.";
        String lockSql = mappedSql(configuration, mapper + "selectActiveForShareById",
                Map.of("tenantId", 41L, "id", 83L));
        assertThat(lockSql).contains("from ddc_ai_resource r", "r.tenant_id = ?", "r.id = ?",
                "r.deleted_at is null", "n.deleted_at is null", "cv.deleted_at is null", "for share of r");
        assertThat(lockSql).doesNotContain("for update");
        assertThat(configuration.getMappedStatement(mapper + "selectActiveForShareById").getTimeout()).isEqualTo(2);
        assertThat(configuration.getMappedStatement(mapper + "selectForUpdateById").getTimeout()).isEqualTo(2);

        Map<String, Object> pageParameters = new HashMap<>();
        pageParameters.put("tenantId", 41L);
        pageParameters.put("bizCode", "biz");
        pageParameters.put("namespaceId", 51L);
        pageParameters.put("envCode", "dev");
        pageParameters.put("appCode", "agent");
        pageParameters.put("kind", DdcAiKindPersistenceEnum.SKILL);
        pageParameters.put("resourceKeyPrefix", "sk");
        pageParameters.put("displayName", "Skill");
        pageParameters.put("offset", 0L);
        pageParameters.put("limit", 10);
        String pageSql = mappedSql(configuration, mapper + "selectPage", pageParameters);
        assertThat(pageSql).contains("n.namespace_code as namespace_code", "cv.version_no as current_version_no",
                "left join ddc_namespace n on n.tenant_id = r.tenant_id and n.id = r.namespace_id",
                "left join ddc_ai_version cv on cv.tenant_id = r.tenant_id and cv.resource_id = r.id",
                "n.deleted_at is null", "cv.id = r.current_version_id", "cv.deleted_at is null",
                "r.tenant_id = ?", "r.deleted_at is null",
                "order by r.update_time desc, r.id desc", "resource_key like concat(?, '%') escape e'\\\\'");
        assertThat(pageSql).doesNotContain("for share", "for update");
        assertThatCode(() -> CCJSqlParserUtil.parse(pageSql)).doesNotThrowAnyException();
        assertThat(configuration.getResultMap(mapper + "DdcAiResourcePOResultMap").getResultMappings())
                .extracting(org.apache.ibatis.mapping.ResultMapping::getProperty)
                .contains("namespaceCode", "currentVersionNo");
    }

    @Test
    void publicationIdempotencyLookupIsTenantGlobalAcrossResourceRoots() throws Exception {
        Configuration configuration = aiMappers();
        String statement = BASE + "dao.DdcAiPublicationMapper.selectByTenantRequestId";
        assertThat(configuration.hasStatement(statement)).isTrue();

        Map<String, Object> parameters = Map.of("tenantId", 41L, "requestId", "request-000000001");
        String sql = mappedSql(configuration, statement, parameters);
        assertThat(sql).contains("tenant_id = ?", "request_id = ?", "deleted_at is null")
                .doesNotContain("resource_id = ?");
        assertThat(sql).contains("tv.version_no as target_version_no");

        String pageSql = mappedSql(configuration, BASE + "dao.DdcAiPublicationMapper.selectPage",
                Map.of("tenantId", 41L, "resourceId", 83L, "requestId", "request-000000001",
                        "offset", 0L, "limit", 10));
        assertThat(pageSql).contains("tv.version_no as target_version_no", "tv.tenant_id = p.tenant_id",
                "tv.resource_id = p.resource_id", "tv.id = p.target_version_id");

        assertThat(Arrays.stream(DdcAiPublicationPO.class.getDeclaredFields()).map(Field::getName).toList())
                .contains("targetVersionNo");
        Field poProjection = DdcAiPublicationPO.class.getDeclaredField("targetVersionNo");
        assertThat(poProjection.getAnnotation(TableField.class).exist()).isFalse();
        assertThat(poProjection.isAnnotationPresent(JsonIgnore.class)).isTrue();
        Field boProjection = DdcAiPublicationBO.class.getDeclaredField("targetVersionNo");
        assertThat(boProjection.isAnnotationPresent(JsonIgnore.class)).isTrue();
        assertThat(tableColumns(resource(AI_DDL).toLowerCase(Locale.ROOT), "ddc_ai_publication"))
                .doesNotContain("target_version_no");
        assertThatCode(() -> CCJSqlParserUtil.parse(sql)).doesNotThrowAnyException();
    }

    @Test
    void scopeParentLocksAreTenantScopedActiveRowsAndUseShareLocks() throws Exception {
        Configuration configuration = aiMappers();
        Map<String, Object> parameters = Map.of("tenantId", 41L, "bizCode", "biz",
                "namespaceCode", "dev-space", "envCode", "dev", "appCode", "agent");
        for (String method : List.of("selectBizIdForShare", "selectNamespaceIdForShare", "selectEnvIdForShare",
                "selectAppIdForShare", "selectBindingIdForShare")) {
            String statement = BASE + "dao.DdcAiResourceMapper." + method;
            assertThat(configuration.hasStatement(statement)).as(statement).isTrue();
            String sql = mappedSql(configuration, statement, parameters);
            assertThat(sql).as(statement).contains("tenant_id = ?", "deleted_at is null", "for share");
            assertThat(configuration.getMappedStatement(statement).getTimeout()).isEqualTo(2);
            assertThatCode(() -> CCJSqlParserUtil.parse(sql)).doesNotThrowAnyException();
        }
    }

    @Test
    void v001KeepsTenantOwnershipLifecycleUniquenessAndImmutableVersionRelations() throws Exception {
        String ddl = resource(AI_DDL).toLowerCase(Locale.ROOT);
        String normalized = ddl.replaceAll("\\s+", " ");

        assertThat(normalized).contains(
                "create unique index uk_ai_resource_lifecycle on ddc_ai_resource_t0 (tenant_id, biz_code, namespace_id, env_code, app_code, kind, resource_key, deleted_at)",
                "create unique index uk_ai_resource_live on ddc_ai_resource_t0 (tenant_id, biz_code, namespace_id, env_code, app_code, kind, resource_key) where deleted_at is null",
                "create unique index uk_ai_draft_node_lifecycle on ddc_ai_draft_node_t0 (tenant_id, resource_id, path, deleted_at)",
                "create unique index uk_ai_draft_node_live on ddc_ai_draft_node_t0 (tenant_id, resource_id, path) where deleted_at is null",
                "create unique index uk_ai_version_path on ddc_ai_version_node_t0 (tenant_id, version_id, path)",
                "create index ix_ai_publication_history on ddc_ai_publication_t0 (tenant_id, resource_id, id desc)",
                "constraint uk_ai_version_no unique (tenant_id, resource_id, version_no)",
                "constraint uk_ai_publication_request unique (tenant_id, request_id)",
                "constraint fk_ai_resource_current_version foreign key (tenant_id, id, current_version_id) references ddc_ai_version_t0 (tenant_id, resource_id, id) deferrable initially deferred",
                "constraint fk_ai_version_resource foreign key (tenant_id, resource_id, kind) references ddc_ai_resource_t0 (tenant_id, id, kind)",
                "constraint fk_ai_version_source foreign key (tenant_id, source_version_id, kind) references ddc_ai_version_t0 (tenant_id, id, kind)",
                "constraint fk_ai_publication_previous foreign key (tenant_id, resource_id, previous_version_id) references ddc_ai_version_t0 (tenant_id, resource_id, id)",
                "constraint fk_ai_publication_target foreign key (tenant_id, resource_id, target_version_id) references ddc_ai_version_t0 (tenant_id, resource_id, id)",
                "constraint fk_ai_publication_source foreign key (tenant_id, source_version_id) references ddc_ai_version_t0 (tenant_id, id)",
                "constraint fk_ai_version_node_parent foreign key (tenant_id, version_id, parent_path) references ddc_ai_version_node_t0 (tenant_id, version_id, path) deferrable initially deferred")
                .doesNotContain("constraint fk_ai_draft_node_parent");

        for (TableContract table : TABLES) {
            assertThat(tableColumns(ddl, table.tableName())).doesNotContain("public_id");
        }

        assertThat(tableColumns(ddl, "ddc_ai_resource")).contains("current_version_id", "last_version_no");
        assertThat(tableColumns(ddl, "ddc_ai_version")).contains("source_version_id", "content_sha256", "content_bytes");
        assertThat(tableColumns(ddl, "ddc_ai_draft_node")).contains("parent_path", "deleted_at", "content");
        assertThat(tableColumns(ddl, "ddc_ai_version_node")).contains("parent_path", "version_id", "content");
        assertThat(tableColumns(ddl, "ddc_ai_publication")).contains("request_id", "request_sha256", "source_version_id");
    }

    @Test
    void persistenceEnumsUseStableCodesAndSdkJsonEnumsRemainSeparate() throws Exception {
        Class<?> kind = requiredType("model.enums.DdcAiKindPersistenceEnum");
        Class<?> node = requiredType("model.enums.DdcAiNodePersistenceEnum");
        Class<?> operation = requiredType("model.enums.DdcAiPublicationOperationEnum");
        for (Class<?> type : List.of(kind, node, operation)) {
            assertThat(type.isEnum()).isTrue();
            assertThat(EgonEnum.class.isAssignableFrom(type)).isTrue();
            Field valueField = Arrays.stream(type.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(EnumValue.class))
                    .findFirst().orElseThrow();
            assertThat(valueField.getType()).isEqualTo(String.class);
            valueField.setAccessible(true);
            List<Object> persistedValues = Arrays.stream(type.getEnumConstants()).map(value -> {
                try {
                    return valueField.get(value);
                } catch (IllegalAccessException failure) {
                    throw new AssertionError(failure);
                }
            }).toList();
            assertThat(persistedValues).doesNotContainNull().doesNotHaveDuplicates();
            assertThat(Arrays.stream(type.getDeclaredMethods()).anyMatch(method -> method.isAnnotationPresent(JsonValue.class)))
                    .isEqualTo(type.equals(operation));
        }

        assertThat(kind).isNotEqualTo(DdcAiKindEnum.class);
        assertThat(node).isNotEqualTo(DdcAiNodeTypeEnum.class);
        Class<?> converterType = requiredType("model.convertor.DdcAiEnumConverter");
        assertThat(converterType.isInterface()).isTrue();
        assertThat(BaseConverter.class.isAssignableFrom(converterType)).isTrue();
        Object converter = Class.forName(converterType.getName() + "Impl").getConstructor().newInstance();
        for (String value : List.of("PROMPT", "SKILL")) {
            Object persisted = enumValue(kind, value);
            Object sdk = converterType.getMethod("toTarget", kind).invoke(converter, persisted);
            assertThat(sdk).isEqualTo(DdcAiKindEnum.valueOf(value));
            assertThat(converterType.getMethod("toSource", DdcAiKindEnum.class).invoke(converter, sdk)).isEqualTo(persisted);
        }
        for (String value : List.of("DIRECTORY", "FILE")) {
            Object persisted = enumValue(node, value);
            Object sdk = converterType.getMethod("toSdkNode", node).invoke(converter, persisted);
            assertThat(sdk).isEqualTo(DdcAiNodeTypeEnum.valueOf(value));
            assertThat(converterType.getMethod("toPersistenceNode", DdcAiNodeTypeEnum.class).invoke(converter, sdk))
                    .isEqualTo(persisted);
        }
        assertThatThrownBy(() -> DdcAiKindPersistenceEnum.fromValue("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DdcAiNodePersistenceEnum.fromValue("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DdcAiKindEnum.fromCode("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DdcAiNodeTypeEnum.fromCode("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);

        Class<?> operationType = requiredType("model.enums.DdcAiPublicationOperationEnum");
        assertThat(Arrays.stream(operationType.getDeclaredMethods()).anyMatch(method -> method.isAnnotationPresent(JsonCreator.class)))
                .isTrue();
        ObjectMapper objectMapper = new ObjectMapper();
        Object publish = enumValue(operationType, "PUBLISH");
        assertThat(objectMapper.writeValueAsString(publish)).isEqualTo("\"PUBLISH\"");
        assertThat(objectMapper.readValue("\"PROMOTE\"", operationType)).isEqualTo(enumValue(operationType, "PROMOTE"));
        assertThatThrownBy(() -> objectMapper.readValue("\"UNKNOWN\"", operationType))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onlyTheRootHistoricalReadIsWhitelistedAndRestoreRemainsClosed() throws Exception {
        var sources = new YamlPropertySourceLoader().load("tianshu-persistence", new ClassPathResource(PERSISTENCE_YAML));
        StandardEnvironment environment = new StandardEnvironment();
        sources.forEach(environment.getPropertySources()::addFirst);
        EgonColaMybatisPlusProperties properties = Binder.get(environment)
                .bind(EgonColaMybatisPlusProperties.PREFIX, Bindable.of(EgonColaMybatisPlusProperties.class))
                .orElseThrow(() -> new IllegalStateException("Missing Tianshu MP configuration"));

        assertThat(properties.getLifecycle().getHistoryReadStatements())
                .containsExactly(BASE + "dao.DdcAiResourceMapper.selectIncludingDeletedById");
        assertThat(properties.getLifecycle().getRestoreStatements()).isEmpty();
    }

    private Configuration aiMappers() throws IOException {
        Configuration configuration = new Configuration();
        for (TableContract table : TABLES) {
            String mapperName = table.modelName() + "Mapper";
            String resource = "mybatis/mapper/tianshu/" + mapperName + ".xml";
            try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertThat(input).as(resource).isNotNull();
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        return configuration;
    }

    private void assertAllSelectStatementsAreTenantAndLifecycleScoped(Configuration configuration,
            TableContract table) {
        String mapperName = table.modelName() + "Mapper";
        String namespace = BASE + "dao." + mapperName + ".";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("tenantId", 41L);
        parameters.put("id", 83L);
        parameters.put("ids", List.of(83L, 84L));
        parameters.put("resourceId", 91L);
        parameters.put("versionId", 97L);
        parameters.put("versionNo", 2L);
        parameters.put("path", "docs/SKILL.md");
        parameters.put("bizCode", "biz");
        parameters.put("namespaceId", 51L);
        parameters.put("envCode", "dev");
        parameters.put("appCode", "agent");
        parameters.put("kind", DdcAiKindPersistenceEnum.SKILL);
        parameters.put("resourceKey", "skill");
        parameters.put("resourceKeyPrefix", "sk");
        parameters.put("displayName", "Skill");
        parameters.put("requestId", "request-000000001");
        parameters.put("offset", 0L);
        parameters.put("limit", 10);

        Set<String> selects = configuration.getMappedStatementNames().stream()
                .filter(statement -> statement.startsWith(namespace))
                .filter(statement -> configuration.getMappedStatement(statement).getSqlCommandType()
                        == org.apache.ibatis.mapping.SqlCommandType.SELECT)
                .collect(Collectors.toSet());
        assertThat(selects).isNotEmpty();
        for (String statement : selects) {
            String sql = mappedSql(configuration, statement, parameters);
            assertThat(sql).as(statement).contains("tenant_id = ?");
            if (statement.equals(BASE + "dao.DdcAiResourceMapper.selectIncludingDeletedById")) {
                assertThat(sql).contains("r.id = ?", "r.deleted_at is not null")
                        .doesNotContain("r.deleted_at is null", "join ddc_namespace", "join ddc_ai_version");
            } else {
                assertThat(sql).as(statement).contains("deleted_at is null");
            }
        }
    }

    private String mappedSql(Configuration configuration, String statement, Object parameters) {
        return configuration.getMappedStatement(statement).getBoundSql(parameters).getSql()
                .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String mapperXml(String name) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream("mybatis/mapper/tianshu/" + name)) {
            assertThat(input).as(name).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String resource(String name) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(name)) {
            assertThat(input).as(name).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Set<String> tableColumns(String ddl, String tableName) {
        Pattern pattern = Pattern.compile("(?is)create\\s+table\\s+" + Pattern.quote(tableName)
                + "_t0\\s*\\((.*?)\\n\\);");
        Matcher matcher = pattern.matcher(ddl);
        assertThat(matcher.find()).as("DDL for " + tableName).isTrue();
        return Arrays.stream(matcher.group(1).split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("CONSTRAINT "))
                .map(line -> line.split("\\s+", 2)[0])
                .filter(column -> column.matches("[a-z][a-z0-9_]*"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String databaseColumn(Field field) {
        TableField mapping = field.getAnnotation(TableField.class);
        if (mapping != null && !mapping.value().isBlank()) return mapping.value();
        return field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private boolean isPersistedField(Field field) {
        TableField mapping = field.getAnnotation(TableField.class);
        return mapping == null || mapping.exist();
    }

    private Set<String> poFieldNames(Class<?> po) {
        return Arrays.stream(po.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName).collect(Collectors.toSet());
    }

    private Set<String> declaredFieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName).collect(Collectors.toSet());
    }

    private void assertGeneratedPojoContract(Class<?> type) throws Exception {
        int fields = declaredFieldNames(type).size();
        assertThat(type.getDeclaredConstructor()).isNotNull();
        assertThat(Arrays.stream(type.getDeclaredConstructors())
                .anyMatch(constructor -> constructor.getParameterCount() == fields)).isTrue();
        assertThat(type.getDeclaredMethod("builder")).isNotNull();
        assertThat(type.getMethod("equals", Object.class)).isNotNull();
        Field sample = Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .findFirst().orElseThrow();
        String setter = "set" + Character.toUpperCase(sample.getName().charAt(0)) + sample.getName().substring(1);
        assertThat(type.getMethod(setter, sample.getType()).getReturnType()).isEqualTo(type);
    }

    private Object interfaceProxy(Class<?> contract) {
        return Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[] {contract}, (proxy, method, arguments) -> null);
    }

    private String beanPrefix(String typeName) {
        return Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
    }

    private Class<?> fieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException failure) {
            throw new AssertionError("Missing field " + type.getName() + "." + name, failure);
        }
    }

    private void assertFieldTypes(String relativeName, Map<String, String> expectedTypes) {
        Class<?> type = requiredType(relativeName);
        expectedTypes.forEach((field, relativeType) -> assertThat(fieldType(type, field))
                .isEqualTo(requiredType(relativeType)));
    }

    private void assertOverrideThrows(String mapperName, String methodName) throws Exception {
        Class<?> mapper = requiredType("dao." + mapperName);
        Class<?> po = requiredType("model.po." + mapperName.replace("Mapper", "") + "PO");
        assertThat(mapper.getDeclaredMethod(methodName, po).isDefault()).isTrue();
    }

    private Class<?> requiredType(String relativeName) {
        try {
            if (relativeName.startsWith("top.egon.")) return Class.forName(relativeName);
            return Class.forName(BASE + relativeName);
        } catch (ClassNotFoundException failure) {
            throw new AssertionError("Missing AI persistence contract: " + relativeName, failure);
        }
    }

    private Object enumValue(Class<?> type, String name) {
        return Arrays.stream(type.getEnumConstants()).filter(value -> ((Enum<?>) value).name().equals(name))
                .findFirst().orElseThrow(() -> new AssertionError("Missing enum constant " + type.getName() + "." + name));
    }

    private record TableContract(String modelName, String tableName, boolean mutable) { }

    private record LifecycleParameters(Long tenantId, Long id, Long version) { }
}
