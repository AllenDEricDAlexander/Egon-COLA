package top.egon.cola.component.tianshu.admin.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TEST-029/030：本计划新增 Admin 类的静态门禁（语义后缀、@Slf4j、具名 Bean、@Qualifier 构造注入、java.time、
 * 工具库白名单、admin.ai 多环境键一致）。
 * / TEST-029/030: static gate over the Admin classes added by this plan (semantic suffixes, @Slf4j, named beans,
 * qualified constructor injection, java.time only, approved utilities and identical admin.ai keys per profile).
 */
class DdcAiOptimizationStandardsTest {

    private static final String BASE = "top.egon.cola.component.tianshu.admin.";
    private static final Path SOURCE_ROOT = Path.of("src/main/java/top/egon/cola/component/tianshu/admin");

    private static final Map<String, String> MODEL_SUFFIXES = Map.of(
            "model.bo.", "BO", "model.po.", "PO", "model.dto.", "DTO", "model.command.", "Command",
            "model.query.", "Query", "model.vo.", "Result", "model.enums.", "Enum", "model.convertor.", "Converter");

    private static final List<String> NEW_MODEL_TYPES = List.of(
            "model.bo.DdcAiJudgeContextBO", "model.bo.DdcAiOptimizationContextBO", "model.bo.DdcAiOptimizationPolicyBO",
            "model.bo.DdcAiOptimizationRunBO", "model.bo.DdcAiScoreAggregateBO", "model.bo.DdcAiUsageFeedbackBO",
            "model.command.DdcAiOptimizationPolicyCommand", "model.command.DdcAiOptimizationTriggerCommand",
            "model.command.DdcAiSkillDraftUploadCommand", "model.command.DdcAiSkillPublishCommand",
            "model.command.DdcAiUsageReportBatchCommand", "model.command.DdcAiUsageReportCommand",
            "model.convertor.DdcAiOptimizationPolicyConverter", "model.convertor.DdcAiOptimizationPolicyResultConverter",
            "model.convertor.DdcAiOptimizationRunConverter", "model.convertor.DdcAiOptimizationRunResultConverter",
            "model.convertor.DdcAiPublicationEventConverter", "model.convertor.DdcAiSyncCommandConverter",
            "model.convertor.DdcAiUsageFeedbackConverter", "model.convertor.DdcAiUsageFeedbackResultConverter",
            "model.dto.DdcAiJudgeVerdictDTO", "model.dto.DdcAiModelRequestDTO", "model.dto.DdcAiModelResultDTO",
            "model.enums.DdcAiJudgeStatePersistenceEnum", "model.enums.DdcAiOutcomePersistenceEnum",
            "model.enums.DdcAiPolicyStatePersistenceEnum", "model.enums.DdcAiRunErrorCodeEnum",
            "model.enums.DdcAiRunStatePersistenceEnum", "model.enums.DdcAiRunTriggerPersistenceEnum",
            "model.po.DdcAiOptimizationPolicyPO", "model.po.DdcAiOptimizationRunPO", "model.po.DdcAiScoreAggregatePO",
            "model.po.DdcAiUsageFeedbackPO", "model.query.DdcAiOptimizationRunPageQuery",
            "model.query.DdcAiUsageFeedbackPageQuery", "model.vo.DdcAiOptimizationPolicyResult",
            "model.vo.DdcAiOptimizationRunResult", "model.vo.DdcAiPublicationReceiptResult",
            "model.vo.DdcAiRevisionResult", "model.vo.DdcAiUsageFeedbackResult",
            "model.vo.DdcAiUsageReportReceiptResult", "model.vo.DdcAiVersionHeadResult");

    private static final List<String> NEW_SPRING_BEANS = List.of(
            "repository.impl.DdcAiOptimizationPolicyRepositoryImpl", "repository.impl.DdcAiOptimizationRunRepositoryImpl",
            "repository.impl.DdcAiUsageFeedbackRepositoryImpl", "service.ai.impl.DdcAiFeedbackServiceImpl",
            "service.ai.impl.DdcAiJudgeContextLoader", "service.ai.impl.DdcAiOptimizationServiceImpl",
            "service.ai.impl.DdcAiOptimizationStateTransition", "service.ai.impl.DdcAiPromptTemplateLoader",
            "service.ai.impl.DdcDefaultAiJudgeStrategy", "service.ai.impl.DdcPromptOptimizerStrategy",
            "service.ai.impl.DdcSkillOptimizerStrategy", "service.ai.notification.DdcAiPublicationDeliveryHandler",
            "service.ai.notification.DdcAiPublicationNotifier");

    private static final List<String> NEW_FACTORY_BEANS = List.of(
            "service.ai.job.DdcAiFeedbackRetentionScanner", "service.ai.job.DdcAiJudgeScanner",
            "service.ai.job.DdcAiOptimizationScanner", "service.ai.job.DdcAiTenantJobRunner",
            "service.ai.model.YuhengLlmModelAdapter");

    private static final List<String> FORBIDDEN_TEXT = List.of(
            "java.util.Date", "java.util.Calendar", "java.text.SimpleDateFormat", "cn.hutool", "org.joda");

    @Test
    void newModelTypesUseSemanticSuffixes() {
        for (String type : NEW_MODEL_TYPES) {
            String suffix = MODEL_SUFFIXES.entrySet().stream().filter(entry -> type.startsWith(entry.getKey()))
                    .map(Map.Entry::getValue).findFirst().orElseThrow();
            assertThat(load(type).getSimpleName()).as("%s semantic suffix", type).endsWith(suffix);
        }
    }

    @Test
    void businessBeansDeclareSlf4jNamedBeansAndQualifiedConstructors() {
        for (String name : NEW_SPRING_BEANS) {
            Class<?> type = load(name);
            assertThat(hasStaticLogger(type)).as("%s must declare @Slf4j", type.getSimpleName()).isTrue();
            assertThat(beanName(type)).as("%s must name its bean", type.getSimpleName()).isNotBlank();
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                for (Parameter parameter : constructor.getParameters()) {
                    assertThat(parameter.isAnnotationPresent(Qualifier.class))
                            .as("%s constructor parameter %s needs @Qualifier", type.getSimpleName(),
                                    parameter.getType().getSimpleName())
                            .isTrue();
                }
            }
        }
        for (String name : NEW_FACTORY_BEANS) {
            assertThat(hasStaticLogger(load(name))).as("%s must declare @Slf4j", name).isTrue();
        }
    }

    @Test
    void newSourcesUseJavaTimeAndApprovedUtilitiesOnly() throws IOException {
        List<String> newFiles = Stream.of(NEW_MODEL_TYPES, NEW_SPRING_BEANS, NEW_FACTORY_BEANS).flatMap(List::stream)
                .map(type -> type.replace('.', '/') + ".java").toList();
        for (String relative : newFiles) {
            String source = Files.readString(SOURCE_ROOT.resolve(relative), StandardCharsets.UTF_8);
            for (String forbidden : FORBIDDEN_TEXT) {
                assertThat(source).as("%s must not use %s", relative, forbidden).doesNotContain(forbidden);
            }
        }
    }

    @Test
    void aiProfilesDeclareIdenticalKeys() {
        Set<String> base = aiKeys("application.yml");
        assertThat(base).contains("egon.cola.component.tianshu.admin.ai.optimization.enabled",
                "egon.cola.component.tianshu.admin.ai.model.base-url");
        assertThat(aiKeys("application-local.yml")).isEqualTo(base);
        assertThat(aiKeys("application-test.yml")).isEqualTo(base);
    }

    private static Set<String> aiKeys(String file) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(file));
        Properties properties = yaml.getObject();
        assertThat(properties).as(file).isNotNull();
        return properties.keySet().stream().map(String::valueOf)
                .filter(key -> key.startsWith("egon.cola.component.tianshu.admin.ai."))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean hasStaticLogger(Class<?> type) {
        try {
            Field field = type.getDeclaredField("log");
            return Modifier.isStatic(field.getModifiers()) && field.getType().getName().equals("org.slf4j.Logger");
        } catch (NoSuchFieldException missing) {
            return false;
        }
    }

    private static String beanName(Class<?> type) {
        if (type.isAnnotationPresent(Service.class)) return type.getAnnotation(Service.class).value();
        if (type.isAnnotationPresent(Component.class)) return type.getAnnotation(Component.class).value();
        if (type.isAnnotationPresent(Repository.class)) return type.getAnnotation(Repository.class).value();
        return "";
    }

    private static Class<?> load(String relativeName) {
        try {
            return Class.forName(BASE + relativeName);
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("Missing class " + relativeName, missing);
        }
    }
}
