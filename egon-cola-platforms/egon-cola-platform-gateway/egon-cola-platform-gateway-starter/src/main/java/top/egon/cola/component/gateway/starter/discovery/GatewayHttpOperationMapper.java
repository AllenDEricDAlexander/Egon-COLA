package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import top.egon.cola.component.gateway.contract.identity.GatewayOperationKey;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps normalized Spring HTTP handler mappings to Gateway interface report
 * entries.
 *
 * 将标准化的 Spring HTTP 处理器映射转换为网关接口报告条目。
 */
final class GatewayHttpOperationMapper {

    /** Reporting properties used to identify the provider application. 用于标识提供方应用的报告配置。 */
    private final GatewayReportingProperties properties;

    /** Validator and mapper for HTTP request schemas. HTTP 请求模式的校验器与映射器。 */
    private final GatewayRequestSchemaValidator requestSchemaValidator;

    /** Mapper for HTTP response schemas. HTTP 响应模式映射器。 */
    private final GatewayResponseSchemaMapper responseSchemaMapper;

    /**
     * Creates an HTTP operation mapper.
     *
     * 创建 HTTP 操作映射器。
     *
     * @param properties   the Gateway reporting properties，网关报告配置
     * @param objectMapper the object mapper used for request and response
     *                     schema processing；用于处理请求和响应模式的对象映射器
     */
    GatewayHttpOperationMapper(
            GatewayReportingProperties properties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.requestSchemaValidator = new GatewayRequestSchemaValidator(
                objectMapper
        );
        this.responseSchemaMapper = new GatewayResponseSchemaMapper(
                objectMapper
        );
    }

    /**
     * Maps all normalized HTTP mappings for a handler type to one interface
     * group.
     *
     * 将处理器类型的全部标准化 HTTP 映射汇总为一个接口分组。
     *
     * @param beanType the handler bean type，处理器 Bean 类型
     * @param mappings the normalized mappings declared by the handler type，处理器类型声明的标准化映射
     * @return the discovered interface group, or {@code null} when the handler
     *         type has no {@link GatewayInterfaceGroup} annotation；未标注 {@link GatewayInterfaceGroup} 时返回 {@code null}
     * @throws IllegalArgumentException if operation keys collide or an
     *                                  operation declaration is invalid
     */
    GatewayDefinitionContributor.DiscoveredInterfaceGroup group(
            Class<?> beanType,
            List<Mapping> mappings) {
        GatewayInterfaceGroup annotation =
                AnnotatedElementUtils.findMergedAnnotation(
                        beanType,
                        GatewayInterfaceGroup.class
                );
        if (annotation == null) {
            return null;
        }
        Map<String, GatewayInterfaceDefinitionReport.Operation> operations =
                new LinkedHashMap<>();
        mappings.forEach(mapping -> {
            for (String method : mapping.methods()) {
                for (String path : mapping.paths()) {
                    GatewayInterfaceDefinitionReport.Operation operation =
                            operation(annotation, mapping, method, path);
                    GatewayInterfaceDefinitionReport.Operation previous =
                            operations.putIfAbsent(
                                    operation.operationKey(),
                                    operation
                            );
                    if (previous != null
                            && !previous.methodIdentity().equals(
                            operation.methodIdentity())) {
                        throw new IllegalArgumentException(
                                "duplicate HTTP operation key "
                                        + operation.operationKey()
                        );
                    }
                }
            }
        });
        return new GatewayDefinitionContributor.DiscoveredInterfaceGroup(
                annotation.businessDomainCode(),
                annotation.businessDomainName(),
                null,
                annotation.entityDomainCode(),
                annotation.entityDomainName(),
                null,
                new GatewayInterfaceDefinitionReport.InterfaceGroup(
                        annotation.code(),
                        annotation.name(),
                        annotation.description(),
                        "STARTER",
                        beanType.getName(),
                        "HTTP",
                        Map.of(
                                "declaredHosts",
                                properties.getDeclaredHosts()
                        ),
                        new ArrayList<>(operations.values())
                )
        );
    }

    /**
     * Maps one HTTP method and path combination to a reported operation.
     *
     * 将一个 HTTP 方法与路径组合映射为报告中的操作。
     *
     * @param group      the declaring interface group annotation，声明接口分组的注解
     * @param mapping    the normalized handler mapping，标准化处理器映射
     * @param httpMethod the HTTP method name，HTTP 方法名称
     * @param path       the mapped request path，映射的请求路径
     * @return the reported HTTP operation，报告中的 HTTP 操作
     * @throws IllegalArgumentException if the request, response, or MCP
     *                                  declaration is invalid
     */
    private GatewayInterfaceDefinitionReport.Operation operation(
            GatewayInterfaceGroup group,
            Mapping mapping,
            String httpMethod,
            String path) {
        HandlerMethod handler = mapping.handler();
        GatewayOperation annotation =
                AnnotatedElementUtils.findMergedAnnotation(
                        handler.getMethod(),
                        GatewayOperation.class
                );
        boolean streaming = mapping.produces().stream()
                .anyMatch(value -> value.contains("text/event-stream"))
                || handler.getMethod().getReturnType().getName()
                .equals("reactor.core.publisher.Flux");
        GatewayRequestSchemaValidator.Result request =
                requestSchemaValidator.validate(handler, annotation, path);
        Map<String, Object> response = responseSchemaMapper.schema(
                handler.getMethod(),
                annotation
        );
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("httpMethod", httpMethod);
        attributes.put("path", path);
        attributes.put("consumes", mapping.consumes());
        attributes.put("produces", mapping.produces());
        attributes.put("responseMode", "TRANSPARENT");
        attributes.put("streaming", streaming);
        attributes.put(
                "idempotent",
                GatewayOperationSemantics.idempotent(annotation)
        );
        Map<String, Object> mcpExposure = McpExposureMapper.map(
                group,
                annotation,
                httpMethod + " " + path,
                streaming || "ANY".equals(httpMethod),
                request.parameters()
        );
        if (!mcpExposure.isEmpty()) {
            attributes.put(McpExposureMapper.ATTRIBUTE_NAME, mcpExposure);
        }
        return new GatewayInterfaceDefinitionReport.Operation(
                GatewayOperationKey.http(
                        properties.getApplicationCode(),
                        httpMethod,
                        path
                ).value(),
                "HTTP",
                httpMethod + " " + path,
                annotation == null || annotation.name().isBlank()
                        ? handler.getMethod().getName()
                        : annotation.name(),
                annotation == null ? "" : annotation.summary(),
                annotation == null ? "" : annotation.description(),
                annotation == null ? "" : annotation.owner(),
                GatewayOperationSemantics.tags(annotation),
                annotation != null && annotation.externalAccessible(),
                streaming ? "UNSUPPORTED" : "SUPPORTED",
                new GatewayInterfaceDefinitionReport.ProviderService(
                        properties.getBizCode(),
                        properties.getApplicationCode(),
                        properties.getEnv(),
                        properties.getNamespace(),
                        "HTTP",
                        properties.getApplicationCode(),
                        "default",
                        properties.getArtifactVersion(),
                        "HTTP"
                ),
                request.schema(),
                response,
                List.of(),
                null,
                attributes,
                handler.getMethod().isAnnotationPresent(Deprecated.class)
        );
    }

    /**
     * Normalized HTTP mapping data shared by MVC and WebFlux discovery.
     *
     * MVC 与 WebFlux 发现流程共用的标准化 HTTP 映射数据。
     *
     * @param handler  the mapped handler method，映射的处理器方法
     * @param paths    the mapped request paths，映射的请求路径集合
     * @param methods  the mapped HTTP method names，映射的 HTTP 方法名称集合
     * @param consumes the accepted media types，接受的媒体类型集合
     * @param produces the produced media types，生成的媒体类型集合
     */
    record Mapping(
            HandlerMethod handler,
            Set<String> paths,
            Set<String> methods,
            Set<String> consumes,
            Set<String> produces
    ) {
    }
}
