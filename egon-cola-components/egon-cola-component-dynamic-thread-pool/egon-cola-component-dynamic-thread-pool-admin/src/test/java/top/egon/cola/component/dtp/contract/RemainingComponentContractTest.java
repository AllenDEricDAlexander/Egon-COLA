package top.egon.cola.component.dtp.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.dtp.admin.types.Response;

import java.io.IOException;
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
 * REQ-004/REQ-005/REQ-006 for the DTP admin service: the response envelope keeps its published
 * String wire codes while the nested status enum moves onto the public {@link ErrorStatus}
 * contract, and the module keeps the common exception hierarchy untouched because it declares no
 * custom exception of its own.
 */
class RemainingComponentContractTest {

    private static final Pattern ENUM_HEADER =
            Pattern.compile("^\\s*(?:[a-z]+\\s+)*enum\\s+(\\w+)[^{]*\\{");

    private static final Pattern EXCEPTION_HEADER =
            Pattern.compile("^\\s*(?:public|private|protected|static|final)\\s+class\\s+(\\w*Exception)\\b");

    /** Response.Code; keeps the source scan non-vacuous. */
    private static final int EXPECTED_HANDWRITTEN_ENUMS = 1;

    @Test
    void responseCodeImplementsThePublicErrorStatusContract() {
        assertThat(ErrorStatus.class.isAssignableFrom(Response.Code.class)).isTrue();
        assertThat(EgonEnum.class.isAssignableFrom(Response.Code.class)).isTrue();
    }

    @Test
    void legacyStringWireCodeMovesToGetStatusWhileGetCodeBecomesTheIntegerContract() {
        assertThat(codeTable()).containsExactly(
                entry("SUCCESS", 0),
                entry("UN_ERROR", 1),
                entry("ILLEGAL_PARAMETER", 2));
        assertThat(statusTable()).containsExactly(
                entry("SUCCESS", "0000"),
                entry("UN_ERROR", "0001"),
                entry("ILLEGAL_PARAMETER", "0002"));
        assertThat(messageTable()).containsExactly(
                entry("SUCCESS", "调用成功"),
                entry("UN_ERROR", "调用失败"),
                entry("ILLEGAL_PARAMETER", "非法参数"));
    }

    @Test
    void responseEnvelopeKeepsItsPublishedCodeAndInfo() {
        Response<String> success = Response.success("payload");
        assertThat(success.getCode()).isEqualTo("0000");
        assertThat(success.getInfo()).isEqualTo("调用成功");
        assertThat(success.getData()).isEqualTo("payload");

        Response<String> illegal = Response.error("corePoolSize must be positive");
        assertThat(illegal.getCode()).isEqualTo("0002");
        assertThat(illegal.getInfo()).isEqualTo("corePoolSize must be positive");
        assertThat(illegal.getData()).isNull();

        Response<String> failure = Response.fail("redis unavailable");
        assertThat(failure.getCode()).isEqualTo("0001");
        assertThat(failure.getInfo()).isEqualTo("redis unavailable");
    }

    @Test
    void handwrittenEnumsUseTheCommonContractAndDeclareNoLocalExceptionHierarchy() throws IOException {
        List<String> enums = new ArrayList<>();
        List<String> exceptions = new ArrayList<>();
        try (Stream<Path> sources = mainSources()) {
            for (Path source : (Iterable<Path>) sources::iterator) {
                for (String line : Files.readAllLines(source)) {
                    Matcher enumMatcher = ENUM_HEADER.matcher(line);
                    if (enumMatcher.find()) {
                        enums.add(source + ": " + enumMatcher.group(1));
                        continue;
                    }
                    Matcher exceptionMatcher = EXCEPTION_HEADER.matcher(line);
                    if (exceptionMatcher.find()) {
                        exceptions.add(source + ": " + exceptionMatcher.group(1));
                    }
                }
            }
        }

        assertThat(enums).as("本模块手写字面量枚举必须全部纳入公共合同").hasSize(EXPECTED_HANDWRITTEN_ENUMS);
        assertThat(exceptions)
                .as("REQ-002/REQ-003 对本模块不适用：admin 不声明自定义异常")
                .isEmpty();
    }

    private static Map<String, Integer> codeTable() {
        Map<String, Integer> codes = new LinkedHashMap<>();
        for (Response.Code constant : Response.Code.values()) {
            codes.put(constant.name(), codeOf(constant));
        }
        return codes;
    }

    private static int codeOf(Response.Code constant) {
        Object code = invokeGetter(constant, "getCode");
        assertThat(code)
                .as("Response.Code.%s 的 getCode() 必须返回 int，旧 String wire code 改由 getStatus() 暴露",
                        constant.name())
                .isInstanceOf(Integer.class);
        return (Integer) code;
    }

    private static Map<String, String> statusTable() {
        return stringTable("getStatus");
    }

    private static Map<String, String> messageTable() {
        return stringTable("getMessage");
    }

    private static Map<String, String> stringTable(String getterName) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Response.Code constant : Response.Code.values()) {
            values.put(constant.name(), (String) invokeGetter(constant, getterName));
        }
        return values;
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
