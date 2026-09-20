package top.egon.cola.component.common.desensitize;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.desensitize.annotation.Sensitive;
import top.egon.cola.component.common.desensitize.annotation.SensitiveScene;
import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.logback.SensitiveLogConverter;
import top.egon.cola.component.common.desensitize.logback.SensitiveLogs;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

/**
 * REQ-004/REQ-005/REQ-006 for the data-desensitize starter: the handwritten annotation enums carry
 * the public {@link EgonEnum} code contract, and the logback converter stays a framework codec
 * instead of being turned into an object mapper (Spec §8.4).
 */
class RemainingComponentContractTest {

    private static final Pattern ENUM_HEADER =
            Pattern.compile("^\\s*(?:[a-z]+\\s+)*enum\\s+(\\w+)[^{]*\\{");

    private static final Pattern PACKAGE_DECLARATION =
            Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");

    /** SensitiveScene + SensitiveType; keeps the source scan non-vacuous. */
    private static final int EXPECTED_HANDWRITTEN_ENUMS = 2;

    @Test
    void everyHandwrittenEnumImplementsThePublicEgonEnumContract() throws Exception {
        List<String> declaredEnums = declaredEnumTypes();

        assertThat(declaredEnums).hasSize(EXPECTED_HANDWRITTEN_ENUMS);
        for (String declaredEnum : declaredEnums) {
            Class<?> enumType = Class.forName(declaredEnum);
            assertThat(EgonEnum.class.isAssignableFrom(enumType))
                    .as("%s 必须实现公共 EgonEnum 合同", declaredEnum)
                    .isTrue();
        }
    }

    @Test
    void enumCodesAreFixedLiteralsInsteadOfDynamicOrdinals() {
        assertThat(codeTable(SensitiveScene.class)).containsExactly(
                entry("RESPONSE", 0),
                entry("LOG", 1));
        assertThat(codeTable(SensitiveType.class)).containsExactly(
                entry("MOBILE", 0),
                entry("EMAIL", 1),
                entry("ID_CARD", 2),
                entry("BANK_CARD", 3),
                entry("NAME", 4),
                entry("ADDRESS", 5),
                entry("FULL", 6));

        for (Class<? extends Enum<?>> enumType : governedEnums()) {
            for (Enum<?> constant : enumType.getEnumConstants()) {
                assertThat(messageOf(constant))
                        .as("%s.%s 必须复用既有标签作为 message", enumType.getSimpleName(), constant.name())
                        .isNotBlank();
            }
        }
    }

    @Test
    void existingWireNamesAndAnnotationAccessorsStayUnchanged() throws Exception {
        assertThat(Stream.of(SensitiveScene.values()).map(Enum::name).toList())
                .containsExactly("RESPONSE", "LOG");
        assertThat(Stream.of(SensitiveType.values()).map(Enum::name).toList())
                .containsExactly("MOBILE", "EMAIL", "ID_CARD", "BANK_CARD", "NAME", "ADDRESS", "FULL");

        assertThat(SensitiveScene.RESPONSE.getDesc()).isEqualTo("前端返回");
        assertThat(SensitiveScene.LOG.getDesc()).isEqualTo("日志输出");
        assertThat(SensitiveType.MOBILE.getDesc()).isEqualTo("手机号");
        assertThat(SensitiveType.FULL.getDesc()).isEqualTo("全部隐藏");
        assertThat(messageOf(SensitiveScene.RESPONSE)).isEqualTo(SensitiveScene.RESPONSE.getDesc());
        assertThat(messageOf(SensitiveType.NAME)).isEqualTo(SensitiveType.NAME.getDesc());

        assertThat(Sensitive.class.getMethod("type").getReturnType()).isEqualTo(SensitiveType.class);
        assertThat(Sensitive.class.getMethod("scenes").getReturnType()).isEqualTo(SensitiveScene[].class);
        assertThat(SensitiveLogs.of("13812345678", SensitiveType.MOBILE)).isEqualTo("138****5678");
    }

    @Test
    void logConverterRemainsALogbackCodecInsteadOfAnObjectMapper() throws Exception {
        assertThat(ClassicConverter.class.isAssignableFrom(SensitiveLogConverter.class)).isTrue();
        assertThat(conversionContractsOf(SensitiveLogConverter.class)).isEmpty();

        Constructor<SensitiveLogConverter> noArgsConstructor =
                SensitiveLogConverter.class.getConstructor();
        SensitiveLogConverter converter = noArgsConstructor.newInstance();

        ILoggingEvent event = new LoggingEvent(
                RemainingComponentContractTest.class.getName(),
                new LoggerContext().getLogger("remaining-component-contract"),
                Level.INFO,
                "value={}",
                null,
                new Object[] {42});

        assertThat(converter.convert(event)).isEqualTo("value=42");
    }

    @Test
    void noMapStructOrBulkCopyingContractIsFabricatedInThisModule() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> sources = mainSources()) {
            for (Path source : (Iterable<Path>) sources::iterator) {
                List<String> lines = Files.readAllLines(source);
                for (int index = 0; index < lines.size(); index++) {
                    String line = lines.get(index);
                    if (line.contains("org.mapstruct")
                            || line.contains("io.github.linpeilie")
                            || line.contains("BaseConverter")
                            || line.contains("BaseForwardConverter")) {
                        offenders.add(source + ":" + (index + 1));
                    }
                }
            }
        }
        assertThat(offenders)
                .as("纯协议 codec 不得被机械改写成 MapStruct 对象转换器")
                .isEmpty();
    }

    private static List<String> declaredEnumTypes() throws IOException {
        List<String> declared = new ArrayList<>();
        try (Stream<Path> sources = mainSources()) {
            for (Path source : (Iterable<Path>) sources::iterator) {
                String packageName = "";
                for (String line : Files.readAllLines(source)) {
                    Matcher packageMatcher = PACKAGE_DECLARATION.matcher(line);
                    if (packageMatcher.find()) {
                        packageName = packageMatcher.group(1);
                        continue;
                    }
                    Matcher enumMatcher = ENUM_HEADER.matcher(line);
                    if (enumMatcher.find()) {
                        declared.add(packageName + "." + enumMatcher.group(1));
                    }
                }
            }
        }
        declared.sort(String::compareTo);
        return declared;
    }

    private static List<Class<? extends Enum<?>>> governedEnums() {
        List<Class<? extends Enum<?>>> enumTypes = new ArrayList<>();
        enumTypes.add(SensitiveScene.class);
        enumTypes.add(SensitiveType.class);
        return enumTypes;
    }

    private static List<Class<?>> conversionContractsOf(Class<?> type) {
        List<Class<?>> contracts = new ArrayList<>();
        for (Class<?> implementedInterface : type.getInterfaces()) {
            if (implementedInterface.getSimpleName().endsWith("Converter")) {
                contracts.add(implementedInterface);
            }
        }
        for (var annotation : type.getAnnotations()) {
            String annotationName = annotation.annotationType().getName();
            if (annotationName.startsWith("org.mapstruct") || annotationName.startsWith("io.github.linpeilie")) {
                contracts.add(annotation.annotationType());
            }
        }
        return contracts;
    }

    private static Map<String, Integer> codeTable(Class<? extends Enum<?>> enumType) {
        Map<String, Integer> codes = new LinkedHashMap<>();
        for (Enum<?> constant : enumType.getEnumConstants()) {
            codes.put(constant.name(), codeOf(constant));
        }
        return codes;
    }

    private static int codeOf(Enum<?> constant) {
        return (Integer) invokeGetter(constant, "getCode");
    }

    private static String messageOf(Enum<?> constant) {
        return (String) invokeGetter(constant, "getMessage");
    }

    private static Object invokeGetter(Enum<?> constant, String getterName) {
        Class<? extends Enum<?>> enumType = constant.getDeclaringClass();
        Method getter;
        try {
            getter = enumType.getMethod(getterName);
        } catch (NoSuchMethodException exception) {
            return fail("%s 缺少公共 %s() 合同", enumType.getSimpleName(), getterName);
        }
        try {
            return getter.invoke(constant);
        } catch (ReflectiveOperationException exception) {
            return fail("%s.%s 的 %s() 无法调用", enumType.getSimpleName(), constant.name(), getterName);
        }
    }

    private static Stream<Path> mainSources() throws IOException {
        return Files.walk(Path.of("src", "main", "java"))
                .filter(path -> path.toString().endsWith(".java"))
                .sorted();
    }
}
