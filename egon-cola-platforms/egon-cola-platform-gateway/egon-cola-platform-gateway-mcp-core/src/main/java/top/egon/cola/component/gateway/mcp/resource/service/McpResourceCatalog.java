package top.egon.cola.component.gateway.mcp.resource.service;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeApp;
import top.egon.cola.component.gateway.mcp.app.service.AppUiResourceDriver;
import top.egon.cola.component.gateway.mcp.resource.domain.McpResourceUriValidator;
import top.egon.cola.component.gateway.mcp.rule.domain.CompiledMcpRules;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resolves exact resources and reviewed URI templates from active rules.
 * 补充说明 / Supplementary summary: {@code McpResourceCatalog} 是类型，位于当前 Gateway 模块的相关包中，负责MCP资源目录相关的职责与边界。
 * English supplement: {@code McpResourceCatalog} is a type in the current Gateway module; it owns the mcp resource catalog-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpResourceCatalog {

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code McpResourceCatalog} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code McpResourceCatalog} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpResourceCatalog} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceCatalog}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：保存 校验器 对应的状态、依赖或配置值；字段类型为 {@code McpResourceUriValidator}，由 {@code McpResourceCatalog} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by validator; its type is {@code McpResourceUriValidator}, and {@code McpResourceCatalog} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpResourceCatalog} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceCatalog}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpResourceUriValidator validator;

    /**
     * 中文说明：创建 {@code McpResourceCatalog} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpResourceCatalog} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param validator 参数 校验器；parameter validator。
     */
    public McpResourceCatalog(
            Supplier<CompiledMcpRules> rules,
            McpResourceUriValidator validator) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    /**
     * 中文说明：执行 resources 操作；该方法是 {@code McpResourceCatalog} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resources operation; this method is the invocation entry point on {@code McpResourceCatalog} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.resources(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @return 返回 resources 的处理结果；returns the result of the operation.
     */
    public List<McpRuntimeResource> resources(String serverCode) {
        CompiledMcpRules current = active();
        ArrayList<McpRuntimeResource> result = new ArrayList<>(
                current.resourcesByQualifiedName()
                .values().stream()
                .filter(McpRuntimeResource::enabled)
                .filter(resource -> resource.serverCode().equals(serverCode))
                .filter(resource -> current.remoteAvailable(
                        resource.remoteMountId(),
                        "RESOURCE"
                ))
                .toList());
        current.appsByQualifiedName().values().stream()
                .filter(McpRuntimeApp::enabled)
                .filter(app -> app.serverCode().equals(serverCode))
                .map(app -> new McpRuntimeResource(
                        app.appId(),
                        app.serverCode(),
                        app.name(),
                        app.resourceUri(),
                        "MCP App UI " + app.appCode() + "@" + app.version(),
                        app.mimeType(),
                        AppUiResourceDriver.DRIVER_TYPE,
                        null,
                        null,
                        Map.of(
                                "appCode", app.appCode(),
                                "version", app.version()
                        ),
                        app.permissions(),
                        app.artifactSizeBytes(),
                        true
                ))
                .forEach(result::add);
        return result.stream().sorted(java.util.Comparator.comparing(
                McpRuntimeResource::name
        )).toList();
    }

    /**
     * 中文说明：执行 templates 操作；该方法是 {@code McpResourceCatalog} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the templates operation; this method is the invocation entry point on {@code McpResourceCatalog} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.templates(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @return 返回 templates 的处理结果；returns the result of the operation.
     */
    public List<McpRuntimeResourceTemplate> templates(String serverCode) {
        CompiledMcpRules current = active();
        return current.templatesByQualifiedName().values().stream()
                .filter(McpRuntimeResourceTemplate::enabled)
                .filter(template -> template.serverCode().equals(serverCode))
                .filter(template -> current.remoteAvailable(
                        template.remoteMountId(),
                        "RESOURCE_TEMPLATE"
                ))
                .sorted(java.util.Comparator.comparing(
                        McpRuntimeResourceTemplate::name
                ))
                .toList();
    }

    /**
     * 中文说明：执行 resolve 操作；该方法是 {@code McpResourceCatalog} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve operation; this method is the invocation entry point on {@code McpResourceCatalog} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.resolve(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param uri 参数 uri；parameter uri。
     * @return 返回 resolve 的处理结果；returns the result of the operation.
     */
    public ResolvedResource resolve(String serverCode, String uri) {
        URI validated = validator.validate(uri);
        for (McpRuntimeResource resource : resources(serverCode)) {
            if (validator.validate(resource.uri()).equals(validated)) {
                return ResolvedResource.exact(resource, validated.toString());
            }
        }
        for (McpRuntimeResourceTemplate template : templates(serverCode)) {
            Map<String, String> variables = match(template, validated);
            if (variables != null) {
                return ResolvedResource.template(
                        template,
                        validated.toString(),
                        variables
                );
            }
        }
        throw McpResourceDriver.rejected("MCP resource was not found");
    }

    /**
     * 中文说明：执行 match 操作；该方法是 {@code McpResourceCatalog} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the match operation; this method is the invocation entry point on {@code McpResourceCatalog} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.match(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param template 参数 模板；parameter template。
     * @param concrete 参数 concrete；parameter concrete。
     * @return 返回 match 的处理结果；returns the result of the operation.
     */
    private Map<String, String> match(
            McpRuntimeResourceTemplate template,
            URI concrete) {
        McpResourceUriValidator.Template checked =
                validator.validateTemplate(template.uriTemplate());
        URI descriptor = URI.create(checked.value().replaceAll(
                "\\{[A-Za-z][A-Za-z0-9_]{0,63}}",
                "template-value"
        ));
        if (!descriptor.getRawAuthority().equals(concrete.getRawAuthority())) {
            return null;
        }
        String[] pattern = URI.create(checked.value().replace("{", "%7B")
                        .replace("}", "%7D"))
                .getRawPath()
                .split("/", -1);
        String[] values = concrete.getRawPath().split("/", -1);
        if (pattern.length != values.length) {
            return null;
        }
        LinkedHashMap<String, String> variables = new LinkedHashMap<>();
        for (int index = 0; index < pattern.length; index++) {
            String segment = pattern[index]
                    .replace("%7B", "{")
                    .replace("%7D", "}");
            if (segment.matches("\\{[A-Za-z][A-Za-z0-9_]{0,63}}")) {
                if (values[index].isBlank()) {
                    return null;
                }
                variables.put(
                        segment.substring(1, segment.length() - 1),
                        values[index]
                );
            } else if (!segment.equals(values[index])) {
                return null;
            }
        }
        return Collections.unmodifiableMap(variables);
    }

    /**
     * 中文说明：执行 active 操作；该方法是 {@code McpResourceCatalog} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the active operation; this method is the invocation entry point on {@code McpResourceCatalog} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.active(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 active 的处理结果；returns the result of the operation.
     */
    private CompiledMcpRules active() {
        CompiledMcpRules active = rules.get();
        return active == null ? CompiledMcpRules.empty() : active;
    }

    /**
     * 中文说明：{@code ResolvedResource} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Resolved资源相关的职责与边界。
     * English summary: {@code ResolvedResource} is an immutable data carrier in the current Gateway module; it owns the resolved resource-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param resource 参数 资源；parameter resource。
     * @param template 参数 模板；parameter template。
     * @param uri 参数 uri；parameter uri。
     * @param uriVariables 参数 uriVariables；parameter uri variables。
     */
    public record ResolvedResource(
            /**
             * 中文说明：保存 资源 对应的状态、依赖或配置值；字段类型为 {@code McpRuntimeResource}，由 {@code McpResourceCatalog.ResolvedResource} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource; its type is {@code McpRuntimeResource}, and {@code McpResourceCatalog.ResolvedResource} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceCatalog.ResolvedResource} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceCatalog.ResolvedResource}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpRuntimeResource resource,
            /**
             * 中文说明：保存 模板 对应的状态、依赖或配置值；字段类型为 {@code McpRuntimeResourceTemplate}，由 {@code McpResourceCatalog.ResolvedResource} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by template; its type is {@code McpRuntimeResourceTemplate}, and {@code McpResourceCatalog.ResolvedResource} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceCatalog.ResolvedResource} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceCatalog.ResolvedResource}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpRuntimeResourceTemplate template,
            /**
             * 中文说明：保存 uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpResourceCatalog.ResolvedResource} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by uri; its type is {@code String}, and {@code McpResourceCatalog.ResolvedResource} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceCatalog.ResolvedResource} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceCatalog.ResolvedResource}; do not couple callers to its representation when the owning type exposes an API.
             */
            String uri,
            /**
             * 中文说明：保存 uriVariables 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code McpResourceCatalog.ResolvedResource} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by uri variables; its type is {@code Map<String, String>}, and {@code McpResourceCatalog.ResolvedResource} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpResourceCatalog.ResolvedResource} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpResourceCatalog.ResolvedResource}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> uriVariables
    ) {

        /**
         * 中文说明：创建 {@code McpResourceCatalog.ResolvedResource} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpResourceCatalog.ResolvedResource} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param resource 参数 资源；parameter resource。
         * @param template 参数 模板；parameter template。
         * @param uri 参数 uri；parameter uri。
         * @param uriVariables 参数 uriVariables；parameter uri variables。
         */
        public ResolvedResource {
            if ((resource == null) == (template == null)) {
                throw new IllegalArgumentException(
                        "exactly one MCP resource descriptor is required"
                );
            }
            uri = Objects.requireNonNull(uri, "uri");
            uriVariables = Map.copyOf(uriVariables);
        }

        /**
         * 中文说明：执行 exact 操作；该方法是 {@code McpResourceCatalog.ResolvedResource} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the exact operation; this method is the invocation entry point on {@code McpResourceCatalog.ResolvedResource} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.ResolvedResource.exact(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param resource 参数 资源；parameter resource。
         * @param uri 参数 uri；parameter uri。
         * @return 返回 exact 的处理结果；returns the result of the operation.
         */
        public static ResolvedResource exact(
                McpRuntimeResource resource,
                String uri) {
            return new ResolvedResource(resource, null, uri, Map.of());
        }

        /**
         * 中文说明：执行 模板 操作；该方法是 {@code McpResourceCatalog.ResolvedResource} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the template operation; this method is the invocation entry point on {@code McpResourceCatalog.ResolvedResource} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.ResolvedResource.template(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param template 参数 模板；parameter template。
         * @param uri 参数 uri；parameter uri。
         * @param variables 参数 variables；parameter variables。
         * @return 返回 模板 的处理结果；returns the result of the operation.
         */
        public static ResolvedResource template(
                McpRuntimeResourceTemplate template,
                String uri,
                Map<String, String> variables) {
            return new ResolvedResource(null, template, uri, variables);
        }

        /**
         * 中文说明：执行 服务器Code 操作；该方法是 {@code McpResourceCatalog.ResolvedResource} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the server code operation; this method is the invocation entry point on {@code McpResourceCatalog.ResolvedResource} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.ResolvedResource.serverCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 服务器Code 的处理结果；returns the result of the operation.
         */
        public String serverCode() {
            return resource == null
                    ? template.serverCode()
                    : resource.serverCode();
        }

        /**
         * 中文说明：执行 name 操作；该方法是 {@code McpResourceCatalog.ResolvedResource} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the name operation; this method is the invocation entry point on {@code McpResourceCatalog.ResolvedResource} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.ResolvedResource.name(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 name 的处理结果；returns the result of the operation.
         */
        public String name() {
            return resource == null ? template.name() : resource.name();
        }

        /**
         * 中文说明：执行 mimeType 操作；该方法是 {@code McpResourceCatalog.ResolvedResource} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the mime type operation; this method is the invocation entry point on {@code McpResourceCatalog.ResolvedResource} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.ResolvedResource.mimeType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 mimeType 的处理结果；returns the result of the operation.
         */
        public String mimeType() {
            return resource == null
                    ? template.mimeType()
                    : resource.mimeType();
        }

        /**
         * 中文说明：执行 驱动器Type 操作；该方法是 {@code McpResourceCatalog.ResolvedResource} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the driver type operation; this method is the invocation entry point on {@code McpResourceCatalog.ResolvedResource} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.ResolvedResource.driverType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 驱动器Type 的处理结果；returns the result of the operation.
         */
        public String driverType() {
            return resource == null
                    ? template.driverType()
                    : resource.driverType();
        }

        /**
         * 中文说明：执行 操作Id 操作；该方法是 {@code McpResourceCatalog.ResolvedResource} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the operation id operation; this method is the invocation entry point on {@code McpResourceCatalog.ResolvedResource} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.ResolvedResource.operationId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 操作Id 的处理结果；returns the result of the operation.
         */
        public String operationId() {
            return resource == null
                    ? template.operationId()
                    : resource.operationId();
        }

        /**
         * 中文说明：执行 远程MountId 操作；该方法是 {@code McpResourceCatalog.ResolvedResource} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the remote mount id operation; this method is the invocation entry point on {@code McpResourceCatalog.ResolvedResource} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.ResolvedResource.remoteMountId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 远程MountId 的处理结果；returns the result of the operation.
         */
        public String remoteMountId() {
            return resource == null
                    ? template.remoteMountId()
                    : resource.remoteMountId();
        }

        /**
         * 中文说明：执行 配置 操作；该方法是 {@code McpResourceCatalog.ResolvedResource} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the configuration operation; this method is the invocation entry point on {@code McpResourceCatalog.ResolvedResource} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.ResolvedResource.configuration(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 配置 的处理结果；returns the result of the operation.
         */
        public Map<String, String> configuration() {
            return resource == null
                    ? template.configuration()
                    : resource.configuration();
        }

        /**
         * 中文说明：执行 maximumBytes 操作；该方法是 {@code McpResourceCatalog.ResolvedResource} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the maximum bytes operation; this method is the invocation entry point on {@code McpResourceCatalog.ResolvedResource} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.ResolvedResource.maximumBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 maximumBytes 的处理结果；returns the result of the operation.
         */
        public long maximumBytes() {
            long configured = resource == null
                    ? template.maxBytes()
                    : resource.maxBytes();
            return configured == 0L ? 4L * 1024 * 1024 : configured;
        }

        /**
         * 中文说明：执行 请求 操作；该方法是 {@code McpResourceCatalog.ResolvedResource} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the request operation; this method is the invocation entry point on {@code McpResourceCatalog.ResolvedResource} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpResourceCatalog.ResolvedResource.request(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param attributes 参数 attributes；parameter attributes。
         * @return 返回 请求 的处理结果；returns the result of the operation.
         */
        public McpResourceDriver.ReadRequest request(
                Map<String, Object> attributes) {
            return new McpResourceDriver.ReadRequest(
                    serverCode(),
                    name(),
                    uri,
                    mimeType(),
                    operationId(),
                    configuration(),
                    uriVariables,
                    maximumBytes(),
                    attributes
            );
        }
    }
}
