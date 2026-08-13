package top.egon.cola.component.gateway.admin.rule.service;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
import top.egon.cola.component.gateway.admin.auth.domain.vo.*;
import top.egon.cola.component.gateway.admin.auth.service.*;
import top.egon.cola.component.gateway.admin.bootstrap.*;
import top.egon.cola.component.gateway.admin.catalog.controller.*;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.*;
import top.egon.cola.component.gateway.admin.catalog.domain.enums.*;
import top.egon.cola.component.gateway.admin.catalog.domain.po.*;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.*;
import top.egon.cola.component.gateway.admin.catalog.repository.*;
import top.egon.cola.component.gateway.admin.catalog.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.catalog.service.*;
import top.egon.cola.component.gateway.admin.config.*;
import top.egon.cola.component.gateway.admin.config.properties.*;
import top.egon.cola.component.gateway.admin.credential.controller.*;
import top.egon.cola.component.gateway.admin.credential.domain.dto.*;
import top.egon.cola.component.gateway.admin.credential.domain.po.*;
import top.egon.cola.component.gateway.admin.credential.domain.vo.*;
import top.egon.cola.component.gateway.admin.credential.repository.*;
import top.egon.cola.component.gateway.admin.credential.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.credential.service.*;
import top.egon.cola.component.gateway.admin.group.controller.*;
import top.egon.cola.component.gateway.admin.group.domain.dto.*;
import top.egon.cola.component.gateway.admin.group.domain.po.*;
import top.egon.cola.component.gateway.admin.group.domain.vo.*;
import top.egon.cola.component.gateway.admin.group.repository.*;
import top.egon.cola.component.gateway.admin.group.service.*;
import top.egon.cola.component.gateway.admin.mcp.controller.*;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.*;
import top.egon.cola.component.gateway.admin.mcp.domain.enums.*;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.*;
import top.egon.cola.component.gateway.admin.mcp.domain.po.*;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.*;
import top.egon.cola.component.gateway.admin.mcp.repository.*;
import top.egon.cola.component.gateway.admin.mcp.repository.filesystem.*;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.mcp.service.*;
import top.egon.cola.component.gateway.admin.observability.controller.*;
import top.egon.cola.component.gateway.admin.observability.controller.message.*;
import top.egon.cola.component.gateway.admin.observability.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.observability.domain.dto.*;
import top.egon.cola.component.gateway.admin.observability.domain.enums.*;
import top.egon.cola.component.gateway.admin.observability.domain.po.*;
import top.egon.cola.component.gateway.admin.observability.domain.vo.*;
import top.egon.cola.component.gateway.admin.observability.repository.*;
import top.egon.cola.component.gateway.admin.observability.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.observability.service.*;
import top.egon.cola.component.gateway.admin.release.controller.*;
import top.egon.cola.component.gateway.admin.release.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.release.domain.*;
import top.egon.cola.component.gateway.admin.release.domain.dto.*;
import top.egon.cola.component.gateway.admin.release.domain.enums.*;
import top.egon.cola.component.gateway.admin.release.domain.po.*;
import top.egon.cola.component.gateway.admin.release.domain.vo.*;
import top.egon.cola.component.gateway.admin.release.repository.*;
import top.egon.cola.component.gateway.admin.release.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.release.service.*;
import top.egon.cola.component.gateway.admin.reporting.controller.openapi.*;
import top.egon.cola.component.gateway.admin.reporting.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.*;
import top.egon.cola.component.gateway.admin.reporting.domain.po.*;
import top.egon.cola.component.gateway.admin.reporting.domain.vo.*;
import top.egon.cola.component.gateway.admin.reporting.repository.*;
import top.egon.cola.component.gateway.admin.reporting.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.reporting.service.*;
import top.egon.cola.component.gateway.admin.routing.controller.*;
import top.egon.cola.component.gateway.admin.routing.domain.*;
import top.egon.cola.component.gateway.admin.routing.domain.dto.*;
import top.egon.cola.component.gateway.admin.routing.domain.po.*;
import top.egon.cola.component.gateway.admin.routing.domain.vo.*;
import top.egon.cola.component.gateway.admin.routing.repository.*;
import top.egon.cola.component.gateway.admin.routing.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.routing.service.*;
import top.egon.cola.component.gateway.admin.rule.domain.dto.*;
import top.egon.cola.component.gateway.admin.rule.domain.vo.*;
import top.egon.cola.component.gateway.admin.rule.service.*;
import top.egon.cola.component.gateway.admin.runtime.controller.*;
import top.egon.cola.component.gateway.admin.runtime.domain.dto.*;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.*;
import top.egon.cola.component.gateway.admin.runtime.service.*;
import top.egon.cola.component.gateway.admin.scope.controller.*;
import top.egon.cola.component.gateway.admin.scope.domain.*;
import top.egon.cola.component.gateway.admin.scope.domain.dto.*;
import top.egon.cola.component.gateway.admin.scope.domain.vo.*;
import top.egon.cola.component.gateway.admin.scope.service.*;
import top.egon.cola.component.gateway.admin.shared.controller.*;
import top.egon.cola.component.gateway.admin.shared.domain.*;
import top.egon.cola.component.gateway.admin.shared.domain.enums.*;
import top.egon.cola.component.gateway.admin.shared.domain.exception.*;
import top.egon.cola.component.gateway.admin.shared.domain.po.*;
import top.egon.cola.component.gateway.admin.shared.domain.vo.*;
import top.egon.cola.component.gateway.admin.shared.repository.*;
import top.egon.cola.component.gateway.admin.shared.repository.jdbc.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import top.egon.cola.component.ddc.format.DdcYamlConfigFormatStrategy;
import top.egon.cola.component.ddc.model.config.DdcConfigFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import top.egon.cola.component.gateway.admin.rule.service.GatewayYamlRemoval;
import top.egon.cola.component.gateway.admin.rule.service.GatewayYamlLeafLocation;
import top.egon.cola.component.gateway.admin.rule.service.GatewayYamlParentLink;
import top.egon.cola.component.gateway.admin.rule.service.GatewayYamlPrefixMatch;
/**
 * 中文说明：{@code GatewayDdcYamlDocument} 是类型，位于当前 Gateway 模块的相关包中，负责网关DdcYamlDocument相关的职责与边界。
 * English summary: {@code GatewayDdcYamlDocument} is a type in the current Gateway module; it owns the gateway ddc yaml document-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayDdcYamlDocument {

    /**
     * 中文说明：表示 资源NAME 这一固定值；它属于 {@code GatewayDdcYamlDocument} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value resource name; it is a state, type, or protocol value of {@code GatewayDdcYamlDocument} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDdcYamlDocument} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcYamlDocument}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String RESOURCE_NAME =
            DdcYamlConfigFormatStrategy.DEFAULT_RESOURCE_NAME;

    /**
     * 中文说明：表示 FORMAT 这一固定值；它属于 {@code GatewayDdcYamlDocument} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value format; it is a state, type, or protocol value of {@code GatewayDdcYamlDocument} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDdcYamlDocument} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcYamlDocument}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String FORMAT = DdcConfigFormat.YAML.name();

    /**
     * 中文说明：表示 ACTIVECONFIG键 这一固定值；它属于 {@code GatewayDdcYamlDocument} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value active config key; it is a state, type, or protocol value of {@code GatewayDdcYamlDocument} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDdcYamlDocument} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcYamlDocument}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String ACTIVE_CONFIG_KEY = "gateway.rules.active";

    /**
     * 中文说明：表示 CHUNKCONFIGPREFIX 这一固定值；它属于 {@code GatewayDdcYamlDocument} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value chunk config prefix; it is a state, type, or protocol value of {@code GatewayDdcYamlDocument} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDdcYamlDocument} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcYamlDocument}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String CHUNK_CONFIG_PREFIX = "gateway.rules.chunk.";

    /**
     * 中文说明：保存 parser 对应的状态、依赖或配置值；字段类型为 {@code Yaml}，由 {@code GatewayDdcYamlDocument} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by parser; its type is {@code Yaml}, and {@code GatewayDdcYamlDocument} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDdcYamlDocument} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcYamlDocument}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Yaml parser;

    /**
     * 中文说明：保存 writer 对应的状态、依赖或配置值；字段类型为 {@code Yaml}，由 {@code GatewayDdcYamlDocument} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by writer; its type is {@code Yaml}, and {@code GatewayDdcYamlDocument} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDdcYamlDocument} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcYamlDocument}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Yaml writer;

    /**
     * 中文说明：保存 校验器 对应的状态、依赖或配置值；字段类型为 {@code DdcYamlConfigFormatStrategy}，由 {@code GatewayDdcYamlDocument} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by validator; its type is {@code DdcYamlConfigFormatStrategy}, and {@code GatewayDdcYamlDocument} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDdcYamlDocument} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcYamlDocument}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DdcYamlConfigFormatStrategy validator =
            new DdcYamlConfigFormatStrategy();

    /**
     * 中文说明：创建 {@code GatewayDdcYamlDocument} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDdcYamlDocument} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    public GatewayDdcYamlDocument() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        this.parser = new Yaml(new SafeConstructor(loaderOptions));

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(
                DumperOptions.FlowStyle.BLOCK
        );
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setSplitLines(false);
        this.writer = new Yaml(dumperOptions);
    }

    /**
     * 中文说明：执行 putLeaf 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put leaf operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.putLeaf(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param propertyKey 参数 property键；parameter property key。
     * @param value 参数 值；parameter value。
     * @return 返回 putLeaf 的处理结果；returns the result of the operation.
     */
    public String putLeaf(String content, String propertyKey, String value) {
        validatePropertyKey(propertyKey);
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        Map<Object, Object> root = parse(content, true);
        List<String> segments = segments(propertyKey);
        List<GatewayYamlLeafLocation> matches = findLeaves(root, segments, 0);
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "ambiguous YAML property path: " + propertyKey
            );
        }
        if (matches.size() == 1) {
            GatewayYamlLeafLocation location = matches.getFirst();
            location.parent().put(location.key(), value);
        } else {
            container(root, segments).put(segments.getLast(), value);
        }
        return write(root);
    }

    /**
     * 中文说明：执行 removeLeaf 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remove leaf operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.removeLeaf(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param propertyKey 参数 property键；parameter property key。
     * @return 返回 removeLeaf 的处理结果；returns the result of the operation.
     */
    public GatewayYamlRemoval removeLeaf(String content, String propertyKey) {
        validatePropertyKey(propertyKey);
        Map<Object, Object> root = parse(content, false);
        List<GatewayYamlLeafLocation> matches = findLeaves(
                root,
                segments(propertyKey),
                0
        );
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "ambiguous YAML property path: " + propertyKey
            );
        }
        if (matches.isEmpty()) {
            return new GatewayYamlRemoval(content, false);
        }
        GatewayYamlLeafLocation location = matches.getFirst();
        location.parent().remove(location.key());
        for (int index = location.ancestors().size() - 1;
                index >= 0;
                index--) {
            GatewayYamlParentLink ancestor = location.ancestors().get(index);
            if (!ancestor.child().isEmpty()) {
                break;
            }
            ancestor.parent().remove(ancestor.key());
        }
        return new GatewayYamlRemoval(write(root), true);
    }

    /**
     * 中文说明：执行 leaf值 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the leaf value operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.leafValue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param propertyKey 参数 property键；parameter property key。
     * @return 返回 leaf值 的处理结果；returns the result of the operation.
     */
    public Optional<String> leafValue(
            String content,
            String propertyKey) {
        validatePropertyKey(propertyKey);
        List<GatewayYamlLeafLocation> matches = findLeaves(
                parse(content, false),
                segments(propertyKey),
                0
        );
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "ambiguous YAML property path: " + propertyKey
            );
        }
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        Object value = matches.getFirst().value();
        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException(
                    "Gateway rule YAML leaf must be a string: "
                            + propertyKey
            );
        }
        return Optional.of(stringValue);
    }

    /**
     * 中文说明：执行 parse 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the parse operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.parse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param allowEmpty 参数 allowEmpty；parameter allow empty。
     * @return 返回 parse 的处理结果；returns the result of the operation.
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Object> parse(String content, boolean allowEmpty) {
        if (content == null || content.isBlank()) {
            if (allowEmpty) {
                return new LinkedHashMap<>();
            }
            throw new IllegalArgumentException(
                    "application.yml must not be empty"
            );
        }
        Object loaded;
        try {
            loaded = parser.load(content);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "invalid application.yml",
                    exception
            );
        }
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(
                    "application.yml root must be a map"
            );
        }
        return (Map<Object, Object>) map;
    }

    /**
     * 中文说明：执行 container 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the container operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.container(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param root 参数 root；parameter root。
     * @param segments 参数 segments；parameter segments。
     * @return 返回 container 的处理结果；returns the result of the operation.
     */
    private Map<Object, Object> container(
            Map<Object, Object> root,
            List<String> segments) {
        Map<Object, Object> current = root;
        int index = 0;
        while (index < segments.size() - 1) {
            GatewayYamlPrefixMatch match = longestMapPrefix(current, segments, index);
            if (match != null) {
                current = match.map();
                index = match.nextIndex();
                continue;
            }
            String segment = segments.get(index);
            Object collision = matchingKey(current, segment);
            if (collision != null) {
                throw new IllegalArgumentException(
                        "YAML property path crosses a scalar: "
                                + String.join(".", segments)
                );
            }
            Map<Object, Object> child = new LinkedHashMap<>();
            current.put(segment, child);
            current = child;
            index++;
        }
        return current;
    }

    /**
     * 中文说明：执行 longestMapPrefix 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the longest map prefix operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.longestMapPrefix(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param current 参数 current；parameter current。
     * @param segments 参数 segments；parameter segments。
     * @param index 参数 索引；parameter index。
     * @return 返回 longestMapPrefix 的处理结果；returns the result of the operation.
     */
    @SuppressWarnings("unchecked")
    private GatewayYamlPrefixMatch longestMapPrefix(
            Map<Object, Object> current,
            List<String> segments,
            int index) {
        GatewayYamlPrefixMatch selected = null;
        for (Map.Entry<Object, Object> entry : current.entrySet()) {
            List<String> keySegments = segments(String.valueOf(entry.getKey()));
            if (!matches(segments, index, keySegments)
                    || index + keySegments.size() >= segments.size()) {
                continue;
            }
            if (!(entry.getValue() instanceof Map<?, ?> child)) {
                throw new IllegalArgumentException(
                        "YAML property path crosses a scalar: "
                                + String.join(".", segments)
                );
            }
            GatewayYamlPrefixMatch candidate = new GatewayYamlPrefixMatch(
                    (Map<Object, Object>) child,
                    index + keySegments.size()
            );
            if (selected == null
                    || candidate.nextIndex() > selected.nextIndex()) {
                selected = candidate;
            }
        }
        return selected;
    }

    /**
     * 中文说明：执行 findLeaves 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find leaves operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.findLeaves(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param current 参数 current；parameter current。
     * @param segments 参数 segments；parameter segments。
     * @param index 参数 索引；parameter index。
     * @return 返回 findLeaves 的处理结果；returns the result of the operation.
     */
    private List<GatewayYamlLeafLocation> findLeaves(
            Map<Object, Object> current,
            List<String> segments,
            int index) {
        return findLeaves(current, segments, index, List.of());
    }

    /**
     * 中文说明：执行 findLeaves 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find leaves operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.findLeaves(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param current 参数 current；parameter current。
     * @param segments 参数 segments；parameter segments。
     * @param index 参数 索引；parameter index。
     * @param ancestors 参数 ancestors；parameter ancestors。
     * @return 返回 findLeaves 的处理结果；returns the result of the operation.
     */
    @SuppressWarnings("unchecked")
    private List<GatewayYamlLeafLocation> findLeaves(
            Map<Object, Object> current,
            List<String> segments,
            int index,
            List<GatewayYamlParentLink> ancestors) {
        List<GatewayYamlLeafLocation> matches = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : current.entrySet()) {
            List<String> keySegments = segments(String.valueOf(entry.getKey()));
            if (!matches(segments, index, keySegments)) {
                continue;
            }
            int nextIndex = index + keySegments.size();
            if (nextIndex == segments.size()) {
                matches.add(new GatewayYamlLeafLocation(
                        current,
                        entry.getKey(),
                        entry.getValue(),
                        ancestors
                ));
            } else if (entry.getValue() instanceof Map<?, ?> child) {
                List<GatewayYamlParentLink> childAncestors =
                        new ArrayList<>(ancestors);
                childAncestors.add(new GatewayYamlParentLink(
                        current,
                        entry.getKey(),
                        (Map<Object, Object>) child
                ));
                matches.addAll(findLeaves(
                        (Map<Object, Object>) child,
                        segments,
                        nextIndex,
                        List.copyOf(childAncestors)
                ));
            }
        }
        return matches;
    }

    /**
     * 中文说明：执行 matches 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the matches operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.matches(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param propertySegments 参数 propertySegments；parameter property segments。
     * @param propertyIndex 参数 property索引；parameter property index。
     * @param keySegments 参数 键Segments；parameter key segments。
     * @return 返回 matches 的处理结果；returns the result of the operation.
     */
    private boolean matches(
            List<String> propertySegments,
            int propertyIndex,
            List<String> keySegments) {
        if (propertyIndex + keySegments.size()
                > propertySegments.size()) {
            return false;
        }
        for (int index = 0; index < keySegments.size(); index++) {
            if (!propertySegments.get(propertyIndex + index)
                    .equals(keySegments.get(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 中文说明：执行 matching键 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the matching key operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.matchingKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param map 参数 map；parameter map。
     * @param segment 参数 segment；parameter segment。
     * @return 返回 matching键 的处理结果；returns the result of the operation.
     */
    private Object matchingKey(Map<Object, Object> map, String segment) {
        return map.keySet().stream()
                .filter(key -> segment.equals(String.valueOf(key)))
                .findFirst()
                .orElse(null);
    }

    /**
     * 中文说明：执行 segments 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the segments operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.segments(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param propertyKey 参数 property键；parameter property key。
     * @return 返回 segments 的处理结果；returns the result of the operation.
     */
    private List<String> segments(String propertyKey) {
        return List.of(propertyKey.split("\\.", -1));
    }

    /**
     * 中文说明：执行 write 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the write operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.write(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param root 参数 root；parameter root。
     * @return 返回 write 的处理结果；returns the result of the operation.
     */
    private String write(Map<Object, Object> root) {
        String content = writer.dump(root);
        try {
            validator.load(
                    DdcYamlConfigFormatStrategy.DEFAULT_RESOURCE_NAME,
                    content,
                    1L
            );
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Gateway produced an invalid application.yml",
                    exception
            );
        }
        return content;
    }

    /**
     * 中文说明：执行 validateProperty键 操作；该方法是 {@code GatewayDdcYamlDocument} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate property key operation; this method is the invocation entry point on {@code GatewayDdcYamlDocument} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcYamlDocument.validatePropertyKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param propertyKey 参数 property键；parameter property key。
     */
    private void validatePropertyKey(String propertyKey) {
        if (propertyKey == null
                || propertyKey.isBlank()
                || propertyKey.endsWith(".")
                || propertyKey.contains("..")
                || !(propertyKey.equals(ACTIVE_CONFIG_KEY)
                || propertyKey.startsWith(CHUNK_CONFIG_PREFIX))) {
            throw new IllegalArgumentException(
                    "unsupported Gateway rule property: " + propertyKey
            );
        }
    }








}
