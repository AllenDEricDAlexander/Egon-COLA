package top.egon.cola.component.gateway.starter.discovery.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.discovery.GatewayDefinitionContributor;
import top.egon.cola.component.gateway.starter.discovery.mapper.GatewayHttpOperationMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discovers Gateway interface definitions from Spring MVC request mappings.
 *
 * 基于 Spring MVC 请求映射发现网关接口定义。
 */
public final class MvcGatewayDefinitionContributor
        implements GatewayDefinitionContributor {

    /** Spring MVC request mapping registry to inspect. 待读取的 Spring MVC 请求映射注册表。 */
    private final RequestMappingHandlerMapping mappings;

    /** Mapper that converts normalized HTTP mappings into report entries. 将标准化 HTTP 映射转换为报告条目的映射器。 */
    private final GatewayHttpOperationMapper mapper;

    /**
     * Creates an MVC Gateway definition contributor.
     *
     * 创建 MVC 网关定义贡献者。
     *
     * @param mappings     the MVC request mapping registry，MVC 请求映射注册表
     * @param properties   the Gateway reporting properties，网关报告配置
     * @param objectMapper the object mapper used for schema generation，用于生成模式的对象映射器
     */
    public MvcGatewayDefinitionContributor(
            RequestMappingHandlerMapping mappings,
            GatewayReportingProperties properties,
            ObjectMapper objectMapper) {
        this.mappings = mappings;
        mapper = new GatewayHttpOperationMapper(properties, objectMapper);
    }

    /**
     * Discovers annotated MVC handler types and maps their request mappings to
     * Gateway interface groups.
     *
     * 发现带注解的 MVC 处理器类型，并将其请求映射转换为网关接口分组。
     *
     * @return the discovered interface groups，已发现的接口分组
     * @throws IllegalArgumentException if an operation declaration cannot be
     *                                  represented by the Gateway report
     */
    @Override
    public List<DiscoveredInterfaceGroup> discover() {
        Map<Class<?>, List<GatewayHttpOperationMapper.Mapping>> byType =
                new LinkedHashMap<>();
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            if (excluded(handler.getBeanType())) {
                return;
            }
            byType.computeIfAbsent(
                    handler.getBeanType(),
                    ignored -> new ArrayList<>()
            ).add(new GatewayHttpOperationMapper.Mapping(
                    handler,
                    paths(mapping),
                    methods(mapping),
                    media(mapping.getConsumesCondition()
                            .getConsumableMediaTypes()),
                    media(mapping.getProducesCondition()
                            .getProducibleMediaTypes())
            ));
        });
        return byType.entrySet().stream()
                .map(entry -> mapper.group(
                        entry.getKey(),
                        entry.getValue()
                ))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Extracts the paths declared by an MVC request mapping.
     *
     * 提取 MVC 请求映射声明的路径。
     *
     * @param mapping the request mapping，请求映射
     * @return the declared paths, or {@code /} when no path is declared，声明的路径；未声明时返回 {@code /}
     */
    private Set<String> paths(RequestMappingInfo mapping) {
        Set<String> values = mapping.getPatternValues();
        return values.isEmpty() ? Set.of("/") : values;
    }

    /**
     * Extracts the HTTP methods declared by an MVC request mapping.
     *
     * 提取 MVC 请求映射声明的 HTTP 方法。
     *
     * @param mapping the request mapping，请求映射
     * @return the declared method names, or {@code ANY} when unrestricted，声明的方法名；未限制时返回 {@code ANY}
     */
    private Set<String> methods(RequestMappingInfo mapping) {
        Set<String> methods = mapping.getMethodsCondition()
                .getMethods()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
        return methods.isEmpty() ? Set.of("ANY") : methods;
    }

    /**
     * Converts media types to their reportable string representations.
     *
     * 将媒体类型转换为可写入报告的字符串表示。
     *
     * @param values the media types to convert，待转换的媒体类型
     * @return an unmodifiable set of media type strings，不可修改的媒体类型字符串集合
     */
    private Set<String> media(
            Set<org.springframework.http.MediaType> values) {
        return values.stream()
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Determines whether a handler type belongs to framework, error, or
     * Gateway infrastructure and must be omitted from discovery.
     *
     * 判断处理器类型是否属于框架、错误处理或网关基础设施，因而必须从发现结果中排除。
     *
     * @param type the handler bean type，处理器 Bean 类型
     * @return {@code true} when the handler must be excluded，处理器需要排除时返回 {@code true}
     */
    private boolean excluded(Class<?> type) {
        String name = type.getName();
        return name.startsWith("org.springframework.")
                || name.contains("BasicErrorController")
                || name.startsWith(
                "top.egon.cola.component.gateway.starter.");
    }
}
