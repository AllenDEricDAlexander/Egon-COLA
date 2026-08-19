package top.egon.cola.component.gateway.mcp.prompt.domain;

import top.egon.cola.component.gateway.mcp.prompt.service.McpPromptDriver;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Performs one-pass literal interpolation without expression evaluation.
 * 补充说明 / Supplementary summary: {@code StrictPromptTemplate} 是类型，位于当前 Gateway 模块的相关包中，负责Strict提示词模板相关的职责与边界。
 * English supplement: {@code StrictPromptTemplate} is a type in the current Gateway module; it owns the strict prompt template-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class StrictPromptTemplate {

    /**
     * 中文说明：表示 MAX模板LENGTH 这一固定值；它属于 {@code StrictPromptTemplate} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value max template length; it is a state, type, or protocol value of {@code StrictPromptTemplate} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code StrictPromptTemplate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code StrictPromptTemplate}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int MAX_TEMPLATE_LENGTH = 256 * 1024;

    /**
     * 中文说明：表示 MAXARGUMENTLENGTH 这一固定值；它属于 {@code StrictPromptTemplate} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value max argument length; it is a state, type, or protocol value of {@code StrictPromptTemplate} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code StrictPromptTemplate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code StrictPromptTemplate}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int MAX_ARGUMENT_LENGTH = 32 * 1024;

    /**
     * 中文说明：执行 render 操作；该方法是 {@code StrictPromptTemplate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the render operation; this method is the invocation entry point on {@code StrictPromptTemplate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code StrictPromptTemplate.render(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param template 参数 模板；parameter template。
     * @param declaredArguments 参数 declaredArguments；parameter declared arguments。
     * @param arguments 参数 arguments；parameter arguments。
     * @return 返回 render 的处理结果；returns the result of the operation.
     */
    public String render(
            String template,
            List<String> declaredArguments,
            Map<String, String> arguments) {
        Objects.requireNonNull(template, "template");
        if (template.length() > MAX_TEMPLATE_LENGTH) {
            throw McpPromptDriver.invalid("MCP prompt template is too large");
        }
        Set<String> declared = declared(declaredArguments);
        Map<String, String> supplied = Map.copyOf(Objects.requireNonNull(
                arguments,
                "arguments"
        ));
        if (!declared.containsAll(supplied.keySet())) {
            throw McpPromptDriver.invalid(
                    "MCP prompt contains an undeclared argument"
            );
        }
        StringBuilder output = new StringBuilder(template.length());
        int cursor = 0;
        while (cursor < template.length()) {
            int start = template.indexOf("${", cursor);
            if (start < 0) {
                output.append(template, cursor, template.length());
                break;
            }
            output.append(template, cursor, start);
            int end = template.indexOf('}', start + 2);
            if (end < 0) {
                throw McpPromptDriver.invalid(
                        "MCP prompt expression is invalid"
                );
            }
            String name = template.substring(start + 2, end);
            if (!identifier(name) || !declared.contains(name)) {
                throw McpPromptDriver.invalid(
                        "MCP prompt expression is not declared"
                );
            }
            String value = supplied.get(name);
            if (value == null) {
                throw McpPromptDriver.invalid(
                        "MCP prompt argument is required: " + name
                );
            }
            if (value.length() > MAX_ARGUMENT_LENGTH) {
                throw McpPromptDriver.invalid(
                        "MCP prompt argument is too large: " + name
                );
            }
            output.append(value);
            cursor = end + 1;
        }
        if (output.length() > 512 * 1024) {
            throw McpPromptDriver.invalid("MCP prompt result is too large");
        }
        return output.toString();
    }

    /**
     * 中文说明：执行 declared 操作；该方法是 {@code StrictPromptTemplate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the declared operation; this method is the invocation entry point on {@code StrictPromptTemplate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code StrictPromptTemplate.declared(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 declared 的处理结果；returns the result of the operation.
     */
    private Set<String> declared(List<String> source) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Objects.requireNonNull(source, "declaredArguments").forEach(name -> {
            if (!identifier(name) || !result.add(name)) {
                throw McpPromptDriver.invalid(
                        "MCP prompt argument declaration is invalid"
                );
            }
        });
        return Set.copyOf(result);
    }

    /**
     * 中文说明：执行 identifier 操作；该方法是 {@code StrictPromptTemplate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identifier operation; this method is the invocation entry point on {@code StrictPromptTemplate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code StrictPromptTemplate.identifier(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 identifier 的处理结果；returns the result of the operation.
     */
    private static boolean identifier(String value) {
        return value != null
                && value.matches("[A-Za-z][A-Za-z0-9_]{0,63}");
    }
}
