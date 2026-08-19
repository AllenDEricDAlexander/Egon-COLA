package top.egon.cola.component.gateway.mcp.resource.domain;

import top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accepts reviewed internal and App UI namespaces and rejects traversal forms.
 * 补充说明 / Supplementary summary: {@code McpResourceUriValidator} 是校验器，位于当前 Gateway 模块的相关包中，负责MCP资源Uri校验器相关的职责与边界。
 * English supplement: {@code McpResourceUriValidator} is a mcp resource uri validator validator in the current Gateway module; it owns the mcp resource uri validator-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpResourceUriValidator {

    /**
     * 中文说明：表示 VARIABLE 这一固定值；它属于 {@code McpResourceUriValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value variable; it is a state, type, or protocol value of {@code McpResourceUriValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpResourceUriValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceUriValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Pattern VARIABLE = Pattern.compile(
            "\\{([A-Za-z][A-Za-z0-9_]{0,63})}"
    );

    /**
     * 中文说明：表示 AUTHORITY 这一固定值；它属于 {@code McpResourceUriValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value authority; it is a state, type, or protocol value of {@code McpResourceUriValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpResourceUriValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceUriValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Pattern AUTHORITY = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._-]{0,127}"
    );

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code McpResourceUriValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpResourceUriValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceUriValidator.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 validate 的处理结果；returns the result of the operation.
     */
    public URI validate(String value) {
        if (value == null || value.isBlank() || value.length() > 2048) {
            throw McpResourceDriver.rejected("MCP resource URI is invalid");
        }
        String uri = value.trim();
        String lower = uri.toLowerCase(Locale.ROOT);
        if (uri.contains("\\")
                || lower.contains("%2e")
                || lower.contains("%2f")
                || lower.contains("%5c")
                || lower.contains("%00")) {
            throw McpResourceDriver.rejected(
                    "MCP resource URI contains an unsafe encoding"
            );
        }
        URI parsed;
        try {
            parsed = URI.create(uri);
        } catch (IllegalArgumentException failure) {
            throw McpResourceDriver.rejected("MCP resource URI is invalid");
        }
        boolean supportedScheme = "egon".equalsIgnoreCase(parsed.getScheme())
                || "ui".equalsIgnoreCase(parsed.getScheme());
        if (!supportedScheme
                || parsed.getRawAuthority() == null
                || !AUTHORITY.matcher(parsed.getRawAuthority()).matches()
                || parsed.getUserInfo() != null
                || parsed.getPort() != -1
                || parsed.getRawQuery() != null
                || parsed.getRawFragment() != null) {
            throw McpResourceDriver.rejected(
                    "MCP resource URI must use an internal scheme"
            );
        }
        String path = parsed.getRawPath();
        if (path == null || !path.startsWith("/")) {
            throw McpResourceDriver.rejected("MCP resource URI path is invalid");
        }
        for (String segment : path.split("/", -1)) {
            if (".".equals(segment) || "..".equals(segment)) {
                throw McpResourceDriver.rejected(
                        "MCP resource URI traversal is forbidden"
                );
            }
        }
        return parsed;
    }

    /**
     * 中文说明：执行 validate模板 操作；该方法是 {@code McpResourceUriValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate template operation; this method is the invocation entry point on {@code McpResourceUriValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceUriValidator.validateTemplate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 validate模板 的处理结果；returns the result of the operation.
     */
    public Template validateTemplate(String value) {
        if (value == null || value.isBlank()) {
            throw McpResourceDriver.rejected(
                    "MCP resource URI template is invalid"
            );
        }
        Matcher matcher = VARIABLE.matcher(value);
        StringBuffer concrete = new StringBuffer();
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            if (!names.add(matcher.group(1))) {
                throw McpResourceDriver.rejected(
                        "MCP resource URI template repeats a variable"
                );
            }
            matcher.appendReplacement(concrete, "template-value");
        }
        matcher.appendTail(concrete);
        if (concrete.indexOf("{") >= 0 || concrete.indexOf("}") >= 0) {
            throw McpResourceDriver.rejected(
                    "MCP resource URI template is invalid"
            );
        }
        URI checked = validate(concrete.toString());
        if (!"egon".equalsIgnoreCase(checked.getScheme())) {
            throw McpResourceDriver.rejected(
                    "MCP resource URI templates must use the egon scheme"
            );
        }
        return new Template(value.trim(), java.util.Set.copyOf(names));
    }

    /**
     * 中文说明：{@code Template} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责模板相关的职责与边界。
     * English summary: {@code Template} is an immutable data carrier in the current Gateway module; it owns the template-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param value 参数 值；parameter value。
     * @param variables 参数 variables；parameter variables。
     */
    public record Template(
    /**
     * 中文说明：保存 值 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpResourceUriValidator.Template} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by value; its type is {@code String}, and {@code McpResourceUriValidator.Template} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpResourceUriValidator.Template} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceUriValidator.Template}; do not couple callers to its representation when the owning type exposes an API.
     */
    String value,
    /**
     * 中文说明：保存 variables 对应的状态、依赖或配置值；字段类型为 {@code java.util.Set<String>}，由 {@code McpResourceUriValidator.Template} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by variables; its type is {@code java.util.Set<String>}, and {@code McpResourceUriValidator.Template} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpResourceUriValidator.Template} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceUriValidator.Template}; do not couple callers to its representation when the owning type exposes an API.
     */
    java.util.Set<String> variables) {
    }
}
